# レビュー結果: fix-ios-tapnotifyingrenderer-actor-isolation (001 回目)

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED

## サマリー

Swift 6 言語モードのエラー 3 群 (準拠 11 件 / closure 1 件 / deinit 8 件) はいずれも意図どおり解消されており、error 0 件・644 tests / 0 failures・実操作証跡まで揃っている。特に deinit の全削除は、過去に「isolated メソッドへの委譲でティアダウンを一本化する」修正が actor isolation でビルド失敗した経緯 (`kasane/lessons/inbox/deinit-teardown-consolidation-breaks-isolation.md`) を繰り返さない解き方で、所有関係 (購読は全 `[weak self]`・`UICollectionView.dataSource`/`delegate` は weak) の読み直しとも整合する。

一方で、本 change が新規に追加した唯一の押下ハイライト解除テストが UIKit の実経路を通っておらず、昇格済みルール `kasane/lessons/test.md` L-001 (実経路で検証する / 状態遷移は往復両方向を通す) に正面から抵触する。これが Major であり CHANGES_REQUESTED とする。他は証跡の粒度とコメントの自己完結性に関する Minor が中心で、設計判断そのものへの異論はない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース) |
| `kasane/handbook/cross/test-execution.md` | テスト実行・テスト結果の報告 (tasks 4.2 の件数報告、収束を待つアサーション) |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (タッチフィードバック・Controller 解放) の完了判定と証跡 |
| `kasane/handbook/cross/public-identifiers.md` | 該当なし (`ios/Package.swift` は Swift 6 一時設定のみで復元済み・差分 0 件。配布座標の決定を含まない) |
| `kasane/handbook/ios/` | 不在 (iOS ドメイン規約は未作成。proposal What Changes (4) で蒸留時に新設予定) |
| `kasane/lessons/code-review.md` L-001 | ミューテーションによる回帰検出力の実測 (Suggestion-6 で言及) |
| `kasane/lessons/test.md` L-001 | 実経路検証・往復遷移 (Major-1 の根拠。code-review scope 外だが本件に直接該当するため適用) |
| `kasane/lessons/process.md` L-003 | 利用者可視の変更に対する視覚証跡 (Minor-5 の根拠) |
| skills (config 解決): `swift-ui-impl-skill` | ios ドメインの impl スキル (Swift Concurrency 観点) |

`kasane/decisions/` に本 change の対象領域 (行タップ通知・Controller ティアダウン・Swift 言語モード) を縛る ADR は無し。`kasane/concepts/` にも `deinit` / `disconnectStore` に言及する記述は無く (grep で確認)、長命層の追随漏れは発生していない。

## 指摘事項

### [🟠 Major] 押下ハイライト解除のテストが UIKit の実経路を通っていない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift:24-46`

**問題点**:

追加された `test_押下解除後は平常時の実効背景色へ戻る` は、`configurationUpdateHandler` を `handler(cell, cell.configurationState)` としてテストコードから直接呼び出している。これは以下の 3 点で、担保しようとしている契約を検証できていない。

1. **昇格済みルールへの抵触**: `kasane/lessons/test.md` L-001 は「UI の動的挙動のテストは、保証したい性質そのものを実経路で検証する。観測しやすい代理値 (…直接代入した状態…) が緑でも保証にならない。状態遷移は往復両方向を通す」と定める。ここでの実経路は「`isHighlighted` の変化 → UIKit が `updateConfiguration(using:)` を経て handler を呼ぶ」であり、handler の直接呼び出しはその代理でしかない。UIKit が handler を呼ばなくなる回帰 (例: `installSelectedColorHandler` の呼び出し漏れ、`configurationUpdateHandler` の後から上書き) をこのテストは一切検出しない。
2. **本 change の書き換え内容と噛み合っていない**: 今回の修正の核心は `MainActor.assumeIsolated` (`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:81`) であり、これは「呼び出し文脈が main actor である」という前提が破れたら trap する構造である。`@MainActor` を付けたテストクラスから直接呼べば前提は常に自明に成立するため、この検証は呼び出し文脈について何も語らない。
3. **実経路のテストが既にあり、解除側だけが漏れている**: 押下側は `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:985` の `test_押下背景も箱形状に収まる` が `host(...)` で window に載せ、`cell.isHighlighted = true` → 収束待ち → 背景色 assert という実経路で通している。つまり同じテストターゲット内に手本があり、解除側だけが代理経路になっている。テストターゲット全体を `configurationUpdateHandler` で検索しても参照はこの新規テスト 1 件だけで、解除遷移を実経路で通すテストは存在しない。

