package com.yhm.smtp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Java 25 ScopedValue 示例
 * 
 * ScopedValue 是 Java 21+ 引入的新特性，用于替代 ThreadLocal
 * 主要优点：
 * 1. 不可变 - 绑定后值不能被修改
 * 2. 自动清理 - 作用域结束后自动清除
 * 3. 继承性 - 子任务自动继承父任务的 ScopedValue
 * 4. 轻量级 - 特别适合虚拟线程
 */
public class ScopedValueExample {
    
    private static final Logger log = LoggerFactory.getLogger(ScopedValueExample.class);
    
    /**
     * 定义作用域值 - 用于传递当前用户信息
     */
    public static final ScopedValue<String> CURRENT_USER = ScopedValue.newInstance();
    
    /**
     * 定义作用域值 - 用于传递请求 ID
     */
    public static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    
    /**
     * 定义作用域值 - 用于传递客户端 IP
     */
    public static final ScopedValue<String> CLIENT_IP = ScopedValue.newInstance();
    
    /**
     * 演示 ScopedValue 的基本用法
     */
    public static void demonstrateBasicUsage() {
        log.info("=== ScopedValue 基本用法演示 ===");
        
        // 使用 ScopedValue.where() 绑定值，并在 run() 中访问
        ScopedValue.where(CURRENT_USER, "alice@example.com")
                .where(REQUEST_ID, "req-12345")
                .run(() -> {
                    log.info("当前用户: {}", CURRENT_USER.get());
                    log.info("请求ID: {}", REQUEST_ID.get());
                    
                    // 嵌套调用也能访问
                    processRequest();
                });
        
        // 作用域外无法访问
        log.info("作用域外 - 是否绑定: {}", CURRENT_USER.isBound());
    }
    
    /**
     * 演示 ScopedValue 与虚拟线程的结合
     */
    public static void demonstrateWithVirtualThreads() {
        log.info("=== ScopedValue 与虚拟线程演示 ===");
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // 在不同的虚拟线程中使用不同的 ScopedValue
            for (int i = 0; i < 5; i++) {
                final int userId = i;
                
                executor.submit(() -> {
                    ScopedValue.where(CURRENT_USER, "user" + userId + "@example.com")
                            .where(REQUEST_ID, "req-" + System.nanoTime())
                            .run(() -> {
                                log.info("[{}] 处理请求 - 用户: {}, 请求ID: {}",
                                        Thread.currentThread().getName(),
                                        CURRENT_USER.get(),
                                        REQUEST_ID.get());
                                
                                // 模拟处理
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                
                                log.info("[{}] 处理完成 - 用户: {}",
                                        Thread.currentThread().getName(),
                                        CURRENT_USER.get());
                            });
                });
            }
        }
    }
    
    /**
     * 演示 ScopedValue 的继承特性
     */
    public static void demonstrateInheritance() {
        log.info("=== ScopedValue 继承特性演示 ===");
        
        ScopedValue.where(CLIENT_IP, "192.168.1.100")
                .where(CURRENT_USER, "admin@example.com")
                .run(() -> {
                    log.info("父作用域 - 用户: {}, IP: {}", 
                            CURRENT_USER.get(), CLIENT_IP.get());
                    
                    // 子作用域可以覆盖某些值
                    ScopedValue.where(CURRENT_USER, "system@internal")
                            .run(() -> {
                                // CURRENT_USER 被覆盖，但 CLIENT_IP 继承
                                log.info("子作用域 - 用户: {}, IP: {}", 
                                        CURRENT_USER.get(), CLIENT_IP.get());
                            });
                    
                    // 回到父作用域
                    log.info("父作用域（恢复） - 用户: {}, IP: {}", 
                            CURRENT_USER.get(), CLIENT_IP.get());
                });
    }
    
    /**
     * 演示使用 ScopedValue.call() 返回值
     */
    public static void demonstrateWithReturnValue() {
        log.info("=== ScopedValue 返回值演示 ===");
        
        String result = ScopedValue.where(CURRENT_USER, "test@example.com")
                .call(() -> {
                    return "处理结果来自用户: " + CURRENT_USER.get();
                });
        
        log.info("返回结果: {}", result);
    }
    
    /**
     * 模拟请求处理方法
     */
    private static void processRequest() {
        log.info("processRequest() - 用户: {}", CURRENT_USER.get());
        
        // 调用其他方法，ScopedValue 自动传递
        validateUser();
        saveData();
    }
    
    private static void validateUser() {
        log.info("validateUser() - 验证用户: {}", CURRENT_USER.get());
    }
    
    private static void saveData() {
        log.info("saveData() - 保存数据 - 请求ID: {}", REQUEST_ID.get());
    }
    
    public static void main(String[] args) {
        demonstrateBasicUsage();
        System.out.println();
        
        demonstrateWithVirtualThreads();
        System.out.println();
        
        demonstrateInheritance();
        System.out.println();
        
        demonstrateWithReturnValue();
    }
}

