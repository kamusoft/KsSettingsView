# レビュー結果: add-verification-ci (001 回目)

**日付**: 2026-08-31
**判定**: CHANGES_REQUESTED

## サマリー

commit `ec24198` の 4 workflow は、デルタスペックの Requirement をほぼ正確に写している。特に Android の件数検査は「存在する XML を数える」実装ではなく、`android/settings.gradle.kts` の `include` から期待集合を導出して XML 欠落と `tests` 合計 0 の両方を失敗にしており、Requirement「Android の検証と実行件数の担保」の意図を満たしている。iOS は `swift test` を使わず Simulator destination を UDID で解決し、MAUI は TRX が無い経路でも合計 0 件として fail するため「0 件だが成功」の穴も塞がっている。外部 action 4 件の pin は SHA とタグの一致を実際に照合して確認し、gitleaks の版・資産名・SHA-256 も公式リリースの digest と一致した。

一方で Requirement「ツールチェーンの再現性」の固定境界に反する箇所が 1 つ残っている (`ubuntu-latest`) ため CHANGES_REQUESTED とする。他は「検査が素通りするのに緑になる」経路のうち発生確率の低いもの (Minor 2 件) と、観測性・運用コストの Suggestion である。

なお ADR-0024 (外部 PR を受け付けない / collaborators only) と `pull_request` トリガー + `contents: read` の組み合わせは整合しており、fork からの secret 露出経路は無い。

### 実行した検証

本 change はライブラリのコード・テストに触れないため、diff が新設した検査そのものを手元で回した。

- 4 workflow の YAML パース: 全て成功。job キーとトリガーは spec の要求どおり
- lint job が呼ぶ 3 スクリプトを現行ツリーに対して実行: `local-path-lint` exit 0 / `identity-lint` exit 0 / `comment-policy-lint` exit 0 (685 ファイル、禁止 0 件)。CI で即赤にはならない
- 新設された `samples` スコープの負ケースを自分で構成して実測: `samples/` 配下に `DEVELOPMENT_TEAM` と署名アイデンティティを置くと `identity-lint` が exit 1 + 行番号付きで検出、ローカル絶対パスを置くと `local-path-lint` が exit 1 + 行番号付きで検出。Requirement「lint の検証」の Scenario「samples 配下の識別子検出」は成立する (probe ファイルは検証後に削除済み、`git status` clean)
- 外部 action の SHA 照合: `actions/checkout@3d3c42e…` = v7.0.1、`actions/setup-java@dd06d9cb…` = v6.0.0、`actions/cache@55cc8345…` = v6.1.0、`actions/setup-dotnet@a98b5685…` = v6.0.0。全て一致
- gitleaks の固定値照合: `gitleaks_8.30.1_linux_x64.tar.gz` の公式 digest = `sha256:551f6fc8…70eb`。`.github/workflows/ci.yml:35` の `KS_GITLEAKS_SHA256` と一致
- ランナーイメージのマニフェスト照合 (`actions/runner-images`): `macos-26` に `/Applications/Xcode_26.5.app` が存在し、`Xcode_26.5*` の glob + `sort -V` は正しく解決する。iOS 26.5 Simulator ランタイムに iPhone 系デバイスがあり、`iOS-(\d+)-(\d+)` の抽出も成立する。Android SDK (platform 35) も同梱されているため、maui job の Android binding ビルドが `android/gradlew` を呼ぶ経路も成立する見込み

## 照合した規約

`kasane/handbook/index.md` の cross ドメインを開き、担当範囲 (CI での 3 platform ビルド・テスト実行、lint 実行、`kasane/config.yaml`) に当たる文書を読んだ。

| 文書 | 適用のきっかけ |
|---|---|
| `handbook/cross/comment-policy.md` | 常時 (ただし `comment_policy_rules.py` の `TARGET_EXT` に `.yml` は含まれず、workflow 自体は lint 対象外) |
| `handbook/cross/test-execution.md` | テストを実行するとき・テスト結果を報告するとき |
| `handbook/cross/local-development-setup.md` | 本体のビルド / lint を行うとき |

