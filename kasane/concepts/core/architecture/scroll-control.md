---
type: concept
title: スクロール制御
description: 命令ハンドルで Cell・Section・内容の両端へスクロールさせ、表示位置を控えて Host の作り直しをまたいで戻す platform 共通の契約
tags: [architecture, scroll, host, public-api]
timestamp: 2026-09-30
---

# スクロール制御

この文書は、iOS / Android / MAUI に共通するスクロール制御の契約を説明する。読むと、命令ハンドル `KsScrollController` をどこにつなぐか、4 種の命令と位置 (start / center / end) が何を意味するか、命令がいつ実行されどんなときに何もしないか、表示位置を控えて Host の作り直しをまたいで戻す仕組みが分かる。Host と Store の関係を先に知りたい場合は [Native Host の責務境界](native-host-boundary.md) と [Store の状態と更新通知](store-and-update-streams.md) を先に読む。

Host は、Store の内容を一覧として描画する platform ごとの表示部品 (iOS の `KsSettingsViewController`、Android の View `KsSettingsView`) である。宣言 UI (SwiftUI / Compose) の `KsSettingsView` は内部で同じ Host を使い、利用者が作った Store を渡す **Store 方式**と、DSL で設定ツリーを書きライブラリが内部に Store を持つ **DSL 方式**の 2 通りの書き方がある。DSL 方式では利用者が指す ID (自分で付けた明示 ID や `ForEach` の key) と内部 Store 上の ID が異なるため、命令を内部 Store 上の ID に変換してから実行する。この文書ではこれを**引き直し**と呼ぶ。

## 目的

設定画面では「開いたら特定の Section へ案内する」「先頭へ戻す」「項目を足して末尾を見せる」のように、プログラムからスクロールさせたい場面がある。スクロール制御は、これを一度きりの命令として出す手段である。形は同系統のライブラリ KsCollectionView の命令ハンドルと同じにしてあり、両ライブラリで同じ書き方になる。

命令は Store の公開操作・通知に乗せず、表示側の Host につないだハンドルから届く ([core/ADR-0037](../../../decisions/core/0037-scroll-control-handle-attached-to-host.md))。スクロール位置は Store の状態ではないため、Store の現在状態から表示を復元する経路 ([core/ADR-0019](../../../decisions/core/0019-host-restores-from-store-on-attach.md)) にも乗らない。Store を通さない理由 (Store の契約を表示の命令へ広げない・同じ Store を購読する他の画面を動かさない) は ADR に記録がある。

## 命令ハンドルと接続

```text
利用者 (View / ViewModel)
   │ scrollTo / scrollToSection / scrollToStart / scrollToEnd
   ▼
KsScrollController ─(弱参照)→ 受け口 ─→ Host の待ち行列 ─→ 反映とレイアウトの後に実行
                              │
                              ├ UIKit Host / Android View Host / Store 方式の宣言 UI: Host そのもの
                              └ DSL 方式の宣言 UI: ID を内部 Store 上の ID へ引き直す受け口 (DSLScrollCommandResolver)

SettingsRootStore ─→ Host (データの反映)   ※ 命令はこの経路を通らない
```

| | iOS | Android | MAUI |
|---|---|---|---|
| ハンドル | `KsScrollController` (`@MainActor` の final class)。protocol `KsScrollControlling` | `KsScrollController`。interface `KsScrollControlling`。Compose は `rememberScrollController()` | `IScrollController` (SettingsView が作って ViewModel へ渡す — [maui/ADR-0029](../../../decisions/maui/0029-scroll-handle-interface-created-by-settingsview.md)) |
| 接続口 | `KsSettingsViewController.scrollController`。SwiftUI は `.scrollController(_:)` 修飾子 | `KsSettingsView.scrollController`。Compose は `KsSettingsView(...)` の `scrollController` 引数 | `SettingsView.ScrollController` (既定 OneWayToSource) と `ScrollControllerReadyCommand` |
| 命令で指す ID | Store 方式・UIKit Host は Cell の `KsCellID` と Section の `id`。DSL 方式は明示 ID か `ForEach` の key | Store 方式・View Host は Cell / Section の `id`。DSL 方式は明示 ID か `forEach` の key | 明示 ID (`CellId` / `SectionId`) の文字列、または ItemsSource の項目オブジェクトそのもの。明示 ID が等しい要素を優先し、無ければ等しい項目から生成された要素を採る |

protocol / interface は、ViewModel がライブラリの実装クラスに依存せず、テストで命令を記録するだけの実装に差し替えるためにある。接続口が受けるのは実装クラスだけで、利用者が自作した実装を Host につなぐ組み合わせは作らない。MAUI の公開形と対象の解決は [MAUI facade](../../maui/api/maui-facade.md) にある。

