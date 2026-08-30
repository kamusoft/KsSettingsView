# Candidate: samples-ios

## 概念候補

### 独立概念候補なし

`KsSettingsViewSample` は新しい公開 API や状態契約を定義する capability ではなく、既存の iOS SwiftUI Bridge、Native Host、基本 Cell、入力 Cell、Theme / CellStyle、visible projection を実行可能な画面として束ねる利用例・目視検証用アプリである。Sample 固有 concept を追加すると、デモ画面の増減や診断コードの入れ替えを長命層へ重複記載することになり、既存 `platforms/`・`cells/`・`styling/` 概念より先に腐る。

したがって新規 concept は作らず、以下の公開利用例だけを既存概念へ合流することを提案する。Xcode の開き方、scheme、ビルドコマンド、デモ画面一覧は `samples/ios/README.md` に残す運用情報であり、concepts へ移さない。

#### 実行可能な接続契約

- `KsSettingsViewSample.xcodeproj` は iOS 16.0 / Swift 6 の SwiftUI App で、`../../ios` を Local Swift Package として参照し、`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の3製品をリンクする。
- `@main` の `KsSettingsViewSampleApp` は `ContentView` の `NavigationStack` を開き、利用経路・Cell 群・表示状態・診断を別画面で選べる。
- Sample は既定の `KsSettingsViewController` を使うため、基本 Cell 7種と入力 Cell 5種を手動登録しない。既定 shared registry では Controller 初期化時に `registerBasicCells()` / `registerInputCells()` が呼ばれる。
- Local Swift Package 接続は、Sample から library 本体へ step-in して UI を観察する開発経路を提供する。ただしこの接続方法は Sample の開発運用であり、ライブラリ利用者の公開 API 契約ではない。

出典: `samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj`、`samples/ios/KsSettingsViewSample/KsSettingsViewSampleApp.swift`、`samples/ios/KsSettingsViewSample/ContentView.swift`、`ios/Package.swift`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` / `InputCellsTests.swift`

#### 標準利用経路と更新経路

一般用途の標準経路は `KsSettingsView { Section { Cell... } }` の DSL 方式である。SwiftUI 状態は callback または入力 Cell の `Binding<T>` initializer から更新し、View 再評価を経て同じ Native Host へ反映する。

```swift
@State private var enabled = true

KsSettingsView {
    Section("通知") {
        SwitchCell(
            title: "プッシュ通知",
            isOn: enabled,
            onValueChanged: { enabled = $0 }
        )
    }
}
```

入力 Cell 5種は `Binding<T>` を受ける TwoWay 経路を持つ。`InputCellsDemoView` は `EntryCell(text:)`、単一・複数の `PickerCell`、`NumberPickerCell(value:)`、`TimePickerCell(time:)`、`DatePickerCell(date:)` を `@State` へ接続する。`EntryCell` では、同じ画面で値 + `onTextChanged` の callback 経路も比較できる。

```swift
@State private var name = ""
@State private var selected: Int? = 0

KsSettingsView {
    Section("入力") {
        EntryCell(title: "名前", text: $name, maxLength: 20)
        PickerCell(
            title: "テーマ",
            items: ["ライト", "ダーク"],
            selectedIndex: $selected
        )
    }
}
```

大量データ、高頻度更新、命令型の部分操作には、利用者所有の `SettingsRootStore` を渡す Store 方式を使う。`StoreDemoView` は `@StateObject` で Store を保持し、`insertCell(_:in:at:)` と `removeCell(cellID:)` による追加・削除を示す。

```swift
@StateObject private var store = SettingsRootStore(initialRoot: root)

KsSettingsView(store: store, style: .classic)

store.insertCell(newCell, in: sectionID, at: index)
store.removeCell(cellID: cellID)
```

合流先:

- DSL / Store の選択、`ForEach`、Root H/F、Cell modifier は `platforms/ios-swiftui.md`。
- `SettingsRootStore` の部分操作、標準 Cell の自動登録、UIKit Host への接続は `platforms/ios-native-host.md`。
- 基本 Cell の値 + callback と入力 Cell の `Binding<T>` は `cells/basic-cells.md` / `cells/input-cells.md`。

