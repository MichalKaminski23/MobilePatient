# Arkusz sekcji – „Jak to dotyczy naszej aplikacji?”

## Nagłówek

| Pole | Wartość |
|---|---|
| **Sekcja / aplikacja** | MobilnyPacjent |
| **Technologia** | Android (Kotlin, Jetpack Compose) + Supabase |
| **Zajęcia nr** | 5 |
| **Temat zajęć** | Kryptografia i kamień milowy MVP |
| **Obszar MASVS** | MASVS-CRYPTO |
| **Osoby obecne** | Michał Kamiński, Patryk Czakon |

---

## 0. Status z poprzedniego tygodnia

| Zadanie | Status (zrobione / częściowo / nie) | Dowód |
|---|---|---|
| **Wdrożenie `EncryptedSharedPreferences` / Android Keystore dla tokenów sesji (K1)** | Zrobione | Implementacja klasy `EncryptedSessionManager.kt` wykorzystującej `MasterKey` (`AES256_GCM`) w sprzętowym Android Keystore oraz `EncryptedSharedPreferences` (`AES256_SIV` dla kluczy, `AES256_GCM` dla wartości); tokeny JWT zapisywane jako szyfrogram w `/data/data/pl.polsl.mobilepatient/shared_prefs/encrypted_supabase_session.xml`. |
| **Zamknięcie kanałów wycieku: flaga `FLAG_SECURE` i weryfikacja logów (K2)** | Zrobione | Dodanie `window.setFlags(FLAG_SECURE, FLAG_SECURE)` w `MainActivity.kt:17`; blokada wykonywania zrzutów ekranu (`adb exec-out screencap` generuje pusty/czarny ekran), ukrycie miniatury w przełączniku zadań oraz weryfikacja braku poufnych nagłówków `Authorization` w Logcat. |
| **Działające MVP z podziałem ról (Pacjent / Lekarz / Admin) na demo Z5** | Zrobione | Działająca prezentacja (demo) na zajęciach: obsługa kont pacjenta, lekarza oraz administratora, przeglądanie i dodawanie dokumentacji medycznej, e-recept i placówek, z pełną egzekucją uprawnień PostgreSQL Row-Level Security (RLS) w `04-polityki.sql`. |
| **Zapis kompletu dokumentacji D1–D5 i arkusza w repozytorium** | Zrobione | Komplet plików `docs/storage/inwentaryzacja.md`, `docs/storage/raport-przed.md`, `docs/storage/decyzje.md`, `docs/storage/kanaly-wyciekow.md`, `docs/storage/polityka-sesji.md` oraz arkusza `docs/arkusze/Z4 - Bezpieczne_przechowywanie_danych.md` umieszczony w repozytorium. |

---

## 1. Gdzie to zagrożenie występuje u nas?

| Miejsce w aplikacji | Jakie dane lub funkcja są zagrożone | Klasyfikacja danych |
|---|---|---|
| **Lokalny magazyn sesji aplikacji (`EncryptedSessionManager.kt` / Android Keystore)** | Tokeny uwierzytelniające JWT (`access_token`, `refresh_token`); ryzyko kradzieży tożsamości w przypadku użycia słabego algorytmu szyfrowania, klucza zapisanego na stałe w kodzie lub przewidywalnego wektora IV. | Krytyczne / Poufne |
| **Kanał transportowy sieci (`SupabaseProvider.kt` / Ktor / OkHttp)** | Całość przesyłanych danych medycznych (diagnozy, e-recepty, wizyty, dane osobowe); ryzyko podsłuchu (sniffing) lub ataku Man-in-the-Middle (MitM) w przypadku nieszyfrowanego ruchu HTTP lub braku walidacji certyfikatów. | Poufne / Szczególnie wrażliwe |
| **Mechanizm uwierzytelniania i haseł (`AuthRepository.kt` / Supabase Auth)** | Hasła użytkowników (pacjentów, lekarzy, administratorów); ryzyko użycia słabych funkcji skrótu (np. MD5, SHA-1/SHA-256 bez soli) podatnych na ataki słownikowe i tablice tęczowe. | Krytyczne / Poufne |
| **Magazyn skanów medycznych w chmurze (Supabase Storage / bucket `medical-scans`)** | Pliki graficzne i dokumenty PDF wyników badań i recept; ryzyko nieuprawnionego wglądu w spoczynku (at rest) oraz modyfikacji plików bez wykrycia naruszenia integralności. | Szczególnie wrażliwe |
| **Generatory wartości losowych (IV, nonce, tokeny sesji i resetu hasła)** | Wektory inicjalizacyjne IV dla trybu AES-GCM oraz identyfikatory sesji; ryzyko powtórzenia IV przy tym samym kluczu (utrata poufności AES-GCM) w razie użycia generatora niekryptograficznego. | Krytyczne / Integralność |

