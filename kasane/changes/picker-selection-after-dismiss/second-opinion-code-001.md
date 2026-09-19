# セカンドオピニオン: picker-selection-after-dismiss (code-001)
**相方**: codex / **label**: so-code-picker-selection-after-dismiss / **日付**: 2026-09-19 / **対象**: 作業ツリーの未コミット差分 46 ファイル (iOS / Android / MAUI / iOS binding) + deviation.md + evidence/integration-host/
---
# レビュー結果: picker-selection-after-dismiss

**判定**: APPROVED  
**指摘件数**: Critical 0 / Major 0 / Minor 0 / Suggestion 0

## サマリー

4 本のデルタスペック、設計、deviation、HEAD 基準の未コミット差分 46 ファイルを照合しました。Changed → Completed の順序、非確定 dismiss の不発火、全構築経路での callback 保持、Bridge の正規化・破棄後の不到達、MAUI の書き戻しと `SelectedCommand` の責務分離は、実装とテストで満たされています。

`deviation.md` の合意済み差分と、tasks 6.1・7.3 の実機担当分は判定から除外しました。core/ADR-0034 は proposed のため判定根拠にはせず、衝突がないことだけ確認しています。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（常時）
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/runtime-behavior-verification.md`
- `kasane/handbook/cross/local-development-setup.md`
- `kasane/handbook/ios/swift6-language-mode-check.md`
- `kasane/handbook/maui/integration-host-verification.md`
- maui/ADR-0002・ADR-0012
- `kasane/lessons/code-review.md`

## 指摘事項

なし。

## 証跡確認

`evidence/integration-host/` の README と両 OS の画像を実際に確認しました。iOS Simulator / Android Emulator とも、単一・複数選択の値反映と「完了通知: 1 回」が提出内容に対応しており、個人情報の写り込みもありません。

ビルド・テストは依頼どおり再実行していません。提示された全件成功結果と、`git diff --check` が問題なしであることを判定に使用しました。



## 突き合わせ結果 (ホスト側 review-001.md との照合、2026-09-19)

- 相方: APPROVED (指摘 0 件) / ホスト: CHANGES_REQUESTED (Minor 1・Suggestion 2)
- **確定 (ホストのみ・根拠強)**: `android/.../ui/KsSettingsView.kt` の構成変更後カレンダー復元経路 (deviation の付随修正) に `onValueCompleted` の回帰テストが無い — 該当箇所と実害シナリオ (回転をまたいだときだけ通知が落ちる退行) が特定されているため修正サイクルへ
- **降格 (ホストのみ・Suggestion)**: MAUI の Handler 切断後テストの形、iOS の肯定側順序テストが「提示なし → completion 即時」分岐を通る点 — いずれも否定側テストと統合ホスト証跡で補完済みの所見として記録のみ
- 未解決: なし。相方 APPROVED は L-002 のとおり証明とは扱わない
