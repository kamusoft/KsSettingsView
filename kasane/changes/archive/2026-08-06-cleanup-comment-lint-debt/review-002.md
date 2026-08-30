# レビュー結果: cleanup-comment-lint-debt (002 回目)

**日付**: 2026-08-06
**判定**: CHANGES_REQUESTED

## サマリー

**前回の指摘 7 件 (Major 3 / Minor 4) はすべて解消**を確認した。書き換え後の記述はいずれもレビュアー自身が実コードで裏取りし、特に事実関係が入り組む 2 件 — 依存の向き (`DSLNodes.swift`) と準拠 Cell 12 種 (`DSLIconModifiable.kt`) — は `Package.swift` の target dependencies および 13 個の Cell 型の準拠宣言まで遡って正しいことを確認した。**新たな誤りの持ち込みもない**。機能コード差分 0 (222 ファイル全件一致)、lint 禁止 0、Android 1986 / iOS 624 テスト全成功も再現した。

一方で、**前回サンプリングでカバーしなかった領域から、書き換えで混入した事実誤認が新たに 12 件見つかった** (Major 2 / Minor 10)。うち最も重いのは `DatePickerUIStyle.kt` で、公開 enum の doc comment が「Spinner は `AlertDialog` を表示する」と現在形で断言しているが、実装はボトムシート + 3 連ホイールであり、`AlertDialog` はコード上に 1 箇所も存在しない (この置き換えは `android/ADR-0009` の決定そのもの)。

さらに構造的な問題として、**今回修正した 3 つの欠陥はいずれも対になるプラットフォーム側が未修正のまま残っている**。修正前は「両方とも間違っている」状態だったが、片側だけ直したことで**対になる公開 API のドキュメントが互いに矛盾する**状態になった。単発の見落としではなくパターンなので、対称性を確認する工程を修正サイクルに入れてほしい。

## 前回指摘 7 件の解消状況

| # | 前回指摘 | 状態 | レビュアーによる裏取り |
|---|---|---|---|
| 1 | `DSLNodes.swift:19-21` 依存の向き | **解消** | `ios/Package.swift:68-72` で `KsSettingsViewSwiftUI` が `["KsSettingsViewUI", "KsSettingsViewCore"]` に依存。新文「SwiftUI 層に置くと `KsSettingsViewUI → KsSettingsViewSwiftUI` の依存が必要になり、既存の `KsSettingsViewSwiftUI → KsSettingsViewUI` と循環する」は成立。`DSLReidentifiable` が `KsSettingsViewCore/DSLCellIdentity.swift:19` にあることも確認 |
| 2 | `ButtonCellViewHolder.kt:19` aux 列 | **解消** | 該当行が削除され、残る L13-18 の `icon` / `valueText` / `hintText` 3 点記述が `ButtonCellViewHolder.kt:75` の `hasAux` と一致 |
| 3 | `RootHeaderFooterAdapterTest.kt:15` 存在しない API | **解消** | 主語が `RootHeaderFooterAdapter.view` に変更。`RootHeaderFooterAdapter.kt:39` に `var view: RootAccessory?` が実在し、テスト本体もこれを直接代入している |
| 4/5 | `SwitchCell.kt:16` / `CheckboxCell.kt:14` の `description` | **解消** | 両者とも `description` / `valueText` / `icon` / `hintText` の 4 点列挙になり、L23 / L21 の `val description: String? = null` と一致。兄弟の `RadioCell.kt` とも書き方が揃った |
| 6 | `KsSettingsViewComposeTest.kt:35` 検証スコープ | **解消** | 「Store 方式・DSL 方式の両 overload」に変更。同ファイルに Store 系 4 本 + DSL 系 3 本が実在することを確認 |
| 7 | `Theme.swift:9` 部分列挙 | **解消** | 導入句が「主なフィールドは以下：」になり、部分列挙であることが明示された |
| 8 | `ThemeRenameTests.swift:7` 過剰断定 | **解消** | 「`Theme` は `viewBackgroundColor` / `titleColor` / `titleFont` を持たない。存在しない名前を参照するコードがあれば本ターゲットのコンパイル自体が失敗する」に緩和され、Android 側 `ThemeRenameTest.kt` の書き方に揃った。同ファイル L62 の `cellBackgroundColor` 使用とも矛盾しない |

