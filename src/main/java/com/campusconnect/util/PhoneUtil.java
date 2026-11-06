package com.campusconnect.util;

import com.campusconnect.exception.ApiException;

public final class PhoneUtil {
    private PhoneUtil() {}

    /** Accepts "+91 98765-43210" and returns E.164 "+919876543210". */
    public static String normalize(String raw) {
        if (raw == null) throw ApiException.bad("WhatsApp number is required");
        String s = raw.replaceAll("[^+0-9]", "");
        if (!s.startsWith("+")) throw ApiException.bad("Include the country code in your WhatsApp number, e.g. +91...");
        if (!s.matches("^\\+[1-9]\\d{7,14}$")) throw ApiException.bad("That WhatsApp number doesn't look valid");
        return s;
    }

    public static String digits(String e164) { return e164.replace("+", ""); }
}
