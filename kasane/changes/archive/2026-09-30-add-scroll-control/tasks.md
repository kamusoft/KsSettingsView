# Tasks: add-scroll-control

作業前に handbook を読む: cross/comment-policy (常時)・cross/test-execution・cross/diagnostic-message-language (警告ログ・例外文言は英語)・cross/sample-parity (5.)・cross/runtime-behavior-verification (6.)・ios/swift6-language-mode-check (1. の完了判定)・maui/integration-host-verification (3.・4.)。KsCollectionView の実装 (`../KsCollectionView/ios/Sources/KsCollectionView/KsScrollController.swift` ほか) を形の参照にする。

## 1. Native iOS (settings-view-ios-ui)

- [x] 1.1 `KsScrollController` / `KsScrollControlling` (既定引数は protocol extension) / `KsScrollPosition` と、内部の命令型・受け口の protocol を `KsSettingsViewUI` に追加する。受け口は弱参照、最後の接続が勝ち、置き換え時に debug 警告ログ (→ Requirement: スクロール命令ハンドルの公開 / Host への接続と最後の接続)
- [x] 1.2 `KsSettingsViewController` に `scrollController` プロパティを追加し、代入・`nil`・`disconnectStore()`・破棄で接続と切断をする。`disconnectStore()` ではプロパティも `nil` に戻す (→ Requirement: Host への接続と最後の接続)
- [x] 1.3 全 apply 経路を、完了を数える共通の入口に通す (現状 completion を使っていない経路を含む)。反映の順序は変えない (→ Requirement: 命令はデータの反映の後に実行される)
- [x] 1.4 命令の待ち行列を実装する: 積む → `main.async` で 1 回遅らせる → apply 進行中なら最後の completion で実行・view 未ロードなら初回 apply の後に実行 → 実行直前に最新の表示で解決し、無ければ飛ばす。次の命令の実行で先行のアニメーションを止める (→ Requirement: 命令はデータの反映の後に実行される)
- [x] 1.5 位置の解決と送り方を実装する: Cell は `dataSource.indexPath(for:)`、Section は見出し (supplementary) のレイアウト属性か最初・最後の表示 Cell で範囲を求める、先頭・末尾は内容の両端 (Root Header / Footer を含む)。clamp・高い対象は start・非表示と未知の ID は no-op (debug 警告) (→ Requirement: Cell への命令の位置 / Section への命令は見出しごと見せる / 先頭・末尾への命令は内容の両端へ送る)
- [x] 1.6 一方向の着地を実装する: 推定高さによるずれを到着後に進行方向と同じ向きでだけ補正し、アニメーションなしは確定位置へ送り直す (→ Requirement: アニメーションは一方向で着地する)
- [x] 1.7 `captureScrollAnchor()` / `restoreScrollAnchor(_:)` と中身を公開しない `KsScrollAnchor` を追加する。戻しは待ち行列を通し、見つからなければ何もしない (→ Requirement: 位置を控える・戻す窓口)
- [x] 1.8 SwiftUI の `.scrollController(_:)` 修飾子を Store 方式・DSL 方式の両方に追加する。DSL 方式は引き直しの受け口を置き、受けた時点の宣言ツリーの世代を控え、`main.async` の後に明示 ID → `ForEach` の key の順で `DSLIdentityUUID.uuid(from:)` により最終 ID を求め、宣言ツリーに在るほうを採って Host へ渡す (→ Requirement: SwiftUI の修飾子と宣言 UI での指し方)
- [x] 1.9 テストを追加する (`KsSettingsViewUITests` / `KsSettingsViewSwiftUITests`)。各 Requirement の Scenario を 1 件以上のテストに対応させる。非同期の反映は条件ベースの待機 (test-execution) で書き、処理完了はテスト用の処理済み件数で待つ。既存の `ApplyDiffTests` / `DiffableDataSourceTests` が通ることを確認する (→ 1. の全 Requirement)

## 2. Native Android (settings-view-android-ui)

