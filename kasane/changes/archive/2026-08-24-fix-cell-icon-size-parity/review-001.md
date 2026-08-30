# レビュー結果: fix-cell-icon-size-parity (001 回目)

**日付**: 2026-08-23
**判定**: APPROVED

## サマリー

両 platform のデルタスペック (Requirement 3 + 3、Scenario 計 24) はいずれも実装とテストで満たされており、無断の仕様逸脱は見つからなかった。Android 2582 件 / iOS 581 件をレビュー側で再実行して全件パス、iOS の制約衝突 0 件も再現できた。視覚証跡は実機・Simulator の実描画で、brief の主張 (Android の icon 枠が承認モックと一致・SF Symbols の title 開始位置が収束・狭幅で title が残り valueText が省略される) が画像そのもので裏付けられている。

設計面では、Android の `hasFillingInlineTrailing` による「残り幅の受け手が別に居る行」の区別、iOS の `setIconVisible(_:size:)` への表示制御集約 (非表示時の制約 deactivate) がいずれも要点を外していない。共通行を組む 13 の ViewHolder が `buildCellBaseViews` と `applyCellBaseLayout` で 1:1 に対応していることを確認し、既定配分の入れ替えで取り残される経路がないことも検証した (ButtonCell のボタンスタイル分岐が唯一の例外で、deviation の付随修正で塞がれている)。

指摘は Minor 2 件・Suggestion 2 件で、いずれも実装の正しさではなく証跡の整合とテストの検出力に関するもの。

## 指摘事項

### [🟡 Minor] tasks.md のチェックボックスが全件未消化のまま

**該当箇所**: `kasane/changes/fix-cell-icon-size-parity/tasks.md` (全 24 項目)

**問題点**: 実装は完了しているのに、`tasks.md` のチェックボックスが 1 件も `[x]` になっていない。虚偽チェック (未実装なのに `[x]`) の逆パターンだが、証跡としての害は同種で、後続の verify・蒸留・オーナー確認が「どのタスクが意図的に消化され、どれが残っているか」をアーティファクトから読めない。特に 2.3 (「削除」と本文に書かれた項目)・3.4 (brief への記入)・4.1 (comment-policy lint)・4.2 (蒸留への申し送り確認) は、成果物を見ても実施の有無が判別しにくい種類のタスクで、チェック状態が唯一の記録になる。

なお 4.1 については、レビュー側で `python3 scripts/comment-policy-lint.py` を触ったファイルに通して禁止 0 件を確認済み。ただし同スクリプトは追跡中のファイルしか走査しないため、新規 3 ファイル (`CellIconFrameTest.kt` / `CellIconFrameTests.swift` / `CellRowWidthAllocationTests.swift`) は引数に渡しても検査対象から外れる (実行結果の「検査対象 8 ファイル」が 11 引数に対する実数)。この 3 ファイルは `scripts/comment_policy_rules.py` の `scan_text` を直接呼んで別途 0 件を確認した。

**推奨修正**: 実施済みタスクにチェックを入れる。新規 (untracked) ファイルが lint の走査対象外になる件は、コミット後に再実行するか、この制約を認識した上で 4.1 のチェックを入れる。

### [🟡 Minor] brief.md が存在しないファイル名を視覚証跡として引用している

**該当箇所**: `kasane/changes/fix-cell-icon-size-parity/ui/brief.md:39`

**問題点**: 「視覚照合の結果」の Android 節が証跡を `verification/android-overflow-long-value-before.png` / `-after.png` と書いているが、実際に置かれているのは `android-overflow-long-value-before.png` と `android-overflow-long-value.png` で、`-after.png` は存在しない (tasks 3.3 が指定したファイル名は `android-overflow-long-value.png`)。brief を頼りに再照合しようとした読み手 (レビュー・verify・蒸留) がファイルを見つけられない。

内容自体は問題なく、`android-overflow-long-value.png` が修正後の証跡であることは画像を開いて確認した (Wi-Fi 行が `Wi-Fi demoAP-0a1b…` と title 全文 + valueText 末尾省略になっており、before 側では title が完全消失している)。

**推奨修正**: brief の記述を実ファイル名 `android-overflow-long-value.png` に合わせる (足場アーティファクトの記述側を実体に合わせる方向。ファイル名の変更は tasks 3.3 の指定と食い違うので避ける)。

