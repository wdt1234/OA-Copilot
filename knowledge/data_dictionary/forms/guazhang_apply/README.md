# 挂账申请表（guazhang_apply）

## 基本信息

- 表单名称：挂账申请表
- 应用模块：FIN（财务）
- 主表：FORMMAIN_10567
- 明细表：FORMSON_10569
- 关联关系：FORMSON_10569.MAINID = FORMMAIN_10567.ID

## 特殊字段清单

以下字段存 ID，查询时必须关联表转显示值：

| 字段 | 显示名 | 类型 | 关联表 | 关联键 | 显示字段 |
|------|--------|------|--------|--------|----------|
| FIELD0002 | 申请人 | 选人 | ORG_MEMBER | ID | NAME |
| FIELD0003 | 申请部门 | 选部门 | ORG_UNIT | ID | NAME |
| FIELD0007 | 币种 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |
| FIELD0008 | 挂账类型 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |
| FIELD0010 | 发票情况 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |
| FIELD0013 | 成本中心 | 选部门 | ORG_UNIT | ID | NAME |
| FIELD0015 | 附件 | 上传附件 | CTP_ATTACHMENT | ID | FILENAME |

## 明细表特殊字段

| 字段 | 显示名 | 类型 | 关联表 | 关联键 | 显示字段 |
|------|--------|------|--------|--------|----------|
| FIELD0001 | 费用类型 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |

## 非特殊字段（普通文本/日期/金额）

以下字段在数据库中为普通值，无特殊语义处理：

- FIELD0001 申请编号（文本）
- FIELD0004 申请日期（日期）
- FIELD0005 供应商名称（文本）
- FIELD0006 挂账金额（金额）
- FIELD0009 挂账事由（文本域）→ **普通文本，无特殊语义**
- FIELD0011 预计冲账日期（日期）
- FIELD0012 关联合同编号（文本）
- FIELD0014 备注（文本域）→ **普通文本，无特殊语义**

## 常用查询场景

1. **查询本月挂账申请列表**：主表 + 选人/选部门/下拉显示值
2. **查询挂账明细汇总**：主表 + 明细表 join + 费用类型显示值
3. **按部门统计挂账金额**：主表 + ORG_UNIT 关联
4. **查询待审批挂账**：主表 + COL_SUMMARY 流程状态关联
5. **查询挂账审批记录**：主表 + COL_SUMMARY + CTP_COMMENT_ALL + CTP_AFFAIR

## 标准流程链路

```
FORMMAIN_10567.id = COL_SUMMARY.form_recordid
COL_SUMMARY.id    = CTP_COMMENT_ALL.module_id
CTP_COMMENT_ALL.id = CTP_AFFAIR.affair_id
```

## 文件清单

- formmain_10567.json — 主表字段定义
- formson_10569.json — 明细表字段定义
- README.md — 本说明文件
