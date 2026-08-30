# Deviation: consolidate-readmes-and-contribution

design / spec と実装の差分。実装フェーズ中に発生順で追記する (spec 本体への逆流修正はしない)。

## 2026-08-29 移送対応表の包括解釈

- design.md Decision 1 の移送対応表: 行が「出典ファイル×節名」で立っており、同一内容クラスの節が別の README にも存在する分 (未分類 9 件) を網羅していなかった → **対応表は内容クラスで読む**ものとし、出典 README を問わず同一内容クラスの節は同じ行の指示に従う、と確定した。理由: Decision 1 の目的は移送**先**の確定 (「移送先をディレクトリ単位で指示すると実装者判断になる」) であり、節の網羅列挙ではないため。design.md は書き換えない (2026-08-29)
- 上記に基づく個別確定 (いずれも対応表の既存行への割り当て):
  - `android/README.md`「Sample アプリでの動作確認」→ C。`cross/conventions/local-development-setup.md` (`samples/*/README.md`「本体ライブラリのデバッグ」行と同クラス)
  - `samples/{ios,android,maui}/README.md`「必要環境」→ C。同上 (`android/README.md`「必要環境」行と同クラス)
  - `samples/android/README.md`「Android SDK ロケーションの設定」「トラブルシューティング」→ C。同上 (`android/README.md` の同名節の行と同クラス)
  - `samples/{ios,android}/README.md`「ディレクトリ構成」→ A (破棄)。`samples/maui/README.md`「ディレクトリ構成」行と同クラス
  - `samples/*/README.md`「関連リンク」→ A (破棄)。リンク集のみで固有知識を持たない
  - `samples/android/README.md`「概要」の `FragmentActivity` / Material3 Theme 前提 → A (破棄)。`android/README.md`「利用アプリ側の前提」行と同クラス
  - `maui/README.md`「facade 層 (KsSettingsView.Maui)」→ A (破棄)。facade の責務は `maui/api/maui-facade.md`、`net10.0` TFM の理由は maui/ADR-0009、テストコマンドは `cross/conventions/test-execution.md` に既出
  - `samples/{ios,android}/README.md`「基本 Cell 7 種デモ画面の Theme」→ 分割。Theme の色値一覧は A (破棄。実ソース `SampleTheme` が正で、`cross/conventions/sample-parity.md` が SampleTheme を共通定義として参照する旨を既に規定)。「ライブラリの Theme 既定値はクロスプラットフォーム中立色のままとし、AiForms 互換色は利用側が設定する」という**方針**のみ、既出でなければ C として `core/styling/` の該当 concept へ移す
- **A 判定は破棄前に既存記述の実在を 1 件ずつ確認する** (tasks 1.11 と同じ扱い)。確認できないものは移送に倒す

## 2026-08-29 MAUI のステップイン手順を新規記述

- spec `repository-docs` Requirement「開発者向け知識の所在」/ Scenario「Sample の実行手順の到達可能性」は「3 platform それぞれの実行手順とステップイン手順が読め」を要求するが、移送元の `samples/maui/README.md` に「本体ライブラリのデバッグ」節が存在しない → **移送ではなく実構成からの新規記述**とした。根拠: `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj` が本体を `ProjectReference` で参照しており (iOS / Android Sample と同じ構成)、デバッガのステップインが成立する。理由: 移送元不在を理由に落とすと Scenario を満たせないため (2026-08-29)

## 2026-08-29 グループ1 実施時の差分

- `maui/README.md`「facade 層」の `dotnet test` コマンド: 上の包括解釈では A (破棄。`cross/conventions/test-execution.md` に既出) としていた → **実在確認の結果 C (移送) へ変更**。`test-execution.md` は 15 行目で「MAUI は実際に実行して確かめた時点で追記する (未検証の手順は書かない)」と明記しており、MAUI のテスト手順を持っていなかったため。移送先は `cross/conventions/local-development-setup.md`。理由: tasks 1.11「A 分類は実在確認してから破棄」に従うと破棄できないため (2026-08-29)
  - **要レビュー**: MAUI のテストコマンドの置き場が `local-development-setup.md` でよいか (`test-execution.md` が本来の主題であり、そちらへ寄せる選択もある)。独立レビューの判断に委ねる
