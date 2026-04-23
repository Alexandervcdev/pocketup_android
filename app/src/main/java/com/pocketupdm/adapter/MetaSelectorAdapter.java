package com.pocketupdm.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.pocketupdm.R;
import com.pocketupdm.model.Meta;

import java.util.List;

public class MetaSelectorAdapter extends RecyclerView.Adapter<MetaSelectorAdapter.MetaViewHolder> {

    private final Context context;
    private final List<Meta> metas;
    private int posicionSeleccionada = -1; // -1 significa que ninguna está seleccionada al principio
    private final OnMetaSeleccionadaListener listener;

    public interface OnMetaSeleccionadaListener {
        void onMetaSeleccionada(Meta meta);
    }

    public MetaSelectorAdapter(Context context, List<Meta> metas, OnMetaSeleccionadaListener listener) {
        this.context = context;
        this.metas = metas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MetaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meta_selector, parent, false);
        return new MetaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetaViewHolder holder, int position) {
        Meta meta = metas.get(position);
        holder.tvNombre.setText(meta.getNombre());

        // 1. Extraer el color y el icono
        int colorBase = Color.GRAY;
        try { if (meta.getColor() != null) colorBase = Color.parseColor(meta.getColor()); } catch (Exception ignored) {}

        int resId = context.getResources().getIdentifier(meta.getIcono(), "drawable", context.getPackageName());
        if (resId != 0) holder.ivIcono.setImageResource(resId);

        holder.ivIcono.setColorFilter(colorBase);

        // 2. ¡LÓGICA VISUAL DE SELECCIÓN!
        if (posicionSeleccionada == position) {
            // Meta seleccionada: Borde grueso del color de la meta y fondo translúcido
            holder.cardMeta.setStrokeWidth(5);
            holder.cardMeta.setStrokeColor(colorBase);
            holder.cardMeta.setCardBackgroundColor(Color.argb(30, Color.red(colorBase), Color.green(colorBase), Color.blue(colorBase)));
        } else {
            // Meta deseleccionada: Borde gris finito y fondo transparente
            holder.cardMeta.setStrokeWidth(2);
            holder.cardMeta.setStrokeColor(Color.LTGRAY);
            holder.cardMeta.setCardBackgroundColor(Color.TRANSPARENT);
        }

        // 3. Click Listener
        holder.itemView.setOnClickListener(v -> {
            int posicionAnterior = posicionSeleccionada;
            posicionSeleccionada = holder.getAdapterPosition();

            // Recargamos visualmente solo las dos tarjetas que cambiaron de estado
            notifyItemChanged(posicionAnterior);
            notifyItemChanged(posicionSeleccionada);

            listener.onMetaSeleccionada(meta);
        });
    }

    @Override
    public int getItemCount() {
        return metas.size();
    }

    public static class MetaViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardMeta;
        ImageView ivIcono;
        TextView tvNombre;

        public MetaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMeta = itemView.findViewById(R.id.card_meta_selector);
            ivIcono = itemView.findViewById(R.id.iv_icono_meta_selector);
            tvNombre = itemView.findViewById(R.id.tv_nombre_meta_selector);
        }
    }
}