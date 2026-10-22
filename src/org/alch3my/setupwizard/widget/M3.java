/*
 * SPDX-FileCopyrightText: 2026 Alch3myOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.alch3my.setupwizard.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.setupdesign.GlifLayout;

import org.alch3my.setupwizard.R;

/**
 * The setup wizard's Material 3 look, shared with the redesigned power menu: large tonal
 * surfaces with 24-32dp corners, pill buttons, big headlines and springy presses.
 */
public final class M3 {

    private M3() {}

    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int sys(Context context, String light, String dark) {
        return context.getColor(context.getResources().getIdentifier(
                isNight(context) ? dark : light, "color", "android"));
    }

    /** Filled buttons, selected tiles and chips. */
    public static int primary(Context c) {
        return sys(c, "system_accent1_600", "system_accent1_200");
    }

    /** Text and icons on {@link #primary}. */
    public static int onPrimary(Context c) {
        return sys(c, "system_accent1_0", "system_accent1_800");
    }

    /** Tonal buttons and the selected tile's fill. */
    public static int tonal(Context c) {
        return sys(c, "system_accent1_100", "system_accent1_700");
    }

    public static int onTonal(Context c) {
        return sys(c, "system_accent1_900", "system_accent1_100");
    }

    /** Cards and tiles. */
    public static int surface(Context c) {
        return sys(c, "system_neutral2_50", "system_neutral2_900");
    }

    /** Chips and the tracks of switches. */
    public static int surfaceHigh(Context c) {
        return sys(c, "system_neutral2_100", "system_neutral2_800");
    }

    public static int onSurface(Context c) {
        return sys(c, "system_neutral1_900", "system_neutral1_50");
    }

    public static int onSurfaceVariant(Context c) {
        return sys(c, "system_neutral2_700", "system_neutral2_200");
    }

    public static int outline(Context c) {
        return sys(c, "system_neutral2_300", "system_neutral2_600");
    }

    public static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    /** {@code content} with a ripple clipped to the same rounded shape. */
    public static Drawable pressable(Context c, Drawable content, float radiusPx) {
        return new RippleDrawable(ColorStateList.valueOf(
                (onSurface(c) & 0x00FFFFFF) | 0x24000000), content, rounded(0xFFFFFFFF, radiusPx));
    }

    public static Typeface font(int weight) {
        return Typeface.create(Typeface.create("google-sans-flex", Typeface.NORMAL), weight, false);
    }

    /** The page headline: large, left aligned, in the theme font. */
    public static void styleHeader(GlifLayout layout) {
        TextView header = layout.getHeaderTextView();
        if (header == null) {
            return;
        }
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        header.setTypeface(font(500));
        header.setLetterSpacing(-0.01f);
        header.setTextColor(onSurface(layout.getContext()));
        header.setLineSpacing(0, 1.05f);
    }

    /** Next as a filled pill, Skip as a tonal one. */
    public static void stylePrimaryButton(Button button) {
        Context c = button.getContext();
        float r = dp(c, 28);
        button.setBackground(pressable(c, rounded(primary(c), r), r));
        button.setTextColor(onPrimary(c));
        styleButtonText(button);
    }

    public static void styleTonalButton(Button button) {
        Context c = button.getContext();
        float r = dp(c, 28);
        button.setBackground(pressable(c, rounded(tonal(c), r), r));
        button.setTextColor(onTonal(c));
        styleButtonText(button);
    }

    private static void styleButtonText(Button button) {
        Context c = button.getContext();
        button.setAllCaps(false);
        button.setTypeface(font(600));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setMinHeight(dp(c, 56));
        button.setMinimumHeight(dp(c, 56));
        button.setMinWidth(dp(c, 112));
        button.setMinimumWidth(dp(c, 112));
        button.setPadding(dp(c, 28), 0, dp(c, 28), 0);
        button.setStateListAnimator(null);
        button.setElevation(0);
        springOnPress(button);
    }

