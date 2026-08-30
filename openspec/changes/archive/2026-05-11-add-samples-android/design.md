## Context

`add-monorepo-foundation` で `samples/android/` ディレクトリと placeholder の `README.md` のみが配置された状態で、`add-settings-view-android-ui`（実装予定）で構築する KsSettingsView (Core / UI / Compose) を実機・エミュレータで目視確認できる Sample アプリ本体が存在しない。本提案では、Sample アプリの土台（Android Studio プロジェクト + Compose Activity + KsSettingsView ローカル参照 + Sample 専用 `SampleLabelCell` の最小デモ）を独立 capability として確立する。具象 Cell（Label / Switch / Command 等）のデモページは後続の `add-cell-types-*` 群が「ページ追加」として担当するため、本提案ではあえて `SampleLabelCell` のみの最小構成にとどめる。

minSdk 29 / Kotlin / Jetpack Compose / AGP 8.7+ / JDK 17 が前提で、これは monorepo-foundation 規約に準拠する。Application ID プレフィックスは `jp.kamusoft.kssettingsview.samples.android` とし、KsSettingsView 本体（`jp.kamusoft.kssettingsview.*`）と区別する。

## Goals / Non-Goals

**Goals:**
- `samples/android/` 配下に Android Studio プロジェクト形式の Compose Sample アプリを配置
- `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` を **Gradle composite build (`includeBuild`)** によりローカル参照
- Sample 専用の Cell 型 `SampleLabelCell`（`Cell` 準拠）と Renderer `SampleLabelCellViewHolder`（`CellViewHolder<SampleLabelCell>` 派生）を Sample アプリ内に定義し、`KsCellRegistry` に登録
- `SampleLabelCell` を含む `settingsRoot { section { ... } }` の 1 ページを `MainActivity` から表示し、エミュレータ起動時に「タイトル付き 1 行のセル」が複数行描画される
- `samples/android/README.md` を実 Sample のクイックスタート README に置き換え（Android Studio で開く手順、エミュレータでの起動手順）
- 「placeholder のまま実 Sample が配置される」という monorepo-foundation review-result_002.md で言及された懸念を解消
- 後続の `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom` が「ページ追加」のみで Sample を拡張可能な構造（後続の追加が容易な「メニュー画面 → 各デモページ」の素地は本提案では用意せず、後続提案の判断に委ねる）

**Non-Goals:**
- 具象 Cell（Label / Switch / Command / Entry / Picker / Custom 等）のデモページ追加 → `add-cell-types-*` 群
- CI 連携（GitHub Actions などで Sample を build / boot する仕組み）
- スナップショットテスト / UI テスト
- Theme 切替 UI / Style（`Classic` / `Modern`）切替 UI
- タブレット専用レイアウト最適化（電話エミュレータでの動作確認を優先）
- Google Play 配布のためのアイコン / Splash Screen 整備（標準テンプレートで足りる範囲とする）

## Decisions

### Decision 1: Android Studio プロジェクト形式 + Gradle composite build 参照

**選択**: `samples/android/` を独立した Android Studio プロジェクト（`settings.gradle.kts` を持つ）として作成し、リポジトリルート相対の `android/` 配下のモジュールを `includeBuild("../../android")` で composite build 参照する。Sample から `import jp.kamusoft.kssettingsview.core.*` / `import jp.kamusoft.kssettingsview.ui.*` / `import jp.kamusoft.kssettingsview.compose.*` ができるようにする。

**理由**:
- `includeBuild` ならパッケージのソース変更が即時 Sample に反映され、開発サイクルが短い
- 公開 Maven Central の URL や Tag に依存しないため、開発中の `develop` ブランチでも常に最新ライブラリを試せる
- monorepo-foundation で `android/` 配下に既に `settings.gradle.kts` があり、別 Android Studio プロジェクトとしての `samples/android/` を独立させるほうが、Sample 単独で開ける利便性が高い

**代替案**:
- リモート Maven Central 参照（`implementation("jp.kamusoft:ks-settingsview-core:...")`）: 開発中はバージョン管理が煩雑。却下。
- `android/settings.gradle.kts` に Sample モジュールを `include` する: KsSettingsView 本体のビルドが Sample のビルドを巻き込むため、責務分離の観点で却下。
- Sample をモノリポ外に置く: 本リポジトリの monorepo 方針に反する。却下。

