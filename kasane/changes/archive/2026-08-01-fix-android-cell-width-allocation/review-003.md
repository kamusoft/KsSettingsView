# レビュー結果: fix-android-cell-width-allocation (003 回目)

**日付**: 2026-08-01
**判定**: APPROVED

**指摘件数**: Critical 0 / Major 0 / Minor 0 / Suggestion 2

## サマリー

2 周目の Minor 1 件と Suggestion 2 件はすべて推奨どおりに解消されている。**焦点だった修正 1
(実描画位置アサーション) は、ミューテーションテストによる実証の結果、回帰検出力を持つ本物のアサーションである**。
とくに「前提 1・前提 2 が通過し、実位置アサーションだけが落ちる」ミューテーションを構成できたことで、
2 周目が指摘したトートロジー性が解消されたことが直接的に証明された (下記 M3)。
`drawnTextLeftOf` の実装と `dispatchOnPreDraw()` の必要性も実測で裏付けが取れた。

副作用は検出されなかった。差分は 2 ファイル (テスト 1 + `ui/brief.md`) のみで、実装コード
(`src/main/`) には 1 行の差分もない。全テスト成功 (`./gradlew test --rerun-tasks` →
BUILD SUCCESSFUL / 166 tasks executed / **debug 535 + release 535 = 1070 件、failures 0 / skipped 0**、
うち `CellRowWidthAllocationTest` 15 件)。足場アーティファクトへの逆流もない。

残る 2 件はいずれもコメント/KDoc の表現に関する Suggestion で、非ブロッキング。

## verify への影響

**verify-003.md は不要 (変化なし)。** 今回の差分はテストコード (`CellRowWidthAllocationTest.kt`) と
`ui/brief.md` に限られ、`src/main/` 配下の実装ファイル 3 本 (`CellBaseLayout.kt` /
`ButtonCellViewHolder.kt` / `EntryCellViewHolder.kt`) には差分がない
(レビュー中の一時ミューテーション後、backup との shasum 一致で原状復帰を確認済み)。
デルタスペックの Requirement / Scenario と実装の対応関係は `verify-002.md` の対応表から 1 行も変わらず、
判定 VALID もそのまま維持される。今回の変更は既存 Scenario に対するテストの**検出力の強化**であって、
対応の追加・削除ではない。

## 修正 1 の実証: 回帰検出力のミューテーションテスト

「トートロジーではなく実際に回帰検出力があるか」を確認するため、実装/テストに一時的な変更を加えて
アサーションの反応を実測した。**併せて、対象テストの実位置アサーション部分 (前提 3) だけを切り出した
一時 probe テストを用意し、前提 1・前提 2 の助けなしで単独検出できるかを分離して測った。**

一時アーティファクトはすべて削除・原状復帰済み (末尾「作業衛生」参照)。

### ベースライン (無改変)

```
[PROBE] centerW=254 padS=0 padE=6 contentW=248 lineMax=34.0 layoutW=1048576
        scrollX=524164 expectedCenter=107.0 drawnCenter=107.0 drawnStart=0.0
```

CENTER は 107.0px、START は 0.0px と **px 単位で判別できている**。値が退化して両者が同じになる
(= 実質何も測っていない) 状態ではない。

### ミューテーション結果

| # | ミューテーション | テスト全体 | 実位置アサーション単独 (probe) |
|---|---|---|---|
| M1 | `titleView` を `wrap_content` + `weight=0` に戻す | **FAILED** (対象テスト含む 4 テスト) | **PASS** (expected 0.0 / drawn 0.0) |
| M2 | `ButtonCellViewHolder.kt:110` の `gravity` 適用を外す | **FAILED** (対象テスト) | **FAILED** (expected 107.0 / drawn 0.0) |
| M3 | `titleView` に compound drawable を追加 (content box ≠ padding) | **FAILED** (対象テスト) | **FAILED** (expected 107.0 / drawn **95.0**) |
| M4 | `dispatchOnPreDraw()` を外し `root.draw(Canvas)` に置換 | — | **FAILED** (drawn 524271.0) |

読み取れること:

