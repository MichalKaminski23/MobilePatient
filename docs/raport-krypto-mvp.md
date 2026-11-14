# Raport bezpieczeństwa — MobilePatient (Sekcja Lekarz-Pacjent)

## 1. Kryptografia w aplikacji

### 1.1 Tabela kryptografii
| Miejsce w aplikacji | Zadanie | Co jest teraz (plik:linia) | Klucz: skąd i gdzie | Problem | Decyzja i źródło |
|---|---|---|---|---|---|
| Przechowywanie sesji / tokenów JWT | Poufność | `EncryptedSessionManager.kt:28` — `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV) | MasterKey w Android Keystore (sprzętowy TEE/StrongBox) | Poprzednio brak szyfrowania (SharedPreferences w plaintext); teraz bezpiecznie | AES-256-GCM, klucz w Android Keystore (OWASP MASTG-STORAGE-5, dokumentacja Android Jetpack Security) |
| Komunikacja z backendem (Supabase API / Auth / Storage) | Poufność i Autentyczność | HTTPS (TLS 1.3 / OkHttp / Ktor) | Certyfikat serwera SSL/TLS zaufany przez system | Brak szyfrowania transportu narażałby na MitM | TLS 1.3 / HTTPS (OWASP MASTG-NETWORK-1, dokumentacja Supabase / Ktor) |
| Hasła użytkowników (Rejestracja / Logowanie) | Ochrona haseł | Supabase Auth (Argon2 / bcrypt po stronie serwera Supabase) | Klucz hashowania / sól na bezpiecznym serwerze bazodanowym Supabase | Hasła nigdy nie są haszowane lokalnie ani przechowywane na urządzeniu | Użycie funkcji skrótu odpornej na ataki brute-force/GPU (Argon2id/bcrypt) po stronie serwera (OWASP MASTG-AUTH-1) |
| Generowanie tokenów / IV / Nonce | Losowość | `java.security.SecureRandom` (domyślnie w Android KeyStore / Supabase SDK) | Kryptograficznie bezpieczne źródło entropii OS (RNG) | Użycie słabych generatorów (`java.util.Random` / `Math.random`) mogłoby przewidzieć tokeny | `SecureRandom` / Android Keystore (OWASP MASTG-CRYPTO-4) |
| Pliki i skany medyczne (Storage) | Poufność i Integralność | Supabase Storage (szyfrowanie w spoczynku AES-256 po stronie serwera + HTTPS podczas przesyłania) | Klucz zarządzany przez dostawcę cloud (Supabase / AWS S3) | Przechowywanie skanów bez kontroli dostępu lub szyfrowania | HTTPS + Signed URLs + RLS (Row Level Security) w bazie Supabase (OWASP MASTG-STORAGE-2 / STORAGE-5) |

### 1.2 Kategorie błędów
| Kategoria | Wynik | Jak szukaliśmy |
|---|---|---|
| Klucz w kodzie | sprawdzone, brak | Przeszukiwanie kodu pod kątem stałych kluczy (`api key`, `secret`, `AES`), dekompilacja JADX / inspekcja kodu źródłowego |
| Przestarzały algorytm lub tryb | sprawdzone, brak | Przegląd używanych trybów szyfrowania (użyto AES-256-GCM / AES-256-SIV przez `EncryptedSharedPreferences`) |
| Przewidywalna losowość | sprawdzone, brak | Inspekcja użycia generatorów losowych (brak `java.util.Random` / `Math.random` do celów kryptograficznych; oparcie na `SecureRandom` / Keystore) |
| Powtarzane wartości jednorazowe | sprawdzone, brak | Weryfikacja generowania IV/nonce (Android Keystore i AES-GCM automatycznie losują unikalne IV dla każdej operacji szyfrowania) |
| Hasło jako zwykły skrót | sprawdzone, brak | Uwierzytelnianie i zarządzanie hasłami całkowicie oddelegowane do Supabase Auth (bezpieczny Argon2id/bcrypt na serwerze, brak lokalnego haszowania haseł) |
| Szyfrowanie bez integralności | sprawdzone, brak | Użycie szyfrowania z uwierzytelnieniem (AES-GCM w `EncryptedSharedPreferences`, co gwarantuje zarówno poufność, jak i wykrycie modyfikacji danych) |

### 1.3 Uzasadnienie decyzji
1. **Poufność tokenów sesyjnych (EncryptedSharedPreferences / Android Keystore):**
   1. *Jakie zadanie?* Poufność (ochrona tokenów JWT i danych sesji przed kradzieżą z urządzenia).
   2. *Czy wybór jest dziś zalecany?* Tak, AES-256-GCM oraz Android Keystore są oficjalnie zalecane przez OWASP MASTG (MASTG-STORAGE-5) oraz dokumentację Android Security.
   3. *Skąd klucz, gdzie leży, kto go używa?* Klucz główny (`MasterKey`) jest generowany i bezpiecznie przechowywany w sprzętowym magazynie Android Keystore (TEE / StrongBox); aplikacja używa go wyłącznie za pośrednictwem `EncryptedSharedPreferences`.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, `EncryptedSharedPreferences` (AES-256-GCM) generuje i zapisuje unikalne IV dla każdej operacji szyfrowania wartości.
   5. *Czy wykryjemy modyfikację danych?* Tak, tryb GCM (Galois/Counter Mode) zapewnia uwierzytelnianie (AEAD), dzięki czemu każda próba modyfikacji zaszyfrowanych danych w plikach preferencji zostanie wykryta i odrzucona.
   6. *Co, jeśli klucz wycieknie?* Klucz główny jest powiązany ze sprzętem urządzenia i chroniony przez Android Keystore (nie da się go wyeksportować z pamięci Non-Root). W przypadku utraty urządzenia sesja zostaje unieważniona na serwerze Supabase.

2. **Komunikacja sieciowa i API (TLS 1.3 / HTTPS):**
   1. *Jakie zadanie?* Poufność i autentyczność transmisji danych między aplikacją mobilną a backendem Supabase.
   2. *Czy wybór jest dziś zalecany?* Tak, HTTPS / TLS 1.3 to branżowy standard zalecany przez OWASP MASTG (MASTG-NETWORK-1).
   3. *Skąd klucz, gdzie leży, kto go używa?* Certyfikaty i klucze sesji TLS są negocjowane dynamicznie podczas handshake'u TLS między klientem a serwerem Supabase.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, protokół TLS gwarantuje unikalność jednorazowych wartości (nonces/IVs) w każdej sesji szyfrowanej transportu.
   5. *Czy wykryjemy modyfikację danych?* Tak, protokół TLS zapewnia nienaruszalność i integrylność pakietów sieciowych (MAC/AEAD).
   6. *Co, jeśli klucz wycieknie?* Klucze sesji TLS są ulotne (Perfect Forward Secrecy); nawet w przypadku kompromitacji długoterminowego klucza serwera, przeszłe sesje pozostają bezpieczne.

3. **Uwierzytelnianie i hasła (Supabase Auth):**
   1. *Jakie zadanie?* Ochrona haseł i autentyczność użytkowników (pacjentów, lekarzy, administratorów).
   2. *Czy wybór jest dziś zalecany?* Tak, delegowanie uwierzytelniania do Supabase Auth z bezpiecznym haszowaniem po stronie serwera jest zalecane przez OWASP MASTG (MASTG-AUTH-1).
   3. *Skąd klucz, gdzie leży, kto go używa?* Hasła użytkowników są haszowane z solą na bezpiecznym serwerze bazodanowym Supabase; aplikacja przesyła dane wyłącznie przez HTTPS i otrzymuje token JWT sesji.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, sól (salt) generowana przy hashowaniu haseł jest unikalna dla każdego użytkownika.
   5. *Czy wykryjemy modyfikację danych?* Tak, żądania autoryzacyjne są podpisane tokenami JWT i zabezpieczone przez TLS oraz Row Level Security (RLS).
   6. *Co, jeśli klucz wycieknie?* Hasła w bazie są bezpiecznie zahaszowane (Argon2id/bcrypt), zapobiegając atakom tęczowym i słownikowym; tokeny JWT mają ograniczony czas życia (TTL) i mogą być unieważnione na serwerze.

4. **Kryptografia plików / Skany medyczne (Supabase Storage + RLS):**
   1. *Jakie zadanie?* Poufność i autentyczność dostępu do dokumentacji medycznej i skanów badań.
   2. *Czy wybór jest dziś zalecany?* Tak, szyfrowanie w spoczynku po stronie chmury oraz kontrola dostępu oparta na politykach RLS (Row Level Security) są zalecane przez OWASP MASTG (MASTG-STORAGE-2).
   3. *Skąd klucz, gdzie leży, kto go używa?* Klucze szyfrowania magazynu są zarządzane przez infrastrukturę chmurową dostawcy (Supabase/AWS S3); dostęp jest autoryzowany tokenem JWT użytkownika.
   4. *Czy wartości jednorazowe się nie powtarzają?* Tak, dostawca chmury zapewnia poprawne szyfrowanie blokowe w spoczynku.
   5. *Czy wykryjemy modyfikację danych?* Tak, mechanizmy sum kontrolnych i TLS gwarantują poprawność przesyłanych plików.
   6. *Co, jeśli klucz wycieknie?* Pliki są zabezpieczone politykami RLS i wymagają uprawnień przypisanych do konta pacjenta lub lekarza prowadzącego.

## 2. Stan aplikacji i plan

### Werdykt MVP: **Tak**
*Największe ryzyko:* Ewentualne próby nieautoryzowanego dostępu do endpointów API są w pełni mitygowane przez rygorystyczne polityki RLS (Row Level Security) w bazie danych oraz bezpieczne przechowywanie tokenów sesji w Android Keystore.

### Tabela stanu aplikacji
| Obszar | Status | Największe ryzyko |
|---|---|---|
| Funkcje (MVP) | Zaimplementowane | Brak pełnego trybu offline (wymagane połączenie z internetem/Supabase) |
| Model zagrożeń | Opracowany w `docs/threat-model/` | Zmiany w architekturze wymagają bieżącej aktualizacji diagramów DFD |
| Uprawnienia | Skonfigurowane i przetestowane | Odmowa uprawnień (np. aparat, powiadomienia, biometria) w runtime przez użytkownika |
| Przechowywanie danych | Zabezpieczone  | Utrata klucza głównego Keystore w rzadkich przypadkach uszkodzenia bezpiecznego magazynu sprzętowego |
| Kryptografia | Zweryfikowana  | Użycie wyłącznie standardowych bibliotek (EncryptedSharedPreferences, Supabase Auth, TLS) |
