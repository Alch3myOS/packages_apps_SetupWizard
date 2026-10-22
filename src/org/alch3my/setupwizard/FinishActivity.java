/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.alch3my.setupwizard;

import static org.alch3my.setupwizard.SetupWizardApp.LOGV;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.setupcompat.util.SystemBarHelper;

import org.alch3my.setupwizard.util.SetupWizardUtils;
import org.alch3my.setupwizard.widget.M3;

public class FinishActivity extends BaseSetupWizardActivity {

    public static final String TAG = FinishActivity.class.getSimpleName();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private enum FinishState {
        NONE,
        SHOULD_ANIMATE,
        ANIMATING,
        FINISHED
    }

    // "Why not just start this activity with an Intent extra?" you might ask. Been there.
    // We need this to affect the theme, and even onCreate is not early enough for that,
    // so "static volatile" it is. Feel free to rework this if you dare.
    private static volatile FinishState sFinishState = FinishState.NONE;

    /** The ArcaniumUI page picked on the What's new cards, opened once setup is done. */
    private static volatile String sOpenPage;
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final String ARCANIUM_ACTIVITY =
            "com.android.settings.Settings$Alch3mySettingsActivity";
    private static final String EXTRA_OPEN_PAGE = "org.alch3my.settings.extra.OPEN_PAGE";
    /** Where each card goes when setup finishes: the home screen (null), or an ArcaniumUI page. */
    private static final String[] PAGES = {null, "dashboard"};
    private static final int[][] PAGE_TEXT = {
        {R.string.whats_new_home, R.string.whats_new_home_summary},
        {R.string.whats_new_dashboard, R.string.whats_new_dashboard_summary},
    };

    private View mRootView;

