# Exploration: restructure-maui-api-concepts

## 課題 / 動機

add-maui-nuget-distribution の蒸留 (2026-09-02) で、`kasane/concepts/maui/api/` の 2 概念が分割検討トリガー (散文 10,000 字、ksn-core references/concepts.md) を超えていることを確認し、オーナー判断で別作業として起票した。2026-09-05 の探索で実測を取り直し、隣接の `maui/architecture/view-materialization.md` も同じトリガーを越えていることを確認して同梱した。

| 概念 | 散文 (起票時 → 2026-09-05) | 構造 lint 違反 | 見出し |
|---|---|---|---|
| `maui/api/maui-facade.md` | 16,101 → 16,451 字 | 30 件 (200 字超の項目・「公開 API の形」配下 11 項目) | 14 |
| `maui/api/native-bridge.md` | 10,684 → 10,645 字 | 19 件 | 11 |
| `maui/architecture/view-materialization.md` | 10,742 字 | 14 件 | 10 |

`index.md` の 1 行説明が「導入と前提・公開 API・CustomCell・双方向バインド・更新の意味論・lifecycle・配置制約」と主題を並べないと書けず、他概念から特定の節 (導入と前提・binding 構成の要点) を名指しでリンクしている — 「1 概念 = 1 ファイル」の粒度基準 (参照される単位) に照らして maui-facade は複数の概念を抱えている。

傍証: 利用者向け派生物 `skills/{en,ja}/kssettingsview-maui` は maui-facade 1 本を cells / custom-cells / styling / updates の 4 references に翻訳して参照している (`skills/.manifest.json`)。派生物が「参照される単位」を既に 4 つに割っている。

maui-facade の節ごとの散文量 (表・コードを除く): 導入と前提 1,675 / 公開 API の形 5,757 (うち PickerCell の object API 約 2,500) / CustomCell 1,748 / ユーザー操作と双方向バインド 1,104 / 更新の意味論 1,947 / lifecycle 914 / 配置の制約 798 / その他 約 2,800。

## 検討した選択肢 (却下案と理由を含む)

### 論点 1: スコープ (view-materialization の同梱)

- 採用: 同梱する — 同じ maui ドメイン・同じトリガー・同じ作庭作業で、リンク張り直しと index / log 更新を一度で済ませる。facade の CustomCell 節と内容が接するため境界を同時に見る
- 却下: 別途の軽作業として簡易起票 — 主題が 1 つで構造整備だけで収まる見込みではあるが、分けるメリットが薄い

### 論点 2: maui-facade の分け方

- 採用 (案 A): 4 本 — 入口 / Cell の MAUI 表現 (CustomCell 含む) / スタイル / 表示への反映と寿命
- 却下 (案 B): 5 本 (A + CustomCell 独立、skills の references と完全同型) — CustomCell 1,748 字は「Cell の MAUI 表現」の一部と見るのが自然で、割りすぎると読者が 2 本またぐ。リンク張り直しも 1 本分増える
- 却下 (案 C): 2〜3 本 (更新の意味論・lifecycle・配置制約だけ切り出す) — 残る本体が約 13,000 字で、表化しても 10,000 字を割れない試算
- 却下 (案 D): 分割せず h3 と表で保つ — 粒度基準 (1 行で言えない・節を名指ししたくなる) に当たっており規約上採りにくい

### 論点 3: native-bridge の扱い

- 採用 (案 A): 構造整備 + 「binding 構成の要点」節を `maui/architecture/binding-build-integration.md` へ統合 — 主題は interop 境界 1 つ。「公開 API の形」3,846 字を 4 小節 (更新 API / Cell DTO / 双方向値の輸送 / Theme・style・Section 装飾の DTO) と表へ。binding 構成の正本は既に binding-build-integration.md にあり、native-bridge 側にしかない情報は Android の Exec 対象 3 module (compose は束縛しない) と aar 再生成の判定入力 (catalog・wrapper・gradle.properties) の 2 点だけ。検証ホストの位置づけは handbook の integration-host-verification が持つ
- 却下 (案 B): 構造整備のみ — build 構成の二重記述が残り、散文も 10,000 前後の境界に残る
- 却下 (案 C): interop 契約と lifecycle・操作通知で 2 分割 — 1 主題を割るので相互参照だらけになる

