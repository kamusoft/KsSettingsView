# セカンドオピニオン: scroll-indicator-visible-not-applied (code-002)
**相方**: codex / **label**: so-code-scroll-indicator-visible-not-applied / **日付**: 2026-09-17 / **対象**: develop の作業ツリーの未コミット差分のうち android/ ・ ios/ ・ maui/ 配下と kasane/changes/scroll-indicator-visible-not-applied/evidence/
---
# レビュー結果: scroll-indicator-visible-not-applied

**判定**: CHANGES_REQUESTED  
**指摘件数**: Critical 0 / Major 1 / Minor 4 / Suggestion 0  
**結果ファイル**: 依頼どおり作成していません。

## サマリー

スクロールバーの適用範囲、利用者 Context の維持、iOS の背景色反映、既定透明色の実装は概ね仕様と整合しています。提示された全テスト結果も成功しています。

一方、完了済みになっている実環境検証の一部が、証跡側で未実施と明記されています。完了ゲートを満たしていないため CHANGES_REQUESTED です。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`
- `kasane/handbook/cross/runtime-behavior-verification.md`
- `kasane/handbook/cross/aiforms-origin-reference.md`
- `kasane/handbook/ios/swift6-language-mode-check.md`
- `kasane/handbook/maui/integration-host-verification.md`
- core/ADR-0018、core/ADR-0030、android/ADR-0020、cross/ADR-0026
- `kasane/lessons/code-review.md` L-001
- Kotlin null safety・DSL・testing・hygiene 観点

core/ADR-0032 は proposed のため、確定済み決定として判定根拠にはしていません。

## 指摘事項

### [🟠 Major] 未実施の実環境検証を完了済みにしている

**該当箇所**: `tasks.md:29`、`evidence/README.md:73`

**問題点**: task 5.2 は、Activity を再生成しない夜間モード変更で Android のスクロールバーのつまみが追従することを実機・エミュレータで確認すると定め、完了済みになっています。しかし `evidence/README.md:78-81` は、その経路を実環境では確認できず、Robolectric テストで代替したと明記しています。

これは単なる画像不足ではありません。生成済み View、Configuration 通知、実 drawable の差し替えが関係する実行時挙動であり、Robolectric は `runtime-behavior-verification.md` および cross/ADR-0026 が要求する実環境確認の代替になりません。また「両 OS の Classic / Modern」とした既定透明の証跡も、Android の未指定 Modern がありません。

**推奨修正**: `configChanges` で `uiMode` を処理する最小検証ホスト、または該当構成を持つ MAUI Android ホストを使い、同一 Activity・同一 `KsSettingsView` のまま外観を切り替えてつまみが変わることを確認してください。識別子と drawable 状態を含む sanitize 済みログ、または判別可能な静止画を evidence に残してください。実施できない場合は task を未完了へ戻し、代替検証を受け入れるオーナー判断を deviation として記録する必要があります。

### [🟡 Minor] Android の Store 更新 Scenario がテストされていない

**該当箇所**: `specs/settings-view-android-ui/spec.md:28`、`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollIndicatorVisibleTest.kt:120`

**問題点**: Scenario は「表示中の Store に `scrollIndicatorVisible=false` を更新する」経路を要求していますが、該当テストは最初から false の Store を `bind` しているだけです。Store の後続 `applyTheme` → collect → 表示更新は観測していません。tasks 1.3 の「Store 経由を担保」とも一致しません。

**推奨修正**: 既定 Theme の Store を bind して表示を確定した後、`store.applyTheme(Theme(scrollIndicatorVisible = false))` を呼び、条件ベース待機で内部 RecyclerView が無効になることを検証してください。

### [🟡 Minor] AsyncListDiffer の収束を固定回数の idle で待っている

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/HostWrappedContextUserContentTest.kt:101`

**問題点**: `repeat(2) { idle(); layout... }` は、バックグラウンドで動く `AsyncListDiffer` の完了条件を待っていません。main looper が一時的に空なら、差分計算結果が post される前に `idle()` が戻るため、負荷時に利用者 View の factory がまだ呼ばれず flaky になり得ます。これは `test-execution.md` の条件ベース待機規約に反します。

