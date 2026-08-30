---
id: 0016
title: CustomCell の内部表現は iOS が型消去内蔵の非ジェネリック struct、Android がジェネリック class
status: accepted
date: 2026-08-03
---

## Context

ADR-0014 は CustomCell を「content 値 + builder クロージャ、事前登録なしで DSL に直接書ける」形と定めたが、その内部表現には両プラットフォームの Registry 解決キーの差という制約がある:

- iOS `KsCellRegistry` は `ObjectIdentifier(type(of: cell))` をキーに Renderer 型を解決する。Swift のジェネリック struct は実体型ごとに別の metatype になるため、`CustomCell<Content>` をそのまま登録キーにすると実体型ごとの事前登録が必要になり、ADR-0014 の「事前登録なし」と矛盾する
- Android の Registry は `cellClass` (KClass) キーで、ジェネリクスは実行時消去されるため単一クラス登録で済む

## Decision

内部表現をプラットフォームごとに変える。

- **iOS**: 非ジェネリック `public struct CustomCell` が content を `AnyHashable`、builder を `(AnyHashable) -> AnyView` へ init 時点で型消去して保持する。ジェネリック `init<C: Hashable, V: View>` が糖衣として型安全な入口を提供する
- **Android**: ジェネリック `class CustomCell<Content : Any>` をそのまま使う (実行時はクラス消去で単一登録)。ViewHolder は `CustomCell<*>` (star projection) で受けるため型パラメータ越しに `builder(content)` を直接呼べず、構築時に content と builder を型付きのまま閉じ込めた消去済みエントリポイント (`internal val composeContent: @Composable () -> Unit`) を保持し、ViewHolder はこれだけを呼ぶ
- Registry へは両プラットフォームとも**標準 Cell と同様に1回だけ**登録する (iOS: `CustomCell.self` → `CustomCellView.self`、Android: `CustomCell::class` → 予約 viewType + `CustomCellViewHolder` factory)

## Alternatives Considered

- **A: 両プラットフォームともジェネリック型のまま Registry を拡張** (型ファミリ単位の解決キーを導入) — Registry 改造は ADR-0014 で「行わない」と確定済み。既存の解決経路の複雑化に見合う利得がない
- **B: 両プラットフォームとも型消去統一** (Android も `content: Any`) — iOS の制約を Android に輸出するだけで、Kotlin では builder の型安全 (`@Composable (Content) -> Unit`) を捨てる損失が発生する。プラットフォーム間の内部表現の対称性は利用者価値ではない
- **C: iOS のみ実体型ごとの登録を要求** — 「事前登録なしで DSL に直書き」という ADR-0014 の中核価値を iOS だけ失う。不採用

## Consequences

- 正: ADR-0014 の「事前登録なし」を単一 `register` API のまま成立させられる。利用者 API は両プラットフォームとも型安全 (iOS はジェネリック init、Android はジェネリック class)
- 負: iOS は `AnyHashable` が Foundation ブリッジ経由で異なる実体型の値を等価と判定する (`AnyHashable(Int(1)) == AnyHashable(Double(1.0))` は真) ため、値比較だけでは content の型変更が差分検出をすり抜け古い builder 出力が残る。実体型トークン `contentType: ObjectIdentifier` を等価性・hash に追加して補った (出典: 実装結果。second-opinion-002 指摘 #2)
- 負: iOS は `AnyView` 消去により SwiftUI の構造的差分が粗くなり、content 変更時に subtree 再構築が起きやすい (再バインド自体を equality で最小化しているため実用上の影響は限定的)
- 負: 内部表現がプラットフォームで非対称になり、実装を読み替える際に対応関係の理解が要る (iOS の消去は init、Android の消去は `composeContent`)

出典: `kasane/changes/archive/2026-08-04-add-cell-types-custom/design.md` (Decision 1) / `kasane/changes/archive/2026-08-04-add-cell-types-custom/second-opinion-001.md` 指摘 #4 / `kasane/changes/archive/2026-08-04-add-cell-types-custom/second-opinion-002.md` 指摘 #2 / `ios/Sources/KsSettingsViewUI/CustomCell.swift` (`contentType` の等価性参加コメント)
