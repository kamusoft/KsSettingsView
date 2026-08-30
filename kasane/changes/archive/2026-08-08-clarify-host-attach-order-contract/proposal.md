# Proposal: clarify-host-attach-order-contract

## Why

「Host 生成 → 構造操作 → view 階層へ取り付け」の順序で iOS が view load 前の構造 Diff を取りこぼすことが検証ホストで実測された。コード調査で原因は iOS 実装ギャップと確定 — `KsSettingsViewController` は viewDidLoad 前の非 full Diff を内部 root にすら反映せず破棄し、viewDidLoad では Store を再 pull せず init 時キャプチャの root から構築する。一方 Android は `onAttachedToWindow` の `resyncFromStore` が Store 現在状態を pull して全復元するため同順序でも安全で、両 OS の観察可能挙動が非対称になっている。MAUI Handler はプロパティマッパー適用が view 階層への追加より必ず前に完結する順序で動くため、この隙間を構造的に踏む。core/ADR-0019 (proposed) で「Host は view load / attach 時に Store 現在状態から表示を復元する」契約を決定した。

## What Changes

- iOS `KsSettingsViewController.viewDidLoad` を「接続中 Store の現在状態 (root / theme) を pull して構築する」形に改修する (Android の `resyncFromStore` パターンへの対称化)
- Android はコード変更なし。現行実装が既に契約を満たすことを回帰テストで固定する
- 「Host は view load (iOS) / window attach (Android) 時に Store 現在状態から表示を復元する」を両 OS 共通の契約としてテストで保証する。復元対象は Store が現在状態として保持するもの (構造・Cell 内容・Section accessory・theme) に限定し、Root header / footer は対象外 (所有者が attach/load 後に適用する責務) — 詳細は design.md の Decision 1〜4
- Store 接続中の theme は Store を正とし、iOS の公開 API `applyTheme` (直接適用) の Store 接続中の併用は非保証と契約化する
- 影響 capability: ios-host / android-host

## Non-Goals

- view load / attach 前に届いた Diff のイベントとしての保全 (保証するのは最終状態への収束のみ。attach 前の中間状態に依存する利用は引き続き保証しない)
- MAUI Handler の実装 (phase-2 本体の責務。本変更はその前提を Native 側で塞ぐ)
- Store 側の変更 (Store の pull 型復元保証は現行のままで足りる)

## Impact

- iOS Host の観察可能挙動が2点変わる: ①view load 前の構造操作・Section accessory 更新・theme 変更が「消える」→「load 時に反映される」(改善方向。既存の正常系利用には影響しない)。②Store 接続中に直接 `applyTheme` した Theme は view load 時に Store theme で上書きされる (併用は元々未定義挙動)
- Root header / footer は復元対象外として契約に明記される (Store が現在状態を保持しないため。core/ADR-0005 の責務分離の帰結)
- Android は挙動不変 (テスト追加のみ)
- Store なし init (プレビュー/テスト用) のフォールバック整合に注意が必要

## 級: L

ios-host / android-host の2能力にまたがる共通 Host ライフサイクル契約の確定で、復元対象・Theme 優先順位・収束境界の設計判断を伴うため (design.md の Decision 1〜4)。

domain: core
