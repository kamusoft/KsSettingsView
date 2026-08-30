# samples-ios Specification

## Purpose
TBD - created by archiving change add-samples-ios. Update Purpose after archive.
## Requirements
### Requirement: iOS Sample アプリの存在

`samples/ios/` 配下に SwiftUI ベースの Sample アプリが Xcode プロジェクト形式で存在しなければならない (SHALL)。Sample アプリは `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` を依存し、Xcode（16+）から開いて iOS シミュレータ（iOS 16+）で起動可能でなければならない (MUST)。

#### Scenario: Xcode プロジェクトの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `samples/ios/` 配下を確認する
- **THEN** `KsSettingsViewSample.xcodeproj`（または同等の Xcode プロジェクト）と SwiftUI App エントリポイント（`@main struct ... : App`）を含む Swift ソースファイルが存在する

#### Scenario: KsSettingsView パッケージへの依存

- **GIVEN** `samples/ios/KsSettingsViewSample.xcodeproj` を Xcode で開く
- **WHEN** プロジェクト設定の `Frameworks, Libraries, and Embedded Content` を確認する
- **THEN** リポジトリルート相対の `ios/Package.swift` が Local Swift Package として参照され、`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の 3 ターゲットがリンクされている

#### Scenario: シミュレータでの起動

- **GIVEN** Xcode でプロジェクトを開いた状態
- **WHEN** iPhone シミュレータをターゲットに `⌘R`（Run）を実行する
- **THEN** ビルドが成功し、シミュレータ上で Sample アプリが起動する

### Requirement: 基本 Cell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`KsSettingsViewSwiftUI`）を使い、本体公開の `LabelCell` を 1 セクション・複数行含む `SettingsRoot` を `SettingsRootStore` 経由で描画しなければならない (SHALL)。Sample アプリは `SettingsRootStore` の動的更新メソッド（最低でも `insertCell` / `removeCell`）を呼び出すボタン（または相当の UI）を提供し、部分更新の動作確認ができなければならない (MUST)。さらに、`add-cell-types-basic` で導入された 7 種の基本 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）を 1 画面に並べて目視確認できるデモ画面を別途提供しなければならない (SHALL)。

「基本 Cell 7 種デモ画面」は **Cell タイプ別のセクション構成** で実装されなければならない (MUST)。各セクションのセクション名は **Cell タイプ名そのもの**（例: `"CommandCell"`、`"LabelCell"`）とし、後述する Android 版と一字一句揃えなければならない (MUST)。

- **MAUI 互換 Theme の明示渡し** — `KsSettingsView { ... }.theme(_:)` modifier として MAUI 互換の Theme を渡す。**Theme 構築は `UIColor` 直接構築でなければならない (MUST)。`KsColor` は使ってはならない (MUST NOT)**。最低限以下のフィールドを `UIColor(red:green:blue:alpha:)` 形式で指定する：
  - `viewBackgroundColor = UIColor(red: 0xF2/255, green: 0xEF/255, blue: 0xE6/255, alpha: 1)`（PaleBackColorPrimary 相当）
  - `cellBackgroundColor = UIColor.white`
  - `separatorColor = UIColor(red: 0xE6/255, green: 0xDA/255, blue: 0xB9/255, alpha: 1)`（DisabledColor 相当）
  - `selectedColor = UIColor(red: 0xFF/255, green: 0xBF/255, blue: 0x00/255, alpha: 0x50/255)`（AccentColor の半透明 30%）
  - `cellAccentColor = UIColor(red: 0xFF/255, green: 0xBF/255, blue: 0x00/255, alpha: 1)`（AccentColor）
  - `titleColor = UIColor(red: 0xCC/255, green: 0x99/255, blue: 0x00/255, alpha: 1)`（TitleTextColor）
  - `headerTextColor = UIColor(red: 0xCC/255, green: 0x99/255, blue: 0x00/255, alpha: 1)`（TitleTextColor）
  - `headerBackgroundColor = UIColor(red: 0xF2/255, green: 0xEF/255, blue: 0xE6/255, alpha: 1)`
  - `footerTextColor = UIColor(red: 0x99/255, green: 0x99/255, blue: 0x99/255, alpha: 1)`（PaleTextColor）
  - `hasUnevenRows = true`
  - `disabledTextColor = UIColor(red: 0x99/255, green: 0x99/255, blue: 0x99/255, alpha: 1)`

- **Section 構成（Cell タイプ別、この順序）** — 各セクションのセクション名は Cell タイプ名そのもの、各 Cell 数は 1〜3（RadioCell のみ最低 2 必須）：

  1. **`"CommandCell"` セクション**（3 個）
     - Cell 1: フル構成（`icon = KsImage.systemName("person.crop.circle")`、`title = "Tanaka Taro"`、`description = "tanaka.taro@example.com"`、`CellStyle(cellHeight: 80)`、`onTap` 有り）
     - Cell 2: シンプル（`title = "プロフィール"`、`onTap` 有り）
     - Cell 3: 中間（`title = "通知設定"`、`valueText = "オン"`、`onTap` 有り）
  2. **`"LabelCell"` セクション**（2 個）
     - Cell 1: フル構成（`icon = KsImage.systemName("externaldrive")`、`title = "Storage"`、`description = "This is description. you can write detail explanation of the item here. long text wrap automatically."`、`valueText = "256 GB"`）
     - Cell 2: シンプル（`title = "バージョン"`、`valueText = "1.0.0"`）
  3. **`"SwitchCell"` セクション**（1 個）
     - Cell 1: `title = "Notification"`、`description = "This is description. you can write detail explanation of the item here. long text wrap automatically."`、`isOn = true`
  4. **`"CheckboxCell"` セクション**（1 個）
     - Cell 1: `title = "Agree to Terms"`、`isChecked = true`
  5. **`"RadioCell"` セクション**（2 個、最低 2 必須）
     - Cell 1: `title = "TypeA"`、`groupId = "type"`、`isSelected = true`
     - Cell 2: `title = "TypeB"`、`groupId = "type"`、`isSelected = false`
     - footer テキスト: `"You can select either TypeA or TypeB."`
  6. **`"SimpleCheckCell"` セクション**（3 個）
     - Cell 1〜3: `title = "Item 1"` / `"Item 2"` / `"Item 3"`
  7. **`"ButtonCell"` セクション**（1 個）
     - Cell 1: `title = "ログアウト"`、`titleAlignment = .center`（既定）

- 画面上には「最後にタップ: ...」の現在値表示など、状態確認用の補助 UI を任意で配置してよい。

#### Scenario: 起動時の画面表示

- **GIVEN** Sample アプリがシミュレータで起動した直後
- **WHEN** トップメニューから「Store 方式デモ」を選択する
- **THEN** `KsSettingsView` が画面いっぱいに表示され、`LabelCell` の `title` を含む 1 行のセルが複数行（2 行以上）描画される

#### Scenario: Section ヘッダ・フッタの描画

- **GIVEN** Sample アプリの Store 方式デモ画面
- **WHEN** 描画されたセクションを確認する
- **THEN** `SectionAccessory.text(...)` 形式のヘッダおよびフッタが、対応する文字列でセクション境界に表示される

#### Scenario: SettingsRootStore + SwiftUI ラッパの使用

- **GIVEN** Sample のソースコードを参照する
- **WHEN** Store 方式デモ画面（例: `StoreDemoView`）の本文を確認する
- **THEN** 当該 View 内に `@StateObject private var store: SettingsRootStore = SettingsRootStore(initialRoot: ...)` が宣言されており、`KsSettingsView(store: store)` を `body` から返している。`store` の初期 root は `SettingsRootBuilder` / `SectionBuilder` の DSL（`SettingsRoot { Section { ... } }` 形式）で構築されている

#### Scenario: Cell 追加ボタンの動作

- **GIVEN** Sample アプリの Store 方式デモ画面に「項目追加」ボタンが存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.insertCell(LabelCell(title: "新規 \(index)"), in: firstSectionID, at: 末尾)` が呼ばれ、画面に新しい Cell 行が挿入アニメーションで追加される

