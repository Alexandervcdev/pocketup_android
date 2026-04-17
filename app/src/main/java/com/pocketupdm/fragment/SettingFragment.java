package com.pocketupdm.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.LinearLayout;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private TextView tvName, tvPais, tvIdioma,tvMoneda, tvTema;
    private ImageView ivProfilePhoto;

    // datos por defecto
    private String paisSeleccionado = "España";
    private String idiomaSeleccionado = "Español";
    private String monedaSeleccionada = "EUR";

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
        tvMoneda = view.findViewById(R.id.tv_profile_moneda);
        tvTema = view.findViewById(R.id.tv_profile_tema);
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo);

        // Cargar el tema guardado en la memoria del teléfono
        SharedPreferences prefs = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE);
        String temaGuardado = prefs.getString("temaNombre", "Predeterminado");
        tvTema.setText(temaGuardado);

        obtenerDatosPerfil();
        //cargarDatosDelUsuario();
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> mostrarBottomSheetEditarNombre());
        view.findViewById(R.id.row_edit_pais).setOnClickListener(v -> mostrarBottomSheetSeleccion("País", new String[]{"España", "EE.UU", "Colombia", "Otro"}, tvPais, true));
        view.findViewById(R.id.row_edit_idioma).setOnClickListener(v -> mostrarBottomSheetSeleccion("Idioma", new String[]{"Español", "Inglés"}, tvIdioma, false));
        view.findViewById(R.id.row_edit_tema).setOnClickListener(v -> mostrarBottomSheetTema());
        view.findViewById(R.id.row_edit_moneda).setOnClickListener(v -> mostrarBottomSheetMoneda());

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

                            // 4. Moneda (Lógica de Fallback)
                            if (user.getMoneda() != null && !user.getMoneda().isEmpty()) {
                                tvMoneda.setText(user.getMoneda());
                                monedaSeleccionada = user.getMoneda();
                            } else {
                                tvMoneda.setText("EUR"); // Valor por defecto
                                monedaSeleccionada = "EUR";
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
    /**
     * Bottom Sheet con campo de texto para editar el nombre
     */
    private void mostrarBottomSheetEditarNombre() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_input, null);
        dialog.setContentView(view);

        EditText etNombre = view.findViewById(R.id.et_bottom_sheet_nombre);
        MaterialButton btnGuardar = view.findViewById(R.id.btn_bottom_sheet_guardar);

        // Pre-llenar con el nombre actual
        etNombre.setText(tvName.getText().toString());
        // Mover el cursor al final de la palabra
        etNombre.setSelection(etNombre.getText().length());

        btnGuardar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            if (!nuevoNombre.isEmpty()) {
                tvName.setText(nuevoNombre);
                guardarCambiosEnBackend(); // Esto actualizará Spring Boot
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    /**
     * Bottom Sheet Dinámico para País e Idioma
     */
    private void mostrarBottomSheetSeleccion(String titulo, String[] opciones, TextView textViewDestino, boolean esPais) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_generico, null);
        dialog.setContentView(view);

        // 1. Ponemos el título (Ej: "Selecciona tu País")
        TextView tvTitulo = view.findViewById(R.id.tv_bottom_sheet_titulo);
        tvTitulo.setText("Selecciona tu " + titulo);

        // 2. Buscamos el contenedor vacío
        LinearLayout llOpciones = view.findViewById(R.id.ll_bottom_sheet_opciones);

        // 3. Magia: Un bucle que crea las opciones dinámicamente
        for (String opcion : opciones) {
            TextView tvOpcion = new TextView(requireContext());
            tvOpcion.setText(opcion);
            tvOpcion.setTextSize(18f);
            tvOpcion.setPadding(60, 40, 60, 40); // Espaciado interno para que sea fácil de tocar

            // Le damos el efecto visual de "botón pulsable" (Ripple) nativo de Android
            tvOpcion.setClickable(true);

            // ¿Qué pasa al hacer clic?
            tvOpcion.setOnClickListener(v -> {
                textViewDestino.setText(opcion);
                if (esPais) paisSeleccionado = opcion;
                else idiomaSeleccionado = opcion;

                guardarCambiosEnBackend();
                dialog.dismiss();
            });

            // Añadimos el nuevo botón al contenedor
            llOpciones.addView(tvOpcion);
        }

        dialog.show();
    }

    /**
     * Bottom Sheet Dinámico exclusivo para el Tema
     */
    private void mostrarBottomSheetTema() {
        String[] opciones = {"Claro", "Oscuro", "Predeterminado"};
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_generico, null);
        dialog.setContentView(view);
        TextView tvTitulo = view.findViewById(R.id.tv_bottom_sheet_titulo);
        tvTitulo.setText("Selecciona el tema visual");
        LinearLayout llOpciones = view.findViewById(R.id.ll_bottom_sheet_opciones);

        for (int i = 0; i < opciones.length; i++) {
            final int index = i;
            String opcion = opciones[i];

            TextView tvOpcion = new TextView(requireContext());
            tvOpcion.setText(opcion);
            tvOpcion.setTextSize(18f);
            tvOpcion.setPadding(60, 40, 60, 40);
            tvOpcion.setClickable(true);

            tvOpcion.setOnClickListener(v -> {
                // MAGIA AQUÍ: Guardar en SharedPreferences antes de que la pantalla se reinicie
                android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("PreferenciasApp", android.content.Context.MODE_PRIVATE);
                prefs.edit().putString("temaNombre", opcion).apply();

                tvTema.setText(opcion);

                if (index == 0) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                } else if (index == 1) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                dialog.dismiss();
            });
            llOpciones.addView(tvOpcion);
        }
        dialog.show();
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
        request.setMoneda(monedaSeleccionada);

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
     * Abre un Bottom Sheet moderno para seleccionar la moneda
     */
    private void mostrarBottomSheetMoneda() {
        // 1. Crear el componente BottomSheet
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // 2. Inflar el diseño que creamos en el paso anterior
        View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_bottom_sheet_moneda, null);

        bottomSheetDialog.setContentView(bottomSheetView);

        // 3. Configurar los clics de cada opción dentro del Bottom Sheet
        bottomSheetView.findViewById(R.id.opcion_eur).setOnClickListener(v -> {
            actualizarMoneda("EUR", bottomSheetDialog);
        });

        bottomSheetView.findViewById(R.id.opcion_usd).setOnClickListener(v -> {
            actualizarMoneda("USD", bottomSheetDialog);
        });

        bottomSheetView.findViewById(R.id.opcion_cop).setOnClickListener(v -> {
            actualizarMoneda("COP", bottomSheetDialog);
        });

        // 4. Mostrarlo en pantalla
        bottomSheetDialog.show();
    }
    /**
     * Método auxiliar para evitar repetir código en los clics
     */
    private void actualizarMoneda(String codigoMoneda, BottomSheetDialog dialog) {
        monedaSeleccionada = codigoMoneda;
        tvMoneda.setText(codigoMoneda);
        guardarCambiosEnBackend(); // Manda el cambio a Spring Boot
        dialog.dismiss(); // Cierra el menú hacia abajo
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
        SessionManager sessionManager = new SessionManager(requireContext());
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;
        // 1. Llamada al Backend
        RetrofitClient.getApiService().eliminarCuenta(usuarioId).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    // 2. Si el backend borró con éxito, limpiamos rastro local
                    eliminarRastroYSalir();
                    Toast.makeText(requireContext(), "Tu cuenta ha sido eliminada. Lamentamos verte partir.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar la cuenta", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(requireContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void eliminarRastroYSalir() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // 1. Intentamos borrar al usuario de Firebase Auth
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("DeleteAccount", "Usuario eliminado de Firebase.");
                } else {
                    // Si falla (ej: requiere login reciente), al menos cerramos sesión
                    Log.e("DeleteAccount", "No se pudo borrar de Firebase, haciendo SignOut.");
                    FirebaseAuth.getInstance().signOut();
                }
                // 2. Independientemente del resultado de Firebase, limpiamos lo local
                completarLimpiezaYRedirigir();
            });
        } else {
            // Si por alguna razón no hay usuario en Firebase, limpiamos lo demás
            completarLimpiezaYRedirigir();
        }
    }


    /**
     * Limpia SharedPreferences, Firebase y redirige al Login
     */
    private void completarLimpiezaYRedirigir() {
        // Limpiar SharedPreferences (SessionManager)
        // 1. Limpiar el archivo de sesión (ID, Nombre, etc.)
        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.cerrarSesion();

        //Limpiar el archivo específico del Login
        SharedPreferences loginPrefs = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        loginPrefs.edit().clear().apply();

        // Limpiar el estado de las credenciales de Google
        CredentialManager credentialManager = CredentialManager.create(requireContext());
        credentialManager.clearCredentialStateAsync(new ClearCredentialStateRequest(), null,
                ContextCompat.getMainExecutor(requireContext()), new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        redirigirAlLogin();
                    }
                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        redirigirAlLogin();
                    }
                });
    }

    private void redirigirAlLogin() {
        if (getActivity() != null) {
            NavigationUtil.irALogin(getActivity());
        }
    }
}