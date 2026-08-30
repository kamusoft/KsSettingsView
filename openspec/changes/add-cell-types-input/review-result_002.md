# レビュー結果 - add-cell-types-input (Android 側実装)

**レビュー日時**: 2026年06月15日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-cell-types-input
**レビュー対象**: Android 側実装（`android/ks-settingsview-ui/`, `android/ks-settingsview-compose/`, `samples/android/`）の Phase 8〜14, 16
**対象外**: iOS（`review-result_001.md` で APPROVED 済）/ Phase 15 docs（ユーザー指示によりスキップ）

---

## サマリー

Android 側実装は spec の MUST 要件をすべて満たしている。`cell-types-basic` の共通規約への opt-in、Native 型直接公開方針、TwoWay binding 規約、`PickerCell` の単一/複数選択両モード、`DatePickerCell` の Material/Spinner UI 切替、`registerInputCells(context)` API いずれも仕様通り。実装は既存基本 Cell の Kotlin パターン（`CellViewHolder` 継承、`buildCellBaseViews` + `applyCellBaseLayout` 経路、`reset()` でのリスナー解除、`accessoryHolder.addView(...)` での programmatic 配置）と一貫している。

- Android テスト全件成功（debug 511 件、入力 Cell 関連 48 件 = `InputCellsTest` 36 件 + `InputCellDslTest` 12 件、失敗 0、エラー 0、スキップ 0）
- Sample debug APK ビルド成功
- iOS 側 review-result_001.md と同じく、Native 型直接公開方針（`android.text.InputType: Int` / `java.time.LocalTime` / `java.time.LocalDate`）は Decision 1 と完全に整合
- 共通行レイアウト関数 `applyCellBaseLayout(views, ...)` 経由でのレイアウト構成が 5 種すべてで遵守されている
- `VisibilityAware` opt-in、`Hashable`/`equals` 経路での `isVisible` 保持、TwoWay 経由の callback ループ防止（`TextWatcher` 解除 → setText → 再設定）も Kotlin idiom として適切
- Theme 要件（`Theme.Material3.*` 前提）は `InputCellsTest` が `ContextThemeWrapper(..., Theme_Material3_Light_NoActionBar)` で MaterialSwitch 要件相当を担保

Critical / Major 指摘は **なし**。Minor / Suggestion レベルの改善余地のみ。

**判定**: ✅ `APPROVED`

---

## 指摘事項

### 🟡 Minor

#### 🟡 [Minor] EntryCell タップで `requestFocus()` のみ呼び、IME を `InputMethodManager.showSoftInput(...)` で明示的に表示していない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:151-152`

**問題点**:

```kotlin
views.root.setOnClickListener {
    editText.requestFocus()
}
```

iOS では `UITextField.becomeFirstResponder()` がフォーカス取得とキーボード表示を同時にトリガするのに対し、Android では `EditText.requestFocus()` だけではソフトキーボードは自動的に出ない場合がある（特にフォーカスが他 view から移動した場合）。spec の iOS 側 Scenario 「Cell タップ → UITextField フォーカス → キーボード表示」を Android でも UX 等価にするには、`requestFocus()` 後に以下を追加するのが慣例：

```kotlin
val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
```

ただし spec の Android 側 MUST は「EditText.isEnabled が反映される / textColor / inputType が反映される」までで、Cell タップ起点のキーボード表示までは MUST 化されていない（iOS のみ MUST）。AiForms 互換編集体験についても spec 上は iOS 限定。

**推奨修正**:

ソフトキーボードの自動表示を期待するユーザーがいることを踏まえ、後続セッションで `InputMethodManager.showSoftInput` 呼び出しを追加する。ただし MUST 違反ではないため、本提案ではこのままマージ可。

---

