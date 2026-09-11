# phase-2-public-readiness

リポジトリを public 化する前に機密情報・個人情報の混入を作業ツリーと git 履歴の両方で点検し、問題がなければ public へ切り替える。

## 論点

(すべて決定事項へ移動済み — 2026-08-30)

## 素材

- [点検結果 (2026-08-21)](artifacts/scan-2026-08-21.md) — ksn-scout による作業ツリー + 全履歴の機密情報・個人情報点検
- [Kasane への依頼プロンプト: 媒体の運用とローカルパス](artifacts/kasane-request-evidence-media.md) — archive 時の媒体削除 (ksn-distill)・撮影時の個人情報規律・成果物のパス規約と標準 hook
- [public 化の実施手順書](artifacts/publish-procedure.md) — 決定事項を実行順に並べたチェックリスト (実施記録欄つき)

## 決定事項

### 履歴スキャンの手段と対象 (2026-08-21)

ksn-scout による grep 点検 (パターン + 機密ファイル名 + メール + ローカルパス + 個人識別子 + .gitignore、作業ツリーと全履歴) を正式な点検とし、エントロピー検出の穴は gitleaks 8.30.1 (`gitleaks git --redact`、HEAD と `--all`) で埋めた。結果は秘密情報 0 件。trufflehog は秘密値 0 件のため見送り。gitleaks は phase-3 の検証 CI (PR ごとの secret scan) へ流用する。詳細は [点検結果](artifacts/scan-2026-08-21.md)

### 履歴の扱い (2026-08-21)

public は新規リポジトリに現ツリーを単一 initial commit で公開し、開発の本籍を移す。旧 private は履歴保管で凍結、ミラー同期はしない (ADR-0018 の却下案「配布用ミラー」とは別物)。filter-repo は不採用。→ [cross/ADR-0021](../../../../decisions/cross/0021-public-repository-fresh-start.md) (proposed)

### 公開対象の範囲 — evidence 媒体 (2026-08-21)

公開ツリー (新 repo の initial commit) から `kasane/changes/archive/**` 配下の媒体 (PNG / MOV / MP4、623 件・181 MB) を外し、`.md` 文書は残す。媒体は旧 private リポジトリに保全する。Git LFS は公開 repo の帯域枠とライブラリ配布の相性が悪く不採用。ADR 化は「今後の evidence 媒体の運用」と合わせて判断。SwiftPM 配信形の変更後も維持 (理由は clone サイズから「リモート容量の無駄」と「スクショの個人情報混入リスク」へ)

### SwiftPM の配信形 (2026-08-21)

monorepo のルート Package.swift ではなく、SwiftPM 専用の配信リポジトリへ release CI が `ios/` のスナップショットを commit + tag する方式に切り替える。cross/ADR-0018 を改訂 (proposed のまま)。ADR-0001 への例外は不要に。ロードマップ本体 (全体図の phase-4 ラベル、phase-4 / 7 / 8 の agenda) への反映は ksn-roadmap (改訂) で行う

### 今後の evidence 媒体の運用 (2026-08-21)

進行中の変更では媒体を全部追跡する (worktree へ仕様を渡すため)。容量対策は ksn-distill の archive 時に媒体を削除 (trash) する手順を Kasane 本体に追加し、config で opt-in する。個人情報対策は archive 時では遅い (履歴に残る) ため、撮影・保存時の規律として Kasane 本体の規約 (ui-artifacts.md / ksn-ui 等) に置く。どちらも全プロジェクト共通なので Kasane 側で行い、本フェーズの成果物は [依頼プロンプト](artifacts/kasane-request-evidence-media.md)。ADR 化しない (ルールの正は Kasane 側)。**2026-08-30 反映**: Kasane 側は `distill.archive-media` を新設して既定を `delete` としたため、当初想定した「プロジェクト側で config に opt-in を書く」作業は不要になった (無効化したいときだけ `keep` を書く)

