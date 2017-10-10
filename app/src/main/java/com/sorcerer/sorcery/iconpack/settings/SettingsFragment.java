package com.sorcerer.sorcery.iconpack.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.sorcerer.sorcery.iconpack.MainActivity;
import com.sorcerer.sorcery.iconpack.R;
import com.sorcerer.sorcery.iconpack.SettingsActivity;
import com.sorcerer.sorcery.iconpack.settings.about.AboutDialog;
import com.sorcerer.sorcery.iconpack.settings.general.tab.CustomizeTabsFragment;
import com.sorcerer.sorcery.iconpack.settings.licenses.ui.OpenSourceLibFragment;
import com.sorcerer.sorcery.iconpack.ui.callbacks.OnMultiTouchListener;
import com.sorcerer.sorcery.iconpack.ui.fragments.BasePreferenceFragmentCompat;
import com.sorcerer.sorcery.iconpack.utils.FileUtil;
import com.sorcerer.sorcery.iconpack.utils.ResourceUtil;

import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

/**
 * @description:
 * @author: Sorcerer
 * @date: 2016/11/11
 */

@SuppressWarnings("ALL")
public class SettingsFragment extends BasePreferenceFragmentCompat {
    private PreferenceScreen mPreferenceScreen;
    private Preference mGeneralCustomizeTabs;
    private Preference mGeneralClearCachePreference;
    private Preference mGeneralLanguagePreference;
    private SwitchPreference mUiNavBarPreference;
    private PreferenceGroup mDevPreferenceGroup;
    private Preference mAboutAppPreference;
    private Preference mAboutLibPreference;
    private boolean mChangingTheme = false;
    private SwitchPreference mUiLessAnimPreference;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_settings, rootKey);
        mPreferenceScreen = (PreferenceScreen) findPreference("preference_screen");
        initGeneral();
        initUi();

        mDevPreferenceGroup = (PreferenceGroup) findPreference("preference_group_dev");
        if (!mPrefs.devOptionsOpened().getValue()) {
            mPreferenceScreen.removePreference(mDevPreferenceGroup);
        } else {
            initDevOps();
        }
        initAbout();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private void initGeneral() {
        mGeneralClearCachePreference = findPreference("preference_general_clear_cache");

        mGeneralLanguagePreference = findPreference("preference_general_language");
        if (!TextUtils.isEmpty(mPrefs.language().get())) {
            mGeneralLanguagePreference.setSummary(mPrefs.language().get());
        }
        mGeneralLanguagePreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Locale[] locales = new Locale[]{
                                Locale.ENGLISH,
                                Locale.CHINESE
                        };
                        new MaterialDialog.Builder(getContext())
                                .items(Stream.of(locales).map(new Function<Locale, String>() {
                                    @Override
                                    public String apply(Locale locale) {
                                        return locale.toString();
                                    }
                                }).toList())
                                .itemsCallback(new MaterialDialog.ListCallback() {
                                    @Override
                                    public void onSelection(MaterialDialog dialog, View itemView,
                                                            int position, CharSequence text) {
                                        mPrefs.language(locales[position].getLanguage());
                                        restartApp();
                                    }
                                })
                                .build().show();
                        return true;
                    }
                });

        mGeneralCustomizeTabs = findPreference("preference_general_customize_tabs");
        mGeneralCustomizeTabs.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ((SettingsActivity) getActivity())
                                .addFragment(new CustomizeTabsFragment());
                        return true;
                    }
                });

        mGeneralClearCachePreference.setSummary("...");
        updateCache(Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> emitter) throws Exception {
                emitter.onNext(FileUtil.calculateDirectorySize(getContext().getCacheDir()));
            }
        }).subscribeOn(Schedulers.newThread()));

        mGeneralClearCachePreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        updateCache(Observable.create(new ObservableOnSubscribe<Long>() {
                            @Override
                            public void subscribe(ObservableEmitter<Long> emitter)
                                    throws Exception {
                                FileUtil.deleteDirectory(getContext().getCacheDir());
                                emitter.onNext(FileUtil
                                        .calculateDirectorySize(getContext().getCacheDir()));
                            }
                        }).subscribeOn(Schedulers.newThread()));
                        return false;
                    }
                });

        SwitchPreference themePreference =
                (SwitchPreference) findPreference("preference_general_theme");

        themePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mChangingTheme) {
                    return false;
                }
                mChangingTheme = true;
                mPrefs.nightMode((Boolean) newValue);
                restartApp();
                return true;
            }
        });
    }

    private void initUi() {
        mUiNavBarPreference =
                (SwitchPreference) findPreference("preference_switch_ui_transparent_nav_bar");

        mUiNavBarPreference.setChecked(mPrefs.enableTransparentNavBar().get());
        mUiNavBarPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            mPrefs.enableTransparentNavBar().set((Boolean) newValue);
            return true;
        });

        mUiLessAnimPreference = (SwitchPreference) findPreference("preference_ui_less_anim");
        mUiLessAnimPreference.setChecked(mPrefs.lessAnim().get());
        mUiLessAnimPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            mPrefs.lessAnim().set((Boolean) newValue);
            return true;
        });
    }

    private void initDevOps() {
        SwitchPreference forceChinesePreference =
                (SwitchPreference) findPreference("preference_switch_force_chinese");
        forceChinesePreference.setChecked(mPrefs.language().get().equals("zh"));
        forceChinesePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    mPrefs.forceChinese(true);
                } else {
                    mPrefs.forceChinese(false);
                }
                restartApp();
                return true;
            }
        });

        SwitchPreference hideItselfPreference =
                (SwitchPreference) findPreference("preference_switch_hide_itself");
        hideItselfPreference.setChecked(mPrefs.hideItself().getValue());
        hideItselfPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PackageManager pm = getContext().getPackageManager();
                ComponentName componentName =
                        new ComponentName(getContext(), MainActivity.class);
                if ((Boolean) newValue) {
                    mPrefs.hideItself().setValue(true);
                    pm.setComponentEnabledSetting(componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                } else {
                    mPrefs.hideItself().setValue(false);
                    pm.setComponentEnabledSetting(componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                }
                return true;
            }
        });
    }

    private void initAbout() {
        mAboutAppPreference = findPreference("preference_about_app");
        mAboutAppPreference.setOnPreferenceClickListener(preference -> {
            if (!mPrefs.devOptionsOpened().getValue()) {
                AboutDialog.show(getActivity(), new OnMultiTouchListener(5, () -> {
                    Toast.makeText(getContext(), "Dev-options is opened", Toast.LENGTH_SHORT)
                            .show();
                    mPreferenceScreen.addPreference(mDevPreferenceGroup);
                    initDevOps();
                    mPrefs.devOptionsOpened().setValue(true);
                }));
            } else {
                AboutDialog.show(getActivity(), null);
            }
            return true;
        });

        mAboutLibPreference = findPreference("preference_about_lib");
        mAboutLibPreference.setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).addFragment(new OpenSourceLibFragment());
            return true;
        });
    }

    private void updateCache(Observable<Long> observable) {
        observable.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long longSize) {
                        double size = Double.valueOf(longSize);
                        String unit = "KB";
                        size /= 1024;
                        if (size > 1024) {
                            unit = "MB";
                            size /= 1024;
                        }
                        mGeneralClearCachePreference
                                .setSummary(String.format("%.2f %s", size, unit));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        Timber.e(throwable);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(ResourceUtil.getString(getContext(), R.string.nav_item_settings));
    }

    private void restartApp() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            getActivity().finish();
            final Intent intent = new Intent(getContext(), MainActivity.class);
            getActivity().startActivity(intent.addFlags(FLAG_ACTIVITY_NO_ANIMATION)
                    .addFlags(FLAG_ACTIVITY_CLEAR_TOP)
            );
            getActivity().finish();
            getActivity().overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        }, 100);
    }
}