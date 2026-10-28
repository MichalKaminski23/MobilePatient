# Model zagrożeń v1 — Mobilny Pacjent

## 1. Skala ryzyka

| Poziom | Opis |
|---|---|
| Niskie | Ograniczony wpływ na bezpieczeństwo aplikacji. |
| Średnie | Możliwe naruszenie bezpieczeństwa wymagające dodatkowych zabezpieczeń. |
| Wysokie | Możliwe poważne naruszenie poufności, integralności lub dostępności danych. |

## 2. Lista zagrożeń

### T01 — Kradzież tokenu sesji

| Pole | Wartość |
|---|---|
| Element DFD | Aplikacja Android — lokalny magazyn danych |
| Zagrożenie | Atakujący pozyskuje token sesji z urządzenia i podszywa się pod zalogowanego użytkownika. |
| STRIDE | S — podszywanie się |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Tokeny muszą być przechowywane w bezpieczny sposób i nie mogą trafiać do logów. |
| Dowód | Inspekcja magazynu lokalnego i logów oraz test wylogowania. |
| MASVS | MASVS-AUTH, MASVS-STORAGE |
| Status | Zaplanowane |
| Kiedy | Zajęcia 4 |

### T02 — Przechwycenie lub modyfikacja komunikacji

| Pole | Wartość |
|---|---|
| Element DFD | Przepływ danych między aplikacją Android a Supabase |
| Zagrożenie | Atakujący próbuje przechwycić lub zmodyfikować żądania zawierające tokeny albo dane medyczne. |
| STRIDE | T — manipulacja; I — ujawnienie informacji |
| Prawdopodobieństwo | Niskie |
| Wpływ | Wysoki |
| Ryzyko | Średnie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Komunikacja musi korzystać z HTTPS/TLS i prawidłowej walidacji certyfikatów. |
| Dowód | Inspekcja konfiguracji sieciowej i test połączenia z nieprawidłowym certyfikatem. |
| MASVS | MASVS-NETWORK |
| Status | Zaplanowane |
| Kiedy | Zajęcia 7 |

### T03 — Odczyt danych innego pacjenta

| Pole | Wartość |
|---|---|
| Element DFD | PostgreSQL — tabela wizyt |
| Zagrożenie | Pacjent zmienia identyfikator rekordu w żądaniu API i próbuje odczytać dane innego pacjenta. |
| STRIDE | E — podniesienie uprawnień |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Polityki RLS muszą ograniczać dostęp do rekordów zgodnie z tożsamością i uprawnieniami użytkownika. |
| Dowód | Test dwóch kont pacjentów: pacjent A nie może odczytać danych pacjenta B po podmianie identyfikatora. |
| MASVS | MASVS-AUTH |
| Status | Zaplanowane |
| Kiedy | Zajęcia 8 |

### T04 — Nieautoryzowana zmiana roli użytkownika

| Pole | Wartość |
|---|---|
| Element DFD | PostgreSQL — tabela profili |
| Zagrożenie | Pacjent próbuje zmienić swoją rolę na lekarza lub administratora. |
| STRIDE | T — manipulacja; E — podniesienie uprawnień |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Zmiana ról musi być kontrolowana po stronie backendu. Użytkownik nie może samodzielnie nadawać sobie uprawnień. |
| Dowód | Próba zmiany roli przez aplikację i bezpośrednie żądanie API. |
| MASVS | MASVS-AUTH |
| Status | Zaplanowane |
| Kiedy | Zajęcia 8 |

### T05 — Dostęp lekarza do nieprzypisanego pacjenta

| Pole | Wartość |
|---|---|
| Element DFD | PostgreSQL — wizyty, recepty i badania |
| Zagrożenie | Lekarz próbuje odczytać lub zmodyfikować dokumentację pacjenta, który nie został mu przypisany. |
| STRIDE | E — podniesienie uprawnień |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Dostęp lekarza musi zależeć od jego roli oraz przypisania do konkretnego pacjenta. |
| Dowód | Test dostępu do pacjenta przypisanego i nieprzypisanego do lekarza. |
| MASVS | MASVS-AUTH |
| Status | Zaplanowane |
| Kiedy | Zajęcia 8 |

### T06 — Nieautoryzowany dostęp do skanów medycznych

| Pole | Wartość |
|---|---|
| Element DFD | Supabase Storage — skany medyczne |
| Zagrożenie | Osoba nieuprawniona próbuje pobrać skan dokumentacji medycznej. |
| STRIDE | I — ujawnienie informacji |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Pliki muszą być przechowywane w prywatnym bucketcie, a dostęp do nich musi podlegać autoryzacji. |
| Dowód | Próba pobrania pliku bez sesji i przez użytkownika bez odpowiednich uprawnień. |
| MASVS | MASVS-STORAGE |
| Status | Zaplanowane |
| Kiedy | Zajęcia 8 |

### T07 — Ujawnienie danych wrażliwych w logach

| Pole | Wartość |
|---|---|
| Element DFD | Aplikacja Android — logi aplikacji |
| Zagrożenie | Token sesji, dane osobowe lub diagnoza zostają zapisane w logach diagnostycznych. |
| STRIDE | I — ujawnienie informacji |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Tokeny i dane medyczne nie mogą być zapisywane w logach. |
| Dowód | Analiza logów podczas logowania i wykonywania operacji na danych pacjenta. |
| MASVS | MASVS-STORAGE |
| Status | Zaplanowane |
| Kiedy | Zajęcia 4 |

### T08 — Niebezpieczny deep link

