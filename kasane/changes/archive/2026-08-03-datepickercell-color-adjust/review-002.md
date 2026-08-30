# レビュー結果: datepickercell-color-adjust (002 回目)

**日付**: 2026-08-02
**判定**: APPROVED

## サマリー

review-001 の必須 2 件と、second-opinion-002 の突き合わせで採用された 3 件 (C1 / C2 / C3) はいずれも解消されている。
特に C1 (描画色を状態判定の入力にしている) は、`WeakHashMap<MaterialShapeDrawable, Role>` による
「ライブラリが与えた Drawable インスタンス 1 個につきロールを 1 回だけ確定する」構造への作り替えで、
指摘された自己参照の振動 (透明アクセントでの選択 → 通常への転落) を原理的に断ち切っており、
透明アクセント・カスタム style の回帰テストまで付いている。C2 も実 `MaterialDatePicker` を
`FragmentActivity` 上に立てる統合テスト 6 件が入り、`attach()` → `FragmentLifecycleCallbacks` →
pre-draw → window 背景という未検証だった実経路が通るようになった。

テストはホスト側報告どおり緑を再現できた (`:ks-settingsview-ui:testDebugUnitTest` 588 件 / failures 0 / errors 0。
うち新規 3 クラスは DatePickerColorizerTest 31・DatePickerDialogIntegrationTest 6・DatePickerMaterialContractTest 9)。
足場の逆流は無く、`tasks.md` の `[x]` はすべて実体を伴う。

残る指摘は Minor 2 件 (いずれも優先度: 低。コメント・証跡の正確性であって挙動の欠陥ではない) と
Suggestion 2 件のみで、Critical / Major は無い。**カレンダーセルの既知の限界はデルタスペックに照らして
受容可能**と判定した (根拠は後述)。

## 前回指摘の解消状況

| # | 出所 | 指摘 | 判定 |
|---|---|---|---|
| H1 | review-001 (Minor / 優先度: 高) | 型ガードのないハードキャスト | **解消**。`DatePickerColorizer.kt:253` / `:257` はいずれも `(view as? TextView)` に揃った。`applyDynamicRole:288` の `view as TextView` は `isCalendarItem` (`:372` で `view !is TextView` を弾く) が型を保証しており、TimePickerColorizer と同じ形 |
| H2 | review-001 (Minor / 優先度: 高) | 「毎フレーム再適用しない」の回帰テストが無い | **解消**。`DatePickerColorizerTest.kt:400-460` に 4 件 (静的部位の非再着色 / 入力欄の枠の非再設定 / 選択日テキストの非再調整 / 差し替え後は再調整される)。推奨した 3 件に「入力欄の枠」が加わっており、静的・動的の分離が両方向で固定された |
| H3 | review-001 (Minor) | 横向きで選択日テキストが折り返す (幅合わせが単一行前提) | **未対応 (許容)**。`fitTextSize` は `paint.measureText` のままで、`ui/verification/07-calendar-landscape.png` も「2026年6月1 / 日」の 2 行折り返しのまま。ただし重なり・クリップは無く Requirement「クリップされずに読める」は満たす。前回も非ブロッキング (任意) とした指摘であり、brief の照合結果に横向きの扱いが明記された分、記録としては前進している。再指摘はしない |
| H4 | review-001 (Minor) | ヘッダの重なり検証が手組みの合成ヘッダに依存 | **解消**。`DatePickerDialogIntegrationTest.kt:143-154` が実 `MaterialDatePicker` のヘッダで重なりを判定する。合成ヘッダのテストは「幅に収まらない長い日付」など入力値を作り込むケースに残っており、前回の推奨どおりの分担 |
| H5 | review-001 (Suggestion) | `TEXT_INPUT_DATE` 未参照 / KDoc の主張と実態のずれ | **解消 (別のずれが 1 つ残る → 指摘 1)**。`TEXT_INPUT_DATE` は `MaterialIds` から削除され、契約テストは `MaterialIds` 経由に統一。`MaterialIds` の 15 個すべてが契約テストで検証されている |
| H6 | review-001 (Suggestion) | 背景ロール (window 背景) の自動テストが無い | **解消**。`DatePickerDialogIntegrationTest.kt:117-125` が実ダイアログの `InsetDrawable(MaterialShapeDrawable)` を unwrap して `fillColor` を確認する |
| H7 | review-001 (Suggestion) | ADR-0006 / ADR-0008 の記述が旧ファイル名を指す | **対応不要 (蒸留時の宿題)**。長命層は正しく無変更のまま |
| C1 | second-opinion-002 (Major、採用) | 描画色を選択状態の判定に利用している | **解消**。指摘の 3 シナリオを個別に確認した (下記) |
| C2 | second-opinion-002 (Major、採用) | 実ダイアログ統合と内部構造契約が未検証 / tasks 4.3・4.4 が過大 | **解消**。統合テスト 6 件 + 契約テストの拡張 (月移動ボタン・`month_grid` と日付セル型・年選択 RecyclerView と年セル型・ヘッダ 2 TextView の同一親) で、tasks 4.3・4.4 のチェックが実体に見合う状態になった |
| C3 | second-opinion-002 (Minor、採用) | brief.md に実装寸法が混在 | **解消**。実測 px・28/100dp・120dp・比率はすべて新設の `impl-notes.md` へ移り、brief 側はデザイン判断・部位対応・照合結果と `../impl-notes.md` への参照に留まっている |

