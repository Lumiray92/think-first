package com.thinkfirst.auth;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("邮箱或密码不正确");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
