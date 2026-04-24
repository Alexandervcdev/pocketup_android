package com.pocketupdm.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.pocketupdm.R;
import com.pocketupdm.adapter.MovimientoAdapter;
import com.pocketupdm.dialogs.MovimientoBottomSheet;
import com.pocketupdm.model.Meta;
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
    private ImageView ivStatusSaldo;

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
        tvSaldo = view.findViewById(R.id.tv_home_saldo); // Este mostrará el Disponible
        ivStatusSaldo = view.findViewById(R.id.iv_status_saldo);

        // 2. Mensaje de bienvenida
        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        tvWelcome.setText("¡Hola, " + sessionManager.getUsuarioNombre() + "!");

        // 3. Subtítulo dinámico con el mes actual
        TextView tvSubtitle = view.findViewById(R.id.tv_subtitle);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
        String mesActual = sdfMes.format(calendar.getTime());
        tvSubtitle.setText("Este es tu progreso de " + mesActual.substring(0, 1).toUpperCase() + mesActual.substring(1));

        // 4. Configurar Lista de Movimientos Recientes
        rvTopMovimientos = view.findViewById(R.id.rv_top_movimientos);
        rvTopMovimientos.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MovimientoAdapter(getContext(), new ArrayList<>(), null);
        rvTopMovimientos.setAdapter(adapter);

        // 4.5. Configurar clic en "Últimos movimientos"
        view.findViewById(R.id.btn_ver_todos).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("TAB_SELECCIONADO", 0);
            Navigation.findNavController(v).navigate(R.id.action_nav_home_to_nav_historial, bundle);
        });

        // 5. Configurar el Menú Flotante
        view.findViewById(R.id.btn_menu_movimientos).setOnClickListener(v -> mostrarMenuOpciones());

        // 6. ¡VITAL! Llamamos al servidor
        cargarDatosHome();
    }

    private void cargarDatosHome() {
        Long usuarioId = sessionManager.getUsuarioId();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", new Locale("es", "ES"));
        tvFecha.setText(sdf.format(new Date()));

        // PRIMERA LLAMADA: Pedimos los movimientos
        RetrofitClient.getApiService().obtenerMovimientos(usuarioId).enqueue(new Callback<List<MovimientoResponse>>() {
            @Override
            public void onResponse(Call<List<MovimientoResponse>> call, Response<List<MovimientoResponse>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<MovimientoResponse> todosLosMovimientos = response.body();

                    // 1. Calculamos el Patrimonio Total (Ingresos - Gastos)
                    BigDecimal saldoTotalCalculado = BigDecimal.ZERO;
                    for (MovimientoResponse m : todosLosMovimientos) {
                        if (m.getTipo() == MovementType.INGRESO) {
                            saldoTotalCalculado = saldoTotalCalculado.add(m.getImporte());
                        } else {
                            saldoTotalCalculado = saldoTotalCalculado.subtract(m.getImporte());
                        }
                    }

                    // 2. Extraemos los top 3 para la lista
                    Collections.sort(todosLosMovimientos, (m1, m2) -> m2.getId().compareTo(m1.getId()));
                    int limite = Math.min(todosLosMovimientos.size(), 3);
                    adapter.setMovimientos(new ArrayList<>(todosLosMovimientos.subList(0, limite)));

                    // 3. SEGUNDA LLAMADA: Pedimos las Metas para calcular el Disponible
                    calcularDisponibleConMetas(usuarioId, saldoTotalCalculado);
                }
            }

            @Override
            public void onFailure(Call<List<MovimientoResponse>> call, Throwable t) {
                Log.e("HOME", "Error al traer movimientos", t);
            }
        });
    }

    // Trae las metas y hace la matemática final
    private void calcularDisponibleConMetas(Long usuarioId, BigDecimal patrimonioTotal) {
        RetrofitClient.getApiService().obtenerMetas(usuarioId).enqueue(new Callback<List<Meta>>() {
            @Override
            public void onResponse(Call<List<Meta>> call, Response<List<Meta>> response) {
                if (!isAdded() || getContext() == null) return;

                BigDecimal dineroAhorrado = BigDecimal.ZERO;
                // Si hay metas, sumamos el dinero que el usuario tiene guardado en ellas
                if (response.isSuccessful() && response.body() != null) {
                    for (Meta meta : response.body()) {
                        dineroAhorrado = dineroAhorrado.add(meta.getMontoActual());
                    }
                }

                // LA FÓRMULA MÁGICA: Disponible = Total - Ahorrado
                BigDecimal saldoDisponible = patrimonioTotal.subtract(dineroAhorrado);

                // Mostramos SOLO el saldo disponible en pantalla
                pintarSaldosEnPantalla(saldoDisponible);
            }

            @Override
            public void onFailure(Call<List<Meta>> call, Throwable t) {
                // Si falla la carga de metas, asumimos que no hay ahorros y mostramos el total
                pintarSaldosEnPantalla(patrimonioTotal);
            }
        });
    }

    private void pintarSaldosEnPantalla(BigDecimal disponible) {
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

        // Pintamos el texto principal
        tvSaldo.setText(formatoMoneda.format(disponible));

        // Lógica visual del ícono basada en el DINERO DISPONIBLE
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        int colorDinamicoTexto = typedValue.data;

        if (disponible.compareTo(BigDecimal.ZERO) >= 0) {
            ivStatusSaldo.setImageResource(R.drawable.ic_ingreso);
        } else {
            ivStatusSaldo.setImageResource(R.drawable.ic_spent);
        }
        ivStatusSaldo.setImageTintList(ColorStateList.valueOf(colorDinamicoTexto));
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
        MovimientoBottomSheet bottomSheet = new MovimientoBottomSheet(tipo, (importe, nota, tipoMovimiento, fecha, categoriaId) ->
                enviarMovimientoAlBackend(importe, nota, tipoMovimiento, fecha, categoriaId)
        );
        bottomSheet.show(getParentFragmentManager(), "MovimientoBottomSheet");
    }

    private void enviarMovimientoAlBackend(BigDecimal importe, String nota, MovementType tipo, String fecha,Long categoriaId) {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) {
            Toast.makeText(getContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            com.pocketupdm.utils.NavigationUtil.irALogin(getActivity());
            return;
        }
        MovimientoRequest request = new MovimientoRequest(importe, fecha, tipo, nota, usuarioId,categoriaId);

        RetrofitClient.getApiService().registrarMovimiento(request).enqueue(new Callback<MovimientoResponse>() {
            @Override
            public void onResponse(Call<MovimientoResponse> call, Response<MovimientoResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "¡" + tipo.name() + " guardado!", Toast.LENGTH_SHORT).show();
                    cargarDatosHome();
                } else {
                    Toast.makeText(getContext(), "Error al guardar. Revisa conexión.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<MovimientoResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Fallo de conexión", Toast.LENGTH_LONG).show();
            }
        });
    }
}