# CHANGELOG

# 2026-05-20

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
