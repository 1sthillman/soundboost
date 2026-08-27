# Google Play Console - Adım Adım Yükleme

## 1. Varsayılan Mağaza Girişi (Store Listing)

### Dil Seçimi
1. **Varsayılan dil:** Türkçe - tr-TR
2. "Dosya içe aktarın" butonuna tıkla
3. `GOOGLE_PLAY_STORE_LISTING_TR.txt` dosyasındaki içerikleri kullan

### Doldurulması Gerekenler:

#### 📱 Uygulama Adı (30 karakter max)
```
Sound'ST Boost - Volume++
```

#### 📝 Kısa Açıklama (80 karakter max)
```
5 tema, ekolayzer ve bas güçlendirmeli güçlü ses yükseltici uygulama.
```

#### 📄 Tam Açıklama
`GOOGLE_PLAY_STORE_LISTING_TR.txt` dosyasındaki "TAM AÇIKLAMA" bölümünü kopyala-yapıştır

---

## 2. Grafikler ve Medya

### ✅ Mevcut Dosyalar
- **Uygulama İkonu:** `play_store_icon_512.png` (512x512)

### ⚠️ Oluşturulması Gerekenler

#### Öne Çıkan Grafik (ZORUNLU)
- **Boyut:** 1024 x 500 piksel
- **Format:** JPG veya PNG
- **İçerik:** Uygulama adı + logosu + temalardan örnekler
- **Önerilen araçlar:** Canva, Figma, Photoshop

#### Telefon Ekran Görüntüleri (Minimum 2)
1. Uygulamayı emülatör veya gerçek cihazda çalıştır
2. Önerilen ekran görüntüleri:
   - Ana ekran (NEON tema)
   - Equalizer ekranı
   - Tema seçim ekranı
   - Dil seçimi ekranı
   - Ayarlar ekranı

**Nasıl Alınır:**
- Emülatör: Android Studio > Running Device > Screenshot
- Telefon: Volume Down + Power tuşlarına basılı tut

---

## 3. Kategori ve Etiketler

### Kategori
- **Ana:** Müzik ve Ses
- **İkincil (opsiyonel):** Araçlar

### Etiketler (Tags)
```
ses yükseltici, ekolayzer, bas güçlendirme, ses amplifikatörü, müzik güçlendirici
```

---

## 4. İletişim Bilgileri

### E-posta (ZORUNLU)
- Kendi e-posta adresini ekle
- Google Play Console'da doğrulanmış olmalı

### Web sitesi (Opsiyonel)
```
https://github.com/1sthillman/soundboost
```

---

## 5. Gizlilik Politikası (ZORUNLU)

### Seçenek 1: GitHub Pages (Önerilen)
1. GitHub repo'na git: https://github.com/1sthillman/soundboost
2. Settings > Pages > Source: main branch
3. `PRIVACY_POLICY.md` dosyasını `privacy-policy.html` olarak rename et
4. URL: `https://1sthillman.github.io/soundboost/privacy-policy.html`

### Seçenek 2: GitHub Raw (Hızlı)
```
https://raw.githubusercontent.com/1sthillman/soundboost/main/PRIVACY_POLICY.md
```

### Seçenek 3: Google Sites (Ücretsiz)
1. https://sites.google.com'a git
2. Yeni site oluştur
3. `PRIVACY_POLICY.md` içeriğini yapıştır
4. Yayınla ve URL'i al

---

## 6. Uygulama İçeriği (App Content)

### Gizlilik Politikası
- URL'i yukarıdaki adımlardan al ve yapıştır

### Veri Güvenliği (Data Safety)
Tüm sorulara **"Hayır"** yanıtı ver çünkü:
- ✅ Hiçbir veri toplanmıyor
- ✅ Hiçbir veri paylaşılmıyor
- ✅ Tüm ayarlar cihazda kalıyor

**Sorular:**
1. Kullanıcı verisi toplanıyor mu? **HAYIR**
2. Kullanıcı verisi paylaşılıyor mu? **HAYIR**
3. Güvenlik uygulamaları: **Veri şifreleme kullanılıyor** (cihaz içi)

### Hedef Kitle ve İçerik
- **Hedef yaş:** 13+ veya Herkes
- **İçerik derecelendirmesi:** Anketi doldur (Herkes çıkacak)
- **Haber uygulaması:** Hayır
- **COVID-19 temas takibi:** Hayır

### Reklamlar
- **Reklam içeriyor mu?** HAYIR

---

## 7. Fiyatlandırma ve Dağıtım

### Fiyat
- **Ücretsiz**
- ✅ İçeride satın alma YOK
- ✅ Reklam YOK

### Ülkeler
- **Tüm ülkeler** seçilsin
- Veya istediğin ülkeleri seçebilirsin

### Cihaz Kategorileri
- ✅ Telefon
- ✅ Tablet
- ❌ Wear OS (hayır)
- ❌ Android TV (hayır)
- ❌ Android Auto (hayır)

---

## 8. Sürüm Oluştur (Create Release)

### Production Track
1. **Create new release** butonuna tıkla
2. **Upload AAB:** `app\build\outputs\bundle\release\SoundSTBoost-v1.0.3-release.aab`
3. **Release name:** 1.0.3
4. **Release notes (Türkçe):**

```
🎉 İlk Sürüm!

✨ Özellikler:
• 5 benzersiz görsel tema
• Profesyonel ses efektleri (ses yükseltme, ekolayzer, bas güçlendirme)
• 10 dil desteği
• Gerçek zamanlı ses görselleştirici
• Arka plan servisi
• Gizlilik odaklı (veri toplama YOK)
• Android 15 (API 35) desteği

🎵 Sound'ST Boost ile ses deneyiminizi mükemmelleştirin!
```

5. **Review and rollout** > **Start rollout to production**

---

## 9. Test (Opsiyonel ama Önerilen)

### Internal Testing
1. **Test kullanıcıları ekle**
2. AAB'yi yükle
3. Test et
4. Sorun yoksa Production'a taşı

**Test kullanıcı uyarısı:** Console'da "Dahili test sürümü oluşturma" uyarısı görebilirsin. Bu opsiyonel.

---

## ✅ Son Kontrol Listesi

- [ ] AAB dosyası yüklendi (v1.0.3, API 35)
- [ ] Uygulama ikonu hazır (512x512)
- [ ] Öne çıkan grafik oluşturuldu (1024x500) ⚠️ ZORUNLU
- [ ] En az 2 ekran görüntüsü alındı ⚠️ ZORUNLU
- [ ] Store listing dolduruldu (Türkçe)
- [ ] Gizlilik politikası URL'i eklendi
- [ ] Data safety "No data collected" işaretlendi
- [ ] İçerik derecelendirmesi tamamlandı
- [ ] İletişim e-postası eklendi
- [ ] Release notes yazıldı
- [ ] Review'a gönderildi

---

## 📞 Sorun Çıkarsa

### API 35 Hatası Aldıysan
✅ Çözüldü! AAB dosyası API 35 ile oluşturuldu.

### "Öne Çıkan Grafik Eksik" Hatası
⚠️ Bu ZORUNLU. Canva ile hızlıca oluştur:
1. https://canva.com
2. "Custom Size" > 1024 x 500
3. Uygulama adı + logo + tema örnekleri ekle
4. Download > PNG

### "Ekran Görüntüleri Eksik" Hatası
⚠️ Minimum 2 ekran görüntüsü gerekli.
Uygulamayı çalıştır ve ekran görüntüsü al.

---

**İyi şanslar! 🚀**
