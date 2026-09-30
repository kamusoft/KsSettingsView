# phase-8-scroll-control

スクロール制御 (ScrollTo 系) を Native 起点で設計して MAUI から利用できるようにする。

## 論点

(なし — すべて決定事項へ移した)

## 決定事項

### スクロール制御は KsCollectionView の KsScrollController を踏襲する

2026-09-29 オーナー指示。`../KsCollectionView/kasane/decisions/core/0007-scroll-controller.md` と `../KsCollectionView/kasane/concepts/core/core-model/collection-interaction.md` の形に合わせる。AiForms 互換でも独自設計でもない。

- 命令ハンドル `KsScrollController` を両 OS 同名の plain オブジェクトで提供する。UI のライフサイクルに依存せず、所有位置は自由 (View 所有・VM 所有・イベント方式)
- 命令語彙: ID で指す `scrollTo(id, position, animated)` と、端への `scrollToStart(animated)` / `scrollToEnd(animated)`。位置は `KsScrollPosition` の start / center / end
- 未接続・接続解除後・存在しない ID への命令は no-op (例外なし、debug は警告ログ)。複数の表示に接続したら最後の接続だけが有効。呼び出しはメインスレッド限定
- 命令は保留中のデータ反映の後に実行する (「追加 → 直後に末尾へ」が素朴に書ける)。呼んだ順に取りこぼさず処理し、後の命令が先のアニメーションを中断する
- スクロール先を状態 (プロパティ) で渡す形は採らない。AiForms の `ScrollToTop` / `ScrollToBottom` (true で発火しライブラリが false に戻す bool) もこれに当たるため継承しない
- KsCollectionView の条項のうち固定される見出し・`contentPadding` に関わるもの (見出しの裏に置かない・位置の基準) は該当物が無いので、提案化で KsSettingsView の表示範囲に読み替える

### 命令の対象に Section を含める

2026-09-29 (論点1)。Section へ送ると見出しごと表示範囲に入り、Cell への命令の意味は KsCollectionView と同じまま命令を足すだけにする。非表示の Section への命令は存在しない ID と同じく no-op。中央・末尾合わせでの Section の範囲の取り方は提案化で詰める。

### 命令ハンドルは表示 (Host) につなぎ、Store は経由しない

2026-09-29 (論点2)。UIKit Host / Android View Host が接続口を持ち、SwiftUI / Compose は KsCollectionView と同じく修飾子・引数で受けて Host へ渡す。Store の契約 (状態保持と変更意図の通知だけを担い、表示位置は担わない) は変えない。maui/ADR-0001 の「更新は Store 一本」は状態の更新の話で、命令は状態ではないため衝突しない (Store 外の経路はスタイル切替 maui/ADR-0023 に次ぐ 2 本目)。

- 同じ Store を複数の画面に出しても、動くのはつないだ 1 画面だけ (最後の接続だけ有効)
- 「データ反映後に実行」は Host 側の待ち合わせで実現する。仕組み (iOS の一覧反映の完了、Android の非同期反映の完了、宣言 UI の更新が Host に届くまでの待ち) は提案化の design で詰める。KsCollectionView は iOS が反映完了で消化、Android がコンポジション後に消化

### 命令の対象は画面を組み立てたときに自分が書いた識別子で指す

2026-09-29 (論点5)。宣言 UI (DSL 方式) では明示 ID (`cellID` / `sectionID`) か `ForEach` / `forEach` の key で指し、ラッパーが最終 ID へ引き直す。Store 方式と UIKit / View Host では Cell / Section の ID で指す。

- 解決は identity の契約に従う (明示 ID と key の併用は明示 ID が勝つ core/ADR-0036、型の違う同じ文字列は別物)。最終 ID を利用者に見せない
- 位置で追跡される要素 (識別子を書いていない要素) は指せない。スクロール先にしたい要素には明示 ID を付ける
- 同じハンドルの引数の意味が接続先の方式で読み替わる (宣言 UI は手がかり、Store 方式・Host は ID)

