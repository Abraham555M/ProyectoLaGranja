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
import android.widget.LinearLayout;

import com.example.proyectolagranja.R;

public class InicioSesion extends Fragment {
    private LinearLayout layoutBienvenida, layoutCodigo;
    private Button btnEnviarTelefono, btnValidarCodigo;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inicio_sesion, container, false);
        // Referencias
        layoutBienvenida = rootView.findViewById(R.id.layout_bienvenida);
        layoutCodigo = rootView.findViewById(R.id.layout_codigo);
        btnEnviarTelefono = rootView.findViewById(R.id.btnEnviarTelefono);
        btnValidarCodigo = rootView.findViewById(R.id.btnValidarCodigo);

        // Acción del botón Enviar Teléfono
        btnEnviarTelefono.setOnClickListener(v -> {
            // Mostrar AlertDialog de confirmación
            new AlertDialog.Builder(requireContext())
                    .setMessage("¿Está seguro que este es su número?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        layoutBienvenida.setVisibility(View.GONE);
                        layoutCodigo.setVisibility(View.VISIBLE);
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // Acción del botón Validar Código (aquí iría tu lógica real)
        btnValidarCodigo.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_nav_inicio_sesion_to_nav_catalogo);
        });

        return rootView;
    }
}