#### Scenario: Cell 削除ボタンの動作

- **GIVEN** Sample アプリの Store 方式デモ画面に「項目削除」ボタンが存在し、削除可能な Cell が複数存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.removeCell(cellID: ...)` が呼ばれ、対応する Cell 行が削除アニメーションで消える

#### Scenario: Root H/F の指定（View modifier）

- **GIVEN** Sample の Store 方式デモ画面または DSL 方式デモ画面
- **WHEN** Root Header を表示する場合のコードを確認する
- **THEN** `KsSettingsView(store: store).rootHeader("...")` のように View modifier 形式で Root H/F が指定される

#### Scenario: 基本 Cell 7 種デモ画面の存在

- **GIVEN** Sample アプリのトップメニュー
- **WHEN** 「基本 Cell 7 種デモ」ナビゲーションリンクを選択する
- **THEN** Cell タイプ別の 7 セクションが順に描画され、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は状態切替が可能、`CommandCell` / `ButtonCell` はタップで `onTap` が発火する

#### Scenario: MAUI 互換 Theme の適用（UIColor 直接構築）

- **GIVEN** 基本 Cell 7 種デモ画面のソースコード
- **WHEN** Theme 構築箇所を確認する
- **THEN** すべての色フィールドが `UIColor(red: ..., green: ..., blue: ..., alpha: ...)` 形式（または `.white` などの組み込み色）で構築されており、`KsColor(red: ..., green: ..., blue: ..., alpha: ...)` 形式は一切使われていない

#### Scenario: MAUI 互換 Theme の表示反映

- **GIVEN** 基本 Cell 7 種デモ画面
- **WHEN** 起動して画面を観察する
- **THEN** 全体の背景がベージュ系（`#F2EFE6`）が **セクション間の隙間も含めて画面全体に反映**され、セル背景が白、セクションヘッダ文字が黄系（`#CC9900`）、SwitchCell / CheckboxCell の ON / Checked 色が `#FFBF00` 系で表示される

