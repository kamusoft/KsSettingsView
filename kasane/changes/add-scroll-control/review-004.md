# レビュー結果: add-scroll-control (004 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

対象は、review-001〜003 で APPROVED 済みのスクロール制御の本体の後に、オーナー指示で足したスコープ 5 件 (deviation.md の「スコープの拡張 (オーナー指示)」と (1)〜(5) の実装・付随修正・オーナー合意の行) と、それが既存のスクロール制御に与える影響。5 件とも deviation.md の記録どおりに実装されている。原因は実測で裏付けられ、(1)(2)(4) は A/B の証跡がある。差分の契約 (Store 操作の意味と、削除 → 追加 → 移動の適用順) も保たれている。Critical / Major は無い。指摘は、再レイアウト後の戻し直しの抑止条件にテストが無いこと (Minor 1 件) と、Suggestion 3 件にとどまる。

確認した観点:

- (3) 宣言の差分: `planReorder` の正しさを机上で確かめた。最長増加部分列に入る項目を動かさず、追加は直前の「動かさない項目か先に追加した項目」の直後へ入れ、残りは新しい順に直前の項目の直後へ移す。どの順で適用しても宣言の並びに着地する。テストは Store に順に適用した結果で着地を確かめており、5 項目の全順列 × 追加・削除の有無の網羅と、移動件数が「項目数 − LIS 長」になることを両 OS で見ている。実際の適用経路 (Android `KsSettingsViewComposable.kt` の `applyDiffToStore`、iOS `KsSettingsView.swift` の `applyDiffToStore`) も差分列を 1 件ずつ Store に流す逐次適用で、テストの `applyToStore` と同じ解釈になっている。iOS の `moveCell` の直後に `replaceCell` を出す並び、`SettingsRootDiff` の公開 doc との矛盾が無いこと、Section の移動の付随修正 (`from` を適用時点の並びで出す) も確かめた。重複 ID を渡したときの振る舞いは従来も未定義で、新しい実装でもクラッシュする経路は無い
- (4) Compose の文字列の Root Header / Footer: Explicit API Strict に適合している (公開 Composable へ既定値付きの引数を足しただけで、補助関数は private)。公開 doc コメントに内部用語は無い。Composable を優先することは doc とテストの両方にあり、MAUI の `RootHeaderView` 優先と一致する (オーナー合意済み)。`isSameText` は、同じ文字列のまま再代入して行を作り直すことを防ぐ。これはテスト (`assertSame`) で固定されている。Sample は 3 platform で文言が一致している (Android `rootFooterText` / iOS `.rootFooter(_:)` / MAUI `RootFooterText`)。実機の描画は `evidence/android-native-root-footer-text-after.png` で見た
- (5) iOS Sample: 書き方が全画面で 1 つにそろっている。`@Sendable` の通知の閉包で、中身を `MainActor.assumeIsolated` で包む形。本体の `EntryCell` / `PickerCell` の Binding の書き戻しと同じ形で、各画面の操作の結果は変わっていない (`guard ... else { return }` が閉包の中の return になっても等価)。`SpikeBoxAttributes.isEqual` の書き換えも、元の論理と同じ判定になっている
- (1) MAUI の最低版: `maui/Directory.Packages.props` のコメントに理由 (AndroidX.Core 1.17 以降の `Checked` の型変更と `set_Checked(bool)` の欠落) と、版を上げるときの確認点がある。10.0.71 は maui/ADR-0026 の前提 (10.0.60 以上) の範囲内。利用者に求めるのは衝突を回避するピンではなく最低版の引き上げなので、maui/ADR-0010 の「利用者にピンを書かせない」にも反しない (NU1605 で止まる影響はオーナー合意済み)。CPM の対象プロジェクトは宣言元 1 か所から、`MauiVersion` を直書きしている 2 つ (Sample・消費者検証) は個別に、いずれも 10.0.71 に上がっている。workflow には版の直書きが無いことを確かめた。`evidence/maui-android-a11y-crash-before-after.log` には、修正前の MissingMethodException、MAUI の assembly が参照するメンバーの照合 (10.0.70 は missing=1、10.0.71 は 0)、修正後の TalkBack 操作がそろっている
- (2) iOS の再接続後の領域: `viewDidLayoutSubviews` の中で待ち行列の命令を実行したときだけ再レイアウトを予約する。実行は `main.async` の後で、1 回にまとめる。予約の後に届いた命令は `enqueueScrollEntry` の `main.async` より後に実行されるため、戻し直しは `incoming` / `ready` が空でないときには行わず、命令の順序は崩れない。回帰テストは修正を外すと失敗すると deviation にあり、`evidence/maui-ios-reconnect-band-before-after.log` に 6 回の A/B と、戻し直しを外した版で位置がずれることの記録がある

実行したテスト (Simulator / Emulator は使っていない。iOS はホスト報告の 1194 / 0 と Swift 6 言語モードの error 0 を採った):

