# レビュー結果: fix-android-cell-width-allocation (002 回目)

**日付**: 2026-08-01
**判定**: APPROVED

**指摘件数**: Critical 0 / Major 0 / Minor 1 / Suggestion 3

## サマリー

1 周目の指摘 6 項目 (A〜F) は**すべて実装に反映されている**。未解消のまま残っている指摘はない。
副作用の持ち込みも検出されなかった: ビルド成功、`./gradlew test --rerun-tasks` で 535 件全成功、
1 周目に「問題なし」と確認した回帰観点 (ベースライン揃え / 縦チェーン / ButtonCell の ConstraintSet 切替 /
accessory 配置 / android/ADR-0001 の IME 差分ガード) はいずれも維持され、うち accessory 配置については
今回 **専用の自動テストが 2 本追加されて検出力が上がっている**。`samples/` に残存差分もない。

一方、A の「aux あり + CENTER の title 位置を検証するテスト」の設計根拠となった技術判断
(「Robolectric では実グリフ位置を検証できない」) は、**レビュー側で実測した結果、誤りである**ことが判明した。
`ViewTreeObserver.dispatchOnPreDraw()` を 1 行足せば `TextView.bringTextIntoView()` による `scrollX` 補正が
Robolectric でも再現され、実描画位置を px 単位で断定的に検証できる。実機証跡が位置を担保しているため
ブロッキングではないが、テストが「オーナーが要求した検証」より一段弱い状態で止まっているので Minor とする。

## 1 周目の指摘への対応状況

| # | 項目 | 対応 | 判定 |
|---|---|---|---|
| A | ButtonCell の `titleAlignment` 実効化を受け入れ + テスト追加 | `deviation.md` 2 件目に記録。`CellRowWidthAllocationTest.kt:625` を追加。実機証跡 `after-buttoncell-center-alignment-pixel6a.png` を取得 | ✅ 解消 (テストの質について Minor 1 件) |
| B | title と行内 trailing の 6dp クリアランス | `CellBaseLayout.kt:99-102, 150` で `paddingEnd = 6dp` を追加。`deviation.md` 3 件目に記録 | ✅ 解消 (副次的な観察を Suggestion 1 件) |
| C | accessory + description の回帰テスト不足 (相方指摘) | `CellRowWidthAllocationTest.kt:462` (SwitchCell) / `:507` (PickerCell) を追加 | ✅ 解消 |
| D | 末尾省略の証跡記述の矛盾 (相方指摘) | `@GraphicsMode(NATIVE)` 化して `getEllipsisCount(0) > 0` を検証。KDoc (`:77-84`) と `ui/brief.md` の照合表・実装メモを「グリフ位置は未検証」で統一 | ✅ 解消 |
| E | `contentRow.visibility = VISIBLE` の到達不能コード | 削除済み (`grep contentRow.visibility` → 0 件) | ✅ 解消 |
| F | brief.md の記述訂正 2 件 | (1) before を「現行コードからビルドした APK / 先頭 2 行はスクロールで画面外」と訂正。(2)「合意済み妥協 / 申し送り」節を廃し「spec との差分 (正は deviation.md)」へ置換 | ✅ 解消 |

**未解消の 1 周目指摘: なし。**

## 指摘事項

