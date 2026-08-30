# Live Summary: adjust-section-spacing

確定日: 2026-08-25 (オーナーが iPhone 17 シミュレータ・Android エミュレータ・実機 Pixel 6a で実物確認のうえ確定)

## 最終状態 (何がどうなったか)

### 1. Section Header / Footer の余白 (iOS / Android 同値化)

- Modern の既定 sectionMargin: iOS `top 22 / leading 16 / bottom 0 / trailing 16` (変更なし)、Android を同値へ (`top 12dp / bottom 12dp` → `top 22dp / bottom 0dp`、水平 16dp 据置)
- Classic の既定 sectionMargin: 両プラットフォームとも Modern と**完全同値**に変更 (旧: 全方向 0)。水平成分は既存の `resolve` 仕様 (Classic では leading/trailing を無視して全幅を保つ) がそのまま効くため、実効値は上下のみ反映。この「上下は反映・水平は無視」の挙動が iOS / Android で同一仕様であることを実物で確認済み
- Header ラベルの下余白 / Footer ラベルの上余白: 両プラットフォームとも 4pt/dp (iOS 旧 2pt、Android 旧 0)。**Section の Header/Footer に限定** — Root Header/Footer は両プラットフォーム 0 で統一 (Root は利用者がカスタム View を設定する想定のためライブラリ側で余白を入れない。iOS は旧 2pt から 0 への意図的変更)
- MAUI: 編集不要。ライブラリ層は既定値を複製せず null をネイティブに委譲しており (`maui/KsSettingsView.Maui/SettingsView.cs:443`)、ネイティブ確定値が MAUI 経由でも反映されることをシミュレータで確認済み。サンプルの明示 margin にも旧既定前提の値なし

### 2. Android SwitchCell の状態色 (対話中にオーナー指示で対象拡大)

オフ状態の色をテーマ attr 直参照 (thumb = `colorOutline` / track = `colorSurfaceContainerHighest`) から **accentColor 由来の導出**に変更。accent の解決はオン track と同一 (`cell.accentColor ?: effective.accentColor`) で、Cell ごとの accent に追従する。

導出は素の MaterialSwitch の構造 (テーマ attr がライト/ダークで明度関係を反転させる) を模倣する: **明度はテーマ attr から取り (track = `colorSurfaceContainerHighest` / thumb = `colorOutline`)、色相 = accent、彩度 = 確定した淡さの絶対値**。

- off track = 彩度 0.09 / 明度比 1.0 (attr の明度そのまま)
- off thumb = 彩度 0.04 / 明度比 0.92
- オフ時の track 枠線 (`trackDecorationTintList` unchecked) = thumb と同一の導出色 (Material3 既定が「枠線 = thumb 色 = colorOutline」の関係であることを踏襲)。オン時は透明 (M3 既定と同値)
- ダークモードでは attr の反転に自動追従し、素の M3 と同じ「thumb が track より明るい」関係になる。ライトの見た目はオーナー確定値と ΔRGB ≤ 2/ch で同一
- **オン thumb**: `colorOnPrimary` 直参照を撤去し、accent 基準のコントラスト色 (`onThumbColorFrom`) に変更 — ダークで onPrimary がテーマ primary の暗トーン (紫紺) に解決され、track = accent なのに thumb だけテーマ色が漏れていた (オーナー指摘) の是正。白とのコントラスト比 ≥ 1.5 なら白、下回る明色 accent のみ accent 色相の暗色 (彩度 0.10 / 明度 0.15)。ライトは両状態とも見た目不変、ダークのオン thumb が紫紺→白。オン track (accent そのまま) は不変

## 採用値と根拠 (却下試行の要点)

| 項目 | 採用値 | 根拠 / 却下した値 |
|---|---|---|
| Header 下 / Footer 上 | 4pt/dp | 旧 2pt は箱に張り付き気味 (オーナー評価)。4pt で Header/Footer の帰属を保ちつつ呼吸が出る |
| Classic 既定 margin | Modern と完全同値 | 「Section Margin の上下は Classic にも反映・水平は無視」という仕様を既定値の対称性ごと示すため (オーナー指示)。上下のみ同値・水平 0 の案は却下 |
| Android Modern margin | top 22dp / bottom 0dp | iOS と同値化。旧 top 12dp が「詰まりすぎ」の主因だった |
| off track 係数 | 彩度 0.09 / 明度比 1.0 (attr 基準) | ライトの対話ループで blend 28%→14% + 減彩 + 明度 +7% と詰めた確定色を、attr 明度基準の絶対彩度で等価に再表現したもの (ΔRGB ≤ 2/ch) |
| off thumb 係数 | 彩度 0.04 / 明度比 0.92 (attr 基準) | 同上。対話ループの経緯: 彩度 45% は強すぎ→16%→実測で M3 既定 (彩度 3%) と比較し 8%、明度は 55% だと暗すぎ→65%。この確定色を attr 基準で再表現 |
| ダーク対応の構造 | 明度をテーマ attr から取得 | ライト実測ベースの固定係数乗算はダークで track/thumb の明度差が潰れる (実測差 2pt)。素の MaterialSwitch の attr 追従を模倣し、ダークで明度関係が自動反転する構造へ (オーナー方針) |
| オン thumb | 白 (対 accent コントラスト比 ≥ 1.5)。明色 accent のみ accent 色相の暗色 | `colorOnPrimary` 参照はダークでテーマ色が漏れる (オーナー指摘)。閾値は当初 WCAG 3.0 で実装したが緑 #34C759 等の一般的 accent (白比 2.2 前後) が暗色に倒れライトの見た目が変わるため 1.5 へ (thumb は文字でなく面積のある図形、素の M3 も同水準) |
| 導出の妥当性検証 | — | 検証用 accent 3 色 (赤/青/オレンジ) をサンプルに一時付与して色相追従を確認 (確定時に完全復元、`samples/` 差分ゼロ)。変更前後スクショのピクセル実測 (HSL) で M3 既定との彩度・明度差を数値比較して係数を決めた |

