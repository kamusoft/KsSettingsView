---
name: kssettingsview-ios
description: KsSettingsView で iOS の設定画面 (settings screen) を作る - SwiftUI の宣言的 DSL (KsSettingsView) または UIKit ホスト (KsSettingsViewController) で、組み込みの Cell (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, NumberPicker, TimePicker, DatePicker)、任意の SwiftUI View を行 (row) として表示する CustomCell、SettingsRootStore による表示中の更新、Theme / CellStyle のスタイル指定を扱う。KsSettingsViewCore / KsSettingsViewUI / KsSettingsViewSwiftUI に依存する Swift アプリで設定画面を追加・変更・レビューするときに使う。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsSettingsView
---

# KsSettingsView for iOS

KsSettingsView は、iOS の設定アプリのようなリスト形式の設定画面を組み立てる UI ライブラリ。画面は行 (Cell) を Section にまとめたツリーとして宣言し、そのツリーがそのまま画面になる。この Skill が扱うのは iOS 版で、SwiftUI の宣言的 DSL と UIKit ホスト (`KsSettingsViewController`) の 2 つの形で提供される。宣言ツリーとして書いても、Store から命令的に操作する形でも書ける。

## できること

| やりたいこと | 参照先 |
|---|---|
| 行を置く: ラベル、操作、ボタン、スイッチ、チェックボックス、ラジオ、テキスト入力、リスト選択、数値、時刻、日付 | [references/cells.md](references/cells.md) |
| 行を Section にまとめる、アイコン・説明・ヒントを付ける、行を無効化・非表示にする | [references/cells.md](references/cells.md) |
| 表示中の画面を変える: 行の挿入・削除・移動・差し替え、複数行のバッチ更新 | [references/updates.md](references/updates.md) |
| 再評価をまたいで行を追跡する、状態から表示・非表示を切り替える、UIKit から画面を組み込む | [references/updates.md](references/updates.md) |
| 変更を `SettingsRootDiff` として表す、Diff や Theme を Controller へ直接適用する | [references/updates.md](references/updates.md) |
| 色・フォント・行高さ、Classic / Modern の list 外観、Section の箱 | [references/styling.md](references/styling.md) |
| Section と画面全体の Header / Footer (任意の SwiftUI View も置ける) | [references/styling.md](references/styling.md) |
| 任意の SwiftUI View を行 (row) として表示する、独自の Cell 型と Renderer を定義する | [references/custom-cells.md](references/custom-cells.md) |

## 導入

ライブラリは `https://github.com/kamusoft/KsSettingsView-SPM` から Swift パッケージとして配布している。Xcode のアプリプロジェクトなら *File > Add Package Dependencies...* でその URL を指定し、`KsSettingsView` product を追加する。SwiftPM パッケージならマニフェストで依存を宣言する:

```swift
// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "MyApp",
    platforms: [.iOS(.v16)],
    dependencies: [
        .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")
    ],
    targets: [
        .target(
            name: "MyApp",
            dependencies: [
                .product(name: "KsSettingsView", package: "KsSettingsView-SPM")
            ]
        )
    ]
)
```

リンクするのはこの 1 product だが、`import` はモジュール名で書く。含まれるモジュールは 3 つ: `KsSettingsViewCore` (設定ツリー)、`KsSettingsViewUI` (Cell・`Theme`・`CellStyle`・UIKit ホスト)、`KsSettingsViewSwiftUI` (SwiftUI View と宣言的 DSL)。組み込みの Cell は [references/cells.md](references/cells.md) で 1 種ずつ扱い、`CustomCell` と独自 Cell 型は [references/custom-cells.md](references/custom-cells.md) で扱う。

| 要件 | 最低バージョン |
|---|---|
| Swift tools version | 5.10 |
| iOS deployment target | 16.0 |

## 最小動作コード

```swift
import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

struct SettingsScreen: View {
    @State private var notifications = true

    var body: some View {
        KsSettingsView {
            ksSection("General") {
                LabelCell(title: "Version", valueText: "1.0.0")
                SwitchCell(
                    title: "Push notifications",
                    isOn: notifications,
                    onValueChanged: { notifications = $0 }
                )
            }
        }
    }
}
```

`Section` ではなく `ksSection` を使うのは、行の builder が `SwiftUI.Section` と衝突しないようにするため。

## リファレンス

- [references/cells.md](references/cells.md) - 組み込み Cell ごとのレシピと、Section・アイコン・全 Cell 共通フィールド。
- [references/updates.md](references/updates.md) - 表示中の画面の更新、行の同一性、可視性、UIKit からの利用。
- [references/styling.md](references/styling.md) - `Theme`、`CellStyle`、style modifier、list 外観、Header / Footer。
- [references/custom-cells.md](references/custom-cells.md) - `CustomCell`、再利用のためのラップ関数、独自 Cell 型と Renderer。
