# レビュー結果: align-sample-parity (002 回目)

**日付**: 2026-08-01
**判定**: APPROVED

## サマリー

review-001 / second-opinion-002 で確定した 5 件の指摘はすべて妥当に解消されている。修正差分そのものにも新規の欠陥は入っていない — 両 platform のビルドはグリーン、iOS の警告はクリーン DerivedData で 33 件 (指摘前と同数・全て既存パターン) かつ `SampleScreen.swift` は 0 件、Kotlin 警告 0 件、追加した 2 依存はいずれも本体の解決バージョンと一致する。共有アクセントパレットは両 platform で同一の 6 値を定義しており、その値が「iOS `UIColor.systemXxx` の light appearance 実値」であることをレビュー側で独立に再測定して 6 色すべて一致を確認した。

残る指摘は deviation.md の 1 行に含まれる色値・出処の事実誤り (🟡 Minor / 優先度低) と、修正で触れた `build.gradle.kts` に残る陳腐化コメント (🔵) の 2 件のみ。いずれも出荷コードの挙動に影響せず、蒸留前に 1 行直せば閉じる。

---

## 前回指摘のクローズ確認

### 1. iOS `SampleScreen.destination` への `@MainActor` 付与 — **クローズ**

`samples/ios/KsSettingsViewSample/SampleScreen.swift:50` に `@MainActor` が付き、なぜ必要か (各 View の `init()` が MainActor 分離) と影響がない理由 (呼び出し元 ContentView は既に MainActor 文脈) がコメントで自己完結している。

レビュー側で新規 DerivedData を用意して full build し、警告を独立に再集計した。

| ファイル | 警告数 | 内容 |
|---|---|---|
| `BasicCellsDemoView.swift` | 18 | `main actor-isolated property ... from a Sendable closure` |
| `UnifyCellCommonFieldsDemoView.swift` | 8 | 同上 |
| `VisibilityDemoView.swift` | 4 | 同上 |
| `InputCellsDemoView.swift` | 3 | 同上 |
| `SampleScreen.swift` | **0** | — |
| 合計 | **33** | 全件が本変更以前からの同一パターン |

`** BUILD SUCCEEDED **`。review-001 が挙げた 2 件は消え、`@MainActor` 付与による**新規警告は 1 件も発生していない** (`SampleTheme` の `static let` 群も `UIColor` / `Theme` が Sendable のため concurrency 警告なし)。他箇所への波及も `destination` の呼び出し元が `ContentView.swift:22` / `:28` の 2 箇所のみで、いずれも `body` 内 = MainActor 文脈のため影響なし。

### 2. `accentColor` の共通パレット化 — **クローズ (独立検証済み)**

- `samples/ios/KsSettingsViewSample/SampleTheme.swift:50-61` と `samples/android/.../SampleTheme.kt:63-79` が同一の 6 値を定義。
- 両デモ画面 (`UnifyCellCommonFieldsDemoView.swift:71/83/93/106/114/126` ⇔ `UnifyCellCommonFieldsDemoScreen.kt:75/85/98/110/118/129`) が対応する定数を参照。**Cell と定数の割り当ても 1 対 1 で一致**している。
- samples 配下に semantic color / ハードコード色の残存がないことを grep で確認 (`UIColor.system*` は SampleTheme のコメント内のみ、`Color(0x...)` リテラルは SampleTheme.kt 以外に無し)。`MinimalDiffableDemoView.swift` の `.systemBackground` / `.systemGroupedBackground` は iOS 固有の検証画面 (規約の一致対象外) の地の色なので対象外。

値の正当性をレビュー側で独立に再測定した (iOS 26.5 シミュレータ / `resolvedColor(with: UITraitCollection(userInterfaceStyle: .light))`):

| 定数 | 実測 | SampleTheme の値 | 判定 |
|---|---|---|---|
| `systemOrange` | #FF8D28 | #FF8D28 | 一致 |
| `systemPurple` | #CB30E0 | #CB30E0 | 一致 |
| `systemTeal` | #00C3D0 | #00C3D0 | 一致 |
| `systemPink` | #FF2D55 | #FF2D55 | 一致 |
| `systemGreen` | #34C759 | #34C759 | 一致 |
| `systemBlue` | #0088FF | #0088FF | 一致 |

**MAUI 互換 Theme 定数との混線もない** — 共有パレットは `// MARK: - 共通フィールド統合デモ用の共有アクセントパレット` 以下に `demoAccent*` / `demoTitleBlue` という別プレフィックスで分離され、`maui*` 系 7 定数 (F2EFE6 / FFFFFF / E6DAB9 / 50FFBF00 / FFBF00 / CC9900 / 999999) と `Theme` の組み立ては一切変わっていない。`maui*` を参照するのは `SampleTheme.maui` と `BasicCellsDemoView.swift:168` / `BasicCellsDemoScreen.kt:184` の `CellStyle(titleColor:)` のみで、統合デモからは参照されていない。

