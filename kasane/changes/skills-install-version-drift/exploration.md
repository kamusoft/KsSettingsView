# Exploration: skills-install-version-drift

## 課題 / 動機

docs-refresh (2026-09-06、fix-default-colors-dark-appearance の蒸留後の追従更新) の実行中に、`skills/{en,ja}/kssettingsview-maui/SKILL.md` と `skills/{en,ja}/kssettingsview-aiforms-migration/SKILL.md` の導入節にある `PackageReference` の version が、実在しない `0.1.0` のままだったことが分かった (README 2 枚と `maui/api/maui-facade.md` の正は `0.1.0-beta.1`。prerelease は明示指定でしか解決されないため、コピーしても restore できない状態だった)。今回の追従で `0.1.0-beta.1` に揃えた。

ただしリリース手順の機械置換 (`scripts/release/set-readme-version.py`) は README 2 枚 × インストール例の行しか対象にしない (AGENTS.md の例外規定)。skills/ 側の version は次のリリースで再び腐る構造になっている。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- 対処の方向: (a) 置換スクリプトの対象に 4 枚の SKILL.md (en/ja × maui/aiforms) を足し、AGENTS.md の例外規定も広げる / (b) skills/ 側から具体 version を落とし「最新版を参照する」旨の記述にする (prerelease 期間中は `exact:` / 明示 version が要るため利用者の手間が増える)
- (a) の場合、docs-refresh の manifest (`lastUpdatedFiles`) と 6-③ コードブロック byte 一致検査 (en/ja) との関係 — 機械置換は両言語を同時に触るので一致は保てる見込み
- iOS Skill (SwiftPM の `exact:`) に同じ問題がないかの確認

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (スクリプトと規約文の修正のみなら S 級の見込み)
