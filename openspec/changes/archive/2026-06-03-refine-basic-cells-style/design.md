## Context

### 現状

`add-cell-types-basic` の実装が完了し、7 種の基本 Cell（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell）が iOS / Android 両プラットフォームで動作している。しかし実機での動作確認の結果、移植元 AiForms.Maui.SettingsView の見た目との差分が以下の 5 点で確認された：

1. **Sample に Theme カスタマイズが入っていない** → 既定の青系で表示され、MAUI 版の黄/ベージュ調と乖離。
2. **Theme / CellStyle が痩せている** → MAUI Sample の `<Style TargetType="sv:SettingsView">` で指定される `BackgroundColor` / `CellTitleFontSize` / `CellValueTextColor` 等が KsSettingsView では指定経路が無い。
3. **SwitchCell / CheckboxCell が他 Cell より高く描画される** → Theme 側に `RowHeight` / `HasUnevenRows` 概念がなく、各 ViewHolder が `WRAP_CONTENT` のまま並ぶため。
4. **iOS にタッチフィードバックが無い** → `UICollectionViewListCell.backgroundConfiguration` の選択状態反映を実装していない。
5. **Android CheckboxCell の右端アクセサリ位置が他 Cell より内側にずれる** → `AppCompatCheckBox` の内部 padding（Material 推奨タッチ域）に起因。

### 制約

- 既存テスト（特に `Theme()` / `CellStyle()` / 各 Cell コンストラクタ）はデフォルト値前提で書かれている。**フィールド追加はすべてデフォルト値付きで API 互換維持**。
- `Cell.equals` / `hashCode` は値型の性質として全フィールド比較。差分検出は id 同一性のみで判定するため、`isEnabled` 変更は `replaceCell` 経路で反映される。
- Android は `Theme.Material3.*` 派生テーマ前提（既存 memory 通り）。MaterialCheckBox の前提条件はクリアしている。
- iOS は `UICollectionLayoutListConfiguration` ベース。`estimatedItemSize = .automatic` で動作している既存設計を維持。
- 「KsSettingsView の Theme 既定値はニュートラルのまま、Sample 側で MAUI 色を渡す」という方針が explore で確定済み。

### ステークホルダー

- ライブラリ利用者：Theme / CellStyle 経由で MAUI 互換の見た目を再現できる必要がある。
- Sample 利用者（このライブラリを評価する人）：Sample が MAUI 原典と同じ見た目で起動することで、移植先としての完成度を判断できる。

## Goals / Non-Goals

**Goals:**

- AiForms.Maui.SettingsView Sample（`Sample/Views/MainPage.xaml`）と KsSettingsView Sample の見た目を **限りなく一致** させる。
- `Theme.rowHeight` / `Theme.hasUnevenRows` / `CellStyle.cellHeight` の組み合わせで、原典の高さ仕様を完全再現する。
- iOS にタッチフィードバック（`Theme.selectedColor` で背景を選択時に塗る）を導入する。
- Android Checkbox の右端アクセサリ位置を他 Cell と揃える（最小コストで）。
- 全 Cell に `isEnabled`、ButtonCell に `titleAlignment` を追加し、原典で確認できた挙動の差分を埋める。
- Description 折返し / HintText / IconSource を Sample で目視確認できるようにする。

**Non-Goals:**

- KsSettingsView の Theme 既定値を MAUI 色（黄/ベージュ）に寄せる。Theme 既定値はクロスプラットフォーム中立色のまま据え置く。
- CommandCell の `KeepSelectedUntilBack` 相当の遷移制御。今回の KsSettingsView は SwiftUI / Compose のナビゲーションに乗る前提のため不要。
- LabelCell の `IgnoreUseDescriptionAsValue`。ValueText を独立フィールドにしている設計上、論理的に不要。
- `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` / `TextPickerCell` / `EntryCell` / `PickerCell` / `CustomCell` 等の入力系・カスタム系 Cell（別 change `add-cell-types-input` / `add-cell-types-custom` のスコープ）。

## Decisions

### Decision 1: RowHeight / HasUnevenRows は `Theme` に置く

**選択**: `Theme.rowHeight: Int`（既定 `-1`）と `Theme.hasUnevenRows: Bool`（既定 `false`）を追加し、`KsSettingsViewController.applyTheme(theme)` / `KsSettingsView`（Compose） の `theme` 引数経由で渡す。

