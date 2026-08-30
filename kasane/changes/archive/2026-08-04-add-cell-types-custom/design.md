# Design: add-cell-types-custom

## Context

core/ADR-0014（accepted）で CustomCell の根幹（content 値 + builder クロージャ、equality は content のみ、Registry 拡張なし、KsAnyView 直持ち不採用）は確定済み。本 design はその決定を現行コードの実物（単一 `register` API・`ComposeCellViewHolder`・共通行レイアウト・CellStyle）へ落とすためのプラットフォーム別の具体化を扱う。

前提となる実物の制約:
- iOS `KsCellRegistry` は `ObjectIdentifier(type(of: cell))` をキーに Renderer 型を解決する（[KsCellRegistry.swift:57](../../../ios/Sources/KsSettingsViewUI/KsCellRegistry.swift)）。Swift のジェネリック struct は実体型ごとに別の metatype になるため、`CustomCell<Content>` をそのまま登録キーにすると実体型ごとの事前登録が必要になり、ADR-0014 の「事前登録なし」と矛盾する
- Android の Registry は `cellClass`（KClass）キーで、ジェネリクスは実行時消去されるため単一クラス登録で済む
- 矢印（Disclosure Indicator）は iOS が `makeChevronView()` を `applyCellBaseLayout` の `accessoryView` へ渡す方式、Android が `CellBaseViews.accessoryHolder` に `ic_navigate_next`（18x26dp）の `AppCompatImageView` を置く方式。`UICellAccessory` / `UIListContentConfiguration` 経路は使わない方針が既存コメントに明記されている
- 固定高さは `CellStyle.cellHeight` を DSL modifier（`.cellHeight()`）が書き換える構造
- Android `ComposeCellViewHolder` は `itemView == ComposeView` を前提に `DisposeOnDetachedFromWindow` を強制する、CustomCell を名指しで見越した基底クラス

なお、full-bleed 宣言 UI ホスティング経路と ADR-0011（共通行統一・RecyclerView 内 `ComposeView.setContent` 不使用）の関係は **core/ADR-0015（適用除外、accepted）** で整理済み（second-opinion-001 指摘 #2 への対処）。

## Goals / Non-Goals

**Goals**: proposal.md の What Changes 1〜8。**Non-Goals**: proposal.md の Non-Goals に同じ。

## Decisions

### Decision 1: iOS は型消去内蔵の非ジェネリック struct、Android はジェネリック class

**採用案:**

iOS — 非ジェネリック `CustomCell` が内部で content / builder を型消去して保持し、ジェネリック `init` が糖衣として型安全な入口を提供する:

```swift
public struct CustomCell: KsCell, DSLReidentifiable, DSLStyleModifiable, VisibilityAware {
    public let id: UUID
    public let style: CellStyle
    /// 型消去済み content（equality の主対象）
    public let content: AnyHashable
    /// 型消去済み builder（equality から除外）
    internal let builder: (AnyHashable) -> AnyView
    public let showArrow: Bool
    public let onTap: (@Sendable () -> Void)?
    public let isEnabled: Bool
    public let isVisible: Bool

    /// content あり（データ駆動）
    public init<C: Hashable, V: View>(
        id: UUID = UUID(), style: CellStyle = CellStyle(),
        content: C, showArrow: Bool = false,
        onTap: (@Sendable () -> Void)? = nil,
        isEnabled: Bool = true, isVisible: Bool = true,
        @ViewBuilder builder: @escaping (C) -> V
    ) { /* content を AnyHashable、builder を (AnyHashable) -> AnyView に消去 */ }

    /// content なし（静的コンテンツ糖衣）
    public init<V: View>(
        id: UUID = UUID(), /* 同上 */
        @ViewBuilder builder: @escaping () -> V
    ) { /* content = 内部の空値 (EmptyContent) */ }
}
```

Android — ジェネリック class（実行時はクラス消去で単一登録）:

```kotlin
class CustomCell<Content : Any>(
    override val id: String = "custom-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val content: Content,
    val showArrow: Boolean = false,
    val onTap: (() -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
    val builder: @Composable (Content) -> Unit,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, VisibilityAware
```

Registry へは両プラットフォームとも**標準 Cell と同様に1回だけ**登録する（iOS: `CustomCell.self` → `CustomCellView.self`、Android: `CustomCell::class` → 予約 viewType + `CustomCellViewHolder` factory）。

