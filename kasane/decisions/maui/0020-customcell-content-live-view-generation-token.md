---
id: 0020
title: MAUI CustomCell の content は live view + 世代トークンで輸送し、埋め込み view の差し替えはトークン変更でのみ起きる
status: accepted
date: 2026-08-12
---

## Context

現行コア契約の CustomCell ([custom-cell.md](../../concepts/core/cells/custom-cell.md)) は「content 値 + builder クロージャ」で表現され、再バインド (builder 再実行) の要否は content を含む値等価で決まる (core/ADR-0014)。等価性に参加しない包みを content に置くことは禁止されている (`KsAnyView` を Cell 本体の代替に使わない — 再バインドが暴発するため)。

一方 MAUI では View は data binding で生きたまま内容が変わるのが慣例で、「値が変わったらクロージャを再実行して View を作り直す」という native の再バインド概念が直訳できない。

MauiView を native の platform view へ実体化する共有基盤は accessory View で確立済みである ([view-materialization.md](../../concepts/maui/architecture/view-materialization.md)、[ADR-0016](0016-mauiview-materialization-self-measuring-wrapper.md)〜[ADR-0018](0018-accessory-view-update-semantics.md)): 自己計測 wrapper・native view インスタンスの直接輸送・「参照が正・内容は live」の更新セマンティクス。

## Decision

- MAUI の CustomCell は **MauiView インスタンスを保持**し、内容の変化は binding による live 更新とする。内容変化で native への再発行はしない ([ADR-0018](0018-accessory-view-update-semantics.md) と同じ規律)
- native へは実体化機構で生成した **wrapper platform view を輸送**し、native CustomCell の builder は `UIViewRepresentable` / `AndroidView` interop でそれを埋め込む**定数返しクロージャ**とする ([ADR-0017](0017-accessory-view-instance-transport.md) と同型)
- native CustomCell の content には **facade (controller) が振る一意の世代トークン** (参照遷移が起きるたびに必ず変わる単調増加値) を格納する。トークンは値等価へ正しく参加する
- この設計が保証するのは**埋め込み platform view インスタンスの安定性**である: native の再バインド (builder 再実行) 自体は現行 native 契約のまま style / showArrow / isEnabled / isVisible の変更や replaceCells 経路の再配信でも発火するが、同一トークンの間は定数返しクロージャが同一インスタンスを返し、view の破棄・再 materialize・Handler 切断は起きない。view の差し替え (別インスタンスへの置換) はトークン変更時のみ
- content のサイズ変化は wrapper 自身の計測無効化だけで両 OS の行高さが追従する (実装フェーズ冒頭の probe で負の対照付きに実測確定)。accessory (ADR-0018) と異なり、native への一過性再計測通知の追加は不要

## Alternatives Considered

- **`MauiView.ToPlatform()` を `KsAnyView` 経由で content に格納**: KsAnyView は意図的に等価性へ参加しないため、ツリー再構築のたびに「変更あり」と判定され再バインドが暴発する。core 契約の禁止事項そのもの。却下
- **content 値 + DataTemplate の再実体化で native に忠実な再バインドを再現**: content 変更のたびに template 再インフレート = View の作り直しと退役が走り、binding で live に更新する MAUI 慣例に反して重い。template 前提の設計は DataTemplate 仮想化を提供する際にセットで再考する。現時点では不採用

## Consequences

- 正: accessory View で実証済みの実体化機構・寿命規律 (論理所有と platform lease の分離・退役順序) をそのまま再利用でき、実装と検証のコストが小さい
- 正: DataTemplate は生成機構として自然に共存する — template から生成された各 CustomCell は別の View インスタンスを持ち、それぞれ独立にこの経路へ乗る (Handler 1:1 と整合)
- 負: 行数分の live View が常存し、行ビューの使い回しに View を載せ替える仮想化は効かない。大量行はテンプレート仮想化の将来提供に委ねる
- 負: MAUI 側には native の「content 値」に相当する公開概念がなく (トークンは内部の合成値)、「見た目を左右する値は content に含める」という native 契約が MAUI では「binding で View に届ける」に置き換わる。プラットフォーム間で契約の見え方が非対称になるため、ドキュメントで明示する必要がある
- 負 (出典: 実装結果): interop 埋め込みの継ぎ目は platform 固有の吸収層を要した — iOS は SwiftUI の遅延した後片付けが共有 view を表示中の行から剥がすため行ごとの入れ物 + 引き取り規則が必要、Android は Compose の `AndroidView` interop が埋め込み領域のタッチを行 click から遮るため継ぎ目での行タップ返還が必要。詳細は [view-materialization.md](../../concepts/maui/architecture/view-materialization.md)
- 正 (出典: 実装結果): 行リサイクル・Handler 再接続・構造的除去を含む寿命規律が accessory の機構の拡張で成立し、facade の公開面に content 用の新概念を追加せずに済んだ

---
出典: kasane/roadmaps/maui-support/phases/phase-5-custom-cell/history.md (2026-08-12: content 等価性による再バインド制御の MAUI 実現) / kasane/changes/archive/2026-08-12-add-maui-custom-cell/second-opinion-spec-001.md (Critical 1: 再バインド契約の書き換え) / 同 design.md Decision 2・3・5 / 同 deviation.md (iOS 埋め込み形)
