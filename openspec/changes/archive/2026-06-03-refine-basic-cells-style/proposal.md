## Why

基本 Cell 7 種（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell）は実機で基本動作までは確認できたが、移植元 AiForms.Maui.SettingsView のスタイル・UI・レイアウトを **再現できていない**（特に行高さの不均一・iOS のタッチフィードバック欠落・Android Checkbox の配置ズレ・MAUI サンプルの色味再現不可・Description 折返しや HintText のサンプル不在）。本変更で Theme / CellStyle / Cell 各層の不足プロパティを補い、ネイティブ Cell 描画ロジックをオリジナル準拠に揃え、Sample を MAUI 原典と同構成に再構築する。

## What Changes

### Theme / CellStyle / Cell 値型の拡張

- **Theme** に以下を追加（すべてデフォルト値付きで既存呼び出しと互換維持）:
  - `viewBackgroundColor`（SettingsView 全体の背景色。`cellBackgroundColor` とは独立）
  - `rowHeight: Int`（既定 `-1` = 未指定、原典 SettingsView.RowHeight 相当）
  - `hasUnevenRows: Bool`（既定 `false`、原典 SettingsView.HasUnevenRows 相当）
  - `disabledTextColor`（`isEnabled = false` 時のタイトル／説明文の置換色）
  - `headerFontSize: Double`（既定 `-1` = 既定サイズ）
  - `footerFontSize: Double`（既定 `-1` = 既定サイズ）
  - `titleColor`（Cell タイトルの既定色。原典 AiForms `Theme.CellTitleColor` 相当。`CellStyle.titleColor` が未指定のとき採用）
  - `titleFont`（Cell タイトルの既定フォント。原典 AiForms `Theme.CellTitleFont` 相当。`CellStyle.titleFont` が未指定のとき採用）
- **CellStyle** に以下を追加（すべて Optional / 未指定 = Theme 継承）:
  - `backgroundColor`（Cell 個別背景。Theme.cellBackgroundColor を上書き）
  - `accentColor`（Cell 個別 accent。Theme.cellAccentColor を上書き）
  - `valueTextColor`
  - `valueTextFont`
- **`enum CellTitleAlignment { start, center, end }`** を core に新設。
- **ButtonCell** に `titleAlignment: CellTitleAlignment`（既定 `.center`）を追加。
- **全 7 種 Cell** に `isEnabled: Bool`（既定 `true`）を追加。`false` のときはコントロール（Switch/Checkbox/Radio/SimpleCheck/Command/Button）を disabled にし、タイトル／説明文／値テキストの **テキスト色のみ** `Theme.disabledTextColor` に置換する（alpha 化や全体半透明化はしない）。**BREAKING**：data class / struct のフィールド追加は SourceCompat 観点では非破壊（デフォルト値あり）だが、`==` / `hashCode` の挙動が変わるためテスト・比較を行うコードに影響する。

### ネイティブ描画ロジックのオリジナル準拠化

- **iOS**: 全 Cell View（LabelCellView / CommandCellView / ButtonCellView / SwitchCellView / CheckboxCellView / RadioCellView / SimpleCheckCellView）で `configurationUpdateHandler` を設定し、`state.isHighlighted || state.isSelected` 時に `backgroundConfiguration.backgroundColor = Theme.selectedColor` を反映する（タッチフィードバック）。
- **iOS / Android 共通**: `Theme.rowHeight` / `Theme.hasUnevenRows` / `CellStyle.cellHeight` から **実効高さ（effective height）** を合成して反映する：
  - `hasUnevenRows == false`：全 Cell に `max(rowHeight, MinRowHeight)` を強制（固定高）。
  - `hasUnevenRows == true`：個別 `CellStyle.cellHeight` 優先、未指定なら最低高さ `max(rowHeight, MinRowHeight)` をガードとした auto layout。
  - `MinRowHeight`：iOS `48pt` / Android `44dp`（原典踏襲）。
