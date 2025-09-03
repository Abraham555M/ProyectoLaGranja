package com.example.proyectolagranja.ui.Servicios;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
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
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {
            // Notificación con título y mensaje
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body);
        }

        if (remoteMessage.getData().size() > 0) {
            // Si envías datos personalizados desde PHP
            String estado = remoteMessage.getData().get("estado");
            String pedido = remoteMessage.getData().get("pedido");
            showNotification("Pedido " + pedido, "Nuevo estado: " + estado);
        }
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "canal_default")
                        .setSmallIcon(R.drawable.logo_la_granja) // ícono pequeño obligatorio
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "canal_default",
                    "Notificaciones de pedidos",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}

