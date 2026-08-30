# OpenSpec から Kasane への移行サマリ

完了日: 2026-07-18

## 対象と方針

- `openspec/specs/` の13 capabilityを、コードとテストを先に読む規律で蒸留した。
- `openspec/changes/archive/` の20 changeから、長命な決定だけをADRへ起こした。
- `docs/` は概念抽出時の補助資料として照合した。
- ユーザー判断により、進行中の `openspec/changes/` 7件は移行対象外とした。
- `openspec/drafts/` は一括移行せず、必要になった時点で個別に蒸留する。
- Requirements / Scenario、具象API一覧、内部実装フローは長命層へ機械変換しなかった。

## 規約

旧 `openspec/config.yaml` の日本語記述規約は `kasane/config.yaml` に包含済みだった。capability命名タクソノミは存在せず、OpenSpec artifact固有の形式規約はKasaneと重複するため移植しなかった。

現仕様のSSoTはコードとテスト、長命な判断は `kasane/decisions/`、責務境界・不変条件は `kasane/concepts/` とする案内へ更新した。

## ADR

決定ファイルは12件である。

- ADR-0001〜0011を作成した。
- ADR-0013を追加し、ADR-0003をsupersededとした。
- 現在のstatusはaccepted 11件、superseded 1件である。
- ADR-0012案はユーザー判断で却下した。Maven `groupId` はADR-0002どおり `jp.kamusoft` とし、製品名は `artifactId` で識別する。

Sampleのプロジェクト形式、Gradle composite build、Material3ホストthemeなど、既存決定の具体化または局所的な現在要件は新規ADRにしなかった。

## concepts

16概念を確定した。

- architecture: リポジトリ境界、Native Host、Store、宣言UI Bridge、宣言ツリー同一性、Cell Renderer Registry、表示状態同期
- core-model: 設定ツリー、構造変更
- cells: 基本Cellの意味と状態契約、Cell用画像の値境界
- styling: スタイル解決、Cellの視覚状態、Cell共通行、リスト外観
- conventions: 公開識別子

Samplesは独立概念を作らず、実行可能な利用例・目視確認という責務をリポジトリ境界へ、AndroidのMaterial3ホストtheme要件をスタイル解決へ合流した。

## drift

バッチ統合で40件のdriftを分類した。

- 旧spec/docsの構成名、API名、内部widget、画面一覧など、現行コード・テストで置き換えられる記述は長命層へ移さなかった。
- accepted ADRと現行コードが異なる事項は、ADRを無断で変更せず後続探索へ送った。
- 実装不具合候補は移行で正当化せず、後続Kasane changeの対象として残した。
- 未確定の公開値・fallback・永続化契約はconceptsへ含めなかった。

主な後続対象:

- Maven `groupId` の現行Gradle設定との不一致
- 宣言ツリーID優先順位と衝突回避
- iOS初回表示前Diff、Theme再適用、separator反映
- DSL無効化modifier、Android予約viewType検証
- Android icon寸法・角丸、fontFamily、Button共通行経路、Radio再タップ通知
- 未消費Style公開値、Android platform fallback、Root Header / Footer再bind契約

## 凍結

`openspec/` は削除・リネームせず、出典リンクを保つ歴史資料として凍結した。`AGENTS.md` に編集禁止とKasaneへの新規変更集約を明記した。

README、docs、Sample READMEは、OpenSpecを現仕様の正本とする旧案内を廃止し、歴史資料として参照する表現へ更新した。固定デモ一覧と誤った利用者定義Cell参照も簡略化・修正した。
