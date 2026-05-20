package com.github.pkyit.desensitive.desensitize.annotation;

import com.github.pkyit.desensitive.desensitize.enums.DesensitizeType;

import java.lang.annotation.*;

/**
 * 脱敏注解。
 * <p>
 * 标注在 VO/DTO 等响应类的字段上，声明该字段需要执行脱敏处理。
 * 配合 {@link com.github.pkyit.desensitive.desensitize.DesensitizeAdvice} 和
 * {@link com.github.pkyit.desensitive.desensitize.DesensitizeHandler} 自动生效。
 * </p>
 *
 * <b>使用示例：</b>
 * <pre>{@code
 * public class UserVO {
 *     private Long id;
 *
 *     @Desensitize(DesensitizeType.CHINESE_NAME)
 *     private String name;
 *
 *     @Desensitize(DesensitizeType.ID_CARD)
 *     private String idCard;
 * }
 * }</pre>
 *
 * @see DesensitizeType
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Desensitize {

    /**
     * 脱敏类型，决定使用哪种脱敏策略处理该字段的值。
     */
    DesensitizeType value();

}
