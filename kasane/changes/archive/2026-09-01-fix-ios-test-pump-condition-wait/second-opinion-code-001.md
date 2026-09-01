# セカンドオピニオン: fix-ios-test-pump-condition-wait (code-001)

**相方**: codex / **label**: so-code-fix-ios-test-pump-condition-wait / **日付**: 2026-09-01 / **対象**: 未 commit の作業ツリー全体 (tracked な変更 + 未追跡の新規 7 ファイル)

---


**判定: APPROVED**

Critical / Major に該当する問題はありません。条件待機、単調時計による期限管理、タイムアウト診断、固定待機の分類はデルタスペックおよび `triage.md` と整合しています。`deviation.md` の記録事項は合意済み差分として除外しました。

指摘件数: Critical 0 / Major 0 / Minor 2 / Suggestion 1

## 指摘事項

### Minor — コメントが旧実装の待機方式を説明している

- **該当箇所:** `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1067`
- **問題点:** コメントに「1 run loop 待つ」とありますが、実装は `awaitCondition` による条件成立までのポーリングへ変更されています。現在のタイミング保証を誤解させる説明です。
- **推奨修正:** 「`becomeFirstResponder()` の結果が観測できるまで条件ベースで待つ」など、現在の保証内容に合わせて更新してください。

### Minor — 新規テストで GCD による遅延実行を使用している

- **該当箇所:**
  - `ios/Tests/KsSettingsViewTestSupportTests/ConditionWaitTests.swift:60`
  - `ios/Tests/KsSettingsViewTestSupportTests/ConditionWaitTests.swift:111`
- **問題点:** iOS ドメインに適用される Swift 実装規律では、非同期処理に `DispatchQueue` ではなく Swift Concurrency を使用する方針です。今回のテストは新規コードのため、この規律から外れています。
- **推奨修正:** `Task { @MainActor in ... }` と `Task.sleep(for:)`、または async XCTest を用いた遅延更新へ置き換えてください。

### Suggestion — 診断文字列の数値整形が C 形式

- **該当箇所:** `ios/Tests/KsSettingsViewTestSupport/KsTestWait.swift:42`
- **問題点:** `String(format:)` は適用される Swift 実装規律で避ける対象です。また、診断文字列の小数点表記が実行ロケールへ依存する余地があります。
- **推奨修正:** 小数点以下3桁を指定した `FormatStyle` と明示的なロケールで整形してください。

## 確認結果

- 旧 `pump` / `pumpEntry` 呼び出しと固定 `RunLoop` 待機は、負の検証用ヘルパーを除いて残っていません。
- 共有 TestSupport ターゲットへの集約、タイムアウト時の実値出力、レイアウト専用処理は仕様と整合しています。
- ホスト提供の全997件成功、反復10/10成功、lint 0件を客観的証跡として確認しました。
- 静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。


---

## 突き合わせ結果

ホスト側レビュー (`review-001.md`、判定 CHANGES_REQUESTED / verify INVALID) との突き合わせ。**双方が一致した指摘は無く、相方の 3 件はいずれもホスト側が挙げていない**。3 件とも該当箇所が特定されており、規約本文と実物で裏を取れたため全件採用する。

| 相方の指摘 | 出典 | 検証 | 採否 |
|---|---|---|---|
| `InputCellsTests.swift` のコメントが旧実装の待機方式を説明している | 相方のみ | 実物を確認。「async Task で `becomeFirstResponder()` が走るのを 1 run loop 待つ」が残存し、実装 (条件成立まで待つ) と食い違う。直後に現在の挙動を説明する別コメントがあり、旧コメントの消し忘れ | **採用** (Minor) |
| 新規テストで GCD による遅延実行を使用している | 相方のみ | ios ドメインの実装スキルが「Grand Central Dispatch は使わない。常にモダン Swift Concurrency を使う」と明記。新規コードのため既存債務の例外に当たらない | **採用** (Minor) |
| 診断文字列の数値整形が C 形式 | 相方のみ | 同スキルが「C スタイルの数値フォーマットは使わない。`FormatStyle` API を使う」と明記。ただし**相方が挙げた「実行ロケールへ依存する余地」は不正確** — `String(format: "%.3f", …)` は C ロケール固定で小数点は `.` になる。採用理由は規約適合であってロケール依存ではない | **採用** (Suggestion → Minor 相当) |

- 未解決 (両者の指摘が矛盾): なし
- 降格: なし
- ホスト側が単独で挙げた Major 1 / Minor 3 / Suggestion 3 は `review-001.md` を参照。相方は APPROVED を出しており、**相方の APPROVED は問題が無いことの証明にならない** (昇格済み process L-002) ことが本 change でも再確認された
