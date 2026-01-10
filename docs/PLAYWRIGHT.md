# Playwright integration with Serenity (guide)

This document explains how to integrate Playwright into the Serenity test suite used in this repository.

## Why use Playwright?
- Playwright offers robust auto-waiting, reliable locator APIs, and consistent headless browser support which can reduce UI flakiness compared to raw WebDriver clicks.
- The `serenity-playwright` artifact provides an integration layer that keeps Serenity reporting and the Screenplay pattern.

## Add dependencies (Gradle)
Add these to `dependencies` in `build.gradle`:

```gradle
// Serenity Playwright integration (match your Serenity version)
testImplementation "net.serenity-bdd:serenity-playwright:5.1.0"
// Playwright Java bindings
testImplementation "com.microsoft.playwright:playwright:1.39.0"
```

The repository already includes these as an opt-in feature. See the `installPlaywrightBrowsers` task and `usePlaywright` property in `build.gradle`.

## Opt-in usage
Do NOT set `-Dwebdriver.driver=playwright` (Serenity's SupportedWebDriver does not support replacing the WebDriver with `playwright`).
Instead, opt-in to the Playwright extension as follows:

- Local quick run (uses Chrome/Chromium):

```bash
# install browsers once (requires Node / npx available) - optional but recommended
./gradlew installPlaywrightBrowsers

# run tests with Playwright enabled (opt-in)
./gradlew -PusePlaywright test -Dserenity.playwright.browser=chromium -Dserenity.playwright.headless=true
```

In Gradle this repo sets `-PusePlaywright` to assign system properties: `serenity.playwright.enabled=true`, `serenity.playwright.browser` and `serenity.playwright.headless`.

## CI (recommended step)
Ensure Playwright browser binaries and OS dependencies are installed in CI. Example GitHub Actions step:

```yaml
- name: Install Node and Playwright browsers
  run: |
    npm ci --silent || true
    npx playwright install --with-deps
```

You can also call the Gradle convenience task from CI:

```yaml
- name: Install Playwright browsers (Gradle)
  run: ./gradlew installPlaywrightBrowsers
```

## Using Playwright in tests (strategy)
1. Minimal/fast path: enable Playwright and re-run tests — many tests will continue to use Selenium/WebDriver. The Playwright extension will be available for new workloads.
2. Targeted migration: rewrite flaky Screenplay tasks to use Playwright APIs and the `SerenityPlaywright` helpers. Example:
   - Use Playwright's `page.locator(selector).click()` or `.fill()` for robust actions.
   - Use `net.serenitybdd.playwright.SerenityPlaywright` (see the dependency jar) to interact with Playwright pages from Screenplay tasks.

Example conceptual snippet (POC):

```java
// get the current Playwright page for the actor (adapter usage depends on Serenity Playwright API)
com.microsoft.playwright.Page page = net.serenitybdd.playwright.PlaywrightSerenity.getCurrentPage();
page.locator("css=.customer-combobox input").click();
page.locator("text=20250601-0931300").click();
```

> Note: See the `serenity-playwright` classes in the dependency for helper methods such as `PlaywrightSerenity.getCurrentPage()`.

## Caveats & notes
- Playwright integration is opt-in and coexists with existing Selenium-based tests — you can migrate tests incrementally.
- The Serenity plugin does not currently treat `webdriver.driver=playwright` as a supported driver; do not set that property.
- For CI, always install Playwright browsers (`npx playwright install --with-deps`) to ensure deterministic runs.

## Suggested migration plan
- Step 1: add dependencies and install browsers in CI (done in this repo as opt-in tasks).
- Step 2: convert 1-2 flaky tasks to Playwright-based Screenplay tasks and run them repeatedly to validate stability.
- Step 3: gradually migrate the rest of flaky selectors to Playwright locators.

## Troubleshooting
- If tests still fail, check screenshots under `build/serenity/screenshots/` and Serenity JSON test outputs.
- If Playwright reports missing browsers, run `./gradlew installPlaywrightBrowsers` locally and in CI.

---

If you'd like, I can implement a small PoC migration for `SelectFromCombobox` and run it repeatedly to measure flakiness improvements.