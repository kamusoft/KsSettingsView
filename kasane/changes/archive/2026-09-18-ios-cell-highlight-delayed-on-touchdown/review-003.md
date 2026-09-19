# レビュー結果: ios-cell-highlight-delayed-on-touchdown (003 回目)

**日付**: 2026-09-18
**判定**: CHANGES_REQUESTED

## サマリー

前回 (002) の 5 件はすべて閉じている。テストの検出力強化は「予約が実在すること」を前提アサーションで押さえた形になっており構造として妥当で、`evidence.md` の件数 (1061) と担保点 (6 点) はこちらの実測 (1061 tests / 0 failures) と一致する。`viewWillAppear` 側の解除 Task を `pendingDeselectTask` に載せた変更にも、Task ハンドルの取り合いや戻り際の解除の退行は見つからなかった。

一方で、今回新たに 1 件の挙動欠陥を実測で確認した。`didSelectItemAt` が `isDisappearing = false` を**タップハンドラの呼び出しより後**に置いているため、ハンドラの中で同期的に遷移が始まる host (UIKit の `pushViewController`) では遷移の合図が上書きされ、この変更の主目的である「遷移中は押下色を残す」が成立しない。SwiftUI の `NavigationStack` は遷移が非同期なので実機確認では踏めておらず、公開型 `KsSettingsViewController` を直接 `UINavigationController` に載せる利用経路だけが漏れている。修正は 1 行の並べ替えで、既存 7 件のテストを壊さないことも実測済み。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 禁止 0 件。要確認 (advisory) は `ios/Sources/KsSettingsViewUI/CustomCellView.swift:114` と `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1947` の 2 件だが、いずれも HEAD に既存の行で本変更の追加分ではない (diff の触れていない領域であることを確認)
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/**` を触る変更の完了判定) — 自分で再確認。結果は下記「確認した観点」
- `kasane/handbook/cross/test-execution.md` (テスト実行・結果の報告) — Simulator 全件実行、バンドル集計行の合算で件数確認、収束待ち / 負の検証の書き分けを照合
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定) — 実機での確認と証跡が `evidence.md` にある。ただし host 種別の網羅に穴がある (下記 A-1)
- `kasane/lessons/code-review.md` (重点観点 L-001) — 挙動欠陥の有無が争点になったため、ミューテーション実測で原因と修正案の双方を確定させた (下記 A-1)

---

## 指摘事項

## A. 見た目・挙動が変わる指摘

### [🟠 Major] 同期的に遷移が始まる host では、遷移中に押下色が残らない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2543-2562`

**問題点**:

`didSelectItemAt` は次の順で動く。

1. `handler()` (Cell の `onTap`) を呼ぶ
2. `isDisappearing = false` を代入する
3. 猶予 Task を張り、`selectionHoldDuration` 後に `!self.isDisappearing` なら解除する

遷移が起きたかの合図は `viewWillDisappear` による `isDisappearing = true` だけである。ところが UIKit の `navigationController.pushViewController(_:animated:)` は、**呼び出しの中で同期的に** 提示元へ `viewWillDisappear` を配信する。つまり host が UIKit で、`onTap` の中で push する実装だと、1 で立った `isDisappearing = true` を 2 が無条件に false へ戻してしまい、3 の判定は「遷移していない」に倒れる。結果として押下色は猶予後にフェードアウトを始め、**この変更が直そうとした「遷移してから色が変わる」体感がそのまま残る**。

コード自身のコメントが「遷移が始まったかは同期的には分からない（SwiftUI の `NavigationStack` は path の変更を次の更新で反映する）」と書いているとおり、猶予判定は**非同期に届く**合図のための仕組みである。同期的に届いた合図を捨てる必要はなく、2 は 1 より前に置けば足りる。

実測 (ミューテーション、原状復帰は shasum 一致で確認済み):

| 条件 | 検証内容 | 結果 |
|---|---|---|
| 現状のまま | `onTap` の中で `viewWillDisappear` が届く場合に、猶予経過後も選択が残るか | **failed** (選択が解除される) |
| `isDisappearing = false` を `handler()` の前へ移動 | 同上 | **passed** |
| 同上 | 既存の `CellTouchFeedbackTimingTests` 7 件 | **passed** (退行なし) |

この経路が実機確認で踏まれていない理由も確認した。`evidence.md` の push 遷移の確認は SwiftUI の Sample (`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift`) で行われており、SwiftUI では path の更新が次の更新サイクルで反映されるため `viewWillDisappear` が `handler()` の戻り後に届く。したがって現状のコードでも成立する。公開型 `KsSettingsViewController` を直接 `UINavigationController` に載せる UIKit host の経路だけが、証跡にもテストにも無い。

**推奨修正**:

1. `isDisappearing = false` を `handler()` の呼び出しより前へ移す。これで同期・非同期どちらの順序でも合図を取りこぼさない (SwiftUI 経路の挙動は変わらない)。
2. 回帰テストを 1 件足す。秒数に依存せず書ける — `onTap` の中から `controller.viewWillDisappear(true)` を呼ぶ Cell を用意し、`didSelectItemAt` の後に `waitForNegativeVerification(in:)` して選択が残ることを確かめる形で、上表のとおり現状では落ち、並べ替えで通ることを確認済み。既存の `test_タップで遷移が始まった場合は選択が残る` は `viewWillDisappear` を `didSelectItemAt` の**後**に呼んでいるため、この順序を検出できない。
3. あわせて `evidence.md` の push 遷移の確認行に、確認した host が SwiftUI であることを書き添える (UIKit host を実機で確認するなら、その結果も)。

## B. 見た目・挙動が変わらない指摘

**なし。** 前回 (002) の 5 件はすべて実態として閉じている (下記「確認した観点」)。

---

## 所見 (今回は指摘にしないもの)

上限サイクルのため、挙動を変える提案は Major 以上に限った。以下は次に触るときの材料として残す。

- **エッジスワイプの完了待ちが登録されないことがある**: `KsSettingsViewController.swift:466-482` は `returnDeselectDelay` の待ちが明けてから `coordinator.animate(alongsideTransition:completion:)` を登録する。素早いスワイプで待ちの明ける前に遷移が終わっていると `animate` は登録に失敗し、completion が呼ばれず選択が残る。次のタップや次の `viewWillAppear` の `deselectAllItems` で解消するため実害は小さい。
- **`viewWillAppear` の coordinator 無し経路だけ Task を張り直さない**: 同 `:484-486` の else 節は `deselectAllItems` を即時に呼ぶだけで `pendingDeselectTask` を cancel しない。即時解除なので実害は無いが、2 つの経路で Task の寿命の扱いが非対称のまま残っている。
- **`resetSelectedColor` は進行中のフェードを打ち切らない**: `KsCellViewSupport.swift:117-124` は状態だけを落とし、走行中の `UIView.animate` はそのまま完走する。前回 A-1 でオーナーが許容した「キャンセル時にフェードインが完了してから戻る」と同じ性質で、再利用経路にも同様に及ぶ。踏むには押下色のフェードイン中 (0.12 秒以内) に行が画面外へ出て再利用される必要があり、通常操作では成立しにくい。
- **長命層への反映**: `kasane/concepts/core/styling/cell-visual-states.md` と `kasane/concepts/ios/api/ios-native-host.md` は今回のタイミング契約を含まないが、既存の記述と矛盾はしない。蒸留の宿題として残る (001 と同じ所見)。`kasane/decisions/ios/0005-cell-press-feedback-library-controlled-timing.md` は `proposed` であり、これを根拠にした指摘は出していない。

---

## 確認した観点 (問題なしと判断したもの)

### 前回 (002) 指摘の閉じ確認

- **(1) 遅延中キャンセルのテストの検出力**: `ios/Tests/KsSettingsViewUITests/CellTouchFeedbackTimingTests.swift:156-161` で `isHighlighted = true` の直後に `cell.layoutIfNeeded()` が入り、`XCTAssertNotNil(pendingSelectedColorTask)` が**予約が実在すること**を前提アサーションとして押さえている。この前提が成立している (全件緑で確認) 以上、取り消しが外れれば予約は `selectedColorHighlightDelay` (0.1 秒) 後に発火し、`waitForNegativeVerification` の既定 0.2 秒の中で塗りが起きて背景色と `isShowingSelectedColor` の 2 アサーションが落ちる。`waitForNegativeVerification` は待機の前後で `layoutIfNeeded` を走らせるため、キャンセル側の構成更新も待機中に必ず届く
- **(1) 再利用テストの 2 段化**: `:190-235` が「表示中の再利用」と「予約段階の再利用」に分かれている。後段は `prepareForReuse()` の直後 (レイアウト前) に `XCTAssertNil(pendingSelectedColorTask)` を置いており、`configurationUpdateHandler` 側の取り消し経路に肩代わりされない位置にある。さらに負の待機の後に `applyRenderedBackgroundColor` の結果を見ているため、ハンドルだけ nil にして cancel を落とす類のミューテーションも捕まる
- **(2) `evidence.md` の更新**: 1061 tests / 担保 6 点に更新済み。6 点と実テスト 7 件の対応も取れている (タッチ遅延の 2 件と選択解除の 2 件がそれぞれ 1 点に対応)
- **(3) テスト冒頭の列挙**: `:6-11` が 5 項目になり、追加した 2 件を含む
- **(4) push / present のコメント**: `:267-268` が「push 遷移のように `viewWillDisappear` が届く遷移」に直っており、`summary.md` / `ios/ADR-0005` の A-2 確定記述と一致する
- **(5) `viewWillAppear` 側の Task 保持**: `:466-467` で `pendingDeselectTask?.cancel()` → 再代入。取り消された Task は `guard !Task.isCancelled` で `pendingDeselectTask = nil` に到達する前に抜けるため、後から張った Task のハンドルを nil で潰す経路は無い。逆向き (タップ側が戻り際の Task を潰す) も同じ形で安全。戻り際の解除自体は、タップが割り込まない限り従来どおり `returnDeselectDelay` 後に開始される。割り込んだ場合はタップ側の猶予 Task が `deselectAllItems` で全選択を解除するため、解除が取り残されることはない

### その他

- **テスト実行**: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` のバンドル集計行の合算で 170 + 88 + 98 + 7 + 698 = **1061 tests / 0 failures** (`** TEST SUCCEEDED **`)。`summary.md` / `evidence.md` の申告と一致する
- **Swift 6 言語モード**: `xcodebuild build -scheme KsSettingsView SWIFT_VERSION=6` で **BUILD SUCCEEDED / error 0 件** (`-swift-version 6` が source ターゲットの引数に入ることを確認)。`ios/Package.swift` に一時設定の残骸は無い (差分 0 件)
- **押下色の描画経路の単一性**: `backgroundConfiguration` への代入は `KsCellViewSupport` の 4 箇所 (render 適用 / フェードイン / フェードアウト / 即時復帰) と `KsSettingsViewController.swift:2374` のみ。後者は Header / Footer の accessory 用 list cell 専用で、選択・押下の対象ではないため `ios/ADR-0005` が前提とする「全 Cell 種の render が 1 経路を通る」は成立している。`applyCellBaseLayout` は 13 の Cell View すべてが呼び、`installSelectedColorHandler` は `KsListCellBase` と `CustomCellView` の 2 箇所で全 Cell 種を覆う
- **待機の書き方**: 追加分も収束待ちは `awaitCondition`、不変性の確認は `waitForNegativeVerification` で分かれており、呼び出し名から負の検証と判別できる。秒数そのものは固定していない
- **コメント規約**: `scripts/comment-policy-lint.py --advisory` で禁止 0 件。新規 2 ファイルに要確認の該当なし。`ios/ADR-0005` 参照はいずれも ADR を開かずに意味が通る自己完結した説明を伴い、内部型・private メンバに置かれている (公開 doc コメントへの混入なし)
- **deviation / スコープ**: `deviation.md` 記載の 2 件は合意済みの差分として扱い、指摘にしていない。Sample 側に一時変更の残骸が無いことは作業ツリーの状態で確認した
- **ミューテーション実測の原状復帰**: 触った `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` は backup と shasum 一致、probe 用に置いた一時テストファイルは削除済み。`git status` が着手時の一覧と完全に一致することを確認した

## アクションプラン

1. A-1 の 1 行 (`isDisappearing = false` を `handler()` の前へ) を直す
2. 同期的に遷移が届く順序の回帰テストを 1 件足す (秒数非依存で書ける。現状で落ちること・並べ替えで通ることは実測済み)
3. `evidence.md` の push 遷移の確認行に、確認した host が SwiftUI であることを書き添える
4. 「所見」の 4 件は対応不要。次にこの領域へ触るときの材料として残す
