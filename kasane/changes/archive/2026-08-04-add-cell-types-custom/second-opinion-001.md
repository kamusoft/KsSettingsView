# セカンドオピニオン: add-cell-types-custom (001 回目・spec-review)
**相方**: codex / **日付**: 2026-08-03 / **対象**: 提案一式 (proposal / design / specs / tasks / ui-brief) + ADR-0014
---
## 判定: CHANGES_REQUESTED

実装開始を止めるべき Critical が2件あります。特に等価性契約は proposal・design・spec・accepted ADR が互いに異なり、現状では正しい実装を一意に決められません。

## Critical

1. **Critical — 等価性契約が proposal / design / spec / ADR 間で矛盾**

   - 該当箇所: [proposal.md:13](kasane/changes/add-cell-types-custom/proposal.md:13)、[design.md:80](kasane/changes/add-cell-types-custom/design.md:80)、[spec.md:7](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:7)、[ADR-0014:20](kasane/decisions/core/0014-customcell-content-value-with-builder.md:20)
   - 問題点: proposal と ADR-0014 は「等価性は content のみ」と明記していますが、design/spec は `id / style / content / showArrow / isEnabled / isVisible` を対象にしています。design はこれを「厳密化」としていますが、accepted ADR の決定内容を実質変更しています。さらに design の ADR 候補では Decision 2 を候補外にしており、矛盾を解消する経路もありません。
   - 推奨修正: 現行 Diff が値等価を再 bind 判定に使うことを踏まえ、`id + 全値プロパティ - 関数値` を採るなら、ADR-0014 を覆す新 ADR を起票・accepted 化し、proposal の「content のみ」も修正してください。content のみに従うなら、style・showArrow・isEnabled の変更を反映する別の invalidation 契約が必要です。

2. **Critical — ComposeView / Hosting 専用行が accepted ADR-0011 の禁止決定と衝突**

   - 該当箇所: [ADR-0011:16](kasane/decisions/core/0011-composed-shared-cell-row-layout.md:16)、[design.md:90](kasane/changes/add-cell-types-custom/design.md:90)、[design.md:103](kasane/changes/add-cell-types-custom/design.md:103)
   - 問題点: ADR-0011 は「全 Cell」を共通行で構成し、Android RecyclerView 内では `ComposeView.setContent` を使わないと決定しています。一方、本 design は CustomCell を共通行から外し、ComposeView/SwiftUI hosting で全面差し替えします。現コードには将来の CustomCell を想定した [ComposeCellViewHolder.kt:8](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ComposeCellViewHolder.kt:8) があるため実装意図は読み取れますが、accepted ADR 群としては未解決の矛盾です。
   - 推奨修正: 「CustomCell は full-bleed 任意 UI のため ADR-0011 の明示的な例外」とする新 ADRを作り、ADR-0011・cell-row-layout の「全 Cell」という適用範囲を整理してください。この判断を design の ADR 候補にも追加すべきです。

## Major

3. **Major — `cellHeight` と動的高さの契約が現行挙動に反する**

   - 該当箇所: [proposal.md:18](kasane/changes/add-cell-types-custom/proposal.md:18)、[spec.md:101](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:101)、[spec.md:127](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:127)、[cell-row-layout.md:56](kasane/concepts/core/styling/cell-row-layout.md:56)
   - 問題点: spec は `.cellHeight()` だけで固定高さになる一方、常に自然サイズへ追従するとも読めます。現行契約では `hasUnevenRows == true` のとき cellHeight は最低高であり、固定されるのは `hasUnevenRows == false` の場合だけです。
   - 推奨修正: `CellStyle.cellHeight / Theme.rowHeight / platform minimum / hasUnevenRows` の組み合わせを表にして CustomCell の高さ契約を確定し、固定高・最低高・動的変更の Scenario を分けてください。

4. **Major — Android の star projection では設計どおり builder を呼べない**

   - 該当箇所: [design.md:56](kasane/changes/add-cell-types-custom/design.md:56)、[design.md:103](kasane/changes/add-cell-types-custom/design.md:103)
   - 問題点: ViewHolder は `CustomCell<*>` を受け取りますが、star projection 後の `(Content) -> Unit` に `content: Any` を渡すことは型安全にコンパイルできません。Registry が具象 `Content` 型を保持しないため、単純な `builder(content)` 実装では詰まります。
   - 推奨修正: `CustomCell` 内部に型付き content を閉じ込めた引数なしの composable renderer、または型消去済み invoker を持たせる設計を明記してください。異なる content 具象型を同一 ID へ差し替える場合の等価性も定義が必要です。

