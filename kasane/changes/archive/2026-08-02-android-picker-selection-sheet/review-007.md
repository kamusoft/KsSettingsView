# レビュー結果: android-picker-selection-sheet (007 回目)

**日付**: 2026-08-02
**判定**: APPROVED

**スコープ**: 本レビューは **review-006 (APPROVED) 後の修正サイクル7 の差分に限定**する。確認対象は (a) ヘッダー文字サイズ導出の配線と固定定数の残骸、(b) 幅配分・タップ領域・ヘッダー総高の既存保証が新サイズでも成立するか (テストの実効性含む)、(c) 巻き込み変更の有無 の3点。過去レビューで解消済みの論点 (高さ制約の対称化・非確定 dismiss・上限挙動・アクセシビリティ・配色の配線等) は再確認していない。オーナー裁定済みの事項 — 「タイトル +1sp / 操作ラベル −1sp」という導出方針そのもの、ドラッグハンドル色の非対応、確定ラベルのコントラスト — は指摘対象外として扱う。

## サマリー

3つの確認事項のうち (a)(b) は問題なし。ヘッダーの3ラベルはいずれも `PickerSheetStyle` の派生プロパティ経由になり、固定 sp 定数はコード・テストのどこにも残っていない。既存の保証 (48dp タップ領域・ヘッダー総高・左右スロットの対称幅) はいずれも「文字サイズに依存しない下限」または「ラベルの実測値」に基づいており、サイズが Theme 由来になっても構造的に成立する。テストも 22sp 指定 (絶対値) と既定 Theme (相対値) の2本で導出を両側から固定しており、固定定数への退行を確実に検出できる。

(c) について、コード側の巻き込み変更はない (差分は指定2ファイルのみ) が、レビュー実施中の **17:50 に `ui/brief.md` と `tasks.md` が更新**され、tasks 4.1 / 4.2 が完了扱いになった。brief.md への視覚照合結果の記録自体は ui 規約どおりで足場凍結の違反ではない。ただし記録された「verification/ の各画像 (11枚) と照合し最終承認」のうち **9枚は本サイクルの文字サイズ変更 (17:44) と前サイクルの配色変更 (17:02) のどちらも反映していない**ため、証跡としての整合が取れていない (Minor)。コード修正は不要。

**検証した客観事実**:

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`*/build/test-results/*/TEST-*.xml` 92 ファイルを集計し **1196 tests / 0 failures / 0 errors / 0 skipped** を確認 (review-006 時点の 1192 から +4 = 今サイクルで追加された 2 テスト × Debug/Release)
- ビルド出力に `PickerSelectionSheet.kt` に対する Kotlin 警告 (`w:`) はゼロ = 未使用の private 定数・import は残っていない
- 作業ツリーの mtime 走査 (build/・.gradle/ を除外): review-006 出力 (17:22) 以降に更新されたのは `deviation.md` (17:43) / `PickerSelectionSheet.kt` (17:44) / `PickerSelectionSheetTest.kt` (17:45) / `ui/verification/single-select-sheet.png` (17:46) / `same/multi-select-sheet.png` (17:47) / `ui/brief.md` (17:50) / `tasks.md` (17:50) の7点
- 実機スクショ2枚を直接比較 (`multi-select-limit-reached.png` 16:17 と `multi-select-sheet.png` 17:47): ヘッダー3ラベルの文字サイズ・候補行の文字色・OK ラベルの文字色がいずれも目視で異なる

## 確認事項ごとの結果

### (a) 導出の配線と固定定数の残骸 — 問題なし

派生プロパティは `PickerSheetStyle` に置かれ (`PickerSelectionSheet.kt:58-64`)、基準値は `itemTextSizeSp` (= `effective.titleSizeSp`、`:94`) の1つだけ。消費点は3箇所で過不足なし:

| ラベル | 行 | 適用値 |
|---|---|---|
| 取消 | `:323` | `headerActionTextSizeSp` (item − 1) |
| タイトル | `:342` | `headerTitleTextSizeSp` (item + 1) |
| 確定 | `:352` | `headerActionTextSizeSp` (item − 1) |
| 候補行 | `:607` | `itemTextSizeSp` (基準そのもの) |

固定 sp 定数の残骸なし — review-003 / review-006 が言及していた `HEADER_TITLE_TEXT_SIZE_SP` / `HEADER_ACTION_TEXT_SIZE_SP` / `HEADER_CONFIRM_TEXT_SIZE_SP` はリポジトリ全体で過去のレビュー文書にしかヒットせず、companion object (`:830-862`) に残っているのは dp 系の寸法定数だけ。差分定数も `HEADER_TITLE_SIZE_DELTA_SP` / `HEADER_ACTION_SIZE_DELTA_SP` (`:68` / `:71`) として明示され、`+`/`-` の向きは各プロパティの KDoc (`:58` / `:62`) が単独で読める形で説明している。コンパイラ警告ゼロがこれを裏付ける。

