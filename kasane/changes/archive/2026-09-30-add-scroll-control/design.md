# Design: add-scroll-control

## Context

KsSettingsView には、どの platform にもスクロールを動かす公開 API が無い。フェーズ議論 (kasane/roadmaps/maui-support/phases/phase-8-scroll-control/agenda.md) で、同系統の KsCollectionView のスクロール制御 (`../KsCollectionView/kasane/decisions/core/0007-scroll-controller.md`) を踏襲すると決めた。そのうえで KsSettingsView 固有の差分として、次を決めた。

- Section を命令の対象に含める
- ハンドルは Host につなぎ、Store は経由しない (core/ADR-0037)
- 宣言 UI では明示 ID / key で指す
- MAUI はインターフェース型のハンドルを SettingsView が作って ViewModel へ渡す (maui/ADR-0029)
- 準備完了をコマンドで知らせる
- Host の作り直しをまたぐ位置は Bridge が控えて戻す (maui/ADR-0030)

本書はそれらを実装に落とす形を決める。

現状の実装で設計を左右する事実:

| 項目 | iOS | Android |
|---|---|---|
| 下地 | UICollectionView + diffable data source (`KsSettingsViewController`) | RecyclerView + `ConcatAdapter(header, main, footer)` (`KsSettingsView`) |
| Store → Host | 同期 (`@MainActor` の Subject を `sink` で受け、同じコールスタックで apply) | 非同期 2 段 (`tryEmit` → `lifecycleScope` の collect、`submitList` の裏の diff → commit) |
| 反映完了の合図 | apply の completion は一部の経路だけで使っている | commit callback は内容通知の経路だけで使っている |
| Section 見出し | 行に数えない supplementary。Root Header / Footer は boundary supplementary | 平坦な一覧の行。Root Header / Footer は ConcatAdapter の前後 |
| 非表示要素 | visible projection に入らない | `flatten` で行にならない |
| 宣言 UI の最終 ID | `DSLIdentityUUID.uuid(from:)` (public の純関数) | `DSLIdentityId.id(from)` (internal の純関数) |
| 位置の控え | なし | 同じ View の付け外し用だけ (`pendingScrollPosition`)。SavedState はカレンダーの状態だけ |

KsCollectionView から移せるのは、公開 API の形・接続の規則・iOS の待ち行列 (UIKit で完結) である。Android の待ち行列と着地の補正は Compose の LazyGrid 前提なので、RecyclerView に合わせて作り直す。

## Goals / Non-Goals

Goals: 両 OS の Native (UIKit / View Host と SwiftUI / Compose) と MAUI から、Cell・Section・両端へ命令でスクロールできる。命令はデータの反映の後に実行される。MAUI で Host が作り直されてもスクロール位置が戻る。Android の View Host は Activity の作り直しでも位置が戻る。

Non-Goals: proposal.md の Non-Goals のとおり (位置を読む API、部品で指すこと、状態で命令する形、KsCollectionView 側の変更、iOS の state restoration)。

## Decisions

### Decision 1: 命令ハンドルの公開形は KsCollectionView と同形にし、Section 用の命令と protocol / interface を足す

**採用案:**

| | Swift (`KsSettingsViewUI`) | Kotlin (`jp.kamusoft.kssettingsview.ui`) |
|---|---|---|
| ハンドル | `@MainActor public final class KsScrollController: KsScrollControlling` (`init()`) | `public class KsScrollController : KsScrollControlling` |
| protocol / interface | `@MainActor public protocol KsScrollControlling: AnyObject` | `public interface KsScrollControlling` |
| Cell へ | `scrollTo(id: some Hashable, position: KsScrollPosition = .start, animated: Bool = true)` | `scrollTo(id: Any, position: KsScrollPosition = KsScrollPosition.Start, animated: Boolean = true)` |
| Section へ | `scrollToSection(id: some Hashable, position: = .start, animated: = true)` | `scrollToSection(id: Any, position: = Start, animated: = true)` |
| 両端へ | `scrollToStart(animated: Bool = true)` / `scrollToEnd(animated: Bool = true)` | `scrollToStart(animated: Boolean = true)` / `scrollToEnd(animated: Boolean = true)` |
| 位置 | `public enum KsScrollPosition: Sendable { case start, center, end }` | `public enum class KsScrollPosition { Start, Center, End }` |
| Compose 用 | — | `@Composable public fun rememberScrollController(): KsScrollController` (`jp.kamusoft.kssettingsview.compose`) |

