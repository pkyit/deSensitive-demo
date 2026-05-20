package com.github.pkyit.desensitive.config;

import com.github.pkyit.desensitive.desensitize.DesensitizeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类。
 * <p>
 * 注册 {@link DesensitizeInterceptor} 到拦截器链。
 * 拦截器不限定 path（默认拦截所有请求），因为 ThreadLocal 的 set/clear
 * 成本极低，无需缩小匹配范围。
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DesensitizeInterceptor());
    }

}
