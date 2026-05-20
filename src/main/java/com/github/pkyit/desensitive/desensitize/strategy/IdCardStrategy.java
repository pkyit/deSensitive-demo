package com.github.pkyit.desensitive.desensitize.strategy;

/**
 * 身份证号脱敏策略。
 * <p>
 * 规则：保留前 6 位（地址码）和后 4 位（顺序码末两位 + 校验码），
 * 中间出生日期部分全部替换为 {@code *}。
 * 此规则符合《个人信息去标识化规范》GB/T 37964-2019 的推荐做法。
 * </p>
 *
 * <pre>
 * 示例：
 *   "110101199001011234" → "110101********1234"
 *   "31010120300101123X" → "310101**********23X"
 * </pre>
 *
 * 边界处理：若输入长度不足 10 位（非标准身份证号），直接返回原值，
 * 避免掩码破坏不可逆。
 */
public class IdCardStrategy implements DesensitizeStrategy {

    /** 前段保留位数（地址码） */
    private static final int PREFIX_LEN = 6;
    /** 后段保留位数（顺序码末2位 + 校验码） */
    private static final int SUFFIX_LEN = 4;

    @Override
    public String desensitize(String source) {
        if (source == null || source.length() < (PREFIX_LEN + SUFFIX_LEN)) {
            return source;
        }
        int maskLen = source.length() - PREFIX_LEN - SUFFIX_LEN;
        StringBuilder sb = new StringBuilder(source.length());
        sb.append(source, 0, PREFIX_LEN);
        for (int i = 0; i < maskLen; i++) {
            sb.append('*');
        }
        sb.append(source.substring(source.length() - SUFFIX_LEN));
        return sb.toString();
    }
}
