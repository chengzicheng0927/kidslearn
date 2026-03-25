import UIKit

enum LottieOverlay {
    static func show(name: String, in containerView: UIView) {
        let overlay = UILabel()
        overlay.text = "Animation: \(name)"
        overlay.textAlignment = .center
        overlay.textColor = .white
        overlay.backgroundColor = UIColor.black.withAlphaComponent(0.65)
        overlay.layer.cornerRadius = 16
        overlay.clipsToBounds = true
        overlay.frame = CGRect(
            x: 24,
            y: containerView.bounds.height * 0.25,
            width: containerView.bounds.width - 48,
            height: 72
        )
        overlay.alpha = 0
        overlay.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
        containerView.addSubview(overlay)

        UIView.animate(withDuration: 0.2, animations: {
            overlay.alpha = 1
        }) { _ in
            UIView.animate(withDuration: 0.2, delay: 1.2, options: [], animations: {
                overlay.alpha = 0
            }) { _ in
                overlay.removeFromSuperview()
            }
        }
    }
}
