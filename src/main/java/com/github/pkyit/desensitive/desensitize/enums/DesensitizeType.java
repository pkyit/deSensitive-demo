package com.github.pkyit.desensitive.desensitize.enums;

/**
 * 脱敏类型枚举。
 * <p>
 * 定义框架支持的敏感信息分类，每种类型对应一种脱敏策略实现。
 * 新增类型只需在此添加枚举值，并在
 * {@link com.github.pkyit.desensitive.desensitize.DesensitizeHandler} 中注册对应策略即可
 * （策略模式，开闭原则）。
 * </p>
 */
public enum DesensitizeType {

    /** 中文姓名：张三丰 → 张*** */
    CHINESE_NAME,
    /** 身份证号码：110101199001011234 → 110101********1234 */
    ID_CARD,
    /** 手机号码：13812345678 → 138****5678 */
    MOBILE_PHONE,
    /** 银行卡号：6222021234561234567 → 622202******4567 */
    BANK_CARD,
    /** 地址：北京市朝阳区建国路88号 → 北京市朝阳区********** */
    ADDRESS

}
