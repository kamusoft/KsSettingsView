## 参考実装

本変更提案の実装に着手する**前に必ず**以下を確認すること。Cell ごとのプロパティ・デフォルト値・BindingMode が後続変更提案 (`add-maui-cells`) で再現されるため、ここでズレると累積する。

- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元の仕様要約
  - **必読セクション**: §2（CellBase 共通プロパティ 22 個）、§3（基本 7 種の Cell 固有プロパティ表）、§5（Handler / PropertyMapper パターン）、§11（旧版との差分）
- 原典コード（Cell ごとに必ず参照）：
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/LabelCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/LabelCell.cs)
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/CommandCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/CommandCell.cs)
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/ButtonCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/ButtonCell.cs)
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/SwitchCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/SwitchCell.cs) — `On` は **TwoWay**
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/CheckboxCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/CheckboxCell.cs) — `Checked` は **TwoWay**
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/RadioCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/RadioCell.cs) — Group 添付プロパティが **TwoWay**
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/SimpleCheckCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/SimpleCheckCell.cs) — `Checked` は **OneWay**（Switch/Checkbox と異なる！）
  - [`../AiForms.Maui.SettingsView/SettingsView/Handlers/`](file://../AiForms.Maui.SettingsView/SettingsView/Handlers/) 配下の対応 Handler — 各 PropertyMapper の動作

**重要な差異の例**: SimpleCheckCell の `Checked` は OneWay、SwitchCell の `On` と CheckboxCell の `Checked` は TwoWay。Native 側で `onValueChanged` を発火する有無が異なるため、同じ「チェック系」でも実装パターンが分かれる。本仕様要約 §3 の表で BindingMode を必ず確認のこと。

## Context

旧 `AiForms.Maui.SettingsView` には 15 種の Cell があり、ユーザーは XAML で自由に組み合わせて利用していた。本変更提案では、利用頻度の高い基本 Cell 群（読み取り・タップ・トグル・選択系）を iOS / Android Native の両方で同時に実装する。Cell ごとの仕様（`../AiForms.Maui.SettingsView/SettingsView/Cells/*.cs`）を参考にしつつ、Native 慣習に合わせた API 設計を行う。

## Goals / Non-Goals

**Goals:**
- 7 種の基本 Cell を iOS / Android で実装：LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell
- 各 Cell の `KsCellRegistry` 一括登録 API
- ユーザー操作通知（`onValueChanged`、`onTap`、`onSelected`）のクロージャ／コールバック方式
- 各 Cell の単体テストと Sample 表示

**Non-Goals:**
- 入力系（Entry/Picker/NumberPicker/TimePicker/DatePicker/TextPicker）は `add-cell-types-input` で対応
- カスタムセル（任意 SwiftUI/Compose 埋め込み）は `add-cell-types-custom` で対応
- MAUI 側の `BindableObject` Cell および Handler、`samples/maui/` への 7 種基本 Cell ページ追加は `add-maui-cells` で対応
- ドラッグ＆ドロップ並べ替えは Phase 6 に延期

## Decisions

### Decision 1: ユーザー操作通知はクロージャ／ラムダ

**選択**: SwitchCell / CommandCell 等のユーザー操作は、Cell インスタンスにクロージャ（Swift）／関数型（Kotlin）プロパティ（`onValueChanged`、`onTap`、`onSelected`）として持たせる。

**理由**:
- Native 利用時に直感的、API が一貫
- Cell が値型（struct / data class）のため、クロージャが含まれても等価性は参照不変な前提で運用可（同じ `id` であれば同じ Cell として扱う）
- MAUI バインディング層では Bridge が delegate/listener interface 経由で受け取り、C# Handler に転送する

**代替案**:
- delegate / listener interface に集約：Cell 個別に渡せず、Section ごとに大量の case 分岐が必要になる。

### Decision 2: Cell の equals/hashCode はクロージャを除外

**選択**: Swift `Hashable` の手動実装、Kotlin `data class` のフィールドリストからクロージャを除く。

**理由**:
- クロージャ／関数型は Hashable / equals に含めると差分検出が壊れる（毎回新規クロージャが生成される）
- DiffUtil / DiffableDataSource は ID + 値フィールドで判定すれば十分

**代替案**:
- すべてのフィールドを equals/hashCode に含む：差分検出が常に「変更あり」と判定され、再レンダリングが暴発する。

### Decision 3: RadioCell の selectedValue は Cell 自身が持つ

**選択**: `RadioCell` は `groupId` と自身の `value` に加え、`selectedValue` プロパティも持つ。利用者は同じ groupId の全 RadioCell に同じ selectedValue を設定する。

**理由**:
- Cell が値型のためグループの状態を Section 全体で持たせるとモデルが複雑化する
- 利用者は SettingsRoot 構築時に状態管理するだけで良い

**代替案**:
- `RadioGroup` 専用の Section 型：API が膨らむ。Native 慣習（iOS の UITableView だと自由度を保つ）に合わない。

### Decision 4: 基本 Cell をまとめて登録する API

**選択**: `KsCellRegistry.registerBasicCells()`（iOS）、`KsCellRegistry.registerBasicCells(context)`（Android）を提供。

**理由**:
- `KsSettingsViewController` / `KsSettingsView` の init で自動呼び出しする
- 利用者は Cell 種類を増やしたいときに自動登録に頼らず明示的に他の registerXxxCells を呼べる

### Decision 5: PoC Cell 削除のタイミング

**選択**: 本変更提案の最終タスクで `PoCLabelCell` / `PocLabelCell` を削除する。

**理由**:
- LabelCell が PoC Cell の上位互換であるため、PoC Cell を残す意味がない
- 削除タスクを忘れずに実行するため tasks.md の最終ステップに含める

### Decision 6: Sample 専用 SampleLabelCell の取扱い

**選択**: `LabelCell` 公開と同時に、Sample アプリ側で過去に独自定義していた `SampleLabelCell` / `SampleLabelCellView`（iOS）/ `SampleLabelCellViewHolder` / `SampleLabelCellDsl`（Android）/ `SampleLabelCellPreview`（iOS）を **すべて削除し、本体 `LabelCell` で置換する**。Sample 専用 Cell の Renderer / ViewHolder 登録コードも併せて削除し、`KsSettingsViewController.init`（iOS）/ `KsSettingsView.<init>`（Android Compose）の `registerBasicCells` 自動呼び出しに任せる。

**理由**:
- `SampleLabelCell` は元々「`internal` の PoC Cell (`PocLabelCell` / `PoCLabelCell`) を Sample から直接参照できない」という当時の制約を回避するためだけに導入された
- `add-cell-types-basic` で `LabelCell` が `public` 公開された時点でその存在意義は消滅し、`add-cell-types-basic/specs/settings-view-{ios,android}-ui/spec.md` の PoC Cell REMOVED Migration 節でも「テスト・サンプルは `LabelCell` に置き換える」と明記されている
- Sample に温存すると、Sample 側の `KsCellRegistry.register(SampleLabelCell::class, viewType = CELL_VIEW_TYPE_MIN, ...)`（Android）と `registerBasicCells` が登録する `LabelCell` が **同じ viewType (100) を異なる Cell 型に重複割当**するため、`KsCellRegistry.register` の衝突検証で `IllegalArgumentException` が発生して起動時クラッシュする（iOS も Cell 型登録の重複は理屈上同等）
- 「viewType を Sample 側で 1000 等にずらす」回避は症状の隠蔽にすぎず Migration 方針に反する。`KsCellRegistry` を namespace 分離設計に拡張する案は本体 API 変更コストが過剰

**代替案（却下）**:
- 案 A: Sample 側 viewType を 1000 等にずらす → Migration 方針違反、`SampleLabelCell` 温存の正当性なし
- 案 B: `KsCellRegistry` に namespace を追加して同一 viewType を許容 → 本体 API 変更が過剰、Sample のためだけのコア設計変更は妥当性低い

### Decision 7: Sample アプリの `android:theme` を `Theme.AppCompat.*` 派生に変更

**選択**: Android Sample アプリの `AndroidManifest.xml` の `android:theme` を `@android:style/Theme.Material.Light.NoActionBar`（フレームワーク標準 Material）から `@style/Theme.AppCompat.Light.NoActionBar` に変更する。あわせて `docs/android-ui.md` に「テーマ要件」セクションを追加し、本体 `ks-settingsview-ui` の利用者アプリは AppCompat 派生テーマを使用する必要があることを明文化する。

**理由**:
- 本体 `ks-settingsview-ui` は `androidx.appcompat:appcompat` に依存し、`SwitchCellViewHolder.create()` で `SwitchCompat` を、`LabelCellViewHolder` 等で `AppCompatImageView` を内部で生成する
- これら AppCompat ウィジェットは `Theme.AppCompat.*` 派生（または `Theme.MaterialComponents.*` / `Theme.Material3.*`）テーマでのみ正しく初期化される
- フレームワーク標準の `@android:style/Theme.Material.*` では `SwitchCompat` のスタイル属性 (`switchTextAppearance` / `textOn` / `textOff` 等) が解決されず、`SwitchCompat.onMeasure` → `makeLayout` で `textOn`/`textOff` が `null` のまま `StaticLayout(charSequence, ...)` に渡され、`CharSequence.length()` の NPE が発生する
- 初版 §18.5 完了直後の動作確認で「基本 Cell 7 種デモ」遷移時に上記 NPE クラッシュが顕在化（初版 Sample が `add-samples-android` 時点の `Theme.Material.Light.NoActionBar` を温存していたことが原因）

**代替案（却下）**:
- 案 A: 本体 `ks-settingsview-ui` 側で `SwitchCompat` を素の `android.widget.Switch` に置換 → Material3 デザインへの追従性が落ちる、アクセシビリティ低下、本体 API への影響が大きい
- 案 B: `SwitchCellViewHolder.create()` で `setShowText(false)` 等を呼んで防御的に NPE を回避 → 根本解決でない、テーマ要件が暗黙のままで他の AppCompat ウィジェット（`AppCompatImageView` 等）の警告は残る、`AppCompat` 系テーマでのみ提供される `colorAccent` 等の整合も取れない

**追補 (Decision 8 で改訂)**: Theme.AppCompat に変更したところ、罫線消失（`onDraw` バグ）と Switch/Radio の貧弱描画問題が顕在化し、最終的に `Theme.Material3.*` 必須化 + Material/AppCompat ウィジェット採用に移行した。Decision 8 を参照。

### Decision 8: Theme.Material3.* 必須化と Material/AppCompat ウィジェット採用

**選択**: 以下 3 点をまとめて実施する。

1. **`Theme.AppCompat.Light.NoActionBar`（Decision 7） → `Theme.Material3.DayNight.NoActionBar` へ移行**: Sample の `AndroidManifest.xml` の `android:theme` をさらに Material3 系に変更する。`docs/android-ui.md` のテーマ要件記述も Material3 必須に強化する。
2. **本体 ViewHolder のウィジェット置換**:
   - `SwitchCellViewHolder`: `SwitchCompat` → `com.google.android.material.materialswitch.MaterialSwitch`
   - `CheckboxCellViewHolder`: `TextView "✓"` → `androidx.appcompat.widget.AppCompatCheckBox`
   - `RadioCellViewHolder`: `TextView "●"` → `androidx.appcompat.widget.AppCompatRadioButton`
3. **罫線描画の `onDraw` → `onDrawOver` 変更**: `ClassicSectionDecoration` で Cell 背景の `setBackgroundColor` に罫線が上書きされて見えなくなる回帰を修正する。

**理由**:
- Decision 7 完了後の Pixel 6a 実機確認で次の問題が発覚した:
  - **罫線消失**: `LabelCellViewHolder` 等が `container.setBackgroundColor(effective.backgroundColor)` で Cell 全面を白で塗るため、`ItemDecoration.onDraw`（children 描画前に呼ばれる）で描いた灰色罫線が Cell 背景に上書きされて見えなくなっていた。`SampleLabelCellViewHolder` は背景を塗っていなかったため見えていただけ。修正: `onDraw` を `onDrawOver`（children 描画後に呼ばれる）に変更する。
  - **Switch/Radio の貧弱描画**: 旧 `RadioCellViewHolder` は `TextView` に文字列 `"●"` を表示するだけの手抜き実装、`CheckboxCellViewHolder` は `TextView "✓"` の同様実装で Material Design 系の見栄えに到達していなかった。`SwitchCompat` も `Theme.AppCompat.*` では素朴な Material1 風になる。AiForms 旧実装も `AppCompatCheckBox` / `SimpleCheck` を使うのが本筋。
  - **`MaterialSwitch` の Theme 要件**: `MaterialSwitch` は `?attr/materialSwitchStyle` を要求するが、これは `Theme.Material3.*` 系でのみ定義されている。`Theme.MaterialComponents.*` / `Theme.AppCompat.*` ではトラック/サムが描画されない。さらに `SwitchCompat` 由来の `textOn/textOff` null NPE は `Theme.Material3.*` でも残るので `showText = false` / `textOn = ""` / `textOff = ""` を明示する。
- AiForms.Maui.SettingsView の Android 実装でも `SwitchCompat` / `AppCompatCheckBox` を使用しており、本提案の選択は前例と整合する。

**代替案（却下）**:
- 案 A: 罫線を `setBackgroundColor` の代わりに Cell コンテナの margin-top で表現 → レイアウト測定への影響大、`ItemDecoration` 設計から逸脱
- 案 B: `MaterialSwitch` の代わりに `MaterialComponents.*` 系の `SwitchMaterial`（旧名）を使う → 既に deprecated、Material Design 3 への追従性が落ちる
- 案 C: `MaterialRadioButton` / `MaterialCheckBox`（`com.google.android.material.*` パッケージ）を使う → `AppCompatRadioButton` / `AppCompatCheckBox` で十分 Material 3 風の見栄えが出る（テーマ由来）、依存方向が単純

**残課題（責務外）**:
- `SimpleCheckCellViewHolder` は依然として `TextView "✓"` で実装している。これは AiForms 側でも同等の独自 Cell であり、左端に小さなチェックを置くだけの最小用途のため、本提案の責務範囲では置換しない（必要なら別 issue で対応）。→ **Decision 9 で見直し**（オリジナル `SimpleCheck` のカスタム Canvas 描画に合わせる）。

### Decision 9: 実機レビュー（Pixel 6a, 2026-06-02）由来のオリジナル準拠化とちらつき修正

**背景**: Decision 8 完了後の Pixel 6a 実機レビューで、AiForms オリジナル UI との描画乖離と ON/OFF 操作時のちらつき（余分なセルリロード）が指摘された。本 Decision で 4 つの描画乖離をオリジナル準拠に揃え、ちらつきの根本原因を解消する。

**追補（オーナーレビュー review-result_002 対応）**: 9-3 / 9-4 が要求する accent 着色を実現するため、`ks-settingsview-core` の `Theme`（Android `Theme.kt` / iOS `Theme.swift` の両方）に `cellAccentColor: KsColor` フィールドを新設した（既定値は iOS tint / Material accent 相当の `#007AFF`）。これにより従来の「title 色流用」を廃し、選択系 Cell のチェック色を `CellStyle` accent 指定 → `Theme.cellAccentColor` の順で解決する。`settings-view-core` 仕様の Theme Requirement は必須フィールドを「最低限」として規定しており、フィールド追加は既存要件と矛盾しないため delta spec の更新は不要。iOS の `Theme` も同一フィールド・既定値で対称に保つ（iOS Cell View 側の着色リワイヤは Decision 9-1 の方針どおり別途 iOS 実機レビューのタスクで実施）。

#### 9-1. チェック系 Cell のトグル反映とちらつき回避 — オリジナル AiForms 準拠の TwoWay 方式

**方式**: ユーザー操作によるチェック/スイッチのトグルは、オリジナル AiForms.Maui.SettingsView の Android Cell View 群（`CheckboxCellView.cs` / `SwitchCellView.cs` / `RadioCellView.cs` / `SimpleCheckCellView.cs`）と同じ **TwoWay 方式** で行う。すなわち:

- **セルタップ → ViewHolder が自分の View 状態を直接トグル**する（オリジナルの `RowSelected` →
  `_checkbox.Checked = !_checkbox.Checked` / `_switch.Checked = !_switch.Checked` /
  RadioCell は `if (!_simpleCheck.Selected) SelectedValue = _radioCell.Value`）。
- **View の変化を `onValueChanged` / `onSelected` でモデルへ書き戻す**（オリジナルの `OnCheckedChanged` →
  `_CheckboxCell.Checked = isChecked` / `_SwitchCell.On = isChecked`）。

この方式では **`submitList` / `DiffUtil` を介してチェック状態を画面反映しない**。View 自身が現在状態の真実
（source of truth）を持ち、タップで即時にトグル表示される。したがって内部状態（`isOn`/`isChecked`/`selectedValue`）の
変化は diff 対象として行のフルリバインドを起こさず、**ちらつき（操作時に行全体がチカチカ再描画される現象）が
構造的に発生しない**。

各 ViewHolder の責務:
- **CheckboxCellViewHolder**: container クリックで `checkBox.toggle()`。`OnCheckedChangeListener` 経由で
  `onValueChanged(newValue)` を一度だけ発火（通知は listener 一本に集約し二重発火を防ぐ）。`bind` は `cell.isChecked` を初期表示に反映するのみ。
- **SwitchCellViewHolder**: container クリックで `switchView.toggle()`（Decision 9-6 のセル全体タップ）。通知は `OnCheckedChangeListener` 一本に集約。`bind` は `cell.isOn` を初期反映。
- **RadioCellViewHolder**: container クリックで、未選択なら自分を即 `checkView.isChecked = true` にし `onSelected(value)` 発火。同一グループの他セルの選択解除は、利用者が `selectedValue` を更新して再構築（再 bind）する経路で `value == selectedValue` 判定により反映する。
- **SimpleCheckCellViewHolder**: container クリックで `checkView` のチェックをトグルし `onValueChanged(newValue)` 発火。`bind` は `cell.isChecked` を初期反映。

**`equals`/`hashCode` の扱い（内部状態を diff 対象外とする — オリジナル AiForms 準拠／クロージャ除外と同じ思想）**:

TwoWay 方式（ViewHolder が View 状態の真実を持ち、タップで自分で直接トグルする）にした以上、オリジナル AiForms と同様に**内部状態は diff の比較対象から外す**。具体的に:

- **`SwitchCell` は `isOn` を、`CheckboxCell` / `SimpleCheckCell` は `isChecked` を `equals`/`hashCode` から除外**する（クロージャ `onValueChanged` が既に除外されているのと同じ思想＝Decision 2 の延長）。これにより `id`/`style`/`title`/`description`/`accentColor` のみで等価判定される。
- **`RadioCell` だけは `selectedValue` を `equals`/`hashCode` に残す**（後述の「RadioCell の別扱い」）。`id`/`style`/`title`/`groupId`/`value`/`selectedValue` で等価判定し、クロージャ `onSelected` のみ除外する。

`areContentsTheSame` は素直に `oldItem == newItem`（equals 委譲）のまま、`DSLDiffCalculator` も equals ベース（`oldCell != cell` での `ReplaceCell` 検出）のままでよい。`hasSameContentAs` や payload 機構は復活させない（撤去済みのまま）。内部状態を equals から抜くことで自然に解決する。

**この変更の理由（ちらつき復活の根本原因）**: 直前の「オリジナル準拠 TwoWay 方式への作り直し」で 4 Cell の `equals`/`hashCode` に内部状態を**戻してしまった**ため、ちらつきが復活した。経路は次のとおり: Sample が `onValueChanged` で state 更新 → 再 compose → `DSLDiffCalculator` が内部状態の差を検出して `ReplaceCell` 発行 → `submitList` → `KsSettingsListAdapter.areContentsTheSame`（`oldItem == newItem`）が `false` → **その行をフルリバインド** → ちらつき。内部状態を equals から除外すると `equals == true` となり、`ReplaceCell` も `areContentsTheSame == false` も発生しないため、`submitList` による再 bind（フルリバインド）が起きず**ちらつきが構造的に解消**する。タップによる即時反映は上記 ViewHolder の View 直接トグルが担うため `submitList`/diff には依存しない。

**トレードオフ**: Switch/Checkbox/SimpleCheck では「利用者が外部から内部状態だけを変えて `submitList` しても再描画されない」が、これはオリジナル AiForms と同じ挙動（submitList でチェックを反映しない・View が source of truth）であり、TwoWay 方式として正しい。

**RadioCell の別扱い（`selectedValue` は equals に残す — option (b)）**: RadioCell は「Light をタップ → 選択が Light に移る」際に、同一グループの他セル（例：Dark）のチェックを消す必要がある。`selectedValue` を equals から除外すると、選択が変わっても他セルの `areContentsTheSame` が `true` を返して再 bind されず、**古い選択（Dark）の ✓ が消えない（複数 ✓）**不具合になる。そのため RadioCell だけは `selectedValue` を equals に残し、選択変更時に当該グループの旧選択・新選択セルが最小限再 bind される経路を維持する。Switch/Checkbox/SimpleCheck は単一セルの自己トグルなので内部状態除外で問題ないが、RadioCell はグループ間で状態が連動するため別扱いとする。選択変更は頻繁でなく、変わったときだけ最小限再描画されるためちらつきは問題にならない。

**実機検証（Pixel 6a, serial=<android-device-serial>, 2026-06-02）スクショ画像差分**（`/tmp/ks_review_fix3/`）:
- **ちらつき解消（最重要）**: Switch ON/OFF の前後スクショ画像差分で、変化したピクセルは「スイッチウィジェット領域内（6787px）」＋「上部の最終タップ表示テキスト帯（2436px、これは仕様上の変化）」のみで、**それ以外（タイトル・背景・罫線・他セル）の変化は 0px**。Checkbox も同様に「チェックボックスウィジェット内 1247px」＋「テキスト帯 1974px」で**他は 0px**。SimpleCheck も「accessory チェック 328px」＋「テキスト帯 2407px」で**他 0px**。操作したセル自身のタイトル・背景もフルリバインドでチカチカしない。
- **(A) Checkbox**: タップでチェックがトグル表示（accessory に 1247px 出現）。
- **(B) RadioCell**: Light タップ前 = Light 0px / Dark 328px（✓ あり）、タップ後 = Light 328px（✓）/ Dark 0px / Auto 0px。**選択が Light に移り、Dark の ✓ が消え、複数 ✓ にならない**ことを確認。`selectedValue` を equals に残した効果。
- **(C) SimpleCheck**: タップ前 0px → タップ後 328px のチェックマーク描画でトグル表示。
- **(D) LabelCell ロングプレス**: 行全幅（x0-1079）が一様に約 -30 の RGB デルタで暗転 = RippleDrawable / 選択ハイライトが表示（Decision 9-5 維持）。

**撤去の経緯（旧 payload 機構の廃止）**: 当初は「内部状態を `equals` から除外 + `Cell.hasSameContentAs` 新設 + `CellChangePayload`/`getChangePayload`/`onBindViewHolder(payloads)` による部分 bind（`bindStateOnly`）」という payload 差分方式を実装した。Robolectric テスト上は動作したが、**Pixel 6a 実機検証で `getChangePayload` が期待どおり呼ばれず**チェックの変更が画面反映されない不具合が出た（DiffUtil の payload 経路は環境依存で挙動が安定しない）。加えて、オリジナル AiForms の素直な設計（View 自身がトグル、submitList に依存しない）から乖離していたため、オーナー判断でオリジナル準拠の TwoWay 方式に作り直した。これに伴い `Cell.hasSameContentAs` / `CellChangePayload` / `getChangePayload` / `onBindViewHolder(payloads)` / 各 ViewHolder の `bindStateOnly` / `DSLDiffCalculator.sectionsHaveSameContent` は**すべて撤去**した。

**iOS との対称性（本提案のスコープ外）**: iOS 側（SwiftUI / `UITableViewDiffableDataSource`）のちらつき・状態反映の最適化は**本提案では対象外**とし、別途 iOS 実機レビューのタスクで対応する。本 Decision 9 は Android 固有の挙動に関する判断のみをスコープとする。

**代替案（却下）**:
- 案 A（旧実装・撤去済み）: 内部状態を `equals` から除外 + `hasSameContentAs` + payload 部分 bind → 実機で `getChangePayload` が安定して呼ばれず反映不具合。オリジナル設計からの乖離も大きい。
- 案 B: `setHasStableIds` を切る／`notifyItemChanged` を手動制御 → ListAdapter + DiffUtil の設計から逸脱し、他 Cell の差分検出にも影響する。

#### 9-2. ナビゲーションインジケータ（右矢印）をオリジナル素材に準拠

**問題**: `CommandCellViewHolder` の Disclosure Indicator が `TextView ">"`（太字テキスト）で実装されており、オリジナルの `ic_navigate_next.xml`（18×26dp の VectorDrawable、`#FFCACACA` の chevron）とデザインが異なる。

**選択**: オリジナル `ic_navigate_next.xml` を `ks-settingsview-ui` モジュールの `res/drawable/` に移植（同等の VectorDrawable を追加）し、`CommandCellViewHolder` の `disclosureView` を `TextView` から `AppCompatImageView` に置換して当該 drawable を表示する。色 `#FFCACACA` はオリジナル準拠とし、`tint` でテーマ追従も検討する。

参照: `../AiForms.Maui.SettingsView/SettingsView/Platforms/Android/Resources/drawable/ic_navigate_next.xml`

#### 9-3. RadioCell のチェックをオリジナルのカスタム描画に準拠

**問題**: `RadioCellViewHolder` は標準 `AppCompatRadioButton`（外側 ring + 内側 dot）を使っているが、オリジナルは標準ラジオボタンを使わず、`SimpleCheck`（`SimpleCheck.cs`）と同じ**チェックマーク**（2 本の線で手描き）で選択状態を表現する。

**選択**: オリジナル `SimpleCheck.cs` の `OnDraw` ロジック（Canvas に 2 本の `DrawLine` でチェックマークを描く、`StrokeWidth = 2dp`、`AntiAlias`、座標は canvas 比率 22%/52%→38%/68%→74%/28%）を移植した**カスタム `View`（仮称 `KsSimpleCheckView`）**を新設し、`RadioCellViewHolder` の accessory を `AppCompatRadioButton` から当該 View に置換する。`value == selectedValue` のとき `Selected = true` でチェック表示。

**着色（確定）**: チェックマークの色は **`CellStyle` の accent 指定 → なければ `Theme.cellAccentColor`** の優先順で着色する。本 Decision の追加修正で `ks-settingsview-core` の `Theme`（Android / iOS 両方）に `cellAccentColor: KsColor` フィールドを新設した（既定値は iOS tint / Material accent 相当の `#007AFF`）。`CellStyle` / `RadioCell` には accent 専用フィールドが無いため、Android 実装では `EffectiveStyle.accentColor`（`Theme.cellAccentColor` 由来）を用いる。`KsSimpleCheckView.color` にこの実効 accent 色を渡す（従来の title 色流用は廃止）。

参照: `../AiForms.Maui.SettingsView/SettingsView/Native/Android/Cells/SimpleCheck.cs`

#### 9-4. SimpleCheckCell のデザインをオリジナルに準拠

**問題**: `SimpleCheckCellViewHolder` は `TextView "✓"` で左端にチェックを置く最小実装で、オリジナル `SimpleCheckCellView.cs`（`SimpleCheck` を 30×30dp で `AccessoryStack` に配置）とデザインが明らかに異なる。Decision 8 で「責務外」としていたが、本レビューで是正対象となった。

**選択**: 9-3 で新設する `KsSimpleCheckView`（カスタム Canvas 描画）を `SimpleCheckCellViewHolder` でも採用し、オリジナルの `SimpleCheckCellView` と同じ表現（`SimpleCheck` を所定サイズで配置、`Checked` で `Selected` 切替）に揃える。

**配置（確定）**: オリジナル `SimpleCheckCellView.cs` は `AccessoryStack.AddView(_checkView, 30, 30)` でチェックを **accessory（右側）・30×30dp** に配置している。これに準拠し、Android 実装でも `KsSimpleCheckView` を **accessory（右側）に 30×30dp 相当で配置**する。従来の「左側 ✓（TextView）」表現からは変更した（tasks.md 21.3.3 の「実装時にオリジナルと照合して配置を決定し design.md 9-4 に追記」を本節で確定）。

**着色（確定）**: チェックマークの色は 9-3 と同様に **`CellStyle` の accent 指定 → なければ `Theme.cellAccentColor`** の順で着色する（`SimpleCheckCell` / `CellStyle` に accent 専用フィールドが無いため、Android 実装では `EffectiveStyle.accentColor`（`Theme.cellAccentColor` 由来）を用いる）。

参照: `../AiForms.Maui.SettingsView/SettingsView/Native/Android/Cells/SimpleCheckCellView.cs`

**共通化**: 9-3 と 9-4 は同一の `KsSimpleCheck` 描画ロジックを共有する。RadioCell・SimpleCheckCell の両方から再利用できる単一のカスタム View として実装する。

#### 9-5. タッチフィードバック（Ripple）の移植 — `Theme.selectedColor` 連動

**問題**: 現在の Android 各 ViewHolder は `container.setBackgroundColor(effective.backgroundColor)` で単色を塗るだけで、**タップ時の Ripple / 選択フィードバックが一切ない**。オリジナル `CellBaseView.cs` ではすべての Cell が `RippleDrawable` を背景に持ち、タップで Ripple、選択状態で `selectedColor` のハイライトが表示される。画像の Storage セルの黄色ハイライトはサンプルが `SelectedColor` に黄色を設定した結果である。

**オリジナルの仕組み（`CellBaseView.cs` L138-156, L335-344）**:
- `_backgroundColor = ColorDrawable()`（通常背景）/ `_selectedColor = ColorDrawable(Argb(125,180,180,180))`（選択時）
- `StateListDrawable` で `state_selected` ↔ 通常を切替
- `Background = RippleDrawable(GetPressedColorSelector(rippleColor), stateListDrawable, null)`
- `rippleColor` は `CellParent.SelectedColor`（指定時）、未指定時は `Rgb(180, 180, 180)`
- `UpdateSelectedColor()`: `_selectedColor.Color = SelectedColor.MultiplyAlpha(0.5f)`、ripple 色も `SelectedColor` に追従

**選択**: KsSettingsView の `Theme.selectedColor`（既存フィールド、設定で変更可能）と `CellStyle` の背景色を組み合わせ、各 Cell の `container` 背景を **`RippleDrawable`** にする共通ヘルパ（仮称 `applyCellBackground(view, effective, theme)`）を `LabelCellViewHolder.kt`（共通ヘルパ層）に新設する。

1. 通常背景は `effective.backgroundColor`、ripple 色は `theme.selectedColor`（未指定時はオリジナル準拠の `Rgb(180,180,180)` 相当をデフォルトに）
2. `RippleDrawable(ColorStateList.valueOf(rippleColor), ColorDrawable(backgroundColor), null)` を `container.background` に設定（`setBackgroundColor` を置換）
3. 全 Cell（Label/Command/Button/Switch/Checkbox/Radio/SimpleCheck）の ViewHolder で本ヘルパを使用し、タッチフィードバックを統一適用する
4. `container.isClickable = true`（**確定**）: `RippleDrawable` の ripple は View が押下状態（`state_pressed`）を受け取れる＝ `isClickable == true` でないと発生しない。オリジナル `CellBaseView.cs` は全 Cell が Ripple を持つ設計のため、`applyCellBackground` 内で `view.isClickable = true` を設定し、`onTap` を持たない LabelCell でも Ripple を出す。`onTap` を持たない Cell は clickable でもクリックリスナーが無いためタップでは何も起きない（Ripple だけ出る・無害）。タップハンドラを持つ Cell は本ヘルパ呼び出し後に `setOnClickListener` を設定するため動作と両立する。

> **リグレッション修正（Pixel 6a 実機検証, 2026-06-02）**: 当初実装は `applyCellBackground` で `RippleDrawable` を背景設定するのみで `isClickable` を立てておらず、さらに `onTap` 未指定 Cell の `else` 分岐で `isClickable = false` を設定していたため、LabelCell や onTap 未指定 Cell で Ripple が出なかった。`applyCellBackground` で `isClickable = true` を設定し、各 ViewHolder の no-handler 分岐から `isClickable = false` を除去（クリックリスナーのみ解除）して是正した。

> `Theme.selectedColor` は既に存在する（既定 `DEFAULT_SELECTED_COLOR ≒ #D9D9D9`）。「色は設定で変更可能」という要件は本フィールドで充足される。`onDrawOver` 罫線（Decision 8）との重畳順序（Ripple は children 描画、罫線は onDrawOver で最前面）に問題がないことを実機で確認する。

**代替案（却下）**:
- 案 A: `?attr/selectableItemBackground` をそのまま使う → `selectedColor` による色カスタマイズができず「設定で変更可能」要件を満たせない。
- 案 B: `foreground` に ripple、`background` に色 → API 23+ で `foreground` 制約があり、罫線重畳と相性が悪い。

#### 9-6. SwitchCell のセル全体タップで ON/OFF（Android 設定アプリ準拠）

**問題**: 現在の `SwitchCellViewHolder` はスイッチウィジェット本体をタップしたときのみ ON/OFF が切り替わる。オリジナル（および Android 標準の設定アプリ）はセル全体タップでもスイッチがトグルする。

**選択**: `SwitchCellViewHolder.bind` で `container.setOnClickListener` を設定し、タップ時に `switchView.toggle()`（または `isChecked = !isChecked`）を呼ぶ。スイッチ自体の `setOnCheckedChangeListener` 経由で `onValueChanged` が一元発火するようにし、二重発火を防ぐ（コンテナクリックは `switchView` の状態を変えるだけ、通知は `OnCheckedChangeListener` 一本に集約）。スイッチウィジェット直接操作も従来どおり機能する。`reset()` で `container` の listener も null 化する。

> Checkbox / Radio / SimpleCheck は既にセル全体タップ対応済み（`container.setOnClickListener`）。SwitchCell のみ未対応だったため本項で揃える。

## Risks / Trade-offs

- **リスク**: SwitchCell の `onValueChanged` がリストア再描画時に古いクロージャを参照
  - **緩和策**: ViewHolder の `bind` 時に毎回 listener を再設定。`reset()` で listener を null 化。
- **リスク**: RadioCell の selectedValue 同期が利用者責任のため、誤って異なる値を設定する事故
  - **緩和策**: ドキュメントで明示。将来 `RadioGroup` ヘルパ関数の提供を検討（Phase 6）。
- **トレードオフ**: 7 種すべての Cell を 2 プラットフォーム同時実装するため、PR が大きくなりやすい
  - **緩和策**: tasks.md で Cell ごとに Phase を区切り、レビューを Cell 単位で実施できるようにする。

## Open Questions

- アイコン（`icon`）は URL ベースか論理名ベースか？ → 本変更提案は「論理名 + 任意 URL」の `KsImage(name: String?, url: URL?)` 値型を Core 側で定義する方針（必要なら `add-settings-view-core` の追補で対応するか、本変更で `KsImage` を導入する）。tasks.md では `KsImage` 導入を本変更提案に含める。
