# phase-8-release-workflow 議論履歴

## 2026-09-03: Central Portal の upload → release を CI から操作する方法

phase-8 の議論開始 (roadmap.md を in-progress に更新)。最初の論点として、publish 段の直列順序の前提になる Central Portal の 2 段操作を扱った。

scout の調査 (vanniktech plugin 0.37.0 のソース・Portal 公式ドキュメント) で判明した事実: plugin のタスクは `publishToMavenCentral` / `publishAndReleaseToMavenCentral` / `dropMavenCentralDeployment` の 3 つだけで、保留 deployment を後から release するタスクは存在しない。deployment ID は 0.37.0 で追加されたログ 1 行にしか出ず、Portal に一覧取得 API もない。後から release するには Publisher API (`POST /api/v1/publisher/deployment/<id>`、status は `POST .../status?id=`、drop は `DELETE`) を直接叩く。保留 deployment は 90 日で自動削除。公開済み version の再 upload は検証 FAILED、保留中の再 upload は別 deployment として並ぶ (未確認)。KsDialogs には release workflow も Portal の決定もなく、本リポジトリが先行事例になる。

選択肢:
- A: plugin で upload して保留 (VALIDATED 待ち)、ログから ID を拾い NuGet push 後に Portal API で release、失敗時は drop
- B: plugin の自動 release 1 段にし、順序を NuGet → Maven (upload + release) → tag に変えて ADR-0020 を改訂

採用: **A**。理由: ADR-0020 の順序をそのまま実現でき、NuGet push (不可逆) の時点で Maven 側の検証が済んでいることを plugin の既定が保証するため、不可逆操作の後に残る失敗要因が release API の呼び出しだけになる。代償のログ解析と API 呼び出しは数行の shell で済む。付随して status 再確認・失敗時 drop・ID 抽出失敗時の即時失敗を workflow に組み込む。

ADR: 新規起票はしない。cross/ADR-0020 の Consequences「Portal の 2 段階を CI から操作する必要がある」の具体化にあたるため、蒸留時に ADR-0020 へ追記する候補として扱う。

## 2026-09-03: 配信リポジトリへの push の publish 順序上の位置

書き込み secret の配置は phase-4 で決定済み (deploy key をリポジトリ単位 secrets へ) のため、順序上の位置だけを扱った。前提: iOS の公開は配信リポジトリの tag の瞬間で、commit の push だけでは公開されない。SwiftPM の push は tag 削除で取り消せる唯一の publish で、失敗要因は deploy key の認証と push 経路。

選択肢:
- P1: commit + tag を Maven release の後にまとめて push (1 ステップ。NuGet・Maven 公開後に push 失敗が起こり得る)
- P2: commit は publish 段の先頭、tag は最後の段 (2 ステップ。push 経路の失敗を不可逆操作の前に出し、公開瞬間を monorepo tag と揃える)
- P3: commit + tag を Maven upload 直後・NuGet より前にまとめて push (取り消せるものを先に。NuGet・Maven 失敗時は iOS だけ先に出て tag 削除が要る)

採用: **P2**。理由: 不可逆操作の前に push 経路を実証でき、lockstep が崩れる窓が構造的に消える。失敗時の後始末は不要 (未 tag の commit は公開されず次回上書き)。iOS の smoke (tag からの解決) の位置は job 構成の論点で決める。

## 2026-09-03: job 構成と needs の骨格

ここまでの決定 (Portal 2 段・SPM の commit / tag 分離) と phase-7 の申し送り (dry-run には publish する成果物そのものを artifact で渡す) を組み合わせ、validate → test ∥ package → dry-run → publish (直列 1 job) → smoke の 6 段を骨格として提示した。

選択肢:
- J1: test 段は phase-3 の verify-*.yml を無改修で呼び、version 注入つきの配布物生成を別の package 段 (3 job) に置く。test と package は並列
- J2: verify-*.yml に version 入力を足し、テストと配布物生成を同じ job で行う (job 数は減るが phase-3 の 3 本を改修し、直列で長くなる)

採用: **J1**。理由: phase-3 の workflow と branch protection の status check 名に触れない。version 注入はテスト結果に影響しない。「publish する成果物そのもの」を dry-run に渡すには独立した配布物生成 job がどのみち要る。Android の publish は署名の都合で再ビルドになる差を受け入れた。

(本文に mermaid 図を書いたところ描画されないと指摘を受け、表で書き直した。以後、本文の図は表かテキストにする)

## 2026-09-03: smoke 段の位置と反映待ち

phase-7 申し送り「smoke 失敗時の扱い (再実行・tag の遅延) を job 構成の論点に含める」を扱った。前提: Maven Central の反映は PUBLISHED から 10〜30 分、nuget.org も数分〜十数分かかる。phase-7 の smoke workflow 3 本は反映待ちを持たない (grep で確認)。ADR-0020 の tag の条件は publish 成功のみで smoke に触れていない。smoke 失敗時点で公開は取り消せない。

