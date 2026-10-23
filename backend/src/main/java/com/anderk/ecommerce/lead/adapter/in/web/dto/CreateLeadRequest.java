package com.anderk.ecommerce.lead.adapter.in.web.dto;

import java.math.BigDecimal;

import com.anderk.ecommerce.auth.adapter.in.web.validation.ValidEmail;
import com.anderk.ecommerce.lead.domain.model.Province;

import jakarta.validation.constraints.NotBlank;

/**
     * The body of the creation of a lead.
     *
     * @param name the name
     * @param email the email
     * @param phone the telephone
     * @param province the province of Ecuador
     * @param saleAmount the amount of the sale
     * @param consent whether the lead agreed to be contacted
     */
    public record CreateLeadRequest(
            @NotBlank String name,
            @NotBlank @ValidEmail String email,
            String phone,
            Province province,
            BigDecimal saleAmount,
            boolean consent) {
    }