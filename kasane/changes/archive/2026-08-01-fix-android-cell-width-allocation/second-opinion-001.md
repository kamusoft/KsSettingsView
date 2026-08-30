# セカンドオピニオン: fix-android-cell-width-allocation (001 回目 / spec-review)
**相方**: codex / **日付**: 2026-08-01 / **対象**: 提案一式 (proposal.md / specs/ / tasks.md / ui/brief.md / exploration.md)
---
総合判定: **CHANGES_REQUESTED**。Critical はありませんが、このまま実装すると既存 `ButtonCell` のレイアウト切替が壊れる可能性が高く、UI 承認ゲートと受け入れ条件も未完了です。

## 指摘事項

### Major — `ButtonCellViewHolder` が新しい View 階層に追随できない

**該当箇所**: [tasks.md:3](kasane/changes/fix-android-cell-width-allocation/tasks.md:3)、[ButtonCellViewHolder.kt:35](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:35)

**問題点**: 現行 `ButtonCellViewHolder` は `titleView` が root `ConstraintLayout` の直接の子である前提で、`ConstraintSet` を切り替えています。提案どおり `titleView` を水平 `LinearLayout` 内へ移すと、`buttonStyleSet.applyTo(views.root)` では title の制約を変更できず、aux なし ButtonCell の全幅配置・中央揃えが壊れます。tasks は `CellBaseLayout.kt` と `EntryCellViewHolder.kt` しか明示していません。

**推奨修正**: `ButtonCellViewHolder.kt` の追随を明示タスクに追加し、通常レイアウトと aux なしボタンスタイルの両方について幅・alignment を検証する Scenario／テストを追加してください。

### Major — 長い title で入力欄が実質 0 幅でも合格できる

**該当箇所**: [settings-view-android-ui/spec.md:27](kasane/changes/fix-android-cell-width-allocation/specs/settings-view-android-ui/spec.md:27)、[ADR-0002:27](kasane/decisions/android/0002-cell-row-width-allocation-linearlayout-weight.md:27)、[plan-a.html:34](kasane/changes/fix-android-cell-width-allocation/ui/mock/plan-a.html:34)

**問題点**: EntryCell の title を `wrap_content`、EditText を `0dp + weight=1` とすると、title が主行幅を消費した場合、入力欄は 0 幅になり得ます。現 Scenario の「残り幅の範囲に収まる」は 0 幅でも成立します。承認画像でも長 title ケースの入力領域はほぼ 1 文字分です。「入力欄が狭い」という元の不具合を、別条件で再現してもテストが通ります。

**推奨修正**: 長 title 時に、

- 入力欄の実用幅を予約して title を省略する
- 原典どおり入力欄の 0 幅を許容する

のどちらを契約とするか明記し、選択した方を測定可能な受け入れ条件にしてください。

### Major — Requirement の分岐と回帰リスクをテストタスクが覆っていない

**該当箇所**: [settings-view-android-ui/spec.md:9](kasane/changes/fix-android-cell-width-allocation/specs/settings-view-android-ui/spec.md:9)、[cell-types-basic/spec.md:18](kasane/changes/fix-android-cell-width-allocation/specs/cell-types-basic/spec.md:18)、[tasks.md:15](kasane/changes/fix-android-cell-width-allocation/tasks.md:15)

**問題点**: 次の契約に対応する明確な Scenario／自動テストタスクがありません。

- 行内 trailing がない場合に title が全幅を使う
- valueText 自体が行幅を超えた場合の末尾省略
- valueText・description・Cell 級アクセサリが同時にある Picker 系
- Switch／Picker の description がアクセサリと重ならない
- 入れ子化後の baseline と title＋description 縦中央
- ButtonCell の aux あり／なし切替

`4.3 全種を視覚確認` だけでは、どの寸法関係をもって合格とするか判定できません。

**推奨修正**: 各分岐に個別 Scenario を追加し、固定した親幅で `measuredWidth`、左右境界、baseline、縦中心を検証するタスクへ対応付けてください。

