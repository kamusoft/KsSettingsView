# レビュー結果: fix-ios-replace-cell-type-change (001 回目)

**日付**: 2026-08-13
**判定**: APPROVED

## サマリー

合意済みスコープどおり、部分更新経路 2 本 (`applyReplaceCell` / `applyContentUpdateBatch`) に
`FullSnapshotContentTargets.compute` と同一基準の `type(of:)` 比較が入り、型変化 Cell だけが
`reloadItems` へ振り分けられている。Simulator 全件 480 tests / 0 failures、修正を外したミューテーション
probe で報告どおりの dequeue 例外を両経路とも再現でき、修正が原因に効いていることを実測で確認した。
指摘は回帰テスト 1 箇所のアサーション検出力に関する Minor 1 件のみで、修正本体に問題は見つからなかった。

## 検証したこと

### ビルド・テスト (test-execution.md 準拠)

```
cd ios && xcodebuild test -scheme KsSettingsView-Package \
  -destination 'id=<ios-simulator-udid>'   # iPhone 17 Pro / iOS 26.0
```

- **Executed 480 tests, with 0 failures** / `** TEST SUCCEEDED **` / warning 0 件
- 新規 `ReplaceCellTypeChangeTests` の 4 件が全て実行され passed
- `python3 scripts/comment-policy-lint.py` — 禁止 0 件 (要確認 2 件はいずれも本変更が触っていない既存行)

### ミューテーション probe (lessons/code-review.md L-001)

修正箇所のみを修正前の実装に戻し (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`
のバックアップを取得 → probe 後に復元、shasum `152aeed2...` の一致で原状復帰を確認)、新規テストだけを実行した。

| probe | 結果 |
|---|---|
| 修正を外す (両経路とも無条件 `reconfigureItems`) | 型変化を含む 3 件が `Attempted to dequeue a cell for a different registration or reuse identifier ... Dequeued reuse identifier: SwitchCellView; Original reuse identifier: LabelCellView` で異常終了。型不変の 1 件のみ passed |
| 常に `reloadItems` させる | 単発経路の型不変テスト (`:115`) が期待どおり fail。バッチ経路の同種アサーション (`:152`) は **passed** (下記 Minor) |

1 つ目の probe は、報告された症状 (exploration に記載の `LabelCell → SwitchCell`) が両経路で
再現すること、および新規テストがその症状を検出できることの直接の証拠になっている。

### 実装面で確認した観点

- 判定基準が `FullSnapshotContentTargets.compute` (`type(of: oldCell) != type(of: cell)`) と同一で、
  full 経路の既存設計と整合している
- バッチ経路は `self.root = store.root` / `rebuildModelIndexes()` より **前** に旧 Cell を退避しており、
  退避順序の要件を満たしている。重複 cellID を含むバッチでも退避値は更新前のもので一定
- `reconfigureTargets` と `reloadTargets` は排他に構築されるため、同一 item を同じ snapshot で
  reconfigure と reload の双方に指定して例外になる経路はない。両方空でない混在ケースと
  reload のみのケースの双方にテストがある
- 単発経路は型比較を `#available(iOS 15.0, *)` の **前** に置いており、iOS 14 以下でも型変化は reload になる
- 他の部分更新経路に同種の穴は残っていない: `applyReplaceSection` は `applyFullSnapshot` に
  フォールバックし `FullSnapshotContentTargets` 側で型変化を処理する。Theme 再適用 (`:400`) と
  `applyInsertCell` / `applyMoveCell` は同一 ID の型変化を起こさない
- MAUI からの報告経路 (`KsSettingsBridge.replaceCells` → `store.replaceCells` → バッチ経路、
  `KsSettingsBridge.replaceCell` → 単発経路) はいずれも修正後のコードに乗る
- コメントは自己完結しており、変更提案 ID・フェーズ番号等の禁止参照を新たに増やしていない
- テストファイル内の `hostController` / `pump` / `renderedTitles` の private 重複は、この
  テストターゲットの既存慣行 (同種ヘルパを持つファイルが 7 件以上) に沿っており指摘しない

## 指摘事項

### [🟡 Minor] バッチ経路の「型が変わらない行は再構成される」アサーションに回帰検出力がない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/ReplaceCellTypeChangeTests.swift:152`

**問題点**: `thirdRowBefore === cv.cellForItem(at: IndexPath(item: 2, section: 0))` は、
`applyContentUpdateBatch` を「対象を常に `reloadItems` する」実装に変えても **passed のまま**だった
(上記 probe で実測)。reload された行の Native cell は再利用プールを経由して同じインスタンスが
返り得るため、複数行を同時に reload する状況では `===` が過剰 reload の検出信号にならない。
同じ意図の単発経路のアサーション (`:115`) は同じ probe で正しく fail しており、こちらだけが空振りしている。

修正本体の挙動は正しい (実装は型不変行を reconfigure に振り分けている) ため実害は現時点でなく、
「バッチ経路が過剰 reload に退行しても気づけない」という将来の検出漏れのみが問題。

**推奨修正**: 次のいずれか。
- 型不変 Cell **1 件だけ**を対象とするバッチのテストを足し、そこで `===` を検証する
  (単発経路の probe 結果から、同時 reload される同型行が無い状況ではこの信号が機能すると分かる)
- 検出力がないと分かった以上、アサーションを残すなら「保護している」と読めない形にする
  (削除、または検出力の限界をコメントで明示する)

## アクションプラン

1. (任意) Minor 1 件: バッチ経路の過剰 reload を実際に検出できる形へアサーションを補強、
   または検出力の限界を明示する。修正本体の再レビューは不要
