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
        holder.nombre.setText(articulo.getNombre());
        holder.precio.setText("S/ " + articulo.getPrecio());

        // Mostrar Promociones
        if (articulo.getEsPromo() == 1) {
            holder.etPromociones.setVisibility(View.VISIBLE);
        } else {
            holder.etPromociones.setVisibility(View.GONE);
        }

        // Mostrar Favoritos
        if (articulo.getTotalComprado() > 2) {
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

        public ArticuloViewHolder(@NonNull View itemView) {
            super(itemView);
            imagen = itemView.findViewById(R.id.imgArticulo);
            nombre = itemView.findViewById(R.id.nombreArticulo);
            precio = itemView.findViewById(R.id.precioArticulo);
            btnAgregar = itemView.findViewById(R.id.btnAgregarCantidad);
            etPromociones = itemView.findViewById(R.id.etPromociones);
            etFavoritos = itemView.findViewById(R.id.etFavoritos);
        }
    }
}