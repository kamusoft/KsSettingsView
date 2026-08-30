# Batch C 統合結果

## 統合方針

`settings-view-android-host`、`settings-view-android-compose`、`settings-view-android-style`、`settings-view-android-theme-bridge` の抽出候補を、Android 利用者が単独で読める2つの platform 概念へ統合した。iOS と Android の両材料は揃ったが、platform 共通原則の確定は Batch C のオーナーレビュー後に始める Batch D まで行わない。

| 統合ドラフト | 元候補 | 判断 |
|---|---|---|
| `platforms/android-native-host.md` | Native Host、Store、visible projection、Renderer Registry、Android list appearance / visual state、Material3 前提 | Android View から組み込む利用者が必要な公開 API、更新境界、拡張方法、ホスト条件を一つの入口へまとめる |
| `platforms/android-compose.md` | Compose Bridge、DSL、identity、Cell / Section modifier、Theme 伝播 | Compose 利用者が方式選択から state・identity・style 適用まで一続きで理解できるようまとめる |

Theme / CellStyle の全フィールド一覧、stable ID の hash、ConstraintLayout の個別制約は移行しない。利用者が選択と挙動を予測するために必要な解決順、背景領域、最低行高、Classic / Modern、無効状態、Material3 前提だけを Android Native Host に残した。

## Batch D へ繰り越す横断統合材料

次は Android 固有 API ではなく、Batch B の iOS 材料と統合して初めて共通原則を確定する。Batch C の確定後に Batch D で旧 cross-platform 概念の後継へ統合する。

| 統合先候補 | Batch C の材料 |
|---|---|
| `architecture/native-host-boundary.md` | Native Host の責務、空状態、ライフサイクル、完全 model と projection の分離 |
| `architecture/store-and-update-streams.md` | `StateFlow` の現在状態と一過性通知、内容更新バッチ、Theme 通知の分離 |
| `architecture/display-state-synchronization.md` | 構造・同一 ID の内容・可視性・Theme の反映経路 |
| `architecture/cell-renderer-registry.md` | Cell 型と ViewHolder factory の登録・解決、予約 viewType、未登録時挙動、再利用境界 |
| `architecture/declarative-ui-bridge.md` | DSL / Store 二方式と同じ Native 更新経路への収束、Compose state bridge |
| `architecture/declarative-tree-identity.md` | dynamic key、明示 ID、static fallback、内容と identity の分離 |
| `styling/style-resolution.md` | UI 層の Theme / CellStyle 所有、Cell 固有値から platform default までの解決 |
| `styling/cell-row-layout.md` | 共通行、trailing control、Android の最低60dpと可変高 |
| `styling/cell-visual-states.md` | ripple / disabled と Native control の状態表現 |
| `styling/list-appearance.md` | Classic / Modern、canvas、Header / Footer、separator / Section 背景 |

Batch A・B から繰り越された「表示状態同期」「Native Host」「宣言 Bridge」「identity」「style」の材料を、これらへ合流させる。

## ADR 候補のトリアージ

| 候補 | 出典 | 基準該当 | 推奨 |
|---|---|---|---|
| Native View を Compose から再利用し、Registry で Cell 描画を拡張する | Android Host / Compose のコード・テスト、旧 spec | 能力境界、将来制約 | ADR-0004 に包含。新規なし |
| Store と構造 Diff を更新境界にする | `SettingsRootStore`、Host / Compose のテスト、旧 spec | 能力境界、将来制約 | ADR-0006 に包含。新規なし |
| DSL と Store を併存させ同じ更新経路へ収束する | `KsSettingsViewComposable.kt`、Compose テスト、旧 spec | 能力境界、将来制約 | ADR-0007 に包含。新規なし |
| 宣言ツリーの内容と identity を分離する | identity / diff calculator のコード・テスト、旧 spec | 能力境界、将来制約 | ADR-0008 に包含。実装との優先順位 drift は別記 |
| Theme / CellStyle を UI 層で Compose Native 型として所有する | Theme / CellStyle / EffectiveStyle のコード・テスト、旧 spec | 能力境界、将来制約 | ADR-0009 に包含。新規なし |
| 表示同期を構造・内容・可視性へ分ける | Host / Compose diff のコード・テスト、旧 spec | 能力境界、将来制約 | ADR-0010 に包含。新規なし |
| Cell 共通行を View composition で統一する | CellBaseLayout と ViewHolder 群、旧 spec | 複数 Cell を横断、将来制約 | ADR-0011 に包含。新規なし |
| 独自 Cell の style / icon / ID 対応を任意 capability にする | modifier protocol と独自 Cell 境界 | コンポーネント境界、将来制約 | ADR-0013 の薄い Cell 抽象に包含。新規なし |
| Android の既定を可変高 + 最低60dpにする | EffectiveStyle、行レイアウトテスト、旧 spec | 複数 Cell を横断 | 可逆な platform 視覚値。概念へ残し ADR にはしない |
| Android View Host に Material3 派生 XML Theme を要求する | Material widget 依存、Sample manifest、実行テスト、docs | ホストアプリ境界、将来の control 選択を制約 | **新規 ADR 候補**。ただし採用理由・代替案を備えた design 出典がないため、この移行で本文を創作せず platform 利用契約として保持する。ADR 化する場合は後続の提案で判断材料を補う |

