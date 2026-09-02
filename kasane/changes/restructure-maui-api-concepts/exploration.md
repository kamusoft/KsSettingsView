# Exploration: restructure-maui-api-concepts

## 課題 / 動機

add-maui-nuget-distribution の蒸留 (2026-09-02) で、`kasane/concepts/maui/api/` の 2 概念が分割検討トリガー (散文 10,000 字、ksn-core references/concepts.md) を超えていることを確認し、オーナー判断で別作業として起票した。

| 概念 | 散文 | 構造 lint 違反 | 状態 |
|---|---|---|---|
| `maui/api/maui-facade.md` | 16,101 字 (見出し 14) | 30 件 (200 字超の項目・「公開 API の形」配下 11 項目) | いずれも 2026-09-02 の追記より前からの債務。今回の追記 (「導入と前提」節) は違反を増やしていない |
| `maui/api/native-bridge.md` | 10,684 字 (見出し 11) | 19 件 | 同上 |

`index.md` の 1 行説明が「導入と前提・公開 API・CustomCell・双方向バインド・更新の意味論・lifecycle・配置制約」と主題を並べないと書けない状態で、他概念から特定の節 (PickerCell の object API・導入と前提・配置制約) を指してリンクしたくなる — 「1 概念 = 1 ファイル」の粒度基準 (参照される単位) に照らして複数の概念を抱えている可能性が高い。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

- 蒸留のたびの小差分では累積の劣化が見えないため、ksn-concept (モード3: 作庭) の独立作業として扱う (2026-09-02 オーナー合意)

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- 先に節の構造を整える (h3 へ割る・対の並びを表へ移す) — 整えた後で主題の境界を見てから分割の要否を決める (ksn-core: 構造を整えるのが先、分割の判断はその後)
- maui-facade の分割案の候補: 「導入と前提 + 公開 API の形」/「PickerCell の object API」/「更新の意味論 + lifecycle + 配置制約」など。割ると相互参照だらけになるなら分割せず h3 と表で保つ判断もある
- native-bridge は主題が 1 つ (interop 境界) で詳細が多いだけの可能性が高い — 構造整備のみで収まるかを先に見る
- 分割した場合の概念間リンクの張り直し (concepts 内・handbook・ADR の `関連` 行・lessons) と `index.md` / `log.md` の更新を一続きで行う
- 利用者向け skills (`skills/{en,ja}/kssettingsview-maui`) は concepts の派生物のため、分割後に docs-refresh の明示依頼が要るかを確認する

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 (概念文書の再構成のみ・コード変更なし。ksn-concept の作庭として実施)
