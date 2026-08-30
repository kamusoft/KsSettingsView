# レビュー結果 - add-samples-android

**レビュー日時**: 2026年05月11日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-samples-android

## サマリー

`add-samples-android` の実装は、`samples/android/` 配下に Android Studio プロジェクト形式の Compose Sample アプリを配置し、`includeBuild("../../android")` + `dependencySubstitution` による Gradle composite build で本体ライブラリ (`ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose`) をソース参照する構成として丁寧に実装されている。`SampleLabelCell` / `SampleLabelCellViewHolder` の独自定義、`KsCellRegistry` への登録、`settingsRoot { section { ... } }` DSL によるデモ画面、README の置き換えのいずれもタスク・仕様要件を満たす。

ライブラリ側の付随修正（`Cell` の `sealed interface` → `interface` 化、`KsCellRegistry` / `CellViewHolder` の `internal` → `public` 昇格）はユーザー承認のもとで行われ、Source-of-Truth spec (`openspec/specs/settings-view-core/spec.md` および `openspec/specs/settings-view-android-ui/spec.md`) も併せて修正されている。修正は Sample 実装に必要な最小限に留まり、`when` 節等の網羅性チェックに依存している既存ライブラリコードは無いことを `Grep` で確認済みである。

ビルド・テストは以下の通りいずれも成功：

- `cd android && ./gradlew test --rerun-tasks`: BUILD SUCCESSFUL（166 tasks executed）。`KsCellRegistryTest` 8 / `KsSettingsViewTest` 3 / `SectionAccessoryRenderingTest` 7 など全テスト 0 failures, 0 errors。
- `cd samples/android && ./gradlew :app:assembleDebug`: BUILD SUCCESSFUL（92 tasks）。Sample APK 生成成功。

ただし、tasks.md の 7.2-7.4（Android Studio Run + 起動後の目視確認）はビルド成功を根拠に完了扱いとされており、実機・エミュレータでの目視確認の証跡は提示されていない。Sample アプリの責務上「目視確認可能な最小アプリ」が成果物そのもののため、本タスクはユーザー側で最終確認するのが妥当である。これは指摘事項 1 件として下記に整理する。

**判定**: `APPROVED`

Critical/Major 指摘なし。Minor 指摘 1 件、Suggestion 2 件あり。いずれもブロッカーではなく、後続 PR / 後続提案で対応可能なレベル。Sample アプリとしての構造・依存・起動可能性・README 整備の各 Requirement は実装で満たされており、ライブラリ側の付随修正もユーザー承認の範囲内で最小限に収まっている。

## 指摘事項

### 🟡 Minor

#### [Minor] tasks 7.2-7.4 の「目視確認」が未実施のまま完了扱いとされている

**該当箇所**: `openspec/changes/add-samples-android/tasks.md:64-66`

**問題点**:
タスク 7.2「Run（Shift+F10）でエミュレータ起動確認」/ 7.3「`SampleLabelCell` 3 行の描画目視」/ 7.4「Section H/F の描画目視」がチェック済みになっているが、orchestrator からの引き継ぎでも「implementer がビルド成功＋ライブラリテスト pass を根拠に完了扱いとしている」と明示されており、実機・エミュレータでの目視確認の証跡（スクリーンショット・実行ログ等）は残っていない。

これらは spec.md の "起動時の画面表示" / "Section ヘッダ・フッタの描画" Scenario に対応するため、ビルド成功＋静的コード読解では Scenario の "THEN" を完全には保証できない。design.md Non-Goals で UI テスト / スナップショットテスト自体は除外されているため、自動検証の追加は本提案のスコープ外で正しいが、最終的な目視確認はユーザー側で実施する必要がある。

**推奨修正**:
- ユーザー側で `cd samples/android && ./gradlew :app:installDebug` → エミュレータ起動 → 画面の目視確認を 1 回実施し、結果（OK / NG）を確認する
- 目視確認に基づいて以下のいずれかの方針で確定する：
  - 結果が OK ならば、tasks 7.2-7.4 は実質的に完了。本指摘は対応不要として記録のみ
  - 結果が NG ならば、修正の上で再レビュー
- 中長期的な再発防止としては、後続提案（CI 整備等）でスクリーンショット保管 or `screengrab` などの自動化を検討するのが望ましいが、本提案のスコープ外

### 🔵 Suggestion

