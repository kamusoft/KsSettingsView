# レビュー結果: add-spm-distribution (001 回目)

**日付**: 2026-09-01
**判定**: APPROVED

## サマリー

デルタスペックの ADDED Requirement 3 本 (umbrella product の一本化 / monorepo 内消費者の追随 / スナップショット同期スクリプト) は、レビュー側で再実行したビルド・テストで実装が確認できた。同期スクリプトは破壊的操作の前に 4 段の検証を置く設計が spec どおりに実装されており、origin 照合を前方一致ではなく受理 URL の列挙で行うなど、誤指定の穴の塞ぎ方は堅い。umbrella 化に伴う scheme 名の変化を検証 CI と handbook へ波及させた付随修正も、実行して漏れがないことを確認した。

未確定なのは配信リポジトリ側 (グループ 4) の外部操作で、これは意図的な先送りとして指摘対象外とした。指摘は Minor 4 件・Suggestion 4 件で、いずれもグループ 4 の実行前に片付けられる範囲。Critical / Major はない。

## 実行した検証

| 検証 | コマンド | 結果 |
|---|---|---|
| package 定義 | `swift package --package-path ios dump-package` | products は `KsSettingsView` 1 件のみ / targets に Core・UI・SwiftUI の 3 つ / platforms は iOS 16.0・macOS 13.0。target の名前・依存・path は diff 上も変更なし |
| scheme 一覧 | `cd ios && xcodebuild -list` | scheme は `KsSettingsView` の 1 本のみ (`KsSettingsView-Package` は生成されない) |
| iOS 全件テスト | `cd ios && xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=<ios-simulator-udid>'` | `** TEST SUCCEEDED **`。5 バンドルすべて実行 — Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UI 645 = **合計 1000 tests, 0 failures** |
| Sample ビルド | `cd samples/ios && xcodebuild build -scheme KsSettingsViewSample -destination 'platform=iOS Simulator,...'` | `** BUILD SUCCEEDED **` |
| binding | `./ios/binding/build-xcframework.sh` | `** ARCHIVE SUCCEEDED **` / xcframework 生成 (ios-arm64 + ios-arm64_x86_64-simulator の 2 スライス) |
| 同期スクリプト | `./scripts/spm-snapshot/sync-snapshot-test.sh` | 33 アサーション全成功 (exit 0) |
| lint | `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | comment-policy 禁止 0 件 (703 ファイル)、local-path・identity は出力なし。doc-structure は既存 387 件が出るが、本変更が触った `handbook/cross/public-identifiers.md` は違反リストに含まれず、`test-execution.md` の 5 件は変更箇所と無関係な既存分 (項目の増減なし) |

補足: scheme 名の変更で全テストターゲットが拾われなくなる懸念があったが、Bridge (product に含まれない target) のテストバンドルも実行されており、カバレッジの後退はない。なお `xcodebuild` 出力末尾の `Executed N tests` は最後のバンドル (UI = 645) の値であり、パッケージ全体の実行件数は 1000 件。完了報告で 645 を全体件数として扱うと過小報告になる (handbook cross/test-execution.md の「出力末尾で確認」がバンドル複数時に誤読を招く形になっている。本変更起因ではない)。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (新設シェルスクリプト 2 本・Package.swift・build-xcframework.sh・csproj のコメント改訂) |
| `kasane/handbook/cross/test-execution.md` | テストを実行するとき・テスト結果を報告するとき (本文の改訂対象でもある) |
| `kasane/handbook/cross/public-identifiers.md` | `ios/Package.swift` を触る / 配布座標を決める (本文の改訂対象でもある) |
| `kasane/handbook/ios/swift6-language-mode-check.md` | `ios/Sources/**` を触るとき → 本変更は `ios/Sources/` を触らないため確認手順自体は非該当。改訂された scheme 識別子が実態と一致することのみ照合 |
| `kasane/lessons/code-review.md` | L-001 (アサーション検出力) を適用。「指摘しないこと」は昇格済みルールなし |

`sample-parity.md` (Sample の画面・文言・デモデータの変更なし)、`user-skill-api-listing.md` (`skills/` を触らない)、`runtime-behavior-verification.md` (実行時挙動の不具合ではない) は非該当と判断した。

コメント規約は新設・改訂コメントを節ごとに照合した。作業文書のパス参照・変更識別子の裸参照・ローカル通番・進捗ログ・デルタスペック構文キーワードのいずれも混入していない。`ios/Package.swift` ヘッダの進捗記述の撤去、`build-xcframework.sh` と csproj のコメントを現在形の仕様説明に保った書き換えは、いずれも規約に沿っている。

## 指摘事項

### [🟡 Minor] 配信 README テンプレートが spec の「誘導のみ」を超え、版番号を二重に持つ

**該当箇所**: `scripts/spm-snapshot/README.template.md:10-20`

**問題点**:
デルタスペックの Requirement「配信リポジトリの初期状態」は「ルートの README は monorepo (ソース・Issue 窓口) への誘導のみを内容とする (SHALL)」と定めている。テンプレートは誘導に加えて依存宣言スニペットと `from: "0.1.0"` という版リテラルを持っており、この逸脱は deviation.md に記録されていない。Scenario「誘導 README の内容」自体は満たしているため機能上の破綻ではないが、次の 2 点が実務上の負債になる。

1. 同じスニペットが `README.md:41-48` / `README_ja.md` / `skills/en/kssettingsview-ios/SKILL.md:45` / `skills/ja/kssettingsview-ios/SKILL.md:45` にも存在し、版を上げるときの更新箇所が 1 つ増える。`skills/` と README 群は docs-refresh が追従させる対象だが、`scripts/` 配下のこのテンプレートはその守備範囲外で、ここだけ取り残される経路になる
2. design.md Decision 2 により配信リポジトリは検証 tag 削除後 phase-8 の初回リリースまで tag ゼロで運用される。その期間に配信リポジトリを訪れた利用者にとって、このスニペットは解決できない依存宣言として提示される

**推奨修正**:
版を含む依存宣言を落とし、product 名と「詳細は monorepo の README / Agent Skills を見る」の誘導だけに絞る (spec の「誘導のみ」に寄せる)。スニペットを残す判断をするなら、deviation.md に「誘導のみ」からの逸脱として記録し、版番号の追従先を一元化する方針 (docs-refresh の対象に含める等) を併記する。

### [🟡 Minor] 「monorepo 自身」テストが検証 4 に到達しておらず、自己指定ガードの検出力がない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot-test.sh:146-151` (対象コード: `scripts/spm-snapshot/sync-snapshot.sh:98-105`)

**問題点**:
このケースは同期先に monorepo 自身を渡して非ゼロ終了を確認するが、monorepo の origin は ssh 形式の monorepo URL (`github.com` の `kamusoft/KsSettingsView.git`) であり、検証 3 (origin 照合、`sync-snapshot.sh:87-94`) が先に失敗する。したがって検証 4 の自己指定ガード (`sync-snapshot.sh:98-100`) には一切到達せず、アサーションが通る理由は origin 不一致である。

さらに `sync-snapshot.sh:98-100` の等価判定は直後の case (`:101-105`) と冗長で、機能的には削除しても挙動が変わらない — `DESTINATION == MONOREPO_ROOT` のとき、subject `${MONOREPO_ROOT}/` はパターン `${DESTINATION}/*` に (`*` が空文字に一致するため) 合致する。実際に検証 4 へ検出力を与えているのは「祖先ディレクトリ」ケース (`:153-170`) だけであり、そちらは配信リポジトリ origin を持つ git top-level の中に fake monorepo を作る形で正しく組まれている。

自己指定ガードが守るのは「スクリプトごと配信リポジトリの作業コピーへ持ち込み、その作業コピー自身を同期先に渡す」という現実に起こり得る誤用であり、テストが名前どおりの経路を検証していないのは検出力の穴になる。

**推奨修正**:
「monorepo 自身」ケースを祖先ケースと同型に組み替える — 配信リポジトリ origin を持つ git top-level 自身が fake monorepo のルートになる構成 (その直下に `scripts/spm-snapshot/` + `ios/Package.swift` / `ios/Sources` / `ios/Tests` / `LICENSE` を置く) を作り、そこを同期先として渡す。検証 3 を通過した状態で検証 4 が発火することを確認できる形にする。

### [🟡 Minor] 新設のシェル資産に自動検査の経路がない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot-test.sh` (全体) / `.github/workflows/ci.yml:42-53`

**問題点**:
33 アサーションのテストはどの workflow からも呼ばれておらず、退行検出が手動実行に依存する。守っている対象が「引数を誤ると任意の作業ツリーを消し得るスクリプト」であることを考えると、安全弁の検査を人の記憶に置くのは弱い。既知の保留事項として `scripts/comment-policy-lint.py` が `.sh` を対象外にしている件があり、これと合わせると新設の 3 ファイルは機械検査の対象がゼロになる。

`ci.yml` の `lint` job は ubuntu-24.04 で動いており、このテストは bash と git しか使わない (実測 1 秒未満) ため、追加コストはほぼない。

**推奨修正**:
`ci.yml` の `lint` job に `scripts/spm-snapshot/sync-snapshot-test.sh` を実行する step を追加する。phase-8 で release workflow から同期スクリプトを呼ぶ前に、退行が CI で止まる状態にしておく。

### [🟡 Minor] SwiftUI module の静的ライブラリ同梱 — オーナー判断待ち項目への実測提供

**該当箇所**: `ios/binding/build-xcframework.sh:10-12` / `maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:58-59,87`

**問題点**:
deviation.md に「オーナー判断待ち」として記録済みの項目であり、spec の受け入れ条件 (ビルド成功・xcframework 生成完了) は満たしているため乖離としては指摘しない。判断の材料が deviation.md の記述だけでは足りないと考えたため、レビュー側で実測した結果を残す。

- 生成物 `KsSettingsViewBridge.xcframework/ios-arm64/.../KsSettingsViewBridge` は `current ar archive` であり、module ごとに独立したアーカイブメンバーになっている (`KsSettingsViewCore.o` 121,144 bytes / `KsSettingsViewUI.o` 1,496,856 bytes / `KsSettingsViewSwiftUI.o` 344,864 bytes)。SwiftUI 分はアーカイブ全体 3,174,696 bytes の約 11%
- csproj の `NativeReference` は `ForceLoad="False"` / `SmartLink="False"` (`:58-59`) であり、`-force_load` は付かない。静的アーカイブの通常のリンク規則により、`KsSettingsViewSwiftUI.o` は最終アプリリンクで参照されない限り引き込まれない見込み

つまり影響は nupkg / xcframework の配布サイズと native ビルド時間 (SwiftUI 12 ファイルの追加コンパイル) に限られ、利用者のアプリバイナリには乗らない公算が高い。この前提は `ForceLoad` を有効化する将来の変更で崩れる。

**推奨修正**:
オーナー判断そのものは指揮側に委ねる。判断が「許容」に落ちる場合は、`build-xcframework.sh` のコメントに「Bridge が参照しない module のコードも静的ライブラリには含まれる」に続けて「force_load しない前提でアプリバイナリには乗らない」旨を足しておくと、将来 `ForceLoad` を触る変更が入ったときに影響が読める。

### [🔵 Suggestion] 同期先の未コミット変更を無警告で破棄する

**該当箇所**: `scripts/spm-snapshot/sync-snapshot.sh:107-115`

**問題点**: 検証 4 段をすべて通過した後、`.git` 以外を無条件に除去する。spec どおりの挙動だが、配信リポジトリの作業コピーに未コミットの手作業 (前回同期の途中結果・調査用のメモ) があった場合、警告なしに消える。git 管理下に入っていない新規ファイルは復旧手段がない。

**推奨修正**: 破壊的操作の直前に `git -C "${DESTINATION}" status --porcelain` を見て、非空なら標準エラーへ 1 行警告を出す (中断はしない — CI から呼ぶ経路を壊さないため)。

### [🔵 Suggestion] `readonly VAR="$(...)"` がコマンド置換の失敗を握りつぶす

**該当箇所**: `scripts/spm-snapshot/sync-snapshot.sh:34-36`

**問題点**: `set -e` 下でも `readonly VAR="$(cmd)"` は `readonly` 自身の終了状態を見るため、`cd` の失敗が検出されない。`SCRIPT_DIR` が空になると `MONOREPO_ROOT` は `/` に解決される。実害は検証 1 (`/ios/Package.swift` が無い) で止まるため軽微だが、エラーメッセージが原因を指さなくなる。同ファイル `:67-68` は宣言と代入を分けており、そちらの書き方が正しい。

**推奨修正**: `:34-36` も宣言と代入を分けるか、`|| fail "..."` を添える。

### [🔵 Suggestion] スナップショットに `.gitignore` が含まれない

**該当箇所**: `scripts/spm-snapshot/sync-snapshot.sh:117-121`

**問題点**: 配置される 5 点に `.gitignore` がないため、配信リポジトリの作業コピーで誰かが `swift build` を実行すると `.build/` が追跡候補として現れる。次回同期で除去はされるが、その前に commit されると配信リポジトリに混入する。ホワイトリスト 5 点は spec が定めた集合なので、集合を増やすかどうかは spec 側の判断になる。

**推奨修正**: 現状維持でも実害は小さい。気にするなら、同期後の commit 手順 (phase-8 の release workflow 側) で `git add -A` ではなく 5 点を明示的に add する形にしておく。

### [🔵 Suggestion] roadmap の決定記録に旧 scheme 名が残る

**該当箇所**: `kasane/roadmaps/package-distribution/phases/phase-3-verification-ci/agenda.md:12`

**問題点**: 「iOS テストの実行形 (2026-08-31)」の決定文が `xcodebuild test -scheme KsSettingsView-Package ...` を引用しており、本変更後は実行できないコマンドになる。日付つきの決定記録なので当時の記述としては正しいが、roadmap は長命層で参照され続ける。

**推奨修正**: 足場アーティファクトではないので追随してよい。決定の内容 (Simulator 実行で全件を回す) は不変のまま scheme 名だけを実態へ合わせるか、判断に迷うなら指揮側で扱いを決める。

## アクションプラン

1. **配信 README テンプレートの版リテラルを整理する** (Minor 1) — グループ 4 で公開 push する前に決着させる必要がある。「誘導のみ」に寄せるか、残す判断を deviation.md に記録するかの二択
2. **「monorepo 自身」テストを検証 4 に到達する形へ組み替える** (Minor 2) — 破壊的スクリプトの安全弁の検出力に直結する
3. **`ci.yml` の lint job に同期スクリプトのテストを追加する** (Minor 3) — phase-8 で CI から同期スクリプトを呼ぶ前に入れておく
4. **SwiftUI 同梱のオーナー判断を確定させ、決着をコメントへ反映する** (Minor 4) — 実測は本レビューに記載済み
5. **Suggestion 4 件** — 同期先の未コミット警告 / `readonly` の書き方 / `.gitignore` の扱い / roadmap の scheme 名。いずれも任意
