package com.anderk.ecommerce.lead.adapter.in.web.dto;

import java.util.List;

import com.anderk.ecommerce.lead.domain.model.Province;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
     * The body of the request of information.
     *
     * @param name the name of the visitor
     * @param email the email of the visitor
     * @param phone the telephone of the visitor
     * @param province the province of Ecuador
     * @param consent whether the visitor agreed to be contacted
     * @param anonymousId the browser of the visitor
     * @param itemIds the items the visitor was interested in
     * @param message an optional message
     */
    public record InfoRequestRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            String phone,
            Province province,
            boolean consent,
            String anonymousId,
            List<Long> itemIds,
            String message) {
    }
