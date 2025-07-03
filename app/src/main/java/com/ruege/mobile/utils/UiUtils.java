package com.ruege.mobile.utils;

import android.content.Context;
import android.content.res.Configuration;

public class UiUtils {

    /**
     * Проверяет, включен ли темный режим
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Конвертирует dp в пиксели
     */
    public static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
} 