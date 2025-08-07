package com.example.proyectolagranja.ui.Catalogo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class CatalogoFragment extends Fragment implements View.OnClickListener {
    private Spinner spCategorias, spProductos;
    private RecyclerView recyclerView;
    private ArticuloAdapter adapter;
    private List<Articulo> listaArticulos = new ArrayList<>();
    private Button btnAgregarCantidad;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_catalogo, container, false);

        spCategorias = rootView.findViewById(R.id.sp_categorias);
        spProductos = rootView.findViewById(R.id.sp_productos);

        cargarCategorias();
        cargarProductos();

        recyclerView = rootView.findViewById(R.id.recyclerViewComentarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticuloAdapter(getContext(), listaArticulos);
        recyclerView.setAdapter(adapter);

        cargarArticulos(); // Método para traer artículos desde el servidor

        return rootView;
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
        if (v == btnAgregarCantidad) {

        }
    }
}