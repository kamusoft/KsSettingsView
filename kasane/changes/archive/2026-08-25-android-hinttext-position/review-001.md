# レビュー結果: android-hinttext-position (001 回目)

**日付**: 2026-08-24
**判定**: CHANGES_REQUESTED

## サマリー

合意済みスコープ (hint を cell 外縁から 上2dp / 右10dp、下端ガード 12dp、左端ガード 16dp) は達成されており、`android/gradlew test --rerun-tasks` は 2626 tests / 0 failures / 0 errors で全件成功する。しかし root padding の廃止が **GONE な View を経由する制約** へ波及しており、余白の再配分が抜けている箇所が 3 つある。うち 1 件 (行右端の余白消失) は提出された修正後スクリーンショット `ui/references/android-after.png` に既に写っており、既存テストも新規テストもこれを検出していない。

指摘は Critical 1 / Major 2 / Minor 2 / Suggestion 2。以下の実測値はすべて本レビューで一時プローブ (Robolectric, density=1.0) を実行して得たもので、プローブは実行後に削除し `CellBaseLayout.kt` の shasum 一致で原状復帰を確認済み。

## 指摘事項

### [🔴 Critical] accessoryHolder が GONE の行で行右端の余白 16dp が消える

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:302` (`contentRow` の END)、同 `:313` (`descriptionView` の END)

**問題点**:
`contentRow` / `descriptionView` の END は `accessoryHolder.START` に接続されている。ConstraintLayout は GONE の View を「サイズ 0 の点、かつ**自身のマージンは 0 として扱う**」ため、`accessoryHolder` が GONE になると新設した END マージン 16dp が消え、参照側 (`contentRow` / `descriptionView`) には goneMargin が設定されていない。従来は root の `paddingRight = 16dp` が制約空間そのものを内側に寄せていたのでこの経路でも余白が保たれていたが、padding 廃止でその保険が無くなった。

`ButtonCellViewHolder.kt:117` は aux (icon / valueText / hintText) を持つ ButtonCell で `accessoryHolder.visibility = View.GONE` を明示的に設定するため、この行が実際に該当する。

実測 (`ButtonCell(title = "登録", valueText = "送信")`, root 幅 320px, density 1.0):

```
contentRow.right = 320 (= root.width)  → 行右端の余白 0px  (期待 16px)
```

比較のため `SwitchCell` (accessoryHolder VISIBLE) では `accessoryHolder.right = 304` で余白 16px が保たれる。

これは机上の懸念ではなく **提出済みの証跡に写っている実害** で、`ui/references/android-after.png` の ButtonCell 行 (「登録 … 送信」) では valueText「送信」が cell の右端に密着して切れかけている。`ui/references/android-current.png` (修正前) と `ios-current.png` (正) ではいずれも 16dp の余白がある。

**推奨修正**:
START 側と対称に END 側にも goneMargin を与える。

```kotlin
set.setGoneMargin(contentRow.id, ConstraintSet.END, rowMarginH)
set.setGoneMargin(descriptionView.id, ConstraintSet.END, rowMarginH)
```

`contentRow` 側にこの 1 行を入れた状態で再測定し、`contentRow.right = 304` (余白 16px) に戻ることを確認済み。あわせて「aux ありの ButtonCell (accessoryHolder GONE) で行右端の余白が 16dp」を検証する回帰テストを追加すること — 現状 `CellRowWidthAllocationTest` の `ButtonCell はボタンスタイルから通常レイアウトへ復帰する` は END の接続先 (`endToStart`) しか見ておらず、実位置を測っていない。

---

### [🟠 Major] descriptionView が GONE の行で内容が縦中央から約 2dp 下へずれる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:301` (`contentRow` の BOTTOM)、`:314-320` (`descriptionView` の BOTTOM マージン)、クラス doc `:59-62`

