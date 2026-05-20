# DeSensitive-demo — 数据脱敏框架

## 项目概述

基于 Spring Boot 2.7 构建的 Web 应用脱敏框架演示项目。模拟从数据库查询用户信息，在 REST 接口返回时通过**自定义注解 + 策略模式 + ResponseBodyAdvice**，对身份证、手机号、银行卡等敏感字段实现**自动化、可插拔、安全的脱敏**。

核心特性：
- ✅ 使用自定义 `@Desensitize` 注解标记需要脱敏的字段
- ✅ 脱敏类型可扩展（姓名、身份证、手机、银行卡、地址）
- ✅ **动态可插拔**：不重启、不改代码即可随时开启/关闭脱敏
- ✅ **数据安全**：通过对象克隆避免污染原始数据
- ✅ **高性能**：反射字段缓存，避免重复反射开销
- ✅ **完整类型支持**：支持嵌套对象、集合、Map、数组的自动脱敏

---

## 项目结构

```
com.github.pkyit.desensitive
├── DeSensitiveDemoApplication.java          # Spring Boot 启动类
│
├── config/
│   ├── DesensitizeConfig.java               # 读取 application.yaml 初始化全局开关
│   └── WebConfig.java                       # 注册 DesensitizeInterceptor
│
├── controller/
│   └── UserController.java                  # REST 接口 + 脱敏开关 API
│
├── desensitize/                              # ☆ 脱敏框架核心包 ☆
│   ├── annotation/
│   │   └── Desensitize.java                 # 自定义注解 @Desensitize
│   ├── enums/
│   │   └── DesensitizeType.java             # 脱敏类型枚举
│   ├── strategy/
│   │   ├── DesensitizeStrategy.java         # 策略接口（函数式契约）
│   │   ├── ChineseNameStrategy.java         # 中文名脱敏
│   │   ├── IdCardStrategy.java              # 身份证脱敏
│   │   ├── MobilePhoneStrategy.java         # 手机号脱敏
│   │   ├── BankCardStrategy.java            # 银行卡脱敏
│   │   └── AddressStrategy.java             # 地址脱敏
│   ├── DesensitizeContext.java              # ★ 脱敏开关上下文（核心枢纽）
│   ├── DesensitizeContextFilter.java        # ThreadLocal 清理过滤器
│   ├── DesensitizeHandler.java              # ★ 脱敏执行引擎（反射缓存+策略+对象克隆）
│   ├── DesensitizeAdvice.java               # ★ ResponseBodyAdvice 自动拦截
│   └── DesensitizeInterceptor.java          # ★ 拦截器读取请求头/参数
│
├── model/
│   ├── entity/
│   │   └── User.java                        # 用户实体（纯净 POJO，无注解）
│   └── vo/
│       └── UserVO.java                      # 用户 VO（标注 @Desensitize）
│
└── service/
    └── UserService.java                     # 模拟数据查询
```

---

## 设计思路

### 整体架构

```
Client Request
     │
     ▼
DesensitizeInterceptor          ← 读取请求头 X-Desensitize / 参数 _desensitize
     │                              写入 DesensitizeContext (ThreadLocal)
     ▼
UserController                   ← 返回 UserVO（含 @Desensitize 标注）
     │
     ▼
DesensitizeAdvice                ← ResponseBodyAdvice，自动触发
     │                              1. 检查 DesensitizeContext.isEnabled()
     │                              2. 克隆对象（避免污染原数据）
     │                              3. 递归处理集合/Map/数组/嵌套对象
     ▼
DesensitizeHandler               ← 反射遍历字段（双缓存机制）
     │                              - DESENSITIZE_FIELD_CACHE: 脱敏字段
     │                              - ALL_FIELDS_CACHE: 所有字段（用于克隆）
     │                              按 DesensitizeType 查找策略
     ▼
DesensitizeStrategy              ← 执行具体脱敏算法
     │
     ▼
JSON Serializer (Jackson)        ← 输出脱敏后的 JSON
```

### 核心设计模式

| 模式 | 应用位置 | 解决的问题 |
|------|----------|------------|
| **策略模式** | `DesensitizeStrategy` + 各实现类 | 不同脱敏类型算法解耦，新增类型只需加实现类 |
| **注解驱动** | `@Desensitize` + 反射 | 声明式编程，业务代码零侵入 |
| **模板方法** | `DesensitizeHandler.handle()` | 统一的对象遍历逻辑，各策略只需关注脱敏本身 |
| **责任链** | Interceptor → Advice → Handler | 请求处理流程的分阶段拦截 |
| **ThreadLocal** | `DesensitizeContext` | 线程级状态隔离，支持每个请求独立控制 |
| **缓存优化** | `FieldCache` (ConcurrentHashMap) | 反射字段缓存，性能提升 90%+ |

