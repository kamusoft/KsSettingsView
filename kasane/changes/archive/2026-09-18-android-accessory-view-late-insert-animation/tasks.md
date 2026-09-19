# Tasks: android-accessory-view-late-insert-animation

実装順は Host 側 (2・3) → MAUI 側 (4) とする。MAUI 側は Host の保証を前提にするため、先に入れると Android で Root の header / footer が表示されなくなる (design.md Migration Plan)。

適用する handbook: `kasane/handbook/cross/runtime-behavior-verification.md` (修正前の再現と同一手順での解消確認)・`kasane/handbook/cross/test-execution.md`・`kasane/handbook/cross/comment-policy.md`・`kasane/handbook/maui/integration-host-verification.md`・`kasane/handbook/ios/swift6-language-mode-check.md`・`kasane/handbook/cross/local-development-setup.md`。

## 1. 修正前の再現 (実環境)

- [x] 1.1 Android 実機 (Pixel 6a) で `samples/maui` の `AccessoryViewsDemoPage` へ遷移し、Root の header・Section の View だけの Header / Footer が遅れて挿入される症状を再現する。遷移直後のフレームが分かる証跡 (画面録画から切り出した連続フレーム) を `evidence/` に残す (→ Requirement: 配置済みの view accessory は Native Host の最初の表示に含まれる)
- [x] 1.2 同じ手順で `CustomCellDemoPage` の Content が遅れて行へ入ること (行の高さの変化) を観測し、証跡を残す。観測できない場合はその旨を記録する (→ Requirement: CustomCell の Content は Native Host の最初の表示に含まれる)
- [x] 1.3 iOS 実機で 1.1・1.2 と同じページの遷移直後を観測し、現状 (遅延・高さのブレの有無) を証跡として残す。修正後の退行確認 (6.3) の比較元にする

## 2. Android の Host: Root の header / footer の保持

- [x] 2.1 取り付け順序の回帰テストに Root の header / footer の Scenario を追加する (attach 前の header / attach 前の footer / detach 中の更新 / 複数回渡したときの最後の値 / `bind` 直後にキューを流さず渡した値 / キューを流さない多数回の連続更新の最後の値 (回数は Store の通知の現在の容量 64 を超える件数にする) / 同じ Store に bind した 2 つの Host の両方が更新される / 一方の unbind は他方の受け取りを妨げない / メインスレッド以外から渡した値が attach 後に表示される / attach 中にメインスレッド以外から渡した値はメインスレッドで反映される / 別 Store へ bind し直した後は以前の Store の値が反映されない / unbind 後は反映されない)。先に失敗することを確認する (→ Requirement: 購読できていない間に渡された Root の header / footer の保持)
- [x] 2.2 attach 中の 1 回の更新で Root の header の行への変更通知が 1 回だけ発行されることを、Adapter の変更通知の回数で確認するテストを追加する (行数では二重適用を検出できない — Root 用の Adapter は行数が常に 0 か 1 のため)。同じ Store へ `bind` を 2 回呼んだ Host でも 1 回であることを併せて確認する (→ Scenario: attach 中の更新は一度だけ適用される)
- [x] 2.3 長命の Store に bind した Host を unbind せずに手放したとき、Host が回収可能であること、次に Root 対象を渡した時点で Store に残っていた登録が取り除かれることを確認するテストを追加する (→ Scenario: unbind されずに手放された Host は回収できる)
- [x] 2.4 Store に、Root 対象の更新を結び付いている Host へ同期に知らせる受け口 (モジュール内部・Host を弱参照) を設ける。Host は `bind` の中で登録し、`unbind` と別 Store への `bind` で外す。受け口へは通知と同じ更新の値を渡し、Host は既存の適用処理と同じ反映を行う。登録は Host ごとに 1 件で、複数の Host へはすべて知らせる。Root 対象の反映はこの受け口だけが行い、取り付け中の通知の購読は Root 対象を読み飛ばす。公開の `applyDiff` へ直接渡す使い方は従来どおり有効に保つ。受け口は値をスレッド安全に控え、表示への反映はメインスレッドで行う (メインスレッドからの呼び出しはその場で、それ以外はメインスレッドへ送る。取り付け時は未反映の値を先に反映する)。ある Host での反映の失敗は記録に残し、他の Host への知らせと通知の発行を続けるStore の通知への Root 対象の発行は現行のまま残す (design.md Decision 3、core/ADR-0033)
- [x] 2.5 既存の復元の回帰テスト (構造・Cell 内容・Section accessory・theme) が変わらず通ることを確認する (→ Requirement: window attach 時の Store 現在状態からの復元)
- [x] 2.6 Host と Store・Bridge の doc コメントのうち「Root の header / footer は呼び出し側が attach 後に再適用する」旨の記述を新しい保証に合わせる

## 3. iOS の Host: Root の header / footer の保持

