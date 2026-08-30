# phase-9-docs

public 化の前に (phase-10〜12 (Skills 化) の完了後)、README を英語化して `README_ja` を併置する原典 (AiForms.Maui.SettingsView) の運用を踏襲した形に大幅改訂し、インストール手順 (3 platform、確定済み識別子で記述し初回リリースまでは「未配信」の状態表記つき) と AiForms からの移行ガイドへの導線を整備する。状態表記の解除は phase-8 (初回リリース) で行う。README は利用者向け派生物であり、更新はユーザーの明示依頼で docs-refresh を起動して行う (cross/ADR-0022)。

## 論点

(すべて決定事項へ移動済み — 2026-08-29)

## phase-10 からの申し送り (2026-08-25)

- 移行ガイドの置き場は決定済み: AiForms 移行 Skill (`kssettingsview-aiforms-migration`、`SKILL.md` + `references/api-mapping.md`) として phase-12 で新規書き起こし。README からはリンクを張る形 — 論点「移行ガイドの範囲と置き場」のうち置き場は解消、範囲 (API 対応表と非互換点) の詰めが残り
- skills/ への導線: ルート README に Skills の節を置き `skills/README.md` (英語索引。日本語は `README_ja.md`) へリンクする。索引 2 枚の形は決定済みで、導線の本文を書くのがこのフェーズの責務

## phase-12 からの申し送り (2026-08-26)

- **導線の具体**: ルート README (現・日本語) には phase-12 で「主な特徴の直後の導線 1 文 + モノレポ構成表の `skills/` 行」が追記済みになる。英語化ではこれを英訳して維持し、英語 `README.md` → `skills/README.md`、`README_ja.md` → `skills/README_ja.md` の言語対応でリンクする
- **docs/ 前提の記述の読み替え**: 本 agenda 冒頭注記の「docs/ と README は利用者向け派生物 (cross/ADR-0014)」と論点内の「docs へ移すもの」は、phase-12 完了後は docs/ が廃止され cross/ADR-0022 (skills/ 体制) に代わっているため「skills へ移すもの」と読み替える (2026-08-29 に冒頭注記へ反映済み)。ただし README から溢れた内容を受けるために Skill 構成 (Skill の増減・references の再編) を変えるのは変更フローの承認が必要 (ADR-0022 — docs-refresh には委ねない)
- **docs-refresh の運用前提**: `--readme-only` とコード正チェックの交差は「報告のみ」で、次回の通常実行へ誘導される (phase-11 の [deviation.md](../../../../changes/archive/2026-08-26-retarget-docs-refresh-to-skills/deviation.md))。初運用は phase-12 が済ませている前提なので、本フェーズでは通常運用として依頼してよい

## 決定事項

### README はルート 2 枚 (英語 + 日本語) に集約する (2026-08-29)

ルート以外の README — `android/README.md`、`maui/README.md`、`samples/ios/README.md`、`samples/android/README.md`、`samples/maui/README.md` の 5 枚 — を**廃止**し、リポジトリの README は英語 `README.md` + 日本語 `README_ja.md` の 2 枚だけにする。 `skills/` の索引 2 枚は対象外 (ADR-0022)。`maui/spike/README.md` も対象外とする (2026-08-29 追記 — 完了済み検証の記録で docs-refresh の追従対象外。`maui/spike/` 自体の存廃は phase-2 の論点へ申し送る)。

根拠は読者の不在と維持コストの非対称:

- エージェントは参照していない。[AGENTS.md](../../../../../AGENTS.md) が指示する知識参照先は `kasane/concepts/` とコード・テストのみで、platform README は挙げられていない。README を名指ししているのはルート README からのリンク 3 本と、docs-refresh が**更新対象として**持つ一覧だけ
- オーナーも読んでいない (2026-08-29 の申告)
- 一方で docs-refresh は 8 枚を追随対象に抱えており、二本立てにすると最大 14 枚へ増える
- 環境まわりの**契約**は既に concepts にある (`android/architecture/build-toolchain.md`・`cross/architecture/repository-boundaries.md`)。README に固有なのは**手順**部分

