package com.pocketupdm.dialogs;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketupdm.R;
import com.pocketupdm.model.Meta;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevaMetaBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText etNombre, etObjetivo, etFecha;
    private LinearLayout llColores, llIconos;
    private MaterialButton btnGuardar;
    private TextView tvTitulo;

    // Puedes añadir iconos más relacionados a metas aquí
    private final String[] PALETA_COLORES = {"#F44336", "#E91E63", "#9C27B0", "#3F51B5", "#2196F3", "#00BCD4", "#4CAF50", "#FFC107", "#FF9800", "#795548"};
    private final String[] PALETA_ICONOS = {"ic_flight", "ic_home", "ic_laptop", "ic_directions_car", "ic_school", "ic_shopping_cart", "ic_pets", "ic_favorite"};

    private String colorSeleccionado = null;
    private String iconoSeleccionado = null;
    private String fechaSeleccionadaParaApi = null; // Guardará "YYYY-MM-DD"

    private View vistaColorSeleccionada = null;
    private View vistaIconoSeleccionada = null;

    private SessionManager sessionManager;
    private Meta metaAEditar = null;

    public interface OnMetaGuardadaListener {
        void onGuardada();
    }
    private OnMetaGuardadaListener listener;

    public void setListener(OnMetaGuardadaListener listener) { this.listener = listener; }
    public void setMetaAEditar(Meta meta) { this.metaAEditar = meta; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_nueva_meta, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        etNombre = view.findViewById(R.id.et_meta_nombre);
        etObjetivo = view.findViewById(R.id.et_meta_objetivo);
        etFecha = view.findViewById(R.id.et_meta_fecha);
        llColores = view.findViewById(R.id.ll_colores_meta);
        llIconos = view.findViewById(R.id.ll_iconos_meta);
        btnGuardar = view.findViewById(R.id.btn_guardar_meta);
        tvTitulo = view.findViewById(R.id.tv_titulo_sheet_meta);

        // LÓGICA DEL CALENDARIO
        etFecha.setOnClickListener(v -> abrirSelectorFecha());

        // MODO EDICIÓN
        if (metaAEditar != null) {
            tvTitulo.setText("Editar Meta");
            btnGuardar.setText("Actualizar");
            etNombre.setText(metaAEditar.getNombre());
            etObjetivo.setText(String.valueOf(metaAEditar.getMontoObjetivo()));

            // La fecha viene del backend en "YYYY-MM-DD"
            fechaSeleccionadaParaApi = metaAEditar.getFechaLimite();
            etFecha.setText(fechaSeleccionadaParaApi); // Aquí podríamos formatearla mejor para la vista

            colorSeleccionado = metaAEditar.getColor();
            iconoSeleccionado = metaAEditar.getIcono();
        }

        generarPaletaColores();
        generarPaletaIconos();

        btnGuardar.setOnClickListener(v -> guardarMeta());
    }

    private void abrirSelectorFecha() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona la fecha límite")
                // Asegúrate de usar el tema de calendario que creaste para los movimientos
                .setTheme(R.style.Theme_App_Calendar_Turquesa)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);

            SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            fechaSeleccionadaParaApi = sdfApi.format(utc.getTime());

            // Para la vista del usuario
            SimpleDateFormat sdfVisual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etFecha.setText(sdfVisual.format(utc.getTime()));
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER_META");
    }

    // --- COPIA EXACTA DE LA LÓGICA DE CATEGORÍAS ---
    private void generarPaletaColores() {
        for (String hexColor : PALETA_COLORES) {
            View colorView = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 120);
            params.setMargins(8, 0, 16, 0);
            colorView.setLayoutParams(params);
            colorView.setBackgroundResource(R.drawable.circle_background);
            colorView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(hexColor)));

            if (hexColor.equals(colorSeleccionado)) {
                colorView.setAlpha(0.5f);
                vistaColorSeleccionada = colorView;
            }

            colorView.setOnClickListener(v -> {
                if (vistaColorSeleccionada != null) vistaColorSeleccionada.setAlpha(1.0f);
                colorView.setAlpha(0.5f);
                vistaColorSeleccionada = colorView;
                colorSeleccionado = hexColor;
            });
            llColores.addView(colorView);
        }
    }

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

                if (nombreIcono.equals(iconoSeleccionado)) {
                    iconView.setColorFilter(Color.BLACK);
                    vistaIconoSeleccionada = iconView;
                } else {
                    iconView.setColorFilter(Color.GRAY);
                }

                iconView.setOnClickListener(v -> {
                    if (vistaIconoSeleccionada != null) ((ImageView) vistaIconoSeleccionada).setColorFilter(Color.GRAY);
                    iconView.setColorFilter(Color.BLACK);
                    vistaIconoSeleccionada = iconView;
                    iconoSeleccionado = nombreIcono;
                });
                llIconos.addView(iconView);
            }
        }
    }

    // --- LÓGICA DE GUARDADO ---
    private void guardarMeta() {
        String nombre = etNombre.getText().toString().trim();
        String objetivoStr = etObjetivo.getText().toString().trim();

        if (nombre.isEmpty()) { etNombre.setError("Requerido"); return; }
        if (objetivoStr.isEmpty()) { etObjetivo.setError("Requerido"); return; }
        if (fechaSeleccionadaParaApi == null) { etFecha.setError("Selecciona una fecha"); return; }
        if (colorSeleccionado == null) { Toast.makeText(getContext(), "Selecciona un color", Toast.LENGTH_SHORT).show(); return; }
        if (iconoSeleccionado == null) { Toast.makeText(getContext(), "Selecciona un icono", Toast.LENGTH_SHORT).show(); return; }

        BigDecimal objetivo;
        try {
            objetivo = new BigDecimal(objetivoStr.replace(",", "."));
            if (objetivo.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etObjetivo.setError("Monto inválido");
            return;
        }

        // Construir la Meta
        Meta meta = new Meta();
        meta.setNombre(nombre);
        meta.setMontoObjetivo(objetivo);
        meta.setFechaLimite(fechaSeleccionadaParaApi);
        meta.setColor(colorSeleccionado);
        meta.setIcono(iconoSeleccionado);

        Long idReal = sessionManager.getUsuarioId();
        if (idReal != -1L) meta.setUsuarioId(idReal);

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        Call<Meta> call;
        if (metaAEditar != null) {
            call = RetrofitClient.getApiService().actualizarMeta(metaAEditar.getId(), meta);
        } else {
            call = RetrofitClient.getApiService().crearMeta(meta);
        }

        call.enqueue(new Callback<Meta>() {
            @Override
            public void onResponse(Call<Meta> call, Response<Meta> response) {
                if (!isAdded() || getContext() == null) return; // Escudo

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "¡Meta guardada!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onGuardada();
                    dismiss();
                } else {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText(metaAEditar != null ? "Actualizar" : "Crear Meta");
                    Toast.makeText(getContext(), "Error del servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Meta> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                btnGuardar.setEnabled(true);
                btnGuardar.setText(metaAEditar != null ? "Actualizar" : "Crear Meta");
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}