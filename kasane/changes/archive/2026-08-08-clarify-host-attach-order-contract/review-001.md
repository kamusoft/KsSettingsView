# レビュー結果: clarify-host-attach-order-contract (001 回目)

**日付**: 2026-08-08
**判定**: APPROVED

## サマリー

iOS `KsSettingsViewController.viewDidLoad` への `resyncFromStore()` 追加は Decision 1〜4 の範囲どおりで、余計な挙動変更を持ち込んでいない。ios-host 6 Scenario / android-host 2 Scenario はいずれも対応テストを持ち、Android は「取り付け前は届いていない」対照アサーションを置いて収束が attach 時の再取り込みによることを示せている。ビルド・テストは iOS 444 件 / Android 1990 件がいずれも 0 failure、MAUI 検証ホストは両 OS とも警告 0・エラー 0 でビルドでき、E2E スクリーンショットも `maui/README.md` の期待表示と一致した。Critical / Major はなく、指摘は低優先の Minor 2 件と Suggestion 3 件のみ。

## 検証の実施内容

| 対象 | コマンド | 結果 |
| --- | --- | --- |
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` | `Executed 444 tests, with 0 failures` / `** TEST SUCCEEDED **` |
| Android | `./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / test-results XML 集計 `tests 1990 failures 0 errors 0 skipped 0` |
| MAUI iOS ホスト | `dotnet build maui/tests/KsSettingsView.IntegrationHost.iOS/...csproj -c Debug` | 成功 (0 警告 / 0 エラー) |
| MAUI Android ホスト | `dotnet build maui/tests/KsSettingsView.IntegrationHost.Android/...csproj -c Debug` | 成功 (0 警告 / 0 エラー) |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py --summary` | 禁止 0 件 (検査対象 401 ファイル) |
| E2E 証跡 | `verification/02` `verification/04` を目視 | root header/footer・Section 3 構成・緑の Section header まで `maui/README.md`「期待される表示」と一致 |

足場アーティファクト (proposal / design / specs) への変更はなく、`tasks.md` の差分はチェックボックスのみ。deviation.md は存在せず、無断の仕様逸脱も確認されなかった。

Scenario 対応の確認:

| Scenario | 対応テスト |
| --- | --- |
| ios: view load 前の構造操作 | `HostViewLoadRestoreTests.test_viewLoad前の構造操作がload時の表示に反映される` |
| ios: view load 前の Cell 内容更新 (`replaceCell` / `replaceCells`) | 同 `test_viewLoad前のreplaceCellが…` / `test_viewLoad前のreplaceCellsバッチが…` |
| ios: Section accessory / theme | 同 `test_viewLoad前のSectionAccessoryとTheme変更が…` |
| ios: 直接 applyTheme の上書き | 同 `test_Store接続中の直接applyThemeはviewLoad時にStoreThemeで上書きされる` |
| ios: Root accessory は復元対象外 | 同 `test_RootAccessoryは復元対象外で所有者の再適用により表示される` |
| ios: Store 非接続 init | 同 `test_Store非接続initはinit時のrootで表示する` |
| android: attach 前の更新 | `AttachOrderRestoreTest.取り付け前の構造操作と内容更新と Theme 変更が取り付け後に反映される` |
| android: detach 中の更新 | 同 `detach 中の Cell 内容更新が再取り付け後に反映される` |

## 指摘事項

### [🟡 Minor] `maui/README.md`「既知の制約」が旧契約のまま残っている

**該当箇所**: `maui/README.md`「既知の制約」節 (`Native Host は view 階層へ取り付けて view の構築が済むまで Store の通知を受け取らない。取り付け前に更新 API を呼ぶと root 全置換以外の反映を取りこぼすため、検証ホストは取り付け後に操作している。`)

**問題点**: 本変更で検証ホストは「Host 生成 → 操作 → 取り付け」の順序に戻り、`AppDelegate` / `MainActivity` のコメントも core/ADR-0019 の復元契約を説明する形になった。README のこの記述は実装と正反対で、しかも同じ README の「期待される表示」を証跡 (`verification/README.md`) が参照しているため、読み手が最初に当たる場所に古い制約が残っている。Root header / footer が復元対象外で所有者が view 構築後に適用する、という新しい注意点も未記載。

**推奨修正**: 本変更内では修正しない (プロジェクト規約により `docs/` と README 群の書き換えは `docs-refresh` スキル経由に限定されている)。蒸留フェーズで concepts を更新した後、`docs-refresh` の対象としてこの節の差し替え (旧制約の削除 + Root accessory の所有者責務の追記) を依頼すること。

### [🟡 Minor] `resyncFromStore()` が内部 index / visible projection を更新せず、直後の `applyFullSnapshot` に依存している

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:376-380`

