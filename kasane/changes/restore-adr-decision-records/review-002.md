# レビュー結果: restore-adr-decision-records (002 回目)

**日付**: 2026-09-07
**判定**: CHANGES_REQUESTED

## サマリー

review-001 の Major 2 件は解消している。maui/0015 は残課題 3 件の bullet が本文へ戻り、footer が `整理` (作業記録) と `現行照合` (主張の判定) に分かれ、(2) の共有破棄が maui/ADR-0026 の決着であることを正しく書いている。`現行照合` は 8 本すべてに入り、8 本のうち 7 本は実際のコード・設定と照合して正しい (cross/0020 の publish 9 step の順序、cross/0021 の archive 媒体 5 件、cross/0023 の README 4 枚、cross/0028 の `ci.yml` トリガーはいずれも実物と一致した)。`release-pipeline.md` の全面書き直しも `.github/workflows/release.yml` と `scripts/release/` に照らして段の役割表・publish 段 9 ステップとも一致しており、初見可読性の指摘 (段の名前・publish 段の内部順序・宙に浮いた語) は概ね解消している。

一方で、新しい事実誤りが 1 件入った。deviation.md の所見が「追跡先の change `investigate-maui-icon-lease-sharing` は `changes/` にも `archive/` にも存在しない」と断定しているが、`kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/` は実在し、maui/ADR-0026 の出典行 3 本がそこを指している。review-001 が所在を明示していた事実を否定する形で、昇格済みルール L-006 (不在の断定は検索を通してから) にも抵触する。ほかに、`concepts/log.md` の concept-work エントリが round-1 時点の記述のまま残っており、review-001 で「実際より広い」と否定された android/0019 の主張をそのまま繰り返している (ADR 本体は正しく直っているので、長命層の 2 箇所が矛盾している)。deviation.md の L-009 事後判定も未達 (`cross/index.md` と `log.md` が行に現れない)。

## 照合した規約

- `kasane/handbook/index.md` / `cross/index.md` の「適用のきっかけ」で判定 — 変更はすべて Markdown で、コード・テスト・`samples/`・`skills/`・ビルドファイルのいずれにも触れないため該当する rule は無し (`comment-policy.md` はコメント構文を持つソースコードが対象)。`cross/release-procedure.md` (guide) は `release-pipeline.md` の二重管理判定と `保証すること` の裏取りのため本文まで読んだ
- ksn-core `references/decisions.md` (footer 規約 = 出典 / 現行照合 / 関連、本文不変、Consequences の導出可能性)
- ksn-core `references/concepts.md` (階層規約・価値 lint・可読性規約) / `references/paths.md` / `references/doc-structure.md`
- `kasane/concepts/rules.md` (カテゴリ定義と配置判断)
- `kasane/lessons/code-review.md` — 重点観点 L-001 (ミューテーション実測) はコード変更ゼロの本変更に非該当。「指摘しないこと」は昇格済みルール無し
- `kasane/lessons/process.md` L-006 (不在の断定は検索を通してから) / L-009 (合意済みスコープ外のファイルは deviation.md へ 1 行)

## review-001 の指摘の解消状況

| review-001 の指摘 | 状態 | 検証したこと |
|---|---|---|
| 🟠 maui/0015 の `整理` footer が事実と異なる | **解消** | `整理` は消化状況の追記を除いたことだけを述べ、共有破棄の決着は `現行照合` が maui/ADR-0026 として指す。ADR-0026 は `status: accepted` / `date: 2026-08-25` で、Decision が「キャッシュ所有と分類した画像には後片付け口を付けない」と定めているのを確認 |
| 🟠 `現行照合` が 8 本中 2 本 | **解消** (1 本に精度の問題あり) | 8 本すべてに `現行照合` 行がある。7 本は実物と一致。android/0019 のみ下記 Minor |
| 🟡 maui/0015 が合意範囲を超えて本文を削っている | **解消** | 旧 29 行目の `- 負: 既知の残課題 — (1)(2)(3)` が Consequences に復元されており、diff 上の除去は 2026-08-22 の追記行 1 行だけになった |
| 🟡 android/0019 の footer の主張が実際より広い | **解消** | footer が「experimental への依存がカレンダーダイアログ 1 箇所に限られる点は同節にはないため、下の現行照合で接地する」と書き分けた。`build-toolchain.md:78-82` に適用範囲の記述が無いことを確認済みで、主張と実態が一致した |
| 🟡 `保証すること` が handbook と二重管理 | **概ね解消** (残留あり) | 冪等条件の列挙は「位置ごとの挙動は handbook の『失敗したとき』が持つ」のポインタに置き換わった。handbook 側の表 7 行に情報の欠落は無い。deployment ID の項だけ残留 (下記 Minor) |
| 🟡 deviation.md が無い | **部分解消** | ファイルは作成されたが L-009 の事後判定を満たさない (下記 Minor) |
| 🔵 Suggestion 6 件 | **未対応** | `関連:` 行 (cross/0018・maui/0015)、`整理:` 行の位置統一、cross/0021 のスクリプト名、初見可読性レビューの証跡、rules.md のカテゴリ説明はいずれも現状のまま。Portal の 2 段階の分割だけは `publish 段の順序` 節の新設で実質解消 |

