# Batch B 統合結果

## 統合方針

`settings-view-ios-host`、`settings-view-ios-swiftui`、`settings-view-ios-style`、`settings-view-ios-theme-bridge` の抽出候補を、iOS 利用者が単独で読める2つの platform 概念へ統合した。Android の材料がない段階で platform 共通原則を確定しないため、architecture / styling 候補は Batch D へ繰り越す。

| 統合ドラフト | 元候補 | 判断 |
|---|---|---|
| `platforms/ios-native-host.md` | Native Host、Store、visible projection、Renderer Registry、iOS list appearance / visual state | UIKit から組み込む利用者が必要な公開 API と制約を一つの入口へまとめる |
| `platforms/ios-swiftui.md` | SwiftUI Bridge、DSL、identity、Cell / Section modifier、Theme 伝播 | SwiftUI 利用者が方式選択から identity・style 適用まで一続きで理解できるようまとめる |

スタイルの全フィールド一覧や UIKit の内部 layout 手順は移行しない。利用者が選択と挙動を予測するために必要な解決順、背景領域、行高、無効・選択状態だけを iOS Native Host に残した。

## Batch D へ繰り越す横断統合材料

次は iOS 固有 API ではなく、Android Host / Compose / style と比較して初めて共通原則を確定できる。Batch C で材料を揃え、Batch D で旧 cross-platform 概念の後継へ統合する。

| 統合先候補 | Batch B の材料 |
|---|---|
| `architecture/native-host-boundary.md` | Native Host の責務、空状態、ライフサイクル、完全 model と projection の分離 |
| `architecture/store-and-update-streams.md` | Store の現在状態と変更通知、Theme 通知の分離 |
| `architecture/display-state-synchronization.md` | 構造・同一 ID の内容・可視性・Theme の反映経路 |
| `architecture/cell-renderer-registry.md` | Cell 型と Renderer の登録・解決、未登録時挙動、再利用境界 |
| `architecture/declarative-ui-bridge.md` | DSL / Store 二方式と同じ Native 更新経路への収束 |
| `architecture/declarative-tree-identity.md` | dynamic key、明示 ID、static fallback、内容と identity の分離 |
| `styling/style-resolution.md` | UI 層の Theme / CellStyle 所有、Cell 固有値から platform default までの解決 |
| `styling/cell-row-layout.md` | 共通行、trailing control、最低行高と可変高 |
| `styling/cell-visual-states.md` | highlighted / selected / disabled と Native control の状態表現 |
| `styling/list-appearance.md` | classic / modern、canvas、Header / Footer、separator の視覚契約 |

Batch A から繰り越された「表示状態同期」は、iOS だけでは横断概念として確定せず、ここに合流させた。

## ADR 候補の扱い

新規 ADR は起こさない。

| 候補 | 既存 ADR |
|---|---|
| Native 描画を SwiftUI から再利用し、Registry で Cell 描画を拡張する | ADR-0004 |
| Store と構造 Diff を更新境界にする | ADR-0006 |
| DSL と Store を併存させ同じ更新経路へ収束する | ADR-0007 |
| 宣言ツリーの内容と identity を分離する | ADR-0008 |
| Theme / CellStyle を UI 層で Native 型として所有する | ADR-0009 |
| 表示同期を構造・内容・可視性へ分ける | ADR-0010 |
| Cell 共通行をコンポジションで統一する | ADR-0011 |

Diffable Data Source 更新中の layout 同期差し替えを避ける判断は、iOS Host 内の局所的な実装制約として platform 概念に必要な範囲だけ残し、独立 ADR にはしない。

## drift 所見

解消方向は決めず、オーナーレビュー対象として保持する。

