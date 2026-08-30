# Tasks: harden-compose-settingsroot-dsl

対象 module: `android/ks-settingsview-compose`。テスト実行は `kasane/concepts/cross/conventions/test-execution.md` に従う (本 change のテストは純粋な builder / リフレクションで、Robolectric の描画待機は不要)。コメントは `kasane/concepts/cross/conventions/comment-policy.md` に従い、change-id の裸参照や履歴記述を KDoc に残さない。

## 1. DSL marker の整理

- [x] 1.1 `SettingsRootScope.kt` の `annotation class SettingsRootDsl` に `@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS)` を付与する (→ Requirement: SettingsRootDsl marker は型にのみ付与できる)
- [x] 1.2 `DSLHandles.kt` (12) / `BasicCellDsl.kt` (8) / `InputCellDsl.kt` (7) / `CustomCellDsl.kt` (2) の top-level 拡張関数から `@SettingsRootDsl` を削除する。receiver 型 6 クラスの付与は残す (→ 同上)
- [x] 1.3 `android/` 直下で `./gradlew :ks-settingsview-compose:compileDebugKotlin --rerun-tasks` を実行し、コンパイルタスクが実行されたこと (UP-TO-DATE / FROM-CACHE でないこと) と、ビルドログに KT-81567 の警告が 0 件であることを確認する (→ Scenario: ビルドで DSL marker の無効付与警告が出ない)
- [x] 1.4 スコープ制御の手動実証 (2 経路): テストソースに一時的に (a) `settingsRoot { section(id = "a") { section(id = "b") {} } }` (b) `KsSettingsView { Section { Section {} } }` を書いてコンパイルし、どちらも外側 receiver のメンバー呼び出しが `DSL_SCOPE_VIOLATION` でエラーになることを確認してから削除する。結果 (エラーメッセージの要旨) を実装報告に残す。拡張関数 (`LabelCell` 等) は外側 receiver から暗黙に呼ばれ得る入れ子構造が DSL 上に存在しないため対象外 (→ Requirement: SettingsRootDsl marker は型にのみ付与できる の補足)

## 2. `section(...)` の引数追加

- [x] 2.1 `SettingsRootScope.section(id, header: SectionAccessory?, footer, ...)` に `headerHeight: Double = -1.0` / `isHeaderVisible: Boolean = true` / `isFooterVisible: Boolean = true` を追加し、引数順を `header, footer, headerHeight, isVisible, isHeaderVisible, isFooterVisible, block` にする。生成する `Section` へ転写する (→ Requirement: settingsRoot builder の section は Section と同じ属性を受け取る)
- [x] 2.2 文字列ヘッダ版 `section(id, header: String, ...)` に `footer: String? = null` と同じ 3 引数を追加し、引数順を `header, footer, headerHeight, isVisible, isHeaderVisible, isFooterVisible, block` にする。`footer` は `SectionAccessory.Text` に包んで accessory 版へ委譲する (→ 同上)
- [x] 2.3 両オーバーロードの KDoc に新引数の説明を追加する (既定値の意味は core `Section` の KDoc を参照させ、重複記述しない)
- [x] 2.4 触るファイル内の既存コメントの規約違反を comment-policy の 3 類型で書き換える: `SettingsRootScope.kt` の `settingsRoot` KDoc (change-id の裸参照) / `DSLHandles.kt` の `CellHandle.disabled` KDoc (「本提案」「後続提案」) / `DSLScope.kt` の `DSLSectionScope.cell` KDoc (デルタスペックへの裸参照 `仕様: settings-view-android-ui "..."`)。現在の契約だけで自己完結する説明に直す (新規 ADR は起票しない)

## 3. テスト

- [x] 3.1 `SettingsRootBuilderTest.kt` に追加: headerHeight 指定 (→ Scenario: headerHeight を指定して構築する)
- [x] 3.2 同: トグル指定 (accessory 版) で `isHeaderVisible` / `isFooterVisible` が転写され、`header` / `footer` 内容が保持される (→ Scenario: Header / Footer の表示トグルを指定して構築する)
- [x] 3.3 同: 文字列ヘッダ版で文字列 `footer` / `headerHeight` / `isVisible` / 2 トグルを指定 (→ Scenario: 文字列ヘッダ版でも同じ属性を指定できる)、および `footer` 省略時に `Section.footer == null` (→ Scenario: 文字列ヘッダ版で footer を省略すると footer は無い)
- [x] 3.4 同: 新引数省略時に `Section(id, header, cells)` と `assertEquals` で等価 (→ Scenario: 省略時は Section data class の既定値と等価)
- [x] 3.5 同: accessory 版・文字列ヘッダ版それぞれを名前なしの位置引数で規定の並びどおりに呼び、各フィールドが対応する位置の値になる (→ Scenario: 位置引数で規定の並びどおりに呼び出せる)
- [x] 3.6 新規テスト (例: `SettingsRootDslMarkerTest.kt`): `SettingsRootDsl` 注釈クラスの許容ターゲットをリフレクションで読み (例: `SettingsRootDsl::class.java.getAnnotation(Target::class.java)?.allowedTargets`)、`CLASS` / `TYPE` / `TYPEALIAS` の 3 つと一致する (→ Scenario: marker 注釈の許容ターゲットが型に限定されている)
- [x] 3.7 同: receiver 型 6 クラスそれぞれに `SettingsRootDsl` が付与されている (→ Scenario: receiver 型の marker 付与が維持されている)
- [x] 3.8 反復中は `./gradlew :ks-settingsview-compose:testDebugUnitTest` で回してよいが、**完了判定**は `android/` 直下で `./gradlew test --rerun-tasks` (debug / release 両 variant、絞り込みなし) を実行し、各 module の `build/test-results/testDebugUnitTest/` および `testReleaseUnitTest/` の `TEST-*.xml` から `tests` / `failures` の合計を集計して `N tests / M failures` を報告する

## 4. 影響範囲の確認

- [x] 4.1 `samples/android` (`StoreDemoScreen.kt`) と `android/ks-settingsview-ui` の KDoc 例が引数追加後もコンパイルされる (`samples/android` 直下で `./gradlew :app:compileDebugKotlin`)
- [x] 4.2 `python3 scripts/comment-policy-lint.py` で禁止 0 件 (変更前も 0 件。2.4 の書き換えで退行しないことの確認)
