package com.oacopilot.controller;

import com.oacopilot.mapper.SqlHistoryMapper;
import com.oacopilot.model.SqlHistory;
import com.oacopilot.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sql")
public class SqlCopilotController {

    private static final Logger log = LoggerFactory.getLogger(SqlCopilotController.class);

    private final SqlHistoryMapper sqlHistoryMapper;
    private final AiService aiService;

    private static final Map<String, String> MOCK_RESULTS = new LinkedHashMap<>() {{
        put("请假", """
                SELECT f.id,
                       f.field0001 AS 申请人,
                       f.field0002 AS 部门,
                       f.field0003 AS 开始日期,
                       f.field0004 AS 结束日期,
                       f.field0005 AS 请假天数,
                       f.field0006 AS 请假事由
                FROM formmain_1001 f
                WHERE f.field0003 >= TRUNC(SYSDATE, 'MM')
                  AND f.field0003 < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
                ORDER BY f.field0003 DESC""");
        put("待办", """
                SELECT wi.id,
                       wi.title      AS 流程标题,
                       wi.sender     AS 发起人,
                       wi.create_date AS 创建时间,
                       wi.state      AS 流程状态
                FROM workflow_info wi
                WHERE wi.state = '0'
                  AND wi.user_id = :currentUserId
                ORDER BY wi.create_date DESC""");
        put("订单", """
                SELECT o.order_number   AS 订单编号,
                       o.order_date     AS 订单日期,
                       c.customer_name  AS 客户名称,
                       o.total_amount   AS 订单金额,
                       o.status         AS 订单状态
                FROM ebs_order o
                LEFT JOIN ebs_customer c ON o.customer_id = c.customer_id
                WHERE o.order_date >= ADD_MONTHS(SYSDATE, -1)
                ORDER BY o.order_date DESC""");
        put("审批", """
                SELECT f.id,
                       f.field0001 AS 单据编号,
                       f.field0002 AS 申请人,
                       f.field0003 AS 申请日期,
                       f.field0004 AS 申请金额,
                       f.field0005 AS 审批状态
                FROM formmain_2003 f
                WHERE f.field0005 = '审批中'
                ORDER BY f.field0003 DESC""");
        put("统计", """
                SELECT t.form_name AS 表单名称,
                       COUNT(*)    AS 提交数量
                FROM (
                  SELECT '请假单' AS form_name FROM formmain_1001
                   WHERE create_date >= TRUNC(SYSDATE, 'MM')
                  UNION ALL
                  SELECT '报销单' FROM formmain_2002
                   WHERE create_date >= TRUNC(SYSDATE, 'MM')
                  UNION ALL
                  SELECT '采购申请' FROM formmain_2003
                   WHERE create_date >= TRUNC(SYSDATE, 'MM')
                ) t
                GROUP BY t.form_name
                ORDER BY 提交数量 DESC""");
    }};

    private static final String DEFAULT_SQL = """
            SELECT *
            FROM formmain_1001
            WHERE create_date >= TRUNC(SYSDATE, 'MM')
            ORDER BY create_date DESC""";

    public SqlCopilotController(SqlHistoryMapper sqlHistoryMapper, AiService aiService) {
        this.sqlHistoryMapper = sqlHistoryMapper;
        this.aiService = aiService;
    }

    @GetMapping("/history")
    public List<SqlHistory> history(@RequestParam(defaultValue = "20") int limit) {
        return sqlHistoryMapper.findRecent(limit);
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        String formCode = body.getOrDefault("formCode", null);
        log.info("收到 SQL 生成请求: {}, formCode: {}", prompt, formCode);

        // 优先调用 AI（带知识资产），失败则 fallback 到 mock
        String result = aiService.generateSql(prompt, formCode);
        if (result == null) {
            log.warn("AI 返回 null，使用 mock SQL");
            result = DEFAULT_SQL;
            for (Map.Entry<String, String> entry : MOCK_RESULTS.entrySet()) {
                if (prompt.contains(entry.getKey())) {
                    result = entry.getValue();
                    break;
                }
            }
        }

        // 保存历史
        SqlHistory record = new SqlHistory();
        record.setPrompt(prompt);
        record.setSqlResult(result);
        record.setCreateTime(LocalDateTime.now());
        sqlHistoryMapper.insert(record);

        return Map.of("sql", result, "id", record.getId());
    }
}
