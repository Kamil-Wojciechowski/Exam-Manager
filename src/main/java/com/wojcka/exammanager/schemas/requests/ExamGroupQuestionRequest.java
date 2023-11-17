package com.wojcka.exammanager.schemas.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wojcka.exammanager.models.CorrectType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ExamGroupQuestionRequest {
    @JsonProperty
    private CorrectType correctType;
}
