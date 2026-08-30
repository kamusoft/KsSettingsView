# レビュー結果: adjust-section-spacing (001 回目)

**日付**: 2026-08-25
**判定**: CHANGES_REQUESTED

## サマリー

余白の同値化 (iOS / Android の Section margin と Header/Footer ラベル余白) は、実装・テストとも確定値どおりに揃っており、Classic 既定値の変更が Root H/F の内側余白へ波及する点まで既存テストが追随している。テストは iOS 588 件 / Android 1319 件 × 2 variant が全 pass、samples/ の差分ゼロ、公開 API の変更なし、comment-policy / local-path / identity lint も 0 件で、余白側だけを見れば完成度は高い。

一方、対話中に対象拡大された **Android SwitchCell のオフ色 accent 導出**は、新規に 6 定数 + 3 ヘルパ関数の導出ロジックを持ち込みながら**回帰テストが 1 件も無く**、既存の Switch 色テスト 4 件は導出を定数グレーに戻しても全件 pass する (検出力ゼロ)。さらにその 4 件の説明コメントが「オフ Track → `colorSurfaceContainerHighest` / オフ Thumb → `colorOutline`」という**撤去済みの仕様を現在仕様として記述したまま**残っており、テストと実装が食い違って見える。この 2 点を Major とする。

なお確定値そのもの (22/0/16dp・4pt/dp・blend/彩度/明度の各係数) はオーナーが実物確認して確定済みのため、本レビューでは値の是非は論じない。見た目に影響し得る指摘 (Minor-3 / Minor-4) は「確認の要否をオーナーが決めるための材料」として挙げるに留める。

## 確認した観点と結果

| 観点 | 結果 |
|---|---|
| ビルド・テスト | iOS `xcodebuild test` (iPhone 17 Simulator) **Executed 588 tests, 0 failures**。Android `./gradlew test` は全タスク UP-TO-DATE (= 現ツリーの入力に対する既存結果が有効)、`build/test-results/*/TEST-*.xml` 集計で ui 958 / bridge 161 / compose 120 / core 80、debug・release 各 variant とも failures 0 |
| summary と diff の対応 | 「触ったファイル」の実装 5 / テスト 8 は `git status` と一致。MAUI 無変更の主張も確認 (`maui/KsSettingsView.Maui/SettingsView.cs:1021-1024` で `SectionMargin` が null のままワイヤ値へ委譲され、既定値の複製が無いこと) |
| samples/ 復元 | 検証用 accent の一時付与は完全復元済み (`samples/` の差分ゼロ) |
| 公開 API | 変更なし。導出係数はすべて `private const` (ライブラリ内部) |
| 付随修正の同梱条件 | SwitchCell オフ色は「本務で触るファイル外・別の関心事」だが、オーナー指示による対象拡大であり同梱の是非は論じない。ただし同梱条件④「テストで担保」は満たしていない (Major-1) |
| 足場アーティファクト | S 級・デルタスペック無し。書き換え対象なし |
| lint | `comment-policy-lint` 対象 11 ファイルで禁止 0 件、`local-path-lint` / `identity-lint` とも summary に指摘なし |
| 証跡 | evidence/ に 2 枚実在。ただし iOS 側の記述と実物が食い違う (Minor-6) |

## 指摘事項

### [🟠 Major] SwitchCell オフ色の導出ロジックに回帰テストが無く、既存テストは検出力を持たない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:14-75,128-143` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt:1063-1155`

**問題点**:
本 change で新設された `offTrackColorFrom` / `offThumbColorFrom` / `Int.scaleHsl` と 6 つの係数定数、および `trackDecorationTintList` の明示設定に対応するテストが 1 件も無い。既存の Switch 色テスト 4 件が検証しているのは次だけで、いずれも「オフ色が accent から導出されている」ことを一切固定していない。

- `trackTintList` / `thumbTintList` が非 null であること
- checked 色 != unchecked 色 であること
- オフ時の track 色 != thumb 色 であること

