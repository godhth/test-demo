# AI Vibe Coding
## WeBox - 企业员工餐食订购平台 产品需求文档 (PRD)
创建日期：2/26/2026
项目：1小时AI辅助开发挑战

---

## 1. 产品简介
WeBox 是一个企业员工餐食订购平台。员工可以浏览每日菜单、加入购物车、下单订餐。平台支持个性化偏好设置与 AI 智能推荐，提升选餐效率。

---

## 2. 需求分层
本项目按 1 小时开发周期设计，需求分为三层：

| 模块 | 功能 | 预期耗时  |
| --- | --- |-------|
| 核心功能 | 注册/登录、菜单浏览、购物车、下单 | 35min |
| 加分项A | 用户偏好设置 | 10min |
| 加分项B | AI 智能推荐 | 15min |

核心功能为必须完成项，加分项按顺序挑战。

---

## 3. 核心功能
### 3.1 注册 / 登录
使用 Email 作为唯一身份标识，实现最简认证流程。
- Email+密码登录，登录成功后进入菜单页

### 3.2 菜单浏览
展示当日可订购的菜品列表，用户可查看菜品信息。
- 菜单列表
- 菜品详情
- 分类筛选

### 3.3 购物车
用户可将菜品加入购物车，编辑数量，删除菜品。
- 移除单个菜品
- 价格合计

### 3.4 下单
用户在结算页确认配送信息后提交订单。
- 订单记录

---

## 4. 加分项 A - 用户偏好设置
允许用户配置个人饮食偏好，菜单据此自动匹配与过滤。
- 多选过敏原（花生、海鲜、乳制品、鸡蛋、麸质等），含过敏原的菜品标记警告
- 菜单过滤：根据偏好自动过滤/排序，提供开关切换“全部/推荐”

---

## 5. 加分项 B - AI 智能推荐
通过自然语言输入，AI 解析用户意图并推荐菜品。
- 输入入口
- 自然语言解析
- 返回3-5个推荐菜品，附推荐理由
- 加入购物车

**AI 推荐参考 Prompt 策略**
1. 输入：用户自然语言 + 用户偏好(可选) + 当日可用菜单
2. 输出：推荐菜品列表 + 推荐理由

---

## 6. 基础数据模型
### User（用户）
```json
{
  "id": "string",
  "email": "string",
  "name": "string",
  "password": "string",
  "created_at": "timestamp"
}
```
### MenuItem（菜品）
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "price": "number",
  "image": "string",
  "category": "string",
  "allergens": "string[]"
}
```
### Order（订单）
```json
{
  "id": "string",
  "user_id": "string",
  "items": "OrderItem[]",
  "total_amount": "number",
  "delivery_date": "string",
  "meal_period": "string",
  "delivery_address": "string",
  "status": "string",
  "created_at": "timestamp"
}
```
### OrderItem（订单项）
```json
{
  "menu_item_id": "string",
  "name": "string",
  "price": "number",
  "quantity": "number"
}
```
## 7. 菜品示例数据
```json
[
  {
    "id": "item_001",
    "name": "宫保鸡丁",
    "description": "经典川菜,鸡肉搭配花生、干辣椒爆炒",
    "price": 22,
    "image": "https://example.com/kungpao.jpg",
    "category": "chinese",
    "allergens": ["peanut"]
  },
  {
    "id": "item_002",
    "name": "凯撒沙拉",
    "description": "新鲜罗马生菜配帕玛森芝士与凯撒酱",
    "price": 28,
    "image": "https://example.com/caesar.jpg",
    "category": "salad",
    "allergens": ["dairy", "egg"]
  },
  {
    "id": "item_003",
    "name": "三文鱼刺身定食",
    "description": "新鲜三文鱼刺身搭配米饭、味噌汤",
    "price": 45,
    "image": "https://example.com/sashimi.jpg",
    "category": "japanese",
    "allergens": ["fish"]
  },
  {
    "id": "item_004",
    "name": "番茄意面",
    "description": "经典意式番茄酱意大利面配新鲜罗勒",
    "price": 26,
    "image": "https://example.com/pasta.jpg",
    "category": "western",
    "allergens": ["gluten"]
  },
  {
    "id": "item_005",
    "name": "冬阴功汤",
    "description": "泰式酸辣虾汤配香茅、南姜、柠檬叶",
    "price": 32,
    "image": "https://example.com/tomyum.jpg",
    "category": "southeast_asian",
    "allergens": ["shellfish"]
  },
  {
    "id": "item_006",
    "name": "鸡胸肉藜麦碗",
    "description": "低脂高蛋白,烤鸡胸配藜麦、牛油果、时蔬",
    "price": 35,
    "image": "https://example.com/quinoa.jpg",
    "category": "salad",
    "allergens": []
  },
  {
    "id": "item_007",
    "name": "麻婆豆腐",
    "description": "四川经典,嫩豆腐配麻辣肉末",
    "price": 18,
    "image": "https://example.com/mapo.jpg",
    "category": "chinese",
    "allergens": ["soy"]
  },
  {
    "id": "item_008",
    "name": "韩式拌饭",
    "description": "石锅拌饭配各式时蔬、煎蛋与辣酱",
    "price": 30,
    "image": "https://example.com/bibimbap.jpg",
    "category": "korean",
    "allergens": ["egg", "soy"]
  }
]
```
## 8. 页面结构参考
```
plaintext
├── 注册页 /register
├── 登录页 /login
├── 菜单页 /menu ← 主页面，含筛选、AI 推荐入口
├── 菜品详情 /menu/:id
├── 购物车 /cart
├── 下单确认 /checkout
├── 订单成功 /order/success
├── 订单列表 /orders
├── 订单详情 /orders/:id
└── 偏好设置 /preferences ← 加分项 A
```