# phase-8-scroll-control 議論履歴

## 2026-09-29: スクロール制御の API の形 (KsCollectionView の踏襲)

フェーズ議論の開始時に現状を調べた。KsSettingsView には両 OS ともスクロールを動かす・読む公開 API が無く (iOS は UICollectionView、Android は RecyclerView の上に載る)、Store にもスクロール関連の状態・通知は無い。MAUI facade は BindableProperty だけで、Handler への命令の委譲の前例も無い。原典 AiForms にあるのは `ScrollToTop` / `ScrollToBottom` の bool プロパティ (true で発火しライブラリが false に戻す) だけ。

オーナー指示: `../KsCollectionView` に既にスクロール制御が実装されているので、それを踏襲する。KsCollectionView の形 (core/ADR-0007) は、両 OS 同名の plain な命令ハンドル `KsScrollController`、ID で指す `scrollTo` と端への `scrollToStart` / `scrollToEnd`、位置 start / center / end、未接続 no-op、データ反映後に実行する順序保証。スクロール先を状態で渡す形は同 ADR が「リセット契約が利用者に漏れる」として却下しており、AiForms の bool プロパティもそれに当たるので継承しない。

踏襲で決まらない KsSettingsView 固有の差分を論点として立て直した。旧「API の形」は踏襲で大半が決まり、残りの「Section を対象に含めるか」を論点1 とした。旧「命令系 API の層配置」は論点2「接続先と順序保証」に改めた。論点3 (MAUI からの公開形) と論点4 (Host 世代をまたぐ位置の保持) はそのまま残した。論点5「DSL・MAUI での対象の指し方」を新設した — KsSettingsView の DSL は識別の手がかりと最終 ID が別物で、MAUI は Cell の ID が利用者に見えないため、KsCollectionView の「利用者の key がそのまま ID」が写らない。

## 2026-09-29: 命令の対象に Section を含めるか (論点1)

KsCollectionView の対象は項目と両端だけで、グループへの命令は無い (グループの見出しは固定され、項目の先頭合わせは見出しのすぐ下に置く)。KsSettingsView の見出しは固定されないので、Cell だけではグループの先頭 Cell を先頭合わせで送ると Section の見出しが表示範囲の上に隠れる。見出しは Android では行、iOS では行に数えない付属 view で、iOS は見出しの位置を別に求める処理が要る。

選択肢: A. Cell と両端だけ (KsCollectionView と完全一致、Section は後から足せる) / B. Section も対象にする (命令が 1 つ増えるが Cell の意味は同じ) / C. グループの先頭 Cell を指したときだけ見出しも見せる (語彙は増えないが Cell の先頭合わせの意味が変わる)。

採用: B (オーナー判断、推奨どおり)。理由: 設定画面の主な用途は「特定のグループへ案内する」で見出しが見えないと文脈を失う、Section は全層で安定した ID を持ち ID で指す語彙がそのまま延びる、Cell への命令の意味を KsCollectionView から変えずに足すだけで済む。非表示の Section は no-op。

## 2026-09-29: 命令ハンドルの接続先 (論点2)

KsCollectionView には Store が無くハンドルは View につなぐ。KsSettingsView は Store 方式で ViewModel が既に Store を持つため、Store に命令を乗せる道があった。

選択肢: A. 表示 (Host) につなぐ (KsCollectionView と同形、Store の契約は不変、動くのはつないだ 1 画面) / B. Store につなぐ (Store 方式の ViewModel は Store だけで命令できるが、Store が表示の命令を運ぶよう契約が広がり、同じ Store を購読する全画面が動く。DSL 方式では Store が内部所有なので結局ハンドルからの橋渡しが要る)。反映完了の待ち合わせは Android の反映が非同期のためどちらでも Host 側に要り、差はない。

採用: A (オーナー判断、推奨どおり)。理由: KsCollectionView と同形、concepts の Store 契約 (表示位置を担わない) を保つ、maui/ADR-0001 は状態の更新の原則で命令とは衝突しない。待ち合わせの仕組みは提案化の design で詰める。

論点の区切りを変更: 論点5 の MAUI 部分 (Cell の ID が利用者に見えない件) を論点3 に移し、論点5 は宣言 UI だけを扱う。C# 側の命令の引数の形と一緒に決めるほうが自然なため。