- **iOS**: HasUnevenRows = true のとき content `StackH.heightAnchor >= effectiveMinHeight` の制約を加える。HasUnevenRows = false のとき固定高さ制約に置換する。
- **Android**: `container.minimumHeight = effectiveMinHeightPx` を全 Cell ViewHolder の bind 時に設定。HasUnevenRows = false 時は `layoutParams.height = effectiveHeightPx` で固定。
- **Android**: CheckboxCell の `AppCompatCheckBox` を `MaterialCheckBox` に置換し、内部 padding（`setPadding(0,0,0,0)`、`minWidth/minHeight = 0`）で他アクセサリ（Switch / Radio / SimpleCheck）と右端位置を揃える。それでも揃わない場合は自前 Drawable で iOS と同形の角丸四角チェックボックスを描画する経路を design.md に記す。
- **CellStyle.backgroundColor / accentColor / valueTextColor / valueTextFont** を iOS / Android 双方の EffectiveStyle 合成に組み込み、各 Cell 描画で反映する。
- **Theme.titleColor / Theme.titleFont** を iOS / Android 双方の EffectiveStyle に組み込み、`CellStyle.titleColor`（および `titleFont`）が未指定のとき Theme 値を採用する 2 段階フォールバックを実装する。Theme 側も未指定（`nil` / `null`）のときはプラットフォーム既定（iOS: `.label`、Android: 既定 TextView color）にフォールバックする 3 段階優先順位とする。ButtonCell の `baseColor` 解決は「Cell 個別 `titleColor` → `CellStyle.titleColor` → `Theme.titleColor` → `.systemBlue`（iOS）/ Material Primary（Android）」の順序にする。
- **isEnabled = false** 時の描画を全 Cell に実装（コントロールの `isEnabled = false` 化 + テキスト色置換）。

### Sample の MAUI 原典互換構成への再構築

- iOS（`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift`）と Android（`samples/android/app/.../BasicCellsDemoScreen.kt`）の両方を MAUI 版 `MainPage.xaml` と同構成に再構築：
  - MAUI 互換 Theme を明示的に渡す（`viewBackgroundColor = #F2EFE6`、`cellBackgroundColor = #FFFFFF`、`separatorColor = #E6DAB9`、`selectedColor = #50FFBF00`、`cellAccentColor = #FFBF00`、`headerTextColor = #CC9900`、`footerTextColor = #999999`、`hasUnevenRows = true`、`disabledTextColor = #999999` 等）。
  - CommandCell（プロフィール風、IconSource + Description + `CellStyle.cellHeight = 80`）。
  - LabelCell（Storage 例、IconSource + ValueText + 長文 Description で折返し確認）。
  - SwitchCell（長文 Description 付きで折返し確認）。
  - CheckboxCell（`isChecked = true` 既定で accent 反映確認）。
  - RadioCell（FooterText 付きセクション）。
  - SimpleCheckCell（複数並べた section）。
  - ButtonCell（`titleAlignment = .center` で `CellStyle.titleColor` 反映確認）。
  - 任意 1 セルで HintText を表示（Hint 描画確認）。

### スコープ外（今回は実装しない）

- `KeepSelectedUntilBack`（CommandCell）／`IgnoreUseDescriptionAsValue`（LabelCell）。後者は ValueText を独立フィールドにしている設計上、論理的に不要。
- Theme 既定値の MAUI 色化（KsSettingsView はニュートラル既定を維持、Sample で MAUI 色を渡す）。

## Capabilities

### New Capabilities

なし。

### Modified Capabilities

