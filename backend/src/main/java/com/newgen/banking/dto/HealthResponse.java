package com.newgen.banking.dto;

/**
 * Data Transfer Object representing the health status response.
 */
public class HealthResponse {

    private String status;
    private String message;
    private String application;

    public HealthResponse() {
    }

    public HealthResponse(String status, String message, String application) {
        this.status = status;
        this.message = message;
        this.application = application;
    }

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

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}
