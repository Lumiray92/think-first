package com.thinkfirst.coach.rag;

/** 语料未加载或缺少必要文件时抛出，接口层映射为 503。 */
public class CoachUnavailableException extends RuntimeException {

    public CoachUnavailableException(String message) {
        super(message);
    }
}
