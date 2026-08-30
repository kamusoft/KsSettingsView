# レビュー結果: fix-ios-full-content-refresh (1 回目)

**日付**: 2026-08-06
**判定**: CHANGES_REQUESTED

指摘件数: Critical 0 / Major 0 / Minor 2 / Suggestion 2

## サマリー

`applyFullSnapshot` への内容再適用の追加は、対象選定を純粋 helper (`FullSnapshotContentTargets`) へ分離した設計・境界ケースを ID 集合で直接検査する単体テスト・実描画を観測する UI テストのいずれも質が高く、デルタスペックの ADDED Requirement は全 Scenario が実装とテストで満たされている。DSL 側の `.replaceCell` 続発の廃止も、2 分岐を `||` へ統合した結果として素直で、コメントも現在の仕様を現在形で説明できている。

一方で、この続発廃止によって**旧契約 (`.full` → `.replaceCell`) を前提に書かれた既存 UI テストの根拠が現実と食い違ったまま残り**、その結果 MODIFIED Requirement の表示レベルの回帰網が空いている。実装の欠陥ではなく、テストと説明の追随漏れ。

検証者側でビルド・全件テストを実行済み: 623 件 / 0 failures (`** TEST SUCCEEDED **`)。内訳と証跡は `verify-001.md`。

## 確認した観点 (指摘に至らなかったもの)

- **未テストのエッジケースを実測**: 使い捨てプローブで (a) 具象型変更 + 行移動の同一 apply 同居、(b) 内容変更 + 行移動の同居、(c) `reloadSections` と `reloadItems` の同一 Section 重複 (既存テスト `FullSnapshotContentRefreshTests.swift:296` と同型) を実行し、いずれも UIKit 例外なし・表示は最新で合格。プローブは判定後に削除済み (working tree は元の状態)
- **`visibleSections` の信頼性**: `oldVisible` の供給元である `self.visibleSections` は全ての部分更新経路 (`applyReplaceCell` の hidden no-op 分岐を含む) で model と同期して更新されており、旧 projection が実表示より新しくなって内容差分を取りこぼす経路はない
- **ソースコメント規約**: 新規 3 ファイルは未追跡のため `scripts/comment-policy-lint.py` の検査対象から落ちる。`comment_policy_rules.py` を直接適用して確認したところいずれも clean。`KsSettingsViewController.swift` の既存違反 54 件は本変更の追加行 (1129-1130 / 1193-1210) の外側にある既存債務
- **対象選定テストの検出力**: `FullSnapshotContentTargetsTests` は返却 ID 集合の完全一致 (`XCTAssertEqual`) を検査しており、全件 reconfigure する誤実装・新規挿入や hidden を巻き込む誤実装のいずれでも落ちる。トートロジーではないためミューテーション実測は不要と判断した

## 指摘事項

