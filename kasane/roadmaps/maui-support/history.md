# maui-support 構造変更履歴

## 2026-08-05: phase-6-accessory-views を追加 (実行順は phase-4 と phase-5 の間)

ksn-explore での議論により、ロードマップに Header / Footer 系の要件が不足していることが判明したため、フェーズを追加した。

- **RootHeader / RootFooter**: 原典 AiForms.Maui.SettingsView に存在しない本ライブラリ固有の概念で、MAUI API へのマッピング要件がどのフェーズにも無かった。text 系は既存 Bridge の `updateAccessory` で輸送可能なため、SettingsView (BindableObject) の API 形状論点として phase-2-maui-core の agenda に追記した
- **Section / Root の Header・Footer への任意 MauiView 設定**: phase-1 の Bridge は accessory を text と clear に限定しており (concepts/maui/api/native-bridge.md)、任意 View の輸送と MauiView→native 実体化は未着手の1変更級の塊。新フェーズ phase-6-accessory-views として切り出した
- 実行順を phase-4 → phase-6 → phase-5 とした理由: MauiView→native 実体化機構は phase-5 (custom-cell) と共有されるが、accessory は再バインド制御・等価性契約と無関係なため、最小の問題で機構を先に建てて phase-5 が再利用する方が phase-5 の膨張リスクを避けられる
- フェーズ番号は末尾採番 (振り直し禁止)。実行順はフェーズ一覧の行順と mermaid で表現

## 2026-08-07: 強化フェーズ4件の追加 (phase-7〜10)

phase-2 の論点「BindableObject 階層の踏襲範囲」の議論で、AiForms 互換として提供しない機能のうち D&D 並べ替え・スクロール制御・Header/Footer 表示トグル・DataTemplate 仮想化は「切る」ではなく Native から再設計して提供する方針となった (maui/ADR-0008)。「1フェーズ = 1 change」の粒度判定により1フェーズに束ねず4フェーズに分割して末尾採番で追加 (オーナー確認済み): phase-7-drag-sort / phase-8-scroll-control / phase-9-accessory-visibility / phase-10-template-virtualization。いずれも pending・change 種別で、実行順は phase-5 までの完了後に順不同。ゴールに強化フェーズの1行を追記。

## 2026-08-20: phase-11-modern-style を追加

`KsSettingsViewStyle.Modern` 完全実装の探索 (kasane/changes/implement-modern-style、ロードマップ外の L 級 change) で、MAUI 伝搬のスコープ振り分けを議論した結果によるフェーズ追加。MAUI には style 公開 API 自体が存在せず、BindableProperty 新設 + Theme 4属性 (sectionMargin / sectionCornerRadius / sectionBorderWidth / sectionBorderColor) の写し + Bridge 伝搬 + サンプルで1 change 級の塊のため、Native change に含めず末尾採番で phase-11 とした (全層一括の cross change は phase-9 の前例より規模が大きくリスク過大として却下)。実行順は他の強化フェーズ同様 phase-5 完了後に順不同だが、Native 側 Modern 実装の完了が前提 (roadmap.md の前提に追記)。
