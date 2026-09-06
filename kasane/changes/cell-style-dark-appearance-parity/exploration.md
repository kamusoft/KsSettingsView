# Exploration: cell-style-dark-appearance-parity

(旧 ID: android-cell-color-unspecified-parity。2026-09-06 の探索で範囲を「Android の placeholderColor 1 本」から「CellStyle と Cell 固有色のダーク外観対応の穴 5 点」へ広げて改名)

## 課題 / 動機

起点は docs-refresh (2026-09-06) の Android Skill 追従中に報告された drift 所見: fix-default-colors-dark-appearance で Android の `Theme` / `CellStyle` の色フィールドは `Color` (未指定 = `Color.Unspecified`) に揃ったが、Cell 固有の色引数 (`EntryCell.placeholderColor` 等) は `Color?` (`null` が未指定) のまま残っている。

探索の入口でオーナーが「Theme と既定色のダーク対応は前回 ([fix-default-colors-dark-appearance](../../changes/archive/2026-09-06-fix-default-colors-dark-appearance/exploration.md)、[core/ADR-0030](../../decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md)) で完了したが、CellStyle については未対応に見える」と提起。コード調査 (ksn-scout、2026-09-06) の結果、CellStyle の**未指定色**は既に解決済み Theme を通って外観に追随している (Android は行の再 bind ごとに `EffectiveStyle.from` を呼び直しキャッシュもない。`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:319-324`, `:810-841`)。未対応で残っていたのは次の 5 点。

| # | 穴 | 現状 (コード) |
|---|---|---|
| 1 | Android の Cell 固有色の未指定の印 | `Color?` のまま 12 本: `EntryCell.placeholderColor` / `accentColor`、`ButtonCell.titleColor`、Switch / Checkbox / SimpleCheck / Radio / Picker / NumberPicker / TimePicker / DatePicker の `accentColor`、`DatePickerCell.androidButtonColor`。解決関数も `Color?` 受け (`android/.../ui/EffectiveStyle.kt:331-338`, `:436-446`)。前回 change の deviation.md:8 が「tasks 対象外のため据え置き」と明記した積み残し |
| 2 | 明示した CellStyle 色を外観に追随させる手段 | iOS: dynamic `UIColor` で追随する (CGColor 化は `KsCheckBoxView.swift:81-137` と `SectionBoxDecorationView.swift:27-74` の 2 箇所で、どちらも trait 再解決あり)。Android Compose DSL: 再 composition → Cell の等価比較で style 差を検出 → `replaceCell` → 行の再 bind まで届く。Android Store / View: `replaceCell` / `replaceCells` 以外に手段なし (Cell は不変の data class)。MAUI: Cell 単位プロパティ → `AffectsSnapshot` → flush → gateway → native の経路はコード上ある (`maui/KsSettingsView.Maui/CellBase.cs:429-440`, `Internals/KsSettingsController.cs:1497-1540`) が実物未検証 (前回の spike は Theme 側のプロパティのみ) |
| 3 | bridge / MAUI facade の欠落 | bridge DTO `KsBridgeCellStyle` に `placeholderColor` が両 platform とも無い (`ios/Sources/KsSettingsViewBridge/KsBridgeCellStyle.swift:19-46`、`android/kssettingsview-bridge/.../KsBridgeCellStyle.kt:19-55`) のに doc コメントは「native の CellStyle と 1 対 1」。MAUI facade の CellStyle 段は 7 色中 5 色 (`CellBase` の TitleColor / DescriptionColor / ValueTextColor / HintTextColor / BackgroundColor)。accent / placeholder は Cell 固有プロパティに畳まれている。`CellBase.TitleColor` は native の ButtonCell 固有 titleColor (解決 1 段目) に写される (`Platforms/iOS/KsBridgeGateway.cs:321`) |
| 4 | Sample の MAUI 面 | **提案作成時の照合で穴ではないと判明 (2026-09-06)**。探索時は「MAUI の XAML の固定 `x:Static` 色 (`Pages/InputCellsDemoPage.xaml:50`、`Pages/UnifyCellCommonFieldsDemoPage.xaml:45-82`) が非追随」と見たが、同じ accent / placeholder は iOS / Android サンプルでも固定値 (`SampleTheme.demoAccentXxx` / `demoPlaceholderOrange`) で、sample-parity 規約どおり 3 面一致している。唯一外観分岐している基本 Cell デモの ButtonCell title (`samples/ios/.../BasicCellsDemoView.swift:167`、`samples/android/.../BasicCellsDemoScreen.kt:184`) は MAUI も code-behind (`Pages/BasicCellsDemoPage.xaml.cs` + `SampleThemeFollower`) で追随済み。Sample の変更は不要 (proposal Non-Goals) |
| 5 | テストと concepts | 明示 CellStyle 色 × 外観変更のテストが 3 面ともゼロ (Android の外観テストは全部空の `CellStyle()`)。`kasane/concepts/core/styling/style-resolution.md` の「Android の色は `Color.Unspecified`」は Cell 固有色 (`Color?`) を読み誤らせる。`KsCheckBoxView` の CGColor 再解決が書かれていない (Section 装飾の Border だけ挙げている)。MAUI 行が Theme 側の経路しか書いていない |

