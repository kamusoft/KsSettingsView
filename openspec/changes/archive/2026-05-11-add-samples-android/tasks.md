## 依存関係

- 前提:
  - `add-monorepo-foundation`（archive 済）: `samples/android/README.md` placeholder が存在
  - `add-settings-view-core`（archive 済）: `Cell` プロトコル / `SettingsRoot` / `Section` / `CellStyle` / `Theme` 等のモデルを使用
  - `refactor-accessory-and-root-hf`（archive 済）: `KsAnyView` / `SectionAccessory` を使用
  - `add-settings-view-android-ui`（提案中・未実装）: `ks-settingsview-ui` の `KsSettingsView` / `Cell` / `CellViewHolder` / `KsCellRegistry` 等の公開 API、および `ks-settingsview-compose` の `@Composable KsSettingsView` ラッパ・DSL を使用
- 実装着手順序:
  - 本提案の **実装着手は `add-settings-view-android-ui` の archive 完了後**とする（design.md Decision 9）
  - 変更提案アーティファクトの作成（proposal / design / specs / tasks）は先行してよい
- 後続:
  - `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`: 本 Sample にページ追加する

## 1. Android Studio プロジェクト作成

- [x] 1.1 `samples/android/` 配下に Android Studio プロジェクトを作成（テンプレート: Empty Activity, Language: Kotlin, Build configuration: Kotlin DSL）
- [x] 1.2 Application ID を `jp.kamusoft.kssettingsview.samples.android` に設定
- [x] 1.3 App Label を `KsSettingsView Sample` に設定
- [x] 1.4 minSdk を 29、targetSdk / compileSdk を最新安定版に設定
- [x] 1.5 Kotlin / Java JVM ターゲットを 17 に設定
- [x] 1.6 Compose を有効化（`buildFeatures.compose = true`、Compose BOM 最新安定版）
- [x] 1.7 不要な自動生成ファイル（テストモジュール等）を整理する

## 2. KsSettingsView プロジェクト参照（Gradle composite build）

- [x] 2.1 `samples/android/settings.gradle.kts` に `includeBuild("../../android")` を追加
- [x] 2.2 `samples/android/app/build.gradle.kts` の `dependencies` に `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` を `implementation(...)` で追加
- [x] 2.3 `import jp.kamusoft.kssettingsview.core.*` / `.ui.*` / `.compose.*` がコンパイル可能であることを確認

## 3. ComponentActivity エントリポイント

- [x] 3.1 `class MainActivity : ComponentActivity()` を実装し、`onCreate` から `setContent { ... }` を呼ぶ
- [x] 3.2 `AndroidManifest.xml` の `<application>` に `MainActivity` を `LAUNCHER` として登録

## 4. Sample 専用 Cell の定義と登録

- [x] 4.1 Sample アプリ内に `data class SampleLabelCell(val id: String, val style: CellStyle, val title: String) : Cell` を定義（プロパティの初期値は適宜。`id` は Android の `Cell` インターフェース要件に従い `String` 型）
- [x] 4.2 Sample アプリ内に `class SampleLabelCellViewHolder(view: View) : CellViewHolder<SampleLabelCell>(view)` を定義し、`bind(cell, theme)` で `view.findViewById<TextView>(...).text = cell.title` 程度の最小描画を実装
- [x] 4.3 `bind` で再利用時の状態残りを防ぐため、`reset()` または既定実装で安全に再描画されることを確認
- [x] 4.4 `MainActivity.onCreate` または `Application.onCreate` で `KsCellRegistry.register(cellType: SampleLabelCell::class, viewHolderFactory: ::SampleLabelCellViewHolder)` を呼び出して登録

## 5. デモ画面の Composable

- [x] 5.1 `MainActivity.onCreate` 内の `setContent { ... }` ブロックで、`remember { settingsRoot { section { ... } } }` 等の方法で `SettingsRoot` を保持
- [x] 5.2 `Section` の `header` に `SectionAccessory.Text("PoC Section")` を設定
- [x] 5.3 `Section` の `footer` に `SectionAccessory.Text("This is a footer")` を設定
- [x] 5.4 Section 内に `SampleLabelCell` を 3 行（`id`（String 値、例: `"sample-1"` `"sample-2"` `"sample-3"` または `UUID.randomUUID().toString()`）と `title` が異なる）配置
- [x] 5.5 `KsSettingsView(root = root, modifier = Modifier.fillMaxSize())` を呼び出して画面いっぱいに表示

## 6. README 整備

- [x] 6.1 `samples/android/README.md` の placeholder を削除
- [x] 6.2 「概要」セクションを記載（このサンプルアプリが何を示すか）
- [x] 6.3 「必要環境」セクションを記載（Android Studio / JDK 17 / Android SDK API 29+）
- [x] 6.4 「開き方」セクションを記載（Android Studio で `samples/android/` を開く）
- [x] 6.5 「実行手順」セクションを記載（Android Studio から Run、または `./gradlew :app:installDebug`）
- [x] 6.6 「ディレクトリ構成」セクションを記載（簡易ツリー）
- [x] 6.7 「関連リンク」セクションを記載（`add-settings-view-android-ui` 提案 / `docs/android-ui.md` などへのリンク）
- [x] 6.8 「本体ライブラリのデバッグ」セクションを記載：本 Sample は `includeBuild("../../android")` による Gradle composite build で本体モジュール（`ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose`）をソース参照するため、本体ソースにブレークポイントを置いてステップイン可能。本体テストを主軸に走らせる場合は `android/` を直接 Android Studio で開く運用と、Sample で動作確認しながら本体を編集する場合は `samples/android/` を Android Studio で開く運用、両者を使い分ける旨を明記

## 7. 動作確認

- [x] 7.1 Android Studio から API 29+ エミュレータでビルドが成功することを確認
- [x] 7.2 Android Studio から Run（Shift+F10）して、エミュレータで Sample アプリが起動することを確認
- [x] 7.3 起動直後の画面に `SampleLabelCell` の `title` が複数行（3 行）描画されることを目視確認
- [x] 7.4 Section ヘッダ "PoC Section" と Section フッタ "This is a footer" が描画されることを目視確認
- [x] 7.5 `cd samples/android && ./gradlew :app:assembleDebug` がコマンドラインから成功することを確認

## 完了条件

- すべてのタスクのチェックボックスが完了している
- `samples-android` capability の全 Scenario が満たされている
- `samples/android/` を Android Studio で開き、API 29+ エミュレータで Run すると、`SampleLabelCell` を含む 1 セクションのデモ画面が描画される
- `samples/android/README.md` が実 Sample 用のクイックスタートに置き換わっている
- `./gradlew :app:assembleDebug` のコマンドラインビルドも成功する
