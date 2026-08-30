# Exploration: fix-android-header-height-refresh

- 起票日: 2026-08-05
- 起票経緯: fix-android-accessory-header-refresh の独立レビュー (review-001 Minor) で発見された既存挙動の簡易起票。実装は未着手
- 議論再開: 2026-08-05 — AiForms オリジナルの裏取りでスコープを確定

## 課題 / 動機

Android で `Section.headerHeight` だけを変えた full 更新 (`replaceSection` / `SettingsRootDiff.Full`) は、header text が同一だと再 bind されず固定高さが表示へ反映されない。

- `CellListItemDiffCallback.areContentsTheSame` の Section H/F 比較は accessory 内容のみで、`CellListItem.SectionHeader.headerHeight` を含まない (fix-android-accessory-header-refresh で明示化し KDoc に注記済み)
- `SectionTextAccessoryViewHolder.bind` は `headerHeight` を `itemView.layoutParams.height` へ反映するため、再 bind されれば直る
- `Section.headerHeight` は公開 API で、これを変える手段は full 更新経路のみ = 実際に到達可能
- fix-android-accessory-header-refresh のデルタスペックは Requirement を accessory 内容に限定しており、本件はスコープ外の既存挙動 (修正前から同じ)

**AiForms オリジナルは headerHeight の変更を明示的に反映している** (下記裏取り)。したがって本件は「オリジナル非準拠の欠落」であり、修正の妥当性が裏付けられた。

## AiForms オリジナルの挙動 (2026-08-05 コード裏取り)

原典: `../AiForms.Maui.SettingsView` (ローカルクローン)

| 観点 | AiForms オリジナル | 該当箇所 |
|---|---|---|
| Text header + HeaderHeight | 固定高さを適用 | `Native/Android/SettingsViewRecyclerAdapter.cs` `BindHeaderView` / `Native/iOS/SettingsTableSource.cs` `GetHeightForHeader` |
| **View header + HeaderHeight** | **無視 (Content 高さ優先)** | Android: `BindCustomHeaderFooterView` が高さに一切触れない / iOS: `GetHeightForHeader` 冒頭で `sec.HeaderView != null` なら `AutomaticDimension` を即 return |
| HeaderHeight 変更の再反映 | 明示通知で反映 | Android: `OnSectionPropertyChanged` が `Section.HeaderHeightProperty` を拾って `UpdateSectionHeader` / iOS: `SettingsViewHandler.iOS.cs:140` が同プロパティを監視 |

Android は Text / Custom View で ViewHolder ごと分岐している (`ModelProxy.cs:136` の `HeaderView == null ? TextHeader : CustomHeader`)。

### KsSettingsView の現状との対比

| 観点 | オリジナル | KsSettingsView Android | KsSettingsView iOS |
|---|---|---|---|
| Text header + headerHeight | 固定高さ | 固定高さ (準拠) | 固定高さ (準拠) |
| View header + headerHeight | 無視 | **無視 (準拠)** | **`.absolute` 適用 (非準拠)** |
| headerHeight 変更の再反映 | 反映 | **反映されない (非準拠 = 本 change)** | 反映される (準拠) |

- iOS が View accessory にも固定高さを強制しているのは `KsSettingsViewController.makeHeaderBoundaryItem` が accessory 種別を見ていないため
- iOS の再反映は `visibleSections` 更新後の `invalidateLayout()` で sectionProvider が再評価されることで成立する (full 更新・`replaceSection` の双方)

## 検討した選択肢

- **(a) `areContentsTheSame` の SectionHeader 比較に `headerHeight` を含める** — 採用。`Double` の値比較でありリスナー等価不安定性の問題はない。「内容が同一なら通知しない」契約とも矛盾しない
- **(b) (a) + Android の View accessory にも headerHeight を適用する** — 却下。裏取りの結果、Android の現行挙動 (View accessory で headerHeight 無視) は**オリジナル準拠**であり、修正対象ではなかった。議論開始時は「Android のバグ・iOS が正」と読んでいたが逆だった
- **(c) (a) + iOS のオリジナル非準拠修正を本 change に含める** — 却下。Android/iOS 双方に触れて S → M へ上がり、回帰確認の重みが増す。iOS 側は「View accessory を使う利用者の見た目が変わる」挙動変更で判断軸が別

## 決定事項

- 本 change のスコープは **(a) のみ** (ユーザー確定 2026-08-05)
- iOS のオリジナル非準拠は別 change [fix-ios-view-header-height-override](../fix-ios-view-header-height-override/exploration.md) として簡易起票する
- テスト: headerHeight のみ変更の full 更新が表示へ反映される Scenario を追加して固定する

## ADR 候補

- 未起票: 「`Section.headerHeight` は text accessory にのみ適用し、View accessory は Content 高さを優先する」— 公開挙動を規定し iOS/Android 双方を縛るため ADR 級に該当し得るが、iOS 側の挙動が確定していない段階では起票しない。別 change (fix-ios-view-header-height-override) で挙動を決める際に起票判断する

## 実装時に決着した論点

- `headerHeight` 比較を無条件に行うか Text accessory 限定にするか → **Text accessory 限定**を採用 (実装 2026-08-05)。`bindKsAnyView` の `KsAnyView.AndroidView` 分岐は `removeAllViews()` + `factory(context)` で View を作り直すため冪等ではなく、表示に影響しない headerHeight 変更で rebind すると View の内部状態が失われる。Android が View accessory に headerHeight を適用しない (オリジナル準拠) 事実とも整合する
  - この判断は iOS 側 change での ADR 起票判断へ引き継ぐ (View accessory の高さ解決を Content 優先と定める根拠の一部になる)

## 未決の論点

- [concepts/core/styling/list-appearance.md](../../concepts/core/styling/list-appearance.md) の headerHeight 記述は text 段落内にあり、View accessory への非適用を明言していない。iOS 側 change で挙動を確定させた後に文言を更新する
- [concepts/core/architecture/display-state-synchronization.md](../../concepts/core/architecture/display-state-synchronization.md) の Android 欄に、Section Header の固定高さが内容差の検出対象に含まれることを追随させる (蒸留フェーズの対象)

## 変更級の推奨: S

DiffCallback への 1 行の比較追加 + テスト。公開 API 変更なし・単一能力内・可逆。独立レビューは ksn-orchestrator の必須ゲートとして実施する。

## 関連

- 出典: [fix-android-accessory-header-refresh review-001](../archive/fix-android-accessory-header-refresh/review-001.md) Minor 3
- 関連 ADR: android/ADR-0012 (Section H/F 内容検出の DiffCallback 化)
- 実行時検証: [verification/README.md](verification/README.md) — Pixel 6a (Android 16) での A/B (症状再現・解消・往復)
- 派生起票: [fix-dsl-header-height-diff](../fix-dsl-header-height-diff/exploration.md) — 実機検証中に発見した DSL 経路 (Android Compose / iOS SwiftUI 両方) の同症状。本 change では解消しない
- 派生起票: [fix-ios-view-header-height-override](../fix-ios-view-header-height-override/exploration.md) — iOS の View accessory への headerHeight 適用 (オリジナル非準拠)
