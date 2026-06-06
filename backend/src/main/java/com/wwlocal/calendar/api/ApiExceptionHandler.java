package com.wwlocal.calendar.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> badRequest(IllegalArgumentException ex) {
    return new ApiResponse<>(false, null, ex.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> securityError(SecurityException ex) {
    return new ApiResponse<>(false, null, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> error(Exception ex) {
    System.err.println("[ERROR] Unhandled exception: " + ex.getClass().getName() + ": " + ex.getMessage());
    ex.printStackTrace(System.err);
    return new ApiResponse<>(false, null, "服务处理失败");
  }
}
