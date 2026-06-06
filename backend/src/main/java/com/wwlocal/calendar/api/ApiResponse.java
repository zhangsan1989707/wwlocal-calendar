package com.wwlocal.calendar.api;

public record ApiResponse<T>(boolean success, T data, String message) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, "OK");
  }

  public static ApiResponse<Void> ok() {
    return new ApiResponse<>(true, null, "OK");
  }

  public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>(false, null, message);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(false, null, message);
  }
}