**Suggestion 対応 2 件も適切**。`DSLIconModifiable.kt` は同一 KDoc ブロックの既存陳腐化まで含めて修正され、記述 (「`icon: KsImage?` を持つ Cell が準拠する。アイコン領域を持たない `CustomCell` は準拠せず、その場合 `CellHandle.icon(_:)` modifier は no-op」) が実コードと完全に一致することを確認した (UI 層 Cell 13 種のうち `icon` を持つ 12 種が準拠、`CustomCell` のみ非準拠、`CellModifiers.kt:58` / `DSLScope.kt:250` の分岐も一致)。`ic_notifications.xml` も兄弟 2 ファイルと同形式になり、参照先の `KsImage.systemName("bell")` が `samples/ios/.../UnifyCellCommonFieldsDemoView.swift:33` に実在することを確認した。

**新たな誤りの持ち込みは 0 件**。上記 9 箇所の書き換えは、いずれも新しい事実誤認を生んでいない。

## 指摘事項

### [🟠 Major] Spinner の選択 UI を `AlertDialog` と断言しているが、実装はボトムシート + 3 連ホイール

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerUIStyle.kt:10-11`

**問題点**:
```kotlin
 * - [Spinner]: `android.widget.DatePicker`（`android:datePickerMode="spinner"`）を内包する
 *   `AlertDialog` を表示（AiForms の `IsAndroidSpinnerStyle = true` 相当）
```
L11 は `+` 行 (「旧 AiForms 〜相当」→「AiForms の〜相当」に書き換えられた)。しかし実装は:

- `DatePickerCellViewHolder.kt:59` — `DatePickerUIStyle.Spinner -> showDateSelectionSheet(cell, theme, effective)`
- `DatePickerCellViewHolder.kt:110-116` — `DateSelectionSheet(...)` を構築 (ボトムシート + 年/月/日の 3 連ホイール)
- 同ファイル L17 の KDoc 自身が「ボトムシート + 年/月/日の3連ホイール（`DateSelectionSheet`、android/ADR-0009）を表示」と正しく書いている

`AlertDialog` は `android/ks-settingsview-ui/src/main/kotlin/` 配下で **この行以外に 1 箇所も出現しない**。さらに `kasane/decisions/android/0009-datepicker-spinner-bottom-sheet-triple-wheel.md` の Context は、`AlertDialog` + `android.widget.DatePicker` 方式が **spinner 表示に切り替わらない不具合として顕在化した旧実装**であり、それを置き換えたのがこの ADR であると明記している。つまりこの `+` 行は、ADR が明示的に廃した方式を現在形で述べている。

これは公開 enum のケース説明であり、利用者が `DatePickerUIStyle.Spinner` を選んだときに何が出るかを判断する唯一の記述である。害が大きい。

**推奨修正**: 「`DateSelectionSheet`（ボトムシート + 年/月/日の 3 連ホイール）を表示（android/ADR-0009）」の趣旨に直す。あわせて L22 の enum メンバ doc (`android.widget.DatePicker` の Spinner モード) も同じ誤りを持つ (未変更行だが、同一ブロックの整合として `DSLIconModifiable.kt` と同じ扱いが自然)。

---

### [🟠 Major] iOS `DSLIconModifiable` の準拠 Cell が 2 種と書かれているが実際は 12 種 — Android 側だけ修正された対の片割れ

**該当箇所**: `ios/Sources/KsSettingsViewUI/DSLIconModifiable.swift:6-7`

**問題点**:
```swift
// SwiftUI DSL の `.icon(_ icon: KsImage)` modifier 経路を満たすために、UI 層で `icon` を持つ
// Cell（`LabelCell` / `CommandCell`）が準拠するプロトコルとして定義する。
```
実際に `DSLIconModifiable` に準拠しているのは **12 種** — `ButtonCell` / `CheckboxCell` / `CommandCell` / `DatePickerCell` / `EntryCell` / `LabelCell` / `NumberPickerCell` / `PickerCell` / `RadioCell` / `SimpleCheckCell` / `SwitchCell` / `TimePickerCell`。UI 層の Cell 13 種のうち非準拠は `CustomCell` (`CustomCell.swift:57`) のみ。

同一 doc comment の下部 (未変更行 L27-28) はさらに強く誤っている:
```swift
/// 他の基本 Cell（`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` /
/// `ButtonCell`）は `icon` フィールドを持たないため本プロトコルに準拠しない。
```
挙げられた 5 種は**すべて `icon` を持ち、すべて準拠している**。読んだ人は `.icon(_:)` が `SwitchCell` で no-op だと結論するが、実際には機能する。

**これは前回 Suggestion で指摘した Android `DSLIconModifiable.kt` と同一内容**であり、Android 側はオーナー判断で既存陳腐化を含めて修正済み。iOS 側の対のファイルが手つかずのため、**同じ設計概念を説明する 2 つのファイルが互いに矛盾する**状態になった。前回この指摘を Suggestion に留めた理由は「合意済みスコープ (ファイル全体の総点検はしない) の外」だったが、オーナーがこのクラスを修正対象に引き上げた以上、スコープはもう制約になっていない。

**推奨修正**: Android 側 `DSLIconModifiable.kt` の書き方に揃える。

---

### [🟡 Minor] `VisibilityAware` の「基本 Cell 7 種が opt-in 準拠」が網羅リストとして読める (iOS / Android 両方)

**該当箇所**: `ios/Sources/KsSettingsViewUI/VisibilityAware.swift:15-16`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/VisibilityAware.kt:11-12`

