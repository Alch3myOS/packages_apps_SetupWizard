/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.alch3my.setupwizard;

import android.app.UiModeManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.alch3my.setupwizard.widget.ChoiceGroup;

public class ThemeSettingsActivity extends BaseSetupWizardActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.theme_summary));

        UiModeManager uiModeManager = getSystemService(UiModeManager.class);
        // Light and Dark as two big tiles, like the power menu's.
        final ChoiceGroup tiles = new ChoiceGroup(this, ChoiceGroup.TILES)
                .add(getString(R.string.light), getDrawable(R.drawable.ic_m3_light), null)
                .add(getString(R.string.dark), getDrawable(R.drawable.ic_m3_dark), null);
        tiles.setSelected(((getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) ? 1 : 0);
        tiles.setOnChoiceListener(index -> uiModeManager.setNightModeActivated(index == 1));
        ((FrameLayout) findViewById(R.id.theme_choices)).addView(tiles);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_theme;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_theme;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_theme;
    }
}
