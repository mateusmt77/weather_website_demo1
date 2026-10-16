package com.exceptions;

public class APIConnectionException extends RuntimeException {

    private int statusError;
    
    public APIConnectionException(int statusError) {
        super("Não foi possível conectar à API: " + statusError);
        this.statusError = statusError;
    }

    public int getStatusError() {
        return statusError;
    }
}
