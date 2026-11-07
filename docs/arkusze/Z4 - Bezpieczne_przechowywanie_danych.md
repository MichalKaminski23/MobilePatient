# Arkusz sekcji – „Jak to dotyczy naszej aplikacji?”

## Nagłówek

| Pole | Wartość |
|---|---|
| **Sekcja / aplikacja** | MobilnyPacjent |
| **Technologia** | Android (Kotlin, Jetpack Compose) + Supabase |
| **Zajęcia nr** | 4 |
| **Temat zajęć** | Bezpieczne przechowywanie danych |
| **Obszar MASVS** | MASVS-STORAGE |
| **Osoby obecne** | Michał Kamiński, Patryk Czakon |

---

## 0. Status z poprzedniego tygodnia

| Zadanie | Status (zrobione / częściowo / nie) | Dowód |
|---|---|---|
| **Tabela uprawnień: po co, kiedy prosimy, co przy odmowie** | Zrobione | Plik `docs/uprawnienia/Uprawnienia.md` z kompletnym opisem cyklu życia uprawnień (Aparat, Notyfikacje, Lokalizacja). |
| **Usunięcie zbędnych uprawnień z bibliotek i weryfikacja Merged Manifest** | Zrobione | Zrzut ekranu z zakładki *Merged Manifest* w Android Studio z dowodem braku zbędnych wpisów (`tools:node="remove"`, jeśli zajdzie potrzeba). |
| **Test odmowy uprawnień** | Zrobione | Test odwołania uprawnienia przez `adb shell pm revoke pl.polsl.mobilepatient android.permission.CAMERA` i zrzut ekranu pokazujący komunikat UI zamiast crasha aplikacji (`docs/zrzuty ekranu/odmowa.png`). |
| **Lista danych przechowywanych lokalnie na urządzeniu** | Zrobione | Plik `docs/dane lokalne/Dane-lokalne.md` oraz kompletna inwentaryzacja `docs/storage/inwentaryzacja.md` klasyfikująca token sesji, cache i dane medyczne przed zajęciami Z4. |

---

## 1. Gdzie to zagrożenie występuje u nas?

| Miejsce w aplikacji | Jakie dane lub funkcja są zagrożone | Klasyfikacja danych |
|---|---|---|
| **Magazyn sesji aplikacji (`SharedPreferences` / wewnętrzny plik Supabase)** | Tokeny uwierzytelniające JWT (`access_token`, `refresh_token`); ryzyko kradzieży tożsamości pacjenta/lekarza w przypadku niezaszyfrowanego odczytu z pamięci urządzenia (np. zrootowany telefon lub atakujący z dostępem fizycznym). | Krytyczne / Poufne |
| **Pamięć podręczna obrazów (`Context.cacheDir` / biblioteka Coil)** | Tymczasowe pliki podglądów i skanów dokumentacji medycznej, recept oraz wyników badań; ryzyko nieautoryzowanego wglądu w pliki cache pozostałe po sesji. | Szczególnie wrażliwe |
| **Przełącznik zadań systemu Android (*App Switcher* / Zrzuty ekranu)** | Automatycznie generowane przez system miniatury okien zawierające historię leczenia, PESEL i kody e-recept widoczne na liście ostatnich aplikacji. | Szczególnie wrażliwe |
| **Dzienniki systemowe (Logcat / logi błędów sieciowych)** | Ryzyko przypadkowego wycieku nagłówków `Authorization: Bearer <token>`, identyfikatorów pacjentów lub adresów e-mail w surowych logach zapytań HTTP biblioteki sieciowej. | Poufne / Operacyjne |

---

## 2. Co już mamy?

* **Strategia „Zero Local Storage” dla dokumentacji medycznej:** Aplikacja celowo nie składuje historii chorób, wizyt ani e-recept w trwałej lokalnej bazie danych (SQLite/Room). Dane medyczne pobierane są na żądanie z bezpiecznej bazy Supabase przez szyfrowane połączenie HTTPS/TLS i przetwarzane wyłącznie w pamięci operacyjnej RAM (`StateFlow` w ViewModelu).
* **Blokada kopii zapasowych w chmurze i ADB:** W pliku `AndroidManifest.xml` zdefiniowano `android:allowBackup="false"` oraz reguły `data_extraction_rules.xml` i `backup_rules.xml`, co skutecznie uniemożliwia pobranie danych sesji i plików aplikacji przez polecenie `adb backup` oraz mechanizm Google Auto Backup.
* **Izolacja piaskownicy Androida (App Sandbox):** Wszystkie pliki sesyjne i tymczasowe znajdują się w prywatnym katalogu `/data/data/pl.polsl.mobilepatient/`, chronionym unikalnym identyfikatorem UID jądra Linux, regułami SELinux oraz sprzętowym szyfrowaniem partycji na poziomie plików (FBE).
* **Procedura niszczenia danych przy wylogowaniu:** Akcja `signOut()` unieważnia token po stronie Supabase, natychmiast zeruje stany użytkownika w pamięci RAM (`loggedInRole = null`, `loggedInName = ""`) oraz usuwa tymczasowe pliki z pamięci podręcznej obrazów.
* **Kompletna dokumentacja magazynu danych (D1–D5):** Opracowano pełny zestaw dokumentów architektonicznych: inwentaryzację danych (`inwentaryzacja.md`), raport stanu przed (`raport-przed.md`), decyzje architektoniczne (`decyzje.md`), mapę kanałów wycieków (`kanaly-wyciekow.md`) oraz politykę cyklu życia sesji (`polityka-sesji.md`).

