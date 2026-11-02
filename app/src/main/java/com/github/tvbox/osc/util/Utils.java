package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatDelegate;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.bean.VodInfo;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;


public class Utils {

    public static boolean supportsPiPMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * 是否为大屏设备（平板）。以最小宽度 600dp 为分界（Google 平板/大屏判定标准）。
     * 手机（含大屏手机）为 false，横向摆放的平板/折叠屏展开态为 true。
     */
    public static boolean isTablet(Context context) {
        return context != null
                && context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    /**
     * 首页海报网格列数：按当前可用宽度动态计算，手机竖屏保持原样，平板/横屏自动多列。
     * 横版 16:9 卡片（横屏海报开关打开）目标列宽 400dp；
     * 竖版 2:3 海报目标列宽 180dp。
     * 手机竖屏 360dp 时分别为 1 列 / 2 列，与旧版固定值完全一致，不影响现有观感。
     */
    public static int getPosterSpanCount(Context context) {
        int widthDp = 360;
        if (context != null) {
            int w = context.getResources().getConfiguration().screenWidthDp;
            if (w > 0) {
                widthDp = w;
            }
        }
        if (Hawk.get(HawkConfig.LANDSCAPE_CARD, false)) {
            // 横版卡片：一行一张~三张，最多 4 列
            int span = Math.round(widthDp / 400f);
            return Math.max(1, Math.min(4, span));
        }
        // 竖版海报：至少 2 列，平板横屏最多 6 列
        int span = Math.round(widthDp / 180f);
        return Math.max(2, Math.min(6, span));
    }

    public static int getSeriesSpanCount(List<VodInfo.VodSeries> list) {
        int spanCount = 4;
        int total = 0;
        for (VodInfo.VodSeries item : list) total += item.name.length();
        int offset = (int) Math.ceil((double) total / list.size());
        if (offset >= 12) spanCount = 1;
        else if (offset >= 8) spanCount = 2;
        else if (offset >= 4) spanCount = 3;
        else if (offset >= 2) spanCount = 4;
        return spanCount;
    }

    public static String stringForTime(long timeMs) {
//        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
//            return "00:00";
//        }
        long totalSeconds = timeMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public static List<VideoInfo> getVideoList() {
        List<VideoInfo> videoList = new ArrayList<>();
        Cursor cursor = App.getInstance().getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[] { // 查询内容
                        MediaStore.Video.Media._ID, // 视频id
                        MediaStore.Video.Media.DATA, // 视频路径
                        MediaStore.Video.Media.SIZE, // 视频字节大小
                        MediaStore.Video.Media.DISPLAY_NAME, // 视频名称 xxx.mp4
                        MediaStore.Video.Media.TITLE, // 视频标题
                        MediaStore.Video.Media.DURATION, // 视频时长
                        MediaStore.Video.Media.RESOLUTION, // 视频分辨率 X x Y格式
                        MediaStore.Video.Media.IS_PRIVATE,
                        MediaStore.Video.Media.BUCKET_ID,
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Video.Media.BOOKMARK // 上次视频播放的位置
                },
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                VideoInfo videoInfo = new VideoInfo();
                videoInfo.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                videoInfo.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
                videoInfo.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
                videoInfo.setDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)));
                videoInfo.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)));
                videoInfo.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
                videoInfo.setResolution(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)));
                videoInfo.setIsPrivate(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_PRIVATE)));
                videoInfo.setBucketId(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)));
                videoInfo.setBucketDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)));
                videoInfo.setBookmark(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BOOKMARK)));
                videoList.add(videoInfo);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return videoList;
    }

    public static boolean isDarkTheme(){
        int currentNightMode = App.getInstance().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES || AppCompatDelegate.getDefaultNightMode()==AppCompatDelegate.MODE_NIGHT_YES;
    }

    public static void initTheme(){
        switch (Hawk.get(HawkConfig.THEME_TAG,0)) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }
}