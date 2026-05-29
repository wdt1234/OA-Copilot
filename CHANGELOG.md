# CHANGELOG

# 2026-05-29

## 日志系统全面优化

### 性能优化

- 大文件读取：使用倒序读取 + 提前终止，避免全量加载
- 支持大文件快速分页浏览

### 新增功能

- 日志统计卡片：总日志数 / ERROR / WARN / INFO / 文件大小
- 日志导出：完整导出 + 筛选导出
- ERROR 日志单独输出到 error.log（保留30天）

### UI 优化

- 新增统计卡片区域
- 添加导出按钮
- 优化分页显示

### 日志格式

- 添加线程名便于排查并发问题
- 优化控制台输出格式

---

# 2026-05-29

## 系统运行日志页面 + 复制功能修复

### 系统运行日志页面

- 新增 SystemLog.vue 前端页面
- 支持查看多个日志文件（app.log + 按日期归档）
- 支持按日志级别筛选（ERROR/WARN/INFO/DEBUG）
- 支持关键词搜索
- 支持分页浏览
- 支持自动刷新（5秒间隔）
- 支持清除和删除日志文件
- 后端新增 LogService + LogController
- logback 配置添加文件输出（logs/app.log）

### 复制功能修复

- 修复非 localhost 环境下复制 SQL 失败的问题
- 添加 Clipboard API fallback 支持
- 使用 document.execCommand('copy') 作为备选方案

### 涉及文件

- backend: LogService.java, LogController.java（新增）
- backend: logback-spring.xml（添加文件输出）
- frontend: SystemLog.vue（新增页面）
- frontend: router/index.js（新增路由）
- frontend: AdminLayout.vue（修正菜单路径）
- frontend: SqlCopilot.vue（复制功能修复）

---

# 2026-05-29

## 快捷模板云端同步 + AI Prompt 管理页面

### 快捷模板云端同步

- 快捷模板从 localStorage 改为后端数据库存储（`shortcut_template` 表）
- 新增 ShortcutTemplate 实体类 + Mapper + Controller
- 支持增删改查、重置默认、批量删除
- 前端 SqlCopilot.vue 改为从后端 API 加载/保存模板
- 解决多设备模板不同步问题

### AI Prompt 管理页面

- 新增 PromptManager.vue 前端页面
- 支持按分类查看 Prompt 模板（SQL / DEE）
- 支持编辑 Prompt 内容并保存
- 后端新增 `PUT /api/knowledge/prompts/{path}` 接口
- KnowledgeService 新增 updatePrompt() 方法
- 支持保存到外部目录（prompts/），修改后可持久化
- 侧边栏菜单更新：AI Prompt 管理指向正确路径

### 涉及文件

- backend: schema.sql（新增 shortcut_template 表）
- backend: ShortcutTemplate.java, ShortcutTemplateMapper.java, ShortcutTemplateController.java
- backend: KnowledgeController.java（新增 updatePrompt 接口）
- backend: KnowledgeService.java（新增 updatePrompt 方法 + 优先加载外部目录）
- frontend: SqlCopilot.vue（模板改为云端同步）
- frontend: PromptManager.vue（新增页面）
- frontend: router/index.js（新增路由）
- frontend: AdminLayout.vue（修正菜单路径）

---

# 2026-05-29

## 系统优化 + 优先级规划

### 性能优化

- 移除 JOIN 时的默认过滤条件（ORG_MEMBER/ORG_UNIT/ORG_POST/CTP_ENUM_ITEM）
- 查询时间从 15s 降到 1.5s
- 用户需要查历史数据，包括已离职/失效的人员、部门、枚举

### 后端接口

- 新增 `/api/knowledge/system-tables` 接口（获取系统表列表）
- 新增 `/api/knowledge/prompts` 接口（获取 Prompt 模板列表）

### 前端优化

- 系统表数据字典穿透功能（点击弹出抽屉）
- AI 星形图标添加（Logo、标题、按钮）
- 快捷模板样式优化（蓝色加粗 → 轻盈胶囊形状）

### 优先级规划

- 更新 TASKS.md，明确开发优先级
- 立即执行：AI Prompt 管理、系统运行日志、系统设置
- 短期：DEE 验证、错题库、Prompt 调优
- 中期：MySQL 迁移、日志系统、编码问题

---

# 2026-05-28

## 前端 UI/UX 重构（现代简洁风格）

### 设计风格升级

- 侧边栏：深色背景 → 浅色背景 + 线性图标
- 卡片：毛玻璃效果 → 纯白 + 轻阴影
- 主色调：蓝色（#3b82f6）
- 圆角：中等（12-16px）
- 整体风格：简洁、现代、专业

### 侧边栏菜单重构

新菜单结构：
```
Dashboard
AI Copilot
├── SQL Copilot
├── 接口文档生成
├── DEE 代码生成

知识资产
├── 系统知识库
├── AI Prompt 管理

系统管理
├── 系统运行日志
├── 系统设置
```

### SQL Copilot 页面重构

- 布局调整：左（输入区）+ 中（结果区）+ 右（历史记录 + 快捷示例）
- 智能生成 SQL 卡片：
  - 标题区：智能生成 SQL + 系统表状态
  - 推荐问题 chips
  - 输入框
  - 操作栏：选择表单 + 生成 SQL + 清空
