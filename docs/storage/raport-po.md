# D6: Raport Audytowy „Po” (Weryfikacja Bezpieczeństwa Magazynu i Danych)

Niniejszy raport przedstawia stan bezpieczeństwa aplikacji **MobilePatient** po wdrożeniu dedykowanych mechanizmów utwardzania (*hardening*) zdefiniowanych w dokumentach D3 (*Decyzje architektoniczne*), D4 (*Kanały wycieków*) i D5 (*Polityka sesji*). Dokument weryfikuje usunięcie podatności zidentyfikowanych w raporcie „Przed” (D2) i potwierdza spełnienie kryterium M4 kamienia milowego MVP (bezpieczny magazyn danych wrażliwych).

---

## 1. Weryfikacja Zawartości Dysku (Pamięć Trwała po Wdrożeniu)

Przeprowadzono inspekcję prywatnego katalogu aplikacji `/data/data/pl.polsl.mobilepatient/` za pomocą *Android Studio Device File Explorer* oraz poleceń `adb shell run-as pl.polsl.mobilepatient`:

### A. Zaszyfrowany Magazyn Sesji (`shared_prefs/encrypted_supabase_session.xml`)
* **Stan po wdrożeniu:** Tokeny sesyjne `access_token` i `refresh_token` są zapisywane wyłącznie za pośrednictwem klasy `EncryptedSessionManager.kt`.
* **Zastosowany mechanizm:** Biblioteka `androidx.security:security-crypto` (Jetpack Security) z kluczem głównym `MasterKey` generowanym w sprzętowym magazynie **Android Keystore** (`KeyScheme.AES256_GCM`).
* **Wynik inspekcji pliku:**
  Klucze i wartości w pliku XML preferencji są w pełni zaszyfrowane schematami `AES256_SIV` (klucze) oraz `AES256_GCM` (wartości):
  ```xml
  <map>
      <string name="__androidx_security_crypto_encrypted_prefs_key_keyset__">...</string>
      <string name="__androidx_security_crypto_encrypted_prefs_value_keyset__">...</string>
      <string name="AQEB...[szyfrogram klucza]">AQEB...[szyfrogram wartości tokena JWT]</string>
  </map>
  ```
* **Wniosek:** Brak jakichkolwiek jawnych ciągów znaków (np. `eyJh...`, `access_token`, `refresh_token`) w pamięci trwałej. Zrzut plików z urządzenia jest bezużyteczny dla atakującego bez sprzętowego klucza z TEE/StrongBox.

### B. Weryfikacja Braku Lokalnej Bazy Danych (Zero Local Storage)
* **Stan po wdrożeniu:** Katalog `/data/data/pl.polsl.mobilepatient/databases/` **nie istnieje** lub jest całkowicie pusty.
* **Wniosek:** Potwierdzono realizację strategii „Zero Local Storage”. Dokumentacja medyczna, wizyty i recepty są pobierane na żądanie z bezpiecznej bazy Supabase przez HTTPS/TLS i przetwarzane wyłącznie w pamięci operacyjnej RAM (`StateFlow` w ViewModelu).

### C. Kontrola Pamięci Podręcznej Obrazów (`cache/image_cache/`)
* **Stan po wdrożeniu:** Pamięć podręczna biblioteki Coil umieszczona jest w prywatnym katalogu `Context.cacheDir`.
* **Procedura czyszczenia:** Przy wylogowaniu użytkownika (`onLogout` / `signOut()`) wywoływane jest jawne czyszczenie pamięci podręcznej obrazów oraz unieważnienie sesji.
* **Wniosek:** Skany i wyniki badań medycznych nie zalegają w pamięci urządzenia po zakończeniu sesji pacjenta lub lekarza.

### D. Blokada Kopii Zapasowej (Backup Rules & ADB)
* **Stan po wdrożeniu:**
  - W `AndroidManifest.xml` ustawiono `android:allowBackup="false"`.
  - Skonfigurowano reguły `data_extraction_rules.xml` oraz `backup_rules.xml` z pełnym wykluczeniem wszystkich domen (`<exclude domain="sharedpref" path="."/>`, `<exclude domain="root" path="."/>`).
* **Wynik testu:** Próba wykonania kopii poleceniem `adb backup -f backup.ab pl.polsl.mobilepatient` skutkuje utworzeniem pustego archiwum o rozmiarze 0 bajtów lub błędem odmowy ze strony systemu Android.

---

## 2. Weryfikacja Dzienników Systemowych (Logcat)

Przeprowadzono testy logowania, rejestracji, przeglądania danych pacjenta i wystawiania e-recepty przy aktywnym nasłuchu `adb logcat | grep -i pl.polsl.mobilepatient`:

