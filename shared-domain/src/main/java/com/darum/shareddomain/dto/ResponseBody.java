package com.darum.shareddomain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class ResponseBody<T> implements Serializable {
    private String status;
    private String message;
    private T data;
    private Instant timestamp = Instant.now();

    public ResponseBody(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseBody<T> success(String message, T data) {
        return new ResponseBody<>("success", message, data);
    }

    public static <T> ResponseBody<T> error(String message) {
        return new ResponseBody<>("error", message, null);
    }
}
