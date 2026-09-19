# レビュー結果: ios-swiftui-representable-safe-area (002 回目)

**日付**: 2026-09-19
**判定**: APPROVED

## サマリー

review-001 の Minor 2 件はいずれも解消した。Minor-1 に対する `RootSafeAreaLayoutTests` は「保持値を読むだけ」だった前回の弱点を正面から塞いでおり、`body` の `.ignoresSafeArea(.container, edges:)` を外すと 7 件中 4 件が落ちる構造になっている (下記の (a) 参照)。期待値は実行機の実測 `safeAreaInsets` から導出されており、iPhone 17 Pro とセーフエリアの異なる iPhone 16e の 2 機種で緑になることを実測して機種非依存を裏付けた。`ios/Sources` は前回から無変更 (diff の内容が一致することを確認済み)。新規の指摘は Suggestion 3 件のみで、いずれも修正サイクルを要しない。

## 前回指摘の再確認

| 前回の指摘 | 対応 | 判定 |
|---|---|---|
| 🟡 Minor-1 既定の全面配置に自動回帰テストが無い | `ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaLayoutTests.swift` (新規 7 件) | **解消** |
| 🟡 Minor-2 Store / DSL 同配置 Scenario の証跡が spec の GIVEN と噛み合っていない | `ui/brief.md`「証跡の範囲についての注記」の先頭に 1 行追記 | **解消** |
| 🔵 Suggestion 3 件 (opt-out A/B の OS 範囲 / Root Footer の青帯 / 逆流検査の不能) | 今回は対応しない方針 (指揮側の指示) | 判定に含めない |

## 確認観点への回答

### (a) 追加テストは `body` の `.ignoresSafeArea` 適用行に依存しているか — **依存している**

読み取り専用の制約によりミューテーションは行わず、アサーションの内容から判定した。`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:97` を削除した場合に何が起きるかを 1 件ずつ辿ると:

| テスト | 97 行目を削除したとき | 検出 |
|---|---|---|
| `test_既定ではStore方式の一覧がセーフエリアの外まで広がる` | 一覧がセーフエリアの内側に置かれ `listFrameInHost` がホスト全面と一致しなくなる | **落ちる** |
| `test_既定ではDSL方式の一覧がセーフエリアの外まで広がる` | 同上 | **落ちる** |
| `test_既定ではbarに覆われる領域のinsetがUIKit側へ渡る` | 一覧がセーフエリアの内側に置かれると `listView.safeAreaInsets` が 0 になり、ホストの実測 inset と一致しなくなる | **落ちる** |
| `test_respectsSafeAreaにfalseを渡すと既定の全面配置になる` | 全面配置に戻らない | **落ちる** |
| `test_既定ではStore方式とDSL方式の配置が一致する` | 両方式が等しく内側へ縮むため一致は保たれる | 通る |
| `test_respectsSafeAreaを付けると一覧がセーフエリアの内側に縮む` | 期待値と同じ内側配置になる | 通る |
| `test_respectsSafeAreaは他のRootModifierと連鎖しても配置に効く` | 同上 | 通る |

7 件中 4 件が落ちる。加えて、`edges` の指定ミス (`.all` → `.bottom` 等) では既定系 3 件が `origin.y` の不一致で落ち、三項演算子の真偽を取り違えた場合も既定系が落ちる。前回「97 行目を消しても全件 green」だった状態は解消しており、回帰検出力があると判断する。

`listFrameInHost` と `listSafeAreaInsets` という 2 つの角度から測っている点も良い。前者だけだと配置は合っているが inset が UIKit 側へ渡っていないケースを見逃すが、後者がそこを押さえている。

### (b) 期待値の導出は機種依存しない形か — **機種依存しない**

- 固定値はホスト寸法 `375x600` と上乗せ inset `top 44 / bottom 34` の 2 つだけで、どちらもテストが自分で与える値 (`host.view.frame` を明示設定、`additionalSafeAreaInsets` は実行機の値への**加算**)。実行機のセーフエリアの実値を前提にしていない。
- 期待値は `hosted.hostSafeAreaInsets` (実測) から `hostSize - insets.left - insets.right` の形で導出しており、`RootSafeAreaLayoutTests.swift:11-12` のコメントもその意図を明示している。
- `test_既定ではbarに覆われる領域のinsetがUIKit側へ渡る:82-83` に「ホストに上下のセーフエリアが与えられている」という前提アサーションがあり、全辺 0 の環境で検証が空振りすることを防いでいる (`handbook/cross/test-execution.md`「絞り込んだ結果が空でないこと」に対応する形)。
- **実測での裏取り**: 指定環境の iPhone 17 Pro に加え、セーフエリアの構成が異なる iPhone 16e / iOS 26.1 でも `KsSettingsViewSwiftUITests` を実行し、112 件すべて緑・`RootSafeAreaLayoutTests` passed を確認した。机上の判断だけでなく別機種でも成立している。