Android の ViewHolder は `CustomCell<*>`（star projection）で受けるため、型パラメータ越しに `builder(content)` を直接呼ぶコードは型安全にコンパイルできない。そこで CustomCell は構築時に content と builder を型付きのまま閉じ込めた消去済みエントリポイント（`internal val composeContent: @Composable () -> Unit`）を保持し、ViewHolder はこれだけを呼ぶ（second-opinion-001 指摘 #4 への対処）。iOS は init 時点で `AnyHashable` / `AnyView` へ消去済みのため同種の問題は生じない。

**理由:** ADR-0014 の「事前登録なし」を単一 `register` API のまま成立させる唯一の素直な形。iOS のジェネリック struct は Registry の metatype キーと両立しない（実体型ごとに登録が必要になる）ため、型消去を Cell 内部に持ち込み、利用者 API はジェネリック init で型安全に保つ。Android は言語仕様（型消去）が同じ効果を無償で与えるため、ジェネリック class をそのまま使い可読性を優先する。

**代替案:**
- **A: 両プラットフォームともジェネリック型のまま Registry を拡張**（型ファミリ単位の解決キーを導入）— Registry 改造は ADR-0014 で「行わない」と確定済み。既存の解決経路の複雑化に見合う利得がない
- **B: 両プラットフォームとも型消去統一**（Android も `content: Any`）— iOS の制約を Android に輸出するだけで、Kotlin では builder の型安全（`@Composable (Content) -> Unit`）を捨てる損失が発生する。プラットフォーム間の内部表現の対称性は利用者価値ではない
- **C: iOS のみ実体型ごとの登録を要求** — 「事前登録なしで DSL に直書き」という ADR-0014 の中核価値を iOS だけ失う。不採用

### Decision 2: equality は「content + 表示に効くスカラー」、関数値は全除外

**採用案:** equality / hashCode の対象は `id` / `style` / `content` / `showArrow` / `isEnabled` / `isVisible`。`builder` / `onTap` は除外（iOS は手動 `==` / `hash(into:)`、Android は手動 `equals` / `hashCode` を実装）。content なし糖衣の content は内部の空値（常に相等）で、実質 id + スカラーの比較になる。

**理由:** ADR-0014（2026-08-03 改訂で「content + 表示に効くスカラー、関数値除外」と明確化済み）に一致する。表示に影響する値プロパティ（showArrow 等）が変わったのに再バインドされないと画面が古いままになるため、これらは参加させる。関数値の除外は CommandCell の Decision 2（毎回新規クロージャで差分検出が暴発する）の家風踏襲。

**代替案:**
- **A: 純粋に content のみ**（id やスカラーも除外）— `showArrow` / `isEnabled` の変更が再バインドを起こさず、画面と model が乖離する。不採用
- **B: builder の参照同一性を equality に含める** — DSL 再評価のたびに新規クロージャが生成され、内容不変でも毎回「変更あり」になる（CommandCell Decision 2 で既知の暴発パターン）。不採用

### Decision 3: 矢印は hosted 宣言 UI の内側で合成する（アセット・寸法定数は既存 accessory と共有）

**採用案:** `showArrow == true` のとき、ライブラリが builder 出力の trailing に chevron を合成した宣言 UI ツリーを hosting へ渡す。

- iOS: `UIHostingConfiguration { HStack { AnyView(builder(content)) /* 残り幅 */ ; chevron } }`。chevron は `makeChevronView()` と同一のアセット・寸法・trailing 余白の定数を共有する
- Android: `composeView.setContent { Row { Box(Modifier.weight(1f)) { builder(content) }; chevron } }`。chevron は `painterResource(R.drawable.ic_navigate_next)` を 18x26dp・`accessoryHolder` と同じ末端余白で縦中央配置

**理由:** iOS の `UIHostingConfiguration` は contentView 全体を占有するため、UIKit 側 accessory と共存させる構造（`UICellAccessory` 経路）は「使わない」と明記された既存方針に反する。Android も `ComposeCellViewHolder` が `itemView == ComposeView` 前提で設計されており、classic View の accessoryHolder と混在させると基底クラスの設計意図（CustomCell 用に用意された強制 Dispose 機構）を壊す。宣言 UI 内合成なら両プラットフォームが同型になり、実装もシンプル。視覚一致（既存 Cell の chevron と同位置・同サイズ）は mock との視覚照合タスクで検証する。

**代替案:**
- **A: iOS を `UICellAccessory.disclosureIndicator` にする** — 既存 Cell 群が意図的に避けている経路（見た目の統一を自前 chevron で取っている）。CustomCell だけ Native accessory だと chevron の見た目・位置が既存 Cell とズレる。不採用
- **B: Android を CellBaseViews ハイブリッド**（ConstraintLayout root + ComposeView + classic accessoryHolder）にする — accessory の配置コードを再利用できる利点はあるが、`ComposeCellViewHolder` を使えず Dispose 強制を自前再実装することになり、行構造も複雑化する。宣言 UI 内合成で同じ見た目が出せる以上、利得が釣り合わない。不採用

