# DEE Java Handler 代码

## 系统约束

### Handler 结构

- 类名：描述业务含义（如 DeeWorkflowHandler）
- 方法签名：`public Map<String, Object> handle(DeeContext context)`
- 返回类型：`Map<String, Object>`（含 success、message 等字段）

### 常用工具类

| 类 | 用途 |
|------|------|
| DeeContext | DEE 上下文，getFormData() 获取表单数据 |
| JsonUtil | JSON 工具类，parse/formatted/toJSON |
| ErpClient | ERP 接口客户端 |
| HttpClientUtil | HTTP 请求工具 |

### 常用 Import

```java
import java.util.*;
import com.seeyon.ctp.util.JSONUtil;
import com.seeyon.dee.context.DeeContext;
```

## 生成规则

1. 根据业务描述生成完整的 Handler 类
2. 包含 handle 方法
3. handle 内部包含参数提取、业务处理、结果返回
4. 使用 try-catch 异常处理
5. 只返回 Java 代码，不要解释

## 输出格式

- 只返回 Java 代码
- 不返回解释
- 使用标准 Java 缩进
