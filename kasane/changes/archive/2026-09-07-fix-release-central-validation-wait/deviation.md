# Deviation: fix-release-central-validation-wait

S 級のため足場は exploration.md のみ。以下は「決定事項」との差分と、スコープ外に手を入れた付随修正の記録。

## 決定事項との差 (レビュー指摘による設計変更)

- 「検証の決着は upload の直後、NuGet push より前で待つ」: 決着待ちは `Publish Android to Central Portal` step の**中**ではなく、独立 step `Wait for Central Portal validation` として `Upload deployment id` の後・`Login to NuGet.org` の前に置いた。理由: upload 受理から deployment ID が artifact へ保存されるまでの窓に最大 30 分の待ちが入ると、cancel / job timeout (どちらも `if: failure()` に該当しない) で保留 deployment の回収手がかりが残らない。ID が durable になった後へ待ちを移すことで、回収不能な窓を元の長さ (数秒) に戻した。決定事項「upload の直後・NuGet push より前」は満たす。review-001 Minor 1 起因 (2026-09-07)
- 「handbook リリース手順『失敗したとき』の表の **Maven upload 行**を、待ちの位置が変わることに合わせて直す」: upload 行には触れず、新しい失敗位置の 2 行 (`Maven の検証待ち (上限超過)` / `Maven の検証 (FAILED)`) を追加する形にした。理由: 待ちが独立 step になったことで失敗位置そのものが増え、upload 行の書き換えでは再実行時の挙動を説明できない。upload 行が説明する「検証済みなら upload せず release へ」の経路は新しい行から従来どおり到達する (2026-09-07)

## 付随修正

- [付随修正] `.github/workflows/release.yml` の publish job `timeout-minutes`: 90 → 120。理由: 1 attempt が踏み得る待ちが「引き継いだ deployment の決着 (30) → FAILED で drop して再 upload した deployment の決着 (30) → `wait-published` (30)」の 3 本直列になる経路があり、本体の作業 (実測 11 分) を足すと旧予算 90 分を超える。job timeout は cancel 扱いで後始末 step が走らないため、超過を許容できない。根拠は直上のコメントに記載。review-001 Minor 2 起因 (2026-09-07)

## 見送り (合意済み)

- review-002 Suggestion「標準出力の純度を名前で示す自己テストを 1 件足す」: 見送り。守りたい性質 (進捗行を標準エラーへ出す) の回帰検出力は既存 4 件のチェックがミューテーション実測で担保しており、指摘は後読みやすさに限る。review-002 自身が「見送っても本変更を止める理由にはならない」と判定している (2026-09-07)