**問題点**: `resyncFromStore()` は `self.root` と `self.currentTheme` を差し替えるだけで、`visibleSections` / `sectionIndex` / `cellIndex` は更新しない。現在は `viewDidLoad` で直後に `applyFullSnapshot(root:animated:)` が走り、そこで `computeVisibleSections` と `rebuildModelIndexes()` が実行されるため実害はない (テストもすべて通る)。ただし doc comment は「Store の現在状態を内部状態へ取り込む」と自己完結した操作のように書かれており、この呼び出し順序への依存はどこにも書かれていない。将来 `viewWillAppear` や再 attach フックから再利用すると、`root` だけが新しく `visibleSections` / `cellIndex` が古いという不整合状態を黙って作る。対称化の元になった Android の `resyncFromStore` は `setRootDirect` 経由で projection まで揃うため、この点だけ非対称になっている。

**推奨修正**: `resyncFromStore()` 内で `rebuildModelIndexes()` / `rebuildVisibleProjection()` まで行って自己完結させるか、doc comment に「呼び出し直後に `applyFullSnapshot` で projection と index を作り直す前提」であることを明記する。

### [🔵 Suggestion] Section accessory / Theme のテストのうち Theme 側は本変更の回帰検出力を持たない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/HostViewLoadRestoreTests.swift:212-222`

**問題点**: Theme 購読 (`store.$theme.dropFirst().sink`) は view load 前でも発火し、`applyTheme(_:)` が `currentTheme` を更新する (`KsSettingsViewController.swift:297-302` / `310-311`)。したがってこのテストの背景色・文字色アサーションは `resyncFromStore()` を外しても通り、Store pull の回帰を検出しない。Scenario 自体は満たしているので不足ではないが、Theme 経路の検出力は `test_Store接続中の直接applyThemeはviewLoad時にStoreThemeで上書きされる` (直接適用した teal を Store の pink が上書きすることを確認) だけが担っている、という理解でテスト群を扱う必要がある。

**推奨修正**: 修正は不要。将来テストを整理する際に、この 2 テストの役割 (前者は accessory の検出力・後者が theme pull の検出力) をコメントで区別しておくと、片方を消したときの穴に気づきやすい。

### [🔵 Suggestion] Root accessory テストが「保証されない」を「nil であること」として固定している

**該当箇所**: `ios/Tests/KsSettingsViewUITests/HostViewLoadRestoreTests.swift:276-283`

**問題点**: spec の THEN は「view load 直後の root header 表示は保証されず」であり、表示されないことまでは要求していない。テストは `XCTAssertNil(controller.rootHeader)` / `XCTAssertNil(visibleRootHeaderText(cv))` で「必ず出ない」を固定しているため、仮に将来 Root accessory の復元経路が入っても契約違反ではないのにこのテストが落ちる。現時点では「復元対象外」の意図を明示する回帰固定として有用なので実害はない。

**推奨修正**: 修正は不要。意図が「対象外であることの固定」であることが読み取れるよう、アサーションのメッセージに「保証対象外の現行挙動を固定している」旨を足しておくと、将来の判断が速い。

