# レビュー結果: add-spm-distribution (003 回目)

**日付**: 2026-09-01
**判定**: APPROVED

## サマリー

2 周目の Major 1 件・Suggestion 3 件はすべて解消を確認した。焦点だった `test-execution.md` の件数の数え方は、修正後の文言どおりに手を動かすと実測でちょうど 5 行が拾え、合計が実行件数 1000 と一致する (誤った旧手順では 2998)。スクリプト側の 2 件も、`HASH_TOOL` の統一と終了コード 1/2 の判別として素直に入っており、41 アサーションは全成功、iOS 全件テストも `** TEST SUCCEEDED **` (1000 tests / 0 failures)。

新規に持ち込まれた問題は 1 点のみで、追記した箇条書きが `doc-structure` lint の 1 項目 200 字上限を 22 字超えている (2 周目時点は違反 0 件だった)。hook 未登録の助言 lint でリポジトリ全体に 388 件の既存違反がある種類のものなので、判定は APPROVED とし低優先度の Minor として残す。

## 実行した検証

| 検証 | コマンド | 結果 |
|---|---|---|
| 数え方の実測 (Major の是正確認) | `cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<ios-simulator-udid>'` の出力を新文言どおりに集計 | `Test Suite '<名>.xctest' passed` 直後の `Executed` 行は **5 行** (166 / 88 / 94 / 7 / 645)、**合計 1000**。実行結果は `** TEST SUCCEEDED **` |
| 旧手順との対照 | 同じ出力の `Executed` 行を全部拾って合算 | **87 行 / 合計 2998**。2 周目の実測と一致し、新文言がこの経路を閉じていることを確認 |
| `All tests` 行の扱い | `Test Suite 'All tests' passed` 直後の `Executed` を合算 | 1000。新文言の抽出パターン (`'<名>.xctest'`) はこの行に一致しないため、指示どおりに拾えば二重計上は起きない |
| 同期スクリプトのテスト | `./scripts/spm-snapshot/sync-snapshot-test.sh` | **41 アサーション全成功** (exit 0)。件数は 2 周目から不変 |
| 変更範囲の限定 | 2 周目 (`review-002.md` の mtime) 以降に更新されたファイルを列挙 | `scripts/spm-snapshot/sync-snapshot-test.sh` / `kasane/handbook/cross/test-execution.md` / `kasane/changes/add-spm-distribution/deviation.md` の 3 点のみ。`sync-snapshot.sh` は 2 周目より前の状態のままで、修正が他へ波及していない |
| lint | `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | comment-policy 禁止 0 件 (703 ファイル)、local-path・identity は出力なし。doc-structure のみ `test-execution.md` が 5 → 6 件 (下の Minor) |
| CI step の実行可能性 | `ls -l scripts/spm-snapshot/` | `sync-snapshot-test.sh` は `-rwxr-xr-x`。`ci.yml` の `run: scripts/spm-snapshot/sync-snapshot-test.sh` は実行できる |

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (`sync-snapshot-test.sh` の `assert_ancestor` 注釈・`ci.yml` の step コメント) |
| `kasane/handbook/cross/test-execution.md` | テストを実行するとき・テスト結果を報告するとき (本文の改訂対象でもある) |
| `kasane/lessons/code-review.md` | L-001。今周期は「文言どおりに数えると何が出るか」を実出力で再実行して確かめる形で適用。「指摘しないこと」は昇格済みルールなし |

`public-identifiers.md` は今周期で触られていないため再照合のみ。`sample-parity.md` / `user-skill-api-listing.md` / `runtime-behavior-verification.md` / `local-development-setup.md` / `ios/swift6-language-mode-check.md` は非該当。コメント規約は今周期で触れた注釈 (`sync-snapshot-test.sh:97` の祖先判定ヘルパ説明) を節ごとに照合し、作業文書のパス・変更識別子の裸参照・ローカル通番・進捗ログのいずれも含まないことを確認した。

## 2 周目の指摘の解消状況

| 指摘 | 解消 | 根拠 |
|---|---|---|
| [Major] `test-execution.md` の全体件数の数え方が実測と合わない | ✅ | `kasane/handbook/cross/test-execution.md:46-47`。「全体件数は**バンドル集計行** (`Test Suite '<名>.xctest' passed/failed` の直後の `Executed` 行) だけを拾って合算する」へ是正。実出力でこの手順を再現し 5 行 / 1000 を確認。末尾 1 行だけを見る誤読 (バンドル複数時に最後の値しか映らない) への注意も同じ項目で塞がれている |
| [Suggestion] テストが `shasum` を直接呼ぶ | ✅ | `scripts/spm-snapshot/sync-snapshot-test.sh:259,267` がともに `"${HASH_TOOL}"`。`grep` でスクリプト内に残る `shasum` 直書きは選択ロジック (`:19-24`) のみ |
| [Suggestion] `assert_ancestor` が usage エラーを「非祖先」と読む | ✅ | `sync-snapshot-test.sh:98-108`。`|| status=$?` で終了コードを取り出し、`case` で 0 → 祖先 / 1 → 非祖先 / その他 → `判定エラー (exit N)` に分けている。usage エラー (exit 2) は期待値 `非祖先` と一致しないため FAIL になる |
| [Suggestion] `ci.yml` 変更が deviation.md に現れない | ✅ | `deviation.md` の付随修正に 1 行追加。追加理由 (安全弁テストを CI で回す) と出所 (レビュー採用指摘 / spec・tasks の範囲外) の両方が書かれている |

あわせて `deviation.md` の `test-execution.md` の行が「規範の内容は不変」から「バンドル集計行の合算で全体件数を確認する手順を追記」へ改まり、記録と実態のずれも解消している。

## 指摘事項

### [🟡 Minor] 追記した箇条書きが doc-structure lint の 1 項目上限を超える

**該当箇所**: `kasane/handbook/cross/test-execution.md:47`

**問題点**:
追記された「**出力末尾の 1 行だけを見ない** — …」の項目が 222 字で、`kasane/config.yaml` の `lint.doc-structure.item-chars: 200` を超えている。2 周目の実測ではこのファイルの違反は 5 件 (いずれも Android 節の既存項目) で、今周期の追記が 6 件目を新規に足した形になる。あわせて「正しい実行方法」配下のトップレベル項目の集計も 12 → 13 件へ増えている (これは同名見出し 3 つを合算する lint 側の挙動で、iOS 節自体は 5 項目・閾値 10 の範囲内)。

規約の内容そのものは正しく、`doc-structure-lint.py` は hook に登録されておらず (`config.yaml` に「hook には登録しない」と明記)、リポジトリ全体で 388 件の既存違反がある助言 lint なので、実害は「育つ文書が読みにくくなる方向に 1 歩進んだ」ことに留まる。優先度は低い。

**推奨修正**:
1 項目に詰め込んだ 3 つの内容 (末尾 1 行を見ない理由 / 拾う行の指定 / 多重集計になる理由) を分ける。例えば「末尾 1 行を見ない」を項目に残し、拾い方と多重集計の説明を項目直下の散文か小節へ落とす。

### [🔵 Suggestion] `HASH_TOOL` の選択がフォールバック先の実在を確かめていない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot-test.sh:19-25`

