import UIKit

enum LockScreenOverlay {
    private static let overlayTag = 42001

    static func setLocked(_ locked: Bool, in containerView: UIView) {
        if locked {
            if containerView.viewWithTag(overlayTag) != nil { return }

            let overlay = UIView(frame: containerView.bounds)
            overlay.tag = overlayTag
            overlay.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            overlay.backgroundColor = UIColor.black.withAlphaComponent(0.55)

            let label = UILabel()
            label.text = "当前处于勿扰时间"
            label.textColor = .white
            label.font = .systemFont(ofSize: 22, weight: .bold)
            label.textAlignment = .center
            label.frame = CGRect(x: 24, y: containerView.bounds.midY - 30, width: containerView.bounds.width - 48, height: 60)
            label.autoresizingMask = [.flexibleWidth, .flexibleTopMargin, .flexibleBottomMargin]

            overlay.addSubview(label)
            containerView.addSubview(overlay)
        } else {
            containerView.viewWithTag(overlayTag)?.removeFromSuperview()
        }
    }
}
