package com.example.proyectolagranja.ui.Clases;

public class Articulo {
    private String id;
    private String nombre;
    private String precio;
    private String imagen;
    private int esPromo;
    private int totalComprado;

    public Articulo(String id, String nombre, String precio, String imagen) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.imagen = imagen;
    }

    public Articulo(String id, String nombre, String precio, String imagen, int esPromo, int totalComprado) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.imagen = imagen;
        this.esPromo = esPromo;
        this.totalComprado = totalComprado;
    }


    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getPrecio() { return precio; }
    public String getImagen() { return imagen; }
    public int getEsPromo() { return esPromo; }
    public int getTotalComprado() { return totalComprado; }


}
