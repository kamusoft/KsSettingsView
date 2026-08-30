# セカンドオピニオン: add-maui-samples-foundation (001 回目)
**相方**: codex / **日付**: 2026-08-09 / **対象**: 提案一式 (proposal.md / specs/samples-maui / specs/maui-bridge / tasks.md、M 級・spec-review モード)
---
総合判定: **CHANGES_REQUESTED**

Critical 0件、Major 7件、Minor 3件です。静的レビューのみで、ビルド・テスト・書き込みは行っていません。

### Major — M級の範囲を超えている

**該当箇所**: [proposal.md:18](kasane/changes/add-maui-samples-foundation/proposal.md:18)、[proposal.md:33](kasane/changes/add-maui-samples-foundation/proposal.md:33)

**問題点**: `samples-maui` と `maui-bridge` の2 capabilityを横断し、利用者側の推移依存まで変える外部パッケージ判断を含みます。Kasaneの級判定では複数能力横断・外部依存判断はL級相当です。さらに、主要判断を担う maui/ADR-0010 はまだ `proposed` です。

**推奨修正**: サンプル土台とAndroidX解決を別changeへ分割するのが最も明確です。分割しない場合はL級へ変更し、primary/fallback、依存グラフ、NuGet利用時の保証範囲を扱う `design.md` を追加し、ADR-0010を実装前にacceptedへ昇格してください。

### Major — UI変更なのに必須のUIアーティファクトがない

**該当箇所**: [proposal.md:9](kasane/changes/add-maui-samples-foundation/proposal.md:9)、`kasane/changes/add-maui-samples-foundation/ui/`（欠落）、[tasks.md:24](kasane/changes/add-maui-samples-foundation/tasks.md:24)

**問題点**: 新しい一覧画面、ナビゲーション、検証ページを作るUI変更ですが、Kasaneで必須のbrief・承認モックがありません。4.xの目視確認も「内容があること」だけで、期待する画面構造との照合基準がありません。

**推奨修正**: `ui/brief.md` と承認済みmockを用意し、tasksへ両OSでのmockとの視覚照合を追加してください。

### Major — AndroidXのfallbackがRequirementを満たさない

**該当箇所**: [maui-bridge/spec.md:5](kasane/changes/add-maui-samples-foundation/specs/maui-bridge/spec.md:5)、[proposal.md:14](kasane/changes/add-maui-samples-foundation/proposal.md:14)、[tasks.md:6](kasane/changes/add-maui-samples-foundation/tasks.md:6)、[ADR-0010:14](kasane/decisions/maui/0010-androidx-conflict-absorbed-in-binding-layer.md:14)

**問題点**:

- `NoWarn` はNU1608を見えなくするだけで、要求された「Lifecycle familyの版整合」を保証しません。
- Scenarioは警告コードだけを見るため、`NoWarn`でも成功扱いになります。
- MauiHostの既存ピンはno-opと判明済みなので、「ピンありと同じ依存解決結果」はbinding変更なしでも成立し、回帰検出力がありません。
- tasksはNU1608とNU1107を同じfallbackで扱いますが、agenda/ADRではNU1107だけアプリ側直接参照としており、Requirementの「利用側ピンなし」と衝突します。
- NuGet利用者にも効果が伝播するとの主張は、NuGetパッケージングをNon-Goalとし、ProjectReferenceしか検証しない現在のScenarioでは証明できません。

**推奨修正**: 実装前spikeでprimary案の成否を確定するか、primary/fallbackを別の受け入れ結果として明文化してください。primaryでは、警告だけでなく `project.assets.json` 上の正確な解決バージョン、`NoWarn`不使用、clean restore/buildを検証します。NuGet保証はProjectReference保証へ狭めるか、一時packしたパッケージを参照するconsumer検証を追加してください。

### Major — ReactivePropertyの選定が未決のまま実装へ送られている

**該当箇所**: [proposal.md:12](kasane/changes/add-maui-samples-foundation/proposal.md:12)、[agenda.md:21](kasane/roadmaps/maui-support/phases/phase-3-samples-foundation/agenda.md:21)、[tasks.md:12](kasane/changes/add-maui-samples-foundation/tasks.md:12)

**問題点**: agendaはパッケージ詳細をproposalで確定するとしていますが、tasksでは `ReactiveProperty` と `ReactiveProperty.Core` の選択を実装者へ委ねています。バージョンも未定です。また、仕様上は静的なLabelCell表示しか要求されず、ReactivePropertyを導入・利用したことを判定できる状態遷移がありません。

