# レビュー結果: fix-dsl-header-height-diff (001 回目)

**日付**: 2026-08-05
**判定**: APPROVED

## サマリー

両 platform の `DSLDiffCalculator` に headerHeight の preflight 検出が追加され、デルタスペックの 5 Requirement / 8 Scenario がいずれも実装とテストで裏付けられている。Android は `Full` のみ + `contentUpdates` 空、iOS は `.full` に続く `.replaceCell` という非対称も spec の記述どおりで、iOS 側の `.replaceCell` 追加が必要である根拠 (`.full` は同一 ID Cell を再構成しない) は既存テスト `test_fullDiffでheader不変ならCellは再構成されない` が固定している契約と整合する。Non-Goal の境界 (`KsSettingsViewController.swift` 無変更) も守られており、ADR-0018 の 4 象限は Android Store (既存 `FullUpdateContentSyncTest`) / Android DSL / iOS Store / iOS DSL のすべてが自動テストで閉じている。Critical / Major はなく、残りは優先度の低い Minor と Suggestion のみ。

## 実施した検証

- **Android**: `cd android && ./gradlew test --rerun-tasks` → **2014 tests / 0 failures** (debug + release 両 variant、`build/test-results/**/TEST-*.xml` の `tests`/`failures` 集計)
- **iOS**: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>' (iPhone 17 Pro / iOS 26.1)` → **Executed 414 tests, with 0 failures**
- 新規追加テスト 7 件がシミュレータで実際に実行され pass することを `-only-testing:KsSettingsViewSwiftUITests/DSLDiffCalculatorTests -only-testing:KsSettingsViewUITests/SectionAccessoryRenderingTests` で個別確認済み (テスト名がログに出ることまで確認)
- `python3 scripts/comment-policy-lint.py <touched files>` → 5 ファイルとも **禁止 0 件**。`--selftest` も全件 OK (検査が無音化していないことを確認)
- コメント中の `core/ADR-0009` (Theme を構造モデルから分離し UI 層の独立経路で扱う) / `core/ADR-0010` (表示状態同期の三層分離) / `core/ADR-0018` の参照は、`kasane/decisions/core/` の各 ADR 本文と内容が対応していることを確認
- 検証証跡 `07`/`10` の実画像を確認し、README の主張 (修正前は高さ不変 / 修正後は高さ 160pt と `固定 Cell A（内容更新）` が同時反映) と一致することを確認

## 取りこぼしを疑って確認し、問題なしと判定した観点

判定の根拠を残すため、疑って調べたうえで問題なしとした点を記録する。

1. **preflight の早期 return が Root H/F の `UpdateAccessory` を落とす件** — `compute` は preflight で早期 return するため、同一再評価で `rootHeader` / `rootFooter` も変わっていた場合その `UpdateAccessory` は発行されず、`SettingsRoot` は sections しか運ばない (両 platform)。しかし DSL 経路では Root H/F は diff を経由せず wrapper が毎回直接適用する (Android: `KsSettingsViewComposable.kt:159-160` の `AndroidView` update ブロック / iOS: `KsSettingsView.swift:181-182` の `updateUIViewController`) ため、表示上の取りこぼしは生じない。**この観点での退行なし**
2. **Section H/F accessory・Section/Cell の追加・削除・移動との併発** — preflight が `Full` / `.full` を返して構造 Diff を飛ばすが、`Full` の適用が snapshot 全体を作り直すため取りこぼさない。iOS の Section header 変更は既存テスト `test_fullDiffのtextヘッダ変更が表示中のsupplementaryに反映される` が担保している
3. **旧ツリーに存在しない Section ID** — `containsHeaderHeightChange` は旧マップに無い ID を `continue` / `guard ... else { continue }` で読み飛ばす。この Section は insert として扱われ headerHeight ごと新規描画されるため、検出しないのが正しい
4. **可視性 preflight との合流 (`||`) による二重発行** — Android は 1 本の `if` で `Full` 1 件を返すだけ、iOS は可視性 preflight が先に `return` するため headerHeight 側に到達しない。二重発行は起きない
5. **iOS `contentUpdateDiffs` が hidden Cell の `.replaceCell` を出しうる件** — `applyReplaceCell` は「snapshot に載っていない場合は model 更新のみの自然な no-op」と設計されており (`KsSettingsViewController.swift:1075` 付近)、危険はない
6. **新規アサーションの回帰検出力** — 新テストは同一アクセサ (`visibleHeaderFrameHeight`) で「更新前 40 / 更新後 90」の前後対を取っているため、トートロジーではありえない (更新前 40 が通る以上、更新後の 90 は値の変化を実際に観測している)。`lessons/code-review.md` L-001 のミューテーション実測は、この構造上不要と判断して実施していない
7. **足場アーティファクトの書き換え** — `tasks.md` の差分はチェックボックス 11 個のみ。`proposal.md` / `specs/` は無変更
8. **tasks.md の虚偽チェック** — 1.1〜4.2 の全項目に対応する実装・テスト・証跡の実体を確認済み