- `test-execution.md` の「iOS は `swift test` ではなく Simulator 実行」「Android のディレクトリ名は variant 名ではなくタスク名 (`testDebugUnitTest` / `testReleaseUnitTest`)」「実行件数を確認するところまでが検証」は、いずれも実装に正しく反映されている。`verify-android.yml` の `VARIANT_TASKS` はタスク名を使っており、規約の落とし穴を踏んでいない
- `test-execution.md` の「Gradle は up-to-date なテストタスクをスキップし、テスト 0 件で BUILD SUCCESSFUL になり得る」に対しては、`build/` をキャッシュしない設計 (`verify-android.yml:28-33` のコメント) が対応になっている。整合している
- `local-development-setup.md` の「MAUI の `dotnet build` は Android SDK を自身で解決できる」により、`verify-maui.yml` が `ANDROID_HOME` を明示していない点は問題にならない
- `decisions/cross/index.md` からは ADR-0024 (外部 PR を受け付けない) を参照。トリガー選択と整合
- `lessons/code-review.md` の重点観点 L-001 (ミューテーションによる検出力の実測) を適用し、lint 4 検査のうち手元で再現できる 2 つについて負ケースを実際に構成して落ちることを確認した (上記「実行した検証」)

## 指摘事項

### [🟠 Major] `ubuntu-latest` が Requirement「ツールチェーンの再現性」の固定境界に反する

**該当箇所**: `.github/workflows/verify-android.yml:16`、`.github/workflows/ci.yml:38`

**問題点**:
デルタスペック Requirement「ツールチェーンの再現性」は固定境界を「**ランナーイメージは版指定 (`macos-26` 等)**」と明示し、Scenario「版の変更が diff に現れる」で「固定境界の粒度で版が diff なしに変わることはない」ことを要求している。iOS / MAUI は `macos-26` と版指定になっているのに対し、android job と lint job は `ubuntu-latest` のままで、これは版指定ではない。

これは机上の懸念ではない。`actions/runner-images` には既に `Ubuntu2604-Readme.md` が存在し、`ubuntu-latest` の指す実体は 24.04 から 26.04 へ移行しうる。移行が起きると、本リポジトリの diff は一切変わらないまま JDK 既定版・Android SDK 同梱内容・python3 の版・プリインストールツールが入れ替わる。Requirement が防ごうとしているのはまさにこの経路である。

`deviation.md` にもこの差分は記録されていないため、合意済みの乖離ではない。

**推奨修正**:
`ubuntu-latest` を `ubuntu-24.04` のような版指定に置き換える (macOS 側と同じ粒度)。ubuntu だけ追随運用のコストを避けたいという判断があるなら、実装ではなく `deviation.md` に「ubuntu は版指定の対象外とする」根拠付きで記録し、Requirement の固定境界一覧との差分を明示する。

---

### [🟡 Minor] android の module 導出が行頭一致で、期待集合が静かに縮む経路がある

**該当箇所**: `.github/workflows/verify-android.yml:73-80`

**問題点**:
導出は「`strip()` した行が `include(` で始まる」行だけを見て、その行内の `"(:[^"]+)"` を拾う。現行の `android/settings.gradle.kts` (4 つの `include(":…")` が各 1 行、説明はすべて `//` の独立行) では正しく 4 module を得るし、`//` コメント行は `include(` で始まらないため拾わない — ここは正しく設計されている。

問題は将来の書き方の揺れに対する挙動が「安全側に倒れない」ことである。

- `include(\n    ":ks-settingsview-foo"\n)` のような複数行形式や Groovy 記法 `include ":x"` に書き換わると、その module は期待集合から**静かに消える**。`if not modules` のガード (`verify-android.yml:79`) は全滅したときしか発火しないため、4 module のうち 1 つが落ちても検査は緑のまま通る
- 逆に `include(":a") // かつては :b もあった` のような行末コメントは `:b` を拾い、存在しない組を要求して誤って fail する

前者は Requirement「Android の検証と実行件数の担保」が塞ごうとしている「タスクごと実行されなかった組を見逃す」穴の再発に相当する。summary の表には導出結果が出るので人間には見えるが、**job は失敗しない**。

