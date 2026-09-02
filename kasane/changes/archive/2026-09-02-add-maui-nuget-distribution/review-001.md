# レビュー結果: add-maui-nuget-distribution (001 回目)

**日付**: 2026-09-02
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 7 Requirement は、名前空間改名・共通メタデータ / CPM・pack 設定・buildTransitive ガード・README・規範追随のすべてで実装が揃っており、独立に再実行した検証 (facade テスト 516 件、3 パッケージの Release pack と同梱物検査、pack 対象の限定、標準 lint 4 種) はいずれも成果物どおりの結果になった。証跡の粒度と再現性は高く、deviation.md の 6 件はいずれも実測の裏付けを持つ合意済み差分として妥当である。

一方で、本 change が新たに「配布物」に格上げしたルート README について、画像参照は絶対 URL 化されたが**リンク参照 7 箇所 × 2 枚が相対パスのまま残っており、nuget.org のパッケージページからは到達できない**。同じ 2 ファイル内の同型の欠陥であり、修正は十数行で閉じるため本サイクルで直すべきと判断した (lessons/process.md L-005)。ほかに同梱アイコンの第三者由来に対する帰属表示の欠落を Minor として挙げる。Critical / Major はない。

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` | **常時** | 適用。lint 0 件に加え、新規 MSBuild 3 ファイルと改変 csproj 5 件のコメントを規約本文から手読み — ADR 参照はすべて `<domain>/ADR-NNNN` 形式、作業文書パス・ローカル通番・進捗ログ・SHALL 等の残存なし |
| `kasane/handbook/cross/test-execution.md` | テストを実行・報告するとき | 適用。MAUI 節のコマンドで実行し件数を併記 (下記) |
| `kasane/handbook/cross/public-identifiers.md` | `**/*.csproj` / `maui/Directory.*.props` を触るとき | 適用。本 change 自身が NuGet 節を追加。Package ID 3 件・namespace の非対称の記述は実装 (nuspec の `id`) と一致 |
| `kasane/handbook/maui/integration-host-verification.md` | `maui/**` に触れ end-to-end 疎通を確認するとき | 適用。MauiHost は 5 手順 + 両 OS 静止画あり。IntegrationHost は build のみ (下記 Suggestion) |

適用外と判定した文書: `cross/runtime-behavior-verification.md` (IME・フォーカス・アニメーション等の実行時挙動を扱わない)、`cross/sample-parity.md` (`samples/` の変更は namespace / xmlns 追随のみで、デモ画面・文言・デモデータに触れていない)、`cross/aiforms-origin-reference.md` (未移植機能の実装ではない)、`cross/user-skill-api-listing.md` (`skills/` を触っていない — 追随は docs-refresh へ委譲済み)、`cross/local-development-setup.md` (guide)、`maui/performance-verification.md` (性能評価ではない)、`ios/` 配下 (作業ドメイン外)。

## 独立に再実行した検証

- `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → **失敗 0 / 合格 516 / スキップ 0 / 合計 516** (基準の 516 件と一致)
- `dotnet pack -c Release` を binding 2 件 → facade の順で実行 → 3 nupkg + 3 snupkg を生成。facade の nuspec は `id=KsSettingsView.Maui` / `readme=README.md` / `icon=icon.png` / license expression `MIT` / TFM 別依存 (`net10.0` に binding なし、`net10.0-android36.0` / `net10.0-ios26.0` に binding 同版) で `evidence/pack-release-inspection.txt` と一致。同梱物も 11 エントリで一致し、`buildTransitive/` の props / targets は各 1 件のみ (csproj の明示 `None` と既定 glob の二重同梱は起きていない)
- 両 binding の nuspec に "do not reference this package directly" を確認。Android は AndroidX 系 14 件 (LiveData 2.11.0.1 を含む)、iOS は `resources.zip` 内 xcframework に `ios-arm64` / `ios-arm64_x86_64-simulator` の両スライスを確認
- テスト + 検証ホスト 3 件に `dotnet pack -c Release` → **nupkg 0 件** (pack 対象の限定が成立)
- `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` → いずれも違反 0 件。未追跡の新規ファイル (`maui/Directory.*.props` / `.targets`、`buildTransitive/`、`evidence/`) も手動 grep でローカル絶対パス 0 件
- CPM の網羅性: `maui/` 配下の csproj / props / targets に `Version=` 付き `PackageReference` は 0 件。`Directory.Packages.props` の 18 件の版は改名前 csproj の値と全件一致
- 改名の残存: `maui/` `samples/` に `namespace KsSettingsView.Maui*` / `using KsSettingsView.Maui*` / `clr-namespace:KsSettingsView.Maui` は 0 件 (`obj/` の生成物を除く)。`skills/` の 6 箇所は Non-Goal (docs-refresh) として残置

## 指摘事項

### [🟡 Minor / 優先度高] package README の相対リンク 7 箇所が nuget.org から到達できない

**該当箇所**: `README.md:5,38,137,141,156,162,166` / `README_ja.md:5,38,137,141,156,162,166`

**問題点**:
本 change はルート `README.md` を facade パッケージの package README として同梱する (`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj:36`、nuspec の `readme=README.md` を再現確認済み)。画像参照は `raw.githubusercontent.com` の絶対 URL に改めた一方、**リンク参照は相対パスのまま**である。両 README に各 7 箇所:

- `README_ja.md` / `README.md` (言語切替)
- `skills/README.md` (2 箇所: 導入節と Skills 節)
- `skills/en/kssettingsview-*/SKILL.md` (MAUI 節の設定手順リンクと Skills 節の 4 リンク)
- `AGENTS.md` / `kasane/concepts/index.md`
- `.github/CONTRIBUTING.md`
- `LICENSE`

nuget.org のパッケージ詳細ページは README を単体でレンダリングするため、`skills/README.md` のような相対パスがリポジトリ上のファイルを指すことはあり得ない。NuGet 公式ドキュメント (learn.microsoft.com の "Package readme on NuGet.org") は画像について「相対ローカルパスの画像はレンダリングされない」と明記しており、本 change の画像対応はこれに沿っている。リンクについて同ドキュメントは明示していないが、解決先の base が存在しない以上、利用者が最初に見る面で「MIT License」「contribution guidelines」「.NET MAUI Skill」を含む 7 リンクが機能しないことは確定する。

Requirement「package README の表示」の SHALL 文はスクリーンショット参照に限定されているため spec 違反ではないが、**本 change が README を配布物に変えたことで生じた欠陥**であり、画像で直したのとまったく同型の問題が同じ 2 ファイルに残っている。修正は 7 行 × 2 ファイルで閉じ、本 change が既に触っているファイル内であるため、docs-refresh への先送りではなく本サイクルで処理する対象と判断した (lessons/process.md L-005)。

**推奨修正**:
両 README のリンク参照を `https://github.com/kamusoft/KsSettingsView/blob/develop/<path>` 形式の絶対 URL に改める (画像と同じく両枚同時)。GitHub 上の表示・遷移は絶対 URL でも変わらない。`README_ja.md` / `README.md` の相互リンクも同様に扱う。修正後に facade を再 pack し、nupkg 内 README にリンクの相対パスが残っていないことを `evidence/readme-image-urls.txt` (または新規の証跡) に追記する。

