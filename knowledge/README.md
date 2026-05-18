# Knowledge — 企业知识资产

本目录存放企业上下文知识资产，供 AI 生成时引用。

## 目录结构

```
knowledge/
├── data_dictionary/       # 数据字典（OA 表单字段、状态、业务语义）
│   └── forms/
│       └── guazhang_apply/    # 挂账申请表（首个真实样例）
├── sql_templates/         # SQL 模板资产库
│   └── form_query/            # 表单查询模板
├── dee_templates/         # DEE 模板资产库（待建设）
├── prompts/               # Prompt 资产库
│   └── sql/                   # SQL 生成 Prompt
├── api_docs/              # API 文档资产库（待建设）
└── error_cases/           # 错误案例库（待建设）
```

## 核心规则

1. **字段类型约束**：流程处理意见/文本域 = 普通文本；特殊字段仅 4 类（选人/选部门/下拉/附件）
2. **标准关联链路**：FORMMAIN ↔ COL_SUMMARY ↔ CTP_COMMENT_ALL ↔ CTP_AFFAIR
3. **ID 转显示值**：选人→ORG_MEMBER、选部门→ORG_UNIT、下拉→CTP_ENUM_ITEM
4. **Oracle 只读**：AI 不允许直接改数据

## 数据字典 JSON Schema

```json
{
  "formName": "表单显示名",
  "appCode": "应用模块代码",
  "tableName": "FORMMAIN_XXXX",
  "tableType": "main|son",
  "sonName": "FORMSON_XXXX（主表填写）",
  "fields": [
    {
      "fieldName": "FIELD0001",
      "displayName": "显示名",
      "dbFinalType": "VARCHAR2(100)",
      "inputType": "文本|日期|下拉|选人|选部门|上传附件|文本域|流程处理意见|金额|系统",
      "isSpecial": false,
      "refTable": "关联表名（isSpecial=true 时填写）",
      "refKey": "关联键",
      "refDisplay": "显示字段"
    }
  ]
}
```
