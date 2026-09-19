# レビュー結果: maui-appearance-change-tracking (002 回目)

**日付**: 2026-09-15
**判定**: NEEDS_DISCUSSION

## サマリー

レビュー対象は [deviation.md](deviation.md) の `[付随修正]` 行とその配下 — iOS Sample の SwiftUI クローム (大タイトル・ステータスバー・戻るボタン) が表示中のダーク切替に追随しない件の修正のみ。spec / tasks 本体 (グループ 1〜6) は切り分けゲートで前提が反証され探索へ差し戻し済みのため対象外、手順 B の残存もオーナー裁定済みとして指摘対象から外した。

前回の 4 件のうち Minor 1 件・Suggestion 2 件は解消を確認した。残る Major (修正が症状に効いた証拠が無い) は**未解消**で、しかも今回の実測で前提そのものが揺らいだ — 修正を外したビルドで手順 A を 3 回通したが、3 回とも chrome は正しく追随し、症状が 1 度も再現しなかった (同じビルドで手順 B も 2 回通したが、こちらも再現しなかった)。証跡 `evidence/ios-sample-chrome-a-before-dark.png` は手順が確定する前に撮られたファイルのままで撮り直されておらず、間欠症状の 1 枚きりの before と 1 枚きりの after では「修正が効いた」を示せない。これは実装側の努力では詰め切れない性質 (SwiftUI 内部 node 由来の間欠症状) なので、CHANGES_REQUESTED として同じ宿題を返すのではなく、処遇の選択肢を挙げてオーナー裁定を求める。

## 照合した規約

- `cross/comment-policy.md` (always)
- `cross/sample-parity.md` (`samples/**` を触る)
- `cross/runtime-behavior-verification.md` (実行時挙動の不具合修正の完了判定)
- `cross/test-execution.md` (テスト実行・結果報告)
- `ios/swift6-language-mode-check.md` — 適用外 (`ios/Sources/` に差分なし)
- `kasane/lessons/process.md` (L-003 / L-005 / L-009)、`kasane/lessons/code-review.md` (L-001: 実測で裏を取る)

## 実行したこと

- ビルド: `samples/ios/KsSettingsViewSample.xcodeproj` の Debug ビルド (iPhone 17 Pro Simulator / iOS 26.x) — **BUILD SUCCEEDED**
- テスト: 本体 (`ios/`) 全テストの Simulator 実行 (結果は「テスト」節)
- lint: `scripts/comment-policy-lint.py --summary --advisory` — 禁止 0 件 / 要確認 164 件。本 change の 3 ファイルはどちらにも現れない
- 実測 A/B: 作業ツリーの現状 (修正後) と、**作業ツリー外へ複製して 3 ファイル + pbxproj を HEAD の内容へ戻したビルド** (修正前) の 2 本を同一 Simulator で突き合わせた。両ビルドの差は中間生成物に `SampleAppearanceWindowStyle.o` が有るか無いかで確認済み。リポジトリには一切変更を加えていない

## 指摘事項

### [🟠 Major] 手順 A の A/B が「修正が効いた」を示していない — 修正前ビルドで手順 A の症状を再現できない

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 配下「手順 A (解消済み)」の箇条書き / `evidence/ios-sample-chrome-a-before-dark.png`

**問題点**:

deviation.md に記録された手順 A を、修正前へ戻したビルドでそのままなぞった結果、**症状が 1 度も再現しなかった**。

手順 A (記録どおり): 端末ライト → ルートで「ライト」→「システム」→ Store 方式デモへ進む → 表示したまま端末をダークへ

| ビルド | 手順 | 試行 | 結果 |
|---|---|---|---|
| 修正後 (作業ツリー現状) | 手順 A | 2 回 | 2 回とも追随 (大タイトル・ステータスバー・戻るボタンが dark) |
| **修正前 (複製して差し戻し)** | **手順 A** | **3 回** | **3 回とも追随** — 症状が出ない |
| 修正前 (同上) | 手順 B | 2 回 | 2 回とも追随 — 症状が出ない |

3 回目の手順 A は各操作の間に 3〜4 秒の間を置いて (選択 → 遷移 → 外観切替がそれぞれ settle した状態で) 通したが、結果は同じだった。操作は自動タップ、外観切替は `simctl ui appearance`、機種は iPhone 17 Pro (iOS 26.x)。

つまり手順 A は `cross/runtime-behavior-verification.md` の 1.「修正前に実環境で症状を再現する。再現できた操作手順が、そのまま解消確認の手順になる」を満たしていない — 再現手順として機能していないので、2.「同一手順で解消を確認する」も成立しない。

