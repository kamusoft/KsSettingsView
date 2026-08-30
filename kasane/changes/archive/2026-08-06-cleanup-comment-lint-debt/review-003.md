# レビュー結果: cleanup-comment-lint-debt (003 回目)

**日付**: 2026-08-06
**判定**: CHANGES_REQUESTED

## サマリー

**review-002 の指摘 14 件 (Major 2 / Minor 10 / Suggestion 2) はすべて解消**を確認した。書き換え後の記述はいずれもレビュアー自身が実コードで裏取りしており、対称性チェックを工程に入れた効果も確認できた — `VisibilityAware` / `DSLIconModifiable` / `PickerSelectionMode` / `RootHeaderFooterAdapter` はいずれも iOS ↔ Android で記述が一致し、`Theme` の所属も core / ui / compose の 3 モジュールすべてで統一された。機械的検証 (機能コード差分 0 / lint 禁止 0 / ADR 実在 11 種全件 / 識別子実在 259 種全件) も全通過し、Android 1986 テスト・iOS 624 テストがいずれも全成功した。

一方で **新規指摘が Major 2 / Minor 15 / Suggestion 4 出た**。前 2 回と違い、**これらは「まだ精読していない領域」から出たものではない** — 大半は review-002 が「追加 `+` 行を全数精読した」と報告した領域から出ており、1 件は **review-002 の指摘を直した結果として新たに混入した誤り** (`ApplyDiffTest.kt:24-25`)。詳細は「新規指摘の出所」節に書く。この事実は次サイクルの判断材料として最も重要なので、指摘一覧より先に読んでほしい。

最も重い `EntryCellView.swift:9-10` は、アーカイブ済み spec の引用文を現在形の実装説明に変換した結果、**同一ファイル 48 行下の防御的コメントと真っ向から矛盾**し、記述どおりに直すと `secureTextEntry` のレイアウト崩れ (コードが明示的に防いでいる現象) が再発する。

---

## 新規指摘の出所 (オーナーの判断材料)

オーケストレーターからの問い「新規指摘は未精読領域からか、精読済み領域からか」への回答。

| 分類 | 件数 | 内訳 |
|---|---|---|
| **review-002 が精読済みと報告した領域からの取りこぼし** | 大半 | `EntryCellView.swift` / `CheckboxCell.kt` / samples 3 件 / `BasicCellsTest.kt` など。review-002 は 4 領域 (android core+compose / android ui main / ios Sources / テスト・サンプル・ビルドスクリプト) に分割して「追加 `+` 行を全数精読した」と報告しているため、いずれもカバー範囲内 |
| **今回の修正が新たに持ち込んだ誤り** | 1 | `ApplyDiffTest.kt:24-25` — review-002 Minor (「`submitList` 平坦リストを検証していない」) の修正で、検証していない `Theme` 更新を新たに書き足してしまった |
| **未精読領域から** | 0 | 3 回のレビューで領域単位の未カバーは残っていない |

**含意**: 「サイクルを重ねれば未カバー領域が尽きて収束する」という前 2 回のモデルは今回で崩れた。約 814 行の書き換え済みコメント行に対する事実誤認の密度が、1 サイクルあたりのレビュー検出能力を上回っている。同じ方法で 4 回目を回しても同様に新規指摘が出る可能性が高い。**サイクル継続よりも、対象を絞った検査 (下記アクションプラン 5) か、残りを別 change に切り出す判断を推奨する**。

---

## review-002 指摘 14 件の解消状況

