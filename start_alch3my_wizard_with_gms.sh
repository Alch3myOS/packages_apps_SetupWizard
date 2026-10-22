#!/bin/bash

# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0

adb root
wait ${!}
adb shell pm enable org.alch3my.setupwizard || true
wait ${!}
# The wizard doesn't run again for a user that has finished setup.
adb shell settings put secure user_setup_complete 0 || true
wait ${!}
adb shell pm enable org.alch3my.setupwizard/.SetupWizardActivity || true
wait ${!}
adb shell pm enable org.alch3my.setupwizard/.DeviceSpecificActivity || true
wait ${!}
adb shell pm enable com.google.android.setupwizard || true
wait ${!}
if adb shell pm list packages | grep com.android.provision; then
  adb shell pm disable com.android.provision || true
  wait ${!}
fi
sleep 1
adb shell am start com.google.android.setupwizard/.SetupWizardTestActivity
