---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-29
last-seen: 2026-08-29
evidence:
  - skills-api-coverage (concepts へ Theme default 定数 18 個・MAUI スタイルプロパティ約 40 個を追記する際、スラッシュ連結の羅列 (箇条書き 1〜3 行に詰め込み) で書いたところ、オーナーが「列挙系は表にしないと読めたもんじゃない」と指摘し表形式へ書き直しになった)
---

## ルール文 (候補)

concepts / ドキュメントへ API 名・定数名・プロパティ名を **おおむね 5 件を超えて列挙**するときは、スラッシュや読点で連結した行内リストにせず、表 (名前 + 役割/分類の列) にする。既存文書の周辺に同種の列挙表 (例: Store 公開操作の表) があるならその形式に合わせる。行内連結が許されるのは、同一系列の機械的なバリエーション (例: `FontFamily` / `FontSize` / `FontAttributes` の 3 連) を表のセル内で束ねる場合まで。

## 経緯

- 2026-08-29 skills-api-coverage: 公開 API 網羅性対応で concepts 3 ファイル (ios/android native-host・maui-facade) に定数・プロパティ名を追記した際、コンテキスト節約を優先してスラッシュ連結の羅列で書いた。オーナー指摘で全て表形式 (名前 + 用途/分類) へ書き直し。同じ文書内に既に「対象 | 操作」形式の表があり、最初からそれに合わせるべきだった。
