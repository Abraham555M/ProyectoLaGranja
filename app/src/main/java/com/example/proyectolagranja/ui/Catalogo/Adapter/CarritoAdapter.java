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

        String nombreCompleto = item.getArticulo().getNombre();
        if (item.getArticulo().getCodPresentacion() != null && !item.getArticulo().getCodPresentacion().isEmpty()) {
            nombreCompleto += " " + item.getArticulo().getCodPresentacion();
        }
        holder.nombreArticulo.setText(nombreCompleto);

        try {
            String precioStr = (item.getArticulo().getEsPromo() == 1 &&
                    item.getArticulo().getPrecioOferta() != null &&
                    !item.getArticulo().getPrecioOferta().isEmpty())
                    ? item.getArticulo().getPrecioOferta()
                    : item.getArticulo().getPrecio();

            double precio = Double.parseDouble(precioStr);
            double subtotal = precio * item.getCantidad();

            holder.precioArticulo.setText(String.format(Locale.US, "Precio: S/ %.2f", precio));
            String cantidadFormateada = String.format(Locale.US, "%.3f", item.getCantidad());
            holder.cantidadArticulo.setText("Cantidad: " + cantidadFormateada);
            holder.subTotalArticulo.setText(String.format(Locale.US, "Subtotal: S/ %.2f", subtotal));

        } catch (NumberFormatException e) {
            holder.precioArticulo.setText("Precio: S/ 0.00");
            holder.subTotalArticulo.setText("Subtotal: S/ 0.00");
        }

        // Imagen
        Glide.with(context)
                .load(item.getArticulo().getImagen())
                .placeholder(R.drawable.logo_la_granja)
                .into(holder.imgArticulo);

        holder.labelOferta.setVisibility(item.getArticulo().getEsPromo() == 1 ? View.VISIBLE : View.GONE);

        // TextWatcher
        holder.etDetalleArticulo.removeTextChangedListener(holder.textWatcher);
        holder.etDetalleArticulo.setText(item.getDetalle());
        holder.textWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                item.setDetalle(s.toString());
            }
        };
        holder.etDetalleArticulo.addTextChangedListener(holder.textWatcher);

        // Eliminar artículo
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
            try {
                String precioStr = (item.getArticulo().getEsPromo() == 1 &&
                        item.getArticulo().getPrecioOferta() != null &&
                        !item.getArticulo().getPrecioOferta().isEmpty())
                        ? item.getArticulo().getPrecioOferta()
                        : item.getArticulo().getPrecio();

                double precio = Double.parseDouble(precioStr);
                total += precio * item.getCantidad();
            } catch (NumberFormatException e) {
                // Ignorar si algún precio no es válido
            }
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