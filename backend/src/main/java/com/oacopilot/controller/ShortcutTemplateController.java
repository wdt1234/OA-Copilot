package com.oacopilot.controller;

import com.oacopilot.mapper.ShortcutTemplateMapper;
import com.oacopilot.model.ShortcutTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shortcut-templates")
public class ShortcutTemplateController {

    private static final Logger log = LoggerFactory.getLogger(ShortcutTemplateController.class);

    private final ShortcutTemplateMapper mapper;

    public ShortcutTemplateController(ShortcutTemplateMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 获取所有模板（按分类返回）
     */
    @GetMapping
    public Map<String, List<ShortcutTemplate>> getAll() {
        List<ShortcutTemplate> all = mapper.findAll();
        Map<String, List<ShortcutTemplate>> result = new HashMap<>();
        List<ShortcutTemplate> sqlTemplates = all.stream()
                .filter(t -> "sql".equals(t.getCategory()))
                .toList();
        List<ShortcutTemplate> deeTemplates = all.stream()
                .filter(t -> "dee".equals(t.getCategory()))
                .toList();
        result.put("sql", sqlTemplates);
        result.put("dee", deeTemplates);
        return result;
    }

    /**
     * 获取指定分类的模板
     */
    @GetMapping("/{category}")
    public List<ShortcutTemplate> getByCategory(@PathVariable String category) {
        return mapper.findByCategory(category);
    }

    /**
     * 新增模板
     */
    @PostMapping
    public ShortcutTemplate create(@RequestBody Map<String, String> body) {
        String category = body.getOrDefault("category", "sql");
        String content = body.getOrDefault("content", "");

        ShortcutTemplate template = new ShortcutTemplate();
        template.setCategory(category);
        template.setContent(content);
        template.setSortOrder(mapper.countByCategory(category));
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());

        mapper.insert(template);
        log.info("新增快捷模板: category={}, id={}", category, template.getId());
        return template;
    }

    /**
     * 更新模板内容
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        mapper.update(id, content, LocalDateTime.now());
        log.info("更新快捷模板: id={}", id);
        return Map.of("message", "更新成功");
    }

    /**
     * 更新排序
     */
    @PutMapping("/{id}/sort")
    public Map<String, Object> updateSortOrder(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer sortOrder = body.getOrDefault("sortOrder", 0);
        mapper.updateSortOrder(id, sortOrder);
        return Map.of("message", "排序已更新");
    }

    /**
     * 批量更新排序（拖拽排序用）
     */
    @PutMapping("/batch-sort")
    public Map<String, Object> batchUpdateSort(@RequestBody List<Map<String, Object>> body) {
        for (Map<String, Object> item : body) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = (Integer) item.get("sortOrder");
            mapper.updateSortOrder(id, sortOrder);
        }
        return Map.of("message", "批量排序已更新");
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        log.info("删除快捷模板: id={}", id);
        return Map.of("message", "已删除");
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Map<String, Object> deleteBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        if (ids.isEmpty()) {
            return Map.of("deleted", 0);
        }
        String idsStr = ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        mapper.deleteByIds(idsStr);
        log.info("批量删除快捷模板: {}", idsStr);
        return Map.of("deleted", ids.size());
    }

    /**
     * 重置为默认模板
     */
    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestBody Map<String, String> body) {
        String category = body.getOrDefault("category", "sql");
        mapper.deleteByCategory(category);

        // 插入默认模板
        List<String> defaults = switch (category) {
            case "dee" -> List.of(
                "OA普通表单查询，查询请假单",
                "OA主子表查询，查询报销明细",
                "OA流程表单查询，统计已结束的流程"
            );
            default -> List.of(
                "查询全量数据-如有明细表需包含明细表",
                "查询待办流程列表",
                "查询审批记录-按流程标题搜索"
            );
        };

        for (int i = 0; i < defaults.size(); i++) {
            ShortcutTemplate template = new ShortcutTemplate();
            template.setCategory(category);
            template.setContent(defaults.get(i));
            template.setSortOrder(i);
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());
            mapper.insert(template);
        }

        log.info("重置快捷模板: category={}, count={}", category, defaults.size());
        return Map.of("message", "已重置为默认模板", "count", defaults.size());
    }
}