### C1 の解消内容 (相方指摘の 3 シナリオを個別に確認)

`resolveCalendarItemRole` (`DatePickerColorizer.kt:454-469`) は
`isEnabled` → 確定済みロールの再利用 (`calendarItemRoles`) → 新規判定、の順になった。

- **透明アクセントで選択 → 通常へ転落**: 解消。判定は Drawable 1 個につき 1 回で、
  自分が書いた `accentCsl` を読み直さない。`透明なアクセント色でも選択日/選択年は選択ロールを保つ` の 2 件が固定
- **通常日の可視 fill を選択と誤認 / 可視 stroke を今日と誤認**: 発生はするが振動しない形に限定され、
  既知の限界として KDoc (`:446-452`) / impl-notes / brief に明文化 + 回帰テスト 2 件。受容可否は後述
- **キャッシュの妥当性**: material 1.12.0 の `CalendarItemStyle.styleItem()` が呼ばれるたびに
  `MaterialShapeDrawable` を新規生成する前提に依存する。この前提が崩れるとロールが固着するが、
  `MonthAdapter` / `YearGridAdapter` の塗り戻しは必ず `styleItem()` を通るため成立している。
  キーは弱参照・値は enum 定数なので参照の残留も無い

## 既知の限界 (ホスト側カスタム calendar style) のスペック照合 — 受容可能と判定

**判定: デルタスペックの Requirement を損なわない。現状のまま進めてよい。**

限界の内容は「ホストが `materialCalendarDay` 等を上書きして通常項目にも可視の塗り・枠を与えた場合、
その項目が選択 / 今日と同じロールで描かれる」。照合結果:

1. **Requirement の GIVEN の外**。Scenario「テーマ色の反映」の GIVEN は
   「既定値と異なる `backgroundColor` / `cellAccentColor` / `cellTitleColor` を持つ Theme」であり、
   ホストが material の calendar style を上書きした構成は前提に含まれない
2. **THEN の文言も破らない**。THEN は「テーマ由来の色で表示され、それらの部位に**プラットフォーム既定配色が残らない**」。
   誤分類しても塗られるのは解決済みアクセント色であり、既定配色が残る方向の失敗ではない
3. **代替手段が無い**。material 1.12.0 は役割ごとの `CalendarItemStyle` を外へ公開しておらず、
   `isSelected` はレイアウト後に `AbsListView.setupChild()` に潰される。ADR-0008 が選んだ
   「表示後の内部 View 走査」方式の内側で取れる手掛かりは、ライブラリが与えた描画だけ
4. **失敗が観測可能な形で固定されている**。振動しないこと (`通常日に可視の塗りを持つカスタム style でも配色は振動しない`)、
   枠のケースは文字がアクセント上文字へ転ばないこと (`通常日に可視の枠を持つカスタム style は枠だけが強調色になる`)
   の 2 件がテストで固定されており、「壊れ方が定義されている」状態になっている

したがって deviation としての記録も不要と判断する (spec が保証していない条件下の挙動であり、
spec からの乖離ではない)。ただし記載の置き場所については指摘 3 を参照。

## 指摘事項

### [🟡 Minor / 優先度: 低] KDoc が宣言する「両者の取り決め」を契約テストが満たしていない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:541-542`
/ `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerMaterialContractTest.kt:134`

**問題点**: `MaterialIds` の KDoc は「**ここに無い ID を契約テストが直接参照しない**、が両者の取り決め」と
宣言しているが、契約テスト `:134` は `MaterialR.id.mtrl_picker_text_input_date` を直接参照している
(まさに「ここに無い ID」)。一方、契約テスト側 KDoc (`:33-37`) が述べる規律は
「`MaterialIds` に集約された ID は `MaterialIds` 経由で引く (集約されているのに検証されていない ID を作らない)」で、
こちらは実態と一致している (15 個すべてが検証済みであることを確認した)。

