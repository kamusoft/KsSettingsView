# Proposal: align-view-accessory-header-height

(旧名 fix-ios-view-header-height-override — 裁定により修正対象が iOS → Android へ転換したため改名 2026-08-11)

## Why

Section Header の固定高さ (`Section.headerHeight` / `Theme.headerHeight`) が view accessory (`SectionAccessory.View`) に効くかどうかが OS 非対称になっている — iOS は accessory 種別を見ずに固定高さを適用する一方、Android は Text accessory にしか headerHeight を渡しておらず view では常に自動高さ (AiForms 原典準拠)。phase-6-accessory-views の議論 (2026-08-11、論点⑤) で「固定値は view accessory にも勝つ」を公開契約として裁定し、Android を iOS へ対称化する方向で確定した ([exploration.md](exploration.md) の裁定を参照)。MAUI の view accessory 対応 (phase-6 本体) はこの対称化済み契約を前提にするため、先行 M 級 change として実施する。

## What Changes

- **settings-view-android-ui**: `SectionAnyViewAccessoryViewHolder.bind()` に headerHeight の解決と適用を追加する。優先順位は Text accessory と同一 — `Section.headerHeight > 0` → 固定 (dp→px)、`-1` かつ `Theme.headerHeight > 0` → Theme 値、いずれもなければ自動 (WRAP_CONTENT)。adapter (`KsSettingsListAdapter.onBindViewHolder`) から Header 位置の View accessory へ `item.headerHeight` と `theme` を伝搬する
- **動的高さ変更 (payload 方式)**: `CellListItemDiffCallback` が View accessory の `headerHeight` 差を検出し、高さのみの変更は change payload で hosted view を維持したまま layoutParams だけを更新する (view の内部状態を失わない)。旧契約 (View accessory の高さ差を無視) を固定していた既存テストは新契約へ置換する
- iOS は挙動変更なし。両 OS 対称の回帰テストで現行挙動 (view accessory + 固定高さ) を固定する
- ADR 起票 (蒸留時): 「`Section.headerHeight` / `Theme.headerHeight` は accessory 種別 (text / view) に依らず適用する (OS 対称)」— core ドメイン。concepts (core/styling/list-appearance.md) の headerHeight 記述が text 段落内にある点も追随明文化

## Non-Goals

- Footer の固定高さ (`Section` に footerHeight は存在しない — 現行契約のまま)
- Root accessory の高さ指定 (存在しない)
- iOS 実装の変更 (現行挙動を意図的拡張として確定・維持)
- MAUI 層の対応 (phase-6 本体 change の責務)

## Impact

- **Android の公開挙動変更**: view accessory と正の headerHeight (Section または Theme) を併用している既存利用者は、自動高さ → 固定高さ (内容がはみ出す場合は clip) に見た目が変わる。これは裁定された対称化であり意図された変更
- 公開 API シグネチャの変更なし (internal ViewHolder / adapter のみ)。iOS・MAUI への影響なし
- リスク: 低〜中。変更点は bind 時の高さ解決に加え、DiffCallback の高さ差検出と payload 経由の高さ更新経路に及ぶ (旧契約の固定テスト置換を含む)

## 級: M

公開 API 変更はないが公開挙動の変更であり、契約の明文化 (concepts 更新) と ADR を伴うため (exploration の暫定判定どおり)。

domain: cross
