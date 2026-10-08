# Exploration: pr-template-guidance-as-comment

## 課題 / 動機

起票元: ../KsDialogs/.github/pull_request_template.md

**PR 雛形の「記入の仕方」を HTML コメントにして、リリース PR の本文に案内の節が残らないようにした**

### 何が起きたか / 何を変えたか

KsDialogs の PR 雛形では「記入の仕方」(`## Changes` の書式・種別・記入例の案内) が見出しの節だった。リリース PR の本文を雛形から始めると毎回この節を消す必要があり、消し忘れると PR にそのまま表示された (0.1.0-beta.3 のリリース PR で発生。オーナーから「毎回発生する」との指摘)。

直し方 (commit `022c3fa`):

- 案内を見出しの節から、`## 概要` の後ろの HTML コメント (`<!-- 記入の仕方 ... -->`) へ移した。コメントは PR に表示されず、本文から消さなくてよい
- Release ノートの組み立て (`scripts/release/build-release-notes.py`) は `## Changes` の見出しの直後から次の見出しまでしか読まないので、`## Changes` より後ろの節にあるコメントは影響しない。案内のコメントを `## Changes` の範囲に置くと、認識できない行として release が止まるため、置き場所は後ろの節に限る
- リリース手順 (`kasane/handbook/cross/release-procedure.md` の「2. リリース PR」) に「本文は雛形から始め、`## Changes` と `## 概要` の中身だけを書く。案内を見出しの節として本文に書き写さない」を、release スキル (`.agents/skills/release/SKILL.md`) に「雛形に無い節を本文に足さない」を加えた

### 相手に関係する理由

`.github` は overlap に入っており、KsSettingsView の `.github/pull_request_template.md` も同じ形 (`## 記入の仕方` が見出しの節) を持つ。同じ Release ノートの組み立ての仕組みを使っていれば、リリース PR で同じ消し忘れが起きる。

### 提案する対応

KsSettingsView の雛形でも「記入の仕方」を `## Changes` より後ろの節の HTML コメントへ移すことを検討してほしい。組み立てのスクリプトが `## Changes` の範囲だけを読む作りであることは、KsSettingsView 側の実装で確かめてから適用するのがよい。リリース手順・リリース用スキルを持っていれば、参照箇所の言い回しも合わせて見直す。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