### Major — UI 承認ゲートの証跡が未完成

**該当箇所**: [ui/brief.md:25](kasane/changes/fix-android-cell-width-allocation/ui/brief.md:25)

**問題点**: `ui/mock/approved.png` は存在しますが、brief は依然として「承認後に記入」のままです。どの HTML が承認されたか、画像との対応、承認事実が記録されていないため、実装時の「見た目の正」が確定していません。

**推奨修正**: `mock/plan-a.html を採用 (approved.png)` と承認日を brief に記録し、長 title の極小入力欄も意図した承認内容か確認してから実装へ進んでください。

### Major — デルタスペックが Kasane の UI lint に反する

**該当箇所**: [cell-types-basic/spec.md:7](kasane/changes/fix-android-cell-width-allocation/specs/cell-types-basic/spec.md:7)、[settings-view-android-ui/spec.md:5](kasane/changes/fix-android-cell-width-allocation/specs/settings-view-android-ui/spec.md:5)

**問題点**: 「主行内」「trailing 側」「垂直センター」「コンテンツ幅」「末尾省略」など、レイアウト配置・見た目・具体的 control をデルタスペックへ直接記載しています。Kasane の規約では、配置関係の正は `ui/brief.md`／mock、デルタスペックは観察可能な挙動契約に限定されます。現在は spec と mock の両方が見た目の正になっています。

**推奨修正**: 幅配分・配置関係は brief／mock に集約し、デルタスペックはユーザーから観察可能な結果に絞ってください。実装方式の根拠は ADR、視覚照合は mock、測定テストは tasks からそれぞれ参照させます。

### Major — `domain: android` と変更対象の長命層が一致しない

**該当箇所**: [proposal.md:31](kasane/changes/fix-android-cell-width-allocation/proposal.md:31)、[exploration.md:34](kasane/changes/fix-android-cell-width-allocation/exploration.md:34)

**問題点**: 実装は Android ですが、`cell-types-basic` の共通契約を変更し、蒸留時には `concepts/core/styling/cell-row-layout.md` を訂正する計画です。Kasane の domain 規約では複数ドメインに及ぶ変更は `cross` です。`android` のままだと蒸留先の判断を誤る可能性があります。

**推奨修正**: `domain: cross` に変更するか、Android change と core 概念訂正を別 change に分けてください。

### Minor — 「両 platform」を単一 Scenario で検証できない

**該当箇所**: [cell-types-basic/spec.md:30](kasane/changes/fix-android-cell-width-allocation/specs/cell-types-basic/spec.md:30)、[tasks.md:11](kasane/changes/fix-android-cell-width-allocation/tasks.md:11)

**問題点**: GIVEN が「iOS または Android」なので、Android だけ検証しても Scenario を満たしたと解釈できます。一方、Requirement は両 platform を要求し、proposal は iOS を Non-Goal としています。

**推奨修正**: Android と iOS の Scenario を分離してください。iOS は「既存挙動の確認のみ」と明記すれば、Non-Goal とも整合します。

### Minor — exploration の ADR 状態が現状と不一致

**該当箇所**: [exploration.md:26](kasane/changes/fix-android-cell-width-allocation/exploration.md:26)、[ADR-0002:4](kasane/decisions/android/0002-cell-row-width-allocation-linearlayout-weight.md:4)

**問題点**: exploration は ADR を `proposed / オーナー確認待ち` としていますが、ADR 本体は `accepted` です。第三者には設計判断が確定済みか判別できません。

**推奨修正**: exploration の状態記述を accepted に更新するか、時点記録であることを明示してください。

### Minor — 症状・期待挙動の参照画像が実装前に保存されていない

**該当箇所**: [ui/brief.md:10](kasane/changes/fix-android-cell-width-allocation/ui/brief.md:10)、[tasks.md:23](kasane/changes/fix-android-cell-width-allocation/tasks.md:23)

**問題点**: before と原典期待の画像を実装後に補完する計画です。変更後では同一条件の before を取り直せず、視覚的な解消判定が弱くなります。