deviation.md の記述「候補行と同じ実効タイトルサイズを基準に、タイトル = +1sp、キャンセル / OK = −1sp」と実装は完全に一致する。mock (`ui/mock/plan-b.html:46-50`) は title 16px / cancel 15px / done 14px、すなわち item(15px) 基準で +1 / ±0 / −1 であり、**取消ラベルだけ mock と導出規則がずれる** (mock は item と同値、実装は −1sp) が、これは deviation.md が「キャンセル / OK = −1sp」と明記したオーナー指示どおりであり、合意済み差分として扱う。

`PickerSheetStyle.from` の KDoc (`:82-83`) も導出の存在を説明済みで、実装と食い違わない。

### (b) 既存保証の成立とテストの実効性 — 成立する

3つの保証はいずれも文字サイズに依存しない形で書かれており、Theme 由来のサイズ変動に対して構造的に成立する:

1. **タップ領域 48dp** — `cancelSlot` / `confirmSlot` の `minimumHeight` (`:382` / `:398`) と `resolveSlotMinWidths` の下限 `minTouchTarget` (`:428-430`) が固定 48dp。文字が小さくなってもスロットは 48dp を割らず、`TouchDelegate` (`:501-509`) がスロット全域を委譲する。文字が大きくなればスロットがラベルに合わせて広がるだけで、下限を下回る経路はない。
2. **左右スロットの対称幅と操作ラベル非切り詰め** — `resolveSlotMinWidths` は `desiredWidthOf(cancelView)` / `desiredWidthOf(confirmView)` で **textSize 適用後の実 View を測る** (`:427-439`)。固定値を前提にした計算は残っていないため、サイズが変わっても配分ロジックはそのまま成立する。`buildHeader` の KDoc (`:305-306`) が主張する「スロットの最小幅は操作ラベルを実測して決めるため、文字サイズが Theme 由来で変わっても配分は成立する」は、コード上正しい。
3. **ヘッダー総高** — 総高は `max(スロット高 48dp, ラベル実高)` で決まる。既定 Theme (item 17sp → タイトル 18sp / 操作 16sp) では確定ラベル実高 ≒ 16sp 行 + 上下 6dp padding < 48dp のため、48dp が引き続き支配的。テスト `ヘッダーの総高は承認モック相当に収まる` (`PickerSelectionSheetTest.kt:377-396`、44〜52dp 範囲) が実際に通っており、実測でも裏が取れている。

テストの実効性:

- `ヘッダーの文字サイズは候補行のサイズから導出される` (`:821-836`) は `cellTitleFontSize = 22.0` を与えて **23sp / 21sp / 21sp を絶対値で** 検証する。固定定数 (16/15/14) に戻す退行では必ず落ちる。
- `既定 Theme でもヘッダーの文字サイズは候補行のサイズから導出される` (`:838-851`) は既定サイズで **相対関係 (±1sp)** を検証する。既定値が偶然一致するケースを塞いでおり、2本で「基準への追随」と「差分の向き」を両側から固定できている。
- 既存の幅配分・タップ領域・総高のテスト (`:308-356` / `:377-396` / `:411-480`) は既定 Theme のまま通過しており、今回の変更による退行はない。

なお、上記3つの保証を**非既定の文字サイズで**確認するテストは存在しない (下記 Suggestion 1)。保証自体はロジック上成立するため Major には当たらない。

### (c) 巻き込み変更 — コードはなし。足場側にレビュー中の更新あり

コード・テストの差分は指定2ファイルに閉じている。`PickerCellViewHolder.kt` / `SampleTheme.kt` / `SampleTheme.swift` / `decisions/android/0004` はいずれも本サイクルでは未更新 (mtime が review-006 以前)。デルタスペック・proposal・mock は提案記録コミット以降 未変更で、足場凍結は保たれている。

review-006 のアクションプラン1 (必須) は履行済み — `deviation.md` (17:43) に確定ラベル配色の乖離が2件目として追加され、「強調色の上に載せる文字色は `Theme.backgroundColor` で描画する」という契約と理由・実測コントラストが記録された。本サイクルのヘッダー文字サイズも3件目として記録済み。

