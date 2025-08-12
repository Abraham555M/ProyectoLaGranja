package com.example.proyectolagranja;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.proyectolagranja.ui.Catalogo.Adapter.CarritoAdapter;
import com.example.proyectolagranja.ui.Clases.ItemCarrito;
import com.example.proyectolagranja.ui.Clases.Producto;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
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

public class MainActivity extends AppCompatActivity {
    private List<Producto> listaMedioPago = new ArrayList<>();
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private Spinner spMedioPago;
    private Button btnCerrar;
    public static List<ItemCarrito> carrito = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Inflar el layout de tu alert_dialog_carrito
                View dialogView = getLayoutInflater().inflate(R.layout.alert_dialog_carrito, null);
                spMedioPago = dialogView.findViewById(R.id.spMedioPago);
                btnCerrar = dialogView.findViewById(R.id.btnCerrar);

                // --- RecyclerView para mostrar el carrito ---
                RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(new CarritoAdapter(MainActivity.this, carrito));
                // --------------------------------------------

                // Crear el AlertDialog
                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setView(dialogView)
                        .create();
                btnCerrar.setOnClickListener(v -> dialog.dismiss());


                cargarMedioPago();

                dialog.show();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

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
                    listaMedioPago.add(new Producto(0, "Seleccione un medio de pago"));
                    nombresProductos.add("Seleccione un medio de pago");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id = obj.getInt("id_pago_medio");
                        String nombre = obj.getString("nom_pago_medio");

                        listaMedioPago.add(new Producto(id, nombre));
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