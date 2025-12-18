# Requirements Document

## Introduction

本文档定义了邮件收发系统的完善需求，系统由三个子系统组成：服务器管理模块（Admin Web）、邮件传输模块（SMTP/POP3 服务器）和移动端 Android 客户端。目标是实现一个实际可用的邮件系统，支持前后端通信，客户端真机调试，服务端 IP 设为 192.168.1.106。

## Glossary

- **Mail_System**: 完整的邮件收发系统，包含服务器端和客户端
- **Admin_Web**: 基于 Spring Boot 的服务器管理后台
- **SMTP_Server**: 简单邮件传输协议服务器，负责发送邮件
- **POP3_Server**: 邮局协议服务器，负责接收邮件
- **Android_Client**: 移动端邮件客户端应用
- **Admin**: 系统管理员，拥有完整管理权限
- **User**: 普通用户，拥有邮箱使用权限
- **Mail_Domain**: 邮件域名，如 test.com
- **Mail_Queue**: 邮件发送队列
- **Delivery_Log**: 邮件投递日志

## Requirements

### Requirement 1: 用户注册功能

**User Story:** As a new user, I want to register an account through the Android client, so that I can use the email service.

#### Acceptance Criteria

1. WHEN a user submits registration with valid email, username and password THEN the Android_Client SHALL send registration request to Admin_Web and display success message upon completion
2. WHEN a user submits registration with an email that already exists THEN the Android_Client SHALL display an error message indicating the email is already registered
3. WHEN a user submits registration with invalid email format THEN the Android_Client SHALL prevent submission and display validation error
4. WHEN a user submits registration with password shorter than 6 characters THEN the Android_Client SHALL prevent submission and display password requirement message
5. WHEN registration succeeds THEN the Admin_Web SHALL create the user record in database with default quota settings

### Requirement 2: 用户登录功能

**User Story:** As a registered user, I want to log in to the Android client, so that I can access my mailbox.

#### Acceptance Criteria

1. WHEN a user enters valid credentials and taps login THEN the Android_Client SHALL authenticate against SMTP_Server or POP3_Server and navigate to mail list screen
2. WHEN a user enters invalid credentials THEN the Android_Client SHALL display authentication failure message and remain on login screen
3. WHEN a user account is disabled THEN the Android_Client SHALL display account disabled message and prevent login
4. WHEN login succeeds THEN the Android_Client SHALL store credentials securely for subsequent email operations

### Requirement 3: 邮件发送功能

**User Story:** As a user, I want to send emails through the Android client, so that I can communicate with others.

#### Acceptance Criteria

1. WHEN a user composes an email with valid recipient, subject and body and taps send THEN the Android_Client SHALL transmit the email via SMTP_Server to port 2525 on 192.168.1.106
2. WHEN SMTP_Server receives a valid email THEN the SMTP_Server SHALL store the email in database and return success response code 250
3. WHEN a user sends email to multiple recipients (comma-separated) THEN the SMTP_Server SHALL deliver to all valid recipients
4. WHEN a user sends email to invalid recipient address THEN the SMTP_Server SHALL return error response and Android_Client SHALL display delivery failure message
5. WHEN email sending fails due to network error THEN the Android_Client SHALL retry up to 3 times with exponential backoff

### Requirement 4: 邮件接收功能

**User Story:** As a user, I want to receive emails through the Android client, so that I can read messages sent to me.

#### Acceptance Criteria

1. WHEN a user taps refresh on mail list THEN the Android_Client SHALL connect to POP3_Server on port 1100 at 192.168.1.106 and sync new emails
2. WHEN POP3_Server receives LIST command THEN the POP3_Server SHALL return list of emails with message numbers and sizes
3. WHEN POP3_Server receives RETR command THEN the POP3_Server SHALL return complete email content including headers and body
4. WHEN Android_Client receives emails THEN the Android_Client SHALL parse email content and store in local database
5. WHEN email sync completes THEN the Android_Client SHALL display updated mail list with sender, subject and received time

### Requirement 5: 邮件删除功能

**User Story:** As a user, I want to delete emails, so that I can manage my mailbox storage.

#### Acceptance Criteria

1. WHEN a user selects an email and taps delete THEN the Android_Client SHALL send DELE command to POP3_Server
2. WHEN POP3_Server receives DELE command THEN the POP3_Server SHALL mark the email for deletion and return success response
3. WHEN user session ends with QUIT command THEN the POP3_Server SHALL permanently remove marked emails from database
4. WHEN email is deleted from server THEN the Android_Client SHALL remove the email from local database and update display

### Requirement 6: 管理员用户管理

**User Story:** As an administrator, I want to manage user accounts through Admin_Web, so that I can control system access.

#### Acceptance Criteria

1. WHEN an admin creates a new user with email, password and quota THEN the Admin_Web SHALL insert user record into database and display success notification
2. WHEN an admin disables a user account THEN the Admin_Web SHALL update user status and prevent that user from logging in
3. WHEN an admin enables a disabled user account THEN the Admin_Web SHALL restore user login capability
4. WHEN an admin deletes a user account THEN the Admin_Web SHALL remove user record and associated emails from database
5. WHEN an admin resets user password THEN the Admin_Web SHALL update password hash and notify completion

