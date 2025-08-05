package com.example.proyectolagranja.ui.Pedidos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.proyectolagranja.R;
public class PedidosFragment extends Fragment implements View.OnClickListener {
    private Button btnNuevoPedido;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pedidos, container, false);

        btnNuevoPedido = (Button) rootView.findViewById(R.id.btnNuevoPedido);
        btnNuevoPedido.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if(v == btnNuevoPedido){
            NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.action_nav_pedidos_to_nav_catalogo);
        }
    }
}