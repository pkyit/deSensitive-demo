package com.github.pkyit.desensitive.desensitize.strategy;

/**
 * 脱敏策略接口 —— 策略模式的核心抽象。
 * <p>
 * 定义统一的脱敏方法契约，所有具体脱敏算法都实现此接口。
 * 框架通过策略模式实现"开闭原则"：新增脱敏类型时，只需新增一个实现类，
 * 无需修改现有代码。
 * </p>
 *
 * 框架内置了以下实现：
 * <ul>
 *   <li>{@link ChineseNameStrategy} - 中文名脱敏</li>
 *   <li>{@link IdCardStrategy} - 身份证号脱敏</li>
 *   <li>{@link MobilePhoneStrategy} - 手机号脱敏</li>
 *   <li>{@link BankCardStrategy} - 银行卡号脱敏</li>
 *   <li>{@link AddressStrategy} - 地址脱敏</li>
 * </ul>
 */
public interface DesensitizeStrategy {

    /**
     * 对原始字符串执行脱敏。
     *
     * @param source 原始敏感数据，可能为 null
     * @return 脱敏后的字符串。若 source 为 null、空串或长度不足，返回原值
     */
    String desensitize(String source);

}