#### 🟡 [Minor] EntryCellViewHolder.reset() で `tintColor` / `textColor` / `gravity` / `highlightColor` が残留する

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:162-175`

**問題点**:

`reset()` で `text` / `hint` / `filters` / `OnClickListener` / `isClickable` をクリアしているが、以下は前回 bind 時の値が残る：

- `editText.setTextColor(...)` で設定した色
- `editText.gravity`
- `editText.highlightColor`
- API 29+ で `textCursorDrawable` に設定した tint
- `editText.inputType`（次回 bind で必ず上書きされるので実害なし）

リサイクル時は同じ viewType（= 同じ Cell 型）の bind 直前に reset が呼ばれ、続く `bind(...)` で必ず全属性が上書きされる設計のため実害は **軽微**。ただし将来 `prepareForReuse` パターンに揃える観点では完全クリアが望ましい。iOS review_001 の同種指摘と同じ性質。

**推奨修正**:

将来的に以下を `reset()` に追加：

```kotlin
editText.gravity = Gravity.END or Gravity.CENTER_VERTICAL
editText.setTextColor(views.root.context.getDefaultTextColor())
editText.highlightColor = 0
```

優先度は低い。後続セッションで対応可。

---

#### 🟡 [Minor] PickerCellViewHolder.reset() で `disclosureView.visibility` が `VISIBLE` のまま

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt:126-136`

**問題点**:

`reset()` で `views.titleView.text = null` 等は実施しているが、`disclosureView.visibility` は前回 `VISIBLE` のまま。次回 `bind(...)` で必ず `View.VISIBLE` に再設定されるため実害は軽微。NumberPicker / TimePicker / DatePicker の各 ViewHolder も同様（5 種すべて）。

**推奨修正**: 後続セッションで `reset()` 内に `disclosureView.visibility = View.GONE` を追加して bind 直前のフラットな初期状態を整えるとよい。優先度低。

---

#### 🟡 [Minor] PickerCell.autoValueText() を毎 bind で算出していて、`DateTimeFormatter.ofPattern(format)` も毎 bind 新規構築

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt:33`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:31, 112`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:38, 183`

**問題点**:

iOS 側 review_001 と同種の指摘。`DateTimeFormatter.ofPattern(format)` は内部で正規表現パース等のコストがあり、ViewHolder の bind ごとに新規構築すると微小なオーバーヘッドが累積する。`format` 文字列は同一 Cell では基本的に変化しないため、ViewHolder インスタンスで lazy / 同一 format チェック付きキャッシュにできる。

**推奨修正**: `TimePickerCellViewHolder` / `DatePickerCellViewHolder` で `formatter: DateTimeFormatter?` と `lastFormat: String?` を保持し、format が変わったときのみ再構築する。優先度低。

---

#### 🟡 [Minor] `android.widget.DatePicker.calendarViewShown` deprecation 警告

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:125`

**問題点**:

`android.widget.DatePicker.calendarViewShown = false` は API 26 で `setCalendarViewShown(boolean)` とともに deprecate されている。Material スタイルでは無効化されない場合があり、警告が出る。

ただし、これは **spec の MUST 要件**（`DatePickerCell` の `androidUiStyle = Spinner` のとき「`android.widget.DatePicker` の Spinner モードを使用」）の実現手段として正当な選択である。AiForms 互換の旧 `IsAndroidSpinnerStyle = true` UX を実現するため、現状この API 以外の選択肢はない（`android:datePickerMode="spinner"` を XML で指定するのも同じ legacy API）。

**推奨修正**:

ファイル先頭または該当行に `@file:Suppress("DEPRECATION")` または `@Suppress("DEPRECATION")` を付与して意図的な利用であることを明示する。将来 Google が `MaterialSpinnerDatePicker` を出した場合の差し替え方針もコメントで残しておくとよい。

```kotlin
@Suppress("DEPRECATION")
val picker = DatePicker(ctx).apply {
    calendarViewShown = false
}
```

---

#### 🟡 [Minor] `RecyclerView.ViewHolder.adapterPosition` deprecation 警告

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:78`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:105`

**問題点**:

`picker.show(fm, "TimePickerCell.$adapterPosition")` の `adapterPosition` は API 23 で deprecate され、`bindingAdapterPosition` / `absoluteAdapterPosition` のどちらか（ConcatAdapter 配下なので `bindingAdapterPosition` が適切）が推奨。

**推奨修正**:

```kotlin
picker.show(fm, "TimePickerCell.${bindingAdapterPosition}")
```

