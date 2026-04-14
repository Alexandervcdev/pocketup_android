package com.pocketupdm.dialogs;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketupdm.R;
import com.pocketupdm.model.MovementType;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MovimientoBottomSheet extends BottomSheetDialogFragment {

    private MovementType tipo;
    private OnMovimientoGuardadoListener listener;
    private String fechaSeleccionada; // Formato YYYY-MM-DD para la API

    // Interfaz para avisarle al HomeFragment que se guardó un dato
    public interface OnMovimientoGuardadoListener {
        void onGuardar(BigDecimal importe, String nota, MovementType tipo,String fecha);
    }

    // Constructor para pasarle el tipo (INGRESO o GASTO) y el listener
    public MovimientoBottomSheet(MovementType tipo, OnMovimientoGuardadoListener listener) {
        this.tipo = tipo;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_movimiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitulo = view.findViewById(R.id.tv_titulo_sheet);
        TextInputEditText etImporte = view.findViewById(R.id.et_importe);
        TextInputEditText etFecha = view.findViewById(R.id.et_fecha);
        TextInputEditText etNota = view.findViewById(R.id.et_nota);
        MaterialButton btnGuardar = view.findViewById(R.id.btn_guardar_movimiento);

        // 1. Configurar fecha por defecto (Hoy)
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfVisual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        fechaSeleccionada = sdfApi.format(calendar.getTime());
        etFecha.setText(sdfVisual.format(calendar.getTime()));
        // 2. Configurar el clic para abrir el calendario
        etFecha.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Selecciona una fecha")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(selection);

                // Guardamos el formato para la API y mostramos el formato visual
                fechaSeleccionada = sdfApi.format(utc.getTime());
                etFecha.setText(sdfVisual.format(utc.getTime()));
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        // ¡MAGIA VISUAL! Cambiamos los colores según el tipo de movimiento
        if (tipo == MovementType.INGRESO) {
            tvTitulo.setText("Nuevo Ingreso");
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro));
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.turquesa_oscuro)));
        } else {
            tvTitulo.setText("Nuevo Gasto");
            tvTitulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
            btnGuardar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red)));
        }

        btnGuardar.setOnClickListener(v -> {
            String importeStr = etImporte.getText().toString().trim();
            String notaStr = etNota.getText().toString().trim();

            if (importeStr.isEmpty()) {
                etImporte.setError("El importe es obligatorio");
                return;
            }
            try {
                importeStr = importeStr.replace(",", ".");
                BigDecimal importe = new BigDecimal(importeStr);
                if (importe.compareTo(BigDecimal.ZERO) <= 0) {
                    etImporte.setError("El importe debe ser mayor a 0");
                    return;
                }

                // 4. Validar que no exceda el límite de la base de datos (99 millones)
                if (importe.compareTo(new BigDecimal("99999999.99")) > 0) {
                    etImporte.setError("¡Vaya! Esa cantidad es demasiado grande");
                    return;
                }

                if (notaStr.isEmpty()) {
                    notaStr = "Sin nota";
                }

                // Si sobrevive a todas las validaciones, lo enviamos
                listener.onGuardar(importe, notaStr, tipo, fechaSeleccionada);
                dismiss();

            } catch (NumberFormatException e) {
                // Si el usuario metió caracteres extraños ("50..5")
                etImporte.setError("Formato de número inválido");
            }
        });
    }
}