# レビュー結果: fix-cell-accessory-vertical-fill (001 回目)

**日付**: 2026-08-01
**判定**: CHANGES_REQUESTED

## サマリー

実装そのものは spec に忠実で品質が高い。`accessoryHolder` の導入・2 系統振り分け・再 render の置換規則・prepareForReuse の後始末はデルタスペックの全 Scenario に対応するテストを備え、iOS Simulator 上の実機検証でも承認済み mock どおりの見た目 (アクセサリ垂直センター / description がアクセサリ列の左で折り返す) を確認できた。テストは 332 件全 pass、無断の仕様逸脱・足場アーティファクトの書き換えも無い。

一方で、本 change が新規に追加したソースコメント 14 箇所が、直前のコミットで確定したばかりの規約 [ソースコメント規約](../../concepts/cross/conventions/comment-policy.md) の禁止参照に該当する。挙動には影響しないが、蒸留 (ksn-distill) で `kasane/changes/fix-cell-accessory-vertical-fill/` がアーカイブされた瞬間に全て死んだ参照になるため、アーカイブ前の修正を求める。

## 指摘事項

### [🟠 Major] 新規追加コメントがソースコメント規約の禁止参照に該当する (11 ファイル 14 箇所)

**該当箇所**:

変更提案文書へのパス参照 (規約「アーカイブ文書のパス / 行番号」に該当):
- `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:29`, `:31`
- `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:16`
- `ios/Sources/KsSettingsViewUI/CheckboxCellView.swift:11`
- `ios/Sources/KsSettingsViewUI/CommandCellView.swift:11`
- `ios/Sources/KsSettingsViewUI/RadioCellView.swift:11`
- `ios/Sources/KsSettingsViewUI/SimpleCheckCellView.swift:11`
- `ios/Sources/KsSettingsViewUI/SwitchCellView.swift:11`
- `ios/Sources/KsSettingsViewUI/PickerCellView.swift:12`
- `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift:12`
- `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:19`, `:522`

デルタスペックの裸参照 (規約「拡張子なしの裸参照」に該当):
- `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:523`, `:546`, `:589`, `:615`, `:634`, `:686` — `/// Scenario: <名前>` だけの参照

デルタスペック構文キーワードの混入 (規約「禁止する記述類型」に該当):
- `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:610` — `// 旧経路の MUST NOT を維持`

ADR 参照の表記形 (規約の許容形は `<domain>/ADR-NNNN`):
- `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:11` — `（kasane/decisions/ios/0001 準拠）`
- `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:18` — `kasane/decisions/ios/0001-accessory-column-outside-content-stack.md`

**問題点**:

規約 (`concepts/cross/conventions/comment-policy.md`, timestamp 2026-08-01) は「`kasane/changes/` の文書はアーカイブされる作業資料であり、指す先の意味を後から追えない」としてパス参照を禁止し、`MUST` / `MUST NOT` 等のデルタスペック構文キーワードのコメント混入も禁止している。適用範囲にはテストコードも含まれる。

本 change は蒸留で `kasane/changes/fix-cell-accessory-vertical-fill/` がアーカイブへ移動する前提であり、追加された 11 箇所のパス参照はアーカイブ直後に全て解決不能になる。規約はこの change の直前コミット (`4b854f9`) で確定したばかりであり、その次の change で 14 箇所を新規に持ち込むと規約が空文化する。

なお、コメントで説明されている内容 (2 系統の役割分担・chevron が Cell 級である理由・各テストが何を検証するか) 自体は有用で、参照句を落としても本文だけで自己完結して読める。規約の「書き換え時の判断基準」の類型 1 (定型句型: 参照句を削除して整形) と類型 2 (理由一体型: ADR 参照へ置換) にそのまま当てはまる。

**推奨修正**:

1. `仕様（拡張）: kasane/changes/.../spec.md` の行を削除し、直後の説明文 (「`KsCheckBoxView` は Cell 級アクセサリとして `accessoryView` へ渡す。」等) だけを残す。設計判断の根拠を残したい箇所は `(ios/ADR-0001)` を添える
2. `kasane/decisions/ios/0001-...md` / `kasane/decisions/ios/0001` のパス表記を、規約の許容形 `ios/ADR-0001` に揃える
3. テストの `/// Scenario: <名前>` は、そのテストが何を検証するかの自己完結した日本語説明に書き直す (例: `/// accessoryView に渡した view が accessoryHolder に 1 個だけ配置され、contentStack には入らないことを検証する`)
4. `// 旧経路の MUST NOT を維持` を `// contentConfiguration / accessories 経路を使わない状態が保たれることを確認する` 等、規範性を自然な日本語で表す形に書き直す

### [🟡 Minor] 触れたコメントブロックに残った openspec パス参照

**該当箇所**: `ios/Sources/KsSettingsViewUI/CheckboxCellView.swift:9-10`、`CommandCellView.swift:9-10`、`RadioCellView.swift:9-10`、`SimpleCheckCellView.swift:9-10`、`SwitchCellView.swift:9-10`、`PickerCellView.swift:9-11`、`CellBaseLayout.swift:22-27`、`KsListCellBase.swift:12-15`、`BasicCellsTests.swift:6-11`、`UnifyCellCommonFieldsTests.swift:16-18`

**問題点**: 本 change はこれらのコメントブロックを実際に書き換えている (例: `CheckboxCellView.swift:10` は「`trailingViews` に追加する」→「使わない」へ変更済み) が、同じブロックの `openspec/changes/.../spec.md` パス参照はそのまま残した。ソースコメント規約の適用契機は「既存コメントに触れる実装をするとき」であり、触れたブロックは規約準拠へ寄せる対象になる。既存違反であって本 change が作った瑕疵ではないため Minor とする。

**推奨修正**: Major の修正と同時に、触れたブロック内の `openspec/changes/...` 行も落として説明文だけを残す。触れていないファイル・ブロックまで広げる必要はない。

### [🔵 Suggestion] valueText とアクセサリの間隔が 6pt から 16pt へ広がる

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:116` (`stackH.spacing = 16`)

**問題点**: chevron が `contentStack` (spacing 6) から `accessoryHolder` (stackH spacing 16) へ移ったため、CommandCell / Picker 系 4 種で valueText と chevron の間隔が広がる。Simulator 実機と `ui/references/current-kssettingsview.png` を比較すると「通知設定 オン ›」の余白が約 6pt → 約 16pt に変化している。

デルタスペックは「spacing・margin 等の視覚パラメータは本 spec の対象外」と明記し、`ui/brief.md` も mock の規範範囲を配置関係のみに限定しているため**規約違反ではない**。新しいマジックナンバーの導入もなく、既存の stackH spacing をそのまま使っている。ADR-0001 の構造変更に伴う必然の副作用としてオーナーが認識していればよい、という情報提供に留める。

**推奨修正**: 修正不要。オーナーがこの余白を現状維持したいと考える場合のみ、`accessoryHolder` 側で負の間隔調整ではなく `stackH.spacing` とは独立した間隔指定を検討する (その場合は視覚パラメータの変更として別 change 相当)。

### [🔵 Suggestion] valueLabel の按分に関するコメントが実態とずれた

**該当箇所**: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:146`

**問題点**: 「value label は残り領域を吸って広がる（trailingViews が無ければ右端まで、あれば trailingViews との間で按分）」というコメントが残っているが、Picker 系 4 種と CommandCell は chevron が trailingViews ではなくなったため、実際の右端は「`accessoryHolder` の手前まで」になる。誤りではないが、このファイルだけを読む人には現在の 2 系統構造が伝わりにくい。

