# レビュー結果: datepickercell-color-adjust (001 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの全 Requirement / Scenario が実装され、テスト (33 クラス・failures 0) と lint (本変更ファイルへの指摘 0 件)
は緑、足場アーティファクトの逆流も無い (詳細は verify-001.md)。TimePicker で確立した構造 (色束の解決 → attach 1 行 →
ヘルパ 1 クラスに走査を閉じる) を素直に横展開できており、机上仮説 (CJK 大フォント) を実機スパイクで棄却して真因
(AppCompat 名前空間属性が解釈されない) に切り替えた経緯が brief に残っている点も良い。

Critical / Major は無い。一方で、(1) 兄弟クラス側では型ガード済みのキャストが本クラスでは無防備になっており、
material 更新時の失敗モードが「着色が外れる」ではなく「クラッシュ」に変わる、(2) ADR-0006 で実際に踏んだ
「毎フレーム再適用による描画ループ」の回帰テストが TimePicker 側にはあるのに本クラスには無い、の 2 点は
いずれも修正が数行で済むわりに守れる範囲が大きいため、優先度の高い Minor として CHANGES_REQUESTED とする。
**現状の挙動に不具合は見つかっていない** — 指摘はいずれも将来の破綻に対する備えと視覚品質である。

## 指摘事項

### [🟡 Minor / 優先度: 高] 型ガードのないハードキャストで、material 更新時にクラッシュ経路が生まれる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:253,255`

**問題点**: `applyStaticRole` の 2 分岐だけが `view as TextView` の無防備なキャストになっている。同ファイルの他の分岐は
`(view as? ImageView)` / `(view as? Button)` / `view as? MaterialButton` と安全キャストで、`applyDynamicRole:288` の
`view as TextView` は `isCalendarItem` (`:369-370` で `view !is TextView` を弾く) で型が保証されている。
TimePickerColorizer 側も同様で、唯一のハードキャスト `:264` は `isClockFaceNumber`
(`TimePickerColorizer.kt:272-274` の `view is TextView &&`) で守られている。本クラスの 2 箇所だけが例外。

このクラスは pre-draw のたびに走るため、`mtrl_picker_title_text` / `mtrl_picker_header_selection_text` の型が
将来の material で変わると、着色が静かに外れるのではなく毎フレームの `ClassCastException` でアプリが落ちる。
`DatePickerMaterialContractTest` は自リポジトリのビルドでしか動かず、AAR を利用するアプリ側が
material の解決バージョンを上げた場合は守れない (`build.gradle.kts:85` は `implementation` 宣言だが、
利用側の依存解決でより新しい版に上がりうる)。ADR-0008 の Consequences が「ライブラリを上げる際の追随確認」を
負の帰結として挙げている以上、失敗モードは「劣化」に倒しておきたい。

**推奨修正**: 他分岐と同じく安全キャストに揃える。

```kotlin
view.id == MaterialIds.TITLE_TEXT -> (view as? TextView)?.let { applyHeaderTitle(it) }

view.id == MaterialIds.HEADER_SELECTION_TEXT ->
    (view as? TextView)?.setTextColor(textCsl)
```

`applyDynamicRole:289` も同様に安全キャストにするか、`isCalendarItem` と同型の述語で型を保証する。

### [🟡 Minor / 優先度: 高] 「毎フレーム再適用しない」の回帰テストが無い (TimePicker 側にはある)

**該当箇所**: `DatePickerColorizer.kt:196-205` (pre-draw フック) / `:285-291` (動的経路) / `:322-341` (選択日テキストの再調整)
に対する `DatePickerColorizerTest`

**問題点**: tasks 3.3 は「不要な毎フレーム再適用をしない」を明示的な要求として持ち、ADR-0006 の Decision 3 は
この失敗モード (静的/動的を分離しないと `setBoxStrokeColorStateList` 等が無条件に再描画を要求し 60fps で回る) を
**実機計測で実際に踏んだ結果**として記録している。TimePicker 側にはこれを守るテストが 3 件ある
(`TimePickerColorizerTest.kt:234` 静的な部位は 2 回目の走査で再着色されない / `:248` 入力欄の枠は 2 回目の走査で
再設定されない / `:262` 文字盤の数字は走査のたびに再着色される)。

DatePicker 側は同型のテストが 1 件も無い。しかも本クラスは動的経路に `setTextSize` / `firstBaselineToTopHeight`
という**レイアウトを要求する** API を新たに持ち込んでおり (`:333, 337`)、その抑止は `fittedText` / `fittedWidth` の
2 変数によるガード (`:327`) だけに掛かっている。ここが壊れても (例えば将来 `applyDateTextInput` を動的側へ移す、
ガード条件を書き換える) テストは緑のまま、実機でのみジャンクとして現れる。読み取り側の負担も大きい部類の性質なので、
テストで固定しておきたい。

