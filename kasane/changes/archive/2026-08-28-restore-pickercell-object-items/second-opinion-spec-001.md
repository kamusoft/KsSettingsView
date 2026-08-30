# セカンドオピニオン: restore-pickercell-object-items (spec-001)

**相方**: codex / **label**: so-spec-restore-pickercell-object-items / **日付**: 2026-08-28 / **対象**: 提案一式 (proposal.md / design.md / specs/ 8 capability / tasks.md / ui/brief.md)

---

# レビュー結果: restore-pickercell-object-items

**日付**: 2026-08-28
**判定**: **NEEDS_DISCUSSION**

## サマリー

`PickerItem` 単型パイプラインと「選択の正は index」という中核方針は core/ADR-0029 と整合しています。一方、公開 API の成立条件、nullable な副表示の wire 表現、MAUI の重複・null・可変コレクションの意味論など、実装前に決定が必要な穴があります。

静的レビューのみ実施し、依頼どおりビルド・テスト・ファイル書き込みは行っていません。

**件数**: Critical 0 / Major 7 / Minor 3 / Suggestion 0

## 指摘事項

### [🟠 Major] Swift の「型制約なし」が既存 `@Sendable` callback と両立しない

**該当箇所**: `design.md:19`、`specs/cell-types-input/spec.md:46`、`ios/Sources/KsSettingsViewUI/PickerCell.swift:49`

**問題点**:
ジェネリック縁は元の `[T]` を捕捉し、index callback から `T` を返す closure を組み立てます。しかし現在の `onSelectionChanged` は `@Sendable` です。Swift 6 の厳格並行性では、制約なしの `T` を含む配列を `@Sendable` closure が捕捉できません。`selectedItem` 経路の `T: Equatable` も Sendable 性を保証しないため、仕様どおりの制約で安全に実装できません。

**推奨修正**:
次のいずれかを設計として確定してください。

- Swift の object callback 経路を `T: Sendable`、TwoWay 経路を `T: Equatable & Sendable` とする。
- 選択 callback を `@MainActor` 隔離へ変更し、`@Sendable` を要求しない。
- 別の安全な保持方式を採用する。

`@unchecked Sendable` による隠蔽は正式な解決策にしないことも明記すべきです。

### [🟠 Major] 公開するジェネリック API のシグネチャと配置が決まっていない

**該当箇所**: `design.md:17`、`design.md:87`、`tasks.md:7`、`tasks.md:15`

**問題点**:
「iOS generic init」「Android generic factory」「String 特殊化」までしか規定されておらず、公開 API の名前・module・引数構成が確定していません。特に Android の `MutableState` 経路は現在 `ks-settingsview-compose` にありますが、Store/callback 経路は `ks-settingsview-ui` 側です。「factory」がどちらへ属するか不明です。

このままでは、既知リスクとして挙げられた Swift overload 解決・Kotlin 型推論を実装前に評価できず、Scenario も具体的な呼び出し形を固定できません。

**推奨修正**:
少なくとも次の署名マトリクスを design に確定してください。

- iOS: 生の `PickerItem`、index Binding、object Binding、Store callback、複数選択
- Android UI: 生の constructor、型制約なし object callback factory
- Android Compose: `MutableState<Int?>` / `MutableState<T?>` / `MutableState<Set<Int>>`
- 各プラットフォームの String 特殊化
- index 書き戻しと object callback を併用する場合の実行順

併せて、各呼び出し形がコンパイルできることを固定するテストを tasks に追加してください。

### [🟠 Major] nullable な副表示の bridge wire 表現が未決定

**該当箇所**: `specs/maui-bridge/spec.md:7`、`tasks.md:35`、`design.md:42`、`ios/Sources/KsSettingsViewBridge/KsBridgePickerCell.swift:22`

**問題点**:
デルタスペックは「主表示と副表示のペア列」、tasks は「副表示列を追加」と異なる表現を使っています。`subText` は nullable ですが、現行 iOS DTO は Objective-C 互換の `[String]` しか輸送していません。

未決事項には以下があります。

- per-item DTO と平行配列のどちらにするか
- `nil` と空文字列を区別するか
- 平行配列なら件数不一致をどう扱うか
- Objective-C/.NET binding で nullable 要素をどう表現するか

空文字列を `nil` の sentinel にすると、「非 nil なら副表示行を持つ」という UI 契約も変わります。

**推奨修正**:
Swift/Kotlin/C# の具体的な wire 型と不変条件を design/spec に追加してください。`nil`・空文字列・混在候補を往復させる Scenario も必要です。

