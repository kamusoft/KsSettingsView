# Design: add-consumer-verification

## Context

3 platform のパッケージング (SwiftPM 配信リポジトリ / Maven 発行構成 / NuGet pack) は整い、各フェーズで一時プロジェクトによる消費者検証を手で 1 回ずつ通した証跡がある。本変更はそれを `verification/` として永続化し、PR CI (dry-run) と release workflow (dry-run → publish → smoke、phase-8) から同じ手段で呼べるようにする。フェーズ議論 ([agenda](../../roadmaps/package-distribution/phases/phase-7-consumer-verification/agenda.md)) で検証範囲・構成・参照先・切り替え・CI への届け方・MAUI の検査・パリティ対象外の 7 件を決め、提案化のセカンドオピニオン ([second-opinion-spec-001.md](second-opinion-spec-001.md)) で参照先の排他性・キャッシュ・workflow 間の成果物受け渡しについて設計判断が追加された。本書はその設計判断を Decision 形式で残す。挙動の契約はデルタスペック、作業は tasks.md にある。

## Goals / Non-Goals

- Goals: 配布物を利用者と同じ経路で解決・ビルドできることを、リポジトリ内の消費者プロジェクトと CI で再実行可能にする。dry-run が公開レジストリやユーザー環境のキャッシュへ静かにフォールバックしない。README の最小例がビルドされ続ける。release workflow が publish 前 / 後に同じ workflow を呼べる
- Non-Goals: proposal.md の Non-Goals に同じ (起動・publish・実機、smoke 正ケースの実証、`main` の保護、XA4301 / NU1507、パリティ規約、`README_ja`)

## Decisions

### Decision 1: dry-run の参照先は本リポジトリ由来の座標について排他的にする

**採用案:** Android は `exclusiveContent { forRepository { mavenLocal() } filter { includeGroup("jp.kamusoft") } }` で `jp.kamusoft` を mavenLocal だけに割り当て、smoke では同じ形で mavenCentral に割り当てる。MAUI は packageSourceMapping で `KsSettingsView.*` をローカルフィード (smoke では nuget.org) に、`*` を nuget.org に写像する。iOS は `path:` 参照そのものが唯一の解決先。
**理由:** dry-run の価値は「ローカルに置いた配布物がそのまま解決できる」ことにあり、無いときに公開済みの版へフォールバックすると検証が偽陽性になる。排他割り当てなら解決失敗が必ず露見する。
**代替案:**
- **A: mavenLocal に `content { includeGroup }` を付けるだけ (当初案)** — Gradle の repository-level content filter は「このリポジトリはこの group を含みうる」の宣言であって排他ではなく、フィルタの無い mavenCentral も同じ group を検索する。公開済みの version を指定した負ケースが Central から解決されて通る (セカンドオピニオン指摘)。却下
- **B: dry-run では mavenCentral / nuget.org を外す (`<clear/>` + ローカルのみ)** — MAUI テンプレートの依存 14 件 (Xamarin.AndroidX.* / Microsoft.Extensions.*) は workload の library-packs に無く nuget.org からしか取れないため restore 自体が成立しない (phase-6 証跡 6-1)。Android も AndroidX / Compose を Google / Central から取る。却下

### Decision 2: MAUI は実行ごとに空のパッケージ展開先を使い、取得元をパッケージ単位で検査する

**採用案:** 消費者ビルドは実行ごとに新しい `RestorePackagesPath` (global packages folder を使わない) で restore し、`project.assets.json` から解決版、展開先の `<id>/<version>/.nupkg.metadata` の `source` から取得元を読んで facade と binding 2 件の一致と取得元を検査する。
**理由:** packageSourceMapping は global packages folder に既に展開されているパッケージには働かず、source lookup 自体が起きない。ユーザー環境や CI キャッシュに同じ version があるとローカルフィードが空でも restore が成功する。`project.assets.json` の `restore.sources` は構成したソース一覧で、実際の取得元ではない。
**代替案:**
- **A: mapping だけで構造的に保証されるとみなし事後検査を持たない (agenda の当初決定)** — 上記のとおり cache 迂回で保証が崩れる。却下し、agenda の決定に訂正を追記した
- **B: `--no-cache` / `--no-http-cache` の付与だけで済ませる** — http cache は無効化できるが global packages folder の展開済みパッケージは対象外。単独では不十分で、空の展開先の補助にしかならない。却下 (必要なら併用)

### Decision 3: 消費者検証の workflow は platform 別 3 本にし、publish 成果物は artifact 入力で受け取る

**採用案:** `verify-consumer-{ios,android,maui}.yml` を `workflow_call` で定義し、入力は `mode` (必須) / `version` (任意、smoke では必須) / `artifact` (任意)。`artifact` があれば download して参照先にし、無ければ job 内でフィード準備 (スナップショット配置 / `publishToMavenLocal` / `pack`) から行う。status check 名は `consumer-<platform> / verify`。
**理由:** GitHub Actions では呼び出し側の job と呼ばれた workflow の job は別 runner で、ファイルシステムを共有しない。release workflow の publish 段が作った成果物を消費者に渡すには artifact の upload / download が必要で、その契約を入力に持たせないと proposal の「準備段を publish 成果物で置き換える」が実現できない。platform 別に分けるのは cross/ADR-0025 の形 (platform 別 reusable workflow) に揃えるためで、release 側が platform ごとに publish 直後の smoke を呼ぶ構成とも噛み合う。
**代替案:**
- **A: 1 本の workflow に 3 platform の job を並べる (agenda の当初決定)** — 再利用契約としては成立するが、ADR-0025 の形と異なり、artifact も platform 別に渡すため 1 本にまとめる利点がない。却下し、agenda の決定に訂正を追記した
- **B: 参照先のパスだけを入力に取り、release 側が同じ runner で準備すると仮定する** — runner が別なので到達できない。却下

