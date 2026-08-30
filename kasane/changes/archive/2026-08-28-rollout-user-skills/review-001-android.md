# レビュー結果: rollout-user-skills (001, kssettingsview-android)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED
**対象**: `skills/en/kssettingsview-android/` と `skills/ja/kssettingsview-android/` の 10 ファイル (SKILL.md + references 4 本 × 2 言語)

## サマリー

構成・frontmatter・翻訳ロックステップ・レシピ形式・コメント規約はデルタスペックを満たしており、ja 訳の品質と description の英語キーワードにも問題はない。API 署名は実 Android ビルドでの検証の結果すべて正しく、`SKILL.md` の最小動作コードはリード文の前提だけでそのままコンパイルが通った。一方で ①「全組み込み Cell が `valueText` を受ける」という源泉 concepts に反する誤記、② references 4 本のリード文が宣言する import 前提では**どのレシピもコンパイルが通らない** (「利用者がコピーして動く」の未達)、③ Store の Section 操作群が丸ごと欠落、の 3 点が Major として残る。

### 検証方法

作業ツリー外の一時領域に `android/` を複製し、そこへ検証専用の Android application モジュールを足して、各レシピをリード文が宣言する import だけで写経してコンパイルした (`compileDebugKotlin`、AGP 8.13.2 / Kotlin 2.4.10)。リポジトリは改変していない。以降の「コンパイルできない」はすべてこの実測に基づく。不足 import を補うと 4 本すべてのレシピがエラーなくコンパイルできたため、**API 署名・引数名・引数順序・型はすべて実装と一致している**ことも同時に確認した。

### 確認して問題がなかった観点

- Skill 一式の構成 (SKILL.md + references 4 本、規定外ファイルなし)、frontmatter 4 フィールドのみ・`metadata.language` とパスの一致・`license: MIT` (ルート LICENSE と一致)
- en/ja の見出し階層の並びが全 5 ファイルで一致、コードブロックの内容一致、意味レベルの等価性 (ja は逐語訳ではなく自然な日本語だが、脱落・追加・意味のずれなし)
- ja `description` の英語キーワード (settings screen / Jetpack Compose / RecyclerView / Cell 12 種の英語名 / CustomCell / SettingsRootStore / Theme / CellStyle / モジュール名) は発火語として適切
- コード例中のコメント 0 件、ローカル絶対パス 0 件、`docs/`・`openspec/` 参照 0 件、内部リンクは全件解決
- 導入節の最低バージョンは `android/gradle/libs.versions.toml` および各 `build.gradle.kts` の値と一致 (Gradle 行は Minor 参照)
- `Theme.Material3.*` 要件と `FragmentActivity` 要件の記述は `android/api/android-native-host.md` と一致
- Store 操作の細部 (index は非表示要素を含む model 位置 / 範囲外 index の clamp / 存在しない ID は no-op / `replaceCells` は空リスト no-op・未知 ID スキップ / `moveSection`・`moveCell` の `to` は取り出し後の挿入位置 / `applyTheme` は構造 Diff を出さない / Store overload に `theme` 引数がない / `bind` 前後の更新も attach 後に収束 / detach→再 attach でスクロール位置は復元されない) はすべて源泉と一致
- identity の記述 (`forEach` key と明示 ID の併用禁止、hint と最終 ID は別物、1 item = 1 要素、回転復元に安定 ID が要る) は `core/architecture/declarative-tree-identity.md` / `android/api/android-compose.md` と一致
- Registry の記述 (`CELL_VIEW_TYPE_MIN` = 100 の予約、同型再登録は後勝ち、別型への同一 viewType は失敗、`strictMode` 既定 true・build 種別に自動追従しない、false で高さ 0 placeholder) は `android/api/android-native-host.md` と一致
- CustomCell の記述 (content の値等価要求、builder / onTap は等価性に不参加、`showArrow` と `onTap` は独立、無効時は content 全体の淡色化 + TalkBack 除外、CellStyle は背景色と高さのみ有効・`icon` は no-op、`remember` 状態の生存を仮定しない) は `core/cells/custom-cell.md` / `core/styling/cell-visual-states.md` と一致
- `font` modifier が hintText の font を変えない旨は実装 (`EffectiveStyle` の hint font 解決は `CellStyle.hintTextFont` → `Theme.cellHintFont` → 既定) と一致。`CellHandle.font` の KDoc は「タイトル / ヒントテキスト用」と書いているが、Skill 側の記述が正しい

