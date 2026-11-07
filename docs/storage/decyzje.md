# D3: Decyzje o Magazynie Danych (Storage Architecture Decisions)

Niniejszy dokument przedstawia formalne decyzje architektoniczne dotyczące wyboru magazynów danych dla poszczególnych kategorii informacji przetwarzanych w aplikacji **MobilePatient**, wraz z konfiguracją dostępności oraz uzasadnieniem technicznym i prawnym.

---

## 1. Macierz Decyzji Architektonicznych

| Dana | Wybrany Magazyn | Konfiguracja / Opcja Dostępności | Uzasadnienie Techniczne i Bezpieczeństwa |
| :--- | :--- | :--- | :--- |
| **Tokeny Autoryzacyjne (JWT)** | **EncryptedSharedPreferences** / **Android KeyStore** | Klucz główny `MasterKey` (AES-256 GCM, weryfikowany sprzętowo w TEE/HSM). | Chroni tokeny sesyjne przed odczytem na urządzeniach z rootem oraz w przypadku wyciągnięcia pamięci. Umożliwia bezpieczne automatyczne odnawianie sesji. |
| **Hasło i Dane Formularza Logowania** | **Pamięć Tymczasowa RAM** (Brak zapisu dyskowego) | Zmienne krótkotrwałe w pamięci RAM procesu. Czyszczone po zakończeniu żądania. | Hasła **nigdy** nie powinny trafiać na dysk urządzenia. Przechowywanie wyłącznie w RAM minimalizuje ryzyko kradzieży danych logowania. |
| **Dokumentacja Medyczna & E-Recepty** | **Pamięć Operacyjna RAM (`StateFlow` / ViewModel)** | Brak pamięci trwałej (Zero Local Disk Retention). Pobieranie wyłącznie przez HTTPS/TLS. | Eliminacja ryzyka wycieku wrażliwych danych medycznych w przypadku kradzieży lub zgubienia telefonu. Dane podlegają kontroli dostępu w czasie rzeczywistym poprzez Supabase RLS. |
| **Profile Pacjentów & Lekarzy** | **Pamięć Operacyjna RAM** | Brak zapisu na dysku. Przechowywanie w instancji ViewModelu. | Dane PII są pobierane dynamicznie z bazy w chmurze i natychmiast usuwane z pamięci po wylogowaniu lub zamknięciu widoku. |
| **Pamięć Podręczna Zdjęć Dokumentów** | **Katalog Prywatny `Context.cacheDir`** | Pliki tymczasowe czyszczone automatycznie przy wylogowaniu oraz przez system Android. | Zapewnia wysoką wydajność wyświetlania podglądów zdjęć dokumentacji medycznej przy zachowaniu ochrony przed dostępem innych aplikacji. |
| **Ustawienia i Flagi Uprawnień** | **Zarządzane przez System Android (`PackageManager`)** | Zapytania do systemowego API w czasie rzeczywistym (`checkSelfPermission`). | Brak konieczności dublowania stanów uprawnień na dysku; zapobiega desynchronizacji ze stanem faktycznym w systemie. |

---

## 2. Naświetlenie Wybranych Technologii Zabezpieczeń

### A. Android KeyStore & EncryptedSharedPreferences
Dla danych sesyjnych stosowany jest standard **Jetpack Security (Security-Crypto)**:
* **MasterKey:** Tworzony w oparciu o bezpieczny moduł sprzętowy urządzenia (*Hardware-backed Trusted Execution Environment - TEE* lub *StrongBox*).
* **Szyfrowanie wartości:** Schemat `AES256_GCM` dla wartości oraz `AES256_SKEY_STREAM` dla kluczy.
* **Korzyść:** Uniemożliwia odczyt tokenów nawet w przypadku sklonowania partycji danych aplikacji.

### B. Strategia "Zero Local Storage" dla Danych Medycznych
Dla danych medycznych (`medical_records`, `prescriptions`, `appointments`) podjęto decyzję o **całkowitym odstąpieniu od lokalnej bazy danych SQLite/Room**:
* **Uzasadnienie:** Aplikacja medyczna działa w trybie połączonym z siecią. Przechowywanie bazy danych medycznych na dysku lokalnym stwarzałoby konieczność wdrożenia skomplikowanego szyfrowania całej bazy (np. SQLCipher), zarządzania kluczami i procedurami bezpiecznego czyszczenia.
* **Rozwiązanie:** Pobieranie danych w czasie rzeczywistym bezpośrednio z zaszyfrowanej chmury Supabase przy użyciu protokołu HTTPS i autoryzacji RLS.

---

## 3. Akceptacja Decyzji Architektonicznych

Wszystkie podjęte decyzje są zgodne z wytycznymi **OWASP Mobile Application Security Verification Standard (MASVS)** w obszarze *MASVS-STORAGE* (Ochrona danych na urządzeniu).