## 2026-09-29: 宣言 UI での対象の指し方 (論点5)

KsSettingsView の宣言 UI は、識別の手がかり (明示 ID・`ForEach` の key・位置 fallback) から最終 ID (SwiftUI は UUID、Compose は文字列) を決め、concepts は「手がかりと最終 ID の一致に頼らない」と定める。KsCollectionView の「利用者の key がそのまま ID」はそのまま写らない。

選択肢: A. 画面を組み立てたときに自分が書いた識別子で指す (宣言 UI は明示 ID か key、Store 方式・Host は ID。ラッパーが最終 ID へ引き直す) / B. 常に最終 ID で指し、宣言 UI には手がかりから最終 ID を引く手段を公開する (引数の意味は揃うが、最終 ID を利用者に開き書き方が 2 段になる) / C. スクロール先専用の名前を宣言に付ける (identity と切り離せるが 1 要素に名前が 2 つ付き、KsCollectionView と形が変わる)。

採用: A (オーナー判断、推奨どおり)。理由: KsCollectionView と同じ感覚で書け、identity の既存契約 (明示 ID 優先・型の区別) がそのまま効き、最終 ID を利用者に見せずに済む。位置で追跡される要素は指せないが、明示 ID を付ければ指せる。弱点は同じハンドルの引数の意味が接続先の方式で読み替わること。

## 2026-09-29: MAUI での命令の出し方 (論点3-1)

論点3 (MAUI からの公開形) を 3-1 命令の出し方 / 3-2 対象の指し方 / 3-3 Bridge までの経路に分けて順に扱う。現状の facade は BindableProperty だけでメソッドも Handler への命令委譲の前例も無い。MAUI 標準部品 (CollectionView.ScrollTo / ScrollView.ScrollToAsync) は部品のメソッドで命令する流儀で、ViewModel からは直接呼べない。

選択肢: A. C# 版の命令ハンドルを SettingsView のプロパティでつなぐ (Native / KsCollectionView と同形、ViewModel から Binding でそのまま命令できる) / B. SettingsView にメソッドを生やす (MAUI 標準部品と同じ感覚だが ViewModel からは中継が要る) / C. 両方 (入口が 2 つになり優先の決まりが要る)。

採用: A に加えて、公開する型を実装クラスではなくインターフェースにする (オーナー判断)。理由 (オーナー): ViewModel が UI に直接依存せず、テスト容易性も確保できる。

参考にした先例: ColorAnalyzer の `ICameraController` (`../ColorAnalyzer/ColorAnalyzer/Controls/CameraView/CameraView.cs`) はインターフェース型の BindableProperty を既定 OneWayToSource で持ち、View が作った実体を ViewModel へ渡す。ViewModel は `ICameraController?` だけを知り、テストは NSubstitute の fake を使う。インターフェースにしたことで「実体を誰が作ってどちら向きに渡すか」を 3-1 の残りとして立て、Native 側もインターフェースにそろえるかを論点6 として新設した。

## 2026-09-29: MAUI の命令ハンドルの実体を誰が作るか (論点3-1 の残り)

選択肢: A. SettingsView が作って ViewModel へ渡す (既定 OneWayToSource。ColorAnalyzer の `ICameraController` と同形。ViewModel はインターフェースだけを知り DI 登録も不要、画面に結び付くまで null、ハンドルは画面ごとに 1 つ、自作実装が渡される組み合わせが起きない) / B. ViewModel 側で作って SettingsView へ渡す (KsCollectionView の ViewModel 所有と同じ向き、同じ実体を持ち続けられ複数画面で使い回せるが、実体を作る場所で実装クラスを知る必要があり、自作実装が渡されたときの決まりが要る)。

採用: A (オーナー判断、推奨どおり)。理由: 「ViewModel が UI に依存しない」を DI なしで最も素直に満たし、KsSettingsView の利用者である ColorAnalyzer と書き方がそろう。SettingsView がつなぐのは常に自分で作った実体になる。

## 2026-09-29: Native のハンドルもインターフェースにするか (論点6)

KsCollectionView は実装クラス `KsScrollController` をそのまま公開し、同 ADR は「ViewModel がライブラリ型に依存する」を負の帰結に挙げている。未接続のハンドルは no-op なので ViewModel のテストで「命令したか」を確かめにくく、Swift は protocol が無いと fake を差し込めない。