| Pole | Wartość |
|---|---|
| Element DFD | Aplikacja Android — wejściowe deep linki |
| Zagrożenie | Złośliwa aplikacja wywołuje link do wizyty i próbuje ominąć uwierzytelnienie lub autoryzację. |
| STRIDE | S — podszywanie się; E — podniesienie uprawnień |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Deep link nie może przyznawać dostępu do danych. Aplikacja musi weryfikować sesję i uprawnienia do wskazanego rekordu. |
| Dowód | Otwarcie linku bez logowania i próba dostępu do danych innego pacjenta. |
| MASVS | MASVS-PLATFORM, MASVS-AUTH |
| Status | Zaplanowane |
| Kiedy | Zajęcia 9 |

### T09 — Niebezpieczna zawartość WebView

| Pole | Wartość |
|---|---|
| Element DFD | Aplikacja Android — WebView i zewnętrzny serwer WWW |
| Zagrożenie | Złośliwa treść strony próbuje wykorzystać niebezpieczną konfigurację WebView do ujawnienia danych lub wywołania nieautoryzowanego działania. |
| STRIDE | T — manipulacja; I — ujawnienie informacji |
| Prawdopodobieństwo | Niskie |
| Wpływ | Wysoki |
| Ryzyko | Średnie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | WebView powinien ładować zaufane źródła, a niepotrzebne funkcje i mostki JavaScript powinny być wyłączone. |
| Dowód | Inspekcja konfiguracji WebView i test odrzucania niezaufanych źródeł. |
| MASVS | MASVS-PLATFORM |
| Status | Zaplanowane |
| Kiedy | Zajęcia 9 |

### T10 — Atak na dostępność logowania

| Pole | Wartość |
|---|---|
| Element DFD | Proces logowania — Supabase Auth |
| Zagrożenie | Atakujący wykonuje dużą liczbę prób logowania, utrudniając dostęp do usługi prawidłowym użytkownikom. |
| STRIDE | D — odmowa usługi |
| Prawdopodobieństwo | Średnie |
| Wpływ | Średni |
| Ryzyko | Średnie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Należy skonfigurować dostępne mechanizmy limitowania prób logowania i ochrony przed nadużyciami. |
| Dowód | Kontrolowany test powtarzanych prób logowania i sprawdzenie reakcji mechanizmów ochronnych. |
| MASVS | MASVS-AUTH |
| Status | Zaplanowane |
| Kiedy | Zajęcia 8 |

### T11 — Brak możliwości ustalenia autora operacji

| Pole | Wartość |
|---|---|
| Element DFD | Backend — operacje na dokumentacji medycznej |
| Zagrożenie | Użytkownik zaprzecza wykonaniu zmiany danych, a system nie ma wystarczających informacji pozwalających ustalić autora operacji. |
| STRIDE | R — wypieranie się |
| Prawdopodobieństwo | Średnie |
| Wpływ | Średni |
| Ryzyko | Średnie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Istotne operacje powinny być rejestrowane w kontrolowanym dzienniku audytowym. |
| Dowód | Wykonanie operacji testowej i sprawdzenie wpisu zawierającego aktora, czas oraz rodzaj operacji. |
| MASVS | MASVS-CODE |
| Status | Zaplanowane |
| Kiedy | Zajęcia 8 |

### T12 — Ujawnienie danych w powiadomieniach

| Pole | Wartość |
|---|---|
| Element DFD | Backend — Firebase Cloud Messaging — urządzenie |
| Zagrożenie | Powiadomienie push zawiera diagnozę lub inne wrażliwe dane widoczne na ekranie blokady. |
| STRIDE | I — ujawnienie informacji |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Powiadomienia nie powinny zawierać diagnoz ani szczegółowych danych medycznych. |
| Dowód | Inspekcja treści powiadomień i test wyświetlania na ekranie blokady. |
| MASVS | MASVS-PLATFORM, MASVS-PRIVACY |
| Status | Zaplanowane |
| Kiedy | Zajęcia 12 |

### T13 — Ujawnienie danych przez zrzuty ekranu

| Pole | Wartość |
|---|---|
| Element DFD | Aplikacja Android — ekran danych pacjenta |
| Zagrożenie | Dane medyczne zostają ujawnione przez zrzut ekranu lub podgląd aplikacji w przełączniku zadań. |
| STRIDE | I — ujawnienie informacji |
| Prawdopodobieństwo | Średnie |
| Wpływ | Wysoki |
| Ryzyko | Wysokie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Należy określić zasady ochrony wrażliwych ekranów przed zrzutami ekranu i podglądem w ostatnich aplikacjach. |
| Dowód | Test zrzutu ekranu i podglądu aplikacji na ekranie z danymi medycznymi. |
| MASVS | MASVS-PLATFORM, MASVS-PRIVACY |
| Status | Zaplanowane |
| Kiedy | Zajęcia 12 |

### T14 — Przeciążenie backendu

| Pole | Wartość |
|---|---|
| Element DFD | API Supabase i PostgreSQL |
| Zagrożenie | Duża liczba żądań lub kosztowne zapytania powodują wyczerpanie zasobów i niedostępność usługi. |
| STRIDE | D — odmowa usługi |
| Prawdopodobieństwo | Niskie |
| Wpływ | Średni |
| Ryzyko | Niskie |
| Decyzja | Zredukować |
| Wymaganie bezpieczeństwa | Należy stosować walidację danych wejściowych, limity żądań i ograniczenia kosztownych operacji. |
| Dowód | Kontrolowany test obciążeniowy w środowisku testowym i weryfikacja skonfigurowanych limitów. |
| MASVS | MASVS-NETWORK |
| Status | Zaplanowane |
| Kiedy | Zajęcia 7 |
