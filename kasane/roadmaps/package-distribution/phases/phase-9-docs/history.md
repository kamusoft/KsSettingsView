# phase-9-docs 議論履歴

## 2026-08-29: 状態確認 — phase-10〜12 完了後に生きている論点の仕分け

議論再開にあたり、起案時 (2026-08-21) の 6 論点が phase-10〜12 の完了でどこまで解消したかを実物で確認した。

- **解消**: 移行ガイド (置き場は phase-10 で AiForms 移行 Skill、範囲は phase-12 で `SKILL.md` + `references/api-mapping.md` として英日実体化)、docs-refresh の依頼タイミング (phase-11 でリターゲット済み・manifest v3 で稼働中)
- **完了**: phase-1 申し送りのツールチェーン追随 (ルート README・`android/README.md` とも現行値、`docs/overview.md` は対象消滅)
- **未着手**: 英語 README 二本立て、状態表記、インストール手順、`verification/` をコード例の出典にする運用

「周辺は片付いたが本丸 3 件は丸ごと残っている」という結果。roadmap の phase-9 を in-progress へ遷移させた (phase-2 との並走は roadmap の制約「phase-2 の議論・手順書の準備は先行してよい」で承認済みのため改めて確認せず)。

## 2026-08-29: ルート以外の README を残すか

当初の論点は「英語化の対象範囲 (ルートのみ / platform も / 他は英語へ)」だったが、オーナーから「そもそもルート以外の README は必要か。自分も読んでいない。環境まわりは concepts にあるのではないか。エージェントはこれを見て作業しているのか」と問題提起があり、**存廃を前段の論点として組み替えた**。

調査結果:

- **エージェントは参照していない**。`AGENTS.md` の知識参照先は `kasane/concepts/` とコード・テストのみ。README を名指ししているのはルート README からのリンク 3 本と、docs-refresh の更新対象一覧だけ
- **環境まわりは契約と手順で分かれている**。SDK の解決方法・build root 境界といった契約は concepts (`android/architecture/build-toolchain.md`・`cross/architecture/repository-boundaries.md`) にあり、`ANDROID_HOME` の設定方法・Android Studio での開き方・トラブルシューティングといった手順は README にしかない
- **消すと困るものが 3 件**: MAUI binding の内部割り込み知識 (concepts の `native-bridge.md` が「正は `maui/README.md`」と書いており知識の正が逆転している)、サンプルのサードパーティ通知 (Material Symbols ライセンス)、サンプルの実行手順とデモ画面一覧

選択肢:

- **案A: ルート 2 枚に全廃** — 3 件を移送し README は英語 + 日本語の 2 枚だけ。docs-refresh の追随は 8 → 4 枚、検査② (デモ画面一覧の照合) も対象消滅
- **案B: samples だけ残す** — `android/` `maui/` を廃止して concepts へ移送、`samples/*/` は実行手順とライセンス表記の置き場として存置。移送は 1 件だが追随は 6 枚残る
- **案C: 現状維持** — 移送なし。public 化後の contributor には親切だが、知識の正の逆転が残る

**採用: 案A**。理由は読者の不在 (エージェントも参照せず、オーナーも読んでいない) に対して維持コストが 8 枚分あり、二本立てにすれば最大 14 枚へ増えること。加えて知識の正が README に滞留している逆転を、廃止に伴う移送で同時に解消できること。オーナーの評は「スッキリして良い」。

## 2026-08-29: サードパーティ通知の置き場

廃止する `samples/android/README.md` が持つ Material Symbols (Apache 2.0) の表記をどこへ移すか。候補は `samples/` 直下に `NOTICE.md` を新設する案と、ルート README の一節にする案。

**採用: ルート README** (オーナー判断)。実質の法的表記は各 vector drawable の先頭コメントにも接地しており、README 側は集約表記としての役割になる。ただしこれはサンプルアプリのアイコン由来であってライブラリ本体の依存ではないため、README では「サンプルアプリで使用しているアイコン」と明記して誤読を防ぐ。

ADR 捕捉は見送り: 置き場の局所判断で覆すコストが低く、選別 3 基準のいずれにも当たらない。

## 2026-08-29: 廃止する README の中身の移送ルール

廃止 5 枚の中身を読んで 4 分類に棚卸しした (A: 他所に既にある / B: 契約だが README にしかない / C: 手順で README にしかない / D: 法的表記)。A は捨て、B は concepts へ、D はルート README へで争点なし。争点は **C (手順) の行き先**。

