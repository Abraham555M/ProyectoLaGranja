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
import com.example.proyectolagranja.ui.Catalogo.Helper.DialogoAgregarHelper;
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

public class CatalogoFragment extends Fragment {
    // Constants
    private static final long SEARCH_DELAY = 500L;
    private static final int MIN_QUANTITY = 1;
    private static final String DEFAULT_PRICE = "0";

    // UI Components
    private Spinner spCategorias, spProductos;
    private RecyclerView recyclerView;
    private ArticuloAdapter adapter;
    private EditText etBusqueda;
    private MaterialButton btnOfertas, btnFavoritos;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateLayout;
    private TextView tvEmptyMessage, tvEmptySubMessage;
    private ImageView ivEmptyIcon;

    // Data
    private final List<Articulo> listaArticulos = new ArrayList<>();
    private final List<Categoria> listaCategorias = new ArrayList<>();
    private final List<Producto> listaProductos = new ArrayList<>();

    // State
    private Integer productoSeleccionado = null;
    private Integer categoriaSeleccionada = null;
    private boolean mostrandoPromociones = false;
    private boolean mostrandoFavoritos = false;

    // Services
    private SessionManager session;
    private Timer searchTimer = new Timer();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_catalogo, container, false);

        initializeComponents(rootView);
        setupRecyclerView();
        setupSwipeRefresh();
        setupEventListeners();
        loadInitialData();

        return rootView;
    }

    private void initializeComponents(View rootView) {
        session = new SessionManager(requireContext());

        spCategorias = rootView.findViewById(R.id.spCategorias);
        spProductos = rootView.findViewById(R.id.spProductos);
        etBusqueda = rootView.findViewById(R.id.etBusqueda);
        btnOfertas = rootView.findViewById(R.id.btnOfertas);
        btnFavoritos = rootView.findViewById(R.id.btnFavoritos);
        emptyStateLayout = rootView.findViewById(R.id.emptyStateLayout);
        tvEmptyMessage = rootView.findViewById(R.id.tvEmptyMessage);
        tvEmptySubMessage = rootView.findViewById(R.id.tvEmptySubMessage);
        ivEmptyIcon = rootView.findViewById(R.id.ivEmptyIcon);
        recyclerView = rootView.findViewById(R.id.recyclerViewComentarios);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticuloAdapter(getContext(), listaArticulos, this::mostrarDialogoAgregar);
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::recargarCatalogo);
    }

    private void setupEventListeners() {
        configurarSpinnerCategorias();
        configurarSpinnerProductos();
        configurarBusquedaPorNombre();

        btnOfertas.setOnClickListener(v -> toggleOfertas());
        btnFavoritos.setOnClickListener(v -> toggleFavoritos());
    }

    private void loadInitialData() {
        cargarCategorias();
        cargarProductos(true);
        cargarArticulos();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarProductos(true);
    }

    // =================== FILTER METHODS ===================

    private void recargarCatalogo() {
        resetearFiltros();
        cargarArticulos();
        resetearBotones();
        resetearSpinners();
        etBusqueda.setText("");
        swipeRefreshLayout.setRefreshing(false);
    }

    private void resetearFiltros() {
        categoriaSeleccionada = null;
        productoSeleccionado = null;
    }

    private void resetearBotones() {
        mostrandoPromociones = false;
        mostrandoFavoritos = false;
        aplicarEstiloBotonInactivo(btnOfertas, R.color.color_verde);
        aplicarEstiloBotonInactivo(btnFavoritos, R.color.color_rojo);
    }

    private void resetearSpinners() {
        deshabilitarListeners();
        spCategorias.setSelection(0);
        spProductos.setSelection(0);
        cargarProductos(false);
        habilitarListeners();
    }

    private void deshabilitarListeners() {
        spCategorias.setOnItemSelectedListener(null);
        spProductos.setOnItemSelectedListener(null);
    }

    private void habilitarListeners() {
        spCategorias.post(this::configurarSpinnerCategorias);
        spProductos.post(this::configurarSpinnerProductos);
    }

    // =================== SPINNER CONFIGURATION ===================

    private void configurarSpinnerCategorias() {
        spCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (esSeleccionValida(position, listaCategorias.size())) {
                    Categoria categoria = listaCategorias.get(position);
                    categoriaSeleccionada = categoria.getId_categoria();
                    filtrarPorCategoria(categoriaSeleccionada);
                    cargarProductosPorCategoria(categoriaSeleccionada);
                } else {
                    categoriaSeleccionada = null;
                    aplicarFiltros();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarSpinnerProductos() {
        spProductos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (esSeleccionValida(position, listaProductos.size())) {
                    Producto producto = listaProductos.get(position);
                    productoSeleccionado = producto.getId_producto();
                    resetearBotones();
                    filtrarPorProducto(productoSeleccionado);
                } else {
                    productoSeleccionado = null;
                    aplicarFiltros();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private boolean esSeleccionValida(int position, int listSize) {
        return position > 0 && position < listSize;
    }

    private void limpiarSpinnerProductos() {
        spProductos.setOnItemSelectedListener(null);
        // Siempre cargar todos los productos cuando se limpia el spinner
        cargarProductos(true);
        spProductos.setSelection(0);
        spProductos.post(this::configurarSpinnerProductos);
    }

    private void limpiarSpinnerCategorias() {
        spCategorias.setOnItemSelectedListener(null);
        spCategorias.setSelection(0);
        spCategorias.post(this::configurarSpinnerCategorias);
    }

    private void recargarTodosLosProductos() {
        // Desconectar listener temporalmente
        spProductos.setOnItemSelectedListener(null);

        // Forzar recarga completa de productos
        String url = ServidorConfig.URL_SERVIDOR + "producto/producto_listar.php";
        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                procesarRespuestaProductos(new String(responseBody));
                // Resetear a posición 0 y reconectar listener
                spProductos.setSelection(0);
                spProductos.post(() -> configurarSpinnerProductos());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarError("Error al recargar productos");
                // Reconectar listener aunque falle
                spProductos.post(() -> configurarSpinnerProductos());
            }
        });
    }

    // =================== SEARCH CONFIGURATION ===================

    private void configurarBusquedaPorNombre() {
        etBusqueda.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTimer.cancel();
                searchTimer = new Timer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        requireActivity().runOnUiThread(() -> {
                            String texto = s.toString().trim();
                            if (!texto.isEmpty()) {
                                buscarPorNombre(texto);
                            } else {
                                aplicarFiltros();
                            }
                        });
                    }
                }, SEARCH_DELAY);
            }
        });
    }

    // =================== FILTER OPERATIONS ===================

    private void aplicarFiltros() {
        if (mostrandoPromociones || mostrandoFavoritos) return;

        if (tieneFiltrosaActivos()) {
            buscarPorNombre("");
        } else {
            cargarArticulos();
        }
    }

    private boolean tieneFiltrosaActivos() {
        return categoriaSeleccionada != null || productoSeleccionado != null;
    }

    private void filtrarPorCategoria(int idCategoria) {
        resetearBotones();
        ejecutarConsultaFiltro(
                ServidorConfig.URL_SERVIDOR + "articulo/articulo_filtrar_categoria.php?id_categoria="
                        + idCategoria + "&id_cliente=" + session.getIdCliente(),
                "Error al filtrar artículos por categoría"
        );
    }

    private void filtrarPorProducto(int idProducto) {
        resetearBotones();
        ejecutarConsultaFiltro(
                ServidorConfig.URL_SERVIDOR + "articulo/articulo_filtrar_producto.php?id_producto="
                        + idProducto + "&id_cliente=" + session.getIdCliente(),
                "Error al filtrar artículos por producto"
        );
    }

    private void buscarPorNombre(String nombre) {
        resetearBotones();

        StringBuilder urlBuilder = new StringBuilder()
                .append(ServidorConfig.URL_SERVIDOR)
                .append("articulo/articulo_buscar_filtro.php?nom_articulo=")
                .append(nombre)
                .append("&id_cliente=")
                .append(session.getIdCliente());

        if (categoriaSeleccionada != null) {
            urlBuilder.append("&id_categoria=").append(categoriaSeleccionada);
        }
        if (productoSeleccionado != null) {
            urlBuilder.append("&id_producto=").append(productoSeleccionado);
        }

        ejecutarConsultaFiltro(urlBuilder.toString(), "Error al buscar artículos");
    }

    // =================== BUTTON HANDLERS ===================

    private void toggleOfertas() {
        if (mostrandoPromociones) {
            mostrarTodosLosArticulos();
            aplicarEstiloBotonInactivo(btnOfertas, R.color.color_verde);
        } else {
            resetearSpinners();
            listarPromociones();
            aplicarEstiloBotonActivo(btnOfertas, R.color.color_verde);
            aplicarEstiloBotonInactivo(btnFavoritos, R.color.color_rojo);
            etBusqueda.setText("");
        }
        mostrandoPromociones = !mostrandoPromociones;
        mostrandoFavoritos = false;
    }

    private void toggleFavoritos() {
        if (mostrandoFavoritos) {
            mostrarTodosLosArticulos();
            aplicarEstiloBotonInactivo(btnFavoritos, R.color.color_rojo);
        } else {
            resetearSpinners();
            listarFavoritos();
            aplicarEstiloBotonActivo(btnFavoritos, R.color.color_rojo);
            aplicarEstiloBotonInactivo(btnOfertas, R.color.color_verde);
            etBusqueda.setText("");
        }
        mostrandoFavoritos = !mostrandoFavoritos;
        mostrandoPromociones = false;
    }

    private void mostrarTodosLosArticulos() {
        cargarArticulos();
        ocultarEmptyState();
    }

    // =================== STYLING METHODS ===================

    private void aplicarEstiloBotonActivo(MaterialButton boton, int colorRes) {
        boton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), colorRes));
        boton.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        boton.setIconTint(ContextCompat.getColorStateList(getContext(), android.R.color.white));
    }

    private void aplicarEstiloBotonInactivo(MaterialButton boton, int colorRes) {
        boton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.white));
        boton.setTextColor(ContextCompat.getColor(getContext(), colorRes));
        boton.setIconTint(ContextCompat.getColorStateList(getContext(), colorRes));
        boton.setStrokeColor(ContextCompat.getColorStateList(getContext(), colorRes));
        boton.setStrokeWidth(2);
    }

    // =================== DATA LOADING METHODS ===================

    private void cargarArticulos() {
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_catalogo.php?id_cliente="
                + session.getIdCliente();

        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                procesarRespuestaArticulos(new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarError("Error de conexión con el servidor");
            }
        });
    }

    private void cargarCategorias() {
        String url = ServidorConfig.URL_SERVIDOR + "categoria/categoria_listar.php";

        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                procesarRespuestaCategorias(new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarError("Error al conectar con el servidor");
            }
        });
    }

    private void cargarProductos(boolean forzar) {
        if (!forzar && (mostrandoPromociones || mostrandoFavoritos)) return;

        String url = ServidorConfig.URL_SERVIDOR + "producto/producto_listar.php";

        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                procesarRespuestaProductos(new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarError("Error al conectar con el servidor");
            }
        });
    }

    private void cargarProductosPorCategoria(int idCategoria) {
        String url = ServidorConfig.URL_SERVIDOR + "producto/producto_listar_categoria.php?id_categoria=" + idCategoria;

        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                procesarRespuestaProductos(new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarError("Error al cargar productos");
            }
        });
    }

    private void listarPromociones() {
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_ofertas.php?id_cliente="
                + session.getIdCliente();

        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                procesarRespuestaArticulos(new String(responseBody));
                if (listaArticulos.isEmpty()) {
                    mostrarEmptyState(
                            "No hay artículos en promoción",
                            "Por favor, intente con otros filtros o vuelva más tarde.",
                            R.drawable.ic_promociones
                    );
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarError("Error de conexión con el servidor");
            }
        });
    }

    private void listarFavoritos() {
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_listar_favoritos.php?id_cliente="
                + session.getIdCliente();

        new AsyncHttpClient().get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                procesarRespuestaArticulos(new String(responseBody));
                if (listaArticulos.isEmpty()) {
                    mostrarEmptyState(
                            "Aún no tienes artículos favoritos",
                            "Realiza algunos pedidos y aquí aparecerán sus productos favoritos.",
                            R.drawable.ic_favoritos
                    );
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                mostrarError("Error al conectar con el servidor");
            }
        });
    }

    private void ejecutarConsultaFiltro(String url, String mensajeError) {
        new AsyncHttpClient().get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                procesarJsonArticulos(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                mostrarError(mensajeError);
            }
        });
    }

    // =================== RESPONSE PROCESSING ===================

    private void procesarRespuestaArticulos(String responseBody) {
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            procesarJsonArticulos(jsonArray);
        } catch (JSONException e) {
            mostrarError("Error al procesar los artículos");
        }
    }

    private void procesarJsonArticulos(JSONArray jsonArray) {
        listaArticulos.clear();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Articulo articulo = crearArticuloDesdeJson(obj);
                listaArticulos.add(articulo);
            }

            if (listaArticulos.isEmpty() && !mostrandoPromociones && !mostrandoFavoritos) {
                mostrarEmptyState(
                        "No se encontraron artículos",
                        "Por favor, intente con otros filtros o vuelva más tarde.",
                        R.drawable.ic_sin_articulos
                );
            } else {
                ocultarEmptyState();
            }

            adapter.notifyDataSetChanged();

        } catch (JSONException e) {
            mostrarError("Error al procesar los artículos");
        }
    }

    private Articulo crearArticuloDesdeJson(JSONObject obj) throws JSONException {
        String id = obj.getString("id_articulo");
        String nombre = obj.getString("nom_articulo");
        String codPresentacion = obj.optString("cod_presentacion", "");
        String imagen = obj.getString("foto_articulo");
        int esPromo = obj.optInt("est_promo_articulo", 0);
        int totalComprado = obj.optInt("total_comprado", 0);
        int esFavorito = obj.optInt("es_favorito", 0);

        // Asignamos siempre los dos precios
        String precio = obj.optString("prec_vent1_articulo", DEFAULT_PRICE);
        String precioOferta = obj.optString("prec_promo_articulo", DEFAULT_PRICE);

        // Pasamos ambos al constructor
        return new Articulo(id, nombre, precio, precioOferta, imagen, esPromo, totalComprado, esFavorito, codPresentacion);
    }

    private void procesarRespuestaCategorias(String responseBody) {
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            List<String> nombres = new ArrayList<>();
            nombres.add("Categoría");

            listaCategorias.clear();
            listaCategorias.add(null);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                int id = obj.getInt("id_categoria");
                String nombre = obj.getString("nom_categoria");

                listaCategorias.add(new Categoria(id, nombre));
                nombres.add(nombre);
            }

            configurarSpinnerAdapter(spCategorias, nombres);

        } catch (JSONException e) {
            mostrarError("Error al procesar los datos");
        }
    }

    private void procesarRespuestaProductos(String responseBody) {
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            List<String> nombres = new ArrayList<>();
            nombres.add("Productos");

            listaProductos.clear();
            listaProductos.add(null);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                int id = obj.getInt("id_producto");
                String nombre = obj.getString("nom_producto");

                listaProductos.add(new Producto(id, nombre));
                nombres.add(nombre);
            }

            configurarSpinnerAdapter(spProductos, nombres);

        } catch (JSONException e) {
            mostrarError("Error al procesar los datos");
        }
    }

    private void configurarSpinnerAdapter(Spinner spinner, List<String> nombres) {
        List<String> nombresFormateados = new ArrayList<>(); // Primera letra mayuscula el resto minuscula
        for (String nombre : nombres) {
            if (nombre != null && !nombre.isEmpty()) {
                String lower = nombre.toLowerCase();
                String capitalizado = lower.substring(0, 1).toUpperCase() + lower.substring(1);
                nombresFormateados.add(capitalizado);
            } else {
                nombresFormateados.add(nombre);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_item,
                nombresFormateados
        ) {
            @Override
            public boolean isEnabled(int position) {
                // Bloquear la primera opción
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    tv.setTextColor(Color.GRAY); // Opción inválida en gris
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }


    // =================== DIALOG METHODS ===================

    private void mostrarDialogoAgregar(Articulo articulo) {
        // Usar la nueva clase externa
        new DialogoAgregarHelper(getContext(), articulo);
    }

    // =================== UI STATE METHODS ===================

    private void mostrarEmptyState(String mensaje, String subMensaje, int iconRes) {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(mensaje);
        tvEmptySubMessage.setText(subMensaje);
        ivEmptyIcon.setImageResource(iconRes);
    }

    private void ocultarEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (searchTimer != null) {
            searchTimer.cancel();
        }
    }
}