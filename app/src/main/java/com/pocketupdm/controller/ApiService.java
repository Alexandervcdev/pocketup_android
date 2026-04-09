package com.pocketupdm.controller;
import com.pocketupdm.dto.UsuarioRegistroRequest;
import com.pocketupdm.model.Usuario;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
public interface ApiService {
    @POST("user/register")
    Call<Usuario> saveUser(@Body UsuarioRegistroRequest request);
    @POST("user/google-auth")
    Call<Usuario> googleAuth(@Body UsuarioRegistroRequest request);
}
