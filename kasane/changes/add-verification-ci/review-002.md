# レビュー結果: add-verification-ci (002 回目)

**日付**: 2026-08-31
**判定**: APPROVED

## サマリー

commit `9642652` は review-001 の Major 1 件・Minor 3 件・Suggestion 4 件に対応しており、いずれも**指摘された経路が実際に塞がっている**ことを手元のミューテーションで確認した。特に gitleaks の `pipefail` 欠落 (修正前は `git archive` が落ちても exit 0 で素通り、修正後は exit 128) と android の module 導出の縮み検出 (include を 1 件落とすと exit 1) は、静的読解ではなく実行で確かめた。

修正が新たに持ち込んだ欠陥のうち Critical / Major に相当するものは無い。残るのは、`concurrency` の group 設計が連続 push 時に中間 commit の実行を pending 段階で打ち切りうる点 (Minor 1 件、影響は限定的で tip は常に検証される) と、新設した検査が将来偽の失敗を出しうる条件・コメントの断定が実挙動より強い点 (Suggestion 5 件) である。いずれも本サイクルでの再修正を待たずに先へ進めてよい。

`identity-lint.py` の 2 つの変更については、コンテキストパッケージの依頼どおり「検出すべきものを取りこぼす方向に効いていないか」を自分で実測した。**mDNS ホスト名の検出は維持されている** (下記「実行した検証」)。

### 実行した検証

本 change はライブラリのコード・テストに触れないため、diff が新設・変更した検査そのものを手元で回した (`lessons/code-review.md` L-001 のミューテーション実測)。作業後の `git status` は clean、リポジトリへの残留物なし。以下、ホスト名の例は `<host>` に置き換えて記す (実測時は英数 + ハイフンの実在形で入力した)。

**ビルド / テスト相当 (lint 3 種の実行)**

- `python3 scripts/local-path-lint.py` exit 0 / `python3 scripts/identity-lint.py` exit 0 / `python3 scripts/comment-policy-lint.py` 685 ファイル・禁止 0 件。4 workflow の YAML パースも全て成功

**identity-lint の検出力 (HOST_LOCAL の否定先読み)** — 17 ケースを新旧パターンで比較

| 入力の形 | 修正後 | 判定 |
|---|---|---|
| `<host>.local` 単体 / `http://<host>.local:8080/` / `adb connect <host>.local:5555` | 検出 | 維持 |
| `Host: <host>.local` / `<host>.local` + 直後に日本語句点 / バッククォート囲み / 丸括弧囲み / 直後にカンマ | 検出 | 維持 |
| `<host>.local.` (末尾ドット付き FQDN 形) / `http://<host>.local./path` | 検出 | 維持 |
| `ssh user@<host>.local` | 検出 (`email` として) | 維持 |
| `settings.local.json` / `.claude/settings.local.json` / `appsettings.local.yaml` / `android/local.properties` / `ro.product.locale=ja` | 不検出 | 意図どおり (誤検出の解消) |
| `<host>.local.1` (`.local` の直後にドット + 数字) | 不検出 (旧は検出) | 唯一の縮み。deviation 記録の設計どおりで、mDNS ホスト名の現実的な出現形ではない |

**identity-lint の `GREP_PATTERN` から `\b` を除いた影響** — `\.local\b` → `\.local` は候補行を**広げる**変更であり、最終判定は `find_identities` が行う (`scripts/identity-lint.py:284`)。検出力が落ちる経路は無い。hook 経路 (`scripts/identity-lint.py:330-333`) は `GREP_PATTERN` を通らず `find_identities` を直接呼ぶため、この変更の影響を受けない

**gitleaks の走査対象検査**

| ケース | 結果 |
|---|---|
| 現行ツリー正常系 | 走査対象 2278 = 追跡 2278 で通過 (偽の失敗なし) |
| `git archive` 失敗 + `pipefail` 有り (修正後) | exit 128 で停止 |
| `git archive` 失敗 + `pipefail` 無し (修正前相当) | exit 0 で**素通り** — 指摘した経路が実在したことの裏付け |
| 展開後に 5 ファイル欠落させた部分展開 | 2273 < 2278 を検出し exit 1 |

