## MODIFIED Requirements

### Requirement: KsImage 値型

`KsImage` は Cell のアイコン表現を運ぶ sealed 型でなければならない (SHALL)。プラットフォーム UI 型（`UIImage`、`Drawable`）を **派生の中に保持してよい**（プラットフォーム固有派生として隔離される）が、`KsImage` 共通 API としてはプラットフォーム UI 型を直接公開してはならない (MUST NOT)。プラットフォーム固有の派生を持ち、UI 層が派生ごとに解決ロジックを切り替えなければならない (MUST)。`Hashable` / `equals` 契約を満たさなければならない (MUST)（内部に `Drawable` / `UIImage` を含む派生についても、その内部 backing は識別子としては参照同一性で扱うこと）。

旧 3 フィールド形式（`name: String?` / `url: String?` / `systemName: String?`）は廃止する。

iOS 側（`KsSettingsViewCore`）は次の派生を持たなければならない (MUST)：

- `systemName(String)`: SF Symbols 名を保持する派生
- `uiImage(UIImage)`: 任意の `UIImage` を保持する派生

Android 側（`ks-settingsview-core`）は次の派生を持たなければならない (MUST)：

- `Resource(@DrawableRes resId: Int)`: Android リソース ID を保持する派生（主軸）
- `Drawable(android.graphics.drawable.Drawable)`: 任意の `Drawable` を保持する派生
- `SystemName(String)`: iOS との API 対称性のための派生（Android では解決不可、UI 層は無視する）

#### Scenario: iOS の派生定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `KsImage` を参照する
- **THEN** `public enum KsImage: Hashable` であり、`case systemName(String)` と `case uiImage(UIImage)` の 2 ケースを持つ。`Hashable` 実装は、`systemName` ケースは内部 String の hash、`uiImage` ケースは `ObjectIdentifier(uiImage)` 相当の参照同一性で hash する

#### Scenario: Android の派生定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `KsImage` を参照する
- **THEN** `sealed interface KsImage` であり、サブタイプとして `data class Resource(@DrawableRes val resId: Int) : KsImage`、`class Drawable(val drawable: android.graphics.drawable.Drawable) : KsImage`、`data class SystemName(val name: String) : KsImage` の 3 派生を持つ。`Drawable` は参照同一性で `equals` / `hashCode` を持ち、`Resource` / `SystemName` は値同一性で `equals` / `hashCode` を持つ

#### Scenario: iOS の構築

- **GIVEN** Swift コード
- **WHEN** `KsImage.systemName("bell")` または `KsImage.uiImage(UIImage(systemName: "bell")!)` を構築する
- **THEN** 該当ケースのイミュータブル値として生成され、パターンマッチ可能となる

#### Scenario: Android の構築

- **GIVEN** Kotlin コード
- **WHEN** `KsImage.Resource(R.drawable.ic_settings)` または `KsImage.Drawable(ContextCompat.getDrawable(context, R.drawable.ic_settings)!!)` または `KsImage.SystemName("bell")` を構築する
- **THEN** 該当派生のイミュータブル値として生成され、`when` でパターンマッチ可能となる

#### Scenario: 旧 KsImage 形式の廃止

- **GIVEN** 既存コード `KsImage(systemName: "bell")` または `KsImage(name = "bell")` の呼び出し
- **WHEN** 本 change のリリース後にコンパイルする
- **THEN** 旧 `KsImage(...)` 形式は廃止されているためコンパイルエラーとなり、利用者は `KsImage.systemName("bell")`（iOS）または `KsImage.Resource(R.drawable.ic_bell)`（Android）への書き換えが要求される

#### Scenario: iOS Cell からの利用（systemName）

- **GIVEN** `LabelCell(icon: KsImage.systemName("bell"))`
- **WHEN** iOS UI 層が描画する
- **THEN** UI 層は `systemName("bell")` 派生を解決し、`UIImage(systemName: "bell")` を ImageView に設定する

#### Scenario: iOS Cell からの利用（任意 UIImage）

- **GIVEN** `LabelCell(icon: KsImage.uiImage(customImage))`（`customImage: UIImage`）
- **WHEN** iOS UI 層が描画する
- **THEN** UI 層は `uiImage(customImage)` 派生を解決し、`ImageView.image = customImage` を設定する

#### Scenario: Android Cell からの利用（リソース ID）

- **GIVEN** `LabelCell(icon = KsImage.Resource(R.drawable.ic_storage))`
- **WHEN** Android UI 層が描画する
- **THEN** UI 層は `Resource(resId)` 派生を解決し、`ContextCompat.getDrawable(context, R.drawable.ic_storage)` を `ImageView.setImageDrawable(...)` に設定する

#### Scenario: Android Cell からの利用（任意 Drawable）

- **GIVEN** `LabelCell(icon = KsImage.Drawable(customDrawable))`（`customDrawable: android.graphics.drawable.Drawable`）
- **WHEN** Android UI 層が描画する
- **THEN** UI 層は `Drawable(customDrawable)` 派生を解決し、`ImageView.setImageDrawable(customDrawable)` を設定する

#### Scenario: Android で SystemName 派生のフォールバック

- **GIVEN** `LabelCell(icon = KsImage.SystemName("bell"))`
- **WHEN** Android UI 層が描画する
- **THEN** UI 層は `SystemName` 派生を解決できないため、アイコン領域を非表示（`ImageView.visibility = View.GONE`）にしてフォールバックする。エラーログや throw は発生してはならない

#### Scenario: icon = null / nil の Cell

- **GIVEN** `LabelCell(icon: nil)` または `LabelCell(icon = null)`
- **WHEN** UI 層が描画する
- **THEN** Cell のアイコン領域は非表示となり、Title が左寄せでアイコン領域分のインデントなしに配置される