廃止に伴う中身の移送先は、下の「廃止する README の中身の移送先」で決めた。

付随して docs-refresh の追随対象は 8 枚 → 4 枚 (`skills/README.md`・`skills/README_ja.md`・ルート 2 枚) になり、検査② (デモ画面一覧と `SampleScreen` 定義の照合) は対象消滅する。この対象定義の変更は ADR-0022 により変更フローの承認を通す。

### 廃止する README の中身の移送先 (2026-08-29)

廃止する 5 枚の中身を棚卸しして 4 分類に整理し、行き先を決めた。基本規則は **ルート README を利用者の入口に純化し、開発者向けの手順は concepts へ寄せる**。

| 分類 | 中身 | 行き先 |
|---|---|---|
| A. 他所に既にある | モジュール構成、利用アプリ側の前提 (Material3 Theme 必須・`FragmentActivity` 必須)、基本のビルド / テストコマンド、ディレクトリ構成、`SDK location not found` の対処 | **捨てる** (concepts・`skills/`・ルート README に既出) |
| B. 契約だが README にしかない | MAUI binding が SDK 内部ターゲットへ割り込む一覧、`XcodeProject` 採否の実験的経緯、`BG8605` / `BG8A00` 警告の意味、共有 scheme を消すと壊れる理由、`KsBridgeFont` の platform 差 | `kasane/concepts/maui/` (`api/native-bridge.md` とその周辺)。同文書の「正は `maui/README.md`」参照 2 箇所を同時に解消する |
| C. 手順で README にしかない | `ANDROID_HOME` と 2 つの `local.properties`、`DEVELOPER_DIR` 指定、検証ホストの起動コマンドと期待表示、サンプルの実行手順・デモ画面一覧・本体へのステップイン手順、実機目視確認チェックリスト | `kasane/concepts/` — 環境セットアップと目視確認は `cross/conventions/` (既存の `test-execution.md`・`runtime-behavior-verification.md` と同じ扱い)、検証ホストの起動と期待表示は `maui/` 配下 |
| D. 法的表記 | Material Symbols (Apache 2.0) の通知 | **ルート README** (サンプルアプリで使用しているアイコン由来である旨を明記し、ライブラリ本体の依存と読まれないようにする) |

ルート README は「概要・特徴・対応 platform・インストール・最小コード例・`skills/` への導線・ライセンス・サードパーティ通知」に純化し、開発者向けの手順は載せない (節構成の詳細は論点として継続)。

### public 化から初回リリースまでの状態表記 (2026-08-29)

ルート README の**冒頭に「配信準備中」のバナーを 1 行だけ置く**。インストール手順の本文は公開後の配布座標で書き切り、未配信の注記を手順側へ分散させない。

- 理由: public 化 (phase-2) は初回リリース (phase-8) の手前にあり、その間に CI 構築・3 platform のパッケージング・消費者検証が挟まる。この空白期間に README を読んだ人は、書かれた座標では実際にインストールできない。1 行で塞ぐ価値がある
- 解除は phase-8 でその 1 行を消すだけ。roadmap の制約「初回リリースで状態表記を解除する」が指す箇所はここ 1 箇所に限定される (分散させないのは解除漏れを防ぐため)
- **API 安定性の表記** (0.x の間は破壊的変更があり得る旨) は状態表記とは別物で、公開後も残る常設の記述として書く
- 前提の変化: cross/ADR-0022 にあった「未公開注記は公開後に履歴のゴミになるため書かない」という縛りは、2026-08-29 に ADR から削除した (作業中のオーナー指示が蒸留で拾われたもので、決定として残す性質ではなかった)

### SwiftPM 配信リポジトリ名と README に書く識別子 (2026-08-29)

配信リポジトリ名を **`KsSettingsView-SPM`** に確定した (Package URL: `https://github.com/kamusoft/KsSettingsView-SPM`)。phase-4 がこのリポジトリを作るが、README を先に書く phase-9 が名前を確定させる責務を持つため、cross/ADR-0018 の Decision へ追記した。

