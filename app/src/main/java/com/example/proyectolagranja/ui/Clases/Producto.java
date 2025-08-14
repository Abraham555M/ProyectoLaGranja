package com.example.proyectolagranja.ui.Clases;

public class Producto {
    private Integer id_producto;
    private String nom_producto;

    public Producto(Integer id_producto, String nom_producto) {
        this.id_producto = id_producto;
        this.nom_producto = nom_producto;
    }

    public Integer getId_producto() {
        return id_producto;
    }

    public void setId_producto(Integer id_producto) {
        this.id_producto = id_producto;
    }

    public String getNom_producto() {
        return nom_producto;
    }

    public void setNom_producto(String nom_producto) {
        this.nom_producto = nom_producto;
    }

    @Override
    public String toString() {
        return nom_producto;  // ✅ Esto hará que el Spinner muestre el nombre
    }
}
