# レビュー結果: add-spm-distribution (002 回目)

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED

## サマリー

1 周目で採用確定した 7 件はすべて解消を確認した。特に検証 4 (自己・祖先指定の拒否) は、包含判定への置き換えとテストの組み替えによって「monorepo 自身」ケースが検証 1〜3 を通過した上で発火する形になり、ガードを無効化するミューテーションで当該 2 ケースだけが落ちることをレビュー側でも実測した。内部モード `--self-or-ancestor-check` は破壊的操作・git 操作・ネットワークのいずれにも触れずに終了するため、spec「スナップショット同期スクリプト」の SHALL 群と矛盾しない。見送り確定分 (`.gitignore` 同梱 / roadmap の旧 scheme 名) はいずれも触られていない。

一方で、今周期で `kasane/handbook/cross/test-execution.md` に追記された全体件数の数え方が**実測と合わない**。この規約が governs する当の領域 (件数確認) で誤った手順を指示しており、そのまま従うと 1000 件の実行を 2998 件と報告することになる。1 行の修正で済むが、規範層に誤りを残せないため CHANGES_REQUESTED とする。

## 実行した検証

| 検証 | コマンド | 結果 |
|---|---|---|
| 同期スクリプトのテスト | `./scripts/spm-snapshot/sync-snapshot-test.sh` | **41 アサーション全成功** (exit 0)。1 周目の 33 から 8 件増 (LICENSE / Sources / Tests の内容一致 3 件、祖先判定単体 5 件) |
| ミューテーション (検証 4 ガード無効化) | `is_self_or_ancestor_of ...` → `if false` に置換して再実行 | 「monorepo 自身」「monorepo の祖先ディレクトリ」の内容不変アサーション 2 件が FAIL。復元後 `shasum` 一致を確認 (`78018d96...`) |
| iOS 全件テスト | `cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<ios-simulator-udid>'` | `** TEST SUCCEEDED **`。5 バンドル — Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UI 645 = **合計 1000 tests, 0 failures** |
| lint | `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | comment-policy 禁止 0 件 (703 ファイル)、local-path・identity は出力なし。doc-structure は `test-execution.md` が編集前後とも 5 件で不変 (指摘中の「13 件」は同名見出し 3 つを合算する lint 側の挙動によるもので、iOS 節自体は 4 → 5 項目、閾値 10 の範囲内) |
| ci.yml | YAML 解析して lint job の step 名を列挙 | 7 step。末尾に `SPM snapshot sync script test` が追加されている |
| git 非操作 (追加した `git status` の副作用) | 一時リポジトリで `.git/index` の shasum を `git status --porcelain` の前後で比較 (未追跡・変更ありの状態を含む) | 変化なし。spec「index を変更せず」に抵触しない |

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (シェルスクリプト 2 本・`build-xcframework.sh`・`ci.yml` のコメント改訂) |
| `kasane/handbook/cross/test-execution.md` | テストを実行するとき・テスト結果を報告するとき (本文の改訂対象でもある) |
| `kasane/handbook/cross/public-identifiers.md` | `ios/Package.swift` を触る / 配布座標を決める (今周期では未変更) |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触るとき → 本変更は `ios/Sources/` を触らないため非該当 |
| `kasane/lessons/code-review.md` | L-001 (ミューテーションによる検出力の実測) を適用。「指摘しないこと」は昇格済みルールなし |

`sample-parity.md` / `user-skill-api-listing.md` / `runtime-behavior-verification.md` / `local-development-setup.md` は非該当。コメント規約は今周期で触れた箇所 (`sync-snapshot.sh` のヘッダと内部モードの説明、`sync-snapshot-test.sh` の各ケース注釈、`build-xcframework.sh` の追記、`ci.yml` の step コメント) を節ごとに照合した。作業文書のパス・変更識別子の裸参照・ローカル通番・進捗ログ・デルタスペック構文キーワードのいずれも混入していない。`build-xcframework.sh` の追記は現在形の仕様説明に収まっている。

## 1 周目の採用確定分の解消状況

| 指摘 (突き合わせ結果) | 解消 | 根拠 |
|---|---|---|
| 配信 README が「誘導のみ」(SHALL) を超える | ✅ | `scripts/spm-snapshot/README.template.md` から依存宣言スニペットと `from: "0.1.0"` が消え、インストール手順・Issue 窓口とも monorepo への誘導のみ。Scenario「誘導 README の内容」は引き続き充足。tasks.md 3.2 のチェックも実態と一致 |
| 検証 4 の検出力 (テスト未到達 / 冗長 case / `/` 指定の穴) | ✅ | `sync-snapshot.sh:50-57` の包含判定へ一本化。`/` が祖先と判定されることを `sync-snapshot-test.sh:227` が直接検査。「monorepo 自身」ケース (`:196-209`) は配信リポジトリ origin を持つ git top-level 自身を fake monorepo に仕立てて検証 1〜3 を通過させており、ミューテーションで検出力を実測した |
| コピー内容テストが Sources/Tests/LICENSE を未検証 | ✅ | `sync-snapshot-test.sh:84-91` の `tree_fingerprint` で相対パス一覧+全ファイル内容のハッシュを突き合わせ (`:127-132`)、LICENSE も内容比較 (`:127-128`) |
| 新設シェル資産の CI 未接続 | ✅ | `.github/workflows/ci.yml:106-109` に step 追加。ubuntu ランナーの lint job で回る |
| SwiftUI 同梱の実測反映 | ✅ | `ios/binding/build-xcframework.sh:10-14` に「参照されないメンバーは force_load しない限り最終アプリに取り込まれない」旨を追記。deviation.md 記録済みのオーナー判断と整合 |
| 未コミット変更の無警告破棄 | ✅ | `sync-snapshot.sh:135-139` で破壊的操作の直前に警告し、中断はしない |
| `readonly VAR="$(...)"` の失敗握りつぶし | ✅ | `sync-snapshot.sh:70-75` / `:98-99` で宣言と代入を分離し、`|| fail` を付与 |

見送り確定分: `.gitignore` は配置対象に入っていない (ホワイトリスト 5 点のまま)。`kasane/roadmaps/` は working tree 上で 1 件も変更されておらず、phase-3 agenda.md:12 の旧 scheme 名は残っている。いずれも合意どおり。

## 指摘事項

### [🟠 Major] test-execution.md に追記した全体件数の数え方が実測と合わない

**該当箇所**: `kasane/handbook/cross/test-execution.md:47`

**問題点**:
追記された「全体件数は出力中の `Executed` 行をすべて拾って合算する」は、実際の `xcodebuild test` 出力では成立しない。`xcodebuild` は**テストクラス単位の `Test Suite` にも `Executed` 行を出す**うえ、バンドル単位の集計行も `'<バンドル名>.xctest'` と `'All tests'` の 2 回出力する。

本レビューで実行した全件テストの出力を実測すると、`Executed` 行は 87 本あり、その総和は **2998**。実際の実行件数は 1000 (Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UI 645) で、約 3 倍に膨らむ。

```
Test Suite 'KsBridgeValueTransportTests' passed ...     ← クラス単位にも出る
	 Executed 17 tests, ...