選択肢:

- **案A: 開発手順は concepts へ、ルート README は利用者の入口に純化** — 環境セットアップと目視確認は `cross/conventions/`、検証ホストの起動と期待表示は `maui/` 配下
- **案B: ルート README を総合案内にする** — 「開発者向け」節を設けて手順を集約。clone した人は 1 枚で完結するが、公開直後の顔に SDK パス設定やエミュレータ起動コマンドが並ぶ
- **案C: 捨てを最大化する** — B だけ concepts へ移し手順の大半を捨てる。作業は最小だが検証ホストの起動手順・期待表示が失われる

**採用: 案A**。ADR-0022 が「利用者向けは `skills/`、正は concepts」と引いた線に沿うこと、`cross/conventions/` に `test-execution.md`・`runtime-behavior-verification.md` という開発手順に近い規約の前例があり C の受け皿になれることが理由。トレードオフとして contributor が開発手順に辿るには `AGENTS.md` → concepts の 2 段になる。

この決定は ADR-0023 (proposed) の具体化にあたるため、新規 ADR は立てず ADR-0023 の Decision / Alternatives / Consequences へ反映した。

## 2026-08-29: 未公開注記の縛りを ADR から外す

README の「未配信」状態表記を議論する前提として、cross/ADR-0022 (accepted) の Decision に「導入節は…公開レジストリに存在する前提の配布座標で書く (**未公開注記は公開後に履歴のゴミになるため書かない**)」という縛りがあり、roadmap のゴール「初回リリースまでは『未配信』の状態表記つき」と方針が割れている点を報告した。

オーナー判断: **括弧内の縛りを ADR から削除する**。「ADR に書くようなことではない。作業中にエージェントへ出した指示が、蒸留で ADR に拾われてしまったのかもしれない」。

処理: ADR-0022 の当該項目から括弧内を削除し、インラインの改訂注記 (core/ADR-0014 の作法に倣う) と出典行への改訂出典を追記した。「導入節は公開レジストリに存在する前提の配布座標で書く」という実務方針は `skills/` が実際にその形で書かれているため残した。

これにより README の状態表記は ADR との整合を気にせず、実害 (public 化から初回リリースまでの空白期間) と解除の手間だけで判断できるようになった。

## 2026-08-29: public 化から初回リリースまでの状態表記

public 化 (phase-2) と初回リリース (phase-8) の間に CI 構築・パッケージング・消費者検証が挟まるため、README が公開されている一定期間はそこに書かれた配布座標で実際にはインストールできない。この空白期間をどう扱うか。

選択肢:

- **案A: 冒頭に「配信準備中」バナーを 1 行だけ置く** — 手順本文は公開後の座標で書き切り、解除は phase-8 で 1 行消すだけ
- **案B: 何も書かない** — README は最初から完成形。phase-8 での作業はゼロだが、空白期間の訪問者は手順どおりにして失敗する
- **案C: 各インストール手順の中に注記を書く** — その場で伝わるが、解除箇所が SwiftPM / Maven / NuGet + prerelease 説明へ分散し消し忘れ得る

**採用: 案A**。空白期間が長い (CI・3 platform のパッケージング・消費者検証をまたぐ) ので実害を 1 行で塞ぐ価値があり、解除箇所を 1 つに閉じれば phase-8 での消し忘れも防げる。

あわせて、API 安定性の表記 (0.x の間は破壊的変更があり得る) は状態表記とは別物で公開後も残す常設の記述と整理した。

ADR 捕捉は見送り: 表記方法の局所判断で覆すコストが低く、選別 3 基準に当たらない。phase-8 での解除は agenda の TODO に申し送りとして残した。

## 2026-08-29: SwiftPM 配信リポジトリの名前

README のインストール手順を書く前提として、cross/ADR-0018 が「SwiftPM 専用の公開配信リポジトリを別に持つ」と決めていながら**名前が未確定**だった。作るのは phase-4 だが、README を先に書く phase-9 が確定させる。

オーナーからの問い「SwiftPM って利用時にこの配信リポジトリ名がリストに載る？」に対する調査結果:

