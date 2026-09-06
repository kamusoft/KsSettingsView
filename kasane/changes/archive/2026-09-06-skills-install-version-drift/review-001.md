# レビュー結果: skills-install-version-drift (001 回目)

**日付**: 2026-09-06
**判定**: CHANGES_REQUESTED

## サマリー

置換 script の中核 (ファイルごとの期待対象集合・0 行 / 2 行以上での全体失敗・SwiftPM の `exact:` 強制・`--check` の呼び出し契約) は決定事項どおりに実装され、実測でも `--selftest` 26 件すべて成功 / `--check 0.1.0-beta.1` 成功 / en-ja コードブロック byte 一致を確認した。機能上の欠陥は見つからなかった。

一方で、「script の対象が README だけ」という前提で書かれた記述の追随が半分残っている。deviation で `kasane/handbook/cross/release-procedure.md` の 2 箇所を直したのと同じ理由が `.github/workflows/` の 2 ファイルにもそのまま当たり、加えて改訂した handbook の `timestamp` が更新されていない。いずれも機械的な数行の修正なので、同じサイクル内で閉じてほしい (lessons process L-001 / L-005 の趣旨)。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — script のコメント・docstring に作業文書パス / 変更識別子 / 仮称の混入なし。`comment-policy-lint.py --advisory` は禁止 0 件 (要確認 173 件はすべて `samples/` の既存債務で、本 diff 由来なし)
- `kasane/handbook/cross/user-skill-api-listing.md` (きっかけ: `skills/**` を触る) — 掲載 API の増減なし。「コード例のコメントは原則書かない / やむを得ない場合は英語」にも抵触なし
- `kasane/handbook/cross/release-procedure.md` (きっかけ: リリース手順の記述を触る) — 手順 2 と注意書きの文言追随を確認 (下記 Minor 2 参照)
- `kasane/handbook/cross/test-execution.md` (きっかけ: テスト実行と結果の報告) — platform ソースに触れない変更のため iOS / Android / MAUI のテストは対象外。実行件数の併記規約に従い、下記「実行した検証」に件数を記載
- `kasane/lessons/process.md` / `kasane/lessons/code-review.md` — L-005 (到達可能な修正はサイクル内で閉じる)、code-review L-001 (静的読解で止めずミューテーションで実測) を適用。「指摘しないこと」は昇格済みルールなし

## 実行した検証

| 検証 | 結果 |
|---|---|
| `python3 scripts/release/set-readme-version.py --selftest` | 26 件 OK / 0 件 NG (「失敗なし」)。拡張前の 17 件から 9 件増 |
| `python3 scripts/release/set-readme-version.py --check 0.1.0-beta.1` | exit 0、「インストール例 14 行が 0.1.0-beta.1 と一致する」 |
| `python3 .agents/skills/docs-refresh/scripts/code-block-parity-check.py` | `code blocks byte-identical` (en/ja の置換 4 行が同値) |
| `python3 scripts/comment-policy-lint.py --advisory` | 禁止 0 件 (要確認 173 件は既存債務) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 検出 0 件 |
| `python3 scripts/doc-structure-lint.py` | `kasane/handbook/cross/release-procedure.md` は違反リストに無し (本 diff 由来の新規違反なし) |
| `python3 scripts/readme-example-lint.py` | 「README の最小例 4 件が消費者検証のソースと一致する」— version 行の書き換えで壊れていない |
| ミューテーション実測 (一時コピーに対して実施。リポジトリは書き換えていない) | `skills/en/kssettingsview-ios/SKILL.md` の `exact:` を `from:` に戻すと `check` が exit 1 で `skills/en/kssettingsview-ios/SKILL.md:40: ... は exact: で書く (実際は from: ...)` を出す。Skill 側でも `exact:` gate が実際に効くことを確認 |

決定事項との一致も確認した: 対象 10 ファイル / 14 行 (`scripts/release/set-readme-version.py:70-84`)、ファイルごとの期待対象集合 (`TARGET_FILES` と `find_targets(lines, names)`)、0 行・2 行以上で `collect` が problems に積み `replace` が何も書かずに 1 を返す構造 (`scripts/release/set-readme-version.py:152-169`)、`decisions/` 無改訂 (ADR-0022 に手を入れていない)、`skills/.manifest.json` の `lastUpdatedFiles` 無更新。`--check` / `--selftest` の引数形と終了コードは変わっておらず、`.github/workflows/release.yml:155` と `.github/workflows/ci.yml:188` の呼び出しはそのまま通る。

## 指摘事項

### [🟡 Minor] 「README だけ」で書かれた workflow の step 名とコメントが追随していない

**該当箇所**: `.github/workflows/release.yml:152`、`.github/workflows/ci.yml:181-187`

**問題点**: 本 change で `--check` の検査範囲が README 2 枚から 10 ファイル 14 行へ広がったのに、それを呼ぶ step の名前とコメントが README だけを指したままになっている。

- `release.yml:152` の step 名 `Verify README install examples` — 失敗ログに出るのはこの名前なので、実際は Skill 8 枚の不一致で落ちているときに読み手を README へ誘導してしまう
- `ci.yml:187` の step 名 `README version script selftest` と、`ci.yml:181-183` のコメント `README の置換は一時コピーに対して行うので、ネットワークにも作業ツリーにも触らない` — selftest は Skill 8 枚も一時コピーに置いて回すようになった

これは deviation.md に記録済みの付随修正 (`kasane/handbook/cross/release-procedure.md` の「README 2 枚」「README の不一致」を広げた) とまったく同じ性質の記述の狭さで、原因も同じく本 change にある。本務で触った script の呼び出し元 1 箇所ずつ・数行で閉じるため、別 change へ逃がす条件 (無関係のファイルへ広がる) に当たらない。

