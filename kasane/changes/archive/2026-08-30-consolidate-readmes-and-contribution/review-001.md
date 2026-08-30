# レビュー結果: consolidate-readmes-and-contribution (001 回目)

**日付**: 2026-08-29
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペック 2 能力の Requirement / Scenario は、機械的に検査できる範囲ではほぼすべて満たしている (公開ドキュメント面の README は 5 枚ちょうど、英日の見出し階層一致、最小コード例と各 SKILL.md のブロック一致、配布座標の文書間一致と `KsSettingsView-Swift` の全消滅、Issue Forms の必須項目、リンクの全解決、lint 2 本 exit 0)。新規 concept 3 本は移送元の README より粒度も接地も良く、`integration-host-verification.md` と `binding-build-integration.md` は実ソース (`_RegisterXcodeProjectNativeReference` / `_AdjustKsBridgeXcodeProjectInputs` / `KsBridgeScenario.cs` / MauiHost のボタン文言) と照合して正確だった。docs-refresh の旧指示除去も、deviation が意図的残置と説明した 2 箇所を除き漏れがない。

一方で、**廃止した README ではなく「全面置換したルート README」の側で参照整合が 1 箇所壊れており** (Major 1)、**日本語 README の貢献導線が英語版へ向いている** (Major 2)。加えて、A 判定 (破棄) の根拠が本変更自身で無効化された箇所、移送漏れ 1 件、index / 1 行説明の追随漏れ、log.md の append-only 逸脱、docs-refresh の網羅検査を新 concept 3 本が落とす件がある。いずれも局所修正で閉じる。

## 指摘事項

