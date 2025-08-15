package com.example.proyectolagranja.ui.Pedidos.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.ArticuloDetalle;

import java.util.List;

public class ArticuloDetalleAdapter extends RecyclerView.Adapter<ArticuloDetalleAdapter.ViewHolder>{
    private Context context;
    private List<ArticuloDetalle> listaArticulos;

    public ArticuloDetalleAdapter(Context context, List<ArticuloDetalle> listaArticulos) {
        this.context = context;
        this.listaArticulos = listaArticulos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_articulo_vendido, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArticuloDetalle articulo = listaArticulos.get(position);
        holder.nombre.setText(articulo.getNombre());
        holder.precio.setText("S/. " + articulo.getPrecio());
        holder.cantidad.setText("Cantidad: " + articulo.getCantidad());
        holder.subTotal.setText("SubTotal: S/. " + articulo.getSubTotal());

        // Cargar imagen
        Glide.with(context)
                .load(articulo.getImagenUrl())
                .into(holder.imagen);
    }

    @Override
    public int getItemCount() {
        return listaArticulos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagen;
        TextView nombre, precio, cantidad, subTotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagen = itemView.findViewById(R.id.imgArticulo);
            nombre = itemView.findViewById(R.id.nombreArticulo);
            precio = itemView.findViewById(R.id.precioArticulo);
            cantidad = itemView.findViewById(R.id.cantidadArticulo);
            subTotal = itemView.findViewById(R.id.subTotalArticulo);
        }
    }
}
