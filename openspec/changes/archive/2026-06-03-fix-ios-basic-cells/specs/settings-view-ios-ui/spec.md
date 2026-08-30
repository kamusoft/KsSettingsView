## MODIFIED Requirements

### Requirement: スタイル切替（クラシック/モダン）

`KsSettingsViewController` は `public var style: KsSettingsViewStyle` プロパティを持たなければならない (SHALL)。`KsSettingsViewStyle` は `.classic`（旧 AiForms 互換のフラットな見た目）と `.modern`（最新 OS 設定画面風の角丸グルーピング）の 2 ケースを持つ enum でなければならない (MUST)。`style` の変更時は内部 `UICollectionView` のレイアウトを再構築しなければならない (MUST)。

classic スタイル（`.plain` Appearance）では、オリジナル `AiForms.Maui.SettingsView` 互換の挙動として、セクションヘッダーをスクロール上端に固定してはならない (MUST NOT)。すなわち header supplementary の `pinToVisibleBounds` は `false` 相当でなければならず、ヘッダーはコンテンツと共にスクロールアウトしなければならない (MUST)。

また、footer（`Section.footer`）を持たないセクションに対して、空のフッター supplementary 領域（グレーの帯）を表示してはならない (MUST NOT)。`UICollectionLayoutListConfiguration.footerMode` は「root 内に footer を持つセクションが 1 つでも存在するか」で決定し、いずれのセクションも footer を持たない場合は `.none` としてフッター領域を生成してはならない (MUST NOT)。

#### Scenario: classic スタイルの Appearance

- **GIVEN** `KsSettingsViewController(style: .classic)` を初期化
- **WHEN** 内部 `UICollectionView` のレイアウト設定を取得する
- **THEN** `UICollectionLayoutListConfiguration.appearance` が `.plain` に設定されている

#### Scenario: classic スタイルでヘッダーが固定されない

- **GIVEN** `KsSettingsViewController(style: .classic)` で複数セクション（各セクションにヘッダーあり）を表示している
- **WHEN** コンテンツを下方向にスクロールする
- **THEN** セクションヘッダーは画面上端に固定されず、コンテンツと共にスクロールアウトする（`pinToVisibleBounds = false` 相当）

#### Scenario: footer を持たないセクションに空フッター帯が出ない

- **GIVEN** `KsSettingsViewController(style: .classic)` で、いずれのセクションも `footer` を持たない `root` を表示している
- **WHEN** レイアウトを構築する
- **THEN** `footerMode` は `.none` となり、各セクション下部に空のフッター supplementary 領域（グレーの帯）が表示されない

#### Scenario: footer を持つセクションがある場合は footer を描画する

- **GIVEN** `KsSettingsViewController(style: .classic)` で、一部のセクションが `footer` を持つ `root` を表示している
- **WHEN** レイアウトを構築する
- **THEN** `footerMode` は `.supplementary` となり、`footer` を持つセクションにのみ意味のあるフッターが描画される（`footer` を持たないセクションには空の帯が現れない）

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
