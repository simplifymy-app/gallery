APP_ID   := app.simplifymy.gallery
SDK      ?= $(HOME)/Library/Android/sdk
ADB      := $(SDK)/platform-tools/adb
EMULATOR := $(SDK)/emulator/emulator
VERSION  := $(shell sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
DIST     := dist
APK      := $(DIST)/simplify-gallery-$(VERSION).apk
AAB      := $(DIST)/simplify-gallery-$(VERSION).aab

.PHONY: help
help: #shows what make commands are available
	@grep '^\S\S*: .*#' $(MAKEFILE_LIST) | sed 's/:.*#/: #/' | column -s '#' -t | sort

.PHONY: run
run: #installs the debug build on a device or emulator and launches it
	./gradlew :app:installDebug
	@$(ADB) shell monkey -p $(APP_ID).debug -c android.intent.category.LAUNCHER 1 >/dev/null
	@echo "launched $(APP_ID).debug"

.PHONY: install
install: #installs the debug apk on the attached device
	./gradlew :app:installDebug

.PHONY: debug
debug: #builds the debug apk
	./gradlew :app:assembleDebug

.PHONY: release
release: #builds the minified release apk
	./gradlew :app:assembleRelease

.PHONY: apk
apk: #builds the release apk and copies it to dist as a versioned file
	./gradlew :app:assembleRelease
	@mkdir -p $(DIST)
	@cp app/build/outputs/apk/release/app-release.apk $(APK)
	@$$(ls $(SDK)/build-tools/*/apksigner | sort -V | tail -1) verify $(APK) && echo "signature ok"
	@ls -lh $(APK)

.PHONY: bundle
bundle: #builds the signed app bundle for google play into dist
	./gradlew :app:bundleRelease
	@mkdir -p $(DIST)
	@cp app/build/outputs/bundle/release/app-release.aab $(AAB)
	@ls -lh $(AAB)

.PHONY: emulator
emulator: #boots the first available avd and waits for it
	@$(EMULATOR) -avd $$($(EMULATOR) -list-avds | head -1) >/dev/null 2>&1 &
	@$(ADB) wait-for-device
	@until [ "$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
	@echo "emulator ready"

.PHONY: logcat
logcat: #follows the app log output
	@$(ADB) logcat --pid=$$($(ADB) shell pidof $(APP_ID).debug)

.PHONY: screenshot
screenshot: #saves a screenshot of the device to screenshot.png
	@$(ADB) exec-out screencap -p > screenshot.png && echo "wrote screenshot.png"

.PHONY: e2e
e2e: #runs the end-to-end tests on the attached device or emulator
	./gradlew :app:connectedDebugAndroidTest

.PHONY: e2e-report
e2e-report: #opens the report of the last end-to-end run
	@open app/build/reports/androidTests/connected/debug/index.html

.PHONY: lint
lint: #runs android lint over the debug variant
	./gradlew :app:lintDebug

.PHONY: version
version: #prints the current version name
	@echo $(VERSION)

.PHONY: bump-patch
bump-patch: #bumps the patch version and the version code, 1.0.0 to 1.0.1
	@sh scripts/bump.sh patch

.PHONY: bump-minor
bump-minor: #bumps the minor version and the version code, 1.0.3 to 1.1.0
	@sh scripts/bump.sh minor

.PHONY: bump-major
bump-major: #bumps the major version and the version code, 1.2.3 to 2.0.0
	@sh scripts/bump.sh major

.PHONY: set-version
set-version: #sets an exact version and bumps the code, make set-version VERSION=1.4.0
	@sh scripts/bump.sh $(VERSION)

.PHONY: test
test: #runs the unit tests
	./gradlew :app:testDebugUnitTest

.PHONY: tag
tag: #tags HEAD as v<version> with its commit message and pushes it, which publishes a release
	@test -z "$$(git status --porcelain --untracked-files=no)" || { echo "commit your changes first"; exit 1; }
	@! git rev-parse -q --verify "refs/tags/v$(VERSION)" >/dev/null || { echo "v$(VERSION) already exists, bump the version first"; exit 1; }
	@git log -1 --format=%B | git tag -a "v$(VERSION)" -F -
	@git push origin "v$(VERSION)"
	@echo "pushed v$(VERSION), the release workflow builds and publishes it"

.PHONY: clean
clean: #removes build output and the dist directory
	./gradlew clean
	@rm -rf $(DIST)