Test Suite 'KsSettingsViewBridgeTests.xctest' passed ...  ← バンドル集計
	 Executed 166 tests, ...
Test Suite 'All tests' passed ...                        ← 同じ値がもう一度
	 Executed 166 tests, ...
```

この規約は「実行件数を確認するところまでが検証」を掲げる文書であり、手順どおりに数えると誤った件数が完了報告に載る。さらに、膨らんだ合計はバンドルが 1 つ欠落しても大きな値のままになるため、1 周目が懸念した「scheme 変更でテストターゲットが拾われなくなる」退行をむしろ隠す方向に働く。

なお deviation.md の付随修正欄は本ファイルについて「参照する scheme 識別子のみを実態へ追随。規範の内容 (Simulator 全件実行・件数確認) は不変」と記録しているが、この追記は規範の内容そのものの追加であり、記録と実態がずれている。

**推奨修正**:
数える対象をバンドル単位の集計行に限定する。例: 「バンドルごとの集計行 (`Test Suite '<バンドル名>.xctest' passed|failed` の直後の `Executed` 行) を拾って合算する。クラス単位の `Executed` 行と `All tests` の行を混ぜると重複計上になる」。実測ではこの数え方でちょうど 5 行が拾え、合計は 1000 になる。あわせて deviation.md の当該行を、規範内容の追加を含む形へ改める。

### [🔵 Suggestion] テストが `shasum` を直接呼ぶ箇所があり、フォールバックが効かない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot-test.sh:258,266`

**問題点**:
`:20-25` で `shasum` / `sha256sum` を選ぶ `HASH_TOOL` を用意しているのに、index 比較の 2 行だけが `shasum` を直書きしている。`shasum` を持たない環境ではコマンド置換が失敗して `index_before` と実測値がともに空文字になり、`assert_equals "" ""` として**通ってしまう** (このテストは `set -e` を使わないため中断もしない)。CI の ubuntu-24.04 には perl 同梱の `shasum` があるので現状は動くが、ランナーイメージやローカル環境の差で無音になる経路が残る。

**推奨修正**: 2 行を `"${HASH_TOOL}"` に揃える。

### [🔵 Suggestion] `assert_ancestor` が内部モードの usage エラーを「非祖先」と読む

**該当箇所**: `scripts/spm-snapshot/sync-snapshot-test.sh:98-107`

**問題点**:
`sync-snapshot.sh:59-68` は「非祖先」を 1、引数不正を 2 で区別しているが、ヘルパは非ゼロをまとめて「非祖先」に畳んでいる。将来フラグ名や引数の数を変えたとき、「非祖先」を期待する 2 件のアサーションは usage エラーのまま通る。

**推奨修正**: 終了コードを取り出して 1 と 2 を区別し、2 は失敗として扱う。

### [🔵 Suggestion] `ci.yml` への step 追加が deviation.md / tasks.md のどこにも現れない

**該当箇所**: `.github/workflows/ci.yml:106-109` / `kasane/changes/add-spm-distribution/deviation.md`

**問題点**:
`.github/workflows/ci.yml` は spec の Requirement にも tasks.md にも登場しないファイルで、変更の出所がアーティファクトから追えない。同種の CI 追随 (`verify-ios.yml`) は deviation.md に付随修正として記録されており、扱いが揃っていない。蒸留・検証の段階でこの step が「どの合意で入ったのか」を再導出できなくなる。

**推奨修正**: deviation.md の付随修正に 1 行足す (レビュー由来の追加であることが分かる書き方でよい)。

## アクションプラン

1. **`test-execution.md:47` の数え方をバンドル単位の集計行に限定する** (Major) — 規範層に誤った手順を残さない。あわせて deviation.md の当該記述を実態へ合わせる
2. **Suggestion 3 件** — `shasum` の統一 / `assert_ancestor` の終了コード判別 / `ci.yml` 変更の deviation 記録。いずれも数行で、グループ 4 の着手前に片付けられる
