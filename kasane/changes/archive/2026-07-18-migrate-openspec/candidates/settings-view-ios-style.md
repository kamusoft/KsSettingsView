# Candidate: settings-view-ios-style

## 概念候補

### iOS スタイルの所有境界と解決階層 (提案カテゴリ: styling/)

iOS の見た目に関する値は UI 層が所有し、利用者が UIKit の色・フォント・寸法を変換なしで指定できるようにする。プラットフォーム非依存モデルはこれらを所有せず、構造・内容のモデルと見た目の入力を分離する。

実効値は、原則として「行固有の指定 → 画面全体の指定 → プラットフォーム既定」の順に解決する。行固有の指定がないことは、値が空であることではなく、上位の見た目を継承することを意味する。ボタンのように部品固有の明示値を持つ場合は、その値が行固有スタイルより先に解決される。

画面のキャンバス背景と行の背景は独立した値であり、一方を変えても他方を暗黙に変えない。無効状態では、タイトル・説明・値・ヒントの各テキスト色を共通の無効色に置き換え、選択時のフィードバックも発生させない。

見た目の変更は構造差分とは別の更新経路で扱う。同じセクション／行の同一性や順序を変えず、可視要素を再評価する。

出典:

- コード: `ios/Sources/KsSettingsViewUI/Theme.swift`、`CellStyle.swift`、`EffectiveStyle.swift`、`CellBaseLayout.swift`、`SettingsRootStore.swift`、`KsSettingsViewController.swift`
- テスト: `ios/Tests/KsSettingsViewUITests/EffectiveStyleTests.swift`、`EffectiveStyleResolutionTests.swift`、`ThemeRenameTests.swift`、`SettingsRootStoreTests.swift`、`ApplyDiffTests.swift`
- spec Purpose: `openspec/specs/settings-view-ios-style/spec.md` の Purpose、`Theme 型 (UI 層)`、`CellStyle 型 (UI 層)`、`EffectiveStyle の解決順序`

### iOS 設定行の共通視覚契約 (提案カテゴリ: styling/)

すべての設定行は、左の任意アイコン、中央のタイトルと任意説明、右側の値または操作部品、右上に浮く任意ヒントという共通の視覚文法を持つ。値や操作部品はタイトルと同じ段に並び、説明はその下に置く。任意要素が空なら、そのためだけの空領域を残さない。

行高には次の不変条件がある。

- iOS の最小行高は 48pt とする。
- 既定は可変高さで、48pt または明示された最低高さを守りながら内容に応じて伸びる。
- 固定高さモードでは、内容の自然高にかかわらず解決済みの行高へ揃える。
- 行固有の高さは画面全体の行高より優先し、どちらも最小行高を下回れない。

アイコンは正方形として扱い、既定の一辺は 24pt、既定の角丸は 0pt とする。ヒントは trailing 操作部品の有無に左右されず行右端を基準に配置する。

出典:

- コード: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`、`KsListCellBase.swift`、`KsCellViewSupport.swift`、各 `*CellView.swift`
- テスト: `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift`、`KsCellViewSupportTests.swift`、`EffectiveStyleTests.swift`、`BasicCellsTests.swift`、`InputCellsTests.swift`
- spec Purpose: `openspec/specs/settings-view-ios-style/spec.md` の Purpose、`UICollectionView のレイアウト`、`Cell.cellHeight の UI 反映`

### iOS リスト外観と補助領域の視覚規則 (提案カテゴリ: styling/)

iOS の設定リストは、フラットな classic と角丸グルーピングの modern という二つの外観を提供する。外観の変更は表示中のモデルを維持したままレイアウトだけを切り替える。

Section と Root の Header / Footer はコンテンツと共にスクロールし、画面端へ固定しない。空の補助領域は生成しない。Header テキストは次の行へ近づくよう下端に、Footer テキストは前の行へ近づくよう上端に配置し、上下の余白を最小化する。Section 固有の Header 高さが画面全体の既定高さより優先し、いずれも未指定なら内容に応じた高さとする。

セクション境界の罫線は端から端まで描き、セクション内の罫線はアイコンの有無に関係なく左 16pt から描く。リストのキャンバスは透明なセクション背景を通して画面背景を見せ、行自身は独立した背景を保つ。

出典:

- コード: `ios/Sources/KsSettingsViewUI/KsSettingsViewStyle.swift`、`KsSettingsViewController.swift`
- テスト: `ios/Tests/KsSettingsViewUITests/KsSettingsViewStyleTests.swift`、`KsSettingsViewControllerTests.swift`、`SectionAccessoryRenderingTests.swift`、`RootAccessoryRenderingTests.swift`
- spec Purpose: `openspec/specs/settings-view-ios-style/spec.md` の Purpose、`スタイル切替（クラシック/モダン）`、Section / Root H/F、罫線、余白、垂直配置に関する Requirements

## ADR 候補

- プラットフォーム固有のスタイル値を Core に抽象化せず、各 UI 層が Native 型で直接所有する — 出典: `Theme 型 (UI 層)` / `CellStyle 型 (UI 層)` の「中間論理表現を経由してはならない」、選別基準: 能力・コンポーネント境界を越えて影響する、将来の決定を制約する
- Theme 更新を構造差分のケースに含めず、専用の再評価経路で反映する — 出典: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift`、`KsSettingsViewController.swift`、`SettingsRootStoreTests.swift`、`ApplyDiffTests.swift`、補助資料 `docs/styling-and-theming.md` §12、選別基準: 能力・コンポーネント境界を越えて影響する、将来の決定を制約する
- Root Header / Footer を Core の Root 値に含めず、UI 層の装飾入力として扱う — 出典: `Root H/F（SettingsRoot.header / footer）の描画` の `MUST NOT`、選別基準: 能力・コンポーネント境界を越えて影響する、将来の決定を制約する
- 全 Cell の共通行レイアウトをシステム既定の List content / accessory 構成に委ねず、自前の共通レイアウト契約で統一する — 出典: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`、`KsListCellBase.swift`、`ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift`。ただし現行 spec に旧経路が残るため、drift 解消後に採否を確定する。選別基準: 能力・コンポーネント境界を越えて影響する、将来の決定を制約する
- Footer の既定文字色を dynamic color にせず固定グレーとする — 出典: `Section Footer の文字色フォールバック` の `MUST NOT` と Rationale、選別基準: 将来の決定を制約する

## drift 所見

- [境界・確定] Purpose は「Native 値への変換責務を `settings-view-ios-theme-bridge` に分離し、本 capability は変換結果を消費する」とする一方、同じ spec の後半は Theme / CellStyle / EffectiveStyle 自体を本 capability の Requirements として規定し、実装も `KsSettingsViewUI` 内で直接 Native 値を保持・解決している。capability 間の所有境界が spec 内で二重化している (`openspec/specs/settings-view-ios-style/spec.md` / `ios/Sources/KsSettingsViewUI/Theme.swift`、`CellStyle.swift`、`EffectiveStyle.swift`)
- [挙動・確定] spec は description 色の未指定時をライトモード固定 RGB と記すが、実装とテストは dynamic color の `UIColor.secondaryLabel` を採用している (`openspec/specs/settings-view-ios-style/spec.md` の `Theme 型 (UI 層)` / `ios/Sources/KsSettingsViewUI/Theme.swift`、`EffectiveStyle.swift` / `ios/Tests/KsSettingsViewUITests/EffectiveStyleResolutionTests.swift`)
- [描画経路・確定] `LabelCell の description と valueText の並列描画` は `UIListContentConfiguration` と accessory を前提に記述されているが、現実は全 Cell 共通の自前 stack レイアウトで、content configuration と accessories を空にすることまでテストされている (`openspec/specs/settings-view-ios-style/spec.md` / `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`、`KsListCellBase.swift` / `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift`)
- [描画経路・確定] 補助資料は各 Cell View が個別の `accessories` を組むと説明するが、実装は trailing view を共通 content stack に並べる (`docs/styling-and-theming.md` §14 / `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`、各 `*CellView.swift`)
- [レイアウト構成・確定] spec は footer のない root では list configuration の `footerMode = .none` とするが、production のレイアウト構築は header/footer mode を常に `.supplementary` に固定し、Section ごとの boundary item 間引きで同じ可視結果を作る。spec とテスト対象の補助関数は旧構成を保持している (`openspec/specs/settings-view-ios-style/spec.md` の `スタイル切替` / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の production `makeLayout` と未使用の `makeListConfig` / `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift`)
- [見た目・確定] spec は区切り線へ Theme の separator 色を適用するとするが、production の separator 構成は可視性と inset だけを変更し、色を設定していない。コードベース内に当該色の描画反映箇所も見当たらない (`openspec/specs/settings-view-ios-style/spec.md` の `UICollectionView のレイアウト` / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の separator 構成)
- [対象範囲・確定] spec の共通基底 Requirement は基本 Cell 7 種だけを「全 Cell View」として列挙するが、現在は入力系 5 種も同じ共通基底と共通行レイアウトを使い、合計 12 種が対象である (`openspec/specs/settings-view-ios-style/spec.md` の `Cell View 共通基底クラスの導入` / `ios/Sources/KsSettingsViewUI/*CellView.swift`)
- [Theme 再適用・要確認] Theme の直接適用はリスト背景更新と可視 Cell の再構成だけを行い、表示中の Section / Root Header・Footer を再構成しない。Header / Footer の色・フォントを含む Theme 変更が、その場で全可視領域へ反映されるという利用者期待とずれる可能性がある (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の Theme 適用と supplementary 描画 / `docs/styling-and-theming.md` §12)
- [未消費フィールド・要確認] Theme は scroll indicator 表示、Header / Footer 背景色を公開するが、対応する描画コードでの消費が確認できない。現行 spec はフィールド保持を要求する一方、描画反映 Scenario を欠くため、未実装か意図的な予約値か判断できない (`openspec/specs/settings-view-ios-style/spec.md` の `Theme 型 (UI 層)` / `ios/Sources/KsSettingsViewUI/Theme.swift`、`KsSettingsViewController.swift`)

## 用語

- Theme: 設定画面全体に適用する見た目の既定値。構造モデルとは別に UI 層が所有する。
- CellStyle: 一つの設定行に限定した部分的な見た目指定。未指定値は Theme を継承する。
- 実効スタイル: 行固有指定、画面全体指定、プラットフォーム既定を優先順に合成した、描画時点の完全な値集合。
- classic: フラットなリストと罫線を中心とする、旧 AiForms 互換の外観。
- modern: 角丸グルーピングと外側余白を持つ、現行 iOS 設定画面風の外観。
- 可変高さモード: 最低高さを保証しつつ、内容の自然高まで行を伸ばすモード。
- 固定高さモード: 内容の自然高にかかわらず、解決済み行高へ揃えるモード。
- Section H/F: 各セクションの直前・直後に置く Header / Footer。空なら領域自体を持たない。
- Root H/F: リスト全体の先頭・末尾に置く UI 層の Header / Footer。
- sticky: スクロールしても Header / Footer を画面端へ固定する挙動。本 capability では採用しない。
- trailing view: タイトルと同じ段の右側に並ぶ値表示または操作部品。
- hint: trailing 部品とは独立し、行右上に浮いて表示される短い補助テキスト。

## 抽出メモ

- 独立概念は 3 候補とした。スタイルの所有・解決、共通設定行、リスト外観・補助領域は寿命と利用者の関心が異なるため、1 枚へ過密に統合しない方がよい。
- `settings-view-ios-theme-bridge` と本 capability は現行 spec 上で責務が重複する。統合側で、Native 値の所有・解決を `styling/` の共通概念に置き、iOS 固有の描画反映だけを `platforms/` に分ける案を検討できる。ここでは統合判断は行わない。
- 共通行のフィールド自体は `cell-types-basic`、共通行の視覚配置と高さは本 capability に隣接する。モデル契約と描画契約を同じ概念へ混ぜない方が drift 検証しやすい。
- Theme 更新を構造差分から分離する原則は `settings-view-ios-host` および全体の状態同期概念にも接続する。ADR 化する場合は iOS 単独判断かプラットフォーム共通判断かを統合側で確認する必要がある。
- Footer 固定色の ADR 候補はダークモード対応を明示的に制約する。将来のデザイン判断に直結するため、単なる既定値一覧へ埋めない方がよい。
- exact API、関数名、具象 Cell の列挙、Auto Layout の制約実装は高腐食情報として概念候補本文から除外し、出典と drift の照合材料にのみ残した。
