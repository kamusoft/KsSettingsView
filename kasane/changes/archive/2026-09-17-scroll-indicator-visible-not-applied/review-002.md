# レビュー結果: scroll-indicator-visible-not-applied (002 回目)

**日付**: 2026-09-17
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペック 3 本の Requirement は、実装としてはおおむね満たされている。review-001 の Major (同梱テーマ全体への `recyclerViewStyle` 差し込みが選択面と回転ホイールへ波及する) は、`ThemeOverlay.KsSettingsView.ScrollIndicator` を生成箇所にだけ重ねる形へ直っており、その修正には**ミューテーションで実測した検出力のある回帰テスト**が付いている (probe C)。Minor / Suggestion・確認事項 (Swift 6 言語モード) もすべて解消済み。ビルドとテストは 3 platform とも全件成功。

ただし、**本 change の中核である「設定リストの `RecyclerView` を同梱テーマ + スクロールバー用オーバーレイの Context から生成する」配線に、回帰検出力を持つテストが 1 件も無い**ことをミューテーションで実測した (probe A)。tasks 1.3 / 1.4 が「反映処理を消すと落ちるテスト」を担保としているのに対し、実際には消しても落ちない。これが唯一の Major であり、観測点を実装自身の代入から切り離せば解消する。

### 実行したビルドとテスト

