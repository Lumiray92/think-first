package com.thinkfirst.auth;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("该邮箱已注册");
    }
}