待機の作法についても問題なし。`host()` は `layoutIfNeeded()` でレイアウトを確定させるだけで、固定秒数の待機を持ち込んでいない (`handbook/cross/test-execution.md` の「レイアウトの確定だけが要る → 待機なしのレイアウト実行」に合致)。

### (c) テストの comment-policy 適合 — **適合**

- `python3 scripts/comment-policy-lint.py` を全体に実行し、検査対象 813 ファイル (新規テスト 1 件が加わって前回の 812 から +1) に対して **禁止 0 件**。`--advisory` でも `RootSafeAreaLayoutTests.swift` に報告は無い。
- 検査が及ばない範囲を規約本文から自分で判定した: ファイル冒頭と各テスト内のコメントは、コード識別子 (`_respectsSafeArea` / `body` / `.ignoresSafeArea(.container, edges:)` / `UIHostingController`) だけを参照して現在形で書かれており、作業文書のパス・change 名・レビュー通番 (`review-001` 等)・デルタスペック構文キーワード・履歴記述はいずれも含まれていない。公開メンバーを持たないテストファイルのため公開 doc コメントの規律は非該当。

### (d) SwiftUI テストターゲットの実行 (iPhone 17 Pro / iOS 26.1) — **green**

完了判定は絞り込みなしの全件実行で行った (`handbook/cross/test-execution.md`)。

| 実行 | 結果 |
|---|---|
| iPhone 17 Pro / iOS 26.1、全件 | **1,102 tests / 0 failures** (Bridge 173 / Core 88 / **SwiftUI 112** / TestSupport 7 / UI 722)。`RootSafeAreaLayoutTests` passed |
| iPhone 16e / iOS 26.1、`-only-testing:KsSettingsViewSwiftUITests` (機種非依存の裏取り) | **112 tests / 0 failures**。`RootSafeAreaLayoutTests` passed |

SwiftUI ターゲットは前回の 105 件から 112 件へ +7 で、tasks 1.3 に追記された「`RootSafeAreaLayoutTests` 7 件」と一致する。

## 指摘事項

