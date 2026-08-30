# レビュー結果: adjust-section-spacing (002 回目)

**日付**: 2026-08-25
**判定**: CHANGES_REQUESTED

## サマリー

review-001 の指摘 10 件 (Major 2 / Minor 4 / Suggestion 4) は**全件が実装に反映されている**。Major-1 (オフ色導出に回帰テストが無い) への対応は形式的な追加ではなく、ミューテーション実測で検出力を確認できた。Major-2 の背景だった「ダークで明度差が潰れる」構造も、係数乗算から attr 明度基準の導出へ組み直され、その構造自体がテストで固定されている。Root Header/Footer のプラットフォーム差は両 platform 0 への統一で解消。iOS 588 件 0 failures、Android ui 967 件 0 failures。

一方、**レビュー実施中に作業ツリーが動き、実装が確定サマリの範囲を超えた**。`SwitchCellViewHolder.kt` に「オン thumb を accent 基準のコントラスト色として導出する」変更 (`onThumbColorFrom` / `ON_THUMB_*`) が加わり、`colorOnPrimary` の直参照が撤去されている。実装とテストの品質自体は高い (両分岐を固定する 2 件のテスト付き) が、確定サマリはこれを一切記述していないどころか「オン状態の見た目は不変」と明記しており、証跡・テスト件数・関数名も現物とずれた。S 級ではサマリが唯一の長命記録で蒸留の入力になるため、これを Major とする。

これに加えて、review-001 の Major-2 (コメントが撤去済み仕様を現在仕様として書いている) の修正が 1 箇所取りこぼされ、書き換えた側の 1 箇所は分離の仕組みを取り違えている。

**見た目に関わる新提案は出していない。** 確定値 (22/0/16dp・4pt/dp・彩度 0.09/0.04・明度比 1.0/0.92・オン thumb のコントラスト閾値 1.5) の是非は論じない。

## レビュー中のツリー変動 (判定の前提)

