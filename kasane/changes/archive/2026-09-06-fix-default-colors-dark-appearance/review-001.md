# レビュー結果: fix-default-colors-dark-appearance (001 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペック 6 本の Requirement / Scenario はほぼ全面的に満たされており、Android の「解決点を 1 つに寄せる」設計 (design Decision 6)・iOS の dynamic `UIColor` 化・bridge の `Unspecified` 写し・視覚証跡の取り方はいずれも質が高い。テストは実描画値 (RecyclerView の背景 drawable・TextView の `currentTextColor`・`UIListSeparatorConfiguration`・画素サンプリング) を観測点に取っており、内部値だけを見て緑になる構図を避けている。deviation.md の 7 件はいずれも記録済みの合意差分として扱った。

一方で、`KsSettingsView` の `init` が **未解決の `Theme()` で `ItemDecoration` を組み立ててから** 未指定色を解決しており、`applyDiff` の非 Full 経路だけで内容を入れる公開経路では `Color.Unspecified` が ARGB へ変換される (spec の SHALL NOT に該当)。修正は 2 行の並べ替えで閉じる。あわせて、新規に書いた **公開 doc コメント 2 箇所に ADR ID** が入っており comment-policy に反する。いずれも軽微だが安価に閉じられるため CHANGES_REQUESTED とする。

### 実行した検証

- Android: `./gradlew test` (`android/`) — 216 の結果 XML を集計して **2796 tests / 0 failures** (debug + release)。差分なしの再実行のため Gradle は UP-TO-DATE、結果 XML は現ソースに対応
- iOS: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` — バンドル集計行の合算で **1021 tests / 0 failures** (Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UI 666)、`** TEST SUCCEEDED **`
- `scripts/comment-policy-lint.py` 0 件 (778 ファイル)、`scripts/local-path-lint.py` 0 件、`scripts/identity-lint.py` 0 件
- 視覚証跡: `ui/verification/` 30 枚 + `ui/verification/baseline-light/` 16 枚 + README を実在確認。baseline は分岐元 `db2d1f2` で撮影した旨と撮影条件が記録されており、brief.md の照合記録 (再照合を含む) と対応する。Android Native の ButtonCell 乖離が再撮影で解消された経緯と、iOS / MAUI iOS を撮り直していない理由 (影響範囲外) も明記されている
- Swift 6 言語モードの一時設定ビルドは本レビューでは再実行していない (実装コードの書き換え禁止のため)。`ios/Package.swift` に差分が残っていないことは確認した

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (always) |
| cross/test-execution.md | テスト実行・テスト結果の報告 |
| cross/sample-parity.md | `samples/android` の `SampleTheme` を触る |
| ios/swift6-language-mode-check.md | `ios/Sources/` を触る変更の完了判定 |
| core/ADR-0030 (proposed) / core/ADR-0009 / android/ADR-0020 / cross/ADR-0016 | 本 change の設計根拠と隣接決定 |
| concepts/core/styling/style-resolution.md | 実効値解決の現行記述 |
| lessons/code-review.md (L-001) / lessons/process.md (L-002・L-003・L-005・L-006) | 昇格済みルール |

core / android のドメイン handbook は現時点で存在しない。`cross/public-identifiers.md` / `release-procedure.md` / `runtime-behavior-verification.md` / `user-skill-api-listing.md` / `aiforms-origin-reference.md` は担当範囲に当たらないため本文まで読んでいない。

## 指摘事項

### [🟡 Minor] `init` が未解決の `Theme()` で ItemDecoration を組み立てる — `applyDiff` の非 Full 経路で `Unspecified` が ARGB へ落ちる

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:266`（`applyDecoration(style)`）と同 `:270-271`（`resolvedDarkTheme` / `internalTheme` の解決）

**問題点**:
`init` は `applyDecoration(style)` を先に呼び、その後で `internalTheme = themeBacking.resolvedFor(...)` を代入している。`applyDecoration` は `internalTheme` を読むため、構築直後の `ItemDecoration` は**フィールド初期化子の `Theme()`（全色 `Color.Unspecified`）**を保持する。

本 change 前は `Theme()` が固定のライト既定色を持っていたため、この順序でも装飾は妥当な色を持っていた。型変更後は、`ClassicSectionDecoration.kt:63` の `theme.separatorColor.toArgb()` と `ModernSectionDecoration.kt:95/122` の `cellBackgroundColor` / `backgroundColor` の変換が `Color.Unspecified.toArgb()`（透明黒）になる。

`applyDecoration` を再度呼ぶのは `applyRootDirect`（`:605`）・`applyThemeInternal`（`:817`）・`style` setter だけなので、**公開経路のうち `bind(store)` も `applyDiff(SettingsRootDiff.Full)` も通らずに `applyDiff(InsertSection)` / `InsertCell` だけで内容を入れる使い方**では、初期装飾がそのまま使われて separator が透明・Modern の箱が透明になる。`setRootDirect` は internal であり、素の View に内容を入れる公開手段は `bind` と `applyDiff` の 2 つなので、この経路は利用者から到達可能である。

