# 企业广告管理系统 - 微信小程序

> 与 PC 端共享同一 Spring Boot 后端，JWT 鉴权，提供移动端核心业务操作。

## 项目结构

```
miniapp/
├── package.json                  # 依赖：tdesign-miniprogram ^1.9.0, TypeScript
├── project.config.json           # 微信开发者工具配置
├── tsconfig.json                 # TypeScript 配置 (strict)
└── miniprogram/                  # 小程序源码目录
    ├── app.ts                    # 入口：全局数据、登录检查、baseUrl
    ├── app.json                  # 路由 + tabBar + 窗口配置
    ├── app.wxss                  # 全局样式 (CSS 变量 --primary: #0052D9)
    ├── sitemap.json
    ├── images/                   # tabBar 图标 (8个 PNG)
    ├── components/               # 自定义组件 (预留)
    ├── utils/
    │   ├── config.ts             # baseUrl / timeout / appId 配置
    │   ├── request.ts            # 统一请求封装 (JWT 注入、401 拦截、upload)
    │   ├── auth.ts               # 登录/登出/token 管理
    │   └── helpers.ts            # 金额格式化、日期格式化、状态映射
    └── pages/
        ├── index/                # Tab1 - 工作台
        ├── orders/               # Tab2 - 订单管理
        │   ├── create/           # 创建订单
        │   └── detail/           # 订单详情 (收款/取消)
        ├── customer/             # Tab3 - 客户管理
        │   ├── create/           # 新建客户
        │   ├── detail/           # 客户详情 (会员/订单历史)
        │   └── recharge/         # 会员充值
        ├── messages/             # Tab4 - 消息中心
        ├── quote/                # 印刷报价计算器
        └── login/                # 登录页
```

## 页面清单 (12个)

| 页面 | 路径 | 说明 |
|------|------|------|
| 工作台 | `/pages/index/index` | 今日统计、快捷操作、待办列表、最近订单 |
| 订单列表 | `/pages/orders/orders` | 搜索 + 状态筛选 + 分页加载 |
| 创建订单 | `/pages/orders/create/create` | 客户选择 + 物料明细 + 自动算总价 |
| 订单详情 | `/pages/orders/detail/detail` | 状态时间线 + 收款记录 + 取消操作 |
| 客户列表 | `/pages/customer/customer` | 搜索 + 类型筛选(普通/会员) |
| 新建客户 | `/pages/customer/create/create` | 名称/手机/类型/地址/备注表单 |
| 客户详情 | `/pages/customer/detail/detail` | 会员状态卡 + 升级会员 + 历史订单 |
| 会员充值 | `/pages/customer/recharge/recharge` | 余额展示 + 快捷金额 + 自定义输入 |
| 消息中心 | `/pages/messages/messages` | 待办事项 + 系统公告双 Tab |
| 印刷报价 | `/pages/quote/quote` | 标签印刷 + 打印报价(阶梯价) |
| 登录 | `/pages/login/login` | 账号密码 + 微信一键登录 |

## tabBar 布局

```
工作台 (index)  |  订单 (orders)  |  客户 (customer)  |  消息 (messages)
```
- 主色调：#0052D9
- 图标：images/tab-*.png / tab-*-active.png (占位 PNG，需替换)

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | TypeScript 5.4 |
| UI 组件库 | TDesign 小程序版 ^1.9.0 |
| 认证方式 | JWT Bearer Token (与 PC 端共享) |
| 后端接口 | 复用现有 Spring Boot API |
| 请求封装 | utils/request.ts (自动注入 Token, 401 跳登录) |

## API 对接关系

小程序直接调用 PC 端已有的后端接口，无需额外开发接口（除微信登录）：

| 功能 | 接口 | 方法 |
|------|------|------|
| 账号登录 | `/auth/login` | POST |
| **微信登录** | `/auth/wx-login` | POST (需新建) |
| 订单列表 | `/order/list` | GET |
| 创建订单 | `/order/add` | POST |
| 订单详情 | `/order/{id}` | GET |
| 收款记录 | `/order/payment` | POST |
| 取消订单 | `/order/cancel/{id}` | PUT |
| 客户列表 | `/customer/list` | GET |
| 新建客户 | `/customer/add` | POST |
| 客户详情 | `/customer/{id}` | GET |
| 会员充值 | `/member/recharge` | POST |
| 待办列表 | `/todo/list` | GET |
| 待办完成 | `/todo/complete/{id}` | PUT |
| 公告列表 | `/notice/list` | GET |
| 仪表盘统计 | `/dashboard/board` | GET |

## 微信登录流程 (需后端配合)

```
小程序 wx.login() → 获取 code → POST /auth/wx-login {code}
                                        ↓
后端 code2session → 获取 openid → 匹配/创建用户 → 返回 JWT
                                        ↓
小程序存储 token → wx.setStorageSync('token', token)
```

后端需新增的接口：
```java
@PostMapping("/auth/wx-login")
public Result<LoginVO> wxLogin(@RequestBody WxLoginRequest request) {
    // 1. 调用微信 code2session 换取 openid
    // 2. 根据 openid 查找已绑定用户，未绑定则创建
    // 3. 生成 JWT 返回
}
```

## 部署前必做

1. **替换 AppID**
   - `project.config.json` → `appid` 字段
   - `utils/config.ts` → `appId` 字段
   - 微信公众平台 → 开发设置 → 获取 AppID

2. **配置后端地址**
   - `miniprogram/app.ts` → `globalData.baseUrl`
   - `miniprogram/utils/config.ts` → `config.baseUrl`
   - 开发环境：`http://localhost:8080`
   - 生产环境：`https://你的域名`

3. **后端配置**
   - 新增 `POST /auth/wx-login` 接口
   - CORS 白名单加入小程序请求域名
   - 配置微信 AppID 和 AppSecret

4. **构建 npm**
   - 微信开发者工具 → 工具 → 构建 npm
   - 确保 `packNpmManually: true` 和 `miniprogramNpmDistDir` 正确

5. **替换 tabBar 图标**
   - 当前 `images/tab-*.png` 为占位图，需替换为设计稿图标
   - 尺寸建议：81×81px，未选中灰色、选中蓝色

6. **微信公众平台配置**
   - 开发管理 → 服务器域名 → request 合法域名
   - 添加后端 API 域名 (仅 HTTPS)
   - 开发阶段可在开发者工具勾选"不校验合法域名"

## 开发注意事项

- **请求拦截**：`utils/request.ts` 已处理 401 自动跳转登录页，页面层无需重复判断
- **Token 存储**：使用 `wx.setStorageSync`，每个页面通过 `request.ts` 自动携带
- **分页格式**：后端返回 `PageResult { records, total, current, size }`，前端已适配
- **订单状态**：0=待确认, 1=进行中, 2=已完成, 3/4=已取消
- **支付状态**：1=未付, 2=部分付, 3=已付清, 4=抹零
- **报价计算**：标签印刷(纸张/尺寸/数量/单价) 和 打印报价(黑白/彩色阶梯价) 两种模式
