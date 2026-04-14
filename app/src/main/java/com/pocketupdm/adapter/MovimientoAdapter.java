package com.pocketupdm.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.pocketupdm.R;
import com.pocketupdm.dto.MovimientoResponse;
import com.pocketupdm.model.MovementType;

import java.util.ArrayList;
import java.util.List;

public class MovimientoAdapter extends RecyclerView.Adapter<MovimientoAdapter.MovimientoViewHolder> {

    private Context context;
    private List<MovimientoResponse> movimientosList;

    // Constructor
    public MovimientoAdapter(Context context, List<MovimientoResponse> movimientosList) {
        this.context = context;
        // Si la lista es null, inicializamos una vacía para evitar crasheos
        this.movimientosList = movimientosList != null ? movimientosList : new ArrayList<>();
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