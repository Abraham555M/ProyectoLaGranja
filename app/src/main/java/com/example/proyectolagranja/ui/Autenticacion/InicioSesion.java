package com.example.proyectolagranja.ui.Autenticacion;

import static com.example.proyectolagranja.ui.Servicios.MyFirebaseMessagingService.enviarTokenAlServidor;

import android.app.AlertDialog;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import com.google.firebase.messaging.FirebaseMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class InicioSesion extends Fragment implements View.OnClickListener {
    private LinearLayout layoutBienvenida, layoutCodigo;
    private EditText etTelefono;
    private Button btnEnviarTelefono, btnValidarCodigo;
    private TextView tvBienvenida, tvEnlaceReenviar;
    // Firebase
    private String verificationId;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String numeroTelefonoActual;

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
        tvEnlaceReenviar = rootView.findViewById(R.id.tvEnlaceReenviar);

        btnEnviarTelefono.setOnClickListener(this);
        btnValidarCodigo.setOnClickListener(this);

        configurarAutoFocusCodigo(rootView);

        mAuth = FirebaseAuth.getInstance();
        // mAuth.getFirebaseAuthSettings()
        //   .forceRecaptchaFlowForTesting(true);

        // **********************************************
        // Agrega esta sección para inicializar App Check
        // **********************************************
        FirebaseApp.initializeApp(requireContext());
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
        // **********************************************

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
            String telefonoFormateado = formatearNumero(etTelefono.getText().toString().trim());
            enviarCodigoFirebase(telefonoFormateado);

            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private String formatearNumero(String telefono) {
        // Si el usuario ingresa: 987654321
        // Convertir a: +51987654321
        if (!telefono.startsWith("+51")) {
            telefono = "+51" + telefono;
        }
        return telefono;
    }

    private void validarTelefono(String telefono) {
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_comprobar_telefono.php?tel_cliente=" + telefono;

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody);
                    Log.d("RESPUESTA_BACKEND", "Response: " + response);

                    JSONObject json = new JSONObject(response);
                    boolean existe = json.getBoolean("existe");

                    NavController navController = Navigation.findNavController(
                            requireActivity(),
                            R.id.nav_host_fragment_content_main
                    );

                    if (existe) {
                        String idClienteStr = json.getString("id_cliente");
                        int idCliente = Integer.parseInt(idClienteStr);

                        String nombre = json.getString("nom_cliente");
                        String telCliente = json.getString("tel_cliente");

                        // Guardamos sesion con SessionManager
                        SessionManager session = new SessionManager(requireContext());
                        session.createLoginSession(idCliente, nombre, telCliente);

                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.w("FCM_TOKEN", "Error al obtener token", task.getException());
                                        return;
                                    }

                                    // Token actual
                                    String token = task.getResult();

                                    // Llamamos al método que envía token a tu backend
                                    enviarTokenAlServidor(idCliente, token);
                                });


                        limpiarEspacios();
                        navController.navigate(R.id.action_nav_inicio_sesion_to_nav_catalogo);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString("telefono", telefono);
                        limpiarEspacios();
                        navController.navigate(R.id.action_nav_inicio_sesion_to_nav_crear_cuenta, bundle);
                    }
                } catch (Exception e) {
                    Log.e("JSON_ERROR", "Error procesando respuesta", e);
                    Toast.makeText(requireContext(), "Error procesando respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP_ERROR", "Código: " + statusCode, error);
                Toast.makeText(requireContext(), "Error de conexión: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
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

    private void enviarCodigoFirebase(String telefono) {
        Log.d("PhoneAuth", "Enviando SMS a: " + telefono);
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
                        // Manejar diferentes tipos de errores
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(requireContext(), "Número de teléfono inválido", Toast.LENGTH_LONG).show();
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            Toast.makeText(requireContext(), "Demasiados intentos. Intenta más tarde", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verifId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(verifId, token);
                        verificationId = verifId;
                        resendToken = token;
                        Log.d("PhoneAuth", "Código enviado. ID: " + verifId);
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

        Log.d("PhoneAuth", "Reenviando SMS a: " + numeroTelefonoActual);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(numeroTelefonoActual)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setForceResendingToken(resendToken) // ✅ USAR EL TOKEN DE REENVÍO
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(requireContext(), "Número de teléfono inválido", Toast.LENGTH_LONG).show();
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            Toast.makeText(requireContext(), "Demasiados intentos. Intenta más tarde", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "Error al reenviar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verifId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(verifId, token);
                        verificationId = verifId;
                        resendToken = token; // Actualizar el token
                        Log.d("PhoneAuth", "Código reenviado. ID: " + verifId);
                        Toast.makeText(requireContext(), "Código reenviado", Toast.LENGTH_SHORT).show();

                        // Opcional: Limpiar los campos de código
                        limpiarCamposCodigo();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void limpiarCamposCodigo() {
        if (getView() != null) {
            EditText et1 = getView().findViewById(R.id.etCodigo1);
            EditText et2 = getView().findViewById(R.id.etCodigo2);
            EditText et3 = getView().findViewById(R.id.etCodigo3);
            EditText et4 = getView().findViewById(R.id.etCodigo4);
            EditText et5 = getView().findViewById(R.id.etCodigo5);
            EditText et6 = getView().findViewById(R.id.etCodigo6);

            et1.setText(""); et2.setText(""); et3.setText("");
            et4.setText(""); et5.setText(""); et6.setText("");
            et1.requestFocus();
        }
    }


    private void verificarCodigoFirebase(String codigoIngresado) {
        if (verificationId == null) {
            Toast.makeText(requireContext(), "No se ha enviado el código", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, codigoIngresado);

        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d("PhoneAuth", "Autenticación exitosa");
                        String telefono = etTelefono.getText().toString().trim();

                        validarTelefono(telefono);
                    } else {
                        Log.e("PhoneAuth", "Autenticación fallida", task.getException());
                        Toast.makeText(requireContext(), "Código incorrecto", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void limpiarEspacios() {
        etTelefono.setText("");

        if (getView() != null) {
            EditText et1 = getView().findViewById(R.id.etCodigo1);
            EditText et2 = getView().findViewById(R.id.etCodigo2);
            EditText et3 = getView().findViewById(R.id.etCodigo3);
            EditText et4 = getView().findViewById(R.id.etCodigo4);
            EditText et5 = getView().findViewById(R.id.etCodigo5);
            EditText et6 = getView().findViewById(R.id.etCodigo6);

            et1.setText(""); et2.setText(""); et3.setText("");
            et4.setText(""); et5.setText(""); et6.setText("");
            et1.requestFocus();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnEnviarTelefono) {
            String telefono = etTelefono.getText().toString().trim();
            if (telefono.isEmpty() || telefono.length() < 9) {
                Toast.makeText(requireContext(), "Número inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogoConfirmarNumero();
        }

        if (v == btnValidarCodigo) {
            EditText et1 = getView().findViewById(R.id.etCodigo1);
            EditText et2 = getView().findViewById(R.id.etCodigo2);
            EditText et3 = getView().findViewById(R.id.etCodigo3);
            EditText et4 = getView().findViewById(R.id.etCodigo4);
            EditText et5 = getView().findViewById(R.id.etCodigo5);
            EditText et6 = getView().findViewById(R.id.etCodigo6);

            if (et1.getText().toString().trim().isEmpty() ||
                    et2.getText().toString().trim().isEmpty() ||
                    et3.getText().toString().trim().isEmpty() ||
                    et4.getText().toString().trim().isEmpty() ||
                    et5.getText().toString().trim().isEmpty() ||
                    et6.getText().toString().trim().isEmpty()) {

                Toast.makeText(requireContext(), "Por favor, complete todos los dígitos del código", Toast.LENGTH_SHORT).show();
                return;
            }

            String codigo = et1.getText().toString() +
                    et2.getText().toString() +
                    et3.getText().toString() +
                    et4.getText().toString() +
                    et5.getText().toString() +
                    et6.getText().toString();

            verificarCodigoFirebase(codigo);
        }

        if (v == tvEnlaceReenviar) {
            reenviarCodigoFirebase();
        }
    }
}