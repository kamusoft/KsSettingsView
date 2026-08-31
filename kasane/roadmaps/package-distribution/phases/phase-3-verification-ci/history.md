# phase-3-verification-ci 議論履歴

## 2026-08-31: workflow の構成 (分割単位)

選択肢: A) platform 別 reusable workflow 3 本 + CI 入口 1 本 (paths フィルタなし) / B) 1 本に 3 job (phase-8 で組み替え) / C) platform 別 3 本を独立トリガー + paths フィルタ。

採用: A。理由: release workflow (phase-8) が `workflow_call` で同じ job を再利用できる (roadmap の前提)。paths フィルタを避けることで必須チェック (branch protection) がスキップ待ちで詰まる問題を回避できる。public 化済みでランナーが無料のため常時 3 platform 実行のコストは許容。

あわせてユーザーの整理により、phase-3 の workflow は「テスト・lint 系 (PR 時)」のみを担い、ビルド・デプロイは phase-8 の release workflow の守備範囲であることを確認。トリガーの確定 (PR 対象ブランチ・push トリガーの要否) は残論点として継続。なお「main マージ時にデプロイ」の想定は cross/ADR-0020 (手動 dispatch 起動) と食い違うため、phase-8 で ADR 改訂から入る可能性を申し送り。

## 2026-08-31: workflow のトリガー

選択肢: A) PR 時のみ / B) PR 時 + develop への push 時 / C) PR 時のみ + マージ前 branch 最新化必須 (strict checks)。

採用: B。理由: PR 検証だけではマージ結果 (semantic conflict) に穴が残る。push 実行は無料ランナー 1 run で事後検知でき、develop 上の実行結果が残るため CI バッジ・健全性確認にも使える。C の rebase コストは避けた。main は直 push しない前提で push トリガーなし。あわせてリリースの起動形はユーザー確認により cross/ADR-0020 (手動 dispatch) のままで問題ないと確定 (phase-8 での ADR 改訂は不要)。

workflow 構成 (A) とトリガー (B) はいずれも YAML 編集で可逆・局所的なため ADR 起票はせず、agenda の決定事項 + 本 history の記録に留めた。

## 2026-08-31: iOS テストの実行形

選択肢: A) Simulator 実行で全件 / B) `swift test` のみ (88/338 件) / C) `swift test` 先行 + Simulator フルの 2 段。

採用: A。理由: ライブラリの中核 (UIKit Cell / SwiftUI Bridge) は canImport ガードの内側にあり、`swift test` では検証にならない (handbook/cross/test-execution.md)。GitHub macOS ランナーは Simulator 同梱で追加コストが低く、CI の緑 = handbook の完了判定 (Simulator 全件) と同義になる。C は早期失敗がわずかに速くなるだけでビルド 2 回分のコストがあり不採用。実行件数の成否判定への組み込みは Android と併せて別論点 (論点 8) で扱う。

## 2026-08-31: iOS job のランナーと Xcode 固定

選択肢: A) イメージ版指定 + Xcode をメジャー.マイナーで明示選択 / B) macos-latest + 既定 Xcode / C) パッチ版まで厳密固定。

採用: A。ただしユーザー指示によりイメージは macos-15 ではなく **macos-26**、Xcode は **26.5** に固定する (ライブラリ要求は Swift 5.10 / iOS 16+ と緩いが、古いイメージを使う理由もないため最新版指定)。理由: Xcode だけがリポジトリ内にバージョンの正を持たず (dotnet は global.json、Android は wrapper/toolchain)、イメージ既定に任せると無変更の CI がイメージ更新で壊れ得る。更新は workflow 変数の変更 PR として diff に残す。

## 2026-08-31: MAUI の検証範囲とランナー

選択肢: A) facade テストのみ (ubuntu 可) / B) + platform TFM / binding のビルド (macOS 必須) / C) + 検証ホストの実行 (E2E)。

採用: B。理由: MAUI で起きやすい回帰は binding / gateway 実体のコンパイル破壊で、ビルドを通せば捕まる。検証ホストの実行は Simulator / Emulator 起動の時間と flakiness を常時抱える割に、handbook 上も手元確認の手順として定義されており CI 化の改修が別途要るため見送り。facade テストは platform TFM を 1 行も実行しない (handbook/cross/test-execution.md) が、その担保ラインは「実行は手元・コンパイルは CI」の役割分担として明示した。付随して MAUI job のランナーは macOS (macos-26) で確定 — 論点「ランナー」は iOS / MAUI = macos-26、Android = ubuntu で全て閉じた。

