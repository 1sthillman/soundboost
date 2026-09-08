/* ============ THEME METADATA ============ */
const THEME_META = {
  sumi:     {label:'Sumi-e',       title:'Mürekkep Nefesi',    sub:'Fırça darbeleriyle beliren tek çizgi'},
  aurora:   {label:'Kutup Şafağı', title:'Kutup Şafağı',       sub:'Gökyüzünde süzülen ışık şeritleri'},
  nova:     {label:'Nova',         title:'Çekirdek Uyanışı',   sub:'Bas vuruşuyla genişleyen plazma çekirdeği'},
  mycel:    {label:'Miselyum',     title:'Yeraltı Fısıltısı',  sub:'Kökler arasında yayılan ışık sinyali'},
  reef:     {label:'Derin Işıltı', title:'Derin Işıltı',       sub:'Karanlıkta parıldayan biyolüminesans'},
  monsoon:  {label:'Muson',        title:'Fırtına Öncesi',     sub:'Şimşek ve yağmurun ritmi'},
};

// i18n Translations
const i18n = {
  tr: {
    boost: 'Yükseltme',
    sensitivity: 'Hassasiyet',
    play_hint: 'Oynat\'a basarak canlı sesi başlat',
    nav_home: 'Ana Sayfa',
    nav_settings: 'Ayarlar',
    nav_equalizer: 'Ekolayzer',
    nav_language: 'Dil',
    themes: {
      mehtap: {label:'Mehtap', title:'Ay Balıkçısı', sub:'Durgun suda sabırla bekleyen bir sandalın hikâyesi'},
      sumi: {label:'Sumi-e', title:'Mürekkep Nefesi', sub:'Fırça darbeleriyle beliren tek çizgi'},
      aurora: {label:'Kutup Şafağı', title:'Kutup Şafağı', sub:'Gökyüzünde süzülen ışık şeritleri'},
      nova: {label:'Nova', title:'Çekirdek Uyanışı', sub:'Bas vuruşuyla genişleyen plazma çekirdeği'},
      mycel: {label:'Miselyum', title:'Yeraltı Fısıltısı', sub:'Kökler arasında yayılan ışık sinyali'},
      reef: {label:'Derin Işıltı', title:'Derin Işıltı', sub:'Karanlıkta parıldayan biyolüminesans'},
      monsoon: {label:'Muson', title:'Fırtına Öncesi', sub:'Şimşek ve yağmurun ritmi'},
      murekkep: {label:'Mürekkep', title:'Sumi Çiçeği', sub:'Suya damlayan mürekkebin doğal deseni'},
      col: {label:'Çöl', title:'Rüzgârın İzi', sub:'Kum tepelerinde süzülen ısı'},
      divit: {label:'Divit', title:'Mürekkep Damlası', sub:'Suya düşen bir divit mürekkebinin sessiz koreografisi'}
    }
  },
  en: {
    boost: 'Boost',
    sensitivity: 'Sensitivity',
    play_hint: 'Tap Play to start live audio',
    nav_home: 'Home',
    nav_settings: 'Settings',
    nav_equalizer: 'Equalizer',
    nav_language: 'Language',
    themes: {
      mehtap: {label:'Mehtap', title:'Moon Fisher', sub:'A patient boat on still water'},
      sumi: {label:'Sumi-e', title:'Ink Breath', sub:'Single stroke emerging from brush'},
      aurora: {label:'Aurora', title:'Northern Lights', sub:'Light ribbons dancing in sky'},
      nova: {label:'Nova', title:'Core Awakening', sub:'Plasma core expanding with bass'},
      mycel: {label:'Mycelium', title:'Underground Whisper', sub:'Light signal spreading through roots'},
      reef: {label:'Deep Glow', title:'Deep Glow', sub:'Bioluminescence glowing in darkness'},
      monsoon: {label:'Monsoon', title:'Before Storm', sub:'Rhythm of lightning and rain'},
      murekkep: {label:'Ink Bloom', title:'Sumi Blossom', sub:'Ink blooming in water with gold veins'},
      col: {label:'Desert', title:'Wind\'s Trace', sub:'Heat drifting over sand dunes'},
      divit: {label:'Inkwell', title:'Ink Drop', sub:'Silent choreography of inkwell drop in water'}
    }
  },
  de: {
    boost: 'Verstärkung',
    sensitivity: 'Empfindlichkeit',
    play_hint: 'Drücken Sie Play, um Live-Audio zu starten',
    nav_home: 'Startseite',
    nav_settings: 'Einstellungen',
    nav_equalizer: 'Equalizer',
    nav_language: 'Sprache',
    themes: {
      mehtap: {label:'Mehtap', title:'Mondfischer', sub:'Geduldiges Boot auf stillem Wasser'},
      sumi: {label:'Sumi-e', title:'Tintenatem', sub:'Einzelner Pinselstrich'},
      aurora: {label:'Aurora', title:'Nordlichter', sub:'Lichtbänder am Himmel'},
      nova: {label:'Nova', title:'Kernerwachen', sub:'Plasmakernerweiterung'},
      mycel: {label:'Myzel', title:'Unterirdisches Flüstern', sub:'Lichtsignal durch Wurzeln'},
      reef: {label:'Tiefes Leuchten', title:'Tiefes Leuchten', sub:'Biolumineszenz in Dunkelheit'},
      monsoon: {label:'Monsun', title:'Vor dem Sturm', sub:'Rhythmus von Blitz und Regen'},
      murekkep: {label:'Tinte', title:'Sumi-Blüte', sub:'Tinte blüht im Wasser mit Goldadern'},
      col: {label:'Wüste', title:'Spur des Windes', sub:'Hitze über Sanddünen'},
      divit: {label:'Tintenfass', title:'Tintentropfen', sub:'Stille Choreographie des Tintentropfens im Wasser'}
    }
  },
  fr: {
    boost: 'Amplification',
    sensitivity: 'Sensibilité',
    play_hint: 'Appuyez sur Play pour démarrer l\'audio en direct',
    nav_home: 'Accueil',
    nav_settings: 'Paramètres',
    nav_equalizer: 'Égaliseur',
    nav_language: 'Langue',
    themes: {
      mehtap: {label:'Mehtap', title:'Pêcheur Lunaire', sub:'Bateau patient sur l\'eau calme'},
      sumi: {label:'Sumi-e', title:'Souffle d\'Encre', sub:'Trait unique émergeant du pinceau'},
      aurora: {label:'Aurore', title:'Aurores Boréales', sub:'Rubans de lumière dansant'},
      nova: {label:'Nova', title:'Éveil du Noyau', sub:'Noyau de plasma en expansion'},
      mycel: {label:'Mycélium', title:'Chuchotement Souterrain', sub:'Signal lumineux à travers racines'},
      reef: {label:'Lueur Profonde', title:'Lueur Profonde', sub:'Bioluminescence dans l\'obscurité'},
      monsoon: {label:'Mousson', title:'Avant la Tempête', sub:'Rythme de la foudre et pluie'},
      murekkep: {label:'Encre', title:'Fleur Sumi', sub:'Encre fleurissant dans l\'eau avec or'},
      col: {label:'Désert', title:'Trace du Vent', sub:'Chaleur sur les dunes de sable'},
      divit: {label:'Encrier', title:'Goutte d\'Encre', sub:'Chorégraphie silencieuse d\'une goutte d\'encrier dans l\'eau'}
    }
  },
  es: {
    boost: 'Amplificación',
    sensitivity: 'Sensibilidad',
    play_hint: 'Toca Reproducir para iniciar audio en vivo',
    nav_home: 'Inicio',
    nav_settings: 'Ajustes',
    nav_equalizer: 'Ecualizador',
    nav_language: 'Idioma',
    themes: {
      mehtap: {label:'Mehtap', title:'Pescador Lunar', sub:'Bote paciente en agua tranquila'},
      sumi: {label:'Sumi-e', title:'Aliento de Tinta', sub:'Trazo único del pincel'},
      aurora: {label:'Aurora', title:'Luces del Norte', sub:'Cintas de luz bailando'},
      nova: {label:'Nova', title:'Despertar del Núcleo', sub:'Núcleo de plasma expandiéndose'},
      mycel: {label:'Micelio', title:'Susurro Subterráneo', sub:'Señal de luz por raíces'},
      reef: {label:'Resplandor Profundo', title:'Resplandor Profundo', sub:'Bioluminiscencia en oscuridad'},
      monsoon: {label:'Monzón', title:'Antes de la Tormenta', sub:'Ritmo de relámpagos y lluvia'},
      murekkep: {label:'Tinta', title:'Flor Sumi', sub:'Tinta floreciendo en agua con oro'},
      col: {label:'Desierto', title:'Rastro del Viento', sub:'Calor sobre dunas de arena'},
      divit: {label:'Tintero', title:'Gota de Tinta', sub:'Coreografía silenciosa de gota de tintero en agua'}
    }
  },
  ru: {
    boost: 'Усиление',
    sensitivity: 'Чувствительность',
    play_hint: 'Нажмите Play, чтобы начать',
    nav_home: 'Главная',
    nav_settings: 'Настройки',
    nav_equalizer: 'Эквалайзер',
    nav_language: 'Язык',
    themes: {
      mehtap: {label:'Мехтап', title:'Лунный Рыбак', sub:'Терпеливая лодка на тихой воде'},
      sumi: {label:'Суми-э', title:'Дыхание Чернил', sub:'Единый мазок кисти'},
      aurora: {label:'Аврора', title:'Северное Сияние', sub:'Ленты света в небе'},
      nova: {label:'Нова', title:'Пробуждение Ядра', sub:'Расширение плазменного ядра'},
      mycel: {label:'Мицелий', title:'Подземный Шепот', sub:'Световой сигнал через корни'},
      reef: {label:'Глубокое Свечение', title:'Глубокое Свечение', sub:'Биолюминесценция во тьме'},
      monsoon: {label:'Муссон', title:'Перед Бурей', sub:'Ритм молнии и дождя'},
      murekkep: {label:'Чернила', title:'Суми Цветок', sub:'Чернила цветут в воде с золотом'},
      col: {label:'Пустыня', title:'След Ветра', sub:'Жар над песчаными дюнами'},
      divit: {label:'Чернильница', title:'Капля Чернил', sub:'Тихая хореография капли чернил в воде'}
    }
  },
  ar: {
    boost: 'تعزيز',
    sensitivity: 'الحساسية',
    play_hint: 'اضغط تشغيل لبدء الصوت المباشر',
    nav_home: 'الرئيسية',
    nav_settings: 'الإعدادات',
    nav_equalizer: 'المعادل',
    nav_language: 'اللغة',
    themes: {
      mehtap: {label:'مهتاب', title:'صياد القمر', sub:'قارب صبور على ماء ساكن'},
      sumi: {label:'سومي-إي', title:'نفس الحبر', sub:'ضربة فرشاة واحدة'},
      aurora: {label:'الشفق', title:'الأضواء الشمالية', sub:'أشرطة ضوئية راقصة'},
      nova: {label:'نوفا', title:'صحوة النواة', sub:'توسع نواة البلازما'},
      mycel: {label:'الفطريات', title:'همس تحت الأرض', sub:'إشارة ضوئية عبر الجذور'},
      reef: {label:'توهج عميق', title:'توهج عميق', sub:'إضاءة حيوية في الظلام'},
      monsoon: {label:'الموسم', title:'قبل العاصفة', sub:'إيقاع البرق والمطر'},
      murekkep: {label:'الحبر', title:'زهرة سومي', sub:'الحبر يتفتح في الماء مع الذهب'},
      col: {label:'الصحراء', title:'أثر الرياح', sub:'الحرارة فوق الكثبان الرملية'},
      divit: {label:'المحبرة', title:'قطرة الحبر', sub:'رقصة صامتة لقطرة المحبرة في الماء'}
    }
  },
  ja: {
    boost: 'ブースト',
    sensitivity: '感度',
    play_hint: '再生を押してライブオーディオを開始',
    nav_home: 'ホーム',
    nav_settings: '設定',
    nav_equalizer: 'イコライザー',
    nav_language: '言語',
    themes: {
      mehtap: {label:'月明かり', title:'月の漁師', sub:'静かな水面の忍耐強い船'},
      sumi: {label:'墨絵', title:'墨の息吹', sub:'筆から生まれる一線'},
      aurora: {label:'オーロラ', title:'北極光', sub:'空に舞う光の帯'},
      nova: {label:'ノヴァ', title:'核の目覚め', sub:'低音で広がるプラズマ核'},
      mycel: {label:'菌糸', title:'地下の囁き', sub:'根を伝う光信号'},
      reef: {label:'深海の輝き', title:'深海の輝き', sub:'闇に光る生物発光'},
      monsoon: {label:'モンスーン', title:'嵐の前', sub:'稲妻と雨のリズム'},
      murekkep: {label:'墨', title:'墨の花', sub:'水に広がる墨と金の脈'},
      col: {label:'砂漠', title:'風の跡', sub:'砂丘の上を漂う熱'},
      divit: {label:'インク壺', title:'インクの滴', sub:'水の中でインクの滴が描く静かな舞'}
    }
  },
  zh: {
    boost: '增强',
    sensitivity: '灵敏度',
    play_hint: '点击播放开始实时音频',
    nav_home: '主页',
    nav_settings: '设置',
    nav_equalizer: '均衡器',
    nav_language: '语言',
    themes: {
      mehtap: {label:'月光', title:'月亮渔夫', sub:'静水上的耐心之舟'},
      sumi: {label:'水墨画', title:'墨息', sub:'毛笔划出的单线'},
      aurora: {label:'极光', title:'北极光', sub:'天空中舞动的光带'},
      nova: {label:'新星', title:'核心觉醒', sub:'低音扩展等离子核'},
      mycel: {label:'菌丝', title:'地下耳语', sub:'根间传播的光信号'},
      reef: {label:'深海辉光', title:'深海辉光', sub:'黑暗中闪耀的生物发光'},
      monsoon: {label:'季风', title:'暴风前', sub:'闪电和雨的节奏'},
      murekkep: {label:'墨水', title:'墨之花', sub:'墨水在水中绽放与金脉'},
      col: {label:'沙漠', title:'风之痕', sub:'沙丘上飘动的热浪'},
      divit: {label:'墨水瓶', title:'墨滴', sub:'墨滴在水中的静默编舞'}
    }
  },
  ko: {
    boost: '부스트',
    sensitivity: '감도',
    play_hint: '재생을 눌러 라이브 오디오 시작',
    nav_home: '홈',
    nav_settings: '설정',
    nav_equalizer: '이퀄라이저',
    nav_language: '언어',
    themes: {
      mehtap: {label:'달빛', title:'달의 어부', sub:'잔잔한 물 위의 인내심 있는 배'},
      sumi: {label:'먹그림', title:'먹의 숨결', sub:'붓에서 나오는 한 획'},
      aurora: {label:'오로라', title:'북극광', sub:'하늘을 춤추는 빛 띠'},
      nova: {label:'노바', title:'핵의 각성', sub:'저음으로 확장되는 플라즈마 핵'},
      mycel: {label:'균사체', title:'지하의 속삭임', sub:'뿌리를 통해 퍼지는 빛 신호'},
      reef: {label:'깊은 빛', title:'깊은 빛', sub:'어둠 속에서 빛나는 생물 발광'},
      monsoon: {label:'몬순', title:'폭풍 전', sub:'번개와 비의 리듬'},
      murekkep: {label:'잉크', title:'먹 꽃', sub:'물에서 피어나는 먹과 금빛 맥'},
      col: {label:'사막', title:'바람의 흔적', sub:'모래 언덕 위를 떠도는 열기'},
      divit: {label:'잉크병', title:'잉크 방울', sub:'물속에서 잉크 방울이 그리는 고요한 안무'}
    }
  }
};
