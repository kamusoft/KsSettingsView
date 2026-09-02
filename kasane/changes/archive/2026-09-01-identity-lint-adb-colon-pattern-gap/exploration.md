# Exploration: identity-lint-adb-colon-pattern-gap

## 課題 / 動機

`scripts/identity-lint.py` の Android シリアル検出が `serial[=:]` と `adb ... -s <値>` の 2 形式のみで、`adb: <値>` のような表記 (コロン区切りのラベル形式) を検出しない。また lint は `git grep` ベースで追跡ファイルのみを見るため、untracked の `kasane/changes/<id>/` 配下に入った実値を検出できない。

発見の文脈: entrycell-keyboard-avoidance-check の review-002 / review-003 (2026-08-24)。`evidence/evidence.md` に `adb: <android-serial>` 形式の実機シリアル断片が入ったが identity-lint は 0 件のままだった (手動レビューで検出・削除済み)。

## 検討した選択肢 (却下案と理由を含む)

- ラベル形式の検出: `adb[:：]\s*<値>` パターンを追加し、既存の値検証 (大文字+数字 8〜24 桁 / UUID) を通す — 採用。誤検出リスクが低く、hook / lint / log-sanitize が判定を共有するため 1 箇所で全経路が直る
- untracked 対応:
  - `git grep --untracked` フラグ追加 — 採用。1 フラグで済み、scope / exclude / .gitignore の既存挙動を維持し、依存も増えない
  - ripgrep への移行 — 却下 (パス列挙・出力形式の作り直しと ignore 解釈の再検証が必要で、依存も増える)
  - Python 自前 walk — 却下 (実装量最大で独自実装ぶんズレやすい)

## 決定事項

- 2026-08-25: Kasane 本体 (`../Kasane/skills/ksn-init/scripts/identity-lint.py`) で修正済み
  - `adb[:：]\s*<値>` ラベル形式の検出を追加 (全角コロン対応、GREP_PATTERN の事前絞り込みも追随)
  - lint モードの `git grep` に `--untracked` を追加 (untracked の kasane/changes/ 配下も検査対象に)
  - 付随修正: `local-path-lint.py` にも同じ `--untracked` を追加 (同根の検出漏れ)
  - `ksn-core references/evidence.md` の検出形式一覧に `adb:` を追記
- 本リポジトリの `scripts/identity-lint.py` / `scripts/local-path-lint.py` に再配置済み。実リポジトリで両 lint クリーン (誤検出の増加なし) を確認

## ADR 候補 (作成済み: なし / 未起票: なし)

なし (lint パターンの追加のみで、覆すコストの高い判断はない)。

## 未決の論点

なし (解消済み)。

## UI 素材 (ui/references/ の一覧と注釈)

なし。

## 変更級の推奨: S / M / L (理由)

S (lint スクリプトのパターン追加+フラグ 1 つ。公開 API 変更なし・可逆)。実装完了につきアーカイブ可。
