# Deviation: add-verification-ci

spec と実装の差分。実装フェーズ中に発生順で追記する (spec 本体への逆流修正はしない)。

## 2026-08-31 マージ保護の適用範囲を develop のみとする

- spec `verification-ci` Requirement「マージ保護」は「`develop` / `main` への変更の取り込みは、4 job すべての成功を必須 status check とする pull_request 経由 SHALL とする」と両ブランチを対象にし、tasks 5.1 / 5.2 も両ブランチへの設定と検査を指示している → **本 change では `develop` のみに設定し、`main` の保護は phase-8 (release workflow・初回リリース) へ申し送る** (オーナー裁定 2026-08-31)。理由: リポジトリに `main` ブランチが存在せず (デフォルトブランチは `develop`、ブランチは `develop` 1 本のみ)、classic branch protection はブランチ名で解決するため設定自体ができない。`main` への PR・push が発生し得ない現時点では、守られない期間の実害がない
- 申し送り先: `main` を作成するフェーズ (phase-8) が、作成と同時に同じ保護 (4 job 必須 status check + PR 必須、force-push 禁止・削除禁止、admin バイパス許容) を設定する
- Scenario「main への PR でも起動する」は影響を受けない — tasks 4.1 が確認方法を「workflow 定義の `branches` 指定で確認する」と既に指定しており、workflow 側は `main` を base とする pull_request トリガーを持つ実装のままとする

## 2026-08-31 gitleaks を action ではなく CLI で実行する

- tasks 3.2 は secret scan の実装手段を「gitleaks (action)」と括弧書きで示していた → **公式リリースの CLI をバージョン + SHA-256 固定でダウンロードして実行する形にした**。理由: `kamusoft` は Organization アカウントであり、gitleaks-action は organization 配下のリポジトリを scan する場合に license key の取得と `GITLEAKS_LICENSE` secret の登録を要求する。spec `verification-ci` Requirement「lint の検証」の要求は「secret scan (gitleaks)」であってツール実行形態を規定しておらず、CLI 方式でも満たせる。版とチェックサムを workflow に明示する形は Requirement「ツールチェーンの再現性」とも整合し、secret 依存を増やさない
- あわせて走査対象を作業ツリーではなく `git archive HEAD` で取り出した追跡内容とした。理由: 作業ツリー直走査では `samples/**/obj/` のビルド生成物を拾い、ローカル実測で 408MB・誤検出 55 件になる (追跡内容のみなら 19MB・誤検出 0 件)。追跡されていない生成物は PR にも公開リポジトリにも現れないため、検査対象として不要