## 独立に検証した `現行照合` の主張

| ADR | 主張 | 判定 |
|---|---|---|
| cross/0018 | 座標の現況は `repository-boundaries.md` の build root の表が 3 platform 分持つ | 一致 |
| cross/0019 | `release.yml` は tag トリガーを持たず手動起動。判定: 乖離あり | 一致 (`.github/workflows/release.yml:20-21` は `workflow_dispatch` のみ) |
| cross/0020 | publish の書き込み step は snapshot commit → Portal upload → ID 保存 → 検証待ち → NuGet push → Maven release → 配信 tag → monorepo tag → GitHub Release | 一致 (`release.yml:531 / 559 / 699 / 715 / 739 / 765 / 776 / 796 / 812`) |
| cross/0021 | archive 媒体 5 件が未除去、`maui/spike/` は不在、ハーネス記録は追跡下 | 一致 (`git ls-files` で webp がちょうど 5 件、`maui/spike` は 0 件、`kasane` 1462 / `openspec` 292 / `.claude` 2 / `.codex` 2 件が追跡下) |
| cross/0023 | 追跡下の README は 4 枚、廃止対象 5 枚は不在 | 一致 (ルート 2 枚 + `skills/` 2 枚。ほかは change 配下の検証・証跡用と `scripts/spm-snapshot/README.template.md` のみ) |
| cross/0028 | `ci.yml` の `on:` は `pull_request` が `main` 宛てのみ・`push` が `develop` のみ・`paths-ignore` に `kasane/**` と Issue テンプレート・CONTRIBUTING | 一致 (`.github/workflows/ci.yml:17-27`) |
| android/0019 | `@ExperimentalMaterial3Api` を使うのは `DateCalendarDialog.kt` の 1 ファイルのみ | **不一致** (下記 Minor) |
| maui/0015 | (1) は退役キュー移行で解消、(2) は ADR-0026 が決着、(3) は未解決 | 一致 (`KsSettingsController.cs:106,1655,2378` に `RetiredView` / `DisposeRetired`、`IconSourceTests.cs` に順序固定の回帰テスト 4 本、リースにファイナライザは存在しない) |

## 指摘事項

### [🟠 Major] deviation.md の所見が事実と異なる — 追跡先の change は archive に実在する

**該当箇所**: `deviation.md:18`

**問題点**: 「`maui/0015` が指していた追跡先の change `investigate-maui-icon-lease-sharing` は `changes/` にも `archive/` にも存在しない」「実在しない change を指す参照が ADR に残っていた」と断定しているが、`kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/` は実在する。`maui/ADR-0026` の出典行はこの archive の `exploration.md` / `deviation.md` / `review-004.md` の 3 本を名指ししており、参照は切れていない。review-001 も Major 1 の中で archive 済みであることとそのパスを明示していた。昇格済みルール L-006 は「『〜が無い』の不在の断定は、対象を特定できる検索を通してから行う」と定めており、`ls kasane/changes/archive/ | grep lease` 1 回で覆る断定になっている。この所見は「本変更では直さない」として蒸留へ引き継がれる形になっているため、放置すると存在しない問題の起票につながる。

**推奨修正**: 該当行を削除するか、「追跡先は `kasane/changes/archive/2026-08-25-investigate-maui-icon-lease-sharing/` として archive 済みで、参照は解決する」と事実に直す。

### [🟡 Minor] `concepts/log.md` の concept-work エントリが round-1 時点のまま — 訂正済みの主張を長命層に残している

**該当箇所**: `kasane/concepts/log.md:424`

