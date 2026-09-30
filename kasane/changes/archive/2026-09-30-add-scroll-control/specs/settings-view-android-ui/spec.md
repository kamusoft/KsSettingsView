# Delta: settings-view-android-ui (add-scroll-control)

## ADDED Requirements

### Requirement: スクロール命令ハンドルの公開

パッケージ `jp.kamusoft.kssettingsview.ui` は、画面のライフサイクルに依存しない命令ハンドル `KsScrollController` (引数なしで生成できる class) と、それが実装する interface `KsScrollControlling`、位置 `KsScrollPosition` (`Start` / `Center` / `End`) を公開する (SHALL)。命令は次の 4 種で、`position` の既定は `Start`、`animated` の既定は `true` とする (SHALL)。

- Cell へ: `scrollTo(id, position, animated)`
- Section へ: `scrollToSection(id, position, animated)`
- 先頭へ・末尾へ: `scrollToStart(animated)` / `scrollToEnd(animated)`

パッケージ `jp.kamusoft.kssettingsview.compose` は、再コンポジションをまたいで同じハンドルを返す `rememberScrollController()` を公開する (SHALL)。どの Host にも接続していないハンドルへの命令は何もしない (SHALL)。命令はメインスレッドから呼ぶ。debug ビルドでは、メインスレッド以外からの呼び出しを例外で知らせる (SHALL)。

#### Scenario: 未接続のハンドルへの命令は何も起こさない
- **GIVEN** どの Host にも接続していない `KsScrollController`
- **WHEN** 4 種の命令をそれぞれ呼ぶ
- **THEN** 例外は起きず、何も起こらない

#### Scenario: interface 型の変数からも同じ命令を出せる
- **GIVEN** Host に接続した `KsScrollController` を `KsScrollControlling` 型の変数で保持している
- **WHEN** その変数から `scrollToEnd()` を呼ぶ
- **THEN** Host は内容の末尾へスクロールする

#### Scenario: rememberScrollController は再コンポジションで同じハンドルを返す
- **GIVEN** `rememberScrollController()` を呼ぶ Composable
- **WHEN** 再コンポジションが起きる
- **THEN** 同じ `KsScrollController` インスタンスが返る

#### Scenario: debug ビルドでメインスレッド以外からの命令を検出する
- **GIVEN** debug ビルドで Host に接続した `KsScrollController`
- **WHEN** バックグラウンドのスレッドから `scrollToEnd()` を呼ぶ
- **THEN** 例外が送出される

### Requirement: Host への接続と最後の接続

View Host (`KsSettingsView`) は `scrollController` プロパティを公開する (SHALL)。`KsScrollController` を代入すると、そのハンドルの命令が Host に届く。`null` を代入したとき・`unbind()` で Store から切断したときは接続を外す (SHALL)。`unbind()` したときは `scrollController` も `null` に戻る。再び `bind()` しても自動ではつなぎ直さない (SHALL)。1 つのハンドルが命令を届ける先は、最後に接続した Host だけとする (SHALL)。別の Host に置き換えられたときは、debug ビルドで警告ログを出す。ハンドルは Host を強参照しない (SHALL)。ハンドルが Host (とそれが持つ Activity) の解放を妨げない。

#### Scenario: 最後に接続した Host だけが命令を受ける
- **GIVEN** 同じ `KsScrollController` を Host A、続けて Host B に接続した
- **WHEN** `scrollToEnd()` を呼ぶ
- **THEN** Host B だけがスクロールし、Host A の位置は変わらない

#### Scenario: 接続を外したハンドルの命令は何も起こさない
- **GIVEN** Host に接続した後、Host の `scrollController` に `null` を代入した
- **WHEN** `scrollToEnd()` を呼ぶ
- **THEN** Host の位置は変わらない

#### Scenario: unbind で接続が外れ、bind し直した後は再代入で命令できる
- **GIVEN** Host に接続した `KsScrollController`
- **WHEN** Host を `unbind()` し、同じ Store を `bind()` し直す
- **THEN** Host の `scrollController` は `null` になっていて命令は何も起こさない。`scrollController` に再び代入すると命令が届く

#### Scenario: ハンドルは Host の解放を妨げない
- **GIVEN** Host に接続した `KsScrollController` を、Host より長く保持している
- **WHEN** Host への参照をすべて手放し、ガベージコレクションを促す
- **THEN** Host は回収され、以後の命令は何も起こさない

### Requirement: 命令はデータの反映の後に実行される

