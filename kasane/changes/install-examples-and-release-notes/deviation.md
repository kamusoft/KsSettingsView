# Deviation: install-examples-and-release-notes

- [付随修正] `.github/workflows/ci.yml` の lint job: tasks は Release ノート組み立てスクリプトの自己テストを CI で走らせることまでは名指ししていないが、`Release notes script selftest` の step を追加した。理由: `scripts/release/` の各スクリプトが lint job に自己テストを持つ慣例があり、繋がないと本番リリースまで一度も回らないため (2026-09-11)
- [付随修正] `scripts/release/check-publish-step-order.py` の自己テスト: 「他の成果物の取得が無くなると落ちる」ケースのフィクスチャを、publish job の成果物取得 step が 1 本増えたことに追随させた。理由: 本変更で Release ノートの成果物取得を足した結果、既存ケースの前提 (download step 2 本) が崩れたため (2026-09-11)
- [付随修正] `.github/workflows/ci.yml` の lint job: tasks 2.4 が名指しするのはインストール例 lint の実行だけだが、`Install example lint selftest` の step も同じ並びへ追加した。理由: `scripts/` の lint と release script はいずれも自己テストを lint job に繋ぐ慣例があり、繋がないと検出力の退化が平時の緑に埋もれるため (2026-09-11)
- [付随修正] `kasane/handbook/index.md`: cross 行の内容要約に「インストール例」を足した。理由: tasks 9.5 で cross に規約を 1 本増やしたため、地図の要約が実体より狭くなるのを避けた (2026-09-11)
- tasks 9.6 の走査結果: `kasane/concepts/cross/architecture/release-pipeline.md:29,65` に `scripts/release/set-readme-version.py` による置換と validate の一致検査の記述が残ることを確認したが、本 change では直さない。理由: proposal の「規範の追随を実装に含める」はオーナー判断で handbook・`AGENTS.md`・インストール例の契約の規約化の 3 点に限定されており、concepts の改訂内容は tasks.md の「蒸留への申し送り」に具体的に明記済みのため (2026-09-11)
