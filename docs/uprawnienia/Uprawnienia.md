# Analiza i Zarządzanie Uprawnieniami — MobilnyPacjent (MASVS-PLATFORM-1)

| Pole                | Wartość                                                         |
|---------------------|-----------------------------------------------------------------|
| **Aplikacja**       | MobilnyPacjent                                                  |
| **Pakiet Androida** | `pl.polsl.mobilepatient`                                        |
| **Obszar MASVS**    | MASVS-PLATFORM (MASVS-PLATFORM-1: Zasada minimalnych uprawnień) |
| **Etap prac**       | Zajęcia 3 (Z3 – Uprawnienia i model bezpieczeństwa platform)    |
| **Autorzy**         | Patryk Czakon, Michał Kamiński                                  |

---

## 1. Uprawnienia w Scalonym Manifeście (Merged Manifest)

Scalony manifest (`Merged Manifest`) aplikacji po kompilacji z uwzględnieniem dodanych bibliotek
(Supabase, Google Maps, Biometria) zawiera wyłącznie niezbędne wpisy:

| Uprawnienie / Wpis                                                | Typ ochrony           | Pochodzenie                                              | Zastosowanie w aplikacji                                                                               | Czy wymaga zgody użytkownika (Runtime)?                     |
|-------------------------------------------------------------------|-----------------------|----------------------------------------------------------|--------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| `android.permission.INTERNET`                                     | `normal`              | `AndroidManifest.xml` (kod własny)                       | Komunikacja z API Supabase (uwierzytelnianie, baza PostgreSQL, pobieranie/wysyłanie plików w Storage). | **Nie** (przyznawane automatycznie przy instalacji).        |
| `android.permission.CAMERA`                                       | `dangerous` (runtime) | `AndroidManifest.xml` (kod własny)                       | Bezpośrednie wykonywanie zdjęć dokumentacji medycznej (recept, skierowań, wyników badań).              | **Tak** (pytamy dopiero przy kliknięciu przycisku aparatu). |
| `android.permission.POST_NOTIFICATIONS`                           | `dangerous` (runtime) | `AndroidManifest.xml` (kod własny)                       | Powiadomienia systemowe o zbliżających się wizytach lekarskich i dawkowaniu leków.                     | **Tak** (Android 13+ / API 33+).                            |
| `android.permission.ACCESS_NETWORK_STATE`                         | `normal`              | Biblioteka `play-services-maps` (Google Maps)            | Weryfikacja stanu sieci komórkowej/Wi-Fi na potrzeby wczytywania kafelków mapy przychodni.             | **Nie** (uprawnienie normalne).                             |
| `android.permission.USE_BIOMETRIC`                                | `normal`              | Biblioteka `androidx.biometric`                          | Obsługa logowania biometrycznego (odcisk palca / biometria twarzy przez `BiometricPrompt`).            | **Nie** (uprawnienie normalne).                             |
| `android.permission.USE_FINGERPRINT`                              | `normal`              | Biblioteka `androidx.biometric`                          | Wsteczna kompatybilność czytnika linii papilarnych dla starszych wersji systemu Android.               | **Nie** (uprawnienie normalne).                             |
| `pl.polsl.mobilepatient.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | `signature`           | Biblioteka `androidx.core` / `androidx.profileinstaller` | Bezpieczna rejestracja wewnętrznych odbiorników `BroadcastReceiver` wewnątrz procesu.                  | **Nie** (weryfikacja podpisu cyfrowego APK).                |

Dodatkowo w manifeście zadeklarowano:

```xml

