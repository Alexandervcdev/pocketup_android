package com.pocketupdm.dialogs;

import static com.pocketupdm.utils.DialogUtils.mostrarDialogoConfirmacion;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pocketupdm.R;
import com.pocketupdm.adapter.CategoriaAdapter;
import com.pocketupdm.adapter.MetaSelectorAdapter;
import com.pocketupdm.model.Categoria;
import com.pocketupdm.model.Meta;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.model.Presupuesto;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovimientoBottomSheet extends BottomSheetDialogFragment {

    private MovementType tipo;
    private OnMovimientoGuardadoListener listener;
    private String fechaSeleccionada; // Formato YYYY-MM-DD para la API

    // VARIABLES PARA CATEGORÍAS
    private RecyclerView rvCategorias;
    private CategoriaAdapter categoriaAdapter;
    private Long categoriaSeleccionadaId = null;
    private SessionManager sessionManager;

    private boolean isModoEdicionCategorias = false;
    private MaterialButton btnEditarToggle;

    // VARIABLES PARA METAS
    private LinearLayout llAsignarMeta;
    private MaterialSwitch switchAsignarMeta;

    private RecyclerView rvMetasSelector;

    private List<Meta> metasDisponibles;
    private Long metaSeleccionadaId = null;

    private TextView tvAdvertenciaPresupuesto;
    private List<Presupuesto> presupuestosUsuario = new ArrayList<>();

    // Interfaz para avisarle al HomeFragment que se guardó un dato
    public interface OnMovimientoGuardadoListener {
        void onGuardar(BigDecimal importe, String nota, MovementType tipo, String fecha, Long categoriaId);
    }

    public MovimientoBottomSheet(MovementType tipo, OnMovimientoGuardadoListener listener) {
        this.tipo = tipo;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_movimiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        // --- 1. CONFIGURACIÓN DE VISTAS ---
        rvCategorias = view.findViewById(R.id.rv_categorias_selector);
        rvCategorias.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        btnEditarToggle = view.findViewById(R.id.btn_editar_categorias_toggle);

        llAsignarMeta = view.findViewById(R.id.ll_asignar_meta);
        switchAsignarMeta = view.findViewById(R.id.switch_asignar_meta);
        llAsignarMeta = view.findViewById(R.id.ll_asignar_meta);
        switchAsignarMeta = view.findViewById(R.id.switch_asignar_meta);

        // NUEVO: Enlazar el RecyclerView
        rvMetasSelector = view.findViewById(R.id.rv_metas_selector);
        rvMetasSelector.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tvAdvertenciaPresupuesto = view.findViewById(R.id.tv_advertencia_presupuesto_movimiento);

        // --- 2. CARGAS INICIALES ---
        cargarCategoriasDesdeBackend();
        cargarMetasDelUsuario(); 
        cargarPresupuestosDelUsuario();

        // --- 3. LÓGICA DEL INTERRUPTOR DE METAS ---
        switchAsignarMeta.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rvMetasSelector.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                filtrarYMostrarMetas();
            } else {
                metaSeleccionadaId = null;
                rvMetasSelector.setAdapter(null); // Limpiamos la selección
            }
        });

        // --- 4. LÓGICA DE CATEGORÍAS (Edición y Creación) ---
        btnEditarToggle.setOnClickListener(v -> {
            isModoEdicionCategorias = !isModoEdicionCategorias;
            if (isModoEdicionCategorias) {
                btnEditarToggle.setText("Cancelar");
                btnEditarToggle.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red)));
                btnEditarToggle.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                Toast.makeText(getContext(), "Toca una categoría para ver opciones", Toast.LENGTH_SHORT).show();
            } else {
                btnEditarToggle.setText("Editar");
                btnEditarToggle.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.darker_gray)));
                btnEditarToggle.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            }
        });

        MaterialButton btnNuevaCategoria = view.findViewById(R.id.btn_nueva_categoria);
        btnNuevaCategoria.setOnClickListener(v -> {
            NuevaCategoriaBottomSheet bottomSheet = new NuevaCategoriaBottomSheet();
            bottomSheet.setListener(this::cargarCategoriasDesdeBackend);
            bottomSheet.show(getParentFragmentManager(), "NuevaCategoria");
        });

        // --- 5. LÓGICA DE FORMULARIO (Fecha y Textos) ---
        TextView tvTitulo = view.findViewById(R.id.tv_titulo_sheet);
        TextInputEditText etImporte = view.findViewById(R.id.et_importe);
        TextInputEditText etFecha = view.findViewById(R.id.et_fecha);
        TextInputEditText etNota = view.findViewById(R.id.et_nota);
        MaterialButton btnGuardar = view.findViewById(R.id.btn_guardar_movimiento);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfVisual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        fechaSeleccionada = sdfApi.format(calendar.getTime());
        etFecha.setText(sdfVisual.format(calendar.getTime()));

        etFecha.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Selecciona una fecha")
                    .setTheme(R.style.Theme_App_Calendar_Turquesa)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(selection);
                fechaSeleccionada = sdfApi.format(utc.getTime());
                etFecha.setText(sdfVisual.format(utc.getTime()));

                // Si cambia la fecha y el switch está activo, hay que recalcular las metas disponibles
                if (switchAsignarMeta.isChecked()) {
                    filtrarYMostrarMetas();
                }
            });
            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        // ¡MAGIA VISUAL! Cambiamos los colores según el tipo de movimiento
        if (tipo == MovementType.INGRESO) {
            tvTitulo.setText("Nuevo Ingreso");
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro));
            llAsignarMeta.setVisibility(View.VISIBLE); // Mostrar sección de metas
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro)));
        } else {
            tvTitulo.setText("Nuevo Gasto");
            llAsignarMeta.setVisibility(View.GONE); // Ocultar sección de metas
            switchAsignarMeta.setChecked(false);
            metaSeleccionadaId = null;
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red)));
        }

        // --- 6. ACCIÓN DE GUARDAR ---
        btnGuardar.setOnClickListener(v -> {
            String importeStr = etImporte.getText().toString().trim();
            String notaStr = etNota.getText().toString().trim();

            if (importeStr.isEmpty()) { etImporte.setError("Obligatorio"); return; }

            try {
                BigDecimal importe = new BigDecimal(importeStr.replace(",", "."));
                if (importe.compareTo(BigDecimal.ZERO) <= 0) { etImporte.setError("Debe ser mayor a 0"); return; }
                if (importe.compareTo(new BigDecimal("99999999.99")) > 0) { etImporte.setError("Cantidad demasiado grande"); return; }

                if (notaStr.isEmpty()) notaStr = "Sin nota";
                if (categoriaSeleccionadaId == null) { Toast.makeText(getContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show(); return; }

                // VERIFICACIÓN DE LA META: Si encendió el switch, debe haber elegido una meta
                if (tipo == MovementType.INGRESO && switchAsignarMeta.isChecked()) {
                    if (metaSeleccionadaId == null) {
                        Toast.makeText(getContext(), "Selecciona una meta en el menú desplegable", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Disparamos la llamada silenciosa al backend para sumar el dinero a la meta
                    aportarDineroAMetaSilenciosamente(metaSeleccionadaId, importe);
                }

                // Guardamos el movimiento de forma normal
                listener.onGuardar(importe, notaStr, tipo, fechaSeleccionada, categoriaSeleccionadaId);
                dismiss();

            } catch (NumberFormatException e) {
                etImporte.setError("Formato inválido");
            }
        });
    }

    private void cargarPresupuestosDelUsuario() {
        RetrofitClient.getApiService().obtenerPresupuestos(sessionManager.getUsuarioId()).enqueue(new Callback<List<com.pocketupdm.model.Presupuesto>>() {
            @Override
            public void onResponse(Call<List<com.pocketupdm.model.Presupuesto>> call, Response<List<com.pocketupdm.model.Presupuesto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    presupuestosUsuario = response.body(); // Guardamos los presupuestos en secreto
                }
            }
            @Override
            public void onFailure(Call<List<com.pocketupdm.model.Presupuesto>> call, Throwable t) {}
        });
    }

    // ========================================================
    // MÉTODOS DE BACKEND Y LÓGICA DE NEGOCIO
    // ========================================================

    // 1. Carga inicial de metas
    private void cargarMetasDelUsuario() {
        RetrofitClient.getApiService().obtenerMetas(sessionManager.getUsuarioId()).enqueue(new Callback<List<Meta>>() {
            @Override
            public void onResponse(Call<List<Meta>> call, Response<List<Meta>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    metasDisponibles = response.body();
                }
            }
            @Override
            public void onFailure(Call<List<Meta>> call, Throwable t) {}
        });
    }

    // 2. Filtrado de fechas (Core UI)
    private void filtrarYMostrarMetas() {
        if (metasDisponibles == null || metasDisponibles.isEmpty()) {
            Toast.makeText(getContext(), "No tienes metas activas", Toast.LENGTH_SHORT).show();
            switchAsignarMeta.setChecked(false);
            return;
        }

        List<String> nombresMetasValidas = new ArrayList<>();
        final List<Meta> metasValidas = new ArrayList<>();

        for (Meta meta : metasDisponibles) {
            // Solo metas cuya fecha límite es >= a la fecha seleccionada del ingreso
            if (meta.getFechaLimite().compareTo(fechaSeleccionada) >= 0) {
                metasValidas.add(meta);
                nombresMetasValidas.add(meta.getNombre());
            }
        }

        if (metasValidas.isEmpty()) {
            Toast.makeText(getContext(), "Ninguna meta coincide con la fecha del ingreso", Toast.LENGTH_SHORT).show();
            switchAsignarMeta.setChecked(false);
            return;
        }

        // NUEVO: Usamos tu MetaSelectorAdapter
        com.pocketupdm.adapter.MetaSelectorAdapter adapter = new MetaSelectorAdapter(getContext(), metasValidas, meta -> {
            metaSeleccionadaId = meta.getId(); // Guardamos el ID cuando toca la tarjeta
        });
        rvMetasSelector.setAdapter(adapter);
    }

    // 3. Aportar a la meta (Llamada paralela)
    private void aportarDineroAMetaSilenciosamente(Long idMeta, BigDecimal cantidad) {
        Map<String, BigDecimal> payload = new HashMap<>();
        payload.put("cantidad", cantidad);

        RetrofitClient.getApiService().agregarFondosMeta(idMeta, payload).enqueue(new Callback<Meta>() {
            @Override public void onResponse(Call<Meta> call, Response<Meta> response) { /* Éxito silencioso */ }
            @Override public void onFailure(Call<Meta> call, Throwable t) { /* Fallo silencioso */ }
        });
    }

    // ... (Mantengo intactos tus métodos de cargarCategoriasDesdeBackend y eliminarCategoriaEnBackend) ...
    private void cargarCategoriasDesdeBackend() {
        RetrofitClient.getApiService().obtenerCategorias(sessionManager.getUsuarioId()).enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoriaAdapter = new CategoriaAdapter(getContext(), response.body(), categoria -> {

                        if (isModoEdicionCategorias) {
                            // === MODO EDICIÓN ACTIVADO ===
                            if (categoria.getUsuarioId() == null) {
                                Toast.makeText(getContext(), "Las categorías del sistema no se pueden modificar", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(categoria.getNombre())
                                    .setItems(new String[]{"Editar", "Eliminar"}, (dialog, which) -> {
                                        if (which == 0) abrirEditorCategoria(categoria);
                                        else confirmarEliminacion(categoria);
                                        btnEditarToggle.performClick();
                                    }).show();
                        } else {
                            // === MODO NORMAL (Seleccionar para el movimiento) ===
                            categoriaSeleccionadaId = categoria.getId();

                            // 1. Ocultamos el aviso por defecto cada vez que tocamos una categoría
                            if (tvAdvertenciaPresupuesto != null) {
                                tvAdvertenciaPresupuesto.setVisibility(View.GONE);
                            }

                            // 2. SI ES UN GASTO, COMPROBAMOS EL SEMÁFORO DEL PRESUPUESTO
                            if (tipo == MovementType.GASTO && presupuestosUsuario != null) {
                                for (com.pocketupdm.model.Presupuesto p : presupuestosUsuario) {
                                    // Buscamos si esta categoría tiene un presupuesto asociado
                                    if (p.getCategoria() != null && p.getCategoria().getId().equals(categoria.getId())) {

                                        BigDecimal gastado = p.getMontoGastado() != null ? p.getMontoGastado() : BigDecimal.ZERO;
                                        BigDecimal limite = p.getMontoLimite();

                                        if (limite.compareTo(BigDecimal.ZERO) > 0) {
                                            // Calculamos el porcentaje
                                            int porcentaje = gastado.divide(limite, 2, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")).intValue();

                                            if (porcentaje >= 100) {
                                                // ROJO: Límite Superado
                                                tvAdvertenciaPresupuesto.setText("¡Cuidado! Ya superaste tu límite de " + p.getCategoria().getNombre() + " (" + porcentaje + "%).");
                                                tvAdvertenciaPresupuesto.setTextColor(android.graphics.Color.RED);
                                                tvAdvertenciaPresupuesto.setBackgroundColor(android.graphics.Color.parseColor("#1AFF0000")); // Fondo rojo transparente
                                                tvAdvertenciaPresupuesto.setVisibility(View.VISIBLE);
                                            } else if (porcentaje >= 75) {
                                                // NARANJA: Alerta de cercanía
                                                tvAdvertenciaPresupuesto.setText("Aviso: Has consumido el " + porcentaje + "% de tu presupuesto en " + p.getCategoria().getNombre() + ".");
                                                tvAdvertenciaPresupuesto.setTextColor(android.graphics.Color.parseColor("#FF9800")); // Naranja Material
                                                tvAdvertenciaPresupuesto.setBackgroundColor(android.graphics.Color.parseColor("#1AFFE0B2")); // Fondo naranja transparente
                                                tvAdvertenciaPresupuesto.setVisibility(View.VISIBLE);
                                            }
                                        }
                                        break; // Ya evaluamos esta categoría, salimos del bucle
                                    }
                                }
                            }
                        }
                    });
                    rvCategorias.setAdapter(categoriaAdapter);
                }
            }
            @Override public void onFailure(Call<List<Categoria>> call, Throwable t) {}
        });
    }

    private void eliminarCategoriaEnBackend(Long idCategoria) {
        RetrofitClient.getApiService().eliminarCategoria(idCategoria).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show();
                    cargarCategoriasDesdeBackend();
                } else {
                    Toast.makeText(getContext(), "No se puede eliminar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void abrirEditorCategoria(Categoria categoria) {
        NuevaCategoriaBottomSheet editSheet = new NuevaCategoriaBottomSheet();
        editSheet.setCategoriaAEditar(categoria);
        editSheet.setListener(this::cargarCategoriasDesdeBackend);
        editSheet.show(getParentFragmentManager(), "EditarCategoria");
    }

    private void confirmarEliminacion(Categoria categoria) {
        mostrarDialogoConfirmacion(
                requireContext(),
                "Eliminar Categoría",
                "¿Seguro que quieres eliminar '" + categoria.getNombre() + "'? Sus movimientos irán a 'General' y se borrará cualquier presupuesto asociado.",
                "Eliminar",
                R.color.black_pu,
                R.color.red,
                () -> eliminarCategoriaEnBackend(categoria.getId())
        );
    }
}