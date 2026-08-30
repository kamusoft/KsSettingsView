# セカンドオピニオン: consolidate-readmes-and-contribution (code-001)
**レビュアー**: ホスト側 2 人目の独立レビュアー (相方 CLI の利用枠切れによる代替。オーナー承認済み) / **日付**: 2026-08-29 / **対象**: HEAD からの作業ツリー差分 (untracked 含む)
---

**判定**: CHANGES_REQUESTED

## サマリー

デルタスペック 2 能力の Requirement / Scenario は、機械的に検査できるものをすべて実測で確認した限り満たされている (README 集合 5 枚・翻訳ロックステップの見出し 17 個完全一致・最小コード例 3 platform の SKILL.md との逐語一致・配布座標 3 ecosystem の一致・`KsSettingsView-Swift` の残存ゼロ・Issue Forms の必須項目 8 個・内部リンク全解決・lint 2 本 exit 0・スクリーンショット 4 枚の目視検査)。移送先の 3 concept は移送元より情報量が増えており、質は高い。

一方で、**この change が書き換えた側 (ルート README) を「知識の正」として参照していた長命層の記述が更新されていない**。`test-execution.md` は削除された記述について今も事実として語っており、`maui/ADR-0006` は削除された `maui/README.md` の表を保守対象として指し続けている。いずれも spec の Scenario が列挙した検査対象 (ルート README 2 枚・`skills/`・`.github/`・docs-refresh SKILL.md) の外側で、tasks 8.3 の検査範囲もそこで止まっているため見逃されている。Requirement「開発者向け知識の所在」が狙った「concepts → README の逆転の解消」は `native-bridge.md` の 2 箇所だけでは閉じていない。

以下、Major 2 件・Minor 5 件・Suggestion 4 件。

---

## 指摘事項

