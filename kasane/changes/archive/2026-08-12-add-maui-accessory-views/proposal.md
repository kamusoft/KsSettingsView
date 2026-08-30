# Proposal: add-maui-accessory-views

## Why

Root / Section の Header・Footer に任意の MauiView (`View`) を設定できるようにする (roadmap maui-support のゴール項目)。現状の MAUI facade は header / footer を text に限定しており (`RootHeaderText` 等)、native が既に持つ任意 View accessory (`KsAnyView`) を MAUI から利用できない。また本フェーズで建てる MauiView → platform view 実体化機構は phase-5 (CustomCell) が再利用する共有基盤の初出である。設計判断は phase 議論で確定済み — agenda 決定①〜⑦ (kasane/roadmaps/maui-support/phases/phase-6-accessory-views/agenda.md) と maui/ADR-0016〜0018 (proposed)。

## What Changes

- **maui-core** (facade): 公開 API `Section.HeaderView` / `FooterView`・`RootHeaderView` / `RootFooterView` (いずれも `View?`、AiForms 互換命名 + phase-2 予約名の実体化)。text との競合は View 優先・View null 戻しで text フォールバック。更新セマンティクスは「差し替え = 明示経路で再発行 / 内容変化 = 再発行せず live 追従」(maui/ADR-0018)。Handler 切断・再接続をまたぐ Host 世代管理 (切断時 wrapper 破棄 + 接続時再実体化・再適用、maui/ADR-0016)。内部機構として materializer seam (per-TFM) + MAUI 公式骨格の自己計測 wrapper を新設 (maui/ADR-0016)。同一 View インスタンスの多重配置は既存の facade 制約に従い例外
- **maui-bridge**: 新 API `updateAccessoryView(target, sectionID, view)` と `KsBridgeSection` への `headerView` / `footerView` フィールド追加 (platform view の直接輸送、maui/ADR-0017)。Bridge 内部で定数返し closure (`KsAnyView.uiKit { view }` 等、返却前 detach 付き) に包んで既存 Store 経路へ。**native (Core / UI) は無変更**
- **samples-maui**: `AccessoryViewsDemoPage` 1ページ追加 (sample-parity の「MAUI のみの画面」例外枠、native 追随義務なし)。一覧ページに「MAUI 固有」区分の Section を新設して配置

## Non-Goals

- `DataTemplate` 版 (HeaderTemplate 等) — 原典に無く、テンプレート系は phase-10 の領域
- CustomCell の content 実体化 — phase-5 の責務 (本 change の共有機構を再利用する側)
- native Core / UI の変更 — headerHeight の OS 対称化は先行 change align-view-accessory-header-height で実施済みの前提。**例外**: iOS の高さ再計算口が tasks 1.1 の検証で「wrapper だけでは届かない」と確定した場合に限り、native 側の再計算口の追加をパリティ整備として本 change のスコープに含める (phase 議論で合意済みの条件付きスコープ。native の `KsAnyView` accessory でも同じ問題が起きる一般ギャップのため)
- Header / Footer の表示トグル (IsHeaderVisible 等) — phase-9 の責務

## Impact

- 公開 API は追加のみ (破壊的変更なし)。Bridge API も追加のみ (既存 12 メソッドと DTO は不変)
- 前提: align-view-accessory-header-height (先行 M 級) の完了
- リスク1: iOS の UICollectionView self-sizing で wrapper の invalidation 中継が行高さ再計算まで届くかは未確証 (agenda TODO)。届かない場合は native 側に再計算の口を足す必要があり (パリティ整備の範囲)、その要否は実装フェーズ冒頭の検証で確定する
- リスク2: view accessory 差し替え時に native が旧 view を正しく剥がすかの検証 (agenda TODO) — 問題があれば ADR-0017 の detach 対策の範囲で吸収する想定

## ui/ について

ui/ (brief / mock) は作成しない (オーナー合意 2026-08-11)。accessory の視覚は利用者が渡す View の素通しで、本 change にデザインすべき固有の見た目が存在しないため。サンプルページは機能チェックリスト (パリティ対象外の MAUI 固有画面) で、MAUI フェーズの先行 change (add-maui-core / add-maui-basic-input-cells 等、いずれも ui/ なし) と同じ扱い。視覚確認は tasks 7.2 のスクリーンショット記録で担保する。

## 級: L

新公開 API + interop 境界の拡張 + 新機構 (両 OS の wrapper) を含み、複数 capability にまたがるため。

domain: maui
roadmap: maui-support/phase-6-accessory-views
