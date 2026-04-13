package com.pocketupdm.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "PocketUpSession";
    private static final String KEY_USER_ID = "usuario_id";
    private static final String KEY_USER_NAME = "usuario_nombre";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // Método para guardar los datos cuando el login es exitoso
    public void crearSesion(Long id, String nombre) {
        editor.putLong(KEY_USER_ID, id);
        editor.putString(KEY_USER_NAME, nombre);
        editor.apply();
    }

    // Métodos para recuperar los datos
    public Long getUsuarioId() {
        // Devuelve -1L si por algún error no encuentra el ID
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public String getUsuarioNombre() {
        return prefs.getString(KEY_USER_NAME, "Usuario");
    }

    // Para cuando implementemos el botón de "Cerrar Sesión"
    public void cerrarSesion() {
        editor.clear();
        editor.apply();
    }
}