### ローカル絶対パスの扱い (2026-08-21)

公開ツリーに `/Volumes/` `/Users/` を残さない。自己参照 864 行は接頭辞 `/Volumes/.../KsSettingsView/` の一括除去でリポジトリ相対に、他リポジトリ参照 (長命層 12 行 + changes 7 行) は `../<リポジトリ名>/<リポジトリ相対パス>` 表記に (同階層に clone されている開発環境の前提。エージェントが辿れる形) (concepts の aiforms-origin-reference はローカルパス列を落とし GitHub URL 列のみ)、`KSN_COUNTERPART_META` 行 (14 ファイル) は行ごと削除、`~/Library` `~/.agents` の 3 行は手で修正。`docs/legacy-aiforms-reference.md` は phase-12 で `docs/` ごと廃止されるため修正しない (2026-08-21 改訂)。`openspec/` は編集禁止のため論点④で扱う。concepts の aiforms-origin-reference はローカルパス列を落とさず `../<リポジトリ名>` 表記へ変更する (2026-08-23 改訂。NativeCollectionView はリモートが無く、列を落とすと参照先を示す手段が消えるため)。`.gitignore` に `hs_err_pid*.log` を追加。実施は新 repo の initial commit 前

### ローカル絶対パスの再発防止 (2026-08-21)

3 層で置く。(1) 書き込み時 hook — `.claude/hooks/local-path-check.py` を PreToolUse (Write / Edit) に追加し `/Volumes/<名前>` `/Users/<名前>/` を含む書き込みをブロック (`<USER>` 等の例示は除外、判定はリポジトリ相対パスの第 1 セグメントで行い `.claude/worktrees/` 配下も対象)。(2) CI lint — hook とルールを共有する `scripts/local-path-lint.py` を phase-3 の検証 CI で gitleaks と並べて実行。(3) Kasane 側 — ワーカースキルの出力規約 (パスはリポジトリ相対、他リポジトリは `../<リポジトリ名>/<相対パス>`)、ksn-counterpart の `KSN_COUNTERPART_META` 出力の修正、ksn-init への標準 hook 同梱 → [依頼プロンプト](artifacts/kasane-request-evidence-media.md) の依頼 3。ADR 化しない (規約の正は Kasane 側、プロジェクト側は hook / lint が規則そのもの)

### 体裁と公開対象の範囲 (2026-08-21)

公開ツリーは現構成をそのまま (`openspec/`・`kasane/changes/archive` の文書・`.claude/` `.codex/` `.mcp.json` `kasane/lessons/` を含む)。`openspec/` は ADR 16 ファイルの出典が参照するため含め、ローカルパス 109 行は論点③と同じ機械置換を**凍結の例外として 1 回だけ**適用 (意味不変)。LICENSE (MIT、kamusoft) はそのまま。**README はこのフェーズでは触らない** (「開発中」表記も現状維持) — README 英語化 + README_ja、docs-refresh の再編、ガイドの Skills 化は別フェーズで議論 (ksn-roadmap 改訂へ)。使わない `.claude/commands/opsx` と `.claude/skills/openspec-*` は public 化の実施作業で trash する

### 端末識別子の扱い (2026-08-23)

検証証跡に混入した作業環境の個体識別子 (Android 実機シリアル・iOS Simulator UDID・相方 CLI の session id・ANR ErrorId) は、ファイルごと除外せず**値をプレースホルダにマスクして証跡は残す**。理由は、識別子が生ログの `.txt` だけでなく `.md` (evidence.md / NOTES.md / verification-report.md / tasks.md / design.md) と `.sh` にも散っており、拡張子ベースの `.gitignore` では取り切れないため。再発防止は `.gitignore` ではなく lint (`scripts/device-id-lint.py`、対象は kasane/ openspec/ docs/) で担保する — `.gitignore` は既存の追跡ファイルに効かず、`.md` に貼られた生ログも防げない。端末の呼び名 (`pixie4`) は個人を特定せず証跡間の対応付けラベルとして機能しているため残す

