# public 化の実施手順書 (2026-08-21 確定)

phase-2 の決定事項 (agenda.md) を実行順に並べたチェックリスト。実施時はこのファイルのチェックを埋め、完了後に「実施記録」節へ結果 (日付・新 repo URL・検査結果) を書く。

前提: 点検結果は [scan-2026-08-21.md](scan-2026-08-21.md)。秘密情報 0 件。履歴は引き継がない (cross/ADR-0021)。**2 節以降は phase-10〜12 (`docs/` 廃止・`skills/` 生成) と phase-9 (README 英語 + `README_ja`) の完了後に行う** — 公開履歴に旧 `docs/` と旧 README を載せないため。1 節は先行してよい。

## 1. 旧 private リポジトリ上での下ごしらえ (commit して記録を残す)

- [x] `git config user.email` (local / global) を `muak@users.noreply.github.com` に切り替える (以後のコミットすべて) — 実施 2026-08-23
- [x] `.gitignore` に追加 (実施 2026-08-23): `*.jks` `*.keystore` `*.p12` `*.pfx` `*.pem` `*.mobileprovision` `google-services.json` `keystore.properties` `secrets.*` `.env.*` `hs_err_pid*.log`
- [x] ローカル絶対パスの除去 (論点③ / ④): 実施 2026-08-23
  - [x] 自己参照: `/Volumes/.../KsSettingsView/` の接頭辞を一括除去してリポジトリ相対に (約 864 行、`kasane/changes/**` 中心)
  - [x] 他リポジトリ参照: `/Volumes/.../<Repo>/...` → `../<Repo>/...` (長命層 12 行: cross/ADR-0018・0019・maui/ADR-0025 の出典行、concepts/cross/conventions/aiforms-origin-reference.md の表はローカルパス列を残し `../<リポジトリ名>` 表記へ (2026-08-23 改訂。NativeCollectionView はリモート無しのため列を落とすと参照先が消えるため)、roadmaps/maui-support の 6 行、package-distribution/exploration.md の 1 行。changes 内 7 行)
  - [x] `KSN_COUNTERPART_META` 行を含む `second-opinion-*.md` 14 ファイル: 当該行を削除 (session_id ごと)
  - [x] `~/Library` 2 行・`~/.agents` 1 行・`/Users/.../CCManagerRepos` (worktree パス) を手で修正
  - [x] `openspec/` 109 行: 同じ機械置換を**凍結の例外として 1 回だけ**適用 (意味不変。CLAUDE.md の編集禁止はこの 1 回のみ例外、history.md に記録済み)
  - [x] `docs/` は phase-12 で廃止されるため触らない (`kasane/config.yaml` の `paths.lint-exclude` に暫定登録。廃止時に除外も外す) (2 節の時点で存在しないこと、`skills/` に `/Volumes/` `/Users/` と端末識別子が無いことを両 lint で確認)
- [x] 再発防止 (論点⑥): 実施済み (commit 7670763) `.claude/hooks/local-path-check.py` (PreToolUse Write / Edit、`/Volumes/<名前>` `/Users/<名前>/` をブロック、`<USER>` 等の例示は除外、判定はリポジトリ相対パスの第 1 セグメント、`.claude/worktrees/` 配下も対象) と、同ルールの `scripts/local-path-lint.py` を作成し `.claude/settings.json` に hook を登録。**S 級 change として ksn-orchestrator で実装・独立レビュー**
- [x] 端末識別子のマスク (2026-08-23 追加の論点): 実機シリアル・Simulator UDID・session id・ErrorId をプレースホルダへ置換し、`scripts/device-id-lint.py` で 0 件を確認
- [x] 使わないスキル群を trash (実施 2026-08-23): `.claude/commands/opsx`、`.claude/skills/openspec-*` (11 個)、`.codex/skills/openspec-*` (11 個。手順書に記載漏れがありオーナー指摘で追加)
- [x] iOS Sample の開発チーム識別子を空にする (`samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj` の `DEVELOPMENT_TEAM`、Debug / Release 各 1 行。決定「iOS Sample の開発チーム識別子」)。**識別子 lint の検査範囲外なので次項の lint では検出されない** — 置換後に `grep -rn DEVELOPMENT_TEAM samples/` で直接 0 件を確認する
- [x] 上記を commit し、`develop` (origin より 100 コミット先行、2026-08-30 時点) と `claude/android-test-worker-c2-crash`・`claude/kasane-share-test-wait-helpers-8b3d17` を origin へ push (archive を完全な状態にする)
- [x] `scripts/local-path-lint.py`・`scripts/identity-lint.py` (旧 device-id-lint.py) と `gitleaks git --redact` を旧 repo で実行し 0 件を確認

