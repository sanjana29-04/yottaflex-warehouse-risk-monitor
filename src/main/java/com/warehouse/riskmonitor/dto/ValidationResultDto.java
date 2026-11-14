package com.warehouse.riskmonitor.dto;

public class ValidationResultDto {
    private int ruleNumber;
    private String category;
    private String ruleName;
    private String status; // PASSED or FAILED
    private String details;
    private long recordsEvaluated;
    private long violationsCount;

    public ValidationResultDto() {}

    public ValidationResultDto(int ruleNumber, String category, String ruleName, String status, String details, long recordsEvaluated, long violationsCount) {
        this.ruleNumber = ruleNumber;
        this.category = category;
        this.ruleName = ruleName;
        this.status = status;
        this.details = details;
        this.recordsEvaluated = recordsEvaluated;
        this.violationsCount = violationsCount;
    }

    public int getRuleNumber() { return ruleNumber; }
    public void setRuleNumber(int ruleNumber) { this.ruleNumber = ruleNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public long getRecordsEvaluated() { return recordsEvaluated; }
    public void setRecordsEvaluated(long recordsEvaluated) { this.recordsEvaluated = recordsEvaluated; }

    public long getViolationsCount() { return violationsCount; }
    public void setViolationsCount(long violationsCount) { this.violationsCount = violationsCount; }
}