- MAUI のステップイン手順の保証範囲を「facade の C# ソースまで」と限定して記述した。理由: MAUI Sample は `ProjectReference` 越しに facade へは入れるが、その先の Native binding は別機構であり、実構成から確認できない範囲を書かないため (2026-08-29)
- Sample Theme の「ライブラリ既定値はクロスプラットフォーム中立色」方針: concepts に既出でなかったため、包括解釈の指示どおり `core/styling/style-resolution.md` へ移送した (2026-08-29)

### [付随修正]

- `kasane/concepts/maui/conventions/performance-verification.md`: `samples/maui/README.md` を知識の正とする参照 2 箇所を `local-development-setup.md` へ差し替え。本務 (README 廃止に伴う知識の正の移動) が直接の原因で要修正になった参照整合 (2026-08-29)
- `kasane/concepts/cross/conventions/public-identifiers.md`: 1.12 の削除で触れたついでに、欠落していた H1 を追加 (可読性規約の充足) (2026-08-29)

## 2026-08-29 グループ 4・6 の実施主体

- tasks 4.1 (旧 README 5 枚の削除) と 6.1〜6.3 (`.agents/skills/docs-refresh/SKILL.md` の改訂) は、**counterpart (codex) ワーカーの sandbox が実行できずオーケストレーター (ホスト) 側で実施**した。前者は外部ボリューム上の Trash 領域へ書けず `trash` が `afpAccessDenied`、後者は `.agents/` への patch が `writing outside of the project` で拒否されたため。成果物の内容に影響はない (2026-08-29)

## 2026-08-29 docs-refresh SKILL.md に残した意図的な言及

- spec `docs-refresh` の Scenario「旧指示の残存がないこと」を字義どおり grep すると、`.agents/skills/docs-refresh/SKILL.md` の **3 箇所**が該当するが、いずれも**意図的に残した**もの:
  - **追従対象の範囲注記** (追従対象の表の直後): 「platform ディレクトリ (`android/` `maui/`) と Sample ディレクトリには README を置かない (cross/ADR-0023)。`maui/spike/README.md` は完了済み検証の記録であり追従対象に含めない」。ADDED Requirement「追従対象の README 群」が `maui/spike/README.md` の除外を SHALL で要求しており、**この記述自体が仕様の実装**。「置かない」と言う否定形であって追従指示ではない
  - **廃止理由の注記** (3d の表直後): 「従前の『モジュール一覧』と『Sample デモ画面一覧』の突合は行わない。前者の突合先だったルート README のモジュール表・`android/README.md`・`maui/README.md` と、後者の突合先だった `samples/*/README.md` がいずれも存在しなくなったため」。`design.md` Decision 2 の理由をそのまま置いたもので、spec の MODIFIED Requirement 本文も同じ言い回しで同じパスを挙げている。**指示ではなく廃止の根拠**
  - **README 委譲プロンプト (5b) の禁止指示**: 「モジュール一覧・ビルド手順・環境セットアップ手順は README に置かない — 利用者の入口に純化する (cross/ADR-0023)。取得元に無いこれらの節を新設しないこと」。Decision 2 が挙げた再導入リスク (「将来の docs-refresh がルート README にモジュール表を再導入し ADR-0023 を静かに破る」) を能動的に塞ぐための記述で、**「確認せよ」の逆向き**
- Scenario の「旧指示」は動作指示を指し、追従範囲の否定形での定義・廃止の根拠・再導入の禁止はこれに当たらないと解釈した。それ以外の箇所 (Step 3d の突合表 / Step 4 の実行例 / README 委譲プロンプトの確認事項 / 整合性チェック / 完了サマリ) からは platform / Sample README への言及とモジュール表確認の指示を除去済み (2026-08-29、verify-001 の指摘を受けて列挙を 2 箇所から 3 箇所へ是正)

