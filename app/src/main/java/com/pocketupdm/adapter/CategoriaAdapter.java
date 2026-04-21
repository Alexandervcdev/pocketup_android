package com.pocketupdm.adapter;
import android.annotation.SuppressLint;
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
import com.pocketupdm.model.Categoria;

import java.util.List;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder> {

    private final Context context;
    private List<Categoria> listaCategorias;
    private final OnCategoriaClickListener listener;
    private OnCategoriaLongClickListener longClickListener; // ¡NUEVO AVISADOR PARA BORRAR!

    // Aquí guardamos la posición de la categoría que el usuario ha tocado
    private int posicionSeleccionada = -1;

    // Interfaz para avisar al BottomSheet cuando toquen una categoría
    public interface OnCategoriaClickListener {
        void onCategoriaClick(Categoria categoria);
    }

    // Interfaz para el click largo (eliminar)
    public interface OnCategoriaLongClickListener {
        void onLongClick(Categoria categoria);
    }

    public CategoriaAdapter(Context context,
                            List<Categoria> listaCategorias,
                            OnCategoriaClickListener listener,
                            OnCategoriaLongClickListener longClickListener) {
        this.context = context;
        this.listaCategorias = listaCategorias;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setCategorias(List<Categoria> nuevasCategorias) {
        this.listaCategorias = nuevasCategorias;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_categoria_selector, parent, false);
        return new CategoriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriaViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Categoria categoria = listaCategorias.get(position);

        // 1. Poner el nombre
        holder.tvNombre.setText(categoria.getNombre());

        // 2. Traducir el HEX (String) a un Color de Android
        int colorFiltro;
        try {
            colorFiltro = Color.parseColor(categoria.getColor());
        } catch (Exception e) {
            colorFiltro = Color.GRAY; // Color por si el servidor manda un HEX mal escrito
        }

        // Tintar el icono con el color de la categoría
        holder.ivIcono.setColorFilter(colorFiltro);

        // 3. Buscar el icono en la carpeta 'drawable' dinámicamente
        int resId = context.getResources().getIdentifier(categoria.getIcono(), "drawable", context.getPackageName());
        if (resId != 0) {
            holder.ivIcono.setImageResource(resId);
        } else {
            // Icono por defecto por si te falta descargar alguno
            holder.ivIcono.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 4. Lógica de SELECCIÓN (Poner el borde si está seleccionado)
        if (posicionSeleccionada == position) {
            holder.cardIcono.setStrokeColor(colorFiltro);
            holder.cardIcono.setStrokeWidth(6); // Borde grueso
        } else {
            holder.cardIcono.setStrokeColor(Color.TRANSPARENT);
            holder.cardIcono.setStrokeWidth(0); // Sin borde
        }

        // 1. Click normal (Seleccionar)
        holder.itemView.setOnClickListener(v -> {
            // Cambiamos la selección actual
            int posicionAnterior = posicionSeleccionada;
            posicionSeleccionada = position;

            // Le decimos al RecyclerView que repinte la vieja y la nueva posición para animar el borde
            notifyItemChanged(posicionAnterior);
            notifyItemChanged(posicionSeleccionada);

            // Avisamos a la pantalla principal
            listener.onCategoriaClick(categoria);
        });

        // 2. Click largo (Eliminar)
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(categoria);
            }
            // Retornamos true para indicar que ya hemos consumido este evento
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaCategorias != null ? listaCategorias.size() : 0;
    }

    public static class CategoriaViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardIcono;
        ImageView ivIcono;
        TextView tvNombre;

        public CategoriaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardIcono = itemView.findViewById(R.id.card_categoria_icono);
            ivIcono = itemView.findViewById(R.id.iv_categoria_icono);
            tvNombre = itemView.findViewById(R.id.tv_categoria_nombre);
        }
    }
}