**推奨修正**:
ビルド側の権威から取るのが最も堅い (例: `./gradlew -q projects` の出力、または `--write-locks` 系の成果物からの列挙)。ファイル解析を続けるなら、行頭一致をやめてファイル全文から `//` と `/* */` を除去したうえで `include\s*\(?[^)\n]*` を走査する形にし、あわせて導出した module 一覧を `::notice` で出力して差分に気づける状態にする。

---

### [🟡 Minor] gitleaks の走査対象が空でも job が緑で通る

**該当箇所**: `.github/workflows/ci.yml:62-70`

**問題点**:
`git archive --format=tar HEAD | tar -x -C "$scan_dir"` の後、`gitleaks dir "$scan_dir"` を回す。展開結果が空でも `gitleaks` は「検出 0 件」で exit 0 になるため、**secret scan が実質的に何も見ていない状態で緑になる**。

GitHub Actions の `run:` の既定シェルは `bash -e {0}` であり `pipefail` は入らない (`shell: bash` を明示した場合のみ `-eo pipefail`)。したがってパイプ前段の `git archive` が落ちても、後段の `tar` が 0 で終われば step は成功する。実際には GNU tar は不完全な入力に対して非 0 で終わることが多く発生確率は低いが、Requirement「lint の検証」が守る最後の砦がこの一手で無検査になるため、確率ではなく構造で塞ぐべき箇所である。

同じ step の `gitleaks version` も、失敗しても `-e` で止まる (これは問題ない)。

**推奨修正**:
step 先頭に `set -euo pipefail` を置き、`gitleaks` 実行前に展開結果が空でないことを検査する (例: `find "$scan_dir" -type f | head -1` が空なら `::error` + exit 1、あるいは `git ls-files | wc -l` と展開後のファイル数を突き合わせる)。

---

### [🟡 Minor] iOS の件数サマリでフォールバックが到達不能、「取れなかった」と「0 件」が区別できない

**該当箇所**: `.github/workflows/verify-ios.yml:102`

**問題点**:
```
grep -E '^[[:space:]]*Executed [0-9]+ tests?' "$log" | tail -n 20 || echo "(Executed 行が見つからない)"
```
パイプラインの終了コードは最後の `tail` のもので常に 0 のため、`grep` が 1 行も拾えなくても `||` の側は**決して実行されない**。この場合 job summary には空のコードブロックだけが出力され、「ログの形式が変わって件数を取れなかった」のか「テストが 0 件だった」のかが読み手に区別できない。

Requirement「iOS の検証」は Scenario で「実行件数がログで確認できる」ことしか要求しておらず、生の `xcodebuild` 出力には件数が残るため spec 違反ではない。ただし本 step の存在意義 (summary で件数を確認できるようにする) が静かに失われる。

**推奨修正**:
一度変数に取ってから空判定する。
```
executed=$(grep -E '^[[:space:]]*Executed [0-9]+ tests?' "$log" | tail -n 20 || true)
[ -n "$executed" ] || executed="(Executed 行が見つからない)"
```

---

### [🔵 Suggestion] iOS だけ「実行 0 件」のゲートが無い

**該当箇所**: `.github/workflows/verify-ios.yml:90-104`

**問題点**:
android は XML 欠落と `tests` 合計 0 で fail し、maui は合計 0 件で fail するのに対し、iOS は件数を summary に出すだけでゲートが無い。`xcodebuild test` がテストを 1 件も実行せずに成功で終わった場合、job は緑になる。

デルタスペックは iOS に件数検査を課していない (Requirement「iOS の検証」の要求はテスト失敗での job 失敗と件数のログ可視化まで) ため、**これは spec 違反ではない**。ただし `handbook/cross/test-execution.md` は「実行件数を確認するところまでが検証」を platform 共通の規律として置いており、3 platform のうち iOS だけがその規律の外に出ている。