**問題点**:
縦の余白 4dp を vertical chain の head (`contentRow` の TOP) と tail (`descriptionView` の BOTTOM) に配ったが、**tail は description が無い行では GONE になる**。Critical と同じ理由で GONE View 自身のマージンは無視されるため、chain の下端アンカーだけ 4dp を失い、上端 4dp / 下端 0dp の非対称な領域で `verticalBias = 0.5` の packed 配置が行われる。結果として内容が約 2dp 下へずれ、真の縦中央に置かれる `accessoryHolder` との縦位置が食い違う。android/ADR-0004 の光学中心補正が -1dp である以上、2dp のずれはその補正を上回る。

実測 (`LabelCell(title = "通知")`, 行高 60px, density 1.0):

| ケース | contentRow の中心 | 行の中心 | 差 |
|---|---|---|---|
| description なし | 32.5 | 30.0 | **+2.5px** |
| description あり | 45.0 | 45.0 | 0.0 |

クラス doc の「`descriptionView` が `GONE` のときも … `contentRow` 単独でも縦中央寄せ配置が維持される」(`:60-62`) は、この変更後は成立していない。

**推奨修正**:
`set.setGoneMargin(contentRow.id, ConstraintSet.BOTTOM, rowMarginV)` を追加する (この 1 行で差が +2.5px → +0.5px = 丸め誤差相当まで戻ることを実測で確認済み)。合わせて「description なしの行で contentRow の中心が行の中心と一致する」回帰テストを追加し、クラス doc の縦中央の説明を実装に合わせること。

---

### [🟠 Major] iconView / accessoryHolder が上下の行余白を失い、大きい icon が cell 上下端に密着する

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:281-282` (iconView の TOP/BOTTOM)、`:341-342` (accessoryHolder の TOP/BOTTOM)

**問題点**:
縦 4dp は `contentRow` の TOP と `descriptionView` の BOTTOM にだけ再配分され、`iconView` と `accessoryHolder` は margin 0 のまま parent の TOP/BOTTOM に接続されている。従来は root の `paddingTop/Bottom = 4dp` がこの 2 つにも効いていたため、icon やアクセサリが行内で最も高いときに (a) 行高が内容 + 8dp になり (b) 上下に 4dp ずつの間隔があった。padding 廃止でどちらも失われる。

実測 (`LabelCell` + `Theme(cellIconSize = 80.dp)`, density 1.0):

```
root.height = 80, icon.top = 0, icon.bottom = 80  → 上下の間隔 0px (従来は 4px ずつ、行高 88px)
```

`Theme.cellIconSize` / `CellStyle.iconSize` は公開 API であり、既定の 24dp では最低行高 60dp に吸収されて見えないが、アバター用途などで 53dp 以上を指定すると icon が cell の上下端 (= 罫線) に接する。exploration.md の決定は「余白 (横16dp/縦4dp) を内容側の ConstraintSet マージンへ再配分」であり、内容側の一部だけに配ることは合意に含まれていない。またこの挙動変化は deviation.md にも記録されていない (process/L-001)。

**推奨修正**:
`iconView` と `accessoryHolder` の TOP / BOTTOM にも `rowMarginV` を与える (両側同値なので縦中央配置は保たれる)。あわせて「行高より大きい icon を指定したとき、行高が icon + 上下 4dp になる」テストを追加すること。

---

### [🟡 Minor] 下端ガードの「効く局面」が未検証で、検証不能とした理由が正確でない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/UnifyCellCommonFieldsTest.kt:394-400`

**問題点**:
新設テスト `hintTextView は cell 下端から 12dp の下端ガードを持つ` が検証しているのは制約の配線 (`bottomToBottom` / `bottomMargin` / `verticalBias` / `constrainedHeight`) と、ガードに触れない通常時の位置だけで、ガードが存在する理由である「高さが足りないときに hint が縮む」挙動そのものは検証されていない。doc コメントは「legacy graphics の TextView は測定高さが textSize に追随しないため観測できない」としているが、本リポジトリには `CellRowWidthAllocationTest` という `@GraphicsMode(GraphicsMode.Mode.NATIVE)` 指定のクラスが既にあり (test-execution.md の Robolectric 節が求めるとおり)、NATIVE では textSize が測定高さに反映される。つまり「Robolectric では観測できない」ではなく「legacy graphics では観測できない」が正しく、検証手段は既に repo 内にある。

