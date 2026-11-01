# Arkusz sekcji – „Jak to dotyczy naszej aplikacji?”

## Nagłówek

| Pole | Wartość |
| ----- | ----- |
| **Sekcja / aplikacja** | MobilnyPacjent |
| **Technologia** | Android (Kotlin, Jetpack Compose) + Supabase |
| **Zajęcia nr** | 3 |
| **Temat zajęć** | Uprawnienia i model bezpieczeństwa platform |
| **Obszar MASVS** | MASVS-PLATFORM, MASVS-PRIVACY |
| **Osoby obecne** | Patryk Czakon, Michał Kamiński |

## 0. Status z poprzedniego tygodnia

| Zadanie | Status (zrobione / częściowo / nie) | Dowód |
| ----- | ----- | ----- |
| **DFD w `docs/threat-model/`** | Zrobione | Plik `DFD.png` w `docs/arkusze/threat-model` |
| **Model zagrożeń v1 (min. 10 zagrożeń, STRIDE, ryzyko)** | Zrobione | Plik `threat-model-v1.md` w `docs/arkusze/threat-model` |
| **MVP: Ekran logowania i pierwsze połączenie z backendem** | Zrobione | Commit `8a61c74` w repozytorium |
| **Zapis arkusza w repozytorium** | Zrobione | Arkusz w repozytorium w `docs/arkusze/` |

## 1. Gdzie to zagrożenie występuje u nas?

| Miejsce w aplikacji | Jakie dane lub funkcja są zagrożone | Klasyfikacja danych |
| ----- | ----- | ----- |
| **Ekran dodawania dokumentacji / badań (funkcja aparatu do skanów recept i wyników badań)** | Ryzyko nieuprawnionego lub ciągłego dostępu do kamery urządzenia (`CAMERA`); przechwycenie poufnych zdjęć dokumentacji medycznej. | Szczególnie wrażliwe |
| **Wybór plików PDF i zdjęć z pamięci telefonu (import zewnętrznych wyników badań)** | Ryzyko żądania zbyt szerokich uprawnień do pamięci (`READ_MEDIA_IMAGES`) i wglądu w prywatne zdjęcia pacjenta spoza aplikacji. | Poufne / Szczególnie wrażliwe |
| **Ekran mapy przychodni (lokalizacja najbliższych placówek medycznych)** | Dostęp do dokładnych współrzędnych GPS użytkownika (`ACCESS_FINE_LOCATION`) i śledzenie położenia pacjenta. | Dane osobowe / Poufne |
| **Powiadomienia systemowe (przypomnienia o terminach wizyt i lekach)** | Wyciek szczegółów leczenia lub danych lekarza na ekranie blokady przez uprawnienie `POST_NOTIFICATIONS`. | Poufne |
| **Manifest aplikacji (`AndroidManifest.xml`) i eksportowane aktywności** | Ryzyko ominięcia ekranu logowania i bezpośredniego uruchomienia widoków pacjenta przez złośliwą aplikację (`adb shell am start`). | Integralność sesji i kontrola dostępu |

## 2. Co już mamy?

* **Minimalizm manifestu źródłowego:** Manifest deklaruje wyłącznie `android.permission.INTERNET`. Jest to uprawnienie typu `normal` – system przyznaje je automatycznie podczas instalacji i aplikacja nie wyświetla prośby runtime.
* **Weryfikacja scalonego manifestu (`Merged Manifest`):** Potwierdzono obecność jedynie technicznego uprawnienia `pl.polsl.mobilepatient.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` o poziomie ochrony `signature`. Żadna z dołączonych bibliotek (Supabase SDK, Ktor, OkHttp) nie wstrzyknęła ukrytych uprawnień runtime (np. dostępu do stanu telefonu czy pamięci masowej).
* **Izolacja piaskownicy (App Sandbox / SELinux):** Prywatne pliki bazy danych, preferencji i pamięci podręcznej są odizolowane procesowo od innych aplikacji w systemie.
* **Wymuszenie szyfrowania transmisji:** Projekt wymusza `minSdk = 28` oraz regułę `usesCleartextTraffic = false`, blokując nieszyfrowany ruch HTTP i zezwalając wyłącznie na szyfrowane kanały TLS.

## 3. Czego brakuje?