Host は命令を受けた処理の中では実行せず、保留中のデータの反映がすべて終わってから実行する (SHALL)。保留中の反映には、Store の更新の受け取りと、一覧の差分の反映の完了の両方を含む。命令は呼ばれた順に取りこぼさず処理し、次の命令を実行するときに先行のスクロールのアニメーションを止める (SHALL)。最終位置は最後の命令のものになる。実行の直前に最新の表示で対象を解決し、対象が無ければその命令は何もしない (SHALL)。後続の命令は処理を続ける。

#### Scenario: Cell の追加と同じ処理で出した末尾への命令が新しい末尾へ届く
- **GIVEN** 表示中の Host と、その Store
- **WHEN** 同じ処理の中で、Store の最後の Section に Cell を追加し、続けて `scrollToEnd()` を呼ぶ
- **THEN** 追加した Cell を含む内容の末尾までスクロールする

#### Scenario: 反映を続けて提出しても命令は最後の反映の後に実行される
- **GIVEN** 表示中の Host と、その Store
- **WHEN** 同じ処理の中で、Store の最後の Section に Cell を 1 つずつ 3 回追加し、続けて `scrollToEnd()` を呼ぶ
- **THEN** 3 つ目に追加した Cell を含む内容の末尾までスクロールし、命令は止まらずに処理される

#### Scenario: 続けて出した命令は最後の命令の位置で止まる
- **GIVEN** 表示中の Host
- **WHEN** `scrollTo(id)` で Cell A を、続けて同じ処理で `scrollToSection(id)` で Section B を指す
- **THEN** 最終位置は Section B の命令の位置になる

#### Scenario: 実行前に対象が削除された命令は飛ばされ、次の命令は実行される
- **GIVEN** 表示中の Host
- **WHEN** Cell A を指す命令を出し、同じ処理で Cell A を Store から削除し、続けて `scrollToEnd()` を呼ぶ
- **THEN** Cell A の命令は何もせず、末尾への命令は実行される

#### Scenario: 画面に取り付ける前に出した命令は初回の表示の後に実行される
- **GIVEN** window に取り付ける前の Host に Store を bind し、`KsScrollController` を接続した
- **WHEN** `scrollToEnd()` を呼んでから Host を画面に取り付ける
- **THEN** 初回の表示の後に内容の末尾までスクロールする

### Requirement: Cell への命令の位置

`scrollTo(id, position, animated)` は、指定した Cell の行を次の位置に合わせる (SHALL)。

- `Start`: 行の上端を表示範囲の上端へ
- `Center`: 行の中央を表示範囲の中央へ
- `End`: 行の下端を表示範囲の下端へ

表示範囲は、内容の余白を除いた実際に見えている領域とする。到達できない位置はスクロール可能範囲の端で止め、表示範囲より高い行は `Start` に合わせる (SHALL)。非表示の Cell、存在しない ID への命令は何もしない (SHALL)。debug ビルドでは存在しない ID を警告ログで知らせる。Store 方式と View Host の ID は Cell の ID (String) とする。

#### Scenario: 表示範囲の外の Cell を中央に合わせる
- **GIVEN** 表示範囲の外 (下方) にある Cell
- **WHEN** `scrollTo(id, KsScrollPosition.Center)` で指す
- **THEN** その Cell の行の中央が表示範囲の中央に来る

#### Scenario: 内容の末尾付近の Cell を上端に合わせようとするとスクロール可能範囲の端で止まる
- **GIVEN** 内容の最後の Cell
- **WHEN** `scrollTo(id, KsScrollPosition.Start)` で指す
- **THEN** スクロール位置は内容の末尾側の端で止まり、行き過ぎない

#### Scenario: 非表示の Cell と存在しない ID への命令は位置を変えない
- **GIVEN** 表示中の Host と、非表示 (`isVisible == false`) の Cell
- **WHEN** 非表示の Cell の ID と、どこにも存在しない ID をそれぞれ指す
- **THEN** 位置は変わらず、例外は起きない

### Requirement: Section への命令は見出しごと見せる

`scrollToSection(id, position, animated)` は、指定した Section の範囲を Cell と同じ規則で合わせる (SHALL)。範囲は、その Section で表示されている見出し・Cell・Footer の行をつないだものとする。上端は最初の行の上端で、見出し、なければ最初の表示 Cell、それもなければ Footer の順に採る。下端は最後の行の下端で、Footer、なければ最後の表示 Cell、それもなければ見出しの順に採る。非表示の Section、表示されている行が 1 つも無い Section、存在しない ID への命令は何もしない (SHALL)。