**android の module 導出と実体突き合わせ** — 疑似 `android/` ルート 8 パターン

| ケース | 結果 | 評価 |
|---|---|---|
| 現行 `settings.gradle.kts` | 4 module を導出して通過 | 正 |
| 複数行 `include(` + Groovy 記法 `include ":x"` の混在 | 4 module を導出して通過 | review-001 の指摘 (記法の揺れで静かに縮む) が解消 |
| include を 1 件落とす | exit 1 (`ks-settingsview-bridge` を名指し) | 縮みの検出成立 |
| module を settings と実体の両方に追加 | 通過 | 通常の module 追加で偽の失敗は出ない |
| `includeBuild("build-logic")` + `android/build-logic/build.gradle.kts` | **exit 1 (偽の失敗)** | 下記 Suggestion |
| settings に含めない `android/sample-app/build.gradle.kts` | **exit 1 (偽の失敗)** | 下記 Suggestion |
| ネスト module (`:features:foo`) の include が欠落 | 通過 (**検出できない**) | 下記 Suggestion |
| Groovy の `build.gradle` を持つ module の include が欠落 | 通過 (**検出できない**) | 下記 Suggestion |

**iOS の実行件数ゲート** — 合成ログ 6 パターン

| ログ | 結果 |
|---|---|
| `Executed 338 tests, with 0 failures` | exit 0 |
| `Executed 0 tests` のみ | exit 1 (「テストが 1 件も実行されていない」) |
| `Executed` 行なし (`** TEST SUCCEEDED **` のみ) | exit 1 (「ログから実行件数を抽出できなかった」) |
| `Executed 0 tests` + `Executed 12 tests` の混在 | exit 0 |
| `Executed 1 test, with 1 failure` | exit 0 (件数検査としては正) |
| ログファイル自体が無い | exit 1 |

review-001 で指摘した「`grep` の出力をパイプした先で `||` のフォールバックが到達不能」は変数化で解消され、フォールバック文言が実際に出力される。

**CI 実行の実測 (run 33354483531)** — `lint` 10s / `android / verify` 5m51s / `ios / verify` 7m21s / `maui / verify` 7m12s、4 job とも success。ただし判定は緑であることではなくロジックの読解と上記ミューテーションに基づく

## 照合した規約

`kasane/handbook/index.md` → `cross/index.md` を開き、担当範囲 (CI での 3 platform ビルド・テスト実行、lint 実行、`scripts/identity-lint.py`、`kasane/config.yaml`) に当たる文書を読んだ。

| 文書 | 適用のきっかけ |
|---|---|
| `handbook/cross/comment-policy.md` | 常時 (本 diff はコメントを大量に追加している) |
| `handbook/cross/test-execution.md` | テストを実行するとき・テスト結果を報告するとき |

- `comment-policy.md`: 追加された全コメント (workflow 4 本・`identity-lint.py`・`config.yaml`) を禁止類型で照合した。変更提案 ID・レビュー通番・タスク通番・`kasane/changes/` 配下のパス・`SHALL` 等の spec キーワード・履歴記述はいずれも混入していない。`verify-ios.yml:7` の「(android / maui の検証と同じ規律)」はリポジトリ内ファイルへの参照であり許容範囲。**規約適合の観点で残る問題は「コメントの内容が実挙動より強い断定になっている」型が 3 箇所ある点**で、これは lint の検出範囲外 (規約の「そのファイルだけを読んでいる人にとって意味が通る」は満たすが、内容が正しくない) — Suggestion に列挙した
- `test-execution.md`: 「実行件数を確認するところまでが検証」を iOS にも適用した本 diff は、規約と整合する方向の変更。Android の結果ディレクトリ名 (variant 名ではなくタスク名) の扱いは前サイクルから不変で、規約どおり
- `lessons/code-review.md` L-001 (ミューテーションによる検出力の実測): gitleaks / android 導出 / iOS ゲート / identity-lint の 4 箇所すべてで適用した。`lessons/code-review.md` の「指摘しないこと」は昇格済みルールなし