### [🟠 Major] test-execution.md が、この change が消した README の記述を今も事実として語っている

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md:60-62`

**問題点**:

当該節はこう書いている。

> ### README の `swift test` との関係
> `README.md` はパッケージを手早く確認する手順として `swift test` を案内している。あれは利用者向けの動作確認であり、**本規約の完了判定とは別物**である。

`swift test` は旧ルート README の「ビルド方法」節にあった (`git show HEAD:README.md:84`)。本 change はルート README を全面置換し、spec Requirement「ルート README の節構成」の「開発者向けのビルド手順・環境セットアップ手順・モジュール一覧を持たない SHALL」に従ってこの節ごと削除している。実測: `grep -n "swift test" README.md README_ja.md` → 該当なし。

つまり長命層 (L2) が、存在しない記述の存在を断定し、それを前提に規範 (「開発時の検証には使わない」) を述べている状態になった。`timestamp` も `2026-08-22` のまま更新されていないため、drift 検出の鮮度チェックにも掛からない。

さらにこの節は、Requirement「開発者向け知識の所在」の「concepts から README を知識の正として指す参照を持たない SHALL」に照らしても該当する形の参照である。tasks 1.3 / 4.2 が対象にしたのは `native-bridge.md` と「廃止した 5 枚」への参照だけで、**置換されたルート README への参照**は誰も見ていない。

**実害のシナリオ**:
iOS の変更を担当するエージェント (または contributor) が `test-execution.md` を規約として読み込む。「README に `swift test` の案内がある」と書かれているので、利用者向けの簡易確認手順がどこかにあると信じて README を探す。見つからないため、(a) README が壊れていると誤認して報告する、(b) 自分の読み落としと判断して `skills/` を探し回る、(c) 「案内がある」を根拠に README へ `swift test` を書き戻す — のいずれかに転ぶ。(c) が起きると ADR-0023「ルート README は利用者の入口に純化する」を静かに破る。これは design.md Decision 2 が docs-refresh 側で塞いだのと同型の再導入経路が、concepts 側に開いたまま残っているということ。

**推奨修正**:
`test-execution.md` の当該節を、ルート README の現状に合わせて改める。最小の形は節ごと削除 (混同の元が消えたので節の存在理由が消滅した)。残すなら「ルート README・`skills/` とも `swift test` を含むテスト実行手順を案内していないため、混同の余地はない」へ書き換える。いずれの場合も `timestamp` を更新する。

---

### [🟠 Major] accepted な maui/ADR-0006 が、削除された `maui/README.md` の表を保守対象として指している

**該当箇所**: `kasane/decisions/maui/0006-android-binding-gradlew-exec.md:23`

**問題点**:

> SDK 側が複数モジュール構成に対応した時点でこの決定は見直す。再検証の入口は `maui/README.md` の「SDK 更新時に再検証する箇所」の表と対で維持する。

`maui/README.md` は tasks 4.1 で削除され、当該の表は `kasane/concepts/maui/architecture/binding-build-integration.md:78-93` へ移送された。ADR-0006 は `status: accepted` (line 4) で、移送先を知らない。

逆向きの導線も無い。`binding-build-integration.md` の「関連」(line 97-101) は `native-bridge.md` / `integration-host-verification.md` / `build-toolchain.md` の 3 本だけで、**maui/ADR-0006 への言及が 1 箇所もない**。`native-bridge.md:101` の ADR 一覧に「基盤は maui/ADR-0001〜0007」と包括的に書かれてはいるが、ADR-0006 → 表 の対応関係はどちらの向きからも辿れない。

design.md Decision 3 は `kasane/` を「公開ドキュメント面」の対象外としたが、その理由は「archive の検証証跡 README 4 枚を書き換えたくない」であって、**live な accepted ADR からの参照整合まで免除する趣旨ではない**。tasks 8.3 の検査対象も `kasane/` を含まないため、この 1 件は検査網の穴に落ちている。

**実害のシナリオ**:
.NET workload を上げた開発者が maui/ADR-0006 を読み、「再検証の入口」として `maui/README.md` を開く → 存在しない。ADR-0006 が指す「SDK 更新時に再検証する箇所」の表 (`_BuildXcodeProjects` / `_XcbInputs` / `_CategorizeAndroidLibraries` への割り込み一覧) に辿り着けず、SDK 内部ターゲットへの割り込みが壊れていないかの確認をスキップする。この割り込みが黙って外れると「Swift を直しても古い xcframework のままビルドが通る」(旧 `maui/README.md:175-178`) という無言の不整合が発生する。

**推奨修正**:
ADR は accepted 後は不変 (ksn-core references/decisions.md:55) なので、ADR 本文の書き換えは採らない。次のいずれかで導線を張り直す。

1. `binding-build-integration.md` の「SDK 更新時に再検証する箇所」節 (line 78) に「本節は maui/ADR-0006 が『再検証の入口』として指す表である」と明記し、「関連」に maui/ADR-0006 を追加する。ADR → 表 の向きは辿れないままだが、表 → ADR が張られるので蒸留時 / drift 時に対応が見える。
2. 1 に加えて `deviation.md` に「accepted ADR からの参照が 1 件切れた (ADR-0006:23)、移送先は binding-build-integration.md」を記録し、supersede が要るかの判断を蒸留 (ksn-distill) へ送る。

本レビューは 1 + 2 の併用を推奨する。ADR 本文をどう扱うか (supersede か放置か) は設計判断なので、その部分だけは NEEDS_DISCUSSION 相当として扱ってよい。

---

### [🟡 Minor] 日本語 README の「貢献ガイドライン」リンクが英語版を指している

**該当箇所**: `README_ja.md:159`

**問題点**:
```
Issueを投稿する前に[貢献ガイドライン](.github/CONTRIBUTING.md)を確認してください。
```
`.github/CONTRIBUTING_ja.md` が存在する (5.5 で作成済み、`CONTRIBUTING.md:3` から相互リンクもある) のに、日本語 README は英語版へ送っている。英語 README (`README.md:159`) が `.github/CONTRIBUTING.md` を指すのは正しいので、この 1 行だけが言語ペアの対称性を崩している。同じファイル内の他のリンクは正しく言語別になっている (`README_ja.md:38` → `skills/README_ja.md`、`:139` → `skills/ja/...`)。

**実害のシナリオ**:
日本語 README だけを読む投稿者が「貢献ガイドライン」をクリックし、英語ドキュメントに着地する。`CONTRIBUTING.md:3` の `[日本語]` リンクに気づけば戻れるが、気づかなければ PR 非受付の理由 (Kasane フローの文脈保持) を読まずに Issue を書く。ADR-0024 が「投稿前に目に入る経路が 3 つになる」と数えた導線の 1 本が、日本語話者にとって半分しか機能しない。

**推奨修正**: `README_ja.md:159` のリンク先を `.github/CONTRIBUTING_ja.md` へ変更する。

---

### [🟡 Minor] concepts/log.md が append-only の既存エントリを新しい日付見出しの下へ移してしまっている

**該当箇所**: `kasane/concepts/log.md:331` (挿入した見出し) / `:334` (巻き込まれた既存行)

**問題点**:
新しい `## 2026-08-29` 見出しをファイル末尾ではなく **既存の最終行の直前** に挿入したため、`- distilled: skills-api-coverage ...` (HEAD 時点ではファイル最終行、`## 2026-08-28` 節の所属) が `## 2026-08-29` 節へ移動した。

