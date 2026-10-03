# レビュー結果: align-toolchain-xcode27-jdk21-dotnet-10-0-401 (002 回目)

**日付**: 2026-10-04
**判定**: APPROVED

## サマリー

前回の Major (iOS 27.0 の Simulator で日付ホイールの Done のテストが 1 件失敗) は解消した。絞り込みなしの全件実行は 1201 tests / 0 failures。直したテストは、本体の通知処理を外すミューテーションで打ち切り (1 秒) を待たずに落ちることを実測し、配線の検証として成立している。`ios/Sources/` に差分は無く、一時ファイルの残存も無い。優先度の低い Minor 1 件と Suggestion 1 件を残す。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき) — 「収束を待つアサーション」「何を待機条件に選ぶか」「iOS の実行方法」の各節
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合を調査・修正し、完了を判定するとき)
- `kasane/decisions/cross/0027-negative-verification-fixed-wait-exception.md` (負の検証の固定待機)
- `kasane/lessons/code-review.md` (L-001: 検出力はミューテーションで実測する)、`kasane/lessons/test.md` (L-001 / L-002)
- `kasane/handbook/ios/swift6-language-mode-check.md` は `ios/Sources/` に差分が無いため適用外

ロードしたスキル: ksn-review、swift-ui-impl-skill

## 実行した検証 (2026-10-04、Xcode 27.0 (27A266a) / iPhone 18 Pro・iOS 27.0)

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS 全件 | `ios/` で `xcodebuild test -scheme KsSettingsView` (絞り込みなし) | **1201 tests / 0 failures**。バンドル別: UITests 786 / TestSupportTests 7 / SwiftUITests 131 / CoreTests 88 / BridgeTests 189 |
| ミューテーション A | `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift` の `handleKeyboardDidHide()` の中身を空にして対象テスト 1 件を実行 | **落ちる** (3 アサーション)。保留の前提アサーション (`InputCellsTests.swift:1572`) は通過し、通知を出した後の `:1580` (`events` が `changed` のみ)・`:1585`・`:1590` が失敗。実行は約 12 秒で、打ち切り待ちに頼っていない |
| ミューテーション C | 同ファイルの `handleWheelsDone()` で、入力面が出ていても即座に `isHideReported = true` にする (非表示完了を待たない) | 対象テストは**通る**。UITests バンドル全体 (786 件) でも失敗 0 (指摘 1) |
| 原状復帰 | 改変前のバックアップへ戻して shasum を照合 | 改変前後で一致。`git status` の `ios/` は `InputCellsTests.swift` のみ |
| lint | `scripts/comment-policy-lint.py` / `scripts/local-path-lint.py` | 禁止 0 件 / 通過 |

失敗する実行には `-collect-test-diagnostics never` を付け、診断収集の待ちを避けた。

### 件数の食い違い (1206 と 1201)

正しいのは **1201 件** (実装側の報告と一致)。数え方は前回と同じ (バンドル集計行の合算) で、差は UITests バンドルの 791 → 786 の 5 件だけ。他の 4 バンドルは前回と同数。

- `InputCellsTests.swift` の `func test` の数は HEAD と作業ツリーで同じ (106)。今回の diff はテストを増減させていない
- 前回のレビュー中は、作業ツリーに計測用の一時変更 (`ios/Sources/` と同テストファイル) が入っていた (`review-001.md` の Major の補足)。`evidence/ios27-keyboard-hide-notification.txt` は計測用の一時テストを調査後に取り除いたと記し、計測の区切りは 5 つ (日付ホイール 4 回 + 素の UITextField)
- したがって前回の 1206 は、計測用の一時テストが混ざった時点の値と推定する (当時の作業ツリーは再現できないため、5 件の内訳は実測ではなく推定)

`deviation.md` の「iOS 1201 件」は現状の実測と一致する。

## 確認した観点