- 生成结果卡片：
  - 标题区：生成结果 + SQL 标签 + 复制按钮
  - 代码区域：行号 + SQL 语法高亮
  - 底部状态栏：SQL 生成成功！共 X 行
- 右侧面板：
  - 历史记录 + 查看全部
  - 快捷示例 + 换一批功能

### 新增功能（仅前端）

1. **快捷示例换一批**：预设多组示例，点击换一批随机展示
2. **系统表状态显示**：静态显示已录入的数据字典数量（7 张系统表已就绪）

### 样式统一

- 所有页面卡片样式统一
- 按钮样式统一
- 表单样式统一
- 移除毛玻璃效果

### 涉及文件

- frontend: style.css（设计系统）
- frontend: AdminLayout.vue（侧边栏布局）
- frontend: SqlCopilot.vue（SQL Copilot 页面）
- frontend: DeeCopilot.vue（样式统一）
- frontend: ApiDocGenerator.vue（样式统一）
- frontend: FieldMapper.vue（样式统一）
- frontend: DataDictionary.vue（样式统一）
- frontend: Dashboard.vue（样式统一）
- frontend: HistoryPanel.vue（全局组件优化）

### 约束验证

- ✅ 仅重构前端 UI/UX
- ✅ 未修改后端接口
- ✅ 未修改 API 参数结构
- ✅ 未修改业务逻辑
- ✅ 未影响现有 SQL Copilot 功能

---

# 2026-05-28

## 接口文档自动生成器（完整功能）

### 功能说明

基于数据字典自动生成标准化 Excel 接口文档，包含 6 个 Sheet 页：
1. **接口基础信息** - 自动填入配置信息（接口编码/名称/类型/联系人等）
2. **Collection** - 留空，自行插入 Postman JSON
3. **Data Mapping** - 从数据字典自动生成字段映射表
4. **Get Token** - 自动生成 Token 请求示例（mule/direct 两种模式）
5. **RESTful API** - 自动生成业务接口请求示例（mule/direct 两种模式）
6. **OA常见报错案例** - 8 种常见错误场景参考

### 后端实现

- ApiDocService.java（~1560行）：Excel 生成核心逻辑
- 支持两种连接方式：过 Mule（外部系统）/ 直连（内部OA系统）
- Apache POI (XSSFWorkbook) 生成 .xlsx
- 样式系统：labelStyle/valueStyle/codeStyle/apiLabelStyle 等 10+ 种样式
- Mule 模式：固定 Token URL/Workflow URL/client_id/client_secret（Excel 模板写死）
- 直连模式：OA workflow 标准调用模板

### 前端实现

- ApiDocGenerator.vue：配置表单 + 生成预览 + 历史记录
- 数据字典远程搜索选择
- 接口类型下拉（新增接口/修改接口）
- 连接方式单选（直连/过Mule）
- 源系统/联系人配置
- 历史记录面板：置顶/删除/批量删除/点击加载

### 历史记录功能

- api_doc_history 表（SQLite）
- ApiDocHistory 模型 + Mapper + XML
- REST API：GET /history, PUT /{id}/pin, DELETE /{id}, DELETE /batch
- 生成 Excel 时自动保存历史记录
- 前端：选择模式、全选、批量删除、点击加载配置

### 涉及文件

- backend: ApiDocService.java, ApiDocController.java, ApiDocHistory.java, ApiDocHistoryMapper.java/xml
- backend: schema.sql（新增 api_doc_history 表）
- frontend: ApiDocGenerator.vue

---

# 2026-05-25

## 启动/停止脚本窗口管理优化

### 问题描述

- stop.bat 无法关闭前后端终端窗口（只能杀进程，窗口卡在"请按任意键继续"）
- `exit` 命令在双击运行 .bat 时无效
- `taskkill /FI "WINDOWTITLE"` 窗口标题匹配不可靠

### 解决方案

**start.bat 优化：**
- 添加自动最小化功能（启动后主窗口自动最小化）
- 使用 `start /min "OA-Backend" cmd /k "title OA-Backend && ..."` 启动前后端
- 窗口标题明确设置为 `OA-Backend` 和 `OA-Frontend`
- 执行完自动关闭主窗口

**stop.bat 优化：**
- 使用 PowerShell `Get-WmiObject Win32_Process` 按命令行内容匹配
- 精准关闭：匹配 `spring-boot`（后端）和 `npm.*dev`（前端）
- 备用方案：按端口 + 进程名杀进程
- 自动最小化执行

### 关键技术点

- `taskkill /FI "WINDOWTITLE"` 不可靠（cmd 窗口标题可能被进程覆盖）
- PowerShell WMI 按 CommandLine 匹配更精准
- 双击 .bat 时 `exit` 不关闭窗口，需用 `start /min` + `exit` 组合

### 涉及文件

- start.bat（自动最小化 + 窗口标题设置）
- stop.bat（PowerShell 精准关闭 + 备用清理）

---

# 2026-05-20

## SQL Copilot 优化 + Bug 修复

### Oracle 11g 别名截取修复

- KnowledgeService.shortenAlias() 改为直接截取前 10 个汉字
- 不再做智能前缀去除，避免误截业务关键词
- 验证："最新版控制计划轴力标准长度要求" → "最新版控制计划轴"（10字）

### 日期过滤逻辑修复

