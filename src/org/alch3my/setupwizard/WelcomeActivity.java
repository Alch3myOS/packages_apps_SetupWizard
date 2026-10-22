/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.alch3my.setupwizard;

import static org.alch3my.setupwizard.SetupWizardApp.ACTION_EMERGENCY_DIAL;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;
import com.google.android.setupcompat.util.SystemBarHelper;

import org.alch3my.setupwizard.util.SetupWizardUtils;

public class WelcomeActivity extends SubBaseActivity {

    private static final String ACTION_ACCESSIBILITY_SETTINGS =
            "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW";

    private static final float WELCOME_LEAD_SCALE = 0.5f;
    private static final float WELCOME_NAME_SCALE = 0.8f;

    @Override
    protected void onStartSubactivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onSetupStart();
        SystemBarHelper.setBackButtonVisible(getWindow(), false);
        setNextText(R.string.start);
        Button startButton = findViewById(R.id.start);
        Button emergButton = findViewById(R.id.emerg_dialer);
        Button skipButton = findViewById(R.id.skip);
        startButton.setOnClickListener(view -> onNextPressed());
        findViewById(R.id.launch_accessibility)
                .setOnClickListener(
                        view -> startSubactivity(new Intent(ACTION_ACCESSIBILITY_SETTINGS)));

        FooterButtonStyleUtils.applyPrimaryButtonPartnerResource(this, startButton, true);

        if (SetupWizardUtils.hasTelephony(this)) {
            setSkipText(R.string.emergency_call);
            emergButton.setOnClickListener(
                    view -> startSubactivity(new Intent(ACTION_EMERGENCY_DIAL)));

            FooterButtonStyleUtils.applySecondaryButtonPartnerResource(this, emergButton, true);
        } else {
            emergButton.setVisibility(View.GONE);
        }

        TextView welcomeTitle = findViewById(R.id.welcome_title);
        if (SetupWizardUtils.isManagedProfile(this)) {
            welcomeTitle.setText(getString(R.string.setup_managed_profile_welcome_message));
        } else {
            setWelcomeTitle(welcomeTitle);
        }

        if (Build.TYPE.equals("eng")) {
            skipButton.setVisibility(View.VISIBLE);
            skipButton.setOnClickListener(v -> {
                SetupWizardUtils.finishSetupWizard(WelcomeActivity.this);
            });
        }
    }

    private void setWelcomeTitle(TextView title) {
        final String osName = getString(R.string.os_name);
        final String message = getString(R.string.setup_welcome_message, osName);
        final int nameStart = message.indexOf(osName);
        if (nameStart < 0) {
            title.setText(message);
            return;
        }
        final int nameEnd = nameStart + osName.length();
        final SpannableStringBuilder text = new SpannableStringBuilder(message);

        if (nameStart > 0 && nameEnd == message.length()
                && Character.isWhitespace(message.charAt(nameStart - 1))) {
            // "Welcome to" on its own line, smaller, above the name.
            text.replace(nameStart - 1, nameStart, "\n");
            text.setSpan(new RelativeSizeSpan(WELCOME_LEAD_SCALE), 0, nameStart,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        text.setSpan(new RelativeSizeSpan(WELCOME_NAME_SCALE), nameStart, nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new StyleSpan(Typeface.BOLD), nameStart, nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final TypedArray a = obtainStyledAttributes(new int[] {android.R.attr.colorAccent});
        final int accent = a.getColor(0, title.getCurrentTextColor());
        a.recycle();
        text.setSpan(new ForegroundColorSpan(accent), nameStart, nameEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(text);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.welcome_activity;
    }

    @Override
    protected int getTitleResId() {
        return -1;
    }
}
