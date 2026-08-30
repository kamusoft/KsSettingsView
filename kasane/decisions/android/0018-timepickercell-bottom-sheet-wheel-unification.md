---
id: 0018
title: TimePickerCell の選択 UI は全ホストでボトムシート + 時分ホイールに統一する
status: accepted
date: 2026-08-27
supersedes: 0006
---

## Context

TimePickerCell は時刻選択 UI として `MaterialTimePicker` (DialogFragment、時計ダイヤル) を FragmentManager 経由で表示している。Compose テンプレート標準の `ComponentActivity` をホストにすると FragmentManager が取得できず、該当行をタップしても何も起きない無言 no-op になる (`findFragmentManager()` が null → 早期 return)。オーナー判定により、ホスト Activity 型の前提 (FragmentActivity 必須) を外し ComponentActivity で動作することが必須要件となった。

加えて、時計ダイヤルは時刻入力 UI として使いにくいという評価があり、iOS 版の TimePicker はホイール型のため、Android もホイール型に寄せるとプラットフォーム間の UI の対照性が取れる。ライブラリには既にボトムシート + 自作ホイールの実装系 (NumberPickerCell の ADR-0007、DatePickerCell Spinner 型の ADR-0009) があり、FragmentManager を要求しない表示経路の実績がある。

## Decision

`MaterialTimePicker` による表示経路を廃止し、TimePickerCell の選択 UI は**全ホスト共通**でボトムシート + 時・分ホイール (既存の自作ホイール部品系) に統一する。

## Alternatives Considered

- **FragmentManager が取れないホストのみシートに切替 (時計ダイヤルと併存)**: ホストの Activity 型でピッカー UI が変わる。時計ダイヤル自体が使いにくく併存して残す価値がないため却下。
- **DialogFragment をやめ Dialog 直接表示で時計ダイヤルを再構築**: 実装コストが最大で、使いにくい UI を温存することになるため却下。
- **Compose Material3 の TimePicker (時計ダイヤル) を利用**: FragmentManager は不要になるが、UI は同じ時計ダイヤルで使いにくさが残るため却下。
- **ホストに FragmentActivity (AppCompatActivity) を要求し続ける**: ComponentActivity 対応必須というオーナー要件に反するため却下。

## Consequences

- 正: ComponentActivity を含む全ホストで TimePickerCell が動作し、「タップしても何も起きない」無言 no-op が解消される。
- 正: iOS のホイール型 TimePicker と UI の対照性が取れる。
- 正: 時計ダイヤルの動的配色を担っていた表示後の内部 View 走査 (ADR-0006) が対象 UI ごと不要になり、ハックを撤去できる。
- 負: 時計ダイヤルおよびキーボード入力モードは提供されなくなる。既存の FragmentActivity ホストでは見た目と操作が変わる破壊的な視覚変更。
- 負: 時・分ホイールのボトムシートを新設する実装 (UI 作業) が必要。
- 負: Material ピッカーダイアログの回転復元 (ADR-0011) の対象から外れる。シート系は復元対象外のため、回転時にシートが閉じる挙動になる (既存のシート系 Cell と同等)。

出典: kasane/changes/relax-android-host-prerequisites/exploration.md (決定事項 1・検討した選択肢)