一方、**レビュー実施中の 17:50 に `ui/brief.md` と `tasks.md` が更新された**。私が最初に mtime 走査を行った時点 (17:4x) には存在しなかった差分であり、本レビューはこの2ファイルの 17:50 時点の内容に基づく。

- `tasks.md`: 全項目が `[x]` になり、グループ4 (4.1 視覚照合 / 4.2 実機動作確認) も完了扱いになった。1〜3 の実装・テスト項目に虚偽チェックはない (対応するコードとテストの実在を確認済み)。
- `ui/brief.md`: 「視覚照合の結果」節が追記され、11枚の照合とオーナー最終承認、合意済み妥協5件が記録された。**これは `ksn-core/references/ui-artifacts.md` が brief.md に求めている記録そのもの**であり、足場凍結の対象 (proposal / design / specs) にも含まれないため、規約違反ではない。記録された妥協5件は deviation.md の4件と矛盾しない (brief 側は mock との視覚差、deviation 側は指示の経緯という役割分担)。

内容の整合について1点、下記 Minor を挙げる。

## 指摘事項

### [🟡 Minor] 「11枚を照合して最終承認」の記録に対し、9枚が本サイクル・前サイクルの視覚変更を反映していない

**該当箇所**: `kasane/changes/android-picker-selection-sheet/ui/brief.md:52-62` (追記された「視覚照合の結果」) / `kasane/changes/android-picker-selection-sheet/tasks.md` 4.1 / 証跡の実体は `ui/verification/` の9枚 (撮影 16:17〜16:27)

**問題点**: brief.md は「verification/ の各画像 (11枚…) と approved.png を照合し 2026-08-02 オーナー最終承認」と記録し、tasks 4.1 も完了になった。しかし 11枚のうち現行実装を写しているのは `single-select-sheet.png` (17:46) と `multi-select-sheet.png` (17:47) の2枚だけで、残る9枚は 16:17〜16:27 撮影 = **配色変更 (17:02) と本サイクルのヘッダー文字サイズ変更 (17:44) のどちらよりも前**の状態を写している。

実際に2枚を並べて確認した差は目視で明らかである:

| | `multi-select-limit-reached.png` (16:17) | `multi-select-sheet.png` (17:47) |
|---|---|---|
| ヘッダー3ラベル | 小さい (旧 16/15/14sp 固定) | 明確に大きい (18/16/16sp 相当) |
| 候補行の文字色 | 金 (旧 `#CC9900`) | 濃灰 (`#555555`) |
| OK ラベルの文字色 | 黒 | 淡色 (`#F2EFE6`) |

とくに `multi-select-limit-reached.png` / `multi-select-limit-tap-rejected.png` は tasks 4.1 が名指しする「上限」状態の証跡であり、landscape 系4枚もヘッダーを含む。brief.md の補足は配色 (`cellTitleColor` を `#555555` に合わせた状態が最終承認版) には触れているが、**ヘッダー文字サイズについては触れていない**ため、後から見た人がどの画像のヘッダーが正なのか判断できない。証跡は足場と共にアーカイブされ、蒸留後は「何が承認されたか」の唯一の記録になるため、ここで整合させておく価値がある。

これは review-006 の Suggestion 1 (配色のみを対象としたもの) が未解消のまま、tasks 4.1 の完了チェックとオーナー最終承認の記録によって「解決済み」の見た目に固まった状態であり、重要度を Minor へ上げて再掲する。**コード・テストの修正は不要**。

**推奨修正**: どちらかで足りる。
1. ヘッダーまたは OK ボタンが写る証跡 (上限2枚・landscape 系) を現行ビルドで撮り直す、または
2. brief.md の補足に「配色とヘッダー文字サイズの正は `single-select-sheet.png` / `multi-select-sheet.png` (17:46-17:47 撮影) の2枚。他の9枚は挙動 (高さ制約・内部スクロール・上限拒否) の証跡であり、撮影時点は配色・文字サイズ変更前」と1〜2行で明記する。

### [🔵 Suggestion] 非既定の文字サイズで幅配分・タップ領域・総高を固定するテストがない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:308-356` / `:377-396` / `:411-480`

**問題点**: ヘッダーサイズが Theme 連動になったことが本サイクルの主眼だが、レイアウト系の既存テストはすべて既定 Theme で実行される。大きな文字サイズを想定した `対称幅が収まらない狭幅では左右を固有幅へ縮退させ操作ラベルを画面内に収める` (`:447`) も、コメントが明言するとおり**文字列長で代用**しており (`:456-457`)、実際に `cellTitleFontSize` を上げた経路は通らない。`buildHeader` の KDoc (`:305-306`) は「文字サイズが Theme 由来で変わっても配分は成立する」と明示的に主張しているので、その主張を固定するテストがあると強い。

