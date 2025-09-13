package com.example.proyectolagranja.ui.Catalogo;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.proyectolagranja.MainActivity;
import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Catalogo.Adapter.ArticuloAdapter;
import com.example.proyectolagranja.ui.Clases.Articulo;
import com.example.proyectolagranja.ui.Clases.Categoria;
import com.example.proyectolagranja.ui.Clases.ItemCarrito;
import com.example.proyectolagranja.ui.Clases.Producto;
import com.example.proyectolagranja.ui.Servicios.DecimalDigitsInputFilter;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.example.proyectolagranja.ui.Servicios.SessionManager;
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

public class CatalogoFragment extends Fragment implements View.OnClickListener {
    private Spinner spCategorias, spProductos;
    private RecyclerView recyclerView;
    private ArticuloAdapter adapter;
    private EditText etBusqueda;
    private List<Articulo> listaArticulos = new ArrayList<>();
    private List<Categoria> listaCategorias = new ArrayList<>();
    private List<Producto> listaProductos = new ArrayList<>();
    private Integer productoSeleccionado = null, categoriaSeleccionada = null;
    private MaterialButton btnOfertas, btnFavoritos;
    private boolean mostrandoPromociones = false, mostrandoFavoritos = false;
    private SessionManager session;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateLayout;
    private TextView tvEmptyMessage;
    private ImageView ivEmptyIcon;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_catalogo, container, false);

        //  Inicializamos SessionManager
        session = new SessionManager(requireContext());

        spCategorias = rootView.findViewById(R.id.spCategorias);
        spProductos = rootView.findViewById(R.id.spProductos);
        etBusqueda = rootView.findViewById(R.id.etBusqueda);
        btnOfertas = rootView.findViewById(R.id.btnOfertas);
        btnFavoritos = rootView.findViewById(R.id.btnFavoritos);
        emptyStateLayout = rootView.findViewById(R.id.emptyStateLayout);
        tvEmptyMessage = rootView.findViewById(R.id.tvEmptyMessage);
        ivEmptyIcon = rootView.findViewById(R.id.ivEmptyIcon);

        recyclerView = rootView.findViewById(R.id.recyclerViewComentarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticuloAdapter(getContext(), listaArticulos, articulo -> mostrarDialogoAgregar(articulo));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Swipe abajo - recargas datos
            recargarCatalogo();
        });

        cargarCategorias();
        cargarArticulos();

        configurarSpinnerCategorias(); // Configuracion categorias
        configurarSpinnerProductos(); // Configuracion productos
        configurarBusquedaPorNombre(); // Configuracion nombre

        btnOfertas.setOnClickListener(this);
        btnFavoritos.setOnClickListener(this);

        return rootView;
    }

    private void recargarCatalogo() {
        // Limpiar filtros seleccionados
        categoriaSeleccionada = null;
        productoSeleccionado = null;

        cargarArticulos();
        resetFiltrosPromocionesYFavoritos();

        // Resetear Spinners
        spCategorias.setOnItemSelectedListener(null); // Desvincular listener temporal
        spCategorias.setSelection(0);
        spCategorias.post(() -> configurarSpinnerCategorias()); // Volver a poner listener después

        spProductos.setOnItemSelectedListener(null); // Desvincular temporal
        cargarProductos(false); // Recargar todos los productos
        spProductos.setSelection(0);
        spProductos.post(() -> configurarSpinnerProductos());

        etBusqueda.setText("");

        swipeRefreshLayout.setRefreshing(false);
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
                    categoriaSeleccionada = null;
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

                    // Si estaba en promociones, salir del modo promociones
                    if (mostrandoPromociones) {
                        mostrandoPromociones = false;
                        btnOfertas.setBackgroundTintList(
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarProductos(true); // Forzamos la carga SIEMPRE al volver
        configurarSpinnerProductos();
    }

    private void configurarBusquedaPorNombre() {
        etBusqueda.addTextChangedListener(new TextWatcher() {
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

        int idCliente = session.getIdCliente();

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
                        String codPresentacion = obj.getString("cod_presentacion");
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

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito, codPresentacion));
                    }
                    if (listaArticulos.isEmpty()) {
                        mostrarEmptyState("No se encontraron artículos", R.drawable.ic_sin_articulos);
                    } else {
                        ocultarEmptyState();
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
        // Resetear bandera
        mostrandoPromociones = false;
        mostrandoFavoritos = false;

        // Restaurar Ofertas al estado inicial
        btnOfertas.setBackgroundTintList(
                ContextCompat.getColorStateList(getContext(), R.color.color_blanco)
        );
        btnOfertas.setTextColor(
                ContextCompat.getColor(getContext(), R.color.color_verde)
        );
        btnOfertas.setIconTint(
                ContextCompat.getColorStateList(getContext(), R.color.color_verde)
        );
        btnOfertas.setStrokeColor(
                ContextCompat.getColorStateList(getContext(), R.color.color_verde)
        );
        btnOfertas.setStrokeWidth(2);

        // Restaurar Favoritos al estado inicial
        btnFavoritos.setBackgroundTintList(
                ContextCompat.getColorStateList(getContext(), R.color.color_blanco)
        );
        btnFavoritos.setTextColor(
                ContextCompat.getColor(getContext(), R.color.color_rojo)
        );
        btnFavoritos.setIconTint(
                ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
        );
        btnFavoritos.setStrokeColor(
                ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
        );
        btnFavoritos.setStrokeWidth(2);
    }

    private void filtrarArticulosPorCategoria(int idCategoria) {
        resetFiltrosPromocionesYFavoritos();
        int idCliente = session.getIdCliente();

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
                        String codPresentacion = obj.getString("cod_presentacion");
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

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito, codPresentacion));
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
        resetFiltrosPromocionesYFavoritos();

        int idCliente = session.getIdCliente();

        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_buscar_filtro.php?"
                + "nom_articulo=" + nombre
                + "&id_cliente=" + idCliente;

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
                        String codPresentacion = obj.getString("cod_presentacion");
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

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito, codPresentacion));
                    }

                    adapter.notifyDataSetChanged();
                    if (listaArticulos.isEmpty()) {
                        mostrarEmptyState("No se encontraron artículos", R.drawable.ic_sin_articulos);
                    } else {
                        ocultarEmptyState();
                    }
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
        // Si estoy en promociones, no aplicar filtros
        if (mostrandoPromociones|| mostrandoFavoritos) return;

        if (categoriaSeleccionada != null || productoSeleccionado != null) {
            buscarArticulosPorNombre("");
        } else {
            cargarArticulos();
        }
    }

    private void cargarArticulos() {
        int idCliente = session.getIdCliente();

        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_catalogo.php?id_cliente=" + idCliente;
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
                        String codPresentacion = obj.optString("cod_presentacion", "");
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);
                        int esFavorito = obj.optInt("es_favorito", 0);

                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }
                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito, codPresentacion));
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
                    listaCategorias.add(null);

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
        TextInputEditText etDetalle = dialogView.findViewById(R.id.etDetalle);
        MaterialButton btnAgregar = dialogView.findViewById(R.id.btnAgregar);
        MaterialButton btnCerrar = dialogView.findViewById(R.id.btnCerrar);
        MaterialButton btnRestar = dialogView.findViewById(R.id.btnRestar);
        MaterialButton btnSumar = dialogView.findViewById(R.id.btnSumar);
        MaterialButton etPromociones = dialogView.findViewById(R.id.etPromociones);
        MaterialButton etFavoritos = dialogView.findViewById(R.id.etFavoritos);
        TextView tvOfertaValida = dialogView.findViewById(R.id.tvOfertaValida);
        TextInputEditText etCantidad = dialogView.findViewById(R.id.etCantidad);
        TextView labelOfertaCantidad = dialogView.findViewById(R.id.labelOfertaCantidad);

        // Limitar a 3 decimales
        etCantidad.setFilters(new InputFilter[]{ new DecimalDigitsInputFilter(3) });

        // Botón Restar
        btnRestar.setOnClickListener(v -> {
            String valorStr = etCantidad.getText().toString().trim();
            if (!valorStr.isEmpty()) {
                double valor = Double.parseDouble(valorStr);
                if (valor > 1) {
                    valor--;
                    etCantidad.setText(String.valueOf(valor));
                }
            }
        });

        // Botón Sumar
        btnSumar.setOnClickListener(v -> {
            String valorStr = etCantidad.getText().toString().trim();
            double valor;
            if (valorStr.isEmpty()) {
                valor = 1; // Si no hay valor, iniciar en 1
            } else {
                valor = Double.parseDouble(valorStr) + 1;
            }
            etCantidad.setText(String.valueOf(valor));
        });


        // Mostrar o ocultar el etPromociones
        if (articulo.getEsPromo() == 1) {
            etPromociones.setVisibility(View.VISIBLE);
            tvOfertaValida.setVisibility(View.VISIBLE);
            labelOfertaCantidad.setVisibility(View.VISIBLE);
        } else {
            etPromociones.setVisibility(View.GONE);
            tvOfertaValida.setVisibility(View.GONE);
            labelOfertaCantidad.setVisibility(View.GONE);
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

        // Buscar si el artículo ya está en el carrito
        // --- Se guarda la referencia y posición ---
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

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        btnAgregar.setOnClickListener(v -> {
            String cantidadStr = etCantidad.getText().toString().trim();
            String detalle = etDetalle.getText().toString().trim();

            if (cantidadStr.isEmpty()) {
                etCantidad.setError("Ingrese una cantidad");
                return;
            }
            double cantidad = Double.parseDouble(cantidadStr);

            if (cantidad <= 0) {
                etCantidad.setError("La cantidad debe ser mayor a 0");
                return;
            }

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
        int idCliente = session.getIdCliente();
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
                        String codPresentacion = obj.getString("cod_presentacion");
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int esFavorito = obj.optInt("es_favorito", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);

                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, esFavorito, codPresentacion));
                    }

                    if (listaArticulos.isEmpty()) {
                        mostrarEmptyState("No tienes artículos favoritos aún", R.drawable.ic_favoritos);
                    } else {
                        ocultarEmptyState();
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
        int idCliente = session.getIdCliente();
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_ofertas.php?id_cliente=" + idCliente;

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
                        String codPresentacion = obj.getString("cod_presentacion");
                        String imagen = obj.getString("foto_articulo");
                        int esPromo = obj.optInt("est_promo_articulo", 0);
                        int totalComprado = obj.optInt("total_comprado", 0);
                        int estFavorito = obj.optInt("es_favorito", 0);

                        String precio;
                        if (esPromo == 1) {
                            precio = obj.optString("prec_promo_articulo", "0");
                        } else {
                            precio = obj.optString("prec_vent1_articulo", "0");
                        }

                        listaArticulos.add(new Articulo(id, nombre, precio, imagen, esPromo, totalComprado, estFavorito, codPresentacion));
                    }

                    if (listaArticulos.isEmpty()) {
                        mostrarEmptyState("No hay artículos en promoción", R.drawable.ic_promociones);
                    } else {
                        ocultarEmptyState();
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

    private void mostrarEmptyState(String mensaje, int iconRes) {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);

        tvEmptyMessage.setText(mensaje);
        ivEmptyIcon.setImageResource(iconRes);
    }

    private void ocultarEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) { // toggle exclusivo
        if(view == btnFavoritos){
            if (mostrandoFavoritos) {
                cargarArticulos(); // Lista completa
                ocultarEmptyState();
                mostrandoFavoritos = false;

                btnFavoritos.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.white)
                );
                btnFavoritos.setTextColor(ContextCompat.getColor(getContext(), R.color.color_rojo));
                btnFavoritos.setIconTint(ContextCompat.getColorStateList(getContext(), R.color.color_rojo));
            } else {
                // Primero resetear selección para que no dispare filtro después
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
                        ContextCompat.getColorStateList(getContext(), R.color.color_rojo)
                );
                btnFavoritos.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                btnFavoritos.setIconTint(ContextCompat.getColorStateList(getContext(), android.R.color.white));

                // Resetear promociones
                mostrandoPromociones = false;
                btnOfertas.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.white)
                );
                btnOfertas.setTextColor(ContextCompat.getColor(getContext(), R.color.color_verde));
                btnOfertas.setIconTint(ContextCompat.getColorStateList(getContext(), R.color.color_verde));

                etBusqueda.setText("");
            }
        }

        if(view == btnOfertas){
            if (mostrandoPromociones) {
                cargarArticulos(); // Lista completa
                mostrandoPromociones = false;

                btnOfertas.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.white)
                );
                btnOfertas.setTextColor(ContextCompat.getColor(getContext(), R.color.color_verde));
                btnOfertas.setIconTint(ContextCompat.getColorStateList(getContext(), R.color.color_verde));

            } else {
                spCategorias.setOnItemSelectedListener(null);
                spCategorias.setSelection(0);
                spCategorias.post(() -> configurarSpinnerCategorias());

                spProductos.setOnItemSelectedListener(null);
                cargarProductos(false);
                spProductos.setSelection(0);
                spProductos.post(() -> configurarSpinnerProductos());

                listarPromociones();
                mostrandoPromociones = true;

                btnOfertas.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.color_verde)
                );
                btnOfertas.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                btnOfertas.setIconTint(ContextCompat.getColorStateList(getContext(), android.R.color.white));

                mostrandoFavoritos = false;
                btnFavoritos.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), android.R.color.white)
                );
                btnFavoritos.setTextColor(ContextCompat.getColor(getContext(), R.color.color_rojo));
                btnFavoritos.setIconTint(ContextCompat.getColorStateList(getContext(), R.color.color_rojo));

                etBusqueda.setText("");
            }
        }
    }
}
