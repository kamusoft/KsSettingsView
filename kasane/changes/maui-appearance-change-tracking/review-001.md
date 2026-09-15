# レビュー結果: maui-appearance-change-tracking (001 回目)

**日付**: 2026-09-15
**判定**: CHANGES_REQUESTED

## サマリー

レビュー対象は [deviation.md](deviation.md) の `[付随修正]` 行 — iOS Sample の SwiftUI クローム (大タイトル・ステータスバー・戻るボタン) が表示中のダーク切替に追随しない件の修正のみ (spec / tasks 本体のグループ 1〜6 は切り分けゲートで探索へ差し戻し済みのため対象外)。コードそのものの品質は良好で、最小・自己完結・既存慣習どおりであり、Sample の他画面 (`@Environment(\.colorScheme)` 駆動の Theme プリセット) への悪影響が無いことは Simulator で実測して確認した。

一方で、**この修正が症状に効いたという因果が実測で確認できない**。修正後ビルドでも同じ症状 (クロームがライトのまま残る) を再現でき、しかも同じ手順で修正前ビルドが症状を出す条件と差が付かなかった。証跡は残っているが再現・解消の操作手順が記録されていないため、レビュー側で同一手順の追試ができない。手順を記録したうえでの A/B の取り直し (または残存症状としてのオーナー裁定) を求める。

## 照合した規約

- `cross/comment-policy.md` (always)
- `cross/sample-parity.md` (`samples/**` を触る)
- `cross/runtime-behavior-verification.md` (実行時挙動の不具合修正の完了判定)
- `cross/test-execution.md` (テスト実行・結果報告)
- `ios/swift6-language-mode-check.md` — 適用外 (`ios/Sources/` に差分なし)
- `kasane/lessons/process.md` (L-003 / L-005 / L-009)、`kasane/lessons/code-review.md` (L-001: 実測で裏を取る)

## 実行したこと

- ビルド: `samples/ios/KsSettingsViewSample.xcodeproj` の Debug ビルド (iPhone 17 Pro Simulator / iOS 26.5) — **BUILD SUCCEEDED**、警告なし
- テスト: 本体 (`ios/`) 全テストの Simulator 実行 — `ios/Sources` に差分は無いが規律として実行 (結果は「テスト」節)
- lint: `scripts/comment-policy-lint.py --summary --advisory` — 禁止 0 件。本 change の 3 ファイルは要確認にも現れない
- 実機相当の動作確認: 修正後ビルドと、**作業ツリー外に複製して変更前の実装へ戻したビルド**の 2 本を同一 Simulator で突き合わせた (リポジトリには一切変更を加えていない)

## 指摘事項

### [🟠 Major] 付随修正が症状に効いた証拠が無い — 修正後ビルドで同じ症状を再現でき、変更前ビルドと差が付かない

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 行 / `samples/ios/KsSettingsViewSample/SampleAppearanceWindowStyle.swift`

**問題点**:

修正後ビルド (作業ツリーの現状) を iPhone 17 Pro Simulator (iOS 26.5) で次の手順にかけると、`[付随修正]` が直したはずの症状がそのまま出る。

手順 B: 端末ライト → ルートで「ダーク」を選ぶ → デモ画面 (基本 Cell 7 種デモ) へ進む → 戻る → 「システム」を選ぶ → 再びデモ画面へ進む → **表示したまま端末をダークへ切り替える**

結果: 行の背景・本文・Theme プリセットは dark へ追随するが、**大タイトル・ステータスバー・戻るボタンはライトの配色のまま残る** (黒地に黒文字でタイトルとステータスバーが読めない)。`evidence/ios-sample-chrome-before-dark.png` と同じ見え方になる。いったんこの状態に入ると端末外観をライト⇄ダークと往復させても回復せず、アプリを再起動するまで残った。

さらに、同じ手順を**変更前の実装に戻したビルド** (`ContentView` を `preferredColorScheme(appearance.colorScheme)` に戻し、`SampleAppearanceWindowStyle.swift` と pbxproj 登録を外したもの) にかけると、同じく症状が出る。つまり手順 B に関する限り、修正前後で挙動差が観測できない。