レビュー開始時と終了時で、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt` と `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellAccentDerivationTest.kt` の内容が変わった。他の 18 ファイルは不変。

| 時点 | SwitchCellViewHolder.kt の diff 規模 | SwitchCellAccentDerivationTest のケース数 | summary.md |
|---|---|---|---|
| レビュー開始時 | 93 行 (78 挿入) — オフ色導出のみ | 7 | 18:21 時点のまま |
| レビュー終了時 | 136 行 (126 挿入) — オン thumb 導出が追加 | 9 | **18:21 時点のまま (未更新)** |

本レビューは**終了時の状態**を対象に判定している。ライブ調整セッションが継続中であれば、確定サマリを現物に合わせ直したうえで再度レビューに掛けるのが筋。

## review-001 指摘の解消状況

| 前回指摘 | 状態 | 確認内容 |
|---|---|---|
| 🟠 Major-1 オフ色導出に回帰テストが無い | **解消** | `SwitchCellAccentDerivationTest.kt` 追加。ミューテーション実測で検出力を確認 (下記) |
| 🟠 Major-2 既存テストが撤去済み仕様を記述 | **一部残** | `BasicCellsTest.kt` の 5 箇所は書き換え済み。別ファイルに同型の残り (Minor-1) と、書き換え後の説明に誤り (Minor-2) |
| 🟡 Minor-3 ダーク配色で係数が潰れる (未検証) | **解消** | 明度を attr (`colorSurfaceContainerHighest` / `colorOutline`) から取る構造へ再設計。ダークの明度反転をテストで固定。証跡 `evidence/switch-off-colors-dark.png` 追加 |
| 🟡 Minor-4 Root H/F の余白がプラットフォーム差 | **解消** | iOS に `rootTextGap = 0` と `textGap(forElementKind:)` を導入し Android (0dp) と同値化。`summary.md:11` にオーナー確定事項として記載 |
| 🟡 Minor-5 `createSectionTextView` の旧根拠コメント | **解消** | `SectionAccessoryViewHolders.kt:294` は現在形の説明に書き換え済み |
| 🟡 Minor-6 summary の iOS 証跡記述が Modern | **解消** | `summary.md:61` が Classic に訂正され、Classic 既定 margin 反転の証跡である旨も明記 |
| 🔵 Sug-1 `SectionBoxLayout` 初期 metrics が resolve 未経由 | **解消** | `SectionBoxLayout.swift:26` で `SectionBoxMetrics.resolve(theme: Theme(), style: .classic)` へ |
| 🔵 Sug-2 iOS `classicDefaultMargin` のリテラル重複 | **解消** | `static let classicDefaultMargin = modernDefaultMargin` の別名方式へ |
| 🔵 Sug-3 HSL 変換が alpha を落とす | **解消** | `SwitchCellViewHolder.kt:59` で `ColorUtils.setAlphaComponent(..., Color.alpha(accent))` |
| 🔵 Sug-4 iOS の 4pt がマジックナンバー | **解消** | `sectionTextGap` / `rootTextGap` として定数化 |

## 確認した観点と結果

| 観点 | 結果 |
|---|---|
| ビルド・テスト (iOS) | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → **Executed 588 tests, 0 failures / TEST SUCCEEDED**。iOS 側 4 ファイルはレビュー中に変動なし (shasum 一致) |
| ビルド・テスト (Android) | ui モジュール **967 tests / 0 failures** (62 クラス、`--rerun-tasks` で全件再実行し XML 集計)。bridge 161 / compose 120 / core 80 も failures 0。なお `./gradlew test` のタスク完了処理が `NoSuchFileException: in-progress-results-generic.bin` / `EOFException` で落ちる事象が再現したが、これは結果集約側の障害で、テスト XML はいずれも完走・全 pass。別セッションが同じ build ディレクトリで Gradle を走らせている競合が原因と見られる (デーモン 2 プロセスを確認) |
| 新規テストの検出力 (lessons L-001) | ミューテーション 2 種で実測。詳細は下記 |
| summary と現物の対応 | **不一致あり (Major-1)**。実装 5 / テスト 8 + 新規 1 というファイル構成は `git status` と一致するが、SwitchCell の変更内容・テスト件数・関数名・証跡がずれている |
| samples/ 復元 | `git status samples/` 差分ゼロ |
| 公開 API | 型・シグネチャの変更なし。導出係数・gap 定数はすべて `private const` / `internal`。ただし**利用者から見える描画結果**はオン thumb でも変わっている (Major-1) |
| 足場アーティファクト | S 級・デルタスペック無し。書き換え対象なし |
| lint | `comment-policy-lint` 全 680 ファイルで禁止 0 件、`local-path-lint` / `identity-lint` とも 0 件。ただし規約自身が明記するとおり **lint 0 件は適合の証明にならない** — 実際に Minor-1 / Minor-2 は素通りしている |
| 証跡 | `evidence/` に 4 枚実在。余白まわりは summary の記述と style・配色が一致。Switch のダーク証跡はオン状態が現行コードと食い違う (Major-1) |

### ミューテーション実測 (lessons L-001)

`SwitchCellViewHolder.kt` に一時的な変異を入れて実行した。作業後は backup との `shasum` 一致 (`6c1fa129...`) で原状復帰を確認済み。

| 変異 | 意味 | 結果 |
|---|---|---|
| `tintedFrom` の明度を `baseHsl[2] * ratio` → 固定値 `0.90 * ratio` | ライト実測ベースの固定係数へ退行 (= review-001 Minor-3 が懸念した実装) | `ダークテーマでも Thumb は Track より明るく明度差が残る` **のみ** FAILED |
| `tintedFrom` の彩度を 0 に固定 | accent 非依存の固定グレーへ退行 (= review-001 Major-1 が指摘した実装) | 新規 3 件 (`accent を変えると〜` / `Cell 個別の accentColor が〜` / `オフ状態の Track の色相は〜`) が FAILED。**`BasicCellsTest` の既存 Switch 色テストは 87 件中 1 件も落ちない** |

2 つ目は、review-001 の「既存 4 件は検出力ゼロ」という診断が正しかったことと、新規テストがその穴をちょうど埋めていることの両方を示している。

## 指摘事項

### [🟠 Major] 実装が確定サマリの範囲を超え、summary が現物を表していない (オン thumb 導出の追加)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:89-119,157-159,175-179` / `summary.md:14,22,46,52,57,63,70`