<uses-feature android:name="android.hardware.camera" android:required="false" />
```

Dzięki `android:required="false"` aplikacja może być instalowana również na urządzeniach bez
wbudowanej kamery (np. dedykowane tablety medyczne), korzystając z alternatywnego systemowego Photo
Pickera.

---

## 2. Usunięcie zbędnych uprawnień dodawanych przez biblioteki zewnętrzne

Zgodnie z wymogiem **MASVS-PLATFORM-1**, biblioteki zewnętrzne (np. wtyczki map, analityki lub
obsługi mediów) mogą próbować dodawać do scalonego manifestu niepożądane uprawnienia.

W projekcie **MobilnyPacjent** zastosowano mechanizm `tools:node="remove"`, jawnie wycinający ze
scalonego manifestu niebezpieczne lub przestarzałe uprawnienia:

```xml
<!-- Usuwanie zbędnych i niebezpiecznych uprawnień wstrzykiwanych potencjalnie przez biblioteki zewnętrzne -->
<!-- Zastąpione bezpiecznymi mechanizmami: Photo Picker i Storage Access Framework (SAF) -->
<uses-permission android:name="android.permission.READ_PHONE_STATE"
    tools:node="remove" /><uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
tools:node="remove" /><uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
tools:node="remove" /><uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
tools:node="remove" /><uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
tools:node="remove" />
```

### Dlaczego te uprawnienia zostały usunięte?

1. **`READ_PHONE_STATE`:**
    - Często wstrzykiwane przez starsze SDK analityczne/diagnostyczne w celu odczytu identyfikatorów
      urządzenia (IMEI/IMSI). Całkowicie zablokowane ze względów prywatności pacjenta.
2. **`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES`:**
    - Dają aplikacji wgląd w całą prywatną galerię zdjęć i plików pacjenta.
    - **Rozwiązanie bezpieczne:** Wdrożono **Android Photo Picker**
      (`ActivityResultContracts.PickVisualMedia()`) oraz **Storage Access Framework**
      (`ACTION_OPEN_DOCUMENT`). Użytkownik wybiera dokładnie jeden plik, a system przekazuje
      tymczasowe uprawnienie URI tylko do tego pliku.
3. **`ACCESS_FINE_LOCATION`:**
    - Do prezentacji listy przychodni na mapie aplikacja korzysta ze stałych współrzędnych
      geograficznych placówek pobranych z tabeli `clinics`. Nie ma potrzeby ciągłego śledzenia
      precyzyjnej pozycji GPS pacjenta. Ewentualne centrowanie mapy może opierać się wyłącznie na
      przybliżonej lokalizacji (`COARSE`), o ile użytkownik wyrazi na to zgodę.

---

## 3. Bezpieczna konfiguracja manifestu

W manifeście wdrożono wzorce obronne:

```xml

