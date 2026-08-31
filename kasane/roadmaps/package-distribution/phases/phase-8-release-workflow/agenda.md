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
- [ ] **phase-9 からの申し送り** (2026-08-30): ルート README 2 枚の冒頭にある「配信準備中」バナー 1 行を初回リリース時に削除する。cross/ADR-0023 の決定により未配信を示す記述はこの 1 箇所だけに集約してあり、インストール節は公開レジストリに存在する前提で書かれている (解除は 2 ファイル 1 行ずつ)
- [ ] **phase-3 からの申し送り** (2026-08-31): `main` ブランチを作成するとき、作成と同時に branch protection を設定する。内容は `develop` と同じ — 検証 CI の 4 job (`ios / verify` / `android / verify` / `maui / verify` / `lint`) を必須 status check とし、pull_request 経由を必須にする (承認数 0)。force-push 禁止・削除禁止を付け、admin バイパスは緊急時の逃げ道として許容する。phase-3 の決定事項「必須チェック化と通知」は `develop`・`main` 両方を対象としていたが、`main` が存在しないため `develop` のみで実施した ([phase-3 の実装結果](../phase-3-verification-ci/agenda.md))。設定は `gh api -X PUT` が保護設定を全体置換する点に注意し、既存設定を含む完全な payload を送る (実例: [branch-protection-develop.txt](../../../../changes/archive/2026-08-31-add-verification-ci/evidence/branch-protection-develop.txt))