- 用户未提及时间时，不再添加 START_DATE 过滤条件
- 原规则：`无时间指定 → 默认使用系统当天日期`（过于激进）
- 新规则：`用户未提及时间 → 不加时间过滤条件`
- 更新位置：prompts/sql/form_query.md 第186行

### 历史记录 UI 优化

- 置顶/删除按钮改为仅鼠标悬停时显示（absolute 定位）
- 历史记录内容区占满宽度，悬停时显示完整描述（el-tooltip）
- 三个页面（SqlCopilot/DeeCopilot/FieldMapper）统一优化

### SQL Copilot 快捷模板改为可编辑

- 移除「从历史记录存为模板」功能（UI 拥挤）
- 快捷模板支持：新增、编辑、删除、设为默认、恢复默认
- 模板存储在 localStorage（key: sql_copilot_quick_templates）
- 仅 SQL Copilot 页面，DEE 和字段映射暂不添加

### 启动脚本修复

- start.bat：PowerShell 最小化窗口 + 子进程静默启动 + 执行完自动关闭
- stop.bat：0.5s 自动关闭（ping -n 1 -w 500）

### 涉及文件

- backend: KnowledgeService.java（shortenAlias 修复）
- backend: knowledge/prompts/sql/form_query.md（日期规则修复）
- backend: SqlCopilotController.java, DeeController.java, FieldMappingController.java（batch delete 路由修复）
- frontend: SqlCopilot.vue, DeeCopilot.vue, FieldMapper.vue（UI 优化）
- start.bat, stop.bat

---

## 历史记录批量删除 + 快捷模板

### 历史记录批量删除

- 三个页面（SQL/DEE/字段映射）历史记录支持多选
- 每条记录前添加 checkbox
- 卡片头部添加「全选」和「批量删除」按钮
- 选中记录高亮显示（浅蓝色背景）
- 后端新增 `DELETE /api/xxx/history/batch` 批量删除接口

### SQL Copilot 快捷模板从历史记录保存

- 历史记录条目添加「存为模板」按钮（文件夹图标）
- 模板存储在 localStorage，无需后端支持
- 快捷模板区域分为「默认模板」和「我的模板」两组
- 我的模板支持点击使用和删除

### 涉及文件

- backend: SqlHistoryMapper.java/xml, DeeHistoryMapper.java/xml, FieldMappingHistoryMapper.java/xml
- backend: SqlCopilotController.java, DeeController.java, FieldMappingController.java
- frontend: SqlCopilot.vue, DeeCopilot.vue, FieldMapper.vue

---

## 历史记录置顶功能

### 功能说明

- SQL Copilot、DEE Copilot、Field Mapper 三个页面的历史记录都支持置顶
- 置顶记录排在列表最前面，背景色为浅黄色
- 支持置顶/取消置顶、删除操作

### 后端修改

- 三个历史表（sql_history, dee_history, field_mapping_history）添加 is_pinned 字段
- DatabaseMigration 自动为现有表添加字段
- 三个 Mapper 添加 updatePinned 和 deleteById 方法
- 三个 Controller 添加 PUT /{id}/pin 和 DELETE /{id} 接口
- 查询排序：is_pinned DESC, create_time DESC

### 前端修改

- 三个页面历史记录列表添加置顶图标和删除按钮
- 置顶记录显示黄色星星图标
- 置顶记录背景色为浅黄色（#fffbe6）
- 删除操作需要确认

### 涉及文件

- backend: schema.sql, DatabaseMigration.java
- backend: SqlHistory.java, DeeHistory.java, FieldMappingHistory.java
- backend: SqlHistoryMapper.java/xml, DeeHistoryMapper.java/xml, FieldMappingHistoryMapper.java/xml
- backend: SqlCopilotController.java, DeeController.java, FieldMappingController.java
- frontend: SqlCopilot.vue, DeeCopilot.vue, FieldMapper.vue

---

## SQL 异步处理功能

### 异步任务机制

- 新增 `sql_task` 表（SQLite），存储异步任务状态
- 任务状态：PENDING → PROCESSING → COMPLETED / FAILED
- 提交 API：`POST /api/sql/async`，立即返回任务 ID
- 查询 API：`GET /api/sql/task/{taskId}`，轮询任务状态
- 清理 API：`DELETE /api/sql/task?days=7`，清除 7 天前任务

### 使用流程

1. 前端提交请求：`POST /api/sql/async`
2. 后端返回任务 ID（立即返回，不阻塞）
3. 前端轮询状态：`GET /api/sql/task/{taskId}`（每 2-5 秒）
4. 任务完成：返回 SQL 结果

### 与缓存的协同

- 提交异步任务时先检查缓存
- 缓存命中：直接返回结果（taskId: "cached"）
- 缓存未命中：创建异步任务，后台执行
- 任务完成后自动存入缓存

### 涉及文件

- backend: schema.sql, SqlTask.java, SqlTaskMapper.java, SqlTaskMapper.xml
- backend: Application.java（@EnableAsync）, SqlCopilotController.java

---

## SQL 缓存功能 + AI 超时优化 + Prompt 约束增强

### SQL 缓存功能

- 新增 `sql_cache` 表（SQLite），存储 SQL 生成结果
- 缓存 Key：MD5(prompt + formCode)，7 天自动过期
- 重复查询响应时间：101 秒 → 0.01 秒（提升 10000 倍）
- 新增 API：`DELETE /api/sql/cache?days=0` 清除所有缓存
- 新增 API：`DELETE /api/sql/cache?days=7` 清除 7 天前缓存

