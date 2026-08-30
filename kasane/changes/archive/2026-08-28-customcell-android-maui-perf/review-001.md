# レビュー結果: customcell-android-maui-perf (001 回目)

**日付**: 2026-08-28
**判定**: CHANGES_REQUESTED

## サマリー

summary.md の主張と実 diff は一致しており、成果物 (concepts への policy 1 本 + index/log 各 1 行) は配置・形式・リンクとも規約に適合している。`samples/` の完全復帰、lint 2 種の通過、log/index の書式整合、そして本文の中核である「MAUI Android の Debug は既定で Mono インタープリタ実行」という機構の主張まで、こちら側で独立に裏取りして正しいことを確認した (裏取りの内容は末尾「確認した観点」)。

一方で、この change の唯一の成果物が**長命層に置かれる実測ベースの policy** であるにもかかわらず、(1) 断定形で書かれた iOS 側の機構が未計測の推定であり本プロジェクトの iOS 検証経路 (Simulator) では成り立たない、(2) 数値の証跡が change 配下に一切残っておらず既存規約 `cross/conventions/runtime-behavior-verification.md` の要求を満たしていない、の 2 点は長命層に入れる前に潰すべきと判断した。いずれも本文の追記・証跡の貼り付けで閉じる軽い修正で、規約本体 (「性能は Release で測る」) の妥当性には異論がない。

## 指摘事項

