# DECISIONS

# 2026-05-13

## 为什么不做微服务

原因：

- MVP 阶段复杂度过高
- 单人开发维护成本大
- 当前业务量不需要

决定：

采用单体 SpringBoot。

---

## AI 接入方式

原因：

- 不绑定具体模型，用户可能随时切换
- 需要一个统一的配置入口

决定：

- AiService 封装统一调用层，兼容 OpenAI 格式 API
- 通过 application.yml 配置 endpoint / api-key / model
- 用户改配置即可切换模型，无需改代码
- AI 未配置时自动 fallback 到 mock

---

## 为什么不做 Agent

原因：

- 当前核心问题是模板化开发
- Agent 容易过度复杂
- 企业环境稳定性优先

决定：

优先 Prompt + 模板方案。

---

## 为什么使用 SQLite

原因：

- 零运维
- 快速开发
- 本地方便

后期：

迁移 MySQL。

---

## 为什么 Oracle 只读

原因：

- 避免误操作
- 企业安全要求
- AI 不允许直接改数据

---

## DEE 模板为什么分 4 种类型

原因：

- DEE 开发中最高频的 4 种场景
- workflow：流程节点定义（占 40%）
- token：接口认证配置（占 20%）
- JSON：字段映射模板（占 30%）
- Java：Handler 脚本（占 10%）

决定：

- 前端按类型选择，后端按类型返回对应 mock
- 第二阶段按类型构建不同 Prompt

---

## 为什么进入企业知识资产化

原因：

- 通用 AI 无法理解企业业务语义
- 企业开发核心是上下文知识
- SQL/DEE/workflow 都高度依赖企业规范

决定：

Phase 2 开始：
建立 knowledge/ 企业知识资产体系。

AI 后续生成：
逐步基于企业知识资产生成，
而不是纯 Prompt 泛化生成。

---

## OA 字段类型约束

原因：

- 企业开发中容易误认为"流程处理意见""文本域"有特殊语义
- 实际在数据库中均为普通文本字段（VARCHAR2/CLOB）
- 混淆会导致 AI 生成错误的查询逻辑

决定：

- "流程处理意见""文本域"按普通文本字段处理，无特殊语义
- 特殊字段仅 4 类：
  - 选人：存 ORG_MEMBER.id
  - 选部门：存 ORG_UNIT.id
  - 下拉/单选：存 CTP_ENUM_ITEM.id
  - 上传附件：极少用，走 CTP_ATTACHMENT（sub_reference=field）

---

## 查询必须优先采用 OA 标准关联链路

原因：

- 致远 OA 流程数据分散在多张表中
- 非标准关联会导致查询结果不完整或性能差
- 标准链路是 OA 平台设计规范，必须遵守

决定：

- 查询流程相关数据时，必须优先使用标准关联链路：
  FORMMAIN_XXXX.id ↔ COL_SUMMARY.form_recordid
  COL_SUMMARY.id ↔ CTP_AFFAIR.OBJECT_ID
  COL_SUMMARY.id ↔ CTP_COMMENT_ALL.module_id
- AI 生成 SQL 时必须基于此链路

---

## 选人/选部门/下拉必须关联表转显示值

原因：

- OA 表单中选人/选部门/下拉框字段存的是 ID（数字）
- 直接展示 ID 无业务含义，必须关联组织表/枚举表转为可读名称
- 这是 OA 查询最常见的开发需求

决定：

- 选人 → JOIN ORG_MEMBER 取 name
- 选部门 → JOIN ORG_UNIT 取 name
- 选岗位 → JOIN ORG_POST 取 name
- 下拉框 → JOIN CTP_ENUM_ITEM 取 showvalue
- AI 生成涉及这些字段的 SQL 时，必须自动关联

---

## Knowledge 资产加载策略

原因：

- 知识资产需要被 AI 生成时引用
- 初期需要简单可靠，不引入外部依赖
- 后续可能需要热更新（不重启即可更新资产）

决定：

