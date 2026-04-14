package com.pocketupdm.fragment;

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

import com.google.android.material.tabs.TabLayout;
import com.pocketupdm.R;
import com.pocketupdm.adapter.MovimientoAdapter;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

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
        // Constructor vacío requerido
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

        if (getArguments() != null) {
            // 2. Sacamos el número de la cajita (si por algún error no hay número, usa el 0 por defecto)
            int tabIndex = getArguments().getInt("TAB_SELECCIONADO", 0);

            // 3. Le decimos al TabLayout que seleccione esa pestaña visualmente
            TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
            if (tab != null) {
                tab.select();
            }
        }

        // 2. Configurar el RecyclerView (La lista)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MovimientoAdapter(getContext(), new ArrayList<>()); // Empezamos con lista vacía
        recyclerView.setAdapter(adapter);

        // 3. Descargar datos del servidor
        cargarMovimientosDesdeBackend();

        // 4. Configurar el clic en las pestañas
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Si tab.getPosition() es 0 -> Ingresos. Si es 1 -> Gastos.
                filtrarYMostrarLista(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
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