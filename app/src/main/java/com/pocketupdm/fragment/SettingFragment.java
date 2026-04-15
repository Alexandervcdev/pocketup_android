package com.pocketupdm.fragment;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pocketupdm.R;
import com.pocketupdm.dto.UsuarioResponse;
import com.pocketupdm.dto.UsuarioUpdateRequest;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.NavigationUtil;
import com.pocketupdm.utils.SessionManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class SettingFragment extends Fragment {
    // Variables de las vistas
    private TextView tvName, tvPais, tvIdioma, tvTema;
    private ImageView ivProfilePhoto;

    // Aquí guardaremos los datos temporalmente antes de mandarlos a Spring Boot
    private String paisSeleccionado = "España";
    private String idiomaSeleccionado = "Español";

    public SettingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvPais = view.findViewById(R.id.tv_profile_pais);
        tvIdioma = view.findViewById(R.id.tv_profile_idioma);
        tvTema = view.findViewById(R.id.tv_profile_tema);
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo);

        obtenerDatosPerfil();
        //cargarDatosDelUsuario();
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> mostrarDialogoEditarNombre());
        view.findViewById(R.id.row_edit_pais).setOnClickListener(v -> mostrarDialogoSeleccion("País", new String[]{"España", "México", "Colombia", "Argentina", "Chile", "Otro"}, tvPais, true));
        view.findViewById(R.id.row_edit_idioma).setOnClickListener(v -> mostrarDialogoSeleccion("Idioma", new String[]{"Español", "Inglés"}, tvIdioma, false));
        view.findViewById(R.id.row_edit_tema).setOnClickListener(v -> mostrarDialogoTema());

        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        MaterialButton btnDeleteAccount = view.findViewById(R.id.btn_delete_account);

        //listeners de los botones
            //cerrar sesion
        btnLogout.setOnClickListener(v -> mostrarDialogoCustom(
                "Cerrar Sesión",
                "¿Deseas cerrar sesión de tu cuenta?",
                "salir",
                R.color.red,
                R.color.turquesa_oscuro,
                this::ejecutarCierreSesion // Pasamos la función que se ejecutará si dice que sí
        ));
            //eliminar cuenta
        btnDeleteAccount.setOnClickListener(v -> mostrarDialogoCustom(
                "Eliminar Cuenta",
                "Esta acción es irreversible. Se borrarán todos tus movimientos, nivel y progreso de forma permanente. ¿Deseas continuar?",
                "Eliminar",
                R.color.turquesa_oscuro,
                R.color.red,
                this::ejecutarEliminarCuenta
        ));
    }

    private void obtenerDatosPerfil() {
        SessionManager sessionManager = new SessionManager(requireContext());
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        // Llamamos al backend para obtener la foto, país e idioma reales
        com.pocketupdm.network.RetrofitClient.getApiService().obtenerPerfil(usuarioId)
                .enqueue(new retrofit2.Callback<com.pocketupdm.dto.UsuarioResponse>() {
                    @Override
                    public void onResponse(Call<com.pocketupdm.dto.UsuarioResponse> call, Response<UsuarioResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UsuarioResponse user = response.body();

                            // 1. Nombre (Prioridad: API -> Session)
                            tvName.setText(user.getNombre() != null ? user.getNombre() : sessionManager.getUsuarioNombre());

                            // 2. País (Lógica de Fallback)
                            if (user.getPais() != null && !user.getPais().isEmpty()) {
                                tvPais.setText(user.getPais());
                                paisSeleccionado = user.getPais();
                            } else {
                                tvPais.setText("España"); // Valor por defecto
                                paisSeleccionado = "España";
                            }

                            // 3. Idioma (Lógica de Fallback)
                            if (user.getIdioma() != null && !user.getIdioma().isEmpty()) {
                                tvIdioma.setText(user.getIdioma());
                                idiomaSeleccionado = user.getIdioma();
                            } else {
                                tvIdioma.setText("Español"); // Valor por defecto
                                idiomaSeleccionado = "Español";
                            }

                            // 4. Cargar Foto (Firebase o Google)
                            cargarFotoPerfil();
                        }
                    }

                    @Override
                    public void onFailure(Call<UsuarioResponse> call, Throwable t) {
                        // Si falla el internet, ponemos los valores de la sesión local
                        tvName.setText(sessionManager.getUsuarioNombre());
                        tvPais.setText("España");
                        tvIdioma.setText("Español");
                    }
                });
    }

    private void cargarFotoPerfil() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_camera) // Si tarda en cargar, muestra la cámara
                    .into(ivProfilePhoto);
        }
    }


    /**
     * Abre un pop-up con un campo de texto para escribir el nuevo nombre
     */
    private void mostrarDialogoEditarNombre() {
        EditText input = new EditText(requireContext());
        input.setText(tvName.getText().toString()); // Ponemos el nombre actual por defecto
        new AlertDialog.Builder(requireContext())
                .setTitle("Editar Nombre")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoNombre = input.getText().toString().trim();
                    if (!nuevoNombre.isEmpty()) {
                        tvName.setText(nuevoNombre);
                        guardarCambiosEnBackend(); // Llamada a la API
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Abre un pop-up con una lista de opciones (ej: Países o Idiomas)
     */
    private void mostrarDialogoSeleccion(String titulo, String[] opciones, TextView textViewDestino, boolean esPais) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona tu " + titulo)
                .setItems(opciones, (dialog, which) -> {
                    String seleccion = opciones[which];
                    textViewDestino.setText(seleccion); // Actualiza la pantalla
                    if (esPais) paisSeleccionado = seleccion;
                    else idiomaSeleccionado = seleccion;
                    guardarCambiosEnBackend(); // Llamada a la API
                })
                .show();
    }

    /**
     * Abre un pop-up para cambiar el tema visual de la aplicación
     */
    private void mostrarDialogoTema() {
        String[] opciones = {"Claro", "Oscuro", "Predeterminado"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona el tema")
                .setItems(opciones, (dialog, which) -> {
                    tvTema.setText(opciones[which]); // Actualiza el texto en pantalla
                    // Aplicar el cambio de tema en Android inmediatamente
                    if (which == 0) {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (which == 1) {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                })
                .show();
    }

    /**
     * Este método lo dejaremos preparado para la Fase 2 (Conexión a Spring Boot)
     */
    private void guardarCambiosEnBackend() {
        SessionManager sessionManager = new SessionManager(requireContext());
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        String nuevoNombre = tvName.getText().toString();

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre(nuevoNombre);
        request.setPais(paisSeleccionado);
        request.setIdioma(idiomaSeleccionado);

        RetrofitClient.getApiService().actualizarPerfil(usuarioId, request)
                .enqueue(new retrofit2.Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                            sessionManager.crearSesion(usuarioId, nuevoNombre);
                        } else {
                            Toast.makeText(getContext(), "Error al guardar los cambios", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(getContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Metodo que sirve para mostrar diálogos personalizados.
     * @param accionAceptar Un bloque de código (Runnable) que se ejecutará si el usuario presiona el botón derecho.
     */
    private void mostrarDialogoCustom(String titulo, String mensaje, String textoBoton, int colorBotonIzquierdo, int colorBotonDerecho,Runnable accionAceptar) {
        // Creamos el diálogo
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.layout_dialog_confirm);

        // Hacemos que el fondo del diálogo sea transparente para que se vean las esquinas redondeadas de nuestra tarjeta XML
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Vinculamos las vistas del XML
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnAction = dialog.findViewById(R.id.btn_dialog_action);

        // Aplicamos los textos
        tvTitle.setText(titulo);
        tvMessage.setText(mensaje);
        btnAction.setText(textoBoton);

        btnCancel.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorBotonIzquierdo)));
        btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorBotonDerecho)));

        // Listeners de los botones
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            accionAceptar.run(); // Ejecutamos la función que nos pasaron por parámetro
        });
        dialog.show();
    }

    private void ejecutarCierreSesion() {
        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.cerrarSesion();
        FirebaseAuth.getInstance().signOut();

        CredentialManager credentialManager = CredentialManager.create(requireContext());
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                null,
                ContextCompat.getMainExecutor(requireContext()),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        redirigirAlLogin();
                    }
                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        redirigirAlLogin();
                    }
                }
        );
    }

    private void ejecutarEliminarCuenta() {
        // Aquí conectaremos la llamada a Retrofit para borrar el usuario de Spring Boot más adelante
        Toast.makeText(requireContext(), "Procesando eliminación de cuenta...", Toast.LENGTH_SHORT).show();
    }

    private void redirigirAlLogin() {
        if (getActivity() != null) {
            NavigationUtil.irALogin(getActivity());
        }
    }
}