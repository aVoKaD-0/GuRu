package com.ruege.mobile.glide;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import java.io.InputStream;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@GlideModule
public class MyAppGlideModule extends AppGlideModule {

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    interface OkHttpClientEntryPoint {
        OkHttpClient getOkHttpClient();
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        OkHttpClientEntryPoint entryPoint =
                EntryPointAccessors.fromApplication(context, OkHttpClientEntryPoint.class);
        OkHttpClient okHttpClient = entryPoint.getOkHttpClient();

        Log.d("MyAppGlideModule", "Using OkHttpClient for Glide: " + okHttpClient);
        if (okHttpClient != null) {
            Log.d("MyAppGlideModule", "OkHttpClient interceptors: " + okHttpClient.interceptors());
            Log.d("MyAppGlideModule", "OkHttpClient authenticator: " + okHttpClient.authenticator());
        }

        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(okHttpClient));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
} 