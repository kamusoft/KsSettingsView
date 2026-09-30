# レビュー結果: add-scroll-control (002 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

前回 (review-001 と second-opinion-code-001 の突き合わせ) で修正対象とした 4 件は、すべて実装とテストで解消されている。修正のために入った変更 (対象を解決した後に先行のスクロールを止める、DSL の受け口がハンドルの差し替えで未実行の命令を捨てる、Host の待ち行列の命令も捨てる、`@Volatile`) も読み直した。新たな欠陥は見つからなかった。deviation.md の末尾 2 行が、Host の待ち行列まで捨てる形への拡張を記録している。足場 (proposal / design / specs) は書き換えられておらず、tasks.md の差分はチェックの更新だけである。

残る指摘は 1 件で、テストの検出力に関する Minor である。DSL の受け口が「まだ引き直していない命令」を捨てる経路は、Android ではどのテストも通っていない。iOS も、テストがこの経路を通るかどうかが SwiftUI の更新の順序に依存している。どちらも実装は 1 行で正しく、この経路を外しても利用者の目に見える結果が守られる順序もあるため、判定を止める重さではないと判断した。

## 照合した規約

- cross/comment-policy (always)。変更ファイル 70 本に `scripts/comment-policy-lint.py --advisory` をかけた結果、禁止は 0 件。本 change で足された行にかかる advisory は 5 件あった。いずれも既存の ADR 参照を保ったままの文言の追随か、「〜だった」を履歴記述と見た誤検出で、前回と同じ扱いにした
- cross/test-execution (テスト実行・結果の報告)。Android は `--rerun-tasks` で全件を実行し、件数は XML で集計した。MAUI も全件実行した
- cross/diagnostic-message-language (警告ログ・例外文言)。修正サイクルで新しく足された文言は無い
- cross/sample-parity (`samples/`)。3 platform の文言を突き合わせ、一致していることを確かめた
- cross/runtime-behavior-verification (アニメーションの挙動)。撮り直しが残っている分は、既知の未了として指摘から除いた
- ios/swift6-language-mode-check (`ios/Sources/` の変更)。error 0 はホスト側の結果を事実として扱った
- maui/integration-host-verification (binding 層・facade 層)。修正サイクルでは binding 層を触っていない
- lessons/code-review L-001、lessons/process L-001 / L-005 / L-006 / L-009

## 確認した事実 (ビルド・テスト)