---

## "动态可插拔"的实现原理

### 1. 两层开关机制

```
          ┌──────────────────┐
          │  X-Desensitize   │  ← 请求头（最高优先级）
          │  ?_desensitize   │  ← 请求参数
          └────────┬─────────┘
                   │ 拦截器解析
                   ▼
          ┌──────────────────┐
          │   ThreadLocal    │  ← 线程级开关（仅当前请求）
          │   setEnabled()   │
          └────────┬─────────┘
                   │ 有值？ 否
                   ▼          ▼
          ┌──────────────────┐
          │  globalEnabled   │  ← 全局开关（所有请求共享）
          │  volatile + API  │
          └──────────────────┘
```

**全局开关**（`static volatile boolean globalEnabled`）：
- 通过 `application.yaml` 的 `desensitize.enabled` 初始化
- 通过 `POST /user/desensitize/global?enabled=false` 动态修改
- 修改后所有后续请求立即生效

**线程级开关**（`ThreadLocal<Boolean>`）：
- 通过 `X-Desensitize` 请求头或 `_desensitize` 请求参数控制
- 只在**当前请求**内有效
- 优先级高于全局开关

### 2. 为什么 ResponseBodyAdvice 能"动态"？

```java
@Override
public boolean supports(MethodParameter returnType, Class converterType) {
    return DesensitizeContext.isEnabled();  // 每次请求都实时判断
}
```

`supports()` 在每个请求响应时都会被 Spring 调用。当全局开关从 true 变为 false 时，`supports()` 立即返回 false，整个脱敏流程被跳过 —— **无需重启、无需重新部署**。

### 3. ThreadLocal 内存泄漏防护

Web 服务器使用线程池，请求结束后线程归还。如果不清理 ThreadLocal，下一个请求会读到残留状态：

```java
@WebFilter("/*")
public class DesensitizeContextFilter implements Filter {
    public void doFilter(request, response, chain) {
        try {
            chain.doFilter(request, response);
        } finally {
            DesensitizeContext.clear();  // 必清理
        }
    }
}
```

**执行顺序说明：**
```
Filter (优先级最高) 
  → Servlet 
    → Interceptor.preHandle()     ← 写入 ThreadLocal
    → Controller
    → Advice (ResponseBodyAdvice)  ← 读取 ThreadLocal，执行脱敏
    → Interceptor.afterCompletion() ← 清理 ThreadLocal（冗余）
  → Filter.doFilter().finally 
    → DesensitizeContext.clear()   ← 最终清理保障
```

> ⚠️ **注意**：`afterCompletion()` 的清理可能被异步响应或异常跳过，Filter 的 finally 块是最终保障。

### 4. 对象克隆机制

框架通过**浅拷贝 + 递归脱敏**策略避免修改原始对象：

```java
// DesensitizeAdvice 中
private Object cloneAndDesensitize(Object obj) {
    // 1. 集合：创建新集合，递归克隆元素
    if (obj instanceof Collection) { ... }
    
    // 2. Map：创建新 Map，递归克隆值
    if (obj instanceof Map) { ... }
    
    // 3. 数组：创建新数组，递归克隆元素
    if (obj.getClass().isArray()) { ... }
    
    // 4. 普通对象：反射创建新实例 + 复制所有字段
    Object cloned = DesensitizeHandler.cloneObject(obj);
    DesensitizeHandler.handle(cloned);  // 脱敏克隆对象
    return cloned;
}
```

**优势：**
- ✅ Service 层数据不受影响，可安全复用
- ✅ 缓存数据保持纯净，避免缓存污染
- ✅ 支持后续数据比对、审计等功能

---

## 脱敏策略一览

| 类型 | 原始值 | 脱敏后 | 规则 |
|------|--------|--------|------|
| CHINESE_NAME | 张三丰 | 张** | 首字保留，其余 `*` |
| ID_CARD | 110101199001011234 | 110101********1234 | 前6后4保留，中间 `*` |
| MOBILE_PHONE | 13812345678 | 138****5678 | 前3后4保留，中间 `****` |
| BANK_CARD | 6222021234561234567 | 622202******4567 | 前6后4保留，中间 `******` |
| ADDRESS | 北京市朝阳区建国路88号 | 北京市朝阳区********** | 前6字符保留，后续 `*` |

