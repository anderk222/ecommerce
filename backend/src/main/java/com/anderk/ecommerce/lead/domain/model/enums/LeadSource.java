package com.anderk.ecommerce.lead.domain.model.enums;

/**
 * Where a lead came from (HU-11).
 *
 * <p>A lead may have several origins: the same person may have asked for
 * information and then bought.
 */
public enum LeadSource {

    /** The visitor completed a purchase. */
    CHECKOUT,

    /** The visitor asked to be notified when the stock comes back. */
    STOCK_NOTIFY,

    /** The visitor asked for more information. */
    INFO_REQUEST,

    /** The visitor wrote by WhatsApp. */
    WHATSAPP,

    /** The administrator created the lead by hand. */
    MANUAL
}
