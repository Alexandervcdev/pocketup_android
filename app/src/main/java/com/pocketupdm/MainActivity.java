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
import com.pocketupdm.model.MovementType;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        NavigationUI.setupWithNavController(navView, navController);

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

            // Abrimos el formulario de registro rápido
            MovimientoBottomSheet bottomSheet = new MovimientoBottomSheet(tipo, (importe, nota, tipoMovimiento, fecha) -> {
                // Aquí temporalmente ponemos un Toast.
                // Luego decidiremos cómo mandarlo a la base de datos desde aquí.
                Toast.makeText(this, "Deberíamos guardar un " + tipoMovimiento.name() + " de " + importe + "€", Toast.LENGTH_SHORT).show();
            });

            bottomSheet.show(getSupportFragmentManager(), "MovimientoBottomSheetRapido");
        }
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