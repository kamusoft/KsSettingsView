# セカンドオピニオン: fix-cell-accessory-vertical-fill (002 回目)

**相方**: codex (session_id: <session-id> / label: so-review-fix-cell-accessory-vertical-fill)
**日付**: 2026-08-01
**モード**: code-review (ホスト側 review-001.md と並走)
**対象**: 未コミット working tree の実装 diff (ios/Sources/KsSettingsViewUI/ 11 ファイル + ios/Tests/KsSettingsViewUITests/ 2 ファイル + tasks.md)、直前コミット 3c24f3c
**注**: `second-opinion-001.md` は提案段階 (spec-review) の証跡。本ファイルは実装レビュー段階の証跡。

---

# レビュー結果: fix-cell-accessory-vertical-fill

**日付**: 2026-08-01  
**判定**: **CHANGES_REQUESTED**

## サマリー

アクセサリ列の実装、9 renderer の振り分け、再 render・reuse 処理はデルタスペックおよび ios/ADR-0001 と整合しており、明白な機能欠陥は確認できませんでした。ただし、ソースコメント規約への系統的な違反と、完了扱いになっている視覚照合を裏付ける証跡の欠落があるため、現状では承認できません。

指摘件数: **Critical 0 / Major 2 / Minor 2 / Suggestion 0**

## 指摘事項

### [🟠 Major] 変更アーティファクト参照など、禁止されたコメントが追加されている

**該当箇所**: [CellBaseLayout.swift:11](ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:11)、[KsListCellBase.swift:16](ios/Sources/KsSettingsViewUI/KsListCellBase.swift:16)、[CheckboxCellView.swift:7](ios/Sources/KsSettingsViewUI/CheckboxCellView.swift:7)、[BasicCellsTests.swift:12](ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift:12)、[UnifyCellCommonFieldsTests.swift:19](ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:19)

**問題点**: 新規・更新コメントに `kasane/changes/.../spec.md`、`openspec/changes/...`、`kasane/decisions/ios/0001`、`MUST NOT` が含まれています。これは `concepts/cross/conventions/comment-policy.md` が禁止する、短命な変更資料への参照、正式形でない ADR 参照、デルタスペック構文キーワードに該当します。同じパターンが Command／Switch／Checkbox／Radio／SimpleCheck／Picker の各変更コメントとテストへ系統的に入っています。

**推奨修正**: コメントを現在の挙動だけで自己完結する説明へ書き直してください。設計根拠が必要な箇所だけ正式形の `(ios/ADR-0001)` を使用し、`kasane/changes/...`・`openspec/...` の参照と `MUST` 系キーワードは除去してください。

### [🟠 Major] 視覚照合タスクの完了を確認できる証跡がない

**該当箇所**: [tasks.md:32](kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:32)、[ui/brief.md:24](kasane/changes/fix-cell-accessory-vertical-fill/ui/brief.md:24)

**問題点**: タスク 5.1・5.2 はスクリーンショット比較済みとしてチェックされていますが、対象アーティファクトには承認モックと修正前の参照画像しかなく、実装後の Simulator 画像や比較記録がありません。提示されたホスト結果も Sample のビルド成功までで、起動・視覚照合結果は含まれていません。本変更の主要な受け入れ条件である「垂直センター」「折り返し幅」「既存 Cell の非劣化」を独立レビューできず、チェック済みタスクを成果物から裏付けられません。

**推奨修正**: 実装後の Simulator スクリーンショットと、brief の規範範囲に対する比較結果を変更アーティファクトへ残してください。少なくとも Switch の長文 description、Picker、description なし、icon あり、EntryCell を確認対象にし、証跡を残さない場合は 5.1・5.2 を未完了へ戻してください。

### [🟡 Minor] Picker 系と EntryCell の2系統配置が renderer 単位でテストされていない

**該当箇所**: [InputCellsTests.swift:77](ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:77)、[InputCellsTests.swift:223](ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:223)

**問題点**: Picker の既存テストは `_lastCell` だけを検証し、コメントも旧 `accessory`／`content.secondaryText` 経路の説明のままです。Picker／NumberPicker／TimePicker／DatePicker の valueText と chevron の振り分け、および EntryCell の「行内で holder は空」という明示 Scenario は、共通関数のテストから推測できるだけで各 renderer の配線を固定できていません。

**推奨修正**: 各 Picker renderer について、value label が `contentStack`、chevron が `accessoryHolder` にあり、chevron が `contentStack` にないことを検証してください。EntryCell では入力 field が `contentStack` 内にあり、`accessoryHolder` が空かつ非表示であることを追加検証してください。

