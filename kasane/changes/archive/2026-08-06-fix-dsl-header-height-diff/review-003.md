# レビュー結果: fix-dsl-header-height-diff (003 回目)

**日付**: 2026-08-05
**判定**: APPROVED
**範囲**: 修正サイクル 2 周目の確認 — 確定 Minor (新規 Store テスト 3 件の controller 生存保証) への対応のみ。フルレビューは実施していない (review-002 / second-opinion-003 / verify-002 の判定を前提とする)。

## サマリー

確定 Minor (Store 購読を張った controller の生存が ARC の保証外だった) は解消されている。second-opinion-003 は `window.rootViewController` への設定「または」`withExtendedLifetime` のいずれかを求めていたが、実装は**両方**を入れており、UIKit の所有関係と Swift の生存延長の二重で保証されている。既存の frame 観測テストは新ヘルパを一切使っておらず前提は崩れていない。`xcodebuild test` 全 602 件が 0 failures で成功し、`MemoryLeakTests` も green。ドキュメントコメントは実装と一致し、コメント規約 lint は当該ファイル 0 件、一時コードの残存もない。

## 確認結果

### 1. controller の生存が実際に保証される形になったか — 保証されている

該当箇所: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:403`-`:418` (ヘルパ)、`:556`-`:562` / `:598`-`:604` / `:641`-`:647` (各テストの `defer`)

二重の保証が入っており、それぞれ独立に機能する。

**(a) `window.rootViewController = controller` (`:409`)**
`UIWindow` は `rootViewController` を強参照する。`window` は戻り値タプルで局所束縛され、`defer` 内の `window.isHidden = true` で**スコープ終端が最後の使用点**になるため、`window` はスコープを抜けるまで生存する。したがって controller も推移的に保持される。この経路だけで既に十分。

**(b) `defer { ... withExtendedLifetime(controller) {} }`**
懸念として挙がっていた「`defer` 内の `withExtendedLifetime` は最後の assert より後まで生存を延ばせているか」は、**延ばせている**。理由は 2 点:

- `defer` の本体はクロージャではなくスコープ終端にインラインで実行されるコードであり、そこにある `controller` の参照は**スコープ終端という位置での使用**として扱われる。ARC は最後の使用点より前に強参照を解放できないため、controller の生存は全 assert より後ろまで伸びる。
- `withExtendedLifetime` はまさにその「使用を最適化で消させない」ための標準プリミティブ (`Builtin.fixLifetime` に落ちる) であり、値が触られていないように見える状況で生存を固定する用途そのもの。

加えて 3 テストは `throws` + `try XCTUnwrap` を使うが、`defer` は throw による巻き戻し時も実行されるため、異常系でも生存保証は途切れない。

補足: lessons `code-review` L-001 に従い、ガードを外したミューテーションプローブで検出力を実測しようとしたが、環境の権限classifierに書き込みを拒否され、かつ本タスクの制約が「コードを修正しない」であるため実施していない。上記の結論は言語 / UIKit の保証と全件 green に基づく静的判断である (プローブがあれば「ガードが load-bearing か」まで言えたが、正しさの判定には影響しない)。

### 2. 既存の frame 観測テストの前提が崩れていないか — 崩れていない

`window.rootViewController` + `makeKeyAndVisible()` は**新設ヘルパ `hostStoreConnectedControllerInWindow` の内部だけ**に閉じている。既存ヘルパ `hostControllerInWindow` (`:379`-`:394`、`window.addSubview(rootView)` + 明示 frame + `layoutIfNeeded()`) は diff 上まったく変更されていない。

ヘルパ利用の対応:

| 行 | 使用ヘルパ |
| --- | --- |
| `:447` / `:472` / `:497` / `:684` | `hostControllerInWindow` (既存・無変更) |
| `:555` / `:597` / `:640` | `hostStoreConnectedControllerInWindow` (新設) |

したがって safe area の適用や window autoresizing 経由のサイズ決定に変わったのは新規 3 件のみで、既存テストの観測経路は 1 行も変わっていない。新規 3 件自体も `accuracy: 0.5` で 40 / 90 をピンポイントに検証して通っており、safe area によるずれは実測で発生していない。実行結果でも `SectionAccessoryRenderingTests` を含む全 suite が passed。

### 3. ヘルパのドキュメントコメントと実装の一致 — 一致している

`:396`-`:402` の記述をコード側で照合した。

- 「Store 購読の `AnyCancellable` は controller 自身が所有し、購読は `[weak self]` で張られる」→ `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:121` (`private var storeSubscription: AnyCancellable?`)、`:286`-`:289` (`connectStore` 内の `.sink { [weak self] diff in ... }`) と一致。
- 「`window.rootViewController` に設定して window が controller を強参照する所有関係を作る」→ `:409` と一致。
- 「呼び出し側も戻り値の controller をテスト終了まで保持する」→ 3 テストの `defer` 内 `withExtendedLifetime(controller) {}` と一致。
- 「更新は Store の公開操作から Publisher 経由で controller へ届く経路を通る」→ `KsSettingsViewController(store:)` → `connectStore(store)` → `store.diffPublisher` の実経路と一致 (代理経路ではない)。

### 4. MemoryLeakTests — green、不変条件は無傷

`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift` はこの変更で**編集されていない** (git status に上がっていない)。実行結果でも suite passed、`test_Store経由でもControllerがdeinitされStore購読が解除される` が 0.004 秒で passed。

不変条件が壊れていない理由も構造的に確認した: `MemoryLeakTests` は `autoreleasepool` 内で window に載せずに controller を生成しており、今回追加した `rootViewController` 経由の所有関係とは無関係。新ヘルパは `SectionAccessoryRenderingTests` の private であり、他 suite へ波及しない。

### 5. コメント規約 lint / 一時コードの残存 — 問題なし

- `python3 scripts/comment-policy-lint.py ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift` → `合計: 0 ファイル / 禁止 0 件 (検査対象 1 ファイル)`、exit 0。
  - なお同ファイルの diff では `Phase 18（revert）` `review-result_002 Major-1` 等の履歴 ID 依存コメントが単独で読める説明へ書き換えられており、規約方向として正しい。
  - リポジトリ全体では 720 件の既存違反が残るが (`ios/Tests/KsSettingsViewUITests/_Bootstrap.swift:6` 等)、いずれも本変更の diff 外の既存負債であり本レビューの対象外。
- 一時的なミューテーション用コードの残存なし。変更ファイル (`SectionAccessoryRenderingTests.swift` / `DSLDiffCalculator.swift`) に `TODO` / `FIXME` / `XCTSkip` / `XCTExpectFailure` / `debugPrint` / 「一時的」「暫定」のいずれもヒットしない。diff 全体を目視しても、コメントアウトされた assert・条件付きスキップ・デバッグ出力は含まれていない。

## 実行したテスト

```
cd ios && xcodebuild test -scheme KsSettingsView-Package \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
```

結果 (実出力より):

```
** TEST SUCCEEDED **

