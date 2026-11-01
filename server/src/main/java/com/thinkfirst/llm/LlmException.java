package com.thinkfirst.llm;

/** 模型调用失败。message 会展示给用户，不能包含 Key 或响应正文。 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }
}
