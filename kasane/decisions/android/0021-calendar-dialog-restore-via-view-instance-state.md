---
id: 0021
title: カレンダー選択面の回転復元は KsSettingsView の View インスタンス状態で自前化し、既定 ID の自前付与で成立させる
status: accepted
date: 2026-08-27
---

## Context

FragmentActivity 依存の撤廃 (ADR-0019) に伴い、Material ピッカーダイアログの回転復元機構 (ADR-0011: FragmentManager の saved state + コンテナ駆動の完全復元) は土台ごと消滅した。新しいカレンダー選択面 (`DateCalendarDialog`: ComponentDialog + ComposeView) は DialogFragment ではないため、Activity 再生成でダイアログが生き残る仕組みを持たず、回転をまたぐ表示継続には代替機構が必要になった。

Android 標準の View 階層インスタンス状態保存は、ID を持つ View にしか状態を保存しない。実装前のスパイク (View 直置き / Compose `AndroidView` の各ホスト形態) で、`KsSettingsView` に安定した View ID がある場合は両形態とも標準経路で保存・復元が成立し、ID がない場合は `onSaveInstanceState` 自体が呼ばれないことを実測確認した。Compose `AndroidView` は `saveHierarchyState` を Compose 側の SaveableStateRegistry へ登録するため、中継実装は不要だった。

## Decision

`KsSettingsView` に `onSaveInstanceState` / `onRestoreInstanceState` を実装し、表示中カレンダー選択面の状態 (対象 `cell.id`・選択日・表示月・表示モード) を View インスタンス状態として保存する。復元は attach 後、保存された `cell.id` と同一 ID の DatePickerCell (uiStyle `Material`) が復元後の root に存在する場合に限り、保存状態で選択面を再表示する。不成立なら再表示せず、いかなる Cell へも値を書き込まない (ADR-0011 が確立した設計原則の踏襲)。

- **既定 ID の自前付与**: `KsSettingsView` は自身の `id` が未設定 (`NO_ID`) のときだけライブラリ既定 ID (`R.id.ks_settings_view`) を自前で付ける。ホストが明示した ID は上書きしない。これにより View 直置き / Compose `AndroidView` の両ホスト形態で、ホスト側の追加作業なしに復元が成立する
- **縮退条件**: ライブラリ既定 ID のインスタンスが同一階層に複数ある構成では、保存先が衝突して状態が混ざるため復元しない (ADR-0011 の単独 attach 原則の踏襲)。ホストが個別の ID を与えれば復元は成立する
- 状態保存に参加できないホスト形態は「回転で閉じる (無発火)」の縮退契約とする (シート系選択面と同じ挙動)。スパイクでは縮退が必要な形態は出なかった
- 保存時は状態の書き出しのみを行い、選択面を閉じるのはホスト破棄の購読 (`showAnchoredTo` の ON_DESTROY) と detach 経路が担う — 構成変更ではないバックグラウンド遷移 (Home キー等) で選択面が失われない
- シート系選択面 (PickerCell / NumberPickerCell / DatePickerCell (Spinner) / TimePickerCell) は従来どおり復元対象外 (回転で閉じる)

## Alternatives Considered

- **ダイアログ内の rememberSaveable のみに頼る**: ダイアログは Activity と共に破棄され、再表示の主体が存在しないため単独では成立しない。却下。
- **プロセス内シングルトンで表示状態を保持**: プロセス再生成で消え、View の寿命と絡んで所有関係が曖昧になる。却下。
- **ホストに安定した View ID の付与を要求する** (設計時の当初前提): スパイクで「未設定時のみ既定 ID を自前付与」で足りると確定し、ホスト要件なしのより緩い条件を採用。rememberSaveable の自前中継も検証したが、標準経路と二重保存になるため採らない。

## Consequences

- 正: FragmentActivity / FragmentManager に依存せず、ComponentActivity ホストでも回転をまたぐカレンダー選択面の表示継続が成立する。
- 正: 復元後の選択面でも配色・今日ジャンプ・確定/破棄の契約が全て有効になる (旧機構の「復元後は配色対象外」の既知問題が再表示方式で構造的に解消)。
- 正: ホスト側の追加作業 (ID 付与・状態中継) なしで復元が成立する。
- 負: ライブラリ既定 ID の複数インスタンス構成では復元しない (ホストの個別 ID 付与が必要)。
- 負: 既定 ID 付与の副作用として、ホストが `findViewById(R.id.ks_settings_view)` で本 View を引けるようになる (公開契約としては謳わない)。
- 負: Compose ラッパ利用時の保存キーは composition キーに従属する — ラッパ内の `AndroidView` 呼び出し位置を安易に動かせない。

出典: kasane/changes/archive/2026-08-28-relax-android-host-prerequisites/design.md (Decision 5) / kasane/changes/archive/2026-08-28-relax-android-host-prerequisites/evidence/spike-findings.md / kasane/changes/archive/2026-08-28-relax-android-host-prerequisites/deviation.md (既定 ID の自前付与・複数インスタンスの縮退条件・保存と閉じの分離)
