# Proposal: fix-cell-icon-size-parity

## Why

`Theme.cellIconSize` / `cellIconRadius` (および `CellStyle.iconSize` / `iconRadius`) は「icon 列を指定サイズの正方形に揃える」公開契約だが、両 platform で実現漏れがある。

- **Android**: `EffectiveStyle.effectiveIconSize` / `effectiveIconRadius` は解決関数とテストだけが存在し、`EffectiveStyle` の実効値にも `applyCellBaseLayout` にも乗っていない。`buildCellBaseViews` が `iconView` を固定サイズで構築するため、Theme に何を渡しても既定サイズ・角丸なしになる
- **iOS**: `iconImageView` のサイズ制約 (priority 750) より hugging / compression resistance (1000、垂直 CCR も 1000) が強く、SF Symbols のように字形ごとに幅が違う画像では icon 列の幅が行ごとに変わり、title の開始位置がずれる。既定サイズ (未指定) でも intrinsic size が勝つため、未指定の利用者にも症状がある

加えて、同じ `stackH` の優先度を組み替えるにあたり、**行幅が足りないときの主行の配分が iOS と Android で逆**であることが分かった (iOS: valueText が先に省略され title が残る / Android: title が先に省略され valueText が残る)。移植元 AiForms の時点で逆だったものをそれぞれ忠実に移植した結果で、概念文書の「iOS も同じ配分」は事実と異なっていた。icon 枠を譲らない契約は「足りないとき誰が譲るか」が決まって初めて閉じるため、同じ change で両 OS を **title を守り valueText を省略する** 側 (iOS 現行) へ揃える ([core/ADR-0026](../../decisions/core/0026-main-row-protects-title-truncates-value.md))。

初回リリース前に直す。利用者向けドキュメントに書く `cellIconSize` の説明と実挙動が食い違ったまま公開すると、後から直したときの見た目変化が利用者への影響になる。出典と証跡は [exploration.md](exploration.md)、相方レビューの指摘は [second-opinion-spec-001.md](second-opinion-spec-001.md)。

## What Changes

探索の決定 (角丸は正方形枠に対して適用する — [core/ADR-0025](../../decisions/core/0025-cell-icon-radius-applies-to-square-frame.md)) に従い、両 platform を「解決済み icon size の正方形枠 + 枠に対する角丸」へ揃える:

1. **Android** (`EffectiveStyle.kt` / `CellBaseLayout.kt`): `EffectiveStyle` に解決済み icon size / radius を載せ、`applyCellBaseLayout` の icon 解決部で `iconView` の LayoutParams を正方形枠へ更新し、`clipToOutline` + `ViewOutlineProvider` で枠に対して角丸 clip する (radius の変更・解除は再 bind で再評価)。Theme 変更は既存の rebind 経路 (`PAYLOAD_THEME`) に乗るため新しい更新経路は作らない
2. **iOS icon 枠** (`KsListCellBase.swift` / `CellBaseLayout.swift`): `iconImageView` のサイズ制約を **表示中は `.required`** にし、icon を非表示にするとき (icon なし Cell の bind・`prepareForReuse`) は制約を **deactivate** する — UIStackView が非表示の arranged subview に張る required 制約との衝突を、優先度の賭けではなく制約の有効/無効で避ける。hugging / CCR は両軸とも下げる。階層コメントの優先度記述も実値に合わせる
3. **主行の幅配分 (Android を iOS へ揃える)** (`CellBaseLayout.kt`): Android の既定配分を title `wrap_content` (主行幅上限で末尾省略) / valueText `0dp + weight 1 + gravity END` (残り幅・末尾省略) へ入れ替える。行内 trailing がない Cell では title を `0dp + weight 1` に戻して主行全幅を使わせる (bind 時に切り替え。ButtonCell の中央揃えを維持)。EntryCell の配分 (title コンテンツ幅・フィールド残り幅) は既定と同じ形になるため、固有の weight 付け替えは整理してよい。iOS は現行の優先度のまま契約を満たしているので無変更、テストで固定する。既存の `CellRowWidthAllocationTest` の期待値 2 本 (title 先行省略・valueText 上限省略) は新契約に反転する
4. **無効値の扱い (両 OS の解決関数)**: icon size は正の有限値のみ有効、radius は 0 以上の有限値のみ有効。それ以外は未指定として次の段へ解決する (既存の `rowHeight > 0` / `cellTitleFontSize > 0` と同じパターン。Android で負の dp が `LayoutParams` の予約値に化けるのを防ぐ)
5. **テスト**: Android は `applyCellBaseLayout` 後の `iconView` の LayoutParams / scaleType / outline (設定・変更・解除) と無効値を、iOS は intrinsic 幅の異なる画像を並べた Cell の `iconImageView` 実寸・非表示時の制約状態・`applyTheme(_:)` 経由の更新・主行の幅配分 (title 先行省略 / valueText 上限省略 / EntryCell) を検証する
6. **視覚証跡**: 両 OS で修正前後の A/B を撮り `ui/verification/` へ保存する (process L-003)。Android は Section 装飾デモで iOS と同じ見た目になること、iOS は共通フィールドデモ (SF Symbols) で title の開始位置が揃うこと (基準線の注釈付き) と、Dynamic Type 最大での幅配分を確認する

