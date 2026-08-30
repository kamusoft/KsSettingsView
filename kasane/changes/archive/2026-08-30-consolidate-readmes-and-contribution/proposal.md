# Proposal: consolidate-readmes-and-contribution

## Why

public 化 (package-distribution phase-2) の前に、公開リポジトリの**利用者向け入口**と**貢献者向け入口**を整える。現状は次の状態にある。

- README が 8 枚あり、ルートは日本語のみ。公開 OSS の顔として英語話者が読めず、インストール手順の節も存在しない。
- ルート以外の README 5 枚 (`android/` `maui/` `samples/*3`) には読者が実在しない。[AGENTS.md](../../../AGENTS.md) はエージェントの知識参照先を `kasane/concepts/` とコード・テストに限定しており platform README を挙げていない。オーナーも読んでいない。一方 docs-refresh は 8 枚を追従対象に抱えており、英語化すれば最大 14 枚に増える。
- 知識の正が README に滞留している。`kasane/concepts/maui/api/native-bridge.md` が binding 構成とビルド手順について「正は `maui/README.md`」と 2 箇所で書いており、「知識の正は concepts とコード・テスト」の原則が破れている。
- 外部貢献の受け付け方が未定で、public 化すると PR が無制限に届く。AI 生成の粗雑な提案 (AI スロップ) の流入とレビュー負荷が読めない。
- `skills/` の iOS 配布座標が仮名 `KsSettingsView-Swift` のまま。配信リポジトリ名が未確定だったための仮置きで、確定値への追従が残っている。

設計は [cross/ADR-0023](../../decisions/cross/0023-readme-root-only-and-developer-knowledge-in-concepts.md) (README はルート 2 枚、開発者向け知識は concepts に一本化) と [cross/ADR-0024](../../decisions/cross/0024-contributions-via-issues-no-external-pull-requests.md) (貢献は Issue で受け外部 PR は受け付けない) で確定済み。配信リポジトリ名は [cross/ADR-0018](../../decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md) に `KsSettingsView-SPM` として追記済み。いずれも proposed で、accepted への昇格は蒸留時に行う。

## What Changes

- **ルート README を英日 2 枚で新規作成する**: 英語 `README.md` + 日本語 `README_ja.md`。節構成は 概要 + 主な特徴 / スクリーンショット / 対応プラットフォーム / インストール / 最小コード例 / Skills / リポジトリ構成 / 貢献 / ライセンス・サードパーティ通知、冒頭に「配信準備中」バナー 1 行。インストール節は 3 platform の依存宣言 (座標) のみを置き、詳細な導入手順は `skills/` に委ねる。最小コード例は Skills の最小動作コードと同一にする。2 枚は翻訳ロックステップで扱う。
- **スクリーンショットを新設する**: iOS / Android × Modern / Classic の 4 枚 (横 2 列 × 縦 2 行) を「主な特徴」の直後に置く。リポジトリルートに `assets/` を新設して画像を置き、英日 README から同じ 1 セットを参照する (キャプションのみ言語別)。撮影はシミュレータ / エミュレータで行い、端末固有情報が写らないようにする。MAUI は Native をラップし見た目が同じになるため画像では示さず 1 行で補足する。
- **ルート以外の README 5 枚を廃止する**: `android/README.md`・`maui/README.md`・`samples/{ios,android,maui}/README.md`。
- **廃止する README の中身を移送する**: MAUI binding の内部割り込み知識と既知の制約 → `kasane/concepts/maui/`。環境セットアップ手順・実機目視確認チェックリスト → `kasane/concepts/cross/conventions/`。検証ホストの起動と期待表示 → `kasane/concepts/maui/`。サードパーティ通知 (Material Symbols / Apache 2.0) → ルート README。`native-bridge.md` の「正は `maui/README.md`」参照 2 箇所を解消する。他所に既にある内容 (モジュール構成・利用アプリ側の前提・基本のビルド / テストコマンド・ディレクトリ構成・`SDK location not found` の対処) は捨てる。
- **`.github/` 一式を新設する**: Issue Forms 2 本 (`.github/ISSUE_TEMPLATE/bug_report.yml` / `feature_request.yml`、英語 1 セット、証拠となる項目を必須化) と `.github/CONTRIBUTING.md` (英日 2 枚)。ルート README に「貢献」節 (3〜4 行) を置く。
- **docs-refresh の対象定義を変更する** (`.agents/skills/docs-refresh/SKILL.md` と `skills/.manifest.json`): 追従対象の README を 8 枚から 4 枚 (`skills/README.md`・`skills/README_ja.md`・ルート 2 枚) へ。コード正の機械チェックは**①モジュール一覧と② Sample デモ画面一覧をともに廃止**し、③ツール最低バージョンの 1 種だけを残す — ①の突合先 (ルート README のモジュール表・`android/README.md`・`maui/README.md`) と②の突合先 (`samples/*/README.md`) がいずれも存在しなくなるため。廃止は機械チェックの表だけでなく、追従対象の表・Step 3d・Step 4 の実行例・README 委譲プロンプト (5b) のモジュール表確認指示・整合性チェック・完了サマリの全箇所に及ぶ。
- **`skills/` の iOS 配布座標を修正する**: `skills/{en,ja}/kssettingsview-ios/SKILL.md` の仮名 `KsSettingsView-Swift` を確定値 `KsSettingsView-SPM` へ (各 3 箇所)。

