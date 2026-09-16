# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Working style

**A question is a question.** "Why/what/how" about the code is not an instruction to change it and
not pushback. Answer with a position grounded in evidence — docs, library source, measurements — and
stand by it. Don't edit, experiment, or revert unless asked. Change the recommendation only when
there is a new argument or new evidence, and say what that evidence is.

**Stay in the requested layer.** The app is built layer by layer. A question about storage gets a
storage answer — no UI, picker, or permissions suggestions unless asked.

## Verification

After every code change, run both, in order, without being asked.

**1. Android Studio's Reformat Code, headless.** The project has no `.idea/codeStyles` and ktfmt is
disabled in `.idea/ktfmt.xml`, so the IDE uses the global Default scheme (KOTLIN_OFFICIAL).

```bash
STUDIO_PROPERTIES=<scratch>/studio/idea.properties \
  "$HOME/Applications/Android Studio.app/Contents/bin/format.sh" \
  -s "$HOME/Library/Application Support/Google/AndroidStudio2026.1.4/codestyles/Default.xml" \
  -r app/src/main/java/com/dustinbluck/nanit
```

A plain `format.sh` fails with "Only one instance of Studio can be run at a time" while the IDE is
open, so point it at separate config/system dirs through a `STUDIO_PROPERTIES` file setting
`idea.config.path`, `idea.system.path`, `idea.log.path` and `idea.plugins.path` to scratchpad
folders. Copy sources to scratch first and `diff -r` afterward to report what changed. The config
folder name changes when Studio updates — re-check under `~/Library/Application Support/Google/`.

**2. `./gradlew check`** — detekt, lint and unit tests. Delete
`app/build/reports/lint-results-debug.sarif` first, or a failed compile leaves a stale "0 findings"
report.

The IDE formatter leaves one-line nested composables alone, so it does not replace the Compose style
rules below. Write in that style first, then format. The IDE can also save a stale buffer over
edits — re-read files before editing if they changed on disk.

## Coding standards

**Write boring, explicit code — one step per line.** Being cute is not helpful. Break work into
separate lines with explicitly named variables; do not chain, nest, or inline expressions to save
lines.

**Hoist repeated accessors into one named local.** If `NanitDeps.instance`, a `CompositionLocal`, or
any shared/singleton accessor is read more than once in a function, assign it once and use that. In
a composable, read each `CompositionLocal` once at the top — `LocalContext.current`,
`LocalDensity.current`, `LocalResources.current` — and never introduce a second local for a value
already in scope.

**Guard required dependencies up front.** Kotlin's version of `guard let` is a single early return
at the top of the function:

```kotlin
val name = baby.name ?: return BirthdayUiState.Error
val birthday = baby.birthday ?: return BirthdayUiState.Error
```

Don't sprinkle `?.` chains and `?: emptyList()` fallbacks through the body. If the dependency isn't
there, the logic shouldn't run at all.

**Name intermediate values.** A parse, a lookup, a computed size, a filter result — each gets its
own line and a descriptive name. Don't feed one call's result straight into another call's argument
list when naming it would make the step readable.

```kotlin
val ageUnitPlurals = when (uiState.age.unit) {
    AgeUnit.MONTHS -> R.plurals.birthday_months_old
    AgeUnit.YEARS -> R.plurals.birthday_years_old
}
val ageText = pluralStringResource(ageUnitPlurals, age).toUpperCase(locale)
```

**Don't inline non-trivial arguments.** Anything that takes multiple lines to express — building an
`ImageRequest`, a string, a set, a `Modifier` used once — gets computed into a named variable first,
then passed. A bare `stringResource(R.string.x)` or `painterResource(R.drawable.y)` is trivial and
stays inline.

**No clever scope-function chains.** A single `apply` to configure an `Intent`, or one `use` on a
stream, is fine. Stacking `let`/`also`/`run`/`apply` to avoid naming something is not. Prefer a
named `val` and a plain `if` over `?.let { }` when the result is used more than once.

**Derive constants rather than repeating numbers.** `EditPhotoOffset` is computed from
`BabyPhotoSize`, so resizing the photo moves the button with it. Don't write the same magic number
in two places.

## Kotlin conventions

- **No top-level declarations.** Properties go in the class or its `companion object`, `private`
  unless something else needs them. Top-level `@Composable` functions are the Compose norm and are
  fine. Don't add new file-level vals without being asked — `NumberDrawables` in `BirthdayScreen.kt`
  and the theme files (`Color.kt`, `Type.kt`, `LightColorScheme`) are existing exceptions.
- **`private companion object`** whenever nothing outside the class uses it. `NanitDeps.instance` is
  the kind of case that stays public.
