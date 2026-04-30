package com.pocketupdm.dialogs;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketupdm.R;
import com.pocketupdm.adapter.CategoriaAdapter;
import com.pocketupdm.model.Categoria;
import com.pocketupdm.model.Presupuesto;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevoPresupuestoBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvCategorias;
    private TextInputEditText etLimite, etRango; // <-- Añadido etRango
    private MaterialButton btnGuardar;
    private TextView tvTitulo;
    private String fechaInicioApi, fechaFinApi;

    private Long categoriaSeleccionadaId = null;
    private SessionManager sessionManager;
    private Presupuesto presupuestoAEditar = null;

    public interface OnPresupuestoGuardadoListener {
        void onGuardado();
    }
    private OnPresupuestoGuardadoListener listener;

    public void setListener(OnPresupuestoGuardadoListener listener) { this.listener = listener; }
    public void setPresupuestoAEditar(Presupuesto p) { this.presupuestoAEditar = p; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_nuevo_presupuesto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        rvCategorias = view.findViewById(R.id.rv_categorias_presupuesto);
        etLimite = view.findViewById(R.id.et_presupuesto_limite);
        etRango = view.findViewById(R.id.et_presupuesto_rango); // <-- Inicializado
        btnGuardar = view.findViewById(R.id.btn_guardar_presupuesto);
        tvTitulo = view.findViewById(R.id.tv_titulo_sheet_presupuesto);

        rvCategorias.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Listener para abrir el calendario de rango
        etRango.setOnClickListener(v -> abrirSelectorRango());

        cargarCategorias();

        // MODO EDICIÓN
        if (presupuestoAEditar != null) {
            tvTitulo.setText("Editar Presupuesto");
            btnGuardar.setText("Actualizar");
            etLimite.setText(String.valueOf(presupuestoAEditar.getMontoLimite()));
            categoriaSeleccionadaId = presupuestoAEditar.getCategoriaId();

            // Recuperamos las fechas del modo edición
            fechaInicioApi = presupuestoAEditar.getFechaInicio();
            fechaFinApi = presupuestoAEditar.getFechaFin();

            // Formateamos las fechas para que el usuario las vea bonitas al editar
            try {
                SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat sdfVisual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date inicio = sdfApi.parse(fechaInicioApi);
                Date fin = sdfApi.parse(fechaFinApi);
                if (inicio != null && fin != null) {
                    etRango.setText("Del " + sdfVisual.format(inicio) + " al " + sdfVisual.format(fin));
                }
            } catch (Exception e) {
                etRango.setText(fechaInicioApi + " - " + fechaFinApi);
            }
        }

        btnGuardar.setOnClickListener(v -> guardarPresupuesto());
    }

    private void cargarCategorias() {
        RetrofitClient.getApiService().obtenerCategorias(sessionManager.getUsuarioId()).enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Pasamos la categoría seleccionada si estamos editando para que aparezca marcada
                    CategoriaAdapter adapter = new CategoriaAdapter(getContext(), response.body(), categoria -> {
                        categoriaSeleccionadaId = categoria.getId();
                    });
                    rvCategorias.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<Categoria>> call, Throwable t) {}
        });
    }

    private void guardarPresupuesto() {
        String limiteStr = etLimite.getText().toString().trim();

        // VALIDACIONES
        if (categoriaSeleccionadaId == null) {
            Toast.makeText(getContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }
        if (limiteStr.isEmpty()) {
            etLimite.setError("Introduce un límite");
            return;
        }
        if (fechaInicioApi == null || fechaFinApi == null) {
            etRango.setError("Selecciona la duración");
            Toast.makeText(getContext(), "Selecciona un rango de fechas", Toast.LENGTH_SHORT).show();
            return;
        }

        BigDecimal limite = new BigDecimal(limiteStr.replace(",", "."));

        Presupuesto p = new Presupuesto();
        p.setMontoLimite(limite);
        p.setCategoriaId(categoriaSeleccionadaId);
        p.setUsuarioId(sessionManager.getUsuarioId());
        p.setFechaInicio(fechaInicioApi); // <-- Enviamos fecha inicio
        p.setFechaFin(fechaFinApi);       // <-- Enviamos fecha fin

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        Call<Presupuesto> call = (presupuestoAEditar != null)
                ? RetrofitClient.getApiService().actualizarPresupuesto(presupuestoAEditar.getId(), p)
                : RetrofitClient.getApiService().crearPresupuesto(p);

        call.enqueue(new Callback<Presupuesto>() {
            @Override
            public void onResponse(Call<Presupuesto> call, Response<Presupuesto> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Presupuesto guardado", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onGuardado();
                    dismiss();
                } else {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText(presupuestoAEditar != null ? "Actualizar" : "Guardar");

                    // Si falla (Error 400 o 500) asumimos que es por la regla de solapamiento
                    Toast.makeText(getContext(), "Ya tienes un presupuesto activo para esta categoría en esas fechas", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Presupuesto> call, Throwable t) {
                if (!isAdded()) return;
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirSelectorRango() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Selecciona el periodo")
                        .setTheme(R.style.Theme_App_Calendar_Turquesa)
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdfApi.setTimeZone(tz);

            SimpleDateFormat sdfVisual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdfVisual.setTimeZone(tz);

            fechaInicioApi = sdfApi.format(new Date(selection.first));
            fechaFinApi = sdfApi.format(new Date(selection.second));

            String visual = "Del " + sdfVisual.format(new Date(selection.first)) +
                    " al " + sdfVisual.format(new Date(selection.second));
            etRango.setText(visual);
            etRango.setError(null); // Limpiamos el error si existía
        });

        picker.show(getParentFragmentManager(), "RANGE_PICKER");
    }
}