**理由**:
- 原典の `SettingsView.RowHeight` / `HasUnevenRows` は SettingsView 単位のスタイル属性であり、Theme（= SettingsView 全体の見た目を司る論理スタイル）と同じ責務領域。
- `KsSettingsViewController` / `KsSettingsView` の API を増やさず、Theme の更新（`SettingsRootDiff.updateTheme`）経路で動的にも変更できる。
- 既存の Theme 更新フロー（applyDiff の `.updateTheme` ケース）が再利用できる。

**代替案**:
- (A) `KsSettingsViewController` / `KsSettingsView` 自身に独立プロパティとして持たせる。原典に最も忠実だが、Theme と切り離されることで「実効高さの合成」ロジックが Theme と高さ属性の二経路を見る必要があり、合成複雑化する。
- (B) `CellStyle` のみで個別に持たせる。SettingsView 全体の一括設定ができず、毎回 Cell ごとに指定が必要で UX が悪い。

### Decision 2: Theme 既定値は KsSettingsView ニュートラル色を維持、Sample で MAUI Theme を渡す

**選択**: `Theme()` の既定値は現状のニュートラル色を維持する。Sample（iOS / Android）が `Theme(viewBackgroundColor: ..., cellBackgroundColor: ..., separatorColor: ..., ...)` を明示的に渡す。

**理由**:
- ライブラリは中立的な見た目を既定とすべき（利用者プロジェクトの大半が AiForms 移植ではない）。
- Sample は移植元との互換確認が目的だから、Sample 側で MAUI 互換 Theme を提供する方が役割分担が綺麗。

**代替案**: Theme 既定値を MAUI 色に変更する。ライブラリ全体のトーンが「移植元準拠」に寄りすぎてしまうため棄却。

### Decision 3: 実効高さ（effective height）合成ロジック

**選択**:

```
effectiveCellHeight(theme, cellStyle) =
    let userHeight = cellStyle.cellHeight ?? theme.rowHeight  // どちらも未指定（-1）なら自動
    let minHeight  = max(userHeight, MinRowHeight)            // MinRowHeight: iOS 48pt / Android 44dp
    if theme.hasUnevenRows {
        // 自動 + 最低高さ保証。タイトル + 説明文の自然な伸縮を許可
        return MinHeight(minHeight)
    } else {
        // 固定高さ。userHeight が未指定（-1）なら MinRowHeight に揃える
        return FixedHeight(minHeight)
    }
```

- **iOS 実装**: 既存の `estimatedItemSize = .automatic` を維持しつつ、`UICollectionViewListCell` の `contentView` に `heightAnchor.constraint(greaterThanOrEqualToConstant: minHeight)` を bind 時に貼る。固定高さ時は `heightAnchor.constraint(equalToConstant: minHeight)` に切り替え。
- **Android 実装**: `container.minimumHeight = minHeightPx`（dp → px 変換は `Resources.displayMetrics.density`）。固定高さ時は `container.layoutParams.height = minHeightPx`。
- 制約は bind 時に毎回構築せず、ViewHolder にキャッシュした最後の制約値と比較して変化時のみ更新する（パフォーマンス）。

**理由**:
- 原典の挙動を完全に再現できる。
- 既存の Auto Layout / WRAP_CONTENT ベースの実装を最小改変で拡張できる。

**代替案**:
- (A) ViewHolder 内で `setHeight(fixed=true, height=...)` のような命令的 API を持つ。ViewHolder ごとに分散実装が必要で、不整合リスクが高い。

### Decision 4: iOS タッチフィードバックは `configurationUpdateHandler` 方式

**選択**: 各 `UICollectionViewListCell` サブクラスの `init` 時に `configurationUpdateHandler` を設定し、選択／ハイライト状態に応じて `backgroundConfiguration.backgroundColor` を `Theme.selectedColor` で塗り替える。bind 時に `Theme` を `objc_setAssociatedObject` ではなく ViewHolder 内 stored property（弱参照は不要、Theme は値型）として保持して handler から参照する。

