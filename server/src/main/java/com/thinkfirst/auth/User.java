package com.thinkfirst.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 登录用户。API Key 只存密文和掩码，明文不落库。 */
@Entity
@Table(name = "tf_user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false, length = 100)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;

    @Column(length = 1024)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String apiKeyCipher;

    @Column(length = 64)
    private String apiKeyMask;

    private Instant apiKeyUpdatedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
