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

    // Solo necesitamos UN listener ahora
    private final OnCategoriaClickListener listener;

    private int posicionSeleccionada = -1;

    // Interfaz única para avisar al BottomSheet
    public interface OnCategoriaClickListener {
        void onCategoriaClick(Categoria categoria);
    }

    // CONSTRUCTOR ACTUALIZADO: Solo pide 3 cosas
    public CategoriaAdapter(Context context,
                            List<Categoria> listaCategorias,
                            OnCategoriaClickListener listener) {
        this.context = context;
        this.listaCategorias = listaCategorias;
        this.listener = listener;
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

        // 2. Traducir el HEX
        int colorFiltro;
        try {
            colorFiltro = Color.parseColor(categoria.getColor());
        } catch (Exception e) {
            colorFiltro = Color.GRAY;
        }

        holder.ivIcono.setColorFilter(colorFiltro);

        // 3. Buscar el icono
        int resId = context.getResources().getIdentifier(categoria.getIcono(), "drawable", context.getPackageName());
        if (resId != 0) {
            holder.ivIcono.setImageResource(resId);
        } else {
            holder.ivIcono.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 4. Lógica de SELECCIÓN VISUAL (Borde)
        if (posicionSeleccionada == position) {
            holder.cardIcono.setStrokeColor(colorFiltro);
            holder.cardIcono.setStrokeWidth(6);
        } else {
            holder.cardIcono.setStrokeColor(Color.TRANSPARENT);
            holder.cardIcono.setStrokeWidth(0);
        }

        // 5. ¡CLIC ÚNICO Y LIMPIO!
        holder.itemView.setOnClickListener(v -> {
            int posicionAnterior = posicionSeleccionada;
            posicionSeleccionada = position;

            notifyItemChanged(posicionAnterior);
            notifyItemChanged(posicionSeleccionada);

            // Le pasamos la pelota al BottomSheet (Él decidirá si estamos en Modo Edición o no)
            listener.onCategoriaClick(categoria);
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