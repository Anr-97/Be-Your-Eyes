package xyz.lanshive.beyoureyes.model;

import java.util.List;

public class HelpRecordsResponse {
    private String status;
    private String message;
    private List<HelpRecord> records;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<HelpRecord> getRecords() {
        return records;
    }

    public void setRecords(List<HelpRecord> records) {
        this.records = records;
    }
} 