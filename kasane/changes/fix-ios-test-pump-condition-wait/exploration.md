# Exploration: fix-ios-test-pump-condition-wait

add-verification-ci の実装中 (2026-08-31)、CI 上で flaky が実際に顕在化したため簡易起票。2026-09-01 に探索を実施し、仕分けと方針を確定した。

## 課題 / 動機

iOS テストの待機ヘルパ `KsBridgeTestHost.pump(_:seconds:)` が固定時間待機であり、`kasane/handbook/cross/test-execution.md` の待機規約 (実時間 deadline で区切る・収束条件で判定する・超過時は実測値付きで fail) を満たしていない。

```swift
// ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:50
static func pump(_ attachment: Attachment, seconds: TimeInterval = 0.05) {
    let view = attachment.collectionView
    view.setNeedsLayout()
    view.layoutIfNeeded()
    RunLoop.current.run(until: Date().addingTimeInterval(seconds))
    view.setNeedsLayout()
    view.layoutIfNeeded()
}
```

指定秒数を待つだけで収束条件を見ないため、実行機が混んでいると収束前に assert へ進む。

### 実際に観測された flaky (2026-08-31)

`ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift` の `test_リサイクルを挟んだ再表示で内容が壊れない` (2026-09-01 時点の該当行は 356/357。起票時の 322 はずれた)。

固定 0.3 秒 × 2 の後に「先頭行が画面外へ出た」ことを assert する。GitHub Actions の `macos-26` ランナー上で、**同一 commit に対する 1 回目の実行が失敗し、2 回目は成功した** (run 33356260591、iOS job)。失敗時も 642 件のテスト自体は `0 failures` でスイート集計を通過しており、xcodebuild の最終判定だけが failing を報告している。

## 現状確認 (2026-09-01、scout による全数調査)

- `pump(` は `ios/Tests/` 配下 221 件・30 ファイル。うち **19 件は定義**で、実際の呼び出しは **202 箇所**
- `pump` の定義は共有版 1 個 (`KsBridgeTestHost`) + private コピー 18 個 (UITests 16 / SwiftUITests 2) に散在するが、**待機本体は全定義で完全同一** (差は引数の包み方のみ、既定値もすべて 0.05 秒)
- 条件ベースの待機ヘルパは iOS テスト内に存在しない (Android 側には規約準拠の実例 `awaitConvergence` 等がある)
- 202 箇所の仕分け結果:

| 分類 | 件数 | 内容 |
|---|---:|---|
| A: 収束待ち (作り替え対象) | 156 | セル生成・再利用、accessory attach/detach、Theme/Store 更新の実描画反映など。3 ターゲット全部に分布 |
| B: レイアウト駆動のみ | 16 | 同期的な frame 確定だけを見ている。固定待機自体が不要 |
| C: 負の検証 | 30 | no-op・不達の確認 (「未知 ID の更新で何も変わらない」「dispose 後に表示へ届かない」)。待つべき正の完了条件が存在しない |

- `seconds:` を既定値より長く明示した 30 箇所は**すべて A** (危険地帯そのもの)
- A の完了条件はいずれも「クロージャで Bool を返す」述語に収まる (cellForItem の nil/非nil、タイトル文字列、view の identity 比較、supplementary の存在、frame 反映、factory 実行回数など)
- 詳細な全リスト: 分類台帳 [triage.md](triage.md) (change 内に永続化)
- 追記 (2026-09-01、second-opinion spec-001 の指摘で発覚): `pump(` 検索から漏れる同型固定待機が 4 呼び出し + 1 定義あった (`pumpEntry` 定義 1・呼び出し 2、直接の `RunLoop.current.run(until:)` 2)。いずれも A 相当で台帳へ追加済み。総数は呼び出し 206 (A 160 / B 16 / C 30)・定義 20 に更新

## 検討した選択肢 (却下案と理由を含む)

- **仕分けの進め方**: scout への全数仕分け委譲を採用。先にヘルパの形を決める案 (対象数不明のまま設計する手戻りリスク)、flaky 実例 1 箇所だけの最小対応案 (規約の名指しが未解消のまま残る) は却下
- **移行方式**: A 156 箇所の**一括置換**を採用。段階移行 (flaky 実績・長秒指定の 30 箇所のみ先行) は、固定待機 pump が 120 箇所以上残り規約違反状態が長引くため却下
- **C の扱い**: 条件ベース化の対象外とし、意図を名前で明示した固定時間待機として残す。条件ベース化を強行する案は、正の完了条件が存在せず原理的に成立しないため却下 (ADR cross/0027)

## 決定事項

1. 待機ヘルパは用途別に 3 つの顔へ分離する:
   - **A 用**: 条件ベース待機 (述語クロージャ + 実時間 deadline + ループ内で RunLoop を短く回す + 超過時は実測値付き fail)。汎用 1 本 + 頻出述語の薄い便利関数
   - **B 用**: 待機なしの「レイアウトだけ走らせる」ヘルパ
   - **C 用**: 意図を名前で明示した固定時間待機 (不変性検証用であることが呼び出し側で判別できる名前)
2. 固定時間待機は名前でなく実装パターンで全数を対象にし、A の 160 箇所を一括置換する (旧 `pump` / `pumpEntry` / 直接待機の定義を消し切り、今後のテストが古い形を真似する余地をなくす)。共有は単一定義が受け入れ条件 (成立しなければ実装を止めてユーザーへ報告)
3. 負の検証への固定時間待機の容認は platform 共通の規約解釈として ADR に残す (cross/0027)。handbook `cross/test-execution.md` の「収束を待つアサーション」節への例外追記も実装スコープに含める
4. Android の兄弟 change [[fix-customcell-test-pump-condition-wait]] も同じ解釈 (ADR cross/0027) に従う

## ADR 候補 (作成済み: cross/0027 / 未起票: なし)

- [cross/ADR-0027](../../decisions/cross/0027-negative-verification-fixed-wait-exception.md) — 負の検証は条件ベース待機の対象外とし、意図を明示した固定時間待機で書く (status: accepted)

## 未決の論点

- 条件ベースヘルパの配置: 18 個の private コピーを 3 テストターゲットへどう共有するか (共有テストサポートターゲットの新設 or ターゲット別配置)。具体 API 名と合わせて ksn-propose の design で確定する
- handbook 例外追記の文面 (propose のデルタスペックで確定)

## UI 素材 (ui/references/ の一覧と注釈)

なし (テストのみの変更で UI に触れない)。

## 関連

- [[fix-customcell-test-pump-condition-wait]] — Android の同型問題 (`android/ks-settingsview-ui` のテスト 2 箇所で、反復回数区切りのループ内 `Thread.yield()`)。課題の型と根拠となる規約は同一で、プラットフォームと規模だけが異なる。実装時は待機規約の解釈を揃える (ADR cross/0027 に従う)
- 発見の文脈: `kasane/changes/add-verification-ci` (検証 CI の構築)。CI を必須 status check にしたため、この flaky は今後 PR のマージを不定期にブロックする

## 変更級の推奨: M (確定)

プロダクトコード・公開 API の変更は伴わないが、置換対象が 206 箇所・30 ファイル超・3 テストターゲットに及び、待機ヘルパの設計判断 (3 用途分離・共有方法) と handbook 追記を含むため M とする。
