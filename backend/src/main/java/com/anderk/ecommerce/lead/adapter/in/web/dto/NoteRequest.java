package com.anderk.ecommerce.lead.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The body of a note.
 *
 * @param text the text of the note
 */
public record NoteRequest(@NotBlank String text) {
}
