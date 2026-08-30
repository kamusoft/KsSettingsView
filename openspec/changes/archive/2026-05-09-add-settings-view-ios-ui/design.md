## 参考実装

本変更提案の実装に着手する**前に必ず**以下を確認すること。

- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元の仕様要約
  - **必読セクション**: §6（iOS Native 実装の特徴）、§9（NativeCollectionView から引き継ぐパターン）、§8（メモリリーク対策）
- 原典コード：
  - [`../AiForms.Maui.SettingsView/SettingsView/Handlers/SettingsViewHandler.iOS.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Handlers/SettingsViewHandler.iOS.cs) — UITableView ベースの旧実装（KsSettingsView では UICollectionView に置き換え）
  - [`../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/`](file://../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/) — `UICollectionViewDiffableDataSource`、Cell 再利用、`PrepareForReuse`、`CancellationTokenSource` パターンの実例
  - [`../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/AiCollectionView.cs`](file://../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/AiCollectionView.cs) — UICollectionView ラッパの実装パターン

**重要**: 旧 AiForms.Maui.SettingsView は UITableView を採用していたが、KsSettingsView は **UICollectionView + DiffableDataSource + UICollectionLayoutListConfiguration** に刷新する。挙動の互換性は要件としないが、Section ヘッダ・Footer・Cell 高さ計算（旧版は AutomaticDimension）の UX は近づける。

## Context

iOS の設定画面 UI を `UICollectionView + DiffableDataSource` で構築する。これにより旧 `AiForms.Maui.SettingsView`（UITableView）よりモダンな実装に刷新する。`UICollectionLayoutListConfiguration`（iOS 14+）は UITableView 風の List 表示を最小コードで実現でき、最低 iOS 16 を前提とすれば `UIHostingConfiguration` も含むモダン API 一式を活用できる。SwiftUI 利用者にも自然に届けるため、`UIViewControllerRepresentable` ラッパと `@resultBuilder` DSL を Phase 1 で同梱する。本変更提案は Cell 1 種（PoC）が表示できるところまでを範囲とし、各種具象 Cell の追加は別変更提案で対応する。

## Goals / Non-Goals

**Goals:**
- `UICollectionView + UICollectionViewDiffableDataSource + UICollectionLayoutListConfiguration` の統合された UI 基盤
- `KsCellRegistry` による Cell 型の動的登録機構（後続変更で各具象 Cell が登録できる）
- `KsCellRenderer` プロトコルによる Cell 描画契約
- SwiftUI ラッパ `KsSettingsView` と `@resultBuilder` DSL
- メモリリーク防止のための `deinit` ハンドリング
- PoC `PoCLabelCell` での動作確認

**Non-Goals:**
- 具象 Cell（LabelCell、SwitchCell ...）は本変更提案では追加しない
- ドラッグ＆ドロップ並べ替えは本変更提案では扱わない（旧 AiForms にあったが Phase 6 のモダン UI で再検討）
- カスタムセル（任意 SwiftUI View 埋め込み）は `add-cell-types-custom` で扱う
- MAUI バインディング層は `add-maui-bindings` で扱う

## Decisions

### Decision 1: UICollectionLayoutListConfiguration の採用

**選択**: `UICollectionViewCompositionalLayout.list(using:)` で `UICollectionLayoutListConfiguration` を構成する。

**理由**:
- iOS 14+ で利用可能、最低 iOS 16 のため問題なし
- UITableView 互換の見た目（区切り線、ヘッダ／フッタ supplementary view）を最小コードで実現
- `estimatedItemSize = .automatic` と Auto Layout で高さ自動計算が標準サポート

**代替案**:
- 手動 `UICollectionViewFlowLayout`：自由度は高いが区切り線・ヘッダの実装をすべて自前で書く必要がある。
- 純粋 UITableView：旧 AiForms と同じだが、本リニューアルの目的（モダン実装への刷新）と矛盾する。

### Decision 2: DiffableDataSource のセクション ID と項目 ID

**選択**: `UICollectionViewDiffableDataSource<Section.ID, AnyCell>` とする。`Section.ID` は `UUID`、`AnyCell` 自体が `Hashable`。

**理由**:
- `Section` 全体を ID として渡すと、ヘッダ／フッタ変更でセクションごと削除・追加扱いになり過剰なアニメーションが起きる
- `Section.ID`（UUID 単独）にすればセクション同一性は ID で判定、内部 cells の差分は項目 ID（`AnyCell` 全体の Hashable）で判定される
- `AnyCell` を項目 ID として使えば、Cell の任意フィールド変更で自動的に「内容変更」と判定される

**代替案**:
- 項目 ID を `UUID` 単独にする：内容変更時に内部 Cell 状態を取得する別 API が必要。`AnyCell` の Hashable 採用が単純。

### Decision 3: KsCellRegistry の中央集権化

**選択**: `KsCellRegistry` シングルトン（または ViewController ローカル）に Cell 型と Renderer 型のペアを登録し、DataSource の cell provider は registry から解決する。

**理由**:
- 後続 `add-cell-types-*` 各変更提案が独立して新 Cell を追加できる
- MAUI バインディング層も Bridge 経由で同じ registry を呼び出せる
- 未登録 Cell は assertion failure にすることで開発時に登録漏れを早期検出

**代替案**:
- DataSource 内に `switch` で Cell 型を直接判定：Cell 種類が増えるたび DataSource を編集する必要があり、独立した変更提案で扱いづらい。

### Decision 4: SwiftUI ラッパは UIViewControllerRepresentable

**選択**: `KsSettingsView: UIViewControllerRepresentable`。

**理由**:
- `KsSettingsViewController` を直接ラップでき、内部 collection view ライフサイクルを UIKit に任せられる
- `@Binding<SettingsRoot>` で SwiftUI ↔ UIViewController 間の状態同期がシンプル
- `UIViewRepresentable` だと自前で UICollectionView ライフサイクル管理する必要があり煩雑

**代替案**:
- `UIViewRepresentable`：軽量だが本ケースでは無理がある。

### Decision 5: @resultBuilder DSL の同梱

**選択**: Phase 1 から `@resultBuilder` ベースの `SettingsRootBuilder` を提供する（プラン更新で確定）。

**理由**:
- SwiftUI 利用者の期待に沿う宣言的 API
- コンパイル時に型安全
- DSL は薄く、内部で `SettingsRoot` を返すため UIKit 利用者と互換

**代替案**:
- DSL を Phase 2 以降に遅らせる：MAUI バインディング側でも DSL 構築は不要なため遅延可能だが、ユーザー要望（プラン質問で確定）により本フェーズで提供。

### Decision 5b: クラシック/モダンのスタイル切替を Phase 1 で同梱

**選択**: `KsSettingsViewController` に `public var style: KsSettingsViewStyle` を持たせ、`UICollectionLayoutListConfiguration.Appearance` を `.plain`（classic）/ `.insetGrouped`（modern）で切り替える。setter で `UICollectionView` のレイアウトを再構築する。SwiftUI ラッパ `KsSettingsView` も同 enum をイニシャライザ引数で受け取る。

**理由**:
- 「クラシック」と「モダン」の違いは描画基盤（UICollectionView）ではなく**見た目（Appearance）**であることが探索で確定した（MAUI バインディング可能性のため Native View ベースは双方共通）
- iOS は `UICollectionLayoutListConfiguration.Appearance` に `.plain` / `.insetGrouped` 等が標準で用意されており、Appearance 切替だけでフラットと角丸グルーピングを切り替えられる
- 旧計画では「Phase 6 でモダン UI として再検討」だったが、実装コストが極めて低いため Phase 1 から同梱して両方をサンプルで検証可能にする
- Phase 1 完成段階で利用者がスタイル選択できる方が、Cell 拡張（`add-cell-types-*`）の開発・検証においても両見た目で確認できる

**代替案**:
- Phase 1 はクラシックのみ実装、モダンは後続変更提案：実装コストは小さいので分割するメリットが薄い。
- Theme に Appearance を含める：見た目スタイルは「Theme（色・フォント）」と直交する概念のため別プロパティの方が利用者にとって直感的。
- 動的切替不可（init 引数のみ）：UICollectionView のレイアウト差し替えは可能なため、setter で変更できる方が DI / テストで便利。

### Decision 5c: Section H/F の `.view(KsAnyView)` ケースを本実装する

**選択**: `Section.header` / `Section.footer` は Core 側で `SectionAccessory?`（`.text(String)` / `.view(KsAnyView)`）として再定義済み（`refactor-accessory-and-root-hf` で確定）。UI 層では `.text` ケースを `UICollectionLayoutListConfiguration` の supplementary header（文字列）で描画し、`.view(KsAnyView)` ケースは `UICollectionViewListCell.contentConfiguration` を `UIHostingConfiguration { ... }`（SwiftUI backing）または `addSubview`（UIView backing）で構成して描画する。本変更提案で両ケースとも完成形を提供する。

**理由**:
- `KsAnyView` は装飾領域専用の型消去ラッパであり、Cell 概念と独立しているため、`add-cell-types-custom` の `CustomCell` 実装と分離して扱える（Cell 概念排除の方針 → `refactor-accessory-and-root-hf` Decision 1）
- iOS 16+ の `UIHostingConfiguration` を採用することで `UIHostingController` の手動親付けが不要となり、Cell 再利用時の lifecycle が安全
- `.view` ケースは中身の差分検出に参加しないため（`KsAnyView` は `Hashable` 非準拠）、bind 時に毎回 `contentConfiguration` を再構成して中身更新を吸収する

**代替案**:
- Phase 1 では `.view` を空 supplementary でフォールバックし `add-cell-types-custom` で本実装：Core 型の受け入れ口だけを残す中途半端な状態となり、ユーザ視点で機能不全。Cell 概念排除（`KsAnyView` の独立化）により本実装の責務切り出しが可能になったため不採用。
- `UIHostingController` を手動で親付けする：lifecycle 管理が複雑。`UIHostingConfiguration` で代替可能。

### Decision 5d: Root H/F は global boundarySupplementaryItem で実装する

**選択**: `SettingsRoot.header` / `footer` の Root ヘッダ／フッタは `UICollectionViewCompositionalLayout.configuration.boundarySupplementaryItems` に `elementKind: "ks-root-header"` / `"ks-root-footer"`、`alignment: .top` / `.bottom` の `NSCollectionLayoutBoundarySupplementaryItem` を 1 つずつ追加して描画する。`pinToVisibleBounds = false`（スクロール追従、デフォルト）。supplementary view は `UICollectionViewListCell` を流用し、`contentConfiguration` を `UIHostingConfiguration`（`.view` ケース、SwiftUI backing）または `addSubview`（`.view` ケース、UIView backing）または `UIListContentConfiguration`（`.text` ケース）で構成する。

**理由**:
- `UICollectionView` 単一の中で完結する。外側に `UIStackView` で挟むより SwiftUI / UIKit 利用者双方にとって自然
- スクロールに乗る挙動が `AiForms.Maui.NativeCollectionView` の `HeaderView` / `FooterView` と一致
- Section の supplementary header / footer と同じ機構を使えるため、`UICollectionViewListCell` + `UIHostingConfiguration` のパターンを再利用できる
- `pinToVisibleBounds` を将来オプション化する余地がある（Open Question）

**代替案**:
- 外側 `UIStackView` ラップ：スクロール追従が崩れる。不採用。
- Section header / footer に擬似的に詰め込む：Section と Root の概念が混ざる。不採用。
- `pinToVisibleBounds = true` をデフォルト：AiForms 互換性とスクロール体験を優先し false をデフォルトに。

### Decision 6: PoCLabelCell を内部に持つ

**選択**: `KsSettingsViewUI` モジュール内部に `internal struct PoCLabelCell: KsCell` と `internal final class PoCLabelCellView: UICollectionViewCell, KsCellRenderer` を配置し、登録は ViewController 初期化時に自動で行う。

**理由**:
- `add-settings-view-ios-ui` のテスト・サンプル動作確認に必要
- 後続 `add-cell-types-basic` で `LabelCell`（`public`）が追加されたら本 PoC は削除する

**代替案**:
- `add-cell-types-basic` 完了まで本 capability の動作確認をスキップ：依存関係が複雑になるため不採用。

## Risks / Trade-offs

- **リスク**: `estimatedItemSize = .automatic` は最初の数セル描画時にコストが発生する
  - **緩和策**: `UICollectionLayoutListConfiguration.itemSize` を `.estimated(50)` などのデフォルト推定値で初期化し、初期描画コストを抑制。
- **リスク**: `KsCellRegistry` をシングルトンにするとテスト時の状態リセットが面倒
  - **緩和策**: registry を `KsSettingsViewController` の init 引数として受け取れるオプションを用意（DI）。デフォルトはシングルトン。
- **リスク**: SwiftUI バインディングが循環参照を作りやすい（`updateUIViewController` 内で `root.wrappedValue` を再代入）
  - **緩和策**: `Coordinator` で前回の root をキャッシュし、等価判定で no-op 化する。
- **トレードオフ**: 旧 AiForms にあった「ドラッグ＆ドロップ並べ替え」は本変更提案では実装しない。Phase 6 で再検討。

## Open Questions

（解消済み）
- ~~ヘッダ／フッタに任意 SwiftUI View を渡せる API は Phase 1 で提供するか？~~ → **Decision 5c（更新版）で解消**。Core 側の `SectionAccessory` を `.text(String)` / `.view(KsAnyView)` に再定義（`refactor-accessory-and-root-hf` で確定）し、UI 層は本変更提案で `.text` / `.view` の両ケースを本実装する。`KsAnyView` は Cell 概念と独立した装飾領域専用ラッパであるため、`add-cell-types-custom` の CustomCell 実装と分離して扱える。
- ~~モダン UI は Phase 6 で再検討~~ → **Decision 5b で解消**。クラシック/モダンは描画基盤の差ではなく Appearance（`.plain` / `.insetGrouped`）の差であることが確定したため、Phase 1 から `style` プロパティで両対応する。

（残課題）
- Root H/F の `pinToVisibleBounds` をオプション化する API の形（`SettingsRoot.headerPinned: Bool?` / `KsSettingsViewStyle` 拡張等）は本変更提案では扱わず、後続改善で検討する。
