# レビュー結果: english-diagnostic-messages (002 回目)

**日付**: 2026-09-07
**判定**: CHANGES_REQUESTED

## サマリー

前回の指摘 5 件のうち 4 件 (binding の glob・`optionalDate` の診断文言・「対象外」節の根拠の絞り込み・concepts へのリンク) は解消を確認した。3 platform のテストも全件緑 (iOS 1027 / Android 2854 / MAUI 520、いずれも 0 failures)、lint も 0 件で、コード側の英語化に退行はない。残る 1 件 — Major に挙げた「句点条項が実態と合っていない」— は**書き換えの軸を取り違えており未解消**。新しい表は句点の有無を「C# か Swift / Kotlin か」で分けているが、リポジトリの実態は言語ではなく**メッセージの形 (1 文の短句か、複数の文を連ねた散文か)** で分かれている。この表のまま確定すると、本変更が触れていない Kotlin のログ 6 箇所と Swift の `fatalError` 1 箇所が規約成立と同時に違反になり、規範層が誤った修正を指示する状態になる。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (always) |
| cross/test-execution.md | テストを実行するとき・テスト結果を報告するとき |
| cross/diagnostic-message-language.md | 本変更が新設した規約そのもの (レビュー対象) |
| cross/public-identifiers.md | 新規規約の frontmatter / 本文形式の比較対象として参照 (規約自体の適用はなし) |

適用外と判定: cross/sample-parity.md (`samples/` に触れていない)、cross/runtime-behavior-verification.md (実行時挙動の不具合修正ではない)、cross/user-skill-api-listing.md (`skills/` に触れていない)、cross/aiforms-origin-reference.md (未移植機能の実装ではない)、cross/release-procedure.md (リリース作業ではない)、ios/ · maui/ の各ドメイン規約 (Swift 6 言語モード適合・検証ホスト実行のいずれにも当たらない)。

ksn-core の handbook 形式規約 (`references/handbook.md`) と rule 記述規約 (`references/rule-style.md`) を新規規約の判定基準に用いた。`kasane/lessons/code-review.md` の重点観点 L-001 (ミューテーションによる検出力の実測) は、文言のみの変更でアサーション検出力が争点にならないため今回も適用しない。「指摘しないこと」は未昇格。

## 前回指摘の解消状況

| review-001 の指摘 | 状態 | 確認内容 |
|---|---|---|
| 🟠 句点条項が実態と一致しない | **未解消** (軸の取り違え) | 下の Major を参照 |
| 🟡 binding 向け glob がどのパスにも一致しない | 解消 | `maui/macios/KsSettingsView.Binding.iOS/**` と `maui/android/KsSettingsView.Binding.Android/**` の 2 本になり、いずれも実在ディレクトリに一致する |
| 🟡 `optionalDate` の「falling back to the default value」 | 解消 | `diagnose` に `fallback` 引数が追加され、`time` → `00:00` / `date` → `1970-01-01` / `optionalDate` → `null` (Kotlin) · `nil` (Swift) を出す。戻り値と一致する |
| 🔵 「対象外」節の UI 文言の根拠が実態より広い | 解消 | 根拠が「選択面の文言」に限定され、「それ以外の UI 文言 (アクセシビリティラベル等) の言語方針は本規約では定めない」と明記された |
| 🔵 concepts への参照がリンクでない | 解消 | `../../concepts/core/cells/picker-selection-surface.md` への相対リンクになり、参照先の実在も確認した |

## 検証したこと

- **ビルド / テスト** (cross/test-execution.md の手順で全件実行、絞り込みなし)
  - iOS: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → バンドル集計行の合算で **1027 tests / 0 failures** (Bridge 166 / Core 88 / SwiftUI 96 / TestSupport 7 / UI 670)、`** TEST SUCCEEDED **`
  - Android: `./gradlew test --rerun-tasks` → `build/test-results/test*UnitTest/TEST-*.xml` 222 ファイルの合算で **2854 tests / 0 failures + 0 errors**、`BUILD SUCCESSFUL`
  - MAUI: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → **520 tests / 0 failures**
