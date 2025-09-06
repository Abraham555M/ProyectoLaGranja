package com.example.proyectolagranja.ui.Autenticacion;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.example.proyectolagranja.ui.Servicios.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class CrearCuenta extends Fragment implements View.OnClickListener{
    private Button btnCrearUsuario;
    private EditText etNombres, etDocumento, etDireccion;
    private String telefono; // Guardamos el telefono recibido

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_crear_cuenta, container, false);

        etNombres = (EditText) rootView.findViewById(R.id.etNombres);
        etDocumento = (EditText) rootView.findViewById(R.id.etDocumento);
        etDireccion = (EditText) rootView.findViewById(R.id.etDireccion);
        btnCrearUsuario = (Button) rootView.findViewById(R.id.btnCrearUsuario);
        btnCrearUsuario.setOnClickListener(this);

        // Recibir teléfono desde InicioSesion
        if (getArguments() != null) {
            telefono = getArguments().getString("telefono");
        }

        return rootView;
    }

    private void crearCliente() {
        String nom_cliente = etNombres.getText().toString();
        String num_doc_cliente = etDocumento.getText().toString();
        String dir_cliente = etDireccion.getText().toString();

        // Obtener el token de Firebase antes de enviar
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.getException());
                        Toast.makeText(getActivity(), "No se pudo obtener token de notificación", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fcmToken = task.getResult(); // Token del dispositivo

                    RequestParams params = new RequestParams();
                    params.put("nom_cliente", nom_cliente);
                    params.put("num_doc_cliente", num_doc_cliente);
                    params.put("dir_cliente", dir_cliente);
                    params.put("tel_cliente", telefono);
                    params.put("tok_fcm_cliente", fcmToken);

                    String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_crear.php";
                    AsyncHttpClient client = new AsyncHttpClient();

                    client.post(url, params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            try {
                                String response = new String(responseBody);
                                JSONObject json = new JSONObject(response);

                                if (json.getBoolean("success")) {
                                    Toast.makeText(getActivity(), "Cliente creado correctamente", Toast.LENGTH_SHORT).show();

                                    int idCliente = json.getInt("id_cliente"); // El metodo, devuelve el id
                                    SessionManager session = new SessionManager(requireContext());
                                    session.createLoginSession(idCliente, nom_cliente, telefono);

                                    limpiarCampos();

                                    NavController navController = Navigation.findNavController(
                                            getActivity(),
                                            R.id.nav_host_fragment_content_main
                                    );
                                    navController.navigate(R.id.action_nav_crear_cuenta_to_nav_catalogo);

                                } else {
                                    Toast.makeText(getActivity(), "Error al crear el cliente", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e("JSON_ERROR", "Error parseando respuesta", e);
                                Toast.makeText(getActivity(), "Error procesando respuesta", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Toast.makeText(getActivity(), "Error en la conexión", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    public void limpiarCampos() {
        etNombres.setText("");
        etDocumento.setText("");
        etDireccion.setText("");
    }

    private boolean validarCampos() {
        if (etNombres.getText().toString().trim().isEmpty()) {
            Toast.makeText(getActivity(), "Ingrese su nombre", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDocumento.getText().toString().trim().isEmpty()) {
            Toast.makeText(getActivity(), "Ingrese su documento", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDocumento.length() != 8) {
            Toast.makeText(getActivity(), "El documento debe tener 8 dígitos", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDireccion.getText().toString().trim().isEmpty()) {
            Toast.makeText(getActivity(), "Ingrese su dirección", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == btnCrearUsuario) {
            if (validarCampos()) {
                crearCliente();
            }
        }
    }
}