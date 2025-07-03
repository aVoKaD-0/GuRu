package com.ruege.mobile.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import timber.log.Timber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Утилитарный класс для проверки состояния сети
 */
public class NetworkUtils {
    
    private static final String TAG = "NetworkUtils";
    
    /**
     * Получает менеджер подключений
     */
    @Nullable
    private static ConnectivityManager getConnectivityManager(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    /**
     * Получает объект NetworkCapabilities для проверки возможностей сети
     */
    @Nullable
    private static NetworkCapabilities getNetworkCapabilities(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null;
        }
        
        ConnectivityManager connectivityManager = getConnectivityManager(context);
        if (connectivityManager == null) {
            return null;
        }
        
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return null;
        }
        
        return connectivityManager.getNetworkCapabilities(network);
    }
    
    /**
     * Проверяет доступность сети
     * @param context контекст приложения
     * @return true, если есть подключение к интернету
     */
    public static boolean isNetworkAvailable(@Nullable Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = getNetworkCapabilities(context);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            ConnectivityManager connectivityManager = getConnectivityManager(context);
            if (connectivityManager == null) {
                return false;
            }
            
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
} 