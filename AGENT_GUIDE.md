# 宠物日常上门服务平台开发规范

## 1. 技术栈约束
- 后端：Spring Boot 3.3.5 + Java 21 + MyBatis-Plus 3.5.9 + MySQL connector-j 9.1 + Redisson 3.36 + SpringDoc OpenAPI 2.6 + JWT(jjwt 0.12)
- 前端：Vue 3 + Vite 5 + Pinia + Vue Router + Element Plus + Axios, 高德地图 JS API 2.0
- 架构风格：前后端分离 RESTful API，统一返回体 `Result<T>`

## 2. 后端代码分层与规范
- 控制层：`com.pet.controller.*`（参数校验、路由映射）
- 业务层：`com.pet.service.*` 及 `impl`
- 数据层：`com.pet.mapper.*`
- 模型层：`entity`（数据库实体）、`dto`（入参对象）、`vo`（出参对象）
- 统一响应：`Result.success(data)` / `Result.fail(code, msg)`
- 统一异常处理：`GlobalExceptionHandler` 处理 `BusinessException`

## 3. Git 提交规范
每次完成一个独立功能并验证无误后，生成 Conventional Commits 格式的提交说明（例如：`feat(order): 新增基于Redis GEO的附近订单检索接口`）。
