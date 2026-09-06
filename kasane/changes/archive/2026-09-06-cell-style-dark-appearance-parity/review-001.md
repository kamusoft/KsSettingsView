# レビュー結果: cell-style-dark-appearance-parity (001 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

Android の Cell 固有色 12 本の `Color? → Color = Color.Unspecified` への統一は、data class・DSL・projection・解決関数・ViewHolder・bridge 変換まで漏れなく通っており、`Color?` の宣言は `android/` 配下に 1 件も残っていない。消費点は `EffectiveStyle.kt` の `toArgbOrElse` 1 本へ集約され、**ミューテーション probe (`toArgbOrElse` を素の `toArgb()` へ差し替え) で Android 本体 25 件のテストが落ちること**を実測して回帰検出力を確認した (lessons code-review L-001)。3 platform のテストは再実行して全件成功 (Android 2850 / iOS 全バンドル / MAUI 520、失敗 0)。描画の非回帰も基準 12 枚と最終 12 枚を独立に画素比較して差分ゼロを確認した。Critical / Major はない。指摘は 3 件の Minor と 1 件の Suggestion で、いずれも回帰検出力の穴と蒸留への申し送りの補完であり、実装の正しさを疑うものではない。

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` (always) | 常時 | 適合。`scripts/comment-policy-lint.py --advisory` は禁止 0 件。要確認 164 件のうち本 change が触ったファイルは 4 件だが、`bridge/KsBridgeCellStyle.kt:12` と `KsBridgeCellStyle.swift:16` の `maui/ADR-0004` は切り出し済みの既存債務、`ui/EffectiveStyle.kt:453` と `ui/PickerSelectionSheet.kt:106,118` はいずれも `internal` 宣言に対する lint の可視性ヒューリスティックの誤検出。触った Cell ファイル 10 本の公開 doc からは ADR 参照が消えている |
| `kasane/handbook/cross/test-execution.md` | テスト実行・結果報告 | 適合。Android は `--rerun-tasks` で 2850 件、iOS は Simulator 実行 (`swift test` ではない)、MAUI は 520 件。3 platform とも件数を確認して報告している。新規テストの待機は `awaitConvergence` / `composeRule.waitForIdle` / `layoutIfNeeded` を使い、固定秒数の収束待ちは持ち込んでいない |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/` を触る変更の完了判定 | 適合。tasks 8.3 に一時設定ビルドの error 0 件が記録され、`git diff -- ios/Package.swift` の差分 0 件を再確認した |
| `kasane/handbook/maui/integration-host-verification.md` | `maui/` の facade に触れ end-to-end 疎通を確認するとき | 適合。MauiHost の既定シナリオに行を足し、「期待される表示」への追記 (`:116`) と timestamp 更新まで済んでいる |
| `kasane/lessons/code-review.md` L-001 | 回帰検出力が争点になったとき | 適用した (下記 Minor 1 / Minor 2 の根拠は静的読解ではなく probe の実測) |
| `kasane/decisions/core/0030-*.md` (accepted) | 未指定色と外観の契約 | 適合。Decision 5 の「既存の `Color?` の色フィールドも同じ形に揃える」と Alternatives の「1 つの data class に未指定の表現が 2 流儀混在する」の却下を、Cell 固有色まで実行した内容になっている |
| `kasane/concepts/core/styling/style-resolution.md` | 解決順の記述 | 解決順は変えていない。記述の追随は tasks の蒸留申し送りへ (本 change の Non-Goal) |

## 指摘事項

### [🟡 Minor] EntryCell の accent 消費点だけがミューテーションで落ちるテストを持たない

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:237`

**問題点**: proposal の Impact が本 change の主リスクとして挙げ、tasks 1.4 が消費点を列挙し、tasks 4.2 が「いずれも `Unspecified` が ARGB へ落ちないことを含む」と定めたにもかかわらず、`EntryCell.accentColor` の消費点だけがその担保から漏れている。

`ui/EffectiveStyle.kt:518` の `toArgbOrElse` を素の `toArgb()` へ差し替えた probe (差し替え後の shasum 一致で原状復帰を確認済み) では Android 本体 25 件が落ちた。落ちたテストが観測しているのは Switch / Checkbox / SimpleCheck / Radio の accent、Picker / Number / Time / Date の選択面、DatePicker の `androidButtonColor` とカレンダーの色ロールで、tasks 1.4 が挙げた 8 消費点のうち **EntryCell の accent (`textCursorDrawable` の tint と `editText.highlightColor`) を観測するテストは 1 件も無い**。この消費点だけは、`toArgbOrElse` を通し忘れた形に退行しても全テストが緑のまま通り、実機では caret と選択ハイライトが透明な黒になる。

**推奨修正**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt` に 1 件足す。`accentColor` を渡さない `EntryCell` を `Theme(cellAccentColor = <色>)` で bind し、`vh.editText.highlightColor` が Theme の accent の ARGB と等しく `Color.Unspecified.toArgb()` ではないことを assert する (placeholder 側の既存 3 段テスト `InputCellsTest.kt:294-417` と同じ流儀)。`highlightColor` は Robolectric で素直に読めるため、`textCursorDrawable` 経路 (SDK ガード + try/catch) より観測点として安定する。