**推奨修正**:
NATIVE graphics 側 (`CellRowWidthAllocationTest` など) に、hint のフォントサイズを行高に対して十分大きくしてガードが効く状態を作り、`root.height - hint.bottom >= 12dp` かつ hint が縮む (= `constrainedHeight` が効く) ことを検証するテストを置く。それが難しい場合でも、doc コメントの「Robolectric では観測できない」を「legacy graphics では観測できない」に正し、NATIVE を使わない理由 (起動コスト等) を書くこと。

---

### [🟡 Minor] 実機スクリーンショットの置き場がホワイトリスト外

**該当箇所**: `ui/references/android-after.png`

**問題点**:
ksn-core の媒体ファイル置き場ホワイトリストでは、`ui/references/` は「議論中に渡されたデザイン画像・参考スクリーンショット (explore / propose が書く)」であり、実装後に実機で撮った画像は `ui/verification/` (視覚照合ループの最終周) または `evidence/` (実機・シミュレータでの動作証跡) に置く。`android-current.png` / `ios-current.png` は探索時にオーナーから渡された画像なので `ui/references/` で正しいが、`android-after.png` (修正後・Pixel 6a 実機) だけ置き場が異なる。archive 時の媒体削除ポリシーも置き場単位で効くため、混在させない方がよい。

なお写り込みの観点では、`android-after.png` のステータスバーに通知アイコンが数個出ている。個人を特定する情報ではないが、ksn-core の撮影規律は「通知が写らない状態にしてから撮る」としているため、次回撮影時は通知を消してから撮ること。

**推奨修正**:
`android-after.png` を `evidence/` (本レビュー / 実装の動作証跡として) へ移し、参照している文書側のパスを更新する。

---

### [🔵 Suggestion] 縦 4dp が定数化されておらず、横 16dp と非対称

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:134`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:43`

**問題点**:
横方向は `CELL_ROW_HORIZONTAL_PADDING_DP` という共有定数を持つのに対し、縦 4dp は `CellBaseLayout` と `ButtonCellViewHolder` にリテラルで二重化されている。今回 root padding を廃止したことで「行の余白」を持つ箇所が 1 箇所から複数へ増えたため、片方だけ変えても気づけない形になった。またこの機に定数名の `PADDING` も実体 (margin) と食い違っている。

**推奨修正**:
`CELL_ROW_VERTICAL_PADDING_DP` (あるいは `CELL_ROW_VERTICAL_MARGIN_DP`) を追加して両所から参照する。定数名の `PADDING` → `MARGIN` へのリネームは internal な定数で影響範囲が閉じているため同時に行ってよい。

---

### [🔵 Suggestion] buttonStyleSet の適用が contentRow の translationY を 0 に戻す (既存の挙動)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:128`

**問題点**:
`buttonStyleSet` は `clone()` を経ない新規 `ConstraintSet` で、`ConstraintSet.applyTo()` は対象 View の translation も Constraint の既定値 (0) で上書きする。実測でも、ボタンスタイルで bind した後の `contentRow.translationY` は `-1.0` → `0.0` になり、android/ADR-0004 の光学中心補正が消える (aux ありで再 bind すると `normalLayoutSet` 経由で `-1.0` に戻る)。本変更が原因ではない既存の挙動だが、今回まさにこの `buttonStyleSet` を書き換えているため、process/L-005 の「本 change で触れたファイル内で数行で閉じる不備は同じサイクル内で直す」に該当する。

ボタンスタイルにはアクセサリが無いため見た目の実害は小さい (同じ文字が他の行より 1dp 低く出る程度) が、補正の意図が黙って消えている点は残しておくべきではない。