Swift の protocol は既定引数を持てないので、既定値は protocol extension で与える。Kotlin はメインスレッド以外からの呼び出しを debug で検出する (Swift は `@MainActor` が静的に守る)。

**理由:** 命令の語彙・既定値・位置の 3 値は KsCollectionView と同じにし、両ライブラリで同じ書き方にする (core/ADR-0037)。`scrollToSection` は Section を対象に含める決定 (agenda 論点1) の受け口。protocol / interface は ViewModel がライブラリの実装クラスに依存せずテストで fake を差し込むため (agenda 論点6)。名前は両 OS で同じにする。`rememberScrollController` はオーナー判断で KsCollectionView の `rememberKsScrollController` から変える。KsSettingsView で最初の公開 remember 関数で、Compose の `rememberNavController()` と同じく型名をそのまま繰り返さない形である。

**代替案:**
- **A: Swift の引数ラベルで Section を区別する (`scrollTo(sectionID:)`)** — Kotlin では Cell ID も Section ID も同じ型で引数ラベルも無く、同名の `scrollTo` では区別できない。両 OS で名前が割れるため却下
- **B: ID の型で Cell と Section を判別する (iOS の `KsCellID` と Section の `UUID`)** — 宣言 UI の手がかりは任意の Hashable で、同じ値が Cell と Section の両方に付きうる。型では判別できないため却下

### Decision 2: Host がハンドルの接続口を持ち、ハンドルから Host への参照は弱参照にする

**採用案:**
- UIKit Host は `public var scrollController: KsScrollController?`、View Host は `var scrollController: KsScrollController?` を持つ。代入すると接続し、`nil` の代入・Store からの切断 (`disconnectStore()` / `unbind()`)・Host の破棄で接続を外す。Store からの切断ではプロパティも `nil` / `null` に戻し、再び Store を接続しても自動ではつなぎ直さない (再代入が要る。Bridge と宣言 UI のラッパーは接続のたびに代入する)。受けるのは実装クラスで、protocol / interface ではない (自作の実装が渡される組み合わせを作らない — agenda 論点6)
- SwiftUI は `KsSettingsView` の修飾子 `.scrollController(_:)`、Compose は `KsSettingsView(...)` の引数 `scrollController: KsScrollController? = null` で受ける (Store 方式・DSL 方式の両方)
- 1 つのハンドルは 1 つの受け口だけを持つ。後から接続したものが勝ち、置き換えるときは debug で警告ログを出す。外すのは現在の受け口と同一のときだけ
- ハンドルは受け口を弱参照で持つ (両 OS)。Host はハンドルを強参照で持つ

**理由:** 接続の規則は KsCollectionView と同じにする。ハンドルは ViewModel が持って画面より長生きしうる。Android の View Host は Activity の Context を持つので、ハンドルが Host を強参照すると画面を離れた後も Activity を保持し続ける。弱参照なら Host が消えたときに自然に未接続 (no-op) になる。

**代替案:**
- **A: KsCollectionView の Android と同じく受け口を強参照で持つ** — KsCollectionView の受け口は applicationContext だけを持つ専用オブジェクトだが、KsSettingsView の受け口は Activity の Context を持つ View Host そのものになる。ViewModel が持つハンドル経由で Activity が漏れるため却下

### Decision 3: 命令は Host の待ち行列に積み、1 回遅らせたうえで保留中の反映の完了後に実行する

