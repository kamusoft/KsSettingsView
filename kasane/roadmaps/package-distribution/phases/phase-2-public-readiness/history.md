# phase-2-public-readiness 議論履歴

## 2026-08-21: 履歴スキャンの手段と対象

- 前提: ローカルに gitleaks / trufflehog なし、git-filter-repo あり。リポジトリは private、235 commit・pack 約 37 MiB
- ksn-scout に作業ツリー + 全履歴の点検を委譲 → 秘密情報 0 件、機密ファイルの履歴混入なし。方針判断が要るものとして (1) コミット author の個人メール、(2) ローカル絶対パス約 1000 行 (長命層に KsDialogs / AiForms の未公開クローン参照)、(3) `DEVELOPMENT_TEAM`、(4) .gitignore の欠落パターン
- 選択肢: A) grep 点検 + gitleaks 1 回 / B) grep 点検のみ / C) trufflehog まで。採用 A — 残る穴がエントロピー検出だけで、gitleaks は導入が軽く phase-3 CI にも流用できる。C は秘密値 0 件の今回は過剰
- 実行結果: gitleaks git (HEAD 199 commit、`--all`) とも 0 件。`gitleaks dir` は未追跡ディレクトリでタイムアウトし打ち切り
- ADR: 該当せず (点検手順は可逆で局所的)。CI への組み込み方針は phase-3 で扱う

## 2026-08-21: 履歴の扱い (検出時の対応)

- 追加で確認した事実: `git config user.email` は local / global とも個人アドレス、Issue 1 / PR 11 / tag 0 / worktree は本体のみ、追跡中の evidence 媒体 (PNG 607 等) が 181 MB で `.git` 182 MB の大半
- ユーザー提案「public は新規リポジトリにする」を案 A として加え、B) filter-repo で書き換え / C) そのまま public 化と比較。軸は個人メールの露出・履歴中のローカルパス・作業コストとリスク・失うもの・clone サイズ・phase-8 への影響
- 採用 A — 秘密情報 0 件で消すべきものはなく、残る懸念 (個人メール・削除済みファイルのローカルパス) は新規リポジトリなら工数ゼロで消える。B は `--replace-text` の検証が重い割に結果は同じ。開発の本籍も新 repo へ移し旧 private は凍結保管 (ADR-0018 が却下した「ミラー」ではない)
- 派生: clone サイズは履歴を捨てても evidence を含む限り 181 MB のまま → 「公開対象の範囲」を論点④ (体裁) に加えた。新 repo 名・旧 repo の rename・`user.email` 切り替えは論点⑤ (実施手順) へ
- ADR: cross/ADR-0021 を proposed で起票 (覆すコスト高 + 将来を制約)

## 2026-08-21: 公開対象の範囲 — evidence 媒体 (論点④の一部を先行)

- 論点③ (ローカルパス) の作業量が archive の公開可否で変わるため、④ のうち「公開対象の範囲」を先に扱った
- 事実: 追跡ツリー約 200 MB のうち `kasane/changes/archive/**` の媒体 623 件 (33 変更分) が 181 MB、媒体を除くと 20 MB。長命層からの媒体参照は文言 2 箇所 (android/ADR-0001 footer、concepts/log.md) でリンクではない。ADR-0018 は「SwiftPM 利用者は monorepo 全体を clone する」を負の影響として既に明記
- 選択肢: A) archive の媒体を外す / B) Git LFS / C) そのまま。採用 A — 利用者全員の clone が 200 MB → 20 MB、証跡は旧 private に残る。B は公開 repo の LFS 帯域 (無料 1 GB/月) が利用者の clone で枯渇し得る
- 派生: これから作る変更の evidence 媒体の運用を論点 ④-b として追加。ADR は ④-b と合わせて判断

## 2026-08-21: 今後の evidence 媒体の運用 (途中経過)

- 案 B (`.gitignore` で媒体を追跡しない) は却下: Claude Code の worktree 運用では、main 上で保存した `ui/references/` や `approved.png` が未追跡だと worktree に存在せず、実装者に「見た目の正」が渡らない (ユーザー指摘、致命的)。worktree で作った産物が消える問題も同根
- 折衷案 E (入力は追跡・検証産物 dir のみ除外) を提示したが、ユーザーから「SwiftPM への配慮で足場を歪めている、履歴は増え続ける」と差し戻し → SwiftPM の配信形を先に見直すことにした (次項)

## 2026-08-21: SwiftPM の配信形 — monorepo 直接 vs 配信リポジトリ

