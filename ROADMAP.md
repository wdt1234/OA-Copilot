# ROADMAP

# Phase 1 - MVP ✅ 完成

- 项目方向确认、系统原型设计
- Vue3 + Vite + Element Plus 集成
- 后台 Layout（侧边栏 + 顶栏 + 路由）
- Dashboard 页面
- SQL Copilot（前端 + 后端 + 前后端联调 + AI 接入）
- DEE 模板生成（前端 + 后端 + 前后端联调 + AI 接入）
- 字段映射助手（前端 + 后端 + 前后端联调 + AI 接入）
- AiService 封装（可配置 API，兼容 OpenAI 格式）
- UTF-8 编码修复、Markdown 剥离、日志优化

---

# Phase 2 - 企业知识资产沉淀

## 当前开发

重点不是扩页面，而是沉淀企业上下文知识，让 AI 后续生成基于企业规范而非泛化 Prompt。

### 知识资产目录结构

```
knowledge/
├── data_dictionary/     # 数据字典（OA 表单字段、状态、业务语义）
├── dee_templates/       # DEE 模板资产库（workflow/token/json/java）
├── sql_templates/       # SQL 模板资产库（标准关联链路、ID 转名称、业务场景）
├── prompts/             # Prompt 资产库
│   ├── sql/
│   ├── workflow/
│   └── dee/
├── api_docs/            # API 文档资产库（workflow/token/header）
└── error_cases/         # 错误案例库（DEE/接口/SQL 常见问题）
```

### 核心产出

- 数据字典：表单字段含义、主从表关系、状态语义
- SQL 模板：标准关联链路 + ID 转名称 join 模板
- DEE 模板：4 类任务模板沉淀
- Prompt 文件化：从代码中抽出，按场景分类
- API 文档：接口文档自动生成器（6-Sheet Excel 模板 + 历史记录）
- 错误案例：常见问题与解决方案
- 时间过滤规范：START_DATE 作为默认时间过滤字段（所有主表通用）

### 禁止事项

- 不做 RAG
- 不做 Agent
- 不做微服务
- 不自动执行 SQL
- 不自动修改企业数据
- 不重构现有结构

### 当前完成状态

- ✅ SQL Copilot：数据字典 + 系统表 + 业务场景模板
- ✅ 接口文档：6-Sheet Excel 模板生成 + 历史记录
- ⏳ DEE 任务：4 模板 + 4 Prompt（需真实业务验证）
- ❌ 错题库：待开发

---

# Phase 3 - 系统优化

- SQLite → MySQL 迁移
- 日志系统（前端可查、可筛选、可导出）
- 终端编码问题根治
- Prompt 调优（基于实际使用反馈）
- 历史记录管理增强
- 前端 UI/UX 重构（现代简洁风格）

---

# Phase 4 - AI 增强

- Swagger 解析
- 错误智能分析
- AI 对话辅助
