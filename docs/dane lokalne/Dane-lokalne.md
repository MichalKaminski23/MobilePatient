# Inwentaryzacja i Klasyfikacja Danych Przechowywanych Lokalnie — MobilnyPacjent (MASVS-STORAGE-1)

| Pole                | Wartość                                                                          |
|---------------------|----------------------------------------------------------------------------------|
| **Aplikacja**       | MobilnyPacjent                                                                   |
| **Pakiet Androida** | `pl.polsl.mobilepatient`                                                         |
| **Obszar MASVS**    | MASVS-STORAGE (MASVS-STORAGE-1: Bezpieczne przechowywanie danych na urządzeniu)  |
| **Wymóg RODO**      | Art. 5 (Minimalizacja danych, integralność i poufność), Art. 9 (Dane szczególne) |
| **Etap prac**       | Przygotowanie do Z4 (Pamięć lokalna i kryptografia)                              |
| **Autorzy**         | Michał Kamiński, Patryk Czakon                                                   |

---

## 1. Wprowadzenie i Cel Inwentaryzacji

Zgodnie ze standardem **OWASP MASVS (Mobile Application Security Verification Standard)** w
kategorii **MASVS-STORAGE**, każda aplikacja mobilna przetwarzająca dane osobowe oraz informacje
medyczne musi posiadać precyzyjną inwentaryzację wszystkich danych zapisywanych na urządzeniu
użytkownika.

Aplikacja **MobilnyPacjent** przetwarza dane pacjentów o szczególnym charakterze (dane o stanie
zdrowia w rozumieniu Art. 9 RODO: historia chorób, e-recepty, skierowania, wyniki badań
laboratoryjnych i obrazowych).

Nadrzędną zasadą architektury aplikacji jest **minimalizacja składowania danych w pamięci trwałej
urządzenia**:

1. Większość danych pobierana jest na żądanie z backendu Supabase i przechowywana wyłącznie w
   pamięci operacyjnej (RAM).
2. Dane niezbędne do działania offline lub utrzymania sesji podlegają ścisłej kontroli, izolacji w
   piaskownicy Androida (*App Sandbox*) oraz wyłączeniu z mechanizmów automatycznych kopii
   zapasowych (*Auto Backup*).

---

## 2. Zbiorcza Tabela Inwentaryzacji Danych Lokalnych

