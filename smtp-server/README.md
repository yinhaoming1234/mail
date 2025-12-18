# SMTP Server - Java 25 虚拟线程版

基于 Java 25 虚拟线程实现的高性能 SMTP 邮件服务器。

## 特性

### Java 25 新特性
- **虚拟线程 (Virtual Threads)** - 每个连接使用一个虚拟线程，支持百万级并发
- **ScopedValue** - 替代 ThreadLocal，更轻量、更安全
- **结构化并发 (Structured Concurrency)** - 简化并发编程模型

### SMTP 协议实现
- 完整实现 RFC 5321 SMTP 协议
- 支持 ESMTP 扩展 (EHLO)
- 支持命令：HELO, EHLO, MAIL FROM, RCPT TO, DATA, RSET, NOOP, QUIT, VRFY, HELP
- 支持多收件人
- 透明邮件数据处理（点转义）

### 数据存储
- PostgreSQL 数据库存储邮件
- HikariCP 高性能连接池
- 支持邮件队列和发送日志

## 快速开始

### 前置条件
- Java 25 或更高版本
- PostgreSQL 数据库
- Maven 3.8+

### 配置数据库

1. 创建数据库：
```sql
CREATE DATABASE maildb;
```

2. 服务器启动时会自动执行 `schema.sql` 初始化表结构

### 配置文件

编辑 `src/main/resources/application.properties`:

```properties
# SMTP 服务器配置
smtp.domain=your-domain.com
smtp.port=2525

# 数据库配置
db.url=jdbc:postgresql://localhost:5432/maildb
db.username=postgres
db.password=your-password
```

### 编译和运行

```bash
# 编译
mvn clean package

# 运行
java --enable-preview -jar target/smtp-server-1.0-SNAPSHOT.jar
```

## 项目结构

```
smtp-server/
├── src/main/java/com/yhm/smtp/
│   ├── SmtpServerMain.java          # 启动入口
│   ├── config/
│   │   └── SmtpServerConfig.java    # 配置类
│   ├── server/
│   │   ├── SmtpServer.java          # 服务器主类
│   │   └── SmtpConnectionHandler.java # 连接处理器
│   ├── protocol/
│   │   ├── SmtpCommand.java         # SMTP命令枚举
│   │   ├── SmtpResponse.java        # SMTP响应码
│   │   ├── SmtpSession.java         # 会话状态
│   │   └── SmtpCommandHandler.java  # 命令处理器
│   ├── db/
│   │   ├── DatabaseConfig.java      # 数据库配置
│   │   ├── EmailRepository.java     # 邮件数据访问
│   │   └── DatabaseInitializer.java # 数据库初始化
│   └── util/
│       ├── StructuredMailSender.java # 结构化并发邮件发送
│       └── ScopedValueExample.java   # ScopedValue示例
├── src/main/resources/
│   ├── application.properties        # 配置文件
│   ├── schema.sql                    # 数据库脚本
│   └── logback.xml                   # 日志配置
└── src/test/java/
    └── SmtpClientTest.java           # SMTP客户端测试
```

## SMTP 协议示例

### 基本邮件发送

```
C: [连接到服务器]
S: 220 localhost ESMTP Service Ready
C: EHLO client.example.com
S: 250-localhost Hello
S: 250-SIZE 26214400
S: 250-8BITMIME
S: 250-PIPELINING
S: 250-ENHANCEDSTATUSCODES
S: 250 HELP
C: MAIL FROM:<sender@example.com>
S: 250 Sender <sender@example.com> OK
C: RCPT TO:<recipient@localhost>
S: 250 Recipient <recipient@localhost> OK
C: DATA
S: 354 Start mail input; end with <CRLF>.<CRLF>
C: From: sender@example.com
C: To: recipient@localhost
C: Subject: Test Email
C:
C: Hello, this is a test email.
C: .
S: 250 OK Message accepted for delivery
C: QUIT
S: 221 localhost Service closing transmission channel
```

## 数据库表结构

### emails 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 邮件唯一标识 |
| sender | VARCHAR | 发件人地址 |
| recipients | VARCHAR[] | 收件人列表 |
| subject | VARCHAR | 邮件主题 |
| body | TEXT | 邮件正文 |
| raw_content | TEXT | 原始邮件内容 |
| size | BIGINT | 邮件大小 |
| received_at | TIMESTAMP | 接收时间 |
| owner | VARCHAR | 所属用户 |

### users 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 用户唯一标识 |
| email | VARCHAR | 邮箱地址 |
| password_hash | VARCHAR | 密码哈希 |
| quota_bytes | BIGINT | 容量限制 |

## Java 25 特性使用示例

### 虚拟线程
```java
// 创建虚拟线程执行器
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// 每个连接在虚拟线程中处理
executor.submit(() -> handleConnection(socket));
```

### ScopedValue
```java
// 定义作用域值
static final ScopedValue<String> USER = ScopedValue.newInstance();

// 在作用域内使用
ScopedValue.where(USER, "alice@example.com").run(() -> {
    String user = USER.get(); // 获取值
    processRequest();
});
```

### 结构化并发
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    // 并行发送到多个收件人
    List<Subtask<Result>> tasks = recipients.stream()
        .map(r -> scope.fork(() -> send(r)))
        .toList();
    
    scope.join(); // 等待所有完成
}
```

## 测试

运行测试客户端：
```bash
java -cp target/smtp-server-1.0-SNAPSHOT.jar \
    com.yhm.smtp.SmtpClientTest localhost 2525
```

使用 telnet 测试：
```bash
telnet localhost 2525
```

## 许可证

MIT License

