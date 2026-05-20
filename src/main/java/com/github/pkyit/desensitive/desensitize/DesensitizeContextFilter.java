package com.github.pkyit.desensitive.desensitize;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * {@link ThreadLocal} 清理过滤器。
 * <p>
 * Web 服务器（Tomcat）使用线程池处理请求，请求处理完毕后线程归还到池中。
 * 如果不清理 ThreadLocal，下一个复用到该线程的请求会读到上一次残留的脱敏状态，
 * 造成"状态串扰" —— 一个本应脱敏的请求返回了明文。
 * </p>
 *
 * <h3>执行顺序保证</h3>
 * 本过滤器通过 {@code @WebFilter(urlPatterns = "/*")} 拦截所有请求，
 * 在 {@code finally} 块中确保清除，无论请求是否成功或抛出异常。
 * 
 * <b>注意</b>：Spring MVC 拦截器的 postHandle 和 afterCompletion 在此 Filter
 * 的作用域内执行，因此 ThreadLocal 会在整个请求处理完成后才被清理。
 */
@WebFilter(urlPatterns = "/*")
public class DesensitizeContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            DesensitizeContext.clear();
        }
    }

}