## 触ったファイル

実装 (iOS 3 / Android 3。MAUI は変更なし):

- `ios/Sources/KsSettingsViewUI/SectionBoxMetrics.swift` — `classicDefaultMargin` を Modern と同値へ
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` — Header ラベル下 4pt / Footer ラベル上 4pt (`sectionTextGap` 定数。Section 限定で Root は 0 — Root/Section の分岐は `textGap(forElementKind:)` に集約)
- `ios/Sources/KsSettingsViewUI/SectionBoxLayout.swift` — 初期 metrics を `resolve` 経由に (margin は読み手ゼロで挙動不変)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionBoxMetrics.kt` — `MODERN_DEFAULT_MARGIN` top 22dp / bottom 0dp、`CLASSIC_DEFAULT_MARGIN` = Modern 同値
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt` — `applySectionTextVerticalPadding` 新設 (Header 下 4dp / Footer 上 4dp)、根拠のない「AiForms 準拠で上下 0」KDoc を実態に合わせ書き換え
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt` — オフ色の accent 由来導出 (`offTrackColorFrom` / `offThumbColorFrom` / `tintedFrom`、係数は定数化)、オン thumb の accent 基準コントラスト色 (`onThumbColorFrom`)、`trackDecorationTintList` 明示設定

テスト (確定値に合わせた期待値・文言更新と、幾何前提の固定。プロダクト挙動の変更なし):

- iOS: `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift` / `AccessoryViewLiveProbeTests.swift` / `AccessoryMeasureInvalidationTests.swift`、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeAccessoryViewTests.swift`
- Android: `SectionBoxMetricsTest` / `SectionAccessoryRenderingTest` / `ClassicSectionDecorationTest` (期待値・テスト名を確定仕様の言葉へ)、`CustomCellRecycleTest` / `KsBridgeOperationContractTest` (全行同時描画の幾何を前提とするため Theme に `sectionMargin = PaddingValues(0.dp)` を明示固定 — margin 変更起因の失敗は実挙動の回帰ではなくテスト幾何の問題と診断実験で切り分け済み)、`BasicCellsTest` (旧仕様のオフ色記述を現行仕様へ)
- Android 追加: `SwitchCellAccentDerivationTest.kt` (状態色導出の回帰テスト 9 ケース — attr 直値でないこと・accent 色相追従・ボーダー = thumb 同色・オン track = accent・オン thumb のテーマ非依存とコントラスト分岐・ダークの明度関係。変異検証済み)

## テスト結果

- iOS: `xcodebuild test` (iOS Simulator) 588 件 / `swift test` (Core) 93 件 — 全 pass
- Android: モジュール順次実行で計 2656 件 (ui 1934 / bridge 322 / compose 240 / core 160、debug+release 変種込み) — 全 pass

## 動作証跡

- `evidence/ios-section-decoration-final.png` — iPhone 17 シミュレータ、Section 装飾デモの最終状態 (**Classic** 選択状態。Classic 既定 margin の上下反映を示す証跡)
- `evidence/android-section-decoration-final.png` — Android エミュレータ (API 35)、同画面の最終状態
- `evidence/switch-off-colors-light.png` / `switch-off-colors-dark.png` — Switch 状態色のライト/ダーク最終状態 (オン/オフ両状態。ライトは確定値と同一、ダークはオフで thumb > track の明度関係・オンは白 thumb)
- 実機確認: Pixel 6a に最終 APK をデプロイし、オーナーが余白と Switch オフ色を目視確認して確定

## 決定事項 / ADR 候補

- **ADR 候補**: 「Classic / Modern の既定 sectionMargin を同値に統一する (水平成分は Classic の全幅契約により無視される)」— concepts に明文化されていた旧仕様 (Classic 上下 0) を覆す決定のため
- **concepts 追随 (distill 送り)**: `kasane/concepts/core/styling/list-appearance.md:56-58` と `kasane/concepts/core/styling/style-resolution.md:48` の「Classic: 上下 0」、および `list-appearance.md:65` の Android Modern 参考値 12dp は本 change の確定値と抵触・陳腐化しており、ksn-distill で更新すること。あわせて「Modern の既定 margin を platform 間で同じ生値に揃えない」という規範文にも本 change (iOS/Android とも top 22 / bottom 0 へ同値化) は抵触するため、概念の改訂 (規範の撤回または条件付け) が必要
- **ADR 候補 (2つ目)**: 「SwitchCell の状態色はテーマ attr 直参照でなく accent 由来で導出する (オフ = attr 明度基準の tint、オン thumb = accent 基準のコントラスト色)」— テーマ attr 直参照へ戻す圧力が将来かかりうる領域のため
- SwitchCell 状態色の accent 由来化は公開 API 変更なし (導出はライブラリ内部の定数)。テーマ attr 由来からの利用者可視の変化 (ダークモードのオン thumb が紫紺→白になる変化を含む) として本 summary に記録 (lessons L-001 対応)
