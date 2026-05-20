package com.github.pkyit.desensitive.desensitize;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Collection;

/**
 * 响应体脱敏增强器 —— 框架与 Spring MVC 的集成点。
 * <p>
 * 通过 Spring 的 {@link ResponseBodyAdvice} 机制，在 Controller 方法返回后、
 * HttpMessageConverter 序列化之前，拦截响应对象并执行脱敏。
 * 对业务代码完全透明 —— Controller 只需返回正常的 POJO，
 * 无需手动调用任何脱敏方法。
 * </p>
 *
 * <h3>执行流程</h3>
 * <ol>
 *   <li>{@link #supports} 方法判断当前是否应启用脱敏（委托 {@link DesensitizeContext}）。</li>
 *   <li>若启用，{@link #beforeBodyWrite} 对响应体执行递归脱敏。</li>
 *   <li>处理集合类型时遍历每个元素单独处理。</li>
 * </ol>
 *
 * <h3>为什么不使用 AOP？</h3>
 * ResponseBodyAdvice 是 Spring 官方提供的"后处理"扩展点，
 * 比 AOP 更贴近 MVC 层语义，且能天然处理
 * {@code @ResponseBody} / {@code @RestController} 的序列化流程，
 * 无需考虑 AOP 拦截器链的兼容性。
 */
@ControllerAdvice
public class DesensitizeAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断当前响应是否需要脱敏。
     * <p>
     * 此方法在每次请求时被调用，返回 false 则跳过整个脱敏流程。
     * 脱敏开关动态变化时，此返回值也随之变化，实现"动态可插拔"。
     * </p>
     */
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return DesensitizeContext.isEnabled();
    }

    /**
     * 响应体序列化前的最终拦截点。
     * <p>
     * 重要改进：不再直接修改原对象，而是克隆后脱敏，避免污染 Service 层返回的原始数据。
     * 这样可以防止缓存污染、日志记录错误等问题。
     * </p>
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        // 克隆对象后再脱敏，避免修改原始对象
        return cloneAndDesensitize(body);
    }

    /**
     * 递归处理响应体，支持集合类型的脱敏。
     * <p>
     * 当 Controller 返回 {@code List<UserVO>} 时，body 是 ArrayList，
     * 需要遍历每个元素脱敏。若元素本身是嵌套对象，由
     * {@link DesensitizeHandler#handle} 递归处理。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private void handleObject(Object obj) {
        if (obj instanceof Collection) {
            for (Object item : (Collection<Object>) obj) {
                DesensitizeHandler.handle(item);
            }
        } else {
            DesensitizeHandler.handle(obj);
        }
    }

    /**
     * 克隆对象并执行脱敏，避免修改原始数据。
     * <p>
     * 使用浅拷贝 + 深度脱敏的策略：
     * - 对于简单对象：通过反射创建新实例并复制字段
     * - 对于集合/Map：创建新容器，克隆内部元素
     * - 对于数组：创建新数组，克隆每个元素
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Object cloneAndDesensitize(Object obj) {
        if (obj == null) {
            return null;
        }

        // 处理集合类型
        if (obj instanceof Collection) {
            Collection<Object> original = (Collection<Object>) obj;
            // 创建相同类型的集合
            Collection<Object> cloned;
            try {
                cloned = (Collection<Object>) obj.getClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // 如果无法创建新实例，退化为 ArrayList
                cloned = new java.util.ArrayList<>();
            }
            for (Object item : original) {
                cloned.add(cloneAndDesensitize(item));
            }
            return cloned;
        }

        // 处理 Map 类型
        if (obj instanceof java.util.Map) {
            java.util.Map<Object, Object> original = (java.util.Map<Object, Object>) obj;
            java.util.Map<Object, Object> cloned;
            try {
                cloned = (java.util.Map<Object, Object>) obj.getClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                cloned = new java.util.HashMap<>();
            }
            for (java.util.Map.Entry<Object, Object> entry : original.entrySet()) {
                cloned.put(entry.getKey(), cloneAndDesensitize(entry.getValue()));
            }
            return cloned;
        }

        // 处理数组类型
        if (obj.getClass().isArray()) {
            Object[] original = (Object[]) obj;
            Object[] cloned = new Object[original.length];
            for (int i = 0; i < original.length; i++) {
                cloned[i] = cloneAndDesensitize(original[i]);
            }
            return cloned;
        }

        // 处理普通对象：克隆后脱敏
        Object cloned = DesensitizeHandler.cloneObject(obj);
        if (cloned != null) {
            DesensitizeHandler.handle(cloned);
        }
        return cloned != null ? cloned : obj;
    }

}
