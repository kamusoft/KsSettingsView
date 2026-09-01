# Tasks: fix-ios-test-pump-condition-wait

仕分けの正は change 内の分類台帳 [triage.md](triage.md) (呼び出し 206 = A 160 / B 16 / C 30、撤去する定義 20)。

## 1. ヘルパの実装

- [x] 1.1 テスト支援の共有ターゲットを Package.swift に新設し、3 テストターゲット (`KsSettingsViewUITests` / `KsSettingsViewSwiftUITests` / `KsSettingsViewBridgeTests`) から依存させる。着手時に次を確定してから 2 以降へ進む: XCTest failure の発火方式 (共有ターゲットでの XCTest リンク可否、不可なら fail 処理のクロージャ注入等)・`@MainActor` 境界。**共有 (単一定義) は受け入れ条件** — 成立しない場合は 3 コピーへ黙って切り替えず、実装を止めてユーザーへ報告する
- [x] 1.2 条件ベース待機を実装する: 述語 + 実時間 deadline (単調増加時計・共通の既定値・呼び出しごと上書き可) + ループ内で RunLoop を短く回す + 超過時は実測値付き fail (→ Requirement: 条件ベース待機)
- [x] 1.3 待機なしのレイアウト実行ヘルパを実装する (→ Requirement: 待機なしのレイアウト実行)
- [x] 1.4 負の検証用の意図明示固定待機を実装し、定義箇所に意図の説明と `cross/ADR-0027` 参照のコメントを残す (→ Requirement: 負の検証のための意図明示の固定待機)
- [x] 1.5 ヘルパ自体のテストを書く: 条件成立で早期 return / **遅延して成立する述語が deadline 内で成功する (旧固定待機の flaky を決定的に再現する回帰検証)** / deadline 超過で fail しメッセージに実測値が載る / レイアウト実行が時間待機しない (→ Requirement: 条件ベース待機、待機なしのレイアウト実行)

## 2. 呼び出し箇所の置換 (ターゲット別)

置換手順: triage.md の該当行の「待つ遷移」を述語に落とす。述語は**操作前には成立せず、非同期反映後に初めて成立する遷移証拠**に限る (更新前から真の不変条件を述語にしない — 例: `FullSnapshotContentRefreshTests.swift:131` は Cell identity ではなくタイトル反映を待つ)。不変条件は待機後の assert として残す。setup ヘルパ内の待機 (`KsBridgeTestHost.attach` / `InputCellsTests` の setup) は初期反映の完了述語を待つ形にする。頻出述語は薄い便利関数に寄せてよい。

- [x] 2.1 `KsSettingsViewBridgeTests` の置換: A 50 / B 2 / C 12 (→ Requirement: 収束待ちの全数条件ベース化)
- [x] 2.2 `KsSettingsViewUITests` の置換: A 99 / B 13 / C 18。`pumpEntry` 2 箇所と直接の `RunLoop.current.run` 2 箇所を含む (→ Requirement: 収束待ちの全数条件ベース化)
- [x] 2.3 `KsSettingsViewSwiftUITests` の置換: A 11 / B 1 / C 0 (→ Requirement: 収束待ちの全数条件ベース化)
- [x] 2.4 複合ケース `KsBridgeOperationContractTests.swift:457` (同一 call site が更新ケースと no-op ケースの両方を通る) は、呼び出し側で分岐させて A/C を使い分ける
- [x] 2.5 `KsBridgeUpdateTests.swift:319` (待機の目的が不明確) は待機の要否自体を判断し、不要なら撤去・必要なら分類に従って置換する。判断結果を deviation.md に記録する

## 3. 旧固定待機の撤去

- [x] 3.1 triage.md「撤去する定義」の 20 定義 (旧 `pump` 19 + `pumpEntry` 1) をすべて削除し、`ios/Tests/` 配下に意図明示の固定待機以外の `RunLoop.current.run(until:)` パターンが残っていないことを grep で確認する (→ Requirement: 収束待ちの全数条件ベース化)

## 4. handbook の追記

- [x] 4.1 `kasane/handbook/cross/test-execution.md` 「収束を待つアサーション」節に負の検証の例外 (cross/ADR-0027 参照) を追記し、iOS 節の適用実例の記述を実装後の状態に更新する

## 5. 完了判定と証跡

- [x] 5.1 修正前の flaky の証跡を `evidence/` に記録する: CI run 33356260591 (macos-26、同一 commit で 1 回目失敗・2 回目成功) の失敗内容の記録 (→ Scenario: flaky が観測されたテストの安定化)
- [x] 5.2 全件 Simulator 実行 (`xcodebuild test -scheme KsSettingsView-Package`) が通ることを実行件数付き (`Executed N tests, with M failures`) で確認し、記録を `evidence/` に残す (→ Requirement: 収束待ちの全数条件ベース化)
- [x] 5.3 flaky が観測された `KsBridgeCustomCellTests` を反復実行 (`-only-testing` で 10 回程度) し、安定して通ることを確認して記録を `evidence/` に残す。決定的な回帰検証は 1.5 の遅延成立述語テストが担い、反復は補助確認とする (→ Scenario: flaky が観測されたテストの安定化)
