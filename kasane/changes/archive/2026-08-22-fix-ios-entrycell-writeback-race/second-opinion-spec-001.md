# セカンドオピニオン: fix-ios-entrycell-writeback-race (spec-001)
**相方**: codex (器: ksn-reviewer) / **日付**: 2026-08-22 / **対象**: 提案一式 (proposal.md / specs/settings-view-ios-ui/spec.md / tasks.md)
---
# レビュー結果: fix-ios-entrycell-writeback-race

**日付**: 2026-08-22  
**指摘件数**: Critical 0 / Major 5 / Minor 2 / Suggestion 0  
**実施範囲**: 静的レビューのみ。ビルド・テスト・ファイル書き込みは未実施。

## サマリー

フォーカス中の入力欄を SSoT とする基本方針は、既存コードおよび Android の先例と整合しています。一方、検証対象と称する Store 経路が実際には DSL 経路であること、IME に関する Requirement 間の矛盾、中心的な同一性 Scenario の観測不能性など、実装前に解消すべき仕様上の問題があります。

## 指摘事項

### [🟠 Major] 「Store 経路」の検証対象が実際には SwiftUI DSL 経路

**該当箇所**: [proposal.md:24](kasane/changes/fix-ios-entrycell-writeback-race/proposal.md:24)、[tasks.md:13](kasane/changes/fix-ios-entrycell-writeback-race/tasks.md:13)、[tasks.md:68](kasane/changes/fix-ios-entrycell-writeback-race/tasks.md:68)、[InputCellsDemoView.swift:86](samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:86)

**問題点**: メール欄とニックネーム欄はいずれも `KsSettingsView { ... }` 内にあり、ニックネーム欄も callback で `@State` を更新した後、SwiftUI body 再評価から内部 DSL Store へ流れます。外部所有の `SettingsRootStore` を直接 `replaceCell` する Store 方式ではありません。

したがって、タスク1.4の「Store 直接経路の窓の有無を兼ねる」およびタスク4.2の別経路検証という主張は成立しません。メール欄との違いは Binding initializer と callback initializerだけで、遅延境界はどちらも SwiftUI DSLです。

**推奨修正**: 次のいずれかにしてください。

- 外部 `SettingsRootStore` と安定IDを持つ `EntryCell` を用意し、callbackから `store.replaceCell` する実際の Store 方式ハーネスを追加する。
- Store直接経路を検証対象から外し、proposal・tasksから「Store経路」「窓の実測を兼ねる」という主張を削除して未検証範囲に明記する。

### [🟠 Major] ニックネーム欄を `value` prefix で一意に取得できない

**該当箇所**: [tasks.md:13](kasane/changes/fix-ios-entrycell-writeback-race/tasks.md:13)、[InputCellsDemoView.swift:42](samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:42)、[InputCellsDemoView.swift:121](samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:121)

**問題点**: スクリプトは `dump ui` の `value` を既知の `PREFIX` で検索する設計ですが、ニックネームの初期値は空文字で、入力欄には `accessibilityIdentifier` もありません。空 prefix は複数要素に一致し得るため、対象の取得、BEFORE値の確定、rect追跡を一意に実装できません。

**推奨修正**: 安定した `accessibilityIdentifier` をテスト用に設定するか、ニックネームを一意な非空 sentinel で初期化してください。スクリプトには「一致件数が必ず1件であること」の検証を加え、0件・複数件はSKIPではなく前提失敗として扱ってください。

### [🟠 Major] 入力系プロパティ変更時のIME契約がRequirement間で矛盾している

**該当箇所**: [spec.md:18](kasane/changes/fix-ios-entrycell-writeback-race/specs/settings-view-ios-ui/spec.md:18)、[spec.md:83](kasane/changes/fix-ios-entrycell-writeback-race/specs/settings-view-ios-ui/spec.md:83)、[spec.md:106](kasane/changes/fix-ios-entrycell-writeback-race/specs/settings-view-ios-ui/spec.md:106)、[tasks.md:35](kasane/changes/fix-ios-entrycell-writeback-race/tasks.md:35)

**問題点**: 以下が同時に規定されています。

