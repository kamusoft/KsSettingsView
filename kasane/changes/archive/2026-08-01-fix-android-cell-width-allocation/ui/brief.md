# UI Brief: fix-android-cell-width-allocation

## 画面と状態

対象は Android Sample アプリの設定リスト画面のうち、行内 trailing (valueText / EntryCell 入力フィールド) を持つ Cell 1 行の幅配分のみ。画面遷移や loading / empty / error 状態は対象外。

- 行の構成要素: icon (任意) / title / description (任意) / 行内 trailing (valueText または入力フィールド) / Cell 級アクセサリ / hintText (任意)
- 状態の軸: title 長 (短 / 長) × 行内 trailing の種類 (valueText / 入力フィールド / パスワード) — mock で代表ケースを網羅

## リファレンス注釈

- `references/current-kssettingsview.png` (症状。取得済み — Pixel 6a / Android 16 / 画面幅 1080px):
  - **問題箇所**: EntryCell の入力フィールド表示領域が行の半分程度しか確保されず、長い入力値・パスワードが途中までしか見えない
  - 実測 (行の内容領域 x=42..1038 = 996px): メール 583px / 電話 420px / パスワード 420px / ニックネーム 441px。
    420px = 160dp × 2.625 で、撤去対象の `minWidth = 160dp` ハックの値そのもの
  - **補足**: この before は現行コードから `:app:installDebug` でビルドした APK での取得。
    先頭の「EntryCell」セクションヘッダと「名前」行が写っていないのは、取得時にリストがスクロール位置を
    保持しており画面外だったため。写っている行順 (メール→電話→パスワード→ニックネーム (callback)→
    PickerCell（単一選択）→テーマ…) は現行 `InputCellsDemoScreen.kt` と完全に一致する
- `references/original-settingsview-maui.png` (期待挙動 = 原典 AiForms 版): **未取得のまま確定**。
  - 原典期待の扱い: **承認済み mock (`mock/approved.png`) を代替の正とする**。
    理由 — 原典の配分は android/ADR-0002 でソースを直接照合済み (`CellBaseView.axml` /
    `EntryCellRenderer.cs:63-73` / `LabelCellRenderer.cs:48-60`) であり、mock はその配分をそのまま写したもの。
    スクリーンショットより一次情報として強い。MAUI Sample のビルド・実機配備は本変更のスコープ外
    (ローカルに `../AiForms.Maui.SettingsView` があるため、
    オーナーが望めば後から再取得は可能)
  - **採用**: EntryCell で title がコンテンツ幅、入力フィールドが残り幅全体を占める配分
  - 対象外: 配色・フォント・アイコン画像そのもの (Sample アプリの既存テーマに従う)

## デザイントークン参照

- スタイル解決順序・トークン: [concepts/core/styling/style-resolution.md](../../../concepts/core/styling/style-resolution.md)
- 行寸法・icon 枠・最低行高: [concepts/core/styling/cell-row-layout.md](../../../concepts/core/styling/cell-row-layout.md)
- mock の配色は Sample アプリ既存テーマ (amber 系 accent) の近似。生値はトークンの代用であり実装の正ではない
- **mock の規範範囲**: mock が「見た目の正」として規定するのは主行の**幅配分の配置関係** (EntryCell: title=コンテンツ幅+入力フィールド=残り幅 / valueText 系: valueText=コンテンツ幅+title=残り幅、および両者が重ならないこと) のみ。spacing・寸法・フォント・配色の生値は非規範で、現行実装のトークン解決値を維持する。実装後の視覚照合は配置関係を比較する

## 承認モック

mock/plan-a.html を採用 (approved.png、2026-08-01 ユーザー承認)。

- EntryCell: title はコンテンツ幅、入力フィールドが主行の残り幅全体 (パスワード・placeholder も同じ配分)
- 長い title の EntryCell: 入力フィールドは固定最低幅で title を押し出さず、残り幅の範囲に収まる (原典 LinearLayout 同型)
- valueText 系: valueText はコンテンツ幅、title が残り幅で末尾省略
- 案は plan-a の1案のみ (android/ADR-0002 で配分方式が確定済みのため。config `mock-variants: 2` からの合意済み逸脱)

## 照合結果 (2026-08-01)

実機 Pixel 6a (Android 16 / 画面幅 1080px / density 2.625) で after を取得し `ui/verification/` に保存。
`uiautomator dump` の bounds で数値照合し、`mock/approved.png` と配置関係を照合した。

### after 実測 (入力 Cell 5 種デモ / 行の内容領域 x=42..1038 = 996px)

レビュー後の `titleView.paddingEnd = 6dp` (= 15px) 追加を含む最終状態。

| 行 | title bounds (幅) | 入力フィールド bounds (幅) | before の幅 |
|---|---|---|---|
| 名前 | [42,527][145,592] (103) | [145,498][1038,622] (**893**) | (before ではスクロール位置により画面外) |
| メール | [42,684][189,749] (147) | [189,655][1038,779] (**849**) | 583 |
| 電話 | [42,841][145,906] (103) | [145,812][1038,936] (**893**) | 420 |
| パスワード | [42,998][277,1063] (235) | [277,969][1038,1093] (**761**) | 420 |
| ニックネーム (callback) | [42,1155][524,1220] (482) | [524,1126][1038,1250] (**514**) | 441 |

全行で「入力フィールドの左端 = title の右端」「入力フィールドの右端 = 主行の右端 (1038)」
「title 幅 + 入力フィールド幅 = 996」が成立。固定 420px (=160dp) は消滅した。

