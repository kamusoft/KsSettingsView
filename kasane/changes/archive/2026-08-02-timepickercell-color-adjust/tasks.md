# Tasks: timepickercell-color-adjust

## 1. 実機検証スパイク (ADR-0006 の机上確定 3 点。以降のタスクの前提)

- [x] 1.1 針・ノブ・中心ドットへの `setLayerType` + `PorterDuffColorFilter(SRC_IN)` が期待どおり色置換されることをエミュレータ/実機で確認する (→ Requirement: テーマ配色を反映する)
- [x] 1.2 `OnPreDrawListener` による冪等再適用 (shader クリア込み) でちらつき・無限 invalidate が発生しないことを確認する (→ Scenario: 時刻選択の操作後も配色が維持される)
- [x] 1.3 キーボード入力欄の枠 (TextInputLayout boxStroke) がどの state (focused / selected) で駆動されるかを特定する (→ Requirement: テーマ配色を反映する)
- [x] 1.4 スパイク結果が View 走査方式の成立を覆す場合は実装を進めず、ユーザーへエスカレーションする (ADR-0006 の前提崩れ)

## 2. 色解決の接続

- [x] 2.1 ダイアログ用アクセント色を `cell.accentColor ?: effective.accentColor` で接続する (`EffectiveStyle` の既存解決を再利用し、新規 resolver を作らない) (→ Requirement: アクセント色は Cell 固有値を先頭に解決される)
- [x] 2.2 `bind` で受けた実効テーマ色を `showTimePicker` へ渡すようシグネチャを変更する (→ Requirement: テーマ配色を反映する)
- [x] 2.3 アクセント上の文字色 (輝度による白/黒自動選択) のユーティリティを実装する (→ Scenario: アクセント上の文字の可読性)

## 3. TimePickerColorizer 実装

- [x] 3.1 `FragmentLifecycleCallbacks` (onFragmentViewCreated) で着色フックを張り、破棄時に解除する (→ Requirement: テーマ配色を反映する)
- [x] 3.2 window 背景の `MaterialShapeDrawable.fillColor` 差し替え (角丸・elevation 維持、非 MaterialShapeDrawable 時のフォールバック) (→ Scenario: テーマ色の反映)
- [x] 3.3 全走査方式の着色 (ID 重複対応)。CSL 状態キー (チップ/文字盤数字=`state_selected`、AM/PM=`state_checked`) を定数化する (→ Scenario: テーマ色の反映)
- [x] 3.4 `OnPreDrawListener` での冪等再適用 + 文字盤数字の shader クリアで、ViewStub 遅延生成 (モード切替) にも追随する (ADR-0006 どおり pre-draw のみで行い、`setOnHierarchyChangeListener` は単一リスナー置換 API のため使わない) (→ Scenario: 入力モード切替後も配色が維持される / 時刻選択の操作後も配色が維持される)
- [x] 3.5 針・ノブ・中心ドットの ColorFilter 適用 (→ Scenario: テーマ色の反映)
- [x] 3.6 キャレット tint (API 29 `textCursorDrawable`、EntryCellViewHolder の前例流用) と入力欄の文字・枠の着色 (→ Scenario: テーマ色の反映)
- [x] 3.7 内部 R.id 参照への lint `PrivateResource` 抑制と、実装箇所への `ADR-0006` コメント付与 (→ Impact)

## 4. テスト

- [x] 4.1 アクセント色解決順の単体テスト (Cell 固有値優先 / null フォールバック) (→ Scenario: Cell 固有値の優先 / 未指定時のフォールバック)
- [x] 4.2 アクセント上の文字色の黒/白判定 (コントラスト比の高い方) の単体テスト。代表色に加え、判定が切り替わる境界近傍の色も対象にする (→ Scenario: アクセント上の文字の可読性)
- [x] 4.3 Colorizer の色マッピング (テーマ値 → 各部位への適用値) の単体テスト (Robolectric 等、実行可能な範囲で) (→ Scenario: テーマ色の反映)

## 5. 視覚照合 (mock が見た目の正)

- [x] 5.1 サンプルアプリでキーボード / 時計両モードのスクリーンショットを取得する (→ Scenario: テーマ色の反映)
- [x] 5.2 mock/approved.png と照合し、乖離を潰したうえで verification/ に最終スクリーンショットを保存、brief.md に照合結果を記録する (→ 全 Scenario)
- [x] 5.3 実機でモード切替後・時→分遷移後の配色維持を確認する (→ Scenario: 入力モード切替後も配色が維持される / 時刻選択の操作後も配色が維持される)
- [x] 5.4 12時間フォーマット (`format` に "a" を含む) で AM/PM トグルの選択状態と切替前後の配色を両モードで確認する (→ Scenario: 12時間フォーマットでの反映)