### [🟠 Major] test-execution.md が、本変更で消えたルート README の記述を前提に語り続けている

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md:60-62`

**問題点**:
当該節は次の前提で書かれている。

> `README.md` はパッケージを手早く確認する手順として `swift test` を案内している。あれは利用者向けの動作確認であり、**本規約の完了判定とは別物**である。

この `swift test` は旧ルート README の「ビルド方法 > iOS」節 (`git show HEAD:README.md:84`) にあった記述で、本変更のルート README 全面置換によって消えている。現行のルート README・`README_ja.md`・`skills/` のいずれにも `swift test` の案内は無い。つまり長命層 (L2) に**事実として誤った文が残った**。

実害: この規約を読んだ実装者 / エージェントが「README のどこに `swift test` があるのか」を探して見つけられず、規約全体の信頼度を落とす。さらに悪いのは逆向きで、「README にある案内との切り分け」という節の存在自体が、ルート README に開発者向けビルド手順があるという誤った像を与える — これは ADR-0023 (README は利用者の入口に純化) が消したはずの像そのもの。`kasane/concepts/log.md:318` を見ると、この節は 2026-08-26 の docs 廃止時にも同じ理由で一度書き換えられており、同じ劣化が再発している。

tasks 4.2 / 8.3 は「廃止した README への Markdown リンク」を検査対象にしていたため、**残存したファイルの、削除された節を指す散文参照**という型がすり抜けた。

**推奨修正**: 当該節 (h3「README の `swift test` との関係」) を削除するか、「`swift test` を案内する文書はリポジトリに存在しない (旧ルート README の案内は cross/ADR-0023 で廃止済み)。それでも `swift test` を完了判定に使わない理由は上記のとおり」の形へ書き換え、timestamp を更新する。あわせて、削除ではなく**全面置換した文書**についても同種の散文参照が無いか、`kasane/concepts/` を `README` で grep して確認する (現状はこの 1 件のみ)。

---

### [🟠 Major] 日本語 README の貢献導線が英語 CONTRIBUTING を指している

**該当箇所**: `README_ja.md:159`

**問題点**:
```
Issueを投稿する前に[貢献ガイドライン](.github/CONTRIBUTING.md)を確認してください。
```
`.github/CONTRIBUTING_ja.md` は本変更で同時に作成されている (tasks 5.5) にもかかわらず、日本語 README は英語版を指している。同じファイル内の他の言語別リンクはすべて日本語側を指しており (`skills/README_ja.md:38`、`skills/ja/kssettingsview-*/SKILL.md:139`)、ここだけが例外なので単純な取りこぼしと読める。

実害: 「PR を受け付けない」という、最も誤解されると摩擦を生む方針を、日本語話者がまず英語で読むことになる。CONTRIBUTING.md 冒頭の `[日本語]` リンクを踏めば 2 ホップで到達できるが、本変更の目的が「貢献者向け入口の整備」である以上、入口で 1 ホップ余計に踏ませるのは成果物の欠陥。

なお spec の Requirement「貢献方針の表明」は英日 CONTRIBUTING の存在を求めるだけでリンク先の言語までは規定していないため、これは仕様違反ではなく品質指摘。

**推奨修正**: `README_ja.md:159` のリンク先を `.github/CONTRIBUTING_ja.md` へ変更する。英語 README (`README.md:159`) は現状のままでよい。

---

### [🟡 Minor] A 判定 (破棄) の根拠が本変更自身で無効化されている — ビルド / lint コマンドの行き先が無い

**該当箇所**: `design.md` Decision 1 の移送対応表 (`android/README.md`「ビルド・テスト」行)、`tasks.md:1.11`

**問題点**:
対応表は `android/README.md`「ビルド・テスト」を **A (移送しない)** とし、その理由を「`cross/conventions/test-execution.md` と**ルート README** に既出」としている。しかしルート README の「ビルド方法」節は本変更で削除された。`test-execution.md` が持っているのは `./gradlew test` と iOS の `xcodebuild test` だけで、次は現在どの concept にも存在しない (grep で確認済み):

| 失われたもの | 旧出典 |
|---|---|
| `./gradlew build` (全モジュールビルド) | `android/README.md`、旧ルート README |
| `./gradlew lint` | `android/README.md` |
| `./gradlew :ks-settingsview-*:assembleDebug` (個別モジュール) | `android/README.md` |
| `swift build` / `swift package describe` | 旧ルート README |
| `dotnet build maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` (facade 単体ビルド) | 旧ルート README |

`local-development-setup.md` が持つ Gradle コマンドは Sample 側 (`:app:assembleDebug` / `installDebug`) だけで、本体モジュールのビルドには触れていない。

tasks 1.11 は「A 分類は実在確認してから破棄する」を要求しているが、実在確認の対象が**同じ変更で削除される予定のルート README**であったため、確認は形式的に成立して実質的には成立していない。deviation の「グループ1 実施時の差分」で MAUI の `dotnet test` について同じ罠に気づいて C へ倒しているので、判定基準自体は正しく運用されている — この 1 行だけ漏れた形。

実害: 中程度。`./gradlew build` は標準的で再導出は容易だが、`./gradlew lint` の存在と本体側の個別モジュールビルドは「知らないと引けない」。

**推奨修正**: `local-development-setup.md` に「本体モジュールをビルド / lint する」節を 1 つ足し、`android/` ルートの `./gradlew build` / `./gradlew lint`、`ios/` の `swift build`、facade の `dotnet build` を置く。テストコマンドは `test-execution.md` の責務なので重複させず、参照でつなぐ。design.md は凍結対象なので書き換えず、deviation.md に「A→C の再判定 1 件」として追記する。

---

### [🟡 Minor] 移送漏れ: binding の .NET namespace 正規化

**該当箇所**: `kasane/concepts/maui/architecture/binding-build-integration.md` (移送元: `git show HEAD:maui/README.md`「binding 層」)

**問題点**:
移送元にあった次の 2 文が、新 concept にも他のどこにも入っていない (`kasane/concepts/` 全体を `KsSettingsView.Bridge` で grep して不在を確認)。

> 生成される .NET の型はどちらの platform でも `KsSettingsView.Bridge` namespace に入る。Android 側は Java パッケージ名からの既定変換を `Transforms/Metadata.xml` で上書きしている。

design.md Decision 1 の対応表はこの節 (「binding 層」) を `binding-build-integration.md` へ移すと定めており、新 concept は `Transforms/Metadata.xml` に `BG8A00` 警告の文脈でしか触れていない。

実際に `maui/android/KsSettingsView.Binding.Android/Transforms/Metadata.xml:5-10` が `managedName` で `Jp.Kamusoft.Kssettingsview.Bridge` → `KsSettingsView.Bridge` へ上書きしている。実害: `Metadata.xml` を触る開発者が、この `managedName` 上書きを「不要な設定」と判断して外すと、生成型の namespace が Java パッケージ由来へ戻り、`cross/conventions/public-identifiers.md` の .NET namespace 規約と iOS binding との対称性が同時に壊れる。緩和要因として、当の XML 自身が冒頭コメントで理由を書いているため完全な喪失ではない。

**推奨修正**: `binding-build-integration.md` の「Native artifact の生成」または「SDK 標準アイテムの採否 > Android」に 1〜2 文で追記する — 生成型は両 platform とも `KsSettingsView.Bridge` に入ること、Android は `Transforms/Metadata.xml` の `managedName` で既定変換を上書きしていること、その根拠が `public-identifiers.md` の namespace 規約と iOS との対称性であること。

---

### [🟡 Minor] 追記した節が index の 1 行説明 / frontmatter description に反映されていない (2 ファイル)

**該当箇所**:
- `kasane/concepts/cross/conventions/runtime-behavior-verification.md:34-45` (追記) / `kasane/concepts/cross/index.md:19` (未更新) / 同ファイル frontmatter `description` (未更新)
- `kasane/concepts/core/styling/style-resolution.md:64-68` (追記) / `kasane/concepts/core/index.md:32` (未更新) / 同ファイル frontmatter `description` (未更新)

**問題点**:
`runtime-behavior-verification.md` の index 行は「実行時挙動が絡む不具合修正の完了判定 (実環境での再現確立と修正後の解消確認)」のままで、追記された「iOS Basic Cell Sample の目視確認」チェックリストの存在が読み取れない。ksn-core `references/concepts.md` は conventions カテゴリの 1 行説明について「適用範囲が読み取れる書き方にする — ワーカーは index 行だけでロード要否を判定するため、対象範囲の見えない説明は規約を素通りさせる」と定めている。実害: iOS Sample の見た目を触るワーカーが index を見てこの規約をロードせず、チェックリストが使われないまま完了報告される (この規約自身が防ごうとしている失敗型と同じ)。`style-resolution.md` の「Sample の AiForms 互換色」も同様。

副次的に、追記されたチェックリストは内容がすべて iOS 固有 (`samples/ios/.../BasicCellsDemoView.swift`、SF Symbols 名、52pt / 16pt の inset) である一方、置き場は `cross/`。`kasane/concepts/rules.md` のドメイン導出規則は「単一 platform のビルドルートに閉じる知識 → その platform ドメイン」なので、厳密には `ios/` へ寄せる余地がある。ただし design.md Decision 1 が「実機目視確認は既存の `runtime-behavior-verification.md` へ統合」と確定済みなので、**この配置自体は指摘ではなく申し送り**とする (spec / design は凍結)。

**推奨修正**: 2 ファイルの frontmatter `description` と、`cross/index.md:19` / `core/index.md:32` の 1 行説明を、追記した主題が読み取れる形へ更新する (例: 「…完了判定と、iOS Basic Cell Sample の目視確認項目」)。ドメイン配置の再検討は蒸留時の論点として申し送る。

---

### [🟡 Minor] log.md の追記が append-only を崩し、新規 concept 3 本が `created:` として記録されていない

**該当箇所**: `kasane/concepts/log.md:328-336`

**問題点**: 3 点ある。

1. **既存エントリの所属セクションが変わった**。新設した `## 2026-08-29` 見出しが、既存行 `- distilled: skills-api-coverage` の**直前**に挿入されたため、この行は `## 2026-08-28` から `## 2026-08-29` へ移動した。同じ change の対になる行 `- created: cross/conventions/user-skill-api-listing.md` は `## 2026-08-28` に残っており、1 つの change の記録が 2 つの日付セクションに割れている。log.md は append-only の履歴であり、既存行の所属を変える編集はしない。
2. **本変更の 2 行目が、無関係な既存行の後ろに置かれている**。`- updated: performance-verification.md` (:336) が `- distilled: skills-api-coverage` (:335) の後ろにあり、本変更の 2 エントリが他 change のエントリを挟む形になっている。
3. **新規作成 3 本が `updated:` 1 行に埋もれている**。`binding-build-integration.md` / `integration-host-verification.md` / `local-development-setup.md` は新規ファイルだが、log の記録は `- updated: consolidate-readmes-and-contribution グループ1 — …へ移送し…` の 1 行。log.md の他の全事例 (`created: maui/conventions/performance-verification.md`、`created: cross/conventions/user-skill-api-listing.md` 等) は新規概念を `created:` で 1 本ずつ、内容要約・index 更新・timestamp・オーナー合意・初見可読性レビューの実施まで書いている。ここだけ形式が崩れており、後から「いつ何が生まれたか」を引けない。

