package com.pocketupdm.dto;
import com.pocketupdm.model.MovementType;
import java.math.BigDecimal;

public class MovimientoRequest {
    private BigDecimal importe;
    private String fecha; // Enviamos como String "YYYY-MM-DD"
    private MovementType tipo;
    private String nota;
    private Long usuarioId;
    private Long categoriaId; //Para que Spring sepa a qué categoría pertenece

    public MovimientoRequest(BigDecimal importe, String fecha, MovementType tipo, String nota, Long usuarioId, Long categoriaId) {
        this.importe = importe;
        this.fecha = fecha;
        this.tipo = tipo;
        this.nota = nota;
        this.usuarioId = usuarioId;
        this.categoriaId = categoriaId;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public MovementType getTipo() {
        return tipo;
    }

    public void setTipo(MovementType tipo) {
        this.tipo = tipo;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }
}