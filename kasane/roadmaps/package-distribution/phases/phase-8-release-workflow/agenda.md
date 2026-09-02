# phase-8-release-workflow

`workflow_dispatch` で version を入力して起動し、全 platform のビルド・テスト → 消費者 dry-run → Maven Central / NuGet.org への publish → SwiftPM 配信リポジトリへのスナップショット push + tag → monorepo の tag + GitHub Release の順で進む release workflow を新設し、初回リリースを行う (cross/ADR-0020)。

## 論点

- dispatch 入力の semver 検証と prerelease ポリシー (`X.Y.Z-{alpha|beta|rc}.N` のみ許可。NuGet / SwiftPM は自動で prerelease 扱い、Maven は同格で出ることを README に明記)
- job 構成と `needs` (phase-3 の job 再利用、publish 段の直列順序: Portal upload (保留) → NuGet push → Portal release → tag + Release)
- Central Portal の upload → release を CI から操作する方法 (vanniktech plugin の automaticRelease 設定 / Portal API)
- NuGet.org の Trusted Publishing (GitHub Actions OIDC) の利用可否 (`nuget-trusted-publishing` スキルで確認)。不可なら API key の保管方針
- secrets / GitHub Environment の設計 (GPG 秘密鍵・パスフレーズ、Portal トークン) と手動設定の手順書、dispatch を起動できるブランチ・人の制限
- version 注入の配線 (`-Pversion=` / `-p:Version=`) と、SwiftPM 側 (配信リポジトリへのスナップショット commit + 同じ version の tag) の整合
- 配信リポジトリへの push の publish 順序上の位置 (tag 削除で取り消せるため Maven release の前後どちらに置くか) と、書き込み secret (deploy key / PAT) の Environment 配置 (phase-4 の決定を引き継ぐ)
- GitHub Release ノートの生成方法 (自動生成 / CHANGELOG)
- 失敗時の再実行性: 既に publish 済みの version の扱い (NuGet は再 push 不可、Maven は上書き不可) と途中再開の可否
- 初回リリースの version (`0.1.0` か `1.0.0-beta.1` か)
- README (英語 / `README_ja`、phase-9 で public 前に作成) の「未配信」状態表記の解除と version 記載の更新を、初回リリースの手順に含める (docs-refresh 経由)
- KsDialogs への逆流を見据えた workflow の汎用化の度合い

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
- [ ] **NuGet.org Trusted Publishing の設定完了** (2026-09-02): 論点「Trusted Publishing の利用可否」は「可」で解消し、nuget.org の Trusted Publisher Policy をオーナーが作成済み (Active)。内容は Package Owner `kamusoft` / GitHub Actions / Repository Owner `kamusoft` / Repository `KsSettingsView` / Workflow File `release.yml` / Environment `release` / Scopes: Push new packages and package versions のみ (Unlist は付与しない) / Glob `KsSettingsView.*`。API key は保管しない。release workflow 側の前提: ファイル名は `.github/workflows/release.yml` とし (NuGet を別 workflow に分けると決めた場合はポリシーの Workflow File を Manage から編集する)、publish ジョブに `environment: release` と `permissions: id-token: write` を宣言し、push 前に `NuGet/login@v1` (`user:` に nuget.org のユーザー名を渡す。メールではない) で得た `NUGET_API_KEY` 出力を `--api-key` に渡す (`--skip-duplicate` 併用)。GitHub 側の `release` Environment の作成と `NUGET_USER` secret の登録は未実施で、Environment 設計の論点と合わせて行う。ポリシーは repo 単位なので KsDialogs には別ポリシー (Repository `KsDialogs` / Glob `KsDialogs.*`) を作る
- [ ] **phase-5 からの申し送り・発行用の認証情報の準備状況** (2026-09-01): Central Portal User Token は発行済み (オーナー保管)。GPG 署名鍵はオーナーのローカルで生成し公開鍵の公開まで完了済み。RSA 4096 / 無期限 / プライマリキー自身が署名鍵 (`[SC]`・サブキーなし — Sonatype が「Maven/Nexus はプライマリキーでのみ署名を検証する」と明記しているため意図的にこの構成)。fingerprint は `85EDDCDA8CF524FB5C4CA3C154DAFFF896DB9B8F` で、`signingInMemoryKeyId` に入れる短い ID は `96DB9B8F`。公開鍵は keyserver.ubuntu.com と keys.openpgp.org へ送信し、両方から HTTP で取得できることを確認済み (openpgp.org はメール未検証だと UID を剥がして鍵本体のみ公開する仕様。Maven Central の検証は fingerprint で引ければ通るため支障なし)。`signingInMemoryKey` の中身 (`gpg --export-secret-keys --armor` の全文) は平文の秘密鍵をディスクに残さないため Secrets 登録の直前に export する方針。失効証明書は鍵生成時に自動生成されオーナーが保管。secrets の置き場所 (repository / Environment) が本フェーズの論点として未解消のため GitHub への登録は未実施
- [ ] **前提知識** (2026-09-01): `mavenCentralUsername` / `mavenCentralPassword` に入れるのは Portal のログイン資格情報ではなく **User Token のペア** (vanniktech plugin の公式が明記)。トークンは https://central.sonatype.com/usertoken で発行し、生成時のモーダルを閉じると二度と再表示できないためオーナーが保管する (2026-09-01 発行済み。失った場合は再生成が必要)
- [ ] **phase-4 からの申し送り** (2026-09-01): 配信リポジトリ `KsSettingsView-SPM` は作成済み (初回スナップショット push 済み・tag なし)。release workflow は `scripts/spm-snapshot/sync-snapshot.sh <作業コピー>` でファイル配置し、commit / tag / push は workflow 側で行う (スクリプトは git 非操作・4 段の事前検証つき)。書き込み用 deploy key の作成と monorepo のリポジトリ単位 secrets (例: `SPM_DEPLOY_KEY`) への登録もここで実施する ([phase-4 の実装結果](../phase-4-ios-packaging/agenda.md))
- [ ] **phase-9 からの申し送り** (2026-08-30): ルート README 2 枚の冒頭にある「配信準備中」バナー 1 行を初回リリース時に削除する。cross/ADR-0023 の決定により未配信を示す記述はこの 1 箇所だけに集約してあり、インストール節は公開レジストリに存在する前提で書かれている (解除は 2 ファイル 1 行ずつ)
- [ ] **phase-3 からの申し送り** (2026-08-31): `main` ブランチを作成するとき、作成と同時に branch protection を設定する。内容は `develop` と同じ — 検証 CI の 4 job (`ios / verify` / `android / verify` / `maui / verify` / `lint`) を必須 status check とし、pull_request 経由を必須にする (承認数 0)。force-push 禁止・削除禁止を付け、admin バイパスは緊急時の逃げ道として許容する。phase-3 の決定事項「必須チェック化と通知」は `develop`・`main` 両方を対象としていたが、`main` が存在しないため `develop` のみで実施した ([phase-3 の実装結果](../phase-3-verification-ci/agenda.md))。設定は `gh api -X PUT` が保護設定を全体置換する点に注意し、既存設定を含む完全な payload を送る (実例: [branch-protection-develop.txt](../../../../changes/archive/2026-08-31-add-verification-ci/evidence/branch-protection-develop.txt))