## 2. 公開ツリーの作成 (単一 initial commit)

- [x] 旧 repo の `develop` で `git ls-files` を取り、次を除いた一覧で新ディレクトリへコピー
  - `kasane/changes/archive/**` の媒体 (png / jpg / gif / mov / mp4、623 件・181 MB)
  - `maui/spike/**` (追跡 45 ファイル・564 KB。決定「`maui/spike/` は公開リポジトリに載せない」)
- [x] 新ディレクトリで `git init -b develop` → 全追加 → 単一 commit (author は noreply、メッセージ例: `Initial public snapshot`)
- [x] 新ディレクトリで `gitleaks git --redact` と `local-path-lint.py`・`identity-lint.py` を実行し 0 件、`git status` で `.gitignore` の効きを確認、ツリー容量が約 20 MB であることを確認

## 3. GitHub: 新 repo の作成と公開

- [x] 旧 repo を `kamusoft/KsSettingsView-private-archive` へ rename (`gh repo rename`)。**手順 1 の push 完了後に行う**
- [x] 新 repo `kamusoft/KsSettingsView` を **private で**作成し、手順 2 のツリーを push (`develop`)
- [x] GitHub 上で中身を目視 (README・ツリー・ファイル数・容量) → visibility を **public** に切り替え
- [x] 設定 (gh api): Issues ON / Wiki OFF / **Discussions OFF** (質問窓口は Issue Form で置くと決定済み — 決定「Issue の質問窓口」) / Projects OFF、Actions 有効 (既定)、**Secret scanning + Push protection ON**、Dependabot alerts ON、`develop` の branch protection = force-push 禁止 + 削除禁止 (PR 必須・必須 status check は phase-3 の CI 後)
- [x] Issue のラベル `bug` / `enhancement` / `question` が存在することを確認する (いずれも GitHub の既定ラベル。Issue Forms の `labels:` は存在しないラベルを自動生成しないため、欠けていたら作成する)
- [x] **Pull requests を collaborators only にする** (Settings > Features)。外部からの PR を受け付けないため (cross/ADR-0024)。完全無効化を採らないのはオーナー自身の PR も作れなくなり phase-3 の PR トリガー CI が成立しないため
- [x] 旧 repo を GitHub Archive (読み取り専用) にする
- [x] 旧 repo の Issue 1 件・PR 11 件は引き継がない (cross/ADR-0021)。必要な Issue があれば手で転記

## 4. ローカルの切り替え

- [x] ローカルの既存クローンを `../KsSettingsView-private-archive` へ改名し、その remote URL を rename 後の URL へ更新 (安全網として保持)
- [x] 新 repo を元のクローン先パス (`../KsSettingsView`) へ clone (手順 2 のディレクトリを移動して remote を設定してもよい)
- [x] 未追跡の開発ファイル (`android/local.properties` `samples/android/local.properties` `.claude/settings.local.json` 等) を旧ディレクトリから複製。`DerivedData` / `build` / `.gradle` は再生成
- [ ] 3 platform のビルドが通ることを確認 (iOS: `swift build` / Android: `./gradlew assemble` / MAUI: `dotnet build`)
- [ ] Claude Code のメモリ・セッションが同じパスで引き継がれていることを確認

