# Deviation: fix-ios-test-pump-condition-wait

実装フェーズで発生した、足場アーティファクト (デルタスペック / 分類台帳 triage.md) と実装の合意済み差分。

## 分類台帳との差分

- `KsBridgeUpdateTests.swift:319` の待機: triage.md では **C 分類 (負の検証)** → 実装判断により**待機自体を撤去**。理由: 当該箇所の直後の assert 対象は Root accessory (`attachment.controller.rootHeader` / `rootFooter`) のみで、`SettingsRootStore.diffSubject` は `PassthroughSubject`、`KsSettingsViewController.connectStore` の購読は `receive(on:)` を挟まない素の `sink` であるため、`applyDiff` が同一呼び出しスタック内でプロパティ更新まで到達する。この assert の対象に非同期の反映は挟まらない。tasks.md 2.5 が「待機の要否自体を判断し、不要なら撤去」と指示した箇所であり、撤去の根拠をコードのコメントにも残した。結果として Bridge の C 分類は 12 → 11 になる (2026-09-01)
- `SectionAccessoryVisibilityTests.swift:36` / `SectionBoxDecorationTests.swift:44,98` の待機: triage.md では **B 分類 (レイアウト駆動のみ)** → 実装判断により **A 分類 (条件ベース待機)**。理由: いずれも 20〜40 のテストが共有する host ヘルパ内の待機で、triage は直後の assert (layout attributes = 同期に確定) だけを見て B としていたが、同じヘルパの利用者には実物の supplementary / Cell を読むテストがある (例: `SectionAccessoryVisibilityTests` が host 直後に `visibleHeaderText` を assert する箇所)。`layoutNow` では初期スナップショットの反映を待たずに落ちるため、初期反映の完了述語を待つ形にした (2026-09-01)
- `SectionAccessoryRenderingTests.swift:559` の待機: triage.md では **A 分類 (収束待ち)** → 実装判断により **C 分類 (負の検証)**。理由: 当該箇所の assert は `beforeCell === afterCell` の不変性のみ (header 不変の full Diff で Cell 再構成が起きないことの確認) で、遷移証拠になる述語が存在しない。spec の「更新前から真である不変条件を述語にしない」に触れるため、負の検証として置換した (2026-09-01)

## 置換の粒度 (call site 被覆と待機呼び出し数の非対応)

triage.md の call site は全数を処理しているが、**置換後の待機呼び出しの数は call site 数と 1:1 にならない**。待つべき遷移の粒度に合わせた結果であり、分類の取りこぼしではない (2026-09-01):

- **統合 (2 call site → 1 呼び出し)**: `KsBridgeAccessoryViewTests.swift:265,266` / `KsBridgeCustomCellTests.swift:356,357` — いずれも「余裕を持って回す」ための 2 連の固定待機で、待っている遷移は 1 つ (回収の完了)。1 つの述語にまとめた
- **分割 (1 call site → 2 呼び出し)**: `KsBridgeAccessoryViewTests.swift:84` (Root header / footer)、`:208` (header view / 行タイトル)、`:313` (Root header view / 初期計測高さ) — 1 箇所で独立した 2 つの遷移を待っていたため、遷移ごとに述語を分けた
- `CustomCellTests.swift:81` (`host(_:)` ヘルパ内の待機、1 call site): triage.md では **A 分類 (収束待ち)** → 利用者 15 箇所のうち **8 箇所は A のまま (`awaitNonNil` で probe view の実体化を待つ)、7 箇所は B 相当 (`layoutNow`)** に分かれた。理由: `Text` / `Image` だけの hosted content は `UIView` を一切生やさない (`contentView.subviews` が常に空で、hosting content view はレイアウト前から差し替わっている) ため、待てる遷移証拠が存在しない。ヘルパに `renderedIdentifier:` を足して呼び出し側ごとに証拠を渡す形にし、証拠を渡せない呼び出しは待機なしのレイアウト実行にした。判断の根拠はヘルパの doc コメントにも残してある (2026-09-01)

## オーナー判断によるスコープの追加

- **Root accessory の追従テストの主張を実挙動に合わせて書き直した**: proposal の Impact は「テストの意味 (何を検証するか) は変えず、待ち方だけを変える」としていたが、置換の過程で **Root accessory の領域は、中身の内在サイズ無効化だけでも次の表示更新で UIKit が測り直す** (放置しても追従する) ことが判明した。従来のテストは 0.05 秒の固定待機を挟んで「追従しない」と主張していたため、待ち時間を負の検証の既定 0.2 秒へ揃えると成立しなくなる。オーナー判断により、別 change へ切り出さず本 change で**テストを実挙動に合わせて書き直した** (「その場では追従しない」+「放置すれば自力で追従する」+「対象限定の無効化なら即座に追従する」の 4 段構成。自力追従の待機は条件ベース待機で書き、固定時間待機は増やしていない)。Section accessory は layout が領域高さの解を保持するため従来どおり無効化が必要で、その対比もテストから読み取れる形にした (2026-09-01)

- **プロダクトコードの doc コメントを 1 箇所修正した**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `invalidateAccessoryMeasurement(target:)` の説明が「内在サイズを無効化しただけでは領域の高さを測り直さない」と一律に書いていたが、上記の調査により Root accessory については不正確と判明した。proposal の Non-Goal は「プロダクトコード (`ios/Sources/`) の変更」だが、handbook `cross/test-execution.md` を実挙動に合わせて更新した結果、コード側のコメントだけが古い説明として残る状態を本 change が作ったため、コメント 2 段落の修正で閉じるものとして同梱した (実行コードは変更していない) (2026-09-01)
