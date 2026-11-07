# D4: Mapa Kanałów Wycieku Danych (Data Leakage Channel Map)

Niniejszy dokument przedstawia analizę **8 głównych kanałów wycieku danych** na platformie Android. Dla każdego kanału określono, czy dotyczy on aplikacji **MobilePatient** oraz opisano wdrożone mechanizmy jego zabezpieczenia i zamknięcia.

---

## Tabela Analizy i Zamknięcia Kanałów Wycieku

| Lp. | Kanał Wycieku | Czy dotyczy aplikacji? | Status | Opis i Zastosowane Zabezpieczenia (Zamknięcie Kanału) |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Dzienniki systemowe (Logcat / System Logs)** | **TAK** | **ZAMKNIĘTY** | Usunięto logowanie jakichkolwiek tokenów JWT, haseł oraz danych medycznych do `Log.d()` / `Log.e()`. W wersji produkcyjnej wyłączono logowanie surowych ciał zapytań HTTP w bibliotece Supabase/Ktor. |
| **2** | **Kopia zapasowa w chmurze (Google Auto Backup)** | **TAK** | **ZAMKNIĘTY** | W pliku `AndroidManifest.xml` ustawiono `android:allowBackup="false"`. Ponadto skonfigurowano pliki `data_extraction_rules.xml` oraz `backup_rules.xml` blokujące ekstrakcję danych za pomocą kopii zapasowej ADB oraz chmury Google. |
| **3** | **Przełącznik aplikacji i podgląd ekranu (App Switcher / Screenshots)** | **TAK** | **ZAMKNIĘTY** | Na ekranach zawierających wrażliwe dane pacjentów i recept wdrożono flagę systemową `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)`. Uniemożliwia to wykonywanie zrzutów ekranu oraz zamaskuje podgląd aplikacji na liście ostatnich aplikacji (*Recent Apps*). |
| **4** | **Schowek systemowy (Clipboard Leakage)** | **TAK** | **ZAMKNIĘTY** | Zablokowano automatyczne kopiowanie kodów e-recept oraz danych osobowych do ogólnodostępnego schowka systemowego Androida. Kody recept są wyświetlane wyłącznie w formie wizualnej (tekst/QR) bez cichego zapisywania w `ClipboardManager`. |
| **5** | **Pliki tymczasowe i pamięć cache (Temp & Cache Files)** | **TAK** | **ZAMKNIĘTY** | Pliki graficzne pobierane przez bibliotekę Coil są zapisywane w prywatnym katalogu `cacheDir`. Wdrożono procedurę jawnego czyszczenia pamięci podręcznej obrazów przy wylogowaniu użytkownika (`onLogout`). |
| **6** | **Autouzupełnianie i słownik klawiatury (Keyboard Cache / Autofill)** | **TAK** | **ZAMKNIĘTY** | Pola tekstowe haseł i kodów recept zostały oznaczone typem `KeyboardType.Password` oraz `ImeAction.Done` z wyłączoną sugeracją słownika klawiatury (`autoCorrect = false`). W formularzach zastosowano flagi blokujące zapamiętywanie wrażliwych fraz medycznych przez słownik klawiatury systemowej. |
| **7** | **Niezabezpieczona komunikacja IPC / Intenty (Intent Redirection)** | **TAK** | **ZAMKNIĘTY** | Eksportowana aktywność `MainActivity` posiada zdefiniowany ścisły filtr intencji `medapp://patient`. Przechwytywany schemat głębokiego linkowania parsowany jest przy użyciu bezpiecznego kodu walidującego `extractPatientIdFromIntent()`, zapobiegając atakom typu *Intent Redirection*. Aktywności nieużywane są jawnie oznaczone jako `android:exported="false"`. |
| **8** | **Raporty o błędach i telemetria (Crash Reports & Analytics)** | **TAK** | **ZAMKNIĘTY** | Aplikacja nie korzysta z zewnętrznych bibliotek analitycznych zbierających dane w tle. Przechwytywanie wyjątków w ViewModelach (`try-catch`) zastępuje surowe stosy błędów czytelnymi komunikatami biznesowymi po polsku, uniemożliwiając wyciek danych pacjenta w raportach o błędach. |

---

## Podsumowanie Bezpieczeństwa Kanałów Wycieku

Dzięki wdrożonym zmianom konfiguracji manifestu, kontroli podglądu okien systemowych oraz wyeliminowaniu trwałej retencji dyskowej, **wszystkie 8 potencjalnych kanałów wycieku danych zostało skutecznie zabezpieczonych i zamkniętych**.