なお step 名は branch protection の required check (job 名) ではないので、変更しても 7 件の check 構成には影響しない。

**推奨修正**: `release.yml` の step 名を「インストール例」の検査だと分かる名前 (例: `Verify install example versions`) に、`ci.yml` の step 名とコメントを「README と利用者向け Skill」を含む表現に改め、deviation.md へ付随修正として 1 行追記する。

### [🟡 Minor] 改訂した handbook の timestamp が更新されていない

**該当箇所**: `kasane/handbook/cross/release-procedure.md:8`

**問題点**: 本文の「リリース PR」手順 2 と注意書きを 2026-09-06 に改訂したが、frontmatter は `timestamp: 2026-09-04` のまま。ksn-core の handbook 規約は `timestamp` を最終検証日と定め「確認した日に更新する」としており、更新漏れは ksn-drift の棚卸しで「90 日閾値までは検証済み」と誤って扱われる元になる。同じ diff の中で `kasane/handbook/cross/test-execution.md` は `timestamp: 2026-09-06` に揃っている (本 change の対象外だが、同層で運用されている証拠) 点でも不揃い。

**推奨修正**: `timestamp` を `2026-09-06` に更新する。

### [🟡 Minor] selftest が `check` の `exact:` 要求を検査していない

**該当箇所**: `scripts/release/set-readme-version.py:375-385`

**問題点**: 本 change が実際に直した腐りは「iOS Skill 2 枚が `from:` だった」ことであり、それを publish 前に止めるのが `check()` の `keyword != "exact"` 分岐 (`scripts/release/set-readme-version.py:198-202`) である。ところが `--selftest` はこの分岐を一度も通っていない — 検査を回すのは `replace` 済み (すでに `exact:`) のツリーに対してだけで、`from:` のまま `check` を掛けるケースが無い。今回この分岐は定数化 (`"SwiftPM の依存宣言"` → `SWIFTPM`) で書き換えられており、CI に回帰検出網が無い状態でリリースゲートの中心的な判定を触ったことになる。

実測 (上表のミューテーション) では分岐が正しく効くことを確認済みなので、現時点の欠陥ではなく回帰検出力の欠落として挙げる。0 行 / 2 行以上の失敗モードは Skill 側も含めて新規テストが足されており (`scripts/release/set-readme-version.py:421-464`)、この 1 つだけが取り残されている。

**推奨修正**: `[置換]` 節に、`from:` のままのツリー (例: `SELFTEST_IOS_SKILL` をそのまま stage したもの) に `check` を掛けて exit 1 と `exact: で書く` の出力を得るケースを 1 件足す。

### [🔵 Suggestion] 「ファイルが無い」経路が selftest で未検査

**該当箇所**: `scripts/release/set-readme-version.py:145-148`

**問題点**: `collect()` の欠落検出は拡張前から未検査だが、対象が 2 枚から 10 枚に増え、そのうち 8 枚は docs-refresh が生成・改名しうる `skills/` 配下になったことで、発火する確率は上がった。ディレクトリ名の変更でリリース直前の validate が落ちる経路なので、失敗メッセージが有用な形で出ることを 1 件で押さえておく価値がある。

**推奨修正**: 対象 Skill 1 枚を stage しないケースを足し、`replace` が exit 1 と `ファイルが無い` を返すことを確認する (優先度は上記 Minor より低い)。

### [🔵 Suggestion] iOS Skill の導入節に prerelease と `exact:` の説明が無い

**該当箇所**: `skills/en/kssettingsview-ios/SKILL.md:30-40`、`skills/ja/kssettingsview-ios/SKILL.md:30-40`

**問題点**: 本 change で iOS Skill の例が `from: "0.1.0"` から `exact: "0.1.0-beta.1"` に変わったが、直前の散文は Xcode の *Add Package Dependencies...* に URL を入れる手順を案内するだけで、なぜ `exact:` なのか (prerelease は明示指定でしか解決されない) に触れていない。README 側には同じ位置に prerelease 選択の説明があるため、Skill を読んだ利用者のエージェントが Xcode UI の既定の依存規則を選んで解決に失敗する余地が残る。

なお `README.md:48` の「To select a prerelease, use its semantic version tag explicitly with `from: "X.Y.Z-beta.N"`, or pin it with `exact:`」は、script の docstring (`scripts/release/set-readme-version.py:21-23`) の「`from:` は prerelease の tag を解決しない」と字面が食い違って見える。どちらも本 change 以前からの記述で diff の範囲外だが、`exact:` 強制の根拠を利用者向け文書に書くときに整理が要る。

**推奨修正**: 本 change では直さない。`skills/` の文面追随は docs-refresh の責務なので、次回の docs-refresh 依頼へ「iOS Skill 導入節に prerelease / `exact:` の説明を足す」「README の prerelease 説明と script の根拠の食い違いを解消する」を申し送る (または簡易起票する)。

## アクションプラン

1. `.github/workflows/release.yml` の step 名と `.github/workflows/ci.yml` の step 名 / コメントを、README + 利用者向け Skill の範囲に合わせて改める。deviation.md に付随修正として追記する (Minor)
2. `kasane/handbook/cross/release-procedure.md` の `timestamp` を `2026-09-06` に更新する (Minor)
3. `--selftest` に「`from:` のままの `check` が失敗する」ケースを 1 件足す (Minor)
4. 余力があれば「ファイルが無い」ケースの selftest を足す (Suggestion)
5. iOS Skill の prerelease 説明は本 change では触らず、docs-refresh への申し送りとして残す (Suggestion)