### MAUI は命令ハンドルを SettingsView のプロパティでつなぎ、型はインターフェースにする

2026-09-29 (論点3-1)。SettingsView にメソッドは生やさず、C# 版の命令ハンドルを BindableProperty でつなぐ (Native / KsCollectionView と同形)。公開する型は実装クラスではなくインターフェースにする。理由 (オーナー): ViewModel が UI に直接依存せず、テスト容易性も確保できる。型名は提案化で決める。

- 実体は SettingsView が作り、プロパティの既定の向きを View → ViewModel (OneWayToSource) にして渡す。ColorAnalyzer の `ICameraController` と同じ形 (論点3-1 の残り)
- ViewModel はインターフェースだけを知り、DI の登録も要らない。画面に結び付くまで ViewModel 側は null
- ハンドルは SettingsView ごとに 1 つ。画面が作り直されたら新しい実体が Binding で届く。実装クラスは公開しなくてよい

### Native のハンドルは実装クラスに加えて、それが準拠するインターフェースも公開する

2026-09-29 (論点6)。作り方・渡し方は KsCollectionView どおり (SwiftUI の `@State`、Compose の remember、ViewModel での直接生成)。ViewModel はインターフェース (Swift の protocol / Kotlin の interface) に依存でき、テストでは fake で命令を確かめられる。接続口 (修飾子・引数・Host のプロパティ) は実装クラスを受けるので、自作の実装が渡される組み合わせは起きない。型名は提案化で決める。

### MAUI の命令の対象は ItemsSource の項目と明示 ID で指す

2026-09-29 (論点3-2、オーナー判断)。`ItemsSource` / `ItemTemplate` から生成した Section / Cell はその元の項目 (データ) で、XAML で手で並べた Section / Cell は新設する明示 ID のプロパティで指す。ViewModel は UI の部品に触れずに済み、宣言 UI (論点5) の「key / 明示 ID で指す」と対になる。

- 項目での照合は生成した要素に限る。手で並べた要素の `BindingContext` は親から継承され一意にならないため
- 部品そのもの (Section / CellBase のインスタンス) では指さない。ViewModel が使うインターフェースに UI の部品の型を入れない
- 明示 ID のプロパティ名、および明示 ID と項目が両方当たるときの優先 (宣言 UI の「明示 ID が勝つ」core/ADR-0036 にそろえるのが自然) は提案化で詰める。既存の `AutomationId` は UI テスト用の意味なので流用しない

### MAUI の命令は Bridge 経由で届け、Host が無い間は何もせず、準備完了をコマンドで知らせる

2026-09-29 (論点3-3、オーナー判断)。Host が無い間 (初回表示前・Pop 後・Activity 再生成中) の命令は Native と同じく no-op にし、控えて後で実行はしない。Host ができて命令が効くようになったら、SettingsView がコマンドで知らせる (ColorAnalyzer の `ReadyControllerCommand` と同じ形)。ViewModel は「開いたらこの Section へ」をそこに書く。

- 経路は MAUI のハンドル → gateway → Bridge → Host につないだ Native のハンドル。Bridge が Native のハンドルを 1 つ持ち、Host を作るたびにつなぎ直す (maui/ADR-0005・0009 から導かれる)
- Handler から Host を直接触る道は、gateway の切り離し (maui/ADR-0009) を破るので採らない。Store 操作 1:1 の枠外の Bridge API は setStyle に次ぐ 2 本目
- 合図のプロパティ名と、Host が作り直されるたびに知らせるかは提案化で詰める

### Host の作り直しをまたいでスクロール位置を保ち、Bridge が控えて戻す

2026-09-29 (論点4、オーナー判断)。Pop したページと同じインスタンスの再 Push や Activity 再生成で Host が作り直されると、両 OS で先頭に戻る (settingsview-migration-defects の探索の実測。exploration.md の実測表)。これを直す。上にページを Push するだけの通常遷移は Host が作り直されず、同 change で解決済み。

