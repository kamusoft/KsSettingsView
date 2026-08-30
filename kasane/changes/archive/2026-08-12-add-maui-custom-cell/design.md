# Design: add-maui-custom-cell

## Context

CustomCell を MAUI から使えるようにする。フェーズ議論で方針は合意済み — 提供層は①②のみ (maui/ADR-0019)、content は live view + 世代トークン (maui/ADR-0020)、公開面は CellBase 継承 + silent no-op (maui/ADR-0021)。3件の ADR はいずれも proposed であり、accepted への昇格は ksn-distill の既定パイプラインで行う。本書はそれらを実装可能な粒度に落とす。

前提となる実物:

- native CustomCell (iOS `CustomCell.swift` / Android `CustomCell.kt`): content 値 + builder。等価性は `id` / `style` / `content` / `showArrow` / `isEnabled` / `isVisible` (関数値除外)。iOS content は `AnyHashable` + 実体型トークン、Android は `Content : Any`
- 実体化機構 (concepts/maui/architecture/view-materialization.md): `IKsViewMaterializer` seam / `IKsViewLease` / 自己計測 wrapper `KsAccessoryHostView` / `KsSettingsController` 所有
- Bridge の既存パターン: per-type DTO (`KsBridgeCell` 派生、maui/ADR-0011)、view インスタンス輸送は「detach してから返す定数返し closure」(`KsBridgeAccessoryView`、maui/ADR-0017)、操作通知は単一 delegate/listener (`IKsInteractionSink` → relay、maui/ADR-0003)

## Goals / Non-Goals

- Goals: proposal.md の What Changes を実装可能な決定に分解する
- Non-Goals: proposal.md の Non-Goals に同じ (③ / ContentTemplate / 仮想化 / native 公開 API 変更)

## Decisions

### Decision 1: facade 表現と snapshot への落とし方

**採用案:** `CustomCell : CellBase`、`[ContentProperty(nameof(Content))]` の `Content : View?`。挙動プロパティは `Command` / `CommandParameter` / `Tapped` / `ShowArrowIndicator` (既定 false)。タップの実効有効状態と発火順は既存 CommandCell の公開契約に合わせる — `IsEnabled` と `Command.CanExecute` の連動、`CanExecuteChanged` の追従、発火順は `Tapped` → `Command`。snapshot は per-type 展開に従い `KsCustomCellSnapshot` を新設し、共通フィールド (id / style / isEnabled / isVisible) + `ContentToken` + `ShowArrowIndicator` + **タップ購読有無** を持つ。タップ購読有無は `Command` 設定と `Tapped` の購読状態から導出し、表示後の変化 (Command の設定/解除、最初/最後の `Tapped` 購読変更) も再配信の対象にする (event accessor の add/remove で通知する)。Content (View 実体) は snapshot に**入れない** — 値比較経路に view を流さない (maui/ADR-0018 の規律)。

**理由:** maui/ADR-0021 (proposed) の合意内容。snapshot から View を外すことで、既存の snapshot 差分検出がそのまま「token が変わった時だけ view の差し替えが起きる」になる。タップ挙動を CommandCell と揃えるのは、同じ facade 内で Cell ごとに発火規則が違う驚きを避けるため。

**代替案:**
- **A: snapshot に View 参照を含め参照比較する** — 値比較経路に view 変更を流さない ADR-0018 の規律に反し、比較器の特殊化も必要。却下
- **B: Content を `object` にして View 以外も許す** — 用途がなく型安全を失うだけ。却下

### Decision 2: cell content の lease 所有と世代トークン

**採用案:** `KsSettingsController` が cell 単位の platform lease を所有する (key は cell ID。accessory の `KsAccessorySlot` と並ぶ cell 用の所有表)。トークンは controller 単位の単調増加カウンタから発行する文字列 (icon の maui/ADR-0015 と同じ latest-wins 発想)。発行タイミング:

