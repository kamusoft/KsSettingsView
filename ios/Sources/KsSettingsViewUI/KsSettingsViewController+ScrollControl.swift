// KsSettingsViewController+ScrollControl.swift
// KsSettingsViewUI
//
// スクロール命令の受け口・待ち行列・位置の解決と、表示位置を控える・戻す窓口。
//
// 命令は Store を経由せず、Host につないだハンドルから届く (core/ADR-0037)。受けた処理の中では
// 実行せず、`main.async` で 1 回遅らせたうえで、進行中の snapshot 適用がすべて完了し、表示の寸法が
// 決まってから実行する。これにより「Store へ項目を追加 → 直後に末尾へ」を同じ処理で書ける。

#if canImport(UIKit)
import UIKit
import os
import KsSettingsViewCore

// MARK: - 命令の受け口

extension KsSettingsViewController: KsScrollCommandReceiver {
    package func receive(_ command: KsScrollCommand) {
        enqueueScrollEntry(.command(command))
    }
}

// MARK: - 位置を控える・戻す窓口

extension KsSettingsViewController {

    /// 現在の表示位置を控えます。
    ///
    /// 表示範囲の上端にかかる最初の要素 (Cell の行・Section の見出しと Footer・Root Header と
    /// Root Footer) と、上端からのずれを控えます。控えた値を同じ内容を表示する画面の
    /// `restoreScrollAnchor(_:)` へ渡すと、その位置へ戻ります。
    ///
    /// `restoreScrollAnchor(_:)` で渡した位置へまだ戻していないときは、現在の表示位置ではなく
    /// その渡した位置を返します。
    ///
    /// 画面が window に取り付けられていないときは、現在の表示位置を控えません (外れた後のレイアウトは
    /// 利用者が見ていた位置と一致しないため)。画面を閉じる前の位置が要るときは、window から外れる前に
    /// 控えてください。まだ戻していない位置がある場合は、外れていてもその位置を返します。
    ///
    /// - Returns: 控えた位置。画面が読み込まれていない・window に取り付けられていない・寸法が
    ///   決まっていない・表示する要素が無いなど、控えられる内容が無いときは `nil`
    public func captureScrollAnchor() -> KsScrollAnchor? {
        // 控えた位置の復元がまだ実行されていないときは、その控えを返す (戻し切る前に再び控えても
        // 位置を失わないため)。window の有無より先に見る。
        if let pending = pendingRestoreAnchor() {
            return pending
        }
        // window から外れた後のレイアウトは window の寸法と安全領域を失っており (adjustedContentInset が
        // 0 になり、内容の高さも変わりうる)、そこから選んだ上端の要素は見ていた位置と一致しない。
        guard isViewLoaded, let cv = collectionView, internalDataSource != nil,
              cv.window != nil, cv.bounds.height > 0 else {
            return nil
        }
        cv.layoutIfNeeded()
        let inset = cv.adjustedContentInset
        let topY = cv.contentOffset.y + inset.top
        let visibleHeight = max(1, cv.bounds.height - inset.top - inset.bottom)
        let rect = CGRect(x: cv.bounds.minX, y: topY, width: max(1, cv.bounds.width), height: visibleHeight)
        let attributes = cv.collectionViewLayout.layoutAttributesForElements(in: rect) ?? []

        var best: (element: KsScrollAnchor.Element, minY: CGFloat)?
        for attribute in attributes where !attribute.isHidden && attribute.frame.maxY > topY {
            guard let element = anchorElement(for: attribute) else { continue }
            if let current = best, current.minY <= attribute.frame.minY { continue }
            best = (element, attribute.frame.minY)
        }
        guard let best else { return nil }
        return KsScrollAnchor(element: best.element, offsetFromTop: topY - best.minY)
    }

    /// 控えた表示位置へ戻します。
    ///
    /// スクロール命令と同じく、データの反映と画面のレイアウトの後に、控えた要素が控えたときと同じ
    /// ずれで表示範囲の上端にかかる位置へ戻します。これより後に出したスクロール命令は、戻した後に
    /// 実行されます。控えた要素が見つからなければ何もしません。
    ///
    /// - Parameter anchor: `captureScrollAnchor()` で控えた位置
    public func restoreScrollAnchor(_ anchor: KsScrollAnchor) {
        enqueueScrollEntry(.restore(anchor))
    }

