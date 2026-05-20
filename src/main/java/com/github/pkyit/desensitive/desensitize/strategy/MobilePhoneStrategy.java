package com.github.pkyit.desensitive.desensitize.strategy;

/**
 * 手机号脱敏策略。
 * <p>
 * 规则：保留前 3 位（网络识别号）和后 4 位（用户号），中间 4 位替换为 ****。
 * 中国大陆手机号为 11 位，此规则可保留运营商归属地所需的前缀信息。
 * </p>
 *
 * <pre>
 * 示例：
 *   "13812345678" → "138****5678"
 *   "15987654321" → "159****4321"
 * </pre>
 */
public class MobilePhoneStrategy implements DesensitizeStrategy {

    /** 正则：捕获前3位和后4位，中间4位用 **** 替换 */
    private static final String REGEX = "(\\d{3})\\d{4}(\\d{4})";
    private static final String REPLACEMENT = "$1****$2";

    @Override
    public String desensitize(String source) {
        if (source == null || source.length() < 7) {
            return source;
        }
        return source.replaceAll(REGEX, REPLACEMENT);
    }
}
