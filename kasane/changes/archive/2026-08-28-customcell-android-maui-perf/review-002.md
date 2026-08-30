# レビュー結果: customcell-android-maui-perf (002 回目)

**日付**: 2026-08-28
**判定**: CHANGES_REQUESTED

## サマリー

review-001 の Major 2 件・Minor 3 件は、いずれも指摘の意図どおりに閉じている。とくに証跡 (`evidence/gfxinfo-pixel6a.md`) は 5 構成の生の集計行・計測スクリプト・計測回数・ビルド前提 (HEAD 0a5a1f3) まで揃っており、concepts の表 4 行すべてがこの証跡から機械的に再導出できる状態になった。計測手順節に書かれた build / install コマンドも、リポジトリの実配置 (`ApplicationId` = `jp.kamusoft.kssettingsview.samples.maui`、`bin/Release/net10.0-android/...-Signed.apk` の実在) と一致することをこちらで確認した。iOS 節の留保化・「不具合ではない」の射程限定・簡易起票も適切で、持ち越し 1 件 (rules.md の「主な type」) の扱いも review-001 の記述どおり。

ただし 1 点だけ、**修正が本文にしか反映されておらず、同じ diff の中の `kasane/concepts/log.md` に撤回前の記述がそのまま残っている**。log.md は append-only の長命層で、コミット前の今なら 1 行の書き換えで済むが、コミット後は「訂正を追記する」しか手段がなくなる。修正コストが今と後で非対称なため、この 1 行だけを理由に CHANGES_REQUESTED とする。他はすべて APPROVED 相当。

## 指摘事項

### [🟡 Minor / 優先度: 高] concepts/log.md の created 行が修正前の本文を記述しており、撤回した iOS 断定が長命層に残っている

**該当箇所**: `kasane/concepts/log.md:324`

**問題点**:

log 行は本文修正の前に書かれたままで、review-001 で Major / Minor として撤回・緩和した 2 つの記述が原文のまま残っている:

- 「**iOS は Debug でも乖離が小さく**「Android だけ遅い」報告の第一容疑者がビルド構成であること**を明記**」 — 本文 (`kasane/concepts/maui/architecture/performance-verification.md:52`) は「iOS 側は本件では**未計測**」「乖離は小さいと**推定**されるが、裏取りはしていない」に書き換わっており、log 行が「明記」したと主張する内容を本文は明記していない
- 「**Debug の遅さ自体は不具合ではないこと**」 — 本文 17 行は「**それ単独では**実装欠陥の証拠にならない」に射程が絞られており、log 行は緩和前の一般化のまま

