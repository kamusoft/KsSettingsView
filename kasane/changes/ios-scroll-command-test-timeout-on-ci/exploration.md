# Exploration: ios-scroll-command-test-timeout-on-ci

## 課題 / 動機

iOS の SwiftUI DSL のスクロール制御のテスト `test_ForEachのkeyでSectionを指せる` (`ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift`) が、develop の検証 CI で 1 度失敗した。再実行で通るかどうかに頼る状態は、リリース前の事前確認 (develop の CI が緑であること) を不安定にする。

発見の文脈: リリース 0.1.0-beta.8 の事前確認中、develop の検証 CI (ci.yml) の `ios / verify` の step「Run tests on iOS Simulator」で失敗した。

- run: https://github.com/kamusoft/KsSettingsView/actions/runs/36677242463 (attempt 1、commit 4d06111)
- 失敗の内容
  - `DSLScrollControlTests.swift:288`: 「命令 1 件の処理」が deadline 3.000 秒以内に成立せず、経過 4.084 秒の時点でも `processed=0`
  - `DSLScrollControlTests.swift:289`: 続く位置の検査も連鎖して失敗 (見出しの上端 2345.0、期待値 0.0)
- 同じ iOS コードでの前回 run (commit 77e90ac、run 36676536425) では同テストが 0.448 秒で通っている。4d06111 の変更は MAUI のテスト 1 ファイルだけで、iOS には触れていない
- 失敗した回はこのテスト 1 件に 11.5 秒かかっており、シミュレータが重かった
- テストの `host(_:)` は、SwiftUI 配下の `KsSettingsViewController` が見つかり、最初の snapshot が反映されるまで待ってから返す。スクロール命令のハンドルは `KsSettingsView` の `makeUIViewController` の中で接続される (`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`)。そのため「接続前に出した命令が捨てられた」わけではなさそう
- スクロール制御 (add-scroll-control) で足したばかりのテストで、CI での実行回数はまだ少ない

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補

## 未決の論点

- 未探索 (簡易起票)
- なぜ 4 秒経っても `processedScrollCommandCount` が 0 のままだったのか。命令の処理が何かの状態 (反映中の snapshot、layout、window への載り方など) を待って止まる経路があるのか、単に遅い環境で deadline 3 秒が足りないだけなのか
- 同じファイルの他のテスト (`awaitProcessed` を使うもの) にも同じ揺れの余地があるか
- 直す先がテストの待ち合わせ (deadline・待つ条件) なのか、実装側の命令処理の経路なのか

## UI 素材

## 変更級の推奨: 未判定
