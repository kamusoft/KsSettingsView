# Exploration: ios26-5-supplementary-recycle-test-timeout

## 課題 / 動機

- **事象**: iOS 26.5 Simulator でのみ、`ios/` のテスト 3 件が「画面外へ送った header supplementary が回収される」の 3.0 秒 deadline 超過で落ちる。`KsSettingsViewBridgeTests.KsBridgeAccessoryViewTests.test_リサイクルを挟んだ再表示が失敗しない` (2 アサーション) と `KsSettingsViewUITests.AccessoryViewDetachDiagnosticTests.test_退役した旧viewは明示的に剥がすまでview階層に残る`。iOS 26.1 (iPhone 17 Pro) では全件 0 失敗
- **発見の文脈**: change `ios-swiftui-representable-safe-area` の実装 (2026-09-19) で全テストを iOS 26.5 Simulator で実行した際に検出。両テストターゲットは `KsSettingsViewSwiftUI` に依存しておらず、当該 change とは無関係と切り分け済み
- **見立て**: iOS 26.5 の UIKit が可視矩形外の supplementary view を保持し続ける挙動差。handbook `cross/test-execution.md` の「framework が決める値を待機条件の代理にしない」に該当する可能性があり、待機条件の取り直しが要る

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- **未探索 (簡易起票)**
- 26.5 で supplementary が回収されないのは UIKit の挙動差 (仕様) か、ライブラリ側の参照保持か
- テストの待機条件を何に置き換えるか (回収を待つのではなく、明示的な剥がし後の状態を検査する等)
- CI が使う Simulator ランタイムの版と、iOS 26.5 を検証対象に含めるか

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定

暫定 S (テストの待機条件の修正のみなら)。ライブラリ側の参照保持が原因なら M。