あわせて、ksn-core `references/concepts.md` は**新規概念ファイルの作成を確定する前に初見可読性レビュー (概念本文だけをレビュアーに渡す)** を経路共通のゲートとして要求しているが、3 本について実施記録が log にも deviation にも無い。本レビューは変更文脈を全部読んでいるため、このゲートの代替にはならない。

**推奨修正**:
- `## 2026-08-29` 見出しを `- distilled: skills-api-coverage` の**後ろ**へ移し、既存行の所属を元に戻す。本変更の 2 エントリはセクション末尾に並べる。
- 新規 3 本を `created:` エントリとして 1 本ずつ書き直す (概念の内容要約 / index 更新 / timestamp)。移送・追随した既存 4 ファイルは `updated:` にまとめてよい。
- 3 本の初見可読性レビューを ksn-reviewer へ委譲して実施する (渡すのは概念本文のみ。ソース・spec・変更文脈は渡さない)。実施済みなら log にその旨を記す。

---

### [🟡 Minor] 新規 concept 3 本が docs-refresh の網羅検査を落とす (manifest 未登録)

**該当箇所**: `skills/.manifest.json` (`excluded` / `targets`)

**問題点**: `.agents/skills/docs-refresh/SKILL.md` Step 3c の検査スクリプトをそのまま実行すると、本変更で作られた 3 本が `UNCOVERED` として報告される。

