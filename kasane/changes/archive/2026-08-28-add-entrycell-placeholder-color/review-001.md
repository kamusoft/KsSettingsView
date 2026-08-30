# レビュー結果: add-entrycell-placeholder-color (001 回目)

**日付**: 2026-08-27
**判定**: CHANGES_REQUESTED

## サマリー

9 capability のデルタスペックに対し、iOS / Android core・bridge 両 platform・MAUI facade・サンプル 3 platform・テスト 3 系統がいずれも過不足なく実装されており、全 Requirement / Scenario に対応するテストが存在する。ビルドとテストは 3 platform とも全件成功 (iOS 606 tests / 0 failures、Android 2698 tests / 0 failures、MAUI 472 tests / 0 failures)。placeholder 色の解決順・attributed 切替・`ColorStateList` 復元・輸送経路のいずれも規約 (`kasane/concepts/core/styling/style-resolution.md`) に忠実で、Critical / Major の指摘は無い。

一方で、本 change が意図的に入れた**利用者可視の挙動変更**である「Android 入力文字色の valueText 是正」に視覚証跡が 1 枚も無く、`ui/brief.md` の照合結果にも証跡範囲外である旨の明記が無い (lessons/process.md L-003 (3)(4))。加えて spec が SHALL で要求する差分判定 (再適用抑止) に対応テストが無く、Android の `EffectiveStyle` に読み手のいないフィールドが 1 つ増えている。いずれも数行〜1 テストで閉じる修正のため、同一サイクル内での対応を求める (lessons/process.md L-005)。

## 指摘事項

### [🟡 Minor / 優先度: 高] Android 入力文字色の valueText 是正に視覚証跡が無い

**該当箇所**: `kasane/changes/add-entrycell-placeholder-color/ui/verification/` (該当画像なし) / `kasane/changes/add-entrycell-placeholder-color/ui/brief.md` の「照合結果」 / 実装は `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:217`

**問題点**:
`specs/settings-view-android-ui/spec.md` の Requirement「入力文字色の valueText 解決 (規約乖離の是正)」は、proposal の Impact にも「**意図した挙動変更** — Theme / CellStyle で valueText 色を明示指定している利用者の Android 入力中テキスト色が変わる」と明記された、色という利用者の目に見える変更である。しかし提出された 7 枚の証跡はすべて placeholder 色の確認であり、この是正が効く経路 (`Theme.cellValueTextColor` を明示した構成) を写した画像は 1 枚も無い。サンプル側も `cellValueTextColor` を設定していない (`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt` に valueText 系の指定なし) ため、既存の Android スクリーンショットにも変化は現れない。

`ui/brief.md` の「照合結果」は 7 枚の対応を列挙しているが、この Requirement が証跡範囲外であることには触れておらず、「合意済み妥協: 0 件」で閉じている。lessons/process.md L-003 は (3) 視覚へ影響する修正には影響スクショの再撮影**または証跡範囲の明記**を、(4) レビューは証跡の実在と提出コードとの対応を判定条件にすることを求めている。担保は `InputCellsTest` の 3 テスト (`currentTextColor` の観測) のみで、画面での確認が無い。

**推奨修正**: 次のいずれか。
- (A) 一時的に `Theme.cellValueTextColor` を title 色と別色にした構成で Android サンプルを撮影し、入力済みテキストが valueText 色で描画されることを示す 1 枚を `ui/verification/` へ追加する (A/B にするなら是正前後 2 枚)。撮影用の一時変更は成果物へ残さない。
- (B) 撮影が過剰と判断するなら、`ui/brief.md` の「照合結果」に「Android 入力文字色の valueText 是正は、サンプルが valueText 色を指定しないため視覚証跡の対象外。担保は `InputCellsTest` の 3 テスト」と証跡範囲を明記する。

### [🟡 Minor] 「変化の無い placeholder 色を再適用しない」(SHALL) に対応テストが無い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:319-328` (`applyPlaceholderColor` の差分判定)