### public 化の実施手順 (2026-08-21)

新 repo は `kamusoft/KsSettingsView` (既存名を引き継ぐ)、旧 private は `KsSettingsView-private-archive` へ rename → GitHub Archive。ローカルも旧クローンを `../KsSettingsView-private-archive` へ退避し、元のローカルクローン先のパスを新作業コピーが引き継ぐ (`../<リポジトリ名>/` 規約・Claude Code のパス紐づけ・他リポジトリからの参照を保つ)。既定ブランチは `develop` (main / release ブランチは phase-8 で判断)。順序: (1) 旧 private 上で下ごしらえを commit し、`develop` (origin より 10 先行) と `claude/*` 2 本を push、`user.email` を noreply へ → (2) 追跡ファイルから archive 媒体を除いた公開ツリーを組み単一 commit → (3) gitleaks + local-path-lint で 0 件確認 → (4) 新 repo を private で作成して push、目視後に public へ切替 → (5) GitHub 設定 (Issues ON / Wiki・Discussions・Projects OFF / Secret scanning + Push protection ON / Dependabot alerts ON / `develop` は force-push・削除禁止のみ。必須 status check は phase-3 後) → (6) 旧 repo rename + Archive、ローカル退避と新 clone、未追跡の開発ファイルは旧から複製 → (7) 後続 (Kasane 依頼・ksn-roadmap 改訂・docs-refresh) は手順の外。同名 repo 作成でリダイレクトが新 repo を指す罠は、(1) の push 完了後に rename し、(6) で remote を明示的に付け替えることで回避

### Issue の質問窓口 — 質問用フォームを 3 本目に置く (2026-08-30)

Issue Forms を **バグ報告 / 提案 / 質問の 3 本**にし、質問用フォーム (`.github/ISSUE_TEMPLATE/question.yml`、英語 1 本) を新設する。**GitHub Discussions は開かない** — 決定「体裁と公開対象の範囲」と実施手順書 3 節の Discussions OFF は維持し、`blank_issues_enabled: false` と `contact_links: []` もそのままにする。

- 窓口を Issues 1 面に保つため。Discussions を開くと巡回先が 2 面に増える一方、質問の総量が読めない (未リリース・利用者ゼロ) 段階で面を先に増やす利得が薄い
- 質問フォームには Issue Forms の必須項目を効かせる (バージョン / platform / 試したこと / 参照した Skill・README の箇所)。cross/ADR-0024 が定めた「AI スロップの抑止は書式の厳密さではなく実際に動かした証拠の必須化で効かせる」を質問窓口にも適用する。Discussions のカテゴリテンプレートは本文の雛形にとどまり必須化ができないため、この抑止が効かない
- 可逆性の向き: 質問が増えたら後から Discussions を開き、既存 Issue を Discussion へ変換できる。逆に Discussions を開いてから閉じると既存スレッドが読めなくなるため、開かない側から始める
- 仕分けは `question` ラベルで行い、バグ報告・提案が質問に埋もれないようにする
- cross/ADR-0024 の Decision「Issue テンプレートは用途別 2 本」と、同 Consequences が phase-2 へ残した宿題 (「窓口を置くかは public 化フェーズの論点として残す」) は、この決定で決着する。ADR-0024 は最小改訂で追随済み (2026-08-30、Decision の本数と Consequences の宿題行。status は accepted のまま)

### 英語 README のスクリーンショットの言語 — 撮影時だけ英訳する (2026-08-30、同日改訂)

ルート README のスクリーンショット 4 枚に写る「Section 装飾デモ (style 切替)」画面の表示文字列を、**撮影のときだけ一時的に英語へ書き換えて撮影し、撮影後に元へ戻す**。`samples/` には恒久的な差分を残さず、ロケールリソースも導入しない。撮影に使った英訳文言は change の `ui/brief.md` に記録し、後日の撮り直しを再現可能にする。

