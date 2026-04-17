package com.pocketupdm.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private ImageView ivDeleteBatch;

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
        ivDeleteBatch = view.findViewById(R.id.iv_delete_batch);

        // 2. Lógica de Pestañas (Arguments)
        if (getArguments() != null) {
            int tabIndex = getArguments().getInt("TAB_SELECCIONADO", 0);
            TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
            if (tab != null) {
                tab.select();
            }
        }

        // 3. Configurar el Adapter con el Listener de Selección
        adapter = new MovimientoAdapter(getContext(), new ArrayList<>(), count -> {
            // Si count > 0, mostramos la basura. Si no, la ocultamos.
            ivDeleteBatch.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        });

        // 4. Configurar el RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // 5. Clic en la basura
        ivDeleteBatch.setOnClickListener(v -> mostrarConfirmacionBorrado());

        // 6. Descargar datos del servidor
        cargarMovimientosDesdeBackend();

        // 7. Configurar el clic en las pestañas para filtrar
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

    private void mostrarConfirmacionBorrado() {
        // 1. Obtenemos cuántos y cuáles IDs vamos a borrar
        List<Long> idsSeleccionados = adapter.getSelectedIds();
        int cantidad = idsSeleccionados.size();

        if (cantidad == 0) return; // Por seguridad

        // 2. Preparamos el Diálogo Customizado (el mismo del modo oscuro)
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.layout_dialog_confirm);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 3. Vinculamos las vistas del diálogo
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_dialog_action);

        // 4. Personalizamos los textos dinámicamente
        tvTitle.setText("Eliminar Movimientos");
        tvMessage.setText("¿Estás seguro de que quieres eliminar " + cantidad + " movimiento(s)? Esta acción no se puede deshacer.");
        btnConfirm.setText("Eliminar");

        // 5. Lógica de los botones
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            ejecutarBorradoEnBackend(idsSeleccionados); // Llamamos al servidor
        });

        dialog.show();
    }
    private void ejecutarBorradoEnBackend(List<Long> ids) {
        RetrofitClient.getApiService().deleteMovements(ids).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful()) {
                    // Éxito: Salimos del modo selección, ocultamos basura y recargamos
                    adapter.exitSelectionMode();
                    ivDeleteBatch.setVisibility(View.GONE);
                    cargarMovimientosDesdeBackend();
                    Toast.makeText(getContext(), "Movimientos eliminados", Toast.LENGTH_SHORT).show();
                } else {
                    // AQUÍ ESTÁ LA MEJORA: Imprimimos el error exacto del servidor en la consola
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e("BORRADO_BATCH", "Código: " + response.code() + " | Error: " + errorBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), "Error al eliminar movimientos", Toast.LENGTH_SHORT).show();
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
                    // Guardamos todo en la lista maestra
                    todosLosMovimientos = response.body();

                    // Como por defecto está seleccionada la pestaña 0 (Ingresos), filtramos para esa
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

    // Método estrella: Separa la lista maestra en Ingresos o Gastos
    private void filtrarYMostrarLista(int tabPosition) {
        List<MovimientoResponse> listaFiltrada = new ArrayList<>();

        // 0 es Ingreso, 1 es Gasto
        MovementType tipoBuscado = (tabPosition == 0) ? MovementType.INGRESO : MovementType.GASTO;

        // Recorremos todos los movimientos y nos quedamos solo con los que coinciden
        for (MovimientoResponse m : todosLosMovimientos) {
            if (m.getTipo() == tipoBuscado) {
                listaFiltrada.add(m);
            }
        }

        // Le pasamos la lista recortada al adaptador para que la dibuje
        adapter.setMovimientos(listaFiltrada);

        // Controlar si mostramos el texto de "Vacío"
        if (listaFiltrada.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}