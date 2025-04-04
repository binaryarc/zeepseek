package com.zeepseek.backend.domain.recommend.exception;

public class DataRetrievalException extends RuntimeException {
    public DataRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
