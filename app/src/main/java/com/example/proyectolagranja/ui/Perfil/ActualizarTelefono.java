package com.example.proyectolagranja.ui.Perfil;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
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
import com.example.proyectolagranja.ui.Servicios.SessionManager;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class ActualizarTelefono extends Fragment implements View.OnClickListener {
    private LinearLayout layoutBienvenida, layoutCodigo;
    private EditText etTelefonoEd;
    private Button btnEnviarTelefonoEd, btnValidarCodigoEd;
    private TextView tvReenviarCodigo;
    private SessionManager session;

    private String verificationId;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String numeroTelefonoActual;

    // Control de intentos
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "BloqueoTelefonos";
    private static final String KEY_INTENTOS_CODIGO = "_intentos_codigo";
    private static final String KEY_INTENTOS_REENVIO = "_intentos_reenvio";
    private static final String KEY_TIEMPO_BLOQUEO = "_tiempo_bloqueo";
    private static final int MAX_INTENTOS = 3;
    private static final long TIEMPO_BLOQUEO_HORAS = 24;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_actualizar_telefono, container, false);

        session = new SessionManager(requireContext());
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        layoutBienvenida = rootView.findViewById(R.id.layout_bienvenida);
        layoutCodigo = rootView.findViewById(R.id.layout_codigo);
        etTelefonoEd = rootView.findViewById(R.id.etTelefonoEd);
        btnEnviarTelefonoEd = rootView.findViewById(R.id.btnEnviarTelefonoEd);
        btnValidarCodigoEd = rootView.findViewById(R.id.btnValidarCodigoEd);
        tvReenviarCodigo = rootView.findViewById(R.id.tvEnlaceReenviar);

        btnEnviarTelefonoEd.setOnClickListener(this);
        btnValidarCodigoEd.setOnClickListener(this);
        tvReenviarCodigo.setOnClickListener(this);

        configurarAutoFocusCodigo(rootView);
        mAuth = FirebaseAuth.getInstance();

        return rootView;
    }

    /** ==================== DIALOGO DE CONFIRMACION ==================== **/
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
            validarTelefono(telefono, dialog); // validamos en backend
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    /** ==================== VALIDAR EN BACKEND ==================== **/
    private void validarTelefono(String telefono, AlertDialog dialog) {
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_validar_telefono.php?tel_cliente=" + telefono;

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
                        dialog.dismiss();
                    } else {
                        // OK: ocultar bienvenida, mostrar código y enviar SMS
                        layoutBienvenida.setVisibility(View.GONE);
                        layoutCodigo.setVisibility(View.VISIBLE);

                        enviarCodigoFirebase(formatearNumero(telefono));
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

    private String formatearNumero(String telefono) {
        if (!telefono.startsWith("+51")) {
            telefono = "+51" + telefono;
        }
        return telefono;
    }

    /** ==================== ENVIAR Y REENVIAR CODIGO ==================== **/
    private void enviarCodigoFirebase(String telefono) {
        numeroTelefonoActual = telefono;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(telefono)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verifId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(verifId, token);
                        verificationId = verifId;
                        resendToken = token;
                        Toast.makeText(requireContext(), "Código enviado", Toast.LENGTH_SHORT).show();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void reenviarCodigoFirebase() {
        if (numeroTelefonoActual == null || resendToken == null) {
            Toast.makeText(requireContext(), "No se puede reenviar el código", Toast.LENGTH_SHORT).show();
            return;
        }
        incrementarIntentosReenvio(numeroTelefonoActual);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(numeroTelefonoActual)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setForceResendingToken(resendToken)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(requireContext(), "Error al reenviar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verifId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(verifId, token);
                        verificationId = verifId;
                        resendToken = token;
                        Toast.makeText(requireContext(), "Código reenviado", Toast.LENGTH_SHORT).show();
                        limpiarCamposCodigo();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /** ==================== VERIFICAR CODIGO ==================== **/
    private void verificarCodigoFirebase(String codigoIngresado) {
        if (verificationId == null) {
            Toast.makeText(requireContext(), "No se ha enviado el código", Toast.LENGTH_SHORT).show();
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, codigoIngresado);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        String telefono = etTelefonoEd.getText().toString().trim();
                        actualizarTelefono(telefono);
                    } else {
                        incrementarIntentosCodigo(numeroTelefonoActual);
                        Toast.makeText(requireContext(), "Código incorrecto", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** ==================== CONTROL DE INTENTOS ==================== **/
    private void incrementarIntentosCodigo(String telefono) {
        String key = telefono + KEY_INTENTOS_CODIGO;
        int intentos = sharedPreferences.getInt(key, 0) + 1;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, intentos).apply();

        if (intentos >= MAX_INTENTOS) {
            editor.putLong(telefono + KEY_TIEMPO_BLOQUEO, System.currentTimeMillis()).apply();
            Toast.makeText(requireContext(), "Número bloqueado por 24h por códigos erróneos", Toast.LENGTH_LONG).show();
        }
    }

    private void incrementarIntentosReenvio(String telefono) {
        String key = telefono + KEY_INTENTOS_REENVIO;
        int intentos = sharedPreferences.getInt(key, 0) + 1;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, intentos).apply();

        if (intentos >= MAX_INTENTOS) {
            editor.putLong(telefono + KEY_TIEMPO_BLOQUEO, System.currentTimeMillis()).apply();
            Toast.makeText(requireContext(), "Número bloqueado por 24h por reenvíos", Toast.LENGTH_LONG).show();
        }
    }

    /** ==================== ACTUALIZAR EN SERVIDOR ==================== **/
    private void actualizarTelefono(String telefono) {
        int idCliente = session.getIdCliente();
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_actualizar_telefono.php";

        RequestParams params = new RequestParams();
        params.put("id_cliente", idCliente);
        params.put("tel_cliente", telefono);

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody).trim();
                if (response.contains("Teléfono actualizado correctamente")) {
                    session.updateTelefono(telefono);
                    Toast.makeText(requireContext(), "Teléfono actualizado correctamente", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                            .navigate(R.id.action_nav_actualizar_telefono_to_nav_perfil);
                    limpiarCamposCodigo();
                } else {
                    Toast.makeText(requireContext(), "Error: " + response, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(requireContext(), "Error de conexión: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** ==================== LIMPIAR CAMPOS ==================== **/
    private void limpiarCamposCodigo() {
        if (getView() != null) {
            int[] ids = {R.id.etCodigo1,R.id.etCodigo2,R.id.etCodigo3,R.id.etCodigo4,R.id.etCodigo5,R.id.etCodigo6};
            for (int id : ids) {
                EditText et = getView().findViewById(id);
                et.setText("");
            }
        }
    }

    private void configurarAutoFocusCodigo(View rootView) {
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
            edits[i].addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() >= 1 && index < edits.length - 1) {
                        edits[index + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

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

    /** ==================== ONCLICK ==================== **/
    @Override
    public void onClick(View v) {
        if (v == btnEnviarTelefonoEd) {
            String telefono = etTelefonoEd.getText().toString().trim();
            if (telefono.isEmpty() || telefono.length() < 9 || !telefono.startsWith("9")) {
                Toast.makeText(requireContext(), "Número inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogoConfirmarNumero(); // 👈 aquí ahora sí usamos el flujo completo
        }

        if (v == btnValidarCodigoEd) {
            StringBuilder codigo = new StringBuilder();
            int[] ids = {R.id.etCodigo1,R.id.etCodigo2,R.id.etCodigo3,R.id.etCodigo4,R.id.etCodigo5,R.id.etCodigo6};
            for (int id : ids) {
                EditText et = getView().findViewById(id);
                if (et.getText().toString().isEmpty()) {
                    Toast.makeText(requireContext(), "Completa todos los dígitos", Toast.LENGTH_SHORT).show();
                    return;
                }
                codigo.append(et.getText().toString());
            }
            verificarCodigoFirebase(codigo.toString());
        }

        if (v == tvReenviarCodigo) {
            reenviarCodigoFirebase();
        }
    }
}

