# Delta: settings-view-ios-ui (fix-ios-full-content-refresh)

## ADDED Requirements

### Requirement: full 更新における同一 ID Cell の内容反映

iOS host は、full snapshot 適用 (`.full` diff・`replaceAll`・`replaceSection`・DSL preflight 由来の `.full` が合流する経路) において、旧・新 visible projection の**双方に存在し値が変化した**同一 ID の Cell の内容を表示へ反映する SHALL。

行 identity の維持 (Native cell を破棄しないこと) は、当該 Section が supplementary (header / footer) の再構成対象でなく、かつ旧・新で Cell の**具象型が同一**の場合に保証する SHALL。次の場合は Native cell の交換を許容するが、内容は最新で表示される SHALL:

- header / footer の変化 (または強制 reload 指定) により Section 全体が再構成される場合 (既存契約どおり Section 内の全 Cell が再構成される)
- 同一 ID のまま Cell の具象型が変わる場合 (構造 identity は UUID のみで判定されるため同一 item と扱われるが、reconfigure では Native cell を維持できない)

内容再適用の対象は旧・新 visible projection の双方に存在する Cell に限る SHALL。新規挿入・削除・hidden の Cell には内容再適用を重ねない SHALL (新規挿入・hidden 復帰の Cell は構造反映の通常 bind で最新内容になる)。内容再適用の対象が空でも、構造の反映は必ず実行される SHALL (初回適用は自然に対象空となる)。

#### Scenario: full 更新で表示中セルの内容変化が反映される
- **GIVEN** 複数の Cell を持つ Section が Store 接続で表示されている
- **WHEN** 同一 Section ID・同一 Cell ID のまま一部の Cell の内容 (title 等) だけを変えた root を `replaceAll` (`.full`) で適用する
- **THEN** 内容を変えた Cell の表示は新しい内容になり、内容を変えていない Cell の表示は変わらず、行の挿入・削除は発生しない

#### Scenario: 内容変化した表示中セルの行 identity が維持される
- **GIVEN** 複数の Cell を持つ Section が Store 接続で表示されている
- **WHEN** 同一 Cell ID のまま内容だけを変えた root を `replaceAll` で適用する
- **THEN** 内容を変えた Cell の Native cell インスタンスは破棄されず、同一インスタンスのまま新しい内容を表示する

#### Scenario: 構造変更と内容変更が混在する full 更新
- **GIVEN** 複数の Cell を持つ Section が Store 接続で表示されている
- **WHEN** 新規 Cell の挿入・既存 Cell の削除・同一 ID Cell の内容変更を同時に含む root を `replaceAll` で適用する
- **THEN** 挿入された Cell は最新内容で表示され、削除された Cell は消え、同一 ID で残った内容変更 Cell は新しい内容を表示する

#### Scenario: 可視性と内容の同時変更で内容が取りこぼされない
- **GIVEN** hidden の Cell と表示中の Cell を持つ root が Store 接続で表示されている
- **WHEN** hidden Cell の再表示 (可視化) と、表示中の同一 ID Cell の内容変更を同時に含む root を `replaceAll` で適用する
- **THEN** 再表示された Cell は最新内容で現れ、表示中だった Cell の内容も新しくなる

#### Scenario: replaceSection で同一 ID Cell の内容変化が反映される
- **GIVEN** Text accessory の header と複数の Cell を持つ Section が Store 接続で表示されている
- **WHEN** 同一 Section ID・同一 header のまま一部の Cell の内容だけを変えた Section へ `replaceSection` する
- **THEN** 内容を変えた Cell の表示は新しい内容になる

#### Scenario: header と Cell 内容の同時変更で両方が反映される
- **GIVEN** Text accessory の header と複数の Cell を持つ Section が Store 接続で表示されている
- **WHEN** 同一 Section ID のまま header の text と一部の Cell の内容を同時に変えた root を `replaceAll` で適用する
- **THEN** header と Cell の表示は両方とも新しい内容になる (この場合、Section 全体が再構成されるため Cell の行 identity の維持は保証されない)

#### Scenario: 同一 ID で具象型が変わる Cell の差し替え
- **GIVEN** ある UUID を持つ Cell が Store 接続で表示されている
- **WHEN** 同一 UUID のまま別の具象型の Cell に差し替えた root を `replaceAll` で適用する
- **THEN** 新しい具象型の内容が表示される (Native cell の交換は許容される)

## MODIFIED Requirements

### Requirement: SwiftUI DSL の headerHeight 変更の表示反映

SwiftUI DSL の diff 算出は、旧・新の宣言ツリーの間で同一 ID の Section の `headerHeight` が変化している場合、可視性 preflight と同じ段階で検出し、`.full(newRoot)` **のみ**を発行する SHALL。検出対象の変化は、固定高さ間の変更 (`正値A → 正値B`)・自動から固定 (`-1 → 正値`)・固定から自動 (`正値 → -1`) のいずれも含む SHALL。これにより DSL 経由の headerHeight 変更は、Store 経由 (`replaceSection` / `.full`) と同じ表示結果へ到達する (core/ADR-0018)。

同一再評価内で同一 ID の Cell の内容も変わっている場合、その内容変化は full 更新が内包する内容再適用 (Requirement「full 更新における同一 ID Cell の内容反映」) で表示へ反映される SHALL。diff 算出は `.full` に続けて当該 Cell の `.replaceCell` を発行しない SHALL — 同一 Cell への内容再適用が同一適用内で二重に発火してはならない (変更前契約「`.full` + `.replaceCell` 続発」の廃止。fix-dsl-header-height-diff 実装時は full 更新が内容反映を内包していなかったための暫定措置であった)。

#### Scenario: headerHeight のみの変更が表示へ反映される
- **GIVEN** Text accessory の header を持つ Section が SwiftUI DSL で表示されている
- **WHEN** 再評価で同一 Section ID のまま `headerHeight` だけが変わる
- **THEN** diff 算出は `.full(新ツリー)` のみを発行し、表示中の header の高さは新しい固定高さになる

#### Scenario: headerHeight と Cell 内容の同時変更で両方が反映され内容再適用は一度だけ
- **GIVEN** Text accessory の header と既知の ID の Cell を持つ Section が SwiftUI DSL で表示されている
- **WHEN** 再評価で同一 ID のまま `headerHeight` と Cell の内容 (title 等) が同時に変わる
- **THEN** diff 算出は `.full(新ツリー)` のみを発行し、表示は header の高さと Cell の内容の両方が新しくなる。当該 Cell への内容再適用は一度だけ行われる

#### Scenario: headerHeight が不変なら preflight は発火しない
- **GIVEN** Section が SwiftUI DSL で表示されている
- **WHEN** 再評価で `headerHeight` は同一のまま Cell の内容だけが変わる
- **THEN** `.full` は発行されず、内容変化は `.replaceCell` (reconfigure 経路) の既存経路で反映される