### [🟡 Minor] A のテストが「実グリフ位置を検証できない」という前提は誤り — `dispatchOnPreDraw()` で検証可能

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:604-685`
(KDoc `:618-622` および `kasane/changes/fix-android-cell-width-allocation/ui/brief.md` 「Robolectric の検証限界」)

**問題点**:

実装側の技術判断は次のように述べられている。

> `isSingleLine = true` は `setHorizontallyScrolling(true)` を伴い `Layout` 幅が `VERY_WIDE` になるため
> `Layout` 座標が View 座標と一致せず、Robolectric は TextView 側の水平オフセット補正を再現しない

前半 (`Layout` 幅が `VERY_WIDE` になる) は**正しい**が、後半 (Robolectric が補正を再現しない) は**誤り**である。
レビュー側で一時的な調査テストを書いて実測した (`@Config(sdk=[33])` + `@GraphicsMode(NATIVE)`、density 1.0):

```
[normal/START ] viewW=254 padEnd=6 textW=34.0  drawnTextLeft=  0.0
[normal/CENTER] viewW=254 padEnd=6 textW=34.0  drawnTextLeft=107.0   ← (254-6-34)/2 に一致
[normal/END   ] viewW=254 padEnd=6 textW=34.0  drawnTextLeft=214.0   ← (254-6)-34 に一致
```

ここで `drawnTextLeft = titleView.layout.getLineLeft(0) - titleView.scrollX`。
`TextView.bringTextIntoView()` が `ALIGN_CENTER` のとき `scrollX = (right+left)/2 - hspace/2` を設定する経路は
Robolectric でも動作しており、**START / CENTER / END の 3 値が px 単位で判別できる**。
ただし発火条件があり、`root.draw(Canvas)` **だけでは補正されない** (実測で `scrollX = 0` のままだった)。
`titleView.viewTreeObserver.dispatchOnPreDraw()` を明示的に呼ぶ必要がある。

この結果、現在のテストには 2 つの弱さがある:

1. **オーナーが要求した検証になっていない**。要求は「aux あり + CENTER の title 位置を検証するテスト」だが、
   実際に検証しているのは実効化の**前提** (title 領域に余白があること / gravity が `Layout` alignment に届くこと) に留まる。
2. **`:671-684` の 2 アサーションは事実上トートロジー**。`Layout.getLineLeft(0)` は `ALIGN_CENTER` のとき
   定義上 `(mWidth - lineMax)/2` を返す (`ALIGN_NORMAL` LTR なら 0) ため、直前の `getParagraphAlignment` の
   アサーションが通れば必ず通る。しかも `layout.width` は実測で `1048576` (= `VERY_WIDE`) であり、
   この式は View の幅とも padding とも無関係。回帰検出力を追加していない。

現状のテストは「実装を書き換えたら落ちる」性質そのものは持っており (title を `wrap_content` に戻せば
`centerTitle.width > naturalTextWidth` が落ちる)、実機証跡 `after-buttoncell-center-alignment-pixel6a.png` で
実描画の中央寄せが確認できる (レビュー側でも目視確認済み) ため、**ブロッキングではない**。

**推奨修正**:

`CellRowWidthAllocationTest.kt:671-684` の Layout 座標系アサーション 2 本を、次の実位置アサーションに置き換える。

```kotlin
// TextView.bringTextIntoView() による scrollX 補正を発火させる
centerTitle.viewTreeObserver.dispatchOnPreDraw()
startTitle.viewTreeObserver.dispatchOnPreDraw()

/** テキストが View 内のどの x に描画されるか。 */
fun drawnTextLeft(t: TextView): Float = t.layout.getLineLeft(0) - t.scrollX

val contentWidth = centerTitle.width - centerTitle.paddingStart - centerTitle.paddingEnd
assertEquals(
    "CENTER では title 領域の中央に描画される",
    (contentWidth - centerTitle.layout.getLineMax(0)) / 2f,
    drawnTextLeft(centerTitle),
    1.0f,
)
assertEquals("START では title 領域の左端に描画される", 0.0f, drawnTextLeft(startTitle), 1.0f)
```

あわせて KDoc と `ui/brief.md` の「Robolectric の検証限界」の記述を訂正する
(末尾省略の**グリフ位置**が未検証である点はそのまま有効なので、両者を混同しないこと)。

### [🔵 Suggestion] B の 6dp が ButtonCell ボタンスタイルの中央揃えを 3dp ずらす (原典同型なので実害なし)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:150`
(`setPaddingRelative(0, 0, titlePaddingEnd, 0)`) / `ButtonCellViewHolder.kt:123-131`

**問題点**: 依頼された「幅配分の不変条件」は**崩れていない**。`padding` は View 幅に含まれるため
`title 幅 + 行内 trailing 幅 = 主行幅` は保たれており、自動テスト (`CellRowWidthAllocationTest.kt:715-719`) と
実機実測 (`ui/brief.md` の全行で合計 996px) の両方で確認した。

一方で、gravity は **content box** (padding を除いた領域) の中で働くため、
`titleView.gravity = CENTER_HORIZONTAL` を使う ButtonCell では中央位置が `paddingEnd / 2` だけ leading 側へずれる。
aux なしのボタンスタイル (`ButtonCell(title = "ログアウト")`) について実測した:

```
[buttonStyle] viewW=288 padEnd=6 textW=85.0 drawnTextLeft=98.0 trueCenterLeft=101.5 offset=3.5
```

