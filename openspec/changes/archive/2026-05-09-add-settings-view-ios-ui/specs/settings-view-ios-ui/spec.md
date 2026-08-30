## ADDED Requirements

### Requirement: KsSettingsViewController の公開 API

`KsSettingsViewController` は `UIViewController` を継承し、`root: SettingsRoot` プロパティの設定で内部 `UICollectionView` のスナップショットを更新しなければならない (SHALL)。本コントローラは UIKit 利用者および MAUI バインディング、SwiftUI ラッパから直接利用される (MUST)。

#### Scenario: ルートの設定で表示が更新

- **GIVEN** 既存の `KsSettingsViewController` インスタンスが画面表示されている
- **WHEN** `controller.root = newRoot` を代入する
- **THEN** `UICollectionViewDiffableDataSource` の `apply(snapshot)` が呼ばれ、Section 数とセル数が `newRoot` の内容と一致する

#### Scenario: 初期化直後の状態

- **GIVEN** `KsSettingsViewController()` を初期化した直後
- **WHEN** `viewDidLoad()` 完了時点を確認する
- **THEN** 内部 `UICollectionView` および空 `SettingsRoot()` 相当のスナップショットが構成され、エラーなく `present` できる

### Requirement: UICollectionView のレイアウト

UI は `UICollectionLayoutListConfiguration`（iOS 14+）を `UICollectionViewCompositionalLayout` の `.list` で構成しなければならない (SHALL)。Cell の高さは `estimatedItemSize = .automatic` で Auto Layout により決定されなければならない (MUST)。

#### Scenario: List 設定の使用

- **GIVEN** `KsSettingsViewController` が初期化済み
- **WHEN** `view.subviews` に含まれる `UICollectionView` のレイアウトを取得する
- **THEN** 取得したレイアウトは `UICollectionViewCompositionalLayout` であり、内部設定は List ベースである

#### Scenario: 区切り線とヘッダ・フッタ

- **GIVEN** `Section` に `header` が `SectionAccessory.text("一般")` で指定されている
- **WHEN** Cell が描画される
- **THEN** `UICollectionLayoutListConfiguration.headerMode = .supplementary` 等を用いてヘッダ領域に "一般" が表示され、`Theme.separatorColor` で区切り線色が設定される

### Requirement: スタイル切替（クラシック/モダン）

`KsSettingsViewController` は `public var style: KsSettingsViewStyle` プロパティを持たなければならない (SHALL)。`KsSettingsViewStyle` は `.classic`（旧 AiForms 互換のフラットな見た目）と `.modern`（最新 OS 設定画面風の角丸グルーピング）の 2 ケースを持つ enum でなければならない (MUST)。`style` の変更時は内部 `UICollectionView` のレイアウトを再構築しなければならない (MUST)。

#### Scenario: classic スタイルの Appearance

- **GIVEN** `KsSettingsViewController(style: .classic)` を初期化
- **WHEN** 内部 `UICollectionView` のレイアウト設定を取得する
- **THEN** `UICollectionLayoutListConfiguration.appearance` が `.plain` に設定されている

#### Scenario: modern スタイルの Appearance

- **GIVEN** `KsSettingsViewController(style: .modern)` を初期化
- **WHEN** 内部 `UICollectionView` のレイアウト設定を取得する
- **THEN** `UICollectionLayoutListConfiguration.appearance` が `.insetGrouped` に設定されている

#### Scenario: 動的なスタイル切替

- **GIVEN** `KsSettingsViewController(style: .classic)` が画面表示中
- **WHEN** `controller.style = .modern` を代入する
- **THEN** 内部レイアウトが `.insetGrouped` ベースで再構築され、既存の `root` スナップショットがそのまま再描画される（差分アニメーションは発生しない）

#### Scenario: SwiftUI ラッパでのスタイル指定

- **GIVEN** SwiftUI で `KsSettingsView(root: $root, style: .modern)` を記述
- **WHEN** `makeUIViewController(context:)` が呼ばれる
- **THEN** 生成された `KsSettingsViewController` の `style` が `.modern` で初期化される

