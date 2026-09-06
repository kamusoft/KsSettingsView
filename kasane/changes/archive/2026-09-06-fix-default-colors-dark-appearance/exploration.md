# Exploration: fix-default-colors-dark-appearance

## 課題 / 動機

ライブラリ本体の Theme 既定色が 3 platform とも外観 (iOS のダーク / Android の夜間モード) に追随しない。title / description の文字色だけがシステム色 (iOS `.label` / `.secondaryLabel`、Android の同梱 DayNight テーマの `textColorPrimary`) で外観に追随するため、Theme を渡さずにダーク外観で使うと**白地に淡色 (白) 文字**になり判読できない。

発見の文脈: add-sample-dark-mode-toggle の実装フェーズ (2026-09-05)。サンプルに外観切替を付けて「Theme を渡さない画面 (isVisible デモ・Section 装飾デモの箱)」をダークで開いたところ、Android Native・iOS Native の両面で再現 (証跡: 本 change の `ui/references/android-visibility-dark.png` / `ios-visibility-dark.png` / `android-section-decoration-dark.png` / `ios-section-decoration-dark.png` — 起票元 change の archive 時 (2026-09-05) に `ui/verification/` から写した。起票元の照合記録 (文章) は `kasane/changes/archive/2026-09-05-add-sample-dark-mode-toggle/ui/brief.md`)。同 change では proposal の Non-Goals (本体の既定値・夜間モード解決の変更) を守って deviation で達成範囲を縮小し、本体側をこの change に切り出した (オーナー裁定 2026-09-05)。

