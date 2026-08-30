# レビュー結果: customcell-android-maui-perf (003 回目)

**日付**: 2026-08-28
**判定**: APPROVED

## サマリー

review-002 の Minor (log.md の created 行) は指摘の意図どおりに解消され、log 行の 3 つの主張 (射程を絞った「単独では証拠にならない」/ iOS 未計測の留保 / `UseInterpreter=false` は Release の代替にならない) がいずれも移設後の本文と一致している。オーナー指示による `maui/architecture/` → `maui/conventions/` の移設も、実ファイル・index.md の節新設・rules.md のカテゴリ行・log 行のパスまで一貫しており、旧パスの残骸は本文・index・log には無い (残るのは review-001/002 と session.md という、書かれた時点の事実を記録する履歴側のみで、これらは書き換えるべきものではない)。相対リンク 3 本の実在、証跡と表の数値一致、lint 2 種 exit 0、samples/ 差分ゼロも再確認した。

残る不整合は 1 点 — `summary.md` の「最終状態」節だけが移設前の記述のまま (存在しないパスを成果物として挙げ、rules.md の新カテゴリ追加が抜けている) で、同じファイルの「触ったファイル」節と食い違っている。ただし同節が正しいパスと移設経緯を明記しているため読者が誤った結論に至る経路は塞がれており、蒸留前に 1 行直せば足りる Minor と判断して APPROVED とする。

## 指摘事項

### [🟡 Minor / 優先度: 中] summary.md の「最終状態」節だけが移設に追随しておらず、存在しないパスを成果物として挙げている

**該当箇所**: `summary.md:5-8`

**問題点**: 「触ったファイル」節 (33-40 行) は移設後のパスと移設理由・rules.md の新カテゴリ追加まで正しく更新されている一方、確定サマリの本題である「最終状態 (何がどうなったか)」節は移設前のまま残っている:

- 7 行: 新規成果物を `kasane/concepts/maui/architecture/performance-verification.md` と記述 — このパスにファイルは存在しない (実体は `kasane/concepts/maui/conventions/performance-verification.md`)。`maui/architecture/` 自体は `view-materialization.md` を抱えて実在するため、archive 後にこの行だけを読んだ人は「あったはずの文書が消えている」と読む余地がある
- 8 行: 「`maui/index.md` に 1 行追加、`log.md` に created 行を append」 — 実際は index.md は `conventions/` 節の新設を伴い (3 行増)、さらに `rules.md` の platform カテゴリ表に新カテゴリ行を足している。**長命層 (rules.md) にカテゴリを新設したことが、最終状態の成果物リストに現れていない**

「最終状態」節は蒸留・後続レビューが最初に読む要約であり、成果物の所在と長命層への影響がここで完結していないと、突き合わせが「触ったファイル」節まで読み進める前提になる。

**推奨修正**: 7-8 行を移設後の最終状態に合わせる。例:

- 7 行のパスを `kasane/concepts/maui/conventions/performance-verification.md` に置換
- 8 行を「`kasane/concepts/rules.md` の platform カテゴリ表に `conventions/` 行を追加 (オーナー合意による新カテゴリ)、`kasane/concepts/maui/index.md` に `conventions/` 節を新設して 1 行追加、`kasane/concepts/log.md` に created 行を append」に置換

### [🔵 Suggestion] rules.md の「新カテゴリの条件」が、同じファイルで新設した `conventions/` 行と整合していない

**該当箇所**: `kasane/concepts/rules.md:35` / `kasane/concepts/rules.md:52-54`

**問題点**: 54 行の条件は「既存カテゴリに適切に収まらない概念が**3つ以上蓄積した場合に**、ユーザー合意の上で新設する」。今回追加した 35 行の `conventions/` は概念 1 本の段階でオーナー合意により新設されている (コンテキスト上、オーナー指示が条件に優先するという判断は妥当で、指摘は判断の是非ではない)。結果として rules.md 自身の中に、明文の条件とその反例が併存し、根拠が行内注記 (「2026-08-28 オーナー合意で新設」) だけになっている。

後続が同種の場面で「3 本溜まるまで待つ」のか「オーナーに諮れば 1 本でよい」のか、rules.md だけでは判断できない。なお `architecture/` 行 (2026-08-12 新設) も同じ構図なので、これは今回の変更が作った問題ではなく既存の運用と明文の乖離が 2 例目で見えたもの。

**推奨修正**: 蒸留時 (または concepts の手入れ) に 54 行へ例外を明示する。例:「3つ以上蓄積した場合に、ユーザー合意の上で新設する。既存カテゴリの定義に明確に反する種類の知識が 1 本目から出た場合は、オーナー合意を条件に先行して新設してよい (新設日と合意を該当行に注記する)」。長命層の予約ファイルなので、この change 内での単独判断による書き換えは求めない。