spec 範囲外の対応である点と、iOS が dark mode 追随を失う点は deviation.md:8 に記録済み。共通フィールド統合デモは承認済み mock の対象外 (mock はルートメニューと入力 Cell 5 種デモのみ) なので、mock との齟齬も発生していない。

### 3. `androidx.fragment:fragment-ktx:1.8.5` → `androidx.fragment:fragment:1.8.4` — **クローズ**

`samples/android/app/build.gradle.kts:79-85`。本体 `android/ks-settingsview-ui/build.gradle.kts:93` が `fragment-ktx:1.8.4` を宣言しており、`fragment-ktx:1.8.4` は `fragment:1.8.4` に依存するため**解決バージョンが完全に一致**する (Sample だけ上がる状態が解消)。KTX 拡張を使っていないこと・非 KTX 版を選んだ理由・バージョンを揃える理由がコメントに残っており単独で読める。`:app:assembleDebug` 成功。

### 4. `collectAsState()` → `collectAsStateWithLifecycle()` — **クローズ**

`StoreDemoScreen.kt:62` + `:19` の import。追加依存 `androidx.lifecycle:lifecycle-runtime-compose:2.8.6` (`build.gradle.kts:111-113`) は本体 `ks-settingsview-ui` の `lifecycle-runtime-ktx:2.8.6` と同バージョンで、他依存 (Compose BOM 2024.10.01 / navigation-compose 2.8.4 / activity-compose 1.9.3) とも整合する。Compose BOM は `androidx.compose.*` のみを管理し `androidx.lifecycle:*` は含まないため、明示バージョン指定は正しい。`:app:compileDebugKotlin --rerun-tasks` で **Kotlin 警告 0 件**。

### 5. tasks.md 3.1 / 3.2 — **クローズ (虚偽チェックなし)**

`git diff` で確認した tasks.md の差分は**チェックボックスのみ** (18 行、`[ ]` → `[x]`)。文面の書き換えはない。3.1 が実際に実施された裏付けとして、3.1 でしか出てこない新規所見が deviation.md:16 に「tasks 3.1 の対照確認で検出」として追加されている (既定 titleColor の platform 差)。3.2 についても、レビュー側で両 platform の明示色パラメータを総当たりで突き合わせた結果、deviation.md 未記録の不一致は検出されなかった。

**足場アーティファクトの凍結**: `git diff --stat kasane/` は tasks.md 1 ファイルのみ。proposal.md / design.md / specs/ / ui/ は未変更。

---

## 指摘事項

### [🟡 Minor / 優先度低] deviation.md:16 の iOS 既定 ButtonCell 色が事実と異なり、同一 change 内で自己矛盾している

**該当箇所**: `kasane/changes/align-sample-parity/deviation.md:16`

**問題点**: 記録は「iOS は `tintColor` 由来の青 `#007AFF`」としているが、2 点とも正しくない。

1. **色値が違う。** iOS 側の既定は `Theme.defaultButtonTitleColor = .systemBlue` (`ios/Sources/KsSettingsViewUI/Theme.swift:275`) で、iOS 26.5 の `systemBlue` を light appearance で解決すると **#0088FF** (レビュー側で実測)。`#007AFF` は本体の別定数 `Theme.defaultAccentColor` (`Theme.swift:239-240` の `UIColor(red: 0.0, green: 0.478, blue: 1.0, ...)`、コメントに「おおよそ #007AFF」) の値であり、両者の取り違えと思われる。
2. **出処が違う。** `tintColor` 由来ではなく `Theme.defaultButtonTitleColor` の 4 段解決の最終フォールバックである (`EffectiveStyle.effectiveButtonTitleColor` 経由)。`UIView` の既定 `tintColor` もたまたま #0088FF だが、本体はそれを参照していない。

その結果、**同じ change の中に iOS の systemBlue について #0088FF (`SampleTheme.swift:60`) と #007AFF (`deviation.md:16`) の 2 つの値が併存**している。この記録は ksn-distill で本体の後続課題 (既定色の platform パリティ) へ引き継がれるため、誤った値と出処のまま流すと後続の調査を無駄に迷わせる。

なお、**「Sample 側では解消できない・後続で既定色を揃えるか判断する」という結論自体は妥当**で、是正すべきは記録の値と出処のみ。「Android は Material 3 `colorScheme.primary` 由来の紫 `#6750A4`」の部分は、Sample が独自 ColorScheme を持たず manifest が `Theme.Material3.DayNight.NoActionBar` (baseline) を指定しているため正しい。

**推奨修正**: 当該行の「iOS は `tintColor` 由来の青 `#007AFF`」を「iOS は本体の `Theme.defaultButtonTitleColor` (= `UIColor.systemBlue`、iOS 26.5 の light appearance で `#0088FF`) 由来の青」に書き換える。

（副産物として後続変更の課題も 1 件見つかった — 下記「後続変更の課題 (e)」）

---

