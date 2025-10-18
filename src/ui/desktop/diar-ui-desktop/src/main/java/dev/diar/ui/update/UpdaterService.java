package dev.diar.ui.update;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class UpdaterService {
    private final String repoOwner;
    private final String repoName;

    public UpdaterService(String repoOwner, String repoName) {
        this.repoOwner = Objects.requireNonNull(repoOwner);
        this.repoName = Objects.requireNonNull(repoName);
    }

    public static class UpdateAsset {
        public final String name;
        public final String url;
        public final long size;
        public final String contentType;
        public UpdateAsset(String name, String url, long size, String contentType) {
            this.name = name; this.url = url; this.size = size; this.contentType = contentType;
        }
        public boolean isWindowsInstaller() {
            String n = name.toLowerCase();
            return n.endsWith(".msi") || n.endsWith(".exe");
        }
    }

    public static class UpdateInfo {
        public final String tag;
        public final String name;
        public final String body;
        public final boolean prerelease;
        public final List<UpdateAsset> assets;
        public final boolean isNewer;
        public UpdateInfo(String tag, String name, String body, boolean prerelease, List<UpdateAsset> assets, boolean isNewer) {
            this.tag = tag; this.name = name; this.body = body; this.prerelease = prerelease; this.assets = assets; this.isNewer = isNewer;
        }
    }

    public UpdateInfo checkLatest(String currentVersion, boolean includePrereleases) throws Exception {
        String api = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases";
        JSONArray releases = new JSONArray(httpGet(api));
        JSONObject latest = null;
        for (int i = 0; i < releases.length(); i++) {
            JSONObject r = releases.getJSONObject(i);
            boolean prerelease = r.optBoolean("prerelease", false);
            if (!includePrereleases && prerelease) continue;
            latest = r; break;
        }
        if (latest == null) return new UpdateInfo(currentVersion, "", "", false, List.of(), false);
        String tag = latest.optString("tag_name", latest.optString("name", ""));
        String name = latest.optString("name", tag);
        String body = latest.optString("body", "");
        boolean prerelease = latest.optBoolean("prerelease", false);
        List<UpdateAsset> assets = new ArrayList<>();
        JSONArray ja = latest.optJSONArray("assets");
        if (ja != null) {
            for (int i = 0; i < ja.length(); i++) {
                JSONObject a = ja.getJSONObject(i);
                assets.add(new UpdateAsset(
                    a.optString("name"),
                    a.optString("browser_download_url"),
                    a.optLong("size", 0L),
                    a.optString("content_type")
                ));
            }
        }
        boolean newer = isNewerVersion(normalize(currentVersion), normalize(tag));
        return new UpdateInfo(tag, name, body, prerelease, assets, newer);
    }

    private static String httpGet(String urlStr) throws Exception {
        URL url = new java.net.URI(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "DIAR-E-Updater");
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    public Path download(UpdateAsset asset, Path destDir, BiConsumer<Long, Long> progress) throws Exception {
        Files.createDirectories(destDir);
        URL url = new java.net.URI(asset.url).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "DIAR-E-Updater");
        long total = conn.getContentLengthLong();
        Path target = destDir.resolve(asset.name);
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(target.toFile())) {
            byte[] buf = new byte[8192];
            long read = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                read += n;
                if (progress != null) progress.accept(read, total);
            }
        }
        return target;
    }

    private static String normalize(String ver) {
        if (ver == null) return "0.0.0";
        ver = ver.trim();
        if (ver.startsWith("v") || ver.startsWith("V")) ver = ver.substring(1);
        if (ver.equalsIgnoreCase("dev") || ver.isBlank()) return "0.0.0";
        return ver.replaceAll("[^0-9.]+", "");
    }

    public static boolean isNewerVersion(String current, String latest) {
        String[] a = (current + "").split("\\.");
        String[] b = (latest + "").split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int ai = i < a.length ? parse(a[i]) : 0;
            int bi = i < b.length ? parse(b[i]) : 0;
            if (bi > ai) return true;
            if (bi < ai) return false;
        }
        return false;
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