## 指摘事項

### [🟡 Minor] iOS「headerHeight + Cell 内容の同時変更」Scenario の *表示* 反映が自動テストでは payload 止まり

**該当箇所**: `ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift:507-533`

**問題点**: iOS spec の Scenario「headerHeight と Cell 内容の同時変更で両方が反映される」の THEN は「`.full` に続けて当該 Cell の `.replaceCell` を発行し、**表示は header の高さと Cell の内容の両方が新しくなる**」と表示結果まで要求している。追加された `test_headerHeightとCell内容の同時変更でfullに続けてreplaceCellが発行される` は diff 列の形 (件数・`cid.id`・`newCell.title`) しか観測しておらず、「`.full` → `.replaceCell` の順で適用したときに表示中の Cell が実際に更新される」ことは自動テストでは押さえられていない。表示の主張は証跡 `10-ios-after-fix-height-and-content.png` の目視のみに依存する。

second-opinion-001 の Major-3 は「テストでは ... 表示中 header の実高さと**表示中 Cell の title** を観測してください」と勧告していたが、tasks 3.1 に落ちたのは header 実高さだけだった。

**評価**: `runtime-behavior-verification.md` はスクリーンショット証跡を実行時挙動の完了判定として認めており、ADR-0018 の対称テスト義務は headerHeight に対するものなので、本 change の受け入れを妨げるものではない。ただし「`.full` の直後に同一 Section の `.replaceCell` を適用する」という組み合わせは本 change で新設された経路であり、目視証跡は回帰を検出しない。

**推奨修正**: `SectionAccessoryRenderingTests` (または `ContentUpdateBatchTests`) に、`controller.applyDiff(.full(...))` → `controller.applyDiff(.replaceCell(...))` を連続適用して、表示中 header の実 frame 高さと表示中 Cell の label テキストの両方を観測するテストを 1 件追加する。本 change で対応しない場合は、蒸留時の申し送りとして残す。

### [🟡 Minor] Android の preflight 条件が `compute` と `contentUpdates` に二重定義されている

**該当箇所**: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt:54-56` と `:264-266`

**問題点**: 同じ `containsVisibilityChange(...) || containsHeaderHeightChange(...)` が 2 箇所に複製され、整合の担保がコメントの口約束 (`[compute] 側の preflight 判定と条件を一致させる必要がある`) になっている。今後 preflight 対象プロパティが増えるたびに 2 箇所を同時に直す必要があり、片方だけ更新すると「`Full` は出るのに `contentUpdates` も出る」二重反映、または逆の取りこぼしが無音で発生する。ADR-0018 が問題視する無音の失敗と同じ形。

なお `contentUpdates` は `compute` とは別に呼び出されるため、preflight 判定は 1 再評価あたり 2 回走る (Section/Cell 全走査 × 2)。設定画面の規模では実害ないが、抽出すれば副次的に解消する。

**推奨修正**: `private fun requiresFullRefresh(from: ResolvedTree, to: ResolvedTree): Boolean` を 1 本切って両者から呼ぶ。条件の単一定義になり、コメントの口約束が不要になる。

### [🔵 Suggestion] iOS `contentUpdateDiffs` が `cellLevelDiffs` の内容比較ロジックを複製している

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:195-215` (`cellLevelDiffs` の相当部分は `:320` 付近)

**問題点**: `AnyHashable(oldCell) != AnyHashable(cell)` → `.replaceCell(cellID: KsCellID(cell: oldCell), new: cell)` という内容比較・発行の組み合わせが 2 箇所に存在する。`cellLevelDiffs` の直上には「Cell 実装が満たすべき等価性規約」の長大な説明コメントがあるが、`contentUpdateDiffs` 側にはその参照がなく、規約が変わったとき片方だけ直る余地がある。

**推奨修正**: 内容変化 1 件分の判定・発行を `private static func contentUpdateDiff(old:new:) -> SettingsRootDiff?` に切り出して両者から使うか、少なくとも `contentUpdateDiffs` の doc comment から `cellLevelDiffs` の等価性規約説明を参照させる。

