# UI Brief: add-cell-types-custom

## 画面と状態

対象は **CustomCell デモ画面**（iOS / Android 同構成。sample-parity 規約に従い両プラットフォームで同じパラメータを渡す）。

構造階層:

- CustomCell デモ画面
  - Section「インライン CustomCell」— content + builder 直書きの例、および content なし静的糖衣の例
  - Section「再利用（SliderCell）」— CustomCell を返すラップ関数の例（スライダー 2 行）
  - Section「動的高さ」— content 内の操作で展開/折りたたみするセル（状態: 折りたたみ / 展開）
  - Section「showArrow / onTap」— Disclosure Indicator 付きセルと行タップの例
  - Section「スクロール耐性（ダミー）」— 同型のインライン CustomCell 40 行（アクセント 6 色循環）。行の再利用（リサイクル）で表示・listener が混線しないことをスクロールで確認するための領域（ユーザー指示 2026-08-03）

状態: 通常表示のみ（loading / empty / error は存在しない静的デモ）。動的高さセルのみ「折りたたみ」「展開」の 2 状態を持つ。

ライブラリ本体の見た目の新規要素は Disclosure Indicator の合成描画のみで、**既存 Cell の chevron と同一の見た目・位置**であることが視覚基準（mock ではなく既存実装が正）。

## リファレンス注釈

references/ なし（探索・提案を通じて画像素材は提供されていない）。

## デザイントークン参照

- Sample のテーマは `samples/android/.../SampleTheme.kt` / `samples/ios/.../SampleTheme.swift` の MAUI 互換 Theme を使用する（基本 Cell 7 種・入力 Cell 5 種のデモと同一。ユーザー指示 2026-08-03）
- プラットフォーム間のパラメータ一致は `concepts/cross/conventions/sample-parity.md` の規約に従う
- mock 中の色は SampleTheme の定義値を参照した近似であり、実装は SampleTheme 定数を正とする（生値の二重管理を作らない）

## 承認モック

mock/plan-a.html（ベーシック構成 + スクロール耐性ダミーセクション）を採用（approved.png、2026-08-03 ユーザー承認）。plan-b.html（リッチ構成）は不採用の対案として保存のみ。

mock は**プラットフォーム中立の共通デモ構成**であり、iOS / Android どちらか一方の見た目の正ではない。実装時の視覚照合は「セクション構成・content の内容・chevron の既存 Cell との一致」を基準に、各プラットフォームのデモ画面と照合する（second-opinion-001 指摘 #10 の明確化）。

## 照合結果

証跡画像とキャプションは `ui/verification/index.md` にある。

- **2026-08-03**: Android 実機（Pixel 6a / Android 16、Pixel 4a / Android 13）と iOS 26.5 シミュレータ（iPhone 17）で `approved.png` と照合。構造（セクション構成・行の並び・content の内容）・トークン（SampleTheme の色）・意図（chevron が既存 Cell と一致、動的高さで後続行が押し下がる）はいずれも一致。ピクセル一致は基準にしていない。差異 4 点は「mock との差異」として `verification/index.md` に列挙済み。
- **2026-08-04（iOS 修正サイクル 1）**: 動的高さの展開アニメーションの修正について、修正前・修正後を同一操作で画面録画してフレーム比較（`verification/` の `08-*` / `09-*`）。修正後の静止状態が `01` / `02` / `03` と同一であること（＝配置方式の変更による退行がないこと）を `10-*` / `11-*` で確認。
- **2026-08-04（修正サイクル 2 / iOS + Android）**: 無効時の淡色化を iOS 側にも適用し、Sample の Slider のアクセント色を両プラットフォームで統一。iOS 26.5 シミュレータ（iPhone 17）と Android 実機（Pixel 6a / Android 16、Pixel 4a / Android 13）で撮影し、無効行と Slider の色を左右に並べて照合（`verification/compare-01` / `compare-02`）。無効行のテキスト濃度は実測で一致（ラベル iOS 190 / Android 191、数値 iOS 216 / Android 217）。iOS 全体・Android 全体の画面（`12` / `15` / `pixel4a-05`）が既存の `01` 系と同一であることを確認し、退行がないことを併せて記録した。

## トークン候補

- **`demoAccentPalette` / `demoPillBackground`(#FAF3D9) / `demoExpandBackground`(#FAF7EE) / `demoExpandText`(#777777)** — CustomCell デモの content（利用者が書く任意 View）が使う色。Cell の内装は利用者責務のため Theme には載らないが、iOS / Android で同一 RGBA を渡す必要があるため両 Sample の `SampleTheme` に一元化した。ライブラリ側のデザイントークンに昇格させる性質のものではない（Sample 固有）。

## 合意済み妥協

- **Section ヘッダの大文字表記**: mock は iOS 標準リストの自動大文字化を模して「インライン CUSTOMCELL」等と書いているが、本ライブラリのヘッダは文字列をそのまま描画する。既存デモと揃えて自然な大小混在で実装した。
- **スライダーの見た目**: mock は丸ノブ + 4px トラック。実装は SwiftUI `Slider` / Material 3 `Slider` の標準描画。mock がプラットフォーム中立である以上、照合基準外。
- **無効スライダーの淡色化**: 両プラットフォームとも `isEnabled = false` のとき content 全体を alpha / opacity 0.38 で淡色化する（オーナー指示。`deviation.md` に記録済み）。テキストの濃度は実測で一致するが、iOS の SwiftUI 標準コントロールだけは `.disabled` による淡色化と opacity が重なり実効 alpha 約 0.19 と 2 倍薄くなる。両プラットフォームで「無効に見える」ことを優先した合意済みの副作用。
- **Slider の thumb と inactive track の色**: **active track** は両プラットフォームとも `SampleTheme.mauiAccent` を指定して揃えた。一方 **thumb** は Android のみ `mauiAccent` を指定でき、iOS は SwiftUI の `.tint` が active track にしか効かないため標準の白い capsule のまま。**inactive track** はどちらも明示指定せず標準既定のまま（iOS = システムのグレー / Android = Material 3 の淡紫）。いずれも OS 標準コントロールの描画差であり、sample-parity 規約が「Sample が明示的に渡していないパラメータの既定値の platform 差」を許容している。片側だけ指定すると「標準コントロールを素のまま置いた例」という意図が崩れるため揃えていない。
- **行タップカウンタの初期値**: mock は「3 回」だが実装の初期値は 0。3 回タップした状態が mock と一致することを証跡で確認した。
- **展開の遷移中に content の上端を行の上端へ固定する（iOS）**: mock は静止状態しか表さないため遷移の正は持たない。`UIHostingConfiguration` は行に収まらない content を行の中央に置くため、行が伸びきるまでの間 content が上下へはみ出し、展開が「一度上へ飛び出してから落ちてくる」動きに見えていた（オーナー指摘 2026-08-03）。**静止時は標準 Cell と同じ縦中央のまま、収まらないときだけ上端揃え**に切り替える配置へ変更した（`CustomCellHostedContent` の `CustomCellRowPlacement`）。静止状態の見え方は変わらない。
