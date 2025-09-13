package com.example.proyectolagranja.ui.Clases;

public class ArticuloDetalle {
    private String nombre;
    private double precio;
    private double cantidad;
    private double subTotal;
    private String imagenUrl;

    public ArticuloDetalle(String nombre, double precio, double cantidad, double subTotal, String imagenUrl) {
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.subTotal = subTotal;
        this.imagenUrl = imagenUrl;
    }

    public String getNombre() { return nombre; }
    public double getPrecio() { return precio; }
    public double getCantidad() { return cantidad; }
    public double getSubTotal() { return subTotal; }
    public String getImagenUrl() { return imagenUrl; }
}