spec の Scenario「押下中はハイライト色になり離すと平常時の背景に戻る」は WHEN が「押下し、その後離す」という往復であり、往復を実経路で通すことが要求されている。

**推奨修正**:

- `SectionBoxDecorationTests.test_押下背景も箱形状に収まる` と同型の足場 (`host(...)` 相当で window に載せ、`cellForItem(at:)` で実 Cell を取得) を使い、`cell.isHighlighted = true` → 収束待ち → 選択色、`cell.isHighlighted = false` → 収束待ち → 実効背景色、の往復を assert する形に作り替える。既存の押下側テストの隣 (`SectionBoxDecorationTests.swift`) に置けば `host` / `pump` の足場を新たに複製せずに済む (両ヘルパは各ファイル private で、`KsCellViewSupportTests` からは参照できない)。
- 待機は `kasane/handbook/cross/test-execution.md` の「収束を待つアサーション」3 条件で書く。既存 `pump(_:seconds:)` は固定秒数待機であり同規約が名指しで作り替え対象としているため、それに倣わず背景色の到達を条件として待つ形にする。
- 作り替えた後、回帰検出力を実測で確かめる (`kasane/lessons/code-review.md` L-001)。`s.isEnabled && isPressed` を `s.isEnabled` に置き換えるなど解除側だけを壊すミューテーションを一時的に入れ、押下側の assert は通り解除側の assert だけが落ちることを確認する。

### [🟡 Minor] `MainActor.assumeIsolated` の安全性根拠がコードに残っていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:78-93`

**問題点**: `MainActor.assumeIsolated` は前提が破れたときに warning ではなく trap する構造であり、「なぜここで assume してよいのか」は次に触る人が必ず知る必要がある情報である。その根拠 (`configurationUpdateHandler` は UIKit が main thread から呼ぶ) と、`isPressed` を closure の外で先に確定している理由 (SDK 側の handler 型が nonisolated で、非 Sendable な `UICellConfigurationState` を isolated 境界の内側へ持ち込めない) は、いずれも exploration.md / proposal.md にしか書かれていない。これらはアーカイブされる作業資料であり、`kasane/handbook/cross/comment-policy.md` はそもそもコメントからの参照を禁じている。結果として、この 2 つの設計判断はコードから辿れる場所に一切残らない。

**推奨修正**: `installSelectedColorHandler` の doc コメントか closure 直上に、自己完結する 1〜2 行を足す。change 文書や tasks 番号は参照せず、「UIKit はこの handler を main thread から呼ぶため main actor 分離とみなして扱う」「handler の型は nonisolated のため、押下判定だけを Bool に落として境界の内側へ渡す」という現在形の説明にする。特に後者を書いておかないと、`isPressed` の算出を `assumeIsolated` の内側へ移す「整理」が後から入りやすい。

### [🟡 Minor] tasks 2.3 の Scenario 参照が spec の Scenario 名と一致しない

**該当箇所**: `tasks.md:12` / `specs/ios-host/spec.md:31`

**問題点**: 実装 diff で tasks 2.3 の参照先が `Scenario: 押下中はハイライト色になり離すと平常時の背景に戻る` から `Scenario: 押下中はハイライト色になり離すと平常時の実効背景色へ戻る` へ書き換えられている。spec 側の Scenario 名は元のままであり、tasks が spec に存在しない Scenario 名を指す状態になっている。デルタスペックと tasks の対応表は verify と蒸留が辿る導線であり、ここが切れると Scenario 単位の追跡ができない。また、この書き換えは deviation.md にも記録されていない (記録されているのは tasks 3.2 の 1 件のみ)。

**推奨修正**: tasks 2.3 の Scenario 参照を spec の表記 (`押下中はハイライト色になり離すと平常時の背景に戻る`) に戻す。spec 側の文言のほうが不正確だと考える場合も、足場は凍結のため spec は書き換えず deviation.md に「spec の Scenario 名と実装対象の表現が異なる」旨を記録する。

