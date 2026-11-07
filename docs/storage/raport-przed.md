# D2: Raport Audytowy „Przed” (Przegląd Danych w Pamięci i Logach)

Niniejszy raport przedstawia stan bezpieczeństwa aplikacji **MobilePatient** przed wdrożeniem dedykowanych mechanizmów utwardzania (*hardening*). Dokument identyfikuje potencjalne miejsca wycieku danych w postaci jawnego tekstu (*cleartext*) na dysku urządzenia oraz w dziennikach systemowych Logcat.

---

## 1. Analiza Zawartości Dysku (Pamięć Trwała)

Przed audytem i wdrożeniem zabezpieczeń przeanalizowano system plików aplikacji w ścieżce prywatnej `/data/data/pl.polsl.mobilepatient/`:

### A. Nieszyfrowane Pliki Preferencji (`shared_prefs/`)
* **Stan:** Standardowe pliki `SharedPreferences` tworzone przez domyślne konfiguracje bibliotek mogą przechowywać tokeny sesyjne `access_token` i `refresh_token` w formie niezaszyfrowanego tekstu JSON / XML.
* **Identyfikowane Ryzyko:** Na urządzeniach z uprawnieniami roota (*rooted devices*) lub poprzez fizyczny dostęp do systemu plików, atakujący może odczytać token JWT i przejąć tożsamość lekarza lub pacjenta.

### B. Pamięć Podręczna Obrazów (`cache/image_cache/`)
* **Stan:** Biblioteka Coil domyślnie zapisuje pobrane obrazy (np. zdjęcia wyników badań medycznych i recept) w katalogu `Context.cacheDir` bez dodatkowego szyfrowania plików na poziomie aplikacji.
* **Identyfikowane Ryzyko:** Choć katalog jest chroniony uprawnieniami systemu Android, pliki na dysku nie są szyfrowane na poziomie aplikacji (*application-level encryption*). W przypadku wykonania kopii zapasowej pliki mogłyby zostać odczytane.

### C. Mechanizm Kopii Zapasowej Androida (`Backup Rules`)
* **Stan:** Domyślnie system Android próbuje włączać automatyczną kopię zapasową danych aplikacji w chmurze Google (`Auto Backup`).
* **Zidentyfikowane Ryzyko:** Ryzyko automatycznego przesłania prywatnych danych sesji i pamięci podręcznej do niezaszyfrowanej kopii zapasowej na kontach Google użytkowników.

---

## 2. Analiza Dzienników Systemowych (Logcat i Wyjątki)

Przeanalizowano komunikat logów generowany podczas standardowych operacji w aplikacji (logowanie, pobieranie listy pacjentów, dodawanie recepty):

### A. Wyjątki Sieciowe i Komunikaty Błędów
* **Stan:** W klasie `AuthRepository.kt` błędy logowania przechwytują surowy komunikat wyjątku:
  ```kotlin
  val message = exception.message.orEmpty()
  ```
  Jeśli w trybie deweloperskim nagłówki HTTP lub parametry zapytania (np. adres e-mail) trafiłyby do `Log.e(...)` lub wyciekły przez domyślny logger Ktor/Supabase, dane te byłyby widoczne w dzienniku systemowym Android Logcat.
* **Zidentyfikowane Ryzyko:** Inne aplikacje posiadające uprawnienie odczytu logów (na starszych wersjach Androida lub urządzeniach deweloperskich) mogą odczytać wrażliwe adresy URL i nagłówki autoryzacyjne `Bearer <token>`.

### B. Przechowywanie haseł w pamięci RAM
* **Stan:** W formularzu logowania `LoginScreen.kt` hasło jest przekazywane jako zwykły obiekt `String` (który jest niezmienny w pamięci JVM i oczekuje na wyczyszczenie przez Garbage Collector).
* **Zidentyfikowane Ryzyko:** Dump pamięci RAM procesu aplikacji w trakcie zalogowania mógłby ujawnić jawne hasło użytkownika.

---

## 3. Podsumowanie Podatności „Przed” (Przed Utwardzeniem)

| Obszar | Identyfikowana Podatność | Poziom Ryzyka | Wymagane Działanie Naprawcze |
| :--- | :--- | :--- | :--- |
| **Pliki preferencji** | Zapis tokenów sesyjnych nieszyfrowanym tekstem. | **WYSOKI** | Wdrożenie `EncryptedSharedPreferences` / Android KeyStore. |
| **Kopia zapasowa** | Ryzyko wycieku danych przez chmurę Google Backup. | **ŚREDNI** | Wdrożenie `android:allowBackup="false"` oraz reguł `dataExtractionRules`. |
| **Cache obrazów** | Zdjęcia dokumentacji w formacie podglądu na dysku `cacheDir`. | **ŚREDNI** | Czyszczenie pamięci podręcznej Coil przy wylogowaniu. |
| **Logcat** | Ryzyko wypisania tokenów i adresów e-mail w logach. | **NISKI / ŚREDNI** | Usunięcie logowania danych wrażliwych i wyłączenie verbose logging w wydaniu produkcyjnym. |
| **Zabezpieczenie ekranu** | Wyciek podglądu ekranu w przełączniku aplikacji (*App Switcher*). | **NISKI** | Dodanie flagi `FLAG_SECURE` na ekranach zawierających dane medyczne. |