したがって `offTrackColorFrom` / `offThumbColorFrom` を「accent を無視した固定グレー 2 色」に置き換えても 4 件とも pass する。本 change の中身 (accent 追従・Cell ごとの accent への追従・枠線色の一致) はすべて未担保で、次に誰かが色まわりを触ったときに黙って壊れる。`trackDecorationTintList` の checked = 透明という M3 既定との一致も同様。

`lessons/code-review.md` L-001 の観点で言えば、争点は「このアサーションに回帰検出力があるか」であり、ここは静的読解の時点で「導出関数を定数に差し替えても落ちない」ことが確定している。

**推奨修正**: 導出の本質だけを固定する軽量なテストを 1〜2 件足す。値そのもの (確定済みの係数) を焼き付ける必要はなく、次の 3 点で十分に検出力が立つ。

- 異なる accent (例: 赤 / 青) で bind したとき、オフ時の track 色・thumb 色がそれぞれ**異なる**こと (= accent 追従。定数グレー化すると落ちる)
- `SwitchCell.accentColor` を指定した Cell が `Theme.cellAccentColor` ではなく Cell 側の accent に追従すること (オン track と同じ解決順であることの固定)
- `trackDecorationTintList` の unchecked 色が `thumbTintList` の unchecked 色と一致し、checked 色が透明であること

### [🟠 Major] 既存 Switch テストの説明が撤去済みの仕様 (テーマ attr 直参照) を現在仕様として記述したまま

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt:1073` / `:1080` / `:1106` / `:1135` / `:1151`

**問題点**:
オフ色の解決を accent 由来の導出へ変えたにもかかわらず、テスト側の説明コメントは旧実装の説明のまま残っている。特に `:1135` は

> オフ Track → `colorSurfaceContainerHighest`、オフ Thumb → `colorOutline` の異なるトークンで分離する

と、現在の実装が採っていない解決方式を「Material 3 標準に揃え」た現行仕様として断定しており、実装を知らない読者はテストの説明を信じて誤読する。`:1080` の「state_checked = false で グレー (colorOutline 相当)」、`:1106` の「state_checked で onPrimary/outline」、`:1073` の「オフ時 = グレー」、`:1151` の失敗メッセージも同類。

`concepts/cross/conventions/comment-policy.md` の「アーカイブされた過去仕様の説明を書かない / 現在の仕様を現在形で書く」に該当する。同規約が自ら明記しているとおり **lint の検出 0 件は適合の証明にならない** (履歴・旧仕様記述は機械判定の対象外)。summary は「テストは確定値に合わせた期待値・文言更新」と述べているが、この 5 箇所は文言更新から漏れている。

**推奨修正**: 4 テストの KDoc・行コメント・失敗メッセージから旧トークン名を外し、現在の解決 (オフ色は accent と `colorSurfaceContainerHighest` から導出し、オン accent とは明確に区別する) を現在形で書き直す。Major-1 のテスト追加と同じ編集で片付く。

### [🟡 Minor] オフ色の導出係数がダーク配色で潰れる可能性 (未検証)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:14-31,61-75`

**問題点**:
係数はライト配色での実測 (summary: 「実測 L 84〜85%」→「M3 既定 L 90% 水準へ +7% 補正」) に対して決めた**乗算**係数だが、下地の `colorSurfaceContainerHighest` は host テーマが dark のとき暗い色に解決される。ダーク配色を仮定して概算すると、track は `×1.07` で明度がほぼ据え置き (L 約 31% → 33%)、thumb は `×0.65` で暗くなり (L 約 58% → 37%) 両者の明度差が数 % に潰れ、つまみの位置が読み取りにくくなる。変更前はオフ色が `colorOutline` / `colorSurfaceContainerHighest` の直参照で、ライト・ダークとも M3 が用意したコントラストに自動追従していたため、この性質は本 change で新しく入ったもの。

evidence の 2 枚はいずれもライト配色であり、ダークでの見え方は証跡にも summary にも無い。