### [🟡 Minor] Android の DSL / Store 経路で「行が作り直されていない」ことが観測されていない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLCellStyleUpdateTest.kt:44` / `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/StoreCellStyleReplaceTest.kt:73`

**問題点**: 対応する 2 つの Scenario の THEN は「差分は**内容更新**として発行され、行の title 色が新しい値になる」「行の title 色が新しい値になり、**Section / Cell の identity は維持される**」で、色が届くことと**行を作り直していないこと**の 2 つを求めている。実装されたテストは前者しか観測していない。

- `DSLCellStyleUpdateTest` は再 composition 後の行の実描画色だけを見る
- `StoreCellStyleReplaceTest` の identity 検査は `view.internalRoot().sections` の id 一覧で、これはモデル側の id であり、テスト自身が同じ id を渡しているので行 (ViewHolder) が作り直されたかどうかとは独立に通る

style だけの差し替えが remove + insert へ退行しても両テストは緑のまま通る。行が作り直されると EntryCell のフォーカスと IME の未確定文字列、スクロール位置、選択面の提示状態が失われるため、これは利用者に見える退行になり得る (lessons test L-001 の「観測しやすい代理値が緑でも保証にならない」に当たる)。iOS 側の対応テストは `ios/Tests/KsSettingsViewUITests/CellStyleAppearanceTests.swift` で `cellBefore === cv.cellForItem(...)` と行の同一性を見ており、Android だけが片側で空振りしている (lessons test L-002 の非対称パターン)。

**推奨修正**: 両テストで、切替の前後に `RecyclerView.getChildViewHolder(...)` で取った ViewHolder インスタンスが同一であることを assert する (`titleColorOfRow` が既に holder を引いているので、その参照を保持して比較するだけで足りる)。lessons impl L-005 のとおり、この推奨形が周辺機構と噛み合うか (Robolectric で ViewHolder が安定して再利用されるか) は採用前に実測で確かめること。

### [🟡 Minor] 蒸留への申し送りに ADR-0030 Decision 3 の MAUI 節の限定が入っていない

**該当箇所**: `tasks.md`「蒸留への申し送り」2 項目め

**問題点**: accepted な `kasane/decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md` の Decision 3 は、両外観の色を利用者が決めるときの手段として「MAUI は XAML の `AppThemeBinding` で色プロパティを書く」と書いている。本 change の spike はこれが **Cell 段では成立しない**ことを実測した (facade の Section / Cell が MAUI の element ツリー外で binding が再評価されない)。

申し送りは「Decision 3 に CellStyle / Cell 固有色への適用を一文追記」とだけ書いており、その一文を素直に足すと、Decision 3 の MAUI 節が Cell 段まで及ぶ読みになって accepted ADR に誤った手段が残る。ADR は長命層で、ここの誤りは後続の設計判断を誤らせる (lessons impl L-006 と同じ型の見落としが、コードのコメントではなく ADR 側で起きる)。

なお ADR の改訂は explore / agenda / propose と蒸留の権利であり、本レビューは指摘にとどめる (supersede / amends は起票しない)。

**推奨修正**: 申し送りに「Decision 3 の MAUI 節は SettingsView 単位の Theme プロパティに限る旨と、Cell 段の手段は `RequestedThemeChanged` 購読 + 再代入である旨を書き分ける (証跡は本 change の tasks 0.1)」を足す。

### [🔵 Suggestion] Android spec の Scenario が名指しする `replaceCell` は Android の実機構と食い違う

**該当箇所**: `specs/settings-view-android-ui/spec.md` の Scenario「DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く」の THEN