選択肢: A. 実装クラスだけ (KsCollectionView と完全一致、Swift では命令の検証が難しい) / B. 実装クラスに加えて、それが準拠するインターフェースも公開する (書き方は KsCollectionView のまま、ViewModel はインターフェースに依存して fake で検証できる、接続口は実装クラスを受ける) / C. インターフェースだけを公開し実体は画面側が作って渡す (MAUI と向きまでそろうが、SwiftUI には OneWayToSource が無く受け取りのコールバックが要り、KsCollectionView と形が変わる)。

採用: B (オーナー判断、推奨どおり)。理由: KsCollectionView の書き方を崩さずに足すだけで、MAUI で決めた「ViewModel が UI の実装に依存せずテストできる」を Native にも通せる。KsCollectionView 側にも足すかは向こうのリポジトリの判断とし、申し送りのメモを TODO に残した。

## 2026-09-29: MAUI での対象の指し方 (論点3-2)

MAUI の Section / Cell は `Element` で、Bridge 採番の ID は利用者に見えない。`ItemsSource` / `ItemTemplate` で生成した要素の `BindingContext` は元の項目。3-1 で ViewModel は UI の部品を持たないと決めた。

選択肢: A. 項目 + 明示 ID で指す (ItemsSource 由来は項目、手で並べた要素は新設の明示 ID。論点5 の key / 明示 ID と同じ考え方) / B. 部品そのもので指す (code-behind には自然だが ViewModel からは部品を別経路で渡す必要があり 3-1 の狙いと噛み合わない) / C. A と B の両方 (インターフェースの引数に UI の部品の型が入る)。推奨は A。

採用: 項目だけで指す (オーナー判断。A から明示 ID を除いた形)。帰結: 手で並べた Section / Cell はスクロール先に指定できない (先頭・末尾への命令は使える)。手で並べた要素の `BindingContext` は親から継承され一意にならないので、照合は生成した要素に限る。手で並べた要素を指す要望が出たら明示 ID を後から足せる。

## 2026-09-29: MAUI での対象の指し方の改訂 (論点3-2)

直前の決定 (項目だけで指す) を、オーナーが「明示 ID でも指定できるようにする」と改めた。結果は当初の推奨 A (項目 + 明示 ID) と同じ形になり、XAML で手で並べた Section / Cell も新設する明示 ID のプロパティでスクロール先にできる。宣言 UI (論点5) の key / 明示 ID と対になる。部品そのもので指す案 (B・C) は引き続き採らない。

提案化で詰めること: 明示 ID のプロパティ名、明示 ID と項目が両方当たるとき (項目が文字列の ItemsSource など) の優先。宣言 UI の「明示 ID が勝つ」(core/ADR-0036) にそろえるのが自然。

## 2026-09-29: MAUI の Bridge までの経路と Host が無い間の命令 (論点3-3)

経路は既存 ADR から導かれるものとして確認した: facade は gateway 経由でのみ Native に触れ (maui/ADR-0009)、Host は Bridge が所有する (maui/ADR-0005) ので、MAUI のハンドル → gateway → Bridge → Host につないだ Native のハンドル。Bridge が Native のハンドルを持ち Host 生成のたびにつなぎ直す。Handler から Host を直接触る道は gateway の切り離しを破るので採らない。

MAUI で Host が無いのは、初回表示前・Pop 後・Activity 再生成中。3-1 で ViewModel は SettingsView 生成時点でハンドルを受け取るので、「ハンドルは届いたのに開いた直後の命令が効かない」落とし穴が Native より目立つ。

選択肢: A. 何もしない・合図なし (最も単純だが、命令のタイミングをページの Appearing などに頼り、そのとき Host があるかは未確認) / B. 控えておき次の Host で実行する (いつ命令しても効くが MAUI だけ契約が違い、古い命令が再表示で効いたり位置の復元とぶつかったりする) / C. 何もしない + 準備完了をコマンドで知らせる (契約は Native と同じ、ColorAnalyzer の `ReadyControllerCommand` と同形、合図のプロパティが 1 つ増える)。