1. **M3 が決定的**。このミューテーションでは前提 1 (`width > naturalTextWidth`) と前提 2
   (`getParagraphAlignment == ALIGN_CENTER`) が**両方とも通過**し、**実位置アサーションだけが落ちた**
   (107.0 期待に対し 95.0 = compound drawable 幅 24px の半分だけ leading 側にずれた)。
   2 周目が指摘した「前提 2 が通れば必ず通る」という旧アサーションの性質は解消されている。
   旧アサーション (`layout.getLineLeft(0)` を `VERY_WIDE` 座標系のまま見る形) では、この差は
   原理的に検出できない。
2. **M2 の gravity 喪失は実位置アサーション単独でも検出できる** (前提 2 と二重の担保)。
3. **M1 の幅退行は実位置アサーション単独では検出できない**。`contentWidth` を View の実幅から
   計算しているため、View が縮むと期待値も一緒に縮む自己整合になる。ただしこれは設計上の役割分担で、
   幅の退行は前提 1 が担保しており、**テスト全体としては確実に落ちる** (M1 で対象テストを含む
   4 テストが FAILED)。オーナー要求「aux あり + CENTER の title 位置を検証する」は
   前提 1〜3 の組で閉じている。
4. **M4 により `dispatchOnPreDraw()` の必要性が裏付けられた**。これを外して `root.draw(Canvas)` に
   替えると `scrollX` は 0 のままで `drawnTextLeft` が 524271.0 (= `VERY_WIDE` 由来の値) になる。
   KDoc の「`root.draw()` だけでは `scrollX` 補正が入らない」は正しい。

## `drawnTextLeftOf` の妥当性

**`scrollX` を引く根拠**: 実測値が AOSP `TextView.bringTextIntoView()` の式と厳密に一致した。

- `hspace` = `width - compoundPaddingLeft - compoundPaddingRight` = 254 − 0 − 6 = **248**
- `ALIGN_CENTER` 経路: `scrollx = (right + left) / 2 - hspace / 2` = 1048576/2 − 248/2 = **524164** (実測一致)
- `layout.getLineLeft(0)` = (1048576 − 34)/2 = **524271** (実測一致)
- 差 = 524271 − 524164 = **107** = `(248 − 34)/2` = `(contentWidth − lineMax)/2` (アサーションの期待値と一致)
- `ALIGN_NORMAL` (LTR) 経路: `scrollx = floor(getLineLeft) = 0`、`getLineLeft = 0` → 差 0 (実測一致)

**発火機構も production 経路と同一**である点は積極的に評価できる。`TextView` は
`ViewTreeObserver.OnPreDrawListener` を実装し、`onPreDraw()` は movement method を持たない TextView に対して
`bringTextIntoView()` を呼ぶ。これは実機で毎フレーム描画前に走る経路そのものであり、
このテストは「Robolectric 固有の抜け道」を測っているのではなく**実機と同じ補正経路の結果**を測っている。
`dispatchOnPreDraw()` はその発火をテストから明示的に起こしているだけで、挙動の捏造ではない。

許容誤差 `1.0f` も妥当 (`bringTextIntoView` 内の `floor`/`ceil` による丸めは最大 1px 程度。
M3 の実差分 12.0px はこれを大きく超えて検出された)。

## 修正 2 (`isSingleLine`) の安全性

`assertTruncatedAtEnd` が適用されるのは `titleView` と `valueTextView` の 2 つのみ。
リポジトリ内で `isSingleLine` / `maxLines` / `setSingleLine` / `setHorizontallyScrolling` に
代入している箇所は `src/main/` に 4 箇所しかなく (`CellBaseLayout.kt:152`(title) / `:176`(valueText) /
`:197`(hintText)、`EntryCellViewHolder.kt:242`(EditText))、**すべて構築時の `= true` のみ**。
bind 経路や `reset()` で false に戻す代入は存在しないため、`isSingleLine` が false になる経路はない。
実際に全テスト成功で確認できている。

API 面の懸念もなし: `TextView.isSingleLine()` の getter は API 29 で追加され、
`ks-settingsview-ui` の `minSdk = 29` と一致する (かつテストコードで `@Config(sdk = [33])`)。

## 修正 3 (`@GraphicsMode` の申し送り) と修正 4 (brief.md)

