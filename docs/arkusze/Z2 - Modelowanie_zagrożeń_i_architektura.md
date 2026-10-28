# Arkusz sekcji – „Jak to dotyczy naszej aplikacji?”

## Nagłówek

| Pole | Wartość |
|---|---|
| **Sekcja / aplikacja** | MobilnyPacjent |
| **Technologia** | Android (Kotlin, Jetpack Compose) + Supabase |
| **Zajęcia nr** | 2 |
| **Temat zajęć** | Modelowanie zagrożeń i architektura |
| **Obszar MASVS** | - |
| **Osoby obecne** | Michał Kamiński, Patryk Czakon |

---

## 0. Status z poprzedniego tygodnia

| Zadanie | Status (zrobione / częściowo / nie) | Dowód |
|---|---|---|
| **Repozytorium** | Zrobione | [GitHub Repozytorium](https://github.com/MichalKaminski23/MobilePatient) |
| **README z kartą projektu** | Zrobione | Plik `README.md` w repozytorium zawierający kompletną Kartę Projektu |
| **Szkielet aplikacji** | Zrobione | Commit [`ac91e91`](https://github.com/MichalKaminski23/MobilePatient/commit/ac91e91bab7ebacbd3444890c0369bf953e95066) – projekt kompiluje się i uruchamia |
| **Szkic architektury** | Zrobione | Wstępny diagram komponentów w `README.md` |

---

## 1. Gdzie to zagrożenie występuje u nas?

| Miejsce w aplikacji | Jakie dane lub funkcja są zagrożone | Klasyfikacja danych |
|---|---|---|
| **Ekrany prezentacji (Jetpack Compose / pamięć RAM)** | Podgląd historii zdrowia, diagnoz, e-recept oraz skanów badań (zagrożenie: podgląd w przełączniku zadań, zrzuty ekranu, zrzut pamięci) | Szczególnie wrażliwe |
| **Lokalny magazyn aplikacji (SharedPreferences / pliki cache)** | Tokeny sesji JWT i Refresh Tokens umożliwiające przejęcie tożsamości użytkownika | Poufne |
| **Baza danych PostgreSQL i Supabase Storage** | Dokumentacja medyczna pacjentów, dane osobowe (PESEL, telefon, adres), pliki badań | Poufne / Szczególnie wrażliwe |
| **Wejściowe punkty integracji (Deep Link / WebView)** | Parametry wywołania wizyty (`medapp://visit/{id}`) oraz wczytywanie zewnętrznego regulaminu/RODO | Funkcje krytyczne / integralność |

---

## 2. Co już mamy?

* **Domyślna piaskownica systemu Android (App Sandbox):** Izolacja plików aplikacji od innych procesów za pomocą unikalnego UID procesu.
* **Wymuszenie bezpiecznej komunikacji:** Przyjęte `minSdk = 28` domyślnie blokuje nieszyfrowany ruch w sieci (`usesCleartextTraffic = false`), wymuszając protokół HTTPS/TLS.
* **Czysty szkielet aplikacji:** Aplikacja uruchamia się w Jetpack Compose z poprawną strukturą modułów i wpiętymi podstawowymi zależnościami.

---

## 3. Czego brakuje?

| Luka / zagrożenie | Gdzie | Ryzyko (niskie / średnie / wysokie) | Dlaczego takie ryzyko |
|---|---|---|---|
| **Brak bezpiecznego magazynu danych sesyjnych** | Magazyn lokalny Androida | **Wysokie** | Przechowywanie tokena JWT w otwartym tekście pozwala na jego kradzież z kopii zapasowej (`adb backup`) lub na zrootowanym telefonie (T01). |
| **Brak wdrożonych polityk autoryzacji RLS** | Supabase (PostgreSQL) | **Wysokie** | Brak reguł Row-Level Security umożliwia atakującemu odczyt lub modyfikację dokumentacji innego pacjenta przez podmianę ID w zapytaniu REST (T03, T05). |
| **Brak walidacji Deep Linków i zabezpieczeń ekranu** | Komponenty platformowe Androida | **Średnie** | Wywołanie ekranu wizyty z zewnątrz może ominąć autoryzację, a brak flagi `FLAG_SECURE` naraża dane na podgląd w menedżerze zadań (T08, T13). |

---

## 4. Jak to rozwiązuje nasza platforma?

| Mechanizm / biblioteka | Do czego go użyjemy | Pułapki i ograniczenia |
|---|---|---|
| **Android Keystore + EncryptedSharedPreferences** | Bezpieczne przechowywanie tokenów JWT i kluczy szyfrujących w sprzętowym magazynie. | Klucze mogą zostać unieważnione po zmianie lub usunięciu blokady ekranu (PIN/biometria); narzut wydajnościowy przy częstym I/O. |
| **BiometricPrompt API** | Odblokowanie aplikacji i autoryzacja dostępu do klucza w Keystore za pomocą biometrii. | Konieczność obsługi urządzeń bez czytnika biometrycznego lub z niepoprawnie skonfigurowanym odciskiem palca (`BIOMETRIC_ERROR_NONE_ENROLLED`). |
| **PostgreSQL Row-Level Security (RLS)** | Wymuszenie kontroli dostępu do wierszy bezpośrednio w silniku bazy danych na podstawie tokena JWT. | Błędnie skonstruowane polityki SQL mogą doprowadzić do wycieku danych (np. brak uwzględnienia roli w warunku `USING`). |
| **WindowManager.LayoutParams.FLAG_SECURE** | Blokowanie wykonywania zrzutów ekranu oraz ukrywanie podglądu aplikacji na liście ostatnich zadań. | Utrudnia debugowanie i tworzenie automatycznych testów interfejsu (zrzuty ekranu są czarne). |

---

## 5. Plan na kolejny tydzień

| Zadanie | Osoba | Jak udowodnimy, że działa | Pozycja w checkliście MASVS |
|---|---|---|---|
| **DFD w `docs/threat-model/` z granicami zaufania** | Michał Kamiński | Zaktualizowany plik Mermaid oraz wyrenderowany plik graficzny DFD z naniesionymi granicami zaufania. | Wymóg formalny Z2 |
| **Model zagrożeń v1 (min. 10 zagrożeń, STRIDE, ryzyko)** | Patryk Czakon | Kompletny plik `docs/threat-model/threat-model-v1.md` z 14 sklasyfikowanymi zagrożeniami i mapowaniem na kod. | Wymóg formalny Z2 |
| **MVP: Ekran logowania i pierwsze połączenie z backendem** | Michał Kamiński, Patryk Czakon | Działający ekran logowania rejestrujący/uwierzytelniający użytkownika w Supabase Auth (zrzut ekranu z panelu Supabase + commit w repo). | MASVS-AUTH-1 |
| **Zapis arkusza w repozytorium** | Michał Kamiński | Plik umieszczony w ścieżce `docs/arkusze/Z02-modelowanie-zagrozen-i-architektura.md`. | Wymóg formalny Z2 |

---

## 6. Pytania do prowadzącego

* Brak