採用: C (オーナー判断、推奨どおり)。理由: Native / KsCollectionView の契約を保ったまま、ViewModel が命令を出すタイミングを明示でき、古い命令が遅れて効かず位置の保持 (論点4) とも干渉しない。合図の名前と Host 再生成のたびに知らせるかは提案化で詰める。

## 2026-09-29: ADR の起票

接続先 (論点2) と MAUI の公開形 (論点3) が決まったので ADR 化を提案し、オーナー判断で 2 本に分けて proposed で起票した。core/ADR-0037 (スクロール制御は KsCollectionView と同形の命令ハンドルを Host につないで提供し、Store を経由しない) と maui/ADR-0029 (MAUI のスクロール命令はインターフェース型のハンドルを SettingsView が作って ViewModel へ渡して提供する)。Section を対象に含めること・各層での対象の指し方・Native のインターフェース・Host が無い間の扱いは、書き換えても ADR のタイトルが変わらない設計として agenda の決定事項に残した。昇格は蒸留時。

## 2026-09-29: Host の作り直しをまたぐスクロール位置 (論点4)

背景 (2026-09-15、settingsview-migration-defects の探索): Pop したページ自身の再 Push や Activity 再生成で Host が作り直されると、両 OS でスクロール位置が先頭に戻る。通常遷移 (上に Push) は Host が作り直されず、同 change で Android Native 側を直して解決済み。KsCollectionView に Host の作り直しをまたぐ先例は無い (同じ表示の中で並びが変わるときに先頭の項目を控えて戻す仕組みだけ)。当初メモは「facade 側で控えて再配信 (maui/ADR-0023 と同型)」としていたが、ADR-0023 の実際の保持者は Host の世代をまたいで生きる Bridge (gateway は作り直されず、再接続時の再送の契機が無い)。

選択肢: A. 保つ・Bridge が控えて戻す (同じページに戻ったときに元の位置。Native に位置を控える・戻す窓口が要る。先頭に見えている要素の ID で控えるので離れている間に項目が変わっても追える) / B. 保たない (作り直しは新しい画面とみなす。追加の仕組みなし) / C. 利用者に任せる (ハンドルに位置を読む命令を足し、利用者が控えて準備完了の合図で戻す。KsCollectionView に無い語彙が増える)。

採用: A (オーナー判断、推奨どおり)。理由: 利用者から見れば同じページに戻っただけで、先頭へ飛ぶのは不具合に見え、通常遷移での位置の保持と体験がそろう。先例 (Bridge が保持して Host 生成時に当て直す) と同形で作れる。復元を済ませてから準備完了の合図を出せば、ViewModel の命令が最後に勝つ。

隣接課題: MAUI を使わない Android の View Host も Activity 再生成で位置を戻さない。Android の標準作法 (保存状態で戻す) に合わせ、カレンダー選択面 (android/ADR-0021) と同じく SavedState に控える形で同じ change に含める (同じ領域の課題は同じ change で直す方針。オーナーには確認せず判断を報告)。

ADR: オーナー判断で maui/ADR-0030 (Host の作り直しをまたぐスクロール位置は、Bridge が控えて次の Host で戻す) を proposed で起票した。Android の View Host を保存状態で戻す件は Android の作法に合わせた設計として agenda に残した。これで論点はすべて決定事項へ移った。

## 2026-09-29: 準備完了の合図と復元の順序の明確化 (提案化のセカンドオピニオン M1)

提案 add-scroll-control の相方レビュー (kasane/changes/add-scroll-control/second-opinion-spec-001.md の M1) が、論点4 の決定文言「復元を済ませてから準備完了の合図を出す」と design の実行順 (復元を Host の待ち行列の先頭に積み、Host の取り付け直後に合図) の不一致を指摘した。目的 (合図の中の命令が最後に勝つ) は満たしていた。

選択肢: A. 合図は命令が効く時点に出し、復元は合図の中の命令より先に処理される、という意味に文言を合わせる (追加の仕組みなし) / B. 復元が画面に反映し終わってから合図を出す (Native から MAUI まで復元完了の通知経路を足し、合図は初回の表示と復元の後に遅れる)。

採用: A (オーナー判断、推奨どおり)。理由: ViewModel は位置を読めないため反映完了を待つ利得が無く、命令の順序は待ち行列で保証できる。agenda の決定文言と maui/ADR-0030 (proposed) をこの意味に書き直した。