- **修正 3**: クラス KDoc (`CellRowWidthAllocationTest.kt:29-33`) に「legacy graphics では
  `TextUtils.ellipsize` が動作せず `getEllipsisCount` が常に 0 になる」「**legacy に戻すと
  `assertTruncatedAtEnd` が必ず落ちる**」「本リポジトリではこのクラスが唯一の使用箇所」が
  すべて入っており、2 周目 Suggestion 3 の推奨を満たしている。
  なお「legacy に戻すと落ちる」の実測は 2 周目レビューで済んでおり、今回は再実測していない
  (グラフィックスモードの切替は本 change のスコープ外)。記述を反証する材料は見つからなかった。
- **修正 4**: `ui/brief.md` の「Robolectric での検証範囲」節の記述は**技術的に正確**。
  「singleLine な TextView のテキストの水平位置も Robolectric で検証できる」は M2 で、
  「`root.draw()` だけでは補正されない」は M4 で、それぞれ実測により追認した。
  末尾省略の **"…" グリフの画面上の位置は未検証**という別論点との区別も維持されており、
  2 周目が注意していた「両者を混同しない」を守れている。当初の記録が誤りだった旨の注記も残っている。

## 指摘事項

### [🔵 Suggestion] `drawnTextLeftOf` の KDoc の座標系が「View 内」と読めるが、実際は content box 起点

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:80`
(併せて `:703` のアサーションメッセージ、`kasane/changes/fix-android-cell-width-allocation/ui/brief.md`
「Robolectric での検証範囲」の同趣旨の一文)

**問題点**: KDoc は「[view] のテキストが **View 内**のどの x に実描画されるか（px）」と書いているが、
`Layout` は `compoundPaddingLeft` だけ平行移動して描画されるため、`getLineLeft(0) - scrollX` が返すのは
**content box (padding を除いた領域) の左端を原点とするオフセット**である。
現状 `titleView.paddingStart == 0` なので両者は一致しており実害はないが、
テスト本体は CENTER 側で「title 領域 (**content box**) の中央」と正しく書き分けているのに対し、
START 側 (`:703`) は「title 領域の左端」でヘルパ KDoc と同じ曖昧さを残している。
将来 `titleStart` 方向の padding が導入されると、KDoc を信じた読み手が期待値の立て方を誤り得る。

**推奨修正**: 任意。KDoc の一文を「View の content box（padding を除いた領域）の左端を原点とする
実描画 x（px）」に直し、`:703` のメッセージも「title 領域 (content box) の左端」に揃えると閉じる。
`ui/brief.md` の同趣旨の一文も同様。

### [🔵 Suggestion] 前提 1 と前提 3 の役割分担を KDoc に残すと、将来の「冗長に見えるアサーション」削除を防げる

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:628-641`
(テストの KDoc)

**問題点**: M1 で実証したとおり、**実位置アサーション (前提 3) は `titleView` の幅退行を単独では
検出しない** (期待値を View の実幅から導くため自己整合になる)。幅の退行を検出しているのは
前提 1 (`centerTitle.width > naturalTextWidth`) である。
現在の KDoc は 1〜3 を並列に列挙しているため、実位置アサーションが入った今、
将来の読み手が「前提 1 は前提 3 に含まれる冗長なチェック」と誤読して削除する余地がある。
削除されると M1 のような幅退行が無検出になる。

**推奨修正**: 任意。KDoc の列挙に「1 は幅配分の退行を、3 は gravity と描画位置の退行を捕まえる。
3 は期待値を View の実幅から導くため、1 が無いと幅の退行を検出できない」の一文を足す。

## 確認したが問題なしと判断した観点

### 副作用 (今回の重点)

- **差分範囲**: `git status` の変更ファイル集合は 2 周目時点と完全に同一 (新規ファイル・削除ファイルなし)。
  今回の 2 ファイル以外に波及はない。
- **実装コードへの波及なし**: `src/main/` 配下 3 ファイルはレビュー開始時のスナップショットと
  shasum 一致 (ミューテーション後の原状復帰も同じ shasum で検証済み)。
