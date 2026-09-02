# レビュー結果: add-maui-nuget-distribution (002 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

前回 (review-001 / second-opinion-code-001) の指摘のうち、修正サイクルの対象だった 7 件はすべて成果物に反映されており、再実行した検証 (facade テスト 516 件、3 パッケージの Release pack と同梱物・nuspec 検査、標準 lint 4 種) はいずれも成果物どおりの結果になった。README の相対リンクは 12 箇所 × 2 枚がすべて絶対 URL になり、nupkg から取り出した README がリポジトリルートの `README.md` とバイト一致で相対参照 0 件であることを独立に確認した。`MauiProgram` 例の追加と、`SwitchCell` / `EntryCell` 両型の CS0104 の実測も証跡に揃っている。

新規指摘は 1 件のみで、実装の修正を要しない記録の不足である。Android binding パッケージにも .NET Android SDK が自動生成する自 assembly 用 aar が 1 本入るため `lib/` の aar は 3 本になるが、deviation.md の該当項目は facade の Scenario にしか及んでいない。事実は evidence に記録済みで、deviation.md への 1 行追記で閉じるため、追加のレビューサイクルは要さないと判断して APPROVED とする (ksn-verify の前に処理することを推奨)。

## 前回指摘の対応状況

| 出典 | 指摘 | 状態 |
|---|---|---|
| review-001 Minor 1 | package README の相対リンクが nuget.org から到達できない | **対応済み** (両 README 12 箇所を絶対 URL 化、`evidence/readme-image-urls.txt` に到達性 17 URL と再 pack 後の確認) |
| review-001 Minor 2 | 同梱アイコンの第三者由来に対する帰属表示がない | **残存** (オーナー判断待ち。推奨した代替策である deviation.md への「公開前の宿題」記録・phase-8 agenda への論点送りもいずれも未反映) |
| review-001 Suggestion 1 | comment-policy lint に MSBuild 拡張子が入っていない | **対応済み** (`kasane/config.yaml` の `lint.comment-policy.ext` に `.csproj` / `.props` / `.targets`。再実行で違反 0 件 / 対象 712 ファイル) |
| review-001 Suggestion 2 | `samples/maui` の最低 OS 版が単一宣言元から外れている | **対応済み** (`maui/KsSettingsView.Maui/buildTransitive/KsSettingsView.Maui.props:11-13` に追随の一文) |
| review-001 Suggestion 3 | IntegrationHost が build のみで固定シナリオの実行が証跡にない | **対応済み** (`evidence/namespace-rename-build-and-test.txt` の「IntegrationHost をビルド成功までで判定した根拠」3 点) |
| review-001 Suggestion 4 | NU1507 の恒久対処の決着点 | **残存** (オーナー判断待ち。本レビューの restore でも 4 プロジェクトに再現) |
| review-001 Suggestion 5 | 他フェーズの agenda への追記が diff に混在 | **対応済み** (コミット `a5ba445` として本 change から分離済み) |
| second-opinion Major 1 | `EntryCell` にも未記録の名前衝突 | **対応済み** (deviation.md 7 件目に拡張、`evidence/consumer-verification.txt` 8-a / 8-b で両型・両 TFM の CS0104 と回避を実測) |
| second-opinion Major 2 | package README の `MauiProgram` 例がそのまま使えない | **対応済み** (両 README に自己完結の `MauiProgram` 例、`evidence/consumer-verification.txt` 7-2 で nupkg 内 README だけを入力にした再検証) |

既知の未対応事項として本レビューの指摘に数えなかったもの: アイコンの帰属表示 (上表)、XA4301 4 件、NU1507、Android binding の xml doc 同梱。

## 照合した規約