### [🔵 Suggestion] radius 変更時の `invalidateOutline()` を除去してもテストが落ちない (ミューテーション実測)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:550-556` / テスト側 `CellIconFrameTest.kt` の `outlineOf()` ヘルパ

**問題点**: `applyIconFrame` は同一 View に既に `IconFrameOutlineProvider` が付いている場合、provider の `radiusPx` を書き換えて `iconView.invalidateOutline()` で View 側のキャッシュを再構築させている。この `invalidateOutline()` を落とすと、画面上の clip 形状は前回の radius のまま固まる (Spec Scenario「再 bind で radius の変更と解除が反映される」が実挙動として破れる) が、テストはこれを検出できない。テストヘルパが

```kotlin
view.outlineProvider.getOutline(view, outline)
```

と provider を直接叩いており、View がキャッシュした outline を経由しないため。実測で確認済み: `iconView.invalidateOutline()` の 1 行だけを削除して `./gradlew :ks-settingsview-ui:testDebugUnitTest --tests "*CellIconFrameTest*"` を実行し BUILD SUCCESSFUL (ミューテーション生存)。検証後は backup との `shasum` 一致で原状復帰を確認済み (`395ca6e0…`)。

**推奨修正**: `IconFrameOutlineProvider` の `radiusPx` を `val` にして、radius が変わったら新しいインスタンスを `outlineProvider` に代入する形へ寄せる。`View.setOutlineProvider` は内部で outline を再構築するので明示的な `invalidateOutline()` 呼び出し自体が不要になり、「provider インスタンスが差し替わったか」を既存の `outlineOf()` がそのまま検出できるようになる (= 検出力の穴が閉じる)。

