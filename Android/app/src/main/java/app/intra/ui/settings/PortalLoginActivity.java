package app.intra.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import app.intra.R;

/**
 * Loads the portal in a WebView so Cloudflare Turnstile can complete.
 * After login the portal stores the JWT in localStorage as portal_token.
 * We inject JS to read it, call /api/v1/tokens, and return the first 32-hex path ID.
 */
public class PortalLoginActivity extends AppCompatActivity {

  public static final String EXTRA_PATH_ID = "path_id";
  private static final String PORTAL_URL = "https://portal.oss.co.za/";
  private WebView webView;
  private boolean extracted = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_portal_login);

    webView = findViewById(R.id.webview);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.addJavascriptInterface(new PortalBridge(), "AndroidBridge");
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        // After any page load, try to extract the path ID if the user is logged in.
        if (!extracted) {
          view.evaluateJavascript(EXTRACT_JS, null);
        }
      }
    });
    webView.setWebChromeClient(new WebChromeClient());
    webView.loadUrl(PORTAL_URL);
  }

  // JS: if portal_token exists, fetch /api/v1/tokens and call AndroidBridge.onPathId(...)
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
      if (extracted) return;
      extracted = true;
      runOnUiThread(() -> {
        Intent result = new Intent();
        result.putExtra(EXTRA_PATH_ID, pathId);
        setResult(Activity.RESULT_OK, result);
        Toast.makeText(PortalLoginActivity.this, "Path ID loaded", Toast.LENGTH_SHORT).show();
        finish();
      });
    }
  }
}
