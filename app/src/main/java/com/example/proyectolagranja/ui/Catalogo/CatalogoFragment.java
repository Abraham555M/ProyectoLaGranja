package com.example.proyectolagranja.ui.Catalogo;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
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
                    cargarProductosPorCategoria(categoriaSeleccionada); // corregir con las etiquetas
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

                    // 👇 Si estaba en promociones, salir del modo promociones
                    if (mostrandoPromociones) {
                        mostrandoPromociones = false;
                        btnPromociones.setBackgroundTintList(
                                ContextCompat.getColorStateList(getContext(), R.color.color_verde)
                        );
                    }
                    if (mostrandoFavoritos) {
                        mostrandoFavoritos = false;
                        btnFavoritos.setBackgroundTintList(
                                ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
                        );
                    }

                    filtrarArticulosPorProducto(productoSeleccionado);
                } else {
                    productoSeleccionado = null;
                    aplicarFiltros();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarProductos(true); // 🔹 Forzamos la carga SIEMPRE al volver
        configurarSpinnerProductos();
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
        resetFiltrosPromocionesYFavoritos();

        int idCliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR
                + "articulo/articulo_filtrar_producto.php?id_producto="
                + idProducto
                + "&id_cliente=" + idCliente;

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
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int esFavorito = obj.optInt("es_favorito", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);

                        // Validar qué precio usar
                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        // Usar el mismo constructor que en listarPromociones
                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito));
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
    private void resetFiltrosPromocionesYFavoritos() {
        // 🔹 Desactivar promociones si estaba activo
        if (mostrandoPromociones) {
            mostrandoPromociones = false;
            btnPromociones.setBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), R.color.color_verde)
            );
        }

        // 🔹 Desactivar favoritos si estaba activo
        if (mostrandoFavoritos) {
            mostrandoFavoritos = false;
            btnFavoritos.setBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
            );
        }
    }

    private void filtrarArticulosPorCategoria(int idCategoria) {
        resetFiltrosPromocionesYFavoritos();

        int idCliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_filtrar_categoria.php?id_categoria="
                + idCategoria + "&id_cliente=" + idCliente;

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
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);
                        int esFavorito = obj.optInt("es_favorito", 0);

                        // Validar qué precio usar
                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        // Usar constructor extendido con favorito incluido
                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito));
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
        int idCliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_buscar_filtro.php?"
                + "nom_articulo=" + nombre
                + "&id_cliente=" + idCliente; // Agregar id_cliente

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
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int esFavorito = obj.optInt("es_favorito", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);

                        // Elegir precio según promoción
                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        // Agregar artículo con todos los campos
                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito));
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
        // 🚫 Si estoy en promociones, no aplicar filtros
        if (mostrandoPromociones|| mostrandoFavoritos) return;

        if (categoriaSeleccionada != null || productoSeleccionado != null) {
            buscarArticulosPorNombre("");
        } else {
            cargarArticulos();
        }
    }

    private void cargarArticulos() {
        int id_cliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_catalogo.php?id_cliente=" + id_cliente;
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
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);
                        int esFavorito = obj.optInt("es_favorito", 0);

                        // Validación: si es promo usar precio de promo, caso contrario precio normal
                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito));
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

    private void cargarProductos(boolean forzarTodos) {
        if (!forzarTodos && (mostrandoPromociones || mostrandoFavoritos)) {
            // Si estoy mostrando promociones, no recargar productos
            return;
        }

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
        MaterialButton etPromociones = dialogView.findViewById(R.id.etPromociones);
        MaterialButton etFavoritos = dialogView.findViewById(R.id.etFavoritos);
        LinearLayout contenedorIzquierdo = dialogView.findViewById(R.id.contenedorIzquierdo);

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

        // Mostrar o ocultar el etPromociones
        if (articulo.getEsPromo() == 1) {
            etPromociones.setVisibility(View.VISIBLE);
        } else {
            etPromociones.setVisibility(View.GONE);

            contenedorIzquierdo.removeView(etFavoritos);
            contenedorIzquierdo.addView(etFavoritos, 0);
        }

        // Mostrar o ocultar el etFavoritos
        if (articulo.getEsFavorito() == 1) {
            etFavoritos.setVisibility(View.VISIBLE);
        } else {
            etFavoritos.setVisibility(View.GONE);
        }


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
                .getInt("id_cliente", 2267);

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
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int esFavorito = obj.optInt("es_favorito", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);

                        // Validación: si es promo usar precio de promo, caso contrario precio normal
                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito));
                    }

                    if (listaArticulos.isEmpty()) {
                        Toast.makeText(getContext(), "No tienes artículos favoritos aún", Toast.LENGTH_SHORT).show();
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
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);
                        int estFavorito = obj.optInt("es_favorito", 0);

                        // Validación: si es promo usar precio de promo, caso contrario precio normal
                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, estFavorito));
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

    private void resetearProductos() {
        if (spProductos != null) {
            // Crear una lista temporal con solo la opción por defecto
            List<Producto> listaVacia = new ArrayList<>();
            listaVacia.add(new Producto(0, "Productos"));

            // Crear un nuevo adaptador con esa lista
            ArrayAdapter<Producto> adapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    listaVacia
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Asignar el adaptador al spinner
            spProductos.setAdapter(adapter);

            // Resetear variable de producto seleccionado
            productoSeleccionado = null;
        }
    }

    @Override
    public void onClick(View view) {
        // toggle exclusivo
        if(view == btnFavoritos){
            if (mostrandoFavoritos) {
                cargarArticulos(); // Lista completa
                mostrandoFavoritos = false;

                btnFavoritos.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
                );
            } else {
                // 🔹 Primero resetear selección para que no dispare filtro después
                spCategorias.setOnItemSelectedListener(null); // Desvincular listener temporal
                spCategorias.setSelection(0);
                spCategorias.post(() -> configurarSpinnerCategorias()); // volver a poner listener después

                spProductos.setOnItemSelectedListener(null); // desvincular temporal
                cargarProductos(false); // Recargar todos los productos
                spProductos.setSelection(0);
                spProductos.post(() -> configurarSpinnerProductos());

                // Mostrar favoritos
                listarFavoritos();
                mostrandoFavoritos = true;

                btnFavoritos.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.darker_gray)
                );

                // Resetear promociones
                mostrandoPromociones = false;
                btnPromociones.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.color_verde)
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
                // 🔹 Resetear selección de spinners sin recargar productos
                spCategorias.setOnItemSelectedListener(null);
                spCategorias.setSelection(0);
                spCategorias.post(() -> configurarSpinnerCategorias());

                // Resetear productos
                spProductos.setOnItemSelectedListener(null);
                cargarProductos(false); // 👈 false = carga general, no filtrada
                spProductos.setSelection(0);
                spProductos.post(() -> configurarSpinnerProductos());
                resetearProductos();


                // 🔹 Mostrar solo promociones
                listarPromociones();
                mostrandoPromociones = true;

                btnPromociones.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.darker_gray)
                );

                // Resetear favoritos
                mostrandoFavoritos = false;
                btnFavoritos.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
                );
            }
        }
    }
}
