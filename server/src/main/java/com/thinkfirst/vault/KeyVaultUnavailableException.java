package com.thinkfirst.vault;

/** 保险箱不可用，映射为 503。 */
public class KeyVaultUnavailableException extends RuntimeException {

    public KeyVaultUnavailableException(String message) {
        super(message);
    }
}
