package com.wojcka.exammanager.schemas.requests;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordRequest {
    private String password;

    private String confirmedPassword;

    @AssertTrue
    public boolean isPasswordEqual() {
        return password.equals(confirmedPassword);
    }

}
