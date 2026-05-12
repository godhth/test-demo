# WeBox 企业员工餐食订购平台 设计文档

- 日期：2026-05-12
- 项目：1 小时 AI 辅助开发挑战（WeBox）
- 参考：`doc/weboxPrd.md`

## 1. 目标与范围

按 PRD 全量实现：核心功能（注册/登录、菜单浏览、菜品详情、购物车、下单、订单列表/详情）+ 加分项 A（用户偏好过滤）+ 加分项 B（Qwen AI 自然语言推荐）。

## 2. 技术选型

- Java 17 + Maven
- Spring Boot 3.2.x：web、thymeleaf、data-jpa、validation
- H2 内存库（进程内，启动注入示例菜品）
- Thymeleaf 服务端渲染；少量原生 JS（购物车异步交互、AI 对话）
- 自制 `HandlerInterceptor` 鉴权（不引入 Spring Security）
- 密码：SHA-256 + 每用户独立盐（JDK `MessageDigest` + `SecureRandom`）
- AI：DashScope OpenAI 兼容端点，模型 `qwen-plus`，通过 Spring `RestClient` 调用
- 配置：API Key 走环境变量 `DASHSCOPE_API_KEY`
- 包名：`com.webox.webox`；端口 `8080`

## 3. 系统架构

单一 Spring Boot 可执行 jar，嵌入 Tomcat。分层：

```
controller  → HTTP 入口，返回 Thymeleaf 视图或 JSON
service     → 业务编排
repository  → Spring Data JPA
entity      → JPA 实体
dto         → 表单 / API 载体
interceptor → 登录拦截器
config      → WebMvcConfigurer、RestClient、数据初始化
```

## 4. 数据模型

### 4.1 User
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 自增主键 |
| email | String | 唯一索引，登录凭证 |
| name | String | 昵称 |
| passwordHash | String | SHA-256 hex |
| passwordSalt | String | 16B 随机，base64 |
| createdAt | Instant | 注册时间 |

### 4.2 MenuItem
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| code | String | 业务编码（如 `item_001`），唯一 |
| name, description, category, image | String | — |
| price | BigDecimal | — |
| allergens | String | 逗号分隔：`peanut,dairy` |

### 4.3 UserPreference（加分项 A）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | — |
| userId | Long | 外键逻辑关联 |
| allergens | String | 逗号分隔过敏原 |
| recommendOnly | Boolean | 菜单"推荐/全部"开关持久态 |

### 4.4 Order
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | — |
| userId | Long | — |
| totalAmount | BigDecimal | 服务端重算 |
| deliveryDate | LocalDate | — |
| mealPeriod | String | `lunch` / `dinner` 等 |
| deliveryAddress | String | — |
| status | String | 初版固定 `CREATED` |
| createdAt | Instant | — |
| items | `List<OrderItem>` | `@OneToMany` 级联保存 |

### 4.5 OrderItem
`id, orderId, menuItemId, name, price, quantity`

### 4.6 购物车
不入库，存 `HttpSession`：`Map<Long menuItemId, Integer quantity>`。下单成功清空。

### 4.7 初始数据
PRD 第 7 节的 8 道菜，`image` 写本地 `/img/placeholder-*.svg`。由 `DataInitializer`（`ApplicationRunner`）于启动时写入，避免 `data.sql` 与 JPA schema 时序问题。

## 5. URL 路由

### 5.1 页面
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/` | 未登录→`/login`；已登录→`/menu` |
| GET/POST | `/register` | 注册 |
| GET/POST | `/login` | 登录 |
| POST | `/logout` | 清 session |
| GET | `/menu` | 主页，参数 `category`、`onlyRecommend` |
| GET | `/menu/{code}` | 菜品详情 |
| POST | `/cart/add` `/cart/update` `/cart/remove` | 购物车操作，重定向回来源 |
| GET | `/cart` | 购物车页 |
| GET/POST | `/checkout` | 下单 |
| GET | `/orders/success` | 下单成功 |
| GET | `/orders` | 订单列表（时间倒序） |
| GET | `/orders/{id}` | 订单详情 |
| GET/POST | `/preferences` | 偏好设置 |

### 5.2 JSON API
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/ai/recommend` | 入参 `{query}`；出参 `{items:[{code,name,price,reason}], raw}` |

### 5.3 拦截
`AuthInterceptor` 放行：`/`、`/login`、`/register`、`/css/**`、`/js/**`、`/img/**`、`/h2-console/**`、静态错误页。其余（含 `/api/ai/**`）要求登录。

## 6. 关键业务逻辑

### 6.1 注册 / 登录
- 注册：email 格式正则、密码 ≥ 6、email 唯一
- 密码：`salt = SecureRandom 16B` → base64；`hash = SHA-256(salt||password)` → hex
- 登录：按 email 取用户、同盐哈希比对；成功写 session `LoginUser{userId,email,name}`
- 失败回表单页并展示字段错误

### 6.2 菜单
- `MenuService.list(category, onlyRecommend, userId)`
- `category` 非空时过滤
- `onlyRecommend=true` + 用户已设偏好：剔除命中任一过敏原的菜品
- 全量模式下菜品含过敏原时页面打"⚠ 含 XX"标签

### 6.3 购物车
Session 态。`CartService` 封装 `add/update/remove/clear/view`；`view` 返回 `{items:[{menuItem,quantity,subtotal}], total}`。

