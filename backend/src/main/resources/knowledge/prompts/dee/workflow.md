# DEE Workflow 流程节点定义

## 系统约束

### 流程节点结构

- workflowId：流程唯一标识（如 wf_001）
- workflowName：流程名称（中文）
- steps：节点数组，按执行顺序排列
- formId：关联表单 ID（如 formmain_XXXX）

### Step 节点字段

| 字段 | 说明 | 示例 |
|------|------|------|
| stepId | 节点序号 | 1, 2, 3 |
| name | 节点名称（中文） | 发起申请, 部门审批, 人事备案 |
| action | 操作类型 | submit, approve, reject, archive |
| role | 审批角色（可选） | dept_manager, hr, finance |
| nextStep | 下个节点 ID（null 表示结束） | 2 或 null |

### Action 类型

- submit：提交申请
- approve：审批通过
- reject：驳回
- archive：归档/备案
- notify：通知（不阻塞流程）

## 生成规则

1. 根据业务描述设计流程节点
2. 每个节点包含 stepId、name、action
3. 审批节点需要指定 role
4. nextStep 串联节点顺序
5. 必须关联一个 formId
6. 只返回 JSON，不要解释

## 输出格式

- 只返回 JSON
- 不返回解释
- 外层为大括号 {}
- step 为数组
