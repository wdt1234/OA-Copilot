# TASKS

# Phase 1 - MVP ✅ 全部完成

- [x] 初始化 Vue3 项目
- [x] 安装 Element Plus
- [x] 完成后台 Layout + 侧边菜单
- [x] 完成 Dashboard 页面
- [x] 完成 SQL Copilot（前端 + 后端 + 联调）
- [x] 完成 DEE 模板生成（前端 + 后端 + 联调）
- [x] 完成字段映射助手（前端 + 后端 + 联调）
- [x] SpringBoot 后端（SQLite + MyBatis + REST API）
- [x] AiService 封装（可配置 API endpoint/key/model）
- [x] 三个模块接入 AiService（AI 优先 + mock fallback）
- [x] AI API 真实调用验证通过
- [x] UTF-8 中文编码全栈修复
- [x] Markdown 代码块自动剥离
- [x] OA 数据库查询规范写入 CLAUDE.md

---

# Phase 2 - 企业知识资产沉淀

## Phase 2 四大模块总览（全部完成后才进 Phase 3）

| 模块 | 知识资产 | 业务验证 | 状态 |
|------|---------|---------|------|
| SQL Copilot | 数据字典 + 系统表 + 业务场景模板 | 已验证 | ✅ 完成 |
| DEE 任务 | 4 模板 + 4 Prompt | 仅通用验证 | 需要真实业务验证 |
| 接口文档 | 6-Sheet Excel 模板 + 历史记录 | 已验证 | ✅ 完成 |
| 错题库 | — | 未开始 | 待开发 |

## knowledge/data_dictionary（数据字典）

- [x] 建立统一 JSON 结构：{ formName, appCode, tableName, tableType, fields[] }
- [x] fields 结构：{ fieldName, displayName, dbFinalType, inputType, isSpecial, isMultiSelect, refTable, refKey, refDisplay }
- [x] 首个真实样例：挂账申请表（formmain_10567 + formson_10569）
- [x] 第二个真实样例：采购申请表（formmain_0433 + formson_0434）
- [x] 特殊字段标注：选人(ORG_MEMBER.id) / 选部门(ORG_UNIT.id) / 下拉(CTP_ENUM_ITEM.id) / 附件(CTP_ATTACHMENT)
- [x] 字段类型约束文档化：流程处理意见/文本域 = 普通文本，无特殊语义
- [x] 审批状态语义：0=未结束 1=终止 2=待发 3=已结束
- [ ] 更多表单样例（随使用场景补充，不提前编造）
- [ ] metadata 模块结构（未来 Oracle 元数据同步用，当前只建结构不连库）

## knowledge/system_tables（系统特殊表）

