package com.pocketupdm.network;

import com.google.gson.Gson;
import com.pocketupdm.model.ApiError;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Clase utilitaria centralizada para el manejo y parseo de errores de red.
 * Actúa como el pilar del "Sistema de Blindaje", interceptando respuestas HTTP
 * no exitosas (4xx, 5xx) y traduciendo el cuerpo del error (JSON) a mensajes
 * seguros y legibles para la interfaz de usuario.
 */
public class ErrorUtil {
    /**
     * Extrae y procesa el mensaje de error del cuerpo de una respuesta HTTP fallida.
     * Utiliza GSON para deserializar el JSON devuelto por el servidor (ej. RestControllerAdvice)
     * y mapearlo al modelo local {@link ApiError}.
     * <p>
     * Incluye un mecanismo de seguridad (try-catch y validación de nulos) para garantizar
     * que la aplicación no sufra un cierre inesperado (crash) si el servidor devuelve
     * un formato no válido (ej. HTML en lugar de JSON) o si la conexión se interrumpe.
     *
     * @param response La respuesta HTTP fallida capturada por Retrofit (donde isSuccessful() es false).
     * @return El mensaje de error exacto extraído del backend (ej. "Credenciales incorrectas")
     * o un mensaje genérico de seguridad si el parseo falla.
     */
    public static String parseError(Response<?> response) {
        try {
            ResponseBody body = response.errorBody();
            if (body == null) return "Error desconocido";

            Gson gson = new Gson();
            // Esto mapea el JSON {"error": "...", "status": 400} a la clase ApiError
            ApiError error = gson.fromJson(body.charStream(), ApiError.class);
            return error.getError();
        } catch (Exception e) {
            return "Error al procesar respuesta del servidor";
        }
    }


}
