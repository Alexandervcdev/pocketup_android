package com.pocketupdm.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

public class Presupuesto implements Serializable {
    private Long id;
    private BigDecimal montoLimite;
    private BigDecimal montoGastado; // El backend lo enviará calculado
    private String fechaInicio;      // YYYY-MM-DD
    private String fechaFin;         // YYYY-MM-DD

    private Categoria categoria;

    @SerializedName("usuarioId")
    private Long usuarioId;

    @SerializedName("categoriaId")
    private Long categoriaId;

    public Presupuesto() {}

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMontoLimite() { return montoLimite; }
    public void setMontoLimite(BigDecimal montoLimite) { this.montoLimite = montoLimite; }

    public BigDecimal getMontoGastado() { return montoGastado; }
    public void setMontoGastado(BigDecimal montoGastado) { this.montoGastado = montoGastado; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
}