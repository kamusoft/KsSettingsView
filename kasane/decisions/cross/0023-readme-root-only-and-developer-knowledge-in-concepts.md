---
id: 0023
title: README はルート 2 枚 (英語 + `README_ja`) に集約し、開発者向け知識は concepts に一本化する
status: accepted
date: 2026-08-29
---

## Context

public 化に向けて README を英語化する (英語 `README.md` + 日本語 `README_ja.md` の二本立て) にあたり、二本立ての対象範囲を決めるため README 群の読者を実物で確認した。

- **エージェントは platform README を参照していない**。`AGENTS.md` がエージェントに指示する知識参照先は `kasane/concepts/` とコード・テストのみで、`android/README.md`・`maui/README.md`・`samples/{ios,android,maui}/README.md` は挙げられていない。これらを名指ししているのは、ルート README からのリンク 3 本と、docs-refresh が**更新対象として**持つ一覧だけだった。
- **オーナーも読んでいない** (2026-08-29 の申告)。
- **環境まわりは契約と手順で分かれている**。SDK の解決方法・build root 境界といった契約は concepts (`android/architecture/build-toolchain.md`・`cross/architecture/repository-boundaries.md`) にあり、`ANDROID_HOME` の設定方法・IDE での開き方・トラブルシューティングといった手順だけが README にある。
- **知識の正が README に滞留している箇所があった**。`kasane/concepts/maui/api/native-bridge.md` が binding 構成について「構成の理由と手順は `maui/README.md` が正」と書いており、cross/ADR-0014 から ADR-0022 へ継承した「知識の正は concepts とコード・テスト」の原則が破れている。
- **維持コストは非対称**。docs-refresh の追随対象は README 8 枚で、二本立てにすると最大 14 枚へ増える。

cross/ADR-0022 は**利用者向け**ドキュメント (`skills/`) の提供形態を定めたもので、開発者向け情報の置き場は射程外だった (README 群は「docs-refresh の追随対象」として登場するのみ)。読者が異なるため、本 ADR で別途定める。

## Decision

- リポジトリの README は**ルートの 2 枚のみ**とする: 英語 `README.md` + 日本語 `README_ja.md`。`android/README.md`、`maui/README.md`、`samples/ios/README.md`、`samples/android/README.md`、`samples/maui/README.md` の 5 枚は廃止する。`skills/README.md` / `skills/README_ja.md` (Skill 索引、cross/ADR-0022) はこの決定の対象外で存置する。`maui/spike/README.md` も対象外とする (2026-08-29 追記): 完了済み検証 (binding toolchain の疎通) の記録であり docs-refresh の追従対象にも入っていない。**(2026-08-30 決着)** `maui/spike/` は**公開リポジトリに載せない** — 旧 private リポジトリへ保全し、公開ツリーからも今後の作業ツリーからも外す。長命の知識は既に蒸留済みで (iOS の手法は maui/architecture/binding-build-integration.md、Android は maui/ADR-0006 が spike の `AndroidGradleProject` 方式を却下)、spike README の BG8401 に関する結論は本番の Metadata.xml と逆のまま追従の仕組みを持たず、`maui/*.slnx` 外でビルドもされないため腐り続けるため。したがって公開リポジトリの README は 4 枚 (ルート 2・`skills/` 索引 2) になる。
- 開発者向けの知識は**契約・手順とも** `kasane/concepts/` に一本化する。concepts から README を知識の正として指す参照 (`maui/api/native-bridge.md` の 2 箇所) は解消する。
- **ルート README は利用者の入口に純化する**: 概要・特徴・スクリーンショット・対応 platform・インストール・最小コード例・`skills/` への導線・リポジトリ構成・貢献・ライセンス・サンプルのサードパーティ通知。開発者向けの手順は載せない。
- 廃止する README の中身は分類ごとに次のとおり扱う。
  - **他所に既にあるもの** (モジュール構成・利用アプリ側の前提・基本のビルド / テストコマンド・ディレクトリ構成・`SDK location not found` の対処) は捨てる。
  - **契約で README にしかないもの** (MAUI binding が SDK 内部ターゲットへ割り込む一覧・`XcodeProject` 採否の実験的経緯・`BG8605` / `BG8A00` 警告の意味・共有 scheme を消すと壊れる理由・`KsBridgeFont` の platform 差) は `kasane/concepts/maui/` へ移す。
  - **手順で README にしかないもの** (`ANDROID_HOME` と 2 つの `local.properties`・`DEVELOPER_DIR` 指定・検証ホストの起動コマンドと期待表示・サンプルの実行手順とデモ画面一覧・本体へのステップイン手順・実機目視確認チェックリスト) は `kasane/concepts/` へ移す — 環境セットアップと目視確認は `kasane/handbook/cross/` (既存の `test-execution.md`・`runtime-behavior-verification.md` と同じ扱い)、検証ホストの起動と期待表示は `maui/` 配下。
  - **サンプルのサードパーティ通知** (Material Symbols / Apache 2.0) はルート README へ移す。サンプルアプリで使用しているアイコン由来である旨を明記し、ライブラリ本体の依存と読まれないようにする。
