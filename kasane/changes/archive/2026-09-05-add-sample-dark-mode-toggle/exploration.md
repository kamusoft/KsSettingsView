# Exploration: add-sample-dark-mode-toggle

起票日: 2026-08-27 (簡易起票) / 探索日: 2026-09-05 / 起票元: relax-android-host-prerequisites の視覚検証 (material3 1.4.0 再照合) で、dark テーマと範囲つきカレンダーの検証にサンプルの一時改変が必要だったことから。オーナー判断で起票

## 課題 / 動機

サンプルアプリにダークモードの切り替え手段がなく、ライブラリのダーク配色 (Theme の dark 値・端末夜間モードでの既定色) を目視確認するには、検証のたびにサンプルへ一時パラメータ (dark Theme の色値等) を投入して撮影後に戻す手当てが必要。Compose 版の更新ごとに視覚再照合が要る運用 (binding csproj の版整合規律) では、この手当てが毎回発生する。

探索でコードを確認した現状:

- ダークの決定源は platform で異なる。iOS のライブラリ既定色はシステム色 (`UIColor.label` 等) でアプリ外観に自動追随する。Android は同梱 DayNight テーマのため端末夜間モードかアプリの uiMode 制御で決まる ([android/ADR-0020](../../decisions/android/0020-bundled-theme-always-wrap-host-independent.md))。MAUI は `Application.UserAppTheme` で両 OS の外観を一括で切り替えられる
- デモ画面の大半 (基本 Cell 7 種 / 入力 Cell 5 種 / CustomCell) は `SampleTheme.maui` (AiForms 互換の固定 light 色) を明示しているため、端末をダークにしても暗くならない。ライブラリ既定色のまま (= 外観に追随する) なのは Store 方式 / DSL 方式 / 共通フィールド統合 / isVisible の 4 画面と Section 装飾デモの一部。「dark Theme の色値を一時投入」が必要だったのは、**Theme の dark プリセットがサンプルに無い**ため
- Android サンプル自身の chrome (Manifest の `Theme.Material.Light.NoActionBar` と Compose 既定の `MaterialTheme`) は常時ライトで、夜間モードに追随しない。iOS の SwiftUI chrome は自動追随する
- 範囲つきカレンダー: 「誕生日」(ホイール型) には 1900-01-01〜今日の min/max があるが、カレンダー型の「予約日」には範囲指定が無い。カレンダー型の範囲外 disabled を常設で確認できる画面は無い
- 12 時間制 TimePicker の parity は [align-timepicker-hour-cycle-across-platforms](../archive/2026-08-28-align-timepicker-hour-cycle-across-platforms/proposal.md) (2026-08-28 アーカイブ) で 3 面に `is24Hour = false` のデモが常設済み。この論点は解消

## 検討した選択肢 (却下案と理由を含む)

### 切り替えの形

| 案 | 内容 | 評価 |
|---|---|---|
| A: Theme の light/dark プリセット切替だけ | ライブラリ API だけで閉じる | 却下。Theme の dark 値は確認できるが、ライブラリ既定色のダーク (外観で決まる) は確認できない |
| B: アプリ外観の切替だけ | OS のダークをアプリ内で切り替える | 却下。固定 light 色を明示している画面が暗くならず、Theme の dark 値を確認できない |
| **C: 外観トグル + ダーク時は dark プリセットへ差し替え** | ルートメニューの外観 (システム / ライト / ダーク) で全体の外観を切り替え、`SampleTheme.maui` を使う画面はダーク時に対の dark プリセットを渡す | **採用**。操作 1 回で両方の確認対象が揃う。dark プリセットの配色を新たに決める作業が増える |

### Android でアプリ外観を切り替える手段

| 案 | 評価 |
|---|---|
| **1: OS の uiMode 制御 (`UiModeManager.setApplicationNightMode`、API 31+)** | **採用**。`ComponentActivity` のまま切り替えられ、Activity 再生成で Configuration が変わるのでライブラリ既定色も切り替わる。制約: minSdk 29 に対し API 31 以上でのみ有効 (29/30 はシステム追随のみ)。設定は OS に永続化され、再生成でデモ内の入力状態は消える |
| 2: サンプルを AppCompatActivity 化して `AppCompatDelegate.setDefaultNightMode` | 却下。API 29 から動くが、サンプルが担う「最小ホスト (`ComponentActivity` + 素のテーマ) でも全 Cell が動く」の検証装置の役割 (android/ADR-0020) を手放すことになる |
| 3: サンプル側で Context の Configuration を上書き (`createConfigurationContext` + `LocalContext` 差し替え) | 却下。ライブラリの `ksThemedContext()` が中間の ContextWrapper を素通りして Activity / Application の Configuration から夜間モードを解決するため、効かない |
| Android は端末設定に任せる (トグルは iOS / MAUI だけ) | 却下。sample-parity の例外扱いが必要になり、片側限定の差異が恒久化する |