- 初期：打包到 classpath（resources/knowledge），随应用启动加载
- KnowledgeService 提供缓存机制（ConcurrentHashMap）
- AiService.generateSql(formCode) 自动注入数据字典上下文
- 后续可演进到外部目录热更新（B 策略），但当前不做

---

## OA 表单字段全部通用

原因：

- 所有表单字段都是通用类型，没有特殊业务含义
- 文本/日期/选人/选部门/下拉/上传附件/文本域/流程处理意见 — 这些类型在所有表单中语义一致
- 不需要针对特定表单做字段特殊处理

决定：

- 数据字典 JSON schema 统一，不区分表单
- AI 生成 SQL 时，按字段类型（inputType）通用处理，不按表单特殊处理
- 字段数量多少不影响处理逻辑（60+ 字段与 15 字段同样处理）

---

## CTP_ENUM_ITEM.id 是唯一铁律

原因：

- 下拉框字段存的是 CTP_ENUM_ITEM.id
- CTP_ENUM_ITEM.id 在系统中是全局唯一的
- FORMMAIN_XXXX.fieldXXXX = CTP_ENUM_ITEM.id 是唯一关联规则
- 不需要按 enumId 区分不同枚举类型

决定：

- 下拉字段 JOIN 规则：FORMMAIN_XXXX.fieldXXXX = CTP_ENUM_ITEM.id
- 不需要额外的 enumId 过滤条件
- AI 生成 SQL 时直接使用此规则

---

## 附件字段 JOIN 规范

原因：

- 附件表 CTP_ATTACHMENT 存储附件元数据
- 表单附件字段（如 FIELD0068）存储的是附件 ID（CTP_ATTACHMENT.ID）
- CTP_ATTACHMENT.SUB_REFERENCE 存储的也是附件 ID（与表单字段值相同）
- 之前误认为 SUB_REFERENCE 存字段名，MODULE_ID 存表单记录 ID（错误）

决定：

- **正确 JOIN**：`CTP_ATTACHMENT.SUB_REFERENCE = formmain_xxxx.fieldxxxx`
- **禁止错误 JOIN**：不允许 `MODULE_ID = m.ID AND SUB_REFERENCE = 'FIELDXXXX'`
- 表单字段本身存附件 ID，SELECT 时直接取字段值作为附件 ID
- 额外 JOIN CTP_ATTACHMENT 取 FILENAME（文件名）
- 不查询附件内容（二进制）

---

## 选多人字段处理（多选 ID 逗号分隔）

原因：

- OA 表单中"选人"字段支持多选，字段存多个 ID 逗号分隔（如 "123,456,789"）
- 行级 LISTAGG 子查询直接扫描 ORG_MEMBER 会导致 SQL 卡死
- 每字段独立 CTE + LIKE 仍有 N×M 扫描问题

决定：

- **三层结构**：REGEXP_SUBSTR 拆分 → 统一 JOIN ORG_MEMBER → LISTAGG 聚合 → CASE WHEN 回填
- Step 1：所有多人字段用 REGEXP_SUBSTR + CONNECT BY 拆为 (form_id, field_code, member_id)
- Step 2：统一 JOIN ORG_MEMBER（只扫描一次）
- Step 3：`LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME) GROUP BY form_id, field_code`
- Step 4：主查询 `MAX(CASE WHEN field_code='xxx' THEN names END)` 转列回填
- **禁止**：每字段独立 CTE + LIKE、SELECT 中行级子查询扫描 ORG_MEMBER
- **选多人与选人同等对待**：isSpecial=true，refTable=ORG_MEMBER

---

## CLOB/LONGTEXT 字段类型归一化

原因：

- OA 表单中"文本域"字段的数据库类型可能是 CLOB 或 LONGTEXT
- CLOB 在 SQL 中不便于处理，实际场景用 VARCHAR2 即可满足
- 解析器遇到 CLOB/LONGTEXT 应统一归一化

决定：

- CLOB/LONGTEXT → dbFinalType 统一为 `VARCHAR2(2000 CHAR)`
- 不影响输入类型（inputType 仍为"文本域"）

