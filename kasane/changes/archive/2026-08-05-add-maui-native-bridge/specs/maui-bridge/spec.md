# Delta Spec: maui-bridge (add-maui-native-bridge)

## ADDED Requirements

### Requirement: Bridge の生成と Root 構築

Bridge は、Builder で設定ツリーを構築し `setRoot` で内部所有 Store へ反映する API を、iOS (`@objc` 互換) / Android (JVM 互換) で提供しなければならない (SHALL)。Builder は Section (text の header / footer を指定可) の構築・追加と、Section への LabelCell の追加をサポートする。

ID の interop 契約: Section / Cell の ID は canonical UUID 文字列とし、**Bridge (Builder / insert 系 API) が採番して呼び出し側へ返す**。呼び出し側は返された ID だけを更新 API に渡す。未知・不正な ID を指定した Cell / Section 操作は no-op であり、この検証と結果は iOS / Android で同一とする。

スレッド契約: Bridge の全 API は各 platform の UI スレッドから呼び出す (呼び出し側契約)。他スレッドからの呼び出しの挙動は保証しない。

#### Scenario: LabelCell を含む root の表示
- **GIVEN** Builder で Section 1個 (header text 付き) と LabelCell 複数個を構築した Bridge
- **WHEN** `setRoot` を呼び、生成済みの Native Host を表示する
- **THEN** Native の設定 list に構築どおりの Section と LabelCell が表示される

#### Scenario: setRoot の再呼び出しは全置換
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** 別の root で `setRoot` を再度呼ぶ
- **THEN** 表示は新しい root の内容に全置換される

#### Scenario: 採番された ID で後続操作ができる
- **GIVEN** Builder が返した cellID を保持している呼び出し側
- **WHEN** その cellID で `replaceCell` を呼ぶ
- **THEN** 対象 Cell の内容が更新される

#### Scenario: 不正な ID は no-op
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** Bridge が採番していない文字列を ID として `removeCell` を呼ぶ
- **THEN** 状態と表示は変化しない (iOS / Android で同じ結果)

### Requirement: Native Host の生成と接続

Bridge は、内部 Store に接続済みの Native Host handle (iOS: view controller、Android: view) を生成して公開する API を提供しなければならない (SHALL)。Android の Host 生成 API は `Context` を引数で受け取り、Bridge は `Context` を保持しない。呼び出し側は handle を view 階層へ取り付けて表示する。

Bridge は同時に1つの Host をサポートする。新たな Host が必要な場合は破棄後に再生成する。`setRoot` は Host 生成の前後どちらで呼んでもよく、Host は接続時点の Store 現在状態から表示を復元する。

#### Scenario: Host 生成 → setRoot の順で表示される
- **GIVEN** Bridge から生成した Native Host を view 階層に取り付けた状態
- **WHEN** `setRoot` を呼ぶ
- **THEN** Native の設定 list に root の内容が表示される

#### Scenario: setRoot → Host 生成の順でも表示される
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** その後に Native Host を生成して view 階層に取り付ける
- **THEN** Native の設定 list に現在の root の内容が表示される (購読開始前の状態復元)

### Requirement: Bridge の lifecycle

Bridge は明示的な破棄 API を提供しなければならない (SHALL)。破棄は冪等であり、破棄後の操作 API (`setRoot` / 更新 API / `setTheme` / Host 生成) の呼び出しは no-op とする。破棄後に Host の表示が更新されることはない。

#### Scenario: 破棄は冪等
- **GIVEN** 破棄済みの Bridge
- **WHEN** 破棄 API を再度呼ぶ
- **THEN** エラーやクラッシュは発生しない

#### Scenario: 破棄後の操作は no-op
- **GIVEN** Host を表示中に破棄した Bridge
- **WHEN** `replaceCell` や `setTheme` を呼ぶ
- **THEN** エラーやクラッシュは発生せず、Host の表示は変化しない

### Requirement: Store 操作 1:1 の更新 API