- **`public-identifiers.md` の禁止事項 2 項目を削除する**: 「composite build が解決する開発用 GAV を公開済みの配布座標と説明しない」「実装のない MAUI product / package ID を現在利用可能な識別子として列挙しない」。いずれも未公開であることを理由にした記述制限で、配信開始後は無意味になる一方、それまでは公開前提で書く方針 (cross/ADR-0022 の Skill 導入節・本変更の README インストール節) と衝突する。phase-12 では deviation で握ったが、パッケージング (phase-4/5/6) とリリース (phase-8) でも再発するため、規約側を正す。

影響する能力: repository-docs (新規)、docs-refresh。

## Non-Goals

- **GitHub の Pull requests 設定を collaborators only にする** — リポジトリ設定の操作であり成果物のコミットではないため、phase-2 の実施手順書で行う (ADR-0024 に決定は記録済み)。
- **`public-identifiers.md` の artifactId 規則の改訂** — android/ADR-0016 の単一 artifact 化に伴う追随で、その実装フェーズ (phase-5) の責務。本変更では README に ADR の確定値を書くにとどめる。
- **配信リポジトリ `KsSettingsView-SPM` の作成** — phase-4 の責務。本変更では README と skills に名前を書くだけ。
- **`verification/` への最小コード例の配置と CI ビルド** — phase-7 の成果物が存在しないため。
- **`skills/` の Skill 構成の変更** (Skill の増減・references の再編) — cross/ADR-0022 により別の変更フローを要する。本変更で触るのは iOS 配布座標の値のみ。
- **`maui/spike/README.md` の廃止と `maui/spike/` の存廃** — 完了済み検証 (binding toolchain の疎通) の記録で docs-refresh の追従対象にも入っていない。README だけ消すと再現手順が失われ、ディレクトリごとの削除はコード資産の削除で本変更のスコープを超える。公開リポジトリに載せるかは phase-2 の論点として申し送る (ADR-0023 に例外として明記済み)。
- **README 内容の継続的な追従更新** — 本変更の後は docs-refresh の責務。

## Impact

- **破壊的変更**: README 5 枚を削除する。リポジトリは現在 private で、外部からのリンクは存在しない。リポジトリ内からの参照 (ルート README の 3 リンク、`samples/maui/README.md` の相互リンク、docs-refresh の対象一覧) はすべて本変更で解消する。
- **知識の移送**: concepts に開発手順を置くのは既存の前例 (`cross/conventions/test-execution.md`・`runtime-behavior-verification.md`) に沿うが、concepts の性格が「契約のみ」から広がる。ADR-0023 の Consequences に記録済み。
- **docs-refresh**: 追従対象が 4 枚になり、デモ画面一覧の照合検査が消える。Sample のデモ画面が増えても機械検出されなくなる (README 自体が無くなるため対象消滅)。
- **リポジトリへの画像追加**: `assets/` に PNG 4 枚が入る。UI が変わったら撮り直しが要る (docs-refresh の追従対象ではなく、UI 変更時の判断)。
- **リスク**: 移送の取りこぼし。廃止前に 5 枚の全節を分類表と突き合わせる手順を tasks に含める。

## 級: L

repository-docs (新規) と docs-refresh の 2 能力を横断し、長命層 (concepts の移送と規約改訂) にも及ぶ。当初 M と判定したが、spec-review (second-opinion-spec-001) の指摘を受けて L へ引き上げた — design.md に書くべき実質 (移送先の対応表・配布座標の正の優先順位・docs-refresh の廃止範囲の全列挙) が生じたため。

domain: cross
roadmap: package-distribution/phase-9-docs