- **既存テストの破壊なし**: `./gradlew test --rerun-tasks` が BUILD SUCCESSFUL、
  1070 件 (debug 535 / release 535) failures 0 / skipped 0。2 周目の 535 件から件数の増減もない
  (今回はアサーションの差し替え・追加であってテストメソッドの増減がないため、これは整合)。
- **足場アーティファクトの逆流なし**: `specs/` 2 ファイル・`proposal.md`・`exploration.md`・`tasks.md`・
  `deviation.md`・`review-001/002.md`・`verify-001/002.md`・`second-opinion-001/002.md` はいずれも
  review-002 出力時刻 (21:40) 以前の更新時刻のまま。更新されたのは `ui/brief.md` (21:44) と
  テストファイルのみ。
- **deviation.md の 4 件**は合意済み差分として扱い、spec 違反としては扱っていない。

### 未解消の指摘の棚卸し (収束判定用)

| 周 | 指摘 | 状態 |
|---|---|---|
| 1 周目 | A〜F の 6 件 | review-002 で全解消確認済み。今回の差分は実装に触れておらず全テスト緑のため退行なし |
| 2 周目 | 🟡 Minor 1: 実位置アサーションへの差し替え | **解消**。M2/M3/M4 で検出力を実証 |
| 2 周目 | 🔵 Suggestion 3: `@GraphicsMode` の申し送り | **解消** (KDoc `:29-33`) |
| 2 周目 | 🔵 Suggestion 4: `isSingleLine` の直接検証 | **解消** (`:102`) |
| 2 周目 | 🔵 Suggestion 2: 6dp による ButtonCell ボタンスタイルの 3dp オフセット | **意図的に未対応**。2 周目レビュー自身が「推奨修正: 不要 (記録のみ)」「原典同型で仕様違反でも回帰でもない」と結論しており、未解消の指摘ではなく**蒸留時の記録 TODO**。この判断に同意する |

**未解消のまま残っている指摘: なし。**

### 蒸留時への申し送り (指摘ではない)

review-002 アクションプラン 3 の 3 項目は引き続き有効。今回の実証を踏まえ、3 項目目は次のように
具体化できる (蒸留時に `concepts/core/styling/cell-row-layout.md` または Android テスト規約へ):

- 実 ellipsize の検証には `@GraphicsMode(NATIVE)` が必要 (legacy では `getEllipsisCount` が常に 0)
- singleLine な TextView の実描画位置は `viewTreeObserver.dispatchOnPreDraw()` +
  `layout.getLineLeft(0) - scrollX` で測れる。`root.draw(Canvas)` では `scrollX` 補正が入らない。
  返る値は content box 起点

### 作業衛生 (レビュー側)

本レビューでは検出力の実証のため実装/テストへ一時的なミューテーション (M1〜M4) と
一時 probe テスト 1 ファイルを投入した。**すべて撤去・原状復帰済み**:

- 一時 probe テスト: `trash` で削除。`find`/`grep` で残存 0 件を確認
- ミューテーション: `MUTATION` 文字列の残存 0 件を `grep -rn` で確認
- 3 ファイル (`CellBaseLayout.kt` / `ButtonCellViewHolder.kt` / `CellRowWidthAllocationTest.kt`) は
  レビュー開始時に取得した backup と **shasum 完全一致**
- 復帰後に `./gradlew test --rerun-tasks` を再実行して BUILD SUCCESSFUL / 1070 件全成功を確認
- git 操作 (add / commit / push) は一切行っていない

## アクションプラン

1. **[任意・非ブロッキング]** Suggestion 1: `drawnTextLeftOf` の KDoc と `:703` のメッセージ、
   および `ui/brief.md` の同趣旨の一文の座標系を「content box 起点」に明記する。
2. **[任意・非ブロッキング]** Suggestion 2: テスト KDoc に前提 1 と前提 3 の役割分担を 1 行足す。
3. **[蒸留時]** review-002 アクションプラン 3 の 3 項目 (上記「蒸留時への申し送り」で具体化済み) を
   `concepts/` へ残す。ButtonCell ボタンスタイルの 3dp オフセット (2 周目 Suggestion 2) もここに含める。
4. 上記はいずれも実装の欠陥ではなく、**本 change はこのままアーカイブに進んで差し支えない**。