## 指摘事項

### [🟠 Major] 「組み込み Cell はすべて valueText を受ける」は誤り (EntryCell は非公開)

**該当箇所**: `skills/en/kssettingsview-android/references/cells.md:240` / `skills/ja/kssettingsview-android/references/cells.md:240`

**問題点**: 「Every built-in cell accepts `description` ..., `valueText` ..., and `hintText` .... `ButtonCell` is the one exception: it has no `description`.」(ja: 「組み込み Cell はすべて `description` ...、`valueText` ...、`hintText` ... を受ける。例外は `ButtonCell` で、これだけ `description` を持たない。」) と書かれているが、`EntryCell` は `valueText` を持たない。源泉概念 `kasane/concepts/core/cells/input-cells.md` は「`EntryCell` は入力 control 自身が値を表示するため `valueText` を持たず、`text` を使う」と明記しており、記述が源泉と真っ向から矛盾している。`android/ks-settingsview-ui/.../EntryCell.kt` の `data class EntryCell` にも `jp.kamusoft.kssettingsview.compose.EntryCell` の DSL 拡張関数にも `valueText` は無い。実測でも `EntryCell(title = ..., text = ..., valueText = "v")` は `No parameter with name 'valueText' found.` でコンパイルに失敗する。例外は 2 つあるのに 1 つと断言しているため、利用者は必ず躓く。

**推奨修正**: 例外を 2 件に直す。例: 「`ButtonCell` has no `description`, and `EntryCell` has no `valueText` (the text field itself shows the value — use `text`).」/ 「例外は 2 つ。`ButtonCell` は `description` を持たず、`EntryCell` は `valueText` を持たない (入力欄自身が値を表示するため `text` を使う)。」

**備考**: `review-001-ios.md` でも同型の指摘 (iOS の `EntryCell` は `valueText` 非公開) が出ている。同じ原因の en/ja 4 ファイル横断の誤りとして一括で直すのが望ましい。

### [🟠 Major] references 4 本のリード文が宣言する import 前提ではレシピがコンパイルできない

**該当箇所**: `references/cells.md:3` / `references/updates.md:3` / `references/styling.md:3` / `references/custom-cells.md:3` (en/ja 共通)

**問題点**: デルタスペックの「生成の内容規約 ④」は「API 署名とコード例は … 実装コード・テストで最終確認する (利用者がコピーして動くこと)」を要求している。各 references はリード文で import の前提を宣言しているが、その前提だけでは実際にはどのレシピもコンパイルが通らない。実測で確認した不足は以下のとおり。

- `cells.md:3` は「最小動作コードの import + 使う Cell 関数を `jp.kamusoft.kssettingsview.compose` から」と宣言するが、
  - `KsImage` (`cells.md:232,233,248`) と `DatePickerUIStyle` (`cells.md:220`) は `jp.kamusoft.kssettingsview.compose` ではなく **`jp.kamusoft.kssettingsview.ui`** にある。リード文が案内する package が誤りで、宣言どおり import しても解決しない
  - `Color` (`cells.md:46` の `Color.Red`)、`java.time.LocalTime` (`cells.md:200`)、`java.time.LocalDate` (`cells.md:214`) が前提に含まれていない
  - `var x by remember { mutableStateOf(...) }` (`cells.md:64,78,92,106,267`) は `androidx.compose.runtime.getValue` / `setValue` を要求する。最小動作コードは `val` + `MutableState` 形なのでこの 2 つを含んでおらず、`Property delegate must have a 'getValue' method` で落ちる
