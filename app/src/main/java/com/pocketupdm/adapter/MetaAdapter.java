package com.pocketupdm.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.pocketupdm.R;
import com.pocketupdm.model.Meta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MetaAdapter extends RecyclerView.Adapter<MetaAdapter.MetaViewHolder> {

    private final Context context;
    private List<Meta> listaMetas;
    private final OnMetaOpcionesListener listener;

    // Interfaz para escuchar los clics
    public interface OnMetaOpcionesListener {
        void onAportar(Meta meta); // Para añadir dinero a la hucha
        void onEditar(Meta meta);
        void onEliminar(Meta meta);
    }

    public MetaAdapter(Context context, List<Meta> listaMetas, OnMetaOpcionesListener listener) {
        this.context = context;
        this.listaMetas = listaMetas != null ? listaMetas : new ArrayList<>();
        this.listener = listener;
    }

    public void setMetas(List<Meta> nuevasMetas) {
        this.listaMetas = nuevasMetas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meta, parent, false);
        return new MetaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetaViewHolder holder, int position) {
        Meta meta = listaMetas.get(position);

        // 1. Textos Básico
        holder.tvNombre.setText(meta.getNombre());

        // 2. Formato de Moneda
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        String acumuladoStr = formatoMoneda.format(meta.getMontoActual());
        String objetivoStr = formatoMoneda.format(meta.getMontoObjetivo());
        holder.tvProgresoTexto.setText(acumuladoStr + " de " + objetivoStr);

        // 3. Matemáticas de la Barra de Progreso
        // Calculamos el porcentaje: (Actual / Objetivo) * 100
        int porcentaje = 0;
        if (meta.getMontoObjetivo().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal division = meta.getMontoActual().divide(meta.getMontoObjetivo(), 4, RoundingMode.HALF_UP);
            porcentaje = division.multiply(new BigDecimal("100")).intValue();
        }

        // Aseguramos que no pase de 100% visualmente
        if (porcentaje > 100) porcentaje = 100;

        holder.pbProgreso.setProgress(porcentaje);
        holder.tvPorcentaje.setText(porcentaje + "%");

        // 4. Fechas (Lógica básica, luego podemos poner "Faltan X días")
        holder.tvFecha.setText("Límite: " + meta.getFechaLimite());

        // 5. ¡LA MAGIA VISUAL DE LAS BURBUJAS! (Reciclada de tus Categorías)
        int colorMeta;
        try {
            colorMeta = Color.parseColor(meta.getColor());
        } catch (Exception e) {
            colorMeta = Color.GRAY;
        }

        int resIdIcono = context.getResources().getIdentifier(meta.getIcono(), "drawable", context.getPackageName());
        if (resIdIcono != 0) {
            holder.ivIcono.setImageResource(resIdIcono);
        } else {
            holder.ivIcono.setImageResource(android.R.drawable.ic_menu_agenda);
        }

        // Pintamos el icono y creamos el fondo transparente al 20%
        int colorFondoClarito = Color.argb(40, Color.red(colorMeta), Color.green(colorMeta), Color.blue(colorMeta));
        holder.cardIcono.setCardBackgroundColor(colorFondoClarito);
        holder.ivIcono.setColorFilter(colorMeta);

        // Opcional: Pintar la barra de progreso del color de la meta (Requiere un Drawable dinámico, por ahora lo dejamos nativo)

        // 6. Menú de 3 Puntos
        holder.ivOpciones.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.ivOpciones);

            // Añadimos las opciones
            popup.getMenu().add("Aportar Dinero");
            popup.getMenu().add("Editar Meta");
            popup.getMenu().add("Eliminar Meta");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Aportar Dinero")) {
                    if (listener != null) listener.onAportar(meta);
                } else if (item.getTitle().equals("Editar Meta")) {
                    if (listener != null) listener.onEditar(meta);
                } else if (item.getTitle().equals("Eliminar Meta")) {
                    if (listener != null) listener.onEliminar(meta);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return listaMetas.size();
    }

    public static class MetaViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardIcono;
        ImageView ivIcono, ivOpciones;
        TextView tvNombre, tvFecha, tvProgresoTexto, tvPorcentaje;
        ProgressBar pbProgreso;

        public MetaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardIcono = itemView.findViewById(R.id.card_icono_meta);
            ivIcono = itemView.findViewById(R.id.iv_icono_meta);
            ivOpciones = itemView.findViewById(R.id.iv_opciones_meta);
            tvNombre = itemView.findViewById(R.id.tv_nombre_meta);
            tvFecha = itemView.findViewById(R.id.tv_fecha_meta);
            tvProgresoTexto = itemView.findViewById(R.id.tv_progreso_texto);
            tvPorcentaje = itemView.findViewById(R.id.tv_porcentaje_meta);
            pbProgreso = itemView.findViewById(R.id.pb_progreso_meta);
        }
    }
}