## drift 所見

解消方向は決めず、オーナーレビュー対象として保持する。同じ原因の指摘は capability 間で統合した。

1. Android Host spec の Theme 更新例は削除済み `SettingsRootDiff.UpdateTheme` と旧名 `Theme.viewBackgroundColor` を使う。現行は `SettingsRootStore.applyTheme` → `theme: StateFlow<Theme>` と `Theme.backgroundColor` である。
2. Host spec の独自 Cell 登録例は `viewType = 1` を使うが、現行 Host は1〜4を Section H/F に予約し、利用者 Cell には `CELL_VIEW_TYPE_MIN = 100` 以上を要求する。この例は Registry より前に Accessory ViewHolder へ分岐される。
3. 現行 public `SettingsRootStore.replaceCells` は RadioCell などの連動内容更新に使われるが、旧 spec と `docs/platform-guide-android.md` の Store メソッド一覧にない。
4. docs の Store 例は `insertCell(..., index = 0)` を使うが、現行 Kotlin API の引数名は `at` でありコンパイルできない。
5. 旧概念は適用不能な Store 操作で状態も通知も変えないと一般化するが、`updateAccessory` は存在しない Section ID でも `UpdateAccessory` を発行する。
6. `KsSettingsView.onDetachedFromWindow` は RecyclerView の Adapter を `null` にする一方、再 attach 時に `concatAdapter` を戻さない。同じ View instance の再 attach 契約は実装・テストとも未確立である。
7. Compose spec の DSL signature は `SettingsRootScope` とするが、現行 Recomposition DSL は `DSLSettingsRootScope`。明示 ID 付き純粋 builder の `SettingsRootScope` とは別型である。
8. Compose spec は Recomposition DSL に小文字 `section(id:, ...)` があるとするが、現行 DSL は大文字 `Section(...)` と Handle modifier を使う。小文字 `section` は純粋 `settingsRoot` builder にだけ存在する。
9. ADR-0008・旧 spec・docs は `forEach` key を明示 ID より優先するが、現行 Android 実装は既存 Explicit hint を `forEach` が上書きしないため明示 ID が勝つ。
10. 旧 spec は `cellID("x")` / `sectionID("x")` の値を最終 ID そのものとするが、現行は namespaced hash の hint として使い、最終 ID は32桁 hexになる。
11. Compose spec の前半は Cell 内容変更で `ReplaceCell` を発行するとするが、現行は構造 Diff を出さず `contentUpdates` → `replaceCells` へ流す。同 spec 後半には現行と一致する記述もあり内部矛盾する。
12. Compose spec は `MutableState` を Cell data class 内へ保持するとするが、現行 helper は評価時の値 snapshot と書き戻し callback だけを Cell に渡す。
13. Compose spec は `DSLReidentifiableCell` と style modifier protocol を Core 所属とするが、現行は ID protocol のみ Core、style / icon protocol は UI 所属である。
14. Compose / theme bridge spec の modifier 例は削除済み `KsFont` / `KsIcon` / `KsColor` や `style.font` / `style.icon` を使う。現行は `TextStyle`、`KsImage`、`CellStyle.titleFont` と Cell 本体の `icon` を使う。
15. docs は `.disabled(true)` を機能する modifier として案内するが、`CellHandle.disabled` と `Cell.disabled` は常に no-op である。
16. `DSLScope.kt`、`CellModifiers.kt`、`DSLIconModifiable.kt` のコメントは Switch / Checkbox 等を icon 非対応の例にするが、現行の組み込み12種はすべて `DSLIconModifiableCell` に準拠する。
17. style spec の Compose 例は削除済み `KsSettingsView(root = state, style = ...)` を使う。現行入口は Store 方式と DSL 方式であり `root` 引数はない。
18. style spec は `TextStyle.fontFamily` が Android View 描画へ到達する e2e を要求するが、現行 `toTypeface()` は `fontFamily`、italic などを無視し、既定 Typeface と numeric weight だけを使う。
19. `Theme.cellIconSize` / `cellIconRadius` と `CellStyle.iconSize` / `iconRadius` は解決・テストされるが、共通行は24dp固定 icon View を変更せず radius も描画へ適用しない。
20. `Theme.scrollIndicatorVisible` は公開値として保持・テストされるが、RecyclerView の scrollbar へ適用するコードがない。
21. style spec の共通行記述は `LinearLayout` と44dp最低高を含むが、現行は `MinHeightConstraintLayout` と最低60dpである。同 spec の別 Requirement、コード、テストも60dpを正とする。
22. 旧概念は操作可能な Cell だけが押下 feedback を示すとするが、現行テストは handler のない LabelCell / CheckboxCell も enabled なら ripple 用に clickable とする。
23. `ClassicSectionDecoration` の冒頭コメントは1dp相当とするが、実装、詳細コメント、spec、テストは density 換算しない1物理 pixelを正とする。
24. theme bridge spec は Theme / CellStyle / KsImage を Core の論理値として扱う一方、現行は UI 層が Compose Native 型を直接所有する。同 spec 後半の UI 層 Requirement とも矛盾する。
25. theme bridge spec は旧 Theme 名 `viewBackgroundColor` / `titleColor` / `titleFont` / `descriptionColor` を使い、現行の `backgroundColor` / `cellTitleColor` / `cellTitleFont` / `cellDescriptionColor` と一致しない。
26. theme bridge spec は Switch の checked thumb / track を両方 accent 色にするとするが、現行は track に accent、thumb に Material `colorOnPrimary` を使う。
27. `CellModifiers.font` のコメントは title / hintText font を変えるとするが、実装は `CellStyle.titleFont` だけを変更する。
28. theme bridge spec は `AndroidView` の Context が `Theme.Material3.*` 派生である必要を説明しない。Compose `MaterialTheme` だけでは `?attr/materialSwitchStyle` を満たせず実行時失敗になり得る。
29. `backgroundColor` / `icon` / 常時 no-op の `disabled` modifier を公開 API 単位で直接固定するテストは見当たらず、Theme bridge のこの部分は検証鮮度が低い。