- `updates.md:3` の宣言では `KsIdentifiable` / `forEach` (`updates.md:129,133`)、`cellID` / `sectionID` (`updates.md:149,150`)、`getValue` / `setValue` (`updates.md:161`) が不足する。加えて DSL の `LabelCell` と `jp.kamusoft.kssettingsview.ui.LabelCell` は同一ファイルに共存できないため、名前衝突に触れていながら回避策 (別ファイルに置く / import alias) が示されていない
- `styling.md:3` は「Compose の型 (`Color` / `TextStyle` / `Dp`) と使うスタイル名」と宣言するが、実際に必要なのは `Dp` ではなく `androidx.compose.ui.unit.dp` / `.sp` 拡張 (`styling.md:38,51,53,76-79`)。さらに `FontWeight`、`PaddingValues`、`Row` / `Alignment` / `Icon` / `Icons` / `Text` / `MaterialTheme` (`styling.md:113-115,128-129`)、および modifier 拡張関数 (`titleColor` / `backgroundColor` / `font` / `icon` / `cellHeight` / `sectionHeader` / `sectionFooter`) が前提から漏れている
- `custom-cells.md:3` は「`core` と `ui` から使う名前」と宣言するが、`Row` / `Spacer` / `fillMaxWidth` / `padding` / `Alignment` / `Modifier` / `dp` / `Slider` / `Text` / `Icon` / `Icons` / `toArgb` / `mutableFloatStateOf` / `getValue` / `setValue` (`custom-cells.md:10-27,37-42,50-56,66-68,91-104`)、`android.view.View` / `LayoutInflater` / `android.widget.TextView` / `ProgressBar` / `ColorStateList` (`custom-cells.md:125-137,152-155`)、`cellHeight` が前提に含まれていない

**推奨修正**: 次のいずれかで「前提どおりに書けばコンパイルが通る」状態にする。(a) 各 references のリード文に、そのファイルが使う **Compose / Android 標準側の import も含めた完全な前提リスト**を書く (`getValue` / `setValue` / `dp` / `sp` / `Modifier` / レイアウト・Material3 の各 Composable、`java.time.*`、`android.text.InputType`)。(b) それが冗長なら、代表レシピ 1 本 (各ファイルの最初のコード) を `updates.md` 最終スニペットと同様に import 込みの完全形にし、残りはその差分だけを述べる。いずれの場合も `KsImage` / `DatePickerUIStyle` / modifier 拡張の所属 package (`...ui` と `...compose`) を正しく書き分けること。

**参考**: `updates.md` の「Host the screen from XML」スニペット (`updates.md:187-225`) は import を全部書いており、そのままコンパイルが通った。同じ水準を他のレシピにも適用すれば解消する。

### [🟠 Major] Store の Section 操作 (`insertSection` / `removeSection` / `replaceSection` / `replaceAll`) が欠落している

**該当箇所**: `skills/en/kssettingsview-android/references/updates.md` 全体 / `skills/ja/kssettingsview-android/references/updates.md` 全体 (源泉: `kasane/concepts/android/api/android-native-host.md` の「公開 API > SettingsRootStore」表)

**問題点**: `updates.md` は「表示中の設定画面を変えるためのレシピ」と名乗り、`SKILL.md` の能力マップも「表示中の画面を変える: 行の挿入・削除・移動・差し替え、複数行のバッチ更新」を掲げている。しかし Section に対する公開操作のうち扱われているのは `moveSection` のみで、`insertSection` / `removeSection` / `replaceSection` と Root 全体の `replaceAll` が en/ja とも 1 度も現れない。源泉概念は Section 操作 4 種と `replaceAll` を Store の公開 API として明示列挙しており、「表示後に Section を足す・消す」は設定画面では珍しくない要求であるため、担当 Skill への反映漏れとして扱うべきである。あわせて `invalidateAccessoryMeasurement` (`core/architecture/store-and-update-streams.md`) も未収録で、これは `styling.md:120` が「Composable の Header は内容で比較されないため、lambda の中身を変えただけでは model の変更として検出されない」と問題提起している状況の唯一の対処手段であるにもかかわらず、対処が示されていない。

**推奨修正**: `updates.md` に (a)「表示後に Section を足す・消す・差し替える」レシピ (`insertSection` / `removeSection` / `replaceSection`、index は非表示 Section を含む `SettingsRoot.sections` 上の位置である旨を添える)、(b) 大きく作り直すときの `replaceAll`、(c) view accessory の内容がサイズを変えたときの `invalidateAccessoryMeasurement` を追加する。en/ja をロックステップで更新すること。

### [🟡 Minor] PickerCell 単一選択の「確定操作のときだけ書き戻す」が不正確

**該当箇所**: `skills/en/kssettingsview-android/references/cells.md:149` / `skills/ja/kssettingsview-android/references/cells.md:149`

