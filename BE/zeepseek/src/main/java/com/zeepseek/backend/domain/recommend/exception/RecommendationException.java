package com.zeepseek.backend.domain.recommend.exception;

public class RecommendationException extends RuntimeException {

    private final String errorCode;

    public RecommendationException(String message) {
        super(message);
        this.errorCode = "RECOMMENDATION_ERROR";
    }

    public RecommendationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RECOMMENDATION_ERROR";
    }

    public RecommendationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