### [🟡 Minor] 廃止した `.full` → `.replaceCell` 契約を現行仕様として説明する既存テストが残っている

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:629-632`、`:655`、`:672`

**問題点**: 本変更は DSL の headerHeight preflight から `.replaceCell` の続発を廃止した。しかしこのテストは doc コメントで「DSL の headerHeight preflight が発行する `.full` → `.replaceCell` の順を Store 経路で適用したとき」と述べ、インラインコメント (`:655`) でも「DSL 側の preflight が発行するのと同じ順序 (`.full` → `.replaceCell`) で Store を操作する」と説明している。この系列はもう存在しない。`concepts/cross/conventions/comment-policy.md` の「現在の仕様を現在形で書く」(および `lessons/impl.md` L-002) に反し、そのファイルだけを読む人に「DSL は今も 2 件発行する」と誤解させる。

さらに、このテストは**主張している内容を検出できなくなっている**。`replaceAll` 単体で Cell 内容が表示へ反映されることは `FullSnapshotContentRefreshTests.swift:74` が示しており、続く `store.replaceCell` が完全な no-op でも `:671` のアサーション (`visibleCellTitle == "新タイトル"`) は通る。失敗メッセージ「`.full` に続く `.replaceCell` で Cell の内容が表示へ反映されていない」が指す事象を、このテストはもう検出しない。

**推奨修正**: テストの存在意義を現行仕様に接地し直す。`.full` と `.replaceCell` を連続適用する Store API 系列そのものの回帰テストとして残すなら、テスト名・doc コメント・失敗メッセージから DSL preflight への言及を外し、「Store API で `.full` 直後に同一 Cell の `.replaceCell` を適用しても表示が壊れない」という現在形の説明に書き直す。DSL 経路の代表として残す意図なら、下の Minor と合わせて単一 `replaceAll` 版へ置き換える。

### [🟡 Minor] MODIFIED Requirement の表示レベルの回帰テストが無く、tasks 2.9 の記述が満たされていない

**該当箇所**: `kasane/changes/fix-ios-full-content-refresh/tasks.md:19`、`ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift:310`

**問題点**: デルタスペックの Scenario「headerHeight と Cell 内容の同時変更で両方が反映され内容再適用は一度だけ」の THEN は 2 つの主張を持つ — (1) diff 算出が `.full` のみを発行する、(2) 表示は header の高さと Cell の内容の両方が新しくなる。(1) は `DSLDiffCalculatorTests.swift:310` が押さえているが、(2) に対応するテストが無い。tasks 2.9 は「表示レベルの両方反映 (高さ + 内容) は UI 層テストで検証する」と明記しているが、該当する UI 層テストは追加されておらず、既存の候補は上の Minor で挙げた旧契約ベースのテストである。

この結果、「単一の `.full` 適用で headerHeight と Cell 内容が同時に表示へ届く」という本変更の中心的な帰結だけが回帰網の外にある (headerHeight 単独は `SectionAccessoryRenderingTests.swift:590`、内容単独は `FullSnapshotContentRefreshTests.swift:74` が押さえているが、両者の合成は誰も見ていない)。

なお挙動自体は正しい。検証者が使い捨てプローブで `replaceAll` 1 回による `headerHeight` 40→90 + Cell title 変更を実行し、header supplementary の実 frame 高さ 90 と Cell title の両方が更新されることを確認済み (プローブは削除済み)。欠けているのは回帰テストのみ。

**推奨修正**: `FullSnapshotContentRefreshTests` または `SectionAccessoryRenderingTests` に、単一の `store.replaceAll` で `headerHeight` と同一 ID Cell の内容を同時に変え、表示中 header の実高さと行の title の双方を検査するテストを 1 件追加する。

### [🔵 Suggestion] 具象型変更 Cell が reload 対象 Section でも `reload` へ積まれる意図がコードから読めない

**該当箇所**: `ios/Sources/KsSettingsViewUI/FullSnapshotContentTargets.swift:66-71`

**問題点**: 具象型の判定が `sectionIsReloaded` の guard より**前**にあるため、Section 全体が `reloadSections` で再構成される場合でも当該 Cell が `reload` に積まれ、`snapshot.reloadSections` と `snapshot.reloadItems` が同じ Section へ重ねてかかる。実測では例外も表示異常も出ず (`FullSnapshotContentRefreshTests.swift:296` が押さえている)、無害な冗長だが、`reconfigure` 側だけが除外される非対称の理由がコメントに無く、後から読むと片方の guard 漏れに見える。

**推奨修正**: 順序が意図的であることを 1 行のコメントで補う (「Section 再構成側でも内容は最新になるため実質冗長だが、除外の判断を Section 再構成の有無に依存させない」等)。または具象型変更も `sectionIsReloaded` で除外して非対称を消す。どちらでも挙動は変わらない。

### [🔵 Suggestion] `#available(iOS 15.0, *)` は deployment target (iOS 16) では常に真

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1202-1206`

**問題点**: `ios/Package.swift` の `platforms` は `.iOS(.v16)` であり、`reloadItems` へのフォールバック分岐は到達しない。

**推奨修正**: 同ファイルの既存 3 箇所 (`:320` `:1100` `:1599`) が同じ形を採っているため、**本変更だけを直すと逆に不揃いになる**。今回は現状維持でよく、直すなら別変更でファイル全体を一括整理するのが妥当。判断材料として記録するに留める。

## アクションプラン

1. Minor 2 件はセットで扱うのが効率的 — 単一 `replaceAll` で `headerHeight` + Cell 内容を同時変更し表示を検査するテストを追加し、あわせて `SectionAccessoryRenderingTests.swift:629-672` から廃止済み DSL 契約への言及を外す
2. Suggestion 1 (`FullSnapshotContentTargets` の guard 順序) はコメント 1 行で解消できる。取り込むかは実装側の裁量
3. Suggestion 2 は本変更では対応しない (既存コードとの一貫性を優先)
