package com.darum.shareddomain.lib;

import com.darum.shareddomain.dto.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import javax.naming.AuthenticationException;
import java.util.HashMap;
import java.util.Map;

@Component
public class Utils {

    public Map<Class<? extends Exception>, ResponseEntity<ResponseBody<Object>>> getExceptionMappings() {
        Map<Class<? extends Exception>, ResponseEntity<ResponseBody<Object>>> exceptionMappings = new HashMap<>();

        exceptionMappings.put(BadCredentialsException.class, ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createResponseBody("error", "Invalid username or password")));

        exceptionMappings.put(AuthenticationException.class, ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createResponseBody("error", "Unable to authenticate user")));

        exceptionMappings.put(InternalAuthenticationServiceException.class, ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponseBody("error", "Internal authentication service error occurred.")));

        exceptionMappings.put(AuthorizationDeniedException.class, ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createResponseBody("unauthorized", "You do not have permission to access this resource.")));

        return exceptionMappings;
    }

    public ResponseBody<Object> createResponseBody(String status, String message) {
        return new ResponseBody<>(
                status,
                message,
                null
        );
    }

    public String getJWTToken(){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getTokenValue();
    }

    public String getUserEmail(){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getClaim("sub");
    }
}
