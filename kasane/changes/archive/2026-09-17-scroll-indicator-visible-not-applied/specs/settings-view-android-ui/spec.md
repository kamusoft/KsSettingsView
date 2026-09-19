## ADDED Requirements

### Requirement: スクロールバー表示の Theme 反映 (Android)

設定リストの縦スクロールバーの表示有無は `Theme.scrollIndicatorVisible` に従う (SHALL)。既定 (`true`) では、スクロール操作中に縦スクロールバーが実際に描画される (SHALL)。`false` では描画されない (SHALL)。反映は最初の表示時と、表示中の Theme 差し替え時 (`theme` への代入・Store 経由・Compose の DSL 再評価・構造更新と同時の Theme 適用のいずれの経路でも) の両方で行う (SHALL)。Theme の差し替えでスクロールバーの表示だけが変わるとき、表示中の行は作り直されず (Section / Cell の ID と、表示中の行の View インスタンスがともに同じ)、スクロール位置 (先頭に見えている行の位置とそのずれ) は変わらない (SHALL)。横スクロールバーは本プロパティの対象にしない (SHALL NOT)。

スクロールバーのつまみの外観は、ライブラリ所有 UI の他の同梱テーマ由来の値と同じく、Activity を再生成しない夜間モードの変更にも追従する (SHALL)。リスト端の overscroll 効果の色は、生成済みの設定リストを維持したまま差し替える手段が platform に無いため、夜間モード変更への追従を保証しない (構築時の外観のまま残り得る)。

PickerCell の選択面の候補リストの縦スクロールバーも、選択面を開いた時点の `Theme.scrollIndicatorVisible` に従い、`true` ならスクロール操作中に実際に描画される (SHALL)。表示中の選択面は、その後の Theme 差し替えに追従しなくてよく、次に開いたときから新しい値に従う (SHALL)。

スクロールバーの有効化は、設定リストと PickerCell の候補リストに限定する (SHALL)。回転ホイール (数値・日付・時刻の選択面) など、ライブラリが所有する他のリストにスクロールバーを出さない (SHALL NOT)。CustomCell の内容や View 形式の Header / Footer など、利用者所有コンテンツの中のリストの見た目を変えない (SHALL NOT)。

#### Scenario: 既定の Theme ではスクロールバーが描画できる状態になる
- **GIVEN** Theme を渡さない KsSettingsView
- **WHEN** 生成して表示する
- **THEN** 設定リストは縦スクロールバーの表示が有効で、スクロールバーの描画に必要な状態が初期化されている

#### Scenario: false を指定した Theme ではスクロールバーを表示しない
- **GIVEN** `scrollIndicatorVisible` が `false` の Theme を渡した KsSettingsView
- **WHEN** 表示する
- **THEN** 設定リストの縦スクロールバーは表示が無効になっている

#### Scenario: 表示中の Theme 差し替えに追従する
- **GIVEN** 既定の Theme で表示中の KsSettingsView
- **WHEN** `scrollIndicatorVisible` が `false` の Theme を代入し、その後 `true` の Theme を代入する
- **THEN** 縦スクロールバーの表示は無効になり、その後ふたたび有効になる

#### Scenario: Store 経由と DSL 経由でも同じ結果になる
- **GIVEN** Store 方式で表示中の KsSettingsView と、DSL 方式で表示中の KsSettingsView
- **WHEN** それぞれの経路で `scrollIndicatorVisible` が `false` の Theme へ更新する
- **THEN** どちらも縦スクロールバーの表示が無効になる

#### Scenario: 構造更新と同時に適用される Theme も反映される
- **GIVEN** 既定の Theme で表示中の KsSettingsView
- **WHEN** Section 構成の全体更新と同時に、`scrollIndicatorVisible` が `false` の Theme を適用する
- **THEN** 更新後の設定リストの縦スクロールバーは表示が無効になっている

#### Scenario: スクロールバーの表示だけを変えても行とスクロール位置は維持される
- **GIVEN** 画面に収まらない数の行を持ち、途中までスクロールした状態で表示中の KsSettingsView
- **WHEN** `scrollIndicatorVisible` だけが異なる Theme を代入する
- **THEN** 表示中の行の View インスタンスと Section / Cell の ID は代入前と同じで、先頭に見えている行の位置とそのずれも代入前と同じである

