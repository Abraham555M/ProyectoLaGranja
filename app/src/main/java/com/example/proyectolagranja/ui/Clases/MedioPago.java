package com.example.proyectolagranja.ui.Clases;

public class MedioPago {
    private Integer id_pago_medio;
    private String nom_pago_medio;

    public MedioPago(Integer id_pago_medio, String nom_pago_medio) {
        this.id_pago_medio = id_pago_medio;
        this.nom_pago_medio = nom_pago_medio;
    }

    public Integer getId_pago_medio() {
        return id_pago_medio;
    }

    public void setId_pago_medio(Integer id_pago_medio) {
        this.id_pago_medio = id_pago_medio;
    }

    public String getNom_pago_medio() {
        return nom_pago_medio;
    }

    public void setNom_pago_medio(String nom_pago_medio) {
        this.nom_pago_medio = nom_pago_medio;
    }
}