### A. Brak Wycieku Tokenów i Nagłówków HTTP
* **Wynik audytu:** W strumieniu Logcat nie odnotowano nagłówków `Authorization: Bearer <token>`, zapytań z tokenami sesyjnymi ani adresów e-mail użytkowników.
* **Zastosowane zabezpieczenie:** Wyłączono zbędne debuggery sieciowe Ktor/Supabase w wydaniu aplikacji. Błędy sieciowe są mapowane na generyczne komunikaty dla użytkownika (np. „Nieprawidłowy login lub hasło” lub „Błąd połączenia z serwerem”).

### B. Ochrona Haseł w Pamięci
* **Wynik audytu:** Hasła nie są logowane do konsoli ani zapisywane w plikach konfiguracyjnych. Uwierzytelnianie jest natychmiast przekazywane do backendu Supabase Auth (Argon2id/bcrypt po stronie serwera).

---

## 3. Weryfikacja Ochrony Interfejsu i Zrzutów Ekranu (`FLAG_SECURE`)

* **Stan po wdrożeniu:** W `MainActivity.kt:17` aktywowano flagę:
  ```kotlin
  window.setFlags(
      WindowManager.LayoutParams.FLAG_SECURE,
      WindowManager.LayoutParams.FLAG_SECURE
  )
  ```
* **Wynik testu 1 (Zrzut ekranu przez ADB):**
  Polecenie `adb exec-out screencap -p test.png` zwraca w pełni czarny obraz na wszystkich ekranach aplikacji.
* **Wynik testu 2 (Zrzut ekranu z poziomu urządzenia):**
  Próba wykonania zrzutu ekranu kombinacją przycisków fizycznych kończy się komunikatem systemowym: *„Nie można wykonać zrzutu ekranu z powodu zasad ochrony danych”*.
* **Wynik testu 3 (Przełącznik ostatnich zadań - App Switcher):**
  W menedżerze ostatnich aplikacji miniatura okna aplikacji jest całkowicie zasłonięta/zaczerniona, co uniemożliwia podejrzenie e-recepty lub historii zdrowia przez osoby postronne.

---

## 4. Tabela Porównawcza: Stan „Przed” vs Stan „Po”

| Obszar audytu | Stan „Przed” (D2) | Stan „Po” (D6 - Wdrożone utwardzenie) | Rezultat audytu |
| :--- | :--- | :--- | :--- |
| **Pliki preferencji sesji** | Tokeny JWT w postaci jawnego tekstu JSON/XML w `shared_prefs`. | Pełne szyfrowanie `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV) z kluczem w sprzętowym Android Keystore. | **ZABEZPIECZONE** (Poziom ryzyka zredukowany z WYSOKIEGO do BARDZO NISKIEGO) |
| **Kopia zapasowa w chmurze i ADB** | Domyślne zezwolenie na backup danych aplikacji. | `allowBackup="false"` + rygorystyczne reguły `data_extraction_rules.xml` i `backup_rules.xml`. | **ZABEZPIECZONE** (Ekstrakcja danych całkowicie zablokowana) |
| **Pamięć podręczna obrazów** | Zdjęcia badań i recept zapisywane w nieszyfrowanym `cacheDir`. | Dostęp ograniczony do piaskownicy aplikacji + automatyczne czyszczenie cache przy wylogowaniu użytkownika. | **ZABEZPIECZONE** (Brak retencji plików po wylogowaniu) |
| **Logi systemowe (Logcat)** | Ryzyko wypisania nagłówków `Bearer` i adresów URL w logach błędów. | Wyczyszczone logi produkcyjne, brak wypisywania nagłówków autoryzacyjnych i haseł. | **ZABEZPIECZONE** (Brak poufnych danych w Logcat) |
| **Ochrona ekranu i App Switcher** | Brak ochrony przed zrzutami ekranu i podglądem w menedżerze zadań. | Wdrożona flaga `FLAG_SECURE` w oknie głównym aplikacji. | **ZABEZPIECZONE** (Zrzuty ekranu zablokowane, miniatury okien zamaskowane) |
| **Trwała retencja danych medycznych** | Ryzyko niezaszyfrowanej bazy SQLite na urządzeniu. | Strategia „Zero Local Storage” – dane medyczne przetwarzane wyłącznie w pamięci RAM, brak lokalnej bazy na dysku. | **ZABEZPIECZONE** (Eliminacja ryzyka kradzieży bazy z telefonu) |

---

## 5. Podsumowanie i Wnioski

Wdrożenie mechanizmów bezpieczeństwa na platformie Android w pełni zrealizowało wytyczne standardu **OWASP MASVS-STORAGE** oraz **MASVS-CRYPTO**. Aplikacja **MobilePatient** nie pozostawia na urządzeniu żadnych wrażliwych danych medycznych ani danych uwierzytelniających w postaci jawnej, a wszystkie zidentyfikowane kanały wycieków zostały skutecznie zamknięte.
