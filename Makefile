JAVA_HOME := /Applications/Android Studio.app/Contents/jbr/Contents/Home
GRADLE = JAVA_HOME="$(JAVA_HOME)" ./gradlew
APK = app/build/outputs/apk/debug/app-debug.apk
PACKAGE = xyz.jishnu.health
ACTIVITY = $(PACKAGE)/.MainActivity
ADB = ~/Library/Android/sdk/platform-tools/adb

.PHONY: build install run clean test lint uninstall logcat devices apk help

help:
	@echo "Targets:"
	@echo "  build      assembleDebug (produces $(APK))"
	@echo "  install    installDebug on the connected device/emulator"
	@echo "  run        install + launch MainActivity"
	@echo "  uninstall  remove the app from the device"
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
