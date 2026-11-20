# Arkusz sekcji – „Jak to dotyczy naszej aplikacji?”

## Nagłówek

| Pole                   | Wartość                                      |
|------------------------|----------------------------------------------|
| **Sekcja / aplikacja** | MobilnyPacjent                               |
| **Technologia**        | Android (Kotlin, Jetpack Compose) + Supabase |
| **Zajęcia nr**         | 6                                            |
| **Temat zajęć**        | Uwierzytelnianie i sesje                     |
| **Obszar MASVS**       | MASVS-AUTH                                   |
| **Osoby obecne**       | Michał Kamiński, Patryk Czakon               |

---

## 0. Status z poprzedniego tygodnia

| Zadanie                                                                        | Status (zrobione / częściowo / nie) | Dowód                                                                                                                                                                                                                                |
|--------------------------------------------------------------------------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Wdrożenie BiometricPrompt API do biometrycznej autoryzacji sesji**           | Zrobione                            | Stworzono klasę `BiometricAuthManager.kt` wykorzystującą flagę `BIOMETRIC_STRONG` zabezpieczającą przed słabszymi autentykatorami. Integracja z logowaniem biometrycznym znajduje się w `AuthRepository.kt` (`signInWithBiometric`). |
| **Implementacja wygasania sesji i automatycznego wylogowania po bezczynności** | Zrobione                            | Zaimplementowano moduł `SessionTimeoutManager.kt` śledzący aktywność użytkownika i blokujący/wylogowujący aplikację po upływie 15 minut bezczynności (`DEFAULT_TIMEOUT_MS = 15 * 60 * 1000L`).                                       |
| **Egzekwowanie flagi `must_change_password` i polityka haseł**                 | Zrobione                            | Uwierzytelnianie całkowicie zaimplementowane przez dostawcę zewnętrznego (Supabase Auth API), który wymusza własną politykę silnych haseł oraz zapobiega bezpośrednim atakom brute-force na aplikację kliencką.                      |
| **Utworzenie raportu sesji (Wymóg wykładu 6)**                                 | Zrobione                            | Utworzono plik `docs/raport-sesje.md` opisujący elementy sesji, diagram sekwencji logowania, politykę i powzięte decyzje deweloperskie z uzasadnieniem i wskazaniem na MASVS-AUTH.                                                   |

---

## 1. Gdzie to zagrożenie występuje u nas?

