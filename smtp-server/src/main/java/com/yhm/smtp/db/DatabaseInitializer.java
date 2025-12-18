package com.yhm.smtp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库初始化器
 * 负责执行数据库初始化脚本
 */
public class DatabaseInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    /**
     * 初始化数据库
     * 执行 schema.sql 脚本创建表结构
     */
    public static void initialize() {
        log.info("开始初始化数据库...");
        
        try (InputStream is = DatabaseInitializer.class.getClassLoader()
                .getResourceAsStream("schema.sql")) {
            
            if (is == null) {
                log.warn("未找到 schema.sql 文件，跳过数据库初始化");
                return;
            }
            
            String schema;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                schema = reader.lines().collect(Collectors.joining("\n"));
            }
            
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // 使用智能分割器处理 $$...$$ 块
                var statements = splitSqlStatements(schema);
                
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty() && !sql.startsWith("--")) {
                        try {
                            stmt.execute(sql);
                            log.debug("执行 SQL: {}", truncate(sql, 100));
                        } catch (SQLException e) {
                            // 忽略某些错误（如表已存在）
                            if (!isIgnorableError(e)) {
                                throw e;
                            }
                            log.debug("忽略 SQL 错误: {}", e.getMessage());
                        }
                    }
                }
                log.info("数据库初始化完成");
            }
            
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
    
    /**
     * 智能分割 SQL 语句，正确处理 PostgreSQL 的 $$...$$ 函数定义块
     */
    private static List<String> splitSqlStatements(String schema) {
        var statements = new ArrayList<String>();
        var current = new StringBuilder();
        boolean inDollarQuote = false;
        
        int i = 0;
        while (i < schema.length()) {
            // 检查 $$ 标记
            if (i + 1 < schema.length() && schema.charAt(i) == '$' && schema.charAt(i + 1) == '$') {
                current.append("$$");
                inDollarQuote = !inDollarQuote;
                i += 2;
                continue;
            }
            
            char c = schema.charAt(i);
            
            // 只有不在 $$ 块内时，分号才是语句分隔符
            if (c == ';' && !inDollarQuote) {
                String sql = current.toString().trim();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
            i++;
        }
        
        // 添加最后一条语句
        String lastSql = current.toString().trim();
        if (!lastSql.isEmpty()) {
            statements.add(lastSql);
        }
        
        return statements;
    }
    
    /**
     * 检查是否是可忽略的错误
     */
    private static boolean isIgnorableError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("already exists") ||
               message.contains("duplicate") ||
               message.contains("relation") && message.contains("exists");
    }
    
    /**
     * 截断字符串
     */
    private static String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str.replace("\n", " ");
        }
        return str.substring(0, maxLength).replace("\n", " ") + "...";
    }
}