### [🟡 Minor] 残存 warning の「スコープ外」判定が検証できない形で記録されている

**該当箇所**: `evidence/swift6-build.txt`

**問題点**: ログは残存 warning 32 件をファイル別件数だけで記録し、「いずれも本 change のスコープ外」と結論している。しかし 32 件のうち 19 件は、本 change が書き換えた `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` のものである。メッセージも行番号も記録されていないため、`MainActor.assumeIsolated` の導入で増えた分が含まれていないことをレビュー側で確認する手段がない。件数だけを見て「スコープ外」と断定するのは裏取りのない不在の主張であり、`SectionBoxAttributes.swift` が 57 行で 12 件という密度から「このコードベースでは Swift 6 モードの warning が元々多い」と推測はできるが、推測は検証の代わりにならない。

なお spec の Requirement はエラーゼロのみを契約としており、warning 残存そのものは仕様違反ではない。指摘は証跡の粒度に対するもの。

**推奨修正**: `file:line: warning: <message>` 形式の一覧 (重複を畳んだユニークメッセージでも可) をログに残す。増分ゼロを主張するなら、修正前コードに同じ一時設定を当てたビルドとの件数比較を併記する。

### [🟡 Minor] 押下ハイライトの視覚証跡を「撮る手段が無い」として除外している

**該当箇所**: `evidence/runtime-check.md`「この確認の範囲外」

**問題点**: 押下中のハイライト色は利用者の目に見える挙動であり、かつ本 change が書き換えた 2 経路の一方そのものである。除外理由として「押下を保持したままの静止画を撮る手段が無い」と書かれているが、この不可能性は検証された形跡がない。`kasane/lessons/process.md` L-003 は遷移の証跡として録画・連続フレームを明示的に認めており、Simulator では押下を保持したまま別シェルから `xcrun simctl io booted screenshot` を撮る手も残っている。手段の不在を根拠にするなら、試して駄目だった内容を書く必要がある (`kasane/lessons/impl.md` L-004 / `kasane/lessons/process.md` L-006 と同型)。

補足として評価しておく: 手順 1〜3 のタップはいずれも highlighted true → false を実経路で通しており、`MainActor.assumeIsolated` が trap しないことはこの証跡で実質的に担保されている。残る穴は「色そのものが正しく出ているか」の目視のみで、影響は限定的。

**推奨修正**: 押下中の画面を録画または押下保持中のキャプチャで 1 点残す。手段を試して不可だった場合は、その手段と失敗内容を `runtime-check.md` に書き換える。

### [🔵 Suggestion] MemoryLeakTests のテスト名が assert していない性質を謳っている / 検出力の実測がない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:34-64`

**問題点**: `test_Store経由でもControllerがdeinitされStore購読が解除される` は、Controller の解放と Store 側の状態遷移は assert するが、「Store 購読が解除される」ことは何も観測していない。deinit の明示 cancel を削除した本 change 以降、購読の自動解除は契約の中心に昇格しているため、名前と観測のずれが以前より目立つ。

また proposal は MemoryLeakTests を「回帰の安全網」と位置づけているが、その検出力は実測されていない。静的には `configureDataSource()` と layout 構築が `viewDidLoad` で走り、`[weak self]` を持つ closure 群 (`KsSettingsViewController.swift:506` / `560` / `669` / `981` / `986` ほか) はすべて `_ = controller.view` の時点で生成されるため、strong 捕捉が持ち込まれれば循環はテスト実行時に成立し weak nil の assert で捕まる、と読める。この読みは妥当だが、読みであって計測ではない。

**推奨修正**: 名前を実態に寄せるか、購読解除を観測できる assert (解放後に Store から Diff を流しても副作用が起きないこと等) を足す。併せて `[weak self]` のいずれか 1 箇所を strong に戻すミューテーションで両テストが落ちることを実測し、evidence に残す (`kasane/lessons/code-review.md` L-001)。deinit 削除という本 change 最大の判断が、後から検証可能な形で固定される。

### [🔵 Suggestion] `isEnabled == false` でハイライトを塗らない分岐に直接のテストがない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:85`

