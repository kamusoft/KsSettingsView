# Proposal: add-verification-ci

## Why

公開リポジトリになった KsSettingsView には PR / push を検証する CI が存在せず、3 platform のテスト・lint はローカル実行頼みである。公開後は他環境・他者からの PR も来るため、「develop / main に入るコードは機械検査済み」をリポジトリ側で保証する仕組みが要る。また後続のパッケージング (phase-4〜6) とリリース (phase-8) は、ここで定義する検証 job を土台として再利用する。

設計判断はフェーズ議論で決着済み ([agenda](../../roadmaps/package-distribution/phases/phase-3-verification-ci/agenda.md) の決定事項 10 件)。本提案はそれをアーティファクトに落とす。

## What Changes

- **GitHub Actions workflow の新設** (`.github/workflows/`):
  - platform 別 reusable workflow (`workflow_call`) 3 本 — ios / android / maui
  - CI 入口 workflow 1 本 — `develop` / `main` への pull_request と `develop` への push で 3 本 + lint job を起動 (paths フィルタなし、常時全検証)
- **ios job**: `macos-26` + Xcode 26.5 (メジャー.マイナーを workflow 変数で明示選択)。`xcodebuild test` を iOS Simulator destination で実行し全件を回す (`swift test` は使わない)。キャッシュなし (外部依存ゼロ)
- **android job**: ubuntu + `setup-java` Temurin 17 (daemon / toolchain とも 17)。キャッシュは Gradle 依存のみ (`build/` はキャッシュしない)。テスト実行件数を検査し、モジュール×variant 単位で 0 件があれば fail、合計件数を job summary に表示する
- **maui job**: `macos-26` + Xcode 26.5 選択 + `setup-java` 17 (binding ビルドの `android/gradlew` 用)。facade のユニットテスト (`dotnet test`、実行 0 件で fail) + facade platform TFM と binding 2 本のビルド。NuGet (`~/.nuget/packages`) をキャッシュ、workload は毎回インストール (workload set は既存の `global.json` `workloadVersion: 10.0.300.3` で固定済み — 本変更では触らない)
- **lint job**: ubuntu で `gitleaks` (secret scan)・`scripts/local-path-lint.py`・`scripts/identity-lint.py`・`scripts/comment-policy-lint.py` を実行
- **`kasane/config.yaml`**: `lint.identity.scope` に `samples` を追加 (試験実行で誤検出ゼロ確認済み)
- **branch protection (GitHub 設定操作)**: `develop`・`main` に必須 status check 4 job + PR 必須化 (admin バイパス可)。既存の force-push 禁止・削除禁止は維持

影響する能力: verification-ci (新設)

## Non-Goals

- **release workflow** — phase-8 の守備範囲 (本変更の reusable workflow を呼ぶ側)
- **検証ホスト実行 (E2E) の CI 化** — handbook/maui/integration-host-verification.md の手元手順のまま (agenda 決定: Simulator / Emulator 起動の時間と flakiness が見合わない)
- **workload / SDK ディレクトリ丸ごとキャッシュ等の高速化** — まず素直な構成で実測し、許容できない長さなら別 change で最適化 (agenda 決定)
- **KsDialogs への逆流** — ロードマップの非ゴール (KsDialogs 側 phase-11 の責務)

## Impact

- 破壊的変更なし。ライブラリのコード・テストには触れない
- 開発フローが変わる: `develop` / `main` への直 push が PR 必須化でブロックされる (admin バイパスは残る)
- リスク: 初回実装時まで各 job の所要時間が未実測 (iOS Simulator テストと MAUI workload install が長い見込み)。許容できなければ最適化は別 change
- `macos-26` イメージ内の Xcode 26.5 の正確な指定名は実装時に runner image マニフェストで確認する

## 級: M

1 能力 (検証 CI) の新設で、公開 API・アーキテクチャ・スキーマに触れず、設計判断は agenda で決着済みのため。3 platform 横断・GitHub 設定操作を理由とする L 昇格は spec レビュー (second-opinion-spec-001) で検討し、branch protection が可逆な運用設定であること・design.md が agenda の複写になることから M 維持をオーナーが裁定した (2026-08-31)。

domain: cross
roadmap: package-distribution/phase-3-verification-ci
