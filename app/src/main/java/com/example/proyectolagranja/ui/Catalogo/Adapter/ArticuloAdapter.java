package com.example.proyectolagranja.ui.Catalogo.Adapter;

import android.content.Context;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.Articulo;
import com.google.android.material.button.MaterialButton;

import java.util.List;

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
        String codPresentacion = articulo.getCodPresensacion();

        // Crear SpannableString: nombre normal + cod_presentacion subrayado
        SpannableString textoSpannable = new SpannableString(nombre + " " + codPresentacion);
        int start = nombre.length() + 1; // +1 por el espacio
        int end = start + codPresentacion.length();
        textoSpannable.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.nombre.setText(textoSpannable);
        holder.precio.setText("S/ " + articulo.getPrecio());

        // Mostrar Promociones
        if (articulo.getEsPromo() == 1) {
            holder.etPromociones.setVisibility(View.VISIBLE);
            holder.labelOferta.setVisibility(View.VISIBLE);
            holder.labelOferta.setVisibility(View.VISIBLE);
            holder.tvMensajeKg.setVisibility(View.VISIBLE);

            holder.precioAnteriorArticulo.setText("S/ " + articulo.getPrecio());
            holder.precioAnteriorArticulo.setPaintFlags(
                    holder.precioAnteriorArticulo.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );

        } else {
            holder.etPromociones.setVisibility(View.GONE);
            holder.labelOferta.setVisibility(View.GONE);
            holder.labelOferta.setVisibility(View.GONE);
            holder.tvMensajeKg.setVisibility(View.GONE);
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
                .placeholder(R.drawable.logo_la_granja) // mientras carga
                .error(R.drawable.logo_la_granja)       // si falla
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