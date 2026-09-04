# 宠物日常上门服务系统平台

前后端分离的宠物上门服务平台，覆盖 **用户端（宠物主人）**、**接单员端（陪护员）** 与 **管理端（运营）** 三端。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.5 |
| JDK | Eclipse Temurin | 21 |
| 持久层 | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | 9.6（脚本兼容 8.x/9.x）|
| 缓存/分布式锁 | Redis 5 + Redisson | 3.36 |
| 接口文档 | SpringDoc OpenAPI (Swagger UI) | 2.6 |
| 鉴权 | JWT (jjwt) | 0.12.6 |
| 前端框架 | Vue | 3.5 |
| 构建工具 | Vite | 5.4 |
| 状态/路由/UI | Pinia / Vue Router / Element Plus | - |
| 图表 | ECharts | 5.5 |

## 目录结构

```
Petplatform/
├── backend/                 # Spring Boot 后端（Maven）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/pet/
│       │   ├── PetPlatformApplication.java
│       │   ├── controller/  # 控制层
│       │   ├── service/     # 业务层 (+impl)
│       │   ├── mapper/      # 数据层
│       │   ├── entity/      # 数据库实体
│       │   ├── dto/         # 入参对象
│       │   ├── vo/          # 出参对象
│       │   ├── common/      # Result / 异常 / BaseEntity / 分布式锁
│       │   ├── config/      # MyBatisPlus / Redis / CORS / OpenAPI
│       │   └── security/    # JWT / 登录上下文 / 角色拦截器
│       └── resources/       # application.yml / application-dev.yml
├── frontend/                # Vue3 前端（Vite）
│   └── src/
│       ├── api/             # axios 封装 + 接口
│       ├── router/          # 三端路由 + 角色守卫
│       ├── stores/          # Pinia
│       ├── layouts/         # 前台 / 后台布局
│       └── views/           # auth user sitter admin
└── docs/sql/init.sql        # 建库建表 + 种子数据
```

## 快速开始

### 0. 前置
确保本机已安装并启动：JDK 21、Maven、MySQL 9、Redis、Node 24 / npm。

### 1. 初始化数据库
```bash
mysql -uroot -p < docs/sql/init.sql
```
> 默认库名 `pet_platform`。脚本内含种子账号（见下表）。

### 2. 启动后端
先按本机情况修改数据库/Redis 连接（可用环境变量覆盖，无需改代码）：

| 变量 | 默认值 |
|------|--------|
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DB` | localhost / 3306 / pet_platform |
| `MYSQL_USER` / `MYSQL_PASSWORD` | root / root |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | localhost / 6379 / (空) |
| `JWT_SECRET` | 内置默认值（生产务必覆盖）|

```bash
cd backend
mvn spring-boot:run
```
- 服务地址：http://localhost:8080/api
- 健康检查：http://localhost:8080/api/health （返回 MySQL / Redis 连通状态）
- 接口文档：http://localhost:8080/api/swagger-ui.html

### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```
- 访问：http://localhost:5173 （已配置 `/api` 代理到后端 8080，无需处理跨域）

## 种子账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理端 ADMIN |
| user | 123456 | 用户端 USER |
| sitter | 123456 | 接单员端 SITTER |

## 约定

- **统一响应**：所有接口返回 `Result{code,message,data,timestamp}`，`code=200` 为成功。
- **鉴权**：请求头 `Authorization: Bearer <token>`；角色控制用 `@RequireRole({"ADMIN"})`。
- **分层结构**：后端基包 `com.pet`，按 `controller / service(+impl) / mapper / entity / dto / vo` 分层；通用设施置于 `common / config / security`。
- **抢单防超卖**：使用 `common/lock/DistributedLock`（Redisson），对 `order:grab:{orderId}` 加锁。
- **逻辑删除**：实体继承 `BaseEntity`，`deleted` 字段自动逻辑删除；`create_time/update_time` 自动填充。

## 待开发模块（脚手架已预留）

宠物档案、服务选购预约、模拟支付、订单状态流转、LBS 接单大厅、上门打卡与存证、收益钱包、资质审核、订单调度、纠纷仲裁、规则配置、数据看板。
