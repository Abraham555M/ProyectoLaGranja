package com.example.proyectolagranja.ui.Pedidos;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Clases.Venta;
import com.example.proyectolagranja.ui.Pedidos.Adapter.PedidosAdapter;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PedidosFragment extends Fragment implements View.OnClickListener {
    private String fechaInicio = "", fechaFin = "";
    private EditText et_fecha_ini, et_fecha_fin;
    private Button btnNuevoPedido;
    private RecyclerView recyclerViewPedidos;
    private PedidosAdapter adapter;
    private List<Venta> listaPedidos = new ArrayList<>();
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pedidos, container, false);

        recyclerViewPedidos = rootView.findViewById(R.id.recyclerViewPedidos);
        recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializamos adapter vacío
        adapter = new PedidosAdapter(getContext(), listaPedidos);
        recyclerViewPedidos.setAdapter(adapter);

        et_fecha_ini = rootView.findViewById(R.id.et_fecha_ini);
        et_fecha_fin = rootView.findViewById(R.id.et_fecha_fin);
        et_fecha_ini.setOnClickListener(v -> mostrarDatePicker(true));
        et_fecha_fin.setOnClickListener(v -> mostrarDatePicker(false));

        btnNuevoPedido = (Button) rootView.findViewById(R.id.btnNuevoPedido);
        btnNuevoPedido.setOnClickListener(this);

        cargarPedidosCliente();
        return rootView;
    }

    private void mostrarDatePicker(boolean esInicio) {
        final Calendar calendario = Calendar.getInstance();
        int anio = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
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
        }, anio, mes, dia);
        datePicker.show();
    }

    private void cargarPedidosCliente() {
        int idCliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 1);

        String url = ServidorConfig.URL_SERVIDOR + "pedido/pedido_listar_cliente.php?id_cliente=" + idCliente;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                try {
                    String respuesta = new String(responseBody, "UTF-8");
                    JSONArray jsonArray = new JSONArray(respuesta);

                    listaPedidos.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        int idVenta = obj.getInt("id_venta");
                        String numVenta = obj.getString("num_venta");
                        String fecha = obj.getString("fec_venta");
                        int idPago = obj.getInt("id_pago_medio");
                        int estado = obj.getInt("act_venta");
                        double total = obj.getDouble("tot_venta");

                        listaPedidos.add(new Venta(idVenta, numVenta, fecha, idPago, estado, total));
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



    @Override
    public void onClick(View v) {
        if(v == btnNuevoPedido){
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_nav_pedidos_to_nav_catalogo);
        }
    }
}