| 文書 | 適用のきっかけ | 判定 |
|---|---|---|
| `kasane/handbook/cross/comment-policy.md` | **常時** | 適用。lint 0 件に加え、修正サイクルで新たに触れた `maui/KsSettingsView.Maui/buildTransitive/KsSettingsView.Maui.props` のコメント追記を規約本文から手読み — 作業文書パス・ローカル通番・進捗ログ・SHALL の残存なし。ADR 参照はすべて `<domain>/ADR-NNNN` 形式で、参照先 (`cross/ADR-0001` / `cross/ADR-0020` / `maui/ADR-0010` / `maui/ADR-0025` / `maui/ADR-0026` / `android/ADR-0013`) は `kasane/decisions/` に実在 |
| `kasane/handbook/cross/test-execution.md` | テストを実行・報告するとき | 適用。MAUI 節のコマンドで実行し件数を併記 (下記) |
| `kasane/handbook/cross/public-identifiers.md` | `**/*.csproj` / `maui/Directory.*.props` を触るとき | 適用。追加された NuGet 節・表の行・「してはいけないこと」の記述が pack した nuspec の `id` 3 件と一致。`applies-when.paths` に `maui/Directory.*.props` を足した自己適用も整合。`doc-structure-lint` で本文書の違反 0 件 |
| `kasane/handbook/maui/integration-host-verification.md` | `maui/**` に触れ end-to-end 疎通を確認するとき | 適用。MauiHost の 5 手順 + 両 OS 静止画に加え、IntegrationHost をビルドまでで判定した根拠が証跡に記録された |

適用外と判定した文書: `cross/runtime-behavior-verification.md` (実行時挙動の不具合調査ではない)、`cross/sample-parity.md` (`samples/` の変更は namespace / xmlns 追随のみ)、`cross/aiforms-origin-reference.md`、`cross/user-skill-api-listing.md` (`skills/` は未変更、追随は docs-refresh へ)、`cross/local-development-setup.md` (guide)、`maui/performance-verification.md`、`ios/` 配下 (作業ドメイン外)。

`kasane/lessons/code-review.md` L-001 (ミューテーションによる検出力の実測) は、本 change がテストコードの検出力を争点にしていない (テスト差分は名前空間の機械的追随のみ) ため適用契機に当たらない。

## 独立に再実行した検証