---

## 未知输入类型的处理

原因：

- OA 表单设计器可能有非标准输入类型（如"表单自定义控件"）
- 解析器不应因未知类型而报错或丢弃字段
- 未知类型的数据库类型无法确定，需要安全默认值

决定：

- 未知输入类型 → dbFinalType 默认为 `VARCHAR2(2000 CHAR)`
- inputType 保留原始值（如"表单自定义控件"），不做映射
- 不标记为特殊字段（isSpecial=false）

SQL 模板（CTE 预关联 + LISTAGG 回填）：

```sql
-- 每个多人字段一个 CTE
WITH member_map_0082 AS (
    SELECT m.id AS form_id, om.NAME
    FROM FORMMAIN_XXXX m
    JOIN ORG_MEMBER om ON ',' || m.FIELD0082 || ',' LIKE '%,' || om.ID || ',%'
    WHERE om.STATE = 1 AND om.IS_ENABLE = 1 AND om.IS_DELETED = 0
),
member_map_0083 AS (
    SELECT m.id AS form_id, om.NAME
    FROM FORMMAIN_XXXX m
    JOIN ORG_MEMBER om ON ',' || m.FIELD0083 || ',' LIKE '%,' || om.ID || ',%'
    WHERE om.STATE = 1 AND om.IS_ENABLE = 1 AND om.IS_DELETED = 0
)
-- 主查询只做聚合回填
SELECT
    m.ID,
    (SELECT LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME)
     FROM member_map_0082 mm WHERE mm.form_id = m.ID) AS 多选人1,
    (SELECT LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME)
     FROM member_map_0083 mm WHERE mm.form_id = m.ID) AS 多选人2
FROM FORMMAIN_XXXX m
```

---

## 选人/选部门/下拉字段直接 JOIN（不加 TO_CHAR）

原因：

- OA 表单的选人/选部门/下拉字段存储的是雪花 ID（带负号的字符串，如 -820787101123853929）
- ID 本身就是字符串类型，不是纯数字
- TO_CHAR 会导致索引失效，查询变慢

决定：

- **默认直接 JOIN**：`ORG_MEMBER.ID = fieldxxxx`（不加 TO_CHAR）
- 仅当报类型不匹配错误时才加 TO_CHAR
- AI 生成 SQL 时默认不使用 TO_CHAR

---

## ORG_MEMBER 选人字段 JOIN 规范

原因：

- OA 表单选人字段存储 ORG_MEMBER.ID
- TO_CHAR 会导致索引失效，查询变慢
- 实际场景中字段类型兼容，直接比较即可

决定：

- **默认直接 JOIN**：`ORG_MEMBER.ID = formmain_xxxx.fieldxxxx`（不加 TO_CHAR）
- **仅当报类型不匹配错误时**才加 TO_CHAR
- 查询 ORG_MEMBER 时默认加：`STATE = 1 AND IS_ENABLE = 1 AND IS_DELETED = 0`
- ORG_MEMBER 关联部门：`ORG_DEPARTMENT_ID = ORG_UNIT.ID`
- ORG_MEMBER 关联岗位：`ORG_POST_ID = ORG_POST.ID`

---

## ORG_UNIT 选部门字段 JOIN 规范

原因：

- OA 表单选部门字段存储 ORG_UNIT.ID，字段类型可能是 VARCHAR
- ID 为超长数字且可能为负数，直接数字比较会导致类型不匹配
- 企业实际使用中 ORG_UNIT 仅作为部门表，不考虑 Account/Team/Group 等类型
- AI 应直接用 NAME 匹配，无需理解复杂组织模型

决定：

- **默认直接 JOIN**：`ORG_UNIT.ID = formmain_xxxx.fieldxxxx`（不加 TO_CHAR）
- 禁止直接 `ORG_UNIT.ID = fieldxxxx`
- 查询 ORG_UNIT 时默认加：`IS_ENABLE = 1 AND IS_DELETED = 0`
- ORG_UNIT 树形层级通过 PATH 字段实现（如一级 000000010007，二级 0000000100070001）
- 企业真实使用方式优先级高于 OA 理论全模型

