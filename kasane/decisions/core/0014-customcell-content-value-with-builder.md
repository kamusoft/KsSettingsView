---
id: 0014
title: CustomCell は content 値 + builder クロージャで表現し等価性は content のみに置く
status: accepted
date: 2026-08-03
---

## Context

openspec 時代の未実施 change `add-cell-types-custom`（任意ビューをセル化する CustomCell の追加提案）を kasane change として再起票するにあたり、CustomCell が「コンテンツをどう持つか」を再検討した。

旧 proposal は「Content 型を `KsCellRegistry` に事前登録し、`registerSwiftUICustomCell` / `registerUIViewCustomCell`（iOS）、`registerComposeCustomCell` / `registerViewCustomCell`（Android）の 2 系統登録 API を新設する」設計を前提としていたが、現行の Registry は iOS / Android とも単一の `register` API のみであり、この前提はコードに存在しない。

一方、旧 proposal 以後に H/F 装飾領域向けの `KsAnyView` 型消去ラッパが本実装され、`UIHostingConfiguration` による描画・`ComposeView` キャッシュ + `DisposeOnDetachedFromWindow` のライフサイクル管理が実運用されている。ただし `KsAnyView` は Decision 3（`refactor-accessory-and-root-hf` design）により**意図的に等価性へ参加しない**（Swift は `Equatable` / `Hashable` 非準拠、Kotlin は参照同一性のまま）。

また AiForms.Maui.SettingsView の `CustomCell` 実装には「Content が生成する NativeView をセル数分保持しており仮想化されていない」というパフォーマンス上の既知課題が TODO コメントとして明記されており、これは避けるべき失敗パターンである。

## Decision

CustomCell は **content 値 + View ビルダクロージャ** を保持する形（探索時の呼称: 新 C 案）で表現する。

- CustomCell は利用者定義の content 値（データ）と、content から View を生成するビルダクロージャを併せ持つ（利用イメージ: `CustomCell(content: X) { x in ... }`）
- **等価性（equals / hashCode / Hashable）は content 値と表示に効くスカラー（id / style / showArrow / isEnabled / isVisible）で計算し、関数値（builder / onTap）は除外する**。これにより差分検出（replaceCells / diffable snapshot）は「表示に効く値が変わったセルだけ」を再バインド対象にできる。（2026-08-03 改訂: 初版の「content 値のみ」という文言を、探索・論点2b の決定「表示に効くスカラーも参加」および design の具体化に合わせて明確化。second-opinion-001 指摘 #1 による）
- 再バインドの実体は、リサイクルされた同一の `ComposeView` / hosting 構成へのビルダ再適用（再コンポーズ / SwiftUI 差分更新）とする。ネイティブ容れ物（ViewHolder / `UICollectionViewCell`）はリサイクルで再利用する
- データを持たない静的コンテンツ向けに content 省略の糖衣（`CustomCell { ... }`、内部的には content = 空値の同一機構）を提供する
- `KsCellRegistry` の拡張（CustomCell 専用登録 API の新設）は行わない

### 用語定義（カスタムセルの3層）

利用者が独自 UI のセルを得る手段は3層あり、次の用語で呼び分ける:

| 層 | 用語 | 内容 |
|---|---|---|
| ① | **CustomCell**（インライン利用） | その場構築・その場専用。DSL に直書き（本 ADR の対象） |
| ② | **CustomCell**（ラップ関数による再利用） | 固定ビルダ + content 型を与えた CustomCell を返す関数（例: `SliderCell(...)`）。登録不要で、①が実装されれば自動的に手に入る |
| ③ | **UserDefinedCell**（利用者定義 Cell） | 自前 Cell 型 + Renderer + `register` による一級市民セル。ADR-0013 で確立済みの既存拡張経路 |

