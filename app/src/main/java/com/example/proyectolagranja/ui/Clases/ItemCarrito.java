package com.example.proyectolagranja.ui.Clases;

public class ItemCarrito {
    private Articulo articulo;
    private double cantidad;
    private String detalle;

    public ItemCarrito(Articulo articulo, double cantidad, String detalle) {
        this.articulo = articulo;
        this.cantidad = cantidad;
        this.detalle = detalle;
    }

    public Articulo getArticulo() { return articulo; }
    public double getCantidad() { return cantidad; }
    public String getDetalle() { return detalle; }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }
}