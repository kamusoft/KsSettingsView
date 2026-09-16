# Proposal: maui-appearance-change-tracking

## Why

MAUI で `SettingsView` を表示したまま OS の外観 (ライト / ダーク) を切り替えたときの追随が、不具合と回帰資産の欠落の両面で欠けている ([exploration.md](exploration.md))。利用者の体験は 1 つ (「表示中に外観を切り替えても正しい色で描かれ続ける」) だが原因は分かれる。

1. **MAUI iOS で行の背景がライトのまま残る。** 表示中にダークへ切り替えると、未指定のテキスト色だけが dark 既定へ解決され、行の背景は白のまま残って文字が読めなくなる (cell-style-dark-appearance-parity の tasks 8.4 で発見、既存不具合。証跡: `kasane/changes/archive/2026-09-06-cell-style-dark-appearance-parity/evidence/maui-host-buttoncell-retheme-ios-dark.png`)。調査の結果、bridge / handler が未指定を固定色に潰している線はコード上否定され、iOS Native 本体に「外観変化を受けて行の背景設定を貼り直す」機構が無い (trait 変化の購読はチェックボックスと Section 箱の枠線だけ) ことと、MAUI 経路で Native の ViewController が親に繋がれないまま成立し得ることの 2 つが原因候補として残った。Android は本体が夜間モード変化を拾って Theme を再解決・再適用しており (core/ADR-0030 の「View は uiMode 変更で未指定色を再解決する」)、iOS にだけ対応物が無い非対称になっている。加えて、iOS の既存テスト「表示中に外観をダークへ切り替えると既定色が描き直される」は保持している色を自前で dark 解決して見ているだけで、描画が古いままでも緑になる (この不具合を検出できない)
2. **`SettingsView` 単位の Theme プロパティに書く `AppThemeBinding` の回帰資産が無い。** 利用者向け Skill が推奨し concepts が「Simulator / Emulator で確認済み」と書く経路だが、`maui/` のコード・テスト・Sample には `AppThemeBinding` が 1 箇所も無い。Theme 変更 → snapshot → bridge → 再適用の経路自体はテストで固定済みで、固定されていないのは「外観変更で `AppThemeBinding` がこの経路を起動する」部分だけ

Cell 単位の色プロパティで `AppThemeBinding` / `DynamicResource` を効かせる改修 (`Section` / `Cell` を MAUI の element ツリーへ繋ぐ) は、成立するかの前提が未検証で設計判断 (core/ADR-0031 の改訂) を伴うため、本 change では可否の spike までに留める (2026-09-15 オーナー裁定、exploration の選択肢 A)。

## What Changes

- **iOS Native の表示中の外観変更で、未指定の行背景 (標準 Cell と CustomCell の Cell 背景・Section 装飾の塗り・list 下地・text Header / Footer の背景と文字) が現在の外観の既定へ描き直され、スクロール位置と可視行が保たれることを保証する。** 修正箇所は実装の先頭の切り分け (下記 Impact のゲート) で確定する: iOS Native 単体でも再現するなら iOS 本体に外観 (trait) 変化の観測主体を 1 つ置き (view controller の view 単位)、既存の Theme 再適用経路で全行・下地・装飾を再適用して Android と対称にする。MAUI 経路でのみ再現するなら MAUI iOS handler の child view controller の結び付け (containment) を正しく確立する (親の再解決の契機・見つからない場合の扱い・二重追加の防止を含む)。両方の可能性もある
- **iOS の text Header / Footer の背景に Theme の `headerBackgroundColor` / `footerBackgroundColor` を適用する** (隣接課題の同梱)。相方レビューの照合で、iOS の描画経路が Theme のこの 2 項目を消費していない (Android は `SectionAccessoryViewHolders` で消費) ことが分かった。外観追随の対象領域を Android と揃えるため、本 change で適用と外観追随の両方を入れる。View accessory には適用しない
- **iOS テストの観測点を「保持している色の自前解決」から「描画に効いている値」へ移す**: 表示中の外観切替で行の背景が実際に dark 既定へ変わることを、描画に使われている値で検証する (既存の `ThemeDarkAppearanceRenderingTests` / `CellStyleAppearanceTests` の観測点の置き換え)
- **`SettingsView` の Theme プロパティに書いた `AppThemeBinding` が外観変更で Theme の再適用まで届くことを回帰資産として固定する**: MauiHost の既定シナリオ (`maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml`) に `AppThemeBinding` を書いた Theme プロパティを常設し、handbook `maui/integration-host-verification.md` の完了条件に加える。あわせて MAUI 層のユニットテストで、外観の切替 (`Application.UserAppTheme`) が `AppThemeBinding` 付き Theme プロパティの変更として snapshot の配信に届くことを固定する (ユニットテストで外観切替を再現できるかは tasks 先頭で確認し、できなければ MauiHost シナリオのみを回帰資産とする)
- **MAUI iOS / Android の検証ホストで、色を指定しない画面を表示したまま外観を切り替えたとき行の背景・文字色がともに現在の外観の既定で描かれることを、完了条件に加える** (実行時挙動の検証規約に従い、修正前の再現と修正後の同一手順の解消確認を証跡付きで行う)
- **spike (記録のみ)**: `Section` / `Cell` を `SettingsView` の論理子に繋いだ状態で Cell の色プロパティの `AppThemeBinding` が外観変更で再評価されるかを最小改変で確かめ、結果 (成立 / 不成立と観察・副作用) を exploration.md に記録する。結果に関わらず本 change には同梱しない。成立していれば core/ADR-0031 の改訂を伴う別 change として起票するかをオーナーが裁定する
- 影響能力: settings-view-ios-ui / maui-bridge (facade の Theme プロパティと検証ホスト)。2 能力にまたがる