#### Scenario: 夜間モードの変更にスクロールバーのつまみが追従する
- **GIVEN** 夜間モードでない Configuration で表示中の KsSettingsView (Activity は uiMode を自前処理し再生成しない)
- **WHEN** Configuration が夜間モードへ変わる
- **THEN** 設定リストのスクロールバーのつまみは、同梱テーマを夜間モードで解決した外観になる

#### Scenario: PickerCell の候補リストも表示設定に従う
- **GIVEN** 既定の Theme の KsSettingsView と、`scrollIndicatorVisible` が `false` の Theme の KsSettingsView
- **WHEN** それぞれの PickerCell から選択面を開く
- **THEN** 前者の候補リストは縦スクロールバーの表示が有効で描画に必要な状態が初期化されており、後者は縦スクロールバーの表示が無効になっている

#### Scenario: 回転ホイールにはスクロールバーを出さない
- **GIVEN** 既定の Theme の KsSettingsView から開いた、数値の選択面の回転ホイール
- **WHEN** 生成する
- **THEN** ホイールのリストはスクロールバーの描画に必要な状態が初期化されておらず、スクロールバーは描画されない

### Requirement: 利用者所有コンテンツの Context は設定リストの生成方法に依らない

CustomCell の内容と View 形式の Section / Root Header・Footer を生成するときに利用者へ渡す Context は、ホストが `KsSettingsView` に渡した Context そのもの (同じインスタンス) である (SHALL)。ホストがテーマを被せた Context を渡した場合、利用者所有コンテンツはそのテーマの属性を解決できる (SHALL)。設定リストをはじめライブラリ所有 UI を同梱テーマ付きの Context から生成することは、この保証を変えない (SHALL)。

#### Scenario: ホストが被せたテーマの属性が利用者所有コンテンツから解決できる
- **GIVEN** Activity と異なるテーマを被せた Context で生成した KsSettingsView と、CustomCell・View 形式の Section Header・View 形式の Root Header
- **WHEN** 表示し、それぞれの生成に渡された Context からテーマ属性を解決する
- **THEN** 3 つとも、渡された Context はホストが `KsSettingsView` に渡した Context と同じインスタンスであり、テーマ属性はホストが被せたテーマ側の値に解決される

#### Scenario: その構成でも設定リストのスクロールバーは描画できる
- **GIVEN** Activity と異なるテーマを被せた Context で生成した KsSettingsView
- **WHEN** 生成して表示する
- **THEN** 設定リストはスクロールバーの描画に必要な状態が初期化されている

### Requirement: Header / Footer 背景色の既定は透明 (Android)

ライブラリ既定色の light / dark セット (`KsSettingsViewDefaults.lightTheme()` / `darkTheme()`) が持つ Header / Footer の背景色は、どちらのセットでも透明である (SHALL)。Header / Footer の背景色が未指定 (`Unspecified`) の Theme は、外観に依らず透明へ解決され、何も指定しない Header / Footer の領域には list 下地がそのまま見える (SHALL)。明示指定した背景色は従来どおりそのまま描画される (SHALL)。この既定は iOS の既定と同じである (SHALL)。

本 Requirement は、ライブラリ既定色の light / dark セットが定めた「Header / Footer の背景」の既定値 (light は従来の固定値、dark はライブラリ所有の dark セットの値) を置き換える。他の既定色と、未指定色の外観解決・夜間モード変更時の再解決の契約は変えない。

#### Scenario: 既定セットの Header / Footer 背景色は透明
- **GIVEN** `lightTheme()` と `darkTheme()` が返す Theme
- **WHEN** Header / Footer の背景色を読む
- **THEN** どちらのセットでも透明であり、`Unspecified` ではない

#### Scenario: 何も指定しない Header / Footer には list 下地が見える
- **GIVEN** list 下地だけを明示した Theme と、text の Section Header / Footer を持つ KsSettingsView
- **WHEN** 夜間モードでない Configuration と夜間モードの Configuration のそれぞれで表示する
- **THEN** どちらでも Header / Footer の領域にライブラリの背景は描画されず、list 下地が見える

#### Scenario: 明示指定した背景色はそのまま描画される
- **GIVEN** Header の背景色を明示した Theme と、text の Section Header を持つ KsSettingsView
- **WHEN** 表示する
- **THEN** Header の領域は明示した色で描画される