---

## 2. Co już mamy?

* **Sprzętowe szyfrowanie sesji (Android Keystore + AEAD):** Wdrożono `EncryptedSessionManager.kt` oparty na bibliotece Jetpack Security (`androidx.security:security-crypto`). Klucz `MasterKey` generowany jest sprzętowo w module TEE/StrongBox (AES-256-GCM), a klucze i wartości preferencji są szyfrowane schematami AES-256-SIV i AES-256-GCM z automatycznie losowanym, unikalnym wektorem IV.
* **Wymuszenie bezpiecznej transmisji sieciowej:** W `AndroidManifest.xml` ustawiono `android:usesCleartextTraffic="false"`, co całkowicie blokuje nieszyfrowany ruch HTTP. Cała komunikacja z Supabase API, Auth oraz Storage odbywa się wyłącznie przez szyfrowany protokół TLS 1.3 / HTTPS.
* **Nowoczesne haszowanie haseł po stronie serwera:** Aplikacja nie przetwarza ani nie haszuje haseł lokalnie. Uwierzytelnianie jest oddelegowane do silnika Supabase Auth, który stosuje bezpieczne, odporne na akcelerację GPU funkcje skrótu (Argon2id/bcrypt) z indywidualną solą kryptograficzną dla każdego konta.
* **Szyfrowanie danych w spoczynku w chmurze:** Pliki skanów medycznych przesyłane do Supabase Storage podlegają szyfrowaniu po stronie serwera (Server-Side Encryption AES-256), a dostęp do nich jest restrykcyjnie kontrolowany tokenem JWT i politykami RLS (`docs/baza danych/04-polityki.sql`).
* **Kryptograficzna losowość:** Zastosowanie systemowych implementacji `SecureRandom` wbudowanych w Android Keystore i bibliotekę Tink eliminuje ryzyko przewidywalności wektorów IV i wartości jednorazowych.
* **Ochrona interfejsu (`FLAG_SECURE`):** Aktywność aplikacji posiada włączoną flagę `FLAG_SECURE`, co uniemożliwia rejestrowanie ekranu przez złośliwe aplikacje oraz wyklucza podgląd okna w menedżerze ostatnich aplikacji.
* **Działające MVP (kamień milowy Z5):** Aplikacja spełnia wszystkie kryteria M1–M5: pełna ścieżka od logowania, przez podział ról, po operacje medyczne z audytem i RLS.

---

## 3. Czego brakuje?

| Luka / zagrożenie | Gdzie | Ryzyko (niskie / średnie / wysokie) | Dlaczego takie ryzyko |
|---|---|---|---|
| **Brak mechanizmu Certificate Pinning (przyszpilania certyfikatu SSL/TLS)** | Warstwa sieciowa aplikacji (Ktor / OkHttp / Supabase Client) | **Średnie** | W przypadku zainstalowania przez użytkownika złośliwego certyfikatu CA w magazynie zaufanych certyfikatów systemowych (np. na zrootowanym telefonie lub przez proxy typu Burp Suite / mitmproxy) możliwa jest inspekcja i modyfikacja ruchu HTTPS. |
| **Brak lokalnego szyfrowania w spoczynku dla pamięci podręcznej obrazów (Coil cache)** | Prywatny katalog `cache/` aplikacji | **Średnie** | Choć katalog jest prywatny i kasowany przy wylogowaniu, pliki miniatur skanów w trakcie aktywnej sesji nie są zaszyfrowane na poziomie pliku kluczem z Keystore. |
| **Brak biometrycznego wymuszenia autoryzacji przed odblokowaniem klucza sesji** | Magazyn kluczy `MasterKey` / `EncryptedSessionManager` | **Średnie** | Dostęp do sesji ma każda osoba, która odblokuje telefon kodem PIN; brak konieczności ponownego potwierdzenia tożsamości biometrią przy operacjach wrażliwych. |
| **Brak jawnej obsługi unieważnienia klucza sprzętowego po zmianie zabezpieczeń telefonu** | Obsługa wyjątków w `EncryptedSessionManager.kt` | **Niskie** | Zmiana lub usunięcie blokady ekranu (PIN/odcisk palca) w ustawieniach systemu może trwale unieważnić klucz sprzętowy (`KeyPermanentlyInvalidatedException`), co wymaga płynnego zresetowania sesji i wymuszenia ponownego logowania zamiast awarii aplikacji. |