### Decision 4: ViewHolder / Renderer は宣言 UI ホスティング専用型を新設する

**採用案:**

- Android: `internal class CustomCellViewHolder(context) : ComposeCellViewHolder<CustomCell<*>>` を新設。`bind` で `composeView.setContent { ... }`（Decision 3 の合成ツリー + タップ・活性の適用）、`reset` で listener 参照を解放（Composition の破棄は基底の `DisposeOnDetachedFromWindow` が担保）
- iOS: `internal final class CustomCellView: UICollectionViewCell, KsCellRenderer` を新設。`render` で `contentConfiguration = UIHostingConfiguration { ... }` を設定し、再バインドは configuration の差し替えのみ。`prepareForReuse` で tapHandler 等を解放
- タップは既存 Cell と同じ経路（iOS: tapHandler、Android: `itemView` の click listener を bind ごとに上書き）。`onTap == nil` または `isEnabled == false` のとき click を無効化する。content 内の操作可能要素がイベントを消費した場合は行タップを発火させない（宣言 UI 側のジェスチャ消費が優先。second-opinion-001 指摘 #7）
- `isEnabled == false` のときは行タップ無効化に加えて content 内の操作も抑止する（iOS: hosted content へ `.disabled(true)` を適用、Android: 合成ツリーで入力を遮断）。視覚状態契約「無効 Cell は操作 callback と内包 control の操作を抑止する」に従う（second-opinion-001 指摘 #6。無効時の見た目の描き分けは利用者責務）

**理由:** Android は CustomCell のために先回りで用意された基底をそのまま使う位置。iOS は H/F の `applyAccessoryToListCell` で実運用済みの `UIHostingConfiguration` パターンの Cell 版で、新規性が最小。

**代替案:**
- **A: iOS で `KsListCellBase` を継承** — title/description スタックを持つ基底は full-bleed と噛み合わず、外す作業（AiForms の `RemoveArrangedSubview` 方式）は既知の複雑さの再生産。不採用
- **B: iOS で `UIHostingController` 手動親付け** — 旧 proposal 時点で既に `UIHostingConfiguration` 採用によって排除済みの方式。lifecycle 管理の手動化はリスクだけ増える。不採用

### Decision 5: style は CellStyle を保持し、適用対象は行レベル項目のみ

**採用案:** `style: CellStyle` を保持して `DSLStyleModifiableCell` / `DSLStyleModifiable` に準拠し、既存の `.cellHeight()` / style modifier チェーンに乗る。ただし CustomCell が適用するのは**行レベルの項目のみ**（背景色・cellHeight — `EffectiveStyle` 経由）。テキスト色・フォント等のコンテンツ内装項目は builder 出力に対して no-op（適用先が存在しない）。この線引きはデルタスペックに Requirement として明記する。

**理由:** DSL の modifier 体験（`.cellHeight()` 等）を CustomCell でも一貫させるには CellStyle 準拠が最短。full-bleed で中身が利用者領分の以上、コンテンツ内装への style 適用は原理的に不可能で、「効くもの・効かないもの」を契約として明文化する方が誠実。

**代替案:**
- **A: style を持たない** — `.cellHeight()`（固定高さの唯一の経路）に乗れなくなり、論点3b の決定（固定高さは既存 modifier に乗る）と矛盾する。不採用
- **B: CustomCell 専用の縮小 style 型を新設** — 型が増え、DSL の style modifier 群（`DSLStyleModifiableCell` 前提）と接続できない。不採用

### Decision 6: DSL は既存規約どおりの拡張関数 + ビルダ直書き

**採用案:**

- Android: `fun <C : Any> DSLSectionScope.CustomCell(content: C, showArrow: Boolean = false, style: CellStyle = CellStyle(), onTap: (() -> Unit)? = null, isEnabled: Boolean = true, isVisible: Boolean = true, builder: @Composable (C) -> Unit): CellHandle` と、content なしオーバーロード `fun DSLSectionScope.CustomCell(..., builder: @Composable () -> Unit): CellHandle`（trailing lambda で `CustomCell(content = x) { ... }` と書ける）
- iOS: `SectionBuilder` は `[any KsCell]` を集めるため、`CustomCell(content: x) { x in ... }` の struct 直書きがそのまま DSL になる（拡張関数の追加は不要）。`DSLReidentifiable` 準拠により id modifier も既存どおり効く
- `DSLIconModifiable`（icon modifier）には**準拠しない**（アイコン領域が存在しないため。modifier を呼んでも効かない振りをするより、型として受け付けない方が誠実）

