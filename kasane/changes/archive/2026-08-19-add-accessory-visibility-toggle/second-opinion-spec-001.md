# セカンドオピニオン: add-accessory-visibility-toggle (spec-001)
**相方**: codex / **日付**: 2026-08-19 / **対象**: 提案一式 (proposal / specs 7 capability / tasks)
---
# レビュー結果: add-accessory-visibility-toggle

**日付**: 2026-08-19  
**判定**: **NEEDS_DISCUSSION**

## サマリー

方向性は妥当ですが、現状の仕様・タスクでは宣言 DSL 経路と iOS の Section 再構築経路が抜けており、実装してもトグルが反映されない、または別操作で `true` に戻る可能性があります。加えて、後方互換性の記述、Header 不在時の高さ契約、変更級・ADR 状態に解消すべき矛盾があります。

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 指摘事項

### [🟠 Major] 複数 capability 横断なのに M 級とされ、必須の設計・UI アーティファクトがない

**該当箇所**: [proposal.md:29](kasane/changes/add-accessory-visibility-toggle/proposal.md:29)、[proposal.md:31](kasane/changes/add-accessory-visibility-toggle/proposal.md:31)、[tasks.md:30](kasane/changes/add-accessory-visibility-toggle/tasks.md:30)

**問題点**: 提案自身が7 capability、Native 2系統、Bridge、MAUI、3 Sample を横断すると認めています。Kasane 規約では「1能力内」が M、「複数能力横断」が L です。また Sample 画面の構成を変更するにもかかわらず `ui/` がありません。規約上、UI に触れる変更は級にかかわらず原則 `ui/` が必要です。

**推奨修正**: L 級へ変更し、横断経路・更新経路・互換性変更を整理した `design.md` を追加してください。Sample について `ui/brief.md`、承認済み mock と `approved.png` を用意し、表示／非表示状態を見た目の受け入れ基準にしてください。

### [🟠 Major] ADR が proposed のまま「確定済み」と扱われている

**該当箇所**: [proposal.md:5](kasane/changes/add-accessory-visibility-toggle/proposal.md:5)、[proposal.md:31](kasane/changes/add-accessory-visibility-toggle/proposal.md:31)、[ADR-0023:4](kasane/decisions/core/0023-accessory-visibility-and-composition.md:4)

**問題点**: Kasane の ADR 規約では `proposed` は AI ドラフトで、人間の確認後に `accepted` へ昇格します。ところが proposal は「設計判断は確定済み」としており、状態と権威が一致しません。特に今回の ADR は既存公開挙動を変更するため、未承認のまま実装の正にするのは危険です。

**推奨修正**: オーナー確認済みなら ADR を `accepted` にしてください。未確認なら proposal の「確定済み」を外し、空 text と高さ挙動の互換性変更を明示的な承認事項にしてください。

### [🟠 Major] 「既定 true で現行挙動と一致」が同じ仕様内の挙動変更と矛盾する

**該当箇所**: [proposal.md:24](kasane/changes/add-accessory-visibility-toggle/proposal.md:24)、[proposal.md:25](kasane/changes/add-accessory-visibility-toggle/proposal.md:25)、[Android spec:19](kasane/changes/add-accessory-visibility-toggle/specs/settings-view-android-ui/spec.md:19)、[iOS spec:19](kasane/changes/add-accessory-visibility-toggle/specs/settings-view-ios-ui/spec.md:19)、[ADR-0023:17](kasane/decisions/core/0023-accessory-visibility-and-composition.md:17)

**問題点**: 既定値が `true` でも、Android の空 text と iOS の Header 不在＋高さ指定は現行と異なる結果になります。したがって「既存コードの挙動不変」「現行挙動と一致」という受け入れ基準は、同じ spec の対称化要件と同時に満たせません。

**推奨修正**: 互換性の主張を「非空の既存 accessory ではトグル未指定時の挙動を維持する」に限定してください。空 text と Header 不在＋高さ指定は、明示的な breaking behavior matrix として切り分けてください。

### [🟠 Major] SwiftUI／Compose の宣言 DSL と差分検出経路が仕様・タスクから欠落している

**該当箇所**: [proposal.md:9](kasane/changes/add-accessory-visibility-toggle/proposal.md:9)、[proposal.md:10](kasane/changes/add-accessory-visibility-toggle/proposal.md:10)、[tasks.md:3](kasane/changes/add-accessory-visibility-toggle/tasks.md:3)、[tasks.md:11](kasane/changes/add-accessory-visibility-toggle/tasks.md:11)

