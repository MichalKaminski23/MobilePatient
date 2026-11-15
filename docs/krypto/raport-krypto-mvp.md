# Raport bezpieczeństwa — MobilePatient (Sekcja Lekarz-Pacjent)

## 1. Kryptografia w aplikacji

### 1.1 Tabela kryptografii
| Miejsce w aplikacji | Zadanie | Co jest teraz (plik:linia) | Klucz: skąd i gdzie | Problem | Decyzja i źródło |
|---|---|---|---|---|---|
| Przechowywanie sesji / tokenów JWT | Poufność | `EncryptedSessionManager.kt:28` — `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV) | MasterKey w Android Keystore (sprzętowy TEE/StrongBox) | Poprzednio brak szyfrowania (SharedPreferences w plaintext); teraz bezpiecznie | AES-256-GCM, klucz w Android Keystore (OWASP MASTG-STORAGE-5, MASTG-CRYPTO-1, dokumentacja Android Jetpack Security) |
| Komunikacja z backendem (Supabase API / Auth / Storage) | Poufność i Autentyczność | `SupabaseProvider.kt:27` — HTTPS (TLS 1.3 / OkHttp / Ktor) oraz `AndroidManifest.xml:13` (`usesCleartextTraffic="false"`) | Certyfikat serwera SSL/TLS zaufany przez system, klucze sesji negocjowane dynamicznie (ECDHE) | Brak szyfrowania transportu (HTTP) narażałby na podsłuch i atak Man-in-the-Middle (MitM) | Wymuszenie TLS 1.3 / HTTPS (OWASP MASTG-NETWORK-1, dokumentacja Supabase / Ktor) |
| Hasła użytkowników (Rejestracja / Logowanie) | Ochrona haseł | `AuthRepository.kt:18` — Supabase Auth (Argon2id / bcrypt po stronie serwera Supabase) | Klucz hashowania / unikalna sól per użytkownik na bezpiecznym serwerze bazodanowym Supabase | Hasła nigdy nie są haszowane lokalnie ani przechowywane w aplikacji; zwykły skrót (MD5/SHA) byłby podatny na rainbow tables | Użycie funkcji skrótu odpornej na ataki brute-force/GPU (Argon2id/bcrypt z solą) po stronie serwera (OWASP MASTG-AUTH-1) |
| Generowanie tokenów / IV / Nonce | Losowość | `EncryptedSessionManager.kt:28` — `java.security.SecureRandom` (domyślnie w Android KeyStore / Jetpack Security) | Kryptograficznie bezpieczne źródło entropii OS (`/dev/urandom` / CSPRNG) | Użycie słabych generatorów (`java.util.Random` / `Math.random`) mogłoby umożliwić przewidzenie wektorów IV i tokenów | `SecureRandom` / Android Keystore (OWASP MASTG-CRYPTO-4) |
| Pliki i skany medyczne (Storage) | Poufność i Integralność | `PatientRepository.kt:110` — Supabase Storage (szyfrowanie at-rest AES-256 + HTTPS + RLS w `04-polityki.sql:88`) | Klucz zarządzany przez dostawcę cloud (Supabase / AWS S3 KMS); autoryzacja tokenem JWT użytkownika | Przechowywanie skanów bez kontroli dostępu lub w postaci nieszyfrowanej | Szyfrowanie po stronie serwera (SSE AES-256) + Signed URLs + polityki RLS w bazie Supabase (OWASP MASTG-STORAGE-2 / STORAGE-5) |

### 1.2 Kategorie błędów
| Kategoria | Wynik | Jak szukaliśmy |
|---|---|---|
| Klucz w kodzie | sprawdzone, brak | Przeszukiwanie kodu pod kątem stałych kluczy (`api key`, `secret`, `AES`, `BEGIN PRIVATE KEY`), dekompilacja JADX / inspekcja kodu źródłowego i historii commitów Git |
| Przestarzały algorytm lub tryb | sprawdzone, brak | Przegląd używanych trybów szyfrowania (użyto AES-256-GCM / AES-256-SIV przez `EncryptedSharedPreferences`; brak przestarzałych DES, 3DES, RC4, MD5 czy trybu AES/ECB) |
| Przewidywalna losowość | sprawdzone, brak | Inspekcja użycia generatorów losowych (brak `java.util.Random` / `Math.random` do celów kryptograficznych; oparcie na systemowym `SecureRandom` i Android Keystore) |
| Powtarzane wartości jednorazowe | sprawdzone, brak | Weryfikacja generowania IV/nonce (Android Keystore i AES-GCM automatycznie losują unikalne, 96-bitowe IV dla każdej operacji szyfrowania wartości) |
| Hasło jako zwykły skrót | sprawdzone, brak | Uwierzytelnianie i zarządzanie hasłami całkowicie oddelegowane do Supabase Auth (bezpieczny Argon2id/bcrypt z solą na serwerze, brak lokalnego haszowania haseł) |
| Szyfrowanie bez integralności | sprawdzone, brak | Użycie szyfrowania z uwierzytelnieniem (AEAD — AES-256-GCM w `EncryptedSharedPreferences`, co gwarantuje zarówno poufność, jak i wykrycie modyfikacji danych) |

### 1.3 Uzasadnienie decyzji
1. **Poufność tokenów sesyjnych (EncryptedSharedPreferences / Android Keystore):**
   1. *Jakie zadanie?* Poufność (ochrona tokenów JWT i danych sesji przed kradzieżą z pamięci urządzenia).
   2. *Czy wybór jest dziś zalecany?* Tak, AES-256-GCM oraz Android Keystore są oficjalnie zalecane przez OWASP MASTG (MASTG-STORAGE-5, MASTG-CRYPTO-1) oraz dokumentację Android Security.
   3. *Skąd klucz, gdzie leży, kto go używa?* Klucz główny (`MasterKey`) jest generowany i bezpiecznie przechowywany w sprzętowym magazynie Android Keystore (TEE / StrongBox); aplikacja używa go wyłącznie za pośrednictwem `EncryptedSharedPreferences`.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, `EncryptedSharedPreferences` (AES-256-GCM) generuje i zapisuje unikalne IV dla każdej operacji szyfrowania wartości.
   5. *Czy wykryjemy modyfikację danych?* Tak, tryb GCM (Galois/Counter Mode) zapewnia uwierzytelnianie (AEAD), dzięki czemu każda próba modyfikacji zaszyfrowanych danych w plikach preferencji zostanie wykryta i odrzucona (błąd tagu uwierzytelniającego).
   6. *Co, jeśli klucz wycieknie?* Klucz główny jest powiązany ze sprzętem urządzenia i chroniony przez Android Keystore (nie da się go wyeksportować z pamięci bez uprawnień roota na poziomie jądra i luki w TEE). W przypadku kompromitacji urządzenia sesja zostaje unieważniona na serwerze Supabase.

2. **Komunikacja sieciowa i API (TLS 1.3 / HTTPS):**
   1. *Jakie zadanie?* Poufność i autentyczność transmisji danych między aplikacją mobilną a backendem Supabase.
   2. *Czy wybór jest dziś zalecany?* Tak, HTTPS / TLS 1.3 to branżowy standard zalecany przez OWASP MASTG (MASTG-NETWORK-1).
   3. *Skąd klucz, gdzie leży, kto go używa?* Certyfikaty i klucze sesji TLS są negocjowane dynamicznie podczas handshake'u TLS (ECDHE) między klientem a serwerem Supabase.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, protokół TLS gwarantuje unikalność jednorazowych wartości (nonces/IVs) w każdej sesji szyfrowanej transportu.
   5. *Czy wykryjemy modyfikację danych?* Tak, protokół TLS zapewnia nienaruszalność i integralność pakietów sieciowych (MAC/AEAD).
   6. *Co, jeśli klucz wycieknie?* Klucze sesji TLS są ulotne (Perfect Forward Secrecy); nawet w przypadku kompromitacji długoterminowego klucza serwera, przeszłe sesje pozostają bezpieczne.

3. **Uwierzytelnianie i hasła (Supabase Auth):**
   1. *Jakie zadanie?* Ochrona haseł i autentyczność użytkowników (pacjentów, lekarzy, administratorów).
   2. *Czy wybór jest dziś zalecany?* Tak, delegowanie uwierzytelniania do Supabase Auth z bezpiecznym haszowaniem po stronie serwera jest zalecane przez OWASP MASTG (MASTG-AUTH-1).
   3. *Skąd klucz, gdzie leży, kto go używa?* Hasła użytkowników są haszowane z kryptograficzną solą na bezpiecznym serwerze bazodanowym Supabase; aplikacja przesyła dane wyłącznie przez HTTPS i otrzymuje token JWT sesji.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, sól (salt) generowana przy hashowaniu haseł jest kryptograficznie unikalna dla każdego użytkownika.
   5. *Czy wykryjemy modyfikację danych?* Tak, żądania autoryzacyjne są podpisane tokenami JWT i zabezpieczone przez TLS oraz Row Level Security (RLS).
   6. *Co, jeśli klucz wycieknie?* Hasła w bazie są bezpiecznie zahaszowane wolną funkcją skrótu (Argon2id/bcrypt), zapobiegając atakom tęczowym i słownikowym; tokeny JWT mają ograniczony czas życia (TTL) i mogą być natychmiast unieważnione na serwerze.

4. **Kryptografia plików / Skany medyczne (Supabase Storage + RLS):**
   1. *Jakie zadanie?* Poufność i autentyczność dostępu do dokumentacji medycznej i skanów badań.
   2. *Czy wybór jest dziś zalecany?* Tak, szyfrowanie w spoczynku po stronie chmury (SSE AES-256) oraz kontrola dostępu oparta na politykach RLS (Row Level Security) są zalecane przez OWASP MASTG (MASTG-STORAGE-2).
   3. *Skąd klucz, gdzie leży, kto go używa?* Klucze szyfrowania magazynu są zarządzane przez infrastrukturę chmurową dostawcy (Supabase/AWS S3 KMS); dostęp jest autoryzowany tokenem JWT użytkownika.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, dostawca chmury zapewnia poprawne szyfrowanie blokowe w spoczynku z unikalnymi wektorami IV.
   5. *Czy wykryjemy modyfikację danych?* Tak, mechanizmy sum kontrolnych i TLS gwarantują poprawność przesyłanych plików.
   6. *Co, jeśli klucz wycieknie?* Pliki są zabezpieczone politykami RLS w bazie danych i wymagają uprawnień przypisanych do konta pacjenta lub przypisanego lekarza prowadzącego.

## 2. Stan aplikacji i plan

### Werdykt MVP: **Tak**
*Największe ryzyko:* Ewentualne próby nieautoryzowanego dostępu do endpointów API lub zasobów są w pełni mitygowane przez rygorystyczne polityki RLS (Row Level Security) w silniku PostgreSQL oraz bezpieczne przechowywanie tokenów sesji w sprzętowym magazynie Android Keystore.

### Tabela stanu aplikacji
| Obszar | Status | Największe ryzyko |
|---|---|---|
| Funkcje (MVP) | Zaimplementowane | Brak pełnego trybu offline (aplikacja wymaga połączenia z internetem i backendem Supabase) |
| Build z repozytorium | Zweryfikowany | Wymóg skonfigurowania kluczy Supabase w pliku `local.properties` (lub zmiennych środowiskowych) przy pierwszym klonowaniu repozytorium |
| Model zagrożeń | Opracowany w `docs/arkusze/threat-model/` | Zmiany w architekturze i dodawanie nowych endpointów wymagają bieżącej aktualizacji diagramów DFD i STRIDE |
| Uprawnienia | Skonfigurowane i przetestowane | Odmowa uprawnień runtime (aparat, powiadomienia, lokalizacja) przez użytkownika wymagająca graceful degradation w UI |
| Przechowywanie danych | Zabezpieczone | Usunięcie lub rekonfiguracja blokady ekranu (PIN/biometria) przez użytkownika unieważniająca klucz w Keystore |
| Kryptografia | Zweryfikowana | Wykorzystanie wyłącznie sprawdzonych bibliotek i algorytmów (EncryptedSharedPreferences AES-256-GCM, Supabase Auth, TLS 1.3) |

### Plan do zajęć 6 (Uwierzytelnianie i sesje / MASVS-AUTH)
| Zadanie | Kto | Termin | Sposób sprawdzenia |
|---|---|---|---|
| **Wdrożenie BiometricPrompt API** (ochrona odblokowania aplikacji i sesji biometrią) | Michał Kamiński | Przed Z6 | Działające uwierzytelnienie odciskiem palca na emulatorze/telefonie z poprawną obsługą braku sprzętu biometrycznego |
| **Mechanizm wygasania sesji po bezczynności** (inactivity timeout) | Patryk Czakon | Przed Z6 | Test bezczynności powodujący automatyczne zablokowanie widoków medycznych i wymóg ponownej autoryzacji |
| **Egzekwowanie flagi `must_change_password`** oraz walidacja złożoności haseł | Michał Kamiński | Przed Z6 | Test pierwszego logowania na konto tymczasowe; blokada nawigacji do momentu ustawienia nowego, silnego hasła |
| **Opracowanie arkusza i raportu Z6** z obszaru MASVS-AUTH | Michał Kamiński, Patryk Czakon | Przed Z6 | Kompletny plik `docs/arkusze/Z6 - Uwierzytelnianie_i_sesje.md` w repozytorium |
