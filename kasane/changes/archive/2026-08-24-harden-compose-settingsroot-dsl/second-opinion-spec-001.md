# セカンドオピニオン: harden-compose-settingsroot-dsl (spec-001)
**相方**: codex (器: ksn-reviewer) / **日付**: 2026-08-22 / **対象**: 提案一式 (proposal.md / exploration.md / specs/settings-view-android-ui/spec.md / tasks.md)
---
# レビュー結果: harden-compose-settingsroot-dsl

**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 0 / Major 5 / Minor 2 / Suggestion 0

## サマリー

文字列ヘッダ版の公開 API 契約が spec・探索・tasks 間で一致しておらず、実装者が一意に判断できません。また、引数順・DSL スコープ制御・警告消失について、現在の検証計画では契約違反を見逃せます。公開 API の範囲を確定し、検証タスクを補強してから実装へ進む必要があります。

静的レビューのため、ビルド・テストは実行していません。

## 指摘事項

### [🟠 Major] 文字列ヘッダ版の `footer` 契約が矛盾している

**該当箇所**: [spec.md:7](kasane/changes/harden-compose-settingsroot-dsl/specs/settings-view-android-ui/spec.md:7)

**問題点**: Requirement は両オーバーロードを「Section と同じ属性」とし、引数順にも `footer` を含めています。exploration も「Section と同じ引数セット」「iOS と完全対称」としています。一方、tasks 2.2 は文字列版へ新しい3引数だけを追加する計画で、現行の文字列版には `footer` がありません（[SettingsRootScope.kt:64](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt:64)）。対称性の参照先である iOS の文字列版 `ksSection` は `footer` を受け取ります（[SectionBuilder.swift:84](ios/Sources/KsSettingsViewSwiftUI/SectionBuilder.swift:84)）。

**推奨修正**: 次のいずれかを明示的に決定してください。

- 完全対称を採るなら、文字列版にも `footer: String? = null` を追加し、proposal・spec・tasks・Scenario を揃える。
- `footer` は対象外なら、Requirement 名、引数順の記述、「同じ引数セット」「完全対称」という主張を、追加する4属性だけの契約へ狭める。

### [🟠 Major] DSL スコープ制御の SHALL を検証できない

**該当箇所**: [spec.md:31](kasane/changes/harden-compose-settingsroot-dsl/specs/settings-view-android-ui/spec.md:31)

**問題点**: Requirement は `settingsRoot` と `KsSettingsView` の双方について、外側 receiver の「メンバーと拡張関数」を暗黙に呼べないことを SHALL としています。しかし Scenario は receiver 型への注釈付与をリフレクションで確認するだけです。tasks 1.4 の手動実証も `settingsRoot` の外側メンバー `section` という1経路だけで、`KsSettingsView` 側と拡張関数側を検証しません。proposal の Non-Goals と規範的 Requirement も釣り合っていません。

**推奨修正**: スコープ制御を契約に残すなら、少なくとも両 DSL 系統とメンバー／拡張関数を含む負のコンパイル検証を定義してください。自動検証を採らないなら、スコープ制御の文言を非規範的な互換性説明へ移し、Requirement は `@Target` と receiver 型の marker 維持に限定してください。

### [🟠 Major] 明記された引数順をテストが固定しない

**該当箇所**: [spec.md:7](kasane/changes/harden-compose-settingsroot-dsl/specs/settings-view-android-ui/spec.md:7)

**問題点**: 引数順は公開 API 契約として SHALL ですが、tasks 3.1〜3.4 のテストはすべて名前付き引数を使う計画です。実装者が `headerHeight` と `isVisible` などの順序を誤っても、全テストが通ります。

**推奨修正**: 両オーバーロードを位置引数で呼び、生成された各フィールドを検証するコンパイル可能なテストを追加してください。通常の単体テストソースで固定できるため、負のコンパイルテスト用依存は不要です。

### [🟠 Major] comment-policy lint の完了条件が現行コードと両立しない

**該当箇所**: [tasks.md:31](kasane/changes/harden-compose-settingsroot-dsl/tasks.md:31)

**問題点**: tasks 4.2 はリポジトリ全体の禁止違反0件を要求しますが、対象コードには既に lint が blocking として検出する `Requirement` の裸参照があります（[DSLScope.kt:162](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScope.kt:162)）。さらに、[SettingsRootScope.kt:115](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt:115) の change-id と、[DSLHandles.kt:109](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLHandles.kt:109) の「本提案／後続提案」もコメント規約に反します。現在の tasks にはこれらの修正がありません。

**推奨修正**: 対象ファイル内の違反コメントを、現在の契約だけで自己完結する説明または正式な ADR 参照へ直すタスクを追加してから lint を完了条件にしてください。

### [🟠 Major] 完了テストがプロジェクトのテスト実行規約を満たさない

**該当箇所**: [tasks.md:26](kasane/changes/harden-compose-settingsroot-dsl/tasks.md:26)