### Requirement: Section H/F（SectionAccessory）の描画

`KsSettingsViewController` は `SectionAccessory.text(String)` 形式のヘッダ／フッタを `UICollectionLayoutListConfiguration` の supplementary header / footer として文字列で描画しなければならない (SHALL)。`SectionAccessory.view(KsAnyView)` 形式は `UICollectionViewListCell.contentConfiguration` を `UIHostingConfiguration { ... }`（SwiftUI backing）または `addSubview`（UIView backing）で構成して描画しなければならない (MUST)。

#### Scenario: text 形式ヘッダの描画

- **GIVEN** `Section(header: .text("一般"), ...)` を含む `SettingsRoot`
- **WHEN** `controller.root` に代入する
- **THEN** ヘッダ supplementary view にテキスト "一般" が描画される

#### Scenario: view 形式ヘッダ（SwiftUI backing）の描画

- **GIVEN** `Section(header: .view(KsAnyView.swiftUI { ProfileCardView() }), ...)` を含む `SettingsRoot`
- **WHEN** `controller.root` に代入する
- **THEN** ヘッダ supplementary view の `contentConfiguration` が `UIHostingConfiguration` で構成され、`ProfileCardView` の中身が描画される

#### Scenario: view 形式ヘッダ（UIView backing）の描画

- **GIVEN** `Section(header: .view(KsAnyView.uiKit { MyCustomUIView() }), ...)` を含む `SettingsRoot`
- **WHEN** `controller.root` に代入する
- **THEN** ヘッダ supplementary view に `MyCustomUIView` インスタンスが addSubview され、可視描画される

#### Scenario: view 形式ヘッダの中身更新（差分検出非対応）

- **GIVEN** `controller.root` に `.view(KsAnyView.swiftUI { Counter(value: 1) })` を含む root を代入
- **WHEN** `.view(KsAnyView.swiftUI { Counter(value: 2) })` を含む root に置き換える
- **THEN** `KsAnyView` は差分検出に参加しないため supplementary view 自体の生成・破棄は走らないが、`contentConfiguration` の再構成によって `Counter(value: 2)` の中身が再描画される

### Requirement: Root H/F（SettingsRoot.header / footer）の描画

`KsSettingsViewController` は `SettingsRoot.header` / `SettingsRoot.footer`（`RootAccessory?`）を `UICollectionViewCompositionalLayout.configuration.boundarySupplementaryItems` に `elementKind: "ks-root-header"` / `"ks-root-footer"`、`alignment: .top` / `.bottom` で配置しなければならない (SHALL)。デフォルトで `pinToVisibleBounds = false`（スクロール追従）でなければならない (MUST)。`RootAccessory.text` ケースは `UIListContentConfiguration` ベースの文字列描画、`RootAccessory.view(KsAnyView)` ケースは Section H/F と同じ `UIHostingConfiguration` または `addSubview` で描画しなければならない (MUST)。`header` / `footer` が `nil` の場合は対応する supplementary item を boundary から省略しなければならない (MUST)。

#### Scenario: Root Header（text）の描画

- **GIVEN** `SettingsRoot(header: .text("プロフィール"), ...)`
- **WHEN** `controller.root` に代入する
- **THEN** UICollectionView 上端に "プロフィール" の boundary supplementary view が表示される

#### Scenario: Root Footer（view、SwiftUI backing）の描画

- **GIVEN** `SettingsRoot(footer: .view(KsAnyView.swiftUI { Text("v1.0.0") }), ...)`
- **WHEN** `controller.root` に代入する
- **THEN** UICollectionView 下端に Text("v1.0.0") の boundary supplementary view が描画される

#### Scenario: Root H/F のスクロール追従

- **GIVEN** Root Header を持つ `SettingsRoot` が描画中
- **WHEN** UICollectionView を下方向にスクロールする
- **THEN** Root Header は画面上端に固定されず、コンテンツと共にスクロールアウトする（`pinToVisibleBounds = false` のデフォルト挙動）

#### Scenario: Root H/F が nil の場合