| # | 前回指摘 | 状態 | レビュアーによる裏取り |
|---|---|---|---|
| Major-1 | `DatePickerUIStyle.kt:10-11` `AlertDialog` | **解消** | 「ボトムシート + 年/月/日の3連ホイール（[DateSelectionSheet]、android/ADR-0009）」に修正。`DateSelectionSheet.kt:299` に実在、`kasane/decisions/android/0009-*.md` 実在、`AlertDialog` は `ks-settingsview-ui/src/main` から **0 件**。enum メンバ doc (L22-24) も同時修正済み |
| Major-2 | `ios/DSLIconModifiable.swift:6-7` 準拠 Cell 2 種 | **解消** | 「`icon: KsImage?` を持つ Cell が準拠する。アイコン領域を持たない `CustomCell` は準拠せず」に修正。iOS UI 層 Cell 13 種中 12 種が `DSLIconModifiable` 準拠、`CustomCell.swift:57` のみ非準拠を確認。誤っていた未変更行 L27-28 も削除され、Android 側 `DSLIconModifiable.kt` と記述が揃った |
| Minor-1 | `VisibilityAware` 7 種 (iOS/Android) | **解消** | 両方とも「UI 層が提供する Cell は `CustomCell` を含めてすべて opt-in 準拠する」に修正。iOS 13 種全件・Android 13 種全件 (`CustomCell.kt:58` 含む) が準拠することを宣言行で確認。直後の「非準拠の Cell」の記述も「ライブラリ利用者が独自定義した型」に修正され矛盾解消 |
| Minor-2 | `ios/SwitchCell.swift` / `CheckboxCell.swift` の `description` | **解消** | 両方とも「`description` / `valueText` / `icon` / `hintText`」の 4 点列挙に修正。兄弟の `RadioCell.swift` / `SimpleCheckCell.swift` と書き方が揃った |
| Minor-3 | `ios/EntryCell.swift` / `PickerCell.swift` の `description` | **解消** | 両方とも `description` を列挙に追加。`EntryCell.swift:32` / `PickerCell.swift:29` の `public let description: String?` と一致 |
| Minor-4 | `ApplyDiffTest.kt:24-25` `submitList` 平坦リスト | **解消 (ただし別の誤りが混入)** | `submitList` の記述は削除された。assert は `internalRoot` 17 / `internalTheme` 1 / `rootHeader` 1 / `rootFooter` 1 で「平坦リスト」の検証がないことと整合。**ただし新たに書かれた「Theme」が誤り → 新規指摘 m-1** |
| Minor-5 | `KsSettingsViewRepresentableTests.swift:8` スコープ | **解消** | 「`SettingsRootStore` 経由の更新、`KsSettingsViewStyle` の反映と切り替え、および Root H/F modifier」に修正。同ファイル 7 テストの内訳 (style 系 3 本を含む) と一致 |
| Minor-6 | `KsCellRegistry.kt:26-27` 呼び出し元 | **解消** | 「同モジュール内の `KsSettingsListAdapter` から呼び出される」に修正。`createViewHolder` の呼び出しはリポジトリ全体で `KsSettingsListAdapter.kt:152` の 1 箇所のみを再確認 |
| Minor-7 | `KsSimpleCheckView.kt:23-24` 共有先 2 箇所 | **解消** | 「行内の選択印を描く各所（`RadioCellViewHolder` / `SimpleCheckCellViewHolder` / `PickerSelectionSheet`）」に修正。3 箇所 (`:72` / `:74` / `PickerSelectionSheet.kt:395`) を確認。あわせて deviation 記載の「〜を移植したもの」も「〜と同じ描画ロジックを持つ」に修正済み |
| Minor-8 | `ic_navigate_next.xml:4-5` 「同型」 | **解消** | 「〜に対応するアイコン。」に修正。直後の「原典からの意図的な差」と両立する |
| Minor-9 | `Cell.kt:8-9` `SampleLabelCell` | **解消** | 型名を挙げない「Sample アプリ独自の Cell を定義できないためである」に修正。`SampleLabelCell` はリポジトリ全体で **0 件** |
| Minor-10 | `compose/build.gradle.kts:15-16` `section { }` | **解消** | 「`settingsRoot { section("general") { ... } }` builder を使う（`section` は ID が必須）」に修正。`SettingsRootScope.kt:38` の `fun section(id: String, ...)` と一致 |
| Suggestion-1 | `ui/build.gradle.kts:65` の `Theme` | **解消** | Core 列挙から `Theme` を除去。あわせてファイル冒頭に「スタイル型（`Theme` / `CellStyle`）を含む」を追記。`Theme.kt` / `CellStyle.kt` / `KsImage.kt` はいずれも `ks-settingsview-ui` にあり core に無いことを確認 |
| Suggestion-2 | `compose/CellModifiers.kt:54-55` / `DSLScope.kt:242-243` | **解消** | 両方とも「アイコン領域を持たない `CustomCell`」に修正。iOS の対 (`SwiftUI/CellModifiers.swift:155-157`) も同時修正され、4 ファイルすべてが一致 |

**deviation.md 記載の追加修正 3 件も適切**。`DatePickerUIStyle.swift` は Android 側の実型名 (`DatePickerUIStyle`) を正しく参照する記述になり、`KsIdentifiable.kt` の KDoc サンプルは `LabelCell(title = item.name)` に差し替えられてコンパイル可能な形 (`BasicCellDsl.kt:215` の `DSLSectionScope.LabelCell`、`DSLScope.kt:31` の `Section(header: String? = null, ...)`) になり、`KsSimpleCheckView.kt` の履歴記述も解消した。`SampleLabelCell` / `DatePickerAndroidStyle` の残存は **0 件**。

---

## 指摘事項

### [🟠 Major] `EntryCellView` の Hugging / CCR 記述が実装と逆で、同一ファイル内の防御的コメントと矛盾する

**該当箇所**: `ios/Sources/KsSettingsViewUI/EntryCellView.swift:9-10`

**問題点**:
```swift
// `UIListContentConfiguration` / `UICellAccessory` は使わず、共通行レイアウトへ `UITextField` を
// `trailingViews` として追加する（core/ADR-0011）。textField の Hugging / CCR を `.defaultLow` に
// することで title 右側残り領域全幅を取得する。
```
実装 (`EntryCellView.swift:53-62`) は 3 点すべて異なる。

- 優先度を設定しているのは **`textField` ではなく `fieldWrapper`**
- Hugging は `.init(100)` であり `.defaultLow`（= 250）ではない
- **CCR は `.required`**。しかも同ファイル `:57-60` の未変更コメントが「CCR を **required** に: textField (secureTextEntry) の intrinsicContentSize が 19pt 程度に縮むため、**CCR が低いと wrapper も 19pt に圧縮される**」と、`.defaultLow` にしてはいけない理由を明示している

`-` 行はアーカイブ済み spec の引用 (`仕様（拡張）: openspec/changes/migrate-cell-base-to-stack-layout/...` の一部) だったため「spec がそう書いていた」という記述として成立していたが、現在形の実装説明に変換したことで実装と正面衝突した。記述に従ってリファクタすると、コードが明示的に防いでいる `secureTextEntry` のレイアウト崩れが再発する。**規約の書き換え類型 1 (定型句型) を適用すべきところに、引用本文を残してしまった形**。

**推奨修正**: `fieldWrapper` の Hugging を低く・CCR を required にすることで title 右側残り領域全幅を確保する、という実装どおりの説明に直す（`:53-62` の既存コメントが正しいので、それを要約する形が自然）。

---

### [🟠 Major] Android `CheckboxCell` が「`onValueChanged` でモデルへ書き戻す」と書いているが、書き戻し経路は存在しない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CheckboxCell.kt:12`