### [🔵 Suggestion] keyboard 領域を無視しないこと (`.container` 限定) は依然として自動テストの外

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:97` / `ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaLayoutTests.swift`

**問題点**: 今回の追加で「全面に広がること」と「opt-out で内側に縮むこと」は自動化されたが、`.container` を `.all` に取り違えた場合 (keyboard 領域まで無視して EntryCell が隠れる) を落とすテストは無い。回帰検出は `ui/verification/ios26-inputcells-keyboard.png` の証跡だけが担っている。ADR-0006 が却下案として明示している経路でもあるため、取り違えの帰結は小さくない。

**推奨修正**: 今回の対応範囲 (Minor-1 は既定配置の回帰保護) を超えるため、このサイクルでの修正は不要。蒸留時の申し送り、または keyboard 回避の経路に手を入れる将来の change の tasks に入れるのが妥当。

### [🔵 Suggestion] CGRect の厳密一致比較と、opt-out 系テストの前提アサーション

**該当箇所**: `ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaLayoutTests.swift:92-101` / `:107-111` / `:123-127`

**問題点**: 2 点ある。(1) `XCTAssertEqual` による `CGRect` の比較は厳密な浮動小数一致で、SwiftUI 側が画素グリッドへ丸める場合と、期待値側の単純な引き算とで差が出る余地が原理的にある。(2) `test_既定ではbarに覆われる領域のinsetがUIKit側へ渡る` にはある「ホストのセーフエリアが 0 でない」前提アサーションが、opt-out 系の 3 件には無い。仮に全辺 0 の環境に当たると、期待値がホスト全面と同じ形になり、既定と opt-out を区別せずに通ってしまう。

いずれも実害の証拠は無い — `additionalSafeAreaInsets` をテスト自身が与えているため insets が 0 になる経路は実質的に塞がれており、丸めについてもセーフエリアの異なる 2 機種で緑を実測した。

**推奨修正**: 必須ではない。将来 landscape や別スケールの機種を対象に加えるときに、前提アサーションを opt-out 系にも 1 行ずつ足し、必要なら `accuracy` 付きの比較に切り替えると自己完結する。

### [🔵 Suggestion] `host()` が `self.window` を上書きするため、1 テスト内で複数回呼ぶと先の window が tearDown の対象から外れる

**該当箇所**: `ios/Tests/KsSettingsViewSwiftUITests/RootSafeAreaLayoutTests.swift:158-161` / `:32-36`

**問題点**: `test_既定ではStore方式とDSL方式の配置が一致する` は `host()` を 2 回呼び、2 回目の代入で 1 つ目の window が `self.window` から外れる。実測値は各 `host()` 呼び出しの内側で取り終えているため結果は正しく、外れた window は参照が切れて解放されるので実害は無い。ただし tearDown の「hidden にしてから捨てる」意図は 1 つ目に適用されない。

**推奨修正**: 必須ではない。window を配列で保持して tearDown で全件 hidden にすれば意図どおりになる。今回の 7 件では挙動に影響しない。

## アクションプラン

修正サイクルを要する項目は無い。Suggestion 3 件はいずれも「将来の change / 蒸留時の申し送り」に回してよく、見送っても APPROVED の判定は変わらない。

---

# 一致検証 (ksn-verify 兼務・差分確認)

**判定**: VALID (review-001 の verify 結果を維持)

デルタスペック (`specs/settings-view-ios-swiftui` / `specs/samples-ios`) と proposal・deviation は今回変更されておらず、`ios/Sources` も無変更のため、review-001 の対応表はそのまま有効。今回の追加による対応表の更新点だけを記す。

| Requirement / Scenario | 追加された担保 | 状態の変化 |
|---|---|---|
| NavigationStack の中身として全画面で使うと一覧が画面下端まで描かれる | `RootSafeAreaLayoutTests.test_既定ではStore方式の一覧がセーフエリアの外まで広がる` / `test_既定ではDSL方式の一覧がセーフエリアの外まで広がる` / `test_既定ではbarに覆われる領域のinsetがUIKit側へ渡る` | ✅ 一致 (証跡のみ → 証跡 + 自動テスト) |
| Store 方式と DSL 方式で配置が同じ | `RootSafeAreaLayoutTests.test_既定ではStore方式とDSL方式の配置が一致する` (同一ホスト条件での frame 比較)、および `ui/brief.md` への証跡範囲の明記 | ✅ 一致 (証跡の GIVEN 不一致は brief の注記で解消) |
| modifier を付けると従来どおりセーフエリアの内側に収まる | `RootSafeAreaLayoutTests.test_respectsSafeAreaを付けると一覧がセーフエリアの内側に縮む` | ✅ 一致 (証跡のみ → 証跡 + 自動テスト) |
| false を渡すと既定の全面配置になる | `RootSafeAreaLayoutTests.test_respectsSafeAreaにfalseを渡すと既定の全面配置になる` (値だけでなく配置を実測) | ✅ 一致 |
| 他の Root modifier と連鎖しても互いの値を失わない | `RootSafeAreaLayoutTests.test_respectsSafeAreaは他のRootModifierと連鎖しても配置に効く` (値の保持だけでなく配置に効くことまで) | ✅ 一致 |
| キーボード表示中は入力 Cell がキーボードに隠れない | 追加なし (証跡 `ui/verification/ios26-inputcells-keyboard.png` のまま) | ✅ 一致 (上記 Suggestion に記録) |

追加検査:

- **tasks.md の虚偽チェック**: 1.3 に追記された「`RootSafeAreaModifierTests` 7 件 / `RootSafeAreaLayoutTests` 7 件」は、両ファイルのテストメソッド数と、SwiftUI ターゲットの実行件数が 105 → 112 に増えた事実の双方と一致する。虚偽は無い。他のタスクの記述に変更は無い。
- **未記録乖離**: 新規テストは tasks 1.3 が名指しした `ios/Tests/KsSettingsViewSwiftUITests` の内側に収まるため、deviation への追記は不要 (`lessons/process.md` L-009 の事後判定を満たす)。`deviation.md` が前回から無変更であることは正しい。
- **足場の逆流**: 今回書き換えられた足場は `tasks.md` (1.3 のテスト名追記) と `ui/brief.md` (証跡範囲の注記) の 2 か所で、どちらも実施済みの事実の記録であり、要求 (Requirement / Scenario) の書き換えではない。`specs/` と `proposal.md` に変更は無い。
- **テスト全件成功**: iPhone 17 Pro / iOS 26.1 で 1,102 tests / 0 failures を実行して確認。