または、Fragment tag は単にユニークである必要があるだけなので `cell.id` を使うほうが堅牢（Fragment tag が ConcatAdapter 内のグローバル position に依存しないため）：

```kotlin
picker.show(fm, "TimePickerCell.${cell.id}")
```

実害は軽微。優先度低。

---

#### 🟡 [Minor] `NumberPickerCellViewHolder` で `min > max` または `candidates` が空のときに `return` するため、利用者の設定ミスが UI 上「タップしても何も起きない」状態になる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:74`

**問題点**:

```kotlin
if (candidates.isEmpty()) return
```

`min > max` の設定ミスは利用者側のバグだが、ダイアログが開かないだけでフィードバックがない。`Log.w(LOG_TAG, "min > max")` 程度の警告ログがあるとデバッグしやすい。

**推奨修正**: ログ追加。優先度低。

---

#### 🟡 [Minor] `EntryCellViewHolder.reset()` 後の `accessoryHolder` 幅問題（実装者懸念点 3 への所見）

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:201-205`

**問題点**:

実装者が挙げた懸念事項「EntryCell の accessoryHolder 幅問題（minWidth=160dp で対応、iOS の `_FieldWrapper` と等価か）」について：

- spec の iOS 用 MUST 要件「`_FieldWrapper` 方式」「`contentHuggingPriority` 100」は **iOS の `KsListCellBase` 階層に対する記述** であり、Android には適用されない（spec 393 行目「iOS UITextField は title 残り領域全幅を占有する」）
- Android 側 spec MUST は「`EditText` を accessory に配置」「`inputType` 反映」「`isEnabled` 反映」までで、Android のレイアウト構造に関する MUST は規定されていない
- 現状の `minWidth = 160dp` 対応は accessory 幅を確保するための妥当な実装。`MATCH_PARENT` 配置と組み合わせて accessoryHolder の幅追従もする

ただし `accessoryHolder` は WRAP_CONTENT で、`minWidth = 160dp` の物理量決め打ちは小画面端末（Galaxy Fold 内側ディスプレイ等）で title 幅を圧迫する可能性がある。

**推奨修正**:

将来的には `LayoutParams.weight = 1f` 経由で `accessoryHolder` が title 列右側の残り全幅を吸う設計のほうが iOS の `_FieldWrapper`（contentHugging 100）と等価な振る舞いになる。本提案では MUST 違反ではないので任意対応。

---

### 🔵 Suggestion

#### 🔵 [Suggestion] PickerCellViewHolder の Multiple AlertDialog 内 `maxSelectedNumber` チェックが UI スレッドで `setItemChecked` を呼ぶフロー

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt:92-106`

**問題点**:

`setMultiChoiceItems` の listener 内で「上限到達時は `listView.setItemChecked(which, false)` で UI を元に戻す」フローは、user が checkbox をタップ → listener が呼ばれて isChecked = true → 直後に setItemChecked(false) を呼ぶ流れになる。一瞬の "ちらつき"（チェックが入ってから即座に外れる視覚的アーティファクト）が観察される可能性がある（実装者懸念点 4 として記載）。

iOS では `tableView.deselectRow` で同様の処理を行うが、`UITableViewCell.accessoryType = .checkmark` の場合は `tableView(_:didSelectRowAt:)` の戻りで `.checkmark` を付けないことでチラつきを避けられる。Android `setMultiChoiceItems` は内部で `ArrayAdapter` の `isChecked` を更新するため、後追いで打ち消すしかない。

**推奨修正**:

`AlertDialog.Builder().setMultiChoiceItems` ではなく `setView(...)` でカスタム `ListView`（`adapter = ArrayAdapter<...>` を独自に提供）を使い、「上限到達時は CheckBox の onCheckedChanged を listener なしで toggle」する形にすれば ちらつき回避可能。ただし実装コストが大きい上、ちらつきは spec 上明文化された MUST ではない。後続セッションで UX 検証後に対応可。

`HapticFeedbackConstants.REJECT` フォールバックとして `HapticFeedbackConstants.KEYBOARD_TAP` を試す実装は妥当な配慮。

