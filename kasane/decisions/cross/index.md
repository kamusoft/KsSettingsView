# cross ADR 一覧

| ID | タイトル | status | 概要 |
|---:|---|---|---|
| [0001](0001-monorepo-platform-build-roots.md) | モノレポとプラットフォーム別ビルドルート | accepted | 横断変更を単一リポジトリで管理し、各プラットフォームは独立ビルドルートを持つ。 |
| [0002](0002-public-identifier-namespace.md) | 公開識別子の名前空間 | accepted | 所有ドメインを基礎に、各エコシステムの公開識別子を一貫して定める。 |
| [0014](0014-docs-as-user-facing-derived-artifact.md) | docs/ は利用者向け派生ドキュメントとして維持 | superseded by 0022 | 知識の正は concepts とコード・テスト。docs は docs-refresh が利用者向けに翻訳・追従させる派生物とする。 |
| [0015](0015-domain-axis-core-plus-platforms.md) | ドメイン軸を core + platform で導入 | accepted | domains (core/ios/android/maui) と domain-skills を定義し、長命層とスキル解決をドメイン分割する。 |
| [0016](0016-sample-cross-platform-parity.md) | Sample をプラットフォーム間パリティ検証装置と位置づける | accepted | Sample は全 platform で同一文言・同一画面構成とし、idiomatic な独自サンプルより厳密一致を優先する。 |
| [0017](0017-port-aiforms-to-native.md) | AiForms.Maui.SettingsView を Native ベースへ移植・リファインする | accepted | Native を主・MAUI を副とし、Native / KMP / MAUI のどのアプリ形態からも使える形で再構築する。互換 shim は提供しない。 |
| [0018](0018-distribution-public-channels-root-swiftpm-manifest.md) | 配布は公開レジストリの標準チャネルのみとし、SwiftPM は配信リポジトリで配る | accepted | SwiftPM / Maven Central / NuGet.org のみ、private 経路なし。SwiftPM は release CI が `ios/` のスナップショットを配信リポジトリへ commit + tag する (monorepo にルート Package.swift は置かない)。 |
| [0019](0019-lockstep-single-version.md) | 全 platform を lockstep の単一バージョンでリリースする | proposed | 全 artifact に同一 semver を付け git tag と一致させる。互換マトリクスは作らない。 |
| [0020](0020-release-dispatch-tag-last-version-injection.md) | リリースは version 入力の手動起動で行い、全 platform の publish 成功後に tag を打ち、version は CI が注入する | proposed | tag 先行で lockstep が壊れるのを防ぐ。version の SSoT は dispatch 入力 (= tag)、ファイルは開発用既定値。 |
| [0021](0021-public-repository-fresh-start.md) | public リポジトリは新規に作り、既存の private リポジトリの履歴は引き継がない | accepted | 現ツリーを単一 initial commit で公開し開発の本籍を移す。旧 private は履歴保管で凍結、ミラー同期はしない。公開ツリーには開発ハーネスの記録を含め、archive 媒体と `maui/spike/` だけ外す。 |
| [0022](0022-user-docs-as-agent-skills.md) | 利用者向けドキュメントは Agent Skills (skills/、en/ja 2 版) として提供 | accepted (supersedes 0014) | docs/ を廃止し platform 別 + 移行の 4 Skill × 2 言語をコピー利用で提供。知識の正は concepts、manifest の逆引きと網羅検査で追従。Skill は単体コピーの閉世界で自己完結させる。初期生成・構成変更は変更フロー、docs-refresh は追従専用。 |
| [0023](0023-readme-root-only-and-developer-knowledge-in-concepts.md) | README はルート 2 枚 (英語 + `README_ja`) に集約し、開発者向け知識は concepts に一本化 | accepted | ルート以外の README 5 枚を廃止し、開発者向けの正は concepts へ一本化。MAUI binding 知識・サードパーティ通知・サンプル実行手順は移送。docs-refresh の追随は 4 枚、デモ画面一覧の照合検査は廃止。maui/spike/ は公開リポジトリに載せない。 |
| [0024](0024-contributions-via-issues-no-external-pull-requests.md) | 貢献は Issue で受け、外部からの Pull Request は受け付けない | accepted | PR は collaborators only、貢献は Issue で受けオーナーが kasane change に起こす。Issue Forms 3 本 (バグ / 提案 / 質問) で証拠を必須化し exploration.md 前半へ写す。Discussions は開かない。表明先は README の節 + .github/CONTRIBUTING.md。 |
| [0025](0025-verification-ci-reusable-platform-workflows.md) | 検証 CI は platform 別の再利用可能 workflow と、それを呼ぶ入口で構成する | accepted | platform 別 workflow 3 本を `workflow_call` で定義し入口 1 本が呼ぶ。リリース用 workflow は検証を再定義せず同じ workflow を再利用する。変更パスによる絞り込みは行わず常に全 platform を検証し、status check 名は呼び出し側と呼ばれる側の双方で固定する。 |
| [0026](0026-ci-guarantee-logic-and-wiring-not-e2e.md) | CI が保証するのはロジックの全件通過と native への配線のコンパイルまでとし、実機・実行ホストでの検証は手元の手順に残す | accepted | 自動で成否が決まるテストは全件実行し (iOS は Simulator 対象)、実行 0 件はテストが緑でも失敗とする。実行ホストを人が見る検証は CI に載せない。利用者可視の変更への実機確認は CI とは独立に完了条件として残る。 |
| [0027](0027-negative-verification-fixed-wait-exception.md) | 負の検証は条件ベース待機の対象外とし、意図を明示した固定時間待機で書く | accepted | no-op・不達の確認には正の完了条件が存在しないため条件ベース化しない。意図を名前で明示したヘルパで書き、収束待ちの直し漏れと区別可能にする。収束待ちへの適用は引き続き規約違反。 |
| [0028](0028-ci-triggers-by-branch-role.md) | 検証 CI のトリガーはブランチの役割で分け、develop は直接 push、main は develop からの PR だけを検証する | proposed | develop 宛て PR トリガーを廃止し、develop push は lint + 本体検証 3 本、main 宛て PR はそれに消費者検証 3 本を加える。develop の必須 status check は撤去 (直接 push 運用に合わせる)、main は 7 件を維持。 |

欠番: 0012 (product-qualified-public-namespace) は移行トリアージで却下 (出典: kasane/changes/archive/2026-07-18-migrate-openspec/candidates/rejected-0012-product-qualified-public-namespace.md)。

番号は旧フラット時代の採番を温存 (採番規則は [../index.md](../index.md) を参照)。
