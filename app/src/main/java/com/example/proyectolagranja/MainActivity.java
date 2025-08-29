package com.example.proyectolagranja;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.proyectolagranja.ui.Catalogo.Adapter.CarritoAdapter;
import com.example.proyectolagranja.ui.Clases.ItemCarrito;
import com.example.proyectolagranja.ui.Clases.MedioPago;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.example.proyectolagranja.ui.Servicios.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
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
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import com.loopj.android.http.RequestParams;

public class MainActivity extends AppCompatActivity {
    private List<MedioPago> listaMedioPago = new ArrayList<>();
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private Spinner spMedioPago;
    private Button btnCerrar, btnEnviarPedido;
    private TextView tvBadge;
    private EditText etDireccion, etDetalleVenta;
    public static List<ItemCarrito> carrito = new ArrayList<>();
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar SessionManager
        session = new SessionManager(this);

        // Siempre infla el layout principal
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        tvBadge = findViewById(R.id.tvBadge);
        binding.appBarMain.fab.setOnClickListener(v -> mostrarCarrito());
        actualizarBadge();

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_catalogo, R.id.nav_pedidos, R.id.nav_perfil)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // 🔹 Manejo del logout
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                // Limpiar la sesión
                session.logout();

                // Limpiar manualmente el header
                View headerView2 = navigationView.getHeaderView(0);
                TextView tvNombre = headerView2.findViewById(R.id.tvNombre);
                TextView tvTelefono = headerView2.findViewById(R.id.tvTelefono);
                tvNombre.setText("");
                tvTelefono.setText("");

                // Redirigir al inicio de sesión
                navController.navigate(R.id.nav_inicio_sesion);

                // Cerrar el drawer
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
            // Mantener el comportamiento normal de navegación
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawer.closeDrawer(GravityCompat.START);
            }
            return handled;
        });

        // Cargar imagen del logo
        View headerView = navigationView.getHeaderView(0);
        ImageView imageView = headerView.findViewById(R.id.imageView);
        Glide.with(this)
                .load("https://i.postimg.cc/3RcFbyqg/logo-blanco-1.png")
                .into(imageView);

        // Actualizar el header
        prefs = getSharedPreferences("DatosUsuario", MODE_PRIVATE);
        listener = (sharedPrefs, key) -> {
            if (key != null && (key.equals("tel_cliente") || key.equals("nom_cliente"))) {
                actualizarHeader();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);

        // Mostrar datos si hay sesión activa
        if (session.isLoggedIn()) {
            actualizarHeader();
            ObtenerDatosUsuario(session.getIdCliente());
            navController.navigate(R.id.nav_catalogo);
        } else {
            //  Si NO hay sesión → navegar al fragment de inicio de sesión
            navController.navigate(R.id.nav_inicio_sesion);
        }

        // Para que no sea visible el encabezado
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_crear_cuenta || destination.getId() == R.id.nav_inicio_sesion || destination.getId() == R.id.nav_actualizar_telefono) {
                binding.appBarMain.toolbar.setVisibility(View.GONE); // Quitar el encabezado
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Desactiva swipe
                binding.appBarMain.fab.setVisibility(View.GONE); // Quitar el carrito de compra
                binding.appBarMain.tvBadge.setVisibility(View.GONE); // Quitar el icono rojo del carrito
            } else {
                binding.appBarMain.toolbar.setVisibility(View.VISIBLE);
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED); // Reactiva swipe
                binding.appBarMain.fab.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (prefs != null && listener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    public void actualizarHeader() {
        View headerView = binding.navView.getHeaderView(0);
        TextView tvNombre = headerView.findViewById(R.id.tvNombre);
        TextView tvTelefono = headerView.findViewById(R.id.tvTelefono);

        if (session.isLoggedIn()) {
            tvNombre.setText(session.getNombre());
            tvTelefono.setText(session.getTelefono());
        }
    }

    private void ObtenerDatosUsuario(int idCliente){
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_obtener_datos.php?id_cliente=" + idCliente;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject json = new JSONObject(new String(responseBody));

                    if (json.has("nom_cliente") && json.has("tel_cliente")) {
                        String nombre = json.getString("nom_cliente");
                        String telefono = json.getString("tel_cliente");

                        // Actualizar UI
                        View headerView = binding.navView.getHeaderView(0);
                        TextView tvNombre = headerView.findViewById(R.id.tvNombre);
                        TextView tvTelefono = headerView.findViewById(R.id.tvTelefono);

                        tvNombre.setText(nombre);
                        tvTelefono.setText(telefono);

                        // Actualizar sesión
                        session.createLoginSession(idCliente, nombre, telefono);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error al procesar datos del usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(MainActivity.this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
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
        LinearLayout layoutDetallesCarrito = dialogView.findViewById(R.id.layoutDetallesCarrito);
        MaterialButton btnAgregarCarrito = dialogView.findViewById(R.id.btnAgregarCarrito);

        cargarDireccionCliente(etDireccion);

        if (carrito.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            layoutDetallesCarrito.setVisibility(View.GONE);
            btnAgregarCarrito.setVisibility(View.VISIBLE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            layoutDetallesCarrito.setVisibility(View.VISIBLE);
            btnAgregarCarrito.setVisibility(View.GONE);

            double totalInicial = 0;
            for (ItemCarrito item : carrito) {
                double precio = Double.parseDouble(item.getArticulo().getPrecio());
                totalInicial += precio * item.getCantidad();
            }
            tvTotal.setText("Total: S/" + redondearTotal(totalInicial));

            recyclerView.setAdapter(new CarritoAdapter( // Carrito interactivo
                    MainActivity.this,
                    carrito,
                    total -> {
                        actualizarBadge();
                        tvTotal.setText("Total: S/" + redondearTotal(total));
                        if (carrito.isEmpty()) {
                            tvEmptyMessage.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            btnEnviarPedido.setVisibility(View.GONE);
                            layoutDetallesCarrito.setVisibility(View.GONE);
                            btnAgregarCarrito.setVisibility(View.VISIBLE);
                        } else {
                            tvEmptyMessage.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            btnEnviarPedido.setVisibility(View.VISIBLE); // volver a mostrar si tiene algo
                        }
                    }
            ));
        }

        // Crear el AlertDialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                .setView(dialogView)
                .create();
        dialog.setCancelable(false); // Evita que se cierre presionando atras
        dialog.setCanceledOnTouchOutside(false); // Evita que se cierre tocando afuera

        btnCerrar.setOnClickListener(v -> dialog.dismiss());
        btnAgregarCarrito.setOnClickListener(v -> dialog.dismiss());

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

            mostrarDialogoConfirmacion(
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

    private String redondearTotal(double v) {
        return String.format(Locale.US, "%.2f", v); // redondea a 2 decimales
    }

    public void EnviarPedido(double total, String direccion, String detalle_venta, androidx.appcompat.app.AlertDialog dialog){
        int idCliente = session.getIdCliente(); // Suplanta al SharePreference -> obtiene el id del cliente
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
                        EnviarDetallesPedido(idVenta);
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

    private void EnviarDetallesPedido(int idVenta) {
        AsyncHttpClient client = new AsyncHttpClient();
        int totalDetalles = carrito.size();  // Cantidad de detalles a enviar
        final int[] detallesEnviados = {0};

        for (ItemCarrito item : carrito) {
            RequestParams params = new RequestParams();
            params.put("id_venta", idVenta);
            params.put("id_articulo", item.getArticulo().getId());
            params.put("cant_venta_detalle", item.getCantidad());
            params.put("prec_venta_detalle", item.getArticulo().getPrecio());

            String url = ServidorConfig.URL_SERVIDOR + "pedido/pedido_registrar_detalle.php";

            client.post(url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    detallesEnviados[0]++; // Contador

                    // Solo mostrar el AlertDialog cuando todos los detalles se hayan enviado
                    if (detallesEnviados[0] == totalDetalles) {
                        mostrarDialogPedidoRealizado();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Toast.makeText(MainActivity.this, "Error al guardar detalle", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void mostrarDialogoConfirmacion(double total, String direccion, String detalle, androidx.appcompat.app.AlertDialog dialogCarrito) {
        // Inflar el layout personalizado
        View view = getLayoutInflater().inflate(R.layout.alert_dialog_confirmar_pedido, null);

        MaterialButton btnSi = view.findViewById(R.id.btnCancelarSi);
        MaterialButton btnNo = view.findViewById(R.id.btnCancelarNo);

        // Crear el AlertDialog con vista personalizada
        androidx.appcompat.app.AlertDialog dialogConfirmacion = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        dialogConfirmacion.setCancelable(false);

        // Botón Sí
        btnSi.setOnClickListener(v -> {
            EnviarPedido(
                    Double.parseDouble(redondearTotal(total)),
                    direccion,
                    detalle,
                    dialogCarrito
            );
            dialogConfirmacion.dismiss(); // cierro el de confirmación
        });

        // Botón No
        btnNo.setOnClickListener(v -> dialogConfirmacion.dismiss());

        dialogConfirmacion.show();
        dialogConfirmacion.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private void mostrarDialogPedidoRealizado() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.alert_dialog_pedido_realizado, null);
        MaterialButton btnCerrar = view.findViewById(R.id.btnCerrar);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();

        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        btnCerrar.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
        int idCliente = session.getIdCliente();
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