- SwiftPM の package identity は git URL の最終パスコンポーネント由来で、`Package.swift` の `name:` は表示専用。したがって Xcode の Package Dependencies 一覧に出るのは package 名の `KsSettingsView` だが、`Package.swift` を書く利用者は `.product(name: "KsSettingsView", package: "KsSettingsView-SPM")` と identity を書き、`Package.resolved` にも `kssettingsview-spm` として残る
- `-SPM` は配信専用リポジトリの既存慣例 (`airbnb/lottie-spm`・`RevenueCat/purchases-ios-spm`・`BranchMetrics/ios-branch-sdk-spm`・`forcedotcom/SalesforceMobileSDK-iOS-SPM`)。大文字表記は PascalCase の製品名に付ける Salesforce の形に合わせた。姉妹ライブラリは `KsDialogs-SPM` で展開する

README に書く 3 platform の識別子は次のとおり (いずれも ADR の確定値。実装後の追随は各パッケージングフェーズの責務):

| platform | 値 | 正 |
|---|---|---|
| SwiftPM | Package URL `https://github.com/kamusoft/KsSettingsView-SPM` / umbrella product `KsSettingsView` 1 本 | cross/ADR-0018 |
| Maven | `jp.kamusoft:kssettingsview` (単一 artifact) | android/ADR-0016 |
| NuGet | `KsSettingsView.Maui` | maui/ADR-0025 |

**未解消の drift**: concepts の `cross/conventions/public-identifiers.md` は旧規則 `jp.kamusoft:ks-settingsview-*` (module 別 artifact) のままで、cross/ADR-0018 の配布先の表も同じ旧値を載せている。android/ADR-0016 が単一 artifact へ変えた際の追随が未実施 (ADR-0016 の Consequences に改訂が宣言済み)。README は ADR の確定値で書き、concepts の改訂は phase-5 で行う。

### ルート README の節構成 (2026-08-29)

ルート README の目次を次のとおりとする。開発者向けの**手順**は載せず、**地図**だけを 1 節に圧縮する。

```
(冒頭) 配信準備中バナー
概要 + 主な特徴
スクリーンショット      — iOS / Android × Modern / Classic の 4 枚 (2 列 × 2 行)
対応プラットフォーム
インストール          — SwiftPM / Maven / NuGet + prerelease
最小コード例          — 3 platform
Skills               — 導線 + 索引 (skills/README.md · skills/README_ja.md) へのリンク
リポジトリ構成         — ディレクトリの表 + AGENTS.md / kasane/concepts/ への 1 行リンク
貢献                  — PR を受け付けず Issue で受ける旨 3〜4 行 + CONTRIBUTING へのリンク
ライセンス / サードパーティ通知
```

- 「リポジトリ構成」節はディレクトリの表 (`ios/` `android/` `maui/` `samples/` `skills/` `kasane/`) とリンクのみ。ビルド手順・モジュール一覧・知識の正本の解説は載せない (公開リポジトリを初めて訪れた人が構造を掴めない不親切を避けつつ、利用者の入口としての純度を保つため)
- 現在の README にある「モジュール一覧」(iOS 4 module / Android 4 module) は落とす。利用者は umbrella product 1 本・単一 artifact 1 点で導入し、module 名が要るのは `import` を書くときだけで、それは Skills 側が説明する
- 英語 `README.md` → `skills/README.md`、日本語 `README_ja.md` → `skills/README_ja.md` の言語対応でリンクする (phase-12 申し送り)

### 貢献の受け付け方 (2026-08-29)

外部からの **Pull Request は受け付けず、Issue で受ける**。動機は AI 生成の粗雑な提案 (AI スロップ) の流入防止と、レビュー負荷・品質維持。

