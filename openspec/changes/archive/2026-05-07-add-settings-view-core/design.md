## 参考実装

本変更提案の実装に着手する**前に必ず**以下を確認すること。仕様の取り違えは後続変更提案との不整合を生む。

- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元 `AiForms.Maui.SettingsView` の仕様要約
  - **必読セクション**: §2（CellBase の共通プロパティ 22 個）、§4（SettingsView 全体プロパティ 40+ 個）、§11（旧版との差分）
- 原典コード（Core モデルが満たすべき意味の根拠）：
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs) — Title / Description / Icon / HintText / Background / IsVisible / IsEnabled 等の共通フィールド
  - [`../AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs`](file://../AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs) — Theme に相当する全体スタイル（HeaderTextColor、CellAccentColor、SeparatorColor、HeaderPadding 等）の根拠
  - [`../AiForms.Maui.SettingsView/SettingsView/Section.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Section.cs) — Section の構造

**重要**: 本変更提案は Core 値型のみ定義し、UI 層・Handler 層には踏み込まない。スタイル合成（CellStyle 未指定 → Theme から補完）の挙動は UI 層責務とし、Core ではフィールドが nullable / Optional であることのみ規定する。

## Context

KsSettingsView は iOS / Android / MAUI で同一の概念モデルを共有するが、各プラットフォームには言語慣習がある（Swift `struct + protocol`、Kotlin `data class + sealed`、C# `BindableObject + BindableProperty`）。MAUI レイヤは Native の Core を直接参照するのではなく Bridge 経由で呼び出すが、Bridge の DTO 設計が Core モデルと矛盾しないためには、まず Native Core が両プラットフォームで論理的に同型であることが必要。本変更提案は UI・Bridge・MAUI を含まず、純粋ドメイン層の設計のみを扱う。

## Goals / Non-Goals

**Goals:**
- iOS / Android で論理的に同型な Core モデル（`SettingsRoot` / `Section` / `Cell` / `Theme` / `CellStyle`）を確立する
- DiffableDataSource / DiffUtil の差分検出に耐える `Hashable` / `data class` 契約を満たす
- プラットフォーム UI 型（`UIColor`、`android.graphics.Color`、`UIFont` 等）を Core から排除する
- 各 Native モジュールにユニットテスト基盤を構築する

**Non-Goals:**
- 具象 Cell 型（`LabelCell`、`SwitchCell` ...）の定義は本変更提案では行わない（後続 `add-cell-types-*` で対応）
- UI レンダリング・差分適用（DiffableDataSource / ListAdapter）は本変更提案では行わない
- MAUI 側の C# 型定義は行わない（Bridge 設計時に対応）
- KMP の `commonMain` への抽出は本フェーズでは行わない

## Decisions

### Decision 1: 値型ファースト

**選択**: Swift は `struct`、Kotlin は `data class` を一貫して採用する。

**理由**:
- DiffableDataSource は `Hashable` を要求し、Swift `struct` は自動 Hashable
- DiffUtil の `areItemsTheSame` / `areContentsTheSame` は `equals` を使用し、Kotlin `data class` は自動 `equals`/`hashCode`
- イミュータブル値型は MAUI からの状態更新時のスナップショット差分が単純化される

**代替案**:
- 参照型（`class` / `open class`）：継承による拡張は容易だが、`Hashable` / `equals` を手動実装する必要があり、変更検知の信頼性が落ちる。

### Decision 2: Cell 抽象は protocol / sealed interface

**選択**: iOS は `protocol KsCell`、Android は `sealed interface Cell`。

**理由**:
- Swift では Cell ごとに異なるフィールドを `struct` で表現したいため、共通契約は protocol が自然
- Kotlin の `sealed interface` はパターンマッチ（`when`）の網羅性チェックを強制でき、UI 層の安全性が高まる
- 両者とも具象 Cell の実装は `add-cell-types-*` で追加されるが、本変更提案では抽象のみを定義する

**代替案**:
- 抽象クラス継承：Swift では struct と相性が悪く、Kotlin でも `sealed class` は abstract class より型推論で劣る。

### Decision 3: AnyCell 型消去（iOS のみ）

**選択**: Swift では `Section.cells: [AnyCell]` とし、`AnyCell` は内部に `any KsCell` を保持する型消去ラッパとする。Kotlin は `sealed interface` のため不要。

**理由**:
- Swift で異種型コレクション（`[any KsCell]`）は `Hashable` 要件と相性が悪く、`AnyCell` ラッパの方が DiffableDataSource との親和性が高い
- 型消去の代償として `as?` キャストが必要になるが、これは UI ViewHolder 側で `switch` パターンマッチに集約される

**代替案**:
- `[any KsCell]`：Swift 5.7+ で利用可能だが、`Hashable` 既存実装のままでは異種コレクションでハッシュが不安定になりやすい。

### Decision 4: Theme と CellStyle の継承

**選択**: `Theme` は SettingsView 全体のデフォルト、`CellStyle` は単一 Cell の上書きとし、`CellStyle` 各フィールドは Optional / nullable で「未指定」を表現する。実効スタイルの合成（CellStyle が未指定なら Theme から取る）は UI 層の責務とする。

**理由**:
- 旧 AiForms.Maui.SettingsView の挙動と互換（個別 Cell 設定が SettingsView 全体設定より優先）
- Core はデータのみ持ち、合成ロジックを持たないことで責務分離

**代替案**:
- 合成ロジックを Core に持たせる：UI 層から重複した実装が消えるが、UI 層が UIColor 変換などを行う際に Core が UIKit 依存になり、純粋データ層の原則を破る。

### Decision 5b: SectionAccessory による header/footer の sum type 化

**選択**: `Section.header` / `Section.footer` を `String?` ではなく `SectionAccessory?` とする。`SectionAccessory` は Swift では `enum`（`.text(String)` / `.custom(AnyCell)`）、Kotlin では `sealed interface`（`Text` / `Custom`）として定義する。

**理由**:
- 旧 AiForms.Maui.SettingsView のヘッダ／フッタは「文字列のみ」だったが、KsSettingsView ではモダン設定画面で求められる「任意 View（角丸グルーピング外で表示するアイコン付きラベル等）」をヘッダ／フッタにも置けるよう拡張する
- 後続変更提案 `add-cell-types-custom` で実装される CustomCell 機構（任意 SwiftUI View / Composable をセル化する仕組み）を、ヘッダ／フッタ位置にもそのまま再利用できる構造を Core で用意する
- sum type にしておけば「文字列専用のシンプル経路」と「任意 View の柔軟経路」を UI 層で分岐させられ、`UICollectionLayoutListConfiguration` の supplementary header API（文字列）と `UIHostingConfiguration`（任意 SwiftUI）双方の最短経路を選べる
- Phase 6（旧計画ではモダン UI 専用機能とされていた「ヘッダ任意 View」）を待たずに Phase 1 から段階的に提供できる

**代替案**:
- `header: String?` のまま据え置き、UI 層で別 API（`setHeaderRenderer(forSection:)` 等）として外付け：データモデル（Section）と表示モデル（外付け API）が二重化し、DSL や MAUI Bridge での一貫性を失う。
- `header: AnyCell?` に統一（文字列も Cell として扱う）：単純だが、ほぼ全利用者が文字列ヘッダしか使わない実態に対し、文字列 1 個のために Cell 登録が必要となり API コストが高い。

**影響範囲**:
- 本変更提案は実装済みだが**アーカイブ前**のため、実装コード（Swift `Section.swift` / Kotlin `Section.kt`）と既存ユニットテストを sum type ベースに書き直すタスクを `tasks.md` に追加する。
- 後続変更提案 `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-cell-types-custom` は本決定を前提に設計される。

### Decision 5: 論理スタイル型 `KsColor` / `KsFont`

**選択**: 色は `struct KsColor(red: Double, green: Double, blue: Double, alpha: Double)`、フォントは `struct KsFont(family: String?, size: Double, weight: KsFontWeight)` とし、UIKit / Android Color / Compose Color への変換は UI 層の責務とする。

**理由**:
- Core を UIKit / android.graphics に非依存にできる
- MAUI Bridge 層も同じ論理表現を C# 側で構築できる（C# `Microsoft.Maui.Graphics.Color` → `KsColor` 変換は Bridge 境界で実施）

**代替案**:
- 各プラットフォームでネイティブ Color 型を直接持つ：型変換コードは減るが、Core が UIKit / android.graphics 依存になり、テストでも UI フレームワークが必要になる。

## Risks / Trade-offs

- **リスク**: iOS / Android で同型を保とうとしても言語仕様差で乖離しうる（例：iOS は `UUID`、Android は `String`）
  - **緩和策**: ID 型は意図的に各プラットフォーム慣習に合わせ、Bridge 境界で `String` に統一する。Core 仕様としては「一意 ID」のみ要求する。
- **リスク**: `KsColor` / `KsFont` の独自表現が MAUI 側 `Color` / `FontFamily` と微妙に乖離する（HSL vs RGB、フォントウェイト数値表現）
  - **緩和策**: `KsColor` は RGBA Double 0.0–1.0、`KsFontWeight` は `regular`/`medium`/`semibold`/`bold` の列挙型に絞り、Bridge での変換ロジックを 1 箇所に集約。
- **トレードオフ**: 値型のため、深い Cell ツリーの 1 個変更で全体スナップショットが再生成される。ただし DiffableDataSource / ListAdapter の差分検出が O(n) で済むため、N 数千程度では実用上問題ない。

## Migration Plan

新規モジュールのため移行は不要。