### [🔵 Suggestion] 蒸留への持ち越し (evidence 参照の archive 後パス確定) が summary.md に残っていない

**該当箇所**: `summary.md:44-48` (「決定事項 / ADR 候補」節)

**問題点**: review-002 Suggestion 3 の「本文 48 行の evidence 参照を archive 後の確定パスへ書き換える」は蒸留時対応として持ち越された。持ち越し自体は妥当だが、根拠が review-002.md の中にしかない。蒸留は summary.md を索引として読むため、review-NNN 本文まで戻らないと TODO に気付けない (review-002 自身も「summary.md か本レビューを根拠にすればよい」と両方を許容している)。

**推奨修正**: summary.md に 1 行足す。例:「蒸留時の TODO: `kasane/concepts/maui/conventions/performance-verification.md:48` の evidence 参照を `kasane/changes/archive/<archive 実行日>-customcell-android-maui-perf/evidence/gfxinfo-pixel6a.md` へ確定し、括弧書きの暫定注記を落とす (review-002 Suggestion 3)」。

## アクションプラン

1. **[Minor] `summary.md:5-8` を移設後の最終状態に合わせる** — 新規成果物のパスを `maui/conventions/` へ、成果物リストに rules.md の新カテゴリ追加を明記
2. **[Suggestion] `summary.md` に蒸留時 TODO (evidence 参照の archive 後パス確定) を 1 行残す**
3. **[Suggestion / 蒸留時] `rules.md:54`「新カテゴリの条件」にオーナー合意による先行新設の例外を明文化する**

## review-002 指摘の対応確認

