# Exploration: fix-android-chevron-vertical-centering

## 課題 / 動機

Android 版で Cell のテキスト（title / valueText）と右端の chevron（`ic_navigate_next`）の
垂直位置がズレて見える。iOS 版は綺麗に中央で揃っている（ユーザー報告、
「入力 Cell 5 種デモ」画面のスクリーンショット比較。2026-08-02）。

## 実測（Pixel 6a・密度 2.625・「テーマ」行）

インク（描画画素）の垂直中心をセル幾何中心 (y=1560.0) と比較:

| 要素 | before | includeFontPadding=false | chevron translateY=1 |
|---|---|---|---|
| title「テーマ」 | +4.0px 下 | +4.0px（変化なし） | +4.0px |
| value「ライト」 | +3.0px 下 | +3.0px（変化なし） | +3.0px |
| chevron | **-2.0px 上** | -2.0px | **+0.5px（ほぼ中央）** |
| テキスト↔chevron 相対ズレ | **6px ≒ 2.3dp** | 6px | **2.5px ≒ 1dp** |

## 検討した選択肢（却下案と理由を含む）

- **A: TextView に `includeFontPadding = false`** — 却下。実機検証でグリフ位置が
  1px も動かなかった（TextView の箱が 1px 縮んだだけ = この端末・フォントでは
  フォントパディングがほぼ上下対称）。当初の主犯仮説だったが実測で棄却。
- **B: chevron drawable のパスを viewport 中央へ補正** — 採用候補。
  `ic_navigate_next.xml` は 18x26 viewport に対しパス縦範囲 y=6..18（中心 12 vs
  viewport 中心 13）で **描画が 1/26 上寄り**（26dp 実寸で約 1dp 上浮き）。
  `<group android:translateY="1">` の検証パッチで chevron が中央 (+0.5px) に乗り、
  目視でもテキストと揃って見えることを確認。
  なお本 drawable は AiForms.Maui.SettingsView 原典の忠実移植であり、**この非対称は
  原典由来**（原典からの意図的 deviation となる）。
- **C: テキスト側に手動オフセット** — 却下。フォント・端末依存のマジックナンバーで破綻しやすい。
  残るテキストの沈み（幾何中心比 +3〜4px ≒ 1.3dp）は Android のフォントメトリクス
  （Roboto ascent/descent と CJK グリフ ink の非対称）由来の標準挙動で、chevron 補正後は
  目視で気にならないレベル。

## 決定事項

- 修正は B 案のみ: `ic_navigate_next.xml` のパスを viewport 縦中央へ補正する。
  A 案（includeFontPadding）は効果ゼロのため採用しない。（ユーザー確定 2026-08-02）
- 修正形式は `<group translateY>` ではなく **pathData の絶対 y 座標を +1 する書き換え**
  （ユーザー指定 2026-08-02）。パス縦範囲 y=7..19、中心 13 = viewport 中心。
  原典からの意図的 deviation は drawable 内コメントに明記。
- 実装済み・実機確認済み（chevron インク中心 +0.5px ≒ 中央、テキストとの相対ズレ 6px → 2.5px）。
  `:ks-settingsview-ui:testDebugUnitTest` 全パス。
- **追補（ユーザー確定 2026-08-02）**: chevron 補正後もテキスト側の沈み（+1.3dp）が体感で残る
  との指摘を受け、**X 案: `contentRow` へ `translationY = -1dp` の光学中心補正**を追加採用。
  - 実測: title +4.0px → **+1.0px** / value +3.0px → **±0.0px** / テキスト↔chevron 相対ズレ
    2.5px → **0.5px**（Pixel 6a「テーマ」行）
  - contentRow ごと動かすため title / valueText / EntryCell の EditText のベースライン関係は不変。
    translationY は描画時オフセットでレイアウト計算（chain・最低高さ保証）に影響しない
  - 却下した対抗案 Y（chevron をテキストの光学中心へ沈める）: テキスト↔chevron ペアは揃うが
    「全体が気持ち下」が残り、Switch 等の非 chevron アクセサリ行に効かないため
  - descriptionView はアクセサリと対にならないため補正対象外
  - 実装場所: `buildCellBaseViews` の `opticalCenterOffsetY`（CellBaseLayout.kt）。
    `:ks-settingsview-ui:testDebugUnitTest` 全パス

## ADR 候補

- 未起票: 「chevron drawable の原典からの意図的 deviation（viewport 中央補正）」。
  小さい決定のため、ADR ではなく実装時の drawable 内コメント + デルタスペックで足りる可能性が高い。
  （原典移植方針との衝突をどう扱うかは実装フェーズで判断）

## 未決の論点

- （解消済み）テキスト側の残り +1.3dp → 追補の X 案（contentRow translationY -1dp）で対処した。
- 補正量 -1dp は 16〜20sp 帯の実測に基づく固定値。利用側が極端に大きいフォントサイズを
  設定した場合は補正不足になり得る（方向は安全側 = わずかな沈みに留まる）。実害が出たら
  フォントサイズ比例への切り替えを検討する。

## UI 素材（ui/references/ の一覧と注釈）

- `pixel6a-before.png` — 修正前の実機スクショ（「入力 Cell 5 種デモ」画面）。ユーザー報告の再現。
- `pixel6a-after-pathdata-fix.png` — chevron pathData y+1 修正のみ適用後の実機スクショ。
- `pixel6a-after-text-optical-fix.png` — 最終形（chevron 補正 + contentRow 光学補正）の実機スクショ。
- `compare-theme-row-before-after.png` — 「テーマ」行の3段拡大比較
  （上: before / 中: chevron 補正のみ / 下: 最終形）。最終形でテキストと chevron が揃って見える。
- ユーザーがチャットに貼った iOS 版スクショは添付画像のため保存不可（iOS の見え方は
  「テキストと chevron が中央で揃っている」という参照基準としてのみ利用）。

## 変更級の推奨: S（理由）

- 触るのは 2 ファイルのみ: `ic_navigate_next.xml`（chevron パス補正）と
  `CellBaseLayout.kt`（contentRow の光学中心補正 + KDoc 追記）
- 公開 API 変更なし・完全可逆・実機証跡で効果確認済み
- UI 変更だがモック不要（原典スクショ比較と実測値が正）
