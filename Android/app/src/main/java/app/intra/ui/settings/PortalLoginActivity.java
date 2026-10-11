package app.intra.ui.settings;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import app.intra.R;

/**
 * Loads the portal in a WebView so Cloudflare Turnstile can complete.
 * After login the portal stores the JWT in localStorage as portal_token.
 * We inject JS to read it, call /api/v1/tokens, and surface the path ID
 * so the user can copy it or return it to the app.
 */
public class PortalLoginActivity extends AppCompatActivity {

  public static final String EXTRA_PATH_ID = "path_id";
  private static final String PORTAL_URL = "https://portal.oss.co.za/";

  private WebView webView;
  private TextView pathStatus;
  private Button usePathButton;
  private Button copyButton;
  private String foundPathId = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_portal_login);

    webView = findViewById(R.id.webview);
    pathStatus = findViewById(R.id.path_status);
    usePathButton = findViewById(R.id.use_path_button);
    copyButton = findViewById(R.id.copy_button);
    Button backButton = findViewById(R.id.back_button);

    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.addJavascriptInterface(new PortalBridge(), "AndroidBridge");
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        view.evaluateJavascript(EXTRACT_JS, null);
      }
    });
    webView.setWebChromeClient(new WebChromeClient());
    webView.loadUrl(PORTAL_URL);

    backButton.setOnClickListener(v -> finish());

    usePathButton.setOnClickListener(v -> {
      if (foundPathId != null) {
        Intent result = new Intent();
        result.putExtra(EXTRA_PATH_ID, foundPathId);
        setResult(Activity.RESULT_OK, result);
        Toast.makeText(this, "Path ID applied", Toast.LENGTH_SHORT).show();
        finish();
      }
    });

    copyButton.setOnClickListener(v -> {
      if (foundPathId != null) {
        String url = "https://dns1.oss.co.za/" + foundPathId;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("OSS Secure DNS URL", url));
        Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show();
      }
    });
  }

  private static final String EXTRACT_JS =
      "(function(){" +
      "  try {" +
      "    var t = localStorage.getItem('portal_token');" +
      "    if (!t) return;" +
      "    fetch('/api/v1/tokens', {headers:{'Authorization':'Bearer '+t}})" +
      "      .then(function(r){return r.json();})" +
      "      .then(function(data){" +
      "        var id = null;" +
      "        var arr = data.tokens || data;" +
      "        if (Array.isArray(arr)) {" +
      "          for (var i=0;i<arr.length;i++) {" +
      "            var tok = arr[i].token || arr[i].path_id || arr[i];" +
      "            if (typeof tok === 'string' && /^[0-9a-fA-F]{32}$/.test(tok)) { id = tok; break; }" +
      "          }" +
      "        }" +
      "        if (id) AndroidBridge.onPathId(id);" +
      "      }).catch(function(){});" +
      "  } catch(e) {}" +
      "})();";

  private class PortalBridge {
    @JavascriptInterface
    public void onPathId(String pathId) {
      runOnUiThread(() -> {
        foundPathId = pathId;
        pathStatus.setText("Path ID: " + pathId);
        usePathButton.setEnabled(true);
        copyButton.setEnabled(true);
      });
    }
  }
}