- **GIVEN** `SettingsRoot(header: nil, footer: nil, ...)`
- **WHEN** `controller.root` に代入する
- **THEN** boundary supplementary items は配置されず、既存の sections のみが描画される

#### Scenario: Root Header の中身更新（差分検出非対応）

- **GIVEN** `controller.root.header = .view(KsAnyView.swiftUI { ... })` で描画中
- **WHEN** 同じスロットに別の `KsAnyView` を持つ root を代入する
- **THEN** boundary supplementary view 自体の生成・破棄は走らないが、`contentConfiguration` の再構成によって新しい中身が描画される

### Requirement: DiffableDataSource

`KsSettingsViewController` は内部で `UICollectionViewDiffableDataSource<Section.ID, KsCellID>` を保持しなければならない (SHALL)（`KsCellID` は Cell を一意に識別する Hashable な値型。`KsCell.id` 単独、または Cell の Hashable な内容ハッシュなど、実装で決定）。スナップショット差分は `Hashable` の等価性で算出されなければならない (MUST)。装飾領域（Section H/F、Root H/F）の `KsAnyView` は差分検出に参加せず、`SettingsRoot` / `Section` 等の `Hashable` 実装は `view` ケースの中身を判定対象外として扱わなければならない (MUST)。

#### Scenario: 同一フィールドのスナップショットは差分なし

- **GIVEN** `controller.root` に同一内容の `SettingsRoot` を 2 回連続で代入
- **WHEN** スナップショット適用を観察する
- **THEN** 2 回目はアニメーション付き挿入・削除が発生しない（DiffableDataSource が等価と判定）

#### Scenario: Cell 追加時のアニメーション

- **GIVEN** `controller.root` に既存の root が設定されている
- **WHEN** 末尾 Section に新しい Cell を追加した root を代入する
- **THEN** 新しい Cell 行のみが挿入アニメーションで追加される

### Requirement: Cell レジストリ

`KsCellRegistry` は具象 Cell 型から `UICollectionViewCell` サブクラスへの解決を担う中央レジストリでなければならない (SHALL)。`KsCellRenderer` プロトコルを実装する `UICollectionViewCell` サブクラスをアプリ起動時に登録できなければならない (MUST)。

#### Scenario: Cell 型の登録と解決

- **GIVEN** `KsCellRegistry` が初期化済み
- **WHEN** `registry.register(cellType: MyCell.self, rendererType: MyCellView.self)` を呼ぶ
- **THEN** 以後 `MyCell` を含む snapshot 適用時に `MyCellView` が `dequeueReusableCell` され、`KsCellRenderer.render(cell:theme:)` が呼ばれる

#### Scenario: 未登録 Cell の扱い

- **GIVEN** `KsCellRegistry` に未登録の Cell が渡される
- **WHEN** スナップショット適用を試みる
- **THEN** 開発時は assertion failure（DEBUG ビルドのみ）、リリース時はプレースホルダ Cell（背景色違いの空セル）を返してアプリクラッシュを防ぐ

### Requirement: KsCellRenderer プロトコル

`KsCellRenderer` は具象 `UICollectionViewCell` サブクラスが実装すべきプロトコルでなければならない (SHALL)。任意の `KsCell` 準拠の Cell と `Theme` を受け取って描画する `render(cell:theme:)` 形式の関数（associatedtype 経由 / 型消去経由のいずれかは実装で決定）を要求しなければならない (MUST)。

#### Scenario: render の呼び出し

- **GIVEN** `KsCellRenderer` 準拠の `UICollectionViewCell` サブクラス
- **WHEN** DataSource が当該 Cell を `dequeueReusableCell` し snapshot 適用する
- **THEN** Cell ごとの `render` 関数が呼ばれ、Cell が描画される

#### Scenario: prepareForReuse でのクリーンアップ

- **GIVEN** `KsCellRenderer` 準拠 Cell が一度 render された後再利用される
- **WHEN** `prepareForReuse()` が UIKit から呼ばれる
- **THEN** Cell 内のサブビュー・テキスト・画像参照がリセットされ、再 `render` 時に古い状態が表示されない