## 指摘事項

### [🟡 Minor] `concurrency` の group が push で ref 単位のため、連続 merge 時に中間 commit の検証が pending 段階で打ち切られる

**該当箇所**: `.github/workflows/ci.yml:20-25`

**問題点**:
```yaml
group: ci-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
cancel-in-progress: ${{ github.event_name == 'pull_request' }}
```
PR 由来の実行は PR 番号で分離され、`cancel-in-progress` も PR のときだけ真になる。ここまでは意図どおりで、PR の実行が `develop` への push の実行を打ち切ることはない。

問題は `cancel-in-progress: false` が「打ち切らない」を意味しないことである。GitHub Actions の concurrency は、**同一 group に in-progress の実行があるとき新しい実行を pending にし、その group に既にいた pending の実行をキャンセルする**。`develop` への push はすべて `ci-CI-refs/heads/develop` の 1 group に入るため、CI 1 周 (実測 7 分半) の間に 3 回 merge されると、真ん中の merge commit に対する実行は一度も走らないままキャンセルされる。

これはデルタスペック Requirement「CI の起動条件」の Scenario「develop へのマージ後にも検証される」(THEN 4 job が**マージ結果に対して**実行される) が保証しようとした状態を、限定的に崩す。ただし影響は限定的で、常に最新の tip に対する実行は完走するため「未検証のコードが緑と表示される」経路にはならない (失われるのは commit 単位の切り分け情報)。**この点で優先度は低い。**

一方で、同じ箇所のコメントは「develop への push で起動した実行は打ち切らない — マージ結果に対する検証は後続の push に追い越されても完走させる必要がある」と、設定が提供していない保証を断定している。次に読む人はこの記述を根拠に安全と判断してしまう。

**推奨修正**:
push 由来の実行を commit 単位の group に分ければ、pending の待ち合わせ自体が起きず Scenario を完全に保てる。

```yaml
group: ci-${{ github.workflow }}-${{ github.event.pull_request.number || github.sha }}
```

`cancel-in-progress` の式はそのままでよい。設計として現状 (ref 単位) を選ぶなら、コメントを「後続の push が来ると、待機中の実行はキャンセルされる (常に最新 tip の実行だけが完走する)」という実挙動の記述に直す。

---

### [🔵 Suggestion] android の実体突き合わせが偽の失敗を出す条件を持ち、逆にネスト module と Groovy build ファイルを覆っていない

**該当箇所**: `.github/workflows/verify-android.yml:92-108`

**問題点**:
`android/` 直下で `build.gradle.kts` を持つディレクトリ (`buildSrc` を除く) が導出結果に現れなければ失敗させる、という突き合わせ。review-001 で指摘した「導出が静かに縮む」経路は実測でふさがれている (include を 1 件落とすと exit 1) 一方、この形には次の非対称がある。

偽の失敗になる条件 (いずれも実測済み):

- `pluginManagement { includeBuild("build-logic") }` のような composite build を足すと、`android/build-logic/build.gradle.kts` が「導出できなかった module」として名指しされて失敗する。convention plugin の導入は Android のモノレポでは通常の進化であり、除外が `buildSrc` 固定の現状では拾えない
- settings に含めないサンプルアプリ等 (`android/sample-app/build.gradle.kts`) を置いた場合も同様に失敗する

覆えていない条件 (いずれも実測済み):

- ネスト module (`include(":features:foo")`) が settings から落ちても検出できない。突き合わせは `android/` 直下しか見ないため
- `build.gradle` (Groovy) で書かれた module が settings から落ちても検出できない

コメント (`.github/workflows/verify-android.yml:94-95`) の「module が増えたときは実体側にも現れるため、この検査は追加を誤って失敗にはしない」は、`includeBuild` とサンプルアプリの 2 ケースで成り立たない。

