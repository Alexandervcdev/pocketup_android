package com.pocketupdm.model;

/**
 * Modelo de datos (POJO) utilizado para mapear las respuestas de error estructuradas
 * provenientes del Backend (Spring Boot).
 * <p>
 * Forma parte fundamental del "Sistema de Blindaje", permitiendo que la aplicación
 * lea respuestas HTTP no exitosas de forma segura sin generar excepciones de parseo (ej. MalformedJsonException).
 * Coincide exactamente con la estructura JSON generada por el @RestControllerAdvice de la API.
 */
public class ApiError {
    private String error;
    private int status;
    /**
     * Constructor principal para instanciar un objeto de error de la API.
     *
     * @param error  El mensaje descriptivo del error generado por la lógica de negocio del servidor
     * (ej. "El correo ya está registrado" o "Credenciales incorrectas").
     * @param status El código de estado HTTP devuelto por el servidor (ej. 400 para Bad Request, 404 para Not Found).
     */
    public ApiError(String error, int status) {
        this.error = error;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