- **Keys are constants.** Preference, storage and bundle keys are `private const val KEY_*` in the
  companion, never inline literals. Typed `Preferences.Key` objects can't be `const`, so they stay
  `val`s built from those constants.
- **Log messages vs. user-facing text.** Anything a user can see goes through `stringResource` and
  lives in `strings.xml`. Log-only constants may stay in the companion but must carry a
  `_LOG_MESSAGE` suffix.
- **Boolean over exceptions.** Expected failures (disk/DataStore IO) return `Boolean` rather than
  throwing. Catch the specific exception as `catch (_: IOException)` — detekt's `SwallowedException`
  accepts the underscore. Never `runCatching`; it swallows `CancellationException` too. Apply
  consistently across an API. Propose a richer type only when a caller actually needs the cause.
- **detekt requires PascalCase for top-level constants**, not `SCREAMING_SNAKE_CASE`.

## Architecture

- **Constructor injection only.** Classes take their real dependencies (`DataStore<Preferences>`,
  `BabyRepository`, `Clock`, `Logger`) as constructor parameters.
- **Never fetch dependencies from a `Context`.** No
  `val Context.babyDataStore by preferencesDataStore(...)` extension properties, no
  `constructor(context: Context) : this(context.babyDataStore)`, nothing shaped like a service
  locator hung off `Context`.
- Dependencies are wired by hand in `deps/NanitDeps` — pure DI, no Dagger or Hilt for an app this
  size.

## Compose conventions

- Wrap Compose content in `NanitTheme` (`ui/theme/Theme.kt`). It is a `MaterialExpressiveTheme`,
  which applies the expressive motion scheme by default, and uses Material You dynamic color on
  Android 12+ and falls back to the static schemes in `Color.kt` on older versions. Typography is in
  `Type.kt`.
- `MainActivity` calls `enableEdgeToEdge()` and uses a `Scaffold`, so screens need to apply the
  scaffold's `innerPadding` or window insets themselves.
- Private reusable composables take `modifier: Modifier = Modifier` and emit one root layout.
- **Choose components for what they are designed for**, verified against their KDoc or the Material
  spec — not by their implementation details. `ListItem` is for entries in an actual list of many
  rows, not for label/value fields or a two-action bottom sheet; a short set of actions is buttons.
  Never claim "Material's guidance shows this pattern" from memory — find the androidx sample or
  spec page and quote it. If no sample exists for the pattern, that absence is the answer.

### Formatting

