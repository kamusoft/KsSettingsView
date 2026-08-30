# レビュー結果: rollout-user-skills / kssettingsview-ios (001)

**日付**: 2026-08-26
**判定**: CHANGES_REQUESTED
**対象**: `skills/en/kssettingsview-ios/` と `skills/ja/kssettingsview-ios/` (各 SKILL.md + references/{cells,updates,styling,custom-cells}.md 計 10 ファイル)

## サマリー

構成・frontmatter・レシピ形式・en/ja の意味等価性はデルタスペックの要求を満たしており、翻訳品質も高い。源泉 concepts 5 系統 (basic/input cells・selection surfaces・styling・core-model/architecture・ios/api) の内容は概ね正確に反映されている。

一方で、デルタスペック「生成の内容規約 ④ (利用者がコピーして動くこと)」が守れていない。**コード例 3 箇所が Swift の実引数順序規則と `@MainActor` 分離により実際にコンパイルできない**ことを、`ios/Sources/` を依存に取った使い捨てパッケージを iOS Simulator 向けにビルドして確認した (4 件の `error:`)。さらに全 Cell 共通フィールドの説明に、源泉 concept ([kasane/concepts/core/cells/input-cells.md](../../concepts/core/cells/input-cells.md):27,62) と実装の双方に反する記述がある。いずれも利用者がそのまま写して失敗する型の誤りであり、修正を要する。

### 検証方法 (証跡)

`skills/` の全 Swift コードブロックを 1 ファイルへ写し、`ios/` を path 依存に取ったスクラッチ SwiftPM パッケージとして `generic/platform=iOS Simulator` 向けにビルドした (作業ツリー外の一時領域。リポジトリへの変更なし)。下記 Major 1〜3 以外のコード例はすべてコンパイルに成功しており、推奨修正の形も同じ手順で通ることを確認済み。本 change はコード非改変のため、`ios/` 本体のビルド・テストは対象外とした。

## 指摘事項

### [🟠 Major] cells.md「説明・値・ヒント」のコード例がコンパイルできない (実引数順序)

**該当箇所**: `skills/en/kssettingsview-ios/references/cells.md:226-233` / `skills/ja/kssettingsview-ios/references/cells.md:227-233`

**問題点**: `LabelCell.init` の宣言順は `id, style, title, description, valueText, icon, hintText, isEnabled, isVisible` ([ios/Sources/KsSettingsViewUI/LabelCell.swift:53](../../../ios/Sources/KsSettingsViewUI/LabelCell.swift)) だが、例は `hintText` を `icon` より前に置いている。Swift は既定値つき引数でも宣言順を要求するため `error: argument 'icon' must precede argument 'hintText'` になる (ビルドで確認)。en/ja でコードブロックは byte 一致のため両言語とも壊れている。

**推奨修正**: `icon` と `hintText` の順序を入れ替える (`... valueText: "256 GB", icon: .systemName("externaldrive"), hintText: "Updated today"`)。翻訳ロックステップの要求どおり en/ja 同時に直す。

### [🟠 Major] styling.md「1 行だけ見た目を上書きする」のコード例がコンパイルできない (実引数順序 2 箇所)

**該当箇所**: `skills/en/kssettingsview-ios/references/styling.md:33-41` / `skills/ja/kssettingsview-ios/references/styling.md:34-42`

**問題点**: 同一スニペット内に順序違反が 2 件ある。

1. `LabelCell(title:style:)` — `style` は宣言順で `title` より前 → `error: argument 'style' must precede argument 'title'`
2. `CellStyle(titleColor:backgroundColor:cellHeight:)` — 宣言順は `... iconSize, iconRadius, cellHeight, hintTextColor, hintTextFont, backgroundColor, accentColor` ([ios/Sources/KsSettingsViewUI/CellStyle.swift:51](../../../ios/Sources/KsSettingsViewUI/CellStyle.swift)) のため `cellHeight` が `backgroundColor` より前 → `error: argument 'cellHeight' must precede argument 'backgroundColor'`

この節は `CellStyle` を初めて紹介する導入例であり、最初のコピーで失敗する位置にある。

**推奨修正**: `LabelCell(style: CellStyle(titleColor: .systemOrange, cellHeight: 80, backgroundColor: .secondarySystemGroupedBackground), title: "Highlighted")` の順へ直す (この形でビルド通過を確認済み)。en/ja 同時。