Bridge は Store 公開操作と 1:1 対応する更新 API (`insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `moveCell` / `replaceCell` / `updateAccessory` / `replaceCells`) を提供しなければならない (SHALL)。各操作は内部所有 Store の対応する公開操作へ素通しされ、Store の現行契約 — hidden 要素を含む model 配列上の index、同じ ID の内容更新は identity を維持、Cell / Section 操作における未知 ID の no-op、`updateAccessory` の現行通知挙動 — がそのまま適用される。

phase-1 の `updateAccessory` および Builder の Section header / footer が輸送する accessory は text (および clear = null) に限定する。`updateAccessory` は target (root header / root footer / 指定 Section の header / footer) と text または clear を受け取り、clear 後は accessory が指定されていない場合と同じ表示になる。

#### Scenario: Cell の構造操作が表示へ反映される
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** `insertCell` で新しい LabelCell を挿入し、`removeCell` で既存 Cell を削除する
- **THEN** Native の設定 list に挿入と削除が反映される

#### Scenario: Section の構造操作が表示へ反映される
- **GIVEN** 複数 Section を持つ root で `setRoot` 済みの Bridge
- **WHEN** `insertSection` で Section を挿入し、`moveSection` で Section の順序を入れ替え、`removeSection` で Section を削除する
- **THEN** Native の設定 list に挿入・並べ替え・削除が反映される

#### Scenario: replaceCell は行の identity を維持する
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** 表示中の LabelCell と同じ cellID で内容の異なる LabelCell を `replaceCell` に渡す
- **THEN** 同じ行の表示内容が更新され、行の削除+挿入 (構造変更) としては扱われない

#### Scenario: replaceCells は1バッチで反映される
- **GIVEN** `setRoot` 済みで複数の LabelCell を表示中の Bridge
- **WHEN** `replaceCells` に複数の (cellID, 新 Cell) を渡す
- **THEN** 対象行の表示内容が1回のバッチ内容更新として反映される

#### Scenario: 全12操作が契約どおりに反映される
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** 12操作それぞれを代表的な引数で呼ぶ
- **THEN** 各操作後に観察可能な結果 (Host の表示内容と通知) が、対応する Store 操作の契約 (index 解釈・identity 維持・no-op 条件を含む) と一致する (操作ごとに検証する)

### Requirement: Theme 適用

Bridge は `setTheme` を提供し、Theme の輸送 DTO (primitive 表現) を Store の `applyTheme` へ変換しなければならない (SHALL)。輸送 DTO の項目は各 platform の Theme 公開項目と 1:1 対応し、未指定 (null) は Theme 側の未指定として扱う。Store の Theme 契約 (構造 Diff を発行しない・同値 Theme は再通知しない・Section / Cell の identity を変えない) がそのまま適用される。MAUI 慣例型での Theme 公開は本 capability の対象外 (phase-2 の facade の責務)。

#### Scenario: Theme 変更が表示へ反映される
- **GIVEN** `setRoot` 済みの Bridge
- **WHEN** 異なる Theme で `setTheme` を呼ぶ
- **THEN** 表示中の list の表示属性が再評価され、設定ツリーの構造と identity は変化しない

#### Scenario: 同値 Theme は再適用されない
- **GIVEN** `setTheme` 済みの Bridge
- **WHEN** 同じ内容の Theme で `setTheme` を再度呼ぶ
- **THEN** Theme 更新は通知されない

### Requirement: .NET binding からの呼び出し

net10.0-ios / net10.0-android の Binding プロジェクトを通じて、C# から Bridge の全公開 API (Builder・Host 生成・破棄・setRoot・更新 API・setTheme) を呼び出せなければならない (SHALL)。

#### Scenario: C# からの参照とビルド
- **GIVEN** Binding プロジェクトを参照する net10.0-ios / net10.0-android の C# コード
- **WHEN** Bridge の公開 API を参照するコードをビルドする
- **THEN** コンパイルとリンクが成功する

#### Scenario: C# からの実行時疎通
- **GIVEN** テスト資産として維持される検証ホスト (シミュレータ / エミュレータ上で動作)
- **WHEN** C# から Builder で LabelCell を構築し、Native Host を取り付けて `setRoot` を呼ぶ
- **THEN** Native の設定 list に LabelCell が表示される