**推奨修正**: 実装着手前に現行ビルドの before を保存してください。原典画像を再取得できない場合は、その理由と代替となる原典コード／mock を brief に明記します。

### Suggestion — 測定条件をテストマトリクスとして固定する

**該当箇所**: [tasks.md:17](kasane/changes/fix-android-cell-width-allocation/tasks.md:17)

**問題点**: 「長文」「主行の大半」だけでは、テストデータや画面幅次第で結果が変わります。

**推奨修正**: 少なくとも通常幅／狭幅、font scale、RTL、icon・accessory の有無を固定した測定マトリクスにし、各ケースの境界関係を数式で判定してください。

静的レビューのみ実施しており、ビルド・テスト・ファイル変更は行っていません。



## 突き合わせ結果 (2026-08-01 / 突き合わせ相手: ksn-propose Step 8 自己レビュー)

| # | 指摘 | 採否 | 判定根拠と反映先 |
|---|---|---|---|
| 1 | Major: ButtonCellViewHolder が新階層に追随できない | **採用** | ButtonCellViewHolder.kt:36 で titleView root 直下前提の ConstraintSet 切替を実物確認。ホスト側の見逃し。→ tasks 2.4 / 3.3、proposal Impact に反映 |
| 2 | Major: 長 title で入力欄 0 幅でも合格できる | **採用** | 契約は原典同型 (0 幅許容) と明記し測定可能化。承認済み mock (長 title で極小入力欄) と ADR-0002 の原典同型方針に整合 → specs Scenario を「表示幅 = 主行幅 − title 幅 (下限 0)」へ |
| 3 | Major: 分岐と回帰リスクをテストが覆っていない | **採用 (一部)** | trailing なし / valueText 行幅超過の Scenario を追加、固定親幅 measure・整列検証・ButtonCell テストを tasks 3.1〜3.3 に明示。Picker 系複合・description 非重なりは既存契約 (前 change の Scenario) のため回帰確認 (4.4) に整理 |
| 4 | Major: UI 承認ゲート証跡未完成 | **解消済み** | レビュー時点と brief 更新のタイミング差。相方レビュー実行中にユーザー承認・approved.png 保存・brief 記録が完了していた |
| 5 | Major: デルタスペックが UI lint に反する | **降格** | 幅配分・2 系統配置は本 change の機能契約そのもので、測定テストの指標になる観察可能な挙動。UI lint が禁じる視覚パラメータ生値 (px/色/余白) は含まれない。アーカイブ済み前例 (fix-cell-accessory-vertical-fill の cell-types-basic spec) も同形式で承認済み。見た目の規範 (寸法・配色) は brief/mock 側に分離済み |
| 6 | Major: domain: android と長命層の不一致 | **採用** | cell-types-basic (core 契約) の MODIFIED と concepts/core/styling の蒸留時訂正を含むため、domain-axis 規約「複数該当は cross」に該当 → proposal を domain: cross へ変更 |
| 7 | Minor: 両 platform を単一 Scenario で検証できない | **採用** | Android (新規挙動) と iOS (既存挙動の確認のみ) に Scenario を分離。Non-Goals と整合 |
| 8 | Minor: exploration の ADR 状態不一致 | **採用** | exploration.md を accepted (2026-08-01 承認) に更新 |
| 9 | Minor: before 画像を実装前に保存すべき | **採用** | 実装後は同一条件の before を取り直せない。tasks 4.1 を「実装着手前」に変更し、原典期待の代替 (承認済み mock) の扱いも明記 |
| 10 | Suggestion: 測定マトリクス固定 (RTL / font scale 等) | **降格 (一部反映)** | 固定親幅での measuredWidth・左右境界検証は 3.1 に反映。RTL・font scale の全組み合わせマトリクスは本 fix のスコープ超過 (根拠となる実害シナリオの提示なし)。必要なら別 change |

採用 6 / 一部採用 1 / 降格 2 (うち1件は一部反映) / 解消済み 1 (計 10 指摘、未解決 0)。採用分はすべて提案一式へ反映済み。