### [🟠 Major] MAUI のリフレクション射影が全入力に対して定義されていない

**該当箇所**: `design.md:69`、`specs/maui-cells/spec.md:5`

**問題点**:
`IList` は null 要素を含められます。また、指定したプロパティについて以下の結果が決まっていません。

- プロパティ値が null
- プロパティ値が string 以外
- write-only、indexer、static、非 public プロパティ
- getter が例外を送出
- 要素自体が null

特に `PickerItem.text` は非 nullable なので、主表示へ変換できない値の扱いが必須です。また null 候補を許すと、`SelectedItem == null` が「未選択」と「null 要素の選択」を区別できません。

**推奨修正**:
null 要素を禁止するか、選択を含む完全な意味論を定義してください。併せて、解決対象を「public instance の引数なし readable property」などに限定し、null/non-string 値の文字列化、getter 例外の扱いを Requirement と Scenario へ追加してください。

### [🟠 Major] `SelectedItems` の値等価逆引きは重複候補に対して一意に定まらない

**該当箇所**: `design.md:30`、`design.md:75`、`specs/maui-cells/spec.md:31`

**問題点**:
Decision 2 は、同値重複による情報欠落を理由に Native core の object 集合 TwoWay を却下しています。一方、MAUI では同じ問題を持つ `SelectedItems` → `IndexOf` 逆引きを導入していますが、解決規則がありません。

例えば同値な候補が index 0 と1にあり、`SelectedItems` に同値要素を2件設定すると、単純な `IndexOf` では両方が0へ解決され、集合化で1件が失われます。さらに次も未規定です。

- 最初の未使用 index を順番に消費するか
- 同値な全 index を選択するか
- 重複候補自体を禁止するか
- `SelectedItems = null` と空リストの正規形
- `SelectedIndices` の範囲外要素しかない場合の null/空

**推奨修正**:
MAUI の重複解決アルゴリズムと null/空の正規形を明記し、同値候補・同じ参照の重複・一部未解決を含む Scenario を追加してください。

### [🟠 Major] 可変な元コレクションと表示 snapshot の対応が崩れ得る

**該当箇所**: `specs/cell-types-input/spec.md:10`、`specs/maui-cells/spec.md:7`、`design.md:73`、`kasane/decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md:49`

**問題点**:
射影は一度だけ行い、MAUI ではコレクション自身の増減を観測しない契約です。しかし Kotlin の `List<T>` と C# の `IList` は、外部から同じ実体を変更できます。

表示側が古い射影のまま、選択確定時だけ現在の可変リストを index で参照すると、画面で選んだ項目とは別の object が callback/`SelectedItem` に返ります。現行 MAUI 実装も選択解決時に生の `ItemsSource` を参照しています（`maui/KsSettingsView.Maui/PickerCell.cs:261`）。

**推奨修正**:
ItemsSource 設定時に「元 object snapshot」と「PickerItem snapshot」を同時に materialize し、次の差し替えまで同じ snapshot で表示と逆引きを行う契約にしてください。少なくとも、元リストを in-place 変更してから選択した場合の Scenario が必要です。この方針は、無関係な snapshot 更新で getter を再評価しないという design の性能意図も担保します。

### [🟠 Major] リフレクション getter の AOT・trimming 成立条件が設計されていない

**該当箇所**: `design.md:73`、`design.md:90`、`tasks.md:40`、`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:13`

**問題点**:
MAUI facade は `net10.0-ios` を対象にしますが、設計は文字列名による reflection と「コンパイル済み getter」を要求するだけです。iOS AOT 環境での Expression getter の生成方式と、文字列でしか参照されない model property の trimming 保全方法がありません。`net10.0` の unit test が通っても、Release iOS で property metadata が失われる可能性を判定できません。

**推奨修正**:
以下を事前に決めてください。

- AOT 対応 getter の生成方式
- model property の trimming 保全を利用者契約にするか、ライブラリ側で担保するか
- 未保全時の診断方法
- iOS/Android Release 構成で実モデルを射影する統合検証

### [🟡 Minor] Native `selectedItem` TwoWay の書き戻し Scenario がない

**該当箇所**: `specs/cell-types-input/spec.md:44`、`tasks.md:9`、`tasks.md:17`

**問題点**:
Requirement は TwoWay binding を要求していますが、Scenario は初期逆引き・重複・候補外だけです。ユーザー確定時に Swift `Binding<T?>` / Kotlin `MutableState<T?>` が更新されるという中心経路が、Scenario として検証可能になっていません。

**推奨修正**:
「object TwoWay で構築し、候補を確定すると binding/state が対応する元要素へ1回更新される」Scenario を追加してください。