**問題点**:
`shasum` が無ければ `sha256sum` を採るが、`sha256sum` の実在は検査していない。どちらも無い環境ではコマンド置換が空文字を返し、`tree_fingerprint` による Sources / Tests の内容一致 (`:130-133`)、冪等性のハッシュ比較 (`:157,161-163`)、index 不変 (`:259,267`) がいずれも `assert_equals "" ""` として**通ってしまう**。このスクリプトは `set -e` を使わないため中断もしない。今周期の修正で `shasum` 直書きが無くなり比較経路が `HASH_TOOL` に一本化されたぶん、単一の欠落が広範囲を無音で通す形になっている。

2 周目の指摘の残りであって新規の劣化ではなく、CI の ubuntu-24.04 と macOS のいずれにも `shasum` があるため現状の実害はない。

**推奨修正**: `else` 側でも `command -v sha256sum` を確かめ、どちらも無ければ理由を出して非ゼロ終了する (検出力を持たないまま緑を返さない)。

## アクションプラン

1. 判定は APPROVED。上記 2 件はいずれもブロッカーではなく、着手は任意
2. 直すなら **Minor (`test-execution.md:47` の項目分割)** を先に — 規範層の可読性に直接効き、数行で済む
3. **Suggestion (`HASH_TOOL` のフォールバック検査)** は、同期スクリプトを CI で回し続ける前提が固まった段階でまとめて入れてもよい
4. tasks.md グループ 4 (配信リポジトリの作成と解決確認) は未実施のまま。本レビューの対象外
