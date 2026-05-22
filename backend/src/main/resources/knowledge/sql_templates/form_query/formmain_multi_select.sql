-- ============================================================
-- 模板：选多人字段标准模式（REGEXP_SUBSTR 拆分 → 统一 JOIN → 聚合 → 回填）
-- 适用：选人字段存多个 ID 逗号分隔（如 "123,456,789"）
-- 用法：替换 FORMMAIN_XXXX、字段名即可复用
--
-- 性能：ORG_MEMBER 只扫描一次，FORMMAIN 只扫描一次
-- 禁止：每字段独立 CTE + LIKE、SELECT 中行级子查询扫描 ORG_MEMBER
-- ============================================================

-- ========== Step 1: REGEXP_SUBSTR 拆分所有多人字段 ==========
WITH all_members AS (
    SELECT id AS form_id, 'FIELD0082' AS field_code,
           TRIM(REGEXP_SUBSTR(FIELD0082, '[^,]+', 1, LEVEL)) AS member_id
    FROM FORMMAIN_XXXX WHERE FIELD0082 IS NOT NULL
    CONNECT BY id = PRIOR id AND PRIOR SYS_GUID() IS NOT NULL
      AND LEVEL <= REGEXP_COUNT(FIELD0082, ',') + 1
    UNION ALL
    SELECT id, 'FIELD0083', TRIM(REGEXP_SUBSTR(FIELD0083, '[^,]+', 1, LEVEL))
    FROM FORMMAIN_XXXX WHERE FIELD0083 IS NOT NULL
    CONNECT BY id = PRIOR id AND PRIOR SYS_GUID() IS NOT NULL
      AND LEVEL <= REGEXP_COUNT(FIELD0083, ',') + 1
    UNION ALL
    SELECT id, 'FIELD0084', TRIM(REGEXP_SUBSTR(FIELD0084, '[^,]+', 1, LEVEL))
    FROM FORMMAIN_XXXX WHERE FIELD0084 IS NOT NULL
    CONNECT BY id = PRIOR id AND PRIOR SYS_GUID() IS NOT NULL
      AND LEVEL <= REGEXP_COUNT(FIELD0084, ',') + 1
),

-- ========== Step 2: 统一 JOIN ORG_MEMBER（只扫描一次）==========
member_names AS (
    SELECT a.form_id, a.field_code, om.NAME
    FROM all_members a
    JOIN ORG_MEMBER om ON om.ID = a.member_id
    WHERE om.STATE = 1 AND om.IS_ENABLE = 1 AND om.IS_DELETED = 0
),

-- ========== Step 3: 统一聚合 ==========
member_agg AS (
    SELECT form_id, field_code,
           LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME) AS names
    FROM member_names
    GROUP BY form_id, field_code
)

-- ========== Step 4: 主查询回填 ==========
SELECT
    m.ID,
    m.FIELD0001                        AS 单据编号,

    -- 单选人字段：直接 LEFT JOIN
    om_single.NAME                     AS 单选人姓名,

    -- 单选部门字段：直接 LEFT JOIN
    ou_single.NAME                     AS 单选部门名称,

    -- 下拉字段：直接 LEFT JOIN
    ei_select.SHOWVALUE                AS 下拉显示值,

    -- 多选人字段：CASE WHEN 回填
    MAX(CASE WHEN ma.field_code = 'FIELD0082' THEN ma.names END) AS 多选人1,
    MAX(CASE WHEN ma.field_code = 'FIELD0083' THEN ma.names END) AS 多选人2,
    MAX(CASE WHEN ma.field_code = 'FIELD0084' THEN ma.names END) AS 多选人3,

    -- 普通字段
    m.FIELD0006                        AS 金额

FROM FORMMAIN_XXXX m

-- 多选人聚合结果
LEFT JOIN member_agg ma ON ma.form_id = m.ID

-- 单选人
LEFT JOIN ORG_MEMBER om_single ON om_single.ID = m.FIELDYYYY
    AND om_single.STATE = 1 AND om_single.IS_ENABLE = 1 AND om_single.IS_DELETED = 0

-- 单选部门
LEFT JOIN ORG_UNIT ou_single ON ou_single.ID = m.FIELDZZZZ
    AND ou_single.IS_ENABLE = 1 AND ou_single.IS_DELETED = 0

-- 下拉
LEFT JOIN CTP_ENUM_ITEM ei_select ON ei_select.ID = m.FIELDWWWW
    AND ei_select.STATE = 1

GROUP BY m.ID, m.FIELD0001, om_single.NAME, ou_single.NAME, ei_select.SHOWVALUE, m.FIELD0006
ORDER BY m.START_DATE DESC
;