MAUI の `ScrollControllerReadyCommand` は、ハンドルの命令が効くようになったとき (Host の生成と取り付けが済んだとき。Host が作り直されるたび) に SettingsView が実行する Command である。利用者は、開いた直後に出したい命令をここで出す。

接続の規則は次のとおり。

- 1 つのハンドルが命令を届ける先は、最後に接続した受け口だけである。別の Host へつなぎ替えると、前の Host には届かなくなる (debug で警告ログ)。
- ハンドルは受け口を弱参照で持つ。ViewModel がハンドルを画面より長く持っても、Host (Android では Host が持つ Activity) の寿命は延びない。Host が破棄されるとハンドルは未接続に戻る。
- 接続口に `nil` / `null` を代入する、Store から切断する (iOS `disconnectStore()`・Android `unbind()`)、Host が破棄される、のいずれかで接続が外れる。Store からの切断では接続口のプロパティも空に戻り、再び Store をつないでも自動ではつなぎ直さない。
- 未接続のハンドルへの命令は何もしない。例外も警告も出さない。
- 接続口のハンドルを差し替えた・外したときは、その Host に積まれた未実行の命令を捨てる。控えた位置の復元 (下記) は Host 自身への要求なので捨てない。宣言 UI の修飾子・引数でハンドルを差し替えた場合も、引き直し前の命令と Host に渡した未実行の命令を同じく捨てる。

命令はメインスレッドから出す。iOS は `@MainActor` が静的に守る。Android はメインスレッド以外からの命令を `KsCellRegistry.strictMode` (既定 `true`) のとき例外で知らせ、`false` のときはメインスレッドへ post して実行する。

## 命令と位置

| 命令 | 対象 | `position` |
|---|---|---|
| `scrollTo(id:position:animated:)` | Cell の行 | 既定 start |
| `scrollToSection(id:position:animated:)` | Section の範囲 (見出しごと) | 既定 start |
| `scrollToStart(animated:)` | 内容の最上端 (Root Header を含む) | — |
| `scrollToEnd(animated:)` | 内容の最下端 (Root Footer を含む) | — |

`animated` の既定は `true`。位置 `KsScrollPosition` は 3 値で、対象を表示範囲のどこへ合わせるかを表す。

| 位置 | 合わせ方 |
|---|---|
| start | 対象の上端を表示範囲の上端へ |
| center | 対象の中央を表示範囲の中央へ |
| end | 対象の下端を表示範囲の下端へ |

表示範囲は、安全領域と一覧の内容の余白を除いた実際に見えている領域である。

### Section の範囲

Section への命令は Cell と同じ規則で、Section の範囲を合わせる。範囲は、その Section で表示されている見出し・Cell・Footer をつないだものである。上端は見出し、無ければ最初の表示 Cell、それも無ければ Footer の上端とする。下端は Footer、無ければ最後の表示 Cell、それも無ければ見出しの下端とする。これにより start で送ると見出しが表示範囲の上端に来る。

先頭・末尾を最後の Cell ではなく内容の両端 (Root Header / Root Footer を含む) にしているのは、設定画面が末尾の Section の Footer や Root Footer に注記 (バージョン表記など) を置くことが多いためである。

### 届かない位置と何もしない命令

- 到達できない位置はスクロール可能範囲の端で止まる (内容の末尾付近の Cell を start に合わせようとしても行き過ぎない)。
- 表示範囲より高い対象は、指定した位置によらず start に合わせる。
- 非表示の Cell / Section、表示される要素が 1 つも無い Section、存在しない ID への命令は何もしない。存在しない ID は debug で警告ログを出す。
- 宣言 UI (DSL 方式) では、明示 ID と key のどちらにも無い値は何もしない。同じ値が両方にあれば明示 ID の要素を採る ([core/ADR-0036](../../../decisions/core/0036-explicit-id-wins-over-collection-key.md) と同じ優先)。

## 命令を実行する時点

命令は受けた処理の中では実行せず、Host の待ち行列に積む。メインスレッドの次の周回まで 1 回遅らせ (iOS は `DispatchQueue.main.async`、Android は `Handler.post`)、そのうえで保留中のデータの反映 (Store の更新の表示への適用) とレイアウトが済んでから実行する。これにより「Store へ Cell を追加して、同じ処理で `scrollToEnd()`」と素朴に書いても、追加した Cell を含む末尾へ届く。反映の完了を待つ仕組みは platform で異なる (iOS は diffable data source の snapshot の apply の完了、Android は最後に提出した `submitList` の差分の適用完了 (commit callback) とその後のレイアウト)。

