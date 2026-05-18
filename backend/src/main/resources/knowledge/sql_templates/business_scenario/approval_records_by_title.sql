-- ============================================================
-- 业务场景：按流程标题查审批记录
-- 示例：查零件采购价格审批表-20250328-005-付超 的审批记录
-- 适用：所有流程（通过标题模糊搜索审批轨迹）
-- ============================================================

SELECT
    t.NODE_NAME                         AS 节点名,
    t.SUBJECT                           AS 流程名称,
    t.CREATE_DATE                       AS 流程开始时间,
    t.RECEIVE_TIME                      AS 接收时间,
    t.COMPLETE_TIME                     AS 处理时间,
    b.EXT_ATT4                          AS 是否同意,
    b.CONTENT                           AS 处理意见
FROM CTP_AFFAIR t

-- 审批意见关联
LEFT JOIN CTP_COMMENT_ALL b
    ON b.AFFAIR_ID = t.ID

WHERE 1=1
    AND t.SUBJECT LIKE '%零件采购价格审批表-20250328-005-付超%'

ORDER BY t.SUBJECT, t.RECEIVE_TIME DESC
;