- Content 設定・差し替え → 新規 materialize + 新トークンで再発行 → 旧 lease は「Store 更新 → native 配信 → 破棄」の順で退役
- null 遷移の規則: `View → null` は新トークン + null view で再発行して旧 lease を退役、`null → View` は新規 materialize + 新トークン、null のまま再接続した場合も新トークンで再発行する (トークンは「参照遷移が起きるたびに必ず変わる」を不変条件にする)
- 同一 View の包み直し (A→B→A 等) → 先にその View を掴む退役待ち lease を破棄してから materialize (Handler 1:1)
- Handler 再接続 → 全 CustomCell content を新しい MauiContext で再実体化し、新トークンで再発行 (platform lease は Host 世代)
- **構造的な除去も同じ退役経路に乗せる**: CustomCell 自体の Remove / Replace、Section の Reset・削除、ItemsSource からの除去、Root 再構築のいずれでも「native への除去配信 → lease 破棄」の順で解放し、cell 所有表・計測購読・多重配置表から取り除く。除去後の View は論理所有も解放され、別の CustomCell / accessory で再利用できる (再設定時は新規 materialize)
- 論理所有 (logical tree + BindingContext 継承) は `KsAccessoryViewOwnership` の共通処理を再利用し、Content プロパティの寿命で管理。多重配置検出も同一契約
- gateway の輸送 seam: 現行 `ToDto` は snapshot + icon のみを参照するため、cell content lease の platform 実体を DTO へ載せる口を gateway 契約に追加する (accessory の view 引当と同じ位置づけ)。fake gateway / fake materializer も同時に更新し、既存 accessory 挙動の回帰テストを添える

**理由:** view-materialization.md の寿命規律をそのまま cell に拡張する形で、新しい概念を増やさない。トークンを controller 発行にすることで「差し替え再発行時のみ変わる」を構造的に保証できる。

**代替案:**
- **A: View インスタンスの hash をトークンにする** — 同一 View の包み直し (A→B→A) で token が戻り「変更なし」と誤判定され、native が古い platform view を保持し続ける。却下
- **B: 毎 push で新トークン** — 内容不変の構造更新でも再バインドが走り、KsAnyView 直持ちと同じ暴発になる。却下

### Decision 3: Bridge DTO と native 埋め込み

**採用案:** `KsBridgeCustomCell` (`KsBridgeCell` 派生) を両 OS に追加し、`view` (UIView / Android View) + `contentToken` (String) + `showArrowIndicator` (Bool) を輸送する。native 構築は bridge モジュール内で:

- iOS: `CustomCell(content: contentToken, builder: { _ in AnyView(埋め込み representable) })`。representable は `KsBridgeAccessoryView.anyView` と同じく**返す前に `removeFromSuperview()`** し、常に同じインスタンスを返す。自己計測 wrapper (`IntrinsicContentSize` 実装済み) を包むため、representable は wrapper のサイズをそのまま SwiftUI へ中継する
- Android: `CustomCell(content = contentToken, builder = { AndroidView(factory = { detach して view を返す }) })`。detach なしの再取り付けは crash 実測済み (maui/ADR-0017) のため必須
- `view` は nullable とし、null (facade の `Content` 未設定) は空内容を返す builder (iOS: `EmptyView`、Android: 空 composable) で構築する — 行は空内容のまま出力される (XAML 構築順に依存しないため)
- **再バインドと view 安定性の意味論**: native の再バインド (builder 再実行) の発火条件は native CustomCell の等価性契約のまま — style / showArrow / isEnabled / isVisible の変更や replaceCells 経路の再配信 (同値スキップなし、maui/ADR-0018) でも発火する。この設計が保証するのは**埋め込み platform view インスタンスの安定性**: 同一トークンの間、再バインドが何度起きても定数返し closure は同一インスタンスを返し、view の破棄・再 materialize・Handler の切断は行われない。view の差し替え (別インスタンスへの置換) はトークン変更時のみ。テストは materialize / detach / dispose の回数を正負両方向 (同一トークン再配信で 0 回 / トークン変更で 1 回) で計測する

**理由:** per-type 展開 (ADR-0011) とインスタンス輸送 (ADR-0017) の既存パターンの合成で、bridge 公開面の追加は DTO 1型 + セル構築分岐のみ。native Core / UI の公開 API に変更が要らない。