### [🔵 Suggestion] `setIconVisible(_:size:)` の `size` 既定値 0 が呼び間違いを許す

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:225`

**問題点**: `internal func setIconVisible(_ isVisible: Bool, size: CGFloat = 0)` は、表示側の呼び出しでも `size` を省略できてしまう。`setIconVisible(true)` はコンパイルが通り、`.required` の 0pt サイズ制約を有効化して icon 枠を無言で潰す。表示するときは必ずサイズが要る (非表示のときだけ不要) という非対称を、既定値がぼかしている。現在の呼び出し 3 箇所はすべて正しいので実害は出ていない。

**推奨修正**: 既定値を外して表示側に `size` を必須化するか、`showIcon(size:)` / `hideIcon()` の 2 入口に分ける。どちらでも「表示にはサイズが要る」が型で表現される。

## 確認した観点 (指摘なし)

- **ビルド・テスト**: Android `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL、JUnit XML 集計で tests 2582 / failures 0 / errors 0 / skipped 0。iOS `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → `** TEST SUCCEEDED **`、`Executed 581 tests, with 0 failures`。`Unable to simultaneously satisfy constraints` の出力は 0 行で、`ui/verification/ios-test-constraints.log` の主張を再現できた
- **視覚証跡 (process L-003)**: `verification/` の PNG 8 点 + ログ 1 点が実在し、実機/Simulator の実描画であることを画像を開いて確認。`android-modern-standard-after.png` は `mock/approved.png` (iOS) と icon 枠の寸法比・角丸・title 開始位置が視覚的に一致。`ios-common-fields-compare-annotated.png` は基準線 56.7pt と各行の実測値が注記された A/B で、修正前のばらつきが実際に見て取れる。`android-overflow-long-value-before.png` は Wi-Fi 行の title が幅 0 で消失した修正前の症状を捉えており、A/B として成立している
- **既定配分の入れ替えで取り残される経路**: `buildCellBaseViews` を呼ぶ 13 ファイルと `applyCellBaseLayout` を呼ぶ 13 ファイルが完全一致し、各 ViewHolder の `applyCellBaseLayout` 呼び出しが `bind` 直下 (無条件) であることを確認。分岐で迂回するのは `ButtonCellViewHolder` のボタンスタイル経路のみで、deviation 記録の付随修正 (`applyTitleWidthMode(views, fillsRow = true)`) で塞がれている。`CustomCell` は共通行を組まないため影響外 (Non-Goal どおり)
- **EntryCell の退行**: `hasFillingInlineTrailing` フラグにより、EntryCell (`valueText == null`) が既定の全幅切り替えに巻き込まれない。フラグを外すと既存の EntryCell 配分テスト群が落ちる構造になっており、ガードに検出力がある。Spec Scenario「EntryCell では title がコンテンツ幅を維持し入力フィールドが縮む」の「長い title」条件は、Android は `EntryCell の入力フィールド幅は固定最低幅に依存せず主行幅から title 幅を引いた値になる` (長い title + 残り幅 0 の境界)、iOS は `test_EntryCellではtitleがコンテンツ幅を維持し入力フィールドが縮む` が担保している
- **テストの空振り**: iOS の `test_intrinsic幅が異なるSFSymbolsでもicon列幅が揃う` は、検証に使う SF Symbols の intrinsic 幅が実際に複数種類あることを先にアサートしており前提が空振りしない。Android の `assertSquareFrame` は `LayoutParams` だけでなく measure/layout 後の `iconView.width` / `height` の実測を見ている。`行内 trailing がない Cell では title が主行の全幅を使う` は「コンテンツ幅より広い」ことまで見ており、`wrap_content` のままでは通らない
- **無効値の扱い**: 両 OS とも解決関数レベル (`EffectiveStyleResolutionTest` / `Tests`) で 0・負値・NaN・±∞ を網羅し、iOS は `CellIconFrameTests` でレイアウト結果まで通した検証も持つ。`radius = 0` を「角丸なし」の有効な指定として次段へ送らないことも両 OS で固定されている
- **プロジェクト固有規約 (process L-002)**: `concepts/cross/conventions/comment-policy.md` に対し、変更 4 + 3 ファイルと新規テスト 3 ファイルの全件で禁止参照 0 件。ADR 参照は `core/ADR-0025` / `core/ADR-0026` / `android/ADR-0002` の許容形式のみ。現行契約と矛盾する既存コメントの是正 (`CellBaseLayout` の階層図・`ButtonCellViewHolder` KDoc・`EntryCellViewHolder.create` の配分説明・iOS の階層コメントの優先度記述) が漏れなく行われており、旧配分を語る記述は残っていない。`concepts/cross/conventions/runtime-behavior-verification.md` の 3 条件 (修正前の再現・修正後の同一手順での解消・証跡の残置) も両 OS で満たされている
- **ADR 適合**: core/ADR-0025 (角丸は正方形枠に対して適用・clamp しない)・core/ADR-0026 (title を守り valueText を省略・icon とアクセサリは主行より先に譲らない・行内 trailing なしでは title 全幅・EntryCell は従来どおり) のいずれも実装とテストが契約どおり。ADR-0025 が「iOS は無変更」とした `layer.cornerRadius` + `clipsToBounds` の枠 clip は維持されている
- **deviation の同梱条件**: 記録された付随修正 5 件はいずれも本務で触るファイル内、または本務の変更が直接開けた穴の修復で、公開 API・データスキーマ・既存 ADR に触れていない。`normalizedIconImage(_:)` は Requirement「枠の寸法が画像の intrinsic size に依存しない」を満たすために必要な実装手段であり、スコープ膨張ではない
- **リソース・性能**: `applyIconFrame` は寸法が変わったときだけ `layoutParams` を差し戻し、`applyTitleWidthMode` も値が変わるときだけ再代入して不要な `requestLayout()` を避けている。`IconFrameOutlineProvider` は View ごとに 1 インスタンスで、bind ごとの生成にはなっていない

## アクションプラン

1. (Minor) `tasks.md` の実施済みタスクにチェックを入れる。新規ファイルが `comment-policy-lint.py` の走査対象外になる制約を踏まえて 4.1 を判断する
2. (Minor) `ui/brief.md:39` の証跡ファイル名を `android-overflow-long-value.png` に直す
3. (Suggestion) `IconFrameOutlineProvider.radiusPx` を `val` 化して radius 変更時にインスタンスを差し替える (明示的な `invalidateOutline()` が不要になり、テストの検出力の穴も閉じる)
4. (Suggestion) `setIconVisible(_:size:)` の `size` 既定値を外す、または表示/非表示を別メソッドに分ける

1・2 は蒸留・アーカイブ前に処理することを推奨する。3・4 は本 change で対応しても、別途起票しても構わない。
