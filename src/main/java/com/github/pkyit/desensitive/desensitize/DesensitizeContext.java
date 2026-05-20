package com.github.pkyit.desensitive.desensitize;

/**
 * 脱敏开关上下文 —— 实现"动态可插拔"的核心基础设施。
 * <p>
 * 框架需要在不重启应用、不修改代码的前提下动态控制是否脱敏。
 * 本类通过 <b>全局开关 + ThreadLocal 线程级开关</b> 两层设计来实现：
 * </p>
 *
 * <h3>两层开关机制</h3>
 * <ul>
 *   <li><b>全局开关</b> ({@code globalEnabled})：静态 volatile 变量，
 *       由 application.yaml 或 API 设定，影响所有请求，适合运维控制。</li>
 *   <li><b>线程级开关</b> ({@link ThreadLocal})：每个请求独立的副本，
 *       由 {@link DesensitizeInterceptor} 从请求头/参数解析而来，
 *       仅影响当前请求，适合业务方精细控制。</li>
 * </ul>
 *
 * <h3>优先级</h3>
 * <blockquote>
 *   线程级开关 &gt; 全局开关
 * </blockquote>
 * <p>
 * 当线程级开关未设置时，回退到全局开关；
 * 一旦设置了线程级开关（即使为 null），则以线程级为准。
 * 请求结束后由 {@link DesensitizeContextFilter} 清理 ThreadLocal，
 * 防止内存泄漏。
 * </p>
 *
 * @see DesensitizeInterceptor
 * @see DesensitizeContextFilter
 */
public class DesensitizeContext {

    /** 全局脱敏开关，volatile 保证多线程可见性 */
    private static volatile boolean globalEnabled = true;

    /**
     * 线程级脱敏开关。
     * 使用 ThreadLocal 而非 request attribute，是为了与 ResponseBodyAdvice
     * 的解耦 —— Advice 不依赖 Servlet API，可复用。
     * withInitial(null) 表示"未设置"，需要回退到全局开关。
     */
    private static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> null);

    private DesensitizeContext() {
    }

    /**
     * 判断当前上下文是否应执行脱敏。
     * 优先级：线程级开关 > 全局开关。
     *
     * @return true 表示应脱敏，false 不脱敏
     */
    public static boolean isEnabled() {
        Boolean threadLocalValue = ENABLED.get();
        if (threadLocalValue != null) {
            return threadLocalValue;
        }
        return globalEnabled;
    }

    /**
     * 设置线程级脱敏开关（仅对当前请求生效）。
     * 通常在拦截器中由请求头/参数触发。
     *
     * @param enabled true-开启脱敏，false-关闭脱敏
     */
    public static void setEnabled(boolean enabled) {
        ENABLED.set(enabled);
    }

    /**
     * 设置全局脱敏开关（对所有请求生效直至下次修改或重启）。
     *
     * @param enabled true-开启脱敏，false-关闭脱敏
     */
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
    }

    /**
     * 获取当前全局开关状态（仅用于运维展示）。
     */
    public static boolean isGlobalEnabled() {
        return globalEnabled;
    }

    /**
     * 清除当前线程的脱敏开关状态。
     * 由 {@link DesensitizeContextFilter} 在当前请求处理完毕后自动调用，
     * 防止 Web 容器线程池复用导致状态串扰。
     */
    public static void clear() {
        ENABLED.remove();
    }

}