- [x] ORG_MEMBER 数据字典（选人表：ID/NAME/CODE/STATE/IS_ENABLE/IS_DELETED/ORG_DEPARTMENT_ID/ORG_POST_ID）
- [x] ORG_MEMBER JOIN 规范：必须 TO_CHAR 兼容，禁止直接数字比较
- [x] ORG_MEMBER 默认过滤：STATE=1, IS_ENABLE=1, IS_DELETED=0
- [x] ORG_MEMBER 关联关系：ORG_DEPARTMENT_ID→ORG_UNIT, ORG_POST_ID→ORG_POST
- [x] ORG_UNIT 数据字典（选部门表：ID/NAME/CODE/SHORT_NAME/TYPE/PATH/IS_ENABLE/IS_DELETED/STATUS/ORG_ACCOUNT_ID）
- [x] ORG_UNIT JOIN 规范：必须 TO_CHAR 兼容
- [x] ORG_UNIT 默认过滤：IS_ENABLE=1, IS_DELETED=0
- [x] ORG_UNIT 树形层级：PATH 字段支持下级部门查询（Phase 2 仅沉淀知识）
- [x] ORG_POST 数据字典（选岗位表：ID/NAME/CODE/TYPE/IS_ENABLE/IS_DELETED/ORG_ACCOUNT_ID）
- [x] ORG_POST JOIN 规范：必须 TO_CHAR 兼容
- [x] ORG_POST 默认过滤：IS_ENABLE=1, IS_DELETED=0
- [x] ORG_POST 岗位类型：1=管理类，2=技术类，3=营销类，4=职能类
- [x] CTP_ENUM_ITEM 数据字典（下拉枚举表：ID/REF_ENUMID/SHOWVALUE/ENUMVALUE/STATE/CODE）
- [x] CTP_ENUM_ITEM JOIN 规范：必须 TO_CHAR 兼容，禁止按 ENUMVALUE/CODE JOIN
- [x] CTP_ENUM_ITEM 默认过滤：STATE=1
- [x] CTP_ENUM_ITEM 核心规则：下拉字段存的是 ID，不是 ENUMVALUE
- [x] CTP_AFFAIR 数据字典（人员事项表：ID/OBJECT_ID/MEMBER_ID/SENDER_ID/SUBJECT/STATE/SUB_STATE/CREATE_DATE/RECEIVE_TIME/COMPLETE_TIME/IS_FINISH/NODE_NAME/FORM_RECORDID/CASE_ID/SUMMARY_STATE/ORG_DEPARTMENT_ID/PRE_APPROVER）
- [x] CTP_AFFAIR 核心认知修正：STATE 是个人事项状态，不是流程整体状态
- [x] CTP_AFFAIR 事项状态：1=待发，2=已发，3=待办，4=已办，5=撤销，6=回退，7=取回，8=竞争执行，15=终止，16=删除
- [x] 标准流程链路修正：FORMMAIN↔COL_SUMMARY(form_recordid)，COL_SUMMARY↔CTP_AFFAIR(OBJECT_ID)
- [x] COL_SUMMARY 数据字典（流程状态表：ID/STATE/SUB_STATE/SUBJECT/FORM_RECORDID/START_MEMBER_ID/CASE_ID/PROCESS_ID/CREATE_DATE/START_DATE/FINISH_DATE/ORG_DEPARTMENT_ID/IMPORTANT_LEVEL/IS_COVER_TIME/CURRENT_NODES_INFO）
- [x] COL_SUMMARY 核心规则：STATE 是流程整体状态唯一权威字段
- [x] CTP_COMMENT_ALL 数据字典（审批意见表：ID/AFFAIR_ID/MODULE_ID/CTYPE/CONTENT/CREATE_ID/CREATE_NAME/CREATE_DATE/DEPARTMENT_ID/DEPARTMENT_NAME/POST_ID/POST_NAME/ACCOUNT_NAME/EXT_ATT4/HAS_WF_OPERATION/SUB_TYPE）
- [x] CTP_COMMENT_ALL JOIN 规范：AFFAIR_ID = CTP_AFFAIR.ID（最常用），MODULE_ID = COL_SUMMARY.ID（备选）
- [x] CTP_COMMENT_ALL 冗余字段：CREATE_NAME/DEPARTMENT_NAME/POST_NAME/ACCOUNT_NAME 已存储，无需再 JOIN

## knowledge/sql_templates（SQL 模板库）

- [x] 标准关联链路模板：FORMMAIN ↔ COL_SUMMARY ↔ CTP_COMMENT_ALL ↔ CTP_AFFAIR
- [x] ID 转名称 join 模板：formmain_with_display.sql（选人/选部门/下拉框关联查询）
- [x] 主从表关联模板：formmain_son_join.sql（FORMMAIN ↔ FORMSON）
- [x] 多选字段拆分聚合模板：formmain_multi_select.sql（REGEXP_SUBSTR + CONNECT BY + LISTAGG）
- [x] 业务场景模板（3 个）：
  - count_by_person_and_month.sql — 按人员+月份统计表单数量
  - count_by_department_and_year.sql — 按部门+年份统计表单数量
  - approval_records_by_title.sql — 按流程标题查审批记录
- [ ] 更多业务场景模板（随使用补充）

## 数据字典前端录入

- [x] DataDictionary.vue：文本粘贴 + 解析预览 + 保存
- [x] DataDictionaryParserService：OA 原始文本自动解析（支持 tab-space-tab 分隔）
- [x] DataDictionaryController：CRUD API + 置顶/清空
- [x] 主从表自动拆分：保存时从表存为独立记录（FORMSON_XXXX）
- [x] KnowledgeService.findSonTables()：parentTable 精确匹配 + fallback 数字匹配
- [x] SQL Copilot 自动注入从表字段和 JOIN 规则
- [x] 日期智能解析：注入当前系统日期 + 相对日期规则
- [x] START_DATE 默认时间过滤：所有主表通用

## knowledge/dee_templates（DEE 模板库）

- [x] workflow 模板结构（workflow.json + workflow.md prompt）
- [x] token 认证模板结构（token.json + token.md prompt）
- [x] JSON 字段映射模板结构（json.json + json.md prompt）
- [x] Java Handler 模板结构（java.java + java.md prompt）

## knowledge/prompts（Prompt 资产库）

- [x] prompts/sql/form_query.md — SQL 生成 Prompt（含字段类型约束、标准链路、JOIN 规则、选多人模式、VARCHAR 兼容）
- [x] prompts/dee/workflow.md — DEE workflow 生成 Prompt
- [x] prompts/dee/token.md — DEE token 生成 Prompt
- [x] prompts/dee/json.md — DEE 字段映射生成 Prompt
- [x] prompts/dee/java.md — DEE Java Handler 生成 Prompt
- [x] KnowledgeService 加载器（从 classpath 读取 Prompt + 数据字典）
- [x] KnowledgeService.getDeePrompt() 扩展（DEE Prompt 文件化加载）
- [x] AiService.generateSql() 集成知识资产（自动注入数据字典上下文）
- [x] AiService.generateDeeTemplate() 改为从 KnowledgeService 加载（先文件后硬编码回退）
- [ ] prompts/sql/ — 更多 SQL Prompt（统计/流程/审批）
- [ ] prompts/workflow/ — Workflow 相关 Prompt