- 発端: evidence 媒体の扱いがすべて「SwiftPM 利用者が monorepo を履歴ごと full clone する」への配慮だったため、ユーザーが配信リポジトリ方式を提案
- ADR-0018 (proposed) は「配布用ミラーリポジトリ」を却下していた (同期 CI と tag の二重管理、URL と issue 窓口の分裂)。再評価: tag は release CI が自動で打つので手作業の二重管理はなく、配信 repo の Issues / PR を無効化して monorepo へ誘導すれば窓口は割れない。ADR-0020 の「取り消せる順で publish → 最後に tag」にも素直に収まる
- 事実: `ios/Package.swift` の `path:` は `ios/` 相対なので、`ios/{Package.swift,Sources,Tests}` を配信 repo のルートへ置けば無改変で解決。ios/ 追跡容量 1.9 MB
- 採用: 配信リポジトリ方式。得るもの = SwiftPM 利用者が monorepo を clone しない (evidence / clone サイズ問題が消える)、ルート Package.swift 不要 (ADR-0001 例外・`.build/` ignore・samples/ios 参照変更が不要)。負 = 配信 repo と書き込み secret、2 URL 体制、dry-run は prerelease tag か `path:` 参照
- 処理: cross/ADR-0018 を改訂 (title / Context / Decision / Alternatives / Consequences / 出典)。ロードマップ本体への反映は ksn-roadmap (改訂) を案内。④-a (公開ツリーから archive 媒体を外す) は維持 — 理由が clone サイズから「リモート容量」「スクショの個人情報混入リスク」(ユーザー) に変わった

## 2026-08-21: 今後の evidence 媒体の運用 (決定)

- SwiftPM 配信形の切り替えで clone サイズの制約が消えたため、論点を「リモート容量」と「スクショの個人情報」に置き直した (ユーザー)
- 容量: archive 時に媒体を削除するか退避するか → 媒体は commit 済みで履歴から復元できるため退避は二重、削除 (trash) で十分。実行主体は ksn-distill Step 6 (現状は mv 1 行で差し込み口なし) → Kasane 本体の変更
- 個人情報: archive 時の削除では履歴に残り、画像は secret scanner で検出できない → 撮影・保存時の規律が唯一の防波堤。全プロジェクト共通なので Kasane 本体の規約 (ui-artifacts.md / ksn-ui / 実機証跡を撮るスキル / 画像を保存する議論系スキル) に置く (ユーザー)
- Kasane リポジトリには起票の仕組みがないため、依頼プロンプトの作成をゴールとした → artifacts/kasane-request-evidence-media.md
- ADR: 該当せず (ルールの正は Kasane 側。プロジェクト側は config 設定のみ)

## 2026-08-21: ローカル絶対パスの扱い

- 種別の再集計: 長命層 12 行 (KsDialogs / AiForms / MAUI ソースの未公開ローカルクローン参照)、docs 9 行 (`file://` リンク含む)、changes の自己参照 864 行、changes のその他 22 行 (`KSN_COUNTERPART_META` 12・AiForms 6 等)、openspec 109 行 (凍結)。ソース側のヒットはすべて未追跡 (ただし `hs_err_pid*.log` は .gitignore 未登録)
- 選択肢: A) 全部直す / B) 長命層 + docs のみ / C) 放置。採用 A — 864 行は接頭辞除去 1 回で片付き、手作業は 20 行程度。他リポジトリは `<リポジトリ名>:<相対パス>` 表記、`KSN_COUNTERPART_META` 行は session_id ごと削除、docs は docs-refresh 経由、openspec は編集禁止のため論点④へ
- 派生: 再発防止 (lint / 規約) をユーザーが要望 → 新しい論点として追加
- ADR: 該当せず (表記規約は再発防止の論点で扱い、置き場が Kasane 側なら依頼プロンプトへ)

## 2026-08-21: ローカル絶対パスの再発防止

- 既存資産: `.claude/hooks/comment-policy-check.py` (PreToolUse Write / Edit) と `scripts/comment-policy-lint.py` (同ルールの一括 lint) の 2 段構えがあり、Kasane の lessons 規約も「機械的に検査できるものは lint / hook / CI へ」
- 選択肢: 3 層 (hook + CI lint + Kasane 規約・発生源修正・ksn-init 同梱) / hook + CI のみ / Kasane 規約のみ。採用 3 層 — 書く瞬間に止め、漏れは CI、癖は規約で。worktree 内の作業にも hook が効く
- 他リポジトリの表記は当初 `<リポジトリ名>:<相対パス>` を提案したが、ユーザー指示で `../<リポジトリ名>/<相対パス>` (同階層 clone 前提、開発環境の約束事として成立とみなす) に変更。理由: エージェントが実際に辿れる形でないと参照の意味がない。論点③の決定事項にも反映
- ADR: 該当せず (規約の正は Kasane 側。プロジェクト側は hook / lint が規則そのもの)