#### Scenario: Start で見出しが表示範囲の上端に来る
- **GIVEN** 見出しを持ち、表示範囲の外にある Section
- **WHEN** `scrollToSection(id)` で指す
- **THEN** その Section の見出しの上端が表示範囲の上端に来る

#### Scenario: 見出しの無い Section は最初の Cell が上端に来る
- **GIVEN** 見出しを持たない Section
- **WHEN** `scrollToSection(id)` で指す
- **THEN** その Section の最初の表示 Cell の上端が表示範囲の上端に来る

#### Scenario: 非表示の Section と表示される行の無い Section への命令は位置を変えない
- **GIVEN** 非表示の Section と、Cell を持たず見出しも Footer も無い Section
- **WHEN** それぞれを `scrollToSection(id)` で指す
- **THEN** 位置は変わらない

### Requirement: 先頭・末尾への命令は内容の両端へ送る

`scrollToStart(animated)` は Root Header を含む内容の最上端へ送る (SHALL)。`scrollToEnd(animated)` は Root Footer を含む内容の最下端へ送る (SHALL)。

#### Scenario: 末尾への命令で Root Footer の下端まで見える
- **GIVEN** Root Footer を持ち、先頭を表示している Host
- **WHEN** `scrollToEnd()` を呼ぶ
- **THEN** 内容の最下端 (Root Footer の下端) が表示範囲の下端に来る

#### Scenario: 先頭への命令で Root Header の上端まで戻る
- **GIVEN** Root Header を持ち、末尾を表示している Host
- **WHEN** `scrollToStart()` を呼ぶ
- **THEN** 内容の最上端 (Root Header の上端) が表示範囲の上端に来る

### Requirement: アニメーションは一方向で着地する

`animated` が `true` の命令は、対象へ向かって一方向に進み、行き過ぎてから戻る動きをしない (SHALL)。これは行の高さが混在していても同じとする。`animated` が `false` の命令は、アニメーションせずに最終位置へ移る (SHALL)。

#### Scenario: 中央・末尾合わせのアニメーションが行き過ぎて戻らない
- **GIVEN** 高さの異なる行が混在し、表示範囲の外 (下方) にある Cell
- **WHEN** `scrollTo(id, KsScrollPosition.Center, animated = true)` と `End` でそれぞれ指す
- **THEN** スクロール位置は下方向にだけ進み、最終位置で止まる

#### Scenario: アニメーションなしの命令は即座に最終位置へ移る
- **GIVEN** 表示範囲の外にある Section
- **WHEN** `scrollToSection(id, KsScrollPosition.Start, animated = false)` で指す
- **THEN** 次の配置の時点で、見出しの上端が表示範囲の上端にある

### Requirement: Compose の引数と宣言 UI での指し方

Compose の `KsSettingsView(...)` は引数 `scrollController: KsScrollController?` (既定 `null`) を受け、内部の Host へ接続する (SHALL)。これは Store 方式・DSL 方式の両方で有効とする。Store 方式ではハンドルの `id` は Store の Cell / Section の ID とする。DSL 方式では、利用者が書いた明示 ID (`cellID(...)` / `sectionID(...)`) または `forEach` の key を `id` として受け、宣言ツリーの最終 ID へ引き直して実行する (SHALL)。同じ値が明示 ID と key の両方として存在するときは、明示 ID の要素を採る (SHALL)。どちらにも無い値は何もしない。DSL 方式で状態の変更と同じ処理から出した命令は、変更後の宣言ツリーで解決する (SHALL)。

#### Scenario: 明示 ID で Cell を指せる
- **GIVEN** DSL 方式の `KsSettingsView` に `cellID("wifi")` を付けた Cell があり、`scrollController = controller` を渡している
- **WHEN** `controller.scrollTo("wifi", KsScrollPosition.Center)` を呼ぶ
- **THEN** その Cell の行の中央が表示範囲の中央に来る

#### Scenario: forEach の key で Section を指せる
- **GIVEN** DSL 方式の `forEach` で key `42` の項目から作った Section
- **WHEN** `controller.scrollToSection(42)` を呼ぶ
- **THEN** その Section の見出しの上端が表示範囲の上端に来る