|  Lp.  | Kategoria danych                                                | Dokładny zakres danych                                                                                                                     | Miejsce zapisu na urządzeniu                                                                         |                                      Klasyfikacja wrażliwości                                      | Szyfrowanie / Mechanizm ochrony                                                                                                                          |               Czy trafia do kopii zapasowej (Backup)?                | Cykl życia i usuwanie                                                                                             |
|:-----:|:----------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------:|:------------------------------------------------------------------------------------------------------------------|
| **1** | **Tokeny sesji (Auth Tokens)**                                  | • JWT Access Token (krótkoterminowy, 1h)<br>• Refresh Token (długoterminowy)                                                               | Pamięć wewnętrzna aplikacji:<br>`/data/data/pl.polsl.mobilepatient/` (Supabase Auth Session Storage) |                         **Szczególnie wrażliwe** (dane uwierzytelniające)                          | • Piaskownica systemu Android (SELinux + unikalny Linux UID)<br>• *(Plan Z4)*: Migracja do `EncryptedSharedPreferences` (AES-256-GCM + Android Keystore) |         **NIE**<br>(zablokowane przez `allowBackup="false"`)         | Usuwane natychmiast podczas akcji `signOut()` / wylogowania lub po wygaśnięciu sesji.                             |
| **2** | **Profil i dane tożsamości użytkownika**                        | • Imię i nazwisko<br>• Rola w systemie (`patient`, `doctor`, `admin`)<br>• Adres e-mail<br>• Identyfikator konta (`user_id`, `patient_id`) | Pamięć operacyjna procesu (RAM):<br>Stany Jetpack Compose (`remember`), ViewModel (`StateFlow`)      |                              **Poufne / Dane osobowe (RODO Art. 6)**                               | • Izolacja pamięci procesu w jądrze Linux<br>• Brak zapisu na dysku trwałym Flash                                                                        |            **NIE** (dane nieulotne nie istnieją na dysku)            | Zwalniane z pamięci RAM po zamknięciu aplikacji lub wyczyszczeniu stanu przy wylogowaniu.                         |
| **3** | **Tymczasowe pliki zdjęć i dokumentów (Skany badań)**           | • Miniatury i skany recept/badań z aparatu<br>• Pliki wybrane przez systemowy Photo Picker                                                 | Pamięć podręczna aplikacji (Cache):<br>`/data/data/pl.polsl.mobilepatient/cache/image_cache/`        |                     **Szczególnie wrażliwe** (dokumentacja medyczna pacjenta)                      | • Wewnętrzny katalog cache aplikacji (dostępny tylko dla UID aplikacji)<br>• Brak zapisu w publicznym `DCIM` ani karcie SD                               |  **NIE**<br>(katalog `cache/` jest domyślnie wykluczony z backupu)   | Czyszczone po wysłaniu do Supabase Storage oraz automatycznie przez mechanizmy czyszczenia cache systemu Android. |
| **4** | **Pamięć podręczna sieci (HTTP / Ktor / Supabase Cache)**       | • Fragmenty odpowiedzi JSON z bazy PostgreSQL (np. lista przychodni, terminy wizyt)<br>• Metadane kafelków mapy Google Maps                | Pamięć podręczna aplikacji:<br>`/data/data/pl.polsl.mobilepatient/cache/`                            |                                             **Poufne**                                             | • Izolacja piaskownicy procesowej<br>• Szyfrowanie partycji `/data` na urządzeniu (FBE — *File-Based Encryption*)                                        |                               **NIE**                                | Unieważniane zgodnie z nagłówkami HTTP Cache-Control lub usuwane ręcznie/automatycznie przy braku pamięci.        |
| **5** | **Preferencje i ustawienia aplikacji**                          | • Wybór motywu interfejsu (jasny/ciemny)<br>• Flaga pierwszego uruchomienia<br>• Ustawienia powiadomień lokalnych                          | Prywatne preferencje XML:<br>`/data/data/pl.polsl.mobilepatient/shared_prefs/`                       |                                    **Niewrażliwe / Operacyjne**                                    | • Dostęp wyłącznie dla aplikacji (tryb `MODE_PRIVATE`)                                                                                                   |      **NIE**<br>(wykluczone przez `data_extraction_rules.xml`)       | Przechowywane do momentu odinstalowania aplikacji lub wyczyszczenia danych aplikacji przez użytkownika.           |
| **6** | **Klucze kryptograficzne aplikacji (Plan Z4)**                  | • Klucz główny aplikacji (*Master Key*)<br>• Symetryczny klucz AES do bazy lokalnej                                                        | Sprzętowy moduł bezpieczeństwa:<br>**Android Keystore** (TEE / StrongBox)                            |                                           **Krytyczne**                                            | Sprzętowo izolowany układ kryptograficzny (brak możliwości wyeksportowania klucza prywatnego poza procesor)                                              | **NIE** (klucze Keystore są powiązane sprzętowo z danym urządzeniem) | Unieważniane przy zmianie blokady ekranu (jeśli skonfigurowano) lub usunięciu aplikacji.                          |
| **7** | **Migawki ekranu w menedżerze zadań (Recents Screen Snapshot)** | • Zrzuty ekranu widoków dokumentacji i recept tworzone automatycznie przez system Android przy przełączaniu aplikacji                      | Bufor menedżera okien systemu (`System WindowManager`)                                               | **Szczególnie wrażliwe** (ryzyko podejrzenia danych medycznych z ekranu blokady/wielozadaniowości) | Flaga okna `FLAG_SECURE` (`WindowManager.LayoutParams.FLAG_SECURE`) blokująca tworzenie miniatur podglądu                                                |                               **NIE**                                | Zrzut ekranu jest zaciemniany na poziomie kompozytora systemowego SurfaceFlinger.                                 |
| **8** | **Logi diagnostyczne i systemowe (Logcat)**                     | • Zdarzenia nawigacji, komunikaty błędów połączenia sieciowego                                                                             | Bufor kołowy logów systemowych Androida (`logd`)                                                     |                                           **Operacyjne**                                           | **Bezwzględny zakaz logowania danych wrażliwych** (brak PESEL, tokenów JWT, haseł i treści dokumentacji w `Log.d` / `println`)                           |                               **NIE**                                | Bufor rotacyjny jądra systemu Android, czyszczony na bieżąco.                                                     |

---

## 3. Szczegółowa Analiza Ryzyk i Zastosowane Zabezpieczenia

### 3.1. Ochrona przed wyciekiem w kopiach zapasowych (Auto Backup i ADB Backup)

W pliku `AndroidManifest.xml` wdrożono atrybuty:

```xml
android:allowBackup="false"
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
```

Oraz reguły w `app/src/main/res/xml/backup_rules.xml` i `data_extraction_rules.xml`:

```xml
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" />
        <exclude domain="file" />
        <exclude domain="database" />
        <exclude domain="sharedpref" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" />
        <exclude domain="file" />
        <exclude domain="database" />
        <exclude domain="sharedpref" />
    </device-transfer>
</data-extraction-rules>
```

* **Uzasadnienie bezpieczeństwa:** Uniemożliwia to wykonanie zrzutu bazy danych i plików sesji przez
  polecenie `adb backup`, a także zabezpiecza dane przed automatyczną synchronizacją z chmurą Google
  Drive użytkownika w nieszyfrowanej postaci.

---

### 3.2. Izolacja Piaskownicy Androida (Application Sandbox)

