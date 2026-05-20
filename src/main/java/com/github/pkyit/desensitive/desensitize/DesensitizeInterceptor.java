package com.github.pkyit.desensitive.desensitize;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 脱敏开关拦截器 —— 实现"每个请求独立控制"的入口。
 * <p>
 * 在请求到达 Controller 之前，从请求头或请求参数中解析脱敏开关，
 * 写入 {@link DesensitizeContext} 的 ThreadLocal 中，
 * 后续 {@link DesensitizeAdvice} 据此决定是否脱敏。
 * </p>
 *
 * <h3>支持的请求控制方式</h3>
 * <table border="1">
 *   <tr><th>方式</th><th>示例</th><th>优先级</th></tr>
 *   <tr><td>请求头</td><td>{@code X-Desensitize: false}</td><td>高</td></tr>
 *   <tr><td>请求参数</td><td>{@code ?_desensitize=false}</td><td>低</td></tr>
 * </table>
 *
 * <h3>设计决策</h3>
 * 为什么同时支持 Header 和 Param？<br>
 * - Header：适合服务间调用，HTTP 标准头语义清晰，可被网关统一注入。<br>
 * - Param：适合浏览器调试，直接改 URL 即可测试，无需浏览器插件。<br>
 * - 二者均可被 Spring Cloud Gateway 等网关层统一改写，实现平台级控制。
 *
 * @see DesensitizeContext
 * @see DesensitizeAdvice
 */
public class DesensitizeInterceptor implements HandlerInterceptor {

    /**
     * 拦截器 preHandle 在 Controller 方法执行前调用
     * （也在 ResponseBodyAdvice.supports() 之前），
     * 确保脱敏开关提前就绪。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TODO: 安全漏洞 - 当前允许任意客户端通过请求头/参数关闭脱敏，存在数据泄露风险
        // 生产环境应添加权限校验：仅允许内部服务调用或管理员角色才能关闭脱敏
        String headerVal = request.getHeader("X-Desensitize");
        if (headerVal != null) {
            DesensitizeContext.setEnabled(Boolean.parseBoolean(headerVal));
            return true;
        }

        String paramVal = request.getParameter("_desensitize");
        if (paramVal != null) {
            DesensitizeContext.setEnabled(Boolean.parseBoolean(paramVal));
        }
        return true;
    }

}
