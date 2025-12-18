# Android 客户端对接 POP3 服务器集成指南

## 概述

本文档说明如何将 Android 邮件客户端与自定义 POP3 服务器集成使用。

## 前置条件

- Java 25+ (用于运行 POP3 服务器)
- Maven 3.8+
- PostgreSQL 12+
- Android Studio
- Android 设备/模拟器 (API 26+)

## POP3 服务器配置

### 1. 数据库准备

确保 PostgreSQL 数据库运行:

```bash
# 检查 PostgreSQL 状态
pg_ctl status

# 如果未运行，启动 PostgreSQL
pg_ctl start
```

创建数据库和测试数据:

```bash
# 连接到 PostgreSQL
psql -U postgres

# 创建数据库
CREATE DATABASE maildb;

# 退出 psql
\q

# 导入初始数据
psql -U postgres -d maildb < init.sql
```

### 2. 启动 POP3 服务器

```bash
cd pop3-server

# 构建项目
mvn clean package

# 启动服务器
java --enable-preview -jar target/pop3-server-1.0-SNAPSHOT.jar
```

服务器将在以下配置下启动:
- **地址**: localhost
- **端口**: 1100
- **SSL**: 否 (明文连接)

验证启动成功:
```
[main] INFO com.yhm.pop3.Pop3Server - POP3 server started on port 1100
[main] INFO com.yhm.pop3.Pop3Server - Maximum connections: 500
```

### 3. 测试账号

默认测试账号 (根据 `init.sql`):
- **用户名**: test@localhost
- **密码**: password123

## Android 客户端配置

### 1. 快速配置 (推荐)

1. 打开应用
2. 点击 "Add Account"
3. 点击 "Use Local Server" 按钮
4. 应用会自动填充:
   - **POP3 Host**: localhost
   - **POP3 Port**: 1100
   - **POP3 SSL**: 关闭
   - **SMTP Host**: localhost
   - **SMTP Port**: 2525
   - **SMTP SSL**: 关闭

5. 输入账户信息:
   - **Account Name**: 任意名称 (例如: "Local Test")
   - **Email Address**: test@localhost
   - **Username**: test@localhost
   - **Password**: password123

6. 点击 "Test Connection" 验证连接
7. 点击 "Save Account" 保存配置

### 2. 手动配置

如果需要连接到其他服务器:

1. **POP3 Host**: 服务器地址
2. **Port**: 服务器端口
   - 标准 POP3: 110 (无 SSL)
   - POP3S: 995 (使用 SSL)
   - 自定义服务器: 1100 (无 SSL)
3. **SSL/TLS**: 根据服务器配置选择
4. 输入相应的用户凭据

## 功能说明

### 邮件接收 (POP3)

- 支持 UIDL 命令,确保不重复同步邮件
- 自动重试机制 (最多 3 次),提高连接稳定性
- 指数退避策略,避免频繁重试

### 邮件发送 (SMTP)

- 完整 SMTP 协议支持 (EHLO/HELO, MAIL FROM, RCPT TO, DATA)
- 支持多收件人(逗号分隔)
- 自动重试机制 (最多 3 次)
- RFC 5322 标准邮件格式
- 实时发送状态反馈

### 邮件查看

- 支持纯文本和 HTML 邮件
- 自动解析 multipart 邮件

### 邮件删除

- 支持从服务器删除邮件
- 本地数据库同步更新

## 网络配置

### 模拟器访问本地服务器

如果使用 Android 模拟器:

1. **使用 10.0.2.2 代替 localhost**:
   - Android 模拟器将 10.0.2.2 映射到宿主机的 localhost
   - 配置时使用: `10.0.2.2:1100`

2. 或者使用 adb 端口转发:
   ```bash
   adb forward tcp:1100 tcp:1100
   ```
   然后在应用中使用 `localhost:1100`

### 真机访问本地服务器

如果使用真机测试:

1. **确保手机和电脑在同一网络**
2. **使用电脑的局域网 IP**:
   ```bash
   # Windows
   ipconfig
   # 查找 IPv4 地址

   # Linux/Mac
   ifconfig
   # 或 ip addr
   ```
3. **配置服务器监听所有接口**:
   编辑 `pop3-server/src/main/resources/application.properties`:
   ```properties
   pop3.domain=0.0.0.0
   ```
   重启服务器

4. **在应用中使用局域网 IP**:
   例如: `192.168.1.100:1100`

## 安全注意事项

### 开发环境

