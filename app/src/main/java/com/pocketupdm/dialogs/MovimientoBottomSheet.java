package com.pocketupdm.dialogs;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.textfield.TextInputEditText;
import com.pocketupdm.R;
import com.pocketupdm.adapter.CategoriaAdapter;
import com.pocketupdm.model.Categoria;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.DialogUtils;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovimientoBottomSheet extends BottomSheetDialogFragment {

    private MovementType tipo;
    private OnMovimientoGuardadoListener listener;
    private String fechaSeleccionada; // Formato YYYY-MM-DD para la API

    // NUEVAS VARIABLES PARA CATEGORÍAS
    private RecyclerView rvCategorias;
    private CategoriaAdapter categoriaAdapter;
    private Long categoriaSeleccionadaId = null;
    private SessionManager sessionManager;

    // Interfaz para avisarle al HomeFragment que se guardó un dato
    public interface OnMovimientoGuardadoListener {
        void onGuardar(BigDecimal importe, String nota, MovementType tipo,String fecha,Long categoriaId);
    }

    // Constructor para pasarle el tipo (INGRESO o GASTO) y el listener
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
        rvCategorias = view.findViewById(R.id.rv_categorias_selector);
        rvCategorias.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 2. Traer las categorías del servidor
        cargarCategoriasDesdeBackend();

        MaterialButton btnNuevaCategoria = view.findViewById(R.id.btn_nueva_categoria);
        btnNuevaCategoria.setOnClickListener(v -> {
            // Instanciamos el nuevo BottomSheet
            NuevaCategoriaBottomSheet bottomSheet = new NuevaCategoriaBottomSheet();

            // Le ponemos el audífono al BottomSheet
            bottomSheet.setListener(() -> {
                // Cuando escuchemos que ha terminado, recargamos las categorías
                cargarCategoriasDesdeBackend();
            });

            // Lo mostramos en pantalla
            bottomSheet.show(getParentFragmentManager(), "NuevaCategoria");
        });

        TextView tvTitulo = view.findViewById(R.id.tv_titulo_sheet);
        TextInputEditText etImporte = view.findViewById(R.id.et_importe);
        TextInputEditText etFecha = view.findViewById(R.id.et_fecha);
        TextInputEditText etNota = view.findViewById(R.id.et_nota);
        MaterialButton btnGuardar = view.findViewById(R.id.btn_guardar_movimiento);

        // 1. Configurar fecha por defecto (Hoy)
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfVisual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        fechaSeleccionada = sdfApi.format(calendar.getTime());
        etFecha.setText(sdfVisual.format(calendar.getTime()));
        // 2. Configurar el clic para abrir el calendario
        etFecha.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Selecciona una fecha")
                    .setTheme(R.style.Theme_App_Calendar_Turquesa)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(selection);

                // Guardamos el formato para la API y mostramos el formato visual
                fechaSeleccionada = sdfApi.format(utc.getTime());
                etFecha.setText(sdfVisual.format(utc.getTime()));
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        // ¡MAGIA VISUAL! Cambiamos los colores según el tipo de movimiento
        if (tipo == MovementType.INGRESO) {
            tvTitulo.setText("Nuevo Ingreso");
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro));
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro)));
        } else {
            tvTitulo.setText("Nuevo Gasto");
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red)));
        }

        btnGuardar.setOnClickListener(v -> {
            String importeStr = etImporte.getText().toString().trim();
            String notaStr = etNota.getText().toString().trim();

            if (importeStr.isEmpty()) {
                etImporte.setError("El importe es obligatorio");
                return;
            }
            try {
                importeStr = importeStr.replace(",", ".");
                BigDecimal importe = new BigDecimal(importeStr);
                if (importe.compareTo(BigDecimal.ZERO) <= 0) {
                    etImporte.setError("El importe debe ser mayor a 0");
                    return;
                }

                // 4. Validar que no exceda el límite de la base de datos (99 millones)
                if (importe.compareTo(new BigDecimal("99999999.99")) > 0) {
                    etImporte.setError("¡Vaya! Esa cantidad es demasiado grande");
                    return;
                }

                if (notaStr.isEmpty()) {
                    notaStr = "Sin nota";
                }

                // NUEVA VALIDACIÓN: Obligar a elegir categoría
                if (categoriaSeleccionadaId == null) {
                    Toast.makeText(getContext(), "Por favor, selecciona una categoría", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Si sobrevive a todas las validaciones, lo enviamos
                listener.onGuardar(importe, notaStr, tipo, fechaSeleccionada,categoriaSeleccionadaId);
                dismiss();

            } catch (NumberFormatException e) {
                // Si el usuario metió caracteres extraños ("50..5")
                etImporte.setError("Formato de número inválido");
            }
        });
    }
    private void cargarCategoriasDesdeBackend() {
        Long usuarioId = sessionManager.getUsuarioId();

        RetrofitClient.getApiService().obtenerCategorias(usuarioId).enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Categoria> categorias = response.body();

                    // Instanciamos el adaptador con LOS DOS listeners
                    categoriaAdapter = new CategoriaAdapter(getContext(), categorias,
                            // 1. Acción de Click Normal (Seleccionar)
                            categoria -> {
                                categoriaSeleccionadaId = categoria.getId();
                            },
                                // 2. CLICK LARGO: Menú de opciones
                            categoria -> {
                                android.util.Log.d("DEBUG_POCKET", "Categoría: " + categoria.getNombre() + " | ID Usuario: " + categoria.getUsuarioId());
                                // SEGURIDAD: Si es una categoría del sistema (usuario == null), no dejamos tocarla
                                if (categoria.getUsuarioId() == null) {
                                    Toast.makeText(getContext(), "Las categorías del sistema no se pueden modificar", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Definimos las opciones
                                String[] opciones = {"Editar", "Eliminar"};

                                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(categoria.getNombre()) // El título es el nombre de la categoría
                                        .setItems(opciones, (dialog, which) -> {
                                            if (which == 0) {
                                                // OPCIÓN: EDITAR (RF 5.3)
                                                abrirEditorCategoria(categoria);
                                            } else if (which == 1) {
                                                // OPCIÓN: ELIMINAR (RF 5.4)
                                                confirmarEliminacion(categoria);
                                            }
                                        })
                                        .show();
                            });
                    rvCategorias.setAdapter(categoriaAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<Categoria>> call, Throwable t) {
                Toast.makeText(getContext(), "Error al cargar categorías", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarCategoriaEnBackend(Long idCategoria) {
        RetrofitClient.getApiService().eliminarCategoria(idCategoria).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show();
                    // Refrescamos la lista para que desaparezca la bolita
                    cargarCategoriasDesdeBackend();
                } else {
                    Toast.makeText(getContext(), "No se puede eliminar la categoría del sistema", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Lógica para lanzar el editor que ya preparamos
    private void abrirEditorCategoria(Categoria categoria) {
        NuevaCategoriaBottomSheet editSheet = new NuevaCategoriaBottomSheet();
        editSheet.setCategoriaAEditar(categoria); // Le pasamos la categoría elegida
        editSheet.setListener(this::cargarCategoriasDesdeBackend); // Recargar al terminar
        editSheet.show(getParentFragmentManager(), "EditarCategoria");
    }

    // Lógica para confirmar el borrado usando tu nueva clase Utils
    private void confirmarEliminacion(Categoria categoria) {
        com.pocketupdm.utils.DialogUtils.mostrarDialogoConfirmacion(
                requireContext(),
                "Eliminar Categoría",
                "¿Estás seguro de que quieres eliminar '" + categoria.getNombre() + "'? Sus movimientos se reasignarán a 'General'.",
                "Eliminar",
                R.color.black_pu, // Color para cancelar (tuyo de colors.xml)
                R.color.red,      // Color para la acción de borrar
                () -> eliminarCategoriaEnBackend(categoria.getId()) // La acción de borrado real
        );
    }
}