## knowledge 集成闭环

- [x] KnowledgeService：加载数据字典 + Prompt 模板
- [x] AiService.generateSql(formCode)：自动注入数据字典上下文
- [x] KnowledgeController：/api/knowledge/forms 列出可用表单
- [x] SqlCopilotController：支持 formCode 参数
- [x] 首个闭环验证：挂账申请表 + AI 生成 SQL（含 JOIN 显示值）
- [x] 第二个闭环验证：采购申请表 + AI 生成 SQL（含 JOIN 显示值）
- [x] DEE 闭环验证：4 种模板类型全部通过（workflow/token/json/java）
- [x] KnowledgeService.getDeePrompt() 扩展验证通过
- [x] AiService.generateDeeTemplate() 文件→回退策略验证通过
- [x] 数据字典前端录入：粘贴 OA 原始文本自动解析 + 保存到数据库
- [x] 主从表自动拆分：保存主表时自动将从表存为独立记录
- [x] SQL Copilot 自动注入从表：选择主表后自动包含从表字段和 JOIN
- [x] 日期智能解析：用户输入"5月"→自动理解为今年5月，注入当前系统日期
- [x] START_DATE 默认时间过滤：所有主表通用，不依赖表单自定义日期字段
- [x] 单选框与下拉同等对待：isSpecial=true，JOIN CTP_ENUM_ITEM
- [x] 选多人字段识别：isSpecial=true，refTable=ORG_MEMBER
- [x] CLOB/LONGTEXT 归一化：dbFinalType → VARCHAR2(2000 CHAR)
- [x] 未知输入类型兼容：dbFinalType 默认 VARCHAR2(2000 CHAR)

## knowledge/api_docs（API 文档库）

- [x] OA 数据库查询规范（已写入 CLAUDE.md）
- [x] 接口文档自动生成器（6-Sheet Excel 模板）
- [x] 前端配置表单（数据字典选择 + 接口信息 + 连接方式）
- [x] 历史记录功能（CRUD + 置顶 + 批量删除）
- [x] Mule 模式模板（固定 Token/Workflow URL）
- [x] 直连模式模板（OA workflow 标准调用）
- [x] Data Mapping 自动字段映射（从数据字典生成）
- [x] OA常见报错案例 Sheet（8 种错误场景）
- [ ] workflow 接口文档结构
- [ ] token 获取接口文档结构
- [ ] 常用 header 规范

## knowledge/error_cases（错误案例库）

- [ ] SQL 常见错误案例
- [ ] DEE 接口常见错误案例
- [ ] token 获取失败案例

---

## Phase 2 禁止事项

- ❌ RAG
- ❌ Agent
- ❌ 微服务
- ❌ 自动执行 SQL
- ❌ 自动修改企业数据
- ❌ 重构现有结构
- ❌ 扩展新页面功能

---

# Phase 3 - 系统优化

- [ ] SQLite → MySQL 迁移
- [ ] 日志系统（前端可查、可筛选、可导出）
- [ ] 终端编码问题根治
- [ ] Prompt 调优（基于实际使用反馈）
- [x] 历史记录管理增强（置顶+删除）
- [x] SQL 缓存功能（7天过期 + 手动清理）
- [x] SQL 异步处理功能（提交任务 + 轮询状态）
- [x] AI API 超时优化（120→300秒）
- [x] System prompt 精简（减少约 50%）
- [x] Oracle 11g 别名长度约束（≤10汉字，直接截取）
- [x] CLOB 字段 GROUP BY 限制约束
- [x] 历史记录批量删除（多选 + 批量删除按钮）
- [x] SQL Copilot 快捷模板可编辑（新增/编辑/删除/设为默认/恢复默认）
- [x] 日期过滤逻辑修复（未提及时间时不自动添加过滤）
- [x] 历史记录 UI 优化（悬停显示按钮 + tooltip 查看完整描述）
- [x] 启动脚本修复（start.bat 最小化 + stop.bat 自动关闭）
- [x] 启动/停止脚本窗口管理优化（PowerShell 精准关闭 + 自动最小化）
- [x] 前端 UI/UX 重构（现代简洁风格）

---

# Phase 4 - AI 增强

- [ ] Swagger 解析
- [ ] 错误智能分析
- [ ] AI 对话辅助
