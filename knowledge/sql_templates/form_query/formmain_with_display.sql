-- ============================================================
-- 模板：主表查询 + 选人/选部门/下拉 显示值
-- 适用：FORMMAIN_XXXX 主表，把 ID 字段转为可读名称
-- 用法：替换 FORMMAIN_XXXX、字段名、关联表即可复用
--
-- 注意：选人/选部门/下拉字段存的是 ID（可能为负数）
-- 关联时使用 CAST 或直接比较，Oracle 会自动处理
-- ============================================================

SELECT
    f.ID,
    -- 普通字段直接取
    f.FIELD0001                        AS 申请编号,
    f.FIELD0004                        AS 申请日期,
    f.FIELD0005                        AS 供应商名称,
    f.FIELD0006                        AS 挂账金额,
    f.FIELD0009                        AS 挂账事由,
    f.FIELD0011                        AS 预计冲账日期,
    f.FIELD0012                        AS 关联合同编号,
    -- ========== ID 转显示值：选人 ==========
    m_field0002.NAME                   AS 申请人,
    -- ========== ID 转显示值：选部门 ==========
    u_field0003.NAME                   AS 申请部门,
    u_field0013.NAME                   AS 成本中心,
    -- ========== ID 转显示值：下拉框 ==========
    e_field0007.SHOWVALUE              AS 币种,
    e_field0008.SHOWVALUE              AS 挂账类型,
    e_field0010.SHOWVALUE              AS 发票情况,
    -- 流程状态（从 COL_SUMMARY 获取）
    s.STATE                            AS 流程状态码,
    CASE s.STATE
        WHEN 0 THEN '未结束'
        WHEN 1 THEN '终止'
        WHEN 2 THEN '待发'
        WHEN 3 THEN '已结束'
    END                                AS 流程状态,
    s.START_TIME                       AS 流程发起时间
FROM FORMMAIN_10567 f

-- 选人：申请人
LEFT JOIN ORG_MEMBER m_field0002
    ON m_field0002.ID = f.FIELD0002

-- 选部门：申请部门
LEFT JOIN ORG_UNIT u_field0003
    ON u_field0003.ID = f.FIELD0003

-- 选部门：成本中心
LEFT JOIN ORG_UNIT u_field0013
    ON u_field0013.ID = f.FIELD0013

-- 下拉：币种
LEFT JOIN CTP_ENUM_ITEM e_field0007
    ON e_field0007.ID = f.FIELD0007

-- 下拉：挂账类型
LEFT JOIN CTP_ENUM_ITEM e_field0008
    ON e_field0008.ID = f.FIELD0008

-- 下拉：发票情况
LEFT JOIN CTP_ENUM_ITEM e_field0010
    ON e_field0010.ID = f.FIELD0010

-- 流程状态：COL_SUMMARY
LEFT JOIN COL_SUMMARY s
    ON s.FORM_RECORDID = f.ID

WHERE 1=1
    -- AND f.FIELD0004 >= TRUNC(SYSDATE, 'MM')  -- 本月
ORDER BY f.FIELD0004 DESC
;