結果、同じ蒸留に属する 2 行が日付見出しをまたいで割れている:
- `:329` `created: cross/conventions/user-skill-api-listing.md` (本文に「2026-08-29 オーナー合意」と書いてある) → `## 2026-08-28` 節に残留
- `:334` `distilled: skills-api-coverage` (本文に「2026-08-29 オーナー判断」) → `## 2026-08-29` 節へ移動

さらに新節の並びが `本 change の updated` → `無関係な skills-api-coverage の distilled` → `本 change の updated` になっており、1 節の中で 2 つの change が交互に現れる。

log.md は append-only の履歴 (ksn-core: 「`index.md` / `log.md` / `rules.md` は … 項目の列挙が正しい形」で構造 lint の対象外だが、履歴としての改変は別問題)。既存記録の日付所属を後続 change が変えるのは、履歴の正確さを損なう。

**実害のシナリオ**:
後日「skills-api-coverage はいつ蒸留したか」を log.md で引くと 2026-08-29 節に出るが、同じ蒸留で作った concept は 2026-08-28 節にある。同一作業の記録が 2 日に割れているため、時系列の再構成 (drift 棚卸しや振り返り) で「28 日と 29 日に別々の作業があった」と誤読される。

**推奨修正**: `## 2026-08-29` 見出しと本 change の 2 行を `:334` の後ろ (ファイル末尾) へ移し、`distilled: skills-api-coverage` を元の `## 2026-08-28` 節へ戻す。既存行の日付所属は本 change の関知するところではない (前 change の起票ミスなら別 change / 蒸留で扱う)。

---

### [🟡 Minor] MAUI のテストコマンドが test-execution.md から分断され、双方向の導線がない (deviation の要レビュー項目への回答)

**該当箇所**: `kasane/concepts/cross/conventions/local-development-setup.md:151-155` / `kasane/concepts/cross/conventions/test-execution.md:15`

**問題点**:
deviation.md が「MAUI のテストコマンドの置き場が `local-development-setup.md` でよいか。独立レビューの判断に委ねる」として明示的に判断を求めている項目。結論は **現状の置き場は許容できるが、片方向の穴が残っている**。

- `test-execution.md:15` は今も「現時点では iOS と Android を記載する。MAUI (`maui/` ビルドルート) は、実際に実行して確かめた時点で追記する (未検証の手順は書かない)」と書いている。**この記述は更新されていない**。
- 一方 `local-development-setup.md:151` には「facade の純ロジック test は次で実行できる。このテストコマンドは、現時点では本書でのみ案内する」として `dotnet test maui/KsSettingsView.Maui.Tests/...` が置かれた。

結果、「テスト実行の正しいコマンドはどこか」の入口である `test-execution.md` を読んだ人は、MAUI について何も記録がないと判断する。実際には別ファイルにある。

加えて、`test-execution.md:13` が全 platform に課している規律 —「テストが 1 件も実行されなくてもコマンド自体は成功で終わる。**実行件数を確認するところまでが検証**」— が MAUI のコマンドには付いていない。`local-development-setup.md:151-155` はコマンドだけを置いており、件数確認の指示も、`test-execution.md` への参照も持たない (関連節 `:172` に一般リンクがあるのみ)。

