# レビュー結果: entrycell-keyboard-avoidance-check (003 回目)

**日付**: 2026-08-24
**判定**: APPROVED

## サマリー

review-002 の Major-1 / Minor-1 はいずれも解消した。追加された 3 点の証跡 (`ios-sign-focused.png` / `maui-android-bottom.png` / `maui-android-memo-focused.png`) はすべて画像を開いて内容を照合し、記述どおりであることを確認した。とくに MAUI Android は「ビルドは通るが起動していない」状態から、実機で IME 直上までせり上がるところまで押さえられており、`MainActivity` に `WindowSoftInputMode` 指定が無い構成のままで動作する事実も exploration.md に残っている。

証跡は 10 点に増え、exploration.md の断定 (`4 環境すべてで機能する`) は各環境に最低 1 枚の focused 証跡が対応する状態になった — **記録が証跡の範囲を超えている箇所はもう無い**。Critical / Major / Minor なし。残るのは記述の細部と後続判断に関する Suggestion 3 件のみで、いずれもマージを妨げない。

## review-002 指摘への対応状況

| # | 指摘 | 対応 | 判定 |
|---|---|---|---|
| Major-1 | 記録された結論が証跡の範囲を超えている (MAUI Android 未検証 / iOS 署名の証跡なし) | 案 A を採用。MAUI Android を実機検証して 2 点、iOS 署名を 1 点追加。exploration.md:30 の断定を「4 環境 (iOS / Android / MAUI iOS / MAUI Android)」に更新し、:35 に `WindowSoftInputMode` 未指定構成で動作する旨を明記 | ✅ 解消 |
| Minor-1 | evidence.md に Android 端末シリアルの断片 | `evidence/evidence.md:10` は `Android 実機 (Pixel 系)` のみになり、シリアル断片は消えた。ファイル全体を再走査しても識別子の残存なし (`adb` の残り 1 件は `:20` の `adb install` という手順説明で、実値を伴わない) | ✅ 解消 |
| Suggestion | identity-lint が `adb: <値>` 形式を拾わない | 対応方針の記録は見当たらない | 下記 Suggestion-3 で再掲 |

## 確認した観点と実行結果

### 追加証跡の実地確認 (画像を開いて内容を照合)

| ファイル | 画像から読み取れたこと | 索引の記述との一致 |
|---|---|---|
| `evidence/ios-sign-focused.png` (新規) | キャレットが「署名」行の右端にあり、focus 対象が署名であることが判別できる。署名がキーボード上端の直上に位置し、`ios-memo-focused.png` と比べて「予約日」行がさらに上へ移動している = **メモのときより深くスクロールして最下部セルを出している** | 一致。review-002 で唯一未撮影だった iOS 最下部ケースが埋まった |
| `evidence/maui-android-bottom.png` (新規) | 最下部までスクロールした状態で「EntryCell（下部配置）」ヘッダ・メモ・署名・footer が画面下部に並ぶ。native Android と同じ着地 | 一致 |
| `evidence/maui-android-memo-focused.png` (新規) | IME 表示に伴いコンテンツ全体が上昇 (「予約日」行が bottom 時より大きく上へ移動)、メモが IME 上端の直上にある。**MAUI Android でもキーボード回避が成立している** | 一致 |

review-001 / 002 で確認済みの 7 点は変更されておらず (mtime も更新されていない)、再照合は行っていない。

### 証跡の規約適合

- 置き場は `evidence/` 直下のみ。change 配下に媒体ファイルの散在なし (`find` で確認)
- 動画なし、静止画のみ (ksn-core references/evidence.md の「置いてよい / 置かない」に適合)
- ファイル名は `<画面>-<状態>.png` の kebab-case で、環境と状態が名前から判別できる
- 索引 `evidence/evidence.md` は 10 点すべてを環境・確認内容付きで列挙しており、exploration.md:30 から change 相対で参照されている

### lint

- `python3 scripts/identity-lint.py` — 0 件。加えて、lint が untracked を見ない件 (review-002 Minor-1) を踏まえ、`evidence.md` / `exploration.md` を `adb` / `serial` / `udid` / UUID / 絶対パスのパターンで直接走査した。残存は `evidence.md:20` の `adb install` (手順説明・実値なし) のみで、問題なし
- `python3 scripts/local-path-lint.py` — 0 件
- `python3 scripts/comment-policy-lint.py` — 禁止 0 件

