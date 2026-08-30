# レビュー結果: entrycell-keyboard-avoidance-check (002 回目)

**日付**: 2026-08-24
**判定**: CHANGES_REQUESTED

## サマリー

review-001 の 4 件 (Major 1 / Suggestion 3) はいずれも対応が入っており、証跡 7 点は実際に画像を開いて内容を確認した — iOS / Android / MAUI(iOS) の 3 環境でコンテンツが実際にせり上がっていることが画像から読み取れる。前回の Major は解消と判断する。

ただし新たに 1 点の Major がある。exploration.md は「3 OS すべてでキーボード回避が機能することを確認した」と断定しているが、証跡が存在しないケースが 2 つ残っている — **MAUI の Android ターゲットは一度も動かしていない**、および **iOS の「署名」(最下部セル) はスクリーンショットがない**。とくに前者は、MAUI Sample の `MainActivity` が `WindowSoftInputMode` を指定していないという**具体的な差分**を抱えており、native Android Sample の結果から推定してよい根拠がない。これは本 change の作業中に `kasane/lessons/inbox/visual-verification-limited-to-primary-platform.md` として捕捉されたばかりのパターン (「主要 1 OS で確認できたので他 OS も同様」と推定しない) に、記録の側で再び該当している。

コードそのものへの指摘は前回同様ゼロ。修正対象は exploration.md / evidence.md の記述 (と、選ぶなら追加の実機確認) に限られる。

## review-001 指摘への対応状況

| # | 指摘 | 対応 | 判定 |
|---|---|---|---|
| Major | 3 OS の視覚確認と証跡が無い | `evidence/` に静止画 7 点 + 索引 `evidence.md` を追加。3 環境でせり上がりを確認 | ✅ 解消 (ただし下記 Major-1 の範囲外れが残る) |
| Suggestion 1 | 検証用 Section の寿命が未記録 | exploration.md 未決の論点に「恒久デモとして残す (推奨)・オーナー確認待ち」を追記 | ✅ 対応 |
| Suggestion 2 | 検証手順 (フォーカスのみで判定) が未記録 | exploration.md 検証結果に手順メモを追記。iOS Simulator のハードウェアキーボード無効化まで書かれており、再現性が上がっている | ✅ 対応 |
| Suggestion 3 | ファイル冒頭コメントが検証用 Section に触れていない | 3 OS すべての冒頭コメントに追記 | ✅ 対応 |

## 確認した観点と実行結果

### 証跡の実地確認 (画像を開いて内容を照合)

| ファイル | 画像から読み取れたこと | 索引の記述との一致 |
|---|---|---|
| `evidence/ios-bottom.png` | 最下部までスクロールした状態で「EntryCell（下部配置）」ヘッダ・メモ・署名・footer が画面の下 1/3 に並ぶ。**目的だった「下半分配置」は達成されている** | 一致 |
| `evidence/ios-memo-focused.png` | ソフトウェアキーボードが表示され、コンテンツ全体が上昇 (「予約日」行が bottom 時より明確に上へ移動)。メモがキーボード上端の直上にある | 一致。ただし**署名はキーボードに隠れて見えない** (下記 Major-1) |
| `evidence/android-bottom.png` | 同セクションが画面下部に表示される | 一致 |
| `evidence/android-memo-focused.png` | IME 表示、メモが IME 直上 | 一致 |
| `evidence/android-sign-focused.png` | 最下部の署名が IME 直上まで上昇。**最下部セルという最も厳しいケースが押さえられている** | 一致 |
| `evidence/maui-bottom.png` | 同セクションが画面下部に表示される | 一致 |
| `evidence/maui-memo-focused.png` | コンテンツが上昇し、メモ・署名の**両方**がキーボード上端より上にある | 一致 (索引はメモのみ記載だが、画像は署名も上にあることを示している) |

置き場・命名は ksn-core references/evidence.md に適合 (`evidence/` 直下、静止画のみ、動画なし、`<画面>-<状態>.png` の kebab-case)。change 配下に画像が散在していないことも確認した。

