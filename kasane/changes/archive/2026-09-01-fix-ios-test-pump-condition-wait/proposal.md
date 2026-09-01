# Proposal: fix-ios-test-pump-condition-wait

## Why

iOS テストの固定時間待機 (ヘルパ `pump` / `pumpEntry` とテスト本文の直接の `RunLoop.current.run(until:)` — 定義 20 個・呼び出し 206 箇所) が待機規約 (`kasane/handbook/cross/test-execution.md` 「収束を待つアサーション」) を満たしておらず、CI (GitHub Actions macos-26) で実際に flaky が顕在化した (`KsBridgeCustomCellTests` のリサイクルテスト。同一 commit で 1 回目失敗・2 回目成功)。検証 CI は必須 status check であり、放置すると PR のマージを不定期にブロックし続ける。

全数仕分けで、呼び出し 206 箇所は A: 収束待ち 160 / B: レイアウト駆動のみ 16 / C: 負の検証 30 に分かれることが確定している。仕分けの正は change 内の分類台帳 (triage.md)。

## What Changes

待機ヘルパを用途別に 3 つへ分離し、固定時間待機の呼び出し 206 箇所を全数置換、旧定義 20 個を消し切る:

- **条件ベース待機** (A 160 箇所): 遷移証拠の述語クロージャ + 実時間 deadline (単調時計・共通既定値) + ループ内で RunLoop を短く回す + 超過時は実測値付き fail。汎用 1 本 + 頻出述語の薄い便利関数
- **レイアウト実行のみ** (B 16 箇所): 待機なしで `setNeedsLayout` → `layoutIfNeeded` を走らせるだけのヘルパ
- **意図明示の固定待機** (C 30 箇所): 負の検証 (no-op・不達の確認) 専用。名前で不変性検証用と判別できる形 (ADR cross/0027)

あわせて、private コピーの散在を解消するためヘルパを 3 テストターゲット (`KsSettingsViewUITests` / `KsSettingsViewSwiftUITests` / `KsSettingsViewBridgeTests`) が依存する**共有ターゲット 1 つに集約する (単一定義が受け入れ条件。成立しない場合は実装を止めてユーザーへ報告し、黙って複数コピーへ切り替えない)**。handbook `cross/test-execution.md` の「収束を待つアサーション」節には負の検証の例外を追記する。

影響する能力: **ios-test-support** (新規 capability。iOS テスト共通の待機・ホスティング支援)

## Non-Goals

- **Android 側の同型修正**: 別 change [[fix-customcell-test-pump-condition-wait]] が担う (対象ビルドルートが別で独立に完結する)。待機規約の解釈 (ADR cross/0027) だけを共有する
- **プロダクトコード (`ios/Sources/`) の変更**: 今回の flaky はテストの待機方法の問題であり、実装側の非同期挙動は変更しない
- **CI workflow の変更**: 検証 CI の構成は add-verification-ci で確定済みで、本 change はテスト側だけで flaky を解消する

## Impact

- 破壊的変更なし (公開 API・プロダクトコードに触れない)
- 影響範囲は `ios/Tests/` 配下のテストファイルと Package.swift (共有ターゲット追加)、handbook 1 ファイル。テストの意味 (何を検証するか) は変えず、待ち方だけを変える
- リスク: 置換時に述語の選び違いでテストの検証内容が弱まる可能性 → 述語を「操作前には成立せず非同期反映後に初めて成立する遷移証拠」に限定する規則を spec に置き、分類台帳 (triage.md) を対応の正として全件 Simulator 実行 (実行件数併記) + 証跡保存で完了判定する
- 副次効果: 通常時の無駄待ち (最低 0.05 秒 × 202 箇所) が条件成立で即時解消され、スイートが速くなる見込み

## 級: M

置換 206 箇所・30 ファイル超・3 ターゲット + ヘルパ設計判断 + handbook 追記を含むため (公開 API 変更なし・可逆・UI なしなので L には届かない)。

domain: ios
