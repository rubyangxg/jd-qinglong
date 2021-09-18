package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Created by yangxg on 2021/9/14
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QLToken {
    private String token;
    private String tokenType;
    private long expiration;

    public QLToken(String token) {
        this.token = token;
    }

    public boolean isExpired() {
        return expiration > System.currentTimeMillis() / 1000;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QLToken qlToken = (QLToken) o;
        return Objects.equals(token, qlToken.token);
    }

    @Override
    public String toString() {
        return "QLToken{" +
                "token='" + token + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiration=" + expiration +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
