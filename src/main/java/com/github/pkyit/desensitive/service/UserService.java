package com.github.pkyit.desensitive.service;

import com.github.pkyit.desensitive.model.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户信息服务 —— 模拟数据访问层。
 * <p>
 * 在真实项目中，此处应注入 Mapper/Repository 从数据库查询。
 * 本 demo 使用硬编码数据模拟查询结果，聚焦脱敏框架的演示。
 * </p>
 */
@Service
public class UserService {

    /**
     * 模拟根据 ID 查询单个用户。
     *
     * @param id 用户 ID
     * @return 用户实体（含完整的明文敏感信息）
     */
    public User findById(Long id) {
        return new User(
                id,
                "张三丰",
                "110101199001011234",
                "13812345678",
                "6222021234561234567",
                "北京市朝阳区建国路88号华贸中心"
        );
    }

    /**
     * 模拟查询所有用户。
     *
     * @return 用户实体列表
     */
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        list.add(new User(1L, "张三丰", "110101199001011234",
                "13812345678", "6222021234561234567", "北京市朝阳区建国路88号华贸中心"));
        list.add(new User(2L, "李小明", "310101199203154567",
                "15987654321", "6217001234567890123", "上海市浦东新区陆家嘴金融中心"));
        list.add(new User(3L, "王重阳", "440101198805209876",
                "18688886666", "9558801234567890123", "广州市天河区珠江新城花城大道"));
        return list;
    }
}
