-- ============================================================
-- 业务场景：按部门 + 年份统计表单数量
-- 示例：数字化中心去年一共提过多少个采购申请
-- 适用：所有 FORMMAIN 表单（按部门 + 年份筛选）
-- ============================================================

SELECT
    u.NAME                              AS 部门,
    COUNT(*)                            AS 申请数量
FROM FORMMAIN_0433 f

-- 选部门：申请部门
LEFT JOIN ORG_UNIT u
    ON u.ID = f.FIELD0004

-- 流程状态：只统计已发起的
LEFT JOIN COL_SUMMARY h
    ON h.FORM_RECORDID = f.ID

WHERE 1=1
    AND u.NAME = '数字化中心'
    AND f.FIELD0001 >= TO_DATE('2025-01-01', 'YYYY-MM-DD')
    AND f.FIELD0001 <  TO_DATE('2026-01-01', 'YYYY-MM-DD')
    AND h.STATE IN (0, 3)  -- 未结束 或 已结束（排除待发）

GROUP BY u.NAME
;