**問題点**: 「The single-selection overload takes a `MutableState<Int?>`, and the value is written back only when the user confirms.」(ja:「単一選択の overload は `MutableState<Int?>` を取り、確定操作のときだけ書き戻す。」) と書かれているが、単一選択には確定操作 (OK ボタン) が無い。源泉 `kasane/concepts/core/cells/picker-selection-surface.md` は「単一選択: … 候補タップで `onSelectionChanged(index)` を1回発火して閉じる (作業状態は持たない)」「OK (強調色で塗ったボタン、**複数選択時のみ**)」と定義しており、実装も `PickerSelectionSheet` が `showConfirm = selectionMode == PickerSelectionMode.Multiple` としている。この表現は「単一選択でも OK を押すまで反映されない」という誤解を与える。なお 2 段下の「確定せずに閉じた場合は選択中の変更を破棄する」は複数選択の作業状態に関する記述であり、単一選択には作業状態そのものが無い点も読み取れない。

**推奨修正**: 単一選択は「候補をタップした時点で書き戻してシートを閉じる (確定ボタンは無い)」、複数選択は「作業状態を持ち、OK でのみ書き戻す。非確定で閉じると破棄する」と、2 つの overload で意味論が違うことを明示する。

**備考**: `review-001-ios.md` にも同型の Suggestion がある。en/ja 4 ファイル横断で揃えるとよい。

### [🟡 Minor] 導入表の「Gradle 9.5.0」は最低版ではない

**該当箇所**: `skills/en/kssettingsview-android/SKILL.md:39` / `skills/ja/kssettingsview-android/SKILL.md:39`

**問題点**: 表の見出しが Minimum / 最低バージョンであるのに対し、Gradle 行の `9.5.0` はライブラリ側の wrapper が固定している版であって最低要件ではない。`android/gradle/libs.versions.toml` は「AGP 8.13.2。要求する Gradle の最低版は 8.13」「Kotlin 2.4 系がサポートする Gradle は 7.6.3〜9.5.0」と明記しており、9.5.0 はむしろサポート上限側の値。利用者アプリに Gradle 9.5.0 を要求する読み方になるため、実際より厳しい要件を課す。

**推奨修正**: Gradle 行を最低版 `8.13` に改める (必要なら「検証済みは 9.5.0」を併記する)。他の行 (minSdk 29 / compileSdk 35 / JDK 17 / Kotlin 2.4.10 / AGP 8.13.2 / Compose BOM 2024.10.01) はビルドファイルと一致しており修正不要。

### [🟡 Minor] 源泉に存在する公開パラメータのうち利用頻度の高いものが未収録

**該当箇所**: `references/cells.md` / `references/styling.md` (en/ja 共通)

**問題点**: レシピ形式である以上すべての引数を網羅する必要はないが、「やりたいこと」として自然に立つ次の公開パラメータが en/ja とも一度も現れない。

- `DatePickerCell.minDate` / `maxDate` — 選択可能な日付範囲の制限 (源泉: `core/cells/date-picker-selection-surface.md`「範囲制限」)
- `PickerCell.displayFormatter` / `pageTitle`、`NumberPickerCell.pickerTitle`、`TimePickerCell` / `DatePickerCell.pickerTitle` — 選択面のタイトルと候補の表示整形 (源泉: `core/cells/input-cells.md`、各選択面 concept)
- 各入力 Cell の `accentColor` — 選択印・ダイアログ強調色の 3 段解決の入口 (源泉: 各選択面 concept のスタイル継承表)
- `CellStyle.iconSize` / `iconRadius`、`Theme.cellIconSize` / `cellIconRadius` — アイコンの大きさ・角丸 (源泉: `core/styling/style-resolution.md`、`core/styling/cell-row-layout.md`)

**推奨修正**: 少なくとも `minDate` / `maxDate` (日付の範囲制限) と icon サイズ・角丸は、それぞれ既存レシピへの 1 行追記または短いレシピ追加で拾う。残りは任意。

### [🔵 Suggestion] `Theme.rowHeight` と `CellStyle.cellHeight` の型差に注意書きがない

**該当箇所**: `skills/en/kssettingsview-android/references/styling.md:87-90` / `ja` 同箇所

**問題点**: 「Height resolves from `CellStyle.cellHeight`, then `Theme.rowHeight`, then the platform minimum of 60dp」と 2 つを同列に並べているが、`CellStyle.cellHeight` は `Dp?`、`Theme.rowHeight` は論理単位の `Int` (未指定は `-1`) で型が異なる。コード例が `rowHeight = 64` なので気づけはするが、`rowHeight = 64.dp` を書いて詰まる導線がある。