**採用案:**
- 命令は受けた処理の中では実行せず、Host の待ち行列に積む
- iOS: 積んだら `DispatchQueue.main.async` で 1 回遅らせる。その時点で apply が進行中なら、最後の apply の completion で実行する。進行中かどうかは、全 apply 経路を完了を数える共通の入口に通して判定する (現状 completion を使っていない経路にも付ける)。view が未ロードなら、初回の apply の後に実行する
- Android: 積んだら `post` で 1 回遅らせる。Store の通知の collect は、この時点までに走るか、すでに走っている。その時点で commit 待ちの `submitList` があれば、最後に提出した世代の commit callback で実行する。全 `submitList` 経路を共通の入口に通し、提出のたびに世代番号を進め、最新世代の callback だけを待つ。`AsyncListDiffer` は先行世代の callback を「最新世代ではない」として破棄する (`KsSettingsListAdapter` の既存コメントが記録済み) ので、callback の数は数えない。未 attach・未 bind なら、attach と初回の反映の後に実行する
- Host が生きたまま画面から外れている間 (上に別の画面を重ねた通常遷移) の命令も、同じ待ち行列に入る。Android は window に取り付け直した後に、iOS は次のレイアウトで実行し、表示に戻ったときには命令の位置にある。接続が外れていない以上、未接続の no-op にはしない
- 命令は呼んだ順に取りこぼさず処理する。次の命令を実行するときに先行のアニメーションを止めるので、最終位置は最後の命令のものになる
- 実行の直前に、最新の表示で対象を解決する。見つからなければ no-op とし、debug で警告ログを出す

**理由:** 「追加 → 直後に末尾へ」を素朴に書ける順序保証は、KsCollectionView の契約でライブラリの責務である。iOS は Store から apply まで同期だが、アニメーション付きの apply の途中では行の位置が確定していない。1 回遅らせて completion を待つ形は KsCollectionView の iOS 実装が UIKit だけで実証している。Android は反映が非同期 2 段なので、`post` で collect を先に通し、commit を待つ必要がある。

**代替案:**
- **A: 受けた時点で直ちに実行する** — iOS は snapshot だけは最新だがアニメーション中は位置が確定せず、Android は反映前のデータで解決してしまう。順序保証が成り立たないため却下
- **B: 命令を Store の通知に乗せ、更新と同じ流れで届ける** — core/ADR-0037 の却下案。加えて、Android では Store の通知順に乗せても `submitList` の commit を待つ必要は残る

### Decision 4: 宣言 UI では、宣言の更新が Host に届くのを待ってから手がかりを最終 ID へ引き直す

**採用案:**
- DSL 方式のラッパー (SwiftUI の Representable、Compose の Composable) は、ハンドルと Host の間に引き直しの受け口を置く。ハンドルが接続するのはこの受け口で、受け口は最終 ID に直した命令を Host の待ち行列 (Decision 3) に渡す
- 引き直しは宣言の更新の後に行う。受け口は命令を受けた時点の宣言ツリーの世代を控える。ラッパーの update (Representable の `updateUIViewController`、`AndroidView` の update) が内部 Store へツリーを流すたびに、世代を進める。そのうえで、iOS は `main.async` で 1 回、Android は 1 フレーム遅らせた時点で、現在のツリーで解決する
- この遅延で足りるという前提: 同じ処理 (Button の処理・クリックの処理) で起きた状態変更による update は、その後の 1 回の遅延より前に行われる。SwiftUI は状態変更を現在の実行ループの終わりで反映し、Compose は次のフレームの再コンポジションで反映するため。KsCollectionView が同じ遅延で、状態変更と同じ処理からの末尾命令が新しい配列に届くことをテストで実証している (iOS `KsSwiftUIIntegrationTests` の「State 更新と同一 Button 処理の末尾命令が新 snapshot へ到達する」、Android `KsCollectionViewInteractionTest.scrollToEndReachesItemAddedInTheSameFrame`)。本 change では spec の Scenario「状態の変更と同じ処理から出した命令は新しいツリーで解決される」のテストでこの前提を守る。世代が進まないまま遅延が過ぎたときは現在のツリーで解決する (状態変更を伴わない命令の通常の経路)。テストで前提が崩れたら実装を止めて報告する
- 手がかりから最終 ID は、明示 ID と `ForEach` / `forEach` の key の両方の形で計算する (iOS `DSLIdentityUUID.uuid(from:)`、Android `DSLIdentityId.id(from)`)。いまの宣言ツリーに存在するほうを採る。両方あれば明示 ID を採る (core/ADR-0036 と同じ優先)。どちらも無ければ no-op
- Store 方式と UIKit / View Host では、ハンドルは Host に直接つなぐ。ID は Cell / Section の ID そのもの (iOS は `KsCellID` と Section の `UUID`、Android は String)

