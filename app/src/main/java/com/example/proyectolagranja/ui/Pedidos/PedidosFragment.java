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

import com.example.proyectolagranja.R;

import java.util.Calendar;

public class PedidosFragment extends Fragment implements View.OnClickListener {
    private String fechaInicio = "", fechaFin = "";
    private EditText et_fecha_ini, et_fecha_fin;
    private Button btnNuevoPedido;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pedidos, container, false);

        et_fecha_ini = rootView.findViewById(R.id.et_fecha_ini);
        et_fecha_fin = rootView.findViewById(R.id.et_fecha_fin);

        et_fecha_ini.setOnClickListener(v -> mostrarDatePicker(true));
        et_fecha_fin.setOnClickListener(v -> mostrarDatePicker(false));


        btnNuevoPedido = (Button) rootView.findViewById(R.id.btnNuevoPedido);
        btnNuevoPedido.setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        if(v == btnNuevoPedido){
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_nav_pedidos_to_nav_catalogo);
        }
    }
}