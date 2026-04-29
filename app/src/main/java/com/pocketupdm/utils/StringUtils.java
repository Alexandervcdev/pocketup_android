package com.pocketupdm.utils;

public class StringUtils {
    public static String LetraMayuscula(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        // La primera en mayúscula, el resto como venga (o puedes forzar minúsculas con .toLowerCase())
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
