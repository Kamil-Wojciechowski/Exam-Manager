package com.wojcka.exammanager.models.annocument;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    @JsonProperty
    private String url;

    @JsonProperty
    private String title;

    @JsonProperty
    private String thumbnailUrl;
}