証跡の側にも裏付けが無い。`evidence/` の更新時刻を見ると、`ios-sample-chrome-a-before-dark.png` だけが 18:17 のまま (前回レビュー 19:04 より前) で、手順が確定して撮り直された 19:20 の一群 (`a-after-dark.png` / `b-before-dark.png` / `b-after-dark.png`) に含まれていない。つまり before は**手順 A が言語化される前に撮られた 1 枚**であり、それが手順 A の操作列で撮られたという裏付けは成果物のどこにもない。加えて手順 A の続き 2 枚 (`a-after-back-to-light.png` / `a-after-dark-2nd.png`) は 18:22 のままなので、deviation が 1 本の連続手順として書いている after 側も、実際には別々の実行から寄せ集められている。

なお、この指摘は「修正が悪さをしている」という主張ではない。修正後ビルドでも手順 A は 2 回とも正常で、悪化の証拠は無い。問題は**効いたことの証拠が無い**ことと、deviation.md がそれを「解消済み」と断定していることにある。

**推奨対応**: 後述の「アクションプラン」の選択肢から裁定を仰ぐ。実装側で追加作業をするなら、まず修正前ビルドで症状を安定して出せる操作列を見つけるところから (見つからなければ手順 A も手順 B と同じ間欠症状として扱う)。

### [🟡 Minor] deviation 内の機構説明が、手順 A を「window override で直った」とする主張と噛み合わない

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 配下、手順 B の原因説明の箇条書き

**問題点**: 手順 B の原因説明は「`UINavigationBar` と stack 下段の `NavigationStackHostingController` だけが light のまま残る。両者の `overrideUserInterfaceStyle` は `.unspecified` で、**window に明示の `.dark` を置いても解決値が変わらない**」と書いている。一方で手順 A については「適用を window の `overrideUserInterfaceStyle` へ移したことで解消した」としている。症状 (chrome だけが切替前の配色で残る) は両手順で同一なので、同じ文書の中で「window override では解決値が変わらない」と「window override へ移して解消した」が並んでいることになる。

読み手 (次の探索・蒸留) は、この記録から「window override は効くのか効かないのか」を判断できない。上の Major の実測 (修正前でも手順 A では症状が出ない) と合わせると、手順 A と手順 B は同一の間欠症状で、修正の有無とは独立に発生しているという読みのほうが整合する。

**推奨修正**: 両手順の関係 (同一症状の別トリガーなのか、別現象なのか) についての現時点の理解を 1 行で明示し、手順 A の「解消済み」という断定を、実測で言える範囲の表現に改める。Major の裁定と合わせて 1 回で直すのが妥当。

### [🔵 Suggestion] 証跡に計測条件が残っていない

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 配下 / `evidence/ios-sample-chrome-*.png`

**問題点**: 間欠症状を扱う記録なのに、機種・OS 版・試行回数・外観切替の方法が成果物に無い。手順 B について「変更前ビルドも同じ手順で追随しないことがあり (計測した回は追随した)」とは書かれているが、何回中何回かが分からない。前回レビューが修正前ビルドで手順 B を 2/2 で再現したのに対し、今回のレビューは同じ手順で 0/2 だった — 発生率が環境や操作速度に強く依存することを示しており、回数の記録がないと次の読み手が「再現しなかった」を異常と見なすか正常と見なすか判断できない。

**推奨修正**: 手順の行に「<機種> / iOS <版>、<方法> で外観切替、N 回中 M 回」の形で条件と回数を添える。tasks 0.1〜0.3 の注記が既にこの粒度 (3 条件 × 2 OS 版) で書かれているので、それに揃えるだけでよい。

## review-001 指摘の対処状況

| review-001 の指摘 | 状態 | 根拠 |
|---|---|---|
| 🟠 Major 付随修正が症状に効いた証拠が無い | **未解消** | 上記 Major。手順は記録されたが、その手順で修正前ビルドの症状を再現できない。before の証跡も撮り直されていない |
| 🟡 Minor 再現・解消の操作手順が成果物に残っていない | **解消** | 手順 A / 手順 B が deviation.md に操作列として記録された。手順そのものは追試可能な粒度になっている (追試の結果は上記 Major) |
| 🔵 Suggestion 箇所列挙に `ContentView.swift` と `project.pbxproj` が無い | **解消** | `[付随修正]` 行の「箇所:」に 4 ファイルすべてが役割付きで列挙された (process L-009 の事後判定を満たす) |
| 🔵 Suggestion Sample の外観適用が MAUI と同形になった前提を残す | **解消** | `[付随修正]` 配下の 1 つ目の箇条書きに、iOS Native 単体を対照に使うとき window override の有無は差にならない旨が残された |

手順 B の残存はオーナー裁定 (受容して閉じる) と回避策 (アプリ再起動) が deviation.md に記録されており、指摘対象から外した。

## 確認して問題が無かった観点