| 場面 | 扱い |
|---|---|
| 続けて複数の命令を出した | 呼んだ順に取りこぼさず処理する。次の命令を実行するときに先行のアニメーションを止めるので、最終位置は対象が見つかった最後の命令のものになる |
| 実行前に対象が消えた | 実行の直前に最新の表示で対象を解決し、無ければその命令だけを飛ばす。後続の命令は処理を続ける |
| 対象の無い命令 | 先行のアニメーションを止めない (何もしない命令が進行中のスクロールの行き先を変えない) |
| 画面の読み込み前・window への取り付け前に出した | 初回の表示とレイアウトの後に実行する |
| Host が生きたまま画面から外れている (上に別の画面を重ねた遷移) | 未接続の no-op にはしない。取り付け直した後のレイアウトで実行し、戻ったときには命令の位置にある |
| DSL 方式で状態の変更と同じ処理から出した | 宣言の更新が内部 Store へ届くのを待ってから ID を引き直すので、変更後の宣言ツリーで解決される |

`animated` が `true` の命令は、対象へ向かって一方向に進み、行き過ぎてから戻る動きをしない。画面外の行・見出しの高さは推定値で、スクロール中に実測へ変わって行き先が動くため、両 OS とも行き先を途中で求め直しながら進める (iOS は [UIKit Host](../../ios/api/ios-native-host.md)、Android は [View Host](../../android/api/android-native-host.md) の「スクロール制御」を参照)。`false` の命令はアニメーションせずに最終位置へ移る。一度送った後、行の高さが実測に置き換わって行き先がずれたら、確定した高さで位置を求め直して合わせる。

## 位置を控える・戻す窓口

両 OS の Host は、表示位置を控える `captureScrollAnchor()` と、控えた位置へ戻す `restoreScrollAnchor(_:)` を公開する。控えは中身を公開しない値 `KsScrollAnchor` (iOS は `Sendable` な struct、Android は `Parcelable`) で、利用者が座標を読む API ではない。この窓口は、MAUI の Bridge (MAUI の SettingsView と iOS / Android の Host をつなぐ内部部品) と Android の保存状態が Host の作り直しをまたいで位置を運ぶ口であり、UIKit Host を自分で作り直す利用者も使える。

### 控える

控えるのは、表示範囲の上端にかかる最初の要素 (Cell の行・Section の見出しと Footer・Root Header と Root Footer のいずれか) の種類と ID と、上端からのずれである。座標ではなく要素の ID で控えるので、離れている間に上に項目が増減しても同じ要素の場所へ戻れる。

控えられる内容が無いとき (読み込み前・内容が空など) は空 (`nil` / `null`) を返す。Host が window から外れた後も現在の表示位置は控えず空を返す — 外れた後のレイアウトは window の寸法と安全領域を失っており、そこから選んだ要素は利用者が見ていた位置と一致しないためである。ただし、まだ実行していない位置の復元が待ち行列にあれば、外れていてもその控えを返す (戻し切る前に再び控えても位置を失わない)。

### 戻す

戻しは命令と同じ待ち行列を通り、データの反映とレイアウトの後に、控えた要素が同じずれで表示範囲の上端にかかる位置へアニメーションなしで送る。戻しより後に出した命令は戻した後に実行されるので、命令が最終位置を決める。控えた要素が見つからなければ何もしない (位置は変えない。作り直した直後の Host なら先頭のまま)。控えた要素の近くの要素へ代わりに戻す仕組みは持たない。

### 祖先の寸法が変わったときの合わせ直し

Host のレイアウトの中で命令や戻しを実行すると、それに連動して祖先の寸法が変わることがある (大きいタイトルのナビゲーションバーが縮んで表示範囲が伸びる)。このとき、最後に位置を決めたのが Cell・Section・先頭・末尾への命令なら、その行き先と位置を新しい表示範囲で解き直す。戻しなら、寸法が変わる前に控えた上端の要素とずれを保つ。新しい命令が届いている、または命令のアニメーションが進行中のときは合わせ直さない。

祖先の寸法の変化に Host が追従しないと、表示範囲の下端に余白が残り、命令で合わせた位置も新しい表示範囲からずれる。iOS の UIKit Host は、レイアウトを終えた後に祖先の view をレイアウトし直してから合わせ直す。Android の View Host はこの合わせ直しを持たない (未対応。中央・下端に合わせた命令を新しい表示範囲で解き直す処理は無い)。

## Host の作り直しをまたぐ位置の保持

同じ Host が生きている間の付け外し (上に画面を重ねる通常遷移) では位置はそのまま残る。Host そのものが作り直されるときに位置を運ぶ主体は、場面ごとに次のとおりである。

