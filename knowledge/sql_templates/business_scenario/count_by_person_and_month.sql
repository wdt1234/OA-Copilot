-- ============================================================
-- 业务场景：按人员 + 月份统计表单数量
-- 示例：查王得童 5 月份发起的采购申请有多少条
-- 适用：所有 FORMMAIN 表单（按发起人 + 时间段筛选）
-- ============================================================

SELECT
    m.NAME                              AS 发起人,
    COUNT(*)                            AS 申请数量
FROM FORMMAIN_0433 f

-- 选人：申请人（TO_CHAR 兼容）
LEFT JOIN ORG_MEMBER m
    ON TO_CHAR(m.ID) = f.FIELD0030

-- 流程状态：只统计已发起的
LEFT JOIN COL_SUMMARY h
    ON h.FORM_RECORDID = f.ID

WHERE 1=1
    AND m.NAME = '王得童'
    AND f.FIELD0001 >= TO_DATE('2026-05-01', 'YYYY-MM-DD')
    AND f.FIELD0001 <  TO_DATE('2026-06-01', 'YYYY-MM-DD')
    AND h.STATE IN (0, 3)  -- 未结束 或 已结束（排除待发）

GROUP BY m.NAME
;