## 検討した選択肢 (却下案と理由を含む)

### 論点 2: 明示した CellStyle 色の外観追随の手段

| 判断軸 | A: platform の慣用に乗せて契約化 | B: Cell 粒度の追随機構を足す |
|---|---|---|
| ADR-0030 との関係 | Decision 3 (対の型・コールバックを作らない) の自然な延長。ADR 改訂なし | Revisit When「明示色まで追随させる要望」に該当し ADR 改訂が要る。ADR-0009 / 0013 (Cell 抽象) にも触る |
| Android Store / View 利用者 | 外観変更時に `replaceCell` で style を差し替える (契約に明記) | ライブラリが自動追随 |
| MAUI 利用者 | Cell プロパティに `AppThemeBinding` (実証が前提) | 同左 |
| コスト | 検証 + テスト + concepts | Cell 抽象か Store に外観の概念が入る。L 級 |

**A を採用** (2026-09-06 オーナー確定)。B は「Store 経路の利用者が replaceCell を 1 回書く手間」を省くために ADR-0030 で却下したコールバックを Cell 粒度で復活させることになり、覆すコストに見合わない。

A で利用者が書く形 (concepts の利用コード例の材料):
- iOS: dynamic な `UIColor` を CellStyle に渡す。定義場所は自由 (enum / struct の static、asset catalog の Color Set を `UIColor(named:)`、SwiftUI `Color` からの変換)。UIKit が再解決するため Cell の差し替えは不要
- Android Compose DSL: `isSystemInDarkTheme()` で選んだ色を CellStyle に渡す。事前定義は light / dark 2 セットを持つ object (ライブラリの `KsSettingsViewDefaults` と同型。Sample の `SampleTheme` がこの形) または `values` / `values-night` のリソースを `colorResource` で読む
- Android Store / View: Configuration の uiMode で選んだ色で Cell を組み、`configChanges` に uiMode を持つホストでは `onConfigurationChanged` で `replaceCell` / `replaceCells` により差し替える (Activity 再生成のホストは onCreate で組み直すだけ)
- MAUI: Cell のプロパティ (`TitleColor` / `PlaceholderColor` / `AccentColor` 等) に `AppThemeBinding` を書く

### 論点 1: Android の Cell 固有色 12 本の未指定の印

| 判断軸 | (a) 12 本を `Color = Color.Unspecified` に揃える | (b) `Color?` のまま concepts に使い分けを一文足す |
|---|---|---|
| 未指定の表現 | Theme / CellStyle / Cell 固有色が 1 流儀 | 2 流儀を利用者に覚えさせる |
| 公開 API | 破壊的 (引数の型変更。未配信のため凍結不要 — ADR-0030 前提と同じ) | 変更なし |
| ADR-0030 との整合 | Decision 5 / Alternatives と整合 | Alternatives で却下した「2 流儀混在」と同型の状態を Cell 側に残す |
| MAUI bridge | DTO は `Int?` のまま、変換で `?: Color.Unspecified` (Theme と同じ形) | 影響なし |

**(a) を採用** (2026-09-06 オーナー確定)。

### 論点 3〜5 と change の形

隣接課題を別 change に逃がさず、5 点を 1 つの change で直す (2026-09-06 オーナー確定)。#3 の placeholderColor DTO 欠落は #1 の bridge 変換に触るついでに iOS / Android 両 DTO と MAUI snapshot へ追加する。#4 は MAUI の Cell 側 `AppThemeBinding` の到達性 spike を通したうえでサンプルを追随させる。

## 決定事項