## 2026-08-21: 体裁と公開対象の範囲

- 事実: LICENSE は MIT (kamusoft)。README は MAUI 行のみ「開発中」、配信手順なし、private 言及なし。`openspec/` 4.4 MB はローカルパス 109 行を含み、ADR 16 ファイルの出典が参照。archive 文書 (媒体除く) 6.4 MB
- openspec/ の選択肢: A) 含める + 機械置換を凍結の例外として適用 / B) 含めてパスはそのまま / C) 外す。採用 A — C は ADR の出典が旧 private にしか無くなる、B は未公開クローンのパス 96 行が残る
- README: 状態表記の追加を docs-refresh に同梱する案を出したが、ユーザーより「README は大幅変更予定 (英語 README + README_ja の原典運用を踏襲、docs-refresh 再編、ガイドの Skills 化) で、別フェーズで議論すべき」→ このフェーズでは README に触らない。ロードマップ改訂の TODO へ
- ユーザー指示: 使わない `.claude/commands/opsx` と `.claude/skills/openspec-*` は削除してよい → 実施作業の TODO へ
- 他 (archive 文書・`.claude/` 等を含める、LICENSE そのまま) は推奨どおり
- ADR: 該当せず

## 2026-08-21: public 化の実施手順

- 事実: `kamusoft` は Organization、gh は repo + workflow scope。旧 repo は private・既定 develop・ブランチ develop / main・Issues 有効。ローカルは develop が origin より 10 コミット先行、`claude/*` 2 本は upstream なし (1 本は未マージ)、stash・追加 worktree なし
- 名前の選択肢: A) 新 repo が `kamusoft/KsSettingsView` を引き継ぎ旧を rename → archive / B) 旧はそのまま、新は別名。採用 A — 公開 URL を製品名・SwiftPM 配信 repo 名と揃える。同名作成でリダイレクトが新 repo を指す罠は push 完了後の rename と remote の明示付け替えで回避
- ローカルの扱い (ユーザー質問への回答): ディレクトリ名 `KsSettingsView` は新 public の作業コピーが引き継ぐ (`../<リポジトリ名>/` 規約・Claude Code のパス紐づけ・他リポジトリからの参照のため)。旧クローンは `KsSettingsView-private-archive` へ退避して安全網に
- 既定ブランチは develop のまま。新 repo は private で作って検査・目視後に public へ切り替える。Secret scanning + Push protection を ON
- ADR: 新規なし。cross/ADR-0021 の Decision に「既存名を引き継ぎ、旧は rename + archive」を追記

## 2026-08-21: agenda の整理 (TODO 15 件で分割トリガー)

- 出口判定: 別ロードマップ → 該当なし (全項目が public 化の範囲内) / ksn-split → 該当なし (実施手順は一続きで分けても 1 change 単位にならない) / agenda 整理 → 採用
- 実施手順の個別ステップを artifacts/publish-procedure.md (チェックリスト + 実施記録欄) へ退避し、TODO を 5 件に圧縮
- 論点は空。research フェーズのため次は手順書に沿った実施 → 実施記録 → ksn-roadmap で research 完了をマーク。hook / lint の実装だけはコード変更なので S 級 change として ksn-orchestrator (独立レビュー) で行う

## 2026-08-23: ローカル絶対パス除去の実施

- 手順書 1 節「ローカル絶対パスの除去」を実施 (598 件 / 112 ファイル)。結果は [publish-procedure.md](artifacts/publish-procedure.md) の実施記録に記載、lint は 0 件
- 論点④ (`openspec/` の凍結例外) は決定どおり適用。オーナー判断「内容の改変ではなく個人情報の除去なので特例」を再確認して実施
- 決定の改訂 1 件: concepts の aiforms-origin-reference の表はローカルパス列を落とす予定だったが、`AiForms.Maui.NativeCollectionView` はリモートが無く列を落とすと参照先を示す手段が消えるため、`../<リポジトリ名>` 表記で列を残す形に変更 (オーナー承認済み)
- 点検時に把握していなかった形が 2 つ見つかった: `.claude/worktrees/<name>/` を含む自己参照 260 件 (剥がしてリポジトリ相対に)、`Projects/maui` の MAUI 本体クローン 3 行 (同一親ディレクトリ前提が成り立たないためリポジトリ名での記述に置換)