## 2026-08-29 相方セカンドオピニオン (code-review) の未実施

- `kasane/config.yaml` の `second-opinion.code-review: [m, l]` は L 級で相方レビューの並走を要求するが、**実施できなかった**。相方 (codex) が実行途中で利用枠の上限に達し (`You've hit your usage limit` / 消費 218,352 tokens)、diff の読み込みまでで停止して判定を出せなかったため。認証エラー (exit 78) ではなく枠の枯渇で、回復は翌 01:15 以降
- 対処: ホスト側の独立レビュー (`review-001.md`) のみで判定した。**クロスモデルの視点は本変更に入っていない**
- 申し送り: 相方の枠が回復したら `ksn-second-opinion` の code-review モードを単独で実行できる (change ディレクトリは蒸留まで凍結されるため入力は同一)。実施した場合の証跡は `second-opinion-code-001.md` へ (2026-08-29)

## 2026-08-29 失われたビルド / テストコマンドの復元 (オーナー指示 → 主題別に配分)

- `design.md` Decision 1 の移送対応表は `android/README.md`「ビルド・テスト」を **A (移送しない。`cross/conventions/test-execution.md` とルート README に既出)** としていた → **C (移送)** へ変更。理由: 破棄の根拠だった「ルート README に既出」が、本変更のルート README 全面置換によって自己無効化されていた (review-001 Minor 3 / second-opinion-code-001 Minor F)
- オーナー指示は当初「git から復元して `test-execution.md` へ移す」だったが、**オーナー自身の再検討により主題別の配分へ改めた**。`test-execution.md` の当該節は「`swift test` を完了判定に使わない」ことを述べる節であり、そこへビルドコマンドを載せると同ファイルの規律と矛盾するため
- 確定した配分:
  - **失われたビルドコマンド** (`swift package describe` / `swift build` / `./gradlew build` / `./gradlew lint` / `./gradlew :ks-settingsview-*:assembleDebug` / `dotnet build KsSettingsView.Maui.csproj`) → `cross/conventions/local-development-setup.md`。git (`git show HEAD:README.md` / `git show HEAD:android/README.md`) から実体を復元する
  - **`test-execution.md`** → 節「README の `swift test` との関係」から偽になった前提 (「`README.md` は `swift test` を案内している」) を除去する。`swift test` 自体は移送しない (同ファイルが非推奨としているため)
  - **MAUI のテスト手順** → `test-execution.md` に「## MAUI」節として追記する。同ファイル 15 行目の「実際に実行して確かめた時点で追記する (未検証の手順は書かない)」を満たすため、`dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` を実行して **516 件合格 / 0 失敗** を確認済み。`local-development-setup.md` 側のテストコマンドは `test-execution.md` への参照に置き換える (2026-08-29)

## 2026-08-29 初見可読性レビューの実施と反映

- `ksn-core` references/concepts.md が経路共通ゲートとして要求する**初見可読性レビュー**を、新規 concept 3 本について独立文脈で実施した (レビュアーには本文のみを渡し、変更アーティファクト・移送元 README・実装ソースは渡していない)
- 判定: Major 4 / Minor 14 / Suggestion 6。**Major 4 件すべてと Minor 11 件・Suggestion 3 件を反映**した。主なもの:
  - `binding-build-integration.md` の構成図が「iOS binding は facade に繋がらない」と読める形だった → 両 binding が facade へ合流する形へ
  - アンダースコア始まりの MSBuild 識別子について、SDK 所有 (割り込み先) と binding csproj 所有 (割り込む側) の区別が本文から読み取れなかった → 所有者の表を追加
  - `integration-host-verification.md` の MauiHost iOS 手順が「IntegrationHost からの読み替え」で済ませており、**書かれたとおりでは動かなかった** (MauiHost は `TargetFrameworks` が 2 つで `-f net10.0-ios` 必須、出力 `.app` 名も別) → iOS の 3 コマンドを省略せず記載
  - 移送作業の経緯 (「ADR 本文は移送前の所在を指しているが」) が読者向け本文に漏れていた → 本文から削り、関連側の 1 行へ集約
