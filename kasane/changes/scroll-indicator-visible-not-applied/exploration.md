# Exploration: scroll-indicator-visible-not-applied

## 課題 / 動機

`SettingsView.ScrollIndicatorVisible` は**宣言され bridge で輸送されているが、どちらのプラットフォームでも適用されていない**。Skill には利用可能なプロパティ (既定 `true`) として載っているため、利用者は「効くもの」として書く。

Android は View の既定がスクロールバー非表示のため、**設定リストにスクロールバーが出ない**という症状で顕在化する。iOS は UIKit の既定が表示なので偶然それらしく見えているだけで、`false` を指定しても消えないはず (未検証)。

`../ColorAnalyzer` の移行作業 (`../ColorAnalyzer/kasane/changes/migrate-settingsview-to-kssettingsview`) で、Android のスクロールバーが出ないという退行として発見された。移行前の `AiForms.Maui.SettingsView` では表示されていた (実機で本番ストア版と比較して確認済み)。

### 調査済みの内容 (同じ調査を繰り返さないための記録)

- **宣言と輸送はある**: `maui/KsSettingsView.Maui/SettingsView.cs:185-187`, `:620-623` に `ScrollIndicatorVisible` (`bool?`) があり、`KsThemeSnapshot` を経て `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:484` / `Platforms/Android/KsBridgeGateway.cs:489` まで届いている
- **Android は受け取るだけ**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/core/Theme.kt:95` が `scrollIndicatorVisible: Boolean = true` を宣言しているが、RecyclerView へ適用する箇所が無い。`android/` 全体で `scrollbars` / `isVerticalScrollBarEnabled` / `scrollBarStyle` は**ヒット 0 件**、スクロールバーを持つ layout XML も無い (`res/` は drawable / themes / ids のみ)
- **RecyclerView は素のコンストラクタ**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:76` は `RecyclerView(context).apply { layoutParams / layoutManager / supportsChangeAnimations }` のみ
- **iOS も受け取るだけ**: `Theme.swift` の宣言と bridge の受け渡しのみ。`showsVerticalScrollIndicator` を設定する箇所は**ヒット 0 件**で、UIKit 既定 (表示) に乗っているだけ
- **Skill には載っている**: `skills/*/kssettingsview-maui/references/styling.md:24`, `:102` / `skills/*/kssettingsview-android/references/styling.md:81` に既定 `true` として記載がある
- **実機での症状確認**: Android 実機の設定リストで低速ドラッグ中にスクリーンショットを取得したところ、内容はスクロールしているがスクロールバーは描かれていない

### 参考: 移行元 (AiForms) の実装

`AiForms.Maui.SettingsView` は aar に `settingsViewScrollBars` スタイル (`android:scrollbars="vertical|horizontal"`) を持ち、`AiRecyclerView` のコンストラクタが `ContextThemeWrapper` 経由でそれを当てていた。aar の `attrs.xml` に「CollectionView/CarouselView renderer に scrollbars スタイルを足すため」という注記があり、**Android で明示的に有効化するのは意図的な実装**だった。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- **Android の適用方法**: `recyclerView.isVerticalScrollBarEnabled` を Theme の値で設定するのが素直だが、AiForms のように style (`ContextThemeWrapper`) 経由で当てる必要があるかは要確認。View によっては属性経由でないと効かない場合がある
- **iOS の適用**: `showsVerticalScrollIndicator` を Theme の値で設定する。現在は既定に乗っているだけなので `false` が効かない状態のはず (未検証)
- **既定値の扱い**: 現在の宣言上の既定は `true`。Android で有効化すると**既存利用者の見た目が変わる** (今まで出ていなかったスクロールバーが出る)。破壊的変更と見なすかの判断が要る
- **水平方向**: AiForms は `vertical|horizontal` の両方を有効化していた。プロパティ名が `ScrollIndicatorVisible` と方向を持たないため、両方を指すのか垂直のみかを決める必要がある
- **他に「宣言・輸送されているが適用されていない」プロパティが無いか**。同じ構造の抜けが他にもある可能性があり、Theme / CellStyle の全プロパティについて適用箇所の有無を洗う価値がある

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (適用箇所を足すだけなら小さいが、既定値の扱いしだいで破壊的変更になる)
