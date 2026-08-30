# レビュー結果: fix-android-cell-width-allocation (001 回目)

**日付**: 2026-08-01
**判定**: NEEDS_DISCUSSION

## サマリー

ADR-0002 の決定 (水平 LinearLayout + weight による残り幅配分、EntryCell 入力欄の行内移設、160dp ハック撤去) は忠実かつ最小差分で実装されており、デルタスペックの全 10 Scenario が実装とテストで満たされている (詳細は `verify-001.md`)。ビルド成功、テスト 382 件すべて成功。既存 Cell の回帰も、ベースライン揃え・縦チェーン・ButtonCell の ConstraintSet 切替・accessory 系配置・ADR-0001 の IME 差分ガードのいずれも維持されていることをコードとテストで確認した。

一方で、**デルタスペックが沈黙している領域で利用者可視の挙動が 2 点変わっている** (title の折り返し廃止、ButtonCell の titleAlignment 実効化)。どちらも「原典 AiForms / iOS と揃う」方向の変化であり技術的には妥当だが、片方は brief.md で「オーナー確認事項」と明記されたまま未合意、もう片方は一切記録されていない。実装だけでは決着できない設計判断のため NEEDS_DISCUSSION とする。**コードの欠陥としての指摘は 0 件**であり、オーナーが 2 点を承認すればそのまま APPROVED 相当になる。

## 指摘事項

### [🟠 Major] ButtonCell の通常レイアウトで `titleAlignment` が初めて実効化する (既定値 CENTER の見た目が変わる)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:138-148` (titleView の weight 化) / `ButtonCellViewHolder.kt:110`

**問題点**:

`titleView` が `0dp + weight=1` になったことで、**ButtonCell の通常レイアウト (aux あり) における `titleAlignment` の見え方が変わる**。この副作用は proposal / tasks / brief.md のいずれにも記載がない。

- 変更前: `titleView` は `ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)` + `horizontalBias = 0.0f` だった。`constrainDefaultWidth(MATCH_CONSTRAINT_WRAP)` は width が `0dp` のときにしか効かないため、titleView の実幅は**テキストのコンテンツ幅**。`ButtonCellViewHolder.bind` が設定する `views.titleView.gravity = gravityFor(cell.titleAlignment)` は、コンテンツ幅ぴったりの View 内での gravity なので**視覚的に無効**で、title は常に左寄せに見えていた。
- 変更後: `titleView` は主行の残り幅全体を占める。同じ `gravity` が実際に効き、`CellTitleAlignment.CENTER` なら残り幅の中央、`END` なら右端に寄る。

`ButtonCell.titleAlignment` の**既定値は `CellTitleAlignment.CENTER`** (`ButtonCell.kt:40`、Compose DSL 側も同じ) なので、`ButtonCell(title = "登録", valueText = "送信")` のように aux を付けただけの既存利用コードで、title の位置が左寄せから中央寄せへ変わる。公開 API の既定値に紐づく破壊的でない見た目変更であり、proposal の「Impact: 公開 API 変更なし。破壊的変更なし」の想定範囲を超えている可能性がある。

なお、この変化は openspec 側の記述 (`ButtonCellViewHolder の aux 切替`: 「通常レイアウトは applyCellBaseLayout 経由 + title 列内 `gravity` で `titleAlignment` を表現」) の**意図どおりに初めて動くようになった**とも読めるため、「回帰」ではなく「積年のバグの副次的解消」と判断する余地もある。だからこそオーナー判断が要る。

既存テスト `UnifyCellCommonFieldsTest.kt:295` は `titleView.gravity` のフラグ値しか見ておらず**位置を検証していない**ため、この変化はテストでも捕捉されていない。付属の `CellRowWidthAllocationTest.kt:445` はボタンスタイル (aux なし) 側だけを見ている。

**推奨修正** (いずれかをオーナーが選択):

- (a) 意図した改善として受け入れる → `deviation.md` に「ButtonCell 通常レイアウトの titleAlignment が実効化した」旨を記録し、`ButtonCellViewHolder` の通常レイアウトで aux あり + `titleAlignment = CENTER` のときの `titleView` の左右位置を検証するテストを `CellRowWidthAllocationTest` に 1 本追加する。
- (b) 旧挙動を維持する → 通常レイアウトの ButtonCell に限り `titleView` の `layoutParams` を `wrap_content + weight = 0` に付け替える (`addFillingInlineTrailing` と同じ手口)。ただし原典 AiForms は `CellTitle` を `0dp + weight=1` 固定にしているため、原典同型からは離れる。

### [🟡 Minor] title の折り返し廃止 (`isSingleLine`) が「合意済み妥協」節に未合意のまま記載されている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:147-148` / `kasane/changes/fix-android-cell-width-allocation/ui/brief.md` 「合意済み妥協 / 申し送り」

**問題点**:

`titleView` に `isSingleLine = true` + `ellipsize = END` を**無条件に**適用したため、行内 trailing を持たない Cell (CommandCell / SwitchCell / CheckboxCell / Radio / SimpleCheck / ボタンスタイルの ButtonCell) の長い title も、従来の複数行折り返しから 1 行末尾省略に変わる。長い title を持つ行では行高さも縮む。

デルタスペックは、既定 (valueText 系) については「収まらない場合は末尾省略で切り詰める」と明記しているが、「行内 trailing がない場合」の Scenario は「title が主行の全幅を表示領域として使える」としか書いておらず折り返しの可否に言及していない。つまり**この挙動変更は spec の範囲外**である。

実装者は brief.md に記載して「→ オーナー確認事項」と明示しているが、置かれている節が**「合意済み妥協 / 申し送り」**であり、未合意の乖離を合意済みの見出しの下に置くのは記録として不正確 (ksn-core: 合意済みの差分は `deviation.md`、記録のない・未合意の乖離だけが問題)。

**なお、レビュー側で裏取りした結果、この変更の方向自体は強く正当化される** (実装者は brief.md でこの根拠を挙げていない):

- 原典 AiForms: `AiForms.Maui.SettingsView/SettingsView/Platforms/Android/Resources/layout/cellbaseview.axml` の `CellTitle` は `android:singleLine="true"` `android:ellipsize="end"`。ADR-0002 の「原典同型」に照らして**一致**する。
- iOS 実装: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:44-48` の `titleLabel` は `numberOfLines = 1` / `.byTruncatingTail`。つまり本変更は Android を **iOS に揃える**方向であり、platform 間の乖離を減らす。

**推奨修正**:

- brief.md の当該項目を「合意済み妥協」から外し、オーナー承認を得たうえで `deviation.md` に「spec 範囲外の挙動変更 (title の 1 行化) を、原典 `cellbaseview.axml` の `CellTitle` および iOS `titleLabel` との同型化として合意」と記録する。
- 蒸留時に `kasane/concepts/core/styling/cell-row-layout.md` の「共通の視覚文法」へ「title は 1 行 + 末尾省略、description は複数行折り返し」を両 platform 共通の規則として明文化する (現状この文書は title の行数に一切言及していない)。

### [🔵 Suggestion] `ButtonCellViewHolder` の `contentRow.visibility = VISIBLE` は到達不能な防御コード

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:125`

**問題点**: `contentRow` を `GONE` にするコードは `buildCellBaseViews` にも各 ViewHolder にも存在しないため、この行は常に no-op。同じ行の他の `visibility` 代入 (iconView / descriptionView / valueTextView / accessoryHolder / hintTextView) は通常レイアウト側で VISIBLE になり得るので意味があるが、`contentRow` だけは対称性のためだけに置かれており、読み手に「どこかで GONE になる経路がある」と誤読させる。

**推奨修正**: 削除するか、「対称性のための防御的初期化であり現状 GONE になる経路はない」とコメントで明示する。

### [🔵 Suggestion] title と行内 trailing の間に余白がなく密着する (原典は `paddingRight="6dp"`)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:138-172`

**問題点**: 原典 `cellbaseview.axml` の `CellTitle` は `android:paddingRight="6dp"` を持ち、title と行内 trailing の間に最小限のクリアランスを確保している。移植側にはこれがなく、brief.md の実機実測でも全行で `title.right == trailing.left` (余白 0) になっている。

現状の Sample では EditText / valueText がいずれも右寄せ描画のため視覚的な問題は出ていない (`ui/verification/after-input-cells-pixel6a.png` で確認済み)。ただし `textAlignment = START` の EntryCell や、title が残り幅を使い切って末尾省略が起きた valueText 系では、title の "…" と trailing テキストが 0px で接する。

なお spacing は brief.md で明示的に**非規範**とされているため、mock 照合上の不一致ではない。

**推奨修正**: 原典同型を徹底するなら `titleView` に 6dp 相当の `paddingEnd` を入れる。別 change に切り出しても構わない。

### [🔵 Suggestion] before スクリーンショットが実装前のビルドと行構成がずれている

**該当箇所**: `kasane/changes/fix-android-cell-width-allocation/ui/references/current-kssettingsview.png`

**問題点**: brief.md が自ら記載しているとおり、この before は「EntryCell セクションヘッダ」と「名前」行を欠いた古い APK での取得。幅配分の症状 (420px = 160dp × 2.625 の固定幅) の証跡としては十分に有効で、取得順序 (before 20:26 → 実装 20:34 → after 20:48) も守られているため**タスク 4.1 は満たしている**。開示が正直である点も評価する。

**推奨修正**: 不要 (記録のみ)。次回同種の変更では before / after を同一 APK 構成で揃えると差分が読み取りやすい。

## 確認したが問題なしと判断した観点