なお、本 change 自身がこのコマンドを実行して確かめた形跡は tasks / deviation のいずれにもない。移送元 `git show HEAD:maui/README.md:39` にあったものの転記であり、内容の捏造ではない。`maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` は `Microsoft.NET.Test.Sdk` + `NUnit3TestAdapter` を持つ標準構成なのでコマンド自体は妥当である。

**実害のシナリオ**:
MAUI facade を変更したエージェントが `test-execution.md` を規約として読み、「MAUI は未記載」を「MAUI にはテストがない / 実行不要」と解釈して facade のユニットテストを回さずに完了報告する。あるいは `local-development-setup.md` のコマンドを見つけて実行し、`dotnet test` が 0 件で成功したことに気づかずに「テスト pass」と報告する (`test-execution.md:13` がまさにこの事故を止めるために置かれた規律)。

**推奨修正**:
1. `test-execution.md:15` に「MAUI facade の純ロジック test の実行コマンドは [local-development-setup.md](local-development-setup.md) にある (実行件数の確認は本規約に従う)」の 1 行を足す。これは「未検証の手順を本文へ書かない」というその文の趣旨を破らない。
2. `local-development-setup.md:151` の直後に「実行件数の確認までが検証であることは [テスト実行規約](test-execution.md) に従う」を足す。
3. 実際に `dotnet test` を走らせて件数を確認できたなら、`test-execution.md` に MAUI 節を新設して本体を移すのが本来の姿。ただしそれは本 change のスコープを超えるため、1 と 2 で導線を閉じたうえで蒸留へ申し送るのでよい。

---

### [🟡 Minor] 「A: 他所に既出だから破棄」の根拠が、同じ change で消した旧ルート README だった

**該当箇所**: `design.md` 移送対応表の `android/README.md`「ビルド・テスト」行 / 検証結果は `kasane/concepts/cross/conventions/local-development-setup.md` (該当記述の不在)

**問題点**:
design.md の移送対応表は `android/README.md`「ビルド・テスト」を「**移送しない** — `cross/conventions/test-execution.md` とルート README に既出」と分類している。tasks 1.11 は「A 分類は実在確認してから破棄する」を要求している。

しかし「ルート README に既出」の側は、**同じ change がルート README を全面置換して消滅させている**。旧ルート README の「ビルド方法」節 (`git show HEAD:README.md:74-107`) にあった `swift build` / `./gradlew build` / `dotnet build KsSettingsView.Maui.csproj` は、新 README には spec の要求により 1 つも無い。したがって 1.11 の「実在確認」は、確認した時点でどちらの版を見たかによって結論が変わる。

実測すると、旧 `android/README.md:80-106` の内容のうち次が現在どこにも無い:

| 旧記述 | 現在の所在 |
|---|---|
| `cd android && ./gradlew test` | `test-execution.md:69-71` にある ✓ |
| `cd android && ./gradlew build` (全モジュールビルド) | **無い** |
| `./gradlew lint` | **無い** |
| `./gradlew :ks-settingsview-core:assembleDebug` 等の個別モジュール build | **無い** (`test-execution.md:76` にあるのは `testDebugUnitTest` の絞り込みのみ) |

`local-development-setup.md` は Sample の実行を主題にしており、本体ライブラリのビルド / lint コマンドは持たない (`:107` の `:app:assembleDebug` は Sample アプリのもの)。

**実害のシナリオ**:
Android 本体を触った contributor / エージェントが lint を回そうとして、`AGENTS.md` → `kasane/concepts/` を辿っても `./gradlew lint` に行き着かない。`./gradlew tasks` から自力で探せば見つかる程度の再導出コストなので実害は限定的だが、**分類手続きそのものが循環していた**点は次の移送作業でも再発する。

**推奨修正**:
1. `local-development-setup.md` に「本体ライブラリのビルドと lint」の小節を足し、`cd android && ./gradlew build` / `./gradlew lint` / iOS の `cd ios && swift build` を置く (テストは `test-execution.md` が正である旨を添える)。
2. 手続き面: A 分類の「実在確認」は、**本 change で書き換える文書を根拠にしてはならない**。この 1 件を deviation に記録し、教訓 (`kasane/lessons/inbox/`) の候補として拾う。既に `transfer-table-enumerated-by-source-not-content-class.md` が inbox にあるが、それとは別型 (根拠文書の同時消滅) である。