**問題点**:
現在のツリーには、オフ色の導出に加えて**オン状態の thumb 色の導出**が入っている。

```kotlin
private fun onThumbColorFrom(accent: Int): Int {
    val opaqueAccent = ColorUtils.setAlphaComponent(accent, 255)
    if (ColorUtils.calculateContrast(Color.WHITE, opaqueAccent) >= ON_THUMB_MIN_CONTRAST) {
        return Color.WHITE
    }
    ...  // 明色 accent は accent の色相を残した暗色へ倒す
}
```

これにより `colorOnPrimary` の直参照が撤去され (`bind` から `MaterialColors.getColor(..., colorOnPrimary, ...)` が消えている)、オン thumb は「白」または「accent 由来の暗色」になった。**実装とテストの品質は問題ない** — `オン状態の Thumb はテーマが変わっても同じ色になる` がテーマ漏れの退行を、`明るい accent ではオン Thumb が暗色へ倒れて視認性を確保する` が明色 accent の分岐を、それぞれ固定している。

問題は確定サマリが現物を表していないことで、以下すべてがずれている。

| summary の記述 | 現物 |
|---|---|
| `:14` 節見出し「Android SwitchCell の**オフ状態色**」 | オン thumb も変わっている |
| `:22`「オン時は透明 (M3 既定と同値) で**オン状態の見た目は不変**」 | ダークテーマおよび明色 accent でオン thumb の色が変わる。`trackDecoration` の話としては正しいが、節全体として「オン状態は不変」と読める |
| `:46` 触ったファイル欄「`offTrackColorFrom` / `offThumbColorFrom` / `Int.scaleHsl`」 | オン側 (`onThumbColorFrom` / `ON_THUMB_MIN_CONTRAST` / `ON_THUMB_DARK_*`) の記載が無い。さらに **`Int.scaleHsl` はリポジトリに存在しない** (`grep -rn "scaleHsl" android/ ios/` で 0 件)。実体は `private fun tintedFrom(base:accent:saturation:lightnessRatio:)` (`:49`) |
| `:52`「回帰テスト **7** ケース」 | 9 ケース |
| `:57`「ui **958**」 | 実測 967 (`concepts/cross/conventions/test-execution.md` は件数の併記までを検証と定めている) |
| `:63` `evidence/switch-off-colors-dark.png` | 画像中のオン Switch のつまみが**暗い紫紺**で描かれている。これはまさに `onThumbColorFrom` の KDoc が「テーマ由来の暗色 (紫青系) が漏れて track と調和しない」として排除した状態であり、現行コードでは白になる。この 1 枚は現在のオン状態を示していない |
| `:70`「オフ色の accent 由来化は公開 API 変更なし … 利用者可視の変化として本 summary に記録 (lessons L-001 対応)」 | オン thumb も利用者可視の変化 (ダークテーマの全利用者に影響) だが記録が無い |

ライブラリの利用者から見ると、これは「ダークテーマでオン中の Switch のつまみの色が変わる」という無視できない挙動変化であり、蒸留 (ADR 候補・concepts 追随) の入力から漏れると、次に誰かが `colorOnPrimary` へ戻そうとしたときに根拠が残らない。

**推奨修正**:
1. summary 第 2 節をオン/オフ両方を含む記述に改め、見出し・`:22`・`:46` の関数名 (`tintedFrom` 含む)・`:52` の件数・`:57` の件数を現物に合わせる
2. `:70` の「利用者可視の変化」にオン thumb を追加する (ADR 候補として起こすかはオーナー判断)
3. ダークの証跡を現行コードで撮り直す (オン thumb が白になっている状態)

