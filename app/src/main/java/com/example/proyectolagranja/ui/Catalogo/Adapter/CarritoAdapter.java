package com.example.proyectolagranja.ui.Catalogo.Adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputEditText;

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

    public abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemCarrito item = lista.get(position);

        holder.nombreArticulo.setText(item.getArticulo().getNombre());
        holder.precioArticulo.setText("Precio: S/" + item.getArticulo().getPrecio());
        String cantidadFormateada = String.format(Locale.US, "%.3f", (double) item.getCantidad());
        holder.cantidadArticulo.setText("Cantidad: " + cantidadFormateada);

        double precio = Double.parseDouble(item.getArticulo().getPrecio());
        double subtotal = precio * item.getCantidad();
        String subtotalFormateado = String.format(Locale.US, "%.2f", subtotal); // Redondear
        holder.subTotalArticulo.setText("Subtotal: S/" + subtotalFormateado);

        // URL de imagen
        Glide.with(context)
                .load(item.getArticulo().getImagen())
                .placeholder(R.drawable.logo_la_granja)
                .into(holder.imgArticulo);

        if (item.getArticulo().getEsPromo() == 1) {
            holder.labelOferta.setVisibility(View.VISIBLE);
        } else {
            holder.labelOferta.setVisibility(View.GONE);
        }

        // Limpiar TextWatcher previo antes de asignar
        holder.etDetalleArticulo.removeTextChangedListener(holder.textWatcher);
        // Rellenar si ya tenía valor
        holder.etDetalleArticulo.setText(item.getDetalle());
        // Crear nuevo watcher
        holder.textWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                item.setDetalle(s.toString()); // Guardar en modelo
            }
        };
        holder.etDetalleArticulo.addTextChangedListener(holder.textWatcher);

        // Botón para eliminar artículo del carrito
        holder.btnEliminarArticulo.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                MainActivity.carrito.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, lista.size());

                if (listener != null) {
                    listener.onCarritoChange(calcularTotal());
                }
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
        TextView nombreArticulo, precioArticulo, cantidadArticulo, subTotalArticulo, labelOferta;
        View btnEliminarArticulo;
        TextInputEditText etDetalleArticulo;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgArticulo = itemView.findViewById(R.id.imgArticulo);
            nombreArticulo = itemView.findViewById(R.id.nombreArticulo);
            precioArticulo = itemView.findViewById(R.id.precioArticulo);
            cantidadArticulo = itemView.findViewById(R.id.cantidadArticulo);
            subTotalArticulo = itemView.findViewById(R.id.subTotalArticulo);
            btnEliminarArticulo = itemView.findViewById(R.id.btnEliminarArticulo);
            etDetalleArticulo = itemView.findViewById(R.id.etDetalleArticulo);
            labelOferta = itemView.findViewById(R.id.labelOferta);
        }
    }
}