**問題点**:
```kotlin
 * `AiForms.Maui.SettingsView` と同じく `isChecked` は TwoWay バインディング相当で、
 * ユーザー操作による変化は `onValueChanged` でモデルへ書き戻す。
```
後半は `+` 行で新規に書き足された。しかし Android 実装に書き戻し経路はない。

- `CheckboxCellViewHolder.kt:56-58` は `handler.invoke(value)` を呼ぶだけで、Cell 値にも Store にも書き戻さない
- Compose DSL 側にも TwoWay overload はない (`BasicCellDsl.kt:358` の `CheckboxCell` は `isChecked: Boolean` の値受け取りのみ。`MutableState` overload を持つのは `BasicCellDsl.kt:301` の `SwitchCell` だけ)
- `kasane/concepts/core/cells/basic-cells.md:20` も「基本 7 種では Android `SwitchCell` の `MutableState<Boolean>` overload だけが該当し、他 6 種は値 + callback」と明記

さらに **同一モジュールの兄弟 `SimpleCheckCell.kt:14-15` は「タップ時に `onValueChanged` を呼ぶのみ（実際の `isChecked` 更新は利用者責務）」と正反対を書いている**。iOS の対 (`CheckboxCell.swift:18`) は「`Checked` は TwoWay バインディング相当。」で止めており、この節を持たない。つまり **プラットフォーム間・兄弟ファイル間の両方で非対称が新規に生じている**。

公開 API の KDoc であり、読んだ利用者は「チェックすればモデルが自動更新される」と結論して状態配線を省く。前回 Major-2 (`.icon(_:)` が `SwitchCell` で no-op だと誤読させる) と同種・同程度の害。

**推奨修正**: 前半の「AiForms と同じく TwoWay バインディング相当」は原典由来の互換仕様として妥当なので残し、後半を `SimpleCheckCell.kt:14-15` の書き方に揃える（「タップ時に `onValueChanged` を呼ぶ。実際の `isChecked` 更新は利用者責務」）。あわせて iOS 側と揃える。

---

### [🟡 Minor] `ApplyDiffTest` の書き換えが、検証していない Theme 更新を新たに書き足した — 前回指摘の修正による回帰

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ApplyDiffTest.kt:24-25`

**問題点**:
```kotlin
 * [SettingsRootDiff] の各差分種別を適用したとき、内部保持 root と Theme、および Root H/F が
 * 期待どおり更新されることを保証する。
```
`applyDiff` は Theme を更新しない。

- `KsSettingsView.kt:439-441` の `Full` ケースは `setRootDirect(diff.root, internalTheme)` と **既存の** `internalTheme` を渡すだけ
- `KsSettingsView.kt:39-41` の未変更 KDoc が「`applyDiff` は Theme 更新を含まない（構造差分のみ）」と明言
- 同ファイル `ApplyDiffTest.kt:226` の見出しが「Theme 更新（… applyDiff 経路ではなくなった）」、テスト名も `:229` で「`view theme プロパティ更新で internalTheme が反映される（Diff 経路ではない）`」

これは **review-002 Minor-4 (「検証していない `submitList` 平坦リストを保証すると書いている」) を直す過程で、別の検証していない対象を書き足してしまった**もの。指摘の型がそのまま再現している。

**推奨修正**: 「内部保持 root と Root H/F が期待どおり更新されることを保証する」に直す（Theme は Diff 経路外なので落とす）。

---

### [🟡 Minor] `SettingsRootDiff` は 10 ケースだが「全 11 ケース」と書かれている (3 箇所)

**該当箇所**: `android/.../ui/ApplyDiffTest.kt:22` (`+` 行)、`ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift:4`、`ios/Tests/KsSettingsViewCoreTests/SettingsRootDiffTests.swift:12`

**問題点**: `SettingsRootDiff` のケースは両プラットフォームとも **10 個** — `Full` / `InsertSection` / `RemoveSection` / `MoveSection` / `ReplaceSection` / `InsertCell` / `RemoveCell` / `ReplaceCell` / `MoveCell` / `UpdateAccessory` (`android/.../core/SettingsRootDiff.kt:19-79`、`ios/.../Core/SettingsRootDiff.swift:19-41`)。`applyDiff` の分岐も 10 本。11 は削除済み `UpdateTheme` を含んでいた時代の数え上げ。

`ApplyDiffTest.kt:22` は `+` 行で、実装者が同じ行を書き換えながら誤った数だけ持ち越した。この change 自身が別ファイルで「本型に Theme 更新のケースは持たない」と何度も書いているため、**同一 change の編集内で自己矛盾している**。iOS 2 箇所は未変更行だが、`ApplyDiffTests.swift:4` は同一コメントブロック内の他行が削除されている (触ったブロック)。

**推奨修正**: 3 箇所とも「全 10 ケース」に直すか、数を書かず「全ケース」とする (`SettingsRootDiffTests.swift` のファイルヘッダは既に「全ケースについて」と数を落としており、その書き方に揃えるのが自然)。

---

### [🟡 Minor] 「`header` テキスト空なら supplementary を生成しない」が実装と不一致 (Core / UI 層の対)

**該当箇所**: `ios/Sources/KsSettingsViewCore/Section.swift:32-34`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:572`

**問題点**: `Section.swift` の `headerHeight` doc は「`header` テキスト**空または**未設定なら UI 層は Header supplementary 自体を生成しない」(結論部が `+` 行)、`KsSettingsViewController.swift:572` は「header 非空 → `.estimated(20)` で再生成」(`+` 行)。

