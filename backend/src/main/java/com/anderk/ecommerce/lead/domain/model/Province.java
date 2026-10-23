package com.anderk.ecommerce.lead.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/**
 * The provinces of Ecuador (mission §6).
 *
 * <p>The list is fixed: it is the list the forms offer.
 */
public enum Province {

    AZUAY,
    BOLIVAR,
    CARCHI,
    COTOPAXI,
    EL_ORO,
    ESMERALDAS,
    GALAPAGOS,
    GUAYAS,
    IMBABURA,
    LOJA,
    LOS_RIOS,
    MANABI,
    MORONA_SANTIAGO,
    NAPO,
    ORELLANA,
    PICHINCHA,
    SANTA_ELENA,
    SUCUMBIOS,
    TUNGURAHUA,
    ZAMORA_CHINCHIPE;

    public static Province string(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")   // quita tildes: Bolívar -> Bolivar
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');          // "Los Ríos" -> LOS_RIOS
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