---

## ORG_POST 选岗位字段 JOIN 规范

原因：

- OA 表单选岗位字段存储 ORG_POST.ID，字段类型可能是 VARCHAR
- ID 为超长数字且可能为正数或负数，不允许假设 ID 永远为正数
- 不允许使用 UNSIGNED 类型思维

决定：

- **默认直接 JOIN**：`ORG_POST.ID = formmain_xxxx.fieldxxxx`（不加 TO_CHAR）
- 禁止直接 `ORG_POST.ID = fieldxxxx`
- 查询 ORG_POST 时默认加：`IS_ENABLE = 1 AND IS_DELETED = 0`
- 遇到"选岗位/岗位/职位/主岗"关键词时，优先联想到 ORG_POST

---

## CTP_ENUM_ITEM 下拉字段 JOIN 规范

原因：

- OA 表单下拉字段存储 CTP_ENUM_ITEM.ID，不是 ENUMVALUE 或 CODE
- 混淆会导致 AI 生成错误的 JOIN 条件
- ENUMVALUE 是枚举内部值，不等于表单存储值
- 不同枚举分类下 ENUMVALUE 可能重复，但 ID 全局唯一

决定：

- **默认直接 JOIN**：`CTP_ENUM_ITEM.ID = formmain_xxxx.fieldxxxx`（不加 TO_CHAR）
- 禁止 `fieldxxxx = ENUMVALUE` 或 `fieldxxxx = CODE`
- 默认显示 SHOWVALUE
- 查询 CTP_ENUM_ITEM 时默认加：`STATE = 1`
- 当前阶段不按 REF_ENUMID 过滤，直接按 ID JOIN
- 多个下拉字段独立 JOIN，使用别名区分（如 cei_currency、cei_status）

---

## CTP_AFFAIR 与 COL_SUMMARY 职责划分

原因：

- 之前将 CTP_AFFAIR.STATE 理解为"流程整体状态"，这是不准确的
- CTP_AFFAIR 是人员事项表，一个流程实例产生多条 affair（每个参与者一条）
- CTP_AFFAIR.STATE 表示个人事项状态（待办/已办等），不是流程整体状态
- 流程整体状态必须查 COL_SUMMARY.STATE

决定：

- **COL_SUMMARY**：流程实例主表，负责流程整体状态（STATE: 0=未结束, 1=终止, 2=待发, 3=已结束）
- **CTP_AFFAIR**：人员事项表，负责待办/已办/节点状态（STATE: 1=待发, 2=已发, 3=待办, 4=已办, 5=撤销, 6=回退, 7=取回）
- 查询"流程状态" → COL_SUMMARY.STATE
- 查询"我的待办" → CTP_AFFAIR.STATE = 3
- 查询"我的已办" → CTP_AFFAIR.STATE = 4
- 标准链路：FORMMAIN.id = COL_SUMMARY.form_recordid，COL_SUMMARY.id = CTP_AFFAIR.OBJECT_ID

---

## COL_SUMMARY 流程状态查询规范

原因：

- COL_SUMMARY 是流程实例主表，一个流程实例对应一条记录
- COL_SUMMARY.STATE 是流程整体状态的唯一权威字段
- 之前将 CTP_AFFAIR.STATE 误解为流程状态，已修正

决定：

- 查询"流程状态""流程是否结束" → 必须用 COL_SUMMARY.STATE
- 标准 JOIN：`FORMMAIN.ID = COL_SUMMARY.FORM_RECORDID`
- 流程状态码：0=未结束, 1=终止, 2=待发, 3=已结束
- 查发起人：`COL_SUMMARY.START_MEMBER_ID = ORG_MEMBER.ID`

---

## CTP_COMMENT_ALL 审批记录查询规范

原因：