### AI API 超时优化

- 超时时间：120 秒 → 300 秒
- 解决 FORMMAIN_9750（107 字段）查询超时问题

### System Prompt 精简

- 系统表摘要：从完整表格改为单行速查格式
- 大表字段显示：>30 字段时只显示特殊字段表格 + 普通字段列表
- Prompt 长度：15,324 → 8,000 字符（减少约 50%）

### Oracle 11g 别名长度约束

- 新增约束：中文别名必须 ≤ 10 个汉字（Oracle 11g 标识符 30 字节限制）
- ≤ 10 字：保持原样
- \> 10 字：自动缩减（保留核心语义）
- 约束位置：移至 prompt 最前面（最高优先级）

### CLOB 字段 GROUP BY 限制

- 新增约束：CLOB 字段不能用于 GROUP BY、DISTINCT、ORDER BY
- 解决方案：使用 SUBSTR(field, 1, 2000) 转换后再聚合

### 涉及文件

- backend: schema.sql, SqlCache.java, SqlCacheMapper.java, SqlCacheMapper.xml, SqlCopilotController.java
- backend: application.yml（超时配置）, KnowledgeService.java（prompt 精简）
- knowledge: prompts/sql/form_query.md（别名约束 + CLOB 限制）

---

# 2026-05-18

## 数据字典前端录入 + SQL Copilot 增强

### 数据字典前端录入

- DataDictionary.vue：粘贴 OA 表单设计器原始文本，自动解析并保存
- 支持 `\t \t`（tab-space-tab）分隔符和空格分隔两种格式
- 解析预览：表单名称、主表名、字段列表、从表列表
- formCode 自动生成（FORMMAIN_XXXX），支持手动修改
- 数据字典列表：置顶/取消置顶、删除、清空全部

### 主从表自动拆分

- DataDictionaryParserService：解析"主表信息"+"从表信息"段落
- 保存主表时自动将从表存为独立记录（formCode=FORMSON_XXXX）
- 从表 JSON 包含 parentTable 字段，指向主表名
- KnowledgeService.findSonTables()：优先用 parentTable 精确匹配，fallback 数字匹配

### SQL Copilot 自动注入从表

- buildSqlSystemPrompt() 自动查找并注入从表字段和 JOIN 规则
- 用户只需选择主表 formCode，AI 自动生成含从表 JOIN 的 SQL
- 验证：FORMMAIN_7850 + FORMSON_7851（零件信息明细表）正确 JOIN

### 日期智能解析

- buildSqlSystemPrompt() 注入当前系统日期（年月日+星期）
- prompt 模板新增日期智能解析规则：X月→当月、X号→当日、无时间→今天
- 用户输入"查看运输通知单5月数据"→ AI 自动生成 `WHERE START_DATE >= TO_DATE('2026-05-01')`

### START_DATE 默认时间过滤

- 所有 FORMMAIN_XXXX 表都有 START_DATE 字段（系统自动记录的流程发起时间）
- 时间相关查询默认按 START_DATE 过滤，不依赖表单自定义日期字段
- 写入 CLAUDE.md OA 数据库查询规范 + prompt 模板

### 数据字典解析器修复（3 个 bug）

- CLOB/LONGTEXT 字段丢失：LONGTEXT 无长度时 cleaned 只有 5 列，新增 5 列兼容解析
- CLOB/LONGTEXT → dbFinalType 统一归一化为 VARCHAR2(2000 CHAR)
- 选多人字段识别：正则 + switch + isSpecial=true + refTable=ORG_MEMBER
- 未知输入类型（表单自定义控件等）：dbFinalType 默认 VARCHAR2(2000 CHAR)
- DECISIONS.md 新增 3 条决策：选多人/CLOB归一化/未知类型

### 单选框与下拉同等对待

- DataDictionaryParserService：单选框标记为 isSpecial=true，refTable=CTP_ENUM_ITEM
- 解析正则支持"单选"类型
- SQL 生成时单选框自动 JOIN CTP_ENUM_ITEM 取 SHOWVALUE
- 更新 prompt 模板 + CLAUDE.md + DECISIONS.md

### 涉及文件

- backend: DataDictionaryController.java, DataDictionaryParserService.java, KnowledgeService.java
- frontend: DataDictionary.vue, router/index.js, AdminLayout.vue
- knowledge: prompts/sql/form_query.md（2处同步）
- CLAUDE.md, ROADMAP.md, TASKS.md, CHANGELOG.md, DECISIONS.md

---

# 2026-05-14

## Phase 2：DEE 知识资产沉淀

### DEE Prompt 文件化（4 个）

- prompts/dee/workflow.md — 流程节点定义规范（step 结构、action 类型、role 分配）
- prompts/dee/token.md — token 认证接口配置规范（header 标准、responseMapping JSONPath 规则）
- prompts/dee/json.md — 字段映射规范（source→target 命名、type 映射规则）
- prompts/dee/java.md — DEE Handler 代码规范（DeeContext 上下文、ERP 调用规则）

### DEE 模板文件（4 个）