---

## 3. Czego brakuje?

| Luka / zagrożenie | Gdzie | Ryzyko (niskie / średnie / wysokie) | Dlaczego takie ryzyko |
|---|---|---|---|
| **Przechowywanie tokenów JWT bez sprzętowego szyfrowania** | Lokalny magazyn sesji (`shared_prefs` / `files`) | **Wysokie** | Brak szyfrowania kluczem z Android Keystore umożliwia odczyt tokena sesji w postaci jawnej na zrootowanym telefonie lub przez zrzut pamięci Flash, co pozwala na przejęcie konta bez podawania hasła. |
| **Brak blokady zrzutów ekranu na widokach medycznych** | Okno główne aplikacji (`WindowManager`) na ekranach pacjenta i e-recept | **Średnie** | Brak flagi `FLAG_SECURE` powoduje, że system zapisuje miniatury ekranów w menedżerze ostatnich aplikacji, co naraża dane zdrowotne na wgląd osób postronnych. |
| **Ryzyko pozostawania plików w pamięci cache po nagłym zamknięciu aplikacji** | Katalog `cache/` (podglądy skanów Coil) | **Średnie** | Choć katalog jest prywatny, zdjęcia badań nie są szyfrowane kryptograficznie. W razie wymuszonego zamknięcia aplikacji pliki pozostają na dysku do kolejnego czyszczenia. |
| **Brak automatycznego wylogowania po czasie bezczynności** | Warstwa zarządzania sesją (Session Manager) | **Średnie** | Pozostawienie odblokowanego telefonu przez lekarza lub pacjenta umożliwia nieuprawniony wgląd w dokumentację medyczną. |

---

## 4. Jak to rozwiązuje nasza platforma?

| Mechanizm / biblioteka | Do czego go użyjemy | Pułapki i ograniczenia |
|---|---|---|
| **Android Keystore + `EncryptedSharedPreferences` (`androidx.security:security-crypto`)** | Bezpieczne przechowywanie tokenów sesji JWT. Klucz główny `MasterKey` generowany jest w sprzętowym module TEE/StrongBox (AES-256-GCM), uniemożliwiając odczyt sekretu nawet przy zrootowanym systemie. | Zmiana lub usunięcie blokady ekranu (PIN/biometria) przez użytkownika może unieważnić klucz sprzętowy; aplikacja musi wtedy wymusić ponowne logowanie. |
| **`WindowManager.LayoutParams.FLAG_SECURE`** | Blokowanie wykonywania zrzutów ekranu oraz ukrywanie podglądu okna aplikacji w menedżerze ostatnich aplikacji (*App Switcher*) na ekranach prezentujących dane medyczne i e-recepty. | Uniemożliwia legalne zrobienie zrzutu ekranu recepty przez pacjenta (wymaga opcji eksportu do PDF) oraz utrudnia automatyczne testy UI (czarne zrzuty ekranu). |
| **Zasady `data_extraction_rules.xml` i `backup_rules.xml`** | Precyzyjne wykluczenie wszystkich domen danych (`sharedpref`, `database`, `root`, `file`) z transferów chmurowych i kopii lokalnych ADB. | Użytkownik zmieniający telefon musi zalogować się od nowa; brak możliwości przeniesienia aktywnej sesji (co w aplikacji medycznej jest pożądanym zachowaniem). |
| **Dedykowany interfejs czyszczenia cache przy starcie i wylogowaniu** | Usuwanie zawartości `Context.cacheDir` oraz pamięci podręcznej biblioteki Coil w momencie naciśnięcia przycisku wylogowania oraz podczas startu aplikacji. | Zbyt agresywne czyszczenie wymusza ponowne pobieranie grafik z chmury, zwiększając zużycie transferu sieciowego. |

---

## 5. Plan na kolejny tydzień

| Zadanie | Osoba | Jak udowodnimy, że działa | Pozycja w checkliście MASVS |
|---|---|---|---|
| **Wdrożenie `EncryptedSharedPreferences` / Android Keystore dla tokenów sesji (K1)** | Michał Kamiński | Inspekcja pliku preferencji przez Device File Explorer wykazująca zaszyfrowany szyfrogram zamiast jawnego tokena JWT. | MASVS-STORAGE-1, MASVS-CRYPTO-1 |
| **Zamknięcie kanałów wycieku: flaga `FLAG_SECURE` i weryfikacja logów (K2)** | Patryk Czakon | Próba zrobienia zrzutu ekranu (`adb exec-out screencap`) kończąca się czarnym ekranem; weryfikacja braku wrażliwych nagłówków w Logcat. | MASVS-STORAGE-2 |
| **Działające MVP z podziałem ról (Pacjent / Lekarz / Admin) na demo Z5** | Michał Kamiński, Patryk Czakon | Działająca prezentacja (5-minutowe demo) logowania na poszczególne role, dodawania i przeglądania wpisów medycznych oraz e-recept z wymuszeniem RLS. | Kamień milowy Z5 / MASVS-AUTH-1 |
| **Zapis kompletu dokumentacji D1–D5 i arkusza w repozytorium** | Michał Kamiński | Komplet plików `docs/storage/*.md` oraz `docs/arkusze/Z4 - Bezpieczne_przechowywanie_danych.md` umieszczony w repozytorium. | Wymóg formalny Z4 |

---

## 6. Pytania do prowadzącego

* Brak

---
