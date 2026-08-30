# セカンドオピニオン: add-maui-custom-cell (spec-001)
**相方**: codex / **日付**: 2026-08-12 / **対象**: 提案一式 (proposal / design / specs / tasks)
---
## 指摘

### Critical 1 — 「トークン変更時のみ再バインド」が現行 native 契約と矛盾する

- 該当箇所:
  - [specs/maui-bridge/spec.md](kasane/changes/add-maui-custom-cell/specs/maui-bridge/spec.md:15)「再バインドはトークンの変更でのみ発火する」
  - [design.md](kasane/changes/add-maui-custom-cell/design.md:20) Decision 1
  - [tasks.md](kasane/changes/add-maui-custom-cell/tasks.md:11) Task 2.2
- 問題点:
  - 現行 CustomCell 契約では `content` だけでなく `style` / `showArrow` / `isEnabled` / `isVisible` も等価性に参加し、変更時に再バインドされます（[custom-cell.md](kasane/concepts/core/cells/custom-cell.md:21)）。
  - iOS/Android の実装も同じです（[CustomCell.swift](ios/Sources/KsSettingsViewUI/CustomCell.swift:202)、[CustomCell.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCell.kt:89)）。
  - さらに MAUI のプロパティ変更は `replaceCell(s)` を通り、native は値等価を見ず明示的に reconfigure/full bind します（[SettingsRootStore.swift](ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:219)、[KsSettingsListAdapter.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:43)）。
  - したがって「同じトークンで他プロパティを変更しても native は再バインドしない」という Scenario は現行経路では成立しません。
- 推奨修正:
  - 「Cell の reconfigure」と「埋め込み platform view の差し替え」を明確に分離してください。
  - 意図が状態保持なら、契約を「同一トークンの scalar 更新では同一 platform view/Handler を維持し、detach・再 materialize・Dispose を行わない」と書き換え、そのための専用部分更新経路を設計してください。
  - materialize/detach/dispose 回数を計測する正負両方向のテストを追加してください。

### Critical 2 — 必須の UI アーティファクトと承認ゲートが欠落している

- 該当箇所:
  - `kasane/changes/add-maui-custom-cell/` 全体
  - [specs/maui-cells/spec.md](kasane/changes/add-maui-custom-cell/specs/maui-cells/spec.md:5)「配置と Content の表示」「ShowArrowIndicator」「無効の視覚状態」
  - [specs/maui-bridge/spec.md](kasane/changes/add-maui-custom-cell/specs/maui-bridge/spec.md:5) `full-bleed`
- 問題点:
  - 行レイアウト、Disclosure Indicator、無効表示、サンプル画面を変更する UI 変更ですが、必須の `ui/brief.md`、mock、`approved.png` がありません。
  - デルタスペックにも `full-bleed`、`trailing`、占有領域、視覚状態など UI lint の移動対象が残っています。
  - `tasks.md` の証跡先も規約上の `ui/verification/` ではなく独自の `screenshots/` です。
- 推奨修正:
  - `ui/brief.md`、承認用 mock、`mock/approved.png` を用意し、実装前の承認を得てください。
  - 視覚詳細は mock/brief へ移し、デルタスペックは観察可能な状態・操作結果、または既存 core CustomCell 契約への参照に限定してください。
  - Task に mock との両 OS 視覚照合と `ui/verification/` への証跡保存を追加してください。

### Major 1 — 高さ追従の設計が未確定で、Non-Goals とも両立していない

- 該当箇所:
  - [design.md](kasane/changes/add-maui-custom-cell/design.md:68) Decision 5 / Open Questions
  - [proposal.md](kasane/changes/add-maui-custom-cell/proposal.md:14) Non-Goals
  - [tasks.md](kasane/changes/add-maui-custom-cell/tasks.md:3) probe
- 問題点:
  - Spec は両 OS の動的高さ追従を無条件に SHALL としていますが、設計は実装中の probe 結果に依存しています。
  - iOS accessory の前例では対象限定 invalidation のため、Store/Controller に public API を追加しています（[KsSettingsViewController.swift](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:349)）。
  - CustomCell でも同じ処置が必要なら、「bridge 内部だけで完結」「Native Core/UI 公開 API は変更しない」という Non-Goal と衝突します。
- 推奨修正:
  - probe を提案承認前に実施し、方式を確定して design に反映してください。
  - native UI 公開口が必要なら Non-Goals、Impact、bridge spec、tasks を更新してください。
  - 未確定のまま進めるなら、少なくとも許容する代替実装と API 境界を事前に規定してください。

### Major 2 — Sample の既存パリティ契約に違反する

- 該当箇所:
  - [specs/samples-maui/spec.md](kasane/changes/add-maui-custom-cell/specs/samples-maui/spec.md:5)
  - [proposal.md](kasane/changes/add-maui-custom-cell/proposal.md:12)
  - [tasks.md](kasane/changes/add-maui-custom-cell/tasks.md:38)
