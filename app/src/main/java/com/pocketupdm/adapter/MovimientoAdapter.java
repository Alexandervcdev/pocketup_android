package com.pocketupdm.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

    // 1. NUEVA INTERFAZ: Escucha los clics de editar y eliminar
    private OnMovimientoOpcionesListener listener;

    public interface OnMovimientoOpcionesListener {
        void onEditar(MovimientoResponse movimiento);
        void onEliminar(MovimientoResponse movimiento);
    }

    // Constructor Actualizado
    public MovimientoAdapter(Context context, List<MovimientoResponse> movimientosList, OnMovimientoOpcionesListener listener) {
        this.context = context;
        this.movimientosList = movimientosList != null ? movimientosList : new ArrayList<>();
        this.listener = listener;
    }

    public void setMovimientos(List<MovimientoResponse> nuevosMovimientos) {
        this.movimientosList = nuevosMovimientos;
        notifyDataSetChanged();
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

        // 1. Textos básicos (Nota y Fecha)
        String notaPrincipal = (movimiento.getNota() != null && !movimiento.getNota().isEmpty())
                ? movimiento.getNota()
                : (movimiento.getCategoria() != null ? movimiento.getCategoria().getNombre() : "Sin descripción");
        holder.tvNota.setText(notaPrincipal);
        holder.tvFecha.setText(movimiento.getFecha());

        // 2. Lógica del Importe (+/- y Color del texto)
        if (movimiento.getTipo() == MovementType.INGRESO) {
            holder.tvImporte.setText("+ " + movimiento.getImporte() + " €");
            holder.tvImporte.setTextColor(ContextCompat.getColor(context, R.color.turquesa_oscuro));
        } else {
            holder.tvImporte.setText("- " + movimiento.getImporte() + " €");
            holder.tvImporte.setTextColor(ContextCompat.getColor(context, R.color.red));
        }

        // 3. LÓGICA DE LA CATEGORÍA
        if (movimiento.getCategoria() != null) {
            int colorCategoria;
            try {
                colorCategoria = Color.parseColor(movimiento.getCategoria().getColor());
            } catch (Exception e) {
                colorCategoria = ContextCompat.getColor(context, R.color.white_pu);
            }

            int resIdIcono = context.getResources().getIdentifier(
                    movimiento.getCategoria().getIcono(),
                    "drawable",
                    context.getPackageName()
            );

            if (resIdIcono != 0) {
                holder.ivIcono.setImageResource(resIdIcono);
            } else {
                holder.ivIcono.setImageResource(android.R.drawable.ic_menu_agenda);
            }

            int colorFondoClarito = Color.argb(40, Color.red(colorCategoria), Color.green(colorCategoria), Color.blue(colorCategoria));

            holder.cardIcono.setCardBackgroundColor(colorFondoClarito);
            holder.ivIcono.setColorFilter(colorCategoria);

        } else {
            holder.cardIcono.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white_pu));
            if (movimiento.getTipo() == MovementType.INGRESO) {
                holder.ivIcono.setImageResource(android.R.drawable.arrow_up_float);
                holder.ivIcono.setColorFilter(ContextCompat.getColor(context, R.color.turquesa_oscuro));
            } else {
                holder.ivIcono.setImageResource(android.R.drawable.arrow_down_float);
                holder.ivIcono.setColorFilter(ContextCompat.getColor(context, R.color.red));
            }
        }

        // 4. LÓGICA DEL MENÚ DE 3 PUNTOS (¡La magia de hoy!)
        if (listener == null) {
            // Si el listener es null (como pasa en tu HomeFragment), OCULTAMOS el ícono
            holder.ivOpciones.setVisibility(View.GONE);
        }else{
            // Si hay un listener (como en tu HistorialFragment), MOSTRAMOS el ícono y su menú
            holder.ivOpciones.setVisibility(View.VISIBLE);
            holder.ivOpciones.setOnClickListener(v -> {
                // Creamos el menú emergente anclado al ícono
                PopupMenu popup = new PopupMenu(context, holder.ivOpciones);
                // Añadimos las opciones (el orden importa)
                popup.getMenu().add("Editar");
                popup.getMenu().add("Eliminar");

                // Escuchamos qué opción elige el usuario
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Editar")) {
                        if (listener != null) listener.onEditar(movimiento);
                    } else if (item.getTitle().equals("Eliminar")) {
                        if (listener != null) listener.onEliminar(movimiento);
                    }
                    return true;
                });

                // Mostramos el menú
                popup.show();
            });
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
        ImageView ivOpciones; // ¡No olvides declarar el nuevo ícono!

        public MovimientoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNota = itemView.findViewById(R.id.tv_movimiento_nota);
            tvFecha = itemView.findViewById(R.id.tv_movimiento_fecha);
            tvImporte = itemView.findViewById(R.id.tv_movimiento_importe);
            cardIcono = itemView.findViewById(R.id.card_icono_movimiento);
            ivIcono = itemView.findViewById(R.id.iv_icono_movimiento);
            ivOpciones = itemView.findViewById(R.id.iv_opciones_movimiento); // Lo vinculamos al XML
        }
    }
}