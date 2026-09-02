# セカンドオピニオン: add-consumer-verification (code-002)
**相方**: codex / **label**: so-code-add-consumer-verification (turn 2) / **日付**: 2026-09-02 / **対象**: 1 周目の採用指摘 5 件の解消確認 (作業ツリー差分)
---
静的再レビューの結果、4件は解消、1件は部分修正に留まっています。修正起因の独立した新規問題はありません。

1. Major: `smoke + artifact` — 解消

- 共通引数検査で `smoke + --reference` を拒否しています：[`verification/lib/verification-args.sh:112`](verification/lib/verification-args.sh:112)
- 3 workflow とも `artifact` を環境変数経由で受け、checkout・artifact download より前に拒否しています：
  - [`.github/workflows/verify-consumer-ios.yml:53`](.github/workflows/verify-consumer-ios.yml:53)
  - [`.github/workflows/verify-consumer-android.yml:49`](.github/workflows/verify-consumer-android.yml:49)
  - [`.github/workflows/verify-consumer-maui.yml:52`](.github/workflows/verify-consumer-maui.yml:52)
- オーナー判断も [`deviation.md:5`](kasane/changes/add-consumer-verification/deviation.md:5) に記録されています。
- 新規問題：なし。

2. Minor: `mavenLocal` の位置固定 — 未解消

### [🟡 Minor] Gradle が実際に使用した Maven Local と検査先がなお一致しない場合がある

**該当箇所**: [`verification/android/prepare-feed.sh:43`](verification/android/prepare-feed.sh:43)、[`verification/android/prepare-feed.sh:47`](verification/android/prepare-feed.sh:47)

**問題点**: `~/.m2/repository` の完全固定は除去され、`~/.m2/settings.xml` の単純な絶対パス指定には対応しました。しかし、Gradle の実際の解決結果ではなく、独自の XML パーサーで再計算しています。

少なくとも次の設定は反映されません。

- `-Dmaven.repo.local=...` などの JVM システムプロパティ
- Maven 設定で一般的な `${user.home}` の補間。`os.path.expandvars()` は通常これを環境変数 `user.home` として探すため、Maven のシステムプロパティ補間と一致しません
- ユーザー設定以外の Maven 設定経路

この場合、発行は成功しても [`prepare-feed.sh:68`](verification/android/prepare-feed.sh:68) が別の場所を検査して失敗します。修正で入った独自パーサーによる解決規則の二重化が、残存問題です。

**推奨修正**: `--work` 配下などにローカルリポジトリを決め、発行時に `-Dmaven.repo.local=<path>` を明示して同じパスを返すか、Gradle が解決した `mavenLocal().url` を直接取得してください。

3. Minor: README lint の重複ブロック — 解消

- `(小見出し, fence言語)` ごとの一致数を数え、ちょうど1件でなければ失敗します：[`scripts/readme-example-lint.py:96`](scripts/readme-example-lint.py:96)
- `--selftest` に2件重複の負ケースと件数確認があります：[`scripts/readme-example-lint.py:269`](scripts/readme-example-lint.py:269)
- 新規問題：なし。

4. Minor: SNAPSHOT時のCentral発行エラー案内 — 解消

- 旧来のバージョンカタログ変更案内はなく、`-Pversion=<version>` と `publishToMavenLocal` を正しく案内しています：[`android/kssettingsview/build.gradle.kts:173`](android/kssettingsview/build.gradle.kts:173)
- 新規問題：なし。

5. Suggestion: `secrets: inherit` — 解消

- phase-8で呼び出し側が `secrets: inherit` を使わないことを、必須確認事項として明記しています：[`agenda.md:60`](kasane/roadmaps/package-distribution/phases/phase-7-consumer-verification/agenda.md:60)
- 未チェックのTODOなのはphase-8への申し送りという性質どおりで、本changeの未実装とは扱いません。
- 新規問題：なし。

**件数**: Critical 0 / Major 0 / Minor 1 / Suggestion 0  
**判定**: **APPROVED**

残るのは非既定のMaven Local設定で明示的に失敗する低優先度Minor 1件であり、`ksn-review` の判定基準上は承認範囲です。5件すべての完全クローズを出口にする場合は、(2)のみ追加修正が必要です。
