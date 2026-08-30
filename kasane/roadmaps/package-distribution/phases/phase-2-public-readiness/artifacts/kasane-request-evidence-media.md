# Kasane への依頼: 変更 (changes/) 内の媒体ファイルの運用とローカル絶対パスの再発防止

KsSettingsView の public 化準備 (package-distribution ロードマップ phase-2、2026-08-21) で決めた、Kasane ハーネス側で実装してほしい 3 件の依頼。Kasane リポジトリにはそのまま依頼プロンプトとして渡す。

---

## 依頼プロンプト (ここから下を Kasane リポジトリに渡す)

Kasane ハーネスに次の 3 件を入れてください。依頼 1・2 は「`kasane/changes/<id>/` に保存される媒体ファイル (スクリーンショット・動画) は public リポジトリで commit した時点で公開される」という前提から、依頼 3 は「エージェントが成果物にローカル絶対パスを書く癖」から来ています。プロジェクト固有ではなく全プロジェクト共通の運用なので、ハーネス本体に置きます。

### 背景 (KsSettingsView での実測)

- 追跡ツリー約 200 MB のうち `kasane/changes/archive/**` の媒体 (PNG / MOV / MP4) が 623 件・181 MB (33 変更分、平均 5.5 MB/変更)。媒体を除くとツリー全体は 20 MB
- 内訳: `screenshots/` 106 MB・`ui/verification/` 34 MB・`artifacts/visual-check/` 16 MB・`evidence/` 14 MB・変更ルート直下 3.8 MB・ルート `verification/` 2.6 MB。仕様を運ぶ入力 (`ui/references/` + `ui/mock/` の approved.png 等) は 29 件・3 MB しかない
- 長命層 (decisions/ concepts/) から媒体への参照は文言 2 箇所のみ (ファイル名の言及、リンクではない)
- 媒体を `.gitignore` で追跡しない案は却下済み: Claude Code の worktree 運用では、main 上で保存した `ui/references/` や `approved.png` が未追跡だと worktree に存在せず、実装者に「見た目の正」が渡らない。**変更が進行中の間は媒体を全部追跡する**のが前提
- 画像の中の個人情報は gitleaks 等では検出できない。archive 時に削除しても git 履歴には残るため、防波堤は撮影・保存の時点にしか置けない

### 依頼 1: ksn-distill の archive 時に媒体ファイルを削除する

対象: `skills/ksn-distill/SKILL.md` Step 6 (アーカイブ)。現状は `mv <repo>/kasane/changes/<change-id> <repo>/kasane/changes/archive/<change-id>` の 1 行。

- mv の前に、変更ディレクトリ配下の媒体ファイル (拡張子の目安: png / jpg / jpeg / gif / webp / mov / mp4 / webm。heic 等も含めてよい) を削除するステップを追加する。削除は `rm` ではなく `trash` を使う
- 削除対象は置き場を問わず全媒体 (`ui/references/` `ui/mock/approved.png` `ui/verification/` `screenshots/` `evidence/` `artifacts/` 直下など)。蒸留が完了した変更では、モック承認・視覚照合・実機証跡の結果はすべて文書側 (`ui/brief.md` の照合記録、review-NNN.md / verify-NNN.md、deviation.md) に残っているのが前提。削除した媒体の一覧 (ファイル名) を brief.md または change 直下の文書に 1 行で残すかは Kasane 側の判断でよい
- mock の HTML (`ui/mock/*.html`) は媒体ではないので残す
- 有効化は `kasane/config.yaml` の opt-in キーを想定 (例: `distill: archive-media: delete | keep`)。既定値をどちらにするかは Kasane 側で決めてよい (KsSettingsView は delete を使う)
- ksn-distill の「やってはいけないこと」に「媒体を削除せずに archive へ移動する (設定が delete のとき)」を追加し、Step 6 の完了条件に含める
- 注意書きとして「削除は作業ツリーの容量対策であり、git 履歴からは消えない。個人情報の対策は依頼 2 の撮影時規律で行う」を明記する
- 関連して、`ksn-core/references/ui-artifacts.md` の「中間ラウンドのスクリーンショットは保存しない (足場層を重くしない)」は、実測では `screenshots/` 278 件が最大勢力で守られていない。ksn-ui / ksn-verify / ksn-review が撮影する側の規律として、保存先を `ui/verification/` (最終周) と `evidence/` (実機証跡) に限定し、`screenshots/` のような任意ディレクトリへの保存をやめる旨を強めてほしい

### 依頼 2: スクリーンショット・画像の個人情報規律 (撮影・保存時)

