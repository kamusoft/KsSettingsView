# セカンドオピニオン: release-host-without-bridge-dispose (001)
**相方**: codex / **日付**: 2026-08-07 / **対象**: 提案一式 (proposal / specs / tasks、spec-review モード)
---
## 指摘事項

1. **Critical — Host 解放後の旧 handle の扱いが未定義**

   **該当箇所:** Requirement「Host の単独解放」「Native Host の生成と接続」  
   [spec.md:7](kasane/changes/release-host-without-bridge-dispose/specs/maui-bridge/spec.md:7)、[spec.md:40](kasane/changes/release-host-without-bridge-dispose/specs/maui-bridge/spec.md:40)

   **問題点:** `releaseHost()` 後も呼び出し側が旧 handle を保持・表示し続ける場合、その Host が Store の購読を継続するのか、無効化されるのか、view 階層からの取り外しを誰が担うのか決まっていない。このまま Bridge が内部参照を `nil` / `null` にするだけで再生成すると、旧 Host と新 Host が同じ Store を同時購読し、「同時に1つの Host」と矛盾する。

   現状、iOS の購読解除は Controller の `deinit` 時のみであり、Android にも明示的な `unbind()` はない。[KsSettingsViewController.swift:206](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:206)、[KsSettingsView.kt:298](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:298)

   **推奨修正:** 次のどちらかを明示的に決定する。

   - `releaseHost()` が旧 Host の Store 購読を解除して無効化する
   - 呼び出し側が detach・参照破棄を完了してから呼ぶことを前提条件とする

   あわせて「release 後の Store 更新は旧 Host に反映されず、新 Host のみに復元される」Scenarioを追加する。前者なら UI モジュール側の disconnect/unbind API追加を tasks に含める。

2. **Major — Context 解放保証に判定可能な受け入れ基準がない**

   **該当箇所:** Requirement「Host の単独解放」、Scenario「Android は解放後に別の Context で再生成できる」  
   [spec.md:7](kasane/changes/release-host-without-bridge-dispose/specs/maui-bridge/spec.md:7)、[spec.md:29](kasane/changes/release-host-without-bridge-dispose/specs/maui-bridge/spec.md:29)

   **問題点:** 「Bridge は Context を含む資源への参照を持たない」は内部構造の主張であり、現在のScenarioは新 Host が別 Context で生成できることしか検証しない。旧 Context を保持したままでも合格できるため、変更の主要目的である Activity 解放を保証できない。

   **推奨修正:** release 前の Host を detach し、外部参照を破棄した後、Bridge/Store経由で旧 Activity が保持されないことを検証する lifecycle/leak Scenarioを追加する。GC依存テストを採る場合は実行環境と判定方法も tasks に明記する。

3. **Major — `.NET binding` 契約がデルタスペックから欠落している**

   **該当箇所:** [tasks.md:17](kasane/changes/release-host-without-bridge-dispose/tasks.md:17)、既存 Requirement「.NET binding からの呼び出し」  
   [旧 spec.md:108](kasane/changes/archive/2026-08-05-add-maui-native-bridge/specs/maui-bridge/spec.md:108)

   **問題点:** tasks は Binding から `releaseHost()` を呼べることを要求しているが、今回のデルタスペックには対応Requirementがない。Native APIだけ追加され、C#へ公開されなくても新デルタスペック上は合格でき、MAUI Handlerから利用できない実装を排除できない。

   **推奨修正:** 既存「.NET binding からの呼び出し」を MODIFIED として全文掲載し、公開API列挙へ `releaseHost` を追加する。両OSでのコンパイル・リンクと実行時呼び出しをScenario化する。