---

### [🟡 Minor] binding 層の namespace 統一と `Transforms/Metadata.xml` による上書きが移送も破棄確認もされていない

**該当箇所**: `kasane/concepts/maui/architecture/binding-build-integration.md` (該当記述の不在) / 移送元は `git show HEAD:maui/README.md:47-48`

**問題点**:
旧 `maui/README.md`「binding 層」節にこうあった。

> 生成される .NET の型はどちらの platform でも `KsSettingsView.Bridge` namespace に入る。Android 側は Java パッケージ名からの既定変換を `Transforms/Metadata.xml` で上書きしている。

design.md の移送対応表は「binding 層」節を `binding-build-integration.md` 行の移送元に含めている。しかし新 concept にこの記述はない。実測: `grep -rn "KsSettingsView.Bridge\|Metadata.xml" kasane/concepts/ kasane/decisions/` の結果、

- `binding-build-integration.md:74` の `Metadata.xml` は BG8A00 警告 (`remove-node`) の説明であり、namespace 上書きとは別件
- `maui/ADR-0025:32` に「binding の名前空間 `KsSettingsView.Bridge` は利用者向け契約ではないため変更しない」がある — namespace が `KsSettingsView.Bridge` である事実は残っている
- **Android 側でそれが `Transforms/Metadata.xml` の上書きによって実現されている**という機構は、どこにも無い

deviation.md にも A 判定 (破棄) として記録されていない。取りこぼしである。

**実害のシナリオ**:
Android binding の `Transforms/Metadata.xml` を整理する作業で、namespace 変換の entry を「用途不明」と判断して削除する。ビルドは通るが生成される C# 型が Java パッケージ由来の namespace (`Jp.Kamusoft.Kssettingsview.Bridge` 等) に落ち、facade 側の変換コードが一斉にコンパイルエラーになる。ADR-0025 の「namespace は変更しない」という決定が、実現手段を失って守れなくなる。

**推奨修正**: `binding-build-integration.md` の「Android」節 (`:43-47`) に 1〜2 文で追加する。「binding tool が生成する型は両 platform とも `KsSettingsView.Bridge` namespace に入る (maui/ADR-0025)。Android は Java パッケージ名からの既定変換を `Transforms/Metadata.xml` で上書きしてこれを揃えている」。あわせて「関連」に maui/ADR-0025 を足す。

---

### [🔵 Suggestion] tasks 8.4 の identity-lint は README / `.github/` を 1 行も検査していない

**該当箇所**: `tasks.md:63` (8.4) / `kasane/config.yaml` の `lint.identity.scope: [kasane, openspec, skills]`

**内容**:
8.4 は「`scripts/local-path-lint.py` と `scripts/identity-lint.py` を通す (→ Requirement: スクリーンショットの提示 / ルート README の節構成)」とあり、両方 exit 0 を確認済みとされている (再現も取れた)。

しかし `identity-lint.py:152-155` の `in_scope()` はパス第 1 セグメントで絞り込むため、`README.md` / `README_ja.md` / `.github/**` / `assets/**` はすべて **対象外として素通り** する。実測: `python3 scripts/identity-lint.py --paths README.md README_ja.md .github/CONTRIBUTING.md` → exit 0 (違反ゼロではなく、検査されていない)。`.agents/skills/docs-refresh/SKILL.md:692` の「実効範囲の注記」が同じことを明記しており、本 change 自身がその注記を更新している。

したがって 8.4 が Requirement「スクリーンショットの提示」「ルート README の節構成」の証跡として立つのは `local-path-lint.py` の側だけ (こちらは scope を持たないので README にも効く。実測でも untracked を含めて検査されることを確認)。スクリーンショットの端末固有情報の不在は 8.5 の目視検査が本体で、それは本レビューでも 4 枚を独立に確認して問題なしと判定した。

