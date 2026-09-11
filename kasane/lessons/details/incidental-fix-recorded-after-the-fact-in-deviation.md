# incidental-fix-recorded-after-the-fact-in-deviation (process L-009 の経緯)

inbox パターンとして pain 3 件で閾値到達し、2026-09-07 にオーナー承認で `process.md` L-009 へ昇格した。

## 経緯

- 2026-09-02 add-consumer-verification: verify-001 が INVALID。Scenario の欠落・乖離は 0 件だったが、deviation.md に無い付随修正が 2 件あった — (1) `kasane/config.yaml` の `lint.identity.allow` への `repo.local` 追加 (tasks 4.3 は scope への `verification` 追加しか指示していない)、(2) `android/kssettingsview/build.gradle.kts` の SNAPSHOT ガードのエラー案内の改訂 (deviation 2 件目の署名条件化の記述に含まれていなかった)。いずれも実装の欠陥ではなく記録の漏れで、deviation.md の追記だけで verify-002 が VALID になった。裁定を要さない小修正 (lint の allow 追加、ガード文言の追随) ほど記録されにくい。
- 2026-09-06 skills-install-version-drift: review-002 Minor 1。指揮側が review-001 Minor 1 への対応として `.github/workflows/release.yml` / `ci.yml` の step 名・コメントを直した際、同じサイクルで直した `release-procedure.md` の付随修正は deviation.md に書いたのに workflow 側は書かず、再レビューで記録漏れとして指摘された。修正内容に異論はなく 1 行追記で解消。レビュー指摘対応としての数行修正は「指摘に従っただけ」と感じられて記録の対象と認識されにくい。
- 2026-09-07 english-diagnostic-messages: tasks.md を持たない S 級で、deviation.md がそもそも作られなかった。付随修正は 2 件 — (1) `KsBridgeValueTransport` の `diagnose` にフォールバック結果の引数を足し、`optionalDate` が既定値ではなく null / nil を返す事実に診断文を合わせた (review-001 Minor 起因。日本語の原文から続く既存の不正確さ)、(2) 診断に載せる既定値をリテラルの二重定義から実際の返却値の定数導出へ変えた (review-002 Suggestion 起因)。どちらもレビューが「隣接する課題として同梱が妥当」と判定したものが、その場で直されたまま記録されずに完了し、蒸留 (ksn-distill Step 2) で再構築することになった。

## 昇格時の一般化 (2026-09-07)

昇格前のルール文は発火条件を「tasks に無いファイルへ手を入れた」と書いていたが、3 件目は tasks.md を持たない S 級で、文字どおりには発火しない条件だった。S 級では合意済みスコープの正が exploration.md の決定事項になるため、名指しの基準を変更級ごとに分けて書き直した。

同時に、書く対象を「付随修正」だけでなく「決定事項と違う形に落ち着いた実装」まで広げた。同じ蒸留で扱った fix-release-central-validation-wait は、決定事項が「upload step に待ちを追加」「handbook の Maven upload 行を直す」だったのに対し、レビュー指摘を受けて実装は独立 step の新設と handbook への 2 行追加になっている。レビューは両方とも「決定事項の意図を満たす」と判定して逸脱扱いにしなかったが、記録が無いと蒸留時に決定事項と実装の差を再構築することになる点は付随修正と同じだった。

## 関連

[[known-limitation-accepted-without-deviation-record]] (既知の限界を記録なしで受容する型) と対をなす — あちらは「限界の受容」、こちらは「合意済みスコープ外の編集の記録」で、どちらも deviation.md が蒸留の入力になることを前提にした記録規律。