- dee_templates/workflow.json — 流程节点定义示例（含 step/action/role）
- dee_templates/token.json — token 调用配置示例（含 url/headers/body/responseMapping）
- dee_templates/json.json — 字段映射示例（含 string/number/date/boolean type）
- dee_templates/java.java — DEE Handler 代码示例（含 handle 方法、try-catch、HR 同步）

### KnowledgeService 扩展

- getDeePrompt(templateType)：从 knowledge/prompts/dee/ 加载 DEE Prompt
- getPrompt() 路径泛化：支持 "sql/form_query" 和 "dee/workflow" 子目录

### AiService 更新

- generateDeeTemplate() 优先从 KnowledgeService 加载 Prompt
- 找不到文件时回退硬编码（向后兼容）

### 闭环验证

- 4 种模板类型全部通过 AI 生成验证（workflow/token/json/java）
- 日志确认 Prompt 从文件加载，无 "未找到" 警告

---

## Phase 2：多选字段支持（isMultiSelect）

### 数据字典 JSON Schema 更新

- 所有 4 个 JSON 文件新增 `isMultiSelect` 字段（Boolean）
- 挂账申请表：formmain_10567.json + formson_10569.json
- 采购申请表：formmain_0433.json + formson_0434.json
- 当前所有字段均为 `isMultiSelect: false`（无多选场景）

### SQL 模板新增

- formmain_multi_select.sql：多选字段拆分聚合模板
  - REGEXP_SUBSTR + CONNECT BY 拆分逗号分隔 ID
  - JOIN ORG_MEMBER / ORG_UNIT 取显示值
  - LISTAGG 聚合多个人名（用"、"分隔）
  - 同时包含多选部门字段模板（注释形式）

### KnowledgeService 更新

- buildSqlSystemPrompt() 输出表格新增"多选"列
- AI 生成 SQL 时可识别字段是否多选，自动选择正确的 JOIN 策略

### Prompt 资产更新

- prompts/sql/form_query.md：已包含选多人模式、VARCHAR 兼容规则

---

## Phase 2：系统特殊表知识资产（ORG_MEMBER）

### 新增目录：knowledge/system_tables/

- 与 data_dictionary/ 平级，专门存放 OA 系统固定表的数据字典
- 表类型标记为 "system"，区别于业务表单的 "main"/"son"

### ORG_MEMBER 数据字典

- knowledge/system_tables/org_member.json
- 核心字段：ID / NAME / CODE / STATE / IS_ENABLE / IS_DELETED / ORG_DEPARTMENT_ID / ORG_POST_ID
- EXT_ATTR_* 预留字段不进入 Prompt（避免污染）
- 关联关系：ORG_DEPARTMENT_ID → ORG_UNIT.ID，ORG_POST_ID → ORG_POST.ID

### ORG_MEMBER JOIN 规范（项目级 SQL 规范）

- 选人字段存 ORG_MEMBER.ID（BIGINT），字段类型可能是 VARCHAR
- **必须使用 TO_CHAR 兼容**：`TO_CHAR(ORG_MEMBER.ID) = formmain_xxxx.fieldxxxx`
- 禁止直接数字比较：`ORG_MEMBER.ID = fieldxxxx`
- ID 可能为负数（如 -820787101123853929），字符串兼容可正确处理

### 默认人员过滤

- 查询 ORG_MEMBER 时建议加：`STATE = 1 AND IS_ENABLE = 1 AND IS_DELETED = 0`
- 排除离职人员、停用账号、已删除账号

### Prompt 更新

- prompts/sql/form_query.md 新增 ORG_MEMBER 表规则章节
- 明确 TO_CHAR JOIN 规则、默认过滤条件、关联部门/岗位规则

### DECISIONS.md 新增

- ORG_MEMBER 选人字段 JOIN 规范（TO_CHAR 兼容 + 默认过滤）

---

## Phase 2：系统特殊表知识资产（ORG_UNIT）

### ORG_UNIT 数据字典

- knowledge/system_tables/org_unit.json
- 核心字段：ID / NAME / CODE / SHORT_NAME / TYPE / PATH / IS_ENABLE / IS_DELETED / STATUS / ORG_ACCOUNT_ID
- EXT_ATTR_* 预留字段不进入 Prompt

### ORG_UNIT 企业使用规范

- 企业实际使用中仅作为**部门表**，不考虑 Account/Team/Group 等类型
- PATH 字段表示树形层级路径（一级 000000010007，二级 0000000100070001）
- 用户输入"数字化中心" → 直接匹配 ORG_UNIT.NAME

### ORG_UNIT JOIN 规范

- 选部门字段存 ORG_UNIT.ID（BIGINT），字段类型可能是 VARCHAR
- **必须使用 TO_CHAR 兼容**：`TO_CHAR(ORG_UNIT.ID) = formmain_xxxx.fieldxxxx`
- 默认过滤：`IS_ENABLE = 1 AND IS_DELETED = 0`

### Prompt 更新

- prompts/sql/form_query.md 新增 ORG_UNIT 表规则章节

### DECISIONS.md 新增

- ORG_UNIT 选部门字段 JOIN 规范（TO_CHAR 兼容 + 默认过滤 + 企业实际用法优先）

---

## Phase 2：系统特殊表知识资产（ORG_POST）

### ORG_POST 数据字典

- knowledge/system_tables/org_post.json
- 核心字段：ID / NAME / CODE / TYPE / IS_ENABLE / IS_DELETED / ORG_ACCOUNT_ID
- 岗位类型：1=管理类，2=技术类，3=营销类，4=职能类

