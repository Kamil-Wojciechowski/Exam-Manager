package com.wojcka.exammanager.schemas.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("data")
    private Object data;

    public static GenericResponse created(Object data) {
        return GenericResponse.builder()
                .code(201)
                .status("CREATED")
                .data(data)
                .build();
    }

    public static GenericResponse ok(Object data) {
        return GenericResponse.builder()
                .code(200)
                .status("OK")
                .data(data)
                .build();
    }
}