## 5. 後続 (この手順書の外、別フローで)

- [x] Kasane リポジトリへ [依頼プロンプト](kasane-request-evidence-media.md) を渡す (2026-08-30 反映済み)。**プロジェクト側の設定追加は不要だった** — Kasane 側は `distill.archive-media` を新設して既定を `delete` にしたため (当初の想定は「config で opt-in」)。ksn-distill が archive 時に媒体を trash し、撮影・保存時の個人情報規律は ksn-core `references/ui-artifacts.md`、生ログの sanitize と個体・個人・秘密の値の検査は同 `references/evidence.md` に入った
- [x] ksn-roadmap (改訂): SwiftPM 配信リポジトリ方式を反映、README 二本立ては phase-9 へ、docs 基盤の再編 (Skills 化) は phase-10〜12 として追加 (2026-08-21)
- [ ] cross/ADR-0021 のオーナー確認 (proposed → accepted は蒸留時)

## 実施記録

### 2026-08-23: 端末識別子のマスクと lint 追加

- 発端: オーナーから「changes 配下の生ログに端末情報が乗るので `.gitignore` で除外した方がよいのでは」との指摘
- 調査結果、拡張子ベースの除外では不十分と判明。追跡中の `.log` は 1 件のみでかつ端末情報を含まない要約 (`ios-test-constraints.log`)、実際の生ログは `.txt` で、識別子は `.md` と `.sh` にも散っていた (計 25 ファイル)
- マスク実施: 実機シリアル 20 / Simulator UDID 24 / 相方 CLI の session id 8 / ANR ErrorId 1 を `<android-device-serial>` `<ios-simulator-udid>` `<session-id>` `<error-id>` へ置換 (46 ファイル)。ソースコードは非改変 (`ios/` の UUID 定数・テスト fixture は正当なため対象外)
- 再発防止: `scripts/device-id-lint.py` を新設し、`.claude/settings.json` と `.codex/hooks.json` の PreToolUse へ登録。判定対象は kasane/ openspec/ docs/ に限る (ソースには正当な UUID 定数があるため)
- lint は検出できることを自己テストで確認済み (実値 3 種を検出、プレースホルダ・`$(...)` 変数展開・全ゼロ fixture は非検出)
- 残置: 端末の呼び名 `pixie4` (個人を特定せず、証跡間の対応付けラベルとして機能。ファイル名にも含まれる)、`/Library/Java/...` `/Applications/Android Studio.app/...` (ユーザー名を含まない標準パス)

### 2026-08-23: .gitignore 補強と不要スキル群の削除

- `.gitignore` に「機密情報 / 署名鍵」節を追加 (`*.jks` `*.keystore` `*.p12` `*.pfx` `*.pem` `*.mobileprovision` `google-services.json` `keystore.properties` `secrets.*` `.env.*` `hs_err_pid*.log`)。追加パターンに該当する追跡ファイルが無いこと、既存ファイルが新たに無視されないことを確認済み
- 不要スキル群を trash: `.claude/commands/opsx` 11 / `.claude/skills/openspec-*` 11 / `.codex/skills/openspec-*` 11 = 33 ファイル
- `.codex/skills` は手順書に記載漏れがあり、オーナー指摘で対象に加えた (codex 側にも同じ openspec スキル群が複製されていた)
- `.claude/skills/docs-refresh` (`.agents/skills/docs-refresh` への symlink) は残置し、実体の存在を確認済み
- 削除対象を参照する設定・コードは無し (`.claude/settings.json` `.codex/config.toml` `.codex/hooks.json` に記載なし。ヒットはロードマップの記述のみ)

### 2026-08-23: コミット author の切り替え

- `user.email` を local / global とも `muak@users.noreply.github.com` へ変更 (変更前の global は個人アドレス)。履歴に既存の 12 commit と同じ表記に揃えた
- `user.name` (実名) は点検の指摘対象外のため変更していない
- 既存 244 commit の author は個人アドレスのまま。cross/ADR-0021 のとおり公開ツリーは履歴を引き継がない (単一 initial commit) ため、履歴書き換えは不要