    /** Shrinks a little while pressed and springs back, like the power menu's tiles. */
    public static void springOnPress(View view) {
        view.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(380)
                            .setInterpolator(new OvershootInterpolator(3f)).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    /**
     * The glif themes, newest first. Looked up by name so a setupdesign without the expressive
     * one still builds and falls back to the next.
     */
    private static final String[] GLIF_THEMES = {
        "SudFullDynamicColorThemeGlifExpressive.DayNight",
        "SudThemeGlifExpressive.DayNight",
        "SudFullDynamicColorThemeGlifV4.DayNight",
    };

    /**
     * Switches the activity to the expressive glif theme Google's setup pages use, with
     * Theme.Setup's own items on top. Call before super.onCreate. Keeps Theme.Setup if the
     * library has none of {@link #GLIF_THEMES}.
     */
    public static void applyExpressiveTheme(Activity activity) {
        for (String name : GLIF_THEMES) {
            int id = activity.getResources().getIdentifier(name, "style",
                    activity.getPackageName());
            if (id != 0) {
                activity.setTheme(id);
                activity.getTheme().applyStyle(R.style.Theme_Setup_Alch3myExtras, true);
                return;
            }
        }
    }

    /** Outer and inner corners of a group of cards, as in Android 16's settings lists. */
    private static final float GROUP_OUTER_DP = 24;
    private static final float GROUP_INNER_DP = 6;

    /**
     * Turns a page's option rows into grouped Material 3 cards: each checkbox or radio button,
     * or the clickable row holding one, gets a rounded surface with a ripple, rows next to
     * each other share a group with large outer and small inner corners, a checked radio
     * button's card fills with the tonal accent, and the buttons take the accent colour.
     */
    public static void stylePage(View page) {
        if (page instanceof ViewGroup) {
            styleGroup((ViewGroup) page);
        }
    }

    private static void styleGroup(ViewGroup group) {
        // Rows next to each other in this parent form one group.
        int start = -1;
        for (int i = 0; i <= group.getChildCount(); i++) {
            View child = i < group.getChildCount() ? group.getChildAt(i) : null;
            if (child != null && child.getVisibility() != View.GONE && isRow(child)) {
                if (start < 0) {
                    start = i;
                }
                continue;
            }
            if (start >= 0) {
                styleRows(group, start, i - 1);
                start = -1;
            }
            if (child instanceof ViewGroup) {
                // Including a RadioGroup, whose buttons become one group of cards.
                styleGroup((ViewGroup) child);
            }
        }
    }

    /** A checkbox or radio button, or a clickable row that holds one. */
    private static boolean isRow(View view) {
        if (view instanceof CompoundButton) {
            return true;
        }
        return view instanceof LinearLayout && view.isClickable()
                && findCompound((ViewGroup) view) != null;
    }

    private static CompoundButton findCompound(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof CompoundButton) {
                return (CompoundButton) child;
            }
        }
        return null;
    }

    private static void styleRows(ViewGroup parent, int first, int last) {
        Context c = parent.getContext();
        float outer = dp(c, GROUP_OUTER_DP);
        float inner = dp(c, GROUP_INNER_DP);
        for (int i = first; i <= last; i++) {
            View row = parent.getChildAt(i);
            float top = i == first ? outer : inner;
            float bottom = i == last ? outer : inner;
            float[] radii = {top, top, top, top, bottom, bottom, bottom, bottom};
            CompoundButton button = row instanceof CompoundButton ? (CompoundButton) row
                    : findCompound((ViewGroup) row);
            row.setBackground(cardBackground(c, radii, row instanceof CompoundButton));
            int h = dp(c, 20);
            int v = dp(c, 14);
            row.setPaddingRelative(h, v, h, v);
            row.setMinimumHeight(dp(c, 64));
            ViewGroup.LayoutParams lp = row.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
                m.width = ViewGroup.LayoutParams.MATCH_PARENT;
                m.leftMargin = 0;
                m.rightMargin = 0;
                m.setMarginStart(0);
                m.setMarginEnd(0);
                m.topMargin = i == first ? dp(c, 8) : 0;
                m.bottomMargin = i == last ? dp(c, 8) : dp(c, 2);
                row.setLayoutParams(m);
            }
            if (button != null) {
                tint(button);
            }
            springOnPress(row);
        }
    }

    /**
     * The surface of a card, with a ripple. A checkbox or radio button that is its own card
     * fills with the tonal accent while checked.
     */
    private static Drawable cardBackground(Context c, float[] radii, boolean checkable) {
        GradientDrawable plain = new GradientDrawable();
        plain.setColor(surface(c));
        plain.setCornerRadii(radii);
        Drawable content = plain;
        if (checkable) {
            GradientDrawable checked = new GradientDrawable();
            checked.setColor(tonal(c));
            checked.setCornerRadii(radii);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_checked}, checked);
            states.addState(new int[0], plain);
            content = states;
        }
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadii(radii);
        return new RippleDrawable(ColorStateList.valueOf(
                (onSurface(c) & 0x00FFFFFF) | 0x24000000), content, mask);
    }

    /** The accent on checkboxes and radio buttons, and the title in the theme font. */
    private static void tint(CompoundButton button) {
        Context c = button.getContext();
        button.setButtonTintList(new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {primary(c), onSurfaceVariant(c)}));
        button.setTextColor(onSurface(c));
        button.setTypeface(font(500));
    }

    /** Fades and slides a view in, a little after {@code delayMs}. */
    public static void riseIn(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(dp(view.getContext(), 24));
        view.animate().alpha(1f).translationY(0f).setStartDelay(delayMs).setDuration(420)
                .setInterpolator(new OvershootInterpolator(1.2f)).start();
    }
}
