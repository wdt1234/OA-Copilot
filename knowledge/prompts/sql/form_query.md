# SQL 生成 Prompt：表单查询

## 系统约束（必须遵守）

### 字段类型约束

- "流程处理意见""文本域"在数据库中均为**普通文本字段**（VARCHAR2/CLOB），无特殊语义处理
- 特殊字段**仅 4 类**：
  - 选人：存 ORG_MEMBER.id → 需 JOIN ORG_MEMBER 取 NAME
  - 选部门：存 ORG_UNIT.id → 需 JOIN ORG_UNIT 取 NAME
  - 下拉：存 CTP_ENUM_ITEM.id → 需 JOIN CTP_ENUM_ITEM 取 SHOWVALUE
  - 上传附件：极少用，走 CTP_ATTACHMENT（sub_reference=field）
- **选多人**：字段存多个 ID 逗号分隔（如 "123,456,789"），需拆分后 JOIN

### ORG_MEMBER 表规则（选人字段必须遵守）

- OA 表单选人字段存储的是 ORG_MEMBER.ID（BIGINT），但字段类型可能是 VARCHAR
- **JOIN 必须使用 TO_CHAR**：`TO_CHAR(ORG_MEMBER.ID) = formmain_xxxx.fieldxxxx`
- **禁止直接数字比较**：不允许 `ORG_MEMBER.ID = fieldxxxx`
- ID 可能为负数（如 `-820787101123853929`），字符串兼容可正确处理
- **默认人员过滤**：查询 ORG_MEMBER 时建议加 `STATE = 1 AND IS_ENABLE = 1 AND IS_DELETED = 0`
  - 排除离职人员、停用账号、已删除账号
- **关联部门**：`ORG_MEMBER.ORG_DEPARTMENT_ID = ORG_UNIT.ID`
- **关联岗位**：`ORG_MEMBER.ORG_POST_ID = ORG_POST.ID`

### ORG_UNIT 表规则（选部门字段必须遵守）

- OA 表单选部门字段存储的是 ORG_UNIT.ID（BIGINT），字段类型可能是 VARCHAR
- **JOIN 必须使用 TO_CHAR**：`TO_CHAR(ORG_UNIT.ID) = formmain_xxxx.fieldxxxx`
- **禁止直接数字比较**：不允许 `ORG_UNIT.ID = fieldxxxx`
- 企业实际使用中 ORG_UNIT 仅作为**部门表**，不考虑 Account/Team/Group 等类型
- 用户输入"数字化中心"→ 直接匹配 `ORG_UNIT.NAME = '数字化中心'`
- **默认过滤**：查询 ORG_UNIT 时建议加 `IS_ENABLE = 1 AND IS_DELETED = 0`
- **树形层级**：PATH 字段表示部门层级路径（如一级 000000010007，二级 0000000100070001），可用于查询下级部门

### ORG_POST 表规则（选岗位字段必须遵守）

- OA 表单选岗位字段存储的是 ORG_POST.ID（BIGINT），字段类型可能是 VARCHAR
- **JOIN 必须使用 TO_CHAR**：`TO_CHAR(ORG_POST.ID) = formmain_xxxx.fieldxxxx`
- **禁止直接数字比较**：不允许 `ORG_POST.ID = fieldxxxx`
- ID 可能为正数或负数 BIGINT，不允许假设 ID 永远为正数
- **默认过滤**：查询 ORG_POST 时建议加 `IS_ENABLE = 1 AND IS_DELETED = 0`
- **岗位类型**：1=管理类，2=技术类，3=营销类，4=职能类
- **关联人员**：`ORG_MEMBER.ORG_POST_ID = ORG_POST.ID`

### CTP_ENUM_ITEM 表规则（下拉字段必须遵守）

