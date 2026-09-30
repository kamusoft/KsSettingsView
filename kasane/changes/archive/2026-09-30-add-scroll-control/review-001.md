# レビュー結果: add-scroll-control (001 回目)

**日付**: 2026-09-30
**判定**: CHANGES_REQUESTED

## サマリー

3 系統 (iOS / Android の Native Host と宣言 UI、Bridge、MAUI facade) とも、デルタスペック 7 本の Requirement / Scenario に沿って実装されている。deviation.md に記録された乖離の範囲を超える仕様逸脱は見つからなかった。足場 (proposal / design / specs) は書き換えられておらず、tasks.md の差分はチェックの更新だけである。

重点確認の `ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift` は末尾まで読んだ。settings-view-ios-ui の全 Scenario に対応するテストがあり、重複や壊れた記述も無い。ただし、同じ Requirement を検証する Android のテスト群と比べると、iOS 側には Section の中央・下端合わせ、表示範囲より高い対象の上端合わせ、Section 見出しの控えと戻し、画面から外れている間の命令のテストが無い。復元の経緯を考えると、失われたまま戻っていない可能性がある。iOS には Section の範囲計算 (見出し・最初と最後の Cell・Footer の和) や推定高さに起因するリスクがあるのに、spec の SHALL 文言の一部が無検証のままなので、優先度の高い Minor として本サイクルでの追加を求める。

## 照合した規約

