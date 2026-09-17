# レビュー結果: scroll-indicator-visible-not-applied (001 回目)

**日付**: 2026-09-17
**判定**: CHANGES_REQUESTED

## サマリー

合意済みスコープ (a)〜(e) の到達状態は、設定リスト本体については満たされている。Android は同梱テーマ経由で `android:scrollbars` を届ける手当てが正しく、`isVerticalScrollBarEnabled` の適用点も初期構築・Theme 差し替え・Store 経路の 3 箇所すべてを押さえている。iOS の `showsVerticalScrollIndicator` と Header / Footer 背景色も、適用範囲が Android と揃っており、テストは Theme 差し替え・Store 更新・ダーク解決まで検証していて回帰検出力がある。ビルドとテストは両 platform とも全件成功 (下記)。

ただし Android の適用手段が**同梱テーマ全体への `recyclerViewStyle` 差し込み**であるため、設定リスト以外の内部 `RecyclerView` (選択面の候補リストとホイール) にも `android:scrollbars="vertical"` が届き、`Theme.scrollIndicatorVisible` の制御外でスクロールバーが描かれるようになる。これが唯一の Major であり、適用点を設定リストに絞れば解消する。

### 実行したビルドとテスト

| platform | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,…'` (ios/ で実行) | `** TEST SUCCEEDED **` / 1037 tests・0 failures (バンドル集計行の合計: Bridge 166 + Core 88 + SwiftUI 96 + TestSupport 7 + UI 680) |
| Android | `./gradlew test --rerun-tasks` (android/ で実行) | `BUILD SUCCESSFUL` / 2870 tests・0 failures (debug + release の `TEST-*.xml` 合計) |
| lint | `scripts/comment-policy-lint.py --summary` / `scripts/local-path-lint.py` | 禁止 0 件 (検査対象 798 ファイル) / 検出なし |

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加コメントは自己完結しており、外部参照は `android/ADR-0020` の ID 形式のみ。作業文書パス・変更 ID・デルタスペック構文キーワードの混入なし。公開 doc コメント (`KsSettingsView` の RecyclerView プロパティは private、`registerCustomCell` の `@param` は既存文) にも内部用語の持ち込みなし
- `kasane/handbook/cross/test-execution.md` (テスト実行・結果報告) — iOS は Simulator 全件、Android は `--rerun-tasks` 付き全件で実行し件数を併記。新規テストの待機は iOS が `awaitCondition` / `awaitInitialRender` の条件ベース待機、Android は `idle()` と layout の明示駆動で、固定秒数待機の持ち込みなし
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合修正の完了判定) — `evidence/` に Android スクロールバー / Android Header・Footer 背景 / iOS Header・Footer 背景の 3 枚。Android のスクロールバー描画は実機画像で確認できる
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/**` を触る変更の完了判定) — 下記「確認できていないこと」参照
- `kasane/decisions/android/0020-*` (accepted) — ライブラリ所有 UI は同梱テーマ、利用者所有コンテンツはホストのテーマ。本 change の `ksUserContentContext()` 導入はこの決定の維持側の手当てであり、違反なし
- `kasane/lessons/code-review.md` — 重点観点 L-001 (アサーションの検出力) を適用。「指摘しないこと」は昇格済みルールなし

## 指摘事項

### [🟠 Major] 同梱テーマへの `recyclerViewStyle` 差し込みが、設定リスト以外の内部 RecyclerView にもスクロールバーを付ける

**該当箇所**: `android/kssettingsview/src/main/res/values/themes.xml:15`

**問題点**:
`Theme.KsSettingsView.Internal` は**ライブラリ所有 UI 全体**の既定テーマであり、選択面 (`PickerSelectionSheet` / `NumberSelectionSheet` / `DateSelectionSheet` / `TimeSelectionSheet`) の `BottomSheetDialog` も `hostContext.ksThemedContext()` から生成される。`Dialog` が被せる `ContextThemeWrapper` は base のテーマを引き継いでから overlay を重ねるため、`recyclerViewStyle` はシート側の Context にもそのまま残る。

シート内のリストは `SelfContainedRecyclerView(context) : RecyclerView(context)` (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SheetChrome.kt:322`) で、本 change が利用しているのと同一のコンストラクタ (defStyleAttr = `recyclerViewStyle`) を通る。該当は 2 箇所:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:256` — 候補リスト
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:119` — ホイールの可動部 (Number / Date / Time の各シートが使う)

結果、これらにも `android:scrollbars="vertical"` が渡り、`View` の構築時に縦スクロールバーが有効化されてスクロール中に描画される。とくにホイールは中央の選択帯を持つ回転 UI で、右端にスクロールバーが出るのは意図した見た目ではない。さらに実行時の表示切替 (`applyScrollIndicatorVisible`) は設定リストの `RecyclerView` にしか効かないため、利用者が `Theme.scrollIndicatorVisible = false` を指定してもシート側のスクロールバーは消えない。合意済みスコープは「設定リストの縦スクロールバー」であり、この波及は要求されていない。

根拠は本 change のテスト自身にある: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollIndicatorVisibleTest.kt` の対比アサーション (同梱テーマ経由なら `verticalScrollbarThumbDrawable` が非 null、ホスト Context 直だと null) が、素の `RecyclerView(Context)` がテーマの `recyclerViewStyle` からスクロールバーの描画状態を受け取ることを示している。`SelfContainedRecyclerView` は同じ経路を通る。

**推奨修正**:
style の適用点を設定リストの `RecyclerView` だけに絞る。たとえば次のいずれか。

- `Theme.KsSettingsView.Internal` からは外し、`recyclerViewStyle` を差す ThemeOverlay を `KsSettingsView` の RecyclerView 生成箇所 (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:82`) でだけ被せる
- exploration で保留にしていた「XML から inflate する」案に切り替える
- 最小手として `SelfContainedRecyclerView` を `RecyclerView(context, null, 0)` で構築し、選択面側を `recyclerViewStyle` から明示的に opt out させる

いずれの手を採っても、シート側に見た目の変化がないことを Sample の Picker / NumberPicker / DatePicker / TimePicker の選択面で確認し、証跡を `evidence/` に足すのが望ましい。なお、レビュー側は「コードを書かない」制約のためプローブテストで実測していない (機構と既存テストの対比アサーションからの判定)。

### [🟡 Minor] 内部 RecyclerView は夜間モードの切り替わりに追従しない Context を抱える

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:82`

**問題点**:
`KsThemedContext` は「生成時の夜間モードを覚えて、変わっていたら作り直す」設計 (`ui/KsThemedContext.kt` の `ksThemedContext()`) で、Activity を再生成しないホストでの外観切替に対応している。しかし `recyclerView` はプロパティ初期化時に一度だけ解決した Context を保持し続けるため、外観が切り替わっても作り直されない。行の Context は `parent.ksThemedContext()` が再解決し、Theme の色も `reresolveThemeIfAppearanceChanged` が解決し直すので追従するが、`RecyclerView` 自身が構築時にテーマから解決したスクロールバーの drawable (と edge effect の色) は旧外観のまま残る。

影響は小さい (スクロールバーの thumb 色が旧外観のまま / 次の Activity 再生成で解消) が、「同梱テーマは夜間モードで作り直す」という既存の約束から外れる唯一の View になるため、意図的にそうしているのか判別できない。

**推奨修正**:
現状の挙動を受け入れるなら、`recyclerView` の doc コメントに「一覧自身は生成時の Context を保持し続ける (追従対象は Theme の色と行の Context)」旨を 1 行足して、後から読む人が抜けと誤解しないようにする。追従させる場合は外観切替の経路でスクロールバー drawable を差し替える (`verticalScrollbarThumbDrawable`、minSdk 29 で利用可) 形になる。

### [🔵 Suggestion] 既定値のテストは `applyScrollIndicatorVisible` を消しても通る

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollIndicatorVisibleTest.kt` (`既定 Theme では縦スクロールバーが有効になる`)

**問題点**:
`android:scrollbars="vertical"` が指定された View は構築時点で縦スクロールバーが有効になるため、このアサーションは `applyScrollIndicatorVisible` の呼び出しを全部消しても通る。同ファイルの 4 番目のテスト (`false` → `true` の往復) が既定値 `true` の適用を実質的に担保しているので検出力の穴にはなっていないが、テスト名からは「既定 Theme の適用経路を守っている」と読めてしまう。

**推奨修正**:
テスト名またはメッセージを「XML 既定でスクロールバーが有効な View になっている」と読める形に寄せるか、`false` を経由してから既定 Theme を当て直す形にして、適用経路を通ることを条件に含める。

### [🔵 Suggestion] iOS の背景色は 1 スタイル分の証跡しかない

**該当箇所**: `evidence/ios-header-footer-background.png`

**問題点**:
`UIBackgroundConfiguration.clear()` を土台にした塗りは inset / 角丸を持たないため、Header / Footer の領域全体 (全幅) が塗られる。Android と同じ範囲という判断は妥当だが、証跡は 1 スタイル分で、Modern (inset 系) の一覧で section の箱と全幅の Header 背景が並んだときの見え方が確認できない。

**推奨修正**:
Modern スタイルの iOS Sample で Header / Footer を 1 枚撮って `evidence/` に足す (Android の同スタイルと並べれば parity の主張が一段強くなる)。

## 確認できていないこと

- **Swift 6 言語モードの一時設定ビルド** (`kasane/handbook/ios/swift6-language-mode-check.md`) — `ios/Sources/` を触る変更の完了判定に必要だが、この確認は `ios/Package.swift` の一時編集を伴うため、コードを書き換えない制約のレビュー側では実施していない。`change` 配下にも実施の記録がない。完了判定の前に実装側で実施し、error 0 件と `Package.swift` の差分 0 件を報告に含めること
- **選択面での見た目変化の実機確認** — Major の指摘に対応した後、Picker / NumberPicker / DatePicker / TimePicker の選択面で確認が要る

## 所見 (指摘ではない)

- iOS の Root Header / Footer は文字色が `headerTextColor` 流用のままで、Android (`RootTextAccessoryViewHolder` は Footer に `footerTextColor`) と割れている。本 change 以前からの差で、背景色だけ Android の規則に揃えたことはコード側のコメントで明示されており、スコープ外として扱うのが妥当
- `ksUserContentContext()` の導入は、内部 RecyclerView を同梱テーマ経由に変えたことで利用者所有コンテンツの Context 解決が壊れるのを防ぐ必須の手当てであり、`HostWrappedContextUserContentTest` がホスト側ラッパのテーマ属性を解決できることを `colorPrimary` の差で検証していて、`android/ADR-0020` の回帰検出になっている。スコープ外の抱き合わせではなく、この変更に必要な範囲に収まっている
- 公開 API・既定値の変更はなし (`Theme` の宣言・bridge・MAUI facade は無変更)。既定 `true` のまま Android でスクロールバーが出るようになる点は exploration の決定どおりリリースノート側の課題

## アクションプラン

1. [Major] `recyclerViewStyle` の適用点を設定リストの `RecyclerView` に限定し、選択面のリスト・ホイールにスクロールバーが出ないことを確認する
2. [確認事項] Swift 6 言語モードの一時設定ビルドを実施し、error 0 件を報告に含める
3. [Minor] 内部 RecyclerView が夜間モード切替で作り直されない点を、コメントで明示するか追従させる
4. [Suggestion] 既定値テストの意図を名前かアサーションに反映する / Modern スタイルの iOS 証跡を 1 枚足す
