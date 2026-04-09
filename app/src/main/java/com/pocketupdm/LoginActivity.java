package com.pocketupdm;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.pocketupdm.controller.ApiService;
import com.pocketupdm.dto.UsuarioRegistroRequest;
import com.pocketupdm.model.Usuario;
import com.pocketupdm.network.RetrofitClient;

import java.util.concurrent.Executor;

import retrofit2.Call;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private com.google.android.gms.common.SignInButton btnLoginGoogle;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Credential Manager
        credentialManager = CredentialManager.create(this);
        mAuth = FirebaseAuth.getInstance();

        // Vincular componentes de la vista
        etEmail = findViewById(R.id.ed_login_email);
        etPassword = findViewById(R.id.ed_login_pass);
        btnLogin = findViewById(R.id.bt_login_submit);
        btnLoginGoogle = findViewById(R.id.bt_login_google);


        //Listeners
        registerClickListener();
        LoginClickListener();
    }

    private void LoginClickListener() {
        btnLoginGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .build();

                GetCredentialRequest request = new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

                Executor executor = ContextCompat.getMainExecutor(LoginActivity.this);

                credentialManager.getCredentialAsync(
                        LoginActivity.this,
                        request,
                        null,
                        executor,
                        new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                            @Override
                            public void onResult(GetCredentialResponse result) {
                                Credential credential = result.getCredential();
                                if (credential instanceof CustomCredential &&
                                        credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                                    try {
                                        GoogleIdTokenCredential googleIdTokenCredential =
                                                GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                                        firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                                    } catch (Exception e) {
                                        Log.e("LoginActivity", "Error parsing Google ID token credential", e);
                                    }
                                } else {
                                    Log.w("LoginActivity", "Unexpected credential type");
                                }
                            }

                            @Override
                            public void onError(@NonNull GetCredentialException e) {
                                Log.e("LoginActivity", "GetCredentialException: " + e.getMessage());
                            }
                        }
                );
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("LOGIN", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Firebase ya acepto el login, ahora se envia a tu API
                            sincronizarUsuarioConBackend(user);
                        }
                    } else {
                        Log.w("LOGIN", "signInWithCredential:failure", task.getException());
                        // Puedes mostrar un Toast de error aquí
                    }
                });
    }

    private void sincronizarUsuarioConBackend(FirebaseUser firebaseUser) {
        // 1. Extraer los datos de Google/Firebase
        String nombre = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        String uid = firebaseUser.getUid(); // UID como "contraseña" de seguridad

        // Si por alguna razón Google no devuelve nombre, usamos parte del correo
        if (nombre == null || nombre.isEmpty()) {
            nombre = email.split("@")[0];
        }

        // 2. Preparar la petición para tu API
        UsuarioRegistroRequest request = new UsuarioRegistroRequest();
        request.setNombre(nombre);
        request.setEmail(email);
        request.setPassword(uid);

        // 3. Ejecutar la llamada a tu base de datos
        ApiService api = RetrofitClient.getApiService();
        Call<Usuario> call = api.googleAuth(request);
        call.enqueue(new retrofit2.Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, retrofit2.Response<Usuario> response) {
                if (response.isSuccessful()) {
                    // ÉXITO: El usuario se guardó en tu BD por primera vez
                    Log.d("LOGIN", "Usuario de Google registrado en el backend");
                    irAMainActivity();
                } else {
                    // Esto solo pasará por errores reales (500 error de servidor, 404 ruta mal escrita, etc.)
                    Log.e("LOGIN", "Error crítico en el servidor: " + response.code());
                    Toast.makeText(LoginActivity.this, "Error de sincronización con el servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e("LOGIN", "Fallo de red al sincronizar: " + t.getMessage());
                // Si no hay internet para registrarlo en tu BD, podrías decidir no dejarlo entrar
            }
        });
    }

    private void irAMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Log.w("LOGIN", "Usuario ya autenticado: " + currentUser.getEmail());

        }
    }


    /**
     * IMPLEMENTAR DESPUES EN AJUSTES, EN CERRAR SESION
     */
    private void signOut() {
        // 1. Cerrar sesión en Firebase
        mAuth.signOut();
        // 2. Cerrar sesión en el Credential Manager (Google)
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        Log.d("LOGIN", "Sesión de Google limpiada con éxito");
                        // Aquí mandas al usuario de vuelta al LoginActivity
                        regresarAlLogin();
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e("LOGIN", "Error al limpiar credenciales: " + e.getMessage());
                    }
                }
        );
    }

    private void regresarAlLogin() {
    }

    private void registerClickListener() {
        TextView tvRegister = findViewById(R.id.tv_login_register);
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                signOut();
            }
        });
    }
}
