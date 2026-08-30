# 検証結果: fix-ios-full-content-refresh (002 回目)

**日付**: 2026-08-06
**判定**: VALID

前回: `verify-001.md` (INVALID — ❌ 1 件)
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
| KsSettingsViewUITests | 437 | 0 |
| **合計** | **624** | **0** |

`** TEST SUCCEEDED **` / 終了コード 0。オーケストレーター報告の件数と一致する。前回 623 からの +1 は `SectionAccessoryRenderingTests` への新規テスト 1 件 (既存 1 件は改名・書き直しのため件数に増減なし)。

## 前回 ❌ の解消確認

### MODIFIED Requirement / Scenario: headerHeight と Cell 内容の同時変更で両方が反映され内容再適用は一度だけ

| THEN の主張 | 実装 | テスト | 状態 |
|---|---|---|---|
| diff 算出は `.full(新ツリー)` のみを発行する | `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:69-72` | `ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift:310` (`diffs.count == 1`) | ✅ |
| 表示は header の高さと Cell の内容の両方が新しくなる | `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1196-1210` | `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:632` `test_replaceAll1回でheader高さとCell内容の両方が表示へ反映される` | ✅ (前回 ❌) |
| 当該 Cell への内容再適用は一度だけ行われる | 同上 | `DSLDiffCalculatorTests.swift:310` (発行 1 件) + 上記 UI テストの行 identity 検査 (`rowBefore === rowAfter`。二重適用や cell 交換が起きれば破綻する) | ✅ |

新テストは**単一の `store.replaceAll`** で `headerHeight` 40→90 と同一 ID Cell の title を同時に変え、以下を検査する:

- header の layout attributes 高さ 90 (`layoutHeaderHeight`)
- 表示中 header supplementary の実 frame 高さ 90 (`visibleHeaderFrameHeight`)
- 行の title が新しい値
- 行の Native cell インスタンスが破棄されていない

前回の ❌ の原因であった「旧契約 (`.full` → `.replaceCell`) の手動再現」は除去され、新契約の経路そのものを通っている。

### 検出力の実測 (ミューテーション probe)

「新テストが fix の検出器として機能するか」は静的読解では確定しないため、`lessons/code-review.md` L-001 に従って実測した。

- ミューテーション: `KsSettingsViewController.swift:1201` の reconfigure 適用ブロックを `if false, ...` で無効化 (内容再適用の出口だけを塞ぐ)
- 結果: `test_replaceAll1回でheader高さとCell内容の両方が表示へ反映される` が **failed**
  - 失敗箇所は `SectionAccessoryRenderingTests.swift:674` の**争点アサーションのみ** — `("Optional("旧タイトル")") is not equal to ("Optional("新タイトル")")`
  - **前提アサーションは通過**: 同テスト内の layout 高さ 90 / 実 frame 高さ 90 は到達・成功しており、高さ経路ではなく内容再適用だけを分離して捕まえている
  - 独立性の確認: `test_Store経由のreplaceAllのheaderHeight変更が表示中headerの実高さに反映される` は同条件で **passed** — 高さ側は元から別テストが押さえており、新テストは真に新しい被覆を足している
- 原状復帰: backup からの復元後、shasum 一致を確認済み (`25f43286ffedfddac45c17de0387061fdad096e7`)。復元後の全件再実行が上表の 624 件 / 0 failures

## 対応表 (再掲。001 から状態変化のない行は要約)

### ADDED Requirement: full 更新における同一 ID Cell の内容反映

Requirement 本文の 5 契約、Scenario 7 件すべて ✅ (verify-001 の対応表から変更なし。実装ファイルはコメント追記のみで挙動は不変)。

対象選定の単体検証 `ios/Tests/KsSettingsViewUITests/FullSnapshotContentTargetsTests.swift` (13 ケース) に、`:86` の重複検査 (`targets.reconfigure.count == 2`) が追加された。`ids()` が Set へ畳むため同一 ID の二重登録を見逃す穴があったが、これで塞がれている。

### MODIFIED Requirement: SwiftUI DSL の headerHeight 変更の表示反映

| Scenario | テスト | 状態 |
|---|---|---|
| headerHeight のみの変更が表示へ反映される | `DSLDiffCalculatorTests.swift:268/281/294`、`SectionAccessoryRenderingTests.swift:590` | ✅ |
| headerHeight と Cell 内容の同時変更で両方が反映され内容再適用は一度だけ | 上記 (前回 ❌ → ✅) | ✅ |
| headerHeight が不変なら preflight は発火しない | `DSLDiffCalculatorTests.swift:342` | ✅ |

## 追加検査

### 旧挙動のテスト残存 (MODIFIED の必須検査)

リポジトリ全体を `preflight` / `.full` + `.replaceCell` の併記で走査した結果、廃止された契約を現行仕様として述べる記述は**残っていない**:

- `SectionAccessoryRenderingTests.swift:629-632` — 書き直し済み。テスト名・doc コメント・失敗メッセージのいずれからも DSL preflight と `.replaceCell` 続発への言及が消え、`store.replaceCell` の呼び出し自体も除去されている
- `SectionAccessoryRenderingTests.swift:589` — 「DSL の headerHeight preflight が発行する `.full` の適用先がこの経路になる」は**新契約でも正しい**記述のため残置で問題ない
- `DSLDiffCalculatorTests.swift:308`、`DSLDiffCalculator.swift:67` — いずれも新契約 (続発しない) を現在形で説明している

### tasks.md の虚偽チェック

| タスク | 判定 |
|---|---|
| 1.1 / 1.2 / 1.3 | ✅ |
| 2.1〜2.8 | ✅ |
| 2.9 | ✅ **解消**。前回 ⚠️ とした「表示レベルの両方反映は UI 層テストで検証する」に対応するテストが実在し、ミューテーションで検出力も確認済み |
| 3.1 | ✅ 検証者側で再実行し 624 件 / 0 failures |

### 逆流検査

`proposal.md` / `specs/` は前回検証以降 mtime に変化がなく (10:59:22 / 10:59:37)、修正サイクルで書き換えられていない。修正は `ios/` 配下のテスト・実装コメントに限定されている。**逆流なし**。

### 未記録乖離

なし。前回の ❌ 1 件は実装ではなくテストの追加で解消され、deviation として記録すべき差分は生じていない。

### ソースコメント規約

修正で触れた 3 ファイル (`SectionAccessoryRenderingTests.swift` / `FullSnapshotContentTargetsTests.swift` / `FullSnapshotContentTargets.swift`) を `comment_policy_rules.py` で直接検査し、いずれも clean (未追跡ファイルは `scripts/comment-policy-lint.py` の対象から落ちるため個別に適用した)。

## 判定

**VALID** — 全 Requirement / Scenario が「✅ 一致」。虚偽チェックなし、逆流なし、テスト全件成功 (624 / 0 failures)。