### [🟠 Major] iOS 非対称の機構が未計測の推定を断定形で書かれている (かつ本プロジェクトの iOS 検証経路には当てはまらない)

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:32-34`

**問題点**:

本文は「iOS は Debug でも AOT 混在で動くため、この乖離が小さく問題として顕在化しにくい」と断定形で書いている。しかし:

- 実測値の表 (同ファイル 22-28 行) に iOS の行はない。Android 3 構成 + native は実測だが、iOS は**測られていない**。同じ節の中で、実測に裏打ちされた記述と未計測の推定が同じ断定の語調で並んでいる (exploration.md では同じ内容が「〜と推定」と留保付きで書かれていた記述が、concepts へ移る過程で留保を失っている)
- 機構としても不正確。`Microsoft.iOS.Sdk` の既定では `MtouchInterpreter` は空 = インタープリタ無効で、実機 Debug は AOT 主体になるためこの記述は実機には当てはまるが、**本プロジェクトの iOS 検証経路は Simulator** (`samples/maui/README.md:86-93` が `-f net10.0-ios -c Debug` → `iossimulator-arm64/*.app`) であり、Simulator は JIT で動くので「AOT 混在だから速い」は成り立たない。しかも Simulator の描画性能は Mac の性能であって実機の性能ではないため、「iOS は問題なし」という観察自体が Android の実機観察と同じ土俵にない

長命層 (ksn-drift のディープ検証対象) に置かれる policy が、検証できない推定を実測と同じ確度で提示している状態は、後続がこの一文を根拠に iOS 側の調査を打ち切るリスクを持つ。

**推奨修正**: 次のいずれか。

- 実測しないなら留保を戻す: 「iOS は Debug でも (実機は AOT 主体で動くため) 乖離が小さいと**推定**される。iOS 側は本 change では計測していない」と明記し、あわせて「本リポジトリの iOS 実機/Simulator 手順のどちらで観察したかで意味が変わる (Simulator は JIT かつ Mac の性能)」を 1 行添える
- あるいは iOS 実機で 1 構成だけ測って表に行を足し、断定を維持する

### [🟠 Major] 実測値の証跡が change 配下に無く、`runtime-behavior-verification.md` の要求を満たしていない

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:19-30` / change 配下 (`evidence/` が存在しない)

**問題点**:

本 change は「実行時にしか現れない症状 (スクロールのカクつき)」の調査であり、既存規約 `kasane/concepts/cross/conventions/runtime-behavior-verification.md` の適用範囲 (「症状が実行時にしか現れない不具合。目安はユニットテストでその症状自体を再現できるか」) に真正面から当たる。同規約は完了条件の 3 番目として「**証跡 (スクリーンショット・ログ等) を change 配下に残す。レビューと蒸留が『解消した』の主張を検証できる形にする**」を課しているが、change 配下には exploration.md / session.md / summary.md しかなく `evidence/` が無い。

結果として、レビュー側は表の 4 行 (31.7% / 8.8〜19.4% / 4.6% / 6.1%) を一切検証できない。しかもこの数値は change と共にアーカイブされて消えるのではなく、**concepts の長命文書へ恒久的に転記されている**。出典が「Pixel 6a 実機の `dumpsys gfxinfo` 実測」という文字列だけで、元の出力・計測手順 (フリング回数・リセットの有無・計測ウィンドウ) が残らないため、将来 ksn-drift が「この数値は今も正しいか」を再検証する手段がない。ksn-core `references/evidence.md` はまさにこの用途に `changes/<id>/evidence/` の抜粋ログを規定している。

**推奨修正**: 4 構成それぞれの `dumpsys gfxinfo` 出力から判定に使った行 (Total frames / Janky frames / 50th・90th・95th・99th percentile) を抜粋し、`python3 scripts/log-sanitize.py` を通したうえで `kasane/changes/customcell-android-maui-perf/evidence/gfxinfo-<構成>.txt` として残す。あわせて concepts 本文の実測値表の直後に、計測手順 (同一画面・フリング回数・`dumpsys gfxinfo reset` の有無) を 2〜3 行で書き、後続が同じ条件で再計測できる状態にする。

### [🟡 Minor] 「Release で測れ」と規定しながら、その手順アンカーがどこにも無い (README の実機手順は全て `-c Debug` のまま)

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:15` / `samples/maui/README.md:86-87,110-111,120-121`

**問題点**:

exploration.md が「サンプル手順の構造問題」として挙げたのは *「README の実機手順がすべて `-c Debug`、手順どおりだと必ず Debug の性能を見てしまう」* という**落とし穴の存在**だった。README を編集しない判断 (利用者向けドキュメントに開発者向け知識を書かない) は妥当だが、その結果、落とし穴の存在自体がどこにも記録されないまま change が閉じようとしている。新しい concepts 文書は「必ず Release で行う」と命じるだけで、Release でどう建てて流し込むか (`-c Release` / `-t:Run` / `dumpsys gfxinfo` の使い方) も、「README に載っている実機手順は Debug 固定なので性能評価にはそのまま使えない」という注意も持っていない。

規約を読んだ開発者が次に取る行動は「README の手順で実機に入れる」であり、それは規約に違反した計測になる。規約と、開発者が実際に辿る手順との間に落とし穴が残ったままになる。

**推奨修正**: README は触らず、concepts 文書側に「計測手順」節 (数行) を足す。`samples/maui/README.md` の実機手順が `-c Debug` 固定であること、性能評価時は `-c Release` に置き換える必要があることを明記し、実際に使った build / run / 計測コマンドを 1 セット載せる。これで「どこに落とし穴があるか」の知識が長命層に残る。

### [🟡 Minor] 「Debug の遅さそのものは不具合ではない」の一般化が強すぎる

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:17`

**問題点**:

「**Debug の遅さそのものは不具合ではない**ため、Debug の観察を根拠に実装を疑ってはならない」は、この change で観測した事象 (一定倍率の全体的な鈍化) には正しいが、規則としては広すぎる。インタープリタ実行は定数倍のペナルティであって、実装側に計算量やアロケーションの問題があれば Debug ではその欠陥が**増幅されて先に見える**。「Debug で遅い」だけを理由に調査を打ち切ると、Release では 1 フレーム予算にぎりぎり収まっているだけの実装欠陥 (本 change 自身が exploration.md で挙げた非 reusable な `AndroidView` 埋め込み等) を見逃す。

concepts 規約は「保証・禁止は規則の列挙で止めず、その規則がないと何が壊れるかを添える」を求めており、ここは禁止の射程を絞るべき箇所。

**推奨修正**: 「Debug の遅さは**それ単独では**不具合の証拠にならない。実装を疑うかどうかは Release の計測で判断する」に緩める。あわせて「Release でも予算を超えるなら実装側を疑う」という肯定側の判断基準を 1 行添えると、規則が両方向に使える。

### [🟡 Minor] 見送った構造課題 2 件が exploration.md にしか残らない

**該当箇所**: `exploration.md` (「構造的な容疑者」節) / summary.md:29

**問題点**:

調査中に「実在するが Release では支配項でないため見送り」と判定された構造課題が 2 件ある — MAUI 埋め込み (`AndroidView`) が `onReset` を渡さず非 reusable なため行リサイクルのたびにノード強制置換 + view 再親付けが起きる件と、`KsAccessoryHostView.OnMeasure` に measure キャッシュが無く毎回 MAUI 全ツリーを計測する件。どちらも「存在する」と結論が出た実在の課題であり、見送りは性能上の優先度判断にすぎない。

これらは現状 exploration.md (アーカイブされる足場アーティファクト) にしか記録がなく、summary.md も「詳細は exploration.md」と参照するだけ。change がアーカイブされると、change-id を知っている人以外には発見不能な知識になる。ksn-live は「本題とは別の発見は簡易起票して対話を続ける」を規律として持っており、この 2 件はまさにその対象。

**推奨修正**: 2 件をまとめて簡易起票 (`kasane/changes/<新 id>/exploration.md` のスタブ) し、見送り理由 (Release では支配項でない) と、着手時の前提 (`kasane/concepts/maui/architecture/view-materialization.md` が `AndroidViewHolder.onReuse` の子側 addView リスクを明示警告している) を書き写す。summary.md の該当行から起票先を指す。

### [🔵 Suggestion] `UseInterpreter=false` の値だけ幅を持ち、n も条件も書かれていない

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:26,30`

**問題点**: 他の 3 行が単一値なのに対し `Debug + UseInterpreter=false` だけ「8.8〜19.4% / 53〜65ms」と 2 倍以上の幅を持つ。本文はこの幅を「振れ幅も大きいため Release の代替にならない」の根拠に使っているが、何回測った幅なのか、他の構成は何回測って単一値になっているのかが書かれていないため、読者はこの幅を「この構成固有の不安定さ」と読むべきか「全構成にある計測ばらつきがこの行にだけ露出した」と読むべきか判断できない。前者なら根拠として有効、後者なら他の行の単一値も同程度の幅を持つはずで根拠にならない。

**推奨修正**: 各構成の計測回数を表の脚注に 1 行で書く。全構成が複数回計測なら、他の行も範囲か中央値であることが分かる表記に揃える。

### [🔵 Suggestion] 関連リンクに `runtime-behavior-verification.md` が無い

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:36-39`

**問題点**: 「関連」に挙がっているのは build-toolchain と test-execution だが、内容的に最も近い隣人は `cross/conventions/runtime-behavior-verification.md` (実行時挙動の症状は実環境で再現・計測して裏取りする、証跡を残す)。本文書はその規約に「どのビルド構成で測るか」という欠けていた 1 段を足す関係にあり、相互に辿れないと片方だけ読んだ人が半分の規律しか適用しない。

**推奨修正**: 本文書の「関連」に `../../cross/conventions/runtime-behavior-verification.md` を追加する (逆方向のリンク追加は既存文書の改訂になるため、蒸留時に判断で足す)。

### [🔵 Suggestion] `type: policy` を `maui/architecture/` に置いた前例が無い

**該当箇所**: `kasane/concepts/maui/architecture/performance-verification.md:2` / `kasane/concepts/rules.md:29-35`

**問題点**: rules.md のカテゴリ表で `<platform>/architecture/` の「主な type」は `concept` のみで、`policy` は `cross/conventions/` の主 type として定義されている。既存 44 概念で `type: policy` を持つのは rules.md と `cross/conventions/` 配下の 5 本だけで、platform ドメイン配下に policy が置かれるのは本文書が初。

配置自体はドメイン導出規則 (maui/ ビルドルートに閉じる知識) とカテゴリ定義 (「platform のビルドツールチェーンの契約」) の両方に合致しており妥当と判断する。指摘は前例の無さの明示だけで、変更を求めるものではない。

**推奨修正**: 蒸留時にオーナー合意を取り、rules.md の `<platform>/architecture/` 行の「主な type」を `concept, policy` に更新する (後続が同種の判断で迷わないようにするため)。

## アクションプラン

1. **[Major] iOS 節に留保を戻す** — 未計測であることを明記し、Simulator/実機で意味が変わる点を添える (または iOS 実機を 1 構成測って表に足す)
2. **[Major] evidence を残す** — 4 構成の `dumpsys gfxinfo` 抜粋を sanitize して `evidence/` へ。concepts 本文に計測手順を 2〜3 行追記
3. **[Minor] 計測手順節を追加** — README の実機手順が `-c Debug` 固定である落とし穴と、Release での build/run/計測コマンドを concepts へ (README は触らない)
4. **[Minor] 「不具合ではない」の射程を絞る** — 「単独では証拠にならない / Release で超えるなら実装を疑う」へ
5. **[Minor] 見送った構造課題 2 件を簡易起票** — summary.md から起票先を指す
6. **[Suggestion] 計測回数の脚注 / 関連リンク追加 / rules.md の主な type 更新 (蒸留時)**

## 確認した観点 (問題なしと判定したもの)

- **summary.md の主張と実 diff の一致**: `git status --porcelain` は `M kasane/concepts/log.md` / `M kasane/concepts/maui/index.md` / `?? kasane/changes/customcell-android-maui-perf/` / `?? kasane/concepts/maui/architecture/performance-verification.md` の 4 件のみ。`git diff HEAD -- samples/ maui/ android/ ios/ core/` は空で、未追跡の残骸も無い。**「samples/ は完全復帰」は正しい**。`git stash list` も空 (試行の退避残りなし)
- **concepts 規約適合**: frontmatter 5 項目 (type/title/description/tags/timestamp) 具備、h1 = title 一致、h1 直後に「この文書を読むと〜」の宣言あり (可読性規約)。日本語 + 技術用語は英語 (`UseInterpreter` / `dumpsys gfxinfo` / Janky frames)、コードから再導出できない外部事実のみで価値 lint を通る。新造語なし。関連リンクは実配置基準の相対パスで、`../../android/architecture/build-toolchain.md` と `../../cross/conventions/test-execution.md` の両方とも実在を確認
- **index.md / log.md の形式整合**: index は `architecture/` 節にアルファベット順で挿入され、既存行と同じ「パス — 1 行説明」形式。log は `## 2026-08-28` 見出し配下への append で、既存の `created:` 行と同じ書式 (概念パス — 内容要約 (change: 〜) — timestamp)
- **本文の機構主張の裏取り** (レビュー側で独立に検証): `Microsoft.Android.Sdk.Darwin/36.1.53/targets/Microsoft.Android.Sdk.DefaultProperties.targets:82` が `Configuration == Debug` で `UseInterpreter` を既定 `true` にしており、同 30-34 行で `UseMonoRuntime` 既定 `true` → `_AndroidRuntime = MonoVM`、`AssemblyResolution.targets:240` が `AndroidUseInterpreter == true` で `hot_reload` コンポーネントを取り込む。**「Debug は既定で Mono インタープリタ実行 (C# Hot Reload を成立させるため)」は SDK の既定値どおりで正しい**。同 118 行で Release かつ MonoVM のとき `RunAOTCompilation` が既定 `true` になることも Release 側の説明と整合する。リポジトリ内に `UseInterpreter` の明示指定は無く (`grep` で 0 件)、既定が効く状態であることも確認
- **数値の内的整合**: summary.md の表と exploration.md の表で Debug 31.7%/121ms・Release 4.6%/12ms・native 6.1%/28ms が一致。concepts 本文の 4 行とも summary と一致。相互矛盾なし
- **ローカル絶対パス・個体特定情報**: `python3 scripts/local-path-lint.py` / `scripts/identity-lint.py` を新規・変更・change 配下の全ファイルに実行し、いずれも exit 0。端末は「Pixel 6a」という機種名のみでシリアル・UDID・ホスト名・氏名の混入なし
- **ksn-live の確定手順**: summary.md は Step 4 のテンプレート 4 節をすべて備え、却下試行 2 件が理由付きで記録されている。出典にない創作なし
- **ビルド / テスト**: ソースコードの diff がゼロ行 (上記 `git diff` で確認) のため、HEAD (蒸留済みコミット) 時点のビルド・テスト結果がそのまま有効。挙動に触れていないため summary.md のテスト不要判断は妥当と判断し、フルビルドは実行していない
- **lessons/code-review.md**: 重点観点 L-001 (ミューテーションによるアサーション検出力の実測) はテストコードを含まない本 change には該当なし。「指摘しないこと」は空
