# レビュー結果: relax-android-host-prerequisites (001 回目)

**日付**: 2026-08-27
**判定**: CHANGES_REQUESTED

## サマリー

3 capability のデルタスペック (android-theming / android-timepicker / android-datepicker) の Requirement / Scenario は、実装とテストでおおむね正面から満たされている。同梱テーマの常時ラップは「ライブラリ所有 UI / 利用者所有コンテンツ」の境界を Context ヘルパ 2 本 (`ksThemedContext` / `ksHostContext`) に集約でき、Fragment 依存機構の撤去も残骸なく完了している。新規テストは反証性の担保 (2 つのホストテーマが実際に異なることを先に assert する等) まで設計されており質が高い。

一方で、カレンダー選択面の表示継続機構に **構成変更ではないバックグラウンド遷移 (Home キー・他アプリ起動) で選択面が黙って失われる** 欠陥がある。旧 `MaterialDatePicker` (DialogFragment) は生存していた経路であり、spec が沈黙する範囲の利用者可視の退行で deviation にも記録がない。これを Major として CHANGES_REQUESTED とする。他は Minor / Suggestion。

### 実行して確認した客観的事実 (本レビューで自ら実行)

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`build/test-results/test*UnitTest/TEST-*.xml` 集計で **2552 tests / 0 failures / 0 errors** (debug + release の延べ、1276 件 × 2)
- `cd maui && dotnet test` → **466 合格 / 0 失敗**
- `cd samples/android && ./gradlew assembleDebug --rerun-tasks` → BUILD SUCCESSFUL (99 tasks executed。`material` / `fragment` の明示依存を外した状態でのコンパイル成立を確認)
- `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` → いずれも exit 0 (禁止 0 件 / 検査対象 683 ファイル)

## 指摘事項

### [🟠 Major] バックグラウンド遷移でカレンダー選択面が失われる (構成変更ではない `onSaveInstanceState` で無条件 dismiss)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:849-861` (`onSaveInstanceState`)

**問題点**:

`onSaveInstanceState` が、表示中のカレンダー選択面を無条件に `dismissActiveCalendarDialog()` している。

```kotlin
if (dialog != null && cellId != null && dialog.isShowing) {
    if (!hasAmbiguousLibraryDefaultId()) { ... }
    dismissActiveCalendarDialog()
}
```

`Activity.onSaveInstanceState` は構成変更のときだけでなく、**API 28 以降は Activity が stop するたび** (Home キー、他アプリへの遷移、別 Activity の起動) に呼ばれる。この経路では Activity は再生成されないため `onRestoreInstanceState` も呼ばれず、`pendingCalendarRestore` に何も積まれない。結果として:

- カレンダー選択面を開いた状態で Home → 復帰すると、選択面は消えており、途中まで進めた選択も失われる
- 同じ状況でボトムシート系選択面 (Picker / Number / DatePicker Spinner / 新 TimeSelectionSheet) はそのまま生存するため、同一ライブラリ内で挙動が割れる
- 旧経路の `MaterialDatePicker` は `DialogFragment` であり復帰後も表示が続いていたため、利用者可視の退行になる (proposal の Impact にも deviation.md にも記録がない)

なお、この dismiss は Activity 破棄時の window leak 対策としても不要になっている。`DateCalendarDialog.showAnchoredTo` (`DateCalendarDialog.kt:270-287`) が ViewTree の lifecycle を購読して `onDestroy` で `dismiss()` しており、破棄経路は既に塞がれている (`evidence/spike-findings.md` の実装方針 4 は、この observer を入れる前の実測と読める)。

**推奨修正**: 状態の保存 (`saved.calendarCellId` / `calendarDisplayState`) は無条件のまま残し、**dismiss だけを「実際に破棄される場合」に限定する**。具体的には `onSaveInstanceState` からの dismiss を落として `showAnchoredTo` の `onDestroy` observer に一本化するか、ホスト Activity の `isChangingConfigurations` / `isFinishing` で分岐する。落としたうえで、回転で WindowLeaked が出ないことを実機で再実測し、`DateCalendarRecreationTest` の `状態保存の時点で表示中の選択面は閉じられ通知しない` を「破棄をまたぐ場合に閉じる」へ読み替えたうえで、「保存だけが起きて破棄されない場合は開いたまま」の Scenario をテストに足すこと。

### [🟡 Minor] 未使用になった id リソースが残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/res/values/ids.xml:7`

**問題点**: `ks_date_picker_today_button` は `DatePickerTodayShortcut` (削除済み) が「今日」ボタンの差し込み重複防止と着色走査の部位判定に使っていた id。撤去後は参照ゼロで、コメントも存在しない機構を説明している (タスク 6.1 の削除漏れ)。ライブラリの公開リソースとして aar に出続ける。

**推奨修正**: `<item name="ks_date_picker_today_button" type="id" />` とその説明コメントを削除する。

### [🟡 Minor] CustomCell の「利用者所有コンテンツはホストテーマのまま」テストが本番配線を通っていない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/HostThemeIndependenceTest.kt:377-400`