**推奨修正**: 「`Theme.rowHeight` takes a plain logical-unit `Int` (`-1` = unspecified), while `CellStyle.cellHeight` takes `Dp`」の 1 文を添える。

### [🔵 Suggestion] 行高さの下限 (60dp を下回らない) が書かれていない

**該当箇所**: `skills/en/kssettingsview-android/references/styling.md:87` / `ja` 同箇所

**問題点**: 60dp は「どこにも指定がないときの fallback」としてのみ説明されているが、源泉 `core/styling/cell-row-layout.md` と `android/api/android-native-host.md` は「最終値は platform の最低行高を下回らない」と定めており、`Theme(rowHeight = 40)` を指定しても 60dp になる。`hasUnevenRows = false` で固定高を狙う利用者が意図した高さにならない場面がある。

**推奨修正**: 「最終的な行高は 60dp を下回らない (60dp 未満を指定しても 60dp になる)」を追記する。

### [🔵 Suggestion] DSL 対応インターフェースの所属 package が書かれていない

**該当箇所**: `skills/en/kssettingsview-android/references/custom-cells.md:172,181` / `ja` 同箇所

**問題点**: 「Implement `DSLReidentifiableCell` …, `DSLStyleModifiableCell` …, and `DSLIconModifiableCell` …」と 3 つを並べているが、`DSLReidentifiableCell` は `jp.kamusoft.kssettingsview.core`、`DSLStyleModifiableCell` / `DSLIconModifiableCell` は `jp.kamusoft.kssettingsview.ui` にあり、所属が分かれている。リード文の「core と ui から使う名前」だけでは、どちらから import するか試行錯誤になる。

**推奨修正**: 3 つの所属 package を 1 行で明示する (Major 2 の import 前提整理と併せて対応できる)。

### [🔵 Suggestion] 導入節が 3 モジュールを挙げながら依存の書き方を示していない

**該当箇所**: `skills/en/kssettingsview-android/SKILL.md:30` / `ja` 同箇所

**問題点**: 「アプリから依存する Gradle モジュールは 3 つ」と述べるが、依存宣言の書き方 (座標) がない。ただし `kasane/concepts/cross/conventions/public-identifiers.md` は「Android module には現在 `maven-publish` の設定がない」「composite build が解決する開発用 GAV を公開済みの配布座標と説明しない」としており、現時点で正しい座標を書けないこと自体は源泉に照らして妥当。

**推奨修正**: 現状の記述のままでも規約違反ではないが、「Maven 公開は未提供のため、現在は composite build / ローカル参照で取り込む」旨を 1 文添えると利用者が止まらない。座標を書く場合は公開済みと誤読されない表現にすること。

## アクションプラン

1. **Major 1** — `cells.md` の共通フィールド記述を「例外は `ButtonCell` (description なし) と `EntryCell` (valueText なし) の 2 つ」に修正 (en/ja)。`review-001-ios.md` の同型指摘と併せて 4 ファイル一括で直す
2. **Major 2** — references 4 本のリード文の import 前提を、実際にコンパイルが通る内容へ修正 (en/ja 計 8 ファイル)。特に `KsImage` / `DatePickerUIStyle` / modifier 拡張の package 誤りと `getValue` / `setValue` の欠落は必須。修正後は再度コンパイル実測で確認することを推奨
3. **Major 3** — `updates.md` に Section 操作 4 種 (`insertSection` / `removeSection` / `replaceSection`) と `replaceAll`、`invalidateAccessoryMeasurement` のレシピを追加 (en/ja ロックステップ)
4. **Minor 1** — PickerCell 単一選択の確定意味論を訂正
5. **Minor 2** — 導入表の Gradle 行を最低版 `8.13` へ
6. **Minor 3** — `minDate` / `maxDate` と icon サイズ・角丸を最低限拾う
7. **Suggestion 1〜4** — 任意。2〜3 は Major 2 の修正と同じ箇所を触るため同時対応が効率的
8. 修正後に完了検査一式 (タスク 4.1) の ②③④ (en/ja 節構成・コードブロック byte 一致・frontmatter) を再実行する