### ORG_POST JOIN 规范

- 选岗位字段存 ORG_POST.ID（BIGINT），字段类型可能是 VARCHAR
- **必须使用 TO_CHAR 兼容**：`TO_CHAR(ORG_POST.ID) = formmain_xxxx.fieldxxxx`
- ID 可能为正数或负数，不允许假设永远为正数
- 默认过滤：`IS_ENABLE = 1 AND IS_DELETED = 0`

### Prompt 更新

- prompts/sql/form_query.md 新增 ORG_POST 表规则章节

### DECISIONS.md 新增

- ORG_POST 选岗位字段 JOIN 规范（TO_CHAR 兼容 + 默认过滤）

---

## Phase 2：系统特殊表知识资产（CTP_ENUM_ITEM）

### CTP_ENUM_ITEM 数据字典

- knowledge/system_tables/ctp_enum_item.json
- 核心字段：ID / REF_ENUMID / SHOWVALUE / ENUMVALUE / STATE / CODE
- OA 下拉字段存储的是 ID，不是 ENUMVALUE 或 CODE

### CTP_ENUM_ITEM JOIN 规范

- **必须使用 TO_CHAR 兼容**：`TO_CHAR(CTP_ENUM_ITEM.ID) = formmain_xxxx.fieldxxxx`
- **禁止错误 JOIN**：不允许 `fieldxxxx = ENUMVALUE` 或 `fieldxxxx = CODE`
- 默认显示 SHOWVALUE
- 默认过滤：`STATE = 1`（0=停用, 1=启用, 3=删除）
- 当前阶段不按 REF_ENUMID 过滤，直接按 ID JOIN

### Prompt 更新

- prompts/sql/form_query.md 新增 CTP_ENUM_ITEM 表规则章节

### DECISIONS.md 新增

- CTP_ENUM_ITEM 下拉字段 JOIN 规范（TO_CHAR 兼容 + 禁止按 ENUMVALUE/CODE JOIN）

---

## Phase 2：系统特殊表知识资产（CTP_AFFAIR）

### CTP_AFFAIR 数据字典

- knowledge/system_tables/ctp_affair.json
- 人员事项表，一个流程实例产生多条 affair（每个参与者一条）
- 核心字段：ID / OBJECT_ID / MEMBER_ID / SENDER_ID / SUBJECT / STATE / SUB_STATE / CREATE_DATE / RECEIVE_TIME / COMPLETE_TIME / IS_FINISH / NODE_NAME / FORM_RECORDID / CASE_ID / SUMMARY_STATE / ORG_DEPARTMENT_ID / PRE_APPROVER

### CTP_AFFAIR 状态修正（关键认知升级）

- **修正**：CTP_AFFAIR.STATE 是个人事项状态，不是流程整体状态
- 流程整体状态必须查 COL_SUMMARY.STATE
- 职责划分：
  - COL_SUMMARY：流程实例主表（流程状态核心）
  - CTP_AFFAIR：人员事项表（待办/已办核心）

### CTP_AFFAIR 事项状态码

- 1=待发，2=已发，3=待办，4=已办，5=撤销，6=回退，7=取回，8=竞争执行，15=终止，16=删除

### 标准流程链路修正

- 旧链路（错误）：FORMMAIN ↔ COL_SUMMARY ↔ CTP_COMMENT_ALL ↔ CTP_AFFAIR
- 新链路（正确）：FORMMAIN ↔ COL_SUMMARY(form_recordid) ↔ CTP_AFFAIR(OBJECT_ID)，COL_SUMMARY ↔ CTP_COMMENT_ALL(module_id)

### Prompt 更新

- prompts/sql/form_query.md：新增 CTP_AFFAIR 表规则 + 修正流程链路 + 职责划分

### DECISIONS.md 新增

- CTP_AFFAIR 与 COL_SUMMARY 职责划分

---

## Phase 2：系统特殊表知识资产（COL_SUMMARY）

### COL_SUMMARY 数据字典

- knowledge/system_tables/col_summary.json
- 流程实例主表，一个流程实例对应一条记录
- 核心字段：ID / STATE / SUB_STATE / SUBJECT / FORM_RECORDID / START_MEMBER_ID / CASE_ID / PROCESS_ID / CREATE_DATE / START_DATE / FINISH_DATE / ORG_DEPARTMENT_ID / IMPORTANT_LEVEL / IS_COVER_TIME / CURRENT_NODES_INFO

### COL_SUMMARY 核心规则

- **STATE 是流程整体状态的唯一权威字段**（0=未结束, 1=终止, 2=待发, 3=已结束）
- 标准 JOIN：`FORMMAIN.ID = COL_SUMMARY.FORM_RECORDID`
- 查发起人：`COL_SUMMARY.START_MEMBER_ID = TO_CHAR(ORG_MEMBER.ID)`

### Prompt 更新

- prompts/sql/form_query.md：新增 COL_SUMMARY 表规则章节

### DECISIONS.md 新增

- COL_SUMMARY 流程状态查询规范

---

## Phase 2：系统特殊表知识资产（CTP_COMMENT_ALL）

### CTP_COMMENT_ALL 数据字典