**問題点**: Sample は両 Native とも宣言 DSL を使っていますが、公開 builder、resolved node、`DSLDiffCalculator` の対応がありません。現コードでは iOS の [SectionBuilder.swift:82](ios/Sources/KsSettingsViewSwiftUI/SectionBuilder.swift:82) と Android の [DSLScope.kt:31](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScope.kt:31) は新トグルを受け取れません。また、同一 ID・同一 accessory のままトグルだけ変えても、既存差分検出は `headerHeight`／`isVisible` しか full 更新対象にしないため、変更が diff 0 件になる可能性があります。これは Store／DSL 対称性契約にも反します。

**推奨修正**: SwiftUI／Compose DSL の引数・node・resolved Section への転写を Requirement と tasks に追加してください。両 `DSLDiffCalculator` にトグル変化の full-refresh preflight を規定し、Store 経路と DSL 経路の対称テストを追加してください。

### [🟠 Major] iOS の既存 Section 再構築が新フィールドを暗黙に true へ戻す

**該当箇所**: [tasks.md:5](kasane/changes/add-accessory-visibility-toggle/tasks.md:5)、[KsSettingsViewController.swift:263](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:263)、[SettingsRootStore.swift:144](ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:144)、[SectionModifiers.swift:61](ios/Sources/KsSettingsViewSwiftUI/SectionModifiers.swift:61)

**問題点**: iOS は `copy` を持たず、多数の場所で `Section` を手動再構築しています。新フィールドを追加するだけでは、visible projection の作成時点でトグルが既定 `true` に戻ります。Cell の追加・削除・置換・移動、accessory 更新、DSL modifier でも同様の状態喪失が起こり得ます。現 tasks は `==`／`hash` と表示判定しか列挙していません。

**推奨修正**: 「既存の Section 再構築では両トグルを保存する」を明示要件にしてください。全手動再構築箇所を tasks に列挙し、`false` の Section に対する Cell 操作・accessory 更新・modifier 適用後も `false` が保たれる回帰テストを追加してください。

### [🟠 Major] Section.headerHeight 正値＋Header 不在の反転が Scenario／task から漏れている

**該当箇所**: [iOS spec:48](kasane/changes/add-accessory-visibility-toggle/specs/settings-view-ios-ui/spec.md:48)、[iOS spec:52](kasane/changes/add-accessory-visibility-toggle/specs/settings-view-ios-ui/spec.md:52)、[tasks.md:8](kasane/changes/add-accessory-visibility-toggle/tasks.md:8)

**問題点**: Requirement と ADR はすべての高さ解決を内容存在判定の後にすると定めていますが、具体的な Scenario と task は `Theme.headerHeight` だけです。現行テストは [KsSettingsViewControllerTests.swift:456](ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:456) で「`Section.headerHeight = 40` なら header nil でも生成する」と明示的に逆の仕様を固定しています。このテストを反転しない実装は ADR に違反します。

**推奨修正**: 少なくとも次の Scenario を追加してください。

- `header=nil`＋正の `Section.headerHeight` → Header を生成しない
- `header=.text("")`＋正の `Section.headerHeight` → Header を生成しない
- 同条件で `Theme.headerHeight` が正でも生成しない

既存の逆契約テストを変更する task も明記してください。

### [🟠 Major] iOS Binding API 定義の更新がなく、MAUI まで配管できない

**該当箇所**: [tasks.md:17](kasane/changes/add-accessory-visibility-toggle/tasks.md:17)、[tasks.md:27](kasane/changes/add-accessory-visibility-toggle/tasks.md:27)、[ApiDefinition.cs:665](maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:665)

**問題点**: iOS の Binding は `ApiDefinition.cs` に `KsBridgeSection` の公開面を手動定義しています。Swift DTO にプロパティを追加し、C# gateway から設定するだけでは C# API が生成されません。現 tasks には Binding 定義の更新も、生成された managed API の検証もありません。

**推奨修正**: iOS `ApiDefinition.cs` への2プロパティ追加を独立 task にしてください。Android 側も実際に生成される managed 名を固定し、両 TFM の gateway が初期構築・挿入・置換の全経路で値を輸送できることを検証してください。