- **PR の締め方: GitHub の Pull requests 設定を「collaborators only」にする** (Settings > Features。2026-02 に GitHub が追加した設定で、完全無効化と collaborators only の 2 段階がある)。完全無効化を採らないのは、roadmap のゴール「PR / push で 3 platform のビルド・テストを回す検証 CI」(phase-3) がオーナー自身の PR を前提とするため。collaborators only なら外部の PR は作成できず、オーナーと将来招く協力者の PR ワークフローと PR トリガー CI は残る
- 貢献は Issue で受け、オーナーが巡回して kasane の change (`kasane/changes/<id>/`) に起こして対応する
- **Issue テンプレートは用途別 2 本を GitHub Issue Forms (`.github/ISSUE_TEMPLATE/*.yml`) で置く**。Forms は項目を必須化できるが、必須にすべき項目がバグと提案で違うため 1 本にまとめない
  - **バグ報告**: バージョン / platform / 再現手順 / 実際の挙動 / 期待した挙動 を必須にする
  - **提案**: 解決したい課題 / 現状どう困っているか / 考えた選択肢 を必須にし、`exploration.md` の「課題 / 動機」「検討した選択肢」へそのまま写る形にする
- 外部から受け取れるのは `exploration.md` の前半 2 節に対応する情報まで。「決定事項」「ADR 候補」「未決の論点」「変更級の推奨」はオーナーの判断領域で、起票時にオーナーが埋める。`proposal.md` (Why / What Changes / Non-Goals / Impact / 級) は外部には求めない
- AI スロップへの抑止は書式の厳密さではなく**実際に動かした証拠の必須化** (バージョン・再現手順・実際の出力) で効かせる。書式の厳密さはむしろ AI が埋めやすい方向に働くため
- **方針の表明先はルート README の「貢献」節 (3〜4 行) + `.github/CONTRIBUTING.md` (詳細)**。README では「PR は受け付けない / Issue で受ける / テンプレートを使ってほしい」を短く述べ、理由と Issue の書き方は CONTRIBUTING に置く。GitHub は `CONTRIBUTING.md` を Issue 作成画面・リポジトリ概要のサイドバー・Contributing タブでリンクするため、投稿前に目に入る経路が README と合わせて 3 つになる (置き場の優先順位は `.github` → ルート → `docs` で、`.github/` に置く)
- **言語**: Issue Forms は英語 1 セット (2 本)、`CONTRIBUTING` は英日 2 枚、投稿本文は英語・日本語どちらでもよいと明記する。Forms を英日 2 セット (4 本) にするとテンプレート選択画面が煩雑になり項目の同期コストも倍になるため、ラベルは英語に保ち、自由記述の中身で日本語話者の書きやすさを担保する

### インストール手順の書き方 (2026-08-29)

README には **3 platform の依存宣言 (座標) だけ**をコードブロック 1 つずつ置き、詳細な導入手順は `skills/` の各 `SKILL.md` の導入節に委ねる。README には「Xcode での追加手順・含まれる module・要件は Skills を参照」の 1 行を添える。

- 理由: Skills の導入節には既に完全な導入手順 (Xcode の GUI 手順・マニフェスト例・module 構成・要件表) があり、同じ手順を README にも持つと二重管理になる。実際に今回、iOS 配布座標が skills 側で仮名 `KsSettingsView-Swift` のままになっている食い違いが見つかっており、重複はズレるという実例が出ている
- 一方、README にインストールが無いのは OSS の README として訪問者の期待を外すため、**座標そのものは README に置く**
- prerelease (`X.Y.Z-{alpha|beta|rc}.N`) の取り方は platform ごとに異なるため、短い節を 1 つ設ける

### 最小コード例の出典 (2026-08-29)

README の最小コード例は **Skills の最小動作コードと同じもの**とし、`concepts` → `skills/` → README の派生の連鎖を 1 本に保つ。`verification/` は出典にしない。

- cross/ADR-0022 は docs-refresh の源泉を「concepts のみ (コード・テストへの追従は蒸留・drift の責務)」と定め、その追従対象に README 群を含めている。`verification/` を出典にすると README の源泉が 2 本になり ADR-0022 の改訂が必要になる
- コンパイル保証は別経路で得る: **phase-7 で `verification/` を作るとき、README / Skills と同じ最小コード例を消費者プロジェクトに入れて CI でビルドする**。出典ではなく裏取りとして機能させ、壊れれば CI が気づく

