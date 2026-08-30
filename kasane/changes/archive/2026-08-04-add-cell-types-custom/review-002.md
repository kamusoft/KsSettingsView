# レビュー結果: add-cell-types-custom (002 回目)

**日付**: 2026-08-04
**判定**: CHANGES_REQUESTED

## サマリー

前回 (review-001 / second-opinion-002) の指摘 8 件とオーナー指摘 3 件のうち、**10 件は実装として正しく解消されている**。iOS の `contentType: ObjectIdentifier` は等価性・hash の一貫性を保ったまま `AnyHashable` の型正規化を塞いでおり、公開 API の互換も壊していない。Android の 3 層無効化 (`clearAndSetSemantics` + `View.isEnabled` + `descendantFocusability`) は実機の accessibility ツリーダンプまで証跡を取っており、`isClickable = isEnabled` への変更も `setOnClickListener(null)` が clickable を立て直す Android の癖を踏まえた正しい順序になっている。`CustomCellRowPlacement` は intrinsic 問い合わせ (proposal が nil / 無限大) を自然高へ落としているため self-sizing を壊しておらず、chevron との共存・静止時の中央配置も実測テストで押さえられている。

残る問題は 1 件で、**オーナー指摘 #10「無効時の淡色化を Android でも行い iOS と揃える」への修正が、揃えたつもりの方向とは逆向きの非対称を新たに作っている**点である。Android は content 全体を淡色化するのに対し iOS の `.disabled(true)` は SwiftUI 標準コントロールしか淡色化しないため、Text だけの CustomCell では Android が薄く iOS が濃いまま残る。証跡スクリーンショット自体にその差が写っている。これはオーナー判断が要る契約の穴であり、`deviation.md` は逆に「揃った」と記録しているため、判定を CHANGES_REQUESTED とする。コード修正が必須という意味ではなく「決めて記録し直す」ことが必要という意味である。

### 実行結果 (件数併記)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ./gradlew test lintDebug` | **BUILD SUCCESSFUL** / `build/test-results/**/TEST-*.xml` 集計 **1848 tests / 0 failures / 0 errors / 0 skipped**、lintDebug 通過 |
| iOS | `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<iPhone 17 / iOS 26.5>'` | **TEST SUCCEEDED** / xcresult 集計 **546 passed / 0 failed / 0 skipped** |

前回 Minor 6 の `InputCellsTests` フレークは**再発しなかった** (今回 1 回実行、546/0)。前回の 3 回連続グリーンと合わせて、フレークは解消したものとして扱ってよい。

足場アーティファクトの状態:

- `specs/` `proposal.md` `design.md` `ui/mock/` は未変更 (`git status` で確認)
- `tasks.md` はチェックと検証メモの追記のみ。1.1〜4.2 のチェックに虚偽はなく (実物を確認)、4.3 (ksn-verify 事前チェック) は未チェックのまま正しく残されている
- `ui/brief.md` の追記 (照合結果 / 合意済み妥協) は `ksn-core/references/ui-artifacts.md` が brief.md の役割として明記している内容であり、足場の書き換えには当たらない

---

## 指摘事項

### [🟠 Major] 無効時の淡色化が iOS↔Android で逆向きの非対称になっており、deviation.md の記述と実物が食い違う

