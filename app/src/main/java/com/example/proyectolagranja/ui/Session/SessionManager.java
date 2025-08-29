package com.example.proyectolagranja.ui.Session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private static final String PREF_NAME = "DatosUsuario";
    private static final String KEY_ID = "id_cliente";
    private static final String KEY_NOMBRE = "nom_cliente";
    private static final String KEY_TELEFONO = "tel_cliente";
    private static final String KEY_IS_LOGGED = "is_logged_in";

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        Log.d("SESSION", "SessionManager inicializado con PREF_NAME=" + PREF_NAME);
    }

    // Guardar datos de usuario
    public void createLoginSession(int idCliente, String nombre, String telefono) {
        editor.putBoolean(KEY_IS_LOGGED, true);
        editor.putInt(KEY_ID, idCliente);
        editor.putString(KEY_NOMBRE, nombre);
        editor.putString(KEY_TELEFONO, telefono);
        editor.apply();

        Log.d("SESSION", "Sesión creada: id=" + idCliente + " nombre=" + nombre + " tel=" + telefono);
    }

    // Obtener id cliente
    public int getIdCliente() {
        int id = pref.getInt(KEY_ID, -1);
        Log.d("SESSION", "getIdCliente() -> " + id);
        return id;
    }

    // Obtener nombre
    public String getNombre() {
        return pref.getString(KEY_NOMBRE, null);
    }

    // Obtener teléfono
    public String getTelefono() {
        return pref.getString(KEY_TELEFONO, null);
    }

    // Verificar si está logueado
    public boolean isLoggedIn() {
        boolean logged = pref.getBoolean(KEY_IS_LOGGED, false);
        Log.d("SESSION", "isLoggedIn() -> " + logged);
        return logged;
    }


    // Cerrar sesión
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
