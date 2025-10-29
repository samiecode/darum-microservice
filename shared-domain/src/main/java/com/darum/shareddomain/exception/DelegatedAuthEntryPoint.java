package com.darum.shareddomain.exception;

import com.darum.shareddomain.dto.ResponseBody;
import com.darum.shareddomain.lib.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component("delegatedAuthEntryPoint")
public class DelegatedAuthEntryPoint implements AuthenticationEntryPoint { ;
    private final ObjectMapper objectMapper;
    private final Utils utils;

    public DelegatedAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.utils = new Utils();
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ResponseEntity<ResponseBody<Object>> responseEntity = utils.getExceptionMappings().entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(authException))
                .map(Map.Entry::getValue)
                .findFirst().orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(utils.createResponseBody("unauthorized", authException.getMessage())));


        response.setStatus(responseEntity.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), responseEntity.getBody());

        log.error("{}", authException.getMessage(), authException);
    }
}
