package com.wojcka.exammanager.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private String id;

    @JsonProperty
    private String state;

    @JsonIgnore
    private String userId;

    @JsonProperty
    private Integer assignedGrade;
}