- Xcode の Package Dependencies 一覧に出るのは `Package.swift` の `name:` (= `KsSettingsView`) であり、リポジトリ名ではない
- リポジトリ名が露出するのは ① Xcode の Package Dependencies 設定タブの URL 欄 ② `Package.resolved` の identity と location ③ 利用者の `Package.swift` の `.package(url:)` と `.product(package:)`
- Swift Forums で「package identity は git URL の最終パスコンポーネント由来、`Package.swift` の `name:` は表示専用」と確認 (https://forums.swift.org/t/why-does-swiftpm-use-github-repo-name-and-not-package-swift-name/55085)

続いてオーナーの依頼で著名ライブラリの実例を調査。**本体とは別に SwiftPM 配信リポジトリを持つ形は実在の慣例**で、`-spm` サフィックスが使われている:

| ライブラリ | 本体 | 配信リポジトリ |
|---|---|---|
| Lottie (Airbnb) | `airbnb/lottie-ios` | `airbnb/lottie-spm` |
| RevenueCat | `RevenueCat/purchases-ios` | `RevenueCat/purchases-ios-spm` |
| Branch | — | `BranchMetrics/ios-branch-sdk-spm` |
| Salesforce Mobile SDK | `forcedotcom/SalesforceMobileSDK-iOS` | `forcedotcom/SalesforceMobileSDK-iOS-SPM` |

`airbnb/lottie-spm` の `Package.swift` は `name: "Lottie"` / product `Lottie` で、我々の構成 (package 名 `KsSettingsView` + umbrella product `KsSettingsView`) と同型であることも確認した。ただし these の `-spm` は主に xcframework のラッパーで、動機 (履歴サイズの削減) は我々 (SwiftPM がサブディレクトリの Package.swift を解決できない制約) と異なる点は留保として共有した。

選択肢は `KsSettingsView-spm` / `KsSettingsView-swift` / `swift-kssettingsview`、およびサフィックスの大小。

**採用: `KsSettingsView-SPM`** (オーナー判断)。PascalCase の製品名に大文字サフィックスを付ける Salesforce の形。cross/ADR-0018 の Decision / Alternatives / Consequences と出典行へ追記した (覆すと利用者の URL と `Package.resolved` の identity が壊れるため ADR 級と判断)。

あわせて識別子の drift を 1 件報告: concepts の `public-identifiers.md` と ADR-0018 の配布先の表が旧規則 `jp.kamusoft:ks-settingsview-*` のままで、android/ADR-0016 の単一 artifact 化に追随していない。README は ADR の確定値で書き、concepts の改訂は phase-5 の責務として TODO に積んだ。

## 2026-08-29: ルート README の節構成

移送ルール (案A) で大枠が決まっているため、残る判断は「contributor 向けの記述をルート README にどこまで残すか」。現在の README には「モノレポ構成」「モジュール一覧」「知識の正本」「ビルド方法」の 4 節がある。

選択肢:

- **案A: 「リポジトリ構成」1 節に圧縮** — ディレクトリの表と `AGENTS.md` / `kasane/concepts/` への 1 行リンクのみ。手順・モジュール一覧・知識の正本の解説は載せない
- **案B: contributor 向けを一切書かない** — README は利用者だけのもの。歩き方は `AGENTS.md` に任せる
- **案C: 現状の 4 節を維持し手順だけ抜く** — モジュール一覧が残るが、利用者は umbrella product 1 本・単一 artifact 1 点で導入するため module 名が要るのは `import` を書くときだけ

**採用: 案A**。公開リポジトリを初めて訪れた人がディレクトリの意味を掴めない不親切を、リンクだけの数行で避けられるため。目次は「配信準備中バナー / 概要 + 主な特徴 / 対応プラットフォーム / インストール / 最小コード例 / Skills / リポジトリ構成 / ライセンス・サードパーティ通知」。

あわせてオーナーから貢献の受け付け方 (PR を受け付けず Issue で受ける、kasane の proposal.md / exploration.md フォーマット、Issue テンプレート) の提案があり、README の 1 節に収まらない広がりを持つため独立した論点として立てた。

## 2026-08-29: 貢献の受け付け方 — PR の締め方

オーナー提案: 外部からの PR は受け付けない (AI スロップの防止、レビュー負荷と品質維持)。貢献は Issue で kasane の proposal.md / exploration.md フォーマットに沿って投稿してもらい、オーナーが巡回して kasane の change に起こす。Issue テンプレートがあるとよい。

事実確認: GitHub は 2026-02 に PR アクセスの設定を追加しており、**完全無効化**と **collaborators only** の 2 段階が選べる (Settings > Features)。以前は自動クローズの workflow で代用するしかなかった。

選択肢:

- **案A: 完全無効化** — PR タブが消え意思表示として最も明確。ただしオーナー自身も PR を作れなくなる
- **案B: collaborators only** — 外部は PR を作成できず、オーナーと招待者は従来どおり
- **案C: 設定は触らず表明のみ** — 来た PR を閉じる運用。負荷が残り AI スロップ防止として弱い

**採用: 案B**。決め手は roadmap のゴール「PR / push で 3 platform のビルド・テストを回す検証 CI がある」(phase-3) — 完全無効化するとオーナー自身の PR も作れず、PR トリガーの CI が成立しない。collaborators only なら外部を締めつつ CI 設計に影響しない。

ADR 起票は、Issue フォーマットとテンプレートの決定まで含めて一本にまとめるため、方針が出揃ってから行う。

## 2026-08-29: 貢献の受け付け方 — Issue テンプレートの構成

`exploration.md` の骨格を確認したところ、外部の人が書けるのは前半 2 節 (「課題 / 動機」「検討した選択肢」) までで、「決定事項」「ADR 候補」「未決の論点」「変更級の推奨」はオーナーの判断領域だった。`proposal.md` (Why / What Changes / Non-Goals / Impact / 級) はさらに内部寄りで外部には書けない。`.github/` は未設置。

選択肢:

- **案A: 用途別 2 本を Issue Forms で** — バグ報告と提案で必須項目を出し分ける
- **案B: 汎用 1 本** — 「課題 / 動機」「検討した選択肢」のみ。必須項目が最大公約数になる
- **案C: kasane の生フォーマットをそのまま埋めてもらう** — 後半はオーナーが埋め直すことになり、埋めさせる意味が薄い

**採用: 案A**。AI スロップに効くのは書式の厳密さではなく「実際に動かした証拠の必須化」(バージョン・再現手順・実際の出力) であり、必須にすべき項目がバグと提案で違うため 1 本にまとめられない、という判断。書式の厳密さはむしろ AI が最も埋めやすい部分である点も理由。

## 2026-08-29: 貢献の受け付け方 — 方針の表明先

事実確認: GitHub は `CONTRIBUTING.md` を、Issue / PR の作成画面のリンク、リポジトリ概要の「Contributing」タブ、サイドバーの 3 箇所で提示する。置き場はルート / `docs/` / `.github/` のいずれかで、優先順位は `.github` → ルート → `docs`。

選択肢:

- **案A: README に「貢献」節 + `.github/CONTRIBUTING.md` に詳細**
- **案B: CONTRIBUTING を作らず README と Issue Forms の説明文だけ** — Issue 作成画面の自動リンクとサイドバー表示が得られない
- **案C: CONTRIBUTING に集約し README はリンク 1 行** — README で完結させる読者に方針が伝わらない

**採用: 案A**。「PR を受け付けない」は投稿しようとする人が投稿前に知るべき情報で、GitHub の自動リンク挙動がそこに効く。同時に README にも数行置き、README しか読まない読者にも届ける。

## 2026-08-29: 貢献の受け付け方 — Issue と CONTRIBUTING の言語

選択肢:

- **案A: Issue Forms は英語 1 セット (2 本)、`CONTRIBUTING` は英日 2 枚、本文は英語・日本語どちらも可と明記**
- **案B: Issue Forms も英日 2 セット (4 本)** — 選択画面に 4 本並び、項目の同期コストが倍
- **案C: すべて英語のみ** — `README_ja.md` を読んだ日本語話者が英語の CONTRIBUTING に飛ばされ、原典 AiForms から引き継いだ日本語話者への導線が切れる

**採用: 案A**。実際に埋めるのは自由記述欄なので、ラベルを英語に保っても本文を日本語で書ければ投稿しやすさはほぼ損なわれない、という判断。README の英日 2 枚体制とも揃う。

これで貢献方針 (PR の締め方・Issue テンプレートの構成・表明先・言語) が出揃った。

## 2026-08-29: Skills に書かれた配布識別子の照合

README のインストール手順を書くにあたり、既に公開形になっている `skills/` の導入節と ADR の確定値を照合した。

- Android `jp.kamusoft:kssettingsview:0.1.0` — android/ADR-0016 と一致
- MAUI `KsSettingsView.Maui` — maui/ADR-0025 と一致
- iOS `https://github.com/kamusoft/KsSettingsView-Swift` — **仮名**。phase-12 の deviation.md が「仮の配布座標で書く。公開時の実座標 (iOS 配信リポジトリ名の確定を含む) への追従は docs-refresh の責務」と記録した既知の仮置きで、本日の決定 `KsSettingsView-SPM` が確定値にあたる

`skills/{en,ja}/kssettingsview-ios/SKILL.md` の 3 箇所 (本文 URL・`.package(url:)`・`.product(package:)`) を更新する TODO を積んだ。

## 2026-08-29: インストール手順の書き方

Skills の各 `SKILL.md` に既に完全な導入節 (Xcode の GUI 手順・マニフェスト例・module 構成・要件表) があるため、README にどこまで書くかが判断。

選択肢:

- **案A: 座標 3 行 + prerelease の節 + Skills への誘導**
- **案B: README にも完全な導入手順を書く** — README だけで完結するが手順全体が二重管理になる
- **案C: README にインストール節を置かず Skills へ誘導** — ズレは起きないが README を開いた訪問者の期待を外す

**採用: 案A**。直前に見つかった iOS 配布座標の食い違い (skills 側が仮名 `KsSettingsView-Swift` のまま) が「重複はズレる」実例になっており、手順の正は Skills 1 箇所に集約する。ただし OSS の README としてインストールが無いのは不親切なので座標だけは README に置く。

## 2026-08-29: 最小コード例の出典

cross/ADR-0022 が docs-refresh の源泉を「concepts のみ」と定め README 群もその追従対象に含めている一方、roadmap 起案時の論点「`verification/` を README のコード例の出典にする」は README に concepts 以外の源泉を持ち込む — というアーキテクチャ上の緊張を整理した。

選択肢:

- **案A: `verification/` を出典にする** — 動作保証は最も強いが README の源泉が 2 本になり ADR-0022 の改訂が必要。phase-7 までは出典が存在しない
- **案B: 派生の連鎖を 1 本に保ち、`verification/` には同じ例を smoke test として置いて CI でビルドする** — 出典にはせず裏取りとして機能させる
- **案C: README に最小コード例を置かない** — 訪問者が書き味を判断できず採用検討の最初の関門で離脱する

**採用: 案B**。ADR-0022 の派生モデルを崩さずに、コンパイル保証だけを別経路 (phase-7 の `verification/` + CI) で得られるため。phase-7 への申し送りを TODO に積んだ。

## 2026-08-29: 英語 README と日本語 README_ja の同期規律

選択肢:

- **案A: 翻訳ロックステップ (常に同時更新)** — cross/ADR-0022 が `skills/` の en/ja に課した規律と同一
- **案B: 英語を正とし日本語は追随** — 日本語話者に古い情報が出る期間が生じる
- **案C: 日本語を正とし英語が追随** — 公開 OSS の顔である英語 README が遅れる

**採用: 案A**。同じ docs-refresh が skills と README 群の両方を扱うため、README だけ別規律にすると道具側に 2 つのモードを持たせることになる。執筆順序は縛らず、「同時に揃うこと」だけを規律とする。ADR-0023 の Decision へ 1 項目として追記した。

## 2026-08-29: 実行方式と change の粒度

実行方式には判断の余地がなかった。cross/ADR-0022 が「初期生成・構成の見直しは変更フローの承認を通す。docs-refresh は既存構成への追従更新に限定」と定めており、本フェーズの作業 (README 5 枚の廃止・2 枚化・docs-refresh の対象定義変更) は構成の見直しそのもの。docs-refresh の出番は change の後の追従運用。

あわせて roadmap 側の不整合を報告した: phase-9 は一覧で種別 `research` (Change 欄 `—`) だが、議論の結果このフェーズは実装を伴う change を持つことになった。種別変更は roadmap 本体の改訂のため ksn-roadmap の責務。

change の粒度の選択肢:

- **案A: 1 change にまとめる** — README 2 枚 + 旧 5 枚廃止 + 移送 + `.github/` 一式 + docs-refresh 対象定義変更 + skills の iOS 座標修正
- **案B: 「ドキュメント再編」と「貢献受け付け」の 2 change** — README の貢献節が指す `CONTRIBUTING.md` が無い期間ができる
- **案C: `.github/` 一式を phase-2 へ送る** — phase-2 の実施手順書が膨らみ、public 化当日にテンプレートを整える段取りになる

**採用: 案A**。README の「貢献」節と `CONTRIBUTING.md` が相互参照するため分割すると中途半端な状態が生じること、roadmap の「1 フェーズ = 1 change」原則に合うことが理由。GitHub の設定変更のみリポジトリ操作として phase-2 へ申し送る。

これで phase-9 の論点はすべて決定事項へ移動し、論点リストは空になった。

## 2026-08-29: 提案化中の発見 — maui/spike/README.md の扱い

ksn-propose でデルタスペックの Requirement「README の所在」を書く段階で、ADR-0023 の廃止対象の列挙 (5 枚) から `maui/spike/README.md` が漏れていることが判明した。docs-refresh の追従対象 (manifest の `readmes` 8 枚) にも入っておらず、concepts からの参照もない。内容は完了済み spike (binding toolchain の疎通検証) の構成・成功ゲート・再現手順・toolchain 注意点。

選択肢: 対象外にする / README だけ廃止する / `maui/spike/` ごと削除する。

**採用: 対象外にする**。`spike/` はコードディレクトリでその存廃は本変更 (ドキュメント再編) のスコープ外であり、README だけ消すと完了済み検証の再現手順が失われるため。ADR-0023 に例外として明記し、`maui/spike/` 自体を公開リポジトリに載せるかは phase-2 の論点として申し送る。

## 2026-08-29: ルート README のスクリーンショット (追加要望)

提案化の途中でオーナーから追加要望: ルート README は利用者が第一に見る文書なので視覚的要素が必要で、Modern と Classic のスクリーンショットを貼って、どんな画面が作れるかを一目で示したい。

撮影対象の実在を確認: 3 platform とも「Section 装飾デモ (style 切替)」があり Modern / Classic を切り替えられる。「基本 Cell 7 種デモ」も設定画面らしい絵として使える。

選択肢: 1 platform × 2 style の 2 枚 / iOS・Android × 2 style の 4 枚 / 3 platform × 2 style の 6 枚。

**採用: 4 枚 (2 列 × 2 行)**。style の対比に加えて「iOS と Android で同じ設定画面が作れる」という本ライブラリの核心を 1 目で示せるため。MAUI は Native をラップし見た目が同じになるため画像では示さず 1 行で補足する (画像を増やしても情報量が増えない)。

あわせてオーナー承認済みの方針: 置き場はルートに `assets/` を新設 (旧 `docs/` は廃止済みで復活は ADR-0023 と衝突、`.github/` は GitHub 以外で読まれたときに意味が通りにくい) / 画像は 1 セットを英日 README から共有しキャプションのみ言語別 / 撮影はシミュレータ・エミュレータで行い端末固有情報を写さない (identity-lint の検査範囲)。採用する画面は実装時に候補を撮って選ぶ (mock 承認ゲートの変形として `ui/` に候補を置く)。

## 2026-08-29: 提案化中の発見 — public-identifiers.md の禁止事項との緊張

ksn-propose の自己レビュー (上位層違反チェック) で、concepts `cross/conventions/public-identifiers.md` の「してはいけないこと」2 項目 —「composite build が解決する開発用 GAV を公開済みの配布座標と説明しない」「実装のない MAUI product / package ID を現在利用可能な識別子として列挙しない」— が、本フェーズの決定「インストール節は公開レジストリに存在する前提の配布座標で書く」と衝突することを検出した。

同じ緊張は phase-12 でも発生しており、deviation.md が合意記録として握っていた。

選択肢: deviation で握る (phase-12 と同じ) / concepts に但し書きを足す / 未配信注記を戻す。

**採用: 該当 2 項目を削除する** (オーナー判断「その項目自体意味ないと思うので消して良い」)。いずれも未公開であることを理由にした記述制限で、配信開始後は無意味になる一方、それまではパッケージング (phase-4/5/6) とリリース (phase-8) でも同じ衝突を繰り返す。規約側を正す。

concepts の編集は提案フェーズではなく実装で行うため、change の tasks (1.9 / 1.10) に積んだ。ADR 起票は見送り (規約の緩和で覆すコストが低く、選別 3 基準に当たらない)。
