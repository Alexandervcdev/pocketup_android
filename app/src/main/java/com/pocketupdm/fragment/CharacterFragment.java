package com.pocketupdm.fragment;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.pocketupdm.R;
import com.pocketupdm.dto.PersonajeResponse;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CharacterFragment extends Fragment {

    private TextView tvNivel, tvRango, tvNivelLabel,tvNombrePersonaje;
    private ProgressBar pbExperiencia;
    private MaterialButton btnCambiarSkin;
    private ImageView ivSkinPersonajeGrande, ivMiniAvatar;

    private SessionManager sessionManager;
    private int nivelActual = 1;


    public CharacterFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        tvNivelLabel = view.findViewById(R.id.tv_nivel_label);
        tvNivel = view.findViewById(R.id.tv_nivel);
        tvRango = view.findViewById(R.id.tv_rango);
        pbExperiencia = view.findViewById(R.id.pb_experiencia);
        btnCambiarSkin = view.findViewById(R.id.btn_cambiar_skin);
        ivSkinPersonajeGrande = view.findViewById(R.id.iv_skin_personaje_grande);
        ivMiniAvatar = view.findViewById(R.id.iv_mini_avatar);
        tvNombrePersonaje = view.findViewById(R.id.tv_nombre_personaje);

        // 2. Click en el botón del armario
        btnCambiarSkin.setOnClickListener(v -> mostrarArmario());

        // 3. Traer datos
        cargarDatosPersonaje();

    }

    private void cargarDatosPersonaje() {
        Long usuarioId = sessionManager.getUsuarioId();
        if (usuarioId == -1L) return;

        RetrofitClient.getApiService().obtenerPersonaje(usuarioId).enqueue(new Callback<PersonajeResponse>() {
            @Override
            public void onResponse(Call<PersonajeResponse> call, Response<PersonajeResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    actualizarUI(response.body());

                } else {
                    // Hacemos que Android nos diga el código exacto (404, 400, 500...)
                    Toast.makeText(getContext(), "Error Código: " + response.code(), Toast.LENGTH_LONG).show();
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Sin detalles";
                        Log.e("CHARACTER", "Motivo del rechazo: " + errorBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<PersonajeResponse> call, Throwable t) {
                Log.e("CHARACTER", "Error de red: ", t);
            }
        });
    }

    private void actualizarUI(PersonajeResponse personaje) {
        this.nivelActual = personaje.getNivel(); // Guardamos su nivel

        if (personaje.getNombre() != null && !personaje.getNombre().isEmpty()) {
            tvNombrePersonaje.setText(personaje.getNombre());
        } else {
            tvNombrePersonaje.setText("Aventurero");
        }


        // Pintamos los textos
        tvNivelLabel.setText("Nivel");
        tvNivel.setText(String.valueOf(personaje.getNivel()));
        tvRango.setText(personaje.getRango());

        // Lógica matemática de la barra de XP:
        // Asumiremos que cada nivel requiere 100 XP.
        // Si el usuario tiene 240 XP totales, significa que es Nivel 3 y tiene 40 en la barra.
        int progresoEnBarra = personaje.getXp() % 100;
        pbExperiencia.setProgress(progresoEnBarra);

        animarBarraXP(progresoEnBarra);
        animarPersonaje();
        // Cambiamos los dibujos según la skin que tenga puesta
        int iconoRecurso = obtenerRecursoSkin(personaje.getSkinActiva());
        ivSkinPersonajeGrande.setImageResource(iconoRecurso);
        ivMiniAvatar.setImageResource(iconoRecurso);
    }

    private int obtenerRecursoSkin(int idSkin) {
        // Reemplazamos los iconos nativos por tus archivos PNG
        switch (idSkin) {
            case 2:
                return R.drawable.mapache; // Asegúrate de tener pj2.png
            case 3:
                return R.drawable.pj1; // Asegúrate de tener pj3.png
            default:
                return R.drawable.pj1; // Tu imagen base pj1.png
        }
    }

    private void mostrarArmario() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        LinearLayout layoutMain = new LinearLayout(requireContext());
        layoutMain.setOrientation(android.widget.LinearLayout.VERTICAL);
        layoutMain.setPadding(60, 60, 60, 60);

        TextView titulo = new TextView(requireContext());
        titulo.setText("Selecciona tu Aspecto");
        titulo.setTextSize(20f);
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);
        titulo.setPadding(0, 0, 0, 40);
        layoutMain.addView(titulo);

        // Agregamos los 3 botones al menú. Si no tiene nivel, salen bloqueados.
        layoutMain.addView(crearBotonSkin("1. Skin Novato (Nivel 1)", 1, nivelActual >= 1, dialog));
        layoutMain.addView(crearBotonSkin("2. Skin Plata (Nivel 2)", 2, nivelActual >= 2, dialog));
        layoutMain.addView(crearBotonSkin("3. Skin Oro (Nivel 5)", 3, nivelActual >= 5, dialog));

        dialog.setContentView(layoutMain);
        dialog.show();
    }

    private MaterialButton crearBotonSkin(String texto, int idSkin, boolean desbloqueado, BottomSheetDialog dialog) {
        MaterialButton btn = new MaterialButton(requireContext());
        btn.setText(texto);

        // Mostramos una miniatura en el botón si está desbloqueado
        if (desbloqueado) {
            btn.setIcon(ContextCompat.getDrawable(requireContext(), obtenerRecursoSkin(idSkin)));
            btn.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            btn.setOnClickListener(v -> {
                dialog.dismiss();
                guardarNuevaSkin(idSkin);
            });
        } else {
            btn.setEnabled(false);
            btn.setAlpha(0.5f);
            btn.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_secure));
        }
        return btn;
    }

    private void guardarNuevaSkin(int idSkin) {
        Long usuarioId = sessionManager.getUsuarioId();
        RetrofitClient.getApiService().cambiarSkin(usuarioId, idSkin).enqueue(new Callback<PersonajeResponse>() {
            @Override
            public void onResponse(Call<PersonajeResponse> call, Response<PersonajeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), "¡Aspecto equipado!", Toast.LENGTH_SHORT).show();
                    actualizarUI(response.body()); // Refresca la pantalla al instante con el nuevo monigote
                } else {
                    Toast.makeText(getContext(), "No se pudo cambiar el aspecto", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PersonajeResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animarPersonaje() {
        // Creamos una animación de escala en X e Y
        ivSkinPersonajeGrande.setScaleX(0.7f); // Empezamos pequeñito
        ivSkinPersonajeGrande.setScaleY(0.7f);

        ivSkinPersonajeGrande.animate()
                .scaleX(1f) // Volvemos al tamaño original
                .scaleY(1f)
                .setDuration(500) // Media segundo de duración
                .setInterpolator(new android.view.animation.BounceInterpolator()) // El efecto rebote mágico
                .start();
    }

    private void animarBarraXP(int valorFinal) {
        // Forzamos que empiece en 0 por si acaso
        pbExperiencia.setProgress(0);

        // Creamos el animador: (objeto, propiedad, valor inicial, valor final)
        ObjectAnimator animation = ObjectAnimator.ofInt(pbExperiencia, "progress", 0, valorFinal);

        animation.setDuration(1200); // 1.2 segundos para que se vea el recorrido
        // Usamos Decelerate para que empiece rápido y frene suave al llegar al final
        animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animation.start();
    }
}