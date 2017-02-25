package com.sorcerer.sorcery.iconpack.apply.appliers.database.pixel;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.sorcerer.sorcery.iconpack.apply.appliers.database.base.BaseApplyActivity;
import com.sorcerer.sorcery.iconpack.apply.appliers.database.base.ILauncherApplier;
import com.sorcerer.sorcery.iconpack.shortcuts.AppShortcutsHelper;

/**
 * @description:
 * @author: Sorcerer
 * @date: 2017/2/9
 */

public class PixelApplyActivity extends BaseApplyActivity {

    public static void apply(Context context, boolean apply) {
        Intent intent = new Intent(context, PixelApplyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.setAction(apply ? ACTION_APPLY : ACTION_RESTORE);
        intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }

    private AppShortcutsHelper mAppShortcutsHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mAppShortcutsHelper = new AppShortcutsHelper(this);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String packageName() {
        return "com.google.android.apps.nexuslauncher";
    }

    @Override
    protected String componentName() {
        return "com.google.android.apps.nexuslauncher.NexusLauncherActivity";
    }

    @Override
    protected ILauncherApplier applier() {
        return new PixelLauncherApplier(mActivity);
    }

    @Override
    protected void onApplyComplete() {
        super.onApplyComplete();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mAppShortcutsHelper != null) {
            mAppShortcutsHelper.addApplyPixelShortcut();
        }
    }

    @Override
    protected void onRestoreComplete() {
        super.onRestoreComplete();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mAppShortcutsHelper != null) {
            mAppShortcutsHelper.removeApplyPixelShortcut();
        }
    }

}
