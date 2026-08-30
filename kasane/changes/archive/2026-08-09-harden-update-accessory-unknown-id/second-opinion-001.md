# セカンドオピニオン: harden-update-accessory-unknown-id (001 回目)
**相方**: codex / **日付**: 2026-08-09 / **対象**: 提案一式 (proposal.md + specs/ios-store + specs/android-store + specs/maui-bridge + tasks.md)
---
- **Major** — [proposal.md:13](kasane/changes/harden-update-accessory-unknown-id/proposal.md:13)、[proposal.md:28](kasane/changes/harden-update-accessory-unknown-id/proposal.md:28) — `ios-store` / `android-store` / `maui-bridge` の3 capability を変更すると明記しながら M 級としている。Kasane の「複数能力横断は L」という基準と矛盾し、L で必須の `design.md` が欠落している。単一の core capability の投影として M とするなら、その扱いと MAUI 契約変更が別 capability ではない理由を明記する。それができなければ L へ変更し `design.md` を追加する。

- **Major** — [ios-store/spec.md:16](kasane/changes/harden-update-accessory-unknown-id/specs/ios-store/spec.md:16)、[android-store/spec.md:16](kasane/changes/harden-update-accessory-unknown-id/specs/android-store/spec.md:16)、[maui-bridge/spec.md:31](kasane/changes/harden-update-accessory-unknown-id/specs/maui-bridge/spec.md:31) — 成功経路は section header のみ、Root 系も root header のみ検証される。実装が「section footer を常に no-op」にしたり、root footer の発行を壊したりしても全 Scenario を満たせる。section header/footer × 既知/未知、および root header/footer をパラメータ化して両方固定する。MAUI Bridge でも未知 section footer を含める。

- **Major** — [ios-store/spec.md:7](kasane/changes/harden-update-accessory-unknown-id/specs/ios-store/spec.md:7)、[android-store/spec.md:7](kasane/changes/harden-update-accessory-unknown-id/specs/android-store/spec.md:7)、[maui-bridge/spec.md:7](kasane/changes/harden-update-accessory-unknown-id/specs/maui-bridge/spec.md:7) — 「状態を変更しない」の観測基準が値の不変だけなのか、状態ストリームも無通知なのか未確定。iOS の `root` は `@Published` なので、同値を再代入して Diff だけ抑止する実装は現在値比較を通る一方、余分な状態通知を発生させる。MAUI Requirement は「通知にも影響しない」とするが Scenario は通知を判定していない。no-op を「root/state 通知も Diff もゼロ」と明記して各ストリームを検証するか、通知が Diff のみを指すならその範囲を明記する。

- **Major** — [proposal.md:26](kasane/changes/harden-update-accessory-unknown-id/proposal.md:26)、[native-bridge.md:47](kasane/concepts/maui/api/native-bridge.md:47) — concepts 追随先として core の1文書しか挙げていないが、MAUI concept は現在「`updateAccessory` は未知 ID no-op の対象外」と変更後契約の正反対を明記している。このまま申し送りどおり蒸留すると矛盾が残る。`maui/api/native-bridge.md` を必須追随先へ追加し、Store の共通保証を置く `core/architecture/store-and-update-streams.md` も更新要否を明示する。

- **Minor** — [android-store/spec.md:31](kasane/changes/harden-update-accessory-unknown-id/specs/android-store/spec.md:31) — 「後続の既知 ID 操作」が曖昧で、どの通知経路を使うか決まっていない。Diff 購読の生存確認が目的なら、同じ `store.diffs` 経路を通る `replaceCell` または既知 section の `updateAccessory` を具体的に指定し、表示結果まで固定する。

- **Minor** — [maui-bridge/spec.md:7](kasane/changes/harden-update-accessory-unknown-id/specs/maui-bridge/spec.md:7) — 「Root 系 target は ID を取らない」は現行 API と不一致。Bridge の `updateAccessory` は Root target でも `sectionID` 引数を持ち、単に参照しない。受け入れ基準を「Root target では sectionID を参照せず、未知 ID 判定の対象外」と修正する。

- **Minor** — [tasks.md:5](kasane/changes/harden-update-accessory-unknown-id/tasks.md:5)、[tasks.md:11](kasane/changes/harden-update-accessory-unknown-id/tasks.md:11) — コードコメントへ `ADR-0020` と記す指示は、ドメイン分割後の正式参照形式と一致しない。両方とも `core/ADR-0020` に修正する。

総評: Major の仕様・検証穴が残っており、このまま実装へ進むべきではありません。特に target 全組合せの回帰保証、no-op の通知意味論、MAUI concept の矛盾を先に解消する必要があります。

---

## 突き合わせ結果

ホスト側自己レビュー (2周・指摘なしで通過) との突き合わせ。全7件が「相方のみ + 根拠強」で採用。

| # | 重要度 | 指摘 | 採否 | 対応 |
|---|---|---|---|---|
| 1 | Major | 3 capability で M 級は「複数能力横断は L」と矛盾 | 採用 (明記対応) | 実体は単一契約 (core/ADR-0020) の platform 投影であり、platform 対称の複数 spec 構成で M とした前例 (fix-dsl-header-height-diff) に従い M を維持。判定理由を proposal の級セクションに明記 |
| 2 | Major | 成功経路が section header のみ・root footer 未検証 (footer 常時 no-op 化の実装でも全 Scenario を満たせる) | 採用 | 既知 sectionID / Root 系の Scenario を header・footer 両方の検証に拡張 (ios-store / android-store / maui-bridge) |
| 3 | Major | no-op の観測基準 (状態ストリーム無通知を含むか) が未確定 | 採用 | 「現在状態の値が不変」かつ「状態・Diff 両ストリーム無発行」を Requirement に明記し、no-op Scenario の THEN を両ストリーム検証に強化 |
| 4 | Major | concepts 追随先の漏れ — maui/api/native-bridge.md が変更後契約の正反対を明記 | 採用 | proposal の申し送りに maui/api/native-bridge.md (必須) と core/architecture/store-and-update-streams.md (更新要否確認) を追加 |
| 5 | Minor | Android「後続の既知 ID 操作」が曖昧 | 採用 | 後続操作を「既知 cellId の `replaceCell`」に具体化し、表示反映まで固定 |
| 6 | Minor | 「Root 系 target は ID を取らない」は Bridge の実 API (sectionID 引数あり・参照しない) と不一致 | 採用 | 「`sectionID` 引数を参照しないため未知 ID 判定の対象外」に修正 |
| 7 | Minor | ADR コメント参照が正式形 (core/ADR-0020) でない | 採用 | tasks.md の2箇所を `core/ADR-0020` に修正 |

降格・未解決: なし。
