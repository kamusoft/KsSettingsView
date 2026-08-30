# レビュー結果: adjust-section-spacing (003 回目)

**日付**: 2026-08-25
**判定**: APPROVED

## サマリー

review-002 の Major (確定サマリが現物を表していない) と Minor 3 件は**全件解消**した。summary はオン thumb 導出を含む記述へ改められ、関数名・テスト件数・実行件数が現物と一致し、ダーク証跡は現行コード (白 thumb) で撮り直されている。最後に入ったオン thumb 変更 (`onThumbColorFrom` / コントラスト閾値 1.5 / `colorOnPrimary` 撤去) は実装・テストとも品質に問題がなく、**ミューテーション実測で「`colorOnPrimary` へ戻す退行」が新規テスト 2 件でちょうど捕まり、既存 85 件では 1 件も捕まらない**ことを確認した。

iOS 588 件 / `swift test` 93 件、Android 2656 件 (ui 1934 / bridge 322 / compose 240 / core 160) いずれも 0 failures で、summary の記載件数と一致する。作業ツリーはレビュー開始から終了まで不変 (凍結を確認)。

残る指摘は Minor 3 / Suggestion 3 で、**いずれもコメントと summary の文言のみ**。挙動を変えず、実物の再確認も再テストも要らない。Critical / Major は無いため APPROVED とするが、`BasicCellsTest.kt:1108` は「撤去済みの実装を現在仕様として書いたコメント」であり、review-001 Major-2 → review-002 Minor-2 と同型の 3 度目の再発なので、アーカイブ前に潰すことを推奨する。

**見た目に関わる新提案は出していない。** 確定値 (22/0/16dp・4pt/dp・彩度 0.09/0.04・明度比 1.0/0.92・コントラスト閾値 1.5) の是非は論じない。

## review-002 指摘の解消状況

| 前回指摘 | 状態 | 確認内容 |
|---|---|---|
| 🟠 Major-1 実装が確定サマリの範囲を超え summary が現物を表していない | **解消** | 下表のとおり 6 項目すべて現物と一致。証跡も撮り直し済み |
| 🟡 Minor-1 `SectionAccessoryRenderingTest` の区切りコメントに旧根拠 | **解消** | `SectionAccessoryRenderingTest.kt:396-399` が現在の仕様 (Cell 側に 4dp / 反対側 0 / 横 16dp) の記述へ |
| 🟡 Minor-2 `BasicCellsTest` の Track/Thumb 分離根拠の取り違え | **解消** | `BasicCellsTest.kt:1134-1141` が「明度の土台に別の attr を使う」構造の説明へ書き換え済み。ダーク反転の担保にも言及 |
| 🟡 Minor-3 `SwitchCellAccentDerivationTest` の KDoc がオフ状態限定 | **解消** | `SwitchCellAccentDerivationTest.kt:25-41` が「Switch 色が実効 accent から導出」へ拡張され、列挙 6 項目にオン thumb 2 件を含む |
| 🔵 Sug-1 `ClassicSectionDecorationTest` の `any` | 未対応 (下記 Suggestion-1 として再掲) |
| 🔵 Sug-2 iOS `minInset` の集約点外での再導出 | 未対応 (下記 Suggestion-2 として再掲) |
| 🔵 Sug-3 `textGap` の既定値が Section 側 | 未対応 (下記 Suggestion-3 として再掲) |
| 🔵 Sug-4 `SectionBoxDecorationTests` の片側不等式 | **対応 (文書化)** | `SectionBoxDecorationTests.swift:432-435` に「`rootTextGap` = 0 のため下端密着」「上向きには押し下げられない」という根拠コメントが付き、推奨修正の後半 (緩さの理由を読み手に明示する) を満たしている |
| 🔵 Sug-5 色相許容幅の根拠が blend | 未対応 (実害小のため再掲しない。`SwitchCellAccentDerivationTest.kt:150`) |
| 🔵 Sug-6 alpha の扱いがオフ色とオン thumb で非対称 | 未対応 (下記 Suggestion で再掲) |
| 🔵 Sug-7 `findMaterialSwitch` の重複 | 未対応 (実害小のため再掲しない) |
| 🔵 Sug-8 証跡がダークに見えない理由の注記 | 未対応 (下記 Minor-3 に含めて再掲) |

