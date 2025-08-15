package com.example.proyectolagranja;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyectolagranja.ui.Catalogo.Adapter.CarritoAdapter;
import com.example.proyectolagranja.ui.Clases.ItemCarrito;
import com.example.proyectolagranja.ui.Clases.MedioPago;
import com.example.proyectolagranja.ui.Clases.Producto;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.databinding.ActivityMainBinding;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.badge.BadgeUtils;
import com.loopj.android.http.RequestParams;

import androidx.annotation.OptIn;

public class MainActivity extends AppCompatActivity {
    private List<MedioPago> listaMedioPago = new ArrayList<>();
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private Spinner spMedioPago;
    private Button btnCerrar, btnEnviarPedido;
    private TextView tvBadge;
    private EditText etDireccion, etDetalleVenta;
    public static List<ItemCarrito> carrito = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        tvBadge = findViewById(R.id.tvBadge);
        binding.appBarMain.fab.setOnClickListener(v -> mostrarCarrito());

        actualizarBadge();

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_catalogo, R.id.nav_pedidos, R.id.nav_perfil)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Para que no sea visible el encabezado
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_crear_cuenta || destination.getId() == R.id.nav_inicio_sesion) {
                binding.appBarMain.toolbar.setVisibility(View.GONE); // Quitar el encabezado
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // 🔒 Desactiva swipe
                binding.appBarMain.fab.setVisibility(View.GONE); // Quitar el carrito de compra
            } else {
                binding.appBarMain.toolbar.setVisibility(View.VISIBLE);
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED); // ✅ Reactiva swipe
                binding.appBarMain.fab.setVisibility(View.VISIBLE);
            }
        });
    }

    private void mostrarCarrito() {
        // Inflar el layout de tu alert_dialog_carrito
        View dialogView = getLayoutInflater().inflate(R.layout.alert_dialog_carrito, null);
        spMedioPago = dialogView.findViewById(R.id.spMedioPago);
        btnCerrar = dialogView.findViewById(R.id.btnCerrar);
        btnEnviarPedido = dialogView.findViewById(R.id.btnEnviarPedido);
        etDireccion = dialogView.findViewById(R.id.etDireccion);
        etDetalleVenta = dialogView.findViewById(R.id.etDetalleVenta);

        // RecyclerView para mostrar el carrito ---
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        TextView tvEmptyMessage = dialogView.findViewById(R.id.tvEmptyMessage);
        EditText etDireccion = dialogView.findViewById(R.id.etDireccion);
        TextView tvTotal = dialogView.findViewById(R.id.tvTotal);


        cargarDireccionCliente(etDireccion);

        if (carrito.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            double totalInicial = 0;
            for (ItemCarrito item : carrito) {
                double precio = Double.parseDouble(item.getArticulo().getPrecio());
                totalInicial += precio * item.getCantidad();
            }
            tvTotal.setText("Total: S/" + totalInicial);

            recyclerView.setAdapter(new CarritoAdapter(
                    MainActivity.this,
                    carrito,
                    total -> {
                        actualizarBadge();
                        tvTotal.setText("Total: S/" + total);
                        if (carrito.isEmpty()) {
                            tvEmptyMessage.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
            ));
        }

        // Crear el AlertDialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                .setView(dialogView)
                .create();
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        // Crear el pedido:
        btnEnviarPedido.setOnClickListener(v -> {
            double total = 0;
            for (ItemCarrito item : carrito) {
                double precio = Double.parseDouble(item.getArticulo().getPrecio());
                total += precio * item.getCantidad();
            }

            if (spMedioPago.getSelectedItemPosition() == 0) {
                Toast.makeText(MainActivity.this, "Seleccione un medio de pago", Toast.LENGTH_SHORT).show();
                return;
            }
            if (etDireccion.getText().toString().trim().isEmpty()) {
                Toast.makeText(MainActivity.this, "Ingrese la dirección", Toast.LENGTH_SHORT).show();
                return;
            }

            EnviarPedido(
                    total,
                    etDireccion.getText().toString().trim(),
                    etDetalleVenta.getText().toString().trim(),
                    dialog
            );
        });

        cargarMedioPago();

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void EnviarPedido(double total, String direccion, String detalle_venta, androidx.appcompat.app.AlertDialog dialog){
        // Preparar envío
        SharedPreferences preferences = getSharedPreferences("DatosUsuario", MODE_PRIVATE);
        int idCliente = preferences.getInt("id_cliente", 1); // provisional

        int idMedioPago = listaMedioPago.get(spMedioPago.getSelectedItemPosition()).getId_pago_medio();

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("id_cliente", idCliente);
        params.put("id_pago_medio", idMedioPago);
        params.put("tot_venta", total);
        params.put("dir_cliente", direccion);
        params.put("det_venta", detalle_venta);

        String url = ServidorConfig.URL_SERVIDOR + "pedido/pedido_registrar.php";

        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String respuesta = new String(responseBody);
                    JSONObject json = new JSONObject(respuesta);

                    if (json.getBoolean("success")) {
                        int idVenta = json.getInt("id_venta");
                        Toast.makeText(MainActivity.this, "Pedido registrado. ID: " + idVenta, Toast.LENGTH_SHORT).show();
                        carrito.clear();
                        actualizarBadge();
                        dialog.dismiss();
                    } else {
                        String error = json.getString("error");
                        Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(MainActivity.this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void actualizarBadge() {
        int cantidad = carrito.size();
        if (cantidad > 0) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(String.valueOf(cantidad));
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }

    private void cargarDireccionCliente(EditText etDireccion) {
         /*
        SharedPreferences preferences = getSharedPreferences("DatosUsuario", MODE_PRIVATE);
        int idCliente = preferences.getInt("id_cliente", -1);

        if (idCliente == -1) {
            Toast.makeText(this, "No se encontró el cliente logueado", Toast.LENGTH_SHORT).show();
            return;
        }
         */
        int idCliente = 1; //Cliente para pruebas
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_obtener_direccion.php?id_cliente=" + idCliente;
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject json = new JSONObject(new String(responseBody));
                    if (json.has("dir_cliente")) {
                        etDireccion.setText(json.getString("dir_cliente"));
                    } else {
                        Toast.makeText(MainActivity.this, "Dirección no encontrada", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(MainActivity.this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarMedioPago() {
        String url = ServidorConfig.URL_SERVIDOR + "medio_pago/medio_pago_listar.php";
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray jsonArray = new JSONArray(new String(responseBody));
                    List<String> nombresProductos = new ArrayList<>();

                    // Primera opción por defecto
                    listaMedioPago.clear();
                    listaMedioPago.add(new MedioPago(0, "Seleccione un medio de pago"));
                    nombresProductos.add("Seleccione un medio de pago");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id = obj.getInt("id_pago_medio");
                        String nombre = obj.getString("nom_pago_medio");

                        listaMedioPago.add(new MedioPago(id, nombre));
                        nombresProductos.add(nombre);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_spinner_item,
                            nombresProductos
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spMedioPago.setAdapter(adapter);

                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Error al procesar los datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(MainActivity.this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}