- `cell-types-basic`: ButtonCell に `titleAlignment` Requirement を追加、全 Cell に `isEnabled` Requirement を追加、CheckboxCell の Android 側「右端アクセサリ位置」Scenario を追加。
- `settings-view-core`: Theme に `viewBackgroundColor` / `rowHeight` / `hasUnevenRows` / `disabledTextColor` / `headerFontSize` / `footerFontSize` / `titleColor` / `titleFont` を追加。CellStyle に `backgroundColor` / `accentColor` / `valueTextColor` / `valueTextFont` を追加。`CellTitleAlignment` enum を新規定義。
- `settings-view-ios-ui`: タッチフィードバック（`configurationUpdateHandler` + `Theme.selectedColor`）、`Theme.rowHeight` / `hasUnevenRows` / `CellStyle.cellHeight` から実効高さ合成、isEnabled 描画、CellStyle.backgroundColor / accentColor / valueTextColor / valueTextFont の EffectiveStyle 合成と反映、`Theme.titleColor` / `Theme.titleFont` を用いた 2 段階タイトル色フォールバック、ButtonCell の baseColor 解決の 3 段階化。
- `settings-view-android-ui`: 同上に加え、CheckboxCell の `MaterialCheckBox` 置換と内側 padding 補正。
- `samples-ios`: 基本 Cell デモ画面（`BasicCellsDemoView.swift`）を MAUI `MainPage.xaml` 互換構成へ再構築。
- `samples-android`: 同上を Android Compose Sample（`BasicCellsDemoScreen.kt`）に対しても適用。

## Impact

### 影響を受けるコード

- iOS（Swift Package `ios/`）:
  - `Sources/KsSettingsViewCore/Theme.swift`、`CellStyle.swift`、`KsCell.swift`、各 `*Cell.swift`
  - `Sources/KsSettingsViewUI/*CellView.swift`、`EffectiveStyle.swift`（あるいは合成ロジック）、`KsSettingsViewController` 周辺の高さ適用ロジック
  - `Sources/KsSettingsViewSwiftUI/`（Theme 渡し API の整合性確認、必要なら追加）
- Android（Gradle modules `android/`）:
  - `ks-settingsview-core/.../Theme.kt`、`CellStyle.kt`、`Cell.kt`、各 Cell data class
  - `ks-settingsview-ui/.../EffectiveStyle.kt`、`*CellViewHolder.kt`、`KsSettingsView.kt`（行高さ反映）、`CheckboxCellViewHolder.kt`（MaterialCheckBox 置換）
  - `ks-settingsview-compose/.../KsSettingsViewComposable.kt`（Theme 渡しの API 整合性確認）
- Sample:
  - `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift`
  - `samples/android/app/src/main/kotlin/.../BasicCellsDemoScreen.kt`

### 影響を受ける外部 API

- `Theme.init` の引数増加（既存呼び出しはデフォルト値付与で互換維持）。新規 `titleColor: KsColor?`（既定 `nil`）と `titleFont: KsFont?`（既定 `nil`）を末尾追加。
- `CellStyle.init` の引数増加（同上）。
- 全 Cell 型のコンストラクタに `isEnabled` 引数追加（デフォルト `true`、既存呼び出し互換）。
- `ButtonCell` コンストラクタに `titleAlignment` 引数追加（デフォルト `.center`、既存呼び出し互換）。
- Theme / Cell の `equals` / `hashCode` / `Hashable` のハッシュ計算が変化する（フィールド増加に伴う既知の波及）。

### 依存関係

- 新規ライブラリ依存はなし。Android は既存の Material Components（`com.google.android.material:material`）に含まれる `MaterialCheckBox` を利用するのみ。

### Risks

- **既存テスト破壊リスク**: Theme / CellStyle / Cell の `equals` ベース比較を行っているテストが新フィールド追加で再評価される可能性がある。デフォルト値付きフィールド追加であれば等価判定は維持できるはずだが、明示的にフィールド全列挙で構築しているテストはアップデートが必要。tasks.md の Phase 4（テスト整備）で追従する。
- **Cell の `equals` 拘束**: `add-cell-types-basic` で確定済みの「diff は id 同一性のみ」規約に反しないよう、`isEnabled` を変更した Cell の diff 経路が **replaceCell** として通る（remove/insert にならない）ことをテストで担保する。
- **Android CheckboxCell の Material 置換**: `MaterialCheckBox` は親 Theme.Material3.* を要求する。これは既存 memory（`Android テーマ要件`）で確認済みのため新規制約ではない。
- **iOS タッチフィードバック挙動の副作用**: `configurationUpdateHandler` 内で `backgroundConfiguration` を毎回再構築するため、`backgroundConfiguration.backgroundColor` を上書きしているクライアントが存在する場合に競合する可能性。今回は内部ユーザーのみのため許容するが、design.md で経路を明示する。
