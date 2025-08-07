package com.example.proyectolagranja.ui.Clases;

public class Articulo {
    private String id;
    private String nombre;
    private String precio;
    private String imagen;

    public Articulo(String id, String nombre, String precio, String imagen) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.imagen = imagen;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getPrecio() { return precio; }
    public String getImagen() { return imagen; }
}
