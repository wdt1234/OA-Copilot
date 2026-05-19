# OA Integration Copilot - Project Context

## 项目定位

这是一个：

企业 AI 集成开发辅助平台。

核心方向：

AI + 企业流程自动化。

目标：

减少 OA / DEE 重复开发劳动。

---

# 当前系统环境

企业环境：

- 致远 OA V8.1SP2
- DEE 集成平台
- Oracle
- ERP / EBS
- DMP

---

# 当前开发特点

OA 表单：

动态生成：

```text
formmain_xxx
fieldxxxx
```

开发工作：

- SQL 查询
- DEE 开发
- workflow 接口调用
- token 获取
- JSON 拼装
- 错误处理

高度模板化。

---

# 项目核心原则

AI 不替代：

- OA 平台
- DEE 平台
- 流程设计
- DRP 封装

AI 只负责：

# 减少重复脑力劳动

---

# 当前 MVP 功能

- SQL Copilot
- DEE Code Copilot
- 字段映射助手

---

# 技术栈

前端：

- Vue3
- Element Plus

后端：

- SpringBoot

数据库：

- SQLite（前期）
- MySQL（后期）

企业数据库：

- Oracle（只读）

AI：

- Minimax API

---

# 开发原则

- 企业稳定优先
- 模板化优先
- 不过度架构
- 不做 Agent 系统
- 不做微服务
- 优先实现 MVP

---

# 当前目标

先完成：

- Dashboard
- SQL Copilot
- DEE 模板生成

不要：

- 权限系统
- 自动部署
- 自动执行 SQL
- AI 聊天宇宙


---

# AI 协作开发规范

## 项目协作模式

当前项目采用：

- GPT：负责项目管理、架构控制、阶段规划
- ClaudeCode：负责代码实现与工程推进

开发过程中：

必须保持：

- MVP 思维
- 企业内部工具思维
- 单人开发思维

禁止：

- 过度工程化
- 微服务化
- Agent 化
- 复杂抽象设计

---

## 开发任务执行规范

每次开始开发前：

必须：

1. 阅读：
   - ROADMAP.md
   - TASKS.md
   - CHANGELOG.md
   - DECISIONS.md

2. 理解当前项目状态

3. 确认当前最高优先级任务

4. 再开始开发

---

## 阶段开发结束后（必须）

每完成一个阶段：

必须输出：

### 1. 本次完成内容

例如：

- 完成 Dashboard 页面
- 完成 SQL Copilot 页面
- 完成 SpringBoot 初始化

---

### 2. 修改文件列表

例如：

- src/views/Dashboard.vue
- src/router/index.js

---

### 3. 当前项目进度

例如：

- MVP 完成度：30%

---

### 4. 下一阶段建议

例如：

- 下一步建议：
  SQL Copilot 页面开发

---

### 5. 当前风险/问题

例如：

- 当前尚未接入 AI API
- 当前仍为 mock 数据

---

## 文档更新规范

每次开发结束后：

必须同步更新：

- TASKS.md
- CHANGELOG.md

保持项目长期上下文完整。

---

## 开发原则（重要）

优先级：

1. 能运行
2. 能演示
3. 能形成 MVP
4. 再考虑优化

不要：

- 提前优化
- 提前抽象
- 提前复杂化

当前阶段：

重点是：

# 快速形成可运行 MVP

---

# 当前阶段（Phase 2）

重点：

不是继续扩展功能。

而是：

# 企业知识资产沉淀

AI 后续生成：
必须逐步基于：

- 数据字典
- DEE 模板
- Prompt 模板
- API 文档
- 错误案例

进行生成。

当前阶段：

禁止：

- 复杂 Agent
- RAG
- 微服务
- 自动执行 SQL
- 自动修改企业数据

---

# OA 数据库查询规范（精简版・开发专用）

## 0. 字段类型约束（非常重要）
- “流程处理意见””文本域”在数据库中均为普通文本字段（VARCHAR2/CLOB 等），无特殊语义处理。
- 特殊字段仅有：
  - 选人：存 ORG_MEMBER.id
  - 选部门：存 ORG_UNIT.id
  - 下拉/单选：存 CTP_ENUM_ITEM.id
  - 上传附件：极少用，走 CTP_ATTACHMENT（sub_reference=field）

## 1. 表单表规则
- 主表：FORMMAIN_XXXX
- 明细表：FORMSON_XXXX
- 选人/选部门/下拉框：存 ID，需关联表转显示值
- **START_DATE**：所有主表都有，系统自动记录的流程发起时间（TIMESTAMP），是时间过滤的默认字段
  - 格式：`2026-05-18 11:39:45.507000`
  - 用户提到时间查询时，优先用 START_DATE，不要假设表单有"申请日期"等自定义字段
- **主从表关联**：`FORMMAIN_XXXX.ID = FORMSON_XXXX.FORMMAIN_ID`（不是 MAINID）

## 2. 核心流程表（高频）
1) COL_SUMMARY：流程实例主表（流程状态核心）
   - state：0=未结束 1=终止 2=待发 3=已结束
   - form_recordid = FORMMAIN_XXXX.id
2) CTP_AFFAIR：人员事项表（待办/已办核心，一个流程产生多条，每个参与者一条）
   - state：1=待发 2=已发 3=待办 4=已办 5=撤销 6=回退 7=取回 8=竞争执行 15=终止 16=删除
   - object_id = COL_SUMMARY.id
   - member_id = ORG_MEMBER.id（事项归属人）
3) CTP_COMMENT_ALL：流程审批意见/处理时间
   - module_id = COL_SUMMARY.id
   - affair_id = CTP_AFFAIR.id（最常用 JOIN）
   - CREATE_NAME 冗余存储审批人姓名
   - DEPARTMENT_NAME / POST_NAME / ACCOUNT_NAME 冗余存储

## 3. 枚举 / 组织表（ID 转名称）
- CTP_ENUM_ITEM：下拉框 -> showvalue，id = FORMMAIN_XXXX.fieldXXXX
- ORG_MEMBER：选人 -> name，id = FORMMAIN_XXXX.fieldXXXX
- ORG_UNIT：选部门 -> name，id = FORMMAIN_XXXX.fieldXXXX
- ORG_POST：选岗位 -> name，id = FORMMAIN_XXXX.fieldXXXX

## 4. 附件表
- CTP_ATTACHMENT：附件信息
  - sub_reference = field

## 5. 标准关联链路
- FORMMAIN_XXXX.id ↔ COL_SUMMARY.form_recordid
- COL_SUMMARY.id ↔ CTP_AFFAIR.OBJECT_ID
- COL_SUMMARY.id ↔ CTP_COMMENT_ALL.module_id