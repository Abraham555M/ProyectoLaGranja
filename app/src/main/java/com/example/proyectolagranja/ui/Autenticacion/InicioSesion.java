package com.example.proyectolagranja.ui.Autenticacion;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyectolagranja.R;

public class InicioSesion extends Fragment implements View.OnClickListener {
    private LinearLayout layoutBienvenida, layoutCodigo;
    private EditText etTelefono;
    private Button btnEnviarTelefono, btnValidarCodigo;
    private TextView etEnlaceReenviar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inicio_sesion, container, false);

        layoutBienvenida = rootView.findViewById(R.id.layout_bienvenida);
        layoutCodigo = rootView.findViewById(R.id.layout_codigo);
        btnEnviarTelefono = rootView.findViewById(R.id.btnEnviarTelefono);
        btnValidarCodigo = rootView.findViewById(R.id.btnValidarCodigo);
        etTelefono = rootView.findViewById(R.id.etTelefono);


        btnValidarCodigo.setOnClickListener(this);
        btnEnviarTelefono.setOnClickListener(this);
        etEnlaceReenviar.setOnClickListener(this);

        return rootView;
    }
    private void mostrarDialogoConfirmarNumero(String telefono) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.alert_dialog_confirmar_numero, null);

        Button btnSi = dialogView.findViewById(R.id.btnSi);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnSi.setOnClickListener(v -> {
            layoutBienvenida.setVisibility(View.GONE);
            layoutCodigo.setVisibility(View.VISIBLE);
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onClick(View v) {
        if (v == btnEnviarTelefono) {
            String telefono = etTelefono.getText().toString().trim();

            if (telefono.isEmpty()) {
                Toast.makeText(requireContext(), "Ingrese su número de teléfono", Toast.LENGTH_SHORT).show();
                return;
            }
            if (telefono.length() < 9) {
                Toast.makeText(requireContext(), "El número debe tener al menos 9 dígitos", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogoConfirmarNumero(telefono);
        }


        if (v == btnValidarCodigo) {
            // Referencias a los EditText de código
            EditText et1 = getView().findViewById(R.id.etCodigo1);
            EditText et2 = getView().findViewById(R.id.etCodigo2);
            EditText et3 = getView().findViewById(R.id.etCodigo3);
            EditText et4 = getView().findViewById(R.id.etCodigo4);
            EditText et5 = getView().findViewById(R.id.etCodigo5);
            EditText et6 = getView().findViewById(R.id.etCodigo6);

            // Verificar que todos tengan valor
            if (et1.getText().toString().trim().isEmpty() ||
                    et2.getText().toString().trim().isEmpty() ||
                    et3.getText().toString().trim().isEmpty() ||
                    et4.getText().toString().trim().isEmpty() ||
                    et5.getText().toString().trim().isEmpty() ||
                    et6.getText().toString().trim().isEmpty()) {

                Toast.makeText(requireContext(), "Por favor, complete todos los dígitos del código", Toast.LENGTH_SHORT).show();
                return; // Salir sin navegar
            }

            // Si todos están llenos, concatenar el código (opcional)
            String codigo = et1.getText().toString() +
                    et2.getText().toString() +
                    et3.getText().toString() +
                    et4.getText().toString() +
                    et5.getText().toString() +
                    et6.getText().toString();

            NavController navController = Navigation.findNavController(
                    requireActivity(),
                    R.id.nav_host_fragment_content_main
            );
            navController.navigate(R.id.action_nav_inicio_sesion_to_nav_catalogo);
        }
    }
}