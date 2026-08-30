# Candidate: docs-foundation-sweep

対象は `docs/README.md`、`docs/overview.md`、`docs/architecture.md`。先に確定済み concepts、platform build 定義、Core / Host / declarative bridge の現行コードと代表テストを確認し、その後で3文書を残差として照合した。

## 概念候補

### 独立した概念候補なし

3文書にある価値の高い責務境界と公開契約は、次の確定済み concepts に回収済みである。

| docs の主題 | 回収先 |
|---|---|
| monorepo、platform 別 build root、Core → UI Host → declarative wrapper の依存方向、Sample / MAUI placeholder | `architecture/repository-boundaries.md` |
| Native list と Store / model / visible projection の境界 | `architecture/native-host-boundary.md` |
| DSL 方式と Store 方式の使い分けと共通 Host への収束 | `architecture/declarative-ui-bridge.md` |
| 構造・内容・可視性・Theme の更新経路 | `architecture/display-state-synchronization.md` / `architecture/store-and-update-streams.md` |
| 動的 key、明示 ID、位置 fallback | `architecture/declarative-tree-identity.md` |
| Core の設定ツリーと Diff | `core-model/settings-tree.md` / `core-model/structural-changes.md` |
| Native style 型、Theme / CellStyle / EffectiveStyle | `styling/style-resolution.md` |
| platform 固有の公開利用契約 | `platforms/ios-native-host.md` / `platforms/ios-swiftui.md` / `platforms/android-native-host.md` / `platforms/android-compose.md` |
| module、namespace、配布座標の命名 | `conventions/public-identifiers.md` |

最低 OS、Swift tools version、Gradle / AGP / Kotlin / JDK / compileSdk などの値は現行 build metadata から直接確認でき、Batch D で長命 concept へ複製しない方針が確定済みである。今回も独立概念へ昇格させない。

出典: `kasane/concepts/index.md`、`ios/Package.swift`、`android/settings.gradle.kts`、`android/gradle/wrapper/gradle-wrapper.properties`、`android/ks-settingsview-{core,ui,compose}/build.gradle.kts`、両 platform の Core / Host / declarative bridge 実装とテスト。

## ADR 候補

新規候補なし。

- monorepo と platform 別 build root は ADR-0001 に包含される。
- Native Host を描画基盤とし宣言 UI / 将来の MAUI から再利用する方針は ADR-0004 に包含される。
- 構造 Diff と Store、DSL / Store の併存、安定 identity、Native style 型、表示状態同期は ADR-0006〜0010 に包含される。
- Native iOS / Android を先行させるという目的記述から、未実装 MAUI / KMP の新しい決定内容・代替案・却下理由は再構成できない。既存 ADR を越える決定を創作しない。

## drift 所見

解消方向は決めず、現行コード・テストまたは現行 Kasane 運用との差だけを記録する。