### [🟠 Major] updates.md の Store 所有例がコンパイルできない (`@MainActor` 欠落)

**該当箇所**: `skills/en/kssettingsview-ios/references/updates.md:10-27` / `skills/ja/kssettingsview-ios/references/updates.md:10-27`

**問題点**: `SettingsRootStore` は `@MainActor` 宣言 ([ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:30-31](../../../ios/Sources/KsSettingsViewUI/SettingsRootStore.swift)) だが、例の `final class SettingsModel: ObservableObject` は非隔離のため、その `init()` から `SettingsRootStore(initialRoot:initialTheme:)` を呼べない → `error: call to main actor-isolated initializer 'init(initialRoot:initialTheme:)' in a synchronous nonisolated context`。この節は「Store 方式」の唯一の完全例で、以降の `insertCell` / `replaceCells` などのレシピはすべてこの Store を前提にしている。

なお同ファイルの `appendUser` / `removeLastUser` などの断片は囲む型が省略されているため単体では判定できないが、`store` の操作はすべて main actor 隔離である。Store 方式を扱う節に「Store とその操作は main actor 上で扱う」旨の 1 行が無いこと自体も、利用者が最初に踏む落とし穴になっている。

**推奨修正**: `SettingsModel` に `@MainActor` を付ける (この形でビルド通過を確認済み)。あわせて Store 節のリード文へ「Store と その操作は main actor で扱う」旨を 1 文足すことを推奨する。en/ja 同時。

### [🟠 Major] 「全組み込み Cell が valueText を持つ」は誤り (EntryCell は非公開)

**該当箇所**: `skills/en/kssettingsview-ios/references/cells.md:224` / `skills/ja/kssettingsview-ios/references/cells.md:224`

**問題点**: 「例外は `ButtonCell` で `description` を持たない」だけを例外として挙げているが、`EntryCell` は `valueText` を**持たない**。源泉 concept が明示している契約 ([kasane/concepts/core/cells/input-cells.md](../../concepts/core/cells/input-cells.md):27「`EntryCell` は入力 control 自身が値を表示するため `valueText` を持たず、`text` を使う」、同:62「してはいけないこと: `EntryCell` に `valueText` を追加して二重の入力値 API を作ってはならない」) であり、実装にもフィールドが無い ([ios/Sources/KsSettingsViewUI/EntryCell.swift:29-58](../../../ios/Sources/KsSettingsViewUI/EntryCell.swift))。記述に従って `EntryCell(title:valueText:text:)` と書くと `error: extra argument 'valueText' in call` になる (ビルドで確認)。

**推奨修正**: 例外を 2 つ挙げる形へ直す (「`ButtonCell` は `description` を持たず、`EntryCell` は `valueText` を持たない (入力欄自身が値を表示するため)」)。en/ja 同時。

### [🟡 Minor] 可視性切り替えの「アニメートされる」は契約にない断定

**該当箇所**: `skills/en/kssettingsview-ios/references/updates.md:166` / `skills/ja/kssettingsview-ios/references/updates.md:166`

**問題点**: 「行はその場で再構成されるのではなく追加・削除としてアニメートされる」と書いているが、源泉 concept ([kasane/concepts/core/architecture/display-state-synchronization.md](../../concepts/core/architecture/display-state-synchronization.md) の「同期経路」「full 更新のコストモデル」) が保証しているのは「可視性差を検出したら visible projection を作り直す full 更新へ切り替える」ことまでで、アニメーションの有無は契約されていない。利用者がアニメーションを前提に組むと期待が外れる。

**推奨修正**: 「表示対象の集合を作り直すため、行の内容更新ではなく行の追加・削除として反映される」程度に留める。

### [🟡 Minor] `.disabled(_:)` を「SwiftUI の modifier」と説明している

**該当箇所**: `skills/en/kssettingsview-ios/references/cells.md:240` / `skills/ja/kssettingsview-ios/references/cells.md:240`

**問題点**: no-op なのは本ライブラリが `KsCell` へ生やしている `disabled(_:)` ([ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift:148](../../../ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift)) であって SwiftUI の `View.disabled(_:)` ではない。源泉 concept も「SwiftUI / Compose の `disabled` **Cell** modifier」と書き分けている ([kasane/concepts/core/styling/cell-visual-states.md](../../concepts/core/styling/cell-visual-states.md):27)。現状の書き方だと、Cell に対する `.disabled(_:)` が補完に出てくること自体が読者から見えず、「呼べるが効かない」罠に気づけない。

