# レビュー結果: add-cell-types-custom (001 回目)

**日付**: 2026-08-03
**判定**: CHANGES_REQUESTED

## サマリー

実装の質は総じて高い。型消去の置き方 (iOS 非ジェネリック + ジェネリック init / Android ジェネリック class + `composeContent`)、関数値を除いた手動 equality、chevron 定数の共有 (`KsChevronAppearance` / `CELL_DISCLOSURE_*_DP`) はいずれも ADR-0014 / ADR-0015 と design の意図どおりで、コメントも単独で読める。テストは probe content (accessibilityIdentifier / testTag) と実 MotionEvent・実測 frame を使っており、代理値ではなく実経路を見ている (mutation probe まで置いてある)。ビルド・テストは両プラットフォームでグリーン。

一方で、**押下フィードバックの扱いが Android の既存共通契約から意図的に外れており、その結果 iOS と Android で観察可能な挙動差が生じているのに、spec / design / deviation.md / 視覚検証のいずれにも記録がない**。これはオーナー判断が要る契約の穴であり、レビュアー側では可否を決められないため CHANGES_REQUESTED とする。コード修正が必須という意味ではなく、「決めて記録する」ことが必要という意味である。

### 実行結果 (件数併記)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test --rerun-tasks` | **1838 tests / 0 failures / 0 errors / 0 skipped** (`build/test-results/**/TEST-*.xml` 集計) |
| iOS | `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<iPhone 17 / iOS 26.5>'` | 2 回実行。1 回目 **540 tests / 1 failure**、2 回目 **540 tests / 0 failures**。詳細は Minor 2 |

`tasks.md` のチェック済み項目に虚偽はない (1.1〜3.3 は実物を確認、4.2 / 4.3 は未チェックのまま残されている)。足場アーティファクト (proposal / design / specs / ui) の書き換えも `git status` 上で発生していない。

## 指摘事項

### [🟠 Major] 押下フィードバックが Android 共通契約から外れ、iOS との観察可能な差になっている (未記録)

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:73-77`
- 比較対象: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/LabelCellViewHolder.kt:37`
- 契約: `kasane/concepts/core/styling/cell-visual-states.md:21`
- iOS 側: `ios/Sources/KsSettingsViewUI/CustomCellView.swift:37` → `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:79-88`

**問題点**:

`concepts/core/styling/cell-visual-states.md:21` は Android の共通行について「handler を持たない LabelCell なども **enabled なら ripple 表示のために clickable flag を持つ**」と明記し、`LabelCellViewHolder.kt:37` は実際に `views.root.isClickable = cell.isEnabled` としている。

これに対し `CustomCellViewHolder.kt` は `onTap == null` (= **既定値**) のとき `isEnabled == true` でも `isClickable = false` にする:

```kotlin
} else {
    // 行タップを持たない場合は clickable も落とす。clickable のままだと押下時に
    // ripple だけが出て、タップできる行に見えてしまう。
    composeView.setOnClickListener(null)
    composeView.isClickable = false
}
```

結果として観察される挙動は次のとおり。

| 構成 (`isEnabled=true`, `onTap=null`) | Android | iOS |
|---|---|---|
| CustomCell 行を押下 | 何も起きない (ripple なし) | `Theme.selectedColor` にハイライトする |
| 同一画面の LabelCell 行を押下 | ripple する | ハイライトする |

iOS 側は `CustomCellView.swift:37` が `installSelectedColorHandler` を張り、そのハンドラ (`KsCellViewSupport.swift:84`) が `s.isEnabled && (isHighlighted || isSelected)` だけで判定するため、`onTap` の有無に関係なくハイライトが出る。これは `KsListCellBase.swift:128` と同じ経路なので **iOS 側は既存 Cell と整合**している。つまりズレているのは Android の CustomCell だけであり、そのズレが結果的に iOS↔Android の差になっている。

判断そのもの (full-bleed のカスタム行で ripple だけ出るのは誤誘導) には十分な理屈がある。問題は**その判断がソースコメント 1 行にしか存在しない**ことである:

- デルタスペックの「行タップ」Requirement は `onTap` 未指定時について「行レベルのタップ処理は発生せず、コントロールの操作がそのまま機能する」としか書いておらず、押下フィードバックには触れていない
- `design.md` Decision 4 も clickable フラグの扱いを規定していない
- `deviation.md` は存在しない
- `ui/verification/index.md` の押下フィードバック証跡 (`android-pixel6a-08-press-feedback.png`) は **onTap 付きの行**だけを撮っており、`onTap` 未指定 (既定) の行は撮られていない。iOS の押下フィードバック証跡は 1 枚もない

`cell-visual-states.md:21` は「この違いを共通化のために隠さない」と、プラットフォーム差を**明示的に名前を付けて残す**方針を取っている。本変更が新たに作った差だけが無名のまま残るのは、その方針と噛み合わない。

