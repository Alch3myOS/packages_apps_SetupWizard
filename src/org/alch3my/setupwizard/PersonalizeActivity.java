/*
 * SPDX-FileCopyrightText: 2026 Alch3myOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.alch3my.setupwizard;

import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.internal.util.alch3my.ThemeUtils;

import lineageos.providers.LineageSettings;

import org.json.JSONException;
import org.json.JSONObject;
import org.alch3my.setupwizard.widget.ChoiceGroup;
import org.alch3my.setupwizard.widget.M3;

import java.io.IOException;
import java.util.List;

/**
 * "Make it yours": the looks and gestures people change first, on one page, each applied as
 * soon as it's tapped. Everything here is also in Settings and ArcaniumUI, under the same keys.
 */
public class PersonalizeActivity extends BaseSetupWizardActivity {

    private static final String TAG = "PersonalizeActivity";

    // Colours (Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, as Wallpaper & style writes)
    private static final String KEY_PALETTE = "android.theme.customization.system_palette";
    private static final String KEY_ACCENT = "android.theme.customization.accent_color";
    private static final String KEY_SOURCE = "android.theme.customization.color_source";
    private static final String KEY_STYLE = "android.theme.customization.theme_style";
    private static final String KEY_INDEX = "android.theme.customization.color_index";
    private static final String KEY_TIMESTAMP = "_applied_timestamp";
    private static final int[] PRESETS = {
        0, 0xFF6F43C0, 0xFF0B7A75, 0xFFC2410C, 0xFF1D4ED8, 0xFFE11D48, 0xFF047857,
        0xFFB45309, 0xFF334155,
    };
    private static final int[] PRESET_NAMES = {
        R.string.personalize_colour_wallpaper, R.string.personalize_colour_amethyst,
        R.string.personalize_colour_lagoon, R.string.personalize_colour_ember,
        R.string.personalize_colour_cobalt, R.string.personalize_colour_rose,
        R.string.personalize_colour_emerald, R.string.personalize_colour_gold,
        R.string.personalize_colour_slate,
    };

    // Settings style and the dashboard banner (read by Settings and ArcaniumUI)
    private static final String KEY_SETTINGS_STYLE = "alch3my_settings_style";
    private static final String KEY_SHOW_BANNER = "alch3my_arcanium_show_banner";

    // Status bar, as SystemUI reads them
    private static final int[] BATTERY_STYLES = {0, 1, 3, 2};
    private static final int[] PERCENT_MODES = {0, 1, 2};
    private static final int[] CLOCK_POSITIONS = {2, 1, 0};

    // Back gesture sensitivity, as Settings > Gesture navigation offers it
    private static final float[] BACK_SCALES = {0.6f, 1.0f, 1.33f, 1.66f};

    // Quick settings icon shapes (Settings.System.QS_TILE_ICON_SHAPE)
    private static final String[] TILE_SHAPES = {
        "circle", "squircle", "rounded_rect", "square", "four_sided_cookie", "hexagon",
        "flower", "pebble",
    };
    private static final int[] TILE_SHAPE_NAMES = {
        R.string.personalize_shape_circle, R.string.personalize_shape_squircle,
        R.string.personalize_shape_rounded, R.string.personalize_shape_square,
        R.string.personalize_shape_cookie, R.string.personalize_shape_hexagon,
        R.string.personalize_shape_flower, R.string.personalize_shape_pebble,
    };

    // Updater: how often it checks for updates (its own setting, set through a broadcast)
    static final String ACTION_UPDATER_CHECK_INTERVAL =
            "org.lineageos.updater.action.SET_AUTO_UPDATES_CHECK_INTERVAL";
    static final String EXTRA_UPDATER_INTERVAL = "interval";
    private static final int[] UPDATE_INTERVALS = {1, 2, 3, 0};