- 問題点:
  - 既存規約は、MAUI 未追随の `CustomCellDemo` をこのフェーズで iOS/Android と同一構成へ収束させると明記しています（[sample-parity.md](kasane/concepts/cross/conventions/sample-parity.md:17)）。
  - 現行 iOS/Android は同一の5構成ですが、提案は内容の異なるMAUI独自6構成です。追随ではなく別画面になっています。
  - `ItemTemplate` がMAUI固有でも、ページ全体の主対象である CustomCell は全platform共通なので、ページ全体を例外扱いできる根拠がありません。
- 推奨修正:
  - 共通 CustomCell デモは既存5構成・文言・並びに一致させてください。
  - MAUI固有の `ItemTemplate`、Handler lifecycle、派生クラス検証は「MAUI固有」と明示した別画面または別Sectionに分離してください。
  - 意図的に規約を覆すなら、accepted な cross/ADR-0016 を supersede する判断が必要です。

### Major 3 — Command / Tapped の公開挙動が決まっておらず、アーティファクト間にも不整合がある

- 該当箇所:
  - [specs/maui-cells/spec.md](kasane/changes/add-maui-custom-cell/specs/maui-cells/spec.md:59)
  - [design.md](kasane/changes/add-maui-custom-cell/design.md:20) Decision 1 / Decision 4
  - [tasks.md](kasane/changes/add-maui-custom-cell/tasks.md:10)
- 問題点:
  - `ICommand.CanExecute`、`CanExecuteChanged`、`Tapped` と `Command` の発火順、両方設定時の挙動が未規定です。既存 `CommandCell` は実効有効状態と「Tapped → Command」の順序を公開契約にしています（[CommandCell.cs](maui/KsSettingsView.Maui/CommandCell.cs:8)）。
  - 表示後の `Tapped +=` / `-=` が native の購読有無へどう反映されるか未規定です。通常の event 追加・削除だけでは `PropertyChanged` が発生しません。
  - design Decision 1 の snapshot 項目にはタップ購読有無がありませんが、Decision 4 と Task 2.2 にはあります。
- 推奨修正:
  - 既存 CommandCell と同じ実効有効状態・通知順にするか、意図的な差を明記してください。
  - `Command`、`CommandParameter`、`CanExecuteChanged`、最初/最後の `Tapped` 購読変更を Scenario 化してください。
  - snapshot/DTO の正確な項目を design と tasks で統一し、購読の動的付け外しを再配信する仕組みを設計してください。

### Major 4 — Cell の構造的な除去時の lease 解放契約がない

- 該当箇所:
  - [design.md](kasane/changes/add-maui-custom-cell/design.md:30) Decision 2
  - [specs/maui-cells/spec.md](kasane/changes/add-maui-custom-cell/specs/maui-cells/spec.md:21) Content 差し替え / Handler 再接続
  - [tasks.md](kasane/changes/add-maui-custom-cell/tasks.md:13) lease 所有
- 問題点:
  - `Content` 差し替えと Handler 切断しか定義されておらず、CustomCell 自体の Remove/Replace、Section/Root の Reset、ItemsSource からの除去がありません。
  - cell ID 所有表からの解除、platform lease の退役、計測購読解除、多重配置表の解放が漏れると、リークまたは除去済み View の再利用失敗になります。
- 推奨修正:
  - 構造的な全解除経路を Requirement/Scenario と Task に追加してください。
  - 「native 除去配信後に lease を破棄」「同じ Cell/View の再追加は再 materialize される」「除去後は別 slot で再利用可能」をテストしてください。
  - leak/購読解除テストも明示してください。

### Major 5 — samples-maui の6項目が Scenario と検証タスクで閉じていない

- 該当箇所:
  - [specs/samples-maui/spec.md](kasane/changes/add-maui-custom-cell/specs/samples-maui/spec.md:5)
  - [tasks.md](kasane/changes/add-maui-custom-cell/tasks.md:38)
- 問題点:
  - 6項目を SHALL としていますが、個別 Scenario は live 更新と差し替えしかありません。
  - 派生クラス、Command/内部操作の非干渉、IsEnabled、IsVisible、高さ追従、ItemTemplate の独立性を受け入れ判定できません。
  - スクリーンショットだけでは Command の発火回数、二重発火、操作抑止、BindingContext の分離を証明できません。
- 推奨修正:
  - 6項目それぞれに操作と期待結果を持つ Scenario を追加してください。
  - 状態・イベントは自動テストまたはカウンタ付きE2E、外観と高さは計測＋視覚証跡で検証してください。

### Major 6 — 未承認 ADR を「確定済み」として基礎にしている

- 該当箇所:
  - [design.md](kasane/changes/add-maui-custom-cell/design.md:3) Context / ADR 候補
  - [decisions/maui/index.md](kasane/decisions/maui/index.md:23) ADR-0019〜0021
- 問題点:
  - design は「方針は確定済み」「ADR-0021 の確定内容」としていますが、ADR-0019〜0021 はすべて `proposed` です。
  - ADR 候補を「なし」としているため、提案承認後に誰が accepted へ昇格させるかも tasks にありません。