## 決定事項

- 切り替えの形は C (2026-09-05 オーナー確定)。ルートメニューに外観の切替 (システム / ライト / ダーク) を置き、ダーク時は `SampleTheme.maui` の対となる dark プリセット (仮称 `SampleTheme.mauiDark`) へ差し替える。3 platform とも同じ文言・構成で置く (sample-parity)
- ~~Android の外観切替は OS の uiMode 制御 (`UiModeManager.setApplicationNightMode`) で行う (2026-09-05 オーナー確定)~~ → **改訂 (同日、提案の相方レビュー second-opinion-spec-001 で判明)**: `setApplicationNightMode` は NO / YES / AUTO (位置・センサー) / CUSTOM (時刻) しか受けず「端末に追随する」値が無いため「システム」を実現できない。代わりにサンプルの Activity 自身が `attachBaseContext` で Configuration (uiMode) を上書きし `recreate()` で反映する手段 4 を採る (「システム」= 上書きなし)。`ComponentActivity` は維持、API 29 から動く、OS 設定への副作用なし。上書きでライブラリ UI・夜間リソース・Compose が揃って切り替わることは spike で実証する (オーナー再判断 2026-09-05)
- Android サンプル自身の chrome (Manifest テーマ・Compose の `MaterialTheme`) も夜間モードに追随させる。ライブラリ部分だけ暗くなり TopAppBar が白いまま、というスクショを避けるため。sample-parity の「許容される差異 (platform の見た目そのもの)」の範囲内
- 範囲つきカレンダーは同梱する。入力 Cell 5 種デモの「予約日」(カレンダー型) に min/max を付け、範囲外 disabled が見える状態にする。3 面同時に追随
- 共通フィールド統合デモの「テーマ: ライト / ダーク / 自動」の RadioCell は RadioCell のデモデータであり、外観トグルとは接続しない (接続すると parity 検証のデモの意味が変わる)
- 12 時間制 TimePicker の parity 追随は解消済みのためスコープ外

## ADR 候補

なし。いずれもサンプル内で閉じる可逆な決定で、覆すコスト・境界・将来制約の選別基準に該当しない。Android の手段選択は既存の android/ADR-0020 の検証装置としての役割を保つ判断であり、新たな決定ではない

## 未決の論点

- dark プリセットの配色: AiForms 原典にダーク配色は無く、この change で新たに決める。文章で決めず ksn-propose の UI モック (light / dark の並び、`ui.mock-variants: 2`) で見て決める
- 外観切替 UI の置き方の細部: ルートメニュー内の位置 (デモ群の上か下か)、行の形 (セグメント / ピッカー / 3 行選択)。文言は「外観」「システム」「ライト」「ダーク」で 3 platform 一致させる前提。モックで確定
- 「予約日」の範囲の値: 今日を含む将来側の範囲 (例: 今日〜数十日後) を想定。範囲外 disabled が同じ月内で見える値にする。デルタスペックで確定
- Android の外観設定が OS に永続化される点の扱い: 検証運用上は再起動後も維持されるほうが便利と見ているが、アプリ起動時に「システム」へ戻すかは提案で決める

## UI 素材 (ui/references/ の一覧と注釈)

なし (探索中に画像の提示なし)

## 変更級の推奨: M (2026-09-05 オーナー確定)

- 触る能力: samples-ios / samples-android / samples-maui の 3 つ。ライブラリ本体は触らない
- 公開 API の変更: なし。可逆性: 高い (サンプル内で閉じる)
- UI: あり。外観トグルの置き場と新設する dark プリセットの配色はモックで見て決めたい
- サンプル限定の点は S 寄りだが、3 platform を同一の文言・構成で揃える必要と、新しい配色の判断をモック無しで進めると手戻りしやすいことから「迷ったら 1 段上」で M。ksn-propose で proposal + デルタスペック + ui/ (モック承認ゲート) を作る