### Major-1 の 6 項目の突き合わせ

| review-002 が指摘したずれ | 現物 |
|---|---|
| 節見出しが「オフ状態色」 | `summary.md:14` は「Android SwitchCell の状態色」。`:24` にオン thumb の項目が独立して立ち、`:36` に採用値と閾値変更の経緯も入った |
| 「オン状態の見た目は不変」 | `:22` は `trackDecoration` の話に限定され、オン thumb の変化は `:24` で「ダークのオン thumb が紫紺→白」と明記 |
| `Int.scaleHsl` (存在しない関数名) | `:48` は `offTrackColorFrom` / `offThumbColorFrom` / `tintedFrom` / `onThumbColorFrom` / `trackDecorationTintList`。`grep` で全て実在を確認 |
| 回帰テスト 7 ケース | `:54` は 9。実ファイルの `@Test` も 9 件 |
| ui 958 件 | `:59` は ui 1934 (debug+release)。実測 967 × 2 = 1934 で一致 |
| ダーク証跡のオン thumb が紫紺 | `evidence/switch-off-colors-dark.png` を目視。オン Switch のつまみが**白**。オフは thumb が track より明るい (attr 反転への追従) |

## 確認した観点と結果

| 観点 | 結果 |
|---|---|
| ビルド・テスト (iOS) | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → **Executed 588 tests, 0 failures / TEST SUCCEEDED**。`swift test` (Core) → **93 tests, 0 failures**。いずれも `summary.md:58` と一致 |
| ビルド・テスト (Android) | `./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL**。XML 集計で ui 967×2 / bridge 161×2 / compose 120×2 / core 80×2 = **2656 tests, 0 failures**。`summary.md:59` の内訳と完全一致。review-002 が遭遇した結果集約側の障害 (`in-progress-results-generic.bin`) は今回再現せず |
| 新規テストの検出力 (lessons L-001) | ミューテーション 2 種で実測。詳細は下記 |
| 作業ツリーの凍結 | レビュー開始時と終了時で `git status` の 16 変更 + 1 新規が不変。ミューテーションは backup との `shasum` 一致 (`6c1fa129...`) で原状復帰を確認 |
| summary と現物の対応 | 実装内容・関数名・件数・証跡は一致。残るずれは実装ファイル数の数え違い 1 箇所 (Minor-2) と distill 送りの記述漏れ (Minor-3) のみ |
| `samples/` 復元 | `git status samples/` 差分ゼロ (検証用 accent 3 色の一時付与が完全復元されている) |
| 公開 API | 型・シグネチャの変更なし。導出係数・gap 定数はすべて `private const` / `internal`。利用者可視の描画変化 (オフ色・オン thumb・既定 margin・H/F の 4pt) は summary に記録済み |
| MAUI 非改変の妥当性 | `maui/KsSettingsView.Maui/SettingsView.cs:443` の `SectionMarginProperty` は `default(Thickness?)` で null をネイティブへ委譲しており、既定値の複製が無いことを確認 (`summary.md:12` の主張どおり) |
| Root H/F の 0 統一 | Android は `RootTextAccessoryViewHolder` が `applySectionTextVerticalPadding` を呼ばない別経路 (`SectionAccessoryViewHolders.kt:210,241`)。iOS は `textGap(forElementKind:)` が `rootTextGap = 0` を返す。両 platform 0 で一致 |
| `SectionBoxLayout` 初期 metrics の挙動不変 | `margin` は `SectionBoxLayout.swift` 内で 1 度も読まれない (読むのは `cornerRadius` / `borderWidth` / `borderColor` の 3 つのみ)。`summary.md:45` の「読み手ゼロで挙動不変」は正しい |
| 無効状態 (`isEnabled = false`) | 新設した `trackDecorationTintList` は checked/unchecked の 2 状態のみで disabled 分岐を持たないが、既存の `thumbTintList` / `trackTintList` も同じ 2 状態指定であり、本 change による退行ではない (指摘としては挙げない) |
| 足場アーティファクト | S 級・デルタスペック無し。書き換え対象なし |
| lint | `comment-policy-lint` 変更 16 ファイル + 新規 1 で禁止 0 件、`local-path-lint` / `identity-lint` とも 0 件。ただし規約自身が明記するとおり **lint 0 件は適合の証明にならない** — 実際に Minor-1 は素通りしている |
| concepts / decisions | `concepts/core/styling/list-appearance.md` と `style-resolution.md` の抵触箇所を実読して確認 (Minor-3 参照)。`comment-policy.md` の禁止類型「アーカイブされた過去仕様の説明」に照らして Minor-1 を検出 |

### ミューテーション実測 (lessons L-001)

review-002 はオフ色導出の検出力のみを実測していたため、今回は**オン thumb 導出**を対象にした。作業後は backup との `shasum` 一致 (`6c1fa1290a69d5dfc3a7493e0c69191510ac4014`) で原状復帰を確認済み。

| 変異 | 意味 | 結果 |
|---|---|---|
| `onThumbColorFrom(accent)` → `MaterialColors.getColor(switchView, colorOnPrimary, WHITE)` | 本 change が是正したテーマ漏れそのものへの退行 | `SwitchCellAccentDerivationTest` の `オン状態の Thumb はテーマが変わっても同じ色になる` と `明るい accent ではオン Thumb が暗色へ倒れて視認性を確保する` の **2 件だけが FAILED**。同時実行した `BasicCellsTest` 85 件は 1 件も落ちない (87 tests completed, 2 failed) |
| `ON_THUMB_MIN_CONTRAST` 1.5 → 1.0 | 明色 accent の暗色分岐が二度と発火しない退行 | `明るい accent ではオン Thumb が暗色へ倒れて視認性を確保する` **のみ** FAILED (9 tests completed, 1 failed) |

1 つ目は、review-002 が Major の根拠に挙げた「`colorOnPrimary` へ戻そうとしたときに根拠が残らない」懸念に対して、**根拠はテストの形で残っている**ことを示す。2 つ目は、閾値の分岐が両側から固定されていることを示す (併せて `オン状態の Track には〜` が使う orange #FF8000 は白比約 2.5 なので、閾値を上げる方向の退行も検出される)。

## 指摘事項

### [🟡 Minor] `BasicCellsTest` の KDoc が撤去済みの `colorOnPrimary` を現在のオン thumb 仕様として書いている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt:1106-1108`

