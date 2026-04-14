package com.pocketupdm.fragment;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.pocketupdm.R;
import com.pocketupdm.utils.NavigationUtil;
import com.pocketupdm.utils.SessionManager;

public class SettingFragment extends Fragment {

    public SettingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        MaterialButton btnDeleteAccount = view.findViewById(R.id.btn_delete_account);

        //listeners de los botones
            //cerrar sesion
        btnLogout.setOnClickListener(v -> mostrarDialogoCustom(
                "Cerrar Sesión",
                "¿Deseas cerrar sesión de tu cuenta?",
                "salir",
                R.color.red,
                R.color.turquesa_oscuro,
                this::ejecutarCierreSesion // Pasamos la función que se ejecutará si dice que sí
        ));
            //eliminar cuenta
        btnDeleteAccount.setOnClickListener(v -> mostrarDialogoCustom(
                "Eliminar Cuenta",
                "Esta acción es irreversible. Se borrarán todos tus movimientos, nivel y progreso de forma permanente. ¿Deseas continuar?",
                "Eliminar",
                R.color.turquesa_oscuro,
                R.color.red,
                this::ejecutarEliminarCuenta
        ));
    }
    /**
     * Metodo que sirve para mostrar diálogos personalizados.
     * @param accionAceptar Un bloque de código (Runnable) que se ejecutará si el usuario presiona el botón derecho.
     */
    private void mostrarDialogoCustom(String titulo, String mensaje, String textoBoton, int colorBotonIzquierdo, int colorBotonDerecho,Runnable accionAceptar) {
        // Creamos el diálogo
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.layout_dialog_confirm);

        // Hacemos que el fondo del diálogo sea transparente para que se vean las esquinas redondeadas de nuestra tarjeta XML
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Vinculamos las vistas del XML
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_dialog_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        MaterialButton btnAction = dialog.findViewById(R.id.btn_dialog_action);

        // Aplicamos los textos
        tvTitle.setText(titulo);
        tvMessage.setText(mensaje);
        btnAction.setText(textoBoton);

        btnCancel.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorBotonIzquierdo)));
        btnAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorBotonDerecho)));

        // Listeners de los botones
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            accionAceptar.run(); // Ejecutamos la función que nos pasaron por parámetro
        });
        dialog.show();
    }

    private void ejecutarCierreSesion() {
        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.cerrarSesion();
        FirebaseAuth.getInstance().signOut();

        CredentialManager credentialManager = CredentialManager.create(requireContext());
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                null,
                ContextCompat.getMainExecutor(requireContext()),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        redirigirAlLogin();
                    }
                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        redirigirAlLogin();
                    }
                }
        );
    }

    private void ejecutarEliminarCuenta() {
        // Aquí conectaremos la llamada a Retrofit para borrar el usuario de Spring Boot más adelante
        Toast.makeText(requireContext(), "Procesando eliminación de cuenta...", Toast.LENGTH_SHORT).show();
    }

    private void redirigirAlLogin() {
        if (getActivity() != null) {
            NavigationUtil.irALogin(getActivity());
        }
    }
}