**理由:** 宣言 UI の利用者は、自分が書いた明示 ID か key しか知らない (agenda 論点5)。最終 ID は手がかりから決まる純関数で求まるので、対応表を持たずに引き直せる。状態変更と同じ処理から出した命令が新しいツリーで解決されるには、宣言の更新が内部 Store に届いてから引き直す必要がある。

**代替案:**
- **A: 手がかりから最終 ID を引く手段を利用者に公開し、ハンドルは常に最終 ID で受ける** — agenda 論点5 の却下案 (最終 ID を利用者に開き、書き方が 2 段になる)

### Decision 5: 位置は表示中の要素で解決し、先頭・末尾は内容の両端とする

**採用案:**

| 対象 | start | center | end |
|---|---|---|---|
| Cell | 行の上端を表示範囲の上端へ | 行の中央を表示範囲の中央へ | 行の下端を表示範囲の下端へ |
| Section | Section の範囲の上端を上端へ | 範囲の中央を中央へ | 範囲の下端を下端へ |

- Section の範囲は、表示されている見出し・Cell・Footer をつないだもの。上端は、それらのうち最初の要素 (見出しがあれば見出し、無ければ最初の表示 Cell、それも無ければ Footer) の上端とする。下端は、最後の要素 (Footer があれば Footer、無ければ最後の表示 Cell、それも無ければ見出し) の下端とする。表示されている要素が 1 つも無い Section (空で見出しも Footer も無い) は no-op
- `scrollToStart` は内容の最上端 (Root Header を含む)、`scrollToEnd` は内容の最下端 (Root Footer を含む) へ送る
- 到達できない位置はスクロール可能範囲で止める。表示範囲より高い対象は start に合わせる。表示範囲は、安全領域や内容の余白を除いた実際に見えている領域とする
- 非表示の Cell / Section、存在しない ID は no-op
- アニメーションは行き過ぎて戻らず、一方向で着地する
  - iOS: 行は `scrollToItem`、見出しと両端はレイアウト属性から求めた位置へ `setContentOffset` で送る。推定高さによるずれは、到着後に進行方向と同じ向きでだけ補正する。アニメーションしないときは、確定した位置へ送り直して詰める
  - Android: `LinearSmoothScroller` に start / center / end の合わせを持たせる。対象が配置された時点で、距離を解き直して一方向に止める。アニメーションしないときは `scrollToPositionWithOffset` で送り、配置後に補正する

**理由:** Cell の位置の意味は KsCollectionView と同じにする。Section は見出しごと見せる決定 (agenda 論点1) を、範囲の合わせとして一般化した。先頭を Root Header を含む最上端にするのは KsCollectionView と同じで、末尾もそれと対称にする。設定画面は末尾の Section の Footer や Root Footer に注記 (バージョン表記など) を置くことが多い。RecyclerView の `LinearSmoothScroller` は、画面外の対象が配置された時点で距離を求め直す。推定高さに頼らずに一方向で止められる。