**問題点**:
本 change で書き換えられた KDoc が、オン側だけ旧実装のまま残っている。

```
 * `SwitchCell` の Thumb には Track と独立した ColorStateList
 * （state_checked で `colorOnPrimary` / オフ時は accent から導出した減彩色）が設定される
 * （同色塗りではない）。
```

`colorOnPrimary` の直参照は本 change で**撤去された** (`SwitchCellViewHolder.kt:157` は `onThumbColorFrom(accent)`)。オフ側は正しく更新されているのにオン側だけ旧記述が残ったのは、オン thumb 変更がこの KDoc の書き換えより後に入ったためと読める。`grep -rn "colorOnPrimary" android/*/src` の結果 7 件のうち、**現在仕様として `colorOnPrimary` を語っているのはこの 1 行だけ**で、他 6 件はいずれも「参照してはいけないもの」として言及している (`SwitchCellViewHolder.kt:100-101` の KDoc、`SwitchCellAccentDerivationTest.kt:30,233,251-252` のテーマ漏れ検出)。

`concepts/cross/conventions/comment-policy.md` の禁止類型「アーカイブされた過去仕様の説明 — 現在の仕様を現在形で書く」に該当する。review-001 Major-2 → review-002 Minor-2 と**同型の 3 度目**であり、ファイルだけを読む人には「オン thumb はテーマ attr を見る」と読めてしまう。