- 推奨修正:
  - 実装前にオーナー承認を得て ADR を accepted にするか、未承認の内容を design の Decision として扱い直してください。
  - ADR の承認ゲートを tasks/handoff に明記してください。

### Minor 1 — nullable Content と null 遷移時のトークン規則が不統一

- 該当箇所:
  - [proposal.md](kasane/changes/add-maui-custom-cell/proposal.md:9) `Content : View`
  - [design.md](kasane/changes/add-maui-custom-cell/design.md:22) `View?`
  - [specs/maui-cells/spec.md](kasane/changes/add-maui-custom-cell/specs/maui-cells/spec.md:5) 既定 null
- 問題点:
  - 公開型の nullability が一致していません。
  - `null → View`、`View → null`、null のまま再接続した場合にトークンをいつ更新するか未規定です。
- 推奨修正:
  - 公開型を `View?` に統一し、すべての参照遷移について token/view/lease の期待状態を表または Scenario で固定してください。

### Minor 2 — gateway への platform view 引き渡しタスクが明示されていない

- 該当箇所:
  - [tasks.md](kasane/changes/add-maui-custom-cell/tasks.md:8) facade / bridge タスク
- 問題点:
  - 現行 gateway の `ToDto` は snapshot と icon しか参照せず、View の引当口は accessory 専用です（[IKsSettingsGateway.cs](maui/KsSettingsView.Maui/Internals/IKsSettingsGateway.cs:115)）。
  - controller 所有の cell lease を両 platform gateway がDTOへ載せる seam の追加がタスク化されていません。
- 推奨修正:
  - cell content 用 store/seam の追加、gateway への attach/detach、fake gateway/materializer の更新を明示タスクにしてください。
  - 「既存 accessory の挙動に影響しない」という Impact を担保する回帰テストも追加してください。

## 総合判定

**CHANGES_REQUESTED**

中心契約であるトークンと再バインドの意味論が現行 native 更新経路と衝突しており、UI 承認ゲート・高さ再計測方式・Sample パリティも未解決です。この状態では実装開始後に仕様変更または Non-Goals の撤回が必要になる可能性が高いため、実装前の修正が必要です。

静的レビューのみ実施し、ビルド・テスト・ファイル変更は行っていません。


## 突き合わせ結果 (ホスト裁定: 2026-08-12)

| 指摘 | 採否 | 根拠 |
|---|---|---|
| Critical 1 (トークン再バインド契約の矛盾) | **採用** | replaceCells 経路は同値スキップなし (maui/ADR-0018 実証) で native 等価性には style 等も参加する — 「再バインドはトークン変更のみ」は事実と不一致。保証は「view インスタンスの安定性 (破棄・差し替えはトークン変更時のみ)」へ書き換える |
| Critical 2 (ui/ 欠落) | **降格** | ui/ 省略は先行 L 級2件 (basic-input-cells / accessory-views) の慣行踏襲としてオーナー合意済み。見た目の正は native 既存描画 + パリティ対象の native 実画面。spec 内の full-bleed 等は core 契約用語の参照で視覚生値ではない。証跡先 screenshots/ も先行慣行と同一 (→ 最終判断はオーナーに提示) |
| Major 1 (高さ追従と Non-Goals の衝突) | **採用** | accessory 前例 (invalidateAccessoryMeasurement) は native Store への追加を要した。probe 分岐で同型の追加を事前許容するよう Non-Goals / design を改訂 (ロードマップ非ゴールの「対称化例外」に該当) |
| Major 2 (sample-parity 違反) | **採用** | sample-parity.md が「CustomCellDemo は本フェーズで追随予定」と明記。パリティ画面 (native 5構成・文言一致) + MAUI 固有デモの分離 (AccessoryViewsDemoPage のオーナー裁定前例) へ再構成 (→ agenda 論点5 の決定を上書きするためオーナーに提示) |
| Major 3 (Command/Tapped 未規定) | **採用** | 既存 CommandCell の実効有効状態・Tapped→Command 順への整合を明記し、購読動的変更の再配信と snapshot 項目 (購読有無) の不整合を修正 |
| Major 4 (構造除去時の lease 解放欠落) | **採用** | Cell 除去 / Section Reset / ItemsSource 除去の解放経路が未規定だった。Requirement / Scenario / tasks を追加 |
| Major 5 (サンプル Scenario 不足) | **部分採用** | Major 2 の再構成と同時に項目対応の Scenario を拡充。状態・イベント検証は maui-cells 側テストが主担 |
| Major 6 (proposed ADR を確定扱い) | **部分採用** | design の「確定済み」表現を「合意済み (proposed、蒸留時に確定)」へ修正。昇格ゲートの tasks 追加は降格 — proposed → accepted は ksn-distill の既定パイプライン |
| Minor 1 (nullability 不統一) | **採用** | proposal を `View?` に統一し、null 遷移のトークン規則を design に明記 |
| Minor 2 (gateway seam タスク不足) | **採用** | cell content 用 seam・fake 更新・accessory 回帰テストを tasks に明示 |

未解決 (相方との矛盾): なし