= 中央から 3dp 相当 leading 側。ボタンスタイルでは `valueTextView` が `GONE` でクリアランスの用途がないため、
この 3dp はもっぱらズレとして現れる。

**ただし原典 AiForms も同じ構造** (`CellBaseView.axml` の `CellTitle` が `paddingRight="6dp"` を持ち、
`ButtonCellRenderer.cs:97` はその上で `Gravity` を設定するだけ) なので、deviation.md の
「原典同型を徹底する」という意図とは**一致している**。仕様違反でも回帰でもない。

**推奨修正**: 不要 (記録のみ)。厳密な中央揃えを優先するなら、ボタンスタイル分岐で
`titleView.setPaddingRelative(0, 0, 0, 0)` に落とす手はあるが、原典から離れるため本 change のスコープ外と考える。
蒸留時に `concepts/core/styling/cell-row-layout.md` へ「title の 6dp クリアランスは content box に効くため、
全幅中央揃えでは 3dp のオフセットが生じる (原典同型)」を残すと、将来の「中央がずれている」報告への回答になる。

### [🔵 Suggestion] `@GraphicsMode(NATIVE)` はリポジトリ唯一の使用箇所 — CI 環境依存の申し送りを残したい

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:35`

**問題点**: D の修正で導入された `@GraphicsMode(GraphicsMode.Mode.NATIVE)` は、リポジトリ内で
このクラスだけが使っている (`grep -rn GraphicsMode android/` の結果)。native graphics モードは
Robolectric の nativeruntime アーティファクトを別途取得して実 Skia を動かすため、legacy モードより
起動コスト・ネットワーク前提・プラットフォーム依存が大きい。ローカル (macOS arm64) では
15 件すべて成功することを確認済みだが、CI で初めて踏むと原因が分かりにくい失敗になり得る。

技術判断そのものは**正しい**: `getEllipsisCount(0) > 0` というアサーションは省略が起きなければ必ず失敗する形なので、
テストが通っている事実が「NATIVE では実 ellipsize が動く」ことの証明になっている。追認テストではない。

**推奨修正**: KDoc (`:29-31`) に「legacy graphics に戻すと `getEllipsisCount` が常に 0 になり
`assertTruncatedAtEnd` が落ちる」旨を 1 行足すか、蒸留時に `concepts/` の Android テスト規約へ
「実 ellipsize / 実レンダリングを検証するテストは `@GraphicsMode(NATIVE)` が必要」を残す。

### [🔵 Suggestion] `assertTruncatedAtEnd` の `maxLines` 検証は `isSingleLine` を直接見るほうが意図に近い

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:86`

**問題点**: 実装は `isSingleLine = true` を設定しているが、テストは `view.maxLines == 1` を見ている。
`maxLines = 1` は `isSingleLine = true` の必要条件だが十分条件ではない
(`isSingleLine` は `setHorizontallyScrolling(true)` も伴う)。実装が `isSingleLine` から
`maxLines = 1` 単体へ退行しても、このアサーションは通ってしまう。
`layout.lineCount == 1` / `getEllipsisCount(0) > 0` が実質的な担保になっているので実害は小さい。

**推奨修正**: 任意。`assertTrue("$label は単一行構成", view.isSingleLine)` を 1 行足すと意図が閉じる。

## 確認したが問題なしと判断した観点

### 修正の副作用 (今回の重点)

- **B の 6dp と幅配分の不変条件**: 保たれている。上の Suggestion 1 のとおり自動テスト + 実機実測の両方で確認。
  `addFillingInlineTrailing` 経路 (EntryCell) では title が `wrap_content` になるため 6dp はそのまま
  クリアランスとして現れ、意図どおり。`descriptionView` には padding がないので title と description の
  左端は一致したまま (`UnifyCellCommonFieldsTest.kt:756, 789` が通過)。
- **`titleAlignment = END` の挙動**: 実測で `drawnTextLeft = (viewW - padEnd) - textW` となり、
  END 寄せでも行内 trailing との間に 6dp が残る。B の意図と整合。
- **C の追加テストの質**: SwitchCell / PickerCell とも `descriptionView.right <= accessoryHolder.left` を
  直接測っており、`CellBaseLayout.kt:239` の制約を外せば落ちる。PickerCell 側は
  `valueTextView.parent == contentRow` / `accessoryHolder.childCount > 0` で 2 系統の分離を
  View 階層として押さえており、相方が要求した内容を満たしている。追認テストではない。