つまり不一致なのは Colorizer 側の書きぶりだけで、しかもこの書き方は成立しない
(`MaterialR.layout.*` / `MaterialR.dimen.*` も同様に直接参照する必要がある)。
review-001 の Suggestion「KDoc の主張と実態がずれている」と同型の再発であり、
次に読む人が「取り決め違反がある」と誤読する。

**推奨修正**: Colorizer 側の一文を契約テスト側の規律に合わせる。例:
「`MaterialIds` に集約した ID はすべて契約テストで検証する (走査が依存しない ID はここに置かない)」。

### [🟡 Minor / 優先度: 低] 「統合テストで確認した」と書かれた確認が、テストとして残っていない

**該当箇所**: `DatePickerColorizer.kt:441-442` (`resolveCalendarItemRole` の KDoc「View 状態を使わない理由」)
/ `kasane/changes/datepickercell-color-adjust/impl-notes.md:66-71`

**問題点**: どちらも「実ダイアログの統合テストで、選択日を含む全セルが `isSelected == false` になることを確認」
「3 か月分の `month_grid` すべてで 0 件」と、**統合テストによる確認**として書かれている。
しかし `DatePickerDialogIntegrationTest` に `isSelected` を見るアサーションは 1 件も無い (grep 0 件)。
実際には実装中の一時的な観測であって、成果物には残っていない。

これは新しい状態判定設計の**中核前提** (「View 状態は使えない」) を支える根拠であり、
material 側が将来 `setSelected` を保つように変わっても誰も気付けない。
`lessons/inbox/test-limitation-asserted-without-measurement` (未検証の断定を証跡に書くと検証範囲の穴が固定化する)
の裏返しの構図 — 実測はしたが、その実測が再現可能な形で残っていない。

なお挙動上のリスクは無い (実装は `isSelected` を読まないので、material が保つようになっても壊れない)。
そのため優先度は低い。

**推奨修正**: 統合テストに 1 行足して観測を固定するのが最も安い
(`assertTrue("日付セルに isSelected が残るようになった", shown.root.dayCells().none { it.isSelected })`)。
足さないのであれば、KDoc / impl-notes の文言を「実装中の実測で確認 (テストとしては残していない)」へ直し、
テスト済みであるかのような記述をやめる。

### [🔵 Suggestion] TODAY / NORMAL の分岐だけ実ダイアログ経路の検証が無い

**該当箇所**: `DatePickerDialogIntegrationTest.kt:157-176` (`実ダイアログの日付セルが選択日と通常日で塗り分けられる`)

**問題点**: 新しい `resolveCalendarItemRole` のうち、実ダイアログで確認されているのは
SELECTED (アクセント塗りがちょうど 1 件) と、それ以外のセルの**文字色**だけ。TODAY (枠) の分岐は
合成テストのみで、統合テストの表示月 (2026/06、`SELECTED_DATE` のコメントどおり「今日」を意図的に外している) には
「今日」が含まれない。

このため、material 既定の通常日スタイルが将来可視の stroke を持つようになった場合
(= 全通常日が TODAY と誤判定されてアクセント枠が付く) でも、このテストは緑のまま通る
(非アクセント塗りセルについて文字色しか見ていないため)。C2 で埋めた「実経路」の穴のうち、
ここだけが判定ロジックの分岐として残っている。

**推奨修正**: 現行テストに「通常日は可視の stroke を持たない」を 1 行追加するか、
別テストで「今日」を含む月を初期表示にしてアクセント枠が 1 件だけ付くことを確認する。

### [🔵 Suggestion] 既知の限界の記述は impl-notes 側へ一本化したい

**該当箇所**: `kasane/changes/datepickercell-color-adjust/ui/brief.md:76` (「実装で確定した部位の扱い」の 4 つ目)

**問題点**: デルタスペックは「ダイアログ内のどの部位・状態がどのロールに属するか…は `ui/brief.md` の
部位対応表と承認モックを**正とする**」と定めており、brief は spec が参照する規範側の文書。そこへ
実装側の限界の但し書きを置くと、形としては「規範文書を実装に合わせて緩めた」ように読める。

前述のとおり内容自体は Requirement を損なわないので、これは配置の問題に留まる。
また C3 の整理 (実装調査の記録は brief から impl-notes へ) とも方向が揃わない —
この 1 項目だけが「実装の限界」という impl-notes 的な内容で brief に残っている。

**推奨修正**: 本文は `impl-notes.md` の「既知の限界」(既に同内容がある) に一本化し、
brief 側は他の項目と同じく `../impl-notes.md` への参照に留める。
なお brief の他の 3 項目 (区切り線・helper/placeholder・無効日と今年未選択) は
「部位対応表のどの行をどう扱ったか」であり、brief に置くのが妥当。

## 確認して問題が無かった観点

