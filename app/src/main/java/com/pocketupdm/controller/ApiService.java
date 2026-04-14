package com.pocketupdm.controller;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.dto.UsuarioLoginRequest;
import com.pocketupdm.dto.UsuarioRegistroRequest;
import com.pocketupdm.model.Usuario;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("user/register")
    Call<Usuario> saveUser(@Body UsuarioRegistroRequest request);
    @POST("user/google-auth")
    Call<Usuario> googleAuth(@Body UsuarioRegistroRequest request);
    @POST("login")
    Call<Usuario> loginUser(@Body UsuarioLoginRequest request);
    @POST("movimientos/nuevo")
    Call<MovimientoResponse> registrarMovimiento(@Body MovimientoRequest request);
    @GET("movimientos/usuario/{id}")
    Call<List<MovimientoResponse>> obtenerMovimientos(@Path("id") Long usuarioId);
}
