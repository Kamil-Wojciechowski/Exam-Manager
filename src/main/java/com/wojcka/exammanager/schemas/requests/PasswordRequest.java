package com.wojcka.exammanager.schemas.requests;

import com.wojcka.exammanager.components.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordRequest {

    @ValidPassword
    private String password;

    @NotNull
    private String confirmedPassword;

    @AssertTrue(message = "password_equal")
    public boolean isPasswordEqual() {
        return password.equals(confirmedPassword);
    }

}