**代替案:**
- **A: 末尾は最後の表示 Cell の下端とする (KsCollectionView と同じ)** — 最後の Section の Footer と Root Footer が隠れ、先頭 (Root Header を含む最上端) と非対称になるため却下
- **B: Android も KsCollectionView と同じく推定高さから一度に送り、残差を後から詰める** — LazyGrid の `animateScrollToItem(index, offset)` 前提の手法である。RecyclerView には、対象の配置時に距離を解き直す標準の手段 (`LinearSmoothScroller`) があるので却下

### Decision 6: 位置を控える・戻す窓口を、中身を見せない値で Host の公開 API に足す

**採用案:**
- 両 OS の Host に `captureScrollAnchor(): KsScrollAnchor?` と `restoreScrollAnchor(anchor)` を足す
- `KsScrollAnchor` は中身を公開しない値にする (Swift は `Sendable` な struct、Android は `Parcelable`)
- 控えるのは、表示範囲の上端にかかる最初の要素 (Cell 行・Section 見出し / Footer・Root Header / Footer) の種類と ID、上端からのずれ
- 戻すのは命令と同じ待ち行列 (Decision 3) を通す。初回の反映とレイアウトの後に実行され、後から来た命令よりも先に処理される
- 控えた要素が見つからなければ何もしない (先頭のまま)

**理由:** MAUI の Bridge は Host とは別モジュールで、Host の作り直しをまたいで位置を運ぶには Host の外から控えて戻す口が要る (maui/ADR-0030)。Bridge が使う既存の Host API (`invalidateAccessoryMeasurement`、`rootHeader` など) は public で揃っている。`KsScrollAnchor` は Bridge 専用の型ではない。Android の View Host 自身が Activity の作り直しで使い (Decision 7)、UIKit Host を作り直す利用者も使える。したがって「既存 UI 層へ interop 都合の型を持ち込まない」(concepts maui/api/native-bridge.md の禁止事項) にはあたらない。中身を見せない値なら、利用者が座標を読む API にはならない (ADR-0030 が却下した「位置を読む命令」にあたらない)。一方で、UIKit Host を利用者が作り直す場合にも使える。要素の ID で控えるので、離れている間に項目が増減しても同じ要素の場所へ戻れる。

**代替案:**
- **A: スクロール量 (contentOffset / ピクセル) で控える** — 離れている間の項目の増減で別の場所に戻るため却下
- **B: Bridge だけに見せる (Swift の SPI / package 可視性、Kotlin の `@RestrictTo`)** — このリポジトリにこれらの前例が無い。iOS の Bridge は SwiftPM とは別に Xcode プロジェクトからソースを直接ビルドしており、package 境界の可視性に頼れるか不確かなため却下
- **C: 控えた要素が消えていたとき、近くの要素 (前後の要素) も控えておいて戻す** — 控える情報と照合の仕組みが重くなる。離れている間に表示範囲の先頭の要素そのものが消えるのは稀なため、見送る

### Decision 7: Android の View Host は、Activity の作り直しで保存状態から位置を戻す

**採用案:**
- 既存の保存状態 (`SavedState`、カレンダーの状態を持つ) に `KsScrollAnchor` を足す
- 復元はカレンダーと同じく、attach と root の反映がそろってから行う (Decision 6 の戻し方)
- 既定 id の View が同じ階層に複数あるときは保存も復元もしない、という既存の規則 (android/ADR-0021) に従う
- Compose の `AndroidView` の中の View Host も同じ経路で戻る。Compose UI の `ViewFactoryHolder` (1.9.5 で確認) は、Compose の保存状態の仕組み (`SaveableStateRegistry`) に登録して、中の View を `saveHierarchyState` で保存し、作り直しで `restoreHierarchyState` する。View Host は既定の id を持つので、その保存状態に含まれる

