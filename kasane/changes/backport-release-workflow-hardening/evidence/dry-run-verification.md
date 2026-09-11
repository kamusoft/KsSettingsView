# dry-run 検証の証跡 (tasks 5.2)

## 実行

| 項目 | 値 |
|---|---|
| 実行 | https://github.com/kamusoft/KsSettingsView/actions/runs/34593366761 |
| 日付 | 2026-09-11 |
| 対象 SHA | `0871a573d90167bba5619118017ea8e4630fd7fa` |
| ブランチ | `verify/dry-run-backport-hardening` (検証専用。`develop` へはマージしない) |
| version | `0.1.0-beta.3` (monorepo・SwiftPM 配信リポジトリとも未使用) |
| dry-run | true |

## 検証対象の差分

対象 SHA は次の 2 つを含む。

1. `feat(release): publish の待機と deployment 引き継ぎを堅牢化する` — 本 change の実装
2. `chore(release): dry-run 検証のためインストール例を 0.1.0-beta.3 に揃える` — **検証のためだけの差分**。validate の `Verify install examples` が version の一致を要求するため、`AGENTS.md` が認める機械置換 (`scripts/release/set-readme-version.py`) で README 2 枚と Skill 8 枚の計 14 行を揃えた。本 change の成果物ではない

したがって本 run が検証したのは「本 change の実装 + インストール例の version 更新」の組み合わせであり、後者は release 手順に元から存在する操作である。

## 結果

| job | 結果 |
|---|---|
| validate | success |
| ios / verify | success |
| android / verify | success |
| maui / verify | success |
| package-ios | success |
| package-android | success |
| package-maui | success |
| consumer-ios / verify | success |
| consumer-android / verify | success |
| consumer-maui / verify | success |
| publish | skipped |
| wait-for-registries | skipped |
| smoke-ios / smoke-android / smoke-maui | skipped |

overall: **success**

`dry-run: true` により publish 以降は実行されない (`release.yml` の設計)。配信先への書き込み (tag・NuGet・Maven Central・GitHub Release) は一切発生していない。

## この検証が確認したこと / 確認していないこと

**確認した**: 本 change の変更 (publish job の待機と step 順序、`ci.yml` の lint job) が、公開前の経路 — validate・本体検証・配布物の生成・消費者からの解決 — を壊していないこと。

**確認していない**: publish job 内部の挙動 (Maven Central の公開待ちの上限分離、レジストリ照会の状態分類、deployment ID の引き継ぎ順序)。これらは `dry-run` では publish job ごと skip されるため到達しない。proposal の Impact が「待機の挙動は `dry-run` では確認できない。自己テストで分類ロジックを検査し、経路全体は次回の本番リリースで確かめる」として認識済みのリスクである。