### コード差分

review-002 時点から変更なし (4 ファイル / 93 行追加、削除・改変ゼロ)。review-001 で確認した 3 OS 一字一句一致・Section 8 個・様式の踏襲・MAUI の TwoWay バインディング、および review-002 で確認した 4 構成のビルド成功はそのまま維持されている。今回の差分は `kasane/` 配下 (証跡・索引・exploration.md) に限られるため、ビルド・テストの再実行は行っていない。

## 指摘事項

### [🔵 Suggestion-1] exploration.md の未決の論点だけ「3 OS」のまま残っている

**該当箇所**: `exploration.md:42`

**問題点**: :30 の断定は「4 環境 (iOS / Android / MAUI iOS / MAUI Android)」へ更新されたが、:42 の解消メモは「検証の結果、3 OS とも機能しており不要」のまま。同一文書内で検証範囲の呼び方が 2 通りあり、後から読むと :30 と :42 のどちらが検証範囲なのか一瞬迷う。

**推奨修正**: :42 を「4 環境とも機能しており不要」に揃える。1 語の修正。

### [🔵 Suggestion-2] Android 側の「せり上がり」機構は window inset ではなく window の pan に見える

**該当箇所**: `exploration.md:36`

**問題点**: :36 は成立の根拠を「Android: window inset」と記している。ただし証跡を見ると、Android・MAUI Android のいずれも IME 表示時に**画面上端の要素 (画面タイトル・「最後のイベント」行) が画面外へ押し出されている** — `android-bottom.png` / `maui-android-bottom.png` ではタイトルバーと直近イベント行が見えているのに、focused の 3 点ではどれも消えて、コンテンツが status bar の直下で切れている。これは window 全体が上へ動く挙動 (pan) の見え方であり、可視領域が縮んでレイアウトし直される inset/resize とは区別できる (iOS 側は 3 点とも上部の「最後のイベント」行が残っており、実際に挙動が違う)。

この差は本 change の結論 (回避は 4 環境で機能する) を変えないが、:36 の一文は蒸留で長命層に渡りうる「機構の説明」であり、観測と食い違ったまま残すと後続の判断材料としては誤導になる。

**推奨修正**: :36 の Android の記述を観測に合わせる (例: 「Android: IME 表示時に window ごと押し上げられる」)。機構名を断定したくなければ「標準機構により成立 (機構の詳細は未特定)」に留めるのでもよい。ライブラリ側の調査までは求めない。

### [🔵 Suggestion-3] identity-lint が `adb: <値>` 形式を拾わない件の扱いが未定 (review-002 から継続)

**該当箇所**: `scripts/identity-lint.py:79`

**問題点**: `ADB_S` は `adb ... -s <値>` の形しか見ておらず、証跡に書くときに自然な `adb: <値>` 形式を拾わない。今回はレビューで見つけて修正されたが、次も人手で見つかる保証はない。本 change のスコープ外ファイルのため修正は求めないが、扱いが決まっていない。

**推奨修正**: 簡易起票して別 change で `ADB_S` の別形を足すか、見送るかをオーナーに確認する。

## 残っているオーナー判断 (指摘ではない)

- `exploration.md:43` の「検証用セクションの寿命: 恒久デモとして残す (推奨)・オーナー確認待ち」は未決のまま。これは記録され追跡できる状態にあるため、[Sample のプラットフォーム間一致](kasane/concepts/cross/conventions/sample-parity.md) の「追跡なしで放置しない」要求は満たしている。アーカイブ前にオーナー確認を通すこと

## アクションプラン

Suggestion のみで、マージを妨げるものはない。優先度順:

1. `exploration.md:42` の「3 OS」を「4 環境」に揃える (1 語)
2. `exploration.md:36` の Android の機構説明を証跡の観測に合わせる
3. identity-lint の `adb: <値>` 未検出を簡易起票するかオーナーに確認する
4. 検証用セクションの寿命についてオーナー確認を通してからアーカイブする