出典: `samples/ios/KsSettingsViewSample/ContentView.swift` / `DSLDemoView.swift` / `InputCellsDemoView.swift`、`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `ForEachDSL.swift`、`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift` / `EntryCell.swift` / `PickerCell.swift` / `NumberPickerCell.swift` / `TimePickerCell.swift` / `DatePickerCell.swift`、`ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewDSLIntegrationTests.swift` / `KsSettingsViewRepresentableTests.swift`、`ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift` / `InputCellsTests.swift`

#### デモ間の責務境界

| デモ | 実行可能に示す契約 | 合流先 |
|---|---|---|
| `StoreDemoView` | 利用者所有 Store、`SettingsRoot` の初期構築、Cell の部分追加・削除 | `platforms/ios-native-host.md` / `platforms/ios-swiftui.md` |
| `DSLDemoView` | 静的 Section、独自 `ForEach`、再評価をまたぐ動的追加・削除、`cellHeight`、Root H/F | `platforms/ios-swiftui.md` |
| `BasicCellsDemoView` | 基本 Cell 7種の意味、値 + callback、MAUI 互換 Theme、`CellStyle(cellHeight:)`、`KsImage.systemName` | `cells/basic-cells.md` / styling の Theme・行高さ概念 |
| `InputCellsDemoView` | 入力 Cell 5種の `Binding<T>` と callback、単一・複数選択、選択上限、unit、`.wheels` / `.calendar` | `cells/input-cells.md` |
| `UnifyCellCommonFieldsDemoView` | `description` / `valueText` / `icon` / `hintText` と `accentColor` の組み合わせ、`ButtonCell` の通常配置 | `cells/basic-cells.md` / Cell 共通行レイアウト概念 |
| `VisibilityDemoView` | Cell / Section × 中間 / 末尾の `isVisible` 切替。hidden model と visible projection の分離 | `platforms/ios-native-host.md` / 表示状態同期概念 |
| `MinimalDiffableDemoView` | library を介さない Diffable / Compositional Layout の比較診断。layout 再生成と `sectionProvider` + invalidate の挙動差 | concepts へ合流しない。診断用 L3 コードとして保持 |

`MinimalDiffableDemoView` は `KsSettingsView` の標準利用例ではない。`UICollectionViewDiffableDataSource` と `UICollectionViewCompositionalLayout` を直接構築し、`setCollectionViewLayout` の同期差し替えを再現する比較実験であるため、ライブラリ公開契約として紹介してはいけない。

出典: `samples/ios/KsSettingsViewSample/*.swift`、`ios/Tests/KsSettingsViewUITests/VisibilityProjectionTests.swift` / `UnifyCellCommonFieldsTests.swift` / `KsSettingsViewControllerTests.swift`

#### customization の利用例

- 画面全体は `.theme(Theme(...))` と `.style(.classic | .modern)`、Root は `.rootHeader(...)` / `.rootFooter(...)` で調整する。
- Section は Header / Footer、`headerHeight`、`isVisible`、Cell は `CellStyle`、`KsImage`、共通フィールド、`isEnabled`、`isVisible` で調整する。
- `BasicCellsDemoView` の MAUI 互換 Theme は `UIColor` を直接使い、canvas の `backgroundColor`、Cell の `cellBackgroundColor`、accent、Header / Footer 色を別フィールドとして指定する。旧 `KsColor` を中間表現として使わない。
- `UnifyCellCommonFieldsDemoView` は個別 `accentColor` と共通フィールドの組み合わせを示すが、各 Cell signature の網羅表として扱わない。

合流先は styling の Theme / CellStyle / EffectiveStyle と Cell 共通行レイアウト概念である。Sample 固有の色値を長命な必須 design token として固定するのではなく、「移植元と見た目を比較するため利用側が明示する Theme」の例として扱う。

出典: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` / `UnifyCellCommonFieldsDemoView.swift`、`ios/Sources/KsSettingsViewUI/Theme.swift` / `CellStyle.swift`、`kasane/decisions/0009-ui-layer-native-styling.md`、`docs/styling-and-theming.md`

#### 保証すること

- DSL 方式と Store 方式の Sample は同じ `KsSettingsViewController` / `SettingsRootStore` 経路へ接続する。
- 基本 Cell の状態更新は値 + callback、入力 Cell は値 + callback に加えて `Binding<T>` を利用できる。
- `isVisible` の切替は model から値を削除せず、visible projection の再構築として観察できる。
- 既定 Host を使う限り、標準12種の Cell は Sample 側の手動 Registry 登録なしで描画できる。
- Theme、CellStyle、Root / Section H/F、icon、共通フィールドを別々の customization 軸として実行確認できる。

#### してはいけないこと

- Sample の画面一覧を公開 API の独立 concept として複製しない。画面一覧は README、公開契約と利用例は既存 concepts に置く。
- iOS の基本 Cell 7種に `Binding` initializer があると案内しない。現行 API と Sample は値 + callback を使う。
- 入力 Cell の TwoWay 経路を基本 Cell 全体へ一般化しない。
- `MinimalDiffableDemoView` を `KsSettingsView` の推奨実装または公開 API 利用例として扱わない。
- MAUI 互換 Sample Theme をライブラリの default Theme とみなさない。

## ADR 候補

なし。

- Sample をモノレポ内の独立 Xcode project とし、Local Swift Package で iOS build root を参照する構成は ADR-0001「モノレポとプラットフォーム別ビルドルート」に包含される。
- Bundle Identifier `jp.kamusoft.kssettingsview.samples.ios` は ADR-0002「公開識別子の名前空間」に包含される。
- DSL / Store の併存と同一更新経路への収束は ADR-0007、`UIColor` を直接使い Theme / CellStyle を UI 層へ置く制約は ADR-0009、visibility の診断パターンは ADR-0010 に包含される。
- MAUI 互換 Theme の具体色とデモの表示順は局所的で可逆な Sample 構成であり、新規 ADR の選別基準を満たさない。

## drift 所見

1. `samples-ios` spec の Purpose は現在も `TBD` で、現行 Sample が「利用経路・標準12 Cell・visibility・診断を実行可能に束ねる」役割を説明していない (`openspec/specs/samples-ios/spec.md` Purpose ↔ `samples/ios/KsSettingsViewSample/ContentView.swift`)。
2. spec 本文は「起動直後の画面」が `KsSettingsView` と Store を直接表示するとするが、現行アプリの起動直後は `ContentView` の `NavigationStack` / `List` であり、Store デモは遷移先である。同じ spec の後続 Scenario はトップメニューから Store デモを選ぶとしており、spec 内部でも表現が不一致 (`openspec/specs/samples-ios/spec.md`「基本 Cell を含むデモ画面」Requirement ↔ `samples/ios/KsSettingsViewSample/ContentView.swift`)。
3. spec の MAUI 互換 Theme は旧フィールド `viewBackgroundColor` / `titleColor` を要求するが、現行 `Theme` と Sample は `backgroundColor` / `cellTitleColor` を使う (`openspec/specs/samples-ios/spec.md`「MAUI 互換 Theme の明示渡し」↔ `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` / `ios/Sources/KsSettingsViewUI/Theme.swift`)。
4. spec は `RadioCell` の選択状態を `isSelected` で記述するが、現行 API と Sample は `value` と `selectedValue` の一致で選択を表す (`openspec/specs/samples-ios/spec.md`「RadioCell セクション」↔ `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` / `ios/Sources/KsSettingsViewUI/RadioCell.swift`)。
5. spec は Store / DSL / 基本 Cell 7種を中心にしており、現行トップメニューに追加された入力 Cell 5種、共通フィールド、`isVisible`、Minimal Diffable の4デモを記述していない (`openspec/specs/samples-ios/spec.md` ↔ `samples/ios/KsSettingsViewSample/ContentView.swift`)。
6. Sample README の概要、起動成功条件、ディレクトリ構成はデモを Store / DSL / 基本 Cell の3画面としており、現行7画面のうち入力 Cell、共通フィールド、visibility、Minimal Diffable を欠く (`samples/ios/README.md`「概要」/「実行手順」/「ディレクトリ構成」↔ `samples/ios/KsSettingsViewSample/ContentView.swift`)。
7. Sample README の目視チェックリストはアイコン有り中間 Cell の罫線 inset を52pt、アイコン無しを16ptとするが、現行 Controller と tests はアイコン有無にかかわらず中間 Cell を16pt固定としている (`samples/ios/README.md`「罫線インセット規則」↔ `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` / `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift` / `docs/styling-and-theming.md`「罫線」)。
8. `docs/platform-guide-ios.md` の Sample 一覧は DSL / Basic / Visibility / Common Fields / Minimal Diffable の5画面だけを挙げ、現行の Store と Input Cells を欠く (`docs/platform-guide-ios.md`「Sample アプリ」↔ `samples/ios/KsSettingsViewSample/ContentView.swift`)。
9. `docs/platform-guide-ios.md` は `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` に `Binding<T>` initializer があるとしており、掲載コードは現行 API では成立しない。現行の基本 Cell と `BasicCellsDemoView` は値 + callback、`Binding<T>` は入力 Cell 5種が提供する (`docs/platform-guide-ios.md`「クイックスタート」/「@State / @Binding 駆動 Cell」↔ `ios/Sources/KsSettingsViewUI/SwitchCell.swift` / `CheckboxCell.swift` / `RadioCell.swift` / `SimpleCheckCell.swift` / `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` / `InputCellsDemoView.swift`)。

## 用語

- Sample: `KsSettingsView` の公開利用経路を実機・シミュレータで実行し、目視確認する SwiftUI App。
- DSL 方式: `KsSettingsView { Section { Cell... } }` から宣言ツリーを評価し、内部 Store を使う一般用途の経路。
- Store 方式: 利用者所有の `SettingsRootStore` を `KsSettingsView(store:)` へ渡し、公開操作で命令的に更新する経路。
- TwoWay 経路: 入力 Cell が `Binding<T>` を受け、Native 入力から SwiftUI 状態へ値を書き戻す経路。
- callback 経路: Cell が現在値を受け、操作結果を callback で外部の状態所有者へ通知する経路。
- 目視検証: 単体テストだけでは表現しにくい layout、色、animation、Native picker の挙動を Sample 上で観察すること。
- Minimal Diffable: `KsSettingsView` を介さず UIKit の Diffable / Compositional Layout を比較する診断画面。

## 抽出メモ

- 独立概念は0件。Sample は公開契約の一次所有者ではなく、確定済み `platforms/ios-swiftui.md`、`platforms/ios-native-host.md`、`cells/basic-cells.md`、`cells/input-cells.md` と、今後確定する styling / 表示状態同期概念へ利用例を供給する立場と判断した。
- `StoreDemoView` と `DSLDemoView` は ADR-0007 の二つの入口を、同じアプリ内で比較できる最小例である。標準経路は DSL、高頻度・命令型更新は Store という既存概念の使い分けを補強する。
- `BasicCellsDemoView` / `InputCellsDemoView` / `UnifyCellCommonFieldsDemoView` の全 Cell・全引数を concept へ転記しない。代表利用例と経路差だけを合流し、signature の網羅はコードへ委ねる。
- `VisibilityDemoView` は ADR-0010 と visible projection 概念の実行例であり、Sample 固有 concept ではない。
- `MinimalDiffableDemoView` は現行 implementation の layout 問題を切り分ける診断コードであり、公開契約と混ぜない。
- Xcode project の存在、Local Package 相対パス、deployment target、Swift version、Bundle Identifier は README / project 設定で管理する。concepts へは公開利用 API の例だけを残す。
- spec / docs の drift は本抽出で解消せず、凍結 OpenSpec と変更フロー管理対象の docs に対する所見として記録する。