### 英語 README と日本語 README_ja の同期規律 (2026-08-29)

**翻訳ロックステップ**とする — 片方だけを更新してコミットしない。docs-refresh は README 2 枚を必ず 1 回で更新する。執筆順序は問わない (日本語で書いて英訳しても逆でもよい)。縛るのは「同時に揃うこと」だけ。

cross/ADR-0022 が `skills/` の en/ja に課した規律と同一にする。同じ docs-refresh が README 群も扱うため、README だけ別規律にすると道具側に 2 つのモードを持たせることになる。

### 実行方式と change の粒度 (2026-08-29)

**変更フロー (ksn-propose → ksn-orchestrator) で 1 つの change として実装する**。docs-refresh の出番はこの change の後で、座標や記述を追従させる通常運用のとき。

- 実行方式に判断の余地はなかった: cross/ADR-0022 が「初期生成・構成の見直しは変更フローの承認を通す。docs-refresh は既存構成への追従更新に限定」と定めており、本フェーズは README 5 枚の廃止・2 枚化・docs-refresh の対象定義変更 (8 → 4 枚) という構成の見直しそのもの
- change に含める範囲: ① 英日 README 2 枚の新規作成 ② 旧 README 5 枚の廃止 ③ 移送 (B: MAUI binding 知識 → concepts / C: 環境セットアップ・目視確認・検証ホスト → concepts / D: サードパーティ通知 → ルート README) ④ `.github/` 一式 (Issue Forms 2 本・CONTRIBUTING 英日) ⑤ docs-refresh の対象定義変更 ⑥ `skills/` の iOS 配布座標の修正
- 分割しないのは、README の「貢献」節と `CONTRIBUTING.md` が相互参照するため。別 change にすると片方だけがマージされた状態が生じる。roadmap の「1 フェーズ = 1 change」原則にも合う
- GitHub の設定変更 (Pull requests を collaborators only) はリポジトリ操作のため phase-2 の実施手順書へ申し送る

### ルート README のスクリーンショット (2026-08-29)

ルート README の「主な特徴」直後に **iOS / Android × Modern / Classic の 4 枚** (横 2 列 = Modern | Classic、縦 2 行 = iOS / Android) を置く。利用者が最初に見る文書として、どんな画面が作れるかを一目で示す。

- MAUI は Native をラップするため見た目が同じになる。画像では示さず「MAUI でも同じ画面になる」旨を 1 行添える (画像を増やしても情報量が増えないため)
- 撮影対象は 3 platform とも存在する「Section 装飾デモ (style 切替)」と「基本 Cell 7 種デモ」。実際に採用する画面は実装時に候補を撮って選ぶ (mock 承認ゲートの変形として `ui/` に候補を置く)
- **置き場**: リポジトリルートに `assets/` を新設し、モノレポ構成表に 1 行足す (旧 `docs/` は廃止済みで復活は ADR-0023 と衝突、`.github/` は GitHub 以外で読まれたときに意味が通りにくい)
- **英日共通**: 画像は 1 セットを両 README から参照し、キャプションだけ言語別にする
- **撮影の統制**: シミュレータ / エミュレータで撮り、ステータスバーに端末固有情報が写らないようにする (identity-lint の検査範囲にも関わる)

### 状態確認で解消した論点 (2026-08-29)

- **移行ガイドの範囲と置き場**: 解消。置き場は phase-10 で AiForms 移行 Skill と決定、範囲は phase-12 で実体化済み (`skills/{en,ja}/kssettingsview-aiforms-migration/` に `SKILL.md` + `references/api-mapping.md`)。ルート README からのリンクも設置済み
- **docs-refresh の依頼タイミング**: 解消。phase-11 で skills/ + README 群へリターゲット済みで稼働中 (manifest v3、2026-08-29 実行実績あり)。残った「大改訂を docs-refresh で行うか変更フローで行うか」は本フェーズの実行方式の論点として分離した
- **phase-1 申し送りのツールチェーン追随**: 完了。ルート README は現行値 (Gradle 9.5.0 / AGP 8.13.2 / Kotlin 2.4.10)、`android/README.md` も AGP 8.13.2 / Gradle 9.5.0 へ更新済み。`docs/overview.md` は対象消滅。残差だった `android/README.md` の JDK 上限表記も、当該 README の廃止決定により消滅

