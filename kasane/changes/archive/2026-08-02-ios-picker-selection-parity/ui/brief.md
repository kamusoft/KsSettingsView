# UI Brief: ios-picker-selection-parity

## 画面と状態

構造は現行のページシートを変えない (構造変更なし):

```
UINavigationController (page sheet)
├── ナビゲーションバー: Cancel | タイトル (pageTitle ?: title — fallback は本 change で追加) | 完了 (複数選択時のみ)
│     → ボタン色 = accent 解決値 / タイトル文字色 = 実効タイトル色 / フォントサイズはシステム既定
└── UITableView (plain)
    └── 行: 項目名 (左、実効タイトル色/フォント) + チェックマーク (右、accent)
        → 行背景 = 実効セル背景色 / 区切り線 = Theme.separatorColor / タップ = Theme.selectedColor
```

状態:

- **単一選択**: 選択中の1項目にチェック。行タップで即確定・閉じる (挙動は現行どおり)
- **複数選択**: 選択済み各項目にチェック。完了で確定 (挙動は現行どおり)
- **項目多数**: 選択中の項目 (複数は最小 index) が可視領域の中央付近に来た状態で開く
- **未選択/範囲外のみ**: 先頭から表示

## リファレンス注釈

- [android-picker-selection-sheet/ui/verification/](../../android-picker-selection-sheet/ui/verification/) の `single-select-sheet.png` / `multi-select-sheet.png` (最終版) — **配色トークンの適用結果の正** (同じ Theme で iOS も同等の見え方になる)
- [android-picker-selection-sheet/ui/references/ios-page-sheet.png](../../android-picker-selection-sheet/ui/references/ios-page-sheet.png) — iOS 現状の構造 (この構造は維持。変わるのは配色とスクロール位置)

## デザイントークン参照

- 行タイトル: `EffectiveStyle.effectiveTitleColor` / `effectiveTitleFont` ([style-resolution](../../../concepts/core/styling/style-resolution.md))
- 背景: `effectiveBackgroundColor` / 区切り線: `Theme.separatorColor` / ハイライト: `Theme.selectedColor` ([list-appearance](../../../concepts/core/styling/list-appearance.md))
- 強調色: `effectiveAccentColor` の3段解決
- 生値はここに書かない

## 承認モック

mock/plan-a.html を採用 (approved.png、2026-08-02 オーナー承認)。

- mock-variants は config 上 2 案だが、案の分岐軸 (ナビバー適用範囲・スクロール位置) は proposal の方向性確認で消化済みのため 1 案で提示し承認された
- 構造は現行ページシートのまま。mock が表すのは配色トークンの適用結果と初期スクロール位置

## 照合結果 (実装フェーズで追記)

verification/ の各画像と approved.png を照合 (2026-08-02、1 周で収束)。撮影環境は iPhone 17 Pro シミュレータ / iOS 26、Sample アプリの MAUI 互換 Theme (明色系。`cellAccentColor` / `cellTitleColor` / `separatorColor` / `selectedColor` を指定したもの)。verification 3点 + 濃色テーマ補足1点をオーナーへ提示し、**2026-08-02 オーナー承認済み**。

| 画像 | 状態 | 照合結果 |
|---|---|---|
| `verification/single-select-sheet.png` | 単一選択 | 構造・トークン・意図とも mock 一致 |
| `verification/multi-select-sheet.png` | 複数選択 | 構造・トークン・意図とも mock 一致 |
| `verification/many-items-initial-scroll.png` | 項目多数 (50 件・選択 index 30) | 選択項目「56 px」が可視領域の中央付近に来た状態で開く。mock 一致 |

- タップハイライト (`Theme.selectedColor`) は押下中のみの過渡状態でスクリーンショット化できないため、視覚照合ではなく単体テスト (`selectedBackgroundView.backgroundColor` の検証) で確認した
- 項目多数の状態は Sample に該当 Cell が無いため、Sample へ一時的に 50 件の PickerCell を足して撮影し、撮影後に Sample を元へ戻した (Sample 側の変更は残していない)

## プラットフォーム由来の見え方の差 (合意事項ではなく観測結果)

- **ナビバーのボタン形状**: mock は「Cancel」「完了」のテキストラベルだが、iOS 26 の実機描画では `.cancel` / `.done` システムアイテムが記号ボタン (✕ / 塗り丸 + ✓) としてレンダリングされる。ボタン構成を現行維持する方針 (proposal) の下で**本 change 以前から同じ描画**であり、本 change が持ち込んだ差ではない。解決済み強調色はこの記号ボタンへ正しく適用されている (✕ は線色、確定は塗り色)
- **トークン候補**: 新規に必要になった生値はなし (`EffectiveStyle` の既存 resolver のみで充足)

## ナビバー背景の確認結果

- ナビバーの背景色は spec / brief のいずれも規定していないためシステム既定のままとした。実装はタイトル文字色のみを差し替え、背景はバーの現在有効な appearance を引き継ぐ
- 濃色テーマ (`cellBackgroundColor` を暗色にした Theme) を Sample へ一時適用して上端表示を撮影し確認した (`verification/navbar-dark-theme-check.png`。撮影後に Sample は元へ戻した)。ページシート上のナビバーは背景が透過して候補リストの実効セル背景色が透けるため、list 背景とナビバー背景は揃う。明度差は生じない
- 承認済み mock と照合する3点 (single / multi / many-items) は、この appearance の作り方を変えても描画が変わらないことを画素比較で確認済みのため撮り直していない
