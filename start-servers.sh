#!/bin/bash

# 邮件系统启动脚本
# 使用方法: ./start-servers.sh [component]
# component: all, db, smtp, pop3, admin

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 服务器IP配置
SERVER_IP="192.168.1.106"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       邮件收发系统启动脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 切换并检查Java版本
check_java() {
    echo -e "${YELLOW}切换到Java 25...${NC}"
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk use java 25.0.1-amzn
    
    echo -e "${YELLOW}检查Java环境...${NC}"
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        echo -e "Java版本: ${GREEN}$JAVA_VERSION${NC}"
    else
        echo -e "${RED}错误: 未找到Java，请安装Java 25${NC}"
        exit 1
    fi
}

# 启动PostgreSQL数据库
start_db() {
    echo -e "${YELLOW}启动PostgreSQL数据库...${NC}"
    docker-compose up -d postgres
    echo -e "${GREEN}数据库已启动${NC}"
    sleep 3
}

# 启动SMTP服务器
start_smtp() {
    echo -e "${YELLOW}启动SMTP服务器 (端口: 2525)...${NC}"
    cd smtp-server
    MAVEN_OPTS="--enable-preview" mvn exec:java -Dexec.mainClass="com.yhm.smtp.SmtpServerMain" &
    cd ..
    echo -e "${GREEN}SMTP服务器已启动${NC}"
}

# 启动POP3服务器
start_pop3() {
    echo -e "${YELLOW}启动POP3服务器 (端口: 1100)...${NC}"
    cd pop3-server
    MAVEN_OPTS="--enable-preview" mvn exec:java -Dexec.mainClass="com.yhm.pop3.Pop3ServerMain" &
    cd ..
    echo -e "${GREEN}POP3服务器已启动${NC}"
}

# 启动管理后台
start_admin() {
    echo -e "${YELLOW}启动管理后台 (端口: 8000)...${NC}"
    cd admin-web
    mvn spring-boot:run &
    cd ..
    echo -e "${GREEN}管理后台已启动${NC}"
}

# 显示服务状态
show_status() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}       服务启动完成${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "服务器IP: ${YELLOW}$SERVER_IP${NC}"
    echo ""
    echo -e "服务端口:"
    echo -e "  - PostgreSQL: ${YELLOW}5432${NC}"
    echo -e "  - SMTP服务器: ${YELLOW}2525${NC}"
    echo -e "  - POP3服务器: ${YELLOW}1100${NC}"
    echo -e "  - 管理后台:   ${YELLOW}8000${NC}"
    echo ""
    echo -e "管理后台地址: ${YELLOW}http://$SERVER_IP:8000${NC}"
    echo -e "默认管理员账号: ${YELLOW}admin / admin123${NC}"
    echo ""
    echo -e "测试邮箱账号:"
    echo -e "  - test@localhost / password123"
    echo -e "  - admin@localhost / admin123"
    echo -e "  - user@example.com / user123"
    echo ""
}

# 主函数
main() {
    check_java
    
    case "${1:-all}" in
        db)
            start_db
            ;;
        smtp)
            start_smtp
            ;;
        pop3)
            start_pop3
            ;;
        admin)
            start_admin
            ;;
        all)
            start_db
            sleep 2
            start_smtp
            sleep 2
            start_pop3
            sleep 2
            start_admin
            show_status
            ;;
        *)
            echo "用法: $0 [all|db|smtp|pop3|admin]"
            exit 1
            ;;
    esac
}

main "$@"
