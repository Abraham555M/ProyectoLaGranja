package com.example.proyectolagranja.ui.Autenticacion;

import static com.example.proyectolagranja.ui.Servicios.MyFirebaseMessagingService.enviarTokenAlServidor;

import android.app.AlertDialog;
import android.app.Dialog;
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
    private Dialog loadingDialog;

    // Firebase
    private String verificationId;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String numeroTelefonoActual;
    private boolean numeroBloqueado = false; // Flag global

    private static final int MAX_INTENTOS = 3;

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
        tvEnlaceReenviar.setOnClickListener(this);

        configurarAutoFocusCodigo(rootView);

        mAuth = FirebaseAuth.getInstance();

        // Inicializar App Check - Envio de sms
        /*
        FirebaseApp.initializeApp(requireContext());
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
        */
        return rootView;
    }

    private void volverAlInicio() {
        layoutCodigo.setVisibility(View.GONE);
        layoutBienvenida.setVisibility(View.VISIBLE);
        tvBienvenida.setVisibility(View.VISIBLE);
        limpiarEspacios();
    }

    private void verificarEstadoBloqueo(String telefono) { // esTelefonoBloqueado
        String telefonoLimpio = telefono; // el que viene del EditText, 9 dígitos
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_estado_bloqueo.php?tel_cliente=" + telefonoLimpio;

        Log.d("DEBUG_BLOQUEO", "Llamando a: " + url);

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String resp = new String(responseBody);
                    Log.d("DEBUG_BLOQUEO", "Respuesta JSON: " + resp);

                    JSONObject json = new JSONObject(resp);
                    boolean bloqueado = json.getBoolean("bloqueado");
                    numeroBloqueado = bloqueado;
                    int horasRestantes = json.getInt("horas_restantes");
                    Log.d("DEBUG_BLOQUEO", "¿Bloqueado? " + bloqueado + " | Horas restantes: " + horasRestantes);

                    if (numeroBloqueado) {
                        // El servidor nos confirma que el cliente sigue bloqueado
                        volverAlInicio();  // más limpio que repetir setVisibility

                        Toast.makeText(requireContext(),
                                "Este número está bloqueado. Tiempo restante: " + horasRestantes + " horas",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // El servidor nos dice que está desbloqueado (o nunca lo estuvo)
                        mostrarDialogoConfirmarNumero();
                    }
                } catch (Exception e) {
                    Log.e("BLOQUEO_CHECK", "Error al procesar estado: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error de sistema al verificar bloqueo.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP_ERROR", "Fallo al verificar bloqueo: " + statusCode, error);
                Toast.makeText(requireContext(), "Error de conexión con el servidor.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //-------------------------
    // Nueva función para registrar un intento fallido en el servidor
    private void registrarFallo(String telefono, String tipoBloqueo) {
        String telefonoFormateado = formatearNumero(telefono);
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_registrar_fallo.php";

        AsyncHttpClient client = new AsyncHttpClient();
        cz.msebera.android.httpclient.entity.StringEntity entity = null;
        try {
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("tel_cliente", telefonoFormateado);
            jsonParams.put("tipo_bloqueo", tipoBloqueo);
            entity = new cz.msebera.android.httpclient.entity.StringEntity(jsonParams.toString());
            entity.setContentType("application/json");
        } catch (Exception e) {
            Log.e("REGISTRO_FALLO", "Error creando JSON: " + e.getMessage());
            return;
        }

        client.post(requireContext(), url, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject json = new JSONObject(new String(responseBody));
                    String estado = json.getString("estado"); // Puede ser "BLOQUEADO" o "NORMAL"
                    int intentosActuales = json.getInt("intentos_actuales");

                    // 🚨 AÑADE ESTOS LOGS CRUCIALES 🚨
                    Log.d("FALLO_DEBUG", "JSON recibido: " + new String(responseBody));
                    Log.d("FALLO_DEBUG", "Intentos actuales (Server): " + intentosActuales);
                    // ------------------------------------

                    if ("BLOQUEADO".equals(estado)) {
                        // El servidor nos indica que el cliente ha sido bloqueado
                        Toast.makeText(requireContext(),
                                "Número bloqueado por 24 horas debido a múltiples fallos.",
                                Toast.LENGTH_LONG).show();
                        volverAlInicio();
                    } else {
                        // El servidor nos indica que es solo un intento fallido, no el límite
                        int intentosRestantes = MAX_INTENTOS - intentosActuales;
                        String mensaje = "Código incorrecto. Te quedan " + intentosRestantes + " intentos";
                        if (tipoBloqueo.equals("REENVIO_EXCESIVO")) {
                            mensaje = "Te quedan " + intentosRestantes + " reenvíos disponibles";
                        }

                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("BLOQUEO_FALLO", "Error al procesar registro: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("HTTP_ERROR", "Fallo al registrar intento: " + statusCode, error);
                Toast.makeText(requireContext(), "Error de conexión al registrar el fallo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Nuevo método para limpiar intentos en el servidor tras éxito
    private void limpiarIntentosEnServidor(String telefono) {
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_limpiar_intentos.php?tel_cliente=" + telefono;
        Log.d("DEBUG_LIMPIAR", "URL enviada: " + url);

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String response = new String(responseBody, "UTF-8");
                    JSONObject json = new JSONObject(response);

                    boolean exito = json.optBoolean("exito", false);
                    int eliminados = json.optInt("intentos_eliminados", 0);

                    if (exito) {
                        Log.d("BLOQUEO_CONTROL", "Intentos limpiados. Registros eliminados: " + eliminados);
                    } else {
                        Log.w("BLOQUEO_CONTROL", "No se limpiaron intentos. Respuesta: " + response);
                    }

                } catch (Exception e) {
                    Log.e("JSON_ERROR", "Error parseando respuesta limpiar_intentos", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("BLOQUEO_CONTROL", "Error en la petición: " + statusCode, error);
                if (responseBody != null) {
                    try {
                        String response = new String(responseBody, "UTF-8");
                        Log.e("BLOQUEO_CONTROL", "Respuesta error: " + response);
                    } catch (Exception e) {
                        Log.e("BLOQUEO_CONTROL", "Error parseando respuesta de error", e);
                    }
                }
            }
        });
    }


    //-------------------------


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
            Log.d("DEBUG_DIALOGO", "Click en SI | numeroBloqueado=" + numeroBloqueado);

            if (numeroBloqueado) {
                // 🚫 Evitar que pase a layoutCodigo
                Toast.makeText(requireContext(), "Tu número está bloqueado", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            String telefonoFormateado = formatearNumero(etTelefono.getText().toString().trim());
            Log.d("DEBUG_DIALOGO", "Llamando a enviarCodigoFirebase con: " + telefonoFormateado);

            enviarCodigoFirebase(telefonoFormateado); // 🚀 Solo dispara Firebase
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private String formatearNumero(String telefono) {
        if (!telefono.startsWith("+51")) {
            telefono = "+51" + telefono;
        }
        return telefono;
    }

    private void validarTelefono(String telefono) {
        String telefonoLimpio = telefono.replace("+51", ""); // por si acaso
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_comprobar_telefono.php?tel_cliente=" + telefonoLimpio;

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

                        SessionManager session = new SessionManager(requireContext());
                        session.createLoginSession(idCliente, nombre, telCliente, false);

                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.w("FCM_TOKEN", "Error al obtener token", task.getException());
                                        return;
                                    }

                                    String token = task.getResult();
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

        mostrarLoading();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(telefono)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        ocultarLoading();
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        ocultarLoading();
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
                        ocultarLoading();
                        verificationId = verifId;
                        resendToken = token;
                        Log.d("DEBUG_FIREBASE", "Código enviado OK. ID: " + verifId);
                        Toast.makeText(requireContext(), "Código enviado", Toast.LENGTH_SHORT).show();

                        layoutBienvenida.setVisibility(View.GONE);
                        layoutCodigo.setVisibility(View.VISIBLE);
                        tvBienvenida.setVisibility(View.GONE);
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }



    private void reenviarCodigoFirebase() {
        if (numeroTelefonoActual == null || resendToken == null) {
            Toast.makeText(requireContext(), "No se puede reenviar el código", Toast.LENGTH_SHORT).show();
            return;
        }
        registrarFallo(numeroTelefonoActual, "REENVIO_EXCESIVO");

        Log.d("PhoneAuth", "Reenviando SMS a: " + numeroTelefonoActual);
        mostrarLoadingConMensaje("Reenviando código...");

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(numeroTelefonoActual)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setForceResendingToken(resendToken)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        ocultarLoading();
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        ocultarLoading();
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
                        ocultarLoading();

                        verificationId = verifId;
                        resendToken = token;
                        Log.d("PhoneAuth", "Código reenviado. ID: " + verifId);
                        Toast.makeText(requireContext(), "Código reenviado", Toast.LENGTH_SHORT).show();
                        limpiarCamposCodigo();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void mostrarLoadingConMensaje(String mensaje) {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(requireContext());
            loadingDialog.setContentView(R.layout.dialog_loading);
            loadingDialog.setCancelable(false);
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        TextView textView = loadingDialog.findViewById(R.id.textMensaje);
        if (textView != null) {
            textView.setText(mensaje);
        }
        loadingDialog.show();
    }
    private void mostrarLoading() {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(requireContext());
            loadingDialog.setContentView(R.layout.dialog_loading);
            loadingDialog.setCancelable(false);
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        loadingDialog.show();
    }

    private void ocultarLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
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
            Toast.makeText(requireContext(), "Código ingresado inválido", Toast.LENGTH_SHORT).show(); // No se ha enviado el código
            return;
        }

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, codigoIngresado);

        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mostrarLoadingConMensaje("Verificando código...");

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    ocultarLoading();

                    if (task.isSuccessful()) {
                        Log.d("PhoneAuth", "Autenticación exitosa");
                        String telefono = etTelefono.getText().toString().trim();
                        limpiarIntentosEnServidor(telefono);

                        validarTelefono(telefono);
                    } else {
                        Log.e("PhoneAuth", "Autenticación fallida", task.getException());
                        String telefono = etTelefono.getText().toString().trim();
                        registrarFallo(telefono, "CODIGO_ERRONEO");
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
            if (et1 != null) et1.requestFocus();
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

            // CORRECTO: Solo se llama a la verificación asíncrona.
            // El resultado de la verificación llamará a mostrarDialogoConfirmarNumero() si no hay bloqueo.
            verificarEstadoBloqueo(telefono);
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