- 4 枚は iOS / Android × Modern / Classic の**すべてが同じ 1 画面**なので、その画面だけで英語 README の第一印象の問題が丸ごと解ける
- **当初はロケール対応 (Android `strings.xml` + iOS String Catalog) を採ったが、同日オーナー判断で撤回した** — 目的がスクリーンショットの撮影だけであり、そのために恒久的なリソース基盤を持ち込むのは過剰。日本語も失わずに済む
- 撤回により [Sample のプラットフォーム間一致](../../../../handbook/cross/sample-parity.md) との衝突も消えた。ロケール対応案では英語ロケールで iOS / Android が英語・MAUI が日本語になり文言一致が崩れるため、MAUI を含めるか「片側先行」として追随を追跡するかの判断が必要だった。恒久差分を残さない本案では committed 状態で 3 platform の一致が保たれ、MAUI 追随の義務も生じない
- Sample 全体のローカライズ (Android 221 / iOS 254、ユニーク 181 / 214 の文字列) は当初から範囲外
- 英語キャプションに「日本語ロケールの Sample のスクショ」と断り書きを添える案は phase-9 で検討済み。解決済みに見えるため採らない
- **対価**: 英語 README のスクリーンショットは Sample のどの状態にも対応しなくなる (Sample を動かすと日本語で表示される)。撮影のためだけの一時改変という性質上避けられず、Sample を英語化しない選択の対価として受け入れる

### `maui/spike/` は公開リポジトリに載せない (2026-08-30)

`maui/spike/` (追跡 45 ファイル・564 KB) を**公開ツリーから外し、旧 private リポジトリに保全する**。cross/ADR-0021 により新 repo へ開発の本籍が移るため、今後の作業ツリーからも消える。手順書 2 節の除外リスト (従来は archive 媒体のみ) に `maui/spike/**` を追加する。

- **長命の知識は蒸留済み**: iOS の手法 (`XcodeProject` + `CreateNativeReference=false` + `_RegisterXcodeProjectNativeReference` による手動登録、MSBuild 18 の `MSB4120` 回避) は `kasane/concepts/maui/architecture/binding-build-integration.md` にある
- **Android 半分は却下済み方式の実装**: maui/ADR-0006 が `AndroidGradleProject` を却下して `gradlew` 直接実行を採ったため、spike の Android 側は本番が採らなかった経路を実装している
- **README の結論が本番と逆**: spike README は「BG8401 の transform は置かない」と結論しているが、本番は `maui/android/KsSettingsView.Binding.Android/Transforms/Metadata.xml` を持つ。この README は docs-refresh の追従対象外なので今後もズレたままになる
- **腐る一方**: `maui/*.slnx` に含まれず通常のビルドで一度もビルドされないため、.NET SDK が上がるたび静かに壊れていく。旧 private で凍結される方が状態として正しい
- phase-9 が懸念した「再現手順が失われる」は起きない。旧 private リポジトリは cross/ADR-0021 の凍結保管先として既に機能しており、archive の evidence 媒体 181 MB と同じ扱いになる

### iOS Sample の開発チーム識別子 (2026-08-30)

公開ツリーでは iOS Sample のプロジェクト設定 (`samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj` の `DEVELOPMENT_TEAM`、Debug / Release 各 1 行) に入っている個人 Apple Developer アカウントの値を**空にする**。追跡外ファイルへの外出しは採らない。