**推奨修正**: 見た目の確定はオーナーの領分なので、ここでは値の変更を主張しない。ダーク配色での確認を行うかどうかの判断材料としてこの指摘を扱ってほしい。仮に手当てするなら、下地の明度に応じて thumb の明度係数の向き (暗くする / 明るくする) を切り替える形が素直で、係数そのものはライト側の確定値を保てる。

### [🟡 Minor] Root Header / Footer のラベル余白が iOS だけ 4pt 入り、Android は 0dp のまま

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2348,2359` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:294,311-320`

**問題点**:
iOS の 4pt は Section H/F と Root H/F が共有するラベル配置ヘルパに入っているため、Root H/F のラベルにも自動的に効く (`ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:528` の `emptyBottomGap` 期待値が 0 → 4 に変わっているのがその現れ)。対して Android は `SectionTextAccessoryViewHolder.bind` の中でだけ padding を入れ直しており、同じ `createSectionTextView` を使う `RootTextAccessoryViewHolder` は 0dp のまま (`SectionAccessoryViewHolders.kt:284-290` の KDoc も「Root の H/F はここで生成した 0 のまま使う」と明記)。

本 change の主旨が「iOS / Android の同値化」であることを踏まえると、Root H/F だけプラットフォーム差が新規に生じているのは意図した非対称なのか副作用なのかが成果物からは読み取れない。concepts の [list-appearance.md](../../concepts/core/styling/list-appearance.md) にも Root H/F ラベルの内側余白に関する契約は無い。

**推奨修正**: 見た目に触れるためどちらへ揃えるかは指摘に留める。意図的な非対称であれば summary に「Root H/F は対象外」と1行残しておくと蒸留時に concepts へ落とせる。副作用であれば、Android 側で Root にも同じ 4dp を入れるか、iOS 側で Root を除外するかのどちらかで揃うことになる。

### [🟡 Minor] `createSectionTextView` 内に旧根拠のコメントが残り、直上の KDoc と矛盾する

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:294`

**問題点**:
summary は「根拠のない『AiForms 準拠で上下 0』KDoc を実態に合わせ書き換え」としており、KDoc (`:284-290`) からは確かに AiForms 由来の根拠が外れている。しかし同じ関数の本体に

```
// 横方向のみ 16dp 相当、上下は AiForms オリジナル準拠で 0
```

が残っており、6 行上の書き換え後 KDoc (Section H/F では bind が入れ直す、と説明) と食い違う。「上下 0 は AiForms 準拠である」という、今回まさに根拠なしと判断された主張がファイル内に生き残っている状態。

**推奨修正**: 行コメントを生成時点の値の説明だけに書き直す (例: 「横方向のみ 16dp 相当。上下は生成時 0 で、Section H/F では bind が入れ直す」)。

### [🟡 Minor] summary の証跡記述が実物と一致しない (iOS 側)

**該当箇所**: `summary.md:55` / `evidence/ios-section-decoration-final.png`

**問題点**:
summary は iOS の証跡を「Section 装飾デモ (**Modern**) の最終状態」と記述しているが、実物のスクリーンショットは style 切替が **Classic** 側に選択された状態で、Section 箱の角丸も水平余白も無い全幅表示になっている (Android 側の 1 枚が Modern)。したがって `:56` の Android 証跡説明「同画面の最終状態」も、同じ画面ではあるが同じ style ではない。

実害として、本 change で最も踏み込んだ変更 (concepts に明文化されていた「Classic は上下 0」の反転) の証跡が Classic の 1 枚として実在しているのに、summary 上は Modern と書かれているため対応が取れず、蒸留時に「Classic の確定形の証跡は無い」と誤読され得る。

**推奨修正**: iOS 側の記述を Classic に直す (この 1 枚が Classic 既定 margin 反転の証跡であることを明示すると、ADR 候補の裏付けとしても効く)。Modern の証跡も残したいなら 1 枚追加する。

### [🔵 Suggestion] `SectionBoxLayout` の初期 metrics が `resolve` を通さず Classic 全幅の不変条件を破る値になった

**該当箇所**: `ios/Sources/KsSettingsViewUI/SectionBoxLayout.swift:25`

**問題点**:
初期 `metrics` を `SectionBoxMetrics.classicDefaultMargin` から直接組み立てており、`resolve` の「`.classic` では leading / trailing を 0 に落とす」正規化を通らない。変更前は定数が全方向 0 だったため実質同じだったが、現在は leading / trailing 16 を持つ値が「Classic の解決済み metrics」として置かれることになり、`.classic` なら水平 0、という不変条件を型の外側で破っている。現状 `SectionBoxLayout` 内で `metrics.margin` を読む箇所は無いため実害は出ていないが、後から margin を参照する処理を足したときに静かに誤る種になる。

**推奨修正**: 初期値の margin を `.zero` にするか、`SectionBoxMetrics.resolve(theme: Theme(), style: .classic)` の結果を使う。

### [🔵 Suggestion] iOS の `classicDefaultMargin` はリテラル重複で「完全同値」が壊れ得る

**該当箇所**: `ios/Sources/KsSettingsViewUI/SectionBoxMetrics.swift:33-37`

**問題点**:
Android は `CLASSIC_DEFAULT_MARGIN: PaddingValues = MODERN_DEFAULT_MARGIN` と別名で置き、「完全同値」を構造として保証している。iOS は同じ数値リテラルを 2 か所に書いており、片方だけ調整すると宣言 (KDoc の「`.modern` と完全同値に置く」) と実体がずれる。

**推奨修正**: `static let classicDefaultMargin = modernDefaultMargin` に寄せる。表示結果は変わらない。

### [🔵 Suggestion] `Int.scaleHsl` が alpha を落とす

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:46-52`

