package com.sorcerer.sorcery.iconpack.appliers.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;

import com.annimon.stream.Stream;
import com.sorcerer.sorcery.iconpack.R;
import com.sorcerer.sorcery.iconpack.data.db.Db;
import com.sorcerer.sorcery.iconpack.su.RxSU;
import com.sorcerer.sorcery.iconpack.utils.ImageUtil;
import com.sorcerer.sorcery.iconpack.utils.PackageUtil;
import com.sorcerer.sorcery.iconpack.utils.ResourceUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * @description:
 * @author: Sorcerer
 * @date: 2017/2/2
 */

public class PixelLauncherApplier {

    private static final String LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher";

    private static final String APP_ICONS_PATH =
            "/data/data/com.google.android.apps.nexuslauncher/databases/app_icons.db";

    private static String CACHE_PATH;

    private Context mContext;

    public PixelLauncherApplier(Context context) {
        mContext = context;
        CACHE_PATH = context.getExternalCacheDir() + "/app_icons.db";
    }

    public Observable<List<String>> apply() {
        return RxSU.getInstance().su().filter(grant -> {
            if (!grant) {
                throw new Exception(
                        ResourceUtil.getString(mContext, R.string.apply_for_pixel_no_root_access));
            }
            return true;
        })
                .flatMap(new Function<Boolean, ObservableSource<List<String>>>() {
                    @Override
                    public ObservableSource<List<String>> apply(Boolean grant) throws Exception {
                        return RxSU.getInstance().runAll(
                                "cp " + APP_ICONS_PATH + " " + CACHE_PATH
                        );
                    }
                })
                .flatMap(list -> {
                    Map<String, String> componentDrawableMap =
                            PackageUtil.getComponentDrawableMap(mContext);
                    Map<String, String> todoMap = new HashMap<>();

                    File file = new File(CACHE_PATH);
                    if (!file.exists()) {
                        throw new Exception("No cache file found");
                    }
                    SQLiteDatabase db = SQLiteDatabase
                            .openDatabase(file.getAbsolutePath(), null,
                                    SQLiteDatabase.OPEN_READWRITE);
                    Cursor cursor = db.query("icons", new String[]{"componentName"},
                            null, null, null, null, null);
                    while (cursor.moveToNext()) {
                        String cp = Db.getString(cursor, "componentName");
                        if (componentDrawableMap.containsKey(cp)) {
                            todoMap.put(cp, componentDrawableMap.get(cp));
                        }
                    }
                    cursor.close();

                    Stream.of(todoMap.entrySet()).forEach(entry -> {
                        ContentValues values = new ContentValues();
                        byte[] bitmap = ImageUtil.flattenBitmap(BitmapFactory
                                .decodeResource(mContext.getResources(),
                                        ResourceUtil.getDrawableRes(mContext, entry.getValue())));
                        values.put("icon", bitmap);
                        values.put("icon_low_res", bitmap);
                        db.update("icons", values, "componentName=? and profileId=0",
                                new String[]{entry.getKey()});
                    });
                    db.close();
                    return RxSU.getInstance().runAll(
                            "cp " + CACHE_PATH + " " + APP_ICONS_PATH,
                            "am force-stop " + LAUNCHER_PACKAGE);
                });
    }
}
