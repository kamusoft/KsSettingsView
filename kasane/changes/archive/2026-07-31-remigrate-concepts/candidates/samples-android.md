# Candidate: samples-android

## 概念候補

### 実行可能な Sample と platform-local source reference (提案カテゴリ: architecture/、`samples-ios` / `monorepo-foundation` と統合)

Android Sample はライブラリ本体の配布物や挙動契約の SSoT ではなく、利用者アプリと同じ境界から公開 API を組み合わせて実行できる reference である。`samples/android/` は独立した Android application build として `android/` の3モジュールを source reference し、IDE / emulator / debugger で統合状態、視覚、操作結果を確認できる。

Sample 固有の画面名・表示文字列・デモの並びを長命な製品契約にはしない。一方、利用者が再導出しにくい「どの公開入口をどう組み合わせるか」「ホスト側で何を満たす必要があるか」は concepts へ残す価値がある。Android 固有の詳細は既存 [Android Compose Bridge と宣言 DSL](../../../concepts/platforms/android-compose.md)、[Android Native Host の利用と更新境界](../../../concepts/platforms/android-native-host.md)、[基本 Cell](../../../concepts/cells/basic-cells.md)、[入力 Cell](../../../concepts/cells/input-cells.md) に合流させ、この候補自体から `samples-android` 独立概念は作らないことを提案する。

#### 責務境界

- Sample は consumer-shaped な application module、起動導線、デモ用状態、公開 API の利用例を所有する。
- ライブラリ本体のコードとテストは Store、DSL identity、Registry、Theme / `CellStyle`、Cell 操作、visible projection の挙動契約を所有する。Sample の目視確認は自動回帰テストを置き換えない。
- `samples/android/settings.gradle.kts` は `includeBuild("../../android")` と明示的な dependency substitution で `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` を本体 Project へ接続する。Sample の app dependency は Maven 座標の形を保つため、利用者側の依存境界を保ちながら本体ソースへ step-in できる。
- `MainActivity` と Navigation Compose はデモの分離と戻る導線だけを担い、設定画面の描画基盤は Compose `KsSettingsView` から Android Native Host へ収束する。

#### 実行可能に示す公開 API

| デモの責務 | 実行可能に示す契約 | 合流先 |
|---|---|---|
| Store 方式 | `remember { SettingsRootStore(initialRoot = settingsRoot { ... }) }`、`KsSettingsView(store = ...)`、`state.collectAsState()`、`insertCell` / `removeCell` による命令型部分操作 | `platforms/android-compose.md` / `platforms/android-native-host.md` |
| DSL 方式 | `KsSettingsView { Section { ... } }`、Root H/F、`KsIdentifiable` による `forEach(items)` の key 省略、`cellHeight` modifier、state 変更による再評価 | `platforms/android-compose.md` |
| 基本 Cell | 基本7種の callback と外部状態所有、`KsImage.Resource`、`CellStyle`、Compose `Color` から構築した `Theme` の明示渡し | `cells/basic-cells.md` / `cells/ks-image.md` / styling 横断概念 |
| 共通フィールド | `description` / `valueText` / `icon` / `hintText`、Cell 固有 `accentColor`、`CellTitleAlignment` の組み合わせ | `cells/basic-cells.md` / styling 横断概念 |
| 可視性 | `Section.isVisible` / Cell `isVisible` を外部 state で切り替え、hidden を model に残した visible projection の更新を観察する | `platforms/android-compose.md` / 表示状態同期の横断概念 |
| 入力 Cell | 入力5種の `MutableState` TwoWay helper、Android Native 型 `InputType` / `LocalTime` / `LocalDate`、`DatePickerUIStyle.Material` / `Spinner` | `cells/input-cells.md` / `platforms/android-compose.md` |

Sample は `KsSettingsViewStyle.Classic` と `Theme` を別の入口として渡す。基本 Cell デモの MAUI 互換配色は Sample の比較・移植確認用であり、ライブラリ既定 Theme を変更する契約ではない。`Theme` / `CellStyle` は UI 層の Compose Native 型を直接使い、旧 `KsColor` のような論理色を経由しない。

