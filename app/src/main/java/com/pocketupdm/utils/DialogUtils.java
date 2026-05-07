package com.pocketupdm.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.pocketupdm.R;
import android.content.res.ColorStateList;

public class DialogUtils {

    public static void mostrarDialogoConfirmacion(Context context, String titulo, String mensaje,
                                                  String textoBotonAction, int colorBotonCancel,
                                                  int colorBotonAction, Runnable accionAceptar) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.layout_dialog_confirm);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnAction = dialog.findViewById(R.id.btn_dialog_action);

        tvTitle.setText(titulo);
        tvMessage.setText(mensaje);
        btnAction.setText(textoBotonAction);

        btnCancel.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorBotonCancel)));
        btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorBotonAction)));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            accionAceptar.run();
        });

        dialog.show();
    }

    // =========================================================
    // NUEVO: DIÁLOGO DE OPCIONES MÚLTIPLES
    // =========================================================

    // 1. Creamos una interfaz para saber qué opción se tocó
    public interface OnOptionSelectedListener {
        void onOptionSelected(int indice, String opcion);
    }

    // 2. El método reutilizable
    public static void mostrarDialogoOpciones(Context context, String titulo, String[] opciones, OnOptionSelectedListener listener) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_options);

        // Fondo transparente para que se vean los bordes redondeados del CardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Para que ocupe casi todo el ancho de la pantalla
            dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        // Referencias
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_options_title);
        LinearLayout llContainer = dialog.findViewById(R.id.ll_dialog_options_container);

        tvTitle.setText(titulo);

        // Inflador para inyectar nuestras opciones en el contenedor
        LayoutInflater inflater = LayoutInflater.from(context);

        // Bucle para crear una vista por cada opción en el array
        for (int i = 0; i < opciones.length; i++) {
            final int posicion = i;
            final String textoOpcion = opciones[i];

            // Inflamos nuestra "fila" personalizada
            View viewOpcion = inflater.inflate(R.layout.item_dialog_option, llContainer, false);
            TextView tvTexto = viewOpcion.findViewById(R.id.tv_option_text);

            tvTexto.setText(textoOpcion);

            // Si es la opción de "Eliminar", "Borrar", etc., podríamos pintarla de rojo automáticamente
            if (textoOpcion.toLowerCase().contains("eliminar") || textoOpcion.toLowerCase().contains("borrar")) {
                tvTexto.setTextColor(ContextCompat.getColor(context, R.color.red));
            }

            // Evento click
            viewOpcion.setOnClickListener(v -> {
                dialog.dismiss(); // Cerramos el diálogo primero
                listener.onOptionSelected(posicion, textoOpcion); // Avisamos a la Activity/Fragment
            });

            // Añadimos la opción al contenedor
            llContainer.addView(viewOpcion);
        }

        dialog.show();
    }

    // =========================================================
    // NUEVO: DIÁLOGO DE BLOQUEO / INFORMACIÓN (Ineludible, 1 solo botón)
    // =========================================================
    // =========================================================
    // NUEVO: DIÁLOGO DE BLOQUEO / INFORMACIÓN (Ineludible, 1 solo botón)
    // =========================================================
    public static void mostrarDialogoBloqueo(Context context, String titulo, String mensaje,
                                             String textoBoton, int colorBoton,
                                             Runnable accionBoton) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false); // No se puede cerrar tocando fuera
        dialog.setContentView(R.layout.layout_dialog_confirm);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 🚨 SOLUCIÓN 1: Forzar al diálogo a usar el 90% de la pantalla para que el texto no se aplaste
            int width = (int)(context.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnAction = dialog.findViewById(R.id.btn_dialog_action);

        tvTitle.setText(titulo);
        tvMessage.setText(mensaje);
        btnAction.setText(textoBoton);

        // Ocultamos el botón de cancelar
        btnCancel.setVisibility(View.GONE);

        // 🚨 SOLUCIÓN 2: Hacer que el botón de acción ocupe todo el ancho y se centre
        android.view.ViewGroup.LayoutParams currentParams = btnAction.getLayoutParams();
        if (currentParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) currentParams;
            params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            params.setMargins(0, 10, 0, 0); // Un pequeño margen superior para que respire
            btnAction.setLayoutParams(params);
        }

        btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorBoton)));

        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            accionBoton.run();
        });

        dialog.show();
    }
}