- Bridge が Host を手放すときに表示位置 (先頭に見えている要素の ID と行内のずれ) を控え、次の Host で表示が整ったらそこへ戻す
- 持ち主は Host の世代をまたいで生きる Bridge (スタイル切替 maui/ADR-0023 と同じ)。当初メモの「facade 側で控える」は誤りで、先例の保持者は Bridge
- 位置を控える・戻す窓口を Native に足す。公開の範囲と、控えた要素が離れている間に消えていたときの戻り先は提案化で詰める
- 復元は準備完了の合図 (論点3-3) の中で出した命令より先に処理し、ViewModel の命令が最後に勝つようにする。合図は命令が効く時点 (Host の取り付け直後) に出し、復元の画面への反映完了は待たない (2026-09-29 提案化のセカンドオピニオン M1 を受けたオーナー判断で文言を明確化)
- 隣接課題も同じ change に含める: MAUI を使わない Android の View Host も、Activity 再生成で位置を戻す。Android の保存状態 (SavedState) に控える (カレンダー選択面 android/ADR-0021 の先例)。Compose 経路で効くかは提案化で確かめる

## TODO

- [x] 論点の解消
- [x] ADR 起票 (proposed、昇格は蒸留時): core/ADR-0037 (Host につなぐ命令ハンドル)、maui/ADR-0029 (SettingsView が作るインターフェース型のハンドル)、maui/ADR-0030 (Bridge が位置を控えて戻す)
- [x] KsCollectionView への申し送りメモ: KsSettingsView の Native はハンドルのインターフェースを足した (論点6)。KsCollectionView 側にも足すかは向こうのリポジトリで判断する (config の relations に KsCollectionView は無い) → 見送り (2026-09-30 オーナー判断。KsCollectionView は形の手本にした先で、こちらに合わせる義務は無い。必要になったら向こうのリポジトリで判断する)
- [x] ksn-propose で変更提案を起こす (kasane/changes/add-scroll-control、L 級)

## 実装結果 (2026-09-30 反映)

- [add-scroll-control](../../../../changes/archive/2026-09-30-add-scroll-control/proposal.md) として実装完了 (verify-003 VALID / review-005・second-opinion-code-005 APPROVED)。core/ADR-0037・maui/ADR-0029・maui/ADR-0030 は accepted へ昇格 (maui/ADR-0030 は Decision の「Host を手放すときに控える」を「手放す前の位置を控える」に改めた)
- 決定事項どおりに実装した。実機検証で見つけた不具合 4 件 (Navigation Compose 配下で Activity の作り直しの位置が保存し直しで消える、MAUI の Handler 切断が window から外れた後に届き位置を控えられない、iOS の末尾への命令がスクロール中の内容・表示範囲の変化で行き過ぎる、iOS の控えが window から外れた後に違う位置を返す) を同じ change で直した
- オーナー合意の乖離: Navigation Compose の画面を離れて戻ったときも位置を保つ (保存の経路が Activity の作り直しと同じため)
- オーナー指示で追加したスコープ 5 件を同じ change に含めた: MAUI の最低版を 10.0.71 に上げて Android のアクセシビリティ経由のクラッシュを解消 (KsDialogs へ知らせ済み)、iOS の再接続後に祖先の再レイアウトが追従しない問題、宣言の差分の最小移動 (両 OS、Section の移動の位置も修正)、Compose の文字列の Root Header / Footer、iOS Sample の Swift 6 警告
- 申し送り:
  - skills/ と README の追随 (MAUI の最低版 10.0.71、iOS / Android の Root Footer と通知の閉包の例) → ロードマップ外。docs-refresh はユーザーの依頼でだけ動かす規約のため、オーナーの依頼を待つ (kasane/changes/archive/2026-09-30-add-scroll-control/deviation.md の蒸留送りの行)
  - KsCollectionView への申し送りメモ → 見送り (上の TODO に理由)