選択肢:
- S1: tag + Release → 反映待ち → smoke。tag の条件は publish 成功のみ。失敗は workflow の赤で知らせ smoke だけ再実行
- S2: SPM tag → 反映待ち → smoke → monorepo tag + Release。tag の条件に smoke を足す (ADR 追記)。失敗時は「公開済みだが tag が無い」状態を人が解消し、smoke の間 iOS だけ先行する窓ができる

採用: **S1**。理由: tag を止めても利用者を守れず説明しにくい状態が残るだけ。ADR-0020 をそのまま維持でき、再実行の単位が smoke job に閉じる。P2 (iOS の公開瞬間を monorepo tag と揃える) とも整合する。付随: 反映待ち job (ポーリング上限 45 分)、Release は publish 段で作ったまま。

## 2026-09-03: 失敗時の再実行性

前提: NuGet は `--skip-duplicate` で重複 push を警告扱いにできる。Maven Central は公開済み version の再 upload を FAILED にする。保留 deployment は失敗時 drop で消える前提。SPM commit は差分ゼロで skip でき、tag は同 commit なら成功扱いにできる。GitHub Actions の再実行は job 単位。ADR-0020 の Consequences は「失敗時は再実行するだけで後始末が要らない」。

選択肢:
- R1: publish 段の全ステップを冪等にして同 version で「失敗した job から再実行」
- R2: publish 済み version は再実行禁止、次の version で出し直す

採用: **R1**。理由: 部分 publish を同 version で埋められないと lockstep が崩れた欠番が残り ADR-0019・0020 に反する。ステップごとの存在検査の表を決定事項に置いた。Maven の公開確認 API は TODO で裏取り。

## 2026-09-03: secrets / Environment の設計と起動ブランチ

前提: secrets が要るのは publish 段の 1 job だけ。Trusted Publishing のポリシーが Environment `release` に固定済み。GitHub Environment は専用 secrets・deployment branch policy・required reviewers を持てる。phase-4 の「リポジトリ単位 secrets」の理由は organization secrets との対比で、Environment に置いても保たれる。`main` は未作成、Environment・secrets も未登録 (gh で確認)。

選択肢:
- E1: `main` を作り `main` からのみ起動。Environment の branch policy を main に限定。secrets 7 件を Environment へ
- E2: `develop` から起動し `main` は作らない (手順は短いが作業途中の commit からも起動できる)

採用: **E1**。理由: リリース対象 commit が main の先端に一意に定まり、誤起動が publish 手前で止まる。phase-3 申し送り (main の branch protection) がここで発火する。required reviewers は付けない。

合わせて agenda を整理: 解決済みの「Trusted Publishing の利用可否」(2026-09-02 に可で解消、TODO に置かれていた) と「version 注入の配線」(ADR-0020 追記と SPM 位置の決定で埋まった) を決定事項へ移した。

ADR: 新規起票はしない。「リリースは main からのみ」はブランチ運用の規範なので、蒸留時に handbook (ブランチ運用) へ書く候補とする。

## 2026-09-03: 初回リリースの version と semver 検証

semver 検証と prerelease ポリシーはロードマップ制約 (`X.Y.Z-{alpha|beta|rc}.N` に統一) の帰結として質問せず決定事項に記録した (validate の正規表現、suffix 時の Release prerelease 印、Maven 同格の README 説明)。

初回 version は `0.1.0` を推奨して提示したところ、オーナーから「ADR か何かで `1.0.0-beta.1` に決めたはず」と指摘を受けた。kasane 配下 (decisions / handbook / concepts / roadmaps / archive の proposal・exploration・design)、KsDialogs の kasane、openspec、git log を横断検索した。prerelease の扱いはロードマップの exploration.md「prerelease の扱い (2026-08-21 追記)」に記録済みで、初回 version の選択 (`0.1.0` か `1.0.0-beta.1` か) はそこが phase-8 の論点として送っていた。ADR には初回 version の決定はない。関連する痕跡として phase-2 の Issue テンプレートの version 欄の例示 `0.1.0-beta.1` がある。オーナーは続けて「プレリリースであることが分かるというのが要件にある」と明示し、exploration の記録を根拠として示した。要件そのものはこの exploration の節を出典に、本議論で初回 version の決定として確定した。

選択肢: V1 `0.1.0` / V4 `0.1.0-beta.1` / V2 `1.0.0-beta.1`

採用: **V4 `0.1.0-beta.1`**。理由: プレリリースであることが利用者に分かる (NuGet / SwiftPM で prerelease 扱い)。0.x で API 変更の余地も伝える。Issue テンプレートの例示と揃う。README のインストール例 (`0.1.0`) は初回リリース時の docs-refresh で `0.1.0-beta.1` に書き換える (SwiftPM は `from:` が prerelease を解決しないため `exact:` へ)。

## 2026-09-03: README の状態表記解除と version 記載の更新