**該当箇所**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:152` (`Modifier.alpha(DISABLED_CONTENT_ALPHA)`)
- `ios/Sources/KsSettingsViewUI/CustomCellHostedContent.swift:43` (`.disabled(!isEnabled)`)
- 記述の食い違い: `kasane/changes/add-cell-types-custom/deviation.md:8-10`
- 公開ドキュメントの食い違い: `android/.../CustomCell.kt:43-45` ↔ `ios/Sources/KsSettingsViewUI/CustomCell.swift:99-100`
- 証跡: `ui/verification/android-pixel6a-09-disabled-dimmed-m3-slider.png` ↔ `ui/verification/ios-sim-iphone17-ios265-07-slider-enabled-dragged-disabled-blocked.png`

**問題点**:

オーナー指摘 #10 は「Android も淡色化して iOS と既定挙動を揃える」であり、`deviation.md:8` も「iOS と既定挙動を揃える」「見た目の濃度は揃う」と記録している。しかし 2 つの機構は淡色化の**適用範囲が異なる**。

| content の要素 | iOS (`.disabled(true)`) | Android (`Modifier.alpha(0.38f)`) |
|---|---|---|
| `Slider` / `Button` 等の標準コントロール | 淡色化される | 淡色化される |
| `Text` / `Image` / 自前描画 | **淡色化されない** | **淡色化される** |

SwiftUI の `.disabled` は環境値 `isEnabled` を伝播し、それを読む標準コントロールだけが淡色描画に切り替わる。`Text` や `Image` は環境値を読まないため素の色のまま残る。一方 Compose の `Modifier.alpha` は subtree のレイヤ不透明度なので、content に含まれるものすべてに掛かる。

これは机上の推論ではなく、両プラットフォームの証跡スクリーンショットに写っている。同じ「無効」行を比べると:

- Android (`android-pixel6a-09`): ラベル「無効」・スライダー・数値「60」がすべて淡色
- iOS (`ios-sim-iphone17-ios265-07`): スライダーだけが淡色で、ラベル「無効」は隣の「明るさ」「音量」と同じ黒、数値「60」も同じ濃度

つまり修正前は「Slider が iOS だけ薄い」非対称だったものが、修正後は「Text が Android だけ薄い」非対称に置き換わっただけで、**非対称そのものは解消していない**。しかも `Text` だけの CustomCell (最も素朴な使い方) では全面的に差が出るため、影響範囲は修正前より広い可能性がある。

さらに公開 KDoc / doc comment が platform ごとに別の契約を宣言している状態になっている:

- Android `CustomCell.kt:44-45`: 「あわせて content を淡色化するが、これは既定の振る舞い」
- iOS `CustomCell.swift:100`: 「無効時の見た目の描き分け（淡色表示等）は builder 側＝利用者の責務。」
- iOS `CustomCellHostedContent.swift:42`: 「見た目の描き分けは利用者責務のため、色の置換等はライブラリ側では行わない。」

`concepts/core/styling/cell-visual-states.md:45` は「iOS と Android の押下対象の違いを同一契約として断定しない」— 差があること自体は許すが、**名前を付けて残す**方針である。現状は「揃えた」と記録された上で差が残っており、この方針と噛み合わない。

**推奨修正** (いずれか。レビュアーは可否を決めない):

1. iOS 側にも content 全体の淡色化を入れて実際に揃える (`CustomCellHostedContent` の `HStack` に `.opacity(isEnabled ? 1 : 0.38)`。SwiftUI コントロールには `.disabled` の淡色化と二重に掛かるため、掛けるなら `.disabled` 側の淡色化と重ならない値・順序の検討が要る)
2. Android の淡色化を撤回し、両プラットフォームとも「標準コントロールだけが淡色化される」に揃える (オーナー指示 #10 の再判断になる)
3. 現状を是として `deviation.md` の記述を「Slider 等の標準コントロールについては揃えた。任意の Text / Image については iOS は素の色のまま、Android は淡色化される差が残る」に**書き直し**、公開 doc comment を両プラットフォームで同じ表現に揃える。蒸留時に `cell-visual-states.md` へ差を追記する

どの案でも、`Text` だけの content を持つ無効 CustomCell の見え方を iOS / Android 両方で `ui/verification/` に追加してほしい (現状の証跡は Slider 行しか撮られておらず、この差が写っているのは偶然である)。

---

### [🟡 Minor] 新規追加のコメントが 1 箇所だけコメント規約に違反したまま残っている

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1862`

```swift
// 仕様: kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md "行タップ" Requirement。
```

**問題点**:

前回指摘 #4 の対応は「今回の新規/変更ファイルのコメントを書き換えた」とされているが、**本変更が新規に追加した行**にアーカイブ文書のパス参照が残っている。`concepts/cross/conventions/comment-policy.md:29` が「アーカイブ文書のパス / 行番号」として明示的に禁止している形式そのものである。