**推奨修正**: package ID・version・利用範囲を提案段階で確定し、値変更がUIへ反映されるScenarioを追加してください。静的画面しか作らないなら、ReactivePropertyは対話的Cellを実装する後続フェーズへ延期する方が検証可能です。

### Major — 土台の中心目的「ページ追加＋一覧1行」が仕様化されていない

**該当箇所**: [proposal.md:5](kasane/changes/add-maui-samples-foundation/proposal.md:5)、[proposal.md:10](kasane/changes/add-maui-samples-foundation/proposal.md:10)、[samples-maui/spec.md:18](kasane/changes/add-maui-samples-foundation/specs/samples-maui/spec.md:18)、[tasks.md:13](kasane/changes/add-maui-samples-foundation/tasks.md:13)

**問題点**: Specが保証するのはタイトル文字列の一元化だけです。カテゴリー、遷移先生成、メニュー登録が別々のswitch/listに分散してもScenarioを満たせます。「後続フェーズがページ追加＋一覧1行だけで拡張できる」という変更の主要目的を検証できません。

**推奨修正**: 画面descriptorが少なくとも分類・タイトル・遷移先を一元的に持つことを設計上の受け入れ基準にし、仮の2画面目を登録したとき一覧とタイトルが同時に増えるテストなどで拡張点を検証してください。

### Major — phase-4への申し送りが実際には記録されていない

**該当箇所**: [proposal.md:23](kasane/changes/add-maui-samples-foundation/proposal.md:23)、[samples-maui/spec.md:32](kasane/changes/add-maui-samples-foundation/specs/samples-maui/spec.md:32)、[phase-4 agenda.md:7](kasane/roadmaps/maui-support/phases/phase-4-basic-input-cells/agenda.md:7)、[sample-parity.md:33](kasane/concepts/cross/conventions/sample-parity.md:33)

**問題点**: proposalはStore/DSLデモの判断をphase-4 agendaへ申し送ったとしていますが、実際のphase-4 agendaにその論点はありません。LabelCell検証ページの削除も明示的に追跡されていません。暫定差異の追随を追跡可能にするsample-parity規約と整合しません。

**推奨修正**: phase-4 agendaへ、少なくとも「LabelCell検証ページの削除」「基本Cellデモへの置換」「Store/DSLデモのMAUI対応判断」を明示的に登録してください。未来の削除を現changeのSHALLに残すなら、後続changeとの対応を追跡可能にする必要があります。

### Major — READMEタスクがプロジェクトの更新手順と衝突する

**該当箇所**: [tasks.md:20](kasane/changes/add-maui-samples-foundation/tasks.md:20)、[AGENTS.md:15](AGENTS.md:15)

**問題点**: README群はconcepts更新後、ユーザーの明示依頼を受けた `docs-refresh` 経由でのみ更新できます。現在のtask 3.1は通常の実装タスクとして直接置換する計画であり、実装ワーカーが規約を守ると完了できません。

**推奨修正**: README更新を実装waveから外し、実装・蒸留によるconcepts更新後の明示的な `docs-refresh` ゲートとして扱ってください。READMEを本changeの受け入れ条件に残すなら、この依存関係をtasksに明記する必要があります。

### Minor — ビルド／起動Scenarioの環境前提が不足している

**該当箇所**: [samples-maui/spec.md:8](kasane/changes/add-maui-samples-foundation/specs/samples-maui/spec.md:8)

**問題点**: .NET SDKとworkloadだけでは、Xcode、Android SDK/JDK、対応ホストOS、シミュレータ等の条件が決まりません。既存MAUIレビューではiOSビルドに `DEVELOPER_DIR` 指定を使用しています。「追加手順なし」も判定者によって意味が変わります。

**推奨修正**: 対応ホスト、必要toolchain、検証するSDK/Xcode、正確なbuild/runコマンド、対象deviceを明記してください。

### Minor — LabelCell表示の受け入れ基準が曖昧

**該当箇所**: [samples-maui/spec.md:31](kasane/changes/add-maui-samples-foundation/specs/samples-maui/spec.md:31)

**問題点**: `Title / ValueText 等` の「等」が未定義で、3行がどの公開フィールドを検証すべきか判定できません。任意の3行を表示するだけでも合格できます。

**推奨修正**: 各行で設定するフィールドと期待文言を列挙してください。少なくともTitle、ValueText、Description、HintTextのどれを疎通対象にするかを固定すると目視判定できます。

### Minor — README Scenarioに検証タスクがない