なお失敗の出方は「ディレクトリ名を名指しした exit 1」であり、静かに素通りする方向ではない。本 change のスコープ (現行 4 module 構成) では実害は無いため、優先度は低い。

**推奨修正**:
突き合わせを `settings.gradle.kts` の記法解析ではなくビルド側の権威に寄せる (`./gradlew -q projects` の出力から module を列挙する) と、上記 4 条件がすべて同時に解消する。テキスト解析を続けるなら、(a) `build.gradle` も module 判定に含める、(b) 走査を `android/` 直下から 2 階層に広げる、(c) `includeBuild(...)` に現れたディレクトリ名を除外集合に加える、の 3 点を足し、コメントの断定を「settings に含めない build ファイルを `android/` 直下に置くと失敗する」という実挙動の記述に直す。

---

### [🔵 Suggestion] gitleaks の件数突き合わせが `export-ignore` / submodule の導入で偽の失敗になる

**該当箇所**: `.github/workflows/ci.yml:85-94`

**問題点**:
`git ls-files` の件数と `git archive HEAD` 展開後のファイル数を突き合わせる形。現行リポジトリでは 2278 = 2278 で一致し、部分展開を模したケースでは正しく exit 1 になることを実測した。指摘した経路は塞がっている。

ただしこの等式は「`git archive` の出力 = index の全エントリ」という前提に依存しており、次のどちらかが将来入ると**検査対象は正常なのに失敗する**:

- `.gitattributes` に `export-ignore` を書く (配布アーカイブを絞る一般的な運用)。`git archive` から除外されるが `git ls-files` には残る
- submodule を追加する。`git ls-files` は gitlink を 1 エントリとして数えるが `git archive` は中身を出力しない

現在はどちらも存在しない (`.gitattributes` / `.gitmodules` とも不在) ため実害は無いが、そのときのエラー文言は「走査対象の展開に失敗している」であり、原因の見当がつかない。

**推奨修正**:
比較の意図 (「展開が途中で切れていないこと」) をコメントに明記したうえで、除外要因を織り込む。最小の手当ては、`.gitattributes` / submodule を追加したら本検査の期待値も更新する必要がある旨を同じコメントに書き添えること。より堅くするなら `git archive` の出力そのものを数える (`tar -t` のファイルエントリ数と展開後の数を比べる) 形にすれば、`export-ignore` の有無に依存しない。

---

### [🔵 Suggestion] iOS の 0 件ゲートはログ全体で 1 件でも実行されていれば通り、テストターゲット単位の空振りを捕まえない

**該当箇所**: `.github/workflows/verify-ios.yml:110-117`

**問題点**:
判定は `grep -Eq '^[[:space:]]*Executed [1-9][0-9]* tests?'` で、**ログのどこかに 1 件以上の行があれば通る**。実測でも `Executed 0 tests` と `Executed 12 tests` が混在するログは exit 0 になる。

android のゲートは module×variant の組ごとに 0 件を失敗にしており、iOS だけ粒度が全体合計になっている。`handbook/cross/test-execution.md` は `KsSettingsViewUITests` が条件次第で「1 件も実行されない」ことを実測付きで記録しており、テストターゲット単位で空振りする事象はこのリポジトリで現実に起きた型である。現在の scheme (`KsSettingsView-Package` + Simulator destination) はその原因を避けているが、ゲート自体はその再発を捕まえない。

デルタスペック Requirement「iOS の検証」は件数検査を課しておらず、0 件ゲート自体が deviation 記録済みの spec 超過であるため、**これは違反ではない**。粒度を上げるかはオーナー判断。

**推奨修正**:
入れるなら `-resultBundlePath` で result bundle を出力し `xcrun xcresulttool` からターゲット別件数を取るのが正攻法だが、実装コストが上がる。より軽い折衷は、`Executed 0 tests` の行が 1 行でもあれば警告 (`::warning`) を出して summary に残すこと (失敗にはしない)。spec を超える強化になるため、入れる場合は deviation への追記が要る。

