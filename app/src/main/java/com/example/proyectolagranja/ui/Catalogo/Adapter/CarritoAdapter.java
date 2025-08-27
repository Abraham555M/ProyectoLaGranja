package com.example.proyectolagranja.ui.Catalogo.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proyectolagranja.MainActivity;
import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.ItemCarrito;

import java.util.List;
import java.util.Locale;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {
    private Context context;
    private List<ItemCarrito> lista;
    private OnCarritoChangeListener listener; // Para los constantes cambios en el carrito

    public CarritoAdapter(Context context, List<ItemCarrito> lista, OnCarritoChangeListener listener) {
        this.context = context;
        this.lista = lista;
        this.listener = listener;

        if (listener != null) {
            listener.onCarritoChange(calcularTotal()); // Muestra total al inicio
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_articulo_carrito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemCarrito item = lista.get(position);

        holder.nombreArticulo.setText(item.getArticulo().getNombre());
        holder.precioArticulo.setText("Precio: S/" + item.getArticulo().getPrecio());
        holder.cantidadArticulo.setText("Cantidad: " + item.getCantidad());

        double precio = Double.parseDouble(item.getArticulo().getPrecio());
        double subtotal = precio * item.getCantidad();
        String subtotalFormateado = String.format(Locale.US, "%.2f", subtotal); // Redondear
        holder.subTotalArticulo.setText("Subtotal: S/" + subtotalFormateado);

        // URL de imagen
        Glide.with(context)
                .load(item.getArticulo().getImagen())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgArticulo);

        // Botón para eliminar artículo del carrito
        holder.btnEliminarArticulo.setOnClickListener(v -> {
            MainActivity.carrito.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, lista.size());

            if (listener != null) {
                listener.onCarritoChange(calcularTotal());
            }
        });
    }

    public interface OnCarritoChangeListener {
        void onCarritoChange(double total);
    }

    private double calcularTotal() {
        double total = 0;
        for (ItemCarrito item : lista) {
            double precio = Double.parseDouble(item.getArticulo().getPrecio());
            total += precio * item.getCantidad();
        }
        return total;
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgArticulo;
        TextView nombreArticulo, precioArticulo, cantidadArticulo, subTotalArticulo;
        View btnEliminarArticulo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgArticulo = itemView.findViewById(R.id.imgArticulo);
            nombreArticulo = itemView.findViewById(R.id.nombreArticulo);
            precioArticulo = itemView.findViewById(R.id.precioArticulo);
            cantidadArticulo = itemView.findViewById(R.id.cantidadArticulo);
            subTotalArticulo = itemView.findViewById(R.id.subTotalArticulo);
            btnEliminarArticulo = itemView.findViewById(R.id.btnEliminarArticulo);
        }
    }
}