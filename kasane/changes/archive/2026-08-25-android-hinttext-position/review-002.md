# レビュー結果: android-hinttext-position (002 回目)

**日付**: 2026-08-24
**判定**: APPROVED

## サマリー

review-001 の指摘 7 件 (Critical 1 / Major 2 / Minor 2 / Suggestion 2) はすべて実装とテストで解消されている。root padding 廃止が GONE な View 経由の余白へ波及していた 3 経路 (行右端 / chain 下端 / icon・accessory の上下) はいずれも `goneMargin` とマージンで補われ、新設の回帰テストが**実際に検出力を持つ**ことを本レビューのミューテーションプローブで実測確認した (5 種の変異でそれぞれ対応するテストだけが落ちる)。修正による新たな回帰は見つからなかった。

`cd android && ./gradlew test --rerun-tasks` は BUILD SUCCESSFUL、**2638 tests / 0 failures / 0 errors / 0 skipped** (review-001 時点の 2626 から +12 = 新設 6 テスト × debug/release 2 variant)。再撮影された実機証跡 `evidence/android-after.png` を画素実測したところ、hint の右マージンは 10.2〜11.6dp (修正前は 26〜27dp)、ButtonCell 行 (`登録 … 送信`) の右マージンは 16.7dp (修正前 16.6dp) で、review-001 の Critical が指摘した「行右端の余白 0」は画面上でも解消している。

残る指摘は 🟡 Minor 2 / 🔵 Suggestion 1 でいずれも優先度は低く、実装の正しさには影響しない。

## review-001 指摘の解消状況

| review-001 の指摘 | 状態 | 根拠 |
|---|---|---|
| 🔴 Critical: `accessoryHolder` GONE 行で行右端 16dp が消える | 解消 | `CellBaseLayout.kt:345` / `:358` に END 側 `goneMargin`。回帰テスト `accessoryHolder が GONE の行でも行右端の余白が残る` / `… description も行右端の余白の内側に収まる` を新設。証跡の ButtonCell 行でも右マージン 16.7dp を実測 |
| 🟠 Major: `descriptionView` GONE 行で内容が約 2dp 下へずれる | 解消 | `CellBaseLayout.kt:342` に BOTTOM 側 `goneMargin`。回帰テスト `description が無い行でも本体行はアクセサリと同じ縦中央に置かれる` を新設。クラス doc (`:64-70`) も実装に合わせて改訂済み |
| 🟠 Major: `iconView` / `accessoryHolder` が上下の行余白を失う | 解消 | `CellBaseLayout.kt:307-321` (iconView) / `:387-401` (accessoryHolder) に TOP/BOTTOM マージン。回帰テスト `行高より大きい icon を指定しても icon は行の上下端に密着しない` (80dp icon で行高 88px・上下 4px を実測) を新設 |
| 🟡 Minor: 下端ガードの「効く局面」が未検証 | 解消 | NATIVE graphics 側 (`CellRowWidthAllocationTest`) に `行高に対して hint が大きいとき下端ガードが hint を縮める` を新設。「1 行の自然高 > 実測高さ」でガードが実際に縛っていることを先に確かめてから下端 12dp を測っており、空振りしない構成になっている。legacy 側の doc コメントも「legacy graphics では作れない」に正されている |
| 🟡 Minor: 実機スクショの置き場 | 解消 | `evidence/android-after.png` へ移動済み (オーケストレーター実施) |
| 🔵 Suggestion: 縦 4dp の定数化 | 解消 | `CELL_ROW_VERTICAL_MARGIN_DP` を新設し `CellBaseLayout` / `ButtonCellViewHolder` の両所から参照。`CELL_ROW_HORIZONTAL_PADDING_DP` → `CELL_ROW_HORIZONTAL_MARGIN_DP` のリネームも完了 (旧名の残存 0 件) |
| 🔵 Suggestion: `buttonStyleSet` が translationY を 0 に戻す | 解消 | `CELL_ROW_OPTICAL_CENTER_OFFSET_DP` を新設し `ButtonCellViewHolder.kt:49` の `setTranslationY` で `buttonStyleSet` にも持たせた。回帰テスト `ButtonCell のボタンスタイルでも本体行の光学中心補正が残る` を新設 |

