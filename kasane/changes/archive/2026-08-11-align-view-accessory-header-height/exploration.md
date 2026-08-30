# Exploration: fix-ios-view-header-height-override

- 起票日: 2026-08-05
- 起票経緯: [fix-android-header-height-refresh](../archive/fix-android-header-height-refresh/exploration.md) の議論中に AiForms オリジナルを裏取りして発見。実装は未着手・議論も未実施の簡易起票

## 課題

iOS では View accessory (`SectionAccessory.View`) の Section Header にも `Section.headerHeight` が固定高さとして適用される。AiForms オリジナルは View Header のとき HeaderHeight を無視して Content の高さを優先するため、**オリジナル非準拠**であり、同じ挙動を守っている Android とも **OS 非対称**になっている。

- `KsSettingsViewController.makeHeaderBoundaryItem` は `headerHeight > 0` なら `.absolute(headerHeight)` を返し、accessory が Text か View かを見ていない ([KsSettingsViewController.swift:542](../../../ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:542))
- 結果、指定高さより背の高い View Header は内容が切れ、低い View Header は余白が生まれる
- Android は `onBindViewHolder` が Text 側にしか headerHeight を渡さないため、View accessory では自動高さのまま (オリジナル準拠)

### AiForms オリジナルの該当実装

原典: `../AiForms.Maui.SettingsView`

- iOS `Native/iOS/SettingsTableSource.cs` `GetHeightForHeader`: 冒頭で `sec.HeaderView != null` なら `UITableView.AutomaticDimension` を即 return し、HeaderHeight を読まない
- Android `Native/Android/SettingsViewRecyclerAdapter.cs`: Text は `BindHeaderView` が `LayoutParameters.Height` を設定、Custom View は `BindCustomHeaderFooterView` が高さに一切触れない

## 修正方向の候補 (未議論)

- `makeHeaderBoundaryItem` で accessory 種別を判定し、View accessory のときは `headerHeight` を無視して `.estimated(...)` を返す
- 併せて公開契約の明文化 — [concepts/core/styling/list-appearance.md](../../concepts/core/styling/list-appearance.md) の headerHeight 記述は text 段落内にあり、View accessory への非適用を明言していない

## 論点 (未決)

- View accessory + headerHeight を併用している既存利用者の見た目が変わる。オリジナル準拠を優先するか、現行 iOS 挙動を「意図的な拡張」として維持するか
- 維持を選ぶ場合は Android 側を iOS に合わせる選択肢もあるが、その場合はオリジナルから両 platform とも離れる
- ADR 候補: 「`Section.headerHeight` は text accessory にのみ適用し、View accessory は Content 高さを優先する」— 公開挙動を規定し iOS/Android 双方を縛るため

## 級の推奨 (暫定)

M — 公開 API の変更はないが公開**挙動**の変更であり、契約の明文化 (concepts 更新) と ADR を伴う。判断は議論後に確定する。

## 裁定 (2026-08-11、phase-6-accessory-views の議論による)

論点「オリジナル準拠か、現行 iOS 挙動を意図的拡張として維持か」は phase-6-accessory-views の論点⑤ (headerHeight との相互作用) で裁定された — **修正方向を転換する**:

- **iOS の現行挙動 (view accessory にも headerHeight 固定が効く) を意図的拡張として確定し、Android を iOS に合わせて対称化する** (View accessory にも headerHeight を適用)
- 優先順位の公開契約: 正値固定 (内容のはみ出しは clip) > `-1` + Theme.headerHeight > `-1` 自己計測
- オリジナル非準拠を両 platform で受け入れる。理由: (1) 明示指定が無言で無視される罠を避ける (2) オリジナル準拠へ倒すと既存 iOS 利用者の見た目が変わる公開挙動変更になる (3) MAUI の view accessory 対応 (phase-6) が固定高さ semantics を両 OS 対称で前提にする。対称化は契約対称化のパリティ整備 (iOS への replaceCells 追加、maui/ADR-0002 と同じ理屈)
- 修正対象は iOS ではなく **Android** (`onBindViewHolder` の View accessory 側にも headerHeight を渡す)。change 名の「fix-ios」は実態と逆になったため、propose 時に改名を検討する
- 併せて concepts (core/styling/list-appearance.md) の headerHeight 記述に View accessory への適用を明文化し、ADR 候補は「`Section.headerHeight` は accessory 種別に依らず適用する (text/view・OS 対称)」へ差し替える

経緯の詳細: [phase-6-accessory-views/history.md](../../roadmaps/maui-support/phases/phase-6-accessory-views/history.md) の 2026-08-11 ⑤

## 関連

- 出典: [fix-android-header-height-refresh](../archive/fix-android-header-height-refresh/exploration.md) の AiForms 裏取り (2026-08-05)
- 関連 ADR: android/ADR-0012 (Section H/F 内容検出の DiffCallback 化)
- 裁定元: [phase-6-accessory-views agenda](../../roadmaps/maui-support/phases/phase-6-accessory-views/agenda.md) 決定⑤ (2026-08-11)
