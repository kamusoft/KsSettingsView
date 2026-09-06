# Proposal: add-sample-dark-mode-toggle

## Why

サンプルアプリにダークモードの切り替え手段がなく、ライブラリのダーク描画 (Theme の dark 値・外観に追随する既定色) を目視確認するには、検証のたびにサンプルへ dark Theme の色値を一時投入して撮影後に戻す手当てが要る。Compose 版の更新ごとに視覚再照合が要る運用では、この手当てが毎回発生する。

デモ画面の大半は AiForms 互換の固定 light 色 (`SampleTheme.maui`) を明示しているため、端末をダークにしても暗くならない。確認したい対象は「Theme に dark 値を渡したときの描画」と「Theme を渡さない画面のライブラリ既定色のダーク描画」の 2 つで、どちらも「外観をダークにしたとき」の確認である ([exploration.md](exploration.md))。

あわせて、カレンダー型 DatePickerCell の範囲外 disabled を常設で確認できる画面が無く、同じ一時投入で確認している。

## What Changes

- **外観の切り替え**をルートメニューに常設する。選択肢は「システム / ライト / ダーク」。選択はアプリ全体の外観 (iOS の light/dark、Android の夜間モード、MAUI の `UserAppTheme`) に反映され、再起動後も維持される
- **dark プリセット**: `SampleTheme.maui` と対になる dark Theme (仮称 `SampleTheme.mauiDark`) を 3 面に新設する。Section 装飾デモの下地 Theme も同じく light/dark の対を持つ。`SampleTheme` の Theme を渡している画面は、外観がダークのとき dark 側を渡す (「システム」選択時は端末の外観に従う)。配色は AiForms 原典に無いため本 change で決める (ui/ のモックで確定)。dark 側は description / valueText の色も明示する (未指定時の既定色が外観に追随しないため)
- **Android サンプルの chrome** (Manifest テーマ・Compose の `MaterialTheme`) を夜間モードに追随させる。Android の切り替えはサンプルの Activity 自身が Configuration (uiMode) を上書きして再生成する形で行い (「システム」は上書きなし = 端末追随)、Activity は `ComponentActivity` のまま維持する。OS のアプリ単位夜間モード設定 (`UiModeManager.setApplicationNightMode`) は「端末に追随する」値を持たないため使わない (second-opinion-spec-001.md の指摘で判明、2026-09-05 オーナー再判断)
- **範囲つきカレンダー**: 入力 Cell 5 種デモの「予約日」(カレンダー型) に min/max (2026/06/01〜06/20) を付け、範囲外 disabled が同じ月の表示内で見える固定範囲にする (各 platform のカレンダーは前後の月の日を描画しないため、範囲は月の途中で切る)
- MAUI サンプルのナビゲーションバー (固定色) は外観の対象外とし、ページ下地とライブラリ UI だけを追随させる (バーは両外観で判読できる固定色のため変えない)
- 3 面の文言・構成は sample-parity に従い一致させる
- 影響能力: samples-ios / samples-android / samples-maui

## Non-Goals

- ライブラリ本体 (Theme の既定値・夜間モード解決) の変更 — サンプル内で閉じる変更であり、既定色の方針は [スタイルの所有と実効値解決](../../concepts/core/styling/style-resolution.md) の通り維持する
- 共通フィールド統合デモの「テーマ: ライト / ダーク / 自動」RadioCell と外観切替の接続 — RadioCell のデモデータであり、接続すると parity 検証のデモの意味が変わる (探索での決定)
- Cell に明示指定しているデモ色 (`demoAccent*`・CustomCell content の色・`demoPlaceholderOrange` 等) の dark 版 — これらは「各 Cell に渡すパラメータを 3 面で一致させる」ための固定値であり、外観に依らず同一の RGBA を渡すのが目的 (sample-parity)。dark 版が要るかはモックで見て判断し、要るなら別 change
- 12 時間制 TimePicker の parity — [align-timepicker-hour-cycle-across-platforms](../archive/2026-08-28-align-timepicker-hour-cycle-across-platforms/proposal.md) で解消済み

## Impact

- 破壊的変更なし (公開 API に触れない。サンプルの表示文字列・デモデータは製品契約ではない)
- Android の外観切替は Activity の再生成を伴い、デモ画面内の入力状態は消える。外観の行はルートメニューにあるため、切り替えは常にルートで行う
- 外観の選択は 3 面ともサンプル自身が永続化する (OS 側の設定には触れない)
- リスク: Activity 単位の Configuration 上書きで夜間リソースと Compose の Configuration が切り替わることは未実証。tasks の先頭で spike として確認し、崩れたら探索へ戻す
- リスク: dark プリセットの配色が「AiForms 互換」の意図から外れる可能性。light 側の各色ロール (下地 / Cell 背景 / separator / accent / header・footer 文字) をそのまま暗色へ写した対とし、accent (#FFBF00 系) は維持してモックで見比べる

## 級: M

サンプル 3 面の同期変更 (公開 API なし・可逆) だが、新規配色の判断をモック承認で固めたいため 1 段上の M。3 能力横断だがライブラリ本体に触れない同形の前例 (align-timepicker-hour-cycle-across-platforms) に倣い、オーナー確定 (2026-09-05) で M 運用とする。

domain: cross