**推奨修正**: 「Cell に対する `.disabled(_:)` modifier は存在するが no-op で、無効化は `isEnabled` で行う」と書き分ける。

### [🟡 Minor] 再利用 CustomCell の例が content の変化を表示へ反映しない

**該当箇所**: `skills/en/kssettingsview-ios/references/custom-cells.md:89-116` / `skills/ja/kssettingsview-ios/references/custom-cells.md:89-116`

**問題点**: `SliderRow` は `@State private var draft` を `State(initialValue:)` で初期化しており、SwiftUI の仕様上この初期値は同一 View identity の初回生成時にしか使われない。したがって Store 更新など外部から `content.value` が変わっても、スライダー位置と右側の数値 (`Text("\(Int(draft))")`) は追随しない。「表示に効く値は content に入れる」という CustomCell の契約 ([kasane/concepts/core/cells/custom-cell.md](../../concepts/core/cells/custom-cell.md) の「等価性と再バインド」) を形の上では満たしているが、実際の表示は content ではなくローカル state が支配しており、再利用部品の手本としては誤誘導になる。

**推奨修正**: ドラッグ中だけローカル値を使い、非ドラッグ時は `content.value` を表示する形にする (例: `isEditing` フラグを持ち、非編集時は `content.value` を描く)。あるいは節のリード文に「外部からの値変更に追随させたい場合は content 側を表示に使う」旨の 1 行を足す。

### [🔵 Suggestion] `titleAlignment` に言及しているが指定方法を示していない

**該当箇所**: `skills/en/kssettingsview-ios/references/cells.md:41` / `skills/ja/kssettingsview-ios/references/cells.md:41`

**問題点**: 「`titleAlignment` が視覚に出るのは `valueText` を持たない行だけ」という制約だけがあり、値 (`.start` / `.center` / `.end`) も指定例もない。レシピ形式 (やりたいこと → コード) の中で唯一「読んでも書けない」記述になっている。

**推奨修正**: `ButtonCell` の例に `titleAlignment: .start` を足すか、制約の文に取りうる値を併記する。

### [🔵 Suggestion] CustomCell に `icon` modifier が効かないことが書かれていない

**該当箇所**: `skills/en/kssettingsview-ios/references/styling.md:57` / `skills/ja/kssettingsview-ios/references/styling.md:57`

**問題点**: 使える modifier の一覧に `icon` があるが、`CustomCell` はアイコン領域を持たないため型として非対応であり ([kasane/concepts/core/cells/custom-cell.md](../../concepts/core/cells/custom-cell.md) の「DSL による配置」)、実装上も no-op で自身を返す ([ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift:158](../../../ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift))。custom-cells.md 側にも記載がないため、無言で無視される。

**推奨修正**: custom-cells.md の「カスタム行の高さを指定する」節 (CellStyle の効く範囲を説明している箇所) に「`icon` modifier は CustomCell には効かない」を 1 文足す。

### [🔵 Suggestion] PickerCell 単一選択の「確定操作のとき」がやや不正確

**該当箇所**: `skills/en/kssettingsview-ios/references/cells.md:137` / `skills/ja/kssettingsview-ios/references/cells.md:137`

**問題点**: 単一選択には確定操作が無く、候補タップがそのまま 1 回の発火と close になる ([kasane/concepts/core/cells/picker-selection-surface.md](../../concepts/core/cells/picker-selection-surface.md) の「共通の挙動契約」)。「確定操作のとき」と書くと複数選択と同じ確定ボタンがあるように読める。あわせて、非確定 dismiss では callback が発火しないという利用者にとって重要な保証がどちらの節にも無い。

**推奨修正**: 単一選択は「候補をタップした時点で 1 回発火して閉じる」、複数選択は「確定操作で 1 回発火する」と書き分け、いずれかのリード文に「キャンセルで閉じた場合は発火しない」を足す。

## 確認して問題がなかった観点