**推奨修正**: `TimePickerColorizerTest.kt:210-260` の spy パターン (`setTextColor` /
`setBoxStrokeColorStateList` の呼び出し回数を数える TextView / TextInputLayout サブクラス) を再利用して、

- 2 回目の `colorize` で静的部位 (曜日ラベル・OK/キャンセル・入力欄の枠) が再設定されないこと
- テキストと幅が変わらない 2 回目の `colorize` で選択日テキストの `setTextSize` / `setPadding` が呼ばれないこと
- 逆に、選択日テキストを差し替えた後の `colorize` では再調整が入ること (動的側が死んでいないことの対)

を検証する。3 番目まで入れて初めて「静的/動的の分離」が両方向で固定される。

### [🟡 Minor] 横向きで選択日テキストが下限まで縮んだ上に折り返す (幅合わせが単一行前提)

**該当箇所**: `DatePickerColorizer.kt:344-355` (`fitTextSize`)

**問題点**: 収まり判定に `paint.measureText(text)` を使っているため「1 行に収まるか」しか見ていない。実際の
`mtrl_picker_header_selection_text` は `maxLines` / `singleLine` を持たず折り返せる (material 1.12.0 の
`res/layout/mtrl_picker_header_selection_text.xml`)。横向きではヘッダ幅が
`mtrl_calendar_landscape_header_width` に固定されるため、日本語の日付は 1 行に収まらず、下限 (`:519` の 0.5 倍) まで
縮めてもなお折り返す — つまり縮小が結果に効いていない。`ui/verification/07-calendar-landscape.png` でも
「2026年6月1」「日」の 2 行折り返しになっており、縦向き (0.8 倍) に比べて明らかに小さい。

Requirement は「クリップされずに読める」であり、承認モックは縦のみなので**仕様違反ではない**。brief 末尾にも
横向きの折り返しは記録済み。ただし「縮めたのに折り返した」状態は A 案 (標準ヘッダ高さのまま収める) の意図とは
ずれた見え方になっている。

**推奨修正**: 折り返し後の実寸で判定する。`StaticLayout` (または `Layout` 取得後の `lineCount` / `height`) で
「表示領域の高さに収まるか」を見るか、あるいは `view.maxLines == 1` のときだけ幅合わせを行い、
複数行を許容する構成では 0.8 倍のまま折り返しに任せる。後者なら数行で済む。

### [🟡 Minor] ヘッダの重なり検証が、実レイアウトではなく手組みの合成ヘッダ + ハードコード寸法に依存している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizerTest.kt:478-493`
(`header()`) / `:543-551` (`HEADER_WIDTH_DP` 244 / `HEADER_HEIGHT_DP` 120 / `TITLE_TEXT_SIZE_SP` 14 / `SELECTION_TEXT_SIZE_SP` 32)

**問題点**: 本変更が直そうとしている不具合そのもの (タイトルと選択日の重なり) は**レイアウトの性質**であり、
その保証が「実機計測に一致する dp 値」と「material 既定と称する sp 値」を手で書き写した合成 FrameLayout に
乗っている。実物は `?attr/materialCalendarHeaderTitle` / `...HeaderSelection` のスタイル (lineHeight・
`includeFontPadding`・`paddingBottom=mtrl_calendar_pre_l_text_clip_padding`・`gravity=start|bottom`) と
`mtrl_calendar_header_text_padding` を伴っており、これらは再現されていない。material がヘッダの文字サイズや
行高を変えた場合、テストは緑のまま実機だけが重なる。

同じ変更の `DatePickerMaterialContractTest.kt:49,82` が**実物の `mtrl_picker_dialog` を Robolectric で inflate
できている**ので、実経路の検証手段は既に手元にある。lessons/inbox の `test-asserts-proxy-not-real-path`
(scope: test / count 2) と同型の構図でもある。

**推奨修正**: 契約テストと同じ `inflate(MaterialR.layout.mtrl_picker_dialog)` で得た階層に対して
`colorize` → measure/layout を掛け、`mtrl_picker_title_text` / `mtrl_picker_header_selection_text` の
描画上端・下端で重なりを判定する。合成ヘッダのテストは「幅に収まらないときの追加縮小」のような
入力値を作り込みたいケースに限定して残せばよい。

### [🔵 Suggestion] `MaterialIds.TEXT_INPUT_DATE` が未参照で、KDoc の主張と実態がずれている

**該当箇所**: `DatePickerColorizer.kt:467-472` (KDoc) / `:484` (`TEXT_INPUT_DATE`)

**問題点**: KDoc は「参照箇所を 1 か所へ集めて契約テストで存在を検証できるようにする」と述べているが、
(a) `TEXT_INPUT_DATE` は本体のどこからも参照されていない (入力欄の判定は `view is TextInputLayout`、`:270`)、
(b) 契約テストは `MaterialIds` を経由せず `MaterialR.id.*` を直接参照している (`DatePickerMaterialContractTest.kt:51-62`)。
結果として「1 か所に集約 → 契約テストで検証」という関係が成立していない。