### [🟡 Minor] テストファイルの区切りコメントに旧根拠 (AiForms 準拠で上下 padding 0) が残り、直下のテストと矛盾する

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryRenderingTest.kt:396-399`

**問題点**:
review-001 Minor-5 の「AiForms 準拠で上下 0」という根拠は、プロダクトコード側 (`SectionAccessoryViewHolders.kt`) では KDoc・行コメントとも書き換えられた。しかし同じ根拠がテスト側の区切りコメントに残っている。

```
// Section TextView の内部 padding ポリシー
//   移植元 AiForms の `headercell.axml` / `footercell.axml` / `TextHeaderView.cs`
//   と揃え、上下 padding は 0、横方向は 16dp 相当を維持する。
```

この 3 行の直後にあるのが、本 change で名前ごと書き換えた 2 つのテスト (`Header bind で TextView は Cell 側 (下) にだけ 4dp の余白を持つ` / `Footer bind で TextView は Cell 側 (上) にだけ 4dp の余白を持つ`) であり、**「上下 padding は 0」という宣言と直下の assert (4dp) が正面から食い違う**。ファイルだけを読む人 (人間・エージェント) には、どちらが現在の仕様か判断できない。

`concepts/cross/conventions/comment-policy.md` の禁止類型「アーカイブされた過去仕様の説明 — 現在の仕様を現在形で書く」に該当し、「今回まさに根拠なしと判断された主張」がファイル内に生き残っている点で review-001 Minor-5 と同型。

**推奨修正**: 区切りコメントを現在の仕様に書き直す (例: 「横方向は 16dp 相当。上下は Section H/F のとき Cell 群に面する側だけ 4dp を入れる」)。AiForms への言及を残すなら、現在も守っている互換契約 (横 16dp 相当・垂直配置の gravity) に限定する。

### [🟡 Minor] 書き換え後の `BasicCellsTest` の説明が Track / Thumb を分離している仕組みを取り違えている

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt:1136-1137`

**問題点**:
review-001 Major-2 を受けて旧トークン名は外れたが、置き換わった説明が実装と合っていない。

> オフ時に両者を同じ導出で塗ると同色化して輪郭が見えなくなるため、同じ accent から
> 異なる係数（Track = 淡く減彩 / Thumb = 濃く減彩したうえで暗く）で導出して分離する。

実装で Track と Thumb を分けている主因は係数ではなく、**土台にする attr が違うこと**である (`offTrackColorFrom` は `colorSurfaceContainerHighest`、`offThumbColorFrom` は `colorOutline` を明度の土台に取る。`SwitchCellViewHolder.kt:68,78`)。彩度 0.09 / 0.04 と明度比 1.0 / 0.92 の差だけでは、実際に生じている明度差を説明できない。さらにこの説明では「ダークで明度関係が反転しても分離が保たれる」という本 change の中核の構造がまったく読み取れず、係数調整で分離しているという誤った理解を誘導する。

comment-policy の最低条件「そのファイルだけを読んでいる人にとって意味が通ること」に対して、意味は通るが**内容が誤り**という状態。

**推奨修正**: 分離の根拠を attr の違いとして書き直す (例: 「明度の土台に別々のテーマ attr (`colorSurfaceContainerHighest` / `colorOutline`) を使うため、テーマがライト・ダークのどちらでも両者の明度差が残る。色相はどちらも accent 由来」)。

### [🟡 Minor] `SwitchCellAccentDerivationTest` のクラス KDoc が検証範囲をオフ状態に限定したまま

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellAccentDerivationTest.kt:25-38`

**問題点**:
クラス KDoc は「[SwitchCell] の**オフ状態の色**が実効 accent から導出されていることを検証する」と宣言し、検出対象を 4 項目 (attr 直値でない / accent 追従 / ボーダー = thumb 同色 / オン時は accent が track に出る) として列挙している。しかし現在このクラスは、オン thumb がテーマに依存しないこと・明色 accent で暗色へ倒れることという**オン状態の導出そのもの**を 2 件で検証している。列挙にその 2 件が無く、クラス名 (`AccentDerivation`) と KDoc の範囲宣言も食い違う。

**推奨修正**: KDoc の対象を「オフ状態の色とオン thumb の色が実効 accent から導出されていること」に広げ、列挙に 2 項目を足す。

### [🔵 Suggestion] `ClassicSectionDecorationTest` の新アサーションが「先頭行にだけ入る」を固定していない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ClassicSectionDecorationTest.kt:179-184`

