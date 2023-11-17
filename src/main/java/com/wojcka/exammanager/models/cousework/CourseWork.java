package com.wojcka.exammanager.models.cousework;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wojcka.exammanager.models.annocument.State;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseWork {
    @JsonProperty
    private String id;

    @JsonProperty
    private String title;

    @JsonProperty
    private String description;

    @JsonProperty
    private Integer maxPoints;

    @JsonProperty
    @Enumerated(EnumType.STRING)
    private AssigneeMode assigneeMode;

    @JsonProperty
    @Enumerated(EnumType.STRING)
    private WorkType workType;

    @JsonProperty
    @Enumerated(EnumType.STRING)
    private State state;
}