新規 20 ファイル側は機械検査 (`kasane/` `openspec/` `changes/` `Decision [0-9]` `Phase [0-9]` `MUST` `SHALL` 等の grep) で 1 件も検出されず、書き換えは正しく行われている。取りこぼしはこの 1 行だけ。なお直上の 1855 行目にある `openspec/changes/...` 参照は既存行であり、別 change (既存 74 ファイル) の対象。

**推奨修正**: 参照句を削除し、直前 2 行の自己完結した説明 (「`onTap` 指定時のみ行タップを発火する」「二重発火しない」) だけを残す。定型句型 (comment-policy.md:49) の書き換えに当たる。

---

### [🟡 Minor] Sample の Slider 色の変更が Android 側だけに適用され、sample-parity が崩れている (未追跡)

**該当箇所**:
- `samples/ios/KsSettingsViewSample/SampleSliderCell.swift:83` (`.tint(Color(uiColor: SampleTheme.mauiAccent))` が残っている)
- `samples/android/.../SampleSliderCell.kt:95-105` (`SliderDefaults.colors(...)` を撤去し M3 既定へ)
- 規約: `concepts/cross/conventions/sample-parity.md`

**問題点**:

オーナー指摘 #11「Sample の Slider を M3 標準の見た目に戻す」は Android にのみ適用され、iOS は `SampleTheme.mauiAccent` (琥珀色) の tint を保持している。結果として同じデモ画面の同じ行が、**Android は Material 3 の紫、iOS は琥珀色**で描画される (証跡 `android-pixel6a-09` と `ios-sim-iphone17-ios265-07` を並べると一目で分かる)。

`sample-parity.md` は「各 Cell に渡すパラメータ (…`accentColor` 等の色) を一致させる」「platform ごとに独自の『改善』をしない」「片側だけの文言・構成・デモデータの変更を**追跡なしで放置しない**」と定めている。Sample はプラットフォーム間差が「本体の仕様差」か「Sample の書き方の差」かを判別するための検証装置 (cross/ADR-0016) であり、Sample 側に無記録の色差があるとその機能が落ちる。

加えて、Android 側のコメント (`SampleSliderCell.kt:95-96`) は「標準コントロールを素のまま置いたときにどう見えるかを示す方が例として素直なため」と理由を述べているが、iOS がその理由に従っていないため、コメントと実物が食い違っている。

**推奨修正**: iOS 側の `.tint(...)` も外して両プラットフォームとも標準描画に揃える (Android のコメントの理由がそのまま iOS にも当てはまる)。揃えないなら `deviation.md` に「Sample の Slider 色は Android のみ M3 既定へ戻した。iOS の追随は後続」と追跡を残す。

---

### [🔵 Suggestion] `clearAndSetSemantics` の代替 — SwiftUI の `.disabled` に対応する Compose の機構は CompositionLocal である

**該当箇所**: `android/.../CustomCellViewHolder.kt:218-223` (`blockDescendantActions`)

現行実装の選択 (`mergeDescendants` では操作可能な子孫が独立ノードとして残るため `clearAndSetSemantics` を使う) は**技術的に正しい**。Compose には「subtree の action だけを剥がして text は残す」公開 API が存在せず、`importantForAccessibility = NO_HIDE_DESCENDANTS` も読み上げごと消えるため代替にならない。この点は現状が現実的な最善である。

そのうえで、iOS と非対称になる根本原因を指摘しておきたい。SwiftUI の `.disabled(true)` は `EnvironmentValues.isEnabled` を子孫へ伝播し、**各コントロールが自分で** 無効描画・無効 action に切り替える。だから読み上げが残る。Compose にはこの環境値に相当する組み込みがない (`LocalEnabled` は存在しない) ため、ライブラリ側が外から一括で塞ぐしかなく、読み上げまで巻き込む。

対称性を回復する道があるとすれば、同じ機構をライブラリが用意することになる:

```kotlin
// ks-settingsview-ui 側で定義し、CustomCellRow が isEnabled を提供する
val LocalKsCellEnabled = compositionLocalOf { true }
// 利用者側: Slider(enabled = LocalKsCellEnabled.current, ...)
```

これなら控えめな `Modifier.semantics { disabled() }` だけで済み、読み上げは残り、コントロールは正しく「無効」とアナウンスされる。引き換えに**利用者の協力が必要**で、協力しない content は無効化されない。したがって現行の強制遮断 (pointer / View.isEnabled / focusability) を残したまま `clearAndSetSemantics` だけを `semantics { disabled() }` へ緩め、CompositionLocal を併設する二段構えが取り得る最善手だと考える。

本変更のスコープを超えるため修正は求めない。オーナー判断の材料として記す。

---

### [🔵 Suggestion] 行に収まらない content の描画が iOS では行外へはみ出し、Android では切れる

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift:60-68` (content には常に自然高を提案して置く)
- `android/.../CustomCellViewHolder.kt:198-199` (`CenterOrTopVertically`)

`CustomCellRowPlacement` は「収まらないときは上端揃え」を実現するために content へ高さ無制限を提案し、はみ出しを許す。この設計は遷移中の飛び出しを消すという目的に対して正しく、`test_contentが行高さを超える間はcontentの上端が行の上端に固定される` が実測で押さえている。

ただし、はみ出した部分の扱いが platform で異なる。Android は `RecyclerView` の `clipChildren` (既定 true) で行の外が切れる — `CustomCellViewHolder.kt:195-196` のコメント自身がそう述べている。iOS の `UICollectionViewCell` は既定で `clipsToBounds == false` のため、はみ出した content が**後続行の上に描かれる**。`Theme.hasUnevenRows == true` (既定) では 1 レイアウトパスの間だけなので実害は小さいが、`hasUnevenRows == false` かつ content が `cellHeight` を超える構成では恒久的に重なる。

修正を求めない理由: `hasUnevenRows == false` は明示指定のオプトインであり、そこで content を行高さより大きく作るのは利用者側の設計ミスに近い。ただし platform で見え方が変わる点は名前を付けておく価値がある (`cell-visual-states.md:45` の方針)。蒸留時に一言残すか、`CustomCellView` の `contentView.clipsToBounds` を検討するかのいずれか。

---

### [🔵 Suggestion] chevron の末端余白だけが定数共有になっておらず、iOS では 16 が二重定義されている

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/KsChevronAppearance.swift:47` (`trailingMargin: CGFloat = 16`)
- `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:111` (`s.layoutMargins = UIEdgeInsets(top: 6, left: 16, bottom: 6, right: 16)`)

`KsChevronAppearance` のファイル冒頭コメントは「双方が本定数を参照することで…構造的に担保する」と書いているが、末端余白だけは共有されていない。UIKit 経路は `KsListCellBase` の literal `16`、宣言 UI 経路は `KsChevronAppearance.trailingMargin` の literal `16` で、**片方を変えるともう片方が追随しない**。symbolName / textStyle / scale / tintColor は正しく共有されているので、残るのはこの 1 項目だけ。

Android 側は `CELL_ROW_HORIZONTAL_PADDING_DP` を `buildCellBaseViews` と `CustomCellRow` の両方が参照しており、こちらは構造的に担保できている。

**推奨修正**: `KsListCellBase.swift:111` の左右マージンを `KsChevronAppearance.trailingMargin` (または行マージン用の別定数) から取るようにする。定数の置き場所が chevron 用としては座りが悪いなら、`CELL_ROW_HORIZONTAL_MARGIN` 相当を切り出して両者が参照する形でもよい。

---

### [🔵 Suggestion] `ui/brief.md` に生カラー値が書かれている / `ui/verification/` の重量

**該当箇所**: `kasane/changes/add-cell-types-custom/ui/brief.md` の「トークン候補」節、`ui/verification/` (47 ファイル / 15 MB、mp4 1 本と修正前フレーム 5 枚を含む)

