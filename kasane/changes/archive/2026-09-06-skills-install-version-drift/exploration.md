# Exploration: skills-install-version-drift

## 課題 / 動機

docs-refresh (2026-09-06、fix-default-colors-dark-appearance の蒸留後の追従更新) の実行中に、`skills/{en,ja}/kssettingsview-maui/SKILL.md` と `skills/{en,ja}/kssettingsview-aiforms-migration/SKILL.md` の導入節にある `PackageReference` の version が、実在しない `0.1.0` のままだったことが分かった (README 2 枚と `maui/api/maui-facade.md` の正は `0.1.0-beta.1`。prerelease は明示指定でしか解決されないため、コピーしても restore できない状態だった)。今回の追従で `0.1.0-beta.1` に揃えた。

探索 (2026-09-06) で実物を確認したところ、腐っているのは MAUI 系だけではなかった:

- `skills/{en,ja}/kssettingsview-ios/SKILL.md` の導入節は `.package(url: ..., from: "0.1.0")` のまま (`from:` は prerelease を解決しないので restore できない)
- `skills/{en,ja}/kssettingsview-android/SKILL.md` の導入節は `implementation("jp.kamusoft:kssettingsview:0.1.0")` のまま (存在しない version)

対象は 4 Skill × 2 言語 = 8 行。うち iOS / Android の 4 行は今も貼っても動かない。

構造的な原因: リリース手順の機械置換 (`scripts/release/set-readme-version.py`) は README 2 枚 × インストール例 3 行しか対象にせず (AGENTS.md の例外規定、phase-8 の 2026-09-03 裁定)、skills/ 側の version はリリースごとに再び腐る。

## 検討した選択肢 (却下案と理由を含む)

- **(a) 置換 script の対象に Skill 4 本 × 2 言語の導入例を足し、AGENTS.md の例外規定を広げる** — 採用。Skill はエージェントが読んで貼る前提の文書で「貼ってそのまま restore できる 1 行」を保つ価値が README より高い。release PR で同じ 1 コマンドに乗り、validate job の `--check` が 8 行も検査して更新忘れを publish 前に止める。en/ja を同時に同じ値へ置換するので docs-refresh 6-③ (コードブロック byte 一致) は保てる
- **(b) skills/ から具体 version を落とし「最新版を参照」の記述にする** — 却下。prerelease 期間は `exact:` / 明示 version が要るため、利用者 (とそのエージェント) が版を調べて書く手間が増える。README で「プレースホルダ + バッジ」案が取り下げられたのと同じ理由
- **(c) リリースごとに docs-refresh を回す** — 却下。phase-8 (2026-09-03) で README 向けに「重すぎて不可」と裁定済みで、skills/ でも同じ

## 決定事項

- 方向は (a) (オーナー承認 2026-09-06)
- 対象ファイルは README 2 枚 + `skills/{en,ja}/kssettingsview-{ios,android,maui,aiforms-migration}/SKILL.md` の 8 枚。各 Skill ファイルは自 platform の行だけを持つ (iOS: SwiftPM 1 行 / Android: Maven 1 行 / maui・aiforms-migration: NuGet 1 行) ので、「各ファイルで期待する対象がちょうど 1 行」の前提はファイルごとの期待対象集合として持つ
- SwiftPM の行は README と同じく置換後 `exact:` に揃える (現状の `from:` も `exact:` へ)
- AGENTS.md の例外規定 (「触れるのは 2 枚 × 3 行」) を「README 2 枚 × 3 行 + Skill 4 本 × 2 言語 × 1 行」に広げる。README の例外が ADR ではなく AGENTS.md に置かれているのに揃え、ADR-0022 は改訂しない
- 拡張後の script を同じ change の中で `0.1.0-beta.1` で一度走らせ、今腐っている iOS / Android の 4 行を直し切る (隣接課題は同じ change で直す)
- `--selftest` の fixture も Skill ファイル分を足す (ci.yml が selftest を呼んでいる)

## ADR 候補 (作成済み: なし / 未起票: なし)

既存の例外規定を同じ性質で広げるだけで、覆すコストは低く、ドメイン境界も越えないため ADR 対象外。出典として phase-8 agenda (2026-09-03 の README 裁定) とこの探索メモで足りる。

## 未決の論点

- なし (docs-refresh manifest の `lastUpdatedFiles` は docs-refresh 自身の実行記録なので、script による置換では更新しない — README の置換でも同じ扱い)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S

理由: 触るのは script 1 本 (対象一覧・selftest) と AGENTS.md の規約文 1 行、それに script を走らせた結果の 8 行 (うち 4 行は値の修正)。公開 API 変更なし、UI なし、可逆。デルタスペックは不要で、独立レビューのみで足りる。