    /// 未実行の位置の復元のうち最後のもの。
    private func pendingRestoreAnchor() -> KsScrollAnchor? {
        for entry in (readyScrollEntries + incomingScrollEntries).reversed() {
            if case .restore(let anchor) = entry { return anchor }
        }
        return nil
    }

    /// レイアウト属性を、控える要素へ対応づける。控える対象でない要素 (装飾など) は `nil`。
    private func anchorElement(for attribute: UICollectionViewLayoutAttributes) -> KsScrollAnchor.Element? {
        switch attribute.representedElementCategory {
        case .cell:
            guard let itemID = internalDataSource?.itemIdentifier(for: attribute.indexPath) else { return nil }
            return .cell(itemID.id)
        case .supplementaryView:
            switch attribute.representedElementKind {
            case UICollectionView.elementKindSectionHeader:
                return sectionID(atVisibleIndex: attribute.indexPath.section).map { .sectionHeader($0) }
            case UICollectionView.elementKindSectionFooter:
                return sectionID(atVisibleIndex: attribute.indexPath.section).map { .sectionFooter($0) }
            case Self.rootHeaderElementKind:
                return .rootHeader
            case Self.rootFooterElementKind:
                return .rootFooter
            default:
                return nil
            }
        default:
            return nil
        }
    }

    private func sectionID(atVisibleIndex index: Int) -> UUID? {
        guard index >= 0, index < visibleSections.count else { return nil }
        return visibleSections[index].id
    }
}

// MARK: - 待ち行列

extension KsSettingsViewController {

    private static let scrollLogger = Logger(subsystem: "jp.kamusoft.kssettingsview", category: "KsScrollControl")

