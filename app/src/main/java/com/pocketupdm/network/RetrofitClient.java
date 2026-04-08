package com.pocketupdm.network;
import com.pocketupdm.controller.ApiService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class RetrofitClient {
    /**
     * Clase encargada de centralizar la configuración de Retrofit.
     * Utiliza el patrón Singleton para asegurar que solo exista una instancia del cliente HTTP,
     * optimizando el uso de recursos y memoria de la aplicación.
     */
    private static final String BASE_URL = "https://alexandervcdev-pocketup-api.hf.space/";
    private static Retrofit retrofit = null;

    /**
     * Configura y devuelve el servicio de la API.
     * Si es la primera vez que se llama, construye la instancia de Retrofit con
     * la URL base y un convertidor de JSON (Gson).
     * * @return Una implementación de la interfaz ApiService para realizar peticiones HTTP.
     */
    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