**問題点**:
`specs/settings-view-android-ui/spec.md` は「同一 Cell への再バインドで変化の無い placeholder 色を再適用しない (SHALL — フォーカス中の再バインドが多い `EntryCell` の既存の差分判定の作法に従う)」を要求している。実装は `appliedPlaceholderColor` / `placeholderColorApplied` の 2 変数で差分判定しているが、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt` の新規 7 テストはいずれも「適用後の色」だけを見ており、再適用が抑止されていることを検証していない。差分判定を丸ごと削っても (毎回 `setHintTextColor` を呼んでも) 全テストが緑のままになる。同 Requirement の他の SHALL (復元・無効状態・解決順) にはすべてテストがあるだけに、ここだけ穴になっている。

**推奨修正**:
`TextView.setHintTextColor(Int)` は呼ぶたびに `ColorStateList.valueOf(color)` で新しいインスタンスを作るため、インスタンス同一性で再適用の有無を観測できる。同一 id・同一 placeholderColor の `EntryCell` を 2 回 bind し、1 回目直後の `vh.editText.hintTextColors` と 2 回目直後の値が `assertSame` であることを確認するテストを 1 件追加する (`InputCellsTest` の placeholder 色ブロック)。差分判定を外すと落ちるため、検出力は担保できる。

### [🟡 Minor] Android `EffectiveStyle.placeholderColor` が誰にも読まれていない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:69` (フィールド宣言)、同 `:177` (算出)、同 `:204` (代入)、doc は同 `:43-45`

**問題点**:
追加された `EffectiveStyle.placeholderColor` インスタンスフィールドは、production・テストのどちらからも読まれていない。描画側の `EntryCellViewHolder` は Cell 固有値を含む必要があるため companion の `EffectiveStyle.effectivePlaceholderColor(entryPlaceholderColor, cellStyle, theme)` を直接呼んでおり (`EntryCellViewHolder.kt:179-184`)、テストも companion 関数を直接叩いている (`EffectiveStyleResolutionTest.kt:156-193`)。`EffectiveStyle` は `internal data class` であり、iOS の同名型と違って利用者定義 Renderer へ公開されていないため、外部の読み手も生まれ得ない。結果として、全 Cell 種別の bind ごとに 1 回 `toArgb()` を回し、`equals` / `hashCode` / `copy` の対象を 1 つ増やすだけのフィールドになっている。

(参考: iOS 側の `EffectiveStyle.placeholderColor` は `public struct` の一員で Renderer 拡張境界に露出しているため、production から読まれていなくても存置に理由がある。指摘は Android 側に限る。)

**推奨修正**: `EffectiveStyle` からフィールドを外し、解決は companion の `effectivePlaceholderColor` 2 種に一本化する (doc の該当 `@property` 行も削除)。あるいは `EntryCellViewHolder` 側で CellStyle → Theme 段を `effective.placeholderColor` から取り、Cell 固有値だけを手前で分岐させる形に寄せて読み手を作る。前者を推奨 (描画側の解決順が 1 箇所に閉じるため)。

### [🔵 Suggestion] ホスト既定 hint 色が取得できない場合に明示色から戻れない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:75` / 同 `:323-325`

**問題点**:
`hostHintTextColors` は生成時の `editText.hintTextColors` をそのまま保持し、復元は `hostHintTextColors?.let { editText.setHintTextColor(it) }` で行う。ホストテーマが `android:textColorHint` を持たず `null` になった場合、明示色 → 未指定の遷移でも復元が no-op になり、直前の明示色が行に残る (`appliedPlaceholderColor` は `null` に更新されるため、以降の差分判定でも再修正されない)。Material3 派生テーマ前提 (`style-resolution.md` の platform theme の前提) では実際には起きないが、無音で誤表示に倒れる形になっている。

**推奨修正**: 必須ではない。対処するなら `hostHintTextColors` が `null` のときは復元をスキップせず `placeholderColorApplied` を `false` のままにして次回 bind で再評価させるか、doc に「ホストテーマが hint 色を持つ前提」であることを 1 行書き添える。

### [🔵 Suggestion] ダーク面の証跡で未指定 placeholder がほぼ判読不能になっている

**該当箇所**: `kasane/changes/add-entrycell-placeholder-color/ui/verification/ios-entry-placeholder-dark.png`

**問題点**:
証跡のダーク面では、サンプルの `SampleTheme` が Cell 背景を固定色 (ライト寄り) に保つ一方、未指定行の placeholder だけが OS 既定としてダークモード側の色へ追従するため、「ニックネーム (callback)」の placeholder が背景にほぼ埋もれている。`ui/brief.md` の記述「未指定行だけが OS 既定としてダークへ追従し、指定色は不変」自体は画像と一致しており、また Non-Goal でライブラリ既定色の持ち込みを明確に排除している以上、本 change の欠陥ではない。ただしサンプルのダークモード時の見栄えとしては読み取りづらく、証跡としても「追従している」ことが伝わりにくい。

**推奨修正**: 本 change では対応不要。サンプルのダークモード時の Cell 背景の扱いは `sample-parity` の「dark mode 追随より一致を優先する」方針とも絡むため、気になるなら別途 ksn-explore の簡易起票が妥当 (本 change に同梱すると Sample の配色方針という設計判断に踏み込むため、付随修正の同梱条件 ② ⑤ を外れる)。

## 確認した観点 (指摘に至らなかったもの)