- **D の記述統一**: テスト KDoc (`:82-83`)・`ui/brief.md` の照合表・実装メモの 3 箇所が
  「`getEllipsisCount > 0` まで検証、"…" グリフの画面位置は未検証」で一致。矛盾は解消。
- **E**: `contentRow.visibility` への代入はリポジトリ内に 1 件も残っていない。
- **F**: `ui/brief.md` に「合意済み妥協」節は残っておらず、spec 差分は `deviation.md` 参照に一本化。
  before の記述も「現行コードから `:app:installDebug` した APK / 先頭 2 行はスクロールで画面外」に訂正済みで、
  `after-input-cells-pixel6a.png` に「EntryCell」ヘッダと「名前」行が写っていることと整合する。

### 1 周目の回帰観点 (再確認)

- **ベースライン揃え**: `CellBaseLayout.kt:132-134` の `isBaselineAligned` は無変更。
  `CellRowWidthAllocationTest.kt:375` / `:438` が成功。
- **縦チェーン (packed 縦中央)**: chain head は `contentRow` のまま (`CellBaseLayout.kt:250-252`)。
  `CellRowWidthAllocationTest.kt:397` と既存 `UnifyCellCommonFieldsTest.kt:615, 658` が成功。
- **ButtonCell の ConstraintSet 切替**: `buttonStyleSet` / `normalLayoutSet` に今回の差分なし。
  `CellRowWidthAllocationTest.kt:566` (ボタンスタイル全幅) / `:691` (通常レイアウトへの復帰) が成功。
- **accessory 系配置**: `accessoryHolder` の ConstraintSet は無変更。今回追加の 2 テストで
  縦中央 (±1px) を含めて直接検証されるようになった。
- **android/ADR-0001 の IME 差分ガード**: `EntryCellViewHolder.bind` に今回の差分なし
  (`inputType` / `hint` / `filters` の差分ガードは無傷)。`InputCellsTest.kt` 全件成功。
- **公開 API**: `contentRow` / `addFillingInlineTrailing` はいずれも `internal` のまま。
  `paddingEnd` は内部構築時の設定で、公開シグネチャの変更なし。

### 足場・作業衛生

- **足場アーティファクトの逆流なし**: `proposal.md` / `exploration.md` / `specs/` の 2 ファイルに差分なし。
  `tasks.md` はチェックボックスのみ、`ui/brief.md` は UI 記録の追記。
- **`samples/` の残存差分なし**: 実機撮影のための一時変更は戻されている (`git status samples/` が空)。
- **deviation.md 記録済みの 4 件**は合意済み差分として扱い、spec 違反としては扱っていない。
- **Kotlin 規約**: `val` 中心、`!!` なし、KDoc あり、既存スタイルとの一貫性いずれも問題なし。
  テストヘルパ (`relayoutWithRowWidth` / `naturalWidthOf`) は意図が KDoc に明記され、
  「フォント metrics に依存せず行幅不足を再現する」という設計は Robolectric / 実機差を避ける妥当な手法。

## アクションプラン

1. **[任意・非ブロッキング]** 指摘 1: `CellRowWidthAllocationTest.kt:671-684` を `dispatchOnPreDraw()` +
   `getLineLeft(0) - scrollX` の実位置アサーションに差し替え、KDoc と `ui/brief.md` の
   「Robolectric の検証限界」の記述を訂正する。これで A のオーナー要求 (CENTER の title 位置検証) が
   自動テストとしても閉じる。
2. **[任意]** 指摘 4 (`isSingleLine` の直接検証)、指摘 3 (`@GraphicsMode(NATIVE)` の申し送り) を取り込む。
3. **[蒸留時]** `concepts/core/styling/cell-row-layout.md` へ次を残す:
   - title は 1 行 + 末尾省略 / description は複数行折り返し (両 platform 共通)
   - title の 6dp クリアランスは content box に効くため、全幅中央揃えでは 3dp のオフセットが生じる (原典同型)
   - 実 ellipsize / 実描画位置を検証する Robolectric テストには `@GraphicsMode(NATIVE)` と
     `dispatchOnPreDraw()` が必要
4. 上記はいずれも実装の欠陥ではないため、**本 change はこのままアーカイブに進んで差し支えない**。
