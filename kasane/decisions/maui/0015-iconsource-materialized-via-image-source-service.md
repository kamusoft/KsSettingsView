---
id: 0015
title: CellBase.IconSource は MAUI image source service で非同期に実体化し platform 画像を輸送する
status: accepted
date: 2026-08-10
---

## Context

Cell の icon を MAUI からどう渡すか。native は `KsImage` の platform case (`uiImage(UIImage)` / `Drawable(Drawable)`) を既に持つ。原典 AiForms は `ImageSourcePartLoader` + `SetImageSource` で `ImageSource` を platform 画像へ非同期解決する機構を持ち、機構の実在と規模は原典実装で確認済み。ただし本ライブラリの Cell は Handler を持たない (logical tree に載らない) ため、handler 前提の原典 loader は直接使えない。

## Decision

- `CellBase.IconSource` (`ImageSource?`、AiForms 互換命名) を公開し、MAUI 標準の image source service (`IImageSourceServiceProvider`) で非同期に platform 画像 (iOS `UIImage` / Android `Drawable`) へ解決する。解決完了時に該当 Cell の内容更新 (dirty → `replaceCell`) として Bridge DTO の platform 画像フィールドへ載せ、native は既存の `KsImage` platform case で受ける (**native 変更なし**)。
- 解決は `KsSettingsController` が所有し、MauiContext は `SettingsViewHandler` から controller 経由で供給する。Handler 未接続 (MauiContext なし) の間は解決を保留し、接続時にまとめて解決する。
- 競合は**解決要求ごとの単調増加トークンで latest-wins**。Handler (MauiContext) の世代も競合判定に含める — 切断時は進行中の解決をキャンセルして結果を破棄し、再接続時に現行 `IconSource` を新しい MauiContext で再解決する。解決失敗は icon なしとして確定し、次の `IconSource` 変更で再試行される。
- 解決結果は画像と後片付けの口を一体で持つリース (`KsImageLease`) として保持し、置換・登録解除・解決口切り替えで破棄する (`IImageSourceServiceResult` の破棄契約の履行)。`ReleaseHost` ではリースを破棄しない — ADR-0007 により Host 解放後も Store 状態から表示が復元され、Bridge DTO が解決済み画像を保持し続けるため。

## Alternatives Considered

- **接頭辞付き platform プロパティ (`IOSIconSystemName` / `AndroidIconResource`)**: 輸送は薄いが、MAUI アプリの画像資産 (MauiImage 等) が使えず利用者価値が低い。`ImageSource` が MAUI の慣例 ([ADR-0004](0004-maui-idiomatic-types-for-styling.md) の精神) であるためオーナー判断で却下。
- **icon 非対応**: サンプル2ページ (BasicCells / UnifyCellCommonFields) の完全一致が崩れ、Theme をスコープに含めた判断と非整合のため却下。

## Consequences

- 正: MAUI アプリの画像資産がそのまま Cell の icon に使える。native 契約 (`KsImage`) に触れない。
- 正: 「MAUI 側の非同期実体化 → 解決済み platform 値の輸送」は phase-5 (CustomCell)・phase-6 (Header/Footer 任意 View) の MauiView 実体化の先例になるパターン。
- 負: 非同期解決に伴う競合管理 (要求トークン + 解決口世代) とリース破棄管理が facade 内部に入る。
- 負: 既知の残課題 — (1) 置換・除去時のリース破棄が native への反映より先に走る窓がある (image loader 経路 = Uri / Stream のみ実害の可能性、file / resource 経路は破棄が no-op)。(2) 複数リースが同一 platform 画像インスタンスを包んだ場合の共有破棄。(3) ページを恒久的に離れて再訪問しない場合、リースにファイナライザが無いため後片付けは走らない。いずれも後続で追跡する (出典: 実装結果 review-002 Minor-10・保留(b))。
- 追記 (2026-08-22): 上記 (1) は**解消済み** — 置換 (`StoreIcon`)・Cell 除去 (`UnregisterCell`)・Root 再構築 (`ClearRegistrations`) の 3 経路とも退役キュー経由となり「native への配信 → 破棄」の順序へ移行している (実装は add-maui-basic-input-cells 内)。順序を固定する回帰テストが `IconSourceTests` に 4 本あり、3 経路を即時破棄へ戻すミューテーションで当該 4 本だけが落ちることを実測で確認した。(2) は investigate-maui-icon-lease-sharing で追跡する。(3) は未解決のまま (出典: 実装結果 fix-maui-icon-lease-disposal-ordering の探索)。

出典: add-maui-basic-input-cells design.md Decision 7 (採用はオーナー判断「B しかない。原典にその機構があるので重くない」) / review-002 (Minor-10・保留(a)(b) の評価)