- 識別子自体は秘密情報ではない (配布アプリのバイナリからも読める類) が、消すコストが 2 行と極小で、他人が clone したときの体験もむしろ良くなる — 実在しないチームが指定されていると Xcode が解決に失敗して署名エラーになるが、空なら自分のチームを選ぶだけで済む
- 外出し案は書き戻りを構造的に断てる代わりに、Sample を実機で動かす人 (オーナー自身を含む) 全員に設定ファイルの用意を課すため採らない
- オーナーが Xcode で実機ビルドすると値は書き戻るが、公開ツリーは手順書 2 節で一度組んで終わりなので、公開の瞬間に入っていなければ足りる。以後は phase-3 の検証 CI で継続的に捕まえる
- **再発防止は lint 側で行う** — 識別子 lint (`scripts/identity-lint.py`) はこのパターン自体を持つが、検査範囲 (`kasane/config.yaml` の `lint.identity.scope`) が `kasane` / `openspec` / `skills` に限られており `samples/` を見ていないため、この値は素通りしていた。範囲に `samples` を追加する (実施は phase-3 の CI 整備時。ソース中の正当な UUID 定数の誤検出を確認してから)。書き込み hook では Xcode の直接書き込みを止められないため捕捉は lint が担う
- [点検結果 (2026-08-21)](artifacts/scan-2026-08-21.md) の総括が「作業ツリーの修正で済むもの」に挙げた 5 項目のうち、決定に落ちていなかった最後の 1 件。ADR 化しない (`samples/` に閉じた可逆な判断で、選別3基準のいずれも通らない)

## 実装結果 (2026-08-30 反映)

決定事項 3 件 (「Issue の質問窓口」「英語 README のスクリーンショットの言語」「`maui/spike/` は公開リポジトリに載せない」) を [changes/archive/2026-08-30-add-question-form-and-english-screenshots](../../../../changes/archive/2026-08-30-add-question-form-and-english-screenshots/proposal.md) で実装した (M 級、独立レビュー APPROVED、乖離なし)。

- **質問窓口**: `.github/ISSUE_TEMPLATE/question.yml` を新設し Issue Forms が 3 本になった。必須項目はバージョン / platform / 試したこと / 参照した Skill・README の箇所。`blank_issues_enabled: false` と `contact_links: []` は維持。CONTRIBUTING 英日にも質問段落を追加した
- **スクリーンショット**: ルート README の 4 枚を英語表示で撮り直した。撮影は Sample の表示文字列を一時的に英訳して行い、撮影後に revert したため `samples/` に恒久差分はない。使用した対訳表と撮影条件 (端末・解像度・ステータスバー統制コマンド) は change の `ui/brief.md` に残っており、後日の撮り直しはこれで再現できる。決定時の対訳から 1 件だけ変更: 画面タイトルは当初案が Android の `TopAppBar` で 2 行に折り返ったため `Section decoration (style switch)` へ短縮した (brief の「文字列が切れたら英訳を短くしてよい」規定に沿う)
- **`maui/spike/`**: リポジトリ内での削除は行っていない (公開ツリーを組む時の除外であり手順書 2 節の担当)。本 change では docs-refresh の `SKILL.md` から `maui/spike/README.md` への言及を削除し、公開リポジトリに存在しないファイルを指す記述を残さないようにした

### 申し送り

- **`question` ラベルの存在確認**: 新 repo 作成時に `bug` / `enhancement` / `question` の 3 つが揃っているかを確認する必要がある (GitHub 既定ラベルだが、Issue Forms の `labels:` は存在しないラベルを自動生成しない)。→ [実施手順書](artifacts/publish-procedure.md) 3 節に反映済み
- **`maui/spike/**` の公開ツリーからの除外**: → [実施手順書](artifacts/publish-procedure.md) 2 節の除外リストに反映済み
- **英語 README のスクリーンショットは Sample のどの状態にも対応しない**: Sample を実際に動かすと日本語で表示される。決定時に「Sample を英語化しない選択の対価」として受け入れ済みで、追加の受け皿は設けない (見送り)

## 調査結果 (2026-08-30 完了)

