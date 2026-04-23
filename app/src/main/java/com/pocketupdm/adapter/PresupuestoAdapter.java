package com.pocketupdm.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.pocketupdm.R;
import com.pocketupdm.model.Presupuesto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PresupuestoAdapter extends RecyclerView.Adapter<PresupuestoAdapter.PresupuestoViewHolder> {

    private final Context context;
    private List<Presupuesto> listaPresupuestos;
    private final OnPresupuestoOpcionesListener listener;

    public interface OnPresupuestoOpcionesListener {
        void onEditar(Presupuesto presupuesto);
        void onEliminar(Presupuesto presupuesto);
    }

    public PresupuestoAdapter(Context context, List<Presupuesto> listaPresupuestos, OnPresupuestoOpcionesListener listener) {
        this.context = context;
        this.listaPresupuestos = listaPresupuestos != null ? listaPresupuestos : new ArrayList<>();
        this.listener = listener;
    }

    public void setPresupuestos(List<Presupuesto> nuevos) {
        this.listaPresupuestos = nuevos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PresupuestoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_presupuesto, parent, false);
        return new PresupuestoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PresupuestoViewHolder holder, int position) {
        Presupuesto pres = listaPresupuestos.get(position);

        // 1. Textos y Formatos
        // Ojo: Si la categoría es null (error de BBDD), ponemos un valor por defecto
        String nombreCat = pres.getCategoria() != null ? pres.getCategoria().getNombre() : "Categoría";
        holder.tvNombre.setText(nombreCat);

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        // Si montoGastado es null, asumimos 0
        BigDecimal gastado = pres.getMontoGastado() != null ? pres.getMontoGastado() : BigDecimal.ZERO;
        BigDecimal limite = pres.getMontoLimite();

        holder.tvGastadoTexto.setText(formatoMoneda.format(gastado) + " de " + formatoMoneda.format(limite));

        // 2. Matemáticas del Porcentaje
        int porcentaje = 0;
        if (limite.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal division = gastado.divide(limite, 4, RoundingMode.HALF_UP);
            porcentaje = division.multiply(new BigDecimal("100")).intValue();
        }

        // Evitamos que la barra visual se rompa si se pasa del 100%
        holder.pbProgreso.setProgress(Math.min(porcentaje, 100));
        holder.tvPorcentaje.setText(porcentaje + "%");

        // 3. ¡MAGIA DEL SEMÁFORO! 🚦
        int colorFiltroCategoria = Color.GRAY;
        if (pres.getCategoria() != null) {
            try { colorFiltroCategoria = Color.parseColor(pres.getCategoria().getColor()); } catch (Exception ignored) {}
        }

        if (porcentaje >= 100) {
            // ROJO: Límite superado
            holder.pbProgreso.setProgressTintList(ColorStateList.valueOf(Color.RED));
            holder.tvPorcentaje.setTextColor(Color.RED);
            holder.tvAdvertencia.setVisibility(View.VISIBLE);
            holder.tvAdvertencia.setText("¡Límite superado!");
            holder.tvAdvertencia.setTextColor(Color.RED);
        } else if (porcentaje >= 75) {
            // NARANJA: Advertencia, cerca del límite
            int colorNaranja = Color.parseColor("#FF9800"); // Naranja Material
            holder.pbProgreso.setProgressTintList(ColorStateList.valueOf(colorNaranja));
            holder.tvPorcentaje.setTextColor(colorNaranja);
            holder.tvAdvertencia.setVisibility(View.VISIBLE);
            holder.tvAdvertencia.setText("Cerca del límite (" + porcentaje + "%)");
            holder.tvAdvertencia.setTextColor(colorNaranja);
        } else {
            // VERDE/NORMAL: Usamos el color de la categoría
            holder.pbProgreso.setProgressTintList(ColorStateList.valueOf(colorFiltroCategoria));
            // Restaurar colores por defecto del tema
            holder.tvPorcentaje.setTextColor(ContextCompat.getColor(context, com.google.android.material.R.color.material_dynamic_neutral10));
            holder.tvAdvertencia.setVisibility(View.GONE);
        }

        // 4. Configurar Burbuja de Icono (Igual que siempre)
        if (pres.getCategoria() != null) {
            int resIdIcono = context.getResources().getIdentifier(pres.getCategoria().getIcono(), "drawable", context.getPackageName());
            if (resIdIcono != 0) holder.ivIcono.setImageResource(resIdIcono);
            else holder.ivIcono.setImageResource(android.R.drawable.ic_menu_agenda);

            int colorFondoClarito = Color.argb(40, Color.red(colorFiltroCategoria), Color.green(colorFiltroCategoria), Color.blue(colorFiltroCategoria));
            holder.cardIcono.setCardBackgroundColor(colorFondoClarito);
            holder.ivIcono.setColorFilter(colorFiltroCategoria);
        }

        // 5. Menú Opciones
        holder.ivOpciones.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.ivOpciones);
            popup.getMenu().add("Editar Límite");
            popup.getMenu().add("Eliminar Presupuesto");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Editar Límite") && listener != null) {
                    listener.onEditar(pres);
                } else if (item.getTitle().equals("Eliminar Presupuesto") && listener != null) {
                    listener.onEliminar(pres);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return listaPresupuestos.size();
    }

    public static class PresupuestoViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardIcono;
        ImageView ivIcono, ivOpciones;
        TextView tvNombre, tvAdvertencia, tvGastadoTexto, tvPorcentaje;
        ProgressBar pbProgreso;

        public PresupuestoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardIcono = itemView.findViewById(R.id.card_icono_presupuesto);
            ivIcono = itemView.findViewById(R.id.iv_icono_presupuesto);
            ivOpciones = itemView.findViewById(R.id.iv_opciones_presupuesto);
            tvNombre = itemView.findViewById(R.id.tv_nombre_presupuesto);
            tvAdvertencia = itemView.findViewById(R.id.tv_advertencia_presupuesto);
            tvGastadoTexto = itemView.findViewById(R.id.tv_gastado_texto);
            tvPorcentaje = itemView.findViewById(R.id.tv_porcentaje_presupuesto);
            pbProgreso = itemView.findViewById(R.id.pb_progreso_presupuesto);
        }
    }
}