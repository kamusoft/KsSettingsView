# セカンドオピニオン: fix-ios-full-content-refresh (002 回目 — 実装レビュー並走。対応ホスト側: review-001)
**相方**: codex / **日付**: 2026-08-06 / **対象**: 実装 diff (working tree、ios/ 6 ファイル)
---
# レビュー結果: fix-ios-full-content-refresh

**日付**: 2026-08-06  
**判定**: **CHANGES_REQUESTED**  
**件数**: Critical 0 / Major 1 / Minor 0 / Suggestion 0

## サマリー

対象選定 helper と `applyFullSnapshot` の実装は、旧・新 visible projection の共通要素だけを値比較し、同型は reconfigure、型変更は reload に分けており、仕様と整合しています。型変更を reconfigure から外す判断も、同一型のセルを返す必要があるという [Apple の API 契約](https://developer.apple.com/documentation/uikit/nsdiffabledatasourcesnapshotreference/reconfigureitems%28withidentifiers%3A%29?changes=latest_major&language=obj_5) に沿っています。

ただし、明示された統合 Scenario に対応する表示テストがなく、完了済みになっているタスクと実態が一致しません。

## 指摘事項

### [🟠 Major] headerHeight と Cell 内容の同時変更を新しい単一 `.full` 経路で表示検証していない

**該当箇所**:

- [kasane/changes/fix-ios-full-content-refresh/tasks.md:19](kasane/changes/fix-ios-full-content-refresh/tasks.md:19)
- [ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift:327](ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift:327)
- [ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:629](ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:629)

**問題点**:  
タスク 2.9 は「headerHeight と Cell 内容の両方が表示へ反映されることを UI 層で検証する」として完了済みですが、変更された DSL テストは `.full` が新しい Cell model を運ぶことまでしか確認していません。

既存の表示テストは旧契約の `.full → .replaceCell` を手動適用しているため、今回の契約である「`.full` 1件だけで高さと内容が反映される」ことを保証しません。新規 `FullSnapshotContentRefreshTests` の複合テストも header text の変更であり、Section reload 経路になるため、headerHeight の layout invalidation と Cell reconfigure が同時に働く Scenario とは異なります。

**推奨修正**:  
既存の表示テストを、同一 header・同一 Cell ID のまま `headerHeight` と Cell title を変更した rootへ、`.full` 1件だけを適用するテストへ更新してください。適用後に次を確認します。

- header の実高さが新しい値になる
- Cell title が新しい値になる
- 追加の `.replaceCell` を発行・適用しない
- 可能なら Renderer の呼び出し回数を計測し、内容再適用が一度だけであることも固定する

## アクションプラン

1. 上記の単一 `.full` 表示統合テストを追加または既存テストから更新する。
2. 旧契約 `.full → .replaceCell` を説明するテスト名・コメントを現行契約へ修正する。
3. 指定の全 iOS テストを再実行する。

指定に従い、ビルド・テストの再実行およびレビュー結果ファイルの書き込みは行っていません。提示された全623テスト成功を前提に判定しました。

## 突き合わせ結果 (2026-08-06)

ホスト側 review-001 (CHANGES_REQUESTED: Minor 2 / Suggestion 2、verify-001: INVALID ❌1) との突き合わせ。

| 指摘 | 採否 | 対応 |
|---|---|---|
| codex Major「単一 `.full` での高さ+内容の表示統合テスト欠落」 | **確定** (ホスト Minor 2件と同根で双方一致。重要度は高い方の Major を採る) | 単一 `replaceAll` で headerHeight + 同一 ID Cell 内容を同時変更し表示 (header 実高さ + title) を検査するテストを追加。旧契約 (`.full` → `.replaceCell`) を現行仕様として説明する SectionAccessoryRenderingTests.swift:629-672 を現行契約へ書き直す |
| ホスト Suggestion 1 (FullSnapshotContentTargets の guard 順序の意図コメント) | 採用 (1行で解消) | 順序が意図的であることのコメント追記 |
| ホスト Suggestion 2 (`#available(iOS 15.0, *)`) | 見送り | レビュアー推奨どおり既存3箇所との一貫性を優先。別変更で一括整理 |

確定 1 (Major、ホスト Minor 2件を包含) / 採用 1 (Suggestion) / 降格 0 / 未解決 0