- フォーカス中の同一Cell再renderはtext・キャレットを変更してはならない。
- `keyboardType`・`isPassword`・`maxLength` の変更ではIME未確定文字列の確定を伴ってよい。
- 同一Cell再renderだけを原因としてmarkedTextを確定・破棄してはならない。

入力系プロパティ変更が例外なのか、IME保全が常に優先されるのかが決まっていません。特に既存実装の `isSecureTextEntry` は `secureSavedText` による復元処理を持つため、曖昧なままでは文字消失やcallback発火順序の不具合を作り込みやすい箇所です。

加えてtasksのテスト対象はplaceholderと`isEnabled`だけで、`keyboardType`・`isPassword`・`maxLength` のフォーカス中変更を検証しません。

**推奨修正**: 入力系プロパティ変更をIME保全契約の明示的な例外とするか、markedText中は変更を遅延するかを決定してください。その上で3プロパティそれぞれについて、フォーカス中の変更後のtext・キャレット・markedText・callback発火可否をScenarioとunit testに追加してください。

### [🟠 Major] 「異なるid・同じtext」のScenarioが正しい同一性更新を観測できない

**該当箇所**: [spec.md:71](kasane/changes/fix-ios-entrycell-writeback-race/specs/settings-view-ios-ui/spec.md:71)、[tasks.md:54](kasane/changes/fix-ios-entrycell-writeback-race/tasks.md:54)

**問題点**: 異なるIDでもtextが同じなら、正しい実装でも、ID更新を忘れた実装でも表示結果は同じです。「新しいCellとして扱われる」は内部状態への言及であり、現状のTHENだけではテストで判定できません。

同一性判定は本修正の中心なので、表示が偶然一致するだけのテストでは回帰を検出できません。

**推奨修正**: 後続操作までScenarioに含めてください。例えば:

1. フォーカス中のCell Aへ、同じtextを持つCell Bをrenderする。
2. 続いて異なるtextを持つCell Bをrenderする。
3. Bが現在の同一Cellとして認識され、フォーカス中ガードが働くことを確認する。
4. 入力イベントがAではなくBのcallbackへ届くことも確認する。

これなら保持IDとhandlerの両方を観察可能に検証できます。

### [🟠 Major] 修正前に再現できなかった場合の停止条件がない

**該当箇所**: [proposal.md:35](kasane/changes/fix-ios-entrycell-writeback-race/proposal.md:35)、[tasks.md:19](kasane/changes/fix-ios-entrycell-writeback-race/tasks.md:19)、[runtime-behavior-verification.md:15](kasane/concepts/cross/conventions/runtime-behavior-verification.md:15)

**問題点**: Simulatorで再現しない場合のWDA・pixie4フォールバックはありますが、そこでもFAILが得られなかった場合の扱いがありません。後続の修正後試験はFAIL 0で合格できるため、修正前から再現していなくても形式上は完了できてしまいます。

これは「修正前に実環境で症状を再現する」を完了条件とする既存規約に反します。

**推奨修正**: グループ1を明示的なゲートにしてください。校正済みの注入手段で修正前FAILを少なくとも1件確立できなければ、実装へ進まず探索へ戻ること、A/B証跡が成立しない限りADRをacceptedへ昇格しないことをtasksへ明記してください。

### [🟡 Minor] 保証対象となる更新APIの範囲が曖昧

**該当箇所**: [spec.md:12](kasane/changes/fix-ios-entrycell-writeback-race/specs/settings-view-ios-ui/spec.md:12)、[spec.md:30](kasane/changes/fix-ios-entrycell-writeback-race/specs/settings-view-ios-ui/spec.md:30)、[display-state-synchronization.md:63](kasane/concepts/core/architecture/display-state-synchronization.md:63)

**問題点**: 用語定義は「同じ`EntryCellView`への再render」に限定されていますが、Scenarioは単に「プログラムから同一Cellを更新」と書かれています。既存Hostには`replaceSection`など、条件によってNative cellをreloadし、first responderを失い得る更新経路があります。view-localなガードだけではその経路を保証できません。

**推奨修正**: 保証対象を「同一Native cellを維持するreconfigure経路」に限定し、Native cell交換・Section reloadは対象外と明記するか、それらも保証するならHost側の実装・テストをスコープへ追加してください。

### [🟡 Minor] IME証跡が「再renderが発生した」ことを確認できない