- cross/comment-policy (always)。`scripts/comment-policy-lint.py` の結果は禁止 0 件。advisory 1 件はテストクラスの doc コメント内の ADR 参照で、公開 API ではない
- cross/test-execution (テスト実行・結果の報告)
- cross/diagnostic-message-language (本体コードへの警告ログ・例外文言の追加)。新規の文言はいずれも英語で、既存の書式 (`KsScrollController: ...` / `KsSettingsView: ...`、C# は句点で終える) にそろっている
- cross/sample-parity (`samples/` へのデモ画面追加)。3 platform の文言・構成が一致していることを確認した。Root Footer の描画差は deviation 記録済み
- cross/runtime-behavior-verification (アニメーション・タイミングの挙動)。evidence/ の連続フレームで一方向の着地を確認した。撮り直しが残っている分は既知の未了として除外した
- ios/swift6-language-mode-check (`ios/Sources/` の変更)。ホスト側で error 0 を確認済み
- maui/integration-host-verification (binding 層・facade 層の変更)。IntegrationHost の証跡があり、表への追随は蒸留送りとして記録済み
- lessons/code-review L-001、lessons/process L-001 / L-003 / L-009

## 確認した事実 (ビルド・テスト)

- Android: `./gradlew test --rerun-tasks` を再実行し、kssettingsview 1375 × 2、bridge 193 × 2 の計 3136 tests / 0 failures (test-results の XML を集計)
- MAUI: `dotnet test maui/KsSettingsView.Maui.Tests` を再実行し、624 tests / 0 failures
- iOS: Simulator を起動しない制約のため再実行していない。ホスト側の 1175 tests / 0 failures を事実として扱った
- evidence/ の生ログ 2 本に、ローカル絶対パスと端末識別子が含まれないことを確認した

## 指摘事項

### 🟡 Minor (優先度高) iOS の ScrollControlTests が、Android では検証済みの SHALL 文言を検証していない

**該当箇所**:
- `ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift:443-491` (Section)
- `ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift:378-441` (Cell の位置)
- `ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift:727-893` (控える・戻す)

**問題点**: settings-view-ios-ui の Scenario はすべてテストに対応している。しかし次の SHALL 文言は iOS で 1 件も検証されていない。対応する Android のテストは `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollControlTest.kt` にある。

1. Section への命令の center / end。Requirement「Section への命令は見出しごと見せる」の「Cell と同じ規則で合わせる」「下端は Footer、なければ最後の表示 Cell…」にあたる。Android は `Section の範囲を中央と下端に合わせる` (:443)。iOS の `sectionRangeFrame` は見出し・最初と最後の Cell・Footer のレイアウト属性の和で範囲を作る。下端側 (Footer の supplementary) を使う経路が、どのテストも通っていない
2. 表示範囲より高い対象を start に合わせる規則。Requirement「Cell への命令の位置」の「表示範囲より高い行は start に合わせる」で、Section にも同じ規則が及ぶ。Android は `表示範囲より高い Section は上端に合わせる` (:466)
3. Section の見出し・Footer を控えて戻す経路。Requirement「位置を控える・戻す窓口」の要素の列挙にあたる。iOS は Cell と Root Header だけを検証している。Android は `Section の見出しと Root Header を控えて戻せる` (:693)
4. 画面から外れている間に受けた命令を、取り付け直した後のレイアウトで実行する経路。design Decision 3 と `canExecuteScrollEntries` / `viewDidLayoutSubviews` にあたる。Android は `画面から外れている間に出した命令は取り付け直した後に実行される` (:338)

design の Risks では「iOS で画面外の Section 見出しの位置は推定高さのまま求まる」をリスクに挙げている。1 と 3 はそのリスクの中心である。作業中にテストの一部が失われて復元された経緯もあり、ここが元から無かったのか、戻っていないのかは成果物からは判別できない。

**推奨修正**: `ScrollControlTests.swift` に Android の上記 4 件と同じ観点のテストを追加する。既存の `Fixture` と `headerFrame` / `cellFrame` / `awaitScrollSettled` をそのまま使える。
- 1 は Section の見出しの上端と Footer の下端から範囲を求め、その中央・下端が表示範囲の中央・下端に来ることを確かめる
- 2 は Cell 数を増やした Section で、見出しの上端が表示範囲の上端に来ることを確かめる
- 3 は見出しが上端にかかる位置で控え、新しい Host で戻す
- 4 は `view.removeFromSuperview()` の後に命令を出し、処理件数が増えないことを負の検証で確かめる。そのうえで window に戻し、処理と位置を確かめる

追加後は iOS の全件実行の件数を報告に併記する。

### 🔵 Suggestion 既定 id の Host 数の判定を、レイアウトのたびに階層全体の走査で行っている

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:591-594`

**問題点**: `onLayout` のたびに `hasAmbiguousLibraryDefaultId()` が `rootView` 以下を走査する。Host の行の高さ変化 (EntryCell の編集、accessory の再計測など) も親へのレイアウト要求として届くため、画面の View 数に比例した走査が頻繁に走る。現状の画面規模では実害は小さいが、Compose 画面など View 階層が大きいアプリでは無駄が積み上がる。

**推奨修正**: `changed == true` のとき、または attach 後の最初のレイアウトのときだけ数える。あるいは `ViewTreeObserver.OnGlobalLayoutListener` で階層の変化時だけ数え直す。deviation に記録された「同じ階層の Host がそろった時点で数える」という意図は保てる。

### 🔵 Suggestion ハンドルの受け口の参照を、メインスレッド以外から同期なしで読んでいる

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsScrollController.kt:33,59`

**問題点**: `strictMode = false` のときは、メインスレッド以外からの命令を捨てずにメインスレッドへ回す (deviation 記録済み)。しかし最初の `receiver?.get()` は呼び出したスレッドで、`@Volatile` の無いフィールドを読む。書き込みは常にメインスレッドで行われるため、別スレッドからは古い値 (未接続に見える、または外した後の受け口) が見えうる。結果として命令が黙って捨てられることがある。

**推奨修正**: `receiver` に `@Volatile` を付ける。もしくは、メインスレッド以外では接続の有無を判定せずに post し、メインスレッド側で `receiver` を読む形にする。

### 🔵 Suggestion Bridge の位置追跡が描画のたびに一時リストを作る

**該当箇所**: `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeHostAnchorTracker.kt:63-66`

**問題点**: `onPreDraw` はスクロール中も毎フレーム `captureScrollAnchor()` を呼ぶ。その中の `pendingRestoreAnchor()` が、毎回 `(readyEntries + incomingEntries).filterIsInstance<...>()` で一時リストを 2 つ作っている。スクロール中の割り当てが増えるだけで、機能上の問題は無い。

**推奨修正**: `pendingRestoreAnchor()` を逆順の走査に書き換え、両リストが空なら即座に抜けるようにする。

## 確認した観点 (指摘なし)

- **接続の規則**: 後勝ち、同一のときだけ外す、Store 切断 / unbind でプロパティを nil / null に戻す、ハンドルは弱参照、差し替え時に未実行の命令を捨てて復元は残す (deviation 記録済み)。両 OS とも実装とテストが一致している
- **順序保証**:
  - iOS: 全 apply 経路が `applySnapshot` を通ることを grep で確認した (直接の `dataSource.apply` は残っていない)
  - Android: `submitList` の全経路が世代付きの入口を通り、commit の後に `isLayoutRequested` / `hasPendingAdapterUpdates` / 配置後の補正待ちで実行を止めている
  - DSL: 受け口が世代を控えて 1 回遅らせてから引き直す (iOS は `main.async`、Compose は `withFrameNanos`)
- **位置の計算**: 端での停止、表示範囲より高い対象の上端合わせ、Section の範囲の上端・下端の採り方、Root Header / Footer を含む両端。iOS の CADisplayLink による毎フレームの行き先の求め直し (deviation 記録済み) と、Android の配置時の解き直し (deviation 記録済み) が一方向を保つこと。iOS の連続フレーム証跡でも確認した
- **位置の控え**:
  - 中身を公開しない型 (Swift は internal メンバーの `Sendable` struct、Kotlin は internal コンストラクタの `Parcelable`) になっている
  - SavedState の Parcel 形式の拡張で、読み書きの順序が一致している
  - 既定 id の Host が複数あるときに保存・復元しない規則 (android/ADR-0021) を守っている
  - window から外れた後の控えを Bridge 側の追跡で補っている (deviation 記録済み)
- **MAUI**:
  - `ScrollController` は OneWayToSource で、coerce により自身の実体に固定されている
  - 実体は持ち主を弱参照で持つ
  - 明示 ID → ItemsSource の項目の順で解決する。binder が生成元の項目の並びを、生成・追加・削除・置換・移動・作り直しの全経路で `_generated` と同じ添字で保っている
  - 準備完了の合図は Host の世代ごとに 1 回で、生成に失敗した世代では出さない
  - iOS の `ApiDefinition.cs` の selector (`scrollToCellWithCellID:position:animated:` など) が Swift の自動命名と一致している
- **付随修正**: 位置の整数変換の切り出し、`KsWireValues.ScrollPosition`、partial 化、setStyle の doc 追随、IntegrationHost のボタンと Section、iOS の未実行の復元の控えを返す処理。いずれも同梱条件 (本 change の変更が直接必要にしたもの・数行〜小規模) に収まっており、変換と控えにはテストがある
- **Sample**: 3 platform で画面タイトル・操作の文言・Section の構成・デモデータが一致している。MAUI の ViewModel は準備完了まで命令を出さない (deviation 記録済み)
- **公開 doc コメント**: 新規の公開 API に内部用語 (ADR ID・change 等) は混入していない。Bridge の公開メソッドには ADR ID が残っているが、既存の書式を踏襲したもので本 change の新規導入ではない

## アクションプラン

1. (必須) iOS の `ScrollControlTests.swift` に、Section の center / end、表示範囲より高い対象の start 合わせ、Section の見出し (と Footer) の控えと戻し、画面から外れている間の命令の 4 観点のテストを追加する。iOS を全件実行し、件数を報告する
2. (任意) Android の `onLayout` での既定 id の走査を、変化時だけに絞る
3. (任意) `KsScrollController.receiver` の可視性を担保する
4. (任意) `pendingRestoreAnchor()` の割り当てを減らす
