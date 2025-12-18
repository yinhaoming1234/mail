-- ===========================================
-- POP3 邮件服务器数据库架构
-- PostgreSQL 兼容
-- ===========================================

-- 邮件域名表
CREATE TABLE IF NOT EXISTS mail_domains (
    id SERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_local BOOLEAN DEFAULT true,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    quota_bytes BIGINT DEFAULT 104857600, -- 100MB 默认配额
    used_bytes BIGINT DEFAULT 0,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    CONSTRAINT fk_user_domain FOREIGN KEY (domain) REFERENCES mail_domains(domain)
);

-- 邮件表
CREATE TABLE IF NOT EXISTS emails (
    id UUID PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    recipients VARCHAR(255)[] NOT NULL,
    subject TEXT,
    body TEXT,
    raw_content TEXT NOT NULL,
    size BIGINT NOT NULL,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    owner VARCHAR(255) NOT NULL,
    CONSTRAINT fk_email_owner FOREIGN KEY (owner) REFERENCES users(email)
);

-- 邮件索引
CREATE INDEX IF NOT EXISTS idx_emails_owner ON emails(owner);
CREATE INDEX IF NOT EXISTS idx_emails_received_at ON emails(received_at DESC);
CREATE INDEX IF NOT EXISTS idx_emails_is_deleted ON emails(is_deleted);
CREATE INDEX IF NOT EXISTS idx_emails_owner_deleted ON emails(owner, is_deleted);

-- 用户索引
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_domain ON users(domain);

-- 域名索引
CREATE INDEX IF NOT EXISTS idx_mail_domains_domain ON mail_domains(domain);

-- 插入默认域名
INSERT INTO mail_domains (domain, description, is_local, is_enabled)
VALUES ('localhost', 'Local development domain', true, true)
ON CONFLICT (domain) DO NOTHING;

INSERT INTO mail_domains (domain, description, is_local, is_enabled)
VALUES ('example.com', 'Example domain', true, true)
ON CONFLICT (domain) DO NOTHING;

-- 插入测试用户
-- 密码: password123 (明文存储，仅用于测试)
INSERT INTO users (email, username, password_hash, domain)
VALUES ('test@localhost', 'test', 'password123', 'localhost')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, username, password_hash, domain)
VALUES ('admin@localhost', 'admin', 'admin123', 'localhost')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, username, password_hash, domain)
VALUES ('user@example.com', 'user', 'user123', 'example.com')
ON CONFLICT (email) DO NOTHING;

-- 更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为用户表添加更新触发器
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为域名表添加更新触发器
DROP TRIGGER IF EXISTS update_mail_domains_updated_at ON mail_domains;
CREATE TRIGGER update_mail_domains_updated_at
    BEFORE UPDATE ON mail_domains
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