| Luka / zagrożenie | Gdzie | Ryzyko (niskie / średnie / wysokie) | Dlaczego takie ryzyko |
| ----- | ----- | ----- | ----- |
| **Brak obsługi stanów odmowy uprawnień w kodzie (brak graceful degradation przy odmowie aparatu lub notyfikacji)** | Warstwa UI Compose (ekran skanera / dodawania badań) | **Średnie** | Po odmowie użytkownika aplikacja może wejść w pętlę żądań, zawiesić się (crash) lub zablokować cały ekran zamiast pozwolić na alternatywne wprowadzenie danych. |
| **Ryzyko proszenia o zbyt szerokie uprawnienia do pamięci (`READ_MEDIA_IMAGES`) zamiast użycia selektora systemowego** | Moduł załączania plików badań | **Średnie** | Naruszenie reguły minimalizacji uprawnień MASVS; żądanie pełnego wglądu w galerię wzbudza nieufność pacjenta i jest niezgodne z nowoczesnymi wytycznymi bezpieczeństwa Androida. |
| **Niezabezpieczone komponenty w manifeście (brak jawnego `exported="false"` dla aktywności wewnętrznych)** | `AndroidManifest.xml` | **Wysokie** | Złośliwa aplikacja lub atakujący przez `adb` może wywołać wewnętrzną aktywność z pominięciem ekranu logowania i weryfikacji tożsamości. |
| **Brak flagi blokującej podgląd ekranu (`FLAG_SECURE`)** | Ekrany prezentujące historię chorób i e-recepty | **Wysokie** | System Android zapisuje zrzut ekranu w pamięci podręcznej menedżera ostatnich aplikacji, co może ujawnić dane medyczne osobom postronnym. |

## 4. Jak to rozwiązuje nasza platforma?

| Mechanizm / biblioteka | Do czego go użyjemy | Pułapki i ograniczenia |
| ----- | ----- | ----- |
| **`rememberLauncherForActivityResult` z `RequestPermission()`** | Dynamiczne proszenie o uprawnienia (`CAMERA`, `POST_NOTIFICATIONS`) dokładnie w chwili kliknięcia w daną funkcję. | System Android po dwukrotnej odmowie nie wyświetli ponownie okna dialogowego (`permanentlyDenied`); aplikacja musi wtedy wyświetlić komunikat i umożliwić ręczne przejście do ustawień systemowych. |
| **Android Photo Picker (`PickVisualMedia`)** | Wybór konkretnego zdjęcia skanu recepty/badania z galerii bez konieczności posiadania jakiegokolwiek uprawnienia runtime. | Działa natywnie od Androida 13 (oraz wstecznie od Androida 11 przez moduły Google Play); wymaga obsłużenia scenariusza, w którym użytkownik anuluje wybór pliku. |
| **Storage Access Framework (`ACTION_OPEN_DOCUMENT`)** | Wybieranie dokumentów PDF z wynikami badań bez uprawnień do całej pamięci masowej urządzenia. | Aplikacja otrzymuje tymczasowy dostęp URI tylko do wskazanego pliku; po restarcie urządzenia URI może wymagać trwałego utrwalenia uprawnień (`takePersistableUriPermission`). |
| **Wariant przybliżonej lokalizacji (`ACCESS_COARSE_LOCATION`)** | Wyszukiwanie placówek medycznych na mapie przy zachowaniu prywatności pacjenta. | Jeśli poprosimy jednocześnie o `FINE` i `COARSE`, Android pozwala użytkownikowi wybrać jedynie przybliżoną lokalizację — aplikacja musi działać poprawnie bez dostępu do dokładnych współrzędnych. |
| **Atrybut `android:exported="false"`** | Zamknięcie wewnętrznych komponentów aplikacji przed wywołaniem przez inne aplikacje. | Główna aktywność (`MainActivity`) musi mieć `exported="true"` ze względu na launcher; autoryzacja i weryfikacja sesji musi być sprawdzana wewnątrz kodu Jetpack Compose przy każdym wejściu na chroniony ekran. |

## 5. Plan na kolejny tydzień

| Zadanie | Osoba | Jak udowodnimy, że działa | Pozycja w checkliście MASVS |
| ----- | ----- | ----- | ----- |
| **Tabela uprawnień: po co, kiedy prosimy, co przy odmowie** | Patryk Czakon | Plik `docs/uprawnienia.md` z kompletnym opisem cyklu życia uprawnień (Aparat, Notyfikacje, Lokalizacja). | MASVS-PLATFORM-1 |
| **Usunięcie zbędnych uprawnień z bibliotek i weryfikacja Merged Manifest** | Patryk Czakon, Michał Kamiński | Zrzut ekranu z zakładki *Merged Manifest* w Android Studio z dowodem braku zbędnych wpisów (`tools:node="remove"`, jeśli zajdzie potrzeba). | MASVS-PLATFORM-1 |
| **Test odmowy uprawnień** | Patryk Czakon, Michał Kamiński | Test odwołania uprawnienia przez `adb shell pm revoke pl.polsl.mobilnypacjent android.permission.CAMERA` i zrzut ekranu pokazujący komunikat UI zamiast crasha aplikacji. | MASVS-PLATFORM-1 |
| **Lista danych przechowywanych lokalnie na urządzeniu** | Michał Kamiński | Plik `docs/dane-lokalne.md` klasyfikujący token sesji, cache i dane konfiguracyjne przed zajęciami Z4. | MASVS-STORAGE-1 |

## 6. Pytania do prowadzącego

* Brak