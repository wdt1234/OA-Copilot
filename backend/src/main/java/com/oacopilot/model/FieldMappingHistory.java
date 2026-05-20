package com.oacopilot.model;

import java.time.LocalDateTime;

public class FieldMappingHistory {
    private Long id;
    private String formId;
    private String inputFields;
    private String resultJson;
    private boolean isPinned;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }

    public String getInputFields() { return inputFields; }
    public void setInputFields(String inputFields) { this.inputFields = inputFields; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
