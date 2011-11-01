#! /bin/sh
export ANDROID_SDK=<path to the Android SDK folder>
$ANDROID_SDK/dx --dex --output=classes.dex <path to your mobile host service jar file>
$ANDROID_SDK/aapt add <path to your mobile host service jar file> classes.dex
