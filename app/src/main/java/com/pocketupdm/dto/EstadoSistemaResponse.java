package com.pocketupdm.dto; // Ajusta tu paquete

public class EstadoSistemaResponse {
    private boolean mantenimientoActivo;
    private String tituloMantenimiento;
    private String mensajeMantenimiento;

    public boolean isMantenimientoActivo() { return mantenimientoActivo; }
    public void setMantenimientoActivo(boolean mantenimientoActivo) { this.mantenimientoActivo = mantenimientoActivo; }
    public String getTituloMantenimiento() { return tituloMantenimiento; }
    public void setTituloMantenimiento(String tituloMantenimiento) { this.tituloMantenimiento = tituloMantenimiento; }
    public String getMensajeMantenimiento() { return mensajeMantenimiento; }
    public void setMensajeMantenimiento(String mensajeMantenimiento) { this.mensajeMantenimiento = mensajeMantenimiento; }
}