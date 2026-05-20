package com.github.pkyit.desensitive.model.vo;

import com.github.pkyit.desensitive.desensitize.annotation.Desensitize;
import com.github.pkyit.desensitive.desensitize.enums.DesensitizeType;

/**
 * 用户信息视图对象 —— 脱敏注解标注的载体。
 * <p>
 * VO 层是脱敏框架的"入口"：在需要对外输出的字段上标注 {@link Desensitize} 注解，
 * 声明该字段需要进行脱敏处理。框架会在响应序列化前自动完成脱敏。
 * </p>
 *
 * <h3>设计原则：注解标注在 VO 而非 Entity</h3>
 * <ul>
 *   <li><b>职责分离</b>：Entity 对应数据库结构，VO 对应接口契约，互不干扰。</li>
 *   <li><b>灵活性</b>：同一 Entity 可映射到多个 VO，不同 VO 可做不同的脱敏策略。
 *       例如，管理端 VO 脱敏程度低（保留更多信息），客户端 VO 脱敏程度高。</li>
 *   <li><b>安全性</b>：Service 层内部传递 Entity 时始终是明文，
 *       脱敏仅在 Controller 返回时触发，避免内部日志打印脱敏后数据。</li>
 * </ul>
 */
public class UserVO {

    /** 用户 ID（主键，无需脱敏） */
    private Long id;

    /** 姓名，JSON 序列化时自动脱敏 */
    @Desensitize(DesensitizeType.CHINESE_NAME)
    private String name;

    /** 身份证号 */
    @Desensitize(DesensitizeType.ID_CARD)
    private String idCard;

    /** 手机号码 */
    @Desensitize(DesensitizeType.MOBILE_PHONE)
    private String mobilePhone;

    /** 银行卡号 */
    @Desensitize(DesensitizeType.BANK_CARD)
    private String bankCard;

    /** 地址 */
    @Desensitize(DesensitizeType.ADDRESS)
    private String address;

    public UserVO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getBankCard() {
        return bankCard;
    }

    public void setBankCard(String bankCard) {
        this.bankCard = bankCard;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
