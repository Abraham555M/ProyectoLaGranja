package com.example.proyectolagranja.ui.Clases;

public class Venta {
    private int id_venta;
    private String num_venta;
    private String fec_venta;
    private int id_pago_medio;
    private int act_venta;
    private double tot_venta;
    private double efec_venta;

    public Venta(int id_venta, String num_venta, String fec_venta, int id_pago_medio, int act_venta, double tot_venta, double efec_venta) {
        this.id_venta = id_venta;
        this.num_venta = num_venta;
        this.fec_venta = fec_venta;
        this.id_pago_medio = id_pago_medio;
        this.act_venta = act_venta;
        this.tot_venta = tot_venta;
        this.efec_venta = efec_venta;
    }

    public double getEfec_venta() {
        return efec_venta;
    }

    public void setEfec_venta(double efec_venta) {
        this.efec_venta = efec_venta;
    }

    public int getId_venta() {
        return id_venta;
    }

    public void setId_venta(int id_venta) {
        this.id_venta = id_venta;
    }

    public String getNum_venta() {
        return num_venta;
    }

    public void setNum_venta(String num_venta) {
        this.num_venta = num_venta;
    }

    public String getFec_venta() {
        return fec_venta;
    }

    public void setFec_venta(String fec_venta) {
        this.fec_venta = fec_venta;
    }

    public int getId_pago_medio() {
        return id_pago_medio;
    }

    public void setId_pago_medio(int id_pago_medio) {
        this.id_pago_medio = id_pago_medio;
    }

    public int getAct_venta() {
        return act_venta;
    }

    public void setAct_venta(int act_venta) {
        this.act_venta = act_venta;
    }

    public double getTot_venta() {
        return tot_venta;
    }

    public void setTot_venta(double tot_venta) {
        this.tot_venta = tot_venta;
    }
}
