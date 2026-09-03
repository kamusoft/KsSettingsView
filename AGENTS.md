# AGENTS.md

## 全体ルール

- 使用言語は日本語とする。（ソースのコメントも日本語とする）
- 削除コマンドは `rm` ではなく `trash` を使用すること。

## 開発ハーネス

このプロジェクトは Kasane (ksn-*) で運用する。
- 探索: ksn-explore / 提案: ksn-propose / 実装: ksn-orchestrator / 蒸留: ksn-distill / 棚卸し: ksn-drift
- 規約: ~/.claude/skills/ksn-core/SKILL.md、プロジェクト設定: kasane/config.yaml
- 他の SDD 系スキル (openspec-* 等) はこのプロジェクトでは使用しない
- 長命層は規範と記述で分かれる: 従うべき規約・手順は `kasane/handbook/`（作業前に `handbook/index.md` の「適用のきっかけ」で担当範囲に当たるものを読む）、今どうなっているかの記述は `kasane/concepts/`
- `skills/` は**ライブラリ利用者向けドキュメント (Agent Skills)** であり、エージェントは開発時の知識参照先にしない（知識の正は kasane/handbook/ と kasane/concepts/ とコード・テスト。skills はそこから利用者向けに翻訳した派生物で、古い記述が残り得る）
- `skills/` と README 群の**継続的な追従更新**は `docs-refresh` スキル経由でのみ行う（自動発動禁止。concepts 更新後にユーザーが明示的に依頼する）。初期生成・構成の見直しは承認済み change の実装として行う。スキル本体は `.agents/skills/docs-refresh/SKILL.md`（`.claude/skills/docs-refresh` は symlink）
- 例外として、README 2 枚のインストール例の version は `scripts/release/set-readme-version.py <version>` による機械置換で更新してよい（リリース PR の手順。触れるのは 2 枚 × 3 行のインストール例の行だけで、他の文面には触れない。SwiftPM の依存宣言は `from:` で書かれていても、prerelease を解決させるため version とあわせて `exact:` に揃える）
- `openspec/` は歴史資料として凍結する（編集禁止）。新規の変更は `kasane/` で行う
