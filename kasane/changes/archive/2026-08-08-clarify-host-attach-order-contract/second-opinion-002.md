# セカンドオピニオン: clarify-host-attach-order-contract (code-review / review-001 対応)
**相方**: codex / **日付**: 2026-08-08 / **対象**: 未コミット working tree 変更全体 (git diff HEAD + 未追跡ファイル)
**注**: second-opinion-001.md は提案段階 (spec-review) の証跡。本ファイルは実装レビュー (review-001) 対応

---

# レビュー結果: clarify-host-attach-order-contract

**日付**: 2026-08-08  
**判定**: **APPROVED**

## サマリー

iOS の Store 再同期は snapshot 構築前に行われ、Store 非接続時の従来経路も維持されています。両 OS の回帰テスト、検証ホスト、E2E 証跡はデルタスペックの各 Scenario と整合しており、Critical / Major はありません。

指摘件数: Critical 0 / Major 0 / Minor 1 / Suggestion 0

## 指摘事項

### [🟡 Minor] Android テストが実時間待機に依存している

**該当箇所**: [android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AttachOrderRestoreTest.kt:81](<android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AttachOrderRestoreTest.kt:81>)

**問題点**: `idleUntilQuiet()` が `Thread.sleep(5)` を30回繰り返しています。読み込まれた Kotlin テスト規約では `Thread.sleep` は禁止されており、この実装も実際には「quiet」を検出せず固定150ms待つだけです。低速なCIでは不足して flaky になり得る一方、通常時にも不要な待機が発生します。

**推奨修正**: `AsyncListDiffer` のコミット完了を観測できるテスト用フック、同期 executor を注入した `AsyncDifferConfig`、または Adapter の更新通知を使った条件ベースの同期へ置き換えてください。

## 確認結果

- `viewDidLoad` で Store の root / theme を再取得してから初期 snapshot を構築している
- iOS の全6 ScenarioとAndroidの全2 Scenarioに対応するテストがある
- `tasks.md` の完了チェックに未実装項目は見当たらない
- proposal / design / specs の無断変更および未記録の仕様逸脱はない
- E2E画像は修正前後、Root accessory除外、Android収束の説明と一致する
- コメント規約 lint と whitespace 検査に違反はない
- ビルド・テストは依頼文記載の結果を採用し、再実行していない

## アクションプラン

1. Androidテストの固定時間待機を、完了条件に基づく同期へ置き換える。
2. 変更後にAndroidテスト全件を再実行する。

## 突き合わせ結果 (ksn-orchestrator)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| Android テスト `idleUntilQuiet()` の固定時間待機 (`Thread.sleep(5)×30`) | 相方のみ | **採用** | kotlin-impl-skill references/testing.md がテストでの `Thread.sleep` (実時間待ち) をアンチパターンと明記。flaky 実害シナリオも具体的。条件ベース同期へ修正する。なお既存テスト4ファイル (AdapterReattachTest 等) にも同パターンが既存するが本変更スコープ外 (別途改善候補) |
| `maui/README.md` 既知の制約の陳腐化 | ホストのみ (review-001 Minor 1) | 確定 (蒸留送り) | docs-refresh スキル専権のため実装フェーズでは対応しない |
| `resyncFromStore()` の projection 更新の `applyFullSnapshot` 暗黙依存 | ホストのみ (review-001 Minor 2) | 確定 (コメント明記で対処) | 指摘どおり現状実害なし。順序契約をコメントで自己文書化する |

未解決 (両者矛盾): なし。両判定 APPROVED で一致。
