package com.thinkfirst.vault;

/** Key 格式或校验失败，映射为 400。 */
public class ApiKeyException extends RuntimeException {

    public ApiKeyException(String message) {
        super(message);
    }
}
