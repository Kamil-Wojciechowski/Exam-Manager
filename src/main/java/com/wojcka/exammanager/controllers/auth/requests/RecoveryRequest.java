package com.wojcka.exammanager.controllers.auth.requests;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecoveryRequest {
    private String password;

    private String confirmedPassword;

    @AssertTrue
    public boolean isPasswordEqual() {
        return password.equals(confirmedPassword);
    }

}
