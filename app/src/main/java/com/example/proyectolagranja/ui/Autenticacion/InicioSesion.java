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

    // Control de intentos fallidos
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "BloqueoTelefonos";
    private static final String KEY_INTENTOS = "_intentos";
    private static final String KEY_TIEMPO_BLOQUEO = "_tiempo_bloqueo";
    private static final int MAX_INTENTOS = 3;
    private static final long TIEMPO_BLOQUEO_HORAS = 24;
    private static final String KEY_INTENTOS_CODIGO = "_intentos_codigo";
    private static final String KEY_INTENTOS_REENVIO = "_intentos_reenvio";

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

        // SharedPreferences para el control de bloqueos
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();

        // Inicializar App Check - Envio de sms
        FirebaseApp.initializeApp(requireContext());
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        return rootView;
    }

    private boolean esTelefonoBloqueado(String telefono) {
        String telefonoFormateado = formatearNumero(telefono);
        long tiempoBloqueo = sharedPreferences.getLong(telefonoFormateado + KEY_TIEMPO_BLOQUEO, 0);

        if (tiempoBloqueo == 0) {
            return false; // No hay bloqueo registrado
        }

        long tiempoActual = System.currentTimeMillis();
        long tiempoTranscurrido = tiempoActual - tiempoBloqueo;
        long tiempoBloqueoMs = TIEMPO_BLOQUEO_HORAS * 60 * 60 * 1000; // 24 horas en millisegundos

        if (tiempoTranscurrido >= tiempoBloqueoMs) {
            // El bloqueo ya expiró, limpiamos los datos
            limpiarDatosBloqueo(telefonoFormateado);
            return false;
        }

        return true; // Aún está bloqueado
    }

    private void incrementarIntentosFallidos(String telefono) {
        String telefonoFormateado = formatearNumero(telefono);
        int intentosActuales = sharedPreferences.getInt(telefonoFormateado + KEY_INTENTOS_CODIGO, 0);
        intentosActuales++;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(telefonoFormateado + KEY_INTENTOS_CODIGO, intentosActuales);

        if (intentosActuales >= MAX_INTENTOS) {
            long tiempoBloqueo = System.currentTimeMillis();
            editor.putLong(telefonoFormateado + KEY_TIEMPO_BLOQUEO, tiempoBloqueo);
            editor.apply();

            Toast.makeText(requireContext(),
                    "Número bloqueado por 24 horas debido a múltiples códigos incorrectos",
                    Toast.LENGTH_LONG).show();

            volverAlInicio();
        } else {
            editor.apply();
            int intentosRestantes = MAX_INTENTOS - intentosActuales;
            Toast.makeText(requireContext(),
                    "Código incorrecto. Te quedan " + intentosRestantes + " intentos",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarDatosBloqueo(String telefono) {
        String telefonoFormateado = formatearNumero(telefono);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(telefonoFormateado + KEY_INTENTOS);
        editor.remove(telefonoFormateado + KEY_INTENTOS_REENVIO);
        editor.remove(telefonoFormateado + KEY_TIEMPO_BLOQUEO);
        editor.apply();
    }

    private void volverAlInicio() {
        layoutCodigo.setVisibility(View.GONE);
        layoutBienvenida.setVisibility(View.VISIBLE);
        tvBienvenida.setVisibility(View.VISIBLE);
        limpiarEspacios();
    }

    private long getTiempoRestanteBloqueo(String telefono) {
        String telefonoFormateado = formatearNumero(telefono);
        long tiempoBloqueo = sharedPreferences.getLong(telefonoFormateado + KEY_TIEMPO_BLOQUEO, 0);
        long tiempoActual = System.currentTimeMillis();
        long tiempoBloqueoMs = TIEMPO_BLOQUEO_HORAS * 60 * 60 * 1000;

        return (tiempoBloqueo + tiempoBloqueoMs - tiempoActual) / (60 * 60 * 1000); // Retorna horas restantes
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

                        // Limpiar datos de bloqueo al hacer login exitoso
                        limpiarDatosBloqueo(telefono);

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
                        Log.d("PhoneAuth", "Código enviado. ID: " + verifId);
                        Toast.makeText(requireContext(), "Código enviado", Toast.LENGTH_SHORT).show();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void incrementarIntentosReenvio(String telefono) {
        String telefonoFormateado = formatearNumero(telefono);
        int intentosReenvio = sharedPreferences.getInt(telefonoFormateado + KEY_INTENTOS_REENVIO, 0);
        intentosReenvio++;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(telefonoFormateado + KEY_INTENTOS_REENVIO, intentosReenvio);

        if (intentosReenvio >= MAX_INTENTOS) {
            // Bloquear el teléfono por 24 horas
            long tiempoBloqueo = System.currentTimeMillis();
            editor.putLong(telefonoFormateado + KEY_TIEMPO_BLOQUEO, tiempoBloqueo);
            editor.apply();

            Toast.makeText(requireContext(),
                    "Número bloqueado por 24 horas debido a múltiples reenvíos",
                    Toast.LENGTH_LONG).show();

            volverAlInicio();
        } else {
            editor.apply();
            int reenviosRestantes = MAX_INTENTOS - intentosReenvio;
            Toast.makeText(requireContext(),
                    "Te quedan " + reenviosRestantes + " reenvíos disponibles",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void reenviarCodigoFirebase() {
        if (numeroTelefonoActual == null || resendToken == null) {
            Toast.makeText(requireContext(), "No se puede reenviar el código", Toast.LENGTH_SHORT).show();
            return;
        }
        incrementarIntentosReenvio(numeroTelefonoActual);
        if (esTelefonoBloqueado(numeroTelefonoActual)) {
            return; // Ya se bloqueó y mostró el toast correspondiente
        }

        Log.d("PhoneAuth", "Reenviando SMS a: " + numeroTelefonoActual);

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
                        resetearIntentosFallidos(telefono);

                        validarTelefono(telefono);
                    } else {
                        Log.e("PhoneAuth", "Autenticación fallida", task.getException());
                        String telefono = etTelefono.getText().toString().trim();
                        incrementarIntentosFallidos(telefono);
                    }
                });
    }

    private void resetearIntentosFallidos(String telefono) {
        String telefonoFormateado = formatearNumero(telefono);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Solo resetear los intentos de código, mantener los de reenvío
        editor.remove(telefonoFormateado + KEY_INTENTOS_CODIGO);
        editor.apply();

        Log.d("BloqueoControl", "Intentos de código reseteados para: " + telefonoFormateado);
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

            // Verificar si el teléfono está bloqueado
            if (esTelefonoBloqueado(telefono)) {
                long horasRestantes = getTiempoRestanteBloqueo(telefono);
                Toast.makeText(requireContext(),
                        "Este número está bloqueado. Tiempo restante: " + horasRestantes + " horas",
                        Toast.LENGTH_LONG).show();
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