# Exploration: perf-ios-customcell-configuration-reuse

調査日: 2026-08-16 / 出発点: perf-android-customcell-composition-reuse (完了) の2段構え第2弾。
「iOS は計測スパイクで効果を確定してから採否判断 (効果が小さければ『やらない』も正解)」
(2026-08-15 オーナー承認) に基づく計測スパイク。

## 課題 / 動機

- iOS `CustomCellView.prepareForReuse` の `contentConfiguration = nil` により、リサイクル毎に
  SwiftUI hosting 階層が全破棄・全再構築されている (`ios/Sources/KsSettingsViewUI/CustomCellView.swift:106`)
- Android 版 (ReusableContent 方式) と同型の「中身のリサイクル」を iOS でも効かせられるか、
  効かせる価値があるかを実測で確定する

## スパイク設計

- 使い捨てブランチ `spike/ios-customcell-configuration-reuse` 上で計装 (マージ禁止):
  - ライブラリ: `KsCustomCellSpike` (起動引数 `-SpikeVariant A|B|C` で切替) +
    bind 数 / contentView 再生成数カウンタ (render 直後と layoutSubviews で identity 追跡)
  - サンプル: CustomCell 200 行のストレス画面 + CADisplayLink 等速フリックドライバ
    (2500pt/s × 4パス往復) + フレーム間隔計測 (hitch = 期待間隔の1.5倍超) +
    プロセス CPU 時間差分 (getrusage) + 結果 JSON を stdout へ (`-SpikeAutorun 1`)
- 変種:
  - **A**: 現行 (prepareForReuse で `contentConfiguration = nil`) — ベースライン
  - **B**: nil 化撤廃のみ (同型 UIHostingConfiguration 再代入 = in-place 更新を期待)
  - **C**: B + hosted content に `.id(cell.id)` (SwiftUI identity の行間分離)
- 備考: `render(cell:)` は cell 本体を受け取るため `custom.id` (UUID) がそのまま使える。
  前回探索の懸念「render が安定 ID を受け取っていない」は杞憂だった

## 計測結果

### pixie5 (iPhone 15 / iOS 26.6, Release, 各3〜4回)

| variant | binds | creations | フレーム | hitch | CPU 時間 (平均) |
|---|---:|---:|---|---|---:|
| A (現行) | 739 | **739** | avg 16.64ms / 60fps 張り付き | 0 | 6.31s |
| B (nil 撤廃) | 739 | **7** | 同上 | 0 | 6.35s |
| C (B + .id) | 739 | **7** | 同上 | 0 | 6.32s |

- **機構: 成立** — nil 化をやめるだけで hosting content view の in-place 更新が効き、
  再生成が bind 毎 → プール成長分 (7) のみに激減。`.id(cell.id)` を付けても
  content view インスタンスの再利用は壊れない (iOS 18 系で in-place 前提が崩れる、
  という事前報告は iOS 26.6 では非該当)
- **効果: 測定不能** — 60fps 張り付き (hitch 0) で、CPU 時間も 3 変種で誤差の範囲内。
  hosting 階層の再生成コストは bind 毎の SwiftUI content 更新コストに埋もれる規模しかない

### pixie4 (iPhone 11 / iOS 16.6.1, Release, 各2回)

| variant | binds | creations | フレーム | hitch | CPU 時間 |
|---|---:|---:|---|---|---:|
| A (現行) | 734 | **734** | avg 16.67ms / 60fps 張り付き | 0 | 6.79s / 6.73s |
| B (nil 撤廃) | 734 | **6** | 同上 | 0 | 6.92s / 6.65s |
| C (B + .id) | 734 | **6** | 同上 | 0 | 6.73s / 6.59s |

- **機構: iOS 16.6.1 でも成立** (deployment target 下限側で in-place 更新が効く)
- **効果: A13 (iPhone 11) でも測定不能** — 60fps 張り付き・hitch 0・CPU 差はノイズ範囲
  (±2%・方向も不定)。ライブラリの想定利用形態 (設定画面の典型的な行 content) では
  hosting 階層の再生成コストが SwiftUI content 更新コストに完全に埋もれる

## スパイクの結論

- **機構は成立するが、効果が存在しない**。iOS の `contentConfiguration = nil` 撤廃は
  「できるが速くならない」変更であり、事前合意のゲート「効果が小さければ『やらない』も正解」
  に該当する
- 採用した場合に増えるもの: プール滞在中の hosting 階層のメモリ、`.id(cell.id)` の
  引き回し (B 単独は @State の行間持ち越しリスクがあるため C 形が必須)、
  MAUI Bridge 退役順序レースの再検証義務 — 対価ゼロでこれらを背負うことになる
- Android (Composition 再構築が実測レベルで重い) と iOS (UIHostingConfiguration が
  実測で十分軽量) の非対称が数字で確定した。Apple の「configuration は軽量」という
  言及は実測と整合

## 検討した選択肢 (前回探索から引き継ぎ)

- **案A: `prepareForReuse` の nil 化撤廃 (+ `.id` 分離)** — 機構は iOS 26.6 で成立を実測確認。
  効果量が閾値に届くかが採否の分かれ目 (pixie4 待ち)
- **案B (却下済み): 自前 UIHostingController 保持** — Apple 非サポート明言のため不採用 (前回探索)

## 決定事項

1. **iOS 版は見送り** (2026-08-16 オーナー確定): 機構成立・効果ゼロの実測結果に基づき、
   変更を起こさず終了する
2. 決定は ios ADR として記録する (オーナー指示)

## ADR 候補

- **作成済み (proposed)**: [ios/ADR-0002](../../decisions/ios/0002-customcell-hosting-recreation-accepted.md)
  — CustomCell の hosting 階層はリサイクル毎の再生成を維持する

## 未決の論点

- なし (クローズ)

## UI 素材

なし (挙動・性能のみ。見た目の変更なし)

## 変更級の推奨: 見送り (変更を起こさない)

- 機構成立・効果なしが両端末 (iOS 16.6.1 / 26.6) で実測確定したため、
  変更を起こさず終了することを推奨する
