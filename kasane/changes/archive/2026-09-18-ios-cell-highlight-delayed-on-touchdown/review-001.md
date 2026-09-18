# レビュー結果: ios-cell-highlight-delayed-on-touchdown (001 回目)

**日付**: 2026-09-18
**判定**: CHANGES_REQUESTED

## サマリー

押下色の状態機械 (遅延 → フェードイン → 選択 → 遷移連動の解除) は破綻なく組めており、Task の競合・再利用時の持ち越し・MainActor 隔離は確認した範囲で穴がない。ビルド・テストとも通り、件数も summary の申告と一致する (1059 tests / 0 failures)。

一方で、summary が契約として書いている「選択されずにキャンセルされたとき (スクロール開始) は即時復帰」が、フェードイン進行中のキャンセルでは成立していない疑いが強い。打ち切りのために書かれた `listCell.backgroundView?.layer.removeAllAnimations()` は、このリポジトリのどこも `backgroundView` へ代入していないため常に nil に対する no-op である。これと、summary / ADR-0005 の「present なら選択を残す」が本ライブラリ自身の modal (pageSheet) では成立しない点の 2 件を、オーナーに戻して確認してもらう必要がある。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 禁止参照 0 件、新規 4 ファイルに要確認 (advisory) の該当なし
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/**` を触る変更の完了判定) — 適合を独自に再確認 (下記「確認した観点」)
- `kasane/handbook/cross/test-execution.md` (テスト実行・結果報告) — Simulator 全件実行・バンドル集計行での件数確認・収束待ちの書き方を照合
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合の完了判定) — 実機での再現と解消確認・証跡が `evidence.md` にある
- `kasane/lessons/code-review.md` (重点観点 L-001) — 争点になったアサーションは無いため、ミューテーション実測は不要と判断

---

## 指摘事項

## A. 見た目・挙動が変わりうるもの (オーナーに戻す必要がある)

### [🟡 Minor / 優先度 高] キャンセル時の「即時復帰」が、フェードイン進行中は成立していない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:207` (および同じ意図の `:133`)

**問題点**:
`restoreNormalColor(_:fadesOut:)` の else 節は「進行中のフェードを打ち切って即座に戻す」ことを意図して `listCell.backgroundView?.layer.removeAllAnimations()` を呼んでいるが、`ios/Sources/` のどこも `UICollectionViewListCell.backgroundView` へ代入しておらず (grep でこの 2 行以外の出現なし)、背景は `backgroundConfiguration` 経由で UIKit 内部の背景 view に描かれる。したがって `backgroundView` は常に nil で、この行は無条件の no-op になる。

残るのは `UIView.performWithoutAnimation` での config 差し替えだけで、これはモデル値を即座に書くだけであり、`fadeInSelectedColor` が張った実行中のアニメーションは取り消されない。到達経路は実在する:

- 指を置く → 0.1 秒経過してフェードイン開始 → 0.12 秒経つ前にスクロールを始める
- このとき `isShowingSelectedColor == true` / `hasBeenSelectedWhilePressed == false` なので `fadesOut: false` へ入る

この場合、押下色は消えずに残りの時間ぶん濃くなり切ってから平常色へ飛ぶ可能性が高い (指を置いてすぐスクロールを始める操作は 0.1 秒の待ちに吸収されるため、`evidence.md` の「リストのスクロールを始める」の確認では踏めていない経路)。

**推奨修正**:
1. 実機で「Cell に指を置いて 0.2 秒ほど止め、押下色が出てからスクロールを始める」を試し、押下色が一瞬濃くなってから飛ぶかを確認する。
2. 症状が出る場合、打ち切りを実効にする (`UIView.performWithoutAnimation` の代わりに duration 0 + `.beginFromCurrentState` の `UIView.animate` で走行中のアニメーションを置き換える、など)。
3. 症状が出ない (UIKit 側が config 差し替えでアニメーションを畳む) 場合は、`:133` と `:207` の `backgroundView?.layer.removeAllAnimations()` 2 行を削除する。残しておくと「打ち切りはここで担保されている」と読めてしまう。

### [🟡 Minor] 「present なら選択を残す」が、本ライブラリ自身の modal では成立しない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:487-489` / 呼び手は `ios/Sources/KsSettingsViewUI/PickerCellView.swift:109`、`ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:118`

**問題点**:
遷移の有無は `viewWillDisappear` が猶予内に来るかで判定しているが、UIKit は pageSheet / formSheet の提示では提示元の `viewWillDisappear` を呼ばない。PickerCell / DatePickerCell はいずれも `modalPresentationStyle` を既定 (`.automatic` → iPhone では pageSheet) のまま `present` しており、`DatePickerCalendarSheetController` は明示的に `.pageSheet` である。つまりこれらの Cell をタップした場合、押下色は 0.1 秒後に解除されてシートの立ち上がりと重なってフェードアウトする。

summary の「タップ後 0.1 秒の猶予内に画面が隠れ始めれば (push / present) 選択を残す」と `kasane/decisions/ios/0005-cell-press-feedback-library-controlled-timing.md` の Decision 3 点目の記述は、この経路では成立しない。judgement は実挙動の側かもしれない (シート提示時に押下色を残すのが望ましいとは限らない) が、記述と実装のどちらを正にするかはオーナーの判断が要る。

**推奨修正**:
実機で Picker / DatePicker のタップ時の押下色を確認したうえで、(a) 現状の見え方で良ければ summary と ADR-0005 の `(push / present)` を「フルスクリーン遷移 (push / fullScreen presentation)」に限定する記述へ直す、(b) シート提示でも残したいなら `viewWillDisappear` 以外の手掛かり (`presentedViewController` の有無など) を猶予判定に足す、のいずれかを選ぶ。

### [🔵 Suggestion] EntryCell の文字選択ドラッグが横取りされないかの確認

**該当箇所**: `ios/Sources/KsSettingsViewUI/ImmediateTouchCollectionView.swift:24`

**問題点**:
`touchesShouldCancel(in:)` の例外を `UISwitch` / `UISlider` に限ったため、`UITextField` 上から始めたドラッグはキャンセル対象になる。文字選択のドラッグも「スライド操作を入力に使う」たぐいの操作であり、例外に入っていない。実際には文字選択は gesture recognizer 側で調停されるため影響しない見込みだが、`evidence.md` の確認項目に EntryCell が無いため未検証のまま残っている。

**推奨修正**: EntryCell に文字を入れて、テキスト内をドラッグして選択できるかを実機で 1 回だけ確認する。横取りされるようなら `UITextField` を例外に足す。

---

## B. 見た目・挙動が変わらないもの (リファクタ・テスト・記述)

### [🟡 Minor] 状態機械の 2 本の分岐がテストで押さえられていない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/CellTouchFeedbackTimingTests.swift`

**問題点**:
summary が挙げる挙動のうち、次の 2 つに対応するテストがない。どちらも秒数を固定せずに書けるうえ、壊れても既存 5 件では検出できない。

- 「待ちの間にキャンセルされたら何も出ない」— 遅延中のキャンセルで塗り始めが捨てられること
- 「`prepareForReuse` で押下色の予約・表示状態をリセットする」— `resetSelectedColor` が呼ばれること

**推奨修正**:
前者は負の検証として書ける。`isHighlighted = true` の直後に `false` へ戻し、`waitForNegativeVerification(in:)` (既定 0.2 秒 > `selectedColorHighlightDelay` の 0.1 秒) のあとに背景が平常色のままであることを確かめれば、予約の取り消しが壊れたときに 0.1 秒後の塗りを捕まえられる。後者は押下色の表示中に `prepareForReuse()` を呼び、`KsCellViewSupport.state(cell).isShowingSelectedColor` が false に戻ることと、その後の `applyRenderedBackgroundColor` が平常色を入れることを確かめる形で足せる。

### [🔵 Suggestion] `delaysContentTouches = false` を collection view 側へ寄せる

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:423` / `ios/Sources/KsSettingsViewUI/ImmediateTouchCollectionView.swift:23`

**問題点**:
`ImmediateTouchCollectionView` の doc コメントは「`delaysContentTouches = false` と組み合わせて使う」と書いているが、その設定は呼び出し側にある。型としては対で初めて意味を持つので、生成箇所が増えたときに片方だけ抜ける余地が残る。

**推奨修正**: `ImmediateTouchCollectionView` の `init` で `delaysContentTouches = false` を自分で設定し、doc コメントを「この型は遅延を持たない」と言い切る形にする (既存の呼び出し側の 1 行は残しても害はないが、正が型側にあることを明確にする)。

### [🔵 Suggestion] didSelect の解除 Task を前のタップと共有しない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2545-2549`

**問題点**:
タップごとに解除 Task を作って捨てているため、連続タップでは前のタップの Task が後のタップの猶予を短く切る (A をタップした 50ms 後に B をタップすると、A 由来の Task が 100ms 時点で `deselectAllItems` を呼び、B の押下色が猶予より早く解除される)。実害は小さいが、猶予時間の意味が「最後のタップから 0.1 秒」ではなくなっている。

**推奨修正**: 解除 Task を 1 本だけ保持し、新しいタップで前の Task を cancel してから作り直す。現状のままで問題ないと判断する場合は、猶予がタップごとではないことをコメントに明記する。

### [所見] 長命層への反映は蒸留の宿題として残っている

`kasane/concepts/core/styling/cell-visual-states.md` と `kasane/concepts/ios/api/ios-native-host.md` は押下・選択中に `Theme.selectedColor` を使うこと、解除後に実効背景へ戻すことまでしか書いておらず、今回入った「立ち上がりの遅延・フェード・遷移連動の解除」のタイミング契約を含まない。既存の記述と**矛盾はしない** (状態解除後に実効背景へ戻す・無効 Cell に feedback を残さない、はいずれも維持されている) ため本レビューでは指摘としない。`kasane/decisions/ios/0005-cell-press-feedback-library-controlled-timing.md` は `proposed` であり、これを根拠にした指摘も出していない。

---

## 確認した観点 (問題なしと判断したもの)

- **Task の競合・取り残し**: 塗り始めの予約 Task は `pendingSelectedColorTask == nil` のときだけ作られ、解放は必ず cancel → nil の順で行われるため、cancel 済みの Task が後から生成された Task のハンドルを nil で潰す経路は無い (cancel 済み Task は `Task.isCancelled` の guard で先に抜ける)。Task は `listCell` / `self` を weak で捕まえており、Cell・Controller を引き伸ばさない
- **再利用時のリーク**: `resetSelectedColor` は `KsListCellBase.prepareForReuse` と `CustomCellView.prepareForReuse` の 2 経路に入っており、`ios/Sources/KsSettingsViewUI/` の Cell 種はすべてこのどちらかを継承して `super.prepareForReuse()` を呼んでいる (全 `prepareForReuse` override を確認)
- **MainActor 隔離 / Swift 6 言語モード**: `configurationUpdateHandler` の nonisolated closure 内で押下状態を Bool へ落としてから `MainActor.assumeIsolated` に入る既存の作法が保たれている。`xcodebuild build -scheme KsSettingsView SWIFT_VERSION=6` (`-swift-version 6` が各 source ターゲットの swiftc 引数に入ることを確認) で error 0 件・BUILD SUCCEEDED。`ios/Package.swift` に一時設定の残骸はない (git diff 0 件)
- **SwiftUI ホスト**: `viewWillAppear` / `viewWillDisappear` は `UIViewControllerRepresentable` 経由でも呼ばれるため、`NavigationStack` の push でも猶予判定は成立する。`KsSettingsViewSwiftUITests` 98 件は緑。ただし SwiftUI の `.sheet` は A-2 と同じ pageSheet の経路に当たる
- **既存契約との整合**: 無効 Cell は `guard s.isEnabled` で押下色に入らず、解除後は `effectiveCellBackgroundColor` へ戻る。render の背景適用は `defaultBackgroundConfiguration()` を土台にする点が変更前と同じで、`CellBaseLayout` での適用位置が `setRenderState` の後ろへ移ったのは実効背景色を読むために必要な順序変更
- **テスト実行**: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` のバンドル集計行の合算で 170 + 88 + 98 + 7 + 696 = 1059 tests / 0 failures。summary の申告と一致
- **テストの待機の書き方**: 収束待ちは `awaitCondition`、不変性の確認は `waitForNegativeVerification` と用途で分かれており、固定待機は名前から負の検証と判別できる。`waitForNegativeVerification` の既定 0.2 秒は `selectionHoldDuration` の 0.1 秒より長く、遷移時に解除しない検証に検出力がある
- **コメント規約**: `comment-policy-lint.py --advisory` で禁止 0 件、今回の 4 ファイルに要確認の該当なし。作業文書のパス・変更 ID・試行番号・デルタスペック構文キーワードの混入も無い
- **deviation**: `deviation.md` 記載の 2 件 (Sample への一時的な push 遷移とその撤去、controller 設定 1 箇所を超えた波及) は合意済みの差分として扱い、指摘にしていない。Sample 側に一時変更の残骸がないことは git status で確認した

## アクションプラン

1. A-1 のキャンセル時の見え方を実機で確認する (押して止めてからスクロール)。症状が出れば打ち切りを実効にする修正、出なければ no-op 2 行の削除
2. A-2 の Picker / DatePicker タップ時の押下色を実機で確認し、summary と ADR-0005 の `(push / present)` の記述を実挙動に合わせる
3. B-1 のテスト 2 本 (遅延中のキャンセル / `prepareForReuse` のリセット) を足す
4. A-3 の EntryCell 文字選択ドラッグを確認する (1 操作)
5. B-2 / B-3 は任意。次に触るときでよい
