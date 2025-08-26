package com.example.proyectolagranja.ui.Pedidos;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.ArticuloDetalle;
import com.example.proyectolagranja.ui.Clases.Venta;
import com.example.proyectolagranja.ui.Pedidos.Adapter.ArticuloDetalleAdapter;
import com.example.proyectolagranja.ui.Pedidos.Adapter.PedidosAdapter;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.google.android.material.button.MaterialButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class PedidosFragment extends Fragment implements View.OnClickListener {
    private String fechaInicio = "", fechaFin = "";
    private EditText et_fecha_ini, et_fecha_fin;
    private Button btnNuevoPedido, btnCancelarPedido, btnVerMasPedido;
    private RecyclerView recyclerViewPedidos;
    private PedidosAdapter adapter;
    private List<Venta> listaVenta = new ArrayList<>();
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pedidos, container, false);

        recyclerViewPedidos = rootView.findViewById(R.id.recyclerViewPedidos);
        recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(getContext()));

        Spinner spEstado = rootView.findViewById(R.id.sp_estado);
        // Cuando el usuario selecciona un estado
        spEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int act_venta;

                switch (position) {
                    case 0: // Todos
                        act_venta = -1; // usamos -1 como bandera
                        break;
                    case 1: // Anulado
                        act_venta = 0;
                        break;
                    case 2: // Pendiente
                        act_venta = 2;
                        break;
                    case 3: // Despachado
                        act_venta = 3;
                        break;
                    case 4: // Entregado
                        act_venta = 4;
                        break;
                    case 5: // Pagado
                        act_venta = 5;
                        break;
                    default:
                        act_venta = -1;
                        break;
                }

                if (act_venta == -1) {
                    cargarPedidosCliente(); // todos los pedidos
                } else {
                    filtrarPorEstado(act_venta); // pedidos filtrados
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Inicializamos adapter vacío
        adapter = new PedidosAdapter(getContext(), listaVenta);
        recyclerViewPedidos.setAdapter(adapter);

        // listener de los botones de cada ítem
        adapter.setOnPedidoClickListener(new PedidosAdapter.OnPedidoClickListener() {
            @Override
            public void onCancelarClick(Venta venta) {
                mostrarDialogCancelarPedido(venta);
            }

            @Override
            public void onVerMasClick(Venta venta) {
                mostrarDialogVerMas(venta);
            }
        });

        et_fecha_ini = rootView.findViewById(R.id.et_fecha_ini);
        et_fecha_fin = rootView.findViewById(R.id.et_fecha_fin);
        et_fecha_ini.setOnClickListener(v -> mostrarDatePicker(true));
        et_fecha_fin.setOnClickListener(v -> mostrarDatePicker(false));

        btnNuevoPedido = (Button) rootView.findViewById(R.id.btnNuevoPedido);
        btnNuevoPedido.setOnClickListener(this);

        cargarPedidosCliente();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fechaInicio = "";
        fechaFin = "";
        if (et_fecha_ini != null) et_fecha_ini.setText("");
        if (et_fecha_fin != null) et_fecha_fin.setText("");
    }

    private void mostrarDatePicker(boolean esInicio) {
        final Calendar calendario = Calendar.getInstance();
        int anio = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                getContext(),
                R.style.MiDatePickerTheme,
                (view, year, month, dayOfMonth) -> {
            String fecha = year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", dayOfMonth);

            if (esInicio) {
                // Validar que no sea mayor que la fecha fin
                if (!fechaFin.isEmpty() && fecha.compareTo(fechaFin) > 0) {
                    Toast.makeText(getContext(), "La fecha de inicio no puede ser mayor que la fecha fin", Toast.LENGTH_SHORT).show();
                    return;
                }
                fechaInicio = fecha;
                et_fecha_ini.setText(fecha);
            } else {
                // Validar que no sea menor que la fecha inicio
                if (!fechaInicio.isEmpty() && fecha.compareTo(fechaInicio) < 0) {
                    Toast.makeText(getContext(), "La fecha fin no puede ser menor que la fecha inicio", Toast.LENGTH_SHORT).show();
                    return;
                }
                fechaFin = fecha;
                et_fecha_fin.setText(fecha);
            }
            if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
                filtrarPorFechas(fechaInicio, fechaFin);
            }        }, anio, mes, dia);
        datePicker.show();
    }


    private void filtrarPorFechas(String fecha_ini, String fecha_fin) {
        int id_cliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR +
                "pedido/pedido_filtro_fechas.php?id_cliente=" + id_cliente +
                "&fecha_ini=" + fecha_ini + "&fecha_fin=" + fecha_fin;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String respuesta = new String(responseBody, "UTF-8");
                    JSONArray jsonArray = new JSONArray(respuesta);

                    listaVenta.clear(); // limpiar lista antes de agregar resultados

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id_venta = obj.getInt("id_venta");
                        String num_venta = obj.getString("num_venta");
                        String fec_venta = obj.getString("fec_venta");
                        int id_pago_medio = obj.getInt("id_pago_medio");
                        int act_venta = obj.getInt("act_venta");
                        double tot_venta = obj.getDouble("tot_venta");

                        listaVenta.add(new Venta(id_venta, num_venta, fec_venta, id_pago_medio, act_venta, tot_venta));
                    }

                    adapter.notifyDataSetChanged();

                    if (listaVenta.isEmpty()) {
                        Toast.makeText(getContext(), "No hay pedidos en ese rango de fechas", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
    }

    private void filtrarPorEstado(int act_venta) {
        int id_cliente = 2267;

        AsyncHttpClient client = new AsyncHttpClient();
        String url = ServidorConfig.URL_SERVIDOR +
                "pedido/pedido_filtro_estado.php?act_venta=" +
                act_venta + "&id_cliente=" + id_cliente;

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody, "UTF-8");
                    JSONArray jsonArray = new JSONArray(response);

                    listaVenta.clear(); // Limpiamos la lista existente

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        int id_venta = obj.getInt("id_venta");
                        String num_venta = obj.getString("num_venta");
                        String fec_venta = obj.getString("fec_venta");
                        int id_pago_medio = obj.getInt("id_pago_medio");
                        int estado = obj.getInt("act_venta");
                        double tot_venta = obj.getDouble("tot_venta");

                        listaVenta.add(new Venta(id_venta, num_venta, fec_venta, id_pago_medio, estado, tot_venta));
                    }

                    adapter.notifyDataSetChanged();

                    if (listaVenta.isEmpty()) {
                        Toast.makeText(getContext(), "No hay pedidos de ese estado", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error al procesar los datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarPedidosCliente() {
        int idCliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR + "pedido/pedido_listar_cliente.php?id_cliente=" + idCliente;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String respuesta = new String(responseBody, "UTF-8");
                    JSONArray jsonArray = new JSONArray(respuesta);

                    listaVenta.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int id_venta = obj.getInt("id_venta");
                        String num_venta = obj.getString("num_venta");
                        String fec_venta = obj.getString("fec_venta");
                        int id_pago_medio = obj.getInt("id_pago_medio");
                        int act_venta = obj.getInt("act_venta");
                        double tot_venta = obj.getDouble("tot_venta");

                        listaVenta.add(new Venta(id_venta, num_venta, fec_venta, id_pago_medio, act_venta, tot_venta));
                    }

                    adapter.notifyDataSetChanged();

                    if (listaVenta.isEmpty()) {
                        Toast.makeText(getContext(), "No hay pedidos registrados", Toast.LENGTH_SHORT).show();
                    }


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

    private void mostrarDialogCancelarPedido(Venta venta) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.alert_dialog_cancelar_pedido, null);

        MaterialButton btnSi = dialogView.findViewById(R.id.btnCancelarSi);
        MaterialButton btnNo = dialogView.findViewById(R.id.btnCancelarNo);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Botón "Sí"
        btnSi.setOnClickListener(v -> {
            // Aquí llamas la función para cancelar el pedido
            CancelarPedido(venta.getId_venta());
            dialog.dismiss();
        });

        // Botón "No"
        btnNo.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void mostrarDialogVerMas(Venta venta) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.alert_dialog_ver_mas_articulos, null);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyMessage);
        MaterialButton btnCerrar = dialogView.findViewById(R.id.btnCerrar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<ArticuloDetalle> listaArticulos = new ArrayList<>();
        ArticuloDetalleAdapter adapterArticulos = new ArticuloDetalleAdapter(getContext(), listaArticulos);
        recyclerView.setAdapter(adapterArticulos);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        // Mostramos el Dialog
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // Llamamos al método que hace la consulta
        cargarDetallesVenta(venta.getId_venta(), listaArticulos, adapterArticulos, tvEmpty);
    }

    private void cargarDetallesVenta(int idVenta, List<ArticuloDetalle> listaArticulos,
                                     ArticuloDetalleAdapter adapterArticulos, TextView tvEmpty) {

        String url = ServidorConfig.URL_SERVIDOR + "pedido/pedido_obtener_detalle.php?id_venta=" + idVenta;
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String respuesta = new String(responseBody, "UTF-8");
                    JSONArray jsonArray = new JSONArray(respuesta);

                    listaArticulos.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String nombre = obj.getString("nom_articulo");
                        double precio = obj.getDouble("prec_venta_detalle");
                        int cantidad = obj.getInt("cant_venta_detalle");
                        double subTotal = precio * cantidad;
                        String imagenUrl = obj.optString("foto_articulo");

                        listaArticulos.add(new ArticuloDetalle(nombre, precio, cantidad, subTotal, imagenUrl));
                    }

                    adapterArticulos.notifyDataSetChanged();
                    tvEmpty.setVisibility(listaArticulos.isEmpty() ? View.VISIBLE : View.GONE);

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error al procesar los artículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void CancelarPedido(Integer id_venta){
        String url = ServidorConfig.URL_SERVIDOR + "pedido/pedido_cancelar.php?id_venta=" + id_venta;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String respuesta = new String(responseBody, "UTF-8");
                    JSONObject json = new JSONObject(respuesta);
                    String status = json.getString("status");
                    String message = json.getString("message");

                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                    if (status.equals("ok")) {
                        // Actualizamos la lista y el RecyclerView
                        cargarPedidosCliente();
                    }

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

    @Override
    public void onClick(View v) {
        if(v == btnNuevoPedido){
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_nav_pedidos_to_nav_catalogo);
        }
    }
}