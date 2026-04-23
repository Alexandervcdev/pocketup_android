package com.pocketupdm.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketupdm.R;
import com.pocketupdm.adapter.CategoriaAdapter;
import com.pocketupdm.model.Categoria;
import com.pocketupdm.model.Presupuesto;
import com.pocketupdm.network.RetrofitClient;
import com.pocketupdm.utils.SessionManager;

import java.math.BigDecimal;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevoPresupuestoBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvCategorias;
    private TextInputEditText etLimite;
    private MaterialButton btnGuardar;
    private TextView tvTitulo;

    private Long categoriaSeleccionadaId = null;
    private SessionManager sessionManager;
    private Presupuesto presupuestoAEditar = null;

    public interface OnPresupuestoGuardadoListener {
        void onGuardado();
    }
    private OnPresupuestoGuardadoListener listener;

    public void setListener(OnPresupuestoGuardadoListener listener) { this.listener = listener; }
    public void setPresupuestoAEditar(Presupuesto p) { this.presupuestoAEditar = p; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_nuevo_presupuesto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        rvCategorias = view.findViewById(R.id.rv_categorias_presupuesto);
        etLimite = view.findViewById(R.id.et_presupuesto_limite);
        btnGuardar = view.findViewById(R.id.btn_guardar_presupuesto);
        tvTitulo = view.findViewById(R.id.tv_titulo_sheet_presupuesto);

        rvCategorias.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 1. Cargar categorías para el selector
        cargarCategorias();

        // 2. Modo Edición
        if (presupuestoAEditar != null) {
            tvTitulo.setText("Editar Límite");
            btnGuardar.setText("Actualizar");
            etLimite.setText(String.valueOf(presupuestoAEditar.getMontoLimite()));
            categoriaSeleccionadaId = presupuestoAEditar.getCategoriaId();
            // Nota: En edición podrías deshabilitar el cambio de categoría si quisieras
        }

        btnGuardar.setOnClickListener(v -> guardarPresupuesto());
    }

    private void cargarCategorias() {
        RetrofitClient.getApiService().obtenerCategorias(sessionManager.getUsuarioId()).enqueue(new Callback<List<Categoria>>() {
            @Override
            public void onResponse(Call<List<Categoria>> call, Response<List<Categoria>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CategoriaAdapter adapter = new CategoriaAdapter(getContext(), response.body(), categoria -> {
                        categoriaSeleccionadaId = categoria.getId();
                    });
                    rvCategorias.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<Categoria>> call, Throwable t) {}
        });
    }

    private void guardarPresupuesto() {
        String limiteStr = etLimite.getText().toString().trim();

        if (categoriaSeleccionadaId == null) {
            Toast.makeText(getContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }
        if (limiteStr.isEmpty()) {
            etLimite.setError("Requerido");
            return;
        }

        BigDecimal limite = new BigDecimal(limiteStr.replace(",", "."));

        Presupuesto p = new Presupuesto();
        p.setMontoLimite(limite);
        p.setCategoriaId(categoriaSeleccionadaId);
        p.setUsuarioId(sessionManager.getUsuarioId());

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        Call<Presupuesto> call = (presupuestoAEditar != null)
                ? RetrofitClient.getApiService().actualizarPresupuesto(presupuestoAEditar.getId(), p)
                : RetrofitClient.getApiService().crearPresupuesto(p);

        call.enqueue(new Callback<Presupuesto>() {
            @Override
            public void onResponse(Call<Presupuesto> call, Response<Presupuesto> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    if (listener != null) listener.onGuardado();
                    dismiss();
                } else {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar");
                    Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Presupuesto> call, Throwable t) {
                if (!isAdded()) return;
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}