### Decision 2: Gradle KTS + Version Catalog（任意）

**選択**: ビルドスクリプトは Kotlin DSL（`build.gradle.kts`）を使用する。Version Catalog（`libs.versions.toml`）は本提案では必須としない（Sample 単独の小規模構成のため）。

**理由**:
- monorepo-foundation 規約で Kotlin DSL を採用済み
- Version Catalog は便利だが、Sample 単独では依存数が少なく、過剰設計を避ける

**代替案**:
- Groovy DSL: monorepo-foundation 規約に反するため却下。

### Decision 3: アプリ構造は ComponentActivity + setContent

**選択**: `class MainActivity : ComponentActivity()` をエントリポイントとし、`onCreate` から `setContent { ... }` で Compose 階層を構築する。`KsSettingsView` を画面いっぱいに表示する最小構成にする。

**理由**:
- Jetpack Compose 標準のエントリポイント
- Fragment や ViewBinding ベースの Activity を不要にし、コードの読みやすさを優先
- `KsSettingsView` の `@Composable` ラッパは `root: SettingsRoot` を引数で受け取り内部状態は持たない設計のため、`MainActivity` 側で `remember { ... }` または `mutableStateOf` で保持する

**代替案**:
- View ベース（`AppCompatActivity` + `setContentView(R.layout.xxx)`）: KsSettingsView 自体が `FrameLayout` 派生のため動作はするが、Compose ラッパの確認には Compose 経由が望ましい。

### Decision 4: Sample 専用 Cell `SampleLabelCell` の独自定義

**選択**: 既存の `PocLabelCell` / `PocLabelCellViewHolder`（`add-settings-view-android-ui` で配置予定、`ks-settingsview-ui` モジュール内 `internal`）は Sample から直接利用できないため、Sample アプリ内に以下を独自定義する：

- `data class SampleLabelCell(...) : Cell`（`id: String` / `style: CellStyle` / `title: String` のみ。Android の `Cell` インターフェースは `id: String` を要求するため `String` 型を使用）
- `class SampleLabelCellViewHolder(view: View) : CellViewHolder<SampleLabelCell>(view)`（`bind(cell, theme)` で `view.findViewById<TextView>(...).text = cell.title` 程度の最小描画）

これらは Sample アプリのモジュール内（`samples/android/app/src/main/java/...`）に配置し、Sample アプリ起動時（`Application.onCreate` または `MainActivity.onCreate`）に `KsCellRegistry.register(cellType: SampleLabelCell::class, viewHolderFactory: ::SampleLabelCellViewHolder)` を呼んで登録する。

**理由**:
- `add-settings-view-android-ui` で `PocLabelCell` / `PocLabelCellViewHolder` は `internal` として配置される計画（`ks-settingsview-ui` モジュール外からは参照不可）
- `Cell` プロトコルおよび `CellViewHolder` クラス、`KsCellRegistry` は `public` で公開される計画のため、Sample 側で外部から準拠する Cell 型を新規定義する難易度は低い
- これにより `add-settings-view-android-ui` の archive 後の改変（`PocLabelCell` を `public` 化する等）を行わずに Sample が成立する

**代替案**:
- 案 A: `PocLabelCell` を `public` に昇格させる。
  - 却下理由: `add-settings-view-android-ui` の design.md でも `PocLabelCell` は「内部 PoC で、後続具象 Cell 追加時に削除する」位置付け。`public` API として公開すると意図に反する。
- 案 B: Sample アプリを `ks-settingsview-ui` モジュール内に同梱する。
  - 却下理由: モジュール責務（Library / Sample）の境界を壊す。テスト性・保守性が低下する。

### Decision 5: 表示する SettingsRoot の内容

**選択**: 1 セクション・3 行程度の `SampleLabelCell` を含む `SettingsRoot` を `MainActivity.onCreate` 内で構築する。Section の `header` には `SectionAccessory.Text("PoC Section")` を、`footer` には `SectionAccessory.Text("This is a footer")` を設定する。Root H/F は本提案では設定しない（後続提案の判断に委ねる）。

**理由**:
- 動作確認には複数行があるとレイアウト確認がしやすい
- Section H/F の `Text` 形式は目視確認のため最小限設定する。`View` 形式（KsAnyView）は後続提案で扱う
- Root H/F は後続の add-cell-types-* または別途 Sample 拡張提案で扱う

