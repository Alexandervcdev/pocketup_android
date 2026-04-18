package com.pocketupdm.controller;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.dto.UsuarioLoginRequest;
import com.pocketupdm.dto.UsuarioRegistroRequest;
import com.pocketupdm.dto.UsuarioResponse;
import com.pocketupdm.dto.UsuarioUpdateRequest;
import com.pocketupdm.model.Categoria;
import com.pocketupdm.model.Usuario;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
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
    @DELETE("user/delete/{id}")
    Call<Map<String, Object>> eliminarCuenta(@Path("id") Long id);

    @PUT("{id}/perfil")
    Call<Map<String, Object>> actualizarPerfil(@Path("id") Long id, @Body UsuarioUpdateRequest request);
    // NUEVO MÉTODO PARA BORRADO MÚLTIPLE (BATCH DELETE)
    // Usamos @HTTP en lugar de @DELETE para poder enviar un @Body con la lista de IDs
    @HTTP(method = "DELETE", path = "movimientos/movements/delete", hasBody = true)
    Call<Map<String, Object>> deleteMovements(@Body List<Long> ids);

    // 1. Obtener todas las categorías (Las del sistema + las del usuario)
    @GET("categorias/get/{usuarioId}")
    Call<List<Categoria>> obtenerCategorias(@Path("usuarioId") Long usuarioId);
    // 2. Crear una nueva categoría personalizada
    @POST("categorias/new")
    Call<Categoria> crearCategoria(@Body Categoria categoria);
    // 3. Editar una categoría existente
    @PUT("categorias/update/{id}")
    Call<Categoria> actualizarCategoria(@Path("id") Long id, @Body Categoria categoria);
    // 4. Eliminar una categoría (Recuerda que el backend reasignará los movimientos)
    @DELETE("categorias/delete/{id}")
    Call<Void> eliminarCategoria(@Path("id") Long id);
}