**問題点**: エントリが 3 点で現状と食い違う。
1. android/0019 について「experimental API の適用範囲と Compose 版整合の方向は**いずれも** build-toolchain.md の『Compose 版の整合』節が現況として持つ」と書いている。これは review-001 Minor で否定された主張そのもので、ADR 本体の footer は「適用範囲は同節にはない」と正しく直っている。長命層の 2 箇所が正反対のことを言う状態になった。
2. `release-pipeline.md` を「公開確認は repo1.maven.org への HEAD・**保証すること 3 項**・してはいけないこと 3 項」と要約しているが、書き直し後は公開確認が 3 チャネルの表、保証することは **4 項**である。publish 段の内部順序という新設の主節にも触れていない。
3. 「3924 字」も書き直し前の値の可能性が高い。

**推奨修正**: エントリを現状の成果物に合わせて書き直す。とくに 1 は ADR footer と同じ表現 (適用範囲は現行照合で接地) に揃える。

### [🟡 Minor] android/0019 の `現行照合` の「1 ファイルのみ」が実際と一致しない

**該当箇所**: `kasane/decisions/android/0019-datepickercell-calendar-compose-datepicker.md:39`

**問題点**: 「`androidx.compose.material3.DatePicker` と `@ExperimentalMaterial3Api` を使うのは `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialog.kt` の 1 ファイルのみ」とあるが、`@ExperimentalMaterial3Api` の opt-in はテスト 2 本 (`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialogTest.kt:8,41` と `DateCalendarRecreationTest.kt:8,41`) にも入っている。除去した本文が言っていた「experimental への依存はカレンダーダイアログの 1 箇所に限定される」という趣旨自体は成立する (テストは同じダイアログの検査で、本番コードの依存は 1 ファイル) が、`現行照合` は ksn-drift のディープ検証が突き合わせる接地点なので、次回「1 ファイルのみ」を機械的に検査すると偽の乖離になる。

**推奨修正**: 「本番コードで opt-in するのは `DateCalendarDialog.kt` の 1 ファイルのみ (テストは同ダイアログの検査 2 本)」のように、検査可能な形へ直す。

### [🟡 Minor] `保証すること` の deployment ID の項が handbook と二重管理のまま残り、状態の列挙も食い違う

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:103`

**問題点**: 冪等条件 (2 項目め) は handbook へのポインタに置き換わったが、4 項目めの「deployment ID は attempt をまたいで引き継ぐ。失敗経路で Portal から破棄 (drop) するのは破棄できる状態 (`VALIDATED` / `FAILED`) の deployment だけで、検証中のものは残して次の attempt に委ねる」は、`kasane/handbook/cross/release-procedure.md:147`「保留中の deployment ID は attempt をまたいで引き継がれる」と同 `:159`「削除できない状態 (検証中か公開処理中) のときは何もせず理由が出て ID もそのまま残る」がそのまま持っている内容で、review-001 が名指しした「deployment ID の引き継ぎ・drop 可能状態」の残りである。しかも 2 文書で削除できない状態の説明が食い違っており、`scripts/release/central-portal.sh:294-303` に照らすと handbook 側 (検証中 = `PENDING` / `VALIDATING`、公開処理中 = `PUBLISHING` / `PUBLISHED`) が正確で、概念側の「検証中のものは残して」は `PUBLISHING` / `PUBLISHED` を落としている。

**推奨修正**: 概念側は「ID の引き継ぎと drop の可否は [リリース手順] の『失敗したとき』が持つ」というポインタに寄せるか、残すなら drop 対象を `VALIDATED` / `FAILED` の 2 状態、残す側を `PENDING` / `VALIDATING` / `PUBLISHING` / `PUBLISHED` と両方向で書き切って、handbook 側にどちらが正かを明記する。

### [🟡 Minor] deviation.md が L-009 の事後判定を満たしていない

**該当箇所**: `deviation.md` (付随修正の節)

**問題点**: L-009 の事後判定は「`git show --stat` に現れるファイルのうち合意済みスコープが名指ししていないものが、すべて deviation.md の行に箇所として現れている」。exploration.md が名指しした範囲は「ADR 8 本 / 新規 `release-pipeline.md` / 既存 `repository-boundaries.md` = 計 10 ファイル」で、実際の `git status` には次の 2 ファイルが追加で現れる。

- `kasane/concepts/cross/index.md` (新概念の 1 行追加)
- `kasane/concepts/log.md` (concept-work エントリの追加)

どちらも deviation.md のどの行にも箇所として現れていない。index 更新と log 追記は ksn-concept の定型作業なので実務上の問題は小さいが、事後判定は機械的な検査として定義されており、この形だと次の変更でも同じ漏れ方をする。

**推奨修正**: 付随修正の節に 2 行足す (`- [付随修正] kasane/concepts/cross/index.md: 新概念の索引行を追加。理由: 概念ファイル追加に伴う index 維持 (2026-09-07)` の形)。定型作業を毎回書くのが冗長だと考えるなら、それは L-009 のルール文側の一般化として lessons へ上げる話で、本変更の中で黙って落とす扱いにはしない。

### [🟡 Minor] `release-pipeline.md` の公開確認の表が、NuGet の実際の確認手段を落としている

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:92-96`