**理由:** 同じ領域の隣接課題として同じ change に含めると決めた (agenda 論点4)。Android では、画面の状態を保存状態で戻すのが標準の作法である。内部の RecyclerView は id を持たず、LayoutManager の状態は保存されない。要素の ID で控える `KsScrollAnchor` を保存状態に載せれば、Decision 6 の戻し方を共有できる。

**代替案:**
- **A: 内部の RecyclerView に id を付け、LayoutManager の状態を OS に保存させる** — 位置 (行番号) で戻るので、作り直しの間に項目が増減すると別の場所に戻る。また View Host が複数あると id が衝突するため却下

### Decision 8: Bridge は Native のハンドルを 1 つ持ち、Host を手放すときに位置を控え、次の Host で戻す

**採用案:**
- iOS / Android の `KsSettingsBridge` は `KsScrollController` を 1 つ持つ
- `makeHost*` で Host を作ったら、ハンドルを Host につなぐ。控えた位置があれば `restoreScrollAnchor` してから返す (待ち行列の先頭に入る)
- `releaseHost` では、Host を手放す前に `captureScrollAnchor` で控え、接続を外す
- `dispose` で控えとハンドルを捨てる
- Bridge の公開 API に次を足す (Store 操作 1:1 の枠外。setStyle と同じ扱い)
  - `scrollToCell(cellID: String, position: Int, animated: Bool)`
  - `scrollToSection(sectionID: String, position: Int, animated: Bool)`
  - `scrollToStart(animated: Bool)` / `scrollToEnd(animated: Bool)`
- 位置は整数で運ぶ (0 = start、1 = center、2 = end。setStyle の整数輸送と同じ)
- Host 不在・破棄後・未知の ID は no-op
- iOS binding の `ApiDefinition.cs` に手書きで追加する。Android は自動束縛のため Metadata.xml の変更は要らない見込み

**理由:** 位置の保持者は、スタイル切替と同じく Host の世代をまたいで生きる Bridge (maui/ADR-0030)。Bridge が Native のハンドルを持って Host ごとにつなぎ直せば、Host 不在中の命令は Native の未接続 no-op にそのまま落ちる。MAUI 側で Host の有無を判定する必要が無い。

**代替案:**
- **A: Bridge が Host の命令用メソッドを直接呼ぶ (ハンドルを持たない)** — Host 不在時の no-op と最後の接続の規則を Bridge 側で再実装することになり、Native の契約と二重になるため却下

### Decision 9: MAUI は `IScrollController` と独自の `ScrollPosition` で公開し、準備完了はコマンドで知らせる

**採用案:**

```csharp
namespace KsSettingsView;

public interface IScrollController
{
    void ScrollTo(object target, ScrollPosition position = ScrollPosition.Start, bool animated = true);
    void ScrollToSection(object target, ScrollPosition position = ScrollPosition.Start, bool animated = true);
    void ScrollToStart(bool animated = true);
    void ScrollToEnd(bool animated = true);
}

public enum ScrollPosition { Start, Center, End }
```

- `SettingsView.ScrollController` (型 `IScrollController`): BindableProperty で、既定の向きは OneWayToSource。実体は SettingsView ごとに作る internal の実装で、`defaultValueCreator` で生成する。外から別の値を代入されても自分の実体に戻す
- `SettingsView.ScrollControllerReadyCommand` (型 `ICommand`): Host の生成と取り付けが済むたびに `Execute(null)` する (`CanExecute(null)` が真のとき)。合図は「命令が効くようになった時点」を知らせるもので、復元が画面に反映し終わるのは待たない。Bridge は位置の復元を `makeHost*` の中で待ち行列の先頭に積むので、合図の中で出した命令は復元の後に実行される (agenda 論点4 の決定文言をセカンドオピニオン M1 を受けてこの意味に明確化 — オーナー判断)
- `Section.SectionId` / `CellBase.CellId` (型 `string?`): 手で並べた要素用の明示 ID
- 命令は `IScrollController` → `KsSettingsController` → gateway → Bridge の順に届く。gateway に 4 メソッドを足し、fake gateway に記録を足す

