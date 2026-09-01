# Exploration: adopt-android-explicit-api-mode

## 課題 / 動機

Android 本体 module (`android/kssettingsview`) は Maven Central へ配る公開ライブラリになった (add-android-maven-distribution) が、Kotlin の Explicit API mode (`explicitApi()`) が未設定で、公開/内部の境界がコンパイラで強制されない。

発見の文脈: add-android-maven-distribution の独立レビュー (review-001 Suggestion)。同レビューの Major — 公開 data class `Theme` の `sectionMargin: PaddingValues?` が露出する外部型の依存 (`androidx.compose.foundation:foundation-layout`) が `api` になっていなかった — は、発行 aar を javap で全走査して「どの宣言が public か」を手作業で判定して初めて検出できた。recyclerview の `api` 漏れも同型 (spec の列挙が公開面の走査を経ていなかった — lessons/inbox/principle-with-enumeration-not-swept-against-public-surface.md に捕捉済み)。公開 API 面が宣言ベースで機械可読になっていれば、依存スコープ設計 (design Decision 6 の原理「公開 ABI に露出する外部型の依存は `api`」) の適用判定と意図しない API 公開の検出をコンパイラに委ねられる。

## 検討した選択肢 (却下案と理由を含む)

### 可視性棚卸しの platform 範囲

- **Android の判断を本 change が持つ** — 採用。Explicit API mode の有効化と、同じコンパイラ診断から見つかる公開・内部境界の判断を一続きで扱う。単一ドメインの変更に保てる
- **iOS の `EffectiveStyle` と Android の候補を cross-platform change に統合する** — 採らない。言語・コンパイラ機構が異なる二つの能力を横断し、変更級と検証範囲が不必要に大きくなる
- **本 change は設定と `public` 明示だけに限定し、Android の可視性引き下げを別 change に残す** — 採らない。同じ診断結果を二度棚卸しすることになり、初回リリース前に公開面を絞れる機会を逃しやすい

### 有効化モード

- **初回から Strict にする** — 採用。公開宣言の visibility と必要な型の明示不足をコンパイルエラーにし、公開境界の強制を導入時点から成立させる
- **Warning で開始する** — 採らない。現行 CI は警告を失敗として扱わず、調査上も型明示不足がほぼ無いため、段階移行の便益より強制が成立しない期間を残す不利益が大きい

### 対象 module

- **Maven 公開本体 (`android/kssettingsview`) のみ** — 採用。一般利用者へ公開する API 境界を Strict で強制する目的に直接対応する
- **非公開 Bridge (`android/kssettingsview-bridge`) も含める** — 採らない。MAUI binding のため JVM 上の public が必要な宣言が多く、約 193 宣言への明示に対して公開面を縮小できる余地が小さい。Bridge の ABI を安定契約として管理する必要が生じた時点で、binding 面の整理と併せて再検討する

### 消費者検証との順序

- **本 change を独立のまま phase-7 より先に完了する** — 採用。Android の公開面を確定してから消費者プロジェクトを検証し、API 降格による検証のやり直しを防ぐ
- **phase-7 の change に合流する** — 採らない。Android 固有の公開面棚卸しと 3 platform の消費者検証が混在し、変更の責務と検証範囲が肥大する
- **phase-7 の後で実施する** — 採らない。公開面変更後の再検証が必要になり、初回リリース前に互換性を壊さず公開面を絞る余裕も減る

### 公開・内部境界の調査結果

- `SettingsRootScope` / `SectionScope` / `CustomCellEmptyContent` / `VisibilityAware` / `DSLIconModifiableCell` / `DSLStyleModifiableCell` / `DSLReidentifiableCell` / `SettingsRootDiff` は、公開関数の引数・戻り値または公開 Cell の supertype に現れるため public を維持する。`SettingsRootDsl` も公開 DSL のスコープ制御を担うため public 明示の対象とする
- `KsCellRegistry.strictMode` / `CELL_VIEW_TYPE_MIN` / `register` は Sample・利用者向け契約・独自 Cell 拡張から使うため public を維持する
- 実際の降格判断候補は、Registry の内部照会 (`viewTypeOf` / `isRegistered`)、標準 Cell 一括登録 (`registerBasicCells` / `registerInputCells` / `registerCustomCell`)、Preview/Test 用 factory (`SettingsRootStore.preview`) に絞られた

