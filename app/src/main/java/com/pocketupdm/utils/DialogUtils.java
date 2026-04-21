package com.pocketupdm.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
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
}