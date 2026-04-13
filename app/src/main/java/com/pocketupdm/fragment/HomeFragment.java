package com.pocketupdm.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.pocketupdm.R;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        String nombreUsuario = sessionManager.getUsuarioNombre();
        tvWelcome.setText("¡Hola, " + nombreUsuario + "!");
        // 1. Buscamos el botón de gestión
        MaterialButton btnMenu = view.findViewById(R.id.btn_menu_movimientos);

        // 2. Configuramos el menú desplegable
        btnMenu.setOnClickListener(v -> mostrarMenuOpciones(v));
    }

    private void mostrarMenuOpciones(View view) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        // Añadimos las opciones al vuelo
        popup.getMenu().add("Nuevo Ingreso");
        popup.getMenu().add("Nuevo Gasto");
        popup.getMenu().add("Ver Ingresos");
        popup.getMenu().add("Ver Gastos");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getTitle().toString()) {
                case "Nuevo Ingreso":
                    mostrarDialogoFormulario(MovementType.INGRESO);
                    return true;
                case "Nuevo Gasto":
                    mostrarDialogoFormulario(MovementType.GASTO);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void mostrarDialogoFormulario(MovementType tipo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(tipo == MovementType.INGRESO ? "Registrar Ingreso" : "Registrar Gasto");

        // Creamos un campo de texto para el importe
        final EditText inputImporte = new EditText(requireContext());
        inputImporte.setHint("0.00");
        inputImporte.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(inputImporte);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String importeStr = inputImporte.getText().toString();
            if (!importeStr.isEmpty()) {
                enviarMovimientoAlBackend(new BigDecimal(importeStr), tipo);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void enviarMovimientoAlBackend(BigDecimal importe, MovementType tipo) {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) {
            Toast.makeText(getContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show();
            com.pocketupdm.utils.NavigationUtil.irALogin(getActivity());
            return;
        }

        // Fecha de hoy en formato ISO (YYYY-MM-DD)
        String fechaHoy = LocalDate.now().toString();

        MovimientoRequest request = new MovimientoRequest(
                importe,
                fechaHoy,
                tipo, "Registro desde el móvil",
                usuarioId
        );

        RetrofitClient.getApiService().registrarMovimiento(request).enqueue(new Callback<MovimientoResponse>() {
            @Override
            public void onResponse(Call<MovimientoResponse> call, Response<MovimientoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "¡" + tipo.name() + " guardado con éxito!", Toast.LENGTH_SHORT).show();
                    // Aquí es donde más adelante refrescaremos el número del balance en la tarjeta
                } else {
                    Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<MovimientoResponse> call, Throwable t) {
                //Log.e("RETROFIT_ERROR", "Error al procesar la respuesta: ", t);
                Toast.makeText(getContext(), "Fallo de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}