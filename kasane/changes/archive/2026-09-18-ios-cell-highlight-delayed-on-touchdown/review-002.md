# レビュー結果: ios-cell-highlight-delayed-on-touchdown (002 回目)

**日付**: 2026-09-18
**判定**: CHANGES_REQUESTED

## サマリー

前回 A-1 / A-2 のオーナー判断にもとづく記述修正は、いずれもコードの実態と一致している (no-op だった 2 行は削除済み、遷移判定は `viewWillDisappear` 駆動のままで記述だけが実態へ寄った)。B-2 / B-3 の修正でも新しい競合・退行は見つからず、Swift 6 言語モードのビルドとテスト全件 (1061 tests / 0 failures) も通る。

一方で B-1 で足した 2 件のうち「立ち上がりの待ちの間にキャンセル」のテストは**検出力がゼロ**である。実装から取り消し処理を外しても、このテストは緑のまま通る (ミューテーションで実測)。加えて `evidence.md` の自動テストの件数と担保点が B-1 適用前のまま残っている。**見た目・挙動を変える必要のある指摘は無く**、直すのはテストと記録の 2 箇所だけになる。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 禁止参照 0 件。要確認 (advisory) の該当 2 件はいずれも HEAD に既存で、本変更が足したものではない
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/**` を触る変更の完了判定) — 一時設定ビルドで error 0 件を自分で再確認
- `kasane/handbook/cross/test-execution.md` (テスト実行・結果の報告) — Simulator 全件実行、バンドル集計行の合算で件数確認、収束待ち / 負の検証の書き分けを照合
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定) — 実機での再現・解消確認と証跡が `evidence.md` にある
- `kasane/lessons/code-review.md` (重点観点 L-001) — 新規テストの検出力が争点になったため、ミューテーション実測を適用した (下記 B-1)

---

## 指摘事項

## A. 見た目・挙動が変わる指摘

**なし。** 今回の確認範囲 (前回指摘の反映と、それによる新規の不具合) では、実機の見え方を変える必要のある問題は見つからなかった。A-1 / A-2 はオーナー判断で挙動据え置きとなり、記述の側が実態に合わせて直っていることを確認した (下記「確認した観点」)。

## B. 見た目・挙動が変わらない指摘 (テスト・記録・コメント)

### [🟡 Minor / 優先度 高] 「待ちの間にキャンセルされたら押下色は出ない」テストが何も検出しない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/CellTouchFeedbackTimingTests.swift:139-167`

**問題点**:
`cell.isHighlighted = true` の直後に `false` を代入しているが、`UICollectionViewCell` の押下状態の変化は `setNeedsUpdateConfiguration` 経由で**次のレイアウトまで遅延**するため、`configurationUpdateHandler` は押下 `true` の状態を一度も見ない。結果として塗り始めの予約 Task がそもそも作られず、テストは「予約が取り消されること」ではなく「最初から何も起きないこと」を確かめている。

実測 (ミューテーション、原状復帰は shasum 一致で確認済み):

| 入れたミューテーション | 対象テスト | 結果 |
|---|---|---|
| `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:148` の `pendingSelectedColorTask?.cancel()` を削除 | 立ち上がりの待ちの間にキャンセル | **passed** (検出できない) |
| 同上 + テスト側で `isHighlighted = true` の後に `cell.layoutIfNeeded()` を挟む | 同上 | **failed** (2 アサーションとも落ちる) |
| `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` の `prepareForReuse` から `resetSelectedColor` 呼び出しを削除 | 再利用でリセットされる | **failed** (検出できている) |

**推奨修正**:
`cell.isHighlighted = true` と `= false` の間に `cell.layoutIfNeeded()` を 1 行挟む (押下が Cell の構成更新まで届いてから指が滑り出す、という実際の順序にもなる)。上表のとおり、これで取り消し処理の削除を捕まえられることは実測済み。

あわせて 2 点:

- `:196-199` の `XCTAssertNil(KsCellViewSupport.state(cell).pendingSelectedColorTask)` も、フェードイン完了後は実装側が既に nil を入れているため常に成立する。同テストの他の 2 アサーション (`isShowingSelectedColor` / 再利用後の render 色) には検出力があることは実測済みなので、このアサーションは落としても、上の待ち中の状態で `prepareForReuse()` を呼ぶ形に変えても良い
- 「塗り始めの予約が残っている段階で再利用される」経路は、現状どのテストも通っていない (フェードイン後にしか `prepareForReuse` を呼んでいない)

### [🟡 Minor] `evidence.md` の自動テストの記述が B-1 適用前のまま

**該当箇所**: `evidence.md` の「自動テスト」節

**問題点**:
`1059 tests / 0 failures` と「4 点を担保する」のままで、B-1 で 2 件足した後の実態 (`summary.md` の申告どおり 1061 tests、担保するのは 6 点) と食い違う。私の実測も 1061 tests / 0 failures だった。証跡の文書が古い数字を持っていると、蒸留時にどちらが正か分からなくなる。

**推奨修正**: 件数を 1061 に、担保する項目を現在の 7 テストの内容 (遅延中のキャンセル / 再利用時のリセットを追加) に直す。

### [🔵 Suggestion] テストファイル冒頭の「検証すること」の列挙が 3 点のまま

**該当箇所**: `ios/Tests/KsSettingsViewUITests/CellTouchFeedbackTimingTests.swift:4-11`

**問題点**: 冒頭コメントが 3 点しか挙げておらず、追加した「待ちの間のキャンセル」「再利用時のリセット」を含まない。ファイル単独で読んだときに、テストの数と説明が合わない。

**推奨修正**: 2 点を列挙に足す。

### [🔵 Suggestion] 「push / present が起きた場合に相当する」コメントが実態と食い違う

**該当箇所**: `ios/Tests/KsSettingsViewUITests/CellTouchFeedbackTimingTests.swift:237`

**問題点**:
`summary.md` と `ios/ADR-0005` は、pageSheet の present では提示元に `viewWillDisappear` が届かないため「遷移しないタップと同じ扱い」と実態に合わせて直った。このコメントだけが `push / present` の並記のままで、直した記述と逆のことを言っている。

**推奨修正**: 「push 遷移のように `viewWillDisappear` が届く遷移が起きた場合に相当する」に直す。

### [🔵 Suggestion] `viewWillAppear` 側の解除 Task は保持・取り消しされない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:459-483` (`didSelectItemAt` 側は `:2552-2558`)

**問題点**:
B-3 でタップ側の解除 Task は 1 本化されたが、戻り際の解除 Task は保持されないまま作り捨てで、新しいタップでも取り消されない。画面が現れてから `returnDeselectDelay` (0.08 秒) の間にタップが入ると、戻り際の Task が新しいタップの押下色を消しうる。戻りの遷移アニメーション中に相当する窓であり、実操作では踏めない見込みなので実害は無いが、2 つの解除経路で寿命の扱いが非対称になっている。

**推奨修正**: 戻り際の Task も `pendingDeselectTask` に載せて、同じく張り直しで取り消す。現状のままで良いと判断するなら不要 (任意)。

---

## 確認した観点 (問題なしと判断したもの)

- **A-1 の記述と実態の一致**: `removeAllAnimations` は `ios/` 配下に 1 件も残っていない。`restoreNormalColor` の `fadesOut == false` 経路は `UIView.performWithoutAnimation` での差し替えのみで、doc コメントは「進行中のフェードインは完了してから戻ることがある」と実態どおりに書かれている。`summary.md` / `ios/ADR-0005` の記述も同じ内容で、コードと矛盾しない
- **A-2 の記述と実態の一致**: 遷移判定は `viewWillDisappear` 由来の `isDisappearing` のみで、pageSheet の提示では成立しない。`summary.md` の「シートを開くタップは遷移しないタップと同じく猶予後にフェードアウトする」と `ios/ADR-0005` の Decision (「遷移しないタップ (pageSheet で開く Picker 系のシート提示を含む) は猶予後に解除する」) は、この実装の実態と一致している
- **B-2 (init への集約)**: `delaysContentTouches` の出現は `ImmediateTouchCollectionView` の doc と init、テストの検査だけで、controller 側の重複設定は残っていない。`@available(*, unavailable) required init?(coder:)` はリポジトリ既存の `fatalError("init(coder:) is not supported")` と同等の作法で、`final` な internal 型かつプログラム生成専用のため coder 経由の生成経路は無い。テスト `test_collectionViewはタッチ遅延を持たない` は型と `delaysContentTouches` の両方を見ており、init から設定が落ちれば落ちる
- **B-3 (解除 Task の 1 本化) の競合**: 新しいタップは `cancel()` → 再代入の順で、取り消された Task は `guard !Task.isCancelled` で先に抜けるため、後から張った Task のハンドルを `nil` で潰す経路は無い。遷移が起きた場合は完了済み Task のハンドルが残るが、次のタップでの `cancel()` が no-op になるだけで副作用は無い。猶予の意味は「最後のタップから 0.1 秒」に揃った
- **押下色の状態機械**: `applyRenderedBackgroundColor` は押下色の表示中だけ `selectedColor` を、それ以外は実効背景色を入れる 1 経路で、`CellBaseLayout` / `CustomCellView` の render 両方がこれを通る。無効 Cell が押下色に入らない点、解除後に実効背景色へ戻る点は変更前の契約のまま
- **Swift 6 言語モード**: `xcodebuild build -scheme KsSettingsView SWIFT_VERSION=6` で **BUILD SUCCEEDED / error 0 件** (`-swift-version 6` が source ターゲットの引数に入ることを確認)。`ios/Package.swift` に一時設定の残骸は無い (差分 0 件)
- **テスト実行**: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` のバンドル集計行の合算で 170 + 88 + 98 + 7 + 698 = **1061 tests / 0 failures**。`summary.md` の申告と一致する (前回から +2 件は B-1 の追加分)
- **待機の書き方**: 追加された 2 件も、収束待ちは `awaitCondition`、不変性の確認は `waitForNegativeVerification` と用途で分かれている。負の検証の既定 0.2 秒は `selectedColorHighlightDelay` (0.1 秒) より長く、待ち時間の秒数自体は固定していない
- **コメント規約**: `scripts/comment-policy-lint.py --advisory` で禁止 0 件。`ios/Sources/KsSettingsViewUI/CustomCellView.swift:114` と `KsSettingsViewController.swift:1944` の要確認 2 件は HEAD に既存で、本変更の追加分ではない。新設した `ios/ADR-0005` 参照 3 箇所はいずれも ADR を開かなくても意味が通る自己完結した説明を伴い、内部型・private メンバに置かれている (公開 doc コメントへの混入なし)。ADR が `proposed` である点は、直前の変更でも proposed の ADR をコードから参照して蒸留で accepted へ昇格させた前例があるため指摘としない
- **deviation / スコープ**: `deviation.md` 記載の 2 件は合意済みの差分として扱った。Sample への一時的な push 遷移が撤去されていることは作業ツリーの状態で確認した
- **ミューテーション実測の原状復帰**: 触った 2 ソースは backup と shasum 一致、テストファイルも挿入した 1 行を除去済み。復帰後に対象テストクラスが緑であることを再実行で確認した

## アクションプラン

1. B-1 のテストに `cell.layoutIfNeeded()` を挟んで検出力を持たせる (必要なら `XCTAssertNil` の扱いも整理する)
2. `evidence.md` の自動テスト件数と担保点を 1061 tests / 現在の 7 件の内容へ更新する
3. テスト冒頭コメントの列挙と、`push / present` のコメントを実態に合わせる (B-3 / B-4)
4. B-5 は任意。次に触るときでよい
