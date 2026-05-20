package com.github.pkyit.desensitive.model.entity;

import java.util.StringJoiner;

/**
 * 用户实体 —— 对应数据库表。
 * <p>
 * 保持 POJO 纯净：不标注任何脱敏注解，不做序列化相关侵入。
 * 脱敏注解标注在对应的 VO 上，职责分离。
 * 这样实体可以在 Service 层自由传递而不会意外暴露脱敏后数据。
 * </p>
 */
public class User {

    private Long id;
    private String name;
    private String idCard;
    private String mobilePhone;
    private String bankCard;
    private String address;

    public User() {
    }

    public User(Long id, String name, String idCard, String mobilePhone, String bankCard, String address) {
        this.id = id;
        this.name = name;
        this.idCard = idCard;
        this.mobilePhone = mobilePhone;
        this.bankCard = bankCard;
        this.address = address;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", User.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("idCard='" + idCard + "'")
                .add("mobilePhone='" + mobilePhone + "'")
                .add("bankCard='" + bankCard + "'")
                .add("address='" + address + "'")
                .toString();
    }
}
