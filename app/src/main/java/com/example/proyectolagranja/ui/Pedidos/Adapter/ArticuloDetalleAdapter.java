package com.example.proyectolagranja.ui.Pedidos.Adapter;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
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
import java.util.Locale;

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

        // 🔹 Nombre + código presentación subrayado
        String nombreConPresentacion = articulo.getNombre() + " " + articulo.getCodPresentacion();
        SpannableString spannableNombre = new SpannableString(nombreConPresentacion);
        int start = nombreConPresentacion.lastIndexOf(articulo.getCodPresentacion());
        int end = nombreConPresentacion.length();
        if (start >= 0) {
            spannableNombre.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.nombre.setText(spannableNombre);

        // 🔹 Manejo de precios con promoción
        double precioNormal = articulo.getPrecio();
        double precioOferta = articulo.getPrecioOferta();
        int esPromo = articulo.getEsPromo();

        double precioFinal;

// Si está en promo y el precio de oferta es válido, usar ese
        if (esPromo == 1 && precioOferta > 0) {
            precioFinal = precioOferta;
        } else {
            precioFinal = precioNormal;
        }

        String precioFormateado = "S/. " + String.format("%.2f", precioFinal);
        holder.precio.setText(precioFormateado);

        // 🔹 Cantidad y subtotal
        String cantidadFormateada = String.format(Locale.US, "%.3f", articulo.getCantidad());
        String subTotalFormateado = String.format("%.2f", articulo.getSubTotal());

        holder.cantidad.setText("Cantidad: " + cantidadFormateada);
        holder.subTotal.setText("SubTotal: S/. " + subTotalFormateado);

        // 🔹 Imagen con Glide
        if (articulo.getImagenUrl() != null && !articulo.getImagenUrl().isEmpty()) {
            Glide.with(context)
                    .load(articulo.getImagenUrl())
                    .placeholder(R.drawable.logo_la_granja)
                    .error(R.drawable.logo_la_granja)
                    .into(holder.imagen);
        } else {
            holder.imagen.setImageResource(R.drawable.logo_la_granja);
        }
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
