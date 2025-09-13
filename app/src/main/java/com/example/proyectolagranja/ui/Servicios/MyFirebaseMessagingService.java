package com.example.proyectolagranja.ui.Servicios;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.proyectolagranja.MainActivity;
import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "canal_default";
    private static final String TAG = "FCM_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "FirebaseMessagingService creado");
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "=== MENSAJE FCM RECIBIDO ===");
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message ID: " + remoteMessage.getMessageId());
        Log.d(TAG, "Data size: " + remoteMessage.getData().size());

        try {
            String title = null;
            String body = null;

            // Caso 1: viene en "notification"
            if (remoteMessage.getNotification() != null) {
                title = remoteMessage.getNotification().getTitle();
                body = remoteMessage.getNotification().getBody();
                Log.d(TAG, "Notification payload - Title: " + title + ", Body: " + body);
            }

            // Caso 2: viene en "data"
            if (remoteMessage.getData().size() > 0) {
                Log.d(TAG, "Data payload: " + remoteMessage.getData().toString());

                if (title == null) title = remoteMessage.getData().get("title");
                if (body == null) body = remoteMessage.getData().get("body");

                String idVenta = remoteMessage.getData().get("id_venta");
                String estado = remoteMessage.getData().get("estado");
                String pedido = remoteMessage.getData().get("pedido");

                Log.d(TAG, "Data - Venta: " + idVenta + ", Estado: " + estado + ", Pedido: " + pedido);
            }

            if (title != null && body != null) {
                Log.d(TAG, "Mostrando notificación: " + title);
                showNotification(title, body);
            } else {
                Log.w(TAG, "Título o cuerpo nulos - Title: " + title + ", Body: " + body);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error procesando mensaje FCM: " + e.getMessage(), e);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "=== NUEVO TOKEN FCM ===");
        Log.d(TAG, "Token: " + token);

        SessionManager sessionManager = new SessionManager(getApplicationContext());
        if (sessionManager.isLoggedIn()) {
            int idCliente = sessionManager.getIdCliente();
            Log.d(TAG, "Enviando token al servidor para cliente: " + idCliente);
            enviarTokenAlServidor(idCliente, token);
        } else {
            Log.w(TAG, "Usuario no logueado, no se envía token al servidor");
        }
    }

    public static void enviarTokenAlServidor(int idCliente, String token) {
        String url = ServidorConfig.URL_SERVIDOR + "cliente/cliente_actualizar_token.php";
        Log.d("FCM_TOKEN", "Enviando token al servidor: " + url);

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("id_cliente", idCliente);
        params.put("tok_fcm_cliente", token);

        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = responseBody != null ? new String(responseBody) : "null";
                Log.d("FCM_TOKEN", "Token actualizado exitosamente - Status: " + statusCode + ", Response: " + response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String response = responseBody != null ? new String(responseBody) : "null";
                Log.e("FCM_TOKEN", "Error al actualizar token - Status: " + statusCode + ", Response: " + response + ", Error: " + error.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificaciones de pedidos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para notificaciones de estado de pedidos");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Canal de notificación creado: " + CHANNEL_ID);
            }
        }
    }

    private void showNotification(String title, String message) {
        Log.d(TAG, "=== CREANDO NOTIFICACIÓN ===");
        Log.d(TAG, "Title: " + title + ", Message: " + message);

        // Verificar permisos para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Sin permisos de notificación para Android 13+");
                return;
            }
        }

        // Crear intent para abrir la app
        Intent intent = new Intent(this, MainActivity.class); // Cambia por tu MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_ONE_SHOT;

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                pendingIntentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_la_granja)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        try {
            int notificationId = (int) System.currentTimeMillis();
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                manager.notify(notificationId, builder.build());
                Log.d(TAG, "Notificación mostrada con ID: " + notificationId);
            } else {
                Log.e(TAG, "Sin permisos para mostrar notificaciones");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar notificación: " + e.getMessage(), e);
        }
    }
}