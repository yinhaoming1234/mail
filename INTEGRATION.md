# 邮件收发系统集成指南

## 概述

本文档说明如何部署和使用完整的邮件收发系统，包括：
- **服务器管理模块** (admin-web): 用户管理、域名管理、群发邮件
- **邮件传输模块**: SMTP服务器(发信)、POP3服务器(收信)
- **Android客户端**: 邮件收发、用户注册、密码修改

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    服务器端 (localhost)                       │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ SMTP Server │  │ POP3 Server │  │   Admin Web (8000)  │  │
│  │   (2525)    │  │   (1100)    │  │  - 用户管理         │  │
│  │  发送邮件   │  │  接收邮件   │  │  - 授权/消权        │  │
│  └──────┬──────┘  └──────┬──────┘  │  - 群发邮件         │  │
│         │                │         │  - API接口          │  │
│         └────────┬───────┘         └──────────┬──────────┘  │
│                  │                            │              │
│         ┌────────▼────────────────────────────▼────────┐    │
│         │           PostgreSQL (5432)                   │    │
│         │  - users (用户表, 含is_admin字段)             │    │
│         │  - emails (邮件表)                            │    │
│         │  - mail_domains (域名表)                      │    │
│         └───────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 网络通信
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Android 客户端                            │
├─────────────────────────────────────────────────────────────┤
│  - 用户登录/注册 (调用 /api/users/*)                        │
│  - 邮件收发 (SMTP/POP3)                                     │
│  - 密码修改 (调用 /api/users/change-password)               │
└─────────────────────────────────────────────────────────────┘
```

## 前置条件

- Java 25+ (服务器端)
- Java 17 (Android客户端)
- Maven 3.8+
- PostgreSQL 16+
- Docker (可选，用于数据库)
- Android Studio
- Android 设备 (API 26+)

## 快速启动

### 使用启动脚本 (推荐)

```bash
# 赋予执行权限
chmod +x start-servers.sh

# 启动所有服务
./start-servers.sh all

# 或单独启动某个服务
./start-servers.sh db      # 只启动数据库
./start-servers.sh smtp    # 只启动SMTP服务器
./start-servers.sh pop3    # 只启动POP3服务器
./start-servers.sh admin   # 只启动管理后台
```

### 手动启动

#### 1. 启动数据库

```bash
# 使用Docker启动PostgreSQL
docker-compose up -d postgres

# 等待数据库就绪
sleep 5
```

#### 2. 启动SMTP服务器

```bash
cd smtp-server
mvn clean package -DskipTests
java --enable-preview -jar target/smtp-server-1.0-SNAPSHOT.jar
```

#### 3. 启动POP3服务器

```bash
cd pop3-server
mvn clean package -DskipTests
java --enable-preview -jar target/pop3-server-1.0-SNAPSHOT.jar
```

#### 4. 启动管理后台

```bash
cd admin-web
mvn spring-boot:run
```

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL | 5432 | 数据库 |
| SMTP | 2525 | 发送邮件 |
| POP3 | 1100 | 接收邮件 |
| Admin Web | 8000 | 管理后台 |

### 测试账号

| 邮箱 | 密码 | 角色 |
|------|------|------|
| admin@localhost | admin123 | 管理员 |
| test@localhost | password123 | 普通用户 |
| user@example.com | user123 | 普通用户 |

## 管理后台功能

访问地址: `http://localhost:8000` (服务器本地) 或 `http://10.0.2.2:8000` (Android模拟器)

### 用户管理

1. **创建用户**: 点击"添加用户"，填写用户名、域名、密码
2. **授权管理员**: 点击用户行的权限图标，授予/撤销管理员权限
3. **禁用用户**: 点击眼睛图标切换用户启用状态
4. **重置密码**: 点击锁图标重置用户密码

### 群发邮件

1. 点击左侧菜单"群发邮件"
2. 选择目标域名（可选）或勾选"仅发送给管理员"
3. 填写邮件主题和内容
4. 点击"预览收件人"查看将发送给多少用户
5. 点击"发送群发邮件"

### API接口

管理后台提供REST API供Android客户端调用：

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/users/register | POST | 用户注册 |
| /api/users/login | POST | 用户登录验证 |
| /api/users/change-password | POST | 修改密码 |
| /api/domains | GET | 获取可用域名列表 |
| /api/users/check-email | GET | 检查邮箱是否可用 |

## Android 客户端配置

### 服务器地址配置

服务器IP地址配置在 `ServerConfig.kt` 文件中：

```kotlin
// mailclient/app/src/main/java/com/yhm/mail_client/data/network/ServerConfig.kt
object ServerConfig {
    // Android模拟器使用10.0.2.2访问宿主机localhost
    const val SERVER_IP = "10.0.2.2"
    const val API_BASE_URL = "http://$SERVER_IP:8000"
    const val SMTP_HOST = SERVER_IP
    const val SMTP_PORT = 2525
    const val POP3_HOST = SERVER_IP
    const val POP3_PORT = 1100
}
```

### 用户注册

1. 打开应用，在登录页点击"立即注册"
2. 输入用户名（将作为邮箱前缀）
3. 选择域名（从服务器获取可用域名列表）
4. 设置密码（至少6个字符）
5. 确认密码
6. 点击"注册"按钮
7. 注册成功后自动跳转到登录页

### 用户登录

1. 输入完整邮箱地址（如 test@localhost）
2. 输入密码
3. 点击"登录"按钮
4. 登录成功后自动配置SMTP/POP3服务器并进入邮件列表

### 修改密码

1. 进入"设置"页面
2. 点击"修改密码"
3. 输入当前密码
4. 输入新密码（至少6个字符）
5. 确认新密码
6. 点击"确认修改"

### 模拟器调试配置（推荐）

1. **使用Android模拟器**，SERVER_IP已配置为 `10.0.2.2`
2. **10.0.2.2是Android模拟器访问宿主机localhost的特殊地址**
3. **网络安全配置已允许10.0.2.2的明文流量**

### 真机调试配置

如需使用真机调试：
1. **确保手机和电脑在同一局域网**
2. **修改ServerConfig.kt中的SERVER_IP为电脑局域网IP**（如192.168.1.106）
3. **在网络安全配置中添加该IP地址**
```

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

### 模拟器访问本地服务器（当前配置）

**服务器端配置**：
- 所有服务器（SMTP、POP3、Admin-Web）均监听 `localhost` (127.0.0.1)
- 数据库连接使用 `localhost:5432`

**客户端配置**：
- Android客户端使用 `10.0.2.2` 连接服务器
- 10.0.2.2 是Android模拟器访问宿主机localhost的特殊地址
- 无需修改任何配置，开箱即用

### 真机访问本地服务器

如需使用真机测试，需要进行以下配置：

1. **获取电脑的局域网IP**：
   ```bash
   # Windows
   ipconfig
   # 查找 IPv4 地址，例如: 192.168.1.106

   # Linux/Mac
   ifconfig
   # 或 ip addr
   ```

2. **修改服务器配置监听所有接口**（可选）：
   编辑服务器配置文件，将 `localhost` 改为 `0.0.0.0`
   ```properties
   # pop3-server/src/main/resources/application.properties
   pop3.domain=0.0.0.0
   
   # smtp-server/src/main/resources/application.properties  
   smtp.domain=0.0.0.0
   ```

3. **修改客户端配置**：
   编辑 `ServerConfig.kt`，将 `SERVER_IP` 改为电脑的局域网IP
   ```kotlin
   const val SERVER_IP = "192.168.1.106"  // 改为你的电脑IP
   ```

4. **更新网络安全配置**：
   在 `network_security_config.xml` 中添加你的局域网IP

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