```
UNCOVERED: ['cross/conventions/local-development-setup.md',
            'cross/conventions/user-skill-api-listing.md',
            'maui/architecture/binding-build-integration.md',
            'maui/conventions/integration-host-verification.md']
```
(`user-skill-api-listing.md` は先行 change 由来で本変更の責ではない)

3 本はいずれも**開発者側の知識**であり、利用者向け Skill には載らない。`excluded` に既に入っている `android/architecture/build-toolchain.md` (「開発側ビルド基盤 (toolchain 更新手順) の知識」) と完全に同じクラスである。放置すると、次に docs-refresh を通常モードで走らせたとき Step 3c が検査失敗を出し、Step 6-① が `UNCOVERED` を報告して、オーナーが 3 本の配置判断を求められる。

さらに **tasks 8.6 は `--readme-only` で docs-refresh を実行して合格としているが、`--readme-only` は 3a〜3c と 3e をスキップする** (SKILL.md:122)。つまり本変更が docs-refresh 能力に持ち込んだこの差分は、本変更の自己検証では原理的に踏めない経路にあった。8.6 の合格は「4 枚を対象に取ること」の確認としては妥当だが、docs-refresh 能力全体の健全性の確認にはなっていない。

なお SKILL.md Step 3c は「配置判断はユーザーに提示し、スキルが独断で決めない」と定めているため、実装者が manifest へ勝手に書き込まなかったこと自体は規律に沿っている。問題は、判断をオーナーへ上げないまま次回実行へ先送りした点。

