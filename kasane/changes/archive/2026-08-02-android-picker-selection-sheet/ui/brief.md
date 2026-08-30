# UI Brief: android-picker-selection-sheet

## 画面と状態

構造階層 (ボトムシート):

```
BottomSheetDialog
├── ドラッグハンドル (Plan B 採用で確定)
├── ヘッダー: キャンセル | タイトル (pageTitle ?: title) | OK (複数選択時のみ)
│     (文字列は OS リソース: android.R.string.cancel / android.R.string.ok — 自前文字列の同梱なし)
└── 候補リスト (縦スクロール)
    └── 行: 項目名 (左) + 選択印 (右、独自チェックマーク drawable)
```

状態:

- **単一選択**: 現在選択の1項目に選択印。行タップで即確定・閉じる
- **複数選択**: 選択済み各項目に選択印。行タップでトグル、「OK」で確定
- **上限到達** (複数選択・maxSelectedNumber): 新規チェック不可 + haptic。解除は可能
- **項目少数**: シートはコンテンツ高
- **項目多数**: 画面約半分を上限に内部スクロール。ドラッグで全展開可

## リファレンス注釈

- `references/android-current-dialog.jpg` — 現実装 AlertDialog。**置き換え対象** (この見た目にしない)
- `references/aiforms-page.jpg` — AiForms ページ形式。行レイアウト「項目名左 + チェック右」は採用。ページ遷移・全画面リストは対象外
- `references/ios-page-sheet.png` — iOS ページシート。**ヘッダー構成 (Cancel / タイトル / 完了)・行レイアウト・チェック描画の正**。高さの挙動のみ Android 慣習 (コンテンツ高 + 半分上限) に変える

## デザイントークン参照

- シート面: `Theme.cellBackgroundColor` / 行区切り: `Theme.separatorColor` (1物理 px 細線、[list-appearance](../../../concepts/core/styling/list-appearance.md))
- 選択印・強調: `PickerCell.accentColor`、未指定時は theme 既定の強調色 ([style-resolution](../../../concepts/core/styling/style-resolution.md))
- 生値はここに書かない。具体レイアウトは mock が正

## 検証条件 (動的挙動の判定基準)

視覚照合 (tasks 4.1) では静的な mock 照合に加えて以下を判定する:

- 初期表示のシート高さはコンテンツ高であり、項目多数時でも画面高の約半分を超えない
- 項目多数時はリストがシート内部でスクロールできる
- 上方向ドラッグで全展開でき、全展開中もヘッダー (キャンセル / タイトル / OK) は表示され続ける
- 上限到達状態は「タップしてもチェック数が `maxSelectedNumber` を超えない (チェック状態が変化しない)」ことで判定する (専用の視覚状態は持たない)

## 承認モック

mock/plan-b.html を採用 (approved.png、2026-08-02 オーナー承認)。

- 構成: ドラッグハンドル + ヘッダー (キャンセル=テキストボタン / タイトル中央 / OK=accent の filled pill、複数選択時のみ)
- 非対称ボタンの意図: M3 の強調度階層 (確定=強調、キャンセル=低強調)。単一選択時にキャンセルだけが残っても控えめに見える
- plan-a.html (iOS 同型ヘッダー・ハンドルなし) は不採用の対案として保存

## 視覚照合の結果

verification/ の各画像 (11枚、実機 Pixel 6a / Android 16) と approved.png を照合し **2026-08-02 オーナー最終承認**。上限到達時の haptic (拒否フィードバック) も同日オーナーが実機体感で確認済み。合意済み妥協・乖離は5件:

1. ヘッダー縦余白の配分差 (総高 48dp は mock 相当。タップ領域 48dp 確保の帰結)
2. 候補行の文字サイズは mock の 15px でなく Cell タイトルの実効値 (E2 優先の決定)
3. 角丸部の影の形は Material 既定の elevation 形状
4. 確定ボタン文字色は `Theme.backgroundColor` で描画 (deviation.md 記録)
5. ヘッダー文字サイズは Theme 連動 (タイトル = 実効タイトルサイズ +1sp / ボタン = −1sp、deviation.md 記録)

補足: 「項目多数」状態はサンプルの候補数上限 (5件) のため端末横向きで同一コード経路を成立させて検証 (`*-landscape.png`)。配色はサンプルテーマの `cellTitleColor` を AiForms 原典 (#555555) に合わせた状態が最終承認版。

画像ごとの証跡範囲: **配色 (#555555 / accent / OK 文字 = backgroundColor) とヘッダー文字サイズ (Theme 連動) の正は、最終状態で再撮影した `single-select-sheet.png` / `multi-select-sheet.png` の2枚**。他の9枚 (上限到達・項目多数・全展開・再折り畳み・初期スクロール・下端余白) は配色・文字サイズ変更前の撮影であり、**構造と動的挙動の証跡**として有効 (これらの挙動は配色・文字サイズ変更の影響を受けない)。
