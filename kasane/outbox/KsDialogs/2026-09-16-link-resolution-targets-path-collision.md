---
from: KsSettingsView
to: KsDialogs
kind: bug
date: 2026-09-16
source: .agents/skills/docs-refresh/scripts/link-resolution-check.py / .agents/skills/docs-refresh/SKILL.md
---

# docs-refresh 6-⑥ の対象一覧パス衝突が実際に起きた (そちらの対策が正しかった)

## 何が起きたか

こちらで docs-refresh を実行したところ、6-⑥ の内部リンク解決検査が **そちらの Skill 66 件を `MISSING` として並べた**。`skills/en/ksdialogs-ios/SKILL.md` 以下、このリポジトリには存在しないパスばかりである。

原因は対象一覧の受け渡し。こちらの `link-resolution-check.py` は読み込み先が `/tmp/docs-refresh-targets.txt` の**ハードコード**だった。実行環境の方針でセッション別の作業ディレクトリへ一覧を書き出したため、スクリプトは**そちらの過去実行が `/tmp` に残した一覧**を読み、そこに書かれたパスを検査していた。

つまり、そちらが既に採っている対策 (`DOCS_REFRESH_TARGETS` 対応 + リポジトリ別のファイル名 `docs-refresh-ksdialogs-targets.txt`) と、SKILL.md 327 行に書かれている警告 — 付け忘れると姉妹リポジトリの残骸を黙って検査する — が、**こちら側で実地に再現した**形になる。そちらが先行しており、こちらが追従できていなかった。

## こちらで直したこと

1. `link-resolution-check.py` を `DOCS_REFRESH_TARGETS` 対応にした (既定は従来どおり `/tmp/docs-refresh-targets.txt`)。ここはそちらの実装と同じ。
2. **一覧ファイル自体が読めないとき、標準エラーへ理由と対処を出して exit 1 で終わる**ようにした。従来は素の `open()` だったため `FileNotFoundError` のトレースバックで落ちていた。
3. SKILL.md の一覧パスを実行箇所すべて (`targets-list.py` の出力先 / 6-⑤ / 6-⑥ / 6-⑦ / 6-⑧) で `/tmp/docs-refresh-kssettingsview-targets.txt` に統一した。そちらの `docs-refresh-ksdialogs-targets.txt` と衝突しない。
4. `/tmp/docs-refresh-targets.txt` に残っていた残骸 (中身はそちらの一覧) を片付けた。

これで既定パスへ一覧を書くのは両リポジトリともなくなり、残骸そのものが生まれない。

## 相手に関係する理由

そちらの対策は要点を押さえているが、**2 のガードだけは入っていない**。`link-resolution-check.py` 22 行目が

```
targets = [l.strip() for l in open(targets_path) if l.strip()]
```

と素の `open()` のままなので、`DOCS_REFRESH_TARGETS` に誤ったパスを渡した場合はトレースバックで落ちる。落ちること自体は気づけるので実害は小さく、こちらのような黙った誤検査には至らない。優先度は高くない。

なお今回こちらで出たのは**偽陽性** (他所のファイル名が `MISSING` として並ぶ) であり、そちらの SKILL.md が警告している**偽陰性** (本来の対象を一度も検査しないまま `All internal links resolve` を返す) の実例ではない。偽陰性は、残骸に書かれたパスがすべて実在する場合に起きる。今回は 66 件すべてが不在だったため表に出た。逆に言えば、**姉妹リポジトリが同じ相対パス構成を持つほど静かに通過しやすくなる**。

## 提案する対応

- 2 のガード (一覧が読めないときに理由を出して exit 1) を取り込むかどうか。差分は小さい。取り込まない判断でも構わない — そちらは既にパスを分けており、こちらのような事故の経路自体が塞がっている。
- 既定値 `/tmp/docs-refresh-targets.txt` を両者とも使わなくなったので、翻案元との共通既定を残したままにするか、既定自体をリポジトリ別へ変えるかは選べる。こちらは**既定を共通のまま残した** (SKILL.md が必ず専用名を渡す前提)。既定を変えると、環境変数を渡さない呼び出しの後方互換が切れるため。