- 仕様充足: 完了条件「iOS Native のテストが Xcode 27.0 / iOS 27.0 の Simulator で全件通る」を実測で充足。テスト修正を本変更に含めることは `deviation.md` に記録済み (オーナー指示) で、合意済みの差分
- 無断の逸脱: `ios/Sources/` に差分なし。`ios/` の差分は `InputCellsTests.swift` の 1 か所 (待機の置き換え) のみ
- 打ち切り経路で偶然通らないか: Done から最後の `events` の判定までに RunLoop を回す箇所が無く、打ち切りの Task (MainActor、1 秒) は走り得ない。末尾の負の検証の待機は 0.2 秒で、その前に 2 件の到着を判定済み。ミューテーション A で実測
- 環境で分岐する書き方: 3 通りの環境 (報告が `resignFirstResponder()` の中で届く / アニメーション後に届く / 届かない) のどれでも、配線が外れていれば「待ち受けが残る → テストが通知を出しても閉じ切りが届かない」で落ちる。分岐の条件 `_isAwaitingWheelsHide` は本体が決める値で、framework の都合で切り替わる値の代理ではない (`test-execution.md`「何を待機条件に選ぶか」)。分岐を通らない環境で検証が空になる点は指摘 1
- テスト実行規約: 旧い条件ベース待機 (UIKit の通知到着を deadline 0.5 秒で待つ) は、通知の時期を UIKit に預ける代理だった。置き換え後は待機そのものが無く、通知をテスト自身が出して経路を確定させている (「代理をやめたときに通らなくなる経路」の節の要求どおり、ミューテーションで検出力を実測)。二重発火の確認は名前で判別できる負の検証の待機 (cross/ADR-0027)
- 実行時挙動の検証規約: 本体は変えておらず、原因がテストランナー側 (通知が 1 件も届かない) にあることを計測で裏取りし、実アプリでは Done から約 0.43 秒で非表示完了が届くことを `evidence/` に残している。コード読解だけの断定ではない
- コメント規約: 追加コメントは現在形の仕様説明で、作業文書・通番・履歴への参照なし。コード識別子の参照のみ
- テストの手抜き: 言い訳コメントでの実質スキップなし。順序 (値 callback が先)・日付の一致・待ち受けの消費・二重発火なしの各アサーションは残っている
- 一時ファイルの残存: 未追跡は本 change のディレクトリのみ。計測用のテスト・ログは作業ツリーに無い
- 成果物のローカル絶対パス: `evidence/` と `deviation.md` に無し (lint 通過)
- Swift の観点 (swift-ui-impl-skill): テストコードの差分に強制アンラップの追加・並行性の問題なし

## 指摘事項

### [🟡 Minor] 「非表示完了を待つ」こと自体は、どのテストも検出しない (優先度: 低)

**該当箇所**: `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1571`

**問題点**: テスト名は「入力面の非表示完了を待って閉じ切り callback を届ける」だが、保留の確認 (`:1572`) は `if view._isAwaitingWheelsHide` の中にある。本体が非表示完了を待たずに閉じ切りを届けるよう壊れた場合 (ミューテーション C)、待ち受けは残らないので分岐に入らず、後続のアサーションはすべて通る。UITests 全体でも失敗 0 だった。

変更前のテスト (0.5 秒以内に 2 件届くことを待つ) も同じ改変を検出できないため、今回の修正による後退ではない。合意スコープ (iOS 27.0 で落ちるテストの修正) も満たしている。ただし「待つ」ことが製品挙動の中心で、いまの環境 (手元も CI も iOS 27.0 のテストランナー) では必ず分岐を通るため、条件を締める余地がある。

**推奨修正**: 分岐に入らなかったときの根拠をテストが自分で持つ。Done の前にテスト側でも `UIResponder.keyboardDidHideNotification` を購読して到着を記録し、待ち受けが残っていない場合は「UIKit の報告が Done の中で届いていた」ことをアサートする。

```swift
// before
if view._isAwaitingWheelsHide { /* 保留を確かめて通知を出す */ }

// after (概形)
if view._isAwaitingWheelsHide { /* 同上 */ } else {
    XCTAssertTrue(didHideObservedDuringDone, "待ち受けが残っていないのは、非表示完了がすでに報告されたときだけ")
}
```

本変更で直すか、別の change に送るかは実装側の判断でよい。

### [🔵 Suggestion] 完了条件のうち消費者検証 3 本と再検証表の結果が成果物から読み取れない (前回 Minor の持ち越し)

**該当箇所**: `deviation.md`

**問題点**: 前回の Minor (消費者検証 3 本と「SDK 更新時に再検証する箇所」の表の実施結果が成果物に無い) は、今回のパッケージと `deviation.md` の追加 2 行でも示されていない。今回のレビュー対象外のため再実行はしていない。

**推奨修正**: 実施済みなら結果を `evidence/` か完了報告に残す。

## アクションプラン

1. (任意) 指摘 1 の分岐外の根拠を足すか、別 change として積むかを決める
2. (任意) 消費者検証 3 本と再検証表の結果を成果物に残す
3. push 後、完了条件どおり `xcode-27` イメージでの Xcode 選択と CI の本体検証 3 本を確認する