### [🟡 Minor] 幾何テストの一部がメッセージどおりの条件を検証していない

**該当箇所**: [UnifyCellCommonFieldsTests.swift:717](ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:717)、[UnifyCellCommonFieldsTests.swift:723](ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:723)、[UnifyCellCommonFieldsTests.swift:746](ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:746)

**問題点**: `holderFrame.width > 0` はアクセサリの自然幅維持を、nil 後に `stackV` が以前より広いことは trailing margin までの回復を保証しません。また垂直位置は holder の中心だけを検証しており、実際の accessory view の中心は直接確認していません。

**推奨修正**: holder 幅を `toggle.intrinsicContentSize.width` と許容差付きで比較し、toggle 自身の中心 Y を確認してください。nil 時は `stackV.frame.maxX` が `contentView.bounds.maxX - stackH.layoutMargins.right` と一致することを検証してください。

## アクションプラン

1. 禁止された変更資料参照と規範キーワードをコメントから除去する。
2. 実装後スクリーンショットと視覚比較結果を残し、タスク 5 の完了を裏付ける。
3. Picker 系・EntryCell の renderer 配線テストを追加する。
4. 幾何テストを実際の要求値まで強化する。

指定に従い、レビュー結果ファイルの作成およびビルド・テストの再実行は行っていません。提示された Simulator テスト 475 件成功などの実行結果は確認済み結果として評価に含めました。


---

## 突き合わせ結果

ホスト側 `review-001.md` (CHANGES_REQUESTED / Major 1 / Minor 1 / Suggestion 2) と本ファイル
(CHANGES_REQUESTED / Major 2 / Minor 2) を ksn-second-opinion Step 3 の採否規則で突き合わせた。

| # | 指摘 | ホスト | 相方 | 採否 | 根拠 |
|---|---|---|---|---|---|
| 1 | 新規追加コメントのソースコメント規約違反 | Major | Major | **確定 (Major)** | 双方一致。重要度も一致 |
| 2 | 触れたコメントブロックに残る `openspec/...` パス参照 | Minor | — | **確定 (Minor)** | ホスト側単独。相方と同格に扱う |
| 3 | 視覚照合タスクの完了を裏付ける証跡がない | — | Major | **降格** | 相方は静的レビュー制約 (ビルド・起動不可) のため検証できなかった。ホスト側は Simulator で独自に視覚照合を実施し mock 一致を確認、記録は `review-001.md`「視覚照合 (レビュアー独自)」節に残る。実害 (独立レビュー不能) は現実化していない。なお `ksn-core/references/ui-artifacts.md` は実装後スクリーンショットの置き場を規定していない — 証跡の残し方はハーネス側の課題として完了報告でオーナーへ提示する |
| 4 | Picker 系・EntryCell の renderer 単位配線テストがない | — | Minor | **採用 (Minor)** | 該当箇所特定済み (`InputCellsTests.swift:77` / `:223`)、実害 (renderer 変更時に Scenario 違反を検出できない) が具体的。ホスト側は Scenario 充足を確認済みのため、spec 未達ではなく回帰検出の強化として採用 |
| 5 | 幾何テストの検証条件が緩い | — | Minor | **部分採用 (Minor)** | 「nil 時に `stackV` が trailing margin まで広がる」は spec の文言 (`settings-view-ios-host` / Scenario: レイアウト後の幾何関係) を検証しきれていないため**採用**。一方「toggle 自身の中心 Y を見よ」「holder 幅を `intrinsicContentSize` と比較せよ」は spec が `accessoryHolder` 基準で規定しているため spec 以上の要求とみなし**降格** |
| 6 | valueText とアクセサリの間隔が 6pt → 16pt へ拡大 | Suggestion (修正不要) | — | **情報提供** | ホスト側が「spec は spacing を対象外と明記、brief も mock の規範範囲を配置関係に限定 → 規約違反ではない」と判定。オーナーの認識合わせとして完了報告に含める |
| 7 | `CellBaseLayout.swift:146` の valueLabel 按分コメントが実態とずれた | Suggestion | — | **採用 (Suggestion)** | 修正が安価で、Major と同じパスで直せる |

**件数**: 確定 2 / 採用 2 (うち 1 件は部分採用) / 降格 1 / 未解決 0

**矛盾した指摘**: なし (#4 はホスト「Scenario 充足」・相方「renderer 単位で未固定」で粒度が異なるだけで、両立する)

**修正サイクルへ回す指摘**: #1 #2 #4 #5 (部分) #7
**回さない指摘**: #3 (降格) / #6 (情報提供のみ)