**推奨修正**: 3 本 (最低でも本変更由来の 2 本 + `local-development-setup.md`) について「利用者向け Skill には載せず `excluded` とする」判断をオーナーに諮り、承認が取れたら `skills/.manifest.json` の `excluded` に理由つきで追記する。オーナー判断を本変更に含めない方針なら、`deviation.md` に「次回 docs-refresh 通常実行時に配置判断が必要な concept 3 本」として明示的に申し送る。

---

### [🟡 Minor] deviation が判断を委ねた MAUI テストコマンドの置き場について

**該当箇所**: `kasane/concepts/cross/conventions/local-development-setup.md:151-155`、`kasane/concepts/cross/conventions/test-execution.md:15`

**問題点**: deviation.md「グループ1 実施時の差分」が明示的に独立レビューへ委ねた論点。判断は次のとおり。

**`test-execution.md` へ寄せるべき**。理由は 2 つ。

1. **索引としての単一性**。「このリポジトリのテストはどう走らせるか」を調べる読者は `test-execution.md` を開く。そこに iOS と Android があって MAUI だけ無ければ「MAUI にはテストが無い / 未検証だ」と読む。一方 `local-development-setup.md` は「環境を作って Sample を起動しステップインする」文書であり、その「本体 source へステップインする > MAUI」節の末尾にテストコマンドがあることは、目次からも 1 行説明からも到達できない。
2. **規約の迂回になっている**。`test-execution.md:15` は「MAUI は、実際に実行して確かめた時点で追記する (未検証の手順は書かない)」と定めている。これは「検証してから書け」という規律であって、「別ファイルなら未検証でも書いてよい」ではない。別ファイルへ置くと規律だけが素通りする。加えて `local-development-setup.md:151` の「このテストコマンドは、現時点では本書でのみ案内する。」は、文書の内容ではなくリポジトリの記述状況を書いた文で、`test-execution.md` に MAUI 節が入った瞬間に腐る。