#### Scenario: 同じ値が明示 ID と key の両方にあるときは明示 ID の要素を採る
- **GIVEN** DSL 方式で、明示 ID `"a"` を付けた Cell X と、`forEach` の key `"a"` から作った Cell Y
- **WHEN** `controller.scrollTo("a")` を呼ぶ
- **THEN** Cell X が表示範囲の上端に来る

#### Scenario: 状態の変更と同じ処理から出した命令は新しいツリーで解決される
- **GIVEN** DSL 方式の `KsSettingsView` と、項目のリストを持つ状態
- **WHEN** 同じクリックの処理の中で、リストに key `"new"` の項目を追加し、続けて `controller.scrollTo("new")` を呼ぶ
- **THEN** 追加した項目の Cell が表示範囲の上端に来る

#### Scenario: Store 方式では Store の Cell ID で指せる
- **GIVEN** Store 方式の `KsSettingsView` に `scrollController = controller` を渡している
- **WHEN** Store の Cell の ID で `controller.scrollTo(id)` を呼ぶ
- **THEN** その Cell が表示範囲の上端に来る

### Requirement: 位置を控える・戻す窓口

View Host は `captureScrollAnchor()` と `restoreScrollAnchor(anchor)` を公開する (SHALL)。`captureScrollAnchor()` は、表示範囲の上端にかかる最初の要素とそこからのずれを、中身を公開しない値 `KsScrollAnchor` (`Parcelable`) として返す (SHALL)。要素は Cell 行・Section の見出しと Footer・Root Header と Root Footer のいずれか。控えられる内容が無いときは `null` を返す。`restoreScrollAnchor(anchor)` は、命令と同じくデータの反映の後に、その要素が同じずれで表示範囲の上端にかかる位置へ戻す (SHALL)。戻すより後に出した命令は、戻した後に実行される。控えた要素が見つからなければ何もしない (SHALL)。

#### Scenario: 控えた位置を新しい Host で戻す
- **GIVEN** 同じ Store を表示する Host A で途中までスクロールし、`captureScrollAnchor()` で控えた
- **WHEN** 同じ Store に新しい Host B を bind し、`restoreScrollAnchor(anchor)` してから画面に取り付ける
- **THEN** Host B では、Host A で上端にかかっていた要素が同じずれで上端にかかる

#### Scenario: 控えた後に上に項目が増えても同じ要素へ戻る
- **GIVEN** 控えを取った後に、控えた要素より上に Cell が挿入された Store
- **WHEN** 新しい Host で `restoreScrollAnchor(anchor)` する
- **THEN** 控えた要素が同じずれで上端にかかる

#### Scenario: 控えた要素が消えていたら何もしない
- **GIVEN** 控えた要素が Store から削除された後
- **WHEN** 新しい Host で `restoreScrollAnchor(anchor)` する
- **THEN** 位置は内容の先頭のまま変わらない

#### Scenario: 戻した後に出した命令が最終位置を決める
- **GIVEN** 新しい Host に `restoreScrollAnchor(anchor)` した
- **WHEN** 続けて同じ処理で `scrollToEnd()` を呼ぶ
- **THEN** 最終位置は内容の末尾になる

### Requirement: Activity の作り直しで位置を戻す

View Host は、View の保存状態にスクロール位置の控え (`KsScrollAnchor`) を含める (SHALL)。Activity の作り直しの後、attach と root の反映がそろった時点で、その位置へ戻す (SHALL)。これは Compose の `KsSettingsView(...)` の中の View Host でも同じとする (SHALL)。既定 id の View Host が同じ階層に複数あるときは、既存のカレンダーの状態と同じく、保存も復元もしない (SHALL)。

#### Scenario: Activity を作り直しても同じ要素が上端にかかる
- **GIVEN** Activity に置いた View Host で途中までスクロールしている
- **WHEN** Activity を作り直す
- **THEN** 作り直した後の View Host で、作り直す前に上端にかかっていた要素が同じずれで上端にかかる

#### Scenario: Compose の KsSettingsView でも Activity を作り直すと同じ要素が上端にかかる
- **GIVEN** Activity に置いた Compose の `KsSettingsView(...)` で途中までスクロールしている
- **WHEN** Activity を作り直す
- **THEN** 作り直した後も、作り直す前に上端にかかっていた要素が同じずれで上端にかかる

#### Scenario: 既定 id の View Host が複数あるときは復元しない
- **GIVEN** 既定 id の View Host を 2 つ置いた Activity で、どちらも途中までスクロールしている
- **WHEN** Activity を作り直す
- **THEN** どちらの View Host も内容の先頭から表示される
