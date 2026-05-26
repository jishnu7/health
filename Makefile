JAVA_HOME := /Applications/Android Studio.app/Contents/jbr/Contents/Home
GRADLE = JAVA_HOME="$(JAVA_HOME)" ./gradlew
APK = app/build/outputs/apk/debug/app-debug.apk
# Debug builds install with .dev suffix so the dev app sits alongside production.
PACKAGE = xyz.jishnu.health.dev
ACTIVITY = $(PACKAGE)/xyz.jishnu.health.MainActivity
ADB = ~/Library/Android/sdk/platform-tools/adb

.PHONY: build install run clean clear-data test lint uninstall logcat devices apk bundle help

help:
	@echo "Targets:"
	@echo "  build      assembleDebug (produces $(APK))"
	@echo "  install    installDebug on the connected device/emulator"
	@echo "  run        install + launch MainActivity"
	@echo "  bundle     bundleRelease — signed AAB for Play upload"
	@echo "  uninstall  remove the app from the device"
	@echo "  clear-data wipe app storage (Room DB, DataStore, prefs) via pm clear"
	@echo "  clean      ./gradlew clean"
	@echo "  test       :app:testDebugUnitTest"
	@echo "  lint       :app:lintDebug"
	@echo "  logcat     tail logcat filtered to $(PACKAGE)"
	@echo "  devices    adb devices -l"
	@echo "  apk        print absolute path to the debug apk"

build:
	$(GRADLE) :app:assembleDebug

install: build
	$(GRADLE) :app:installDebug

run: install
	$(ADB) shell am start -n $(ACTIVITY)

uninstall:
	$(ADB) uninstall $(PACKAGE) || true

clear-data:
	$(ADB) shell pm clear $(PACKAGE)

clean:
	$(GRADLE) clean

test:
	$(GRADLE) :app:testDebugUnitTest

lint:
	$(GRADLE) :app:lintDebug

logcat:
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(PACKAGE))

devices:
	$(ADB) devices -l

apk:
	@echo $(CURDIR)/$(APK)

bundle:
	$(GRADLE) :app:bundleRelease
	@echo "AAB → $(CURDIR)/app/build/outputs/bundle/release/app-release.aab"
