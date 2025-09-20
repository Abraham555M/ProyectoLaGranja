package com.example.proyectolagranja.ui.Catalogo.Adapter;

import android.content.Context;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
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
import com.example.proyectolagranja.ui.Clases.Articulo;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class ArticuloAdapter extends RecyclerView.Adapter<ArticuloAdapter.ArticuloViewHolder>{
    private Context context;
    private List<Articulo> listaArticulos;
    private OnAgregarClickListener listener;

    public ArticuloAdapter(Context context, List<Articulo> listaArticulos, OnAgregarClickListener listener) {
        this.context = context;
        this.listaArticulos = listaArticulos;
        this.listener = listener;
    }

    public interface OnAgregarClickListener {
        void onAgregarClick(Articulo articulo);
    }


    @NonNull
    @Override
    public ArticuloAdapter.ArticuloViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(context).inflate(R.layout.item_articulo_catalogo, parent, false);
        return new ArticuloViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticuloAdapter.ArticuloViewHolder holder, int position) {
        Articulo articulo = listaArticulos.get(position);

        String nombre = articulo.getNombre();
        String codPresentacion = articulo.getCodPresentacion();

        // Crear SpannableString: nombre normal + cod_presentacion subrayado
        SpannableString textoSpannable = new SpannableString(nombre + " " + codPresentacion);
        int start = nombre.length() + 1; // +1 por el espacio
        int end = start + codPresentacion.length();
        textoSpannable.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.nombre.setText(textoSpannable);

        try {
            double precioNormal = Double.parseDouble(articulo.getPrecio());
            double precioOferta = Double.parseDouble(articulo.getPrecioOferta());

            if (articulo.getEsPromo() == 1) {
                // Mostrar el precio promocional como oficial
                holder.precio.setText(String.format(Locale.US, "S/ %.2f", precioOferta));

                // Mostrar el precio normal tachado
                holder.precioAnteriorArticulo.setVisibility(View.VISIBLE);
                holder.precioAnteriorArticulo.setText(String.format(Locale.US, "S/ %.2f", precioNormal));
                holder.precioAnteriorArticulo.setPaintFlags(
                        holder.precioAnteriorArticulo.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );

                // Mostrar etiquetas relacionadas a la promo
                //holder.etPromociones.setVisibility(View.VISIBLE);
                holder.labelOferta.setVisibility(View.VISIBLE);
                holder.tvMensajeKg.setVisibility(View.VISIBLE);

            } else {
                // Solo mostrar precio normal
                holder.precio.setText(String.format(Locale.US, "S/ %.2f", precioNormal));
                holder.precioAnteriorArticulo.setVisibility(View.GONE);

                // Ocultar etiquetas de promo
               //holder.etPromociones.setVisibility(View.GONE);
                holder.labelOferta.setVisibility(View.GONE);
                holder.tvMensajeKg.setVisibility(View.GONE);
            }
        } catch (NumberFormatException e) {
            holder.precio.setText("S/ 0.00");
            holder.precioAnteriorArticulo.setVisibility(View.GONE);
        }

        // Mostrar Favoritos
        if (articulo.getEsFavorito() == 1) {
            holder.etFavoritos.setVisibility(View.VISIBLE);
        } else {
            holder.etFavoritos.setVisibility(View.GONE);
        }

        // Cargar imagen
        Glide.with(context)
                .load(articulo.getImagen())
                .placeholder(R.drawable.logo_la_granja)
                .error(R.drawable.logo_la_granja)
                .into(holder.imagen);

        holder.btnAgregar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAgregarClick(articulo);
            }
        });
    }


    @Override
    public int getItemCount() {
        return listaArticulos.size();
    }

    public static class ArticuloViewHolder extends RecyclerView.ViewHolder {
        ImageView imagen;
        TextView nombre, precio;
        MaterialButton btnAgregar, etPromociones, etFavoritos;
        TextView labelOferta, precioAnteriorArticulo, tvMensajeKg;

        public ArticuloViewHolder(@NonNull View itemView) {
            super(itemView);
            imagen = itemView.findViewById(R.id.imgArticulo);
            nombre = itemView.findViewById(R.id.nombreArticulo);
            precio = itemView.findViewById(R.id.precioArticulo);
            btnAgregar = itemView.findViewById(R.id.btnAgregarCantidad);
            etPromociones = itemView.findViewById(R.id.etPromociones);
            etFavoritos = itemView.findViewById(R.id.etFavoritos);
            labelOferta = itemView.findViewById(R.id.labelOferta);
            precioAnteriorArticulo = itemView.findViewById(R.id.precioAnteriorArticulo);
            tvMensajeKg = itemView.findViewById(R.id.tvMensajeKg);
        }
    }
}