## 見送った情報

- RecyclerView の3 Adapter、`CellListItem` の内部 sealed 階層、stable ID hash、DSL hash の演算はコードから再導出しやすいため記載しない。
- Theme / CellStyle の全フィールド表は API 定義から再導出しやすく、利用者の選択を助ける解決規則だけを残した。
- ConstraintLayout の全 constraint、dp / px 変換、個々の Material color fallback は内部描画詳細として移行しない。
- 旧 docs / spec の廃止済み API 例は現行利用例へ置き換え、歴史的 signature は移行しない。
- Android 固有の `StateFlow` / `SharedFlow` 実装詳細は Native Host の復元契約に必要な範囲だけ残し、横断 Store 概念の確定は Batch D へ送る。

## 初見可読性レビュー

`batch-c-readability-review.md` の必須2件と推奨5件を反映した。

- Store 方式では `store.theme` を唯一の正とし、`view.theme` との優先関係、直接駆動時の入口、併用禁止を明確にした。
- `Cell` / `CellHandle` modifier の receiver、戻り値、非対応時の no-op、chain 継続、`DSLReidentifiableCell` 非準拠時の ID を明確にした。
- Root Header の例、利用者向けの用語、二つの DSL scope の import、明示 ID の安全な例、平易な表現を追加した。

再レビューは **PASS** で、残存する必須・推奨の可読性問題はない。最終配置 `concepts/platforms/` 基準の文書間・関連概念リンクも確認済みである。

## オーナーレビュー

2026-07-19 に Batch C の確定承認を得た。統合ドラフト2件を `concepts/platforms/` へ配置し、index・log・tasks を更新した。

Material3 派生 XML Theme の要求は platform 利用契約として確定した。新規 ADR 候補には該当するが、採用理由・代替案を備えた design 出典がないため、本移行では ADR 本文を創作せず、後続の提案で判断材料を補う扱いを維持する。