### Registry の公開面

- **内部照会だけを `internal` へ降格する** — 採用。`viewTypeOf` は内部 Adapter だけが使い、`isRegistered` は KDoc がテスト・診断用と明記するため、公開契約から外す。`registerBasicCells` / `registerInputCells` / `registerCustomCell` はコードの KDoc と concepts が利用者向け一括登録 API として保証しているため public を維持する
- **標準 Cell 一括登録 API も `internal` へ降格する** — 採らない。既存 concepts の公開契約改訂と iOS との API 非対称化を伴う一方、Android では公開維持のコストが小さい
- **すべて public のまま維持する** — 採らない。内部 Adapter の処理とテスト・診断用 API まで公開契約として固定され、Strict 導入時に意図しない公開面を絞る目的に反する

### Preview / Test 用 factory

- **`SettingsRootStore.preview` を `internal` へ降格する** — 採用。通常の public コンストラクタが同じ `SettingsRoot` / `Theme` を受け取っており、利用者は同じ状態を構築できる。現行参照は本体テスト 1 件だけで、Sample・Bridge・MAUI・skills・concepts には存在しない
- **public Preview 支援 API として維持する** — 採らない。通常コンストラクタと機能が重複し、公開維持するなら利用者向け契約への掲載と互換性保証が新たに必要になる

## 決定事項

- `ios-effectivestyle-visibility` は iOS 限定のまま維持し、Android の可視性引き下げ候補は本 change が判断する (2026-09-01 ユーザー合意)
- `android/kssettingsview` は Kotlin Explicit API mode を初回から Strict で有効にする。Warning での移行期間は設けない (2026-09-01 ユーザー合意)
- `kssettingsview-bridge` は対象外とする。Maven 非公開であり、MAUI binding が必要とする JVM public 面への大量の明示に対して境界縮小の便益が小さいため。将来 Bridge ABI を安定契約として管理する場合に再検討する (2026-09-01 ユーザー合意、android/ADR-0022 に包含)
- 本 change は独立した M 級 change として phase-7 (消費者検証) より先に完了させる。phase-7 へ合流させず、確定後の Android 公開面を消費者視点で検証する (2026-09-01 ユーザー合意)
- `KsCellRegistry.viewTypeOf` と `isRegistered` は `internal` へ降格する。`register` / `strictMode` / `CELL_VIEW_TYPE_MIN` と `registerBasicCells` / `registerInputCells` / `registerCustomCell` は public を維持する (2026-09-01 ユーザー合意)
- `SettingsRootStore.preview` は `internal` へ降格する。通常の public コンストラクタで同じ状態を構築でき、現行の利用も同一 module のテストだけであるため (2026-09-01 ユーザー合意)

## ADR 候補 (作成済み: android/ADR-0022 accepted / 未起票: なし)

## 未決の論点

製品・API スコープに関する未決事項はない。提案・実装時に確認する技術事項:

- 有効化に伴う棚卸し: 相方のヒューリスティック調査では本体の暗黙 public は約 390 宣言、戻り値型・プロパティ型の明示不足は実質 0 件。コンパイラ実測で診断件数を確定し、公開維持する宣言へ `public` と必要な型を明示する
- コンパイラ実測: `-Pkotlin.explicitApi=strict` を付けた `:kssettingsview:compileDebugKotlin` は、Android SDK location が未設定のため Kotlin コンパイル前に停止した。SDK を利用できる環境で診断件数を確定する必要がある
- 公開シグネチャ上 public が必須と判断した DSL scope・marker interface・戻り値型について、Strict 有効化後のコンパイルで可視性違反がないことを確認する

## UI 素材 (ui/references/ の一覧と注釈)

なし (UI に触れない)。

## 変更級の推奨: M

単一 Android 能力内だが、全公開宣言への `public` 修飾子の明示と、`viewTypeOf` / `isRegistered` / `SettingsRootStore.preview` の公開 API 降格を伴う。約 390 宣言・約 93 production source へ及ぶ見込みのため S の「公開 API 変更なし・局所的」には収まらない。アーキテクチャ・データスキーマ・外部連携には触れず、対象も単一能力のため L には該当しない (2026-09-01 ユーザー合意)。
