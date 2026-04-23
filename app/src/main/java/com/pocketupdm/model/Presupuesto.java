package com.pocketupdm.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

public class Presupuesto implements Serializable {
    private Long id;
    private BigDecimal montoLimite;

    // El backend nos devolverá el objeto Categoría completo
    // para que podamos pintar su icono y nombre en la UI
    private Categoria categoria;

    @SerializedName("usuarioId")
    private Long usuarioId;

    @SerializedName("categoriaId")
    private Long categoriaId;
    private BigDecimal montoGastado = BigDecimal.ZERO;


    public Presupuesto() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMontoLimite() { return montoLimite; }
    public void setMontoLimite(BigDecimal montoLimite) { this.montoLimite = montoLimite; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }


    public BigDecimal getMontoGastado() { return montoGastado; }
    public void setMontoGastado(BigDecimal montoGastado) { this.montoGastado = montoGastado; }
}