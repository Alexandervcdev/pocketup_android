package com.pocketupdm.dto;

public class PersonajeResponse {
    private Long id;
    private String nombre;
    private Integer nivel;
    private Integer xp;
    private String rango;
    private Integer skinActiva;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getNivel() { return nivel; }
    public void setNivel(Integer nivel) { this.nivel = nivel; }

    public Integer getXp() { return xp; }
    public void setXp(Integer xp) { this.xp = xp; }

    public String getRango() { return rango; }
    public void setRango(String rango) { this.rango = rango; }

    public Integer getSkinActiva() { return skinActiva; }
    public void setSkinActiva(Integer skinActiva) { this.skinActiva = skinActiva; }
}