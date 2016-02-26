package upc.edu.pe.semana05_clase01;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.internal.js;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GCMTestActivity extends Activity {

    //Constantes
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final String EXTRA_MESSAGE = "message";
    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_EXPIRATION_TIME = "onServerExpirationTimeMs";
    private static final String PROPERTY_USER = "user";

    public static final long EXPIRATION_TIME_MS = 1000 * 3600 * 24 * 7;

    String SENDER_ID = "73782715721";

    static final String TAG = "GCMDemo";

    //Variables
    private Context context;
    private String regid;
    private GoogleCloudMessaging gcm;

    private EditText txtUsuario;
    private Button btnRegistrar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcmtest);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });



        txtUsuario = (EditText) findViewById(R.id.txtUsuario);
        btnRegistrar = (Button) findViewById(R.id.btnGuadar);

        btnRegistrar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                context = getApplicationContext();

                // Chequemos si está instalado Google Play Services
                gcm = GoogleCloudMessaging.getInstance(GCMTestActivity.this);

                // Obtenemos el Registration ID guardado
                regid = getRegistrationId(context);

                // Si no disponemos de Registration ID comenzamos el registro
                if (regid.equals("")) {
                    TareaRegistroGCM tarea = new TareaRegistroGCM();
                    tarea.execute(txtUsuario.getText().toString());
                }

              }
            });
    }


    private String getRegistrationId(Context context) {
        SharedPreferences prefs = getSharedPreferences(GCMTestActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        String registrationId = prefs.getString(PROPERTY_REG_ID, "");

        if (registrationId.length() == 0) {
            Log.d(TAG, "Registro GCM no encontrado.");
            return "";
        }

        String registeredUser = prefs.getString(PROPERTY_USER, "user");

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);

        long expirationTime = prefs.getLong(PROPERTY_EXPIRATION_TIME, -1);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String expirationDate = sdf.format(new Date(expirationTime));

        Log.d(TAG, "Registro GCM encontrado (usuario=" + registeredUser
                + ", version=" + registeredVersion + ", expira="
                + expirationDate + ")");

        int currentVersion = getAppVersion(context);

        if (registeredVersion != currentVersion) {
            Log.d(TAG, "Nueva versión de la aplicación.");
            return "";
        } else if (System.currentTimeMillis() > expirationTime) {
            Log.d(TAG, "Registro GCM expirado.");
            return "";
        } else if (!txtUsuario.getText().toString().equals(registeredUser)) {
            Log.d(TAG, "Nuevo nombre de usuario.");
            return "";
        }

        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Error al obtener versión: " + e);
        }
    }




    private class TareaRegistroGCM extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            String msg = "";

            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }

                // Nos registramos en los servidores de GCM
                regid = gcm.register(SENDER_ID);

                Log.d(TAG, "Registrado en GCM: registration_id=" + regid);

                // Nos registramos en nuestro servidor
                boolean registrado = registroServidor(params[0], regid);

                // Guardamos los datos del registro
                if (registrado) {
                    msg = "Correcto";
                    setRegistrationId(context, params[0], regid);
                }
            } catch (IOException ex) {
                Log.d(TAG, "Error registro en GCM:" + ex.getMessage());
            }


            return msg;
        }

        @Override
        protected void onPostExecute(String result) {
            final String mensaje;
            if(!result.isEmpty() && result != null){
                mensaje = "Se Registro Dispositivo";
            }else{
                mensaje = "No se pudo registar el dispositivo";
            }

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), mensaje , Toast.LENGTH_SHORT).show();
                }
            });

        }
    }



    private void setRegistrationId(Context context, String user, String regId) {
        SharedPreferences prefs = getSharedPreferences(GCMTestActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        int appVersion = getAppVersion(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_USER, user);
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.putLong(PROPERTY_EXPIRATION_TIME, System.currentTimeMillis()	+ EXPIRATION_TIME_MS);

        editor.commit();
    }

    private boolean registroServidor(String usuario, String regId) {
        boolean reg = false;

        Log.i("======================", "registroServidor()");

        try {

            JsonObject object = new JsonObject();
            object.addProperty("usuario", usuario);
            object.addProperty("codigoGCM", regId);
            String json = object.toString();

            HttpClientUtil RestClient = new HttpClientUtil();

            String respuesta = RestClient.POST("dispositivos",json);

            if(respuesta.equalsIgnoreCase("CORRECTO")){
                reg = true;
            }

        } catch (Exception ex) {
            Log.e("GCMIntentService", "Error: " + ex);
        }
        return reg;
    }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }


}