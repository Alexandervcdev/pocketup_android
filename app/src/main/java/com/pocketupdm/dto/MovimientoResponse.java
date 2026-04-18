package com.pocketupdm.dto;

import com.pocketupdm.model.Categoria;
import com.pocketupdm.model.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimientoResponse {
    private Long id;
    private BigDecimal importe;
    private String fecha;
    private MovementType tipo;
    private String nota;
    private Long usuarioId;
    private Categoria categoria;
    private boolean isSelected = false; // Solo para la lógica de la UI

    public Long getId() {
        return id;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public String getFecha() {
        return fecha;
    }

    public MovementType getTipo() {
        return tipo;
    }

    public String getNota() {
        return nota;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}
