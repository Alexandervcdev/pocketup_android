package com.pocketupdm.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.pocketupdm.R;
import com.pocketupdm.dialogs.MovimientoBottomSheet;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    //variables globales
    private SessionManager sessionManager;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflamos el layout que editamos en el paso anterior
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        //mensaje de bienvenida
        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        String nombreUsuario = sessionManager.getUsuarioNombre();
        tvWelcome.setText("¡Hola, " + nombreUsuario + "!");

// 2. NUEVO: Subtítulo dinámico con el mes actual (Compatible con API 24+)
        TextView tvSubtitle = view.findViewById(R.id.tv_subtitle);

        // Usamos Calendar en lugar de LocalDate
        Calendar calendar = Calendar.getInstance();

        // "MMMM" significa "nombre completo del mes"
        SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
        String mesActual = sdfMes.format(calendar.getTime());

        // Ponemos la primera letra en mayúscula ("abril" -> "Abril")
        mesActual = mesActual.substring(0, 1).toUpperCase() + mesActual.substring(1);

        // Asignamos el texto final
        tvSubtitle.setText("Este es tu progreso de " + mesActual);

        // 1. Buscamos el botón de gestión
        MaterialButton btnMenu = view.findViewById(R.id.btn_menu_movimientos);
        // 2. Configuramos el menú desplegable
        btnMenu.setOnClickListener(v -> mostrarMenuOpciones());
    }

    private void mostrarMenuOpciones() {
        // 1. Creamos el diálogo inferior
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_menu, null);
        bottomSheetDialog.setContentView(view);

        // 3. Vinculamos los botones del panel y les damos funcionalidad
        view.findViewById(R.id.btn_opcion_ingreso).setOnClickListener(v -> {
            bottomSheetDialog.dismiss(); // Cerramos el menú
            abrirBottomSheetMovimiento(MovementType.INGRESO); // Abrimos el formulario
        });
        view.findViewById(R.id.btn_opcion_gasto).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            abrirBottomSheetMovimiento(MovementType.GASTO);
        });

        view.findViewById(R.id.btn_opcion_ver_ingresos).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // 1. Creamos la "cajita" y le metemos el número 0 -> 0 significando ingreso
            Bundle bundle = new Bundle();
            bundle.putInt("TAB_SELECCIONADO", 0);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_nav_home_to_nav_historial,bundle);
        });

        view.findViewById(R.id.btn_opcion_ver_gastos).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();

            // 1. Creamos la "cajita" y le metemos el número 1
            Bundle bundle = new Bundle();
            bundle.putInt("TAB_SELECCIONADO", 1);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_nav_home_to_nav_historial,bundle);
        });

        // 4. Mostramos el menú en pantalla
        bottomSheetDialog.show();
    }

    private void abrirBottomSheetMovimiento(MovementType tipo) {
        MovimientoBottomSheet bottomSheet = new MovimientoBottomSheet(tipo, new MovimientoBottomSheet.OnMovimientoGuardadoListener() {
            @Override
            public void onGuardar(BigDecimal importe, String nota, MovementType tipoMovimiento,String fecha) {
                enviarMovimientoAlBackend(importe, nota, tipoMovimiento,fecha);
            }
        });
        bottomSheet.show(getParentFragmentManager(), "MovimientoBottomSheet");
    }

    private void enviarMovimientoAlBackend(BigDecimal importe,String nota, MovementType tipo,String fecha) {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) {
            Toast.makeText(getContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            com.pocketupdm.utils.NavigationUtil.irALogin(getActivity());
            return;
        }
        MovimientoRequest request = new MovimientoRequest(
                importe,
                fecha,
                tipo,
                nota,
                usuarioId
        );

        RetrofitClient.getApiService().registrarMovimiento(request).enqueue(new Callback<MovimientoResponse>() {
            @Override
            public void onResponse(Call<MovimientoResponse> call, Response<MovimientoResponse> response) {
                // Validación de seguridad para evitar crashes si el fragmento se cerró antes de recibir respuesta
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "¡" + tipo.name() + " guardado con éxito!", Toast.LENGTH_SHORT).show();
                    // Aquí refrescaremos el balance
                } else {
                    // ¡AQUÍ ESTÁ LA MAGIA DE DEPURACIÓN!
                    try {
                        // Leemos el mensaje real que manda Spring Boot
                        String mensajeBackend = response.errorBody() != null ? response.errorBody().string() : "Error desconocido";
                        Log.e("MOVIMIENTO_ERROR", "Código HTTP: " + response.code() + " | Razón: " + mensajeBackend);

                        // O si prefieres usar tu ErrorUtil (descomenta esta línea y borra las de arriba):
                        // String mensajeBackend = com.pocketupdm.network.ErrorUtil.parseError(response);

                        Toast.makeText(getContext(), "Error " + response.code() + ": Revisa el Logcat", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<MovimientoResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Log.e("MOVIMIENTO_ERROR", "Fallo de red: ", t);
                Toast.makeText(getContext(), "Fallo de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}