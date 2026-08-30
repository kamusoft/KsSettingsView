---
id: 0019
title: DatePickerCell のカレンダー型 UI は Compose Material3 DatePicker のダイアログ表示に統一する
status: accepted
date: 2026-08-27
supersedes: 0008, 0010, 0011
---

## Context

DatePickerCell はカレンダー型とホイール型 (Spinner) を利用者が選べるのが仕様である。カレンダー型は `MaterialDatePicker` (DialogFragment) を FragmentManager 経由で表示しており、Compose テンプレート標準の `ComponentActivity` ホストでは表示できない (無言 no-op)。ComponentActivity 対応が必須要件のため、ホスト Activity 型によってカレンダー型が使えなくなる解は採れない。

裏取りの結果: `MaterialDatePicker` は DialogFragment サブクラスで内部でも childFragmentManager を使うため正規の Fragment ホスト環境が必須であり、ComponentActivity で表示する公式にサポートされた手段は存在しない。一方 Compose Material3 の `DatePicker` は純 composable で FragmentManager 不要。ui モジュールは CustomCell の Compose ホスティングのため compose material3 に既に依存しており (ADR-0016)、MAUI binding も Xamarin.AndroidX.Compose.* パッケージと AppCompat 1.7.1 を配達済みのため、新規依存なしで採用できる。appcompat 1.7.0 以降は `AppCompatDialog` が ComposeView に必要な ViewTree owner (Lifecycle / SavedStateRegistry / OnBackPressedDispatcher) を自動設定するため、ダイアログに ComposeView を載せる構成が ComponentActivity で成立する。

## Decision

`MaterialDatePicker` による表示経路を廃止し、DatePickerCell のカレンダー型 UI は**全ホスト共通**で「ComposeView を載せたダイアログに Compose Material3 の `DatePicker` を描画する」方式に統一する。配色は表示後の View 走査ではなく `DatePickerColors` パラメータで正面から指定する。ホイール型 (ボトムシート + 3連ホイール、ADR-0009) は従来どおり維持し、カレンダー/ホイール選択可の仕様を全ホストで保つ。

## Alternatives Considered

- **FragmentActivity ホストでは `MaterialDatePicker` を維持し、ComponentActivity のみ Compose 版を表示 (併用)**: 既存ホストの見た目を完全維持できるが、カレンダー実装2系統 (配色・今日ジャンプ・回転復元をそれぞれ) を機能同等に恒久保守することになり、以後の機能追加が全て2倍になるため却下。
- **ホストに AppCompatActivity を要求し続ける**: Compose 専業アプリでも `AppCompatActivity` + `setContent` は成立する業界標準の回避策だが、ComponentActivity 対応必須というオーナー要件に反するため却下。
- **`FragmentController` / `FragmentHostCallback` でライブラリが自前 Fragment ホストを合成**: public API だが公式にサポートされたユースケースではなく、SavedStateRegistry・ActivityResult・各種 provider の dispatch を全て自前で正しく実装する責務を負う高リスク領域のため却下。
- **カレンダー型を廃しホイール型のみにする**: カレンダー/ホイール選択可の仕様に反するため却下。

## Consequences

- 正: ComponentActivity を含む全ホストでカレンダー型が動作し、カレンダー/ホイール選択可の仕様がホスト型に依存せず保たれる。
- 正: TimePickerCell のシート統一 (ADR-0018) と合わせて Material ピッカーの DialogFragment が消滅し、FragmentActivity 依存 (`findFragmentManager()` と Fragment ベースの回転復元機構 ADR-0011) を完全撤去できる。
- 正: 配色が `DatePickerColors` で正面から指定でき、表示後の内部 View 走査による配色・ヘッダ補正ハック (ADR-0008) が不要になる。
- 正: MAUI からも追加依存なしで動作する (Compose ランタイムは binding が配達済み)。
- 負: Compose Material3 の DatePicker は experimental API 表記であり、compose 版更新への追随リスクを負う。MAUI 側は Gradle の compose 版と NuGet (Xamarin.AndroidX.Compose.Material3) の版整合を保つ規律が必要。
- 負: 既存ホストでカレンダーの見た目の細部が変わる (M3 スペック準拠同士でほぼ同一だが完全一致ではない)。
- 負: カレンダーの今日ジャンプ (ADR-0010 の機能) は Compose ダイアログ側で再実装が必要 (状態操作で正面から実現でき、View 階層駆動は不要になる)。
- 負: 回転復元は Fragment 機構に代わる自前機構 (表示中状態の保存 + rememberSaveable) の新設が必要。

> 追記 (2026-08-28、オーナー承認): 実装で確定した2点を補足する。
> (1) **experimental API への依存はカレンダーダイアログの1箇所に限定**される — Compose 自体 (runtime / ui / foundation) は stable であり、`@ExperimentalMaterial3Api` の opt-in を要するのは material3 の `DatePicker` のみ。TimePickerCell は自作ホイールのため非依存。
> (2) **版整合の既定は「Gradle コンパイル版を MAUI ランタイム解決版 (compose 1.11 系 / material3 1.4 系) に追随させる」**とする。MAUI 本体の依存連鎖が Compose ランタイム版を事実上固定するため、NuGet 側を下げる整合は成立しない (実測: kasane/changes/relax-android-host-prerequisites/evidence/spike-consumer-pin-build-log.md)。利用者アプリ側の直接ピン + ExcludeAssets による旧版固定は技術的には可能だが、運用負担と実行時リスクから既定にしない (同 evidence)。MAUI の minor 更新 (10.0.70 → 10.0.100) では AndroidX 依存集合が動かないことも実測済み (同 evidence/spike-maui-100-build-log.md)。

出典: kasane/changes/relax-android-host-prerequisites/exploration.md (決定事項 2・現状の裏取り・検討した選択肢)