#### Android host の前提と Registry の利用例

- app の XML Theme は `Theme.Material3.*` 派生でなければならない。Compose の `MaterialTheme` だけでは、Native Host が作る `MaterialSwitch` / `MaterialCheckBox` 等の theme attribute を満たさない。
- `MainActivity.onCreate` は `KsSettingsView` を構築する前に `KsCellRegistry.strictMode = BuildConfig.DEBUG` を設定する。未登録 Cell を Debug で早期検出し、Release では高さ0の placeholder へ退避する利用側ポリシーの実例である。
- 標準の基本7種と入力5種は `KsSettingsView` の初回構築時に自動登録されるため、Sample は標準 Cell を手動登録しない。利用者定義 Cell だけが事前の `KsCellRegistry.register` を必要とする。

#### 保証すること

- Android Sample を本体とは独立した build root として開き、API 29+ の application として構築できる consumer 境界を保つ。
- 本体3モジュールを local source reference し、本体変更を Sample で確認して本体ソースへ step-in できる。
- Store と DSL の両入口が同じ Android Native Host の公開描画経路を使うことを、実行可能な利用例として示す。
- Material3 XML Theme、Registry strict mode、外部状態所有など、ライブラリを app へ組み込む側の条件を Sample 自身で満たす。

#### してはいけないこと

- Sample の画面数、ナビゲーション文言、デモデータ、MAUI 比較用の色値をライブラリ本体の挙動契約にしない。
- Sample の目視確認を Store / DSL / Cell / styling の自動テストの代わりにしない。
- Compose `MaterialTheme` を設定しただけで Native Host の Material3 XML Theme 前提を満たしたと考えない。
- 標準 Cell 12種の利用例で利用者定義 Cell の Registry 登録手順まで実証していると説明しない。