`ksn-core/references/ui-artifacts.md` は brief.md について「書かないもの: px 値・**生カラー値**・具体レイアウト」、verification について「中間ラウンドのスクリーンショットは保存しない (…足場層を重くしない)」としている。

- 生カラー値 (#FAF3D9 等) は `SampleTheme` の識別子参照だけに置き換えられる (値はコードが持つ)
- 遷移フレーム列を残すこと自体は `lessons/inbox/static-screenshots-miss-transition-animation-defects.md` のルール文が明示的に要求しており、規約と正面から衝突する。ただし `08-expand-before-*` (修正前) の 5 枚と `.mp4` (500 KB) は「何が承認されたか」ではなく「修正過程」であり、証跡としては修正後の `09-*` で足りる

いずれも実装の問題ではない。アーカイブ前に整理するか、lessons のルールと ui-artifacts.md の緊張関係として蒸留時に扱うかの判断を残しておきたい。

---

### [🔵 Suggestion] `onTap` なしの CustomCell が accessibility 上「操作可能」と告知される

**該当箇所**: `android/.../CustomCellViewHolder.kt:81` (`composeView.isClickable = isEnabled`)

指摘 #1 の修正として正しく、`cell-visual-states.md:21` と `LabelCellViewHolder.kt:37` に揃った。ただし副作用として、`isClickable == true` の `ComposeView` は `AccessibilityNodeInfo` に `ACTION_CLICK` を載せるため、TalkBack は callback を持たない行でも「ダブルタップで作動」と読み上げる。

LabelCell も同じ性質を持つ既存の共通挙動なので新規の逸脱ではない。ただし CustomCell は content 自身が実 action を持つ (Slider 等) ケースがあり、その場合「no-op の行 action + 実 action の子」が並ぶ点は LabelCell にはない形になる。実害の証拠はないため修正は求めないが、蒸留時に共通行の clickable 契約を concepts へ書くなら、この副作用も一緒に名前を付けておくとよい。

---

### [🔵 Suggestion] `tasks.md` 4.3 (ksn-verify) が未了

`4.3 デルタスペック全 Scenario とテストの対応を確認` が未チェックのまま。本レビューは対応表の機械的検証 (ksn-verify の責務) を行っていないため、アーカイブ前に verify 工程を通すこと。前回同様、読んだ範囲では spec の全 Requirement に対応するテストが iOS / Android 双方に存在することは確認できている。

---

## 前回指摘への対応の検証結果

| # | 前回指摘 | 検証 | 判定 |
|---|---|---|---|
| 1 | Android `isClickable` が視覚状態契約に違反 | `CustomCellViewHolder.kt:81` で `isClickable = isEnabled`。`setOnClickListener(null)` が clickable を立て直す Android の挙動を踏まえ、代入がその**後**に来ている (`reset()` も同じ順序)。テスト `onTap がなくても有効な行は押下 feedback のために clickable を持つ` が clickable / hasOnClickListeners を分けて実証。実機証跡 `android-pixel6a-10` / `-11` も追加済み | **解消** |
| 2 | iOS `AnyHashable` が `Int(1)` と `Double(1.0)` を等価判定 | `contentType: ObjectIdentifier` を `==` / `hash` の両方に追加。等価なら contentType も content も一致するので hash 契約は保たれる。`withDSLID` / `withStyle` は内部 init 経由で引き継ぎ、専用テスト (`test_content実体型の区別はwithDSLIDとwithStyleのcopyでも保たれる`) あり。DSL 層にも `test_content実体型の変化でreplaceCellが発行される` を追加。`contentType` は `internal` で公開 API に露出せず、public memberwise init も元から存在しないため互換は無傷 | **解消** |
| 3 | Android の無効化が semantics 経由の操作を遮断しない | `clearAndSetSemantics { disabled() }` + `composeView.isEnabled = false` + `FOCUS_BLOCK_DESCENDANTS` の 3 層。テストは semantics ツリー全ノードの `OnClick` / `OnLongClick` / `SetProgress` を総当たり実行して「実行できた action 0 件」を確認し、有効時に同じ総当たりで 1 件発火する mutation probe まで置いてある。実機 `uiautomator dump` (`android-pixel6a-14` / `android-pixel4a-04`) で TalkBack が見る実ツリーからも SeekBar が消えていることを確認済み | **解消** (読み上げが消える副作用は Suggestion 4 参照。オーナー判断中の件として blocking にはしない) |
| 4 | コメントが `changes/` パス・`Decision N` を参照 | 新規 20 ファイルは grep で 0 件。ただし変更ファイル `KsSettingsViewController.swift:1862` に 1 件残存 | **未解消 (1 箇所)** → Minor 1 |
| 5 | deviation 未記録 | `deviation.md` 作成済み。Decision 4 / 5 の具体化・オーナー指示・未規定事項の実装判断・検証範囲の限界を網羅 | **解消** (ただし淡色化の記述が実物と食い違う → Major) |
| 6 | iOS テスト flaky | 今回の全件実行でも再発せず (546/0)。`CustomCellTests` の `host(_:)` は依然 window を後片付けしないが、実害は観測されていない | **解消** |
| 7 | 高さ追従の検証が再バインド経路のみ | iOS `test_content同値のままbuilder内部の状態変化で行高さが追従する` (`ObservableObject` で builder 内の View だけを動かし、展開・折りたたみ両方向を実測)、Android `builder 内部の状態変化だけでも行高さが追従する` (`remember { mutableStateOf }` を外から toggle し、再 measure を挟まずに `measuredHeight` の変化を見る) を追加。いずれも再バインド API を呼ばない経路 | **解消** |
| 8 | `registerCustomCell` 未使用引数 | 現状維持。KDoc に理由が書かれており判断として妥当 | **合意** |
| 9 | iOS 動的高さの遷移が不自然 | `CustomCellRowPlacement` を新設。`sizeThatFits` は有限提案をそのまま返し、提案なし (intrinsic 問い合わせ) / 無限大のときだけ自然高を返すため self-sizing は壊れていない (`test_hasUnevenRowsがtrueならcellHeight未指定でもcontentの自然高に追従する` が実証)。`placeSubviews` の「余白があれば等分、無ければ 0」は上下端の両ケースをテストで押さえてある。chevron は Layout の内側の `HStack` にあり、修正後の実測 (`ios-sim-...-11-post-fix-chevron-vs-commandcell.png`) で bbox・右端余白が修正前と同一。修正前後のフレーム列で飛び出しの消失も確認済み。Android も `CenterOrTopVertically` で同じ配置に揃えた | **解消** |
| 10 | 無効時の淡色化を Android でも行い iOS と揃える | Android に `alpha(0.38f)` を追加。Slider については揃ったが Text 等では逆向きの差が残る | **部分解消** → Major |
| 11 | Sample の Slider を M3 標準に戻す | Android のみ適用。iOS は `.tint` を保持 | **部分解消** → Minor 2 |

## 確認したが問題を検出しなかった観点

- **`CustomCellRowPlacement` の正しさ**: `finite(_:)` が `nil` と非有限をどちらも「提案なし」に落としているため、intrinsic 問い合わせ (`ProposedViewSize.unspecified` / `.infinity`) では自然高が返り self-sizing が成立する。`ProposedViewSize.zero` では 0 を返すが、これは「最小まで縮められる」という正しい申告であり、行高さの決定経路には使われない。`Layout` は iOS 16.0+ で deployment target (`.iOS(.v16)`) を満たす。`placeSubviews` が `sizeThatFits` を再度呼ぶ二重測定はキャッシュ (`cache: inout ()`) を使っていないが、subview 1 個の測定であり実害はない
- **`contentType` の等価性・hash 一貫性**: `==` に参加する要素はすべて `hash(into:)` にも入っており、`x == y ⟹ hash(x) == hash(y)` が保たれる。`EmptyContent` の静的形も両 init で同じ `ObjectIdentifier(EmptyContent.self)` を入れるため、省略形同士の等価性は content 以外の要素だけで決まるという spec の要求を維持している
- **`Modifier.alpha(0.38f)` と `cell-visual-states.md`**: 「行全体の alpha だけで disabled 状態を表現しない」に対し、実装は content 部分 (`weight(1f)` の `Box`) にのみ適用し、行背景と Disclosure Indicator は対象外にしている。行全体ではないうえ `deviation.md:20` に緊張関係が記録済みであり、違反として扱わない。ただし手段の差が Major 指摘の非対称の原因になっている点は上記のとおり
- **Android accessibility テストの実証範囲**: `performAllActions` は Compose の semantics ツリー (`SemanticsNode.children` = 置換済み semantics を除いた、accessibility service が見るのと同じ構造) の action を直接呼ぶもので、実 accessibility service の配送そのものではない。ただし実機 `uiautomator dump` が同じ結論を独立に裏付けており、単体テストの限界は証跡側で補われている
- **`setOnClickListener(null)` の副作用**: Android の `View.setOnClickListener` は引数が null でも `setClickable(true)` を呼ぶ。`bind` (75→81 行) / `reset` (99→100 行) のいずれも代入がその後に来ており、意図した clickable 状態に落ち着く
- **無効化とタッチ配送**: `ViewGroup.dispatchTouchEvent` は自身が disabled でも子へ配送するため `View.isEnabled = false` だけでは content 内の操作を止められないが、`consumePointerInput` の Initial パス消費が実 `MotionEvent` テストで実証されている。3 層の各層が別経路を塞いでおり冗長ではない
- **Sample の文言・構成 parity**: 両 Sample の文字列リテラルを機械比較したところ、差分は文字列補間の構文差のみで、文言・セクション構成・ダミー行の 0 埋め番号まで一致している。色は Slider tint を除き `SampleTheme` に一元化されている
- **新規ファイルのコメント規約**: 新規 20 ファイル (実装 / テスト / Sample) を `kasane/` `openspec/` `changes/` `Decision [0-9]` `Phase [0-9]` `design.md` `mock` `SHALL` `MUST` `SHOULD` で grep して 0 件。いずれのコメントもファイル単独で読める説明になっており、参照は `core/ADR-0014` / `core/ADR-0015` のみ (comment-policy.md の許容形式)
- **Kotlin 言語層** (kotlin-impl-skill 観点): `CustomCell<Content : Any>` の型制約で non-null を強制、`val` のみの不変クラス、手動 `equals` / `hashCode` は `data class` が関数値を除外できないための正当な選択で KDoc に理由あり。`composeContent` による型消去エントリポイントは star projection 越しのキャストを避ける正しい手法。`!!` はテストコード内の 1 箇所 (`toggle!!.invoke()`) のみでプロダクションコードには無い。Coroutines / Flow は不使用。`@Suppress("UNUSED_PARAMETER")` は範囲が最小で理由が KDoc にある
- **ktlint / detekt**: 本リポジトリの Gradle には該当タスク・プラグインが構成されていないため適用対象外。代替として `lintDebug` が通過している

## アクションプラン

1. **[Major] 無効時の淡色化の非対称を決着させる** — iOS も content 全体を淡色化する / Android の淡色化を撤回する / 現状を是として `deviation.md` と両プラットフォームの公開 doc comment を実物に合わせて書き直す、のいずれか。オーナー判断が必要
2. **[Major の付随]** `Text` だけの content を持つ無効 CustomCell の見え方を iOS / Android 両方で `ui/verification/` に追加する
3. **[Minor]** `KsSettingsViewController.swift:1862` の `kasane/changes/...` 参照を削除する
4. **[Minor]** Sample の Slider 色を iOS 側も M3 / 標準描画へ揃える (揃えないなら `deviation.md` に追跡を残す)
5. **[Suggestion]** `KsChevronAppearance.trailingMargin` と `KsListCellBase` の行マージンを同一定数から引く
6. **[Suggestion]** `ui/brief.md` の生カラー値を識別子参照へ、`ui/verification/` の修正前フレーム・動画の要否を判断する
7. **[Suggestion]** `tasks.md` 4.3 (ksn-verify) をアーカイブ前に通す