- knowledge/system_tables/ctp_comment_all.json
- 审批意见/处理时间表
- 核心字段：ID / AFFAIR_ID / MODULE_ID / CTYPE / CONTENT / CREATE_ID / CREATE_NAME / CREATE_DATE / DEPARTMENT_NAME / POST_NAME / ACCOUNT_NAME / EXT_ATT4 / HAS_WF_OPERATION

### CTP_COMMENT_ALL 核心规则

- **最常用 JOIN**：`CTP_COMMENT_ALL.AFFAIR_ID = CTP_AFFAIR.ID`
- **备选 JOIN**：`CTP_COMMENT_ALL.MODULE_ID = COL_SUMMARY.ID`
- **查审批记录标准方式**：通过流程标题搜索（CTP_AFFAIR.SUBJECT LIKE '%关键词%'）
- 冗余字段：CREATE_NAME / DEPARTMENT_NAME / POST_NAME / ACCOUNT_NAME 已存储，无需再 JOIN

### 审批记录查询标准模板

```sql
SELECT t.NODE_NAME, t.SUBJECT, t.CREATE_DATE, t.RECEIVE_TIME, t.COMPLETE_TIME,
       b.EXT_ATT4, b.CONTENT
FROM CTP_AFFAIR t
LEFT JOIN CTP_COMMENT_ALL b ON t.ID = b.AFFAIR_ID
WHERE t.SUBJECT LIKE '%标题关键词%'
ORDER BY t.SUBJECT, t.RECEIVE_TIME DESC
```

### Prompt 更新

- prompts/sql/form_query.md：新增 CTP_COMMENT_ALL 表规则 + 审批记录查询标准模板

### DECISIONS.md 新增

- CTP_COMMENT_ALL 审批记录查询规范

### Phase 2 系统特殊表完成

- 全部 7 张系统表数据字典完成：ORG_MEMBER / ORG_UNIT / ORG_POST / CTP_ENUM_ITEM / CTP_AFFAIR / COL_SUMMARY / CTP_COMMENT_ALL
- 标准流程链路修正完成
- SQL Prompt 已包含所有系统表规则

---

## Phase 2：业务场景 SQL 模板（3 个）

### 新增目录：knowledge/sql_templates/business_scenario/

- 与 form_query/ 平级，存放基于真实业务场景的 SQL 模板
- 直接喂给 AI，提升 SQL Copilot 生成质量

### 3 个业务场景模板

- count_by_person_and_month.sql — 按人员+月份统计表单数量（示例：查王得童 5 月份发起的采购申请有多少条）
- count_by_department_and_year.sql — 按部门+年份统计表单数量（示例：数字化中心去年一共提过多少个采购申请）
- approval_records_by_title.sql — 按流程标题查审批记录（示例：查零件采购价格审批表的审批记录）

### 模板特点

- 使用 TO_CHAR 兼容 JOIN（ORG_MEMBER / ORG_UNIT）
- 使用标准流程链路（FORMMAIN ↔ COL_SUMMARY）
- 使用 CTP_AFFAIR + CTP_COMMENT_ALL 查审批记录
- 默认过滤已发起流程（排除待发状态）

---

## Phase 2：采购申请表数据字典 + 前端集成

### 采购申请表数据字典

- formmain_0433.json（主表，60+ 个字段，10 个特殊字段）
- formson_0434.json（明细表，16 个字段，无特殊字段）
- README.md（主从关系、特殊字段清单、常用查询场景、标准流程链路）
- 特殊字段：选人×3（field0030/field0050/field0088）、选部门×1（field0004）、下拉×4（field0065/field0070/field0086/field0087）、附件×2（field0064/field0071）
- 流程处理意见字段×12：全部标记为 isSpecial=false（普通文本）

### 前端集成

- SqlCopilot 页面增加"数据字典"下拉选择框
- 从 /api/knowledge/forms 加载可用表单列表
- 选择表单后，AI 自动生成 SQL 时自动参考该表单数据字典
- 支持清空选择（不使用数据字典）

### KnowledgeService 优化

- getFormDictionary() 改为动态扫描目录找 formmain_*.json
- 支持任意表单代码，无需硬编码文件名

### 闭环验证

- 采购申请表（purchase_apply）数据字典 → SQL Copilot
- AI 正确生成：FORMMAIN_0433 + ORG_UNIT + ORG_MEMBER JOIN
- 自动识别选人/选部门字段并关联显示值

### DECISIONS.md 新增决策

- OA 表单字段全部通用（不需要按表单特殊处理）
- CTP_ENUM_ITEM.id 是唯一铁律（不需要按 enumId 区分）
- 附件字段只查 ID + filename（基础版）
- 选多人字段处理（多选 ID 逗号分隔，REGEXP_SUBSTR + CONNECT BY 拆分，LISTAGG 聚合）
- VARCHAR 字段存 ID 需要 TO_CHAR 兼容

### Prompt 资产更新

- prompts/sql/form_query.md：新增选多人模式、VARCHAR 兼容规则

---

## Phase 2：Knowledge + SQL Copilot 集成闭环完成

### KnowledgeService（知识资产加载器）

- KnowledgeService：从 classpath 加载数据字典 JSON + Prompt 模板
- getFormDictionary(formCode)：返回表单字段结构
- getPrompt(name)：返回 Prompt 文本
- buildSqlSystemPrompt(formCode)：自动拼接数据字典上下文到 system prompt
- 缓存机制：ConcurrentHashMap 缓存已加载的资产