settings-view-android-ui「未指定色の外観解決」の「描画時に `Unspecified` が ARGB へ変換されることはない (SHALL NOT)」に抵触する。design.md Risks が名指しした「解決漏れが透明色として現れる」型の残りでもある。

**推奨修正**: `init` 内で `resolvedDarkTheme` / `internalTheme` の解決を `applyDecoration(style)` より前へ移す（2 行の並べ替え）。あわせて、`applyDiff(InsertSection)` を最初の内容操作にした素の View で `internalCurrentDecoration()` の Theme が解決済みであることを固定する回帰テストを 1 本足すと、同じ順序依存が再発したときに検出できる。

### [🟡 Minor] 新規に書いた公開 doc コメントに ADR ID が入っている（comment-policy 違反）

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:22`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewDefaults.kt:11`

**問題点**:
handbook cross/comment-policy.md「公開メンバーの doc コメント」は、**ADR ID を含むリポジトリ運用由来の語を公開 doc コメントに書かない**ことを求め、設計根拠は非公開の実装側コメントに置くよう定めている。本 diff は `public data class Theme` の KDoc（`Theme.kt:22`）と `public object KsSettingsViewDefaults` の KDoc（`KsSettingsViewDefaults.kt:11`）に、新たに `（core/ADR-0030）` を書き足している。

`comment-policy-lint.py` は ADR ID を許容参照として通すため、この型は検査に掛からず（規約本文が「検出 0 件は適合の証明にならない」と明記）、コードレビューが判定する範囲にあたる。

なお `internal` の `KsThemePalette.kt:11` / `EffectiveStyle.kt` 側の ADR 参照は公開面ではないため規約どおりで問題ない。

**推奨修正**: 公開 KDoc からは ADR ID を落とし、機能と契約だけを述べる（例: `Theme.kt:22` は「未指定の色は描画時に現在の外観の既定セットへ解決される。」で完結する）。根拠を残したいなら `internal` の実装側コメント（`KsSettingsViewDefaults.resolvedFor` の KDoc は internal なのでそこは可）へ寄せる。

### [🟡 Minor] `darkTheme()` / `lightTheme()` を端末の外観と食い違う組み合わせで渡すと、title / description だけ逆の外観の既定になる

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewDefaults.kt:29-56`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:115`（`darkTheme` の出所は各 ViewHolder の `views.root.context.isKsDarkAppearance()`）

**問題点**:
deviation.md の裁定 B' により、factory と `resolvedFor` は `cellTitleColor` / `cellDescriptionColor` を埋めず、実効 style の最終段が **Context の夜間モード**から選ぶ。この結果、`KsSettingsView(theme = KsSettingsViewDefaults.darkTheme())` を端末がライトのまま渡すと、Cell 背景は `#1C1C1E`、title は `#000000` に解決され、判読できない組み合わせになる（逆向きも同様）。

乖離そのものはオーナー合意済みで違反として扱わないが、**その残余として公開 factory に到達可能な判読不能の組み合わせが残る**点は記録に値する。本 change 前も title 既定が同梱テーマの `textColorPrimary`（外観駆動）だったため回帰ではなく、`KsSettingsViewDefaults` の KDoc に `copy` で明示する回避策も書かれている。ただし、この組み合わせを固定するテストは無い（`DSLDefaultThemeAppearanceTest` は外観と factory が一致する対だけを検証している）。

**推奨修正**: 修正か記録かはオーナー判断で足りる。最小案は、`KsSettingsViewDefaults` の KDoc に「これらの factory は端末の外観と同じ側を選んで渡す前提である」ことを 1 文足し、`lightTheme()` を夜間で渡した場合の実効 title を固定するテストを 1 本足して契約として明示すること。恒久対応（`Theme` に ButtonCell 既定用の独立フィールドを設け、factory が title / description を埋められるようにする）は ADR-0030 の領分なので本 change では扱わない。

### [🔵 Suggestion] `ButtonCellViewHolder.bind` が同じ外観判定を 2 回引く

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCellViewHolder.kt:89` と `:97`

**問題点**: `views.root.context.isKsDarkAppearance()` を 1 回の bind 内で 2 回呼んでいる。`resources.configuration` の読み出しが行ごとに 2 回走る点は些細だが、2 つの引数が同じ値であることがコード上で保証されない形になっている。

**推奨修正**: `bind` の冒頭で `val darkTheme = views.root.context.isKsDarkAppearance()` を取り、両方へ渡す。

### [🔵 Suggestion] 「表示中の選択面は描き直さない」半分がアサートされていない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ThemeAppearanceResolutionTest.kt`（`選択面は開き直すと新しい外観の配色になる`）

**問題点**: settings-view-android-ui「未指定色の外観解決」は「表示中の選択面・カレンダーの選択面は開いた時点の解決済み値で描かれ、**表示中の外観変更では描き直さず**、次回表示から新しい外観の値を使う (SHALL)」と両側を定めている。テストは切替後に `PickerSheetStyle` を組み立て直した値（＝開き直し側）だけを見ており、「開いたままのシートが再配色されない」側は観測していない。

**推奨修正**: 実際にシートを開いた状態で夜間へ切り替え、シート内の実描画色が切替前の値のままであることを 1 件足す。難しければ「意図的に検証対象外」とテスト側のコメントで明示する。