**問題点**:
`ColorUtils.colorToHSL` は alpha を無視し、`ColorUtils.HSLToColor` は常に不透明を返すため、半透明の accent を指定した Cell ではオフ色だけが不透明化する (オン色は accent の alpha を保つ)。現状 accent は不透明前提で運用されているので実害は出ていないが、関数名からは読み取れない副作用になっている。

**推奨修正**: 元の alpha を保って返すか、KDoc に「戻り値は不透明」と明記する。

### [🔵 Suggestion] Header / Footer ラベル余白 4pt が iOS 側だけマジックナンバー

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2348,2359`

**問題点**:
Android は `SECTION_TEXT_GAP_DP = 4` (`SectionAccessoryViewHolders.kt:302`) として名前付き定数にしているが、iOS は制約の定数式に `4` を直書きしている。両 platform で同値を保つ意図が iOS 側のコードからは見えず、grep でも対応が取れない。

**推奨修正**: iOS も名前付き定数に切り出す。同じファイル内に残る `>= 2` / `<= -2` (領域内に収めるための最小インセット) と値が異なるため、定数名で区別が付くと読み違いも減る。

## アクションプラン

1. **Major-1 / Major-2** (同一ファイル群で一括): `BasicCellsTest.kt` に accent 追従 (異なる accent → 異なるオフ色)、Cell 別 accent の解決順、`trackDecorationTintList` の unchecked = thumb 同色 / checked = 透明 を固定するテストを追加し、あわせて既存 4 件の旧トークン記述を現行仕様の現在形へ書き直す
2. **Minor-6**: summary の iOS 証跡記述を Classic に訂正する (Modern の証跡も残すなら 1 枚追加)
3. **Minor-5**: `SectionAccessoryViewHolders.kt:294` の行コメントを KDoc と揃える
4. **Minor-3 / Minor-4**: ダーク配色での確認要否、および Root H/F のプラットフォーム差を意図とするかを、オーナー判断として決める。意図的な非対称とする場合は summary に 1 行残して蒸留へ送る
5. **Suggestion 群**: 余力があれば 1〜4 と同じ回で片付ける。いずれも表示結果は変えない
