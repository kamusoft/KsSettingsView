# レビュー結果: add-cell-types-custom (003 回目)

**日付**: 2026-08-04
**判定**: CHANGES_REQUESTED

## サマリー

review-002 の 3 件 (Major 1 / Minor 2) のうち、**コードとしては 3 件とも正しく実装されている**。iOS の `.opacity(isEnabled ? 1 : 0.38)` は content にのみ掛かり chevron・行背景に及んでおらず、条件分岐ではなく三項演算子で値だけを変える形なので content の view identity が保たれる (`if` 分岐にすると builder 出力内の `@State` がリセットされ、動的高さデモが壊れる)。淡色化後のテキスト濃度は証跡の実測で iOS 190 / Android 191 と一致しており、修正 A は狙いどおり機能している。修正 C の該当行も削除され、新規 20 ファイル・変更ファイルの追加行のいずれにもコメント規約違反は残っていない。

残る問題は 1 件で、**review-002 の Major が指摘した「公開ドキュメントの食い違い」が iOS 側だけ未解消**である。`CustomCell.swift:100` は依然として「無効時の見た目の描き分け（淡色表示等）は builder 側＝利用者の責務。」と宣言しており、ライブラリが content を淡色化するようになった実装と正反対の契約を公開 API の doc comment として掲げている。Android 側 (`CustomCell.kt:44-45` / `134`) は更新済みなので、**修正前と同じ「platform ごとに別の契約を宣言している」状態が、コードから doc へ場所を変えて残っている**。1 行の書き換えで済むが、利用者がこの記述に従って自前の淡色化を content 側へ入れると二重に薄くなるという実害があるため Major とする。

副次的に、修正 B の証跡の記述が実物より強い主張になっている (Minor 1 件)。

### 実行結果 (件数併記)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test` | **BUILD SUCCESSFUL** / `build/test-results/**/TEST-*.xml` 集計 **1848 tests / 0 failures / 0 errors / 0 skipped** |
| iOS | `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>…'` | **TEST SUCCEEDED** / バンドル別 **KsSettingsViewCoreTests 83 + KsSettingsViewSwiftUITests 68 + KsSettingsViewUITests 395 = 546 tests / 0 failures** |

iOS の件数について注意点を 1 つ残す。`xcodebuild` の出力を `tail` で切ると最後のバンドル (`KsSettingsViewUITests` の 395) しか見えない。コンテキストパッケージに記載された「395 tests」もこの切り取りによるもので、実際は 3 バンドル計 546 であり review-002 の 546 から減っていない (退行なし)。ログ全体を取って確認した。

足場アーティファクトの状態: `specs/` `proposal.md` `design.md` `ui/mock/` は未変更 (`git status --short` で確認)。`tasks.md` は 4.1 / 4.2 への検証メモ追記のみで、4.3 (ksn-verify 事前チェック) は未チェックのまま正しく残されている。

---

## 修正 3 件の解消状況

| # | review-002 の指摘 | コード | 付随ドキュメント | 判定 |
|---|---|---|---|---|
| A | 無効時の淡色化が逆向きの非対称 [Major] | `CustomCellHostedContent.swift:35` に `.opacity(isEnabled ? 1 : 0.38)`。適用範囲・値とも Android と一致 | `deviation.md:8-14` / `brief.md` / Android KDoc は更新済み。**iOS `CustomCell.swift:100` だけ旧契約のまま** | **部分解消** → Major |
| B | Sample の Slider 色の platform 差 [Minor] | `SampleSliderCell.kt:101-104` に `thumbColor` / `activeTrackColor` = `SampleTheme.mauiAccent`。active track は実測で一致 | 証跡が「thumb も両 platform で mauiAccent」と記述しているが iOS の thumb は標準の白 | **解消 (記述に過剰な主張)** → Minor |
| C | `KsSettingsViewController.swift:1862` のコメント規約違反 | 該当行を削除し、自己完結した 3 行の説明だけが残っている。定型句型の書き換えとして正しい | — | **解消** |

---

## 指摘事項

### [🟠 Major] iOS の公開 doc comment が実装と正反対の契約を宣言したまま残っている

**該当箇所**: `ios/Sources/KsSettingsViewUI/CustomCell.swift:100`

```swift
/// 有効／無効フラグ（既定 `true`）。
///
/// `false` のとき行タップは発火せず、content 内部の操作も抑止される。
/// 無効時の見た目の描き分け（淡色表示等）は builder 側＝利用者の責務。   // ← 実装と逆
```

**問題点**:

修正 A によって `CustomCellHostedContent` は `isEnabled == false` のとき content 全体へ `opacity 0.38` を掛けるようになった。つまり**淡色表示はライブラリの既定の振る舞いになった**。にもかかわらず、利用者が最初に読む `public let isEnabled` の doc comment は「淡色表示等は利用者の責務」と宣言し続けている。

同じ位置の Android 側は正しく更新されている:

- `android/.../CustomCell.kt:44-45`: 「あわせて content を淡色化するが、これは既定の振る舞いであり、無効時の描き分けを content 側で追加するのは利用者の自由」
- `android/.../CustomCell.kt:134`: 「`false` で行タップと content 内部の操作を抑止し、content を淡色化する」

review-002 の Major は 3 つの食い違いを根拠に挙げていた — (1) 実装の非対称、(2) `deviation.md` の記述、(3) 公開ドキュメントの platform 間食い違い (`CustomCell.kt:43-45` ↔ `CustomCell.swift:99-100`)。(1) と (2) は解消したが **(3) はそのまま残っている**。「platform ごとに別の契約を宣言している」状態が、コードから doc へ場所を移して存続しているだけである。

実害は机上の話ではない。この記述を読んだ iOS 利用者は「淡色化は自分で入れる必要がある」と解釈して content 側に `.opacity(0.5)` 等を書く。ライブラリ側の 0.38 と重なって二重に薄くなる — `deviation.md:14` が「合意済みの副作用」として記録している SwiftUI 標準コントロールの二重淡色化 (実効 alpha ≒ 0.19) が、利用者コードでも同じ形で再生産される。

なお `specs/cell-types-custom/spec.md:69` も同じ「利用者責務」の文言を持つが、これは足場であり `deviation.md:8` が正しく乖離として記録している。書き換えるべきは spec ではなく doc comment のほうである。

**推奨修正**:

`CustomCell.swift:100` を Android の `CustomCell.kt:44-45` と同義の表現へ書き直す。例:

```swift
/// `false` のとき行タップは発火せず、content 内部の操作も抑止される。
/// あわせて content 全体を淡色化するが、これは既定の振る舞いであり、
/// 無効時の描き分けを builder 側で追加するのは利用者の自由。
```

あわせて次の 2 箇所も同じ表現へ揃えると、公開 API の記述が platform 間で完全に一致する (いずれも「淡色化する」の一句が欠けているだけで、逆の宣言ではないため Major の本体には含めない):

- `ios/Sources/KsSettingsViewUI/CustomCell.swift:116` (`- isEnabled: 有効フラグ（既定 \`true\`）` のパラメータ説明。もう 1 つの public init `:168` も同様)
- `android/ks-settingsview-compose/.../CustomCellDsl.kt:66` / `:97` (Compose DSL の `@param isEnabled`。Compose 利用者が実際に読む入口はこちら)

---

### [🟡 Minor] 修正 B の証跡が「thumb も両プラットフォームで mauiAccent」と記述しているが、掲載画像自身がそれを否定している

**該当箇所**:

- `kasane/changes/add-cell-types-custom/ui/verification/index.md` — 「3 行とも active track / thumb が同じアンバー」
- `kasane/changes/add-cell-types-custom/ui/brief.md`「合意済み妥協」節 — 「active track / thumb は両プラットフォームとも `SampleTheme.mauiAccent` を明示指定して揃えた」
- `ui/verification/compare-01-disabled-dimming-and-slider-accent-ios-vs-android.png` の焼き込みキャプション — 「Slider の active track / thumb は両方とも SampleTheme.mauiAccent (#FFBF00)」

**問題点**:

`compare-01` と `compare-02` の両方で、**iOS の thumb は SwiftUI 標準の白い capsule、Android の thumb はアンバーの縦バー**である。拡大図 (`compare-02`) では特にはっきり写っている。SwiftUI の `.tint(_:)` が `Slider` に効くのは active (minimum) track であって thumb ではなく、thumb は system の白いノブのまま残る。つまり「thumb も揃えた」という記述は 3 箇所とも実物と食い違っている。

`activeTrackColor` が両 platform で `mauiAccent` に揃ったこと自体は実測どおりで、`sample-parity.md` が求める「各 Cell に渡すパラメータ (…色) を一致させる」も満たしている。**コード側の修正 B は妥当**である。問題なのは、その結果を検証する証跡が実際より強い主張をしている点で、後から証跡だけを読む人 (蒸留・drift・後続 change) が「thumb は揃っている」と誤って引き継ぐ。

厳密には、渡しているパラメータの数にも非対称が残っている — Android は `thumbColor` と `activeTrackColor` の 2 つを明示指定し、iOS は `.tint` 1 つ (= active track のみ着色) で thumb を指定する手段を持たない。これは inactive track の色差と同じ「片側でしか指定できない / 指定していないパラメータの platform 差」であり、現在オーナー確認中と聞いている inactive track の件と**同じ束で扱うのが素直**だと考える。単独では blocking にしない。

**推奨修正**: 3 箇所の記述を実物に合わせる (例: 「active track は両プラットフォームとも `SampleTheme.mauiAccent`。thumb は Android のみアンバー、iOS は SwiftUI 標準の白 — `.tint` は active track にしか効かないため」)。`compare-01` はキャプションが画像に焼き込まれているため、キャプションだけの再生成が要る。inactive track の扱いをオーナーが決める際に、thumb も同じ節へまとめて記録するとよい。

---

### [🔵 Suggestion] 淡色化に自動テストの回帰ガードがない

**該当箇所**:

- `ios/Sources/KsSettingsViewUI/CustomCellHostedContent.swift:35` / `:57`
- `android/.../CustomCellViewHolder.kt:152` / `:235`

淡色化は spec の Scenario ではなくオーナー指示由来の振る舞いだが、`deviation.md:8-14` に記録された時点で**合意済みの契約**になった。にもかかわらず、iOS / Android のどちらにも `opacity` / `alpha` を検証するテストが 1 件もない (`CustomCellTests.swift` / `CustomCellRenderingTest.kt` / `CustomCellTest.kt` を grep して 0 件)。誰かが `.opacity(...)` や `Modifier.alpha(...)` の行を消しても、テストは 546 / 1848 とも緑のまま通る。

現実的にはこれは仕方のない面がある — SwiftUI の `opacity` は inspection API に出ず、Compose の `Modifier.alpha` は semantics に現れないため、値を直接 assert する手段がない。押さえるならスクリーンショット比較テストが必要だが、本リポジトリにその基盤はない。

したがって修正は求めない。ただし「この契約の唯一の証拠は `ui/verification/` の画像であり、アーカイブ後は回帰ガードが残らない」という状態には名前を付けておく価値がある。蒸留で `cell-visual-states.md` へ CustomCell の淡色化を書くなら、検証手段がスクリーンショットに限られる点も併記しておきたい。

---

### [🔵 Suggestion] review-002 の Suggestion 4 件は今回未着手のまま

いずれも今回の修正 3 件のスコープ外であり、改めて blocking にはしない。アーカイブ前に判断が要るものとして再掲する。

- `KsChevronAppearance.trailingMargin` (`:47`) と `KsListCellBase.swift:111` の行マージンが別々の literal `16` のまま (定数共有になっていない)
- `ui/brief.md`「トークン候補」節に生カラー値 (`#FAF3D9` `#FAF7EE` `#777777`) が残っている (`ksn-core/references/ui-artifacts.md` は brief に生カラー値を書かないとしている)
- `ui/verification/` が 52 ファイル規模まで増えた。修正前フレーム (`08-expand-before-*` 5 枚) と `.mp4` の要否判断が未了
- `tasks.md` 4.3 (ksn-verify の事前セルフチェック) が未チェック

---

## 確認したが問題を検出しなかった観点

- **修正 A の適用範囲**: `.opacity` は `HStack` の第 1 要素 (`content`) にのみ付いており、chevron (`:37-39`) と行背景 (`UIHostingConfiguration` の外側 = `backgroundConfiguration`) には掛かっていない。Android も `weight(1f)` の `Box` にのみ `Modifier.alpha` を掛けており、`Image` の Disclosure Indicator と行背景は対象外。両 platform で適用範囲が一致している
- **修正 A の書き方 (view identity)**: iOS は `if` 分岐ではなく `.opacity(isEnabled ? 1 : 0.38)` の値変更にしているため、`isEnabled` の切り替えで content の subtree が作り直されない。分岐形にすると builder 出力内の `@State` (動的高さデモの展開状態など) がリセットされるので、この形が正しい。Android は逆に `.then(if (isEnabled) Modifier else Modifier.alpha(...))` の条件形だが、Compose では `alpha` が `graphicsLayer` を確保するため有効時にノードを持たないほうが素直であり、`Box` の content ラムダは modifier 連鎖の構造変化に影響されないので content の composition state は保たれる。どちらもその platform で正しい選択
- **有効時の余計なレイヤ**: iOS の `.opacity(1)` は compositing を発生させない no-op であり、1 行あたりのコストは無視できる。実際に `ios-sim-iphone17-ios265-12-disabled-dimmed-content.png` で有効行 (明るさ 70 / 音量 40) の色・濃度が `01` / `10` と同一であることを確認した
- **`.disabled` との併用**: `.disabled(!isEnabled)` は `CustomCellRowPlacement` の外側に掛かり、opacity は内側の content に掛かる。両者は独立で、順序の入れ替えによる挙動差はない。二重淡色化 (実効 alpha ≒ 0.19) は `deviation.md:14` / `brief.md` / `verification/index.md` に実測値つきで記録済みであり、合意済みの差分として扱った
- **修正 A のレイアウトへの影響**: `opacity` はサイズに影響しないため `CustomCellRowPlacement` の測定・配置は変わらない。`hasUnevenRows` 系の高さテスト (iOS 395 件中の該当分 / Android 側) が全て緑のままであることも確認した
- **修正 C の周辺**: `KsSettingsViewController.swift:1856-1858` の `openspec/changes/...` 参照は既存行 (`EntryCellView` の説明) であり、本 change の追加行ではない。既存 74 ファイル側の別 change 対象という review-002 の切り分けは正しい
- **コメント規約の全数検査**: 新規 20 ファイル (実装 / テスト / Sample) と、変更ファイルの追加行 (`git diff develop -U0` の `+` 行) を `kasane/` `openspec/` `changes/` `Decision [0-9]` `Phase [0-9]` `design.md` `spec.md` `proposal.md` `tasks.md` `review-` `論点` `Major` `Minor` `Critical` `MUST` `SHALL` `SHOULD` `MAY` で grep して **0 件**。修正 C の取りこぼしは無い
- **前回「正しい」と確認した実装の健全性**: `CustomCellRowPlacement` (`finite()` による intrinsic 問い合わせの自然高フォールバック、`placeSubviews` の「余白があれば等分・無ければ 0」)、`contentType: ObjectIdentifier` (`==` / `hash(into:)` 両方に参加、`withDSLID` / `withStyle` で引き継ぎ)、`clearAndSetSemantics { disabled() }` + `composeView.isEnabled` + `FOCUS_BLOCK_DESCENDANTS` の 3 層、`Modifier.alpha`、`CenterOrTopVertically` — いずれも今回の修正で変更されておらず、そのまま残っている
- **sample-parity (修正 B)**: `sample-parity.md` が求める「各 Cell に渡すパラメータ (…色) を一致させる」に対し、両 Sample とも accent として `SampleTheme.mauiAccent` を渡す形に揃った。inactive track を明示指定しない判断は同規約の「Sample が明示的に渡していないパラメータの、本体既定値の platform 差」の許容範囲に収まり、`brief.md`「合意済み妥協」に追跡も残っている。Android 側コメント (`SampleSliderCell.kt:95-97`) も「色だけ SampleTheme のアクセント色に揃える」という現在の実装と整合する内容へ書き換わっており、review-002 が指摘したコメントと実物の食い違いは解消している
- **Kotlin 言語層** (kotlin-impl-skill 観点): 修正 B で追加された `SliderDefaults.colors(...)` は Sample 内の局所変更で、`val` / null 安全 / sealed / Coroutines のいずれにも触れていない。`CustomCellViewHolder` 側の修正はなく、前回確認した手動 `equals` / `hashCode`・`composeContent` による型消去・`!!` 不使用の状態が維持されている。新規の言語層の問題は検出していない
- **足場の凍結**: `specs/cell-types-custom/spec.md` `proposal.md` `design.md` `ui/mock/` はいずれも未変更。`tasks.md` と `ui/brief.md` の追記は検証メモ・照合結果であり、`ksn-core/references/ui-artifacts.md` が brief の役割として認めている範囲

## アクションプラン

1. **[Major]** `ios/Sources/KsSettingsViewUI/CustomCell.swift:100` を Android の `CustomCell.kt:44-45` と同義の表現へ書き直す。ついでに `CustomCell.swift:116` / `:168` と `CustomCellDsl.kt:66` / `:97` の `@param isEnabled` にも「content を淡色化する」の一句を足し、公開 API の記述を platform 間で完全に揃える
2. **[Minor]** `ui/verification/index.md`・`ui/brief.md`・`compare-01` のキャプションから「thumb も両プラットフォームで mauiAccent」の主張を外し、実物どおり「active track のみ一致。thumb は iOS が標準の白」に直す。inactive track のオーナー確認と同じ束で扱う
3. **[Suggestion]** 淡色化の回帰ガードが証跡画像しかない点を、蒸留時に `cell-visual-states.md` へ書く内容に含める
4. **[Suggestion]** review-002 から持ち越しの 4 件 (chevron の余白定数共有 / brief の生カラー値 / verification の重量 / tasks 4.3 の ksn-verify) をアーカイブ前に処理する