- 反映しなかったもの: `local-development-setup.md` の分割提案 (Suggestion s6。「Sample を動かす人」と「本体をビルド / デバッグする人」で読者が違うという指摘)。節構成は綺麗に割れており分割は必須でないとレビュー自身が述べているため、**蒸留時の論点として申し送る**。ほか、対象読者を SDK 保守者に絞る旨の明示 (m5)・表セルの重さ (s2)・「既知の制約」節の読者混在 (s1) は構造の再設計を伴うため見送り (2026-08-29)

## 2026-08-29 accepted ADR からの参照切れ (蒸留への申し送り)

- `kasane/decisions/maui/0006-android-binding-gradlew-exec.md:23` は「再検証の入口は `maui/README.md` の『SDK 更新時に再検証する箇所』の表と対で維持する」と書いているが、**その `maui/README.md` を本変更で削除した**。ADR の status は accepted
- 移送先は `kasane/concepts/maui/architecture/binding-build-integration.md` の「SDK 更新時に再検証する箇所」節。**表 → ADR の導線は張った** (同節に「maui/ADR-0006 が再検証の入口として対で維持すると定めた表である」と明記し、「関連」に ADR を追加)
- ADR は accepted 後に本文を編集しない (ksn-core references/decisions.md) ため、**ADR → 表の向きは切れたまま**。supersede が要るかの判断は蒸留 (ksn-distill) に送る (2026-08-29)

## 2026-08-29 本変更で見送った改善 (phase-2 への申し送り)

両レビューが Suggestion として挙げ、本変更では修正しないと判断したもの。**記録がないと phase-2 で失われるため明示する**。

- **英語 README のスクリーンショットが日本語 UI** — Sample の英語化は `samples/` のコード変更で本変更の範囲外。オーナー承認済み (`ui/brief.md` の申し送り)。なお相方レビューが挙げた軽い代替案「英語 README のキャプションに `screenshots from the Japanese-locale sample app` を添える」(本変更内で 2 行) も**採らなかった** — 断り書きを足すより Sample の英語化で解くほうが筋がよく、中途半端な注記が残ると解決済みに見えるため
- **Issue の質問窓口が無い** (`.github/ISSUE_TEMPLATE/config.yml` の `contact_links: []`) — blank issue を無効化した結果、「使い方が分からない」「仕様か不具合か判断できない」利用者の行き先が bug / feature の 2 択しかない。spec Requirement「Issue テンプレートの必須項目」は `blank_issues_enabled: false` を求めるのみで窓口の追加を要求しておらず、cross/ADR-0024 の意図 (AI スロップ抑止) は現状で満たされるため見送る。ただし **`contact_links` は本変更の成果物内のフィールドであり「リポジトリ設定だから範囲外」ではない** — 窓口の設計 (Discussions を開くか、CONTRIBUTING へ誘導するか) は決定を伴う別議論として phase-2 へ送る。`kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/publish-procedure.md` が **Discussions OFF を確定済み**であり、窓口を置くならこの決定の見直しが要る点も併せて申し送る
- **`cross/conventions/user-skill-api-listing.md` が docs-refresh Step 3c で `UNCOVERED` に残る** — 先行 change (skills-api-coverage) 由来で本変更の責ではないが、本変更で `excluded` へ 3 本を足した結果、Step 3c の失敗要因はこの 1 本だけになった。docs-refresh SKILL.md は「配置判断はスキルが独断で決めない」と定めるため実装側では追加しなかった (2026-08-29)。**蒸留後 (2026-08-30) にオーナー判断を得て `excluded` へ追加し解消済み** — 「利用者向け Skill への API 掲載基準 (書き手向けの規約)。基準そのものは利用者向けレシピの対象外」。Step 3c を再実行して `concepts coverage OK` を確認した
