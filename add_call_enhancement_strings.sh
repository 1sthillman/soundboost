#!/bin/bash

# German
cat >> app/src/main/res/values-de/strings.xml << 'EOF'
    
    <!-- Anrufverbesserung -->
    <string name="call_enhancement_title">ANRUFVERBESSERUNG</string>
    <string name="call_enhancement_desc">Verbessert die Klarheit eingehender Stimmen + reduziert IHRE Hintergrundgeräusche während Anrufen</string>
    <string name="call_enhancement_noise_suppression">Geräuschunterdrückung</string>
    <string name="call_enhancement_auto_gain">Automatische Verstärkung</string>
EOF

# Spanish
cat >> app/src/main/res/values-es/strings.xml << 'EOF'
    
    <!-- Mejora de llamadas -->
    <string name="call_enhancement_title">MEJORA DE LLAMADAS</string>
    <string name="call_enhancement_desc">Mejora la claridad de la voz entrante + reduce el ruido de fondo durante las llamadas</string>
    <string name="call_enhancement_noise_suppression">Supresión de ruido</string>
    <string name="call_enhancement_auto_gain">Ganancia automática</string>
EOF

# French
cat >> app/src/main/res/values-fr/strings.xml << 'EOF'
    
    <!-- Amélioration des appels -->
    <string name="call_enhancement_title">AMÉLIORATION DES APPELS</string>
    <string name="call_enhancement_desc">Améliore la clarté de la voix entrante + réduit VOTRE bruit de fond pendant les appels</string>
    <string name="call_enhancement_noise_suppression">Suppression du bruit</string>
    <string name="call_enhancement_auto_gain">Gain automatique</string>
EOF

# Japanese
cat >> app/src/main/res/values-ja/strings.xml << 'EOF'
    
    <!-- 通話強化 -->
    <string name="call_enhancement_title">通話強化</string>
    <string name="call_enhancement_desc">着信音声のクリアさを向上 + 通話中のあなたの背景ノイズを削減</string>
    <string name="call_enhancement_noise_suppression">ノイズ抑制</string>
    <string name="call_enhancement_auto_gain">自動ゲイン</string>
EOF

# Korean
cat >> app/src/main/res/values-ko/strings.xml << 'EOF'
    
    <!-- 통화 향상 -->
    <string name="call_enhancement_title">통화 향상</string>
    <string name="call_enhancement_desc">수신 음성 선명도 향상 + 통화 중 배경 소음 감소</string>
    <string name="call_enhancement_noise_suppression">소음 억제</string>
    <string name="call_enhancement_auto_gain">자동 게인</string>
EOF

# Russian
cat >> app/src/main/res/values-ru/strings.xml << 'EOF'
    
    <!-- Улучшение звонков -->
    <string name="call_enhancement_title">УЛУЧШЕНИЕ ЗВОНКОВ</string>
    <string name="call_enhancement_desc">Улучшает четкость входящего голоса + снижает ВАШ фоновый шум во время звонков</string>
    <string name="call_enhancement_noise_suppression">Подавление шума</string>
    <string name="call_enhancement_auto_gain">Автоматическое усиление</string>
EOF

# Chinese
cat >> app/src/main/res/values-zh/strings.xml << 'EOF'
    
    <!-- 通话增强 -->
    <string name="call_enhancement_title">通话增强</string>
    <string name="call_enhancement_desc">提升来电语音清晰度 + 减少通话中您的背景噪音</string>
    <string name="call_enhancement_noise_suppression">噪音抑制</string>
    <string name="call_enhancement_auto_gain">自动增益</string>
EOF

echo "Call enhancement strings added to all languages!"
