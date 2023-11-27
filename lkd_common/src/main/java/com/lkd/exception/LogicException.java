package com.lkd.exception;

/**
 * 逻辑异常
 */
public class LogicException extends RuntimeException{
    public LogicException(String errorMsg){
        super(errorMsg);
    }

    public LogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
