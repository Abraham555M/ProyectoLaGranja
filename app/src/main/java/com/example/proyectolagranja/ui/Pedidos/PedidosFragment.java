package com.example.proyectolagranja.ui.Pedidos;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.ArticuloDetalle;
import com.example.proyectolagranja.ui.Clases.Venta;
import com.example.proyectolagranja.ui.Pedidos.Adapter.ArticuloDetalleAdapter;
import com.example.proyectolagranja.ui.Pedidos.Adapter.PedidosAdapter;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.example.proyectolagranja.ui.Servicios.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class PedidosFragment extends Fragment implements View.OnClickListener {
    private String fechaInicio = "", fechaFin = "";
    private EditText etFechaIni, etFechaFin;
    private Button btnNuevoPedido;
    private RecyclerView recyclerViewPedidos;
    private PedidosAdapter adapter;
    private List<Venta> listaVenta = new ArrayList<>();
    private SessionManager session;
    private LinearLayout emptyStateLayout;
    private TextView tvEmptyMessage;
    private ImageView ivEmptyIcon;
    private SwipeRefreshLayout swipeRefreshLayout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pedidos, container, false);

        session = new SessionManager(requireContext());
        recyclerViewPedidos = rootView.findViewById(R.id.recyclerViewPedidos);
        recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyStateLayout = rootView.findViewById(R.id.emptyStateLayout);
        tvEmptyMessage = rootView.findViewById(R.id.tvEmptyMessage);
        ivEmptyIcon = rootView.findViewById(R.id.ivEmptyIcon);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);

        // Configurar el SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            cargarPedidosCliente(); // recargar pedidos
        });

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

        String[] estados = getResources().getStringArray(R.array.listado_estado);

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner_estado, // layout personalizado
                estados
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                setColorFondo(position, view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                setColorFondo(position, view);
                return view;
            }

            private void setColorFondo(int position, View view) {
                int color;
                switch (getItem(position)) {
                    case "Cancelado":
                        color = ContextCompat.getColor(requireContext(), R.color.color_cancelar);
                        break;
                    case "Pendiente":
                        color = ContextCompat.getColor(requireContext(), R.color.color_pendiente);
                        break;
                    case "Despachado":
                        color = ContextCompat.getColor(requireContext(), R.color.color_despachado);
                        break;
                    case "Entregado":
                        color = ContextCompat.getColor(requireContext(), R.color.color_entregado);
                        break;
                    case "Pagado":
                        color = ContextCompat.getColor(requireContext(), R.color.darker_gray);
                        break;
                    default: // "Todos" u otros
                        color = ContextCompat.getColor(requireContext(), android.R.color.white);
                        break;
                }
                view.setBackgroundColor(color);

                TextView tv = view.findViewById(R.id.tvEstado);
                if (tv != null) {
                    tv.setTextColor(Color.BLACK);
                }
            }
        };

        spEstado.setAdapter(adapterSpinner);

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

        etFechaIni = rootView.findViewById(R.id.etFechaIni);
        etFechaFin = rootView.findViewById(R.id.etFechaFin);
        etFechaIni.setOnClickListener(v -> mostrarDatePicker(true));
        etFechaFin.setOnClickListener(v -> mostrarDatePicker(false));

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
        if (etFechaIni != null) etFechaIni.setText("");
        if (etFechaFin != null) etFechaFin.setText("");
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
                etFechaIni.setText(fecha);
            } else {
                // Validar que no sea menor que la fecha inicio
                if (!fechaInicio.isEmpty() && fecha.compareTo(fechaInicio) < 0) {
                    Toast.makeText(getContext(), "La fecha fin no puede ser menor que la fecha inicio", Toast.LENGTH_SHORT).show();
                    return;
                }
                fechaFin = fecha;
                etFechaFin.setText(fecha);
            }
            if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
                filtrarPorFechas(fechaInicio, fechaFin);
            }        }, anio, mes, dia);
        datePicker.show();
    }

    private void filtrarPorFechas(String fecha_ini, String fecha_fin) {
        int idCliente = session.getIdCliente();

        String url = ServidorConfig.URL_SERVIDOR +
                "pedido/pedido_filtro_fechas.php?id_cliente=" + idCliente +
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
        int idCliente = session.getIdCliente();

        AsyncHttpClient client = new AsyncHttpClient();
        String url = ServidorConfig.URL_SERVIDOR +
                "pedido/pedido_filtro_estado.php?act_venta=" +
                act_venta + "&id_cliente=" + idCliente;

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody, "UTF-8");
                    JSONArray jsonArray = new JSONArray(response);

                    listaVenta.clear();

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
                        mostrarEmptyState("No tienes pedidos en este estado", R.drawable.ic_carrito_compras);
                    } else {
                        ocultarEmptyState();
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
        int idCliente = session.getIdCliente();

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
                        mostrarEmptyState("No tienes pedidos registrados", R.drawable.ic_carrito_compras);
                    } else {
                        ocultarEmptyState();
                    }

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                } finally {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void mostrarEmptyState(String mensaje, int iconRes) {
        recyclerViewPedidos.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);

        tvEmptyMessage.setText(mensaje);
        ivEmptyIcon.setImageResource(iconRes);
    }

    private void ocultarEmptyState() {
        recyclerViewPedidos.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
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
        TextView tvMensajeTotal = dialogView.findViewById(R.id.tvMensajeTotal);
        TextView tvMensajeEstado = dialogView.findViewById(R.id.tvMensajeEstado);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<ArticuloDetalle> listaArticulos = new ArrayList<>();
        ArticuloDetalleAdapter adapterArticulos = new ArticuloDetalleAdapter(getContext(), listaArticulos);
        recyclerView.setAdapter(adapterArticulos);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        cargarDetallesVenta(venta.getId_venta(), listaArticulos, adapterArticulos, tvEmpty, tvMensajeTotal, tvMensajeEstado, venta.getAct_venta());
    }

    private void cargarDetallesVenta(int idVenta, List<ArticuloDetalle> listaArticulos,
                                     ArticuloDetalleAdapter adapterArticulos, TextView tvEmpty, TextView tvMensajeTotal,
                                     TextView tvMensajeEstado, int actVenta) {

        String url = ServidorConfig.URL_SERVIDOR + "pedido/pedido_obtener_detalle.php?id_venta=" + idVenta;
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String respuesta = new String(responseBody, "UTF-8");
                    JSONArray jsonArray = new JSONArray(respuesta);

                    listaArticulos.clear();
                    double total = 0;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String nombre = obj.getString("nom_articulo");
                        double precio = obj.getDouble("prec_venta_detalle");
                        double cantidad = obj.getDouble("cant_venta_detalle");
                        double subTotal = precio * cantidad;
                        String imagenUrl = obj.optString("foto_articulo");
                        total += subTotal;

                        listaArticulos.add(new ArticuloDetalle(nombre, precio, cantidad, subTotal, imagenUrl));
                    }

                    adapterArticulos.notifyDataSetChanged();
                    tvEmpty.setVisibility(listaArticulos.isEmpty() ? View.VISIBLE : View.GONE);

                    // Actualizar total
                    tvMensajeTotal.setText("Total: S/ " + String.format("%.2f", total));

                    // Actualizar estado
                    String estadoTexto;
                    int colorFondo;
                    switch (actVenta) {
                        case 0: estadoTexto = "Cancelado"; colorFondo = getResources().getColor(R.color.color_cancelar); break;
                        case 2: estadoTexto = "Pendiente"; colorFondo = getResources().getColor(R.color.color_pendiente); break;
                        case 3: estadoTexto = "Despachado"; colorFondo = getResources().getColor(R.color.color_despachado); break;
                        case 4: estadoTexto = "Entregado"; colorFondo = getResources().getColor(R.color.color_entregado); break;
                        case 5: estadoTexto = "Pagado"; colorFondo = getResources().getColor(R.color.darker_gray); break;
                        default: estadoTexto = "Desconocido"; colorFondo = getResources().getColor(android.R.color.darker_gray); break;
                    }
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setColor(colorFondo);
                    drawable.setCornerRadius(30f);
                    tvMensajeEstado.setText(estadoTexto);
                    tvMensajeEstado.setBackground(drawable);
                    int padding = 20;
                    tvMensajeEstado.setPadding(padding, padding/2, padding, padding/2);
                    tvMensajeEstado.setPadding(padding, padding/2, padding, padding/2);

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