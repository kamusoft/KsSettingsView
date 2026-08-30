# Proposal: align-maui-accessory-placement-guard

## Why

add-maui-custom-cell で CustomCell.Content に入った多重配置の規律 — 「値確定前の検査 (`IKsCellContentGuard`)・バッチ内重複の native 更新前の全件検査・失敗時に公開値/論理所有/実体を一切動かさない」 — が、accessory (Root / Section × Header / Footer) には未適用のまま残っている (同変更 review-001 / review-002 の Minor 指摘)。

現行コードでは、配置済み Section の `HeaderView` を他所有の View へ差し替えると、値の確定と `ReassignIfFree` による旧 View の論理所有の解除が先に走り、その後の controller 検査で `InvalidOperationException` になるため、公開値・論理所有・表示状態が分離する (Root 側は所有は保たれるが公開値と表示が分離する)。

## What Changes

- **maui-core**:
  - `Section.HeaderView` / `Section.FooterView` / `SettingsView.RootHeaderView` / `RootFooterView` に `validateValue` ガード (`IKsCellContentGuard` と同型の内部 guard) を追加し、値確定前に多重配置を検査する。失敗時は公開値・論理所有・実体 (lease)・表示のいずれも動かさない
  - `EnsureSectionsAreNotPlaced` にバッチ内 accessory 重複の数えあげを追加し、content 側 (`EnsureCellsAreNotPlaced` の in-batch seen) と検査の置き場を対称にする
  - 回帰テスト: 「例外前後で gateway 呼び出しなし・旧状態維持」を、失敗差し替え (Section / Root / Content との交差) とバッチ重複の両方で固定する (CustomCellContentTests の鏡像)
  - 失敗時契約の明文化: 公開コレクションはロールバックしない (content 側と同一)、失敗後は呼び出し元の Root 全体再構築で再収束できる — いずれも現行挙動の固定であり実装変更を伴わない

## Non-Goals

- Native (iOS / Android) 側の変更なし。別 SettingsView 間・通常 Layout 配下との重複検出の拡大なし
- Content 側の挙動変更なし
- Replace バッチでの releasing 意味論 (旧 Section から新 Section への View 引っ越し許容) の導入はしない (現行の strict 検査を維持。content 側 `ReplaceCells` も同じ strict であり対称)
- 公開コレクション (`Root` / `Section.Cells`) の失敗時ロールバックはしない (content 側と同一の契約 — コレクションは呼び出し元の操作後の状態のまま残り、native・対応表には反映されない。スペックで明文化する)
- Root 再構築内での null を経ない所有者間 View 移動の意味論は規定しない (保証経路は null 解除後の再設定。accessory の論理所有は解放時も所有者が保持する現行設計 — `RetireAccessoryView` は content 側 `RetireCellContent` と異なり所有を解かない — に踏み込まない)

## Impact

- 破壊的変更なし。失敗時の例外送出タイミングが「値確定後」から「値確定前」へ変わる (例外型 `InvalidOperationException` は同一、失敗後の状態はより安全側になる)。成功経路は不変
- 調査所見: 依頼時に想定された「バッチ内 accessory 重複が native 更新後に例外になり部分更新が残る」経路は、現行コードでは `AddSections` / `ReplaceSections` の両入口で `EnsureTreeHasNoDuplicates` が native 前に弾いており再現しない。ただし検査の置き場が非対称 (`EnsureSectionsAreNotPlaced` 自身は数えない) で、tree-dup を伴わない将来の呼び出し口で穴が開く構造のため、対称化と回帰テストで契約を固定する

## 級: M

maui-core 1能力内の公開契約 (多重配置検出の例外タイミングと失敗時の状態保証) の小変更。

domain: maui
