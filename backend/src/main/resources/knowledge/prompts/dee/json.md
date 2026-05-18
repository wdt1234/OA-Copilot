# DEE 字段映射模板

## 系统约束

### 映射结构

- formId：表单 ID（如 formmain_XXXX）
- mappings：字段映射数组

### Mapping 字段

| 字段 | 说明 | 示例 |
|------|------|------|
| source | 源字段（OA 字段名） | field0001 |
| target | 目标字段（ERP/业务系统字段名） | billNo |
| type | 数据类型 | string, number, date, boolean |

### Type 映射规则

| type | 适用场景 | 示例值 |
|------|---------|--------|
| string | 文本、编号、名称 | "FIELD0001" → "billNo" |
| number | 金额、数量、ID | "FIELD0004" → "amount" |
| date | 日期字段 | "FIELD0003" → "applyDate" |
| boolean | 开关、是否 | "FIELD0005" → "isActive" |

## 生成规则

1. 根据表单字段列表生成映射
2. source 使用 OA 字段名（fieldXXXX）
3. target 使用目标系统字段名（驼峰或下划线）
4. type 根据 OA 字段类型推断
5. 只返回 JSON，不要解释

## 输出格式

- 只返回 JSON
- 不返回解释
- 外层为大括号 {}
- mappings 为数组