コンテキストパッケージで名指しされた回帰観点を含め、以下は問題なしと確認した。

- **valueText ↔ title のベースライン揃え**: `ConstraintSet.BASELINE` 紐付けの喪失を `LinearLayout.isBaselineAligned`(既定 true を明示設定) で代替。`CellRowWidthAllocationTest.kt:360` が `titleView.top + baseline == valueTextView.top + baseline` を直接検証しており、既存 `UnifyCellCommonFieldsTest.kt:679` も追随不要のまま成功している。EntryCell の EditText についても `:423` で同じ検証がある。
- **title + description の縦チェーン (packed 縦中央)**: chain head を `titleView` → `contentRow` に正しく付け替え済み (`CellBaseLayout.kt:241-243`)。GONE chain member の扱いに関する既存の前提も維持されている。既存テスト 2 本は `titleView` が `contentRow` 相対座標になったことに対し `titleLeftInRoot()` 等のヘルパで**座標系を換算**して追随しており、閾値を緩めて通すような手抜きはしていない。
- **ButtonCell の ConstraintSet 切替**: `buttonStyleSet` の対象を `titleView` → `contentRow` に移した判断は正しい (titleView はもはや root の直接の子ではないため、旧コードのままなら `applyTo` が対象を見つけられず全幅化が壊れていた)。`normalLayoutSet` は構築直後に `clone(views.root)` しているため既定配置を正しく退避しており、ボタンスタイル→通常レイアウトの復帰も `CellRowWidthAllocationTest.kt:487` で検証されている。
- **accessory 系 Cell (Switch / checkbox / chevron) の配置**: `accessoryHolder` の ConstraintSet (END/TOP/BOTTOM = parent) は無変更。`UnifyCellCommonFieldsTest.kt:396`, `:520` が成功。実機証跡でも配置不変。
- **description が Cell 級アクセサリと重ならないこと**: `descriptionView.END = accessoryHolder.START` は無変更。`after-basic-cells-pixel6a.png` の SwitchCell / LabelCell で description が Switch 手前で折り返している。
- **hintText の右上 float と Z 順**: `addView` 順序 (accessoryHolder → hintTextView) を維持。`UnifyCellCommonFieldsTest.kt:360`, `:430` が成功。
- **android/ADR-0001 の IME payload 経路**: `EntryCellViewHolder.bind` は KDoc 以外に差分がなく、`inputType` の差分ガード (`EntryCellViewHolder.kt:96-98`)、`hint` の差分ガード、`filters` の差分ガードすべて無傷。`InputCellsTest.kt:180` が**新 View 階層のもとで**成功している (テストヘルパ `createEntryCellViewHolder` を `addFillingInlineTrailing` 経由に追随させた変更は、本番 `create()` と同じ構成にする正しい修正)。payload 発行自体は Adapter 層で View 階層に非依存。
- **公開 API**: 追加された `CellBaseViews.contentRow` と `addFillingInlineTrailing` はいずれも `internal`。公開シグネチャの変更なし。
- **テストが実装の追認になっていないか**: `CellRowWidthAllocationTest` は「主行幅 − title 幅 = trailing 幅」「`editText.minWidth == 0`」「title を主行幅より広くして幅 0 まで縮む」など、**実装を書き換えたら落ちる**性質の検証になっている。Robolectric が ellipsize を実行しない制約は KDoc (`:71-79`) で正確に説明され、代替として構成 + 表示幅制約を検証し実描画は実機に委ねると明記している。制約の認識・開示ともに適切。
- **Kotlin 規約**: `val` 中心、`?.`/`?:` の適切な使用、`!!` なし、公開/内部 API への KDoc、既存コードスタイルとの一貫性いずれも問題なし。`addFillingInlineTrailing` が `views.titleView.layoutParams` を書き換える副作用は KDoc に明示されており、呼び出しが `create()` の 1 箇所に限られるため許容範囲。

## アクションプラン

1. **[要オーナー判断]** ButtonCell の `titleAlignment` 実効化 (指摘 1) を「受け入れ + deviation 記録 + 位置テスト追加」とするか「旧挙動維持」とするかを決める。
2. **[要オーナー判断]** title の 1 行化 (指摘 2) を承認し、brief.md の「合意済み妥協」から `deviation.md` へ移す。裏取り済みの根拠 (原典 `cellbaseview.axml` の `CellTitle` singleLine / iOS `titleLabel` numberOfLines=1) を記録に含める。
3. 蒸留時: `concepts/core/styling/cell-row-layout.md` の「Android の EntryCell は入力フィールドを accessory 領域に置く既存配置を維持する」を撤回し、title の 1 行 + 末尾省略規則を追記する (ADR-0002 の Consequences が予告済み)。
4. 任意: 指摘 3 (到達不能な防御コード) の整理、指摘 4 (title の paddingEnd 6dp) の要否判断。
