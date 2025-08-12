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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Catalogo.Adapter.ArticuloAdapter;
import com.example.proyectolagranja.ui.Clases.Articulo;
import com.example.proyectolagranja.ui.Clases.Categoria;
import com.example.proyectolagranja.ui.Clases.Producto;
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

public class CatalogoFragment extends Fragment {
    private Spinner spCategorias, spProductos;
    private RecyclerView recyclerView;
    private ArticuloAdapter adapter;
    private EditText et_busqueda;
    private List<Articulo> listaArticulos = new ArrayList<>();
    private List<Categoria> listaCategorias = new ArrayList<>();
    private List<Producto> listaProductos = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_catalogo, container, false);

        spCategorias = rootView.findViewById(R.id.sp_categorias);
        spProductos = rootView.findViewById(R.id.sp_productos);
        et_busqueda = rootView.findViewById(R.id.et_busqueda);

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

        return rootView;
    }

    private void configurarSpinnerCategorias() {
        spCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position < listaCategorias.size()) {
                    Categoria seleccionada = listaCategorias.get(position);
                    filtrarArticulosPorCategoria(seleccionada.getId_categoria());
                } else {
                    cargarArticulos(); // Mostrar todo si no se selecciona ninguna categoría válida
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
                    filtrarArticulosPorProducto(seleccionado.getId_producto());
                } else {
                    cargarArticulos();
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

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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
                                cargarArticulos();
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
        String url = ServidorConfig.URL_SERVIDOR + "articulo/articulo_buscar_nombre.php?nom_articulo=" + nombre;

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
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.alert_dialog_definir_cantidad, null);

        TextView nombreArticulo = dialogView.findViewById(R.id.tvNombreArticulo);
        TextView precioArticulo = dialogView.findViewById(R.id.tvPrecioArticulo);
        TextInputEditText etCantidad = dialogView.findViewById(R.id.etCantidad);
        TextInputEditText etDetalle = dialogView.findViewById(R.id.etDetalle);
        MaterialButton btnAgregar = dialogView.findViewById(R.id.btnAgregar);

        nombreArticulo.setText(articulo.getNombre());
        precioArticulo.setText("S/ " + articulo.getPrecio());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        btnAgregar.setOnClickListener(v -> {
            String cantidad = etCantidad.getText().toString().trim();
            String detalle = etDetalle.getText().toString().trim();

            if (cantidad.isEmpty()) {
                etCantidad.setError("Ingrese una cantidad");
                return;
            }

            Toast.makeText(getContext(),
                    "Artículo agregado:\n" +
                            "Nombre: " + articulo.getNombre() + "\n" +
                            "Cantidad: " + cantidad + "\n" +
                            "Detalle: " + detalle,
                    Toast.LENGTH_LONG).show();

            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
