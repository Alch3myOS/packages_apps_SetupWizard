/*
 * SPDX-FileCopyrightText: 2026 Alch3myOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.alch3my.setupwizard.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Pick one of a few choices. TILES draws each as a tonal tile with an icon (or a swatch) over
 * its name, in one row; CHIPS draws them as rounded chips that wrap onto more rows.
 */
public class ChoiceGroup extends ViewGroup {

    public interface OnChoiceListener {
        void onChoice(int index);
    }

    public static final int TILES = 0;
    public static final int CHIPS = 1;

    private final int mMode;
    private final List<View> mItems = new ArrayList<>();
    private int mSelected = -1;
    private OnChoiceListener mListener;
    private final int mGap;

    public ChoiceGroup(Context context, int mode) {
        super(context);
        mMode = mode;
        mGap = M3.dp(context, 8);
    }

    public void setOnChoiceListener(OnChoiceListener listener) {
        mListener = listener;
    }

    /** Adds a choice. {@code icon} may be null; {@code swatch} is a fill shown instead. */
    public ChoiceGroup add(CharSequence label, Drawable icon, Drawable swatch) {
        Context c = getContext();
        final int index = mItems.size();
        LinearLayout item = new LinearLayout(c);
        item.setOrientation(mMode == TILES ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(label);
        if (mMode == TILES) {
            item.setPadding(M3.dp(c, 8), M3.dp(c, 16), M3.dp(c, 8), M3.dp(c, 14));
        } else {
            item.setPadding(M3.dp(c, 16), 0, M3.dp(c, 16), 0);
            item.setMinimumHeight(M3.dp(c, 40));
        }
        if (swatch != null) {
            View dot = new View(c);
            dot.setBackground(swatch);
            int size = M3.dp(c, mMode == TILES ? 40 : 18);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (mMode == TILES) {
                lp.bottomMargin = M3.dp(c, 10);
            } else {
                lp.setMarginEnd(M3.dp(c, 8));
            }
            item.addView(dot, lp);
        } else if (icon != null) {
            ImageView image = new ImageView(c);
            image.setImageDrawable(icon.mutate());
            int size = M3.dp(c, mMode == TILES ? 28 : 18);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (mMode == TILES) {
                lp.bottomMargin = M3.dp(c, 10);
            } else {
                lp.setMarginEnd(M3.dp(c, 8));
            }
            item.addView(image, lp);
        }
        TextView text = new TextView(c);
        text.setText(label);
        text.setTypeface(M3.font(600));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        text.setGravity(Gravity.CENTER);
        item.addView(text);
        item.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            setSelected(index);
            if (mListener != null) {
                mListener.onChoice(index);
            }
        });
        M3.springOnPress(item);
        mItems.add(item);
        addView(item);
        paint(index);
        return this;
    }

    public void setSelected(int index) {
        int old = mSelected;
        mSelected = index;
        if (old >= 0 && old < mItems.size()) {
            paint(old);
        }
        if (index >= 0 && index < mItems.size()) {
            paint(index);
        }
    }

    public int getSelected() {
        return mSelected;
    }

    private void paint(int index) {
        Context c = getContext();
        View item = mItems.get(index);
        boolean on = index == mSelected;
        float radius = M3.dp(c, mMode == TILES ? 24 : 12);
        GradientDrawable bg = M3.rounded(on ? (mMode == TILES ? M3.tonal(c) : M3.primary(c))
                : M3.surfaceHigh(c), radius);
        if (on && mMode == TILES) {
            bg.setStroke(M3.dp(c, 2), M3.primary(c));
        }
        item.setBackground(M3.pressable(c, bg, radius));
        item.setSelected(on);
        int fg = on ? (mMode == TILES ? M3.onTonal(c) : M3.onPrimary(c)) : M3.onSurface(c);
        ViewGroup group = (ViewGroup) item;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(fg);
            } else if (child instanceof ImageView) {
                ((ImageView) child).setImageTintList(
                        ColorStateList.valueOf(on ? fg : M3.primary(c)));
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int n = mItems.size();
        if (n == 0) {
            setMeasuredDimension(width, 0);
            return;
        }
        if (mMode == TILES) {
            int each = (width - mGap * (n - 1)) / n;
            int height = 0;
            for (View item : mItems) {
                item.measure(MeasureSpec.makeMeasureSpec(each, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                height = Math.max(height, item.getMeasuredHeight());
            }
            for (View item : mItems) {
                item.measure(MeasureSpec.makeMeasureSpec(each, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(width, height);
            return;
        }
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (View item : mItems) {
            item.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            if (x > 0 && x + item.getMeasuredWidth() > width) {
                x = 0;
                y += rowHeight + mGap;
                rowHeight = 0;
            }
            x += item.getMeasuredWidth() + mGap;
            rowHeight = Math.max(rowHeight, item.getMeasuredHeight());
        }
        setMeasuredDimension(width, y + rowHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        for (View item : mItems) {
            int w = item.getMeasuredWidth();
            int h = item.getMeasuredHeight();
            if (mMode == CHIPS && x > 0 && x + w > width) {
                x = 0;
                y += rowHeight + mGap;
                rowHeight = 0;
            }
            int left = rtl ? width - x - w : x;
            item.layout(left, y, left + w, y + h);
            x += w + mGap;
            rowHeight = Math.max(rowHeight, h);
        }
    }

    /** A round swatch split between two colours, for colour choices. */
    public static Drawable swatch(int a, int b) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {a, a, b, b});
        d.setShape(GradientDrawable.OVAL);
        d.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return d;
    }
}