- OA 表单下拉字段存储的是 **CTP_ENUM_ITEM.ID**，不是 ENUMVALUE 或 CODE
- **JOIN 必须使用 TO_CHAR**：`TO_CHAR(CTP_ENUM_ITEM.ID) = formmain_xxxx.fieldxxxx`
- **禁止错误 JOIN**：不允许 `fieldxxxx = ENUMVALUE` 或 `fieldxxxx = CODE`
- **默认显示 SHOWVALUE**：`SELECT cei.SHOWVALUE AS 中文名`
- **默认过滤**：查询 CTP_ENUM_ITEM 时建议加 `STATE = 1`（0=停用, 1=启用, 3=删除）
- **当前阶段不按 REF_ENUMID 过滤**：直接按 ID JOIN 即可
- **多个下拉字段**：每个下拉字段独立 JOIN，使用别名区分（如 `cei_currency`、`cei_status`）

### VARCHAR 字段存 ID 兼容

- 部分表单选人/选部门/下拉字段类型是 VARCHAR，需使用 TO_CHAR(id) = field 兼容
- NUMBER 类型字段直接比较：id = field

### 标准流程链路（查询流程状态/审批记录时必须使用）

```
FORMMAIN_XXXX.id        = COL_SUMMARY.form_recordid
COL_SUMMARY.id          = CTP_AFFAIR.OBJECT_ID
COL_SUMMARY.id          = CTP_COMMENT_ALL.module_id
```

**职责划分：**
- **COL_SUMMARY**：流程实例主表，负责流程整体状态（是否结束/终止/待发）
- **CTP_AFFAIR**：人员事项表，负责待办/已办/节点状态（一个流程产生多条，每个参与者一条）
- **CTP_COMMENT_ALL**：流程审批意见/处理时间

### COL_SUMMARY 流程状态码（流程整体状态）

- 0 = 未结束
- 1 = 终止
- 2 = 待发
- 3 = 已结束

**查询"流程状态""流程是否结束" → 必须用 COL_SUMMARY.STATE**

### CTP_AFFAIR 事项状态码（个人事项状态）

- 1 = 待发
- 2 = 已发
- 3 = 待办
- 4 = 已办
- 5 = 撤销
- 6 = 回退
- 7 = 取回
- 8 = 竞争执行
- 15 = 终止
- 16 = 流程事项被删除

**查询"我的待办" → CTP_AFFAIR.STATE = 3**
**查询"我的已办" → CTP_AFFAIR.STATE = 4**
**查询"谁审批了" → CTP_AFFAIR.MEMBER_ID**

### CTP_AFFAIR 表规则（人员事项必须遵守）

- CTP_AFFAIR 是人员事项表，一个流程实例产生多条 affair（每个参与者一条）
- **CTP_AFFAIR.STATE 是个人事项状态，不是流程整体状态**
- 流程整体状态必须查 COL_SUMMARY.STATE
- **标准 JOIN**：`COL_SUMMARY.id = CTP_AFFAIR.OBJECT_ID`
- **查某人事项**：`CTP_AFFAIR.MEMBER_ID = TO_CHAR(ORG_MEMBER.ID)`
- **查发起人**：通过 CTP_AFFAIR.SENDER_ID 或直接查 COL_SUMMARY
- **查审批耗时**：CTP_AFFAIR.RECEIVE_TIME → CTP_AFFAIR.COMPLETE_TIME
- **查节点名称**：CTP_AFFAIR.NODE_NAME

### COL_SUMMARY 表规则（流程状态必须遵守）

- COL_SUMMARY 是流程实例主表，一个流程实例对应一条记录
- **COL_SUMMARY.STATE 是流程整体状态的唯一权威字段**
- **标准 JOIN**：`FORMMAIN_XXXX.id = COL_SUMMARY.FORM_RECORDID`
- **查发起人**：`COL_SUMMARY.START_MEMBER_ID = TO_CHAR(ORG_MEMBER.ID)`
- **查流程标题**：`COL_SUMMARY.SUBJECT`
- **查流程时间**：`COL_SUMMARY.CREATE_DATE` / `START_DATE` / `FINISH_DATE`
- **查当前处理人**：`COL_SUMMARY.CURRENT_NODES_INFO`
- **查超期**：`COL_SUMMARY.IS_COVER_TIME = 1`

