package com.pocketupdm;

import android.Manifest;
import android.app.AlertDialog; // <--- NUEVO IMPORT
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log; // <--- NUEVO IMPORT
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth; // <--- NUEVO IMPORT
import com.pocketupdm.dialogs.MovimientoBottomSheet;
import com.pocketupdm.dto.EstadoSistemaResponse;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.dto.UsuarioResponse;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.model.Usuario; // <--- NUEVO IMPORT
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.DialogUtils;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verificarMantenimiento();

        sessionManager = new SessionManager(this);

        if (sessionManager.getUsuarioId() == -1L) {
            // Si alguien intenta entrar aquí (ej. por notificación) y NO hay sesión activa...
            Toast.makeText(this, "Tu sesión ha expirado. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show();
            // Lo mandamos al Login de una patada (y cerramos la notificación)
            NotificationManagerCompat.from(this).cancel(999);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return; // Detenemos el MainActivity
        }

        // --- 🚨 NUEVO: VERIFICACIÓN SILENCIOSA DE CUENTA SUSPENDIDA ---
        verificarEstadoCuenta();
        // --------------------------------------------------------------

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configuración de la navegación inferior
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        NavController navController = navHostFragment.getNavController();

        // 1. Configuramos el menú de forma estándar para que los iconos se pinten bien
        NavigationUI.setupWithNavController(navView, navController);

        // 2. EVENTO A: Cuando vienes de OTRA pestaña (Ej: De Ajustes a Inicio)
        navView.setOnItemSelectedListener(item -> {
            // Dejamos que Android haga la navegación normal hacia la pestaña
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);

            // Inmediatamente después, forzamos a destruir cualquier fragmento que se haya quedado abierto encima
            navController.popBackStack(item.getItemId(), false);

            return handled;
        });

        // 3. EVENTO B: Cuando YA ESTÁS en la pestaña y la vuelves a tocar (Ej: Estás en Historial y tocas Inicio)
        navView.setOnItemReselectedListener(item -> {
            // Limpiamos el fragmento que esté encima para volver a la raíz
            navController.popBackStack(item.getItemId(), false);
        });

        // 1. VERIFICAMOS Y PEDIMOS PERMISO DE NOTIFICACIONES (Obligatorio en Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Si no tenemos permiso, lanzamos la ventanita para preguntarle al usuario
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                // Si ya nos dio permiso antes, la mostramos directamente
                mostrarNotificacionPersistente();
            }
        } else {
            // Si es un teléfono con Android 12 o inferior, no hace falta preguntar, se muestra sola
            mostrarNotificacionPersistente();
        }

        // 2. Comprobamos si la app se abrió tocando la notificación (ej. estaba totalmente cerrada)
        manejarAccionesRapidas(getIntent());
    }

    // --- MÉTODOS DE SEGURIDAD (SUSPENSIÓN DE CUENTAS) ---

    private void verificarEstadoCuenta() {
        Long idUsuario = sessionManager.getUsuarioId();
        if (idUsuario == -1L) return;

        // USAMOS TU ENDPOINT EXISTENTE
        RetrofitClient.getApiService().obtenerPerfil(idUsuario).enqueue(new Callback<UsuarioResponse>() {
            @Override
            public void onResponse(Call<UsuarioResponse> call, Response<UsuarioResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UsuarioResponse perfil = response.body();

                    if ("SUSPENDIDO".equals(perfil.getEstado())) {
                        expulsarUsuarioSuspendido();
                    }
                }
            }
            @Override
            public void onFailure(Call<UsuarioResponse> call, Throwable t) {
                Log.e("MAIN_ACTIVITY", "No se pudo verificar el estado de la cuenta. Permitiendo uso offline.", t);
            }
        });
    }

    private void expulsarUsuarioSuspendido() {
        sessionManager.cerrarSesion();

        FirebaseAuth.getInstance().signOut();

        // 3. Borrar la notificación persistente de la barra superior
        NotificationManagerCompat.from(this).cancel(999);

        // 4. Mostrar el diálogo ineludible
        DialogUtils.mostrarDialogoBloqueo(
                this,
                "Cuenta Suspendida",
                "Tu acceso ha sido bloqueado por un administrador debido a una infracción de las normas o inactividad.\n\nPor favor, ponte en contacto con pocketup.soporte@gmail.com para solicitar la restauración de tu cuenta.",
                "Volver al Inicio",
                R.color.red, // Usa tu color rojo o el color primario de tu app
                () -> {
                    // Acción al presionar el botón: Redirigir al Login
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
        );
    }

    // --- MÉTODOS DEL CICLO DE VIDA Y NOTIFICACIONES ---

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mostrarNotificacionPersistente();
            } else {
                Toast.makeText(this, "Permiso denegado. No se mostrará el atajo rápido.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        manejarAccionesRapidas(intent);
    }

    private void manejarAccionesRapidas(Intent intent) {
        if (intent != null && intent.hasExtra("ACCION_RAPIDA")) {
            String accion = intent.getStringExtra("ACCION_RAPIDA");
            MovementType tipo = accion.equals("INGRESO") ? MovementType.INGRESO : MovementType.GASTO;

            MovimientoBottomSheet bottomSheet = new MovimientoBottomSheet(tipo, (nombre, importe, nota, tipoMovimiento, fecha, categoriaId) -> {
                enviarMovimientoAlBackend(nombre, importe, nota, tipoMovimiento, fecha, categoriaId);
            });

            bottomSheet.show(getSupportFragmentManager(), "MovimientoBottomSheetRapido");
        }
    }

    private void enviarMovimientoAlBackend(String nombre, BigDecimal importe, String nota, MovementType tipo, String fecha, Long categoriaId) {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        MovimientoRequest request = new MovimientoRequest(nombre, importe, fecha, tipo, nota, usuarioId, categoriaId);

        RetrofitClient.getApiService().registrarMovimiento(request).enqueue(new Callback<MovimientoResponse>() {
            @Override
            public void onResponse(Call<MovimientoResponse> call, Response<MovimientoResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "¡Movimiento guardado desde el acceso rápido!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<MovimientoResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarNotificacionPersistente() {
        String CHANNEL_ID = "pocketup_atajos";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Atajos Rápidos",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Intent intentIngreso = new Intent(this, MainActivity.class);
        intentIngreso.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentIngreso.putExtra("ACCION_RAPIDA", "INGRESO");
        PendingIntent piIngreso = PendingIntent.getActivity(
                this, 100, intentIngreso, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent intentGasto = new Intent(this, MainActivity.class);
        intentGasto.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentGasto.putExtra("ACCION_RAPIDA", "GASTO");
        PendingIntent piGasto = PendingIntent.getActivity(
                this, 101, intentGasto, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("PocketUp")
                .setContentText("Añade un movimiento rápido")
                .setOngoing(true)
                .setColor(ContextCompat.getColor(this, R.color.turquesa_oscuro))
                .addAction(android.R.drawable.ic_input_add, "Ingreso", piIngreso)
                .addAction(android.R.drawable.ic_delete, "Gasto", piGasto);

        try {
            NotificationManagerCompat.from(this).notify(999, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void verificarMantenimiento() {
        RetrofitClient.getApiService().verificarEstadoSistema().enqueue(new retrofit2.Callback<EstadoSistemaResponse>() {
            @Override
            public void onResponse(Call<EstadoSistemaResponse> call, retrofit2.Response<EstadoSistemaResponse> response) {
                // 🚨 NUEVO: Comprobamos que la actividad siga viva antes de mostrar un diálogo
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    EstadoSistemaResponse estado = response.body();

                    if (estado.isMantenimientoActivo()) {
                        com.pocketupdm.utils.DialogUtils.mostrarDialogoBloqueo(
                                MainActivity.this,
                                estado.getTituloMantenimiento(),
                                estado.getMensajeMantenimiento(),
                                "Cerrar Aplicación",
                                R.color.red,
                                () -> {
                                    finishAffinity();
                                }
                        );
                    }
                }
            }

            @Override
            public void onFailure(Call<EstadoSistemaResponse> call, Throwable t) {
                // Falla silenciosamente, permitiendo el uso offline
            }
        });
    }
}