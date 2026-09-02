# phase-7-consumer-verification 議論履歴

## 2026-09-02: 検証範囲 (解決・ビルドまでか、起動まで含めるか)

- 選択肢: A) 解決 + Release ビルドまで (起動なし) / B) A + Simulator / Emulator 起動 / C) A + `dotnet publish` (MAUI のみ)
- 採用: A

理由: 検証対象は配信経路 (解決・メタデータ・推移依存・利用者側 SDK でのビルド) で、実行時挙動はユニットテストと Sample の目視が担う (phase-3 の E2E 非搭載と同じ分担)。MAUI Release ビルドは trimming / R8 / AOT まで走るため `dotnet publish` は追加検出がなく、実機 RID は署名不可。Simulator / Emulator 起動は CI で最も flaky で publish 前ゲートの信頼性を下げる

- 付随: phase-6 申し送り (`dotnet publish` と実機起動の要否) をこの決定で閉じた。ADR は起こさない (起動ステップは後から足せる可逆な判断)。iOS 消費者はアプリでなく SwiftPM パッケージでも成立する形が開けた (次論点の前提)

## 2026-09-02: `verification/` の構成

- 選択肢: A) iOS = SwiftPM パッケージ / Android・MAUI = アプリ / B) 3 platform ともアプリ / C) 3 platform ともライブラリ (MAUI のみアプリ)
- 採用: A。README (英語) の最小例 3 本を無編集で同梱し、README との一致を `scripts/` の lint で検査して CI lint job に載せる

理由: iOS は phase-4 の消費者パッケージをそのまま流用でき pbxproj を持たないため identity lint の事故が起きない。Android は manifest merger (minSdk) と R8 / dex マージが app ビルドでしか走らず、C では配信経路の検証にならない。B は iOS の pbxproj 一式の保守と lint scope 追加が増えるだけで検出力は A と同じ (起動しないため)

- 付随: phase-9 申し送り (README 例のビルド未検証) をこの決定で受け皿確定。`README_ja` は docs-refresh の英日同期に任せる

## 2026-09-02: ローカルフィード方式 (dry-run の参照先)

- 選択肢: iOS) スナップショットの `path:` 参照 / 配信リポジトリの prerelease tag。Android) mavenLocal / Central Portal 保留状態。MAUI) ローカルフォルダフィード + packageSourceMapping / 併記 + 隔離 packages path の事後検査
- 採用: `path:` / mavenLocal (`includeGroup`) / ローカルフィード + packageSourceMapping

理由: prerelease tag は phase-4 裁定 (検証 tag を残さない) と ADR-0020 (tag は最後) に緊張。Portal 保留状態は upload 後にしか取れず dry-run にならない。mapping は取得元を restore 自体が保証し、phase-8 の NU1507 対処 (候補 a) の先行実証にもなる。一時ディレクトリ名を `KsSettingsView-SPM` にすれば消費者の `package:` 指定が実レジストリと同一文字列になる

- 付随: phase-6 申し送り (`nuget.config` 設計) を閉じた。ADR なし

## 2026-09-02: dry-run と smoke の切り替え方

- 選択肢: A) 1 構成 + 2 引数 (モード・version) / B) dry-run 用と smoke 用でプロジェクトを分ける / C) dry-run のみ (smoke は手動)
- 採用: A。iOS は `Package.swift` をテンプレート生成、Android は Gradle プロパティ、MAUI は MSBuild プロパティ + `nuget.config` 2 枚 (`-p:RestoreConfigFile=`)
- 理由: version は ADR-0020 によりファイルに置けず smoke は必ず引数になる。B は README 一致 lint の対象が倍になり片側修正の事故を招く。C は publish 後の smoke を自動化できない。iOS の環境変数方式はマニフェスト cache で古い評価が残るため生成方式にした
- ADR なし

## 2026-09-02: CI からの起動方法

- 選択肢: A) reusable workflow (`mode` / `version` 入力) + platform 別スクリプト、PR CI にも dry-run を載せる / B) release workflow からのみ呼ぶ / C) PR CI は README 一致 lint だけ
- 採用: A。スクリプトはフィード準備と消費者ビルドの 2 段 (release で publish 成果物をそのまま渡すため)
- 理由: phase-9 申し送りの意図は「README 例を CI でビルドさせる」であり、release 時だけではメタデータや例の壊れがリリース直前まで見つからない。public リポジトリでランナーは無料、既存 CI の「全 job 常時実行」方針とも整合。phase-8 は同じ workflow を dry-run / smoke で 2 回呼ぶだけ
- 付随: 必須 status check 3 job 追加 (develop の branch protection 更新、phase-8 の main 作成申し送りへ追記)。ADR なし

## 2026-09-02: MAUI 消費者でのトランジティブ依存の確認

- 選択肢: A) ビルド成功のみ / B) restore 警告エラー化 + binding 版一致検査 / C) B + AndroidX 解決版の期待値照合
- 採用: B
- 理由: 事実は phase-6 PoC で確定済み (binding 推移到達、LiveData 2.11.0.1、警告 0)。B は lockstep の崩れを smoke で検出できる唯一の検査で保守コストが小さい。C は CPM 更新のたびの期待値追随になる。ビルド警告のエラー化は XA4301 (phase-8) と論点が混ざるため restore 警告に限定
- ADR なし

## 2026-09-02: cross/ADR-0016 (Sample パリティ) との関係

- 選択肢: A) ADR も規約も改訂せず concepts (repository-boundaries) に `verification/` の役割を記述 / B) sample-parity 規約に対象外を追記 / C) ADR-0016 を改訂
- 採用: A
- 理由: 規約の適用範囲は `samples/**` で閉じ、ADR の決定文も `verification/` に触れないため対象外は改訂なしに読める。規範に対象外列挙を足すと二重化し、`verification/` の 3 platform 一致は README 一致 lint の帰結で独自の義務ではない
- 付随: これで全 7 論点が解消。TODO を整理 (Explicit API mode 申し送りは実装・蒸留済みとして完了、docs-refresh 依頼内容を表に集約)。ksn-propose へ

## 2026-09-02: フェーズ議論の完了

全 7 決定 (検証範囲 / `verification/` 構成 / dry-run 参照先 / 切り替え方 / CI への届け方 / MAUI 推移依存の検査 / パリティ対象外) で論点が出尽くした。ADR は起票なし (いずれも可逆・局所、または既存 ADR-0019 / 0020 の帰結)。残 TODO は提案化・人の作業 (branch protection)・docs-refresh 依頼・API 版付き TFM 要件の実測 (change のタスク)。

## 2026-09-02: 提案化とセカンドオピニオンでの決定の訂正

change `add-consumer-verification` を起票 (M 級として提案)。セカンドオピニオン (codex、second-opinion-spec-001.md) で決定 2 件を訂正した: (1) dry-run の参照先 — Gradle の content filter は排他でないため `exclusiveContent` に、NuGet は mapping に加えて空の packages path と `.nupkg.metadata` の取得元検査を併用 (「事後検査は不要」を撤回)。(2) CI への届け方 — workflow は 1 本 3 job ではなく platform 別 3 本 (ADR-0025 の形)、publish 成果物は artifact 入力で受け取る。級の再分類 (M → L) はオーナー判断待ち。