    /// 命令を待ち行列に積み、1 回遅らせてから実行を試みる。
    ///
    /// 遅延は、同じ処理の中で後から行われるデータの変更 (Store の更新) を命令の前に反映させるため。
    /// 同じ実行の中で積まれた命令は、1 回の遅延をまとめて経る。
    internal func enqueueScrollEntry(_ entry: KsScrollQueueEntry) {
        incomingScrollEntries.append(entry)
        guard !isScrollDeferralScheduled else { return }
        isScrollDeferralScheduled = true
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.isScrollDeferralScheduled = false
            self.readyScrollEntries.append(contentsOf: self.incomingScrollEntries)
            self.incomingScrollEntries.removeAll()
            self.flushScrollEntriesIfPossible()
        }
    }

    /// ハンドルから届いた未実行の命令を捨てる。位置の復元は残す。
    ///
    /// 宣言 UI のラッパーが、ID を引き直す受け口に接続したハンドルを差し替えた・外したときにも使う。
    package func dropPendingScrollCommands() {
        func isRestore(_ entry: KsScrollQueueEntry) -> Bool {
            if case .restore = entry { return true }
            return false
        }
        incomingScrollEntries.removeAll { !isRestore($0) }
        readyScrollEntries.removeAll { !isRestore($0) }
    }

    /// 命令を実行できる状態か。
    ///
    /// 最初の snapshot 適用が済み、進行中の適用が無く、画面に取り付けられて寸法が決まっていること。
    /// 画面から外れている間 (上に別の画面を重ねた遷移など) に受けた命令は、取り付け直した後の
    /// レイアウト (`viewDidLayoutSubviews`) で実行する。
    private var canExecuteScrollEntries: Bool {
        guard isViewLoaded,
              let cv = collectionView,
              internalDataSource != nil,
              hasAppliedInitialSnapshot,
              applyingSnapshotCount == 0,
              cv.window != nil,
              cv.bounds.height > 0 else {
            return false
        }
        return true
    }

    /// 遅延を経た命令を、実行できる状態なら呼ばれた順にすべて実行する。
    ///
    /// - Returns: 命令を 1 件以上実行したら `true`
    @discardableResult
    internal func flushScrollEntriesIfPossible() -> Bool {
        guard !readyScrollEntries.isEmpty, canExecuteScrollEntries else { return false }
        collectionView.layoutIfNeeded()
        let entries = readyScrollEntries
        readyScrollEntries.removeAll()
        lastSettledScroll = nil
        for entry in entries {
            executeScrollEntry(entry)
        }
        processedScrollCommandCount += entries.count
        return true
    }

    /// 現在のレイアウトを終えた後で、Host の view の祖先すべてにレイアウトをもう一度求める。
    ///
    /// 再レイアウトで表示範囲の寸法が変わると、命令で合わせた位置が新しい表示範囲とずれる
    /// (上端の要素が同じでも、中央・下端に合わせた対象は表示範囲の中央・下端から外れる)。また一覧は
    /// 表示位置を自分で動かすこともある。命令で決めた位置を保つため、再レイアウトの後に位置を合わせ直す。
    /// 最後に位置を決めたのが Cell・Section・先頭・末尾への命令なら、その行き先と合わせる位置を新しい
    /// 表示範囲で解き直す。控えた位置の戻しなら、再レイアウトの前に控えた上端の要素とずれへ戻す。
    /// 同じ実行の中で何度呼ばれても、要求は 1 回にまとめる。
    internal func scheduleAncestorRelayout() {
        guard !isAncestorRelayoutScheduled else { return }
        isAncestorRelayoutScheduled = true
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.isAncestorRelayoutScheduled = false
            guard self.isViewLoaded, let window = self.view.window else { return }
            let anchor = self.captureScrollAnchor()
            var ancestor = self.view.superview
            while let current = ancestor {
                current.setNeedsLayout()
                ancestor = current.superview
            }
            window.layoutIfNeeded()
            // 再レイアウトの後に届いた命令が位置を決めているときは、それを優先する。命令のアニメーションが
            // 進行中のときは、アニメーションが毎フレーム最新のレイアウトで行き先を求め直すため、途中で
            // 打ち切らない。
            guard self.readyScrollEntries.isEmpty, self.incomingScrollEntries.isEmpty,
                  self.activeScrollAnimation == nil else { return }
            switch self.lastSettledScroll {
            case let (target, position)? where !target.isAnchor:
                // 命令の行き先を、新しい表示範囲で解き直す。
                self.performScroll(to: target, position: position, animated: false)
            default:
                // 控えた位置の戻し (または行き先が求まらなかった実行) は、上端の要素とずれを保つ。
                guard let anchor, self.captureScrollAnchor() != anchor else { return }
                self.performScroll(to: .anchor(anchor), position: .start, animated: false)
            }
        }
    }

    /// 1 件の命令を実行する。実行の直前に最新の表示で対象を解決し、無ければ何もしない。
    ///
    /// 対象が無い命令は、先行のアニメーションも止めない (何もしない命令が進行中のアニメーションの
    /// 行き先を変えないため)。先行のアニメーションは、行き先が求まった時点で `performScroll` が止める。
    private func executeScrollEntry(_ entry: KsScrollQueueEntry) {
        switch entry {
        case let .command(.cell(id, position, animated)):
            guard let cellID = resolveCellID(id) else { return }
            performScroll(to: .cell(cellID), position: position, animated: animated)
        case let .command(.section(id, position, animated)):
            guard let sectionID = resolveSectionID(id) else { return }
            performScroll(to: .section(sectionID), position: position, animated: animated)
        case let .command(.start(animated)):
            performScroll(to: .contentStart, position: .start, animated: animated)
        case let .command(.end(animated)):
            performScroll(to: .contentEnd, position: .end, animated: animated)
        case let .restore(anchor):
            performScroll(to: .anchor(anchor), position: .start, animated: false)
        }
    }

    /// 命令の ID を、表示中の Cell の ID に解決する。非表示の Cell は黙って、存在しない ID は
    /// debug ビルドで警告を出して `nil` を返す。
    private func resolveCellID(_ id: AnyHashable) -> KsCellID? {
        let cellID: KsCellID?
        if let value = id.base as? KsCellID {
            cellID = value
        } else if let value = id.base as? UUID {
            cellID = KsCellID(id: value)
        } else {
            cellID = nil
        }
        guard let cellID, modelContainsCell(cellID) else {
            warnUnknownScrollTarget(kind: "cell", id: id)
            return nil
        }
        guard internalDataSource?.indexPath(for: cellID) != nil else { return nil }
        return cellID
    }

    /// 命令の ID を、表示中の Section の ID に解決する。規則は `resolveCellID(_:)` と同じ。
    private func resolveSectionID(_ id: AnyHashable) -> UUID? {
        guard let sectionID = id.base as? UUID, modelContainsSection(sectionID) else {
            warnUnknownScrollTarget(kind: "section", id: id)
            return nil
        }
        guard internalDataSource?.snapshot().indexOfSection(sectionID) != nil else { return nil }
        return sectionID
    }

    private func warnUnknownScrollTarget(kind: String, id: AnyHashable) {
        #if DEBUG
        let description = String(describing: id.base)
        Self.scrollLogger.warning(
            "KsSettingsViewController: ignored a scroll command to an unknown \(kind, privacy: .public) id \(description, privacy: .public)"
        )
        #endif
    }
}

