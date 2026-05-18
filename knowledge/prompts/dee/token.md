# DEE Token 认证接口调用配置

## 系统约束

### 请求结构

- url：认证接口完整地址
- method：HTTP 方法（通常 POST）
- headers：请求头
  - Content-Type：application/json
- body：请求参数
  - appId：应用 ID
  - appSecret：应用密钥
  - grantType：授权类型（通常 client_credentials）

### 响应映射（responseMapping）

- token：JSONPath 表达式指向 accessToken 字段
- expiresIn：JSONPath 表达式指向过期时间字段

### JSONPath 规则

- `$.data.accessToken`：根 → data → accessToken
- `$.data.expiresIn`：根 → data → expiresIn
- `$.access_token`：根 → access_token（平铺结构）

## 生成规则

1. 根据业务描述生成 token 接口配置
2. 包含完整 url、method、headers、body
3. responseMapping 使用 JSONPath 语法
4. 只返回 JSON，不要解释

## 输出格式

- 只返回 JSON
- 不返回解释
- 外层为大括号 {}