---

### [🔵 Suggestion] `timeout-minutes` のコメントが根拠にしている「実測」値が job の実測と食い違う

**該当箇所**: `.github/workflows/verify-ios.yml:25-27`、`.github/workflows/verify-maui.yml:26-28`

**問題点**:
コメントは ios を「実測 5 分に対する上限」、maui を「実測 6 分に対する上限」としているが、実際の job 所要時間は ios 7m21s / maui 7m12s である (run 33354483531)。おそらくテスト step 単体の時間を書いているが、`timeout-minutes` は setup を含む job 全体に効く値であり、根拠として書かれた数字と上限の対象がずれている。

上限値そのものは妥当 (ios 20 分 = 実測の約 2.7 倍、maui 25 分 = 約 3.5 倍) で、機能上の問題は無い。ただし次にこの値を詰める人がコメントの数字を出発点にすると、余裕を過大に見積もることになる。android (`.github/workflows/verify-android.yml:19` の「実測 6 分」に対し実測 5m51s) と lint (`.github/workflows/ci.yml:48` の「実測 9 秒」に対し 10s) は一致している。

**推奨修正**:
ios / maui のコメントの数字を job 全体の実測に合わせる (「実測 7 分台に対する上限」)。

---

### [🔵 Suggestion] deviation.md の記述が hook 経路の挙動と食い違う

**該当箇所**: `deviation.md` (`[付随修正] identity-lint の .local 検出` の `GREP_PATTERN` の項)

**問題点**:
「`\b` は Linux の git では効き macOS では効かないため、**手元の lint と書き込み hook が CI より検査範囲が狭い**状態になっていた」とあるが、hook 経路 (`scripts/identity-lint.py:330-333`) は `GREP_PATTERN` を経由せず入力テキストの全行に `find_identities` を適用する。`GREP_PATTERN` の `\b` の影響を受けていたのは lint 経路 (`scripts/identity-lint.py:284`) だけで、hook の検査範囲は変更の前後で変わっていない。

コードは正しく、記録の側だけが実態より広く書かれている。将来この記録を読んで「hook の検査範囲がこの修正で広がった」と判断すると、hook のカバレッジを実際より高く見積もることになる。

**推奨修正**:
該当箇所を「手元の lint が CI より検査範囲が狭い」に直す (hook への言及を落とす)。レビュー側では書き換えない。

## 確認して問題なかった観点

review-001 の指摘に対する対応の成否と、修正が触った範囲の副作用について、指摘に至らなかったものを証跡として残す。