// MARK: - 送り方

extension KsSettingsViewController {

    /// 命令によるアニメーション付きスクロールの長さ (秒)。
    internal static let scrollAnimationDuration: CFTimeInterval = 0.4

    /// 行き先へ送る。行き先の要素が表示に無ければ何もしない。
    ///
    /// アニメーションするときは、画面の更新ごとに行き先を最新のレイアウトで求め直しながら進める
    /// (`stepScrollAnimation(at:)`)。アニメーションしないときは、送った先でレイアウトを確定させ、
    /// 確定した位置へ送り直して詰める。
    private func performScroll(to target: KsScrollTarget, position: KsScrollPosition, animated: Bool) {
        guard let cv = collectionView else { return }
        cv.layoutIfNeeded()
        guard let destination = targetOffsetY(for: target, position: position) else { return }
        // 行き先が求まったときだけ、先行のアニメーションを止めてから送る。最終位置は最後の命令のものになる。
        stopActiveScrollAnimation()
        lastSettledScroll = (target, position)
        let current = cv.contentOffset.y
        if animated, abs(destination - current) >= 0.5 {
            activeScrollAnimation = KsActiveScrollAnimation(
                target: target,
                position: position,
                startOffset: current,
                direction: destination > current ? 1 : -1,
                duration: Self.scrollAnimationDuration,
                startTime: nil,
                destination: destination
            )
            if scrollAnimationTicker == nil {
                scrollAnimationTicker = KsScrollAnimationTicker(owner: self)
            }
            scrollAnimationTicker?.start()
            return
        }
        cv.setContentOffset(CGPoint(x: cv.contentOffset.x, y: destination), animated: false)
        settleScroll(to: target, position: position)
    }

    /// 送った先でレイアウトを確定させ、確定した位置へ送り直して詰める。
    ///
    /// 推定高さの行・見出しが表示範囲に入ると実測の高さへ変わり、行き先の位置もずれる。
    private func settleScroll(to target: KsScrollTarget, position: KsScrollPosition) {
        guard let cv = collectionView else { return }
        for _ in 0..<3 {
            cv.layoutIfNeeded()
            guard let next = targetOffsetY(for: target, position: position) else { return }
            guard abs(next - cv.contentOffset.y) >= 0.5 else { return }
            cv.setContentOffset(CGPoint(x: cv.contentOffset.x, y: next), animated: false)
        }
    }