**推奨修正**: 「trailingViews が無ければ contentStack の右端 (= アクセサリ列の手前) まで広がる」と書き換える。Major の修正と同じパスで直せる。

## アクションプラン

1. Major: 11 ファイル 14 箇所の新規コメントから禁止参照を除去し、ADR 参照を `ios/ADR-0001` 形へ揃える (機械的な作業。挙動変更なし)
2. Minor: 上記で触れたコメントブロックに残る `openspec/changes/...` 行も同時に整理する
3. Suggestion: `CellBaseLayout.swift:146` のコメントを現構造に合わせて更新する
4. 修正後、`xcodebuild test` の再実行のみで足りる (コメント以外の変更が入らない限り視覚照合の再実施は不要)
5. valueText とアクセサリの間隔変化 (Suggestion) はオーナーへの情報共有まで。修正は求めない

## 確認した観点 (指摘に至らなかったもの)

**ビルド・テスト**
- `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,...iPhone 17 Pro'` → **332 tests / 0 failures**。macOS ホストの `swift test` では UIKit テストが除外されるため Simulator で実行した

**仕様充足**
- `settings-view-ios-host` の 6 Scenario (初期化直後の階層 / accessoryHolder 配置 / nil 時非表示 / 再 render 非蓄積 / 幾何関係 / 行内 trailing / prepareForReuse) すべてに対応テストが存在し、いずれも実効的な assert を持つ (`UnifyCellCommonFieldsTests.swift:519-731`)
- `cell-types-basic` の 4 Scenario も充足。Android Scenario は `android/ks-settingsview-ui/.../CellBaseLayout.kt:194` (`descriptionView.END = accessoryHolder.START`) と `:210-212` (holder を親の TOP/BOTTOM に接続 = 縦センター) で既存実装が満たしており、変更不要という spec の主張は正しい
- `applyCellBaseLayout` の呼び出し元 12 種すべてを確認。Cell 級アクセサリを持つ 9 種が `accessoryView` へ移り、ButtonCell / LabelCell / EntryCell は `trailingViews` のまま (= holder 空 → 非表示) で spec どおり
- tasks.md のチェックに虚偽なし。足場アーティファクト (`specs/` `proposal.md` `ui/`) の書き換えなし。deviation.md は不要 (無断の逸脱を検出しなかった)

**視覚照合 (レビュアー独自)**
- Sample アプリを iPhone 17 Pro Simulator で起動し、「基本 Cell 7 種デモ」「入力 Cell 5 種デモ」「共通フィールド統合デモ」を確認
- SwitchCell "Notification": description が Switch の左で折り返し、Switch はセル全体に対して垂直センター → `ui/mock/approved.png` と一致、`ui/references/current-kssettingsview.png` の不具合 (description が Switch の下へ回り込む) は解消
- EntryCell は入力欄が行内で右端いっぱい (アクセサリ領域を確保しない)、LabelCell の valueText は右マージンまで到達、Picker 系は valueText 行内 + chevron 垂直センター — いずれも mock どおりで劣化なし
- icon あり / description なし / hintText あり (「推奨」「省データ」) の各セルにも崩れなし

**堅牢性・設計品質**
- `setAccessoryView` の除去ループは `arrangedSubviews` のスナップショット配列を走査するため、反復中の変更で不整合を起こさない。同一インスタンス再指定時に付け替えない分岐も妥当 (`UISwitch` のアニメーション中断回避)
- `prepareForReuse` での無条件除去は、現状 `accessoryHolder` に first responder になり得る view が入らない (EntryCell の TextField は `contentStack` のまま) ため、既存の first responder 保護と衝突しない
- `hintLabel` は `self` 直下の float のままで、accessoryHolder 追加による制約競合はない。アクセサリが垂直センターへ移った分、右上 hint との重なりはむしろ減る
- VoiceOver の読み上げ順が title → accessory → description から title → description → accessory へ変わるが、iOS 標準の設定画面と同じ順序であり劣化ではない
