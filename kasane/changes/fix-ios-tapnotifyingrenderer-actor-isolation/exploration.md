# Exploration: fix-ios-tapnotifyingrenderer-actor-isolation (簡易起票スタブ)

起票日: 2026-08-28 / 起票元: restore-pickercell-object-items の実装中 (iOS core モデル実装ワーカーが既存警告として発見、オーナー指示で起票)

## 課題 / 動機

iOS のビルドで Swift concurrency の警告が出ている: 行タップ通知プロトコル (`TapNotifyingRenderer`) への `PickerCellView` の準拠が main actor 分離コードをまたぐ、という内容 (`conformance of 'PickerCellView' to protocol 'TapNotifyingRenderer' crosses into main actor-isolated code`)。

- 該当: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` のプロトコル定義 (2450 行付近) と各 CellView の準拠 extension 群 (2455 行以降。警告は PickerCellView で観測されているが、同形の準拠が Button / Checkbox / Radio / SimpleCheck / NumberPicker / TimePicker / DatePicker / Entry / Custom の各 CellView にも並ぶ)
- 警告文が明示するとおり、**Swift 6 言語モードではエラーになる**。現在は警告のまま動作しており、restore-pickercell-object-items 以前から存在する (本 change 起因ではない)
- toolchain / 言語モード更新時に必須対応となるため、先に潰しておきたい

## 検討した選択肢 (却下案と理由を含む)

(未検討)

## 決定事項

(なし)

## ADR 候補

(なし)

## 未決の論点

- **未探索 (簡易起票)**
- プロトコル自体を `@MainActor` 分離にするか (準拠側は全 CellView = UIView 系で main actor 上のはずなので、定義側の分離宣言で解消する見込み)、他の解消手段 (isolated conformance 等) が適切か
- 同型の警告が他の internal プロトコル準拠にも潜んでいないかの横断確認 (Swift 6 言語モードでのビルド試行で洗い出せるか)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定

(定義への分離宣言だけで閉じるなら S 見込み。横断確認で件数が多ければ M)
