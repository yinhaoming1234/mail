package com.yhm.adminweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 * 配置认证和授权规则
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 授权配置
            .authorizeHttpRequests(auth -> auth
                // 静态资源允许匿名访问
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // 登录页允许匿名访问
                .requestMatchers("/login", "/error").permitAll()
                // Actuator 健康检查允许匿名
                .requestMatchers("/actuator/health").permitAll()
                // API 接口允许匿名访问（供移动客户端使用）
                .requestMatchers("/api/**").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            // 表单登录配置
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // 登出配置
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // 记住我功能
            .rememberMe(remember -> remember
                .key("mail-admin-remember-key")
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7天
            )
            // 会话管理
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            )
            // 完全禁用 CSRF 保护
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 用户详情服务 - 内存存储管理员账户
     * 生产环境建议改为数据库存储
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails operator = User.builder()
                .username("operator")
                .password(passwordEncoder.encode("operator123"))
                .roles("OPERATOR")
                .build();

        return new InMemoryUserDetailsManager(admin, operator);
    }
}

