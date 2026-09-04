#!/bin/bash
# 配信リポジトリの同名 tag と、今回のスナップショットの内容の照合。
#
# 使い方:
#   scripts/release/check-distribution-tag.sh <配信リポジトリの作業コピー> <version>
#
# 作業コピーには scripts/spm-snapshot/sync-snapshot.sh を適用済みであることを前提とする
# (スナップショットの生成そのものはこのスクリプトの責務ではない)。
#
# 判定は次の 3 通りで、結果を標準出力へ 1 語で出す。説明とエラーは標準エラーへ出す。
#
#   absent   同名の tag が無い (これから作れる)
#   match    同名の tag があり、内容が今回のスナップショットと同一 (作成を飛ばせる)
#   (失敗)   同名の tag があり、内容が異なる。exit 1
#
# 内容が異なる tag は上書きできないため、公開の途中でこれに当たると iOS だけ利用者から
# 解決できない状態が残る。呼び出し側は、不可逆な公開に入る前と tag を作る直前の両方で
# この検査を通す。
#
# 判定は remote 側の tag を正とする。手元の tag は古くなり得るので、有無は remote へ
# 直接聞き、手元の同名 tag は remote の状態に合わせて取り直す (または消す)。
#
# 未追跡ファイルを含めたツリーを比較するため、作業コピーの index を書き換える
# (`git add -A`)。呼び出し側は index と手元の tag の状態に依存しないこと。

set -euo pipefail

usage() {
    echo "使い方: $(basename "${BASH_SOURCE[0]}") <配信リポジトリの作業コピー> <version>" >&2
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

if [ $# -ne 2 ]; then
    usage
    exit 2
fi

readonly WORK="$1"
readonly VERSION="$2"

[ -d "${WORK}/.git" ] || fail "配信リポジトリの作業コピーではありません: ${WORK}"

# 判定の正は常に remote 側の tag とする。手元の tag は clone / fetch の時点で古くなり、
# remote から削除された tag もローカルには残り続けるため、有無は remote へ直接聞く。
remote_refs="$(git -C "${WORK}" ls-remote --tags origin "refs/tags/${VERSION}")"

if [ -z "${remote_refs}" ]; then
    # remote に無いのに手元に残っている tag は、呼び出し側の `git tag` を重複で失敗させる。
    # remote に合わせて消しておく。
    if git -C "${WORK}" rev-parse -q --verify "refs/tags/${VERSION}" > /dev/null; then
        git -C "${WORK}" tag -d "${VERSION}" > /dev/null
        echo "remote から削除された tag ${VERSION} を手元からも消した" >&2
    fi
    echo "配信リポジトリに tag ${VERSION} は無い" >&2
    echo "absent"
    exit 0
fi

# remote にある tag が指すものを手元へ取り直す (別の場所へ移されていれば上書きする)。
git -C "${WORK}" fetch --quiet --force origin "refs/tags/${VERSION}:refs/tags/${VERSION}"

# スナップショットは未追跡ファイルを含むので、index へ載せてからツリーを取る。
git -C "${WORK}" add -A
snapshot_tree="$(git -C "${WORK}" write-tree)"
tag_tree="$(git -C "${WORK}" rev-parse "refs/tags/${VERSION}^{tree}")"

if [ "${snapshot_tree}" != "${tag_tree}" ]; then
    git -C "${WORK}" diff --cached --stat "refs/tags/${VERSION}" >&2 || true
    fail "配信リポジトリの tag ${VERSION} が今回のスナップショットと異なる内容を指している"
fi

echo "配信リポジトリの tag ${VERSION} は今回のスナップショットと同一" >&2
echo "match"