- Android: `./gradlew test --rerun-tasks` を再実行した。kssettingsview 1378 × 2、bridge 193 × 2 で、計 3142 tests / 0 failures / 0 errors (test-results の XML を集計)。前回より 3 件多く、増えた分は本サイクルで足されたテスト (対象の無い命令 1 件、Compose の差し替え 2 件) と一致する
- MAUI: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` を再実行した。624 tests / 0 failures
- iOS: Simulator を起動しない制約があるため再実行していない。ホスト側の 1184 tests / 0 failures を事実として扱った。前回の 1175 件から 9 件増えており、本サイクルで足されたテスト (UIKit Host 5 件、SwiftUI DSL 4 件) と件数が一致する
- `scripts/local-path-lint.py` と `scripts/identity-lint.py` の違反は 0 件
- L-009: 修正サイクルで名指しの範囲を超えて触れた箇所は 2 つあり、どちらも deviation.md に記録がある。iOS の `dropPendingScrollCommands()` の package への引き上げと、Android の `internalDropPendingScrollCommands()` の追加である

## 前回指摘の解消状況

| 指摘 (出典・採否) | 状況 | 根拠 |
|---|---|---|
| 対象の無い命令が先行のアニメーションを止める (相方 Major 1・採用) | 解消 | iOS は `ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift:189-204,257-262` で、ID を解決し行き先が求まってから `stopActiveScrollAnimation()` を呼ぶ。行き先が求まらない復元も止めない。Android は `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewScrollControl.kt:156-159` で、`planFor` が `null` なら `stopScroll()` の前に戻る。回帰テストは iOS `ScrollControlTests.swift:766` と Android `ScrollControlTest.kt:599` にある。存在しない ID・非表示の Cell / Section・要素の無い Section・未知の Section を続けて出し、先行のアニメーションが進行中のままで、最後は末尾に着くことを確かめている |
| SwiftUI DSL で切断前の保留命令が新しい接続へ流れる (相方 Major 2・採用) | 解消 (テストの検出力に Minor 1 件) | `ios/Sources/KsSettingsViewSwiftUI/DSLScrollCommandResolver.swift:52-61` で、`connect(_:)` が `pending.removeAll()` と `host?.dropPendingScrollCommands()` を呼ぶ。Android の `DSLScrollCommandResolver.kt` と同じ形になった。テストは `DSLScrollControlTests.swift:350,382,415,443` の 4 件。検出力の残りは下の Minor に書いた |
| iOS テストに Section の center / end・高い対象の start・Section 見出しの控えと戻し・画面外の間の命令の 4 観点が無い (ホスト Minor・確定) | 解消 | `ScrollControlTests.swift` に 4 件が足された。:509 は Footer の下端と、見出しから Footer までの範囲の中央を確かめる。:535 は 30 行の Section で見出しが上端に来ることを確かめる。:948 は見出しと Footer の両方を、ずれ付きで控えて戻す。:386 は外れている間に命令を出し、固定待機 (cross/ADR-0027) で処理件数 0 を確かめてから、取り付け直しの後の位置を確かめる。推奨した検証の形をすべて満たしている |
| Android `KsScrollController.receiver` の可視性 (ホスト Suggestion・採用) | 解消 | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsScrollController.kt:36` に `@Volatile` が付き、理由も KDoc に書かれている。post の先では受け口を改めて読み直している (:71) |
| Android `onLayout` ごとの既定 id の走査、Bridge `onPreDraw` の毎フレームの一時リスト (ホスト Suggestion・降格) | 修正対象外 | 突き合わせで降格した。コードは変わっておらず、再度は指摘しない |

## 指摘事項

### 🟡 Minor DSL の受け口が「まだ引き直していない命令」を捨てる経路が、Android ではテストされず、iOS も SwiftUI の更新の順序頼みになっている

**該当箇所**:
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScrollCommandResolver.kt:77` (`pending.clear()`)
- `ios/Sources/KsSettingsViewSwiftUI/DSLScrollCommandResolver.swift:58` (`pending.removeAll()`)
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollControlTest.kt:318,357`
- `ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift:350,382`

**問題点**: ハンドルを差し替えたときに古い命令が画面を動かさないのは、2 つの処理がそれぞれ別の順序を受け持っているからである。

- (a) 受け口がまだ引き直していない命令を捨てる。差し替えが引き直しより先に起きた場合を受け持つ
- (b) 引き直して Host の待ち行列へ渡した命令を捨てる。差し替えが引き直しより後に起きた場合を受け持つ

Android のテスト 2 件 (:318, :357) は、どちらも `resolver.resolvePending()` を呼んでから `connect` している。つまり (b) だけを通る。受け口に命令が残った状態で `connect` するテストは無く、`pending.clear()` を消してもどのテストも落ちない。

iOS の :350 と :382 は、命令を出した直後に `@State` を差し替え、SwiftUI の更新による `connect` を待つ。受け口の引き直し (`main.async`) は、RunLoop が眠る前の SwiftUI の更新より先に走りうる。その順序だと命令は Host へ渡った後に (b) で捨てられ、(a) の `pending.removeAll()` を消してもテストは通る可能性がある。これはコード読みによる推測で、制約により Simulator での実測 (L-001 のミューテーション) はしていない。

実装はどちらの OS も正しい。この経路は相方 Major 2 の中心なので、回帰の検出力を持たせておきたい。

