# Exploration: android-compose-scroll-restore-with-multiple-hosts

## 課題 / 動機

起票元: ../KsAppKMP/kasane/changes/kssettingsview-scroll-restore-bump/evidence/README.md

Android の Composable の `KsSettingsView` を複数の画面で使うアプリで、別の画面へ進んで戻ったとき・タブを切り替えて戻ったときに、スクロール位置が先頭に戻る。add-scroll-control で入れた「Host の作り直しをまたぐ位置の保持」が、画面が 1 つだけのときにしか効かない。

KsAppKMP のリファレンスアプリを 0.1.0-beta.9 に上げて、API 36 のエミュレータで確かめた (2026-10-07)。このアプリは、カタログ・タブ 2 つ・言語・テーマなど 7 画面を Composable の `KsSettingsView` で組み、Navigation Compose で行き来する。

| 場面 | 戻った後 |
|---|---|
| 起動した直後に、KsSettingsView を使わない画面へ進んで戻る | 保たれる |
| KsSettingsView で組んだ画面へ進んで戻る | 先頭に戻る |
| タブを、KsSettingsView で組んだ別のタブに切り替えて戻る | 先頭に戻る |
| 上のどちらかを一度した後に、KsSettingsView を使わない画面へ進んで戻る | 先頭に戻る |

ソースの読みでの見立て:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` の `hasAmbiguousLibraryDefaultId` は、同じ View の階層にライブラリ既定の id (`R.id.ks_settings_view`) の `KsSettingsView` が 2 つ以上あると、`onSaveInstanceState` で位置を載せず、`onRestoreInstanceState` でも戻さない。window から外れるときに居合わせていた View は、外れた後の保存でも載せない (`wasAmbiguousWhileAttached`)
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` の `bindAndroidView` は View に id を付けず、利用者が付ける引数も無い。Composable で置いた View はすべて既定の id になる
- 画面遷移の途中 (前の画面と次の画面が同時に階層にある間) と、タブの画面が階層に残る構成では、既定の id の View が 2 つ居合わせる
- 4 つめの場面は、開いたことのあるタブの View が階層に残って居合わせが続くためと見ているが、View の数は端末で数えていない

スキルの文書 (`skills/ja/kssettingsview-android/references/updates.md` の「Activity の作り直しをまたいでスクロール位置を保つ」) は、Navigation Compose の `NavHost` で進んで戻ったときも位置へ戻ると書き、保存しない条件を「1 画面に Composable を 2 つ置いた場合」としている。行き先の画面も `KsSettingsView` のときに保存されないことは、この文からは読み取れない。

### 探索で確かめたこと (2026-10-07)

- Compose の `AndroidView` は、置いた View ごとに別の入れ物へ状態を保存する。`androidx.compose.ui:ui-android:1.11.4` (compose-bom 2025.11.01) の `classes.jar` を `javap` で読んで確かめた
  - `AndroidViewHolder` は自身に `setSaveFromParentEnabled(false)` を呼び、Activity からの保存の走査を自分より下へ通さない
  - `ViewFactoryHolder` は保存のたびに新しい `SparseArray` を作って `typedView.saveHierarchyState` を呼び、呼び出し位置から決まるキー (`compositeKeyHash`) で `SaveableStateRegistry` に登録する。復元は同じキーで取り出して `restoreHierarchyState` を呼ぶ
  - したがって、別々の `AndroidView` に置いた既定の id の View どうしは、同じ入れ物へ書き込まない。android/ADR-0021 の縮退条件の理由 (保存先が衝突して状態が混ざる) は、Composable で置いた View には当たらない