---

## 4. Jak to rozwiązuje nasza platforma?

| Mechanizm / biblioteka | Do czego go użyjemy | Pułapki i ograniczenia |
|---|---|---|
| **Android Keystore Provider + `androidx.security:security-crypto`** | Sprzętowa generacja i przechowywanie klucza głównego `MasterKey` w module TEE/StrongBox oraz obsługa szyfrowania `EncryptedSharedPreferences` (AES-256-GCM). | Klucz sprzętowy jest powiązany z urządzeniem i chroniony przed ekstrakcją; zmiana blokady ekranu może wymagać wygenerowania nowego klucza i ponownego logowania. |
| **Network Security Config (`network_security_config.xml`) / OkHttp `CertificatePinner`** | Ograniczenie zaufania wyłącznie do certyfikatów systemowych i opcjonalne przypięcie skrótów kluczy publicznych (SPKI) serwera Supabase. | Przyszpilenie certyfikatu bez zapasowych pinów (backup pins) grozi zablokowaniem aplikacji w przypadku rotacji certyfikatu na serwerze. |
| **BiometricPrompt API (`androidx.biometric:biometric`)** | Wymuszenie biometrycznego potwierdzenia tożsamości (odcisk palca / skan twarzy) przed udostępnieniem widoków medycznych lub odblokowaniem klucza w Keystore. | Wymaga obsługi urządzeń bez modułu biometrycznego (`BIOMETRIC_ERROR_NO_HARDWARE`) oraz urządzeń z nieskonfigurowaną biometrią. |
| **Supabase Auth API (Argon2id / bcrypt)** | Przeniesienie procesu haszowania i weryfikacji haseł całkowicie na zabezpieczony backend serwerowy. | Aplikacja operuje wyłącznie na krótkotrwałych tokenach JWT; wymaga bezpiecznego odświeżania tokena sesji (`refresh_token`). |
| **AEAD (Authenticated Encryption with Associated Data — AES-GCM)** | Zapewnienie jednoczesnej poufności danych oraz nienaruszalności (integralności) za pomocą 128-bitowego tagu uwierzytelniającego. | Wymaga bezwzględnego zachowania unikalności wektora IV dla każdego szyfrowania tym samym kluczem (automatycznie gwarantowane przez Android Keystore). |

---

## 5. Plan na kolejny tydzień

| Zadanie | Osoba | Jak udowodnimy, że działa | Pozycja w checkliście MASVS |
|---|---|---|---|
| **Wdrożenie BiometricPrompt API do biometrycznej autoryzacji sesji** | Michał Kamiński | Działające okno biometryczne przy starcie aplikacji i odblokowaniu sesji na emulatorze/telefonie; obsługa braku biometrii. | MASVS-AUTH-2 |
| **Implementacja wygasania sesji i automatycznego wylogowania po bezczynności** | Patryk Czakon | Test bezczynności (inactivity timeout) powodujący automatyczne przejście do ekranu blokady i wyczyszczenie wrażliwych stanów z pamięci RAM. | MASVS-AUTH-1 |
| **Egzekwowanie flagi `must_change_password` i polityka haseł** | Michał Kamiński | Test logowania na konto z wymuszoną zmianą hasła; blokada nawigacji do dashboardu do momentu nadania nowego, silnego hasła. | MASVS-AUTH-1 |
| **Opracowanie kompletnego arkusza i raportu Z6 (Uwierzytelnianie i sesje)** | Michał Kamiński, Patryk Czakon | Plik `docs/arkusze/Z6 - Uwierzytelnianie_i_sesje.md` umieszczony w repozytorium. | Wymóg formalny Z6 |

---

## 6. Pytania do prowadzącego

* Brak

---