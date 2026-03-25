<script setup lang="ts">
</script>

<template>
  <Teleport to="body">
    <div class="overlay">
      <div class="sheet">
        <div class="handle"></div>

        <div class="s-think">
          <div class="t-label">批改中，请稍候…</div>
          <div class="teacher-wrap">
            <div class="scan"></div>

            <!-- Teacher SVG -->
            <svg viewBox="60 55 80 120" width="160" height="160" xmlns="http://www.w3.org/2000/svg" style="overflow:visible;display:block;margin:0 auto">
              <ellipse cx="100" cy="175" rx="32" ry="18" fill="#FF8A65" opacity=".5"/>
              <rect x="74" y="148" width="52" height="34" rx="14" fill="#FF7043"/>
              <polygon points="100,152 104,162 100,172 96,162" fill="#FFF3EF" opacity=".8"/>
              <ellipse cx="100" cy="112" rx="28" ry="30" fill="#FFCCBC"/>
              <ellipse cx="100" cy="112" rx="28" ry="30" fill="none" stroke="#FF8A65" stroke-width="1.5" opacity=".4"/>
              <rect x="76" y="62" width="48" height="28" rx="6" fill="#FF7043"/>
              <rect x="68" y="86" width="64" height="8" rx="4" fill="#FF8A65"/>
              <path d="M100 69L102.4 75.2L109 75.2L103.7 79L105.8 85L100 81.2L94.2 85L96.3 79L91 75.2L97.6 75.2L100 69Z" fill="#FFD54F"/>
              <path d="M100 71.8L101.4 75.4L105.3 75.4L102.2 77.7L103.4 81.4L100 79.1L96.6 81.4L97.8 77.7L94.7 75.4L98.6 75.4L100 71.8Z" fill="#FFF7C2"/>
              <ellipse cx="72" cy="112" rx="7" ry="9" fill="#FFCCBC" stroke="#FF8A65" stroke-width="1"/>
              <ellipse cx="128" cy="112" rx="7" ry="9" fill="#FFCCBC" stroke="#FF8A65" stroke-width="1"/>

              <!-- Think eyes -->
              <g class="eye-blink">
                <ellipse cx="88" cy="108" rx="9" ry="9.5" fill="#fff" stroke="#FF7043" stroke-width="1.5"/>
                <ellipse cx="88" cy="108" rx="4.5" ry="4.5" fill="#5D4037" class="pupil-look"/>
                <circle cx="90" cy="106" r="1.8" fill="#fff"/>
              </g>
              <g class="eye-blink">
                <ellipse cx="112" cy="108" rx="9" ry="9.5" fill="#fff" stroke="#FF7043" stroke-width="1.5"/>
                <ellipse cx="112" cy="108" rx="4.5" ry="4.5" fill="#5D4037" class="pupil-look"/>
                <circle cx="114" cy="106" r="1.8" fill="#fff"/>
              </g>
              <path class="mouth" d="M87 122 Q100 128 113 122" stroke="#5D4037" stroke-width="2" stroke-linecap="round" fill="none"/>

              <circle cx="88" cy="108" r="12" fill="none" stroke="#5D4037" stroke-width="1.5" opacity=".35"/>
              <circle cx="112" cy="108" r="12" fill="none" stroke="#5D4037" stroke-width="1.5" opacity=".35"/>
              <path d="M76 108 Q72 104 70 108" stroke="#5D4037" stroke-width="1.5" fill="none" opacity=".35"/>
              <path d="M124 108 Q128 104 130 108" stroke="#5D4037" stroke-width="1.5" fill="none" opacity=".35"/>
              <path d="M74 155 Q58 165 60 178" stroke="#FF7043" stroke-width="8" stroke-linecap="round" fill="none"/>
              <path d="M126 155 Q142 165 140 178" stroke="#FF7043" stroke-width="8" stroke-linecap="round" fill="none"/>
              <circle cx="140" cy="180" r="7" fill="#FFCCBC"/>
              <rect x="142" y="168" width="4" height="22" rx="2" fill="#5D4037" transform="rotate(-20 144 180)"/>
            </svg>

            <!-- Thought chain dots -->
            <div class="thought-chain">
              <div class="td td1"></div>
              <div class="td td2"></div>
              <div class="td td3"></div>
            </div>

            <!-- Thought bubble -->
            <div class="thought-bub">
              <span class="thdot"></span>
              <span class="thdot"></span>
              <span class="thdot"></span>
            </div>
          </div>

          <div class="t-cap">小星老师正在思考中</div>
          <div class="t-sub">认真批改你的答案，马上就好…</div>
        </div>

        <button class="sheet-cta cta-t" disabled>批改中…</button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(10, 5, 30, 0.65);
  backdrop-filter: blur(3px);
  animation: backdropIn 0.25s ease;
}

