package com.github.pkyit.desensitive.config;

import com.github.pkyit.desensitive.desensitize.DesensitizeContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 脱敏配置初始化 —— 将 application.yaml 中的 {@code desensitize.enabled} 配置
 * 加载到 {@link DesensitizeContext} 的全局开关中。
 * <p>
 * 使用 {@code @Value} 读取配置文件，{@code @PostConstruct} 在 Bean 初始化后
 * 同步到静态变量。后续用户可通过 API 动态修改，但重启后会恢复到配置值。
 * </p>
 */
@Configuration
public class DesensitizeConfig {

    /**
     * 从配置文件读取脱敏开关，默认 true（开启脱敏）。
     * 对应 application.yaml 中的：
     * <pre>
     * desensitize:
     *   enabled: true
     * </pre>
     */
    @Value("${desensitize.enabled:true}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        DesensitizeContext.setGlobalEnabled(enabled);
    }

}
