# handbook 地図

規範層。作業するときに従う決まりと手順を置く。コードがこの層に従う側であり、食い違いを見つけたら直すのはコードの側になる (今どうなっているかの記述は [concepts](../concepts/index.md))。

文書はドメイン別に分割する ([cross/ADR-0015](../decisions/cross/0015-domain-axis-core-plus-platforms.md))。作業ドメイン + cross の index を開き、`always` の文書と担当範囲 (触るファイル・行う作業) に当たる文書だけを読む。

| ドメイン | 内容 |
|---|---|
| [cross](cross/index.md) | リポジトリ横断の規約 (ソースコメント・テスト実行・実行時挙動の検証・Sample 一致・公開識別子・利用者向け Skill・開発環境) |
| [maui](maui/index.md) | .NET MAUI 系統の規約 (検証ホストの実行・描画性能の計測構成) |

core / ios / android のドメイン規約は現時点で存在しない。最初の文書を書くときに `<domain>/index.md` ごと作る。
