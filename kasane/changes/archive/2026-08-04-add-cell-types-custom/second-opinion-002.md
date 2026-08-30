# セカンドオピニオン: add-cell-types-custom (002 回目・code-review)

**相方**: codex / **日付**: 2026-08-03 / **対象**: 実装一式 (iOS / Android / Sample の新規19ファイル + 変更11ファイル)
**対応するホスト側レビュー**: review-001.md (同一入力・同一スナップショット。相方にはホスト側の結論を渡していない)

> 番号が 002 なのは、001 が提案段階の spec-review モードで既に使われているため。code-review モードとしては初回。

---

## 判定: CHANGES_REQUESTED

Critical 0件、Major 2件、Minor 1件です。ホスト報告の全テスト成功は確認済み事実として扱い、ビルド・テストは再実行していません。

## 指摘事項

### [🟠 Major] iOS の型消去が content の型変更を等価と判定し得る

**該当箇所**: `ios/Sources/KsSettingsViewUI/CustomCell.swift:127`、`ios/Sources/KsSettingsViewUI/CustomCell.swift:197`

**問題点**: `C` を `AnyHashable` だけへ消去して比較していますが、`AnyHashable` は異なる数値型を同値として正規化する場合があります。例えば同じ ID の content が `Int(1)` から `Double(1.0)` へ変わっても等価になり得ます。

一方、builder の引数型はそれぞれ `Int` と `Double` であり、builder 自体は等価性から除外されています。そのため DSL 差分検出が再バインドを省略し、古い builder/content の表示が残ります。「content の更新で表示が変わる」という SHALL に違反します。Android 側には異なる content 型を非等価とするテストがありますが、iOS 側には対応する境界テストがありません。

**推奨修正**: 型消去時に `ObjectIdentifier(C.self)` などの型トークンを保持し、content の値と型の両方を equality/hash に含めてください。`withDSLID` / `withStyle` でも保持し、少なくとも `Int(1)` と `Double(1.0)` が非等価になり、DSL が content update を発行するテストを追加してください。

### [🟠 Major] Android の無効化はポインタイベント以外の操作を抑止しない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:123`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:144`

**問題点**: `isEnabled=false` の処理は `pointerInput` の Initial pass でポインタイベントを消費するだけです。子の `clickable`、`Button`、`Slider` が公開する Compose semantics action は残り、TalkBack によるアクティベーションなどはポインタ配送を通らず直接実行されます。キーボードやフォーカス経由の操作も同様に遮断されません。

したがって、タッチ操作のテストは通っても、アクセシビリティ経由では無効な content を操作でき、「content 内部の操作も抑止される」という SHALL を満たしません。

**推奨修正**: hosting View と semantics subtree を無効状態として扱い、子孫の操作 action・フォーカス・キー入力も遮断してください。単にポインタを消費するだけでなく、`ComposeView.isEnabled` の同期や disabled semantics、子孫 action の除去・上書きを組み合わせる必要があります。TalkBack 相当の semantics `OnClick` / `SetProgress` を直接実行しても callback が発火しないテストも追加してください。

### [🟡 Minor] 新規コメントがプロジェクトのコメント規約に違反している

**該当箇所**: `ios/Sources/KsSettingsViewUI/CustomCell.swift:6`、`ios/Sources/KsSettingsViewUI/CustomCellView.swift:7`、`ios/Sources/KsSettingsViewUI/KsChevronAppearance.swift:11`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:190`、`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/CustomCellDemoScreen.kt:44`

**問題点**: `kasane/changes/...`、`design.md Decision N`、change-id、mock パスなど、アーカイブされる作業資料や議論通番をソースコメントから参照しています。テストと Sample を含め、`comment-policy.md` が明示的に禁止している形式です。同種の参照が新規 Swift ファイル・テスト・両 Sample に広く存在します。

**推奨修正**: コメントを単独で理解できる現在形の説明へ書き換えてください。設計判断への参照が必要な箇所だけ `core/ADR-0014` または `core/ADR-0015` を使用し、spec/design/mock/change-id への参照は削除してください。

---

## 突き合わせ結果

ホスト側 (review-001.md) と相方の指摘は **重複ゼロ**。双方一致の項目がなく、互いの見逃しを補完する形になった。降格 0 件・採用 4 件・未解決 0 件。

| # | 指摘 | 出典 | 採否 | 判定理由 |
|---|---|---|---|---|
| 1 | Android `isClickable` が視覚状態契約に違反 (`CustomCellViewHolder.kt:73-77`) | ホスト Major | **確定** | `cell-visual-states.md:21` と `LabelCellViewHolder.kt:37` という既存規約・既存実装の2つの証拠あり。オーナーの「プラットフォーム間で揃える」方針とも整合 |
| 2 | iOS `AnyHashable` が異なる数値型を等価と判定 | 相方 Major | **採用** | 相方のみ + 根拠強。`AnyHashable` の数値正規化は Swift の既知挙動で、該当箇所が特定され、spec「content の更新で表示が変わる」に対する実害シナリオが具体的。iOS 側に境界テストが無いという指摘も事実 |
| 3 | Android の無効化が semantics 経由の操作を遮断しない | 相方 Major | **採用** | 相方のみ + 根拠強。spec「content 内部の操作も抑止される SHALL」に対する明確な穴。実装者自身が「検証したのは `Modifier.clickable` に対する遮断のみ」と申告しており、その未検証領域を的確に突いている |
| 4 | コメントが `changes/` パス・`Decision N` を参照 | 相方 Minor | **採用** | `comment-policy.md` の「禁止する参照」に明確に該当 (アーカイブ文書のパス / Decision 番号の二重違反)。過去に count 3 で lint/hook 化された既知の頻出パターンでもある |
| 5 | deviation.md への未記録 | ホスト Minor | **確定・対応済** | オーケストレーターが `deviation.md` を作成し、Decision 4 / Decision 5 の差異とオーナー指示を記録済み |
| 6 | iOS 全件実行で `InputCellsTests` が 1 回だけ失敗 | ホスト Minor | **確定** | 再現せず (単体・2クラス同時・全件再実行いずれも成功)。Simulator 起動待ちのタイミング起因の可能性が高い。修正サイクル後の再実行で監視する |
| 7 | 「高さの自動追従」の検証が再バインド経路のみ | ホスト Minor | **確定** | builder 内の `remember` / `@State` だけが変わる経路 (利用者が最も自然に書く形) のテスト・デモを追加する |
| 8 | Suggestion 3件 (`setContent` 方式の非対称 / `registerCustomCell` 未使用引数 / tasks 4.3 未了) | ホスト Suggestion | **一部採用** | 未使用引数は対応。`setContent` 方式は design.md 準拠であり現状維持 (変えるなら design 側の判断)。tasks 4.3 は verify 工程で解消 |

### オーナー指摘 (レビューとは別経路、同格に扱う)

| # | 指摘 | 出典 |
|---|---|---|
| 9 | iOS 動的高さの遷移アニメーションのブレ (展開時に content が上へ飛び出してから落ちる) | オーナー実機操作 2026-08-03 |
| 10 | 無効時の淡色化を Android でも行い iOS と揃える | オーナー指示 2026-08-03 (deviation.md 記録済み) |
| 11 | Android Sample の Slider の `thumbColor` 上書きを外し M3 標準に戻す | オーナー指示 2026-08-03 |