README を実査したところ初回リリースで触る箇所はバナー 1 行 (2 枚) のほか、Maven / NuGet の未公開表記 (Publication status、2 枚 × 2 行) とインストール例の version (`0.1.0` × 3 platform × 2 枚) があり、facade の NuGet がルート README を同梱するため書き換えは pack より前に要ると分かった。

経緯 (3 回組み直した):
- D1 (具体 version をリリース PR の中で docs-refresh で更新) → オーナー「PR の中で docs-refresh は無理」で取り下げ
- D2 (プレースホルダ + バッジ + prerelease 説明文。README をリリース非依存に) と D4 (workflow が書き換えて main に commit) を提示 → オーナー「AGENTS.md の該当部分は緩めてよい」
- D1' (リリース PR で専用 script が置換、validate が check モードで一致検査、AGENTS.md に例外 1 行) と D4 を提示

採用: **D1'**。理由: 貼ってそのまま使え、行そのものから prerelease と分かる。人の PR に乗るので workflow に書き込み権限が要らず、ADR-0020 の「CI が bump commit を積まない」も保たれる。更新忘れは validate が止める。D4 は main への push 権限と branch protection の bypass が要るため却下。

## 2026-09-03: GitHub Release ノートの生成方法

前提: CHANGELOG は無く、変更の単位は Kasane の change と develop への PR。自動生成は tag 間のマージ済み PR 題名を列挙し `.github/release.yml` で分類できる。初回は前回 tag が無い。

選択肢: N1 自動生成 + release.yml 分類 (初回は手で補う) / N2 CHANGELOG ファイルを正にする

採用: **N1**。理由: 手で保守するファイルを増やさず proposal との二重管理を避ける。PR 題名を利用者向けに書く規律を提案化に含める。

## 2026-09-03: KsDialogs への逆流を見据えた汎用化の度合い

前提: KsDialogs はパッケージング未着手 (phase-11)、形態 4 つ (KMP 含む)、リリース CI はそのロードマップの非ゴール。共通なのは骨格と ADR 群 (cross/0008・0009 が翻案元)。

選択肢: G1 リポジトリ内に閉じ固有値を env に集約 (逆流はコピー) / G2 別リポジトリの共有 workflow を両者から uses

採用: **G1**。理由: 逆流先の形態が未確定で抽象の境界を当てられない。共有側の変更が両リリースに同時に効くのを避ける。

合わせて agenda の TODO を整理: 取り込み済みの申し送りを表にまとめ、phase-5 の認証情報の準備状況を artifacts/credentials-status.md へ退避、phase-6 申し送りの NU1507 / XA4301 を論点へ昇格した。

## 2026-09-03: NU1507 の恒久対処 (phase-6 申し送り)

前提: NU1507 は複数ソース環境でのみ出る (GitHub hosted ランナーの素の状態では出ない)。`maui/` に nuget.config は無く、phase-7 の消費者検証は自前の設定ファイルを使うため影響を受けない。

選択肢: (a) `maui/nuget.config` + packageSourceMapping / (b) `NoWarn`

採用: **(a)**。理由: 原因 (ソースの曖昧さ) ごと消え、restore 元が nuget.org に固定される。phase-7 と同型。

## 2026-09-03: XA4301 の恒久対処 (phase-6 申し送り)

scout の調査 (dotnet/android の targets・CreateAar.cs・docs、ローカルの生成 aar の実測): 除外の公式プロパティは存在しない。SDK は class library の自 assembly 用 aar に推移依存の `.so` を無条件で詰め nupkg に入れる (jar だけ `Pack=false` を見る非対称)。XA4301 は最初の 1 件を採用して 2 件目を捨てる無害な警告だが、版ずれ時にどちらが勝つかは未追跡。生成 aar の中身は `libandroidx.graphics.path.so` 4 ABI 分のみ。

選択肢: X1 生成 aar を nupkg から落とす + pack 時検査 / X2 README に既知事項として記す / X3 生成 aar の中身から `.so` を除く (内部ターゲットと増分キャッシュに依存、未検証)

採用: **X1**。理由: 失うものが無く警告 0 件、版ずれの余地も消える。非公式手段の代償は検査の同居で受ける。消費者検証で XA4301 だけを検出対象に加える。

これで agenda の論点は全件解消。次は ksn-propose (フェーズ由来入力)。

## 2026-09-03: default branch の切替 (提案化の中で決定)

proposal のドラフトで「default branch は develop のまま」を Non-Goals に置いたところ、オーナーから理由を問われた。示した理由 (README の絶対リンクが develop を指す) はリンクが壊れない以上根拠にならず、実際の考慮は (1) トップに見せるもの (main = リリース状態、NuGet 同梱 README と揃う)、(2) `blob/develop/` リンクの付け替え、(3) 新規 PR の base 既定が main になる注意、の 3 点と整理した。

採用: **切り替える (change に含める)**。README 2 枚のリンク (各 7 箇所)を `blob/main/` に付け替える (docs-refresh 依頼に含める)。