- **lint**: `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも 0 件 (comment-policy は検査対象 784 ファイル)。`doc-structure-lint.py` は `diagnostic-message-language.md` への指摘なし (exit=1 は roadmaps 側の既存債務)
- **英語化漏れ**: 追跡下の本体ソース (`ios/Sources/` / `android/*/src/main/` / `maui/KsSettingsView.Maui/` / `maui/macios/KsSettingsView.Binding.iOS/` / `maui/android/KsSettingsView.Binding.Android/`) を日本語文字で grep し、残るヒットはすべてコメント (`//` · KDoc · XML コメント · `.csproj` コメント) で、実行時・コンパイル時に出る文字列は **0 件**
- **規約と実装の照合**: 本変更が書いた 7 文字列を新しい表に照らすと 7 件とも適合する (C# 1 件は句点あり、Swift / Kotlin 6 件は句点なし)。**規約の実効範囲を本変更の外まで広げて照合したところ不適合が 7 箇所**あり、それが下の Major
- **frontmatter の glob**: `ios/Sources/**` / `android/*/src/main/**` / MAUI 3 本すべてが実在パスに一致することを確認

## 指摘事項

### [🟠 Major] 句点条項の軸が「言語」になっているが、実態の軸は「メッセージの形」

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md`「書き方」節の表

**問題点**:

新しい表は次の 2 行だけを持つ。

| 言語 | 句点 |
|---|---|
| C# | 完全な文は句点で終える |
| Swift / Kotlin | 完全な文でも句点を付けない |

しかしリポジトリの Swift / Kotlin は「句点を付けない」で統一されていない。**1 文で終わる短句 (例外・assert) は句点なし、複数の文を連ねた散文 (主にログ) は各文を句点で区切る**、という形での分かれ方をしている。後者は本変更が触れていない箇所に 7 件ある:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:159-160` — `"NumberPickerCell(id=…) has invalid range: min=… > max=…." + " Selection sheet will not be shown."`
- 同 `:169-170` — `"… (min=…, max=…, step=…). Selection sheet will not be shown."`
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialog.kt:124` — `"DatePickerCell(id=…) has $reason. Calendar dialog will not be shown."`
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:252` と `:263-264` — 同じ形の 2 件
- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:425-426` — `"bind(): findViewTreeLifecycleOwner() returned null. " + "Diff collection will be retried on onAttachedToWindow()."`
- `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:155` — `fatalError("makeController() is supported only for store-backed KsSettingsView. Use DSL via SwiftUI hierarchy.")`

handbook は規範層であり、食い違ったときに直すのはコードの側になる (`kasane/handbook/index.md`)。この表のまま確定すると、上の 7 箇所は規約成立と同時に違反になり、レビューでの判定 (本文書の「適用契機」が定める運用) は「句点を落とせ」という**実際には誤った修正**を指示することになる。複数の文を連ねた診断で文の区切りの句点を落とすのは英文として退行であり、規約が守らせたい姿ではないはずである。

同じ理由で表は MSBuild の Error / Warning テキストも扱えていない。「規則」節はこれを対象に挙げているのに、`maui/KsSettingsView.Maui/buildTransitive/KsSettingsView.Maui.targets:28` の Text は 2 文構成で両方に句点があり、表のどの行にも当てはまらない。

前回の Major は「規約が既存メッセージと本変更の成果物の双方に合っていない」だった。成果物側 (7 文字列) は今回適合したが、既存メッセージ側は軸の取り違えによって未解消のまま残っている。

**推奨修正**:

表の軸を言語からメッセージの形へ移す。実態をそのまま写すなら次の 3 行で全件を説明できる (確認した範囲では例外なし):

| メッセージの形 | 句点 | 例 |
|---|---|---|
| Swift / Kotlin の 1 文で終わる短句 (例外・assert・deprecated 警告) | 付けない | `IllegalStateException("Cell type … is not registered in KsCellRegistry")` |
| Swift / Kotlin / MSBuild の複数の文を連ねた診断 | 各文を句点で区切る | `"DatePickerCell(id=…) has $reason. Calendar dialog will not be shown."` |
| C# の例外 message | 1 文でも句点で終える | `InvalidOperationException("A null Section cannot be placed in SettingsView.Root.")` |

言語軸を残したいのであれば、Swift / Kotlin の行を「1 文なら句点を付けない。複数の文を連ねる場合は各文を句点で区切る」に書き換えるだけでも上の 7 箇所は違反でなくなる。いずれの案でもコード側の一斉修正は要らない。

### [🔵 Suggestion] 診断に載せる既定値が定数と二重定義になっている

**該当箇所**:
- `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeValueTransport.kt:70,80` (`fallback = "00:00"` / `"1970-01-01"`) と `:208` (`EPOCH_DATE`)、`:71` (`LocalTime.MIDNIGHT`)
- `ios/Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift:37-40,48-51` (`fallback:` の文字列と `parse("00:00"…)` / `parse("1970-01-01"…)`)

**問題点**: 診断に出す既定値が、実際に返す値とは別のリテラルとして書かれている。`EPOCH_DATE` や `LocalTime.MIDNIGHT` を変えても診断文だけ古い値を出し続け、しかも「診断が事実と一致しない」という今回まさに直した種類の欠陥として再発する。テストが文言に依存していないため機械的にも捕まらない。実害はまだ無い (どちらの既定値も固定の契約) ので優先度は低い。

**推奨修正**: 返す値そのものを整形して渡す (Kotlin なら `fallback = dateText(EPOCH_DATE)`、Swift なら整形済みの既定値を 1 箇所で作って両方に使う)。あるいは既定値の定義とリテラルが隣り合うように 1 箇所へまとめる。

### [🔵 Suggestion] 規約の例文と iOS 実装で先頭の大小文字が食い違う

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md`「書き方」節 3 番目の項目と `ios/Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift:193`

**問題点**: 規約は `"The time string '25:00' does not match the transport format 'HH:mm'; using 00:00 instead"` を例に挙げるが、iOS の実装は接頭辞 `KsSettingsViewBridge: ` の後に続けるため `the time string …` と小文字で始まる (Android は Log タグが別なので `The …`)。それぞれの前置きに対しては妥当な選択で退行ではないが、規約の例文を実装からそのまま引いたように見えて実は一致していない。

**推奨修正**: 例文の出典を明示するか (Android 側の文言である旨)、接頭辞を含めた形で引く。あるいは例文を C# 側の 1 本に絞る。

### [🔵 Suggestion] 作業ツリーに別 change の未コミット差分が同居している

**該当箇所**: `kasane/changes/fix-release-central-validation-wait/exploration.md`、`scripts/release/central-portal.sh`

**問題点**: レビュー対象の作業ツリーには、本 change と無関係な `fix-release-central-validation-wait` (Central Portal の検証待ちサブコマンド追加) の差分が含まれている。本 change のスコープ (exploration.md の決定事項) 外であり、本レビューでは内容を評価していない。このまま `git commit -a` 相当で確定すると 2 つの変更が 1 コミットに混ざり、どちらかの revert ができなくなる。

**推奨修正**: commit するファイルを本 change の 8 ファイル (`android/` 2 · `ios/` 4 · `maui/` 1 · `kasane/handbook/cross/` 2 のうち index を含む) に限定し、`scripts/release/central-portal.sh` と `fix-release-central-validation-wait/exploration.md` は別コミットに分ける。

## アクションプラン

1. **Major**: 「書き方」節の表を、句点の軸をメッセージの形 (1 文の短句 / 複数の文を連ねた散文) へ移して書き直す。MSBuild テキストも表で扱えるようにする。コード側へ句点を足す・落とす方向では直さない
2. **Suggestion**: 診断に載せる既定値を、実際に返す値から導出する (Android / iOS の 2 箇所)
3. **Suggestion**: 規約の例文と iOS 実装の大小文字の食い違いを解消する
4. **Suggestion**: commit を本 change のファイルに限定し、`fix-release-central-validation-wait` の差分を分離する
5. 1 は handbook のみの修正のためテスト再実行は不要。2 に手を入れた場合は Android と iOS の全件テストを再実行して件数を報告する