| platform | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=<機種名>'` (`ios/` で実行) | `** TEST SUCCEEDED **` / **1045 tests・0 failures** (バンドル集計行の合計: Bridge 166 + Core 88 + SwiftUI 98 + TestSupport 7 + UI 686) |
| Android | `./gradlew test --rerun-tasks` (`android/` で実行) | `BUILD SUCCESSFUL` / **2896 tests・0 failures** (debug + release の `TEST-*.xml` の `tests` / `failures` 合計) |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | **564 合格・0 失敗** |
| lint | `scripts/comment-policy-lint.py --summary` / `local-path-lint.py` / `identity-lint.py` | 禁止 0 件 (検査対象 802 ファイル) / 検出なし / 検出なし。`--advisory` の要確認 1 件は下記 Minor |

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加コメントは自己完結しており、作業文書パス・変更 ID・デルタスペック構文キーワードの混入なし。**公開メンバーの doc コメントの節で 1 件抵触** (下記 Minor)
- `kasane/handbook/cross/test-execution.md` (テスト実行・結果報告) — iOS は Simulator 全件でバンドル集計行のみを合算、Android は `--rerun-tasks` 付き全件、MAUI は facade 全件。いずれも件数を併記。新規テストの待機は iOS が `awaitCondition` / `awaitInitialRender`、Android が `awaitConvergence` と `idle()` + 明示 layout で、固定秒数待機の持ち込みなし。`HeaderFooterBackgroundDefaultTest` / `SheetScrollbarTest` は絞り込み結果が空でないことを「前提:」アサーションで確かめており、0 件ループにならない形になっている
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動が絡む不具合修正の完了判定) — `evidence/` に 16 枚 + README。Android のスクロールバーは before / after (ホイール波及) を同一端末・同一操作で撮り比べており、修正が原因に効いた証明になっている。未取得分は README の「取れていない確認」に明示 (下記 Minor で tasks 側との齟齬を指摘)
- `kasane/handbook/cross/sample-parity.md` (`samples/` の見た目) — samples は iOS / Android / MAUI とも Header / Footer 背景を明示済みで、既定を透明にしても画面は変わらない (tasks 4.4 の主張どおり)
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/**` を触る変更の完了判定) — `evidence/README.md` に 2 回目の実施記録 (error 0 件・`ios/Package.swift` の shasum 一致) があり、review-001 の確認事項は解消
- `kasane/decisions/android/0020-*` (accepted) — ライブラリ所有 UI は同梱テーマ、利用者所有コンテンツはホストのテーマ。`ksUserContentContext()` の導入と `SectionAccessoryViewHolders` / `KsCellRegistryCustomCell` の置換はこの決定の維持側の手当てで、`HostWrappedContextUserContentTest` が `assertSame` + `colorPrimary` の差で固定している。ライブラリ所有側の残る呼び出し (`SectionAccessoryViewHolders.kt:111` / `:244`、`CellBaseLayout.kt:120`、各 Sheet) はすべて `ksThemedContext()` のままで、取り違えは無い
- `kasane/decisions/core/0030-*` (accepted) — Decision 1 の既定値のうち Header / Footer 背景だけを置き換える変更であり、accepted な決定との衝突にあたる。改訂 ADR (core/0032) は `proposed` のため、これを根拠に判定は下していない (所見に記載)
- `kasane/lessons/code-review.md` — 重点観点 L-001 (アサーションの検出力) を適用し、ミューテーション 5 本を実測した。「指摘しないこと」は昇格済みルールなし
- `proposal.md` の Impact に記録された「`ui/` (brief・モック) を作らない」はオーナー合意済みの逸脱として扱い、違反として数えていない

## 指摘事項

### [🟠 Major] 設定リストの Context 配線に回帰検出力が無い (ミューテーションで実測)

**該当箇所**:
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollIndicatorVisibleTest.kt:101` (`内部 RecyclerView はスクロールバーの描画状態を持つ`)
- `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/HostWrappedContextUserContentTest.kt:216` (`ホストが被せた ContextThemeWrapper でもスクロールバーの描画状態は同梱テーマから届く`)
- 守るべき対象: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:89`

**問題点**:
両テストは「`verticalScrollbarThumbDrawable` が非 null であること」を、`android:scrollbars` が `recyclerViewStyle` 経由で届いたことの観測点として使っている (テストの doc コメントにその趣旨が書かれている)。ところが同じ観測点は、実装自身の `recyclerView.verticalScrollbarThumbDrawable = thumb` (`KsSettingsView.kt` の `applyScrollbarThumbForAppearance`) でも満たされる。`View.setVerticalScrollbarThumbDrawable` は内部で ScrollabilityCache を作るため、Context が何であれ代入した時点で描画状態が生まれる。`applyScrollbarThumbForAppearance` は構築時の `applyScrollIndicatorVisible` から必ず 1 回走るので、観測時にはすでに作られている。

ミューテーション実測 (`android/` で `./gradlew :kssettingsview:testDebugUnitTest` を対象クラスに絞って実行。いずれも実施後に backup から書き戻し、shasum 一致で原状復帰を確認済み):

| probe | 入れた変更 | 結果 |
|---|---|---|
| A | `KsSettingsView.kt:89` を `RecyclerView(context)` に戻す (オーバーレイを外す) | **18 件すべて green (検出できていない)** |
| B | `PickerSelectionSheet.kt:263` を `SelfContainedRecyclerView(context)` に戻す | `SheetScrollbarTest > 既定の Theme では候補リストにスクロールバーが出る` FAILED (検出できている) |
| C | `recyclerViewStyle` を `Theme.KsSettingsView.Internal` へ戻す (review-001 Major の再現) | `SheetScrollbarTest` の 2 件 FAILED (検出できている) |
| D | probe A + `applyScrollbarThumbForAppearance` を即 return | 上記 2 件 + `外観の切り替えでスクロールバーの thumb が解決し直される` の 3 件 FAILED |
| E | `applyScrollbarThumbForAppearance` のみ即 return (Context は正しいまま) | `外観の切り替え…` の 1 件のみ FAILED |

A と D の差が、観測点を満たしているのがオーバーレイではなく実装側の代入であることの証明になる。tasks 1.3 / 1.4 は「反映処理を消すと落ちるテスト」を担保に挙げているが、Context 配線についてはその担保が成立していない。

副次的に、E が示すとおりオーバーレイと thumb の実行時代入は設定リストに対して互いに冗長で、**どちらが効いているかをテストが区別できない**状態にある。片方を消しても気づけないため、後から「重複しているように見える」方を整理されると、テーマが `android:scrollbarThumbVertical` を解決しない状況 (`applyScrollbarThumbForAppearance` の `if (thumb == null) return` 経路) で静かに壊れる。

**推奨修正**:
観測点を実行時の代入から切り離す。たとえば「設定リストの Context 自体がスクロールバー用の style を運んでいること」を直接観測する形にする — `view.internalRecyclerView().context` から新しい `RecyclerView` を作り、その `verticalScrollbarThumbDrawable` が非 null であることを確かめれば、probe A で落ちるようになる (`SheetScrollbarTest:150` の `同梱テーマから作った RecyclerView にスクロールバーの描画状態が無い` と同じ観測の仕方であり、こちらは実際に検出力がある)。既存の 2 件はそのまま残してよいが、doc コメントの「`recyclerViewStyle` 経由で届き」という主張は、検出力のある側のアサーションへ寄せる。

### [🟡 Minor] 公開 doc コメントに ADR ID を新しく持ち込んでいる

**該当箇所**: `ios/Sources/KsSettingsViewUI/Theme.swift:283` (`public static let defaultHeaderBackgroundColor` の doc コメント末尾の `（core/ADR-0032）`)

**問題点**:
`kasane/handbook/cross/comment-policy.md` の「公開メンバーの doc コメント」は、ADR ID を許容参照であっても内部用語とし、公開 doc コメントに書かず非公開の実装側コメントへ置くことを求めている。`scripts/comment-policy-lint.py --advisory` もこの行を要確認として報告する (機械検査は止めずコードレビューに判定を委ねる建て付け)。bridge 層に同種の既存債務が多数あるが、本 change はそれを 1 件増やしている。

なお参照先の core/ADR-0032 は現時点で `proposed` であり、確定前の ID を利用者から見える面に置いている点でも早い。

**推奨修正**:
公開 doc コメントからは `（core/ADR-0032）` を落とす。前段の「両外観の値を持つ色の形を保つので、この定数を明示した Theme は既定と等価になり、同じ見た目の固定色を明示した Theme は等価にならない」という説明は参照が無くても自己完結しているため、削除だけで文意は壊れない。根拠を残したいなら `internal` 側 (`lightHeaderBackgroundColorValue` 付近) の実装コメントへ移す。

### [🟡 Minor] tasks 5.2 が [x] だが、evidence 側には未実施の確認が残っている

**該当箇所**: `tasks.md` の 5.2 / `evidence/README.md` の「取れていない確認」

**問題点**:
tasks 5.2 は確認項目として「`false` で両 OS とも消える」を挙げて [x] になっている。一方 `evidence/README.md` は、iOS のスクロールインジケータについて「静止画の証跡を取れていない / 実機での目視が残っている」と明記している。README 側が正直に開示しているので隠れた虚偽ではないが、チェックボックスだけを見ると完了したように読める。完了判定に使われる面と証跡の面で食い違いが残るのは、後から状態を判断できなくする。

iOS 側は実 window に載せたテスト (`showsVerticalScrollIndicator` の観測) があり、Simulator の `simctl io screenshot` にインジケータが写らないという制約も現実的なので、**実機目視をやり切るか、tasks 側に未了として残すか**はオーナーの判断で構わない。どちらかに揃える必要がある。

**推奨修正**:
tasks 5.2 を「iOS の実機目視を除いて完了」と読める形に書き分ける (あるいは項目を分割する) か、実機で 1 回確認して README の「取れていない確認」から落とす。

### [🔵 Suggestion] 参照されていない internal 定数を足している

**該当箇所**: `ios/Sources/KsSettingsViewUI/Theme.swift:379` (`lightFooterBackgroundColorValue`)

**問題点**:
本 change で追加された `lightHeaderBackgroundColorValue` は `defaultHeaderBackgroundColor` から使われているが、`lightFooterBackgroundColorValue` はリポジトリ内のどこからも参照されていない (Sources / Tests とも 0 件)。既存の内部定数は `dark*Value` の系統しかなく、`light*Value` はこの change で新設された系統なので、「対称にしておく」慣習も今はまだ無い。

**推奨修正**:
削除する。Footer 側の値が要るようになった時点で、`darkFooterBackgroundColorValue` と同じ形で足せばよい。

### [🔵 Suggestion] 「表示中の選択面は Theme 差し替えに追従せず、次に開いたときから新しい値に従う」にテストが無い

**該当箇所**: `specs/settings-view-android-ui/spec.md` および `specs/settings-view-ios-ui/spec.md` の「スクロールバー表示の Theme 反映」内の SHALL

**問題点**:
両 platform とも、開いた時点の値を控える実装 (`PickerSelectionSheet` の `scrollIndicatorVisible` / `PickerListViewController` の `let scrollIndicatorVisible`) になっており、前半 (追従しない) は構造上そうなる。しかし後半の「次に開いたときから新しい値に従う」は、Theme を差し替えてからもう一度開く経路を通らないと確かめられず、その形のテストが無い。Android は `PickerCellViewHolder` が bind 時の `theme` から渡す経路なので、theme 差し替え後の再 bind が漏れると静かに古い値のままになり得る。

**推奨修正**:
Android の `SheetScrollbarTest` に、既定で開く → 閉じる → `false` の Theme を代入 → もう一度開く、の 1 ケースを足す。iOS 側は `makePickerList` が毎回 Theme から組み立てる seam を通っているので、同等のケースを足すかは任意。

## 所見 (指摘ではない)

- **review-001 の指摘はすべて解消している**。Major (選択面・ホイールへの波及) は適用点の限定で直り、probe C で回帰検出力も確認できた。Minor (内部 RecyclerView が夜間モードに追従しない) はコメントで済ませず `applyScrollbarThumbForAppearance` による実追従まで実装され、`ScrollIndicatorVisibleTest` の該当ケースに probe E の検出力がある。Suggestion 2 件 (既定値テストのトートロジー / Modern の iOS 証跡) も、false を経由してから既定を当て直す形への書き換えと `ios-header-footer-background-modern.png` の追加で解消済み。確認事項の Swift 6 言語モードも実施記録あり
- `applyScrollbarThumbForAppearance` の外観判定は `context.isKsDarkAppearance()` を読む一方、`onConfigurationChanged` は `newConfig.isKsDarkAppearance()` で `resolvedDarkTheme` を決めている。ホストが `createConfigurationContext` で別 Configuration を持つ Context を渡した場合に食い違い得るが、通常のホストでは View の Context の Configuration は更新済みで届くため実害は想定しにくい
- `typed.getDrawable(0)` で得た thumb を `mutate()` せずに代入している。スクロールバーの thumb は他所と状態を共有して困る drawable ではないため、現状は問題にならない
- iOS の回転ホイール (数値・日付・時刻) は `UIPickerView` / `UIDatePicker` を使っており、spec の「本プロパティの対象にせずスクロールバーを表示しない」は platform の既定で成立している (専用の実装・テストが無いのは妥当)
- iOS の accessory は `UIBackgroundConfiguration.clear()` を土台にする一方、Cell 行は `defaultBackgroundConfiguration()` を土台にしている。塗る範囲を cell 全体にするための意図的な差で、コード側にその旨のコメントがあり、View 形式に色を残さないことは `AccessoryBackgroundColorTests` が押さえている
- core/ADR-0032 は `proposed` のまま。accepted な core/ADR-0030 Decision 1 の既定値を実装が置き換えている状態なので、アーカイブ (蒸留) の前に 0032 を accepted へ上げ、core/ADR-0030 の index 行にも 0031 と同じ形で「一部改訂: 0032」の後方参照を入れておくと、後から読む人が現行の既定値を追える
- MAUI は facade / bridge の実装が無変更で、追加された 4 件のテストが未指定・指定・表示中の変更をそれぞれ押さえている。`GatewayScope.Connect` 後に `Reset()` を挟んで `Single<SetTheme>` を見る形なので、初期適用と変更適用が混ざらない

## アクションプラン

1. [Major] 設定リストの Context 配線に、実行時の thumb 代入に依存しない観測点のテストを足す (probe A で落ちるようにする)
2. [Minor] `ios/Sources/KsSettingsViewUI/Theme.swift:283` の公開 doc コメントから ADR ID を外す
3. [Minor] tasks 5.2 と `evidence/README.md` の「取れていない確認」を揃える (iOS の実機目視を実施するか、tasks 側を未了として書き分ける)
4. [Suggestion] `lightFooterBackgroundColorValue` を削除する / 選択面を開き直したときに新しい値に従うケースを足す