**No single-line lambdas at all**, composable or not. The body always goes on its own line.

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Text(stringResource(R.string.app_name))
            }
        )
    }
) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onBirthdayClick) {
            Text("Birthday")
        }
    }
}
```

- Every composable call goes on its own line; a lambda containing a composable call is always
  multi-line.
- When a call has more than one argument, or any argument is a composable lambda, each argument goes
  on its own line.
- Modifier chains put each `.call()` on its own line.
- **`when` branches never use a composable as an expression body.** Wrap each branch in braces and
  pull non-trivial content into a private composable.
- Before calling a change done, grep for `{ ... }` on one line in `.kt` files and fix every hit.

## Testing

- **A failing test or it isn't a bug.** For anything in a testable layer (ViewModel, repository,
  util), don't report a defect without a failing test that demonstrates it. Write the failing test
  first, show it red, fix, show it green — then verify the test is load-bearing by stubbing out the
  fix and confirming that test, and only that test, goes red. When a finding resists an honest
  failing test, say so plainly and drop it; don't manufacture contrived infrastructure to reach a
  branch production cannot reach. The exception is layers where tests cannot express the problem at
  all — manifest/config, build wiring, layout/rendering — verify those by reading the file or by
  arithmetic, and say which.
- **Assertions use Google Truth.** Never `org.junit.Assert.*`. JUnit stays as the runner only (
  `@Test`, `@Rule`, `TemporaryFolder`). Truth is on both `testImplementation` and
  `androidTestImplementation`.
- **Test classes are `internal`.**
- **The object under test is `subject`** — a `private lateinit var` created in`@Before fun setUp()`.
  Never `by lazy` or an inline `val`.
- **One assert per test.** Write the values through the subject, read them back separately, then
  make a single `assertThat`. Separate those three steps with blank lines. Test each behavior in its
  own test.
- **Coroutine tests use `runTest`**, written as an expression body: `fun x() = runTest {`. Never
  `runBlocking` per test body.
- **Flows are tested with Turbine** — `flow.test { awaitItem() }`, and `skipItems(1)` then the write
  then one `assertThat(awaitItem())` for new-emission tests. Don't hand-roll `Channel` plus `launch`
  collectors.
- **Avoid tests that pass even when the behavior is broken** — for example `expectNoEvents()` right
  after an asynchronous write can run before a stray emission arrives.
- Same formatting rules as main code: constants in the companion, one argument per line, no one-line
  lambdas.

## Project state

Single `:app` module, Jetpack Compose + Material 3, package / applicationId `com.dustinbluck.nanit`.
Two screens, navigated by Navigation 3 from a back stack of `Screen` keys in `MainActivity`:

- **`ui/main/MainScreen`** edits the baby's name, birthday and photo, and opens the birthday screen.
- **`ui/birthday/BirthdayScreen`** shows the baby's age in months or years, over one of three
  illustrated backgrounds (`BirthdayMode`) picked at random each time it opens, and shares the
  screen as an image.

`data/PreferencesBabyRepository` stores the baby in DataStore, `data/PhotoManager` handles photo
files and the shared birthday card, and `ui/birthday/AgeCalculatorUtil` turns a birthday plus
today's date into an `Age`. `ui/photo/PhotoEditor` owns photo-edit state and is composed by both
ViewModels. `ui/capture` provides `LocalCaptureAlpha` and `Modifier.hiddenInCapture()`, which fade
controls out of the shared image without changing layout. Dependencies are wired by hand in
`deps/NanitDeps`.

## Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # build + install on a connected device/emulator
./gradlew lint                          # Android lint
./gradlew testDebugUnitTest             # JVM unit tests (app/src/test)
./gradlew connectedDebugAndroidTest     # instrumented tests (app/src/androidTest), needs a device

# Single unit test class or method
./gradlew :app:testDebugUnitTest --tests "com.dustinbluck.nanit.ui.birthday.AgeCalculatorUtilTest"
./gradlew :app:testDebugUnitTest --tests "com.dustinbluck.nanit.ui.birthday.AgeCalculatorUtilTest.ageIsOneMonthOnFirstMonth"

# Single instrumented test class
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dustinbluck.nanit.data.PhotoManagerUriTest
```

## Build setup

The toolchain is newer than most examples online, so check these before copying snippets:

- **AGP 9.4 / Gradle 9.6.** Kotlin support is built into AGP 9, so there is no
  `org.jetbrains.kotlin.android` plugin, only `org.jetbrains.kotlin.plugin.compose`. Don't add the
  Kotlin Android plugin.
- **R8 config uses AGP 9 conventions.** Keep rules go in `app/src/main/keepRules/*.keep` (not
  `proguard-rules.pro`), and shrinking is controlled by `optimization { enable = ... }` in the build
  type (not `isMinifyEnabled`). It is on for release. That single flag turns on R8 full-mode code
  shrinking, optimization, obfuscation, optimized resource shrinking, and Android's default keep
  rules. The app currently runs without custom keep rules. Release builds write `mapping.txt`,
  needed to deobfuscate stack traces, and the R8 config analyzer report to
  `app/build/outputs/mapping/release/`.
- `compileSdk { version = release(37) }`, `targetSdk = 37`, `minSdk = 24`. Java source/target is 11.
  The Gradle daemon runs on a JDK 25 toolchain, resolved via foojay (
  `gradle/gradle-daemon-jvm.properties`).
- **Configuration cache is on** (`gradle.properties`). Build logic you add must be
  configuration-cache compatible.
- Dependencies and plugins are declared in the version catalog `gradle/libs.versions.toml`.
  Reference them as `libs.*`, and add new ones to the catalog instead of hardcoding coordinates.
  Compose library versions come from the Compose BOM, except Material3.
- **Material3 is pinned to an alpha** (`material3` in the version catalog), overriding the BOM, to
  get the M3 Expressive APIs (`MaterialExpressiveTheme`, `MaterialShapes`, `LoadingIndicator`,
  flexible top app bars, emphasized typography). Stable 1.4.0 ships them as internal code only. Its
  transitive requirements also move every core Compose library (`runtime`, `ui`, `foundation`,
  `animation`) from the BOM's stable version to the matching alpha, so the whole Compose stack is
  pre-release. Expressive APIs still marked experimental need
  `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Move Material3 back to the BOM version once
  Expressive is stable.
- `settings.gradle.kts` uses `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, so repositories can only be
  declared there, not in module build files.
- **Detekt is 2.0 alpha** (plugin id `dev.detekt`). Rule names and config keys differ from 1.x, so
  check any 1.x docs against the 2.0 default config before copying. `config/detekt/detekt.yml`builds
  on the default config and lists only the overrides: noisy Compose and style rules are off,
  `@Composable` functions are exempt from FunctionNaming, `@Preview` functions are exempt from
  UnusedPrivateFunction, and top-level constants may use PascalCase. Keep the enabled rule set
  minimal. `./gradlew check` runs the type-resolved `detektMain` and `detektTest` tasks in place of
  the plain `detekt` task. The plain task skips every rule that needs type resolution, doesn't scan
  `androidTest`, and is being retired in 2.0.