**推奨修正**: `awaitConvergence(target) { internalMainListAdapter().currentList.isNotEmpty() }` を使用し、その後も必要なら accessory Context と `ComposeView` が生成されたこと自体を条件として待ってください。

### [🟡 Minor] 公開 API の doc comment に内部かつ未確定の ADR を露出している

**該当箇所**: `ios/Sources/KsSettingsViewUI/Theme.swift:283`

**問題点**: `defaultHeaderBackgroundColor` は public API ですが、その doc comment に `core/ADR-0032` が含まれています。公開 doc comment へ ADR・Kasane 等の内部用語を含めることは `comment-policy.md` で禁止されています。加えて ADR-0032 は proposed であり、一般のソースコメントで許容される確定済み ADR 参照にも該当しません。同じ proposed ADR 参照は複数のテストコメントにもあります。

**推奨修正**: public doc comment から ADR 参照を削除し、現在の公開契約だけで自己完結させてください。テストコメントも、ADR が確定するまでは仕様内容を直接記述してください。

### [🟡 Minor] 既定色パレットの説明が変更後の契約と矛盾している

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemePalette.kt:9`

**問題点**: KDoc は light セットを「既存利用者の見た目を変えないための現行値」と説明していますが、この変更は light の Header / Footer 背景を不透明な `#F2F2F7` から透明へ変更します。変更後はコメントが事実ではなくなります。

**推奨修正**: 履歴的な「見た目を変えない」という説明を外し、light/dark の現在の契約を記述してください。必要なら「Header / Footer 背景だけは両外観とも透明」と明記します。

## アクションプラン

1. Android の非再生成 uiMode 切替を実環境で検証し、証跡を追加する。
2. Android の Store 後続更新テストを追加する。
3. `HostWrappedContextUserContentTest` の待機を条件ベース化する。
4. public doc comment の内部 ADR 参照と、既定色パレットの古い説明を修正する。
5. 修正後に最終 diff を再レビューする。



## 突き合わせ結果

突き合わせの相手: ホスト側の独立レビュー review-002.md (CHANGES_REQUESTED。Major 1 / Minor 2 / Suggestion 2)。2026-09-17。両者の指摘に矛盾なし。

| # | 指摘 | 出どころ | 採否 | 根拠 |
|---|---|---|---|---|
| 1 | 設定リストの Context 配線に回帰検出力が無い (配線を戻しても関連テストが全件 green) | ホストのみ (Major) | 採用 | ミューテーション実測に基づく。観測点 (つまみの drawable が非 null) が夜間モード追従の代入で常に満たされる |
| 2 | tasks 5.2 が完了済みだが evidence が未実施と明記する実環境確認がある (Android の非再生成 uiMode 切替・iOS のインジケータ目視・Android の Modern 未指定の証跡) | 双方一致 (相方 Major / ホスト Minor) | 確定 (Major) | 重要度は高い方を採る。runtime-behavior-verification の完了判定に関わる |
| 3 | 公開 doc comment に内部かつ proposed の ADR 参照 (ios の Theme.swift)。テストコメントの proposed ADR 参照も同様 | 双方一致 (Minor) | 確定 | comment-policy の禁止事項 |
| 4 | Android の Store 後続更新の Scenario がテストされていない (最初から false の Store を bind しているだけ) | 相方のみ (Minor) | 採用 | 根拠強 (Scenario の WHEN とテストの操作が一致しない) |
| 5 | HostWrappedContextUserContentTest が AsyncListDiffer の収束を固定回数の idle で待つ | 相方のみ (Minor) | 採用 | 根拠強 (test-execution の条件ベース待機規約) |
| 6 | KsThemePalette の KDoc が変更後の契約と矛盾 | 相方のみ (Minor) | 採用 | 根拠強 (記述が事実でなくなる) |
| 7 | 未参照の internal 定数 / 「選択面を開き直すと新しい値に従う」の SHALL にテストが無い | ホストのみ (Suggestion) | 採用 | 後者は Requirement の SHALL に対するテスト欠落 |

集計: 確定 2 / 採用 5 / 降格 0 / 未解決 0