### [🟡 Minor] トグルの独立性と内容・identity 保持を Scenario が十分に拘束していない

**該当箇所**: [iOS spec:9](kasane/changes/add-accessory-visibility-toggle/specs/settings-view-ios-ui/spec.md:9)、[Android spec:9](kasane/changes/add-accessory-visibility-toggle/specs/settings-view-android-ui/spec.md:9)、[MAUI spec:23](kasane/changes/add-accessory-visibility-toggle/specs/maui-core/spec.md:23)、[samples-ios spec:5](kasane/changes/add-accessory-visibility-toggle/specs/samples-ios/spec.md:5)

**問題点**: Header を隠した際に Footer／Cell が残ること、Footer を隠した際に Header／Cell が残ることが明示されていません。また、非表示中に内容を更新してから再表示した場合の内容保持、MAUI の Section 置換をまたぐ Cell ID／書き戻し保持も受け入れ条件にありません。Sample の記述も、Header と Footer を独立した2操作にするのか、1操作で同時に切り替えるのか判定不能です。

**推奨修正**: 独立トグルの2×2状態、非対象領域の不変、非表示中の内容更新後の復帰、Cell ID／操作通知の保持を Scenario に追加してください。Sample は Header 用・Footer 用の独立した切り替え操作と対象 Section を明記してください。

## アクションプラン

1. 変更級を L に改め、ADR-0023 の承認状態と互換性変更をオーナー判断で確定する。
2. `design.md` と `ui/` を追加し、Store・DSL・Bridge・MAUI の全経路を設計上列挙する。
3. デルタスペックへ DSL 対称性、Section 再構築時の値保持、高さの全組み合わせ、独立性／identity 保持を追加する。
4. tasks に iOS の全手動 Section 再構築、両 DSL 差分検出、iOS Binding 定義、既存逆契約テストの更新を追加する。
5. 修正後に再レビューする。

**件数**: Critical 0 / Major 7 / Minor 1 / Suggestion 0

---

## 突き合わせ結果 (ホスト側判定 2026-08-19)

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | M 級判定と ui/ 欠如 | **降格** | 級はオーナー確定済み (proposal 確認)。ui/ 省略も理由付きで proposal に明記しオーナー承認済み (先行 MAUI 系 change と同型)。ただし指摘4の DSL スコープ追加で前提が変わるため、級の再確認のみオーナーへ提示 |
| 2 | ADR proposed のまま「確定済み」 | **降格** | Kasane の運用では ADR の accepted 昇格は蒸留時が正規 (先行 change も proposed 参照で実装)。決定自体はフェーズ議論でオーナー承認済み。proposal の表現のみ「フェーズ議論で承認済み (ADR は蒸留時確定)」へ改善 |
| 3 | 「既定 true で挙動不変」と対称化の矛盾 | **採用** | 実在の文言矛盾。互換性主張を「非空の既存 accessory に限る」へ限定し、対称化2件を明示的な breaking 変更として分離 |
| 4 | 宣言 DSL 経路 (SwiftUI/Compose) の欠落 | **採用** (ホスト側の見逃し) | 実物確認済み — ksSection / DSLScope.Section にトグル引数なし。core/ADR-0018 (Store/DSL 対称テスト義務) にも抵触。specs / tasks へ DSL 経路と対称テストを追加 |
| 5 | iOS の手動 Section 再構築でトグルが true へ戻る | **採用** | iOS Section は copy を持たず再構築箇所多数。保持要件の Scenario と全再構築箇所の task を追加 |
| 6 | Section.headerHeight 正値 + header 不在の反転漏れ | **採用 (契約判断はオーナーへ)** | 既存テストが逆契約を意図的に固定 (KsSettingsViewControllerTests.swift:458)。ただし Android は同条件で行を生成せず、現行挙動自体が OS 非対称 → 反転の方向性をオーナーに確認 |
| 7 | iOS Binding ApiDefinition.cs の更新漏れ | **採用** | 実物確認済み — KsBridgeSection の公開面は手動定義。task 追加 |
| 8 | 独立性・identity 保持の Scenario 不足 (Minor) | **採用** | 2×2 独立・非対象領域の不変・非表示中の内容更新→再表示の保持・sample の操作構成を Scenario に追加 |

採用 6 / 降格 2 / 未解決 0 (うち #6 はオーナー裁定待ちの契約論点)
