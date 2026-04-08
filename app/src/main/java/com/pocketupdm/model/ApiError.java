package com.pocketupdm.model;

public class ApiError {
    private String error;
    private int status;
    public ApiError(String error, int status) {
        this.error = error;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
