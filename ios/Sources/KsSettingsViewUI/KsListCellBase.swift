// KsListCellBase.swift
// KsSettingsViewUI
//
// 全 Cell View（`LabelCellView` / `CommandCellView` / `SwitchCellView` 等 12 種）の共通基底クラス。
// `UICollectionViewListCell` を継承し、以下の共通処理を集約する：
//   - AiForms オリジナル `CellBaseView.cs` SetUpContentView() 準拠の自前 UIStackView 階層を contentView 直下に install
//   - タッチフィードバック（`KsCellViewSupport.installSelectedColorHandler`）の登録
//   - `preferredLayoutAttributesFitting(_:)` の override で `CellStyle.cellHeight` を frame.height に反映
//   - `hintLabel`（右上 float 配置の hint テキスト）の所有とリサイクル管理
//
// 構造:
//   contentView
//     └─ stackH (horizontal, alignment=.center, spacing=16, margins=(6,16,6,16))
//          ├─ iconImageView (required の正方形サイズ制約で固定、Hugging/CCR は両軸とも低い。
//          │                 icon なし時は isHidden=true にしてサイズ制約を無効化する)
//          ├─ stackV (vertical, spacing=4, Hugging=defaultLow, CCR=required)
//          │    ├─ contentStack (horizontal, spacing=6, Hugging=defaultLow, CCR=required)
//          │    │    └─ titleLabel (Hugging=defaultLow, CCR=required)
//          │    │    [render 時に行内 trailing (valueLabel / trailingViews) が addArrangedSubview される]
//          │    └─ descriptionLabel (numberOfLines=0, 空時 isHidden=true)
//          └─ accessoryHolder (Cell 級アクセサリ列。Hugging/CCR=required で自然幅を保ち、空時 isHidden=true)
//             ※ stackV との間隔だけ custom spacing で 6 (contentStack と同じ行内間隔) に詰める
//   self (cell)
//     └─ hintLabel (右上 float, ensureHintLabel() で lazy 生成)

#if canImport(UIKit)
import UIKit

/// 全 Cell View の共通基底。`KsCellRenderer` 準拠の具象 Cell View は本クラスを継承する。
@MainActor
internal class KsListCellBase: UICollectionViewListCell {

    // MARK: - AiForms 準拠の自前 UIStackView 階層

    /// アイコン領域。`KsImage` 由来の画像を表示し、`image == nil` のとき `isHidden = true`。
    internal let iconImageView: UIImageView = {
        let iv = UIImageView()
        iv.contentMode = .scaleAspectFit
        iv.clipsToBounds = true
        iv.isHidden = true
        return iv
    }()

    /// タイトルラベル。`titleLabel.text` を直接更新する。
    internal let titleLabel: UILabel = {
        let label = UILabel()
        label.numberOfLines = 1
        label.lineBreakMode = .byTruncatingTail
        return label
    }()

    /// 説明文ラベル。`text == nil || isEmpty` のとき `isHidden = true`。
    internal let descriptionLabel: UILabel = {
        let label = UILabel()
        label.numberOfLines = 0
        label.lineBreakMode = .byWordWrapping
        label.isHidden = true
        return label
    }()

    /// 右上 float 配置の hint ラベル。`applyCellBaseLayout` 初回呼び出しで lazy 生成。
    internal private(set) var hintLabel: UILabel?

