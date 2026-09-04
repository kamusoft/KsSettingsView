# phase-13-ci-trigger-slimming

初回リリース後の実測を受けて、検証 CI のトリガーと branch protection を「外部 PR なし・開発者 1 人・ローカルマージ → `develop` へ直接 push」という実際の開発運用に合わせて絞り、`develop` へ反映するたびの待ち時間を減らす。phase-3 で決めたトリガーと必須チェック化の改訂にあたる。

## 論点

(すべて決定事項へ移動済み — ksn-explore 2026-09-04 で解消)

## 決定事項

- **トリガーの割り当て (2026-09-04)**: `develop` 宛ての pull_request トリガーは廃止する。`develop` への push では lint + 本体検証 3 本 (ios / android / maui) だけを走らせる。`main` 宛ての pull_request (head は `develop` のみ、lint job が検査) ではそれに消費者検証 3 本 (dry-run) を加えて 7 job とする。release workflow (workflow_dispatch) は変更しない。消費者検証は配布経路の壊れを見るもので、本体のコード変更ごとに繰り返す価値が低く、`main` 宛て PR とリリース (dry-run + smoke) で足りる
- **`develop` の branch protection (2026-09-04)**: 必須 status check と PR 必須化を撤去し、force-push 禁止・削除禁止だけ残す。開発者は管理者で enforce_admins は off のため、必須 check を残しても直接 push は素通りし効力がない (走る 4 本に絞って残す案はこの理由で却下)。7 本のまま残す案は、消費者検証 3 本が `develop` で二度と走らず「expected」のまま宙に浮くため却下。`develop` の CI は直接 push に対する事後検証として動き、失敗は GitHub 標準の通知で拾う
- **`main` の branch protection (2026-09-04)**: 必須 status check 7 件を維持する。以後「develop と同じ」ではなく `main` 独自の設定として扱う (phase-8 agenda に追記済み)
- **実装方式 (2026-09-04)**: 入口 workflow 1 本のまま、消費者検証 3 job に事象による条件 (`if: github.event_name == 'pull_request'`) を付ける。job 名は変えず、`main` の必須 check 名「consumer-xxx / verify」を固定したまま保つ (cross/ADR-0025 の規律)。入口を develop 用 / main 用の 2 ファイルに割る案は、status check 名が job 名で決まるため利点がなく、ADR-0025 の「入口 1 本」にも反するので採らない
- **変更パスによる絞り込み (2026-09-04)**: `main` 宛て PR では引き続き行わない (cross/ADR-0025 を維持)。`develop` への push に限り paths-ignore を入れる (`kasane/**`・Issue テンプレート・CONTRIBUTING 2 枚)。必須 check を撤去した develop では ADR-0025 の理由 (素通り経路) が成り立たず、ハーネス記録だけの push で macOS ランナーが起きるのは無駄なため。README と `skills/` は README example lint の入力なので除外しない (同日、実測記録の push で 4 job が起動したのを受けて追加決定)
- **`develop` push の concurrency (2026-09-04)**: group をブランチ単位 (`github.ref`) にし `cancel-in-progress: true` とする。現状の group は push では commit SHA で、同じ group に後続が来ないため打ち切りは構造的に起きていなかった。worktree 並列で push が連続したとき最新だけを検証する。打ち切られた commit の結果は残らないが最新 push がそれを含むので検出は漏れない。`main` 宛て PR は PR 番号 group のまま
- **ADR 化 (2026-09-04)**: 上記をまとめて [cross/ADR-0028](../../../../decisions/cross/0028-ci-triggers-by-branch-role.md) (proposed) に起票した。accepted への昇格は change の蒸留時

## TODO

- [x] 論点の解消 (2026-09-04: ksn-explore で全件決定)
- [x] change `slim-ci-triggers` を S 級として実装する (2026-09-04、commit e85cb98。develop の保護設定変更の証跡は change の evidence/branch-protection-develop.md) ([exploration.md](../../../../changes/slim-ci-triggers/exploration.md) の「実装の見当」)
- [x] handbook `cross/release-procedure.md` の「develop の保護設定が正」「必須 check 7 件」の記述を追従させる (change に同梱、2026-09-04)
- [x] 実装後、`develop` への push で 4 job だけが走ることを確認し、所要時間の実測をここに残す → run 33849577847 (2026-09-04、commit e85cb98): 壁時計 7.7 分 (変更前 14〜20 分)。lint 0.1 / ios 3.3 / android 5.3 / maui 7.6 分、consumer 3 job は skipped。壁時計は maui に張り付く
- [x] paths-ignore の確認 (2026-09-04): ci.yml を含む push (29cc361) では run 33850817547 が 4 job で起動。kasane だけの push (このコミット) で起動しないことは push 後の run 一覧で確認する
- [ ] 蒸留時に cross/ADR-0028 を accepted へ昇格し、本フェーズを completed にする
