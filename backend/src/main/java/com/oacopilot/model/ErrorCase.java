package com.oacopilot.model;

public class ErrorCase {
    private Long id;
    private String category;
    private String title;
    private String symptom;
    private String cause;
    private String solution;
    private String exampleWrong;
    private String exampleCorrect;
    private String tags;
    private Integer isPinned;
    private String createTime;
    private String updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSymptom() { return symptom; }
    public void setSymptom(String symptom) { this.symptom = symptom; }

    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }

    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }

    public String getExampleWrong() { return exampleWrong; }
    public void setExampleWrong(String exampleWrong) { this.exampleWrong = exampleWrong; }

    public String getExampleCorrect() { return exampleCorrect; }
    public void setExampleCorrect(String exampleCorrect) { this.exampleCorrect = exampleCorrect; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Integer getIsPinned() { return isPinned; }
    public void setIsPinned(Integer isPinned) { this.isPinned = isPinned; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