1. `docs/README.md:3-11,27,71-72` と `docs/overview.md:38,148-150` は `openspec/specs/` を仕様の SSoT とし、最初に docs → OpenSpec を読む運用を案内する。一方、現行 Kasane ではコードとテストが現仕様の SSoT で、`openspec/` は歴史資料として凍結されている。この所見は Batch D 統合結果の drift 3 を担当文書で再確認したもの。
2. `docs/overview.md:47,80` と `docs/architecture.md:27-38` は Core が platform 型を一切持たないと説明する。現行 `KsAnyView` は iOS Core で UIKit / SwiftUI、Android Core で Android View / Compose を保持し、Android Core は `com.android.library` と Compose Runtime に依存する。正しい境界は「Core は Theme / CellStyle / KsImage と描画責務を持たない」であり、確定済み `core-model/settings-tree.md` が回収済み。この所見は Batch D drift 2 の再確認。
3. `docs/overview.md:55` は Android Core を `sealed interface` ベース、依存を Kotlin stdlib のみとする。現行 `Cell` は利用側 module から実装可能な通常の `interface` で、`ks-settingsview-core` は Android Framework / Compose Runtime / AndroidX annotation に依存する (`Cell.kt`、`ks-settingsview-core/build.gradle.kts`)。
4. `docs/overview.md:3,18-24` は .NET MAUI を提供済みの対応 platform と同列に置く一方、同文書 `:22,32,61-63` と現行 repository は未着手とする。現行成果物は空の `maui/KsSettingsView.slnx` と Sample placeholder だけであり、利用可能な MAUI library はない。
5. `docs/overview.md:14` の「iOS / Android で一字一句揃う」は現行公開 API と一致しない。たとえば Cell ID は iOS `UUID` / Android `String`、Section ID の既定も異なり、Host、DSL、modifier の型と名前にも platform 差がある。確定済み concepts は責務の対応関係を共通化し、API の文字列一致までは保証していない。
6. `docs/README.md:54` は具象 Cell を基本7種だけと説明するが、現行 UI module は基本7種に加えて入力5種を公開する。確定済み `cells/basic-cells.md` と `cells/input-cells.md` が12種を分けて回収済み。
7. `docs/architecture.md:92-100` の SwiftUI DSL 例は `SwitchCell("...", isOn: $notifEnabled)` という Binding initializer を使う。現行 `SwitchCell` は `title:`、値の `isOn:`、`onValueChanged:` callback を受け、基本 Cell に Binding initializer はない (`ios/Sources/KsSettingsViewUI/SwitchCell.swift`)。
8. `docs/architecture.md:124-131` の Android Store 例は `store.insertCell(..., index = 0)` とする。現行公開 signature のラベルは `at` である (`SettingsRootStore.insertCell(cell, sectionId, at)`)。
9. `docs/architecture.md:133-157` は DSL / Store の両経路が最終的に `SettingsRootDiff` 列へ収束し、Android の内容更新は `ReplaceCell` を発行しないと一般化する。現行 Android では DSL の複数内容更新は `replaceCells` → `contentUpdateBatches` の専用 stream を使う一方、公開 Store の単一 `replaceCell` は `SettingsRootDiff.ReplaceCell` を発行する。共通点は Store / Native Host へ収束することであって、すべてが一つの Diff stream に統一されることではない。
10. `docs/architecture.md:182` は Android の Section Header / Footer を `ConcatAdapter` の先頭・末尾 Adapter とする。先頭・末尾の `RootHeaderFooterAdapter` が扱うのは Root Header / Footer であり、Section Header / Footer は `KsSettingsListAdapter` 内の `CellListItem.SectionHeader` / `SectionFooter` として Cell 行と同じ main list に入る。
11. `docs/architecture.md:184` は public iOS entry を `struct KsSettingsView: UIViewControllerRepresentable` とする。現行 public `KsSettingsView` は `SwiftUI.View` で、`StoreBackedRepresentable` / `DSLBackedRepresentable` が internal の `UIViewControllerRepresentable` 境界を担う (`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`)。
12. `docs/architecture.md:187` は iOS / Android とも動的 collection key を明示 ID より優先すると説明し、accepted ADR-0008 も同じ優先順位を定める。一方、現行 iOS の `DSLHintRegistry` と Android の hint 付与は、併用時にどちらも明示 ID を残す。確定済み `architecture/declarative-tree-identity.md` は解消方向を決めず、同じ要素で key と明示 ID を併用しない安全な利用契約にしている。この所見は Batch D drift 10 の再確認。

## 用語

- foundation docs: `docs/README.md`、`docs/overview.md`、`docs/architecture.md`。project 全体の入口・構造・設計総論を扱う旧 docs 群。
- 残差: 確定済み concepts / ADR に未回収で、現行コード・テストに裏付けられた長命知識。

## 抽出メモ

- 独立 concept は追加せず、既存21 concepts を全体の入口として維持するのが適切である。
- toolchain の個別 version と build command は高腐食で build metadata から再導出しやすく、Batch D の見送り判断を維持する。Xcode 16 / .NET 9 のように現行 build metadataだけでは強制を確認できない値は製品契約へ移さない。
- KMP 展開 (`docs/overview.md:24`) は現行コード、accepted ADR、Kasane roadmap の裏付けがなく、構想以上の知識として採用しない。
- `AiForms.Maui.SettingsView` の後継という由来は意図として読めるが、既存 ADR-0004 と Sample / legacy reference 以上の新しい責務契約を導かないため、独立 concept にしない。
- docs の stub 化・書き換えは本抽出の担当外。Batch E 統合側で、確定済み concepts への入口化を別変更として提案する材料にする。
