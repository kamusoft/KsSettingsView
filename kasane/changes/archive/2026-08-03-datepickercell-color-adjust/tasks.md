# Tasks: datepickercell-color-adjust

## 1. スパイク (机上仮説の実機確定)

- [x] 1.1 ヘッダ重なりの原因確定: エミュレータ/実機で現象を再現し、`mtrl_picker_header_title_and_selection` 配下 2 TextView の実測 (ベースライン位置・描画上端) で CJK 大フォント仮説を検証する。棄却された場合は真因を特定して補正手段を見直す (→ Requirement: 日付選択ダイアログのヘッダはタイトルと選択日の両方が読める)
- [x] 1.2 A 案の補正パラメータ確定: 標準ヘッダ高さのまま選択日テキストの縮小 (+必要なら firstBaselineToTopHeight 再設定) で、日本語タイトル + 日本語日付が重ならない値を実測で決める (→ 同上。見た目の正は mock/approved.png)
- [x] 1.3 MaterialDatePicker 内部構造の棚卸し: material 1.12.0 の内部 R.id・View 階層から部位対応表 (ui/brief.md) の各部位への到達経路と、モード切替 (カレンダー/テキスト入力) 時の View 再生成挙動を確認する

## 2. 色ロール導出の再利用整備

- [x] 2.1 `TimePickerColors` / `ColorRoles` の導出規則を DatePicker からも使える形に整理する (共有名へのリネーム等は可。TimePickerCell の挙動・既存テストの意味は変えない — proposal Non-Goals)

## 3. DatePickerColorizer の実装

- [x] 3.1 走査ヘルパ (DatePickerColorizer 相当) の骨格: FragmentLifecycleCallbacks でフックし、部位対応表に従って静的着色を適用する。走査・補正は本ヘルパ 1 クラスに閉じる (android/ADR-0008) (→ Requirement: DatePickerCell の日付選択ダイアログはテーマ配色を反映する / Scenario: テーマ色の反映)
- [x] 3.2 ヘッダ重なり補正: 1.2 で確定したパラメータを同フックで適用する (→ Requirement: ヘッダ / Scenario: 日本語タイトルと日本語日付の同時表示・日付選択後もヘッダが崩れない)
- [x] 3.3 モード切替・カレンダー操作・日付再選択への追随: テキスト入力モードの遅延生成 View、月移動・年選択で再生成される View に加え、日付再選択時に material が `notifyDataSetChanged()` で同じ View を Material 配色へ塗り戻す経路にも再適用が効くことを保証する (TimePicker の静的/動的分離の知見に従い、不要な毎フレーム再適用をしない) (→ Scenario: 入力モード切替後も配色が維持される・カレンダー操作後も配色が維持される・日付を選び直しても配色が維持される)
- [x] 3.4 アクセント上文字の自動選択: 選択日サークル・選択年の上の文字に `onAccent` (実効面とのコントラストによる白黒自動) を適用する (→ Scenario: アクセント上の文字の可読性)
- [x] 3.5 色束の解決と受け渡し: 背景 (`Theme.backgroundColor`)・アクセント (`DatePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`)・通常文字 (実効タイトル文字色) の 3 色を表示時に解決してヘルパへ渡す (TimePicker の resolveDialogColors 相当) (→ Requirement: テーマ配色 / アクセント色の解決)
- [x] 3.6 ViewHolder への組み込み: showMaterialDatePicker() からヘルパを attach する (attach 1 行に留める)

## 4. テスト

- [x] 4.1 色束解決の単体テスト: アクセントの解決順 (Cell 固有値の優先 / CellStyle 値へのフォールバック / Theme 値へのフォールバック) を個別に検証し、背景・通常文字を含む 3 色の解決も検証する (→ Scenario: Cell 固有値の優先・CellStyle 値へのフォールバック・Theme 値へのフォールバック)
- [x] 4.2 色ロール導出の単体テスト: DatePicker 向けに使う導出色 (onAccent 等) が既存 `ColorRoles` 検証でカバーされているか確認し、不足分 (半透明アクセントでの実効面判定を含む) を追加する (→ Scenario: アクセント上の文字の可読性)
- [x] 4.3 Robolectric テスト: ダイアログ表示時に部位対応表の代表部位へテーマ色が適用されること、日付再選択後の再着色 (Material 配色の塗り戻しから復帰すること)、対象外部位 (エラー表示等) が上書きされないこと、ヘッダ 2 TextView が重ならず各自クリップ/省略されないこと (bounds + クリップ判定) (→ Requirement: テーマ配色 / ヘッダ)
- [x] 4.4 内部構造の契約テスト: 走査が依存する material 1.12.0 の必須内部 ID・View 型の存在を Robolectric で検証し、ライブラリ更新時の破綻を自動検出できるようにする (→ android/ADR-0008 の追随確認の自動化)

## 5. 検証

- [x] 5.1 mock との視覚照合: 実機/エミュレータのスクリーンショット (カレンダー表示・テキスト入力・年選択・月移動後・日付再選択後) を ui/verification/ に保存し、mock/approved.png と部位対応表で照合する (→ 全 Requirement)
- [x] 5.2 lint: 内部 R.id 参照の `PrivateResource` 抑制がヘルパ 1 クラスに限定されていることを確認する
