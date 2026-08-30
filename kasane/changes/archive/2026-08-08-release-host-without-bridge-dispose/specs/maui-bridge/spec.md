# Delta Spec: maui-bridge (release-host-without-bridge-dispose)

## ADDED Requirements

### Requirement: Host の単独解放

Bridge は `releaseHost()` を提供しなければならない (SHALL)。`releaseHost()` は Host のみを解放し、Store (設定ツリーと Theme) は維持する。

解放時、Bridge は旧 handle の Store 購読を解除して無効化する — 解放後に Store へ適用された更新は旧 handle の表示に反映されない。旧 handle の view 階層からの取り外しと参照破棄は呼び出し側の責務とする。

`releaseHost()` は冪等であり、Host 不在時 (未生成・解放済み) および `dispose()` 後の呼び出しは no-op とする。解放後の Bridge は旧 Host が保持していた資源 (Android の `Context` を含む) への参照を持たない。

#### Scenario: 解放後の再生成は Store 現在状態を復元する
- **GIVEN** `setRoot` 済みで Host を表示中の Bridge
- **WHEN** `releaseHost()` を呼び、`makeHost*` で新しい Host を生成して view 階層へ取り付ける
- **THEN** 返る handle は解放前と別のインスタンスで、表示には解放前と同じ Store 現在状態が復元される

#### Scenario: 解放中の更新は再生成時に反映される
- **GIVEN** `releaseHost()` 済み (Host 不在) の Bridge
- **WHEN** `replaceCell` / `updateAccessory` / `setTheme` を呼んでから `makeHost*` で Host を生成して取り付ける
- **THEN** 新しい Host の表示は更新後の Store 現在状態になる

#### Scenario: 解放後の Store 更新は旧 handle に反映されない
- **GIVEN** Host を表示中に `releaseHost()` を呼び、旧 handle を view 階層に残置したままの状態
- **WHEN** `replaceCell` / `setTheme` を呼ぶ
- **THEN** 旧 handle の表示は変化しない

#### Scenario: releaseHost は冪等で Store を維持する
- **GIVEN** `setRoot` 済みで Host を表示中の Bridge
- **WHEN** `releaseHost()` を連続で複数回呼ぶ
- **THEN** エラーやクラッシュは発生せず、その後の `makeHost*` は root と Theme を復元した新しい handle を返す

#### Scenario: dispose 後の releaseHost は no-op
- **GIVEN** `dispose()` 済みの Bridge
- **WHEN** `releaseHost()` を呼ぶ
- **THEN** エラーやクラッシュは発生せず、`makeHost*` は引き続き null を返す

#### Scenario: Android は解放後に別の Context で再生成できる
- **GIVEN** `releaseHost()` 済みの Android Bridge
- **WHEN** 解放前とは別の `Context` で `makeHostView(context)` を呼ぶ
- **THEN** 新しい `Context` に紐づく handle が返り、Store 現在状態が復元される

#### Scenario: 解放後、Bridge は旧 Host への参照を保持しない
- **GIVEN** `releaseHost()` 済みで、旧 handle への外部参照をすべて破棄した状態
- **WHEN** GC (相当の回収処理) を実行する
- **THEN** 旧 Host インスタンス (Android では生成時の `Context` を含む) が回収される (`WeakReference` で検証する)

## MODIFIED Requirements

### Requirement: Native Host の生成と接続

Bridge は、内部 Store に接続済みの Native Host handle (iOS: view controller、Android: view) を生成して公開する API を提供しなければならない (SHALL)。Android の Host 生成 API は `Context` を引数で受け取り、Bridge は `Context` を保持しない。呼び出し側は handle を view 階層へ取り付けて表示する。

Bridge は同時に1つの Host をサポートする。生きている Host がある間の `makeHost*` 再呼び出しは同じ handle を返す。`releaseHost()` による解放後の `makeHost*` は、Store 現在状態から表示を復元した**新しい** handle を返す。`setRoot` は Host 生成の前後どちらで呼んでもよく、Host は接続時点の Store 現在状態から表示を復元する。

#### Scenario: Host 生成 → setRoot の順で表示される
- **GIVEN** Bridge から生成した Native Host を view 階層に取り付けた状態
- **WHEN** `setRoot` を呼ぶ
- **THEN** Native の設定 list に root の内容が表示される

#### Scenario: setRoot → Host 生成の順でも表示される
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** その後に Native Host を生成して view 階層に取り付ける
- **THEN** Native の設定 list に現在の root の内容が表示される (購読開始前の状態復元)

#### Scenario: 生きている Host がある間は同じ handle を返す
- **GIVEN** Host 生成済み (未解放) の Bridge
- **WHEN** `makeHost*` を再度呼ぶ
- **THEN** 最初と同じ handle が返る

### Requirement: .NET binding からの呼び出し

net10.0-ios / net10.0-android の Binding プロジェクトを通じて、C# から Bridge の全公開 API (Builder・Host 生成・`releaseHost`・破棄・`setRoot`・更新 API・`setTheme`) を呼び出せなければならない (SHALL)。

#### Scenario: C# からの参照とビルド
- **GIVEN** Binding プロジェクトを参照する net10.0-ios / net10.0-android の C# コード
- **WHEN** Bridge の公開 API を参照するコードをビルドする
- **THEN** コンパイルとリンクが成功する

#### Scenario: C# からの実行時疎通
- **GIVEN** テスト資産として維持される検証ホスト (シミュレータ / エミュレータ上で動作)
- **WHEN** C# から Builder で LabelCell を構築し、Native Host を取り付けて `setRoot` を呼ぶ
- **THEN** Native の設定 list に LabelCell が表示される

#### Scenario: C# からの解放と再生成
- **GIVEN** 検証ホストで Host を表示中の状態
- **WHEN** C# から `releaseHost()` を呼び、Store を更新してから `makeHost*` で再生成して取り付ける
- **THEN** Native の設定 list に更新後の Store 現在状態が表示される
