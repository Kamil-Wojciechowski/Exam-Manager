package com.wojcka.exammanager.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentSubmissions {
    @JsonIgnore
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String state;

    @JsonIgnore
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String userId;

    @JsonProperty
    private Integer assignedGrade;

    @JsonProperty
    private Integer draftGrade;
}