影響する能力: settings-view-android-ui / settings-view-ios-ui

## Non-Goals

- 描画矩形への角丸追従、aspect fill への変更 (ADR-0025 で却下)
- 非正方形画像で radius が余白より大きいとき、および radius が枠の半辺を超えるときの clamp / 警告 (ADR-0025 の Consequences で許容。platform の描画系に委ねる)
- `KsImage.SystemName` の Android 解決 (現行どおり非表示 fallback。[ks-image.md](../../concepts/core/cells/ks-image.md))
- 既定値の変更 — icon size / radius の既定は現状で両 OS とも同じ生値 (size 24 / radius 0 = 角丸なし) であり、これを維持する。「既定は iOS と同じ生値」を契約として spec に明文化するだけで、値は動かさない
- サンプルの `SampleIconBadge` (色地の正方形バッジ生成) の削除 — 本体修正後も色地バッジというデザイン選択として残す
- CustomCell (共通行レイアウトの適用除外)
- MAUI の変更 (Bridge は値を渡すだけで、描画は native 本体が担う)
- iOS の主行の優先度変更 (現行が ADR-0026 の契約を満たしているため無変更。テストで固定のみ)

## Impact

- 公開 API 変更なし (値域の制約は「無効値は未指定扱い」で、正常値の利用者には影響しない)
- **見た目が変わる利用者**: (a) `cellIconSize` / `cellIconRadius` / `CellStyle.iconSize` / `iconRadius` を指定していた利用者 — Android は指定が効くようになる、iOS は SF Symbols の列幅が揃う。(b) **iOS で icon size 未指定の利用者も**、intrinsic size が既定枠と異なる画像 (SF Symbols・既定より大きい `UIImage`) の描画と title の開始位置が変わる (既定値の生値は不変だが、これまで既定枠が効いていなかったため)。(c) **Android で長い valueText を持つ行**は、省略される側が title から valueText へ変わる (iOS と同じになる)。iOS の幅配分は変化なし
- iOS の icon 枠制約は表示中 `.required`。狭幅では主行 (valueText → title の順) が譲るため icon 枠・Cell 級アクセサリは潰れない。icon 非表示時は制約を deactivate するので UIStackView の非表示制約と衝突しない
- 蒸留時の申し送り: [style-resolution.md](../../concepts/core/styling/style-resolution.md) の「Android の現行共通行は … 反映を保証しない」但し書きの削除と ADR-0025 の契約・無効値の扱いの追記。[cell-row-layout.md](../../concepts/core/styling/cell-row-layout.md) の「主行の幅配分」を ADR-0026 の契約 (title を守り valueText を省略) に書き直す。android/ADR-0002 の配分項目は置き換え済み (index 注記)

## 級: M

修正は公開 API 変更なしだが、2 platform 同時・利用者に見える変更であり、両 OS 共通契約 (icon 枠・角丸・幅配分 = ADR-0025 / 0026) をデルタスペックで固定して verify と両 OS の視覚証跡で担保する価値があるため。

domain: cross
