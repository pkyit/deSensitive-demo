package com.github.pkyit.desensitive.controller;

import com.github.pkyit.desensitive.desensitize.DesensitizeContext;
import com.github.pkyit.desensitive.model.entity.User;
import com.github.pkyit.desensitive.model.vo.UserVO;
import com.github.pkyit.desensitive.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息 REST 接口。
 * <p>
 * 模拟查询用户信息，返回的 {@link UserVO} 中各敏感字段标注了 {@code @Desensitize} 注解，
 * 由框架自动脱敏。提供脱敏开关 API，支持运行时动态控制。
 * </p>
 *
 * <h3>脱敏控制 API</h3>
 * <ul>
 *   <li>{@code POST /user/desensitize/global?enabled=true|false} — 全局开关</li>
 *   <li>请求头 {@code X-Desensitize: true|false} — 单次请求开关</li>
 * </ul>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 根据 ID 查询单个用户。
     * 返回脱敏后的 UserVO（脱敏开启时）。
     *
     * @param id 用户 ID
     * @return 脱敏后的用户信息
     */
    @GetMapping("/{id}")
    public UserVO getUser(@PathVariable Long id) {
        return toVO(userService.findById(id));
    }

    /**
     * 查询所有用户列表。
     * 返回脱敏后的 UserVO 列表（脱敏开启时）。
     *
     * @return 脱敏后的用户列表
     */
    @GetMapping("/list")
    public List<UserVO> listUsers() {
        return userService.findAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 全局开启/关闭脱敏功能。
     * <p>
     * 调用后会修改DesensitizeContext的globalEnabled静态变量，
     * 对所有后续请求生效，直至再次调用或重启应用。
     * </p>
     *
     * @param enabled true-开启脱敏，false-关闭脱敏
     * @return 操作结果提示
     */
    // TODO: 安全漏洞 - 此接口未做权限校验，任意用户均可关闭全局脱敏
    // 生产环境应添加@PreAuthorize("hasRole('ADMIN')")或类似鉴权注解
    @PostMapping("/desensitize/global")
    public String toggleGlobal(@RequestParam boolean enabled) {
        DesensitizeContext.setGlobalEnabled(enabled);
        return "全局脱敏已" + (enabled ? "开启" : "关闭");
    }

    /**
     * 当前请求开启/关闭脱敏（覆盖全局开关）。
     * <p>
     * 注意：这是线程级开关，仅对当前请求生效。
     * 主要用于测试或特殊场景（如运营后台需要查看明文）。
     * 更推荐使用请求头 {@code X-Desensitize} 控制。
     * </p>
     *
     * @param enabled true-开启脱敏，false-关闭脱敏
     * @return 操作结果提示
     */
    // TODO: 安全漏洞 - 此接口的实际效果受限于 ThreadLocal 的生命周期
    // 由于 ThreadLocal 在请求结束后会被清理，此设置只会影响当前请求，对后续请求无效
    // 建议通过请求头 X-Desensitize 或参数 _desensitize 在每次请求时动态控制
    @Deprecated
    @PostMapping("/desensitize/once")
    public String toggleOnce(@RequestParam boolean enabled) {
        DesensitizeContext.setEnabled(enabled);
        return "本次请求脱敏已" + (enabled ? "开启" : "关闭") + "（注意：此设置仅当前请求有效，请使用请求头或参数控制）";
    }

    /**
     * 将 User 实体转换为 UserVO（脱敏框架的入口对象）。
     * <p>
     * 实体层（entity）不应感知脱敏注解，保持 POJO 纯净。
     * 在 VO 层标注注解，Controller 返回前自动脱敏。
     * </p>
     */
    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
