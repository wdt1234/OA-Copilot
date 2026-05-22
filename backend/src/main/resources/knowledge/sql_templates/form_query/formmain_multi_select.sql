-- ============================================================
-- 模板：选多人字段标准模式（CTE 预关联 + LISTAGG 回填）
-- 适用：选人字段存多个 ID 逗号分隔（如 "123,456,789"）
-- 用法：替换 FORMMAIN_XXXX、字段名即可复用
--
-- 禁止在 SELECT 中直接扫描 ORG_MEMBER，会导致 SQL 卡死
-- ============================================================

-- ========== Step 1: 每个多人字段一个 CTE ==========

WITH member_map_0082 AS (
    SELECT m.id AS form_id, om.NAME
    FROM FORMMAIN_XXXX m
    JOIN ORG_MEMBER om ON ',' || m.FIELD0082 || ',' LIKE '%,' || om.ID || ',%'
    WHERE om.STATE = 1 AND om.IS_ENABLE = 1 AND om.IS_DELETED = 0
),
member_map_0083 AS (
    SELECT m.id AS form_id, om.NAME
    FROM FORMMAIN_XXXX m
    JOIN ORG_MEMBER om ON ',' || m.FIELD0083 || ',' LIKE '%,' || om.ID || ',%'
    WHERE om.STATE = 1 AND om.IS_ENABLE = 1 AND om.IS_DELETED = 0
),
member_map_0084 AS (
    SELECT m.id AS form_id, om.NAME
    FROM FORMMAIN_XXXX m
    JOIN ORG_MEMBER om ON ',' || m.FIELD0084 || ',' LIKE '%,' || om.ID || ',%'
    WHERE om.STATE = 1 AND om.IS_ENABLE = 1 AND om.IS_DELETED = 0
)

-- ========== Step 2: 主查询 ==========

SELECT
    m.ID,
    m.FIELD0001                        AS 单据编号,

    -- 单选人字段：直接 LEFT JOIN
    om_single.NAME                     AS 单选人姓名,

    -- 单选部门字段：直接 LEFT JOIN
    ou_single.NAME                     AS 单选部门名称,

    -- 下拉字段：直接 LEFT JOIN
    ei_select.SHOWVALUE                AS 下拉显示值,

    -- 多选人字段：CTE 回填
    (SELECT LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME)
     FROM member_map_0082 mm WHERE mm.form_id = m.ID) AS 多选人1,

    (SELECT LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME)
     FROM member_map_0083 mm WHERE mm.form_id = m.ID) AS 多选人2,

    (SELECT LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME)
     FROM member_map_0084 mm WHERE mm.form_id = m.ID) AS 多选人3,

    -- 普通字段
    m.FIELD0006                        AS 金额

FROM FORMMAIN_XXXX m

-- 单选人
LEFT JOIN ORG_MEMBER om_single ON om_single.ID = m.FIELDYYYY
    AND om_single.STATE = 1 AND om_single.IS_ENABLE = 1 AND om_single.IS_DELETED = 0

-- 单选部门
LEFT JOIN ORG_UNIT ou_single ON ou_single.ID = m.FIELDZZZZ
    AND ou_single.IS_ENABLE = 1 AND ou_single.IS_DELETED = 0

-- 下拉
LEFT JOIN CTP_ENUM_ITEM ei_select ON ei_select.ID = m.FIELDWWWW
    AND ei_select.STATE = 1

WHERE 1=1
ORDER BY m.START_DATE DESC
;