**推奨修正** (いずれか。レビュアーは可否を決めない):

1. Android を既存共通行に揃える (`isClickable = cell.isEnabled`) — 一貫性優先。ただし「押せそうに見える」問題は残る
2. 現行実装 (`onTap` の有無で clickable を決める) を正とし、**iOS 側も `tapHandler == nil` のときハイライトを抑止する**よう `installSelectedColorHandler` の判定に条件を足す — プラットフォーム間の一致優先
3. 現行の差をそのまま是とし、`deviation.md` に「CustomCell の押下フィードバックは `onTap` の有無で決まり、Android の共通行ルール (enabled なら常に clickable) の適用除外とする。iOS は既存 Cell と同じく isEnabled のみで判定するため差が残る」と記録する。蒸留時に `cell-visual-states.md` へ差を追記する

どの案でも、`onTap` 未指定 (既定) の CustomCell 行の押下時スクリーンショットを **iOS / Android 両方**で `ui/verification/` に追加してほしい。

---

### [🟡 Minor] design.md Decision 4 との具体化差と Android の高さ解決経路が deviation.md に記録されていない

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/CustomCellView.swift:30` (`UICollectionViewListCell` を継承) ↔ `design.md` Decision 4 (`internal final class CustomCellView: UICollectionViewCell, KsCellRenderer` と記述)
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:114-122` (`Modifier.height` / `Modifier.heightIn` による Compose ツリー内での高さ解決) ↔ `design.md` Decision 5 (行レベル style の適用先としか書いていない)

**問題点**:

いずれも**実装として妥当**である。前者は `KsCellViewSupport.installSelectedColorHandler` / `applyEffectiveHeight` / `adjustedLayoutAttributes` の引数型がすべて `UICollectionViewListCell` であり (`KsCellViewSupport.swift:79` / `:101` / `:153`)、標準 Cell (`KsListCellBase.swift:30`) と同じ機構を共有するには必然の選択。`UICollectionViewListCell` は `UICollectionViewCell` のサブクラスなので design の記述と矛盾もしない。後者は「`ComposeView` は `onMeasure` を composition へ委譲するため View の `minimumHeight` が効かない」という実物の制約への対処で、ソースコメントに理由が書かれている。

ただし ksn-core の足場凍結規約は「乖離は deviation.md に記録するだけ」「記録の**ない**乖離だけが問題」としており、後続の蒸留 (ADR / concepts への反映) はこの記録を入口にする。前者は罫線 (`UIListSeparatorConfiguration`) と backgroundConfiguration の共通機構に CustomCell が乗ることを意味し、後者は「行高さの解決点が platform で異なる (iOS = contentView の NSLayoutConstraint、Android = Compose ツリー内 Modifier)」という長命の知識になり得る。どちらもコードコメントだけに置くと蒸留から漏れる。

なお `applyEffectiveHeight(composeView, effective)` (`CustomCellViewHolder.kt:79`) と Compose 側の `Modifier.height/heightIn` は同じ値を二重に適用している。動作としては整合するが (テストで実測確認済み)、意図が「View 側 = RecyclerView への申告 / Compose 側 = 実際の測定」であることが読み取りにくい。上記記録の際に併せて一言残すとよい。

**推奨修正**: `deviation.md` を作成し、上記 2 点を「合意済みの具体化」として記録する。コード修正は不要。

---

### [🟡 Minor] iOS 全件実行で `InputCellsTests` が 1 回だけ失敗した (再現せず)

**該当箇所**: `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` の `test_EntryCellView_tapHandler呼び出しでtextFieldがbecomeFirstResponder`

**問題点**:

初回の全件実行 (540 tests) でこの 1 件だけが失敗した。その後、

- 同テストクラス単体 (`-only-testing:KsSettingsViewUITests/InputCellsTests`, 57 tests) → 0 failures
- `CustomCellTests` + `InputCellsTests` の 2 クラス同時 (88 tests) → 0 failures
- 全件再実行 (540 tests) → 0 failures

と、いずれも再現しなかった。初回実行はテストフェーズに 273 秒かかっており (再実行は 4 秒)、Simulator の初回起動待ちが `becomeFirstResponder` のタイミングに影響した可能性が高い。本変更が触っていないファイルのテストであり、因果は特定できていない。

疑ったのは `CustomCellTests` の `host(_:)` ヘルパ (`CustomCellTests.swift:52` 付近) が `UIWindow` を毎回 `makeKeyAndVisible()` して後片付けしない点で、key window の汚染が後続の first responder 取得を妨げる経路は理論上あり得る。ただし 2 クラス同時実行では再現しなかったため、証拠としては弱い。