**問題点**:
コメントは「上余白は Section の先頭行にだけ入り」と書いているが、アサーションは `offsets.any { it.top == expectedTopPx }` であり、**どれか 1 行に入っていれば pass する**。全行に上余白が入る退行 (Section 境界の判定が壊れて毎行に offset を出す類の不具合) を検出できない。bottom / left / right は全行を回して固定しているので、top だけ検出粒度が落ちている。

**推奨修正**: `offsets.count { it.top == expectedTopPx }` が Section 数と一致することを固定するか、行 index と Section 境界の対応から期待値の配列を組み立てて突き合わせる。

### [🔵 Suggestion] iOS の `minInset` が「Root か Section か」を `textGap` の値から再推定している

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2375`

**問題点**:
`let minInset: CGFloat = textGap > 0 ? 2 : 0` は、`textGap` が 0 であることをもって Root と判定している。summary (`:42`) と `textGap(forElementKind:)` の KDoc はいずれも「Root/Section の分岐は `textGap(forElementKind:)` に集約」と宣言しているのに、この 1 行だけがその集約点の外で同じ分岐を値から再導出している。将来 Section 側の gap を 0 に調整した場合、意図せず最小余白 (領域内に収めるためのインセット) まで一緒に 0 になる。

**推奨修正**: 判定点を 1 つに保つなら、`forElementKind` の解決側から gap と最小余白を組で運ぶ (例: `(gap: CGFloat, minInset: CGFloat)` を返す) か、Section 用の最小余白を `sectionTextGap` とは独立した名前付き定数として渡す。

### [🔵 Suggestion] `textGap` の既定値が Section 側なので、Root の呼び出しを足したときに指定漏れが黙って通る

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:2260,2349`

**問題点**:
`applyAccessoryToListCell` / `applyAccessoryLabel` の `textGap` は `= KsSettingsViewController.sectionTextGap` を既定に持つ。現行の本番呼び出し 3 箇所 (`:1292` / `:2149` / `:2222`) はすべて `Self.textGap(forElementKind:)` を明示しているので実害は無いが、既定が「Section 側」に倒れているため、Root accessory の経路を新設して指定を忘れると Root に 4pt が入る (今回わざわざ 0 に揃えた挙動が黙って崩れる)。

**推奨修正**: 既定値を外して必須引数にする。テストからの呼び出し (`SectionAccessoryRenderingTests.swift:170,182,196` は view 形式で `textGap` 非依存) も明示指定に直すだけで済む。

### [🔵 Suggestion] `SectionBoxDecorationTests` の Root Header 位置の固定が等値から片側不等式に緩んだ

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:435-436`

**問題点**:
`XCTAssertEqual(marginLabel.minY, zeroLabel.minY, accuracy: 0.5)` が `XCTAssertLessThanOrEqual(marginLabel.minY, zeroLabel.minY + 0.5)` に変わった。失敗メッセージ (「Root Header 自体が下へ押し下げられている」) と向きは合っているが、上方向へは**無制限に許容する**ため、Root Header の位置が margin に引きずられて動く退行のうち上向きのものを検出できない。テスト全体の主張 (余白が Root Header と先頭 Section の**間**に入る) は直前の `marginGap - zeroGap == 24` で担保されているので実害は小さいが、この行が担っていた固定は弱まっている。

**推奨修正**: `rootTextGap = 0` と領域の `.estimated(20)` 下限から導かれる差分を期待値として明示し、等値で固定する。それが脆いと判断するなら、この行は落として「Root Header 自体の位置は主張しない」ことをコメントで明示する方が、緩い不等式を残すより読み手に正直。

### [🔵 Suggestion] 色相追従テストの許容幅の根拠が実装と合っていない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellAccentDerivationTest.kt:148`

