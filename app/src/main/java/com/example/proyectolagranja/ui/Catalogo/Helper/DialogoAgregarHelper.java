package com.example.proyectolagranja.ui.Catalogo.Helper;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyectolagranja.MainActivity;
import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.Articulo;
import com.example.proyectolagranja.ui.Clases.ItemCarrito;
import com.example.proyectolagranja.ui.Servicios.DecimalDigitsInputFilter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class DialogoAgregarHelper {
    // Constants
    private static final int MIN_QUANTITY = 1;

    // UI Components
    private final TextView nombreArticulo, precioArticulo, tvOfertaValida, labelOfertaCantidad;
    private final TextInputEditText etDetalle, etCantidad;
    private final MaterialButton btnAgregar, btnCerrar, btnRestar, btnSumar;
    private final MaterialButton etPromociones, etFavoritos;

    // Data
    private final Context context;
    private final Articulo articulo;
    private final MainActivity mainActivity;

    // State
    private ItemCarrito itemExistente = null;
    private int indexExistente = -1;

    public DialogoAgregarHelper(Context context, Articulo articulo) {
        this.context = context;
        this.articulo = articulo;
        this.mainActivity = (MainActivity) context;

        // Inflar la vista del diálogo
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.alert_dialog_definir_cantidad, null);

        // Inicializar componentes de la UI
        nombreArticulo = dialogView.findViewById(R.id.tvNombreArticulo);
        precioArticulo = dialogView.findViewById(R.id.tvPrecioArticulo);
        etDetalle = dialogView.findViewById(R.id.etDetalle);
        etCantidad = dialogView.findViewById(R.id.etCantidad);
        btnAgregar = dialogView.findViewById(R.id.btnAgregar);
        btnCerrar = dialogView.findViewById(R.id.btnCerrar);
        btnRestar = dialogView.findViewById(R.id.btnRestar);
        btnSumar = dialogView.findViewById(R.id.btnSumar);
        etPromociones = dialogView.findViewById(R.id.etPromociones);
        etFavoritos = dialogView.findViewById(R.id.etFavoritos);
        tvOfertaValida = dialogView.findViewById(R.id.tvOfertaValida);
        labelOfertaCantidad = dialogView.findViewById(R.id.labelOfertaCantidad);

        // Configurar e inicializar la vista
        inicializarVista();

        // Crear y mostrar el diálogo
        crearYMostrarDialogo(dialogView);
    }

    private void inicializarVista() {
        etCantidad.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3)});

        configurarVisibilidadPromociones();
        configurarVisibilidadFavoritos();
        configurarDatosArticulo();
        buscarItemExistente();
    }

    private void configurarVisibilidadPromociones() {
        int visibility = articulo.getEsPromo() == 1 ? View.VISIBLE : View.GONE;
        etPromociones.setVisibility(visibility);
        tvOfertaValida.setVisibility(visibility);
        labelOfertaCantidad.setVisibility(visibility);
    }

    private void configurarVisibilidadFavoritos() {
        etFavoritos.setVisibility(articulo.getEsFavorito() == 1 ? View.VISIBLE : View.GONE);
    }

    private void configurarDatosArticulo() {
        nombreArticulo.setText(articulo.getNombre());
        precioArticulo.setText("S/ " + articulo.getPrecio());
    }

    private void buscarItemExistente() {
        for (int i = 0; i < MainActivity.carrito.size(); i++) {
            if (MainActivity.carrito.get(i).getArticulo().getId().equals(articulo.getId())) {
                itemExistente = MainActivity.carrito.get(i);
                indexExistente = i;
                cargarDatosExistentes();
                break;
            }
        }
    }

    private void cargarDatosExistentes() {
        etCantidad.setText(String.valueOf(itemExistente.getCantidad()));
        etDetalle.setText(itemExistente.getDetalle());
        btnAgregar.setText("Actualizar");
    }

    private void crearYMostrarDialogo(View dialogView) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        configurarEventos(dialog);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void configurarEventos(AlertDialog dialog) {
        btnRestar.setOnClickListener(v -> decrementarCantidad());
        btnSumar.setOnClickListener(v -> incrementarCantidad());
        btnAgregar.setOnClickListener(v -> procesarAgregarArticulo(dialog));
        btnCerrar.setOnClickListener(v -> dialog.dismiss());
    }

    private void decrementarCantidad() {
        String valorStr = etCantidad.getText().toString().trim();
        if (!valorStr.isEmpty()) {
            double valor = Double.parseDouble(valorStr);
            if (valor > MIN_QUANTITY) {
                etCantidad.setText(String.valueOf(valor - 1));
            }
        }
    }

    private void incrementarCantidad() {
        String valorStr = etCantidad.getText().toString().trim();
        double valor = valorStr.isEmpty() ? MIN_QUANTITY : Double.parseDouble(valorStr) + 1;
        etCantidad.setText(String.valueOf(valor));
    }

    private void procesarAgregarArticulo(AlertDialog dialog) {
        String cantidadStr = etCantidad.getText().toString().trim();
        String detalle = etDetalle.getText().toString().trim();

        if (!validarCantidad(cantidadStr)) return;

        double cantidad = Double.parseDouble(cantidadStr);

        if (itemExistente == null) {
            agregarNuevoItem(cantidad, detalle);
        } else {
            actualizarItemExistente(cantidad, detalle);
        }

        mainActivity.actualizarBadge();
        dialog.dismiss();
    }

    private boolean validarCantidad(String cantidadStr) {
        if (cantidadStr.isEmpty()) {
            etCantidad.setError("Ingrese una cantidad");
            return false;
        }

        double cantidad = Double.parseDouble(cantidadStr);
        if (cantidad <= 0) {
            etCantidad.setError("La cantidad debe ser mayor a 0");
            return false;
        }

        return true;
    }

    private void agregarNuevoItem(double cantidad, String detalle) {
        MainActivity.carrito.add(new ItemCarrito(articulo, cantidad, detalle));
        Toast.makeText(context, "Artículo agregado al carrito", Toast.LENGTH_SHORT).show();
    }

    private void actualizarItemExistente(double cantidad, String detalle) {
        itemExistente.setCantidad(cantidad);
        itemExistente.setDetalle(detalle);
        MainActivity.carrito.set(indexExistente, itemExistente);
        Toast.makeText(context, "Artículo actualizado en el carrito", Toast.LENGTH_SHORT).show();
    }
}