**問題点**: 両方とも `+` 行で、書き換え前は `本変更で扱う 7 種の Cell` / `- 本変更で扱う 7 種の Cell` というデルタ枠だったため部分列挙が自然だった。これを「基本 Cell 7 種」「UI 層の基本 Cell 7 種」に変換した結果、網羅リストに見える。

実際の準拠型は **両プラットフォームとも 13 種** (基本 7 種 + `EntryCell` / `PickerCell` / `DatePickerCell` / `TimePickerCell` / `NumberPickerCell` + `CustomCell`)。とくに `CustomCell` は直後の未変更行「非準拠の Cell（外部 Sample Cell や `CustomCell` 等）は…常に visible として扱われる」と真っ向から矛盾する (`ios/.../CustomCell.swift:57`、`android/.../CustomCell.kt:58` はいずれも `VisibilityAware` 準拠)。

前回の `Theme.swift` 部分列挙 Minor とまったく同じ変換パターン。

**推奨修正**: 「`CustomCell` を含む UI 層の全 Cell が準拠する」等、実態に合わせる。直後の `CustomCell` 非準拠の記述も同一ブロックなので併せて直すのが自然。

---

### [🟡 Minor] iOS `SwitchCell` / `CheckboxCell` の共通フィールド列挙から `description` が脱落 — 前回 Android 側だけ直った同一欠陥

**該当箇所**: `ios/Sources/KsSettingsViewUI/SwitchCell.swift:6-8`、同 `CheckboxCell.swift:6-8`

**問題点**: 両方とも `+` 行で「`valueText` / `icon` / `hintText` は全 Cell 共通の行レイアウトフィールドとして持つ」と書かれているが、両 Cell とも `public let description: String?` を持つ。**前回 Minor で指摘され Android 側 (`SwitchCell.kt` / `CheckboxCell.kt`) だけが修正された欠陥の、iOS 側の対**。

前回の指摘理由もそのまま当てはまる: 同じ iOS の兄弟である `RadioCell.swift:7` / `SimpleCheckCell.swift:7` は「`description` / `valueText` / `icon` / `hintText` / `accentColor`」と正しく列挙しており、同種記述の書き換え方針が iOS 内でも揃っていない。

**推奨修正**: `description` を列挙に加える (`RadioCell.swift` / `SimpleCheckCell.swift` の書き方に揃える)。

---

