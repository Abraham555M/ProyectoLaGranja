package com.example.proyectolagranja.ui.Pedidos.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.Venta;
import com.google.android.material.button.MaterialButton;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class PedidosAdapter extends RecyclerView.Adapter<PedidosAdapter.ViewHolder>{
    private Context context;
    private List<Venta> listaVenta;
    private OnPedidoClickListener listener;

    public PedidosAdapter(Context context, List<Venta> listaVenta) {
        this.context = context;
        this.listaVenta = listaVenta;
    }

    public interface OnPedidoClickListener {
        void onCancelarClick(Venta venta);
        void onVerMasClick(Venta venta);
    }

    public void setOnPedidoClickListener(OnPedidoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pedido, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Venta venta = listaVenta.get(position);
        holder.tvNumeroVenta.setText(venta.getNum_venta());
        holder.tvFechaVenta.setText(venta.getFec_venta());
        holder.tvTotal.setText("S/ " + venta.getTot_venta());

        // Listener de botones
        holder.btnCancelarPedido.setOnClickListener(v -> {
            if(listener != null) listener.onCancelarClick(venta);
        });

        holder.btnVerMasPedido.setOnClickListener(v -> {
            if(listener != null) listener.onVerMasClick(venta);
        });

        // Medio de pago
        String medioPagoTexto;
        switch (venta.getId_pago_medio()) {
            case 1: medioPagoTexto = "Efectivo"; break;
            case 2: medioPagoTexto = "Tarjeta"; break;
            case 3: medioPagoTexto = "App"; break;
            case 4: medioPagoTexto = "Transferencia"; break;
            case 5: medioPagoTexto = "Crédito"; break;
            default: medioPagoTexto = "Desconocido"; break;
        }
        holder.tvMedioPago.setText(medioPagoTexto);

        // Estados
        String estadoTexto;
        int colorFondo;
        switch (venta.getAct_venta()) {
            case 0:
                estadoTexto = "Cancelado";
                colorFondo = context.getResources().getColor(R.color.color_cancelar);
                break;
            case 2:
                estadoTexto = "Pendiente";
                colorFondo = context.getResources().getColor(R.color.color_pendiente);
                break;
            case 3:
                estadoTexto = "Despachado";
                colorFondo = context.getResources().getColor(R.color.color_despachado);
                break;
            case 4:
                estadoTexto = "Entregado";
                colorFondo = context.getResources().getColor(R.color.color_entregado);
                break;
            case 5:
                estadoTexto = "Pagado";
                colorFondo = context.getResources().getColor(R.color.color_pagado);
                break;
            default:
                estadoTexto = "Desconocido";
                colorFondo = context.getResources().getColor(android.R.color.darker_gray);
                break;
        }

        // Crear el fondo con bordes
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colorFondo);
        drawable.setCornerRadius(30f);

        holder.tvEstado.setText(estadoTexto);
        holder.tvEstado.setBackground(drawable);
        int padding = 20;
        holder.tvEstado.setPadding(padding, padding/2, padding, padding/2);

        // Bloquear botón si estado es 2 (Pendiente)
        if (venta.getAct_venta() > 2 || venta.getAct_venta() == 0) {
            holder.btnCancelarPedido.setEnabled(false);  // Desactiva el botón
            holder.btnCancelarPedido.setAlpha(0.5f);      // Visualmente se ve deshabilitado
        } else {
            holder.btnCancelarPedido.setEnabled(true);
            holder.btnCancelarPedido.setAlpha(1.0f);
        }

        // Listener de botones
        holder.btnCancelarPedido.setOnClickListener(v -> {
            if (listener != null) listener.onCancelarClick(venta);
        });
    }

    @Override
    public int getItemCount() {
        return listaVenta.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumeroVenta, tvFechaVenta, tvTotal, tvEstado, tvMedioPago;
        MaterialButton btnCancelarPedido, btnVerMasPedido;
        androidx.cardview.widget.CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroVenta = itemView.findViewById(R.id.tvNumeroVenta);
            tvFechaVenta = itemView.findViewById(R.id.tvFechaVenta);
            tvMedioPago = itemView.findViewById(R.id.tvMedioPago);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnCancelarPedido = itemView.findViewById(R.id.btnCancelarPedido);
            btnVerMasPedido = itemView.findViewById(R.id.btnVerMasPedido);
            cardView = (androidx.cardview.widget.CardView) itemView;
        }
    }
}