出典: `samples/android/settings.gradle.kts` / `samples/android/app/build.gradle.kts` / `samples/android/app/src/main/AndroidManifest.xml` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt` / `BasicCellsDemoScreen.kt` / `UnifyCellCommonFieldsDemoScreen.kt` / `VisibilityDemoScreen.kt` / `InputCellsDemoScreen.kt`、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` / `KsIdentifiable.kt`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` / `KsCellRegistry.kt` / `Theme.kt` / `CellStyle.kt`、対応 tests、`openspec/specs/samples-android/spec.md` Purpose（TBD のため意図回収不可）、`samples/android/README.md`。

## ADR 候補

- なし。Sample を単一リポジトリ内の独立 platform build とし local source reference で本体へ接続する方針は ADR-0001「モノレポとプラットフォーム別ビルドルート」に包含される。Android Native Host に Material3 派生 XML Theme を要求する判断は Batch C で新規 ADR 候補として記録済みだが、採用理由・代替案を備えた design 出典がないため、この capability から重複候補を追加しない。

## drift 所見

1. frozen spec の Purpose は `TBD` のままで、Sample が「consumer 境界の実行可能 reference」として存在する意図を回収できない。(`openspec/specs/samples-android/spec.md` Purpose / `samples/android/README.md` / Sample code)
2. frozen spec の Requirement 本文は「起動直後の画面」が `KsSettingsView` と Store の Cell 一覧であるとするが、現行 app の start destination は `MenuScreen` であり、Store デモはメニュー選択後に表示される。同じ Requirement 配下の Scenario はメニュー選択を前提にしており、spec 内部でも記述が割れている。(`openspec/specs/samples-android/spec.md`「基本 Cell を含むデモ画面」 / `MainActivity.kt`)
3. README の概要・実行成功条件・ディレクトリ構成は Store / DSL / 基本 Cell の3デモと Kotlin 2ファイルだけを列挙するが、現行メニューは共通フィールド、`isVisible`、入力 Cell を加えた6デモ、Kotlin 5ファイルを持つ。標準 Cell の自動登録も基本7種だけでなく入力5種を含む。(`samples/android/README.md` / `MainActivity.kt` / `UnifyCellCommonFieldsDemoScreen.kt` / `VisibilityDemoScreen.kt` / `InputCellsDemoScreen.kt` / `KsSettingsView.kt`)
4. frozen spec の MAUI 互換 Theme は旧フィールド `viewBackgroundColor` / `titleColor` を要求するが、現行 `Theme` と Sample は `backgroundColor` / `cellTitleColor` を使う。また RadioCell の例は旧 `isSelected` を使うが、現行 API は `value` / `selectedValue` で選択を表す。(`openspec/specs/samples-android/spec.md` / `BasicCellsDemoScreen.kt` / `Theme.kt` / `RadioCell.kt`)
5. frozen spec は罫線、Ripple、右端 X 座標、Switch thumb / track、長文折返し、固定・可変行高などライブラリ本体の描画契約を Sample capability の Requirement に含める。Sample code はこれらを実装せず公開 Cell を配置して観察するだけであり、契約と回帰検証は Android Host / styling / Cell のコードと tests が所有している。(`openspec/specs/samples-android/spec.md` / Sample code / `android/ks-settingsview-ui/src/main` / `src/test`)
6. frozen spec の JVM target Scenario は `kotlinOptions.jvmTarget` の存在を要求するが、現行 build は Java 17 の `compileOptions` と `kotlin { jvmToolchain(17) }` を使い、`kotlinOptions` block を持たない。要求の意図は満たすが検証方法が現行 Gradle wiring と一致しない。(`openspec/specs/samples-android/spec.md`「JVM ターゲットの確認」 / `samples/android/app/build.gradle.kts`)
7. `docs/platform-guide-android.md` は利用者定義 Cell の詳細実装先として `samples/android/` の Cell 実装を案内するが、現行 Sample はライブラリ提供 Cell 12種だけを使い、独自 `Cell` / `CellViewHolder` / `KsCellRegistry.register` の実例を持たない。(`docs/platform-guide-android.md` §10 / `samples/android/app/src/main/kotlin/...`)

## 用語

- consumer-shaped Sample: ライブラリ内部からではなく、利用者 application と同じ依存・host 境界から公開 API を呼ぶ実行可能例。
- platform-local source reference: 各 ecosystem のローカル依存機構で Sample から開発中の本体ソースを参照すること。Android では Gradle composite build の `includeBuild` と dependency substitution を使う。
- Store 方式: 利用者が `SettingsRootStore` を保持し、公開操作で命令型更新する Compose 利用経路。
- DSL 方式: Compose state から `KsSettingsView { Section { ... } }` を再評価する利用経路。
- host Theme: `AndroidView` が受け取る Context の XML Theme。Compose `MaterialTheme` や KsSettingsView の `Theme` とは別物。
- 目視確認: emulator / device 上で統合状態、視覚、操作結果を観察する補助検証。自動回帰テストの代替ではない。

## 抽出メモ

- 概念候補は1件。ただし `samples-android` 固有ファイルを新設する提案ではなく、`samples-ios` / `monorepo-foundation` と合わせて旧 `architecture/repository-boundaries.md` の後継となる横断概念へ統合する材料である。
- Android Sample の公開 API 例はすでに `platforms/android-compose.md`、`platforms/android-native-host.md`、`cells/basic-cells.md`、`cells/input-cells.md` が現行コードに沿って詳述している。Sample 固有概念に複製すると更新箇所が増え、腐り度が高くなる。
- 長命層へ残す価値がある Sample 固有材料は、consumer 境界、platform-local source reference、目視確認と自動テストの責務分離である。デモ別の文字列・色値・画面構成は Sample code に任せる。
- Material3 host Theme は Sample 固有条件ではなく Android Native Host 全利用者の契約である。Registry strict mode、標準12種の自動登録、Store / DSL、Theme / Style も既存 Android platform / cells 概念へ合流させる。
- app の Gradle / SDK / IDE version の網羅列挙、resource ファイル一覧、Navigation Compose の個別 route は再導出コストが低いため概念候補に含めない。
