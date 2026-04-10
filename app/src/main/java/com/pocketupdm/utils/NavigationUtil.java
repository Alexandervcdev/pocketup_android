package com.pocketupdm.utils;

import android.app.Activity;
import android.content.Intent;

import com.pocketupdm.LoginActivity;
import com.pocketupdm.MainActivity;

/**
 * Clase utilitaria para centralizar la navegación global de la aplicación.
 * Evita la duplicación de código en transiciones comunes
 * como cierres de sesión o redirecciones de seguridad.
 */
public class NavigationUtil {

    /**
     * Redirige al usuario a la pantalla de Login y destruye completamente
     * el historial de pantallas anteriores (pila de navegación).
     * Ideal para usar después de un registro, cierre de sesión o error crítico.
     *
     * @param currentActivity La actividad actual desde donde se invoca la acción.
     */
    public static void irALogin(Activity currentActivity) {
        Intent intent = new Intent(currentActivity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        currentActivity.startActivity(intent);
        currentActivity.finish();
    }

    /**
     * Redirige al usuario a la pantalla principal (MainActivity) y destruye el historial.
     * Se utiliza tras un inicio de sesión o registro exitoso.
     * @param currentActivity La actividad actual.
     */
    public static void irAMainActivity(Activity currentActivity) {
        Intent intent = new Intent(currentActivity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        currentActivity.startActivity(intent);
        currentActivity.finish();
    }
}