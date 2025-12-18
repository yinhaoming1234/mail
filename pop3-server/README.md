# POP3 邮件服务器

基于 Java 25 实现的完整 POP3 邮件服务器，遵循 RFC 1939 协议规范。

## 特性

### Java 25 新特性

- **虚拟线程 (Virtual Threads)** - 轻量级线程，支持高并发连接
- **ScopedValue** - 比 ThreadLocal 更轻量的上下文传递机制
- **结构化并发 (Structured Concurrency)** - 管理并发任务生命周期
- **Record 类型** - 不可变数据载体
- **增强的 switch 表达式** - 更简洁的分支处理
- **模式匹配** - 简化类型检查和转换

### POP3 协议实现

完整实现 RFC 1939 定义的 POP3 协议：

#### 授权状态命令
- `USER` - 指定用户名
- `PASS` - 指定密码
- `APOP` - 摘要认证（可选）
- `QUIT` - 退出

#### 事务状态命令
- `STAT` - 获取邮箱状态（邮件数量和总大小）
- `LIST` - 列出邮件
- `RETR` - 获取邮件内容
- `DELE` - 标记删除邮件
- `NOOP` - 空操作
- `RSET` - 重置删除标记
- `TOP` - 获取邮件头和部分正文
- `UIDL` - 获取邮件唯一标识符
- `QUIT` - 退出并提交删除

#### 扩展命令
- `CAPA` - 获取服务器能力

## 快速开始

### 前置条件

- Java 25+ 
- Maven 3.8+
- PostgreSQL 12+

### 构建

```bash
cd pop3-server
mvn clean package
```

### 运行

```bash
java --enable-preview -jar target/pop3-server-1.0-SNAPSHOT.jar
```

或者使用 Maven：

```bash
mvn exec:java -Dexec.mainClass="com.yhm.pop3.Pop3ServerMain"
```

### 配置

编辑 `src/main/resources/application.properties`：

```properties
# POP3 服务器配置
pop3.domain=localhost
pop3.port=1100
pop3.maxConnections=500
pop3.readTimeout=600000

# 数据库配置
db.url=jdbc:postgresql://localhost:5432/maildb
db.username=postgres
db.password=postgres
```

## 测试

使用 telnet 测试 POP3 服务器：

```bash
telnet localhost 1100
```

示例会话：

```
+OK localhost POP3 server ready <12345.1234567890@localhost>
USER test@localhost
+OK user accepted
PASS password123
+OK maildrop has 5 messages (12345 octets)
STAT
+OK 5 12345
LIST
+OK 5 messages (12345 octets)
1 1234
2 2345
3 3456
4 2123
5 3187
.
RETR 1
+OK 1234 octets
From: sender@example.com
To: test@localhost
Subject: Test Email

This is a test email.
.
QUIT
+OK localhost POP3 server signing off (0 messages deleted)
```

## 项目结构

```
pop3-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/yhm/pop3/
│   │   │       ├── Pop3ServerMain.java          # 启动入口
│   │   │       ├── config/
│   │   │       │   └── Pop3ServerConfig.java    # 配置类
│   │   │       ├── db/
│   │   │       │   ├── DatabaseConfig.java      # 数据库配置
│   │   │       │   ├── DatabaseInitializer.java # 数据库初始化
│   │   │       │   └── Pop3EmailRepository.java # 邮件仓库
│   │   │       ├── protocol/
│   │   │       │   ├── Pop3Command.java         # 命令枚举
│   │   │       │   ├── Pop3CommandHandler.java  # 命令处理器
│   │   │       │   ├── Pop3Response.java        # 响应生成器
│   │   │       │   └── Pop3Session.java         # 会话状态
│   │   │       └── server/
│   │   │           ├── Pop3Server.java          # 服务器主类
│   │   │           └── Pop3ConnectionHandler.java # 连接处理器
│   │   └── resources/
│   │       ├── application.properties           # 配置文件
│   │       ├── logback.xml                      # 日志配置
│   │       └── schema.sql                       # 数据库架构
│   └── test/
│       └── java/
└── pom.xml
```

## 架构说明

### 虚拟线程

使用 Java 25 的虚拟线程处理每个客户端连接：

```java
ThreadFactory virtualThreadFactory = Thread.ofVirtual()
        .name("pop3-handler-", 0)
        .factory();

ExecutorService executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
```

虚拟线程的优势：
- 轻量级，可以创建数百万个
- 适合 I/O 密集型任务
- 自动挂起和恢复

### ScopedValue

使用 ScopedValue 传递请求上下文：

```java
public static final ScopedValue<Pop3ServerConfig> SERVER_CONFIG = ScopedValue.newInstance();

ScopedValue.where(SERVER_CONFIG, config).run(() -> {
    startServer(config);
});
```

### 会话状态机

POP3 会话遵循状态机模式：

```
AUTHORIZATION --> TRANSACTION --> UPDATE
      │                │
      └── QUIT ────────┴── QUIT
```

## 安全注意事项

1. **密码存储** - 当前使用明文存储密码，生产环境应使用 BCrypt 等安全哈希
2. **TLS 加密** - 建议在生产环境启用 TLS（STLS 命令）
3. **认证限制** - 默认 3 次认证失败后断开连接
4. **邮箱锁定** - 同一用户同一时间只能有一个活跃会话

## 许可证

MIT License

