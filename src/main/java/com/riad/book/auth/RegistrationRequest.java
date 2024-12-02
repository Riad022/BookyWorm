package com.riad.book.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegistrationRequest {

    @NotEmpty(message="firstname is mandatory")
    @NotBlank(message="firstname is mandatory")
    private String firstname;
    @NotEmpty(message="lastname is mandatory")
    @NotBlank(message="lastname is mandatory")
    private String lastname;
    @Email
    @NotEmpty(message="Email is mandatory")
    @NotBlank(message="Email is mandatory")
    private String email;
    @NotEmpty(message="Password is mandatory")
    @NotBlank(message="Password is mandatory")
    @Size(min=8 ,message = "Password should be 8 characters long minimum")
    private String password;
}