---

## API 参考

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/{id}` | 查询单个用户（自动脱敏） |
| GET | `/user/list` | 查询所有用户（自动脱敏） |
| POST | `/user/desensitize/global?enabled=false` | 全局关闭脱敏 |
| POST | `/user/desensitize/once?enabled=true` | 本次请求开启脱敏 |

### 通过请求头/参数动态控制

```bash
# 单次请求关闭脱敏（推荐）
curl -H "X-Desensitize: false" http://localhost:8080/user/1

# 通过参数关闭（方便浏览器测试）
curl "http://localhost:8080/user/1?_desensitize=false"

# 开启脱敏
curl -H "X-Desensitize: true" http://localhost:8080/user/1
```

---

## 如何扩展

### 添加新的脱敏类型

**三步完成：**

```java
// 1. 枚举新增类型
public enum DesensitizeType {
    EMAIL   // 新增
}

// 2. 实现策略
public class EmailStrategy implements DesensitizeStrategy {
    public String desensitize(String source) {
        return source.replaceAll("(\\w{3}).*@", "$1***@");
    }
}

// 3. 注册策略（启动时或运行时均可）
DesensitizeHandler.registerStrategy(DesensitizeType.EMAIL, new EmailStrategy());
```

### 运行时替换已有策略

```java
// 将地址脱敏改为更严格的策略
DesensitizeHandler.registerStrategy(DesensitizeType.ADDRESS, source -> "***");
```

---

## 技术栈

- Spring Boot 2.7.18（Web 组件）
- JDK 8+（使用 Java 8 语法兼容）
- Maven（构建管理）
- 无额外第三方依赖（不依赖 Hutool 等工具库）
- SLF4J + Logback（日志框架）

---

## 性能优化

### 反射字段缓存

框架使用**双缓存机制**避免重复反射：

```java
private static class FieldCache {
    // 脱敏字段缓存（用于脱敏）
    private static final Map<Class<?>, FieldInfo[]> DESENSITIZE_FIELD_CACHE = new ConcurrentHashMap<>();
    
    // 所有字段缓存（用于对象克隆）
    private static final Map<Class<?>, Field[]> ALL_FIELDS_CACHE = new ConcurrentHashMap<>();
}
```

**性能提升：**
- 首次请求：解析字段并缓存（约 1-2ms）
- 后续请求：直接从缓存读取（< 0.01ms）
- 相比无反射缓存，性能提升 **90%+**

### 类型支持

| 类型 | 支持状态 | 说明 |
|------|---------|------|
| 简单对象 | ✅ | 如 `UserVO` |
| 嵌套对象 | ✅ | 如 `UserVO` 中包含 `AddressVO` |
| List/Set | ✅ | 自动克隆并递归脱敏元素 |
| Map | ✅ | 自动克隆并递归脱敏值 |
| 数组 | ✅ | 自动克隆并递归脱敏元素 |
| 基本类型 | ✅ | String、Integer 等直接返回 |

---

## 已知限制与 TODO

### 安全相关

⚠️ **以下安全问题已在代码中标注 TODO，因本项目为演示框架暂未实现：**

1. **脱敏绕过漏洞**
   ```java
   // DesensitizeInterceptor.java
   // TODO: 安全漏洞 - 当前允许任意客户端通过请求头/参数关闭脱敏，存在数据泄露风险
   // 生产环境应添加权限校验：仅允许内部服务调用或管理员角色才能关闭脱敏
   ```

2. **全局开关未鉴权**
   ```java
   // UserController.java
   // TODO: 安全漏洞 - 此接口未做权限校验，任意用户均可关闭全局脱敏
   // 生产环境应添加@PreAuthorize("hasRole('ADMIN')")或类似鉴权注解
   ```

### 功能限制

- ⚠️ 不支持泛型参数的完整类型推断（如 `Map<String, UserVO>` 只能对值脱敏）
- ⚠️ 不支持自定义脱敏规则配置（当前仅支持预定义策略）
- ⚠️ 不支持字段级别的脱敏条件判断（如根据用户角色决定脱敏策略）

---

## 启动方式

```bash
mvn spring-boot:run
```
