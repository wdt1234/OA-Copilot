package com.oacopilot.model;

import java.time.LocalDateTime;

public class SqlTask {
    private Long id;
    private String taskId;
    private String prompt;
    private String formCode;
    private String status;  // PENDING, PROCESSING, COMPLETED, FAILED
    private String sqlResult;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getFormCode() { return formCode; }
    public void setFormCode(String formCode) { this.formCode = formCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSqlResult() { return sqlResult; }
    public void setSqlResult(String sqlResult) { this.sqlResult = sqlResult; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