### [🟡 Minor] iOS `EntryCell` / `PickerCell` の共通規約列挙から `description` が脱落

**該当箇所**: `ios/Sources/KsSettingsViewUI/EntryCell.swift:9`、同 `PickerCell.swift:9`

**問題点**: 両方とも `+` 行で「基本 Cell 共通の規約（`isEnabled` / `isVisible` / `icon` / `hintText`）へは opt-in で準拠し」と書かれているが、`EntryCell.swift:32` / `PickerCell.swift:29` に `public let description: String?` が存在する。`EntryCell` は直後に「`valueText` は例外として持たない」と例外を明記しているぶん、列挙が網羅的に読める度合いが強い。

**推奨修正**: `description` を列挙に加える。

---

### [🟡 Minor] `ApplyDiffTest` が検証していない `submitList` 平坦リストを「保証する」と書いている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ApplyDiffTest.kt:24-25`

**問題点**:
```kotlin
 * [SettingsRootDiff] の各差分種別を適用したとき、内部保持 root と `submitList` へ渡る
 * 平坦リストが期待どおり更新されることを保証する。
```
同ファイルの assert は全 20 箇所が `view.internalRoot()` / `view.internalTheme()` / `view.rootHeader` / `view.rootFooter` のみで、`submitList` にも adapter の `currentList` にも一切触れていない。平坦リストの検証は行われていない。

直上の未変更行 L22 (`内部 root・submitList 状態検証テスト`) が元から同じ誤りを含んでおり、書き換えがそれを断定形に強めた形。

**推奨修正**: 「内部保持 root と Theme が期待どおり更新されることを保証する」に直す。

---

### [🟡 Minor] `KsSettingsViewRepresentableTests` の検証スコープから style 検証 3 本が漏れている

**該当箇所**: `ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewRepresentableTests.swift:8`

**問題点**: 「検証対象は `SettingsRootStore` 経由の更新と Root H/F modifier。」と限定しているが、同ファイル 7 テスト中 3 本は style を検証している — `test_makeControllerでstoreのrootとstyleが反映される` (L21)、`test_applyUpdateでstyleが切り替わる` (L53)、`test_modernで初期化したcontrollerは即時にmodernになる` (L70)。

削除行は「Binding ベースのテストを Store 経由・Root H/F modifier の検証に置き換えた」という**経緯**の記述だったため部分的な言及で自然だったが、現在形のスコープ断定に変換したことで漏れが顕在化した。**前回 Minor で指摘され修正済みの `KsSettingsViewComposeTest.kt:35` と同一パターンの、iOS 側の対**。

**推奨修正**: style 検証を含めたスコープ記述に直す。

---

### [🟡 Minor] `KsCellRegistry.createViewHolder` の呼び出し元に `RootHeaderFooterAdapter` を挙げているが呼んでいない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:26-27`

**問題点**:
```kotlin
 * `createViewHolder` は `RootHeaderFooterAdapter` /
 * `KsSettingsListAdapter` 等の同モジュール内コンポーネントから呼び出される
```
`KsCellRegistry.createViewHolder` の呼び出しはリポジトリ全体で `KsSettingsListAdapter.kt:152` の 1 箇所のみ。`RootHeaderFooterAdapter.kt:81-87` の `onCreateViewHolder` は `RootTextAccessoryViewHolder.create(parent)` / `RootAnyViewAccessoryViewHolder.create(parent)` で自前生成しており、registry を経由しない (`KsCellRegistry` からは `VIEW_TYPE_ROOT_*` 定数を参照しているだけ)。

**推奨修正**: `KsSettingsListAdapter` のみを挙げる。

---

