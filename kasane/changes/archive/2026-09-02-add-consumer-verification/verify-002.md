# Verify 002: add-consumer-verification (再検証)

- 実施日: 2026-09-02
- 対象: verify-001.md で **INVALID** の根拠にした未記録差分 2 件の解消確認
- 判定: **VALID**

## 前提

verify-001.md の対応表 (consumer-verification 9 Requirement / 18 Scenario、verification-ci 4 Requirement / 11 Scenario) を**そのまま継承**する。本再検証は `deviation.md` の追記のみを対象とし、コード・スクリプト・workflow・spec には変更が無いことを確認したうえで、判定を更新する。

## 変更の確認

`git diff --stat HEAD` の実装側 4 ファイル (`.github/workflows/ci.yml` / `android/build.gradle.kts` / `android/kssettingsview/build.gradle.kts` / `kasane/config.yaml`) は verify-001 実施時と同一の差分規模で、内容も一致した。

- `kasane/config.yaml:78` — `allow: ["@kamusoft.jp", "repo.local"]` (verify-001 時と同一)
- `android/kssettingsview/build.gradle.kts:173-177` — `GradleException` のメッセージは「リリース版の version は `-Pversion=<version>` で注入する (cross/ADR-0020)」のまま (verify-001 時と同一)

新規・削除ファイルも `verify-002.md` を除いて増減なし。**コードの変更は無い。**

## 未記録差分の解消

| # | verify-001 の指摘 | 現在の記録 | 状態 |
|---|---|---|---|
| ❗1 | `kasane/config.yaml:78` の `lint.identity.allow` への `repo.local` 追加が tasks・deviation のいずれにも無い | `deviation.md` 末尾に `[付随修正] kasane/config.yaml` として追記済み。誤検出の対象 (`second-opinion-code-001.md` の `maven.repo.local`)・identity-lint が mDNS ホスト名と解釈すること・ホスト名でない旨をコメントで注記したことまで記録されている | ✅ 解消 |
| ❗2 | `android/kssettingsview/build.gradle.kts` の `GradleException` メッセージ改訂が deviation の 2 件目の文言に含まれない | 2 件目の付随修正の記述に「SNAPSHOT で Central 向けタスクを実行したときのエラー案内 (カタログの version を変更する旨) を『リリース版の version は `-Pversion=` で注入する』に改めた (1 件目の version 注入の追随。second-opinion-code-001 の Minor を採用)」が追加済み | ✅ 解消 |

`deviation.md` の記録形式は ksn-core `references/delta-spec.md` の `[付随修正] <箇所>: <何を直したか>。理由: <一言> (YYYY-MM-DD)` に沿っており、4 件とも箇所・内容・理由・日付を備える。ksn-core「付随修正」の同梱条件との整合も確認した — ❗1 は 1 ファイル・公開 API とデータスキーマに触れず、lint の誤検出回避という判断を要さない修正で、同梱条件に収まる。

## 再実行した検査

`deviation.md` の追記が既存の検査に影響しないことを確認した。

- `python3 scripts/local-path-lint.py` → exit 0
- `python3 scripts/identity-lint.py` → exit 0 (追記した `deviation.md` の本文に `maven.repo.local` は現れず、allow 依存は増えていない)
- `python3 scripts/readme-example-lint.py` → exit 0

verify-001 で実施したその他の検査 (負ケース 18 通り、Android dry-run、`-Pversion=` 注入、workflow の YAML 構造、逆流検査、tasks.md の虚偽チェック) は、対象ファイルに変更が無いため再実行していない。

## 判定

**VALID**

- ❌ (Scenario の欠落・乖離): 0 件
- 未記録の差分: **0 件** (verify-001 の ❗1 / ❗2 とも `deviation.md` に記録済み = 合意済みの差分)
- 虚偽チェック: なし
- 逆流: なし (足場は commit `72df5b3` 以降未変更)
- 実行した検査: すべて成功

verify-001 で ⏸ とした 7 Scenario (smoke の公開レジストリ解決、CI 起動の実証 3 件、artifact の CI 経路、マージ保護 2 件) は、spec / proposal / tasks が phase-8 またはオーナー作業へ送ることを明記した合意済みの未実施であり、判定に影響しない。tasks 5.6 / 5.7 / 6.1 が未チェックのまま残っている点も同様。

## 申し送り (判定外)

verify-001 の「気づき」2 件は解消していない (いずれも spec の要求ではない)。

- `verification/maui/check-dependencies.py` に `--selftest` が無く、版一致検査の回帰検出手段が evidence の手動実行だけになっている
- `verification/` が未追跡のうちは `identity-lint` / `comment-policy-lint` のローカル実行がこのディレクトリを走査しない (コミット後は対象に入る)