#### [Suggestion] `KsCellRegistry.register` の重複検証ロジックに線形探索が残る

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:131-140`

**問題点**:
`register` の中で「同じ viewType が別の cellClass に既に登録されているか」を判定するため、`entriesByCellClass.entries.firstOrNull { it.value === existing }` と線形探索している。Cell 種類が増えてきた段階では問題ないが、`add-cell-types-*` で 10〜20 種類追加された後の登録集中タイミングで O(N²) 的になる可能性がある。

**推奨修正**:
本提案のスコープ外だが、後続で `viewType → KClass` の逆引きマップ（`MutableMap<Int, KClass<out Cell>>`）を別途持てば O(1) で衝突検証できる。本提案では現状のままで問題ない（Sample で使う登録は 1 件のみ）。

#### [Suggestion] Sample 用の `MainActivity.onCreate` で `KsCellRegistry.strictMode` を明示設定していない

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:37-43`

**問題点**:
`KsCellRegistry.strictMode` のデフォルトは `true`（デバッグ優先）。Sample では `SampleLabelCell` のみを登録しているため未登録 Cell の経路は通らないが、後続提案（`add-cell-types-*` で Sample にページ追加する想定）で別 Cell を submit したときに即座に `IllegalStateException` がスローされる。Sample の意図（「目視確認可能な最小アプリ」）からは strictMode = true で問題ないが、利用者ガイドとして明示的に `KsCellRegistry.strictMode = BuildConfig.DEBUG` のような書き方を入れておくと、後続でページを増やすときの参考になる。

**推奨修正**:
本提案のスコープ外として、後続提案（`add-cell-types-basic` の Sample ページ追加）の中で
- `KsCellRegistry.strictMode = false`（リリース APK 配布想定）または
- `KsCellRegistry.strictMode = BuildConfig.DEBUG`（デバッグビルドのみ厳格）
の明示設定を README 例として追加することを検討する。

## アクションプラン

優先度順：

1. **【Minor / ユーザー対応推奨】** ユーザー側で 1 回 `samples/android/` を Android Studio から API 29+ エミュレータで Run し、画面の目視確認（`SampleLabelCell` 3 行 + Section H/F "PoC Section" / "This is a footer" の表示）を実施する。OK ならば本提案は archive 可。
2. **【Suggestion / 後続】** 後続 `add-cell-types-*` 群で Sample にページを追加する際に `KsCellRegistry.strictMode` の明示設定を README に追記する運用を検討する。
3. **【Suggestion / 後続】** Cell 種類が 10 種類を超える段階で `KsCellRegistry.register` の重複検証を `viewType → KClass` 逆引きマップに最適化する（本提案スコープ外、別の改善提案として）。

## 各レビュー観点に対する評価

### 正確性・機能性

- spec.md の 5 Requirement（"Android Sample アプリの存在" / "Sample 専用 Cell の定義と登録" / "SampleLabelCell を含むデモ画面" / "README の整備" / "アプリのメタデータ"）の SHALL/MUST 要件はすべて実装で満たされている。
- 各 Scenario の GIVEN-WHEN-THEN について、目視確認系の 2 つ（"起動時の画面表示" / "Section ヘッダ・フッタの描画"）以外はコード上で確認できた。
- tasks.md 全 34 タスクは tasks.md 上ですべてチェック済み。実装に対する整合性も確認した。
- design.md の Decision 1〜9 はいずれも実装に正しく反映されている。Decision 1（composite build）は `pluginManagement.includeBuild` と top-level `includeBuild { dependencySubstitution { ... } }` の二段構成として実装されており、AGP の Maven publication 不在問題への配慮も `settings.gradle.kts:32-46` のコメントで明示されている。

### archived spec の修正範囲

- `openspec/specs/settings-view-core/spec.md` の `Cell` Requirement: `sealed` 制約除去 + 外部モジュールから実装可能の追記。これは Sample で `SampleLabelCell : Cell` を別 Gradle モジュールから定義するための最小限の変更で、過剰な仕様変更はない。
- `openspec/specs/settings-view-android-ui/spec.md` の `KsCellRegistry` / `CellViewHolder` Requirement: `public` 可視性の明文化と "外部モジュールからの利用" Scenario の追加。これも Sample 実装に必要な最小限。
- `CellListItem` の説明文（"Core 側の Cell（sealed interface）型との衝突を避けるため" → "Core 側の Cell（インターフェース）型との衝突を避けるため"）の wording も整合済み。
- `Cell` の `sealed` 解除に伴う既存ライブラリコードへの影響：`when (cell: Cell)` や `is Cell` での網羅性チェックを使っている箇所は無い（`Grep` 確認済み）。`KsCellRegistry` がレジストリ + `strictMode` で実行時カバーする方針は一貫している。`ListAdapterDiffTest.kt` 等の既存テストもすべて pass。