- 論点 2: A。明示した CellStyle 色はライブラリが置き換えない (Theme と同じ)。両外観で変えたいときの手段は各 platform の慣用 (iOS: dynamic `UIColor` / Android Compose: 構築時分岐 / Android Store: 外観変更時の `replaceCell` / MAUI: Cell プロパティの `AppThemeBinding`)。ライブラリに新しい型・API は足さない。ADR-0030 Decision 3 の延長であり新規 ADR は起票せず、蒸留時に ADR-0030 へ「CellStyle も同じ」と一文足す程度にとどめる (2026-09-06 オーナー確定)
- 論点 1: (a)。Android の Cell 固有色 12 本を `Color?` から `Color = Color.Unspecified` に揃える (2026-09-06 オーナー確定)
- 5 点を 1 change (本 change) にまとめ、ID を `cell-style-dark-appearance-parity` に改名する (2026-09-06 オーナー確定)
- 利用者向け書き方 (事前定義した色の持ち方・Store 経路の差し替え) は concepts に置く: 契約 (保証・しないこと) は `kasane/concepts/core/styling/style-resolution.md`、利用コード例は platform 別 api 文書 (`ios/api/ios-native-host.md`、`android/api/android-native-host.md` / `android-compose.md`、`maui/api/maui-styling.md`)。skills/ への追従は既存経路 (`skills/.manifest.json` の `targets` でこれらの concepts が 3 Skill の `references/styling.md` に紐付いている) を docs-refresh で流すだけでよく、新しい経路は不要。本 change の Non-Goal に「skills の追従は docs-refresh で別途」と書く (2026-09-06 オーナー確定)
- ゲート: MAUI の Cell 単位プロパティ (`AppThemeBinding`) が native の CellStyle 再適用まで届くかの spike を実装の先頭に置く (前回の Theme 側 spike と同じ扱い)

## ADR 候補 (作成済み: なし / 未起票: なし)

論点 2 は ADR-0030 Decision 3 の延長で新規決定にあたらない。蒸留時に ADR-0030 の Decision 3 に CellStyle への適用を一文追記する (amend 相当の小改訂) ことを ksn-distill への申し送りとする。

## 未決の論点

- (解決済み 2026-09-06) 「ライブラリは未配信」の前提は誤りで、Android は `0.1.0-beta.1` (2026-09-04) 配信済みだった (相方レビューの指摘)。オーナー裁定: beta 期間中の破壊的変更として受容、互換経路は作らない (proposal Impact)

- MAUI の Cell 側 `AppThemeBinding` の到達性 (spike で確定)
- `KsBridgeCellStyle` に placeholderColor を足すとき、MAUI facade 側 (`KsCellStyleSnapshot`) に CellStyle 段の placeholder を持たせるか、Cell 固有の `EntryCell.PlaceholderColor` だけで足りるとして DTO のみ埋めるか (propose で決める。MAUI は CellStyle 段と Cell 固有段を区別しない設計なので後者が有力)
- 前回 deviation.md:21 の据え置き (ButtonCell.kt / CellBaseLayout.kt / RadioCell.kt 等の doc コメントの ADR 参照除去) を、同じファイルに触るついでに含めるか
- iOS の Store / UIKit 直接利用で固定色を渡した場合の挙動 (固定のまま) をテストで固定するか (自明のため契約文のみでよい可能性)

## UI 素材 (ui/references/ の一覧と注釈)

なし (新規デザインなし。MAUI サンプルの追随は既存の見た目に合わせる視覚照合のみで、承認モックは不要)

## 変更級の推奨: M (2026-09-06 オーナー確定)

- 触る能力: Android UI (12 本の型変更 + 解決関数)、bridge (Android 変換 + placeholderColor DTO 欠落を iOS / Android DTO と MAUI snapshot に追加)、samples-maui (XAML を `AppThemeBinding` に)、tests 3 面 (明示 CellStyle 色 × 外観)、concepts 4〜5 文書。複数能力横断
- 公開 API: Android の Cell 固有色の型変更 (破壊的、未配信前提)。MAUI facade・iOS は変更なし
- 可逆性: 型変更は ADR-0030 と同じ前提。契約の追記は文書のみ
- UI: 新規デザインなし、承認モック不要
- L にしない理由: 新しい契約・既定値・ADR が増えず、UI 承認ゲートもない。S にしない理由: 複数能力横断で公開 API の破壊的変更を含む