## 2026-08-23: 端末識別子の扱い (論点追加)

- オーナー指摘「生ログ系 *.log も端末情報が乗る可能性がある。.gitignore で除外した方がよいのでは」から派生した新論点
- 調査で前提が覆った: 追跡中の `.log` は 1 件だけで中身は端末情報を含まない要約。生ログは `.txt` で入っており、識別子は `.md` と `.sh` にも散っていた。拡張子ベースの `.gitignore` では取り切れない
- 選択肢: A) 値をマスクして証跡は残す / B) 生ログを削除し今後も追跡しない / C) 公開ツリーからのみ除外。採用 **A** (オーナー判断)。B・C は `.md` 側の識別子が残るため単独では不十分
- 再発防止の置き場は `.gitignore` ではなく lint とした。`.gitignore` は既存の追跡ファイルに効かず、`.md` に貼られた生ログも防げないため
- `pixie4` (端末の呼び名) は残置と判断。個人を特定せず、証跡間の対応付けラベルとして機能している

## 2026-08-30: Issue の質問窓口 (phase-9 申し送り 2、論点⑦)

- 発端: phase-9 が `.github/ISSUE_TEMPLATE/config.yml` を `blank_issues_enabled: false` / `contact_links: []` で設置したため、Issue 作成画面の選択肢がバグ報告と提案の 2 本だけになり、質問の行き先が消えた。cross/ADR-0024 が Consequences に負の帰結として記録し、窓口の要否を phase-2 の論点として残していた
- 制約: 本フェーズの決定「体裁と公開対象の範囲」と実施手順書 3 節が Discussions を OFF と確定しており、窓口を Discussions で置くならその見直しを伴う
- 選択肢: A) Discussions を開いて Q&A で受ける (`contact_links` から誘導) / C) 質問用 Issue Form を 3 本目に置く / D) 窓口を置かない。`contact_links` 単独は飛ばす先が Discussions しかないため A に含めた
- 軸: 質問者が辿り着けるか / 雑な投稿の抑止 (必須項目を強制できるか) / 巡回面の数 / 同じ質問の再来への効き / 可逆性 / 既存決定への影響
- 採用 **C** — 巡回を Issues 1 面に保て、Issue Forms の必須項目で ADR-0024 の抑止方針 (動かした証拠の必須化) がそのまま効く。質問が増えたら後から Discussions を開いて Issue を変換できるが、逆は既存スレッドが読めなくなるため、開かない側から始める。A は必須項目を強制できず面が 2 つに増える。D は正当な質問がバグ報告テンプレートへ流れ込み、結局オーナーの巡回負荷になる
- 波及: 決定事項が 11 件になったため、agenda の決定事項を箇条書きから h3 の小節形式へ組み替えた (ksn-core doc-structure)
- ADR: cross/ADR-0024 の Decision「Issue テンプレートは用途別 2 本」と食い違うため処理を検討。選択肢は A) 最小改訂 / B) supersede する新 ADR / C) ADR に触れない。採用 **A** — 決定の本体 (外部 PR を受けず Issue で受ける、抑止は動かした証拠の必須化) は覆っておらず変わったのは受け口の本数だけ。B は「0024 は覆った」という誤ったシグナルを残し、生きている決定 (PR collaborators only・CONTRIBUTING の置き場・言語方針) を新 ADR へ複写することになる。C は accepted ADR と実体の drift を残す。改訂表記は cross/ADR-0022 の前例 (インラインの `(YYYY-MM-DD 改訂: ...)` + 出典行の追加) に倣い、`date` と status は据え置き

## 2026-08-30: 英語 README のスクリーンショットの言語 (phase-9 申し送り 3、論点⑧)

