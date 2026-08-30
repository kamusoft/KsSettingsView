# レビュー結果: entrycell-keyboard-avoidance-check (001 回目)

**日付**: 2026-08-24
**判定**: CHANGES_REQUESTED

## サマリー

コードとしては合意済みスコープ (「入力Cell 5種」デモ末尾に検証用 EntryCell セクションを 3 OS 同一様式で追加) を過不足なく満たしている。Section header / footer・Cell title・placeholder・初期値の全文字列が 3 OS で完全一致しており、[Sample のプラットフォーム間一致](kasane/concepts/cross/conventions/sample-parity.md) の要求 (画面構成の一致・表示文言の一字一句一致) を満たす。各 OS の既存様式 (tracked ラッパー / VM の `Set` + `LastEvent` 記録 / 直近イベント 1 行表示) も忠実に踏襲されており、実行したビルド・テスト・lint はすべて成功している。

一方で、3 OS いずれについても Simulator / エミュレータ / 実機での表示確認と証跡が提出されていない。本変更は利用者の目に見えるレイアウト追加であり、かつ「EntryCell が画面下半分に来る状態を作る」という**達成できたかどうかが画面を見ないと判定できない目的**を持つ。`kasane/lessons/process.md` L-003 は、この種の変更で視覚確認と証跡の change 配下保存を APPROVED の条件とし、実機確認を Suggestion へ格下げすることを明示的に禁じている。この 1 点を Major として CHANGES_REQUESTED とする。コードそのものへの修正要求はない。

## 確認した観点と実行結果

### ビルド

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS Sample | `xcodebuild build -scheme KsSettingsViewSample -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | `** BUILD SUCCEEDED **` (error 0 件。警告は本 diff 対象外ファイルの既存 Swift 6 concurrency 警告のみ) |
| Android Sample | `./gradlew :app:compileDebugKotlin` (`samples/android`) | 成功 |
| MAUI Sample | `dotnet build -f net10.0-ios` (`samples/maui/KsSettingsView.Sample.Maui`) | 成功 (0 警告 / 0 エラー) |

### テスト

| 対象 | コマンド | 実行件数 |
|---|---|---|
| Android | `./gradlew test` (`android`) | `testDebugUnitTest` 1310 tests / 0 failures、`testReleaseUnitTest` 1310 tests / 0 failures (計 2620 / 0) |
| MAUI | `dotnet test` (`maui`) | 439 tests / 0 failures / 0 skipped |
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | 起動したがレビュー確定時点で完走せず、件数は取得していない |

iOS 全件テストについて: 完走を待たずに判定した理由を残す。本変更は `samples/ios/` のみに閉じており、`ios/Package.swift` の `targets` / `testTarget` はいずれも `Sources/` と `Tests/` だけを指していて `samples/` を含まない。したがって Sample の変更が iOS テスト結果を変えうる経路は存在せず、iOS 側で本 diff を検証するのは Sample のビルド (上表の `BUILD SUCCEEDED`) である。Android / MAUI も同様に本体テストは本 diff と無関係だが、短時間で完走したため件数を記録した。**本 change に不足しているのはテストではなく実行時の視覚確認**であり、それは下記 Major で扱う。

### lint

- `python3 scripts/comment-policy-lint.py` — 禁止 0 件 (検査対象 677 ファイル)。検査が無音で壊れていないことを `--selftest` (全件 OK) で確認済み。なお lint の検出範囲は規約本文より狭いため、追加コメントは規約の禁止類型からも目視で判定した
- `python3 scripts/local-path-lint.py` — 0 件
- `python3 scripts/identity-lint.py` — 0 件

### 個別に照合した観点

- **合意スコープの充足**: exploration.md 「決定事項」の案A (既存デモ末尾に検証用 EntryCell セクションを 3 OS 分追加) と一致。案B (専用画面新設) / 案C (並び替え) に寄せた実装は混入していない
- **文言の一字一句一致 (3 OS)**: `EntryCell（下部配置）` / footer 全文 / `下部配置の検証用` / `最下部の検証用` の各文字列を 3 ファイルで機械照合し、出現数・バイト列とも一致を確認。Cell title (`メモ` / `署名`)・初期値 (すべて空文字列) も一致
- **画面構成の一致**: 3 OS とも Section 8 個・新規 Section 内 Cell 2 個・並び順 (メモ → 署名) が一致。新規 Cell へ渡すパラメータ集合も 3 OS で同一 (title / placeholder のみ。keyboardType 相当はいずれも未指定)
- **既存様式の踏襲**: iOS は `tracked($memo, title:)`、Android は `memo.tracked(title =, onEvent = record)`、MAUI は `Set(ref _memo, value)` 成功時のみ `LastEvent` 更新 — いずれも同ファイル内の既存 EntryCell と同一の書き方
- **MAUI バインディング**: `EntryCell.ValueTextProperty` は `defaultBindingMode: BindingMode.TwoWay` (`maui/KsSettingsView.Maui/EntryCell.cs:18`) のため、`{Binding Memo}` / `{Binding Signature}` で入力値が VM に戻る。`x:DataType` 付きのコンパイル済みバインディングに必要なプロパティも両方定義済み
- **[ソースコメント規約](kasane/concepts/cross/conventions/comment-policy.md)**: 追加された 3 箇所のコメント (iOS の `// MARK:` と `// 8. ...`、Android の同等、MAUI XAML の `<!-- ... -->`) はいずれも禁止参照 (change 識別子・フェーズ/タスク通番・アーカイブ文書パス) を含まず、ファイル単独で意味が通る
- **README / docs**: `samples/ios/README.md` / `samples/android/README.md` は本デモを Cell 種別単位でしか記述しておらず Section 一覧を持たないため、追随不要。`docs/` は docs-refresh スキルの責務であり本変更の対象外
- **足場アーティファクト**: `exploration.md` は未変更 (git status 上 untracked のまま、内容の書き換えなし)。diff は 4 ファイル・86 行の追加のみで削除・改変ゼロ
- **lessons の適用**: `kasane/lessons/code-review.md` L-001 (ミューテーションでアサーションの検出力を実測する) は本 change にテストアサーションが無いため該当なし。「指摘しないこと」は昇格済みルールなし。`kasane/lessons/process.md` L-003 は該当あり (下記 Major)

