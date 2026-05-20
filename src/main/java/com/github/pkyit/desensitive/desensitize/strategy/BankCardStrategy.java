package com.github.pkyit.desensitive.desensitize.strategy;

/**
 * 银行卡号脱敏策略。
 * <p>
 * 规则：保留前 6 位（发卡行识别码 BIN）和后 4 位（账户标识），
 * 中间部分替换为 ******。
 * 银行卡号通常为 16~19 位，中间位数不固定，故用 {@code +} 匹配剩余所有数字。
 * </p>
 *
 * <pre>
 * 示例：
 *   "6222021234561234567" → "622202******4567"
 *   "6217001234567890123" → "621700******0123"
 * </pre>
 */
public class BankCardStrategy implements DesensitizeStrategy {

    /** 正则：保留前6位和后4位，中间变长部分固定替换为 ****** */
    private static final String REGEX = "(\\d{6})\\d+(\\d{4})";
    private static final String REPLACEMENT = "$1******$2";

    @Override
    public String desensitize(String source) {
        if (source == null || source.length() < 10) {
            return source;
        }
        return source.replaceAll(REGEX, REPLACEMENT);
    }
}
