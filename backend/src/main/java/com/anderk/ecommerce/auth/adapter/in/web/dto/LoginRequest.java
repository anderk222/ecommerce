package com.anderk.ecommerce.auth.adapter.in.web.dto;

import com.anderk.ecommerce.auth.adapter.in.web.validation.ValidEmail;
import com.anderk.ecommerce.auth.application.usecases.LoginUseCase.LoginCommand;
import com.anderk.ecommerce.auth.domain.model.vo.RawPassword;
import com.anderk.ecommerce.domain.model.vo.Email;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @ValidEmail String email, @NotBlank String password) {

    public LoginCommand toLoginUseCaseCommand(){
        return new LoginCommand(new Email(email), new RawPassword(password));
    }
}