**該当箇所**: [samples-maui/spec.md:39](kasane/changes/add-maui-samples-foundation/specs/samples-maui/spec.md:39)、[tasks.md:18](kasane/changes/add-maui-samples-foundation/tasks.md:18)

**問題点**: READMEを書くタスクはありますが、掲載したコマンドだけで両OSの起動に到達できることを確認するタスクがありません。またScenarioの「IDEまたはCLI」は、Requirementの「CLIコマンドを含む両OS手順」より弱い条件です。

**推奨修正**: README記載のコマンドをそのまま実行する検証タスクを追加し、ScenarioをRequirementと同じ強さへ揃えてください。


## 突き合わせ結果 (2026-08-09)

ホスト側自己レビュー (2周・指摘なし) との突き合わせ。採否は ksn-second-opinion の規則による。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| Major 1 | M級の範囲超過 (2 capability 横断・ADR proposed のまま) | **降格** (オーナー最終確認は提示) | 級は L 提示の上でオーナーが M を確定済み。AndroidX は決定済み事項の csproj 1行で未決の設計判断なし。ADR proposed のまま実装→蒸留時 accepted は Kasane の通常パイプライン (ADR-0007/0009/0019 前例) |
| Major 2 | ui/ (brief・mock) 欠落 | **降格** (オーナー承認済み判断) | 前例 add-maui-core (L級・UI描画変更) も ui/ なし。見た目の正は native ライブラリ側で、サンプルは新規視覚デザインを持たない。ui/ なしはオーナー確認済み |
| Major 3 | fallback が Requirement を満たさない / Scenario の検証力不足 / NU1107 の扱い衝突 | **採用 (部分)** | Scenario を強化 (NoWarn 不使用前提 + project.assets.json の解決バージョン確認)、MauiHost Scenario の THEN を具体化、NU1107 と fallback 発動時の deviation 意味論を spec 注記に明文化。NuGet 伝播は検証対象外と明記 |
| Major 4 | ReactiveProperty 選定が未決のまま実装送り | **採用** | agenda「propose で確定」との矛盾は事実。パッケージ選定をオーナーに提示して確定する。値変更反映の Scenario を追加済み |
| Major 5 | 「ページ追加+一覧1行」の拡張性が未仕様化 | **部分採用** | tasks 2.3 に descriptor 一元定義の範囲 (区分・文言・遷移先) と拡張形を明記。spec には書かない — 内部構造は挙動契約の対象外 (Kasane の spec 規約)。「仮の2画面目テスト」は不採用 (サンプルに過剰) |
| Major 6 | phase-4 申し送りが実際には未記録 | **採用** | 事実 (書き忘れ)。phase-4 agenda に「LabelCell 検証ページの削除と置換」「Store/DSL 方式デモの MAUI 対応要否」を登録済み |
| Major 7 | README タスクが docs-refresh 規約と衝突 | **未解決 → オーナー裁定** | AGENTS.md「README 群は docs-refresh 経由のみ」の適用範囲 (サンプル新設 README を含むか) はオーナーの規約解釈事項 |
| Minor 1 | ビルド Scenario の環境前提不足 | **降格** | 環境詳細 (Xcode 版等) は README の責務で、spec に書くと腐る (腐り度原則)。README Scenario の強化で間接対応 |
| Minor 2 | LabelCell「等」の曖昧さ | **採用** | フィールド割当 (Title/ValueText 全行 + Description/HintText 各1行以上) を spec と tasks に明記済み |
| Minor 3 | README コマンド実証タスクなし / Scenario が弱い | **採用** | task 4.4 (README コマンドの実実行確認) を追加、Scenario を CLI 実行到達の強さに揃えた |

採用 4 (うち部分2) / 降格 3 / 未解決 1 (オーナー裁定待ち: Major 7)。相方総合判定 CHANGES_REQUESTED のうち、修正サイクルを回した項目は上記「採用」欄のとおり反映済み。

## オーナー裁定 (2026-08-09 追記)

- **Major 1 (級)**: M 維持を裁定 (分割・L 化とも不採用)。境界観測を lessons/inbox/change-grade-underestimated-for-cross-capability-contract.md の経緯に追記
- **Major 4 (ReactiveProperty)**: `ReactiveProperty.Core` に確定 (proposal / tasks 反映済み)
- **Major 7 (README 規約)**: 却下 — サンプル付属 README は docs-refresh 規約の対象外で実装タスクのまま (的外れパターンとして lessons/inbox/readme-convention-scope-misapplied-to-sample-artifacts.md に捕捉)

最終集計: 採用 4 (うち部分2) / 降格 3 / 却下 1 / 未解決 0
