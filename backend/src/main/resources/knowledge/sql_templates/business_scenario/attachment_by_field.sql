-- 按表单附件字段查询附件信息
-- 场景：查询某个表单记录的上传附件字段数据及其对应的附件文件名
-- 关键：CTP_ATTACHMENT.SUB_REFERENCE 存的是附件 ID（与表单字段值相同）

SELECT
    m.FIELD0025                         AS 表单编号,
    m.FIELD0068                         AS 电子发票附件ID,
    a1.FILENAME                         AS 电子发票文件名,
    m.FIELD0117                         AS 其他资料附件ID,
    a2.FILENAME                         AS 其他资料文件名
FROM FORMMAIN_10567 m
LEFT JOIN CTP_ATTACHMENT a1 ON a1.SUB_REFERENCE = m.FIELD0068
LEFT JOIN CTP_ATTACHMENT a2 ON a2.SUB_REFERENCE = m.FIELD0117
WHERE m.FIELD0025 = 'RAP-20260519-008'