**問題点**: 表は NuGet.org の確認手段を「smoke の解決が確認を兼ねる」とだけ書き、Maven Central だけが「`repo1.maven.org` への HEAD で代替する」特別扱いに見える。実際は反映待ち段の `scripts/release/wait-for-registries.sh` が Maven と NuGet の**両方**を能動的に確認しており、NuGet 側は flat container の `index.json` に当該 version が含まれるかを 3 Package ID (`kssettingsview.maui` / `kssettingsview.binding.ios` / `kssettingsview.binding.android`) で判定する (`scripts/release/wait-for-registries.sh:28-34,56-60`)。SwiftPM だけが専用の確認を持たず smoke に委ねられている、というのがこの表が本来示すべき非対称性である。この節は「公開できたことをどう確かめているか」を冒頭で読者に約束している箇所なので、記述と実装がずれていると再導出のとき誤った結論になる。

**推奨修正**: NuGet.org の行を「反映待ちが flat container の index に version が現れるのを確認し、smoke の解決が最終確認になる」に直し、Maven Central の行の「Portal に公開済み API が無いため HEAD で代替」という**理由**は残す (これがこの文書の中核の知識)。

### [🔵 Suggestion] `USER_MANAGED` がリポジトリのどこにも現れない

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:78`

**問題点**: 「plugin の `publishToMavenCentral` を自動 release なし (`USER_MANAGED`) で実行して upload する」とあるが、`android/kssettingsview/build.gradle.kts:104` は `publishToMavenCentral()` を引数なしで呼んでおり、`USER_MANAGED` という文字列はリポジトリのどこにも無い (plugin 既定の挙動を Portal の publishingType 名で言い換えたもの)。読者がこの語で grep すると何も見つからず、初見可読性レビューが潰した「宙に浮いた語」と同じ型になる。

**推奨修正**: 「plugin の既定 (自動 release しない) のまま `publishToMavenCentral` を実行して upload する」と書くか、`USER_MANAGED` を Portal 側の用語だと明示する。

### [🔵 Suggestion] publish 段の一覧が「外へ書き込む step」を名乗るが 1 つは run 内の保存

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:40-50`

**問題点**: 「外へ書き込む step は次の順に走る」と宣言した 9 項目のうち、3 の「deployment ID を run の artifact へ保存する」は GitHub Actions の run 内に閉じた保存で、外部への書き込みではない。取り消せる順という主題からすると並べる価値はあるので、宣言の方を緩めるのが素直である。

**推奨修正**: 「publish 段の step は次の順に走る (5 以降が不可逆)」等に言い換える。