### ビルド (コメント追記後の再確認)

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS Sample | `xcodebuild build -scheme KsSettingsViewSample -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | `** BUILD SUCCEEDED **` |
| Android Sample | `./gradlew :app:compileDebugKotlin` (`samples/android`) | `BUILD SUCCESSFUL` |
| MAUI Sample (iOS) | `dotnet build -f net10.0-ios` | 成功 (0 警告 / 0 エラー) |
| MAUI Sample (Android) | `dotnet build -f net10.0-android` | 成功 (0 警告 / 0 エラー) — **ビルドは通るが一度も起動されていない** |

テストは review-001 で実行済み (Android 2620 tests / 0 failures、MAUI 439 tests / 0 failures)。今回の差分はコメント追記と `kasane/` 配下のみで、テスト対象コードに変化はない。

### lint

- `python3 scripts/comment-policy-lint.py` — 禁止 0 件。追加された 3 OS の冒頭コメントも規約本文 (禁止参照・禁止記述類型) から目視で判定し、違反なし
- `python3 scripts/identity-lint.py` — 0 件。ただし**この 0 件は evidence の検査結果ではない** (下記 Minor-1)
- `python3 scripts/local-path-lint.py` — 0 件

### コード差分

review-001 時点の差分に対する追加は 3 OS のファイル冒頭コメントのみ。Section・Cell・文言・バインディングに変更はなく、review-001 で確認した一致 (3 OS 一字一句一致・Section 8 個・様式の踏襲・MAUI の TwoWay バインディング) はそのまま維持されている。

## 指摘事項

### [🟠 Major-1] 記録された結論が証跡の範囲を超えている (MAUI Android 未検証 / iOS 署名の証跡なし)

**該当箇所**: `exploration.md:30`、`exploration.md:32`、`exploration.md:34-35`、`evidence/evidence.md:17`

**問題点**: exploration.md は「3 OS すべてでキーボード回避 (フォーカス時のせり上がり) が**機能することを確認**した」と断定しているが、確認していないケースが 2 つある。

1. **MAUI の Android ターゲットが一度も動いていない。** `samples/maui/KsSettingsView.Sample.Maui` は `net10.0-ios;net10.0-android` の 2 ターゲットを持つが、証跡は iOS ターゲットのみ。しかも推定を許さない具体差分がある — MAUI Sample の `MainActivity` (`samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs`) は `[Activity]` 属性に **`WindowSoftInputMode` を指定していない**。native Android Sample が実機で正しくせり上がったことは、MAUI の Activity 構成でも同じになる根拠にならない。exploration.md:35 が挙げる根拠「MAUI: ネイティブ挙動」は、まさに native 側の挙動が Activity 設定に依存するため、この 1 語で 2 ターゲット分を代表させられない。本 change の作業中に捕捉された `kasane/lessons/inbox/visual-verification-limited-to-primary-platform.md` (「主要 1 OS で確認できたので他 OS も同様、と推定してはいけない — プラットフォームごとに機構が異なり、結果は独立に検証するまで不明」) が、そのまま当てはまる。
2. **iOS の「署名」に証跡がない。** exploration.md:32 は「メモ・署名ともフォーカスでせり上がり」と断定し、evidence.md:17 は「目視確認済み (証跡は memo 分のみ保存)」と補足するが、`kasane/lessons/process.md` L-003 は証跡の実在をレビューの判定条件に据えている。しかも `ios-memo-focused.png` を見ると、メモにフォーカスした状態では**署名がキーボードに隠れて見えない** — 最下部セルは iOS で唯一未撮影のケースであり、「隣のセルが上がったから最下部も上がる」は Android で別途撮り直されているのと同じ理由で自明ではない (Android 側は `android-sign-focused.png` をきちんと撮っている)。

放置すると、蒸留で「3 OS でキーボード回避は標準機構により成立する」が検証済みの知識として長命層に入り、後から未検証の組み合わせだったことを追えなくなる。

**推奨修正**: どちらかを選ぶ。混ぜてもよい。

- **A (強い)**: MAUI Sample を Android で起動して最下部→メモ/署名フォーカスの静止画を `evidence/` に追加し、iOS の署名フォーカスも 1 枚撮る。断定を維持できる
- **B (最小)**: 断定を証跡の範囲に合わせる。exploration.md:30 を「iOS / Android / MAUI(iOS ターゲット) で機能することを確認した」に、:32 を証跡のあるケースだけの記述に直し、**未決の論点に「MAUI Android ターゲットのキーボード回避は未検証 (`MainActivity` に `WindowSoftInputMode` 指定がないため native Android の結果から推定できない)」を追加**して追跡を残す。evidence.md:17 の「証跡は memo 分のみ保存」も、断定ではなく未取得として書き直す

### [🟡 Minor-1] evidence.md に Android 端末シリアルの断片が書かれている

**該当箇所**: `evidence/evidence.md:9`

**問題点**: 環境欄が `Android 実機 (Pixel 系 / adb: 0B26...)` となっており、端末シリアルの先頭が実値のまま入っている。ksn-core references/evidence.md はプレースホルダ語彙として Android シリアルに `<android-serial>` を定めており、証跡本文に実値を書かない規約になっている。`kasane/` はコミットされるため、入った時点で履歴に残る。

`python3 scripts/identity-lint.py` が 0 件で通るのは適合の証明にならない。理由は 2 つあり、どちらもこの行を素通りさせる。

- lint モードは `git grep` で**追跡ファイル**から候補を絞る。`kasane/changes/entrycell-keyboard-avoidance-check/` はまだ untracked のため、`evidence.md` は検査対象に入っていない
- 仮に追跡済みでも、検出パターンは `serial[=:]` 形式 (`scripts/identity-lint.py:78`) と `adb ... -s <値>` 形式 (`:79`) の 2 つで、`adb: <値>` という書き方はどちらにも一致しない

**推奨修正**: `adb: 0B26...` を落として `Android 実機 (Pixel 系)` にするか、識別が必要なら `<android-serial>` に置き換える。証跡としてシリアルが果たしている役割はないため、削除で足りる。

### [🔵 Suggestion] identity-lint が `adb: <値>` 形式を拾わない

**該当箇所**: `scripts/identity-lint.py:79`

**問題点**: Minor-1 で判明したとおり、`ADB_S` は `adb ... -s <値>` の形しか見ておらず、人が証跡に書くときに自然な `adb: <値>` / `シリアル: <値>` のような書き方を拾わない。今回は人手のレビューで見つかったが、次も見つかるとは限らない。

**推奨修正**: 本 change のスコープ外のファイルなので、ここでの修正は求めない。簡易起票して別 change で `ADB_S` の別形を足すかどうかをオーナーに判断してもらう。

## アクションプラン

1. **(Major-1 / 必須)** exploration.md の結論を証跡の範囲に合わせる。最小対応なら記述の修正 + 未決の論点に「MAUI Android 未検証」を追加、強い対応なら MAUI Android と iOS 署名の証跡を追加して断定を維持する
2. **(Minor-1 / 必須)** `evidence/evidence.md:9` から端末シリアルの断片を落とす
3. (Suggestion) identity-lint の `adb: <値>` 未検出を簡易起票するかオーナーに確認する

1 と 2 はいずれもテキスト修正で閉じる (A を選ぶ場合のみ実機/シミュレータ操作が伴う)。コードへの修正要求はない。
