## ADDED Requirements

### Requirement: 表示中の外観変更で行の背景と下地が描き直される (iOS)

Theme を渡さない、または該当する色を未指定にした KsSettingsView を表示中に window の外観 (ライト / ダーク) が変わったとき、表示中の描画領域のうち未指定の項目 — 標準 Cell と CustomCell の行の背景・list 下地・Section 装飾の塗り・text の Header / Footer の背景と文字 — は、現在の外観のライブラリ既定の値で描き直される (SHALL)。切替はライト → ダークとダーク → ライトの両方向で成立する (SHALL)。描き直しは行を作り直さずに行われ、Section / Cell の identity・可視行の集合・スクロール位置は維持される (SHALL)。明示した固定色の項目は変わらない (SHALL — 本 change 前と同じ)。

行の背景の追随は、行に適用されている背景設定が保持する `UIColor` の値ではなく、外観変更後にその行の描画に実際に使われている値で観測できる (SHALL)。保持している dynamic な `UIColor` を検証側で解決した値は、この Requirement の充足の根拠にならない。

#### Scenario: 表示中の外観切替で行の背景が dark 既定で描かれる
- **GIVEN** Theme を渡さずライト外観で表示中の KsSettingsView と、実体化している標準 Cell の先頭行
- **WHEN** window の外観をダークへ切り替える
- **THEN** 先頭行の描画に使われている背景の値は dark セットの Cell 背景と一致し、同じ行のタイトル文字と組み合わせて判読できる。先頭行の identity は切替前と同じである

#### Scenario: CustomCell の行の背景も描き直される
- **GIVEN** Theme を渡さずライト外観で表示中の、CustomCell を含む KsSettingsView
- **WHEN** window の外観をダークへ切り替える
- **THEN** CustomCell の行の描画に使われている背景の値は dark セットの Cell 背景と一致する

#### Scenario: スクロール位置と可視行を保ったまま往復する
- **GIVEN** 画面に収まらない行数の root を Theme なしで表示し、途中までスクロールした状態の KsSettingsView (ライト外観)
- **WHEN** window の外観をダークへ切り替え、続けてライトへ戻す
- **THEN** 各切替後の可視行の Cell ID の集合とスクロール位置 (content offset) は切替前と同じで、可視行のインスタンスも同じである。ダーク時は行の背景と list 下地が dark セット、ライトへ戻した後は light セットの値で描かれている

#### Scenario: 明示した固定色の行背景は外観切替で変わらない
- **GIVEN** 固定の (dynamic でない) 色を Cell 背景に明示した Theme でライト外観で表示中の KsSettingsView
- **WHEN** window の外観をダークへ切り替える
- **THEN** 行の描画に使われている背景の値は明示した固定色のままである

#### Scenario: 既存の外観切替テストは描画に効いている値を観測する
- **GIVEN** 表示中の外観切替を検証する既存のテスト (既定色の追随・CellStyle の外観再解決)
- **WHEN** 行の背景の追随を検証する
- **THEN** 行に適用された背景設定が保持する `UIColor` を検証側で解決するのではなく、切替後に描画へ使われている値を観測しており、行の背景が古いまま残る実装ではテストが失敗する

### Requirement: text の Header / Footer の背景色を Theme から適用する (iOS)

Section の text の Header / Footer と Root の text の Header / Footer の背景は、Theme の `headerBackgroundColor` / `footerBackgroundColor` で描かれる (SHALL)。未指定なら現在の外観のライブラリ既定 (Android と同じ light / dark セット) になり、明示した色はその色で描かれる (SHALL)。View accessory (利用者の view を載せた Header / Footer) の背景には適用しない (SHALL NOT — view の見た目は利用者のもの)。

#### Scenario: text Header の背景が Theme の色で描かれる
- **GIVEN** `headerBackgroundColor` に固定色を明示した Theme と、text の Header を持つ Section
- **WHEN** 表示する
- **THEN** Header の描画に使われている背景の値は明示した色と一致する

#### Scenario: 未指定の Header / Footer 背景はダーク外観で dark 既定になる
- **GIVEN** Theme を渡さない KsSettingsView と、text の Header と Footer を持つ Section
- **WHEN** ダーク外観で表示する
- **THEN** Header / Footer の描画に使われている背景の値は dark セットの Header / Footer 背景と一致する

#### Scenario: View accessory の背景には適用しない
- **GIVEN** `headerBackgroundColor` に固定色を明示した Theme と、利用者の view を Header に載せた Section
- **WHEN** 表示する
- **THEN** 利用者の view の背景は Theme の色で塗り替えられない
