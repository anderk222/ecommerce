package com.anderk.ecommerce.lead.adapter.in.web.dto;

import java.math.BigDecimal;

import com.anderk.ecommerce.lead.domain.model.Province;

/**
     * The body of the edition of a lead.
     *
     * @param name the name
     * @param phone the telephone
     * @param province the province of Ecuador
     * @param saleAmount the amount of the sale
     * @param consent whether the lead agreed to be contacted
     */
    public record UpdateLeadRequest(
            String name,
            String phone,
            Province province,
            BigDecimal saleAmount,
            boolean consent) {
    }