### AiService 集成

- generateSql(prompt, formCode)：支持传入表单代码，自动注入数据字典上下文
- 向后兼容：generateSql(prompt) 仍可用（无字典上下文）

### KnowledgeController

- GET /api/knowledge/forms：列出所有可用表单数据字典
- GET /api/knowledge/forms/{formCode}：获取指定表单的字段结构

### SqlCopilotController 增强

- POST /api/sql/generate：新增 formCode 参数
- 传入 formCode 时，AI 生成 SQL 会自动参考数据字典

### 首个闭环验证

- 挂账申请表（guazhang_apply）数据字典 → SQL Copilot
- AI 正确生成：FORMMAIN_10567 + ORG_MEMBER + ORG_UNIT + CTP_ENUM_ITEM JOIN
- 自动识别选人/选部门/下拉字段并关联显示值

---

# 2026-05-13

## Phase 2：首个真实表单字典样例落地

### knowledge/data_dictionary（挂账申请表）

- 建立统一 JSON schema：formName / appCode / tableName / tableType / fields[]
- fields 结构：fieldName / displayName / dbFinalType / inputType / isSpecial / refTable / refKey / refDisplay
- 首个真实样例：挂账申请表
  - formmain_10567.json（主表，15 个字段，6 个特殊字段）
  - formson_10569.json（明细表，10 个字段，1 个特殊字段）
  - README.md（主从关系、特殊字段清单、常用查询场景、标准流程链路）
- inputType 直接使用 OA 类型：文本/日期/下拉/选人/选部门/上传附件/文本域/流程处理意见
- isSpecial=true 仅用于：选人/选部门/下拉/上传附件
- 文本域/流程处理意见 isSpecial=false（普通文本）

### knowledge/sql_templates（SQL 模板）

- formmain_with_display.sql：主表 + 选人/选部门/下拉显示值 JOIN 模板
- formmain_son_join.sql：主从 JOIN + 显示值模板
- 模板内含完整 ID 转名称规则（ORG_MEMBER / ORG_UNIT / CTP_ENUM_ITEM）
- 标准流程链路（FORMMAIN ↔ COL_SUMMARY ↔ CTP_COMMENT_ALL ↔ CTP_AFFAIR）

### knowledge/prompts（Prompt 资产）

- prompts/sql/form_query.md：表单查询 SQL 生成 Prompt
- 包含字段类型约束、标准流程链路、JOIN 规则、输出格式要求

---

## Phase 2 开始：企业知识资产沉淀

- CLAUDE.md 更新：新增 Phase 2 企业知识资产沉淀目标 + OA 数据库查询规范
- 确定 OA 数据库查询规范与字段类型约束为项目硬规则
- 完成文档对齐：ROADMAP.md / TASKS.md / DECISIONS.md / CHANGELOG.md 同步更新

### OA 数据库查询规范（写入 CLAUDE.md）

- 字段类型约束：流程处理意见/文本域 = 普通文本；特殊字段仅 4 类（选人/选部门/下拉/附件）
- 核心流程表：CTP_AFFAIR / CTP_COMMENT_ALL / COL_SUMMARY
- 枚举/组织表：CTP_ENUM_ITEM / ORG_MEMBER / ORG_UNIT / ORG_POST
- 标准关联链路：FORMMAIN ↔ COL_SUMMARY ↔ CTP_COMMENT_ALL ↔ CTP_AFFAIR

### DECISIONS.md 新增决策

- OA 字段类型约束（为什么按普通文本处理）
- 标准关联链路（为什么必须优先采用）
- 选人/选部门/下拉必须关联表转显示值（为什么不能直接展示 ID）

---

## Phase 1 MVP 全部完成

### 前端

- Vue3 + Vite + Element Plus 项目初始化
- 后台 Layout（深色侧边栏 + 顶栏 + 路由）
- Dashboard 页面（统计卡片 + 趋势图 + 使用记录 + 系统状态）
- SQL Copilot（Prompt 输入 + 快捷模板 + SQL 输出 + 历史记录）
- DEE 模板生成（4 种模板类型 + 历史记录）
- 字段映射助手（表单 ID + 字段列表 + 映射表格）

### 后端

- SpringBoot 3.2.5 + Java 21 + SQLite + MyBatis
- REST API：SQL/DEE/字段映射 三个模块完整 CRUD
- AiService 封装：兼容 OpenAI 格式，可配置 endpoint/key/model
- AI 优先 + mock fallback 策略
- UTF-8 编码全栈修复（Utf8Config + Jackson + SQL init）
- Markdown 代码块自动剥离（stripMarkdown）
- 请求日志 + AI 调用日志

### AI 集成

- mimo-v2.5-pro API 接入验证通过
- SQL 生成、DEE 模板生成、字段映射推荐均正常
- 超时时间 120s，支持复杂查询生成

---

## 已知待优化

- 终端日志中文需 `chcp 65001`（Phase 3 解决）
- 日志仅终端可见，无前端查看入口（Phase 3 解决）
- SQLite 限制（Phase 3 迁移 MySQL）

---

## 当前状态

- Phase 1 MVP 100% 完成
- Phase 2 企业知识资产沉淀 已启动
- 4 个 md 文档已与 CLAUDE.md 对齐