| 場面 | 位置を控えて戻す主体 | 詳細 |
|---|---|---|
| MAUI の Handler の切断と再接続 (同じページの再 Push・Android の Activity の再生成) | Bridge。iOS / Android の `KsScrollController` を 1 つ持って Host を作るたびにつなぎ直し、Host を手放すときに控え、次の Host で戻す ([maui/ADR-0030](../../../decisions/maui/0030-scroll-position-kept-across-host-by-bridge.md))。戻しは `ScrollControllerReadyCommand` の中で出した命令より先に処理される | [Native Bridge](../../maui/api/native-bridge.md)、[MAUI の描画と lifecycle](../../maui/api/maui-rendering-lifecycle.md) |
| Android の View Host の Activity の再生成 (Compose の `AndroidView` の中の Host を含む) | View Host 自身。保存状態 (SavedState) に `KsScrollAnchor` を載せ、復元後に戻す | [Android Native Host](../../android/api/android-native-host.md) |
| iOS の UIKit Host を利用者が作り直す | 自動では保持しない。利用者が窓口で控えて戻す | [iOS Native Host](../../ios/api/ios-native-host.md) |

iOS に自動の保持が無いのは、Android の保存状態に当たる標準の作法が UIKit に無いためである (UIKit の state restoration には対応しない)。

## 保証すること

- 命令は Store の状態・通知を変えず、同じ Store を表示する他の画面を動かさない。
- 同じ処理の中で行った Store の更新 (DSL 方式では状態の変更) は、命令より先に表示へ反映される。
- 続けて出した命令の最終位置は、対象が見つかった最後の命令のものになる。
- 未接続・非表示・存在しない ID への命令は例外を起こさず、位置を変えない。
- アニメーション付きの命令は一方向に進んで着地し、行き過ぎて戻らない。
- ハンドルは Host の寿命を延ばさない。
- 控えた位置は要素の ID で戻るため、離れている間の項目の増減で別の場所へ戻らない。

## してはいけないこと

- 命令を Store の公開操作や通知に足さない (core/ADR-0037 の却下案)。
- スクロール先を状態 (bool プロパティなど) で表さない。一度きりの命令を状態で表すと、実行後に誰が状態を元の値へリセットするかの約束が利用者に漏れる。
- `KsScrollAnchor` の中身を公開して、利用者が現在位置を読む API にしない (現在位置を利用者が読み取る API は maui/ADR-0030 で却下済み)。
- 位置の控えをスクロール量 (contentOffset・ピクセル) で持たない。項目の増減で別の場所に戻る。
- 命令を受けた処理の中で即座に実行しない。反映前のデータで対象を解決してしまう。

## 用語

| 用語 | 意味 |
|---|---|
| Host | Store の内容を一覧として描画する platform ごとの表示部品 (iOS の `KsSettingsViewController`、Android の View `KsSettingsView`) |
| Store 方式 / DSL 方式 | 宣言 UI (SwiftUI / Compose) の 2 通りの書き方。Store 方式は利用者が作った Store を渡し、DSL 方式は DSL で設定ツリーを書いてライブラリが内部に Store を持つ |
| 引き直し | DSL 方式で、利用者が指す ID (明示 ID / key) を内部 Store 上の ID へ変換すること |
| Bridge | MAUI の SettingsView と iOS / Android の Host をつなぐ内部部品 |
| 命令ハンドル | `KsScrollController` (MAUI は `IScrollController`)。画面のライフサイクルに依存しない、命令を出すためのオブジェクト |
| 受け口 | ハンドルが命令を届ける先。Host そのもの、または DSL 方式で ID を引き直す内部の受け口 |
| 待ち行列 | Host が命令と位置の復元を積み、反映とレイアウトの後に順に実行する列 |
| 表示範囲 | 安全領域と内容の余白を除いた、実際に見えている領域 |
| スクロール可能範囲 | スクロール位置が取り得る範囲 (内容の最上端が表示範囲の上端に来る位置から、最下端が下端に来る位置まで) |
| 控え | `KsScrollAnchor`。表示範囲の上端にかかる要素の ID とずれ |

## 関連

- [Native Host の責務境界](native-host-boundary.md)
- [Store の状態と更新通知](store-and-update-streams.md)
- [宣言 UI と Native Host の Bridge](declarative-ui-bridge.md)
- [宣言ツリーの identity](declarative-tree-identity.md)
- [iOS Native Host](../../ios/api/ios-native-host.md) / [iOS SwiftUI](../../ios/api/ios-swiftui.md)
- [Android Native Host](../../android/api/android-native-host.md) / [Android Compose](../../android/api/android-compose.md)
- [MAUI facade](../../maui/api/maui-facade.md) / [Native Bridge](../../maui/api/native-bridge.md)