**推奨修正**:
`buttonStyleSet.applyTo(views.root)` の直後に `views.contentRow.translationY` を光学補正値へ戻すか、`buttonStyleSet` 側に translationY を設定する。数行で閉じないと判断した場合は deviation.md にその判断を記録すること。

---

## 確認して問題がなかった観点

- **合意済みスコープの充足**: hint の実効位置が cell 外縁から 上2dp / 右10dp になっていること (`UnifyCellCommonFieldsTest` の実測アサーション + `android-after.png` の目視)、下端ガード 12dp と左端ガード 16dp が入っていること、長い hint がフォント縮小ではなく末尾省略になること (NATIVE graphics のテストで実 ellipsize を検証) を確認した。deviation.md 記載の 2 件 (ButtonCell の余白マージン付随修正 / hint 左端ガード) はいずれも ksn-core の付随・隣接修正の条件に収まっており、回帰テストも付いている。
- **ビルドとテスト**: `cd android && ./gradlew test --rerun-tasks` が BUILD SUCCESSFUL。件数は `build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` 集計で **2626 tests / 0 failures / 0 errors / 0 skipped** (test-execution.md の Android 節に従い件数まで確認)。
- **root padding を外部から読むコードの不在**: `android/` 配下の main ソースに `views.root.padding*` を読む箇所は無い。罫線 (`ClassicSectionDecoration` / `ModernSectionDecoration`) が読むのは RecyclerView 側の padding であり、cell root とは独立。
- **背景色・Ripple・最低高さ保証**: 背景は root 全体に敷かれるため padding 廃止の影響を受けない。`MinHeightConstraintLayout` は measuredHeight と minimumHeight の比較のみを行い padding を参照しない。
- **CustomCell / Compose bridge**: `CustomCellViewHolder` は共通行を使わず `CELL_ROW_HORIZONTAL_PADDING_DP` を Compose の `padding(end = …)` として使うだけで、root padding には依存しない。`ks-settingsview-compose` / `ks-settingsview-bridge` に cell root の padding を読む箇所は無い。
- **icon 非表示時の左端**: `contentRow` / `descriptionView` の START 側 goneMargin 16dp が入っており、`CellIconFrameTest` と `UnifyCellCommonFieldsTest` の実測アサーションで担保されている。
- **足場アーティファクト**: exploration.md / deviation.md に実装中の書き換え跡は無い。
- **コメント規約**: 新規・改訂コメントは ADR 参照 (`android/ADR-0002` / `core/ADR-0026` / `android/ADR-0004`) と自己完結した説明のみで、change 名・Phase 通番・アーカイブ文書パスの混入は無い。テスト名から `Phase 15_4` が外れているのも規約方向として正しい。

## アクションプラン

1. **Critical**: `contentRow` / `descriptionView` の END に goneMargin 16dp を追加し、aux あり ButtonCell (accessoryHolder GONE) の行右端余白を測る回帰テストを追加する。
2. **Major**: `contentRow` の BOTTOM に goneMargin 4dp を追加し、description なしの行の縦中央を測る回帰テストを追加する。クラス doc の縦中央の記述も実装に合わせる。
3. **Major**: `iconView` / `accessoryHolder` の TOP / BOTTOM に 4dp を与え、行高より大きい icon での行高と上下間隔を検証するテストを追加する。
4. 1〜3 を入れた後、**実機で再撮影して A/B 証跡を取り直す** (process/L-003 (3): 承認済み照合の後に視覚へ影響する修正を入れたら影響スクショを再撮影する)。特に ButtonCell 行の右端余白と、description なし行のアクセサリとの縦位置は画面で確認すること。
5. **Minor**: 下端ガードの挙動テストを NATIVE graphics 側に追加する (難しければ doc コメントの「Robolectric では」を「legacy graphics では」に正す)。
6. **Minor**: `android-after.png` を `evidence/` へ移す。
7. **Suggestion**: 縦 4dp の定数化 (+ 定数名の margin 化)、`buttonStyleSet` 適用後の translationY 復帰。