### [🔵 Suggestion] Store 接続中に公開 `applyDiff(.full:)` を view load 前へ渡す経路が黙って無効化される

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1009-1014` / `1051-1055` / `376-380`

**問題点**: `applyDiff(_:)` は public API で、view load 前の `.full` は `updateInternalRoot(for:)` が `self.root` に取り込む。Store 接続中はその後 `resyncFromStore()` が `store.root` で上書きするため、Store 由来でない `.full` を直接渡していた利用者はその root を失う。Decision 2 の「Store 接続中は Store を正とする」と整合しており仕様違反ではないが、proposal の Impact には挙がっていない公開挙動の変化ではある (Store 非接続時は従来どおり)。

**推奨修正**: 実装変更は不要。蒸留時に concepts (`ios/api/ios-native-host.md`) へ復元契約を書き起こす際、「Store 接続中は Store が正であり、view load 前に直接渡した `.full` / `applyTheme` は load 時に Store 現在状態で置き換わる」を1行で明記しておくと、公開契約として閉じる。

## アクションプラン

1. (蒸留フェーズ) concepts への復元契約の書き起こし — `ios/api/ios-native-host.md` に view load 時の Store pull と復元対象/対象外を追記し、あわせて Suggestion 4 の1行を入れる
2. (蒸留後・オーナー起動) `docs-refresh` で `maui/README.md`「既知の制約」の旧記述を差し替える (Minor 1)。実装フェーズでは触らない
3. (任意) `resyncFromStore()` の自己完結化またはコメントでの呼び出し順序の明記 (Minor 2)
4. Suggestion 3・4・5 はいずれも修正不要。将来のテスト整理・concepts 記述時の参考とする

## 補足 (環境メモ、指摘ではない)

worktree には `android/local.properties` が無いため (gitignore 対象)、`./gradlew test` は SDK location not found で構成段階から失敗する。`ANDROID_HOME=~/Library/Developer/Xamarin/android-sdk-macosx` を与えれば通る。変更内容とは無関係だが、この worktree で Android を検証する後続作業は同じところで詰まるので記録しておく。

---

# 再確認 (修正サイクル1)

**日付**: 2026-08-08
**判定**: APPROVED (維持)

## 対象

review-001 後に入った2点の修正のみを差分確認した。

1. Android テストの実時間待機の除去 (`AttachOrderRestoreTest.kt`)
2. iOS `resyncFromStore()` の doc コメント追記 (`KsSettingsViewController.swift`) — review-001 Minor 2 への対応

## 再検証の実施内容

| 対象 | コマンド | 結果 |
| --- | --- | --- |
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` | `Executed 444 tests, with 0 failures` / `** TEST SUCCEEDED **` |
| Android | `./gradlew test --rerun-tasks` | BUILD SUCCESSFUL (2m2s) / test-results XML 集計 `tests 1990 failures 0 errors 0 skipped 0` |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py --summary` | 禁止 0 件 (401 ファイル) |
| 行末空白 | `AttachOrderRestoreTest.kt` を走査 | 検出なし |

`AttachOrderRestoreTest` 単体の実行時間は debug variant で **2.249 秒 → 0.141 秒**へ短縮した (release variant 0.118 秒)。固定待機 6 回分 (150ms × 6 ≒ 0.9 秒) を含む待ち時間が消え、条件成立で即座に戻っていることが実測で裏づけられた。

## 修正1: Android テストの実時間待機の除去 — 対応妥当

**確認観点への回答**

- **`Thread.sleep` 残存の有無**: 無し。ファイル内の残存は `awaitConvergence` ループ末尾の `Thread.yield()` (`AttachOrderRestoreTest.kt:109`) のみで、これは固定時間を待つものではなくスケジューラへの譲渡ヒントであり、kotlin-impl-skill `references/testing.md` の禁止事項「テストで `Thread.sleep` を使う（実時間を待つ）」には当たらない。
- **待機条件の妥当性**: 条件は `committedTexts(view)` (= `internalMainListAdapter().currentList` から取り出した平坦リスト) と `internalTheme()` の一致。`currentList` は `AsyncListDiffer` が**コミット済み**のリストなので、バックグラウンドの差分計算がメインスレッドへ post されて反映された時点でのみ成立する。待ちたい対象そのものを観測しており、収束判定として正しい。
  さらに Theme 側の順序も問題ない — `KsSettingsView.applyThemeInternal` (`KsSettingsView.kt:614-638`) は `internalTheme` 代入・`RecyclerView` 背景色・`notifyItemRangeChanged`・`applyDecoration(style)` を同一呼び出し内で同期実行するため、`internalTheme() == newTheme` が成立した時点で背景色と `ItemDecoration` も更新済みである。待機後に置かれた背景色・decoration のアサーションがレースで先走る余地はない。
- **タイムアウト時の挙動**: 黙って通過しない。期限超過時は `org.junit.Assert.fail` (import は `AttachOrderRestoreTest.kt:21`) を呼んで `AssertionError` を投げる。失敗メッセージにその時点の `committedTexts` と `internalTheme` を載せているため、後続アサーションへ到達しなくても診断できる。既定 5,000ms は Robolectric の差分コミット待ちに対して十分な余裕がある。
- **シナリオ・アサーション内容の不変性**: 期待値が `expectedRows` / `restoredRows` / `initialRows` へ括り出されただけで、値も検証項目も変わっていない。収束が attach 時の再取り込みによることを示す対照アサーション (「取り付け前は Store 更新が Host に届かない」「取り付け前は Theme も Host に届かない」「detach 中は内容更新が Host に届かない」) も従前どおり残っている。
- **本体コード無変更**: `git status` に `android/**/src/main` の変更は無い。待機条件が使う `internalMainListAdapter()` はテスト用アクセサとして既存 (`KsSettingsView.kt:867`)。

## 修正2: iOS `resyncFromStore()` の doc コメント追記 — 対応妥当

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `resyncFromStore()` doc コメント

`- Important:` として「本メソッドは `root` / `currentTheme` の取り込みのみを行い、表示系の派生状態 (snapshot・supplementary の再構築) は更新しない」「それらの再構築は直後の `applyFullSnapshot` / `applyBackgroundColor` が担う前提」「呼び出しは `viewDidLoad` のこの並び (resync → full snapshot → 背景色) の中でのみ行うこと」が明記された。review-001 Minor 2 の推奨修正のうち「doc comment に呼び出し順序の前提を明記する」側を満たしており、将来の再利用時に不整合を作る危険は文書として塞がれた。diff はコメント行の追加のみでコード動作は不変。ADR 参照 (`core/ADR-0019` / `core/ADR-0005`) はコメント規約の許容参照であり、デルタスペック構文キーワードの混入も無い。

## 追加の指摘

### [🔵 Suggestion] 待機条件と後続アサーションが同じ期待値を共有するため、回帰はタイムアウトとして現れる

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AttachOrderRestoreTest.kt:237-247` / `275-278` / `297-304`