- [x] 3.1 `HostViewLoadRestoreTests` の「Root accessory は復元対象外で所有者の再適用により表示される」を取り除き、新しい Scenario (view load 前の header / footer / 値を渡しても view load が起きない / 複数回渡したときの最後の値 / 値を渡した後の解除 `nil`) のテストに置き換える。先に失敗することを確認する (→ Requirement: view load 前に渡された Root の header / footer の保持)
- [x] 3.2 view load 前に届いた Root 対象の更新を捨てずに Host のプロパティへ控え、view load 時の構築に使う。控える際に view load を誘発しない (design.md Decision 4)
- [x] 3.3 既存の view load 時の復元テストが変わらず通ることを確認する (→ Requirement: view load 時の Store 現在状態からの復元)
- [x] 3.4 Swift 6 言語モードの確認を行う (`kasane/handbook/ios/swift6-language-mode-check.md`)
- [x] 3.5 Host と Bridge の doc コメントのうち Root の header / footer の再適用責務の記述を新しい保証に合わせる

## 4. MAUI facade: 実体化と配信の前倒し

- [x] 4.1 新しい保証を固定するテストを追加する: 初回接続で Section の View だけの Footer / HeaderView が Native Host の生成時点の配信データに載る・Root は取り付けの通知より前に適用される・再接続でも同じ・切断中に配置した View も載る・取り付けの通知では配信が発生しない (→ Requirement: 配置済みの view accessory は Native Host の最初の表示に含まれる)
- [x] 4.2 CustomCell の Content について同じテストを追加する: 初回接続で配信データに載る・再接続では 1 回のバッチで届く・取り付けの通知では配信が発生しない (→ Requirement: CustomCell の Content は Native Host の最初の表示に含まれる)
- [x] 4.3 現行の順序を固定しているテストを新しい保証へ改める (`CustomCellContentTests` の「取り付け前は配信されない」系 3 件、`HandlerTests` の「取り付け確定の後に Root を適用する」)。テスト足場の「取り付け」操作を、親子関係の確定だけを表すものへ役割を変え、これを呼ぶ既存テストの期待の前後関係を見直す (design.md Decision 5)
- [x] 4.4 実体化の口の差し込みを接続より前へ移し、初回接続では設定ツリー全体の配信データを組む前に全配置を実体化する。実体化は controller で行い、gateway の配信データの組み立て中には行わない (design.md Decision 1、maui/ADR-0027)
- [x] 4.5 再接続では、口の差し込み直後・Native Host の生成前に、全 Section の slot と全 CustomCell の Content (1 バッチ) を実体化して届ける (design.md Decision 1)
- [x] 4.6 Root の header / footer を Native Host の生成直後・platform view を返す前に適用する。取り付けの通知では親子関係の成立確定だけを行う (design.md Decision 2)
- [x] 4.7 既存の保証が変わらず通ることを確認する: Handler 切断・再接続をまたぐ保持と復元、切断中の変更の反映、Host 解放後に配置した View の再接続時の適用、多重配置の例外、退役順序、同一 View の包み直し前の先行破棄、埋め込み view の安定性 (既存の `AccessoryViewTests` / `CustomCellContentTests` / `HandlerTests`)
- [x] 4.8 Handler・controller・SettingsView の doc コメントのうち「Host 取り付け後にまとめて適用する」「取り付け前の適用は Android で失われる」旨の記述を新しい順序に合わせ、該当箇所に `maui/ADR-0027`・`core/ADR-0033` の参照を残す

## 5. 実行時の連続差し替えの確認 (design.md Open Questions)

- [x] 5.1 Android で、表示中の画面に対して複数の Section の view accessory を同じ操作で続けて差し替えたとき、すべてが表示へ反映されることを確認するテストを追加する。反映が欠ける場合は、その再現内容と 1 バッチ化の規模をオーナーへ報告して判断を仰ぐ

## 6. 実環境での解消確認

- [x] 6.1 統合ホスト検証 (`kasane/handbook/maui/integration-host-verification.md`) で C# から Native への疎通を確認する
- [x] 6.2 Android 実機 (Pixel 6a) で 1.1・1.2 と同一の手順を行い、遷移直後の最初のフレームから Root の header・Section の Header / Footer・CustomCell の Content が正しい位置・高さで表示されることを確認する。ページの再訪問でも同じであることを確認する。証跡を `evidence/` に残す
- [x] 6.3 iOS 実機で 6.2 と同じ項目を確認する: Root の header / footer・Section の HeaderView / FooterView (折り返す Label を持つ FooterView を含む)・CustomCell の Content が、初回の表示とページの再訪問の両方で、ページ内容が最初に描画されるフレームから存在すること。証跡を `evidence/` に残す。view accessory の後着、または 1.3 の修正前には無かった view accessory の高さの変動が出た場合は、本 change の完了を止めてオーナーへ報告する。CustomCell については「Content が後着していないこと」を本 change の合否とし、Content が最初から在るうえで行の高さだけが後から変わる現象 (1.3 で修正前から観測されていたもの) は `kasane/changes/ios-customcell-initial-height-grow-animation` の対象として観測内容をあちらへ追記する
- [x] 6.4 Android の Native Sample (Kotlin) と iOS の Native Sample で Root の header / footer の表示に退行が無いことを確認する
- [ ] 6.5 `../ColorAnalyzer` の `AnalysisSettingsPage` (FooterView 2 箇所) で症状の解消を確認する (ローカル参照でのビルドが可能な場合。不可ならリリース後の確認項目としてオーナーへ申し送る)

## 7. 全体の確認

- [x] 7.1 3 platform のテストを実行し、実行件数と結果を報告する (`kasane/handbook/cross/test-execution.md`)
- [x] 7.2 標準 lint (ローカル絶対パス・個体識別値・ソースコメント規約) を通す