### iOS Sample との比較（マルチプラットフォーム整合性）

- iOS (`samples/ios/KsSettingsViewSample`) と Android (`samples/android/`) の双方で：
  - `SampleLabelCell` の独自定義 + Renderer / ViewHolder の独自定義 + 起動時のレジストリ登録の構造一致。
  - 1 セクション・3 行の最小デモ + Section ヘッダ "PoC Section" / フッタ "This is a footer" の構成一致。
  - PoC Cell を Library 側 `internal` に閉じ込め、Sample 側で同等 Cell を独自定義するという設計方針の一致。
- 微妙な差異：
  - iOS は `SampleLabelCell.id: UUID`（Swift `KsCell` プロトコル要件）、Android は `SampleLabelCell.id: String`（Kotlin `Cell` インターフェース要件）。これはプラットフォーム契約による正当な差異。
  - 行ラベル文言：iOS は `"Sample Row 1/2/3"`、Android は `"Sample Label 1/2/3"`。この程度の差は問題なし（spec / tasks ではいずれも "複数行" としか規定していない）。

### テスト

- ライブラリ側のユニットテスト全件 pass（`cd android && ./gradlew test --rerun-tasks`）。
  - 主要テスト: `KsCellRegistryTest`(8), `KsSettingsViewTest`(3), `SectionAccessoryRenderingTest`(7), `RootHeaderFooterAdapterTest`(7), `EffectiveStyleTest`(4), `KsSettingsViewComposeTest`(3), `SettingsRootBuilderTest`(5), `SectionTest`(7) ほか、合計 0 failures / 0 errors。
- Sample 側のテストモジュールは design.md Decision 8 に従い意図的に置かない（Non-Goals）。これは正当。

### セキュリティ / パフォーマンス / 可読性

- セキュリティ: Sample アプリでハードコードされた機密情報・ネットワーク I/O・暗号化対象データなし。問題なし。
- パフォーマンス: `remember { settingsRoot { ... } }` で SettingsRoot を 1 度だけ構築。`AndroidView.update` は `view.root = root` で data class equals により DiffUtil no-op で扱われる設計。問題なし。
- 可読性: KDoc が丁寧で、各ファイル冒頭に「仕様」「設計」リンクを明示。命名は spec / tasks / design と完全一致。問題なし。

### 一貫性

- monorepo-foundation 規約（minSdk 29 / Kotlin DSL / JDK 17 / Application ID プレフィックス `jp.kamusoft.kssettingsview.*`）に準拠。
- 本体 `android/` モジュールと整合する Kotlin ソースルート `src/main/kotlin` を採用。
- `samples/android/.gitignore` 相当の `local.properties` は root の `.gitignore`（`local.properties` パターン）でカバー済み。`gradlew` / `gradle-wrapper.jar` / `gradle-wrapper.properties` は untracked から git add される予定で、これは Sample が独立 Gradle プロジェクトであることから妥当。

### 多言語対応

- `samples/android/app/src/main/res/values/strings.xml` のみ（`app_name` 1 件）。Sample の責務を超える多言語化は本提案 Non-Goals。問題なし。

## 判定結果

**ステータス**: `APPROVED`

- ✅ Critical 指摘なし
- ✅ Major 指摘なし
- ✅ ライブラリビルド成功（166 tasks executed, 0 errors）
- ✅ ライブラリテスト全件 pass（0 failures / 0 errors）
- ✅ Sample アプリ APK assembleDebug 成功（92 tasks）
- ✅ spec.md / design.md / tasks.md と実装の整合性確認済み
- ✅ archived spec の修正範囲は Sample 実装に必要な最小限（過剰変更なし）
- ✅ `Cell` の sealed 解除に伴う既存コードへの影響なし（`when` 網羅性チェック箇所なし）
- ✅ iOS Sample との構造・挙動の整合性確認済み
- 🟡 Minor 1 件: 7.2-7.4 の目視確認はユーザー側で 1 回実施推奨（ブロッカーではない）

ユーザー側でエミュレータでの目視確認を 1 回実施した上で問題なければ、本変更提案は archive 可能です。