### 2026-08-23: ローカル絶対パスの除去

- 対象 598 件 / 112 ファイルを修正し、`scripts/local-path-lint.py` が exit 0 (違反 0 件) になることを確認
- 変更は 113 ファイル (+584 / -593)。`kasane/config.yaml` 以外はすべて Markdown で、ソースコードは非改変
- 内訳: `kasane/changes` 78 / `openspec/changes` 18 / `kasane/roadmaps` 11 / `kasane/decisions` 3 / `kasane/concepts` 1 / `openspec/drafts` 1
- worktree パスは `.claude/worktrees/<name>/` と `/Users/.../CCManagerRepos/KsSettingsView-*/` の両方を剥がしてリポジトリ相対にした (点検時に把握していたのは後者のみ)
- `dotnet/maui` のローカルクローン参照 3 行は、同一親ディレクトリ前提が成り立たない (`Projects/maui`) ため `../<リポジトリ名>/` にせずリポジトリ名での記述に置き換えた
- 手順書・点検結果・agenda の「パスを示すこと自体が目的の記述」は省略例示形 (`/Volumes/.../` `/Users/.../`) に変更。本手順書 4 節の `mv` も `../KsSettingsView-private-archive` 表記へ
- 決定との差異 1 件: 上記 concepts の表 (オーナー承認済み)
- 残 9 件は `docs/legacy-aiforms-reference.md` のみ。決定どおり未修正で `paths.lint-exclude` に登録

### 2026-08-30: 下ごしらえの仕上げと公開ツリーの作成 (1〜2 節)

**1 節 (完了)**

- iOS Sample の開発チーム識別子を空にした (Debug / Release 各 1 行)。自動署名 (`CODE_SIGN_STYLE = Automatic`) は維持しているので、clone した人は自分のチームを選ぶだけで実機ビルドできる。`xcodebuild -list` でプロジェクトが正しく読めること (Target / Build Configuration の列挙) を確認済み
- 検査 3 種すべて 0 件 — `local-path-lint.py` exit 0 / `identity-lint.py` exit 0 / `gitleaks git --redact` は 273 commit・22.35 MB を走査して no leaks found
- `develop` (origin より 102 commit 先行) と `claude/android-test-worker-c2-crash`・`claude/kasane-share-test-wait-helpers-8b3d17` を origin へ push。旧 repo はこれで完全な保管状態になった
- 他 platform の署名情報も確認し、追跡下に該当なし (Android の `storeFile` / `storePassword` 等、MAUI の `CodesignKey` / `CodesignProvision` はいずれも 0 件)

**2 節 (完了)**

- 除外の実測は決定どおり: archive 媒体 623 件 / 180.1 MB、`maui/spike/` 45 件 / 428 KB。追跡総数 2933 件のうち**公開ツリーに残るのは 2265 件 / 19.2 MB**
- 残る媒体は `assets/` の README スクリーンショット 4 枚のみ (archive 配下の媒体だけを外す条件が意図どおり効いている)
- 作成先は `../KsSettingsView-public-tree` (3 節で新 repo へ push し、4 節で `../KsSettingsView` へ移す)
- symlink 2 件 (`CLAUDE.md` → `AGENTS.md`、`.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh`) はリンクのままコピーし、リンク先が追跡下にあることも確認済み
- **`.gitignore` の効き**: `git add -A` で 2265 件すべてが追跡され、無視されたファイルは 0 件 (元 repo で追跡されていて新 repo で弾かれるファイルは無かった)
- 単一 commit `Initial public snapshot` を作成 (author は noreply)。作業ツリー 19.2 MB / `.git` 16 MB
- 公開ツリー上でも検査 3 種を再実行し、すべて 0 件
- **既知の帰結**: archive 媒体を外したことで画像リンクが 5 ファイル・10 件だけ壊れる (`2026-08-02-ios-picker-selection-parity/ui/brief.md`、`2026-08-11-fix-entrycell-writeback-caret-race/evidence.md` 他)。いずれも過去の change の証跡内で、決定「公開対象の範囲 — evidence 媒体」の想定内

