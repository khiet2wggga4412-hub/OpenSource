package com.liquid.org.ui.overlay;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class MusicStore {
    public static final class Track {
        public final long id;
        public final String name;
        public final String artist;
        public final String url;
        Track(long id, String name, String artist, String url) { this.id = id; this.name = name; this.artist = artist; this.url = url; }
    }
    public interface Callback { void onResult(List<Track> tracks); void onError(String message); }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Track current;

    public MusicStore(Context context) { this.context = context.getApplicationContext(); }

    public void search(String query, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String encoded = URLEncoder.encode(query == null ? "" : query.trim(), "UTF-8");
                URL endpoint = new URL("https://music.163.com/api/search/get/web?csrf_token=&limit=20&type=1&s=" + encoded);
                connection = (HttpURLConnection) endpoint.openConnection();
                connection.setConnectTimeout(10000); connection.setReadTimeout(15000); connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IllegalStateException("HTTP " + connection.getResponseCode());
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder body = new StringBuilder(); String line; while ((line = reader.readLine()) != null) body.append(line); reader.close();
                JSONObject root = new JSONObject(body.toString()); JSONObject resultObject = root.optJSONObject("result"); JSONArray songs = resultObject == null ? null : resultObject.optJSONArray("songs");
                List<Track> result = new ArrayList<>();
                if (songs != null) for (int i = 0; i < songs.length(); i++) { JSONObject song = songs.optJSONObject(i); if (song == null) continue; JSONArray artists = song.optJSONArray("artists"); JSONObject firstArtist = artists != null && artists.length() > 0 ? artists.optJSONObject(0) : null; String artist = firstArtist == null ? "" : firstArtist.optString("name", ""); long id = song.optLong("id"); result.add(new Track(id, song.optString("name", ""), artist, "https://api.injahow.cn/meting/?server=netease&type=url&id=" + id)); }
                main.post(() -> callback.onResult(result));
            } catch (Exception e) { main.post(() -> callback.onError(e.getMessage() == null ? "Network error" : e.getMessage())); }
            finally { if (connection != null) connection.disconnect(); }
        }).start();
    }

    public void play(Track track, Callback callback) {
        stop(); current = track;
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
            player.setDataSource(track.url); player.setOnPreparedListener(MediaPlayer::start); player.setOnErrorListener((mp, what, extra) -> { if (callback != null) main.post(() -> callback.onError("Playback unavailable")); return true; }); player.prepareAsync();
            if (callback != null) callback.onResult(new ArrayList<>());
        } catch (Exception e) { if (callback != null) callback.onError(e.getMessage()); }
    }

    public void stop() { if (player != null) { try { player.stop(); } catch (Exception ignored) {} player.release(); player = null; } current = null; }
    public Track getCurrent() { return current; }
}
