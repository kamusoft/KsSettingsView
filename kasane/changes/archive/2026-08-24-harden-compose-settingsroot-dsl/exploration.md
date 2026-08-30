# Exploration: harden-compose-settingsroot-dsl

統合起票 (2026-08-21、公開前トリアージ)。`fix-compose-dsl-marker-top-level-annotations` と `add-settingsroot-dsl-visibility-args` を統合した。両者は `ks-settingsview-compose` の `settingsRoot { section(...) }` DSL (`SettingsRootScope.kt` / `DSLHandles.kt` / `InputCellDsl.kt` 等) という同じファイル群を触り、「この DSL の API 面とスコープ制御を初回リリース前に確定する」という 1 つのレビュー文脈で閉じる。

探索 (2026-08-22) で両論点の方針を確定した。

## 課題 / 動機

### (1) DSL marker の無効な付与 (旧 fix-compose-dsl-marker-top-level-annotations)

Kotlin 2.4.10 へ更新した結果、`@SettingsRootDsl` を **top-level 拡張関数**に付けている **29 箇所** (`DSLHandles.kt` 12 / `BasicCellDsl.kt` 8 / `InputCellDsl.kt` 7 / `CustomCellDsl.kt` 2) に対し、コンパイラが次の警告を出すようになった:

> Applying DSL marker annotation to a function has no effect. DSL marker annotations must only be applied to types. (KT-81567)

探索で裏取りした事実 (ksn-scout、2026-08-22):

- 関数への `@DslMarker` 系注釈の付与は**以前から一貫して no-op**。KT-81567 は Kotlin 2.3.20 で入った「無意味な付与に気づかせる use-site 警告」で、挙動変更ではない (公式 Type-safe builders: "Applying a DSL marker to other targets (such as functions or properties) has no effect on scope control."、KEEP scope-control-for-implicit-receivers)
- スコープ制御は**暗黙 receiver の型が marked か**だけで決まる。receiver 型 6 クラス (`SettingsRootScope` / `SectionScope` / `DSLSettingsRootScope` / `DSLSectionScope` / `SectionHandle` / `CellHandle`) には付与済みなので、拡張関数側の注釈を外しても制御は締まりも緩みもしない
- 関数に付けられてしまった原因は、`SettingsRootScope.kt` の `annotation class SettingsRootDsl` に `@Target` が無く、全デフォルトターゲット (FUNCTION 含む) を許しているため
- したがって簡易起票時の「スコープ制御をリリース後に効かせると破壊的変更になる」という懸念は**成立しない** (制御は今すでに効いている)

出典: `kasane/changes/archive/2026-08-21-upgrade-android-build-toolchain/review-001.md` (Suggestion) / 同 実装報告。`kasane/roadmaps/package-distribution/phases/phase-1-android-build-toolchain/agenda.md` の申し送り。

### (2) `section(...)` の引数が `Section` data class より少ない (旧 add-settingsroot-dsl-visibility-args)

`SettingsRootScope.section(...)` (2 オーバーロード) は `headerHeight` / `isHeaderVisible` / `isFooterVisible` を受け取れない。他の 3 経路はすべて揃っている:

| 経路 | header/footer | isVisible | headerHeight | isHeaderVisible / isFooterVisible |
|---|---|---|---|---|
| core `Section` data class | ✓ | ✓ | ✓ | ✓ |
| iOS `ksSection` (2 overload) | ✓ | ✓ | ✓ | ✓ |
| Compose `DSLScope.Section` | ✓ | ✓ | ✓ | ✓ |
| `settingsRoot { section(...) }` (2 overload) | ✓ | ✓ | ✗ | ✗ |

探索での発見: `SettingsRootScope.sections` は private で、Section を足す経路は `section(...)` **だけ**。add-accessory-visibility-toggle review-001 の「Kotlin は `Section(...)` を直接書けるので機能欠落ではない」は `settingsRoot { }` の**外**でのみ成り立ち、中では `headerHeight` / トグルを使いたい Section が 1 つでもあると builder 全体を諦めて `SettingsRoot(sections = listOf(...))` を手書きするしかない (閉じた漏斗)。

出典: `kasane/changes/archive/2026-08-19-add-accessory-visibility-toggle/review-001.md` (Suggestion) / 実装ワーカーの申し送り。

### 公開前に扱う理由

- `settingsRoot` DSL の API 面は、利用者向け Skills (package-distribution phase-12) が DSL を文書化する前に決める
- `android/` を広く触る phase-5 (Android パッケージング・dir 改名) との衝突を避けるため、**phase-5 の着手前**に完了させる

## 検討した選択肢 (却下案と理由を含む)

### (1) marker

