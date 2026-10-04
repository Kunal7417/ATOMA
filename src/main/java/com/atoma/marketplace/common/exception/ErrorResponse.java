package com.atoma.marketplace.common.exception;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ErrorResponse {
    String code;
    String message;
    @Builder.Default
    Map<String, Object> details = Map.of();
}
