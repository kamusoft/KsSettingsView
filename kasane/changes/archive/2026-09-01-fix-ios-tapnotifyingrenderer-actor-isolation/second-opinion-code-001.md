# セカンドオピニオン: fix-ios-tapnotifyingrenderer-actor-isolation (code-001)
**相方**: codex / **label**: so-code-fix-ios-tapnotifyingrenderer-actor-isolation / **日付**: 2026-09-01 / **対象**: HEAD からの working tree 差分 (ios/Sources 2 件・ios/Tests 3 件) と kasane/changes/fix-ios-tapnotifyingrenderer-actor-isolation/ 一式
---
# レビュー結果

**日付**: 2026-09-01  
**判定**: **APPROVED**

Critical 0 / Major 0 / Minor 1 / Suggestion 0

## サマリー

実装はデルタスペックを満たしており、`@MainActor` 分離、タッチフィードバック、Controller の所有関係、追加テストにブロッキングな問題は見つかりませんでした。`deviation.md` の記録内容も合意済み差分として妥当です。

## 指摘事項

### [🟡 Minor] 画面操作による Controller 解放確認を過大に表現している

**該当箇所**: `evidence/runtime-check.md:6`、`evidence/runtime-check.md:16`

**問題点**: root 画面へ正常に pop できたことは、ナビゲーション経路と画面状態の健全性は示しますが、`KsSettingsViewController` インスタンスが実際に解放されたことまでは証明しません。「Controller の解放は画面操作でしか通らない」「通る経路: Controller の解放」という記述は、観測できた範囲より強い主張です。実際の解放は `MemoryLeakTests` の weak 参照が nil になるアサーションで検証されています。

**推奨修正**: 手順 4 の経路を「Controller を含む画面の pop・破棄経路」などに変更し、解放自体は `MemoryLeakTests.test_Store経由でもControllerがdeinitされStore購読が解除される` で担保すると明記してください。

## 照合した規約

- `cross/comment-policy.md`（常時）
- `cross/test-execution.md`（テスト結果の報告）
- `cross/runtime-behavior-verification.md`（実行時挙動の完了判定）
- ios 固有 handbook は現時点で存在しないため適用なし
- `swift-ui-impl-skill` の Swift Concurrency・テスト・メモリ観点
- `kasane/concepts/ios/api/ios-native-host.md` の Store／Controller 所有関係

提示済みの Swift 6 ビルド成功、Simulator 644件・失敗0件、lint結果を検証事実として採用し、指示どおり再実行とファイル書き込みは行っていません。


## 突き合わせ結果

ホスト側レビュー ([review-001.md](review-001.md)、判定 CHANGES_REQUESTED / Major 1・Minor 4・Suggestion 2) と突き合わせた。
両者の指摘が矛盾した論点は無い。相方は APPROVED、ホストは CHANGES_REQUESTED と判定が割れたが、
割れの原因は Major-1 (押下解除テストが UIKit 実経路を通っていない) をホストのみが検出したことによる。

| 指摘 | 出典 | 採否 | 対応 |
|---|---|---|---|
| Minor: 画面 pop の証跡が Controller 解放の証明としては過大な表現 | 相方のみ (根拠強: 観測できた範囲と主張のずれを特定) | **採用** | `evidence/runtime-check.md` の手順 4 を「Controller を含む画面の pop・破棄経路」に改め、解放自体は MemoryLeakTests が担保すると明記 |
| Major: 押下解除テストが実経路を通っていない | ホストのみ (根拠強: 既存の実経路テストを特定、lessons/test.md L-001 に該当) | **確定** | 実装ワーカーへ差し戻し (実経路の往復検証へ作り替え) |
| Minor: `assumeIsolated` の根拠がコードに残らない | ホストのみ | **確定** | 実装ワーカーへ差し戻し |
| Minor: tasks 2.3 の Scenario 参照が spec と不一致 | ホストのみ | **確定** | ホスト側で spec 表記へ戻した |
| Minor: 残存 warning の「スコープ外」判定が検証不能 | ホストのみ | **確定** | 修正前コードとのビルド比較を実施し、メッセージ一覧とともに `evidence/swift6-build.txt` へ記録 |
| Minor: 押下ハイライトの視覚証跡の除外理由が未検証 | ホストのみ | **確定** | 押下保持中のキャプチャに成功し証跡を追加 (`evidence/05-press-highlight.png` / `06-press-released.png`) |
| Suggestion: MemoryLeakTests の名前と観測のずれ / 検出力の実測 | ホストのみ | **採用 (名前と観測のずれのみ)** | 実装ワーカーへ差し戻し。検出力のミューテーション実測は Major-1 の新テストについてホスト側で実施する |
| Suggestion: `isEnabled == false` のケースにテストが無い | ホストのみ | **採用** | Major-1 の作り替えに同梱 |

相方が Major を検出しなかった点について: 相方には同一の入力 (成果物・diff・検証結果) を渡しており、
テストの実経路性は `kasane/lessons/test.md` の昇格済みルールと既存テストの手本を突き合わせて初めて見える論点だった。
プロジェクト固有の昇格済みルールに基づく検出はホスト側の責務として扱う (kasane/lessons/process.md L-002)。
