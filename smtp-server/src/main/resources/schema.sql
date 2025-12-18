-- SMTP 邮件服务器数据库初始化脚本
-- PostgreSQL 数据库

-- 创建扩展（如果需要）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==================== 邮件域名表 ====================
CREATE TABLE IF NOT EXISTS mail_domains (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    domain VARCHAR(255) NOT NULL UNIQUE,
    is_local BOOLEAN NOT NULL DEFAULT true,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 域名索引
CREATE INDEX IF NOT EXISTS idx_mail_domains_domain ON mail_domains(domain);
CREATE INDEX IF NOT EXISTS idx_mail_domains_local_enabled ON mail_domains(is_local, is_enabled);

-- ==================== 用户表 ====================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(64) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    quota_bytes BIGINT NOT NULL DEFAULT 1073741824, -- 默认 1GB
    used_bytes BIGINT NOT NULL DEFAULT 0,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_users_domain FOREIGN KEY (domain) REFERENCES mail_domains(domain)
);

-- 用户索引
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username_domain ON users(username, domain);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(is_enabled);

-- ==================== 邮件表 ====================
CREATE TABLE IF NOT EXISTS emails (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender VARCHAR(320) NOT NULL,
    recipients VARCHAR(320)[] NOT NULL,
    subject VARCHAR(998),
    body TEXT,
    raw_content TEXT NOT NULL,
    size BIGINT NOT NULL DEFAULT 0,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT false,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    owner VARCHAR(320) NOT NULL,
    
    -- 可选：外键约束（如果需要严格的数据完整性）
    -- CONSTRAINT fk_emails_owner FOREIGN KEY (owner) REFERENCES users(email)
    
    CONSTRAINT chk_emails_size CHECK (size >= 0)
);

-- 邮件索引
CREATE INDEX IF NOT EXISTS idx_emails_owner ON emails(owner);
CREATE INDEX IF NOT EXISTS idx_emails_owner_deleted ON emails(owner, is_deleted);
CREATE INDEX IF NOT EXISTS idx_emails_owner_read ON emails(owner, is_read, is_deleted);
CREATE INDEX IF NOT EXISTS idx_emails_received_at ON emails(received_at DESC);
CREATE INDEX IF NOT EXISTS idx_emails_sender ON emails(sender);

-- GIN 索引用于收件人数组搜索
CREATE INDEX IF NOT EXISTS idx_emails_recipients ON emails USING GIN(recipients);

-- ==================== 邮件队列表（用于发送外部邮件） ====================
CREATE TABLE IF NOT EXISTS mail_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email_id UUID NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, sending, sent, failed
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mail_queue_email FOREIGN KEY (email_id) REFERENCES emails(id) ON DELETE CASCADE,
    CONSTRAINT chk_mail_queue_status CHECK (status IN ('pending', 'sending', 'sent', 'failed'))
);

-- 邮件队列索引
CREATE INDEX IF NOT EXISTS idx_mail_queue_status ON mail_queue(status);
CREATE INDEX IF NOT EXISTS idx_mail_queue_next_retry ON mail_queue(next_retry_at) WHERE status = 'pending';

-- ==================== 发送日志表 ====================
CREATE TABLE IF NOT EXISTS delivery_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email_id UUID NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    remote_host VARCHAR(255),
    remote_ip VARCHAR(45),
    status VARCHAR(20) NOT NULL, -- delivered, bounced, deferred
    smtp_code INT,
    smtp_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_delivery_logs_email FOREIGN KEY (email_id) REFERENCES emails(id) ON DELETE CASCADE
);

-- 发送日志索引
CREATE INDEX IF NOT EXISTS idx_delivery_logs_email ON delivery_logs(email_id);
CREATE INDEX IF NOT EXISTS idx_delivery_logs_created ON delivery_logs(created_at DESC);

-- ==================== 更新时间触发器 ====================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要的表创建触发器
DROP TRIGGER IF EXISTS update_mail_domains_updated_at ON mail_domains;
CREATE TRIGGER update_mail_domains_updated_at
    BEFORE UPDATE ON mail_domains
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_mail_queue_updated_at ON mail_queue;
CREATE TRIGGER update_mail_queue_updated_at
    BEFORE UPDATE ON mail_queue
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==================== 初始数据 ====================
-- 插入默认本地域名（如果不存在）
INSERT INTO mail_domains (domain, is_local, is_enabled)
VALUES ('localhost', true, true)
ON CONFLICT (domain) DO NOTHING;

-- 插入示例用户（密码为 'password' 的 bcrypt 哈希）
-- 实际使用时请修改密码
INSERT INTO users (username, domain, email, password_hash)
VALUES 
    ('admin', 'localhost', 'admin@localhost', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
    ('test', 'localhost', 'test@localhost', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy')
ON CONFLICT (email) DO NOTHING;

-- ==================== 视图 ====================
-- 邮箱统计视图
CREATE OR REPLACE VIEW mailbox_stats AS
SELECT 
    u.email,
    u.quota_bytes,
    u.used_bytes,
    COUNT(e.id) as total_emails,
    COUNT(CASE WHEN e.is_read = false AND e.is_deleted = false THEN 1 END) as unread_emails,
    COALESCE(SUM(e.size), 0) as total_size
FROM users u
LEFT JOIN emails e ON u.email = e.owner AND e.is_deleted = false
GROUP BY u.id, u.email, u.quota_bytes, u.used_bytes;

-- 待发送邮件视图
CREATE OR REPLACE VIEW pending_deliveries AS
SELECT 
    mq.*,
    e.sender,
    e.subject,
    e.size
FROM mail_queue mq
JOIN emails e ON mq.email_id = e.id
WHERE mq.status = 'pending'
  AND (mq.next_retry_at IS NULL OR mq.next_retry_at <= CURRENT_TIMESTAMP)
ORDER BY mq.created_at;

-- ==================== 注释 ====================
COMMENT ON TABLE mail_domains IS '邮件域名表，存储系统支持的邮件域名';
COMMENT ON TABLE users IS '用户表，存储邮箱用户信息';
COMMENT ON TABLE emails IS '邮件表，存储所有接收的邮件';
COMMENT ON TABLE mail_queue IS '邮件队列表，用于管理待发送的外部邮件';
COMMENT ON TABLE delivery_logs IS '发送日志表，记录邮件发送结果';

COMMENT ON COLUMN emails.raw_content IS '原始邮件内容，包含完整的邮件头和正文';
COMMENT ON COLUMN emails.owner IS '邮件所属用户的邮箱地址';
COMMENT ON COLUMN users.quota_bytes IS '用户邮箱容量限制（字节）';
COMMENT ON COLUMN users.used_bytes IS '用户已使用的邮箱容量（字节）';