`paddingEnd` 追加前の実測は title が各 15px 狭く (名前 88 / メール 132 / 電話 88 / パスワード 220 /
ニックネーム 467)、入力フィールドが各 15px 広かった (908 / 864 / 908 / 776 / 529)。padding は
View 幅に含まれるため合計 996 の等式は不変で、6dp 分は title の**文字と行内 trailing の間の余白**
として現れる。

valueText 系 (chevron あり、主行 x=42..991): テーマ [42..859]+[859..991] /
通知種別 [42..746]+[746..991] / サイズ [42..883]+[883..991] — valueText はコンテンツ幅、
title が残り幅、両者は非重なり。

### mock (approved.png) との照合 — 規範範囲 = 主行の幅配分の配置関係

| 観点 | 判定 | 備考 |
|---|---|---|
| EntryCell: title=コンテンツ幅 / 入力フィールド=残り幅全体 | 一致 | 名前・メール・電話・ニックネームの 4 行で bounds 確認 |
| パスワードも同じ配分 | 一致 | 220 + 776 = 996 |
| valueText 系: valueText=コンテンツ幅 / title=残り幅 | 一致 | Picker 系 3 行で bounds 確認 |
| title と行内 trailing が重ならない | 一致 | 全行で title.right == trailing.left。さらに title の `paddingEnd = 6dp` で文字同士も接しない |
| 長い title の EntryCell で入力欄が title を押し出さない | 実機未確認 | Sample に長い title の EntryCell 行が無いため、Robolectric テスト (`CellRowWidthAllocationTest`) で検証 |
| 長い title の valueText 行の末尾省略 | 実機未確認 | 同上 (Sample に該当行が無い)。"…" グリフの描画位置は未確認で、Robolectric (`@GraphicsMode(NATIVE)`) の `getEllipsisCount > 0` までを検証 |

非規範 (spacing・寸法・フォント・配色) は現行実装のトークン解決値のまま。mock の生値とは意図的に不一致。

### 視覚リグレッション (タスク 4.4)

- 基本 Cell 7 種デモ: CommandCell (icon+description+chevron) / LabelCell (valueText+description) /
  SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / ButtonCell いずれも配置不変。
  description は Cell 級アクセサリの leading 側で折り返したまま (例: SwitchCell の description は
  Switch の左端 901 で折り返す)。
- 共通フィールド統合デモ: hintText の右上 float、Picker 系の description とアクセサリの非重なり、
  ButtonCell の aux あり (通常レイアウト) / aux なし (全幅・中央揃え) の両方が同一画面で成立。
  aux なしの「ログアウト」は主行 [42..1038] 全幅で中央揃え、aux ありの「登録」は
  title [147..950] + valueText [950..1038] で通常レイアウト。
- EntryCell の IME: Cell 本体 (title 領域) タップで EditText にフォーカスが移り IME が表示され
  (`mInputShown=true`)、入力が既存値に追記された。textAlignment END の右寄せも維持。

### トークン候補

なし (本変更は配置構造のみで、新しい色・寸法の生値を導入していない)。

### spec との差分 (正は deviation.md)

デルタスペックからの合意済み差分は **`../deviation.md` が正**。本 brief では列挙しない。
該当するのは「title の 1 行 + 末尾省略化」「ButtonCell の `titleAlignment` 実効化」
「title と行内 trailing の 6dp クリアランス」「原典期待スクリーンショットの未取得」の 4 件で、
いずれもオーナー承認済み。

### 実装上のメモ (spec 差分ではない)

- ベースライン揃えの手段が変わった: 旧 `ConstraintSet.BASELINE` 紐付け → `LinearLayout` の
  `isBaselineAligned` (水平 LinearLayout の既定挙動)。入れ子化で ConstraintLayout の baseline 制約が
  使えなくなったための代替。
- `ButtonCell` の `titleAlignment` 実効化 (deviation.md) の実機確認: Sample の aux あり ButtonCell は
  `titleAlignment = START` 指定のため既定 CENTER の見た目を観測できない。**Sample を一時的に
  CENTER へ書き換えて撮影し、撮影後に元へ戻した** (`ui/verification/after-buttoncell-center-alignment-pixel6a.png`)。
  title 領域 [147..950] の中で、START では x=147 に左寄せ、CENTER では中央に描画されることを確認。
- Robolectric での検証範囲:
  - 末尾省略は `@GraphicsMode(NATIVE)` により `getEllipsisCount > 0` まで実レンダリングで検証できる
    (legacy graphics では ellipsize が動作せず常に 0)。ただし "…" グリフの**画面上の位置**は未検証。
  - singleLine な TextView の**テキストの水平位置**も Robolectric で検証できる。
    `isSingleLine = true` は `setHorizontallyScrolling(true)` を伴い `Layout` 幅が `VERY_WIDE` になるため
    `Layout.getLineLeft` 単体では View 座標にならないが、`viewTreeObserver.dispatchOnPreDraw()` で
    `TextView.bringTextIntoView()` の `scrollX` 補正を発火させれば `getLineLeft(0) - scrollX` が
    実描画位置になる (`root.draw()` だけでは補正されない)。`titleAlignment` の実効化は
    この実位置アサーションでテストし、実機証跡と二重に担保している。
    ※ 当初は「Robolectric では検証不能」と記録していたが、レビューでの実測により誤りと判明したため訂正した。