| 手順 | 修正後ビルド | 変更前ビルド |
|---|---|---|
| 起動直後・「システム」のまま・デモ画面表示中に端末をダークへ | 追随する | 追随する |
| ルートのみ (「ダーク」→「システム」→ 端末をダークへ。画面遷移なし) | 追随する | 追随する |
| 手順 B (上記。「ダーク」選択と画面遷移を挟んでから「システム」へ戻す) | 3 回中 2 回で**追随しない** | 2 回中 2 回で**追随しない** |

補足として、この変更が**何かを悪化させている証拠も無い** — 上表のとおり変更前ビルドと同じか、手順 B で失敗する頻度がやや低い。したがって差し戻しを求めるものではなく、求めるのは「効いたことの証明」か「効かない範囲の明示」のいずれかになる。

`evidence/ios-sample-chrome-before-dark.png` / `-after-*.png` は A/B の形式にはなっているが、どの外観選択からどう遷移した状態で撮ったかが成果物のどこにも書かれていないため、「before で失敗し after で成功した手順」を追試できない。`cross/runtime-behavior-verification.md` は「再現できた操作手順が、そのまま解消確認の手順になる」ことを完了条件にしており、現状はその手順が特定できない。上表のとおり、手順によっては**変更前ビルドでも追随する**ため、after の成功だけでは修正の効果を示せない。

**推奨修正** (いずれか):

1. before で失敗し after で成功する操作手順を特定して deviation.md (または tasks) に書き、その手順で A/B を撮り直す。あわせて上記の手順 B が解消するところまで直す
2. 手順 B の残存が window の `overrideUserInterfaceStyle` では解消できない SwiftUI 側の挙動だと判断するなら、オーナー裁定のうえで「残る症状と回避策 (アプリ再起動 / 外観選択を固定して検証する)」を deviation.md に明記する。検証画面の可読性を目的にした修正なので、残るなら残ると分かる形にしておかないと、この Sample を使う次の切り分けで同じ罠を踏む

### [🟡 Minor] 再現・解消の操作手順が成果物に残っていない

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 行 / [tasks.md](tasks.md)

**問題点**: `[付随修正]` は症状と修正内容を書いているが、再現手順 (外観選択の遷移・画面遷移の順・切替方向) が無い。tasks.md 側にも付随修正のタスク行が無いため手順の置き場が存在せず、`cross/runtime-behavior-verification.md` の 1.・2. (再現手順 = 解消確認手順) と process L-003 (4) 「レビューは証跡の実在と提出コードとの対応を判定条件にする」を満たすための材料が欠けている。Major の直接の原因でもある。

**推奨修正**: 撮影時の操作列 (どの外観選択から、どの画面で、どちら向きに切り替えたか) を deviation.md の該当行か tasks に 1〜2 行で残す。証跡ファイル名に手順の向き (light→dark など) が入っているので、対応が分かる粒度で十分。

### [🔵 Suggestion] deviation の箇所列挙に `ContentView.swift` と `project.pbxproj` が現れていない

**該当箇所**: [deviation.md](deviation.md) の `[付随修正]` 行

**問題点**: process L-009 の事後判定は「`git show --stat` に現れるファイルのうち合意済みスコープが名指ししていないものが、すべて deviation.md の行に箇所として現れている」。現在の行は `samples/ios/KsSettingsViewSample` 配下という括りと `SampleAppearanceWindowStyle.swift` / `SampleAppearance.colorScheme` を挙げているが、`ContentView.swift` と `KsSettingsViewSample.xcodeproj/project.pbxproj` は名前が出ていない。

**推奨修正**: 同じ行に 2 ファイルを足す (修飾子の差し替えと Sources / 参照登録)。

### [🔵 Suggestion] Sample の外観適用機構が MAUI と同形になったことを、次の探索の前提として残す

**該当箇所**: `samples/ios/KsSettingsViewSample/SampleAppearanceWindowStyle.swift`

**問題点**: 今回の変更で iOS Sample の外観適用が window の `overrideUserInterfaceStyle` になり、MAUI (`AppTheme.Unspecified` → iOS では window への override) と同じ機構になった。`ios/Sources/KsSettingsViewUI/PresentationAppearance.swift` が想定している「window に override が掛かっている構成」を iOS Sample も通るようになる、という意味で本体の検証経路が変わる。今回の change は tasks 0.2 で iOS Sample を「MAUI 経路と対比するための iOS Native 単体」として使っており、差し戻し後の再探索でも同じ使い方をするなら、「Sample 側も window override になったので、この対比では override の有無の差は出ない」ことを前提として持っておく必要がある。規約違反ではなく、消えると困る前提という位置づけ。