実装 (`KsSettingsViewController.swift:568-570`) は `if section.header == nil { return nil }` すなわち **nil 判定のみ**。`.text("")` は `nil` にならず `.estimated(20)` の boundary item が生成される。`supplementaryModes` (`:665`) も header は `$0.header != nil || $0.headerHeight > 0` としか見ていない。空文字列の除外は **footer 側のみ**実装されている (`shouldShowFooter` `:586-590`) — つまり header と footer で挙動が非対称であり、その非対称こそが記述すべき事実。

`Section.swift` は Core の公開 API doc なので害が大きい。

**推奨修正**: 「`header` 未設定 (`nil`) なら UI 層は Header supplementary 自体を生成しない」に限定する。footer との非対称を書き添えるとなお良い (`shouldShowFooter` は `.text("")` も非表示扱いする)。

---

### [🟡 Minor] Sample の `MainActivity` は `FragmentActivity` なのに「ComponentActivity ベース」と新規に書き足している

**該当箇所**: `samples/android/app/src/main/AndroidManifest.xml:5`

**問題点**: `+` 行「ComponentActivity ベースの単一 Activity（MainActivity）をエントリポイントとする。」に対し、`MainActivity.kt:37` は `class MainActivity : FragmentActivity()`。しかも `MainActivity.kt:32-35` が「基底クラスは [FragmentActivity]（**`ComponentActivity` ではない**）。… `FragmentActivity` でないと picker が無反応になる」と、名指しで否定したうえ理由まで書いている。

継承関係上は `FragmentActivity ⊂ ComponentActivity` なので嘘ではないが、**リポジトリ自身が明示的に否定している表現を新規に書き足した**形。`-` 行はアーカイブ spec のタスク名の引用だったため、それを現在形の断定に昇格させたことで矛盾が生じた。

**推奨修正**: 「`FragmentActivity` ベースの単一 Activity（`MainActivity`）をエントリポイントとする」に直す。

---

### [🟡 Minor] `BasicCellsDemoView` は色を 1 つも構築していないのに「`UIColor` で直接構築する」と書いている

