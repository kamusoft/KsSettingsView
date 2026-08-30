---
id: 0018
title: view accessory の更新は明示経路のみを正とし、内容変化は live view の直接描画と wrapper の invalidation 中継で追従する
status: accepted
date: 2026-08-12
---

## Context

Core 契約では accessory の view 同士は `KsAnyView` の中身を比較せず「同じ case なら等価」とする (concepts/core/core-model/settings-tree.md)。このため **Core の値等価だけ**では view accessory の差し替えを検出できない (Android UI の DiffCallback は `KsAnyView` の参照同一性比較を独自に持ち別インスタンスへの差し替えを検出できるが、これは UI 層のローカルな実装であり OS 共通の契約上の保証ではない)。一方、Store の `updateAccessory` (iOS `SettingsRootStore.swift:274`) には同値スキップのガードが無く、明示的な呼び出しは必ず Diff として UI 層へ届くことを実装で確認した — OS 共通に保証されるのはこの明示経路である。

また [ADR-0016](0016-mauiview-materialization-self-measuring-wrapper.md) により、native へ渡る platform view は MAUI binding が直接更新する生きたオブジェクトであり、AiForms のような「view を作り直して貼り直す」構造ではない。

## Decision

- **差し替え (新しい VisualElement インスタンスの設定)**: facade がプロパティ変更を検知して再実体化し、**必ず明示経路 (`updateAccessoryView` → Store `updateAccessory`) で再発行**する。値比較に乗る経路 (`replaceSection` 等) を view accessory の変更輸送に使ってはならない。
- **内容変化 (同一インスタンスの内部変化)**: Store / Bridge へ**何も再発行しない**。live platform view が直接描画更新される — 「view accessory は参照が正、内容は live」。
- **サイズ変化の伝播**: native の行/領域高さ再計算まで届けるのは ADR-0016 の wrapper の invalidation 中継の責務。Android は `requestLayout` → RecyclerView の再 measure で追従する (要求は行 container ではなく hosted view へ出す — container だけでは子の計測キャッシュが再評価されない)。iOS の UICollectionView self-sizing は、内在サイズ無効化や `setNeedsLayout` だけでは高さを測り直さず、**対象 supplementary 限定の `invalidateLayout(with:)` の発行で初めて追従する**ことを実装フェーズの段階実測で確定した。このため native に再計算口 `invalidateAccessoryMeasurement(target)` を追加する — Store は一過性通知 (`SettingsRootDiff` に case を足さず復元可能状態も変えない。`replaceCells` の内容更新バッチと同型)、iOS Controller は対象限定の invalidation context で `invalidateLayout(with:)` (headerHeight 固定の Section では無害な no-op)、Android は hosted view への `requestLayout()` (実測上は不要だが API を対称にして facade 側の OS 分岐を消す)、Bridge は素通し、facade が wrapper の `MeasureInvalidated` を slot ごとに合体してから呼ぶ。native の `KsAnyView` accessory でも同じ問題が起きる native 一般のギャップの解消 (プラットフォーム間パリティ整備) を兼ねる。

## Alternatives Considered

- **内容変化も再発行する (AiForms 型の descendant 購読 + デバウンス)**: AiForms は「view を作り直して貼り直す」構造だから再発行に意味があるが、live view + 自己計測 wrapper の構造では再発行するものが無い。リフレクションによる descendant 購読 (ADR-0016 で不採用としたハック) の復活にもなる。却下。
- **`KsAnyView` に世代トークンを追加して等価比較へ参加させる**: 両 OS の Core 契約変更であり「XAML 都合の native 変更はしない」方針に抵触。等価比較テストの改修も重い。却下。

## Consequences

- 正: 等価比較の契約 (view は case 等価) を変えずに、変更検出の確実性を明示経路の実証済み挙動で担保できる。
- 正: CustomCell content の更新セマンティクスも同じ原則 (live view + 明示経路) を再利用できる。
- 負: 「view の変更は明示経路のみ」という規約を facade 実装が守り続ける必要がある (値比較経路に view 変更を流すと無言で欠落する)。
- 正: 実装フェーズの検証で iOS のサイズ変化伝播の未確証は解消し、再計算口は native の `KsAnyView` accessory 利用者の同一ギャップも閉じた (出典: 実装結果)。
- 負: 一過性通知 API (`invalidateAccessoryMeasurement`) が Store / Controller / Host / Bridge の公開面に増える (Store の復元可能状態には含まれない)。連続発火の合体は呼び出し側 (facade) の責務として残る。

出典: phase 議論 2026-08-11 (kasane/roadmaps/maui-support/phases/phase-6-accessory-views/history.md、Store 同値スキップ無しの実証と iOS self-sizing の未確証を含む) / 実装結果 2026-08-12: kasane/changes/archive/2026-08-12-add-maui-accessory-views/ の deviation.md (案A 採用の裁定) と tasks 1.1 の段階実測 (ios/Tests/KsSettingsViewUITests/AccessoryViewLiveProbeTests.swift)