**推奨修正**: 修正は求めない。フレークの観測事実として記録し、以後の全件実行で再発するようなら `CustomCellTests` に `tearDown` で window を破棄する (`window.isHidden = true` / `resignKey()`) 手当を検討する。

---

### [🟡 Minor] 「高さの自動追従」は再バインド駆動の経路しか検証されていない

**該当箇所**:
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellRenderingTest.kt:405-421` (`content のサイズ変化に行高さが追従する`)
- `ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:610-628` (`test_hasUnevenRowsがtrueならcellHeight未指定でもcontentの自然高に追従する`)
- Sample: `samples/ios/KsSettingsViewSample/CustomCellDemoView.swift:33-39` (`SampleExpanderState`) / `samples/android/.../CustomCellDemoScreen.kt:57`

**問題点**:

デルタスペックの「高さの自動追従」Scenario は「content 内の操作で展開状態に切り替える」→「行高さが追従する」である。実装・テスト・Sample はいずれも**展開状態を content 値に持たせ、等価性が崩れて再バインドが走る**経路でこれを満たしている (Sample のコメントもそう明言している)。この経路は spec 文言に適合しており、指摘は spec 違反ではない。

一方、ライブラリ利用者がごく自然に書く「展開状態を builder 内の `remember` / `@State` だけに持つ」形 — つまり再バインドを伴わず composition / SwiftUI ツリー内部のサイズだけが変わる形 — は、単体テストにも Sample にも実機検証にも 1 件もない。この形で行高さが追従するかどうかは、Android では `ComposeView` の `requestLayout` が RecyclerView へ伝播するか、iOS では `UIHostingConfiguration` が self-sizing を再申告するかに依存し、design.md の Risks が「機種依存のもたつきが理論上あり得る」と挙げていたのはまさにこの経路である。

「利用者・ライブラリのどちらにも専用の再計測 API を要求しない SHALL」という文言は、この経路まで含めて読まれる可能性が高い。

**推奨修正**: Sample の動的高さデモに「状態を builder 内に閉じ込めた行」を 1 行追加して挙動を確認するか、確認しないなら「本変更が保証する自動追従は content 差し替えによる再バインド経路であり、builder 内部の状態変化のみによる高さ変化は未検証」と `deviation.md` に明記する。

---

### [🔵 Suggestion] `setContent` を bind ごとに呼ぶ方式は既存の `bindKsAnyView` と非対称

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:55-63`, `:82-86` (`reset` の `setContent {}`)
- 比較対象: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:313-341`

`bindKsAnyView` は「`setContent` は ViewHolder 生成時 1 度だけ、bind では `MutableState<@Composable () -> Unit>` の値だけ差し替える」方式を採り、その理由を「再 bind 時に毎回コンポジションが新規作成されるのを防ぎ、`ComposeView` の再利用効果と Compose の差分更新を活かす」と明記している (`SectionAccessoryViewHolders.kt:301-306`)。

CustomCellViewHolder は bind ごとに新しいラムダで `setContent` を呼ぶため、同じ位置に別のラムダが入り composition subtree が作り直される。観察される差は 2 つ:

1. スクロールによる再 bind ごとに composition が再構築される (行数が多い設定画面での無駄)
2. builder 内の `remember` が再バインドをまたいで保持されない

現状の Sample は状態をすべて外側の `@State` / `remember` に持ち上げているため 2 の影響を受けず、`design.md` Decision 4 も「`bind` で `composeView.setContent { ... }`」と明記しているので**設計どおりの実装**である。よって修正は求めない。ただし house pattern が既に逆の判断を明文で持っている以上、なぜ CustomCell では素朴な `setContent` でよいのか (あるいは将来 `bindKsAnyView` 方式へ寄せるのか) を、`deviation.md` か蒸留時の concepts に一言残しておくと後から読む人が迷わない。

---

### [🔵 Suggestion] `registerCustomCell(context)` の未使用引数

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryCustomCell.kt:22-31`

`@Suppress("UNUSED_PARAMETER")` を付けた public API の未使用引数。「基本 Cell / 入力 Cell の登録 API とシグネチャを揃えるため」という理由が KDoc に書かれており、判断としては妥当。公開 API なので後から引数を落とすのは破壊的変更になる点だけ意識しておきたい。実装上の問題はない。

---

### [🔵 Suggestion] `tasks.md` 4.3 が未了

`4.3 デルタスペック全 Scenario とテストの対応を確認 (ksn-verify の事前セルフチェック)` が未チェックのまま。本レビューは対応表の機械的検証 (ksn-verify の責務) を行っていないため、アーカイブ前に verify 工程を通すこと。なお本レビューで読んだ範囲では、spec の全 Requirement に対応するテストが iOS / Android 双方に存在することは確認できている。

