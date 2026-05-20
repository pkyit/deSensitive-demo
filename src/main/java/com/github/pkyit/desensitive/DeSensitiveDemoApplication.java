package com.github.pkyit.desensitive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * Spring Boot 启动类。
 *
 * @ServletComponentScan 用于扫描 @WebFilter 注解，注册 DesensitizeContextFilter，
 * 确保每个请求结束时清理 ThreadLocal，防止内存泄漏。
 */
@SpringBootApplication
@ServletComponentScan
public class DeSensitiveDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeSensitiveDemoApplication.class, args);
    }

}