**問題点**: 完了条件が Compose module の `testDebugUnitTest` だけです。一方、tasks 自身が参照する規約は Android の完了判定を `android/` 直下の `./gradlew test` とし、debug/release 両 variant、絞り込みなし、実行件数の確認を要求しています。

**推奨修正**: module の debug テストは反復用として残し、完了ゲートに `./gradlew test --rerun-tasks` と XML からの実行件数・failure 数確認を追加してください。

### [🟡 Minor] Impact の「挙動変化なし」がデルタスペックと矛盾する

**該当箇所**: [proposal.md:33](kasane/changes/harden-compose-settingsroot-dsl/proposal.md:33)

**問題点**: 新しい引数によって、builder から従来生成できなかった `Section` を生成できること自体が観察可能な挙動追加です。「既存呼び出しの結果は変わらない」と「新しい入力で新しい結果へ到達できる」が混同されています。

**推奨修正**: 「既存呼び出しの挙動変化なし。新引数指定時は対応する Section 属性を反映する」と書き分けてください。

### [🟡 Minor] 警告ゼロ確認がコンパイル未実行でも成立し得る

**該当箇所**: [tasks.md:9](kasane/changes/harden-compose-settingsroot-dsl/tasks.md:9)

**問題点**: `compileDebugKotlin` が `UP-TO-DATE` またはキャッシュ復元になった場合、警告が出ないことではなく、コンパイラが実行されなかったことしか確認できません。

**推奨修正**: `--rerun-tasks` を付け、対象コンパイルタスクが実行されたことも記録した上で、KT-81567 の診断文が0件であることを確認してください。

## アクションプラン

1. 文字列ヘッダ版に `footer` を追加するか、対称性の契約を狭めるか決定する。
2. DSL スコープ制御を規範的契約として維持する範囲と検証方法を確定する。
3. 位置引数テスト、全 Android テスト、警告確認の再実行条件を tasks に追加する。
4. 対象コードの既存コメント規約違反を tasks の範囲へ含める。
5. proposal の Impact を既存呼び出しと新機能に分けて修正する。

## 突き合わせ結果 (ホスト側自己レビューとの照合、2026-08-22)

ホスト側の自己レビュー (ksn-propose Step 8、2 周) は整合性チェックリストを通過しており、以下はすべて相方のみの指摘。根拠で採否を判定した。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1 | 文字列ヘッダ版に `footer` が無く「Section と同じ属性」と矛盾 | **採用 (設計判断はユーザーへ)** | `SettingsRootScope.kt:64` に `footer` 無しを確認。iOS 文字列版 `ksSection` は `footer: String?` を受ける (`SectionBuilder.swift:84`)。spec の主張と現行 API の事実が食い違うのは相方の言うとおり |
| 2 | スコープ制御の SHALL が検証不能 | **採用** | Scenario がリフレクション確認のみで SHALL を裏付けない。Requirement を `@Target` + receiver marker 維持 + 警告 0 件に狭め、スコープ制御は非規範の補足へ移動。手動実証は両 DSL 系統 (2 経路) に拡張。拡張関数は外側 receiver から暗黙に呼ばれ得る入れ子が DSL 上に無いため対象外と明記 |
| 3 | 引数順の SHALL をテストが固定しない | **採用** | 名前付き引数のみでは順序誤りが通る。位置引数 Scenario と tasks 3.5 を追加 |
| 4 | comment-policy lint の完了条件が現行コードと両立しない | **降格 (Major → Minor) のうえ採用** | `python3 scripts/comment-policy-lint.py` を実測: 禁止 0 件 / 669 ファイル — 「lint が blocking 検出する」は事実誤認。ただし規約本文の適用契機「既存コメントに触れる実装をするとき」には該当し、指摘の 3 箇所は規約の禁止類型 (change-id 裸参照 / 「本提案・後続提案」/ デルタスペック裸参照) に一致するため、触るファイル内の書き換えを tasks 2.4 として追加 |
| 5 | 完了テストが test-execution 規約を満たさない | **採用** | `test-execution.md:69-76` が `android/` 直下 `./gradlew test`、`--rerun-tasks`、XML 集計を要求。tasks 3.8 を規約どおりに改めた |
| 6 | Impact の「挙動変化なし」が不正確 | **採用** | 既存呼び出しと新入力を書き分けた |
| 7 | 警告 0 件確認が UP-TO-DATE でも成立 | **採用** | tasks 1.3 と Scenario に `--rerun-tasks` と「タスクが実行されたこと」を追加 |

確定 0 / 採用 7 (うち降格 1) / 未解決 0 (#1 の設計判断はユーザー決定待ち)。矛盾による再提示は不要。

追記 (2026-08-22): #1 はユーザー決定で「文字列ヘッダ版に `footer: String? = null` を追加し iOS 文字列版 `ksSection` と揃える」に確定。spec (Requirement 本文・Scenario 2 件追加/改訂)・tasks 2.2 / 3.3 / 3.5・proposal What Changes・exploration 決定事項に反映済み。未解決 0。
