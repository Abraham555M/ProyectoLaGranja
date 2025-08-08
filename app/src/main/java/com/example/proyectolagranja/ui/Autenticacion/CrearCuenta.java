package com.example.proyectolagranja.ui.Autenticacion;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.proyectolagranja.R;

public class CrearCuenta extends Fragment implements View.OnClickListener{
    private Button btnCrearUsuario;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_crear_cuenta, container, false);

        btnCrearUsuario = (Button) rootView.findViewById(R.id.btnCrearUsuario);
        btnCrearUsuario.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        if(v == btnCrearUsuario){
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_nav_crear_cuenta_to_nav_catalogo);
        }
    }
}