**問題点**:
「減彩しても色相そのものは保たれる（下地との blend でわずかにずれる分だけ許容する）」とあるが、現行の導出に blend は無く、色相は `accentHsl[0]` として accent から素通しでコピーされる (`SwitchCellViewHolder.kt:55`)。実際にずれる原因は、低彩度で HSL→RGB→HSL を往復する際の 8bit 量子化。書かれている根拠が誤っているため、30 度という許容幅の妥当性を後から判断できない。

**推奨修正**: 理由を量子化に書き直す (例: 「彩度が低いと RGB の差が数階調しかなく、往復で色相が振れるため幅を持たせる」)。

### [🔵 Suggestion] accent の alpha の扱いがオフ色とオン thumb で非対称

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:59,109-119`

**問題点**:
review-001 Sug-3 への対応で、オフ色は `ColorUtils.setAlphaComponent(..., Color.alpha(accent))` により accent の alpha を引き継ぐようになった。一方 `onThumbColorFrom` は `Color.WHITE` または `ColorUtils.HSLToColor(...)` を返すため常に不透明で、alpha は落ちる。半透明の accent を指定した Cell では、オフ色だけが透け、オン thumb は透けないという非対称になる。現状 accent は不透明前提の運用なので実害は無いが、どちらの扱いが意図なのかコードから読み取れない。

**推奨修正**: オン側も alpha を引き継ぐか、`onThumbColorFrom` の KDoc に「戻り値は常に不透明 (つまみは下地を透かさない)」と明記して意図を残す。

### [🔵 Suggestion] `findMaterialSwitch` がテスト間で重複している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellAccentDerivationTest.kt:76-85`

**問題点**:
`BasicCellsTest` にある同名・同実装のヘルパの写し。ViewHolder の階層構造が変わったとき 2 箇所を直すことになる。

**推奨修正**: 既存のテスト共有ヘルパへ寄せる (優先度は低い)。

### [🔵 Suggestion] 証跡がダークに見えない理由が summary に無い

**該当箇所**: `summary.md:63` / `evidence/switch-off-colors-dark.png`

**問題点**:
2 枚を突き合わせると、オフ Switch の明度関係が反転していることは読み取れる。ただし画面の大部分 (アプリバー・Section 箱・背景) はライトのままで、サンプルが Theme で背景色を固定しているため Switch と文字色だけが attr に追従した状態になっている。事情を知らずにこの 1 枚を見ると「ダーク配色の証跡になっていない」と誤読しかねない。

なお同じ画像で Cell タイトルが背景に沈んで判読困難になっているが、これは本 change が触れていない領域 (文字色の解決) の既存挙動であり、本 change による退行ではない。

**推奨修正**: 証跡欄に一言添える (例: 「サンプルの Theme が背景色を固定しているため、端末ダーク時に反転するのは Switch と文字色のみ」)。Major-1 の撮り直しと同じ回で片付く。

## アクションプラン

1. **Major-1**: まずライブ調整が収束したことを確定させ、そのうえで summary を現物に合わせる — 第 2 節の対象範囲 (オン thumb を含める)、`:22` の「オン状態は不変」、`:46` の関数名 (`Int.scaleHsl` → `tintedFrom` + オン側 3 定数/1 関数)、`:52` の 7 → 9、`:57` の ui 958 → 967、`:70` の利用者可視の変化にオン thumb を追加。ダークの証跡を現行コードで撮り直す (Suggestion の注記も同時に)
2. **Minor-1 / Minor-2 / Minor-3** (review-001 Major-2 の残り): `SectionAccessoryRenderingTest.kt:396-399` の区切りコメント、`BasicCellsTest.kt:1136-1137` の分離根拠、`SwitchCellAccentDerivationTest.kt:25-38` のクラス KDoc をそれぞれ現在の仕様へ
3. **Suggestion 群**: 余力があれば同じ回で。テスト検出力に関わるもの (`ClassicSectionDecorationTest` の `any`) と、集約点を崩しているもの (`minInset`) は効果が大きい

なお Minor / Suggestion はいずれも挙動を変えない編集であり、実物の再確認は不要。実物確認が要るのは Major-1 の証跡撮り直しのみ。
