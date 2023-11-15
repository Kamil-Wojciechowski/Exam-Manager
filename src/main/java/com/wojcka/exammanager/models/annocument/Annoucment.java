package com.wojcka.exammanager.models.annocument;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Annoucment {
    @JsonProperty(required = false)
    private String id;

    @JsonProperty("text")
    private String text;

    @JsonProperty
    private List<Material> materials;

    @JsonProperty("state")
    @Enumerated(EnumType.STRING)
    private State state;
}
