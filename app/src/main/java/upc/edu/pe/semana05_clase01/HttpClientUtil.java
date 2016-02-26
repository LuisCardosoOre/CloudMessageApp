package upc.edu.pe.semana05_clase01;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by lcardoso on 07/10/2015.
 */
public class HttpClientUtil {

    static String host = "192.168.0.6:8080";

    public static String POST(String urlRest, String param) throws Exception {
        HttpURLConnection conn = null;
        OutputStream os = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(String.format("http://%s/RestCloud/rest/%s", host, urlRest));
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("charset", "utf-8");

            os = conn.getOutputStream();
            os.write(param.getBytes());
            os.flush();

            Log.d("HttpClientUtil", "Status code: " + conn.getResponseCode());

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();

            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }

              String result = sb.toString();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                return result;
            } else {
                Log.e("HttpClientUtil", String.format("Failed: HTTP error dode : %d %s", conn.getResponseCode(), conn.getResponseMessage()));
                throw new RuntimeException(result);
            }
        } catch (RuntimeException ex){
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
        finally {
            if(conn != null){
                conn.disconnect();
            }
            if(os != null){
                os.close();
            }
            if(reader != null){
                reader.close();
            }
        }
    }
}
