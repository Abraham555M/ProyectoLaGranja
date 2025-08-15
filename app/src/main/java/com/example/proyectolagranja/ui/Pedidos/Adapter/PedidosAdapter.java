package com.example.proyectolagranja.ui.Pedidos.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.Venta;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class PedidosAdapter extends RecyclerView.Adapter<PedidosAdapter.ViewHolder>{
    private Context context;
    private List<Venta> listaPedidos;

    public PedidosAdapter(Context context, List<Venta> listaPedidos) {
        this.context = context;
        this.listaPedidos = listaPedidos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pedido, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Venta venta = listaPedidos.get(position);
        holder.tvNumeroVenta.setText("Pedido #" + venta.getNum_venta());
        holder.tvFechaVenta.setText("Fecha: " + venta.getFec_venta());
        holder.tvTotal.setText("Total: S/" + venta.getTot_venta());

        String estadoTexto = "Desconocido";
        switch (venta.getAct_venta()) {
            case 1: estadoTexto = "Pendiente"; break;
            case 2: estadoTexto = "Confirmado"; break;
            case 3: estadoTexto = "Enviado"; break;
            case 4: estadoTexto = "Cancelado"; break;
        }
        holder.tvEstado.setText("Estado: " + estadoTexto);
    }

    @Override
    public int getItemCount() {
        return listaPedidos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumeroVenta, tvFechaVenta, tvTotal, tvEstado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroVenta = itemView.findViewById(R.id.tvNumeroVenta);
            tvFechaVenta = itemView.findViewById(R.id.tvFechaVenta);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvEstado = itemView.findViewById(R.id.tvEstado);
        }
    }
}
