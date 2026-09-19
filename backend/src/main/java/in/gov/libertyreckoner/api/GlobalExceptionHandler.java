package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.ErrorView;
import in.gov.libertyreckoner.config.RequestIdFilter;
import in.gov.libertyreckoner.service.BusinessRuleException;
import in.gov.libertyreckoner.service.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorView> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorView> routeNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested endpoint does not exist", request, Map.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ErrorView> businessRule(BusinessRuleException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "BUSINESS_RULE", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorView> concurrentUpdate(ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "The record changed while this action was being processed. Refresh and try again.", request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorView> authentication(AuthenticationException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Email or password is incorrect", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorView> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields", request, fields);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorView> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled request failure [requestId={}]", request.getAttribute(RequestIdFilter.ATTRIBUTE), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed", request, Map.of());
    }

    private ResponseEntity<ErrorView> response(HttpStatus status, String code, String message,
            HttpServletRequest request, Map<String, String> fields) {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        return ResponseEntity.status(status).body(new ErrorView(Instant.now(), status.value(), code,
                message, request.getRequestURI(), requestId, fields));
    }
}