    /// title とその右側に並ぶ subview を組む horizontal stack（AiForms `ContentStack` 相当）。
    /// render 時に派生 Cell の `trailingViews` がここに addArrangedSubview される。
    internal let contentStack: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.alignment = .center
        s.distribution = .fill
        s.spacing = 6
        return s
    }()

    /// 上段に contentStack、下段に descriptionLabel を並べる vertical stack（AiForms `StackV` 相当）。
    internal let stackV: UIStackView = {
        let s = UIStackView()
        s.axis = .vertical
        s.alignment = .fill
        s.distribution = .fill
        s.spacing = 4
        return s
    }()

    /// Cell 級アクセサリ（`UISwitch` / checkbox / checkmark / chevron）を収める列
    /// （AiForms オリジナルの `UITableViewCell.AccessoryView` / `Accessory` 相当）。
    ///
    /// `stackH` の 3 番目の arrangedSubview として `stackV` の後ろに置かれ、`stackH.alignment = .center`
    /// によりセル全体（title + description）に対して垂直センターに配置される。
    /// `contentStack` の内側ではなく独立した列として持つ根拠は ios/ADR-0001。
    /// 内容は常に 0 個または 1 個であり、空のとき `isHidden = true`（アクセサリ用の空領域を残さない）。
    internal let accessoryHolder: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.alignment = .center
        s.distribution = .fill
        s.spacing = 0
        // 空の状態で install されるため初期値は非表示。
        s.isHidden = true
        return s
    }()

    /// 左に iconImageView、中央に stackV、右に accessoryHolder を並べる horizontal stack（AiForms `StackH` 相当）。
    /// contentView 全域に貼られ、layoutMargins で左右上下のマージンを管理する。
    internal let stackH: UIStackView = {
        let s = UIStackView()
        s.axis = .horizontal
        s.alignment = .center
        s.distribution = .fill
        s.spacing = 16
        // AiForms オリジナル: StackH.LayoutMargins = new UIEdgeInsets(6, 16, 6, 16)
        s.layoutMargins = UIEdgeInsets(top: 6, left: 16, bottom: 6, right: 16)
        s.isLayoutMarginsRelativeArrangement = true
        return s
    }()

    /// `stackH` の minHeight 制約（CCR 衝突を避けるため priority 999 で保持）。
    /// 必要に応じて `applyMinHeight(_:)` で更新する。
    private var stackHMinHeightConstraint: NSLayoutConstraint?

    /// `iconImageView` の正方形枠を作るサイズ制約（width / height）。
    /// 表示状態と一辺の長さは `showIcon(size:)` / `hideIcon()` からのみ更新する。
    internal private(set) var iconWidthConstraint: NSLayoutConstraint?
    internal private(set) var iconHeightConstraint: NSLayoutConstraint?

    override init(frame: CGRect) {
        super.init(frame: frame)
        installBaseLayout()
        KsCellViewSupport.installSelectedColorHandler(self)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    // MARK: - 自前 UIStackView 階層の install

    /// AiForms オリジナル `CellBaseView.cs` SetUpContentView() 準拠の自前 stack 階層を組む。
    /// `init(frame:)` で 1 度だけ呼ばれる。
    private func installBaseLayout() {
        // contentStack: [titleLabel]
        contentStack.addArrangedSubview(titleLabel)

        // stackV: [contentStack, descriptionLabel]
        stackV.addArrangedSubview(contentStack)
        stackV.addArrangedSubview(descriptionLabel)

        // stackH: [iconImageView, stackV, accessoryHolder]
        stackH.addArrangedSubview(iconImageView)
        stackH.addArrangedSubview(stackV)
        stackH.addArrangedSubview(accessoryHolder)

        // stackV と accessoryHolder の間隔のみ `contentStack` と同じ行内間隔まで詰める。
        // stackH.spacing（16）は iconImageView と stackV の間隔として残るため、アイコンとの
        // 間隔を変えずに valueText（contentStack の行内 trailing）と Cell 級アクセサリの間隔だけを
        // 行内要素どうしと同じリズムに揃えられる。
        // accessoryHolder が空（isHidden = true）のときは UIStackView が隣接 spacing ごと畳むため、
        // この custom spacing による余白は残らない。
        stackH.setCustomSpacing(contentStack.spacing, after: stackV)

        // Hugging / CCR 優先度
        //   - iconImageView: 両軸とも低い。枠の寸法は required のサイズ制約が決めるため、
        //     画像の intrinsic size（SF Symbols の字形差・任意寸法の UIImage）が枠に影響しない。
        //     非表示のとき UIStackView が張る required の寸法 0 制約とも競合しない。
        //   - accessoryHolder: 縮みも広がりもしない（内容の自然幅を保つ）
        //   - stackV / contentStack / titleLabel / descriptionLabel:
        //       Hugging=低（残り領域を吸って広がる）、CCR=高（縮みにくい）
        iconImageView.setContentHuggingPriority(.defaultLow, for: .horizontal)
        iconImageView.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        iconImageView.setContentHuggingPriority(.defaultLow, for: .vertical)
        iconImageView.setContentCompressionResistancePriority(.defaultLow, for: .vertical)

        stackV.setContentHuggingPriority(.defaultLow, for: .horizontal)
        stackV.setContentCompressionResistancePriority(.required, for: .horizontal)
        stackV.setContentHuggingPriority(.defaultLow, for: .vertical)
        stackV.setContentCompressionResistancePriority(.required, for: .vertical)

        contentStack.setContentHuggingPriority(.defaultLow, for: .horizontal)
        contentStack.setContentCompressionResistancePriority(.required, for: .horizontal)

        titleLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        titleLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

        descriptionLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        descriptionLabel.setContentCompressionResistancePriority(.required, for: .horizontal)

        // accessoryHolder は内容の自然幅を保ち、残り領域は stackV 側へ回す。
        accessoryHolder.setContentHuggingPriority(.required, for: .horizontal)
        accessoryHolder.setContentCompressionResistancePriority(.required, for: .horizontal)
        accessoryHolder.setContentHuggingPriority(.required, for: .vertical)
        accessoryHolder.setContentCompressionResistancePriority(.required, for: .vertical)

        // contentView に stackH を貼る
        stackH.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(stackH)
        NSLayoutConstraint.activate([
            stackH.topAnchor.constraint(equalTo: contentView.topAnchor),
            stackH.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            stackH.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            stackH.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
        ])

        // iconImageView のサイズ制約。優先度は required で、画像の intrinsic size より枠が強い。
        // 初期状態は iconImageView が非表示なので、制約も無効な状態で持つ。
        let width = iconImageView.widthAnchor.constraint(equalToConstant: Theme.defaultCellIconSize)
        let height = iconImageView.heightAnchor.constraint(equalToConstant: Theme.defaultCellIconSize)
        width.priority = .required
        height.priority = .required
        iconWidthConstraint = width
        iconHeightConstraint = height
    }

    // MARK: - icon 領域

    /// icon 領域を表示し、正方形枠の一辺を `size` にする。
    ///
    /// `hideIcon()` と対で icon の表示状態を切り替える唯一の経路であり、`isHidden` とサイズ制約の
    /// 有効・無効を必ず対で動かす。表示にはサイズが必ず要るため、`size` は省略できない入口にしてある
    /// （0pt の枠を無言で作らせないための分離）。
    ///
    /// - Parameter size: 正方形枠の一辺（pt）
    internal func showIcon(size: CGFloat) {
        iconWidthConstraint?.constant = size
        iconHeightConstraint?.constant = size
        iconWidthConstraint?.isActive = true
        iconHeightConstraint?.isActive = true
        iconImageView.isHidden = false
    }

    /// icon 領域を非表示にし、サイズ制約を無効化する。
    ///
    /// 非表示のとき UIStackView は arranged subview へ required の寸法 0 制約を張るため、
    /// required のサイズ制約を有効なままにすると解けない制約の組み合わせになる。
    internal func hideIcon() {
        iconImageView.isHidden = true
        iconWidthConstraint?.isActive = false
        iconHeightConstraint?.isActive = false
    }

    /// `contentStack` の `titleLabel` 以外の arrangedSubviews を除去する。
    /// `applyCellBaseLayout` 呼び出し前に呼び、trailingViews を再追加できる状態にする。
    ///
    /// **注意**: first responder を保持している view（編集中の `UITextField` 等）は除去しない。
    /// `EntryCell` の TwoWay binding 経由で 1 文字入力ごとに `applyDiff(.replaceCell)` → 再 render が
    /// 走ると、`removeFromSuperview` で `UITextField` が first responder を失いキーボードが閉じる現象
    /// （Cell 描画リフレッシュとキーボード状態のレース）を回避する。
    internal func clearContentStackTrailingViews() {
        for view in contentStack.arrangedSubviews where view !== titleLabel {
            // first responder を保持中の view は外さない（編集中の UITextField を保護）。
            if view.isFirstResponderInSubtree() {
                continue
            }
            contentStack.removeArrangedSubview(view)
            view.removeFromSuperview()
        }
    }

    // MARK: - accessoryHolder

    /// `accessoryHolder` の内容を `view`（`nil` で空）に置換する。
    ///
    /// 旧内容は必ず除去するため、再 render（reconfigure）を繰り返しても `accessoryHolder` の内容は
    /// 常に 0 個または 1 個で、アクセサリが蓄積しない。
    /// 同一インスタンスが再指定された場合は付け替えずそのまま残す（`UISwitch` 等の常設アクセサリで
    /// 不要な view 階層操作とアニメーション中断を避けるため）。
    ///
    /// - Parameter view: 配置する Cell 級アクセサリ。`nil` のとき holder を空にして `isHidden = true` にする。
    internal func setAccessoryView(_ view: UIView?) {
        // 旧内容の除去（再指定された同一インスタンスは残す）
        for old in accessoryHolder.arrangedSubviews where old !== view {
            accessoryHolder.removeArrangedSubview(old)
            old.removeFromSuperview()
        }
        guard let view = view else {
            // nil のときはアクセサリ用の空領域を残さない
            accessoryHolder.isHidden = true
            return
        }
        if !accessoryHolder.arrangedSubviews.contains(where: { $0 === view }) {
            accessoryHolder.addArrangedSubview(view)
        }
        accessoryHolder.isHidden = false
    }

    // MARK: - hintLabel

    /// `hintLabel` を lazy に生成し、cell 直下に AutoLayout 制約付きで `addSubview` する。
    ///
    /// AiForms オリジナル `CellBaseView.cs` SetUpHintLabel() 準拠：
    ///   - `top == cell.topAnchor + 2`
    ///   - `leading == cell.leadingAnchor + 16`（オリジナル `LeftAnchor=16`）
    ///   - `trailing == cell.trailingAnchor - 10`
    ///   - `bottom <= cell.bottomAnchor - 12`
    ///
    /// `trailingAnchor` の参照は `cell.contentView.trailingAnchor` ではなく **`cell.trailingAnchor`** を使う
    /// （accessory 有無に関わらず cell 右端基準で float、AiForms オリジナル準拠）。
    @discardableResult
    internal func ensureHintLabel() -> UILabel {
        if let existing = hintLabel { return existing }
        let label = UILabel()
        label.translatesAutoresizingMaskIntoConstraints = false
        label.textAlignment = .right
        label.numberOfLines = 1
        label.lineBreakMode = .byTruncatingTail
        label.adjustsFontSizeToFitWidth = true
        label.baselineAdjustment = .alignCenters
        label.isHidden = true
        // contentView ではなく cell 直下に addSubview することで、accessory レイアウトと
        // 独立した右上 float 配置を実現する（オリジナル CellBaseView.cs の HintLabel と同様）。
        self.addSubview(label)
        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: self.topAnchor, constant: 2),
            label.leadingAnchor.constraint(greaterThanOrEqualTo: self.leadingAnchor, constant: 16),
            // `self.trailingAnchor`（cell 自身の右端）を基準にすることで、accessory の
            // 有無に関わらず hintLabel を右上 float 配置できる。
            label.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -10),
            label.bottomAnchor.constraint(lessThanOrEqualTo: self.bottomAnchor, constant: -12),
        ])
        // 常に accessoryHolder 相当（contentView の各種 trailing accessory）より前面に来るよう最前面に持ち上げる。
        self.bringSubviewToFront(label)
        self.hintLabel = label
        return label
    }

    // MARK: - Section の箱への clip

    /// Modern の箱に収めるための mask を bounds の変化に追従させる。
    /// self-sizing で行高さが後から確定するため、clip の形状はここで作り直す。
    override func layoutSubviews() {
        super.layoutSubviews()
        KsCellViewSupport.updateSectionBoxClipMask(self)
    }

    // MARK: - preferredLayoutAttributesFitting

    /// Compositional Layout の self-sizing で proposed attributes が返ってきたタイミングで、
    /// `KsCellViewSupport.applyEffectiveHeight` が記録した実効 cellHeight を反映する。
    override func preferredLayoutAttributesFitting(
        _ layoutAttributes: UICollectionViewLayoutAttributes
    ) -> UICollectionViewLayoutAttributes {
        let base = super.preferredLayoutAttributesFitting(layoutAttributes)
        return KsCellViewSupport.adjustedLayoutAttributes(self, proposed: base)
    }

    // MARK: - prepareForReuse

    /// セルリサイクル時に subview 状態をリセットする。
    /// subview の **構造（arrangedSubviews 配列の構成要素）は破壊せず**、
    /// 各 `UILabel.text` / `UIImageView.image` を nil クリアし、`contentStack` から
    /// titleLabel 以外の行内 trailing を、`accessoryHolder` から Cell 級アクセサリを除去する。
    override func prepareForReuse() {
        super.prepareForReuse()
        // 共通 subview のリセット
        titleLabel.text = nil
        descriptionLabel.text = nil
        descriptionLabel.isHidden = true
        iconImageView.image = nil
        iconImageView.layer.cornerRadius = 0
        hideIcon()
        // 行内 trailing 除去（titleLabel は残す）
        clearContentStackTrailingViews()
        // Cell 級アクセサリ除去（holder は恒常メンバーとして残す）
        setAccessoryView(nil)
        // hint label のリセット
        hintLabel?.text = nil
        hintLabel?.isHidden = true
    }
}

// MARK: - UIView: first responder 探索ヘルパ

@MainActor
fileprivate extension UIView {
    /// 自身またはその subview 階層内に first responder が存在するか判定する。
    func isFirstResponderInSubtree() -> Bool {
        if self.isFirstResponder { return true }
        for sv in subviews {
            if sv.isFirstResponderInSubtree() { return true }
        }
        return false
    }
}
#endif
