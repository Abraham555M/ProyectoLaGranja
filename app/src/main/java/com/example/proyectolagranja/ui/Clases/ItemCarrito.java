package com.example.proyectolagranja.ui.Clases;

public class ItemCarrito {
    private Articulo articulo;
    private int cantidad;
    private String detalle;

    public ItemCarrito(Articulo articulo, int cantidad, String detalle) {
        this.articulo = articulo;
        this.cantidad = cantidad;
        this.detalle = detalle;
    }

    public Articulo getArticulo() { return articulo; }
    public int getCantidad() { return cantidad; }
    public String getDetalle() { return detalle; }
}