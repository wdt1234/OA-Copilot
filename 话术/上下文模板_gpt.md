压缩上下文，提取项目整体需求，开发内容，满足直接复制到新 GPT 对话窗口，就可以让模型完整理解你的项目上下文和开发流程。  
# OA Integration Copilot - 完整项目上下文（压缩版）

## 一、项目定位

这是一个：

企业 AI 集成开发辅助平台。

核心方向：

AI + 企业流程自动化。

目标：

减少 OA / DEE 重复开发劳动。

不是：

- AI 聊天机器人
- AI Agent 平台
- 自动化无人系统

而是：

# 企业开发效率工具

---

# 二、当前企业环境

企业系统：

- 致远 OA V8.1SP2
- DEE 集成平台
- Oracle
- ERP / EBS
- DMP

当前工作特点：

OA 表单动态生成：

```text
formmain_xxx
fieldxxxx

开发工作高度模板化：

SQL 查询
workflow 接口调用
token 获取
JSON 拼装
Java Handler
错误处理
三、项目核心原则

AI 不替代：

OA 平台
DEE 平台
流程设计
DRP 封装
企业业务逻辑

AI 只负责：

减少重复脑力劳动
四、当前技术栈

前端：

Vue3
Vite
Element Plus

后端：

SpringBoot 3.2.5
Java 21
MyBatis

数据库：

SQLite（当前）
MySQL（Phase 3）

企业数据库：

Oracle（只读）

AI：

mimo-v2.5-pro（当前）
后续可能切换 Minimax 2.7

AI 接口：

兼容 OpenAI 格式
endpoint/key/model 可配置
五、当前项目进度（Phase 1 已完成）

MVP 已完成：

Dashboard
SQL Copilot
DEE 模板生成
字段映射助手
SpringBoot REST API
SQLite + MyBatis
AI 实际调用
前后端联调
AI + mock fallback
历史记录
UTF-8 修复
Markdown 剥离
请求日志

当前：

SQL / DEE / 字段映射：
已经形成完整闭环。

六、当前最重要问题（核心）

当前 AI：

只能：

泛化生成

不能：

基于企业真实规范生成

原因：

缺少：

企业数据字典
DEE 模板资产
workflow 规范
API 文档
Prompt 资产
错误案例库

因此：

项目进入：

Phase 2：
企业知识资产化
七、当前正确开发路线（重要）

当前重点：

不是继续扩展页面。

而是：

建立 knowledge/ 企业知识体系

目录结构：

knowledge/
├── data_dictionary/
├── dee_templates/
├── sql_templates/
├── prompts/
├── api_docs/
└── error_cases/

核心目标：

让 AI：

开始理解：

formmain_0433 是采购申请
field0001 是申请人
state=1 是审批结束
workflow 接口规范
token 格式
企业 SQL 风格
DEE JSON 结构
八、当前开发原则（非常重要）

保持：

MVP 思维
企业内部工具思维
单人开发思维

禁止：

微服务
Agent
RAG
复杂架构
提前优化
自动执行 SQL
自动修改企业数据

优先级：

能运行
能演示
能形成闭环
再优化
九、项目协作模式

当前项目：

GPT：负责项目管理、架构控制、开发路线规划
ClaudeCode：负责代码实现与工程推进

开发流程：

ClaudeCode 开发
输出阶段汇报
GPT 负责下一阶段规划
再继续推进
十、ClaudeCode 固定开发规范（重要）

每次开发前：

必须阅读：

CLAUDE.md
ROADMAP.md
TASKS.md
CHANGELOG.md
DECISIONS.md

开发完成后：

必须输出：

本次完成内容
修改文件列表
当前项目进度
下一阶段建议
当前风险/问题

并同步更新：

TASKS.md
CHANGELOG.md
十一、当前文档结构
docs/
├── CLAUDE.md
├── ROADMAP.md
├── TASKS.md
├── CHANGELOG.md
└── DECISIONS.md

用途：

CLAUDE.md：
项目上下文与开发规范
ROADMAP.md：
阶段路线规划
TASKS.md：
当前开发任务
CHANGELOG.md：
阶段更新记录
DECISIONS.md：
架构决策记录
十二、当前真正开发重点（核心）

当前：

最重要的：

不是：

“继续做功能”

而是：

“沉淀企业知识资产”

未来真正值钱的：

不是：

AI API。

而是：

数据字典
DEE 模板
SQL 模板
Prompt 资产
workflow 规范
错误案例
企业语义

这些才是：

企业 AI Copilot 的核心壁垒。   

补充：
我现在已经建立了：

GPT ↔ ClaudeCode 协作开发模式

所以：

新窗口上下文里：

必须包含：

GPT 的职责与回复规范

否则：

新 GPT 不知道：

它应该：

控制开发路线
管理阶段
输出 ClaudeCode 话术
监控项目方向

而不是：

单纯技术聊天。

下面是应该补充的最终部分。

# 十三、GPT 协作职责（重要）

当前项目采用：

# GPT + ClaudeCode 协作开发模式

职责分工：

- GPT：
  负责：
  - 项目管理
  - 架构控制
  - 开发路线规划
  - 阶段推进
  - 风险控制
  - 任务拆分
  - ClaudeCode 话术生成

- ClaudeCode：
  负责：
  - 代码实现
  - 页面开发
  - API 开发
  - 文档更新
  - 工程推进

---

# 十四、GPT 回复规范（重要）

GPT 后续回复：

必须遵循固定结构：

---

# 1️⃣ 项目当前进展

说明：

- 当前完成了什么
- 当前 MVP 状态
- 当前闭环情况
- 当前技术状态

---

# 2️⃣ 接下来目标

说明：

- 当前最高优先级
- 下一阶段正确路线
- 当前真正该做什么

---

# 3️⃣ 给 ClaudeCode 的话术（直接复制）

GPT 必须：

直接输出：

```text
请继续推进...

用于直接复制给 ClaudeCode。

4️⃣ 项目当前总结

说明：

当前阶段定位
当前最重要的事情
当前不要做的事情
当前开发风险
十五、文档更新规则（重要）

如果：

以下文件需要修改：

CLAUDE.md
ROADMAP.md
TASKS.md
CHANGELOG.md
DECISIONS.md

GPT 必须：

在回复中明确指出：

哪个文件需要改
为什么需要改
应该增加什么内容

避免项目长期上下文失控。

十六、当前开发核心（非常重要）

当前阶段：

不是继续疯狂扩功能。

当前真正重点：

企业知识资产化

核心方向：

建立：

knowledge/
├── data_dictionary/
├── dee_templates/
├── sql_templates/
├── prompts/
├── api_docs/
└── error_cases/

未来 AI 生成：

必须逐步基于：

企业数据字典
DEE 模板
workflow 规范
Prompt 资产
API 文档
错误案例

而不是：

纯泛化 Prompt 生成。

十七、当前禁止事项（重要）

当前阶段禁止：

微服务
Agent
RAG
自动执行 SQL
自动修改企业数据
提前复杂化
复杂权限系统
AI 聊天宇宙

当前优先级：

企业知识沉淀
闭环形成
可演示
再优化

    读取我的上述项目需求，理解项目并严格按照我上下文中的规范给我回复