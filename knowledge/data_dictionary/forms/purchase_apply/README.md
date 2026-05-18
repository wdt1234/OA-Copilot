# 采购申请表（purchase_apply）

## 基本信息

- 表单名称：采购申请表
- 应用模块：PUR（采购）
- 主表：FORMMAIN_0433
- 明细表：FORMSON_0434
- 关联关系：FORMSON_0434.MAINID = FORMMAIN_0433.ID

## 特殊字段清单

以下字段存 ID，查询时必须关联表转显示值：

| 字段 | 显示名 | 类型 | 关联表 | 关联键 | 显示字段 |
|------|--------|------|--------|--------|----------|
| FIELD0004 | 申请部门 | 选部门 | ORG_UNIT | ID | NAME |
| FIELD0030 | 选择采购员 | 选人 | ORG_MEMBER | ID | NAME |
| FIELD0050 | 申请人1 | 选人 | ORG_MEMBER | ID | NAME |
| FIELD0064 | 验收标准 | 上传附件 | CTP_ATTACHMENT | ID | FILENAME |
| FIELD0065 | 是否有附件 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |
| FIELD0070 | 是否标准件 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |
| FIELD0071 | 上传相关标准 | 上传附件 | CTP_ATTACHMENT | ID | FILENAME |
| FIELD0086 | 是否为非订单类 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |
| FIELD0087 | 是否为订单类 | 下拉 | CTP_ENUM_ITEM | ID | SHOWVALUE |
| FIELD0088 | 对应采购工程师 | 选人 | ORG_MEMBER | ID | NAME |

## 非特殊字段（普通文本/日期/金额/流程处理意见/文本域）

以下字段在数据库中为普通值，无特殊语义处理：

- FIELD0001 申请单号（文本）
- FIELD0002 申请日期（日期）
- FIELD0003 申请人（文本）→ 注意：此为文本字段，非选人
- FIELD0005 描述（文本域）→ **普通文本，无特殊语义**
- FIELD0006-FIELD0019 各类业务字段（文本）
- FIELD0020-FIELD0031 流程处理意见 → **普通文本，无特殊语义**
- FIELD0044-FIELD0090 各类业务字段（文本/金额）

## 流程处理意见字段（全部为普通文本）

| 字段 | 显示名 |
|------|--------|
| FIELD0020 | 一级审批 |
| FIELD0022 | 二级审批 |
| FIELD0023 | 三级审批 |
| FIELD0024 | 四级审批 |
| FIELD0025 | 归口部门负责人审批 |
| FIELD0026 | 预算会计审批 |
| FIELD0027 | 财务总监审批 |
| FIELD0028 | 总经理审批 |
| FIELD0029 | 采购部门负责人审批 |
| FIELD0031 | 采购工程师审批 |
| FIELD0051 | 项目管理审批 |
| FIELD0059 | 供应链部门负责人审批 |

## 常用查询场景

1. **查询本月采购申请列表**：主表 + 选人/选部门/下拉显示值
2. **查询采购申请明细**：主表 + 明细表 join
3. **按部门统计采购金额**：主表 + ORG_UNIT 关联
4. **查询待审批采购申请**：主表 + COL_SUMMARY 流程状态关联
5. **查询采购申请审批记录**：主表 + COL_SUMMARY + CTP_COMMENT_ALL + CTP_AFFAIR

## 标准流程链路

```
FORMMAIN_0433.id = COL_SUMMARY.form_recordid
COL_SUMMARY.id    = CTP_COMMENT_ALL.module_id
CTP_COMMENT_ALL.id = CTP_AFFAIR.affair_id
```

## 文件清单

- formmain_0433.json — 主表字段定义
- formson_0434.json — 明细表字段定义
- README.md — 本说明文件