### 2026-08-30: public 化の実施 (3〜4 節)

**3 節 — 新 repo の作成と公開 (完了)**

- 旧 repo を `kamusoft/KsSettingsView-private-archive` へ rename。**その直後にローカルの remote を rename 後の実 URL へ固定した** — 手順書では 4 節の作業だが、同名の新 repo を作るとリダイレクトが解除されて既存クローンの `origin` が新 repo を指すため、誤 push で全履歴が公開側へ入る経路をこの時点で塞いだ
- 新 repo `kamusoft/KsSettingsView` を private で作成 → 公開ツリーを push → オーナー目視 → public へ切替
- 説明文は README の Overview 1 文目を短縮した英文、topics は 10 個 (ios / android / dotnet-maui / swift / swiftui / kotlin / jetpack-compose / settings-screen / ui-library / cross-platform)。website は配布先が未確定のため空のまま (phase-8 で判断)
- 設定: Issues ON / Wiki OFF / Discussions OFF / Projects OFF (Projects は作成時の既定が ON だったため public 切替の前に OFF にした)、Secret scanning + Push protection ON、Dependabot alerts ON (204 で確認)、`develop` は force-push 禁止 + 削除禁止 (PR 必須・必須 status check は phase-3 の CI 後)
- **PR の collaborators only は `pull_request_creation_policy` フィールドで実現した** (値 `collaborators_only`)。PR 機能自体 (`has_pull_requests`) は有効なままなので、phase-3 の PR トリガー CI は成立する
- ラベル `bug` / `enhancement` / `question` は既定で存在したため作成不要だった
- **Issue の転記は不要と確定**: 唯一の Issue #12 (iOS の死経路整理) は NOT_PLANNED でクローズ済みで、指摘対象 (`supplementaryModes` / `makeListConfig` / `layoutModesDiffer`) は現在のコードに 1 件も残っていない。PR 11 件はすべて merged
- 旧 repo を GitHub Archive (読み取り専用) にした
- 実行制約: `gh repo rename` はエージェントの実行分類器にブロックされたためオーナーが手で実行した。以降の `gh repo create` / visibility 変更 / `gh repo archive` / `gh api` はエージェントから実行できた

**4 節 — ローカルの切り替え (完了)**

- ローカルクローンを入れ替え: 旧クローン → `../KsSettingsView-private-archive`、公開ツリー → 元のクローン先パス。`../<リポジトリ名>/` 規約と Claude Code のパス紐づけを保つため、新作業コピーが元のパスを引き継ぐ形にした
- 引き継いだ未追跡ファイル: `.claude/settings.local.json`、`android/local.properties`、`samples/android/local.properties`、`.claude/plans/` 9 件。JVM クラッシュログ・IDE 設定・空ファイルは引き継がず、`DerivedData` / `build` / `.gradle` は再生成に任せた
- 旧クローンの worktree 1 件 (detached HEAD) は未 commit の変更がなく、旧側に残置した
- **3 platform のビルドはすべて成功**: iOS `swift build` 12.0 秒 / Android `./gradlew assemble` 19 秒・214 タスク / MAUI `dotnet build maui/KsSettingsView.slnx` 1 分 44 秒・20 警告 0 エラー (警告は binding の既存 BG8605 / BG8606 / BG8A00)
- Claude Code のメモリ 14 件が同じパスで引き継がれていること、`local-path-lint.py` と `identity-lint.py` が新クローンで exit 0 になることを確認した
- 記録の残し方: 1〜2 節の実施記録までが公開スナップショットに入り、3〜4 節のこの記録は公開後の通常 commit として新 repo に入る
