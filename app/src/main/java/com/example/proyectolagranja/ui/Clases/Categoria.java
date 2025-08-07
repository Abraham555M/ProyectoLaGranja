package com.example.proyectolagranja.ui.Clases;

public class Categoria {
    private String id_categoria;
    private String nom_categoria;

    public Categoria(String id_categoria, String nom_categoria) {
        this.id_categoria = id_categoria;
        this.nom_categoria = nom_categoria;
    }

    public String getNom_categoria() {
        return nom_categoria;
    }

    public void setNom_categoria(String nom_categoria) {
        this.nom_categoria = nom_categoria;
    }

    public String getId_categoria() {
        return id_categoria;
    }

    public void setId_categoria(String id_categoria) {
        this.id_categoria = id_categoria;
    }

    @Override
    public String toString() {
        return nom_categoria;  // ✅ Esto hará que el Spinner muestre el nombre
    }
}

