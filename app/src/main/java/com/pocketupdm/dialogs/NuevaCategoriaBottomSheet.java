package com.pocketupdm.dialogs;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketupdm.R;
import com.pocketupdm.model.Categoria;
import com.pocketupdm.model.Usuario;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevaCategoriaBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText etNombre;
    private LinearLayout llColores, llIconos;
    private MaterialButton btnGuardar;

    // Paletas predefinidas para que el usuario elija
    private final String[] PALETA_COLORES = {"#F44336", "#E91E63", "#9C27B0", "#3F51B5", "#2196F3", "#00BCD4", "#4CAF50", "#FFC107", "#FF9800", "#795548"};
    private final String[] PALETA_ICONOS = {"ic_pets", "ic_fitness_center", "ic_shopping_cart", "ic_flight", "ic_school", "ic_home", "ic_local_cafe"};

    private String colorSeleccionado = null;
    private String iconoSeleccionado = null;
    private View vistaColorSeleccionada = null;
    private View vistaIconoSeleccionada = null;

    private SessionManager sessionManager;

    private Categoria categoriaAEditar = null;
    public void setCategoriaAEditar(Categoria categoria) {
        this.categoriaAEditar = categoria;
    }
    public interface OnCategoriaGuardadaListener {
        void onGuardada();
    }

    private OnCategoriaGuardadaListener listener;

    public void setListener(OnCategoriaGuardadaListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_nueva_categoria, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        etNombre = view.findViewById(R.id.et_categoria_nombre);
        llColores = view.findViewById(R.id.ll_contenedor_colores);
        llIconos = view.findViewById(R.id.ll_contenedor_iconos);
        btnGuardar = view.findViewById(R.id.btn_guardar_categoria);

        if (categoriaAEditar != null) {
            etNombre.setText(categoriaAEditar.getNombre());
            colorSeleccionado = categoriaAEditar.getColor();
            iconoSeleccionado = categoriaAEditar.getIcono();
            btnGuardar.setText("Actualizar");
        }

        generarPaletaColores();
        generarPaletaIconos();



        btnGuardar.setOnClickListener(v -> guardarCategoria());
    }

    // --- MAGIA UI: Generar círculos de colores ---
    private void generarPaletaColores() {
        for (String hexColor : PALETA_COLORES) {
            View colorView = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 120);
            params.setMargins(8, 0, 16, 0);
            colorView.setLayoutParams(params);

            colorView.setBackgroundResource(R.drawable.circle_background);
            colorView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(hexColor)));

            // ¡NUEVO!: Pre-seleccionar visualmente si estamos editando
            if (hexColor.equals(colorSeleccionado)) {
                colorView.setAlpha(0.5f); // Lo marcamos como seleccionado
                vistaColorSeleccionada = colorView;
            }

            colorView.setOnClickListener(v -> {
                if (vistaColorSeleccionada != null) {
                    vistaColorSeleccionada.setAlpha(1.0f); // Desmarca el anterior
                }
                colorView.setAlpha(0.5f); // Marca el actual con opacidad
                vistaColorSeleccionada = colorView;
                colorSeleccionado = hexColor;
            });

            llColores.addView(colorView);
        }
    }

    // Generar iconos seleccionables ---
    // Generar iconos seleccionables ---
    private void generarPaletaIconos() {
        for (String nombreIcono : PALETA_ICONOS) {
            ImageView iconView = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 120);
            params.setMargins(8, 0, 16, 0);
            iconView.setLayoutParams(params);
            iconView.setPadding(24, 24, 24, 24);

            int resId = getResources().getIdentifier(nombreIcono, "drawable", requireContext().getPackageName());
            if (resId != 0) {
                iconView.setImageResource(resId);
                iconView.setBackgroundResource(R.drawable.circle_background);
                iconView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));

                // ¡NUEVO!: Pre-seleccionar visualmente si estamos editando
                if (nombreIcono.equals(iconoSeleccionado)) {
                    iconView.setColorFilter(Color.BLACK); // Lo marcamos oscuro
                    vistaIconoSeleccionada = iconView;
                } else {
                    iconView.setColorFilter(Color.GRAY); // Los demás quedan grises
                }

                iconView.setOnClickListener(v -> {
                    if (vistaIconoSeleccionada != null) {
                        ((ImageView) vistaIconoSeleccionada).setColorFilter(Color.GRAY); // Desmarca el anterior
                    }
                    iconView.setColorFilter(Color.BLACK); // Resalta el icono seleccionado
                    vistaIconoSeleccionada = iconView;
                    iconoSeleccionado = nombreIcono;
                });

                llIconos.addView(iconView);
            }
        }
    }

    // --- LÓGICA DE BACKEND ---
    private void guardarCategoria() {
        String nombre = etNombre.getText().toString().trim();

        // 1. Validaciones básicas (para crear y editar categoria)
        if (nombre.isEmpty()) {
            etNombre.setError("El nombre es obligatorio");
            return;
        }

        if (colorSeleccionado == null) {
            Toast.makeText(getContext(), "Seleccione un color", Toast.LENGTH_SHORT).show();
            return;
        }

        if (iconoSeleccionado == null) {
            Toast.makeText(getContext(), "Seleccione un icono", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Construimos el objeto con los datos de la UI
        Categoria categoriaDatos = new Categoria();
        categoriaDatos.setNombre(nombre);
        categoriaDatos.setColor(colorSeleccionado);
        categoriaDatos.setIcono(iconoSeleccionado);

        Long idReal = sessionManager.getUsuarioId();
        if (idReal != -1L) {
            categoriaDatos.setUsuarioId(idReal); // ¡Perfecto! Enviamos el ID plano directamente
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText(categoriaAEditar != null ? "Actualizando..." : "Guardando...");

        // 3. MAGIA: Decidimos qué endpoint llamar
        Call<Categoria> call;
        String mensajeExito;

        if (categoriaAEditar != null) {
            // MODO EDICIÓN: Usamos PUT y el ID de la categoría existente
            call = RetrofitClient.getApiService().actualizarCategoria(categoriaAEditar.getId(), categoriaDatos);
            mensajeExito = "Categoría actualizada";
        } else {
            // MODO NUEVO: Usamos POST
            call = RetrofitClient.getApiService().crearCategoria(categoriaDatos);
            mensajeExito = "Categoría creada con éxito";
        }

        // 4. Ejecutamos la llamada
        call.enqueue(new Callback<Categoria>() {
            @Override
            public void onResponse(Call<Categoria> call, Response<Categoria> response) {
                // ¡ESCUDO PROTECTOR VITAL AQUÍ!
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), mensajeExito, Toast.LENGTH_SHORT).show();

                    if (listener != null) {
                        listener.onGuardada(); // Avisamos a la pantalla de atrás para que recargue
                    }
                    dismiss();
                } else {
                    reestablecerBoton();
                    Toast.makeText(getContext(), "Error del servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Categoria> call, Throwable t) {
                // ¡ESCUDO PROTECTOR VITAL AQUÍ!
                if (!isAdded() || getContext() == null) return;

                reestablecerBoton();
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reestablecerBoton() {
        btnGuardar.setEnabled(true);
        btnGuardar.setText(categoriaAEditar != null ? "Actualizar" : "Crear Categoría");
    }
}