該当箇所:
- iOS: `ios/Sources/KsSettingsViewUI/Theme.swift` — `defaultBackgroundColor` (白固定) / `cellBackgroundColor` の既定引数 `.white` / `defaultSeparatorColor` / `defaultHeaderTextColor` / `defaultFooterTextColor` が固定 RGB。dynamic なのは `defaultCellTitleColor` = `.label`、`defaultCellDescriptionColor` = `.secondaryLabel` のみ
- Android: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` — `DEFAULT_BACKGROUND_COLOR` (#FFFFFF) / `cellBackgroundColor` 既定 `Color.White` / `DEFAULT_SEPARATOR_COLOR` / `DEFAULT_CELL_DESCRIPTION_COLOR` が固定値。`ui/EffectiveStyle.kt` の `resolveDefaultTitleColor` だけが `android.R.attr.textColorPrimary` から解決。ライブラリの res に `values-night` は無い
- MAUI: facade は native をラップするため同じ挙動。MAUI iOS / MAUI Android の両実行面で同じ症状を確認済み (証跡: 本 change の `ui/references/maui-ios-visibility-dark.png` / `maui-android-visibility-dark.png`、起票元 archive の `ui/brief.md` の照合結果)

長命層の記述の乖離 — **2026-09-05 の起票元 change の蒸留で反映済み** (本 change での再修正は不要。既定色を追随させる実装が入ったら、下記の記述を再度追随させる):
- `kasane/concepts/core/styling/style-resolution.md` の「Android のライト / ダークは、同梱テーマが DayNight 派生であるため端末の夜間モードとアプリの uiMode 制御で決まる」— chrome・選択面・title 既定には当てはまるが、Theme の既定色定数には届いていない
- add-sample-dark-mode-toggle の exploration.md「iOS のライブラリ既定色はシステム色 (`UIColor.label` 等) でアプリ外観に自動追随する」— 文字色にしか当てはまらない

### スコープの統合 (2026-09-05 探索、オーナー裁定)

根本原因は「Theme の色が (iOS を除き) 外観を持たない静的な値しか表せない」ことで、既定色の追随と「利用者が渡す Theme のダーク対応」は同じ根を持つ。仕組みを先に決めないと既定色の直し方が手戻りになるため、本 change の対象を **Theme のダーク対応の仕組み全体 (既定色 + 利用者が渡す Theme の追随手段)** へ広げた。

現状の整理 (コードで確認):
- MAUI facade は未指定を null で native に渡し native の `Theme()` 既定に任せるため、native を直せば MAUI は自動的に追随する
- 「Theme を渡す利用者」の追随手段は platform で異なる: iOS は dynamic な `UIColor` を渡せば切替コード不要、Android は Compose `Color` が静的なため構築時に分岐して渡す (夜間切替は Activity 再生成で再構築される)、MAUI は XAML の `AppThemeBinding` (facade 経由で Theme 再適用まで届くかは未検証)
- iOS の Modern Section 装飾の枠線は CGColor を layer に置くため、外観切替時の再解決が別途要る (既定の枠線は透明で実害なし)

## 検討した選択肢 (却下案と理由を含む)

論点の区分 (探索中の番号。以後この番号で参照する):
1. 方向 / 2. iOS の既定 dark 値の持ち方 / 3. Theme が外観を表す仕組み / 4. Android の未指定の印と解決先 / 5. 3 platform の dark 値の揃え方 / 6. 公開 API への影響 / 7. 外観切替時の再適用経路 / 8. concepts の記述更新範囲

### 論点 1: 方向

| 案 | 評価 |
|---|---|
| **A: 全既定色を外観に追随させる (既存不具合として直す)** | **採用**。ライト時の見た目は変えない設計にする |
| B: 文字色の追随もやめて全固定ライトに揃える | 却下。判読性は確保できるが iOS で OS 外観を無視する挙動を契約にしてしまう |
| C: 現状維持 + 「dark は Theme を渡して」と案内 | 却下。不具合を仕様に格上げするだけ |

### 論点 2: iOS の手段

| 案 | 評価 |
|---|---|
| A: OS のセマンティック色 (`.systemBackground` / `.separator` 等) に置き換え | 却下。ライト時の見た目が変わる (separator が半透明に、header 文字が `.secondaryLabel` 相当に) |
| **B: light = 現行固定値、dark = 新規生値を `UIColor(dynamicProvider:)` で対にする** | **採用**。ライト不変、dark 生値が明示的に存在するため Android と同じ値を写せる。dark 生値の候補は iOS の dark パレットを参考にする |
| C: light = 現行固定値、dark = セマンティック色の dark 解決値 | B に吸収 (dark 値の出所の違いのみ。半透明色は Android の描き方と合わない) |

### 論点 3: Theme が外観を表す仕組み

| 案 | 評価 |
|---|---|
| P: light / dark の Theme の対を持つ新型を 3 面に追加 | 利用者が減らせるのは「どちらを渡すか」の 1 行だけで、両外観分の定義の手間は変わらない。受け口が二重になる。Compose の慣習に無い発想 (並走調査で両者とも挙げず) |
| Q: 外観を受け取って Theme を返すコールバック | 同上。XAML と相性が悪い |
| R: 型は今のまま、未指定フィールドを描画時に外観で解決する仕組みを土台にし、両外観の値を渡す手段は各 platform の慣用に乗せる | 並走調査の結果 Compose の慣習 (light / dark 2 セット + 未指定の遅延解決) と一致。P / Q はいずれも R の土台の上にしか成り立たない |

### 論点 5: 3 platform の dark 既定値の揃え方

| 案 | 評価 |
|---|---|
| **A: iOS の dark システムパレットの不透明近似を 3 面同値で置く** | **採用**。現行の light 既定値が iOS の light パレットの近似なので同じ出所で対になる。OS 標準色なので中立 |
| B: 独自のダーク配色を新規デザイン | 却下。light は iOS 風のまま dark だけ独自になり、特定の趣味の配色になりやすい |
| C: Material3 の dark baseline | 却下。light と dark で文法が変わる |

### 論点 7: 外観切替時の再適用経路

| 案 | 評価 |
|---|---|
| A: Activity の再生成に任せる | 却下。`configChanges` に uiMode を持つホスト (MAUI 既定テンプレート、一部の Compose アプリ) で表示中に追随しない |
| **B: View が uiMode 変更を受けて未指定の既定を再解決・再適用** | **採用**。既存の Theme 更新経路 (行・canvas・Section 装飾の再評価) を流用。解決済み値のキャッシュ無効化が要る |

### 論点 4: Android の未指定の印と解決先

| 案 | 評価 |
|---|---|
| `Color?` (null = 未指定) | 当初推奨したが並走調査で撤回: Compose では `Color` が inline value class で nullable はボクシングを起こし、公式の sentinel は `Color.Unspecified` (`takeOrElse` で解決)。既存の `cellTitleColor: Color? = null` 群との不揃いは逆にそちらを寄せる方向を検討 |
| `Color.Unspecified` を既定にする | 並走調査の慣習形。型は不変 |
| `Theme.default(context)` ファクトリを利用者が呼ぶ | 却下。Theme を渡さない利用者が直らない |
| 解決先 (i) 同梱の色リソース (values / values-night) | 内部実装としては許容。公開面の正にはしない |
| 解決先 (ii) 同梱 Material3 テーマの attr | 却下。ライト時の見た目が変わる (Material の surface は純白ではない)、Material Components 更新時の追従保守が要る |
| 公開面: Kotlin の light / dark 2 セット (`KsSettingsViewDefaults` 相当の factory) | 並走調査の慣習形。Compose 入口の既定値式で `isSystemInDarkTheme()` により選び、View / MAUI 入口は Configuration の uiMode で同じ 2 セットを選ぶ |


## 決定事項

- スコープを「既定色の追随」から「Theme のダーク対応の仕組み全体」へ統合する (2026-09-05 オーナー裁定)
- 論点 1: A。既定色を全部、外観に追随させる。既存利用者のライト時の見た目は変えない (2026-09-05 オーナー確定)
- 論点 2: B。iOS は light = 現行固定値、dark = 新規生値を `UIColor(dynamicProvider:)` で対にする (2026-09-05 オーナー確定)
- 論点 3: R。Theme の型は今のまま、未指定フィールドを描画時に外観で解決する仕組みを土台にし、両外観の値を渡す手段は各 platform の慣用 (iOS: dynamic `UIColor` / Android: 構築時の分岐 / MAUI: `AppThemeBinding`) に乗せる。対の型・コールバックは作らない (2026-09-05 オーナー確定)
- 論点 4: B + (i)。Theme と CellStyle の色フィールドを全部 `Color.Unspecified` 既定の `Color` 型に揃える (色以外の nullable は対象外)。dark 既定値は Kotlin の light / dark 2 セット (Defaults factory) を唯一の正とし `values-night` は置かない (2026-09-05 オーナー確定)
- 論点 1〜4 を core/ADR-0030 (proposed) に起票済み (2026-09-05)
- 論点 5: A。dark 既定値は iOS の dark システムパレットを不透明に近似した値を採り 3 面同値で置く。iOS の title / description はシステム色 (`.label` / `.secondaryLabel`) のまま、Android は 2 セットに近似値を置く (title の `textColorPrimary` 動的解決はやめてセットの値にする)。ライトの platform 差 (title / description) は現状のまま許容。生値の表は propose のデルタスペックに載せ、モック (light / dark 並び) で承認ゲートを通す。accent の dark 値 (#0A84FF 相当にするか) は表を作るときの細目 (2026-09-05 オーナー確定)
- 論点 6: B。iOS の既定色の `public static let` は残す (型は不変で dynamic になるだけ)。Android 相当の Defaults factory は iOS に作らない (UIColor 自体が両外観を持つため)。3 面同値の検証はテストで dynamic 色を dark trait で解決して比較する (2026-09-05 オーナー確定)
- 論点 7: B。Android のライブラリ View は uiMode の Configuration 変更を受けたら未指定の既定を現在の外観で再解決し、既存の Theme 更新経路で再適用する (明示指定色は変えない)。根拠: MAUI テンプレート既定の MainActivity が `ConfigChanges.UiMode` を宣言し Activity が再生成されないため。iOS は CGColor を layer に置く Section 装飾の枠線を `registerForTraitChanges` で再解決する (2026-09-05 オーナー確定)
- 論点 8: concepts の更新範囲 (蒸留時に ksn-distill が行う) — core/styling/style-resolution.md (解決順への「未指定は現在の外観のライブラリ既定へ」の明記、「platform theme の前提」「既定色と外観の追随」の全面改訂、「Theme 更新」への uiMode 再解決の追記、`Color.Unspecified`)、core/styling/list-appearance.md (iOS Footer 固定 gray の段落改訂、`sectionBorderColor` の Android 型)、android/api/android-native-host.md (public 定数の段落と定数表を Defaults factory + `Unspecified` の契約へ。「既定へ戻す」= `Color.Unspecified`、「派生値を作る」= factory の値から。uiMode 再解決)、ios/api/ios-native-host.md (定数表は維持、dynamic である旨、枠線の trait 再解決)、maui/api/maui-styling.md (native 既定は外観に追随する注記)。handbook cross/sample-parity.md は変更なし。既存の「public 定数として公開」契約を裏付ける accepted ADR は無い (2026-09-05 オーナー確定)

## ADR 候補 (作成済み: core/ADR-0030 (proposed、論点 1〜4) / 未起票: なし)

## 未決の論点

- 論点 8: 未議論 (上の「検討した選択肢」に並走調査を反映した候補あり)
- MAUI の `AppThemeBinding` が facade の色プロパティ経由で Theme 再適用まで届くかは未検証 (実装前に実物で確認)
- 既定値の等価性テスト (iOS `isEqual`) が dynamic な UIColor で通るかは実装時の確認事項
- 並走調査が本 change の範囲外として挙げた指摘: `Theme` の全部入り data class は Compose Component API Guidelines の非推奨形 (grab-bag style)。core/ADR-0009 の領分であり別途扱う
- 既定の背景・Cell 背景・separator・header / footer 文字色を dynamic / 夜間対応にするか (公開 API の既定の見た目が変わる利用者可視の変更。既存利用者のライト時の見た目は変えない設計にできるか)
- 3 platform で既定の dark 値をどう揃えるか (iOS はシステム色、Android は同梱テーマの属性 or `values-night`、MAUI は native 追随でよいか)
- 既定色の platform 差 (sample-parity の「本体既定値の platform 差」) との関係。dark でも同じ扱いにするか
- 「文字色だけ追随して背景が追随しない」現状は既存不具合として扱うか (利用者がダーク端末で Theme を渡さずに使うと判読不能)
- concepts (style-resolution.md) の記述修正の範囲

## UI 素材 (ui/references/ の一覧と注釈)

## 調査記録: 並走調査 (ksn-dual-research、2026-09-05)

相方 codex とホスト側 ksn-researcher に「Compose 開発者にとっての色リソース管理の通例・慣習・ベストプラクティスをゼロベース・バイアスなしで」の原文をそのまま渡し、独立に調査させた (label: dual-compose-colors)。判定: 双方一致、追ラウンドなし。

双方一致した結論:
- 色は 3 層 (原色トークン internal → 用途名のロール public → light / dark ごとの割り当て)。描画コードは用途名だけを参照し、色相名や生 RGB を直接書かない
- Compose の `Color` は不変の値で外観に応じて変わる色は存在しない。ダーク対応は light / dark の 2 セットを定義し composition 時に `isSystemInDarkTheme()` で選ぶ一択 (ライブラリの `DateCalendarDialog` が既にこの形)
- ライブラリが独自の light / dark 配色を所有するのは Compose 的に不自然ではない (公式の Fully custom design system)。ホスト追随が既定の期待ではあるが、視覚隔離 (android/ADR-0020) を選んだ以上、独立型として曖昧にしない
- 明示指定された色は外観で変えない。未指定の色だけを描画直前に現在の外観へ解決する。旧既定値との値比較で未指定を推測しない
- `values-night` は View をラップするライブラリの内部実装として自然だが、Compose 利用者向けの公開テーマの正本にはしない
- 既定値は `XxxDefaults` オブジェクトに集約し、`@Composable` factory で現在のテーマを読む。実装本体で CompositionLocal を読まず既定値式で読む
- 既存利用者のライト時の見た目は保てる (light セット = 現行定数)

発見事項:
- 未指定の表現は `Color?` ではなく `Color.Unspecified` (ホスト側、androidx `Text.kt` の `color: Color = Color.Unspecified` + `takeOrElse`)
- 40 フィールド超の全部入り `Theme` は Component API Guidelines の非推奨形 grab-bag style (ホスト側。本 change の範囲外)
- 既定色の public 定数 (`0xFF007AFF` 等) は internal 化し公開面はロール名だけにするのが慣習 (両者)
- 背景と前景は対で切り替える。「文字だけ dark、背景は白固定」は Android 公式が挙げる典型的な失敗 (相方)。相方は KsSettingsView の色ロールと Material3 ロールの対応 (list 下地 = background、Cell 背景 = surface、description / Header 文字 = onSurfaceVariant、separator = outlineVariant、accent = primary、selected = secondaryContainer 相当) を「値ではなく関係を借りる」目的で提示
- ホスト追随 (`MaterialTheme.colorScheme` から写す) は将来の opt-in adapter で足せる (両者。Native / MAUI との非対称と ADR-0020 改訂が要る)
- 同ドメインの実例 alorma/Compose-Settings は自前の色を持たずホストの `ListItemDefaults.colors()` へ委譲 (ホスト側)

両者の推奨: 「ライブラリ所有の light / dark 2 セット + 未指定だけ描画時に解決」(ホスト追随の案は ADR-0020 と View / MAUI 入口に composition が無いことから両者とも非推奨)。

未解決の論点: なし (主要論点で異論なし)。

## 変更級の推奨: L (2026-09-05 オーナー確定)

- 触る能力: iOS UI (Theme 既定値・Section 装飾の trait 再解決)、Android UI (Theme / CellStyle の型変更・Defaults factory・EffectiveStyle の解決・uiMode 再解決・bridge の変換)、samples 3 面と tests の追随、concepts 5 文書。複数能力横断
- 公開 API の変更: あり。Android の色フィールドの型変更 (`Color?` → `Color`、`Unspecified` 既定) と public 定数の internal 化は破壊的。iOS は型不変、MAUI facade は不変
- 可逆性: 低い。3 platform の既定の見た目 (dark) が製品契約に加わる (core/ADR-0030)
- UI: あり。既定色のダーク側は見た目の変更なので `ui/` (モック: light / dark の並び) の承認ゲートを通す
- 1 変更に収まる規模 (仕組み・既定値・再適用経路は同じ根から出ており分割すると中途半端な状態を経る) のため、ロードマップへのエスカレーションはしない
- ハンドオフ: ksn-propose (フル: proposal + design + デルタスペック + ui/ + スペックレビュー。second-opinion.spec-review の該当級)