**理由:** 形は maui/ADR-0029 と agenda 論点3 の決定どおり。位置の enum は Native の `KsScrollPosition` と同じ 3 値にする。MAUI 標準の `ScrollToPosition` には `MakeVisible` があり、Native と KsCollectionView の語彙に対応が無い。準備完了の名前には対象 (スクロールのハンドル) を含める。SettingsView は将来ほかの命令ハンドルを持ちうるためで、ColorAnalyzer の `ReadyControllerCommand` の形を踏襲する。準備完了を Host の生成ごとに知らせるのは、Activity の作り直しの後にも ViewModel が命令を出し直せるようにするため。1 回だけにしたい ViewModel は自分で判定する。明示 ID の名前は、MAUI の `Element.Id` (Guid) と衝突しない。また Native の宣言 UI の `sectionID` / `cellID` と対応させる。既存の `AutomationId` は UI テスト用の意味なので流用しない (agenda 論点3-2)。

**代替案:**
- **A: MAUI 標準の `ScrollToPosition` を再利用する** — `MakeVisible` (見えていれば動かさない) に Native の対応が無い。受けるなら Native に語彙を足すか、黙って別の値に読み替えることになるため却下
- **B: 復元が画面に反映し終わってから合図を出す** — ViewModel は位置を読めないため反映完了を待つ利得が無く、命令の順序は待ち行列で保証できる。Native から MAUI まで復元完了の通知経路を足す費用だけが残るため却下 (オーナー判断)
- **C: 準備完了を最初の Host のときだけ知らせる** — Activity の作り直しや同じページの再 Push の後に、ViewModel が命令を出し直す機会が無くなるため却下

### Decision 10: MAUI の対象は明示 ID を先に、次に ItemsSource の項目の並びで解決する

**採用案:**
- `ScrollTo` は Cell、`ScrollToSection` は Section を探す
- まず表示順に、明示 ID (`CellId` / `SectionId`) が `target` と等しい要素を探す
- 無ければ、ItemsSource から生成した要素を探す。探し方は、binder が持つ生成元の項目の並びで `target` と等しい項目を見つけ、同じ位置の生成物を採る
- 現状の binder (`KsItemsSourceBinder`) は生成物の並びだけを持ち、生成元の項目を持たない。生成・追加・削除・置換・移動・作り直しの各経路で、生成物と同じ並びの項目の並びを保持するようにする。`ItemsSource` は `IEnumerable` なので、命令のたびに列挙し直して対応を推測はしない
- 最初に見つかった要素の Bridge ID (`FindCellId` / `FindSectionId`) で gateway を呼ぶ
- 見つからない・gateway 未接続なら no-op とし、Debug 出力に英語の警告を出す
- 非表示の要素はそのまま Native に渡す (Native が no-op にする)

**理由:** 明示 ID を先にするのは、宣言 UI の「明示 ID が勝つ」(core/ADR-0036) と同じ優先にするため。照合に `BindingContext` を使わず項目の並びで対応を取るのは、生成物の `BindingContext` を利用者が差し替えても、元の項目との対応が崩れないようにするため。binder は生成物を項目と同じ並びで持っている。

**代替案:**
- **A: 生成物の `BindingContext` と `target` を照合する** — 利用者が生成物の `BindingContext` を差し替えると対応が崩れる。手で並べた要素が親から継承した `BindingContext` にも当たってしまうため却下

### Decision 11: Sample は 3 platform 共通の「スクロール制御」画面を足し、操作は一覧の中の ButtonCell で行う

**採用案:**
- 画面の構成は 3 platform で同一の文言・構成にする (sample-parity)
  - 先頭に操作の Section を置く。ButtonCell は「末尾へ」「Section へ」「Cell を中央へ」「追加して末尾へ」
  - 続けて番号付きの Section を複数並べる (各 Section に LabelCell と Footer)
  - 最後の Section に「先頭へ」を置き、Root Footer を付ける