- 調査で範囲が縮んだ: `assets/` の 4 枚は iOS / Android × Modern / Classic の**すべてが同じ 1 画面** (「Section 装飾デモ (style 切替)」) で、写る文字列の定義元は各 platform 4 ファイル・十数個だった。1 画面を英語にすれば 4 枚すべてが片付く
- 一方 Sample 全体では Android 221 / iOS 254 個 (ユニーク 181 / 214) の日本語 UI 文字列が Kotlin / Swift にハードコードされており、ローカライズ基盤は両 platform とも未整備 (`strings.xml` は `app_name` 1 件のみ、iOS は `.strings` / `.xcstrings` なし)
- 選択肢: A′) 該当画面をロケール対応にして英語で撮り直す / A) 該当画面を英語ハードコードへ置換 / C) Sample 全体をロケール対応 / D) 現状のまま公開
- 軸: 英語 README の第一印象 / オーナーの目視確認で日本語が残るか / Sample 実行時の英語話者の体験 / 作業量 / 残る半端さ / その後の広げ方
- 採用 **A′** — 1 画面のロケール対応で README の問題が丸ごと解け、オーナーの実機目視確認では日本語のまま見られる (CJK の折り返しと文字幅を確かめる価値を失わない)。A はこの画面でその確認ができなくなり、C は 475 文字列規模で public 化の関門としては重すぎる。D は既知の欠陥を抱えたまま公開することになる
- 波及: `samples/` のコード変更のため、public 化の前に S 級 change を 1 本挟む (ローカルパス hook / lint と同じ扱い)。撮り直した 4 枚はオーナー承認を経て `assets/` を差し替える
- ADR: 該当せず (選別3基準のいずれも通らない — `samples/` に閉じた可逆な判断。「以後 Sample の文字列は同じ仕組みに乗せる」は決定事項の中に書き、ADR には上げない)

## 2026-08-30: `maui/spike/` の公開可否 (phase-9 申し送り 4、論点⑨)

- 事実: 追跡 45 ファイル・564 KB (ディスク上の 788 MB はすべて gitignore 済みの生成物)。`maui/*.slnx` に含まれず通常のビルドで一度もビルドされない。長命層からの参照は cross/ADR-0023 の例外記述のみ
- 調査で前提が動いた 3 点: (1) iOS の手法 (`XcodeProject` + `CreateNativeReference=false` + `_RegisterXcodeProjectNativeReference`) は phase-9 の移送で `concepts/maui/architecture/binding-build-integration.md` に入っている (2) Android は maui/ADR-0006 が `AndroidGradleProject` を却下して `gradlew` 直接実行を採ったため、spike の Android 半分は本番が採らなかった方式の実装 (3) spike README の「BG8401 の transform は置かない」は本番の `Transforms/Metadata.xml` と逆で、docs-refresh の追従対象外なので今後もズレたまま
- 選択肢: A) 公開ツリーから外す / B) そのまま公開 / C) README を直して公開
- 軸: 長命の知識の保全 / 本番との食い違い / 再現手順の所在 / 時間による腐り方 / 作業量 / 公開リポジトリの見え方
- 採用 **A** — 残すべき知識は蒸留済みで spike 固有の価値はもう無い。公開すると誤った結論と却下済み方式が追従の仕組みなしに残り、slnx 外なので腐り続ける。phase-9 が懸念した「再現手順が失われる」は、旧 private が cross/ADR-0021 の凍結保管先として機能しているため起きない (archive の evidence 媒体 181 MB と同じ扱い)
- ADR: cross/ADR-0023 が「`maui/spike/` 自体を公開リポジトリに載せるかは public 化フェーズの論点とする」と宿題を残していたため、ADR-0024 と同じ最小改訂で決着を追記 (この処理方針は同日の論点⑦でオーナーが選択済み)

## 2026-08-30: スクリーンショット英語化の方式を撤回 (論点⑧の改訂)

- 発端: ksn-propose で design.md を起草中、[Sample のプラットフォーム間一致](../../../../concepts/cross/conventions/sample-parity.md) (cross/ADR-0016) との衝突を検出した。同規約は `samples/ios` / `samples/android` / `samples/maui` の 3 つで表示文言を一字一句一致させることを要求しており、MAUI Sample にも同じ画面が同じ日本語タイトルで存在する (`samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs`)。iOS / Android だけロケール対応にすると英語ロケールで文言一致が崩れる
- 上位層違反として自己修正せずオーナーへ提示。選択肢は A) MAUI も含めて 3 platform ロケール対応 / B) 2 platform + 「片側先行」として追随を追跡 / C) 3 platform 英語ハードコード
- **オーナー判断で方式そのものを撤回** — 「ただスクショを撮るだけなので、該当画面をハードコードで英訳、スクショとって元に戻すで良い。大袈裟なことはしなくて良い」
- 結果: `samples/` に恒久差分を残さないため、committed 状態では 3 platform の文言一致が保たれ、パリティ規約との衝突は**そもそも発生しない**。ロケール基盤も MAUI 追随義務も不要になった
- 対価として、英語 README のスクリーンショットが Sample のどの状態にも対応しなくなる点を決定事項に明記した。撮影に使った英訳文言は change の `ui/brief.md` に記録し、撮り直しを再現可能にする
- 波及: change の対象能力が 4 → 2 (repository-docs / docs-refresh) に縮小。設計判断が残らないため design.md は作らない