**推奨修正**:
上記 Minor の変数化と同時に、抽出した `Executed N tests` の合計が 0 なら `::error` + exit 1 とする。spec に無い要求を足す判断になるため、実装前にオーナーの合意を取るか、`deviation.md` に「spec より厳しくした」旨を記録するのが筋。

---

### [🔵 Suggestion] `timeout-minutes` が無く、macOS job が固まると 6 時間消費する

**該当箇所**: `.github/workflows/verify-ios.yml:20-22`、`.github/workflows/verify-maui.yml:23-25`

**問題点**:
job の既定タイムアウトは 360 分。iOS Simulator の起動待ちや `xcodebuild test` のハングは実際に起こりうる事象で、macOS ランナーは Linux の約 10 倍のコストがかかる (github-workflow-skill 注意事項 6)。公開リポジトリで PR ごとに毎回回る構成であることを踏まえると、上限を切っておく価値が高い。

**推奨修正**:
ios / maui の job に `timeout-minutes` を置く (tasks 4.1 で所要時間を実測してから、その 2〜3 倍を目安に設定するのが自然)。

---

### [🔵 Suggestion] `concurrency` が無く、連続 push で macOS job が積み上がる

**該当箇所**: `.github/workflows/ci.yml:7-14`

**問題点**:
paths フィルタを持たない設計 (Requirement「CI の起動条件」) と組み合わさるため、PR への連続 push でその都度 macos-26 job が 2 本ずつ起動し、古い実行もキューに残ったまま走り切る。

**推奨修正**:
`ci.yml` に `concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }` を置く。ただし tasks 4.1 の所要時間実測前に入れると測定条件が変わるため、実測後の適用が扱いやすい。

---

### [🔵 Suggestion] `lint.identity.scope` のコメントが列の内容と食い違う

**該当箇所**: `kasane/config.yaml:70`

**問題点**:
```
scope: [kasane, openspec, skills, samples]   # 検査範囲 (ソースには正当な UUID 定数があるため含めない)
```
`samples/` はソースコードを含むディレクトリであり、「ソースには正当な UUID 定数があるため含めない」というコメントの理由付けと、列に `samples` が入っている事実が矛盾して読める。将来この行を読んだ人が「間違って入っている」と判断して外す余地が残る。コメントが単独で理解できるかという観点 (ksn-review 設計品質) に該当する。

**推奨修正**:
`samples` を例外として入れた理由 (Xcode の実機ビルドが `DEVELOPMENT_TEAM` を samples 配下へ書き戻すため、その捕捉が目的) を同じコメントに書き添える。

---

### [🔵 Suggestion] `global.json` の `rollForward` 未指定で .NET SDK が完全には固定されない

**該当箇所**: `global.json:2-5` (本 diff の対象外)、`.github/workflows/verify-maui.yml:47-50`

**問題点**:
Requirement「ツールチェーンの再現性」は固定境界を「.NET SDK と workload set は `global.json` の完全指定」としている。現行の `global.json` は `version: "10.0.300"` のみで `rollForward` を持たないため既定の `latestPatch` が効き、同一 feature band 内のより新しい patch が選ばれうる。`macos-26` イメージには 10.0.302 / 10.0.400 が同梱されており、`setup-dotnet` が 10.0.300 を追加インストールしても解決結果が 10.0.302 になる余地がある。

ただし `tasks.md` の備考が `global.json` を「既存 — 本変更では触らない (前提条件)」と明示しているため、**本 change の実装欠陥ではない**。前提そのものが Requirement の文言を満たしきっていない可能性がある、という指摘である。

**推奨修正**:
本 change では触らない。tasks 4.1 の実測時に `dotnet --version` の実値を記録し、10.0.300 以外が選ばれていたら `"rollForward": "disable"` の追加を別 change (またはオーナー判断で本 change の付随修正) として扱う。

## 確認して問題なかった観点

コンテキストパッケージで指定された観点のうち、指摘に至らなかったものを証跡として残す。

