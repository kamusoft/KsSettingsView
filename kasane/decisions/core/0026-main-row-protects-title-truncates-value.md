---
id: 0026
title: 主行の幅配分は title を守り valueText を省略する (両 platform)
status: accepted
date: 2026-08-22
---

## Context

Cell の主行では title と行内 trailing (valueText) が同じ 1 行の幅を分け合う。幅が足りないとき「どちらが省略されるか」は、移植元 AiForms の時点で platform ごとに逆だった:

- iOS (`CellBaseView.cs` / `LabelCellView.cs`): title の compression resistance 999、ValueLabel 100。**valueText が先に省略され、title が残る**
- Android (`cellbaseview.axml` / `LabelCellView.cs`): CellTitle `0dp + weight 1`、ValueLabel `wrap_content`。**title が先に省略され、valueText が残る**

KsSettingsView はそれぞれを忠実に移植したため、同じ非対称を引き継いでいた (Android 側は android/ADR-0002 で構造ごと採用)。一方、概念文書「Cell 共通行のレイアウト」は Android 側の配分を共通契約として記述し「iOS も同じ配分を満たす」としていたが、これは事実と異なっていた。

icon 枠を「譲らない」契約へ直す (fix-cell-icon-size-parity) にあたり、「幅が足りないとき誰が譲るか」を両 platform で同じ答えにする必要が生じ、この非対称が表面化した。

## Decision

主行の幅が足りないとき、**title を守り valueText を省略する**。両 platform 共通:

- title はコンテンツ幅を確保する (主行幅を上限とし、超える分だけ末尾省略)。
- valueText は主行の残り幅を占め、収まらない分は末尾省略する (残り幅が 0 なら表示されない)。
- icon 枠と Cell 級アクセサリの幅は主行より先に譲らない。
- 行内 trailing がない Cell では title が主行の全幅を使える (従来どおり。`ButtonCell` の中央揃えなどが依存する)。
- EntryCell (title がコンテンツ幅、入力フィールドが残り幅) は従来どおりで、既定配分と同じ形になる。

iOS は移植元どおりの現行優先度を維持し、Android の既定配分を title `wrap_content` / valueText `0dp + weight 1` へ入れ替えて iOS 側へ揃える。android/ADR-0002 の決定のうち「既定の配分は原典同型 (title が残り幅・valueText がコンテンツ幅)」の項目は本決定が置き換える。LinearLayout + weight の構造と EntryCell の配分は ADR-0002 のまま維持する。

- title を守る理由: title は開発者が決める短いラベルで、valueText は SSID・メールアドレス・ファイルパスのようなユーザーデータで長くなりやすい。title が潰れて値だけ全文出る行より、ラベルが残って値が省略される行の方が設定画面として読める。iOS 標準の設定アプリ (value style の行) も同じ挙動で、利用者の期待に沿う。

## Alternatives Considered

- **iOS を Android (title が先に省略) へ揃える**: 却下。概念文書と android/ADR-0002 の記述には合うが、短い title が長い valueText に潰されて「W… | eoGW-276ccc8-5」のような行になる。ユーザーデータ側を全文表示するより、ラベルを残す方が設定画面として読みやすい。
- **両 platform とも移植元のまま (意図的な platform 差として記録)**: 却下。同じ Theme・同じ内容で OS ごとに省略される側が違うのは、プラットフォーム間で仕様・動作を統一する製品目的に反する。既存利用者への影響はないが、差異を概念文書に書き続けるコストが残る。

## Consequences

- 正: 両 platform で同じ内容が同じ見え方になり、ラベルが読める状態が保たれる。
- 正: Android の既定配分が EntryCell と同じ形になり、EntryCell 固有の weight 付け替えが不要になる (ADR-0002 の構造は維持したまま単純化できる)。
- 負: Android の既存利用者には、長い valueText を持つ行の見た目が変わる (title が省略されていた行で valueText が省略されるようになる)。
- 負: android/ADR-0002 の一部を core 側から覆すため、android の ADR 一覧に注記が要り、概念文書「Cell 共通行のレイアウト」の主行の幅配分を書き直す必要がある。
- 負: Android で行内 trailing の有無に応じて title の LayoutParams (wrap / weight) を bind 時に切り替える必要がある (行内 trailing がない Cell で title が全幅を使う契約を保つため)。

出典: kasane/changes/fix-cell-icon-size-parity/exploration.md (決定事項) / 移植元 AiForms.Maui.SettingsView の `Native/iOS/Cells/CellBaseView.cs` `LabelCellView.cs` と `Platforms/Android/Resources/layout/cellbaseview.axml` `Native/Android/Cells/LabelCellView.cs` / 探索・提案の会話中の議論 (2026-08-22)