**推奨**: tasks の記述を残すなら 8.4 の `→ Requirement:` 欄から「スクリーンショットの提示」を外し、identity-lint については「scope 外のため README / `.github/` は非対象」と併記する。証跡の空振りを「検査した」と数えないため。

---

### [🔵 Suggestion] 英語 README に日本語 UI のスクリーンショットが載る (オーナー承認済み)

**該当箇所**: `README.md:21-24` / `ui/brief.md` の「申し送り」

**内容**:
採用した 4 枚はいずれも Sample アプリの日本語 UI (「Section 装飾デモ (style 切替)」「機内モード」「装飾プリセット」等)。brief がこれを申し送りとして明記し、オーナーが「そのまま採用」を選んでいるため **spec 違反ではなく、本 change での修正も求めない**。

ただし public 化 (phase-2) の観点では、英語 README を最初に見る英語話者にとって製品の第一印象が読めない文字列になる。Sample の英語化はスコープ外だが、以下のいずれかは phase-2 の論点として拾う価値がある: (a) Sample に英語リソースを追加して撮り直す、(b) 英語 README のキャプションに「screenshots from the Japanese-locale sample app」の 1 行を添える。(b) は本 change 内でも 2 行で済む。

---

### [🔵 Suggestion] blank issue 無効 + `contact_links: []` で、質問の窓口が 1 つも無い

**該当箇所**: `.github/ISSUE_TEMPLATE/config.yml:1-2`

**内容**:
```yaml
blank_issues_enabled: false
contact_links: []
```
spec Requirement「Issue テンプレートの必須項目」が `blank_issues_enabled: false` を要求しているので、この設定自体は正しい。ただし `contact_links` が空のため、Issue 作成画面には bug / feature の 2 択しか出ない。「使い方が分からない」「これは仕様か不具合か判断できない」という利用者は、必須の再現手順を埋められないまま bug テンプレートに無理やり流し込むか、諦めるかになる。ADR-0024 の動機 (AI スロップ抑止) は満たすが、正当な質問も同じ網で止まる。

GitHub Discussions を有効にして `contact_links` に 1 本足すのが定石だが、Discussions の有効化はリポジトリ設定であり phase-2 の実施手順書の領分。**本 change での修正は求めない** — phase-2 の論点として申し送ることを推奨する。

---

### [🔵 Suggestion] リポジトリ構成の表に `scripts/` `openspec/` `.github/` が無く、`openspec/` の説明が失われた

**該当箇所**: `README.md:143-151` / `README_ja.md:143-151`

**内容**:
表は 7 ディレクトリ (`ios/` `android/` `maui/` `samples/` `skills/` `assets/` `kasane/`) を挙げているが、リポジトリルートには他に `scripts/` `openspec/` `.github/` がある。spec は「ディレクトリの表と `AGENTS.md` / `kasane/concepts/` へのリンクだけを持つ SHALL」としか言っておらず網羅は要求していないので **違反ではない**。

ただし旧ルート README (`git show HEAD:README.md:72`) は「`openspec/` は旧運用 (OpenSpec) の歴史資料として凍結されています」と明記していた。public 化後、リポジトリを開いた訪問者は `openspec/` という大きなディレクトリを見て、それが現行の仕様置き場なのか凍結資産なのか判別できない。`AGENTS.md` には書いてあるが、README の表からは辿れない。

**推奨**: 表に `openspec/` の 1 行 (「Frozen historical artifacts from the previous OpenSpec workflow」) を足す。`scripts/` `.github/` は開発インフラなので省略のままでよい。

---

## 確認したが問題なしと判定した観点

