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

        // 1. Textos básicos (Nota y Fecha)
        // He añadido la categoría al texto para que quede más completo si no hay nota.
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

        // 3. LÓGICA DE LA CATEGORÍA (¡La Magia Nueva!) 🪄
        if (movimiento.getCategoria() != null) {
            // A. Extraemos el color de la categoría
            int colorCategoria;
            try {
                // Parseamos el HEX que viene del backend (Ej: "#FFCA28")
                colorCategoria = Color.parseColor(movimiento.getCategoria().getColor());
            } catch (Exception e) {
                // Si algo falla, ponemos un gris por defecto
                colorCategoria = ContextCompat.getColor(context, R.color.white_pu);
            }

            // B. Buscamos el icono dinámicamente en la carpeta drawable de Android
            int resIdIcono = context.getResources().getIdentifier(
                    movimiento.getCategoria().getIcono(), // Ej: "ic_payments"
                    "drawable",
                    context.getPackageName()
            );

            // C. Aplicamos los estilos
            if (resIdIcono != 0) {
                holder.ivIcono.setImageResource(resIdIcono);
            } else {
                // Si se te olvidó descargar el icono, ponemos uno genérico
                holder.ivIcono.setImageResource(android.R.drawable.ic_menu_agenda);
            }

            // Pintamos el fondo de la "bolita" con un color clarito (transparente 20%) del color original
            // El color original lo usamos para pintar el icono en sí.
            int colorFondoClarito = Color.argb(40, Color.red(colorCategoria), Color.green(colorCategoria), Color.blue(colorCategoria));

            holder.cardIcono.setCardBackgroundColor(colorFondoClarito);
            holder.ivIcono.setColorFilter(colorCategoria);

        } else {
            // 4. Lógica de Respaldo (Por si hay movimientos antiguos sin categoría en la BBDD)
            holder.cardIcono.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white_pu));
            if (movimiento.getTipo() == MovementType.INGRESO) {
                holder.ivIcono.setImageResource(android.R.drawable.arrow_up_float);
                holder.ivIcono.setColorFilter(ContextCompat.getColor(context, R.color.turquesa_oscuro));
            } else {
                holder.ivIcono.setImageResource(android.R.drawable.arrow_down_float);
                holder.ivIcono.setColorFilter(ContextCompat.getColor(context, R.color.red));
            }
        }

        // 5. LÓGICA VISUAL DE SELECCIÓN (Mantengo tu código original)
        if (movimiento.isSelected()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1A8FE3CF"));
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

        // CLIC NORMAL: Si estamos en modo selección, marca/desmarca.
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