Test Suite 'KsSettingsViewBridgeTests.xctest' passed
  Executed  28 tests, with 0 failures (0 unexpected) in 6.376 (6.400) seconds
Test Suite 'KsSettingsViewCoreTests.xctest' passed
  Executed  83 tests, with 0 failures (0 unexpected) in 1.259 (2.289) seconds
Test Suite 'KsSettingsViewSwiftUITests.xctest' passed
  Executed  76 tests, with 0 failures (0 unexpected) in 0.216 (0.299) seconds
Test Suite 'KsSettingsViewUITests.xctest' passed
  Executed 415 tests, with 0 failures (0 unexpected) in 5.740 (5.958) seconds
```

合計 **602 件 / 0 failures (0 unexpected)**。xcodebuild の exit code は 0。

対象 3 件はいずれも passed:

```
Test Case '-[...SectionAccessoryRenderingTests
  test_Store経由のreplaceSectionのheaderHeight変更が表示中headerの実高さに反映される]' passed (0.116 seconds).
Test Case '-[...SectionAccessoryRenderingTests
  test_Store経由のreplaceAllのheaderHeight変更が表示中headerの実高さに反映される]'      passed (0.116 seconds).
Test Case '-[...SectionAccessoryRenderingTests
  test_Store経由のfull直後のreplaceCellでheader高さとCell内容の両方が表示へ反映される]' passed (0.116 seconds).
Test Case '-[...MemoryLeakTests
  test_Store経由でもControllerがdeinitされStore購読が解除される]'                      passed (0.004 seconds).
```

ログ中の `error:` 該当行は `CHHapticPattern` の `hapticpatternlibrary.plist` 不在および実機 (passcode protected) への接続失敗のみで、いずれもシミュレータ / 環境由来のノイズ。テスト失敗はゼロ。

## 指摘事項

Critical / Major / Minor: **なし**。

### [🔵 Suggestion] 2 つの window ホスティング方式が併存する理由が書かれていない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:379` / `:403`

**問題点**: 同一ファイル内に `hostControllerInWindow` (`window.addSubview(rootView)`) と `hostStoreConnectedControllerInWindow` (`window.rootViewController = controller`) が並ぶ。新設側のコメントは「なぜ rootViewController が必要か」を説明しているが、既存側には「こちらは Store 購読を持たないので addSubview で足りる」旨の対比が無い。将来「重複しているから統一しよう」と新設側を addSubview へ寄せる整理が入ると、Store 購読の生存保証が無音で失われる (今回直した問題がそのまま再発する)。

**推奨修正**: 既存 `hostControllerInWindow` の doc コメントに 1 行、「Store 購読を持たない controller 用。Store 接続版は生存保証のため `hostStoreConnectedControllerInWindow` を使う」といった対比を足す。優先度は低い。

### [🔵 Suggestion] 新ヘルパの明示 frame 代入が rootViewController 設定後で冗長

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:409`-`:413`

**問題点**: `window.rootViewController = controller` を設定した時点で UIKit が root view を window bounds へ追従させるため、直後の `rootView.frame = CGRect(origin: .zero, size: size)` は同値の再代入であり、かつ以降の layout pass で UIKit 側に上書きされうる。現在は window bounds と `size` が同じ 375x600 なので実害はないが、「明示 frame で観測サイズを固定している」という読みは正確ではなくなっている。

**推奨修正**: 冗長な `rootView.frame` 代入を落とすか、「window bounds と同値であることに依存している」旨を 1 行添える。優先度は低い。

### [🔵 Suggestion] 生存保証そのものを固定するアサーションは無い

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:556` / `:598` / `:641`

**問題点**: 今回入れた 2 つのガードは正しいが、ガードが外れたときに落ちるのは「更新が届かない」という間接的な失敗であり、原因が寿命だと分かりにくい。`MemoryLeakTests` が固定しているのは逆向き (解放されること) の不変条件なので、こちら向きは無防備。

**推奨修正**: 必須ではない。もし固定したいなら、更新適用の直前に `weak var` 経由で controller が生存していることを 1 行 assert する形が最小。優先度は低く、現状のまま archive しても問題ない。

## アクションプラン

1. **対応不要 (そのまま次工程へ)**: 確定 Minor は解消済み。Critical / Major / Minor は残っていない。
2. Suggestion 3 件はいずれも任意。特に 1 件目 (2 ヘルパの対比コメント) だけは、同じ問題の再発防止という意味で拾う価値がある。ただし本変更のスコープ外として別途扱っても差し支えない。
