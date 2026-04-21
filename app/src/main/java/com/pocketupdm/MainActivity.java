package com.pocketupdm;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import com.pocketupdm.dialogs.MovimientoBottomSheet;
import com.pocketupdm.dto.MovimientoRequest;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.model.MovementType;
import com.pocketupdm.network.RetrofitClient;
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

    // --- MÉTODOS NUEVOS FUERA DEL ONCREATE ---

    // Este método escucha la respuesta del usuario cuando le sale la ventanita de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ¡El usuario dijo que sí! Lanzamos la notificación
                mostrarNotificacionPersistente();
            } else {
                Toast.makeText(this, "Permiso denegado. No se mostrará el atajo rápido.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Este método salta si la app ya estaba abierta de fondo y tocas la notificación
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        manejarAccionesRapidas(intent);
    }

    private void manejarAccionesRapidas(Intent intent) {
        if (intent != null && intent.hasExtra("ACCION_RAPIDA")) {
            String accion = intent.getStringExtra("ACCION_RAPIDA");

            MovementType tipo = accion.equals("INGRESO") ? MovementType.INGRESO : MovementType.GASTO;

            // 1. Abrimos el formulario de registro rápido
            // IMPORTANTE: Ahora incluimos 'categoriaId' en el lambda (5 parámetros)
            MovimientoBottomSheet bottomSheet = new MovimientoBottomSheet(tipo, (importe, nota, tipoMovimiento, fecha, categoriaId) -> {

                // 2. Ya no ponemos solo un Toast, ¡enviamos los datos de verdad!
                enviarMovimientoAlBackend(importe, nota, tipoMovimiento, fecha, categoriaId);

            });

            bottomSheet.show(getSupportFragmentManager(), "MovimientoBottomSheetRapido");
        }
    }

    private void enviarMovimientoAlBackend(BigDecimal importe, String nota, MovementType tipo, String fecha, Long categoriaId) {
        // 3. AHORA SÍ RECONOCE EL sessionManager
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        // Creamos el request con el nuevo categoriaId
        MovimientoRequest request = new MovimientoRequest(importe, fecha, tipo, nota, usuarioId, categoriaId);

        RetrofitClient.getApiService().registrarMovimiento(request).enqueue(new Callback<MovimientoResponse>() {
            @Override
            public void onResponse(Call<MovimientoResponse> call, Response<MovimientoResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "¡Movimiento guardado desde el acceso rápido!", Toast.LENGTH_SHORT).show();
                    // Opcional: Si tienes el Home abierto debajo, podrías recargar los datos
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

        // 1. Crear el Canal de Notificaciones (Obligatorio en Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Atajos Rápidos",
                    NotificationManager.IMPORTANCE_LOW // LOW para que no suene ni vibre, solo aparezca
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        // 2. Crear los "Intent" (Lo que pasará al tocar los botones)
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

        // 3. Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda) // Logo temporal
                .setContentTitle("PocketUp")
                .setContentText("Añade un movimiento rápido")
                .setOngoing(true) // ¡La hace FIJA como Spotify!
                .setColor(ContextCompat.getColor(this, R.color.turquesa_oscuro))
                .addAction(android.R.drawable.ic_input_add, "Ingreso", piIngreso)
                .addAction(android.R.drawable.ic_delete, "Gasto", piGasto);

        // 4. Mostrarla
        try {
            NotificationManagerCompat.from(this).notify(999, builder.build());
        } catch (SecurityException e) {
            // Si no hay permisos, no crashea
            e.printStackTrace();
        }
    }
}