<application android:allowBackup="false" android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules" ...>
```

* **`android:allowBackup="false"`:** Blokuje tworzenie nieszyfrowanej kopii zapasowej aplikacji za
  pomocą polecenia `adb backup -apk pl.polsl.mobilepatient`, co uniemożliwia wyciągnięcie tokenów
  sesji JWT oraz prywatnych danych pacjenta.
* **`data_extraction_rules.xml` & `backup_rules.xml`:** Wykluczają domenę `sharedpref`, `database` i
  `root` z synchronizacji w chmurze i transferu między urządzeniami.
* **`android:exported="true"` wyłącznie dla `MainActivity`:** Aktywność startowa jest jedynym
  punktem wejścia (filtr `MAIN` i `LAUNCHER`). Żadne wewnętrzne ekrany ani komponenty nie są
  eksportowane na zewnątrz.

---

## 4. Tabela Cyklu Życia Uprawnień Runtime

| Uprawnienie                                 | Po co jest potrzebne?                                                                                      | Kiedy prosimy użytkownika?                                                                                    | Co się dzieje przy odmowie? (Graceful degradation)                                                                                                                         | Alternatywa bez uprawnień                                                                                                                 |
|---------------------------------------------|------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **`android.permission.CAMERA`**             | Bezpośrednie wykonanie zdjęcia recepty, skierowania lub wyniku badania laboratoryjnego wewnątrz aplikacji. | Dopiero w chwili kliknięcia w przycisk **„Aparat”** w sekcji dodawania dokumentacji medycznej (Just-In-Time). | Aplikacja **nie ulega awarii (brak crasha)**. Wyświetla się czytelny komunikat z wyjaśnieniem, opcją przejścia do ustawień systemowych oraz sugestią użycia Photo Pickera. | **Android Photo Picker** (`PickVisualMedia`) – pacjent wybiera wcześniej zrobione zdjęcie z galerii bez przyznawania uprawnień aplikacji. |
| **`android.permission.POST_NOTIFICATIONS`** | Przypomnienia o zbliżających się konsultacjach lekarskich i powiadomienia o nowych e-receptach.            | Dopiero w chwili kliknięcia przycisku **„Zezwól na powiadomienia”** w panelu pacjenta lub ustawieniach.       | Aplikacja działa całkowicie normalnie. Wyświetlana jest informacja, że powiadomienia są nieaktywne, a wizyty można wciąż sprawdzać bezpośrednio w aplikacji.               | Brak powiadomień w pasku systemowym; informacje widoczne na liście wizyt wewnątrz aplikacji.                                              |

---

## 5. Procedura Testu Odmowy i Odwołania Uprawnień

W ramach weryfikacji odporności aplikacji na brak uprawnień runtime (Graceful Degradation)
zaimplementowano i przetestowano dwa scenariusze testowe:

### 5.1. Test Odmowy i Odwołania Uprawnienia Aparatu (`CAMERA`)

1. **Uruchomienie:** Zaloguj się do aplikacji i przejdź do widoku `Panel Pacjenta`
   (`MainDashboardScreen`).
2. **Scenariusz A (Odmowa w oknie dialogowym):**
    * Kliknij przycisk **„Aparat”**.
    * W systemowym oknie uprawnień wybierz opcję **„Nie zezwalaj”** (*Deny*).
    * **Obserwowany rezultat:** Aplikacja natychmiast wyświetla czerwoną kartę ostrzegawczą:
      > *„Odmowa dostępu do aparatu! Aplikacja nie ma zgody na korzystanie z aparatu. Nadal możesz
      załączyć plik przez bezpieczny Photo Picker lub nadać uprawnienie w ustawieniach systemu
      Android.”*
    * Aplikacja **nie ulega awarii** (`SecurityException`), udostępnia przycisk bezpośredniego
      przejścia do systemowych ustawień aplikacji oraz alternatywny przycisk **Photo Picker**.
3. **Scenariusz B (Odwołanie uprawnienia przez ADB):**
    * Nadaj uprawnienie aparatu w ustawieniach lub klikając „Zezwól”.
    * W terminalu komputera wykonaj polecenie cofające uprawnienie w działającej aplikacji:
      ```bash
      adb shell pm revoke pl.polsl.mobilepatient android.permission.CAMERA
      ```
    * Wróć do aplikacji i kliknij przycisk **„Aparat”**.
    * **Obserwowany rezultat:** Aplikacja natychmiast wykrywa brak uprawnienia, nie rzuca błędu i
      wyświetla komunikat o konieczności nadania dostępu w ustawieniach.

### 5.2. Test Odmowy Powiadomień (`POST_NOTIFICATIONS`)

1. Kliknij przycisk **„Zezwól na powiadomienia”**.
2. W oknie systemowym wybierz **„Nie zezwalaj”** lub cofnij uprawnienie poleceniem:
   ```bash
   adb shell pm revoke pl.polsl.mobilepatient android.permission.POST_NOTIFICATIONS
   ```
3. **Obserwowany rezultat:** Wyświetla się czytelna informacja, że przypomnienia nie będą aktywne w
   zasobniku powiadomień. Aplikacja zachowuje 100% stabilności.

### 5.3. Test Bezpiecznej Alternatywy (Android Photo Picker)

1. W sekcji dodawania dokumentacji kliknij **„Photo Picker”**.
2. System Android otwiera izolowany selektor systemowy (`PickVisualMediaRequest`).
3. Wybierz dowolny obraz badania.
4. **Obserwowany rezultat:** Aplikacja odbiera URI wybranego pliku i wyświetla potwierdzenie
   załączenia bez konieczności posiadania jakiegokolwiek uprawnienia do pamięci masowej
   (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES`)**.