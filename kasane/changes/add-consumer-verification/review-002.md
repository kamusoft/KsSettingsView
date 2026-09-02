# レビュー結果: add-consumer-verification (002 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

1 周目の指摘 (ホスト側 Major 1 / Minor 3 / Suggestion 4、相方由来の採用分 5 件) は 12 件すべてが実物に反映されており、いずれも手元の再実測で意図どおりに効くことを確認した。とくに Android SDK ロケーションの解決 (Major-1) と `smoke` + 準備済み参照先の早期拒否は、分岐を隔離した実行と 3 platform × 2 スクリプトの負ケースで直接確かめている。修正による回帰も見つからなかった (本体 Android 2700 tests / 失敗 0、lint 4 本 + README 一致 lint、workflow の入力検査 15 通りの分岐)。

新規の指摘は Suggestion 4 件のみで、いずれも構造にも Scenario の成立にも触れない。Critical / Major / Minor はなく、修正なしでもこのまま進めてよい状態と判断する。

## 照合した規約

handbook (作業ドメイン cross + 触る android / maui / ios の index):

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` | 常時 | 適用。今回変更・追加されたコメント (`verification/android/android-sdk.sh` 全体、消費者 app の署名・R8 の 2 箇所、MAUI の既定 version 3 箇所、`android/build.gradle.kts` の version 注入、SNAPSHOT ガードのメッセージ) を規約本文と 1 つずつ照合。禁止参照 (作業文書パス・変更識別子・ローカル通番・行番号)・禁止記述類型 (履歴記述・SHALL 等) の混入なし。ADR は `cross/ADR-0020` の ID 形式で、いずれも説明が自己完結したうえでの添え書き |
| `kasane/handbook/cross/test-execution.md` | テストを実行・報告するとき | 適用 (下記「実行した検証」に件数を併記) |
| `kasane/handbook/cross/public-identifiers.md` | `**/build.gradle.kts` / `**/*.csproj` を触るとき | 適用。今回の修正で識別子・配布座標に変更なし |
| `kasane/handbook/cross/local-development-setup.md` | 環境構築の手順が要るとき | 適用 (guide)。Major-1 の修正は経路 (a) (スクリプト内で完結) を採ったため、handbook 側の更新は不要。`android/local.properties` 経路の環境でも消費者検証が動くことを実測で確認 |
| `kasane/handbook/cross/sample-parity.md` / `user-skill-api-listing.md` / `runtime-behavior-verification.md` / `aiforms-origin-reference.md` | `samples/` / `skills/` / 実行時挙動 / 未移植機能 | 適用外 (該当変更なし) |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/` を触るとき | 適用外 |
| `kasane/handbook/maui/integration-host-verification.md` / `performance-verification.md` | binding / facade の疎通・描画性能 | 適用外 |

決定 (照合のみ、抵触なし): `cross/ADR-0020` (version 注入は `-Pversion=` / `-p:Version=`。SNAPSHOT ガードのメッセージがこの文言に揃った)、`cross/ADR-0025` (両側 job 名の固定)、`cross/ADR-0026` (CI の保証範囲)、`cross/ADR-0018` / `ADR-0019` / `ADR-0023`、`android/ADR-0016`。

lessons: `kasane/lessons/code-review.md` の重点観点 L-001 (検出力は静的読解で争わず実測する) を、SDK 解決の分岐・`--reference` の排他性・`providers.gradleProperty` の環境変数解決に適用した。「指摘しないこと」は昇格済みルールなし。

## 前回指摘の解消状況

| # | 前回の指摘 | 現物 | 判定 |
|---|---|---|---|
| 🟠 Major-1 | Android 消費者が `ANDROID_HOME` 必須で `local.properties` 経路では動かない | `verification/android/android-sdk.sh` を新設し、両スクリプトが `ksv_ensure_android_home` を呼ぶ。`ANDROID_HOME` → `ANDROID_SDK_ROOT` → `android/local.properties` の `sdk.dir` の順で解決し、`verification/android/local.properties` は生成しない | **解消**。環境変数を外した実行で exit 0、解決メッセージが出ること、`local.properties` 不在で自己説明的に exit 1、`sdk.dir` が実在しないとき別メッセージで exit 1 を隔離実行で確認 |
| 🟡 Minor-1 | 消費者 app のコメント 2 箇所が実挙動と一致しない | `verification/android/app/build.gradle.kts:53-54` は「signingConfig を割り当てないため release の出力は未署名 APK になる」、`:73-74` は「Compose の推移依存として入る androidx.activity の版を固定する」に書き直された | **解消**。どちらも実挙動 (未署名 APK・Activity 宣言なし) と一致する |
| 🟡 Minor-2 | mavenLocal の位置をハードコードしながら「Maven の設定に従う」と書いている | `verification/android/prepare-feed.sh:47-64` が `~/.m2/settings.xml` の `localRepository` を読み、無ければ `~/.m2/repository` を使う実装になり、コメントもその内容に一致 | **解消** (残る限界は Suggestion-3 に分離) |
| 🟡 Minor-3 | 署名任意化 (付随修正) がリリース経路側の担保を持たない | `deviation.md` 2 件目に「本 change の CI では踏まれず自動担保が無い」旨の補足、phase-7 agenda に phase-8 申し送り 1 行 | **解消** (推奨どおり申し送りで処理) |
| 🔵 Suggestion-1 | `--work` が Android のスクリプトで無視される | `verification/android/prepare-feed.sh:12-13` / `build-consumer.sh:14-15` に「効かない」旨を明記 | **対応** (ただし `--help` の usage は共通のまま → Suggestion-2) |
| 🔵 Suggestion-2 | R8 が走らないため app module を選んだ理由の一部が実現していない | `verification/android/app/build.gradle.kts:50-51` に、R8 が走らず consumer ProGuard ルールの不足は検出できない旨を明記 | **対応** |
| 🔵 Suggestion-3 | 依存ツリー取得の `\|\| true` が Gradle の失敗を握り潰す | `verification/android/build-consumer.sh:54-67` が出力を一時ファイルに取り、Gradle の終了コードと grep の結果を分けて判定する形になった。失敗時は部分出力を stderr に出す | **対応** |
| 🔵 Suggestion-4 | MAUI の開発用既定 version だけがハードコードで、コメントが宣言元を指す | `verification/maui/prepare-feed.sh:18` / `build-consumer.sh:21` / `VerificationApp.csproj:8-9` の 3 箇所とも「本体の開発用既定値と同じ値を持つ (宣言元から自動追随はしない)」に弱められた | **対応** (推奨の後者を採用) |
| 🟠 相方 Major | `smoke` + artifact で artifact が黙って無視される | `verification/lib/verification-args.sh:110-114` が `smoke` + `--reference` を拒否、3 workflow の `Validate inputs` が checkout / download-artifact より前に `smoke` + `artifact` を拒否。`deviation.md` 3 件目に合意として記録 | **解消**。3 platform × 2 スクリプトの 6 本すべて exit 1、workflow の run ブロックを抜き出した実行でも 3 本すべて exit 1 |
| 🟡 相方 Minor | mavenLocal の位置 | Minor-2 と同一 | **解消** |
| 🟡 相方 Minor | README lint が同じ見出し・言語の重複を黙って無視する | `scripts/readme-example-lint.py:96-123` が「一致はちょうど 1 件」を要求し、候補数を出して失敗する。`--selftest` に重複の負ケース 2 項目を追加 (計 12 項目) | **解消**。selftest 実行で「同じ見出し・言語の重複で exit 1」「重複した候補数が出力される」の 2 項目が OK |
| 🟡 相方 Minor | SNAPSHOT 時の Central 発行エラー案内が旧運用のまま | `android/kssettingsview/build.gradle.kts:174-176` が「リリース版の version は `-Pversion=<version>` で注入する (cross/ADR-0020)」に更新 | **解消**。`-Pversion=0.1.0-rc.1` で `:kssettingsview` の version が切り替わることも再確認 |
| 🔵 相方 Suggestion | `secrets: inherit` で呼ばれ得る | phase-7 agenda に phase-8 申し送り 1 行 | **対応** |

## 実行した検証

| 検証 | 結果 |
|---|---|
| `android/gradlew test` (本体。version 注入・署名条件化・メッセージ修正の回帰) | BUILD SUCCESSFUL / exit 0。**2700 tests / failures 0 / errors 0 / skipped 0** (`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` 集計) |
| SDK 解決関数の隔離実行 (`ksv_ensure_android_home`) | 環境変数なし + `android/local.properties` あり → exit 0 で `ANDROID_HOME` が実在ディレクトリに解決。`REPO_ROOT` を実在しない値にした場合 → exit 1 と自己説明的なメッセージ |
| Android 消費者ビルド (`--reference <準備済み mavenLocal>`、環境変数なし) | exit 0。`publishToMavenLocal` は実行されず (フィード準備の再実行なし)、`+--- jp.kamusoft:kssettingsview:0.1.0-SNAPSHOT` を出力 |
| 同上で `--reference` に空ディレクトリを指定 | BUILD FAILED / `Could not find jp.kamusoft:kssettingsview:0.1.0-SNAPSHOT`。探索先は与えた参照先のみで、mavenLocal にも Central にもフォールバックしない (warm cache 下でも成立) |
| `smoke` + `--reference` (3 platform × prepare-feed / build-consumer の 6 本) | すべて exit 1。出力はエラー 1 行のみで、引数解釈末尾の `platform=... mode=...` 行も出ない = フィード準備・依存解決に入っていない |
| 既存の負ケース (`--mode nope` / `--mode smoke` で version 省略) | いずれも exit 1。`--help` は exit 0 |
| 3 workflow の `Validate inputs` を抜き出した実行 (mode / version / artifact の 5 通り × 3) | smoke+artifact / 不正 mode / smoke の version 省略が exit 1、dry-run+artifact と素の dry-run が exit 0 |
| `python3 scripts/readme-example-lint.py` / `--selftest` | exit 0 / exit 0 (12 項目すべて OK) |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも exit 0 (comment-policy は 728 ファイル検査。**検出 0 件は適合の証明にしていない** — 規約本文からの読解も行った) |
| 全 shell スクリプトの `bash -n` (verification 配下 8 本) | すべて OK |
| 4 workflow の YAML ロードと job 名 | `ci.yml` = ios / android / maui / consumer-ios / consumer-android / consumer-maui / lint、呼ばれる側は 3 本とも `verify` |
| `git add -An verification` | 追跡対象は 34 ファイル (前回 33 + `android-sdk.sh`)。`build/` `.gradle/` `.kotlin/` は既存 `.gitignore` で除外済み |
| `providers.gradleProperty` が `ORG_GRADLE_PROJECT_*` を見るか (init script による実測) | 見る (env あり → `isPresent=true` / なし → `false`)。署名の要否ゲートは release CI の鍵渡し方 (`ORG_GRADLE_PROJECT_signingInMemoryKey`) で正しく働く |

iOS / MAUI の消費者ビルドは、1 周目以降の変更がコメントと引数解釈と workflow に限られ、引数解釈は上記の負ケース実行で直接確認したため再実行していない。

## 指摘事項

### [🔵 Suggestion] `lint.identity.allow` の `repo.local` は、永続ファイル側のコメントだけでは由来をたどれない

**該当箇所**: `kasane/config.yaml:78`、`deviation.md:6`

コメントは「`repo.local` は Maven のシステムプロパティ `maven.repo.local` の一部 (mDNS ホスト名ではない)」と述べるが、`maven.repo.local` を使う実装はリポジトリのどこにも無い (Minor-2 の修正で `~/.m2/settings.xml` を読む方式に変わったため)。現在この allow を必要としているのは、検査範囲 `kasane` に含まれるレビュー成果物 `kasane/changes/add-consumer-verification/second-opinion-code-001.md:52` に引用された `-Dmaven.repo.local=...` の 1 行だけである (allow を外して `find_identities` を通すと `('host', 'repo.local')` として検出されることを実測)。

由来は `deviation.md` の 4 件目に正確に記録されているため、判断の追跡はできる。ただし `deviation.md` は蒸留でアーカイブされ、残るのは永続ファイル `kasane/config.yaml` のコメントだけになる。この allow は `identity-lint` の mDNS ホスト名検出を `repo.local` についてリポジトリ全体で無効化するもの (`repo` という名前の端末名が `samples/` や `verification/` に書き戻されても検出されない) なので、なぜ緩めたのかが永続側だけで読めることに価値がある。

**推奨修正**: エントリ自体は現に必要 (外すと lint が赤くなる) なので残し、コメントを「レビュー証跡に引用された Maven のコマンドライン (`-Dmaven.repo.local=`) の一部」等、探しに行ける形に言い換える。蒸留の際に config を見るタイミングでよい。

---

### [🔵 Suggestion] `--help` の usage は Android で効かない `--work` をそのまま案内する

**該当箇所**: `verification/lib/verification-args.sh:49-58`、`verification/android/prepare-feed.sh:12-13`、`verification/android/build-consumer.sh:14-15`

前回 Suggestion-1 への対応はスクリプト冒頭のコメントで行われたが、利用者が実際に見る `--help` の出力は 3 platform 共通のままで、Android でも `--work <dir> 作業ディレクトリ` と案内する。ファイルを開かずに `--help` だけを見る人には前と同じ誤解が残る。`ksv_usage` を platform ごとに 1 行差し替えられるようにするか、Android の 2 本で `ksv_usage` を上書きするのが安い。

---

### [🔵 Suggestion] `localRepository` の `${user.home}` 形式を展開しないため、誤った理由で落ちうる

**該当箇所**: `verification/android/prepare-feed.sh:49-64`

`os.path.expandvars` はシェル形式の変数 (`$VAR` / `${VAR}` で環境変数にあるもの) しか展開しないため、Maven の設定でよく見る `<localRepository>${user.home}/.m2/repo</localRepository>` はリテラルのまま残る。この場合 `published` の存在確認が必ず失敗し、発行自体は成功しているのに「要求した version が発行されていません」という原因と食い違うメッセージで終わる。同様に、`-Dmaven.repo.local` システムプロパティとグローバル設定 (`<maven home>/conf/settings.xml`) も見ていない — Gradle の `mavenLocal()` はどちらも解釈するため、その環境では発行先と検査先がずれる。

いずれも loud に落ちるので実害は限定的だが、直すなら `${user.home}` (と `${env.VAR}`) の展開を足すか、存在確認の失敗メッセージに「解決した mavenLocal の位置」を含めて原因を追えるようにするのが安い。後者だけでも十分。

---

### [🔵 Suggestion] Android の `prepare-feed.sh` は smoke でも Android SDK の解決を先に要求する

**該当箇所**: `verification/android/prepare-feed.sh:31-41`

`ksv_ensure_android_home` が smoke の早期 return より前にあるため、「smoke では何も準備しない」と宣言している経路でも、SDK の場所が分からない環境では exit 1 になる。実際に消費者ビルドまで進めばどのみち SDK は要るので実害はほぼ無いが、責務としては SDK の解決は発行 (`publishToMavenLocal`) の直前で足りる。smoke 判定の後ろへ移すと、スクリプトの宣言と挙動が揃う。

## アクションプラン

1. **Suggestion 4 件** — 採否はオーナー判断。いずれも 1〜3 行で、Scenario の成立には影響しない。`kasane/config.yaml:78` のコメントだけは蒸留 (ksn-distill) で config を見るタイミングに合わせると自然
2. tasks 5.6 / 5.7 の CI 側 (draft PR での 7 job・artifact 入力の確認) と 6.1 (branch protection) はオーナーの作業として残る。Requirement「マージ保護」と Scenario「artifact を与えた呼び出し」は、それらが済むまで CI 上では未実証 (手元での `--reference` 経路は本レビューで実測済み)

## 確認したが指摘しなかったこと

- **`smoke` + artifact の早期拒否** — spec の Requirement「消費者検証 workflow の再利用契約」は artifact 入力を mode で条件づけていないが、`deviation.md` 3 件目にオーナー判断として記録済みの合意差分。蒸留で spec / concepts 側へ反映される前提で、違反としては扱っていない
- **付随修正 3 件 (version 注入の受け口・署名の条件化・`lint.identity.allow` の追加)** — いずれも `deviation.md` に記録済み。同梱条件④ (自動担保) の欠落は前回 Minor-3 で挙げ、申し送りとして処理された。allow の追加は lint を通すための最小の緩和で、条件内の付随修正と判断した (残る指摘はコメントの言い回しのみ)
- **署名ゲートが release CI で働くか** — 「鍵が `ORG_GRADLE_PROJECT_signingInMemoryKey` (環境変数) で渡ると `providers.gradleProperty` が見落として未署名で通ってしまう」経路を疑い、init script で実測した。環境変数経由でも `isPresent` は true になるため、この穴は無い
- **`tasks.md` の虚偽チェック** — 未完了の 5.6 / 5.7 / 6.1 は未チェックのまま。1.1〜5.5 は evidence の対応節と突き合わせて、実行の記録があることを確認した
- **足場アーティファクトの書き換え** — `proposal.md` / `design.md` / `specs/` に変更なし。`tasks.md` はチェックのみ、agenda は append のみ
- **`verification/` の build 成果物** — `.gitignore` で除外され、追跡対象は 34 ファイル。バイナリの新規持ち込みは wrapper のみ (前回 shasum 一致を確認済み)
- **`android-sdk.sh` のコメントにある「開発環境の手順」への言及** — 文書名・パスの参照ではなく、2 経路の内容をコメント内で自己完結して説明しているため comment-policy の禁止参照には当たらないと判断した
