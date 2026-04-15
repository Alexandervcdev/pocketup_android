package com.pocketupdm.controller;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.dto.UsuarioLoginRequest;
import com.pocketupdm.dto.UsuarioRegistroRequest;
import com.pocketupdm.dto.UsuarioResponse;
import com.pocketupdm.dto.UsuarioUpdateRequest;
import com.pocketupdm.model.Usuario;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @GET("usuario/{id}")
    Call<UsuarioResponse> obtenerPerfil(@Path("id") Long id);
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

    @PUT("{id}/perfil")
    Call<Map<String, Object>> actualizarPerfil(@Path("id") Long id, @Body UsuarioUpdateRequest request);
}