**問題点**: `awaitConvergence` の条件が後続 `assertEquals` と同一の期待値を使うため、実装が回帰した場合そのアサーションが差分付きで落ちることはなく、必ず 5 秒後の `awaitConvergence` のタイムアウト `fail` として現れる。条件ベース待機に本質的に伴う性質で、失敗メッセージに実際の `committedTexts` と `internalTheme` が載る設計になっているため診断性は保たれており、実害はない。

**推奨修正**: 不要。回帰時の失敗の出方 (assertEquals の diff ではなく 5 秒後のタイムアウト) だけ、将来この失敗に出会う人が面食らわないよう認識しておけばよい。

## 判定の更新

review-001 の Critical 0 / Major 0 は変わらず、Minor 2 件のうち **Minor 2 (`resyncFromStore()` の順序契約)** は本サイクルで解消。**Minor 1 (`maui/README.md` の旧制約)** は意図どおり未対応のまま残る (`docs-refresh` 専権のため実装フェーズ対象外)。Suggestion は既存 3 件 + 本サイクル 1 件で計 4 件、いずれも修正不要。

**APPROVED を維持する。** アクションプランの残件は以下のみ。

1. (蒸留フェーズ) concepts (`ios/api/ios-native-host.md`) への復元契約の書き起こし
2. (蒸留後・オーナー起動) `docs-refresh` で `maui/README.md`「既知の制約」の旧記述を差し替える