**代替案**:
- 1 行のみの最小構成: 動作確認としては 1 行でも足りるが、複数行のほうがレイアウト確認がしやすい。
- Style 切替 UI を含める: Sample 土台の責務を超えるため、後続提案に委ねる。

### Decision 6: README の構成

**選択**: `samples/android/README.md` を以下のセクション構成で書き換える：
1. 概要（このサンプルアプリが何を示すか）
2. 必要環境（Android Studio Hedgehog 以上 / JDK 17 / Android SDK API 29+ / minSdk 29）
3. 開き方（Android Studio で `samples/android/` を開く）
4. 実行（Android Studio から Run、または `./gradlew :app:installDebug`）
5. ディレクトリ構成（簡易ツリー）
6. 関連リンク（KsSettingsView Core / UI / Compose README、`add-settings-view-android-ui` 提案へのリンク）

**理由**:
- monorepo-foundation review-result_002.md で「placeholder のまま実 Sample が配置される」リスクが指摘されており、明確に置き換える
- オンボーディング時間の短縮（Android Studio で開く → Run）

**代替案**:
- README なし: クイックスタート不能で却下。
- 詳細チュートリアル化: 本提案のスコープ外。後続で `docs/` に整備する。

### Decision 7: Application ID と表示名

**選択**:
- Application ID: `jp.kamusoft.kssettingsview.samples.android`
- App Label: `KsSettingsView Sample`
- minSdk: 29
- targetSdk / compileSdk: 最新安定版（実装時点）に追従
- Kotlin / Java JVM ターゲット: JDK 17
- Compose BOM: 最新安定版（実装時点）に追従

**理由**:
- monorepo-foundation のパッケージ ID プレフィックス規約 `jp.kamusoft.kssettingsview.*` に準拠
- minSdk 29 は KsSettingsView 本体の規約に一致

**代替案**:
- minSdk を 24 に下げる: KsSettingsView 本体（minSdk 29）と一致しないため、依存解決で失敗する。却下。

### Decision 8: テストモジュールは置かない

**選択**: 本提案では Sample 専用のテストモジュール（unit test / instrumented test）は配置しない。

**理由**:
- KsSettingsView 本体のテストは `android/ks-settingsview-*/src/test/` に既に配置される計画
- Sample の責務は「目視確認可能な最小アプリ」であり、自動テストは Non-Goals

**代替案**:
- UI Test を追加: スナップショットテストや UI Test は後続提案（CI 整備時）で扱うべき。本提案のスコープ外。

### Decision 9: 実装順序の制約

**選択**: 本 Sample 提案の **実装着手は `add-settings-view-android-ui` の archive 完了後**とする。本変更提案の作成（proposal / design / specs / tasks）は先行してよいが、実装フェーズ（`opsx:apply`）は依存提案完了後に行う。

**理由**:
- `ks-settingsview-ui` / `ks-settingsview-compose` モジュールが存在しない状態では Sample のビルドが通らない
- `add-settings-view-android-ui` の API（`KsSettingsView` / `Cell` / `CellViewHolder` / `KsCellRegistry` 等）が確定してから Sample を実装するほうが、API 不整合の手戻りが少ない

**代替案**:
- 本提案を先行実装する: モックや想定 API で進めることになり、`add-settings-view-android-ui` 確定後に大幅な書き換えが必要。却下。

## Risks / Trade-offs

- **Risk**: `add-settings-view-android-ui` の API が変更されると Sample の実装が手戻る
  - **緩和策**: 本提案の実装着手を `add-settings-view-android-ui` archive 後に限定する（Decision 9）
- **Risk**: `includeBuild` のパス（`../../android`）が壊れる可能性
  - **緩和策**: `samples/android/settings.gradle.kts` 内のパスを README に明記、CI で `./gradlew assembleDebug` を実行（CI は本提案の Non-Goals だが、後続提案で対応）
- **Risk**: Compose のバージョン整合性（`add-settings-view-android-ui` と Sample で Compose BOM が乖離）
  - **緩和策**: 双方とも最新安定版に追従し、必要なら `android/settings.gradle.kts` に共通版数を定義
- **Trade-off**: ComponentActivity を採用したことで、AppCompat 系の Theme / DayNight 切替を享受できない
  - **緩和策**: Compose の `MaterialTheme` で代替、現時点で問題なし