## 確認したが問題を検出しなかった観点

- **iOS deployment target (`.iOS(.v16)`)**: 新規 4 ファイルに iOS 17+ / 18+ 専用 API は見当たらない。使用しているのは `UIHostingConfiguration` / `.margins` (16.0+)、`UICollectionViewListCell.defaultBackgroundConfiguration()` (14.0+)、`Color(uiColor:)` / `foregroundStyle` (15.0+) のみ。可用性違反があればデプロイメントターゲット下でコンパイルが通らないが、実際に 540 件がビルド・実行できているため、**「iOS 16 で動作しない実装上の根拠」は diff から見つからなかった**。ただし iOS 16 実機での `UIHostingConfiguration` self-sizing の実挙動は静的解析では担保できないため、申し送りどおり実機確認は別途必要である
- **等価性**: 関数値 (`builder` / `onTap`) の除外、表示スカラーの参加、静的形の空 content が常に相等 — iOS / Android 双方でテスト済み。Android の `equals` は `other !is CustomCell<*>` でチェックしており、star projection 越しでも安全。`content 型が異なれば非等価` のケースもテストがある
- **再利用境界**: iOS `prepareForReuse` で `contentConfiguration = nil` + `tapHandler = nil`、Android `reset` で listener 解除 + `setContent {}`。いずれも「前の content 表示が残らない」ことを probe で実測しているテストがある。Composition の破棄は `ComposeCellViewHolder` の `DisposeOnDetachedFromWindow` が担保しており、基底の設計意図どおり
- **タップ競合**: Android は `AndroidComposeView` が pointer input を消費した場合に親 `ComposeView` の `onTouchEvent` へ届かない性質を使っており、実 `MotionEvent` を流すテスト (`子要素の操作では行タップが発火しない`) で実経路を確認している。iOS は行タップ用の gesture recognizer を Cell に追加せず `didSelectItemAt` 経路のみに乗せており、その不在自体をテストが検証している (`test_行タップ用のgestureRecognizerをCell自身に追加しない`)
- **無効化**: iOS は `KsCellViewSupport.setRenderState` の `isUserInteractionEnabled = false` と hosted content への `.disabled(true)` の二重、Android は Initial パスでの pointer 消費。いずれも「無効時は content 内の操作も抑止される」Scenario に対応するテストがあり、iOS 側は mutation probe (有効に戻すとヒットテストが成立する) まで置かれている。`isEnabled=false` 時に押下フィードバックが残らないことも両プラットフォームで満たされている (`cell-visual-states.md` の「無効 Cell に押下・選択 feedback を残さない」に適合)
- **chevron の見た目一致**: 定数の共有 (`KsChevronAppearance` / `CELL_DISCLOSURE_WIDTH_DP` 等) で構造的に担保したうえで、`ui/verification/` の拡大スクリーンショットで bbox・右端余白・画素差まで実測している。既存 `CommandCellViewHolder` / `makeChevronView()` 側も同じ定数を参照するようリファクタされており、片側だけ変わる余地がない
- **Sample の platform parity**: 両 Sample の文字列リテラルを機械比較したところ、差分は文字列補間の構文差のみで、文言・セクション構成・パラメータは一致している (`cross/conventions/sample-parity.md` 準拠)。`SampleTheme` の追加色も iOS / Android で同一 RGBA を定義し、生値の二重管理を作っていない
- **コメント方針**: 新規ファイルのコメントはいずれも「何を・なぜ」が単独で読め、外部文書 ID だけに依存した説明になっていない (`cross/conventions/comment-policy.md`)
- **ktlint / detekt**: 本リポジトリの Gradle には該当タスク・プラグインが構成されていないため、適用対象外と判断した

## アクションプラン

1. **[Major] 押下フィードバックの契約を決める** — Android を共通行に揃える / iOS を `tapHandler` 条件に揃える / 現状を是として `deviation.md` に記録する、のいずれか。オーナー判断が必要
2. **[Major の付随]** `onTap` 未指定 (既定) の CustomCell 行の押下時スクリーンショットを iOS / Android 両方で `ui/verification/` に追加する
3. **[Minor]** `deviation.md` を作成し、(a) iOS 基底クラスの具体化、(b) Android の高さ解決を Compose ツリー内で行っている点、(c) 「高さの自動追従」の検証範囲 (再バインド経路のみ) を記録する
4. **[Minor]** iOS 全件実行のフレーク (`InputCellsTests` の becomeFirstResponder) を記録し、再発を監視する
5. **[Suggestion]** `setContent` を bind ごとに呼ぶ方式と `bindKsAnyView` 方式の違いを、蒸留時に concepts か deviation へ一言残す
6. **[Suggestion]** `tasks.md` 4.3 (ksn-verify) をアーカイブ前に通す