**理由**:
- `selectedBackgroundView` は古い `UITableViewCell` 系 API で、modern compositional layout の `UICollectionViewListCell` では `backgroundConfiguration` 経由が公式推奨。
- `configurationUpdateHandler` は UIKit が状態遷移時に自動呼び出すため、ハイライトのフェードイン／アウトが自然に再生される。
- Android の Ripple と挙動的に等価（押下中は `selectedColor` で塗り、離すと元に戻る）。

**代替案**:
- (A) `selectedBackgroundView = UIView()` を bind 時に設定。古いパターンで Modern Layout との整合性が悪い。
- (B) bind 時に毎回 backgroundConfiguration を更新。state 遷移時の自動ハイライトが効かず、タップ時に見た目が変わらない。

### Decision 5: Android CheckboxCell の Material 化

**選択**: `AppCompatCheckBox` を `com.google.android.material.checkbox.MaterialCheckBox` に置換する。さらに：
- `minimumWidth = 0` / `minimumHeight = 0`（既定のタッチ域 padding を無効化）
- `setPadding(0, 0, 0, 0)`
- `buttonTintList` で `cell.accentColor ?? theme.cellAccentColor` を反映
- `isClickable = false` / `isFocusable = false`（既存通り、container タップでトグル）

これで他 Cell（Switch / Radio / SimpleCheck）の accessoryHolder 右端と CheckboxCell のチェックボックス右端が同一 X 座標に揃う。**もしそれでもズレが残る場合**は、自前 Drawable で iOS と同形の角丸四角チェックボックス（角丸 4dp、border 2dp accent 色、check 時は accent で塗りつぶし＋白チェックマーク）を生成して `setButtonDrawable(customDrawable)` で差し替える経路（design 上のフォールバックパス）を tasks.md に記載しておく。

**理由**:
- `MaterialCheckBox` は Material3 テーマで標準提供される。新規依存なし。
- padding 補正だけで揃う可能性が高く、コスト最小。
- iOS の角丸四角と概観が近い（Material3 のチェックボックスはもともと角丸の四角形）。

**代替案**:
- (A) いきなり自前 Drawable 化。実装コスト中で、iOS との視覚的一致は最も高いが、リカバリ余地を奪う。フォールバックとして残す。
- (B) `AppCompatCheckBox` のまま padding 操作のみ。Material3 ではない古い CheckBox 描画になり Theme.cellAccentColor が反映されない場合がある。

### Decision 6: `isEnabled` の描画は「コントロール disabled + テキスト色置換」

**選択**:
- `isEnabled = false` のとき：
  - SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / CommandCell / ButtonCell の中の操作可能 UI 要素（Switch / CheckBox / Radio チェックビュー / Button 風 TextView 等）に **`view.isEnabled = false`** を設定。
  - container の `setOnClickListener(null)` / `isClickable = false`（Android）／`isUserInteractionEnabled = false`（iOS）でタップ無効化。
  - タイトル / 説明文 / 値テキスト / ヒントテキストの `textColor` を **`Theme.disabledTextColor`** に置換。
- `isEnabled = true`（既定）のとき：通常の bind ロジックを通常通り適用。
- alpha 0.5 などの全体半透明化は **しない**（explore で確定済み方針）。

**理由**:
- テキスト色の置換だけでも視覚的に十分に「無効」が伝わる。
- alpha は親 ViewGroup レイヤーや RippleDrawable と干渉しやすく副作用が大きい。

**代替案**: 全体 alpha 0.5 化。原典 AiForms `SetEnabledAppearance` に最も忠実だが、Ripple との干渉や Compose / SwiftUI ラッパでの再構築コストが大きい。

### Decision 7: `Cell.equals` への `isEnabled` 影響と差分判定

**選択**:
- `isEnabled` を Cell の data class / struct の通常フィールドとして追加。`equals` / `hashCode` に参加させる。
- 既存規約「差分検出は id 同一性のみ」（`add-partial-update-core` で確定）通り、`isEnabled` だけ変更したケースは `replaceCell` 経路で `reconfigureItems` / `notifyItemChanged` され、ViewHolder の bind が再実行される。
- テストで「`isEnabled` を `true → false` に変更すると `replaceCell` 経路で bind 内のテキスト色置換が反映される」ことを確認する。

**理由**: 既存規約と整合し、特別扱いせずに通常の値型として扱える。

### Decision 8: ButtonCell の `titleAlignment` 描画

