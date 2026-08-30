# Tasks: align-view-accessory-header-height

## 1. Android 実装

- [x] 1.1 `SectionAnyViewAccessoryViewHolder.bind()` に `theme` / `headerHeight` / `isHeader` を追加し、Text 側と同一の解決 (`Section.headerHeight > 0` → 固定 dp / `Theme.headerHeight > 0` → Theme 値 / それ以外 → WRAP_CONTENT) を container の layoutParams に適用する。ViewHolder 再利用時に前回の固定高さを引きずらないこと (→ Requirement: Section Header の固定高さは accessory 種別に依らず適用される / Scenario: 固定高さの Header と自動高さの Header が混在しても互いに影響しない)
- [x] 1.2 `KsSettingsListAdapter.onBindViewHolder` の `SectionHeader` × `SectionAccessory.View` 分岐から `item.headerHeight` と `theme` を伝搬する。`SectionFooter` × View 分岐は現行のまま (→ Scenario: Footer の view accessory は高さ指定の対象外)
- [x] 1.3 高さ解決ロジックが Text / View の2 ViewHolder で重複しないよう共通化する (解決関数の抽出等。実装形は実装者判断)
- [x] 1.4 `CellListItemDiffCallback`: View accessory 同士でも `headerHeight` の差を検出し、**高さのみの変更は change payload** で通知する — 内容の再バインド (`bindKsAnyView`) を伴わず layoutParams だけを更新する経路を adapter に追加する。`isSameHeaderHeight` の「View accessory は高さ差を扱わない」前提のコメントを新契約へ更新する (→ Requirement: 表示済み Header の headerHeight 変更は hosted view を維持したまま反映される)
- [x] 1.5 旧契約を固定している既存テスト (ListAdapterDiffTest の View accessory 高さ差無視 / FullUpdateContentSyncTest の同等ケース) を新契約 (payload 通知) のテストへ置換する

## 2. テスト

- [x] 2.1 Android ユニットテスト: view accessory の固定高さ (Section 指定 / Theme フォールバック / Section > Theme 優先 / 未指定自動 / 固定と自動の混在 / Footer 対象外) (→ 全 Scenario)
- [x] 2.2 Android ユニットテスト: text accessory の高さ解決の回帰確認 (→ Scenario: text accessory の高さ解決は変更されない)
- [x] 2.4 Android ユニットテスト: 動的高さ変更 (自動→固定 / 固定→自動 / 固定A→B) と、高さのみ変更時に view インスタンスが同一のまま維持されること (→ Requirement: 表示済み Header の headerHeight 変更は hosted view を維持したまま反映される の全 Scenario)
- [x] 2.3 iOS ユニットテスト: 現行挙動 (view accessory + `headerHeight` 正値 → `.absolute` 固定) を回帰固定する対称テストを追加する (iOS 実装は無変更。対称化の両端をテストで固定する)

## 3. 検証

- [x] 3.1 Android テストスイート全体が green であること
- [x] 3.2 iOS テストスイート全体が green であること
- [x] 3.3 実機/エミュレータで clip の視覚確認: 固定高さより大きい内容の view accessory が境界外へ描画されないことをスクリーンショットで記録する (`layoutParams.height` の設定だけでは clip は検証できないため。ui/verification/ に保存) (→ Scenario: view accessory + Section.headerHeight 正値で固定高さになる)