- **[Major] `ubuntu-latest`**: `.github/workflows/ci.yml:47` と `.github/workflows/verify-android.yml:18` の両方が `ubuntu-24.04` に置き換わり、Requirement「ツールチェーンの再現性」の固定境界 (ランナーイメージは版指定) を満たした。macOS 側 (`macos-26`) と粒度が揃っている
- **[Minor] gitleaks の走査対象が空でも緑**: `set -euo pipefail` + 追跡ファイル数との突き合わせで塞がれた。修正前後の挙動差 (exit 0 → exit 128) と部分展開の検出をいずれも実測
- **[Minor] iOS の件数サマリのフォールバック到達不能**: 変数化 + `[ -z ]` 判定で解消。フォールバック文言が実際に出力されることを実測。`set -uo pipefail` としつつ `-e` を意図的に外している点も正しい (`grep` の exit 1 で step が落ちない)
- **[Minor] android の module 導出が静かに縮む**: 記法の揺れ (複数行 `include(` / Groovy `include ":x"` / 行末コメント) に対する解析が堅くなり、縮みは実体突き合わせで exit 1 になる。導出結果は `::notice` で annotation に出る (run 33354483531 で 4 module の出力を確認)
- **[Suggestion] iOS の 0 件ゲート**: 実行 0 件と抽出失敗の双方が exit 1 になり、deviation.md に spec 超過として記録済み。合意済み差分として扱った
- **[Suggestion] `timeout-minutes`**: ios 20 / android 20 / maui 25 / lint 10。いずれも実測に対する倍率が 2.7 倍以上あり、正常な実行を巻き込む余地は小さい。`uses:` で呼ぶ側の job には `timeout-minutes` を置けないため、reusable workflow の job 側に置いているのは正しい配置
- **[Suggestion] config のコメント矛盾**: `kasane/config.yaml:69-72` が前置コメントに書き換わり、`samples` を例外として含めた理由 (Xcode の実機ビルドによる書き戻しの捕捉) が同じ場所で読めるようになった。矛盾は解消
- **[Suggestion] `global.json` の `rollForward`**: 見送りの判断 (tasks.md の備考が「本変更では触らない (前提条件)」と明示) は妥当。再指摘しない
- **`identity-lint.py` の変更が検出力を落としていないか**: 上記「実行した検証」のとおり、mDNS ホスト名の 11 形式で検出が維持され、ファイル名 5 形式の誤検出だけが消えた。`GREP_PATTERN` からの `\b` 除去は候補行を広げる方向で、検出力の低下経路は無い。全体実行も exit 0
- **`concurrency` が PR 側の Scenario を壊していないか**: PR 由来の実行は PR 番号で group が分かれるため、PR の連続 push で打ち切られるのは同一 PR の古い実行だけ。Scenario「PR で全 job が起動する」「main への PR でも起動する」は影響を受けない。`cancel-in-progress` に式を書く形は仕様上有効
- **足場アーティファクト**: 本 commit の変更は `deviation.md` への追記 2 件 (append-only、既存記述の書き換えなし) と `review-001.md` の追加のみ。`proposal.md` / `specs/` / `tasks.md` は本 commit では無変更。commit 後に `tasks.md` の 4.1〜4.5 がチェック済みへ更新されているが、いずれも本レビュー時点で確認できる実績 (run 33354483531 の 4 job success と所要時間、`::notice` の module 出力、review-001 で実測した lint の負ケース) に対応しており、虚偽のチェックは見当たらない。4.6 と 5.x は未チェックのまま
- **付随修正の同梱条件**: `[付随修正] identity-lint の .local 検出` は `scripts/identity-lint.py` 1 ファイルに閉じ、公開 API・データスキーマ・ADR に触れず、正/負ケースの実測で担保されている。ksn-core の同梱条件 (①〜⑤) を満たす
- **コメント規約 (`handbook/cross/comment-policy.md`)**: 追加された全コメントに禁止参照 (change ID・レビュー通番・タスク通番・`kasane/changes/` のパス・spec キーワード) と履歴記述の混入なし
- **workflow の構造**: 4 本とも YAML パース成功。`permissions: contents: read` は 4 本すべてで維持。外部 action の SHA pin と gitleaks の版 / SHA-256 は本 commit で変更されていない (review-001 で照合済み)

## アクションプラン

優先度順。いずれも本サイクルのブロッカーではない。

1. **[Minor]** `.github/workflows/ci.yml:24` の concurrency group を push 時に `github.sha` へ分けるか、コメントを実挙動の記述に直す
2. **[Suggestion]** `.github/workflows/verify-android.yml:92-108` の突き合わせを `./gradlew -q projects` に寄せる (偽の失敗 2 条件と未カバー 2 条件が同時に解消する)。当面はコメントの断定だけでも直す
3. **[Suggestion]** `.github/workflows/verify-ios.yml:26` / `.github/workflows/verify-maui.yml:27` の「実測」値を job 全体の実測に合わせる
4. **[Suggestion]** `deviation.md` の `GREP_PATTERN` の項から hook への言及を落とす
5. **[Suggestion]** `.github/workflows/ci.yml:85-94` のコメントに `export-ignore` / submodule を入れたら期待値の更新が要る旨を書き添える
6. **[Suggestion]** iOS のテストターゲット単位の 0 件検知はオーナー判断 (入れるなら deviation への追記が要る)