### [🟡 Minor] 同梱アイコンの第三者由来に対する帰属表示がない

**該当箇所**: `assets/icon.png`、`maui/Directory.Build.props:22`、`maui/Directory.Build.targets:13-15`

**問題点**:
`assets/icon.png` は AiForms.Maui.SettingsView の `images/icon.png` の複製であり (tasks 2.1、phase-6 history の記録どおりオーナー指定)、本 change で公開 NuGet パッケージ 3 件すべてに同梱される (再現確認済み: 3 nupkg のルートに `icon.png`)。一方、パッケージのメタデータは `copyright=Copyright (c) kamusoft` / `license=MIT` のみで、リポジトリにも第三者素材の帰属を示す記載 (`LICENSE` 内の注記や NOTICE 相当のファイル) がない。原典が MIT である場合、著作権表示の保持が条件に含まれる。

phase-6 の議論では「原典の継承」としてアイコンの採用自体は決まっているが、帰属表示の要否は agenda / design / deviation のいずれにも現れておらず、未検討のまま公開物に載っている状態である。

**推奨修正**:
公開前 (phase-8) に決着させる。最小の形は `LICENSE` 末尾か新規の帰属記載に「`assets/icon.png` は AiForms.Maui.SettingsView 由来」の 1 段落を、原典のライセンス表記を確認したうえで置き、必要なら package README にも 1 行添える。オーナー判断を要するため、本サイクルで結論が出ない場合は deviation.md に「公開前の宿題」として記録し、phase-8 の agenda へ論点として送る。

### [🔵 Suggestion] comment-policy lint の検査対象に MSBuild ファイルが入っていない

**該当箇所**: `kasane/config.yaml` の `lint.comment-policy.ext`

**問題点**:
`scripts/comment-policy-lint.py` の既定拡張子は `.swift .kt .kts .cs .java .xml .xaml .axml .gradle .pro` で、`.csproj` / `.props` / `.targets` を含まない。本 change で規約性の高いコメント (版選定理由・ADR 参照・ガードの設計意図) の相当量が `maui/Directory.Packages.props` と `buildTransitive/` へ移ったため、lint が素通りする領域が実質的に広がった。今回は手読みで違反 0 件を確認したが、以後の編集は機械検査に載らない。