- Fragment も同じ形で入れ物を分ける。`androidx.fragment:fragment:1.8.9` の `FragmentStateManager` は Fragment の View に `setSaveFromParentEnabled(false)` を呼び、Fragment ごとの `SparseArray` へ `saveHierarchyState` で保存する
- 縮退条件は実測ではなく、防御側に倒して入れたもの。`kasane/changes/archive/2026-08-28-relax-android-host-prerequisites/evidence/spike-findings.md:27` は「多重配置ケースは未検証のため防御側に倒す」と書く。実測したのは View 直置きと `AndroidView` のそれぞれ 1 つずつだけ。ADR-0021 の本文は「未検証」の但し書きを落として断定形になっている
- `NavHost` で進んで戻る操作そのものは、add-scroll-control でテストでも実機でも確かめていない。残っているテスト (`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollRecreationTest.kt:205, 217`) は `SaveableStateProvider` の固定の key の下で Activity を作り直すだけ。サンプルのメニュー画面は `LazyColumn` (`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MenuScreen.kt:49`) で、行き先も戻り先も `KsSettingsView` になる経路が無い
- 今の挙動を期待値として固定しているテストは 3 本。Composable が対象なのは `ComposeScrollRecreationTest.kt:245` (Composable を 2 つ置くと復元しない) の 1 本で、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollRecreationTest.kt:118` と `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt:389` は View 直置き
- カレンダーの選択面の状態も、コードの上では同じ条件で落ちる。位置と同じ判定 (`KsSettingsView.kt:1348-1349`) の後ろで保存状態に載せているため。端末では実測していない
- 4 つめの場面の見立て (未実測): 居合わせの印 (`wasAmbiguousWhileAttached`) を数え直すのはレイアウトのときだけ (`KsSettingsView.kt:594`)。戻りの遷移で前の画面と居合わせて印が立った後、前の画面が消えてもレイアウトが走らなければ印は立ったまま残り、次に window から外れるとき (`:557`) にそのまま使われる

## 検討した選択肢 (却下案と理由を含む)

起票元で挙がった 3 つの方向 (B・C・D) に、探索で A を足して比べた。android/ADR-0021 に従うのは C・D、改めるのは A・B。

| 案 | 選んだ結果 | 判定 |
|---|---|---|
| A: 保存の入れ物が同じときだけ「複数ある」と数える | Composable の複数画面・タブ、利用者が自分で `AndroidView` に包んだ View、Fragment ごとに置いた View が、利用者の作業なしで直る。同じ Activity に View を 2 つ直置きした構成は今のまま保存しない。公開 API は変えない | 採用 |
| B: Composable で置いた View だけ判定から外す | ライブラリの Composable で置いた画面だけ直る。公開 API は変えない | 却下 |
| C: Composable に、利用者が id か保存のキーを渡す引数を足す | キーを付けた画面だけ直る。公開 API が増える | 却下 |
| D: 文書だけを直す | 挙動は変わらない | 却下 |

- A の弱み: 入れ物の境目を「親からの保存を受けない」という Android 標準の設定で見分ける。Compose と Fragment がこの設定を使うことは確かめたが、独自の保存をするホストまでは確かめていない
- B の却下理由: 理由が「Compose だから」になって混ざる本当の条件を表さず、Fragment ごとに置いた View などに同じ落ち方が残る
- C の却下理由: 入れ物が既に分かれているのに利用者へ全画面の作業を求める。ADR-0021 が採った「ホスト側の追加作業なしで成立」とも逆になる
- D の却下理由: 設定画面が 2 つ以上あるアプリで、位置の保持が効かないまま残る

## 決定事項

- 論点 1 (2026-10-07): A を採用する。既定の id の View Host が複数あるかは、window の階層全体ではなく、保存の入れ物を共有する範囲で数える (android/ADR-0023)
  - 範囲の見分け方: 自身から親をたどり、最初に当たる「親からの保存を受けない」View (`isSaveFromParentEnabled` が `false`) 以下を範囲にする。当たらなければ階層全体 (`rootView`) で、今と同じ数え方になる
  - Compose では、window から外れた後に保存を求められたときも View は `AndroidView` の入れ物 (`AndroidViewHolder`) の子のままなので、外れた後でも範囲を見分けられる見込み (実装で確かめる)
  - カレンダーの選択面の状態とスクロール位置の両方に同じ判定を使う (今と同じ)
  - 期待値が変わるテスト: `ComposeScrollRecreationTest.kt:245` (Composable を 2 つ置くと復元しない → それぞれの位置へ戻る)。View 直置きの 2 本 (`ScrollRecreationTest.kt:118`・`DateCalendarRecreationTest.kt:389`) は据え置き
- 論点 2 (2026-10-07): テストに加えて、サンプルに行き来の経路を足して端末で確かめる。前回の抜け (実際の `NavHost` で進んで戻る操作を、テストでも端末でも通していなかった) をふさぎ、再現の足場をこのリポジトリに残すため
  - テスト (Robolectric) は 4 つ: Composable を 2 つ置いた形の期待値の反転 / 2 つの画面が居合わせる形 (`SaveableStateHolder` の key を切り替えて進む・戻る) の追加 / 入れ物を分けた親の下に View を置いた形 (Fragment 相当) の追加 / 同じ入れ物に 2 つ置いた形は保存しないまま (既存の 2 本の据え置き)
  - サンプル: `samples/android` に、`KsSettingsView` の画面から別の `KsSettingsView` の画面へ進む経路を 1 本足す。足す場所は実装で決める (既存のデモ画面どうしをつなぐ形を優先し、新しい画面は作らない)
  - 端末 (API 36 のエミュレータ): 課題の表の 1・2・4 つめの場面で位置が保たれること。3 つめ (タブ) はサンプルに構成が無いため、リリース後に起票元のアプリで確かめる
  - MAUI Android: ページごとに Fragment を使うため A の影響が届く。`SettingsView` のページから `SettingsView` のページへ進んで戻る操作で、位置が保たれ、Bridge の戻し (maui/ADR-0030) と食い違わないことを確かめる。位置の戻しは Bridge が別の経路で持ち、ページが 1 つのときは今も両方が動いているため、変わるのはページが 2 つ重なったときも 1 つのときと同じになることだけ、と見ている (未実測)
  - 却下した確かめ方: テストだけ (実際の `NavHost` が今回も端末で通らない) / 起票元のアプリで確かめる (4 場面すべてを確かめられるが、未公開版を配る手順と別リポジトリでの作業が要り、このリポジトリに足場が残らない)
- 蒸留時に反映: `kasane/decisions/android/0021-calendar-dialog-restore-via-view-instance-state.md` — frontmatter に `amended-by: 0023` を足し、index の行に「一部改訂: 0023」を書く
- 蒸留時に反映: `kasane/concepts/android/api/android-native-host.md` — 「同じ階層に複数あるとき」と書く箇所 (カレンダー選択面の復元の条件、Activity の作り直しと保存状態、保証の箇条書き) を「同じ保存の入れ物に複数あるとき」に直し、`NavHost` で行き先も `KsSettingsView` の場合に位置が戻ることを書く
- 利用者向けの文書 (`skills/` の「1 画面に Composable を 2 つ置いた場合」の記述) は、この change では直さない。concepts の更新後に docs-refresh で追従する

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- 作成済み: android/ADR-0023 (proposed、android/ADR-0021 を一部改訂) — 既定 ID の View Host が複数あるかは、保存の入れ物を共有する範囲で数える

## 未決の論点

なし

簡易起票の時点の疑問のうち、保存の入れ物の確認とカレンダーの選択面は「探索で確かめたこと」に、4 つめの場面の実測は論点 2 の端末での確かめに移した。

## UI 素材 (ui/references/ の一覧と注釈)

なし (ライブラリの見た目は変えない。サンプルは画面遷移の経路を足すだけで、モックは作らない)

## 変更級の推奨: S — オーナーが確定 (2026-10-07) (不具合の修正で、触る能力は Android の View Host の保存・復元の 1 つ。公開 API を変えず、変更は「複数ある」を数える範囲を決める 1 箇所に閉じて後から戻せる。android/ADR-0021 の一部改訂は android/ADR-0023 に記録した。0.1.0-beta.9 までと保存の成否が変わる構成があるため、確かめる範囲を決定事項の論点 2 に明示した)
