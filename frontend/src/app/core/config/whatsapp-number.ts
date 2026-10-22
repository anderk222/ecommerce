import { InjectionToken } from '@angular/core';

/**
 * The number of WhatsApp of the shop, in international format.
 *
 * <p>It is the number of the link of the CTA "Contactar por WhatsApp" (HU-05).
 * The default is a placeholder: the shop sets its own number.
 */
export const WHATSAPP_NUMBER = new InjectionToken<string>('WHATSAPP_NUMBER');
