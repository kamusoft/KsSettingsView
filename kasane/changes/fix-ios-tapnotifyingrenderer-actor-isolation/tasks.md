# Tasks: fix-ios-tapnotifyingrenderer-actor-isolation

## 1. 行タップ通知プロトコルの分離適合

- [ ] 1.1 `TapNotifyingRenderer` の定義に `@MainActor` を付与し、準拠 11 件の警告が消えることを確認する (→ Requirement: Swift 6 言語モードでのビルド適合)
- [ ] 1.2 準拠 11 種 (Command / Button / Checkbox / Radio / SimpleCheck / Picker / NumberPicker / TimePicker / DatePicker / Entry / Custom) がすべて `TapNotifyingRenderer` として解決できることを検証するテストを追加する (準拠 extension の誤削除はビルドでは検出できないため) (→ Requirement: 行タップ通知とタッチフィードバックの挙動維持 / Scenario: 準拠 11 種すべてが行タップ通知の解決対象であり続ける)

## 2. タッチフィードバック closure の分離適合

- [ ] 2.1 `KsCellViewSupport.installSelectedColorHandler` の closure を `MainActor.assumeIsolated` で包み、`cellState` は Sendable な Bool (押下判定) に落としてから持ち込む (→ Requirement: Swift 6 言語モードでのビルド適合 / 行タップ通知とタッチフィードバックの挙動維持)
- [ ] 2.2 同 closure 内の無意味な条件 downcast (`listCell as? UICollectionViewListCell`) を通常の `guard let` に直す (スコープ内の同梱修正)
- [ ] 2.3 押下ハイライトの解除側 (highlighted true → false 遷移で平常時の実効背景色へ戻る) を検証するテストを追加する (既存テストは押下側の選択色適用しか見ていない) (→ Requirement: 行タップ通知とタッチフィードバックの挙動維持 / Scenario: 押下中はハイライト色になり離すと平常時の背景に戻る)

## 3. Controller deinit の明示解放処理の削除

- [ ] 3.1 `KsSettingsViewController.deinit` を、解放順序・Cycle 断ちを説明する既存コメントごと削除する (方針は提案時の実測と所有関係の確認で決着済み — proposal.md What Changes (3)) (→ Requirement: Swift 6 言語モードでのビルド適合 / Controller 解放の維持)
- [ ] 3.2 `disconnectStore()` 側の「deinit と両方に追記すること」という保守注意書きを、deinit 削除後の実態に合わせて更新する (→ Requirement: Controller 解放の維持)
- [ ] 3.3 MemoryLeakTests を強化する: Store 経路のテストで Controller 解放後の Store 操作について、クラッシュしないことだけでなく操作結果の状態 (Cell の増減) を assert する (→ Requirement: Controller 解放の維持 / Scenario: Store 経由でも Controller が解放され Store は使い続けられる)

## 4. 検証

- [ ] 4.1 `ios/Package.swift` に Swift 6 言語モードを一時設定してパッケージ全体をビルドし、エラーゼロを確認して設定を戻す (→ Requirement: Swift 6 言語モードでのビルド適合 / Scenario: Swift 6 言語モードでのビルド試行がエラーゼロで成功する)
- [ ] 4.2 iOS 全件テストを Simulator で実行し、実行件数と失敗ゼロを確認する (kasane/handbook/cross/test-execution.md の規約に従い件数を報告する) (→ Requirement: 行タップ通知とタッチフィードバックの挙動維持 / Controller 解放の維持)