- `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → **失敗 0 / 合格 516 / スキップ 0 / 合計 516** (基準の 516 件と一致)
- binding 2 件 → facade の順に `dotnet pack -c Release -p:Version=0.1.0-alpha.1` → 3 nupkg + 3 snupkg。facade の nuspec は `id=KsSettingsView.Maui` / `readme=README.md` / `icon=icon.png` / license expression `MIT` / `repository` の url・branch・commit / TFM 別依存 (`net10.0` に binding なし、`net10.0-android36.0` に `KsSettingsView.Binding.Android`、`net10.0-ios26.0` に `KsSettingsView.Binding.iOS` を同版) で `evidence/pack-release-inspection.txt` と一致
- facade nupkg から取り出した `README.md` は**リポジトリルートの `README.md` とバイト一致**。相対リンク (`](` で `https://` / `#` 以外に始まるもの) 0 件、`using KsSettingsView;` を含む `MauiProgram` 例あり
- 両 README の相対リンク残存: `grep` で 0 件 (絶対 URL は各 12 箇所、うち一意 10、画像 4 箇所)
- Android binding: `lib/net10.0-android36.0/` に `kssettingsview-release.aar` / `kssettingsview-bridge-release.aar` と SDK 自動生成の `KsSettingsView.Binding.Android.aar`。nuspec の依存に AndroidX 系 14 件 (LiveData 2.11.0.1 を含む)。iOS binding: `resources.zip` 内 xcframework に `ios-arm64` と `ios-arm64_x86_64-simulator` の両スライス。両 nuspec の description に "do not reference this package directly"
- CPM の網羅性: `maui/` 配下の csproj / props / targets に `Version=` 付き `PackageReference` は 0 件
- `python3 scripts/comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` → いずれも違反 0 件 (comment-policy は拡張子追加後の 712 ファイル)。`doc-structure-lint.py` に本 change が触れた文書の違反なし
- 足場の凍結: `proposal.md` / `design.md` / `specs/maui-nuget-distribution/spec.md` は基準コミットから無変更。`tasks.md` はチェックのみで、全 22 項目の完了は evidence の対応する節で裏付けられている

## 指摘事項

### [🟡 Minor] Android binding の aar が 3 本になることが合意済み差分として記録されていない

**該当箇所**: `kasane/changes/add-maui-nuget-distribution/deviation.md` (2 件目の項目) / `kasane/changes/add-maui-nuget-distribution/evidence/consumer-verification.txt` の 6-2

**問題点**:
デルタスペックの Scenario「binding パッケージの同梱物と説明」は「Android binding の `lib/` に aar 2 本があり」を求めているが、実際に pack すると 3 本入る (再現確認済み):

```
lib/net10.0-android36.0/kssettingsview-release.aar          607084
lib/net10.0-android36.0/kssettingsview-bridge-release.aar    90119
lib/net10.0-android36.0/KsSettingsView.Binding.Android.aar   17130
```

3 本目は .NET Android SDK が Android ライブラリ assembly ごとに自動生成する自 assembly 用 aar で、中身は推移依存 `androidx.graphics.path` の ABI 別 `.so` 4 ファイルのみ (facade 側の `KsSettingsView.Maui.aar` と同一内容・同一サイズであることを展開して確認)。つまり deviation.md の 2 件目で合意済みの事由がそのまま当てはまる。

しかし deviation.md の当該項目は Scenario「3 パッケージのローカル pack」(= facade の nupkg) だけを対象に書かれており、binding 側の Scenario には及んでいない。`evidence/consumer-verification.txt` の 6-2 は binding の自 aar の存在と中身を正確に記録し、「facade 側の aar は deviation.md で合意済み」と facade 限定であることまで書いているため、**事実は把握されているが合意済み差分としての記録だけが片側に閉じている**状態である。この記録の欠けは、spec の文言と機械的に突き合わせる ksn-verify で「aar 2 本」の不一致として現れる。

**推奨修正**:
deviation.md の 2 件目の項目に binding の Scenario を含めるか、同じ事由で 1 項目を足す (実装の修正は不要)。文面は既存項目に倣い、「Android binding の nupkg にも SDK 自動生成の `KsSettingsView.Binding.Android.aar` が入り `lib/` の aar は 3 本になる。中身は `androidx.graphics.path` の ABI 別 `.so` のみで、spec の意図である『Gradle 由来の aar 2 本が同梱される』は満たす」旨を証跡の参照付きで残す。ksn-verify の前に処理するのが安い。

### [🔵 Suggestion] 証跡内のリンク本数が実測と食い違う

**該当箇所**: `kasane/changes/add-maui-nuget-distribution/evidence/readme-image-urls.txt` (リンク参照の節の「各 11 リンク」)

**問題点**:
証跡は「README.md / README_ja.md を同時、各 11 リンク」と書いているが、続く一覧は 12 行あり、実測でも各ファイルに 12 箇所の絶対 URL がある (一意は 10。`skills/README.md` と MAUI の `SKILL.md` が各 2 回現れる)。同ファイル内の他の数値 (画像 4 箇所、curl 17 URL、nupkg 同梱物 11 エントリ = nuspec を除く数) はいずれも実測と一致しており、この 1 箇所だけが合わない。

**推奨修正**:
「各 12 箇所 (一意 10)」に直す。判定に影響する誤りではないが、証跡は後続フェーズが数え上げの根拠に使うため数値は実測に揃える。

## アクションプラン

1. **deviation.md に Android binding の自動生成 aar を追記**する (1 項目。実装変更なし)。ksn-verify の前に済ませる
2. `evidence/readme-image-urls.txt` のリンク本数を実測値に直す (任意)
3. 残存事項 (アイコンの帰属表示 / NU1507) はオーナー判断待ちとして phase-8 へ送る。帰属表示は review-001 が代替策として挙げた「deviation.md への公開前の宿題としての記録」も未反映のため、どちらの形で残すかを決める