### CTP_COMMENT_ALL 表规则（审批意见/处理时间必须遵守）

- CTP_COMMENT_ALL 存储审批意见和处理时间
- **最常用 JOIN**：`CTP_COMMENT_ALL.AFFAIR_ID = CTP_AFFAIR.ID`
- **也可用**：`CTP_COMMENT_ALL.MODULE_ID = COL_SUMMARY.ID`
- **查审批记录的标准方式**：通过流程标题搜索（见下方标准模板）
- **CONTENT**：处理意见（如：同意、驳回、请补充材料）
- **CREATE_NAME**：审批人姓名（已冗余存储，无需再 JOIN ORG_MEMBER）
- **DEPARTMENT_NAME / POST_NAME / ACCOUNT_NAME**：已冗余存储，无需再 JOIN

### 审批记录查询标准模板

```sql
SELECT
    t.NODE_NAME     AS 节点名,
    t.SUBJECT       AS 流程名称,
    t.CREATE_DATE   AS 流程开始时间,
    t.RECEIVE_TIME  AS 接收时间,
    t.COMPLETE_TIME AS 处理时间,
    b.EXT_ATT4      AS 是否同意,
    b.CONTENT       AS 处理意见
FROM CTP_AFFAIR t
LEFT JOIN CTP_COMMENT_ALL b ON t.ID = b.AFFAIR_ID
WHERE t.SUBJECT LIKE '%标题关键词%'
ORDER BY t.SUBJECT, t.RECEIVE_TIME DESC
```

## SQL 生成规则

1. 主表：FORMMAIN_XXXX
2. 明细表：FORMSON_XXXX（通过 formmain_id = FORMMAIN.ID 关联）
3. 涉及选人/选部门字段 → 自动 JOIN 对应组织表
4. 涉及下拉字段 → 自动 JOIN CTP_ENUM_ITEM
5. 涉及流程状态 → 自动 JOIN COL_SUMMARY
6. 涉及审批记录 → 自动 JOIN COL_SUMMARY + CTP_COMMENT_ALL + CTP_AFFAIR
7. 文本域/流程处理意见 → 直接取值，不做任何特殊处理
8. **选多人字段**（多选 ID 逗号分隔）→ 使用 REGEXP_SUBSTR + CONNECT BY 拆分，JOIN ORG_MEMBER，LISTAGG 聚合
9. VARCHAR 字段存 ID → 使用 TO_CHAR(id) = field 兼容

### 日期智能解析规则

- 用户输入"X月"→ 当前年份的 X 月（如"5月"→ 今年5月）
- 用户输入"X号"→ 当前月份的 X 号（如"5号"→ 当月5号）
- 用户输入"去年""今年""前年"→ 基于系统日期推算
- 用户输入"上个月""本月""下个月"→ 基于系统日期推算
- 无时间指定 → 默认使用系统当天日期
- 日期过滤应使用范围查询，避免遗漏
- Oracle 日期比较使用 TO_DATE 函数

### START_DATE 默认时间过滤规则（必须遵守）

- **所有 FORMMAIN_XXXX 表都有 START_DATE 字段**，格式如 `2026-05-18 11:39:45.507000`（TIMESTAMP）
- START_DATE 是系统自动记录的流程发起时间，是最可靠的时间过滤字段
- **用户提到时间相关查询（如"5月数据""上周""本月"）时，默认按 START_DATE 过滤**
- 不要假设表单有"申请日期""创建日期"等自定义字段，这些字段不一定存在
- 除非用户明确指定某个字段（如"按审批时间过滤"），否则一律用 START_DATE
- 范围查询示例：`START_DATE >= TO_DATE('2026-05-01', 'YYYY-MM-DD') AND START_DATE < TO_DATE('2026-06-01', 'YYYY-MM-DD')`

## 输出格式

- 只返回 SQL 语句
- 不返回解释
- 表名和字段名大写
- 中文别名
- JOIN 条件完整
