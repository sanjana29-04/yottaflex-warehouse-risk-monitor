package com.warehouse.riskmonitor.dto;

public class AlertActionDto {
    private String user;
    private String notes;

    public AlertActionDto() {}

    public AlertActionDto(String user, String notes) {
        this.user = user;
        this.notes = notes;
    }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

