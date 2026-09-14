const fs = require('fs');

const languages = {
  'ja': {
    comment: '<!-- オーディオ許可の重要な開示（Google Play必須） -->',
    strings: {
      audio_permission_title: 'ビジュアルテーマには許可が必要です',
      audio_permission_description: '私たちのビジュアルテーマは、バックグラウンドで再生される音楽（Spotify、YouTubeなど）と同期します。テーマがリズムに合わせて動くには、オーディオ分析の許可が必要です。',
      audio_permission_why_title: 'なぜ必要なのか',
      audio_permission_why_desc: 'テーマは外部アプリ（Spotify、YouTube）の音楽に反応します。Androidは周波数分析のためにオーディオ許可を要求します。',
      audio_permission_privacy_title: 'あなたのプライバシー',
      audio_permission_privacy_desc: 'オーディオデータはテーマアニメーションのためにリアルタイムでのみ分析されます。私たちはオーディオデータを録音、保存、または共有しません。',
      audio_permission_technical_title: '技術的な詳細',
      audio_permission_technical_desc: 'Androidの公式Visualizer APIを使用しています。オーディオはデバイス上でローカルに処理され、すぐに破棄されます。',
      audio_permission_accept: '同意します',
      audio_permission_deny: '結構です',
      audio_permission_footer: '設定でいつでもこの許可を変更できます'
    }
  },
  'ko': {
    comment: '<!-- 오디오 권한 주요 공개 (Google Play 필수) -->',
    strings: {
      audio_permission_title: '비주얼 테마에 권한 필요',
      audio_permission_description: '우리의 비주얼 테마는 백그라운드에서 재생되는 음악(Spotify, YouTube 등)과 동기화됩니다. 테마가 리듬에 맞춰 움직이게 하려면 오디오 분석 권한이 필요합니다.',
      audio_permission_why_title: '왜 필요한가요',
      audio_permission_why_desc: '테마가 외부 앱(Spotify, YouTube)의 음악에 반응합니다. Android는 주파수 분석을 위해 오디오 권한을 요구합니다.',
      audio_permission_privacy_title: '귀하의 개인정보',
      audio_permission_privacy_desc: '오디오 데이터는 테마 애니메이션을 위해 실시간으로만 분석됩니다. 우리는 오디오 데이터를 녹음, 저장 또는 공유하지 않습니다.',
      audio_permission_technical_title: '기술 세부사항',
      audio_permission_technical_desc: 'Android의 공식 Visualizer API를 사용합니다. 오디오는 장치에서 로컬로 처리되고 즉시 삭제됩니다.',
      audio_permission_accept: '동의합니다',
      audio_permission_deny: '사양합니다',
      audio_permission_footer: '설정에서 언제든지 이 권한을 변경할 수 있습니다'
    }
  },
  'ru': {
    comment: '<!-- Важное раскрытие разрешения на аудио (обязательно для Google Play) -->',
    strings: {
      audio_permission_title: 'Визуальным Темам Требуется Разрешение',
      audio_permission_description: 'Наши визуальные темы синхронизируются с музыкой, воспроизводимой в фоновом режиме (Spotify, YouTube и т.д.). Чтобы темы двигались в ритме, нам нужно разрешение на анализ аудио.',
      audio_permission_why_title: 'Почему Это Необходимо',
      audio_permission_why_desc: 'Темы реагируют на музыку из внешних приложений (Spotify, YouTube). Android требует разрешение на аудио для анализа частот.',
      audio_permission_privacy_title: 'Ваша Конфиденциальность',
      audio_permission_privacy_desc: 'Аудио данные анализируются в реальном времени ТОЛЬКО для анимации тем. Мы НЕ записываем, НЕ храним и НЕ передаем аудио данные.',
      audio_permission_technical_title: 'Технические Детали',
      audio_permission_technical_desc: 'Мы используем официальный Visualizer API Android. Аудио обрабатывается локально на вашем устройстве и немедленно удаляется.',
      audio_permission_accept: 'Я Принимаю',
      audio_permission_deny: 'Нет, Спасибо',
      audio_permission_footer: 'Вы можете изменить это разрешение в любое время в Настройках'
    }
  },
  'zh': {
    comment: '<!-- 音频权限重要披露（Google Play 必需） -->',
    strings: {
      audio_permission_title: '视觉主题需要权限',
      audio_permission_description: '我们的视觉主题与后台播放的音乐（Spotify、YouTube等）同步。要让主题随节奏移动，我们需要音频分析权限。',
      audio_permission_why_title: '为什么需要此权限',
      audio_permission_why_desc: '主题会响应外部应用（Spotify、YouTube）的音乐。Android需要音频权限进行频率分析。',
      audio_permission_privacy_title: '您的隐私',
      audio_permission_privacy_desc: '音频数据仅实时分析以用于主题动画。我们不会录制、存储或分享任何音频数据。',
      audio_permission_technical_title: '技术细节',
      audio_permission_technical_desc: '我们使用Android的官方Visualizer API。音频在您的设备上本地处理并立即丢弃。',
      audio_permission_accept: '我接受',
      audio_permission_deny: '不了，谢谢',
      audio_permission_footer: '您可以随时在设置中更改此权限'
    }
  }
};

Object.keys(languages).forEach(lang => {
  const filePath = `app/src/main/res/values-${lang}/strings.xml`;
  let content = fs.readFileSync(filePath, 'utf8');
  
  // Find the position after microphone_permission_desc
  const marker = 'microphone_permission_desc';
  const markerIndex = content.indexOf(marker);
  
  if (markerIndex === -1) {
    console.log(`Marker not found in ${lang}`);
    return;
  }
  
  // Find the end of the line containing microphone_permission_desc
  const lineEndIndex = content.indexOf('</string>', markerIndex) + '</string>'.length;
  const insertPosition = content.indexOf('\n', lineEndIndex) + 1;
  
  // Build the new strings
  let newStrings = '\n    ' + languages[lang].comment + '\n';
  Object.keys(languages[lang].strings).forEach(key => {
    const value = languages[lang].strings[key];
    newStrings += `    <string name="${key}">${value}</string>\n`;
  });
  
  // Insert the new strings
  content = content.slice(0, insertPosition) + newStrings + content.slice(insertPosition);
  
  // Write back to file
  fs.writeFileSync(filePath, content, 'utf8');
  console.log(`✓ Added strings to ${lang}`);
});

console.log('\n✅ All 4 language files updated successfully!');
