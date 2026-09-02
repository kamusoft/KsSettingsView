# phase-4-ios-packaging 議論履歴

## 2026-09-01: products の最終形

- 選択肢: A) umbrella `KsSettingsView` 1 本のみ (既存 3 product 削除) / B) umbrella + 3 product 併存 / C) 3 product のまま
- 採用: A
- 理由: cross/ADR-0018 の決定文が「product は 1 本、3 target を束ねる」と既に定めており、Android の単一 artifact と「利用者が手で入れるのは 1 点」で揃える骨子。未リリースのため product 削除の互換性負債はゼロ。B の利点 (部分リンク) は Core 単独利用という非現実的なケースにしか効かず、アプリサイズは dead code stripping で差が出ない
- 付随: `samples/ios` は 3 productRef → umbrella 1 本へ差し替え。Local Swift Package 参照 `../../ios` は変更不要。新規 ADR は不要 (ADR-0018 そのまま。蒸留時の accepted 昇格の裏付けとする)
- 補足: 論点「配信リポジトリの名前」は phase-9 (2026-08-29) で `KsSettingsView-SPM` と確定済みだったため、論点を初期設定のみに絞った

## 2026-09-01: スナップショットの中身と除外規則

- 選択肢: A) ホワイトリスト方式 (含めるものだけ列挙) / B) 除外リスト方式 (`binding/` 等を弾く)
- 採用: A — `Package.swift` / `Sources/` / `Tests/` / ルート `LICENSE` コピー / 誘導 README の 5 点
- 理由: 除外リスト方式は将来増える生成物の追記漏れで公開リポジトリへの混入事故が方式的に残る (現に agenda 未記載の `.build/` `.swiftpm/` が `ios/` 直下に実在した)。ホワイトリストなら列挙外は構造的に入らず、追記忘れはビルド破壊で即検出される
- 付随: `Tests/` は `Package.swift` の testTarget path が参照するため除外不可 (利用者のビルド対象にはならない)。誘導 README テンプレートの置き場は次論点 (生成実装) で扱う

## 2026-09-01: スナップショット生成の実装と phase-8 との分担

- 選択肢: A) `scripts/spm-snapshot/` にファイル配置専用スクリプト (git 操作は呼び出し側) / B) スクリプトが commit/tag/push まで担う / C) release workflow にインライン実装
- 採用: A
- 理由: tag は publish 全成功後にのみ生まれる (cross/ADR-0020) ため git 操作のタイミング制御は workflow 側の責務。ファイル配置だけに絞れば phase-4 の手動実行と phase-8 の CI が同一スクリプトを無改変で共有でき、手動検証した経路がそのまま本番経路になる。ローカルでは push 不要で動作確認できる
- 付随: 誘導 README テンプレートも `scripts/spm-snapshot/` に同居。スクリプトは冪等 (作業コピーの `.git` 以外を空にして 5 点配置)

## 2026-09-01: tag 表記の統一

- 選択肢: A) 接頭辞なし `X.Y.Z` / B) `vX.Y.Z`
- 採用: A
- 理由: ADR-0020 の「version の SSoT は dispatch の入力値 (= 生成される tag)」を文字どおりに保つため。dispatch 入力・tag・SwiftPM 解決バージョン・Gradle / MSBuild 注入値が同一文字列で流れ、CI・手順書・検証に v の付け外し変換が一切入らない。SwiftPM はどちらの表記も解決できるため技術差はなく、変換ゼロを取った
- 付随: monorepo と配信リポジトリで同じ値。KsDialogs (tag なし・表記未規定) へは phase-11 の逆流で同表記を展開。ADR は新規起票せず cross/ADR-0020 へ追記 (ADR-0018 のリポジトリ名追記と同じ前例)

## 2026-09-01: 配信リポジトリへの CI 書き込み権限と secret の置き場

- 選択肢: A) 書き込み許可付き deploy key / B) fine-grained PAT / C) GitHub App
- 採用: A。秘密鍵は monorepo のリポジトリ単位 Actions secrets (例: `SPM_DEPLOY_KEY`)
- 理由: release workflow が配信リポジトリへ行う操作は git push (commit と tag) だけで API 操作は不要。deploy key は対象 1 リポジトリの git 操作に限定され、有効期限がなくローテーション運用が発生しない (PAT は最長 1 年で失効し、年次更新を忘れるとリリースが突然壊れる)。個人アカウントにも紐付かない
- 付随: organization secrets は KsDialogs と鍵を共有する形になるため不採用 (1 鍵 = 1 リポジトリ)。鍵の作成・登録は phase-8 で実施 — phase-4 の初回 push は手動のため不要。ADR 起票なし (鍵方式の差し替えは後からでも安価)

## 2026-09-01: 配信リポジトリの初期設定

- 内容: 最初から public / default branch `main` / Issues・Wiki・Projects・Discussions 無効 / PR は collaborators only (ADR-0024 と同じ機構) / README は誘導のみ / workflow・branch protection なし / GitHub Release なし (tag のみ、Release は monorepo 側) / description・Website は monorepo へ
- 論点だった PR の扱い: 「README 表明のみ」は ADR-0024 が却下済みの形 (都度閉じる運用負荷・投稿者が書き上げてから断られる) のため、monorepo と同じ collaborators only に揃えた。配信リポジトリは PR CI 不要なので完全無効化の不利益もないが、設定を monorepo と揃えて説明を 1 つにする方を優先
- private → public の 2 段階は不採用: ホワイトリスト方式により機密混入の余地が構造的になく、public が phase-4 ゴール (https 解決確認) の前提

## 2026-09-01: binding への影響確認

- agenda の「Sources の位置は変わらないので無影響の見込み」は実査で覆った: binding xcodeproj (`ios/binding/KsSettingsViewBridge.xcodeproj`) が product `KsSettingsViewCore` / `KsSettingsViewUI` を productRef でリンクしていた — product 削除 (本日 1 件目の決定) でビルドが壊れる
- 採用: binding xcodeproj の product 参照も umbrella 1 本へ差し替え、phase-4 の作業範囲・受け入れ条件 (ビルド成功 + xcframework 生成確認) に含める。「3 product を残す」への巻き戻しは、monorepo 内部の消費者の都合で公開 product 面を広げることになるため不採用
- MAUI csproj 側は ProjectReference 連鎖のみで `Package.swift` の product を参照せず無影響と確認

## 2026-09-01: フェーズ議論の完了

- 全 7 決定 (products 1 本化 / スナップショット中身 / 生成スクリプト / tag 表記 = ADR-0020 追記 / deploy key / 初期設定 / binding 差し替え) で論点が出尽くし、残り 2 論点 (https 解決確認・concepts 追随) は議論の余地のない作業項目として TODO 化。ksn-propose (フェーズ由来入力) へ進む

## 2026-09-01: 提案化とセカンドオピニオンでの追加決定

- change `add-spm-distribution` を起票。セカンドオピニオン (codex、second-opinion-spec-001.md) の指摘を受けオーナー裁定 2 件: (1) 検証用 prerelease tag は検証完了後に削除し配信リポジトリに tag を残さない (lockstep / tag-last との緊張回避)、(2) 級は M → L (ksn-core「外部連携」基準)・domain は ios → cross に改訂し design.md を追加
