# Live Session: customcell-android-maui-perf

対象: MAUI Android CustomCell デモのスクロール性能 — Release ビルドでの体感確認と、検証手順の落とし穴 (README が Debug 手順のみ・csproj に Release 最適化なし) への手当て
開始: 2026-08-28

前提 (探索済み、詳細は exploration.md): カクつきの支配項は Debug ビルドのオーバーヘッドと実測特定。Release は native 同等以上 (Janky 4.6% / p90 12ms)。Pixel 6a (実機) に Release ビルドをインストール済み。

## 試行ログ (append-only)

- Release ビルドを Pixel 6a にインストールし gfxinfo 計測 → Debug: Janky 31.7% / p90 121ms、Release: Janky 4.6% / p90 12ms、native 基準: 6.1% / 28ms → ユーザー体感確認待ち
- ユーザーが Pixel 6a 実機で Release ビルドを操作 → 「全然なめらか」と体感でも解消を確認 → 採用 (原因確定: Debug ビルドのオーバーヘッド)
- README に「性能確認は Release で」節を追記 (samples/maui/README.md:124、ワーカー実施) → 継続 (次試行の結果次第で見直し)
- ユーザー指摘「毎回 Release で検証はビルドが遅くて困る」→ Debug 構成限定 `UseInterpreter=false` (Hot Reload 用インタープリタが Debug 低速の支配項という仮説) を csproj に追加して Debug のまま実行性能を上げる試行へ → 継続 (ビルド時間計測中)
- csproj:32-40 に android+Debug 条件 PropertyGroup 追加 (なし→UseInterpreter=false、Hot Reload 喪失をコメント明記)。Debug warm build 57 秒 → Pixel 6a 実機計測: Janky 8.8〜19.4% / p90 53〜65ms (インタープリタ Debug 31.7%/121ms から大幅改善、Release 4.6%/12ms には未達。2周目悪化は最適化なし IL + 発熱の影響と推定) → ユーザー体感確認待ち (未達なら Optimize=true 追試)
- ユーザー体感 (Debug+UseInterpreter=false): 「カクカクはする。若干マシな気がする」→ Debug の滑らかさ追求は打ち切り (開発時はこれで問題ない、あくまでパフォーマンスの問題)。Optimize=true 追試は不要
- 方針確定: 「パフォーマンス計測・描画速度調査は Release ビルドで行う」を concepts に記録する (README は利用者向けのため不適切 → 追記を取り消す)
- csproj の UseInterpreter=false も取り消しで確定 (性能確認は Release で行うため、Hot Reload を犠牲にする価値なし)。concepts は既存の同カテゴリ文書があれば節追記でも可 (ユーザー確定)

## 確定作業ログ

- README 追記を削除して復帰、csproj の UseInterpreter ブロックを削除して復帰 (git status で samples/ 差分なしを確認、ワーカー実施)
- concepts 新規作成: maui/architecture/performance-verification.md (type: policy)。既存への追記は候補 (cross/conventions/test-execution.md = 別軸、android/architecture/build-toolchain.md = ドメイン違い) に自然な受け皿がなく新規とした。maui/index.md と log.md も更新
- メイン側検証: diff は kasane/ 配下のみ・端末シリアル混入なし・concepts のリンク先 2 件の実在確認
- 独立レビュー 001: CHANGES_REQUESTED (Major 2: iOS 断定の留保欠落・証跡なし / Minor 3 / Suggestion 3)。対応: evidence/gfxinfo-pixel6a.md 作成 (メイン)、構造課題 2 件を maui-android-customcell-embed-perf として簡易起票 (メイン)、concepts 本文修正 5 点 (ワーカー)。rules.md の「主な type」更新は蒸留時判断として持ち越し
- 独立レビュー 002: CHANGES_REQUESTED (Minor 1: log.md の created 行が修正前本文のまま / Suggestion 3)。同時にオーナー指摘「規約系は conventions カテゴリへ (cross/conventions と揃える)」→ maui/conventions/ を新設して移設 + rules.md の platform カテゴリ表へ conventions 行追加 (オーナー合意)。log 行修正・「振れ幅」論拠修正もワーカーへ指示
- 独立レビュー 003: **APPROVED** (Minor 1: summary.md 最終状態節の旧パス → メインが修正済み / Suggestion 2 は蒸留時申し送りとして summary.md に記録)

## 決定事項

- カクつきの原因は Debug ビルド起因で確定 (実測 + 実機体感)。コード変更は行わない
- csproj への Release 最適化プロパティ追加は不要 (無設定の Release で十分な性能を実測確認済み)
- 手当ては samples/maui/README.md への性能検証手順 (Release) の追記のみ

## エスカレーション・スコープ外の発見
