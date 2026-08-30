---
type: reference
title: iOS SwiftUI Bridge と宣言 DSL
description: KsSettingsView の Store 方式・DSL 方式、identity、modifier、Theme 伝播の利用契約
tags: [ios, swiftui, dsl, public-api]
timestamp: 2026-08-19
---

この文書は、SwiftUI から KsSettingsView を使うための公開 API 利用契約と責務境界を整理した reference である。読むと、Store 方式と DSL 方式の選び方、動的要素の identity、Root・Section・Cell modifier、Theme の更新経路が分かる。UIKit Host を直接使う場合は [iOS Native Host の利用と更新境界](ios-native-host.md) を参照する。

## 目的

`KsSettingsView` は SwiftUI の状態または利用者所有の `SettingsRootStore` を、UIKit の `KsSettingsViewController` へ接続する公開 `SwiftUI.View` である。公開型自体は `UIViewControllerRepresentable` ではなく、内部の Representable が Native Host の生成・更新境界を担う。

SwiftUI 側は宣言状態と値・event の橋渡しを行い、Native list、visible projection、Cell renderer、Theme / CellStyle の実効値解決を再実装しない。

## 二つの利用方式

| 方式 | Store の所有者 | 向くケース | 更新方法 |
|---|---|---|---|
| DSL | `KsSettingsView` の内部 | 静的・中規模の一般的な設定画面 | SwiftUI 状態から宣言ツリーを再評価する |
| Store | 利用者 | 大量データ、高頻度更新、命令型の部分操作 | 利用者が `SettingsRootStore` の公開操作を呼ぶ |

### DSL 方式

一般的な静的・中規模の設定画面には `KsSettingsView(style:) { ... }` を使う。内部 Store と前回の resolved tree は SwiftUI の View identity が続く間保持される。再評価時は前回との差だけが Store と Native Host の共通更新経路へ流れる。

```swift
import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

struct SettingsScreen: View {
    @State private var enabled = true

    var body: some View {
        KsSettingsView {
            ksSection("通知") {
                SwitchCell(
                    title: "プッシュ通知",
                    isOn: enabled,
                    onValueChanged: { enabled = $0 }
                )
            }
            .sectionFooter("端末の通知設定も確認してください")
        }
        .rootHeader("プロフィール")
        .style(.modern)
    }
}
```

DSL は SwiftUI `body` の getter から Native View や Store を直接変更しない。内部 Representable の update 境界で宣言ツリーを再評価し、構造・内容・可視性・Theme をそれぞれの更新経路へ渡す。

### Store 方式

大量データ、高頻度更新、命令型の部分操作を利用者側で制御したい場合は `KsSettingsView(store:style:)` を使う。

```swift
import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

let section = KsSection("一般") {
    LabelCell(title: "バージョン", valueText: "1.0.0")
}
let root = SettingsRoot(sections: [section])
let store = SettingsRootStore(initialRoot: root, initialTheme: Theme())
let view = KsSettingsView(store: store, style: .classic)

store.insertCell(
    LabelCell(title: "ライセンス"),
    in: section.id,
    at: section.cells.count
)
```

Store 方式は外部 Store をそのまま Native Host へ渡す。DSL 方式と別の描画基盤を持たず、どちらも `SettingsRootStore → KsSettingsViewController` に収束する。

`makeController()` と `applyUpdate(to:coordinator:)` は、SwiftUI hierarchy の外で Store backing の Controller を生成・更新するテストや独自ホスティング向けの補助 API である。通常の SwiftUI 画面では直接呼ばない。DSL backing に `makeController()` を使うと `fatalError` になるため、DSL の一般更新経路として使わない。

## DSL の構築 API

`KsSettingsViewBuilder` と Section 用 builder は、単一要素、配列、`for`、`if`、`if/else` を平坦な Section / Cell 列へ展開する。

Section は文字列または `SectionAccessory` の Header / Footer、`headerHeight`、`isVisible`、Header / Footer の表示トグル `isHeaderVisible` / `isFooterVisible` (core/ADR-0023) と Cell 列を受ける。`import SwiftUI` 時に `SwiftUI.Section` と名前が衝突する場合は、`KsSection` または `ksSection(...)` を使う。

```swift
KsSettingsView {
    ksSection("一般") {
        LabelCell(title: "バージョン", valueText: "1.0.0")
        if showAdvanced {
            LabelCell(title: "高度な設定")
        }
    }
}
```

Root modifier は `rootHeader` / `rootFooter` / `style` / `theme`、Section modifier は `sectionHeader` / `sectionFooter` / `sectionID`、Cell modifier は style / icon 系と `cellID` を提供する。Root と Section の Header / Footer は文字列と任意 SwiftUI `View` の両方を受ける。

## 宣言ツリーの identity

SwiftUI は再評価ごとに Section と Cell の値を作り直すため、一時インスタンスのランダム ID をそのまま最終 identity にしない。DSL は identity hint から決定的な UUID を解決し、同じ ID の内容変更を remove + insert ではなく reconfigure として扱う。

- 動的コレクションでは、DSL 専用 `ForEach` の `Identifiable` 版または `id:` KeyPath 版を使う。
- 動的な挿入・削除・並べ替えで特定要素を追跡する場合は、`sectionID(_:)` / `cellID(_:)` で意味上の ID を明示できる。
- 静的 Section は header text と位置、静的 Cell は親 Section ID・位置・Cell 型から fallback ID を解決する。
- title、選択値、CellStyle などの内容は identity に含めない。

同じ要素では、DSL 専用 `ForEach` の key と `sectionID(_:)` / `cellID(_:)` を併用しない。どちらか一方だけを identity として指定する。両方を組み合わせた優先順位は [core/ADR-0008](../../../decisions/core/0008-stable-declarative-tree-identity.md) と現行 iOS 実装に drift があるためである。