**代替案:**
- **A: native 側に MAUI 専用の CustomCell 変種を追加する** — native 公開 API の変更が必要になり、ロードマップの非ゴール (XAML 都合の Native 変更をしない) に反する。却下
- **B: builder を interop 境界越しに輸送する** — 関数は `@objc` / JNI 境界を越えられず、Handler 1:1 で view は1個しかないため factory 輸送に意味がない (ADR-0017 の却下経緯と同一)。却下

### Decision 4: 行タップ通知

**採用案:** `IKsInteractionSink.CustomCellTapped(string cellId)` を追加し、両 OS の delegate / listener (`KsBridgeInteractionDelegate` / `KsBridgeInteractionListener`) に対応メソッドを足す。native CustomCell の `onTap` は、facade 側でタップ購読あり (Decision 1 の導出) の場合のみ非 nil で構築する (nil のとき content 内部の操作を妨げない native 契約を活かす)。購読有無の表示後の変化は Decision 1 の再配信で native へ追従する (同一トークンのため view は安定したまま onTap だけが切り替わる)。書き戻しは無い (通知のみ。maui/ADR-0012 の書き戻し一覧に変更なし)。

**理由:** ADR-0003 の単一 delegate 集約と、既存 CommandCell/ButtonCell の通知形 (`XxxTapped(cellId)`) の踏襲。

**代替案:**
- **A: onTap を常に非 nil で構築し facade 側で無視する** — native の「onTap nil なら content 操作を妨げない」挙動が失われ、content 内のボタン等とのタップ消費関係が変わる。却下

### Decision 5: content サイズ変化の行高さ追従

**採用案:** wrapper の `MeasureInvalidated` を facade で合体し、native の行再計測へ届ける。native CustomCell は self-sizing 契約 (専用再計測 API なし) のため、まず「wrapper の計測無効化だけで両 OS の行高さが追従するか」を実装フェーズ冒頭で probe する。不足する場合 (iOS は accessory で対象限定 invalidateLayout が必要だった実測がある — maui/ADR-0018。その際は native Store / Controller への公開口追加を要した) は、accessory の `invalidateAccessoryMeasurement` と同型の**一過性の再計測通知** (cell 対象版) を native 側へ追加する。この追加は proposal の Non-Goals が事前許容する対称化例外で、採る場合は bridge spec への追記を deviation.md に記録する。

**理由:** 追従保証は core の CustomCell 契約 (SHALL)。iOS の accessory 実測が「自動では追従しない」前例を示しており、probe を先に置くことで手戻りを最小化する。

**代替案:**
- **A: サイズ変化で再発行 (新トークン)** — 再バインド = builder 再実行 = platform view の付け替えが走り、live 更新の規律 (ADR-0020) に反する。スクロール位置や入力状態も失われる。却下

## Risks / Trade-offs

- **行リサイクルとの交差** (proposal の主リスク): native renderer の reuse 規律 (`prepareForReuse()` / `reset()`) で埋め込み view が残らないこと、リサイクル後の再 bind で「detach してから返す」closure が正しく再取り付けすること。E2E でスクロールを含む検証を必須にする
- 行数分の live View が常存する (仮想化なし)。パリティ対象の CustomCellDemo にはスクロール耐性の多数行構成が含まれるため、live View 常存のコストがそのまま露出する — 行リサイクル検証と兼ねて実測し、問題があれば deviation として記録する
- iOS の representable 中継 (wrapper → SwiftUI) のサイズ伝搬は probe 対象で、実測結果次第で Decision 5 の追加経路が要る

## Migration Plan

追加のみで破壊的変更なし。既存アプリへの影響なし。

## Open Questions

- Decision 5 の probe 結果 (両 OS で自然追従するか)。結果は deviation ではなく実装タスク内の分岐として扱う

## ADR 候補

なし — 本質的な決定は maui/ADR-0019〜0021 として起票済み (proposed)。Decision 1〜5 はその実装展開であり、局所的な API 詳細はコード + テストに任せる。Decision 5 の probe で公開契約に影響する事実が出た場合のみ、ksn-distill で ADR-0020 への追記または新規起票を検討する