### [🟡 Minor] 副表示導入後の初期スクロールを判定する Scenario がない

**該当箇所**: `specs/settings-view-ios-ui/spec.md:7`、`specs/settings-view-android-ui/spec.md:7`、`tasks.md:23`、`tasks.md:29`

**問題点**:
両 spec の Requirement は初期スクロール維持を要求しますが、対応 Scenario がありません。特に Android は混在行高と折り畳み高さが明示的なリスクなのに、task 4.4 のテスト観点にも初期スクロールが列挙されていません。

**推奨修正**:
副表示あり/なしが混在し、選択 index が初期表示範囲外にあるケースで、選択行が表示された状態で開く Scenario を両 OS に追加してください。

### [🟡 Minor] 承認 mock 内で行高の説明と実体が矛盾している

**該当箇所**: `ui/mock/plan-a-subtext-single-line.html:43`、`ui/mock/plan-a-subtext-single-line.html:54`、`ui/brief.md:9`

**問題点**:
mock は「全行の行高が一定」と説明していますが、CSS は `min-height` のみで、副表示あり行は2行、なし行は1行のため高さが変わります。approved.png でも副表示なし行は短く見えます。brief/design の「副表示なしは従来の1行構成」とは後者が整合します。

**推奨修正**:
「長い副表示でも副表示あり行の高さが一定」という意味なら、その表現に直してください。本当に全行同高なら CSS・brief・混在 Scenario を揃えて再承認が必要です。

## アクションプラン

1. Swift の隔離/Sendable 条件と、両 platform の公開シグネチャ表を確定する。
2. bridge の nullable 副表示 wire 型を確定する。
3. MAUI の null・反射変換・重複逆引き・null/空正規形を確定する。
4. 元 object と表示項目を同一 snapshot として扱う契約を追加する。
5. MAUI AOT/trimming 方針を確定する。
6. TwoWay 書き戻し・初期スクロール・重複/null/wire の Scenario を追加し、mock の行高説明を整合させる。

---

## 突き合わせ結果

ホスト側自己レビュー (Step 8、指摘 0 件) との突き合わせ。全 10 件が相方のみの指摘で、いずれも該当箇所の特定と実害シナリオを伴う根拠強と判定 — **10 件全て採用** (ホスト側の見逃しとして扱う)。

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| M1 | Swift `@Sendable` と型制約の両立 | 採用 | design Decision 1 に iOS の型制約 (callback 経路 `T: Sendable` / TwoWay `T: Equatable & Sendable`) を明記 |
| M2 | 公開シグネチャ・module 配置の未確定 | 採用 | design Decision 1 にシグネチャ一覧と配置 (Android: callback/生 = ui、MutableState = compose — 実在確認済み) を追加。tasks にコンパイル固定テスト |
| M3 | wire の nullable 副表示表現 | 採用 | design Decision 7 (新設): per-item DTO (text + nullable subText)、空文字は副表示なしへ正規化。bridge spec に往復 Scenario |
| M4 | MAUI リフレクションの全入力意味論 | 採用 | design Decision 6 を拡張 (解決対象の限定・null 要素禁止・非 string 値の文字列化・getter 例外伝播)。spec に Requirement/Scenario 追加 |
| M5 | `SelectedItems` 重複逆引きの規則 | 採用 | design Decision 6 を拡張 (最初の index + 重複除去 + 正からの再導出で揃う、null/空の正規形)。Scenario 追加 |
| M6 | 可変コレクションと snapshot の不一致 | 採用 | design Decision 8 (新設): 縁 / facade は設定時に元要素列を snapshot し、表示と逆引きが同一 snapshot を参照。Scenario 追加 |
| M7 | AOT / trimming | 採用 | design Decision 6 を拡張 (`PropertyInfo.GetValue` ベースの AOT 安全実装 + trimming 保全は利用者契約、未保全は ToString() フォールバックで観測可能)。Risks 追記 |
| m1 | selectedItem TwoWay 書き戻し Scenario 欠落 | 採用 | cell-types-input に Scenario 追加 |
| m2 | 初期スクロール Scenario 欠落 | 採用 | ios/android spec に Scenario 追加、task 4.4 の観点に追記 |
| m3 | mock の行高説明の矛盾 | 採用 | 説明文を「副表示あり行の行高が一定」へ修正 (視覚実体は不変)、approved.png 再撮影 |

降格・未解決: なし。M4 (null 要素禁止 = 例外)・M4 (getter 例外伝播)・M7 (trimming 利用者契約) の3点は設計判断を伴うため、反映後にオーナーへ確認する。