    /// 命令によるアニメーション付きスクロールを、指定時刻のフレームまで進める。
    ///
    /// 毎フレーム行き先を最新のレイアウトで求め直し、出発点からその行き先までを進み具合で補間した
    /// 位置へ送る。行き先はスクロール中に動く (推定より低い行が実測で確定して内容が短くなる・大きい
    /// タイトルの縮小で表示範囲が伸びてスクロール可能範囲が縮む) ため、命令の時点の位置へ一度に送ると
    /// スクロール可能範囲の端を越えて止まる。
    ///
    /// 送る位置は進んでいる向きへだけ動かし、行き先は越えない。行き先が現在位置より手前へ移ったとき
    /// だけは、スクロール可能範囲に収めるため行き先へ置く。時間が尽きたら行き先に置いて詰め、終える。
    ///
    /// - Parameter time: フレームの時刻 (`CACurrentMediaTime()` と同じ時計)
    internal func stepScrollAnimation(at time: CFTimeInterval) {
        guard var active = activeScrollAnimation, let cv = collectionView else {
            scrollAnimationTicker?.stop()
            return
        }
        let startTime = active.startTime ?? time
        active.startTime = startTime
        cv.layoutIfNeeded()
        guard let destination = targetOffsetY(for: active.target, position: active.position) else {
            // 行き先の要素が表示から消えた。その場で止める。
            stopActiveScrollAnimation()
            return
        }
        active.destination = destination
        let progress = active.duration > 0 ? (time - startTime) / active.duration : 1
        let current = cv.contentOffset.y
        var next = active.startOffset
            + (destination - active.startOffset) * CGFloat(KsActiveScrollAnimation.eased(progress))
        // 進んでいる向きへだけ動かす。
        if (next - current) * active.direction < 0 {
            next = current
        }
        // 行き先は越えない (行き先が現在位置より手前へ移っていれば行き先へ置く)。
        if (next - destination) * active.direction > 0 {
            next = destination
        }
        let finished = progress >= 1
        if finished {
            next = destination
        }
        if abs(next - current) >= 0.01 {
            cv.setContentOffset(CGPoint(x: cv.contentOffset.x, y: next), animated: false)
        }
        guard finished else {
            activeScrollAnimation = active
            return
        }
        activeScrollAnimation = nil
        scrollAnimationTicker?.stop()
        settleScroll(to: active.target, position: active.position)
    }

    /// 進行中の命令のアニメーションを、その時点の位置で止める。
    internal func stopActiveScrollAnimation() {
        scrollAnimationTicker?.stop()
        activeScrollAnimation = nil
    }

    /// 行き先に対応するスクロール位置 (contentOffset.y) を、現在のレイアウトから求める。
    ///
    /// 表示範囲は安全領域と内容の余白 (`adjustedContentInset`) を除いた領域とする。到達できない位置は
    /// スクロール可能範囲の端で止め、表示範囲より高い対象は上端に合わせる。
    /// - Returns: 行き先の要素が表示に無ければ `nil`
    private func targetOffsetY(for target: KsScrollTarget, position: KsScrollPosition) -> CGFloat? {
        guard let cv = collectionView else { return nil }
        let inset = cv.adjustedContentInset
        let minOffset = -inset.top
        let contentHeight = cv.collectionViewLayout.collectionViewContentSize.height
        let maxOffset = max(minOffset, contentHeight + inset.bottom - cv.bounds.height)
        let visibleHeight = cv.bounds.height - inset.top - inset.bottom

        let raw: CGFloat
        switch target {
        case .contentStart:
            raw = minOffset
        case .contentEnd:
            raw = maxOffset
        case .cell(let cellID):
            guard let frame = cellFrame(cellID) else { return nil }
            raw = offsetY(aligning: frame, position: position, topInset: inset.top, visibleHeight: visibleHeight)
        case .section(let sectionID):
            guard let frame = sectionRangeFrame(sectionID) else { return nil }
            raw = offsetY(aligning: frame, position: position, topInset: inset.top, visibleHeight: visibleHeight)
        case .anchor(let anchor):
            guard let frame = anchorFrame(anchor.element) else { return nil }
            raw = frame.minY + anchor.offsetFromTop - inset.top
        }
        return min(max(raw, minOffset), maxOffset)
    }

    /// 対象の範囲を表示範囲の指定位置へ合わせるスクロール位置。表示範囲より高い対象は上端に合わせる。
    private func offsetY(
        aligning frame: CGRect,
        position: KsScrollPosition,
        topInset: CGFloat,
        visibleHeight: CGFloat
    ) -> CGFloat {
        if frame.height > visibleHeight {
            return frame.minY - topInset
        }
        switch position {
        case .start:
            return frame.minY - topInset
        case .center:
            return frame.midY - topInset - visibleHeight / 2
        case .end:
            return frame.maxY - topInset - visibleHeight
        }
    }

