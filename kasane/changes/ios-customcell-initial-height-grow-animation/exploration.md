# Exploration: ios-customcell-initial-height-grow-animation

## 課題 / 動機

(2026-09-17、`../ColorAnalyzer` の change `migrate-remaining-pages-to-kssettingsview` より越境の簡易起票。出典: `../ColorAnalyzer/kasane/changes/migrate-remaining-pages-to-kssettingsview/session.md`)

**症状** (オーナーの実機観察。iOS 実機 iPhone 11、表示言語は日本語、KsSettingsView.Maui 0.1.0-beta.4 系):
利用側アプリのリアルタイム色設定ページへ遷移した直後、先頭 Section の `CustomCell` (中身は `ContentView Padding="16"` + 折り返し `Label`) の**高さが後から増え**、それに合わせて後続の Section 以下がわずかにスライドダウンする。AiForms.Maui.SettingsView には無かった挙動。機能面の問題は無い。

**依頼元の調査で読んだ根拠** (起票時に実物で行の実在を確認し、行番号を実物に合わせた。因果は未検証):

- 幅未確定の間は無限幅で測る: `maui/KsSettingsView.Maui/Platforms/iOS/KsAccessoryHostView.cs:49-59` の `IntrinsicContentSize` は `Bounds.Width` が 0 の間 `double.PositiveInfinity` を幅制約にして measure する (折り返し Label は無限幅だと 1 行分の高さを返す)。同 `:70-80` の `LayoutSubviews` で幅の変化を検知すると `InvalidateIntrinsicContentSize()` し、実幅で測り直す
- この wrapper は accessory と CustomCell の content で共通: `maui/KsSettingsView.Maui/Platforms/iOS/KsViewMaterializer.cs:17-23`。Android 版は `OnMeasure` の measureSpec で測るため、この状態は無い (iOS 固有と見られる)
- content には Native へ再計測を送り直す経路が無い: `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:873-893` (remarks のとおり意図的で、`:892` のコールバックは `static () => { }`)。iOS は intrinsic size の無効化 → self-sizing 任せで、`ios/Sources/KsSettingsViewBridge/KsBridgeCellContentHostView.swift:188-193` (`layoutSubviews` 内で高さの変化を検知して `invalidateIntrinsicContentSize`) → `ios/Sources/KsSettingsViewBridge/KsBridgeCellContentView.swift:40-53` (`sizeThatFits`) → `ios/Sources/KsSettingsViewUI/CustomCellView.swift:101-106` (`preferredLayoutAttributesFitting`) と伝わる
- `ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift:19-21` のコメントが「行の高さは content のサイズ変化より 1 レイアウトパス遅れて追いつく」と明言している。この Layout は content のはみ出し対策で、行そのものの高さ遷移と後続行のシフトは抑止していない
- `ios/Sources/` 配下に `UIView.performWithoutAnimation` / `setAnimationsEnabled(false)` は無い。あるのは `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:123` の mask 更新用 `CATransaction.setDisableActions(true)` のみ。内容差し替え時の snapshot 適用は `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1948` で `animatingDifferences: true` (同ファイルの他の `apply` 呼び出しも同様)

**関連する既存の記録**:

- `kasane/decisions/maui/0016-mauiview-materialization-self-measuring-wrapper.md` (自己計測 wrapper の決定。起票時に存在のみ確認)
- `kasane/decisions/maui/0020-customcell-content-live-view-generation-token.md`
- `kasane/decisions/ios/0002-customcell-hosting-recreation-accepted.md`
- `kasane/concepts/maui/architecture/view-materialization.md`
- `kasane/roadmaps/maui-support/phases/phase-5-custom-cell/artifacts/probe/2026-08-12-cell-content-size-follow.md`
- 関連: `kasane/changes/android-accessory-view-late-insert-animation` (Android 固有の FooterView 遅延挿入。別症状のため合流させない)

**利用側の状況**: ColorAnalyzer は**本体の修正を先に行う方針** (オーナー決定 2026-09-17)。利用側では回避策 (Label の高さ固定・テキストの見出しへの移動) をあえて入れず、`../ColorAnalyzer/ColorAnalyzer/Views/Others/RealtimeColorSettingsPage.xaml` の先頭 Section の CustomCell を修正確認のサンプルとして残している。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- **未探索 (簡易起票)** — 上記の根拠は依頼元のコード読みを行の実在まで確認しただけで、因果の検証・議論はしていない。コード読みを事実とせず、実機の観察を正とする
- **未解明**: 同じアプリの解析設定ページにも XAML 上完全に同型の説明行 (文言はより長く日本語で 3 行ぶん。増分はより大きいはず) があるが、そちらではスライドダウンが観測されていない。同じ経路を通るはずなので、差はコードから説明できていない (レイアウト順序・画面構成への依存は推測)。再現条件の特定が要る
- 初回の問い合わせ時点で `Bounds.Width` が実機で実際に 0 になるか、高さ変化が実際にアニメーションとして出る条件は、コードだけでは断定できていない。これを固定するテストも見つかっていない
- iOS シミュレータ (iPhone 17 Pro) の自動確認では気づかれなかった (取得粒度の問題か、機種・幅の問題かは不明)
- accessory (Header / Footer の View) も同じ wrapper を通るので、同種の初回高さのブレが起きるかは未検証 (利用側の観察では、iOS の解析設定ページのフッターに遅延・アニメーションは出ていない)
- 競合する原因仮説 (2026-09-17、`kasane/changes/android-accessory-view-late-insert-animation` の探索より): CustomCell の content も初回配信には載らず `Loaded` 後に遅れて届く (`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:346-354`)。iOS は内容差し替えを `animatingDifferences: true` で適用するので、「無限幅での初回計測」ではなく「content の後着」でも同じ見え方になり得る。あちらの change が content も初回配信に間に合わせる形になった場合は、その後に再現確認をすると切り分けになる
- 直し方は未検討

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S / M / L (理由)

未判定
