package com.pocketupdm.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.pocketupdm.R;
import com.pocketupdm.adapter.MovimientoAdapter;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections; // Necesario para envolver 1 solo ID en una lista
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovimientoAdapter adapter;
    private TabLayout tabLayout;
    private TextView tvEmptyState;
    private SessionManager sessionManager;

    // Guardaremos TODOS los movimientos que lleguen del servidor aquí
    private List<MovimientoResponse> todosLosMovimientos = new ArrayList<>();

    public HistorialFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        // 1. Vincular vistas
        recyclerView = view.findViewById(R.id.rv_movimientos);
        tabLayout = view.findViewById(R.id.tab_layout_movimientos);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        // 2. Lógica de Pestañas (Arguments)
        if (getArguments() != null) {
            int tabIndex = getArguments().getInt("TAB_SELECCIONADO", 0);
            TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
            if (tab != null) {
                tab.select();
            }
        }

        // 3. NUEVO: Configurar el Adapter con el Listener del Menú (3 puntos)
        adapter = new MovimientoAdapter(getContext(), new ArrayList<>(), new MovimientoAdapter.OnMovimientoOpcionesListener() {
            @Override
            public void onEditar(MovimientoResponse movimiento) {
                // Dejamos un aviso temporal hasta que implementemos la pantalla de edición
                Toast.makeText(getContext(), "Opción Editar en construcción...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEliminar(MovimientoResponse movimiento) {
                // Llamamos al diálogo pasándole exactamente el movimiento que queremos borrar
                mostrarConfirmacionBorrado(movimiento);
            }
        });

        // 4. Configurar el RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // 5. Descargar datos del servidor
        cargarMovimientosDesdeBackend();

        // 6. Configurar el clic en las pestañas para filtrar
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filtrarYMostrarLista(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // MODIFICADO: Ahora recibe el objeto completo para poder mostrar su importe en el mensaje
    private void mostrarConfirmacionBorrado(MovimientoResponse movimiento) {
        // 1. Preparamos el Diálogo Customizado
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.layout_dialog_confirm);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 2. Vinculamos las vistas del diálogo
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_dialog_action);

        // 3. Personalizamos los textos dinámicamente
        tvTitle.setText("Eliminar Movimiento");
        tvMessage.setText("¿Estás seguro de que quieres eliminar este movimiento por valor de " + movimiento.getImporte() + "? Esta acción no se puede deshacer.");
        btnConfirm.setText("Eliminar");

        // 4. Lógica de los botones
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            ejecutarBorradoEnBackend(movimiento.getId()); // Llamamos al servidor con un solo ID
        });

        dialog.show();
    }

    // MODIFICADO: Recibe un Long (1 ID) en lugar de una List<Long>
    private void ejecutarBorradoEnBackend(Long id) {
        // Como el backend sigue esperando una lista (por si acaso), metemos nuestro único ID en una lista de 1 elemento.
        List<Long> idsABorrar = Collections.singletonList(id);

        RetrofitClient.getApiService().deleteMovements(idsABorrar).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful()) {
                    // Éxito: Simplemente recargamos la lista
                    cargarMovimientosDesdeBackend();
                    Toast.makeText(getContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e("BORRADO", "Código: " + response.code() + " | Error: " + errorBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), "Error al eliminar el movimiento", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarMovimientosDesdeBackend() {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        RetrofitClient.getApiService().obtenerMovimientos(usuarioId).enqueue(new Callback<List<MovimientoResponse>>() {
            @Override
            public void onResponse(Call<List<MovimientoResponse>> call, Response<List<MovimientoResponse>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    todosLosMovimientos = response.body();
                    filtrarYMostrarLista(tabLayout.getSelectedTabPosition());
                } else {
                    Toast.makeText(getContext(), "Error al cargar historial", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MovimientoResponse>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Log.e("HISTORIAL", "Error de red", t);
                Toast.makeText(getContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarYMostrarLista(int tabPosition) {
        List<MovimientoResponse> listaFiltrada = new ArrayList<>();

        MovementType tipoBuscado = (tabPosition == 0) ? MovementType.INGRESO : MovementType.GASTO;

        for (MovimientoResponse m : todosLosMovimientos) {
            if (m.getTipo() == tipoBuscado) {
                listaFiltrada.add(m);
            }
        }

        adapter.setMovimientos(listaFiltrada);

        if (listaFiltrada.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}