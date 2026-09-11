---
kind: rule
applies-when:
  always: false
  paths: ["README.md", "README_ja.md", "skills/**"]
  tasks: [利用者向け Skill の追加, インストール手順の記述]
title: インストール例の契約
description: README 2 枚と利用者向け Skill のインストール例が守る形 — version のプレースホルダ・SwiftPM の `exact:`・最新リリースへの案内・英日の同一構成と、その機械検査
timestamp: 2026-09-11
---

# インストール例の契約

この文書は、ルート README 2 枚 (`README.md` / `README_ja.md`) と利用者向け Skill (`skills/{en,ja}/kssettingsview-*/SKILL.md`) に載せる依存宣言が守る形を定める。読むと、インストール例を書くとき・新しい Skill を足すときに何を満たせばよいかが分かる。

インストール例は具体的な version を持たない。リリースはこれらの行を書き換えず、最新版の案内は GitHub Releases に委ねる ([cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md))。

## 守る形

- **version はプレースホルダ `{version}` で書く。** 3 経路 (SwiftPM / Maven / NuGet) すべてで構文としては合法だが、どこでも version として解決できない。埋め忘れは依存解決の失敗として利用者に必ず露見する
- **SwiftPM の依存宣言は `exact:` で書く。** `from:` は下限が prerelease であれば prerelease も解決するが、上限が次のメジャー version まで開くため特定の版に固定されない
- **最新リリースへの案内を各ファイルに置く。** 案内先は `https://github.com/kamusoft/KsSettingsView/releases/latest` とする。この位置は常にその時点の最新リリースへ解決される
- **英語版と日本語版は同じ構成を持つ。** 同じ Skill の集合を持ち、対応する Skill は同じ種類・同じ本数のインストール宣言を持つ

宣言の種別は 3 つで、どのファイルがどれを持つかは決まっている。README 2 枚は 3 種すべてを 1 行ずつ持ち、Skill は自 platform の 1 種だけを持つ (iOS は SwiftPM、Android は Maven、MAUI と AiForms 移行は NuGet)。

## 新しい利用者向け Skill を足すとき

`skills/{en,ja}/` に Skill を足したら、検査の対象表 (`scripts/install-example-lint.py` の `TARGET_FILES`) にも英日 2 行を登録する。インストール宣言を持たない Skill であっても、対象表に無い Skill が実在する状態は検査が失敗する — 表への登録漏れをそこで捕まえるためである。

## 機械検査

検査は `scripts/install-example-lint.py` 1 本で、検証 CI の lint job が走らせる。

- **検査** (`python3 scripts/install-example-lint.py`) — 上の 4 つと、対象表と `skills/{en,ja}/` の実構成の突合を行う。違反はファイルと行を添えて出力する
- **自己テスト** (`--selftest`) — 各検査項目が違反を検出できることを確かめる。検査が退化して無音になっても平時は緑のままなので、検出力そのものを別に確かめる

検査が見るのはコードブロック内の宣言だけである。散文に書いた説明 (プレースホルダの読み方・prerelease の選び方) は対象にならないため、そちらの記述が契約と食い違っていても検査は通る。**検出 0 件は適合の証明にならない** — 散文と宣言が噛み合っているかは、書いた人とコードレビューが読んで判定する。

## 関連

- [リリース手順](release-procedure.md) — リリースはインストール例を書き換えない
- [利用者向け Skill の API 掲載基準](user-skill-api-listing.md) — `skills/` に何を載せるかの基準
