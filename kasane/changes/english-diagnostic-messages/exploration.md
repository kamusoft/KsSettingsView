# Exploration: english-diagnostic-messages

## 課題 / 動機

姉妹リポジトリ KsDialogs で「例外メッセージ・診断メッセージが日本語で出る」問題が見つかり
(`../KsDialogs/kasane/changes/localize-dialog-error-messages`)、本リポジトリにも同じ問題が
ないかを棚卸しした (2026-09-07、ksn-explore)。

結果: 本体コード (利用者が参照するパッケージに含まれる製品コード) で実行時に日本語が出る箇所は
次の 7 箇所に限られる。それ以外の例外・ログは英語で統一されており、UI 文言の日本語ハードコードは
ない (UI 文言は OS の公開リソース・端末 Locale 由来という契約が concepts にある)。

| 場所 | 種類 | 到達経路 |
|---|---|---|
| `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScope.kt:44-49` | `require` の例外 ×2 (header/headerContent、footer/footerContent の同時指定) | 利用者の誤用。公開 API から確実に到達 |
| `maui/KsSettingsView.Maui/PickerCell.cs:339` | `ArgumentException` (ItemsSource の null 要素) | 利用者の誤用。公開 API から確実に到達 |
| `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeValueTransport.kt:228` (+ `kind` の語 70/80/94 行) | `Log.w` の診断 (輸送書式に一致しない日付/時刻文字列)。Release でも出る | MAUI 層から不正な文字列が来たとき |
| `ios/Sources/KsSettingsViewBridge/KsBridgeValueTransport.swift:193` (+ `kind` の語 37/48/62 行) | 同内容の `print`。`#if DEBUG` のみ | 同上 |
| `ios/Sources/KsSettingsViewUI/CustomCell.swift:138` | `assertionFailure` (content と builder の型不一致) | 内部不変条件 (到達しない前提) |
| `ios/Sources/KsSettingsViewUI/KsCheckBoxView.swift:134`、`ios/Sources/KsSettingsViewUI/SectionBoxDecorationView.swift:70` | `@available(message:)` のコンパイル時警告文 | internal な override。実質リポジトリ内のみ |

周辺の確認:
- テストはメッセージ文言に依存していない (grep のヒットはテスト名と doc コメントのみ)
- `skills/` と README にメッセージの引用はない
- 既存の英語メッセージの流儀: `assertionFailure("KsCellRegistry: no renderer registered for …")`
  (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1034`)、
  `IllegalStateException("Cell type … is not registered in KsCellRegistry")`
  (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:157`)、
  `InvalidOperationException("A null Section cannot be placed in SettingsView.Root.")`
  (`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2129`)
- 「例外・診断メッセージの言語」を定めた規約・ADR は handbook / concepts / decisions のいずれにもない。
  ソースコメントは日本語 (CLAUDE.md、`kasane/handbook/cross/comment-policy.md`) で、これは対象外

## 検討した選択肢 (却下案と理由を含む)

- **A: 既存の英語流儀に揃える文言修正 (採用)** — 公開 API 変更なし、テスト非依存、7 箇所の局所修正で可逆
- B: ローカライズ機構 (Bundle / リソース) を導入 — 却下。開発者向け診断メッセージには過剰で、
  既に英語固定の他メッセージも全部載せ替える必要が出る (M 級以上)
- C: 方針だけ明文化して文言は据え置き — 却下。英語利用者への影響が残る

## 決定事項

- 例外・診断メッセージ (例外の message、ログ、assert / precondition、`@available(message:)` 等の
  開発者向け文字列) は英語固定にする。UI 文言は従来通り OS リソース由来 (自前の文字列を持たない)
- この方針を `kasane/handbook/cross/` の規約 (rule) として明文化する。却下した代替案 (ローカライズ機構)
  の理由を規約本文に一言添える
- 診断ログのガード非対称 (Android `Log.w` は Release でも出る / iOS `print` は DEBUG のみ) は各
  platform の慣行の範囲として据え置き、文言だけ直す (実装時に異論があれば報告)

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

未起票。4 platform を横断して将来のメッセージを制約する決定ではあるが、却下案の理由は規約本文に
収まる分量のため handbook の rule で足りると判断した。

## 未決の論点

- handbook 規約の適用範囲の文言 (samples / verification の例外メッセージを含めるか。現状はすべて英語なので
  含めても実害はない)

## UI 素材 (ui/references/ の一覧と注釈)

(なし)

## 変更級の推奨: S / M / L (理由)

**S**。触るのは 3 platform の 7 箇所の文字列と handbook の rule 1 本。公開 API の変更なし、
テストの文言依存なし、可逆。