当前配置允许本地开发:
- ✅ localhost 明文流量
- ✅ 127.0.0.1 明文流量
- ✅ 10.0.2.2 明文流量 (模拟器)
- ❌ 其他域名必须使用 HTTPS

网络安全配置文件位于:
`mailclient/app/src/main/res/xml/network_security_config.xml`

### 生产环境

**⚠️ 警告**: 当前配置仅适用于开发和测试!

生产环境必须:
1. 启用 TLS/SSL 加密
2. 使用有效的 SSL 证书
3. 禁用明文流量
4. 使用安全的密码存储 (Android Keystore)

## 常见问题

### Q: 连接失败 "Connection refused"

**A**: 检查以下项:
1. POP3 服务器是否运行
2. 端口号是否正确
3. 如果使用模拟器,是否使用 10.0.2.2
4. 防火墙是否阻止连接

### Q: 认证失败

**A**: 确认:
1. 用户名和密码正确
2. 数据库中存在该用户
3. 检查服务器日志获取详细错误

### Q: 无法同步邮件

**A**: 检查:
1. 数据库中是否有邮件数据
2. 查看 Android Logcat 日志
3. 查看服务器日志

### Q: SSL/TLS 错误

**A**: 
- 连接本地服务器时,关闭 SSL 选项
- 本地服务器不支持 TLS

## 日志查看

### Android 客户端日志

使用 Android Studio Logcat:
```
Tag: Pop3Client
```

关键日志:
- 连接状态
- POP3 命令和响应
- 同步进度
- 错误信息

### 服务器日志

查看控制台输出或日志文件:
```bash
cd pop3-server/logs
tail -f pop3-server.log

# SMTP 日志
cd smtp-server/logs
tail -f smtp-server.log
```

## 使用说明

### 接收邮件

1. 在邮件列表界面,点击顶部的刷新按钮同步邮件
2. 或下拉列表刷新
3. 点击邮件查看详情
4. 支持从服务器删除邮件

### 发送邮件

1. 在邮件列表界面,点击右下角的撰写按钮 (铅笔图标)
2. 输入收件人邮箱地址
   - 支持多收件人,用逗号分隔
   - 例如: `user1@localhost, user2@localhost`
3. 输入邮件主题
4. 输入邮件正文
5. 点击右上角或底部的发送按钮
6. 等待发送完成提示
7. 自动返回邮件列表

## 日志查看

### Android 客户端日志

使用 Android Studio Logcat:
```
Tag: Pop3Client (POP3协议)
Tag: SmtpClient (SMTP协议)
Tag: EmailRepository (数据操作)
```

关键日志:
- 连接状态
- POP3/SMTP 命令和响应
- 同步进度
- 发送状态
- 错误信息

### 服务器日志

查看控制台输出或日志文件:
```bash
cd pop3-server/logs
tail -f pop3-server.log

# SMTP 日志
cd smtp-server/logs
tail -f smtp-server.log
```

## 开发者信息

### 代码结构

**Android 客户端**:
- `Pop3Client.kt`: POP3 协议实现
- `SmtpClient.kt`: SMTP 协议实现
- `AccountSetupScreen.kt`: 账户配置界面
- `ComposeEmailScreen.kt`: 邮件撰写界面
- `EmailRepository.kt`: 邮件数据管理
- `EmailViewModel.kt`: UI 状态管理

**POP3 服务器**:
- `Pop3Server.java`: 服务器主类
- `Pop3CommandHandler.java`: 命令处理
- `Pop3EmailRepository.java`: 邮件查询

**SMTP 服务器**:
- `SmtpServer.java`: 服务器主类
- `SmtpCommandHandler.java`: 命令处理
- `EmailRepository.java`: 邮件存储

### 扩展功能

已实现的功能:
- ✅ SMTP 发送邮件
- ✅ 多收件人支持
- ✅ 自动重试机制

可以添加的功能:
- IMAP 协议支持
- 推送通知
- 邮件搜索
- 附件下载
- 邮件草稿保存
- 已发送邮件列表

## 参考资料

- [RFC 1939 - POP3 Protocol](https://www.rfc-editor.org/rfc/rfc1939)
- [RFC 5321 - SMTP Protocol](https://www.rfc-editor.org/rfc/rfc5321)
- [RFC 5322 - Internet Message Format](https://www.rfc-editor.org/rfc/rfc5322)
- [Android Network Security Config](https://developer.android.com/training/articles/security-config)
- [JavaMail API](https://javaee.github.io/javamail/)
