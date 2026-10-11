package app.intra.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import app.intra.R;

public class PortalLoginActivity extends AppCompatActivity {

  public static final String EXTRA_PATH_ID = "path_id";
  private static final String LOGIN_URL = "https://portal.oss.co.za/api/v1/login";
  private static final String TOKENS_URL = "https://portal.oss.co.za/api/v1/tokens";

  private EditText emailField;
  private EditText passwordField;
  private TextView statusView;
  private Button loginButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_portal_login);

    emailField = findViewById(R.id.email);
    passwordField = findViewById(R.id.password);
    statusView = findViewById(R.id.status);
    loginButton = findViewById(R.id.login_button);

    loginButton.setOnClickListener(v -> doLogin());
  }

  private void doLogin() {
    final String email = emailField.getText().toString().trim();
    final String password = passwordField.getText().toString();
    if (email.isEmpty() || password.isEmpty()) {
      statusView.setText("Enter email and password.");
      return;
    }
    loginButton.setEnabled(false);
    statusView.setText("Logging in…");

    new Thread(() -> {
      try {
        String jwt = login(email, password);
        String pathId = fetchPathId(jwt);
        new Handler(Looper.getMainLooper()).post(() -> {
          Intent result = new Intent();
          result.putExtra(EXTRA_PATH_ID, pathId);
          setResult(Activity.RESULT_OK, result);
          Toast.makeText(this, "Path ID loaded", Toast.LENGTH_SHORT).show();
          finish();
        });
      } catch (Exception e) {
        new Handler(Looper.getMainLooper()).post(() -> {
          statusView.setText("Error: " + e.getMessage());
          loginButton.setEnabled(true);
        });
      }
    }).start();
  }

  private String login(String email, String password) throws Exception {
    URL url = new URL(LOGIN_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);
    String body = "{\"email\":\"" + escape(email) + "\",\"password\":\"" + escape(password) + "\"}";
    try (OutputStream os = conn.getOutputStream()) {
      os.write(body.getBytes(StandardCharsets.UTF_8));
    }
    int code = conn.getResponseCode();
    String response = read(conn);
    if (code != 200) {
      throw new Exception("Login failed (" + code + "): " + response);
    }
    // Expect {"access_token":"eyJ..."} or similar
    Matcher m = Pattern.compile("\"(?:access_token|token)\"\s*:\s*\"([^\"]+)\"").matcher(response);
    if (!m.find()) {
      throw new Exception("No token in login response");
    }
    return m.group(1);
  }

  private String fetchPathId(String jwt) throws Exception {
    URL url = new URL(TOKENS_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Authorization", "Bearer " + jwt);
    int code = conn.getResponseCode();
    String response = read(conn);
    if (code != 200) {
      throw new Exception("Tokens failed (" + code + "): " + response);
    }
    // Look for a 32-hex path ID / token field
    Matcher m = Pattern.compile("\"(?:token|path_id|pathId)\"\s*:\s*\"([0-9a-fA-F]{32})\"").matcher(response);
    if (!m.find()) {
      throw new Exception("No path ID found in response");
    }
    return m.group(1);
  }

  private String read(HttpURLConnection conn) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        conn.getErrorStream() != null && conn.getResponseCode() >= 400
            ? conn.getErrorStream() : conn.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) sb.append(line);
    reader.close();
    return sb.toString();
  }

  private String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