対象: `ksn-core/references/ui-artifacts.md` (規約本体)、`ksn-ui/SKILL.md` の「スクリーンショットを取得する」ステップ、実機証跡を撮る ksn-verify / ksn-review、ユーザーから貼られた画像を即保存する ksn-explore / ksn-agenda / ksn-propose。lessons は閾値昇格型なので規約の初期配置には向かない — 規約本文に置くのが適切だと考えている (判断は Kasane 側で)。

規律の内容 (案):

- 前提: `kasane/` 配下に保存した画像・動画は、リポジトリが public なら commit した時点で公開される。git 履歴に残るため後から消せない。画像内の個人情報は secret scanner で検出できない
- 撮影はシミュレータ / エミュレータ + デモデータ (架空の氏名・メール・電話番号) で行うのを原則とする
- 実機で撮る場合は、通知・アカウント名・メールアドレス・連絡先・写真・位置情報・端末名・Wi-Fi 名・ステータスバーの個人要素が写らない状態で撮る。写り込んだ画像は commit せず、結果を brief.md / verify の文言で記録するか、マスク処理した画像を保存する
- 動画 (mov / mp4) は原則撮らない・残さない (容量が大きく、個人情報の確認・マスクが困難)。必要なら静止画の連番で代替する
- ユーザーから貼られた画像を `ui/references/` や `artifacts/` に保存するときも同じ基準で確認し、個人情報が含まれるなら保存前にユーザーへ確認する (トリミング・マスクの提案)
- ksn-ui の視覚照合ループ・ksn-verify の実機証跡の「保存してから提示する」規律は維持する (保存しないのではなく、保存してよい画像だけを撮る)

### 依頼 3: 成果物に書くパスの規約と、ローカル絶対パスの発生源の修正


- a) **記述規約**: ワーカースキル (ksn-implement / ksn-review / ksn-verify / ksn-scout / ksn-second-opinion / ksn-distill など成果物を書くもの) と議論系スキル (ksn-explore / ksn-agenda / ksn-propose) の出力規約に次を明記する:
  - 成果物内のファイル参照は**リポジトリ相対パス**で書く (`kasane/changes/<id>/review-001.md:12` の形)。絶対パスは書かない
  - 他リポジトリのファイルは **`../<リポジトリ名>/<そのリポジトリ内の相対パス>`** で書く。リポジトリ群が同じ親ディレクトリに clone されていることを開発環境の前提とし、エージェントが実際に辿れる形を正とする (`<リポジトリ名>:<パス>` のような記号表記は辿れないので使わない)
  - ADR の `出典:` 行・concepts の参照表も同じ規約に従う
- c) **標準 hook の同梱**: ksn-init の scaffold に、Write / Edit の PreToolUse で `/Volumes/<名前>` `/Users/<名前>/` (Windows なら `C:\\Users\\`) を含む書き込みをブロックする hook を含める。判定は**リポジトリ相対パスの第 1 セグメント**で行い (`.claude/worktrees/<name>/kasane/...` も検査対象にする。除外パスに `.claude/` を丸ごと入れると worktree 配下が無検査になる)、`<USER>` のようなプレースホルダや `/Users/...` のような例示は除外する。同じルールを CI で一括実行できる lint スクリプトも対にする (KsSettingsView では `.claude/hooks/local-path-check.py` + `scripts/local-path-lint.py` を先行して作るので、それを取り込んでよい)

### 受け入れの目安

- ksn-distill Step 6 に媒体削除ステップと config キーが入り、「やってはいけないこと」に反映されている
- ui-artifacts.md と ksn-ui (+ 実機証跡を撮るスキル、画像を保存する議論系スキル) に撮影・保存時の個人情報規律が入っている
- ksn-init の scaffold にローカルパス検査の hook + lint が含まれる
- deploy.sh で配布される各プラットフォームのスキルに反映されている

---

## KsSettingsView 側での対応 (依頼の外)

- `.claude/hooks/local-path-check.py` + `scripts/local-path-lint.py` を先行実装し、phase-3 の検証 CI に載せる
- `scripts/device-id-lint.py` (実機シリアル・Simulator UDID・session id の検査) も先行実装済み。ローカルパス検査と同じ性質の混入 (検証証跡に作業環境の個体情報が乗る) を防ぐもので、c) の標準 hook 同梱を検討する際の追加候補として提示できる

- 公開ツリー (新規 public リポジトリの initial commit) からは `kasane/changes/archive/**` の媒体 623 件を外す (phase-2 決定事項「公開対象の範囲 — evidence 媒体」)。既存の媒体は旧 private リポジトリの履歴に残る
- Kasane 側の反映後、`kasane/config.yaml` で `distill: archive-media: delete` (キー名は Kasane 側の確定値に合わせる) を設定する
