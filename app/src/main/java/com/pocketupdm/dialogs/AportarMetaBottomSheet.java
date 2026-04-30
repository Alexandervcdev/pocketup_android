package com.pocketupdm.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketupdm.R;
import com.pocketupdm.model.Meta;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.NotificacionUI;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AportarMetaBottomSheet extends BottomSheetDialogFragment {

    private Meta meta;
    private Runnable onSuccess;

    public void setMeta(Meta meta) { this.meta = meta; }
    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.aportar_meta, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitulo = view.findViewById(R.id.tv_aportar_titulo);
        tvTitulo.setText("Aportar a: " + meta.getNombre());

        TextInputEditText etCantidad = view.findViewById(R.id.et_aportar_cantidad);
        MaterialButton btnGuardar = view.findViewById(R.id.btn_aportar_guardar);

        btnGuardar.setOnClickListener(v -> {
            String cantidadStr = etCantidad.getText().toString().trim();
            if (cantidadStr.isEmpty()) {
                etCantidad.setError("Introduce una cantidad");
                return;
            }

            try {
                BigDecimal cantidad = new BigDecimal(cantidadStr.replace(",", "."));
                // 1. Validar que no ponga números negativos o cero
                if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                    etCantidad.setError("Debe ser mayor a 0");
                    return;
                }

                BigDecimal restante = meta.getMontoObjetivo().subtract(meta.getMontoActual());

                // 3. Si la cantidad es mayor a lo que falta, le lanzamos el aviso
                if (cantidad.compareTo(restante) > 0) {
                    // Formateamos para que se vea bonito (Ej: Solo te faltan 5.00)
                    java.text.NumberFormat formato = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "ES"));
                    etCantidad.setError("Solo te faltan " + formato.format(restante));
                    return;
                }

                ejecutarAporte(cantidad);

            } catch (Exception e) {
                etCantidad.setError("Cantidad no válida");
            }
        });
    }

    private void ejecutarAporte(BigDecimal cantidad) {
        Map<String, BigDecimal> payload = new HashMap<>();
        payload.put("cantidad", cantidad);

        RetrofitClient.getApiService().agregarFondosMeta(meta.getId(), payload).enqueue(new Callback<Meta>() {
            @Override
            public void onResponse(Call<Meta> call, Response<Meta> response) {
                if (!isAdded() || getActivity() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    Meta metaActualizada = response.body();

                    // Comprobamos si se ha completado
                    if (metaActualizada.getMontoActual().compareTo(metaActualizada.getMontoObjetivo()) >= 0) {
                        NotificacionUI.mostrar(getActivity(), "¡Meta Completada! 🏆",
                                "Felicidades, has ahorrado: " + metaActualizada.getNombre(),
                                android.R.drawable.star_on);
                    } else {
                        Toast.makeText(getContext(), "Dinero añadido a la hucha", Toast.LENGTH_SHORT).show();
                    }

                    if (onSuccess != null) onSuccess.run();
                    dismiss();
                }
            }

            @Override
            public void onFailure(Call<Meta> call, Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}