**問題の実害は低い** — `resolveSlotMinWidths` はラベルを実測するため、固定値由来の退行は構造上起こりにくい。

**推奨修正**: 必須ではない。追加するなら `cellTitleFontSize = 30.0` 程度で1本、(1) 両スロット幅 ≧ 各ラベルの希望幅、(2) スロット高 ≧ 48dp、(3) ヘッダー総高 ≧ 48dp かつ確定ラベルがヘッダー内に収まる、を確認するだけで KDoc の主張を証跡化できる。

### [🔵 Suggestion] 極小フォント指定時に操作ラベルのサイズが 0 以下へ落ちうる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:62-64`

**問題点**: `headerActionTextSizeSp` は減算のみで下限を持たない。`Theme.cellTitleFontSize` は `> 0` のときだけ採用される (`EffectiveStyle.kt:284`) ため 0 や負値は入らないが、`0 < 値 ≦ 1` (例: `cellTitleFontSize = 0.5`) を指定すると操作ラベルのサイズが 0 以下になる。`Paint.setTextSize` は負値を無視するため、クラッシュはせず「取消 / OK だけ TextView 既定サイズのまま」という不可解な表示になる。

**前提条件がすでに実用外** (候補行のタイトルが 0.5sp = 判読不能) であり、実害はほぼない。既定 (`DEFAULT_TITLE_SIZE_SP = 17.0f`) や現実的な Theme 指定では発生しない。

**推奨修正**: 必須ではない。潰すなら `get() = (itemTextSizeSp - HEADER_ACTION_SIZE_DELTA_SP).coerceAtLeast(1f)` の1行で足りる。

### [🔵 Suggestion] `buildHeader` の KDoc「スロット高 48dp をそのままヘッダー高とする」が条件付きになった

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:296-298`

**問題点**: この記述は固定 14〜16sp を前提に書かれたもので、当時はラベル実高が常に 48dp 未満だったため無条件に成立していた。サイズが Theme 連動になった今は、item が 30sp を超えるあたりでラベル実高 (行高 + 上下 6dp padding) が 48dp を上回り、ヘッダーはスロット高ではなくラベル高で決まる。挙動としては正しい (大きな文字には広いヘッダーが要る) が、KDoc は無条件の断定のままである。

**推奨修正**: 必須ではない。「スロット高 (48dp) が下限となり、通常はこれがそのままヘッダー高になる」程度に条件を1語足せば、同ファイル内の他の高さ説明 (`:305-306` の「文字サイズが変わっても成立する」) と整合する。

## 裁定済みにつき指摘しない事項の客観記録

修正要求ではなく、蒸留時の判断材料として残す。

- 確定ラベルのコントラストは review-006 が記録した 2.25:1 (サンプル Theme) / 4.02:1 (ライブラリ既定 Theme) から変わらない (色は不変)。ただし**サイズの前提は変わった** — review-006 は「14sp Bold = WCAG large text 相当 (基準 3:1)」として評価したが、現行の確定ラベルは `itemTextSizeSp − 1`。既定 Theme では 16sp Bold で引き続き large text 相当だが、利用者が `cellTitleFontSize` を 15 未満に設定すると 14sp Bold を割り、適用基準が 4.5:1 へ上がる。配色方針はオーナー裁定済みのため指摘としては挙げない
- 取消ラベルのサイズが mock (item と同値) ではなく item − 1sp である点は deviation.md 記載どおりのオーナー指示

## アクションプラン

1. **(必須・蒸留前)** brief.md の視覚照合記録と `ui/verification/` の整合を取る (Minor)。撮り直しか、どの画像が配色・文字サイズの正なのかを brief.md に1〜2行で明記するかのいずれか。コード変更不要のため実装レビューの再周回は不要
2. **(蒸留時)** review-006 から申し送りの検討事項を引き継ぐ — 「強調色の上に載せる文字色は `Theme.backgroundColor`」および今回の「ヘッダー文字サイズは実効タイトルサイズから ±1sp で導出」を、`concepts/core/styling/` の公開契約とするか android/ADR-0005 の追補とするか。どちらも `Theme` の値が選択面の見た目を規定する非自明な結合であり、利用者向けの契約として拾う価値がある
3. **(任意)** Suggestion 3件 — 非既定サイズのレイアウトテスト追加 / 極小フォントの下限 / KDoc の条件付け
4. **(蒸留時・継続)** review-004 / review-005 から申し送りの ADR-0005 の Suggestion 2件 (追補と本文の矛盾・「Material の標準挙動」の文言) は本サイクルでも未解消
