package com.ruege.mobile.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

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
    
    /**
     * Проверяет, подключен ли пользователь через Wi-Fi
     * @param context контекст приложения
     * @return true, если есть подключение к Wi-Fi
     */
    public static boolean isWifiConnected(@Nullable Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = getNetworkCapabilities(context);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            ConnectivityManager connectivityManager = getConnectivityManager(context);
            if (connectivityManager == null) {
                return false;
            }
            
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * Получает тип сетевого подключения в виде строки
     * @param context контекст приложения
     * @return строка с типом подключения: "WIFI", "MOBILE", "ETHERNET", "NONE" или "UNKNOWN"
     */
    @NonNull
    public static String getConnectionType(@Nullable Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = getNetworkCapabilities(context);
            if (capabilities == null) {
                return "NONE";
            }
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WIFI";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "MOBILE";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "ETHERNET";
            } else {
                return "UNKNOWN";
            }
        } else {
            ConnectivityManager connectivityManager = getConnectivityManager(context);
            if (connectivityManager == null) {
                return "UNKNOWN";
            }
            
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                return "NONE";
            }
            
            int type = activeNetwork.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                return "WIFI";
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                return "MOBILE";
            } else if (type == ConnectivityManager.TYPE_ETHERNET) {
                return "ETHERNET";
            } else {
                return "UNKNOWN";
            }
        }
    }
    
    /**
     * Регистрирует колбэк для отслеживания изменений состояния сети 
     * (рекомендуется использовать в службах и активностях с длительным жизненным циклом)
     * @param context контекст приложения
     * @param callback колбэк для уведомления об изменении состояния сети
     */
    public static void registerNetworkCallback(@Nullable Context context, @Nullable ConnectivityManager.NetworkCallback callback) {
        if (context == null || callback == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        
        try {
            ConnectivityManager connectivityManager = getConnectivityManager(context);
            if (connectivityManager != null) {
                connectivityManager.registerDefaultNetworkCallback(callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not register network callback", e);
        }
    }
    
    /**
     * Отменяет регистрацию колбэка
     * @param context контекст приложения
     * @param callback колбэк для отмены регистрации
     */
    public static void unregisterNetworkCallback(@Nullable Context context, @Nullable ConnectivityManager.NetworkCallback callback) {
        if (context == null || callback == null) {
            return;
        }
        
        try {
            ConnectivityManager connectivityManager = getConnectivityManager(context);
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not unregister network callback", e);
        }
    }
} 