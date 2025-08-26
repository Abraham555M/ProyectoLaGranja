package com.example.proyectolagranja.ui.Perfil;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.google.android.material.textfield.TextInputLayout;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;
import android.text.Editable;
import android.text.TextWatcher;

import cz.msebera.android.httpclient.Header;

public class PerfilFragment extends Fragment implements View.OnClickListener {
    private Button btnGuardarCambios;
    private EditText etNombresEd, etDocumentoEd, etTelefonoEd, etDireccionEd;
    private TextInputLayout tilTelefono, tilEditName, tilDireccion;

    // 🔹 Variables para guardar los valores originales
    private String nombresOriginal = "";
    private String documentoOriginal = "";
    private String telefonoOriginal = "";
    private String direccionOriginal = "";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_perfil, container, false);

        etNombresEd = rootView.findViewById(R.id.etNombresEd);
        etDocumentoEd = rootView.findViewById(R.id.etDocumentoEd);
        etTelefonoEd = rootView.findViewById(R.id.etTelefonoEd);
        etDireccionEd = rootView.findViewById(R.id.etDireccionEd);
        tilTelefono = rootView.findViewById(R.id.tilTelefono);
        tilEditName = rootView.findViewById(R.id.tilEditName);
        etDireccionEd = rootView.findViewById(R.id.etDireccionEd);

        // OnClick para cambiar el telefono
        tilTelefono.setEndIconOnClickListener(v -> onClick(tilTelefono));

        btnGuardarCambios = rootView.findViewById(R.id.btnGuardarCambios);
        btnGuardarCambios.setOnClickListener(this);

        // Desactivamos el botón al inicio
        btnGuardarCambios.setEnabled(false);
        // Escuchamos cambios en los EditText
        agregarTextWatcher(etNombresEd);
        agregarTextWatcher(etDocumentoEd);
        agregarTextWatcher(etTelefonoEd);
        agregarTextWatcher(etDireccionEd);

        cargarDatosUsuario();
        return rootView;
    }

    private void agregarTextWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Si cualquiera de los EditText tiene algo escrito, activamos el botón
                verificarCampos();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void verificarCampos() {
        boolean hayCambios =
                !etNombresEd.getText().toString().trim().equals(nombresOriginal) ||
                !etDocumentoEd.getText().toString().trim().equals(documentoOriginal) ||
                !etTelefonoEd.getText().toString().trim().equals(telefonoOriginal) ||
                !etDireccionEd.getText().toString().trim().equals(direccionOriginal);

        btnGuardarCambios.setEnabled(hayCambios);
    }

    private boolean validarCampos() {
        boolean valido = true;

        if (etNombresEd.getText().toString().trim().isEmpty()) {
            etNombresEd.setError("El nombre no puede estar vacío");
            valido = false;
        } else {
            etNombresEd.setError(null);
        }

        if (etDocumentoEd.getText().toString().trim().isEmpty()) {
            etDocumentoEd.setError("El documento no puede estar vacío");
            valido = false;
        } else {
            etDocumentoEd.setError(null);
        }

        if (etTelefonoEd.getText().toString().trim().isEmpty()) {
            etTelefonoEd.setError("El teléfono no puede estar vacío");
            valido = false;
        } else {
            etTelefonoEd.setError(null);
        }

        if (etDireccionEd.getText().toString().trim().isEmpty()) {
            etDireccionEd.setError("La dirección no puede estar vacía");
            valido = false;
        } else {
            etDireccionEd.setError(null);
        }

        return valido;
    }

    private void cargarDatosUsuario() {
        int idCliente = getActivity().getSharedPreferences("DatosUsuario", getActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_perfil.php?id_cliente=" + idCliente;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                try {
                    // Guardamos valores originales
                    nombresOriginal = response.getString("nom_cliente");
                    documentoOriginal = response.getString("num_doc_cliente");
                    telefonoOriginal = response.getString("tel_cliente");
                    direccionOriginal = response.getString("dir_cliente");

                    // Seteamos los EditText
                    etNombresEd.setText(nombresOriginal);
                    etDocumentoEd.setText(documentoOriginal);
                    etTelefonoEd.setText(telefonoOriginal);
                    etDireccionEd.setText(direccionOriginal);

                    // Limpiar errores al cargar los datos
                    etNombresEd.setError(null);
                    etDocumentoEd.setError(null);
                    etTelefonoEd.setError(null);
                    etDireccionEd.setError(null);

                    // Al cargar datos → botón deshabilitado
                    btnGuardarCambios.setEnabled(false);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error parseando JSON", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarDatosUsuario() {
        int idCliente = requireActivity()
                .getSharedPreferences("DatosUsuario", requireActivity().MODE_PRIVATE)
                .getInt("id_cliente", 2267);

        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_actualizar_perfil.php";

        RequestParams params = new RequestParams();
        params.put("id_cliente", idCliente);
        params.put("nom_cliente", etNombresEd.getText().toString().trim());
        params.put("dir_cliente", etDireccionEd.getText().toString().trim());
        params.put("num_doc_cliente", etDocumentoEd.getText().toString().trim());

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String respuesta = new String(responseBody);
                if (respuesta.equals("ok")) {
                    Toast.makeText(requireContext(), "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();

                    // 🔹 Actualizamos los valores originales con los nuevos
                    nombresOriginal = etNombresEd.getText().toString().trim();
                    documentoOriginal = etDocumentoEd.getText().toString().trim();
                    telefonoOriginal = etTelefonoEd.getText().toString().trim();
                    direccionOriginal = etDireccionEd.getText().toString().trim();

                    // 🔹 Guardar el nombre actualizado en SharedPreferences para el nav header
                    SharedPreferences prefs = requireActivity().getSharedPreferences("UsuarioPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("nom_cliente", nombresOriginal);
                    editor.apply(); // Esto disparará el listener en MainActivity

                    btnGuardarCambios.setEnabled(false); // 🔹 Lo desactivamos porque ya está igual
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(requireContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoCambiarNumero() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.alert_dialog_cambiar_numero, null);

        Button btnSi = dialogView.findViewById(R.id.btnCancelarSi);
        Button btnNo = dialogView.findViewById(R.id.btnCancelarNo);

        // Creamos el AlertDialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false) //Si o No
                .create();

        // Acción botón "Sí"
        btnSi.setOnClickListener(v -> {
            dialog.dismiss();
            // Navegar al otro fragment
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_nav_perfil_to_nav_actualizar_telefono);
        });

        // Acción botón "No"
        btnNo.setOnClickListener(v -> {
            dialog.dismiss(); // Cerrar el diálogo
        });

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void mostrarDialogoActualizarPerfil() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.alert_dialog_confirmacion_actualizar_perfil, null);

        Button btnSi = dialogView.findViewById(R.id.btnCancelarSi);
        Button btnNo = dialogView.findViewById(R.id.btnCancelarNo);

        // Creamos el AlertDialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false) //Si o No
                .create();

        // Acción botón "Sí"
        btnSi.setOnClickListener(v -> {
            actualizarDatosUsuario();
            dialog.dismiss();
        });

        // Acción botón "No"
        btnNo.setOnClickListener(v -> {
            cargarDatosUsuario();
            dialog.dismiss(); // Cerrar el diálogo
        });

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onClick(View view) {
        if(view == tilTelefono) {
            mostrarDialogoCambiarNumero();
        }
        if (view == btnGuardarCambios) {
            if (validarCampos()) {
                mostrarDialogoActualizarPerfil();
            }
        }
    }
}