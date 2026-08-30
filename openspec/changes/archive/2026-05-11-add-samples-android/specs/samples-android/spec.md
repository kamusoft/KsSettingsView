## ADDED Requirements

### Requirement: Android Sample アプリの存在

`samples/android/` 配下に Jetpack Compose ベースの Sample アプリが Android Studio プロジェクト形式で存在しなければならない (SHALL)。Sample アプリは `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` を依存し、Android Studio から開いて Android エミュレータ（API 29+）で起動可能でなければならない (MUST)。

#### Scenario: Android Studio プロジェクトの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `samples/android/` 配下を確認する
- **THEN** `settings.gradle.kts`、`build.gradle.kts`（root および app モジュール）、`MainActivity` の Kotlin ソースを含む Android Studio プロジェクト構造が存在する

#### Scenario: KsSettingsView パッケージへの依存

- **GIVEN** `samples/android/settings.gradle.kts`
- **WHEN** その内容を確認する
- **THEN** `includeBuild("../../android")` などの composite build 設定により、`ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` の 3 モジュールが Sample アプリから依存可能になっている

#### Scenario: エミュレータでの起動

- **GIVEN** Android Studio でプロジェクトを開いた状態
- **WHEN** Android エミュレータ（API 29+）をターゲットに Run（Shift+F10）を実行する
- **THEN** ビルドが成功し、エミュレータ上で Sample アプリが起動する

### Requirement: Sample 専用 Cell の定義と登録

Sample アプリは、`ks-settingsview-ui` モジュール内 `internal` の `PocLabelCell` に依存せず、Sample アプリ内に独自の `SampleLabelCell`（`Cell` 準拠 / `id` / `style` / `title` の最小プロパティを持つ）と `SampleLabelCellViewHolder`（`CellViewHolder<SampleLabelCell>` 派生）を定義しなければならない (SHALL)。アプリ起動時に `KsCellRegistry.register(...)` を呼び出して登録しなければならない (MUST)。

#### Scenario: SampleLabelCell の存在

- **GIVEN** Sample アプリのソースコード
- **WHEN** Cell モデル定義を確認する
- **THEN** `Cell` プロトコルに準拠した `SampleLabelCell` 型（`data class` または同等）が Sample アプリ内に定義されている

#### Scenario: SampleLabelCellViewHolder の存在

- **GIVEN** Sample アプリのソースコード
- **WHEN** ViewHolder 定義を確認する
- **THEN** `CellViewHolder<SampleLabelCell>` を継承した `SampleLabelCellViewHolder` クラスが Sample アプリ内に定義されている

#### Scenario: KsCellRegistry への登録

- **GIVEN** Sample アプリの起動シーケンス
- **WHEN** `MainActivity.onCreate` または `Application.onCreate` の処理を確認する
- **THEN** `KsCellRegistry` に `SampleLabelCell` 型 → `SampleLabelCellViewHolder` ファクトリのマッピングが登録されている

### Requirement: SampleLabelCell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`ks-settingsview-compose` の `@Composable`）を使い、`SampleLabelCell` を 1 セクション・複数行含む `SettingsRoot` を描画しなければならない (SHALL)。

#### Scenario: 起動時の画面表示

- **GIVEN** Sample アプリがエミュレータで起動した直後
- **WHEN** 画面のコンテンツを確認する
- **THEN** `KsSettingsView` が画面いっぱいに表示され、`SampleLabelCell` の `title` を含む 1 行のセルが複数行（2 行以上）描画される

#### Scenario: Section ヘッダ・フッタの描画

- **GIVEN** Sample アプリがエミュレータで起動した直後
- **WHEN** 描画されたセクションを確認する
- **THEN** `SectionAccessory.Text(...)` 形式のヘッダおよびフッタが、対応する文字列でセクション境界に表示される

#### Scenario: Compose DSL の使用

- **GIVEN** Sample のソースコードを参照する
- **WHEN** `MainActivity` または同等の起点 Composable を確認する
- **THEN** `MainActivity` 内で `remember { settingsRoot { section { ... } } }` 等の方法で `SettingsRoot` が保持され、`KsSettingsView(root = root, ...)` を呼び出している

### Requirement: README の整備

`samples/android/README.md` は、`add-monorepo-foundation` で配置された placeholder から、実 Sample アプリのクイックスタート README に置き換えられていなければならない (SHALL)。

#### Scenario: クイックスタートの記載

- **GIVEN** `samples/android/README.md` を開く
- **WHEN** その内容を確認する
- **THEN** 「概要」「必要環境（Android Studio / JDK 17 / Android SDK API 29+）」「開き方（Android Studio でプロジェクトを開く手順）」「実行手順（Run / `./gradlew :app:installDebug` 等）」「ディレクトリ構成」「関連リンク」のいずれにも該当する記載が含まれている

#### Scenario: placeholder からの置き換え

- **GIVEN** `samples/android/README.md`
- **WHEN** その内容を確認する
- **THEN** 「後続変更提案で追加予定」等の placeholder 文言は残っておらず、実 Sample 用のクイックスタートに更新されている

#### Scenario: 本体ライブラリのデバッグ手順の記載

- **GIVEN** `samples/android/README.md`
- **WHEN** その内容を確認する
- **THEN** 「本体ライブラリのデバッグ」セクションが存在し、本 Sample が `includeBuild` による Gradle composite build で本体モジュール（`ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose`）にブレークポイントを置いてステップインできる旨と、本体テストを主軸に走らせる場合は `android/` を直接 Android Studio で開く運用が併記されている

### Requirement: アプリのメタデータ

Sample アプリは、Application ID プレフィックスとして `jp.kamusoft.kssettingsview.samples.android` を使用しなければならない (SHALL)。minSdk は 29 以上、Kotlin / Java JVM ターゲットは 17 でなければならない (MUST)。

#### Scenario: Application ID の確認

- **GIVEN** `samples/android/app/build.gradle.kts`
- **WHEN** `applicationId` を確認する
- **THEN** `jp.kamusoft.kssettingsview.samples.android` で始まる識別子が設定されている

#### Scenario: minSdk の確認

- **GIVEN** `samples/android/app/build.gradle.kts`
- **WHEN** `defaultConfig.minSdk` を確認する
- **THEN** `29` 以上の値が設定されている

#### Scenario: JVM ターゲットの確認

- **GIVEN** `samples/android/app/build.gradle.kts`
- **WHEN** `compileOptions.targetCompatibility` および `kotlinOptions.jvmTarget` を確認する
- **THEN** いずれも `17` に相当する値が設定されている