**推奨修正**: deviation.md の `[付随修正]` 行に一言添えるか、再探索の exploration に前提として書き出す。

## 確認して問題が無かった観点

- **Sample 他画面への影響**: 「ダーク」選択 (端末はライト) の状態で基本 Cell 7 種デモを開き、`SampleTheme.maui(dark: colorScheme == .dark)` の dark プリセット (暗い canvas・橙のアクセント) が適用されることを実測。window の override が SwiftUI の `@Environment(\.colorScheme)` へ伝わっており、`BasicCellsDemoView` / `InputCellsDemoView` / `CustomCellDemoView` / `SectionDecorationDemoView` の Theme 切替は従来どおり動く
- **`overrideUserInterfaceStyle` 方式の妥当性**: 「システム」を `.unspecified` に対応づける形は、MAUI Sample の `SampleAppearances.ToAppTheme` (`AppTheme.Unspecified`) および Android Sample の app 全体への外観指定と機構が対応する。同値代入を避けるガード、`didMoveToWindow` での初回適用、`isUserInteractionEnabled = false` も妥当
- **付随修正の同梱条件**: 公開 API・データスキーマ・既存 ADR に触れず、実質 3 ファイル (+ pbxproj 登録) に閉じ、新しい設計判断を持ち込んでいない。本務のファイルそのものではないが、tasks 0.2 が切り分け装置として使う画面であり、オーナーが「今対応」と裁定したことが deviation.md に記録されている。スコープ膨張としては指摘しない。ただし「テストで担保」の条件だけは Sample にテストターゲットが無いため実環境の証跡が代替になっており、その証跡が追試できないことが上の Major に直結している
- **スコープと最小性**: spec / 合意スコープが要求しない抽象化・設定項目・拡張点は足されていない。`SampleAppearance` から SwiftUI 依存が外れて `UIKit` 依存になった点も、唯一の利用者 (`ContentView`) 側で閉じている
- **pbxproj 登録**: `PBXBuildFile` / `PBXFileReference` / group children / Sources build phase の 4 箇所に、既存の採番規則どおりの ID (`...A01A`) で 1 件ずつ追加されている。ID の重複なし、ビルドで実際にコンパイルされることを確認
- **comment-policy**: 作業文書パス・変更 ID・通番・履歴記述・デルタスペック構文キーワードの混入なし。新規ファイルのヘッダは SwiftUI の現在の挙動と設計理由を現在形で自己完結的に書いており、規約の許容範囲。lint も禁止 0 件
- **sample-parity**: 表示文言・画面構成・デモデータに変更なし。「外観」セクションの項目名も不変で、他 platform との一致は崩れていない

## テスト

- iOS 本体テスト (`ios/`、`xcodebuild test -scheme KsSettingsView`、iPhone 17 Pro Simulator / iOS 26.5): **1027 tests / 0 failures** (`** TEST SUCCEEDED **`)。バンドル集計行の内訳は Bridge 166 / Core 88 / SwiftUI 96 / TestSupport 7 / UI 670。`ios/Sources` に差分は無いため結果は変更前と同じ意味を持つ
- Sample にはテストターゲットが無く、本件に対応するユニットテストは存在しない (実行時挙動の症状であり `cross/runtime-behavior-verification.md` の対象。担保は実環境の再現・解消確認になる)

## アクションプラン

1. (Major) 手順 B で残る症状を解消する。解消できない場合はオーナー裁定を取り、残存症状として deviation.md に明記する
2. (Major / Minor 共通) before で失敗し after で成功する操作手順を特定して deviation.md か tasks に記録し、その手順で A/B 証跡を撮り直す
3. (Suggestion) deviation.md の `[付随修正]` 行に `ContentView.swift` と `project.pbxproj` を足す
4. (Suggestion) Sample の外観適用が MAUI と同形になった前提を、再探索へ引き継ぐ形で残す
