package com.pocketupdm;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;
import static com.pocketupdm.utils.NavigationUtil.irAMainActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.pocketupdm.utils.SessionManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
import com.pocketupdm.dto.UsuarioLoginRequest;
import com.pocketupdm.dto.UsuarioRegistroRequest;
import com.pocketupdm.model.ApiError;
import com.pocketupdm.model.Usuario;
import com.pocketupdm.network.ErrorUtil;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    //variables utilizadas de la vista
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private com.google.android.gms.common.SignInButton btnLoginGoogle;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private CheckBox chRemember;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_EMAIL = "email";
    private static final String PREF_PASS = "password";
    private static final String PREF_REMEMBER = "remember";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. APLICAR TEMA (Antes de super.onCreate)
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

        // 2. AUTO-LOGIN (Ahorro de recursos)
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.getUsuarioId() != -1L) {
            irAMainActivity(this);
            finish(); // ¡IMPORTANTE! Cerramos el Login para que no se quede detrás en la pila
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 3. LOGO DINÁMICO
        ImageView ivLogo = findViewById(R.id.iv_login_logo);
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            ivLogo.setImageResource(R.drawable.logo_blanco); // Tu versión blanca
        } else {
            ivLogo.setImageResource(R.drawable.logo); // Tu versión color
        }

        // 4. GESTIÓN DE INSETS
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 5. INICIALIZACIONES RESTANTES
        credentialManager = CredentialManager.create(this);
        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.ed_login_email);
        etPassword = findViewById(R.id.ed_login_pass);
        btnLogin = findViewById(R.id.bt_login_submit);
        btnLoginGoogle = findViewById(R.id.bt_login_google);
        chRemember = findViewById(R.id.ch_login_remeber_user);

        cargarPreferencias();
        registerClickListener();
        LoginClickListener();
    }

    /**
     * Carga las preferencias de sesión almacenadas localmente usando SharedPreferences.
     * Si el usuario marcó previamente la opción de "Recordar usuario", autocompleta
     * automáticamente los campos de correo y contraseña en la interfaz.
     */
    private void cargarPreferencias() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isChecked = preferences.getBoolean(PREF_REMEMBER, false);
        chRemember.setChecked(isChecked);

        if (isChecked) {
            etEmail.setText(preferences.getString(PREF_EMAIL, ""));
            etPassword.setText(preferences.getString(PREF_PASS, ""));
        }
    }

    /**
     * Ejecuta la petición asíncrona de inicio de sesión manual contra el Backend.
     * Gestiona la respuesta del servidor: en caso de éxito, guarda las preferencias
     * si la casilla de recordar está activa y navega al flujo principal; en caso
     * de error, utiliza ErrorUtil para extraer y mostrar el mensaje exacto del servidor.
     *
     * @param email    El correo electrónico ingresado por el usuario.
     * @param password La contraseña ingresada por el usuario.
     */
    private void LoginManualClickListener(String email, String password) {
        btnLogin.setEnabled(false);
        UsuarioLoginRequest request = new UsuarioLoginRequest(email, password);

        RetrofitClient.getApiService().loginUser(request).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                btnLogin.setEnabled(true);

                if (response.isSuccessful()) {
                    // LÓGICA DE RECORDAR USUARIO
                    SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    if (chRemember.isChecked()) {
                        editor.putString(PREF_EMAIL, email);
                        editor.putString(PREF_PASS, password);
                        editor.putBoolean(PREF_REMEMBER, true);
                    } else {
                        editor.clear(); // Borra todo si el usuario desmarcó la casilla
                    }
                    editor.apply();

                    // ÉXITO: El servidor devolvió el objeto Usuario
                    Usuario usuario = response.body();
                    SessionManager sessionManager = new SessionManager(LoginActivity.this);
                    // 2. Guardamos el ID y el Nombre del usuario
                    sessionManager.crearSesion(usuario.getId(), usuario.getNombre());
                    Toast.makeText(LoginActivity.this, "¡Bienvenido " + usuario.getNombre() + "!", Toast.LENGTH_SHORT).show();
                    irAMainActivity(LoginActivity.this);
                } else {
                    String mensajeError = ErrorUtil.parseError(response);
                    Toast.makeText(LoginActivity.this, mensajeError, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                btnLogin.setEnabled(true);
                Log.e("POCKET_APP", "Fallo de red", t); // Siguiendo tu filtro de Logcat [cite: 16]
                Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Configura los eventos de clic (Listeners) para los botones de inicio de sesión.
     * - Para el login manual: Valida que los campos no estén vacíos antes de llamar a la API.
     * - Para el login con Google: Construye y lanza la solicitud asíncrona mediante el
     * Credential Manager nativo de Google.
     */
    private void LoginClickListener() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                } else {
                    LoginManualClickListener(email, password);
                }
            }
        });

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

    /**
     * Autentica al usuario en el entorno de Firebase utilizando el token de identidad
     * proporcionado por el inicio de sesión de Google.
     * Si la autenticación es exitosa, procede a sincronizar el usuario con el Backend.
     *
     * @param idToken El token de identidad generado por GoogleIdTokenCredential.
     */
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
                    }
                });
    }

    /**
     * Puente de sincronización entre el ecosistema de Google/Firebase y la base de datos
     * del Backend (Spring Boot). Extrae los datos del usuario logueado en Firebase y los
     * envía al endpoint '/google-auth' para garantizar el registro del usuario en el sistema propio.
     *
     * @param firebaseUser Objeto que contiene la información del usuario autenticado en Firebase.
     */
    private void sincronizarUsuarioConBackend(FirebaseUser firebaseUser) {
        // Extraer los datos de Google/Firebase
        String nombre = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        String uid = firebaseUser.getUid(); // UID como "contraseña" de seguridad

        if (nombre == null || nombre.isEmpty()) {
            nombre = email.split("@")[0];
        }

        UsuarioRegistroRequest request = new UsuarioRegistroRequest();
        request.setNombre(nombre);
        request.setEmail(email);
        request.setPassword(uid);

        ApiService api = RetrofitClient.getApiService();
        Call<Usuario> call = api.googleAuth(request);
        call.enqueue(new retrofit2.Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, retrofit2.Response<Usuario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuarioGoogle = response.body();
                    SessionManager sessionManager = new SessionManager(LoginActivity.this);
                    sessionManager.crearSesion(usuarioGoogle.getId(), usuarioGoogle.getNombre());
                    //Log.d("LOGIN", "Usuario de Google registrado en el backend");
                    Toast.makeText(LoginActivity.this, "¡Bienvenido " + usuarioGoogle.getNombre() + "!", Toast.LENGTH_SHORT).show();
                    irAMainActivity(LoginActivity.this);
                } else {
                    Log.e("LOGIN", "Error crítico en el servidor: " + response.code());
                    Toast.makeText(LoginActivity.this, "Error de sincronización con el servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e("LOGIN", "Fallo de red al sincronizar: " + t.getMessage());
            }
        });
    }


    /**
     * Método del ciclo de vida de Android. Se ejecuta cuando la actividad se vuelve visible.
     * Verifica si ya existe una sesión activa de Firebase en el dispositivo.
     */
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Log.w("LOGIN", "Usuario ya autenticado: " + currentUser.getEmail());

        }
    }

    /**
     * Cierra la sesión activa del usuario en todos los gestores de autenticación.
     * 1. Cierra la sesión en FirebaseAuth.
     * 2. Limpia el estado de las credenciales en el CredentialManager de Google.
     * Nota: Este método está preparado para ser invocado desde la configuración/ajustes de la app.
     */
//    private void signOut() {
//        // 1. Cerrar sesión en Firebase
//        mAuth.signOut();
//        // 2. Cerrar sesión en el Credential Manager (Google)
//        credentialManager.clearCredentialStateAsync(
//                new ClearCredentialStateRequest(),
//                null,
//                ContextCompat.getMainExecutor(this),
//                new CredentialManagerCallback<Void, ClearCredentialException>() {
//                    @Override
//                    public void onResult(Void result) {
//                        Log.d("LOGIN", "Sesión de Google limpiada con éxito");
//                        // Aquí mandas al usuario de vuelta al LoginActivity
//                        regresarAlLogin();
//                    }
//
//                    @Override
//                    public void onError(@NonNull ClearCredentialException e) {
//                        Log.e("LOGIN", "Error al limpiar credenciales: " + e.getMessage());
//                    }
//                }
//        );
//    }

    private void registerClickListener() {
        TextView tvRegister = findViewById(R.id.tv_login_register);
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