- [x] 2.1 `KsScrollController` / `KsScrollControlling` / `KsScrollPosition` と内部の命令型・受け口を `jp.kamusoft.kssettingsview.ui` に、`rememberScrollController()` を `jp.kamusoft.kssettingsview.compose` に追加する。受け口は弱参照、最後の接続が勝ち、置き換え時に debug 警告ログ、メインスレッド以外の呼び出しは debug で例外 (→ Requirement: スクロール命令ハンドルの公開 / Host への接続と最後の接続)
- [x] 2.2 `KsSettingsView` に `scrollController` プロパティを追加し、代入・`null`・`unbind()` で接続と切断をする。`unbind()` ではプロパティも `null` に戻し、`bind()` し直しても自動ではつなぎ直さない (→ Requirement: Host への接続と最後の接続)
- [x] 2.3 全 `submitList` 経路 (部分 Diff・`submitFullUpdate`・`submitContentUpdate`) を共通の入口に通し、提出のたびに世代番号を進めて最新世代の commit callback だけを待てるようにする (先行世代の callback は `AsyncListDiffer` が破棄するので数えない)。反映の順序は変えない (→ Requirement: 命令はデータの反映の後に実行される)
- [x] 2.4 命令の待ち行列を実装する: 積む → `post` で 1 回遅らせる → commit 待ちがあれば最新世代の commit callback で実行・未 attach / 未 bind なら attach と初回の反映の後に実行・window から外れている間 (通常遷移) は取り付け直しの後に実行 → 実行直前に `currentList` で解決し、無ければ飛ばす。次の命令の実行で先行のスクロールを止める (→ Requirement: 命令はデータの反映の後に実行される)
- [x] 2.5 位置の解決と送り方を実装する: Cell 行・Section の範囲 (見出し行か最初の Cell 行 〜 Footer 行か最後の Cell 行)・内容の両端 (ConcatAdapter の Root Header / Footer を含む)。clamp・高い対象は Start・非表示と未知の ID は no-op (debug 警告) (→ Requirement: Cell への命令の位置 / Section への命令は見出しごと見せる / 先頭・末尾への命令は内容の両端へ送る)
- [x] 2.6 一方向の着地を実装する: アニメーションありは Start / Center / End の合わせを持つ `LinearSmoothScroller`、アニメーションなしは `scrollToPositionWithOffset` と配置後の補正 (→ Requirement: アニメーションは一方向で着地する)
- [x] 2.7 `captureScrollAnchor()` / `restoreScrollAnchor(anchor)` と `Parcelable` の `KsScrollAnchor` を追加する。戻しは待ち行列を通し、見つからなければ何もしない (→ Requirement: 位置を控える・戻す窓口)
- [x] 2.8 既存の `SavedState` に `KsScrollAnchor` を足し、attach と root の反映がそろってから戻す。既定 id の View が複数あるときは保存・復元しない既存の規則に従う (→ Requirement: Activity の作り直しで位置を戻す)
- [x] 2.9 Compose の `KsSettingsView(...)` の Store 方式・DSL 方式の両方に `scrollController` 引数を追加する。DSL 方式は引き直しの受け口を置き、受けた時点の宣言ツリーの世代を控え、1 フレーム後に明示 ID → `forEach` の key の順で `DSLIdentityId.id(from)` により最終 ID を求め、宣言ツリーに在るほうを採って Host へ渡す (→ Requirement: Compose の引数と宣言 UI での指し方)
- [x] 2.10 Compose の `KsSettingsView(...)` の中の View Host でも、Activity の作り直しで保存状態から位置が戻ることを Robolectric で確かめる (Compose UI の `ViewFactoryHolder` が保存状態を運ぶ — design Decision 7)。戻らない場合は実装を止めて報告する (→ Requirement: Activity の作り直しで位置を戻す)
- [x] 2.11 テストを追加する (Robolectric。付け外しは `AdapterReattachTest`、Activity の作り直しは `DateCalendarRecreationTest` の形を流用)。各 Requirement の Scenario を 1 件以上のテストに対応させ、非同期の反映は条件ベースの待機で書く。既存の `ListAdapterDiffTest` / `AdapterReattachTest` / `ApplyDiffTest` が通ることを確認する (→ 2. の全 Requirement)

## 3. Bridge / Binding (maui-bridge)

- [x] 3.1 iOS の `KsSettingsBridge` に Native のハンドルを持たせ、`makeHostViewController` でつなぎ、控えがあれば `restoreScrollAnchor` してから返す。`releaseHost` で `captureScrollAnchor` してから外す。`dispose` で控えとハンドルを捨てる (→ Requirement: Host の作り直しをまたぐスクロール位置の保持)
- [x] 3.2 iOS の `KsSettingsBridge` に `@objc` の `scrollToCell` / `scrollToSection` / `scrollToStart` / `scrollToEnd` を追加する (位置は整数、範囲外は start)。`maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` に手書きで追随する (→ Requirement: スクロール命令の Bridge API)
- [x] 3.3 Android の `KsSettingsBridge` に 3.1・3.2 と同じものを追加する。自動束縛で managed API が生成されることを確認し、要れば `Transforms/Metadata.xml` を直す (→ Requirement: スクロール命令の Bridge API / Host の作り直しをまたぐスクロール位置の保持)
- [x] 3.4 両 OS の Bridge の単体テストを追加する: Host 生成後の命令・Host が無いときと破棄後の no-op・releaseHost をまたぐ位置の保持・切断中の挿入後の保持・作り直し直後の命令が優先・控えは一度だけ (→ maui-bridge の全 Scenario)
- [x] 3.5 IntegrationHost の共通シナリオ (`maui/tests/shared/KsBridgeScenario.cs`) にスクロール命令を足し、両 OS で C# からの呼び出しが Native の Host に届くことを確かめる (→ Scenario: C# の binding から命令を呼べる)

