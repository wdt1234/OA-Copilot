import java.util.HashMap;
import java.util.Map;

import com.seeyon.ctp.util.JSONUtil;
import com.seeyon.dee.context.DeeContext;

/**
 * DEE 工作流回调 Handler
 * 流程审批通过后回调，调用 ERP 下单接口
 */
public class DeeWorkflowHandler {

    public Map<String, Object> handle(DeeContext context) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 获取表单数据
            String formData = context.getFormData();
            Map<String, Object> data = JSONUtil.parse(formData);

            // 2. 提取字段
            String billNo = (String) data.get("field0001");
            Double amount = ((Number) data.get("field0004")).doubleValue();
            String applicant = (String) data.get("field0002");

            // 3. 调用 ERP 接口
            ErpResponse erpResult = ErpClient.sendOrder(billNo, amount);

            // 4. 返回结果
            result.put("success", erpResult.isSuccess());
            result.put("message", erpResult.getMessage());
            result.put("billNo", billNo);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "ERP 调用异常: " + e.getMessage());
        }

        return result;
    }
}