## ミューテーションプローブ (新設テストの検出力の実測)

lessons/code-review L-001 に従い、新設テストが本当に回帰を検出するかを実装への一時変異で確かめた。変異は 2 バッチで適用し、`:ks-settingsview-ui:testDebugUnitTest` を実行、結果取得後に backup から復元して shasum 一致 (`CellBaseLayout.kt` = `80a40ea…`、`ButtonCellViewHolder.kt` = `ddad733…`) と `git status` の一致で原状復帰を確認済み。

| 変異 | 落ちたテスト | 実測メッセージ |
|---|---|---|
| `contentRow` の END goneMargin → 0 | `accessoryHolder が GONE の行でも行右端の余白が残る` | `expected:<304> but was:<320>` |
| `contentRow` の BOTTOM goneMargin → 0 | `description が無い行でも本体行はアクセサリと同じ縦中央に置かれる` | `contentRow.centerY(32.5) ≒ root.centerY(30.0)` |
| `iconView` の TOP/BOTTOM マージン → 0 | `行高より大きい icon を指定しても icon は行の上下端に密着しない` | `expected:<88> but was:<80>` |
| `buttonStyleSet` の `setTranslationY` を除去 | `ButtonCell のボタンスタイルでも本体行の光学中心補正が残る` | `expected:<-1.0> but was:<0.0>` |
| hint の下端ガード 12dp → 0dp | `hintTextView は cell 下端から 12dp の下端ガードを持つ` / `行高に対して hint が大きいとき下端ガードが hint を縮める` | `expected:<12> but was:<0>` (両方) |

いずれの変異でも**対応する新設テストだけ**が落ち、既存テストは 1 件も落ちなかった。これは新設テストが唯一の検出者であること (= 既存スイートではこれらの回帰を捕まえられなかったこと) を同時に示している。実測値は review-001 が一時プローブで得た値 (320 / 32.5 vs 30.0 / 80) と完全に一致する。

## 指摘事項

### [🟡 Minor] ボタンスタイルの光学補正復帰が deviation.md に記録されていない

**該当箇所**: `deviation.md`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:49`

**問題点**:
`buttonStyleSet` に `setTranslationY` を持たせたことで、ボタンスタイルの `ButtonCell` (`ログアウト` のような aux なし行) のタイトルは本変更前より 1dp 上がる。合意済みスコープ (hint の位置) が沈黙している領域で利用者から見える描画が変わっており、lessons/process L-001 の記録対象に当たる。deviation.md は 21:49 が最終更新で、review-001 対応で入った修正はどれも反映されていない。

同 change の他の修正 (END/BOTTOM の goneMargin、icon・accessory の上下マージン) は**本変更前の見え方を復元する**もので net の変化がないため記録不要だが、この 1 件だけは既存挙動を積極的に変えている。deviation.md は既に同型の ButtonCell 側修正を `[付随修正]` として記録しているので、記録の粒度としても揃わない。

**推奨修正**:
deviation.md に `[隣接修正]` として 1 行追記する。「`ConstraintSet.applyTo` が translation を既定値で上書きするためボタンスタイル行だけ android/ADR-0004 の光学補正が 0 に戻っていた。同じ `buttonStyleSet` を本変更で書き換えるため同 change で修正 (見た目は 1dp 上へ)。回帰テスト追加済み」程度で足りる。

---

### [🟡 Minor] 再撮影した実機証跡にも通知アイコンが写り込んでいる

**該当箇所**: `evidence/android-after.png`

**問題点**:
ksn-core の撮影規律は「実機で撮るときは通知…が写らない状態にしてから撮る」と定めている。review-001 が同じ点を「次回撮影時は通知を消してから撮ること」として挙げたが、再撮影された画像のステータスバーには依然として通知アイコンが 4 個ほど残っている。個人を特定する情報ではないため証跡としての価値は損なわれていないが、`kasane/` 配下の画像は commit 後に git 履歴から消せない以上、防波堤は撮影時にしか置けない。指摘済みの規律が次の撮影で守られていない点を残しておく。

**推奨修正**:
今回の証跡は判定を保留するほどのものではないので差し替えは必須としない。次に実機証跡を撮る際は、撮影前に通知シェードを消化する (またはエミュレータ + デモデータで撮る) 運用を実際に踏むこと。

---

### [🔵 Suggestion] hint の 2dp / 10dp / 12dp が定数化されておらず、実装とテスト 3 ファイルに散在している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:166-169`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/UnifyCellCommonFieldsTest.kt:374,380,426,443`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CellRowWidthAllocationTest.kt:589,907-908`