**推奨修正**: オン側を現行仕様に書き直す (例: 「state_checked では accent に対するコントラスト色 (通常は白) / オフ時は accent から導出した減彩色」)。挙動は変わらないので再テスト不要。

### [🟡 Minor] `BasicCellsTest` の assert メッセージ「減彩・暗色」がライト限定の説明になっている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt:1155`

**問題点**:
直上の KDoc (`:1134-1141`) は review-002 Minor-2 を受けて「明度の土台に別の attr を使うのでダークで反転しても差が残る」と正しく書き直されたが、その 14 行下の assert メッセージだけが「Thumb 色（accent 由来の減彩・**暗色**）」のままで、KDoc と食い違う。thumb が track より暗いのはライトのときだけで、ダークでは `colorOutline` の側が明るくなるため thumb の方が明るい (`SwitchCellAccentDerivationTest` の `ダークテーマでも Thumb は Track より明るく〜` が固定している関係)。テストが落ちたときにこのメッセージを読んだ人が、逆向きの前提で原因を探すことになる。

**推奨修正**: 「Thumb 色（accent 由来の減彩色。明度の土台が Track と別 attr）」のようにテーマ非依存の表現へ。

### [🟡 Minor] summary の実装ファイル数と distill 送りの記述に取りこぼしがある

**該当箇所**: `summary.md:41` / `summary.md:71` / `summary.md:72`

**問題点**:
S 級では summary が唯一の長命記録で蒸留の入力になるため、3 点を挙げる。

1. **`:41`「実装 (iOS 2 / Android 3)」が自分の箇条書きと合わない** — 直後の `:43-45` に iOS 3 ファイル (`SectionBoxMetrics.swift` / `KsSettingsViewController.swift` / `SectionBoxLayout.swift`)、`:46-48` に Android 3 ファイルが並んでおり、`git status` の実測も iOS 3 / Android 3 = 6。「iOS 2」は数え違い

2. **`:71` の concepts 追随の記述が `list-appearance.md:65` を「参考値 12dp」としか捉えていない** — 同じ行には「Modern の既定 **margin** は各 platform が所有し、platform 間で**同じ生値に揃えない**」という規範文がある。本 change は Android の Modern 既定を iOS と同値 (22/16/0/16) にしたので、参考値だけでなく**この規範文自体が現物と抵触**している。数値だけ直して規範文を残すと、次に既定値を触る人が「揃えなくてよい」と読む

3. **`:72` の「利用者可視の変化」がオフ色のみを対象にしている** — オン thumb も利用者可視の変化 (ダークテーマ全般、および `colorOnPrimary` が白でないテーマ) であり、review-002 の推奨 2 がここに残っている。`:24` と `:36` に経緯は書かれているので情報自体は失われていないが、蒸留時に拾う口がオフ色だけになっている

**推奨修正**: 1 は「iOS 3 / Android 3」へ。2 は追随対象に「Modern 既定 margin を platform 間で揃えない、という規範文」を明記。3 は `:72` の対象にオン thumb を追加 (ADR 候補として起こすかはオーナー判断)。

### [🔵 Suggestion] `ClassicSectionDecorationTest` の上余白アサーションが「先頭行にだけ入る」を固定していない (review-002 Sug-1 の再掲)

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ClassicSectionDecorationTest.kt:180-184`

**問題点**: コメントは「上余白は Section の先頭行にだけ入り」と書いているが、アサーションは `offsets.any { it.top == expectedTopPx }` で**どれか 1 行に入っていれば pass** する。bottom / left / right は全行を回して固定しているのに top だけ粒度が落ちており、Section 境界の判定が壊れて毎行に offset が出る退行を検出できない。

**推奨修正**: `offsets.count { it.top == expectedTopPx }` が Section 数と一致することを固定する。

### [🔵 Suggestion] iOS の `minInset` が Root/Section の判定を `textGap` の値から再導出している (review-002 Sug-2 の再掲)

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2375`

