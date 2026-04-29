package com.pocketupdm.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.pocketupdm.R;

public class NotificacionUI {

    // =======================================================
    // MÉTODOS PARA ACTIVITY (Mantienen tu código actual a salvo)
    // =======================================================
    public static void mostrar(Activity activity, String titulo, String mensaje, int iconoRes) {
        mostrar(activity, titulo, mensaje, iconoRes, R.color.turquesa_dinamico);
    }

    public static void mostrar(Activity activity, String titulo, String mensaje, int iconoRes, int colorRes) {
        View rootView = activity.findViewById(android.R.id.content);
        mostrar(rootView, titulo, mensaje, iconoRes, colorRes);
    }

    public static void mostrarError(Activity activity, String mensaje) {
        mostrar(activity, "¡Ups! Algo salió mal", mensaje, android.R.drawable.stat_notify_error, R.color.red);
    }

    public static void mostrarExito(Activity activity, String mensaje) {
        mostrar(activity, "¡Conseguido!", mensaje, R.drawable.ic_celebration, R.color.turquesa_oscuro);
    }

    public static void mostrarInformacion(Activity activity, String mensaje) {
        mostrar(activity, "Información", mensaje, android.R.drawable.ic_dialog_info, R.color.turquesa_oscuro);
    }

    // =======================================================
    // NUEVOS MÉTODOS PARA VISTAS/DIALOGOS (Soluciona el BottomSheet)
    // =======================================================

    public static void mostrarInformacion(View rootView, String mensaje) {
        mostrar(rootView, "Información", mensaje, android.R.drawable.ic_dialog_info, R.color.turquesa_oscuro);
    }

    public static void mostrarError(View rootView, String mensaje) {
        mostrar(rootView, "¡Ups! Algo salió mal", mensaje, android.R.drawable.stat_notify_error, R.color.red);
    }

    /**
     * El verdadero MOTOR de la notificación, ahora acepta un View.
     */
    @SuppressLint("RestrictedApi")
    public static void mostrar(View rootView, String titulo, String mensaje, int iconoRes, int colorRes) {
        Context context = rootView.getContext();

        // 1. Creamos el Snackbar anclado a la vista que nos pasen
        Snackbar snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_LONG);
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();

        // 2. Configuración visual
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT);
        snackbarLayout.setPadding(0, 0, 0, 0);

        // 3. Posicionamiento (Arriba)
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarLayout.getLayoutParams();
        params.gravity = Gravity.TOP;
        float density = context.getResources().getDisplayMetrics().density;
        int margenSuperiorPx = (int) (60 * density);
        int margenLateralPx = (int) (16 * density);
        params.setMargins(margenLateralPx, margenSuperiorPx, margenLateralPx, 0);
        snackbarLayout.setLayoutParams(params);

        // 4. Inflar diseño
        View customView = LayoutInflater.from(context).inflate(R.layout.layout_notificacion_app, null);

        // 5. Referenciar componentes
        TextView tvTitulo = customView.findViewById(R.id.tv_notif_titulo);
        TextView tvMensaje = customView.findViewById(R.id.tv_notif_mensaje);
        ImageView ivIcono = customView.findViewById(R.id.iv_notif_icono);
        MaterialCardView cardContainer = customView.findViewById(R.id.card_notif_container);

        // 6. Aplicar estilos
        tvTitulo.setText(titulo);
        tvMensaje.setText(mensaje);

        int colorFinal = ContextCompat.getColor(context, colorRes);
        cardContainer.setStrokeColor(ColorStateList.valueOf(colorFinal));
        ivIcono.setImageTintList(ColorStateList.valueOf(colorFinal));

        if (iconoRes != 0) {
            ivIcono.setImageResource(iconoRes);
        } else {
            ivIcono.setImageResource(android.R.drawable.ic_dialog_info);
        }

        // 7. Mostrar
        snackbarLayout.removeAllViews();
        snackbarLayout.addView(customView, 0);
        snackbar.show();
    }
}