### [🔵 Suggestion] 修正で触れた `build.gradle.kts` に、実際の manifest と食い違うコメントが残っている

**該当箇所**: `samples/android/app/build.gradle.kts:93-94`

**問題点**: `com.google.android.material:material` の依存コメントが「AndroidManifest.xml で `Theme.MaterialComponents.DayNight.NoActionBar` を指定するために必要」と書いているが、実際の `samples/android/app/src/main/AndroidManifest.xml:27` は `@style/Theme.Material3.DayNight.NoActionBar`。本変更以前からの陳腐化だが、今回の修正サイクルで同ファイルに 2 ブロック追記しており、周辺コメントの整合を取る機会ではある。本体 UI 層は `MaterialSwitch` の `materialSwitchStyle` 解決のため Theme.Material3.* を要求するので、コメントを信じて MaterialComponents テーマに戻すと壊れる。

**推奨修正**: コメントの `Theme.MaterialComponents.DayNight.NoActionBar` を `Theme.Material3.DayNight.NoActionBar` に直す。本変更のスコープに入れたくなければ後続で可。

---

## 後続変更の課題 (本体ライブラリ — 今回の変更対象外)

review-001 の (a)〜(d) は既出のため再掲しない。今回のレビューで新たに 1 件。

- **(e) 本体 iOS の既定青が 2 系統に分裂している。** `Theme.defaultAccentColor` はハードコードの `UIColor(red: 0.0, green: 0.478, blue: 1.0, ...)` (≒ #007AFF) だが、`Theme.defaultButtonTitleColor` は semantic な `.systemBlue` (iOS 26.5 で #0088FF)。iOS 18 以前は両者がほぼ同値だったが、iOS 26 の system color 刷新で **同一画面上のアクセント色と ButtonCell の既定タイトル色が別の青になる**。(d) の既定色パリティ判断と同じ後続変更で、iOS 内部の一貫性としても扱うのが自然。

---

## 確認した観点 (問題なし)

- **ビルド**: iOS `xcodebuild -scheme KsSettingsViewSample -configuration Debug` (新規 DerivedData) → `** BUILD SUCCEEDED **`。Android `:app:assembleDebug` 成功、`:app:compileDebugKotlin --rerun-tasks` で `w:` 行 0。テストは本変更の対象外 (tasks.md 冒頭の但し書きどおり)。
- **修正差分の副作用**: `@MainActor` は `destination` 1 プロパティに限定され、`title` / `demos` / `verifications` は nonisolated のまま (ContentView 以外からの参照も無し)。パレット追加は既存 `maui*` 定数・`Theme` 生成に一切触れていない。依存追加 2 件はいずれも既存依存とバージョン競合しない。
- **明示色パラメータの platform 総当たり照合**: samples 配下の `accentColor` / `titleColor` / 色リテラルを両 platform で列挙して突き合わせ、不一致 0 件 (7 箇所すべてが対応する `SampleTheme` 定数を参照)。
- **共通フィールド統合デモの表示文言**: 修正で両 platform とも書き換えた画面なので再照合した。Section header 5 件・Cell の title / description / valueText / hintText すべて一字一句一致。初期状態 (switch1=true / switch2=false / checkbox1=false / simpleCheck1=true / simpleCheck2=false / selectedTheme="dark") も一致。
- **足場アーティファクトの凍結**: proposal / design / specs / ui は未変更。tasks.md はチェックボックスのみ。deviation.md はオーナー合意分の追記 (新規ファイル)。
- **spec との整合**: 今回の修正はいずれも samples-ios / samples-android デルタスペックの Requirement を後退させていない。`accentColor` の共通パレット化は spec に記述のない範囲外対応だが、共通フィールド統合デモの Requirement は hintText と Section header のみを規定しており抵触せず、deviation.md:8 に合意として記録済み。
- **Android 側スタイル指定の差**: iOS は `KsSettingsView` の `style` 既定値が `.classic` (`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:63/76`)、Android は全画面で `KsSettingsViewStyle.Classic` を明示。規約 sample-parity.md「許容される差異」の「引数名・型名・リソース指定方法」に該当し、実効値は同一。
- **lessons**: `kasane/lessons/` に昇格済み scope ファイルなし (inbox に spec-review scope の 1 件のみ)。code-review 用の重点観点・除外観点は未定義。

## アクションプラン

1. (蒸留前に) deviation.md:16 の iOS 既定 ButtonCell 色を `Theme.defaultButtonTitleColor` (= `UIColor.systemBlue` / #0088FF) 由来に訂正する。
2. (任意) `build.gradle.kts:93-94` のコメントを `Theme.Material3.DayNight.NoActionBar` に直す。
3. (後続) 上記「後続変更の課題 (e)」を、review-001 (d) の既定色パリティ判断と同じ後続変更に含める。

1 は 1 行の記録訂正で、コードの再検証を必要としない。judgement を APPROVED としたのはこのため — 出荷される samples の状態は spec と規約に対して収束している。