結果として、(a) log.md 単体を読む/grep する読者は、未計測の推定を実測と同じ確度の断定として受け取る (Major #1 が問題にした状態が、本文から log へ場所を移して残っている)、(b) log 行が指す概念の内容と log 行の記述が食い違い、更新履歴としての機能を果たさない。

`log.md` は ksn-core `references/concepts.md` が定める **append-only の更新履歴**であり、archive されずに恒久的に残る。今はまだ未コミット (`git status` で `M kasane/concepts/log.md`) なので当該行の書き換えで直せるが、コミット後は訂正行の追記が必要になり、履歴に矛盾した 2 行が並ぶ。

**推奨修正**: `log.md:324` の当該 2 箇所を本文の現状に合わせる。例:

- 「iOS は Debug でも乖離が小さく」→ 「iOS 側は未計測で、既存の『iOS は問題なし』の観察は Simulator 経路のため Android 実機と同じ土俵にないこと」
- 「Debug の遅さ自体は不具合ではないこと」→ 「Debug の遅さは単独では実装欠陥の証拠にならず、Release で予算を超えるなら実装を疑うこと」

あわせて計測手順節と証跡の追加も log 行に含めておくと、後続が本文を開かずに何が入ったか判断できる。

なお `summary.md:5` の「(Pixel 6a 実機で顕著、iOS / Native Android は問題なし)」も同種の断定だが、こちらは change 配下 (archive される足場) の症状記述であり、本文が Simulator 経路の注記を持っているため必須の修正とはしない (直すなら「iOS は Simulator 経路での観察」と一言添える程度)。

### [🔵 Suggestion] 「振れ幅も大きい」の根拠が、同じ節の脚注 (発熱の交絡) に自ら打ち消されている

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:30,32`

**問題点**: 新設された脚注 (30 行) は `UseInterpreter=false` の幅を「連続 2 回の計測で、2 回目には発熱の影響が混ざり得る」と説明している。一方 32 行は同じ幅を「**振れ幅も大きいため** Release の代替にならない」の根拠に使っている。脚注を読んだ読者は、この幅を構成固有の不安定さではなく計測条件の交絡として理解するため、根拠として受け取れない。

review-001 の Suggestion (計測回数の明示) 自体は完全に閉じており、むしろ透明化した結果として論拠の弱さが見えるようになった、という関係にある。

**推奨修正**: 「振れ幅も大きい」を落として「値は Release に届かない (8.8% / 53ms でも Release の 4.6% / 12ms とは開きがある) うえ、2 回の計測で再現性を確認できていない」に置き換える。結論 (Release の代替にならない) は値の差だけで十分成立する。

### [🔵 Suggestion] 蒸留時に、本文の evidence 参照を archive 後の確定パスへ書き換える

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:48`

**問題点**: 長命層から change 配下の証跡を指す参照が `kasane/changes/customcell-android-maui-perf/evidence/gfxinfo-pixel6a.md (アーカイブ後は kasane/changes/archive/ 配下)` の形になっている。ksn-core `references/paths.md` は archive 済みの参照形式を `kasane/changes/archive/<YYYY-MM-DD>-<change-id>/...` と定め、**長命層を書く側は最初から archive 後のパスを書く**としている。現時点では archive 日が未定なので暫定形が妥当だが、このまま蒸留を通すと恒久的に「解決規則を知らないと辿れない参照」が残る。

**推奨修正**: 蒸留 (archive) の Step で 48 行を `kasane/changes/archive/<archive 実行日>-customcell-android-maui-perf/evidence/gfxinfo-pixel6a.md` に確定し、括弧書きの注記を落とす。この change 内での対応は不要 (蒸留時の TODO として summary.md か本レビューを根拠にすればよい)。

### [🔵 Suggestion] summary.md の「触ったファイル」が今回の追加分を反映していない

**該当箇所**: `summary.md:33-40`

**問題点**: 「触ったファイル」は change 配下を `exploration.md / session.md / summary.md` と列挙したままで、今回追加された `evidence/gfxinfo-pixel6a.md` と、新規に作成された別 change ディレクトリ (`kasane/changes/maui-android-customcell-embed-perf/`) が入っていない。どちらも本文の別の箇所 (29 行・31 行) では言及されているため実害は小さいが、この節は蒸留・レビューが diff と突き合わせる索引として使う箇所であり、`git status` の `?? kasane/changes/maui-android-customcell-embed-perf/` が索引に無い状態は突き合わせを一手増やす。

**推奨修正**: 「触ったファイル」に `evidence/gfxinfo-pixel6a.md` (新規) と、起票した別 change のパスを 1 行ずつ足す。

## アクションプラン

1. **[Minor / 優先度高] `kasane/concepts/log.md:324` を本文の現状に合わせる** — 未コミットの今のうちに。これだけで判定は APPROVED 相当になる
2. **[Suggestion] `performance-verification.md:32` の「振れ幅も大きい」を再現性未確認の表現へ**
3. **[Suggestion] `summary.md` の触ったファイルに evidence と起票先を追加**
4. **[Suggestion / 蒸留時] `performance-verification.md:48` の evidence 参照を archive 後パスへ確定 / rules.md の `<platform>/architecture/` 主な type にオーナー合意で `policy` を追加 (review-001 からの持ち越し)**

## review-001 指摘の対応確認

| # | 重要度 | 指摘 | 判定 |
|---|---|---|---|
| 1 | 🟠 Major | iOS 非対称が未計測の推定を断定形で記述 | **対応済み** (本文 50-54 行が「未計測」「推定」「Simulator 経路 = JIT かつ Mac の性能」まで明記。frontmatter description にも「iOS 側は本件では未計測」を追加)。ただし log.md への波及漏れ → 上記 Minor |
| 2 | 🟠 Major | 実測値の証跡が change 配下に無い | **対応済み** (`evidence/gfxinfo-pixel6a.md` を新設。5 構成の集計行・計測スクリプト・計測回数・端末/ビルド前提を記載。本文にも「計測手順」節と証跡へのポインタ) |
| 3 | 🟡 Minor | 「Release で測れ」の手順アンカーが無い | **対応済み** (本文 34-48 行。README が `-c Debug` 固定であることの明示 + 実コマンド 1 セット。README は不変 = 判断どおり) |
| 4 | 🟡 Minor | 「不具合ではない」の一般化が強すぎる | **対応済み** (本文 17 行が「それ単独では実装欠陥の証拠にならない」+「Release でも予算を超えるなら実装側を疑う」の肯定側基準まで併記)。log.md への波及漏れ → 上記 Minor |
| 5 | 🟡 Minor | 見送った構造課題 2 件が exploration.md にしか残らない | **対応済み** (`kasane/changes/maui-android-customcell-embed-perf/exploration.md` を簡易起票。summary.md:29 から参照) |
| 6 | 🔵 Suggestion | `UseInterpreter=false` の幅に n も条件も無い | **対応済み** (本文 30 行に計測回数と交絡の注記、証跡にも同内容)。副作用として上記 Suggestion 1 |
| 7 | 🔵 Suggestion | 関連リンクに runtime-behavior-verification が無い | **対応済み** (本文 60 行。リンク先の実在と相対パスの正しさを確認) |
| 8 | 🔵 Suggestion | `type: policy` を platform/architecture に置く前例が無い | **持ち越し (妥当)** — review-001 自身が「蒸留時にオーナー合意を取って rules.md を更新」を推奨しており、この change で rules.md (長命層の予約ファイル) を単独判断で書き換えないのは正しい扱い |

## 確認した観点 (問題なしと判定したもの)

- **summary.md と実 diff の一致**: `git status --porcelain` は `M kasane/concepts/log.md` / `M kasane/concepts/maui/index.md` / `?? kasane/changes/customcell-android-maui-perf/` / `?? kasane/changes/maui-android-customcell-embed-perf/` / `?? kasane/concepts/maui/architecture/performance-verification.md` の 5 件。`git diff HEAD -- samples/` は空で、**samples/ 差分ゼロは維持されている** (前回からの追加は change 配下の evidence と新規 change ディレクトリのみ)。`git stash list` も空
- **証跡と concepts の表の整合**: 証跡の生値 → 本文の表を 1 行ずつ突き合わせ、native 6.09%→6.1% / p90 28ms、Debug 31.69%→31.7% / 121ms、Release 4.59%→4.6% / 12ms、`UseInterpreter=false` 8.82〜19.44%→8.8〜19.4% / 53〜65ms がすべて一致。summary.md の表とも一致し、丸め以外の差異なし
- **計測手順の再現性**: 本文 41-45 行のコマンドを実配置と照合。`ApplicationId` = `jp.kamusoft.kssettingsview.samples.maui` (csproj:17)、apk 出力先 `samples/maui/KsSettingsView.Sample.Maui/bin/Release/net10.0-android/jp.kamusoft.kssettingsview.samples.maui-Signed.apk` が実在。README の実機手順が `-c Debug` 固定 (86-87 / 110-111 / 120-121 行、`-c Release` は 0 件) という主張も grep で確認。証跡が主張するビルド前提 `HEAD 0a5a1f3` も現 HEAD と一致
- **証跡の規約適合 (ksn-core references/evidence.md)**: 置き場は `evidence/` 配下、内容は生ログ全文ではなく判定に要る集計行の抜粋。シリアルは `$SERIAL` 変数のままで実値なし、端末表記は機種名「Pixel 6a」のみ。ホスト名・氏名・IP・UDID の混入なし。拡張子が `.md` (規約の例示は `.log` / `.txt`) だが、手順・回数・体感確認を同一文書にまとめる用途では妥当と判断し指摘しない
- **lint**: `python3 scripts/identity-lint.py` / `python3 scripts/local-path-lint.py` をリポジトリ全体で実行し双方 exit 0。両スクリプトとも `git grep --untracked` を使うため、未追跡の change 配下・新規 concepts も検査対象に入っている
- **簡易起票の形式適合 (ksn-explore「簡易起票 (軽量エントリ)」)**: `課題 / 動機` に発見の文脈と対象ファイルパス、`未決の論点` の先頭に「未探索 (簡易起票)」と元 change、`変更級の推奨: 未判定` (暫定の根拠付き)、他セクションは見出しのみ — 規定どおり。既存 change 一覧に同一課題の重複なし。記載された技術的主張もコードで裏取りした: `KsBridgeCellContentView.kt:64` の `AndroidView` に `onReset` の引き渡しが無いこと、`KsAccessoryHostView.OnMeasure` (54-69 行) に制約の同一判定も結果キャッシュも無く毎回 `_view.Measure` を呼ぶこと、`kasane/concepts/maui/architecture/view-materialization.md:75` が `AndroidView` への `onReset` 付与を前提破壊として明示警告していること、いずれも記述どおり
- **concepts 規約適合 (rules.md / ksn-core references/concepts.md)**: 配置 (`maui/architecture/` = platform のビルドツールチェーンの契約) は前回同様妥当。frontmatter 5 項目具備、h1 = title 一致、「この文書を読むと〜」の宣言あり。関連リンク 3 本すべて実在を確認 (`../../android/architecture/build-toolchain.md` / `../../cross/conventions/test-execution.md` / `../../cross/conventions/runtime-behavior-verification.md`)、実配置基準の相対パスで Markdown ビューアから辿れる形
- **runtime-behavior-verification.md の完了条件**: 3 条件 (実環境での再現・同一手順での解消確認・証跡を change 配下に残す) をすべて満たした。とくに条件 2 は Debug/Release の A/B が証跡に両方残っている形で満たされている
- **index.md の形式**: `architecture/` 節へアルファベット順で挿入、既存行と同じ「リンク — 1 行説明」形式。1 行説明から適用範囲 (何を測るときに読むか) が読み取れる
- **ビルド / テスト**: ソースコードの diff がゼロ行 (`git diff HEAD --stat` は log.md / index.md の 2 ファイル 2 行のみ、新規は概念 md と change 配下の md)。挙動に触れていないため HEAD 時点のビルド・テスト結果がそのまま有効で、summary.md の「テスト不要」判断は妥当
- **lessons/code-review.md**: 重点観点 L-001 (ミューテーションによる検出力の実測) はテストコードを含まない本 change に非該当。「指摘しないこと」は空 (昇格済みルールなし)
