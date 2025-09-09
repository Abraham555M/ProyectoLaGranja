package com.example.proyectolagranja.ui.Servicios;

import android.text.InputFilter;
import android.text.Spanned;

public class DecimalDigitsInputFilter implements InputFilter {
    private final int decimalDigits;

    public DecimalDigitsInputFilter(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        String input = dest.toString() + source.toString();
        if (input.contains(".")) {
            String[] parts = input.split("\\.");
            if (parts.length > 1 && parts[1].length() > decimalDigits) {
                return "";
            }
        }
        return null;
    }
}