## 4. MAUI facade (maui-core)

- [x] 4.1 `IScrollController` と `ScrollPosition` を公開し、internal の実装クラスを追加する (→ Requirement: スクロール命令のインターフェースと位置の公開)
- [x] 4.2 `SettingsView.ScrollController` (既定 OneWayToSource・`defaultValueCreator` で自身の実体・外からの代入は自身の実体へ戻す) を追加する (→ Requirement: SettingsView が作った命令ハンドルを ViewModel へ渡す)
- [x] 4.3 `Section.SectionId` / `CellBase.CellId` を追加する。表示と Bridge への配信に影響しないこと (→ Requirement: Section と Cell の明示 ID)
- [x] 4.4 `KsItemsSourceBinder` に、生成物と同じ並びの生成元の項目の並びを持たせ、生成・追加・削除・置換・移動・作り直しの各経路で更新する。そのうえで対象の解決を実装する: 明示 ID → 生成元の項目 (その並びで対応を取る。`ItemsSource` を列挙し直さない) の順に表示順で最初の要素を採り、`FindCellId` / `FindSectionId` で Bridge の ID へ引き直す。解決できなければ gateway を呼ばず Debug に英語の警告 (→ Requirement: 命令の対象の解決)
- [x] 4.5 `IKsSettingsGateway` に 4 メソッドを足し、両 OS の `KsBridgeGateway` で Bridge へ委譲する。gateway 未接続の命令は何もしない (→ Requirement: 命令の gateway 経路と Host が無い間の扱い)
- [x] 4.6 `SettingsView.ScrollControllerReadyCommand` を追加し、Handler で Host の生成と取り付けが済むたびに `CanExecute(null)` を確かめて `Execute(null)` する (→ Requirement: 準備完了のコマンド)
- [x] 4.7 fake gateway に 4 メソッドの `GatewayCall` 記録を足し、net10.0 のユニットテストを追加する (GatewayScope の `Connect` / `ReleaseHost` / `Reconnect` で Host の世代を再現)。各 Requirement の Scenario を 1 件以上のテストに対応させる (→ maui-core の全 Requirement)

## 5. Samples (samples-ios / samples-android / samples-maui、sample-parity)

- [x] 5.1 iOS の Sample に「スクロール制御」デモ画面とルートメニュー項目を追加する (ハンドルは `@State`) (→ Requirement: スクロール制御デモ画面 / samples-ios)
- [x] 5.2 Android の Sample に同じ画面とメニュー項目を追加する (ハンドルは `rememberScrollController()`) (→ Requirement: スクロール制御デモ画面 / samples-android)
- [x] 5.3 MAUI の Sample に同じ画面 (Demo 区分) とメニュー項目を追加する。ViewModel は `IScrollController` を OneWayToSource で受け取り、`ScrollControllerReadyCommand` で準備完了を控える (→ Requirement: スクロール制御デモ画面 / samples-maui)
- [x] 5.4 3 platform の画面タイトル・メニュー項目・操作の文言・Section 構成・デモデータが一字一句一致することを突き合わせる (→ sample-parity)

## 6. 検証

- [x] 6.1 iOS / Android / MAUI のテストを全件実行し、実行件数を報告する (コマンドと空振りする範囲は cross/test-execution の手順に従う)
- [x] 6.2 `ios/Sources/` の変更について Swift 6 言語モードの適合を確認する (ios/swift6-language-mode-check)
- [x] 6.3 3 platform の Sample を Simulator / Emulator で動かし、スクロール制御の観測点を確かめて証跡を `evidence/` に残す: 各操作の到達・追加直後の末尾命令が新しい末尾へ届く・中央 / 末尾合わせのアニメーションが行き過ぎずに一方向で着地する (動きの過程を見る)
- [x] 6.4 MauiHost のページ再訪問 (Handler の切断と再接続) と Android の Activity の作り直しで、スクロール位置が戻ることを両 OS で確かめて証跡を残す (settingsview-migration-defects の再現手順で A/B)
