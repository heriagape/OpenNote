package co.heri.dev.opennote;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    public static final String USER_TOKEN = null;
    String email, password;
    EditText email_EditText, password_EditText;

    String SERVER_ADDRESS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email_EditText = (EditText) findViewById(R.id.email_edit);
        password_EditText = (EditText) findViewById(R.id.pass_edit);




        SharedPreferences prefs = getSharedPreferences("open_settings", MODE_PRIVATE);
        String restoredSetting = prefs.getString("ip", null);
        if (restoredSetting != null) {
            String ip_set = prefs.getString("ip", "No name defined");//"No name defined" is the default value.
            int port_set = prefs.getInt("port", 9292); //0 is the default value.
            SERVER_ADDRESS = ip_set + ":" + Integer.toString(port_set);
            if (prefs.getString("token", null) != null) {
                // IMPORTANT: This line is verydangerous
                login(prefs.getString("email", null), prefs.getString("token", null), "true");
            }
        }else{
            startActivity(new Intent(LoginActivity.this, ServerSetActivity.class));
        }




        findViewById(R.id.register_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), RegisterActivity.class));
            }
        });

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(getBaseContext(), MainActivity.class));

                email = email_EditText.getText().toString();
                password = password_EditText.getText().toString();

                login(email, password, "false");
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.server_enu:
                startActivity(new Intent(LoginActivity.this, ServerSetActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void login(final String email, String password, String token) {


        class LoginAsync extends AsyncTask<String, Void, String> {

            private Dialog loadingDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loadingDialog = ProgressDialog.show(LoginActivity.this, "Please wait", "Loading...");
                Log.e("INTERNET", "Dialog");

            }

            @Override
            protected String doInBackground(String... params) {
                String uemail = params[0];
                String pass = params[1];
                Boolean token = Boolean.valueOf(params[2]);

                InputStream is = null;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("email", uemail));
                if (token) {
                    nameValuePairs.add(new BasicNameValuePair("token", pass));
                } else {
                    nameValuePairs.add(new BasicNameValuePair("password", pass));
                }

                String result = null;

                try{
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://" + SERVER_ADDRESS + "/login"); // login link to be changed
//                    httpPost.setHeader("Content-type", "application/json");

                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = httpClient.execute(httpPost);

                    HttpEntity entity = response.getEntity();

                    is = entity.getContent();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
                    StringBuilder sb = new StringBuilder();

                    String line = null;
                    while ((line = reader.readLine()) != null)
                    {
                        sb.append(line + "\n");
                    }
                    result = sb.toString();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                return result;
            }

            @Override
            protected void onPostExecute(String result){
                JSONObject jsonObj = null;
                JSONObject token = null;
                Boolean error = null;
                loadingDialog.dismiss();

                try {
                    jsonObj = new JSONObject(result);
                    error = jsonObj.getBoolean("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                TextView resultText = (TextView) findViewById(R.id.response_text);
//                resultText.setText(error.toString());


                if (error) try {
                    resultText.setText(jsonObj.getString("message"));
                    Toast.makeText(getBaseContext(), jsonObj.getString("message"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                else {
                    try {
                        token = jsonObj.getJSONObject("_token");
                        SharedPreferences.Editor editor = getSharedPreferences("open_settings", MODE_PRIVATE).edit();
                        editor.putString("token", token.getString("token"));
                        editor.putString("email", token.getString("email"));
                        editor.commit();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    intent.putExtra(USER_TOKEN, token.toString());
                    finish();
                    startActivity(intent);
                }

            }
        }

        LoginAsync la = new LoginAsync();

        la.execute(email, password, token);

    }


}
