package com.pocketupdm.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.OnSelectionChangedListener;
import com.pocketupdm.R;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.model.MovementType;

import java.util.ArrayList;
import java.util.List;

public class MovimientoAdapter extends RecyclerView.Adapter<MovimientoAdapter.MovimientoViewHolder> {

    private Context context;
    private List<MovimientoResponse> movimientosList;
    private boolean isSelectionMode = false; // ¿Estamos borrando cosas?
    private OnSelectionChangeListener selectionListener;
    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }
    // Constructor
    public MovimientoAdapter(Context context, List<MovimientoResponse> movimientosList, OnSelectionChangeListener listener) {
        this.context = context;
        // Si la lista es null, inicializamos una vacía para evitar crasheos
        this.movimientosList = movimientosList != null ? movimientosList : new ArrayList<>();
        this.selectionListener = listener;
    }

    // Método para actualizar la lista (útil para cuando descarguemos los datos de la API)
    public void setMovimientos(List<MovimientoResponse> nuevosMovimientos) {
        this.movimientosList = nuevosMovimientos;
        notifyDataSetChanged(); // Avisa a Android que redibuje la lista
    }

    @NonNull
    @Override
    public MovimientoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movimiento, parent, false);
        return new MovimientoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovimientoViewHolder holder, int position) {
        MovimientoResponse movimiento = movimientosList.get(position);

        // 1. Textos básicos
        holder.tvNota.setText(movimiento.getNota() != null && !movimiento.getNota().isEmpty() ? movimiento.getNota() : "Sin descripción");
        holder.tvFecha.setText(movimiento.getFecha());

        // 2. Lógica de diseño: INGRESO vs GASTO
        if (movimiento.getTipo() == MovementType.INGRESO) {
            // Diseño para INGRESO
            holder.tvImporte.setText("+ " + movimiento.getImporte() + " €");
            holder.tvImporte.setTextColor(ContextCompat.getColor(context, R.color.turquesa_oscuro));

            holder.cardIcono.setCardBackgroundColor(ContextCompat.getColor(context, R.color.turquesa));
            // Usamos un icono genérico de suma o flecha arriba (puedes cambiarlo por tus propios iconos XML)
            holder.ivIcono.setImageResource(android.R.drawable.arrow_up_float);
            holder.ivIcono.setColorFilter(ContextCompat.getColor(context, R.color.white));

        } else {
            // Diseño para GASTO
            holder.tvImporte.setText("- " + movimiento.getImporte() + " €");
            holder.tvImporte.setTextColor(ContextCompat.getColor(context, R.color.red));

            // Fondo rojo clarito para el icono (opcional, puedes usar white_pu)
            holder.cardIcono.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white_pu));
            holder.ivIcono.setImageResource(android.R.drawable.arrow_down_float);
            holder.ivIcono.setColorFilter(ContextCompat.getColor(context, R.color.red));
        }

        // LÓGICA VISUAL DE SELECCIÓN
        // Si está seleccionado, pintamos un fondo sutil (ej: turquesa clarito o gris)
        if (movimiento.isSelected()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1A8FE3CF")); // Turquesa con mucha transparencia
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
        // CLIC LARGO: Activa el modo selección
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(position);
            }
            return true;
        });

        // CLIC NORMAL: Si estamos en modo selección, marca/desmarca. Si no, no hace nada (por ahora).
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(position);
            }
        });
    }
    private void toggleSelection(int position) {
        movimientosList.get(position).setSelected(!movimientosList.get(position).isSelected());
        notifyItemChanged(position);

        // Contamos cuántos hay seleccionados
        int count = getSelectedCount();
        if (count == 0) isSelectionMode = false; // Si desmarcamos todo, salimos del modo
        if (selectionListener != null) selectionListener.onSelectionChanged(count);
    }
    public int getSelectedCount() {
        int count = 0;
        for (MovimientoResponse m : movimientosList) {
            if (m.isSelected()) count++;
        }
        return count;
    }
    public List<Long> getSelectedIds() {
        List<Long> ids = new ArrayList<>();
        for (MovimientoResponse m : movimientosList) {
            if (m.isSelected()) ids.add(m.getId());
        }
        return ids;
    }

    public void exitSelectionMode() {
        isSelectionMode = false;
        for (MovimientoResponse m : movimientosList) m.setSelected(false);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return movimientosList.size();
    }

    // Clase interna que "sujeta" las vistas de nuestro molde XML
    public static class MovimientoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNota, tvFecha, tvImporte;
        MaterialCardView cardIcono;
        ImageView ivIcono;

        public MovimientoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNota = itemView.findViewById(R.id.tv_movimiento_nota);
            tvFecha = itemView.findViewById(R.id.tv_movimiento_fecha);
            tvImporte = itemView.findViewById(R.id.tv_movimiento_importe);
            cardIcono = itemView.findViewById(R.id.card_icono_movimiento);
            ivIcono = itemView.findViewById(R.id.iv_icono_movimiento);
        }
    }
}