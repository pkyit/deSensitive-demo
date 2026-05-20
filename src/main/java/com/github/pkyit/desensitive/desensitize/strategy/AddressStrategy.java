package com.github.pkyit.desensitive.desensitize.strategy;

/**
 * 地址脱敏策略。
 * <p>
 * 规则：保留前 6 个字符（通常是省市区/县级别），
 * 后续详细街道/门牌号/小区名等信息用 {@code *} 替换。
 * 前 6 位已包含"省市区"三级行政区划，足够定位到大致区域，
 * 同时又隐藏了精确地址，兼顾可用性与隐私保护。
 * </p>
 *
 * <pre>
 * 示例：
 *   "北京市朝阳区建国路88号华贸中心" → "北京市朝阳区**********"
 *   "上海市浦东新区陆家嘴金融中心"     → "上海市浦东新区******"
 * </pre>
 *
 * 优化说明：地址长度差异大，使用 {@code charAt / length()} 逐个掩码，
 * 不用正则避免因特殊字符导致回溯陷阱。
 */
public class AddressStrategy implements DesensitizeStrategy {

    /** 保留的前缀长度（约等于"省市区"三级区划的字符数） */
    private static final int PREFIX_LEN = 6;

    @Override
    public String desensitize(String source) {
        if (source == null || source.length() <= PREFIX_LEN) {
            return source;
        }
        StringBuilder sb = new StringBuilder(source.length());
        sb.append(source, 0, PREFIX_LEN);
        for (int i = PREFIX_LEN; i < source.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