### [🟡 Minor] `KsSimpleCheckView` の共有先を 2 箇所と書いているが 3 箇所ある

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSimpleCheckView.kt:23-24`

**問題点**: 「`RadioCellViewHolder` と `SimpleCheckCellViewHolder` の**両方**で共有する単一のカスタム View として実装する」とあるが、`PickerSelectionSheet.kt:395` でも選択印として生成している (`RadioCellViewHolder.kt:74` / `SimpleCheckCellViewHolder.kt:72` と合わせて 3 箇所)。「両方」という数え上げが誤り。

**推奨修正**: 「行内の選択印を描く各所 (`RadioCellViewHolder` / `SimpleCheckCellViewHolder` / `PickerSelectionSheet`) で共有する」等に直す。

---

### [🟡 Minor] `ic_navigate_next.xml` が「原典と同型」と書いた直後に原典との差分を自認している

**該当箇所**: `android/ks-settingsview-ui/src/main/res/drawable/ic_navigate_next.xml:4-5`

**問題点**:
```xml
  AiForms.Maui.SettingsView の
  Platforms/Android/Resources/drawable/ic_navigate_next.xml と同型。
```
直後の L8 が「原典からの意図的な差: 原典のパス（縦範囲 y=6..18）は viewport 中心 (13) に対し…絶対 y 座標を +1 してパスを viewport 縦中央へ補正している」と述べ、実 `pathData` も `M10,7L8.59,8.41 13.17,13l-4.58,4.59L10,19l6,-6z` (y=7..19) で原典と異なる。書き換え前の「移植したもの」なら差分の存在と両立したが、「同型」は同一ブロック内で自己矛盾する。

**推奨修正**: 「〜に対応するアイコン。」等、差分の存在と両立する表現に直す。

---

### [🟡 Minor] 実在しない型名 `SampleLabelCell` を現在形の設計理由の中で例示している

**該当箇所**: `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt:8-9`

**問題点**: 「Kotlin の sealed 制約では別 Gradle モジュールから実装できず、Sample アプリ独自の Cell（`SampleLabelCell` 等）を定義できないためである。」— `SampleLabelCell` はリポジトリの実コードに存在しない (`openspec/` のアーカイブ文書にのみ残る)。現在の Sample が使う独自セルは `samples/android/.../SampleSliderCell.kt` で、UI 層の公開 `CustomCell` を返すラップ関数として実装されている。

comment-policy がリポジトリ内識別子の参照を無条件に許すのは「grep で到達でき、消えれば同一コミット内で壊れに気づける」ためだが、この名前は grep で到達できない。旧コメントは「〜の必要性が明確化された」という過去の経緯だったため名前が古くても整合していた。

**推奨修正**: 実在する例 (`SampleSliderCell` 等) に差し替えるか、型名を挙げずに「Sample アプリ独自の Cell」とする。

---

### [🟡 Minor] `settingsRoot { section { ... } }` はそのままではコンパイルできない

**該当箇所**: `android/ks-settingsview-compose/build.gradle.kts:15-16`

**問題点**: 「Store 初期値の構築には純粋関数の `settingsRoot { section { ... } }` builder を使う。」とあるが、`SettingsRootScope.section` は `id: String` が必須の第 1 引数 (`SettingsRootScope.kt:38-43`)。`section { }` の形では呼べない (対になる宣言 DSL の `DSLSettingsRootScope.Section` は全引数に既定値があり `Section { }` で呼べる、という非対称がある。同じ `+` 行が両方を並べているため紛らわしい)。

**推奨修正**: `settingsRoot { section("general") { ... } }` のように必須引数を含めるか、`settingsRoot { ... }` に留める。

---

### [🔵 Suggestion] プラットフォーム間・モジュール間で「対の片側だけ直った」箇所

**該当箇所**: 下表

この change で修正された記述には、対になるファイルが未修正のまま残っているものが複数ある。上で Major / Minor として個別に挙げたもの以外にも、以下がある。

| 直った側 | 直っていない対 | 内容 |
|---|---|---|
| `android/ks-settingsview-compose/build.gradle.kts:59-61` (Core 列挙から `Theme` を外し UI 列挙に移した) | `android/ks-settingsview-ui/build.gradle.kts:65` | 「Core モジュール（SettingsRoot / Section / Cell / **Theme** / KsAnyView / RootAccessory 等）」のまま。`Theme.kt` は core に存在せず UI 層にある |
| `android/.../ui/DSLIconModifiable.kt` (準拠 Cell の記述) | `android/ks-settingsview-compose/.../CellModifiers.kt:54-55`、同 `DSLScope.kt:242-243` | いずれも「`SwitchCell` / `CheckboxCell` 等、icon フィールドを持たない Cell」と書くが、両 Cell とも `icon` を持ち `DSLIconModifiableCell` に準拠する。未変更行 |

**推奨修正**: この change に含めるか別債務にするかはオーナー判断。ただし**修正サイクルの工程として、指摘を直したら対になるプラットフォーム / モジュールの同名・同概念ファイルを必ず突き合わせる**ことを推奨する。今回の Major 2 件のうち 1 件と Minor 10 件のうち 3 件は、この突き合わせだけで前回サイクル中に発見できた。

---

### [🔵 Suggestion] 同一 KDoc ブロック内の未変更行と `+` 行が矛盾するクラスタ

**該当箇所**: `android/.../ui/EntryCell.kt:12` (「右側 accessory に `EditText` を配置し」— 実際は `contentRow` の行内 trailing、同ブロックの `+` 行 L16-17 が正)、`android/.../ui/RootHeaderFooterAdapter.kt:8` (「Root H/F（`SettingsRoot.header` / `footer`）専用 Adapter」— 存在しないプロパティ。同ブロックの `+` 行 L18-19 が正)、`android/.../ui/PickerSelectionMode.kt:11-12` (`MutableState<Int?>` — 実際は `Int?`)、`android/.../ui/KsCellRegistry.kt:16` (`RESERVED_*` 定数 — 実際は `VIEW_TYPE_*`)

**問題点**: いずれも未変更行のため deviation.md の「書き換え対象ブロックの周辺に限る。ファイル全体の総点検はしない」に合致し、**違反としては指摘しない**。ただし `RootHeaderFooterAdapter.kt:8` は前回 Major-3 (`RootHeaderFooterAdapterTest.kt:15` の `SettingsRoot.header` / `footer`) とまったく同じ誤りであり、テスト側だけ直してプロダクション側が残った形になっている。

**推奨修正**: 残債務を別 change として起票する際にまとめて拾う。

## 自分で再実行した検証

| 項目 | 結果 |
|---|---|
| `python3 scripts/comment-policy-lint.py --summary` | **禁止 0 件** (検査対象 401 ファイル) |
| `python3 scripts/comment-policy-lint.py --selftest` | **全件 OK** (検出ロジック 11 項目 + hook 疎通 5 項目) |
| `python3 scripts/comment-policy-lint.py --advisory` | 要確認 **2 件** — deviation.md 記載の誤検知 2 件と完全一致。いずれも自然な日本語の条件節で誤検知判定は妥当 |
| lint / hook スクリプト自体の変更 | **なし** (0 件は検査を緩めた結果ではない) |
| `comment-policy:allow` マーカー | リポジトリ全体で **0** |
| **機能コード差分** | **0** — 言語別コメント除去 (Swift/Kotlin/kts の文字列・raw string・ネスト block comment を状態機械で処理、XML は `<!-- -->`) 後に正規化比較。**222 ファイル全件一致 / 未検査 0**。コメント構文で始まらない `+/-` 行 17 件はすべて XML コメントの継続行 |
| Android `./gradlew test --rerun-tasks` | **1986 tests / 0 failures / 0 errors / BUILD SUCCESSFUL** ※初回実行は JIT の SIGSEGV (`libjvm.dylib` の `Node::uncast`) で `:ks-settingsview-ui:testReleaseUnitTest` が exit 134 になった。テスト失敗ではなく JVM クラッシュで、同一条件の再実行で成功。環境側フレークと判断 |
| iOS `xcodebuild test` (iPhone 17 Pro / iOS 26.5) | **624 passed / 0 failed / 0 skipped** |
| 追加行の禁止パターン | 通番 (Phase/Round/Decision/論点/Major-N 等) **0**、アーカイブ文書パス **0**、MUST/SHOULD 等 **0**、裸 change-id **0** (openspec 時代を含む 59 語彙で走査)、履歴記述 **0** (検出 2 件はいずれも合意済み誤検知) |
| ADR 参照の実在性 | 追加行に出る **10 種すべて実在** (core/0005,0006,0008,0009,0010,0011,0013・cross/0016・android/0002,0005)。前回と同一集合 |
| ADR 参照の主旨一致 | 全種の使用文脈を抽出して ADR タイトルと照合。**全件一致** |
| コメント中のリポジトリ内ファイルパス参照 | 3 件すべて実在 |
| コメント中の識別子の実在性 | 追加行のバッククォート / 角括弧参照を全抽出しコーパス照合。**実在しない識別子 0** (※ `SampleLabelCell` はアーカイブ由来でコーパスには載るため本検査では捕捉できず、人手で検出) |
| doc comment の構造 | 空 KDoc・孤立 doc・ファイル末尾破損 **0** |
| deviation 対象外の assertion 文字列 | `KsSettingsViewControllerTests.swift:440` の `"Phase 14.2 で …"` は未変更 (制約遵守) |
| 足場アーティファクト | 実装中の書き換えなし。`tasks.md` / `deviation.md` は新規追加、`exploration.md` の変更は探索フェーズ由来 |
| git 操作 | コミット・ステージいずれもなし (制約遵守) |

**残存する規約違反の件数認識**: 裸 change-id 参照を独立に数えたところ **30 件 / 22 ファイル**。前回報告の「33 件 / 22 ファイル」とファイル数は完全一致、件数差 3 は数え方 (前回は履歴記述との合算) で説明がつく。別 change として起票する方針に影響する規模のズレはない。

## カバー範囲

**全件確認したもの**: 222 ファイルの機械的検証 (機能コード差分・禁止パターン・ADR 実在性・識別子実在性・ファイルパス実在性・doc comment 構造) は全数。

**人手で精読したもの**: 前回未カバー領域を重点に、4 領域 (android core+compose / android ui main / ios Sources / テスト・サンプル・ビルドスクリプト) へ分割して追加 `+` 行を全数精読した。**報告した指摘は全てレビュアー自身が実コードを読んで再確認**しており、裏取りできなかったものは採用していない。前回指摘 7 件の解消確認も全件レビュアー自身が実コードで検証した。

**カバーしなかったもの**: deviation.md で対象外と合意済みの 2 件 (assertion メッセージ文字列内の議論通番 / advisory 誤検知 2 件) は判定対象外。実機での視覚確認は本 change の性質から不要と判断。

## アクションプラン

1. **Major 2 件を修正** — `DatePickerUIStyle.kt:10-11` の選択 UI (`AlertDialog` → `DateSelectionSheet` / android/ADR-0009)、`ios/.../DSLIconModifiable.swift:6-7` の準拠 Cell (Android 側の書き方に揃える)。いずれも同一 KDoc ブロック内の未変更行 (`DatePickerUIStyle.kt:22`、`DSLIconModifiable.swift:27-28`) が同じ誤りを持つので、`DSLIconModifiable.kt` と同じ扱いで併せて直すのが自然
2. **Minor 10 件を修正** — 対称崩れ 3 件 (`SwitchCell.swift` / `CheckboxCell.swift` の `description`、`KsSettingsViewRepresentableTests.swift:8` のスコープ)、`VisibilityAware` 両プラットフォーム、`EntryCell.swift` / `PickerCell.swift` の `description`、`ApplyDiffTest.kt:24-25`、`KsCellRegistry.kt:26-27`、`KsSimpleCheckView.kt:23-24`、`ic_navigate_next.xml:4-5`、`Cell.kt:8-9`、`compose/build.gradle.kts:15-16`
3. **修正の工程を変える** — 指摘を直したら、対になるプラットフォーム / モジュールの同名・同概念ファイルを必ず突き合わせる。今回の指摘 12 件のうち 4 件はこの一手で前サイクル中に潰せた
4. **修正後に lint + 両プラットフォームのテストを再実行**し、機能コード差分 0 を再確認する
5. **Suggestion 2 件はオーナー判断** — 未変更行の矛盾クラスタと `ui/build.gradle.kts:65` を、この change に含めるか残債務の別 change に回すか