    private LinearLayout mSections;
    private ContentResolver mResolver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.personalize_summary));
        mResolver = getContentResolver();
        mSections = findViewById(R.id.personalize_sections);
        build();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_personalize;
    }

    @Override
    protected int getTitleResId() {
        return R.string.personalize_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_theme;
    }

    private void build() {
        mSections.removeAllViews();
        addColours();
        addWallpaper();
        addSettingsStyle();
        addStatusBar();
        addGestures();
        addLockScreen();
        addFontAndShape();
        addUpdates();
        for (int i = 0; i < mSections.getChildCount(); i++) {
            M3.riseIn(mSections.getChildAt(i), 60L * i);
        }
    }

    // ---- Sections ----

    private void addColours() {
        LinearLayout card = section(R.string.personalize_colours_title,
                R.string.personalize_colours_summary);
        ChoiceGroup chips = new ChoiceGroup(this, ChoiceGroup.CHIPS);
        for (int i = 0; i < PRESETS.length; i++) {
            chips.add(getString(PRESET_NAMES[i]), null, i == 0
                    ? ChoiceGroup.swatch(M3.primary(this), M3.tonal(this))
                    : ChoiceGroup.swatch(PRESETS[i], lighten(PRESETS[i])));
        }
        chips.setSelected(currentPreset());
        chips.setOnChoiceListener(this::applyColour);
        card.addView(chips, content());
    }

    private void addWallpaper() {
        LinearLayout card = section(R.string.personalize_wallpaper_title,
                R.string.personalize_wallpaper_summary);
        ChoiceGroup tiles = new ChoiceGroup(this, ChoiceGroup.TILES);
        GradientDrawable ours = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF2A1B4D, 0xFF6F43C0, 0xFF6AD7E5});
        ours.setShape(GradientDrawable.OVAL);
        tiles.add(getString(R.string.personalize_wallpaper_alch3my), null, ours);
        tiles.add(getString(R.string.personalize_wallpaper_more),
                getDrawable(R.drawable.ic_features), null);
        tiles.setOnChoiceListener(index -> {
            if (index == 0) {
                try {
                    WallpaperManager.getInstance(this).clear(
                            WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                } catch (IOException | RuntimeException e) {
                    Log.w(TAG, "Couldn't reset the wallpaper", e);
                }
            } else {
                tiles.setSelected(-1);
                try {
                    startActivity(Intent.createChooser(
                            new Intent(Intent.ACTION_SET_WALLPAPER),
                            getString(R.string.personalize_wallpaper_more)));
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "No wallpaper picker", e);
                }
            }
        });
        card.addView(tiles, content());
    }

    private void addSettingsStyle() {
        LinearLayout card = section(R.string.personalize_settings_style_title,
                R.string.personalize_settings_style_summary);
        ChoiceGroup tiles = new ChoiceGroup(this, ChoiceGroup.TILES);
        int p = M3.primary(this);
        int t = M3.tonal(this);
        tiles.add(getString(R.string.personalize_style_stock), null,
                ChoiceGroup.swatch(M3.surfaceHigh(this), M3.surfaceHigh(this)));
        tiles.add(getString(R.string.personalize_style_minimal), null,
                ChoiceGroup.swatch(M3.outline(this), M3.surface(this)));
        tiles.add(getString(R.string.personalize_style_aurora), null,
                ChoiceGroup.swatch(0xFFF48FB1, 0xFF80DEEA));
        tiles.add(getString(R.string.personalize_style_alch3mist), null,
                ChoiceGroup.swatch(p, t));
        tiles.setSelected(clamp(Settings.System.getInt(mResolver, KEY_SETTINGS_STYLE, 0), 0, 3));
        tiles.setOnChoiceListener(index ->
                Settings.System.putInt(mResolver, KEY_SETTINGS_STYLE, index));
        card.addView(tiles, content());

        card.addView(switchRow(R.string.personalize_banner_title,
                R.string.personalize_banner_summary,
                Settings.System.getInt(mResolver, KEY_SHOW_BANNER, 1) != 0,
                on -> Settings.System.putInt(mResolver, KEY_SHOW_BANNER, on ? 1 : 0)));
    }

    private void addStatusBar() {
        LinearLayout card = section(R.string.personalize_status_bar_title, 0);
        label(card, R.string.personalize_battery_style);
        card.addView(chips(new int[] {R.string.personalize_battery_icon,
                R.string.personalize_battery_circle, R.string.personalize_battery_dotted,
                R.string.personalize_battery_text}, indexOf(BATTERY_STYLES,
                lineageInt(LineageSettings.System.STATUS_BAR_BATTERY_STYLE, 0)),
                i -> putLineage(LineageSettings.System.STATUS_BAR_BATTERY_STYLE,
                        BATTERY_STYLES[i])), content());
        label(card, R.string.personalize_battery_percent);
        card.addView(chips(new int[] {R.string.personalize_percent_off,
                R.string.personalize_percent_inside, R.string.personalize_percent_next_to},
                indexOf(PERCENT_MODES,
                        lineageInt(LineageSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0)),
                i -> putLineage(LineageSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                        PERCENT_MODES[i])), content());
        label(card, R.string.personalize_clock_position);
        card.addView(chips(new int[] {R.string.personalize_clock_left,
                R.string.personalize_clock_centre, R.string.personalize_clock_right},
                indexOf(CLOCK_POSITIONS, lineageInt(LineageSettings.System.STATUS_BAR_CLOCK, 2)),
                i -> putLineage(LineageSettings.System.STATUS_BAR_CLOCK, CLOCK_POSITIONS[i])),
                content());
    }

    private void addGestures() {
        LinearLayout card = section(R.string.personalize_gestures_title, 0);
        boolean gestural = getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode) == 2;
        if (gestural) {
            label(card, R.string.personalize_back_sensitivity);
            float scale = Settings.Secure.getFloatForUser(mResolver,
                    Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT, 1.0f, UserHandle.USER_CURRENT);
            int current = 1;
            for (int i = 0; i < BACK_SCALES.length; i++) {
                if (Math.abs(BACK_SCALES[i] - scale) < 0.01f) {
                    current = i;
                }
            }
            card.addView(chips(new int[] {R.string.personalize_back_low,
                    R.string.personalize_back_default, R.string.personalize_back_high,
                    R.string.personalize_back_highest}, current, i -> {
                        Settings.Secure.putFloatForUser(mResolver,
                                Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT, BACK_SCALES[i],
                                UserHandle.USER_CURRENT);
                        Settings.Secure.putFloatForUser(mResolver,
                                Settings.Secure.BACK_GESTURE_INSET_SCALE_RIGHT, BACK_SCALES[i],
                                UserHandle.USER_CURRENT);
                    }), content());
            card.addView(switchRow(R.string.personalize_gesture_bar_title,
                    R.string.personalize_gesture_bar_summary,
                    lineageInt(LineageSettings.System.NAVIGATION_BAR_HINT, 1) != 0,
                    on -> putLineage(LineageSettings.System.NAVIGATION_BAR_HINT, on ? 1 : 0)));
            boolean assistDefault = getResources().getBoolean(
                    com.android.internal.R.bool.config_assistTouchGestureEnabledDefault);
            card.addView(switchRow(R.string.personalize_assistant_title,
                    R.string.personalize_assistant_summary,
                    Settings.Secure.getIntForUser(mResolver,
                            Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, assistDefault ? 1 : 0,
                            UserHandle.USER_CURRENT) != 0,
                    on -> Settings.Secure.putIntForUser(mResolver,
                            Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, on ? 1 : 0,
                            UserHandle.USER_CURRENT)));
        }
        card.addView(switchRow(R.string.personalize_dt2s_title,
                R.string.personalize_dt2s_summary,
                lineageInt(LineageSettings.System.DOUBLE_TAP_SLEEP_GESTURE, 1) != 0,
                on -> putLineage(LineageSettings.System.DOUBLE_TAP_SLEEP_GESTURE, on ? 1 : 0)));
        if (getResources().getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake)) {
            card.addView(switchRow(R.string.personalize_dt2w_title,
                    R.string.personalize_dt2w_summary,
                    Settings.Secure.getIntForUser(mResolver, Settings.Secure.DOUBLE_TAP_TO_WAKE,
                            0, UserHandle.USER_CURRENT) != 0,
                    on -> Settings.Secure.putIntForUser(mResolver,
                            Settings.Secure.DOUBLE_TAP_TO_WAKE, on ? 1 : 0,
                            UserHandle.USER_CURRENT)));
        }
    }

    private void addLockScreen() {
        LinearLayout card = section(R.string.personalize_lock_screen_title, 0);
        label(card, R.string.personalize_clock_size);
        ChoiceGroup tiles = new ChoiceGroup(this, ChoiceGroup.TILES);
        tiles.add(getString(R.string.personalize_clock_big), null, null);
        tiles.add(getString(R.string.personalize_clock_small), null, null);
        tiles.setSelected(Settings.Secure.getIntForUser(mResolver,
                Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK, 1, UserHandle.USER_CURRENT)
                != 0 ? 0 : 1);
        tiles.setOnChoiceListener(i -> Settings.Secure.putIntForUser(mResolver,
                Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK, i == 0 ? 1 : 0,
                UserHandle.USER_CURRENT));
        card.addView(tiles, content());
        boolean aodDefault = getResources().getBoolean(
                com.android.internal.R.bool.config_dozeAlwaysOnEnabled);
        card.addView(switchRow(R.string.personalize_aod_title, R.string.personalize_aod_summary,
                Settings.Secure.getIntForUser(mResolver, Settings.Secure.DOZE_ALWAYS_ON,
                        aodDefault ? 1 : 0, UserHandle.USER_CURRENT) != 0,
                on -> Settings.Secure.putIntForUser(mResolver, Settings.Secure.DOZE_ALWAYS_ON,
                        on ? 1 : 0, UserHandle.USER_CURRENT)));
    }

    private void addFontAndShape() {
        LinearLayout card = section(R.string.personalize_font_title, 0);
        try {
            ThemeUtils themes = ThemeUtils.getInstance(this);
            List<String> fonts = themes.getOverlayPackagesForCategory(ThemeUtils.FONT_KEY);
            if (fonts.size() > 1) {
                label(card, R.string.personalize_font);
                ChoiceGroup chips = new ChoiceGroup(this, ChoiceGroup.CHIPS);
                String current = "android";
                for (android.content.om.OverlayInfo info
                        : themes.getOverlayInfos(ThemeUtils.FONT_KEY)) {
                    if (info.isEnabled()) {
                        current = info.getPackageName();
                    }
                }
                for (String pkg : fonts) {
                    chips.add("android".equals(pkg) ? getString(R.string.personalize_font_default)
                            : label(pkg), null, null);
                }
                chips.setSelected(Math.max(0, fonts.indexOf(current)));
                chips.setOnChoiceListener(i ->
                        themes.setOverlayEnabled(ThemeUtils.FONT_KEY, fonts.get(i), "android"));
                card.addView(chips, content());
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Couldn't list fonts", e);
        }
        label(card, R.string.personalize_tile_shape);
        String shape = Settings.System.getStringForUser(mResolver,
                Settings.System.QS_TILE_ICON_SHAPE, UserHandle.USER_CURRENT);
        int current = 0;
        for (int i = 0; i < TILE_SHAPES.length; i++) {
            if (TILE_SHAPES[i].equals(shape)) {
                current = i;
            }
        }
        card.addView(chips(TILE_SHAPE_NAMES, current, i ->
                Settings.System.putStringForUser(mResolver, Settings.System.QS_TILE_ICON_SHAPE,
                        TILE_SHAPES[i], UserHandle.USER_CURRENT)), content());
    }

    private void addUpdates() {
        LinearLayout card = section(R.string.personalize_updates_title,
                R.string.personalize_updates_summary);
        // Updater keeps this in its own preferences; it starts at weekly.
        card.addView(chips(new int[] {R.string.personalize_updates_daily,
                R.string.personalize_updates_weekly, R.string.personalize_updates_monthly,
                R.string.personalize_updates_never}, 1, i -> {
                    Intent intent = new Intent(ACTION_UPDATER_CHECK_INTERVAL)
                            .setPackage("org.lineageos.updater")
                            .putExtra(EXTRA_UPDATER_INTERVAL, UPDATE_INTERVALS[i])
                            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND
                                    | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    sendBroadcast(intent);
                }), content());
    }

    // ---- Colours ----

    private int currentPreset() {
        JSONObject o = readThemeJson();
        if (!"preset".equals(o.optString(KEY_SOURCE))) {
            return 0;
        }
        String palette = o.optString(KEY_PALETTE).replace("#", "");
        for (int i = 1; i < PRESETS.length; i++) {
            if (String.format("%06X", PRESETS[i] & 0xFFFFFF).equalsIgnoreCase(palette)) {
                return i;
            }
        }
        return -1;
    }

    private void applyColour(int index) {
        JSONObject o = readThemeJson();
        try {
            o.remove(KEY_PALETTE);
            o.remove(KEY_ACCENT);
            o.remove(KEY_INDEX);
            if (index == 0) {
                o.put(KEY_SOURCE, "home_wallpaper");
            } else {
                String hex = String.format("%06X", PRESETS[index] & 0xFFFFFF);
                o.put(KEY_PALETTE, hex);
                o.put(KEY_ACCENT, hex);
                o.put(KEY_SOURCE, "preset");
                o.put(KEY_STYLE, "TONAL_SPOT");
            }
            o.put(KEY_TIMESTAMP, System.currentTimeMillis());
        } catch (JSONException e) {
            Log.w(TAG, "Couldn't set the colours", e);
            return;
        }
        Settings.Secure.putStringForUser(mResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, o.toString(),
                UserHandle.USER_CURRENT);
    }

    private JSONObject readThemeJson() {
        String json = Settings.Secure.getStringForUser(mResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, UserHandle.USER_CURRENT);
        try {
            return TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static int lighten(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += (255 - r) * 55 / 100;
        g += (255 - g) * 55 / 100;
        b += (255 - b) * 55 / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ---- Building blocks ----

    private interface OnToggle {
        void onToggle(boolean on);
    }

    /** A card with a title and an optional line under it; content is added to it. */
    private LinearLayout section(int title, int summary) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = M3.dp(this, 20);
        card.setPadding(pad, pad, pad, M3.dp(this, 16));
        card.setBackground(M3.rounded(M3.surface(this), M3.dp(this, 28)));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTypeface(M3.font(600));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTextColor(M3.onSurface(this));
        card.addView(t);
        if (summary != 0) {
            TextView s = new TextView(this);
            s.setText(summary);
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            s.setTextColor(M3.onSurfaceVariant(this));
            s.setPadding(0, M3.dp(this, 2), 0, 0);
            card.addView(s);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = M3.dp(this, 12);
        mSections.addView(card, lp);
        return card;
    }

    private LinearLayout.LayoutParams content() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = M3.dp(this, 12);
        return lp;
    }

    private void label(LinearLayout card, int text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTypeface(M3.font(600));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTextColor(M3.primary(this));
        t.setPadding(0, M3.dp(this, 14), 0, 0);
        card.addView(t);
    }

    private ChoiceGroup chips(int[] names, int selected, ChoiceGroup.OnChoiceListener listener) {
        ChoiceGroup chips = new ChoiceGroup(this, ChoiceGroup.CHIPS);
        for (int name : names) {
            chips.add(getString(name), null, null);
        }
        chips.setSelected(selected);
        chips.setOnChoiceListener(listener);
        return chips;
    }

    private View switchRow(int title, int summary, boolean on, OnToggle listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, M3.dp(this, 14), 0, 0);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setTextColor(M3.onSurface(this));
        texts.addView(t);
        TextView s = new TextView(this);
        s.setText(summary);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        s.setTextColor(M3.onSurfaceVariant(this));
        texts.addView(s);
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(on);
        toggle.setContentDescription(getString(title));
        toggle.setOnCheckedChangeListener((b, checked) -> listener.onToggle(checked));
        row.addView(toggle);
        row.setOnClickListener(v -> toggle.toggle());
        return row;
    }

    private int lineageInt(String key, int def) {
        return LineageSettings.System.getIntForUser(mResolver, key, def, UserHandle.USER_CURRENT);
    }

    private void putLineage(String key, int value) {
        LineageSettings.System.putIntForUser(mResolver, key, value, UserHandle.USER_CURRENT);
    }

    private CharSequence label(String pkg) {
        PackageManager pm = getPackageManager();
        try {
            return pm.getApplicationInfo(pkg, 0).loadLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