#### Scenario: 長文 Description の折返し

- **GIVEN** 基本 Cell 7 種デモ画面、`hasUnevenRows = true` が設定されている
- **WHEN** SwitchCell または LabelCell に長文 description（`"This is description. you can write detail explanation of the item here. long text wrap automatically."` 相当）が指定されている
- **THEN** description は折返して 2 行以上で表示され、当該 Cell の高さは他 Cell よりも大きくなる

#### Scenario: タッチフィードバックの目視確認

- **GIVEN** 基本 Cell 7 種デモ画面で `Theme.selectedColor = UIColor(red: 1, green: 0.75, blue: 0, alpha: 0.3)` 相当
- **WHEN** ユーザーが CommandCell や LabelCell をタップして指を離さない
- **THEN** タップ中に背景色が橙の半透明色に変化し、リリース後に元に戻る

#### Scenario: CommandCell.icon と CellStyle.cellHeight の反映

- **GIVEN** プロフィール風 CommandCell（CommandCell セクションの Cell 1）が `icon = KsImage.systemName("person.crop.circle")` と `CellStyle(cellHeight: 80)` を持つ
- **WHEN** 表示される
- **THEN** 当該 Cell の高さが他 Cell よりも大きく `80pt` 程度になり、icon が左端に表示される

#### Scenario: Section 構成の Cell タイプ別並び

- **GIVEN** 基本 Cell 7 種デモ画面のソースコード
- **WHEN** ソースを参照する
- **THEN** Section は `"CommandCell"` → `"LabelCell"` → `"SwitchCell"` → `"CheckboxCell"` → `"RadioCell"` → `"SimpleCheckCell"` → `"ButtonCell"` の順で並ぶ。各セクション名は Cell タイプ名そのものである

