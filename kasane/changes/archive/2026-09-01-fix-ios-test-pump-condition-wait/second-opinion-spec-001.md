# セカンドオピニオン: fix-ios-test-pump-condition-wait (spec-001)

**相方**: codex / **label**: so-spec-fix-ios-test-pump-condition-wait / **日付**: 2026-09-01 / **対象**: 提案一式 (proposal.md / specs/ios-test-support/spec.md / tasks.md / exploration.md)

---

# レビュー結果: fix-ios-test-pump-condition-wait

**判定**: `NEEDS_DISCUSSION`
**指摘件数**: Critical 0 / Major 6 / Minor 2 / Suggestion 0

## サマリー

`pump` については、提案記載どおり「221 マッチ＝定義 19・呼び出し 202、30 ファイル」であることを静的に確認しました。一方、検索対象外の同型固定待機が残っており、分類台帳・完了条件・共有方式・deadline・再現検証も未確定です。このままでは全数移行を検証できず、条件待機へ置き換えても別の早期 return を作る可能性があります。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`（テスト変更・結果報告）
- `kasane/handbook/cross/runtime-behavior-verification.md`（タイミング起因不具合の完了判定）
- `kasane/decisions/cross/0027-negative-verification-fixed-wait-exception.md`
- `swift-ui-impl-skill` の Swift Concurrency・テスト衛生観点

## 指摘事項

### [🟠 Major] `pump(` 以外の同型固定待機がスコープから漏れている

**該当箇所**: specs/ios-test-support/spec.md:45、ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:334,418,1071、ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:922

**問題点**: `pumpEntry` は旧 `pump` と同じ固定待機であり、表示更新を待つ正の検証にも使われています。また、first responder の反映と Cell 生成を待つ直接の `RunLoop.current.run(until:)` もあります。したがって、202 箇所だけを置換しても「非同期反映の検証に固定時間待機を使わない」という Requirement は満たせません。

**推奨修正**: 名前ではなく `RunLoop.current.run(until:)` など固定時間待機の実装パターンから全数調査し、`pumpEntry` の2呼び出しと直接待機も A/B/C 分類へ追加してください。件数・影響範囲・tasks も更新してください。

### [🟠 Major] A/B/C の全数仕分けが永続化されておらず、実装・検証できない

**該当箇所**: tasks.md:3、exploration.md:44、specs/ios-test-support/spec.md:49

**問題点**: 仕分けの正が、change 外の scout 応答とセッションラベルだけに置かれています。実装者・レビュアー・検証者は、どの呼び出しが156件の A、16件の B、30件の C なのか再現できません。件数が一致しても誤分類や入れ替わりを検出できません。

**推奨修正**: change 内に分類台帳を保存してください。各行には、ファイル・テスト名またはヘルパ名・分類・待つ遷移・述語候補・負の検証理由・deadline 方針を記載し、tasks と Scenario はこの台帳を参照する形にしてください。

### [🟠 Major] 「直後の assert から述語を逆算」は早期 return を作る

**該当箇所**: tasks.md:15、exploration.md:43、ios/Tests/KsSettingsViewUITests/FullSnapshotContentRefreshTests.swift:131、ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:45

**問題点**: `FullSnapshotContentRefreshTests` では `pump` 直後の最初の assert が Cell identity の維持です。identity は更新前から成立しているため、これを述語にすると即時 return し、タイトル `"A2"` の反映を待てません。また `KsBridgeTestHost.attach` など、`pump` の直後が assert ではなく `return` の呼び出しも多数あります。

**推奨修正**: 述語は「操作前には成立せず、対象の非同期反映後に成立する遷移証拠」に限定してください。この例では `"A2"` の表示を待ち、identity は後続の不変条件として残します。setup ヘルパ内の無条件待機は、呼び出し側へ移すか明示的な完了述語を受け取る構造を決めてください。

### [🟠 Major] 共有ヘルパの配置が未決のまま、相反する実装を許している

**該当箇所**: proposal.md:17、exploration.md:68、tasks.md:7

**問題点**: proposal は18コピーを「集約」するとしていますが、tasks はリンクできなければ3コピーへ戻すことを事前に許しています。既知の設計分岐を実装時の deviation に委ねており、集約という成果が受け入れ条件になっていません。

**推奨修正**: 実装前に共有方式を確定してください。少なくともターゲット種別、配置、依存関係、XCTest failure の発生方法、`@MainActor` 境界を定め、3コピーを許すなら proposal の集約目標を修正して重複防止策を仕様化してください。

### [🟠 Major] deadline の選定契約がなく、長時間指定箇所を再び flaky にできる

**該当箇所**: specs/ios-test-support/spec.md:9、exploration.md:42、tasks.md:8

**問題点**: 明示的な `seconds:` 指定30件はすべて A で、現状は0.2～1.0秒ですが、新ヘルパの既定 deadline、呼び出しごとの上書き、旧値の扱いが決まっていません。どの値でも形式上は spec を満たすため、0.05秒へ一律化して再び flaky にする実装も排除できません。

**推奨修正**: 既定 deadline、上書き可能性、30件の移行方針を受け入れ条件に追加してください。経過時間の計測には壁時計ではなく単調増加時計を使うことも明記するのが安全です。

### [🟠 Major] flaky 解消 Scenario の実行条件と証跡が定義されていない

**該当箇所**: specs/ios-test-support/spec.md:53、tasks.md:33、kasane/handbook/cross/runtime-behavior-verification.md:17

**問題点**: 「実行機が混雑した環境相当」が観測可能な条件として定義されておらず、ローカルで10回通すことも同条件の再現にはなりません。また、タイミング不具合の規約が求める同一手順での修正前後確認と change 配下の証跡保存が tasks にありません。

**推奨修正**: 修正前に失敗させる手順と修正後の同一手順を定義し、A/B結果を evidence として残すタスクを追加してください。10回反復は補助確認とし、旧固定待機では落ち、新条件待機では通る遅延収束テストを決定的な回帰検証として用意してください。

### [🟡 Minor] no-op の Scenario が欠けている

**該当箇所**: specs/ios-test-support/spec.md:33

**問題点**: Requirement と `cross/ADR-0027` は負の検証を「no-op」と「不達」の2型に分けていますが、Scenario は不達だけです。未知ID・範囲外指定などの no-op が受け入れ対象として検証されません。

**推奨修正**: no-op 操作後に意図明示固定待機を経て表示・通知が変化しない Scenario を追加してください。

### [🟡 Minor] ADR 参照形式と status が不整合

**該当箇所**: tasks.md:10、exploration.md:64

**問題点**: ソースコメントへ `ADR-0027` と書く指示は、コメント規約の正式形 `<domain>/ADR-NNNN` に反します。また exploration は ADR を `proposed` としていますが、現ファイルは `accepted` です。

**推奨修正**: コメント指示を、意図が自己完結する説明＋`cross/ADR-0027` に直し、exploration の status を `accepted` に合わせてください。

## アクションプラン

1. `pump` 名に依存しない固定待機の全数台帳を change 内へ追加する。
2. 各 A に「操作前は偽、反映後に真となる遷移述語」と deadline を割り当てる。
3. 共有ヘルパ構成と failure 検証方法を実装前に確定する。
4. no-op Scenario、再現手順、修正前後の証跡タスクを追加する。
5. ADR 参照と status を修正後、提案を再レビューする。

ビルド・テストは依頼どおり実行していません。ファイル変更も行っていません。

---

## 突き合わせ結果 (2026-09-01、ホスト側)

ホスト側自己レビュー (指摘なしで通過) との突き合わせ。全指摘が相方のみだが、根拠を実物で検証して採否を決めた。

| 指摘 | 採否 | 対応 |
|---|---|---|
| Major 1: `pumpEntry`・直接 `RunLoop.current.run` のスコープ漏れ | **採用** (grep で実在確認: 定義 1 + 呼び出し 4) | スコープを実装パターン基準に変更、台帳へ A として追加。総数 206/A 160 に更新 |
| Major 2: 仕分けの永続化なし | **採用** | 分類台帳 triage.md を change 内に作成 (B 明細は scout に追加依頼して補完) |
| Major 3: 述語逆算の早期 return リスク | **採用** (FullSnapshotContentRefreshTests:131 で確認) | spec に「遷移証拠の述語」規則を追加、tasks の置換手順を書き換え、setup ヘルパの扱いを明記 |
| Major 4: 共有方式の未決 | **採用** | 共有 (単一定義) を受け入れ条件に昇格。成立しない場合は停止してユーザー報告 (黙ったフォールバック禁止)。確定項目 (failure 発火方式・@MainActor 境界) を tasks 1.1 に明記 |
| Major 5: deadline 契約なし | **採用** | spec に deadline 契約を追加 (単調時計・共通既定値 > 旧最長 1.0 秒・上書き可・旧 seconds: は統合) |
| Major 6: flaky 検証の実行条件・証跡未定義 | **採用** (runtime-behavior-verification.md と照合) | 決定的回帰検証 = ヘルパの遅延成立述語テスト (tasks 1.5)。evidence/ に修正前 CI run 記録 + 修正後の全件・反復実行記録を残すタスクを追加 (tasks 5.1〜5.3) |
| Minor 1: no-op Scenario 欠落 | **採用** | spec に no-op Scenario を追加 |
| Minor 2: ADR 参照形式・status 不整合 | **採用** | tasks を `cross/ADR-0027` 形式に修正、exploration の status 表記を accepted に修正 |

未解決・降格: なし。判定 NEEDS_DISCUSSION の論点は全件、足場修正で解消した。