### [🔵 Suggestion] 同じファイル内に残る既存の公開 doc コメントの ADR 参照

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:42`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt:18`、`ios/Sources/KsSettingsViewUI/Theme.swift:22`

**問題点**: いずれも本 diff が触った公開型の doc コメントに `core/ADR-0009` が残っている既存債務。lessons process L-005 の「本 change で触れたファイル内で数行で閉じるならその場で直す」に当たる範囲。

**推奨修正**: 上の Minor を直すついでに同じ方針で整理する。判断が要るなら本 change では見送り、記録だけ残す。

## 確認して問題が無かった観点

- **仕様充足**: settings-view-android-ui / settings-view-ios-ui / maui-bridge / samples-{android,ios,maui} の Requirement と Scenario に対応するテストまたは視覚証跡が揃っている。tasks.md のチェックは 9.1（蒸留への申し送り）以外すべて実装・証跡と対応しており、虚偽チェックは見つからなかった
- **足場凍結**: `specs/` / `proposal.md` / `design.md` / `exploration.md` に diff は無い（変更は `tasks.md` のチェックと `ui/brief.md` の照合記録の追記のみ）
- **deviation**: 7 件の乖離と 5 件の `[付随修正]` はいずれも本 change が触れたファイル内・数行規模に収まっており、`SectionBoxMetrics` の `takeOrElse` 化・`EntryCellViewHolder` の `isSpecified` 化・`SYSTEM_BLUE_ARGB` 削除はテストで担保されている
- **解決点の単一化**: `EffectiveStyle.from` / `ItemDecoration` / Header・Footer ViewHolder / `PickerSheetStyle` / `resolveDatePickerDialogColors` / `RecyclerView` 背景の 6 消費者がすべて `internalTheme`（解決済み）を読む。`toArgb()` の変換点を全数追い、上記 Minor の 1 経路以外に未解決 Theme が届く経路は見つからなかった
- **構造更新経路**: `setRootStructureOnly` の新設で Full diff・Section 置換・可視性変更・Store 再同期が `themeBacking` を書き戻さなくなっており、回帰 Scenario もテスト済み
- **外観判定源の一致**: `Context.isKsDarkAppearance()` は `ksThemedContext()` と同じ `resources.configuration.uiMode` を見る。行の Context は `androidx.appcompat.view.ContextThemeWrapper` で `getResources()` をラップ元へ委譲するため、構成変更後も現在の夜間モードを返す
- **iOS**: `cgColor` を layer に置く箇所は `SectionBoxDecorationView` と `KsCheckBoxView` の 2 つだけで、前者に `registerForTraitChanges` / iOS 16 フォールバックが入り、後者は対応済み（tasks 5.3 の再確認どおり）。`Theme.==` は `isEqual` 判定だが既定値が static の単一インスタンスのため `Theme() == Theme()` は保たれ、既定定数を明示した Theme との等価性もテストで固定されている
- **bridge**: `KsBridgeTheme.resolve()` / `KsBridgeCellStyle.resolve()` は `?: Color.Unspecified` に単純化され、全 null / 一部明示の両方がテストされている。wire 形式（項目・型・null の意味）は不変。iOS bridge は `?? base.x` のままで、`base` の static が dynamic になったことで自動的に追随する
- **公開面**: `Theme.DEFAULT_*` の色定数の削除に対し、リポジトリ内の残存参照は `skills/`（Non-Goal、docs-refresh の責務）と `kasane/changes/archive/` の記録だけで、ビルド対象に壊れる参照は無い
- **テストの手抜き**: 追随した既存テストは全体として弱化していない。`BasicCellsTest` の ButtonCell 既定は「alpha が 0xFF」から「ライブラリ既定色そのもの」へ、`InputCellsTest` の EntryCell 入力色は同梱テーマ由来からライブラリ既定へと、いずれも検出力が上がっている
- **sample-parity**: `SampleTheme` の変更は Sample 内部 API の引数型のみで、文言・画面構成・デモデータは不変。3 面の対応関係は保たれている
- **証跡の実在と対応 (lessons L-003)**: 視覚証跡は実在し、ButtonCell の乖離修正後に Android Native / MAUI Android を撮り直した旨と、撮り直していない面の理由が brief.md に明記されている。個人要素の確認も両回とも記録されている

## アクションプラン

1. `KsSettingsView.kt:266` の `applyDecoration(style)` を `:270-271` の解決の後ろへ移し、`applyDiff(InsertSection)` を最初の内容操作にした場合の装飾 Theme を固定する回帰テストを 1 本足す
2. `Theme.kt:22` / `KsSettingsViewDefaults.kt:11` の公開 doc コメントから ADR ID を外す（同じファイル内の既存 ADR 参照もついでに整理するかはオーナー判断）
3. `KsSettingsViewDefaults` の factory を端末の外観と食い違って渡した場合の扱いを、KDoc の 1 文とテスト 1 本で明示する
4. （任意）`ButtonCellViewHolder` の外観判定の一本化、開いたままの選択面が再配色されないことのアサート追加