1. Controller の view load 前に Store が部分 Diff を発行すると、Store は更新されるが Controller の内部 root へ取り込まれず、初期 snapshot が古い root から構築される。
2. `SettingsRootStore.updateAccessory` は存在しない Section Header / Footer を対象にしても `.updateAccessory` を発行し、通常の missing-target 操作と通知契約が揃っていない。
3. `docs/platform-guide-ios.md` は独自 Renderer に internal の `KsListCellBase` 継承を案内するが、ライブラリ外から継承できない。
4. 旧 spec / docs は公開 `KsSettingsView` 自体を `UIViewControllerRepresentable` とするが、現行は `SwiftUI.View` で内部型が Representable を担う。
5. 旧 spec の DSL initializer / builder / `ForEach` signature は、現行 `KsSettingsViewBuilder` と builder 付き `ForEach` に一致しない。
6. ADR-0008 と旧 spec は dynamic collection key を明示 ID より優先するが、現行 iOS の `DSLHintRegistry` は明示 ID を優先する。両方を組み合わせたテストはない。
7. 旧 spec / docs が機能する API として案内する `.disabled(_:)` は、現行では常に no-op である。
8. `sectionHeader` / `sectionFooter` の copy は元 Section の `isVisible` を渡さず、非表示 Section に適用すると `true` へ戻る。
9. 独自 `ForEach` は一 item から複数要素を返せる signature だが、全結果へ同じ hint を付けるため、同一階層で ID が衝突する。
10. docs の独自 Cell 手順は Registry 登録までしか説明せず、DSL で ID を再束縛する `DSLReidentifiable` を欠く。
11. `Theme.separatorColor` は保持・比較されるが、現行 separator 描画へ適用されない。
12. `Theme.scrollIndicatorVisible` は保持・比較されるが、UICollectionView の indicator へ適用されない。
13. `Theme.headerBackgroundColor` / `footerBackgroundColor` は保持・比較されるが、supplementary 背景へ適用されない。
14. `KsSettingsViewController.applyTheme(_:)` は Cell と canvas を更新する一方、表示済み Header / Footer の再構成や layout 再構築を行わず、font・色・`Theme.headerHeight` が即時反映されない可能性がある。
15. 旧 spec の description 既定色はライトモード固定色とするが、現行は dynamic `UIColor.secondaryLabel` である。
16. 旧 style spec は廃止済みの `KsSettingsView(root:style:)` と `.header` / `.footer` を参照する。
17. 旧 theme bridge spec は Theme / CellStyle / KsImage を Core 所有の変換対象とするが、現行は UI 層で UIKit 型を直接所有し、変換層は存在しない。
18. 旧 spec は削除済み Theme 名 `viewBackgroundColor` / `titleColor` / `titleFont` / `descriptionColor` を参照する。
19. docs の `LabelCell` + `CellStyle` 例は initializer の引数順が現行 signature と違い、そのままではコンパイルできない。
20. `DSLIconModifiable.swift` と `CellModifiers.swift` のコメントは icon 対応を一部 Cell に限定するが、現行の組み込み12種はすべて `DSLIconModifiable` に準拠する。

SwiftUI `.theme(_:)` の Store / DSL make・update 経路を直接検証するテストは見当たらない。これは資料間の矛盾ではなく、Theme bridge の検証鮮度に関する所見として保持する。

## 見送った情報

- Diff 算出順、stable hash の演算、snapshot / compositional layout の内部手順はコードから再導出しやすいため記載しない。
- Theme / CellStyle の全フィールド表は API 定義から再導出しやすく、利用者の選択を助ける解決規則だけを残した。
- stack spacing、margin、estimated height など UIKit layout の生値は長命な公開契約にしない。
- 旧 docs / spec の廃止済み API 例は現行利用例へ置き換え、歴史的 signature は移行しない。

## 初見可読性レビューの反映

`batch-b-readability-review.md` の必須4件と推奨6件を確認し、L2 reference としての位置づけ、実配置基準の概念間リンク、import と前提値を備えた SwiftUI 最小例、identity の安全な利用契約を修正した。

あわせて Store / Controller の状態所有表、DSL / Store 方式比較表、補助 API の対象用途、動的 `ForEach` 例、用語表を追加した。再レビューは **PASS** で、残存する必須・推奨の可読性問題はない。

## オーナーレビュー

2026-07-19 に Batch B の確定承認を得た。統合ドラフト2件を `concepts/platforms/` へ配置し、index・log・tasks を更新した。

承認時の指摘により、Batch A で確定済みの `cells/basic-cells.md` も現行 `ButtonCell` の公開 API に合わせて訂正した。基本7種はすべて `valueText` / `icon` / `hintText` を持ち、`ButtonCell` だけが `description` を持たない。この訂正は `concepts/log.md` に記録した。
