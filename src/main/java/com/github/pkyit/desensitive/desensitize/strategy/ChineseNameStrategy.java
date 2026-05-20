package com.github.pkyit.desensitive.desensitize.strategy;

/**
 * 中文名脱敏策略。
 * <p>
 * 规则：保留第一个汉字，其余字符全部替换为 {@code *}。
 * 之所以不保留末位（如 "李*明"），是因为中文名长度通常为 2~4 字，
 * 保留首字已足够保留辨识度的同时隐藏大部分信息。
 * 若长度不足 2（单名或空值），直接返回原值。
 * </p>
 *
 * <pre>
 * 示例：
 *   "张三"   → "张*"
 *   "张三丰" → "张**"
 *   "欧阳小明" → "欧***"
 * </pre>
 */
public class ChineseNameStrategy implements DesensitizeStrategy {

    @Override
    public String desensitize(String source) {
        if (source == null || source.length() < 2) {
            return source;
        }
        return source.charAt(0) + repeat('*', source.length() - 1);
    }

    /**
     * 生成指定数量的重复字符。
     */
    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
