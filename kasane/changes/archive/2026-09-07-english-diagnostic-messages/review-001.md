# レビュー結果: english-diagnostic-messages (001 回目)

**日付**: 2026-09-07
**判定**: CHANGES_REQUESTED

## サマリー

本体コード 7 箇所の日本語メッセージはすべて英語化されており、意味の欠落・退行はない。英語化漏れも独立の grep で確認したところ 0 件で、3 platform のテストは全件緑 (iOS 1027 / Android 2854 / MAUI 520、いずれも 0 failures)、lint も 0 件だった。一方、同梱の新規規約 `kasane/handbook/cross/diagnostic-message-language.md` に 2 点の欠陥がある — 「完全な文は句点で終える」という規則が、既存メッセージの実態にも本変更が書いた文字列自身にも合っておらず、規範層として成立した瞬間に本体コードの大半が違反になる。また `applies-when.paths` の binding 向け glob がリポジトリのどのパスにも一致しない。規約側の修正を求める。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (always) |
| cross/test-execution.md | テストを実行するとき・テスト結果を報告するとき |
| cross/public-identifiers.md | 新規規約の frontmatter / 本文形式の比較対象として参照 (規約自体の適用はなし) |
| cross/diagnostic-message-language.md | 本変更が新設した規約そのもの (レビュー対象) |

適用外と判定: cross/sample-parity.md (`samples/` に触れていない)、cross/runtime-behavior-verification.md (実行時挙動の不具合修正ではない)、cross/user-skill-api-listing.md (`skills/` に触れていない)、cross/aiforms-origin-reference.md (未移植機能の実装ではない)、ios/ · maui/ の各ドメイン規約 (言語モード適合・検証ホスト実行のいずれにも当たらない)。

ksn-core の handbook 形式規約 (`references/handbook.md`) と rule 記述規約 (`references/rule-style.md`) を新規規約の判定基準に用いた。`kasane/lessons/code-review.md` の重点観点 L-001 (ミューテーションによる検出力の実測) は、文言のみの変更でアサーション検出力が争点にならないため今回は適用しない。「指摘しないこと」は未昇格。

## 検証したこと

- **ビルド / テスト** (cross/test-execution.md の手順で全件実行、絞り込みなし)
  - iOS: `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → バンドル集計行の合算で **1027 tests / 0 failures** (Bridge 166 / Core 88 / SwiftUI 96 / TestSupport 7 / UI 670)、`** TEST SUCCEEDED **`
  - Android: `./gradlew test --rerun-tasks` → `build/test-results/test*UnitTest/TEST-*.xml` 222 ファイルの合算で **2854 tests / 0 failures + 0 errors**
  - MAUI: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → **520 tests / 0 failures**
- **lint**: `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも 0 件。`doc-structure-lint.py` は新規文書 `diagnostic-message-language.md` への指摘なし
- **英語化漏れ (観点 b)**: 追跡下の本体ソース (`ios/Sources/` / `android/*/src/main/` / `maui/KsSettingsView.Maui/` / `maui/macios/KsSettingsView.Binding.iOS/` / `maui/android/KsSettingsView.Binding.Android/`) を日本語文字で grep し、コメント行を除いた残りは **0 件**。`KsSettingsView.Maui.targets:28` の MSBuild Error テキストも英語であることを確認した
- **テストのメッセージ依存**: 旧文言の grep ヒットは `ios/Tests/KsSettingsViewBridgeTests/KsBridgeValueTransportTests.swift:51` のテストメソッド名のみで、アサーションが文言に依存する箇所はない
- **観点 d (comment-policy の禁止類型)**: 新規規約本文に履歴記述・作業文書 (change / デルタスペック / review) への参照・ローカル通番はない。適合

## 指摘事項

### [🟠 Major] 新規規約の「完全な文は句点で終える」が、既存メッセージにも本変更の成果物自身にも一致していない

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md`「書き方」節 2 番目の項目

**問題点**:
規約は「完全な文で書く場合は句点で終える」と定め、例として C# の `InvalidOperationException("A null Section cannot be placed in SettingsView.Root.")` のみを挙げている。しかしリポジトリの実態は言語で分かれており、Swift / Kotlin の既存メッセージは完全な文でも句点を付けていない:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:157` — `"Cell type ${cell::class} is not registered in KsCellRegistry"` (主語 + 動詞の完全な文、句点なし)
- 同 `:` の `"viewType $viewType is not registered in KsCellRegistry"`、`RootHeaderFooterAdapter.kt` の `"Unknown viewType $viewType in RootHeaderFooterAdapter"`
- `ios/Sources/KsSettingsViewUI/` の `assertionFailure("... received unexpected cell type: ...")` 群、`fatalError("init(coder:) is not supported")`

さらに、本変更が新しく書いた 7 文字列のうち句点で終えているのは MAUI の 1 件だけで、残りは規約の文面に照らすと違反になる:

- `android/.../compose/DSLScope.kt:45,48` — `"Section: header and headerContent cannot both be specified"` (完全な文、句点なし)
- `ios/Sources/KsSettingsViewUI/CustomCell.swift:138` — `"CustomCell: content type does not match the type expected by builder"`
- `android/.../bridge/KsBridgeValueTransport.kt:228` / `ios/Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift:193` — `"... does not match the transport format '...'; falling back to the default value"`

handbook は規範層であり、食い違ったときに直すのはコードの側になる (`kasane/handbook/index.md`)。この文面のまま確定すると、本体の既存メッセージの大半と、本変更が今書いた文字列の 5 箇所が、規約成立と同時に違反状態になる。規約が要求している実際の姿と、実装が到達した姿が食い違っている。

