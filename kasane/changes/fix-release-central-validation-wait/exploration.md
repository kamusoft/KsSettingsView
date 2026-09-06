# Exploration: fix-release-central-validation-wait

## 課題 / 動機

release workflow の publish が、Maven Central の deployment の検証完了を待たずに release へ進み、失敗することがある。

発見の文脈: 0.1.0-beta.2 のリリース (run 34039941831) の publish (attempt 1) が `Release Maven Central deployment` で失敗した。

```
##[error]release できる状態ではない (VALIDATED を期待、実際は VALIDATING): <deployment-id>
```

このステップは upload の直後に置かれており、次の 2 行を続けて実行する (`.github/workflows/release.yml`)。

```
scripts/release/central-portal.sh release "${KS_DEPLOYMENT_ID}"
scripts/release/central-portal.sh wait-published "${KS_DEPLOYMENT_ID}"
```

`scripts/release/central-portal.sh` のサブコマンドは次の構成で、**VALIDATED になるまで待つ手段がない**。

| サブコマンド | 振る舞い |
|---|---|
| `status` | 状態を出力する |
| `release` | VALIDATED を再確認してから release する (待たない。VALIDATING なら失敗) |
| `wait-published` | PUBLISHED になるまで待つ |
| `drop` | VALIDATED / FAILED のときだけ削除する |

つまり公開の完了 (PUBLISHED) は待つのに、検証の完了 (VALIDATED) は待たない非対称がある。Central Portal の検証時間は upload の内容と混雑で変わるため、待たずに進む限り成否が運任せになる。

影響: publish は不可逆な公開を含む段であり、この失敗は NuGet.org への push が済んだ後に起きる。失敗しても後始末で deployment は削除されず (このときは既に VALIDATED になっていた)、同じ version で「失敗した job から再実行」すれば復旧できたが、リリースのたびに再実行が 1 回増える。復旧できたのは後始末の時点で検証が終わっていた偶然にも依っており、まだ VALIDATING のままなら drop も release もできない状態で attempt が終わる。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- **未探索 (簡易起票)** — リリース作業中に発見したためスコープ外として起票した。深掘りは本 exploration.md を出発点に通常の探索で行う
- 手段の候補: (a) `wait-validated` 相当のサブコマンドを足して release の前に挟む、(b) `release` 自体に待ちを内蔵する、(c) upload 直後に待ちの段を置く。`wait-published` が既にある以上、待ちの実装は流用できる見込み
- 待ちの上限をどう置くか。Central Portal の検証は混雑で伸びるため、上限超過を失敗にするか、deployment を残して次の attempt に委ねるか
- 後始末 (`Drop pending deployment`) が VALIDATING のときに何もできない経路の扱い。待ちを入れれば到達しにくくなるが、消えるわけではない

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定

workflow 1 ファイルと script 1 本の局所修正に収まれば S 相当と見込めるが、待ちの上限と失敗時の後始末の設計次第で判断が変わるため、探索で手段を決めてから判定する。
