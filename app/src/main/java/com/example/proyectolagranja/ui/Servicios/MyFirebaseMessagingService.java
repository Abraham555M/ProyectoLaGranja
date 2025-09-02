package com.example.proyectolagranja.ui.Servicios;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.proyectolagranja.R;
import com.example.proyectolagranja.ui.Servidor.ServidorConfig;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // Crear canal si no existe (Android 8+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "ventas_channel",
                        "Notificaciones de Ventas",
                        NotificationManager.IMPORTANCE_HIGH
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ventas_channel")
                    .setSmallIcon(R.drawable.logo_la_granja)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat manager = NotificationManagerCompat.from(this);

            // Usar un ID único para que no se reemplacen
            int notificationId = (int) System.currentTimeMillis();
            manager.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.d("FCM_TOKEN", "Nuevo token: " + token);

        // ✅ Aquí deberías enviar el nuevo token al backend
        // Solo si el usuario ya tiene sesión activa
        SessionManager session = new SessionManager(getApplicationContext());
        if (session.isLoggedIn()) {
            int userId = session.getIdCliente();

            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();
            params.put("id_cliente", userId);
            params.put("tok_fcm_cliente", token);

            String url = ServidorConfig.URL_SERVIDOR + "cliente/actualizar_token.php";
            client.post(url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d("FCM_TOKEN", "Token actualizado en backend");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e("FCM_TOKEN", "Error al actualizar token en backend", error);
                }
            });
        }
    }
}