**推奨修正**: 使わないなら `TEXT_INPUT_DATE` を削除する。集約の意図を保つなら契約テストの参照を
`DatePickerColorizer.MaterialIds.*` 経由に変え、KDoc の主張と実装を一致させる。

### [🔵 Suggestion] 背景ロール (window 背景) の適用に自動テストが無い

**該当箇所**: `DatePickerColorizer.kt:175-184` / `:458-465` (`unwrapShapeDrawable`)

**問題点**: デルタスペックの色ロール 4 つのうち背景ロールだけが自動テストを持たず、証跡が
`ui/verification/01-calendar.png` の目視のみ。特に `InsetDrawable(MaterialShapeDrawable)` の unwrap は
TimePicker 版 (`TimePickerColorizer.kt:156-162` は `MaterialShapeDrawable` 直判定) には無い本変更固有の追加分で、
ここが外れると「角丸は残るが色だけ既定のまま」という気づきにくい壊れ方をする。

**推奨修正**: `DialogFragment` を立てなくても、`InsetDrawable(MaterialShapeDrawable)` /
`RippleDrawable` 包み / 想定外 Drawable の 3 ケースで `unwrapShapeDrawable` の結果 (と fallback の分岐) を
検証すれば足りる。`applyToWindowBackground` を検証可能な粒度に切り出す必要は無い。

### [🔵 Suggestion] リネームにより長命層の記述が旧ファイル名を指している (蒸留時の宿題)

**該当箇所**: `kasane/decisions/android/0006-timepicker-dialog-runtime-coloring-via-view-traversal.md:49` /
`kasane/decisions/android/0008-datepicker-dialog-coloring-and-header-fix-via-view-traversal.md:25`

**問題点**: `TimePickerColors.kt` → `PickerDialogColors.kt` のリネーム (tasks 2.1 で許可済み) により、
ADR-0006 の「現行照合」行が存在しないファイル名を指すようになった。ADR-0008 の Decision 1 も
`TimePickerColors` の名前で導出規則の再利用を述べている。

**推奨修正**: 本変更では触らない (長命層の更新は蒸留の責務)。ksn-distill の際に ADR-0006 の現行照合行を
新ファイル名で貼り直し、ADR-0008 側は「(現 `PickerDialogColors`)」の補記に留めるのが妥当。

## 確認して問題が無かった観点

- **仕様充足**: 全 Requirement / Scenario の実装・テスト対応は verify-001.md の対応表のとおり。tasks.md の
  虚偽チェック無し、`proposal.md` / `specs/` の逆流無し、`ui/brief.md` は追記のみ
- **色ロールの数値**: `PickerDialogColors` に追加された 3 派生色の期待値をテストで手計算検証済み
  (`PickerDialogColorRolesTest.kt:152-185`)。暗背景で明暗関係が反転しないことまで見ているのは良い
- **既存挙動の非破壊**: `TimePickerColors` → `PickerDialogColors` のリネームは型名のみで、TimePicker 側の
  導出規則・既存アサーションの意味は変わっていない (proposal Non-Goals どおり)
- **対象外部位の保護**: `isTextInputDecoration` (`:444-450`) による除外と、それを固定するテスト
  (`DatePickerColorizerTest.kt:154-167`)。`ui/verification/06-text-input-invalid.png` でも error 表示が
  Material 既定の赤のまま残っている
- **コメントの自己完結性**: `SELECTION_TEXT_SCALE` / `SELECTION_TEXT_MIN_SCALE` / `DISABLED_ALPHA` /
  `SUBDUED_ALPHA` はいずれも「値の意味と上下させたときの影響」で説明されており、モック ID への依存だけで
  済ませていない (timepickercell-color-adjust review-001 の Major と同型を踏んでいない)
- **リソース解放**: pre-draw フックの解除 (`:207-212`) と `FragmentLifecycleCallbacks` の自己解除 (`:150-154`)、
  `styledViews` の弱参照集合化 (`:123-124`)。ダイアログ再表示ごとに新しい Colorizer を張る構造も TimePicker と同一
- **lint**: `PrivateResource` の抑制は着色ヘルパ 2 クラスのみに限定 (tasks 5.2 の要求どおり)。
  本変更の追加・変更ファイルに対する lint 指摘は 0 件

## アクションプラン

1. (必須) ハードキャスト 2 箇所を安全キャストへ (`DatePickerColorizer.kt:253,255`、必要なら `:289` も)
2. (必須) 冪等性の回帰テスト 3 件を追加 (静的部位の非再設定 / 選択日テキストの非再調整 / 差し替え後は再調整される)
3. (任意・同サイクル推奨) 幅合わせを折り返し前提に直す、またはヘッダ検証を実物 inflate へ寄せる
4. (任意) `TEXT_INPUT_DATE` の削除または契約テストの `MaterialIds` 経由化 / 背景ロールの自動テスト追加
5. (蒸留時) ADR-0006 の現行照合行のファイル名更新
