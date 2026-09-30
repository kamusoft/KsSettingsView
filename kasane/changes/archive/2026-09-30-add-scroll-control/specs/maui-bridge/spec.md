# Delta: maui-bridge (add-scroll-control)

## ADDED Requirements

### Requirement: スクロール命令の Bridge API

iOS / Android の `KsSettingsBridge` は、スクロール命令の公開 API を持つ (SHALL)。

- `scrollToCell(cellID, position, animated)`
- `scrollToSection(sectionID, position, animated)`
- `scrollToStart(animated)` / `scrollToEnd(animated)`

ID は他の更新 API と同じ Bridge の ID (文字列) とする。`position` は整数で受ける (0 = start、1 = center、2 = end)。範囲外の値は start として扱う。これらは Store の公開操作に対応しない更新 API で、Store を経由しない。Bridge が持つ Native のスクロール命令ハンドルを通じて、生きている Host に届く (SHALL)。命令の実行時期と位置の規則は Native の Host の契約 (データの反映の後に実行、位置の合わせ方、非表示・未知の ID は何もしない) に従う。Host が無いとき・`dispose()` の後の呼び出しは何もしない (SHALL)。iOS の binding (`ApiDefinition.cs`) と Android の binding から、C# の managed API として呼べる (SHALL)。

#### Scenario: Host 生成後の命令が Host をスクロールさせる
- **GIVEN** `makeHost*` で Host を生成し、root を設定した Bridge
- **WHEN** 表示範囲の外の Cell の ID で `scrollToCell(cellID, 1, false)` を呼ぶ
- **THEN** その Cell の行の中央が表示範囲の中央に来る

#### Scenario: Host が無いときの命令は何も起こさない
- **GIVEN** `makeHost*` を呼ぶ前、または `releaseHost()` の後の Bridge
- **WHEN** 4 種の命令をそれぞれ呼ぶ
- **THEN** 例外は起きず、その後に生成した Host の位置にも影響しない

#### Scenario: 破棄後の命令は何も起こさない
- **GIVEN** `dispose()` した Bridge
- **WHEN** `scrollToEnd(true)` を呼ぶ
- **THEN** 例外は起きず、何も起こらない

#### Scenario: C# の binding から命令を呼べる
- **GIVEN** 検証ホスト (IntegrationHost) で Bridge の Host を表示している
- **WHEN** C# から `ScrollToEnd(false)` を呼ぶ
- **THEN** Native の Host が内容の末尾までスクロールする

### Requirement: Host の作り直しをまたぐスクロール位置の保持

Bridge は、`releaseHost()` で Host を手放す前に、その Host のスクロール位置を控える (SHALL)。控えは表示範囲の上端にかかる要素とそこからのずれで、Native の Host の `captureScrollAnchor()` による。次の `makeHost*` で作った Host には、返す前に控えた位置を戻す (SHALL)。これは Native の Host の `restoreScrollAnchor` による。戻しは、Host 生成後に届いた命令より先に処理される。控えは戻した時点で使い切る。`dispose()` で控えを捨てる (SHALL)。控えが無い Host は内容の先頭から表示する。

#### Scenario: Host を作り直しても同じ要素が上端にかかる
- **GIVEN** Bridge の Host で途中までスクロールしている
- **WHEN** `releaseHost()` の後に `makeHost*` で新しい Host を作って表示する
- **THEN** 新しい Host で、手放す前に上端にかかっていた要素が同じずれで上端にかかる

#### Scenario: Host が無い間に上に項目が増えても同じ要素へ戻る
- **GIVEN** `releaseHost()` の後、控えた要素より上に Cell を挿入した Bridge
- **WHEN** `makeHost*` で新しい Host を作って表示する
- **THEN** 控えた要素が同じずれで上端にかかる

#### Scenario: 作り直しの直後に出した命令が戻した位置より優先される
- **GIVEN** 途中までスクロールした Host を `releaseHost()` した Bridge
- **WHEN** `makeHost*` で新しい Host を作り、続けて同じ処理で `scrollToStart(false)` を呼ぶ
- **THEN** 最終位置は内容の先頭になる

#### Scenario: 控えは一度だけ使われる
- **GIVEN** 控えた位置を戻した Host を、先頭まで戻してから `releaseHost()` した Bridge
- **WHEN** `makeHost*` で新しい Host を作って表示する
- **THEN** 新しい Host は、2 回目に手放したときの位置 (内容の先頭) で表示される