**問題点**: THEN は「差分は内容更新 (`replaceCell`) として発行され」と書くが、Android の Compose DSL は内容変化で `SettingsRootDiff.ReplaceCell` を**発行しない**設計で (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt:18-20` が明記)、内容更新は `compose/KsSettingsViewComposable.kt:130` の `store.replaceCells` が担う。`replaceCell` を発行するのは iOS の `DSLDiffCalculator` 側である。

spec は足場なので本 change では直さない (書き換えを指示しない)。実装は実質を満たしており、テストが行の実描画色を観測しているのはむしろ正しい選択。

**推奨修正**: 蒸留で `kasane/concepts/core/styling/style-resolution.md` (および `android/api/android-compose.md`) へ写すときに、Android の内容更新経路を `replaceCells` として実態どおりに書く。spec の語をそのまま長命層へ持ち込まない。

## 確認したが指摘に至らなかった観点

- **`Unspecified` が ARGB へ落ちる経路の残り**: `android/` の `.kt` に `Color?` の宣言は 0 件。素の `toArgb()` が残るのは `ui/EffectiveStyle.kt:116` / `:141` の 2 箇所のみで、いずれも `takeOrElse` チェーンを通った後の値。placeholder だけは `Unspecified` を意味のある値として扱う必要があるため `ui/EntryCellViewHolder.kt:198` が `takeIf { it.isSpecified }?.toArgb()` で `Int?` に落としており、`applyPlaceholderColor` の `null` = 同梱テーマ既定への復元という既存契約と噛み合っている (`toArgbOrElse` に揃えなかったのは正しい)
- **10 Cell の equals / hashCode**: 10 型すべての手書き `hashCode` が `?.hashCode() ?: 0` から直呼びへ追随し、`equals` は `==` 比較のまま非 null 化で正しく動く。`CellSpecificColorTest.kt` が 12 フィールドを表で回して「省略 = `Unspecified` 明示」「明示色が違えば非等価かつ hashCode も別」を固定しており、hashCode からのフィールド欠落も拾える
- **旧契約 (`null` = 未指定) を語る記述の残り** (lessons impl L-006): 触った 10 Cell ファイルの公開 doc はすべて `Color.Unspecified` を主語にする現在形へ書き換わり、`PickerSelectionSheet.kt:125-128` の internal doc も追随している。`bridge/KsBridgeColor.kt` の `null` は DTO 側の wire 表現の説明であり旧契約ではない。`samples/` に `null` を色引数へ渡す呼び出しは無い
- **bridge の wire 形式**: 10 Cell DTO と `KsBridgeCellStyle` / `KsBridgeTheme` の公開フィールドは `Int?` のまま。`KsBridgeColor.color` の戻り型が `Color?` → `Color` になったのは internal object の内部契約で、公開面は不変
- **視覚証跡の実在と提出コードとの対応** (lessons process L-003): `ui/verification/` の 24 枚と `evidence/` の 12 ファイルはすべて実在。before/after 12 組をステータスバー帯を除いて画素比較し差分ゼロを独立に再現した。MauiHost の 4 枚は画像を開いて目視し、light = 緑 / dark = マゼンタが `SettingsPage.xaml.cs:36-37` の定数と、行の文言が `SettingsPage.xaml:47-49` と一致することを確認した
- **付随修正 5 件の同梱条件**: (1) ADR 参照除去 4 Cell ファイル、(2) `KsBridgeTheme` / `KsBridgeCellStyle` の private `color()` ラッパ削除 (2 ファイル、公開面不変、既存の bridge 変換テストで担保)、(3) `AttachOrderRestoreTest` の非 null 化と未使用 import 削除 (型変更が直接の原因)、(4) `PickerCellItemProjection` の ADR 参照除去 (1.2 で触ったファイル)、(5) `KsCheckBoxView.swift` の作業識別子の裸参照除去 (comment-policy 違反の 1 行修正) — 5 件とも「本務で触るファイル内」「公開 API・スキーマ・ADR の決定に触れない」「局所的」「テストで担保」を満たす。スコープ膨張はない
- **MauiHost の購読の後始末**: `SettingsPage` は再訪問検証のため使い回される唯一のインスタンス。`Loaded` / `Unloaded` の購読・解除を `_themeSubscribedTo` の参照で対にしており、再訪問での二重購読とリークは起きない。`AppTheme.Unspecified` がライト扱いになるが、既定を追う検証ホストの用途では妥当
- **iOS の新規テストの観測点**: `CellStyleAppearanceTests` は期待値を literal で持ち実装定数と突き合わせない形になっており、恒真化を避けている。`test_CellStyleのdynamic色は…` の「ライブラリの dark 既定とは別の値である」という補助 assert は、テスト自身の前提 (選んだ色が既定と衝突していない) を守るガードとして妥当
- **既知の切り出し済み先送り問題**: MAUI の `AppThemeBinding` 非対応 (`maui-appthemebinding-coverage` へ合流済み)、MAUI iOS のダークで行背景がライトのまま残る現象 (HEAD でも再現、本 change 起因ではない)、bridge DTO の公開 doc に残る `maui/ADR-0004`、`KsBridgeTheme.swift` の「1 対 1」記述、Android Sample の installDebug の注意 — いずれも本 change の対象外として指摘していない。MAUI iOS の dark 証跡に実際にその症状が写っていることは確認した (tasks 8.4 の注記どおりで、記録と提出物が一致している)

## アクションプラン

1. **Minor 1** — `InputCellsTest` に EntryCell の accent (`highlightColor`) の 1 件を足す。本 change が明示した主リスクの最後の穴で、コストも 1 テスト
2. **Minor 2** — DSL / Store の 2 テストに ViewHolder 同一性の assert を足す (推奨形が Robolectric で成立するかは採用前に実測)
3. **Minor 3** — `tasks.md` の蒸留申し送りに ADR-0030 Decision 3 の MAUI 節の限定を追記する
4. **Suggestion** — 蒸留で concepts へ写すときに Android の内容更新経路を `replaceCells` と実態どおり書く (本 change では作業なし)

1〜3 はいずれも本サイクル内で閉じる小修正だが、Critical / Major ではないため判定は APPROVED とした。オーナーが「このまま蒸留へ進む」と判断する場合は、1 と 2 をフォローの簡易起票へ、3 を蒸留時の作業として引き継ぐこと。