| # | 重要度 | 指摘 | 判定 |
|---|---|---|---|
| 1 | 🟡 Minor / 高 | log.md の created 行が修正前の本文を記述 | **対応済み**。`log.md:324` は (a)「Debug の遅さは単独では実装欠陥の証拠にならないこと (実装を疑うかは Release の計測で判断)」= 本文 17 行と一致、(b)「iOS 側は未計測 (Simulator 経路の観察で非同一土俵、実機 Debug の乖離は推定にとどまる) と留保付きで」= 本文 50-54 行と一致、(c) 計測手順と証跡の所在にも言及、(d) パスも移設後の `maui/conventions/...`。撤回した断定は log から消えている |
| 2 | 🔵 Suggestion | 「振れ幅も大きい」が同節の脚注に打ち消されている | **対応済み**。本文 32 行は「**最良値 (p90 53ms) でも Release (12ms) と大差がある**ため Release の代替にはならない」を主根拠に置き換え、幅は「補足材料にとどまる (発熱が交絡し得る)」へ降格。証跡 (`evidence/gfxinfo-pixel6a.md:81`) の 1 回目 p90 53ms が最良値であることも確認 |
| 3 | 🔵 Suggestion | evidence 参照を archive 後パスへ (蒸留時) | **持ち越し (妥当)** — この change で archive 日は未定のため暫定形が正しい。ただし持ち越しの記録場所について上記 Suggestion |
| 4 | 🔵 Suggestion | 「触ったファイル」が evidence / 起票先を反映していない | **対応済み**。`summary.md:39-40` に `evidence/gfxinfo-pixel6a.md`・review-001/002・別 change `kasane/changes/maui-android-customcell-embed-perf/exploration.md` が入り、移設後パスと移設理由も併記。ただし同じ更新が「最終状態」節に及んでいない → 上記 Minor |
| — | (review-001 #8 の持ち越し) | `type: policy` を platform/architecture に置く前例が無い | **解消**。architecture/ の主な type を広げる代わりに、`conventions/` (主な type = policy) を新設して移設する形で決着。rules.md:35 の書式は既存の architecture/ 行 (「(YYYY-MM-DD オーナー合意で新設。〜 で使用)」) と一致 |

## 確認した観点 (問題なしと判定したもの)

- **移設の完全性**: 実ファイルは `kasane/concepts/maui/conventions/performance-verification.md` のみ (`find kasane/concepts/maui -type f` で `architecture/` 配下は `view-materialization.md` だけ、旧ファイルの残骸なし)。旧パス文字列の grep ヒットは review-001 (7 箇所)・review-002 (3 箇所)・session.md (1 箇所)・summary.md:7 の 4 ファイル。前 3 者は**書かれた時点の事実を記録する履歴**であり書き換え対象ではない (ksn-review「足場アーティファクトの書き換えを指示しない」と同じ理由)。実質の残骸は summary.md:7 のみ → 上記 Minor
- **index.md の節構成**: `maui/index.md` は `api/` → `architecture/` → `conventions/` の順で、`cross/index.md` (`architecture/` → `conventions/`) と同じ並び。行形式も既存と同一の「リンク — 1 行説明」で、1 行説明から適用場面 (何を測るときに読むか) が読める。ルートの `concepts/index.md` はドメイン地図のみで概念行を持たないため更新不要 (正しく触れられていない)
- **本文の相対リンク 3 本**: `../../android/architecture/build-toolchain.md` / `../../cross/conventions/test-execution.md` / `../../cross/conventions/runtime-behavior-verification.md` — いずれも実在を確認。移設で 1 階層の深さは変わっていない (`maui/architecture/` → `maui/conventions/` はどちらも `concepts/<domain>/<category>/`) ため `../../` は正しいまま
- **rules.md の書式整合**: 追加行 (35 行) は既存の architecture/ 行と同じ列構成・同じ注記書式で、`主な type` は `policy` 単独。`cross/` 表の `conventions/` 行 (主な type = policy) とも整合。カテゴリ名も cross と揃っており、「platform 固有の開発規約 (計測・検証などの決まり事)」と cross の「命名・API 対称性・横断的な開発規約」で棲み分けが読める
- **log.md の形式**: `## 2026-08-28` 見出し配下への append (324 行、末尾)。既存の `created:` 行 4 件と同じ書式 (概念パス — 内容要約 (change: 〜) — timestamp) で、記述内容は移設後の最終状態と一致
- **summary.md と実 diff の一致**: `git status --porcelain` は `M kasane/concepts/log.md` / `M kasane/concepts/maui/index.md` / `M kasane/concepts/rules.md` / `?? kasane/changes/customcell-android-maui-perf/` / `?? kasane/changes/maui-android-customcell-embed-perf/` / `?? kasane/concepts/maui/conventions/` の 6 件。`git diff develop...HEAD` は空 (コミットなし)、`git diff HEAD --stat` は 3 ファイル 6 行追加のみ (削除ゼロ)。**samples/ の差分はゼロを維持**し、`samples/maui/README.md` / `KsSettingsView.Sample.Maui.csproj` に未追跡・未コミットの残骸なし
- **証跡と本文の数値整合 (再確認)**: `evidence/gfxinfo-pixel6a.md` の生値 → 本文の表を再突き合わせ。native 6.09%→6.1% / 28ms、Debug 31.69%→31.7% / 121ms、Release 4.59%→4.6% / 12ms、`UseInterpreter=false` 8.82〜19.44%→8.8〜19.4% / 53〜65ms。summary.md・exploration.md の表とも一致、丸め以外の差異なし。本文 30 行の計測回数 (Debug/Release/native 各 1 回、`UseInterpreter=false` のみ連続 2 回) も証跡 25 行と一致
- **計測手順の再現性 (再確認)**: 本文 36 行の「README の実機手順は `-c Debug` 固定」は現物と一致 (`samples/maui/README.md` は `-c Debug` 3 件 / `-c Release` 0 件)
- **lint**: `python3 scripts/identity-lint.py` / `python3 scripts/local-path-lint.py` をリポジトリ全体で実行し双方 exit 0。両者とも `git grep --untracked` 経路のため、未追跡の新規 concepts (`maui/conventions/`) と change 配下も検査対象に入っている。成果物内のパスは全てリポジトリ相対 / 概念間相対で、ローカル絶対パスなし
- **ビルド / テスト**: ソースコードの diff がゼロ (変更 3 ファイルはすべて `kasane/` 配下の .md、新規も .md のみ)。挙動に触れていないため HEAD (0a5a1f3) 時点のビルド・テスト結果がそのまま有効で、summary.md の「テスト不要」判断は妥当。今回の修正 (log 行の書き換えと移設) でこの前提は変わっていない
- **修正による新規の不整合**: 移設に伴って壊れ得る箇所 (本文の相対リンク・index の節・rules のカテゴリ・log のパス・他 concepts からの被リンク) を全て確認。他の concepts から本文書への被リンクは存在しないため、移設で壊れた参照はない。新たに混入した不整合は summary.md:7-8 の 1 箇所のみ
- **concepts 規約適合**: frontmatter 5 項目具備、h1 = title 一致、「この文書を読むと〜」の宣言あり。type: policy と `conventions/` カテゴリ (主な type = policy) が一致し、review-001 #8 が指摘した「前例のなさ」は配置側で解消。ドメイン導出規則 (maui/ ビルドルートに閉じる知識 → maui ドメイン) にも適合
- **lessons/code-review.md**: 重点観点 L-001 (ミューテーションによる検出力の実測) はテストコードを含まない本 change に非該当。「指摘しないこと」は空 (昇格済みルールなし)
