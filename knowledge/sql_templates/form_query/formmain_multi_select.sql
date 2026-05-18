-- ============================================================
-- 模板：多选字段拆分 + 聚合
-- 适用：选人/选部门字段存多个 ID 逗号分隔（如 "123,456,789"）
-- 用法：替换 FORMMAIN_XXXX、字段名、关联表即可复用
--
-- 场景：平台经理、其他多选人员字段
-- ============================================================

-- ========== 多选人字段：拆分 → JOIN ORG_MEMBER → LISTAGG 聚合 ==========

WITH split_member AS (
    SELECT
        a.id AS main_id,
        REGEXP_SUBSTR(a.FIELDXXXX, '[^,]+', 1, LEVEL) AS member_id
    FROM FORMMAIN_XXXX a
    WHERE a.FIELDXXXX IS NOT NULL
    CONNECT BY REGEXP_SUBSTR(a.FIELDXXXX, '[^,]+', 1, LEVEL) IS NOT NULL
        AND PRIOR a.id = a.id
        AND PRIOR SYS_GUID() IS NOT NULL
),
member_agg AS (
    SELECT
        s.main_id,
        LISTAGG(m.NAME, '、') WITHIN GROUP (ORDER BY m.NAME) AS display_name
    FROM split_member s
    JOIN ORG_MEMBER m ON TO_CHAR(m.id) = s.member_id
    GROUP BY s.main_id
)

-- ========== 多选部门字段：拆分 → JOIN ORG_UNIT → LISTAGG 聚合 ==========

-- WITH split_dept AS (
--     SELECT
--         a.id AS main_id,
--         REGEXP_SUBSTR(a.FIELDXXXX, '[^,]+', 1, LEVEL) AS dept_id
--     FROM FORMMAIN_XXXX a
--     WHERE a.FIELDXXXX IS NOT NULL
--     CONNECT BY REGEXP_SUBSTR(a.FIELDXXXX, '[^,]+', 1, LEVEL) IS NOT NULL
--         AND PRIOR a.id = a.id
--         AND PRIOR SYS_GUID() IS NOT NULL
-- ),
-- dept_agg AS (
--     SELECT
--         s.main_id,
--         LISTAGG(u.NAME, '、') WITHIN GROUP (ORDER BY u.NAME) AS display_name
--     FROM split_dept s
--     JOIN ORG_UNIT u ON TO_CHAR(u.id) = s.dept_id
--     GROUP BY s.main_id
-- )

-- ========== 主查询：关联多选聚合结果 ==========

SELECT
    a.ID,
    a.FIELD0001                        AS 单据编号,
    a.FIELD0003                        AS 申请人,
    -- 多选人字段：已聚合的姓名
    ma.display_name                    AS 多选人姓名,
    -- 单选人字段：直接 JOIN
    om_single.NAME                     AS 单选人姓名,
    -- 单选部门字段：直接 JOIN
    ou_single.NAME                     AS 单选部门名称,
    -- 下拉字段：直接 JOIN
    ei_select.SHOWVALUE                AS 下拉显示值,
    -- 普通字段
    a.FIELD0006                        AS 金额
FROM FORMMAIN_XXXX a

-- 多选人聚合
LEFT JOIN member_agg ma ON ma.main_id = a.id

-- 单选人
LEFT JOIN ORG_MEMBER om_single ON om_single.ID = a.FIELDYYYY

-- 单选部门
LEFT JOIN ORG_UNIT ou_single ON ou_single.ID = a.FIELDZZZZ

-- 下拉
LEFT JOIN CTP_ENUM_ITEM ei_select ON ei_select.ID = a.FIELDWWWW

WHERE 1=1
ORDER BY a.ID DESC
;