- **ビルド / テスト**: `:ks-settingsview-ui:testDebugUnitTest` を自分で再実行し、588 件 / failures 0 / errors 0 / skipped 0 を確認
- **足場の凍結**: `proposal.md` / `specs/cell-types-input/spec.md` / `exploration.md` は無変更。
  変更されている足場は `tasks.md` (チェック付与のみ) と `ui/brief.md` (追記のみ、C3 の是正を含む)。
  `deviation.md` は無く、記録の無い spec 逸脱も見つからなかった
- **tasks.md の虚偽チェック**: 1.1〜5.2 の全項目について実体を確認 (スパイクは impl-notes §1、
  4.1〜4.4 は各テストクラス、5.1 は `ui/verification/` の 7 枚、5.2 は `@SuppressLint("PrivateResource")` が
  着色ヘルパ 2 クラスに限定されていること)
- **状態判定キャッシュの安全性**: `WeakHashMap` のキーは `MaterialShapeDrawable` (equals/hashCode は
  Object 既定なので同一性比較)、値は enum 定数。UI スレッド専用。塗り戻しごとに新インスタンスへ移るため
  ロールの固着も参照の残留も起きない
- **View リサイクル経路**: `MonthAdapter.getView` が月外セルを `setEnabled(false)` + `GONE` で
  `styleItem()` を通さず早期 return する経路でも、`isEnabled` 判定が先にあるため
  リサイクル元の塗り (自分が塗ったアクセント) が誤って再解釈されることはない
- **契約テストの網羅**: `MaterialIds` の 15 個すべてが `DatePickerMaterialContractTest` で
  存在・型を検証されている (直接数え上げて確認)。走査が前提とする親子関係
  (`month_grid` の直接の子 / 年選択 frame の直接の子 / ヘッダ 2 TextView の同一親) も含まれる
- **統合テストの graphics mode**: `DatePickerDialogIntegrationTest` が `@GraphicsMode(NATIVE)` を持たない点を
  計測で確認した (レビュー用の一時テストを立てて実行・削除済み)。LEGACY でも `fontMetricsInt` は
  文字サイズに比例した値を返す (32sp で top=-32 / bottom=9、NATIVE は top=-34 / bottom=9) ため
  縦方向の重なり判定は成立する。一方 `measureText` は文字数を返す (9 文字 → 9.0px、NATIVE は 204.0px) ので
  幅合わせの追加縮小は LEGACY では走らない。この分担は `DatePickerColorizerTest` 側が
  `@GraphicsMode(NATIVE)` を持つことで埋まっており、モード指定の使い分けは妥当
- **コメントの自己完結性**: 新規の KDoc (状態判定の設計理由・既知の限界・キャッシュの機序) はいずれも
  外部 ID への参照だけで済ませず、その場で機序が読める。impl-notes への参照も
  「実測値はあちら」という補足であって、コメント単体の理解を妨げていない
- **既存挙動の非破壊**: `TimePickerColors` → `PickerDialogColors` のリネームは型名のみ。追加された
  3 派生色は TimePicker 側の部位では使われず、既存アサーションの意味は変わらない
- **リソース解放**: pre-draw フックの解除 (`:207-212`)、`FragmentLifecycleCallbacks` の自己解除 (`:150-154`)、
  `styledViews` の弱参照集合化は前回から変更なし

## verify について

デルタスペックの Requirement / Scenario と実装の対応関係は前回 (verify-001.md、VALID) から変化していない。
本サイクルの変更は (a) カレンダーセルの状態判定の内部設計の作り替え、(b) テストの追加 (統合・契約・冪等性・状態判定)、
(c) 証跡文書の整理であり、Scenario ↔ 実装の割り当てが増減したものはない。
よって **verify-002.md は作成しない** (ksn-verify の再実行は不要)。

## アクションプラン

Critical / Major は無く、以下はいずれもマージを妨げない。蒸留前に片付ける想定。

1. (任意・推奨) 指摘 2: 統合テストに `isSelected` の観測を 1 行固定する、または KDoc / impl-notes の
   「統合テストで確認」という記述を実態に合わせる
2. (任意) 指摘 1: `MaterialIds` の KDoc の「取り決め」を契約テスト側の規律に合わせて書き直す
3. (任意) 指摘 3: 通常日に可視 stroke が無いことを統合テストに 1 行追加する
4. (任意) 指摘 4: 既知の限界の本文を impl-notes へ一本化し、brief からは参照に留める
5. (蒸留時) review-001 の Suggestion どおり、ADR-0006 の現行照合行のファイル名を
   `PickerDialogColors.kt` へ貼り直す
6. (対応しない) review-001 H3 (横向きの折り返し): Requirement を満たしており、
   brief の照合結果にも記録済み。別変更で扱うか、このままでよい