4. **Major — 実装の前提となるADRが未確定かつ記述が不整合**

   **該当箇所:** [ADR-0007:4](kasane/decisions/maui/0007-release-host-without-bridge-dispose.md:4)、[ADR-0007:34](kasane/decisions/maui/0007-release-host-without-bridge-dispose.md:34)、[proposal.md:17](kasane/changes/release-host-without-bridge-dispose/proposal.md:17)

   **問題点:** ADR-0007 は `proposed` のまま、accepted の ADR-0005 の生成制約を置き換えようとしている。またADR-0007は最終 `dispose()` 時機を「未解決」とする一方、proposalはphase-2で「明示 dispose なし決定済み」としており矛盾する。

   **推奨修正:** 最終dispose方針をADR-0007へ反映するか、proposalから「決定済み」の記述を除去する。そのうえで、本変更の実装開始前ゲートとしてADR-0007のacceptを明示する。

5. **Minor — 冪等ScenarioがStore維持を検証しない**

   **該当箇所:** Scenario「releaseHost は冪等」  
   [spec.md:19](kasane/changes/release-host-without-bridge-dispose/specs/maui-bridge/spec.md:19)

   **問題点:** 期待結果が「エラーやクラッシュがない」だけなので、2回目の呼び出しでStoreを消去する実装でも合格する。

   **推奨修正:** 複数回の `releaseHost()` 後に `makeHost*` し、rootとThemeが維持されていることまで期待結果に含める。

6. **Minor — tasks のScenario対応が不正確**

   **該当箇所:** [tasks.md:7](kasane/changes/release-host-without-bridge-dispose/tasks.md:7)、[tasks.md:18](kasane/changes/release-host-without-bridge-dispose/tasks.md:18)

   **問題点:** iOSタスクが「全Scenario」対応を称しているが、Android固有Context Scenarioは対象外である。またE2Eタスクには対応Requirement、観察方法、合格条件がない。既存検証ホストは目視確認中心なので、自動判定可能なテストと解釈できない。

   **推奨修正:** 各OSタスクへ具体的なScenario名を列挙し、E2Eは自動assertか、期待表示・操作手順を固定した明示的な手動受け入れ手順のどちらかにする。

7. **Suggestion — Impact の「既存12メソッド」が曖昧**

   **該当箇所:** [proposal.md:22](kasane/changes/release-host-without-bridge-dispose/proposal.md:22)

   **問題点:** `makeHost*` は既存APIで契約が変更されるため、「既存12メソッドの挙動は不変」は読み方によってproposal内で矛盾する。

   **推奨修正:** 「既存のStore操作12メソッド」と限定し、`makeHost*` は条件付きで契約変更されることを併記する。

総合判定: NEEDS_DISCUSSION

## 突き合わせ結果 (2026-08-07)

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| 1 | Critical: 旧 handle の扱い未定義 | **採用** (根拠強: 購読解除が deinit のみと実コードで特定) | オーナー裁定で「Bridge が購読解除し無効化」を採用。spec に無効化契約と Scenario 2件追加、UI モジュールへの購読解除 API 追加を tasks に反映 |
| 2 | Major: Context 解放の受け入れ基準なし | **採用** | Scenario「解放後、Bridge は旧 Host への参照を保持しない」(WeakReference 検証) を追加、tasks に判定方法を明記 |
| 3 | Major: .NET binding Requirement 欠落 | **採用** | 「.NET binding からの呼び出し」を MODIFIED で全文掲載し releaseHost を追加、実行時 Scenario も追加 |
| 4 | Major: ADR 未確定 + dispose 記述不整合 | **採用 (部分)** | ADR-0007 の「未解決のまま残る」を「本 ADR の範囲外 (利用層の設計判断)」に修正。accepted 化を実装前ゲートとして tasks に追加 |
| 5 | Minor: 冪等 Scenario が Store 維持を検証しない | **採用** | THEN に「makeHost* が root と Theme を復元」を追加 |
| 6 | Minor: tasks の Scenario 対応が不正確 | **採用** | 各タスクに Scenario 名を列挙、E2E に合否基準と証跡を明記 |
| 7 | Suggestion: 12メソッド表記が曖昧 | **採用** | 「既存の Store 操作 12 メソッド」に限定し makeHost* の条件付き契約変更を併記 |

採用 7 / 降格 0 / 未解決 0。NEEDS_DISCUSSION の論点 (#1 の設計判断・#4 の ADR ゲート) はオーナー裁定で解消。
