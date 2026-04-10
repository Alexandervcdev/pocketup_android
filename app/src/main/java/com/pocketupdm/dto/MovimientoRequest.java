package com.pocketupdm.dto;
import com.pocketupdm.model.MovementType;
import java.math.BigDecimal;

public class MovimientoRequest {
    private BigDecimal importe;
    private String fecha; // Enviamos como String "YYYY-MM-DD"
    private MovementType tipo;
    private String nota;
    private Long usuarioId;

    public MovimientoRequest(BigDecimal importe, String fecha, MovementType tipo, String nota, Long usuarioId) {
        this.importe = importe;
        this.fecha = fecha;
        this.tipo = tipo;
        this.nota = nota;
        this.usuarioId = usuarioId;
    }
}