Wszystkie pliki aplikacji są zapisywane wewnątrz katalogu prywatnego:

```
/data/data/pl.polsl.mobilepatient/
├── cache/            # Pamięć podręczna obrazów Coil i Ktor HTTP
├── code_cache/       # Skompilowany kod DEX / AOT
├── files/            # Pamięć sesji Supabase Auth
└── shared_prefs/     # Konfiguracja i ustawienia XML (MODE_PRIVATE)
```

* **Mechanizm ochrony:** Jądro Linux przydziela aplikacji unikalny identyfikator użytkownika
  (`UID`). Żadna inna aplikacja zainstalowana na urządzeniu (bez uprawnień roota) nie ma prawa
  odczytu ani zapisu w tym drzewie katalogów.
* Dodatkowo urządzenia z systemem Android 9+ (nasz `minSdk = 28`) posiadają sprzętowe szyfrowanie
  oparte na plikach (**FBE — File-Based Encryption**), co oznacza, że pliki w pamięci flash są
  zaszyfrowane kluczem powiązanym z poświadczeniami blokady ekranu użytkownika (*Credential
  Encrypted Storage*).

---

### 3.3. Bezpieczne Przetwarzanie Zdjęć Badań (Zero-Permanent-Storage w Galerii)

Podczas skanowania recept i wyników badań:

1. **Zdjęcia nie trafiają do publicznej galerii:** Aplikacja nie zapisuje plików w publicznych
   katalogach `Environment.getExternalStoragePublicDirectory()` ani `MediaStore.Images`.
2. **Photo Picker:** Wykorzystanie `ActivityResultContracts.PickVisualMedia()` sprawia, że aplikacja
   otrzymuje jedynie bezpieczny deskryptor `content://` do wybranego przez użytkownika pliku, bez
   wglądu w pozostałe zdjęcia na urządzeniu.
3. **Pliki tymczasowe:** Jeżeli zachodzi potrzeba utworzenia pliku pośredniego przed wysłaniem do
   Supabase Storage, jest on tworzony w `context.cacheDir` z losową nazwą
   (`File.createTempFile("scan_", ".tmp", context.cacheDir)`) i bezwzględnie usuwany natychmiast po
   ukończeniu transferu sieciowego.

---

### 3.4. Procedura Wylogowania i Niszczenia Danych (Sign-Out & Data Sanitization)

W momencie kliknięcia przycisku **„Wyloguj się”** w `PatientDashboardScreen.kt`:

```kotlin
onLogout = {
    scope.launch {
        authRepository.signOut()
        loggedInRole = null
        loggedInName = ""
    }
}
```

Aplikacja realizuje sekwencję niszczenia danych lokalnych:

1. **Unieważnienie tokenów:** Wywołanie `authRepository.signOut()` powoduje wysłanie żądania do
   Supabase Auth unieważniającego aktywny token odświeżania na serwerze oraz usunięcie lokalnych
   tokenów sesji z pamięci trwałej.
2. **Czyszczenie pamięci operacyjnej:** Zmienne przechowujące rolę, tożsamość i dane pacjenta są
   zerowane w stanie Composable (`loggedInRole = null`, `loggedInName = ""`), co powoduje
   natychmiastowe zniszczenie ekranu dashboardu przez Jetpack Compose i przejście do czystego
   `LoginScreen`.
3. **Czyszczenie buforów Coil:** Wyczyszczenie pamięci podręcznej Coil zapobiega pozostaniu podglądu
   badań w pamięci podręcznej po zalogowaniu innego użytkownika na tym samym telefonie.

---

## 4. Wnioski i Plan Wdrożeń Kryptograficznych na Kolejne Zajęcia (Z4)

Na obecnym etapie (Z3) aplikacja MobilnyPacjent spełnia wszystkie wymagania w zakresie:

* Braku nieuzasadnionego zapisu danych medycznych na dysku trwałym urządzenia,
* Wyłączenia niebezpiecznych kopii zapasowych (`allowBackup="false"`),
* Izolacji danych w piaskownicy systemowej,
* Bezpiecznego cyklu życia tokenów sesji i procedury wylogowania.

W ramach kolejnych zajęć (**Z4 – Pamięć lokalna i kryptografia**), zgodnie z MASVS-STORAGE-1 oraz
MASVS-CRYPTO, zrealizowane zostaną:

1. **Szyfrowanie tokenów sesji:** Wdrożenie biblioteki `androidx.security:security-crypto` i
   opakowanie przechowywania sesji w `EncryptedSharedPreferences`.
2. **Integracja z Android Keystore:** Generowanie klucza `MasterKey` chronionego biometrią
   użytkownika (`BiometricPrompt` / `BiometricManager`).
3. **Flaga `FLAG_SECURE`:** Dodanie ochrony przed zrzutami ekranu i podglądem w menedżerze ostatnich
   zadań na kluczowych ekranach medycznych.