## TODO

- [x] 論点の解消 (2026-08-29 完了 — 起案時の 6 論点と議論中に立てた 5 論点をすべて決定事項へ移動)
- [x] 移送の実施 (2026-08-30 完了) (B: MAUI binding 知識 → `concepts/maui/` / C: 環境セットアップ・目視確認 → `concepts/cross/conventions/`、検証ホスト → `concepts/maui/` / D: サードパーティ通知 → ルート README)
- [x] `kasane/concepts/maui/api/native-bridge.md` の「正は `maui/README.md`」参照を解消する (2026-08-30 完了 — 2 箇所とも `maui/architecture/binding-build-integration.md` への参照へ)
- [x] docs-refresh の対象定義変更 (2026-08-30 完了 — manifest の `readmes` を 4 枚へ、機械チェックを①②廃止・③のみへ、SKILL.md の 6 箇所から platform / Sample README への言及とモジュール表確認の指示を除去)
- [x] docs-refresh 依頼時に含める (phase-1 からの申し送り、2026-08-21): ツールチェーン記述の現行値追従 → 2026-08-29 に追従済みを確認 (`docs/overview.md` は対象消滅、`android/README.md` は廃止対象)
- [x] phase-5 への申し送り (2026-08-30 に phase-5 agenda へ転記済み): concepts `cross/conventions/public-identifiers.md` の artifactId 規則を単一 artifact (`jp.kamusoft:kssettingsview`) へ改訂し、cross/ADR-0018 の配布先の表も追随させる
- [x] phase-4 への申し送り (2026-08-30 に phase-4 agenda へ転記済み): 配信リポジトリ `KsSettingsView-SPM` を作成し、`public-identifiers.md` へ配信リポジトリ名を追記する
- [x] `skills/{en,ja}/kssettingsview-ios/SKILL.md` の iOS 配布座標を仮名 `KsSettingsView-Swift` から確定値 `KsSettingsView-SPM` へ更新する (2026-08-30 完了 — 各 3 箇所、残存 0) (各 3 箇所: 本文の URL・`.package(url:)`・`.product(package:)`)。phase-12 deviation.md が docs-refresh の責務と記録した追従
- [x] phase-7 への申し送り (2026-08-30 に phase-7 agenda へ転記済み): `verification/` の消費者プロジェクトに README / Skills と同じ最小コード例を入れ、CI でビルドさせる (出典ではなく裏取り)
- [x] phase-2 への申し送り (2026-08-30 に phase-2 agenda へ転記済み): 実施手順書へ「GitHub の Pull requests 設定を collaborators only にする」を追加する (cross/ADR-0024)
- [x] phase-8 への申し送り (2026-08-30 に phase-8 agenda へ転記済み): ルート README 冒頭の「配信準備中」バナー 1 行を初回リリース時に削除する
- [x] ksn-propose で変更提案を起こす (2026-08-29 完了 — L 級。[changes/consolidate-readmes-and-contribution](../../../../changes/consolidate-readmes-and-contribution/proposal.md))

## 実装結果 (2026-08-30 反映)

change: [changes/archive/2026-08-30-consolidate-readmes-and-contribution](../../../../changes/archive/2026-08-30-consolidate-readmes-and-contribution/proposal.md) (L 級)。独立レビュー 3 周で APPROVED、verify は全 31 Scenario VALID。cross/ADR-0023 / ADR-0024 を accepted へ昇格。

### 決定どおりに着地したもの

ルート README 英日 2 枚 (節構成・見出し階層が一致、最小コード例は各 platform Skill と逐語一致)、スクリーンショット 4 枚 (`assets/`、オーナー承認済み)、platform / Sample README 5 枚の廃止、`.github/` 一式、docs-refresh の追従対象 4 枚化と機械チェックの 1 種化、`skills/` の iOS 配布座標の確定値化。公開ドキュメント面の README は 5 枚 (ルート 2・`skills/` 索引 2・`maui/spike/` 1)。