## 2026-08-30: iOS Sample の開発チーム識別子 (論点⑩)

- 発端: 実施手順書の続きに入る前に決定事項と [点検結果](artifacts/scan-2026-08-21.md) を突き合わせたところ、総括が「作業ツリーの修正で済むもの」に挙げた `DEVELOPMENT_TEAM` (個人 Apple Developer アカウントの値) だけが決定にも手順書にも落ちていないことが判明した
- 事実: `samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj` の Debug / Release 各 1 行に残存。識別子 lint (`scripts/identity-lint.py`) はこのパターン自体を持つが、検査範囲 (`kasane/config.yaml` の `lint.identity.scope` = kasane / openspec / skills) が `samples/` を含まないため素通りしていた。手順書 1 節の「lint で 0 件確認」を通っても検出されない
- 選択肢: A) 値を空にする / B) 追跡外ファイルへ外出し / C) そのまま公開
- 軸: 個人アカウント識別子の公開 / 他人が clone → 実機ビルドの体験 / オーナー手元での再混入 / 再発防止の担保 / 作業量
- 採用 **A** — 2 行で消せて、実在しないチームが指定されているより空の方が clone した人の署名解決がすんなり通る。B は書き戻りを構造で断てる代わりに、Sample を実機で動かす人 (オーナー自身を含む) 全員に設定ファイルの用意を課す。C は消すコストが極小なのに残す理由が薄い
- 再混入 (Xcode が実機ビルド時に値を書き戻す) は書き込み hook では止められない (Xcode が直接書くため)。公開ツリーは手順書 2 節で一度組んで終わりなので公開の瞬間の担保で足り、以後は phase-3 の CI lint で捕まえる。lint の検査範囲に `samples` を追加する作業は phase-3 の CI 整備時 (ソース中の正当な UUID 定数の誤検出確認を伴うため)
- ADR: 該当せず (`samples/` に閉じた可逆な判断。選別3基準のいずれも通らない。論点⑧と同じ整理)

## 2026-08-30: public 化の実施と research 完了

- 手順書 1〜4 節をこのセッションで実施した。1 節 (開発チーム識別子・push・検査 3 種) → 2 節 (公開ツリー 2265 件 / 19.2 MB を単一 commit) → 3 節 (新 repo を public 化・設定・旧 repo を Archive) → 4 節 (ローカル切り替え・3 platform ビルド確認)。各段の結果は [実施手順書](artifacts/publish-procedure.md) の実施記録に記録した
- 3 節は不可逆な操作を含むため、public 切替の直前でオーナーの目視確認を挟んだ。`gh repo rename` はエージェントの実行分類器にブロックされたためオーナーが手で実行し、以降の GitHub 操作はエージェントから実行できた
- 途中で 1 つ設計判断が要った: 旧 repo の rename 直後にローカル remote を実 URL へ固定する順序変更 (手順書では 4 節の作業)。同名の新 repo を作るとリダイレクトが解除され、既存クローンの `origin` が新 repo を指すため、誤 push で全履歴が公開側へ入る経路をこの時点で塞いだ
- Kasane 側の反映 (オーナー実施) を確認した。`distill.archive-media` が既定 `delete` で新設されたため、当初想定した「プロジェクト側で config に opt-in を書く」作業は不要になった
- **ADR**: 「開発ハーネスの記録を公開リポジトリに含める」判断を cross/ADR-0021 へ最小改訂で追記した (Decision に公開範囲、Consequences に「今後の change 記録もすべて公開される」制約と lint / hook がその担保である旨。status は proposed のまま)。選択肢は A) ADR-0021 に最小改訂 / B) 新規 ADR / C) ADR 化しない で、採用 **A** — ADR-0021 が既にこれを前提にしており (Consequences の「経緯は kasane/changes/archive・kasane/decisions に残る」)、決定として明示されていなかっただけのため
- **research 完了**: roadmap.md の phase-2 を completed にし、後続 (phase-3) へ 2 論点を申し送った