- CTP_COMMENT_ALL 存储审批意见和处理时间
- 最常用场景：通过流程标题搜索审批记录
- 表中已冗余存储审批人姓名、部门、岗位、单位，无需再 JOIN 组织表

决定：

- **最常用 JOIN**：`CTP_COMMENT_ALL.AFFAIR_ID = CTP_AFFAIR.ID`
- **备选 JOIN**：`CTP_COMMENT_ALL.MODULE_ID = COL_SUMMARY.ID`
- **查审批记录标准方式**：CTP_AFFAIR.SUBJECT LIKE '%关键词%' + LEFT JOIN CTP_COMMENT_ALL
- CONTENT 存储处理意见，CREATE_NAME 存储审批人姓名
- DEPARTMENT_NAME / POST_NAME / ACCOUNT_NAME 已冗余存储，无需再 JOIN

---

## START_DATE 作为默认时间过滤字段

原因：

- 所有 FORMMAIN_XXXX 表都有 START_DATE 字段，是系统自动记录的流程发起时间
- 表单自定义的日期字段（如"申请日期"FIELD0003）不一定存在，不同表单字段不同
- 用户说"5月数据"时，需要一个通用可靠的时间过滤字段
- START_DATE 格式为 TIMESTAMP（如 2026-05-18 11:39:45.507000），可精确过滤

决定：

- **时间相关查询默认按 START_DATE 过滤**
- 不要假设表单有"申请日期""创建日期"等自定义字段
- 除非用户明确指定某个字段（如"按审批时间过滤"），否则一律用 START_DATE
- 范围查询：`START_DATE >= TO_DATE('2026-05-01') AND START_DATE < TO_DATE('2026-06-01')`

---

## 数据字典前端录入 + 从表自动拆分

原因：

- 手工编写 JSON 数据字典效率低，容易出错
- OA 表单设计器可复制原始文本，格式固定
- 主从表需要分别存储，SQL Copilot 才能正确生成 JOIN

决定：

- 前端提供文本粘贴入口，后端自动解析 OA 原始文本
- 主表存为 FORMMAIN_XXXX，从表存为 FORMSON_XXXX（独立记录）
- 从表 JSON 包含 parentTable 字段，用于关联主表
- SQL Copilot 选择主表时自动包含从表字段和 JOIN 规则

---

## SQL 缓存策略（SQLite vs Redis）

原因：

- AI 生成 SQL 响应时间长（100+ 秒），重复查询体验差
- 需要缓存机制避免重复调用 AI
- 选择缓存方案：SQLite 还是 Redis？

决定：

- **使用 SQLite 缓存表**，不引入 Redis
- 理由：
  - 当前已是 SQLite，零额外成本
  - 企业内部工具，访问量不大
  - 单机部署，不需要分布式缓存
  - 符合"不过度架构"原则
- 缓存 Key：MD5(prompt + formCode)
- 过期时间：7 天自动过期
- 后期如遇性能瓶颈再考虑 Redis

---

## Oracle 11g 别名长度约束

原因：

- Oracle 11g 标识符最长 30 字节
- 中文字符在 UTF-8 下占 3 字节，因此中文别名最多 10 个汉字
- OA 表单字段名普遍较长，容易超过限制
- 违反会导致 ORA-00932 错误

决定：

- **≤ 10 个汉字的别名：保持原样，不做任何改动**
- **> 10 个汉字的别名：必须缩减到 10 个汉字以内**
- 缩减原则：保留核心语义，去掉冗余修饰词
- 约束位置：放在 prompt 最前面（最高优先级）

---

## CLOB 字段 GROUP BY 限制

原因：

- Oracle 中 CLOB 字段不能用于 GROUP BY、DISTINCT、ORDER BY
- OA 表单的"文本域""流程处理意见"等字段可能是 CLOB 类型
- 违反会导致 ORA-00932 错误

决定：