**推奨修正**:
規約側を実態に合わせて言語別に書き分ける。例:

> - 句点は言語の慣行に従う: C# の例外 message は句点で終える (`"A null Section cannot be placed in SettingsView.Root."`)。Swift の `assertionFailure` / `fatalError`、Kotlin の例外 message、および各 platform のログは句点を付けない

もしくは句点の条項自体を落とし、「文脈を先頭に置く」「原因特定に要る値を補間で載せる」の 2 点だけを書き方として残す。逆にコード側へ句点を足す解は採らない — 本変更が触れていない既存メッセージ (`KsCellRegistry.kt` 等) が今度は違反になり、S 級の範囲を超えた一斉修正が必要になる。

### [🟡 Minor] `applies-when.paths` の binding 向け glob がどのパスにも一致しない

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md` frontmatter `applies-when.paths`

**問題点**:
`"maui/KsSettingsView.Binding.*/**"` と書かれているが、リポジトリに `maui/KsSettingsView.Binding.*` は存在しない。実際の配置は `maui/macios/KsSettingsView.Binding.iOS/` と `maui/android/KsSettingsView.Binding.Android/` で、この glob は 1 ファイルにも一致しない。ksn-core の handbook 形式規約は `paths` を「機械判定できる glob で書く」と定めており、一致しない glob は絞り込みの役目を果たさない。本文の散文は「MAUI の facade と binding」と正しく述べているので意図は明確だが、frontmatter だけを見て担当範囲を判定すると binding のコードが規約の適用外に見える。

**推奨修正**: `"maui/*/KsSettingsView.Binding.*/**"` に直すか、`"maui/macios/KsSettingsView.Binding.iOS/**"` と `"maui/android/KsSettingsView.Binding.Android/**"` の 2 本を明記する。

### [🟡 Minor] `optionalDate` の経路で「falling back to the default value」が実際の戻り値と一致しない

**該当箇所**:
- `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeValueTransport.kt:94` (`optionalDate` からの `diagnose` 呼び出し) と `:228` (文言の定義)
- `ios/Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift:62` (`optionalDate` からの `diagnose` 呼び出し) と `:193` (文言の定義)

**問題点**:
`time()` / `date()` は解釈失敗時に既定値 (`LocalTime.MIDNIGHT` / `EPOCH_DATE`、iOS は `00:00` / `1970-01-01`) を返すため「falling back to the default value」で正しい。しかし `optionalDate()` は同じ `diagnose` を呼んだうえで **null / nil を返す**。診断を読んだ開発者は既定値が入ったと解釈するが、実際には値が入っていない — 原因調査時に誤誘導する。

日本語の原文 (「既定値で構築します」) も同じ不正確さを持っていたので新規の欠陥ではないが、本変更はまさにその一文を書き換える作業であり、隣接する課題として同梱で直すのが妥当。

**推奨修正**: `diagnose` にフォールバック結果を表す引数を足し、`time` / `date` は `"falling back to the default value"`、`optionalDate` は `"returning no value"` (等) を渡す。あるいは両経路で成り立つ言い方 (`"the value could not be parsed"` 止まり) に切り替える。

### [🔵 Suggestion] 「対象外」節が根拠に挙げる UI 文言の契約が、実態より広い

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md`「対象外」節の 1 項目

**問題点**:
「ボタンラベル・既定タイトル・アクセシビリティラベル等」を一括して「自前の翻訳文字列を持たず OS の公開リソース・端末 Locale 由来にする契約が別にあり (concepts の各選択面の記述)」と述べている。concepts が実際にその契約を述べているのは picker / date / time の**選択面**の候補表記と操作ラベルに限られ、以下は自前の英語ハードコードである:

- `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CommandCellViewHolder.kt:74` ほか各 ViewHolder — `contentDescription = "Disclosure indicator"` / `"Checked"` / `"Selected"`
- `ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift:116,120` — `NSLocalizedString("Cancel"/"Done", comment: "")` (ライブラリは .strings を同梱しないため常にキー = 英語が返る)

除外するという結論自体は変わらないが、根拠として書かれた契約が実態より広いため、将来の drift 検査で誤った不一致として拾われる。

**推奨修正**: 根拠の記述を「UI 文言の言語は各 concepts の契約が定めるため本規約の対象外」程度に絞る。

### [🔵 Suggestion] concepts への参照がリンクでなく、参照先を特定できない

**該当箇所**: `kasane/handbook/cross/diagnostic-message-language.md`「対象外」節の `(concepts の各選択面の記述)`

**問題点**: 同じ cross の他の rule (`test-execution.md` / `public-identifiers.md`) は concepts へ `../../concepts/<category>/<concept>.md` の相対リンクで繋いでおり、末尾に「## 関連」節を置いている。本文書の参照は文書名を持たない散文で、読み手が参照先へ到達できない。

**推奨修正**: `../../concepts/core/cells/date-picker-selection-surface.md` などの相対リンクに置き換えるか、「## 関連」節を設けてそこに並べる。

## アクションプラン

1. **Major**: 規約の「書き方」節の句点条項を言語別に書き分ける (または条項を落とす)。本体コードへ句点を足す方向では直さない
2. **Minor**: frontmatter の binding 向け glob を実配置に合わせる
3. **Minor**: `optionalDate` 経路の診断文言を戻り値と一致させる (Android / iOS の 2 箇所)。テスト文言依存はないため回帰リスクなし
4. **Suggestion**: 「対象外」節の UI 文言の根拠を実態の範囲に絞り、concepts への参照をリンク化する
5. 1〜3 を反映後、Android と iOS の全件テストを再実行して件数を報告する (MAUI は 3 の対象外)