- Native はハンドルを View が持つ (SwiftUI の `@State`、Compose の `rememberScrollController()`)
- MAUI は ViewModel が `IScrollController` を OneWayToSource で受け取り、`ScrollControllerReadyCommand` で準備完了を控える (画面には出さず、使い方をコードで示す)
- 開いたときの自動スクロールはしない
- Host の作り直し後の位置は、MAUI の既存の再接続の検証手順 (MauiHost のページ再訪問) で確かめる

**理由:** 操作を一覧の中の ButtonCell にすると、platform ごとの画面 chrome に依存せず、文言と構成をそろえやすい。自動スクロールを入れると、開いた直後に操作の Section が画面外に出て、デモとして分かりにくい。準備完了の使い方は MAUI 固有の API なので、画面の差を生まない形でコードに示す。

**代替案:**
- **A: 開いたときに特定の Section へ送る (3 platform 共通)** — 準備完了の使いどころは示せるが、操作の Section が画面外から始まり、デモとして分かりにくいため見送る

## Risks / Trade-offs

- 全 apply 経路 (iOS) と全 `submitList` 経路 (Android) を共通の入口に通すので、既存の反映のタイミングを変えるおそれがある → 入口は完了を数えるだけで反映の順序は変えない。既存の反映・付け外しのテスト (`ApplyDiffTests` / `DiffableDataSourceTests`、`ListAdapterDiffTest` / `AdapterReattachTest`) を回帰の網にする
- iOS で画面外の Section 見出しの位置は、推定高さのまま求まることがある → 到着後の同じ向きの補正と、アニメーションなし時の送り直しで詰める。一方向の着地は実機で動きの過程を見て確かめる (runtime-behavior-verification)
- Android の MAUI では、保存状態からの復元 (Decision 7) と Bridge の復元 (Decision 8) が両方効きうる → どちらも同じ要素 ID のアンカーを戻すだけなので結果は同じ。ただし順序の実測は実装で行う
- Host に公開 API (アンカー) が増える → 中身を見せない値に限り、座標を読む API にはしない
- Kotlin の `DSLIdentityId` は internal のまま、同じモジュール内の Compose ラッパーからだけ使う
- 既定 id の View Host が同じ階層に複数あるときに保存を避ける既存の判定が、Compose の `AndroidView` の中でどう働くか (ComposeView の中の他の View Host を数えるか) は実装で確かめる。数えて保存を避ける場合も、既存の規則どおりの結果として扱う

## Migration Plan

公開 API の追加のみで、既存利用者の移行は無い。Store / Diff / 既存の Bridge API は変えない。Android の保存状態の形式に項目が増えるが、保存状態はアプリの版をまたいで持ち越さないので移行は要らない。

## Open Questions

- なし。提案時点で Open Question としていた「Compose の `AndroidView` の中の View Host が Activity の作り直しで保存状態を受け取るか」は、Compose UI の実装を確認して解消した (Decision 7)。実装では Robolectric で実際に戻ることをテストで確かめる

## ADR 候補

新規の候補は無い。本 change の決定は起票済みの proposed ADR に対応し、蒸留時に accepted へ昇格する。

- Decision 1〜4 → core/ADR-0037 (スクロール制御は KsCollectionView と同形の命令ハンドルを Host につないで提供し、Store を経由しない)
- Decision 9・10 → maui/ADR-0029 (MAUI のスクロール命令は、インターフェース型のハンドルを SettingsView が作って ViewModel へ渡して提供する)
- Decision 6〜8 → maui/ADR-0030 (Host の作り直しをまたぐスクロール位置は、Bridge が控えて次の Host で戻す)

Decision 5 (位置の意味・末尾の定義) と Decision 11 (Sample) は、書き換えても ADR のタイトルが変わらない設計として本書と concepts に残す。