**選択**:
- iOS: `UIListContentConfiguration` を使わず、`ButtonCellView` の content view 直下に `UILabel` を配置し、`UILabel.textAlignment = .left / .center / .right` を `titleAlignment` から決定する。既存実装が中央寄せの `UILabel` 1 枚であれば、`textAlignment` の切り替えだけで対応可能。
- Android: `titleView`（`TextView`）の `gravity = Gravity.START | END | CENTER_HORIZONTAL` を切り替える。既存の `LabelCellViews` から派生する `ButtonCellViewHolder` の中で適用する。

**理由**: 最小コストかつ両プラットフォーム同等の表現。

### Decision 9: Theme / CellStyle のフィールド追加順序と互換性

**選択**:
- Swift: `init(...)` の追加引数は **既存引数の末尾** に追加し、デフォルト値を付与（呼び出し側の名前付き引数を維持）。
- Kotlin: `data class` のコンストラクタ引数も既存引数の末尾に追加し、デフォルト値を付与。
- 既存テストの構築呼び出しは無変更で通る想定。新フィールドを使う新規テストのみ明示指定する。

**理由**: SourceCompat 維持。

### Decision 10: Sample の再構築方針（iOS / Android）

**選択**: 既存 `BasicCellsDemoView.swift` / `BasicCellsDemoScreen.kt` を MAUI 原典 `MainPage.xaml` と同構成・同 Theme で **書き換える**（破棄して再生成相当）。差分が大きいため部分編集ではなく全文置換アプローチを取る。

**理由**: MAUI 互換 Theme + 各セル種を網羅的にデモする画面に再構築するため、差分編集より全文置換のほうがレビューしやすい。

### Decision 11: CheckboxCell の Android 側 Requirement 表現

**選択**: 既存の `cell-types-basic` spec の `CheckboxCell` Requirement は iOS のみ「角丸の四角いチェックボックス UI」を要求している。本 change では Android 側に対応する Scenario として「右端の Material チェックボックス（accent 色反映）が他アクセサリと同一 X 座標に揃う」「`isChecked=true` で accent カラーで塗り潰される」を追加する（MODIFIED Requirement）。

**理由**: iOS / Android 並行を保つため、Android 側の挙動を明文化する。

### Decision 12: Theme.titleColor / Theme.titleFont の追加とフォールバック順序

**背景**: AiForms.Maui.SettingsView では `Theme.CellTitleColor` / `Theme.CellTitleFont` が「全 Cell タイトルの既定色／フォント」として定義されており、Cell 個別の上書きがないときに採用される。現状の KsSettingsView は `Theme` に `titleColor` / `titleFont` を持たず、`CellStyle.titleColor` が `nil` のときシステム既定（iOS `.label`、Android デフォルト TextView color）に直接フォールバックしていた。これは原典の基本プロパティの仕様漏れであり、本 change（基本 Cell スタイル整合）のスコープに本来含まれるべきもの。

**選択**:
- `Theme` に `titleColor: KsColor?`（既定 `nil`）と `titleFont: KsFont?`（既定 `nil`）を末尾追加する。
- EffectiveStyle 合成のタイトル色／フォントは次の順序で解決する（3 段階優先順位）:
  1. `CellStyle.titleColor` が `nil` でなければそれを採用
  2. それ以外で `Theme.titleColor` が `nil` でなければそれを採用
  3. それ以外はプラットフォーム既定（iOS: `UIColor.label`、Android: `TextView` の既定色）
- ButtonCell の `baseColor` 解決は同じ思想で 4 段階に拡張する:
  1. `ButtonCell.titleColor` が指定されていればそれを採用（Cell 個別）
  2. `CellStyle.titleColor` が指定されていれば `effective.titleColor`（合成済み）を採用
  3. `Theme.titleColor` が指定されていれば `effective.titleColor`（合成済み）を採用
  4. それ以外は iOS `.systemBlue` / Android Material Primary
  - 実装上は 2 と 3 を「`effective.titleColor` が明示由来（cellStyle or theme）か、プラットフォーム fallback 由来か」を判定するヘルパで合算する。EffectiveStyle に `titleColorIsExplicit: Bool` を追加し、`(cellStyle.titleColor != nil) || (theme.titleColor != nil)` のときに `true` とすることで Button 側から 1 行で判定可能にする。