| 観点 | 実測 |
|---|---|
| README 集合 (spec Scenario) | 公開ドキュメント面から `README*.md` を列挙 → ルート 2・`skills/` 2・`maui/spike/` 1 の 5 枚のみ ✓ |
| 翻訳ロックステップ | 見出し 17 個、階層と並びが完全一致 ✓ |
| 最小コード例 ⇔ SKILL.md | iOS / Android / MAUI とも逐語一致。AiForms 移行 Skill は非対象 ✓ |
| 配布座標の一致 | SwiftPM `KsSettingsView-SPM`・Maven `jp.kamusoft:kssettingsview:0.1.0`・NuGet `KsSettingsView.Maui` 0.1.0 が README ×2 と SKILL.md ×6 で一致。`KsSettingsView-Swift` の残存ゼロ ✓ |
| 対応プラットフォーム表の値 | AGP 8.13.2 / Kotlin 2.4.10 / Gradle 9.5.0 / compileSdk 35 / minSdk 29 / swift-tools 5.10 / .iOS(.v16) / SDK 10.0.300 / net10.0-ios;net10.0-android / Maui.Controls 10.0.70 — 全項目をビルドファイルと突合して一致 ✓ |
| 内部リンク | README ×2・CONTRIBUTING ×2・skills README ×2・docs-refresh SKILL・concepts 全ファイルの Markdown リンクを解決 → 破断 0 (テンプレート placeholder `{target}` を除く) ✓ |
| スクリーンショット 4 枚 | `assets/` と `ui/references/` の SHA-256 一致。4 枚を目視 → platform×style 4 通り、同一デモ画面・同一スクロール位置 (最上部)、時刻 9:41 / キャリア非表示 / 電池残量非表示。端末特定表示なし ✓ |
| Issue Forms | bug 5 項目 (version / platform / reproduction_steps / actual_behavior / expected_behavior)・feature 3 項目、すべて `required: true`。両フォームに日英どちらでも可の案内あり。`blank_issues_enabled: false` ✓ |
| CONTRIBUTING | 英日相互リンク解決。PR 非受付の理由 (Kasane フローの文脈保持) と Issue の書き方あり。`.github/CONTRIBUTING.md` は GitHub が Issue 作成画面でリンクする正規の配置 ✓ |
| docs-refresh 除去範囲 | design Decision 2 の 6 箇所すべてで除去を確認。残存 2 箇所 (`:182` 廃止理由の注記 / `:366` 再導入禁止指示) は deviation に意図的として記録済み、かつ「指示」ではなく「根拠」「禁止」であり Scenario の趣旨に反しない ✓ |
| manifest | `readmes` 4 枚。`targets` / `excluded` に旧 README への参照なし ✓ |
| 移送先 concept の配置 | `cross/conventions/`=policy、`maui/architecture/`=concept、`maui/conventions/`=policy。`rules.md` のカテゴリ定義と type 対応に適合。各 index に 1 行追加済み ✓ |
| 移送内容の充実 | `integration-host-verification.md` は移送元に無かった MauiHost の 5 手順と「解放 → 再生成」の期待表示を追加。`runtime-behavior-verification.md` の目視チェックリストは色値の正典化を避けつつ観測点 6 件を維持。いずれも劣化なし ✓ |
| lint | `local-path-lint.py` / `identity-lint.py` とも exit 0 を再現。両者とも `git grep --untracked` を使うため untracked の新規ファイルも走査対象 (identity は scope による絞り込みあり → Suggestion 参照) ✓ |
| 付随修正 | `performance-verification.md` の参照差し替え 2 箇所 (本務が直接の原因)、`public-identifiers.md` の H1 追加 — いずれも ksn-core の同梱条件 (①〜⑤) に収まる ✓ |

---

## アクションプラン

優先度順。1〜2 が CHANGES_REQUESTED の理由。

1. **Major A** — `test-execution.md:60-62` を現状のルート README に合わせて改める (節削除、または「README・skills とも案内していない」へ書き換え) + timestamp 更新。
2. **Major B** — `binding-build-integration.md:78` に maui/ADR-0006 が指す表である旨を明記し「関連」へ ADR を追加。あわせて deviation.md に「accepted ADR からの参照 1 件が切れた」を記録し、supersede の要否を蒸留へ送る。
3. **Minor C** — `README_ja.md:159` のリンク先を `.github/CONTRIBUTING_ja.md` へ。
4. **Minor D** — `log.md` の `## 2026-08-29` 見出しと本 change の 2 行をファイル末尾へ移し、`distilled: skills-api-coverage` を `## 2026-08-28` 節へ戻す。
5. **Minor F** — `local-development-setup.md` に本体ビルド / lint の小節を足す (`./gradlew build` / `./gradlew lint` / `swift build`)。
6. **Minor G** — `binding-build-integration.md` の Android 節に namespace 統一と `Transforms/Metadata.xml` 上書きを 1〜2 文で追加。
7. **Minor E** — `test-execution.md:15` と `local-development-setup.md:151` に相互の 1 行を足して MAUI テストの導線を閉じる。
8. **Suggestion H** — `tasks.md:63` の 8.4 から identity-lint の Requirement 対応を実態に合わせる。
9. **Suggestion I / J** — 英語 README のスクリーンショット言語、質問窓口の不在。いずれも phase-2 へ申し送り (本 change では修正不要)。
10. **Suggestion K** — リポジトリ構成の表に `openspec/` の 1 行を足す。