- **Requirement「CI の起動条件」**: `pull_request.branches: [develop, main]` + `push.branches: [develop]`、paths フィルタなし。過不足なし (`.github/workflows/ci.yml:8-14`)
- **Requirement「platform workflow の再利用契約」**: 3 本とも `on: workflow_call` のみで必須 inputs を持たず、CI 入口を経由せず `uses:` で単独呼び出しできる
- **Requirement「Android の検証と実行件数の担保」**: 期待集合を `settings.gradle.kts` から導出し、XML 欠落と `tests` 合計 0 の**両方**を `failures` に積んで exit 1 する。`//` コメント行は `include(` 始まりでないため拾わない。結果ディレクトリ名は variant 名ではなくタスク名を使っており handbook の落とし穴を回避している (上記 Minor は将来の書き方の揺れに対する脆さの指摘であって、現行構成での誤りではない)
- **Requirement「MAUI の検証」**: TRX が 1 件も無い場合も `total = 0` で exit 1 になるため、「件数取得が壊れたときに 0 件だが成功へ倒れる」経路が無い。`if: always()` によりテスト step 失敗時も件数検査が走る
- **Requirement「iOS の検証」**: `swift test` は成否判定に使われていない。destination は機種名ではなく `xcrun simctl list --json` から解決した UDID を使っており、イメージ更新での機種名変更に強い。`xcodebuild … | tee` には `set -o pipefail` が明示されており、テスト失敗がパイプで握り潰されない (`.github/workflows/verify-ios.yml:88-94`)
- **Requirement「ツールチェーンの再現性」(ubuntu 以外)**: Xcode 選択は ios / maui の両方に同じ形で入り、`DEVELOPER_DIR` を `GITHUB_ENV` 経由で後続 step に効かせている。Xcode 不在時は `::error` + exit 1 で明示的に落ちる。JDK は Temurin 17 を android / maui 双方で `setup-java` により明示
- **Requirement「lint の検証」**: 4 検査が独立した step として並び、いずれも違反で exit 1 する (2 検査は手元で負ケースを実測)。`identity-lint` の検査範囲に `samples` が入った
- **status check context 名**: 呼び出し側 job 名 (`ios` / `android` / `maui`) と reusable 側 job 名 (`verify`) を両方 `name:` で明示しており、`ios / verify` 等の context 名が固定される。意図と壊れ方が `ci.yml:19-21` のコメントに残っている
- **`permissions`**: 4 workflow すべてに `permissions: contents: read` があり最小。reusable 側でも明示されているため、呼び出し元の権限が広がっても縮小される
- **外部 action の pin**: 4 件すべて commit SHA 固定で、SHA とコメントのタグが実際に一致することを照合済み。gitleaks は action ではなく版 + SHA-256 固定の CLI (deviation 記録済み)
- **足場アーティファクト**: `tasks.md` の diff はチェックボックスのみ。`proposal.md` / `specs/` の書き換えなし
- **deviation.md**: 記録された 2 件 (マージ保護を develop のみに限定 / gitleaks を CLI 実行 + `git archive` 走査) はいずれも根拠付きで、合意済み差分として扱った。付随修正の記載は無く、スコープ膨張も見られない

## アクションプラン

1. **[Major]** `verify-android.yml:16` と `ci.yml:38` の `ubuntu-latest` を版指定に置き換える (または `deviation.md` に固定境界の例外として記録する)
2. **[Minor]** `ci.yml:62-70` の secret scan に `set -euo pipefail` と「展開結果が空でないこと」の検査を入れる
3. **[Minor]** `verify-ios.yml:102` の件数抽出を変数化し、フォールバックが実際に発火するようにする
4. **[Minor]** `verify-android.yml:73-80` の module 導出をビルド側の権威から取るか、コメント除去 + 全文走査に変え、導出結果を `::notice` で可視化する
5. **[Suggestion]** `kasane/config.yaml:70` のコメントに `samples` を入れた理由を書き添える
6. **[Suggestion]** tasks 4.1 の所要時間実測後に、`timeout-minutes` と `concurrency` の導入、および `dotnet --version` 実値に基づく `global.json` の `rollForward` 要否を判断する
7. **[Suggestion]** iOS の 0 件ゲート追加はオーナー判断 (spec 超過のため、入れるなら `deviation.md` に記録)