**問題点**: このテストは `CustomCellViewHolder(activity)` を直接 new し、`LocalContext.current` が host の `colorPrimary` を解決することだけを見ている。渡した Context がそのまま返るだけなので、実装側の `registerCustomCell` の `parent.context.ksHostContext()` (`KsCellRegistryCustomCell.kt:29`) を `parent.context` や `parent.ksThemedContext()` に書き換えてもテストは緑のままで、回帰検出力がない。デルタスペック android-theming の Scenario「利用者所有コンテンツはホストテーマのまま」は CustomCell を明示している。

同じファイルの `KsAnyView` 側テスト (:339-374) は `showRoot` 経由で本番配線を通し、かつ「ライブラリ所有の行は別の値を解決する」という反証アサーションまで置いており、こちらが模範。

**推奨修正**: CustomCell も `showRoot` で root に `CustomCell` を載せ、`registerCustomCell` が張った factory 経由で生成された ViewHolder の content Context を観測する形にする。`KsAnyView` 側と同じ「ライブラリ所有の行は別の値を解決する」反証アサーションも添えること。

### [🟡 Minor] TimeSelectionSheet がホストの破棄に追随しない (回転時の window leak)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:55-66`

**問題点**: `TimeSelectionSheet(...).show()` は素の `show()` であり、`DateCalendarDialog.showAnchoredTo` のような lifecycle 追随を持たない。Activity 再生成のときにダイアログを閉じる主体がいないため、`evidence/spike-findings.md` が実測した WindowLeaked と同型の leak が出る。spec の「構成変更で閉じる」は満たされる (再提示されない・無発火) が、閉じ方が clean ではない。

旧 `MaterialTimePicker` は `DialogFragment` で FragmentManager が破棄を面倒見ていたため、時刻選択面については退行にあたる。既存シート 3 種 (`PickerCellViewHolder` / `NumberPickerCellViewHolder` / `DatePickerCellViewHolder` の Spinner 経路) も同じ形なので「既存踏襲」ではあるが、`showAnchoredTo` に相当する仕組みは既にこの change の中に存在しており、寄せるコストは小さい。

**推奨修正**: `DateCalendarDialog.showAnchoredTo` と同じ「ViewTree lifecycle の `onDestroy` で dismiss」をシート系にも共有ヘルパとして適用する。4 シート一括だと本 change のスコープを超えると判断するなら、時刻シートだけ対処して残りは付随修正の判定 (同梱 / 起票) をオーナーに諮ること。

### [🟡 Minor] ui/verification/ に旧ラウンドの証跡が併存している

**該当箇所**: `kasane/changes/relax-android-host-prerequisites/ui/verification/` / `ui/brief.md`

**問題点**: material3 1.3.1 時点の証跡 (`datepicker-calendar-light.png` / `datepicker-yearselect-*.png` / `datepicker-textinput-*.png` / `datepicker-outofrange-*.png`) と、BOM 2025.11.01 で撮り直した `datepicker-m3-140-*.png` が併存している (brief.md 側にも「旧証跡は上書きせず併存」と明記)。ksn-core の ui/ 規約は verification/ を「視覚検証ループの**最終周**の画像」に限り、中間ラウンドは残さない・最終承認時点で verification/ にあるのは brief.md の照合記録が指す集合と同じ、と定めている。

旧新の画素比較そのものは価値のある検証だが、その結論は brief.md の文言 (「有意差は面の縁のアンチエイリアスのみ / 描画そのものの変化はゼロ」) が既に持っている。

**推奨修正**: 最終周 (m3-140 系 + landscape-fixed 系 + timepicker 系 + portrait-after-height-cap) だけを残し、1.3.1 時点の証跡は削除する。1.4.0 で撮り直していないランドスケープ (brief.md 末尾に明記) は、証跡が最終周であることが分かるよう brief.md 側の注記で補うこと。

### [🔵 Suggestion] SavedState の書き出しと読み込みが非対称になり得る

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:1064-1076` (`SavedState.writeToParcel`)

**問題点**: 書き出しは `calendarDisplayState` の null 判定で早期 return し、読み込みは `calendarCellId` の null 判定で早期 return する。判定に使うフィールドが食い違っているため、「`calendarDisplayState` は非 null だが `calendarCellId` が null」の状態では String(null) + long + long + int を書いて String だけ読む形になり、Parcel のカーソルがずれる。現状は `onSaveInstanceState` が 2 つを必ず同時に設定するため到達しないが、`var` で外から書ける以上は将来の変更で踏み得る。

**推奨修正**: 2 つを 1 つの nullable なペア (data class 1 つ) にまとめて分岐点を一本化するか、書き出し側の判定を `calendarCellId == null || display == null` に揃える。

### [🔵 Suggestion] 使われていない系列パラメータと enum

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimeSelectionSheet.kt:25` / `:484-491`

**問題点**: `TimeWheelSeries` と `onSeriesChanged(series)` の引数は、コメントどおり分岐に使われておらず `@Suppress("UNUSED_PARAMETER")` で黙らされている。「経路を追える形」は 3 箇所のラムダ (`hourWheel.onSelectionChanged` 等) が既に表しており、enum は追加の情報を持っていない。