### 決定と実装がずれた点 (deviation.md に 12 項目)

- **移送対応表の粒度**: design.md の対応表を「出典ファイル×節名」で立てたため、同一内容クラスの節が別 README にも現れる分 (9 件) を網羅できず実装が停止した。「対応表は内容クラスで読む」包括解釈を確定して再開。教訓として捕捉 (`transfer-table-enumerated-by-source-not-content-class`)
- **「他所に既出」を根拠にした破棄の循環**: `android/README.md`「ビルド・テスト」を A (破棄) と決めた根拠が「ルート README に既出」であり、そのルート README を本フェーズ自身が置換して消していた。`./gradlew build` / `lint` / 個別モジュール assemble / `swift build` / facade の `dotnet build` を git から復元し `cross/conventions/local-development-setup.md` へ移送。cross/ADR-0023 の Consequences に負の帰結として追記
- **MAUI のステップイン手順**: 移送元 (`samples/maui/README.md`) に節が無く、`ProjectReference` 構成を根拠に新規記述した
- **MAUI のテスト手順**: `test-execution.md` が「MAUI は実際に実行して確かめた時点で追記する」としていたため、`dotnet test` を実行して 516 件 / 0 失敗を確認したうえで MAUI 節を新設
- **`maui/ADR-0006` の参照切れ**: 同 ADR が `maui/README.md` の「SDK 更新時に再検証する箇所」の表を再検証の入口として指しており、README 削除で切れた。移送先 concept から ADR への逆リンクを張り、決定内容は変わっていないため **supersede はしない**と判断 (cross/ADR-0023 の Consequences に記録)
- **docs-refresh SKILL.md に意図的に残した 3 箇所**: 追従範囲の否定形での定義 / 廃止の根拠 / 再導入の禁止指示。Scenario「旧指示の残存がないこと」の「旧指示」は動作指示を指すと解釈

### 申し送り (受け皿を確定済み)

| 申し送り | 受け皿 |
|---|---|
| 配信リポジトリ `KsSettingsView-SPM` の作成と `public-identifiers.md` への追記 | [phase-4](../phase-4-ios-packaging/agenda.md) TODO |
| artifactId 規則の単一 artifact 化と cross/ADR-0018 の配布先表の追随 | [phase-5](../phase-5-android-packaging/agenda.md) TODO |
| `verification/` に最小コード例を入れて CI でビルドさせる | [phase-7](../phase-7-consumer-verification/agenda.md) TODO |
| Pull requests を collaborators only にする | [phase-2](../phase-2-public-readiness/agenda.md) TODO 申し送り 1 |
| Issue の質問窓口が無い (Discussions OFF 決定との衝突を含む) | [phase-2](../phase-2-public-readiness/agenda.md) TODO 申し送り 2 |
| 英語 README のスクリーンショットが日本語 UI | [phase-2](../phase-2-public-readiness/agenda.md) TODO 申し送り 3 |
| `maui/spike/` を公開リポジトリに載せるか | [phase-2](../phase-2-public-readiness/agenda.md) TODO 申し送り 4 |
| 「配信準備中」バナーの解除 | [phase-8](../phase-8-release-workflow/agenda.md) TODO |
| `cross/conventions/user-skill-api-listing.md` が docs-refresh Step 3c の `UNCOVERED` に残る (先行 change 由来) | **解消済み** (2026-08-30、オーナー判断で `skills/.manifest.json` の `excluded` へ追加)。Step 3c は `concepts coverage OK` |

### 見送った改善

- 英語 README のキャプションに「screenshots from the Japanese-locale sample app」を添える案 (本変更内で 2 行) — 断り書きが残ると解決済みに見えるため採らず、Sample の英語化で解く方向を phase-2 へ送った
- `cross/conventions/local-development-setup.md` の分割 (「Sample を動かす人」と「本体をビルド / デバッグする人」で読者が異なるという初見可読性レビューの指摘) — 節構成は割れており分割は必須でないとレビュー自身が述べたため見送り