**問題点**:
行の余白は今回 `CELL_ROW_HORIZONTAL_MARGIN_DP` / `CELL_ROW_VERTICAL_MARGIN_DP` / `CELL_ROW_OPTICAL_CENTER_OFFSET_DP` として定数へ引き上げられたのに対し、本変更の主題そのものである hint の 3 値 (上 2dp / 右 10dp / 下端ガード 12dp) はローカル変数のリテラルのままで、テスト側 2 ファイルにも同じリテラルが計 7 箇所複製されている。iOS 側の値と対で管理すべき「パリティの数字」がコードから読み取りにくく、値を変えたときに落ちるテストのメッセージだけが手掛かりになる。

**推奨修正**:
`CELL_HINT_MARGIN_TOP_DP` / `CELL_HINT_MARGIN_END_DP` / `CELL_HINT_BOTTOM_GUARD_DP` (名前は任意) を `CELL_ROW_*` の隣に置き、実装とテストの双方から参照する。KDoc に「iOS の `KsListCellBase` の hintLabel と同値」の一文を添えると、次に iOS 側を触る人がパリティの存在に気づける。

---

## 確認して問題がなかった観点

- **合意済みスコープの充足**: hint の実効位置が cell 外縁から 上2dp / 右10dp であること (`UnifyCellCommonFieldsTest` の実測アサーション + `evidence/android-after.png` の画素実測で右 10.2〜11.6dp)、下端ガード 12dp と左端ガード 16dp が入っていること、長い hint がフォント縮小ではなく末尾省略になることを確認した。exploration.md が挙げた「現行テストの期待値を外縁基準へ書き直す」も完了している。
- **ビルドとテスト**: `cd android && ./gradlew test --rerun-tasks` が BUILD SUCCESSFUL (4m54s)。件数は `build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` 集計で 2638 tests / 0 failures / 0 errors / 0 skipped (test-execution.md の Android 節に従い件数まで確認)。なお本レビューの worktree には `android/local.properties` が無く (gitignore 対象)、SDK パスは `ANDROID_HOME` で与えて実行した。
- **A/B 証跡の対応**: 修正前 `ui/references/android-current.png` では hint が Switch と重なり右マージン 26〜27dp、修正後 `evidence/android-after.png` では 10dp 前後で Switch の上へ退避しており、正である `ui/references/ios-current.png` と同じ見え方になっている。ButtonCell 行の右余白も 16.7dp で iOS と揃う。証跡のタイムスタンプ (22:32) は review-001 対応の実装 (22:28) の後であり、再撮影の要件 (lessons/process L-003 (3)) を満たしている。
- **足場アーティファクトの不可侵**: `exploration.md` の最終更新は 21:29、`deviation.md` は 21:49 で、いずれも review-001 (22:16) より前。実装・レビュー対応中の書き換えは無い。
- **`ConstraintSet` の退避経路**: `normalLayoutSet` は `buildCellBaseViews` 直後の root を `clone()` しており、新設した `goneMargin` 群と `translationY` を取り込む。aux ありの `ButtonCell` が `normalLayoutSet.applyTo` を経由して行右端 16dp を保つことは新設テストが実測で担保している (clone が goneMargin を落としていないことの間接証明にもなっている)。
- **`applyIconFrame` の LayoutParams 更新**: icon サイズの再評価は既存 `layoutParams` の `width` / `height` を書き換えて再代入する形で、制約とマージンを作り直さない。新設した icon の TOP/BOTTOM マージンは bind をまたいで保持される (80dp icon のテストが bind 経路を通っている)。
- **root padding への外部依存の不在**: `android/` の main ソースで cell root の padding を読む箇所は無い。罫線 (`ClassicSectionDecoration` / `ModernSectionDecoration`) が読むのは RecyclerView 側の padding。`MinHeightConstraintLayout` は measuredHeight と minimumHeight の比較のみで padding を参照しない。`CustomCellViewHolder` は定数をリネーム後の名前で参照するだけ。
- **`accessoryHolder` を GONE にする経路の網羅**: `accessoryHolder.visibility = GONE` は `ButtonCellViewHolder` の 2 箇所のみで、いずれも新設テストの対象に入っている。`accessoryHolder` の高さは常に `WRAP_CONTENT` で、TOP/BOTTOM に同値のマージンを与えても縦中央配置と「アクセサリが行より高いとき行が伸びる」挙動は変わらない。
- **concepts / ADR との整合**: `concepts/core/styling/cell-row-layout.md` は行の余白の実現手段 (padding か margin か) を規定しておらず、「hintText は行の右上を基準にする」「Cell 級アクセサリはセル全体に対して垂直センター」「任意要素がないときに空領域を残さない」のいずれも本実装で満たされる。core/ADR-0011 も構造の実現手段までは縛っていない。concepts / ADR の更新を要する乖離は無い。
- **コメント規約**: 新規・改訂コメントの外部参照は `android/ADR-0002` / `android/ADR-0004` / `core/ADR-0026` と移植元 AiForms の現在形の互換仕様記述のみで、change 名・Phase 通番・レビュー通番・アーカイブ文書パスの混入は無い。`python3 scripts/comment-policy-lint.py` は 677 ファイル走査で 0 件、`--selftest` も全件 OK。`local-path-lint.py` / `identity-lint.py` も検出 0 件。
- **hint の下端ガードと iOS のセマンティクス差** (実害なしと判断): iOS の hintLabel は `bottom <= cell.bottom - 12` という不等式制約で、cell の高さを押し広げない。Android は TOP/BOTTOM 両接続 + `verticalBias = 0` + `constrainedHeight` で近似しているため、`wrap_content` の行では原理的に「hint 高 + 14dp」が行高の下限に加わる。既定の hint フォントでは 35dp 前後にしかならず最低行高 60dp に吸収されるため実害は無く、超える場合に行が伸びるのは可変高さの契約 (`concepts/core/styling/cell-row-layout.md` の「高さの解決」) とも整合するので、指摘としては挙げない。
- **`descriptionView` の END 側 goneMargin の到達性**: `description` を持つ Cell で `accessoryHolder` が GONE になる組み合わせは現状の production コードには存在しない (GONE にするのは `ButtonCell` だけで、`ButtonCell` は `description = null` を渡す)。対応するテストが `applyCellBaseLayout` を直接叩く合成的な構成になっているのはこのためで、対称性を保つ防御的実装として妥当と判断した。

## アクションプラン

判定は APPROVED であり、以下はいずれもマージの前提ではない。

1. 🟡 deviation.md に `[隣接修正]` としてボタンスタイルの光学補正復帰を 1 行追記する (lessons/process L-001)。
2. 🟡 次回の実機証跡は通知を消した状態 (またはエミュレータ + デモデータ) で撮る。
3. 🔵 hint の 2dp / 10dp / 12dp を定数化し、実装とテスト 3 ファイルの重複を解消する。
