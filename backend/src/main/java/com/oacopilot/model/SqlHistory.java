package com.oacopilot.model;

import java.time.LocalDateTime;

public class SqlHistory {
    private Long id;
    private String prompt;
    private String sqlResult;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getSqlResult() { return sqlResult; }
    public void setSqlResult(String sqlResult) { this.sqlResult = sqlResult; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