5. **Major — builder / onTap 更新だけでは再 bind されず古いクロージャが残る**

   - 該当箇所: [spec.md:9](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:9)、[spec.md:37](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:37)、[design.md:107](kasane/changes/add-cell-types-custom/design.md:107)
   - 問題点: builder/onTap は等価性から除外され、同一 content では再 bind されません。したがって builder のキャプチャ値や onTap の処理だけを変更して DSL を再評価しても、Renderer/ViewHolder は古い View 構成・listener を保持します。content なし糖衣は常に空値なので特に起こりやすい問題です。
   - 推奨修正: 「見た目・action を変える値は必ず content に含める」「同一 ID/content に対する builder/onTap は意味的に安定している必要がある」と公開契約へ追加するか、明示的な revision/rebind key を提供してください。builder-only/onTap-only 変更の Scenario も必要です。

6. **Major — `isEnabled` と content 内コントロールの意味が未定義**

   - 該当箇所: [spec.md:63](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:63)、[design.md:107](kasane/changes/add-cell-types-custom/design.md:107)、[cell-visual-states.md:23](kasane/concepts/core/styling/cell-visual-states.md:23)
   - 問題点: spec/design が保証するのは行 onTap の無効化だけです。しかし現行共通契約は、無効 Cell では内包 control の操作も抑止するとしています。CustomCell の SwiftUI/Compose content を自動的に disabled にするのか、利用者責務にするのかが決まっていません。
   - 推奨修正: `isEnabled=false` 時の行タップ、content 内 Button/Slider、押下 feedback、disabled 表現を個別に定義してください。既存契約の例外にする場合は Non-Goals/差異として明示すべきです。

7. **Major — 行タップと content 内ジェスチャの競合規則がない**

   - 該当箇所: [spec.md:65](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:65)、[spec.md:73](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:73)
   - 問題点: `onTap=nil` の場合しか content 内操作との共存を規定していません。`onTap != nil` の CustomCell に Button/Slider がある場合、子操作だけ発火するのか、行 onTap も発火するのか、二重発火を防ぐのかが未定です。特に Android で ComposeView 自体へ View.OnClickListener を設定する案は、Compose の pointer input と競合する可能性があります。
   - 推奨修正: 「子 control が gesture を消費した場合は行 onTap を発火しない」など優先順位を決め、背景タップ・子 Button・Slider drag の Scenario を追加してください。

8. **Major — 「標準登録集合」の具体的な接続先が決まっていない**

   - 該当箇所: [design.md:141](kasane/changes/add-cell-types-custom/design.md:141)、[spec.md:27](kasane/changes/add-cell-types-custom/specs/cell-types-custom/spec.md:27)
   - 問題点: iOS には basic/input の個別自動登録フラグがあり、shared Registry の場合だけ自動登録します（[KsSettingsViewController.swift:175](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:175)）。Android も LabelCell/EntryCell を sentinel に別々に登録します（[KsSettingsView.kt:214](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:214)）。CustomCell を basic へ混ぜるのか、専用 API/flag を作るのかが未決です。
   - 推奨修正: iOS/Android それぞれについて、登録 API、Host 初期化時の自動登録、opt-out、独立 Registry、既に一部 Cell が登録済みの場合の挙動を確定してください。

9. **Major — テストタスクだけでは Scenario の成否を判定できない**

   - 該当箇所: [tasks.md:10](kasane/changes/add-cell-types-custom/tasks.md:10)、[tasks.md:19](kasane/changes/add-cell-types-custom/tasks.md:19)、[tasks.md:27](kasane/changes/add-cell-types-custom/tasks.md:27)
   - 問題点: 「render/bind 出力」とだけあり、任意 SwiftUI/Compose content をどう観測するかが定義されていません。Android UI モジュールには現状 Compose UI test の依存もありません（[build.gradle.kts:113](android/ks-settingsview-ui/build.gradle.kts:113)）。また、同値 content の no-rebind、子 control 操作、disabled、showArrow と onTap の独立性は明示的なテストタスクがありません。
   - 推奨修正: accessibility identifier/testTag を持つ probe content とテスト harness を決め、各 Scenario とテスト名を1対1で列挙してください。依存追加または実機 UI テストが必要なら tasks に明記してください。

## Minor

