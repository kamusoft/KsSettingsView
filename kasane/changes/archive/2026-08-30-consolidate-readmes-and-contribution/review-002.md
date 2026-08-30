# レビュー結果: consolidate-readmes-and-contribution (002 回目)

**日付**: 2026-08-29
**判定**: CHANGES_REQUESTED

## サマリー

`second-opinion-code-001.md` の突き合わせ表が採用と決めた 13 件のうち **11 件は現物で解消を確認**した。復元したビルド / lint コマンドは実ビルド構成と実測で一致し (`./gradlew lint --dry-run` が 4 module すべてで lint タスクを解決、`dotnet sln KsSettingsView.slnx list` が 8 project を列挙)、MAUI テストの移送先で謳う実測値も `dotnet test` の再実行で 516 件 / 0 失敗 / 338 ms として再現できた。docs-refresh Step 3c の検査スクリプトを実際に走らせたところ、本変更由来の 3 本は `UNCOVERED` から消えている。1 周目で APPROVED だった範囲 (英日見出し 17 個の階層一致・最小コード例 3 platform × en/ja の逐語一致・内部リンク全解決・README 集合 5 枚・画像 SHA 一致・lint 2 本 exit 0) はいずれも壊れていない。

一方で、**双方のレビューが Major として確定させた 13 件中 1 件 (#2) が手つかずのまま残っている** — `README_ja.md` の貢献導線が今も英語 CONTRIBUTING を指す。同ファイルの直上 (:137) には別の採用項目 (`.AddKsSettingsView()` の案内) が入っているので、編集自体は届いていながらこの 1 行だけ落ちた形。加えて #3 (accepted ADR からの参照切れ) は concepts 側の導線だけが張られ、`deviation.md` への記録と supersede 判断の蒸留への申し送りが欠けている。

修正で持ち込んだ新規の劣化は軽微なもの 3 件のみで、いずれも局所修正で閉じる。

## (a) 採用 13 件の解消可否

| # | 指摘 | 判定 | 根拠 |
|---|---|---|---|
| 1 | `test-execution.md` の削除済み README 前提 | **解消** | `kasane/concepts/cross/conventions/test-execution.md:60-62` が「`swift test` を案内している文書は無い」へ書き換わり、cross/ADR-0023 を根拠に明示。`grep -rn "swift test"` で live 文書に残存 0 件 (`.claude/worktrees/` のみ)。timestamp 2026-08-29 |
| 2 | `README_ja.md` の貢献導線 | **未解消** | `README_ja.md:162` が依然 `.github/CONTRIBUTING.md`。下の 🔴 参照 |
| 3 | maui/ADR-0006 → 移送先の導線 | **部分解消** | 導線 (推奨修正 1) は着地 — `kasane/concepts/maui/architecture/binding-build-integration.md:92` が「maui/ADR-0006 が『再検証の入口』として対で維持すると定めた表」と明記し、`:115` の「関連」に ADR を追加。推奨修正 2 (deviation への記録 + supersede 要否を蒸留へ) は未実施。下の 🟡 参照 |
| 4 | 失われたビルド / lint コマンド | **解消** | `local-development-setup.md:156-195` に「本体をビルドする」節を新設。実構成と照合: `ios/Package.swift` 実在、`android/settings.gradle.kts` の 4 module 名 (`:ks-settingsview-{core,ui,compose,bridge}`) と一致、`cd android && ./gradlew lint --dry-run` が 4 module の `lint` を解決 (BUILD SUCCESSFUL)、`cd maui && dotnet sln KsSettingsView.slnx list` が成功。テストは `test-execution.md` が正である旨を :158 で明示し重複なし |
| 5 | 生成型 namespace と `Metadata.xml` | **解消** | `binding-build-integration.md:86-88`。`maui/android/KsSettingsView.Binding.Android/Transforms/Metadata.xml:9-10` の `managedName` = `KsSettingsView.Bridge`、既定変換 `Jp.Kamusoft.Kssettingsview.Bridge` と逐一一致。`public-identifiers.md` への相対リンクも解決 |
| 6 | `log.md` の append-only 逸脱 | **解消** | `git diff` 上、`kasane/concepts/log.md` の変更は末尾への純粋な追記のみ (既存行の移動なし)。新規 3 本が `created:` で 1 本ずつ記録され、移送・追随分が `updated:` 1 行 |
| 7 | MAUI テストコマンドの置き場 | **解消** | `test-execution.md:64-78` に `## MAUI` 節を新設。`local-development-setup.md:211` はコマンドを持たず参照へ置換され、「本書でのみ案内する」の 1 文も消えている。`test-execution.md:15` は 3 platform 記載へ更新。**実測再現**: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → 失敗 0 / 合格 516 / 338 ms。:73 が示す出力形式 (`失敗: N、合格: M、スキップ: K、合計: T`) も実出力と一致 |
| 8 | index の 1 行説明 / frontmatter description | **解消** | `kasane/concepts/cross/index.md:19` と `kasane/concepts/core/index.md:32` に主題を追記、両 concept の `description` も更新。`cross/index.md:17` に新規 `local-development-setup.md` の 1 行も追加済み |
| 9 | manifest 未登録による `UNCOVERED` | **解消** | `skills/.manifest.json` の `excluded` に 3 本を理由つきで追加。Step 3c のスクリプトを実行 → 本変更由来の 3 本は消え、残るのは `cross/conventions/user-skill-api-listing.md` 1 本 (先行 change 由来。下の 🔵 参照) |
| 10 | tasks 8.4 の Requirement 対応 | **解消** | `tasks.md:63` が「`identity-lint.py` は `lint.identity.scope` により README / `.github/` を検査しない」を併記し、`→ Requirement:` から「スクリーンショットの提示」を除去 |
| 11 | MAUI 例の `.AddKsSettingsView()` | **解消** | `README.md:137` / `README_ja.md:137`。コードブロック外の 1 文なので Requirement に抵触せず、3 platform の逐語一致も維持 (再検証済み) |
| 12 | Simulator / Emulator の boot 手順 | **解消** | `local-development-setup.md:123-128` に `xcrun simctl boot` + `open -a Simulator` と「`booted` 指定は 2 台以上で宛先が定まらない」の注記、`:142-147` に `emulator -list-avds` と AVD 起動 |
| 13 | リポジトリ構成表の `openspec/` | **解消** | `README.md:154` / `README_ja.md:154` に 1 行追加。凍結資産であり現行仕様ではない旨も入っている |

**解消 11 / 部分解消 1 / 未解消 1。**

## 指摘事項

### [🔴 Critical] 確定 Major #2 が未修正 — 日本語 README の貢献導線が今も英語 CONTRIBUTING を指す

**該当箇所**: `README_ja.md:162`

**問題点**:
```
Issueを投稿する前に[貢献ガイドライン](.github/CONTRIBUTING.md)を確認してください。
```

両レビューが一致して確定させ (`review-001.md` Major 2 / `second-opinion-code-001.md` Minor C、突き合わせ表 #2 で「確定 (Major)」)、両方のアクションプランが 1 行の修正として挙げた項目が、そのまま残っている。`.github/CONTRIBUTING_ja.md` は実在し、`.github/CONTRIBUTING.md:3` から相互リンクもある。同じファイルの他の言語別リンクは日本語側を指しており (`README_ja.md:137` は `skills/ja/kssettingsview-maui/SKILL.md`、`:141` は `skills/README_ja.md`)、この 1 行だけが例外である点も 1 周目から変わっていない。

重要度を Critical に上げた理由は指摘の内容ではなく**修正周回の実効性**にある。この行の 25 行上 (`:137`) には同じ周回で採用した別項目 (`.AddKsSettingsView()` の案内) が入っており、ファイルは確かに編集されている。つまり「確定 Major を 1 件落としたまま 2 周目に出した」状態であり、他の採用項目の反映も同じ検査を通っていないことを疑わせる。実際、下の 🟡 で挙げるとおり #3 の後半も落ちている。

**推奨修正**: `README_ja.md:162` のリンク先を `.github/CONTRIBUTING_ja.md` へ変更する。あわせて、確定した 13 件を 1 件ずつ現物で突き合わせる (指摘の該当行を開いて確認する) チェックを完了報告の前に入れる。

---

### [🟡 Minor] accepted な maui/ADR-0006 の参照切れが deviation に記録されず、supersede 判断が誰にも渡っていない

**該当箇所**: `deviation.md` (記録の不在) / `kasane/decisions/maui/0006-android-binding-gradlew-exec.md:23`

**問題点**:
突き合わせ表 #3 が採用した推奨修正は「1 (concept 側に導線を張る) + 2 (deviation に記録し supersede の要否を蒸留へ送る)」の併用だった。1 は着地している (`binding-build-integration.md:92` / `:115`)。しかし 2 は行われておらず、`deviation.md` を `0006` で grep しても該当なし。

ADR-0006:23 は今も「再検証の入口は `maui/README.md` の『SDK 更新時に再検証する箇所』の表と対で維持する」と書いており、この参照先は本変更で削除されている。ADR は accepted 後に本文を編集しない (ksn-core references/decisions.md) ため、正しい処理は「切れた事実を記録して supersede の要否を蒸留 (ksn-distill) に判断させる」ことだが、その入口が塞がったままになる。

実害: 蒸留時に ADR-0006 の supersede が検討されず、accepted な ADR が存在しないファイルを指し続ける。ADR → 表の向きは辿れないままで、`.NET` workload を上げた開発者が ADR-0006 から再検証手順に到達できない状態が固定される (second-opinion Major B の実害シナリオがそのまま残る)。

**推奨修正**: `deviation.md` に「accepted な maui/ADR-0006:23 の参照先 (`maui/README.md`) が本変更で消滅した。移送先は `kasane/concepts/maui/architecture/binding-build-integration.md` の『SDK 更新時に再検証する箇所』節で、表 → ADR の導線は張った。ADR 本文は不変のため、supersede の要否は蒸留で判断する」を追記する。

---

### [🟡 Minor] `local-development-setup.md` の冒頭宣言が、新設した「本体をビルドする」節を含んでいない

**該当箇所**: `kasane/concepts/cross/conventions/local-development-setup.md:11`

**問題点**:
ksn-core references/concepts.md の可読性規約は「h1 の直後は『この文書を読むと何が分かるか』の宣言から始める」ことを求める。現行の宣言は次のとおり。

> この文書は、リポジトリを clone した開発者が iOS・Android・MAUI の Sample を開いて実行し、本体 source へデバッガでステップインするまでの手順をまとめる。読むと、Android SDK を二つの Gradle build root から解決する理由と、複数の Xcode を使う環境で選択を固定する方法も分かる。

本変更 (2 周目) が新設した「本体をビルドする」(`:156-195`) は、Sample の実行でもステップインでもない第 3 の主題であり、宣言のどこにも現れない。frontmatter の `description` と `cross/index.md:17` の 1 行説明には「本体モジュールのビルド / lint コマンド」が入っているので、**文書の入口 3 つのうち本文だけが追随していない**。

実害: 中程度。`./gradlew lint` を探す読者は index 行から本書に辿り着けるが、本書を開いて冒頭を読んだ時点で「Sample を動かす文書」と判断して離脱し得る。1 周目の Minor 5 (index / description の未追随) と同型の漏れが、今度は本文側で起きている。

**推奨修正**: 冒頭宣言に本体ビルド / lint を 1 句加える (例: 「…Sample を開いて実行し、本体モジュールをビルド / lint し、本体 source へデバッガでステップインするまでの手順をまとめる」)。

---

### [🔵 Suggestion] `test-execution.md` の節順が、冒頭と frontmatter の列挙順と食い違う

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md:15` / `:17` / `:64` / `:80`

**問題点**: 本文は `## iOS` (:17) → `## MAUI` (:64) → `## Android` (:80) の順で並ぶが、frontmatter の `description` と `:15` の宣言はどちらも「iOS / Android / MAUI」と書いている。新設した MAUI 節が iOS と Android の間に挿入されたためで、内容の誤りではない。

実害: 小。ただし本書は「どのコマンドで何が実行されるか」を引く索引として使われ、`## Android` を探す読者が宣言の順序どおりに 2 番目を見ると MAUI に当たる。

**推奨修正**: MAUI 節を Android 節の後ろへ移すか、`:15` と `description` の列挙を「iOS / MAUI / Android」へ揃える。前者が自然 (Android 節が最も長く、末尾に置くほうが読み流しやすい)。

---

### [🔵 Suggestion] log.md の `created:` 3 件に初見可読性レビューの実施が記録されていない

**該当箇所**: `kasane/concepts/log.md` の 2026-08-29 節 (`created:` 3 行)

**問題点**: `deviation.md`「初見可読性レビューの実施と反映」が、3 本について独立文脈のレビューを実施し Major 4 件すべてを反映したことを詳細に記録しており、**ゲート自体は確実に通っている** (`integration-host-verification.md` の MauiHost iOS 手順が「書かれたとおりでは動かない」状態から修正された経緯は、実際に csproj と突き合わせて着地を確認した — `TargetFrameworks` が 2 つのため `-f net10.0-ios` が必須、`AssemblyName` = `KsSettingsView.MauiHost` で `.app` 名が一致、`ApplicationId` = `jp.kamusoft.kssettingsview.mauihost`、出力 RID `iossimulator-arm64` も実在)。

問題は記録の所在だけである。直前の先行事例 `created: cross/conventions/user-skill-api-listing.md` は「(2026-08-29 オーナー合意、初見可読性レビュー実施済み)」を log 本文に持つのに対し、今回の 3 行は持たない。`deviation.md` は change と一緒に archive されるため、長命層 (log.md) だけを見た将来の読者からはゲート通過が見えなくなる。

**推奨修正**: 3 行それぞれの末尾に「初見可読性レビュー実施済み」を添える (1 行あたり数語)。

---

### [🔵 Suggestion] docs-refresh Step 3c は依然として検査失敗で終わる (残 1 本)

**該当箇所**: `skills/.manifest.json` の `excluded`

**問題点**: Step 3c を実行すると本変更由来の 3 本は消えたが、`cross/conventions/user-skill-api-listing.md` 1 本が `UNCOVERED` として残り、スクリプトは `concepts coverage OK` を返さない。

```
UNCOVERED:
  cross/conventions/user-skill-api-listing.md
```

これは `review-001.md` Minor 8 自身が「先行 change 由来で本変更の責ではない」と切り分けたものなので、**指摘ではなく観測**として置く。ただし採用項目 #9 の目的は「次回の docs-refresh 通常実行で Step 3c が失敗しない状態にする」ことであり、その状態には到達していない。当該 concept は同じ `excluded` ブロックに 1 行足すだけの隣接課題で、内容も「利用者向け Skill への掲載基準」という開発側の規約であり、既存の除外 3 本と同じクラスに見える。

なお SKILL.md Step 3c は「配置判断はユーザーに提示し、スキルが独断で決めない」と定めているため、実装側が勝手に書き込むのは規律違反になる。

**推奨修正**: オーナーに 1 行の配置判断を諮って同梱するか、`deviation.md` に「次回 docs-refresh 通常実行時に配置判断が必要な concept 1 本」として明示的に申し送る。どちらも取らないなら、次回実行時にオーナーが理由を思い出せない状態で判断を求められる。

---

## (c) 未対応として申し送った 2 件の妥当性

**判断そのものは妥当。ただし申し送りが実体として存在しない** — ここは是正が要る。

### #14 英語 README に日本語 UI のスクリーンショット

見送り妥当。修正には Sample アプリへの英語リソース追加 (= `samples/` のコード変更) が要り、本変更の Non-Goals と spec の範囲を明確に超える。`ui/brief.md` の「申し送り」欄にオーナー承認と理由が記録されており、spec Requirement「スクリーンショットの提示」は言語を規定していないため仕様違反でもない。

ただし second-opinion Suggestion I が挙げた選択肢 (b)「英語 README のキャプションに『screenshots from the Japanese-locale sample app』を添える」は本変更内で 2 行の作業であり、これも見送った判断の理由がどこにも書かれていない。採否の記録は残すべき。

### #15 blank issue 無効 + `contact_links: []` で質問窓口が無い

見送りの結論は妥当だが、**理由づけは一部正しくない**。`contact_links` は `.github/ISSUE_TEMPLATE/config.yml` という本変更の成果物内のフィールドであり、「リポジトリ設定だから phase-2 の領分」とは言い切れない (Discussions を有効化せずとも、CONTRIBUTING や既存チャネルへの `contact_links` を 1 本置くことは本変更内で可能)。それでも見送りは支持する — spec Requirement「Issue テンプレートの必須項目」は `blank_issues_enabled: false` を要求するのみで窓口の追加を求めておらず、cross/ADR-0024 の意図 (AI スロップ抑止) は現状で満たされているため、窓口の設計は決定を伴う別議論が適切。

### 共通の問題: 申し送り先が存在しない

2 件とも `deviation.md` にも `kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/` の agenda / artifacts にも記録がない。レビュー文書は change と一緒に archive されるため、このままでは蒸留時に拾われない限り消える。

さらに #15 は**既存の決定と正面から衝突している**: `kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/publish-procedure.md:35` は「Discussions OFF」を実施手順として既に確定済みで、agenda.md:26 の public 化手順も同じ。second-opinion が推奨した解 (Discussions を有効にして `contact_links` に 1 本足す) は、申し送り先が既に反対方向に決めている論点である。何も書かずに送ると、phase-2 では「決定済み」として素通りする。

**推奨修正**: `deviation.md` の申し送り節に 2 件を書く。#15 は「phase-2 は Discussions OFF を確定済みであり、質問窓口を置くならその決定の再検討が要る」ことまで書いて渡す。

## (b) その他、修正が新たな問題を持ち込んでいないかの確認結果

指摘に至らなかった検査:

- **1 周目 APPROVED 範囲の非破壊**: 英日 README の見出し 17 個 — 階層・並び・番号すべて一致 (`## Overview and key features` ↔ `## 概要と主な特徴` 等)。最小コード例は swift / kotlin / xml の 3 ブロックが `skills/{en,ja}/kssettingsview-{ios,android,maui}/SKILL.md` の対応ブロックと逐語一致 (英日 README × en/ja Skill の 6 組すべて確認)。`.AddKsSettingsView()` の追記はコードブロック外なので一致に影響しない
- **リンク解決**: `kasane/concepts/**`・`kasane/decisions/**`・`skills/**`・`.github/**`・ルート README 2 枚・`AGENTS.md`・`CLAUDE.md`・docs-refresh SKILL.md の計 187 ファイルの相対リンクを解決 → 破断 0。新規に張られたリンク (`binding-build-integration.md` → maui/ADR-0006 / `public-identifiers.md`、`test-execution.md` → cross/ADR-0023 / `integration-host-verification.md`、`local-development-setup.md` ⇔ `test-execution.md`、`runtime-behavior-verification.md` → `local-development-setup.md`) はすべて解決
- **README 集合**: 公開ドキュメント面に `README*.md` は 5 枚 (ルート 2・`skills/` 2・`maui/spike/` 1)。削除 5 枚への言及は live 文書では docs-refresh SKILL.md:182 の廃止理由注記のみ (deviation 記録済みの意図的残置)
- **画像**: `assets/` 4 枚と `ui/references/` 4 枚の SHA-256 が一致。`ui/brief.md` の承認欄・撮影条件・申し送りが記入済み
- **lint**: `scripts/local-path-lint.py` / `scripts/identity-lint.py` ともに exit 0
- **新規 concept の事実照合 (2 周目で触った箇所)**: `binding-build-integration.md:86-88` ↔ `Metadata.xml:9-10`、`:92` の ADR 対応、`integration-host-verification.md:84-96` の MauiHost iOS 手順 ↔ csproj の `TargetFrameworks` / `AssemblyName` / `ApplicationId` と実在する出力 RID、`runtime-behavior-verification.md` のチェックリスト ↔ `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` (`person.crop.circle` / `externaldrive` / `Tanaka Taro` / `Storage` / RadioCell footer / Section 7 種の順序) — すべて一致
- **足場の凍結**: `proposal.md` / `design.md` / `specs/` に diff なし。`tasks.md` はチェックボックスと 8.4 の記述 (レビュー推奨に基づく Requirement 対応の是正) のみ。ksn-core の凍結対象は proposal / design / specs であり tasks.md は含まれないため違反としない
- **付随修正**: `performance-verification.md` の参照差し替え 2 箇所、`public-identifiers.md` の H1 追加 — いずれも ksn-core の同梱条件内 (1 周目の判定を維持)
- **テスト**: ソース・ビルドファイルへの変更なし。`dotnet test` を独自に実行し 516 件 / 0 失敗を確認 (concept の記述の裏取りとして)

## アクションプラン

1. **🔴 Critical** — `README_ja.md:162` を `.github/CONTRIBUTING_ja.md` へ。あわせて確定 13 件の該当行を 1 件ずつ開いて突き合わせ直す
2. **🟡 Minor** — `deviation.md` に maui/ADR-0006:23 の参照切れと supersede 判断の蒸留送りを記録
3. **🟡 Minor** — `local-development-setup.md:11` の冒頭宣言に本体ビルド / lint を加える
4. **🔵** — `deviation.md` の申し送り節に (c) の 2 件を記録 (#15 は phase-2 の Discussions OFF 決定との衝突まで書く)。#14 は選択肢 (b) を採らなかった理由も添える
5. **🔵** — `test-execution.md` の節順または列挙順を揃える
6. **🔵** — `log.md` の `created:` 3 行に初見可読性レビュー実施済みを追記
7. **🔵** — `user-skill-api-listing.md` の manifest 配置判断をオーナーに諮るか deviation へ申し送る
