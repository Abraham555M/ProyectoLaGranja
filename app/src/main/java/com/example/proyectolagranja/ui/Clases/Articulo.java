package com.example.proyectolagranja.ui.Clases;
public class Articulo {
    private String id;
    private String nombre;
    private String precio;       // prec_vent1_articulo
    private String precioOferta; // prec_promo_articulo
    private String imagen;       // foto_articulo
    private int esPromo;         // est_promo_articulo
    private int totalComprado;   // total_comprado
    private int esFavorito;      // es_favorito
    private String codPresentacion; // cod_presentacion

    public Articulo(String id, String nombre, String precio, String precioOferta,
                    String imagen, int esPromo, int totalComprado, int esFavorito, String codPresentacion) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.precioOferta = precioOferta;
        this.imagen = imagen;
        this.esPromo = esPromo;
        this.totalComprado = totalComprado;
        this.esFavorito = esFavorito;
        this.codPresentacion = codPresentacion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public String getPrecioOferta() {
        return precioOferta;
    }

    public void setPrecioOferta(String precioOferta) {
        this.precioOferta = precioOferta;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public int getEsPromo() {
        return esPromo;
    }

    public void setEsPromo(int esPromo) {
        this.esPromo = esPromo;
    }

    public int getTotalComprado() {
        return totalComprado;
    }

    public void setTotalComprado(int totalComprado) {
        this.totalComprado = totalComprado;
    }

    public int getEsFavorito() {
        return esFavorito;
    }

    public void setEsFavorito(int esFavorito) {
        this.esFavorito = esFavorito;
    }

    public String getCodPresentacion() {
        return codPresentacion;
    }

    public void setCodPresentacion(String codPresentacion) {
        this.codPresentacion = codPresentacion;
    }
}