## 2026-08-31: Android テストの実行件数担保

選択肢: A) 依存キャッシュのみ + 実行件数検査を成否判定に / B) --rerun-tasks 常時付与 / C) フレッシュランナー任せで検査なし。

採用: A。理由: 0 件成功は build/ をキャッシュした場合にのみ起きるため、キャッシュを依存のみに絞れば構造的に防げる。その上で件数検査 (モジュール×variant 単位 0 件 fail + summary 表示) を CI の仕様として残すことで、将来キャッシュ構成が変わっても検知できる二重の守りにする。B はキャッシュの恩恵を打ち消す「お守りフラグ」が残るため不採用。論点「キャッシュ」の Gradle 分は依存のみで確定。

## 2026-08-31: Android job / MAUI job の JDK 供給

選択肢: A) setup-java で Temurin 17 に統一 / A') daemon は 25・toolchain は 17 の 2 版構成 / B) イメージ同梱 JDK 依存 / C) toolchain resolver plugin 追加。

採用: A。理由: JVM 版が成果物に効くのは toolchain (jvmToolchain(17) でリポジトリ側固定) だけで、Gradle daemon の版はビルド成否にしか影響しない (phase-1 で 17/21/25 実測済み) — ならば 1 版で済む最小構成が良い。C はリポジトリ変更 + 毎回のネットワーク解決が増えるため不採用。B はイメージ更新で出どころが変わるため不採用。議論の中で「17 は古すぎないか」を確認し、toolchain 17 は AGP 最低要件に合わせて利用者側の互換性を広く取る意図的な契約であり、上げるのは CI ではなく成果物互換性の変更として別 change の判断、と整理した。

## 2026-08-31: キャッシュと MAUI workload の扱い

選択肢: A) NuGet キャッシュ + workload 毎回 install (版固定) / B) SDK + workload ディレクトリ丸ごとキャッシュ / C) キャッシュなし。

採用: A。理由: workload は SDK ディレクトリ内部に入るためキャッシュは数 GB の丸ごと保存になり、サイズ・復元時間・SDK 更新時の不整合と壊れやすい割に稼げる時間が微妙。まず素直な構成で始め、実測が痛ければ別 change で最適化する (測ってから最適化)。SwiftPM は外部依存ゼロでキャッシュ対象なし。workload set version の固定はユーザー指摘により global.json の workloadVersion で行い、SDK 版と一元管理する。

## 2026-08-31: lint 群の CI 搭載と identity スコープ拡大

選択肢: A) gitleaks + local-path + identity + comment-policy の 4 検査を lint job に載せ、identity スコープに samples を追加 / B) 4 検査は載せるが samples 見送り / C) 公開前提の 3 検査のみ (comment-policy は hook 任せ)。

採用: A。理由: 公開リポジトリでは秘密・ローカルパス・個体情報の混入が push 時点で公開事故になるため、PR で機械的に止める価値が最も高い。samples 追加の懸念だった誤検出は 130 ファイルへの試験実行で実測ゼロ。phase-2 決定「iOS Sample の開発チーム識別子」の残穴 (実機ビルド時の書き戻しは hook で止められない) を CI が塞ぐ。comment-policy-lint の搭載は、hook がローカル環境限定であるのに対し CI は他環境・他者の PR にも規約を保証するため。

## 2026-08-31: 必須チェック化と失敗時の通知

選択肢: A) 必須チェック 4 つ + PR 必須化 (admin バイパス可)・通知は GitHub 標準 / B) 必須チェックのみで直 push 許可 / C) A + Slack 等の追加通知。

採用: A。理由: 必須チェックは PR 経由のマージにしか効かず、直 push が許可のままでは保証にならない — PR 必須化とセットで「develop / main に入るコード = 4 検査通過済み」が成立する。ソロ運用の hotfix には admin バイパスを逃げ道として残す。通知は現運用規模では GitHub 標準で十分とし、追加基盤は作らない。対象は develop に加えて main (develop → main の PR にも同じチェックを要求)。

これで phase-3 の論点は全て解消。決定事項は 10 件 (workflow 構成 / トリガー / iOS 実行形 / iOS ランナー・Xcode / MAUI 範囲・ランナー / Android 件数担保 / JDK / キャッシュ・workload / lint 群 / 必須チェック)。次は ksn-propose (フェーズ由来入力) で変更提案化する。branch protection 設定は GitHub 側の操作を伴うため、change の tasks に手順として含めるか実施時に確認する。