- **仕様充足**: 9 capability の全 Requirement / Scenario に実装と対応テストが存在。`specs/maui-cells` の SHALL NOT (CellStyle 段を持たない) は `CellShapeTests.PlaceholderColorIsExposedByEntryCellOnly` で `KsCellStyleSnapshot` に生えていないことまで検証済み。
- **足場凍結**: `tasks.md` はチェックボックスのみの変更で本文の書き換えなし。`proposal.md` / `specs/` は無改変。`ui/brief.md` の追記は照合結果・トークン候補という UI フローの所定欄のみ。
- **deviation.md の付随修正 2 件**: いずれも ksn-core の同梱条件に収まる (① 本務で触れたファイル内 ② 公開 API・スキーマ・ADR に触れない ③ 各 1 ファイル・コメントのみ ④ 既存テストの通過で担保 ⑤ 分岐なし)。記録外の無断逸脱は検出されなかった。
- **列挙漏れ**: iOS `EntryCell` の init 2 種 / `==` / `hash` / `withDSLID` / `withStyle` / `withIcon`、`CellStyle.==` / `hashCellStyle`、`Theme.==`、Android の手動 `equals` / `hashCode`、Compose DSL 2 overload、MAUI `AffectsSnapshot`、iOS `ApiDefinition.cs` の 2 型 — すべて反映済み。`EntryCell` を構築する箇所は他に無い (`ios/Sources/KsSettingsViewSwiftUI/` に構築点なし、Android は DSL と bridge のみ)。
- **再利用とライフサイクル**: iOS は `prepareForReuse` で `attributedPlaceholder` / `placeholder` の両方を落とし、Android は `reset()` (`KsSettingsListAdapter` の `onViewRecycled` 経由) でホスト既定へ戻して差分判定フラグも初期化している。色指定 → 未指定の同一 ViewHolder 上の遷移も両 platform でテスト済み。
- **視覚証跡と提出コードの対応** (lessons/process.md L-003 (4)): 7 枚すべてを実見し、行タイトル「表示名」・placeholder 文字列「placeholder 色の指定例」・橙色 (`#D6885A`) と Footer 文言が `samples/{ios,android,maui}` の差分と一致することを確認した。MAUI の 2 枚 (`maui-android-entry-placeholder.png` / `maui-ios-entry-placeholder.png`) は、単体テストが届かない Platforms 配下の Gateway と生成 binding を含む経路が実際に通っていることの証拠にもなっている。撮影データは架空の氏名・メール・電話番号のみで個人要素なし。
- **Sample パリティ**: 3 platform で行タイトル・placeholder 文言・Footer 追記・Section 内の位置・色の RGBA (`SampleTheme` に一元化) が一致。
- **コメント規約**: 新規コメントに禁止参照・履歴記述・spec キーワードの混入なし。`cross/ADR-0016` 参照は許容形式。`python3 scripts/comment-policy-lint.py --summary` は 681 ファイル / 禁止 0 件、`local-path-lint.py` / `identity-lint.py` も検出なし (lint 0 件は適合の証明ではないため本文からも判定した)。
- **テスト実行** (`kasane/concepts/cross/conventions/test-execution.md` 準拠): iOS は Simulator 全件 (`xcodebuild test -scheme KsSettingsView-Package`) で `Executed 606 tests, with 0 failures`、Android は `./gradlew test --rerun-tasks` で `BUILD SUCCESSFUL` かつ test-results XML 集計 2698 tests / 0 failures / 0 errors、MAUI は `dotnet test` で 472 合格 / 0 失敗・警告 0 件。
- **テストの検出力**: 新規テストにトートロジー・実質スキップは見当たらない。iOS の未指定判定は素の `UITextField` から取り出した既定色との照合、Android の復元判定は `ColorStateList` のインスタンス同一性という、実装が誤ると落ちる形になっている。Robolectric の Theme 追従テスト (`EntryCellPlaceholderThemeRefreshTest.kt`) も `awaitConvergence` を使い、idle 系だけの「待ったつもり」を避けている。

## アクションプラン

1. Android 入力文字色 valueText 是正の視覚証跡を追加する、または `ui/brief.md` の照合結果に証跡範囲外であることを明記する (Minor / 優先度: 高)
2. 同一 Cell・同一色の再バインドで `setHintTextColor` を再適用しないことを検証するテストを 1 件追加する (Minor)
3. Android `EffectiveStyle` から未読の `placeholderColor` フィールドを外し、解決を companion 関数へ一本化する (Minor)
4. (任意) ホスト既定 hint 色が `null` の場合の復元の扱いを doc または実装で明示する (Suggestion)
5. (任意・本 change 外) サンプルのダークモード時の Cell 背景の扱いを別途起票するか判断する (Suggestion)
