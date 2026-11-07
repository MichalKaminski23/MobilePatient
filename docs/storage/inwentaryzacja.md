# D1: Inwentaryzacja Danych (MobilePatient)

Niniejszy dokument przedstawia pełną inwentaryzację wszystkich danych przetwarzanych i
przechowywanych na urządzeniu mobilnym przez aplikację **MobilePatient**, z uwzględnieniem danych
zapisywanych przez biblioteki zewnętrzne oraz ich klasyfikacji pod kątem wrażliwości (RODO / GDPR /
HIPAA).

---

## 1. Klasyfikacja Poziomów Wrażliwości Danych

* **[KRYTYCZNE]**: Dane umożliwiające przejęcie tożsamości użytkownika lub pełny dostęp do konta
  (np. tokeny sesyjne JWT, klucze autoryzacyjne).
* **[MEDYCZNE / SZCZEGÓLNA KATEGORIA]**: Dane dotyczące stanu zdrowia, rozpoznania, leczenia,
  e-recept oraz dokumentacji medycznej pacjenta.
* **[DANE OSOBOWE (PII)]**: Dane pozwalające na identyfikację tożsamości osoby fizycznej (np. imię,
  nazwisko, adres e-mail, identyfikator UUID).
* **[OPERACYJNE / SYSTEMOWE]**: Tymczasowe pliki pamięci podręcznej UI, ustawienia aplikacji oraz
  dzienniki błędów.

---

## 2. Pełny Rejestr i Inwentaryzacja Danych

| Lp.   | Dana                                                        | Źródło / Moduł                                | Magazyn na urządzeniu                                  | Klasyfikacja         | Czy zapisywane na dysku?                          |
|:------|:------------------------------------------------------------|:----------------------------------------------|:-------------------------------------------------------|:---------------------|:--------------------------------------------------|
| **1** | **Tokeny sesyjne JWT (`access_token`, `refresh_token`)**    | Supabase Auth (`io.github.jan.supabase.auth`) | Pamięć sesji Supabase / `SharedPreferences` biblioteki | **[KRYTYCZNE]**      | **TAK** (pamięć podręczna sesji biblioteki)       |
| **2** | **Dane logowania (Email i Hasło)**                          | Formularz `LoginScreen.kt`                    | Pamięć operacyjna RAM (Stan Compose `mutableStateOf`)  | **[KRYTYCZNE]**      | **NIE** (czyszczone z RAM po zalogowaniu)         |
| **3** | **Dane Profilowe Użytkownika (Imię, Nazwisko, Rola, UUID)** | `AuthRepository` / `ShortProfileDto`          | Pamięć RAM (ViewModel / App State)                     | **[DANE OSOBOWE]**   | **NIE** (pobierane z sieci w czasie rzeczywistym) |
| **4** | **Historia Medyczna (`medical_records`)**                   | `DoctorRepository` / `MedicalRecordDto`       | Pamięć RAM (`StateFlow` w ViewModelu)                  | **[MEDYCZNE]**       | **NIE** (brak lokalnej retencji SQLite/Room)      |
| **5** | **E-Recepty (`prescriptions`)**                             | `DoctorRepository` / `PrescriptionDto`        | Pamięć RAM (`StateFlow` w ViewModelu)                  | **[MEDYCZNE]**       | **NIE** (baza w chmurze Supabase)                 |
| **6** | **Wizyty Lekarskie (`appointments`)**                       | `DoctorRepository` / `AppointmentDto`         | Pamięć RAM (`StateFlow` w ViewModelu)                  | **[MEDYCZNE]**       | **NIE** (baza w chmurze Supabase)                 |
| **7** | **Pamięć podręczna załadowanych zdjęć/dokumentów**          | Biblioteka Coil (`coil.compose.AsyncImage`)   | Prywatny katalog cache aplikacji (`Context.cacheDir`)  | **[MEDYCZNE / PII]** | **TAK** (tymczasowy cache plików graficznych)     |
| **8** | **Zgody i statusy uprawnień (Aparat, Powiadomienia)**       | `SettingsScreen.kt` / System Android          | Pamięć RAM (`LifecycleEventObserver`)                  | **[OPERACYJNE]**     | **NIE** (zarządzane przez system Android)         |
| **9** | **Logi sieciowe i komunikaty błędów**                       | Ktor / Supabase Client / Logcat               | Pasek dziennika systemowego Android Logcat             | **[OPERACYJNE]**     | **NIE** (strumień dziennika systemowego)          |

---

## 3. Szczegółowy Opis Przetwarzania wg Bibliotek Zewnętrznych

### A. Biblioteka Supabase Kotlin SDK (`io.github.jan.supabase`)

* **Moduły:** `Auth`, `Postgrest`.
* **Działanie:** Obsługuje szyfrowane połączenia HTTPS/TLS z backendem Supabase. Zarządza
  odświeżaniem tokenów JWT.
* **Klasyfikacja danych:** Krytyczne (Tokeny autoryzacyjne).

### B. Biblioteka Coil (`coil-kt`)

* **Działanie:** Odpowiada za asynchroniczne pobieranie i renderowanie obrazów (zdjęć dokumentacji
  medycznej i avatarów).
* **Magazyn:** Zapisuje tymczasowe pliki w dyskowym katalogu pamięci podręcznej `cacheDir`.
* **Ryzyko & Ochrona:** Pliki cache mogą zawierać miniaturki dokumentacji. Katalog jest prywatny dla
  aplikacji, a pliki są czyszczone przy wylogowaniu oraz przez mechanizmy czyszczenia systemu.

---

## 4. Podsumowanie Retencji Danych na Urządzeniu

* **Trwała retencja na dysku (SQLite/Room):** **0%** (Aplikacja nie posiada lokalnej bazodanowej
  retencji danych medycznych).
* **Retencja tymczasowa (RAM / Cache):** Wszystkie wrażliwe dane medyczne i osobowe są przetwarzane
  wyłącznie w pamięci operacyjnej podczas aktywnej sesji użytkownika.
