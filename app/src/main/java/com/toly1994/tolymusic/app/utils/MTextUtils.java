package com.toly1994.tolymusic.app.utils;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MTextUtils {
    /**
     * 设置关键字颜色
     *
     * @param text
     * @param key
     */
    public static SpannableStringBuilder setTextColorByKey(String text, String key, int color) {
        SpannableStringBuilder style = new SpannableStringBuilder(text);
        if (text.toLowerCase(Locale.ENGLISH).contains(key.toLowerCase(Locale.ENGLISH))) {
            int start = text.toLowerCase(Locale.ENGLISH).indexOf(key.toLowerCase(Locale.ENGLISH));
            style.setSpan(new ForegroundColorSpan(color),
                    start, start + key.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        return style;
    }

    public static String long2Minute(long dur) {
        Date currentTime = new Date(dur);
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        return formatter.format(currentTime);
    }
}
