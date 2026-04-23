package com.pocketupdm.fragment;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.pocketupdm.R;
import com.pocketupdm.adapter.MetaAdapter;
import com.pocketupdm.adapter.PresupuestoAdapter;
import com.pocketupdm.dialogs.NuevaMetaBottomSheet;
import com.pocketupdm.model.Meta;
import com.pocketupdm.model.Presupuesto;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.DialogUtils;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlanificacionFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SessionManager sessionManager;

    private MetaAdapter metaAdapter;
    private PresupuestoAdapter presupuestoAdapter; // <-- NUEVO

    private ExtendedFloatingActionButton fabAgregar;


    public PlanificacionFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_planificacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        tabLayout = view.findViewById(R.id.tab_layout_planificacion);
        recyclerView = view.findViewById(R.id.rv_planificacion);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fabAgregar = view.findViewById(R.id.fab_agregar_plan);

        // ✅ ¡AQUÍ ESTÁ LA MAGIA! Se abre solo cuando tocas el botón
        fabAgregar.setOnClickListener(v -> {
            if (tabLayout.getSelectedTabPosition() == 1) { // Si estamos en la pestaña Metas
                NuevaMetaBottomSheet bottomSheet = new NuevaMetaBottomSheet();
                bottomSheet.setListener(() -> mostrarMetas());
                bottomSheet.show(getParentFragmentManager(), "NuevaMeta");
            } else {
                // PESTAÑA PRESUPUESTOS
                com.pocketupdm.dialogs.NuevoPresupuestoBottomSheet bottomSheet = new com.pocketupdm.dialogs.NuevoPresupuestoBottomSheet();
                bottomSheet.setListener(() -> mostrarPresupuestos());
                bottomSheet.show(getParentFragmentManager(), "NuevoPresupuesto");
            }
        });

        // Lógica para esconder el botón si estamos en presupuestos (por ahora)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    mostrarPresupuestos();
                    fabAgregar.setText("Nuevo Presupuesto"); // Cambia el texto del botón
                } else {
                    mostrarMetas();
                    fabAgregar.setText("Nueva Meta"); // Cambia el texto del botón
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });


        // Por defecto
        mostrarPresupuestos();
    }

    // ==========================================
    // LÓGICA DE PRESUPUESTOS
    // ==========================================

    private void mostrarPresupuestos() {
        Long usuarioId = sessionManager.getUsuarioId();

        RetrofitClient.getApiService().obtenerPresupuestos(usuarioId).enqueue(new Callback<List<Presupuesto>>() {
            @Override
            public void onResponse(Call<List<Presupuesto>> call, Response<List<Presupuesto>> response) {
                if (!isAdded() || getContext() == null) return; // Escudo protector

                if (response.isSuccessful() && response.body() != null) {
                    List<Presupuesto> presupuestos = response.body();
                    configurarAdaptadorPresupuestos(presupuestos);
                } else {
                    Toast.makeText(getContext(), "Error al cargar presupuestos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Presupuesto>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error de red al cargar presupuestos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarAdaptadorPresupuestos(List<Presupuesto> presupuestos) {
        presupuestoAdapter = new com.pocketupdm.adapter.PresupuestoAdapter(getContext(), presupuestos, new com.pocketupdm.adapter.PresupuestoAdapter.OnPresupuestoOpcionesListener() {
            @Override
            public void onEditar(Presupuesto presupuesto) {
                // Ya tenemos la lógica de edición lista en el BottomSheet
                com.pocketupdm.dialogs.NuevoPresupuestoBottomSheet bottomSheet = new com.pocketupdm.dialogs.NuevoPresupuestoBottomSheet();
                bottomSheet.setPresupuestoAEditar(presupuesto);
                bottomSheet.setListener(() -> mostrarPresupuestos());
                bottomSheet.show(getParentFragmentManager(), "EditarPresupuesto");
            }

            @Override
            public void onEliminar(Presupuesto presupuesto) {
                DialogUtils.mostrarDialogoConfirmacion(
                        requireContext(),
                        "Eliminar Presupuesto",
                        "¿Seguro que deseas eliminar este límite de gasto? (Tus movimientos reales no se borrarán).",
                        "Eliminar",
                        R.color.black_pu,
                        R.color.red,
                        () -> eliminarPresupuestoEnBackend(presupuesto.getId())
                );
            }
        });
        recyclerView.setAdapter(presupuestoAdapter);
    }

    private void eliminarPresupuestoEnBackend(Long id) {
        RetrofitClient.getApiService().eliminarPresupuesto(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Presupuesto eliminado", Toast.LENGTH_SHORT).show();
                    mostrarPresupuestos(); // Recargar la lista
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // LÓGICA DE METAS DE AHORRO
    // ==========================================
    private void mostrarMetas() {
        Long usuarioId = sessionManager.getUsuarioId();

        RetrofitClient.getApiService().obtenerMetas(usuarioId).enqueue(new Callback<List<Meta>>() {
            @Override
            public void onResponse(Call<List<Meta>> call, Response<List<Meta>> response) {
                if (!isAdded() || getContext() == null) return; // Escudo

                if (response.isSuccessful() && response.body() != null) {
                    List<Meta> metas = response.body();
                    configurarAdaptadorMetas(metas);
                } else {
                    Toast.makeText(getContext(), "Error al cargar metas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Meta>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarAdaptadorMetas(List<Meta> metas) {
        metaAdapter = new MetaAdapter(getContext(), metas, new MetaAdapter.OnMetaOpcionesListener() {
            @Override
            public void onAportar(Meta meta) {
                dialogoAportarDinero(meta);
            }

            @Override
            public void onEditar(Meta meta) {
                // TODO: Abriremos un BottomSheet para editar (lo haremos en el siguiente paso)
                Toast.makeText(getContext(), "Editar meta en construcción...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEliminar(Meta meta) {
                DialogUtils.mostrarDialogoConfirmacion(
                        requireContext(),
                        "Eliminar Meta",
                        "¿Estás seguro de que deseas eliminar la meta '" + meta.getNombre() + "'? Esta acción no se puede deshacer.",
                        "Eliminar",
                        R.color.black_pu,
                        R.color.red,
                        () -> eliminarMetaEnBackend(meta.getId())
                );
            }
        });
        recyclerView.setAdapter(metaAdapter);
    }

    // --- LÓGICA PARA AÑADIR DINERO A LA HUCHA ---
    private void dialogoAportarDinero(Meta meta) {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Ej: 50.00");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Aportar a " + meta.getNombre())
                .setMessage("¿Cuánto dinero deseas añadir a esta meta?")
                .setView(input)
                .setPositiveButton("Añadir", (dialog, which) -> {
                    String cantidadStr = input.getText().toString().trim();
                    if (!cantidadStr.isEmpty()) {
                        try {
                            BigDecimal cantidad = new BigDecimal(cantidadStr.replace(",", "."));
                            aportarDineroEnBackend(meta.getId(), cantidad);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Cantidad no válida", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void aportarDineroEnBackend(Long metaId, BigDecimal cantidad) {
        Map<String, BigDecimal> payload = new HashMap<>();
        payload.put("cantidad", cantidad);

        RetrofitClient.getApiService().agregarFondosMeta(metaId, payload).enqueue(new Callback<Meta>() {
            @Override
            public void onResponse(Call<Meta> call, Response<Meta> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "¡Fondos añadidos con éxito!", Toast.LENGTH_SHORT).show();
                    mostrarMetas(); // Recargamos la lista para ver la barra de progreso avanzar
                }
            }

            @Override
            public void onFailure(Call<Meta> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error al aportar fondos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarMetaEnBackend(Long id) {
        RetrofitClient.getApiService().eliminarMeta(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Meta eliminada", Toast.LENGTH_SHORT).show();
                    mostrarMetas(); // Recargamos la lista
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }


}