.sheet {
  width: 100%;
  background: #fff;
  border-radius: 28px 28px 0 0;
  padding: 0 0 36px;
  animation: modalSlide 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.handle {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: #E0D8D0;
  margin: 12px auto 0;
}

/* THINKING STATE */
.s-think {
  padding: 24px 28px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.t-label {
  font-size: 13px;
  font-weight: 600;
  color: #9E9E9E;
  margin-bottom: 16px;
  letter-spacing: 0.3px;
  animation: slideUp 0.3s ease;
}

.teacher-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.scan {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(255, 112, 67, 0.4), transparent);
  animation: scanLine 1.5s ease-in-out infinite alternate;
  pointer-events: none;
  top: 60px;
}

.eye-blink {
  animation: blink 4s ease-in-out infinite;
}

.eye-blink:nth-child(2) {
  animation-delay: 0.6s;
}

.pupil-look {
  animation: pupilLook 3s ease-in-out infinite;
}

.mouth {
  animation: none;
}

.thought-chain {
  position: absolute;
  top: 6px;
  right: -8px;
}

.td {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 112, 67, 0.2);
  border: 2px solid rgba(255, 112, 67, 0.35);
}

.td1 {
  width: 10px;
  height: 10px;
  right: 4px;
  top: 28px;
  animation: bubble1 1.4s ease-in-out infinite;
}

.td2 {
  width: 16px;
  height: 16px;
  right: -2px;
  top: 10px;
  animation: bubble1 1.4s 0.2s ease-in-out infinite;
}

.td3 {
  width: 24px;
  height: 24px;
  right: -12px;
  top: -10px;
  animation: bubble1 1.4s 0.4s ease-in-out infinite;
}

.thought-bub {
  position: absolute;
  top: -52px;
  right: -60px;
  background: #fff;
  border: 2.5px solid rgba(255, 112, 67, 0.3);
  border-radius: 18px 18px 18px 4px;
  padding: 10px 14px;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: #BF360C;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  animation: bubble1 2s ease-in-out infinite;
}

.thdot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #FF7043;
  margin: 0 2px;
  animation: thoughtDot 1s ease-in-out infinite;
}

.thdot:nth-child(2) {
  animation-delay: 0.18s;
}

.thdot:nth-child(3) {
  animation-delay: 0.36s;
}

.t-cap {
  font-size: 15px;
  font-weight: bold;
  color: #2D2D2D;
  margin-top: 4px;
  text-align: center;
  animation: slideUp 0.4s 0.1s ease both;
  opacity: 0;
}

.t-sub {
  font-size: 12px;
  color: #9E9E9E;
  margin-top: 4px;
  text-align: center;
  animation: slideUp 0.4s 0.2s ease both;
  opacity: 0;
}

/* CTA */
.sheet-cta {
  margin: 0 24px;
  width: calc(100% - 48px);
  height: 52px;
  border-radius: 18px;
  border: none;
  font-size: 16px;
  font-weight: 900;
  cursor: not-allowed;
  font-family: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #F5F5F5;
  color: #9E9E9E;
}

/* Animations */
@keyframes blink {
  0%, 90%, 100% {
    transform: scaleY(1);
  }
  95% {
    transform: scaleY(0.08);
  }
}

@keyframes pupilLook {
  0%, 100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(2px, -1px);
  }
  50% {
    transform: translate(-1px, 1px);
  }
  75% {
    transform: translate(1px, -2px);
  }
}

@keyframes bubble1 {
  0%, 100% {
    transform: scale(1);
    opacity: 0.9;
  }
  50% {
    transform: scale(1.04);
    opacity: 1;
  }
}

@keyframes thoughtDot {
  0%, 80%, 100% {
    transform: scale(1);
    opacity: 0.7;
  }
  40% {
    transform: scale(1.35);
    opacity: 1;
  }
}

@keyframes scanLine {
  0% {
    top: 15%;
  }
  100% {
    top: 85%;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes backdropIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes modalSlide {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>