#### Scenario: iOS / Android 間の表記揃え

- **GIVEN** iOS 版と Android 版の基本 Cell 7 種デモ画面
- **WHEN** 両方の画面のセクション名・Cell タイトル・Cell description・Cell valueText・Footer テキストを比較する
- **THEN** すべての文字列が一字一句一致する（差分はゼロ）。**Theme の色値も hex 表現で同じ値（例: `0xF2EFE6`）を使い、iOS 側は `UIColor(red: 0xF2/255, ...)`、Android 側は `Color(0xFFF2EFE6)` のように記述形式は異なるが論理値は一致する**

#### Scenario: Sticky Footer の不在

- **GIVEN** RadioCell セクションが Footer テキスト `"You can select either TypeA or TypeB."` を持ち、画面をスクロールする
- **WHEN** Footer が画面下端を下回ろうとする
- **THEN** Footer は画面下端に固定されず、通常スクロールに従って画面外に出る（Sticky 不採用）

#### Scenario: Section Header / Footer の無駄余白なし

- **GIVEN** Section Header テキストおよび Footer テキストが指定されていないセクション
- **WHEN** 表示される
- **THEN** そのセクションには Header / Footer の supplementary 領域が生成されず、Section 境界の上下に不要な余白が発生しない

#### Scenario: Section.headerHeight 明示指定のサンプル

- **GIVEN** 基本 Cell 7 種デモ画面のソースコード
- **WHEN** ソースを参照する
- **THEN** 少なくとも 1 つのセクションが `headerHeight = 60`（または明示的な正値）を渡しており、当該セクションのヘッダが固定高さで描画され、その他の自動高さセクションと見た目が明確に異なることが確認できる

### Requirement: README の整備

`samples/ios/README.md` は、`add-monorepo-foundation` で配置された placeholder から、実 Sample アプリのクイックスタート README に置き換えられていなければならない (SHALL)。

#### Scenario: クイックスタートの記載

- **GIVEN** `samples/ios/README.md` を開く
- **WHEN** その内容を確認する
- **THEN** 「概要」「必要環境（Xcode 16+ / iOS 16+ シミュレータ）」「開き方（Xcode でプロジェクトを開く手順）」「実行手順（Run / `xcodebuild`）」「ディレクトリ構成」「関連リンク」のいずれにも該当する記載が含まれている

#### Scenario: placeholder からの置き換え

- **GIVEN** `samples/ios/README.md`
- **WHEN** その内容を確認する
- **THEN** 「後続変更提案で追加予定」等の placeholder 文言は残っておらず、実 Sample 用のクイックスタートに更新されている

#### Scenario: 本体ライブラリのデバッグ手順の記載

- **GIVEN** `samples/ios/README.md`
- **WHEN** その内容を確認する
- **THEN** 「本体ライブラリのデバッグ」セクションが存在し、本 Sample が Local Swift Package 参照によって本体ソース（`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`）にブレークポイントを置いてステップインできる旨と、本体テストを主軸に走らせる場合は `ios/Package.swift` を直接 Xcode で開く運用が併記されている

### Requirement: アプリのメタデータ

Sample アプリは、Bundle Identifier プレフィックスとして `jp.kamusoft.kssettingsview.samples.ios` を使用しなければならない (SHALL)。Deployment Target は iOS 16.0 以上、Swift 言語バージョンは 6 でなければならない (MUST)。

#### Scenario: Bundle Identifier の確認

- **GIVEN** Sample アプリのビルド設定
- **WHEN** `PRODUCT_BUNDLE_IDENTIFIER` を確認する
- **THEN** `jp.kamusoft.kssettingsview.samples.ios` で始まる識別子が設定されている

#### Scenario: Deployment Target の確認

- **GIVEN** Sample アプリのビルド設定
- **WHEN** `IPHONEOS_DEPLOYMENT_TARGET` を確認する
- **THEN** `16.0` 以上の値が設定されている