**問題点**: `let minInset: CGFloat = textGap > 0 ? 2 : 0` は `textGap` が 0 であることをもって Root と判定する。`textGap(forElementKind:)` の KDoc (`:2329`) は「Root と Section で余白の扱いが分かれる唯一の判定点」と宣言しているのに、この 1 行が集約点の外で同じ分岐を値から再導出している。将来 Section 側の gap を 0 に調整すると、意図せず最小余白まで 0 になる。

**推奨修正**: `forElementKind` の解決側から gap と最小余白を組で運ぶか、Section 用の最小余白を独立した名前付き定数として渡す。

### [🔵 Suggestion] `textGap` の既定値が Section 側なので Root の呼び出し追加で指定漏れが黙って通る (review-002 Sug-3 の再掲)

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2260,2349`

**問題点**: `applyAccessoryToListCell` / `applyAccessoryLabel` の `textGap` は `= KsSettingsViewController.sectionTextGap` を既定に持つ。現行の本番呼び出し 3 箇所 (`:1292` / `:2149` / `:2222`) はすべて `Self.textGap(forElementKind:)` を明示しているので実害は無いが、Root accessory の経路を新設して指定を忘れると Root に 4pt が入り、今回わざわざ 0 に揃えた挙動が黙って崩れる。

**推奨修正**: 既定値を外して必須引数にする。

### [🔵 Suggestion] accent の alpha の扱いがオフ色とオン thumb で非対称 (review-002 Sug-6 の再掲)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:59,109-119`

**問題点**: オフ色は `ColorUtils.setAlphaComponent(..., Color.alpha(accent))` で accent の alpha を引き継ぐが、`onThumbColorFrom` は `Color.WHITE` / `ColorUtils.HSLToColor(...)` を返すため常に不透明。半透明 accent の Cell ではオフ色だけが透ける。現状 accent は不透明前提の運用なので実害は無いが、どちらが意図なのかコードから読み取れない。

**推奨修正**: `onThumbColorFrom` の KDoc に「戻り値は常に不透明 (つまみは下地を透かさない)」と明記して意図を残す (オン側も alpha を引き継ぐ選択でもよい)。

## アクションプラン

判定は APPROVED であり、**挙動・テスト・証跡に手を入れるべき指摘は無い**。以下はすべてコメントと summary の文言のみで、実物の再確認も再テストも不要。

1. **アーカイブ前に潰すことを推奨 (Minor)** — 挙動を変えない編集で、いずれも数分で終わる:
   - `BasicCellsTest.kt:1106-1108` のオン thumb を現行仕様へ (3 サイクル連続で出ている同型の最後の 1 箇所)
   - `BasicCellsTest.kt:1155` の「暗色」をテーマ非依存の表現へ
   - `summary.md:41` の「iOS 2」→「iOS 3」、`:71` に「Modern 既定 margin を platform 間で揃えないという規範文」の追随を追記、`:72` の利用者可視の変化にオン thumb を追加
2. **任意 (Suggestion)** — 本 change で対応しなくてもよい。効果順に `ClassicSectionDecorationTest` の `any` (テスト検出力)、iOS `minInset` (判定点の集約)、`textGap` の既定値、`onThumbColorFrom` の alpha 注記
3. **蒸留への申し送り** — `summary.md:70` の ADR 候補 (Classic / Modern 既定 sectionMargin の同値統一) に加え、`SwitchCell` の状態色を accent 由来の導出にした判断 (オフ色の attr 明度基準・オン thumb の accent 基準コントラスト色) も ADR 化の候補として検討する価値がある。テーマ attr 直参照へ戻したくなる圧力が将来かかる領域であり、根拠が summary のアーカイブと共に消えるため