**問題点**: `s.isEnabled && isPressed` の `isEnabled` 側は本 change が触れた行であり、doc コメントで「`isEnabled == false` の Cell では selectedColor を反映しない」と契約として宣言されているが、これを検証するテストが見当たらない (2 軸で検索: テストターゲット内の `selectedColor` 参照 3 ファイルと、`isEnabled: false` / `isEnabled = false` の全出現。前者は Theme 構築と押下側 assert のみ、後者に背景色を見るものは無し)。spec の Scenario は enabled な Cell に限定されているため契約違反ではないが、Major-1 でテストを実経路に作り替えるなら 1 ケース足すコストは数行で済む。

**推奨修正**: 作り替えるテストに `isEnabled: false` の Cell で押下しても実効背景色のままであるケースを 1 つ加える。

## アクションプラン

1. **Major-1** — 押下解除テストを UIKit の実経路 (`host` + `isHighlighted` 切替 + 条件ベース待機) の往復検証に作り替え、ミューテーションで検出力を実測する。Suggestion-7 の `isEnabled == false` ケースはこの作業に同梱する
2. **Minor-2** — `MainActor.assumeIsolated` と `isPressed` 事前確定の根拠を、自己完結するコメントとしてコードに残す
3. **Minor-3** — tasks 2.3 の Scenario 参照を spec の表記に戻す (または deviation に記録する)
4. **Minor-4** — `evidence/swift6-build.txt` に warning のメッセージ一覧を追加し、`KsCellViewSupport.swift` の 19 件が本 change 由来でないことを確認できる形にする
5. **Minor-5** — 押下ハイライトの視覚証跡を録画または押下保持中のキャプチャで 1 点足す (不可なら試した手段を記録する)
6. **Suggestion-6** — MemoryLeakTests の名前と assert のずれを解消し、`[weak self]` ミューテーションでの検出力実測を evidence に残す

## 確認したが問題を認めなかった観点

- **仕様充足**: spec の 6 Scenario はいずれも実装・テストに対応がある (11 種解決 = `KsSettingsViewControllerTests.swift:17`、行タップ発火 = `CustomCellTests.swift:396`/`411` の `didSelectItemAt` 実経路、解放 2 件 = `MemoryLeakTests.swift`、Swift 6 ビルド = evidence)。押下往復のみ Major-1 の指摘対象
- **足場凍結**: `proposal.md` / `specs/ios-host/spec.md` は無変更。tasks.md はチェック更新以外に Minor-3 の 1 箇所のみ
- **deviation**: 記録済みの tasks 3.2 の乖離は妥当。`themeSubscription` (`KsSettingsViewController.swift:268`) の doc コメントには元から deinit への言及がなく、「3 件更新」で漏れはない
- **deinit 削除の安全性**: Store 購読 4 本はすべて `[weak self]`、`connectedStore` は weak、`UICollectionView` の `dataSource` / `delegate` は UIKit 側が weak 保持。削除された旧コメントの「`UICollectionView` は `dataSource` を strong 参照で保持する」という前提自体が誤りであり、削除は妥当。削除に伴う stale コメントは iOS ソース・samples・concepts のいずれにも残っていない (grep 済み)
- **`assumeIsolated` の適用範囲**: `configurationUpdateHandler` の呼び出し元は UIKit の configuration 更新経路のみで、off-main の呼び出し経路は見当たらない。deployment target との availability も Swift 6 ビルド成功で担保されている
- **`@MainActor` 付与の波及**: `TapNotifyingRenderer` は internal、唯一の呼び手 `didSelectItemAt` (`KsSettingsViewController.swift:2412`) は `UIViewController` 継承により main actor 分離済み。準拠 11 件と exploration の列挙は一致
- **コメント規約**: 追加・変更されたコメント (購読プロパティ doc 3 件、MemoryLeakTests の 1 行) はいずれも現在形・自己完結で、禁止参照・履歴記述・spec キーワードの混入なし
- **テスト報告**: `Executed 644 tests, with 0 failures` が Simulator 全件実行の結果として記録されており、`kasane/handbook/cross/test-execution.md` の件数併記の規律を満たす
- **証跡の内容**: `evidence/03-picker-selected.png` を実見し、runtime-check.md の記述 (「最後のイベント: テーマ → ダーク」と行の値表示更新) と一致することを確認。個体・個人を特定する値の混入なし (デモデータのみ)