- Android: `./gradlew :kssettingsview:testDebugUnitTest --tests 'jp.kamusoft.kssettingsview.compose.*'` を実行し、180 tests / 0 failures (うち `DSLDiffCalculatorTest` 27、`ComposeRootAccessoryTextTest` 6)
- MAUI: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` を実行し、624 tests / 0 failures

## 照合した規約

- cross/comment-policy.md (always): 新しいコメント・公開 doc コメントに禁止参照・内部用語・履歴記述が無いこと
- cross/test-execution.md (テスト実行・結果報告): 件数の確認、条件ベースの待機、負の検証
- cross/runtime-behavior-verification.md (実行時挙動の不具合修正の完了判定): (1)(2) の再現 → 修正 → 同じ手順での解消と証跡
- cross/sample-parity.md (`samples/**` の変更): Root Footer の文言の 3 platform 一致と、Sample の書き方の統一
- cross/public-identifiers.md (`**/*.csproj`・`maui/Directory.*.props` を触るとき): 識別子の変更は無い
- ios/swift6-language-mode-check.md (`ios/Sources/` を触る変更の完了判定): ホストの報告 (error 0) を確認
- decisions: core/ADR-0006・0007 (差分と Store の境界、DSL と Store の収束)、core/ADR-0033 (Root の header / footer)、maui/ADR-0010・0026 (版の選定)、cross/ADR-0027 (負の検証)
- lessons: code-review.md (L-001)、process.md (L-001・L-003・L-005・L-009)

## 指摘事項

### 🟡 Minor 再レイアウト後に位置を戻し直すかどうかの抑止条件にテストが無い

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift:208-211`
**問題点**: `scheduleAncestorRelayout()` は、再レイアウトの後に新しい命令が届いているとき (`readyScrollEntries` / `incomingScrollEntries` が空でない) と、命令のアニメーションが進行中のとき (`activeScrollAnimation != nil`) は、位置を戻し直さない。deviation のスコープの拡張 (2) の実装の行もこの振る舞いを明記している。このうちアニメーション中の抑止は、戻し直しの `performScroll` が先行のアニメーションを止めることを防ぐ条件で、「レイアウトの中で実行したアニメーション付きの命令を、再レイアウトの戻し直しが途中で打ち切らない」という命令の順序の保証を担っている。しかし追加されたテスト (`ScrollControlTests.swift:1104` の 1 件) は、アニメーションなしの戻しで表示領域が伸びる経路だけを通る。どちらの抑止条件を外してもテストは緑のまま、つまり回帰を検出できない。
**推奨修正**: 既存の `BoundsFollowingContainerController` / `ExpandOnScroll` の構成に、アニメーション付きの命令 (例: `scrollToEnd(animated: true)`) を取り付け前に積むテストを 1 件足す。領域が伸びた後もアニメーションが最後まで進み、行き先に着地することを確かめ、条件を外すと落ちることを実測する (lessons code-review L-001)。

### 🔵 Suggestion レイアウトの中で実行した中央合わせの命令は、再レイアウトの後に中央からずれる

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift:200-212`
**問題点**: 戻し直しは、再レイアウトの前に控えた位置 (上端にかかる要素とずれ) を `.start` で戻す。控えた位置の戻し (restore) なら意図どおりに動く。一方、取り付け前に受けて最初のレイアウトで実行した `.center` の命令 (SwiftUI の `onAppear` で出す命令など) では、上端の要素を保つ結果になる。大きいタイトルが縮んで表示領域が伸びると、対象の Cell は伸びた量の半分だけ中央から上へずれる。`.end` は範囲の制限で末尾に留まるので影響しない。
**推奨修正**: 実害は小さいので現状のままでよい。蒸留でスクロール制御の概念に「レイアウトの中で実行した命令の後に祖先の寸法が変わった場合、位置は上端の要素で保つ」と書き添えておくと、後から挙動を読み解きやすい。

### 🔵 Suggestion Sample の `MainActor.assumeIsolated` は、公開 API に書かれていない「メインスレッドで呼ぶ」保証に頼っている

**該当箇所**: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift:27-28` ほか各画面の doc コメント、`ios/Sources/KsSettingsViewUI/CommandCell.swift:31-32`
**問題点**: Sample は「呼ばれるのは常にメインスレッドなので」という前提で `assumeIsolated` を使っている。しかし本体の公開 doc コメント (例: `CommandCell.onTap`「タップ時に発火するクロージャ」) は、呼び出しのスレッドを約束していない。`assumeIsolated` は前提が崩れると実行時に停止するため、利用者が手本として写すには、保証が公開の契約として書かれている必要がある。また利用者向け `skills/` の例 (`onTap: { showLicense = true }`) は書き換え前の形のままで、Sample と食い違う (skills の追従は docs-refresh の対象なので、この change の指摘ではない)。
**推奨修正**: この change では直さない。Cell の通知の閉包の型 (`@MainActor @Sendable` にする) か、公開 doc コメントでメインスレッドで呼ぶことを明記するか、を別の change の論点として積むことを勧める (簡易起票の候補)。docs-refresh の申し送りにも、Sample の書き方がこう変わったことを添えておく。

### 🔵 Suggestion 「Composable と文字列を両方渡す」テストの行の不在の確認は、一覧の反映を待たずに成立しうる

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeRootAccessoryTextTest.kt` の `Composable と文字列を両方渡すと Composable が表示される`
**問題点**: `flushIdle()` の後の `findRootTextRows(layout).isEmpty()` は、一覧の行がまだ配置されていない時点でも成立する (`AsyncListDiffer` の反映は idle 系の待機では待てない。cross/test-execution の「非同期反映を待たないアサーション」)。回帰を検出しているのは `layout.rootHeader is RootAccessory.View` のほうで、行の確認は補強にとどまる。
**推奨修正**: Cell の行か Composable の Root 行が配置されるまで条件ベースで待ってから、文字列の行が無いことを確かめる。

## アクションプラン

1. (Minor) `scheduleAncestorRelayout()` のアニメーション中の抑止条件を検出できるテストを 1 件足す。条件を外すと落ちることを実測する
2. (Suggestion) 蒸留時に、レイアウトの中で実行した命令の後の戻し直しは上端の要素で位置を保つことを、スクロール制御の概念へ書き添える
3. (Suggestion) Cell の通知の閉包のスレッドの契約 (型か公開 doc) を別の change の論点として積み、docs-refresh の申し送りに Sample の書き方の変化を添える
4. (Suggestion) `ComposeRootAccessoryTextTest` の不在の確認の前に、行の配置を条件ベースで待つ
