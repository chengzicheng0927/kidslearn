import AVFoundation
import Foundation
import Speech

final class SpeechManager {
    static let shared = SpeechManager()

    private let audioEngine = AVAudioEngine()
    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var isSilentStop = false  // 静默停止标志

    private init() {}

    func start(
        language: String,
        onResult: @escaping (_ text: String, _ isFinal: Bool) -> Void,
        onError: @escaping (Error) -> Void
    ) {
        isSilentStop = false
        stop()

        let locale = Locale(identifier: language)
        speechRecognizer = SFSpeechRecognizer(locale: locale)

        guard let speechRecognizer, speechRecognizer.isAvailable else {
            onError(NSError(domain: "SpeechManager", code: -1, userInfo: [
                NSLocalizedDescriptionKey: "语音识别当前不可用"
            ]))
            return
        }

        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            guard let self else { return }

            switch status {
            case .authorized:
                DispatchQueue.main.async {
                    self.startAudioSession(speechRecognizer: speechRecognizer, onResult: onResult, onError: onError)
                }
            case .denied, .restricted, .notDetermined:
                onError(NSError(domain: "SpeechManager", code: -2, userInfo: [
                    NSLocalizedDescriptionKey: "请在系统设置中开启语音识别权限"
                ]))
            @unknown default:
                onError(NSError(domain: "SpeechManager", code: -3, userInfo: [
                    NSLocalizedDescriptionKey: "未知的语音识别权限状态"
                ]))
            }
        }
    }

    private func startAudioSession(
        speechRecognizer: SFSpeechRecognizer,
        onResult: @escaping (_ text: String, _ isFinal: Bool) -> Void,
        onError: @escaping (Error) -> Void
    ) {
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.record, mode: .measurement, options: [.duckOthers])
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

            let request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            recognitionRequest = request

            recognitionTask = speechRecognizer.recognitionTask(with: request) { [weak self] result, error in
                if let result {
                    onResult(result.bestTranscription.formattedString, result.isFinal)
                    if result.isFinal {
                        self?.stop()
                    }
                }

                if let error {
                    if self?.isSilentStop == true {
                        // 静默停止，不发送错误
                        self?.stop()
                    } else {
                        self?.stop()
                        onError(error)
                    }
                }
            }

            let inputNode = audioEngine.inputNode
            let recordingFormat = inputNode.outputFormat(forBus: 0)
            inputNode.removeTap(onBus: 0)
            inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { [weak self] buffer, _ in
                self?.recognitionRequest?.append(buffer)
            }

            audioEngine.prepare()
            try audioEngine.start()
        } catch {
            onError(error)
        }
    }

    func stop() {
        isSilentStop = true
        if audioEngine.isRunning {
            audioEngine.stop()
            audioEngine.inputNode.removeTap(onBus: 0)
        }

        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionTask = nil
        recognitionRequest = nil

        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            // no-op
        }
    }

    func finish() {
        if audioEngine.isRunning {
            audioEngine.stop()
            audioEngine.inputNode.removeTap(onBus: 0)
        }

        // 只结束音频输入，不取消识别任务，让它完成识别
        recognitionRequest?.endAudio()
        recognitionTask = nil
        recognitionRequest = nil

        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            // no-op
        }
    }
}