---

## 突き合わせ結果 (2026-08-29)

ホスト側 `review-001.md` と本レビューの指摘を突き合わせた。**本来は相方 CLI (codex) によるクロスモデルレビューを並走させる級 (`second-opinion.code-review: [m, l]`) だが、相方が利用枠の上限に達して実行できず、オーナー承認のうえホスト側 2 人目の独立レビュアーで代替した**。同一モデル同士のためモデル由来の盲点は重なる — この点は `deviation.md` に記録済み。

| # | 指摘 | review-001 | 本レビュー | 採否 |
|---|---|---|---|---|
| 1 | `test-execution.md` が削除済み README の `swift test` 案内を事実として語る | Major 1 | Major A | **確定 (Major)** — 双方一致 |
| 2 | `README_ja.md` の貢献導線が英語 CONTRIBUTING を指す | Major 2 | Minor C | **確定 (Major)** — 一致。重要度は高い方を採用 |
| 3 | accepted な maui/ADR-0006 が削除済み `maui/README.md` の表を指す | — | Major B | **採用** — 本レビューのみ。該当行・移送先・実害シナリオが特定されており根拠強 |
| 4 | A 判定 (破棄) の根拠が本変更自身で無効化 (ビルド / lint コマンドの消失) | Minor 3 | Minor F | **確定** |
| 5 | 生成型 namespace と `Transforms/Metadata.xml` の `managedName` 上書きが移送漏れ | Minor 4 | Minor G | **確定** |
| 6 | `log.md` の append-only 逸脱 (既存行が新見出しの下へ移動) | Minor 7 | Minor D | **確定** |
| 7 | MAUI テストコマンドの置き場 (deviation の要レビュー項目) | Minor 9 (`test-execution.md` へ寄せる) | Minor E (双方向の導線を張る) | **確定** — 結論は `test-execution.md` へ移送 |
| 8 | 追記に対する index の 1 行説明 / frontmatter description の未追随 2 件 | Minor 5 | — | **採用** — review-001 のみ。ksn-core の規約条文を引いており根拠強 |
| 9 | 新規 concept 3 本が docs-refresh の網羅検査で `UNCOVERED` になる | Minor 8 | — | **採用** — review-001 のみ。実行結果を伴い根拠強 |
| 10 | `tasks.md` 8.4 の Requirement 対応が実態と合わない (identity-lint の scope 外) | 確認事項で言及 | Suggestion H | **採用 (軽微)** |
| 11 | ルート README の MAUI 例に `.AddKsSettingsView()` の案内がない | Suggestion 10 | — | **採用** |
| 12 | Sample 実行手順から Simulator / Emulator の boot 手順が欠落 | Suggestion 12 | — | **採用** |
| 13 | リポジトリ構成表に `openspec/` の説明がない | — | Suggestion K | **採用** |
| 14 | 英語 README に日本語 UI のスクリーンショットが載る | Suggestion 11 | Suggestion I | **申し送り** — 双方とも「本変更では修正不要」。phase-2 へ |
| 15 | blank issue 無効 + `contact_links: []` で質問窓口が無い | — | Suggestion J | **申し送り** — Discussions の有効化はリポジトリ設定であり phase-2 の領分 |

**採用 13 件 / 降格 0 件 / 申し送り 2 件 / 未解決 0 件。** 両レビューの結論が矛盾した指摘はなかった (#7 は方向性の違いであり、より強い側 = 移送に倒して両立する)。
