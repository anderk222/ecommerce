package com.anderk.ecommerce.lead.application.command;

import java.math.BigDecimal;

import com.anderk.ecommerce.lead.domain.model.Province;

/**
 * The creation of a lead by hand (HU-21).
 *
 * @param name the name
 * @param email the email
 * @param phone the telephone
 * @param province the province of Ecuador
 * @param saleAmount the amount of the sale
 * @param consent whether the lead agreed to be contacted
 */
public record CreateLeadCommand(
        String name,
        String email,
        String phone,
        Province province,
        BigDecimal saleAmount,
        boolean consent) {
}
