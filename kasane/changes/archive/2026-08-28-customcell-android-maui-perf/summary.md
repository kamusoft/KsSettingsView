# Live Summary: customcell-android-maui-perf

## 最終状態 (何がどうなったか)

MAUI Android の CustomCell デモ画面のスクロールカクつき (Pixel 6a 実機で顕著、iOS / Native Android は問題なし) を調査し、原因を **Debug ビルドのオーバーヘッド (Mono インタープリタ実行)** と実測特定した。コード・サンプル・README への変更はなし。成果物は concepts への開発規約の追加のみ:

- 新規 `kasane/concepts/maui/conventions/performance-verification.md` (type: policy) — 「性能の評価・調査・カクつき報告の裏取りは必ず Release ビルドで行う。Debug の遅さは単独では実装欠陥の証拠にならない」を実測値・計測手順付きで規定
- `kasane/concepts/rules.md` の platform カテゴリ表に conventions/ 行を追加 (オーナー合意による新カテゴリ。規約系 type: policy を cross/conventions と揃える判断)
- `kasane/concepts/maui/index.md` に conventions/ 節を新設して 1 行追加、`kasane/concepts/log.md` に created 行を append

蒸留時への申し送り (2026-08-28 蒸留で処理済み): (1) concepts 本文の evidence 参照パスを archive 後パスへ確定する (review-002 Suggestion) → 確定済み (2) rules.md の「新カテゴリは概念 3 つ以上で新設」の明文と、オーナー合意による 1 本目新設の運用 (conventions/ と architecture/ の 2 例) の乖離を明文側で解消するか判断する (review-003 Suggestion) → **見送り** (オーナー判断: 明文は現状維持、次に同種の議論が起きたときに改めて判断)

## 採用値と根拠 (却下試行の要点)

採用: **「対処は concepts への規約記録のみ。プロダクト・サンプルのコードには手を入れない」**

根拠となる実測 (Pixel 6a 実機、同一画面・同一フリング操作の `dumpsys gfxinfo`):

| 構成 | Janky frames | p90 |
|---|---|---|
| Debug (既定) | 31.7% | 121ms |
| Debug + UseInterpreter=false | 8.8〜19.4% | 53〜65ms |
| Release | 4.6% | 12ms |
| Android native サンプル (基準) | 6.1% | 28ms |

Release は native 同等以上で、ユーザーも実機体感で解消を確認 (「全然なめらか」)。

却下試行:

- **README への Release 検証手順の追記** — 一度実施したが取り消し。README は利用者向けドキュメントで、この知識は開発者向けのため concepts が正しい置き場所 (ユーザー判断)
- **csproj への Debug 限定 `UseInterpreter=false`** — 一度実施し実機で中間性能 (Janky 8.8〜19.4%) を確認したが取り消し。性能確認は Release で行うと確定した以上、C# Hot Reload を犠牲にしてまで Debug を速くする価値がない (ユーザー判断)。中間性能の実測値は concepts に「Release の代替にならない」根拠として記録
- **構造最適化 (MAUI 埋め込み `AndroidView` の onReset による reusable 化、KsAccessoryHostView の measure キャッシュ)** — 構造課題としては実在するが、Release では 1 フレーム予算内に収まっており支配項でないため見送り。別 change として簡易起票済み: `kasane/changes/maui-android-customcell-embed-perf/exploration.md`

実測の証跡 (生の gfxinfo 抜粋と計測手順): `evidence/gfxinfo-pixel6a.md`

## 触ったファイル

- `kasane/concepts/maui/conventions/performance-verification.md` (新規。当初 maui/architecture/ に置いたが、規約系 (type: policy) は conventions カテゴリに揃えるというオーナー指示で移設)
- `kasane/concepts/rules.md` (platform カテゴリ表に conventions/ 行を追加 — オーナー合意による新カテゴリ)
- `kasane/concepts/maui/index.md` (conventions/ 節を新設して 1 行追加)
- `kasane/concepts/log.md` (1 行 append)
- change 配下: exploration.md / session.md / summary.md / evidence/gfxinfo-pixel6a.md (実測の生ログと計測手順) / review-001.md / review-002.md
- 簡易起票 (別 change): `kasane/changes/maui-android-customcell-embed-perf/exploration.md`

(samples/maui/README.md と samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj は試行中に編集したが完全に復帰済み — `git status` で samples/ 差分なしを確認)

## 決定事項 / ADR 候補

- 決定: 性能の評価・調査は Release ビルドで行う (concepts に policy として記録済み)。ADR 級ではない (覆すコスト低・境界を越えない — concepts の policy で十分)
- テスト: 最終 diff はドキュメントのみでコード・挙動に触れていないため、テスト追加・実行は不要
