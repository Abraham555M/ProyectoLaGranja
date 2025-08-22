package com.example.proyectolagranja.ui.Catalogo;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.MainActivity;
import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Catalogo.Adapter.ArticuloAdapter;
import com.example.proyectolagranja.ui.Clases.Articulo;
import com.example.proyectolagranja.ui.Clases.Categoria;
import com.example.proyectolagranja.ui.Clases.ItemCarrito;
import com.example.proyectolagranja.ui.Clases.Producto;
import com.example.proyectolagranja.ui.Clases.Venta;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;

public class CatalogoFragment extends Fragment implements View.OnClickListener{
    private Spinner spCategorias, spProductos;
    private RecyclerView recyclerView;
    private ArticuloAdapter adapter;
    private EditText et_busqueda;
    private List<Articulo> listaArticulos = new ArrayList<>();
    private List<Categoria> listaCategorias = new ArrayList<>();
    private List<Producto> listaProductos = new ArrayList<>();
    private Integer categoriaSeleccionada = null;
    private Integer productoSeleccionado = null;
    private Button btnPromociones, btnFavoritos;
    private boolean mostrandoPromociones = false, mostrandoFavoritos = true;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_catalogo, container, false);

        spCategorias = rootView.findViewById(R.id.sp_categorias);
        spProductos = rootView.findViewById(R.id.sp_productos);
        et_busqueda = rootView.findViewById(R.id.et_busqueda);
        btnPromociones = rootView.findViewById(R.id.btnPromociones);
        btnFavoritos = rootView.findViewById(R.id.btnFavoritos);

        recyclerView = rootView.findViewById(R.id.recyclerViewComentarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticuloAdapter(getContext(), listaArticulos, articulo -> mostrarDialogoAgregar(articulo));
        recyclerView.setAdapter(adapter);

        cargarCategorias(); // Cargar Categorias en el spinner
        cargarProductos(); // Cargar Productos en el spinner
        cargarArticulos(); // Cargar Articulos

        configurarSpinnerCategorias(); // Configuracion categorias
        configurarSpinnerProductos(); // Configuracion productos
        configurarBusquedaPorNombre(); // Configuracion nombre

        btnPromociones.setOnClickListener(this);
        btnFavoritos.setOnClickListener(this);

        return rootView;
    }

    private void configurarSpinnerCategorias() {
        spCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position < listaCategorias.size()) {
                    Categoria seleccionada = listaCategorias.get(position);
                    categoriaSeleccionada = seleccionada.getId_categoria();

                    filtrarArticulosPorCategoria(categoriaSeleccionada);
                    cargarProductosPorCategoria(categoriaSeleccionada);
                } else {
                    categoriaSeleccionada = null; // 🔹 Ninguna categoría
                    //cargarArticulos(); // Mostrar todo si no se selecciona ninguna categoría válida
                    //cargarProductos();
                    aplicarFiltros();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void configurarSpinnerProductos() {
        spProductos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position < listaProductos.size()) {
                    Producto seleccionado = listaProductos.get(position);
                    productoSeleccionado = seleccionado.getId_producto();

                    filtrarArticulosPorProducto(productoSeleccionado);
                } else {
                    productoSeleccionado = null; // 🔹 Ningún producto
                    //cargarArticulos();
                    aplicarFiltros();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void configurarBusquedaPorNombre() {
        et_busqueda.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private static final long DELAY = 500;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                timer.cancel();
                timer = new Timer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        requireActivity().runOnUiThread(() -> {
                            String texto = s.toString().trim();
                            if (!texto.isEmpty()) {
                                buscarArticulosPorNombre(texto);
                            } else {
                                aplicarFiltros();
                            }
                        });
                    }
                }, DELAY);
            }
        });
    }

    private void filtrarArticulosPorProducto(int idProducto) {
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_filtrar_producto.php?id_producto=" + idProducto;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                listaArticulos.clear();

                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        String id = obj.getString("id_articulo");
                        String nombre = obj.getString("nom_articulo");
                        String precio = obj.getString("prec_vent3_articulo");
                        String imagen = obj.getString("foto_articulo");

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen));
                    }

                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar los artículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(getContext(), "Error al filtrar artículos por producto", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarProductosPorCategoria(int idCategoria) {
        String url = ServidorConfig.URL_SERVIDOR + "producto/producto_listar_categoria.php?id_categoria=" + idCategoria;
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    List<String> nombresProductos = new ArrayList<>();
                    nombresProductos.add("Productos");

                    listaProductos.clear();
                    listaProductos.add(null);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id = obj.getInt("id_producto");
                        String nombre = obj.getString("nom_producto");

                        listaProductos.add(new Producto(id, nombre));
                        nombresProductos.add(nombre);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            nombresProductos
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spProductos.setAdapter(adapter);

                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar productos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al cargar productos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarArticulosPorCategoria(int idCategoria) {
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_filtrar_categoria.php?id_categoria=" + idCategoria;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                listaArticulos.clear();

                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        String id = obj.getString("id_articulo");
                        String nombre = obj.getString("nom_articulo");
                        String precio = obj.getString("prec_vent3_articulo");
                        String imagen = obj.getString("foto_articulo");

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen));
                    }

                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar los artículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(getContext(), "Error al filtrar artículos por categoría", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buscarArticulosPorNombre(String nombre) {
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_buscar_filtro.php?"
                + "nom_articulo=" + nombre;

        if (categoriaSeleccionada != null) {
            url += "&id_categoria=" + categoriaSeleccionada;
        }

        if (productoSeleccionado != null) {
            url += "&id_producto=" + productoSeleccionado;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                listaArticulos.clear();

                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        String id = obj.getString("id_articulo");
                        String nombre = obj.getString("nom_articulo");
                        String precio = obj.getString("prec_vent3_articulo");
                        String imagen = obj.getString("foto_articulo");

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen));
                    }

                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar los artículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(getContext(), "Error al buscar artículos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void aplicarFiltros() {
        // Si hay categoría o producto seleccionado → hacer búsqueda filtrada
        if (categoriaSeleccionada != null || productoSeleccionado != null) {
            buscarArticulosPorNombre(""); // ← pasamos vacío pero respeta filtros
        } else {
            // Si no hay filtros, traer todos
            cargarArticulos();
        }
    }


    private void cargarArticulos() {
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_catalogo.php";
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    listaArticulos.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String id = obj.getString("id_articulo");
                        String nombre = obj.getString("nom_articulo");
                        String precio = obj.getString("prec_vent3_articulo");
                        String imagen = obj.getString("foto_articulo");

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen));
                    }

                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar los artículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarCategorias() {
        String url = ServidorConfig.URL_SERVIDOR + "categoria/categoria_listar.php";
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    List<String> nombresCategorias = new ArrayList<>();
                    nombresCategorias.add("Categoría");

                    listaCategorias.clear();
                    listaCategorias.add(null); // Posición 0 reservada para "Categoría"

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id = obj.getInt("id_categoria");
                        String nombre = obj.getString("nom_categoria");

                        listaCategorias.add(new Categoria(id, nombre));
                        nombresCategorias.add(nombre);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            nombresCategorias
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spCategorias.setAdapter(adapter);
                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarProductos() {
        String url = ServidorConfig.URL_SERVIDOR + "producto/producto_listar.php";
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    List<String> nombresProductos = new ArrayList<>();
                    nombresProductos.add("Productos");

                    listaProductos.clear();
                    listaProductos.add(null);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id = obj.getInt("id_producto");
                        String nombre = obj.getString("nom_producto");

                        listaProductos.add(new Producto(id, nombre));
                        nombresProductos.add(nombre);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            nombresProductos
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spProductos.setAdapter(adapter);
                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoAgregar(Articulo articulo) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.alert_dialog_definir_cantidad, null);

        TextView nombreArticulo = dialogView.findViewById(R.id.tvNombreArticulo);
        TextView precioArticulo = dialogView.findViewById(R.id.tvPrecioArticulo);
        TextInputEditText etCantidad = dialogView.findViewById(R.id.etCantidad);
        TextInputEditText etDetalle = dialogView.findViewById(R.id.etDetalle);
        MaterialButton btnAgregar = dialogView.findViewById(R.id.btnAgregar);
        MaterialButton btnCerrar = dialogView.findViewById(R.id.btnCerrar);
        MaterialButton btnRestar = dialogView.findViewById(R.id.btnRestar);
        MaterialButton btnSumar = dialogView.findViewById(R.id.btnSumar);

        // Botón Restar
        btnRestar.setOnClickListener(v -> {
            String valorStr = etCantidad.getText().toString().trim();
            if (!valorStr.isEmpty()) {
                int valor = Integer.parseInt(valorStr);
                if (valor > 1) {
                    valor--;
                    etCantidad.setText(String.valueOf(valor));
                }
            }
        });

        // Botón Sumar
        btnSumar.setOnClickListener(v -> {
            String valorStr = etCantidad.getText().toString().trim();
            int valor;
            if (valorStr.isEmpty()) {
                valor = 1; // Si no hay valor, iniciar en 1
            } else {
                valor = Integer.parseInt(valorStr) + 1;
            }
            etCantidad.setText(String.valueOf(valor));
        });

        // Imprimir el encabezado del articulo
        nombreArticulo.setText(articulo.getNombre());
        precioArticulo.setText("S/ " + articulo.getPrecio());

        // --- Buscar si el artículo ya está en el carrito ---
        // Se guarda la referencia y posición
        final ItemCarrito[] itemExistente = {null};
        final int[] indexExistente = {-1};

        for (int i = 0; i < MainActivity.carrito.size(); i++) {
            if (MainActivity.carrito.get(i).getArticulo().getId().equals(articulo.getId())) {
                itemExistente[0] = MainActivity.carrito.get(i);
                indexExistente[0] = i;
                break;
            }
        }

        // --- Si ya existe, precargar datos ---
        if (itemExistente[0] != null) {
            etCantidad.setText(String.valueOf(itemExistente[0].getCantidad()));
            etDetalle.setText(itemExistente[0].getDetalle());
            btnAgregar.setText("Actualizar");
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        dialog.setCanceledOnTouchOutside(false); // 🔹 No cerrar al tocar fuera
        dialog.setCancelable(false); // 🔹 No cerrar con botón atrás

        btnAgregar.setOnClickListener(v -> {
            String cantidadStr = etCantidad.getText().toString().trim();
            String detalle = etDetalle.getText().toString().trim();

            if (cantidadStr.isEmpty()) {
                etCantidad.setError("Ingrese una cantidad");
                return;
            }
            int cantidad = Integer.parseInt(cantidadStr);

            if (itemExistente[0] == null) {
                // No existía → agregar
                MainActivity.carrito.add(new ItemCarrito(articulo, cantidad, detalle));
                Toast.makeText(getContext(), "Artículo agregado al carrito", Toast.LENGTH_SHORT).show();
            } else {
                // Ya existía → actualizar
                itemExistente[0].setCantidad(cantidad);
                itemExistente[0].setDetalle(detalle);
                MainActivity.carrito.set(indexExistente[0], itemExistente[0]);
                Toast.makeText(getContext(), "Artículo actualizado en el carrito", Toast.LENGTH_SHORT).show();
            }

            ((MainActivity) requireActivity()).actualizarBadge();
            dialog.dismiss();
        });

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void listarFavoritos(){
        int idCliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 1);

        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_favoritos.php?id_cliente=" + idCliente;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    listaArticulos.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String id = obj.getString("id_articulo");
                        String nombre = obj.getString("nom_articulo");
                        String imagen = obj.getString("foto_articulo");

                        // verificamos si es promo
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        String precio;

                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent3_articulo", "0");
                        }

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen));
                    }
                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private  void listarPromociones(){
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_promociones.php";
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    listaArticulos.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String id = obj.getString("id_articulo");
                        String nombre = obj.getString("nom_articulo");
                        String precio = obj.getString("prec_promo_articulo");
                        String imagen = obj.getString("foto_articulo");

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen));
                    }

                    adapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Error al procesar los artículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        if(view == btnFavoritos){
            if (mostrandoFavoritos) {
                cargarArticulos(); // Lista completa
                mostrandoFavoritos = false;

                btnFavoritos.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
                );
            } else {
                // Mostrar favoritos
                listarFavoritos();
                mostrandoFavoritos = true;

                btnFavoritos.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.darker_gray)
                );
            }
        }

        if(view == btnPromociones){
            if (mostrandoPromociones) {
                cargarArticulos(); // Lista completa
                mostrandoPromociones = false;

                btnPromociones.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.color_verde) // Cambiar de color al boton
                );
            } else {
                // Mostrar promociones
                listarPromociones();
                mostrandoPromociones = true;

                btnPromociones.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.darker_gray)
                );
            }
        }
    }
}