public 化を完了した。公開リポジトリは [kamusoft/KsSettingsView](https://github.com/kamusoft/KsSettingsView)、旧 private は `KsSettingsView-private-archive` として GitHub Archive 済み。

- **履歴を引き継がず新規リポジトリで公開した** (cross/ADR-0021)。点検 (grep + gitleaks) で秘密情報は 0 件だったが、個人メールとローカル絶対パスが全履歴に残るため、現ツリーを単一 initial commit として本籍を移した
- **公開ツリーは 2265 件 / 19.2 MB** — 追跡 2933 件から archive 媒体 623 件 (180.1 MB) と `maui/spike/` 45 件を除外した。開発ハーネスの記録 (`kasane/` `openspec/` `.claude/` `.codex/`) は含めている
- **公開前提の規律を仕組みに落とした**: ローカルパス lint・識別子 lint・書き込み hook を新設し、撮影・保存時の個人情報規律と archive 時の媒体削除は Kasane 本体へ移した (2026-08-30 反映済み、`distill.archive-media` は既定 `delete`)
- 実施の全過程と検査結果は [実施手順書](artifacts/publish-procedure.md) の「実施記録」節に残っている

### 後続フェーズへの影響

**phase-3 (検証 CI)** へ 2 つの論点として申し送った ([agenda](../phase-3-verification-ci/agenda.md) に反映済み) — 公開前提の検査 (gitleaks / 2 つの lint、識別子 lint の検査範囲拡張) の CI 化と、`develop` への必須 status check の追加。public リポジトリになったため GitHub Actions の標準ランナー (macOS 含む) が無料で使える点も phase-3 の前提として効く。

## TODO

- [x] 論点の解消 (2026-08-21、決定 9 件)
- [x] **[実施手順書](artifacts/publish-procedure.md) に沿って public 化を実施 (2026-08-30 完了)** — 1 節: 下ごしらえ (開発チーム識別子・push・検査 3 種 0 件) → 2 節: 公開ツリー 2265 件 / 19.2 MB を単一 commit → 3 節: 新 repo を public 化し旧 repo を Archive → 4 節: ローカル切り替えと 3 platform ビルド確認。hook / lint は S 級 change として実施済み
- [ ] 後続フロー (手順書 5 節): **cross/ADR-0021 のオーナー確認のみ残り** (proposed → accepted は蒸留時)。Kasane への依頼は 2026-08-30 反映済み、ksn-roadmap 改訂は 2026-08-21 実施済み
- [x] **着手条件** (2026-08-30 充足): phase-10〜12 (`docs/` 廃止・`skills/` 生成) と phase-9 (README 英語 + `README_ja`) の完了を待ってから手順書 2 節以降 (公開ツリーの作成) に進む。1 節の下ごしらえは先行可
- [x] **phase-3 への申し送り** (2026-08-30、[phase-3 の agenda](../phase-3-verification-ci/agenda.md) へ論点として反映済み): 識別子 lint の検査範囲に `samples` を追加 / gitleaks と 2 つの lint の CI 化 / 必須 status check の追加
- [x] 調査結果のまとめ (2026-08-30、手順書の「実施記録」節に 1〜2 節と 3〜4 節の結果を記入済み)
- [x] ksn-roadmap で research 完了をマーク (2026-08-30)
- [x] **phase-9 からの申し送り 1** (2026-08-30、cross/ADR-0024): 実施手順書 3 節へ「GitHub の Pull requests 設定を collaborators only にする」を追加 (2026-08-30 反映済み)。判断の余地がないため論点にしていない。`.github/` 一式 (Issue Forms 2 本 + `config.yml` + CONTRIBUTING 英日) は phase-9 で設置済み
- [x] **phase-9 からの申し送り 2〜4** (2026-08-30 決着): 論点⑦ (質問窓口)・⑧ (スクショの言語)・⑨ (`maui/spike/` の存廃) をすべて決定事項へ移動
- [x] 上記 3 件を 1 本の change にまとめて実装 (2026-08-30 完了、[changes/archive/2026-08-30-add-question-form-and-english-screenshots](../../../../changes/archive/2026-08-30-add-question-form-and-english-screenshots/proposal.md))。公開ツリーの作成 (手順書 2 節) より前に実施
