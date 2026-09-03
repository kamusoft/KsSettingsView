# verification-ci デルタスペック

## MODIFIED Requirements

### Requirement: マージ保護
`develop` / `main` への変更の取り込みは、7 job (ios / android / maui / lint と消費者検証 3 job) すべての成功を必須 status check とする pull_request 経由 SHALL とする (管理者のバイパスは許容する)。`main` はリポジトリの default branch であり、その先端は最新リリース、またはリリース進行中 (リリース PR のマージ後、publish 成功まで) のリリース候補を表す。`main` を base とする PR は head が `develop` のものだけを受け付け、それ以外は CI の検査で失敗する SHALL。

#### Scenario: 検査未通過のマージ拒否
- **GIVEN** 7 job のいずれかが失敗している PR
- **WHEN** マージしようとする
- **THEN** マージはブロックされる

#### Scenario: 直 push の拒否
- **GIVEN** `develop` または `main` への直接 push
- **WHEN** push を試みる
- **THEN** push は拒否される (admin バイパスを除く)

#### Scenario: develop 以外から main への PR は失敗する
- **GIVEN** feature branch を head、`main` を base とする PR
- **WHEN** CI が実行される
- **THEN** head の制限を理由に必須 check が失敗し、マージはブロックされる

#### Scenario: main が保護された default branch である
- **GIVEN** リポジトリの設定
- **WHEN** default branch と `main` の保護設定を確認する
- **THEN** default branch は `main` で、`develop` と同じ必須 status check・PR 必須・force-push 禁止・削除禁止が設定されている