- **実装の妥当性**: `samples/ios/KsSettingsViewSample/SampleAppearanceWindowStyle.swift` は最小・自己完結で、描画を持たない `UIViewRepresentable` を `background` に置いて window へ `overrideUserInterfaceStyle` を書くだけ。`didMoveToWindow` での初回適用、`style` の `didSet` 経由の再適用、`applyStyle()` の同値ガード (同値代入でも trait 再評価が走るため)、`isUserInteractionEnabled = false` によるヒットテスト非干渉、`init(coder:)` の `@available(*, unavailable)` はいずれも妥当。`ContentView` は `WindowGroup` 直下の唯一のインスタンスなので、window を共有する複数インスタンスの競合も起きない
- **「システム」= `.unspecified` の対応づけ**: MAUI Sample の `AppTheme.Unspecified` と機構が対応する。`SampleAppearance` が `SwiftUI` 依存から `UIKit` 依存へ変わった点も、唯一の利用者 (`ContentView`) 側で閉じている
- **他画面への影響**: window の override は SwiftUI の `@Environment(\.colorScheme)` へ伝わるため、`SampleTheme.maui(dark:)` を使うデモ画面の Theme 切替は従来どおり。今回の実測でも「ダーク」選択下の基本 Cell 7 種デモが dark プリセット (暗い canvas・橙のアクセント) で描かれることを確認した
- **スコープと最小性**: 合意スコープが要求しない抽象化・設定項目・拡張点は足されていない
- **pbxproj 登録**: `PBXBuildFile` / `PBXFileReference` / group children / Sources build phase の 4 箇所に既存の採番規則どおりの ID で 1 件ずつ追加。ビルドで実際にコンパイルされることを確認 (中間生成物に `SampleAppearanceWindowStyle.o` が現れる)
- **付随修正の同梱条件**: 公開 API・データスキーマ・既存 ADR に触れず、実質 3 ファイル (+ pbxproj 登録) に閉じ、新しい設計判断を持ち込んでいない。オーナーが「今対応」と裁定したことも deviation.md に記録済み。スコープ膨張としては指摘しない
- **comment-policy**: 作業文書パス・変更 ID・通番・履歴記述・デルタスペック構文キーワードの混入なし。新規ファイルのヘッダは SwiftUI の挙動と設計理由を現在形で自己完結的に書いており規約の許容範囲。lint も禁止 0 件
- **sample-parity**: 表示文言・画面構成・デモデータに変更なし。「外観」セクションの項目名も不変
- **足場の非改変**: `specs/` / `proposal.md` / `design.md` / `exploration.md` に差分なし。`tasks.md` の差分は 0.1〜0.3 のチェックと切り分け結果の注記のみで、足場の書き換えではない

## テスト

- iOS 本体テスト (`ios/`、`xcodebuild test -scheme KsSettingsView`、iPhone 17 Pro Simulator): **1027 tests / 0 failures** (`** TEST SUCCEEDED **`)。バンドル集計行の内訳は Bridge 166 / Core 88 / SwiftUI 96 / TestSupport 7 / UI 670。`ios/Sources` に差分は無いため結果は変更前と同じ意味を持つ
- Sample にはテストターゲットが無く、本件に対応するユニットテストは存在しない (`cross/runtime-behavior-verification.md` の対象であり、担保は実環境の再現・解消確認になる — それが上記 Major)

## アクションプラン

Major は実装側の追加作業だけでは閉じられない (修正前ビルドで症状を安定して出せないため、A/B が組めない)。次のいずれかをオーナーが選ぶ形を推奨する。

1. **機構整備として受け入れ、症状への効果の主張を取り下げる** — 修正はそのまま残し、deviation.md の記述を「iOS Sample の外観適用を MAUI と同じ window override へ揃えた。chrome が追随しない間欠症状 (手順 A / 手順 B) は修正前後で差が確認できておらず未解決」に改める。Minor / Suggestion もこの書き換えに含める。コードは最小・無害で MAUI との対照条件も揃うため、残す利点はある
2. **付随修正を差し戻す** — 症状への効果が示せない以上、Sample を触らずに元へ戻す。手順 A / 手順 B の間欠症状は未解決のまま残り、回避策 (アプリ再起動) の記録だけを残す
3. **再現条件の特定を続ける** — 修正前ビルドで症状を安定して出せる操作列 (端末操作の速度・Settings 経由の切替・バックグラウンド往復・OS 版違いなど) を探し、見つかってから A/B を撮り直す。tasks 本体が探索へ差し戻し済みであることを考えると、この探索は再探索の側で扱うほうが筋がよい

裁定後の実務は、1. なら deviation.md の書き換えのみ、2. なら 4 ファイルの差し戻しと deviation.md の書き換え、3. なら再探索への引き継ぎ。
