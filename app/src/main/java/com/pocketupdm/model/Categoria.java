package com.pocketupdm.model;

import java.io.Serializable;

public class Categoria implements Serializable {
    private Long id;
    private String nombre;
    private String icono; // Aquí vendrá "ic_restaurant", "ic_car", etc.
    private String color; // Aquí vendrá el Hex "#FF7043"
    private Usuario usuario;

    public Categoria() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}