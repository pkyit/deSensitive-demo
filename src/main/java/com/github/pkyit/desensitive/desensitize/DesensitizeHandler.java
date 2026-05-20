package com.github.pkyit.desensitive.desensitize;

import com.github.pkyit.desensitive.desensitize.annotation.Desensitize;
import com.github.pkyit.desensitive.desensitize.enums.DesensitizeType;
import com.github.pkyit.desensitive.desensitize.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脱敏处理器 —— 反射执行脱敏的核心引擎。
 * <p>
 * 职责：
 * <ol>
 *   <li><b>策略注册</b>：管理 {@link DesensitizeType} 到 {@link DesensitizeStrategy} 的映射，
 *       支持运行时动态注册/替换策略。</li>
 *   <li><b>对象处理</b>：遍历对象的所有字段（含父类），发现有 {@link Desensitize} 注解则执行脱敏。</li>
 *   <li><b>值脱敏</b>：对单个字符串按指定策略执行脱敏。</li>
 * </ol>
 * </p>
 *
 * <h3>扩展性设计</h3>
 * <ul>
 *   <li><b>新增脱敏类型</b>：实现 {@link DesensitizeStrategy} + 枚举新增 + 注册，三步即可。</li>
 *   <li><b>运行时替换策略</b>：调用 {@link #registerStrategy(DesensitizeType, DesensitizeStrategy)}，
 *       可动态改变已有类型的脱敏算法，无需重启。</li>
 * </ul>
 */
public class DesensitizeHandler {

    private static final Logger log = LoggerFactory.getLogger(DesensitizeHandler.class);

    /** 策略注册表，ConcurrentHashMap 保证并发安全 */
    private static final Map<DesensitizeType, DesensitizeStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    static {
        STRATEGY_MAP.put(DesensitizeType.CHINESE_NAME, new ChineseNameStrategy());
        STRATEGY_MAP.put(DesensitizeType.ID_CARD, new IdCardStrategy());
        STRATEGY_MAP.put(DesensitizeType.MOBILE_PHONE, new MobilePhoneStrategy());
        STRATEGY_MAP.put(DesensitizeType.BANK_CARD, new BankCardStrategy());
        STRATEGY_MAP.put(DesensitizeType.ADDRESS, new AddressStrategy());
    }

    /**
     * 注册或覆盖指定脱敏类型的策略。
     * <p>
     * 框架使用者可通过此方法在运行时替换已有策略或添加自定义策略。
     * 例如：将地址脱敏改为更严格的只保留前 4 位。
     * </p>
     *
     * @param type     脱敏类型
     * @param strategy 脱敏策略实现
     */
    public static void registerStrategy(DesensitizeType type, DesensitizeStrategy strategy) {
        STRATEGY_MAP.put(type, strategy);
    }

    /**
     * 对单个字符串值执行脱敏。
     *
     * @param value 原始敏感字符串
     * @param type  脱敏类型
     * @return 脱敏后的字符串
     */
    public static String desensitize(String value, DesensitizeType type) {
        DesensitizeStrategy strategy = STRATEGY_MAP.get(type);
        if (strategy == null) {
            return value;
        }
        return strategy.desensitize(value);
    }

    /**
     * 对 JavaBean 对象执行字段脱敏。
     * <p>
     * 遍历对象及其父类的所有字段，对标注了 {@link Desensitize} 注解且类型为 String 的字段
     * 执行相应的脱敏策略。
     * </p>
     *
     * @param obj 要脱敏的对象，允许 null
     */
    public static void handle(Object obj) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        // 使用缓存的 FieldInfo 提升性能
        FieldInfo[] fieldInfos = FieldCache.getFieldInfos(clazz);
        for (FieldInfo fieldInfo : fieldInfos) {
            try {
                Object value = fieldInfo.field.get(obj);
                if (value instanceof String) {
                    String desensitized = desensitize((String) value, fieldInfo.type);
                    fieldInfo.field.set(obj, desensitized);
                }
            } catch (IllegalAccessException e) {
                log.warn("无法访问字段: {}.{}, 跳过脱敏", clazz.getName(), fieldInfo.field.getName());
            }
        }
    }

    /**
     * 克隆对象并返回新实例（浅拷贝）。
     * <p>
     * 通过反射创建新对象并复制所有字段值，用于避免修改原始数据。
     * 注意：这是浅拷贝，嵌套对象仍然共享引用，但会在递归脱敏时处理。
     * </p>
     *
     * @param obj 要克隆的对象
     * @return 克隆后的新对象，失败则返回 null
     */
    public static Object cloneObject(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            Class<?> clazz = obj.getClass();
            Object cloned = clazz.getDeclaredConstructor().newInstance();

            // 复制所有字段（包括父类），不仅仅是脱敏字段
            // 使用缓存的 Field 数组提升性能
            Field[] allFields = FieldCache.getAllFields(clazz);
            for (Field field : allFields) {
                Object value = field.get(obj);
                field.set(cloned, value);
            }

            return cloned;
        } catch (Exception e) {
            log.warn("克隆对象失败: {}, 将使用原对象", obj.getClass().getName());
            return null;
        }
    }

    /**
     * 字段信息缓存类，存储已解析的字段和注解信息。
     */
    private static class FieldInfo {
        final Field field;
        final DesensitizeType type;

        FieldInfo(Field field, DesensitizeType type) {
            this.field = field;
            this.type = type;
        }
    }

    /**
     * 字段信息缓存器，避免重复反射操作。
     * <p>
     * 使用 ConcurrentHashMap 保证线程安全，key 为 Class，value 为该类的字段信息数组。
     * 只在首次访问某个类时进行反射解析，后续直接从缓存读取。
     * </p>
     */
    private static class FieldCache {
        /** 缓存需要脱敏的字段信息 */
        private static final Map<Class<?>, FieldInfo[]> DESensitize_FIELD_CACHE = new ConcurrentHashMap<>();
        /** 缓存所有字段（用于克隆） */
        private static final Map<Class<?>, Field[]> ALL_FIELDS_CACHE = new ConcurrentHashMap<>();

        /**
         * 获取指定类的所有需要脱敏的字段信息（带缓存）。
         *
         * @param clazz 目标类
         * @return 字段信息数组，仅包含标注了 @Desensitize 注解的字段
         */
        static FieldInfo[] getFieldInfos(Class<?> clazz) {
            return DESensitize_FIELD_CACHE.computeIfAbsent(clazz, FieldCache::parseFieldInfos);
        }

        /**
         * 获取指定类的所有字段（带缓存），用于对象克隆。
         *
         * @param clazz 目标类
         * @return 所有字段数组（包括父类字段）
         */
        static Field[] getAllFields(Class<?> clazz) {
            return ALL_FIELDS_CACHE.computeIfAbsent(clazz, FieldCache::parseAllFields);
        }

        /**
         * 解析类的脱敏字段信息（无缓存版本）。
         */
        private static FieldInfo[] parseFieldInfos(Class<?> clazz) {
            java.util.List<FieldInfo> fieldInfoList = new java.util.ArrayList<>();
            parseFieldsRecursive(clazz, fieldInfoList);
            return fieldInfoList.toArray(new FieldInfo[0]);
        }

        /**
         * 解析类的所有字段（无缓存版本）。
         */
        private static Field[] parseAllFields(Class<?> clazz) {
            java.util.List<Field> fieldList = new java.util.ArrayList<>();
            parseAllFieldsRecursive(clazz, fieldList);
            Field[] allFields = fieldList.toArray(new Field[0]);
            // 批量设置 accessible，避免重复调用
            for (Field field : allFields) {
                field.setAccessible(true);
            }
            return allFields;
        }

        /**
         * 递归解析类及其父类的脱敏字段。
         */
        private static void parseFieldsRecursive(Class<?> clazz, java.util.List<FieldInfo> fieldInfoList) {
            if (clazz == null || clazz == Object.class) {
                return;
            }

            // 先处理父类
            parseFieldsRecursive(clazz.getSuperclass(), fieldInfoList);

            // 再处理当前类
            for (Field field : clazz.getDeclaredFields()) {
                Desensitize annotation = field.getAnnotation(Desensitize.class);
                if (annotation != null) {
                    field.setAccessible(true); // 必须设置，否则无法访问 private 字段
                    fieldInfoList.add(new FieldInfo(field, annotation.value()));
                }
            }
        }

        /**
         * 递归解析类及其父类的所有字段。
         */
        private static void parseAllFieldsRecursive(Class<?> clazz, java.util.List<Field> fieldList) {
            if (clazz == null || clazz == Object.class) {
                return;
            }

            // 先处理父类
            parseAllFieldsRecursive(clazz.getSuperclass(), fieldList);

            // 再处理当前类
            for (Field field : clazz.getDeclaredFields()) {
                fieldList.add(field);
            }
        }
    }
}