### Requirement 7: 管理员域名管理

**User Story:** As an administrator, I want to manage mail domains, so that I can configure which domains the system handles.

#### Acceptance Criteria

1. WHEN an admin adds a new domain THEN the Admin_Web SHALL create domain record with active status
2. WHEN an admin disables a domain THEN the Admin_Web SHALL update domain status and SMTP_Server SHALL reject emails for that domain
3. WHEN an admin enables a domain THEN the Admin_Web SHALL restore domain to active status
4. WHEN an admin deletes a domain THEN the Admin_Web SHALL remove domain record after confirming no users exist under that domain

### Requirement 8: 邮件群发功能

**User Story:** As an administrator, I want to send bulk emails, so that I can efficiently distribute notifications to multiple users.

#### Acceptance Criteria

1. WHEN an admin composes bulk email and selects recipient group THEN the Admin_Web SHALL queue emails for all selected recipients
2. WHEN bulk email is queued THEN the SMTP_Server SHALL process queue and deliver to each recipient
3. WHEN bulk email delivery completes THEN the Admin_Web SHALL display delivery statistics showing success and failure counts

### Requirement 9: 服务器配置管理

**User Story:** As an administrator, I want to configure server parameters, so that I can customize system behavior.

#### Acceptance Criteria

1. WHEN an admin updates SMTP port setting THEN the Admin_Web SHALL store new port value (default 25, current 2525)
2. WHEN an admin updates POP3 port setting THEN the Admin_Web SHALL store new port value (default 110, current 1100)
3. WHEN an admin updates server domain setting THEN the Admin_Web SHALL store new domain value (default test.com)
4. WHEN an admin updates mailbox size limit THEN the Admin_Web SHALL apply new quota to specified users

### Requirement 10: 日志管理功能

**User Story:** As an administrator, I want to view and manage server logs, so that I can monitor system activity and troubleshoot issues.

#### Acceptance Criteria

1. WHEN an admin views SMTP logs THEN the Admin_Web SHALL display log entries with timestamp, sender, recipient and status
2. WHEN an admin views POP3 logs THEN the Admin_Web SHALL display log entries with timestamp, user and command
3. WHEN an admin clears logs THEN the Admin_Web SHALL remove log entries older than specified date
4. WHEN an admin exports logs THEN the Admin_Web SHALL generate downloadable log file

### Requirement 11: 邮件队列管理

**User Story:** As an administrator, I want to manage the mail queue, so that I can handle delivery issues.

#### Acceptance Criteria

1. WHEN an admin views mail queue THEN the Admin_Web SHALL display pending emails with status, retry count and error message
2. WHEN an admin retries a failed email THEN the Admin_Web SHALL reset retry count and requeue for delivery
3. WHEN an admin cancels a queued email THEN the Admin_Web SHALL remove email from queue and update status to cancelled

### Requirement 12: 用户密码修改

**User Story:** As a user, I want to change my password through the Android client, so that I can maintain account security.

#### Acceptance Criteria

1. WHEN a user enters current password and new password and taps save THEN the Android_Client SHALL send password change request to Admin_Web
2. WHEN current password verification succeeds THEN the Admin_Web SHALL update password hash and return success response
3. WHEN current password verification fails THEN the Admin_Web SHALL return error and Android_Client SHALL display incorrect password message

### Requirement 13: 网络连接配置

**User Story:** As a user, I want to configure server connection settings, so that I can connect to the mail server.

#### Acceptance Criteria

1. WHEN a user configures account with server IP 192.168.1.106 THEN the Android_Client SHALL store connection settings for SMTP and POP3
2. WHEN a user taps test connection THEN the Android_Client SHALL attempt connection to both SMTP_Server and POP3_Server and display results
3. WHEN connection test fails THEN the Android_Client SHALL display specific error message indicating which server failed

### Requirement 14: 邮件内容解析

**User Story:** As a user, I want to view email content properly formatted, so that I can read messages clearly.

#### Acceptance Criteria

1. WHEN Android_Client receives email with plain text body THEN the Android_Client SHALL display text content with proper line breaks
2. WHEN Android_Client receives email with HTML body THEN the Android_Client SHALL render HTML content in WebView
3. WHEN Android_Client receives multipart email THEN the Android_Client SHALL parse MIME structure and display appropriate content type
4. WHEN email parsing succeeds THEN the Android_Client SHALL extract and display From, To, Subject, Date headers correctly

### Requirement 15: 邮件协议解析与序列化

**User Story:** As a developer, I want email parsing and serialization to be reliable, so that email content is preserved accurately.

#### Acceptance Criteria

1. WHEN SMTP_Server receives email data THEN the SMTP_Server SHALL parse RFC 5322 format headers and body correctly
2. WHEN POP3_Server sends email via RETR command THEN the POP3_Server SHALL serialize email in RFC 5322 format
3. WHEN email is parsed then serialized THEN the Mail_System SHALL produce equivalent output preserving all headers and body content (round-trip consistency)
4. WHEN email contains special characters or encoding THEN the Mail_System SHALL handle character encoding correctly