AiForms.Maui.SettingsView の「`CustomCell` 直置き」は①、「`CustomCell` を継承したサブクラス定義（`SliderCell` 等）」は②に対応する。③は独自の型 identity・スタイル参加・描画の完全制御が必要な場合に選ぶ。`UserDefinedCell` はコード上の型名ではなくドキュメント・概念上の呼び名（実体は利用者が書く具象 Cell が `Cell` / `KsCell` に準拠したもの）。

## Alternatives Considered

- **A 案（旧 openspec proposal 型）: Content 型を Registry に事前登録する 2 系統登録 API**。content 値の差分で再描画でき、データと View の分離は最も綺麗。しかし 2 系統 Registry API の新設とジェネリック型消去の再設計が必要で実装リスクが高く、事前登録が冗長で API の使い心地が悪い。さらに現行アーキテクチャでは「利用者が自前 Cell 型 + Renderer を書いて単一 `register` で登録する」拡張経路が既に開いており（ADR-0013）、型登録方式はこの既存手段と役割が重複するため採用しない。
- **B 案: `KsAnyView` を Cell に直接持たせる**。H/F で実証済みの描画基盤に乗れて実装リスクは最小だが、`KsAnyView` は Decision 3 により等価性へ参加しないため、ツリー再構築のたびに「内容が変わっていなくても変更あり」と判定され再バインドの無駄打ちが構造的に発生する。さらに `uiKit` / `AndroidView` backing のファクトリは呼ばれるたび新規ネイティブ View を返す契約であり、再バインドごとのネイティブ View 再生成は AiForms 本家の「Content 仮想化未対応」と同じ失敗パターンに近づく。H/F（個数が少なく更新が稀）では正解だった設計を、スクロールで大量にリサイクルされる Cell 本体へ流用するのは筋が悪いため採用しない。

## Consequences

- 差分検出・リサイクルと整合するデータ駆動の CustomCell になり、AiForms 本家の仮想化未対応問題を設計段階で回避できる。
- 事前登録なしで DSL に直接書ける（Registry 拡張が不要）。
- Cell 本体（content + builder、等価性あり）と装飾領域（`KsAnyView`、等価性なし）で語彙が分かれる。`KsAnyView` は H/F 装飾領域専用にとどまる。
- ビルダクロージャは等価性に参加しないため、「同じ content で異なる見た目にしたい」場合は content 側に見た目を左右する値を含める必要がある。
- AiForms の「CustomCell 継承による再利用」ユースケースの大半は②（ラップ関数）で登録なしに賄えるため、③（UserDefinedCell）はネイティブ Renderer レベルの制御が必要な場合に限られる。②と③の使い分け指針は concepts 側で文書化する。
- AiForms 由来の挙動プロパティ（IsSelectable / LongCommand 等）の引き継ぎ範囲、コンテンツサイズ変化時の高さ再計測は本 ADR のスコープ外（探索の別論点として継続）。

出典: `openspec/changes/add-cell-types-custom/proposal.md`

出典: `ios/Sources/KsSettingsViewCore/KsAnyView.swift`（Decision 3 の等価性非参加コメント）

出典: `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/KsAnyView.kt`（等価性契約コメント）

出典: `kasane/decisions/core/0013-extensible-cell-abstractions.md`（Registry を介した外部拡張経路）

出典: AiForms.Maui.SettingsView `Native/iOS/Cells/CustomCellContent.cs` の仮想化未対応 TODO コメント（2026-08-03 調査）

出典: 2026-08-03 探索セッションでのユーザー判断「OK、新C案で行こう」

出典: 2026-08-03 探索セッションでのユーザー判断（カスタムセルは「即時/ラムダ型」と「事前定義・再利用型」の2種があるとの整理、および③の英語名 `UserDefinedCell` の確定）

出典: 2026-08-03 ユーザー判断「ADRは昇格でOK」

出典: 2026-08-03 等価性文言の改訂（second-opinion-001 指摘 #1、ユーザー承認「ADR-0014 を改訂」）