### Decision 4: version は dry-run では platform ごとの開発用既定値、smoke では必須

**採用案:** `version` 入力が空の dry-run は Android `0.1.0-SNAPSHOT`、MAUI `0.0.0-dev` (それぞれ本体の開発用既定値と同じ)、iOS は version を持たない。smoke は version 必須で、無ければフィード準備前に失敗する。version が与えられた dry-run (release の publish 前検証) は 3 platform とも同じ文字列で準備・解決する。
**理由:** 本体の開発用既定値は platform ごとに異なり (cross/ADR-0020 の「ファイルは開発用既定値」)、PR CI の dry-run に単一の version を渡すと少なくとも一方のローカル成果物と一致しない。smoke は実レジストリの tag / 座標を指すため version なしでは意味を持たない。
**代替案:**
- **A: 3 platform の開発用既定値を同じ文字列に統一する** — 本体側 (`libs.versions.toml` / `Directory.Build.props`) の既定値を変える変更が要り、消費者検証の都合で本体の宣言を動かすことになる。却下
- **B: PR CI でも常に version を明示して渡す** — 渡す値を ci.yml が持つことになり、本体の既定値との二重管理になる。却下

### Decision 5: 解決結果の証跡は platform ごとに取れるものを取る

**採用案:** iOS は dry-run では生成した `Package.swift` と依存グラフの表示 (`swift package show-dependencies` または xcodebuild の解決ログ) で path と identity を、smoke では `Package.resolved` で URL・revision・version を残す。Android は `dependencies --configuration releaseRuntimeClasspath` の `jp.kamusoft` 行。MAUI は Decision 2 の検査出力。CI では job summary に転記する。
**理由:** path 参照には version constraint が無く、依存がすべて path 参照のとき SwiftPM は pins を持たず `Package.resolved` を書かない (または除去する)。dry-run で `Package.resolved` を証跡にする要件は成立しない。
**代替案:**
- **A: 3 platform とも lock ファイル (`Package.resolved` / assets.json) を証跡にする (当初案)** — iOS dry-run で成立しない。却下

### Decision 6: 配信先への副作用の不在は、前後比較と権限の不在で示す

**採用案:** 手元の検証では実行前後で配信リポジトリの tag 一覧・Central Portal の deployments・nuget.org の `KsSettingsView.*` 一覧を比較して同一であることを残す。CI の消費者 job は `permissions: contents: read` のみで secrets を受け取らない構成にし、書き込み経路が存在しないことを workflow 定義で示す。
**理由:** 「tag 数 0」のような絶対値の検査は初回リリース後に成立しなくなる。副作用の不在は状態の同一性か、書き込み手段の不在でしか継続的に示せない。
**代替案:**
- **A: 配信リポジトリの tag 数 0 を確認する (当初案)** — リリース後は無関係に失敗する一時的な検査。却下

## Risks / Trade-offs

- `exclusiveContent` と packageSourceMapping、空の `RestorePackagesPath`、`path:` 参照の identity は机上確定のため、tasks 1.x のスパイクで先に実測する (lessons process L-004)。覆った場合は Decision 1 / 2 の見直しをオーナーへ上げる
- 空の展開先を使うため MAUI 消費者 job は NuGet キャッシュの恩恵を受けず、restore が毎回フルになる (MAUI 本体 + AndroidX 系の取得)。検証の正しさを優先する
- README の最小例がコンパイルできない場合、README 修正が本変更に同梱される (proposal Impact)
- `artifact` 入力の実証は draft PR 上の一時的な呼び出しで行う (tasks 5.7)。release workflow 側の upload 名は phase-8 で決まるため、本変更では入力の契約と download の動作までを保証する

## Migration Plan

新規ディレクトリと workflow の追加で、既存のコード・Sample・CI job には影響しない。`ci.yml` の job 追加と `develop` の必須 check 追加 (tasks 6.1) は同じ PR のマージ後に行う (追加前にマージすると必須 check が未登録のまま、追加後にマージすると check が存在せず PR が詰まるため、順序は「マージ → 保護設定の更新」)。

## Open Questions

- なし (smoke 正ケースの実証・`main` の保護・release からの呼び出しは phase-8 に申し送り済み)

## ADR 候補

- なし。Decision 1〜6 はいずれも `verification/` と CI 定義に閉じた可逆な判断で、選別 3 基準 (覆すコスト高 / 境界を越える / 将来を制約) に該当しない。配布チャネル・lockstep・release 手順・CI の構成原則は既存の cross/ADR-0018 / 0019 / 0020 / 0025 / 0026 が担う。蒸留時は `verification/` の役割を concepts `cross/architecture/repository-boundaries.md` に追記する (agenda 決定)