位置 fallback は動的な挿入・削除・並べ替えに弱い。動的構造で位置を意味上の identity として使わない。一つの `ForEach` item から同じ階層へ複数 Section / Cell を返すと、現行実装では同じ hint が付き ID 衝突を起こすため、一 item は一要素へ対応させる。

利用者定義 Cell の ID を DSL 再評価後も再束縛するには、`KsCell` と Renderer 登録だけでなく `DSLReidentifiable` への準拠が必要である。非準拠 Cell は `.cellID(_:)` を呼んでも実体の `KsCell.id` が書き換わらない。

動的コレクションでは item の key だけを identity として使う。

```swift
struct Item: Identifiable {
    let id: UUID
    let title: String
}

let items = [
    Item(id: UUID(), title: "通知"),
    Item(id: UUID(), title: "プライバシー")
]

let settingsView = KsSettingsView {
    ksSection("項目") {
        ForEach(items) { item in
            LabelCell(title: item.title)
        }
    }
}
```

この例では item の追加・削除・並べ替え後も既存 item の ID が保たれる。`LabelCell` に `.cellID(_:)` を重ねない。

## Cell と Section の modifier

Cell の `font`、`descriptionFont`、`iconSize`、`cellHeight`、`titleColor`、`backgroundColor` は、対象が `DSLStyleModifiable` に準拠する場合に `CellStyle` の該当値だけを変更した copy を返す。`icon` は `DSLIconModifiable` に準拠する場合に `KsImage` を変更した copy を返す。組み込み Cell 12 種は両方に準拠する。

```swift
let cell = LabelCell(title: "名前")
    .titleColor(.systemOrange)
    .backgroundColor(.secondarySystemGroupedBackground)
    .font(.preferredFont(forTextStyle: .headline))
    .icon(.systemName("person"))
    .cellHeight(60)
```

style modifier の連鎖は、それまでに指定した他の `CellStyle` フィールド、Cell ID、DSL identity hint を維持する。利用者定義 Cell で同じ modifier を有効にするには、対応 protocol と `withStyle(_:)` / `withIcon(_:)` の copy API を実装する。非準拠 Cell では型を壊さず元の Cell を返す。

`disabled(_:)` は現行では常に no-op であり、Cell の `isEnabled` を書き換えない。無効な Cell は各 Cell initializer の `isEnabled` で構築する。

`sectionHeader` / `sectionFooter` は文字列または SwiftUI `View` を `SectionAccessory` に変換した Section copy を返す。

## Theme の伝播

`.theme(_:)` は Theme を構造 Diff へ混ぜず、Store の Theme 状態へ渡す。

Store 方式では、modifier がなければ外部 Store の現在 Theme を維持する。指定した Theme は controller の make / update 境界で `store.applyTheme(_:)` へ渡るため、wrapper だけでなく外部 Store の Theme も更新する。Store の所有者が直接 `store.applyTheme(newTheme)` を呼んでも同じ経路で反映される。

DSL 方式では、初回に指定 Theme、未指定なら `Theme()` から内部 Store を作る。再評価時は前回と異なる Theme だけを適用する。後の再評価で Theme modifier が未指定になっても、`Theme()` へ戻す命令とは解釈せず、直前の Theme を維持する。

`.style(_:)` と `.rootHeader(_:)` / `.rootFooter(_:)` は Theme とは別の画面状態として Controller へ渡る。

## 保証すること

- Store 方式と DSL 方式は同じ Native Host と Store / Diff 経路を使う。
- SwiftUI の `body` getter 自体に Store 更新の副作用を置かない。
- 同じ意味上の identity と内容から再評価した場合、不要な構造 Diff を発行しない。
- 同じ ID の内容変更は identity を保ったまま Cell 内容を更新する。
- 可視性変更は通常の内容更新へ押し込まず、visible projection を再構築する full 更新へ切り替える。
- Root / Section / Cell の modifier は元の値を変更せず copy を返す。
- Theme 更新は Section / Cell の ID と構造を変えない。

## してはいけないこと

- Root Header / Footer や Theme を `SettingsRoot` のフィールドとして扱わない。
- DSL 評価中の `body` から Native View や Store を直接変更しない。
- 動的構造で位置 fallback に意味上の identity を期待しない。
- 同じ要素で `ForEach` の key と明示 `sectionID` / `cellID` を併用しない。
- Cell の内容を構造 identity に含めない。
- `.disabled(_:)` を機能する無効化 API として案内しない。
- Theme が未指定の Store 方式で、外部 Store の Theme を `Theme()` に上書きしない。

## 用語

| 用語 | 意味 |
|---|---|
| Store backing | 利用者所有の `SettingsRootStore` を使う方式 |
| DSL backing | `KsSettingsView` が内部 Store と前回ツリーを保持する方式 |
| identity hint | `ForEach` key、明示 ID、静的位置など、最終 ID を決める入力 |
| resolved tree | identity hint を `Section.id` / `KsCell.id` へ反映済みの宣言ツリー |
| 位置 fallback | 明示 key がない静的要素を親 ID・位置・型などから追跡する代替規則 |
| reconfigure | 同じ Cell ID のまま表示内容だけを更新すること |
| full 更新 | visible projection の再構築が必要なとき、Root 全体を現在 model から反映する更新 |

## 関連

- [iOS Native Host の利用と更新境界](ios-native-host.md)
- [SettingsRoot・Section・Cell の設定ツリー](../../core/core-model/settings-tree.md)
- [SettingsRootDiff による構造変更](../../core/core-model/structural-changes.md)
- [基本 Cell](../../core/cells/basic-cells.md)
- [入力 Cell](../../core/cells/input-cells.md)
- [KsImage](../../core/cells/ks-image.md)
