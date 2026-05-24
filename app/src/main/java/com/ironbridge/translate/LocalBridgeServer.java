package com.ironbridge.translate;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public final class LocalBridgeServer extends NanoHTTPD {

    private static volatile LocalBridgeServer instance;
    private static final int PORT = 8080;

    private volatile boolean started;

    private LocalBridgeServer() {
        super(PORT);
    }

    public static synchronized LocalBridgeServer getInstance() {
        if (instance == null) {
            instance = new LocalBridgeServer();
        }
        return instance;
    }

    public synchronized void ensureStarted() throws IOException {
        if (started && isAlive()) {
            return;
        }

        if (started) {
            stop();
            started = false;
        }

        start(1000, true);
        started = true;
    }

    public int getPort() {
        return PORT;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (!"/translate".equals(uri) && !"/".equals(uri)) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain; charset=utf-8", "Not found");
        }

        String html = buildBridgeHtml();
        Response response = newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html);
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.addHeader("Pragma", "no-cache");
        return response;
    }

    private String buildBridgeHtml() {
        return "<!doctype html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>IronBridge Translate</title>"
                + "<style>"
                + "body{margin:0;padding:16px;background:#111;color:#eee;font-family:system-ui,sans-serif;}"
                + "#text{white-space:pre-wrap;word-break:break-word;padding:14px;border-radius:16px;background:rgba(255,255,255,.04);border:1px solid #333;min-height:6rem;line-height:1.5}"
                + ".badge{font-size:12px;color:#888;margin-bottom:10px;font-weight:700;letter-spacing:.08em;text-transform:uppercase}"
                + ".hint{margin-top:12px;color:#9aa0a6;font-size:13px;line-height:1.45}"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class=\"badge\">IronBridge Offline Translate</div>"
                + "<div id=\"text\" lang=\"en\" dir=\"ltr\"></div>"
                + "<div class=\"hint\">Firefox can translate this page locally if the translation engine is available. "
                + "When the page content changes, the bridge tries to copy the translated text back to the clipboard.</div>"
                + "<script>"
                + "(function(){"
                + "const params=new URLSearchParams(location.search);"
                + "const raw=params.get('q')||'';"
                + "const lang=params.get('lang')||'en';"
                + "const source=document.getElementById('text');"
                + "const original=raw.trim();"
                + "source.textContent=original || 'No text supplied';"
                + "source.setAttribute('lang', lang);"
                + "source.setAttribute('dir', 'ltr');"
                + "let copied=false;"
                + "function copyText(value){"
                + " if(copied || !value || value.trim()===original) return;"
                + " copied=true;"
                + " const done=()=>{copied=true;};"
                + " if(navigator.clipboard && navigator.clipboard.writeText){"
                + "   navigator.clipboard.writeText(value).then(done).catch(function(){copied=false;});"
                + " } else {"
                + "   const ta=document.createElement('textarea');"
                + "   ta.value=value;"
                + "   ta.style.position='fixed';"
                + "   ta.style.opacity='0';"
                + "   document.body.appendChild(ta);"
                + "   ta.focus();ta.select();"
                + "   try{document.execCommand('copy');done();}catch(e){copied=false;}"
                + "   document.body.removeChild(ta);"
                + " }"
                + "}"
                + "new MutationObserver(function(){"
                + " const current=source.textContent.trim();"
                + " if(current && current!==original){copyText(current);}"
                + "}).observe(source,{childList:true,subtree:true,characterData:true});"
                + "setTimeout(function(){copyText(source.textContent.trim());},1500);"
                + "})();"
                + "</script>"
                + "</body>"
                + "</html>";
    }
}
