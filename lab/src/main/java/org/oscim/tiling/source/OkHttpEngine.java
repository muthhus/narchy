package org.oscim.tiling.source;

import com.squareup.okhttp.*;

import org.oscim.core.Tile;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;

public class OkHttpEngine implements HttpEngine {
    static final Logger log = LoggerFactory.getLogger(OkHttpEngine.class);

    private final OkHttpClient mClient;
    private final UrlTileSource mTileSource;

    public static class OkHttpFactory implements HttpEngine.Factory {
        private final OkHttpClient mClient;

        public OkHttpFactory() {
            mClient = new OkHttpClient();
        }

        public OkHttpFactory(Cache responseCache) {
            mClient = new OkHttpClient();
            mClient.setCache(responseCache);
        }

        @Override
        public HttpEngine create(UrlTileSource tileSource) {
            return new OkHttpEngine(mClient, tileSource);
        }
    }

    private InputStream inputStream;

    public OkHttpEngine(OkHttpClient client, UrlTileSource tileSource) {
        mClient = client;
        mTileSource = tileSource;
    }

    @Override
    public InputStream read() throws IOException {
        return inputStream;
    }

    @Override
    public void sendRequest(Tile tile) throws IOException {
        if (tile == null) {
            throw new IllegalArgumentException("Tile cannot be null.");
        }
        URL url = new URL(mTileSource.getTileUrl(tile));
        Request.Builder b = new Request.Builder().url(url);

        for (Entry<String, String> opt : mTileSource.getRequestHeader().entrySet())
            b.addHeader(opt.getKey(), opt.getValue());

        Call conn = mClient.newCall(b.build());

        try {
            Response execute = conn.execute();
            inputStream = execute.body().byteStream();
        } catch (IOException e) {
            throw new IOException("ERROR " + e + " " + url);
        }
    }

    @Override
    public void close() {
        if (inputStream == null)
            return;

        final InputStream is = inputStream;
        inputStream = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                IOUtils.closeQuietly(is);
            }
        }).start();
    }

    @Override
    public void setCache(OutputStream os) {
        // OkHttp cache implented through tileSource setResponseCache
    }

    @Override
    public boolean requestCompleted(boolean success) {
        IOUtils.closeQuietly(inputStream);
        inputStream = null;

        return success;
    }
}