    @Override
    protected boolean useExpressiveTheme() {
        return false;
    }
    private Resources.Theme mEdgeToEdgeWallpaperBackgroundTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate: sFinishState=" + sFinishState);

        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.translucent_enter,
                R.anim.translucent_exit);
        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }
        setNextText(R.string.start);

        // Edge-to-edge. Needed for the background view to fill the full screen.
        final Window window = getWindow();
        window.setDecorFitsSystemWindows(false);

        // Make sure 3-button navigation bar is the same color as the rest of the screen.
        window.setNavigationBarContrastEnforced(false);

        // Ensure the main layout (not including the background view) does not get obscured by bars.
        mRootView = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(mRootView, (view, windowInsets) -> {
            final View linearLayout = findViewById(R.id.linear_layout);
            final Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            final MarginLayoutParams params = (MarginLayoutParams) linearLayout.getLayoutParams();
            params.leftMargin = insets.left;
            params.topMargin = insets.top;
            params.rightMargin = insets.right;
            params.bottomMargin = insets.bottom;
            linearLayout.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });

        if (sFinishState != FinishState.NONE) {
            disableNavigation();
        }
        setUpWhatsNew();

        switch (sFinishState) {
            case NONE:
                break;
            case SHOULD_ANIMATE:
                startFinishSequence();
                break;
            case FINISHED:
                Log.e(TAG, "Should not start again when finished!");
                finish();
                break;
            default:
                Log.w(TAG, "Unexpected onCreate state " + sFinishState);
                break;
        }
    }

    /** Two cards side by side: the home screen, or ArcaniumUI. Tapping one finishes setup. */
    private void setUpWhatsNew() {
        final View section = findViewById(R.id.whats_new);
        final LinearLayout cards = findViewById(R.id.whats_new_cards);
        if (section == null || cards == null) {
            return;
        }
        if (sFinishState != FinishState.NONE) {
            section.setVisibility(View.GONE);
            return;
        }
        final TextView title = findViewById(R.id.whats_new_title);
        if (title != null) {
            title.setTypeface(M3.font(600));
        }
        cards.removeAllViews();
        for (int i = 0; i < PAGES.length; i++) {
            final String page = PAGES[i];
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int pad = M3.dp(this, 16);
            card.setPadding(pad, pad, pad, pad);
            float radius = M3.dp(this, 24);
            card.setBackground(M3.pressable(this, M3.rounded(i == 0 ? M3.tonal(this)
                    : M3.surface(this), radius), radius));
            TextView name = new TextView(this);
            name.setText(PAGE_TEXT[i][0]);
            name.setTypeface(M3.font(600));
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            name.setTextColor(i == 0 ? M3.onTonal(this) : M3.onSurface(this));
            TextView summary = new TextView(this);
            summary.setText(PAGE_TEXT[i][1]);
            summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            summary.setTextColor(i == 0 ? M3.onTonal(this) : M3.onSurfaceVariant(this));
            summary.setPadding(0, M3.dp(this, 4), 0, 0);
            card.addView(name);
            card.addView(summary);
            card.setClickable(true);
            card.setOnClickListener(v -> {
                sOpenPage = page;
                onNavigateNext();
            });
            M3.springOnPress(card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) {
                lp.setMarginStart(M3.dp(this, 10));
            }
            cards.addView(card, lp);
            M3.riseIn(card, 80L * i);
        }
    }

    private void openPickedPage() {
        final String page = sOpenPage;
        sOpenPage = null;
        if (page == null) {
            return;
        }
        try {
            // Not this activity's startActivity: that adds the setup wizard's extras.
            getApplicationContext().startActivity(new Intent()
                    .setClassName(SETTINGS_PACKAGE, ARCANIUM_ACTIVITY)
                    .putExtra(EXTRA_OPEN_PAGE, page)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Couldn't open " + page, e);
        }
    }

    private void disableNavigation() {
        hideNextButton();
        SystemBarHelper.setBackButtonVisible(getWindow(), false);
    }

    private void disableActivityTransitions() {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
    }

    @Override
    protected void applyForwardTransition() {
        if (sFinishState == FinishState.NONE) {
            super.applyForwardTransition();
        }
    }

    @Override
    protected void applyBackwardTransition() {
        if (sFinishState == FinishState.NONE) {
            super.applyBackwardTransition();
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.finish_activity;
    }

    @Override
    public Resources.Theme getTheme() {
        Resources.Theme theme = super.getTheme();
        if (sFinishState == FinishState.NONE) {
            return theme;
        }
        if (mEdgeToEdgeWallpaperBackgroundTheme == null) {
            theme.applyStyle(R.style.EdgeToEdgeWallpaperBackground, true);
            mEdgeToEdgeWallpaperBackgroundTheme = theme;
        }
        return mEdgeToEdgeWallpaperBackgroundTheme;
    }

    @Override
    public void onNavigateNext() {
        switch (sFinishState) {
            case NONE:
                relaunchAndRunAnimation();
                break;
            default:
                Log.e(TAG, "Unexpected state " + sFinishState + " when navigating next");
        }
    }

    private void relaunchAndRunAnimation() {
        sFinishState = FinishState.SHOULD_ANIMATE;
        // Relaunching the activity before finishing is the only way currently known to prevent
        // an out-of-place slide transition from happening, even when disabling transitions, and
        // regardless of when we disable them. This also means we can't simply call recreate(), but
        // another reason is that recreate() doesn't seem to reinitialize the theme, which is the
        // entire point of relaunching - to ensure this activity reveals a wallpaper background.
        // These theme shenanigans and relaunching were not necessary prior to Android 14 QPR3.
        startActivity(getIntent());
        finish();
        disableActivityTransitions();
    }

    private void startFinishSequence() {
        sFinishState = FinishState.ANIMATING;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        disableNavigation();

        // Begin outro animation.
        if (mRootView.isAttachedToWindow()) {
            mHandler.post(() -> animateOut());
        } else {
            mRootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    mHandler.post(() -> animateOut());
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    // Do nothing
                }
            });
        }
    }

    private void animateOut() {
        if (sFinishState != FinishState.ANIMATING) {
            Log.e(TAG, "animateOut but in " + sFinishState + " phase. How?");
            return;
        }
        final int cx = (mRootView.getLeft() + mRootView.getRight()) / 2;
        final int cy = (mRootView.getTop() + mRootView.getBottom()) / 2;
        final float fullRadius = (float) Math.hypot(cx, cy);
        Animator anim;
        try {
            anim = ViewAnimationUtils.createCircularReveal(mRootView, cx, cy, fullRadius, 0f);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to create finish animation", e);
            finishAfterAnimation();
            return;
        }
        anim.setDuration(900);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mRootView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRootView.setVisibility(View.INVISIBLE);
                mHandler.post(() -> {
                    if (LOGV) {
                        Log.v(TAG, "Animation ended");
                    }
                    finishAfterAnimation();
                });
            }
        });
        anim.start();
    }

    private void finishAfterAnimation() {
        SetupWizardUtils.finishSetupWizard(FinishActivity.this);
        sFinishState = FinishState.FINISHED;
        openPickedPage();
    }
}