### 6.4 下单
- 校验购物车非空、菜品仍存在
- 金额服务端重算；`status = CREATED`
- 级联保存；清空购物车；跳转成功页

### 6.5 偏好
- upsert：按 `userId` 查，存在即更新
- 过敏原候选固定：`peanut, shellfish, dairy, egg, gluten, fish, soy`

### 6.6 AI 推荐
- Prompt 组装
  - system：角色定位 + 严格 JSON 输出 schema
  - user：`{用户诉求, 用户过敏原, 当日菜单(code/name/description/price/category/allergens)}`
- HTTP
  - `POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
  - `Authorization: Bearer ${DASHSCOPE_API_KEY}`
  - body：`model=qwen-plus`，`response_format={type:"json_object"}`，`messages=[system,user]`
  - 超时：连接 5s / 读取 20s
- 响应解析
  - 读 `choices[0].message.content` → 反序列化为 `{recommendations:[{code,reason}]}`
  - 按 `code` 回查 MenuItem，过滤幻觉，保留 3–5 条
  - 不足 3 条时用本地评分回退（类别匹配 + 无过敏原命中）
- 失败兜底（Key 缺失 / 网络异常 / JSON 解析失败）：返回 `{error, items:[]}`，前端提示"AI 暂不可用，请稍后再试"

### 6.7 错误处理
`@ControllerAdvice`：
- `BusinessException` → 友好页或字段回显
- 其他 → `error/500.html`
- 404 → `error/404.html`

### 6.8 日志
SLF4J；AI 调用打耗时与请求摘要，**绝不**打印 API Key。

## 7. 工程结构

```
src/main/java/com/webox/webox/
├── WeboxApplication.java
├── config/            WebMvcConfig, RestClientConfig, DataInitializer
├── interceptor/       AuthInterceptor
├── controller/        Home/Auth/Menu/Cart/Checkout/Order/Preference/AiRecommend + GlobalExceptionHandler
├── service/           User/Menu/Cart/Order/Preference + PasswordEncoder
│   └── ai/            AiRecommendService, QwenClient, PromptBuilder
├── repository/        User/MenuItem/Order/UserPreference
├── entity/            User, MenuItem, Order, OrderItem, UserPreference
├── dto/               LoginForm, RegisterForm, CheckoutForm, PreferenceForm,
│                      AiRecommendRequest/Response, CartView
├── model/             LoginUser
└── support/           BusinessException, SessionKeys

src/main/resources/
├── application.yml
├── templates/         layout + login/register + menu/{list,detail} + cart + checkout +
│                      order/{success,list,detail} + preferences + error/{404,500,business}
└── static/            css/app.css, js/{cart,ai}.js, img/placeholder-*.svg
```

### 7.1 `application.yml`
```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:h2:mem:webox;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
  jpa:
    hibernate.ddl-auto: update
    show-sql: false
  h2.console.enabled: true
webox:
  ai:
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${DASHSCOPE_API_KEY:}
    model: qwen-plus
    timeout-ms: 20000
```

## 8. 测试策略

### 8.1 单元测试
- `PasswordEncoderTest`：相同密码不同盐→不同哈希；校验成功/失败
- `MenuServiceTest`：偏好过滤生效；`onlyRecommend=false` 返回全量
- `CartServiceTest`：add/update/remove/clear/合计；重复 add 数量累加
- `OrderServiceTest`：金额服务端重算、伪造客户端金额无效；下单后购物车清空
- `PromptBuilderTest`：prompt 含菜单 / 用户诉求；无偏好时不注入偏好段

### 8.2 Web 层（`@WebMvcTest` / `MockMvc`）
- `AuthControllerTest`：错误回显；登录成功跳 `/menu`
- `AuthInterceptorTest`：未登录访问 `/menu` 重定向 `/login`；静态资源不拦截
- `AiRecommendControllerTest`：mock `QwenClient` 返回 3 条；Key 缺失走兜底

### 8.3 集成冒烟（`@SpringBootTest`）
注册 → 登录 → 菜单 → 加购 → 下单 → 订单可查

### 8.4 外部调用
AI 一律 mock；手动验收时设置真实 `DASHSCOPE_API_KEY`。

### 8.5 手动验收清单
- [ ] 未登录首页重定向登录
- [ ] 注册错误态正确提示
- [ ] 菜单筛选 / 推荐开关生效
- [ ] 菜品详情可加购
- [ ] 购物车数量编辑、移除、合计正确
- [ ] 下单后订单可查
- [ ] 偏好保存后菜单过滤随之变化
- [ ] AI 入口 Key 缺失时提示；Key 正常时返回 3–5 条

## 9. 非功能约束

- 安全：密码不明文；API Key 不落代码与日志；session 只存必要用户摘要
- 性能：H2 内存库 + 单进程，demo 级别足够；AI 调用设置超时
- 可运行性：`./mvnw spring-boot:run` 即可启动；无需任何额外环境（AI 功能需要 Key）

## 10. 非目标（YAGNI）

- 不做支付 / 退款 / 优惠券
- 不做多角色（管理员）与菜品后台编辑
- 不做真实图片上传；仅本地占位 SVG
- 不做国际化、不做响应式高阶适配；桌面端可用即可
- 不引入 Spring Security、Lombok、BCrypt 等额外依赖

## 11. 后续

下一步：用 `superpowers:writing-plans` 技能把本设计拆成可执行的实施计划（分任务、注明验证命令与完成判定）。
