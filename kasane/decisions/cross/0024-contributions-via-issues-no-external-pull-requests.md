---
id: 0024
title: 貢献は Issue で受け、外部からの Pull Request は受け付けない
status: accepted
date: 2026-08-29
---

## Context

public リポジトリへの移行 (cross/ADR-0021) にあたり、外部からの貢献をどう受けるかを決める必要が生じた。

- 動機は AI 生成の粗雑な提案 (AI スロップ) の流入防止と、レビュー負荷・コード品質の維持。
- 開発は Kasane ハーネスの change フロー (探索 → 提案 → 実装 → レビュー → 蒸留) で回っており、知識の正は `kasane/concepts/` とコード・テストにある。外部からの Pull Request は、このフローの外から実装だけが飛び込む形になり、探索・提案・ADR の文脈を持たないまま レビュー対象になる。
- GitHub は 2026-02 にリポジトリの Pull requests アクセス設定を追加し、**完全無効化**と **collaborators only** の 2 段階が選べるようになった。それ以前は自動クローズの workflow で代用するしかなかった。
- 一方 package-distribution ロードマップのゴールには「PR / push で 3 platform のビルド・テストを回す検証 CI」(phase-3) があり、オーナー自身が PR を作れることが前提になっている。
- `exploration.md` の構成は「課題 / 動機」「検討した選択肢」「決定事項」「ADR 候補」「未決の論点」「変更級の推奨」で、後半 4 つはオーナーの判断領域。`proposal.md` (Why / What Changes / Non-Goals / Impact / 級) はさらに内部寄りで、外部の投稿者には書けない。

## Decision

- **外部からの Pull Request は受け付けない**。GitHub の Pull requests 設定を **collaborators only** にする。完全無効化を採らないのは、オーナー自身の PR も作れなくなり phase-3 の PR トリガー CI が成立しないため。
- **貢献は Issue で受ける**。オーナーが Issue を巡回し、Kasane の change (`kasane/changes/<id>/`) に起こして対応する。
- **Issue テンプレートは用途別 3 本**を GitHub Issue Forms (`.github/ISSUE_TEMPLATE/*.yml`) で置く。Forms は項目を必須化できるが、必須にすべき項目が用途ごとに違うため 1 本にまとめない。(2026-08-30 改訂: 初版は「バグ報告 / 提案」の 2 本だった。下の Consequences が残した質問の行き先の欠落を、Discussions の有効化ではなく 3 本目のフォームで塞ぐと決めたため)
  - バグ報告: バージョン / platform / 再現手順 / 実際の挙動 / 期待した挙動 を必須にする。
  - 提案: 解決したい課題 / 現状どう困っているか / 考えた選択肢 を必須にし、`exploration.md` の「課題 / 動機」「検討した選択肢」へそのまま写る形にする。
  - 質問: バージョン / platform / 試したこと / 参照した Skill・README の箇所 を必須にする。
- **外部に求めるのは `exploration.md` の前半 2 節に対応する情報まで**。「決定事項」「ADR 候補」「未決の論点」「変更級の推奨」は起票時にオーナーが埋める。`proposal.md` 形式は外部に求めない。
- **AI スロップへの抑止は、書式の厳密さではなく実際に動かした証拠の必須化** (バージョン・再現手順・実際の出力) で効かせる。書式の厳密さはむしろ AI が埋めやすい方向に働く。
- **方針の表明先はルート README の「貢献」節 (3〜4 行) + `.github/CONTRIBUTING.md` (詳細)**。GitHub は `CONTRIBUTING.md` を Issue 作成画面・リポジトリ概要の Contributing タブ・サイドバーでリンクするため、README と合わせて投稿前に目に入る経路が 3 つになる。
- **言語**: Issue Forms は英語 1 セット (3 本)、`CONTRIBUTING` は英日 2 枚、投稿本文は英語・日本語どちらでもよいと明記する。(2026-08-30 改訂: 上のテンプレート 3 本化に本数を追随させた。1 セット = 英語のみという方針自体は変えていない)

## Alternatives Considered

- **Pull requests を完全に無効化する**: PR タブが消えて意思表示として最も明確。しかしオーナー自身も PR を作れなくなり、phase-3 の PR トリガー CI を push トリガーへ組み替える必要が出る。却下。
- **GitHub の設定は変えず README / CONTRIBUTING での表明にとどめる**: 来た PR を都度閉じる運用負荷が残り、投稿者は書き上げてから断られる。AI スロップ防止という動機に対して弱い。却下。
- **Kasane の生フォーマット (`exploration.md` / `proposal.md`) をそのまま埋めてもらう**: 後半 4 節はオーナーが埋め直すことになり埋めさせる意味が薄く、外部のハードルだけが上がって良い提案を逃す。却下。
- **Issue Forms を英日 2 セット (4 本) にする**: 日本語話者が最初から日本語のフォームを選べるが、テンプレート選択画面に 4 本並び、項目の同期を 2 倍維持することになる。実際に埋めるのは自由記述欄であり、本文の日本語を許容すれば書きやすさはほぼ損なわれない。却下。

## Consequences

- 正: 外部からの実装が change フローを迂回して入ることがなくなり、レビュー負荷とコード品質を制御できる。
- 正: Issue が `exploration.md` の前半へ機械的に写る形になり、Issue から change への起票コストが下がる。
- 正: 「実際に動かした証拠」を必須項目として要求できるため、動作確認を伴わない提案を入口で減らせる。
- 負: コードで直接貢献したい人の道が塞がれる。OSS としての参加障壁は上がる。
- 負: Issue の巡回とオーナーによる起票が滞ると、貢献が放置される。巡回の運用が前提になる。
- 負: collaborators only は「PR タブは見えるが作れない」状態で、完全無効化ほど意思表示が明確でない。README と CONTRIBUTING で補う必要がある。
- 負: GitHub の設定変更 (Pull requests を collaborators only にする) と `.github/` 一式の設置という、README 以外の作業が public 化フェーズに加わる。
- 負 (実装で判明): `blank_issues_enabled: false` と `contact_links` 未設定の組み合わせにより、Issue 作成画面の選択肢がバグ報告と提案の 2 本だけになる。「使い方が分からない」「仕様か不具合か判断できない」という**質問の行き先が無く**、必須の再現手順を埋められない利用者はバグ報告テンプレートへ無理に流し込むか諦めることになる。AI スロップの抑止という動機は満たすが、正当な質問も同じ網で止まる。窓口を置くか (Discussions の有効化、または `contact_links` の追加) は public 化フェーズの論点として残していた。**(2026-08-30 決着)** 質問用フォームを 3 本目に足して Issues 1 面で受け、**Discussions は開かない**。Issue Forms なら必須項目で「実際に動かした証拠」を要求でき、上の抑止方針を質問窓口にも効かせられるため — Discussions のカテゴリテンプレートは本文の雛形にとどまり必須化ができない。質問が増えたら後から Discussions を開いて既存 Issue を変換できるが、逆に開いてから閉じると既存スレッドが読めなくなるため、開かない側から始める。

---
出典: kasane/roadmaps/package-distribution/phases/phase-9-docs/history.md (2026-08-29 の「貢献の受け付け方」4 節) / kasane/roadmaps/package-distribution/phases/phase-9-docs/agenda.md (決定事項「貢献の受け付け方」)
出典 (2026-08-30 改訂): kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/history.md (2026-08-30「Issue の質問窓口」) / kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/agenda.md (決定事項「Issue の質問窓口」)