10. **Minor — UI mock の個数と対象 platform が proposal と一致しない**

   - 該当箇所: [proposal.md:39](kasane/changes/add-cell-types-custom/proposal.md:39)、[brief.md:30](kasane/changes/add-cell-types-custom/ui/brief.md:30)
   - 問題点: proposal は「iOS/Android 各2案」としていますが、実体は共通の plan-a/plan-b と単一の [approved.png](kasane/changes/add-cell-types-custom/ui/mock/approved.png) です。tasks は両 platform で mock 照合するため、どちらの platform の正か判定できません。
   - 推奨修正: 共通 content 構成だけを表す platform-neutral mock だと明記するか、iOS/Android 別の承認画像を用意してください。

11. **Minor — iOS Sample のメニュー導線タスクが抜けている**

   - 該当箇所: [tasks.md:23](kasane/changes/add-cell-types-custom/tasks.md:23)、[SampleScreen.swift:15](samples/ios/KsSettingsViewSample/SampleScreen.swift:15)
   - 問題点: Android タスクはメニュー導線追加を明記していますが、iOS は View 追加のみです。現コードでは enum case、title、`demos`、destination の更新が必要で、漏れると画面が到達不能かつ sample-parity 違反になります。
   - 推奨修正: iOS/Android 両方について SampleScreen・メニュー・destination・タイトル一致を明示タスク化してください。

12. **Minor — content の nullability が platform 間で不一致**

   - 該当箇所: [design.md:39](kasane/changes/add-cell-types-custom/design.md:39)、[design.md:59](kasane/changes/add-cell-types-custom/design.md:59)
   - 問題点: iOS の `C: Hashable` は Optional content を受けられますが、Android の `Content : Any` は nullable content を受けられません。spec はこの公開 API 差を説明していません。
   - 推奨修正: 両 platform とも non-null を契約にするか、Android でも nullable content を許容するかを決め、spec/API例へ反映してください。

静的レビューのみ実施し、ファイル変更およびビルド・テスト実行は行っていません。

## 突き合わせ結果 (2026-08-03)

ホスト側自己レビュー (2周、tasks の対応漏れ2件のみ検出) との突き合わせ。双方一致の指摘はなし — 以下はすべて「相方のみ + 根拠強/弱」の判定。

| # | 重要度 | 採否 | 反映先 |
|---|---|---|---|
| 1 | Critical | **採用** (裏取り: ADR-0014 原文と design/spec の食い違いを確認) | ADR-0014 改訂 (ユーザー承認)、proposal 文言修正 |
| 2 | Critical | **採用** (裏取り: ADR-0011 Decision に「全 Cell」「setContent 不使用」明記を確認) | core/ADR-0015 新規起票 (ユーザー承認)、design Context / proposal Impact に参照追記 |
| 3 | Major | **採用** (裏取り: cell-row-layout.md の高さ解決契約を確認) | spec スタイル適用範囲 / 高さ自動追従を hasUnevenRows 条件付きに修正、Scenario 分割 |
| 4 | Major | **採用** (Kotlin star projection の型制約は事実) | design Decision 1 に消去済みエントリポイント `composeContent` を明記、tasks 2.2 |
| 5 | Major | **採用** (契約明文化として) | spec 定義と等価性に builder/onTap の意味的安定性契約を追記 |
| 6 | Major | **採用** (裏取り: cell-visual-states.md の保証事項を確認。ユーザー判断「ライブラリが操作抑止」) | spec 行タップ + Scenario 追加、design Decision 4、proposal、tasks 1.2/2.2 |
| 7 | Major | **採用** | spec 行タップに消費優先規則 + Scenario 追加、design Decision 4 |
| 8 | Major | **採用** (裏取り: iOS autoRegister フラグ / Android sentinel 方式は実在) | design Decision 7 を具体化、tasks 1.3/2.3 |
| 9 | Major | **一部採用** (probe content 方式・テスト依存の明記は採用) / **一部降格** (Scenario とテスト名の1対1列挙は tasks の粒度として過剰) | tasks 1.6/2.6 |
| 10 | Minor | **採用** | proposal UI アーティファクト文言修正、brief.md にプラットフォーム中立を明記 |
| 11 | Minor | **採用** (裏取り: SampleScreen の enum 構造は実在) | tasks 3.1 に iOS メニュー導線を明記 |
| 12 | Minor | **採用** (注記レベル) | spec に non-null 契約、design Risks に platform 差を追記 |

集計: 採用 11 / 一部採用・一部降格 1 / 全降格 0 / 未解決 0。ユーザー判断 3 件 (#1 ADR-0014 改訂 / #2 ADR-0015 起票 / #6 ライブラリ操作抑止) はいずれも推奨案で承認済み。