**推奨修正**: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` を実際に実行して実行件数を確認し、`test-execution.md` に「## MAUI」節として、実行方法・実行件数の得方 (`dotnet test` の `Passed! - Failed: N, Passed: M`)・素の `net10.0` で走る範囲 (= platform TFM 側は検証されない、という「黙って検証にならない範囲」) を書く。`local-development-setup.md` 側はコマンドを消して `test-execution.md` への参照に置き換え、「本書でのみ案内する」の 1 文も削除する。実行して確かめられないなら本変更では動かさず、上記を deviation の申し送りとして残す。

---

### [🔵 Suggestion] ルート README の MAUI 最小コード例は、そのままでは動かない

**該当箇所**: `README.md:121-135` / `README_ja.md:121-135`

**問題点**: MAUI の例は XAML だけで、`MauiProgram` の `.AddKsSettingsView()` が無い。これを貼っただけでは `SettingsView` の handler が未登録で動かない。iOS / Android の例は貼れば動くので、3 例の中で MAUI だけ性質が違う。

spec の Requirement「ルート README の節構成」は「各例は対応する platform Skill の最小動作コードブロックと一致する SHALL」と定めており、`kssettingsview-maui/SKILL.md` の "Minimal working example" もこの XAML ブロックなので、**コードブロック自体は仕様どおり**。したがってコードは変えられない。

**推奨修正**: コードブロックの直前または直後に 1 文だけ添える (例: "Register the library once in `MauiProgram` with `.AddKsSettingsView()` — see the [.NET MAUI Skill](skills/en/kssettingsview-maui/SKILL.md)."/「利用前に `MauiProgram` で `.AddKsSettingsView()` を呼ぶ。詳細は MAUI Skill を参照」)。節の外枠なので Requirement には抵触しない。

---

### [🔵 Suggestion] 英語 README に日本語 UI のスクリーンショットが載る (承認済みの申し送り)

**該当箇所**: `README.md:19-26`、`ui/brief.md`「申し送り」

**問題点**: 4 枚とも Sample の日本語 UI (「Section 装飾デモ (style 切替)」「機内モード」「外観モード」等)。brief.md がオーナー承認と申し送りを記録しているので**指摘ではない**が、public 化 (phase-2) 時点で英語話者に見せる顔として残るため、忘れられると効く。あわせて、採用画像には Sample のデモ操作部 (Classic / Modern 切替セグメント、装飾プリセット行) が写っており、ライブラリの見た目ではなく Sample アプリの見た目に見える面もある。

**推奨修正**: 修正不要。phase-2 の実施手順書または簡易起票へ「Sample の英語リソース追加 → スクリーンショット撮り直し」を申し送る。

---

### [🔵 Suggestion] Sample 実行手順から Simulator / Emulator の起動手順が落ちている

**該当箇所**: `kasane/concepts/cross/conventions/local-development-setup.md:112-135`

**問題点**: 移送元 `samples/maui/README.md` にあった次が落ちている。

- iOS: `xcrun simctl boot "$SIMULATOR_UDID"` と `open -a Simulator`、および「`booted` 指定は起動中シミュレータが 2 台以上あると宛先が定まらないので UDID を明示する」という注記
- Android: `emulator -list-avds` で AVD 名を確認し `"$ANDROID_HOME/emulator/emulator" -avd <AVD 名> &` で起動する手順 (新文書は「Emulator を起動してから次を実行する」とだけ書く)

本文書は「clone した開発者が Sample を実行するまで」を掲げているので、install / launch はあるのに boot が無いのは手順として途切れている。`booted` の宛先が定まらない注記は、実際に踏むまで気づけない類の知識。

**推奨修正**: 「MAUI iOS」「MAUI Android」の各節に boot の 1 行と `booted` 指定に関する注記を戻す。1 行ずつで済む。

## アクションプラン

1. **Major 1** — `test-execution.md:60-62` の `swift test` 節を削除または書き換え、timestamp 更新
2. **Major 2** — `README_ja.md:159` のリンク先を `.github/CONTRIBUTING_ja.md` へ
3. **Minor 9** — MAUI テストコマンドを `test-execution.md` へ寄せる (実行して件数を確認してから)。できなければ deviation へ申し送り
4. **Minor 3** — `local-development-setup.md` に本体モジュールのビルド / lint 節を追加し、A→C の再判定を deviation へ記録
5. **Minor 4** — `binding-build-integration.md` に `KsSettingsView.Bridge` namespace と `Metadata.xml` の `managedName` 上書きを追記
6. **Minor 5** — 2 ファイルの frontmatter `description` と `cross/index.md:19` / `core/index.md:32` の 1 行説明を更新
7. **Minor 7** — `log.md` の見出し位置を戻し、新規 3 本を `created:` で記録。初見可読性レビューを実施
8. **Minor 8** — 新規 concept 3 本の manifest 配置判断をオーナーに諮る (または deviation へ申し送り)
9. **Suggestion 10 / 12** — README の MAUI 例に登録手順の 1 文、`local-development-setup.md` に boot 手順を追加
10. **Suggestion 11** — Sample 英語化とスクリーンショット撮り直しを phase-2 へ申し送り

## 確認した観点 (指摘に至らなかったもの)

- **spec `repository-docs` 全 Requirement**: README の所在 (公開ドキュメント面で `README*.md` はちょうど 5 枚)、節構成と順序、開発者向け手順の不在、導入手順の委譲、最小コード例と各 SKILL.md の一致 (3 platform とも完全一致・AiForms 移行 Skill は非対象)、prerelease の 3 ecosystem 別記載、サードパーティ通知の Sample 限定明示、配信準備中バナーの単一性とインストール節への未配信注記の不在、API 安定性表記の常設化、英日の見出し階層一致、貢献節と CONTRIBUTING 英日、Issue Forms のバグ 5 項目 / 提案 3 項目の `required: true`、`blank_issues_enabled: false`、日英どちらでも投稿可の案内、配布座標の文書間一致 — いずれも充足
- **spec `docs-refresh` 全 Requirement**: manifest `readmes` の 4 枚、3d の 1 種化、旧指示の残存 (deviation が意図的残置と説明した 2 箇所以外は除去済み。SKILL.md を `samples/` `モジュール一覧` `モジュール表` `デモ画面` `3 種` で grep して確認) — 充足。deviation の「意図的に残した 2 箇所」の解釈 (廃止の根拠と再導入の禁止は「旧指示」ではない) は妥当と判断
- **移送の網羅**: 廃止 5 枚を `git show HEAD:<path>` で全文読み、design.md の対応表・deviation.md の包括解釈と突き合わせた。C 判定 (移送) は Minor 4 の 1 件を除きすべて着地。A 判定 (破棄) は Minor 3 の 1 件を除き移送先の実在を確認
- **新規 concept の事実照合**: `binding-build-integration.md` の `_RegisterXcodeProjectNativeReference` / `_AdjustKsBridgeXcodeProjectInputs` / `CreateNativeReference=false` は csproj 実在を確認。`integration-host-verification.md` の期待表示・「解放 → 再生成」シナリオ・MauiHost のボタン文言は `maui/tests/shared/KsBridgeScenario.cs` / `MenuPage.cs` / `SettingsPage.xaml` と一致。README には無かった記述 (MauiHost の 5 手順、再生成後の表示) は実ソースから起こされており、移送元より情報量が増えている
- **バージョン値**: README の対応プラットフォーム表 (.NET SDK 10.0.300 / net10.0-ios;net10.0-android / AGP 8.13.2 / Kotlin 2.4.10 / Gradle 9.5.0 / minSdk 29 / compileSdk 35 / swift-tools 5.10 / iOS 16.0) を `global.json`・csproj・`libs.versions.toml`・`gradle-wrapper.properties`・`build.gradle.kts`・`Package.swift` と突合。全一致
- **リンク解決**: ルート README 2 枚・`.github/*.md` の全相対リンク、`kasane/concepts/**` と `skills/README*.md` の全相対リンクをスクリプトで検査。未解決 0 件。廃止 README への参照はリポジトリ全体でも `.claude/worktrees/` の古い作業ツリー (Decision 3 の対象外) のみ
- **スクリーンショット**: 4 枚を目視。platform × style の 4 通り、同一デモ画面・同一スクロール位置 (最上部)・同一プリセット (既定)、ステータスバーに時刻 9:41 固定でキャリア名・バッテリー残量・実機時刻なし。`maui/spike/README.md` は無参照で健全
- **lint**: `scripts/local-path-lint.py` / `scripts/identity-lint.py` ともに exit 0。`local-path-lint.py` は `git grep --untracked` を使うため未追跡の新規ファイル (`README_ja.md` / `.github/**` / 新規 concept 3 本) も検査対象に入っており、8.4 の合格は実質を伴う。`identity-lint.py` は `lint.identity.scope` により `kasane/` `skills/` のみ実効 (SKILL.md が同じ注記を持つ既知の制約)
- **足場の凍結**: `git diff HEAD` で確認した限り、proposal / design / specs は無改変。`tasks.md` はチェックボックスのみ、`ui/brief.md` は tasks 2.3 が指定した承認欄の記入のみ。逆流修正なし
- **付随修正 2 件**: `performance-verification.md` の参照差し替えは本務が直接の原因、`public-identifiers.md` の H1 追加は同ファイルを触ったついで。いずれも ksn-core の同梱条件内 (本務で触るファイル / 局所的 / 設計判断なし)。テスト対象のない文書変更のため担保はリンク解決と lint で足りる
- **テスト**: 本変更はコードの振る舞いを変えないため単体テストの対象なし (tasks.md 冒頭の宣言は妥当)。ソース・ビルドファイルへの変更が diff に一切無いことを確認済み
