# 検証結果: fix-ios-full-content-refresh (001 回目)

**日付**: 2026-08-06
**判定**: INVALID

対象デルタスペック: `kasane/changes/fix-ios-full-content-refresh/specs/settings-view-ios-ui/spec.md`
deviation.md: 存在しない (乖離記録なし)

## テスト実行 (検証者による実行)

```
cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'
```

| bundle | 実行件数 | 失敗 |
|---|---|---|
| KsSettingsViewBridgeTests | 28 | 0 |
| KsSettingsViewCoreTests | 83 | 0 |
| KsSettingsViewSwiftUITests | 76 | 0 |
| KsSettingsViewUITests | 436 | 0 |
| **合計** | **623** | **0** |

`** TEST SUCCEEDED **` / 終了コード 0。オーケストレーター報告の件数と一致する。

## 対応表

### ADDED Requirement: full 更新における同一 ID Cell の内容反映

Requirement 本文の各節の実装対応:

| Requirement 本文の契約 | 実装 | 状態 |
|---|---|---|
| full snapshot 適用経路で同一 ID Cell の内容を表示へ反映する | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1196-1210` | ✅ |
| 行 identity 維持 (supplementary 再構成対象外 かつ 具象型同一) | `ios/Sources/KsSettingsViewUI/FullSnapshotContentTargets.swift:63-75` (`sectionIsReloaded` guard + `type(of:)` 比較) | ✅ |
| Section 再構成時 / 具象型変更時は cell 交換を許容し内容は最新 | 同上 `reload` 分離 + `KsSettingsViewController.swift:1208-1210` | ✅ |
| 対象は旧・新 visible projection の双方に存在する Cell に限る | `FullSnapshotContentTargets.swift:53-65` (`oldCellsByID` 引き当て失敗は `continue`) | ✅ |
| 対象が空でも構造反映は必ず実行 | `KsSettingsViewController.swift:1212` (`dataSource.apply` は無条件) | ✅ |

Scenario 対応:

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| full 更新で表示中セルの内容変化が反映される | `KsSettingsViewController.swift:1196-1207` | `ios/Tests/KsSettingsViewUITests/FullSnapshotContentRefreshTests.swift:74` | ✅ |
| 内容変化した表示中セルの行 identity が維持される | `FullSnapshotContentTargets.swift:66-75` / `KsSettingsViewController.swift:1201-1207` | 同上 `:113` (`firstRowBefore === firstRowAfter`) | ✅ |
| 構造変更と内容変更が混在する full 更新 | `FullSnapshotContentTargets.swift:62-77` | 同上 `:142` | ✅ |
| 可視性と内容の同時変更で内容が取りこぼされない | 同上 (visible projection 突き合わせ) | 同上 `:175` | ✅ |
| replaceSection で同一 ID Cell の内容変化が反映される | `KsSettingsViewController.swift:1196-1207` (replaceSection は full 経路へ合流) | 同上 `:205` | ✅ |
| header と Cell 内容の同時変更で両方が反映される | `FullSnapshotContentTargets.swift:63,71` (reload 対象 Section を除外) | 同上 `:233` | ✅ |
| 同一 ID で具象型が変わる Cell の差し替え | `FullSnapshotContentTargets.swift:66-70` / `KsSettingsViewController.swift:1208-1210` | 同上 `:261`、`:296` (reload Section との同居) | ✅ |

対象選定契約そのものの単体検証: `ios/Tests/KsSettingsViewUITests/FullSnapshotContentTargetsTests.swift` (12 ケース。初回適用・完全同値・新規挿入・削除・Cell / Section の可視性切替・移動 (同一 Section 内 / 別 Section 間)・reload 対象 Section 除外・具象型変更の分離)。いずれも返却 ID 集合の完全一致を検査しており、全件 reconfigure する誤実装では落ちる。

### MODIFIED Requirement: SwiftUI DSL の headerHeight 変更の表示反映

| Requirement 本文の契約 | 実装 | 状態 |
|---|---|---|
| headerHeight 変化を可視性 preflight と同じ段階で検出し `.full(newRoot)` のみ発行 | `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:69-72` | ✅ |
| 検出対象は 正値A→正値B / -1→正値 / 正値→-1 のいずれも含む | `DSLDiffCalculator.containsHeaderHeightChange` (未変更) / テスト `DSLDiffCalculatorTests.swift:268,281,294` | ✅ |
| `.full` に続けて `.replaceCell` を発行しない | `contentUpdateDiffs` を削除 (`DSLDiffCalculator.swift` から関数ごと除去) | ✅ |

Scenario 対応:

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| headerHeight のみの変更が表示へ反映される | `DSLDiffCalculator.swift:69-72` | `DSLDiffCalculatorTests.swift:268/281/294` (diff 列)、`ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:590` (表示中 header の実高さ) | ✅ |
| headerHeight と Cell 内容の同時変更で両方が反映され内容再適用は一度だけ | `DSLDiffCalculator.swift:69-72` + `KsSettingsViewController.swift:1196-1207` | `DSLDiffCalculatorTests.swift:310` (`.full` 1 件のみ + 新ツリーが新内容を運ぶ)。**THEN 後段「表示は header の高さと Cell の内容の両方が新しくなる」に対応するテストが存在しない** | ❌ |
| headerHeight が不変なら preflight は発火しない | `DSLDiffCalculator.swift:69-72` (分岐に入らない) | `DSLDiffCalculatorTests.swift:342` | ✅ |

## 追加検査

### tasks.md の虚偽チェック

| タスク | 判定 |
|---|---|
| 1.1 / 1.2 / 1.3 | ✅ 実装あり |
| 2.1〜2.8 | ✅ 対応テストあり |
| 2.9 | ⚠️ **前段のみ完了**。`DSLDiffCalculatorTests` の期待値更新 (`.full` 1 件) は済んでいるが、同タスクが明記する「表示レベルの両方反映 (高さ + 内容) は UI 層テストで検証する」に対応する UI 層テストが追加されておらず、既存の該当候補 (`SectionAccessoryRenderingTests.swift:632`) は変更で廃止された `.full` → `.replaceCell` 系列を Store API で再現するテストであり、新しい単一 `.full` 経路を通っていない |
| 3.1 | ✅ 検証者側で再実行し 623 件 / 0 failures を確認 |

### 逆流検査 (足場アーティファクトの書き換え)

`proposal.md` / `specs/` / `tasks.md` は未追跡 (`??`) のため git 履歴による検査ができない。代替として mtime を用いた:

| ファイル | mtime |
|---|---|
| `specs/settings-view-ios-ui/spec.md` | 10:59:22 |
| `proposal.md` | 10:59:37 |
| 実装ファイル群 (`DSLDiffCalculator.swift` ほか) | 11:18:04〜11:22:16 |
| `tasks.md` | 11:23:34 |

spec / proposal はいずれも実装着手より前で停止しており、実装中の書き換えの形跡はない。`tasks.md` の実装後更新はチェック記入によるもので想定内。`exploration.md` の変更 (working tree 上の `M`) は実挙動検証結果の追記であり、内容も日付も提案作成前 (10:31) の探索フェーズのもの。**逆流なし**と判定する。

なお、未追跡ファイルは diff ベースのレビュー・lint から見落とされやすい (実際 `scripts/comment-policy-lint.py` は未追跡の新規 3 ファイルを検査対象から落とした)。本検証では rules モジュールを直接適用して補った (結果はいずれも clean)。

### 未記録乖離の洗い出し

❌ 1 件。deviation.md への記録はない。

**見立て**: 実装ではなく**テストの追加**で解消すべき。検証者が使い捨てプローブ (`replaceAll` 1 回で `headerHeight` 40→90 と Cell title を同時変更し、header の実 frame 高さ 90 と Cell title の両方を検査) を実行したところ**合格**しており、Scenario の挙動自体は満たされている。欠けているのは回帰テストのみで、deviation として合意する性質のもの (実装を仕様から意図的に外す判断) ではない。プローブは判定後に削除済みで working tree は元の状態に戻っている。

### UI 変更

`ui/` アーティファクトを持たない変更 (視覚仕様の変更なし)。該当なし。

## 判定

**INVALID** — ❌ 1 件 (MODIFIED Requirement の Scenario「headerHeight と Cell 内容の同時変更で両方が反映され内容再適用は一度だけ」の表示レベル THEN に対応テストが無く、deviation.md への記録もない)。

実装側の欠落・乖離はなく、テスト 1 件の追加で VALID に到達する。
