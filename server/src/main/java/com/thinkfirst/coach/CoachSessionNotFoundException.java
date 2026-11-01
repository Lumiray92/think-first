package com.thinkfirst.coach;

public class CoachSessionNotFoundException extends RuntimeException {

    public CoachSessionNotFoundException(String id) {
        super("会话不存在或已过期: " + id);
    }
}
