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
    private View mClockPreviewDot;
    private View mSbBattery;
    private TextView mSbPercent;
    private View mShapePreview;
    private TextView mClockSizePreview;
    private View mBackPreviewLeft;
    private View mBackPreviewRight;
    private android.widget.FrameLayout mStyleMockWrap;
    private TextView mFontSample;

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
        addTheme();
        addWallpaper();
        addSettingsStyle();
        addStatusBar();
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
        int styleSel = clamp(Settings.System.getInt(mResolver, KEY_SETTINGS_STYLE, 0), 0, 3);
        // Preview: a little mock settings page drawn in the chosen style, updated as tiles are
        // tapped, so each style is shown on the page itself.
        mStyleMockWrap = new android.widget.FrameLayout(this);
        mStyleMockWrap.addView(buildStyleMock(styleSel));
        card.addView(mStyleMockWrap, barLp(104));
        tiles.setSelected(styleSel);
        tiles.setOnChoiceListener(index -> {
            Settings.System.putInt(mResolver, KEY_SETTINGS_STYLE, index);
            if (mStyleMockWrap != null) {
                mStyleMockWrap.removeAllViews();
                mStyleMockWrap.addView(buildStyleMock(index));
            }
        });
        card.addView(tiles, content());

        card.addView(switchRow(R.string.personalize_banner_title,
                R.string.personalize_banner_summary,
                Settings.System.getInt(mResolver, KEY_SHOW_BANNER, 1) != 0,
                on -> Settings.System.putInt(mResolver, KEY_SHOW_BANNER, on ? 1 : 0)));
    }

    private void addStatusBar() {
        LinearLayout card = section(R.string.personalize_status_bar_title, 0);
        // One live mock status bar for the whole section: the battery (and its percent) sit at
        // the end and the clock hops left/centre/right - all three update as the chips are tapped,
        // so each choice is visible on the page itself.
        card.addView(statusBarPreview(
                lineageInt(LineageSettings.System.STATUS_BAR_CLOCK, 2),
                lineageInt(LineageSettings.System.STATUS_BAR_BATTERY_STYLE, 0),
                lineageInt(LineageSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0)),
                barLp(28));
        label(card, R.string.personalize_battery_style);
        card.addView(chips(new int[] {R.string.personalize_battery_icon,
                R.string.personalize_battery_circle, R.string.personalize_battery_dotted,
                R.string.personalize_battery_text}, indexOf(BATTERY_STYLES,
                lineageInt(LineageSettings.System.STATUS_BAR_BATTERY_STYLE, 0)),
                i -> {
                    putLineage(LineageSettings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLES[i]);
                    updateBatteryPreview(BATTERY_STYLES[i]);
                }), content());
        label(card, R.string.personalize_battery_percent);
        card.addView(chips(new int[] {R.string.personalize_percent_off,
                R.string.personalize_percent_inside, R.string.personalize_percent_next_to},
                indexOf(PERCENT_MODES,
                        lineageInt(LineageSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0)),
                i -> {
                    putLineage(LineageSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                            PERCENT_MODES[i]);
                    updatePercentPreview(PERCENT_MODES[i]);
                }), content());
    }

    /** A mock status bar strip: a clock marker placed per STATUS_BAR_CLOCK (0 right, 1 centre,
     *  2 left, matching SystemUI), and a battery glyph with its percent at the end. */
    private View statusBarPreview(int clockPos, int batteryStyle, int percentMode) {
        int m = M3.dp(this, 10);
        android.widget.FrameLayout bar = new android.widget.FrameLayout(this);
        bar.setBackground(M3.rounded(M3.surfaceHigh(this), M3.dp(this, 10)));

        // End group: percent text then the battery glyph, pinned to the end.
        LinearLayout end = new LinearLayout(this);
        end.setOrientation(LinearLayout.HORIZONTAL);
        end.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView percent = new TextView(this);
        percent.setText("85%");
        percent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        percent.setTypeface(M3.font(600));
        percent.setTextColor(M3.onSurface(this));
        percent.setPaddingRelative(0, 0, M3.dp(this, 4), 0);
        end.addView(percent);
        View battery = new View(this);
        end.addView(battery, new LinearLayout.LayoutParams(M3.dp(this, 22), M3.dp(this, 11)));
        android.widget.FrameLayout.LayoutParams elp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END;
        elp.setMarginEnd(m);
        bar.addView(end, elp);
        mSbBattery = battery;
        mSbPercent = percent;

        // Clock marker.
        View clock = new View(this);
        clock.setBackground(M3.rounded(M3.primary(this), M3.dp(this, 3)));
        android.widget.FrameLayout.LayoutParams clp = new android.widget.FrameLayout.LayoutParams(
                M3.dp(this, 30), M3.dp(this, 10));
        clp.gravity = android.view.Gravity.CENTER_VERTICAL | clockGravity(clockPos);
        clp.setMarginStart(m);
        clp.setMarginEnd(clockPos == 0 ? M3.dp(this, 56) : m);
        bar.addView(clock, clp);
        mClockPreviewDot = clock;

        updateBatteryPreview(batteryStyle);
        updatePercentPreview(percentMode);
        return bar;
    }

    private void updateClockPreview(int position) {
        if (mClockPreviewDot == null) {
            return;
        }
        android.widget.FrameLayout.LayoutParams lp =
                (android.widget.FrameLayout.LayoutParams) mClockPreviewDot.getLayoutParams();
        lp.gravity = android.view.Gravity.CENTER_VERTICAL | clockGravity(position);
        // When the clock sits at the end, keep it clear of the battery group.
        lp.setMarginEnd(position == 0 ? M3.dp(this, 56) : M3.dp(this, 10));
        mClockPreviewDot.setLayoutParams(lp);
    }

    /** Draw the battery glyph for STATUS_BAR_BATTERY_STYLE (0 icon, 1 circle, 3 dotted, 2 text). */
    private void updateBatteryPreview(int style) {
        if (mSbBattery == null) {
            return;
        }
        int accent = M3.primary(this);
        int w = M3.dp(this, 2);
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable();
        boolean round = style == 1 || style == 3;
        if (style == 1) {                       // circle: a ring
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(0x00000000);
            g.setStroke(w, accent);
        } else if (style == 3) {                // dotted: a dashed ring
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(0x00000000);
            g.setStroke(w, accent, M3.dp(this, 2), M3.dp(this, 2));
        } else if (style == 2) {                // text: the percent is the battery
            g.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            g.setColor(0x00000000);
        } else {                                // icon: a filled battery
            g.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            g.setCornerRadius(M3.dp(this, 2));
            g.setColor(accent);
        }
        mSbBattery.setBackground(g);
        ViewGroup.LayoutParams lp = mSbBattery.getLayoutParams();
        lp.width = round ? M3.dp(this, 13) : M3.dp(this, 22);
        lp.height = round ? M3.dp(this, 13) : M3.dp(this, 11);
        mSbBattery.setLayoutParams(lp);
        mSbBattery.setVisibility(style == 2 ? View.GONE : View.VISIBLE);
        if (mSbPercent != null) {
            // In text style the percent IS the battery, so it is always shown.
            mSbPercent.setPaddingRelative(0, 0, style == 2 ? 0 : M3.dp(this, 4), 0);
        }
    }

    /** Show the percent per STATUS_BAR_SHOW_BATTERY_PERCENT (0 off, 1 inside, 2 next to). */
    private void updatePercentPreview(int mode) {
        if (mSbPercent == null) {
            return;
        }
        boolean batteryIsText = mSbBattery != null && mSbBattery.getVisibility() == View.GONE;
        mSbPercent.setVisibility(mode != 0 || batteryIsText ? View.VISIBLE : View.GONE);
    }

    private static int clockGravity(int position) {
        switch (position) {
            case 1:
                return android.view.Gravity.CENTER_HORIZONTAL;
            case 0:
                return android.view.Gravity.END;
            default:
                return android.view.Gravity.START;
        }
    }

    private LinearLayout.LayoutParams barLp(int heightDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, M3.dp(this, heightDp));
        lp.topMargin = M3.dp(this, 10);
        return lp;
    }

    private void applyClockSizePreview(boolean big) {
        if (mClockSizePreview == null) {
            return;
        }
        if (big) {
            mClockSizePreview.setText("12\n30");
            mClockSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            mClockSizePreview.setLineSpacing(0f, 0.95f);
        } else {
            mClockSizePreview.setText("12:30");
            mClockSizePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            mClockSizePreview.setLineSpacing(0f, 1f);
        }
    }

    /** A mock QS tile filled with the shape for {@code key}, using SystemUI's own shape paths. */
    private android.graphics.drawable.Drawable shapeDrawable(String key) {
        String data = SHAPE_PATHS.get(key);
        if (data == null) {
            data = SHAPE_PATHS.get("circle");
        }
        android.graphics.Path path;
        try {
            path = android.util.PathParser.createPathFromPathData(data);
        } catch (RuntimeException e) {
            path = android.util.PathParser.createPathFromPathData(SHAPE_PATHS.get("circle"));
        }
        android.graphics.drawable.ShapeDrawable d = new android.graphics.drawable.ShapeDrawable(
                new android.graphics.drawable.shapes.PathShape(path, 100f, 100f));
        d.getPaint().setColor(M3.primary(this));
        d.getPaint().setStyle(android.graphics.Paint.Style.FILL);
        d.getPaint().setAntiAlias(true);
        return d;
    }

    private int backWidth(float scale) {
        return Math.round(M3.dp(this, 12) * scale);
    }

    /** A mock screen with the back-gesture reach shaded along both edges. */
    private View backPreview(float scale) {
        android.widget.FrameLayout screen = new android.widget.FrameLayout(this);
        screen.setBackground(M3.rounded(M3.surfaceHigh(this), M3.dp(this, 10)));
        int strip = (M3.primary(this) & 0x00FFFFFF) | 0x55000000;
        mBackPreviewLeft = new View(this);
        mBackPreviewLeft.setBackgroundColor(strip);
        android.widget.FrameLayout.LayoutParams l = new android.widget.FrameLayout.LayoutParams(
                backWidth(scale), ViewGroup.LayoutParams.MATCH_PARENT);
        l.gravity = android.view.Gravity.START;
        screen.addView(mBackPreviewLeft, l);
        mBackPreviewRight = new View(this);
        mBackPreviewRight.setBackgroundColor(strip);
        android.widget.FrameLayout.LayoutParams r = new android.widget.FrameLayout.LayoutParams(
                backWidth(scale), ViewGroup.LayoutParams.MATCH_PARENT);
        r.gravity = android.view.Gravity.END;
        screen.addView(mBackPreviewRight, r);
        return screen;
    }

    private void updateBackPreview(float scale) {
        for (View v : new View[] {mBackPreviewLeft, mBackPreviewRight}) {
            if (v == null) {
                continue;
            }
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.width = backWidth(scale);
            v.setLayoutParams(lp);
        }
    }

    // The QS tile icon shapes, the same 100x100 paths SystemUI/ArcaniumUI use, for the preview.
    private static final java.util.Map<String, String> SHAPE_PATHS = new java.util.HashMap<>();
    static {
        SHAPE_PATHS.put("circle", "M50 0A50 50,0,1,1,50 100A50 50,0,1,1,50 0");
        SHAPE_PATHS.put("square", "M53.689 0.82 L53.689 .82 C67.434 .82 74.306 .82 79.758 2.978 "
                + "87.649 6.103 93.897 12.351 97.022 20.242 99.18 25.694 99.18 32.566 "
                + "99.18 46.311 V53.689 C99.18 67.434 99.18 74.306 97.022 79.758 "
                + "93.897 87.649 87.649 93.897 79.758 97.022 74.306 99.18 67.434 99.18 "
                + "53.689 99.18 H46.311 C32.566 99.18 25.694 99.18 20.242 97.022 "
                + "12.351 93.897 6.103 87.649 2.978 79.758 .82 74.306 .82 67.434 "
                + ".82 53.689 L.82 46.311 C.82 32.566 .82 25.694 2.978 20.242 "
                + "6.103 12.351 12.351 6.103 20.242 2.978 25.694 .82 32.566 .82 "
                + "46.311 .82Z");
        SHAPE_PATHS.put("rounded_rect", "M50,0L88,0 C94.4,0 100,5.4 100 12 L100,88 "
                + "C100,94.6 94.6 100 88 100 L12,100 C5.4,100 0,94.6 0,88 L0 12 "
                + "C0 5.4 5.4 0 12 0 L50,0 Z");
        SHAPE_PATHS.put("squircle", "M41.8,0h16.4C72.1,0 77.6,1.5 83,4.4c5.4,2.9 9.7,7.2 12.6,12.6"
                + "c2.9,5.4 4.4,11 4.4,24.7v16.4c0,13.7 -1.5,19.3 -4.4,24.7"
                + "c-2.9,5.4 -7.2,9.7 -12.6,12.6c-5.4,2.9 -11,4.4 -24.7,4.4H41.8"
                + "c-13.8,0 -19.3,-1.5 -24.7,-4.4c-5.4,-2.9 -9.7,-7.1 -12.7,-12.5"
                + "S0,72 0,58.3V41.9c0,-13.8 1.5,-19.3 4.4,-24.7S11.6,7.4 17,4.5"
                + "S28.1,0 41.8,0z");
        SHAPE_PATHS.put("four_sided_cookie", "M39.888,4.517C46.338 7.319 53.662 7.319 60.112 "
                + "4.517L63.605 3C84.733 -6.176 106.176 15.268 97 36.395L95.483 39.888"
                + "C92.681 46.338 92.681 53.662 95.483 60.112L97 63.605"
                + "C106.176 84.732 84.733 106.176 63.605 97L60.112 95.483"
                + "C53.662 92.681 46.338 92.681 39.888 95.483L36.395 97"
                + "C15.267 106.176 -6.176 84.732 3 63.605L4.517 60.112"
                + "C7.319 53.662 7.319 46.338 4.517 39.888L3 36.395"
                + "C -6.176 15.268 15.267 -6.176 36.395 3Z");
        SHAPE_PATHS.put("hexagon", "M12,0 88,0 100,50 88,100 12,100 0,50 12,0 Z");
        SHAPE_PATHS.put("flower", "M50,0 C60.6,0 69.9,5.3 75.6,13.5 78.5,17.8 82.3,21.5 86.6,24.5 "
                + "94.7,30.1 100,39.4 100,50 100,60.6 94.7,69.9 86.5,75.6 "
                + "82.2,78.5 78.5,82.3 75.5,86.6 69.9,94.7 60.6,100 50,100 "
                + "39.4,100 30.1,94.7 24.4,86.5 21.5,82.2 17.7,78.5 13.4,75.5 "
                + "5.3,69.9 0,60.6 0,50 0,39.4 5.3,30.1 13.5,24.4 "
                + "17.8,21.5 21.5,17.7 24.5,13.4 30.1,5.3 39.4,0 50,0 Z");
        SHAPE_PATHS.put("pebble", "M55,0 C25,0 0,25 0,50 0,78 28,100 55,100 85,100 100,85 100,58 "
                + "100,30 86,0 55,0 Z");
    }

    private void addLockScreen() {
        LinearLayout card = section(R.string.personalize_lock_screen_title, 0);
        label(card, R.string.personalize_clock_size);
        boolean doubleLine = Settings.Secure.getIntForUser(mResolver,
                Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK, 1, UserHandle.USER_CURRENT) != 0;
        // Preview: a mock lock-screen clock - stacked and large for Big, one line for Small.
        TextView clockSize = new TextView(this);
        clockSize.setGravity(android.view.Gravity.CENTER);
        clockSize.setTextColor(M3.onSurface(this));
        clockSize.setTypeface(M3.font(600));
        LinearLayout clockWrap = new LinearLayout(this);
        clockWrap.setGravity(android.view.Gravity.CENTER);
        clockWrap.setBackground(M3.rounded(M3.surfaceHigh(this), M3.dp(this, 14)));
        clockWrap.addView(clockSize, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mClockSizePreview = clockSize;
        applyClockSizePreview(doubleLine);
        card.addView(clockWrap, barLp(76));
        ChoiceGroup tiles = new ChoiceGroup(this, ChoiceGroup.TILES);
        tiles.add(getString(R.string.personalize_clock_big), null, null);
        tiles.add(getString(R.string.personalize_clock_small), null, null);
        tiles.setSelected(doubleLine ? 0 : 1);
        tiles.setOnChoiceListener(i -> {
            Settings.Secure.putIntForUser(mResolver,
                    Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK, i == 0 ? 1 : 0,
                    UserHandle.USER_CURRENT);
            applyClockSizePreview(i == 0);
        });
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
                // Preview: a sample word rendered in the chosen font (best-effort - loads the
                // font from the overlay when it ships one, otherwise shows the sample as-is).
                TextView fontSample = new TextView(this);
                fontSample.setText(R.string.personalize_font_sample);
                fontSample.setGravity(android.view.Gravity.CENTER);
                fontSample.setTextColor(M3.onSurface(this));
                fontSample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                LinearLayout fontWrap = new LinearLayout(this);
                fontWrap.setGravity(android.view.Gravity.CENTER);
                fontWrap.setBackground(M3.rounded(M3.surfaceHigh(this), M3.dp(this, 14)));
                fontWrap.addView(fontSample, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                mFontSample = fontSample;
                applyFontPreview(current);
                card.addView(fontWrap, barLp(52));
                chips.setSelected(Math.max(0, fonts.indexOf(current)));
                chips.setOnChoiceListener(i -> {
                    themes.setOverlayEnabled(ThemeUtils.FONT_KEY, fonts.get(i), "android");
                    applyFontPreview(fonts.get(i));
                });
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
        // Preview: a mock QS tile drawn in the chosen shape, using the same paths SystemUI uses.
        LinearLayout shapeWrap = new LinearLayout(this);
        shapeWrap.setGravity(android.view.Gravity.CENTER);
        View shapeView = new View(this);
        shapeView.setBackground(shapeDrawable(TILE_SHAPES[current]));
        shapeWrap.addView(shapeView, new LinearLayout.LayoutParams(M3.dp(this, 36), M3.dp(this, 36)));
        mShapePreview = shapeView;
        card.addView(shapeWrap, barLp(52));
        card.addView(chips(TILE_SHAPE_NAMES, current, i -> {
                Settings.System.putStringForUser(mResolver, Settings.System.QS_TILE_ICON_SHAPE,
                        TILE_SHAPES[i], UserHandle.USER_CURRENT);
                if (mShapePreview != null) {
                    mShapePreview.setBackground(shapeDrawable(TILE_SHAPES[i]));
                }
            }), content());
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

    // ---- Theme ----

    private void addTheme() {
        LinearLayout card = section(R.string.personalize_theme_title,
                R.string.personalize_theme_summary);
        android.app.UiModeManager ui = getSystemService(android.app.UiModeManager.class);
        int mode = ui != null ? ui.getNightMode() : android.app.UiModeManager.MODE_NIGHT_AUTO;
        int sel = mode == android.app.UiModeManager.MODE_NIGHT_NO ? 0
                : (mode == android.app.UiModeManager.MODE_NIGHT_YES ? 1 : 2);
        card.addView(chips(new int[] {R.string.personalize_theme_light,
                R.string.personalize_theme_dark, R.string.personalize_theme_auto}, sel, i -> {
                    int m = i == 0 ? android.app.UiModeManager.MODE_NIGHT_NO
                            : (i == 1 ? android.app.UiModeManager.MODE_NIGHT_YES
                            : android.app.UiModeManager.MODE_NIGHT_AUTO);
                    try {
                        if (ui != null) {
                            ui.setNightMode(m);
                        }
                    } catch (RuntimeException e) {
                        Log.w(TAG, "Couldn't set the theme", e);
                    }
                }), content());
    }

    // ---- Settings-style preview ----

    private GradientDrawable rect(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(M3.dp(this, radiusDp));
        return d;
    }

    /** A little mock settings page in the chosen style, so each style shows on the page. */
    private View buildStyleMock(int style) {
        int pad = M3.dp(this, 10);
        LinearLayout mock = new LinearLayout(this);
        mock.setOrientation(LinearLayout.VERTICAL);
        mock.setPadding(pad, pad, pad, pad);
        mock.setBackground(rect(M3.surfaceHigh(this), 14));
        GradientDrawable header;
        switch (style) {
            case 1: // Minimal - flat, outlined, square-ish
                header = rect(M3.surface(this), 4);
                header.setStroke(M3.dp(this, 1), M3.outline(this));
                break;
            case 2: // Aurora - soft gradient
                header = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[] {0xFFF48FB1, 0xFF80DEEA});
                header.setCornerRadius(M3.dp(this, 12));
                break;
            case 3: // Alch3mist - accent gradient, rounded
                header = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[] {M3.primary(this), M3.tonal(this)});
                header.setCornerRadius(M3.dp(this, 16));
                break;
            default: // Stock
                header = rect(M3.primary(this), 8);
        }
        View head = new View(this);
        head.setBackground(header);
        mock.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, M3.dp(this, 20)));
        int rowRadius = style == 1 ? 4 : (style == 0 ? 8 : 16);
        for (int i = 0; i < 2; i++) {
            View row = new View(this);
            GradientDrawable rb = rect(M3.surface(this), rowRadius);
            if (style == 1) {
                rb.setStroke(M3.dp(this, 1), M3.outline(this));
            }
            row.setBackground(rb);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, M3.dp(this, 16));
            rp.topMargin = M3.dp(this, 7);
            mock.addView(row, rp);
        }
        return mock;
    }

    // ---- Font preview ----

    private void applyFontPreview(String pkg) {
        if (mFontSample == null) {
            return;
        }
        android.graphics.Typeface tf = null;
        if (pkg != null && !"android".equals(pkg)) {
            try {
                android.content.res.AssetManager am = createPackageContext(pkg, 0).getAssets();
                String path = findFontAsset(am, "fonts");
                if (path == null) {
                    path = findFontAsset(am, "");
                }
                if (path != null) {
                    tf = new android.graphics.Typeface.Builder(am, path).build();
                }
            } catch (Exception e) {
                Log.w(TAG, "Couldn't load the font preview for " + pkg, e);
            }
        }
        mFontSample.setTypeface(tf != null ? tf : android.graphics.Typeface.DEFAULT);
    }

    private String findFontAsset(android.content.res.AssetManager am, String dir) {
        try {
            String[] list = am.list(dir);
            if (list != null) {
                for (String f : list) {
                    String low = f.toLowerCase(java.util.Locale.ROOT);
                    if (low.endsWith(".ttf") || low.endsWith(".otf")) {
                        return dir.isEmpty() ? f : dir + "/" + f;
                    }
                }
            }
        } catch (IOException e) {
            // no assets in this folder
        }
        return null;
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
