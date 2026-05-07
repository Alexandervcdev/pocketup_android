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
import com.pocketupdm.dialogs.AportarMetaBottomSheet;
import com.pocketupdm.dialogs.NuevaMetaBottomSheet;
import com.pocketupdm.model.Meta;
import com.pocketupdm.model.Presupuesto;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.DialogUtils;
import com.pocketupdm.utils.NotificacionUI;
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

    private com.google.android.material.button.MaterialButton fabAgregar;

    private View layoutEmpty;
    private android.widget.ImageView ivEmpty;
    private android.widget.TextView tvEmptyTitulo, tvEmptySub;


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
        layoutEmpty = view.findViewById(R.id.layout_empty_planificacion);
        ivEmpty = view.findViewById(R.id.iv_empty_planificacion);
        tvEmptyTitulo = view.findViewById(R.id.tv_empty_titulo_planificacion);
        tvEmptySub = view.findViewById(R.id.tv_empty_sub_planificacion);

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
                    fabAgregar.setIconResource(R.drawable.ic_wallet);
                } else {
                    mostrarMetas();
                    fabAgregar.setText("Nueva Meta"); // Cambia el texto del botón
                    fabAgregar.setIconResource(R.drawable.ic_pig);
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });


        // Por defecto
        mostrarPresupuestos();
        fabAgregar.setText("Nuevo Presupuesto");
        fabAgregar.setIconResource(R.drawable.ic_wallet);

    }

    // ==========================================
    // LÓGICA DE PRESUPUESTOS
    // ==========================================

    private void mostrarPresupuestos() {
        Long usuarioId = sessionManager.getUsuarioId();

        RetrofitClient.getApiService().obtenerPresupuestos(usuarioId).enqueue(new Callback<List<Presupuesto>>() {
            @Override
            public void onResponse(Call<List<Presupuesto>> call, Response<List<Presupuesto>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Presupuesto> presupuestos = response.body();
                    // Verificamos si está vacío
                    verificarEstadoVacio(presupuestos.isEmpty(), true);
                    configurarAdaptadorPresupuestos(presupuestos);
                } else {
                    // --- AÑADE ESTO PARA DEBUG ---
                    int codigo = response.code();
                    android.util.Log.e("API_ERROR", "Error al cargar presupuestos. Código: " + codigo);
                    try {
                        String errorMsg = response.errorBody().string();
                        android.util.Log.e("API_ERROR", "Mensaje del servidor: " + errorMsg);
                    } catch (Exception e) {}

                    Toast.makeText(getContext(), "Error " + codigo + ": No se pudo cargar", Toast.LENGTH_SHORT).show();
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
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Meta> metas = response.body();
                    // --- LÓGICA DE ORDENACIÓN ---
                    // Usamos sort para poner las NO completadas arriba
                    metas.sort((m1, m2) -> {
                        boolean m1Completada = m1.getMontoActual().compareTo(m1.getMontoObjetivo()) >= 0;
                        boolean m2Completada = m2.getMontoActual().compareTo(m2.getMontoObjetivo()) >= 0;
                        // Boolean.compare devuelve 0 si son iguales, negativo si el primero es false y el segundo true
                        // Esto pondrá los "false" (no completadas) al principio.
                        return Boolean.compare(m1Completada, m2Completada);
                    });
                    verificarEstadoVacio(metas.isEmpty(), false);
                    configurarAdaptadorMetas(metas);
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
                NuevaMetaBottomSheet bottomSheet = new NuevaMetaBottomSheet();
                bottomSheet.setMetaAEditar(meta); // Le pasamos la meta que queremos cambiar
                bottomSheet.setListener(() -> mostrarMetas()); // Para que refresque al terminar
                bottomSheet.show(getParentFragmentManager(), "EditarMeta");
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
        AportarMetaBottomSheet bottomSheet = new AportarMetaBottomSheet();
        bottomSheet.setMeta(meta);
        bottomSheet.setOnSuccess(() -> mostrarMetas());
        bottomSheet.show(getParentFragmentManager(), "AportarMeta");
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

    private void verificarEstadoVacio(boolean estaVacio, boolean esPresupuesto) {
        if (estaVacio) {
            recyclerView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);

            if (esPresupuesto) {
                ivEmpty.setImageResource(R.drawable.ic_wallet); // Usa un icono de billetera o moneda
                tvEmptyTitulo.setText("Aún no tienes presupuestos");
                tvEmptySub.setText("¡No dejes que tu dinero se escape!\nCrea un límite de gastos y empieza a ahorrar hoy mismo.");
            } else {
                ivEmpty.setImageResource(R.drawable.ic_pig); // Usa un icono de alcancía o billete
                tvEmptyTitulo.setText("Tus metas están vacías");
                tvEmptySub.setText("¿Un viaje? ¿Un coche nuevo? ¿Ahorros?\nDefine tu objetivo y nosotros te ayudaremos a alcanzarlo.");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }


}