**推奨修正**:
`kasane/config.yaml` の `lint.comment-policy.ext` に `.csproj` / `.props` / `.targets` を追加し、lint を 1 回通して既存債務の有無を確認する。

### [🔵 Suggestion] `samples/maui` の最低 OS 版が単一宣言元から外れている

**該当箇所**: `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj:25,29`

**問題点**:
Requirement「最低 OS 版のビルド時ガード」は同梱 props を単一の宣言元と定め、`maui/` 配下の 6 プロジェクトはすべて定数参照に置き換わった (評価値も再確認済み)。Sample は別ビルドルートかつ `ProjectReference` 参照のため props も buildTransitive ガードも届かず、`16.0` / `29` が直書きのまま残る。spec の「要件の宣言元の一致」Scenario は Sample を対象にしていないので違反ではないが、将来 `KsSettingsViewMinIOSVersion` 等を引き上げたとき、Sample だけが要件未満のまま黙って通る経路が残る。

**推奨修正**:
現状維持でよい (Sample が利用者の csproj と同じ形であることに価値がある) が、要件の数値を変える手順の一部として Sample の追随を明示しておく。`buildTransitive/KsSettingsView.Maui.props` のコメントに「この値を変えたら `samples/maui` の直書きも合わせる」の一文を足すのが最も安い。

### [🔵 Suggestion] IntegrationHost は build のみで、固定シナリオの実行が証跡にない

**該当箇所**: `evidence/os-version-guard.txt`、`evidence/namespace-rename-build-and-test.txt`

**問題点**:
`kasane/handbook/maui/integration-host-verification.md` の完了条件は「IntegrationHost の固定シナリオが期待される内容を表示し、両 OS で一致する」を含むが、証跡にあるのはビルド成功のみで、起動と表示確認は MauiHost だけである。緩和材料は揃っている — binding の名前空間 `KsSettingsView.Bridge` は改名対象外で、`evidence/cpm-restore-invariance.txt` が 7 プロジェクトの解決パッケージを導入前と完全一致と示しているため、binding の実行時挙動が変わる経路が見当たらない。判定としてはこの根拠が記録されていれば十分と考える。

**推奨修正**:
IntegrationHost を両 OS で起動して固定シナリオを確認するか、上記の理由 (binding 層に挙動変更の経路がないこと) を証跡に 1 行残して完了条件の判定根拠を追えるようにする。

### [🔵 Suggestion] NU1507 の恒久対処の決着点が決まっていない

**該当箇所**: `maui/Directory.Packages.props:8`、`evidence/cpm-restore-invariance.txt`

**問題点**:
CPM 導入により、NuGet ソースを 2 件以上構成した環境では全プロジェクトの restore で NU1507 が出る。証跡の「作業機の構成が原因でリポジトリの内容には依存しない」は正確だが、release workflow (phase-8) のランナーが複数ソースを持つ構成になった場合は同じ警告が CI にも出る。spec は restore 警告 0 件を消費者側にしか要求していないため本 change の違反ではない。

**推奨修正**:
phase-8 の agenda に「NU1507 の扱い (packageSourceMapping を入れるか `NoWarn` で明示的に受け入れるか)」を 1 論点として送る。

### [🔵 Suggestion] 他フェーズの agenda への追記が本 change の diff に混在している

**該当箇所**: `kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md:28`

**問題点**:
NuGet.org Trusted Publishing のポリシー作成結果 (オーナー実施) の追記が、本 change の作業ツリーに含まれている。tasks / deviation のいずれにも対応がなく、レビュー範囲からは由来を追えない。内容自体は phase-8 の記録として妥当で、足場の書き換えにも当たらない。

**推奨修正**:
本 change のコミットからは切り離し、ksn-agenda の記録として独立に扱う (または deviation.md に付随の記録として 1 行残す)。

## アクションプラン

1. **両 README のリンク参照を絶対 URL 化** (7 行 × 2 ファイル) → facade を再 pack して nupkg 内 README を再確認し、証跡に追記する
2. **同梱アイコンの帰属表示**をオーナーに諮る。本サイクルで決着しない場合は deviation.md へ「公開前の宿題」として記録し、phase-8 へ送る
3. `kasane/config.yaml` の `lint.comment-policy.ext` に MSBuild 拡張子を追加し、lint を 1 回通す
4. IntegrationHost の完了条件について、実行するか判定根拠を証跡に残すかを選ぶ
5. `buildTransitive/KsSettingsView.Maui.props` のコメントに Sample 追随の一文を足す (任意)
6. NU1507 の扱いを phase-8 の論点として起票し、phase-8 agenda への追記の扱いを整理する
