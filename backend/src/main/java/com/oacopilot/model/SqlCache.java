package com.oacopilot.model;

import java.time.LocalDateTime;

public class SqlCache {
    private Long id;
    private String cacheKey;
    private String prompt;
    private String formCode;
    private String sqlResult;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getFormCode() { return formCode; }
    public void setFormCode(String formCode) { this.formCode = formCode; }

    public String getSqlResult() { return sqlResult; }
    public void setSqlResult(String sqlResult) { this.sqlResult = sqlResult; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