| 案 | 内容 | 評価 |
|---|---|---|
| A | 注釈 29 箇所削除 + `@Target` 制限 + コンパイルエラーを固定するテスト | 却下: テストに kotlin-compile-testing 系の新規依存が要り、この系統は Kotlin 本体の版追随が遅れがちで、Kotlin 2.4.10 環境ではツールチェーン更新をテストが縛る逆転リスクがある。守りたい契約 (receiver 型の marker が外されない) に対して重い |
| **A'** | **注釈 29 箇所削除 + `annotation class SettingsRootDsl` に `@Target(CLASS, TYPE, TYPEALIAS)` を付与** | **採用**: 警告解消と「関数に再び付けられる事故」の防止 (コンパイルエラー化) を新規依存なしで達成する |
| B | 注釈削除のみ | 却下: `@Target` が無いままだと将来また関数に付く |

A' では受け手クラスから marker が外される退行は検出できない。代わりに、実装時に「注釈を外した状態で、入れ子ラムダから外側 receiver を呼ぶコードが依然コンパイルエラーになる」ことを 1 回手で確認する手順を tasks に入れる (スコープ制御が効いている実証)。

### (2) API 面

| 案 | 内容 | 評価 |
|---|---|---|
| (a) | `isHeaderVisible` / `isFooterVisible` だけ足す | 却下: `headerHeight` の穴が次の change として残る |
| **(b)** | **`headerHeight` + 2 トグルを足し、`Section` data class と同じ引数セットにする** | **採用**: iOS `ksSection` / `DSLScope.Section` と完全対称になり、Skills で「`Section` と同じ引数」の一言で説明できる。既定値付き追加なのでソース互換 |
| (c) | 意図的な最小 API として現状維持 | 却下: 「閉じた漏斗」が公開 API として固定される |

## 決定事項

- 公開前トリアージ (2026-08-21): **初回リリース前に対応** (phase-5 Android パッケージングの前)。旧 2 件を本 change に統合、旧ディレクトリは破棄
- (1) **A'** (2026-08-22): 拡張関数 29 箇所の `@SettingsRootDsl` を削除し、`annotation class SettingsRootDsl` に `@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS)` を付与する。コンパイルテストは追加しない
- (2) **(b)** (2026-08-22): `SettingsRootScope.section(...)` の 2 オーバーロードに `headerHeight: Double = -1.0` / `isHeaderVisible: Boolean = true` / `isFooterVisible: Boolean = true` を追加する。既定値は `Section` data class と同一、引数順は `Section` / iOS `ksSection` に合わせる (`header, footer, headerHeight, isVisible, isHeaderVisible, isFooterVisible, block`)。文字列ヘッダ版の糖衣も同じ 3 引数を通し、さらに文字列 `footer: String? = null` を追加して iOS 文字列版 `ksSection` と揃える (相方レビュー second-opinion-spec-001 #1 で発見、2026-08-22 にユーザー決定)

## ADR 候補

なし (2026-08-22 に閉じた)。簡易起票時の 2 候補はいずれも不成立:

- 「settingsRoot DSL はスコープ制御を保証する」: スコープ制御は既に receiver 型側で成立しており、本 change は新たに保証を足すものではない (A' は警告解消と再発防止)。可逆で局所的
- 「settingsRoot DSL の API 面を最小に保つ」: (c) を却下したため起票対象外。(b) は「builder の引数を `Section` と同一にする」という局所的で可逆な決定で、探索メモの決定事項で足りる

## 未決の論点

なし。

## UI 素材

なし

## 変更級の推奨: M (理由)

(2) が公開関数への既定値付き引数追加 = ksn-core の「公開 API の小変更」に該当するため M。作業量は S 相当 (注釈 29 行削除 + `@Target` 1 行 + 2 オーバーロード × 3 引数 + 配管テスト) だが、`section(...)` が `Section` と同じ引数・同じ既定値を持つという契約をデルタスペックで固定しておくと、phase-12 の Skills 生成と蒸留 (concepts `android-compose.md` の `settingsRoot` builder 記述への追随) の入力になる。UI なし (`ui/` 不要)。

## 関連ファイル

- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt` (`settingsRoot` / `section(...)` の定義、`@SettingsRootDsl` 定義 — `@Target` 付与先)
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLHandles.kt` / `BasicCellDsl.kt` / `InputCellDsl.kt` / `CustomCellDsl.kt` (注釈削除 29 箇所)
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScope.kt` (`DSLScope.Section` — 引数順・既定値の参照)
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Section.kt` (data class — 既定値の正)
- `ios/Sources/KsSettingsViewSwiftUI/SectionBuilder.swift` (`ksSection` — 対称性の参照)
- `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootBuilderTest.kt` (配管テストの追加先)
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/StoreDemoScreen.kt` (唯一の本体外利用、影響なし)
- `kasane/concepts/android/api/android-compose.md` (蒸留時の追随先: `settingsRoot` builder の記述)
- 出典: `kasane/changes/archive/2026-08-21-upgrade-android-build-toolchain/review-001.md`、`kasane/changes/archive/2026-08-19-add-accessory-visibility-toggle/review-001.md`
