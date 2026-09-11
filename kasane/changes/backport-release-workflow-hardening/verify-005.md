# 一致検証: backport-release-workflow-hardening (005 回目)

**日付**: 2026-09-11
**判定**: VALID

デルタスペック `kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md` の 4 Requirement / 11 Scenario を実装と突き合わせた。前回 (004 回目) 以降の変更は `scripts/release/deployment-handover.sh` と `.github/workflows/release.yml` の `Drop pending deployment` step の 2 箇所で、影響するのは Requirement「保留中 deployment の引き継ぎ」のみ。11 Scenario すべてが ✅ 一致で、❌ は 0 件。

## 対応表

### Requirement: Maven Central の公開待ち

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同期に時間がかかっても公開を待ち切る | `scripts/release/central-portal.sh:267` `cmd_wait_published` (上限 `KSR_PUBLISHED_TIMEOUT_SECONDS` 既定 5400 秒 = 90 分) | `central-portal.sh --selftest` の `wait-published` 群 (52 件に含まれる) | ✅ 一致 |
| 検証待ちと公開待ちの上限を別々に設定できる | `central-portal.sh:228` (`KSR_POLL_TIMEOUT_SECONDS` 既定 1800) / `:270` (`KSR_PUBLISHED_TIMEOUT_SECONDS` 既定 5400) の 2 変数 | 同 selftest (`:536` / `:543` / `:547` — 公開待ち用の 0 指定が `wait-validated` に効かないことを含む) | ✅ 一致 |
| 時間予算が待ちの最悪ケースと応答上限を収容する | `scripts/release/check-time-budget.py` (定数を読んで合計を算出) + `.github/workflows/release.yml:443` `timeout-minutes: 240` | `.github/workflows/ci.yml:199` で lint job が実行 (実測: 合計 217 分 / 上限 240 分) | ✅ 一致 |

### Requirement: 公開レジストリへの反映待ち

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未反映は待機を続ける | `scripts/release/wait-for-registries.sh:208` / `:259` (404 → `STATE_PENDING`)、`:370` `has_unreflected` | `wait-for-registries.sh --selftest` (61 件) の分類表と巡回検査 | ✅ 一致 |
| 照会できなかった場合も待機を続ける | `:206` / `:247` / `:252` / `:263` / `:273` (`unknown-transport` / `unknown-status` / `unknown-parse` の 3 種別) | 同 selftest の Maven / NuGet 分類表 | ✅ 一致 |
| 判定不能から回復すれば反映済みになる | `:337` `poll_once` (反映済みの対象を再照会しない) | 同 selftest (回復・再照会なしの検査) | ✅ 一致 |
| 対象ごとに状態が分かれて残る | `:306` `reset_targets` + `TARGET_STATE` 配列 (Maven 1 + NuGet 3 = 4 対象)、`:278` `state_label` | 同 selftest (混在巡回の検査) | ✅ 一致 |
| 上限超過時に対象ごとの最後の状態が分かる | `:408` 「対象ごとの最後の分類」の出力 (種別つき) | 同 selftest `:659` | ✅ 一致 |

### Requirement: 保留中 deployment の引き継ぎ

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 引き継ぎの読み込みが他の成果物の取得より先に行われる | `.github/workflows/release.yml:500` `Download previous deployment id` → `:510` `Inspect previous deployment handover` → `:517` `Download Android artifacts` の並び。機械検査は `scripts/release/check-publish-step-order.py` | `check-publish-step-order.py` (`ci.yml:202`、実測「順序正常」) と `--selftest` 7 件 (`ci.yml:207`) | ✅ 一致 |
| 引き継ぎが無い初回の実行と区別できる | `scripts/release/deployment-handover.sh:73` `cmd_inspect` (`loaded` / `present` / `deployment-id` の 3 キー。読み込み失敗は出力を出さず失敗) | `deployment-handover.sh --selftest` の `inspect` 側 11 件 (引き継ぎあり 3 / 初回なし 4 / 読み込み失敗 4) | ✅ 一致 |

