# Tasks: backport-release-workflow-hardening

## 1. Maven Central の公開待ち

- [x] 1.1 `scripts/release/central-portal.sh` の `wait-published` の上限を、検証の決着待ちとは別の環境変数で設定できるようにし、既定を 90 分にする (→ Requirement: Maven Central の公開待ち)
- [x] 1.2 `central-portal.sh` の自己テストに、新しい環境変数が `wait-published` にだけ効き `wait-validated` に影響しないことを確かめる検査を足す (→ Requirement: Maven Central の公開待ち)
- [x] 1.3 publish job の `timeout-minutes` を 240 にし、算出根拠の表 (待機の上限 150 + 各待ちの最後の照会 15 + 巡回間隔の端数 2 + 待機外の照会 30 + 本体処理 20 + 余裕 23) をコメントに書く (→ Requirement: Maven Central の公開待ち)
- [x] 1.4 各項の定数 (待機の上限・照会の応答上限・巡回間隔) と `timeout-minutes` を突き合わせ、合計が上限を超えていたら失敗する検査を用意し、lint job で走らせる (→ Scenario: 時間予算が待ちの最悪ケースと応答上限を収容する)

## 2. 公開レジストリへの反映待ち

- [x] 2.1 `scripts/release/wait-for-registries.sh` の nuget.org 照会に HTTP status の分岐を入れ、反映済み・未反映・判定不能を区別する (→ Requirement: 公開レジストリへの反映待ち)
- [x] 2.2 同ファイルの Maven Central 照会でも、200 以外を一様に扱わず未反映 (404) と判定不能 (それ以外・通信失敗) を区別する (→ Requirement: 公開レジストリへの反映待ち)
- [x] 2.3 判定不能を失敗の種別 (通信の失敗 / 応答が成功を示さない / 応答を解釈できない) まで分けて記録する (→ Requirement: 公開レジストリへの反映待ち)
- [x] 2.4 Maven 座標 1 件と NuGet Package ID 3 件の計 4 対象それぞれについて、巡回ごとの分類を保持し出力する (→ Scenario: 対象ごとに状態が分かれて残る)
- [x] 2.5 上限に達して失敗するときの出力に、対象ごとの最後の分類と判定不能の種別を含める (→ Scenario: 上限超過時に対象ごとの最後の状態が分かる)

## 3. 保留中 deployment の引き継ぎ

- [x] 3.1 引き継いだ deployment ID を読み込む step を、publish が必要とする他の成果物の取得より前へ移す (→ Requirement: 保留中 deployment の引き継ぎ)
- [x] 3.2 引き継ぎの読み込み結果 (読み込み済みか・引き継ぎが存在したか) を output またはログに残し、存在しない初回と読み込み失敗を区別できるようにする (→ Scenario: 引き継ぎが無い初回の実行と区別できる)
- [x] 3.3 引き継ぎの準備判定 (引き継ぎの有無の分類と、失敗経路で書き戻してよいかの判定) を `scripts/release/` のスクリプトへ切り出し、workflow からはそれを呼ぶ形にする (→ Requirement: 保留中 deployment の引き継ぎ)
- [x] 3.4 3.3 のスクリプトに自己テストを足し、次の 5 状態を検査する: 引き継ぎあり / 初回で引き継ぎなし / 引き継ぎの読み込み自体が失敗 / 読み込み後に別の成果物の取得が失敗 / 各失敗経路が既存の引き継ぎを空で上書きしない (→ Requirement: 保留中 deployment の引き継ぎ)
- [x] 3.5 publish job の step 順序 (引き継ぎの読み込みが他の成果物の取得より前にあること) を機械的に検査する検査を用意し、lint job で走らせる (→ Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる)

## 4. 自己テストの CI 接続

- [x] 4.1 `scripts/release/wait-for-registries.sh` に自己テストを足し、Maven と NuGet のそれぞれについて次の入力を表として与えて分類を検査する: 200 かつ当該 version あり / 200 かつ当該 version なし / 404 / 5xx / 通信そのものの失敗 / 応答を解釈できない (→ Requirement: 待機スクリプトの自己テスト)
- [x] 4.2 同じ自己テストで、4 対象の分類が混在する巡回・判定不能からの回復・上限超過時の対象別出力を検査する (→ Requirement: 公開レジストリへの反映待ち)
- [x] 4.3 `.github/workflows/ci.yml` の lint job に `wait-for-registries.sh --selftest` を追加し、`central-portal.sh --selftest` と並べる (→ Requirement: 待機スクリプトの自己テスト)

## 5. 検証

- [x] 5.1 `central-portal.sh --selftest` と `wait-for-registries.sh --selftest` が全件通ることを確認する
- [x] 5.2 `dry-run` で release を起動し、validate から消費者検証までが通ることを確認する
- [x] 5.3 1.4 と 3.5 の検査が lint job で実際に走り、意図的に定数を壊したときに失敗することを確認する
- [x] 5.4 3.4 の自己テストが 5 状態すべてで通ることを確認する

## 蒸留への申し送り (実装タスクではない)

- `kasane/handbook/cross/release-procedure.md` — 「失敗したとき」節の上限 30 分の記述と、「起動」節の所要時間の説明を新しい上限に合わせる
- `kasane/concepts/cross/architecture/release-pipeline.md` — 「公開確認」節に状態分類を反映する
- KsDialogs へ `kind: change` の知らせを出す — 知らせ #5 の対象が `wait-for-registries.sh` の Maven 側にも及ぶこと (あちらの探索では `central-portal.sh published` と混同されていた可能性がある)、および待機スクリプトの自己テストを CI へ接続した判断を伝える
