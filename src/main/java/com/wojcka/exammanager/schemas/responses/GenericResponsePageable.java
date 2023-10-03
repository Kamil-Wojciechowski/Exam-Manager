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
public class GenericResponsePageable {
    @JsonProperty("status")
    private String status;

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("data")
    private Object data;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("hasNext")
    private boolean hasNext;

    @JsonProperty("pages")
    private Integer pages;

    @JsonProperty("total")
    private Long total;
}