- **構成** (デルタスペック「Skill 一式の構成」): en/ja とも `SKILL.md` + `references/{cells,updates,styling,custom-cells}.md` の 5 ファイルのみ。規定外ファイルなし。
- **frontmatter** (同「frontmatter の標準準拠」): `name` / `description` / `license` / `metadata.{language,source}` の 4 フィールドのみ。en/ja で `name` 一致、`language` はパスと一致。
- **ja description の英語キーワード**: `settings screen` / `SwiftUI` / `UIKit` / `KsSettingsViewController` / 12 Cell 名 / `SettingsRootStore` / `Theme` / `CellStyle` / モジュール 3 種を含み、発火語として妥当。
- **設計原則** (同「Skill 内容の設計原則」): SKILL.md は 能力マップ表 → 導入 (索引へのコピー手順参照 + 最低バージョン) → 最小動作コード → references 振り分けの構成。references は全節が「やりたいこと見出し + リード文 + 完動コード」。アーキテクチャ解説の読み物なし。旧 `docs/` の章立ての引き継ぎなし。
- **内容規約** ①②③: コードブロック内にコメント 0 件、ローカル絶対パス 0 件、`docs/` / `openspec` への参照 0 件。
- **ツール最低バージョン**: Swift tools 5.10 / iOS 16.0 が [ios/Package.swift](../../../ios/Package.swift):1,12 と一致。
- **内部リンク**: `references/*.md`、`../SKILL.md`、`../../README.md` / `../../README_ja.md` すべて解決。
- **en/ja 等価性**: 全節の見出し・リード文が意味等価。コードブロックは byte 一致。訳語 (Cell / Section / Header / Footer / 行 / 識別子) の揺れなし。
- **コード例と実装の突合** (上記 Major 1〜3 以外): SKILL.md 最小動作コード、cells.md の Cell 12 種すべて、styling.md の Theme 3 種と modifier 連鎖・Section H/F、updates.md の Store 操作 8 種・`ForEach`・`cellID`/`sectionID`・UIKit ホスティング、custom-cells.md の `CustomCell` 4 形・独自 Cell + Renderer + Registry 登録・`DSLReidentifiable` 準拠 — いずれも実 API と一致しコンパイル通過。
- **源泉 concepts の反映** (manifest-draft.json の `targets` 記載 17 concept を 1 件ずつ確認): 上記 Major 4 / Minor 1〜3 / Suggestion 2〜3 に挙げた箇所を除き、各 concept の「保証すること」がレシピの粒度で反映されている。`ios-native-host.md` の view load 復元 (取り付け順序非依存)、`store-and-update-streams.md` の未知 ID no-op と `replaceCells` の 1 バッチ、`structural-changes.md` の move の `to` 解釈と Section 間移動 = remove + insert、`declarative-tree-identity.md` の ForEach key と明示 ID の非併用・1 item 1 要素、`style-resolution.md` の 4 段解決、`cell-row-layout.md` の 48pt 最低行高と title 優先の幅配分、`list-appearance.md` の Modern 箱の適用範囲と Classic の水平成分無視、`custom-cell.md` の再バインド契約と CellStyle の効く範囲、`cell-renderer-registry.md` の独立 Registry と `KsListCellBase` 非継承、`date-picker-selection-surface.md` の `todayText` 非空条件、`public-identifiers.md` のモジュール名 — いずれも正確。
- **足場アーティファクトの非改変**: proposal / spec / tasks / deviation に本レビュー時点で書き換えの形跡なし。
- **deviation.md 記録済みの乖離**: 1 件 (second-opinion 引用のサニタイズ) のみで、本 Skill には関係しない。

## アクションプラン

1. Major 1〜3 のコード例を修正する (実引数順序 3 箇所、`@MainActor` 1 箇所)。en/ja 同時。修正後は実際にコンパイルを通して確認する — 3 件とも人間の目視では見落としやすい型の誤りであり、**他の 3 Skill (android / maui / aiforms-migration) のコード例にも同種の順序違反がある可能性が高いため、Skill 単位レビューの残り 3 本でも同じ検査 (実コンパイル) をかけることを推奨する**。
2. Major 4 (EntryCell の `valueText`) を修正する。
3. Minor 1〜3 を修正する。
4. Suggestion 1〜3 は採否を判断する (いずれも 1〜2 文の追記で済む)。
5. 修正後、tasks.md 4.1 の機械検査 (特に③コードブロック byte 一致・②節構成一致) を再実行する。