**推奨修正**: `onSeriesChanged()` を引数なしにし、`TimeWheelSeries` を削除する。将来 `DateSelectionSheet` のような系列間の従属が必要になった時点で入れ直すほうが、`@Suppress` を残すより安い。

### [🔵 Suggestion] `minDate > maxDate` のときの非提示が spec / deviation のどこにも現れない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialog.kt:117-134` (`DateCalendarRange.of`)

**問題点**: 利用者の設定ミス (`minDate > maxDate`) では warning ログを残して選択面を出さない。判断としては妥当 (silent failure を避けるログもある) だが、「行タップで選択面が提示される SHALL」に対する新しい例外であり、spec の Requirement にも deviation.md にも記録がない。ホイール型 (`DateSelectionSheet`) が同じ入力でどう振る舞うかとの整合も本文からは追えない。

**推奨修正**: ホイール型の既存挙動と揃っているかを確認したうえで、揃っていれば「両 uiStyle 共通の縮退」として deviation.md に 1 行残す。揃っていなければ、どちらへ寄せるかをオーナーに諮る。

## 確認したが問題なしと判断した観点

- **足場アーティファクトの書き換え**: `specs/` 3 本・`proposal.md` / `design.md` に差分なし。`tasks.md` の変更はチェック更新と 4.6 の追加 (実機観察起点の追加タスク) のみ、`ui/brief.md` の追記は ksn-core が ksn-ui に求める照合結果の記録であり、いずれも凍結違反ではない
- **deviation.md の付随修正**: 記録された 13 件はいずれも本務で触るファイル / 同一能力内で、公開 API・データスキーマ・ADR の決定に触れていない。テスト JVM ヒープ 2g と `PickerDialogColors` の未使用派生色削除はオーナー承認の記録つき。同梱条件を超えるものは見当たらない
- **Fragment 依存機構の撤去の完全性**: `findFragmentManager` / `PickerRestoreRegistry` / `PickerDialogTag` / `TimePickerColorizer` / `DatePickerColorizer` / `DatePickerTodayShortcut` / `MaterialDatePickerPresenter` はソース・テストとも参照ゼロ。`fragment-ktx` の明示依存は ui モジュールと binding csproj の双方から消えている (`ks-settingsview-bridge` の `testImplementation` は本 change の対象外の既存テスト依存)
- **ホストテーマ隔離の境界**: `ksThemedContext` はライブラリ所有 UI (共通行・chrome・4 種の選択面・placeholder)、`ksHostContext` は利用者所有コンテンツ (CustomCell content・`KsAnyView` の Compose / AndroidView) と、`SectionAccessoryViewHolders.kt` / `KsCellRegistry*.kt` / `CellBaseLayout.kt` で一貫している。残る `MaterialColors.getColor` 2 箇所 (`SwitchCellViewHolder.kt:161,166` / `SheetChrome.kt:112`) はいずれも同梱テーマ側の View を渡している
- **Context キャッシュのリーク**: `themedContextCache` は key 弱参照・value も `WeakReference` 越しで、値からキーへの強参照経路がない。夜間モード変化で作り直す条件も入っている
- **タイムゾーン非依存の日付往復**: `toEpochMilliUtc` / `toLocalDateUtc` が UTC 固定で、端末 TZ を見るのは `today()` だけ。`DateCalendarDialogTest` が Kiritimati / Midway の東西両側で往復一致を検証している
- **12/24h 判定**: `timeFormatUsesAmPm` は引用符の開閉と `''` エスケープを正しく扱い、`'at'` / 大文字 `A` / `'a'` を除外する。spec の 4 Scenario と境界 (00:30 / 12:30) が全てテスト済み
- **配色の段階解決**: 選択面 3 種とも `accent` は Cell → CellStyle → Theme の 3 段で、復元経路も同じ解決 (`KsSettingsView.kt:928` のラップ済み Context 経由) を通ることを `DateCalendarRecreationTest` が色ロールの等価比較で確認している
- **テストの検出力**: 新規 3 テストクラスは「観測が空振りしないことの確認」を先に assert する形 (2 つのホストテーマが実際に異なる / 再生成前の状態が初期値と区別できる) を取っており、トートロジー化していない。Compose 描画そのものへの操作を state 操作で代替している範囲は、クラス KDoc に明記のうえ実機証跡が担っている

## アクションプラン

1. **Major**: `onSaveInstanceState` の無条件 dismiss を破棄経路限定に変更し、バックグラウンド復帰で選択面が残ることをテストで固定する
2. **Minor**: 未使用 id (`ks_date_picker_today_button`) を削除する
3. **Minor**: CustomCell のホストテーマ解決テストを本番配線 (`registerCustomCell` 経由) へ差し替え、反証アサーションを添える
4. **Minor**: TimeSelectionSheet をホスト破棄に追随させる (他シートへ広げるかはオーナー判断)
5. **Minor**: `ui/verification/` を最終周の集合に整理し、brief.md の索引と一致させる
6. **Suggestion** (任意): SavedState の分岐一本化 / `TimeWheelSeries` の削除 / `minDate > maxDate` 縮退の deviation 記録