## Non-Goals

- **`Section` / `Cell` を element ツリーへ繋ぎ Cell 単位の `AppThemeBinding` を効かせる本実装** — 多重配置判定の二系統化・leak・継承プロパティ伝播の副作用範囲が読めず、core/ADR-0031 Decision 2 の MAUI 節の改訂を伴う設計判断。spike の結果を見てオーナーが別 change として起票するかを裁定する (本 change には同梱しない)
- **親 view controller を持たない (view だけを取り出す) ホスティングの公式サポート** — MAUI の Host は親 Page の child VC として embed する契約 (`concepts/maui/api/maui-rendering-lifecycle.md`) で、UIKit の containment 契約にも沿う。view-only 利用を保証するのは別の設計判断
- **MAUI 層のユニットテストでの外観切替の再現** — `SettingsView` を Application / Window / Page の element ツリーに載せないと `AppThemeBinding` の再評価が起きない可能性があり、成立可否が未確認。受け入れ基準は検証ホストの Scenario とし、ユニットテスト化は tasks の調査項目 (成立すれば追加) に留める
- **MAUI Sample への `AppThemeBinding` デモの追加** — Sample は 3 面一致を優先する規約 (handbook `cross/sample-parity.md`) で、外観追随は既に 3 面とも code-behind の再代入 (`SampleThemeFollower` 相当) で揃えている。`AppThemeBinding` は MAUI 固有の書き方で対応物を native に置けず、回帰資産としては MauiHost が既にその位置づけを持つ
- **Android の変更** — 本体の夜間モード再解決は実装・テスト済みで MAUI Android でも症状が出ない
- **利用者向け文書 (`skills/`) の `DynamicResource` 記述の訂正** — `settingsview-migration-defects` が扱う
- **`skills/` と README の追従** — 契約の変化は蒸留で concepts へ書き、docs-refresh の既存経路で別途追従する

## Impact

- **破壊的変更**: なし。公開 API・bridge の wire 形式・facade のプロパティは変えない
- **利用者可視の変更**: MAUI iOS で表示中に外観を切り替えたとき、色を指定していない画面の行背景が現在の外観の既定へ描き直されるようになる (現状は白のまま残る)。iOS Native 単体でも再現していた場合は iOS 利用者にも同じ改善が届く。iOS で text Header / Footer の背景に Theme の色が効くようになる (明示していた利用者にのみ見た目の変化)
- **ゲート (実装の先頭)**: iOS Native 単体 (iOS Sample またはテストホスト) で同じ手順で再現するかの切り分けを行い、修正箇所 (iOS 本体 / MAUI iOS handler / 両方) を確定してから修正に入る。切り分けの結果は deviation.md ではなく tasks の記録として残す (spec の Requirement は修正箇所に依らない挙動で書く)
- **リスク**: iOS 本体に外観変化の観測を足す場合、表示中の Theme 再適用が既存の Theme 更新経路 (構造・内容更新と分かれた Theme 経路、`core/architecture/display-state-synchronization.md`) に乗ることを確認する。再適用で行が作り直されず、可視行の集合とスクロール位置が保たれることをテストで固定する (押下状態は契約に含めない)。観測点を描画値へ移すことで既存の外観テストが赤になる可能性があり、それは検出力の回復なので修正側で吸収する。Header / Footer 背景の適用は iOS の見た目を変える (これまで塗られていなかった領域が Theme の色になる) — 既定色は list 下地と同じ値 (`#F2F2F7` / `#000000`) なので Theme を渡さない画面の見た目は変わらないが、`headerBackgroundColor` を明示していた利用者には初めて色が効く
- **長命層への影響 (蒸留で追随)**: `core/styling/style-resolution.md` の iOS 行 (表示中の外観変更の実現方法に本体の trait 購読を追記)、`maui/api/maui-styling.md` (Theme プロパティの `AppThemeBinding` の回帰資産の所在)、handbook `maui/integration-host-verification.md` (完了条件の追加は本 change の実装タスクに含める — 規約の変更ではなく確認項目の追加)。新規 ADR の候補は無し。spike で ②-b が成立した場合は core/ADR-0031 の改訂候補として exploration に記録する
- **検証手順**: MauiHost の設定画面を表示したまま OS の外観を切り替える (iOS Simulator / Android Emulator)。修正前に症状を再現し、修正後に同一手順で解消を確認し、証跡を `evidence/` に残す。iOS Native の切り分けも同じ手順で行う

## 級: L

2 能力 (settings-view-ios-ui / maui-bridge) にまたがり、iOS の外観観測主体と MAUI iOS の containment という設計判断を含む (ksn-core の「複数能力横断」)。公開 API は変わらず可逆で UI 承認ゲートは無いが、判断を design.md に固定する。当初 M で確定していたが、相方の提案レビュー (second-opinion-spec-001) の指摘で再判定 (2026-09-15)。

domain: cross
