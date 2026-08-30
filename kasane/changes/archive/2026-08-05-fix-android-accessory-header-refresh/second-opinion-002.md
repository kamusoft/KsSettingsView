# セカンドオピニオン: fix-android-accessory-header-refresh (002 回目 — 対応: review-001 / code-review モード)
**相方**: codex / **日付**: 2026-08-05 / **対象**: 実装 diff (KsSettingsListAdapter.kt / KsSettingsView.kt / ListAdapterDiffTest.kt / FullUpdateContentSyncTest.kt)
---
# レビュー結果: fix-android-accessory-header-refresh

**日付**: 2026-08-05  
**判定**: APPROVED

## サマリー

仕様・ADR・合意済み deviation と実装の対応を確認しました。Critical / Major はなく、Section H/F の差分検出、full 更新後の Cell rebind、旧・新 visible intersection の制約はいずれも満たしています。

ホスト実施済みテスト結果（1964件成功、失敗0）を前提としています。レビュー中のビルド・テスト実行およびファイル書き込みは行っていません。

## 指摘事項

### [🟡 Minor] 非同期待機のタイムアウトがテスト失敗にならない

**該当箇所**: [android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FullUpdateContentSyncTest.kt:53](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FullUpdateContentSyncTest.kt:53)

**問題点**: `awaitDifferCommit` は5秒経過後に main Looper を一度処理するだけで、`condition` が成立しなくても正常に戻ります。多くのテストは後続アサーションで失敗しますが、通知がないことを検証するテストでは、コミット自体が完了しなかった場合にも成功する余地があります。また、`Thread.sleep` による実時間ポーリングは実行環境による不安定さを持ち込みます。

**推奨修正**: タイムアウト後に少なくとも `assertTrue("AsyncListDiffer のコミットがタイムアウトした", condition())` を実行してください。可能なら Robolectric の executor 制御または明示的な commit callback を使い、実時間待機を排除してください。

## アクションプラン

1. `awaitDifferCommit` のタイムアウトを明示的なテスト失敗にする。
2. 将来的に実時間ポーリングを決定論的な非同期処理へ置き換える。

**件数**: Critical 0 / Major 0 / Minor 1 / Suggestion 0


## 突き合わせ結果

ホスト側 review-001.md (CHANGES_REQUESTED: Major 1 / Minor 3 / Suggestion 2) との突き合わせ:

| 指摘 | 採否 |
|---|---|
| codex Minor「awaitDifferCommit がタイムアウトで失敗しない」 | **確定** (ホスト Minor 2 と双方一致。ホスト側はさらに Major の増幅要因として実測で裏付け) |
| ホスト単独: Major (positive テスト2件のトートロジー)・Minor (itemCount==5 誤り)・Minor (headerHeight 漏れ)・Suggestion ×2 | ホスト側指摘としてそのまま採用 (ミューテーション実測・テスト XML 所要時間の根拠付き) |

矛盾 (割れた論点): なし。未解決: なし。
