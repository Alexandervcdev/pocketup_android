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
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.pocketupdm.R;
import com.pocketupdm.adapter.MovimientoAdapter;
import com.pocketupdm.dialogs.MovimientoBottomSheet;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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
    private SearchView searchView;

    private List<MovimientoResponse> todosLosMovimientos = new ArrayList<>();

    // Estados de los filtros
    private String queryBusqueda = "";
    private Long idCategoriaFiltro = null;
    private boolean ordenAscendente = false;

    public HistorialFragment() {}

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

        View headerContainer = view.findViewById(R.id.header_container);
        View searchBarCard = view.findViewById(R.id.cv_search_bar);
        View title = view.findViewById(R.id.tv_historial_title);
        ImageView btnFiltros = view.findViewById(R.id.iv_btn_filtros);
        ImageView btnCloseSearch = view.findViewById(R.id.iv_close_search);
        searchView = view.findViewById(R.id.search_view_movimientos);

        // --- LÓGICA DE TRANSICIÓN ---
        btnFiltros.setOnClickListener(v -> {
            androidx.transition.TransitionManager.beginDelayedTransition((ViewGroup) headerContainer);
            searchBarCard.setVisibility(View.VISIBLE);
            title.setVisibility(View.GONE);
            btnFiltros.setVisibility(View.GONE);
        });

        btnCloseSearch.setOnClickListener(v -> {
            androidx.transition.TransitionManager.beginDelayedTransition((ViewGroup) headerContainer);
            searchBarCard.setVisibility(View.GONE);
            title.setVisibility(View.VISIBLE);
            btnFiltros.setVisibility(View.VISIBLE);

            // Reset de filtros al cerrar
            searchView.setQuery("", false);
            queryBusqueda = "";
            idCategoriaFiltro = null;
            filtrarYMostrarLista();
        });

        // --- LISTENERS DE FILTROS ---
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                queryBusqueda = newText;
                filtrarYMostrarLista();
                return true;
            }
        });

        // Listener del Chip de Orden
        Chip chipOrden = view.findViewById(R.id.chip_orden_fecha);
        chipOrden.setOnClickListener(v -> {
            ordenAscendente = !ordenAscendente;
            chipOrden.setText(ordenAscendente ? "Más antiguos" : "Más recientes");
            filtrarYMostrarLista();
        });

        // Listener del Chip de Categoría
        view.findViewById(R.id.chip_filtro_categoria).setOnClickListener(v -> {
            // Aquí llamarías a un selector de categorías.
            // Por ahora, un ejemplo de reset si se vuelve a pulsar:
            idCategoriaFiltro = null;
            filtrarYMostrarLista();
            Toast.makeText(getContext(), "Filtro de categoría reiniciado", Toast.LENGTH_SHORT).show();
        });

        // 2. Lógica de Pestañas iniciales
        if (getArguments() != null) {
            int tabIndex = getArguments().getInt("TAB_SELECCIONADO", 0);
            TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
            if (tab != null) tab.select();
        }

        // 3. Configurar el Adapter
        adapter = new MovimientoAdapter(getContext(), new ArrayList<>(), new MovimientoAdapter.OnMovimientoOpcionesListener() {
            @Override
            public void onEditar(MovimientoResponse movimiento) {
                abrirBottomSheetEditar(movimiento);
            }

            @Override
            public void onEliminar(MovimientoResponse movimiento) {
                mostrarConfirmacionBorrado(movimiento);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // 4. Carga inicial
        cargarMovimientosDesdeBackend();

        // 5. Cambio de pestaña
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filtrarYMostrarLista();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // --- EL MÉTODO MAESTRO (Filtra todo a la vez) ---
    private void filtrarYMostrarLista() {
        List<MovimientoResponse> listaFiltrada = new ArrayList<>();
        int tabPosition = tabLayout.getSelectedTabPosition();
        MovementType tipoBuscado = (tabPosition == 0) ? MovementType.INGRESO : MovementType.GASTO;

        for (MovimientoResponse m : todosLosMovimientos) {
            // 1. Filtro Pestaña
            if (m.getTipo() != tipoBuscado) continue;

            // 2. Filtro Buscador
            if (!queryBusqueda.isEmpty() && !m.getNombre().toLowerCase().contains(queryBusqueda.toLowerCase())) continue;

            // 3. Filtro Categoría
            if (idCategoriaFiltro != null && (m.getCategoria() == null || !m.getCategoria().getId().equals(idCategoriaFiltro))) continue;

            listaFiltrada.add(m);
        }

        // 4. Lógica de Ordenación
        Collections.sort(listaFiltrada, (m1, m2) -> {
            int comp = m1.getFecha().compareTo(m2.getFecha());
            if (comp == 0) comp = m1.getId().compareTo(m2.getId());
            return ordenAscendente ? comp : -comp;
        });

        adapter.setMovimientos(listaFiltrada);
        tvEmptyState.setVisibility(listaFiltrada.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(listaFiltrada.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void abrirBottomSheetEditar(MovimientoResponse mov) {
        MovimientoBottomSheet bottomSheet = new MovimientoBottomSheet(mov.getTipo(), (nombre, importe, nota, tipoMovimiento, fecha, categoriaId) -> {
            actualizarMovimientoEnBackend(mov.getId(), nombre, importe, nota, tipoMovimiento, fecha, categoriaId);
        });
        bottomSheet.setMovimientoAEditar(mov);
        bottomSheet.show(getParentFragmentManager(), "EditarMovimientoHistorial");
    }

    private void actualizarMovimientoEnBackend(Long id, String nombre, BigDecimal importe, String nota, MovementType tipo, String fecha, Long categoriaId) {
        MovimientoRequest request = new MovimientoRequest(nombre, importe, fecha, tipo, nota, sessionManager.getUsuarioId(), categoriaId);
        RetrofitClient.getApiService().editarMovimiento(id, request).enqueue(new Callback<MovimientoResponse>() {
            @Override
            public void onResponse(Call<MovimientoResponse> call, Response<MovimientoResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Actualizado correctamente", Toast.LENGTH_SHORT).show();
                    cargarMovimientosDesdeBackend();
                }
            }
            @Override public void onFailure(Call<MovimientoResponse> call, Throwable t) {}
        });
    }

    private void mostrarConfirmacionBorrado(MovimientoResponse movimiento) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.layout_dialog_confirm);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_dialog_action);

        tvTitle.setText("Eliminar Movimiento");
        tvMessage.setText("¿Seguro que quieres eliminar este movimiento de " + movimiento.getImporte() + " €?");
        btnConfirm.setText("Eliminar");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            ejecutarBorradoEnBackend(movimiento.getId());
        });
        dialog.show();
    }

    private void ejecutarBorradoEnBackend(Long id) {
        List<Long> idsABorrar = Collections.singletonList(id);
        RetrofitClient.getApiService().deleteMovements(idsABorrar).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    cargarMovimientosDesdeBackend();
                    Toast.makeText(getContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void cargarMovimientosDesdeBackend() {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        RetrofitClient.getApiService().obtenerMovimientos(usuarioId).enqueue(new Callback<List<MovimientoResponse>>() {
            @Override
            public void onResponse(Call<List<MovimientoResponse>> call, Response<List<MovimientoResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    todosLosMovimientos = response.body();
                    filtrarYMostrarLista(); // Llama al método maestro
                }
            }
            @Override public void onFailure(Call<List<MovimientoResponse>> call, Throwable t) {
                Log.e("HISTORIAL", "Fallo de red", t);
            }
        });
    }
}