### [🔵 Suggestion] 段の図が実際の job 依存より強い順序を示している

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:19-22`

**問題点**: 図は `本体検証 ∥ 配布物の生成 → 消費者検証 (dry-run)` と書くが、`release.yml` の `consumer-*` の `needs` は `[validate, package-*]` だけで、本体検証 (`ios` / `android` / `maui`) には依存しない (両方が `publish` の `needs` に入って合流する)。段の粒度の説明としては許容範囲だが、CI の待ち時間を追う読者には誤読の元になる。

**推奨修正**: 「本体検証と配布物の生成は並列で、消費者検証は配布物の生成だけを待つ。publish が両方の合流点になる」を 1 文添える。

### [🔵 Suggestion] 「消費者検証 (dry-run)」の `dry-run` が workflow 入力の `dry-run` と別物

**該当箇所**: `kasane/concepts/cross/architecture/release-pipeline.md:31`

**問題点**: 段の名前の `dry-run` は `consumer-*` に渡す `mode: dry-run` (ローカルフィードから解決する) を指すが、同じ workflow には `publish` 以降を丸ごと止める入力 `dry-run` がある (`release.yml:418` の `if: ${{ !inputs['dry-run'] }}`)。同名の別概念が 1 本の workflow に同居していることが本文からは読めない。

**推奨修正**: 段の説明に「(workflow 入力の `dry-run` = リハーサルとは別物。リハーサルは publish 手前で止まる — [リリース手順])」を括弧書きで足す。

### [🔵 Suggestion] footer のキー順が依然そろっていない (review-001 Suggestion の再掲)

**該当箇所**: `出典` → `整理` → `現行照合` が 5 本 (cross/0018・0019・0020、android/0019、maui/0015)、`整理` → `出典` → `現行照合` が 3 本 (cross/0021:41、cross/0023:52、cross/0028:58)

**問題点**: 8 本すべてに `現行照合` が入ったことで、並びの不揃いが前回より目立つ形になった。`整理` はプロジェクト独自キーなので、順序を決めておかないと後から機械で拾うときの基準が無い。

**推奨修正**: `出典` → `整理` → `現行照合` (→ `関連`) に 8 本で揃える。

## 確認した観点 (指摘なし)

- **決定内容の不変性**: 8 本すべてで Context / Decision / Alternatives Considered の意味が保たれている。round-2 で本文に入った変更は maui/0015 の残課題 bullet の復元だけで、それ以外の本文は round-1 から動いていない。cross/0018 は削除した追記に対応する `出典 (2026-09-01 …)` / `出典 (2026-09-02 …)` の 2 行も一緒に落としており、footer の整合が取れている
- **足場凍結**: `exploration.md` は未変更。`現行照合` を 8 本へ入れる形で合意済みスコープに寄せたため、スコープ側の書き換えは発生していない
- **`release-pipeline.md` と実装の一致 (上の表以外)**: 段の役割表 5 行は `release.yml` の job 構成と一致 (validate の 4 検査は `Validate inputs` / `Verify monorepo tag` / `Verify distribution repository tag` / `Verify install examples`)。version 注入表は `android/build.gradle.kts:13`、`android/gradle/libs.versions.toml:32` の `0.1.0-SNAPSHOT`、`maui/Directory.Build.props:27` の `0.0.0-dev` と一致。`--skip-duplicate` は `release.yml:759` に実在。Trusted Publishing は `Login to NuGet.org` と `id-token: write` に対応。鍵つき再ビルドの説明は `check-signatures.sh` / `compare-maven-artifacts.sh` が `publishToMavenCentral` の**前**に走る構成と一致。Portal の一覧 API・公開確認 API の不在という中核の知識は `central-portal.sh` のコマンド構成 (status / wait-validated / release / wait-published / drop) と整合する
- **`repository-boundaries.md` の移送**: 移した記述はすべて `release-pipeline.md` 側に対応する記述があり、落ちた情報は無い。timestamp を据え置いた判断も全体未検証である以上妥当
- **リンク**: 変更・追加した 11 ファイルの相対リンクを機械的に全件解決確認、切れ無し。footer が名指しするコード・テストのパス (`KsSettingsController.cs` / `IconSourceTests.cs` / `DateCalendarDialog.kt` / `scripts/spm-snapshot/`) もすべて実在
- **lint**: `doc-structure-lint.py` は `release-pipeline.md` / `repository-boundaries.md` / `cross/index.md` / `log.md` に指摘なし。`local-path-lint.py` / `identity-lint.py` も新規・変更ファイル全件で指摘なし
- **ビルド・テスト**: コード変更ゼロ (`git status` に `.kt` / `.cs` / `.swift` / ビルドファイルは 1 件も現れない) のため実行対象なし
- **deviation.md のその他の所見**: 「ADR 4 本が旧形式 frontmatter」「構造 lint の既存違反 29 件 / 9 ファイル」「archive 媒体 5 件」はいずれも実物で確認でき、正しい

## アクションプラン

1. deviation.md:18 の不在の断定を撤回し、archive の実パスに直す (Major・事実誤り / L-006)
2. `concepts/log.md:424` の concept-work エントリを現状の成果物に合わせて書き直す。とくに android/0019 の 1 文を ADR footer と同じ表現に揃える (Minor・長命層の矛盾)
3. android/0019 の `現行照合` を「本番コードで 1 ファイル (テスト 2 本を除く)」の形へ直す (Minor)
4. `release-pipeline.md:103` の deployment ID の項をポインタへ寄せるか、drop 可否の状態を両方向で書き切る (Minor)
5. deviation.md の付随修正に `cross/index.md` と `log.md` の 2 行を足す (Minor・L-009 事後判定)
6. `release-pipeline.md:92-96` の公開確認の表で NuGet の確認手段を実装に合わせる (Minor)
7. Suggestion 群 (`USER_MANAGED`、「外へ書き込む step」の宣言、段の図の依存、`dry-run` の語の衝突、footer のキー順) は任意。review-001 から持ち越した Suggestion (`関連:` 行、cross/0021 のスクリプト名、初見可読性レビューの証跡、rules.md のカテゴリ説明) も未対応のまま残っている
