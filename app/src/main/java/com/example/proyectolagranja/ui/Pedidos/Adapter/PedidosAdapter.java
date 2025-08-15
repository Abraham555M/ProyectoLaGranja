package com.example.proyectolagranja.ui.Pedidos.Adapter;

import android.content.Context;
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
        holder.tvNumeroVenta.setText("Pedido #" + venta.getNum_venta());
        holder.tvFechaVenta.setText("Fecha: " + venta.getFec_venta());
        holder.tvTotal.setText("Total: S/" + venta.getTot_venta());

        // Listener de botones
        holder.btnCancelarPedido.setOnClickListener(v -> {
            if(listener != null) listener.onCancelarClick(venta);
        });

        holder.btnVerMasPedido.setOnClickListener(v -> {
            if(listener != null) listener.onVerMasClick(venta);
        });

        // Para el mensaje de los estados
        String estadoTexto = "Desconocido";
        switch (venta.getAct_venta()) {
            case 1: estadoTexto = "Registrando"; break; // No se está usando
            case 2: estadoTexto = "Pendiente"; break;
            case 3: estadoTexto = "Despachado"; break;
            case 4: estadoTexto = "Entregado"; break;
            case 5: estadoTexto = "Pagado"; break;
        }
        holder.tvEstado.setText("Estado: " + estadoTexto);
    }

    @Override
    public int getItemCount() {
        return listaVenta.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumeroVenta, tvFechaVenta, tvTotal, tvEstado;
        MaterialButton btnCancelarPedido, btnVerMasPedido;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumeroVenta = itemView.findViewById(R.id.tvNumeroVenta);
            tvFechaVenta = itemView.findViewById(R.id.tvFechaVenta);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnCancelarPedido = itemView.findViewById(R.id.btnCancelarPedido);
            btnVerMasPedido = itemView.findViewById(R.id.btnVerMasPedido);
        }
    }
}
