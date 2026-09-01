# Proposal: fix-ios-tapnotifyingrenderer-actor-isolation

## Why

iOS のビルドで、行タップ通知プロトコル (`TapNotifyingRenderer`) への各 CellView の準拠が「main actor 分離コードをまたぐ」という Swift concurrency 警告が 11 件出ている。警告文が明示するとおり Swift 6 言語モードではエラーになるため、toolchain / 言語モード更新時の必須対応を先に潰す。

探索時の Swift 6 言語モード試行 (exploration.md) で、エラーになる箇所は本命を含め 3 群で全部と確認済み。3 群すべてを本 change で解消する (隣接課題は同じ change で直す)。

## What Changes

影響する能力: **ios-host** (UIKit Host 内部)。挙動の変更はなく、Swift 6 言語モード適合が新たな契約になる。

1. **行タップ通知プロトコルの main actor 分離**: `TapNotifyingRenderer` の定義に `@MainActor` を付与し、準拠 11 件の警告を解消する (探索で実証済み)
2. **タッチフィードバック closure の分離適合**: `KsCellViewSupport.installSelectedColorHandler` の `configurationUpdateHandler` closure を `MainActor.assumeIsolated` で包み、`cellState` は Sendable な Bool に落として持ち込む (探索で実証済み。handler は UIKit が main thread で呼ぶ契約)
3. **Controller deinit の明示解放処理の削除**: nonisolated deinit から main actor 分離プロパティへ直接触れている解放処理 (購読 cancel・dataSource/delegate 解除等 8 エラー分) を、コメントごと削除する。提案時の実測 (deinit を削った状態で MemoryLeakTests 2 件 pass) と所有関係の確認で、明示解放が不要なことは決着済み: 購読 (AnyCancellable) は解放時に自動 cancel、`UICollectionView.dataSource`/`delegate` は weak、Store 購読・cellProvider・layout の closure はすべて `[weak self]` 捕捉で参照サイクルが存在しない (そもそもサイクルがあれば deinit 自体が走らないため、deinit 内でサイクルを断つという既存コメントの前提は成立しない)。`disconnectStore()` との「両方に追記」保守注意書きも不要になるため合わせて整理する
4. **適合維持の仕組み (蒸留への申し送り)**: 「iOS の source ターゲットを触る変更は、完了判定の前に Swift 6 言語モードの一時設定ビルドでエラーゼロを確認する」を iOS ドメインの handbook 規約として追加する (`kasane/handbook/ios/` を新設。確認手順は本 change の tasks 4.1 と同じ)。handbook への追加は実装タスクに入れず、蒸留 (ksn-distill) で行う — これにより後続変更が Swift 6 エラーを再導入しても手順で検出される (セカンドオピニオン Major 指摘への対応。CI ゲートは採らない)

## Non-Goals

- **Swift 6 言語モードへの恒久切替 (`ios/Package.swift`)**: toolchain 更新のタイミングで行う運用判断のため対象外。本 change の到達点は「source 4 ターゲットが Swift 6 言語モードでエラーゼロ」の状態まで (パッケージ全体の切替可否ではない)
- **テストターゲットの Swift 6 適合**: 探索の build 試行では対象外で未計測。言語モード切替時に露出したものを対応する (source targets と異なり出荷物でなく、修正が機械的な見込みのため切替と同時で足りる)
- **deprecated `supplementariesFollowContentInsets` (iOS 16.0) の警告**: 代替 API の挙動差確認を要する別課題のため対象外。なお同じく concurrency 外の警告である無意味な条件 downcast は、(2) の修正で同じ行を触るため本 change のスコープ内タスクとして含める (Non-Goal ではない)

## Impact

- 破壊的変更なし (触るのはすべて internal。公開 API 変更なし)
- 挙動不変が原則: 行タップ通知・タッチフィードバック・Controller 解放 (リークなし) を既存テストで担保する
- リスクは (3) の deinit 削除に集中する。将来 `[weak self]` でない捕捉や強参照の所有が持ち込まれるとリークが再発し得るため、MemoryLeakTests を回帰の安全網として強化する (押下ハイライト解除の復元検証と Store 解放後の状態検証も薄い箇所として合わせて足す)

## 級: M

(1)(2) は実証済みの機械的修正だが、(3) が deinit の解放設計の判断 (提案時に実測で決着) とリーク検証の強化を伴うため。

domain: ios