**推奨修正**:
- Android: `ComposeScrollControlTest` にテストを 1 件足す。`runOnUiThread` の中で `first.scrollToEnd(animated = false)` の直後に、`resolvePending()` を呼ばずに `current = second` と `resolver.connect(second)` を実行する。その後フレームを進め、処理件数が 0 のまま位置が変わらないことを負の検証で確かめる
- iOS: :350 / :382 を決定的にする。Host の待ち行列のテスト (:415) と同じく、`first.scrollToEnd` の直後、`main.async` を経る前に `resolver.connect(second)` / `resolver.connect(nil)` を同期で呼ぶ。そうすれば SwiftUI の更新の順序に依らず (a) を通る
- どちらも、(a) の 1 行を外すと落ちることを一度だけ実測しておく (L-001)

## 確認した観点 (指摘なし)

- **修正で入った変更の副作用**:
  - iOS: 先行のアニメーションを止める位置を `performScroll` へ移した。アニメーションあり・なし・行き先との差が 0.5pt 未満のいずれの経路でも、送る前に一度だけ止まる。復元 (`.restore`) でも、要素が見つからなければ止めない
  - Android: `recyclerView.stopScroll()` を `planFor` の後へ移した。アニメーションありで、クランプの結果 0 になる場合 (その場が行き先) も、止めてから何もしない。一貫している
- **DSL の差し替えで Host の待ち行列も捨てる拡張** (deviation.md 末尾):
  - 捨てるのは `.command` / `Command` だけで、復元は残る (iOS `KsSettingsViewController+ScrollControl.swift:146-153`、Android `KsSettingsViewScrollControl.kt:107-110`)
  - `makeUIViewController` / `factory` では、`host` を新しい Host に差し替えてから `connect` するため、新しい Host の保存状態からの復元を捨てない
  - 同じハンドルでの再接続は早期に戻るので、SwiftUI / Compose の更新のたびに命令が捨てられることは無い
  - Compose の `onRelease` と `DisposableEffect` の二重の `connect(null)` は冪等になっている
- **SwiftUI の Store 方式・UIKit Host の接続口**: `didSet` / setter で同一ハンドルなら何もせず、差し替えのときだけ未実行の命令を捨てる。Bridge の `makeHostViewController` は、ハンドルを代入してから復元を積む順序なので、復元が捨てられない
- **iOS のアニメーションの駆動**:
  - `CADisplayLink` は Host を弱参照する ticker を挟んでいる。Host が解放されれば次のフレームで止まる
  - 利用者のドラッグ開始 (`scrollViewWillBeginDragging`) で止める。これは既存の delegate 実装と衝突しない (他に実装が無いことを検索で確認した)
- **Bridge の位置追跡** (両 OS の `KsBridgeHostAnchorTracker`): Host の差し替え・`releaseHost()`・`dispose()` で `stop()` し、登録を外している。iOS は Host の view に置く見えない子 view で、Host の view の子を前提にした既存コードは無い (検索で確認した)
- **MAUI**:
  - 準備完了は Host の世代ごとに 1 回で、切断で取り消される
  - `IsLoaded` 済みで接続された場合も、その場で知らせる
  - 対象の解決は、明示 ID → ItemsSource の項目の順
  - binder の `_items` は `_generated` と同じ添字で、生成・追加・削除・置換・移動・作り直しの全経路で更新される
- **足場の凍結**: proposal / design / specs に差分は無く、tasks.md は `[ ]` → `[x]` の変更だけだった (6.3 は未チェックのまま。既知の未了)
- **Sample**: 3 platform の画面の文言・Section の構成・操作の説明文が一致している

## アクションプラン

1. (推奨) Android の `ComposeScrollControlTest` に、受け口に命令が残った状態でハンドルを差し替えるテストを足す。iOS の `DSLScrollControlTests` :350 / :382 は `resolver.connect(_:)` の同期呼び出しで決定的にする。どちらも (a) の 1 行を外すと落ちることを実測する
2. (既知の未了) tasks.md 6.3 の連続フレームの撮り直しを済ませ、チェックする
