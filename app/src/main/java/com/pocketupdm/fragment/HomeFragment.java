package com.pocketupdm.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.pocketupdm.R;
import com.pocketupdm.adapter.MovimientoAdapter;
import com.pocketupdm.dialogs.MovimientoBottomSheet;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // Variables globales
    private TextView tvFecha, tvSaldo;
    private SessionManager sessionManager;

    // Variables para la lista de recientes
    private RecyclerView rvTopMovimientos;
    private MovimientoAdapter adapter;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        // 1. Vincular Vistas de la Tarjeta de Balance
        tvFecha = view.findViewById(R.id.tv_home_fecha);
        tvSaldo = view.findViewById(R.id.tv_home_saldo);

        // 2. Mensaje de bienvenida
        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        String nombreUsuario = sessionManager.getUsuarioNombre();
        tvWelcome.setText("¡Hola, " + nombreUsuario + "!");

        // 3. Subtítulo dinámico con el mes actual
        TextView tvSubtitle = view.findViewById(R.id.tv_subtitle);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
        String mesActual = sdfMes.format(calendar.getTime());
        mesActual = mesActual.substring(0, 1).toUpperCase() + mesActual.substring(1);
        tvSubtitle.setText("Este es tu progreso de " + mesActual);

        // 4. Configurar Lista de Movimientos Recientes (Top 3)
        rvTopMovimientos = view.findViewById(R.id.rv_top_movimientos);
        rvTopMovimientos.setLayoutManager(new LinearLayoutManager(getContext()));
        // Le pasamos 'null' al listener porque aquí NO queremos seleccionar para borrar
        adapter = new MovimientoAdapter(getContext(), new ArrayList<>(), null);
        rvTopMovimientos.setAdapter(adapter);

        // 5. Configurar el Menú Flotante (Botón "+")
        MaterialButton btnMenu = view.findViewById(R.id.btn_menu_movimientos);
        btnMenu.setOnClickListener(v -> mostrarMenuOpciones());

        // 6. ¡VITAL! Llamamos al servidor para cargar los datos al entrar
        cargarDatosHome();

    }

    private void cargarDatosHome() {
        Long usuarioId = sessionManager.getUsuarioId();

        // Ponemos la fecha actual en la esquina de la tarjeta
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", new Locale("es", "ES"));
        tvFecha.setText(sdf.format(new Date()));

        // Pedimos los movimientos al Backend
        RetrofitClient.getApiService().obtenerMovimientos(usuarioId).enqueue(new Callback<List<MovimientoResponse>>() {
            @Override
            public void onResponse(Call<List<MovimientoResponse>> call, Response<List<MovimientoResponse>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<MovimientoResponse> todosLosMovimientos = response.body();

                    // 1. Calculamos el saldo con TODOS los movimientos
                    calcularYMostrarSaldo(todosLosMovimientos);

                    Collections.sort(todosLosMovimientos, (m1, m2) -> m2.getId().compareTo(m1.getId()));

                    // 2. Extraemos solo los 3 últimos creando una NUEVA lista real en memoria
                    int limite = Math.min(todosLosMovimientos.size(), 3);
                    List<MovimientoResponse> top3 = new ArrayList<>(todosLosMovimientos.subList(0, limite));

                    // 3. Le pasamos la lista al adapter (el adapter ya hace el notifyDataSetChanged por dentro)
                    adapter.setMovimientos(top3);
                }
            }

            @Override
            public void onFailure(Call<List<MovimientoResponse>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Log.e("HOME", "Error al traer datos", t);
            }
        });
    }

    private void calcularYMostrarSaldo(List<MovimientoResponse> movimientos) {
        BigDecimal saldoTotal = BigDecimal.ZERO;

        for (MovimientoResponse m : movimientos) {
            if (m.getTipo() == MovementType.INGRESO) {
                saldoTotal = saldoTotal.add(m.getImporte());
            } else {
                saldoTotal = saldoTotal.subtract(m.getImporte());
            }
        }

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        tvSaldo.setText(formatoMoneda.format(saldoTotal));
    }

    private void mostrarMenuOpciones() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_menu, null);
        bottomSheetDialog.setContentView(view);

        view.findViewById(R.id.btn_opcion_ingreso).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            abrirBottomSheetMovimiento(MovementType.INGRESO);
        });

        view.findViewById(R.id.btn_opcion_gasto).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            abrirBottomSheetMovimiento(MovementType.GASTO);
        });

        view.findViewById(R.id.btn_opcion_ver_ingresos).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Bundle bundle = new Bundle();
            bundle.putInt("TAB_SELECCIONADO", 0);
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_home_to_nav_historial, bundle);
        });

        view.findViewById(R.id.btn_opcion_ver_gastos).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Bundle bundle = new Bundle();
            bundle.putInt("TAB_SELECCIONADO", 1);
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_home_to_nav_historial, bundle);
        });

        bottomSheetDialog.show();
    }

    private void abrirBottomSheetMovimiento(MovementType tipo) {
        MovimientoBottomSheet bottomSheet = new MovimientoBottomSheet(tipo, (importe, nota, tipoMovimiento, fecha) ->
                enviarMovimientoAlBackend(importe, nota, tipoMovimiento, fecha)
        );
        bottomSheet.show(getParentFragmentManager(), "MovimientoBottomSheet");
    }

    private void enviarMovimientoAlBackend(BigDecimal importe, String nota, MovementType tipo, String fecha) {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) {
            Toast.makeText(getContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            com.pocketupdm.utils.NavigationUtil.irALogin(getActivity());
            return;
        }

        MovimientoRequest request = new MovimientoRequest(importe, fecha, tipo, nota, usuarioId);

        RetrofitClient.getApiService().registrarMovimiento(request).enqueue(new Callback<MovimientoResponse>() {
            @Override
            public void onResponse(Call<MovimientoResponse> call, Response<MovimientoResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "¡" + tipo.name() + " guardado!", Toast.LENGTH_SHORT).show();
                    // ¡RECARGAMOS LOS DATOS PARA QUE EL SALDO Y LA LISTA SE ACTUALICEN SOLOS!
                    cargarDatosHome();
                } else {
                    try {
                        String mensajeBackend = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e("MOVIMIENTO_ERROR", "Código HTTP: " + response.code() + " | Razón: " + mensajeBackend);
                        Toast.makeText(getContext(), "Error al guardar. Revisa conexión.", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<MovimientoResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Log.e("MOVIMIENTO_ERROR", "Fallo de red: ", t);
                Toast.makeText(getContext(), "Fallo de conexión", Toast.LENGTH_LONG).show();
            }
        });
    }
}