今回の変更が触った `cmd_writeback` (`deployment-handover.sh:103`) と `Drop pending deployment` (`release.yml:872`) は、この Requirement の「引き継ぎを次の attempt へ渡す」動作を支える後始末側にあたり、直接対応する Scenario を持たない (spec の 2 Scenario は読み込み順序と初回の区別を問う)。Requirement 本文の SHALL「前の試行が残した保留中の Maven deployment を引き継ぎ、その決着から続ける」に対しては、`NOT_FOUND` を確認できたときだけ引き継ぎを断ち、それ以外では ID を残す挙動として適合している (`store` の枝が `cleared` を立てないため、`release.yml:908` `Clear stored deployment id` が動かないことを実測で確認)。

### Requirement: 待機スクリプトの自己テスト

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 待機スクリプトの自己テストが lint job で走る | `.github/workflows/ci.yml:187` `Registry wait script selftest` (`:184` Central Portal / `:190` Deployment handover と並ぶ) | 自己テスト自身 (`wait-for-registries.sh --selftest` 61 件) | ✅ 一致 |

## 追加検査

- **tasks.md の虚偽チェック**: なし。`[x]` の 15 項目はいずれも対応表の実装と一致する。`5.2` (`dry-run` で release を起動) のみ未チェックで、実態 (未実施) と一致している。`3.4` が要求する 5 状態は selftest の 4 見出し (`[引き継ぎあり]` / `[初回で引き継ぎなし]` / `[引き継ぎの読み込み失敗]` / `[読み込み後に別の成果物の取得が失敗]`) と `[後始末の後の引き継ぎの扱い]` の `store` 群に対応する。今回の変更で「読み込み後に別の成果物の取得が失敗」の検査が「現状維持を返す」から「判定を返さず失敗する」へ変わったが、3.4 の文面が求める「各失敗経路が既存の引き継ぎを空で上書きしない」は満たされている (実測で引き継ぎファイルが手つかずであることを確認)
- **逆流検査**: `specs/release-workflow/spec.md` / `proposal.md` / `exploration.md` は HEAD との差分なし。更新時刻も 16:00〜16:21 で、前回レビュー (19:01) 以降に触れられていない。`tasks.md` (17:44) / `deviation.md` (18:14) も同様。逆流なし
- **未記録乖離**: ❌ が 0 件のため該当なし
- **付随修正**: `deviation.md` の `[付随修正]` 2 件 (`ci.yml` への `deployment-handover.sh --selftest` 追加、`check-publish-step-order.py --selftest` 追加) はいずれも Requirement を持たない記録済みの同梱で、対応表の対象外。今回の変更 (`writeback` の 2 語化と `Drop pending deployment` の `case` 化) は Requirement「保留中 deployment の引き継ぎ」の範囲内の実装であり、新たな付随修正の記録を要しない
- **UI 変更**: なし (該当なし)
- **テストの全件成功**: 実行して確認した (下表)

## テスト実行結果

| 検査 | 結果 |
|---|---|
| `scripts/release/deployment-handover.sh --selftest` | 21 件 OK / 0 件 NG (終了コード 0) |
| `scripts/release/wait-for-registries.sh --selftest` | 61 件 OK / 0 件 NG (終了コード 0) |
| `scripts/release/central-portal.sh --selftest` | 52 件 OK / 0 件 NG (終了コード 0) |
| `python3 scripts/release/set-readme-version.py --selftest` | 33 件 OK / 0 件 NG (終了コード 0) |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 7 件 OK / 0 件 NG (終了コード 0) |
| `python3 scripts/release/check-time-budget.py` | 合計 217 分 / 上限 240 分 (終了コード 0) |
| `python3 scripts/release/check-publish-step-order.py` | 順序正常 (終了コード 0) |

`deployment-handover.sh --selftest` の件数は前回の 22 件から 21 件へ 1 件減っている。内訳は `inspect` 側 11 件 (引き継ぎあり 3 / 初回で引き継ぎなし 4 / 読み込み失敗 4) と `writeback` 側 10 件 (空 ID の契約違反 2 / 後始末の後の扱い 8) で、`writeback` の判定語が 3 語から 2 語になったことに伴う入れ替わり。件数の減少に伴う検査範囲の欠落が無いことは、ミューテーションで検出力を実測して確認した (`review-005.md` に記録)。