- **CLOB 字段不能直接用于 GROUP BY、DISTINCT、ORDER BY**
- 解决方案：
  - 如果不需要聚合：去掉 GROUP BY，直接 SELECT
  - 如果需要聚合：用 `SUBSTR(field, 1, 2000)` 转换后再 GROUP BY
  - 如果需要去重：用 `SUBSTR(field, 1, 2000)` 转换后再 DISTINCT
- 判断方法：数据字典中标记为"文本域"或"CLOB"的字段，大概率是 CLOB 类型

---

## SQL 异步处理机制

原因：

- AI 生成 SQL 响应时间长（100+ 秒），同步请求阻塞用户体验
- 需要异步机制让用户可以继续操作，后台处理完成后通知
- 选择实现方案：提交任务 + 轮询状态

决定：

- **采用异步任务机制**，不使用 WebSocket（过度复杂）
- 流程：
  1. 前端提交请求，后端立即返回任务 ID
  2. 前端轮询任务状态（每 2-5 秒）
  3. 任务完成后返回 SQL 结果
- 与缓存协同：
  - 提交时先检查缓存，命中直接返回
  - 未命中创建异步任务，完成后自动存缓存
- 任务状态：PENDING → PROCESSING → COMPLETED / FAILED
- 任务过期：7 天自动清理

---

## 历史记录统一置顶功能

原因：

- SQL Copilot、DEE Copilot、Field Mapper 三个页面都有历史记录列表
- 数据字典页面已有置顶功能，其他页面需要保持一致
- 用户需要标记重要记录，方便快速访问

决定：

- **三个页面统一添加置顶/取消置顶功能**
- 置顶记录排在列表最前面（ORDER BY is_pinned DESC, create_time DESC）
- 置顶记录视觉区分：浅黄色背景 + 黄色星星图标
- 删除操作需要确认（ElMessageBox.confirm）
- 数据库：三个历史表统一添加 is_pinned 字段（INTEGER NOT NULL DEFAULT 0）
- DatabaseMigration 自动为现有表添加字段（ALTER TABLE ADD COLUMN）

---

## 历史记录批量删除

原因：

- 逐条删除效率低，用户需要批量清理历史记录
- 三个页面（SQL/DEE/字段映射）需要保持一致

决定：

- **三个页面统一添加多选 + 批量删除功能**
- 每条记录前添加 checkbox，支持全选/取消全选
- 选中后点击批量删除，弹出确认框
- 后端新增 batch delete 接口，接收 ids 数组
- 选中记录高亮显示（浅蓝色背景）

---

## SQL Copilot 快捷模板改为可编辑

原因：

- 从历史记录存为模板的方案导致 UI 拥挤
- 用户需要更灵活的模板管理方式
- 需要支持新增、编辑、删除、设为默认、恢复默认

决定：

- **快捷模板区域可编辑**，移除「从历史记录存为模板」功能
- 支持：新增（弹窗输入）、编辑（弹窗修改）、删除、设为默认、恢复默认
- 模板存储在 localStorage（key: sql_copilot_quick_templates）
- 仅在 SQL Copilot 页面实现，DEE 和字段映射暂不添加

---

## Oracle 11g 别名直接截取

原因：

- Oracle 11g 标识符最长 30 字节，中文别名最多 10 个汉字
- 智能前缀去除逻辑可能误截业务关键词（如"控制计划轴力标准"）
- 用户表示会尽量避免特别长的别名

决定：

- **直接截取前 10 个汉字**，不做智能缩减
- KnowledgeService.shortenAlias() 改为 displayName.substring(0, 10)
- prompt 中的缩减规则仍保留（指导 AI 生成时主动控制长度）

---

## 日期过滤条件不自动添加

原因：

- 原规则：用户未提及时间时默认使用系统当天日期过滤
- 实际场景：用户查"所有请假单"时不需要时间限制
- 自动添加时间过滤会遗漏数据，与用户预期不符

决定：

- **用户未提及时间 → 不加时间过滤条件**
- 用户提到时间相关词（如"5月""上周""本月"）时才按 START_DATE 过滤
- 更新位置：prompts/sql/form_query.md 日期智能解析规则
