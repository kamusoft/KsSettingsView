# Deviation: android-compose-scroll-restore-with-multiple-hosts

- 決定事項 論点 1「Compose では、window から外れた後でも範囲を見分けられる見込み」: 決定事項では View が `AndroidView` の入れ物の子のまま残る見込み → 実際は、Compose は View を入れ物から取り外した後に保存を求め、その時点で親は 1 つも無い。範囲の見分けは、レイアウトの時点と window から外れる時点の控え (`wasAmbiguousWhileAttached`・`scrollAnchorAtDetach`) に頼る。直し方 (数える範囲) は決定事項のまま。理由: 実装前のスパイクで実測 (2026-10-07)
- 決定事項 論点 1「範囲の見分け方」: 決定事項では自身から親をたどって最初に当たる View → 実際は、View Host 自身に「親からの保存を受けない」設定が付いているときは自身を範囲の根にする。理由: AndroidX Fragment はこの設定を Fragment の View そのものに付けるため、Fragment の View が View Host である形を含めないと直らない (2026-10-07)
- 決定事項 論点 1「範囲の見分け方」: 決定事項では範囲の根から下をすべて数える読みだった → 範囲の中に入れ子の境目 (親からの保存を受けない View) があれば、その下は数えない。理由: 入れ子の境目の下は別の入れ物へ保存され、android/ADR-0023 の「保存の入れ物を共有する範囲」に合わせるため。数えると、同じ window に直置きの View Host と Composable の View Host がある構成で、単独の直置き側が保存されない (2026-10-07)
- 決定事項 論点 2「サンプルに経路を 1 本足す」: 決定事項では `KsSettingsView` の画面から別の `KsSettingsView` の画面へ進む経路を 1 本 → 実際は、デモ画面共通の上部バーに「次のデモへ進む」と「メニューを開く」の 2 つを足した。理由: デモ画面はすべて `KsSettingsView` で、1・4 つめの場面を端末で確かめるには `KsSettingsView` を使わない行き先が要る。画面の内容の行ではなく上部バーに置いたのは、デモの内容を platform 間でそろえる規約 (`kasane/handbook/cross/sample-parity.md`) に触れないため (2026-10-07)
- 決定事項 論点 2「テストは 4 つ」: 決定事項の 4 つに加えて、自身が範囲の根になる形・Compose が頼る挙動の見張り・カレンダー選択面の新しい形のテストを足した。理由: 上の差分と、カレンダー選択面が位置と同じ判定を通ることをテストで担保するため (2026-10-07)
- 決定事項 論点 2「MAUI Android」: 決定事項ではサンプルを変えずに確かめる前提だった → 指示により、`samples/maui` の上部バーに `SettingsView` のページから別の `SettingsView` のページへ進む操作を足して、端末で確かめる。理由: MAUI のサンプルにも検証用のホストアプリにも `SettingsView` のページが 2 つ重なる経路が無く、ちらつきと二重の戻しは実物の MAUI でしか見えない (2026-10-07)
- 決定事項 論点 2「MAUI Android」の見立て: 決定事項では「変わるのはページが 2 つ重なったときも 1 つのときと同じになることだけ」→ 実際は、MAUI の `NavigationPage` は上にページを積むと下のページの View を階層から外すため、修正前の数え方でも View Host は 1 つと数えられ、修正前後で観測に差が無い。位置の保持と Bridge の戻しとの整合は修正前後とも成立。理由: 端末で実測 (`evidence/maui-android-scroll-restore-ab.txt`)。回転での作り直し、タブ・モーダルなど View が階層に残る構成は確かめていない (2026-10-07)
- 決定事項 論点 2「端末での確かめ」で未確認のまま残した点 (review-001.md の指摘 1・3): (a) 進んで戻るテストは、レイアウト中に出たレイアウト要求を出し直す補助 (`reissueLayoutRequestsMadeDuringLayout`) を足場に入れている。端末では補助なしで場面 2・4 の位置が戻ることを最終の見え方で確かめたが、レイアウト要求が残らないことを直接には観測していない (b) Android の端末で、2 つの画面が重なった状態からの Activity の作り直しは、修正後のビルドで観測していない (テストでは 1 画面に複数置いた形で確かめた)。理由: 合意した端末の確かめは場面 1・2・4。リリース後に起票元のアプリで確かめる項目に加える (2026-10-07)
- 蒸留時に反映: `kasane/handbook/cross/runtime-behavior-verification.md` — MAUI では上にページを積むと下のページの View が階層から外れること、ページが重なる構成の確かめ方 (サンプルのバーの「次のデモへ進む」) を、Host の作り直しを起こす操作の表に足す
- 蒸留時に反映: `kasane/handbook/cross/local-development-setup.md` — 「MAUI Android」に、アプリのデータを消去すると Debug 構成の配備済みアセンブリも消えて起動しなくなることを足すか確かめる
- 蒸留時に反映: `kasane/decisions/android/0023-default-id-ambiguity-scoped-to-save-container.md` — Compose は View を入れ物から取り外した後に保存を求めるため、範囲の見分けは外れる時点までの控えに頼る、という事実の置き場を決める (ADR の Revisit When か concepts)
- 蒸留時に反映: `kasane/concepts/core/cells/date-picker-selection-surface.md` — 既定 ID と復元の条件を書く箇所 (106 行付近) に、同じ言い直しが要るかを確かめる
- 蒸留時に反映: `kasane/handbook/cross/runtime-behavior-verification.md` — 「Sample のデモ画面には進む先が無い」の記述。サンプルに進む先ができたので手順を書き換える