**理由:** 既存の DSL 規約（Android は DSLSectionScope 拡張関数、iOS は result builder への値直書き）にそのまま従う形。新規の DSL 概念を持ち込まない。

**代替案:**
- **A: iOS にも専用のビルダ関数を足す** — struct 直書きで既に成立しており、重複 API になる。不採用
- **B: Android で builder を CellHandle の modifier として後付けする**（`CustomCell(content).builder { ... }`）— builder なしの CustomCell が一時的に存在でき、契約が緩む。不採用

### Decision 7: 標準集合として自動登録する

**採用案:** CustomCell の Renderer / ViewHolder は基本 7種・入力 5種と同じく標準登録対象に加える。接続先は既存の自動登録機構にそのまま倣う（second-opinion-001 指摘 #8 への対処）:

- iOS: `registerCustomCell()` を新設し、`KsSettingsViewController` の自動登録フラグ群に `autoRegisterCustomCell`（既定 true）を追加する。挙動は既存の `autoRegisterBasicCells` / `autoRegisterInputCells` と同一（shared Registry のときのみ自動登録、独立 Registry 注入時は明示登録）
- Android: 予約 viewType 域に CustomCell 用定数を1つ追加し、`KsSettingsView` 初期化時の sentinel 方式登録（LabelCell / EntryCell の sentinel と同列）に CustomCell の sentinel を加える

利用者の明示登録は不要。

**理由:** ADR-0014 の「事前登録なしで DSL に直接書ける」を成立させる前提条件。ライブラリ提供 Cell である以上、標準集合と同じ扱いが一貫する。

**代替案:**
- **A: 利用者による opt-in 登録** — 「DSL に書いたのに表示されない（strictMode なら例外）」という初見の罠を作るだけ。不採用

## Risks / Trade-offs

- **Swift の Sendable 整合**: `KsCell` は `Sendable` 要求を持つが、`AnyView` を返す builder は厳密には Sendable でない。`KsAnyView` が `@unchecked Sendable` + MainActor 描画で解決済みの同型問題であり、同じ手当（`@unchecked Sendable` と描画側の MainActor 限定）を踏襲する
- **AnyView 経由の SwiftUI 差分粒度**: 型消去により SwiftUI の構造的差分が粗くなり、content 変更時に subtree 再構築が起きやすい。ただし再バインド自体を equality で最小化しているため、実用上の影響は限定的と見込む（動的高さデモで体感検証）
- **Android の動的高さ伝播**: RecyclerView 内 ComposeView の requestLayout 伝播は機種依存のもたつきが理論上あり得る。動的高さデモを受け入れ条件としてサンプル駆動で検証する（proposal の Impact に同じ）
- **chevron の視覚一致**: Decision 3 の宣言 UI 内合成は、既存 Cell の chevron と定数共有で揃える方式のため、ズレは実装時の視覚照合で検出する前提（mock 照合タスクで担保）
- **content の Hashable / equals 品質は利用者責務**: content の equality が壊れていると再バインドが過剰/過少になる。契約としてデルタスペックに明記する
- **content の nullability の platform 差**: Android は `Content : Any` で non-null を型強制できるが、iOS の `C: Hashable` は Optional も受けられてしまう。契約は non-null とし、iOS は API ドキュメントで明記する（second-opinion-001 指摘 #12）

## Migration Plan

追加のみで破壊的変更なし。移行作業は発生しない。旧 `openspec/changes/add-cell-types-custom/` は凍結のまま触らない。

## Open Questions

- なし（探索で全論点決着済み。実装中の乖離は deviation.md 経路で扱う）

## ADR 候補

- **Decision 1**（iOS 型消去内蔵 / Android ジェネリック）: 公開 API の形を固定し覆すコストが高い。ADR-0014 の実現手段としてプラットフォーム境界をまたぐ判断のため候補とする
- Decision 2 の等価性は **ADR-0014 の改訂（2026-08-03）で確定済み**、Decision 3 / 4 の共通行適用除外と宣言 UI 内合成は **core/ADR-0015（2026-08-03 起票・accepted）で確定済み** — いずれも本 propose 中の second-opinion 対応で決定層へ反映したため、蒸留への申し送りは不要
- Decision 5 / 6 / 7 は ADR-0014 / ADR-0015 と既存規約の帰結・局所詳細のため候補としない（コード + デルタスペックで十分）