**該当箇所**: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift:10-11`

**問題点**: `+` 行「色はスタイル層の Native 型である `UIColor` で直接構築する（core/ADR-0009）。中間の論理色表現は挟まない。」

このファイルに `UIColor(` の出現は **0 件** (`UIColor` の語自体がこのコメント行にしかない)。色の参照は `SampleTheme.mauiTitleText` (`:164`) と `.theme(SampleTheme.maui)` (`:171`) のみで、`UIColor` の直接構築は `SampleTheme.swift:22-40` にある。

`-` 行は「このファイルで `KsColor` → `UIColor` に書き換えた」という**履歴記述**だった。規約の類型 3 は「現在の仕様の説明に書き換えるか、**情報価値がなければ削除する**」と定めており、色定義が `SampleTheme.swift` へ移った現在、このファイルの説明としては成立しないため削除が正しい選択だった。

副次的に、「スタイル層」という語はリポジトリ全体でこの 1 行にしか存在しない (ADR-0009 本文も Android 側の対も「UI 層」で統一)。

**推奨修正**: この 2 行を削除する (色定義は `SampleTheme.swift` にある旨を書くなら、そちらの記述に寄せる)。

---

### [🟡 Minor] 存在しないリソース名 `StylesAndColors` を現在形で参照している

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt:21`

**問題点**: `+` 行「各色は `AiForms.Maui.SettingsView` の Sample が持つ StylesAndColors を踏襲する。」

移植元リポジトリ (`../AiForms.Maui.SettingsView`) に `StylesAndColors` という名前のファイル・リソースは存在しない (grep ヒット 0)。当該色は `Sample/Views/MainPage.xaml` の `<ContentPage.Resources>` に定義されている。

**同一ファイル内で矛盾している** — `SampleTheme.kt:9` の KDoc は正しく `AiForms.Maui.SettingsView/Sample/Views/MainPage.xaml` と書いている。iOS の対 (`SampleTheme.swift`) にこの誤参照はない。`SampleLabelCell` / `DatePickerAndroidStyle` と同じ「grep で到達できない名前」のクラスであり、オーナーがそれらを修正対象に引き上げた判断がそのまま当てはまる。

**推奨修正**: `SampleTheme.kt:9` と同じく `Sample/Views/MainPage.xaml` を参照するか、名前を挙げず「Sample の色定義を踏襲する」とする。

---

### [🟡 Minor] Switch の色分離を「AiForms と同じ」と断定しているが、実装は Material 3 トークンで、同ファイル内の別コメントが「Material 3 標準に揃えた」と説明している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt:1081`

**問題点**: `+` 行「移植元 AiForms の `SwitchCellView.cs` と**同じ** Thumb / Track 色分離を保つ。」(`-` 行は「参照: AiForms.Maui.SettingsView/…/SwitchCellView.cs」という単なる参照)

実装 `SwitchCellViewHolder.kt:50-73` は Material 3 トークン (`colorOnPrimary` / `colorOutline` / `colorSurfaceContainerHighest`) で着色している。同ファイル `BasicCellsTest.kt:1131-1134` の**書き換え後 KDoc** が「Material 3 標準に揃え オフ Track → `colorSurfaceContainerHighest`、オフ Thumb → `colorOutline`」と、むしろ原典から離れたことを説明しており、**同じ change の編集どうしが食い違っている**。加えて AiForms のソースはこのリポジトリに同梱されていないため「同じ」は裏取り不能。

`-` 行の「参照:」は出典表示として無害だったが、「同じ」への昇格が検証不能な断定を作った。

**推奨修正**: 「Thumb と Track の色を分離する」に留めるか、`:1131-1134` の Material 3 説明に寄せる。

---

### [🟡 Minor] 「各 Cell は共通行を経由する」が `CustomCell` の適用除外 (core/ADR-0015) を無視している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/UnifyCellCommonFieldsTest.kt:19-20`

**問題点**: `+` 行「…の振る舞いを検証する。**各 Cell は**個別に描画するのではなくこの共通行を経由する（core/ADR-0011）。」

`CustomCell` は共通行レイアウト統一の適用除外であり、`kasane/decisions/core/0015-customcell-exemption-from-shared-row-layout.md` (status: accepted) がそれを決定している。「各 Cell」という全称は ADR-0015 と衝突する。

**推奨修正**: 「基本 Cell は」等に限定する。除外の根拠を書くなら `core/ADR-0015` を添える。

---

### [🟡 Minor] `SettingsAccessory` を「Diff DTO 内部専用」と書いているが公開 Store API の引数型である

**該当箇所**: `ios/Sources/KsSettingsViewCore/SettingsAccessory.swift:19-20`

**問題点**: `+` 行「本型は `RootAccessory` / `SectionAccessory` を置き換えない。」に続く「Store API や利用者コードでは個別型を使い、Diff DTO 内部での統一表現専用とする。」

`SettingsRootStore.updateAccessory(target:accessory:)` は **public** で引数型が `SettingsAccessory?` (`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:270`)。利用者は Store 経由で装飾を更新する際に `SettingsAccessory` を直接構築する必要があり、テストもそれを前提にしている (`SettingsRootStoreTests.swift:233`)。「Diff DTO 内部専用」は成立しない。

**推奨修正**: 「Store の `updateAccessory(target:accessory:)` と Diff での統一表現に使い、Section / Root の宣言には個別型 (`RootAccessory` / `SectionAccessory`) を使う」等、実態に合わせる。

---

### [🟡 Minor] `layoutModesDiffer` に呼び出し元が 1 つも無いのに、現役の再評価経路として説明している

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1238-1239`

**問題点**: `+` 行「更新前後の visible projection で `supplementaryModes` が異なるかを判定する。異なる場合は Section 部分更新・Accessory 更新でも layout mode の再評価が要る。」

`layoutModesDiffer` の出現は `ios/` 全体で定義行 (`:1240`) の **1 箇所のみ**。テストからも呼ばれていない。`-` 行は「〜Requirement で要求される」という spec 参照だったため「要求はあるが実装状況は別」と読めたが、現在形の断定に変換したことで死んだヘルパを現役経路と誤認させる。

**推奨修正**: 現状の呼び出し状況を反映した説明にする (未使用であること自体は本 change の守備範囲外なので、記述を「判定するヘルパ」に留めるのが安全)。

---

### [🟡 Minor] 「Header / Footer 上下システム padding ゼロ化」は `headerTopPadding = 0` のみ

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:374`

**問題点**: `+` 行「「Header / Footer 上下システム padding ゼロ化」を適用した `UICollectionLayoutListConfiguration`」に対し、実際に設定しているのは `listConfig.headerTopPadding = 0` (`:388`) のみ。Footer 側・下端側は該当 API 自体がなく触れていない。

あわせて、同一 doc ブロックの未変更行 `:370` 「`style` から `UICollectionViewLayout` を生成する」と `:372-373` の `rootHeader` / `rootFooter` の記述は、この `static` ヘルパ (引数は appearance と sections、戻り値は listConfig) には当てはまらず `makeLayout(for:)` の説明が紛れ込んでいる。`+` 行がその誤ったブロックを温存している。

**推奨修正**: 「Header 上部のシステム padding をゼロ化した」に限定する。

---

### [🟡 Minor] supplementary は `UIListContentConfiguration` を使っていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:533-534`

**問題点**: `+` 行を含む記述「supplementary view（`UICollectionViewListCell` + `UIListContentConfiguration`）の上下マージン 2pt とラベル intrinsic 高さで表現される」。

supplementary のテキスト描画は `applyAccessoryToListCell` が `listCell.contentConfiguration = nil` にしたうえで UILabel + AutoLayout で行う (`:1804-1812`, `:1836-`)。`UIListContentConfiguration` は使っておらず、既定フォントを真似ているだけ (`:1855` の未変更コメントが「…相当（footnote）に揃え」と明言)。

**推奨修正**: `UIListContentConfiguration` の言及を落とし、UILabel ベースである旨に直す。

---

### [🟡 Minor] iOS ソースへの行番号参照がずれている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:623`

**問題点**: `+` 行「（原典 `ButtonCellRenderer.cs:97` / iOS `ButtonCellView.swift:61` と同型）」

`ButtonCellView.swift:61` は `prepareForReuse()` 内の `self.tapHandler = nil` で、`titleAlignment` とは無関係。該当する `textAlignment` 設定は `:56-57`。

規約がリポジトリ内識別子の参照を無条件に許すのは「grep で到達でき、消えれば同一コミット内で壊れに気づける」ためだが、**行番号はこの前提を満たさない** — オーナーが `SampleLabelCell` / `DatePickerAndroidStyle` を修正対象に引き上げたときと同じ理屈が当てはまる。`ButtonCellRenderer.cs:97` も原典非同梱で検証不能。

**推奨修正**: 行番号を落とし `ButtonCellView` / `ButtonCellRenderer` の型名参照に留める。

---

### [🟡 Minor] 兄弟テストファイル間で検証スコープの列挙が非対称になった

**該当箇所**: `ios/Tests/KsSettingsViewCoreTests/RootAccessoryTests.swift:4-5`

**問題点**: `+` 行「text / view 両ケースの構築とケース別取り出し、各ケースの等価性、および `SectionAccessory` とはコンパイル時に別型であることを検証する。」

同ファイルには `test_Hashable_Set_に格納できケース別に区別される()` (`:85`) があり、Hashable / Set の観点が列挙から抜けている。**対になる `SectionAccessoryTests.swift:4-6` は同じテストを「および Hashable として Set に格納できること」と明示的に列挙している** — 両ファイルとも今回書き換えられたのに、片方だけ落ちた。前回 Minor-5 (`KsSettingsViewRepresentableTests` のスコープ漏れ) と同じ型。

**推奨修正**: `SectionAccessoryTests.swift` の書き方に揃えて Hashable / Set を列挙に加える。

---

### [🔵 Suggestion] `DatePickerAndroidStyle` と同一クラスの取りこぼし — `androidUiStyle` はどこにも存在しない

**該当箇所**: `ios/Sources/KsSettingsViewUI/DatePickerCell.swift:15`

**問題点**: 未変更行「備考: iOS には `androidUiStyle` / `androidButtonColor` 引数は **持たない**（Android 限定）。」

`androidButtonColor` は Android に実在する (`compose/InputCellDsl.kt:289`) が、**`androidUiStyle` はリポジトリ内のどこにも存在しない** — 出現はこの 1 行のみで、Android 側の実名は `uiStyle`。

この change は兄弟ファイル `DatePickerUIStyle.swift` で**まったく同じクラスの誤り** (`DatePickerAndroidStyle` — 実名は `DatePickerUIStyle`) をオーナー判断で修正している。対称性チェックの取りこぼし。

**推奨修正**: `uiStyle` に直すか、この change に含めないなら残債務に加える。

---

### [🔵 Suggestion] `SampleTheme` の対で規約の出典が食い違う

**該当箇所**: `samples/ios/KsSettingsViewSample/SampleTheme.swift:44-46` ↔ `samples/android/.../SampleTheme.kt:58-59, :103`

**問題点**: 同一趣旨の「共有アクセントパレット」の根拠ブロックで、iOS 側だけが `cross/ADR-0016` 参照に書き換えられ、Android 側は `cross/conventions/sample-parity.md「各 Cell に渡すパラメータを一致させる」` のまま。どちらも事実としては正しい (規範文は `kasane/concepts/cross/conventions/sample-parity.md:23` に実在) が、対のブロックが別々の出典を指す状態になった。加えて iOS だけが dark mode トレードオフの一文を持つ。

**推奨修正**: どちらかに揃える。

---

### [🔵 Suggestion] `cellHeight` は Cell のフィールドではなく `CellStyle` のフィールド

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:146`

**問題点**: 「これにより Cell の `cellHeight` がレイアウト結果の行高さへ反映される」。`cellHeight` は `CellStyle.cellHeight` であり、実効値は `EffectiveStyle.effectiveCellHeight` (`EffectiveStyle.swift:218-228`) が `cellStyle.cellHeight → theme.rowHeight → minRowHeight` の順で解決する。バッククォート付きで Cell のフィールド名に読める。

**推奨修正**: 「Cell の `style.cellHeight`」等に直す。

---

### [🔵 Suggestion] `CellHandle.icon` のシグネチャが nullable を落としている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DSLIconModifiable.kt:8`

**問題点**: 「Compose DSL の `CellHandle.icon(_: KsImage)` modifier 経路」。実シグネチャは `fun CellHandle.icon(icon: KsImage?)` (`compose/DSLHandles.kt:118`、`CellModifiers.kt:57`) で nullable。加えて `_:` は Swift 記法で Kotlin ファイルには馴染まない (同種の Swift 記法混入は `ApplyDiffTest.kt:22` の `applyDiff(_:)` など複数ある)。

**推奨修正**: `CellHandle.icon(icon: KsImage?)` に直す。

---

## 自分で再実行した検証

| 項目 | 結果 |
|---|---|
| `python3 scripts/comment-policy-lint.py --summary` | **禁止 0 件** (検査対象 401 ファイル) |
| 領域別 lint (tasks.md の 9 領域すべて) | **全領域 禁止 0 件** — core 24 / compose 26 / ui-main 69 / ui-test 44 / iosCore 12 / iosSwiftUI 11 / iosUI 58 / iosTests 56 / samples 41 ファイル |
| `python3 scripts/comment-policy-lint.py --selftest` | **全件 OK** (検出ロジック + hook 疎通 5 項目) |
| `python3 scripts/comment-policy-lint.py --advisory` | 要確認 **2 件** — deviation.md 記載の誤検知 2 件と完全一致。誤検知判定は妥当 |
| lint / hook スクリプト自体の変更 | **なし** (`git diff --name-only -- scripts/ .claude/hooks/` が 0 件。禁止 0 は検査を緩めた結果ではない) |
| `comment-policy:allow` マーカー | リポジトリ全体で **0** |
| **機能コード差分** | **0** — Swift / Kotlin / kts は文字列リテラル・raw string (`"""` / `#"…"#`)・ネスト block comment を状態機械で処理、XML は `<!-- -->` を除去したうえで空白正規化比較。**224 ファイル全件一致 / 未検査 0** (md 1 件を除く) |
| **Android `./gradlew test --rerun-tasks`** | **1986 tests / 0 failures / 0 errors / 0 skipped / BUILD SUCCESSFUL** (test-results XML 142 件を集計)。※初回実行は `:ks-settingsview-ui:testReleaseUnitTest` が exit 134 で落ちたが、これはテスト失敗ではなく JVM クラッシュ。同一条件の再実行で成功しており review-002 と同じ環境側フレーク |
| **iOS `xcodebuild test`** (iPhone 17 Pro) | **624 passed / 0 failed / ** TEST SUCCEEDED **** — 4 バンドル (Bridge / Core / SwiftUI / UI) すべて `Test Suite 'All tests' passed` |
| 追加行の禁止パターン | 通番 (Phase / Round / Decision / 論点 / Major-N 等) **0**、アーカイブ文書パス **0**、`MUST` / `SHALL` / `SHOULD` / `MAY` **0**、`本提案` / `本変更` / `後続変更提案` **0**、履歴記述 **0** (検出 2 件はいずれも合意済み誤検知)。※ソース以外の 942 行中、`exploration.md` 由来のヒットは対象外として除外 |
| ADR 参照の実在性 | 追加行に出る **11 種すべて実在** (core/0005,0006,0008,0009,0010,0011,0013・android/0002,0005,0009・cross/0016)。前回の 10 種に `android/ADR-0009` が加わった (`DatePickerUIStyle.kt` の Major 修正による) |
| ADR 参照の主旨一致 | 全種の使用文脈を ADR タイトルと照合。**全件一致** |
| 追加行の識別子実在性 | バッククォート / 角括弧参照を機械抽出 (**259 種 / 延べ 638 出現**) しコーパス照合。**未ヒット 0**。※コーパスはリポジトリ全体のため「別モジュールにしか無い名前」は捕捉できず、それは人手・エージェント側で検出 |
| コメント中のファイルパス参照 | リポジトリ内 3 件すべて実在。AiForms 原典パス 3 件は原典非同梱のため未検証 |
| doc comment の構造 | 空 KDoc・孤立 doc 行・ファイル末尾破損 **0** (218 ファイル) |
| deviation 対象外の assertion 文字列 | `KsSettingsViewControllerTests.swift:440` の `"Phase 14.2 で …"` は未変更 (制約遵守) |
| 足場アーティファクト | 実装中の書き換えなし。`tasks.md` / `deviation.md` / `review-00N.md` は未追跡の新規、`exploration.md` の変更は探索フェーズ由来 |
| git 操作 | コミット・ステージいずれもなし (制約遵守) |
| tasks.md のチェック | G1〜G6 の `[x]` はいずれも該当領域の lint 0 件で裏付けあり。虚偽チェックなし |

**残債務の件数** (オーケストレーターからの依頼): 裸 change-id 参照を独立に数え直したところ **30 件 / 22 ファイル** — review-002 と完全一致で、**今回の修正で件数は動いていない**。追加行には 1 件も混入していないことも確認済み。あわせて `android/ks-settingsview-ui/build.gradle.kts:24, :59` の `development.md` 参照 (リポジトリ内に該当ファイルなし) を残債務クラスタに加えることを推奨する。

---

## カバー範囲

**全件確認したもの**: 224 ファイルの機械的検証 (機能コード差分・禁止パターン・ADR 実在性/主旨・識別子実在性・ファイルパス実在性・doc comment 構造・lint 領域別) は全数。

**人手で精読したもの**: 追加 `+` コメント行を 5 領域 (ios UI / ios Core+SwiftUI+Tests / android ui main / android core+compose+ui test / samples) に分割して全数精読し、**事実主張を約 450 件抽出して 1 件ずつ実コードで裏取り**した。報告した指摘はすべてレビュアー側で実コードを読んで再確認しており、裏取りできなかったものは採用していない。前回指摘 14 件の解消確認も全件レビュアー自身が実コードで検証した。

**加えて対称性を 3 軸で確認した**: プラットフォーム間 (同名ファイル 36 対のヘッダ記述を突き合わせ)、モジュール間 (`Theme` / `CellStyle` / `KsImage` の所属記述を core / ui / compose / swiftui の全記述で照合 — **全件一致**)、兄弟ファイル間 (Cell 13 種のフィールド列挙、Accessory テスト対、DSL modifier 対)。

**カバーしなかったもの**:
- AiForms 原典への言及 (約 12 件) — 原典リポジトリに `StylesAndColors` が無いことは確認できたが、`cellbaseview.axml` の 4dp、`divider.xml` の 1px、`SimpleCheck.cs` の `OnDraw` ロジック、`ButtonCell` の `private new` 隠蔽、各 `.cs:行番号` などは照合していない
- 実機・シミュレータでの視覚確認 (`SampleTheme` の「`UIColor.systemXxx` の light appearance 実測値」等) — 本 change の性質から不要と判断
- deviation.md で対象外と合意済みの 2 件 (assertion 文字列内の議論通番 / advisory 誤検知) は判定対象外

**ADR 参照の妥当性について 1 点補足** (指摘ではない): 入力系 Cell の「Native 型を直接公開する」に `core/ADR-0009` を添えている箇所が 4 件ある (`EntryCell.swift:8` / `PickerCell.swift:7` / `TimePickerCell.swift:7` / `PickerSelectionMode.swift:5`)。ADR-0009 の Decision は Theme / CellStyle / KsImage と色・フォントに限定されており、`Foundation.Date` / `UIKeyboardType` には触れていない (規範の実体は `kasane/concepts/core/cells/input-cells.md:29`)。ただし **exploration.md 論点2 の決定 α は「ADR 本文の読み込み・全件突合はしない。index.md のタイトルレベルで一致すれば可」と定めており**、タイトル「スタイルを UI 層に隔離し Native 型で表現」との照合としては合意済み手順どおり。**合意スコープに反する指摘はしない**という規律から、これは指摘に上げない。ADR 本文レベルの精度が要るなら、それは合意事項の変更としてオーナーが判断する事項。

---

## APPROVED にできるか

**できない。** Major 2 件がいずれも「読んだ人が誤った実装判断をする」種類の誤りであるため。

- `EntryCellView.swift:9-10` は、記述どおりに直すとコードが明示的に防いでいるバグ (`secureTextEntry` 時のレイアウト圧縮) が再発する。**同一ファイル内の防御コメントとの矛盾**であり、将来この矛盾に出会った人はどちらが正か判断できない
- `CheckboxCell.kt:12` は公開 API の KDoc で、利用者に「モデルは自動で書き戻る」と誤認させる。兄弟 `SimpleCheckCell.kt` と iOS 側の双方と矛盾する

### この change で直すべきもの / 別 change に回してよいもの

**この change で直すべき (すべて `+` 行 = この change が書いた行の、明確な事実誤認)**

Major 2 件と Minor のうち以下 8 件:
`ApplyDiffTest.kt:24-25` (Theme) / `ApplyDiffTest.kt:22` ほか「全 11 ケース」/ `Section.swift:32-34` + `KsSettingsViewController.swift:572` (header 空文字列) / `AndroidManifest.xml:5` / `BasicCellsDemoView.swift:10-11` / `SampleTheme.kt:21` / `BasicCellsTest.kt:1081` / `UnifyCellCommonFieldsTest.kt:19-20`

理由: いずれも「この change が書いた現在形の断定が事実と違う」もので、deviation.md が承認済みの「書き換え対象ブロック内の事実誤認の訂正」に真正面から該当する。放置すると **この change の目的 (コメントの信頼性回復) が達成されない** — 出典参照を消したぶん、残った記述の正しさに全面的に依存する状態になっているため。

**別 change に回して差し支えないもの**

残る Minor 6 件 (`SettingsAccessory.swift:19-20` / `layoutModesDiffer` / `padding ゼロ化` / `UIListContentConfiguration` / `ButtonCellView.swift:61` の行番号 / `RootAccessoryTests.swift:4-5`) と Suggestion 4 件。

理由: いずれも (a) 誤りの範囲が限定的で読者が実装を誤る度合いが低い、(b) 記述の一部が未変更行にまたがる、(c) 実装側の実態 (`layoutModesDiffer` が未使用であること等) にまで踏み込まないと最終形が決まらない、のいずれかに該当する。既に確定している「裸 change-id 30 件 / 22 ファイル + 未変更行の矛盾クラスタ」の別 change に合流させるのが自然。

### サイクル継続についての所見

これが上限サイクルであることを踏まえた率直な評価を書く。

前 2 回は「未カバー領域から新規指摘が出る」構図だったが、**今回の新規指摘はほぼすべて review-002 が精読済みと報告した領域から出ている** (詳細は冒頭「新規指摘の出所」)。加えて 1 件は前回指摘の修正そのものが持ち込んだ回帰。これは領域カバレッジの問題ではなく、**約 814 行の書き換え済みコメントに対する事実誤認の残存密度が、1 サイクルのレビュー検出能力を上回っている**ことを示している。同じ方法で 4 回目を回しても新規指摘は出ると考えるのが妥当。

そのうえで、今回見つかった誤りには **明確な発生パターン**がある。全 21 件のうち 14 件が「**アーカイブ済み spec / 履歴記述の引用文を、削除せずに現在形の断定へ変換した**」ものだった (`EntryCellView` / `AndroidManifest` / `BasicCellsDemoView` / `BasicCellsTest` / `layoutModesDiffer` / `padding ゼロ化` ほか)。規約の書き換え類型 1 は「参照句を**削除**し、残る説明が自然に読めるよう整形する」、類型 3 は「現在の仕様の説明に書き換えるか、**情報価値がなければ削除する**」と定めており、**引用本文まで現在形に昇格させるのは規約の想定外**。

したがって、次に打つ手として全面的な 4 回目のレビューより費用対効果が高いのは、**このパターンに絞った機械的な絞り込み**だと考える (アクションプラン 5)。

---

## アクションプラン

1. **Major 2 件を修正** — `EntryCellView.swift:9-10` (`fieldWrapper` / Hugging 100 / CCR required に直す。同ファイル `:53-62` の既存コメントが正)、`CheckboxCell.kt:12` (書き戻しの断言を削り `SimpleCheckCell.kt:14-15` と iOS 側に揃える)
2. **「この change で直すべき」Minor 8 件を修正** — 上記「APPROVED にできるか」節の一覧
3. **残る Minor 6 件 + Suggestion 4 件は、確定済みの残債務 change に合流** — 起票時に本ファイルの該当節を引く
4. **修正後に lint + 両プラットフォームのテストを再実行**し、機能コード差分 0 を再確認する
5. **[推奨] 次サイクルの前に、機械的な絞り込みを 1 回入れる** — `git diff` の `-` 行が `仕様:` `仕様（拡張）:` `設計:` `参照:` `旧仕様:` 等の**引用ヘッダを持つブロック**を全抽出し、対応する `+` 行が (a) 引用本文を現在形へ昇格させていないか、(b) 昇格させているなら実コードで裏が取れるか、を 1 件ずつ確認する。今回の 21 件中 14 件がこのパターンで、全面精読より狭い範囲で同じ密度の誤りに到達できる
6. **[オーナー判断] 4 回目のレビューを回すか** — 上記 5 を実施したうえでなら意味がある。実施せずに同じ方法で回す場合、新規指摘がまた出ることを織り込んだうえで「どこで打ち切るか」を先に決めておくことを推奨する
