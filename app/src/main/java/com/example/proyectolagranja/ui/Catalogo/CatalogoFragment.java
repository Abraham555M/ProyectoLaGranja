package com.example.proyectolagranja.ui.Catalogo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Catalogo.Adapter.ArticuloAdapter;
import com.example.proyectolagranja.ui.Clases.Articulo;
import com.example.proyectolagranja.ui.Clases.Categoria;
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

public class CatalogoFragment extends Fragment implements View.OnClickListener {
    private Spinner spCategorias, spProductos;
    private RecyclerView recyclerView;
    private ArticuloAdapter adapter;
    private List<Articulo> listaArticulos = new ArrayList<>();
    private EditText et_busqueda;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_catalogo, container, false);

        spCategorias = rootView.findViewById(R.id.sp_categorias);
        spProductos = rootView.findViewById(R.id.sp_productos);
        et_busqueda = rootView.findViewById(R.id.et_busqueda);

        cargarCategorias();
        cargarProductos();

        recyclerView = rootView.findViewById(R.id.recyclerViewComentarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticuloAdapter(getContext(), listaArticulos, articulo -> mostrarDialogoAgregar(articulo));
        recyclerView.setAdapter(adapter);

        cargarArticulos(); // Método para traer artículos desde el servidor

        // Busqueda conforme se escribe
        et_busqueda.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 500; // medio segundo para evitar llamadas excesivas

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

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
                            if (!s.toString().trim().isEmpty()) {
                                buscarArticulosPorNombre(s.toString().trim());
                            } else {
                                cargarArticulos(); // Si se borra el texto, volver a mostrar todo
                            }
                        });
                    }
                }, DELAY);
            }
        });
        //

        return rootView;
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

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject categoria = jsonArray.getJSONObject(i);
                        String nombre = categoria.getString("nom_categoria");
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
                    e.printStackTrace();
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

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject categoria = jsonArray.getJSONObject(i);
                        String nombre = categoria.getString("nom_producto");
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
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
    }
}