- docs-refresh の追随対象は `skills/README.md`・`skills/README_ja.md`・ルート 2 枚の計 4 枚とし、デモ画面一覧と `SampleScreen` 定義の照合検査は対象消滅により廃止する。この対象定義の変更は ADR-0022 に従い変更フローの承認を通す (docs-refresh 自身には委ねない)。
- ルート README 2 枚は**翻訳ロックステップ**で扱う (片方だけを更新してコミットしない。docs-refresh は 2 枚を 1 回で更新する。執筆順序は問わない)。cross/ADR-0022 が `skills/` の en/ja に課した規律と同一にし、docs-refresh に 2 つのモードを持たせない。
- 以後 platform 別 README を新設するには本 ADR の改訂を要する。

## Alternatives Considered

- **`samples/*/README.md` だけ残す**: 移送は MAUI binding 知識の 1 件で済み、サンプルを動かしたい人の入口が現地に残る。しかし追随対象が 6 枚残って英語化コストの上限が下がらず、サンプル README の読者が実在するかも確認できていない。却下。
- **ルート README を開発者向けも含む総合案内にする**: 「開発者向け」節を設けて手順を集約すれば、clone した人が 1 枚で完結する。しかし公開直後の顔に SDK パス設定やエミュレータ起動コマンドが並び、利用者の入口としての性格が薄れる。ADR-0022 が利用者向けを `skills/` へ切り出した線とも合わない。却下。
- **手順は捨てて契約だけ移す**: 移送作業は最小で済む。しかし検証ホストの起動手順と期待表示のように、どこにも記録のない知識が失われる。却下。
- **現状維持 (英語化の対象範囲だけ決める)**: 移送作業が不要で、public 化後の contributor には最も親切。しかし知識の正が README に滞留する逆転が残り、docs-refresh の追随枚数も減らない。却下。

## Consequences

- 正: 開発者向け知識の正が concepts に一本化され、concepts → README の逆参照が解消する。
- 正: docs-refresh の追随対象が 8 枚 → 4 枚になり、英語二本立てのコストが上限 4 枚に収まる。デモ画面一覧の照合検査も不要になる。
- 負: public 化後の contributor が platform 別のビルド手順を探す入口が、ルート README 一本になる。
- 負: contributor が開発手順に辿り着くには `AGENTS.md` → concepts の 2 段になる (ルート README には載らない)。
- 負: concepts は契約を書く場所であって手順書ではないという性格が薄まる。`kasane/handbook/cross/` に既にある開発規約 (`test-execution.md`・`runtime-behavior-verification.md`) と同じ扱いに揃えることで整合を取る。
- 負 (実装で判明): 「他所に既にあるもの」として破棄すると定めた項目のうち、**ビルド / lint コマンドは破棄できなかった**。破棄の根拠が「ルート README に既出」であり、その記述を本 ADR 自身の適用 (ルート README の利用者向けへの純化) が削除するという循環になっていた。実装時に git 履歴から復元し `kasane/handbook/cross/local-development-setup.md` へ移した。廃止する文書の記述を「他所に既出」で破棄するときは、**その「他所」が同じ変更で消えないか**を確認する必要がある。
- 負 (実装で判明): `maui/ADR-0006` が `maui/README.md` の「SDK 更新時に再検証する箇所」の表を「再検証の入口」として指しており、README の削除で参照が切れた。移送先 (`maui/architecture/binding-build-integration.md`) から ADR への逆リンクを張って表 → ADR は辿れるようにしたが、accepted な ADR の本文は不変のため **ADR → 表の向きは切れたまま**である。ADR-0006 の決定内容 (Android binding は `gradlew` を Exec で呼ぶ) は変わっていないため supersede はしない。文書を廃止する決定は、その文書を名指しする accepted な ADR の有無を確認する必要がある。

---
出典: kasane/roadmaps/package-distribution/phases/phase-9-docs/history.md (2026-08-29 の 2 節) / kasane/roadmaps/package-distribution/phases/phase-9-docs/agenda.md (決定事項「README はルート 2 枚 (英語 + 日本語) に集約する」)
出典 (2026-08-30 改訂): kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/history.md (2026-08-30「`maui/spike/` の公開可否」) / kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/agenda.md (決定事項「`maui/spike/` は公開リポジトリに載せない」)
