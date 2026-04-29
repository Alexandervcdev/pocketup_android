package com.pocketupdm.dialogs;

import static com.pocketupdm.utils.DialogUtils.mostrarDialogoConfirmacion;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.google.android.material.color.MaterialColors;
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
import com.pocketupdm.utils.DialogUtils;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

/**
 * Esta clase se utiliza para la creación de nuevos Movimientos (Ingresos o Gastos).
 * Gestiona la selección de categorías, asignación a metas y alertas de presupuestos.
 */
public class MovimientoBottomSheet extends BottomSheetDialogFragment {

    private MovementType tipo;
    private OnMovimientoGuardadoListener listener;
    private String fechaSeleccionada; // Formato YYYY-MM-DD para enviar a la API

    // --- VARIABLES DE CATEGORÍAS ---
    private RecyclerView rvCategorias;
    private CategoriaAdapter categoriaAdapter;
    private Long categoriaSeleccionadaId = null;
    private boolean isModoEdicionCategorias = false;
    private MaterialButton btnEditarToggle;

    // --- VARIABLES DE METAS ---
    private LinearLayout llAsignarMeta;
    private MaterialSwitch switchAsignarMeta;
    private RecyclerView rvMetasSelector;
    private List<Meta> metasDisponibles;
    private Long metaSeleccionadaId = null;

    // --- VARIABLES DE PRESUPUESTOS ---
    private TextView tvAdvertenciaPresupuesto;
    private List<Presupuesto> presupuestosUsuario = new ArrayList<>();

    private SessionManager sessionManager;

    /**
     * Interfaz de comunicación para devolver los datos validados a la pantalla Home (u otra).
     */
    public interface OnMovimientoGuardadoListener {
        void onGuardar(BigDecimal importe, String nota, MovementType tipo, String fecha, Long categoriaId);
    }

    /**
     * Constructor del BottomSheet.
     * @param tipo INGRESO o GASTO. Determina el color y los campos visibles (ej. Metas).
     * @param listener Callback para enviar los datos una vez validados.
     */
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

        // --- CONFIGURACIÓN DE VISTAS ---
        rvCategorias = view.findViewById(R.id.rv_categorias_selector);
        rvCategorias.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        btnEditarToggle = view.findViewById(R.id.btn_editar_categorias_toggle);

        llAsignarMeta = view.findViewById(R.id.ll_asignar_meta);
        switchAsignarMeta = view.findViewById(R.id.switch_asignar_meta);
        rvMetasSelector = view.findViewById(R.id.rv_metas_selector);
        rvMetasSelector.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        tvAdvertenciaPresupuesto = view.findViewById(R.id.tv_advertencia_presupuesto_movimiento);

        // --- CARGAS INICIALES (Llamadas al Backend) ---
        cargarCategoriasDesdeBackend();
        cargarMetasDelUsuario(); 
        cargarPresupuestosDelUsuario();

