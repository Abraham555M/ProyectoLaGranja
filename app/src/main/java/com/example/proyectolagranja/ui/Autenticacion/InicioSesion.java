package com.example.proyectolagranja.ui.Autenticacion;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class InicioSesion extends Fragment implements View.OnClickListener {
    private LinearLayout layoutBienvenida, layoutCodigo;
    private EditText etTelefono;
    private Button btnEnviarTelefono, btnValidarCodigo;
    private TextView etEnlaceReenviar, tvBienvenida;

    // Firebase
    private String verificationId; // Guardar el ID de verificación
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inicio_sesion, container, false);

        layoutBienvenida = rootView.findViewById(R.id.layout_bienvenida);
        layoutCodigo = rootView.findViewById(R.id.layout_codigo);
        btnEnviarTelefono = rootView.findViewById(R.id.btnEnviarTelefono);
        btnValidarCodigo = rootView.findViewById(R.id.btnValidarCodigo);
        etTelefono = rootView.findViewById(R.id.etTelefono);
        tvBienvenida = rootView.findViewById(R.id.tvBienvenida);

        btnValidarCodigo.setOnClickListener(this);
        btnEnviarTelefono.setOnClickListener(this);

        configurarAutoFocusCodigo(rootView);

        mAuth = FirebaseAuth.getInstance();

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
            layoutBienvenida.setVisibility(View.GONE);
            layoutCodigo.setVisibility(View.VISIBLE);
            tvBienvenida.setVisibility(View.GONE);
            enviarCodigoFirebase(etTelefono.getText().toString().trim()); // <- Aquí enviamos el código

            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void validarTelefono(String telefono) {
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_comprobar_telefono.php?tel_cliente=" + telefono;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody);
                    JSONObject json = new JSONObject(response);
                    boolean existe = json.getBoolean("existe");

                    NavController navController = Navigation.findNavController(
                            requireActivity(),
                            R.id.nav_host_fragment_content_main
                    );

                    if (existe) {
                        // Si existe → ir al catálogo
                        limpiarEspacios();
                        navController.navigate(R.id.action_nav_inicio_sesion_to_nav_catalogo);
                    } else {
                        // Si no existe → ir al registro y pasar teléfono
                        Bundle bundle = new Bundle();
                        bundle.putString("telefono", telefono);
                        limpiarEspacios();
                        navController.navigate(R.id.action_nav_inicio_sesion_to_nav_crear_cuenta, bundle);
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

    private void enviarCodigoFirebase(String telefono) {
        if (telefono.equals("325475745")) { // Número de prueba 987654321
            verificationId = "VERIFICATION_ID_TEST"; // Cualquier string único
            Toast.makeText(requireContext(), "Código de prueba listo: 123456", Toast.LENGTH_SHORT).show();
            return;
        }

        // Código real para envío de SMS (solo si fuera producción y región habilitada)
        com.google.firebase.auth.PhoneAuthOptions options =
                com.google.firebase.auth.PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+51" + telefono)
                        .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(new com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull com.google.firebase.auth.PhoneAuthCredential credential) {
                                signInWithPhoneAuthCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException e) {
                                Toast.makeText(requireContext(), "Error verificación: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verifId, @NonNull com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken token) {
                                super.onCodeSent(verifId, token);
                                verificationId = verifId;
                                Toast.makeText(requireContext(), "Código enviado", Toast.LENGTH_SHORT).show();
                            }
                        }).build();

        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options);
    }


    private void verificarCodigoFirebase(String codigoIngresado) {
        // Validación para número de prueba
        String telefono = etTelefono.getText().toString().trim();
        if (telefono.equals("325475745") && codigoIngresado.equals("123456")) {
            Toast.makeText(requireContext(), "Autenticación exitosa (prueba)", Toast.LENGTH_SHORT).show();
            validarTelefono(telefono);
            return;
        }

        if (verificationId == null) {
            Toast.makeText(requireContext(), "No se ha enviado el código", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.auth.PhoneAuthCredential credential =
                com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, codigoIngresado);

        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(com.google.firebase.auth.PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Usuario autenticado correctamente
                        String telefono = etTelefono.getText().toString().trim();
                        validarTelefono(telefono); // Tu función existente
                    } else {
                        Toast.makeText(requireContext(), "Código incorrecto", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void limpiarEspacios(){
        etTelefono.setText("");

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

    @Override
    public void onClick(View v) {
        if (v == btnEnviarTelefono) {
            String telefono = etTelefono.getText().toString().trim();
            /*
            if (telefono.isEmpty() || telefono.length() < 9 || !telefono.startsWith("9")) {
                Toast.makeText(requireContext(), "Número inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            */
            mostrarDialogoConfirmarNumero();
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
                return;
            }

            // Si todos están llenos, concatenar el código
            String codigo = et1.getText().toString() +
                    et2.getText().toString() +
                    et3.getText().toString() +
                    et4.getText().toString() +
                    et5.getText().toString() +
                    et6.getText().toString();

            verificarCodigoFirebase(codigo);
        }
    }
}