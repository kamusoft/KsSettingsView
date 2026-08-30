# ADR-0005: PickerCell の選択 UI はボトムシート (Material BottomSheetDialog) で表示する

- Status: accepted
- Date: 2026-08-02
- Domain: android
- 関連: [ADR-0001](0001-content-update-preserves-viewholder.md) (ViewHolder 維持の既存決定)

## Context

Android の PickerCell は選択 UI として `AlertDialog` (`setSingleChoiceItems` / `setMultiChoiceItems`) を表示している (`PickerCellViewHolder.showPickerDialog`)。オーナーの評価は「ダイアログ選択は古臭くて求めていたものではない」。

比較対象は3つある (スクリーンショット: `kasane/changes/android-picker-selection-sheet/ui/references/`):

- **現実装 (Android)**: AlertDialog + Radio/Checkbox 描画
- **オリジナル AiForms**: ページ遷移形式。ただしこれは MAUI 側のナビゲーション機構で実現されており、KsSettingsView の Android ライブラリが同じことをするにはページ遷移機構を自前で抱える必要がある
- **KsSettingsView iOS**: ページシート (`PickerListViewController`)。行は「タイトル左 + チェックマーク右 (accentColor tint)」、単一選択はタップ即確定 dismiss、複数選択は Cancel / 完了 ボタン、選択上限到達で haptic

挙動仕様 (確定 callback のタイミング・`maxSelectedNumber`・haptic) は iOS と Android で既に揃っており、変更対象は「器」と「行の見た目」だけである。また本プロジェクトの利用アプリは Theme.Material3 が必須要件のため、material ライブラリは既に依存にある。

## Decision

PickerCell の選択 UI を `AlertDialog` から Material の `BottomSheetDialog` によるボトムシートに変更し、中身は iOS のレイアウトを再現する。

- ヘッダー: 左に「キャンセル」、中央に `pageTitle ?: title`、複数選択時のみ右に確定ボタン (iOS の navigation bar 構成と同型)

**追補 (2026-08-02, オーナー承認)**: 選択印の描画は専用ベクター drawable の新設ではなく、既存の `KsSimpleCheckView` (RadioCell / SimpleCheckCell と共有の Canvas 描画) を accent 色で再利用する。見え方の意図 (Checkbox/Radio ではない独自チェック・accent 色) は同一であり、ライブラリ内のチェック表現を1系統に保つ。

**追補 (2026-08-02, オーナー決定)**: 確定・キャンセルの操作ラベルは OS の公開文字列リソース (`android.R.string.ok` / `android.R.string.cancel`) を用い、自前文字列の同梱を避ける。Android の公開リソースに「完了 (Done)」は存在せず (AppCompat/Material の内部リソースは private で依存不可)、自前同梱よりも OS ローカライズに追従する「OK」を採る。iOS の「完了」と文言は揃わないが、各 OS の慣習語彙を優先する。
- リスト: RecyclerView で「タイトル左 + チェック右」。チェックは Checkbox/Radio ではなく、チェックマークのベクター drawable を accentColor で tint して表示/非表示を切替える (iOS の `.checkmark` accessory と同じ見え方)
- 単一選択: タップ即確定 dismiss / 複数選択: 完了で確定。上限・haptic は現挙動を移植する
- 高さ: コンテンツ高で表示し、画面約半分を上限に内部スクロール。ドラッグで全展開可 (Material の標準挙動)

挙動仕様 (callback のタイミング・上限・haptic) は変更しない。

## Alternatives Considered

- **ダイアログ現状維持 (AlertDialog)** — 却下: 古臭く、求めていた UX ではない (オーナー評価)
- **ページ形式 (AiForms 同型)** — 却下: AiForms は MAUI 側のナビゲーション機構で実現しており、KsSettingsView の Android ライブラリが同じことをするにはページ遷移機構を自前で抱える必要がある
- **自前オーバーレイ実装** — 却下: material ライブラリは既存依存であり、`BottomSheetDialog` なら sheet の出現・ドラッグ dismiss・アニメーションが標準で得られる。自前実装は保守コストに見合う利点がない
- **高さ: 常にほぼ全画面 (iOS ページシート踏襲)** — 却下: 項目が少ないときに空っぽの面が立ち上がり Android では不自然。「下から出る選択面」という体験の同質性は保ちつつ、高さは Android の慣習に合わせる
- **高さ: 固定比率 (例: 50%)** — 却下: 同上 (項目が少ないときの空白が目立つ)

## Consequences

- 正: 追加依存なしでモダンな選択 UI になり、iOS と「下から出る選択面」の体験が揃う
- 正: sheet の出現・ドラッグ dismiss・edge-to-edge 対応を Material 標準挙動として得られる
- 正: チェック描画が独自 drawable になり、iOS と行レイアウトが揃う
- 負: iOS ページシートとは高さの見え方が完全一致はしない (コンテンツ依存の高さ)
- 負: 既存の AlertDialog 表示に見慣れた利用者アプリでは見た目が変わる (挙動仕様は不変だが視覚的変更が入る)

出典: kasane/changes/android-picker-selection-sheet/exploration.md / 探索会話 (2026-08-02)
