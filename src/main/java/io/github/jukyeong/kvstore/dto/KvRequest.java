package io.github.jukyeong.kvstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class KvRequest {

    // Key must not be blank and must not exceed 255 characters
    @NotBlank(message = "key is required")
    @Size(max = 255, message = "key must not exceed 255 characters")
    private String key;

    // Value can be any JSON-compatible object
    @NotNull(message = "value is required")
    private Object value;
}