---

#### 🔵 [Suggestion] `DatePickerCell` 既定 date が `LocalDate.of(1970, 1, 1)` で旧 AiForms 既定（`DateTime.Today`）と異なる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCell.kt:36`

**問題点**:

旧 AiForms `DatePickerCell.Date` の defaultValue は `DateTime.Today` (= 現在日付)。本実装は `LocalDate.of(1970, 1, 1)` を既定値にしている。これは `LocalDate.now()` を data class default にすると同一 Cell でも生成タイミングで日付が変わって `equals` が壊れる可能性を回避した妥当な選択。

iOS 側の `DatePickerCell.date` 既定値もチェックすると、`Date()` を default にしているはず（iOS review_001 にも同種の懸念は出ていない）。**iOS と Android で既定値が一致しない**点は migration ドキュメントで明示が望ましい。

**推奨修正**:

Phase 15（docs）でユーザー指示によりスキップとのことなので、docs 担当の後続セッションで明記する。

design.md Decision 8 の表では「DatePickerCell.date 既定値: iOS `Date()` / Android `LocalDate`」と書かれているが、Android の具体的既定値（`LocalDate.of(1970, 1, 1)`）は明記されていない。docs フェーズで追記する。

---

#### 🔵 [Suggestion] `DatePickerCellViewHolder.showMaterialDatePicker` で `setStart` / `setEnd` の二重設定

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:87-90`

**問題点**:

`CalendarConstraints.Builder().setValidator(...)` と `setStart(...)` / `setEnd(...)` を併用している。`MaterialDatePicker` の `CalendarConstraints` は `setStart` / `setEnd` が "表示可能な月の範囲"、`DateValidator` が "選択可能な日の検証" の二重制限になっており、`setStart` 未指定なら 1970/1/1、`setEnd` 未指定なら 5000 年程度（Material のデフォルト）が使われる。両方とも `minDate.toEpochMilliUtc()` / `maxDate.toEpochMilliUtc()` で揃えるのは妥当だが、コメントに意図を残しておくと将来の保守者が混乱しない。

**推奨修正**:

```kotlin
// CalendarConstraints.setStart/setEnd は "表示可能な月" の範囲、DateValidator は "選択可能な日"
// の検証。MaterialDatePicker 内部で両方とも見るため、minDate/maxDate 指定時は両方に同じ値を
// 渡してダブル抑制する。
cell.minDate?.let { constraintsBuilder.setStart(it.toEpochMilliUtc()) }
cell.maxDate?.let { constraintsBuilder.setEnd(it.toEpochMilliUtc()) }
```

優先度低。

---

#### 🔵 [Suggestion] `DSL 経路で auto-register` ガード式の冗長性

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:179-181`

**問題点**:

```kotlin
if (!KsCellRegistry.isRegistered(EntryCell::class)) {
    KsCellRegistry.registerInputCells(context)
}
```

`EntryCell` が代表値として使われており、コメントで「テストや利用者が事前に異なる factory を登録するケースに対応」と説明されている。これは仕様 MUST「オプトアウト可能な auto-register」を満たす実装で適切。

`registerInputCells(context)` の冒頭でも各 `register(cellClass, ...)` 呼び出しに二重登録ガードがあるとさらに安全だが、`KsCellRegistry.register(...)` 側の実装が既に同 viewType 上書きを許容する設計なら不要。

**推奨修正**: なし。現状で OK。

---