**該当箇所**: [tasks.md:74](kasane/changes/fix-ios-entrycell-writeback-race/tasks.md:74)

**問題点**: markedTextを表示したスクリーンショットだけでは、その最中に書き戻しエコーによる再renderが実際に到着したことを示せません。再renderが起きていなければ、Scenarioは空振りでも成功に見えます。

**推奨修正**: 一時ログまたはカウンタで、markedText中に同一Cellの`render`が到着したことを記録し、スクリーンショットと同じ試行の証跡としてevidence.mdへ残してください。

## アクションプラン

1. IME・入力系プロパティ変更の優先順位と、保証対象となる更新経路を確定する。
2. 実際のStore方式を検証するか、対象外としてproposal/tasksを修正する。
3. ニックネーム欄の一意な取得方法と、修正前再現の必須ゲートを定義する。
4. 同一性ScenarioおよびIME証跡を、誤実装を落とせる観測可能な形へ変更する。

**判定: NEEDS_DISCUSSION**


## 突き合わせ結果 (ホスト側自己レビューとの照合、2026-08-22)

ホスト側の自己レビュー (ksn-propose Step 8 チェックリスト) は 2 周とも指摘なしだったため、以下はすべて「相方のみ」の指摘として根拠で採否を判定した。

| # | 指摘 | 裏取り | 採否 | 反映 |
|---|---|---|---|---|
| Major-1 | ニックネーム欄は Store 直接経路ではなく SwiftUI DSL 経路 | `InputCellsDemoView.swift:121` で確認 — `KsSettingsView { }` 内の callback → `@State` → body 再評価。`StoreDemoView.swift` に EntryCell なし | **採用** | オーナー裁定 (2026-08-22): (a) `StoreDemoView` に callback で `store.replaceCell` する EntryCell を追加し、実 Store 直接経路を試験対象にする (tasks 1.0 / 1.5 / 4.2、proposal What Changes)。ゲート対象はメール欄のままで、Store 欄は窓の有無の実測として記録 |
| Major-2 | ニックネーム欄は初期値が空で PREFIX 検索できない | `InputCellsDemoView.swift:42` で確認 | **採用** | tasks 1.3: 一致件数 1 件でなければ前提失敗として非ゼロ終了。ニックネーム欄は対象から外し、Store 欄は非空で一意な初期値で作る (tasks 1.0) |
| Major-3 | 入力系プロパティ変更時の IME 契約が Requirement 間で矛盾 | spec の「再 render だけを原因として」の限定で意図は表せていたが、例外関係が明示されていなかった | **一部採用** | spec: 優先順位 2. を「入力継続性 (IME)」の明示的な例外と記述し、IME 側にも例外を明記。3 プロパティのフォーカス中変更の Scenario / test 追加は**降格** — 既存挙動 (変更しない) で Android 版と同範囲、本変更の対象外 |
| Major-4 | 「異なる id・同 text」Scenario は正しい同一性更新を観測できない | 指摘どおり (表示が偶然一致する) | **採用** | spec: Scenario を「以後の入力が B の callback へ届く」+ 新 Scenario「別 Cell の render 後は新しい Cell が同一性の基準になる」に強化。tasks 3.4 を保持 id と handler の両方を観測する形に更新 |
| Major-5 | 修正前に再現できなかった場合の停止条件がない | 実行時挙動の検証規約に照らして正当 | **採用** | tasks グループ 1 を実装ゲートに格上げ (FAIL 1 件以上の確立が必須、得られなければ探索へ戻す、A/B 不成立なら ADR を accepted にしない) |
| Minor-1 | 保証対象となる更新 API の範囲が曖昧 | `applyReplaceSection` は Full 経路で reload (Native cell 交換) を確認 | **採用** | spec 用語: 保証対象を同一 Native cell の reconfigure 経路に限定し、交換経路を対象外と明記。proposal Non-Goals にも追加 |
| Minor-2 | IME 証跡が「再 render が発生した」ことを確認できない | 指摘どおり | **採用** | tasks 4.4: markedText 中の `render` 到着を一時ログ / カウンタで記録し同じ試行の証跡にする |

集計: 採用 5 / 一部採用 1 / 降格 1 (Major-3 のテスト拡張部分) / 未解決 0 (両者の矛盾なし)
