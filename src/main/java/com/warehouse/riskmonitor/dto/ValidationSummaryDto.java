package com.warehouse.riskmonitor.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ValidationSummaryDto {
    private LocalDateTime timestamp = LocalDateTime.now();
    private int totalRulesRun;
    private int passedRules;
    private int failedRules;
    private boolean fullyValid;
    private List<ValidationResultDto> results = new ArrayList<>();

    public ValidationSummaryDto() {}

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getTotalRulesRun() { return totalRulesRun; }
    public void setTotalRulesRun(int totalRulesRun) { this.totalRulesRun = totalRulesRun; }

    public int getPassedRules() { return passedRules; }
    public void setPassedRules(int passedRules) { this.passedRules = passedRules; }

    public int getFailedRules() { return failedRules; }
    public void setFailedRules(int failedRules) { this.failedRules = failedRules; }

    public boolean isFullyValid() { return fullyValid; }
    public void setFullyValid(boolean fullyValid) { this.fullyValid = fullyValid; }

    public List<ValidationResultDto> getResults() { return results; }
    public void setResults(List<ValidationResultDto> results) { this.results = results; }
}