**理由**:
- 原典 AiForms の `Theme.CellTitleColor` / `Theme.CellTitleFont` を欠落させたままだと、MAUI 互換 Theme（Sample）で「全 Cell のタイトル色を一括変更する」操作が不可能になる。これは Theme の主要価値の一つで、Sample 再構築の本来の意図にも反する。
- 「Theme と CellStyle が同じセマンティクスを持つフィールド対」を増やすことで、`CellStyle 個別 → Theme 既定 → プラットフォーム fallback` の階層が一貫し、設計の対称性が保たれる。
- ButtonCell の baseColor を 4 段階にすることで、「`Theme.titleColor` だけ指定して `ButtonCell` のテキスト色も Theme と同色にしたい」が成立する。これは AiForms 原典でも `Theme.CellTitleColor` が ButtonCell に効く挙動と一致する。

**代替案と却下理由**:
- `Theme.titleColor` 不在のまま「将来別 change で追加」する案: 第一次レビューでこの方針を採用したが、ユーザ指摘の通り「原典の基本プロパティの欠落是正」が本 change のテーマであるため、ここで対応するのが正しい。
- `EffectiveStyle.titleColor` を `KsColor?`（Optional）に変更してプラットフォーム解釈を後回しにする案: 各 Cell View で `?? .label` を書く必要があり、現状の非 Optional `UIColor` / `Int` 設計から後退する。代わりに `titleColorIsExplicit` フラグで Button 用 baseColor 判定の必要を満たす。

## Risks / Trade-offs

- **[Risk] Theme フィールド追加で既存テストの `assertEquals(Theme(), Theme())` が失敗する可能性**
  → **Mitigation**: フィールド追加はすべてデフォルト値付き。既存テストは引数なし `Theme()` を呼ぶ限り何も変更不要。新フィールドを明示する新テストのみ追加。

- **[Risk] `isEnabled` を持つ Cell の追加で、既存 Cell 構築（コンストラクタ全引数指定）が壊れる**
  → **Mitigation**: `isEnabled` を **既存引数の末尾** にデフォルト `true` で追加。名前付き引数で構築する Swift / Kotlin の文化上、末尾追加は SourceCompat を保つ。grep で全 Cell コンストラクタ呼び出し箇所を確認し、影響なしを確認する。

- **[Risk] iOS の `configurationUpdateHandler` 内で `backgroundConfiguration` を毎回再構築するとセル再利用時のチラつきリスク**
  → **Mitigation**: handler 内で `state.isHighlighted || state.isSelected` を判定し、変化があったときだけ `backgroundConfiguration.backgroundColor` を更新（同じ状態の連続呼び出しでは set しない）。bind 完了後の最初の呼び出しは無条件で初期化。

- **[Risk] Android `MaterialCheckBox` 置換が既存テスト（`AppCompatCheckBox` 前提）を壊す**
  → **Mitigation**: 既存 Checkbox 関連テスト（`CheckboxCellViewHolder` の simulate 系）を grep し、`MaterialCheckBox` でも動作するよう（型を `CompoundButton` 抽象に寄せる、または型名のみ書き換える）アップデート。

- **[Risk] HasUnevenRows = false 時に長文 Description が省略される副作用**
  → **Trade-off**: これは原典の挙動と一致。長文を完全表示したいケースは `hasUnevenRows = true` を選ぶか、`CellStyle.cellHeight` で個別に伸ばす（仕様通り）。

- **[Risk] 「実効高さ」変更時に bind 中の view が再レイアウトされず古い高さで描画される**
  → **Mitigation**: bind 末尾で `container.requestLayout()`（Android）／`contentView.setNeedsLayout()`（iOS）を呼ぶ。

- **[Risk] Sample の Theme 色がアクセシビリティ的に薄い**
  → **Trade-off**: Sample の目的は MAUI 互換確認なので忠実性を優先。本番アプリで使う Theme は利用者責任。

- **[Risk] `Cell.equals` への `isEnabled` 組み込みで、`isEnabled` トグルが Set / Dictionary のキー位置を変えてしまう**
  → **Mitigation**: 既存規約通り「差分検出は id 同一性のみ」なので Set / Dictionary を identity ベースで使う層は影響を受けない。値型としての等価性が変わるだけで、テスト上の `assertEquals` が新規シナリオで失敗するかもしれないが期待挙動の更新で対応可能。