## 指摘事項

### [🟠 Major] 3 OS の視覚確認と証跡が無く、目的を達成できたかが判定できない

**該当箇所**: `kasane/changes/entrycell-keyboard-avoidance-check/` (`evidence/` が存在しない) / `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:231-246` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:256-271` / `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:102-111`

**問題点**: 2 点ある。

1. `kasane/lessons/process.md` L-003 は「利用者の目と操作に見える変更 (色・寸法・レイアウト・遷移・IME・フォーカス等) は、実機または Simulator での視覚・操作確認と証跡の change 配下保存を完了報告・レビュー APPROVED の条件にする」「レビューは証跡の実在と提出コードとの対応を判定条件にし、実機確認を Suggestion へ格下げしない」と定める。本変更は 3 OS の Sample 画面へ可視の Section を足すレイアウト追加であり、対象に当たる。現状 `evidence/` は存在せず、視覚確認を行った記録もない。
2. より実質的に、本変更の目的は「EntryCell を画面下半分に置いた状態を作れるようにする」ことであり、**それが達成できたかは diff からは判定できない**。末尾に Section を足せば下に来るというのは妥当な想定だが、実際にどの高さに着地するかは端末高さ・Section footer の行数・list 端の `UICollectionView.contentInset` (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:714` 付近で Section 余白を反映) に依存する。想定どおり下半分に来ていなければ、コードは正しく見えたまま change が自分の目的を果たせていないことになる。ビルド成功はこの問いに何も答えない。

なお本変更は `samples/` のみに閉じ、新しい描画コードを 1 行も足していない (既存 Cell と既存 Renderer の再利用のみ) ため、L-003 が origin 事例で求めた修正前後の A/B 撮影までは要らない。必要なのは「追加後の状態が想定どおりか」を示す 1 状態ずつの確認である。

**推奨修正**: 3 OS それぞれで Sample を起動し、最下部までスクロールした状態の静止画を `kasane/changes/entrycell-keyboard-avoidance-check/evidence/` に保存する (ksn-core references/evidence.md の置き場と命名規約、references/ui-artifacts.md の撮影規律に従う。ファイル名は `input-cells-bottom-ios.png` のような画面と状態が分かる kebab-case)。

