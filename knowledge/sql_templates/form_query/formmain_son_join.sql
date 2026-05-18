-- ============================================================
-- 模板：主表 + 明细表 JOIN + 显示值
-- 适用：FORMMAIN_XXXX + FORMSON_XXXX 主从结构
-- 用法：替换表名、字段名、关联条件即可复用
-- ============================================================

SELECT
    -- 主表字段
    f.ID                               AS 主表ID,
    f.FIELD0001                        AS 申请编号,
    f.FIELD0004                        AS 申请日期,
    f.FIELD0005                        AS 供应商名称,
    f.FIELD0006                        AS 挂账金额,

    -- 主表选人/选部门/下拉显示值
    m_field0002.NAME                   AS 申请人,
    u_field0003.NAME                   AS 申请部门,
    e_field0007.SHOWVALUE              AS 币种,
    e_field0008.SHOWVALUE              AS 挂账类型,

    -- 明细表字段
    s.ID                               AS 明细ID,
    s.FIELD0002                        AS 费用说明,
    s.FIELD0003                        AS 明细挂账金额,
    s.FIELD0004                        AS 税率,
    s.FIELD0005                        AS 税额,
    s.FIELD0006                        AS 不含税金额,
    s.FIELD0007                        AS 成本科目,

    -- 明细表下拉显示值
    e_s_field0001.SHOWVALUE            AS 费用类型

FROM FORMMAIN_10567 f

-- 主从关联
INNER JOIN FORMSON_10569 s
    ON s.MAINID = f.ID

-- 主表：选人
LEFT JOIN ORG_MEMBER m_field0002
    ON m_field0002.ID = f.FIELD0002

-- 主表：选部门
LEFT JOIN ORG_UNIT u_field0003
    ON u_field0003.ID = f.FIELD0003

-- 主表：下拉
LEFT JOIN CTP_ENUM_ITEM e_field0007
    ON e_field0007.ID = f.FIELD0007
LEFT JOIN CTP_ENUM_ITEM e_field0008
    ON e_field0008.ID = f.FIELD0008

-- 明细表：下拉
LEFT JOIN CTP_ENUM_ITEM e_s_field0001
    ON e_s_field0001.ID = s.FIELD0001

WHERE 1=1
    -- AND f.FIELD0004 >= TRUNC(SYSDATE, 'MM')  -- 本月
ORDER BY f.FIELD0004 DESC, s.ID ASC
;
