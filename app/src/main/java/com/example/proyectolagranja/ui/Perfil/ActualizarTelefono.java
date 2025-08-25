package com.example.proyectolagranja.ui.Perfil;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.Toast;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class ActualizarTelefono extends Fragment implements View.OnClickListener{
    private LinearLayout layoutBienvenida, layoutCodigo;
    private EditText etTelefonoEd;
    private Button btnEnviarTelefonoEd, btnValidarCodigoEd;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_actualizar_telefono, container, false);
        layoutBienvenida = rootView.findViewById(R.id.layout_bienvenida);
        layoutCodigo = rootView.findViewById(R.id.layout_codigo);
        btnEnviarTelefonoEd = rootView.findViewById(R.id.btnEnviarTelefonoEd);
        btnValidarCodigoEd = rootView.findViewById(R.id.btnValidarCodigoEd);
        etTelefonoEd = rootView.findViewById(R.id.etTelefonoEd);

        btnValidarCodigoEd.setOnClickListener(this);
        btnEnviarTelefonoEd.setOnClickListener(this);

        configurarAutoFocusCodigo(rootView);

        return rootView;
    }

    private void mostrarDialogoConfirmarNumero() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.alert_dialog_confirmar_numero, null);

        Button btnSi = dialogView.findViewById(R.id.btnConfirmarSi);
        Button btnNo = dialogView.findViewById(R.id.btnConfirmarNo);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnSi.setOnClickListener(v -> {
            String telefono = etTelefonoEd.getText().toString().trim();
            validarTelefono(telefono, dialog); // 👈 validar antes de avanzar
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void configurarAutoFocusCodigo(View rootView) { // Para los digitos del codigo
        EditText[] edits = {
                rootView.findViewById(R.id.etCodigo1),
                rootView.findViewById(R.id.etCodigo2),
                rootView.findViewById(R.id.etCodigo3),
                rootView.findViewById(R.id.etCodigo4),
                rootView.findViewById(R.id.etCodigo5),
                rootView.findViewById(R.id.etCodigo6)
        };

        for (int i = 0; i < edits.length; i++) {
            final int index = i;

            // Avanzar si hay un dígito
            edits[i].addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() >= 1 && index < edits.length - 1) {
                        edits[index + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            // Retroceder si está vacío
            edits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                        event.getAction() == android.view.KeyEvent.ACTION_DOWN) {

                    if (edits[index].getText().toString().isEmpty() && index > 0) {
                        edits[index - 1].requestFocus();
                        edits[index - 1].setSelection(edits[index - 1].getText().length());
                    }
                }
                return false;
            });
        }
    }

    public void limpiarEspacios(){
        etTelefonoEd.setText("");

        // Limpiar campos de código
        if (getView() != null) {
            EditText et1 = getView().findViewById(R.id.etCodigo1);
            EditText et2 = getView().findViewById(R.id.etCodigo2);
            EditText et3 = getView().findViewById(R.id.etCodigo3);
            EditText et4 = getView().findViewById(R.id.etCodigo4);
            EditText et5 = getView().findViewById(R.id.etCodigo5);
            EditText et6 = getView().findViewById(R.id.etCodigo6);

            et1.setText("");
            et2.setText("");
            et3.setText("");
            et4.setText("");
            et5.setText("");
            et6.setText("");

            // Dejar el foco en el primer campo del código
            et1.requestFocus();
        }
    }

    private void validarTelefono(String telefono, AlertDialog dialog) {
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_comprobar_telefono.php?tel_cliente=" + telefono;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody);
                    JSONObject json = new JSONObject(response);
                    boolean existe = json.getBoolean("existe");

                    if (existe) {
                        Toast.makeText(requireContext(), "El número ya está registrado", Toast.LENGTH_SHORT).show();
                    } else {
                        // Solo si el número es nuevo → mostrar layoutCodigo
                        layoutBienvenida.setVisibility(View.GONE);
                        layoutCodigo.setVisibility(View.VISIBLE);
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error procesando respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(requireContext(), "Error de conexión: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarTelefono(String telefono) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UsuarioPrefs", Context.MODE_PRIVATE);
        int idCliente = prefs.getInt("id_cliente", 1); // 👈 lo guardaste al iniciar sesión

        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_actualizar_telefono.php";

        RequestParams params = new RequestParams();
        params.put("id_cliente", idCliente);
        params.put("tel_cliente", telefono);

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody).trim();
                    if (response.contains("Teléfono actualizado correctamente")) {
                        // Guardar nuevo teléfono
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("tel_cliente", telefono);
                        editor.apply();

                        Toast.makeText(requireContext(), "Teléfono actualizado correctamente", Toast.LENGTH_SHORT).show();
                        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
                        navController.navigate(R.id.action_nav_actualizar_telefono_to_nav_perfil);

                        limpiarEspacios();
                    } else {
                        Toast.makeText(requireContext(), "Error: " + response, Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error procesando respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(requireContext(), "Error de conexión: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == btnEnviarTelefonoEd) {
            String telefono = etTelefonoEd.getText().toString().trim();

            if (telefono.isEmpty()) {
                Toast.makeText(requireContext(), "Ingrese su número de teléfono", Toast.LENGTH_SHORT).show();
                return;
            }
            if (telefono.length() < 9) {
                Toast.makeText(requireContext(), "El número debe tener al menos 9 dígitos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!telefono.startsWith("9")) {
                Toast.makeText(requireContext(), "El número debe comenzar con 9", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogoConfirmarNumero();
        }

        if (v == btnValidarCodigoEd) {
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
                return;
            }

            // Si todos están llenos, concatenar el código
            String codigo = et1.getText().toString() +
                    et2.getText().toString() +
                    et3.getText().toString() +
                    et4.getText().toString() +
                    et5.getText().toString() +
                    et6.getText().toString();
            String telefono = etTelefonoEd.getText().toString().trim();
            actualizarTelefono(telefono);
        }
    }
}