#### 🔵 [Suggestion] `MainActivity` の `KsCellRegistry.strictMode = BuildConfig.DEBUG` 設定が `setContent` の前にある

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:75-78`

**問題点**:

サンプル側は `KsCellRegistry.strictMode = BuildConfig.DEBUG` を `onCreate` 内で `setContent` 前に設定。これにより `InputCellsDemoScreen` が起動するとき、登録漏れがあれば `KsSettingsView.applyDiff` の `reportMissingId` が `error()` でクラッシュする。本提案で追加された入力 Cell 5 種は `KsSettingsView.init` の auto-register で登録済みのため、デモ画面でクラッシュは発生しない。設計として正しい。

**推奨修正**: なし。

---

## アクションプラン

優先度順：

1. **（任意・実機検証で要観察）EntryCell タップ起点のソフトキーボード自動表示** — `InputMethodManager.showSoftInput(...)` を `requestFocus()` 直後に呼ぶ。iOS と UX を揃えるための追加対応
2. **（任意）Spinner DatePicker と `adapterPosition` の `@Suppress("DEPRECATION")` 付与** — 警告抑制とコード意図の明示
3. **（任意）`TimePickerCellViewHolder` / `DatePickerCellViewHolder` の `DateTimeFormatter` キャッシュ化** — 性能改善（軽微）
4. **（任意）`reset()` の完全クリア（`disclosureView` 視認性、tintColor / textColor / gravity 等）** — 将来 prepareForReuse パターン化を見据えた整備
5. **（任意・docs フェーズ）`DatePickerCell` Android 既定 date `LocalDate.of(1970, 1, 1)` の明記** — iOS との差分を docs に追記
6. **（任意・UX 検証後）PickerCell Multiple の `setItemChecked(false)` ちらつき検証** — Sample で実機検証して必要なら独自 ListView 化

いずれも本提案の MUST 要件には影響しないため、修正なしでマージ可。

---

## 判定結果

**ステータス**: ✅ **APPROVED**

### 判定理由

- **spec MUST 要件をすべて充足**:
  - EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell の 5 種すべてが `Cell` / `DSLReidentifiableCell` / `DSLStyleModifiableCell` / `DSLIconModifiableCell` / `VisibilityAware` に準拠
  - 共通 Optional フィールド（`description` / `valueText` / `icon` / `hintText` / `isEnabled` / `isVisible`）の保持。EntryCell は `valueText` を持たない（MUST NOT 遵守）
  - 共通行レイアウト関数 `applyCellBaseLayout(views, ...)` 経由での描画（5 種すべて確認）
  - `isEnabled = false` のときの色置換・タップ無効化
  - `VisibilityAware` opt-in（`(cell as? VisibilityAware)?.isVisible` で取得可能）
  - `Theme.cellTitleColor` / `cellTitleFont` の 3 段階解決（`EffectiveStyle.from` 経由）
  - `accentColor` の 4 段階解決（`cell.accentColor` → `effective.accentColor` フォールバック）
  - **Native 型直接公開**：`keyboardType: Int`（`android.text.InputType` 定数）/ `time: LocalTime` / `date: LocalDate`。独自列挙型・独自値型は導入していない（Decision 1 完全準拠）
  - TwoWay binding（`MutableState<T>`）と callback 経路の併設。DSL では `onTextChanged = { newValue -> text.value = newValue }` の橋渡し
  - PickerCell の単一/複数選択両モード（`selectionMode: PickerSelectionMode` + `selectedIndex` / `selectedIndices`）、`maxSelectedNumber` 上限制御（上限到達時は新規チェック無視 + `HapticFeedbackConstants.REJECT`）
  - PickerCell の自動 valueText 表示（単一: `items[selectedIndex]` + `displayFormatter` / 複数: `, ` 連結 / 明示指定が優先）
  - DatePickerCell の Material / Spinner UI 切替（`androidUiStyle: DatePickerAndroidStyle`）。Spinner では `androidButtonColor` を Positive/Negative に反映
  - DatePickerCell の minDate/maxDate（Material は `CalendarConstraints` + `DateValidatorPointForward/Backward`、Spinner は `DatePicker.minDate/maxDate`、ZoneOffset.UTC vs ZoneId.systemDefault の使い分けも適切）
  - id デフォルト値 `"<className>-${UUID}"` 形式（`entry-cell-`, `picker-cell-`, `number-picker-cell-`, `time-picker-cell-`, `date-picker-cell-`）。連続生成で衝突しないことをテストで担保
  - `KsCellRegistry.registerInputCells(context)` API（5 種登録、viewType 110-114）
  - `KsSettingsView.init` での auto-register（`!KsCellRegistry.isRegistered(EntryCell::class)` ガードでオプトアウト可能）
  - Compose DSL 拡張関数 5 種（`@SettingsRootDsl` 付与、`DSLSectionScope` 拡張、`CellHandle` 返却）。`PickerCell` の単一/複数 overload は `selectedIndex` / `selectedIndices` 引数で解決
  - `androidx.fragment:fragment-ktx:1.8.4` の依存追加（MaterialDatePicker/MaterialTimePicker 用）
  - `PickerSelectionMode { Single, Multiple }` / `DatePickerAndroidStyle { Material, Spinner }` 列挙型の存在と各 2 ケース

- **テスト網羅性**:
  - `InputCellsTest` 36 件すべて成功（id デフォルト値規約 / VisibilityAware opt-in / EntryCell の Native Int keyboardType / isPassword の TYPE_TEXT_VARIATION_PASSWORD OR 合成 / maxLength の LengthFilter 設定 / TextWatcher 経由 onTextChanged / reset 後の再利用時旧 callback 非発火 / isEnabled での EditText disable / textAlignment END / PickerCell single 自動 valueText / displayFormatter / multi カンマ連結 / valueText 明示指定優先 / AlertDialog 表示確認 / NumberPickerCell 既定値 / 自動 valueText / TimePickerCell 既定 format / formatTime ヘルパ / valueText format 適用 / DatePickerCell 既定 format `yyyy/MM/dd` / 既定 androidUiStyle Material / formatDate ヘルパ / valueText format 適用 / minDate/maxDate equals / 共通フィールド全反映 / isVisible 値保持 / registerInputCells 5 種登録 / 補助型ケース存在）
  - `InputCellDslTest` 12 件すべて成功（TwoWay binding が MutableState を更新、Native InputType / maxLength の data class 反映、callback overload、PickerCell single/multi overload 解決、NumberPicker TwoWay、TimePicker LocalTime TwoWay、DatePicker LocalDate TwoWay、Spinner UI スタイル反映、`CellHandle.cellHeight` chain、5 種一括配置）
  - 全 Android モジュールテスト debug 511 件 / release も同様、失敗なし
  - Sample debug APK ビルド成功

- **設計判断の妥当性**:
  - `import jp.kamusoft.kssettingsview.ui.EntryCell as UiEntryCell` での DSL シャドウ回避は Kotlin 慣用句として適切
  - PickerCell を 1 つの data class で `selectedIndex` / `selectedIndices` 両方を保持し、Compose DSL overload で切り替える設計は iOS の precondition crash 方式より安全（iOS review_001 の Minor 指摘との対比でも一貫性が取れる）
  - MaterialDatePicker (UTC) と `android.widget.DatePicker` (system default zone) の epoch ms 解釈の使い分けが正しい（`toEpochMilliUtc()` ヘルパで UTC を明示）
  - `KsCellRegistryInputCells.kt` の `@Suppress("UNUSED_PARAMETER")` で `context` を将来の Cell が必要とする際に互換性確保する設計は妥当
  - `auto-register` ガードに `EntryCell::class` を使う設計は基本 Cell の `LabelCell::class` ガードと並列で一貫
  - `findFragmentManager()` extension で `ContextWrapper` を unwrap して `FragmentActivity` を見つけるアプローチは Compose `AndroidView` 経由でも動作する堅実な実装
  - ユーザー記憶ルール（Theme.Material3.* 前提、ItemDecoration は onDrawOver、複数 Cell 内容更新はバッチ化）は本提案で新規追加された範囲では関係箇所が少ないが、`InputCellsTest` の `ContextThemeWrapper(..., Theme_Material3_Light_NoActionBar)` で Material3 テーマ前提を担保している

- **Critical / Major 指摘なし**。Minor / Suggestion レベルの改善余地（IME 自動表示、reset の完全クリア、DateTimeFormatter キャッシュ、deprecation 警告抑制、Spinner の `setStart`/`setEnd` 二重設定コメント、PickerCell multi のちらつき検証）はあるが、いずれも本提案の MUST 要件に影響せず、後続でも対処可能。

Android 側実装はマージ可能。iOS 版（review-result_001.md で APPROVED 済）と合わせて、本提案は次のフェーズ（sdd-validator 検証 / archive）へ進行できる。
