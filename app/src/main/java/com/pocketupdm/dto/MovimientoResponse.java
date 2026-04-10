package com.pocketupdm.dto;

import com.pocketupdm.model.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimientoResponse {
    private Long id;
    private BigDecimal importe;
    private LocalDate fecha;
    private MovementType tipo;
    private String nota;
    private Long usuarioId;

    public Long getId() {
        return id;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public LocalDate getFecha() {
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
}