### 論点 4: 後始末

- ADR (cross/0023・maui/0013・0016・android/0016)、lessons、roadmaps の履歴的言及は触らない (`maui-facade.md` / `native-bridge.md` は入口として残りパスが変わらない)
- 張り直す先: maui の index、native-bridge・view-materialization・binding-build-integration・repository-boundaries の関連節、android の build-toolchain が binding 構成節を名指しする 1 箇所、handbook の integration-host-verification、新 4 本と入口の相互リンク。log.md に記録
- timestamp は内容を検証しない再構成では更新しない。分割は大規模改訂のため初見可読性レビュー (ksn-reviewer) を必須とする

## 決定事項

- 蒸留のたびの小差分では累積の劣化が見えないため、ksn-concept (モード3: 作庭) の独立作業として扱う (2026-09-02 オーナー合意)
- 2026-09-05 探索:
  - 論点 1: view-materialization.md を同梱する (構造整備のみ・分割なし。3,583 字の「native への埋め込みの継ぎ目」節を h3 へ割る)
  - 論点 2: maui-facade を 4 本に分ける (案 A)。すべて既存カテゴリ `maui/api/` 内で rules.md は触らない

    | ファイル (仮名) | 1 行で言うと | 中身 (現在の節) | 散文見込み |
    |---|---|---|---|
    | `maui-facade.md` (残す・入口) | XAML / C# から使う facade の入口と公開面の骨格 | 目的・導入と前提・型名衝突・Root / Section / Cell 階層・header / footer・ItemsSource / ItemTemplate・してはいけないこと・現時点の範囲 | 約 6,000 |
    | `maui-cells.md` | core の Cell 意味論を MAUI の型でどう表すか | 値の型 (MAUI 慣例型)・PickerCell の object API・ユーザー操作と双方向バインド・CustomCell | 約 6,500 |
    | `maui-styling.md` | Theme / CellStyle / ListStyle の MAUI 表現 | スタイル項目・ListStyle・Section 装飾 4 属性・プロパティ一覧 2 表 | 約 1,500 + 表 |
    | `maui-rendering-lifecycle.md` | 変更がいつどう表示へ届き、Host の寿命をまたいで何が保たれるか | 更新の意味論・lifecycle の保証・配置の制約 (Android measure) | 約 3,700 |

  - 論点 3: native-bridge は構造整備 + binding 構成節の binding-build-integration.md への統合 (案 A)。native-bridge には生成構成の所在を示す 1 行だけ残す
  - 論点 5: S 級。ksn-concept モード 3 (作庭) で実施する。着手前にローカル `develop` の差分を作業 branch へ取り込む (オーナー指示)

## ADR 候補 (作成済み: なし / 未起票: なし)

覆すコストの高い判断がなく、分割粒度は index と log で追える。

## 実施結果 (2026-09-05)

ksn-concept モード 3 で実施済み。概念 6 本 (入口 + 新規 3 + 構造整備 2) の散文はすべて 10,000 字前後以下・構造 lint 違反 0、元 3 ファイルの全文の残存を文単位で機械照合、concepts / handbook のリンク切れ 0、初見可読性レビュー (CHANGES_REQUESTED → 反映済み)。詳細は `kasane/concepts/log.md` の 2026-09-05 `gardened:` エントリ。

## 未決の論点

- 派生物 `skills/{en,ja}/kssettingsview-maui` は `skills/.manifest.json` のハッシュが変わり新 4 本の source 対応も要るため、作庭完了後に docs-refresh の明示依頼が別途必要 (自動発動禁止)。作庭の完了報告で改めて伺う
- 新ファイルの仮名 (`maui-cells` / `maui-styling` / `maui-rendering-lifecycle`) は作庭時に本文の title と揃えて確定する

## UI 素材 (ui/references/ の一覧と注釈)

なし (UI に触れない)

## 変更級の推奨: S (理由: コード変更なし・公開 API 変更なし・git で可逆・UI なし。規模はあるが ksn-concept 自身が構造 lint と初見可読性レビューのゲートを持つため proposal は不要。2026-09-05 オーナー確定)
