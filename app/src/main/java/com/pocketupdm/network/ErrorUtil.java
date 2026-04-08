package com.pocketupdm.network;

import com.google.gson.Gson;
import com.pocketupdm.model.ApiError;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class ErrorUtil {
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
