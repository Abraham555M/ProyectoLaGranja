package com.example.proyectolagranja.ui.Clases;

public class ArticuloDetalle {
    private String nombre;
    private double precio;
    private double cantidad;
    private double subTotal;
    private String imagenUrl;
    private int esPromo;
    private String codPresentacion;
    private double precioOferta;

    public ArticuloDetalle(String nombre, double precio, double cantidad, double subTotal, String imagenUrl, int esPromo, String codPresentacion, double precioOferta) {
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.subTotal = subTotal;
        this.imagenUrl = imagenUrl;
        this.esPromo = esPromo;
        this.codPresentacion = codPresentacion;
        this.precioOferta = precioOferta;
    }

    public int getEsPromo() {
        return esPromo;
    }

    public void setEsPromo(int esPromo) {
        this.esPromo = esPromo;
    }

    public String getCodPresentacion() {
        return codPresentacion;
    }

    public void setCodPresentacion(String codPresentacion) {
        this.codPresentacion = codPresentacion;
    }

    public double getPrecioOferta() {
        return precioOferta;
    }

    public void setPrecioOferta(double precioOferta) {
        this.precioOferta = precioOferta;
    }

    public String getNombre() { return nombre; }
    public double getPrecio() { return precio; }
    public double getCantidad() { return cantidad; }
    public double getSubTotal() { return subTotal; }
    public String getImagenUrl() { return imagenUrl; }
}
