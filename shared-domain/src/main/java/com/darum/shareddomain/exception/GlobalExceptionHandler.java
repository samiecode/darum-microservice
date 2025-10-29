package com.darum.shareddomain.exception;

import com.darum.shareddomain.dto.ResponseBody;
import com.darum.shareddomain.lib.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Utils utils;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseBody<Object>> handleGeneralException(Exception ex) {
        return utils.getExceptionMappings().entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(ex))
                .map(Map.Entry::getValue)
                .findFirst().orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(utils.createResponseBody("error", ex.getMessage())));
    }
}