- 最低限: 3 OS 各 1 枚 (最下部までスクロールし、メモ / 署名 が画面下半分に来ていることが分かる状態)
- 本 change の目的そのものである「フォーカス時にせり上がるか」も同じ操作の続きで観察できるため、フォーカス後の状態も併せて撮っておくと、この change の成果 (回避が効くか否か) がそのまま証跡になる。効かないと判明した場合は exploration.md の未決の論点どおり後続 change を起票する

### [🔵 Suggestion] 検証用セクションの寿命が記録されていない

**該当箇所**: `exploration.md:19-22`

**問題点**: 追加した Section は「キーボード回避を確かめるための検証装置」であり、恒久的なデモではない。しかし exploration.md には検証完了後にこの Section を残すのか外すのかの記載がない。[Sample のプラットフォーム間一致](kasane/concepts/cross/conventions/sample-parity.md) は Section 構成の 3 OS 一致を要求するため、残す場合は今後この Section も一致維持の対象として扱われ続ける (片側だけ消す・片側だけ増やすが規約違反になる)。扱いが未記録だと、後から「これは何のために残っているのか」を判断できない。

**推奨修正**: exploration.md の決定事項に一行足す。「検証後も恒久デモとして残す」か「本体対応の change が閉じた時点で 3 OS 同時に撤去する」かのどちらかをオーナーに確認して明記する。後者なら撤去タスクを追跡できる形 (後続 change か本 change の未決論点) に残す。

### [🔵 Suggestion] 検証手順として「入力せずフォーカスだけで観察する」ことを残したい

**該当箇所**: `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:236-245` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:261-270`

**問題点**: 新規 EntryCell も既存様式どおり `tracked` ラッパー経由で 1 文字入力ごとに `lastEvent` を更新する。これは様式の踏襲として正しい (指摘ではない) 一方、`lastEvent` の更新は画面上部 Text の再構成 → SwiftUI body / Compose recomposition を誘発するため、**キーボード表示中に文字を打ちながら観察すると、せり上がりの有無とは無関係な再構成が挟まる**。フォーカスするだけなら値が変わらずイベントも発火しないため観察は汚れないが、その前提が成果物のどこにも書かれていない。

**推奨修正**: exploration.md (または実際に検証した際の記録) に「タップしてフォーカスした直後の状態で判定する。文字入力は再構成を誘発するため観察の後に行う」と検証手順を一行残す。コード変更は不要。

### [🔵 Suggestion] ファイル冒頭のコメントが 8 番目の Section の存在を説明していない

**該当箇所**: `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:4-14` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:28-34` / `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:2-7`

**問題点**: 3 OS いずれもファイル冒頭のコメントが「入力系 Cell 5 種を 1 画面に並べて目視確認するデモ画面」とだけ説明しており、デモではない検証専用 Section が末尾にある事実に触れていない。Section 直上のコメントと footer で局所的には説明されているため誤りではないが、[ソースコメント規約](kasane/concepts/cross/conventions/comment-policy.md) が求める「そのファイルだけを読んでいる人にとって意味が通る」水準からは、冒頭の構成説明と実体に一段のずれがある。

**推奨修正**: 各ファイル冒頭の構成説明に一文足す (例: 「末尾の Section は 5 種のデモではなく、下部配置の EntryCell でキーボード回避を確認するための検証用」)。3 OS 同時に入れること。

## アクションプラン

優先度順:

1. **(Major / 必須)** 3 OS で Sample を起動し、最下部までスクロールした状態 (できればフォーカス後の状態も) の静止画を `evidence/` に保存する。想定どおり下半分に来ていなければ、Section の位置・構成を見直す
2. 検証手順 (フォーカスのみで判定する。文字入力は再構成を誘発するので観察の後) を exploration.md に一行残す
3. 検証用 Section の寿命 (恒久デモか、撤去予定か) をオーナーに確認し exploration.md に明記する
4. 3 OS のファイル冒頭コメントに、末尾 Section が検証用である旨を追記する (3 OS 同時)

1 以外はコード修正を伴わない (4 のみコメント追記)。1 の結果しだいでコードの見直しが要る可能性がある。