| Miejsce w aplikacji                                                             | Jakie dane lub funkcja są zagrożone                                                                                                                                         | Klasyfikacja danych              |
|---------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| **Pamięć podręczna logowania (Ekran logowania)**                                | Hasło wpisywane przez pacjenta, podatne na podglądnięcie (shoulder surfing) lub wyciek przy użyciu nieodpowiedniej warstwy widoku.                                          | Krytyczne / Poufne               |
| **Przechowywane lokalnie tokeny uwierzytelniające (`EncryptedSessionManager`)** | Token dostępu JWT (`access_token`) dający nieograniczony dostęp do danych medycznych użytkownika na czas swojej ważności (1 godzina) i token odświeżania (`refresh_token`). | Krytyczne / Poufne               |
| **Moduł odświeżania sesji i odblokowania aplikacji**                            | System uwierzytelniania biometrycznego - ewentualny brak kryptograficznego poświadczenia operacji (zależność na wartości zwracanej true/false).                             | Wysokie / Integralność           |
| **Aktywny stan aplikacji (pozostawienie na włączonym ekranie)**                 | Dostęp do całej zawartości medycznej bez zabezpieczenia w wypadku np. kradzieży wyrwanego i odblokowanego urządzenia (brak timeout'u).                                      | Krytyczne / Szczególnie wrażliwe |

---

## 2. Co już mamy?

* **Sprzętowe szyfrowanie magazynu tokenów (Z5):** Wszystkie tokeny niezbędne do autoryzacji API
  Supabase są zabezpieczone za pomocą `EncryptedSharedPreferences` w `EncryptedSessionManager.kt`.
* **Polityka limitu czasu (Timeouts):** Działający menedżer `SessionTimeoutManager.kt`, który
  blokuje dostęp do wrażliwych danych po 15 minutach bezczynności.
* **Silne wymogi dla logowania zastępczego (Biometria):** Skonfigurowano użycie wyłącznie
  najbezpieczniejszych dostępnych sensorów z użyciem parametru `BIOMETRIC_STRONG`. Słabsze czujniki
  zostaną przez aplikację odrzucone.
* **Bezpieczne procedury kończenia sesji:** Odwołanie do `AuthRepository.signOut()` zapewnia, że
  żądanie do API Supabase o unieważnieniu sesji faktycznie dociera na serwer, po czym natychmiastowo
  niszczone są powiązane dane z lokalnego magazynu aplikacji. Dodatkowo zdarzenia są ujęte w ramach
  zapisów audytu (AuditLog).

---

## 3. Czego brakuje?

| Luka / zagrożenie                                                              | Gdzie                                         | Ryzyko (niskie / średnie / wysokie) | Dlaczego takie ryzyko                                                                                                                                                                                                                                                                 |
|--------------------------------------------------------------------------------|-----------------------------------------------|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Brak "Zdalnego wylogowania" i wymuszenia końca na wszystkich urządzeniach**  | Konto użytkownika / Konfiguracja Supabase API | **Średnie**                         | Opcja wylogowania ze wszystkich sesji w przypadku wykrycia nieautoryzowanego zalogowania na innym urządzeniu zapobiegłaby przejęciu dostępu do konta przez dłuższą chwilę.                                                                                                            |
| **Natychmiastowe unieważnienie wciąż trwającego (wydanego) tokenu JWT**        | Backend dostawcy Auth                         | **Niskie / Średnie**                | Standardowe działanie JWT (tzw. "stateless") oznacza, że raz wystawiony zaufany token jest ważny do końca zadeklarowanego czasu (u nas ok. 1 godziny), nawet po kliknięciu "wyloguj", aż backend samodzielnie go nie zweryfikuje poprzez odrzucenie jako wygasłego na czarnej liście. |

---

## 4. Jak to rozwiązuje nasza platforma?

| Mechanizm / biblioteka                             | Do czego go użyjemy                                                                                                                                                         | Pułapki i ograniczenia                                                                                                           |
|----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| **Supabase Auth API i JWT**                        | Zapewnienie centralnego i elastycznego systemu zarządzania kontami z gotowymi politykami Rate Limit (ograniczenie brute-force).                                             | Aplikacja operuje wyłącznie na krótkotrwałych tokenach JWT (1h), a odświeżanie tokena zależy od łączności z serwerem.            |
| **Jetpack BiometricPrompt (`androidx.biometric`)** | Pozwala pacjentom lub pracownikom omijać uciążliwe podawanie bardzo skomplikowanego, bezpiecznego hasła przy zachowaniu wysokiego rygoru klasyfikacji zabezpieczeń klasy 3. | Zależy od konfiguracji biometrii narzuconej w ustawieniach systemu operacyjnego przez docelowego użytkownika.                    |
| **Coroutine StateFlow & Lifecycle Observer**       | Ciągłe i płynne zarządzanie blokowaniem oraz resetowaniem czasu timeout (w module `SessionTimeoutManager.kt`).                                                              | Poprawne wykrywanie braku interakcji może być ograniczone przez cykl życia fragmentów (np. gdy apka trafia w pełne tło i wraca). |

---

## 5. Plan na kolejny tydzień

| Zadanie                                                                | Osoba           | Jak udowodnimy, że działa                                                                                                             | Pozycja w checkliście MASVS      |
|------------------------------------------------------------------------|-----------------|---------------------------------------------------------------------------------------------------------------------------------------|----------------------------------|
| **Wdrożenie Certificate Pinning w warstwie sieciowej (Ktor/OkHttp)**   | Patryk Czakon   | Działająca komunikacja tylko w przypadku zgodnego klucza serwera. Próba użycia MitM (np. Proxyman) nie pozwoli na załadowanie danych. | MASVS-NETWORK-1, MASVS-NETWORK-2 |
| **Konfiguracja Network Security Config i walidacja komunikacji HTTPS** | Michał Kamiński | Brak możliwości podsłuchiwania ruchu przez zainstalowanie nieautoryzowanych certyfikatów roota w systemie na urządzeniu.              | MASVS-NETWORK-1                  |
| **Odporność API Supabase na zmianę certyfikatów głównych z czasem**    | Michał Kamiński | Odpowiednia i przygotowana polityka zarządzania kluczami publicznymi (hash backup pins) wektorów ataku.                               | MASVS-NETWORK-1                  |

---