    private func cellFrame(_ cellID: KsCellID) -> CGRect? {
        guard let indexPath = internalDataSource?.indexPath(for: cellID) else { return nil }
        return collectionView.collectionViewLayout.layoutAttributesForItem(at: indexPath)?.frame
    }

    /// Section の範囲。表示されている見出し・Cell・Footer をつないだもの。
    ///
    /// 上端は見出し、無ければ最初の表示 Cell、それも無ければ Footer の上端。下端は Footer、
    /// 無ければ最後の表示 Cell、それも無ければ見出しの下端。表示される要素が 1 つも無ければ `nil`。
    private func sectionRangeFrame(_ sectionID: UUID) -> CGRect? {
        guard let dataSource = internalDataSource,
              let sectionIndex = dataSource.snapshot().indexOfSection(sectionID),
              sectionIndex < visibleSections.count else {
            return nil
        }
        let section = visibleSections[sectionIndex]
        let layout = collectionView.collectionViewLayout
        let accessoryIndexPath = IndexPath(item: 0, section: sectionIndex)

        var frames: [CGRect] = []
        if Self.shouldShowHeader(for: section),
           let header = layout.layoutAttributesForSupplementaryView(
               ofKind: UICollectionView.elementKindSectionHeader,
               at: accessoryIndexPath
           ) {
            frames.append(header.frame)
        }
        let itemCount = dataSource.snapshot().numberOfItems(inSection: sectionID)
        if itemCount > 0 {
            if let first = layout.layoutAttributesForItem(at: IndexPath(item: 0, section: sectionIndex)) {
                frames.append(first.frame)
            }
            if let last = layout.layoutAttributesForItem(at: IndexPath(item: itemCount - 1, section: sectionIndex)) {
                frames.append(last.frame)
            }
        }
        if Self.shouldShowFooter(for: section),
           let footer = layout.layoutAttributesForSupplementaryView(
               ofKind: UICollectionView.elementKindSectionFooter,
               at: accessoryIndexPath
           ) {
            frames.append(footer.frame)
        }
        guard let firstFrame = frames.first else { return nil }
        return frames.dropFirst().reduce(firstFrame) { $0.union($1) }
    }

    /// Root Header / Footer (layout 全体の boundary supplementary) のレイアウト属性の indexPath。
    /// Section に属さないため、Section 側の `[section, item]` ではなく長さ 1 の `[0]` で置かれる。
    private static let rootAccessoryIndexPath = IndexPath(index: 0)

    /// 控えた要素の現在の frame。要素が表示に無ければ `nil`。
    private func anchorFrame(_ element: KsScrollAnchor.Element) -> CGRect? {
        let layout = collectionView.collectionViewLayout
        switch element {
        case .cell(let id):
            return cellFrame(KsCellID(id: id))
        case .sectionHeader(let sectionID):
            guard let index = internalDataSource?.snapshot().indexOfSection(sectionID),
                  index < visibleSections.count,
                  Self.shouldShowHeader(for: visibleSections[index]) else { return nil }
            return layout.layoutAttributesForSupplementaryView(
                ofKind: UICollectionView.elementKindSectionHeader,
                at: IndexPath(item: 0, section: index)
            )?.frame
        case .sectionFooter(let sectionID):
            guard let index = internalDataSource?.snapshot().indexOfSection(sectionID),
                  index < visibleSections.count,
                  Self.shouldShowFooter(for: visibleSections[index]) else { return nil }
            return layout.layoutAttributesForSupplementaryView(
                ofKind: UICollectionView.elementKindSectionFooter,
                at: IndexPath(item: 0, section: index)
            )?.frame
        case .rootHeader:
            guard rootHeader != nil else { return nil }
            return layout.layoutAttributesForSupplementaryView(
                ofKind: Self.rootHeaderElementKind,
                at: Self.rootAccessoryIndexPath
            )?.frame
        case .rootFooter:
            guard rootFooter != nil else { return nil }
            return layout.layoutAttributesForSupplementaryView(
                ofKind: Self.rootFooterElementKind,
                at: Self.rootAccessoryIndexPath
            )?.frame
        }
    }
}
#endif