        // --- LÓGICA DEL INTERRUPTOR DE METAS ---
        switchAsignarMeta.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rvMetasSelector.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                filtrarYMostrarMetas();
            } else {
                metaSeleccionadaId = null;
                rvMetasSelector.setAdapter(null);
            }
        });

        // --- LÓGICA DE EDICIÓN DE CATEGORÍAS ---
        btnEditarToggle.setOnClickListener(v -> {
            isModoEdicionCategorias = !isModoEdicionCategorias;
            if (isModoEdicionCategorias) {
                btnEditarToggle.setText("Cancelar");
                btnEditarToggle.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
                Toast.makeText(getContext(), "Toca una categoría para ver opciones", Toast.LENGTH_SHORT).show();
            } else {
                btnEditarToggle.setText("Editar");
                int colorOnSurface = MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface);
                btnEditarToggle.setIconTint(ColorStateList.valueOf(colorOnSurface));
                btnEditarToggle.setTextColor(colorOnSurface);
            }
        });

        MaterialButton btnNuevaCategoria = view.findViewById(R.id.btn_nueva_categoria);
        btnNuevaCategoria.setOnClickListener(v -> {
            NuevaCategoriaBottomSheet bottomSheet = new NuevaCategoriaBottomSheet();
            bottomSheet.setListener(this::cargarCategoriasDesdeBackend);
            bottomSheet.show(getParentFragmentManager(), "NuevaCategoria");
        });

        // --- CONFIGURACIÓN DEL FORMULARIO Y FECHA ---
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

                // Recalcular metas si cambia la fecha y el switch está activo
                if (switchAsignarMeta.isChecked()) {
                    filtrarYMostrarMetas();
                }
            });
            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        // ---  COLOREADO DINÁMICO (INGRESO VS GASTO) ---
        TextInputLayout tilImporte = view.findViewById(R.id.til_importe);
        TextInputLayout tilFecha = view.findViewById(R.id.til_fecha);
        TextInputLayout tilNota = view.findViewById(R.id.til_nota);

        if (tipo == MovementType.INGRESO) {
            tvTitulo.setText("Nuevo Ingreso");
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro));
            llAsignarMeta.setVisibility(View.VISIBLE);
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro)));

            // Configurar colores del Switch de Metas a Turquesa
            int colorTurquesa = ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro);
            int colorGris = ContextCompat.getColor(requireContext(), android.R.color.darker_gray);
            ColorStateList thumbStates = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{
                            colorTurquesa,
                            colorGris
                    }
            );
            // Configurar colores del Switch de Metas a Turquesa
            ColorStateList trackStates = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{
                            androidx.core.graphics.ColorUtils.setAlphaComponent(colorTurquesa, 120),
                            androidx.core.graphics.ColorUtils.setAlphaComponent(colorGris, 100)
                    }
            );
            switchAsignarMeta.setThumbTintList(thumbStates);
            switchAsignarMeta.setTrackTintList(trackStates);

            // Bordes turquesa
            if (tilImporte != null) tilImporte.setBoxStrokeColor(colorTurquesa);
            if (tilFecha != null) tilFecha.setBoxStrokeColor(colorTurquesa);
            if (tilNota != null) tilNota.setBoxStrokeColor(colorTurquesa);

        }else {
            tvTitulo.setText("Nuevo Gasto");
            llAsignarMeta.setVisibility(View.GONE);
            switchAsignarMeta.setChecked(false);
            metaSeleccionadaId = null;
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red)));

            // Bordes rojos
            int colorRojo = ContextCompat.getColor(requireContext(), R.color.red);
            if (tilImporte != null) tilImporte.setBoxStrokeColor(colorRojo);
            if (tilFecha != null) tilFecha.setBoxStrokeColor(colorRojo);
            if (tilNota != null) tilNota.setBoxStrokeColor(colorRojo);
        }

        // --- ACCIÓN DE GUARDAR ---
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

                // Verificación de Meta en caso de Ingreso
                if (tipo == MovementType.INGRESO && switchAsignarMeta.isChecked()) {
                    if (metaSeleccionadaId == null) {
                        Toast.makeText(getContext(), "Selecciona una meta en el menú desplegable", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    aportarDineroAMetaSilenciosamente(metaSeleccionadaId, importe);
                }

                // Disparar el callback hacia el Fragment padre y cerrar el diálogo
                listener.onGuardar(importe, notaStr, tipo, fechaSeleccionada, categoriaSeleccionadaId);
                dismiss();

            } catch (NumberFormatException e) {
                etImporte.setError("Formato inválido");
            }
        });
    }

    // MÉTODOS DE BACKEND Y LÓGICA DE NEGOCIO

    /**
     * Descarga los presupuestos activos del usuario de forma silenciosa.
     * Se usarán después para validar si el gasto supera algún límite.
     */
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

    /**
     * Descarga todas las metas del usuario para rellenar el selector horizontal.
     */
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

    /**
     * Filtra las metas disponibles para mostrar únicamente aquellas cuya
     * fecha límite es igual o posterior a la fecha seleccionada en el formulario.
     */
    private void filtrarYMostrarMetas() {
        if (metasDisponibles == null || metasDisponibles.isEmpty()) {
            Toast.makeText(getContext(), "No tienes metas activas", Toast.LENGTH_SHORT).show();
            switchAsignarMeta.setChecked(false);
            return;
        }
        List<Meta> metasValidas = new ArrayList<>();

        for (Meta meta : metasDisponibles) {
            if (meta.getFechaLimite().compareTo(fechaSeleccionada) >= 0) {
                metasValidas.add(meta);
            }
        }

        if (metasValidas.isEmpty()) {
            Toast.makeText(getContext(), "Ninguna meta coincide con la fecha del ingreso", Toast.LENGTH_SHORT).show();
            switchAsignarMeta.setChecked(false);
            return;
        }

        MetaSelectorAdapter adapter = new MetaSelectorAdapter(getContext(), metasValidas, meta -> {
            metaSeleccionadaId = meta.getId();
        });
        rvMetasSelector.setAdapter(adapter);
    }

    /**
     * Envía una petición PUT al servidor para sumar el importe directamente a la meta seleccionada.
     * Al ser silenciosa, no bloquea al usuario ni muestra Toasts de carga.
     * @param idMeta ID de la meta destino
     * @param cantidad Cantidad del ingreso
     */
    private void aportarDineroAMetaSilenciosamente(Long idMeta, BigDecimal cantidad) {
        Map<String, BigDecimal> payload = new HashMap<>();
        payload.put("cantidad", cantidad);

        RetrofitClient.getApiService().agregarFondosMeta(idMeta, payload).enqueue(new Callback<Meta>() {
            @Override public void onResponse(Call<Meta> call, Response<Meta> response) { }
            @Override public void onFailure(Call<Meta> call, Throwable t) { }
        });
    }

    /**
     * Descarga y pinta la lista de categorías (Base + Personalizadas).
     * También incluye la lógica de Presupuestos (Semáforo de gastos).
     */
    private void cargarCategoriasDesdeBackend() {
        RetrofitClient.getApiService().obtenerCategorias(sessionManager.getUsuarioId()).enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    categoriaAdapter = new CategoriaAdapter(getContext(), response.body(), categoria -> {

                        if (isModoEdicionCategorias) {
                            // --- MODO EDICIÓN ---
                            if (categoria.getUsuarioId() == null) {
                                Toast.makeText(getContext(), "Las categorías del sistema no se pueden modificar", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            DialogUtils.mostrarDialogoOpciones(
                                    requireContext(),
                                    "Opciones",
                                    new String[]{"Editar", "Eliminar"},
                                    (indice, opcion) -> {
                                        if (indice == 0) abrirEditorCategoria(categoria);
                                        else if (indice == 1) confirmarEliminacion(categoria);
                                        btnEditarToggle.performClick(); // Apagar modo edición
                                    }
                            );
                        } else {
                            // --- MODO SELECCIÓN (Semáforo de Presupuestos) ---
                            categoriaSeleccionadaId = categoria.getId();
                            if (tvAdvertenciaPresupuesto != null) tvAdvertenciaPresupuesto.setVisibility(View.GONE);

                            if (tipo == MovementType.GASTO && presupuestosUsuario != null) {
                                for (Presupuesto p : presupuestosUsuario) {
                                    if (p.getCategoria() != null && p.getCategoria().getId().equals(categoria.getId())) {

                                        BigDecimal gastado = p.getMontoGastado() != null ? p.getMontoGastado() : BigDecimal.ZERO;
                                        BigDecimal limite = p.getMontoLimite();

                                        if (limite.compareTo(BigDecimal.ZERO) > 0) {
                                            int porcentaje = gastado.divide(limite, 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).intValue();

                                            if (porcentaje >= 100) {
                                                // ROJO: Límite Superado
                                                tvAdvertenciaPresupuesto.setText("¡Cuidado! Ya superaste tu límite de " + p.getCategoria().getNombre() + " (" + porcentaje + "%).");
                                                tvAdvertenciaPresupuesto.setTextColor(Color.RED);
                                                tvAdvertenciaPresupuesto.setBackgroundColor(Color.parseColor("#1AFF0000"));
                                                tvAdvertenciaPresupuesto.setVisibility(View.VISIBLE);
                                            } else if (porcentaje >= 75) {
                                                // NARANJA: Alerta de cercanía
                                                tvAdvertenciaPresupuesto.setText("Aviso: Has consumido el " + porcentaje + "% de tu presupuesto en " + p.getCategoria().getNombre() + ".");
                                                tvAdvertenciaPresupuesto.setTextColor(Color.parseColor("#FF9800"));
                                                tvAdvertenciaPresupuesto.setBackgroundColor(Color.parseColor("#1AFFE0B2"));
                                                tvAdvertenciaPresupuesto.setVisibility(View.VISIBLE);
                                            }
                                        }
                                        break;
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

    /**
     * Elimina permanentemente una categoría personalizada.
     * Los movimientos huérfanos se reasignan en el Backend a la categoría general.
     */
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