### Requirement: Theme / CellStyle の UIKit 変換

`Theme` および `CellStyle` の論理スタイルを `UIColor` および `UIFont` に変換するユーティリティが提供されなければならない (SHALL)。変換は `KsSettingsViewUI` モジュールの内部または公開ユーティリティで行わなければならない (MUST)。

#### Scenario: KsColor から UIColor

- **GIVEN** `KsColor(red: 1.0, green: 0.5, blue: 0.0, alpha: 1.0)`
- **WHEN** `UIColor(ksColor:)` イニシャライザを呼ぶ
- **THEN** `UIColor` の RGBA が `(1.0, 0.5, 0.0, 1.0)` と一致する

#### Scenario: 実効スタイルの合成

- **GIVEN** Cell の `CellStyle.titleColor = nil`、`Theme` のデフォルト titleColor が指定されている
- **WHEN** 描画用に「実効スタイル」を計算する
- **THEN** `CellStyle.titleColor` の代わりに `Theme` のデフォルト値が使われる

### Requirement: SwiftUI ラッパ KsSettingsView

`KsSettingsView` は `UIViewControllerRepresentable` に準拠し、SwiftUI から `KsSettingsViewController` を直接利用できなければならない (SHALL)。`@Binding<SettingsRoot>` を受け取り、変更を内部 ViewController に反映しなければならない (MUST)。

#### Scenario: 初回作成

- **GIVEN** SwiftUI View 内で `KsSettingsView(root: $root)` を記述
- **WHEN** SwiftUI が `makeUIViewController(context:)` を呼ぶ
- **THEN** 新規 `KsSettingsViewController` が生成され、`controller.root` に `root.wrappedValue` が代入される

#### Scenario: バインディング更新

- **GIVEN** `KsSettingsView(root: $root)` が画面表示中
- **WHEN** `root.wrappedValue` を SwiftUI 側で更新する
- **THEN** `updateUIViewController(_:context:)` が呼ばれ、`controller.root` が新しい値に更新される

### Requirement: SwiftUI DSL

宣言的 DSL（`@resultBuilder` を用いた `SettingsRootBuilder`、`SectionBuilder`）を提供し、SwiftUI 内で Cell ツリーを構築できなければならない (SHALL)。DSL は `SettingsRoot` を生成する純粋関数として動作しなければならない (MUST)。

#### Scenario: DSL から SettingsRoot 構築

- **GIVEN** SwiftUI コード内で
  ```swift
  let root = SettingsRoot {
      Section("一般") { /* PoCLabelCell(title: "...") など */ }
  }
  ```
  と記述
- **WHEN** `root` を評価する
- **THEN** `SettingsRoot.sections` に 1 つの `Section` が含まれ、その `cells` に DSL で記述された Cell が並ぶ

### Requirement: メモリリーク防止

`KsSettingsViewController` および `KsSettingsView` は `deinit` 時に内部 `UICollectionView` の DataSource、Delegate、registered Cell の参照をすべて解放しなければならない (MUST)。SwiftUI ラッパのバインディングが ViewController を強参照し続けないこと。

#### Scenario: ViewController が deinit される

- **GIVEN** `KsSettingsViewController` を `present` したのち `dismiss` する
- **WHEN** 親 ViewController から開放後 1 ランループ以上経過する
- **THEN** `KsSettingsViewController` インスタンスは deinit され、`weak var` で保持していた参照が `nil` になる

### Requirement: PoC Cell の存在

`KsSettingsViewUI` モジュールは PoC 用の最小 Cell（`PoCLabelCell`：id・title のみ）を内部に持ち、ユニットテストおよびサンプル動作確認で使用しなければならない (SHALL)。具象 Cell が追加された段階で削除されなければならない (MUST)。

#### Scenario: PoCLabelCell の表示

- **GIVEN** `SettingsRoot` 内に `PoCLabelCell(title: "Hello")` を含む `Section` が 1 つ
- **WHEN** `KsSettingsViewController.root` に代入する
- **THEN** UICollectionView 内に 1 行のセルが描画され、テキストに "Hello" が表示される