### [🔵 Suggestion] `containsHeaderHeightChange は同一 headerHeight で false` テストが上位テストに包含されている

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculatorTest.kt:221-225`

**問題点**: 直前の `headerHeight 不変で内容だけ変わると Full を発行せず contentUpdates で列挙される` が同じ入力で `compute` / `contentUpdates` の観測可能な結果まで検証しており、内部関数を直接叩く本テストが落ちて上位テストが落ちないケースは存在しない。回帰検出力が重複しているぶん、`containsHeaderHeightChange` を `internal` に開ける理由もこのテスト 1 件だけになっている (`containsVisibilityChange` が `internal` なので一貫性はある)。

**推奨修正**: 本テストを削除して観測可能な結果のテストに一本化するか、内部関数を直接叩く価値のある入力 (例: 旧ツリーに存在しない Section ID を新ツリーが持つケース = 検出しないことの明示) に差し替える。後者なら「取りこぼしではなく意図的な非検出」を固定できるので価値が上がる。

### [🔵 Suggestion] `DSLDiffCalculator.kt` に変更提案 ID の裸参照が残り、iOS 側と非対称

**該当箇所**: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt:60`, `:175`, `:296`

**問題点**: `comment-policy.md` が禁止する「変更提案の識別子の裸参照」(`（refactor-display-state-sync）`、L60 / L175) と「拡張子なしの裸参照」(`仕様: \`DSL → SettingsRootDiff 算出ロジック（Compose）\` の \`可視性変化の preflight 検出\` ステップ`、L296) が残っている。lint の BLOCKING パターンは kebab-case の change-id を機械検出できないため 0 件と出る (`scripts/comment_policy_rules.py` の `BLOCKING_PATTERNS` に該当パターンなし)。

本変更は同一ファイルで `refactor-display-state-sync` / `purify-core-extract-style-to-ui-layer` 参照の除去を実施しており、iOS 側では `containsVisibilityChange` の同等コメントを `core/ADR-0010` へ置換済みなのに、Android の同じ関数 (L290-297) だけ旧記述が残る非対称になっている。

**評価**: ラチェット方式では既存債務であり本変更の違反ではない。ただし同一ファイル内でクリーンアップを行った以上、残置は意図的でなく取りこぼしに見える。

**推奨修正**: L60 / L175 は `core/ADR-0010` 参照へ、L296 は iOS 側と同じ文面 (`... `Full(newRoot)` のみを発行する（core/ADR-0010）`) へ揃える。本 change の範囲外と判断するなら、`lessons` ではなく drift 側の棚卸し対象として残す。

### [🔵 Suggestion] Android の全件テストが間欠的に失敗する (本 diff 非関与)

**該当箇所**: `:ks-settingsview-ui:testReleaseUnitTest` (本 diff が触っていないモジュール)

**問題点**: レビュー中に `cd android && ./gradlew test --rerun-tasks` を計 7 回走らせたところ、**2 回** `:ks-settingsview-ui:testReleaseUnitTest FAILED` で BUILD FAILED になった。ただし `build/test-results/**/TEST-*.xml` には `<failure>` / `<error>` が 1 件も記録されておらず (集計でも 0)、同タスクを単独 (`./gradlew :ks-settingsview-ui:testReleaseUnitTest --rerun-tasks`) で回すと常に成功する。テストアサーションの失敗ではなく worker プロセス側の失敗 (並列 4 executor × `-Xmx512m` + Robolectric) が疑われる。

**評価**: 本 diff の Android 側変更は `ks-settingsview-compose` に閉じており、失敗タスクは別モジュール。**本変更起因ではないと判断する**。ただし `test-execution.md` の「実行件数の確認までが検証」という規律に対し、この不安定さは今後「全件 green」の報告を曇らせる (実際、キャッシュ有効時の `./gradlew test` は 228 タスク UP-TO-DATE / テスト 0 件実行で BUILD SUCCESSFUL になる)。

**推奨修正**: 本 change では対応不要。別途、`testOptions.unitTests` の heap 設定や `maxParallelForks` の見直しを起票し、再現したら Gradle の失敗詳細 (worker crash log) を採取する。

## アクションプラン

1. (任意・本 change 内) Minor 2 — Android の preflight 条件を `requiresFullRefresh` 1 本に抽出する。修正コスト小・退行リスク低で、ADR-0018 が嫌う無音の不整合を構造的に潰せるため優先度は Minor の中で最上位
2. (任意・本 change 内 or 蒸留申し送り) Minor 1 — `.full` → `.replaceCell` 連続適用の表示反映テストを iOS UI テストへ 1 件追加する
3. (別 change / drift 起票) Suggestion 5 — `DSLDiffCalculator.kt` に残るコメント債務の解消
4. (別 change 起票) Suggestion 6 — Android 全件テストの間欠失敗の切り分け
5. Suggestion 3 / 4 は今後 Cell 内容比較の規約や preflight 対象を触るときに併せて対応すればよい

いずれも Critical / Major ではないため、上記未対応のままでも本 change はアーカイブ可能と判断する。
