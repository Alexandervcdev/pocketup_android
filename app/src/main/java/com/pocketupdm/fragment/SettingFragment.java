package com.pocketupdm.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
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
import com.pocketupdm.utils.DialogUtils;
import com.pocketupdm.utils.NavigationUtil;
import com.pocketupdm.utils.SessionManager;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Callback;

public class SettingFragment extends Fragment {
    // Variables de las vistas
    private TextView tvName, tvPais, tvIdioma, tvMoneda, tvTema;
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

        //datos cargados del sharedpreferences
        cargarDatosDesdeCache();
        obtenerDatosPerfil();

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> mostrarBottomSheetEditarNombre());
        view.findViewById(R.id.row_edit_pais).setOnClickListener(v -> mostrarBottomSheetSeleccion("País", new String[]{"España", "EE.UU", "Colombia", "Otro"}, tvPais, true));
        view.findViewById(R.id.row_edit_idioma).setOnClickListener(v -> mostrarBottomSheetSeleccion("Idioma", new String[]{"Español", "Inglés"}, tvIdioma, false));
        view.findViewById(R.id.row_edit_tema).setOnClickListener(v -> mostrarBottomSheetTema());
        view.findViewById(R.id.row_edit_moneda).setOnClickListener(v -> mostrarBottomSheetMoneda());

        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        MaterialButton btnDeleteAccount = view.findViewById(R.id.btn_delete_account);

        //listeners de los botones
        //cerrar sesion
        btnLogout.setOnClickListener(v -> DialogUtils.mostrarDialogoConfirmacion(
                requireContext(),
                "Cerrar Sesión",
                "¿Deseas cerrar sesión de tu cuenta?",
                "Salir",
                R.color.red,
                R.color.turquesa_oscuro,
                this::ejecutarCierreSesion
        ));
        //eliminar cuenta
        btnDeleteAccount.setOnClickListener(v -> DialogUtils.mostrarDialogoConfirmacion(
                requireContext(),
                "Eliminar Cuenta",
                "Esta acción es irreversible. Se borrarán todos tus movimientos de forma permanente. ¿Deseas continuar?",
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

        RetrofitClient.getApiService().obtenerPerfil(usuarioId)
                .enqueue(new Callback<UsuarioResponse>() {
                    @Override
                    public void onResponse(Call<UsuarioResponse> call, Response<UsuarioResponse> response) {
                        if (!isAdded() || getContext() == null) return;

                        if (response.isSuccessful() && response.body() != null) {
                            UsuarioResponse user = response.body();

                            String nombreBackend = user.getNombre() != null ? user.getNombre() : sessionManager.getUsuarioNombre();
                            String paisBackend = (user.getPais() != null && !user.getPais().isEmpty()) ? user.getPais() : "España";
                            String idiomaBackend = (user.getIdioma() != null && !user.getIdioma().isEmpty()) ? user.getIdioma() : "Español";
                            String monedaBackend = (user.getMoneda() != null && !user.getMoneda().isEmpty()) ? user.getMoneda() : "EUR";

                            // Actualizar UI
                            tvName.setText(nombreBackend);
                            tvPais.setText(paisBackend);
                            tvIdioma.setText(idiomaBackend);
                            tvMoneda.setText(monedaBackend);
                            cargarFotoPerfil(); // Carga simple

                            // Actualizar variables y caché
                            paisSeleccionado = paisBackend;
                            idiomaSeleccionado = idiomaBackend;
                            monedaSeleccionada = monedaBackend;

                            guardarAjustesEnCache(nombreBackend, paisBackend, idiomaBackend, monedaBackend);
                        }
                    }

                    @Override
                    public void onFailure(Call<UsuarioResponse> call, Throwable t) {
                        Log.d("Ajustes", "Sin conexión, usando datos de caché local");
                    }
                });
    }

    // --- CARGA DE FOTO SIMPLIFICADA (Cero errores) ---
    private void cargarFotoPerfil() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Si el usuario viene de Google y tiene foto, la cargamos. Si no, icono por defecto.
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_default) // Pon el nombre de tu icono por defecto real aquí
                    .into(ivProfilePhoto);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_user_default) // Pon el nombre de tu icono por defecto real aquí
                    .circleCrop()
                    .into(ivProfilePhoto);
        }
    }

    /**
     * Bottom Sheet con campo de texto para editar el nombre
     */
    private void mostrarBottomSheetEditarNombre() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_input, null);
        dialog.setContentView(view);

        EditText etNombre = view.findViewById(R.id.et_bottom_sheet_nombre);
        MaterialButton btnGuardar = view.findViewById(R.id.btn_bottom_sheet_guardar);

        etNombre.setText(tvName.getText().toString());
        etNombre.setSelection(etNombre.getText().length());

        btnGuardar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            if (!nuevoNombre.isEmpty()) {
                tvName.setText(nuevoNombre);
                guardarCambiosEnBackend();
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

        TextView tvTitulo = view.findViewById(R.id.tv_bottom_sheet_titulo);
        tvTitulo.setText("Selecciona tu " + titulo);

        LinearLayout llOpciones = view.findViewById(R.id.ll_bottom_sheet_opciones);

        for (String opcion : opciones) {
            TextView tvOpcion = new TextView(requireContext());
            tvOpcion.setText(opcion);
            tvOpcion.setTextSize(18f);
            tvOpcion.setPadding(60, 40, 60, 40);
            tvOpcion.setClickable(true);

            tvOpcion.setOnClickListener(v -> {
                textViewDestino.setText(opcion);

                if (esPais) {
                    paisSeleccionado = opcion;
                } else {
                    idiomaSeleccionado = opcion;
                    aplicarTraduccion(opcion);
                }

                guardarCambiosEnBackend();
                dialog.dismiss();
            });

            llOpciones.addView(tvOpcion);
        }
        dialog.show();
    }

    private void aplicarTraduccion(String idioma) {
        String languageCode = (idioma.equalsIgnoreCase("Inglés") || idioma.equalsIgnoreCase("English")) ? "en" : "es";
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
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
                dialog.dismiss();
                if (!isAdded() || getActivity() == null) return;

                SharedPreferences prefs = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE);
                prefs.edit().putString("temaNombre", opcion).apply();
                tvTema.setText(opcion);

                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (index == 0) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (index == 1) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }
                }, 400);
            });
            llOpciones.addView(tvOpcion);
        }
        dialog.show();
    }

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
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                            sessionManager.crearSesion(usuarioId, nuevoNombre);
                            guardarAjustesEnCache(nuevoNombre, paisSeleccionado, idiomaSeleccionado, monedaSeleccionada);
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

    private void mostrarBottomSheetMoneda() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_moneda, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        bottomSheetView.findViewById(R.id.opcion_eur).setOnClickListener(v -> actualizarMoneda("EUR", bottomSheetDialog));
        bottomSheetView.findViewById(R.id.opcion_usd).setOnClickListener(v -> actualizarMoneda("USD", bottomSheetDialog));
        bottomSheetView.findViewById(R.id.opcion_cop).setOnClickListener(v -> actualizarMoneda("COP", bottomSheetDialog));

        bottomSheetDialog.show();
    }

    private void actualizarMoneda(String codigoMoneda, BottomSheetDialog dialog) {
        monedaSeleccionada = codigoMoneda;
        tvMoneda.setText(codigoMoneda);
        guardarCambiosEnBackend();
        dialog.dismiss();
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
                        NavigationUtil.irALogin(getActivity());
                    }
                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        NavigationUtil.irALogin(getActivity());
                    }
                }
        );
    }

    private void ejecutarEliminarCuenta() {
        SessionManager sessionManager = new SessionManager(requireContext());
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        RetrofitClient.getApiService().eliminarCuenta(usuarioId).enqueue(new retrofit2.Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful()) {
                    eliminarRastroYSalir();
                    Toast.makeText(requireContext(), "Tu cuenta ha sido eliminada. Lamentamos verte partir.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar la cuenta", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarRastroYSalir() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("DeleteAccount", "Usuario eliminado de Firebase.");
                } else {
                    Log.e("DeleteAccount", "No se pudo borrar de Firebase, haciendo SignOut.");
                    FirebaseAuth.getInstance().signOut();
                }
                completarLimpiezaYRedirigir();
            });
        } else {
            completarLimpiezaYRedirigir();
        }
    }

    private void completarLimpiezaYRedirigir() {
        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.cerrarSesion();

        SharedPreferences loginPrefs = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        loginPrefs.edit().clear().apply();

        CredentialManager credentialManager = CredentialManager.create(requireContext());
        credentialManager.clearCredentialStateAsync(new ClearCredentialStateRequest(), null,
                ContextCompat.getMainExecutor(requireContext()), new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        NavigationUtil.irALogin(getActivity());
                    }
                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        NavigationUtil.irALogin(getActivity());
                    }
                });
    }

    private void cargarDatosDesdeCache() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE);
        SessionManager sessionManager = new SessionManager(requireContext());

        String nombreCache = prefs.getString("nombre", sessionManager.getUsuarioNombre());
        paisSeleccionado = prefs.getString("pais", "España");
        idiomaSeleccionado = prefs.getString("idioma", "Español");
        monedaSeleccionada = prefs.getString("moneda", "EUR");

        tvName.setText(nombreCache);
        tvPais.setText(paisSeleccionado);
        tvIdioma.setText(idiomaSeleccionado);
        tvMoneda.setText(monedaSeleccionada);

        cargarFotoPerfil();
    }

    private void guardarAjustesEnCache(String nombre, String pais, String idioma, String moneda) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE);
        prefs.edit()
                .putString("nombre", nombre)
                .putString("pais", pais)
                .putString("idioma", idioma)
                .putString("moneda", moneda)
                .apply();
    }
}