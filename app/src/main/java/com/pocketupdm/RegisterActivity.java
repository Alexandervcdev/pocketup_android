package com.pocketupdm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pocketupdm.controller.ApiService;
import com.pocketupdm.dto.UsuarioRegistroRequest;
import com.pocketupdm.model.Usuario;
import com.pocketupdm.network.ErrorUtil;
import com.pocketupdm.network.RetrofitClient;

import retrofit2.Call;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword;
    private static final String TAG = "POCKET_APP";
    private Button btnRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("PreferenciasApp", MODE_PRIVATE);
        String tema = prefs.getString("temaNombre", "Predeterminado");

        if (tema.equals("Claro")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (tema.equals("Oscuro")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // LOGO DINÁMICO (Justo después de cargar la vista)
        ImageView ivLogo = findViewById(R.id.iv_register_logo);
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            ivLogo.setImageResource(R.drawable.logo_blanco);
        } else {
            ivLogo.setImageResource(R.drawable.logo);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Vincular componentes
        etNombre = findViewById(R.id.ed_register_name);
        etEmail = findViewById(R.id.ed_register_email);
        etPassword = findViewById(R.id.ed_register_pass);
        btnRegistrar = findViewById(R.id.bt_register_submit);

        loginClickListener();
        btnRegistrar.setOnClickListener(v -> ejecutarRegistro());
    }

    /**
     * Ejecuta la lógica principal para registrar un nuevo usuario en el sistema de forma manual.
     * 1. Extrae y limpia los datos de los campos de texto (nombre, email, password).
     * 2. Realiza una validación local para asegurar que no se envíen campos vacíos.
     * 3. Construye el DTO UsuarioRegistroRequest.
     * 4. Realiza la llamada asíncrona a la API (/user/register) mediante RetrofitClient.
     * * Gestiona las respuestas: En caso de éxito (201), notifica al usuario y redirige al Login.
     * En caso de fallo (ej. email duplicado), utiliza ErrorUtil para parsear y mostrar el
     * mensaje exacto generado por el Backend, evitando cierres inesperados.
     */
    private void ejecutarRegistro() {
        //recogida de los datos de los campos.
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        // Validación
        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creacion del DTO
        UsuarioRegistroRequest request = new UsuarioRegistroRequest();
        request.setNombre(nombre);
        request.setEmail(email);
        request.setPassword(pass);

        //Pedir la instancia y ejecutar la llamada
        ApiService api = RetrofitClient.getApiService();
        Call<Usuario> call = api.saveUser(request);

        call.enqueue(new retrofit2.Callback<Usuario>() {
        @Override
        public void onResponse(Call<Usuario> call, retrofit2.Response<Usuario> response) {
            //Log.d(TAG, "Código de respuesta del servidor: " + response.code());
            if (response.isSuccessful()) {
                // ÉXITO: Código 201 Created
                Usuario usuarioCreado = response.body();
                String nombre = (usuarioCreado != null) ? usuarioCreado.getNombre() : "";
                Toast.makeText(RegisterActivity.this, "¡Registro exitoso! Bienvenido " + nombre, Toast.LENGTH_LONG).show();
                com.pocketupdm.utils.NavigationUtil.irALogin(RegisterActivity.this);
            } else {
                // ERROR CONTROLADO: El servidor respondió (ej: 400 Bad Request)
                String mensajeError = ErrorUtil.parseError(response);
                Toast.makeText(RegisterActivity.this, mensajeError, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(Call<Usuario> call, Throwable t) {
            // ERROR CRÍTICO: No hay internet o el servidor está apagado
            Toast.makeText(RegisterActivity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    });
    }

    /**
     * Configura el evento de clic para el texto inferior de la pantalla
     * (típicamente "¿Ya tienes cuenta? Inicia sesión").
     * Permite al usuario navegar a la pantalla de inicio de sesión (LoginActivity)
     * sin limpiar la pila de navegación, facilitando el flujo libre entre Login y Registro.
     */
    private void loginClickListener() {
        TextView tvRegister = findViewById(R.id.tv_register_login);
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}